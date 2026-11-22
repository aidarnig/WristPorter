# Software architecture

This document owns technical decisions and module boundaries. Product requirements live in [PRODUCT.md](PRODUCT.md), quality gates in [QUALITY.md](QUALITY.md).

## Modules

```text
watch-domain    Plain Java: photo validation, naming
      ^
watch-adb       ADB subprocess client: pairing, device state, push, scan
      ^
watch-core      Spring non-web orchestration: batch/retry
      ^
watch-desktop   JavaFX UI, preferences and Spring Boot bootstrap
```

Spring runs with `WebApplicationType.NONE`, not as a local web server. Blocking ADB calls run on background workers and JavaFX state changes run on the JavaFX application thread.

### Shared selected-watch transfer owner (W14-A)

`watch-core/TransferCoordinator` owns the selected watch+ADB executable lease (with a generation that cannot be revived by selecting the old device again), per-origin logical photo queues and previous results, cancel/retry state, and **one global active ADB transfer slot**. Desktop and a future authorized Web adapter share this instance in the same Windows process. New submissions while busy are rejected, not run concurrently. A watch or ADB selection change invalidates old leases and requests cancellation; the slot is released **only after the running operation exits**, not when cancellation is clicked. Before an enqueued job starts it rechecks the lease, and every invocation of the unchanged transfer service uses the snapshotted serial.

`WatchTransferService.send/retry` remains the single copier/indexer; a successful copy followed by scan failure retains the remote name for **scan-only** retry, never a second push. A cancelled or failed batch can be resumed **only under the original lease and unchanged queue**; acknowledged successes are skipped, scan failures are rescanned, and unacknowledged paths are retried. The last in-flight push can be ambiguous if interrupted before its callback, so this is best-effort de-duplication rather than a guarantee of no duplicate on crash/cancel. The ordinary Desktop **Send** action uses `sendPending` against the selected watch lease: it submits only never-attempted paths, retaining acknowledged successes in the queue when more photos are added. Earlier copy/scan failures remain available through explicit **Retry**, and clearing/removing photos forgets their outcomes. Switching the selected watch or ADB executable invalidates old outcomes: an image already present on the previous watch is not assumed to exist on the next. `TransferCoordinator.Event` is an immutable internal callback invoked off the JavaFX thread; `WatchWorkspace` marshals it with `Platform.runLater`. The event may contain a local path or ADB failure cause, so **a future HTTP adapter must expose only sanitized, coarse progress**, never the event record verbatim. The current refactor starts no listener and introduces no Safari/browser functionality.

## Copy and indexing

Target the explicitly selected watch via `adb -s SERIAL`. Ensure `/sdcard/DCIM/Camera` exists; create a UUID-based safe filename; push the photo; then broadcast `android.intent.action.MEDIA_SCANNER_SCAN_FILE` for the uploaded URI. A scan failure must be distinguishable from a failed copy. A scan-only retry reuses the remote filename, without a second push. The broadcast is a legacy Android API confirmed on the reference Galaxy Watch 4 Classic.

## Security decisions

- Use `ProcessBuilder` argument arrays rather than a shell string.
- Provide pairing codes through stdin, not command arguments or logs.
- Validate image signatures and use generated ASCII basenames for destination paths.
- Never write to an implicitly chosen watch or invoke destructive remote commands.
- No telemetry, silent network services, or runtime downloads.

## Portable distribution and third-party ADB

Windows `jpackage --type app-image` bundles the Java runtime. `ci/bundle-adb.ps1` obtains a **checksum-pinned official Genymobile scrcpy Windows distribution** at packaging time and extracts *only* `adb.exe`, `AdbWinApi.dll`, and `AdbWinUsbApi.dll`, which are published with scrcpy and originate from Android Debug Bridge (AOSP). This avoids redistributing the entire Android SDK. The script checks the upstream ZIP SHA-256 and tests `adb version`; the portable ZIP contains provenance and Apache-2.0 license text in `platform-tools/`. The third-party version/hash/source of truth is the pinned packaging script, not a duplicated constant in documentation.

The app discovers `platform-tools/adb.exe` next to its packaged `runtime/` folder. An explicit `WRISTPORTER_ADB` override takes precedence; the legacy `WGS_ADB`, user Documents Platform-Tools and PATH remain supported fallbacks.

A legal/license review before public release must confirm compliance with the upstream notices and any third-party terms. Do not use Samsung/Google marks in a way that implies endorsement.

## Planned independent Wear OS project (not present in v1.1.2)

[ADR-0001](adr/0001-independent-wearos-bridge.md) fixes separation: the four Java/Maven desktop modules remain the tested Wi-Fi ADB/photo-transfer product. A new Kotlin/Gradle Wear OS APK will communicate with native iPhone BLE services without an iOS companion or running desktop PC. The desktop may later install/update the watch APK through explicit ADB actions; this does not make Bridge a dependency of photo transfer.

Technical working design and open device questions: [BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md). Protocol and feature constraints: [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md). First verify hardware via [W0 issue #13](https://github.com/aidarnig/WristPorter/issues/13), and never import an unsupported Android minSdk from another repository.

## Planned ecosystem: same Windows EXE with optional browser entry point

[ADR-0004](adr/0004-one-desktop-optional-lan-portal.md) approves **one Windows application** with the existing JavaFX desktop view plus an **optional, explicitly started Windows-hosted local web frontend**. Safari on iPhone uploads to Windows over a trusted private LAN; the Windows process sends files to selected Wear OS watch using existing ADB `WatchTransferService`. This **requires Windows running** and is independent of ANCS/AMS direct Bluetooth Bridge.

Do not turn current `WebApplicationType.NONE` into a permanent public Spring Boot web service. During W13 evaluate a small isolated HTTP gateway (`watch-web` module if warranted), with a shared transfer coordinator extracted from `WatchWorkspace` so local files and browser-uploaded files cannot race or silently select a different watch. Existing `PhotoFiles` validation, copy-vs-scan results and retries remain the **single** transfer implementation.

This browser mode does **not** expose arbitrary ADB commands, remote APK management or filesystem access. Start/stop, private network binding, short-lived authenticated sessions, CSRF/Host checks, upload/pixel limits, staging cleanup and HTTP plaintext/HTTPS acceptance are release blockers. [Detailed design](LOCAL_WEB_PORTAL.md); [security](SECURITY.md).
