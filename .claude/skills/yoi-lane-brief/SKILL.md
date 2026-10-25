---
name: "yoi-lane-brief"
description: "Launch a Year of Ice builder agent for one milestone lane: the fixed setup, rules, stacked base, memory limits, ship and reply format, so the brief needs only the scope. Use when starting any M4–M9 lane, follow-up lane or debt lane."
---

# Year of Ice lane brief

Use the Agent tool, `subagent_type: general-purpose`, `model: sonnet`, `run_in_background: true`. The prompt is the template below with the placeholders filled. Keep SCOPE to a numbered priority list (skip the tail if it grows); name only the doc sections to grep. Hold at 4 builders on the 8 GB box; a steward or a proxy client counts as one. Launch independent lanes in one message. When usage is tight, say so in the brief: "single pass, no exploration beyond the list".

## Prompt template
```
You are the {ROLE} developer for Year of Ice (NeoForge 1.21.1 Minecraft mod, Alaskan survival). First read docs/studio/AGENT-RULES.md and follow it. Be frugal: read only what the task needs. Others work in parallel; you own {PACKAGES}; keep shared-file edits (YearOfIce.java, YoiConfig, lang, inventory-rules, credits.json) append-only and minimal. Commit early and often; push after every commit. Memory is tight: GameTests one prefix at a time via tools/gametest.sh -Pgametest.filter={prefix}/; exit 137 is an OOM kill, not a failure — wait and retry; one `./gradlew build` at the end; no client runs; never pkill broadly.

TASK: {MILESTONE} lane {LANE} — {TITLE}. Branch dev/{dept}/{slug}, worktree ../wt-{slug}. Base: {main | `git fetch origin pull/N/head:prN` and branch from prN (unmerged stack)}; PR against main.
Read docs/ready-for-development.md › {MILESTONE}; grep docs/blueprint/ for: {DOC SECTIONS}. RECONCILE items {N–M} in docs/studio/RECONCILE.md (absolute path in AGENT-RULES) hold the Director's rulings.
Existing: {WHAT EXISTS}.
Scope (priority order; list what you skip):
{SCOPE}
Every registered id must be obtainable and usable in play (recipe/loot/placement/spawn/trade/taming + a player-facing path) — the source sweeps (StationInputSourceSweepTest, FeatureSourceSweepTest) must pass; no placeholder sources on blocks worldgen never places. Every tuning number is a yearofice-common.toml dial with Gentle/Standard/Brutal; never tune a blueprint number silently — report it.
Tests: {GAMETESTS}; ≤60 s each; own test group; far-region tests force-load their chunks; never weaken assertions; never runServer/EULA. Before the final squash: `git fetch origin && git merge origin/main` must EXIT 0, then `git diff --stat origin/main` must list only your files, then `git reset --soft origin/main && git commit`. tools/prepush.sh once (commit first); PR.
Reply ≤10 lines: PR #, test count, decisions (with blueprint cites), skipped, any test >30 s.
```

## Lessons baked in
- New ids go through `tools/newid.sh`; never hand-edit inventory.json; a new tag/id needs a rule in `studio/inventory-rules.json` then `./gradlew docInventory` (InventoryTest "unplanned id").
- `credits.py write` rewrites credits.json — add entries by hand. Save fixtures go in the newest `saves/0.x.0`, never the frozen 0.3.0.
- GameTests in one batch run in parallel: pin conditions on the block/entity under test, never through a global (ClockService.setDay, WeatherSystem.calmOverride); a shared static test override must be saved and restored, never nulled (offscreen_pool_conserves and glaze_forms both broke that way).
- Statistical tests pin their roll stream (streamOriginOverride) — layout moves seed them. Long runways need their own far region AND forced chunks; entities in a freshly forced chunk are invisible for ~20 ticks; clear the box at test start (a killed run leaves animals).
- A test that passes with a stub but not with the real block (Rooms.stubSource vs a real stove) is not a test: gate on the real thing.
- A stale fetch before `reset --soft` reverts everything main merged since (#107 reverted two PRs, #118 nearly did) — the merge exit code and diff --stat check above are not optional.
- When a lane shares an api with another (events, services), name the hook in both briefs: who posts, who consumes.
- Follow-up lanes (A2, B2…) are normal: the first pass ships the engine, the second the content it skipped. Brief them from the RECONCILE "SKIPPED →" list.
- Lanes end their turn while their own background gradle run is still going; they resume on their own — don't resend. A lane that reports "cut off" needs one resume message naming the remaining steps.

## After launch
Append one line to docs/studio/BOARD.md. The release steward (yoi-release-steward) merges the PR. When the report arrives, log one RECONCILE item: accepted decisions, then `SKIPPED → <lane or milestone>` for every skipped item, and forward any hooks other running lanes must consume with SendMessage.