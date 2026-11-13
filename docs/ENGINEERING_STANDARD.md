# Unified development and documentation standard

**Status: binding for future work.** This document defines a predictable workflow for humans and new ChatGPT/Codex chats, and a single-source-of-truth rule. Read [AGENTS.md](../AGENTS.md), [STATE.md](STATE.md), [ROADMAP.md](ROADMAP.md) before starting.

## 1. Contract for a fresh chat / agent session

**Input:** GitHub URL and an Issue number are sufficient. The agent must retrieve current files and live metadata, **not request a replay of earlier chats**.

Before changing anything:
1. Read `AGENTS.md`, `docs/STATE.md`, `docs/ROADMAP.md`, `docs/INDEX.md` and the target Issue.
2. Check latest `main` commit, pending PRs, current branch, relevant code/tests, existing Issues. Resolve any contradiction between snapshot and live repository in favor of live verified data.
3. Read feature feasibility, architecture and ADRs relevant to the issue.
4. State the planned work, exact scope/non-goals, affected modules, dependencies, risks and test approach. Start only if dependencies are satisfied; otherwise document what is blocked.
5. Verify the actual availability of GitHub connector/tools before claiming no access.

At end: write PR/review and update Issue and [STATE.md](STATE.md) with commit, tests run, real hardware results (or explicit **NOT TESTED**), residual risks and next task. Never depend on a private invisible scratchpad.

**Prompt template for a new chat:** “Repository: https://github.com/aidarnig/WristPorter. Work on issue #N. First read AGENTS.md, docs/STATE.md, the roadmap, relevant ADRs and current code; verify the live GitHub state. Implement only that issue, test, create/merge a focused PR as appropriate and update the canonical handoff. Do not rely on previous chat context.”

## 2. Single owner for each fact

Use [INDEX.md](INDEX.md). No redundant `PLAN_FINAL`, duplicate feature lists or alternate handoffs. Stable decisions live in ADRs, live task status in Issues, current overview in STATE, actual evidence in VERIFICATION, acceptance policy in QUALITY. Existing Desktop documentation remains authoritative for released code.

**Status labels in prose:** PLANNED, EXPERIMENTAL, IMPLEMENTED / NOT DEVICE-VERIFIED, VERIFIED, BLOCKED, OUT OF SCOPE. Include date, exact hardware/iOS and specific behavior for a VERIFIED assertion.

## 3. Changes as small, traceable units

- **One Issue → one coherent concern per PR.** A long milestone may have several PRs; each links the same W-series Issue with precise acceptance subset. Avoid simultaneous edits to the same files by separate chats.
- Branch names: `feat/ancs-events`, `fix/gatt-reconnect`, `test/ams-fragments`, `docs/watch-roadmap`; prefer Conventional Commits `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `ci:`.
- Do not push speculative code straight into `main`. Propose focused PR, verify its actual diff, review changed paths and test outcome, then merge after acceptance; do not silently merge unrelated changes.
- Respect contract boundaries: existing Maven Java desktop modules vs new Wear Kotlin Gradle project. Shared protocol/contract changes land before dependent parallel PRs.
- No unreviewed source transplant from WeakBridge/other projects: verify licenses and NOTICE, document derived code, cover imported behavior with tests.

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

**ADR rule:** never silently rewrite an accepted historical decision to hide its rationale. Create a new numbered ADR that supersedes it; update index and canonical architecture links.

## 9. Security and contributor safety

- Public issue/PR and logs must **not** contain phone numbers, BLE IDs/MAC, local IP, pairing PIN, credentials, real notification content, personal photos, signing materials or full device logs.
- No hidden network/telemetry, runtime binary downloads, silent pairing or destructive actions. Windows app code signing and watch APK signing have different risks.
- Actions that affect iPhone notification meaning must use iOS-supplied action labels and explicit current-session validity.
- Review privacy/permission/battery impacts even for apparently cosmetic features.

## 10. Definition of “handoff complete”

A new chat can restore project state **without asking the user to retell anything**: the work Issue links merged PR(s), STATUS/STATE agrees with GitHub, critical decisions are ADRs, evidence is logged and the next step is precisely named. If this cannot be reconstructed, the previous task is not fully documented.
