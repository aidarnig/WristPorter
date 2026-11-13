# WristPorter 1.1.2 — Photos from Windows to your Wear OS watch

WristPorter sends JPG and PNG pictures from Windows to a compatible Wear OS watch over Wi-Fi. The tested Samsung Galaxy Watch4 Classic shows the transferred pictures in Samsung Gallery. No Android phone is needed for the transfer **after the watch has been set up**.

## Download

Download **WristPorter-1.1.2-windows-x64.zip** under **Assets** below, not the automatically generated *Source code* archive. Extract the whole ZIP and run **WristPorter.exe** inside the WristPorter folder.

Java and ADB are bundled. No installer, Android Studio or separate phone app is needed for the transfer.

## What you can do

- Pair and connect to the watch using wireless ADB.
- Add multiple JPG/PNG photos or entire folders, including drag-and-drop.
- See previews, progress and per-photo transfer results; retry failed operations.
- Use the English or Russian interface, in light or dark mode.

**In 1.1.2:** the app's wordmark and Windows icon were polished, and the English/Russian presentation was improved. Photo-transfer behavior is unchanged.

## Compatibility and first steps

**Tested:** Samsung Galaxy Watch4 Classic. Other Wear OS watches with wireless debugging may work, but their Gallery behavior has **not** been verified. See the [compatibility guide](https://github.com/aidarnig/WristPorter/blob/main/docs/COMPATIBILITY.md) and [quick start](https://github.com/aidarnig/WristPorter/blob/main/docs/QUICK_START.md).

WristPorter cannot pair a Galaxy Watch with an iPhone and cannot replace an Android phone needed for initial watch setup.

## Security

This Windows EXE is **not yet signed by a trusted publisher**. Windows SmartScreen may warn about an unknown publisher. Download from the official GitHub repository and check the ZIP SHA-256 using the accompanying **.sha256** asset. Do not disable Windows security. [Details](https://github.com/aidarnig/WristPorter/blob/main/docs/SECURITY.md).

---

Full technical history: [CHANGELOG.md](https://github.com/aidarnig/WristPorter/blob/main/CHANGELOG.md).

