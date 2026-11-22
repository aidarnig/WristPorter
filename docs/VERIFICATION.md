# Verification ledger — desktop evidence and future Bridge tests

**This is an evidence ledger, not an automatic CI report.** All new watch-to-iPhone results start **NOT TESTED**. Keep private device data out of GitHub. For accepted Desktop evidence see [QUALITY.md](QUALITY.md).

## Claim-evidence rule

- **PASS:** observed by tester on stated watch/iOS version and scenario, with reproducible steps.
- **FAIL:** tested and observed failure; record symptoms, software builds and safe reproduction.
- **BLOCKED:** couldn't run because required permission, access, hardware or dependency is missing.
- **NOT TESTED:** not run (the default; do not turn a successful build into PASS).
- **INCONCLUSIVE:** observation ambiguous; document uncertainty and retry plan.

All observations must include **date, app build/commit, watch model/firmware/API, iPhone model/iOS, exact behavior and sanitized evidence**. No guesswork. Counts/durations/power figures must be measured, not assumed.

## Existing Desktop reference

- **Desktop Watch4 Classic Gallery transfer:** confirmed eight-photo Gallery visibility in 2026-11-11 trial; [historical details](QUALITY.md#manual-verification-log).
- **Desktop v1.1.2 screenshots:** EN/RU user visual acceptance, 9/9 application-reported transfer; this is not an independent later Gallery verification.
- **Bridge ANCS/AMS on Watch4 Classic:** **NOT TESTED**.

## Desktop #45 — Queue order versus Samsung Gallery order (2026-11-22)

**Tester:** project owner, interactive report in [Issue #45](https://github.com/aidarnig/WristPorter/issues/45). **Application:** existing Desktop v1.1.2 (production ordering path in `main` prior to [draft PR #66](https://github.com/aidarnig/WristPorter/pull/66)); exact installed executable/checksum not reconfirmed. **Watch:** Galaxy Watch4 Classic; Android 16/API36 previously confirmed on the same day during W0, **not rechecked during this photo test**. Windows build not reported. iPhone not involved. **Fixture:** 16 numbered, non-private JPEG/PNG test images, with intentionally scrambled EXIF dates and modification times.

| ID | Hardware check | Status | Owner-observed result |
| --- | --- | --- | --- |
| DESK-ORDER-01 | Add 16 synthetic photos as a folder, verify visible queue and journal, send once and inspect watch Gallery | **FAIL (Gallery ordering)** | Desktop queue and journal were `01..16`; Galaxy Gallery displayed `01-04-03-02-07-06-05-10-09-08-13-12-11-16-15-14`. All 16 present |
| DESK-ORDER-02 | Prior real-world batch of ~70 personal photos followed by individually initiated sends | **FAIL (batch), PASS (owner-reported manual workaround)** | Some images were out of order in the original bulk transfer, whereas sending the same photos one at a time in the desired order produced the expected Gallery sequence. No private photos or raw device data shared |

**Interpretation:** Synthetic Gallery sequence does not match ascending/descending source EXIF capture time or source file modification time. The repeated three-image reversals **suggest**, but do not prove, a MediaStore indexing/timestamp tie. `MEDIA_SCANNER_SCAN_FILE` acknowledges a broadcast request, **not completion of media indexing**. The minimum-delay candidate was **not yet tested at this initial failure checkpoint**. The later targeted ordered-preview test is recorded below; neither result establishes a universal Gallery ordering guarantee. Do not claim PASS based on CI or the reported 16/16 transfer count.

## Desktop #45 — Ordered pacing preview accepted on reference Watch4 (2026-11-22)

**Tester:** project owner; **build:** [draft PR #66 Windows preview run #45048471637](https://github.com/aidarnig/WristPorter/actions/runs/45048471637), commit `05aa185576bb9b21f1fbb2a2e3da132305ff9ac9`. **Device:** Galaxy Watch4 Classic (Android 16/API36 previously observed on same date; exact firmware not independently reconfirmed). **Fixture:** 12 distinct artificial JPEG/PNG photos labelled `B01`–`B12`. **Result: PASS (owner-reported)** — the owner states all new photos appeared in Samsung Gallery in the **same order as the PC queue** using the paced build, whereas a previous default-speed 16-photo test failed. The owner also observed that this build's transfer was **substantially slower**; no precise duration was measured.

**Limit:** The observed PASS validates the paced behavior for **this fixture and watch**, not a universal Gallery guarantee or every 70+ photo scenario. The new Fast/Ordered mode selector requested after this test is a separate code change; its UI and saved preference have **no hardware verification yet**. No private files were uploaded, and no photo bytes or EXIF were changed.

## W14-A — Desktop regression on physical Galaxy Watch4 Classic (2026-11-22)

**Tester:** project owner, interactive report in Issue #38 executor chat. **Artifact:** PR #43, test-only Windows preview [Actions run #45024705910](https://github.com/aidarnig/WristPorter/actions/runs/45024705910), commit `0df7472b47b79e976c24b08a8231d8768bbb82d6`, Desktop internal version 1.1.2. **Device:** Galaxy Watch4 Classic; exact watch firmware/Android API and Windows OS build **not reported**. No iPhone involved.

| ID | Manual scenario | Result | Actual owner-observed outcome |
| --- | --- | --- | --- |
| W14A-DESK-01 | Selected watch, transfer one photo, inspect Gallery | **PASS** | Photo sent and visible in Galaxy Watch4 Classic Gallery |
| W14A-DESK-02 | Transfer batch larger than 20 photos | **PASS** | Owner reports 20+ photos all transferred; precise count and Gallery confirmation for each batch item not separately specified |
| W14A-DESK-03 | Cancel during a 15-photo batch at 10/15 | **PASS (stop)** | Transfer stops on Cancel; no claim of device-side cleanup of in-flight photo |
| W14A-DESK-04 | Press Send after cancellation and check already transferred files | **FAIL (resume UX)** | Progress restarts at 0/15 and already transferred photos are sent again; duplicate local-to-watch pushes observed, but exact extra Gallery entries not counted |
| W14A-DESK-05 | After a successful photo, leave it in queue, add 10 more and press Send again | **FAIL (incremental send)** | Owner reports first photo is sent again along with 10 new photos; reported from prior Windows preview, not retested on corrected build |

**Disposition:** W14-A transfer/cancel hardware baseline verified; resume and incremental-queue behaviors were found defective on the first preview; both have proposed fixes and require targeted owner retests on the new build before PR merge. No claims about Safari, HTTP, iOS, scan-only retry under injected failure or alternate devices. For interruptions mid-ADB push, remote copy completion may be ambiguous without a callback; do not promise perfect deduplication of that in-flight file.

## W14-A — Owner retest after fixes (2026-11-22)

**Tester:** project owner (direct chat report: "Оба сценария сработали верно"). **Target build:** current-HEAD Windows preview [run #45028266585](https://github.com/aidarnig/WristPorter/actions/runs/45028266585), commit `7821acbfd7a21f353a0def62680293c72e4e56b3`, which was provided immediately before this feedback. Owner did not independently state ZIP checksum, watch firmware or Windows build in this confirmation. Treat the two scenario results as owner-reported, not independent automated/Gallery verification.

| ID | Retest scenario | Result | Actual owner report |
| --- | --- | --- | --- |
| W14A-DESK-06 | Successful photo retained in queue, append 10 new photos, send only new items | **PASS (owner)** | Confirms the previously reported 1+10 duplicate-copy regression is fixed; both prescribed scenarios "worked correctly" |
| W14A-DESK-07 | Cancel a roughly 15-photo queue after partial progress, then Continue only outstanding items | **PASS (owner)** | Confirms cancel→Continue behavior is correct; previously completed items no longer retriggered as in the original bug |

**Disposition:** Both original defects remain recorded as FAIL on the first preview (historical evidence), and both fixed scenarios are now **PASS on owner retest**. This does **not** verify other watch models, actual Gallery counts for each new image, scan-only retry under injected failure or HTTP/Safari. **Independent review of PR #43 at latest head is still REQUIRED before merge**; do not close #38 solely on this test evidence.

## W0 — initial real-device probe (owner checkpoint 2026-11-22)

| ID | Scenario | Status | Evidence requirements |
| --- | --- | --- | --- |
| BLE-00 | Record Watch4 Classic firmware/API (`getprop ro.build.version.sdk`) | **PASS (owner)** | Watch4 Classic, Android 16, API 36, firmware display prefix `BP2A…` (full build string not supplied) |
| BLE-01 | Record selected iPhone model, iOS version, Bluetooth status | **INCONCLUSIVE** | iOS 26.5.2, Bluetooth on and watch shown connected; exact iPhone model not reported |
| BLE-02 | Explicit discoverable watch and iOS pairing/bond with matching confirmation | **INCONCLUSIVE** | iPhone Bluetooth UI displayed watch as «Подключено» after probe discoverability; matching-code confirmation not separately reported; no notification-sharing dialog appeared |
| BLE-03 | Watch opens authorized GATT client to iPhone; ANCS/AMS discoverability | **PASS (owner)** | GATT connected status=0; ANCS=true, AMS=true; iPhone notification-sharing consent; ANCS/AMS CCCD subscription statuses=0 |
| BLE-04 | Fresh incoming iPhone notification reaches watch via ANCS | **PASS (owner)** | Following directed fresh-notification test, owner confirmed ANCS events appear in watch probe; category/count not supplied, no content recorded |
| BLE-05 | iPhone music metadata and supported command received via AMS | **INCONCLUSIVE (partial event proof)** | After directed track-switch test owner confirms AMS events; earlier player entity=0 attribute=0/1 events and Remote Command count=2 observed; exact fresh track attribute and command execution unverified |
| BLE-06 | Disconnect/restore and Bluetooth off/on | **PASS (manual reconnection on v0.0.4; historical failure retained)** | On v0.0.4, iPhone Bluetooth OFF/ON + explicit bonded-phone reselection restored GATT, ANCS/AMS CCCDs=0, AMS registrations=2/2 and AMS events=3; automatic/background recovery NOT VERIFIED. Earlier v0.0.1/v0.0.3 errors preserved below |
| BLE-07 | Screen off/watch face for a short initial observation | NOT TESTED | Whether service remains alive; time and battery |
| BLE-08 | Explicit disconnect halts retries and advertisement | NOT TESTED | No residual GATT/foreground service |
| BLE-09 | Read optional Battery, Device Information, Current Time services | **INCONCLUSIVE** | Battery, DeviceInfo, Time service UUIDs discovered; characteristics/values were not read |
| BLE-10 | Investigate minSdk candidate based on reference Android build | **PASS (development choice only)** | Actual reference Watch4: Android 16/API 36. Use minSdk=30 as W1 initial **development floor** (already compiles in W0 Gradle); no hardware proof for API30–35, and broad support is NOT VERIFIED. Keep targetSdk separately aligned with current Wear OS policy |

**W0 owner evidence (2026-11-22):** User-installed experimental debug probe from [PR #34](https://github.com/aidarnig/WristPorter/pull/34), [Android CI run #45024297957](https://github.com/aidarnig/WristPorter/actions/runs/45024297957), source commit `5b8e2c9d41a65293020089b9ca49a6f64b93f83b`. Owner read Android **16 / API 36** from the watch probe UI and reported only firmware prefix **BP2A…** (full build string deliberately omitted); owner reported **iOS 26.5.2** with the watch showing **Connected** in iPhone Bluetooth settings, but no popup for sharing notifications. Device model of iPhone and matching pairing code observation are not yet reported; do not equate iOS “Connected” with ANCS/AMS GATT authorization. No private identifiers were retained. Exact installed binary checksum was not confirmed independently. Owner mentioned a **planned** iOS update to 27.2.1 (not an observed version). **BLE-03…BLE-10 were NOT TESTED at this first checkpoint; subsequent on-device results below supersede that snapshot.** Next check at the time was bonded phone selection and ANCS/AMS discovery. See [Issue #13 evidence comment](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7081269183). No production minSdk decision yet.

**W0 GATT/ANCS/AMS owner observation (2026-11-22):** On the existing experimental APK ([run #45024297957](https://github.com/aidarnig/WristPorter/actions/runs/45024297957), [PR #34](https://github.com/aidarnig/WristPorter/pull/34)), owner tapped **3. Choose bonded phone**, selected their own iPhone and explicitly accepted iPhone's **share notifications with this device** permission request. On the watch: `Link state=connected, GATT status=0`; `ANCS present=true`, `AMS present=true`; `Battery=true`, `DeviceInfo=true`, `Time=true`. ANCS Notification Source, AMS Remote Command and AMS Entity Update descriptor subscriptions all returned `status=0`. AMS advertised **2** remote commands (not sent/tested), returned `AMS metadata subscription request status=0`, and delivered `AMS metadata event entity=0 attribute=0` and `entity=0 attribute=1`, both `truncated=false` (player attributes, not verified track-change events). Owner supplied photo privately in executor chat; public repository records only sanitized statuses, no raw/private data. **BLE-03 PASS (owner); BLE-05 and BLE-09 INCONCLUSIVE; BLE-04 NOT TESTED** because an actual fresh incoming ANCS notification and actual playback/track change were not yet tested. The initial matching-pairing-code confirmation remains undocumented, so BLE-02 stays INCONCLUSIVE despite operational bond/GATT. The iPhone model remains unreported and last confirmed iOS is **26.5.2**. See [Issue #13 checkpoint](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7081378341). No claims of background reliability, production support, full notification content, media control or decided minSdk. Next minimal meaningful test: receive one fresh non-sensitive iPhone notification and switch a track while the probe is connected; record only event summaries.

**W0 live-event test, owner report (2026-11-22):** After the GATT/ANCS/AMS subscription check, the owner was instructed to trigger one fresh, non-sensitive iPhone notification and a music track change while the W0 probe remained connected. Their response was **“AMS и ANCS события появляются”** (“AMS and ANCS events appear”). This is owner-reported hardware proof of ANCS event reception (**BLE-04 PASS**) and AMS event reception during the directed playback test (**BLE-05 partially demonstrated**). No exact fresh AMS track entity/attribute ID, remote-command execution, number/category of notifications or visible media values were supplied; the full BLE-05 acceptance remains **INCONCLUSIVE**. No personal content, screenshots or device identifiers were placed in GitHub. [Issue #13 follow-up](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7081444727). Before deciding W0 completion, test one controlled explicit disconnect / manual reconnect (and, if needed, iPhone Bluetooth off/on) without deleting any existing pairing. No background/long-term stability or production minSdk claim.

**W0 disconnect/reconnect test, owner report (2026-11-22):** On the experimental debug APK ([run #45024297957](https://github.com/aidarnig/WristPorter/actions/runs/45024297957)), owner tapped **4. Disconnect** then manually reselected their iPhone in **3. Choose bonded phone**. They did **not** notice a repeated GATT status line, but **new notification and music events resumed immediately** (functional PASS for this simple manual reconnect). Owner then switched **iPhone Bluetooth OFF/ON**. iPhone showed rapid system-level reconnection to the watch, and the owner manually selected the iPhone again in W0 Probe; the watch displayed **`Link state=connected, GATT status=0`**. Despite subsequent track switches, **no new AMS events appeared**. They did not report ANCS results, GATT service discovery, CCCD statuses, or AMS registration statuses after the Bluetooth toggle, so **root cause cannot yet be attributed to iPhone, service publication, subscription or client code**. Overall BLE-06 is **FAIL (partial recovery)** for the W0 end-to-end event recovery requirement, while transport reconnection itself succeeded. No unpairing, reset, or destructive operations. [Issue #13 report](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7081563011). A bounded diagnostic fix is being investigated in experimental PR #34; confirmation requires a targeted new-APK retest, **not** an assumption that the fix works. Background use, full media commands and production minSdk remain unverified.

**W0 post-toggle screenshot, owner report (2026-11-22):** The owner supplied a private photograph after reconnecting the watch to iPhone following a Bluetooth OFF/ON. On-screen sanitized facts: `Link state=connected, GATT status=0`; `ANCS present=true; AMS present=true`; `Subscribed ANCS Notification Source status=0`; **`Subscribed AMS Remote Command status=1`; `Subscribed AMS Entity Update status=1`**, and a visible `AMS metadata subscription request status=1`. Owner reported **no new events**. This narrows the observed failure from merely missing AMS events to **non-success AMS characteristic subscription results** while the GATT link and ANCS subscription are successful, but does not prove their root cause. **Exact installed APK identity is not established by this screenshot:** v0.0.2 implementation should defer AMS registration until Entity Update CCCD succeeds, so this image alone must not be treated as proof that v0.0.2's recovery logic failed (a previous parallel app may have been opened). The same photo shows diagnostics clipped by the watch's circular edge, with insufficient trailing scroll space. Owner requested round-safe side margins and scroll slack, implemented for a new isolated W0 diagnostic build in draft [PR #34](https://github.com/aidarnig/WristPorter/pull/34); compile/test and real-device UI fit are separate verification gates. [Issue #13 evidence](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7081704258). No private photo, BLE address or personal content committed.

**W0 v0.0.3-round diagnostic snapshot, owner photo (2026-11-22):** Owner explicitly showed `v0.0.3 round | Latest session` from [W0 Android CI run #45041246590](https://github.com/aidarnig/WristPorter/actions/runs/45041246590), experimental draft [PR #34](https://github.com/aidarnig/WristPorter/pull/34). On-screen **Session #1** summary: `link=connected`, `discoveries=1`; `ANCS present=true sub=0 events=0`; `AMS present=true commandSub=3 entitySub=0`; `AMS registrations=0/1 events=0` and `AMS metadata subscription request status=3`. **GATT code 3 = WRITE_NOT_PERMITTED**; the iPhone exposed the AMS service, but one AMS CCCD subscription and one AMS entity-registration write were rejected while AMS Entity Update CCCD itself succeeded. The image captures only one registration result (`0/1`); do not assert the second queued registration completed or that toggling Bluetooth was repeated on this build. The probe uses write-with-response for AMS Entity Update, consistent with Apple's AMS specification; root cause and whether a one-time GATT rediscovery helps remain **UNVERIFIED**. The photo shows the **entire session summary legible inside the circular watch display**, improved over earlier truncation; touch scroll responsiveness has not been independently reported. This v0.0.3 failure is a **new observed setup failure**, not proof all iPhones or all sessions fail. See [Issue #13 comment](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7081829785); do not publish the private photograph or device identifiers. Next step: one manual **5. Refresh GATT** on the same v0.0.3 app, compare sanitized subscriptions and registration/event counters. Existing BLE-05 status remains INCONCLUSIVE (historical AMS events confirmed on v0.0.1); BLE-06 remains FAIL for its earlier Bluetooth-toggle test until recovery is shown.

**W0 v0.0.3 manual refresh follow-up (owner report, 2026-11-22):** After being asked to press `5. Refresh GATT` **once** on the already connected v0.0.3 Round Probe, the owner replied **“ошибка 3 сохранилась”** (“error 3 persisted”). The report does not identify exactly which of the AMS Remote Command CCCD or Entity Update metadata-registration callbacks retained status 3 after refresh; it does establish that **manual rediscovery was not a sufficient remedy**. The former screenshot's `AMS registrations=0/1` count may lag the final callback outcome because source inspection found session-summary rendering occurred before some callback state updates; the actual logged `status=3` GATT failure remains valid. An instrumented v0.0.4 `W0 GATT Inspector` is being prepared in [draft PR #34](https://github.com/aidarnig/WristPorter/pull/34) to report safe characteristic properties, CCCD presence and **local start vs remote callback** results without introducing undocumented write types or automatic retries. Independent BLE review and real-device test remain pending, no stable recovery assertion. See [Issue #13 report](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7081876840) and [callback summary correction](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7081908971). No personal content or identifiers recorded.

**W0 v0.0.4 GATT Inspector — successful foreground session (owner photo, 2026-11-22):** On the owner's Galaxy Watch4 Classic the `v0.0.4 inspector | Latest session` screen from [Android CI run #45042600288](https://github.com/aidarnig/WristPorter/actions/runs/45042600288), [draft PR #34](https://github.com/aidarnig/WristPorter/pull/34), source `0a615789f97cf5509596fb2fcc15151f3e0d9df3`, showed `Session #1 link=connected`, `discoveries=1`, `ANCS present=true sub=0 events=87`, `AMS present=true commandSub=0 entitySub=0`, `AMS registrations=2/2 events=3`, `Last AMS issue: none`, `GATT operations: queued=0 active=false`. The screenshot's preceding log also showed an AMS metadata event ending in `attribute=2 truncated=false`, but the full entity label was outside the visible area; no private attribute value was retained. Event counts are **callback counts**, not unique notification or track counts. This is **PASS for this initial live session's ANCS/AMS subscription, AMS registration and event reception**, after the previous v0.0.3 `status=3` issue; it does **not** establish the cause of recovery, actual remote command execution, Bluetooth OFF/ON reconnection recovery, screen-off/background robustness, or support for other OS/model versions. In particular, historical **BLE-06 FAIL (partial recovery)** remains until a specific v0.0.4 Bluetooth-toggle retest; complete BLE-05 criterion (including command operation) remains INCONCLUSIVE although media-event receipt is verified. The rounded-screen session summary is fully visible in the owner's photo. No personal screenshot, event content or device identifiers committed. [Issue #13 v0.0.4 evidence](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7082012270).

**W0 v0.0.4 Bluetooth OFF/ON recovery, owner photo and report (2026-11-22):** While opening iPhone Bluetooth settings, W0 Inspector on Watch4 displayed `Disconnected: activity no longer visible`. Source check confirms the **foreground-only experimental app explicitly calls `disconnect()` in Android `Activity.onStop()`**, so this is expected probe behavior when its Activity becomes invisible, not evidence of an iPhone link fault; the exact reason the Activity stopped (e.g., screen timeout) was not established. After toggling iPhone Bluetooth OFF/ON, iPhone reconnected to the watch at the system Bluetooth layer, but **did not automatically reconnect the probe GATT client**. Owner manually selected the existing bonded iPhone. Photo explicitly identified `v0.0.4 inspector`, `Session #2 link=connected`, `discoveries=1`, `ANCS present=true sub=0 events=88`, `AMS present=true commandSub=0 entitySub=0`, `AMS registrations=2/2 events=3`, `Last AMS issue: none`, `GATT operations: queued=0 active=false`. The **manual** recovery of protocol subscriptions and delivered event callbacks is **PASS on v0.0.4** for the original W0 feasibility experiment. AMS events could include initial metadata following subscription; a deliberate new post-toggle track switch and autonomous BLE recovery were not separately confirmed. **Historical failures** after Bluetooth toggles and write status=3 remain recorded above as genuine regressions on earlier probe versions; do not rewrite them as never occurring. Screen-off/background continuity and bounded automatic reconnect are **not provided by this probe** and are deferred to the separately planned [W7 Issue #20](https://github.com/aidarnig/WristPorter/issues/20) after W2–W5, with measured reliability/battery gates. No private image, identifiers, event contents or pairing secrets committed. [Issue #13 evidence](https://github.com/aidarnig/WristPorter/issues/13#issuecomment-7082090849).

**W0 minimum Android SDK development decision (2026-11-22):** The only owner-tested reference Watch4 reports **Android 16/API 36**. The isolated experimental APK compiles and runs on that reference with `minSdk=30` (experiment configuration); this does **not** demonstrate device support on API 30–35. **Decision for W1 scaffolding:** provisionally keep `minSdk=30` to avoid unnecessarily excluding Wear OS 3-era watches; guard newer Android platform APIs and test other hardware before any compatibility or support announcement. **Do not conflate minSdk with targetSdk:** as of August 31, 2026, Google's [Wear OS Play policy](https://developer.android.com/google/play/requirements/target-sdk) requires target **API 35 or newer** for newly submitted Wear OS apps. W1 must separately select a compliant target SDK and verify behavior. This is an initial development floor, **not** product owner sign-off on all lower API/firmware combinations or a Wear Bridge release.

**W0 completion condition:** at minimum BLE-00..05 observed or recorded FAIL/BLOCKED with a reproducible report; before proceeding to W2, the product owner reviews whether BLE is feasible on this particular watch and iPhone.

**W0 final coordinator acceptance (2026-11-22): ACCEPTED FOR FOREGROUND PROTOCOL FEASIBILITY ONLY.** The owner-observed Watch4 Classic/API36 ↔ iPhone GATT session, fresh ANCS notifications, AMS event receipt and manual Bluetooth OFF/ON → user-selected GATT/subscription restoration answer the exploratory W0 feasibility question. The original trial failures (AMS GATT status 1/3) remain recorded above; the cause of intermittent errors was not proven. **BLE-01** iPhone model, **BLE-02** initial matching-code observation and parts of **BLE-05** media command behavior remain incomplete; this is an explicit evidence limitation, not a claim that every scenario passed. No persistent background connection, automatic recovery, battery profile, remote player-command execution or API30–35 compatibility has been demonstrated. [PR #34](https://github.com/aidarnig/WristPorter/pull/34) passed experimental CI and independent BLE/privacy review but was **closed without merging**: disposable diagnostic code, not a shippable product component. Original reproducible source and test artifacts remain linked from that PR and [Issue #13](https://github.com/aidarnig/WristPorter/issues/13). **W0 accepted, Bridge unimplemented; no further W0 device testing requested.**

## Reliability scenarios (plan; not evidence)

| ID | Scenario | Expected safety property |
| --- | --- | --- |
| REL-01 | Watch face / screen off / AOD / lock and unlock phone | Bridge not falsely “connected” if services are unavailable; no runaway wakeups |
| REL-02 | Leave range, return | Bounded recovery; no duplicates/stale media actions |
| REL-03 | Watch reboot / process killed | Correct permission/lifecycle restart; no permission bypass |
| REL-04 | iPhone reboot, Bluetooth toggle, iOS Focus, ANCS permission change | Clear status and recovery steps |
| REL-05 | Service Changed, ANCS/AMS disappears/reappears | Re-discover, invalidate old UID/command state |
| REL-06 | 100+ notification burst / update/remove race | Bounded memory and no resurrection of removed items |
| REL-07 | Several media apps and track changes | No phantom artist/track and no obsolete command execution |
| REL-08 | Incorrect pairing/deny permission/revoke access | No silent pairing, untrusted fallback or infinite loop |
| REL-09 | Watch system media tile after app closed | Explicit verification of Samsung OS behavior |
| REL-10 | 12h / 24h / 72h connected idle + routine use where feasible | Measured stability with exact test duration; no untested uptime claim |
| REL-11 | Desktop pushes JPEG/PNG after bridge installation | No regression of Gallery transfer |
| REL-12 | APK update/rollback | Old settings retained or explicitly migrated, no silent unbond |

Hardware tests should be run **in addition to** pure protocol and Android integration tests; use failures to refine scope.

## Battery/thermal protocol — proposed, not measured

Use the same watch firmware, brightness/AOD, Wi-Fi, SIM/LTE use, Bluetooth, notification traffic, wrist detection and battery health wherever possible. Record all uncontrolled differences. Compare:

- **PWR-BASE:** bridge disabled/not running; similar user activity.
- **PWR-IDLE:** bridge paired, subscribed, no active media; low notification traffic.
- **PWR-NOTIFY:** agreed count/rate of iPhone notifications, screen on/off documented.
- **PWR-MEDIA:** sustained playback controls and metadata while iPhone plays audio.
- **PWR-LOSS:** leave range, return, toggle Bluetooth to test reconnection energy cost.
- **PWR-OVERNIGHT:** measured overnight idle period to check wakeup/thermal and battery trends.

For each trial: start/end local time and percentage, run hours, charging state, screen-on/AOD, ambient/signal context, configuration and app commit. Calculate simple percentage-points/hour with clear caveat **battery % ≠ energy-meter precision**. Repeat baseline/bridge samples before proposing an acceptance threshold. Do **not** promise “<N% per day” without hardware data. Battery data and thermal effects must be treated as approximate device observations.

### Sanitized record template

```text
Test ID:
Date (UTC):
Issue / PR / commit:
Desktop version (if involved):
Bridge APK version / signing flavor:
Watch model + firmware + Android API:
iPhone model + iOS:
Network / Bluetooth / power settings (no IDs):
Test steps and expected result:
Actual result:
Status: PASS | FAIL | BLOCKED | NOT TESTED | INCONCLUSIVE
Elapsed time / notification count / app player:
Battery initial/final/charging, if relevant:
Evidence location (sanitized or private, never raw identifiers):
Known limitations / follow-up Issue:
Tester confirmation:
```

Only add facts actually observed. Update status in linked Issues, not through optimistic roadmap text.

## Planned iPhone Safari → Windows → Watch Web Upload tests (all NOT TESTED)

The native Desktop photo-transfer result above **does not prove** any iPhone browser upload. These cases are separate from Watch ↔ iPhone ANCS/AMS tests and must be executed on real hardware.

| ID | Scenario | Status | Acceptance evidence |
| --- | --- | --- | --- |
| WEB-00 | Windows vNext defaults to **no listening LAN port** | NOT TESTED | Listener/socket inspection, startup/shutdown |
| WEB-01 | Start Web Upload on approved private network; QR opens Windows portal from iPhone Safari | NOT TESTED | Exact iOS/browser, Windows OS, network topology |
| WEB-02 | Pairing code + session expiry + revoke/Stop | NOT TESTED | Accept/deny/rate-limit and manual stop results |
| WEB-03 | Upload 1 JPEG and 10+ JPEG/PNG from iPhone Photos/Files | NOT TESTED | Per-file hashes/count/status and memory usage |
| WEB-04 | Windows copies selected files over ADB to Watch4 and Gallery displays them | NOT TESTED | Real model/firmware and user-observed Gallery |
| WEB-05 | Selected watch changes mid-session, browser cannot change device | NOT TESTED | Invalidation and wrong-device prevention |
| WEB-06 | Network isolation / firewall / VPN / PC sleeps | NOT TESTED | Clear failure status without security bypass |
| WEB-07 | Invalid HEIC, oversized/malformed images, path traversal, zip/executable disguised as JPG | NOT TESTED | Correct rejection, no persistence/resource exhaustion |
| WEB-08 | Simultaneous Windows photo queue and browser jobs | NOT TESTED | Serialized transfers, correct cancel/retry and scan-only semantics |
| WEB-09 | Invalid CSRF/Host/Origin, brute-force and forged browser origins | NOT TESTED | Unauthorized requests rejected |
| WEB-10 | File/temp cleanup, staged photo privacy, stop/restart/crash recovery | NOT TESTED | No residual public access or old sessions |
| WEB-11 | Actual iPhone photo selection: JPEG vs HEIC, Live Photo, rotation, iCloud, multi-select | NOT TESTED | Exact MIME+magic/format and expected UX |
| WEB-12 | Existing Windows-only JPG/PNG Gallery transfer after Web Upload feature | NOT TESTED | Regression compared to v1.1.2 evidence |
| WEB-13 | Local HTTP plaintext threat / proposed TLS onboarding | NOT TESTED | Security decision and user-facing warnings |
| WEB-14 | Windows package with browser mode off | NOT TESTED | EXE opens, bundled ADB works, no web runtime dependency |

**Testing instruction:** record local browser URL/IP, device address and PIN privately only. Public issue/PR may include non-sensitive Windows/iOS/watch versions and test status. Log detailed results using the sanitized template above. The strict W13–W16 gates live in [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md) and [QUALITY.md](QUALITY.md).

## W14-B — Real Safari to Windows test-only PIN gateway (2026-11-22)

**Tester:** project owner, directly reported in the W14-B Issue #39 executor chat. **Test artifact:** no-install Windows `wristporter-w14b-gateway-probe-45035303499`, [Actions run #45035303499](https://github.com/aidarnig/WristPorter/actions/runs/45035303499), commit `4c4c892b72c6310dc720ea8eb5636711159a4d5e` (PR #44). **Client:** iPhone Safari on iOS **26.5.2**; iPhone model and Windows version/build not stated. **Network:** owner-operated local Wi-Fi, actual network trust and firewall configuration not independently audited; no private address or PIN recorded. **No watch or photos used:** test server exposes a pairing/status probe only.

| ID | Owner-assisted real-device scenario | Result | Sanitized observation |
| --- | --- | --- | --- |
| W14B-LAN-01 | iPhone Safari opens explicitly started Windows test-only HTTP gateway | **PASS (reachability)** | Safari loaded the served PIN page and submitted the form; this demonstrates LAN HTTP reachability, not a shipped Web Upload interface |
| W14B-AUTH-01 | Earlier test PIN form submission on iPhone Safari | **FAIL (historical)** | `Origin rejected` occurred with previous probe builds; implementation investigation identified server `Referrer-Policy: no-referrer` as a spec-consistent cause of opaque `Origin: null`. Actual Safari request headers were not captured |
| W14B-AUTH-02 | Retry one-time PIN on corrected gateway with `Referrer-Policy: same-origin` | **PASS (owner)** | On iOS 26.5.2 owner explicitly reported `Paired (test only)` from the tested Windows build |
| W14B-STOP-01 | Press ENTER in Windows console, then reload Safari; listener should be unreachable | **PASS (owner)** | After pairing, owner confirmed that the previously accessible Safari page stopped opening following the instructed Windows stop and browser retry; no direct port scan or process audit performed |
| W14B-NEG-01 | Wrong/expired PIN, Host/Origin/CSRF abuse, rate-limit, session expiry and revocation on real iPhone/Windows LAN | **NOT TESTED on hardware** | Automated JUnit/PR gate passed separately; those do not prove real LAN attack resistance |

**Scope limitation:** W14B-AUTH-02 verifies only the explicitly invoked **test-only PIN session endpoint**. It does not verify real photos, Safari file selection, JPEG/PNG upload staging, ADB transfers, Galaxy Gallery visibility, Windows bundled Desktop integration, production Desktop-integrated start/stop, or the overall WEB-01/WEB-02/WEB-09 acceptance rows above. Public/stable release remains unapproved, and a **separate independent security review of PR #44's latest HEAD** is required before merge. Do not share local IP, pairing code, session cookie or photo contents in public logs/issues.
