# WristPorter — current project state

**Snapshot: 2026-11-13.** This is a short cross-track orientation, not a task log or a guarantee that GitHub hasn't changed. Check live `main`, Issues and PRs. Task-specific progress belongs **in its Issue**; the [roadmap](ROADMAP.md) owns milestones and dependencies.

## Shipped and verified

- **Desktop v1.1.2**: portable Windows x64 app, Java 25/Maven/JavaFX, bundled runtime + ADB, Wi-Fi ADB pairing and JPEG/PNG upload to Wear OS Gallery, RU/EN and light/dark. Modules: `watch-domain`, `watch-adb`, `watch-core`, `watch-desktop`.
- **Actual hardware evidence:** Samsung Galaxy Watch4 Classic showed an eight-PNG batch in Galaxy Gallery on 2026-11-11. Later 9/9 desktop UI-reported transfer and visual EN/RU acceptance were **not** a separate Gallery observation. Detailed proof and outstanding Desktop QA: [QUALITY.md](QUALITY.md).
- No Wear OS Bridge APK, embedded Web Upload or Safari-to-watch transfer currently exists in the shipped app. SmartScreen code-signing remains an open risk: [SECURITY.md](SECURITY.md).

## Approved direction (all new components PLANNED)

WristPorter is a **local-first Wear OS ecosystem for owners switching from Android to iPhone**, with independent tracks:

- **WristPorter Bridge**: an independent Kotlin Wear OS APK using direct watch ↔ iPhone BLE, proposed ANCS notification and AMS media integration. No iOS app or continuously running Windows PC. Feasibility on the user's **Galaxy Watch4 Classic and iPhone has NOT BEEN TESTED**; don't infer it from another watch. See [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md), [BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md), [roadmap](ROADMAP.md).
- **WristPorter Desktop Web Upload**: **the same Windows app** optionally serves an on-demand private-LAN browser uploader. Safari → Windows → existing Wi-Fi ADB → watch, with QR + short-lived PIN, a user-selected watch and private staging; Windows must remain running. No native iOS app, cloud, anonymous upload, always-on listener or browser-accessible remote ADB administration. The product owner approved an **opt-in HTTP-only beta boundary strictly for trusted private LANs**, with an explicit unencrypted-transfer warning; this is **not** a release or physical-compatibility sign-off. Architecture: [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md), [ADR-0005](adr/0005-on-demand-http-upload-transport.md); contract: [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md).
- **Windows APK management** belongs to Issue [#24](https://github.com/aidarnig/WristPorter/issues/24) after an installable Wear APK and version/signature contract exist.

**Original-photo invariant:** accepted JPEG/PNG images keep original bytes, resolution and metadata; no silent compression, resizing or re-encoding. The existing Desktop sender is not being rewritten for this requirement. Browser upload must separately verify whether iOS Safari supplied the actual original representation. Owners: [PRODUCT.md](PRODUCT.md) and [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md).

## Active coordination queue

Independent Issues that **may be assigned to separate chats** (read live Issue/PR before assuming work has started):

- **[#13 — W0](https://github.com/aidarnig/WristPorter/issues/13):** non-destructive Watch4 ↔ iPhone BLE/ANCS/AMS feasibility. Cannot honestly pass W0 without real hardware and iOS/watch versions.
- **[#27 — W13](https://github.com/aidarnig/WristPorter/issues/27):** private-LAN Web Upload architecture, browser feasibility and threat-model decision. Independent of W0.
- **[#7 — CI](https://github.com/aidarnig/WristPorter/issues/7):** dependency-aware downstream build checks. Independent of W0/W13.

**Deferred in this wave:** a separate photo-quality audit/test project (already-working Desktop transfer is unchanged); issue [#2](https://github.com/aidarnig/WristPorter/issues/2) is a distinct, still-open photo-import/thumbnail backlog, not the quality task. Other W0–W16 dependencies remain in [ROADMAP.md](ROADMAP.md).

## Genuine blockers and acceptance

- W0 requires real Watch4 firmware/API, selected iPhone model/iOS, permitted Bluetooth pairing and actual service/notification/media evidence; no factory reset. Log PASS/FAIL/BLOCKED/NOT TESTED **per scenario** in [VERIFICATION.md](VERIFICATION.md).
- W13 HTTP-on-trusted-private-LAN plaintext risk is **explicitly accepted for W14 development and controlled tests** (2026-11-13). All security controls, real Safari/Windows/Watch4 behavior and release acceptance remain **NOT TESTED**; never infer that a private IP encrypts photos or that the feature is shipped.
- Fake ADB tests, successful Maven, docs CI and experimental PoCs are **not** equivalent to real-device compatibility or battery acceptance. No signed release without [quality gates](QUALITY.md).

## Cross-chat handoff

New executor receives **only its GitHub Issue**. Mandatory read order, parallel branch ownership, autonomous safe PR merge, escalation, docs-writing and handoff rules live in [AGENTS.md](../AGENTS.md) and [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md).

During parallel work, executors keep updates in Issues/PRs and avoid racing edits to this shared file. Update STATE **only** for major merged milestones, blocking findings, shipped behavior or changes to the cross-track next priority. Do not paste full PR histories here. Planning history is recoverable from [PR #26](https://github.com/aidarnig/WristPorter/pull/26) (Bridge plan) and [PR #31](https://github.com/aidarnig/WristPorter/pull/31) (one Windows app/Web Upload plan).

**Next exact step:** assign Issues #13, #27 and #7 to independent executors; each can work from its Issue alone and must report a concrete blocker only when it cannot safely proceed.
