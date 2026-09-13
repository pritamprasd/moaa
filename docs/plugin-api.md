# Plugin API Contract

Status: **DESIGNED** (no implementation yet).

Defines the stable boundary between the host and downloadable tools
(ADR-009). It is split into **two stable surfaces**:

1. `plugin-api` — pure Kotlin/JVM, zero dependencies (ADR-012): metadata,
   lifecycle, persistence, navigation. **No Android types, no Compose.**
2. `plugin-rendering` — a separate, deliberately thin, Compose-bound surface: how
   a tool exposes its UI. Kept out of `plugin-api` on purpose (ADR-015).

This doc is the contract. The current `plugin-api` module
(`ToolId`, `ToolState`, `ToolInfo`, `Plugin`) is a subset of what is designed
here; extending it to this contract is a later implementation milestone.

---

## 1. Principles

- Plugins depend **only** on `plugin-api` (+ `plugin-rendering` for UI).
  They never see `plugin-runtime` or `app` internals.
- The contract never leaks host implementation details: no `Context`, no
  `Activity`, no `Application`, no `PackageManager`, no repository/git concepts.
- The bytecode ABI of both surfaces is **frozen per contract version**
  (see §8 backward compatibility).
- All asynchronous execution is delivered to the plugin through host-owned
  execution primitives from `java.util.concurrent` (JVM stdlib, keeps the
  dependency-free rule). Threads are owned and bounded by the host.
- Everything a plugin does is scoped to a **session**; nothing survives outside
  it unless the plugin writes to its persistence scope.

## 2. Vocabulary

| Term | Meaning |
| --- | --- |
| Tool | What a user installs and runs (a product perspective) |
| Plugin | The code+resources package that implements a tool (architectural perspective) |
| Session | One activation of a plugin by the host (dashboard→tool, tool→tool) |
| Contract version | The `plugin-api` / `plugin-rendering` ABI version a plugin was compiled against |
| `ToolInfo` | Host-visible, catalog-facing descriptor (already exists) |

## 3. `plugin-api` surface (pure Kotlin)

### 3.1 Catalog metadata (exists, extended)

```kotlin
@JvmInline value class ToolId(val value: String)   // exists

enum class ToolState {                              // exists; extended (see §7)
    AVAILABLE, DOWNLOADING, INSTALLED,
    UPDATE_AVAILABLE, INCOMPATIBLE, CORRUPT, FAILED,
}

data class ToolInfo(
    val id: ToolId,
    val name: String,
    val description: String,
    val version: String,        // display version; the engine compares buildNumber
    val state: ToolState,
    // DESIGNED additions (future contract versions):
    //   val buildNumber: Long
    //   val icon: ToolIcon?,            // deferred (future-use-cases.md)
    //   val sizeBytes: Long,
)
```

### 3.2 Lifecycle contract

```kotlin
interface ToolPlugin {
    val info: ToolInfo

    /** One-time, synchronous. Runs inside ACTIVATING with an init-time budget. */
    fun onInitialize(context: PluginContext)

    /**
     * Runs when the tool becomes ACTIVE. May do heavy work; the returned
     * rendering description is consumed by the host (or null for pure-task tools).
     */
    fun onActivate(session: PluginSession): ToolRendering?

    /** Runs before the host suspends the tool. Persist anything not yet flushed. */
    fun onSuspend(persistence: PersistenceOutput)

    /** Runs after state restore, before onActivate, when resuming from persistence. */
    fun onRestore(persistence: PersistenceInput)

    /** Runs once when the loaded version is torn down (SUSPENDING→DISPOSING→UNLOADED). */
    fun onDispose(reason: DisposeReason)
}

enum class DisposeReason { USER_NAVIGATED_AWAY, MEMORY_EVICTION, VERSION_REPLACED, HOST_SHUTDOWN, CRASH_RECOVERY }
```

Lifecycle timing rules:

- `onInitialize` and `onActivate` are invoked on the host's **activation
  dispatcher** (bounded executor, never the main thread); the host enforces a
  per-phase budget (see `docs/performance.md` §3.4).
- `onSuspend` and `onRestore` may be called on a persistence worker.
- All four are **cancellable-deadline** enforced: if a callback exceeds its
  budget it is declared failed and the host moves the tool to
  `FAILED`/`CORRUPT` per the failure model (`docs/plugin-runtime.md` §8).
- Plugins must be **serializable-state reconstructable**: after
  `onRestore(persistence)`, the plugin must be able to `onActivate` again fully
  without any prior in-memory data (process-death requirement 6/9).

### 3.3 `PluginContext` (host services, no Android types)

```kotlin
interface PluginContext {
    /** Host-declared host version + contract version, for negotiation. */
    val hostEnvironment: HostEnvironment

    /** Executor owned & bounded by the host, cancelled on suspension. Never the main thread. */
    val executor: ToolExecutor            // thin wrapper over java.util.concurrent.Executor

    /** Scoped file locations. See §4 persistent state. */
    val state: PluginStorage              // path to state/ (host+plugin state)
    val userData: FileScope               // path to user-data/
    val cache: FileScope                  // path to cache/ (evictable, see policy)

    /** Read-only view of the current tool's installed artifact dir (if needed). */
    val resourcesRoot: FileScope
}
```

`HostEnvironment`:

```kotlin
data class HostEnvironment(
    val hostVersionName: String,          // e.g. "0.1.0"
    val hostBuildNumber: Long,            // monotonic
    val pluginApiVersion: Int,            // contract version of plugin-api in host
    val renderingApiVersion: Int,         // contract version of plugin-rendering in host
    val androidApi: Int,                  // Build.VERSION.SDK_INT snapshot
)
```

Plugins negotiate via `plugin.json`'s `requiresPluginApi`/`requiresHostVersion`
(`docs/plugin-artifact.md` §4); the runtime enforces the gate **before**
activating (`docs/plugin-runtime.md` §6).

### 3.4 Session

```kotlin
interface PluginSession {
    val toolId: ToolId
    val metricScope: SessionMetrics        // counters/timers the host attributes to this session

    /** Request the host move to another installed tool (tool-to-tool navigation). */
    fun requestNavigate(target: ToolId, payload: NavigationPayload? = null)

    /** Bring up host-provided system capabilities the tool was granted (FUTURE capability model). */
    // fun request(capability: Capability): CapabilityHandle?      // FUTURE
}
```

`NavigationPayload` is an immutable ID-keyed bag (e.g. "imagePath" → path string).
Pointer-sized data crosses via a payload file the host stages in the target
tool's `state/nav-inbox/` before activating it (lifecycle handoff in
`docs/plugin-runtime.md` §4). Nothing user-owned crosses by reference.

### 3.5 Persistence contract (`java.io`, zero deps)

```kotlin
interface PersistenceOutput {
    fun section(name: String): OutputStream        // raw section stream
    fun putBlob(key: String, bytes: ByteArray)
    fun putJson(key: String, json: String)         // plugin-provided JSON; host does not parse
    fun commit()                                    // durable ack (see §5 durability)
}

interface PersistenceInput {
    fun sectionNames(): Set<String>
    fun section(name: String): InputStream?
    fun blob(key: String): ByteArray?
    fun json(key: String): String?
}
```

- Sections are atomic per key (write-tmp + rename). `commit()` flushes and
  (where the platform permits) fsyncs; it returns only after durable ack, and the
  host schedules commits on a persistence worker (no main-thread fsync).
- The plugin is free to use any serialization **it** depends on; the host only
  stores opaque bytes. No schema shared between host and plugin beyond
  section/key strings.

### 3.6 `FileScope`

```kotlin
interface FileScope {
    fun root(): File
    fun resolve(relative: String): File
    fun list(): List<File>
}
```

Read-only for `resourcesRoot`; read/write for `userData` and `cache`.

---

## 4. Persistent state ownership (requirement 5)

Four distinct areas. The plugin sees only its three; the host owns the fourth.

```
filesDir/plugins/<toolId>/
├── v<N>/                  # HOST-owned, read-only installed artifact (plugin never writes)
├── state/
│   ├── host-state.json    # HOST-owned: last-run version, flags, timestamps. NOT plugin-visible
│   ├── plugin-state/      # PLUGIN-owned: PersistenceOutput sections (opaque bytes)
│   └── nav-inbox/         # HOST-owned: staged NavigationPayload for next activation
├── user-data/             # PLUGIN-owned: durable user data (big, kept across versions)
└── cache/                 # PLUGIN-visible, HOST-managed eviction budget
```

| Area | Owner | Survives version upgrade? | Survives uninstall? | Example content |
| --- | --- | --- | --- | --- |
| `host-state.json` | host | yes | no | last active version, suspend timestamp |
| `plugin-state/` | plugin | migrated if schema allows | no | UI scroll, in-progress document |
| `user-data/` | plugin | yes (by policy) | no | documents, outputs, settings |
| `cache/` | plugin | yes (evicted on budget) | no | thumbnails, fetched API caches |

`user-data/` is **not** included in version rollback; `plugin-state/` is
**snapshot-rolled-back** with the version to keep out-of-document state
consistent with the binary that produced it (see `docs/plugin-versioning.md`
§5 rollback).

---

## 5. Durability & aggressive persistence (requirement 6)

- The host saves `host-state.json` **on every state transition** and at
  suspend time; plugin sections are committed whenever the plugin calls
  `commit()` or the host suspends/persists.
- Default write cadence: UI-affecting host state = immediate; plugin sections =
  on `commit()` / suspend / backgrounding; large `user-data/` files = streaming
  on write with periodic fsync.
- Aggressive means "state is reconstructable from disk at any crash point", not
  "fsync on every keystroke". Per-section atomic rename keeps torn writes
  impossible; a torn **directory** (renamed but unfinished sections) is repaired
  by the recovery step in `docs/plugin-runtime.md` §7.

## 6. `plugin-rendering` surface (Compose-bound, intentionally thin)

Kept in a separate artifact so `plugin-api` stays pure (ADR-012/015). It is
versioned together with `plugin-api` as one **contract version**.

```kotlin
// plugin-rendering (Compose + Material3 types, NO host internals)
interface ToolRendering {
    @Composable
    fun Content(interaction: ToolInteraction)
}

interface ToolInteraction {
    val session: PluginSession
    fun navigateBack(): Unit                     // to dashboard or previous tool
    fun onDisposeCallback(): (() -> Unit)?       // hook for last-chance cleanup
}

@Composable
fun ToolTheme(content: @Composable () -> Unit)   // maps to the host's dark futuristic theme
```

- The host invokes `Content(interaction)` inside its NavHost composable route for
  the tool. `ToolTheme` guarantees visual consistency without exposing Material3
  palette internals.
- Plugins are encouraged to build with Compose primitives + `plugin-rendering`
  components; using raw Material3 outside `ToolTheme` is possible but will break
  visual identity (documented contract guidance, not enforced at runtime).
- The Compose **runtime** classes come from the host's parent classloader
  (shared-library whitelist, `docs/plugin-runtime.md` §2.2). Plugins compile
  against the pinned mapped-in Compose BOM and must **not** bundle duplicate
  Compose runtime classes.

## 7. `ToolState` mapping (host-visible)

The catalog state derives from the version machine + lifecycle:

```
AVAILABLE        → artifact not installed
DOWNLOADING      → install/update transfer in progress
INSTALLED        → a verified version is on disk, quiet
UPDATE_AVAILABLE → newer signed manifest version exists; user must trigger download
INCOMPATIBLE     → newest version exists but fails compatibility gate
CORRUPT          → verification failed after placement (see recovery, plugin-runtime §8, versioning §5)
FAILED           → install/activation failed deterministically; user-visible retry
```

## 8. Backward compatibility (host/plugin API compatibility, req 22)

- Contract version `N` is additive-only. A plugin compiled against `N` runs on
  host contract `N` and **`N+1`** (additive). A plugin compiled against `N+1`
  that needs new calls must declare `requiresPluginApi.min = N+1`.
- Enforcement (gate) happens against the signed manifest before activation:
  `host.contractVersion ∈ plugin.requiresPluginApi.[min,max]`; mismatch ⇒
  `INCOMPATIBLE` (`docs/plugin-runtime.md` §6).
- Compose/mapped dependency ABIs: the host pins the Compose BOM it ships; a
  plugin compiled against an older BOM is binary-forward-compatible with an
  additive upgrade. A **downgrade** of the shared runtime (host update that
  removes an ABI) is treated as a contract break and is subject to the same
  min-version gate. This is the single most fragile part of the scheme and is
  documented as a **verified-on-device** assumption (integration suite,
  `docs/performance.md` §verification matrix).

## 9. What is deliberately NOT in the contract

- `Context`, `Activity`, `Application`, resources inflation.
- `ViewModel` and host DI container.
- Downloading / manifest fetching / signature verification — the plugin can
  never initiate its own update; it cannot read or write other tools' data.
- File system access outside its three `FileScope`s and `resourcesRoot`.
- Anything process-level (kill, restart, native load policy).