# Contributing to WristPorter

[**English**](CONTRIBUTING.md) · [Русский](CONTRIBUTING.ru.md)

Thanks for helping improve WristPorter. You don't have to be a developer: testing on a different Wear OS watch is especially useful.

## Test your watch

1. Read the [compatibility requirements](docs/COMPATIBILITY.md) and [Quick start](docs/QUICK_START.md).
2. Try pairing, transferring a small JPEG/PNG batch and opening the watch's Gallery.
3. [Submit a watch compatibility report](https://github.com/aidarnig/WristPorter/issues/new?template=watch-compatibility.yml) with the **exact model** and **separate results** for ADB transfer and Gallery visibility.

Please **never post** local IP addresses, serial numbers, pairing codes, ADB keys, private pictures or other personal data in public issues or screenshots.

## Bugs and suggestions

[Search existing issues](https://github.com/aidarnig/WristPorter/issues) before opening a new one. Include the app version, watch model, Windows version, steps to reproduce and what happened. For a feature idea, describe the problem you're trying to solve rather than only a proposed technical implementation.

## Code contributions

- Fork the repository, work on a focused branch and send a pull request.
- Build and test with JDK 25 and Maven 3.9+: see [Development](docs/DEVELOPMENT.md).
- Keep changes focused and add or update tests for behavior changes.
- Respect the [product boundaries](docs/PRODUCT.md) and [security decisions](docs/ARCHITECTURE.md). Do not add destructive watch operations, cloud dependencies or silent telemetry.
- For new features, update the one document that owns the changed requirement, architecture or UX decision. Don't commit watch/PC identifiers, user photos or generated builds.

This is an independent open-source project, not a Samsung or Google product.
