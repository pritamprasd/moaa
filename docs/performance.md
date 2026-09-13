# Performance

Status: **DESIGNED** (targets are set; nothing is measured yet; measurement is FUTURE).

Defines measurable targets for the host and runtime and how they are measured.
Budget owners are named so a violation is actionable in one place.

---

## 1. Principles (from ADR-008)

- One active tool at a time; inactive resources released.
- No blocking I/O on the main/UI thread.
- Version checks never part of critical startup path.
- Correctness before micro-optimization; then measure, then optimize.

## 2. Measurement method

- **Macrobenchmark** (Jetpack Macrobenchmark, `androidx.benchmark`) for cold/warm
  start, frame timing, switch latency, and tool activation, run on the installed
  emulator (API 37) matrix plus an API 34 image once provisioned
  (environment.md). Results recorded per commit via CI from `future-use-cases.md`.
- **Profiler trips** (`studio-profiler`, Perfetto) for memory PSS and allocation
  during switch/suspend cycles.
- **Low-memory conditioning**: AVD `hw.ramSize`/`ro.config.low_ram` profile to
  verify eviction behavior under pressure (`future-use-cases.md`).
- Baseline environment for every number below: emulator, debug-unoptimized build
  is NOT the baseline — targets are stated for release-optimized builds
  (R8 on) against a mid-tier device profile; emulator numbers are recorded for
  relative regressions only.

## 3. Targets

### 3.1 Startup

| Metric | Target | Owner |
| --- | --- | --- |
| Process start → first dashboard frame (cold) | ≤ 1.2 s P50, ≤ 1.8 s P95 on device; emulator relative baseline recorded | Host |
| Warm start (process resident) → interactive | ≤ 350 ms | Host |
| Registry read + reconcile + artifact re-verify before first ACTIVATION | ≤ 200 ms | Runtime/Verifier |
| Manifest background check | Never on critical path; ≤ 400 ms when run | Repository |

### 3.2 Dashboard rendering

| Metric | Target | Owner |
| --- | --- | --- |
| First frame after tools present | ≤ 500 ms (warm) ≤ 16.6 ms steady frame budget | Host UI |
| 100-tool catalog scroll | 60 fps, no dropped frames >1% | Host UI |
| Catalog recomposition on ToolState change | ≤ 8 ms | Host UI |

### 3.3 Navigation

| Metric | Target | Owner |
| --- | --- | --- |
| Dashboard → tool (SUSPENDED hot resume) | first tool frame ≤ 200 ms | Runtime/Session |
| Dashboard → tool (UNLOADED cold) | first frame ≤ 1.0 s (verify + load + compose) | Loader |
| Tool → tool | SUSPEND then ACTIVATE; total ≤ 400 ms + target cold/hot as above | LifecycleManager |

### 3.4 Tool activation (also a runtime contract budget)

| Phase | Budget | Owner |
| --- | --- | --- |
| onInitialize | ≤ 150 ms (typical small tool) | ToolPlugin |
| onRestore | ≤ 150 ms | ToolPlugin |
| onActivate | ≤ 250 ms to first `@Composable` emission | ToolPlugin + Loader |
| Total activation wall (verify excluded) | ≤ 600 ms P95 | LifecycleManager |

### 3.5 Suspension

| Metric | Target | Owner |
| --- | --- | --- |
| onSuspend handshake | ≤ 50 ms | ToolPlugin |
| Reference drop + composition removal + ResourcesProvider close | ≤ 30 ms | LifecycleManager |
| Full SUSPENDED | ≤ 100 ms P99 | LifecycleManager |

### 3.6 State persistence

| Metric | Target | Owner |
| --- | --- | --- |
| host-state.json write on transition | ≤ 20 ms | PersistenceStore |
| plugin section commit (typical ≤ 64 KB) | ≤ 30 ms P50, ≤ 100 ms P99 | PersistenceStore |
| Full flush on suspend (all sections) | ≤ 50 ms typical | PersistenceStore |
| fsync policy | batched; never on main thread | PersistenceStore |

### 3.7 Frame time

- 16.6 ms (60 Hz) budget; 120 Hz devices honor 8.3 ms where enabled.
- No frame dropped during activation/suspension *transitions* (they run on
  background dispatchers; only the final composition commit touches UI).

### 3.8 Memory

| Metric | Target | Owner |
| --- | --- | --- |
| Host baseline (no tool) PSS | ≤ 160 MB | Host |
| Host + ACTIVE tool PSS | ≤ 256 MB (budget against `getMemoryClass()`) | MemoryController |
| Inactive tool after SUSPENDED/UNLOADED | no measurable growth (eligible for collection) | MemoryController |
| Plugin cache budget | ≤ 15% of free heap, capped 64 MB | MemoryController + plugin contract |
| DEX/art code mmap (clean pages) | excluded from PSS worry, but logged | Loader |

### 3.9 Disk I/O

| Metric | Target | Owner |
| --- | --- | --- |
| Install of 50 MB artifact (verify+place) | ≤ 3 s on device storage | Installer |
| Streaming verify on activation | ≤ 300 ms for 50 MB | Verifier |
| Suspension flush | ≤ 100 ms (see 3.6) | PersistenceStore |
| Storage budget (all tools) | bounded; cleanup threshold defined in §5 | Cleaner |

### 3.10 Network I/O

| Metric | Target | Owner |
| --- | --- | --- |
| Manifest fetch | ≤ 300 ms, ≤ 200 KB | Repository |
| Artifact throughput | saturate available bandwidth; resume on interruption | Downloader |
| Byte-checks: sha256 streaming (no extra full copies) | 1 pass | Verifier |

## 4. Allocation & jank guidance (runtime hosting)

- The tool's Compose composition must be side-effect-free on scroll and keep
  allocations out of the hot path; host-provided `ToolHost` slot is `remember`d.
- Session executor threads are bounded (≤ 4) and never synthesized per frame.
- Image caches: session-scoped and LRU-capped (contract); the host never caches
  plugin images in the dashboard.

## 5. Storage budget & eviction (relates to versioning cleanup)

- Global plugin budget: default 500 MB (device-dependent; floor 20 % of free
  space). Manifest-aware: the dashboard shows per-tool size (from signed manifest)
  before the user's install/update tap.
- Cleanup triggers: after activation, on process start, `onTrimMemory`.
- Never evicts user-data or plugin `plugin-state` beyond version-retention rules
  (`docs/plugin-versioning.md` §6).

## 6. Verification matrix (measurement-grade)

Each target carries an automated check where infrastructure allows
(Macrobenchmark in CI, `future-use-cases.md`); until CI exists, targets are
validated manually on the API 37 emulator and recorded next to the run.