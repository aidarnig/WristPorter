# User experience

This file owns **screen structure, interaction and visual guidance**. Functional acceptance criteria live in [PRODUCT.md](PRODUCT.md).

## Initial screen

A header identifies the app. The connection sidebar uses bundled ADB automatically in portable mode and hides custom ADB path selection under a collapsed advanced section. It guides first pairing and connection without exposing ADB internals by default. The primary workspace provides a drag-and-drop zone, file and folder pickers, selected photos, transfer results and a prominent Send button. The footer shows task progress. After a batch, **Retry failures** acts only on photos still in the queue: failed copies are retried; failed scan requests are repeated without re-uploading. Switching to a different watch blocks retry of the previous batch.

## Pairing guidance

The device finder displays connected ADB devices and requires explicit selection if multiple are available. The **pairing port** and **debugging connection port** shown by Wear OS are different. The interface must explicitly distinguish them, explain where each is displayed, and never ask users to run shell commands.

## Visual system

JavaFX 27 + AtlantaFX 3; light/dark theme switcher with the selected preference stored locally, low-noise surfaces, clear hierarchy, rounded interactive elements and status colors with adequate contrast. RU/EN resource bundles are planned. The initial screen has asynchronous 64px image previews and recursive folder import; it is not the final polished layout.

## Accessible behavior

Every action must be keyboard-accessible and state should be textually represented, not only colored. Long-running actions show progress and offer cancellation that interrupts the running ADB subprocess. Send/retry remain disabled until the worker actually stops; failure messages tell users what they can fix. Never say Gallery definitely displays a photo simply because the scan request succeeded.

## Future screens

Image thumbnail grid, explicit multi-device picker, resumable activity view and settings are separately tracked as GitHub Issues.
