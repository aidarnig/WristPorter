# ADR-0003 — GitHub-first context; one owner for every fact

**Status:** Accepted, 2026-11-13.

## Context

Development is split across multiple chats and long-context conversations can degrade. Chat memory and isolated reasoning are unreliable as persistent project management. A project must be reconstructible from GitHub alone.

## Decision

- Root [AGENTS.md](../../AGENTS.md) is the mandatory onboarding contract; [STATE.md](../STATE.md) is the canonical mutable cross-chat handoff.
- [INDEX.md](../INDEX.md) maps distinct source-of-truth documents; roadmap owns order and dependencies, Issues own work status, ADRs own stable decisions, VERIFICATION owns actual hardware evidence, QUALITY owns acceptance policy.
- Each task anchors to a GitHub Issue; branch/PR includes scope, exact tests, status of physical verification and next step.
- Read **live main and open PRs** before starting; do not trust stale context summaries blindly.
- If a new fact changes architecture/scope, create a new ADR rather than silently rewriting historical rationale.
- For unrelated subprojects, parallel chats may work on distinct Issues/branches after shared interface contracts are stable. Merge/retest dependent changes in order.
- No private messages, device addresses, codes, credentials or photos enter GitHub documentation.

## Consequences

A small maintenance cost is intentional: end-of-task STATE and Issue updates are part of Done. Documents must not duplicate each other's role. A new agent should need only repository URL and Issue number, not old chat exports.

[Engineering standard](../ENGINEERING_STANDARD.md) · [Documentation map](../INDEX.md).
