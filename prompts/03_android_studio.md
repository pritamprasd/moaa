# PROMPT 03 — UBUNTU + ANDROID STUDIO DEVELOPMENT WORKFLOW

Continue the Android super-app project.

The development environment is explicitly:

* Ubuntu Linux
* Android Studio
* Android SDK
* JDK
* Gradle
* Git
* OpenCode as the agentic coding environment

The developer will primarily open, run, debug, profile, and manage the application through Android Studio on Ubuntu.

Read all existing documentation before making changes:

* docs/environment.md
* docs/architecture.md
* docs/plugin-runtime.md
* docs/plugin-api.md
* docs/plugin-artifact.md
* docs/plugin-security.md
* docs/plugin-versioning.md
* docs/performance.md
* docs/decisions.md
* future-use-cases.md

## OBJECTIVE

Establish a clean Android Studio + Ubuntu development workflow for this repository.

Do not implement plugin downloading or dynamic loading yet.

Do not create real tools yet.

## 1. ANDROID STUDIO COMPATIBILITY

Verify that the project opens correctly in Android Studio.

Ensure:

* Gradle sync works.
* Android Studio recognizes all modules.
* Kotlin code indexing works.
* Compose preview can be used where appropriate.
* Debug builds work.
* Release builds can eventually be configured cleanly.
* No IDE-specific generated files are committed.

Document any Android Studio configuration that developers need to know.

## 2. UBUNTU DEVELOPMENT

Document Ubuntu-specific development requirements.

Include:

* JDK setup
* Android SDK location
* ANDROID_HOME / Android SDK environment considerations
* ADB
* USB debugging
* udev rules if required for physical devices
* emulator setup
* Gradle usage
* Git
* useful shell commands

Do not assume the developer uses Windows.

Do not add Windows-specific instructions unless they are explicitly marked as optional.

## 3. BUILD WORKFLOW

The project must work through both:

Android Studio:

```
Run
Debug
Build
Profile
```

and terminal:

```
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

Use the actual Gradle tasks available in the project rather than blindly assuming task names.

Document the canonical commands in:

```
docs/development.md
```

## 4. DEVICE WORKFLOW

Document:

### Physical Android device

How to:

* enable developer options
* enable USB debugging
* verify with adb
* install debug APK
* launch/debug from Android Studio

### Emulator

Document the recommended emulator configuration.

We eventually need to test:

* phone
* tablet
* Android 14+

Do not create unnecessary emulator configurations automatically.

## 5. DEBUGGING WORKFLOW

Document how to investigate:

* crashes
* ANRs
* startup problems
* Compose recomposition issues
* memory problems
* leaked references
* background work
* plugin loading failures
* disk problems
* network problems

The eventual architecture must make these failures observable.

## 6. LOGGING

Define the project's logging strategy.

Requirements:

* useful during development
* minimal overhead
* structured where practical
* no sensitive information
* easy filtering through Android Studio Logcat
* production logging must be controllable

Do not add a large logging framework unless justified.

## 7. PERFORMANCE TOOLING

Identify the Android Studio tools that will later be used for:

* CPU profiling
* memory profiling
* allocation tracking
* frame rendering
* startup profiling
* battery/background activity
* network inspection

Document them in:

```
docs/performance.md
```

Do not perform extensive profiling yet.

## 8. ANDROID STUDIO PROJECT CONFIGURATION

Ensure the repository contains only source-controlled project configuration.

Do not commit:

* .idea configuration that is machine-specific
* local.properties
* generated build artifacts
* APKs
* signing keys
* secrets
* IDE caches

Update .gitignore appropriately.

## 9. DOCUMENTATION

Create:

```
docs/development.md
```

Update:

```
docs/environment.md
docs/decisions.md
future-use-cases.md
```

Document:

* Ubuntu development assumptions
* Android Studio workflow
* terminal workflow
* physical device workflow
* emulator workflow
* debugging workflow
* profiling workflow

## VALIDATION

Perform:

1. Gradle sync/build.
2. Debug APK build.
3. Static checks available at this stage.
4. Verify Android Studio project structure.
5. Verify adb if a device/emulator is available.

Do not modify the OS unnecessarily.

Do not install packages unless required and safe.

## DEFINITION OF DONE

A developer using Ubuntu + Android Studio should be able to clone the repository and understand:

* how to open it
* how to configure Android SDK
* how to run it
* how to debug it
* how to build it
* how to use a physical Android device
* how to use an emulator
* how to profile it

Do not implement the plugin runtime in this task.

Continue autonomously when complete.
