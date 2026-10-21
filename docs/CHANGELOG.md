# Changelog

By milestone, newest last. PR numbers are the squash-merged GitHub PRs on `ruhanu155/years-of-survival`. "Proxy" means a scripted Xvfb client run standing in for a human gate. RECONCILE items are the Director's log (not in the repo).

## M0 - M2.5 (context)
- #1 seeded world, stages 1-3. #13 water and set pieces, stages 4-5. #20 walkable valley (terrain, water, slice preset, trees). #9 slice fire, #10 slice body and Trail Messenger, #14 rod and fight overlay, #15 small game and the passive moose, #18 slice cooking, #22 guided first night. #7 CI, studio inventory, content lint.

## M3 - start site, fire, onboarding
- #28 felling, bucking, firewood; placed logs never fall. #27 bough bed, day-one half-speed clock.
- #30 save migration: versioned codecs, aliases, world gate and backup, fixtures.
- #31 practice, gates, station recipes, workbench, tool wear, lift a full box. #32 fire: water, light, builds, ferro rod, spread, roster. #33 field-note cards, music, credits.
- #34 start-site score, Station Nine outpost, starting kit. #36 start-site cover and bank rules (the first proxy found the site on a huge bare gravel flat). #35 api/README context pack.
- #60 layer cache for the start-site search. #61 re-landed the lost crafting system.

## M4 - body
- #37 pack bands and hauling. #38 injuries, sources, treatment. #39 collapse, the Endless SOS, ground sleep, vitals. #40 illnesses, metabolism, rest days, sunburn, beard. #41 first-person clips. #42 treat flow, infection, garment tear. #43 wellness wiring. #44 live frost and exposure, two-day proxy. #45 fireless first-night tuning, starting clothes worn.

## M5 - food, plants, fishing
- #46 racks, food cache, trap rolls. #47 small game, furbearers, grouse, carcasses, theft. #48 species data and multi-species rod loop. #49 forage, knowledge, garden, lick salt. #50 named foods, dishes, deadfalls. #51 gear by hand, baits, live fish. #52 vegetation placement, quern, garden catch-up. #63 art pass. #64 integration and month proxy. #65 first-month tuning.

## M6 - hunt
- #66 weapons, hit-zone shot, blood trails, calls, blinds. #67 door entry, rack raids, encounter clips, trophy wall. #62 big game, predators, carcasses, tracks. #68 birds and waters. #78 long-runway tests in their own far region.
- #85 animal sign (track, scat, browse, bed) and the bow chain; feathers from birds.
- #86 first bear proxy: no bears in range. #88 the valley's bears: off-screen pool, scent reach, bear contact. #91 stone, flint, splits; live scent-following; input-source sweep. #96 scent lock through BED/WATER acts. #106 far piles reached abstractly. #110 arrival tolerance.
- Proxy logs #95, #99, #102, #109, #114 (v7: 2/10). **Gate still open.**

## M7 - winter
- #70 weather and storms. #69 seasons, freeze-up, snow, ice, terrain-change queue. #72 life of the year. #71 travel, winter gear, sky lore, rare events, music. #73 integration: storm water into snow pack, three-year test.
- #93 proxy fail (clothing never worn). #94 equip path; body reads worn items. #98 frost-stage Messenger lines per zone. #101, #105 awake clock and sleep windows. #97, #100, #103 proxy logs. #111 v7 **PASS**.

## M8 - cabin
- #77 pieces, rooms, heat model. #75 stoves, chimneys, storage, cellars, smokehouse. #76 outposts, decay, raft, fence drifts, hot tent (rebuilt after a squash dropped files). #81 M6 review blockers, one heat-source api.
- #90 proxy fail (stove posts 0 W). #92 room cache fix, recipes, stove/pipe saved. #104 proxy fail (body ignores room). #107 body reads room air; chimney-draw dial. #108 v3 **PASS**.

## M9 - homestead
- #80 wolf, sled team, lead fence. #79 iron tier, homestead gate, sluice, canoe. #82 radio, supply drops, ambitions, 16 regions, dev border. #83 livestock, long world, stovehouse, sourdough. #84 item models, rare events live, ambition triggers. #87 trust attachment, store spoil, feed boxes, hold-to-work pieces.
- #112 proxy **FAIL** on content (stovehouse greens 0, unobtainable homestead). #113 lane J: homestead obtainable, FenceDrift hang, radio on HudBus, stovehouse soil, outpost line, fall guard. Not re-run.

## Docs / QA (this PR)
- CHANGELOG, ROADMAP, DEVGUIDE, PLAYTEST, BLUEPRINT-DEVIATIONS, qa/start-sites-50.md.
