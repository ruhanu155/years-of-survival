#!/usr/bin/env bash
# Run the GameTest server with a memory cap and a 2-slot lock so parallel agents don't OOM the 8 GB box.
# Usage: tools/gametest.sh [extra gradle args]   e.g. tools/gametest.sh -Pgametest.filter=hud/
set -e
cd "$(git rev-parse --show-toplevel)"
LOCKDIR="${TMPDIR:-/tmp}/yoi-gametest-slots"; mkdir -p "$LOCKDIR"
slot=""
for i in 1 2; do
  exec {fd}>"$LOCKDIR/slot$i.lock"
  if flock -n "$fd"; then slot=$i; break; fi
  exec {fd}>&-
done
if [ -z "$slot" ]; then
  echo "gametest: both slots busy, waiting..."; exec {fd}>"$LOCKDIR/slot1.lock"; flock "$fd"; slot=1
fi
echo "gametest: slot $slot"
python3 tools/validate_json.py || exit 1
rm -rf run/world/dimensions/yearofice/slice_test
./gradlew runGameTestServer "$@" 2>&1 | tee build/gametest.log | grep -E "required tests|failed at|Exception in" | tail -5
grep -q "required tests passed" build/gametest.log
