# WristPorter — Quick start

[English](QUICK_START.md) · [Русский](QUICK_START.ru.md)

WristPorter sends JPEG and PNG photos from Windows to compatible Wear OS watches over Wi-Fi. It is a local utility: no cloud account, Android smartphone or extra app on the watch is required **after the watch's initial setup**.

## Start on Windows

1. Extract the entire `WristPorter-...-windows-x64.zip` archive.
2. Open the `WristPorter` directory and run `WristPorter.exe`.
3. Keep the `runtime/`, `app/` and `platform-tools/` directories alongside the EXE. **ADB and Java are already included**; you do not have to install either.
4. You only need to connect your computer and watch to the same Wi-Fi network. Some networks isolate devices; use a normal home network if connection fails.

## Pair the watch (first time)

1. On Galaxy Watch 4 Classic, enable **Developer options** in Settings → About watch → Software information by repeatedly tapping the software version.
2. Open Developer options and enable **ADB debugging** and **Wireless debugging**.
3. Select **Pair device with pairing code** on the watch. Read the IP address, pairing port and six-digit code.
4. Enter the IP and **pairing port** in WristPorter's first pairing section, then the six-digit code. Click **Pair**.
5. Return to the main Wireless debugging page on the watch. Its **connection/debugging port is different from the pairing port**.
6. Enter the IP and connection port in WristPorter's connection section and click **Connect**. Already paired devices can also be discovered.

## Send photos

1. Drag JPEG/PNG files or folders into the app, or use the file/folder pickers.
2. Check the photo list and click **Send to watch**.
3. WristPorter copies each photo and requests Samsung Gallery media indexing.
4. Open Gallery on the watch. If a copied photo was not indexed, use **Retry failures** to repeat its scan request without copying the file twice.

## Windows SmartScreen

The unsigned EXE may display an **Unknown publisher** SmartScreen warning. Verify the file comes from the official Releases page and compare its SHA-256 with the published checksum before deciding whether to trust it. Do not disable Windows security. See [Security and signing](SECURITY.md).

## Troubleshooting

- **No watch found:** Confirm that wireless debugging is active, both devices share Wi-Fi, and the currently displayed debugging port is correct.
- **Pairing fails:** Pairing and connection use different ports. Codes expire; generate a new code on the watch.
- **Wrong Java icon in Explorer:** Windows may cache a previously seen icon for the same EXE path. Extract the latest ZIP into a *new directory*, refresh Explorer, and check the icon again. The EXE icon is verified during packaging; see [Branding](BRANDING.md).
- **ADB missing:** The portable ZIP normally includes `platform-tools/adb.exe` and two DLL files. Do not move the EXE outside the extracted directory. Advanced settings can select a custom ADB executable.
- **Photo not visible:** A successful scan request is not proof that Samsung Gallery displayed the image. Close and reopen Gallery and check the operation result.

Turn **Wireless debugging** off after use to reduce battery use and limit access to the watch.

WristPorter is an independent open-source project and is not affiliated with Samsung or Google.
