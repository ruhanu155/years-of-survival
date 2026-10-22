# Year of Ice agent rules (read once; ~1 min)
Repo ruhanu155/years-of-survival. Worktree: `cd /home/claude/years-of-survival && git fetch -q origin && git worktree add -b <branch> /home/claude/<wt> origin/main`. Read CLAUDE.md + the milestone section of docs/ready-for-development.md; grep docs/blueprint/ only for the sections your brief names.
Code: pure logic in com.yearofice.sim (no Minecraft imports); server decides, client shows; systems talk via com.yearofice.api; every tuning number in config with Gentle/Standard/Brutal presets; ids yearofice:<snake_case>; /yoi debug commands per system.
Tests: `./gradlew build` must pass. Locally run ONLY your area: `tools/gametest.sh -Pgametest.filter=<prefix>/` (slot-locked; never raw runGameTestServer while others run).
Before push: `git status --porcelain --ignored | grep src/` must be empty; merge origin/main; new ids → studio/inventory-rules.json then `./gradlew docInventory` (never hand-edit inventory.json); keep edits to YearOfIce.java/YoiConfig/lang minimal (others merge in parallel).
Ship: one squashed commit ending "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" + "Claude-Session: https://claude.ai/code/session_01Xr2CVpzoZ636TDaUZHoGmj". PR: `gh api repos/ruhanu155/years-of-survival/pulls -f title=... -f head=<branch> -f base=main -f body=...`, body ending "🤖 Generated with [Claude Code](https://claude.com/claude-code)\n\nhttps://claude.ai/code/session_01Xr2CVpzoZ636TDaUZHoGmj". Don't merge. If CI fails: `gh api repos/ruhanu155/years-of-survival/check-runs/<id>/annotations`. Prefer two small PRs over one big one when a lane splits cleanly.
Finish: append one line to v2/BOARD.md; reply ≤10 lines (PR #, tests, decisions, skipped, any test >30 s).

## Speed rules (added 20:15)
- Ownership: edit only the packages your brief names. Shared files (YearOfIce.java, YoiConfig, lang/en_us.json, inventory-rules.json, credits): APPEND at the end of the relevant block; never reorder or reformat.
- Checkpoint: commit every working step (`git commit -qm "wip: <step>"`); squash before the PR.
- Before push run `tools/prepush.sh` (ignored-file check, inventory, lint, build).
- Only data/JSON changed? `./gradlew contentLint` is enough locally.
- New ids: `tools/newid.sh <type> <id> <owner> <milestone> "<basis>"` (adds the rule and regenerates). Read `src/main/java/com/yearofice/api/README.md` before touching any shared service.

- Director rulings live in /tmp/claude-0/-home-claude/3dd9ded9-61c9-50f0-89ad-cfeae0edf5c6/scratchpad/v2/RECONCILE.md (not in the repo); read only the item numbers your brief names.
- Commit early and often; the box restarts sometimes.
- RECONCILE numbering: the steward logs merges as "MERGED PR #N" lines; the Director renumbers sequentially. Do not reference item numbers above 199 as stable until the Director confirms them.
- Notice board: read v2/HOOKS.md and v2/LESSONS.md at lane start (same folder as this file). Append to HOOKS.md the moment you add or need a cross-lane api; append to LESSONS.md any hard-won lesson instead of putting it in your report. Do not message other agents directly — the board is cheaper and the Director routes NEEDS.

## Ponytail rules (from DietrichGebert/ponytail, MIT) — write less code
Climb this ladder in order and stop at the first rung that works: (1) does it need to exist at all? (2) does the codebase already have a helper? (3) does the JDK/standard library cover it? (4) does Minecraft/NeoForge already do it natively? (5) does an installed dependency (GeckoLib, Curios) solve it? (6) can it be one line? (7) only then, the minimum code that works.
- Fix bugs at the root in the shared function, not in one caller.
- A simplification with a known ceiling gets a `// ponytail: <limit> — upgrade: <path>` comment.
- Never simplify away validation, security, save-data safety, or anything the brief explicitly asks for.
- Non-trivial logic gets one small runnable check (a JUnit or a GameTest assert), not a prose promise.
- Understand the problem fully first; laziness shortens the solution, not the reading.
- Reports: code first, then at most three short lines on what was skipped and when to add it.
- gradle/gametest exit 137 is a cgroup OOM kill, not a test failure: wait for other lanes, retry once, and run GameTests as prefix groups (tools/gametest.sh -Pgametest.filter=<prefix>/) rather than the full suite while others build. Shared test strips/predators leak between parallel GameTests: give entities their own test group and never clear blocks outside your own template.
- Statistical GameTests (counts over many simulated hours) must pin their roll stream to a fixed origin (ShelterEntry.streamOriginOverride pattern) and calm weather (WeatherSystem.calmOverride): the batch layout moves absolutePos, so a position-seeded stream drifts between runs.
- Running GameTests with a prefix filter still runs every non-generator test in a different batch layout (LESSONS); worldgen_no_spill/start_station_nine/slice_valley can fail there while main's full run is green. Treat those three as layout-sensitive: confirm on CI (full suite) before chasing.
- After a stacked base PR is squash-merged, rebuild your branch with `git reset --soft origin/main` + commit (or cherry-pick your own commits onto origin/main); merging main into a pr-based branch add/add-conflicts everywhere.
- Before squashing onto origin/main: `git merge origin/main` into your branch FIRST (resolve), then `git reset --soft origin/main && git commit`. A squash from a tree that predates main deletes main's new files (happened on #74 and #80).
- Review rule: for every milestone noun check three things — the id is registered, a player can OBTAIN it without /yoi (recipe/loot/placement), and a player-facing api/interaction exists. Registry-only = GAP.
- Far-region GameTests MUST force-load their own chunks (EcoGameTests.force) before laying floors/spawning and release at the end; unloaded far chunks only tick when a neighbour holds them (arrow_drop, bear_contact_sequence).
- Test overrides that are shared statics (ValleyPool.testPool, windOverride, calmOverride, streamOriginOverride) must be saved and RESTORED at test end, never nulled — nulling clobbers a parallel test's override (offscreen_pool_conserves, #91).
- chase_cap_150m can also fail when the wolf catches prey before 150 m (kill at 129 m on main) — the test should make the prey uncatchable (speed pin) or accept kill-before-cap as a pass only if the cap logic is separately asserted.
- Proxy drivers: open the craft gates the way a player does (lit fire → bushcraft; furnace → iron) and never sneak when using blocks; read /yoi craft why on any refusal.
- WRAP-UP box rule: with no builder lane compiling/testing, TWO clients may run at once: launch with JAVA_TOOL_OPTIONS="-Xmx2200m" (client heap cap), require free ≥2400 MB and ≤1 other runClient before launch; never pkill broadly.
- Before the final squash: `git fetch origin` THEN `git merge origin/main` THEN `git reset --soft origin/main && git commit`. A stale fetch makes the squash revert everything main merged since (#107 reverted #105/#106). Director's deletion guard caught it; also diff --stat against main for unexpected files.
- Far-chunk GameTests: wait ~20 ticks after force() before getEntitiesOfClass; clear the box at test start (a killed run leaves animals); a region must list the outpost id for Regions.allowsOutpost.
- Before a squash: the merge of origin/main must have EXITED 0 (a dirty-tree merge failure is silent later); then `git diff --stat origin/main` must list only your files.
