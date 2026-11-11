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

## Release gate

Only publish v1.0 after green full reactor, packaging validation, clean Windows smoke test, real-watch acceptance, license review, and matching checksums. Do not claim compatibility with all Wear OS devices based on one hardware model.

**Current state:** full reactor and portable ZIP construction have passed a GitHub Windows runner; extracted-app GUI and physical-watch acceptance are still pending.
