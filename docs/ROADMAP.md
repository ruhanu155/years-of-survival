# Roadmap

State on 2026-10-21. Gates are proxy runs standing in for Ruhan's human playtest; none has had a human yet.

## Done
| Milestone | Gate | Result |
| --- | --- | --- |
| M0-M2.5 | slice | passed |
| M3 start site, fire, onboarding | earlier proxy | passed |
| M4 body | two-day proxy | passed |
| M5 food | month proxy | passed |
| M7 winter | December week, M4-M7 kit: no collapse, every vital explained, mittens hold at -25 C | PASSED (v7, #111) |
| M8 cabin | stove heats a built cabin, stamp <= 10 %, outpost repair | PASSED (v3, #108): +17...+31 C inside at -38 C |

## Open gates
| Milestone | Gate | State |
| --- | --- | --- |
| M6 hunt | bear reaches an uncleared fish-cleaning spot in 2 days on 7 of 10 seeds; no chase > 150 m | OPEN, 2/10 (v7). Moose-hunt driver not built. |
| M9 homestead | an hour of year 2: save < 5 MB, MSPT in budget, no collapse | Engine passed (0.03 MB save, MSPT p50 2.3 / p95 8 ms). Content failed (stovehouse greens, grants needed). #113 fixed both after the run; not re-run. |

## Open list, in order
1. M6 bear: instrument one failing seed (act, drive, target, nav state under 50 m) before any fix; then 10 seeds once.
2. M6 moose-hunt proxy driver (sign, wind, blood trail, no /yoi).
3. M9 re-run after #113 (stovehouse greens; grants should be zero).
4. M8 crafting gate: bushcraft did not open with a lit fire ring in the driver. Real "first fire" path or gate bug.
5. Design debt: room heat duty-cycle model (replaces the chimney-draw dial), Hot 3x with 50 % chimney loss, log-wall seam metres, outpost registry for repair-in-play, wolf den layer (pup spawns at a random spot), grain_sheaf balance, braided-flat flint rate, frozen-ground clay ban.
6. Untested: fall guard, outpost-visit line, supply-drop livestock, DenPups.
7. Polish: proxy screenshots (camera aim), placeholder textures on the 7 start-kit garments (release_blocker), dead EV_* lang keys, 42 unwired ambition triggers, pink salmon, door/window frames.
8. Mac build sync. Start-site run and this docs batch: done in this PR.

Items 1-7 were handed to lanes K, M, N at 16:02 on the 21st; check main before starting any of them.

## M10 (placeholder)
Not scoped. Candidates: the first human playtest and its fixes; the 51 unwired ambition triggers; original art for livestock and the garments; the permafrost consumers and `ground_frozen`.

## M11 (placeholder)
Not scoped. Candidates: the Interior ring (64 km) beyond the 16 regions; release hardening (licence review of release_blocker assets, 0.x save freeze).
