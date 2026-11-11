# Change-aware CI

This file owns **build triggering and verification costs**. Release gates live in [QUALITY.md](QUALITY.md).

The workflow runs on source, POM, CI script or workflow edits. Documentation-only changes do not launch Maven. `dorny/paths-filter` detects changed modules; root POM or CI edits select all modules.

For ordinary pushes and PRs, Maven first installs changed modules and their required upstream dependencies with tests skipped, then runs tests **only for the changed modules**. Other modules are not tested. Manual `workflow_dispatch` runs full `mvn verify`.

The selection policy has a standalone shell regression test with a fake Maven executable: `ci/test-verify-changed.sh`. It checks one-module, multiple-module, full-reactor and empty-change cases without compiling Java.

## Cost controls

One Linux runner, Maven dependency cache, canceled superseded runs, no Windows matrix or installer creation per commit.

## Limitation

A change to a shared API can break downstream modules that were not selected. Run full reactor checks for breaking APIs and before releases. GitHub may leave required jobs pending when an entire workflow is skipped by path filters: add an always-present status check before enabling mandatory branch protection.
