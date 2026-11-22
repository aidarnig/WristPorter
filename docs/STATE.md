# WristPorter — current cross-track state

**Reviewed 2026-11-22 against live GitHub** (snapshot only; verify `main`, Issue discussions and PRs before work). Task-level progress belongs to Issues/PRs, not this document. Read [AGENTS.md](../AGENTS.md) for the minimal executor entry point.

## Shipped baseline

**Desktop v1.1.2:** portable Windows x64, Java 25/Maven/JavaFX, bundled ADB/Java, Wi-Fi ADB JPEG/PNG → Wear OS Gallery, RU/EN and themes. Reference **Galaxy Watch4 Classic** confirmed eight PNG files visible in Gallery on 2026-11-11; later 9/9 UI-reported desktop transfer is **not** a new Gallery proof. [QUALITY.md](QUALITY.md) records evidence. Existing desktop transfer remains independent; original JPEG/PNG bytes/resolution/metadata are not to be modified.

## Ecosystem directions

- **Watch BLE Bridge** (PLANNED; no shipped Bridge APK): independent Kotlin Wear OS app, direct watch ↔ iPhone ANCS/AMS. **W0 foreground protocol feasibility ACCEPTED (2026-11-22)** on Watch4 Classic Android 16/API36 + iPhone (iOS 26.5.2 last confirmed): GATT connection, ANCS notifications, AMS event reception, explicit manual reconnection and subscription recovery. **NOT proven:** background/automatic recovery, remote media commands, battery or compatibility with other devices/OS. [#13](https://github.com/aidarnig/WristPorter/issues/13) closed; disposable [PR #34](https://github.com/aidarnig/WristPorter/pull/34) independently reviewed and **closed without merge**. W1 production design is still unstarted.
- **Local Web Upload** (planned, not shipped): Safari → manually enabled **same** Windows app → Wi-Fi ADB → selected watch. No cloud, anonymous upload, always-on public listener or remote ADB administration. HTTP on **trusted private LAN only**, with unencrypted-transfer warning, was approved as a controlled beta architecture via [ADR-0005](adr/0005-on-demand-http-upload-transport.md); this does not approve release or imply hardware acceptance. Browser representation may differ from the photo's original bytes.
- **Windows APK manager** is a later step, [Issue #24](https://github.com/aidarnig/WristPorter/issues/24), dependent on an installable Wear APK.

Canonical scope/constraints: [PRODUCT.md](PRODUCT.md), [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md), [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md), [ROADMAP.md](ROADMAP.md).

## Workstream checkpoint

- **CI — [Issue #7](https://github.com/aidarnig/WristPorter/issues/7): CLOSED.** Downstream module validation was implemented; inspect the live Issue/CI for final test evidence.
- **BLE W0 — [Issue #13](https://github.com/aidarnig/WristPorter/issues/13): CLOSED (feasibility accepted).** Actual Watch4+iPhone GATT, ANCS, AMS and explicit manual recovery observed; prior AMS write errors remain in the [verification ledger](VERIFICATION.md). Prototype [PR #34](https://github.com/aidarnig/WristPorter/pull/34) closed unmerged after independent BLE/privacy review. **W1 provisional minSdk=30 is only a development choice; API30–35 untested.** No production Bridge, media commands, screen-off persistence or automatic recovery verified.
- **Web W13/W14 — [#27](https://github.com/aidarnig/WristPorter/issues/27) and [#28](https://github.com/aidarnig/WristPorter/issues/28): IN PROGRESS.** Trusted-LAN architecture approved; isolated gateway [#39](https://github.com/aidarnig/WristPorter/issues/39) merged with real Safari→Windows PIN pairing PASS. Actual file upload, ADB/Watch4 Gallery integration and iOS Photos/Files representation remain NOT IMPLEMENTED/NOT TESTED.

**Next coordination:** W0 research is complete. No new executor task has been assigned by this closeout; owner and coordinator will decide priorities separately. Existing W14 dependencies remain in their Issues. A separate photo-quality audit is deferred; preserving original accepted files remains binding.

## Working convention

Each executor receives one Issue. [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md) requires **value-gated owner testing**, a firm stop rule when the task's question is answered and one meaningful documentation update per milestone, not per screenshot. Executors autonomously handle routine code/CI; owner/coordinator control product, security and release decisions. Put iterations in Issues/PRs and observed hardware evidence in [VERIFICATION.md](VERIFICATION.md).
