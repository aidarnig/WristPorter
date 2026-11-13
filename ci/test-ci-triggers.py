#!/usr/bin/env python3
"""Regression checks for the simple, path-filtered Java CI workflow.

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
for event in ("push", "pull_request"):
    event_lines = section(on, event, 2)
    selected = patterns(section(event_lines, "paths", 4), 6)
    assert set(selected) == expected_paths and len(selected) == len(expected_paths), event
    cases = {
        "watch-domain/src/main/java/Example.java": True,
        "watch-core/src/test/java/ExampleTest.java": True,
        "watch-adb/pom.xml": True,
        "watch-desktop/pom.xml": True,
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

print("Selective CI trigger and job-condition checks passed")
