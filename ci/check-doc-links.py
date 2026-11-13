#!/usr/bin/env python3
"""Dependency-free integrity check for canonical Markdown and local repository links."""

from pathlib import Path
from urllib.parse import unquote, urlsplit
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {".git", ".gradle", ".idea", ".venv", "node_modules", "target", "build", "dist"}
REQUIRED = [
    "AGENTS.md",
    "docs/INDEX.md",
    "docs/STATE.md",
    "docs/ROADMAP.md",
    "docs/ROADMAP.ru.md",
    "docs/BRIDGE_CAPABILITIES.md",
    "docs/BRIDGE_ARCHITECTURE.md",
    "docs/ENGINEERING_STANDARD.md",
    "docs/VERIFICATION.md",
    "docs/adr/README.md",
]
# Path-bearing Markdown inline links (images and regular links).
LINK = re.compile(r'!?\[[^\]\n]*\]\(\s*(<[^>]+>|[^)\s]+)(?:\s+"[^"]*")?\s*\)')


def check() -> int:
    problems: list[str] = []
    link_count = 0
    documents = sorted(
        path for path in ROOT.rglob("*.md")
        if not set(path.relative_to(ROOT).parts).intersection(SKIP_DIRS)
    )

    for relative in REQUIRED:
        if not (ROOT / relative).is_file():
            problems.append(f"Required context document missing: {relative}")

    for doc in documents:
        content = doc.read_text(encoding="utf-8")
        relative_doc = doc.relative_to(ROOT)
        for match in LINK.finditer(content):
            target = match.group(1).strip("<>")
            parts = urlsplit(target)
            if parts.scheme or parts.netloc or target.startswith("#"):
                continue
            if not parts.path:
                continue
            resolved = (
                ROOT / unquote(parts.path).lstrip("/")
                if target.startswith("/")
                else doc.parent / unquote(parts.path)
            ).resolve()
            link_count += 1
            try:
                resolved.relative_to(ROOT.resolve())
            except ValueError:
                problems.append(f"{relative_doc}: link leaves repository: {target}")
                continue
            if not resolved.exists():
                problems.append(f"{relative_doc}: missing local target {target}")

    if problems:
        print("Documentation validation FAILED:")
        for problem in problems:
            print(" -", problem)
        return 1
    print(f"Documentation OK: {len(documents)} Markdown files, {link_count} local links, {len(REQUIRED)} required context docs")
    return 0


if __name__ == "__main__":
    sys.exit(check())
