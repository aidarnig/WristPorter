# ADR-0004 — One Windows app with opt-in private-LAN browser upload

**Status:** Accepted for planning/architecture, 2026-11-13  
**Extends:** [ADR-0001](0001-independent-wearos-bridge.md) specifically on the earlier *“no iPhone photo transfer without iOS app”* exclusion. The exclusion still applies to **direct iPhone → watch ANCS/AMS and iPhone-native-app features**, but a **browser → Windows relay → ADB watch** is now an approved separate design. No iOS companion will be created.

## Context

WristPorter began as a Windows desktop photo transfer utility. It is now envisioned as an **ecosystem for owners of Wear OS watches after moving from Android to iPhone**, comprising an optional independent Wear OS BLE bridge and a powerful Windows management/transfer companion. The product owner wants to send iPhone Photos from Safari without a native iOS app while keeping the existing Windows program.

**Alternatives examined**
1. **Two separate Windows distributions** (desktop photos vs web uploader): duplicate packaging, update workflows, settings, watch management and ADB logic, increasing regression risk.
2. **Single Windows app with permanently listening web server**: simpler phone access, but unacceptable default attack surface / resource usage.
3. **Hosted/cloud portal or a standalone iOS app**: deviates from local-only/no-iOS-app direction and adds accounts, public exposure or Apple app signing.
4. **On-watch HTTP server for direct Safari upload**: could eventually remove Windows PC but demands watch-side Wi-Fi server, battery and security work beyond the current shipping proof.
5. **One optional Windows-resident browser portal**: reuses existing ADB transfer code and release while remaining off unless the user chooses to start it.

## Decision

- **One WristPorter Desktop EXE/ZIP and product identity.** Keep current JavaFX file transfer fully available. Add **Web Upload** as a manually activated local HTTP service with its own mobile-friendly browser frontend and temporary upload staging.
- The **same selected watch and transfer engine** serve both desktop and browser clients. Browser cannot select a new watch, issue generic ADB commands, install APK or read Windows files.
- No iOS app, cloud, remote relay, account, persistent server or watch HTTP receiver in W13–W16. Windows PC must remain running/reachable; watch must remain ADB-connected.
- Must have explicit session authorization (QR address only + short-lived code), least-privilege LAN binding, CSRF/Host/Origin controls, quotas, staging cleanup, correct session teardown and real security tests.
- **Local HTTP traffic is plaintext** and not protected by the authorization code. Investigate usable HTTPS/trust flow in W13 or approve a clearly documented *private trusted LAN only* boundary before release; do not pretend Wi-Fi is inherently private.
- Preserve `WebApplicationType.NONE` for existing desktop startup; use on-demand isolated web gateway, not a permanent embedded Spring public server.
- HEIC/HEIF and browser-specific conversion are **investigations**, not guaranteed in first JPEG/PNG browser release.
- Windows APK installation/update and diagnostics remain in existing [W11 #24](https://github.com/aidarnig/WristPorter/issues/24), sharing the same desktop application, not replicated in web UI.

## Consequences

- A single download and update channel, clear feature discoverability and code reuse.
- Network consent, Windows firewall issues and HTTP privacy require careful support/QA.
- Existing JavaFX queue/selected-watch logic must be moved into a shared coordinator, with regression tests.
- Windows desktop need not run for Bluetooth ANCS/AMS: that Bridge remains separate and optional.
- Updating the public brand from photo-transfer-only to an ecosystem is **product positioning**, not a claim that unshipped components currently exist.

**Follow-up:** [W13 #27](https://github.com/aidarnig/WristPorter/issues/27), [W14 #28](https://github.com/aidarnig/WristPorter/issues/28), [W15 #29](https://github.com/aidarnig/WristPorter/issues/29), [W16 #30](https://github.com/aidarnig/WristPorter/issues/30).  
**Design:** [LOCAL_WEB_PORTAL.md](../LOCAL_WEB_PORTAL.md), [Product](../PRODUCT.md), [Roadmap](../ROADMAP.md).
