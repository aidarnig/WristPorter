# WristPorter W0 — isolated BLE feasibility probe

**EXPERIMENTAL / PARTIAL DEVICE FEASIBILITY VERIFIED, NOT A STABLE BRIDGE.** Owner confirmed Watch4 ↔ iPhone ANCS/AMS events on 2026-11-22; Bluetooth OFF/ON exposed a reproducible *loss of media events despite GATT status=0*, root cause not yet established. Owned by [Issue #13](https://github.com/aidarnig/WristPorter/issues/13). The Android project in this directory is not an installed/shipped WristPorter Bridge and does not modify the Windows Maven reactor.

## Probe behavior

- Show Android release, firmware and actual watch API level locally, without writing identifiers to logs.
- User requests the Android system's discoverability dialog for at most 120 seconds, and initiates pairing in **iPhone Settings → Bluetooth**, verifying matching codes. Discovery through iPhone settings is a **hypothesis to test**, not a guarantee.
- Show bonded-device names **only on watch**; explicit selection and confirmation of the chosen iPhone are mandatory.
- Use the selected bonded device for a one-shot LE GATT client connection, inspect service UUIDs: ANCS, AMS, optional Battery/Device Information/Current Time. No address/name/identifier is logged.
- Subscribe (if exposed and permitted) to ANCS Notification Source and AMS Remote Command / Entity Update; register AMS Player/Track attribute monitoring **after a successful Entity Update subscription acknowledgment**. Show only sanitized notification type/category, AMS command count, entity/attribute indicators and per-session subscription/event counters. **Do not capture message text, UID or media metadata values.**
- Listen for optional GATT Service Changed indication and the Android service-change callback; trigger **at most one automatic service rediscovery per explicitly selected connection**. Tap **5. Refresh GATT services** to request another one-shot re-discovery manually. This does not reconnect or pair automatically.
- Manual Disconnect closes GATT and stops pending operations. **Leaving the app (Activity onStop) also closes GATT**; onDestroy is a fallback, so this foreground-only probe does not keep a hidden connection alive and compete with other test APKs. No background service, automatic reconnect, continuous scan, Internet permission, cloud, account or stored phone data. A successful GATT link **does not mean** ANCS/AMS have been rediscovered, CCCD operations accepted, or media events recovered; inspect the summary.
- This app has no iPhone media control commands, ANCS attribute fetching or mature lifecycle management. It does **not** prove successful day-to-day Bridge behavior or battery life.

## Build

Use JDK 17+, Android SDK 35 and Build Tools 35.0.0, Gradle 8.11.1 (installed by CI). No generated Gradle wrapper binary is committed.

From experiments/w0-ble-probe:

    gradle :app:testDebugUnitTest :app:assembleDebug

On a Watch4 already configured for Wi-Fi ADB, install the built debug APK using the user's explicitly selected ADB device:

    adb -s YOUR_WATCH_DEVICE install -r app/build/outputs/apk/debug/app-debug.apk

Turn off wireless debugging after installation. Do not reset, unpair or wipe the watch.

The experimental debug APK is attached to the latest passing [W0 experimental BLE probe GitHub Actions run](https://github.com/aidarnig/WristPorter/actions/workflows/w0-ble-probe.yml) as a downloadable ZIP artifact named **wristporter-w0-probe-debug-apk** (requested retention: 30 days; older run artifacts may expire). You must be signed into GitHub to download an Actions artifact. Unzip it; the APK inside is `app-debug.apk`. This is a development-signed probe, **not** a signed consumer release.

### Safer installation using the repository's Windows helper

From a trusted checkout of WristPorter's `main`, use [`ci/install-watch-test-apk.ps1` on main](https://github.com/aidarnig/WristPorter/blob/main/ci/install-watch-test-apk.ps1). Connect to your already configured watch over Wi-Fi ADB using the existing WristPorter UI (or your authorized ADB pairing), then run from repository root:

```powershell
.\ci\install-watch-test-apk.ps1 -ApkPath "C:\Path\to\app-debug.apk" -AdbPath "C:\Path\to\platform-tools\adb.exe"
```

Use the actual paths where you unzipped the APK and where your trusted `adb.exe` is installed. If `adb` is on PATH, omit `-AdbPath`. With multiple ADB devices, pass the locally checked `-Serial`. The helper validates one authorized device and requires you to type `INSTALL` before installing. Do not share ADB serials, watch IPs, pairing codes, or private notification text in public screenshots or Issues.

If `adb` says `unauthorized` or `offline`, do not retry an install blindly; reconnect/authorize via the normal Windows/watch interface. To stop the test, press **4. Disconnect** and close the probe. Turning off Wireless debugging after installation is recommended; no watch reset or Bluetooth unpair is required.

### First owner-assisted checkpoint

**Start with BLE-00, BLE-01, BLE-02 only.** Launch the probe and record Android release/API and firmware build (excluding any identifier); record iPhone model and iOS separately; tap `1. Permissions`, then `2. Discoverable (120s)` and approve the system dialog. On the iPhone go to Settings → Bluetooth and look for the newly discoverable watch. If it appears, confirm matching numeric prompts and the iOS notification-sharing permission if offered. If it does not appear, record that outcome without restarting/resetting watches or changing bonds.

Reply to Issue #13's executor chat with the watch Android/API, iPhone model/iOS and precisely what happened at the discoverability/pairing stage (no IDs). Only then proceed with BLE-03…BLE-05 in the same chat for GATT/ANCS/AMS; the owner should not be asked to run a massive full matrix blindly.



## Targeted recovery test after iPhone Bluetooth OFF/ON (W0 v0.0.3 Round Probe)

The 2026-11-22 owner observed that an explicit **Disconnect → Choose bonded phone** restored both notification and music events, but after switching iPhone Bluetooth OFF/ON a new manual GATT connection showed `status=0` while **AMS track-change events stopped**. This is **not** evidence of a stable recovery path and not a reason to unpair/reset.

1. Install the **v0.0.3-round** APK from its passing Android Actions run on the already authorized watch. It is a **separate side-by-side app** named **W0 Round Probe**, with package `io.github.aidarnig.wristporter.w0.round`, so it does **not** overwrite or require uninstalling earlier GitHub Actions debug builds (the signing key can change). Grant Nearby devices permission and select the already bonded iPhone. No re-pairing or reset. Confirm the **v0.0.3 round** label in the on-screen session summary; earlier **WristPorter W0 Probe** and **W0 Recovery Probe** are distinct apps.
2. Open the watch probe; **3. Choose bonded phone** and manually select your own iPhone. Confirm ANCS/AMS services and `sub=0` / `entitySub=0` / `AMS registrations=2/2` in the top summary. Change one track on the iPhone; note whether the AMS event count increases.
3. Switch iPhone Bluetooth OFF/ON **once**. If the watch probe shows disconnected, reselect the same phone via **3. Choose bonded phone**; do not erase the bond. On a resumed `Link state=connected`, inspect the **session summary**, notably `AMS present`, `entitySub`, `AMS registrations` and `events`. Change a track once.
4. **Only if AMS events do not resume**, press **5. Refresh GATT services** once. Let its descriptor and registration statuses settle, change a track again and compare the summary counters. Stop after that one attempt; report the sanitized before/after summaries or failure lines to the executor chat.
5. **4. Disconnect** exits the experiment's GATT session; close the app. Disable Wireless debugging after installing. No repeated Bluetooth toggles, forced unpair, logs containing personal data or device identifiers.

**Round-screen layout:** diagnostics use proportional side/top insets, two-line buttons, and extra blank scrollable space below the latest session summary. Swipe upward at the bottom to move the last lines toward the center, clear of the watch bezel. Reconnecting clears the previous session's scrollback so subscription errors are not mistaken for the current link. These are UX changes; screen fit still needs a real-device check.

**Specific observed failure 2026-11-22:** on the watch photograph after iPhone Bluetooth OFF/ON, the GATT link returned `status=0`, ANCS Notification Source subscription returned `status=0`, while **AMS Remote Command and AMS Entity Update subscriptions returned `status=1`**; the owner observed no further events. The picture alone cannot identify the exact opened APK build, so this is recorded as an AMS subscription error with **unconfirmed APK version**, not proof v0.0.2 failed. Do not equate service discovery (`AMS present=true`) with a successful CCCD subscription.

This is a bounded foreground-only diagnostic, not an automatic repair guarantee. If the service is temporarily unpublished or the iPhone media player sends no event, a refresh alone may not restore it. No production battery/reliability guarantee is implied.

## v0.0.4 GATT Inspector — one targeted AMS write-permission checkpoint

**Why:** Owner confirmed v0.0.3 `GATT_WRITE_NOT_PERMITTED` (status 3) **persists even after one manual Refresh GATT**; see [Issue #13](https://github.com/aidarnig/WristPorter/issues/13). We must distinguish characteristics' advertised write/notify capabilities, the local Android operation start status, and the callback result from the remote GATT server. Apple AMS defines Entity Update as writable **with response**, so this probe does not blindly switch write types or auto-retry failures.

- Latest independent test build is labeled **W0 GATT Inspector** and shows **v0.0.4 inspector** in the on-watch summary, package `io.github.aidarnig.wristporter.w0.inspector`. It installs alongside prior debug apps and does not require unpairing, uninstalling any probe, or resetting the watch.
- When services are discovered, the screen lists `ANCS source`, `AMS command`, `AMS entity` with safe public capabilities: `N` (Notify), `I` (Indicate), `W` (write with response), `WNR` (write without response), `R` (read) and `CCCD` descriptor present. These flags are **not** proof of authorization.
- For each subscription: `CCCD start` / `CCCD local start code` describe **whether the Android call started**; `CCCD remote callback ... status` describes the asynchronous BLE result. For each AMS entity registration: `AMS write start entity=0/2 type=WITH_RESPONSE`, `AMS local start code` and `AMS register entity=... remote callback status` distinguish the same two stages. Status 3 means `WRITE_NOT_PERMITTED`. No message text, track value, UID or BLE identifiers are logged.
- The round-safe end-of-log summary shows `Last AMS issue` and `GATT operations: queued / active`. Let these become `queued=0 active=false` before assuming all writes completed; `AMS registrations=0/1` is not proof that the second registration finished.

**Minimal device test after successful CI build:** open the new **W0 GATT Inspector**, grant Nearby devices and select the already bonded iPhone. Let the one-shot connection settle (no Bluetooth toggle). Swipe to the end of the text, and send one sanitized photo or transcribe the `AMS command` / `AMS entity` capabilities and the relevant `local start`, `remote callback`, and `Last AMS issue` statuses. **Do not press Refresh** unless specifically requested after those results. If there is no music event, that is already known; do not repeatedly switch tracks. Close with **4. Disconnect**. The next implementation decision is whether the iPhone advertises a different characteristic write mode from the one required by AMS, or whether it advertises that mode but rejects writes during this link.

## Real watch/iPhone test instructions

1. Record watch model, Android API and firmware (probe UI or adb shell getprop ro.build.version.sdk); record iPhone model, iOS version and Bluetooth state separately. Never publish serial, MAC, pairing codes or private notification/media contents.
2. Grant Nearby devices on watch. Tap **Discoverable (120s)** and approve the OS dialog. On iPhone open **Settings → Bluetooth** and select the watch **if present**, with matching confirmation on both devices. If absent, record the exact failure (including whether watch was visibly discoverable). Do not assume a universal incompatibility from one attempt.
3. If offered in iPhone Bluetooth device settings, enable **Share System Notifications**. On the watch choose and confirm the actual bonded iPhone; start one explicit GATT connection. Record connect/discovery statuses, ANCS/AMS presence and subscribe results.
4. Trigger one fresh non-sensitive iPhone notification and one media playback/track change while the probe is open. Only sanitized event indicators count. A successful Android build is **not** a device PASS.
5. Disconnect manually; toggle Bluetooth off/on and attempt user-initiated reconnect. Record concrete failures and whether discoverability expires. Screen-off and background operation remain untested by this foreground-only probe.
6. Fill the existing [W0 verification ledger](../../docs/VERIFICATION.md) scenario-by-scenario (BLE-00…BLE-10), with actual UTC date, watch/firmware/API, iPhone/iOS, build SHA, outcome and reproduction steps. Use NOT TESTED, BLOCKED, INCONCLUSIVE, FAIL or PASS precisely.

If GATT is unavailable, distinguish no system pairing, bond present but GATT failure, no ANCS/AMS, authorization denied and a subscription failure before choosing next steps.

References: [Apple ANCS](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleNotificationCenterServiceSpecification/Specification/Specification.html), [Apple AMS](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleMediaService_Reference/Specification/Specification.html), [Android Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions).
