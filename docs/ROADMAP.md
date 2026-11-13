# WristPorter roadmap

This document defines **milestone order**. Concrete tasks and their progress are tracked in [GitHub Issues](https://github.com/aidarnig/WristPorter/issues), not duplicated here.

| Milestone | Outcome |
| --- | --- |
| M0: Foundation | Maven modules, JavaFX + Spring Boot startup, change-aware CI |
| M1: Connection | Discover ADB, pair, select and connect to watches |
| M2: Import | Select files/folders, thumbnails and drag-and-drop |
| M3: Transfer | Batch push, Gallery scan, retries and cancellation |
| M4: UX | Themes, localization, preferences and accessible messaging |
| M5: QA | Simulated ADB tests, real-watch checks, clean Windows testing |
| M6: 1.0 | Verified Windows x64 portable ZIP with bundled ADB, notices, checksum and tagged release |

Only a hardware-validated build can be called 1.0. Other Wear OS devices remain best-effort until independently tested.

## Presentation and community feedback (after 1.1.2)

- [x] Present the Windows → Wear OS Gallery use case clearly, including the Android-to-iPhone switch scenario.
- [x] Separate verified Galaxy Watch4 Classic behavior from potential compatibility with other Wear OS watches; collect reports through an Issue form.
- [x] Keep release-facing copy short and readable; leave technical evidence in the quality log.
- [ ] Record and publish a Windows-to-watch Gallery video or GIF from real hardware — tracked in [issue #10](https://github.com/aidarnig/WristPorter/issues/10). This is intentionally deferred, not a documentation-release blocker.

