# Architectural Decisions

This document records significant architectural decisions for the project. Before making any architectural decision, read this file. Never silently change an established decision; change it via a new dated entry.

Format: lightweight ADRs (status: Accepted / Pending / Superseded).

---

## ADR-001: Application stack

- **Date:** 2026-09-13
- **Status:** Accepted

The application is developed in **Kotlin** using **Jetpack Compose** (Material 3) as the UI toolkit. No imperative View/XML UI.

## ADR-002: System support baseline

- **Date:** 2026-09-13
- **Status:** Accepted

- **minSdk:** 34 (Android 14)
- **compileSdk:** 37 (Android 17, stable) — required by Compose 1.12+
- **targetSdk:** 36 (Android 16) — see rationale below.

**Rationale for targetSdk 36:** Android 17 (API 37) removes the developer opt-out for orientation and resizability restrictions for apps *targeting* API 37 on large screens (sw >= 600 dp). The project is portrait-only on phones and tablets, so targetSdk stays at 36 while compileSdk is 37 (compileSdk being newer is normal and does not trigger the API-37 large-screen behavior). Revisit only if the portrait-only decision changes.

## ADR-003: Orientation & form factors

- **Date:** 2026-09-13
- **Status:** Accepted

The app is **portrait-only** and must support **phones and tablets**. Large-screen/adaptive behavior is handled within a portrait layout (e.g. responsive grid for tablets), not via landscape. Note the API 37 large-screen restriction in ADR-002.

## ADR-004: Visual language

- **Date:** 2026-09-13
- **Status:** Accepted

**Dark Material 3** based UI with a futuristic look. Theming follows Material 3
dark color schemes and typography. No other UI framework.

## ADR-005: Runtime plugin mechanism is NOT Play Dynamic Feature Delivery

- **Date:** 2026-09-13
- **Status:** Accepted

The architecture must **not** assume Google Play Dynamic Feature Delivery. Tools/plugins are downloaded at runtime from GitHub (source of truth for metadata/artifacts), versioned independently, and loaded into a lightweight host/dashboard. The host is initially a personal sideloaded app.

## ADR-006: Toolchain (recommended, coherent set)

- **Date:** 2026-09-13
- **Status:** Accepted (pending first-build verification of the Kotlin/AGP interplay)

| Component | Version |
| --- | --- |
| JDK | 17 (Corretto 17.0.20, installed) |
| Android Gradle Plugin | 9.4.0 |
| Gradle (wrapper) | 9.6.0 |
| Android SDK Build Tools | 36.1.0 (installed; AGP default 36.0.0) |
| Kotlin | 2.4.20 (via AGP built-in Kotlin; Compose compiler plugin `org.jetbrains.kotlin.plugin.compose:2.4.20`) |
| Compose BOM | 2026.09.00 -> Compose 1.12.1, Material 3 (compose material3) 1.4.0 |
| Android Studio | Quail 4 (2026.1.4) |

Details and verification source in `docs/environment.md`.

**STATUS UPDATE (2026-09-13, first build):** verified working. AGP 9.4 built-in
Kotlin + Kotlin 2.4.20 + Compose compiler plugin 2.4.20 build successfully on
the first scaffold (`./gradlew build` -> app-debug.apk + app-release-unsigned.apk,
lint clean). The top-level `buildscript` classpath declares KGP 2.4.20 exactly as
the AGP 9.0 release notes recommend for upgrading the runtime KGP 2.2.10. No
fallback to 2.4.10 was needed. Dependency versions resolved against Google/Maven
metadata on 2026-09-13: Compose BOM 2026.09.00, activity-compose 1.13.0,
navigation-compose 2.10.1, lifecycle 2.11.0, kotlinx-coroutines 1.11.0.

## ADR-007: Dependency & library policy

- **Date:** 2026-09-13
- **Status:** Accepted

- Prefer the smallest dependency set possible.
- Prefer Android platform capabilities over third-party libraries.
- Never introduce a dependency without explaining why it is needed.
- Never implement a feature merely because it might be useful.
- Deliberately postponed ideas go to `future-use-cases.md`.

## ADR-008: Performance & lifecycle rules

- **Date:** 2026-09-13
- **Status:** Accepted

- Exactly **one** tool is active at a time.
- Tool state is persisted aggressively; inactive tool resources are released; tools are restored from persistent state after navigation or process death.
- Downloaded tool versions are cached; newer versions are checked asynchronously; updates download only when the user explicitly requests them.
- Never perform blocking I/O on the main/UI thread.
- Never claim something is unloaded from memory unless Android/runtime semantics actually guarantee it (verify against official documentation when uncertain).
- Optimize for correctness before micro-optimization.

## ADR-009: Plugin architecture rules

- **Date:** 2026-09-13
- **Status:** Accepted

- Components may be independently versioned downloadable tools.
- The host application must remain independent from individual tools.
- Plugins must depend on a stable plugin API, not host implementation internals.
- Preserve backward compatibility of the plugin API whenever practical.
- Do not prematurely implement the actual tools.

## ADR-010: Repository hygiene

- **Date:** 2026-09-13
- **Status:** Accepted

Inspect the repository before modifying anything; read `docs/decisions.md` before architectural decisions; document significant decisions here; no git repository initialized yet (the audit produced no commits).

## ADR-011: Initial module structure (deviation from the intended diagram, documented)

- **Date:** 2026-09-13
- **Status:** Accepted

The monorepo starts with exactly **two** modules: `app` (Android host) and `plugin-api` (Kotlin contract). The intended `plugin-runtime`, `plugins/`, `core/`, and `build-tools/` directories are **not** created yet because empty modules with no responsibility are worse than deferring them (task instruction: "do not create empty modules merely to match this diagram"). `plugin-runtime` will be inserted between `app` and `plugin-api` when the runtime is actually implemented; until then `app` depends on `plugin-api` directly. This is a deviation from the planned dependency chain and is tracked in `docs/architecture.md` section 2.

## ADR-012: plugin-api is pure Kotlin, zero dependencies

- **Date:** 2026-09-13
- **Status:** Accepted

`plugin-api` is a plain Kotlin/JVM library (`org.jetbrains.kotlin.jvm`) with **no Android types and no library dependencies**. The tool contract (`ToolId`, `ToolState`, `ToolInfo`, `Plugin`) must not drag in Android or coroutines. This keeps the contract host-agnostic and is the most stable surface for independently versioned tools (ADR-009). Android/Compose can depend on it (host and future plugins) without breaking the contract.

## ADR-013: Host application shape

- **Date:** 2026-09-13
- **Status:** Accepted

- A single host `MainActivity` with `enableEdgeToEdge()`; **no custom `Application` class** (nothing requires one yet).
- No DI framework. A hand-rolled `HostApplicationContainer` owns `HostAppState` (`StateFlow<List<ToolInfo>>`). `plugin-runtime` will be wired through this container later.
- State is in-memory only for now; persistence is DESIGNED in `docs/architecture.md` but not implemented (ADR-017).

## ADR-014: Navigation foundation

- **Date:** 2026-09-13
- **Status:** Accepted

Navigation Compose **2.10.1** (stable, 2026-09) is used as the navigation foundation with a single `dashboard` route (`HostNavHost`). It is the designated extension point for future tool detail screens. Justified even at one screen to avoid retrofitting navigation after the runtime lands.

## ADR-015: Backward-incompatible assumption — no screen/host Compose code in plugin-api

- **Date:** 2026-09-13
- **Status:** Accepted

plugin-api currently has **no UI** contract (no `@Composable` screen factory). Tool UI is a FUTURE design problem to solve with the plugin runtime; solving it now would couple the contract to Compose and to a rendition model that does not exist yet. `ToolInfo.icon` is similarly deferred (see future-use-cases.md).

## ADR-016: Dark only, no dynamic color

- **Date:** 2026-09-13
- **Status:** Accepted

Dark Material 3 only; the app does not follow the system light scheme and does not use dynamic/extraction color. A fixed futuristic palette (cyan/violet/rose on a deep space background) gives the host a stable identity. Light theme or dynamic color is a FUTURE enhancement, not a goal.

## ADR-017: State persistence deferred

- **Date:** 2026-09-13
- **Status:** Accepted

Tool catalog state is held in memory. Repository prefers platform capabilities and smallest dependency set (ADR-007), so persistence is not added until the plugin runtime defines what actually must survive process death. Persistence rationale is documented in `docs/architecture.md` section 6 (DESIGNED).

## ADR-018: Reproducible build pinning

- **Date:** 2026-09-13
- **Status:** Accepted

Gradle wrapper **9.6.0**, AGP **9.4.0**, JDK **17**, all dependency versions centrally in `gradle/libs.versions.toml`. Versions are resolved against official Google/Maven metadata, not invented. Upgrades (e.g. Gradle 9.7.x flagged by lint) are deliberate and documented, never silent. Release signing and CI are FUTURE (see future-use-cases.md).

## ADR-019: Orientation behavior note (lint-informed)

- **Date:** 2026-09-13
- **Status:** Accepted

`android:screenOrientation="portrait"` in the manifest, targetSdk 36 (ADR-002). Lint flags `DiscouragedApi`/`LockedOrientationActivity`: Android is moving to ignore fixed orientations and recommends opting out; this is accepted because portrait-only across phone and tablet is a hard requirement (ADR-003) and the API-37 large-screen opt-out is avoided via targetSdk 36. Re-visit on Android 16+ devices if the OS starts forcing dismissal of `screenOrientation` for targetSdk 36.

## ADR-020: Plugin artifact format — APK-shaped `.tpack` container

- **Date:** 2026-09-13
- **Status:** Accepted

A downloaded tool is a single self-contained ZIP-family file
`tool-<id>-<buildNumber>.tpack` with an APK file layout (`classes*.dex`,
`resources.arsc`, `res/`, `assets/`, optional `lib/<abi>/`) produced by the
standard aapt2/AGP pipeline, plus `meta/plugin.json` (untrusted, cross-checked)
and `meta/signature.bin` (our own detached signature over a canonical content
digest). The container is **never** installed through `PackageManager` and never
exposes components; its AndroidManifest is ignored. Rationale and rejected
alternatives: `docs/plugin-artifact.md` §2. Single-file containers are the only
atomic unit that can be verified, signed, and renamed in one operation
(requirements 14/15/17).

## ADR-021: Plugin loading — per-version DexClassLoader with parent-first whitelist

- **Date:** 2026-09-13
- **Status:** Accepted

Each installed version gets its own `DexClassLoader` whose `dexPath` is raw
extracted `.dex` files (avoids in-memory extraction of large containers per
`BaseDexClassLoader` docs); `optimizedDirectory` is null (deprecated since
API 26); `parent` is the host `PathClassLoader`, so shared libraries
(kotlin-stdlib, kotlinx-coroutines, Compose runtime, androidx core, plugin-api,
plugin-rendering) resolve host-first exactly once. Resources load via the
**public** `ResourcesProvider`/`ResourcesLoader` + read-only `ParcelFileDescriptor`
(API 30+); the reflective `AssetManager.addAssetPath` hack is rejected. Plugins
get **no Android Context**. "Unloaded" means "no live references, eligible for
collection" — ART does not guarantee deterministic class or native-library
unload, so the design claims reference-level disposal only (ADR-008).
Full details and uncertainty checklist: `docs/plugin-runtime.md` §2–§3, §13.

## ADR-022: Plugin lifecycle state machine

- **Date:** 2026-09-13
- **Status:** Accepted

Two layered machines: a persisted catalog `ToolState` (`AVAILABLE`, `DOWNLOADING`,
`INSTALLED`, `UPDATE_AVAILABLE`, `INCOMPATIBLE`, `CORRUPT`, `FAILED`) and an
in-memory runtime `PluginLifecycle` (`UNLOADED → ACTIVATING → ACTIVE → SUSPENDING
→ SUSPENDED → RESUMING → ACTIVE`, plus `DISPOSING → UNLOADED`, `PERSISTING`
overlay, `ERROR` terminal). Exactly one tool may be `ACTIVE` (ADR-008).
Diagram and transitions: `docs/plugin-runtime.md` §4.

## ADR-023: Rendering contract lives outside plugin-api

- **Date:** 2026-09-13
- **Status:** Accepted

`plugin-api` remains pure Kotlin/JVM with zero dependencies and **no UI**
(ADR-012, ADR-015). The Compose-bound rendering surface is a separate stable
artifact, `plugin-rendering`, versioned together with `plugin-api` as one
contract version. Plugins depend on both; the host supplies the Compose runtime
on the parent classloader. Contract and backward-compat rules:
`docs/plugin-api.md` §6, §8.

## ADR-024: Cryptographic scheme — ECDSA P-256 with SHA-256

- **Date:** 2026-09-13
- **Status:** Accepted

All authenticity/integrity uses `SHA256withECDSA` (P-256) available via platform
JCA providers on the supported floor (API 34+). Trust is anchored in **public
keys pinned inside the host** (per role: tool publisher, manifest author);
private keys exist only in CI/publisher infrastructure, never in the repo or app.
Both the server manifest and each artifact carry signatures (defense in depth,
offline-verifiable). APK v1/v2/v3 signatures are **not** used (they are written
for installed packages). Compromise response, cost/infrastructure
classification, and verification layers: `docs/plugin-security.md`.

## ADR-025: GitHub is the source of truth

- **Date:** 2026-09-13
- **Status:** Accepted

A manifest repository serves a signed `manifest.json` over `raw.githubusercontent.com`
(HTTPS); artifacts are GitHub **Releases** (`/releases/download/tool-<id>-<buildNumber>/…`,
with `.sha256` sidecars). Update discovery = compare per-tool `buildNumber`
(latest signed manifest) against installed; background checks happen on
foreground + ≤ every 6 h and **never download without explicit user action**
(ADR-008). Protocol and manifest format: `docs/plugin-runtime.md` §9.

## ADR-026: Versioning — monotonic build numbers + CURRENT/STAGED/AVAILABLE/FAILED

- **Date:** 2026-09-13
- **Status:** Accepted

The canonical version per tool is a strictly monotonic integer `buildNumber`;
`displayVersion` (semver-like) is decorative. On disk each version is an
immutable, read-only `v<N>/` directory beside the active one, so rollback is a
registry pointer change. STAGED is fully installed but not yet activated;
activation of a new version only flips `CURRENT` after it proves itself; on
failure the known-good version is restored. Interrupted/corrupt downloads,
crash windows, repair, and cleanup (keep CURRENT + STAGED, superseded removal,
never delete user-data) are specified in `docs/plugin-versioning.md`.

## ADR-027: Host persistence — DataStore registry + atomic section writes

- **Date:** 2026-09-13
- **Status:** Accepted

The host runtime persists the catalog and per-tool registry via a Jetpack
`DataStore` (single source of truth for `currentBuild`/`stagedBuild`/
`availableBuild`/`failedBuild` and `ToolState`), written **only after** the
filesystem state it records is already true (rename-before-register). Plugin
state sections are opaque atomic-renamed files; host `host-state.json` flushes on
every transition. Layering, ownership, and durability: `docs/plugin-api.md` §4–§5,
`docs/plugin-runtime.md` §7.

## ADR-028: Measurable performance baselines

- **Date:** 2026-09-13
- **Status:** Accepted

Numeric targets for startup, dashboard, navigation, activation, suspension,
persistence, frame time, memory, disk, and network are fixed in
`docs/performance.md` and will be enforced with Jetpack Macrobenchmark on the
API 34/37 emulator matrix (measurement is FUTURE). This supersedes the
qualitative principles in `docs/architecture.md` §10.