# WristPorter documentation map

**This index is normative for document ownership.** Start at [AGENTS.md](../AGENTS.md). A new chat must prefer current GitHub files, issues and commits over old chat summaries.

| Canonical source | Owns | Does not own |
| --- | --- | --- |
| [STATE.md](STATE.md) | Current truth, last planning/implementation state, next action, open risks, reusable chat handoff | Detailed technical design, historical completed milestones |
| [ROADMAP.md](ROADMAP.md) | Priorities, milestone dependencies, links to GitHub Issues, exit gates | Live task checklists duplicated from Issues |
| [BRIDGE_CAPABILITIES.md](BRIDGE_CAPABILITIES.md) | Feasible / conditional / prohibited features **without** an iOS app, Apple protocol references | Unverified claims that specific watch firmware works |
| [PRODUCT.md](PRODUCT.md) | Observable user behavior, supported use cases, explicitly excluded behavior | Internal implementation |
| [ARCHITECTURE.md](ARCHITECTURE.md) | High-level module responsibilities and design boundaries | Individual decision rationale |
| [BRIDGE_ARCHITECTURE.md](BRIDGE_ARCHITECTURE.md) | Proposed Wear OS BLE design, transport, lifecycle, permissions, privacy | Signed-off real-device measurements |
| [adr/](adr/README.md) | Immutable decisions and rationale; supersede through new ADRs | Mutable task status |
| [ENGINEERING_STANDARD.md](ENGINEERING_STANDARD.md) | How to implement, test, review, change docs and safely hand off tasks | Product feature list |
| [QUALITY.md](QUALITY.md) | Global test policy, release gates and historical accepted desktop evidence | Results of unexecuted tests |
| [VERIFICATION.md](VERIFICATION.md) | Traceable cross-device test evidence, FAIL/BLOCKED/NOT_TESTED, battery measurement logs | Marketing copy |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Developer commands, CI and packaging workflows | Claim of device acceptance |
| [CI.md](CI.md) | Selective CI strategy and its costs | Hardware QA |
| [UX.md](UX.md) | Current desktop UX; link future watch UX spec as it is created | Protocol design |
| [SECURITY.md](SECURITY.md) | Existing Windows signing/SmartScreen policy | Unreviewed BLE threat model |
| [COMPATIBILITY.md](COMPATIBILITY.md) | Public **desktop photo transfer** verified watches | Watch-to-iPhone bridge support |
| [BRANDING.md](BRANDING.md) | Public identity and visual assets | Development milestones |
| [QUICK_START.md](QUICK_START.md) | User steps for shipped desktop behavior | Unreleased watch app |
| [RELEASE_NOTES.md](RELEASE_NOTES.md) and [CHANGELOG.md](../CHANGELOG.md) | User-focused latest release copy / technical release history | Planned features |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Open-source contributor intro | Full agent instructions |
| [GitHub Issues](https://github.com/aidarnig/WristPorter/issues) | Live work ownership, PR links, blockers, per-task acceptance | Repository-wide decisions |

## Status vocabulary

- **PLANNED** — approved direction, no implementation proof.
- **EXPERIMENTAL** — code or PoC may exist, not demonstrated as complete on reference hardware.
- **IMPLEMENTED / NOT DEVICE-VERIFIED** — code and tests exist; hardware acceptance missing.
- **VERIFIED** — specific user-facing behavior observed on a specified device/firmware and recorded in [VERIFICATION.md](VERIFICATION.md).
- **BLOCKED** — cannot proceed until a named dependency/permission/hardware result.
- **OUT OF SCOPE** — excluded unless changed via ADR and explicit user direction.

Do **not** use “works”, “supported” or “done” for physical behavior from just a green CI build.

## Update rule

Never create alternative “final roadmap”, “v2 plan”, “new context summary” documents. Update the canonical owner and link it from the Issue. Append verified data (with date/device/version) to [VERIFICATION.md](VERIFICATION.md). Keep `STATE.md` compact and refresh it at every merge or meaningful status transition. Public English and Russian docs remain synchronized for shipped features.
