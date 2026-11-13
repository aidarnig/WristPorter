# Change-aware CI

This file owns **build triggering and verification costs**. Release gates live in [QUALITY.md](QUALITY.md).

The lightweight Java workflow runs when module sources/POMs, the root POM, the Java selection scripts, or its own workflow change. Documentation-only changes and Windows packaging-only changes do not launch Java CI. The separate Windows packaging workflow runs manually or when its packaging scripts/workflow change. `dorny/paths-filter` detects changed modules; root POM or CI edits select all modules.

For ordinary pushes and PRs, Maven first installs changed modules and their required upstream dependencies with tests skipped, then runs tests **only for the changed modules**. Other modules are not tested. Manual `workflow_dispatch` runs full `mvn verify`.

The selection policy has a standalone shell regression test with a fake Maven executable: `ci/test-verify-changed.sh`. It checks one-module, multiple-module, full-reactor and empty-change cases without compiling Java.

## Cost controls

One Linux runner, Maven dependency cache, canceled superseded runs, no Windows matrix or installer creation per commit.

## Limitation

A change to a shared API can break downstream modules that were not selected. Run full reactor checks for breaking APIs and before releases. GitHub may leave required jobs pending when an entire workflow is skipped by path filters: add an always-present status check before enabling mandatory branch protection.

## Planned Wear OS CI (not yet configured)

W1 [#14](https://github.com/aidarnig/WristPorter/issues/14) will add **independent path-filtered Android/Kotlin CI** for `wearos/**` sources, unit tests and lint. Full Gradle/watch builds must not become a dependency of unrelated Java or documentation changes. Release-signing secrets stay outside the repository, and device acceptance remains manual in [VERIFICATION.md](VERIFICATION.md).

## Documentation integrity workflow

A lightweight [documentation workflow](../.github/workflows/docs-check.yml) runs the dependency-free [link/context checker](../ci/check-doc-links.py) on documentation PRs and main changes. It validates the required multi-chat context documents and local Markdown links without running Maven or a Windows packager. Developers can run `python ci/check-doc-links.py` before creating a documentation PR. The validator does **not** prove remote web URLs, hardware claims, or Markdown semantic correctness.
