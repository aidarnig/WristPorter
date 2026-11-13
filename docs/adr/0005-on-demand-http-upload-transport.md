# ADR-0005 — On-demand raw-byte browser uploads with bounded JDK HTTP gateway

**Status:** Proposed — pending owner decision on HTTP plaintext risk, 2026-11-13  
**Builds on:** [ADR-0004](0004-one-desktop-optional-lan-portal.md), which already **accepts** one Windows application and no always-on listener.  
**Issue:** [#27 — W13](https://github.com/aidarnig/WristPorter/issues/27); implementation [#28 — W14](https://github.com/aidarnig/WristPorter/issues/28).  
**Canonical full contract:** [LOCAL_WEB_PORTAL.md §8](../LOCAL_WEB_PORTAL.md#8-w13-feasibility-findings-and-implementation-decision-2026-11-13).

## Context and evidence

Desktop 1.1.2 has Java 25, JavaFX, Spring Boot \`WebApplicationType.NONE\` and an existing ADB \`WatchTransferService.send/retry\`. Photo validation is in \`PhotoFiles.requireReadable\`. The only watch selection / transfer queue currently lives in JavaFX \`WatchWorkspace\`. A web uploader needs the same engine without allowing the browser to choose a watch or issue ADB commands.

An HTML file input supports user-selected multiple images, but not arbitrary Photos-library access. Safari may provide an automatically converted JPEG when an HEIC image is selected: see [WebKit issue 292350](https://bugs.webkit.org/show_bug.cgi?id=292350). **None of the iPhone/Windows/Watch4 acceptance tests has been performed by this ADR.**

## Proposed technical decision (not a release authorization)

1. **Wire format:** browser sends the selected JavaScript \`File\` as a raw binary request body, one image per authenticated \`POST /uploads\`. This avoids a multipart library and any additional browser-side transcode. The Windows server still verifies signature, limits, quotas and decoding bounds, and stages bytes privately. No client-supplied path, filename or watch target is trusted.
2. **Server:** first prototype uses Java 25 \`jdk.httpserver.HttpServer\`, **manually started and stopped** in the existing Windows process. It binds exactly one owner-selected RFC1918 IPv4 interface to ephemeral port \`0\`, never a wildcard address. The module's built-in request/header/connection timeouts **must be configured and verified**, plus application-level per-request byte counting, bounded executor and total staging caps. [JDK API](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/module-summary.html) describes this implementation as a minimal HTTP server; if slow-client / resource exhaustion tests fail, reconsider Jetty/Undertow before any LAN use.
3. **Auth/operations:** address-only QR; one-time random desktop-displayed code; rate-limited pairing; random short-lived revocable cookie + anti-CSRF; strict \`Host\`, \`Origin\`, method and route checks; no CORS; one Windows-selected-watch session lease. Web API exposes only image upload, own job status/cancel and logout. Existing Windows upload remains operational when the listener is OFF.
4. **Transfer:** refactor \`WatchWorkspace\` job ownership into one shared serialized \`TransferCoordinator\`; reuse \`PhotoFiles.requireReadable\` and \`WatchTransferService.send/retry\`. Keep scan-only retries scan-only and preserve exact accepted image bytes. No silent device switching or simultaneous unmanaged ADB writes.
5. **Cleanup:** per-user private staging, server-generated names, atomic validate/promote, bounded disk use, lifecycle cancellation, crash-recovery cleanup and no content/tokens/identifiers in logs.
6. **Packaging:** no new Windows installer or permanent daemon. Verify module inclusion in portable \`jpackage\` image and no LAN port when disabled.

## Explicit open security decision — must not be silently accepted

**HTTP on a private LAN is plaintext.** A short code, same-site cookie, CSRF protection, QR or Wi-Fi password **does not make the transmitted photographs confidential** from an attacker able to observe that network. Usable authenticated HTTPS with a per-PC certificate would need a user-trusted issuance/verification process. Apple's [manual trust guidance](https://support.apple.com/en-ie/102390) confirms a manually installed certificate is not automatically trusted for SSL; a packaged shared TLS key or clicking through invalid certificate warnings is not an acceptable solution.

**Recommended choice for owner review:** permit a clearly labeled **opt-in trusted-private-network-only HTTP beta**, no auto-start, no public/guest Wi-Fi, no automatic firewall changes; require real-device and adversarial security verification before any release. **Alternative:** require authenticated HTTPS onboarding before W14's LAN listener (more engineering/onboarding work, lower LAN interception risk). Without one explicit approved boundary, keep implementation to **loopback-only tests**, and W14 cannot expose a LAN endpoint. This proposed ADR is **not** an approval to bind to a real LAN.

## Consequences and gates

- Low dependency and package impact; a deliberately narrow HTTP surface instead of a general web server or multipart framework.
- More responsibility for enforcing header/body/time/connection limits, CSRF and session invalidation. All constants in the canonical [W13 threat test matrix](../LOCAL_WEB_PORTAL.md#threat-model-adversarial-tests-for-w14w16-all-not-tested) are **provisional**, not verified safe.
- Safari Photos/Files selection, original representation, firewall, multi-adapter behavior and Watch4 Gallery must be tested on actual hardware. Until then status stays **NOT TESTED**.
- ADR-0004's one-app direction remains accepted independently. ADR-0005 becomes **Accepted** only after explicit network-security choice and technical review; a green documentation check does not change that status.
