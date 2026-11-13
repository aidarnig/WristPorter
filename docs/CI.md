# Change-aware CI

This file owns **build triggering and verification costs**. Release gates live in [QUALITY.md](QUALITY.md).

The lightweight Java workflow runs when module sources/POMs, the root POM, the Java selection scripts and their regression tests, or its own workflow change. Documentation-only changes and Windows packaging-only changes do not launch Java CI. The separate Windows packaging workflow runs manually or when its packaging scripts/workflow change. `dorny/paths-filter` detects changed modules; root POM or CI edits select all modules.

For ordinary pushes and PRs, Maven installs the **changed modules, their upstream dependencies (`-am`) and their downstream consumers (`-amd`)**, with test *execution* skipped. This compiles production and test sources of dependent modules, catching source-incompatible shared APIs without running unrelated test suites. A second Maven invocation runs tests **only for changed modules**. This conservative compatibility check runs for every change instead of attempting to detect public APIs from file paths; changes to a widely used base module can compile the entire small reactor. It is **not** equivalent to running downstream tests or full `verify`: behavior-only and runtime regressions still require full verification. Manual `workflow_dispatch` runs full `mvn verify`.

The selection policy has a standalone shell regression test with a fake Maven executable: `bash ci/test-verify-changed.sh`. It checks one-module, multiple-module, downstream-aware build flags, full-reactor and empty-change cases without compiling Java. The Python standard-library test `python3 ci/test-ci-triggers.py` checks path triggers, docs/packaging skips, Maven-module filter coverage and job conditions. Both run in Java CI. When adding a new Maven module, update the selection script and workflow module filters/outputs/env together.

## Cost controls

One Linux runner, Maven dependency cache, canceled superseded runs, no Windows matrix or installer creation per commit. Unchanged downstream tests are **compiled but not executed** by default. When a shared `watch-domain` changes, all its dependents are compiled; leaf-module changes do not add downstream work.

## Limitation

Downstream tests run only when their modules also changed or full reactor verification is requested. Run full reactor checks for breaking behavioral/API changes and before releases. GitHub may leave required jobs pending when an entire workflow is skipped by path filters: add an always-present status check before enabling mandatory branch protection.

## Planned Wear OS CI (not yet configured)

W1 [#14](https://github.com/aidarnig/WristPorter/issues/14) will add **independent path-filtered Android/Kotlin CI** for `wearos/**` sources, unit tests and lint. Full Gradle/watch builds must not become a dependency of unrelated Java or documentation changes. Release-signing secrets stay outside the repository, and device acceptance remains manual in [VERIFICATION.md](VERIFICATION.md).

## Documentation integrity workflow

A lightweight [documentation workflow](../.github/workflows/docs-check.yml) runs the dependency-free [link/context checker](../ci/check-doc-links.py) on documentation PRs and main changes. It validates the required multi-chat context documents and local Markdown links without running Maven or a Windows packager. Developers can run `python ci/check-doc-links.py` before creating a documentation PR. The validator does **not** prove remote web URLs, hardware claims, or Markdown semantic correctness.

## Planned local browser-upload CI

W13/W14 must introduce separate focused tests for embedded HTTP/session/auth/CSRF/origin rules, upload limits and temporary storage/cleanup; use fake ADB and deterministic concurrency tests rather than real mobile hardware in CI. **Actual iPhone Safari and Watch4 Gallery** acceptance is manual and separately logged in [VERIFICATION.md](VERIFICATION.md).

If a new `watch-web` Maven module is added, update `ci/verify-changed.sh` and `.github/workflows/ci.yml` so changes run its own tests and compile its downstream consumers; manually run full `verify` when downstream behavior needs testing. Do not make docs-only commits trigger full Windows packaging. Web Upload server is never started implicitly in test agents or ordinary desktop runs. The separate Wear Kotlin workflow remains independent.
