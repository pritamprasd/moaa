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

- **Plugin runtime module (`plugin-runtime`)** — the biggest deferred piece: runtime download, verification, dynamic loading/unloading, and tool state transitions. The intended position is between `app` and `plugin-api` (see `docs/architecture.md`); today `app` depends on `plugin-api` directly. Do not implement until the loading/unloading semantics are verified against official Android docs.
- **Tool UI screen contract** — `plugin-api` is currently metadata-only. How a tool provides its `@Composable` screen (and how the host hosts it) is an open design; it must not be coupled into the contract prematurely.
- **`ToolInfo.icon`** — not modeled. Requires deciding the icon format (drawable, vector XML, packaged resource vs remote bytes) and delivery mechanism.
- **`plugins/` and `core/` directories** — not created; no concrete tools exist and no shared host logic has yet warranted its own module.
- **Native tool plugins (NDK/JNI)** — some tools may need native libraries. Not begun. Notes:
  - Requires installing the NDK (not currently installed).
  - The installed emulator image is 16 KB page size; all future native code must be 16 KB-aligned/ABI-compatible (Android 17 default for new images).
  - ABI coverage strategy (`x86_64` vs `arm64-v8a`, and 16 KB support) must be decided before shipping native artifacts.
- **Plugin API versioning** — stable plugin API with versioned contracts so tools evolve independently without breaking the host; confirm the version negotiation scheme (compatible-with ranges, min host version) before first tool ships.

## Performance & correctness tooling

- **Automated performance benchmarking** — e.g. Jetpack Macrobenchmark for startup, frame timing, and tool-switch latency; low-memory and large-tablet profiles.
- **Memory/resource guarantees** — verify actual unload semantics (activity/process, classloader release, native library freeing) against official documentation before claiming resources are freed; research per-tool classloader unloading risks (heap leaks, static state).
- **Low-memory conditioning (emulator)** — simulate low-RAM devices via AVD `hw.ramSize`/`ro.config.low_ram`, and `adb shell cmd activity memory-info` profiling.

## Tools & infrastructure

- **SDK/tooling automation** — install `cmdline-tools` (`sdkmanager`/`avdmanager`) for headless package management and AVD creation; useful for CI and reproducible emulator setups.
- **GitHub CLI (`gh`)** — install for metadata/artifact workflow automation later.
- **Additional system images** — download API 34 (Android 14) and API 36 (Android 16) `google_apis` x86_64 images to test the minSdk floor and intermediate OS versions; plus a tablet-profile AVD (e.g. Pixel Tablet) for the tablet form factor.
- **ADB over Wi-Fi 2.0 / physical device testing** — currently no physical device is connected; later, test on real hardware (orientation, thermal, large-screen behavior).

## Quality & reach

- **Crash reporting & diagnostics** — currently a personal app; decide later between Play + Firebase vs lightweight self-hosted/local capture; keep it non-blocking and privacy-frugal.
- **Accessibility** — Compose accessibility (content descriptions, touch targets, dynamic type, TalkBack passthrough) and a11y test coverage. Verify against the Compose a11y docs when implemented.
- **Localization** — extract strings, lint `missingTranslation`, per-locale layout checks (our portrait-only + tablet design may need language-aware density checks).
- **Automatic updates of downloaded tools** — only user-initiated downloads for now; later consider explicit one-tap "update all" with checksum verification and rollback (signed artifacts, signature/checksum verification before execution).
- **Offline-first tool cache** — already implied by caching decisions; formalize eviction policy and storage budget (disk is shared with the OS and the user's device runs tight on storage).

## Foundation (from the first scaffold, deliberately deferred)

- **Host state persistence** — `HostAppState` is in-memory only. Decide `SharedPreferences` vs `DataStore` and what must survive process death when the plugin runtime exists (see `docs/architecture.md` section 6).
- **Real dashboard catalog UI** — the dashboard currently shows an empty state plus a presentational `ToolCard` list; the actual catalog UI (icons, states, install/update actions) waits for the runtime.
- **Dynamic color / light theme** — deliberately not supported (ADR-016); revisit only if the futuristic dark identity changes.
- **Localization of current strings** — only default `res/values` exists.
- **Automated tests** — no unit or UI tests yet. Pick test frameworks (JUnit, Compose UI test, Robolectric if needed) and add coverage for `HostAppState`, navigation, and the dashboard when tool rendering exists.
- **Emulator/device validation matrix** — no AVD exists (`cmdline-tools` + `avdmanager` missing, no API 34/35/36/37 phone+tablet profiles yet); verify the portrait-only host on API 34..37 and a tablet profile once provisioning tooling is installed.
- **Release signing & CI** — covered under "Build & delivery" above.