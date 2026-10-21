# Playtest notes

Every proxy check so far was a scripted client standing in for a human. Logs and screenshots are in docs/proxy/m6..m9. This file lists what each run ruled, with the numbers, and what the first human should look at.

## Proxy rulings
**M6 bear (open).** Gate: a bear reaches an uncleared fish-cleaning spot within 2 days on 7 of 10 seeds; no chase over 150 m.
- First run 0/5: no bear within 500 m of a 2-day gut pile (grizzly density 0.08 per km2, animals only exist inside the 150 m spawn radius). Fixed by the off-screen valley pool (#88): 9 of 10 seeds in the GameTest.
- v3 0/7, closest 41-144 m (scent walk died in BED/WATER acts, #96). v4 0/10, closest 19-120 m (the player stood at the pile, so the stops were the bear contact sequence). v5 0/10 with player 200 m away (body demoted past 180 m, #106). v6 3/10 at 90 m (five stopped 15-24 m short, #110). v7 2/10, eight stopped at 12-41 m (one at 88). Cause not found; next step is a per-tick trace on seed 3141592.
- Driver ruling: pile 60-120 m from the player; beyond about 165 m read ValleyPool.REACHED.
- Chase cap: one failure at 129 m was the wolf killing the caribou before the cap, a test design issue.

**M7 winter (passed, v7).** December week, M4-M7 kit.
- Run 1: collapsed night 1-2 at -14...-26 C, clothing never worn. After #94: 7 days, hands 0 frost points through -27/-29.5 C, warmth min 100, kcal in 1680 / out about 2020 (-345 a day), BMI 23.0 to 22.7.
- Frost lines: 1 of 15 crossings silent (head stage 3 at -24 C, day 6). Fixed per zone (#98). v3: 1 of 12.
- Energy cap bug: morning cap 10, 7, 5, 3, 3, 3 on -340 kcal a day with 8 h sleep. Fixed twice (#101, #105). Lean-meat diet gave 10, 10, 6, 6, 6, 7 (blueprint's lean row, intended). v5 fat-rich: 10 through day 6, 7 on day 7. v7: 10 every morning, awakeMin 0 from day 3, 8 of 8 crossings covered, 0 frost at -29.5 C, BMI 22.75, net -300 kcal a day.
- Birch night fire: 4.2 armloads, about 3 h to 0.7, 3-4 wakes a night. Spruce lasts about 2 h a load.
- Still-air numbers at -25 C: naked -25, start kit -10, full kit +16 (proxy with wind and work: -20.5 vs +3.6).

**M8 cabin (passed, v3).** Inside feels-like +17.2...+31.0 C at -38.2 C outside, stove 8091 W, warmth min 58, no collapse. Stamp 5.3 % (limit 10), repair 146/146, 13.9 armloads for a night-week.
- Bugs found on the way: stove posted 0 W to its room (min -26.1 C inside at -30.4 outside); body ignored rooms (+86 C reading, warmth 52 to 0); room heat unbounded. GameTest real stove: STEADY 4079 W gives +26 C at -30 C, LOW 1632 W gives -4.2 C, U 81 W/C. Chimney-draw dial then brought the 30 m3 / 8 kW cabin from +86 to about +32 C.
- Bushcraft gate stayed shut with a lit fire ring in the driver (open).

**M9 homestead (engine passed, content failed).** Save 0.03 MB mod data (world 18.6 MB). MSPT p50 2.3 / p95 8.0 ms, max 2.5 s once at outpost chunk generation. No collapse. Wolf follows, livestock eat, radio fires. Failed: stovehouse greens 0 to 0 (soil read outdoor air), 6 of 24 frost crossings without a line (likely bus dedupe), and radio desk, hoe, pens, troughs, livestock, wolf, straw needed grants. #113 changed all of these; no re-run.

## What a first human should look at
1. First hour: does the start site look right (bank by the creek, spruce and birch near, not a bare gravel flat)? See docs/qa/start-sites-50.md for seeds that fail the blueprint check.
2. Can you light the first fire, make the bow, get stone and flint, without /yoi? Every gate was opened by hand in drivers; a human is the real test of the craft path (bushcraft gate open item).
3. Bears: do they come to a gut pile or a rack in September, and does the contact sequence (notice, huff, rush, bluff or contact) read as fair? This is the open gate.
4. Cold: wear the parka, mittens, mukluks, fur hat. Do the Messenger lines explain every vital? Does the energy cap on the rest line make sense after a broken night?
5. Fire as a night job: birch against spruce, 3-4 wakes.
6. Cabin: build, stove, chimney. The inside temperature is a fitted number, not the blueprint's duty-cycle model. Does +32 C at -30 C feel too hot?
7. Homestead year 2: where do hens, goats, reindeer and the wolf pup come from (supply drop day 365 / 1095; pup 120-200 m out in summer), and is it findable?
8. Visuals the proxies could not judge: placeholder textures on the 7 start-kit garments, vanilla textures on several M8/M9 blocks, camera aim in screenshots.
9. Untested paths: fall guard, outpost-visit line, supply-drop livestock, DenPups.
