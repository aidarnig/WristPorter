# WristPorter

**WristPorter** is an open-source Java desktop application for transferring photos directly from Windows to Samsung Galaxy Watch 4 Classic (and potentially other Wear OS watches) via wireless ADB, without an Android smartphone or Galaxy Wearable.

**Tagline:** Your files, one wrist away.

> **Early development / 0.1.0-SNAPSHOT:** not yet a hardware-verified portable release. The repository URL retains its original slug until it can be renamed.

## Current capabilities

- Native JavaFX UI, AtlantaFX dark theme, Spring Boot in non-web mode.
- Wireless ADB pairing by IP, pairing port and six-digit code.
- ADB connection via the **different debugging port**; device discovery.
- Add multiple JPEG/PNG photos via file picker, folder picker and drag-and-drop.
- Sequential ADB copy with unique filenames, followed by a media scanner broadcast for each successful copy.
- Live progress and per-file statuses. A successful scan request **does not guarantee** Gallery rendered the image.

## Run from sources

Requires JDK 25, Maven 3.9+ and Google's [ADB Platform-Tools](https://developer.android.com/tools/releases/platform-tools).

```bash
mvn -B -ntp verify
mvn -B -ntp -pl watch-desktop -am install
mvn -pl watch-desktop javafx:run
```

The Windows portable ZIP will include ADB inside the application folder, so separate installation is unnecessary. The UI can still select a custom `adb.exe`; `WRISTPORTER_ADB` overrides the default path and legacy `WGS_ADB` remains supported.

## Documentation — one file per responsibility

- [Product](docs/PRODUCT.md): functional scope and acceptance criteria.
- [Architecture](docs/ARCHITECTURE.md): modules, ADB protocol and security decisions.
- [UX](docs/UX.md): screens, visuals and interaction.
- [Roadmap](docs/ROADMAP.md): milestone sequence.
- [Quality](docs/QUALITY.md): test strategy and release gates.
- [Development](docs/DEVELOPMENT.md): setup, commands and contribution workflow.
- [Windows portable build](.github/workflows/windows-portable.yml): manually triggered packaging workflow (prototype only); usage details are in [Development](docs/DEVELOPMENT.md).
- [CI](docs/CI.md): affected-module test policy and build cost.
- [Issues](https://github.com/aidarnig/watch-gallery-sync/issues): individual tasks and implementation progress.

## Privacy

No accounts, cloud service or telemetry. Photos remain between the PC and selected watch on the local wireless ADB connection. Turn off wireless debugging when finished.

Licensed under [MIT](LICENSE).
