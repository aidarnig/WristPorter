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

## Development across separate chats (new program)

Every new coding chat/agent must first read repository-root [AGENTS.md](../AGENTS.md), then [STATE.md](STATE.md), [ROADMAP.md](ROADMAP.md), [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md), [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md) and the actual GitHub Issue/PR. The normative document map is [INDEX.md](INDEX.md); substantive design changes use [ADRs](adr/README.md).

**WristPorter Bridge is planned, not yet built.** New Kotlin/Gradle Wear OS tooling, watch app commands and separate CI workflow will be documented here *once implemented in W1*. Until then this page's JDK 25/Maven instructions apply only to Desktop. No Android SDK/Gradle dependency may be added to the desktop-only Maven build.
