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

This is an **unverified portable prototype**, not a Windows installer or a stable release. The non-modular JavaFX app uses a plain-Java `DesktopLauncher` entry point.
