# Product requirements

This file defines **observable user requirements**. Architecture choices belong in [ARCHITECTURE.md](ARCHITECTURE.md).

## Identity and scope

**WristPorter is an independent local-first ecosystem for people who own Wear OS watches and have moved from Android to iPhone.** Its mission is to restore useful watch functionality without giving up privacy or the already-working Windows tools. WristPorter is not an official Galaxy Wearable replacement and cannot initially configure unsupported iPhone/watch pairing.

The ecosystem has two complementary pillars: **WristPorter Bridge** (planned Kotlin Wear OS app: direct iPhone ↔ watch Bluetooth ANCS/AMS, without Windows or an iOS app) and **WristPorter Desktop** (shipped Windows app: local ADB photos; planned local browser upload for iPhone Safari; planned APK install/updates and diagnostics). Each part remains independently usable. **As of v1.1.2, only Windows → Wear OS JPEG/PNG transfer is shipped.**

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
- For supported JPEG/PNG transfers, preserve the **original image file bytes**: no compression, resizing, recompression, re-encoding or metadata stripping. Thumbnails/previews are display-only and must not replace transfer payloads. Changing the destination filename is allowed; changing image contents is not. Preserve source resolution and quality. This requirement also applies to future Windows-mediated browser transfers, subject to verifying what Safari actually supplies.
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


## Planned ecosystem experience (NOT SHIPPED)

A user who moved from Android to iPhone wants useful Wear OS watch functions without an installed iOS companion:

- **Watch:** optional WristPorter Bridge connects directly to iPhone through Apple ANCS/AMS for shared notifications and supported media controls. Windows is not required after provisioning.
- **Windows:** one WristPorter Desktop portable application contains the existing photo transfer plus a future **on-demand LAN Web Upload**, optional APK installation/updates and diagnostics.
- **iPhone browser:** Safari opens the **Windows-hosted local page** from QR/URL over private Wi-Fi, authorizes a temporary session, selects photos, and sends them through Windows → ADB → a selected watch. **The PC must remain on and reachable.** This is not direct iPhone-to-watch Bluetooth image transfer and no native iOS app is installed.
- **Future research only:** a watch-hosted local web uploader might remove Windows dependency, but requires a separate watch battery/security/protocol feasibility decision.

**Web Upload v1 acceptance:** same Windows package, manually enabled local listener, mobile selection and upload of JPG/PNG, short-lived authorization, safe file validation and size/pixel quotas, temporary file cleanup, selected-watch-bound transfer through the existing engine, observable progress and scan-request results, Desktop-only regression PASS.

**LAN security:** local-only does not mean encrypted. An HTTP session transmits images/credentials in plaintext. Require a clear trusted-private-network restriction and documented TLS strategy/risk acceptance before public release; no anonymous access, global/untrusted network binding, remote ADB admin, auto-start or Internet/cloud relay.

**Detailed ownership:** [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md) for implementation design; [ROADMAP.md](ROADMAP.md) W13–W16; [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md) for one-Windows-app decision; [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md) for independent direct Bluetooth features. HEIC/HEIF and Live Photos are planned investigations, not automatically supported by current JPEG/PNG engine.
