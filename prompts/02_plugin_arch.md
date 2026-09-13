# PROMPT 02 — DESIGN THE PLUGIN RUNTIME ARCHITECTURE

We are building a lightweight Android super-app with dynamically downloaded tools.

Before implementing the runtime, design it thoroughly.

Read:

* docs/environment.md
* docs/architecture.md
* docs/decisions.md
* future-use-cases.md

## OBJECTIVE

Produce a technically rigorous design for the runtime plugin system.

Do NOT implement dynamic loading yet.

Do NOT download anything yet.

Do NOT create actual tool implementations.

## REQUIREMENTS

The final architecture must support:

1. Independently versioned tools.
2. Runtime download.
3. Local caching.
4. Offline operation after download.
5. One active tool at a time.
6. Aggressive state persistence.
7. Tool suspension.
8. Tool resource disposal.
9. Process death recovery.
10. Android lifecycle integration.
11. Tool-to-tool navigation.
12. Background update checking.
13. Explicit user-triggered update downloads.
14. Atomic version activation.
15. Old-version cleanup.
16. Cryptographic integrity verification.
17. Cryptographic authenticity verification.
18. Compatibility checks.
19. Failure recovery.
20. Future native library support.
21. Large plugin artifacts.
22. Host/plugin API compatibility.

## DESIGN THE FOLLOWING

### 1. Plugin artifact format

Define what a downloaded tool actually consists of.

Consider:

* metadata
* DEX
* resources
* native libraries
* assets
* manifest
* checksums
* signature

Do not assume APK is the correct format.

Compare viable options and select one.

Document tradeoffs.

### 2. Plugin loading

Investigate and design:

* DexClassLoader
* PathClassLoader
* classloader isolation
* resource loading
* context handling
* class unloading limitations
* Android security restrictions
* Android 14+ dynamic code loading requirements
* future native library loading

Do not claim true class unloading unless technically guaranteed.

### 3. Plugin lifecycle

Define an explicit lifecycle.

For example:

```
UNAVAILABLE
   ↓
INSTALLED
   ↓
LOADING
   ↓
ACTIVE
   ↓
SUSPENDING
   ↓
PERSISTING
   ↓
DISPOSED
   ↓
INSTALLED
```

Determine whether additional states are required.

### 4. Plugin contract

Design the stable plugin API.

It should conceptually support:

* metadata
* initialization
* rendering
* state restoration
* state persistence
* suspension
* disposal
* navigation requests
* resource ownership

Avoid leaking host implementation details into the API.

### 5. Persistent state

Design how each plugin stores:

* configuration
* UI state
* document state
* user data
* temporary state
* large files

Separate:

```
host state
plugin state
plugin user data
plugin cache
```

### 6. Process death

Design what happens when:

```
Android kills the application process
```

The tool must be reconstructable entirely from persistent state.

### 7. Version management

Design:

```
CURRENT
STAGED
AVAILABLE
FAILED
```

or an equivalent state machine.

Support:

* interrupted downloads
* corrupted downloads
* incompatible plugins
* failed activation
* rollback
* cleanup

### 8. Security

Design:

```
HTTPS
SHA-256
digital signatures
trusted public key
manifest verification
artifact verification
version verification
read-only protection
```

Determine what cryptographic mechanism should be used.

Explain:

* what is free
* what requires infrastructure
* what requires secret-key protection
* where signing keys should live
* what happens if the signing key is compromised

### 9. GitHub infrastructure

Design the protocol between the app and GitHub.

Define:

* manifest format
* release format
* artifact naming
* versioning
* compatibility
* checksum distribution
* signatures
* update discovery

Do not implement it yet.

### 10. Performance

Define measurable targets.

At minimum consider:

* startup
* dashboard rendering
* navigation
* plugin activation
* plugin suspension
* state persistence
* frame time
* memory
* disk I/O
* network I/O

### 11. Failure model

Explicitly design behavior for:

* no internet
* slow internet
* GitHub unavailable
* partially downloaded artifact
* corrupted artifact
* invalid signature
* incompatible host version
* incompatible Android version
* insufficient storage
* process death during installation
* process death during activation
* plugin crash
* plugin initialization timeout

### 12. Memory model

Design how the host ensures that only one tool is active.

Explicitly identify:

* strong references
* coroutine scopes
* image caches
* ViewModels
* Compose compositions
* listeners
* callbacks
* executors
* native resources

Define what "inactive" means.

## IMPORTANT

Do not implement any of this yet.

This task is architecture and documentation only.

## OUTPUT

Create/update:

```
docs/plugin-runtime.md
docs/plugin-api.md
docs/plugin-artifact.md
docs/plugin-security.md
docs/plugin-versioning.md
docs/performance.md
docs/decisions.md
```

Update:

```
future-use-cases.md
```

with ideas deliberately postponed.

Include diagrams using Mermaid.

Include explicit tradeoffs.

Whenever Android behavior is uncertain, verify against current official Android documentation.

## DEFINITION OF DONE

A developer should be able to read the documents and understand exactly:

* what a plugin is
* how it is packaged
* how it is downloaded
* how it is verified
* how it is loaded
* how it is displayed
* how it is suspended
* how state is persisted
* how it is restored
* how versions are updated
* how failures are handled
* how memory is controlled

No production runtime implementation should be added in this task.

After completing the design, continue autonomously to the next implementation milestone.
