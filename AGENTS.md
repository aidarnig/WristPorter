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

**Manual tests are expensive, not a checklist.** The executor first tests autonomously (CI, mocks, real local processes). Ask the owner to use their **iPhone, watch or Windows PC only if the real-device result would materially change implementation, validate a critical integration boundary or unblock genuine acceptance** and cannot be established reliably in automation. Prefer **one well-chosen end-to-end checkpoint** over routine negative cases, repeated smoke tests or testing solely to change `NOT TESTED` to PASS. If warranted, give safe concise steps in the **same executor chat**, use the feedback to fix/retest only affected behavior, and honestly record unperformed hardware checks as `NOT TESTED` without treating this as automatic failure. See [verification decision rule](docs/ENGINEERING_STANDARD.md#3-owner-assisted-real-device-testing).

**Issue readiness:** execute only READY Issues whose predecessors/contracts are satisfied. Use four visible states: READY, IN PROGRESS, NEEDS DEVICE TEST and BLOCKED, synchronized from the Issue body to GitHub labels. Prefer one small testable result per executor chat.

**Independent reviewer mode:** a chat assigned a PR instead of an Issue reads the real current diff, linked Issue, CI and relevant contracts, reports findings on that PR, and does not implement or merge it. Request a second-chat review for risky Bluetooth, network, ADB, signing, shared-API or release changes; details live in the engineering standard.

Keep interim status in the Issue/PR, not duplicate chat-context documents. Update [STATE.md](docs/STATE.md) only for substantive cross-project changes. Finish with a concise Issue/PR link, verified result, remaining limitations and next action.
