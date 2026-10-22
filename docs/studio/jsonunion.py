#!/usr/bin/env python3
"""Union-resolve JSON conflicts. usage: jsonunion.py <oursRef> <theirsRef> file...
theirs (main) wins duplicate keys; credits unions by asset_id with main's file ownership;
inventory-rules dedupes additions by (type, ids). Writes the files; run docInventory after."""
import json, subprocess, sys
ours_ref, theirs_ref, files = sys.argv[1], sys.argv[2], sys.argv[3:]
def show(ref, p):
    try: return json.loads(subprocess.check_output(['git', 'show', f'{ref}:{p}'], stderr=subprocess.DEVNULL))
    except Exception: return None
def idk(e): return e.get('asset_id') or e.get('id') or json.dumps(e, sort_keys=True)[:40]
def dump(p, o):
    with open(p, 'w') as f: json.dump(o, f, indent=2, ensure_ascii=False); f.write('\n')
for p in files:
    a, b = show(ours_ref, p), show(theirs_ref, p)
    if a is None or b is None:
        src = b if b is not None else a
        if src is None: print('skip', p); continue
        dump(p, src); print('one-side', p); continue
    if p.endswith('credits.json'):
        owner = {f: idk(e) for e in b['entries'] for f in e.get('files', [])}
        byid = {}
        for src in (a, b):
            for e in src['entries']:
                k = idk(e)
                if k in byid: byid[k]['files'] = sorted(set(byid[k].get('files', [])) | set(e.get('files', [])))
                else: byid[k] = dict(e)
        seen = set()
        for e in byid.values():
            if 'files' in e:
                e['files'] = [f for f in e['files'] if owner.get(f, idk(e)) == idk(e) and not (f in seen or seen.add(f))]
        out = dict(b); out['entries'] = [e for e in byid.values() if e.get('files') or 'files' not in e]
    elif p.endswith('inventory-rules.json'):
        out = {**a, **b}
        seen, adds = set(), []
        for x in a.get('additions', []) + b.get('additions', []):
            k = (x.get('type'), tuple(x.get('ids', [])))
            if k not in seen: seen.add(k); adds.append(x)
        out['additions'] = adds
    elif isinstance(a, dict) and isinstance(a.get('values'), dict):
        out = dict(b); out['values'] = {**a['values'], **b['values']}
    elif isinstance(a, dict):
        out = {**a, **b}
        for k in a:
            if isinstance(a[k], dict) and isinstance(b.get(k), dict): out[k] = {**a[k], **b[k]}
            elif isinstance(a[k], list) and isinstance(b.get(k), list):
                seen = set(json.dumps(x, sort_keys=True) for x in b[k])
                out[k] = list(b[k]) + [x for x in a[k] if json.dumps(x, sort_keys=True) not in seen]
    else:
        seen = set(json.dumps(x, sort_keys=True) for x in b)
        out = list(b) + [x for x in a if json.dumps(x, sort_keys=True) not in seen]
    dump(p, out); print('union', p)
