#!/usr/bin/env python3
"""Set VS Code / Jupyter metadata so Kotlin code cells run with the Kotlin kernel."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXAMPLES = ROOT / "examples"

KERNELSPEC = {
    "display_name": "Kotlin",
    "language": "kotlin",
    "name": "kotlin",
}

LANGUAGE_INFO = {
    "name": "kotlin",
    "pygments_lexer": "kotlin",
    "codemirror_mode": "text/x-kotlin",
}


def fix_notebook(path: Path) -> bool:
    nb = json.loads(path.read_text(encoding="utf-8"))
    changed = False

    metadata = nb.setdefault("metadata", {})
    if metadata.get("kernelspec") != KERNELSPEC:
        metadata["kernelspec"] = KERNELSPEC
        changed = True
    if metadata.get("language_info") != LANGUAGE_INFO:
        metadata["language_info"] = LANGUAGE_INFO
        changed = True

    for cell in nb.get("cells", []):
        if cell.get("cell_type") != "code":
            continue
        cell_meta = cell.setdefault("metadata", {})
        vscode_meta = cell_meta.setdefault("vscode", {})
        if vscode_meta.get("languageId") != "kotlin":
            vscode_meta["languageId"] = "kotlin"
            changed = True

    if changed:
        path.write_text(json.dumps(nb, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    return changed


def main() -> int:
    paths = sorted(EXAMPLES.glob("*.ipynb"))
    if not paths:
        print("No notebooks under examples/", file=sys.stderr)
        return 1
    updated = [p.name for p in paths if fix_notebook(p)]
    if updated:
        print("Updated notebook metadata:", ", ".join(updated))
    else:
        print("All example notebooks already have Kotlin metadata.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
