# WristPorter engineering workflow

**Binding execution policy.** [AGENTS.md](../AGENTS.md) owns the minimal reading order; this document owns **how executors work**. [INDEX.md](INDEX.md) identifies the authoritative source for each technical fact.

## 1. Roles and autonomous delivery

- **Product owner + coordination chat:** prioritize Issues, decide material product/security/architecture changes, and accept releases and actual hardware compatibility.
- **Executor chat:** owns one Issue or clearly specified PR-sized slice. Recover context from GitHub, make routine implementation decisions, write/test/fix code, update the Issue, and independently merge **safe in-scope** PRs where the conditions below are met.
- **GitHub:** Issue = task, acceptance, progress and blockers; PR = code changes and test evidence; canonical docs = lasting facts; [STATE.md](STATE.md) = short cross-track snapshot. No alternative chat handoff files.

Only a GitHub Issue number/link is needed to start. Follow the reading path in AGENTS; inspect actual `main`, active PRs and dependencies before editing. When Issue details are incomplete but the decision is ordinary, investigate, choose and record acceptance details without sending the owner another context questionnaire.

### Readiness and task sizing

Use four Issue execution states: **READY**, **IN PROGRESS**, **NEEDS DEVICE TEST**, **BLOCKED**. The Issue form or first-line execution-status field is synchronized to four GitHub labels by the issue-status workflow. Labels do not verify prerequisites: inspect predecessor Issues and merged API contracts before acting. Correct stale READY states.

Prefer one independently testable outcome and a focused PR per executor chat. Break an oversized roadmap Issue into linked child Issues, each with a "Depends on #N" field, owned modules, scope/non-goals and acceptance. Keep the original Issue as the milestone anchor; do not copy its full specification into every child.

### Test artifact checkpoints

The Windows preview workflow builds a test-only portable ZIP, checksum and provenance notice for relevant PRs, or when manually dispatched. It does not publish GitHub Releases. The existing W0 experiment branch builds its own Android debug APK. A generic Wear OS preview build is deferred until W1 creates a real installable Wear project. Never confuse a test artifact with a signed stable release.

For owner-assisted Android tests, a trusted checkout includes ci/install-watch-test-apk.ps1. It requires an explicit APK path, one authorized selected ADB watch and a typed INSTALL confirmation; it never pairs, unpairs, resets or silently installs anything. Give the owner a confirmed Actions run and precise test scenario in the same chat, then iterate based on observed feedback.

## 2. Per-Issue execution cycle

1. Confirm goal, scope/exclusions, dependency gates, affected contracts and observable acceptance tests. Check whether real iPhone/watch/PC facts or tests are needed **now or after a build**.
2. Create a unique branch from current `main` (`feat/`, `fix/`, `test/`, `docs/`, `ci/`). One chat owns one working branch. Split an oversized Issue into focused PRs without creating redundant planning documents.
3. Implement, test and repair in scope. Use fake transports and automated checks first **where possible**, but do not replace physical acceptance with simulations. Preserve the shipped Desktop photo path, security and original-image constraints.
4. **At the earliest meaningful hardware checkpoint, follow section 3 in the current chat.** Continue the task after the owner's result; do not defer every real-device test to a final summary.
5. Open a focused PR linked to the Issue; inspect diff, dependencies, CI and documentation. Fix failures, then apply the merge policy (section 4).
6. Record verified outcomes and remaining work in Issue/PR and, only when required, canonical docs. Close the Issue only after all acceptance gates pass. A merged experimental step can leave the Issue open.

A short PR summary must include scope, changed components, **actual** test commands/check status, hardware result or pending checkpoint, relevant docs, risks and rollback. Never report a test as passed when only planned or simulated.

## 3. Owner-assisted real-device testing

**The project owner is an available manual tester, not an unavailable CI environment.** When a task requires an iPhone, Galaxy Watch4 Classic or the owner's Windows PC, the executor must initiate a **guided test in the same working conversation**, at the earliest point when the test is both safe and meaningful. Device/OS information needed to choose an implementation target should be requested **before** building the wrong target. Do not wait for the PR to be final.

**Before requesting a test**, complete everything possible autonomously: build a reproducible artifact or probe, run appropriate automated tests, verify the download/build link and prepare the expected behavior. If there is no actionable artifact, continue development instead of asking the owner to improvise. Prefer small checkpoints (first launch/discovery → first event/upload → reconnect/negative cases) over an overwhelming full test matrix.

**Each request to the owner must contain:**
1. **What is being checked:** Issue, precise scenario / `VERIFICATION.md` test ID and why this checkpoint is necessary.
2. **What to use:** exact artifact/commit/version and a verified GitHub Actions or Release download link (when applicable), target device, prerequisites and a safe installation/rollback method. If a link is not verified, say so; do not invent one.
3. **What to do:** concise, numbered, reproducible user actions in ordinary Russian (or the owner's current language), including the next screen/button/command only where necessary. No factory reset, unpairing, forced install, destructive change or unsupported workaround.
4. **Expected result and failure path:** what success looks like, what to do if a permission prompt or error appears, and how to stop/undo the test safely.
5. **What to report back:** PASS/FAIL/UNSURE, the exact observed screen/error, relevant watch/iOS/Windows/app versions and optional sanitized screenshot. Never ask for real notification contents, Bluetooth identifiers, pairing codes, LAN IPs or personal photos in public Issues/PRs.

**After the owner's response:** interpret it, ask at most the specific follow-up information actually needed, update code/test instructions where necessary, and request a targeted **retest** until accepted or genuinely blocked. Record sanitized evidence in [VERIFICATION.md](VERIFICATION.md) and link the Issue/PR to it. Do not downgrade an available test to a permanent `NOT TESTED` simply because the executor lacks physical hardware.

**Pending user test:** if a question has been posed but not answered, record `AWAITING OWNER TEST` in the Issue/PR as workflow status; the scenario remains **NOT TESTED** in the verification ledger. Keep a safe PR open or isolate unverified functionality behind an OFF-by-default/experimental boundary. If the owner cannot or chooses not to run the test, state the genuine blocker and finish only the independently verifiable portion; do not falsely mark hardware acceptance or ship the feature.

An executor cannot simulate the owner's physical result. The owner's test is **interactive within the chat**, not a promise of unattended background execution.

## 4. PR integration and escalation

**Always-on PR check:** the PR gate has one stable pr-gate job on EVERY PR. It conditionally tests modified Java modules and documentation. Java/docs push-to-main workflows and Windows preview artifacts are separate; a green gate is never hardware acceptance. To enforce this status, the repository owner must enable the pr-gate required check in GitHub branch rules; connector access does not authorize that administration. See CI.md.



**Autonomous merge permitted only when all conditions hold:** approved Issue/scope; small reviewed diff; current green relevant CI/tests with no unexplained skipped gates; no active branch/shared-contract collision; no regression to shipped Desktop behavior; no material new security/network/privacy or destructive behavior; repository permissions allow the merge. Isolated, OFF-by-default experimental foundations may merge while hardware acceptance is pending **only if** the unverified feature is not presented as shipped and its Issue remains open.

**Hold the PR and bring a concise recommendation to the coordinator/owner** for: material scope or shared API change affecting another task; new network exposure or unapproved security/TLS/privacy trade-off; destructive data/bond operations; signing/credentials, licensing, release/tag publication; unverified hardware acceptance; unresolved significant CI/security failure or unsafe conflict. Do not repeatedly seek approval for ordinary technical choices.

**Parallel development:** one owner per branch/shared contract; inspect open PRs before changing shared code. Merge dependency contracts first, update dependent branches from `main`, re-run affected tests after rebase or conflict resolution. Use Issues/PRs for interim updates rather than concurrently rewriting `STATE.md`.

**Important distinction:** merging code ≠ shipping a release ≠ verifying real-device compatibility. See [QUALITY.md](QUALITY.md) and [VERIFICATION.md](VERIFICATION.md).

### Independent second-chat review for high-risk PRs

Use a reviewer chat only for potentially high-impact changes: Bluetooth state/permissions, selected-watch ADB commands, new LAN auth/transport, transfer-byte integrity, signing/release, shared public APIs or large complex diffs. Routine documentation/tests/internal fixes with green CI do not need a second chat.

The reviewer receives the current PR link and independently reads the actual diff, Issue and relevant contracts. It examines security/privacy/battery risks, error/cancel/reconnect flows, regression coverage, CI evidence, device-test gaps and dependency consistency. It leaves blocking findings with file/line and suggested fix, or records "Independent review: no blocking findings at HEAD SHA ..." in the PR. The author fixes findings, the reviewer checks the new head. If both chats use the same GitHub account, a review comment is NOT a formal GitHub approval.

The PR gate flags candidate sensitive paths in its job summary. This is only a review hint, not proof of correctness. **A high-risk PR needs a recorded independent review of its latest head SHA and resolution of blocking findings before any merge.** If no independent reviewer is available, hold the PR for the coordinating chat; a green automated gate alone is insufficient. Material scope/security/release decisions also require owner approval. Real release/device acceptance remains separate.

## 5. Write documentation once

Follow [INDEX.md](INDEX.md), without duplicating stable requirements or full PR histories:

1. **Issue/PR:** live implementation status, blockers, test commands/results, linked commits and owner-assisted test requests/results.
2. **VERIFICATION.md:** only actual physical-device test evidence (date, app commit, exact OS/device, steps, sanitized observations). Pending/blocked/untested scenarios retain the correct status.
3. **Canonical owner:** update only when a lasting requirement/architecture/API/security/CI contract changes. Product → [PRODUCT.md](PRODUCT.md); architecture → [ARCHITECTURE.md](ARCHITECTURE.md)/[BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md); LAN portal → [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md); build/CI → [DEVELOPMENT.md](DEVELOPMENT.md)/[CI.md](CI.md). A consequential approved decision gets a **new** [ADR](adr/README.md); do not rewrite historical ADRs.
4. **STATE.md / ROADMAP.md:** only meaningful cross-track state, milestone or dependency changes. Do not update these for each test run or intermediate PR. Keep public EN/RU docs consistent **when shipped behavior changes**.

Run `python ci/check-doc-links.py` for documentation edits when possible (otherwise state NOT RUN); inspect docs CI. Its green status validates links, not underlying facts.

## 6. Security, verification and handoff

- Desktop: JDK 25, Maven; full `mvn -B -ntp verify` for relevant shared changes and before release. Wear Kotlin/Gradle and its CI are separate; [DEVELOPMENT.md](DEVELOPMENT.md) and [CI.md](CI.md) own detailed commands.
- BLE: explicit permissions and user consent; bounded GATT operations, reconnect and cancellation; ANCS/AMS events cannot be called verified without real Watch4+iPhone evidence. LAN upload: approved trusted-private-network HTTP boundary remains subject to [security and release gates](LOCAL_WEB_PORTAL.md).
- Privacy: do not put private photos/messages, local addresses, Bluetooth identifiers, pairing secrets, credentials, keys, or raw sensitive logs in public artifacts. Avoid destructive changes, unbounded retries, silent telemetry and unauthorized remote commands. Follow [PRODUCT.md](PRODUCT.md) original-photo rule.
- Proof status definitions and hardware test matrix live in [VERIFICATION.md](VERIFICATION.md); **NOT TESTED** is not PASS. Do not invent results or battery measurements.

**End-of-task response:** Issue and PR/merge link; user-visible outcome; executed tests and evidence; owner-assisted test result or actionable pending checkpoint; blockers/risks; next small step. Leave GitHub independently understandable without the chat history.
