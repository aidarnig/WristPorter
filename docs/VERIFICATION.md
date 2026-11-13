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

## W0 — initial real-device probe (all NOT TESTED)

| ID | Scenario | Status | Evidence requirements |
| --- | --- | --- | --- |
| BLE-00 | Record Watch4 Classic firmware/API (`getprop ro.build.version.sdk`) | NOT TESTED | Model/firmware/API only, no serial |
| BLE-01 | Record selected iPhone model, iOS version, Bluetooth status | NOT TESTED | Hardware and OS version, not Bluetooth address |
| BLE-02 | Explicit discoverable watch and iOS pairing/bond with matching confirmation | NOT TESTED | UI sequence and errors redacted |
| BLE-03 | Watch opens authorized GATT client to iPhone; ANCS/AMS discoverability | NOT TESTED | Exposed service set, permission behavior |
| BLE-04 | Fresh incoming iPhone notification reaches watch via ANCS | NOT TESTED | App category / sanitized timing, no message text |
| BLE-05 | iPhone music metadata and supported command received via AMS | NOT TESTED | Player type and command support, no private metadata |
| BLE-06 | Disconnect/restore and Bluetooth off/on | NOT TESTED | Transition states, retry count, reconnect result |
| BLE-07 | Screen off/watch face for a short initial observation | NOT TESTED | Whether service remains alive; time and battery |
| BLE-08 | Explicit disconnect halts retries and advertisement | NOT TESTED | No residual GATT/foreground service |
| BLE-09 | Read optional Battery, Device Information, Current Time services | NOT TESTED | Which exposed, values type, permission |
| BLE-10 | Investigate minSdk candidate based on reference Android build | NOT TESTED | Exact actual API and resulting ADR |

**W0 completion condition:** at minimum BLE-00..05 observed or recorded FAIL/BLOCKED with a reproducible report; before proceeding to W2, the product owner reviews whether BLE is feasible on this particular watch and iPhone.

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
