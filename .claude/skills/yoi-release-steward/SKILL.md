---
name: "yoi-release-steward"
description: "Merge, conflict-resolve and CI-fix pull requests for the Year of Ice mod repo (ruhanu155/years-of-survival) in stack order with deletion, revert and JSON guards; use for any PR merge, red CI, or JSON/inventory conflict in that repo."
---

# Year of Ice release steward

Repo `ruhanu155/years-of-survival`; main clone `the repo root`. Be frugal: short commands, no exploring. GraphQL is blocked; use `gh api` REST. The Director usually merges directly (Ruhan approved); the steward lane is for stacks and conflict rounds.

## Order
PRs are usually a stack (each later PR contains the earlier ones). Merge strictly oldest-first, one at a time, waiting for green on a head that includes origin/main. Retarget a stacked PR's base to main with `gh api -X PATCH repos/ruhanu155/years-of-survival/pulls/N -f base=main`.

## Per open PR
1. CI: `sha=$(gh api repos/ruhanu155/years-of-survival/pulls/N --jq .head.sha); gh api repos/ruhanu155/years-of-survival/commits/$sha/check-runs --jq '.check_runs[]|"\(.name) \(.status) \(.conclusion)"'`. Ignore `skipped`. A docs-only PR gets no smoke run (changed-paths filter) — merge on the deletion guard alone; a PR touching any `src/` file waits for smoke.
2. Guards before EVERY merge, in this order:
   - `git fetch origin; git diff --diff-filter=D --name-only origin/main...$sha` — only files the PR means to delete (superseded screenshots are fine; a `src/` file is not).
   - `git diff --stat origin/main...$sha | tail -1` and eyeball the file list: a lane that squashed over a stale fetch REVERTS main's recent merges and CI stays green (#107 reverted #105/#106; #118 nearly did). Files outside the lane's department = rebuild the branch: `git diff <lane base> <pre-squash tip> -- src/ | git apply --3way` onto origin/main (the pre-squash tip is in the worktree's reflog), compile, run the touched JUnit, force-with-lease the LANE branch (never main).
   - `python3 tools/validate_json.py` if present, else json.load every file under src/main/resources.
3. All green and mergeable → `gh api -X PUT repos/ruhanu155/years-of-survival/pulls/N/merge -f merge_method=squash -f commit_title="<title> (#N)"`; after a squash lands, the next stacked PR must `git merge origin/main` BEFORE its own `reset --soft` squash. Append `<n>. MERGED PR #N <title>.` to `docs/studio/RECONCILE.md`.
4. `dirty` → in the PR's worktree run `git merge origin/main`, resolve with the recipes, `./gradlew compileJava docInventory contentLint test --tests '*InventoryTest*' --tests '*SaveFixtures*' --tests '*Sweep*'`, push the merge commit (never force on main).
5. Red → `gh api repos/ruhanu155/years-of-survival/check-runs/<id>/annotations --jq '.[]|.message'`. Fix only mechanical causes; otherwise report to the lane that owns the code. Compare with main's latest run (`actions/runs?branch=main&per_page=3`): a test red on the PR but green on main after the same code is the layout/isolation family (worldgen_no_spill, start_station_nine, slice_valley, chase_cap_150m, outpost_repairable) — still root-cause it, never re-run to green. "recent account payments have failed" is GitHub billing: report and stop.

## Conflict recipes
- Any JSON (lang, credits, data maps, inventory-rules): `python3 docs/studio/jsonunion.py HEAD origin/main <files>` (theirs = main wins duplicates; credits union by asset_id; inventory-rules dedupe additions). `studio/inventory.json`: always `git checkout origin/main -- studio/inventory.json && ./gradlew docInventory` (never hand-edit).
- `YearOfIce.java`, `YoiConfig.java`, `BodySystem`, `BodyTicker`: both sides add lines → keep both, in order. The same id registered twice (client crashes at load) → keep one.
- `src/test/resources/saves/0.x.0/stacks.snbt`: keep both fixture stacks.
- Overlapping logic in Java: stop and report; do not guess.

## Mechanical CI fixes
- `InventoryTest ... unplanned id X` → add a rule for X in `studio/inventory-rules.json` `additions` (basis, milestone, owner, type, ids), run `./gradlew docInventory`.
- `SaveFixturesTest` missing fixture → add to the newest `src/test/resources/saves/0.x.0/` (0.3.0 is frozen).
- Ignored source: `git status --porcelain --ignored | grep ' src/'` must be empty. Stray `crash-reports/*.txt` in a PR → remove.
- Job died in <30 s with no annotations → runner loss; re-run once: `gh api -X POST repos/ruhanu155/years-of-survival/actions/runs/<id>/rerun` (403 while active: wait).
- CI log download is blocked by the proxy; annotations are the only readable output.

## Rules
- Never force-push main, rewrite main, run `runServer`, or accept the Minecraft EULA. `--force-with-lease` on a lane branch is allowed only for the stale-squash rebuild above.
- Run GameTests only through `tools/gametest.sh` (the box has 8 GB). Exit 137 = OOM, not a failure: wait and retry once; CI green on the merged head is sufficient.
- Commit trailers: your tool's attribution lines (Co-Authored-By and any session link).
- Report in ≤8 lines: merged, blocked and why.