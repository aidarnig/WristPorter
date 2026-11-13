# WristPorter documentation ownership

Start with [AGENTS.md](../AGENTS.md). **A single GitHub Issue is sufficient to delegate work.** The executor's reading order, autonomy and PR/merge workflow are defined in AGENTS and [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md); do not duplicate the rules here.

| Canonical source | Owns |
| --- | --- |
| [GitHub Issues](https://github.com/aidarnig/WristPorter/issues) | Live assignments, progress, dependencies, acceptance and linked PRs |
| [STATE.md](STATE.md) | Short cross-track state, significant blockers, next priority (not per-PR history) |
| [ROADMAP.md](ROADMAP.md) | Milestones, approved dependencies and release gates |
| [PRODUCT.md](PRODUCT.md) | Product mission, shipped/planned scope, observable behavior and exclusions |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Existing module responsibilities and shared contracts |
| [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md) | Conditional/direct ANCS/AMS capabilities and unsupported claims |
| [BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md) | Wear OS proposed protocol, state and lifecycle design |
| [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md) | Browser/Safari upload through Windows, session security and formats |
| [adr/](adr/README.md) | Historic architectural decisions and their supersession |
| [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md) | Executor autonomy, branching, CI/review, documentation and handoff procedure |
| [VERIFICATION.md](VERIFICATION.md) | Real-device scenarios, actual PASS/FAIL/BLOCKED/NOT TESTED, measurements |
| [QUALITY.md](QUALITY.md) | Acceptance policy, release gates and historical Desktop hardware evidence |
| [DEVELOPMENT.md](DEVELOPMENT.md), [CI.md](CI.md) | Build/packaging commands and change-aware CI |
| [UX.md](UX.md), [BRANDING.md](BRANDING.md) | Shipped app UX and branding |
| [SECURITY.md](SECURITY.md) | Desktop signing and existing security policy |
| [COMPATIBILITY.md](COMPATIBILITY.md) | Public verified Desktop/watch support, not speculative BLE compatibility |
| [QUICK_START.md](QUICK_START.md), [RELEASE_NOTES.md](RELEASE_NOTES.md), [CHANGELOG.md](../CHANGELOG.md) | Actual shipped behavior and releases |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Public contributor onboarding |

## Status language

- **PLANNED** — accepted direction, no implementation proof.
- **EXPERIMENTAL** — PoC or code exists, but not product-accepted on reference hardware.
- **IMPLEMENTED / NOT DEVICE-VERIFIED** — implemented and code-tested; device acceptance missing.
- **VERIFIED** — exact behavior observed on specified hardware/build and recorded in VERIFICATION.
- **BLOCKED** — named dependency or permission is missing.
- **OUT OF SCOPE** — requires an explicit product decision to change.

**Rule:** write each lasting fact in its owner document, report in-progress work in the Issue/PR, and keep STATE short. Do not invent alternate plan/handoff docs. Public RU/EN shipped information stays synchronized. The [documentation CI](../.github/workflows/docs-check.yml) checks links and required files, **not** correctness or hardware behavior.
