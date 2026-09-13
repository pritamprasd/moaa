# PROMPT 00 — ANDROID DEVELOPMENT ENVIRONMENT AUDIT

You are the primary coding agent for a new Android project.

The user is an experienced software engineer but has not previously developed Android applications. We are building a high-performance, lightweight Android "super-app" that hosts dynamically downloaded tools/plugins.

DO NOT start implementing the application yet.

Your first responsibility is to inspect and prepare the development environment.

## PROJECT GOAL

The eventual application will:

* Be a lightweight Android host/dashboard.
* Use Kotlin + Jetpack Compose.
* Support phones and tablets.
* Support Android 14 / API 34 and newer.
* Be portrait-only.
* Use a dark Material 3 based futuristic UI.
* Dynamically download tool/plugin implementations at runtime.
* Keep only one tool active at a time.
* Persist tool state aggressively.
* Release inactive tool resources.
* Restore tools from persistent state after navigation/process death.
* Cache downloaded tool versions.
* Check for newer versions asynchronously.
* Download updates only when explicitly requested by the user.
* Use GitHub as the source of truth for tool metadata/artifacts.
* Eventually support potentially large tools, including tools that may need native libraries.
* Be optimized for maximum performance and responsiveness.
* Initially be a personal sideloaded application.

The architecture must NOT assume Google Play Dynamic Feature Delivery.

## IMPORTANT

Do not create application source code yet.

Do not create plugins yet.

Do not choose libraries merely because they are popular.

First inspect the actual machine and determine what is installed.

## STEP 1 — INSPECT THE MACHINE

Inspect:

* Operating system
* Architecture
* Java/JDK versions
* Android Studio
* Android SDK
* SDK platforms
* SDK build-tools
* Android platform-tools
* ADB
* Gradle
* Git
* GitHub CLI if installed
* Kotlin tooling if installed
* Available disk space
* Available RAM

Use appropriate shell commands.

Do not install anything without first reporting what is missing.

## STEP 2 — DETERMINE REQUIRED TOOLCHAIN

Determine the recommended stable versions of:

* JDK
* Android SDK
* Android Build Tools
* Android Gradle Plugin
* Gradle
* Kotlin
* Jetpack Compose
* Compose Compiler
* Android Studio

Use current official Android/Kotlin documentation when determining versions.

Do not blindly upgrade an already-working environment.

The final project should use a coherent, mutually compatible toolchain.

## STEP 3 — CHECK ANDROID DEVICE/EMULATOR

Determine whether:

* A physical Android device is connected.
* A usable emulator exists.
* A suitable emulator can be created.

We eventually need an environment capable of testing:

* Android 14+
* phone form factor
* tablet form factor
* low-memory conditions where practical

Do not create or modify an emulator unless necessary.

## STEP 4 — REPORT

Create:

docs/environment.md

Document:

* Host OS
* Host architecture
* Installed Java version
* Installed Android Studio version
* Installed SDK versions
* Build tools
* Gradle
* Git
* ADB
* Emulator/device status
* Missing components
* Recommended setup
* Compatibility concerns

Also create/update:

docs/decisions.md

Record the initial decision that the project will target:

* Kotlin
* Jetpack Compose
* Android API 34+
* Portrait-only
* Phone + tablet
* Dark Material 3 based UI

If a toolchain version cannot yet be determined with confidence, explicitly mark it as:

PENDING

Do not invent versions.

## STEP 5 — FUTURE WORK

Create:

future-use-cases.md

Add any useful future concerns discovered during environment inspection that are intentionally not being addressed yet.

Examples:

* CI/CD
* release signing
* multiple Android build variants
* native plugin support
* automated performance benchmarking
* Play Store distribution
* localization
* accessibility
* crash reporting

Do not implement these.

## STEP 6 — VALIDATION

Do not proceed to application implementation.

At the end, provide a concise report containing:

1. Environment status
2. What is already installed
3. What is missing
4. Recommended actions
5. Any blockers

Do not ask the user for permission to document findings.

Do not make unnecessary system changes.

## GENERAL AGENT RULES

From this project onward:

1. Inspect the repository before modifying anything.
2. Read docs/decisions.md before making architectural decisions.
3. Never silently change an established architectural decision.
4. Document significant architectural decisions.
5. Prefer the smallest dependency set possible.
6. Prefer Android platform capabilities over unnecessary third-party libraries.
7. Performance is a first-class requirement.
8. Never perform blocking I/O on the main/UI thread.
9. Never introduce a dependency without explaining why it is needed.
10. Never implement a feature merely because it might be useful.
11. Put deliberately postponed ideas in future-use-cases.md.
12. Do not prematurely implement the actual tools.
13. Do not use Android Dynamic Feature Modules as the runtime plugin mechanism.
14. The eventual plugin architecture must allow independently versioned downloadable tools.
15. The host application must remain independent from individual tools.
16. Plugins must depend on a stable plugin API, not host implementation internals.
17. Preserve backward compatibility of the plugin API whenever practical.
18. Optimize for correctness before micro-optimization.
19. Never claim something is unloaded from memory unless Android/runtime semantics actually guarantee it.
20. When uncertain about Android behavior, verify it against official Android documentation.

Wait for no human approval between internal subtasks unless a destructive or irreversible action would be required.
