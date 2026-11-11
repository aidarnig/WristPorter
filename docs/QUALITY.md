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

Reference hardware: Galaxy Watch 4 Classic. The pairing/reconnect and eight-photo Gallery scenario were accepted on 2026-11-11. Cancellation, retry, Unicode paths and subprocess behavior are covered by automated checks where implemented. A 100-photo stress test, storage-full test, and Wi-Fi fault injection are recommended future regression tests, **not** 1.0 release blockers by product-owner decision.

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

**Follow-up confirmation from user:** Initial pairing **was performed successfully in the app**; after restarting it, **Discover devices** found the previously connected watch again. The screenshots also revealed UX issues (tiny progress/results, excess whitespace in single-column queue, overly wide advanced settings control). These findings motivated the 1.0 UI improvements.

**Still unverified manually:** clean Windows without developer tools, 100-photo stress batch, Wi-Fi disruption, cancellation/retry under induced failures, full storage, Windows 10 and other Wear OS hardware.

## Release gate

**Stable 1.0 release decision (2026-11-11):** The product owner accepts the verified portable Windows launch, successful in-app pairing/re-discovery and real Watch 4 Classic eight-photo transfer, plus passing automated tests for other key workflows. The separate 100-photo and induced-cancellation/retry tests are **not required** for 1.0. Stable publication requires a green full Maven reactor, successful Windows app-image packaging, verified ADB/Java runtime and SHA-256 release assets. Limit the compatibility claim to the tested Watch 4 Classic scenario and disclose unverified clean-PC/Windows 10 conditions. Third-party license/provenance files must remain bundled; additional legal/trademark review is tracked separately before broader promotion.

**Current state:** The real-watch reference scenario and in-app pairing/re-discovery passed; 1.0 UI improvements have passed selective Java CI. The Windows 1.0 package must pass full CI before its stable release is considered complete.
