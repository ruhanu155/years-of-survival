# Docs log, 2026-10-19: research round B (B1–B15, Proposed)

Edits made in the doc under Ruhan's standing rule (research ideas go in as "Proposed (Ruhan may veto)"), then re-exported into `docs/blueprint/` and `docs/ready-for-development.md`. Source: the research desk's IDEAS3.md, specs B1–B15. RECONCILE 175 records the changed numbers, conflicts and inventory fixes.

| Tab | Where | Edit |
| --- | --- | --- |
| Open world in depth | Traversal | Speed and cost table: rows Skates (B1, Proposed) and Skis (B2, Proposed). Insert after "The shoulder seasons": B1 Glare-ice skating, B2 Arvid's skis, B3 Break it today, use it tomorrow |
| Crafting and building in depth | Trees, felling and firewood | Drying paragraph: woodshed ×2 → ×1.3, 10/25 days → about 58 days, October 15 → about July 5 (each "(B4, Proposed; was …)"). Insert after it: B4 Seasoning by the calendar, with 1 GameTest |
| Crafting and building in depth | Building pieces; review audit | Woodshed row "twice as fast" → 1.3 times; audit rows "woodshed −3.0 a summer day" and "dries ×2" (B4, Proposed) |
| Crafting and building in depth | Warm rooms | Insert after the room tests: B8 The cabin tells, B10 Stale air in snow shelters |
| Every concept from the video | Firewood drying | Drying table replaced by the B4 monthly table, a place table and a "Before B4" line; lead, the June example, unsplit rounds, worked year, Ties (rain +5 → +1.5), GameTests `fire/shed_birch_june` and `fire/moisture_drying` re-dated (B4, Proposed) |
| Blueprint (full) | Gameplay ideas; A year of work; Inventory by weight; Fire › Seasoning; Packages row | Text replacements only: woodshed 1.3×, the monthly calendar, about 58 days, July 5 (B4, Proposed) |
| Blueprint (full) | Cooking (Marrow broth, Flatbread rows); Danger in detail (Alert row) | One pointer sentence each to B5, B6 (Items › Food values) and B7 (Water, calls and encounters › Staging) |
| Items | Food values | Insert after "Debug and tests": B5 Bone grease, B6 The sourdough starter |
| Water, calls and encounters | Animal encounters › Staging | Insert after the staging list: B7 Talk as you walk |
| Weather and storms | Blizzards, ground blizzards and whiteout navigation | Insert after "Counterplay": B9 Drifts bury fences |
| Structures and buildables | Warmth, water and hygiene | Insert after "Water-hole box": B11 The hot tent |
| Land animals in depth | Caribou; Caribou herds | Caribou Verbs: the lead-fence snare as the one exception. Insert after "Thousands in Minecraft": B12 The caribou lead fence |
| Body, injury and sickness | Problems by part; Injury sources; Trail Messenger and journal lines | Inserts: B13 Face grease (after "What garments add"), B15 Back strain (end of Injury sources), B14 Beard and hair (after "Journal lines are firsts") |
| Art and audio production | Every sound group and its source | Insert after the RECONCILE 170/171 list: 12 new sound ids for B1, B2, B5, B7, B8, B13, B15 |
| Ready for development | M4, M6, M7, M8, M9 | One "Research round B (Proposed; Ruhan may veto; RECONCILE 175)" bullet each with ids and GameTests |

Repo: `docs/decisions/RECONCILE.md` gains items 173–175 from the scratchpad copy; `studio/inventory-rules.json` gains 30 additions (106 ids: 27 proposed B ids with a `note`, 3 packs, 76 found items) and an `ids` rule moving `measuring_stick` to M5; `studio/inventory.json` regenerated (1,335 entries).
