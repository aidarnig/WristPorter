# WristPorter — mandatory agent/developer entry point

This file is the durable operating contract for **every new chat, AI coding agent and human contributor**. GitHub `main` and its Issues/PRs are the source of truth; previous chat histories are not. Read this before writing code, creating branches or reporting progress.

## Read in this order

1. [Current state / chat handoff](docs/STATE.md) — what exists **now**, next task, known risks.
2. [Roadmap](docs/ROADMAP.md) — versioned milestones, scope, dependencies and issue links.
3. [Feature feasibility](docs/BRIDGE_CAPABILITIES.md) — what works in principle without an iOS app, what requires device testing, what is unsupported.
4. [Architecture](docs/ARCHITECTURE.md) and [Bridge design](docs/BRIDGE_ARCHITECTURE.md) — existing Java vs new Kotlin Wear OS boundaries.
5. [Engineering standard](docs/ENGINEERING_STANDARD.md) — branch, change, verification, documentation and review requirements.
6. [Verification ledger](docs/VERIFICATION.md) and [Quality gates](docs/QUALITY.md) — never substitute speculation or unit tests for physical hardware.
7. [Architecture decisions](docs/adr/README.md) and the GitHub **Issue for your task**.

The [docs index](docs/INDEX.md) assigns a unique owner to each type of information. Check actual files, live issues, open PRs and latest commits on GitHub **before** acting. Do not assume a missing tool or access without attempting connector discovery.

## Fixed decisions for the current program

- **No iOS companion application for this program.** No iOS project, iOS app deployment, cloud relay, new account system or backend. Feasibility of other designs may be documented, but implementation requires a new explicit product decision.
- Preserve released **WristPorter Desktop v1.1.2**: Windows / Java 25 / Maven / JavaFX / bundled ADB / JPEG+PNG Gallery transfer. It remains useful *without* the watch bridge.
- Build new **WristPorter Bridge** as an independent **Kotlin Wear OS APK**, with Bluetooth interaction directly between an already configured watch and iPhone.
- Main target: **Samsung Galaxy Watch4 Classic**. Existing photo transfer is hardware-confirmed. **Bluetooth-to-iPhone is not yet confirmed on this model in WristPorter.**
- Apple's **ANCS** / **AMS** are the proposed documented protocol surfaces; extra BLE services require real-device discovery. Never call untested hardware or iOS functionality supported.
- No factory reset, destructive watch operations, silent APK installation, pairing without user consent, collection of private notifications in logs, telemetry or uncontrolled retry loops.
- Public customer copy must not claim an iPhone watch setup/pairing replacement, full Galaxy Wearable replacement, Health sync, arbitrary replies or watch call audio.
- Wear OS `minSdk` is **not decided**: first inspect actual Watch4 Classic firmware/API in W0; do not silently inherit WeakBridge's API 33 requirement.
- Respect third-party licenses. Reviewing external open-source code does **not** automatically authorize wholesale copying without attribution and license compliance.

## One issue, one branch, one verification story

- Select one [W-series milestone](docs/ROADMAP.md) and an Issue/task; inspect live state and dependencies.
- Work in `feat/<scope>`, `fix/<scope>`, `docs/<scope>` or `test/<scope>`. Do not mix unrelated requirements; link the Issue in PR.
- State goals, non-goals, risks and acceptance checks **before** implementation. Keep existing desktop behavior intact.
- Add tests for protocol and state behavior; apply [Quality](docs/QUALITY.md) and [Verification](docs/VERIFICATION.md). Say **NOT RUN** or **NOT TESTED** when applicable.
- Update only the canonical docs that changed. Add an ADR for any substantial scope/technology/security/lifecycle decision.
- Each PR must contain: Issue, summary, changed components, tests executed/results, physical-device evidence or **not tested**, risk/rollback, docs changed.
- After merge, update [STATE.md](docs/STATE.md) and the relevant Issue/verification ledger **in the same PR or a clearly linked follow-up**; verify `main`. A merged PR is not hardware acceptance.
- Never commit pairing codes, Bluetooth MACs, IP addresses, ADB keys, personal messages/photos, passwords, build outputs or signing keys.

## Starting a fresh conversation

Paste only a **task/Issue number** and the repository URL if convenient. Instruct the new agent to read **`AGENTS.md` and `docs/STATE.md` on GitHub first**, then the linked owners, inspect the actual branch and continue that Issue. Do not rely on a chat-generated handoff alone. The required end-of-task handoff format is in [STATE.md](docs/STATE.md).

**Canonical repository:** https://github.com/aidarnig/WristPorter
