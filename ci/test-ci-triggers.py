#!/usr/bin/env python3
"""Regression checks for selective Java CI plus always-on PR gate and preview.

Uses only the Python standard library. Fail closed when the workflow layout
changes instead of silently accepting an untested trigger policy.
"""

from fnmatch import fnmatchcase
from pathlib import Path
import re
from xml.etree import ElementTree

root = Path(__file__).resolve().parent.parent
workflow = (root / ".github/workflows/ci.yml").read_text(encoding="utf-8")
lines = workflow.splitlines()


def section(source, name, indent):
    header = " " * indent + name + ":"
    assert header in source, f"Missing YAML section {header!r}"
    start = source.index(header) + 1
    end = start
    while end < len(source):
        line = source[end]
        if line.strip() and len(line) - len(line.lstrip()) <= indent:
            break
        end += 1
    return source[start:end]


def patterns(source, indent):
    matches = []
    for line in source:
        if line.strip():
            match = re.fullmatch(r" " * indent + r"- '([^']+)'", line)
            assert match, f"Unexpected path filter: {line!r}"
            matches.append(match.group(1))
    return matches


pom = ElementTree.parse(root / "pom.xml")
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
modules = [item.text for item in pom.findall("./m:modules/m:module", ns)]
assert modules and all(name and name.startswith("watch-") for name in modules)
groups = [module.removeprefix("watch-") for module in modules]

common = {
    "pom.xml", "ci/verify-changed.sh", "ci/test-verify-changed.sh",
    "ci/test-ci-triggers.py", ".github/workflows/ci.yml",
}
expected_paths = common | {"watch-*/pom.xml", "watch-*/src/**"}
on = section(lines, "on", 0)
for event in ("push",):
    event_lines = section(on, event, 2)
    selected = patterns(section(event_lines, "paths", 4), 6)
    assert set(selected) == expected_paths and len(selected) == len(expected_paths), event
    cases = {
        "watch-domain/src/main/java/Example.java": True,
        "watch-core/src/test/java/ExampleTest.java": True,
        "watch-adb/pom.xml": True,
        "watch-desktop/pom.xml": True,
        "watch-web/src/main/java/Example.java": True,
        **{p: True for p in common},
        "docs/CI.md": False,
        "README.md": False,
        "ci/package-windows.ps1": False,
        ".github/workflows/windows-portable.yml": False,
        "docs/assets/logo-light.png": False,
    }
    for path, expected in cases.items():
        assert any(fnmatchcase(path, glob) for glob in selected) is expected, (event, path)
assert "  workflow_dispatch:" in on

assert "          filters: |\n" in workflow
filter_lines = workflow.split("          filters: |\n", 1)[1].split("\n\n  verify:", 1)[0].splitlines()
filter_names = re.findall(r"^            ([a-z][a-z0-9_-]*):$", "\n".join(filter_lines), re.M)
assert filter_names == groups, (filter_names, groups)
for group, module in zip(groups, modules):
    selected = patterns(section(filter_lines, group, 12), 14)
    assert set(selected) == common | {f"{module}/**"}, group

assert "      any: ${{ " + " || ".join(
    f"steps.changed.outputs.{group} == 'true'" for group in groups
) + " }}" in workflow
assert "    if: github.event_name == 'workflow_dispatch' || needs.detect.outputs.any == 'true'" in workflow
for group in groups:
    assert "      " + group + ": ${{ steps.changed.outputs." + group + " }}" in workflow
    assert "          " + group.upper() + "_CHANGED: ${{ needs.detect.outputs." + group + " }}" in workflow
for command in (
    "bash ci/test-verify-changed.sh",
    "python3 ci/test-ci-triggers.py",
    "bash ci/verify-changed.sh",
):
    assert "        run: " + command in workflow, command
assert "          FULL_REACTOR: ${{ github.event_name == 'workflow_dispatch' }}" in workflow

# The main Java workflow no longer runs twice on PRs: the always-present gate
# is the PR execution path, while ci.yml remains main-push/manual full verify.
assert "  pull_request:" not in on
gate = (root / ".github/workflows/pr-gate.yml").read_text(encoding="utf-8")
gate_lines = gate.splitlines()
gate_on = section(gate_lines, "on", 0)
assert "  pull_request:" in gate_on
assert not any("    paths:" in line or "    paths-ignore:" in line for line in gate_on), "Required gate must run on all PRs"
assert "    name: pr-gate" in gate
for required in (
    "python3 ci/test-ci-triggers.py", "bash ci/test-verify-changed.sh",
    "bash ci/verify-changed.sh", "python3 ci/check-doc-links.py",
):
    assert "        run: " + required in gate, required
assert "      - uses: dorny/paths-filter@v4" in gate
gate_filters = gate.split("          filters: |\n", 1)[1].split("\n      - name: Validate CI", 1)[0].splitlines()
gate_groups = re.findall(r"^            ([a-z][a-z0-9_-]*):$", "\n".join(gate_filters), re.M)
assert gate_groups == groups + ["docs", "review_sensitive"], gate_groups
for group, module in zip(groups, modules):
    got = set(patterns(section(gate_filters, group, 12), 14))
    assert got == common | {".github/workflows/pr-gate.yml", f"{module}/**"}, group
assert set(patterns(section(gate_filters, "docs", 12), 14)) == {
    "**/*.md", "ci/check-doc-links.py", ".github/workflows/docs-check.yml",
}
assert set(patterns(section(gate_filters, "review_sensitive", 12), 14)) == {
    "watch-adb/src/**", "watch-web/**", "wearos/**",
    "experiments/w0-ble-probe/**", "ci/package-windows.ps1",
    ".github/workflows/windows-portable.yml", "docs/adr/**",
}
assert "          FULL_REACTOR: 'false'" in gate
docs = (root / ".github/workflows/docs-check.yml").read_text(encoding="utf-8")
docs_on = section(docs.splitlines(), "on", 0)
assert "  push:" in docs_on and "  workflow_dispatch:" in docs_on
assert "  pull_request:" not in docs_on
preview = (root / ".github/workflows/windows-preview.yml").read_text(encoding="utf-8")
preview_on = section(preview.splitlines(), "on", 0)
assert "  pull_request:" in preview_on and "  workflow_dispatch:" in preview_on
assert "actions/upload-artifact@v4" in preview
assert "gh release" not in preview and "publish-release:" not in preview
assert "  contents: read" in preview and "  contents: write" not in preview

# Issue readiness labels are synchronized by a scoped workflow, not a PR script.
issue_status = (root / ".github/workflows/issue-status.yml").read_text(encoding="utf-8")
assert "  issues:" in section(issue_status.splitlines(), "on", 0)
assert "    types: [opened, edited, reopened]" in issue_status
assert "  issues: write" in issue_status
for label in ("status:ready", "status:in-progress", "status:needs-device-test", "status:blocked"):
    assert label in issue_status, label
assert "          REVIEW_SENSITIVE:" in gate
assert "            review_sensitive:" in gate
assert "      - name: Apply exactly one status label" in issue_status

print("Selective CI routing, always-on PR gate and preview checks passed")
