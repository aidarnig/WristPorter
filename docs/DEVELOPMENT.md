# Development workflow

This document owns **local builds, contribution rules and portable packaging instructions**. Requirements and test gates live in [PRODUCT.md](PRODUCT.md) and [QUALITY.md](QUALITY.md).

## Prerequisites for developers

- JDK 25, Maven 3.9+
- Windows 10/11 x64 for packaging and manual UI testing
- ADB for development-time watch testing (the packaged ZIP supplies ADB automatically)

## Build from source

```bash
mvn -B -ntp verify
mvn -B -ntp -pl watch-domain -am test
mvn -B -ntp -pl watch-desktop -am install
mvn -pl watch-desktop javafx:run
```

Install the reactor before executing the desktop module directly.

## Portable build

On Windows, run `./ci/package-windows.ps1` in PowerShell from the project root. It runs full verification, builds the `jpackage` app-image, fetches a pinned and checksum-verified ADB distribution, and produces:

```text
dist/
  WristPorter-0.1.0-windows-x64.zip
  WristPorter-0.1.0-windows-x64.zip.sha256
```

The GitHub [Windows portable workflow](../.github/workflows/windows-portable.yml) does the same. It runs manually or when packaging scripts change; ordinary Java edits do not trigger an expensive Windows build.

To use the ZIP: extract **the entire folder** and run `WristPorter/WristPorter.exe`. Keep `runtime/`, `app/`, and `platform-tools/` alongside it; moving only the EXE will not work. No administrative installation or separate ADB download is required for the packaged version.

ADB can be overridden through the advanced UI field or `WRISTPORTER_ADB` environment variable (`WGS_ADB` is an older compatibility alias). Development fallback searches `~/Documents/platform-tools` and PATH. The bundled ADB is downloaded only by the *packager*, never by the app at startup.

## Collaboration

Use focused `feat/<scope>` or `fix/<scope>` branches and Conventional Commit-style messages. Track acceptance in GitHub Issues; update only the document that owns the relevant decision. Do not commit photographs, pairing secrets, ADB keys, build artifacts or private device identifiers.

There is currently **no v1.0 release**: real-watch and clean-Windows UI acceptance remain outstanding.
