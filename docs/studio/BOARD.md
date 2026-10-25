# Agent board (low-usage mode, from 2026-10-19 18:05 ET)
Rules: one agent at a time; Sonnet; read only what your task needs; append ONE line here when you start and ONE when you finish (`date | branch | status | PR#`). Reply to the Director in ≤10 lines.
Queue: 1 integration-pass (finish) → 2 bough-bed (finish) → 3 felling (finish) → M2.5 proxy check (Director) → M3 lanes one by one.
2026-10-19 | dev/studio/integration-pass | PR open, build + 77 GameTests pass, not merged | #25
2026-10-19T22:44:01Z slice-bough-bed: PR opened (dev/building/slice-bough-bed), build + 79 GameTests pass, not merged
- 2026-10-19T23:06:46Z dev/building/m2-felling: PR #28 open (not merged), build + 85/85 GameTests green on merged main incl #27; .gitignore build/ and logs/ anchored, build package now committed.
2026-10-19T23:49:41Z dev/fire/m3-fire-water-light: M3 lane C started (worktree /home/claude/wt-m3c)
2026-10-19T00:27:22Z dev/build/m3-migrate: M3 lane E PR #30 open (not merged), data.migrate + migrate_fixtures + B3 as tests, build + 88 GameTests green; B5 release dry run skipped (listed in PR)
- M3 lane B (building): PR #31 crafting/stations/practice/wear/lift box; build + 94 GameTests green
2026-10-19T21:40:00Z dev/fire/m3-fire-water-light: M3 lane C PR #32 open (not merged), build + 102 GameTests green on merged main incl #30; skipped list in PR
- D (look, m3-polish): field-note cards A8, 176 corner fix, 181 MessengerAccess, Spirits music + credits screen, TestFilter. PR above.
2026-10-19T21:50:00Z dev/worldgen/m3-start-site: M3 lane A start site + Station Nine + starting kit; PR open, 109 GameTests pass, M1 hashes re-recorded (own commit)
2026-10-20 | dev/body/m4-injury | started
2026-10-20 | dev/body/m4-illness | started
2026-10-20 | dev/body/m4-sos | started
2026-10-20 | dev/pack/m4-strength-encounters | started
M4-D pack strength/hauling PR done; clips/stagger/screen skipped
- 23:46 body lane A (M4 injuries): PR #38, 5 GameTests + InjuryLaneTest; skipped hold/radial UI, garment tear, infection stages
2026-10-20 | dev/client/m4-encounter-clips | started (D2)
- M4 lane C (body/m4-sos): PR #39, 6 GameTests (48 in body/,net/ pass), skipped body_public, spirits day, Hardcore RunEndedScreen/records button, left_pack map pin, Curios slots in left pack
2026-10-20 | dev/body/m4-completion | started (A2)
- M4 lane B (Body illness, metabolism, rest, skin, hair): PR #40, JUnit all green, 47 GameTests (body/,data/) pass; skipped list in PR/report.
M4 D2 encounter hand clips: PR above, dev/client/m4-encounter-clips; pack readout skipped
2026-10-20 | dev/body/m4-wellness-wiring | started (B2)
- M4 A2: PR #42 (restores injury code deleted by #39 merge); 58 body/ + net/ GameTests pass; skipped: storage-reach, inner_pocket slot, left_pack map pin, body_public rendering, BodyCamera grey (done as client.body.CollapseFx)
B2 (PR above, stacked on #40): face bonus, glove x0.7, fever sleep, rest rates, pack_readout; skipped scurvy/body_public/exposure_min/grease hold
2026-10-20 | dev/body/m4-integration | started (INT)
M4-INT: PR opened, 134 GameTests green, proxy check run (fireless survivor collapses night 1)
2026-10-20 | dev/body/m4-tune | started (TUNE)
M4 TUNE: PR #45 body starting kit worn (kit_warmth_c dial), fireless night tests, fire smoke PASS
2026-10-20 | dev/ecology/m5-small-game | started
2026-10-20 | dev/plants/m5-forage-garden | started
2026-10-20 | dev/fishing/m5-fishing | started
2026-10-20 | dev/food/m5-traps-racks | started
M5 lane D (food racks/cache/trap rolls/commands): PR pending, 52 gametests green, skipped black sand/deadfall
2026-10-20 | dev/food/m5-food-complete | started (D2)
M5 lane A (small game/furbearers/grouse/carcasses/theft): PR #47, 5 new gametests + 1 JUnit class green (eco/,data/ 48 pass), skipped calls/leverets/midden/sign
- 04:45 Fishing M5 lane C: PR #48 (species data, multi-species rod loop, passive gear, /yoi fish spot|fight|gear; 5 GameTests + FishDataTest). Skipped: fish mobs/schools, gear blocks/items, extra rods/bait, fish wheel.
M5-B plants/forage/garden/lick: PR #49 (stacked on pr45), 5 GameTests + JUnit pass
2026-10-20 | dev/fishing/m5-fishing-gear | started (C2)
2026-10-20 | dev/plants/m5-plant-placement | started (B2)
C2 fishing gear PR done: see PR; gear by hand, baits, fish entities, fight art; skipped rods/fly_pattern/tip-up, pull_line, wings
2026-10-20 | dev/look/m5-art | started (ART)
- M5 B2 (plants placement/soil mean/poison/quern/catch-up): PR #52, stacked on #49; Hash JUnit green; skipped station recipe type
2026-10-20 | dev/m5-integration | started (INT + proxy)
2026-10-20 | dev/food/m5-tune | started (TUNE)
M5 TUNE: PR #55 (stacked on #54), 4 GameTests + FishSpotNearStartTest, month proxy PASS min BMI 19.63 (hares 0.50/d, fish 0.73/d), pilot bread 100 kcal, spot<=60 m, bait chain; trend still falling
2026-10-20 | dev/ecology/m6-big-game | started
2026-10-20 | dev/hunt/m6-weapons | started
2026-10-20 | dev/birds/m6-birds-waters | started
2026-10-20 | dev/story/m6-encounters | started
M6 lane B (hunt weapons/shot/blood/calls/blinds): PR #56, 6 GameTests + HuntSimTest; skipped: spear throw, sinew bow, walking stick/limp, fish arrow, minigame call bar, sounds, arrow model
M6 lane D: PR #57 dev/story/m6-encounters (stacked on #55): door entry, rack raids, encounter clips, trophy wall, bone grease, talk as you walk; 6 GameTests pass
- M6 lane C birds+waters: PR #58 open, 6 required GameTests (58 in birds/+fish/) and JUnit green, prepush OK
2026-10-20 | dev/build/gametest-boot | started
2026-10-20 | build gametest-boot | PR #60 layer cache (seed 3141592 start search 34s->~0 warm), datamap guard; JVM flags measured worse, dropped
2026-10-20 | dev/building/m3-crafting-reland | started
12:28 | build M3-reland | PR pushed dev/building/m3-crafting-reland
2026-10-20T23:45 | dev/weather/m7-storms | started
2026-10-20T23:45 | dev/world/m7-seasons-ice | started
2026-10-20T23:45 | dev/ecology/m7-year-life | started (stack pr68)
2026-10-20T23:57 | dev/ecology/m6-int | started (stack pr62)
2026-10-20 | M7 B dev/world/m7-seasons-ice | PR #69 | seasons/ice/snow/queue/catch-up; 6 GameTests + 2 JUnit classes green locally
2026-10-20 | M7 A weather | PR #70 dev/weather/m7-storms: storm table, WeatherService, glaze/hail/limb, strikes, decade storm, /yoi weather; 5 JUnit + 3 GameTests
2026-10-21T00:44 | dev/weather/m7-storms | PR #70 reported
2026-10-21T00:44 | dev/travel/m7-winter | started (stack pr70)
2026-10-21T00:46 | dev/world/m7-seasons-ice | PR #69 MERGED
2026-10-21T01:12 | dev/ecology/m7-year-life | PR #72 reported
2026-10-21T01:20 | dev/weather/m7-storms | PR #70 MERGED
2026-10-21T01:38 | dev/ecology/m7-year-life | PR #72 MERGED
2026-10-21T01:40 | dev/ecology/m6-big-game (M6-INT) | reported
2026-10-21T01:40 | dev/world/m7-int | started
2026-10-20 | M7 D | PR #71 travel/winter/snow rooms/sky/music done; worldgen_no_spill, worldgen_slice_valley, start_station_nine fail in the unfiltered run (not touched by D)
2026-10-21T01:45 | dev/travel/m7-winter | PR #71 reported
2026-10-21T01:58 | dev/building/m8-rooms | started
2026-10-21T01:58 | dev/fire/m8-stoves-stores | started
2026-10-21T02:01 | dev/travel/m7-winter | PR #71 MERGED
2026-10-21T02:13 | dev/building/m3-crafting-reland | PR #61 MERGED (craft type yearofice:craft_station; fold with crop station in M7-INT/M8)
2026-10-20 | M7-INT PR #73 dev/world/m7-int | storm water->SnowPack, 3yr x5 seed test, tint models, plant lang, salmon_swim_run + storm_snow GameTests; skipped permafrost/land cmds/ring/wind+cold storms; prepush build hit 137 twice (OOM), CI to verify
2026-10-21T02:30 | dev/world/m7-int | PR #73 reported
2026-10-21T02:31 | dev/building/m8-outposts | started
2026-10-21T02:37 | dev/world/m7-int | PR #73 MERGED
2026-10-21T03:19 | dev/ecology/m9-wolf | started (stack pr62)
2026-10-21T03:23 | dev/ecology/m6-big-game | PR #62 MERGED — M6 fully on main
2026-10-20 | M8 C | PR outposts/raft/drift/tells/hot tent/mishaps; 7 GameTests; tents unpitched, stubs for RoomService/HeatSource
2026-10-21T03:25 | dev/building/m8-outposts | PR #74 reported
2026-10-21T03:26 | dev/building/m9-iron | started
2026-10-21T03:37 | dev/fire/m8-stoves-stores | PR #75 opened (M8 lane B: stoves, stores, cellars, dugouts; 9 GameTests + StoveTest)
2026-10-21T03:45 | dev/fire/m8-stoves-stores | PR #75 reported
2026-10-21T03:45 | dev/building/m8-outposts | #74 closed → rebuilt as PR #76
M8 A: pieces+rooms+heat PR open (dev/building/m8-rooms); 4 GameTests, JUnit HeatTest; full build 137 twice locally
2026-10-21T03:46 | dev/building/m8-rooms | PR #77 reported
2026-10-21T03:52 | dev/building/m8-rooms | PR #77 MERGED
2026-10-21T03:55 | dev/ecology/chase-cap-isolate | PR #78 (long runways in own far regions)
2026-10-21T03:55 | dev/ecology/m9-homestead | started
2026-10-21T03:58 | dev/ecology/chase-cap-isolate | PR #78 MERGED
2026-10-21T04:07 | dev/fire/m8-stoves-stores | PR #75 MERGED
2026-10-21T04:13 | dev/building/m8-int | started (M6 review blockers first)
2026-10-21T04:21 | dev/building/m8-outposts-rebased | PR #76 MERGED — all M8 build lanes on main
2026-10-21 | M9 B (iron tier, sluice, wheel conditions, canoe) | PR #79 | 6 GameTests build/black_sand_bloom, build/sluice_triple, craft/iron_gate_opens, craft/station_alias_loads, fish/wheel_catches, travel/canoe_capsizes
2026-10-21 | M9 A dev/ecology/m9-wolf | wolf taming + sled team + B12 lead fence; 4 GameTests (companion/*, hunt/lead_fence_funnels), 8 JUnit; PR pending
2026-10-21T04:59 | dev/ecology/m9-wolf | PR #80 reported
2026-10-21T05:00 | dev/story/m9-world | started
2026-10-21T05:03 | dev/building/m9-iron | PR #79 MERGED
2026-10-21T05:23 | dev/ecology/m9-wolf | PR #80 MERGED
2026-10-21T05:47 | dev/building/m8-int | PR #81 reported
2026-10-21T05:54 | dev/building/m8-int | PR #81 MERGED
2026-10-21T06:18 | dev/story/m9-world | PR #82 reported (radio, drops, 61 ambitions, keepsakes, 16 regions, dev border; 6 GameTests + 2 JUnit)
2026-10-21T06:50 | dev/ecology/m9-homestead | PR #83 reported (livestock, long world, stovehouse, sourdough; 646 JUnit, 15 GameTests, all under 1.3 s)
2026-10-21T06:52 | dev/story/m9-world | PR #82 reported
2026-10-21T06:52 | dev/ecology/m9-homestead | PR #83 reported
2026-10-21T06:57 | dev/story/m9-world | PR #82 MERGED
2026-10-21T07:06 | dev/world/m9-int | started
2026-10-21T07:06 | dev/story/m6-proxy | started (M6 proxy check: m6hunt, m6bear)
2026-10-21T07:06 | dev/ecology/m9-homestead | PR #83 MERGED — all M9 build lanes on main
2026-10-21T07:09 | dev/hunt/m6-sign-bow | started (M6 lane E: sign system + bow/arrow/spear recipes — proxy block)
2026-10-21T07:27 | dev/world/m9-int | PR #84 reported
2026-10-21T07:27 | dev/world/m9-int2 | started (stack pr84)
2026-10-21T07:34 | dev/world/m9-int | PR #84 MERGED
2026-10-21 | M6 E dev/hunt/m6-sign-bow | sign blocks + age model + SignService + bow/arrow/spear recipes; 5 GameTests; PR open
2026-10-21T08:02 | dev/hunt/m6-sign-bow | PR #85 reported (feather drop pending)
2026-10-21T08:12 | dev/story/m8-proxy | started (M8 proxy check: m8cabin)
2026-10-21T08:37 | dev/hunt/m6-sign-bow | PR #85 MERGED
2026-10-21T08:52 | dev/story/m6-proxy | PR #86: m6bear FAIL (design gap → lane F); m6hunt deferred
2026-10-21T08:53 | dev/ecology/m6-valley-bears | started (M6 lane F: off-screen pool + scent reach)
2026-10-21T09:06 | dev/story/m6-proxy | PR #86 MERGED (m6bear driver)
2026-10-21 | M9-INT-2 | PR dev/world/m9-int2: see PR; skipped tent/jack, 3 GAP tests (no behaviour), 42 triggers
2026-10-21T09:48 | dev/world/m9-int2 | PR #87 reported
2026-10-21 | M6 F PR open | valley pool + scent reach + bear contact; 9/10 seeds reach
2026-10-21T09:49 | dev/ecology/m6-valley-bears | PR #88 reported (9/10 seeds)
2026-10-21T10:46 | dev/ecology/m6-valley-bears | PR #88 MERGED (incl. #89's arrow fix; #89 closed)
2026-10-21T10:55 | dev/world/m9-int2 | PR #87 MERGED — all build + integration lanes M1–M9 on main
2026-10-21T11:24 | dev/story/m6-proxy2 | m6bear: bears appear, none reach (live AI gap); m6hunt blocked (no stone/split sources) → lane G
lane G running; m6proxy2 parked; M8 proxy running
M8 proxy: dev/story/m8-proxy RESULT FAIL (RoomService ignores solid stove source)
M8 proxy FAILED (314): stove W never reaches room → lane D launching
M8 lane D (heat in room) + M7 proxy (m7week) launched; #90 merges when green
2026-10-21 | M6 G dev/hunt/m6-stone-scent | stone/clay loot, plank/log_piece recipes, scent pick+lope, /yoi scent list
2026-10-21 | M8 D dev/build/m8-heat-in-room | Rooms.entry rebuilds on clock-ds staleness or source-W change; Structs keep stove+pipe; recipes for pieces/materials/tin_stove/chimney_pipe
#92 merged (lane D); #91 red on glaze_forms, lane G fixing
M7 proxy FAILED (317): clothing never worn → lane F launching
M7 lane F (wear it) + M9 proxy (m9hour) launched; running: lane G fix (#91), lane F, M9 proxy, proxy2 parked
#91 merged (lane G); m6bear re-run next (after M9 proxy client)
2026-10-21 | M7 F dev/body/m7-wear-it | wear it: ClothingService, Curios inner slots, StartOutfit, body reads worn
m6bear v3 FAIL 0/7 (319) → lane G chase-cap-scent fix; #94 CI pending; M9 proxy running
#94 (lane F) + #95 (m6 logs, docs-only) merged; m7week re-run queued behind M9 proxy; lane G on chase-cap/bear
M7 proxy v2 near-PASS (321): frost-stage lines → lane G-msg launched; #97 to merge; #96 CI
G-msg: PR frost zone lines
#96 #97 merged; #98 frost msgs CI; m6bear v4 running; M9 + M8 proxies running
#98 merged (frost msgs). M7 gate now needs one confirming m7week run after proxies free the box
M7 lane H (awake cap) launched; #99 #100 to merge on green; m6bear v5, M9, M8 proxies running
2026-10-21 M7 H dev/body/m7-awake-cap: awake clock repaid by sleep; asleep lines at wake
#101 merged (M7 awake cap). m6bear v5 0/10 → lane G investigating bear-goes-to-player; m7week v4 gate run next when box free
MAIN RED build/outpost_repairable → lane E fixing first; M8 proxy v2 FAIL (body ignores room) → lane E
#106 bear pool fix CI; #105 awake clock CI; m6bear v6 after #106; M9 proxy run in flight; lane E on main red/body-in-room
#106 #105 merged. Queued clients: M9 loop (in flight), m6bear v6, m7week v6 (gate). Lane E on main red/body-in-room.
2026-10-21 | M8 E | PR body-in-room: sampler reads RoomService; Heat chimney draw 1 W/C per kW^2; StationCraft.why/lastRefusal
WRAP-UP MODE: no new lanes; finish 4 runs + #107
M7 PASSED (#111 pending CI). M8 PASSED. Open: #110 bear arrive; M9 PR pending.
2026-10-21 | M9 J | PR #113 homestead obtainable + bugs (sweep, fence hang, radio/outpost Messenger, stovehouse soil, fall guard)
DEFERRED ROUND: lanes K M N O running; L queued
16:55 | M9 N dev/eco/m9-debt | wolf ranges, ground yield, 4 debt tests | PR
#115 #116 #117 merged; lane K (bear) + lane L (m6hunt, M9 v2) running — last of the deferred round
proxy-check lane: m6hunt PR#120 FAIL (no moose found), m9 proxy v2 PR below FAIL
