# WristPorter 1.0.0

**First stable Windows x64 portable release**

## Included

- Native JavaFX desktop interface with light and dark themes and automatic ADB detection
- Built-in Java Runtime, ADB executable and required DLL files; unzip and launch without an installer
- Pairing and reconnecting to Galaxy Watch 4 Classic over local Wi-Fi debugging
- JPEG/PNG file and folder import with an adaptive photo gallery
- Enlarged progress indicator and detailed transfer results
- Safe ADB photo copy and automatic Samsung Gallery media scanning
- Cancel and retry controls; scan-only retry avoids uploading a copied photo twice

## Verification

The product owner confirmed successful in-app pairing, discovery after restart, and **8 of 8 PNGs** transferring with immediate appearance in Galaxy Watch 4 Classic's Samsung Gallery. Automated Java and fake-ADB checks cover transport, retries and core behavior. The stable artifact is released only if the full Windows packaging/test workflow succeeds.

## Installation

1. Under **Assets**, download `WristPorter-1.0.0-windows-x64.zip` (not GitHub's automatically generated source archives).
2. Extract the ZIP completely.
3. Run `WristPorter/WristPorter.exe`.

The package contains Java, ADB and license notices. The separate SHA-256 asset can be used to verify integrity.

## Scope and limitations

Officially tested scenario: **Samsung Galaxy Watch 4 Classic, Windows desktop, Wi-Fi ADB, JPEG/PNG**. Windows 10 clean-machine launch, other Wear OS models, 100-image stress batches, network failures and manual cancellation/retry stress are not independently verified. Wireless debugging must be enabled on the watch; disable it after use. This independent app is not affiliated with Samsung or Google.
