#!/usr/bin/env bash
# Agent pre-push check: ignored sources, inventory, lint, own tests. Usage: tools/prepush.sh [gametest-filter]
set -e
cd "$(git rev-parse --show-toplevel)"
if git status --porcelain --ignored | grep -q ' src/'; then echo "ERROR: ignored files under src/:"; git status --porcelain --ignored | grep ' src/'; exit 1; fi
./gradlew docInventory contentLint -q
./gradlew build -q
echo "prepush OK"
