# WristPorter — durable roadmap

**Canonical plan:** update milestone definitions here and work progress in the linked Issues. Status at planning time (2026-11-13): **Desktop v1.1.2 shipped; W0–W16 are PLANNED, not built.** This is a future **Wear OS + Windows ecosystem program without an installed iOS companion app**, with two independent tracks: direct Bluetooth Bridge and Windows-hosted local browser upload. [Read current state](STATE.md) and [all feature limits](BRIDGE_CAPABILITIES.md) before starting any milestone.

## Product goals

**North star:** WristPorter is a **local-first ecosystem for Wear OS owners after moving from Android to iPhone**, not merely a photo-transfer utility. Existing Desktop, future Wear OS Bridge and optional browser access through Windows must compose without becoming mandatory dependencies on each other.

1. Preserve already-working **Windows → Wear OS JPEG/PNG Gallery transfer** and the no-installer portable experience.
2. Add **WristPorter Bridge** on Wear OS to connect an *already configured* watch directly to iPhone over Bluetooth, without an iOS app, cloud, account or desktop running continuously.
3. Maximize genuine **ANCS notifications/actions and AMS media metadata/commands**, plus watch-local UX and BLE services **only where observed**.
4. Treat **reliability, battery, privacy, security, accessibility, recovery and honest compatibility** as product features and release blockers, not cleanup tasks.
5. Extend the **same Windows app** with an **on-demand private-LAN browser page** so iPhone Safari can upload JPEG/PNG via Windows → ADB → watch, with APK management in that same Windows app. The PC must remain on. Validate auth, network transport, photo format and cleanup before release.
6. Make cross-chat development reproducible through [AGENTS.md](../AGENTS.md), [engineering rules](ENGINEERING_STANDARD.md), [STATE.md](STATE.md), PR templates and issue-specific acceptance checks.

## Version/compatibility policy

- **Desktop current stable:** v1.1.2, unchanged until a separately verified desktop change ships.
- **Bridge:** brand-new independent Wear OS app and release version line (**version not selected yet**); never label the current desktop ZIP as a bridge release.
- Reference Watch4 Classic firmware/API and an actual iPhone model/iOS version must first be recorded in W0. Do **not** assume other project's Watch6/iPhone test verifies ours.
- iOS companion development is **explicitly DEFERRED**, and no feature whose implementation needs it is in the W0–W12 delivery commitment.
- Windows APK management is optional: the Android app must be usable after initial ADB installation without a continuously running PC.
- **One Windows ZIP/EXE, two photo entry points**: native Desktop picker and manually started browser Web Upload; no separately distributed Windows edition or installed iOS app ([ADR-0004](adr/0004-one-desktop-optional-lan-portal.md)).
- The Web Upload portal **requires** a running Windows PC; the independent ANCS/AMS Bridge **does not**. HEIC and iPhone web uploads are still unimplemented.

## Milestones and issue anchors

| ID | Issue / status | Outcome | Blocking dependencies |
| --- | --- | --- | --- |
| **W0** | [#13](https://github.com/aidarnig/WristPorter/issues/13) · PLANNED | Validate Watch4/iPhone pairing, ANCS/AMS discovery and first events on real hardware | None — **start here** |
| **W1** | [#14](https://github.com/aidarnig/WristPorter/issues/14) · PLANNED | Independent Kotlin Wear OS build, tests, CI, bilingual UI shell | W0 API/device facts |
| **W2** | [#15](https://github.com/aidarnig/WristPorter/issues/15) · PLANNED | Explicit BLE bond/pairing, selected-device identity, serialized GATT transport | W0, W1 |
| **W3** | [#16](https://github.com/aidarnig/WristPorter/issues/16) · PLANNED | ANCS notification lifecycle, app attrs, categories, privacy-safe watch notifications | W2 |
| **W4** | [#17](https://github.com/aidarnig/WristPorter/issues/17) · PLANNED | Conditional ANCS actions and call alerts; verified answer/decline only if exposed | W3 |
| **W5** | [#18](https://github.com/aidarnig/WristPorter/issues/18) · PLANNED | AMS player/queue/track metadata + supported media commands + MediaSession | W2 |
| **W6** | [#19](https://github.com/aidarnig/WristPorter/issues/19) · PLANNED | Wear UI, settings, notification filters, quiet hours, grouping, RU/EN, accessibility | W3, W5 |
| **W7** | [#20](https://github.com/aidarnig/WristPorter/issues/20) · PLANNED | Background lifecycle, service restoration and bounded reconnection | W2–W5 |
| **W8** | [#21](https://github.com/aidarnig/WristPorter/issues/21) · PLANNED | Battery/thermal benchmark and measured optimization | W2, W7 |
| **W9** | [#22](https://github.com/aidarnig/WristPorter/issues/22) · PLANNED | Threat model, permissions, privacy verification and signed APK delivery | Start W1; final sign-off after W3 |
| **W10** | [#23](https://github.com/aidarnig/WristPorter/issues/23) · PLANNED | Probe available iPhone battery/device/time BLE services and advanced ANCS/AMS commands | W3, W5 |
| **W11** | [#24](https://github.com/aidarnig/WristPorter/issues/24) · PLANNED | Optional Windows APK install/update/diagnostics, preserving photo path | W1, stable watch APK |
| **W12** | [#25](https://github.com/aidarnig/WristPorter/issues/25) · PLANNED | **Bluetooth Bridge** acceptance, beta signing/release, documented limitations | Integrated W0–W11 evidence for Bridge scope |
| **W13** | [#27](https://github.com/aidarnig/WristPorter/issues/27) · PLANNED | LAN Web Upload architecture, Safari feasibility, threat model/TLS decision | Desktop v1.1.2; parallel to W0 |
| **W14** | [#28](https://github.com/aidarnig/WristPorter/issues/28) · PLANNED | iPhone browser → Windows → ADB → watch JPG/PNG in same Windows EXE | W13 security/design gate |
| **W15** | [#29](https://github.com/aidarnig/WristPorter/issues/29) · PLANNED | HEIC/Live Photo/metadata/orientation conversion research | W13/W14 architecture; optional for MVP |
| **W16** | [#30](https://github.com/aidarnig/WristPorter/issues/30) · PLANNED | Browser-upload security, network and real-device acceptance; desktop regressions | W14; W15 only if included in release |

Issues are **work packages**, not single mega-PRs. Split each into small coherent implementation/verification PRs; link all PRs to the anchor Issue and update acceptance evidence. A milestone cannot be called VERIFIED until its exit gate is met on reference hardware where required.

## Detailed gates and implementation order

### Gate A: prove the communication path — W0 + W1 + W2

- W0: log actual watch Android `ro.build.version.sdk`, firmware and iPhone iOS (without identifiers); confirm pairing permissions and roles, ANCS/AMS availability, sample new notification/music event. A failure is a design decision point, **not** permission to reset a watch.
- W1: implement only a watch installable shell and testable infrastructure; keep Maven reactor isolated and desktop build unchanged.
- W2: bounded GATT queue, callbacks, generation IDs, bond identity, explicit disconnect and safe authorization. **Pass:** hardware pairing repeated, stale callbacks and denied permissions tested.
- **Do not proceed to promising full bridge usability if W0's real-device path is blocked.** Document blockers and alternatives before changing scope.

### Gate B: first useful watch bridge — W3 + W5 (MVP)

- Notifications: add/update/remove, attrs/UTF-8 fragmentation, app metadata, Android presentation, privacy and stale-event suppression.
- Media: play/pause/skip/relative volume; track, player, queue, playback status and timestamps; per-player support lists.
- **MVP acceptance:** receive a fresh iPhone notification and display it accurately on Watch4, then operate and observe music controls, without PC running after installation.
- **Not MVP promises:** generic call audio, freeform replies, photo transfer from iPhone, contacts/Health sync, Apple Watch feature parity.

### Gate C: reach/exceed WeakBridge's no-iOS-app scope — W4 + W6 + W10

- Add actions that iOS advertises (including conditional call answer/decline), dynamic notification policy, app filters/quiet hours, secure UI and accessibility.
- Explore all documented AMS extras only if player support list includes them: repeat, shuffle, skip forward/back, like/dislike, bookmark. Do not call them universally available.
- Probe optional Battery/Device Information/Current Time GATT services; display only verified values, gracefully handle absence.
- **Exit:** feature-by-feature iPhone/watch matrix with both PASS and FAIL/NOT TESTED, not a blanket “all iOS features supported” badge.

### Gate D: dependable daily use — W7 + W8 + W9

- Lifecycle: screen-off, watch-face, disconnect/out of range, recovery, Bluetooth toggles, reboot, process death, iOS authorization changes.
- Power: baseline vs idle paired, typical notifications, media, poor signal; repeated measurements, documented conditions, no invented battery percentages.
- Privacy/security: model unauthorized BLE pairing, malformed packets, untrusted notification text, stale actions, wrong-device installs, exposed logs, permissions and signing.
- These are **cross-cutting requirements starting in W1**. W7/W8/W9 are dedicated integration/measurement and hardening milestones, not permission to defer safety until the end.

### Gate E: package, manage, validate — W11 + W12

- Desktop installer/update actions are explicit, selected-watch-only and signature/version aware. Regression-test existing JPEG/PNG transfers on Watch4, including gallery visibility.
- Separate Windows/Desktop and Wear APK CI and signed release assets, changelogs, version lines, checksums, onboarding, docs and the supported-device matrix.
- **Exit:** complete [verification ledger](VERIFICATION.md), resolved release blockers, approved exceptions documented, beta release explicitly scoped to tested hardware.

## Delivery sequencing and parallel tasks

- **Critical path:** W0 → W1 → W2 → W3 and W5 → W7/W8 → W12.
- After W2 stabilizes, W3 (ANCS) and W5 (AMS) can be developed in **different chats/branches**, sharing a documented BLE interface; merge transport-contract changes first.
- After W3, W4 actions and W6 policy/UX can proceed separately. W9 threat model can start at W1 in parallel; W10 BLE probes wait for mature transport.
- W11 desktop management can proceed once an installable stable watch APK and agreed package signature/version contract exist.
- **Never run conflicting chats against the same branch or module without an interface contract.** Rebase/merge with checks after shared changes. GitHub Issues and merged PRs are the coordination mechanism.

## Definition of Done for every PR

- Issue scope and real acceptance criterion addressed; relevant tests green, or exact missing checks recorded.
- Privacy and power impact considered; error/cancel/reconnect behavior exercised by fakes and by hardware when possible.
- Documentation owners updated, ADR if contract/scope changes, [STATE.md](STATE.md) refreshed when progress changes.
- Evidence distinguishes code verification from physical Watch4/iPhone verification.
- Legacy Windows photo transfer not broken and no accidental mandatory iOS/backend dependency added.

## Risks and contingencies

| Risk | Prevention / decision point |
| --- | --- |
| Watch4 cannot expose usable ANCS/AMS with the tested iPhone | Stop broad build; document W0 reproduction and choose an evidence-based alternative via ADR |
| Watch firmware API older than expected | Probe actual API in W0; pick minimum compatible SDK rather than importing WeakBridge's API 33 |
| Samsung background management kills bridge | Compare OS-sanctioned FGS/CompanionDeviceManager approaches; quantify recovery vs power |
| iOS hides notification text/actions or media app lacks AMS | Reflect capability/unavailable state; never invent missing data/actions |
| BLE retry storm drains battery | Bounded backoff/circuit breaker, no permanent discovery, energy benchmarks |
| User loses existing Gallery transfer | Keep desktop modules independent, existing tests and real regression on Watch4 |
| Private content leaks through issues/logs | Redaction, RAM-only defaults, controlled diagnostics, PR review |
| Long chat context degrades | New chats use [AGENTS.md](../AGENTS.md) → [STATE.md](STATE.md) → anchored Issue and live GitHub, not prior chat transcripts |

## Independent second delivery track — Windows local browser upload (W13–W16)

[Detailed technical specification](LOCAL_WEB_PORTAL.md) · [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md).

- **W13, #27:** verify iPhone Safari photo selection, choose an embeddable **opt-in** LAN server, decide authentication and plaintext/HTTPS policy. Windows user selects the watch; QR exposes the PC URL only.
- **W14, #28:** add Web Upload tab in **the same** Windows app; browser uploads user-selected JPG/PNG to a bounded, private staging area; existing `PhotoFiles` / `WatchTransferService` pushes to the watch. A shared transfer coordinator serializes Desktop and browser jobs, pins authorized watch and preserves copy-vs-scan result semantics.
- **W15, #29:** actually test iPhone HEIC/HEIF, iCloud, Live Photos, multi-select, orientation and safe conversion options; do not assert support before data.
- **W16, #30:** adversarial/upload/CSRF/auth/rate/cleanup/Windows-firewall tests, real Safari → Windows → Watch4 Gallery confirmation and old desktop transfer regression. Public release only after explicit security acceptance.

These milestones can begin alongside the Bluetooth path as a **separate task/chat** once a shared interface contract is agreed. W11 [#24](https://github.com/aidarnig/WristPorter/issues/24) remains the **only** owner of Windows APK installation/update; it shares the Desktop shell but is never exposed as a remote web command. W12 Bridge beta is **not blocked** by W13–W16; local Web Upload can ship independently.

**Long-term research only:** a watch-hosted on-demand Wi-Fi web uploader could theoretically remove the Windows dependency, but requires separate Android storage, camera/gallery, privacy and battery review. **Not an approved W13–W16 deliverable.**

## Existing completed desktop history and deferred non-blockers

M0 Foundation, M1 ADB connection, M2 photo import, M3 transfer, M4 desktop UX, M5 desktop QA and M6 initial stable releases were shipped and refined through Desktop **v1.1.2**. Historical acceptance and outstanding Windows QA limitations stay in [QUALITY.md](QUALITY.md) and existing Issues [#2](https://github.com/aidarnig/WristPorter/issues/2), [#4](https://github.com/aidarnig/WristPorter/issues/4), [#7](https://github.com/aidarnig/WristPorter/issues/7), [#8](https://github.com/aidarnig/WristPorter/issues/8), [#9](https://github.com/aidarnig/WristPorter/issues/9).

**Deferred:** installed iOS companion, direct browser-to-watch with no Windows PC, and real video/GIF showcase [#10](https://github.com/aidarnig/WristPorter/issues/10). iOS app and direct iPhone photo transfer are **not part of this roadmap**. Do not reopen them casually in W-series implementation.

[Russian orientation](ROADMAP.ru.md) · [Engineering standard](ENGINEERING_STANDARD.md) · [Current handoff](STATE.md).
