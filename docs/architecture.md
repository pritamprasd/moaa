# Architecture

Status for each section is marked with one of:

- **IMPLEMENTED** - exists and is exercised by the current code.
- **DESIGNED** - the design is fixed on purpose and documented here, but the code does not implement it yet.
- **FUTURE** - the concept is deferred; the exact design is not yet locked.

The current foundation intentionally implements only what the host application
needs today. Concepts that would require the plugin runtime are documented but
not coded.

---

## 1. Module structure (IMPLEMENTED)

```
mother_of_all_apps/
├── app/                    Android application (the host)
├── plugin-api/             Stable tool/plugin contract (pure Kotlin, zero dependencies)
├── build-tools/            Not created yet (FUTURE)
├── core/                   Not created yet (FUTURE - for shared host logic if extraction is warranted)
├── plugins/                Not created yet (FUTURE - concrete downloadable tools)
├── plugin-runtime/         Not created yet (FUTURE - runtime download/loading/verification)
└── docs/                   Architecture, decisions, environment baseline
```

Only `app` and `plugin-api` exist. They are the only modules that currently
have a real responsibility; empty modules are deliberately avoided (see
ADR-011).

## 2. Dependency direction (IMPLEMENTED)

```
plugin-api         <- pure Kotlin contract. No Android, no dependencies.
   ↑
   │ project(":plugin-api")
   │
  app              <- host application (Compose UI, state model)
```

The intended full direction (ADR-009) is:

```
plugin-api
   ↑
   │
plugin-runtime      (FUTURE)
   ↑
   │
  app
```

When `plugin-runtime` is created it will sit between `app` and `plugin-api`.
Until then `app` depends on `plugin-api` directly; the insertion point is
`HostApplicationContainer` (see section 5).

Concrete plugins (FUTURE) must depend only on `plugin-api` and must never
depend on `app` or `plugin-runtime` implementation details.

## 3. What the host knows about a tool (DESIGNED)

The host models a tool purely through `ToolInfo` in `plugin-api`:

| Property    | Type                    | Status      |
|-------------|-------------------------|-------------|
| Tool ID     | `ToolId` value class    | IMPLEMENTED |
| Tool name   | `String`                | IMPLEMENTED |
| Description | `String`                | IMPLEMENTED |
| Version     | `String`                | IMPLEMENTED |
| State       | `ToolState` enum        | IMPLEMENTED |
| Availability| derived from `ToolState`| DESIGNED    |
| Update state| `ToolState.UPDATE_AVAILABLE` | DESIGNED |
| Icon        | not modeled yet         | FUTURE      |

The host never sees tool behavior: no image/video editor, converter,
downloader, or network-tool types exist anywhere in `app`. Those belong to
plugins (FUTURE).

## 4. Application lifecycle concept

### Currently (IMPLEMENTED)

- One process, one `MainActivity`, no custom `Application` class (ADR-015).
- `MainActivity` calls `enableEdgeToEdge()` for modern edge-to-edge insets.
- `HostApplicationContainer` is created in `setContent` via `remember`; it owns
  the `HostAppState` and lives as long as the activity composition.
- `HostAppState` exposes a `StateFlow<List<ToolInfo>>` (currently always empty).

### Designed (DESIGNED)

- Host state survives configuration changes (single-activity, Compose state
  holder kept outside recomposition).
- Process death: `HostAppState` is in-memory only for now; restoring the tool
  catalog after process death requires persistence (section 6), which is DESIGNED.

## 5. Future plugin lifecycle concept (DESIGNED / FUTURE)

Designed states each tool moves through:

```
AVAILABLE ──download──▶ INSTALLED ──update check──▶ UPDATE_AVAILABLE ──update─▶ INSTALLED
     │                                                     │
     └────────────verify fail────────▶ CORRUPT ◀───────────┘
```

- Exactly one tool is active at a time (ADR-008).
- The runtime will be a new `plugin-runtime` module inserted between `app` and
  `plugin-api`. It will:
  1. Resolve tool metadata (source of truth: GitHub, per ADR-005).
  2. Download artifacts (FUTURE, section 7).
  3. Verify integrity before install (FUTURE, section 9).
  4. Load and unload tool code in an isolated way (FUTURE).
  5. Publish `ToolInfo` + state transitions into `HostAppState`.
- Unloading semantics are NOT yet claimed to guarantee classloader release;
  that must be verified against official documentation before the runtime
  ships (ADR-008, future-use-cases.md).

## 6. State persistence concept (DESIGNED)

- The tool catalog (list of `ToolInfo` + per-tool state) is the only state the
  host cares about today.
- Design: persist catalog state and the "active tool" selection; restore
  aggressively after process death (ADR-008).
- Mechanism not chosen yet (`SharedPreferences` vs `DataStore`); currently the
  state is in-memory. Persistence itself is FUTURE (ADR-017).

## 7. Runtime download concept (FUTURE)

- Not implemented. Downloads happen only on explicit user action (ADR-008).
- Metadata and artifacts come from GitHub (ADR-005), version-checked
  asynchronously against the source of truth without blocking the UI.
- Newer versions are cached in parallel with the installed version so a failed
  update never loses the working tool (rollback path).

## 8. Versioning concept (DESIGNED)

- Host and plugin-api share the repository and are versioned together until
  `plugin-runtime` and independent tool releases exist (ADR-012, ADR-009).
- Versions are pinned in `gradle/libs.versions.toml` (single source of truth);
  Gradle wrapper pinned to 9.6.0; JDK 17. Reproducible build (ADR-018).
- FUTURE: tools are independently versioned; plugin-api must remain backward
  compatible so tools evolve without breaking the host.

## 9. Security concept (FUTURE)

- Signed/checksum-verified artifacts before execution; integrity check before
  any tool is marked `INSTALLED`.
- No tool code runs with privileges the host does not grant.
- Secrets/keystores are never committed (`.gitignore`; release signing is
  FUTURE). No signing keys exist in the repository.
- Revisit: certificate pinning, artifact signing, provenance, and the
  unload-safety of per-tool classloaders are all FUTURE with no silver bullet
  assumed.

## 10. Performance principles (DESIGNED)

- One active tool at a time; inactive tool resources released (ADR-008).
- No blocking I/O on the main thread; coroutines + Flow are the concurrency
  model (`kotlinx-coroutines-android`, StateFlow in `HostAppState`).
- Version checks are asynchronous and never part of critical startup.
- Correctness first, then micro-optimization (ADR-008).
- FUTURE: Macrobenchmark-based startup/frame/latency measurement.

## 11. UI & navigation (IMPLEMENTED)

- Compose + Material 3, dark scheme only (ADRs 001, 004, 016).
- Single host scalable root `HostApp` -> `AppTheme` -> `HostNavHost`.
- Navigation Compose 2.10.1 with one `dashboard` route; NavHost is the
  extension point for future tool screens (ADR-014).
- Placeholder `DashboardScreen`: top app bar + empty catalog state; renders a
  `ToolCard` list from `List<ToolInfo>` when tools exist (proving host/plugin
  decoupling without any concrete tool).

## 12. Known limitations (IMPLEMENTED)

- `tool` catalog is empty by design; no tools exist yet.
- No runtime download, no dynamic loading, no persistence.
- `ToolInfo.icon` is not modeled.
- Not localized beyond default English strings (`res/values`).
- No automated tests yet (unit/UI suites are FUTURE).
- No emulator/device validation gap is hidden: launching on API 34..37 images
  is pending (see decisions ADR-002 targetSdk rationale).