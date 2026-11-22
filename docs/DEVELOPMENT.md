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

## Test artifacts for owner-assisted checks

The **Windows preview artifact** workflow runs on eligible Desktop PR changes and can be manually dispatched on a selected branch. Download the artifact named "wristporter-windows-preview-<run number>" from the successful Actions run. It contains a portable ZIP, SHA-256 checksum and PREVIEW-READ-ME.txt explaining that this is **not a release**, even if the internal app version still says 1.1.2. Extract the complete ZIP and test only the scenario requested by the executor. Stable releases are published separately by windows-portable.yml.

For an APK from a trusted, successful W0 or future Wear OS Actions build, use the optional PowerShell test installer in a checkout of this repository:

    .\ci\install-watch-test-apk.ps1 -ApkPath "C:\path\to\app-debug.apk" -AdbPath "C:\path\to\platform-tools\adb.exe"

The script checks authorized ADB devices, requires one selected watch (or an explicit -Serial) and asks to type INSTALL. It never factory-resets, unpairs or silently installs apps. Do not run it against an unidentified device. The executor is responsible for giving the exact build, download and test/rollback instructions and recording the observed result in the same chat; see [engineering standard](ENGINEERING_STANDARD.md).

## Collaboration

Use focused `feat/<scope>` or `fix/<scope>` branches and Conventional Commit-style messages. Track acceptance in GitHub Issues; update only the document that owns the relevant decision. Do not commit photographs, pairing secrets, ADB keys, build artifacts or private device identifiers.

The 1.1.2 package has the accepted Watch 4 Classic scenario but is not certified on other watches or network conditions. See the release notes and [Quality](QUALITY.md) for verification boundaries.

## Public-facing release notes

Keep technical changes in [CHANGELOG.md](../CHANGELOG.md). The short description for the current release is [docs/RELEASE_NOTES.md](RELEASE_NOTES.md), which is intended for the GitHub Releases page. For new releases, update the user-facing notes, version, release tag and asset names together. The Windows publication workflow uses the user-facing release-notes file rather than pasting the full multi-version changelog. Updating an existing release description (such as v1.1.2) still requires editing that release on GitHub: file changes only affect **future** published releases. Before each new release, update the release notes and the workflow's RELEASE_TAG, ZIP filename and version together.

The repository's About metadata and Social Preview are managed in GitHub Settings, not by changing README files. See [Branding](BRANDING.md) and [Sharing guide](OUTREACH.md).

## Separate executor chats

Delegate an executor by **Issue URL or number only**. The unique mandatory read order, autonomous safe-merge rules and documentation-writing procedure are in [AGENTS.md](../AGENTS.md) and [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md); this development guide does not duplicate them.

## Wear OS app shell (W1-A, debug-only)

`wearos/` is a separate Kotlin/Gradle Android build, NOT part of the JDK 25/Maven reactor. Use JDK **17**, Android SDK Platform **36** and Build Tools **36.0.0**, Gradle **8.13**, AGP **8.13.2** and Kotlin/Compose Compiler plugin **2.3.21** (pinned in the Gradle files). Gradle 8.13 must be installed or provisioned explicitly; this initial shell does not bundle a Gradle wrapper.

```bash
cd wearos
gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK is `wearos/app/build/outputs/apk/debug/app-debug.apk`, package `io.github.aidarnig.wristporter.bridge`. It has no Bluetooth/network/notification permissions or working pairing controls. `minSdk=30` is a provisional build floor (API 30-35 untested), `targetSdk=36` and `compileSdk=36` are independent choices. The test-only APK is uploaded by [Wear OS CI](../.github/workflows/wearos.yml), never tagged or published as a release. Use [the explicit selected-watch installer](../ci/install-watch-test-apk.ps1) only if an owner-assisted packaging check is actually warranted. Uninstall safely with `adb -s <selected-watch-serial> uninstall io.github.aidarnig.wristporter.bridge` after explicit device confirmation.


## Future single-app Web Upload mode — not yet implemented

A new on-demand Windows-hosted HTTP frontend for iPhone Safari is planned **inside the existing portable Windows app**, not in a separately distributed Windows server or installed iOS app. The first goal is JPG/PNG upload to the existing WatchTransferService via ADB after the user starts a private-network share session. Keep `WebApplicationType.NONE` on ordinary desktop startup. If W13 chooses a `watch-web` module, document its Maven/packaging wiring and real test commands here only after it exists.

Follow [W13–W16 roadmap](ROADMAP.md), [design](LOCAL_WEB_PORTAL.md) and [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md). Later signed releases need Desktop-only regression, Web Upload browser tests and explicit LAN transport security policy. No code / web server exists for this mode today.
