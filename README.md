# WristPorter — Photo transfer for Wear OS watches

[![Windows release](https://img.shields.io/github/v/release/aidarnig/WristPorter?label=Windows%20portable)](https://github.com/aidarnig/WristPorter/releases/latest) [![Build](https://github.com/aidarnig/WristPorter/actions/workflows/ci.yml/badge.svg)](https://github.com/aidarnig/WristPorter/actions/workflows/ci.yml) [![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[**English**](README.md) · [Русский](README.ru.md)

![WristPorter banner](docs/assets/social-preview.png)

**Wireless ADB photo transfer from Windows to Wear OS smartwatches — no Android phone required after initial watch setup.** WristPorter is an independent open-source JavaFX desktop utility for Windows and compatible Wear OS watches. The reference device is Samsung Galaxy Watch 4 Classic.

## Download

**[Download the latest Windows portable ZIP from Releases](https://github.com/aidarnig/WristPorter/releases/latest)**

Choose `WristPorter-1.1.2-windows-x64.zip` under **Assets** — not the auto-generated **Source code** ZIP. Extract the complete archive and double-click `WristPorter.exe` inside the `WristPorter` directory. Java, JavaFX and ADB (including Windows DLLs) are already bundled. No installer, phone companion or Android Studio is needed after the watch has been set up.

## What it does

- Pair and connect to your watch via wireless ADB, with automatic watch discovery.
- Select JPEG/PNG files, entire folders, or drag and drop photos onto the queue.
- Preview thumbnails in an adaptive multi-column gallery.
- Transfer batches with a prominent progress bar and per-photo results; automatically ask Samsung Gallery to scan copied files.
- Retry failed copies and rescan already copied photos without duplicating uploads.
- Switch **English / Русский** and **light / dark** appearance without restarting. Both preferences are saved.

## Real screenshot

![WristPorter English interface — nine photos transferred successfully](docs/assets/ui-screenshot-en.webp)

*WristPorter 1.1.2 on Windows, English interface: **9 of 9 images sent, 0 errors**. Actual screenshot with the local watch address redacted.*

## How it works

1. Enable Developer options and **Wireless debugging** on the watch.
2. Pair WristPorter using the displayed **pairing port** and six-digit code.
3. Connect using the **different debugging port** shown on the watch.
4. Select or drag JPEG/PNG images into the queue and click **Send to watch**.
5. Open Samsung Gallery; copied images are automatically submitted for media indexing.

For the detailed pairing steps, [see the Quick Start guide](docs/QUICK_START.md).

Turn **Wireless debugging** off after transferring your photos.

> **SmartScreen:** the current Windows EXE is unsigned by a trusted publisher. Verify the download source and SHA-256. [Details](docs/SECURITY.md).

## Development

Requires JDK 25 and Maven 3.9+ to build from source:

```bash
mvn -B -ntp verify
mvn -B -ntp -pl watch-desktop -am install
mvn -pl watch-desktop javafx:run
```

Technical decisions: [Architecture](docs/ARCHITECTURE.md) · [UX](docs/UX.md) · [Quality](docs/QUALITY.md) · [Development](docs/DEVELOPMENT.md) · [CI](docs/CI.md) · [Roadmap](docs/ROADMAP.md) · [Branding](docs/BRANDING.md) · [Issues](https://github.com/aidarnig/WristPorter/issues).

MIT licensed. Not affiliated with Samsung or Google. The user-confirmed Galaxy Watch 4 Classic photo-transfer scenario is supported; other watches and network conditions have not all been verified.
