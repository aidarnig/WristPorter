# Quality and release verification

This file owns test strategy and release gates. CI trigger and cost policy live in [CI.md](CI.md).

## Automated checks

- Domain: JPEG/PNG validation, safe names, folder import
- ADB: parsing, endpoint validation, subprocess failure handling and pairing code stdin
- Core: transfer states, partial failures, cancellation, scan-only retries
- CI: fake Maven process verifies affected-module routing without building irrelevant modules

Fake-ADB integration tests exercise the transport without touching a real watch.

## Portable acceptance

The manual Windows packaging pipeline must: run full Maven verification, create the Windows app-image, validate bundled runtime/app directories, verify that `adb.exe` and its two Windows DLLs exist, execute `adb version`, retain third-party notices, and generate a SHA-256 for the finished ZIP.

A successful CI package **does not** verify UI launch after extraction. Before a public release, test on a clean Windows 10/11 x64 system without Java, Android Studio, or administrator privileges; confirm that the EXE launches, bundled ADB works, and connecting to a watch is possible.

## Hardware acceptance

Reference hardware: Galaxy Watch 4 Classic. The pairing/reconnect and eight-photo Gallery scenario were accepted on 2026-11-11. Cancellation, retry, Unicode paths and subprocess behavior are covered by automated checks where implemented. A 100-photo stress test, storage-full test, and Wi-Fi fault injection are recommended future regression tests, **not** 1.0 release blockers by product-owner decision.

## Manual verification log

### 2026-11-11 — Galaxy Watch 4 Classic, WristPorter 0.1.0 Preview #3

**Status: PASS for the observed batch-transfer scenario.**

The user tested the portable Windows ZIP published as [v0.1.0-preview.3](https://github.com/aidarnig/WristPorter/releases/tag/v0.1.0-preview.3) on a Windows desktop with a Samsung Galaxy Watch 4 Classic over wireless ADB.

Observed and confirmed in screenshots and user feedback:

- The extracted desktop application launched successfully.
- The bundled ADB executable was detected automatically, without a separate Android SDK installation.
- The device appeared in the ADB discovery control.
- Both light and dark themes rendered.
- Eight PNG images were imported with preview thumbnails.
- The transfer completed **8 of 8** with **zero reported failures**.
- Each file's status indicated that copying succeeded and media indexing was requested.
- The user confirmed that the photos became visible in the watch's Samsung Gallery **immediately**, without manual commands.

**Evidence:** user-provided screenshots and confirmation in the project conversation. The screenshots are not committed to this public repository because they include personal desktop content and local device information.

**Follow-up confirmation from user:** Initial pairing **was performed successfully in the app**; after restarting it, **Discover devices** found the previously connected watch again. The screenshots also revealed UX issues (tiny progress/results, excess whitespace in single-column queue, overly wide advanced settings control). These findings motivated the 1.0 UI improvements.

**Still unverified manually:** clean Windows without developer tools, 100-photo stress batch, Wi-Fi disruption, cancellation/retry under induced failures, full storage, Windows 10 and other Wear OS hardware.

## Release gate

**Stable 1.0 release decision (2026-11-11):** The product owner accepts the verified portable Windows launch, successful in-app pairing/re-discovery and real Watch 4 Classic eight-photo transfer, plus passing automated tests for other key workflows. The separate 100-photo and induced-cancellation/retry tests are **not required** for 1.0. Stable publication requires a green full Maven reactor, successful Windows app-image packaging, verified ADB/Java runtime and SHA-256 release assets. Limit the compatibility claim to the tested Watch 4 Classic scenario and disclose unverified clean-PC/Windows 10 conditions. Third-party license/provenance files must remain bundled; additional legal/trademark review is tracked separately before broader promotion.

**Current state (2026-11-11):** The stable [WristPorter 1.0.0](https://github.com/aidarnig/WristPorter/releases/tag/v1.0.0) release was published after [successful full Windows verification and checksum validation](https://github.com/aidarnig/WristPorter/actions/runs/43060788068). The prior reference Watch 4 Classic photo-transfer/pairing tests passed; the redesigned 1.0 UI has compiled successfully but has not yet been visually rechecked by the user. See the release notes for device and test-scope limits.


## 2026-11-11 — WristPorter 1.1.0 release verification

- [Release v1.1.0](https://github.com/aidarnig/WristPorter/releases/tag/v1.1.0) published as a stable Windows x64 ZIP.
- [GitHub Actions run 43065389395](https://github.com/aidarnig/WristPorter/actions/runs/43065389395) completed full Java verification, Windows app-image packaging with the branded `.ico`, bundled ADB validation, portable ZIP creation and SHA-256 verification.
- The English/Russian resource bundles have parity tests, and live localization plus the theme-aware logo compile in [selective Java CI](https://github.com/aidarnig/WristPorter/actions/runs/43065389440).
- Release branding includes 1280×640 `docs/assets/social-preview.png`; it must still be selected manually in GitHub repository Social preview settings.
- The README illustration files are **mockups**, clearly labeled and free from personal/device data. They are not evidence that the newly localized 1.1.0 interface was independently tested on a physical Windows PC.
- The photo-transfer protocol is unchanged from the hardware-accepted Galaxy Watch 4 Classic scenario. Other watches and new language/theme interactions are not independently hardware/GUI-verified.


## 2026-11-11 — 1.1.1 regression scope

User screenshots of v1.1.0 revealed a missing smartwatch symbol in the header, incorrectly rendered runtime/taskbar icon and a tagline alignment issue. A red SmartScreen Unknown publisher warning also appeared; the unsigned executable requires genuine trusted signing to mitigate it. The 1.1.1 fix uses bundled logo PNGs for the JavaFX header/window, checks the embedded EXE icon, and includes bilingual portable guides. Manual Windows visual retesting and future code signing remain outstanding.

### Verified 1.1.1 release build

[WristPorter v1.1.1](https://github.com/aidarnig/WristPorter/releases/tag/v1.1.1) is published. [Windows release workflow](https://github.com/aidarnig/WristPorter/actions/runs/43068975192) succeeded: Java/resource tests, bundled ADB, icon comparison (354 blue pixels in both source ICO and embedded EXE icon), bilingual quick-start file checks, ZIP checksum verification and GitHub Release upload. The main window's new PNG-based logo is not yet visually verified by the user on Windows; SmartScreen remains an expected possibility until trusted code signing/reputation requirements are met (tracked in issue #9).


## 2026-11-11 — 1.1.2 logo and Windows icon validation

The [v1.1.2 stable release](https://github.com/aidarnig/WristPorter/releases/tag/v1.1.2) was published after [full Windows packaging and SHA-256 checks](https://github.com/aidarnig/WristPorter/actions/runs/43072311593), alongside green [selective Java tests](https://github.com/aidarnig/WristPorter/actions/runs/43072311571).

- Canonical light/dark SVG logos use a wider 1380-pixel canvas so the trailing "r" of WristPorter remains within the image. The export script checks transparent padding along the right edge; Java resource tests verify that PNGs have a safe right margin.
- `ci/render-brand.py` verifies that the Windows ICO contains sizes 16, 32, 48 and 256 px. Windows packaging also checks that the generated EXE embeds the blue WP watch icon.
- The portable EXE now has explicit description/vendor metadata. This is not a trusted Authenticode signature and does not remove SmartScreen publisher warnings.
- Windows Explorer may display a stale previously cached Java icon for the same EXE path. Users should check a *new extraction directory* before treating it as a packaging regression.
- **Remaining manual acceptance:** user confirmation of the complete title wordmark and Explorer's icon on their desktop. CI verifies resources and EXE packaging but cannot assert the user's local icon-cache state.


## 2026-11-11 — WristPorter 1.1.2 visual acceptance

**Status: PASS for the inspected RU/EN application interface.** The product owner supplied two full application screenshots (Russian and English) and confirmed that everything now looks correct and no visible bugs were observed.

Confirmed on the user's Windows desktop:
- The approved smartwatch WP icon and complete “WristPorter” wordmark render correctly.
- The description/tagline is aligned in the header.
- Language switching displays both Russian and English labels correctly.
- The queue, device controls, connection status, log and prominent transfer-progress panel display in both language versions.
- Previous in-app pairing, discovery and photo-transfer acceptance remains unchanged.

The latest user-supplied screenshots additionally show a completed **9/9 transfer with 0 reported errors**, in both language modes. This confirms the application's reported result, not a fresh independent observation of Samsung Gallery. The earlier real-watch Gallery confirmation remains recorded separately. The public README now uses optimized screenshots with the private watch IP and debugging port masked; original images have not been committed.
