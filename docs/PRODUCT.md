# Product requirements

This is the canonical source for **observable product behavior**. Implementation decisions are documented in [ARCHITECTURE.md](ARCHITECTURE.md).

## Primary scenario

Someone with an already configured Galaxy Watch 4 Classic, Windows laptop and no available Android smartphone wants to transfer one or hundreds of photos without typing PowerShell commands.

## v1 acceptance criteria

- First-time pairing and subsequent connection work directly in the UI.
- A user can import local JPEG/PNG images in batches of at least 100.
- Every file receives an individually visible result: copy+scan requested, copied but scan failed, or copy failed.
- Users can retry failed copies and re-request only media scanning for confirmed uploads, without creating duplicate watch images.
- Transfer runs without freezing the interface; failed files do not prevent unrelated files from transferring.
- Images are visible in Samsung Gallery after media scanning, confirmed through a real-watch acceptance test.
- Local image bytes are not modified, existing watch files are not intentionally overwritten.
- A Windows 10/11 installer includes a Java runtime and does not demand preinstalled JDK.

## Boundaries

Target: Windows 10/11 x64 and Samsung Galaxy Watch 4 Classic. Other Wear OS watches are best effort until tested.

No cloud sync, accounts, telemetry, destructive remote management, hidden background upload, or mobile companion app in v1.

A successful `adb` command or media-scan broadcast is not proof that a photo appeared in Samsung Gallery.
