# WristPorter — mandatory agent/developer entry point

This file is the durable operating contract for **every new chat, AI coding agent and human contributor**. GitHub `main` and its Issues/PRs are the source of truth; previous chat histories are not. Read this before writing code, creating branches or reporting progress.

## Issue-first onboarding: mandatory reading order

A new executor chat receives only a repository Issue URL/number (and repository name if it is not already in the workspace). **The Issue is the assignment; the repository is the context.** Do not ask the user to restate chat history or paste full documentation. Retrieve current GitHub data yourself.

1. Read this `AGENTS.md` first, then [current shared state](docs/STATE.md). Treat the state snapshot as provisional until checked against GitHub.
2. Read the **assigned live Issue** (goal, scope, exclusions, dependencies, acceptance, deliverables), then [ROADMAP.md](docs/ROADMAP.md) for dependency gates.
3. Read [INDEX.md](docs/INDEX.md) and [ENGINEERING_STANDARD.md](docs/ENGINEERING_STANDARD.md) for document ownership and the execution/PR contract.
4. Read only **task-relevant** product, feasibility, architecture, security, verification, quality and ADR sources. For Bridge start with [BRIDGE_CAPABILITIES.md](docs/BRIDGE_CAPABILITIES.md) and [BRIDGE_ARCHITECTURE.md](docs/BRIDGE_ARCHITECTURE.md); for browser transfer with [LOCAL_WEB_PORTAL.md](docs/LOCAL_WEB_PORTAL.md) and ADR-0004. Follow links from the Issue; inspect actual code and tests.
5. Verify latest `main`, existing branches/PRs and dependency results **before editing**. Check connected GitHub tool availability rather than claiming access is impossible without trying.

If a blocking hardware fact, required owner decision, unclear issue acceptance or conflicting contract prevents safe progress, record the specific blocker in the Issue and request a decision. Otherwise execute the defined task without repeatedly asking for background.

## Fixed decisions for the current program

- **No iOS companion application for this program.** No installed iOS application, iOS app deployment, cloud/public backend or new account system. A **user-started local Windows-hosted browser Web Upload server** is explicitly in scope under [ADR-0004](docs/adr/0004-one-desktop-optional-lan-portal.md); it is not an Internet service or an installed iPhone app. Feasibility of other designs may be documented, but implementation requires a new explicit product decision.
- **North star:** a local-first WristPorter ecosystem for Wear OS owners switching from Android to iPhone — independent Watch Bridge, Windows photo/gallery transfer, optional iPhone Safari→Windows→watch upload and Windows APK management; all planned features must be clearly labeled NOT SHIPPED until tested.
- Preserve released **WristPorter Desktop v1.1.2**: Windows / Java 25 / Maven / JavaFX / bundled ADB / JPEG+PNG Gallery transfer. It remains useful *without* the watch bridge.
- Build new **WristPorter Bridge** as an independent **Kotlin Wear OS APK**, with Bluetooth interaction directly between an already configured watch and iPhone.
- **One Windows app, not two:** opt-in QR/PIN browser mode runs on Windows and routes selected iPhone photos to the watch via the existing Wi-Fi ADB transfer core. The PC must remain on. No public listener, anonymous upload, persistent server, remote ADB admin or untested HEIC support. See [Local Web Portal](docs/LOCAL_WEB_PORTAL.md).
- Main target: **Samsung Galaxy Watch4 Classic**. Existing photo transfer is hardware-confirmed. **Bluetooth-to-iPhone is not yet confirmed on this model in WristPorter.**
- Apple's **ANCS** / **AMS** are the proposed documented protocol surfaces; extra BLE services require real-device discovery. Never call untested hardware or iOS functionality supported.
- No factory reset, destructive watch operations, silent APK installation, pairing without user consent, collection of private notifications in logs, telemetry or uncontrolled retry loops.
- Public customer copy must not claim an iPhone watch setup/pairing replacement, full Galaxy Wearable replacement, Health sync, arbitrary replies or watch call audio. **Direct** iPhone-to-watch photo transfer without PC is not included; **browser-assisted via running Windows** is a separate planned feature.
- Wear OS `minSdk` is **not decided**: first inspect actual Watch4 Classic firmware/API in W0; do not silently inherit WeakBridge's API 33 requirement.
- Respect third-party licenses. Reviewing external open-source code does **not** automatically authorize wholesale copying without attribution and license compliance.

## One issue, one branch, one verification story

- Select one [W-series milestone](docs/ROADMAP.md) and an Issue/task; inspect live state and dependencies.
- Work in a **fresh issue-scoped branch from current `main`**, using `feat/<scope>`, `fix/<scope>`, `docs/<scope>`, `test/<scope>` or `ci/<scope>`. Never share a working branch between executor chats; link the Issue in PR.
- State goals, non-goals, risks and acceptance checks **before** implementation. Keep existing desktop behavior intact.
- Add tests for protocol and state behavior; apply [Quality](docs/QUALITY.md) and [Verification](docs/VERIFICATION.md). Say **NOT RUN** or **NOT TESTED** when applicable.
- Update only the canonical docs that changed. Add an ADR for any substantial scope/technology/security/lifecycle decision.
- Each PR must contain: Issue, summary, changed components, tests executed/results, physical-device evidence or **NOT TESTED**, risk/rollback, docs changed and any requested product-owner decision. **Do not merge or publish releases without explicit owner approval.**
- During parallel work, record in-progress status and evidence in the **Issue/PR**, not competing rewrites of [STATE.md](docs/STATE.md). At integration, refresh STATE and the verification ledger **in the approved PR or a clearly linked follow-up**; confirm `main`. A merged PR is not hardware acceptance.
- Never commit pairing codes, Bluetooth MACs, IP addresses, ADB keys, personal messages/photos, passwords, build outputs or signing keys.

## Starting a fresh executor conversation

**Minimum handoff:** `https://github.com/aidarnig/WristPorter/issues/NN` (or `WristPorter Issue #NN` if the repository is already connected). This contract is automatic: read the files above, verify live state, work within the Issue, run accessible tests, open a focused PR and report its URL and untested hardware steps. **Do not assume permission to merge.**

The [engineering standard](docs/ENGINEERING_STANDARD.md) defines exact startup, parallel branch ownership, documentation writing order and finish criteria. The [state handoff](docs/STATE.md) is a global snapshot, **not** a replacement for the Issue or PR.

**Canonical repository:** https://github.com/aidarnig/WristPorter

**Documentation check:** run `python ci/check-doc-links.py` when changing documentation; [docs integrity CI](.github/workflows/docs-check.yml) checks required context sources and internal links. This does not replace hardware verification.
