#!/usr/bin/env python3
"""Small, explicit edits to studio/inventory.json between docInventory runs: add an id or set a status, and
recompute the counts block. The file keeps its one-entry-per-line layout.

Usage:
  python3 tools/content-lint/inventory_edit.py add <type> <id> <milestone> <owner> [status]
  python3 tools/content-lint/inventory_edit.py status <type> <id> <status>
"""
import collections
import json
import sys
from pathlib import Path

PATH = Path(__file__).resolve().parents[2] / "studio/inventory.json"


def load():
    return json.loads(PATH.read_text(encoding="utf-8"))


def save(d):
    e = d["entries"]
    c = d["counts"]
    c["total"] = len(e)
    for key, field in (("by_type", "type"), ("by_milestone", "milestone"), ("by_owner", "owner"),
                       ("by_milestone_source", "milestone_source"), ("by_status", "status")):
        counter = collections.Counter((x[field] if x[field] is not None else "none") for x in e)
        old = c.get(key, {})
        ordered = {k: counter[k] for k in old if counter.get(k)}
        for k in sorted(counter):
            if k not in ordered:
                ordered[k] = counter[k]
        c[key] = ordered
    lines = ["{"]
    for k, v in d.items():
        if k in ("counts", "entries"):
            continue
        if isinstance(v, list):
            lines.append(f'  "{k}": [')
            lines.append(",\n".join("    " + json.dumps(x, ensure_ascii=False) for x in v))
            lines.append("  ],")
        else:
            lines.append(f'  "{k}": {json.dumps(v, ensure_ascii=False)},')
    lines.append('  "counts": {')
    lines.append(f'    "total": {c["total"]},')
    parts = [f'    "{k}": {json.dumps(v, ensure_ascii=False)}' for k, v in c.items() if k != "total"]
    lines.append(",\n".join(parts))
    lines.append("  },")
    lines.append('  "entries": [')
    lines.append(",\n".join("    " + json.dumps(x, ensure_ascii=False) for x in e))
    lines.append("  ]")
    lines.append("}")
    text = "\n".join(lines) + "\n"
    PATH.write_text(text, encoding="utf-8")


def main():
    d = load()
    op = sys.argv[1]
    if op == "add":
        t, i, m, o = sys.argv[2:6]
        st = sys.argv[6] if len(sys.argv) > 6 else "planned"
        i = i if ":" in i else "yearofice:" + i
        if any(x["id"] == i and x["type"] == t for x in d["entries"]):
            raise SystemExit(f"{t} {i} already listed")
        entry = {"id": i, "type": t, "milestone": m, "owner": o, "milestone_source": "doc", "status": st}
        same = [n for n, x in enumerate(d["entries"]) if x["type"] == t]
        at = len(d["entries"])
        if same:
            at = same[-1] + 1
            for n in same:
                if d["entries"][n]["id"] > i:
                    at = n
                    break
        d["entries"].insert(at, entry)
    elif op == "status":
        t, i, st = sys.argv[2:5]
        i = i if ":" in i else "yearofice:" + i
        hits = [x for x in d["entries"] if x["id"] == i and x["type"] == t]
        if not hits:
            raise SystemExit(f"{t} {i} not listed")
        for x in hits:
            x["status"] = st
    else:
        raise SystemExit(__doc__)
    save(d)


if __name__ == "__main__":
    main()
