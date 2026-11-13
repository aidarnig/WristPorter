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
