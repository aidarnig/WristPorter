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
