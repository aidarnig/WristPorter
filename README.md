# WristPorter

**Your files, one wrist away.**

WristPorter is an open-source Java desktop application that transfers photos from Windows directly to compatible Wear OS watches over local Wi-Fi ADB. It does not require a nearby Android phone once the watch is configured. The current feature set targets **JPEG/PNG** transfers to Samsung Gallery on the Galaxy Watch 4 Classic.

> **1.0.0 (Windows x64):** The core photo-transfer workflow was verified on Galaxy Watch 4 Classic. Other watch models and unusual network conditions remain best-effort.

## Portable Windows edition

**[Download WristPorter from GitHub Releases](https://github.com/aidarnig/WristPorter/releases)**. Open the latest stable release and, under **Assets**, download `WristPorter-1.0.0-windows-x64.zip` — **not** the automatically generated "Source code" archive. Extract it fully, then launch `WristPorter/WristPorter.exe`.

The application folder already includes the Java runtime and a verified `adb.exe` with its required DLL files. No installer, separate Java installation, or manual Android Platform-Tools setup is needed. The application supports manual ADB overrides for advanced users.

See [Development](docs/DEVELOPMENT.md) for build instructions; see [Quality](docs/QUALITY.md) for acceptance requirements before a public release.

## Current capabilities

- JavaFX + AtlantaFX light/dark UI, Spring Boot non-web services
- Wireless ADB pairing and device selection, without PowerShell
- Batch JPEG/PNG import via file picker, folders or drag-and-drop, with responsive thumbnail grid
- Prominent transfer progress, per-photo outcome status, and Samsung Gallery media scan requests
- Retry failed copies or re-scan already copied files, without duplicate upload
- No accounts, cloud storage or telemetry

## Documentation

Each document has a distinct responsibility:

- [Quick start](docs/QUICK_START.md): end-user pairing, photo transfer and troubleshooting
- [Product](docs/PRODUCT.md): supported user scenarios and acceptance criteria
- [Architecture](docs/ARCHITECTURE.md): modules, transport and dependency security
- [UX](docs/UX.md): visual and interaction behavior
- [Roadmap](docs/ROADMAP.md): milestone sequence
- [Quality](docs/QUALITY.md): testing and release gates
- [Development](docs/DEVELOPMENT.md): building, packaging and contributing
- [CI](docs/CI.md): affected-module verification and cost controls
- [GitHub Issues](https://github.com/aidarnig/WristPorter/issues): concrete implementation tasks

MIT licensed; see [LICENSE](LICENSE). This is an independent project, not affiliated with Samsung or Google.
