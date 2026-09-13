# Development Environment Audit

Audited: 2026-09-13

Purpose: baseline the host machine and record the recommended Android toolchain for this project. No application code exists yet; this is the preparation step.

---

## 1. Host

| Item | Value |
| --- | --- |
| Operating system | Ubuntu 24.04.4 LTS (Noble Numbat) |
| Kernel | 6.8.0-139-generic (x86_64) |
| Architecture | x86_64 |
| CPU | 16 logical cores |
| RAM | 94 GiB total (~74 GiB available at audit time) |
| Swap | 63 GiB |
| Disk (root `/`) | 394 GiB total, **~28 GiB free (93% used)** |
| KVM | Available (`/dev/kvm` present, VMX/SVM flags on CPU) -> hardware-accelerated emulator supported |

Disk is the tightest resource. See "Compatibility concerns" below.

## 2. Java / JDK

| Item | Value |
| --- | --- |
| Default JDK (`JAVA_HOME`) | Amazon Corretto 17.0.20 LTS via SDKMAN (`~/.sdkman/candidates/java/17.0.20-amzn`) |
| `java -version` | `17.0.20 2026-07-21 LTS` |
| Other system JDKs | OpenJDK 8 and OpenJDK 21 under `/usr/lib/jvm` |
| Android Studio bundled JBR | OpenJDK 25.0.3 |

JDK 17 satisfies the AGP 9.x minimum (17) and is the toolchain we will standardize the Gradle daemon on.

## 3. Android Studio

| Item | Value |
| --- | --- |
| Version | Quail 4 (2026.1.4) |
| Build | AI-261.26222.65.2614.16204760 |
| Install method | snap (`android-studio` 2026.1.4.7-quail4, revision 242), `classic` |
| Launcher | `/snap/bin/android-studio` |
| Bundled JRE | JDK 25.0.3 |

## 4. Android SDK

Location: `~/Android/Sdk` (default Android Studio location). `ANDROID_HOME` and `ANDROID_SDK_ROOT` are **not** set; a `local.properties` with `sdk.dir` (or the env vars) will be required for Gradle builds.

| Component | Installed | Notes |
| --- | --- | --- |
| Platforms | `android-19` (4.4.2/KitKat), `android-36` (Android 16) | API 34 platform is not installed; not needed to compile (minSdk only requires the platform to compile against nothing extra). API 37 platform not installed yet. |
| Sources | `android-19`, `android-36` | |
| Build tools | `36.1.0`, `37.0.0` | AGP 9.4 default is 36.0.0; installed 36.1.0 is newer and satisfies it (can pin `buildToolsVersion = "36.1.0"`). |
| Platform-tools / ADB | adb 35.0.0 (`~/Android/Sdk/platform-tools`) | A system `adb` 35.0.0 is also present at `/usr/bin/adb` (apt package). |
| Emulator | 36.5.10.0 (build 15081367) | |
| System images | `android-37.0` / `google_apis_playstore_ps16k` / `x86_64` | Android 17 (API 37), Google Play + 16 KB page size image. |
| `cmdline-tools` (`sdkmanager`, `avdmanager`) | **NOT installed** | Must be installed to manage SDK packages and create AVDs from the CLI. |
| NDK | **NOT installed** | Not needed yet; relevant for future native tool plugins. |
| SDK licenses | Accepted (`~/Android/Sdk/licenses` present; apt `google-android-licenses` installed) | AGP can auto-download missing components. |
| AVDs | None | |

## 5. Other tooling

| Tool | Version | Notes |
| --- | --- | --- |
| Gradle | 8.14.4 (snap) | Too old for AGP 9.4 (needs 9.6.0). The project will use the Gradle Wrapper pinned to 9.6.0; the wrapper downloads its own distribution. |
| Git | 2.43.0 | |
| GitHub CLI (`gh`) | **NOT installed** | Optional but useful later (GitHub is the source of truth for tool metadata/artifacts). |
| Kotlin | kotlinc 2.4.20 (snap) | Standalone compiler; build uses Kotlin via AGP built-in Kotlin (2.2.10 runtime minimum) + Compose compiler plugin 2.4.20. |

`~/.gradle/caches` already holds ~2.0 GiB of cached artifacts.

## 6. Device / emulator status

| Item | Status |
| --- | --- |
| Physical device | **None connected** (`adb devices` is empty) |
| Existing AVDs | **None** |
| Emulator binary | Installed (36.5.10.0) and functional |
| KVM acceleration | Available |
| Creatable emulator | Yes | 

The only installed system image is API 37 (Android 17), 16 KB page size, Google APIs + Play Store. No API 34/35/36 images and no API 34 image are installed, so testing the supported floor (Android 14 / API 34) requires downloading an `android-34` (and ideally `android-36`) system image. AVDs can be created via Android Studio's Device Manager, or via `avdmanager` once `cmdline-tools` is installed.

## 7. Recommended toolchain (coherent set, verified against official sources 2026-09-13)

| Component | Recommended | Status | Rationale / source |
| --- | --- | --- | --- |
| JDK | 17 (Corretto 17.0.20 already installed) | Installed | AGP 9.x minimum and default toolchain JDK. |
| Android Gradle Plugin | **9.4.0** | Not in project yet | Latest stable (2026-09-03); max supported API 37; compatible with Android Studio Quail 4 (2026.1.4). |
| Gradle | **9.6.0** (via wrapper) | Not present | AGP 9.4 minimum/default. System Gradle 8.14.4 is too old. |
| Build Tools | 36.1.0 (installed) | Installed | Satisfies AGP 9.4 default 36.0.0. |
| compileSdk | **37** (Android 17) | Platform not installed; AGP will auto-download | Required by Compose 1.12+ (per official docs); Android 17 is stable (released 2026-06-16). |
| minSdk | **34** (Android 14) | per requirements | Application requirement. |
| targetSdk | **36** (Android 16) | decision | See decisions.md -> keeps portrait-only enforceable on tablets (API 37 drops the orientation opt-out for sw >= 600 dp when targeting 37). |
| Kotlin | **2.4.20** | Toolchain-level | Latest stable (2026-09-07). Used via AGP built-in Kotlin (runtime KGP minimum 2.2.10; AGP auto-upgrades to a declared higher version). |
| Compose BOM | **2026.09.00** | Not in project yet | Latest stable BOM on Google Maven (2026-09-09) -> Compose 1.12.1. |
| Compose Compiler | Bundled with Kotlin; `org.jetbrains.kotlin.plugin.compose:2.4.20` | not in project yet | Since Kotlin 2.0 the Compose compiler ships from the Kotlin repo and is applied as a Gradle plugin matching the Kotlin version. |
| Material 3 | 1.4.0 (via BOM 2026.09.00) | not in project yet | Latest stable. |
| Android Studio | Quail 4 (2026.1.4) | Installed | Latest stable. |

> Note on AGP 9 "built-in Kotlin": from AGP 9.0 the `org.jetbrains.kotlin.android` plugin is no longer applied; Kotlin support is built in and enabled by default. The Compose compiler Gradle plugin (`org.jetbrains.kotlin.plugin.compose`) is still applied per Compose module. AGP 9.4 carries a runtime dependency on KGP 2.2.10 and auto-upgrades to a higher declared version. **First scaffold/build should confirm the exact Kotlin 2.4.20 + AGP 9.4 interplay before committing to it** (see decisions.md, status PENDING-to-verify).

## 8. Missing components

1. **`cmdline-tools`** (provides `sdkmanager`/`avdmanager`) - needed for SDK package management and headless AVD creation/management.
2. **API 37 platform** (android-37) - will be auto-downloaded by AGP on first build (licenses accepted), or installed via `sdkmanager`.
3. **System images for API 34/35/36** - required to test the minSdk 34 floor and Android 14-16 behavior on emulators.
4. **AVD** - no AVD exists; at least one phone AVD (and later a tablet AVD) must be created.
5. **Gradle Wrapper** pinned to 9.6.0 - not created yet (project has no build files; do this during scaffolding).
6. **`ANDROID_HOME`/`ANDROID_SDK_ROOT`** (or `local.properties`) - not set; set one or the other.
7. **`gh`** - optional; helpful for the GitHub-as-source-of-truth workflow.
8. **NDK** - deliberately deferred (future native tool plugins only).
9. **Physical device** - none connected; emulators only for now.

## 9. Recommended setup sequence (next steps, not executed in this audit)

1. Free disk space on `/` (see compatibility concerns) before downloading images/caches.
2. Install `cmdline-tools` into `~/Android/Sdk/cmdline-tools/latest` via the Google ZIP (or via Android Studio SDK Manager).
3. Set `ANDROID_HOME=~/Android/Sdk` and `ANDROID_SDK_ROOT=~/Android/Sdk` (or accept `local.properties` `sdk.dir`).
4. Scaffold the project with the Gradle Wrapper 9.6.0, AGP 9.4.0, Compose BOM 2026.09.00; first build will auto-download the android-37 platform.
5. Create a phone AVD from the installed API 37 image; download an API 34 (and 36) `google_apis` x86_64 image and create corresponding phone/tablet AVDs.
6. Optionally install `gh`.

## 10. Compatibility concerns

- **Disk space is low (~28 GiB free, 93% used).** The Android SDK + emulator images + Gradle caches grow quickly (a system image is ~2-7 GiB, an AVD disk is multi-GiB, a fresh dependency cache is ~1-2+ GiB). Free space before provisioning AVDs. Do not create multiple large AVDs simultaneously.
- **targetSdk 36 is intentional.** On Android 17 (API 37), apps that *target* API 37 on large screens (sw >= 600 dp) can no longer restrict orientation or resizability (`screenOrientation`, `setRequestedOrientation()`, `resizeableActivity=false`, min/max aspect ratio). Our decision is portrait-only across phones and tablets; therefore compile against 37 but keep `targetSdk 36`. This is the safest way to honor portrait-only on tablets while still using the latest Compose (which requires compileSdk 37).
- **16 KB page size image.** The installed emulator system image uses 16 KB memory pages (Android 17 default for new images). This is fine for the platform, but any *future* native (NDK) code must be 16 KB-aligned / ABI-stable; keep in mind when native tool plugins arrive (already in future-use-cases.md).
- **Play Store system image.** The only installed image bundles Google Play, which is acceptable for sideload testing but not rooted by default in the same way as `google_apis` images; for low-RAM and behavior testing, also grab a plain `google_apis` image.
- **AGP 9 / Kotlin 2.4.20 date delta.** AGP 9.4.0 (2026-09-03) ships with a KGP 2.2.10 runtime dependency; Kotlin 2.4.20 (2026-09-07) is newer than AGP 9.4. This combination is expected to work (built-in Kotlin auto-upgrades KGP >= 2.2.10 and KGP 2.x is forward compatible with AGP), but the first build should validate it. Fallback: Kotlin 2.4.10 (bug-fix release; the version referenced by current official Compose setup docs).
- **`gh` absent.** No blocker; affects only GitHub automation convenience.
- **No git repository initialized** in `mother_of_all_apps/` yet (audit only; no commit made).