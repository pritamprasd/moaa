# PROMPT 01 — CREATE THE PROJECT FOUNDATION

Continue the Android super-app project.

First inspect the current repository and read:

* docs/environment.md
* docs/decisions.md
* future-use-cases.md

Do not assume anything about the current repository.

## OBJECTIVE

Create the minimum viable Android project foundation.

The project must eventually become a lightweight host application capable of loading independently versioned downloadable tools.

Do NOT implement runtime downloading yet.

Do NOT implement dynamic plugin loading yet.

Do NOT implement real tools yet.

## ARCHITECTURE DIRECTION

Use a monorepo.

The intended high-level structure is:

```
app/
core/
plugin-api/
plugin-runtime/
plugins/
build-tools/
docs/
```

However, do not create empty modules merely to match this diagram.

Only create modules when they have an actual responsibility.

The intended dependency direction is:

```
plugin-api
   ↑
   │
plugin-runtime
   ↑
   │
  app
```

Individual plugins should depend on:

```
plugin-api
```

and must NOT depend directly on:

```
app
plugin-runtime implementation details
```

The exact Gradle module structure may be refined if you identify a better architecture.

Any deviation must be documented in docs/decisions.md.

## TECHNOLOGY

Use:

* Kotlin
* Jetpack Compose
* Material 3
* Android SDK
* Gradle
* Kotlin Coroutines where required
* Flow where appropriate

Avoid adding libraries unless there is a concrete requirement.

## ANDROID

Target:

* minimum Android API 34
* modern Android APIs
* phone
* tablet
* portrait orientation

The app should use a modern Android application architecture.

## APPLICATION STRUCTURE

Create a minimal host application with:

* Application class only if actually necessary
* single host Activity
* Compose root
* basic application state model
* placeholder dashboard screen
* navigation foundation
* dark theme foundation

Do not build the complete dashboard yet.

## DESIGN PRINCIPLE

The host must be independent of concrete tools.

The host should eventually know:

```
Tool ID
Tool name
Tool description
Tool icon
Tool version
Tool state
Tool availability
Tool update state
```

It should NOT know:

```
image editor implementation
video editor implementation
converter implementation
downloader implementation
network tool implementation
```

Those belong to plugins.

## DOCUMENTATION

Create:

docs/architecture.md

Document:

* module structure
* dependency direction
* application lifecycle concept
* future plugin lifecycle concept
* state persistence concept
* runtime download concept
* versioning concept
* security concept
* performance principles

Do not pretend features exist that haven't been implemented.

Clearly distinguish:

```
IMPLEMENTED
DESIGNED
FUTURE
```

Update:

docs/decisions.md

with every significant architectural decision.

Update:

future-use-cases.md

with anything deliberately deferred.

## GIT

Initialize Git if necessary.

Create a sensible .gitignore.

Do NOT commit secrets.

Do NOT create GitHub repository credentials.

Do NOT create signing keys yet.

## VALIDATION

At the end:

1. Build the application.
2. Run appropriate static checks available in the project.
3. Confirm the APK can be generated.
4. Confirm the Compose application launches.
5. Confirm the project has no unnecessary dependencies.

Do not begin implementing the plugin runtime.

## DEFINITION OF DONE

The project should have:

* clean Gradle structure
* compilable Android application
* Compose foundation
* Material 3 dark theme
* placeholder dashboard
* documentation
* Git repository
* reproducible build

The app should still be extremely small.

Report:

* files created
* modules created
* dependencies added
* architectural decisions
* validation performed
* known limitations

Then continue to the next task without waiting for user approval.
