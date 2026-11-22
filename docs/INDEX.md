# WristPorter documentation index

**Read on demand.** [AGENTS.md](../AGENTS.md) defines the Issue-first entry path. [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md) is the **single owner of agent workflow**, including interactive owner-assisted device testing and PR merge rules. This index only identifies where lasting facts belong.

| Source | Owns |
| --- | --- |
| [GitHub Issues](https://github.com/aidarnig/WristPorter/issues) / linked PRs | Individual scope, current progress, acceptance, evidence and blockers |
| [STATE.md](STATE.md) | Compact **cross-track** status and next priorities, not an issue-by-issue history |
| [ROADMAP.md](ROADMAP.md) | Approved milestones, dependencies and gates |
| [PRODUCT.md](PRODUCT.md) | Product mission, shipped/planned behavior and exclusions |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Existing module boundaries and shared contracts |
| [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md) | BLE/iPhone ANCS/AMS feasibility and conditional limits |
| [BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md) | Proposed Wear BLE protocol, state and lifecycle |
| [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md) | Safari → Windows → watch browser upload, API, security and formats |
| [adr/](adr/README.md) | Approved decisions and their historical rationale |
| [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md) | Agent autonomy, branch/PR/merge, **guided hardware tests**, documentation workflow |
| [VERIFICATION.md](VERIFICATION.md) | Actual hardware test IDs, statuses, evidence and measurement records |
| [QUALITY.md](QUALITY.md) | Quality/release gates and historical Desktop acceptance |
| [DEVELOPMENT.md](DEVELOPMENT.md), [CI.md](CI.md) | Build, packaging and automation commands/policy |
| [UX.md](UX.md), [BRANDING.md](BRANDING.md) | Shipped app interface/branding |
| [SECURITY.md](SECURITY.md), [COMPATIBILITY.md](COMPATIBILITY.md) | Desktop signing and publicly verified compatibility |
| [QUICK_START.md](QUICK_START.md), [RELEASE_NOTES.md](RELEASE_NOTES.md), [CHANGELOG.md](../CHANGELOG.md) | User onboarding and shipped release history |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Public contributor entry point |

**No duplicate plans, handoff documents or test claims:** Issue/PR for progress, verification ledger for observed devices, one canonical document for lasting facts. Use status terms from [VERIFICATION.md](VERIFICATION.md) and never call CI-only evidence hardware verification. Synchronize RU/EN public docs for shipped features. [Docs CI](../.github/workflows/docs-check.yml) validates file links, not truth.
