# WristPorter Bridge — maximum capability without an iOS application

**Status: PLANNED, not implemented or verified by WristPorter.** This document is the normative feature feasibility and support-claim matrix. It covers communication directly between an **already configured Wear OS watch** and a stock iPhone, with **no iOS app, account, Wi-Fi relay, cloud or jailbreak**. Focus on legally documented native interfaces and observed behavior. User requirements live in [PRODUCT.md](PRODUCT.md).

## Interpretation

- **Documented protocol** = Apple defines a mechanism; **not** evidence it will work on every iOS/watch combination.
- **Conditional / investigate** = exposed service, player-specific support, consent or hardware behavior must be discovered.
- **Watch-local** = can be implemented without phone data (UI, filtering, privacy, Tile, local watch battery).
- **Out of scope** = no documented path through the chosen no-iOS-app architecture; never advertise as shipped.
- Every feature goes from PLANNED → coded → **VERIFIED only with device evidence** recorded in [VERIFICATION.md](VERIFICATION.md).

## A. Apple Notification Center Service (ANCS)

| Capability | Basis | Planned behavior / boundaries | Priority |
| --- | --- | --- | --- |
| Bluetooth pairing and iOS notification-sharing consent | BLE/iOS pairing | User-initiated visible device, explicit bond/authorization; watch must already be set up | W0, W2 |
| New incoming notifications | ANCS Notification Source | Per-UID events for notifications iOS chooses to forward; no universal guarantee for every app | W3 |
| Updated / removed notifications | ANCS Notification Source | Amend/dismiss local projections and invalidate pending work; no ghost cards | W3 |
| Title, subtitle, message, date, application ID | ANCS attributes | Retrieve only attributes supplied by iOS; truncate safely; no arbitrary access to full messages/photos | W3 |
| App display name and category | ANCS app attributes/categories | Cache per session; classify call, mail, social, schedule, location, etc. as source reports | W3 |
| Flags: silent, important, pre-existing, actions | ANCS flags | Respect hints, protect privacy; pre-existing events are not guaranteed complete history | W3 |
| Positive/negative actions | ANCS action flags and action labels | Show **actual provided labels**, execute only valid supported action; outcomes are iOS-defined | W4 |
| Incoming-call action (answer/decline) | ANCS categories/actions | **Conditional:** if iOS exposes action flag/label; call audio still stays on phone or its selected output | W4 |
| Clear notification when removed on iPhone | ANCS removal event | Reflect removal only when event received; not a general remote notification-management API | W3 |
| App/category filters, silent updates, grouping, hide body, quiet hours | Watch-local policy | Filters only on received content; no permission to change iOS's notification settings | W6 |
| Local notification timeline | Watch-local bounded state | RAM-only by default, no persistent private history unless explicit future opt-in security decision | W6 |
| Arbitrary text replies / voice dictation from watch to iPhone apps | **Not in ANCS** | OUT OF SCOPE; positive/negative actions are **not** free-form reply APIs | — |
| Delete all iPhone notifications, read SMS/iMessage database | **Not in ANCS** | OUT OF SCOPE; no full history or bulk control | — |
| Full call control and speaking through watch microphone/speaker | **Not ANCS** | Call alert action ≠ Bluetooth Hands-Free Profile audio/call integration; NOT supported here | — |

**Design rule:** ANCS UID is valid only for its session. Never execute an old action after reconnect, and never assume action meaning by category. The service can be temporarily absent; react to GATT Service Changed. Attribute service components may be unavailable. Official specification: [Apple ANCS](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleNotificationCenterServiceSpecification/Specification/Specification.html).

## B. Apple Media Service (AMS)

| Capability | Basis | Planned behavior / boundaries | Priority |
| --- | --- | --- | --- |
| Active player name, playback state/rate, volume | AMS Player attributes | Native watch player/MediaSession shows observed iPhone playback | W5 |
| Current title, artist, album, duration | AMS Track attributes | Handle missing, empty, truncated and changed metadata; no artwork guarantee | W5 |
| Queue size/index, repeat/shuffle mode | AMS Queue attributes | Display only if values exist; don't invent list entries or playlist contents | W5 |
| Progress / elapsed time | AMS PlaybackInfo + local clock | Extrapolate only from timestamped state/rate; no high-frequency polling | W5 |
| Play / pause / toggle, next / previous | AMS Remote Command | Send **only if advertised** by the active player; successful write ≠ player acted | W5 |
| Relative volume up/down | AMS Remote Command | Step-based relative control; not guaranteed precise absolute volume setting | W5 |
| Advance repeat / shuffle, skip forward/back | AMS Remote Command | Advertised commands only; do not silently issue unsupported commands | W5, W10 |
| Like/dislike/bookmark track | AMS Remote Command | Conditionally available, player-dependent; test each app | W10 |
| Native watch media tile / volume bezel | Watch-local UI + AMS | Optional Samsung/system UX variations; no global gesture claims | W6 |
| iPhone audio on watch / AirPods selection / queue editing | **Not AMS** | OUT OF SCOPE; AMS is control + metadata, not audio streaming or full queue editing | — |
| Accurate album artwork from AMS | **Not in documented AMS track attributes** | OUT OF SCOPE without independent lawful source; never promise covers from AMS | — |

AMS services or individual characteristics can be absent; remote command support is dynamic. Official [Apple AMS service](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleMediaService_Reference/Specification/Specification.html) and [command/attribute IDs](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleMediaService_Reference/Appendix/Appendix.html).

## C. Other GATT services and watch-local improvements

| Capability | Feasibility | Decision / gate |
| --- | --- | --- |
| iPhone battery level | **Investigate BLE Battery Service (0x180F)** | Probe on actual iOS device; value/notifications only if service exposed/authorized |
| iPhone manufacturer/model info | **Investigate Device Information (0x180A)** | Some fields may be missing, blank or generic; don't advertise complete device profile |
| Phone time | **Investigate Current Time (0x1805)** | Read if actually exposed; watch already has own system clock, so utility low |
| Watch battery, charging state, local time | **Watch-local** | Can show immediately without asking iPhone; avoid continuous polling |
| Bluetooth connection indicator, service status, last disconnect reason | **Watch-local** | High-value for troubleshooting; no raw address leakage |
| App filters, low-power preferences, connection help, diagnostics | **Watch-local** | Provide robust user experience independently of BLE success |
| Watch Tile, complication or quick launch | **Watch-local / platform-dependent** | Favor useful controls and connection state; validate battery impact |
| Audio calls through watch, Apple Health, Samsung Health iPhone sync, contacts sync, find/ring iPhone, photo transfer from iPhone, general phone commands | **No dependable ANCS/AMS path** | OUT OF SCOPE for current no-iOS-app program, unless a documented independent mechanism with verified evidence is approved via ADR |

Bluetooth SIG standardized service names are not proof an iPhone actually publishes them. Reference: [Bluetooth SIG Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/). Device evidence belongs in [VERIFICATION.md](VERIFICATION.md).

## D. Strategic exclusions

- **No iOS app or iOS code now.** Do not implement the earlier proposed companion, HealthKit sync, contact bridge or iPhone-side photo sharing in this program.
- No factory-reset setup, no Galaxy Wearable replacement claim, no official Samsung/Google/Apple support claim.
- No Internet relay, account, server, telemetry, paid SDK or third-party notification scraper.
- Apple's newer [Accessory Notifications](https://developer.apple.com/documentation/AccessoryNotifications) requires an iOS companion/extension model and is region-restricted for customer installations; it is **not** our no-app mechanism.
- No non-public iOS protocol dependency or undocumented hacks as an assumed feature. Any experiment requires explicit security/legal/scope review.

## E. References and inspiration versus evidence

- [WeakBridge](https://github.com/Weakmafaka/WeakBridge) is a useful independent ANCS/AMS design reference, but its documented Galaxy Watch6 tests **do not verify our Watch4 Classic**.
- [Bridge by ThomasShelby4477](https://github.com/ThomasShelby4477/wear-bridge) reports Watch4 Classic work but uses an iOS companion for many extended features. Its own statements are not WristPorter verification.
- External project code may be reused only with verified licensing, attribution and testing; review principles, not assumed complete compatibility.

**Desired outcome:** maximize dependable **ANCS + AMS + watch-local UX**, then evaluate genuinely exposed additional GATT services. Feature scope is broad, but supported claims remain narrow until verified.
