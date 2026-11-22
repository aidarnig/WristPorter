# WristPorter — agent entry point

**Start with one GitHub Issue.** An executor chat needs only `WristPorter Issue #N` (or its URL). GitHub `main`, the assigned Issue and linked PRs are the source of truth; do not ask the owner to repeat previous chats.

## Minimal reading path

1. Read **this file**, then the **live assigned Issue** and linked PRs/comments.
2. Skim [current state](docs/STATE.md) and verify it against live `main` and relevant open PRs.
3. Follow [engineering workflow](docs/ENGINEERING_STANDARD.md) for execution, tests, autonomous integration and handoff.
4. Consult the [documentation index](docs/INDEX.md) to read **only task-relevant** design, roadmap, ADR, build, security and verification pages. Inspect relevant code and tests; do not load the entire docs tree by default.

Verify available repository tools before claiming access is missing. Resolve routine ambiguities by inspecting code and documenting the chosen approach in the Issue.

## Non-negotiable product boundaries

- Shipped Desktop: Windows portable, Java 25/Maven/JavaFX, Wi-Fi ADB JPEG/PNG → Wear OS Gallery. Preserve this independent photo path.
- Planned separately: Kotlin Wear OS iPhone BLE ANCS/AMS Bridge; **optional** Safari → the **same** Windows app → Wi-Fi ADB → watch; Windows APK manager. No native iOS app or public cloud. Consult [PRODUCT.md](docs/PRODUCT.md) and relevant [ADRs](docs/adr/README.md).
- Accepted JPEG/PNG transfer payloads preserve original **bytes, resolution and metadata**; never silently compress/re-encode. Safari may provide a transformed representation; do not claim original-iPhone-file identity without measurement.
- No factory resets, destructive watch commands, silent pairing/APK installation, unauthorized LAN exposure, remote ADB admin, sensitive logs, committed secrets or unsupported compatibility claims. Follow the task's security and verification gates.

## Work autonomously; test with the owner interactively

Own the assigned Issue: investigate, implement, run available automated checks, document, open/review PRs and integrate **low-risk verified** changes according to the [engineering standard](docs/ENGINEERING_STANDARD.md). Escalate only material decisions/risks, not routine implementation choices.

**Required:** if the Issue needs a test on the owner's **iPhone, Wear OS watch or Windows PC**, request it **as soon as a safe meaningful test is possible** — including prerequisite firmware/device checks **before coding** when necessary. In that **same executor chat**, give exact step-by-step instructions and the expected result, ask for the observed outcome, then use the feedback to continue/fix/retest. **Do not merely leave `NOT TESTED` in a final report when the owner can perform an actionable test.** Never invent hardware evidence. See [owner-assisted verification](docs/ENGINEERING_STANDARD.md#3-owner-assisted-real-device-testing).

Keep interim status in the Issue/PR, not duplicate chat-context documents. Update [STATE.md](docs/STATE.md) only for substantive cross-project changes. Finish with a concise Issue/PR link, verified result, remaining limitations and next action.
