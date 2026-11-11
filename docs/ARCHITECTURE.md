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
