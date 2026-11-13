# Unified development and documentation standard

**Status: binding for future work.** This document defines a predictable workflow for humans and new ChatGPT/Codex chats, and a single-source-of-truth rule. Read [AGENTS.md](../AGENTS.md), [STATE.md](STATE.md), [ROADMAP.md](ROADMAP.md) before starting.

## 1. Contract for an issue-only executor session

**Minimum input:** a repository Issue URL or `WristPorter Issue #N` in a WristPorter-connected chat. The executor must recover scope from GitHub, not from previous conversation history. Product coordination and merge approval belong to the project owner/coordinating chat; the executor owns only its assigned Issue and branch.

**Read order (do not skip or reverse):**
1. `AGENTS.md` (this repository's binding agent contract).
2. `docs/STATE.md` (provisional project snapshot).
3. The **live assigned GitHub Issue**, including acceptance, exclusions, blockers and PR links.
4. `docs/ROADMAP.md` (upstream prerequisites and milestone gates).
5. `docs/INDEX.md` and this `docs/ENGINEERING_STANDARD.md` (canonical owner and workflow).
6. **Only the relevant** product/architecture/feasibility/security/ADR/verification files linked by the Issue or index, then applicable implementation and tests.
7. Current `main` commit, open PRs/branches, dependency changes and existing CI status. Reconcile stale documentation against verified GitHub state before work; do not invent access limitations without checking available tools.

**Before changes:** check that upstream gates are satisfied; write a short execution plan in the Issue/PR containing specific goal, non-goals, affected paths, contracts, test approach, risks and hardware dependencies. Work only on the Issue's accepted scope; escalate a required product/security/architecture decision rather than silently broadening it.

**After changes:** open a focused PR (do not auto-merge), provide exact tests and their results, sanitized evidence and honest physical-device status, list docs/ADR changes, rollback and remaining risks. Add a concise Issue status update with PR link and any blocked user-run hardware instructions. Owner approval is mandatory for merge, release, new externally accessible network behavior, irreversible actions and material design changes.

**Issue readiness requirement:** before delegating an Issue, the coordinator ensures it has purpose, deliverables/acceptance, scope and exclusions, dependencies, paths/contracts likely affected, tests including hardware if relevant, docs owner, risks/rollback and any decision gate. A milestone Issue may be split into small PR-sized subsets; add the subset to the Issue before starting it, rather than creating an undocumented cross-chat side quest.

**Short invocation example:** `WristPorter Issue #27`. All other operating instructions come from `AGENTS.md` and this standard.

## 2. Single owner for each fact

Use [INDEX.md](INDEX.md). No redundant `PLAN_FINAL`, duplicate feature lists or alternate handoffs. Stable decisions live in ADRs, live task status in Issues, current overview in STATE, actual evidence in VERIFICATION, acceptance policy in QUALITY. Existing Desktop documentation remains authoritative for released code.

**Status labels in prose:** PLANNED, EXPERIMENTAL, IMPLEMENTED / NOT DEVICE-VERIFIED, VERIFIED, BLOCKED, OUT OF SCOPE. Include date, exact hardware/iOS and specific behavior for a VERIFIED assertion.

## 3. Changes as small, traceable units

- **One Issue → one coherent concern per PR.** A long milestone may have several PRs; each links the same W-series Issue with precise acceptance subset. Avoid simultaneous edits to the same files by separate chats.
- Branch names: `feat/ancs-events`, `fix/gatt-reconnect`, `test/ams-fragments`, `docs/watch-roadmap`; prefer Conventional Commits `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `ci:`.
- Do not push speculative code straight into `main`. Propose a focused PR and verify its diff and test outcome. **The executor must leave it unmerged until the project owner explicitly approves it**; no silent release or unrelated change.
- Respect contract boundaries: existing Maven Java desktop modules vs new Wear Kotlin Gradle project. Shared protocol/contract changes land before dependent parallel PRs.
- No unreviewed source transplant from WeakBridge/other projects: verify licenses and NOTICE, document derived code, cover imported behavior with tests.

### Parallel chat / branch coordination

- **Coordinator/project owner:** decides the active Issue queue, cross-issue priorities, shared API/ownership contracts, security/scope exceptions and merge order. An executor cannot redefine another chat's issue or claim another branch.
- **Executor:** claims one Issue or a precisely documented subtask, creates a uniquely named branch from fresh `main`, uses the Issue for progress/blockers and provides one or more small reviewable PRs. Distinct chats never commit to the same branch.
- **Before parallel starts:** identify touched modules and shared contracts. Independent lanes (for example W0 Bluetooth probe, W13 browser feasibility and #7 CI) may proceed together. If they require the same file/interface, sequence the contract PR first or have the coordinator assign one owner; do not race incompatible edits.
- **Shared source and docs:** avoid concurrent edits to `AGENTS.md`, `docs/INDEX.md`, `docs/ROADMAP.md`, `docs/STATE.md` and ADR indexes unless ownership is assigned. Executors keep interim status in Issues/PRs; the integration step updates shared STATE after an approved merge.
- **Integration order:** after a shared dependency is merged, update the dependent branch from current `main`, resolve conflicts deliberately and rerun impacted CI/tests. Never interpret stale green CI or unrelated prior hardware evidence as current verification.
- **Blocked work:** specify which Issue, hardware observation, interface or owner decision is missing, what was attempted and the next safe action. Do not implement speculative substitutes or modify a different feature to appear unblocked.

## 4. Specification-first change checklist

Every implementation Issue or PR should specify:

| Field | Required description |
| --- | --- |
| Goal / user story | One observable user result |
| Scope | Included + explicitly excluded behavior |
| Dependencies | Issues / interface version / hardware / OS assumptions |
| Contract | Types, states, events, permissions and failure semantics |
| Acceptance | Reproducible tests and hardware scenarios |
| Risks | Security, privacy, battery, compatibility, data loss |
| Docs | Canonical owner files to update |
| Rollback | How to disable/revert without breaking Desktop or losing bonds |
| Proof | Automated runs vs manual watch+iPhone evidence (separate) |

**No new feature can claim supported** because its unit tests or CI are green; physical watch/iPhone results must be written to [VERIFICATION.md](VERIFICATION.md).

## 5. Architecture and coding expectations

### Java/Desktop (existing)

- Retain JDK 25, Maven, Spring non-web bootstrapping and JavaFX; avoid introducing Android dependencies here.
- Run `mvn -B -ntp verify` for full cross-module/API changes, selective tests when safe.
- Reuse ADB selected-device safeguards, process argument arrays and pairing-code stdin; never introduce destructive remote shell operations.
- Guard UI thread and cancellation; keep retry idempotency and scan/copy result distinction.

### Kotlin/Wear OS (planned)

- Tool versions pinned in Gradle wrapper/config; minimum Android API from W0 device evidence, not unrelated repo defaults.
- Pure Kotlin protocol parsers/reducers, bounded buffers and fake transports where practical; Android service integration behind interfaces.
- Structured concurrency, single owner of BLE/GATT operations, explicit cancellation, generation-correlated callbacks, no stale actions after reconnect.
- Use standard Android permissions, foreground-service lifecycle rules and encrypted BLE bonding; no Internet permission in initial program.
- Explicit state for available/unavailable/permission denied/connecting/connected **per ANCS and AMS**, not a false global success.

### Cross-cutting

- Accessibility: text status, large round touch targets, TalkBack where available, RU/EN parity, notification privacy.
- Energy: no unbounded scans, wakelocks, polling or retry storms; account for background behavior at every layer.
- Test malformed BLE packet handling, notification UID reuse, reconnect, canceled commands, playback state changes and service disappearance.
- Keep dependency/license compliance, sensitive logs and release provenance reviewable.

## 6. Testing and CI discipline

**For each PR:** list exact commands/tests, outcomes and environment; do not say “all tests pass” without actually running them. Missing physical access = NOT TESTED, never implied verified.

- Protocol unit tests: ANCS fragment combinations, malformed/unbounded values, UID lifecycle and actions; AMS metadata and supported-command changes.
- Transport tests: fake clock, timeouts, retries, generation IDs, GATT operation serial queue and cancellation.
- Android integration/device tests: pairing, permission prompts, notification rendering and background state; actual Watch4 + iPhone when available.
- Privacy tests: redaction, RAM-only content, no exported private components, no Internet permission.
- Power tests: standard scenarios and baseline in VERIFICATION; never guess battery claims.
- Desktop regression tests: Maven and actual photo transfer whenever desktop APK-management or ADB internals change.
- CI: **separate Android/Wear workflow**, path-filtered for Gradle source; never make docs-only edits trigger full Windows packaging. Full verification before signed watch release.

## 7. Release and compatibility policy

- Desktop `v1.1.2` and future Wear APK have **independent versions** and their own release assets/checksums.
- First Wear build is **experimental** until W0 physical proof; beta after [W12](ROADMAP.md) gates, real-device coverage and explicit caveats.
- Exact watch/iOS/version combination is required for VERIFIED; never infer from other models' successes.
- Release documentation includes supported feature matrix, real-device test outcomes, known failure modes, installation and rollback, permissions, battery caveats and signing/key continuity.
- Never overwrite signed ZIP/APK or historical releases without explicit verified version policy. Release keys remain outside GitHub source.

## 8. Docs + ADR update triggers

| Change | Update |
| --- | --- |
| Scope/feature support/non-goal | [PRODUCT.md](PRODUCT.md), [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md); ADR if policy |
| Module/API/permission/lifecycle design | [ARCHITECTURE.md](ARCHITECTURE.md), [BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md), new ADR if consequential |
| Task completion/blocker | GitHub Issue + [STATE.md](STATE.md); [ROADMAP.md](ROADMAP.md) only for milestone-status transitions |
| Device tests/battery/QA | [VERIFICATION.md](VERIFICATION.md); [QUALITY.md](QUALITY.md) only if acceptance policy changes |
| Build/CI/install change | [DEVELOPMENT.md](DEVELOPMENT.md), [CI.md](CI.md), or scripts/workflows |
| Shipped features | README/README.ru, user quick starts, CHANGELOG/release notes, public compatibility after verified test |

### Required documentation writing and handoff order

1. **While starting:** check the Issue and `INDEX.md` to identify the one canonical owner for each fact; do not make a separate plan/context document.
2. **While implementing/researching:** put progress, experimental findings, code/test links and blockers into the **Issue or PR**. Document a proposed API/security decision there; seek product-owner approval for a changed contract before treating it as settled.
3. **After evidence exists:** record reproducible test commands and outcomes in the PR. Record actual device observations in `VERIFICATION.md` using its template; mark unperformed scenarios **NOT TESTED**, never PASS. Do not invent measured hashes, devices or results.
4. **For a changed contract:** update the canonical product/architecture/security/development document listed above; add a new numbered ADR only for a consequential approved decision, and update the ADR index. Keep released-feature EN/RU public documentation synchronized **only when shipped behavior changes**.
5. **At PR review:** include a compact docs-change checklist: documents changed, deliberately unchanged owners, Issue/ADR links, evidence status, rollback and test commands. Run `python ci/check-doc-links.py` when accessible; otherwise label **NOT RUN** and rely on PR docs CI once reported.
6. **At approved merge:** coordinator/integrator reconciles the Issue, affected verification entries and `STATE.md` in the approved PR or a linked follow-up; note the merged SHA, actual CI/device results, risks and next Issue. Do not independently overwrite another chat's handoff.

Documenting a proposal does not mean the feature has shipped. `ROADMAP.md` changes only for approved milestone or dependency changes; use the Issue for routine task status. Neither a successful PR nor a documentation check implies physical-device acceptance.
**ADR rule:** never silently rewrite an accepted historical decision to hide its rationale. Create a new numbered ADR that supersedes it; update index and canonical architecture links.

## 9. Security and contributor safety

- Public issue/PR and logs must **not** contain phone numbers, BLE IDs/MAC, local IP, pairing PIN, credentials, real notification content, personal photos, signing materials or full device logs.
- No hidden network/telemetry, runtime binary downloads, silent pairing or destructive actions. Windows app code signing and watch APK signing have different risks.
- Actions that affect iPhone notification meaning must use iOS-supplied action labels and explicit current-session validity.
- Review privacy/permission/battery impacts even for apparently cosmetic features.

## 10. Definition of “handoff complete”

A new chat can restore project state **without asking the user to retell anything**: the work Issue links merged PR(s), STATUS/STATE agrees with GitHub, critical decisions are ADRs, evidence is logged and the next step is precisely named. If this cannot be reconstructed, the previous task is not fully documented.

## Automated context integrity check

Run `python ci/check-doc-links.py` after any documentation reorganization and before submitting a documentation PR. The dedicated [documentation integrity workflow](../.github/workflows/docs-check.yml) checks required source-of-truth files and relative Markdown links on PRs/main. This is separate from the future Android CI and current Java/Windows packaging workflows; green link checks do not imply protocol or hardware validation.

## New ecosystem workstream: optional Windows Web Upload

Web Upload is **PLANNED**, not part of Desktop v1.1.2. [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md) fixes **one Windows app** with on-demand private-LAN web frontend; [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md) owns web API, target binding, staging and threat model. Use W13–W16 [Issues #27–#30](ROADMAP.md).

- Do not create a second distribution, native iOS companion, cloud app or always-on Java/Spring web server as a shortcut.
- Both phone browser and desktop local photos must reuse the existing safe ADB transfer engine; extract testable selected-watch and job queue ownership before allowing concurrent inputs.
- Develop HTTP code behind isolated interfaces, with explicit opt-in network listener, rate-limited short-lived authorization, CSRF/Host validation, input quotas, cleanup, and no remote ADB/admin endpoints.
- Treat HTTP on LAN as **unencrypted** unless authenticated TLS actually exists; no "secure" marketing claim from PIN-only pairing. Escalate transport security acceptance in W13/W16.
- New tests cover iPhone Safari picker behavior, HEIC failures, upload body/mime spoofing, pixel bombs, process crash, Windows firewall, wrong watch and previous desktop-gallery regression.
- Preserve two independent release gates: Bluetooth Bridge beta (W12) and Web Upload desktop security/QA (W16). Do not mark one implemented just because the other passes.

New-chat prompt remains exactly as before: open `AGENTS.md`, `STATE.md`, the task Issue and owners. Web Upload tasks should additionally read [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md) and ADR-0004.
