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
  WristPorter-1.1.2-windows-x64.zip
  WristPorter-1.1.2-windows-x64.zip.sha256
```

The GitHub [Windows portable workflow](../.github/workflows/windows-portable.yml) does the same. It runs manually or when packaging scripts change; ordinary Java edits do not trigger an expensive Windows build. After successful packaging, a lightweight Ubuntu job verifies SHA-256 and publishes the versioned ZIP at [GitHub Releases](https://github.com/aidarnig/WristPorter/releases). A stable channel publishes `v1.1.2`; future preview builds can use distinct prerelease tags. Release decisions and supported claims follow [Quality](QUALITY.md).

To use the ZIP: extract **the entire folder** and run `WristPorter/WristPorter.exe`. Keep `runtime/`, `app/`, and `platform-tools/` alongside it; moving only the EXE will not work. The portable folder contains both QUICK_START.md (English) and QUICK_START.ru.md (Russian), indexed from START_HERE.txt. No administrative installation or separate ADB download is required for the packaged version.

ADB can be overridden through the advanced UI field or `WRISTPORTER_ADB` environment variable (`WGS_ADB` is an older compatibility alias). Development fallback searches `~/Documents/platform-tools` and PATH. The bundled ADB is downloaded only by the *packager*, never by the app at startup.

## Collaboration

Use focused `feat/<scope>` or `fix/<scope>` branches and Conventional Commit-style messages. Track acceptance in GitHub Issues; update only the document that owns the relevant decision. Do not commit photographs, pairing secrets, ADB keys, build artifacts or private device identifiers.

The 1.1.2 package has the accepted Watch 4 Classic scenario but is not certified on other watches or network conditions. See the release notes and [Quality](QUALITY.md) for verification boundaries.

## Public-facing release notes

Keep technical changes in [CHANGELOG.md](../CHANGELOG.md). The short description for the current release is [docs/RELEASE_NOTES.md](RELEASE_NOTES.md), which is intended for the GitHub Releases page. For new releases, update the user-facing notes, version, release tag and asset names together. The Windows publication workflow uses the user-facing release-notes file rather than pasting the full multi-version changelog. Updating an existing release description (such as v1.1.2) still requires editing that release on GitHub: file changes only affect **future** published releases. Before each new release, update the release notes and the workflow's RELEASE_TAG, ZIP filename and version together.

The repository's About metadata and Social Preview are managed in GitHub Settings, not by changing README files. See [Branding](BRANDING.md) and [Sharing guide](OUTREACH.md).

## Separate executor chats

Delegate an executor by **Issue URL or number only**. The unique mandatory read order, autonomous safe-merge rules and documentation-writing procedure are in [AGENTS.md](../AGENTS.md) and [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md); this development guide does not duplicate them.

**WristPorter Bridge is planned, not yet built.** Add Kotlin/Gradle Wear OS build instructions and isolated CI here *once W1 creates them*. Until then the JDK 25/Maven commands above apply only to Desktop. Do not add Android SDK/Gradle dependencies to the Desktop Maven build.

## Future single-app Web Upload mode — not yet implemented

A new on-demand Windows-hosted HTTP frontend for iPhone Safari is planned **inside the existing portable Windows app**, not in a separately distributed Windows server or installed iOS app. The first goal is JPG/PNG upload to the existing WatchTransferService via ADB after the user starts a private-network share session. Keep `WebApplicationType.NONE` on ordinary desktop startup. If W13 chooses a `watch-web` module, document its Maven/packaging wiring and real test commands here only after it exists.

Follow [W13–W16 roadmap](ROADMAP.md), [design](LOCAL_WEB_PORTAL.md) and [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md). Later signed releases need Desktop-only regression, Web Upload browser tests and explicit LAN transport security policy. No code / web server exists for this mode today.
