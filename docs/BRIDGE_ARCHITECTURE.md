# WristPorter Bridge — proposed Wear OS architecture

**Design status:** proposal, accepted direction; **no Kotlin implementation exists yet**. [ADR-0001](adr/0001-independent-wearos-bridge.md) fixes separation from Windows; [ADR-0002](adr/0002-direct-ble-safety.md) governs BLE/security defaults. Implementation starts after [W0 #13](https://github.com/aidarnig/WristPorter/issues/13) establishes the reference watch capabilities.

## System boundary

```text
Windows desktop (existing, JVM/Maven/JavaFX)
  watch-domain -> watch-adb -> watch-core -> watch-desktop
            |
            | Wi-Fi ADB (setup, APK install/update, photos)
            v
Watch4 Classic — WristPorter Bridge (new Kotlin Android APK)
    Compose UI / Android notifications / MediaSession
                    |
    lifecycle supervisor (connectedDevice FGS or evidence-backed alternative)
                    |
    BLE identity + bond + serial GATT op queue
               /                 \
          ANCS client          AMS client
       (notifications)      (media controls)
               \                 /
             iPhone built-in Bluetooth GATT services
             NO iOS app / account / cloud / internet
```

The phone/wear BLE connection functions without a Windows PC once provisioned. Bluetooth **link role** can differ from **GATT client role**: the watch may advertise for pairing while remaining GATT client of iPhone's ANCS/AMS. Verify this on the reference device in W0, not in assumptions.

## Proposed source layout (not yet created)

```text
wearos/
  settings.gradle.kts
  build.gradle.kts
  gradle/...
  app/                   # app shell, Wear Compose screens, service, Android integrations
  ble/                   # transport contract, bond identity, reconnect policy, GATT queues
  ancs/                  # pure Kotlin codec, ANCS state/attributes/actions
  ams/                   # pure Kotlin AMS codec, playback/queue state, supported commands
  test-support/          # optional fake clocks/transports, only if justified
```

Prefer minimal useful modules over premature fragmentation. Protocol parsers/state reducers must compile/test without Android runtime if practical; choose actual Gradle structure in W1 ADR if different.

**Existing Maven reactor stays unchanged.** Do not move Java domain/ADB/transfer logic into the Android app or require Kotlin/Android SDK to build the desktop. Wear APK versioning, signing and CI are independent of desktop v1.1.2.

## Connection and lifecycle state machine

```text
DISABLED
  -> READY (permissions + Bluetooth enabled)
  -> ADVERTISING (explicit pairing window, time limited)
  -> BONDED (user verified code + selected device)
  -> CONNECTING
  -> DISCOVERING
  -> AUTHORIZING/SUBSCRIBING
  -> CONNECTED [ANCS status, AMS status independently tracked]
  -> RECOVERING (bounded retry if unexpected disconnection)
  -> CONNECTED or DISCONNECTED (actionable status)
```

- Explicit DISCONNECT must stop advertising, scan, pending reconnects and active GATT queues.
- On bond loss require new user selection; never guess by device name, first scan result or MAC observed under BLE address randomization.
- A GATT connection generation ID invalidates callbacks/notifications/actions from old sessions. ACK/timeouts and callback types must be matched; one serialized queue per active GATT.
- GATT `Service Changed` events can invalidate ANCS/AMS availability; re-discover and re-subscribe, showing **partial feature status** rather than fake “fully connected”.
- Robust backpressure: bounded pending ANCS attribute requests, bounded displayed notifications, coalesced AMS updates, length/time limits on fragmentation and UTF-8.
- Android service selection: compare `connectedDevice` foreground service with companion presence options under supported Android APIs/firmware, based on real reliability/power tests. A foreground service is not a guarantee of perpetual connection or unlimited background starts.
- No uncontrolled permanent discovery, continuously running scans, periodic keepalive by default, blind retries or wakelock.

References: [Android BLE background behavior](https://developer.android.com/develop/connectivity/bluetooth/ble/background) · [Android connected-device foreground service](https://developer.android.com/develop/background-work/services/fgs/service-types).

## Protocol responsibilities

### ANCS

`Notification Source` → stable per-session event parser (added/modified/removed/category/flags) → bounded state repository → queued attribute requests through `Control Point` → validated `Data Source` fragment assembler → Android notification surfaces and optional app policy. Use actual iOS positive/negative action labels; only send an advertised action for current-session UID. Never store action handles across disconnect.

### AMS

Subscribe to `Remote Command` supported-list and `Entity Update`. Maintain independent Player/Queue/Track state; watch-local time extrapolates playback position from rate/timestamp without polling. Truncated field recovery uses serialized `Entity Attribute` read and revisions. Clear stale track data when player changes. Route supported commands through an Android `MediaSession`/remote VolumeProvider where compatible with Samsung software. No local playback or audio streaming.

### Other BLE services

Probe, don't presume, `Battery Service`, `Device Information` and `Current Time` after W0/W10. Each optional service has its own availability and permission state. Avoid sharing generic 'connected' success across absent features.

## Security / privacy by design

- Request only necessary nearby-device, notification and foreground-service permissions with understandable UI; no Internet permission for the first watch app.
- Pair only by explicit user action; user verifies Bluetooth pairing code; keep selected bond identity through OS identity APIs, not a stored raw address guess.
- Notification text remains RAM-only by default. Clear sensitive state on disconnect/restart, use private lockscreen visibility, never emit message content/addresses/pairing IDs in logs or CI artifacts.
- Watch app exported components default non-exported; externally reachable launch surface does not accept untrusted commands.
- Bound every incoming byte stream and handle unexpected fields without crashes or data leaks.
- Local app settings persist without secret token or raw mirrored notification content; no backup of secrets.
- APK signature keys never in Git; updates must preserve package signature and user data. Future Windows-side APK install requires explicit selected watch and consent.

## Reliability and power gates

- Instrument **sanitized** connection transitions, retry counts, queue depth, reconnect reasons, ANCS/AMS availability, CPU/wake activity where available.
- Battery baselines include watch with bridge disabled vs idle paired vs notifications vs media vs degraded signal at same settings. Collect multiple samples; no unmeasured “low battery usage” claim.
- Required failure injection: Bluetooth off/on, iPhone reboot/lock, watch reboot, leave range/return, process kill, permission revoked/restored, stale action after reconnection, iPhone media app switch, notification burst and malformed frames.
- [VERIFICATION.md](VERIFICATION.md) stores measured outcomes; [QUALITY.md](QUALITY.md) decides release gates. W7/W8 are dedicated workstreams, **but each preceding PR must preserve these invariants**.

## Open design decisions (not to assume)

- Exact Watch4 Android API and minSdk.
- Best lifecycle mechanism on Samsung firmware.
- How iPhone publishes service/bond following pairing and device reboots.
- Notification permission/user-consent prompts and background restart on target firmware.
- Measured battery impact and acceptable release threshold.
- How the Galaxy Watch's built-in media UI and system notification UI behave.

Create superseding ADRs when experimental evidence changes these assumptions.

## Independence from the Windows-hosted Safari upload mode

**This document applies to direct Bluetooth ANCS/AMS between iPhone and the Wear OS app.** The ecosystem separately plans an **on-demand Windows-hosted [Local Web Upload](LOCAL_WEB_PORTAL.md)** where Safari uploads a user-selected photo to the Windows PC and the PC pushes it to the selected watch via Wi-Fi ADB. That mode does **not** require the ANCS/AMS Bridge APK, and the Bridge does **not** require Windows running after setup. Both must preserve independent lifecycle, network trust, diagnostics and resource use. See [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md).
