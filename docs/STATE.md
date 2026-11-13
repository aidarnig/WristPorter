# CURRENT PROJECT STATE — read first on every new chat

**Snapshot date:** 2026-11-13. This is a documented planning baseline, **not** a claim that later GitHub commits/PRs do not exist. Check live `main`, open PRs and Issues before relying on it.

## What the product actually ships

- Public repository: https://github.com/aidarnig/WristPorter
- Shipped stable: **WristPorter Desktop v1.1.2** — Windows x64 portable ZIP, Java 25/Maven/JavaFX, bundled Java runtime + ADB, wireless ADB pairing, JPEG/PNG import/preview/transfer and Samsung Gallery indexing requests, EN/RU and light/dark.
- **Confirmed reference scenario:** Samsung Galaxy Watch4 Classic; Galaxy Gallery visibility of a real eight-photo batch has been confirmed. The desktop UI was later visually accepted in EN/RU with a 9/9 app-reported batch. Details: [QUALITY.md](QUALITY.md).
- Desktop modules: `watch-domain`, `watch-adb`, `watch-core`, `watch-desktop`; no watch APK source exists at this baseline.
- Current GitHub Social Preview is configured by the repository owner. GIF/video remains deferred in [issue #10](https://github.com/aidarnig/WristPorter/issues/10).
- Windows SmartScreen: executable is not trusted-publisher-signed; see [SECURITY.md](SECURITY.md).

## Updated ecosystem direction (PLANNED ONLY)

**WristPorter is evolving into a local-first ecosystem for Wear OS owners who switched from Android to iPhone**, not only a desktop photo sender. Approved, independent future components: (1) Watch-side **Bluetooth ANCS/AMS Bridge**, requiring no PC after installation; (2) the same Windows Desktop app with existing photo transfer and future **Web Upload** through an iPhone Safari browser on the private LAN while Windows stays on; (3) Desktop-installed watch APK updates/diagnostics. **No native iOS app or public cloud service**. None of these new features ship in Desktop v1.1.2. [Product](PRODUCT.md) · [Local Web Upload design](LOCAL_WEB_PORTAL.md) · [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md).

## Decision for the new Bridge program (PLANNED ONLY)

Build **WristPorter Bridge**, an independent Kotlin Wear OS app that talks to iPhone directly via its built-in Bluetooth **ANCS/AMS** services, without any iOS companion app. Aim for maximum **documented, observed** features with reliability, low battery impact, privacy and accessibility as first-class constraints.

- Reference watch: Samsung Galaxy Watch4 Classic; actual BLE ↔ iPhone ability **NOT TESTED BY WRISTPORTER**.
- First task: **[W0 — hardware/protocol feasibility, #13](https://github.com/aidarnig/WristPorter/issues/13)**. Do not start wide implementation until W0 evidence exists.
- Roadmap: [ROADMAP.md](ROADMAP.md), milestones W0–W16, [Bluetooth Issues #13–#25](https://github.com/aidarnig/WristPorter/issues) and [Web Upload Issues #27–#30](https://github.com/aidarnig/WristPorter/issues).
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
8. **LAN Web Upload is NOT IMPLEMENTED:** requires opt-in local Windows listener, device-target lock, temporary upload storage, short-lived authentication, and transparent warning that HTTP on Wi-Fi is unencrypted. Safari photo selection and HEIC behavior are NOT TESTED.
9. Web Upload runs **through Windows**; it is neither direct Bluetooth photo transfer nor an iOS companion app. Watch-side Bluetooth reliability/battery work remains independent.

## Canonical next action

**Two parallel starting points:** [#13 W0 Bluetooth feasibility](https://github.com/aidarnig/WristPorter/issues/13) and independently [#27 W13 LAN Web Upload architecture/security](https://github.com/aidarnig/WristPorter/issues/27). Do not prematurely implement either broad product. For Bridge, open [#13](https://github.com/aidarnig/WristPorter/issues/13), confirm the watch firmware/API and available iPhone, design a **non-destructive, opt-in BLE feasibility PoC**, use [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md), and log all actual device observations in [VERIFICATION.md](VERIFICATION.md). No mandatory iOS application.

## Required chat handoff (update this section in place)

After each substantial completed task / merged PR, update `STATE.md` with:
- **Task anchor**: issue number, objective, branch/PR link, merged commit SHA (from GitHub).
- **Verified result**: code/tests actually executed, real hardware model/OS/version, PASS/FAIL/NOT TESTED; link to log.
- **Changed boundaries**: architecture/API/contracts and ADRs; note any divergences.
- **Remaining work**: concrete blockers, known issues, owner decisions required.
- **Next exact step**: issue and small reproducible objective.

At chat start, do *not* assume this snapshot is fresh: compare its last completed work with live `main` and open PRs. Update the file if stale rather than inventing a second handoff document.

### Latest planning handoff

- **Action:** documentation/engineering standard and 13 W-series planning Issues created; no application code changed.
- **Task anchor:** [PR #26](https://github.com/aidarnig/WristPorter/pull/26), merged into `main` as `447fe4b2d303bfd5a85914d9e92e99b33a1e3f1f`.
- **Verification:** [documentation integrity CI #43462385660](https://github.com/aidarnig/WristPorter/actions/runs/43462385660) **PASS** (required context documents and local links); PR inspection confirmed no Java/application code changes. **Bridge BLE, physical Watch4+iPhone behavior, power and APK build: NOT TESTED.**
- **Remaining work:** first verify Watch4 Android API/firmware and iPhone BLE ANCS/AMS availability, no factory reset; do not assume compatibility from other projects.
- **Next issue:** [#13 — W0 feasibility](https://github.com/aidarnig/WristPorter/issues/13).

## Context automation

A lightweight [documentation CI workflow](../.github/workflows/docs-check.yml) and [local checker](../ci/check-doc-links.py) validate required handoff files and relative Markdown links. They cannot test physical BLE features or the accuracy of status claims.

## Latest ecosystem planning handoff (2026-11-13)

- **New decision:** one Windows app with **native Desktop photo mode** and **user-activated Web Upload** served over private LAN for iPhone Safari; no separate Windows edition, installed iOS companion or cloud. The existing transfer core will be shared, not copied. [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md).
- **New Issue anchors:** [#27 W13 security/design](https://github.com/aidarnig/WristPorter/issues/27), [#28 W14 JPG/PNG implementation](https://github.com/aidarnig/WristPorter/issues/28), [#29 W15 HEIC/format research](https://github.com/aidarnig/WristPorter/issues/29), [#30 W16 browser hardening/QA](https://github.com/aidarnig/WristPorter/issues/30). Desktop APK management remains [#24 W11](https://github.com/aidarnig/WristPorter/issues/24).
- **Implementation & verification:** docs and GitHub planning tasks only. No web server, Safari upload, HEIC conversion or Bridge APK has been added/verified. Public release v1.1.2 stays photo-transfer-only.
- **Next independent issues:** [#13](https://github.com/aidarnig/WristPorter/issues/13) (actual Watch4 ↔ iPhone BLE feasibility) **and** [#27](https://github.com/aidarnig/WristPorter/issues/27) (private-LAN browser design/PoC).
- **Planning PR:** refer to the merged GitHub PR linked to this doc revision; check main/history for the exact merge SHA before new work.
