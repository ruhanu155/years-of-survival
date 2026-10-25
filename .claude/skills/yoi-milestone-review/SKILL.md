---
name: "yoi-milestone-review"
description: "Independent read-only review of a merged Year of Ice milestone against the blueprint before the proxy check: a Sonnet reviewer that did not build it checks ids, obtainability, wiring, tests and a client load. Use after each milestone's PRs merge, before yoi-proxy-check."
---

# Year of Ice milestone review

The build lanes grade their own work; this reviewer has not seen the work being made. Launch with the Agent tool, `subagent_type: general-purpose`, `model: sonnet`, `run_in_background: true`, after the milestone's PRs are on main and before the proxy check. One reviewer per milestone; it edits nothing.

## Prompt template
```
You are the independent reviewer for Year of Ice (NeoForge 1.21.1 mod). Read-only: you may build and run tests, never edit or push. Be frugal; single pass.
Branch to review: {main | prN}. Worktree: ../wt-review (`git worktree add ../wt-review origin/main`).
Check {MILESTONE} against docs/ready-for-development.md › {MILESTONE} (Goal, Ids and data, Tests, Debug commands) and the Director's rulings in docs/studio/RECONCILE.md items {N–M}.
Produce, in this order:
1. IDS: every id/data kind/payload/command the milestone line names — present, missing, renamed (grep src/main/resources/data/yearofice, studio/inventory.json, the /yoi command tree).
2. OBTAINABLE: an id in the registry is not a feature. For every new item/block/entity: the in-play source (recipe with sourced inputs, loot, placement, spawn, trade, taming) and the player-facing path (equip/use/interact) — run `./gradlew test --tests '*SourceSweep*'` and list anything the sweeps do not cover. A source on a block worldgen never places is a false pass.
3. WIRING: for each new engine (sim.* class), the live caller in the tick/event path; list any model that is pure-tested only, and any system whose live path differs from its test path (a test stub where play uses the real block; a GameTest that pins an act the live AI changes).
4. TESTS: the named GameTests/JUnit — present and asserting the blueprint number (not a widened tolerance); shared static overrides restored not nulled; run the milestone's prefix once via tools/gametest.sh and `./gradlew test`.
5. LOAD: `python3 tools/validate_json.py`; a headless client load smoke (`YEAROFICE_SMOKE=world`, Xvfb, setsid, JAVA_TOOL_OPTIONS=-Xmx2200m, ≥3500 MB free) must reach the world without a crash; list duplicate registrations or missing models.
6. RULINGS: any RECONCILE ruling the code contradicts; any blueprint number changed without a RECONCILE item.
Reply ≤15 lines, severity-ordered: BLOCKER (unobtainable feature, missing wiring, crash, contradicted ruling), GAP (missing id/test), NOTE. No praise.
```

## What the Director does with it
- BLOCKERs become the first items of the M-INT lane's scope; GAPs go to the follow-up lane; log the review as one RECONCILE item.
- Only then run yoi-proxy-check. A proxy check on an unreviewed milestone passes on luck (M4's body wore no clothes; M5's bread was 4×; M9's whole homestead needed grants).