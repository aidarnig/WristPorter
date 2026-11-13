# Watch compatibility — Windows photo transfer

> This document covers **the shipped Windows ADB photo-transfer workflow only**. The proposed independent iPhone Bluetooth bridge has **no hardware test evidence yet**; see [Bridge capabilities](BRIDGE_CAPABILITIES.md) and [verification ledger](VERIFICATION.md).

[**English**](COMPATIBILITY.md) · [Русский](COMPATIBILITY.ru.md) · [Back to README](../README.md)

WristPorter uses **wireless Android Debug Bridge (ADB)** to copy JPEG/PNG files from Windows to a selected watch. After copying, it asks Android's media scanner to index the image. The scanner request currently uses a legacy Android broadcast proven to work on **Samsung Galaxy Watch4 Classic**; another watch may accept the file but not display it in Gallery.

**Two different things must be checked:** (1) pairing and file transfer over ADB, and (2) photo visibility in the watch's own gallery. A successful ADB transfer alone is not a confirmed Gallery result.

## Confirmed on a physical watch

| Device | Wireless pairing and transfer | Images visible in Gallery | Evidence |
| --- | --- | --- | --- |
| **Samsung Galaxy Watch4 Classic** | **Yes** | **Yes, Samsung Gallery** | [Manual verification log](QUALITY.md#manual-verification-log) — eight PNGs verified in Gallery; later 9/9 transfer reported by the app |

Only this model has confirmed end-to-end hardware testing. The result does not certify every firmware version or network.

## Wear OS watches worth trying (not verified)

| Manufacturer | Example Wear OS models | Status |
| --- | --- | --- |
| **Samsung** | Galaxy Watch4 / 4 Classic, Watch5 / 5 Pro, Watch6 / 6 Classic, Watch7, Watch FE, Watch Ultra; newer Wear OS Galaxy Watches | **Other models untested** |
| **Google** | Pixel Watch, Pixel Watch 2 / 3 | **Untested** |
| **OnePlus** | Watch 2 / 2R | **Untested** |
| **Xiaomi** | Watch 2 / 2 Pro | **Untested** |
| **Mobvoi** | TicWatch Pro 5 / Pro 5 Enduro, TicWatch Atlas | **Untested** |
| **OPPO** | Watch X | **Untested** |
| **Other Wear OS brands** | TAG Heuer Connected, Montblanc Summit 3 and other models | **Untested** |

These examples are based on published [Google Wear OS watch listings](https://wearos.google.com/) and [Samsung's Wear OS watch guidance](https://www.samsung.com/us/support/answer/ANS10003455/), **not** WristPorter test results. Model availability, firmware and debugging capabilities vary; the table is not a support guarantee.

### Requirements for trying another watch

- The watch must run **Wear OS with wireless ADB debugging**, and must already be set up.
- Developer options, ADB debugging and wireless debugging must be available and enabled.
- The Windows PC and the watch must be on a Wi-Fi network that allows direct device-to-device connections.
- Use the **pairing** address/port and six-digit code first, then the separate **connection** port.
- WristPorter sends **JPEG/PNG** to **/sdcard/DCIM/Camera** and requests media indexing. Your gallery may index these files differently, or may not expose them.

Read [Google's official Wear OS wireless debugging instructions](https://developer.android.com/training/wearables/get-started/debug-wifi) or our [Quick start](QUICK_START.md).

### Phones and unsupported watches

WristPorter **doesn't pair a watch with an iPhone** and doesn't replace the compatible Android phone that some Wear OS models require for their initial setup. In particular, Samsung says Galaxy Watch4 and other newer Wear OS Galaxy Watches aren't compatible with iOS. See [Samsung's phone compatibility page](https://www.samsung.com/us/support/answer/ANS10004668/).

**Not supported:** Apple Watch (watchOS), older Samsung Galaxy Watch / Watch3 / Active models running Tizen, and watches running HarmonyOS, Zepp OS or other non-Wear OS platforms. Some brands sell both Wear OS and non-Wear OS models; check the **specific model**, not just the brand.

## Help us verify another watch

Have a Wear OS watch not listed as tested? [Open a watch compatibility report](https://github.com/aidarnig/WristPorter/issues/new?template=watch-compatibility.yml).

Please report the **exact model**, Wear OS version, whether pairing worked, whether JPG/PNG copies succeeded, and **whether you actually saw the images in Gallery**. A transfer result shown in WristPorter is not sufficient evidence for the last step.

Do **not** share your watch's IP address, serial number, pairing code or private photos. Reports will be reviewed before a model is marked as tested.
