# WristPorter — current cross-track state

**Reviewed 2026-11-22 against live GitHub** (snapshot only; verify `main`, Issue discussions and PRs before work). Task-level progress belongs to Issues/PRs, not this document. Read [AGENTS.md](../AGENTS.md) for the minimal executor entry point.

## Shipped baseline

**Desktop v1.1.2:** portable Windows x64, Java 25/Maven/JavaFX, bundled ADB/Java, Wi-Fi ADB JPEG/PNG → Wear OS Gallery, RU/EN and themes. Reference **Galaxy Watch4 Classic** confirmed eight PNG files visible in Gallery on 2026-11-11; later 9/9 UI-reported desktop transfer is **not** a new Gallery proof. [QUALITY.md](QUALITY.md) records evidence. Existing desktop transfer remains independent; original JPEG/PNG bytes/resolution/metadata are not to be modified.

## Ecosystem directions

- **Watch BLE Bridge** (planned; no shipped Bridge APK): independent Kotlin Wear OS APK, direct watch ↔ iPhone ANCS/AMS. No iOS companion or continuously running Windows. **Experimental W0 real-device proof (2026-11-22):** Watch4 Classic Android 16/API 36 established iPhone GATT; ANCS/AMS exposed and subscriptions succeeded. Owner subsequently confirmed fresh ANCS and AMS events during instructed notification/media test. Exact track attribute, remote command execution, reconnect and background reliability remain unverified; W0 stays open.
- **Local Web Upload** (planned, not shipped): Safari → manually enabled **same** Windows app → Wi-Fi ADB → selected watch. No cloud, anonymous upload, always-on public listener or remote ADB administration. HTTP on **trusted private LAN only**, with unencrypted-transfer warning, was approved as a controlled beta architecture via [ADR-0005](adr/0005-on-demand-http-upload-transport.md); this does not approve release or imply hardware acceptance. Browser representation may differ from the photo's original bytes.
- **Windows APK manager** is a later step, [Issue #24](https://github.com/aidarnig/WristPorter/issues/24), dependent on an installable Wear APK.

Canonical scope/constraints: [PRODUCT.md](PRODUCT.md), [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md), [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md), [ROADMAP.md](ROADMAP.md).

## Workstream checkpoint

- **CI — [Issue #7](https://github.com/aidarnig/WristPorter/issues/7): CLOSED.** Downstream module validation was implemented; inspect the live Issue/CI for final test evidence.
- **BLE W0 — [Issue #13](https://github.com/aidarnig/WristPorter/issues/13): OPEN.** Isolated foreground-only [draft PR #34](https://github.com/aidarnig/WristPorter/pull/34); Android CI passed. **BLE-03 GATT/ANCS/AMS service and subscription check and BLE-04 ANCS live notification event PASS (owner)** on Watch4/iPhone; AMS event reception observed, but full media command behavior and reconnect remain unverified. See [VERIFICATION.md](VERIFICATION.md#W0--initial-real-device-probe-owner-checkpoint-2026-11-22) and live Issue. No shipped Bridge, background reliability or production minSdk decision.
- **Web W13 — [Issue #27](https://github.com/aidarnig/WristPorter/issues/27): OPEN.** Architecture/security decision documented and merged in [PR #33](https://github.com/aidarnig/WristPorter/pull/33); Safari Photos/Files, Windows LAN connectivity and Watch4 Gallery are **NOT TESTED**. Begin actionable owner-assisted iPhone/PC checks as soon as a safe probe/implementation is testable; W14 development is not blocked by the approved trusted-LAN HTTP decision.

**Next coordination:** finish W0 real hardware probe; gather W13 Safari/PC real-device observations; choose subsequent W1 and W14 implementation Issues in accordance with dependency gates. Separate photo-quality audit remains **deferred**; preserving original accepted files is still a binding product rule.

## Working convention

Give each executor only its Issue. [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md) requires **proactive owner-assisted testing in the same task chat** whenever a meaningful iPhone/watch/Windows checkpoint exists: exact build/link, safe actions, expected results, interpretation, fix/retest. Do not park all device cases as NOT TESTED at final handoff. Executors autonomously handle routine coding/tests/low-risk PRs; material product/security, release and actual compatibility decisions remain controlled. Keep this shared snapshot short; individual iterations live in Issues/PRs and hardware evidence in [VERIFICATION.md](VERIFICATION.md).
