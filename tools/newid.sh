#!/usr/bin/env bash
# Register a new id in one step: inventory rule + regenerate + fixture stub reminder.
# Usage: tools/newid.sh <type> <id> <owner> <milestone> "<basis>"   e.g. tools/newid.sh item bone_grease food m6 "B5 bone grease"
set -e; cd "$(git rev-parse --show-toplevel)"
t=$1; id=$2; owner=$3; ms=$4; basis=${5:-"$t $id"}
python3 - "$t" "$id" "$owner" "$ms" "$basis" <<'PY'
import json,sys
t,id,owner,ms,basis=sys.argv[1:]
p='studio/inventory-rules.json'; d=json.load(open(p))
rules=d['additions']
for r in d['additions']+d['ids']:
    if id in r.get('ids',[]): print('already planned:',id); sys.exit(0)
rules.append({"basis":basis,"milestone":ms,"owner":owner,"type":t,"ids":[id]})
json.dump(d,open(p,'w'),indent=2); print('added rule for',id)
PY
./gradlew docInventory -q
case "$t" in attachment|data_component|saved_data) echo "REMINDER: add a fixture for $id under src/test/resources/saves/0.3.0/ (SaveFixturesTest)";; esac
