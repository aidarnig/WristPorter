# CURRENT PROJECT STATE — read first on every new chat

**Snapshot date:** 2026-11-13. This is a documented planning baseline, **not** a claim that later GitHub commits/PRs do not exist. Check live `main`, open PRs and Issues before relying on it.

## What the product actually ships

- Public repository: https://github.com/aidarnig/WristPorter
- Shipped stable: **WristPorter Desktop v1.1.2** — Windows x64 portable ZIP, Java 25/Maven/JavaFX, bundled Java runtime + ADB, wireless ADB pairing, JPEG/PNG import/preview/transfer and Samsung Gallery indexing requests, EN/RU and light/dark.
- **Confirmed reference scenario:** Samsung Galaxy Watch4 Classic; Galaxy Gallery visibility of a real eight-photo batch has been confirmed. The desktop UI was later visually accepted in EN/RU with a 9/9 app-reported batch. Details: [QUALITY.md](QUALITY.md).
- Desktop modules: `watch-domain`, `watch-adb`, `watch-core`, `watch-desktop`; no watch APK source exists at this baseline.
- Current GitHub Social Preview is configured by the repository owner. GIF/video remains deferred in [issue #10](https://github.com/aidarnig/WristPorter/issues/10).
- Windows SmartScreen: executable is not trusted-publisher-signed; see [SECURITY.md](SECURITY.md).

## Decision for the new program (PLANNED ONLY)

Build **WristPorter Bridge**, an independent Kotlin Wear OS app that talks to iPhone directly via its built-in Bluetooth **ANCS/AMS** services, without any iOS companion app. Aim for maximum **documented, observed** features with reliability, low battery impact, privacy and accessibility as first-class constraints.

- Reference watch: Samsung Galaxy Watch4 Classic; actual BLE ↔ iPhone ability **NOT TESTED BY WRISTPORTER**.
- First task: **[W0 — hardware/protocol feasibility, #13](https://github.com/aidarnig/WristPorter/issues/13)**. Do not start wide implementation until W0 evidence exists.
- Roadmap: [ROADMAP.md](ROADMAP.md), milestones W0–W12, [Issues #13–#25](https://github.com/aidarnig/WristPorter/issues).
- Feasibility and deliberate exclusions: [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md).
- Proposed architecture: [BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md); no approved minimum Android API until measured on reference watch.
- No new Watch APK, iOS code, BLE test evidence, battery results or bridge releases yet. **Planning docs and issues are not implementation.**

## Known risks and blocked assumptions

1. We have no recorded Watch4 Classic ↔ iPhone ANCS/AMS connection from this project. WeakBridge's Galaxy Watch6 result is **not** Watch4 evidence.
2. Watch4 firmware/API level, iPhone/iOS combination and system BLE behavior must be observed; never silently target API 33 and exclude older Watch4 firmware.
3. Android OS background rules, Samsung power management, iOS ANCS consent and GATT role requirements can break long-running connectivity.
4. Event-driven BLE design alone does not prove acceptable battery drain; collect baselines, paired cases and long-term measurements.
5. ANCS actions, AMS extras, battery/device info are **conditional** on iPhone/active player/service; no arbitrary replies, phone call audio, Apple Health or phone photos via ANCS/AMS.
6. External repo designs provide reference ideas, **not** proof of our implementation, security, ownership or third-party license compliance.
7. Current desktop photo workflow must remain independently usable if the bridge is absent or disconnected.

## Canonical next action

Open [#13](https://github.com/aidarnig/WristPorter/issues/13), confirm the watch firmware/API and available iPhone, design a **non-destructive, opt-in BLE feasibility PoC**, use [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md), and log all actual device observations in [VERIFICATION.md](VERIFICATION.md). No mandatory iOS application.

## Required chat handoff (update this section in place)

After each substantial completed task / merged PR, update `STATE.md` with:
- **Task anchor**: issue number, objective, branch/PR link, merged commit SHA (from GitHub).
- **Verified result**: code/tests actually executed, real hardware model/OS/version, PASS/FAIL/NOT TESTED; link to log.
- **Changed boundaries**: architecture/API/contracts and ADRs; note any divergences.
- **Remaining work**: concrete blockers, known issues, owner decisions required.
- **Next exact step**: issue and small reproducible objective.

At chat start, do *not* assume this snapshot is fresh: compare its last completed work with live `main` and open PRs. Update the file if stale rather than inventing a second handoff document.

### Latest planning handoff

- **Action:** documentation and 13 W-series planning Issues created; no application logic has been changed.
- **Task anchor:** roadmap/governance planning, branch `docs/bridge-roadmap-and-engineering-standard`; the PR/merge record is the live authoritative link once merged.
- **Verification:** specification sources reviewed; no BLE/Android watch APK code built or physically verified.
- **Next issue:** [#13 — W0 feasibility](https://github.com/aidarnig/WristPorter/issues/13).

## Context automation

A lightweight [documentation CI workflow](../.github/workflows/docs-check.yml) and [local checker](../ci/check-doc-links.py) validate required handoff files and relative Markdown links. They cannot test physical BLE features or the accuracy of status claims.
