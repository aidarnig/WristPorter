# Development workflow

This file owns **local setup and collaboration conventions**.

## Requirements

JDK 25, Maven 3.9+, Android SDK Platform-Tools for watch testing; Windows 10/11 for final packaging verification.

## Useful commands

```bash
mvn -B -ntp verify
mvn -B -ntp -pl watch-domain -am test
mvn -B -ntp -pl watch-desktop -am install
mvn -pl watch-desktop javafx:run
```

Build/install the reactor before `javafx:run` when running only the desktop module so local module dependencies resolve. The app checks the PATH and `~/Documents/platform-tools` for ADB and accepts `WGS_ADB` as an override.

## Collaboration

Use branches named `feat/<scope>` or `fix/<scope>`, focused PRs, and Conventional Commit-style messages. Link issues to changes. Edit **only the document owning the changed decision**, and link from another document rather than copying sections.

Do not commit personal photos, pairing secrets, private ADB keys, binaries, IDE artifacts or build output.

JavaFX state updates must happen on the FX Application Thread, while ADB work runs in a background executor.

## Manual Windows portable build

The separately triggered `Windows portable build (manual)` workflow runs the full reactor and packages a Windows app image using `jpackage`. It does not run on ordinary push/PR CI.

On Windows with JDK 25 and Maven in PATH, run `./ci/package-windows.ps1` in PowerShell. The output is `dist/WatchGallerySync-windows-x64.zip` and a SHA-256 checksum. Extract the whole directory and start `WatchGallerySync.exe`. Java is bundled; Android Platform-Tools remain external.

The **portable ZIP is the primary release format**; no MSI/EXE installer is planned for v1. After a successful manual GitHub Actions run, open its artifact, download the ZIP, extract the complete `WatchGallerySync` folder (including `runtime/` and `app/`), and launch `WatchGallerySync.exe`. Do not launch the exe from inside a ZIP viewer or move it out of its directory. The non-modular JavaFX app uses a plain-Java `DesktopLauncher` entry point.

**Current limitation:** the portable ZIP bundles Java but **not** Android Platform-Tools. ADB must be available via `WGS_ADB`, the program's ADB selector, `~/Documents/platform-tools`, or PATH. A copy of Platform-Tools can be kept in a user directory without running an installer. Packaging on Windows and actual clean-machine/Watch 4 operation are still unverified; do not describe this as a published release.
