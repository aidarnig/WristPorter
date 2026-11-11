# User experience

This file owns **screen structure, interaction and visual guidance**. Functional acceptance criteria live in [PRODUCT.md](PRODUCT.md).

## Initial screen

A header identifies the app. The connection sidebar guides ADB discovery, first pairing and connection. The primary workspace provides a drag-and-drop zone, file and folder pickers, selected photos, transfer results and a prominent Send button. The footer shows task progress.

## Pairing guidance

The **pairing port** and **debugging connection port** shown by Wear OS are different. The interface must explicitly distinguish them, explain where each is displayed, and never ask users to run shell commands.

## Visual system

JavaFX 27 + AtlantaFX 3; dark initial theme, low-noise surfaces, clear hierarchy, rounded interactive elements and status colors with adequate contrast. A light theme and RU/EN resource bundles are planned. The initial screen has asynchronous 64px image previews and recursive folder import; it is not the final polished layout.

## Accessible behavior

Every action must be keyboard-accessible and state should be textually represented, not only colored. Long-running actions show progress and offer cancellation that interrupts the running ADB subprocess; failure messages tell users what they can fix. Never say Gallery definitely displays a photo simply because the scan request succeeded.

## Future screens

Image thumbnail grid, explicit multi-device picker, resumable activity view and settings are separately tracked as GitHub Issues.
