# WristPorter — agent entry point

**Single-task handoff:** in a WristPorter-connected chat, give the executor only `Issue #N` or its GitHub URL. The Issue is the assignment, GitHub is the shared memory, and [this repository](https://github.com/aidarnig/WristPorter) is the source of truth. Do not ask the owner to paste prior chats or duplicate the task description.

## Start in this order

1. **This `AGENTS.md`**, then the **live assigned GitHub Issue**, including its linked PRs and blockers.
2. [Current state](docs/STATE.md) — short snapshot; confirm it against live `main`.
3. [Documentation index](docs/INDEX.md) and [engineering workflow](docs/ENGINEERING_STANDARD.md).
4. [Roadmap](docs/ROADMAP.md) for dependency gates, **only the task-relevant** product/architecture/security/ADR/verification documents from the index and Issue, and the actual code/tests.
5. Check current `main`, open PRs and relevant prior commits. Verify available GitHub tools before claiming access is unavailable.

If the Issue lacks a routine implementation detail, investigate and choose a reasonable approach; update its acceptance checklist if needed. Ask the owner **only** for a material product/security decision, a genuine hardware action or another blocker that cannot be resolved safely from the repository.

## Product invariants

- **Shipped:** Windows Desktop v1.1.2, Java 25/Maven/JavaFX and Wi-Fi ADB transfer of JPEG/PNG to Wear OS. Preserve this standalone path.
- **Planned, not shipped:** independent Kotlin Wear OS Bluetooth ANCS/AMS Bridge; an **optional** iPhone Safari → local Windows → ADB → watch upload mode **within the same Windows app**; and optional Windows APK management. No native iOS app, public cloud, background Internet server or user accounts. See [PRODUCT.md](docs/PRODUCT.md).
- Watch4 Classic ↔ iPhone BLE feasibility, Safari Web Upload and battery behavior are **not hardware-verified**. Never extrapolate device support from CI or another project. Follow [VERIFICATION.md](docs/VERIFICATION.md).
- Accepted JPEG/PNG files must retain **original bytes, resolution and metadata**: no silent resize, compression, re-encoding or quality reduction. A browser-provided iOS representation may differ from the original and needs separate verification; see [LOCAL_WEB_PORTAL.md](docs/LOCAL_WEB_PORTAL.md).
- No watch factory reset, silent pairing/installation, destructive ADB commands, remote ADB/admin interface, private notification logging or committed keys/identifiers. Honor licenses and existing [ADRs](docs/adr/README.md).

## Autonomous execution

**Default: complete the assigned Issue without requiring routine confirmations.** Independently inspect, plan, branch, implement, test, document, open/review a PR and **merge low-risk verified changes** when the precise conditions in [ENGINEERING_STANDARD.md](docs/ENGINEERING_STANDARD.md) are met. Do not ask permission for routine technical choices or every PR.

**Escalate:** unapproved scope change, security/privacy trade-off, public/network exposure, destructive operation, breaking shared contract, release/signing, unverified device-dependent acceptance or an unresolved cross-branch conflict. Never imply hardware testing happened when it did not. If a safe PR can be merged while device acceptance remains pending, label that partial milestone **EXPERIMENTAL / NOT DEVICE-VERIFIED** and leave the Issue open.

**Every handoff:** Issue/PR link, changed behavior, exact tests and CI evidence, real-device PASS/FAIL/NOT TESTED, known limits, and next action. Keep interim status in GitHub Issues/PRs; update [STATE.md](docs/STATE.md) only for durable cross-track changes. No redundant context documents.

**Documentation validation:** `python ci/check-doc-links.py` and the [documentation CI](.github/workflows/docs-check.yml) check links and required files, not technical correctness or physical devices.
