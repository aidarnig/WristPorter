# Product requirements

This file defines **observable user requirements**. Architecture choices belong in [ARCHITECTURE.md](ARCHITECTURE.md).

## Identity and scope

**WristPorter** is a local desktop companion for transferring files to compatible smartwatches. The initial release handles JPEG and PNG photos; additional file types require explicit product design and validation. The app is independent of Samsung and Google.

## Primary scenario

A Windows user with an already-configured Galaxy Watch 4 Classic, without an Android smartphone nearby, selects photos and sends them directly to Samsung Gallery.

## v1 acceptance criteria

- Pair and connect from the UI, using the appropriate pairing and debugging ports.
- Import 100+ local JPEG/PNG images without freezing the interface.
- Report per-photo copying and media-scan request outcomes independently.
- Retry failed copies and scan requests without duplicating successfully uploaded photos.
- Verify transferred PNG/JPEG visibility in Gallery on real Watch 4 Classic hardware.
- Never modify source photos or intentionally overwrite existing images on the watch.
- Distribute a Windows 10/11 x64 **portable ZIP** containing `WristPorter.exe`, Java Runtime, and working ADB binaries with required DLLs and license notices. A user only extracts and launches; no installer, Java setup, or Android SDK setup.
- Permit an explicit custom ADB override for advanced users.

## v1 boundaries

No cloud service, accounts, telemetry, Android companion app, destructive remote file management or automatic background transfers. Other Wear OS models are best-effort until verified.

Successful ADB commands or media-scan broadcasts do **not** prove that Samsung Gallery displayed an image.
