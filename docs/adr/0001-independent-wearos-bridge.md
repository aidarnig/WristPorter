# ADR-0001 — Independent Wear OS bridge; no iOS companion for current roadmap

**Status:** Accepted (planning scope), 2026-11-13  
**Context:** Desktop WristPorter v1.1.2 already delivers local Windows → Galaxy Watch Gallery photo transfer. After switching to iPhone, the user lacks supported Galaxy Wearable watch management and would like to regain as many features as possible **without developing/installing an iOS app**.

## Decision

- Preserve Windows Desktop photo transfer as an independent shipped product and keep its Java/Maven modules unchanged by default.
- Add a **separately built Kotlin Wear OS APK** (WristPorter Bridge) and version independently.
- Watch connects to iPhone's native BLE services (ANCS, AMS and verified optional standard GATT services) **without** iPhone-side application, account or server.
- Explicitly defer iOS companion app, iPhone photo sharing, contacts/Health sync, full Samsung Galaxy Wearable equivalence and any proprietary iOS control claims.
- Initial target/watch verification device is **Galaxy Watch4 Classic**; no claim of BLE interoperability until W0 physical evidence.
- Product may grow other independent Wear tools after a separate requirement decision and acceptance tests.

## Rationale

ANCS/AMS give meaningful notifications and music controls without introducing iPhone distribution/signing complexity. A standalone APK keeps photo transfer functioning without a bond or active iPhone. A separate codebase and CI protect the existing Java application.

## Consequences / risks

- Installation initially requires ADB/Wi-Fi (existing Windows bundle can help); initial watch setup still may require a compatible Android phone.
- Missing iOS privileges restrict contacts, rich replies, health data and direct iPhone photo transfer.
- Samsung background behavior, hardware BLE roles, Android API and battery are unknown until actual tests.
- Public wording must say “partial independently developed companion functionality”, not official Galaxy Wearable replacement.

**Evidence gate:** [Issue #13](https://github.com/aidarnig/WristPorter/issues/13), [VERIFICATION.md](../VERIFICATION.md).  
**Related:** [Roadmap](../ROADMAP.md), [Capabilities](../BRIDGE_CAPABILITIES.md).
