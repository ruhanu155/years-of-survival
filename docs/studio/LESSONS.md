# LESSONS — hard-won, one line each (append-only; read once at lane start)
- GameTests in one batch run in parallel: pin conditions on the block under test, never via EnvService overrides.
- GameTest worlds have structure generation off; placement must be proven in the world smoke, not a GameTest.
- `makeMockServerPlayerInLevel` fails with a curios payload error → use `EcoGameTests.player(h)`.
- GameTest structures are 3x3x3 with barriers beyond: keep shots/blood/paths inside; register test listeners with `EVENT_BUS.register(obj)` + `@SubscribeEvent`, not lambdas.
- contentLint forces unit-suffix keys (`_min`, `_h`, `_days`, `_s`, `_kg`, `_g`); a food row with no `weight_g` defaults to 100 g.
- Save fixtures: new attachments/components/saved data go in the newest `saves/0.x.0`; 0.3.0 is frozen; SaveFixturesTest wants one fixture per saved-data type.
- `tools/newid.sh` for every id; never hand-edit inventory.json; `credits.py write` rewrites credits.json — add entries by hand.
- tools/prepush.sh's ignored-file check trips on uncommitted src: commit first.
- Run client smokes with `setsid`; never `pkill` broadly — other lanes' gradle/Xvfb die.
- After a worldgen change delete `run/world/dimensions/yearofice/slice_test` (gametest.sh does it now).
- A squash whose base predates main deletes files silently and CI stays green: merge origin/main before squashing.
- Lang/credits JSON can carry conflict markers after a union merge: run tools/validate_json.py.
- The same id registered in two registries crashes the client at load (rose_hips, barley_flour, back_twinge happened).
- Five JVMs on the 8 GB box = exit 137. Filter GameTests; build once.
- New species data must pass data/fish_species_data_only (bite_base > 0, item, capacity): run -Pgametest.filter=data/,fish/ after adding fish JSON.
- C1-only (-XX:TieredStopAtLevel=1) slows the compute-heavy worldgen search; GameTest tick timeouts run much faster than wall time, so block on futures instead of polling ticks.
- After a squash-merge, `git log --oneline origin/main..origin/<branch>` must be empty before deleting the branch; a non-empty result means a late push was lost (PR #31 lost 96 files this way).
- Stacked PRs that combine two lanes can double-register an id (rose_hips #49/#50): run `./gradlew test --tests *InventoryTest*` after every stack merge, not just compile.
- A GameTest that sets blocks over an area must force-load the chunks and wait ~40 ticks first.
- Before merging a "stacked" PR check `git merge-base prN pr55`; if it is not truly stacked, apply its diff with `git apply --3way` onto main instead of merging.
2026-10-20 | world | ChunkAccess.getHeight(type,x,z) returns the TOP BLOCK y (first available minus 1), not the first free y: subtracting 1 again put a whole pass one block too low and every write silently did nothing; equal-hash tests passed trivially — always assert something was written.
2026-10-20 | build | tools/gametest.sh runs ALL non-generator GameTests despite -Pgametest.filter (only TestFilter.apply generators filter): ~3 min and 51 tests; full prepush JUnit worker got OOM-killed (exit 137) twice while 2 other agents ran.
- InventoryTest scans source for ResourceLocation.fromNamespaceAndPath(MOD_ID, "literal"): build data paths through a helper taking a variable. contentLint wants unit suffixes (_h, _pct, _c), never _hour/_day/_hours. GameTest template empty3 has a ceiling: clear it before heightmap-based tests. A box under memory pressure kills the test JVM with 137 (not a failure).
- Never seed java.util.Random with consecutive longs for hourly rolls: neighbouring seeds give near-identical first draws (600 Hot hours never rolled a 3% fire). Use SplittableRandom.
- A BlockEntity registry that fills in onLoad() stays empty for BEs created by level.setBlock in a GameTest; register in setLevel(Level) instead.
- tools/gametest.sh runs every non-generator GameTest (53) whatever the filter: the box OOMs (137) most of the time while 2+ lanes build; retry in a sleep loop.
- GameTest "roofed" tests: an overhang roofs the air beside the wall too; keep a test room's roof flush with its walls when the check is "solid within 6 above".
- sim.fire.Seasoning, WoodConfig rates and `/yoi wood dry` already shipped in M2; B4 only needed the forecast subcommand and a calendar test (read main before planning).
- Long runways never share the batch grid: a test whose line is longer than its template (chase_cap 300 m, sight_cone 220 m) runs in its own far region (index x 4096 blocks on +z), force-loaded for its duration; refcounted floors cannot stop a neighbour's structure or entities on the line. (M6-INT)
2026-10-21 | M9 B | Recipe holder ids include the folder: data/recipe/station/x.json is yearofice:station/x, so tests match endsWith("/x"). A registry holds each serializer object once: an id alias needs its own instance over the shared codec. A WorkJob giving items can land them in the held slot after a shrink to 0, so test "used up" by countItem, not getMainHandItem().isEmpty().
2026-10-21 | M9 C | A statistical JUnit band must be sized to its sample: 100 coin flips at 50% fall outside 45-55 about one run in three; use 2,000 draws and 3 sigma (940-1060), not a looser band on 100. (Never widen a spec number; resize the sample.)
2026-10-21 | M9 C | A new entity that borrows another species' art needs only SpeciesArt.ALIAS entries (Map.of takes 10 pairs) plus a renderer registration; animation names follow the alias. A kept animal must override shouldBeSaved() to true (YoiAnimal's default is false) and write its own NBT; give its core a grown default (age past weaning) or every spawned adult reads as a suckling.
2026-10-21 | M9 C | tools/artgen/credits.py write rewrites the whole credits.json (thousands of lines) and `check` crashes on main's asset_id-less fish-model entry; append the new entries by hand and leave the file otherwise untouched. artgen build also rewrites unrelated item models: git checkout them.
2026-10-21 | ecology | Grouped test animals lay no sign (SignLaying.optIn); far-region table lives in EcoGameTests (FAR_*); pin entity streams with getRandom().setSeed in chase tests. A detached test pool (ValleyPool.testPool) keeps pool tests off the world's SavedData.
- M8 proxy: Room heat cache counts server ticks (stale at Sleep rate); stove block is not an air cell so byCellHas drops its W; camera teleports kill the bot (use invulnerable + ground spots); shared box OOMs, gate on free -m.
2026-10-21 | M8 D | main already resolved solid sources (sourceOnWall, INT-2); the real failure was the tick-based room cache serving 0 W after lighting. Test the real block, not stubSource; a cache must also key on its inputs.
2026-10-21 | M8 E | GameTests that place things at absolutePos-hashed decay (Outposts.age) must re-audit after their own edits; a player in a GameTest who hits warmth 0 is teleported to spawn by SOS (check p.blockPosition after the loop).
- Proxy screenshots: wait for chunk meshing before shooting; aim at the subject, never the sky.
- Pack cap ~30 kg silently drops crafted items (stash kit first); crate knife not in #knives (craft flint_knife); 2+ clients plus other lanes => 137, launch only when free>=3200.
