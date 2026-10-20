#!/usr/bin/env python3
"""json.load every JSON under src/main/resources; fail on conflict markers."""
import json,os,re,sys
bad=0
for root in ("src/main/resources","src/test/resources","studio"):
    for d,_,fs in os.walk(root):
        for f in fs:
            if not f.endswith((".json",".mcmeta",".snbt",".toml",".lang")): continue
            p=os.path.join(d,f); t=open(p,encoding="utf-8",errors="replace").read()
            if re.search(r"^(<<<<<<< |=======$|>>>>>>> )",t,re.M): print("CONFLICT",p); bad+=1; continue
            if f.endswith((".json",".mcmeta")):
                try: json.loads(t)
                except Exception as e: print("BAD",p,e); bad+=1
sys.exit(1 if bad else 0)
