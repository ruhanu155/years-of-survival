#!/usr/bin/env bash
# Agent pre-push check: ignored sources, inventory, lint, own tests. Usage: tools/prepush.sh [gametest-filter]
set -e
cd "$(git rev-parse --show-toplevel)"
if git status --porcelain --ignored | grep -q ' src/'; then echo "ERROR: ignored files under src/:"; git status --porcelain --ignored | grep ' src/'; exit 1; fi
OFF=""; [ -d "$HOME/.gradle/caches/modules-2" ] && OFF="--offline"
./gradlew $OFF docInventory contentLint -q
./gradlew $OFF build -q
echo "prepush OK"
