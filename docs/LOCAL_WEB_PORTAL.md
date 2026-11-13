# Local Web Upload — iPhone browser → Windows → Wear OS

**Status: PLANNED ONLY (2026-11-13).** This is a technical design target, not an existing public feature. One Windows application will serve an **optional local-only browser interface**. The user does **not** install an iOS app; the Windows PC must stay awake and reachable for every transfer. The shipped standalone Desktop v1.1.2 remains unchanged and useful when Web Upload is off.

**Canonical product intent:** [PRODUCT.md](PRODUCT.md). **Decision:** [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md). **Roadmap:** [W13–W16](ROADMAP.md), issues #27–#30. **Safety:** [SECURITY.md](SECURITY.md). **Evidence:** [VERIFICATION.md](VERIFICATION.md).

## 1. Experience and topology

```text
                          Windows computer (ONE WristPorter application)
iPhone Safari                JavaFX/Desktop                Galaxy Watch4 Classic
Photos / Files picker          [Photo gallery]                 Samsung Gallery
       |                       [Web Upload ON/OFF]                  ^
       | 1. QR + short PIN       [APK manager (later)]              |
       | 2. private-LAN HTTP(S)         |                            |
       v                               v                            |
Mobile web UI  -- authenticated --> bounded HTTP gateway             |
                                         |                            |
                                         v                            |
                               one transfer coordinator              |
                               / safe staging + validation           |
                                         |                            |
                              existing WatchTransferService ---------+
                               ADB over Wi-Fi; media scan
```

- A QR code on Windows opens the **local Windows PC address** from the iPhone's browser. The iPhone uploads user-selected images; the **Windows program**, not Safari, connects to the watch over ADB.
- The iPhone and PC must be able to reach each other on a **trusted private local network**. PC and watch must be reachable by ADB, typically on the same Wi-Fi. Guest/client-isolated Wi-Fi, VPN, captive portals or Windows firewall may block either link.
- Bluetooth ANCS/AMS WristPorter Bridge is an **independent path**: notifications/music run between watch and iPhone without this portal or PC. The portal is **not** an iOS companion application.
- Browser doesn't remotely install apps, issue ADB commands, connect arbitrary watches, read local files or browse the PC; only the desktop owner can choose/watch/manage those operations.
- **No cloud, no permanent account, no Internet exposure, no separate Windows edition, no required Android-side web server.**

## 2. Architecture decision: one binary, two interfaces

**Choose one Windows ZIP/EXE**, with the same JavaFX desktop UI plus an optional **Web Upload** switch/tab. The web frontend is a bundled, mobile-first responsive webpage served by a short-lived embedded HTTP service **inside the same Windows process**. No second installer, Windows background daemon or hosted website is needed.

Possible source module: `watch-web` (HTTP handlers, policy, temporary files, session state and bundled frontend), wired from `watch-desktop` into the existing `watch-domain` and `watch-core`. Decide module split during W13 after prototype complexity review. Maintain `WebApplicationType.NONE` for current Desktop Spring Boot; do **not** globally turn the app into an always-running Spring web server. Evaluate a lightweight JDK server (`jdk.httpserver`) against a small, well-maintained embedded HTTP library for multipart/streaming, timeouts, packaging size and maintainability; document actual dependency tradeoffs before choosing.

**Mandatory refactor before enabling concurrent modes:** extract watch selection, transfer job lifecycle, cancellation and queue ownership from JavaFX `WatchWorkspace` into a testable shared `TransferCoordinator`. Both local file picker and web-uploaded temporary files must use existing `PhotoFiles.requireReadable`, `WatchTransferService.send` / `retry` and correct scan-only retry semantics. Never duplicate the ADB copy/indexing implementation. UI updates happen on JavaFX thread; upload/network/ADB work runs on bounded background executors.

Single explicit **selected-watch lease per web session**. If selected ADB serial changes, watch disconnects, or desktop owner closes/shuts down the app, pause/revoke the active browser session; never silently push files to a newly selected device. Define serialization/capacity when Desktop and Web both enqueue photos. No remote arbitrary ADB API.

## 3. Proposed user journey (RU and EN)

1. Windows user connects to a specific watch in the existing WristPorter interface.
2. User opens **Web Upload**, reads the visible privacy warning and explicitly presses **Start sharing**. Server is OFF by default, never restored as listening automatically at app startup.
3. Desktop chooses a reachable private network interface and ephemeral/configurable port, shows network name/interface, a **QR with URL only**, a human-readable URL, short-lived **one-time random authorization code**, selected-watch name, timer and **Stop** button.
4. On iPhone Safari, user scans QR or types `http://192.168.x.y:PORT/`. The portal asks for the code from the Windows screen. QR **must not contain the session code, reusable bearer token or device identifier**.
5. After successful authentication a short-lived, scoped session is created. User selects one or several images using iOS **Photos/Files** picker, reviews previews/status and taps **Send**.
6. Server streams JPEG/PNG to a private bounded staging directory (not a public static path), validates content, enqueues with the Windows-selected watch and transfers via existing ADB pipeline.
7. Browser receives **job-scoped** upload, copying and media-scan-request status; must not equate a successful scan request with proof of Gallery visibility. Galaxy Gallery visibility is verified by opening it on the watch.
8. User may retry failed copies/scans only while the authorized session remains active and the device still matches; repeat scan requests never reupload copied images.
9. After done/cancel/timer expiry/manual Stop/Windows app close: revoke session, stop network listener and clean temporary files; show an understandable status on both interfaces.

**Important distinction:** a PIN/session stored in browser memory is not the same as encryption. An HTTP request on local Wi-Fi is **plaintext** and can be inspected/modified by malicious actors on that network. Never claim that a PIN alone makes the link private. Evaluate an authenticated encrypted HTTPS flow during W13; if secure trusted-certificate onboarding is impractical, release-gate explicitly on accepted limits **private trusted network only**, with transparent warning and no auto-start. Never recommend public Wi-Fi or exposing a port to the Internet.

## 4. Security policy and threat model (release blockers)

| Threat | Required control / engineering test |
| --- | --- |
| Web service unintentionally exposed | OFF by default; bind explicitly to selected **private LAN interface** after consent, not all interfaces/public IP/IPv6 unspecified; stop and release port on shutdown |
| LAN eavesdropping, PIN/session theft, photo interception | Evaluate proper TLS and trust bootstrapping; if HTTP-only, disclose plaintext and restrict mode to trusted private network; never claim encryption |
| Random LAN user uploads to watch | Short-lived unpredictable code, rate-limited attempts/temporary lockout, short-lived HttpOnly/SameSite cookie, session revocation; no URLs carrying persistent secrets |
| CSRF / DNS rebinding / malicious cross-origin page | Strict accepted Host/interface/origin, CSRF token + Origin checks for state-changing requests, browser security headers, no permissive CORS, no JSONP |
| Remote administration / ADB misuse | API allowlist limited to upload + scoped job progress + cancel; no raw ADB command, endpoint editing, watch selection, APK management or arbitrary file reading |
| Wrong watch receives private photos | Desktop-selected serial snapshot, device lease, visible target on Windows and browser, refuse if selection changes |
| Huge/malformed image or unsafe parsing | Limits for request, file, batch and total staging space; validate JPEG/PNG signatures **and** decode safely if needed; pixel/decompression caps; no blind trust in MIME/name; no ZIP/scripts |
| Path traversal, overwritten files | Server-generated safe temp basename in private non-webroot, no client filename use, no symlink following; reuse UUID remote names |
| Upload/file persistence privacy | Files staged only as needed, automatic cleanup on completion, cancellation, TTL, crash recovery; no public preview/file download endpoints |
| App UI lockup / retry storms | Bounded executor/queue, explicit timeouts/cancel, no socket work on JavaFX thread, max retries and cleanup |
| Windows firewall/network uncertainty | Show connection diagnostics and trusted/private network warning; never automatically disable firewall or open Internet port |
| Device or LAN changes mid-transfer | Invalidate/revoke session, stop accepting new uploads, report interrupted jobs honestly |
| Browser page XSS via filenames/metadata | Escape all filenames, safe CSP, no HTML injection, restrictive cache/Referrer-Policy headers |

Based on [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html). Security controls must be covered by tests before a stable public release. Source includes no LAN server today, so no implementation should describe existing security as complete.

## 5. API contract sketch (not an implementation)

Use **relative same-origin endpoints** on the ephemeral Windows host, no public API:

| Endpoint concept | Permission | Response |
| --- | --- | --- |
| `GET /` | Unauthenticated minimal static login page | Nothing about private watch/PC/photo state |
| `POST /session` | One-time pairing code + rate limit + Origin checks | New scoped session cookie and CSRF token |
| `POST /uploads` | Authenticated + CSRF + quotas + bound target | Upload job IDs and validation results |
| `GET /jobs/{id}` | Authenticated + owns job | Per-file status (prefer polling with capped interval or SSE if justified) |
| `POST /jobs/{id}/cancel` | Authenticated + CSRF | Cancels only owned job |
| `POST /session/logout` | Authenticated + CSRF | Revokes session and client access |

Even on authorized sessions return only the minimum needed: sanitized watch model/label and job state, not device serial, filesystem path, detailed ADB output or other users' jobs. Use bounded event streams, no telemetry.

## 6. File formats and iPhone Safari behavior

**First web MVP:** JPEG/JPG and PNG only, because the current Windows transfer service intentionally validates those formats. Safari file picker may present different representations (including HEIC/HEIF); do **not** assert Safari always converts HEIC to JPEG. Detect and explain unsupported selections, with clear options for user export if necessary.

**Original-quality invariant:** the web page and Windows server must **not resize, compress, re-encode or strip metadata** from accepted JPEG/PNG images. Transfer the exact accepted upload bytes onward through the existing ADB sender. Browser thumbnails may be scaled for display but must not become upload data. Do not use canvas export or an image optimization service as a silent fallback. Preserve resolution and EXIF/other file metadata. W13/W14 must explicitly investigate whether iOS Photos/Files provides the device's actual original bytes or an OS-generated representation; do **not** claim byte-identical-to-iPhone-original behavior without a real Safari/iPhone comparison. If Safari supplies an altered representation or an unsupported HEIC, explain the limitation rather than presenting silent conversion as lossless.

**W15 research:** test JPEG/PNG/HEIC/HEIF from Photos vs Files; image orientation, iCloud-backed photo retrieval, live photos, HDR, iOS multiple selection. Choose a safe conversion path only after device evidence; avoid unbounded canvas/pixel memory or heavy native codec in the portable ZIP. If conversion is implemented, test actual JPEG/PNG output and Galaxy Gallery; be transparent about reduced quality, HDR and metadata/location EXIF. Browser folder access is limited on iOS: don't promise selecting arbitrary photo folders or direct always-on Photos access.

Photos **from Windows** must keep working unchanged. **Phone browser** does not have direct ADB or Bluetooth access to a watch.

## 7. Reliability, test matrix and release gates

- W13 [#27](https://github.com/aidarnig/WristPorter/issues/27): choose server architecture, check real Safari upload and document security/TLS decision and network behavior.
- W14 [#28](https://github.com/aidarnig/WristPorter/issues/28): implement opt-in browser mode and shared transfer queue; real end-to-end JPG/PNG on Watch4.
- W15 [#29](https://github.com/aidarnig/WristPorter/issues/29): determine HEIC/Live Photo selection and safe conversion policy.
- W16 [#30](https://github.com/aidarnig/WristPorter/issues/30): complete security, fault and clean-Windows acceptance; release independently of Bridge beta.

**Minimum evidence cases:** iPhone Safari (exact iOS), Windows 10/11 portable runtime, Watch4 Classic (firmware/API), 1 image and 10+ batch, browser reconnect/refresh, expired/mistyped PIN, forged origin, unauthorized upload, oversized/malformed image, two senders, wrong watch, watch Wi-Fi drop, scan-only retry, PC sleep, temp cleanup, public network rejection, Windows Defender Firewall, clean Windows, web mode OFF and unchanged desktop flow. Record in [VERIFICATION.md](VERIFICATION.md).

### Future exploration (not a commitment)

A **watch-hosted, on-demand Wi-Fi web server** might someday let Safari upload directly to an installed WristPorter Watch APK with no Windows PC. That would be a different architecture with watch storage, Android gallery integration, watch battery/thermal and local HTTPS/pairing risks. **Do not merge it into the current W13–W16 scope** or imply browser-only direct watch access is already possible. Analyze separately after real-hardware proof and product approval.

**Status reminder:** all browser/Web Upload UX, security measures, QR flow and HEIC support above are specifications **not yet implemented or tested**.


## 8. W13 feasibility findings and implementation decision (2026-11-13)

**Status: DESK-RESEARCHED / DESIGN PROPOSED; actual iOS Safari, Windows LAN and Watch4 tests NOT TESTED.** This section refines, but does not change, accepted [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md). The narrower server/security proposal is [ADR-0005](adr/0005-on-demand-http-upload-transport.md) (**PROPOSED**, awaiting explicit plaintext-network risk decision). No production listener or Web Upload code is enabled by this design work.

### Browser selection and preserving received bytes

- Use a normal, user-activated `<input type="file" multiple accept=".jpg,.jpeg,.png,image/jpeg,image/png">` and native Photos/Files picker, without `capture` or an iOS companion. Per [MDN](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input/file), `multiple` is supported by the HTML interface, but `accept` is only a chooser hint, **not validation**. Exactly how iOS Safari presents Photos/Files, iCloud assets and 10+ selections is **NOT TESTED here**.
- **Important new finding:** [WebKit bug 292350](https://bugs.webkit.org/show_bug.cgi?id=292350), fixed in February 2026, describes Safari automatically transcoding HEIC to JPEG depending on the `accept` list. This is **upstream browser conversion**, not proof the iPhone original was preserved. Show the actual selected `File.name`, `File.type`, `File.size` and server-detected format in diagnostics without logging private filenames. Do not equate uploaded-byte preservation to equality with the Photos library original.
- Browser must send the selected `File` itself with `fetch` body = `File` (not a resized canvas/blob reconstruction). One raw binary `POST /uploads` per file is the preferred W14 contract. The server streams it into a private temporary file with a generated `.jpg` or `.png` extension based on actual validated content. No `multipart/form-data` is needed for this path, reducing parser/dependency complexity. The browser retains client-only display filenames; do not trust or persist them on the server.
- For each accepted file, calculate SHA-256 on received staging bytes and verify equality with the exact bytes handed to the ADB transfer engine in automated tests. Real original-vs-Safari comparisons require independent source-photo bytes obtained with a documented iPhone export workflow; this is **NOT TESTED**.

### HTTP server selection and packaging

| Alternative | Advantages | Risks / verdict |
| --- | --- | --- |
| **JDK 25 `jdk.httpserver.HttpServer` + raw request bodies** | Already available in JDK; minimal dependency/portable ZIP overhead; explicit socket binding, request handlers and supplied executor; easy opt-in lifecycle | Basic HTTP server, no first-class multipart parser, framework auth or upload quotas. Implement request, header, connection and time limits and adversarial tests ourselves. **Preferred for a bounded W14 prototype; re-evaluate after tests.** |
| JDK HttpServer + third-party multipart parser | Retains tiny server but supports regular HTML multipart posts | Adds parsing and dependency maintenance that raw `File` requests avoid; no first-release need. |
| Embedded Undertow/Jetty | More extensive multipart, connection and HTTP controls | Additional dependencies, update monitoring, packaging tests and a wider exposed surface. **Fallback** if the simple server fails security or slow-client tests; not justified just to render one small local page. |
| Spring MVC / Boot web server always enabled | Familiar routing and multipart | Conflicts with `WebApplicationType.NONE`, heavier on-demand setup and increases default exposure. **Rejected**. |

The JDK HTTP API is [documented for Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html). The JDK [notes its server is minimal](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/module-summary.html); this recommendation is **conditional on W14 adversarial tests**. Specifically, configure and verify bounded handlers/executor, `jdk.httpserver.maxConnections`, header limits and `sun.net.httpserver.maxReqTime` / `maxRspTime` as applicable before allowing LAN traffic. The [properties are documented](https://docs.oracle.com/en/java/javase/25/core/java-networking.html); defaults include unbounded request time and are **not safe to assume sufficient**. Check that the `jdk.httpserver` module is included in the `jpackage` runtime; preserve the default `WebApplicationType.NONE` app startup.

### W14 executable component contracts (provisional values)

- **Gateway lifecycle:** a `WebUploadController` (Windows owner only) starts `LanUploadGateway` after a selected, ready watch and a verified private IPv4 adapter are chosen; binds to that exact address with OS-assigned port `0`. Never `0.0.0.0`, `::`, public IP, automatic startup, port forwarding or remote administration. Stop closes the socket, revokes tokens and cancels/settles jobs. Switching adapters, watch serial, or Windows session invalidates old access; no silent target change.
- **Network approval:** explicitly enumerate adapter addresses; require a user-selected, directly reachable RFC1918 IPv4 address (10/8, 172.16/12, 192.168/16), excluding VPN/virtual/disconnected interfaces unless manually validated. IP shape is insufficient to certify a network as **trusted**. Show interface/address and a plaintext warning before consent; no unsafe automatic firewall changes. Windows firewall may show an app exception prompt or silently block, and a changed random port may require new inbound rules; provide diagnostics, never auto-open broad firewall rules.
- **Authorization:** QR contains *only* the ephemeral URL. One cryptographically random short-lived one-time code displayed solely on Windows (proposed 8 base32 characters, expire in 2 min); constant-time comparison where appropriate, per-peer and global attempt limits (proposed three failures in five minutes, regenerate/revoke on lockout), no secrets in URLs, logs or browser storage. Success grants random revocable opaque session ID in `HttpOnly; SameSite=Strict; Path=/` cookie and a separate session-bound CSRF value. On HTTP, **`Secure` cookie cannot protect plaintext; this remains an explicit blocker**, not a substitute for TLS.
- **Request checks:** exact `Host` = displayed address:port, permitted `Origin` = same origin for every state-changing request (including `POST /session`), require explicit anti-CSRF header for session-authenticated POST, reject missing/mismatched origin until tested against actual Safari; no wildcard CORS. Exact route+HTTP-method allowlist (JDK context matching is prefix-based), cache suppression and restrictive CSP, no embedded third-party resources. Unauthorized GET exposes no watch identity or file state.
- **Upload wire contract:** `POST /uploads`, `Content-Type: image/jpeg | image/png | application/octet-stream`, raw bytes; authenticated cookie + CSRF header. Inspect magic bytes and extension of generated staging filename; `Content-Length` (when present) and actual bytes are independently capped, including chunked requests. Return a random job ID and coarse states only. Never accept a client-controlled path, destination, serial, ADB command or arbitrary file download.
- **Provisional quotas (must be tested/tuned):** 25 MiB maximum per image; 20 files and 250 MiB staged per session; 500 MiB total staging budget; two active upload sockets and **one** ADB transfer job globally. Reject at admission and while streaming before disk exhaustion; count file sizes, queue entries and in-flight requests. Apply bounded execution, per-request deadlines and cancellation, and server-side JPEG/PNG signature + decoded pixel bounds (e.g. 50 megapixels) without decoding/rewriting accepted upload bytes.
- **Staging/cleanup:** generated names in a per-user ACL-restricted directory under `%LOCALAPPDATA%/WristPorter/WebUpload/staging` outside any served root. Reject symlinks/unsafe paths, keep incomplete uploads isolated, atomically commit validated files, purge on cancel/logout/session expiration/stop and safely sweep owned abandoned staging on next launch. A copy failure may retain its source until retry/revocation; after a successful copy with scan-only failure, keep remote filename/status for scan-only retry, **not** a second push. Keep server log data free of image contents, filenames, cookies, tokens, device IDs and phone addresses.
- **Shared transfer coordinator:** move the active job, selected-serial snapshot, watch lease and exclusive queue ownership out of `WatchWorkspace`; don't change the tested ADB path. `TransferCoordinator` serializes Desktop and Web jobs and invokes existing `WatchTransferService.send/retry`, which already uses `PhotoFiles.requireReadable` and differentiates scan-only retries. The coordinator owns cancel/interruption, snapshots adb executable + selected serial, and sends immutable progress to FX/web adapters. Check serial is unchanged before queue admission and before each job; watch disconnection causes honest failure, not device fallback.

### Threat-model adversarial tests for W14/W16 (all NOT TESTED)

| Probe | Required observation |
| --- | --- |
| Start without clicking Web Upload / restart Windows app / Stop / crash recovery | No LAN port before consent or after Stop; process exit closes listener; crash-staged files are purged next launch |
| Public/VPN/guest WLAN, Wi-Fi client isolation, dual adapter, unplug, DHCP change | Correct interface refusal, no wildcard/public binding, useful unreachable diagnostics and session revocation |
| Browser `Host` forged / DNS rebinding, cross-origin POST, missing Origin/CSRF, cookie replay | Consistent reject, no staging or watch commands |
| Guess code repeatedly, expired code, session theft simulation, logout, open two browsers | Attempts throttled; authorization one-time, scoped, revocable; no token in QR, logs or URL |
| Missing/false length, chunked body over cap, slow sender, many sockets, malformed headers | Connection/time/disk budgets enforced; no OOM or unbounded worker growth |
| Empty, spoofed JPEG, HEIC, ZIP, polyglot, giant pixel dimensions, traversal/symlink names | Reject safely; invalid file never reaches ADB, staged residue removed |
| Switch watch or ADB device between code entry, upload, queue and scan retry | Never sends private image to different serial; reauthorization required |
| Competing Desktop send, cancellation during ADB, failed scan after successful copy | One serialized device queue; no duplicate remote push on scan-only retry; no FX-thread blocking |
| Observe same Wi-Fi traffic while using HTTP | Images, authorization and cookies can be read; test **documents risk**, never marks HTTP encrypted |
| Clean Windows 10/11 package, Defender Firewall private/public rules and selected port | Portable runtime includes server module, mode OFF works without inbound rules and existing Desktop v1.1.2 flow remains unaffected |

### Genuine external acceptance and decision gate

| W13 finding | Status | What remains |
| --- | --- | --- |
| HTML file picker has a multiple-selection mechanism | **Documented API, not hardware PASS** | Real iPhone Photos and Files picker tests with exact model/iOS/Safari |
| iOS Safari `accept` may trigger HEIC conversion | **Documented upstream WebKit behavior, not Watch4 proof** | Compare actual selected `File` bytes against independently obtained original |
| JDK HttpServer can bind exact address, port and handlers | **Documented API, not built or Windows tested** | Implement/compile loopback and LAN prototype, test limits and portable image |
| Windows private-LAN browser upload → ADB → Gallery | **NOT TESTED** | Windows 10/11 + actual iPhone + Watch4 test and hashes |
| Transport security: trustworthy HTTPS onboarding vs restricted HTTP | **OWNER DECISION: APPROVED (2026-11-13)** | Manually enabled HTTP **only on a trusted private LAN**, with explicit plaintext warning and user confirmation; W14 implementation is allowed, stable release remains gated on adversarial and hardware tests |

Apple's [certificate trust instructions](https://support.apple.com/en-ie/102390) require extra manual root-certificate trust for many self-signed HTTPS setups; shipping a shared private key in a portable EXE is unacceptable. Do not tell users to ignore a certificate warning, install a shared CA or disable OS/firewall security. Security choice is now recorded in [ADR-0005](adr/0005-on-demand-http-upload-transport.md): private-LAN HTTP with explicit informed opt-in is permitted for W14 development and controlled tests, **not** unrestricted public release. No web server exists in v1.1.2.

**W13 exit criteria:** preserve the design and approved security boundary; document Safari feasibility with truthful PASS/FAIL/BLOCKED/NOT TESTED. For this research task, physical Safari and Watch4 tests remain **NOT TESTED**, not silently passed. W14 implementation planning and restricted private-LAN testing are unblocked by the security decision, but actual compatibility/release claims require the recorded hardware evidence and W16 security gates.
