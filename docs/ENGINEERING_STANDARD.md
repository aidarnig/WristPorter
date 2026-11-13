# WristPorter engineering workflow

**Binding procedure for executor chats.** Read [AGENTS.md](../AGENTS.md) first; this file defines *how work gets done*. Feature requirements and detailed design belong to their [canonical document owners](INDEX.md), not here.

## 1. Ownership and autonomy

- **Coordinator chat + product owner:** choose priorities and independent Issues, accept material product/security/architecture decisions and release/device-acceptance gates. The coordinator reviews cross-track dependencies and merge conflicts; it does **not** repeat implementation work.
- **Executor chat:** owns one assigned Issue or explicit PR-sized slice, discovers missing ordinary details, makes routine technical decisions, implements/tests/fixes, updates its Issue/PR and integrates safe changes. It must not depend on private previous-chat context.
- **GitHub:** current `main` plus Issues/PRs are authoritative for code and task status. [STATE.md](STATE.md) is a small overview, **not a log of each commit**.

Minimum prompt: `WristPorter Issue #N`. The mandatory reading sequence is defined **once** in [AGENTS.md](../AGENTS.md). Do not copy all linked documents into an Issue or another chat. If an Issue is underspecified, inspect relevant code and define executable acceptance steps in that Issue without asking the owner to repeat known facts.

### Decisions executors make without asking

Library structure within approved constraints, implementation approach, test design, bug fixes inside assigned scope, routine refactors, internal UX details consistent with [UX.md](UX.md), small ADR-free improvements, writing documentation, opening PRs and resolving non-semantic merge conflicts.

### Escalate to coordinator/owner only when necessary

- Changing product scope, supported platforms, core architecture or a published API used by another active task.
- New network exposure, security/TLS risk acceptance, reduced authentication/privacy, sensitive data handling or consequential license changes.
- Destructive actions, factory resets, pairing/bond removal, credential or signing-key changes, release/tag publication, public claims of compatibility.
- A dependency that cannot be verified or resolved safely, a security-critical review finding, a shared-file/API collision that changes behavior, or a required **physical watch/iPhone/Windows test** the executor cannot perform.

When escalating, supply **one concise decision package**: evidence, recommended option, alternatives, implications and the smallest requested action. Never request general context already in GitHub.

## 2. Starting and delivering one Issue

1. Read the Issue, current state and relevant canonical docs using the [AGENTS.md order](../AGENTS.md). Check dependency status, latest `main`, open PRs and touched modules. If an upstream hardware gate is missing, do not fabricate success or build an unsupported dependency.
2. Define **observable output**, in-scope and out-of-scope behavior, affected contracts, acceptance checks and rollback. For a large milestone, split into coherent PR-sized steps **within the same Issue**; do not open redundant planning docs.
3. Create a **unique branch from current `main`**: `feat/...`, `fix/...`, `test/...`, `docs/...`, `ci/...`. One executor owns a branch; nobody writes directly to `main`. Prefer Conventional Commits.
4. Implement the smallest useful slice, add relevant automated tests and handle error/cancel/retry behavior. Keep Desktop and Wear OS builds independent. Never copy third-party code without verifying its license.
5. Run appropriate focused tests and full module/reactor checks when shared contracts change. If a test cannot be run, mark **NOT RUN** and explain; failed CI must be repaired, not ignored. For hardware, record **NOT TESTED** unless actually observed.
6. Open a focused PR linked to the Issue. Examine the actual diff, CI results, dependency conflicts, behavior and docs. Fix defects before deciding whether to merge.
7. Follow the merge rules below. Update the Issue with PR, commit, test evidence, remaining work and next step. Close only when **all** Issue acceptance checks are met; partial experimental integration leaves it open.

**Required PR summary (brief, not a duplicate spec):** Issue/scope, changes and touched modules; commands and CI outcomes; physical-device outcome or NOT TESTED; security/compatibility risks and rollback; canonical docs touched; any unresolved blocker.

## 3. Merge policy: autonomy by default, strict exceptions

### Routine change — executor may merge autonomously

A PR may be merged without an additional owner prompt only if **all** apply:

- It implements an **already approved Issue** without extending its product/security scope, destructive behavior, user-visible compatibility promise or release policy.
- The diff is focused, no unrelated files or active branch ownership conflicts, no unresolved review findings; current `main` has been considered and shared API consumers are tested.
- Applicable automated checks are **actually green** on the PR's current head (or the exact approved equivalent); no unexplained skipped required checks, failing tests, unsupported dependency or unreviewed security change.
- It preserves existing Windows photo transfer and other agreed invariants. Any code dependent on missing physical-device evidence remains explicitly experimental, isolated and not enabled by default; the Issue stays open until actual acceptance.
- The executor's GitHub permissions and repository branch protection allow the merge. If not, report a concrete blocker; never circumvent protections.

Examples: docs fixes, test/CI improvements with proven results, non-breaking internal fixes and isolated experimental groundwork that does not silently enable an unverified end-user feature.

### Hold PR and request a decision

Do **not** self-merge when changes touch a previously undecided material security/privacy design, add a new LAN listener or remote capability, alter pairing/permissions/OTA/install semantics, change transfer data integrity, break shared interfaces, remove data, require licensing/signing decisions, release binaries or claim real-device acceptance without evidence. Prepare a complete PR and ask a **targeted** question rather than pausing routine development. If risk or classification is uncertain, hold the PR for coordinator review.

**Merging code is not the same as accepting a feature or publishing it.** A draft/experimental implementation can be integrated behind an inactive gate only under the safe conditions above. A milestone may remain open after a merged PR. Release or changing a hardware status to VERIFIED always follows [QUALITY.md](QUALITY.md) and [VERIFICATION.md](VERIFICATION.md).

## 4. Parallel development and conflicts

- Before work, compare active Issues and open PRs for overlapping source files, shared APIs and database/build contracts. Independent W0 Bluetooth, W13 Web feasibility and #7 CI may progress in parallel; W3 and W5 need a stabilized W2 BLE contract first.
- One chat owns a shared interface change. Other branches wait for that contract or use mocks; do not race into different incompatible APIs.
- Minimize simultaneous edits to cross-track docs (`AGENTS.md`, `INDEX.md`, `ROADMAP.md`, `STATE.md`, ADR index). Put interim progress in the Issue/PR and rebase dependent branches after the shared merge.
- After any dependency change, update from current `main`, inspect conflicts and rerun affected tests before merge. Failed conflict resolution is a blocker, not a reason to discard work silently.

## 5. Documentation: read little, write once

**Single owner per fact:** use [INDEX.md](INDEX.md). No extra handoff, duplicate roadmap, alternative architecture file or long session summary. Issue = task/scope/status; PR = change/tests/review; canonical docs = lasting facts/decisions; [STATE.md](STATE.md) = brief cross-track snapshot.

**Write in this order when relevant:**
1. Record discoveries and unfinished work **in Issue/PR**, not in global STATE.
2. Record *actual* physical/compatibility evidence with model/OS/build/test conditions in [VERIFICATION.md](VERIFICATION.md); code/CI does not qualify.
3. Update the **one canonical doc** whose contract or product behavior changed: product/scope → [PRODUCT.md](PRODUCT.md); module/API → [ARCHITECTURE.md](ARCHITECTURE.md) or [BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md); browser/HTTP → [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md); build/CI → [DEVELOPMENT.md](DEVELOPMENT.md) or [CI.md](CI.md); quality thresholds → [QUALITY.md](QUALITY.md).
4. A consequential **approved** architectural decision gets a new [ADR](adr/README.md), never an overwritten historical rationale. Shipped public EN/RU docs, releases and compatibility claims change **only when release behavior is verified**.
5. During PR review, validate links with `python ci/check-doc-links.py` when possible or the docs CI; report NOT RUN if unavailable. Update [STATE.md](STATE.md) **only** when a milestone, shipped behavior, fundamental blocker, or next cross-track priority materially changes — preferably by one coordinator/integrator, not every chat.

Do not copy roadmap definitions into STATE. Do not update the ROADMAP for every passing unit test; update it only for approved dependency/milestone changes. Keep documents succinct by editing the canonical fact in place.

## 6. Verification and security

- **Automated:** use JDK 25 and Maven for Desktop; `mvn -B -ntp verify` for changes to shared APIs and as the release gate. See [DEVELOPMENT.md](DEVELOPMENT.md) and [CI.md](CI.md). Future Wear OS Gradle/Kotlin tests are separate from the Maven reactor. Cover parser/transport errors, reconnect, malformed inputs and cancellation when applicable.
- **Real hardware:** reference Galaxy Watch4 Classic + actual iPhone tests are not implied by fake ADB, build output, screenshots of a mock or another watch. Use [VERIFICATION.md](VERIFICATION.md) statuses PASS/FAIL/BLOCKED/NOT TESTED/INCONCLUSIVE, recording exact model, firmware/API, app commit, steps and privacy-safe evidence. Never invent battery measurements.
- **Privacy/safety:** no private photos, notification text, account credentials, pairing codes, BLE IDs, IPs, keys or identifying logs in public artifacts. No unexpected telemetry, unapproved cloud/iOS app, malicious external code, unbounded retries or destructive watch operations.
- **Original photo rule:** accepted JPEG/PNG transfer payloads are not resized, compressed, re-encoded or stripped of metadata; preserve original bytes. For Safari, [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md) owns the iOS representation caveat. A separate quality-audit task is **deferred**, not silently assigned.
- **Release:** separate Desktop and future Bridge version lines/signing. No automatic signed release, no blanket compatibility claim, and no release-gate bypass from a green unit test.

## 7. End-of-task handoff

Report concisely: Issue + PR/merge commit; what changed; test commands and results; physical-device status; remaining risks/decisions; next small task. Put lasting facts in the appropriate canonical source. Never ask the owner to manually transfer summaries between chats.

**Done means the state is reconstructible from current GitHub without the chat history.** If tool access fails, record the exact failing action and offer a patch/PR-ready output; do not pretend remote writes or checks occurred.
