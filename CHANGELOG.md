# WristPorter releases

## 1.1.0 — Branding and bilingual interface

- Added the selected WP-on-watch identity with full wordmark adapting to light and dark mode.
- Added the watch-only icon to the native Windows EXE and application window.
- Added an **English / Русский** switch in the application header, saved between launches.
- Translated pairing, connection, photo selection, gallery status, batch progress and error messages.
- Added bilingual README pages and localized, privacy-safe interface illustrations.
- Prepared a GitHub Social Preview image at 1280×640 and a reproducible asset-generation workflow.
- No changes to the core ADB push / media scanner protocol verified on Galaxy Watch 4 Classic.

**Installation:** Under [Releases](https://github.com/aidarnig/WristPorter/releases), download `WristPorter-1.1.0-windows-x64.zip` from **Assets**. Extract the entire folder and launch `WristPorter.exe`; Java and ADB are already included. The separate `.zip.sha256` file allows integrity verification.

**Validation:** The 1.0 photo-transfer scenario was confirmed on Galaxy Watch 4 Classic, including in-app pairing and immediate Gallery visibility for 8/8 photos. Version 1.1.0 additionally requires green Java tests and Windows portable packaging checks. New localization and branding have not yet been independently tested on the user's PC; other watch models are not certified.

## 1.0.0 — First stable portable release

- JavaFX desktop interface, bundled Java Runtime and ADB.
- Wireless ADB pairing, discovery, selection and reconnect.
- Multi-photo JPEG/PNG transfer and automatic Samsung Gallery scanning.
- Batch progress, cancel, retry, light/dark themes and responsive thumbnail grid.
- Real device acceptance: Galaxy Watch 4 Classic, eight photos successfully transferred.

