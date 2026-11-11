# Quality strategy

This file is the single source for **tests and release gates**. Execution scheduling and module selection belong to [CI.md](CI.md).

## Automated tests

- Domain: supported types, unsafe input and unique remote filenames.
- ADB: parsing authorized/offline devices, command arguments, subprocess timeouts and secret handling.
- Core: progress callbacks, transfer ordering, batch partial failures and interruption. Fake-ADB regression tests ensure a scan-only retry never repeats a successful push.
- Desktop: non-blocking operations and UI state as test infrastructure becomes available.

Use a fake ADB executable for integration tests; real hardware must not be necessary for pull request CI.

## Manual acceptance

Reference device: Galaxy Watch 4 Classic with wireless debugging on a shared Wi-Fi network. Verify pairing, reconnect, 100-photo import, Gallery visibility, Cyrillic paths, missing ADB, disconnected watch, partial copy failures and insufficient storage.

## Release gate

Require green full Maven verification, Windows installer launched on a clean PC without JDK, successful real-watch tests, no known data-loss bugs and checksums for release artifacts. Do not publish v1.0 based only on mocked integration tests.

**Current state:** source baseline is committed; successful complete build, packaging and hardware acceptance still need verification.
