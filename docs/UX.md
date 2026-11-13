# User experience

This file owns **screen structure, interaction and visual guidance**. Functional acceptance criteria live in [PRODUCT.md](PRODUCT.md).

## Initial screen

A header identifies the app. The connection sidebar uses bundled ADB automatically in portable mode and hides custom ADB path selection under a compact advanced button matching the width of pairing/connection controls. It guides first pairing and connection without exposing ADB internals by default. The primary workspace provides a compact drag-and-drop target, file/folder pickers, an adaptive multi-column thumbnail grid with clickable/keyboard-selectable cards, a vertically resizable results journal, a wide progress bar with prominent counts and a Send button. After a batch, **Retry failures** acts only on photos still in the queue: failed copies are retried; failed scan requests are repeated without re-uploading. Switching to a different watch blocks retry of the previous batch.

## Pairing guidance

The device finder displays connected ADB devices and requires explicit selection if multiple are available. The **pairing port** and **debugging connection port** shown by Wear OS are different. The interface must explicitly distinguish them, explain where each is displayed, and never ask users to run shell commands.

## Visual system

JavaFX 27 + AtlantaFX 3; light/dark theme switcher with the selected preference stored locally, low-noise surfaces, clear hierarchy, rounded interactive elements and status colors with adequate contrast. The UI ships with RU/EN resource bundles and persists the selected language, switching without a restart. Full header branding responds to light/dark theme changes. The gallery uses asynchronous bounded thumbnails; cards wrap to use available width and display per-file transfer outcomes. Recursive folder import runs outside the UI thread.

## Accessible behavior

Every action must be keyboard-accessible and state should be textually represented, not only colored. Long-running actions show progress and offer cancellation that interrupts the running ADB subprocess. Send/retry remain disabled until the worker actually stops; failure messages tell users what they can fix. Never say Gallery definitely displays a photo simply because the scan request succeeded.

## Future screens

Additional languages, native accessibility testing and extended activity history remain future enhancements.

## Future independent WristPorter Bridge interface

The current sections above describe **Desktop v1.1.2** only. The planned Wear OS app will have an independent round-screen UI for connection status, notifications, media, privacy settings and diagnostics, in RU/EN. Watch UI scope is tracked in [W6 #19](https://github.com/aidarnig/WristPorter/issues/19); platform behavior, lifecycle and privacy rules are documented in [BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md). No Wear UI has been built or verified yet.

## Planned Windows Web Upload and iPhone browser UX (NOT SHIPPED)

[ADR-0004](adr/0004-one-desktop-optional-lan-portal.md) approves a **single Windows application** with a new **Web Upload** tab/button, not a second edition. Windows owner must select the watch and explicitly activate LAN sharing. Show chosen private interface/network, QR with local URL (no bearer token), short-lived pairing code, expiry and a high-visibility **Stop sharing** control.

iPhone browser is a small RU/EN, mobile-first user interface: code entry → Photos/Files selection → preview and count → upload progress → per-file ADB-copy/media-scan-request status, retry/cancel, clear explanation of unsupported HEIC and unavailable network/watch. Safari and iPhone photo selection behavior must be verified on real hardware. Browser cannot remotely change device, install APK, execute ADB or inspect Windows files.

Accessible alternatives: text URL if QR scanning is unavailable, readable error messages, large touch targets and clear progress independent of color. Web mode remains OFF by default, stops when desktop closes, refuses untrusted/public-network setup (or displays approved risk gate), and shows that HTTP LAN transfer is **not encrypted** unless a trusted TLS solution is provided.

[Detailed design](LOCAL_WEB_PORTAL.md); [issues #27–#30](ROADMAP.md).
