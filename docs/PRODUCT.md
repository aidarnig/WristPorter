# Product requirements

This file defines **observable user requirements**. Architecture choices belong in [ARCHITECTURE.md](ARCHITECTURE.md).

## Identity and scope

**WristPorter** is a local desktop companion for transferring files to compatible smartwatches. The initial release handles JPEG and PNG photos; additional file types require explicit product design and validation. The app is independent of Samsung and Google.

## Primary scenario

A Windows user with an already-configured Galaxy Watch4 Classic wants pictures in Samsung Gallery after moving from an Android/Samsung phone to an iPhone. They select JPEG/PNG on the PC and transfer it to the watch over Wi-Fi without relying on a phone for the transfer. WristPorter does not pair the watch with an iPhone or replace initial watch setup.

Wear OS support beyond the reference device is a testable possibility, **not** a confirmed promise; keep the [compatibility matrix](COMPATIBILITY.md) updated only with verified device results.

## v1 acceptance criteria

- Pair and connect from the UI, using the appropriate pairing and debugging ports.
- Import and send multiple local JPEG/PNG images without freezing the interface (eight-image batch verified on reference hardware).
- Report per-photo copying and media-scan request outcomes independently.
- Retry failed copies and scan requests without duplicating successfully uploaded photos.
- Verify transferred PNG/JPEG visibility in Gallery on real Watch 4 Classic hardware.
- Never modify source photos or intentionally overwrite existing images on the watch.
- Distribute a Windows 10/11 x64 **portable ZIP** containing `WristPorter.exe`, Java Runtime, and working ADB binaries with required DLLs and license notices. A user only extracts and launches; no installer, Java setup, or Android SDK setup.
- Permit an explicit custom ADB override for advanced users.

## v1 boundaries

No cloud service, accounts, telemetry, Android companion app, destructive remote file management or automatic background transfers. Other Wear OS models are best-effort until verified.

Successful ADB commands or media-scan broadcasts do **not** prove that Samsung Gallery displayed an image.

## Verified launch target

Initial stable scope is Windows x64 and Galaxy Watch 4 Classic over a local Wi-Fi ADB connection. Other devices, unusually large batches and transient network errors are supported on a best-effort basis and are not claims of hardware certification.

## Planned expansion: WristPorter Bridge (not part of Desktop v1.1.2)

A user with an **already configured Wear OS watch** wants iPhone notifications and iPhone media controls on the watch **without installing any iOS application**. We plan an independent Wear OS APK using built-in Bluetooth ANCS/AMS services, plus optional device-specific GATT capabilities verified on hardware. The existing Windows photo transfer remains standalone and must not require the bridge or the iPhone.

**Desired behavior:** explicit pairing/permissions; receive, update and remove ANCS notifications; show safely retrieved app/title/content; execute only iOS-advertised actions; display and control AMS music only when the active player supports it; provide quiet hours, filtering, reconnection, user-facing diagnostics, accessible RU/EN watch UI. Battery, security and lifecycle reliability are mandatory quality outcomes.

**Boundaries:** no iOS companion, iPhone-to-watch photo transfer, contacts/Health sync, arbitrary text replies, remote call audio, Galaxy Wearable replacement, factory-reset watch setup, cloud accounts or Internet relay in this program. Optional battery/device/time services require device discovery, not assumption.

**Acceptance:** evidence from an actual Galaxy Watch4 Classic/iPhone pairing before calling BLE features supported. No Bridge features have yet been implemented or hardware-verified in WristPorter. Detailed [feature matrix](BRIDGE_CAPABILITIES.md), [W0–W12 roadmap](ROADMAP.md), [device verification ledger](VERIFICATION.md) and [ADR-0001](adr/0001-independent-wearos-bridge.md).
