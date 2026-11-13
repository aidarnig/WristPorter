# WristPorter W0 — isolated BLE feasibility probe

**EXPERIMENTAL / NOT DEVICE-VERIFIED.** Owned by [Issue #13](https://github.com/aidarnig/WristPorter/issues/13). The Android project in this directory is not an installed/shipped WristPorter Bridge and does not modify the Windows Maven reactor.

## Probe behavior

- Show Android release, firmware and actual watch API level locally, without writing identifiers to logs.
- User requests the Android system's discoverability dialog for at most 120 seconds, and initiates pairing in **iPhone Settings → Bluetooth**, verifying matching codes. Discovery through iPhone settings is a **hypothesis to test**, not a guarantee.
- Show bonded-device names **only on watch**; explicit selection and confirmation of the chosen iPhone are mandatory.
- Use the selected bonded device for a one-shot LE GATT client connection, inspect service UUIDs: ANCS, AMS, optional Battery/Device Information/Current Time. No address/name/identifier is logged.
- Subscribe (if exposed and permitted) to ANCS Notification Source and AMS Remote Command / Entity Update; register AMS Player/Track attribute monitoring. Show only sanitized notification type/category and AMS command count / entity/attribute event indicators. **Do not capture message text, UID or media metadata values.**
- Manual Disconnect closes GATT and stops pending operations. Activity destruction closes the connection. No background service, automatic reconnect, continuous scan, Internet permission, cloud, account or stored phone data.
- This app has no iPhone media control commands, ANCS attribute fetching or mature lifecycle management. It does **not** prove successful day-to-day Bridge behavior or battery life.

## Build

Use JDK 17+, Android SDK 35 and Build Tools 35.0.0, Gradle 8.11.1 (installed by CI). No generated Gradle wrapper binary is committed.

From experiments/w0-ble-probe:

    gradle :app:testDebugUnitTest :app:assembleDebug

On a Watch4 already configured for Wi-Fi ADB, install the built debug APK using the user's explicitly selected ADB device:

    adb -s YOUR_WATCH_DEVICE install -r app/build/outputs/apk/debug/app-debug.apk

Turn off wireless debugging after installation. Do not reset, unpair or wipe the watch.

## Real watch/iPhone test instructions

1. Record watch model, Android API and firmware (probe UI or adb shell getprop ro.build.version.sdk); record iPhone model, iOS version and Bluetooth state separately. Never publish serial, MAC, pairing codes or private notification/media contents.
2. Grant Nearby devices on watch. Tap **Discoverable (120s)** and approve the OS dialog. On iPhone open **Settings → Bluetooth** and select the watch **if present**, with matching confirmation on both devices. If absent, record the exact failure (including whether watch was visibly discoverable). Do not assume a universal incompatibility from one attempt.
3. If offered in iPhone Bluetooth device settings, enable **Share System Notifications**. On the watch choose and confirm the actual bonded iPhone; start one explicit GATT connection. Record connect/discovery statuses, ANCS/AMS presence and subscribe results.
4. Trigger one fresh non-sensitive iPhone notification and one media playback/track change while the probe is open. Only sanitized event indicators count. A successful Android build is **not** a device PASS.
5. Disconnect manually; toggle Bluetooth off/on and attempt user-initiated reconnect. Record concrete failures and whether discoverability expires. Screen-off and background operation remain untested by this foreground-only probe.
6. Fill the existing [W0 verification ledger](../../docs/VERIFICATION.md) scenario-by-scenario (BLE-00…BLE-10), with actual UTC date, watch/firmware/API, iPhone/iOS, build SHA, outcome and reproduction steps. Use NOT TESTED, BLOCKED, INCONCLUSIVE, FAIL or PASS precisely.

If GATT is unavailable, distinguish no system pairing, bond present but GATT failure, no ANCS/AMS, authorization denied and a subscription failure before choosing next steps.

References: [Apple ANCS](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleNotificationCenterServiceSpecification/Specification/Specification.html), [Apple AMS](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleMediaService_Reference/Specification/Specification.html), [Android Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions).
