# Future Use Cases (deliberately postponed)

Intentional future concerns discovered during environment inspection and/or project planning. These are **not** being implemented now. Add new postponed ideas here instead of implementing them prematurely.

---

## Build & delivery

- **CI/CD pipeline** — automate assemble, lint, unit + instrumentation tests (e.g. GitHub Actions since GitHub is the source of truth for metadata/artifacts).
- **Release signing** — set up a signed release build for sideloading and (later) distribution; store the keystore securely, never in the repo.
- **Multiple build variants / flavors** — e.g. `dev`/`release`, different application IDs, and the AGP 9 strict 1:1 flavor-dimension parity rules to keep in mind if dynamic feature modules are ever evaluated (they are ruled out as the plugin mechanism, but plain modules could still be adopted).
- **Play Store distribution** — currently a personal sideloaded app; if ever published, revisit targetSdk requirements, Play policies, and the API 37 large-screen orientation rule impact on portrait-only.
- **Gradle build cache / remote cache / daemon tuning** — a fresh ~1-2+ GiB dependency cache and ~28 GiB free disk mean build hygiene (cache control, `.gitignore`) matters early.

## Plugins & native support

- **Plugin runtime implementation** — the architecture is now fully designed (`docs/plugin-runtime.md`, `plugin-api.md`, `plugin-artifact.md`, `plugin-security.md`, `plugin-versioning.md`, `performance.md`; ADRs 020–028). What remains is the **implementation milestone**: building `plugin-runtime` between `app` and `plugin-api`, then the verification matrix on API 34..37 (loading, read-only enforcement, resource IDs, classloader collection, Compose ABI). Do not build until the loading/unloading assumptions are proven on-device (runtime §13).
- **`ToolInfo.icon`** — not modeled. Requires deciding the icon format (drawable, vector XML, packaged resource vs remote bytes) and delivery mechanism; the container's `assets/` is the natural future carrier.
- **`plugins/` and `core/` directories** — not created; no concrete tools exist and no shared host logic has yet warranted its own module.
- **Native tool plugins (NDK/JNI)** — designed in `docs/plugin-artifact.md` §7 but not begun. Notes:
  - Requires installing the NDK (not currently installed).
  - The installed emulator image is 16 KB page size; all future native code must be 16 KB-aligned/ABI-compatible (Android 17 default for new images).
  - ABI coverage strategy (`x86_64` vs `arm64-v8a`, and 16 KB support) must be decided before shipping native artifacts.
  - Native libraries are **not unloadable** within a process; per-tool native lifetime policy (process restart vs load-once) must be resolved at implementation time.
- **Plugin permission / capability model** — designed as FUTURE in `docs/plugin-api.md` §3.4 (capability requests: e.g. storage, camera intent). Not implemented; every tool currently runs with only what the host grants and sandboxed FileScopes.
- **Dev-mode hot reload for plugin authors** — sideloaded unsigned `.tpack` in debug builds only (`docs/plugin-artifact.md` §6); a fuller live-reload loop (watcher + rebuild + re-activate) is outside the runtime's ship scope.
- **Multi-process plugin sandboxing** — running tool code in an isolated process (separate classloader+namespace per process) would give stronger fault isolation and real native unload (process death) at a heavy IPC cost. Postponed; the in-process guarded harness in `docs/plugin-runtime.md` §8 is the v1 boundary.
- **WebAssembly / JS tool runtime** — an alternative sandbox for untrusted or third-party tools, at the cost of losing Compose tools. Only reconsidered if tool trust changes or isolation requirements outgrow DEX.

## Performance & correctness tooling

- **Automated performance benchmarking** — targets are fixed in `docs/performance.md`; the harness (Jetpack Macrobenchmark + Perfetto trips + low-memory AVD profiles) is not built yet. Wire it into CI once CI exists; validate manually on API 37 meanwhile.
- **Memory/resource guarantees** — verify actual unload semantics (activity/process, classloader release, native library freeing) against official documentation before claiming resources are freed; research per-tool classloader unloading risks (heap leaks, static state).
- **Low-memory conditioning (emulator)** — simulate low-RAM devices via AVD `hw.ramSize`/`ro.config.low_ram`, and `adb shell cmd activity memory-info` profiling.

## Tools & infrastructure

- **SDK/tooling automation** — install `cmdline-tools` (`sdkmanager`/`avdmanager`) for headless package management and AVD creation; useful for CI and reproducible emulator setups.
- **GitHub CLI (`gh`)** — install for metadata/artifact workflow automation later.
- **Additional system images** — download API 34 (Android 14) and API 36 (Android 16) `google_apis` x86_64 images to test the minSdk floor and intermediate OS versions; plus a tablet-profile AVD (e.g. Pixel Tablet) for the tablet form factor.
- **ADB over Wi-Fi 2.0 / physical device testing** — currently no physical device is connected; later, test on real hardware (orientation, thermal, large-screen behavior).

## Plugin delivery & trust infrastructure

- **Signed revocation channel** — `docs/plugin-security.md` §9 depends on a host repin to truly revoke a key; a resilient out-of-band revocation channel (e.g. a short-lived signed "blocklist" also fetched over HTTPS) would shrink the response window. Postponed until tool count grows.
- **Hardware-backed key custody** — CI secret storage works for v1; a signing HSM / isolated signer for both manifest and tool keys is the planned escalation as releases multiply.
- **Delta / differential plugin updates** — for very large artifacts, binary diffs (vs full re-download) would cut bandwidth; postponed — correctness of atomic verified installs comes first.
- **Per-tool signed provenance / SBOM** — recording build provenance for each `.tpack` (build job id, deps, source commit) to strengthen the authenticity story; useful once multiple people publish tools.

## Quality & reach

- **Crash reporting & diagnostics** — currently a personal app; decide later between Play + Firebase vs lightweight self-hosted/local capture; keep it non-blocking and privacy-frugal.
- **Accessibility** — Compose accessibility (content descriptions, touch targets, dynamic type, TalkBack passthrough) and a11y test coverage. Verify against the Compose a11y docs when implemented.
- **Localization** — extract strings, lint `missingTranslation`, per-locale layout checks (our portrait-only + tablet design may need language-aware density checks).
- **Automatic updates of downloaded tools** — only user-initiated downloads for now; later consider explicit one-tap "update all" with checksum verification and rollback (signed artifacts, signature/checksum verification before execution).
- **Offline-first tool cache** — already implied by caching decisions; formalize eviction policy and storage budget (disk is shared with the OS and the user's device runs tight on storage).

## Foundation (from the first scaffold, deliberately deferred)

- **Host state persistence** — design accepted (ADR-027: DataStore registry + atomic section writes, `docs/plugin-api.md` §4–§5, `docs/plugin-runtime.md` §7); implementation waits for the plugin runtime milestone.
- **Real dashboard catalog UI** — the dashboard currently shows an empty state plus a presentational `ToolCard` list; the actual catalog UI (icons, states, install/update actions) waits for the runtime.
- **Dynamic color / light theme** — deliberately not supported (ADR-016); revisit only if the futuristic dark identity changes.
- **Localization of current strings** — only default `res/values` exists.
- **Automated tests** — no unit or UI tests yet. Pick test frameworks (JUnit, Compose UI test, Robolectric if needed) and add coverage for `HostAppState`, navigation, and the dashboard when tool rendering exists.
- **Emulator/device validation matrix** — no AVD exists (`cmdline-tools` + `avdmanager` missing, no API 34/35/36/37 phone+tablet profiles yet); verify the portrait-only host on API 34..37 and a tablet profile once provisioning tooling is installed.
- **Release signing & CI** — covered under "Build & delivery" above.