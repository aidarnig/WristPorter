# Quality and release verification

This file owns test strategy and release gates. CI trigger and cost policy live in [CI.md](CI.md).

## Automated checks

- Domain: JPEG/PNG validation, safe names, folder import
- ADB: parsing, endpoint validation, subprocess failure handling and pairing code stdin
- Core: transfer states, partial failures, cancellation, scan-only retries
- CI: fake Maven process verifies affected-module routing without building irrelevant modules

Fake-ADB integration tests exercise the transport without touching a real watch.

## Portable acceptance

The manual Windows packaging pipeline must: run full Maven verification, create the Windows app-image, validate bundled runtime/app directories, verify that `adb.exe` and its two Windows DLLs exist, execute `adb version`, retain third-party notices, and generate a SHA-256 for the finished ZIP.

A successful CI package **does not** verify UI launch after extraction. Before a public release, test on a clean Windows 10/11 x64 system without Java, Android Studio, or administrator privileges; confirm that the EXE launches, bundled ADB works, and connecting to a watch is possible.

## Hardware acceptance

Reference hardware: Galaxy Watch 4 Classic. Verify initial pairing, reconnect, Cyrillic paths, batch of 100 photos, cancellation, retry, Gallery display after scan request, missing network and full storage.

## Manual verification log

### 2026-11-11 — Galaxy Watch 4 Classic, WristPorter 0.1.0 Preview #3

**Status: PASS for the observed batch-transfer scenario.**

The user tested the portable Windows ZIP published as [v0.1.0-preview.3](https://github.com/aidarnig/WristPorter/releases/tag/v0.1.0-preview.3) on a Windows desktop with a Samsung Galaxy Watch 4 Classic over wireless ADB.

Observed and confirmed in screenshots and user feedback:

- The extracted desktop application launched successfully.
- The bundled ADB executable was detected automatically, without a separate Android SDK installation.
- The device appeared in the ADB discovery control.
- Both light and dark themes rendered.
- Eight PNG images were imported with preview thumbnails.
- The transfer completed **8 of 8** with **zero reported failures**.
- Each file's status indicated that copying succeeded and media indexing was requested.
- The user confirmed that the photos became visible in the watch's Samsung Gallery **immediately**, without manual commands.

**Evidence:** user-provided screenshots and confirmation in the project conversation. The screenshots are not committed to this public repository because they include personal desktop content and local device information.

**Not yet covered by this test:** initial pairing using the application's pairing form, clean Windows without developer tools or an existing ADB environment, 100-photo batches, recovery from Wi-Fi disconnects, cancellations, retries after induced failures, full storage, Windows 10, or independent tests on other Wear OS watches.

## Release gate

Early **pre-releases** may be published after the Windows CI builds the portable ZIP and verifies its checksum, provided they clearly disclose missing real-device/GUI verification. Publish **stable v1.0** only after green full reactor, packaging validation, clean Windows smoke test, real-watch acceptance, license review, and matching checksums. Do not claim compatibility with all Wear OS devices based on one hardware model.

**Current state:** full reactor and portable ZIP construction passed GitHub Windows CI, and the 8-image real-watch acceptance scenario above passed. The remaining release-gate scenarios still require verification.
