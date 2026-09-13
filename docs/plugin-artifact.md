# Plugin Artifact Format

Status: **DESIGNED** (no implementation yet).

Defines what a downloaded tool actually is, how it is packaged, named, and
verified as a file. The decision here constrains the loading design in
`docs/plugin-runtime.md` and the signing design in `docs/plugin-security.md`.

---

## 1. Decision

A **plugin artifact** is a single self-contained ZIP/APK-family container file
with a canonical layout: a `.tpack` file ("tool package"). It is **not** a
Play-installable APK, and it is **not** a Play Dynamic Feature (ADR-005).

- The container is produced by the standard Android toolchain (`aapt2`/AGP) so a
  plugin is built like a normal Android library module plus one metadata step.
- The host never installs it; it is never handed to `PackageManager` for
  installation and its `AndroidManifest.xml` is ignored by the runtime
  (component registration is not supported/needed).
- `classes*.dex` carries the plugin bytecode.
- `resources.arsc` + `res/` carry compiled resources.
- `assets/` carries file-based assets (icons, fonts, data files).
- `lib/<abi>/` carries optional native libraries (FUTURE, ADR milestone).
- `meta/plugin.json` carries machine-readable metadata.
- `meta/signature.bin` carries the publisher's encrypted signature over the
  canonical content digest (see `docs/plugin-security.md`).

Naming: `tool-<toolId>-<buildNumber>.tpack`, e.g. `tool-image-resizer-42.tpack`.
The build number is the canonical monotonic version; upper/lower display names
are decorative (see `docs/plugin-versioning.md`).

---

## 2. Why not the alternatives

### 2.1 Plain APK loaded uninstalled

| Aspect | Verdict |
| --- | --- |
| Tooling reuse (aapt2, AGP R class) | Strong: everything a plugin author already knows works |
| Read resources from an uninstalled APK | OK via public `ResourcesProvider.loadFromApk(ParcelFileDescriptor)` (API 30+; our floor is API 34) |
| Load dex | OK via `DexClassLoader` (documented to load from APKs containing `classes.dex`) |
| Components (Activity/Service/…) | **Unusable without a pre-declared host container Activity + reflective `startActivity`, which leaks host internals and breaks THE one-activity host (ADR-013)** |
| Icon/theme/overlay manifest fields | Unsupported by the host by design |
| APK package ID / `packageName` | Meaningless for an uninstalled APK loaded by the host process |

The APK **shape** is excellent; the APK **semantics** (package identity, manifest
components, install) are wrong for us. Our answer is `.tpack`: keep the APK
*file layout*, drop the manifest semantics.

### 2.2 Loose files (dex + resources + assets shipped separately)

| Aspect | Verdict |
| --- | --- |
| Partial update atomicity | **Fails requirement 14** — no single-file commit point, hard to keep CURRENT/STAGED coherent |
| Verification | Must verify N files; signature over a directory is fragile (ordering, extra files, path traversal) |
| Android 14 read-only enforcement | Must make every loaded file read-only, more surface to get wrong |
| Download resume | Multiple parallel downloads, more failure modes |

Rejected: single-file containers are the only atomic unit we can verify, sign,
and rename in one operation.

### 2.3 Plain JAR with `classes.dex`

| Aspect | Verdict |
| --- | --- |
| `DexClassLoader` load | OK — documented to accept `.jar` with `classes.dex` |
| Compiled resources (`res`/`resources.arsc`) | **No tooling** produces a valid arsc inside a plain jar without aapt2; we would lose the resource pipeline |
| Native libs | Possible but we would hand-roll the layout |

Rejected: loses the Android resource build pipeline with no benefit over `.tpack`.

### 2.4 Zipped DEX + manual `AssetManager.addAssetPath` reflection

| Aspect | Verdict |
| --- | --- |
| Resource loading | Relies on hidden/reflective `AssetManager.addAssetPath(String)` behavior — fragile across versions, blocked by hidden-API policy |

Rejected. The public API `ResourcesProvider` + `ResourcesLoader` + 
`Resources#addLoaders(...)` (API 30+, `android.content.res.loader.*`) is the
supported path and is used instead. Using a **file descriptor** (`ParcelFileDescriptor`)
keeps the container read-only and satisfies the Android 14 safer-DCL model.

### 2.5 In-app virtualmachine sandbox (WebAssembly / JS / scripting engine)

| Aspect | Verdict |
| --- | --- |
| Isolation | Strongest in principle |
| Tool fidelity | Would rule out Compose-based tools; every tool becomes JS/WASM |
| Native support | Different story, not better |

Rejected for now; noted as a long-term alternative sandbox in
`future-use-cases.md`. Tools are expected to be rich Compose UIs; a bytecode
interpreter cannot provide that.

---

## 3. Canonical layout

```
tool-<toolId>-<buildNumber>.tpack        (ZIP; entry names are '/' separated)
├── classes.dex                          # required — plugin bytecode (could be raw dex)
├── classes2.dex ...                     # optional — AGP multidex
├── resources.arsc                       # optional — compiled resource table
├── res/…                                # optional — compiled resource files
├── assets/…                             # optional — file-based assets
├── lib/<abi>/lib*.so                    # optional — FUTURE native libraries
│                                        #   abi ∈ {arm64-v8a, armeabi-v7a, x86_64}
├── meta/
│   ├── plugin.json                      # required — runtime metadata (see §4)
│   └── signature.bin                    # required — publisher signature (see §5)
└── META-INF/                            # produced by tooling; IGNORED by the host
    ├── MANIFEST.MF
    └── …  (any detached APK signature is irrelevant to us)
```

Rules:

1. `classes*.dex`, `meta/*` are always present; everything else is optional.
2. Entry names must be safe on extraction: no `..`, no leading `/`, no absolute
   paths. The host rejects any container that violates this (Android 14 ZIP
   path-traversal hardening is relied on **and** checked ourselves before
   extraction).
3. All entries that will ever be passed to a classloader or `ResourcesProvider`
   are served **read-only**. On Android 14+ (targetSdk 34+), loading a writable
   dex/jar/apk throws `SecurityException: writable dex file ... is not allowed`
   (Change ID `218865702 ENFORCE_READ_ONLY_JAVA_DCL`). The host therefore never
   writes into an extracted artifact directory after placement; artifacts are
   finalized (verified, read-only) before first load.
   Reference: https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
4. Compressed native libs inside the ZIP cannot be `dlopen`ed directly by the
   runtime's native lookup (AOSP `DexPathList.findLibrary` only resolves libs
   that are **STORED uncompressed** in the zip). The host therefore **extracts
   native libs** to a per-tool native dir and passes that dir as
   `librarySearchPath` to the classloader (FUTURE, §7).

Why the repeated `meta/` + `META-INF/` split: `META-INF/` is where AGP signs;
we never rely on APK v1/v2/v3 signatures and always ignore it. Our own signing
data lives under `meta/` and is defined by us, not by the Android signing scheme
(see `docs/plugin-security.md` §4 — "why not APK signatures").

---

## 4. `meta/plugin.json` — runtime metadata

Untrusted convenience metadata. It is **cross-checked** against the signed
server manifest before anything is honored; by itself it proves nothing
(see `docs/plugin-security.md` §6).

```json
{
  "toolId": "image-resizer",
  "buildNumber": 42,
  "displayVersion": "1.7.2",
  "displayName": "Image Resizer",
  "description": "Resize, crop and convert images.",
  "requiresPluginApi": { "min": 1, "max": 1 },
  "requiresHostVersion": { "min": "0.1.0" },
  "requiresAndroidSdk": { "min": 34, "max": 37 },
  "abi": ["arm64-v8a", "x86_64"],
  "licenses": ["MIT"],
  "sizeBytes": 4821337
}
```

- `toolId` and `buildNumber` must equal the values from the **signed server
  manifest** (versioning doc). Discrepancy ⇒ reject.
- `requires*` ranges drive the compatibility gate in
  `docs/plugin-runtime.md` §6 (host/plugin API compatibility).
- `abi` is advisory today and authoritative when native libs ship.

## 5. `meta/signature.bin` — self-containment

Contents: a fixed-length canonical digest list plus a signature over it.

```
signature.bin :=
  header(2 bytes: fmtVersion=0x0001)
  keyIdLen(4)   keyId               # which trusted public key should verify
  entryCount(4)
  for each entry (sorted lexicographically by path, EXCLUDING meta/signature.bin):
     pathLen(2) path
     sha256(32)
  signatureLen(4) signature         # ECDSA P-256 / SHA-256withECDSA (host pubkey must match keyId)
```

Verification procedure (detailed in `docs/plugin-security.md` §6): sort entries,
recompute SHA-256 per entry, rebuild the digest block, look up `keyId` in the
host-configured trust store, verify the signature, then `close()` the provider
and keep files read-only.

Why both an outer (server manifest) signature and this inner (artifact)
signature — tradeoff:

| | Outer manifest signature only | **Both** (chosen) | Inner only |
| --- | --- | --- | --- |
| Metadata authenticity (version, hashes) | ✔ | ✔ | Needs manifest anyway |
| Offline re-verification after download | ✘ (needs cached manifest; cache can be evicted) | ✔ | ✔ |
| Tamper resistance without host trust-store update | partial | ✔ | ✔ |
| Build/signing infra cost | one key | **two keys or one key reused** | one key |
| Attack if manifest is stale/spoofed on device storage | possible | mitigated | mitigated |

Selected: **both**. The extra cost is one more signature verification per
install/activation (negligible) and one more signing step in the build pipeline.

---

## 6. Build process (source-side, FUTURE)

A `build-tools/` Gradle module (or CI script) produces `.tpack` files:

1. Build the plugin module as a normal Android **library** (`aar`) or apk-shaped
   output so `res`, `resources.arsc`, and `classes*.dex` are produced by AGP
   exactly as a plugin author expects.
2. Assemble the container with the canonical layout (§3).
3. Stamp `meta/plugin.json`.
4. Compute the canonical digest block (§5) and sign it with the publisher key
   kept in CI-downloaded secrets — never in the repository (ADR-024, `docs/plugin-security.md` §5).
5. Upload to the tool's GitHub release (see `docs/plugin-runtime.md` §9) and
   generate the server manifest entry.

Dev convenience: a `dev-mode` unsigned `.tpack` (with a `dev=1` flag) may be
sideloaded during development only; the runtime refuses it in non-debug builds.

---

## 7. Native libraries (FUTURE, designed now)

- `lib/<abi>/lib*.so` unpacked into `filesDir/plugins/<toolId>/v<N>/native/<abi>/`
  (read-only) and supplied via `DexClassLoader(dexPath, null, nativeDir, parent)`.
- ABI selection uses `Build.SUPPORTED_ABIS`; artifact `abi` list is filtered.
- **Unloading a native library is not guaranteed.** A loaded `.so` cannot be
  reliably freed without terminating the process (Android's native loader
  keeps handles/namespaces for process lifetime). Consequences:
  - Native-having tools are effectively process-scoped once activated.
  - Version upgrade of a native-having tool while another version is live in the
    same process must be handled by "load new version, never reuse old process
    resident lib" or by process restart policy. This is the documented reason the
    version unload story is partial for native tools (see `plugin-runtime.md` §2.5, §12).
- The installed emulator/system images are **16 KB page size**; native code must
  be 16 KB-aligned/ABI-compatible (environment.md, future-use-cases.md).

---

## 8. Large artifacts (requirement 21)

- Zip default (deflate) for dex/resources; `STORED` for entries that must be
  mmap-friendly; container files routinely 5–100 MB.
- Download is streamed to `downloads/<toolId>-<buildNumber>.part` with
  length-aware resume (HTTP Range) — see `plugin-runtime.md` §8 table row
  "interrupted download".
- Verification streams SHA-256 (no full materialization).
- Extraction is streaming and single-pass; the `codeCache`/dex-opt outputs are
  managed by the OS.
- The `custom` extension hint is ignored: this is a plain ZIP, no size limit.

## 9. Atomicity & placement

```
filesDir/plugins/<toolId>/
├── downloads/            # .part files, resumable, never loadable
├── v<N>.tmp/             # staged container unpacked + verified (mid-install)
├── v<N>/                 # CURRENT or STAGED installed artifact (read-only)   [rename target]
├── native/<N>/<abi>/     # extracted native libs (FUTURE)
└── state/                # host + plugin persisted state (see §5 persistent state)
```

- Install = `unpack into v<N>.tmp → verify → chmod read-only → atomic rename
  v<N>.tmp → v<N> → registry update` (same filesystem ⇒ same-volume rename).
- The **registry** (`DataStore`, host-side) is the single source of truth for
  which `v<N>` is CURRENT/STAGED. See `docs/plugin-versioning.md` §5.
- Crash between "rename done" and "registry updated" is repaired on next startup
  by comparing registry vs on-disk `v<N>` dirs (`docs/plugin-runtime.md` §7).

## 10. Verification checklist at a glance

Before any byte is trusted:

1. Transport is HTTPS (GitHub) — nothing loads DCL content over plaintext.
2. Server manifest downloaded; its signature verified against pinned host trust.
3. Artifact SHA-256 from the signed manifest matches the downloaded `.part`.
4. Container self-signature (`meta/signature.bin`) verifies over canonical digests.
5. Entry names are path-traversal-safe (rejected by the runtime, and by the
   Android 14 ZIP hardening if missed).
6. `meta/plugin.json` is consistent with the signed manifest.
7. Everything extracted is placed read-only before any load (Android 14).