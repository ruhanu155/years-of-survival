# Reconciliation notes collected by the Director (apply in the review round)

1. Canada jay / gray jay defined in both Creative concepts and Bestiary: Bestiary wins; Creative references it.
2. Cache "mice": Bestiary made them red-backed voles; the player-facing name stays "mice" (voles), video wording kept.
3. Ice house: Land and seasons says "22 ice blocks (2 t)"; Structures defines ice cake 45 kg, 20 cakes to a block (900 kg). Fix Land to Structures' units.
4. Pete Lund's monthly mail flight (Items tab) is new; add one line to Open world in depth › The radio, consistent with drop rules.
5. Keys: Minecraft engine tab controls win (R quick-eat, E pack, F off-hand swap, G whistle, Q drop, right mouse use/hold-to-work; M Z V B L kept). Land's "Read" key R conflicts: move Read to a free key per the engine table. Inventory section in main tab says X swaps off-hand; engine says F. Engine wins; note in main tab text replace only.
6. Data paths and component names: engine conventions win (data/yearofice/<kind>/<id>.json, no doubled namespace; data_maps; component yearofice:fresh, condition = vanilla durability). Bestiary used data/yearofice/yearofice/species/, Structures data/yearofice/yearofice/multiblock/, Waters fish_species/, Items item_props.json and yearofice:freshness/condition. Normalise.
7. Ideas from viewers: row 11 co-op should read "Open decision (Ruhan)", not Decline; row 38 other places should read "Later: regions as data packs", not Decline; row 32 release: "Business decision, later".
8. Water calls tab: CallRhythmOverlay GUI layer must be added to the engine HUD layer list, sharing the lower-centre minigame slot.
9. Multiplayer time: proposed rule that hold-to-work time-lapse charges game minutes to the worker's body while the world clock stays shared (needs to go in engine tab under multiplayer, marked pending the co-op decision).
10. Bestiary/Water calls: confirm gray jay, ermine, great horned owl, coyote are defined in Bestiary; caribou straggler band and moose lick-rate split from Water calls tab must agree with Bestiary/main.
11. Main tab still has some 8 km-world phrasing fixed by Director; World map tab is the authority on world size (three rings, 4,096-block districts, height 1,968).
12. Structures: permafrost cellar is the one dig exception; Video tab: dugouts are a building piece capped 4×2×6 m. Make sure both tabs and the engine's "Explore, don't dig" rules list both exceptions.
13. Look sound UI fixes: stomach fill shows on Vitals screen and eat prompt (no hunger line); daylight line on Forecast screen; thunder flash capped lightmap; fire readout "Fire, Low".
14. Raw fat: 700 kcal per 0.1 kg raw, 900 per 0.1 kg lard (Body dept). Video tab "900 kcal per 0.1 kg raw fat" must change; carcass/butchering tables must agree.
15. Berry bushes: one block per species yearofice:<species>_bush, tag yearofice:berry_bushes, class com.yearofice.forage.BerryBushBlock; regrow 0–8 state. Plants tab must match.
16. Packages: com.yearofice.forage.* for plants; food.* for eating/stomach. Add "forage" to the engine's system list.
17. Calorie scaling: BMR ÷ 1,794 replaces kg ÷ 77 in burn formula (main tab text replace if found).
18. Ideas from viewers tab: data paths data/yearofice/yearofice/plants/ → data/yearofice/plant/; camelCase JSON → snake_case.
19. Player attachments: grime and nausea folded into yearofice:body (Viewers tab lists separate attachments; update).
20. Engine › Explore, don't dig, rule 4: reword to "Deep ground may be cleared only inside a validated dugout, root cellar (permafrost cellar) or chimney footprint, level with or above the mouth floor, with no drops." (apply in engine tab).
21. Heat data: use data map data_maps/block/heat.json (Video tab says data/yearofice/heat/faces.json).
22. Multiblock/piece data path: pick one: data/yearofice/build/piece/<id>.json (engine singular kind convention); Structures uses data/yearofice/yearofice/multiblock/, Video tab data/yearofice/build/pieces/. Normalise to data/yearofice/multiblock/<id>.json for multiblocks and data_maps/block/piece.json for per-block numbers.
23. Cache: grizzly test = deck underside ≥3 m above the surface the bear stands on; guards: a band set right is proof, 20% only when snow brings it in reach. Crafting recipe makes a set of 4 bands.
24. Marker posts/cairns: Structures numbers win (5 stones, 30 blocks); Creative's 12-stone is the letter cairn; markers link within whiteout sight (20/40 blocks). Video tab's 6 stones/15 m must change.
25. Lanterns replace the Evening light Spirits source (cap +3, daily clamp +4).
26. Plexiglass: wreck window cuts into 6 offcuts; then tin lanterns. Items tab must agree.
27. Shoring: 1 game hour per frame.
28. Livestock prices: radio catalogue is the single source. Director already set Viewers tab to hens (3 + rooster) 100, goats 150, reindeer 180. In the Open world in depth radio catalogue: change "Four hens" to "3 hens and a rooster" at 100 and add reindeer at 180 (Homestead tier, after 2 years of logs). Milk: 240 days after a May kidding.
29. Hen laying curve: light factor 0→1 between 10 and 14 h; temp factor 0 below 0 °C, 0.5 at 0–5 °C, 1 above 5 °C. Feed 0.1 kg/day at ≥5 °C, 0.15 kg below; fish meal = 0.75 kg grain per kg. Crafting tab and main tab must agree.
30. Wolf taming: new per-action limits apply, but the existing daily trust cap still clamps; Open world's "once a day, 10 m" wording must point to the new table.
31. Talk input: V voice key (empty-hand use is an alias only where it hits a block/entity).
32. Java packages: animals in com.yearofice.ecosystem.*, traps in com.yearofice.food.traps per engine (or hunt.* per engine's system list — engine wins). Video and Viewers tabs use animal.*, hunt, fish: normalise.
33. Swim speeds: moose 1.8, caribou 1.6, grizzly 1.5, black bear 1.4, wolf 1.2 (Companion wolf 1.2 in my Swimming section matches), player 1.0 m/s.
34. Engine "vanilla turned off" list: hide/ban vanilla lily_pad, mud, packed_mud, mud_bricks (use yearofice versions); filter their recipes.
35. Engine gap: held light (torch/lantern/headlamp) dynamic lighting rule: decide client.light.HeldLight approach (and Iris/Sodium compatibility) for the Minecraft dev pass.
36. Vanilla dropped-item despawn (5 real minutes) must be overridden: dropped items persist 7 game days (Inventory section says map pins 7 days) via ItemEntity lifespan; sunk gear goes into lost_gear blocks. Add to engine "No vanilla mob mechanics"/"vanilla rules" list.
37. Walking stick: only for blocks outside yearofice:terrain_steps (engine gives 1.0 step height on terrain).
38. Lightning fires escalate to wildfire only if the year drew wildfire or fire danger is Extreme; player-started fires always can.
39. Weather card % follow the shown (forecast) state in 10% steps.
40. Beaver meat: raise Butchering and yields beaver meat from 2.8 kg to about 6.5 kg (18 kg whole) and kcal accordingly; video's "11 beaver meat" stack fine.
41. Carcass: single yearofice:carcass id in Video tab = tag/base class over carcass_tiny..huge. Death loot tables empty; butchering/<id>.json pools by step. core_temp attachment (Viewers) → coreTemp field of yearofice:carcass.
42. Butchering and yields (main tab): small game can be dressed anywhere (cleaning spot 30% faster); leftovers stay in the kill pile (no vanish).
43. Food units: engine counts food in 100 g stack units; Bestiary loot example uses kg — normalise loot counts to 100 g units.
44. Engine game rules: add doMobLoot false, maxEntityCramming 0; animals MobCategory.CREATURE; damage types scaling never; difficulty locked Normal.
45. Black-billed magpie defined in Bestiary carcass section; Birds tab must reuse id.
46. Fish data folder: engine says data/yearofice/fish/, Waters and Water life use fish_species/. Pick engine's fish/ (singular kind) unless lead engineer decides otherwise.
47. Caught-fish item id: species id + yearofice:fish_data component (Waters/Water life) vs engine's yearofice:fish_<species>. Decide one.
48. Burbot spawning window: Jan 25 – Feb 28, peak Feb 10 (Water life) — main tab says February; ok, align wording.
49. Lake trout forage test in Waters and fish Tests 6: Big Lake 0.73 ±0.02 with pygmy whitefish.
50. Mink theft: Bestiary 25% of nights empties a minnow trap; Waters 5% a fish; Water calls 10%. Owner: Bestiary (species owner). Unify: mink visits a minnow trap or net on 10% of nights near its den range and takes 1 fish; empties a minnow trap 25% of those visits.
51. Duck ids assumed by Water life: common_goldeneye, bufflehead, common_merganser — Birds tab ids win.
52. Plant ids assumed: bur_reed, mares_tail, buckbean — Plants tab ids win.
53. Moose yields: live weight bull 450–600 kg (Land animals); main tab yields (bull 103 kg meat?) are low; carcass tab whole bull 450 kg. Reconcile meat yield to ~45% of live weight (bull ≈ 200 kg meat) or justify; update Butchering and yields + kcal (moose 150,000 kcal figure may rise).
54. Loot paths: loot_table/butchering/<id>.json (Bestiary/carcass) vs engine "butcher/" data kind: pick one (prefer loot_table/butchering/).
55. Magpie now exists (yearofice:black_billed_magpie from carcass section) — Land animals scene should use it.
56. Rare-range species flagged (fisher, Alaska hare, collared lemming): keep rare, data can cut.
57. Duplicates across Land animals and Birds tabs: mourning_cloak (entity in both), yellowjacket (entity in Land; particle "yellowjacket" + aerial in Birds; yellowjacket_nest block in both), caribou warble/nose bot flies (Land: particle swarms; Birds: particles), magpie (carcass section + Birds new). Assign: insects → Land animals tab owns ground/nest behaviour; Birds tab owns the airborne particle; magpie defined once (Bestiary carcass section's id black_billed_magpie; Birds' sheet extends it).
58. Caribou warble hide penalty: Land says ×0.5 Jan 1–Aug 19; Birds says Feb–Jun hide grade −1. Unify to Land's rule.
59. Jay: entity yearofice:gray_jay; canada_jay is an alias (Creative concepts fold into gray_jay.json).
60. Input: V voice key: hold V looking level = pish; looking 45° up = howl; Talk to wolf = V near wolf. Client dev must define V context rules.
61. Bug pressure: B_total = mosquito + black fly + no-see-um + tabanid.
62. Birch syrup ratio: Director rules 100:1 (Blueprint › Cooking 40 L → 0.4 L stands). Fix the plant table and the Viewers tab from 40:1, and change Viewers' "about 2 L of syrup a spring" to about 0.8 L.
63. Plant data path: singular `data/yearofice/plant/` (engine convention). Fix the Land and seasons tab, which uses `plants/`.
64. Berry bushes: one block per species, shared class BerryBushBlock, all tagged `yearofice:berry_bushes`. Fix the Video tab's single `berry_bush` with a species property (supersedes nothing; confirms item 11).
65. Feral rhubarb leaves are toxic (−15 Condition a meal); native Alaska wild rhubarb is a separate species (Plants and fungi tab).
66. New cross-tab rules from Plants and fungi: cold cough (2 game h hard work below −30 °C → −1 Spirits/day for 3 days); no fungus can be taste-tested; a herbarium sheet is a 4th way to identify a plant. Health and Foraging sections must reference these.
67. Plant–animal hooks owned by Plants and fungi (moose browse brooming, hare girdling, bear scat seeding, sap-bucket raids, sapsicles, push-up tubers, goat toxicity, bird seed rain under perches): Land animals and Birds tabs should link, not restate.
68. Frozen food: below −5 °C decay stops (Blueprint rule and 120-day test win). The video's "58 days" becomes the tooltip's thaw countdown. Fix the Video tab wording.
69. Freshness component: `yearofice:fresh` with engine 1/40 steps; drop Items' `yearofice:freshness`, its 0–1000 `condition` and its 5-point bands.
70. Spoilage settling: lazy on read, nothing ticks. Fix Engine › Spoilage "every game hour".
71. Bone piles last 2 years (Bestiary). Fix Video › Winter's dead "1 year".
72. Wood rot factors: soil 1.5 × heated-room site 2 (= Structures ×3 for sills), forest shade 0.9. Structures' "10% a year" means 10% of what is left (37% after 10 years). Fix Structures wording.
73. Old camp's "30% of logs rotten at 10 years" is computed per log by AgeWorld (holds for an unpeeled cabin on soil sills). Fix Viewers / World detail wording.
74. Raw-fish parasite freeze is −20 °C everywhere. Fix Blueprint › Cooking (−18 °C).
75. Ice cave: food laid on the glare-ice floor counts as −6 °C. Add that to the Blueprint ice cave text.
76. Aging data folds into `data/yearofice/decay/` and `data_maps/block/decay.json`. Remove Structures' doubled `data/yearofice/yearofice/aging/materials.json`.
77. Additions: Field care green hide +6 days at 0–5 °C; Items rawhide rots if soaked over 3 days above 5 °C; Botulism added to Health, medicine and nutrition.
78. Open decision (Ruhan): the "stay out 1/2/5/10/30 years" choice on the Endless SOS screen.

## Lead engineer (Minecraft development › Architecture and the interlace)
SETTLED there: 6, 8, 9 (pending co-op), 16, 18, 19, 21, 22, 32, 34, 35, 36, 38, 41, 43, 44.
Left for client dev: 5, 13, 31, 37, 39. World dev: 12, 20. Content pass: 3, 10, 14, 23–30, 33, 40, 42.
79. Overrides to apply across tabs: tool condition = vanilla durability + `yearofice:edge`; freshness = `yearofice:fresh`; fuel litres = `yearofice:fuel_l`; all notes/logbook pages = one item `yearofice:note` + `yearofice:note_id`; `butcher/` data kind retired (species death loot empty, every death makes a carcass); 1,024-block sound cap dropped; snow/ice passes live in `world.pass`; TerrainChangeQueue moves out of `sim`.
80. Package names: use the lead's 22-package map and normalisation table (supersedes 20 `ecosystem.*`, and Plants' `food.forage` if it differs).
81. All JSON examples in every tab: camelCase → snake_case (validator rejects camelCase).
82. Needed data: `ground` block in `weather/interior_alaska.json` (world); decay curves `decay/<curve>.json` and `spoil_days_at_5c` in the food data map (food/hunting); confirm the 13 GUI layer order and that the call rhythm bar runs in the single minigame slot (UI).

## Client dev (Minecraft development › Player systems and client)
SETTLED there: 5, 13, 14, 17, 19, 31, 33, 37, 39, 43, 44 (8, 35, 36 follow the lead).
83. Engine-tab overrides: SOS away phase is a closed-form 14-day jump; belt = 3 typed Curios slots (sheath, quiver, belt), neck 2, inner pocket 2; map drains 4%/game h; step height 0.6 (1.0 on terrain blocks or with walking stick); PackMenu → item.inventory.PackMenu.
84. Blueprint › Inventory says Endless SOS keeps the pack: wrong. The pack stays at the collapse spot as a `left_pack` block that never expires (Collapse and SOS, M4 test). Fix Inventory wording.
85. For lead: confirm `item.inventory`; add `headwear` and `captions` to Layer 8 (15 layers); add `BodyService.wake(player, cause)`; game rules `yearofice:first_person_only` and `reducedDebugInfo` true; co-op proposals (1 game day away after SOS, item 9) go into the co-op decision (Ruhan).
86. For world dev: hourly temperature record for closed-form body steps; `lost_gear` placement; outpost bar return position.
87. For entity dev: `BodyService.applyInjury` is the only path for animal attacks; `carried_arms` and `carried_shoulders` poses on every species model; `env.light` for night sight; predator wake calls `BodyService.wake`.
88. For build dev: lints for subtitle keys, literal units in lang, codec defaults; sound asset pipeline and credits file; flash scan and HUD shots in client smoke.

## Entity dev (Minecraft development › Entities and AI)
SETTLED there: 1, 2, 10, 30, 32, 33, 36, 40, 41, 43, 44, 45, entity parts of 6, plus S1–S24 (fish folder `fish/`, caught-fish item id = species id, walk 3.0 / run 5.5 m/s in Land tab, dead bird = carcass entity, canada_jay → gray_jay, 60-caribou herds as group members, lick window ends Aug 31, legends in `story.legend` with one `legend/` folder, Yukon floater = `mussel_bed` state).
89. DIRECTOR RULING, dropped items: they last 7 game days (lead, item 24). The `left_pack` block never expires. Client text saying items never despawn or merge into ground piles is corrected.
90. DIRECTOR RULING, injuries: `BodyService.applyInjury` is the only path, animal attacks included; `BodyService.strike` is removed.
91. DIRECTOR RULING, ecology records: tiered records are accepted. Full records for big game, predators and named animals; small game, small birds and fish get slot records near players only. The 8 ms animal budget splits land/birds/fish as the entity section says. The lead's Layer 4 text is amended to match.
92. Body plans: build dev's 13 coarse body plans map onto entity dev's 10 classes and 34 rigs. Build dev updates its registry intro to match.
93. For world dev: `brush` block (no entity collision, slowdown inside), trunk columns per chunk, trail graph, path types on pieces/pens/fire, air points under ice.
94. For client dev: animation LOD hook, instanced impostor renderers, Known-mark and eyeshine render layers, herd/flock/school/aim/wolf-command payloads.
95. Content gaps: sounds for ermine, weasel, mink, muskrat, flying squirrel, rooster, dragonfly, belled reindeer; behaviour for loose livestock and muskox/bison calves; young as the same entity type; breath for divers; a click margin for tiny animals.

## World dev (Minecraft development › World and worldgen)
SETTLED there: 3, 11, 12, 15, 20, 37, 52, 64, 65; applied 6, 16, 18, 34, 36, 38, 44, 63 as lead ruled.
96. World rulings to apply across tabs: decay advances block stages on its day pass (randomTickSpeed 0); roof snow = `snow_cover`, not vanilla snow layers; stand age lives in the region stand map; trail wear cells are 2 m (fix Entities' 8 m); lily pads ship M7; Water tab's feature registry renamed `yearofice_water_features`; every berry block id ends in `_bush`; World map's sled figure fixed (6 m/s ≈ 9.4 new chunks/s, not 2–3).
97. Dropped items (ruling 89 stands): 7 game days; no `ground_pile` merging. Any persistent ground block (`left_pack`, `lost_gear`) sets `placed_mask` and blocks pruning.
98. For lead: confirm `world.change.ChangeTicks` derives the 5 change ticks from existing events or add `ChangeTickEvent`; add `world.catchup.LoadGate` (chunk not sent until its catch-up is done) to the threading rules.
99. For entity dev: dens, push-ups, redds, nests and middens go through World edits ops (`WorldEditService.submit`), never direct block writes.
100. For build dev: datagen for ~150 world blocks, a template validation test, layer hashes in CI on all platforms, the save rule for a generator version bump.

## Build dev (Minecraft development › Content pipeline, build and the evolving modpack)
SETTLED there: 1, 7, 15; enforces lead's 6, 16, 18, 19, 21, 22, 32, 34, 35, 41, 43, 44. Master inventory: 1,152 ids (Decay not counted). Its "Conflicting ids" (27 rows) and "Names settled" tables are binding for every tab.
101. DIRECTOR RULINGS: new data kinds `item_family/`, `flag/`, `retired/`, `valley_memory/` approved (Creative's `decay/` folder renamed `valley_memory/`); landed-fish item id = species id (engine's `fish_<species>` retired); `core_temp` renamed `core_temp_c` (unit-suffix rule holds); fish use spawn category WATER_AMBIENT, land animals CREATURE (spawning is by the population model anyway); bumblebee and dragonfly are particles; yellowjacket stays an entity with a `yellowjacket_swarm` particle; mourning_cloak is owned by Birds; Bestiary's datapack species registry is withdrawn (reload listeners + bootstrap reader).
102. Content tabs: apply the Conflicting ids and Names settled renames, including 174 camelCase JSON keys across 11 tabs.
103. Decay tab ids (item 68–78, component/attachment list) must be added to the master inventory.

## Director critique (pre-development)
104. BLOCKER: the cloud workspace cannot reach maven.neoforged.net, repo1.maven.org, services.gradle.org, plugins.gradle.org, libraries.minecraft.net, piston-meta/piston-data.mojang.com, resources.download.minecraft.net, the GeckoLib (cloudsmith) or Curios (theillusivec4) mavens. Java 21, gradle and xvfb-run are installed. Ruhan must allow those domains or link his computer to build.
105. GAP: no asset production plan for about 190 texture sets, 34 GeckoLib rigs, 107 plant blocks × phases, ~120 items and ~220 sound events (who makes them, tools, placeholders, licensing/credits). Needs an "Art and audio production" section: generator scripts for 16 px textures and geo.json from rig templates, a placeholder-art rule per milestone, and sound sourcing (CC0 libraries or synthesis) with a credits file.
106. GAP: no vertical slice before M5 (sent to reviewer G).
107. GAP: milestone content budgets predate the content explosion (sent to G).
108. GAP: main-tab studio rules 6–9 and Code layout still in browser terms (sent to G).
109. DECISION BEFORE M0 (Ruhan): co-op yes/no. The time model (time-lapse, close clock, sleep skip, SOS 14-day jump) and the architecture's time-debt proposal depend on it.
110. DECISION (Ruhan): game-day length (24 real min default; every rate is tuned to it).
111. GAP: the spec is ~2 MB across 21 tabs, with no per-milestone build spec. The Ready-for-development tab must give each milestone a reading list (tab › section) and its acceptance tests.
112. RISK: Body tab (new) and reviewer A both touch Health and injuries; reconcile after both land.
113. LOW: Minecraft EULA and brand rules for distributing a modpack (no paid access; "Minecraft" not in the title); en_us only, other languages later.

## Ruhan's decisions (2026-10-19 01:4x)
114. BUILD: both. The cloud runs builds and tests once the domains are allowed; Ruhan's linked computer runs the real client.
115. CO-OP: solo first, co-op later. Keep time rules co-op-safe (time-lapse and close clock per player, sleep skip only when all sleep, SOS jump per player) so co-op can be added after 1.0. Item 9 and the lead's time-debt proposal are recorded as the future co-op design, not built.
116. ART AND SOUND: generated only, plus found models and textures. Found assets need a licence that allows redistribution in a modpack (CC0, CC-BY or MIT, with credits). Vanilla Minecraft textures may be referenced at runtime but never copied into the jar.
117. DAY LENGTH: player-set at world creation, 24–72 real minutes, default 24. Every rate is defined per game minute so the slider rescales them. Logged as a world option in `yearofice_world`.
118. LOADER (Ruhan: "whatever you recommend"): NeoForge 1.21.1 (Java 21, ModDevGradle 2.x, GeckoLib and Curios mature). The Forge 1.20.1 MDK in Ruhan's Desktop/Game Development/YearOfIce folder gets converted; its CLAUDE.md rules carry over (debug `/yoi` commands for every system, tuning in `yearofice-common.toml` with presets, original names and art). The messenger's in-world name is the "Trail Messenger" (from that CLAUDE.md).
119. RELEASE (Ruhan): personal use only. Drop public release work: Modrinth/CurseForge pages, release channels and the public pack README. Keep packwiz only to install the pack on Ruhan's own machine. Keep the save-migration promise (his own worlds must survive updates).

## Body tab (Body, injury and sickness, tab 1d99fbf0-ca30, body node 9af295f7-655e, order a67)
120. DIRECTOR RULINGS on its C1–C12, all accepted:
    - C1 Condition heals unless a wound bleeds, an infection is active or warmth < 30.
    - C2 dried sphagnum is its own pad: clean 24 h; plain dry moss 12 h.
    - C3 wormwood clears trichinella by day 7; tapeworm still needs medicine.
    - C4 the 5% raw-game parasite is tapeworm (×0.85).
    - C5 gut illness has a 12 game-hour floor.
    - C6 no nausea MobEffect.
    - C7 `injury_source/`.
    - C8 10 `part_frost` counters replace `zone_frost`.
    - C9 burns slow work per part.
    - C10 falls over 3 m −4, over 6 m −10.
    - C11 a Hurt or Badly hurt torso cuts useful swim time 10 → 8 or 6 game min. APPROVED: reviewer C and Main › Swimming must match.
    - C12 the Blueprint wins on numbers.
    P1–P10 proposals: review in the Ready-for-development pass.
121. Body record schema 2: `part_hp`, `d_sys`, `part_frost`, `lost_digits`, `crooked`, `injury_seq`. New data kinds `body_part/`, `injury/`, `injury_source/`, `illness/`, `action/treat_*`; sync `yearofice:body_public`; BodyService adds `partHealth`, `factor`, `canUse`, `limp`. Add to the master inventory.
122. TOOLCHAIN VERIFIED 2026-10-19 02:07: the cloud built a NeoForge 21.1.264 (MC 1.21.1) mod with ModDevGradle 2.0.148, GeckoLib 4.9.3 and Curios 9.5.1+1.21.1 on Java 21 / Gradle 8.14.3. The first build took about 5 min.

## Reviewer C done (water)
123. DIRECTOR RULINGS: the torso factors (×0.8 Hurt, ×0.6 Badly hurt) apply to every swim-time band, including 20 → 16 / 12. Swim time stays banded by water temperature (no flat 10 min). Decided by the Director as tuning, not a Ruhan decision.
124. Routed: E fixes Danger in detail › Self-rescue keys (RMB pull and brace, Q drop pack, Shift crawl, RMB hold to tap; remove "hold Space 10 s"). A fixes Blueprint › Cooking −18 → −20 °C. F fixes Video › Bears in water (grizzly 1.5, black bear 1.4 m/s). G adds 3 Voice wheel segments (Mouse squeak, Hen cluck, Go-back; 9 total).

## Reviewer D done (build and craft)
125. DIRECTOR RULING: "Split all" automation unlocks after the first 10 armloads split by hand (pillar 2, Respect the player's time). Every strike before that stays manual.
126. Routed to F (Video tab):
    - Firewood drying › softwood and hardwood: birch burns ×2.0 and gives about ×2.2 heat per armload against spruce, per the Blueprint Wood table.
    - Logs, falls and jams rule 2: point to Body tab "falling tree, direct, −40".
    - Bears in water: grizzly 1.5.
127. Routed to G:
    - Engine › Fire: dryness = the `yearofice:moisture` component plus `data_maps/item/fuel.json`.
    - Main › Build milestones: browser-era paths.
    - Add D's new ids to the master inventory: `stump`, `fallen_log`, `chopping_block`, `<wood>_round`/`_split` × 8, `spruce_bark`, the `felled_log` `hung` state, `action/fell|limb|buck|split|peel`, the `tree.*` sounds, `build.wood.FellingService`, `world.logs.FallingTreeEntity`/`FelledLogEntity`.
128. Docs tool note: an `"as":"text"` replace writes backticks literally, so code formatting needs a whole-block markdown replace.

## Reviewer B done (plants and land)
129. DIRECTOR RULINGS (tuning, not Ruhan's): accept B's Gentle and Brutal columns for `[forage]`, `[garden]` and `[land]`. Needle tea stays uncapped for vitamin C, since each cup costs boiled water, fuel and about 12 game min.
130. Routed:
    - F, Video › Berry patches: per-species `<species>_bush` blocks, `forage.BerryBushBlock`, states `phase`/`taken`/`ripe` 0–2, regrow count in the `patches` ledger, path `plant/<species>_bush.json`, rule 3 → 30 game min a cup. F, Open world › Sample prompts: add the `garden_window_closed` row (priority 2).
    - A, Blueprint › Cooking: mushroom stew names the edible species, with poison pax as the brown lookalike; berries' vitamin C defers to each species. A, Health: RECONCILE 66 references (cold cough, no fungus taste test, herbarium).
    - E, Land and Birds: link to Plants and fungi › Animals and plants instead of restating (item 67).
    - G, Dev › World creation options: day length 24–72, player-set. G, master inventory: crop blocks and items, `seed_tray`, `feral_rhubarb`, hoe, digging_stick, shovel, hand quern, `seed_lot`, the tags, and B's command classes.

## Reviewer E done (animals and hunting)
131. DIRECTOR RULING: item 58 is struck. S13 (entity dev, settled) wins for the caribou hide: Jun 1–Aug 14 graded −40%, Aug 15–Sep 30 full grade, from Nov 1 −20%.
132. Routed to A (Blueprint › Butchering and yields): bull moose meat 103 → 180 kg; beaver 2.8 → 6.5 kg; "a March moose is still about 135,000 kcal" → about 223,000; bull moose total 239,000 kcal. New kcal: hare 1,070, spruce grouse 640, ptarmigan 440, beaver 9,300.
133. Routed to G (Entities and AI): bumblebee and dragonfly are particles, not `CritterEntity` (ruling 101); S15 → `yellowjacket` entity plus `yellowjacket_swarm` particle. Voice wheel needs Loon wail, Crane rattle and Owl trill too (12 segments with item 124).
134. Director fixed directly: Body tab posture "C held" → "Shift held" (×2); Body P5 limp counts as weak "like Condition under 40". Main › fish spot capacity path → data/yearofice/fish/<id>.json.

## Reviewer A done (food and body)
135. DIRECTOR RULING (tuning): weight change stays at 2,400 kcal per kg at Standard; Gentle uses 3,500. A lost cache in a hare-crash winter is meant to be an SOS without extra set lines. Brutal stays at 2,400.
136. Director fixed directly: Crafting › Render fat → 0.7 kg lard (6,300 kcal) plus 0.1 kg cracklings; Waters › Sour fish defers to Decay and time (115% kcal, botulism only when sealed); Main › Pemmican 5,000 → 5,600 kcal per kg.
137. Routed:
    - to G: Layer 5 `fixed:ice_house` −4 → 1 °C; Layer 6 adds the `yearofice:nutrients` and `dish_id` components; master inventory `page` → `note` (item 79); Main › Clothing: the head net is sewn from the outpost window screen, not from the crate.
    - to F, Video tab: Eating fat → 0.7 kg lard; raw fat 900 → 700 kcal per 0.1 kg; moose-meat day about 5,100 kcal; stomach fill's mealtimes match auto-eat's two windows.

## Weather and storms tab (tab 503580ab-07fc, body node 7a75f110-bb1c, order a64)
138. DIRECTOR RULINGS:
    - Approve the new EnvSample fields (`cloud_pct`, `pressure_hpa`, `pressure_trend`, `humidity_pct`, `air_mass`, `glaze_mm`, `snow_ice_mm`, `smoke`, plus the new precipitation types), `env_sync` at 48 B, and `StormEvent.Start/.Peak/.End`.
    - Approve the 12 changed numbers. In particular, snowfall is scaled to the precipitation normals: a 59 cm March peak, and lightning on the player costs −30 Condition plus a burn.
    - The storm of the decade is the 8th rare event (10% a year from year 2, at most one in 7 years).
    - One sky-reading skill, "Sky lore" (Land), uses Creative's notebook page as its screen; Creative's L_sky formula is folded into it.
    - Quinzhee: Structures wins (+25 °C, candle +3); fix Land.
    - Structures' "Roof load 100%" → "Roof creaking under the load".
    - `story.Lightning` → `env.phenomena.Lightning`.
139. Routed:
    - to G: EnvSample and `env_sync` in Layer 2; `StormEvent`; `LIMB_BREAK` op; blocks `glaze_layer`, `hail_layer`, `fallen_limb`; `battened` state; payload `yearofice:strike`; SavedData `yearofice_weather`; `sim.weather`; data kinds `weather/storm/`, `weather/ring/`, `weather/forecast.json`, `event/decade_storm.json`; creation options day length 24–72.
    - to F: Open world hidden ambition "seven rare events" → eight; the weather JSON sample in Open world in depth defers to the Blueprint table and uses snake_case.
140. FINAL-PASS LIST (owners finished, so the Director applies these): Land quinzhee +18/+4 → +25/+3; Structures roof-load line; Creative L_sky folded into Sky lore; Video `story.Lightning` package.

## Art and audio production tab (tab b7a04995-cbfe, body node 3cfdcdda-6106, order a15)
141. DIRECTOR RULINGS:
    - 409 sound events plus 12 music events is the count.
    - One `assets/yearofice/credits.json` (replaces `sounds/credits.json`); CC-BY is allowed.
    - Ambience beds stay stereo 60 s loops (replaces 60–120 s).
    - Adding a species step 4 follows Entities and AI paths; Blockbench is the fallback only.
    - The foot bone is `foot` on every quadruped (Land's "hoof/paw" are texture words).
    - Critters: 5 entities; bumblebee and dragonfly are particles (ruling 101).
    - `yukon_floater` is NOT a mob: it is a state of `mussel_bed` (S21). Drop its 1-bone model; fix the inventory.
    - `moose_quarter` is a carry-load texture.
    - M2.5 content follows reviewer G's definition; the art tab's assumption (one valley region, start site and fire, raw and cooked meat, moose, hare and ptarmigan) is fine unless G differs.
142. LOOK CHECK by the Director (prototype sheet):
    - 16 px textures pass: the tamarack log, end grain, moose quarter and soapberry phases read clearly in a muted palette.
    - Generated models are G0 blockouts only: the moose's antlers and dewlap barely read, and the hare and ptarmigan silhouettes are weak.
    - Gate for G1: each species' side, front and 3/4 views must be recognisable at 8 m in a screenshot; antler palms on bull moose ≥ 6 px wide.
143. Routed to G: the engine tab's art budget, `tools/render-sounds` path and store art are superseded by the Art tab; `sounds/credits.json` → `credits.json`.

## Reviewer F done (world, story and Forge Labs)
144. Forge Labs audit: 322 rows, 273 Ready, 43 Fixed, 6 missing in finished owners' sections (FINAL PASS). All 18 Add and Adapt viewer ideas are specified.
145. DIRECTOR RULINGS: only the World rate scales with day length (720 ÷ L ds a tick, L in steps 24, 30, 36, 40, 45, 48, 60, 72); Travel, Close, Work and Sleep rates are fixed. Cover-class rules live in a `cover` list in `worldgen_params/valley.json`.
146. FINAL PASS (Director):
    - A row 185: "Body needs fat" alert in Macros and strength.
    - Auto-eat hand key Q → R.
    - Main Food systems "Build and test" is browser-era.
    - C row 189: pike in the lake gill net row.
    - C row 198: deep holes and drop-offs hold bigger fish.
    - D rows 95 and 143: wood slab floor and path pieces.
    - G row 71: "You need more food" alert in What the HUD shows.
    - Drop the local file path in F's audit block.
    - Code-format leftovers.
147. Routed to G: day length in the dev world options, Main Survival tuning "Game-day length" and "Four time rates" (rates fixed except World); confirm the `cover` list.
148. RUHAN DECISIONS (2026-10-19):
    - SOS stay-out choice (item 78): 1, 2 or 5 years.
    - The wolf can leave depending on preset: never on Gentle; warned and recoverable on Standard; for good on Brutal.
    - Default mode: Endless, with Hardcore picked at world creation and locked for that world.
    - FINAL PASS: record all three in Main › Open decisions.

## Reviewer G done (engine, dev tab, milestones)
149. Pin recorded: NeoForge 21.1.264, ModDevGradle 2.0.148, Gradle 8.14.3, GeckoLib 4.9.3, Curios 9.5.1+1.21.1.
     M2.5 slice: a 1 km test valley cut from one region's layers (world preset `yearofice:slice_valley`, batch `m2_5`); hare, ptarmigan and a passive moose; the outpost as start site; 6 slice GameTests; human gate 60 real min.
     Budgets: 161 entity types, 34 rigs.
150. FINAL PASS: the dev tab body-record table `zone_frost` ×5 → `part_frost` ×10 (Body C8).
151. RUHAN DECISION (2026-10-19 03:0x): art licences widened for personal use. CC-BY-SA and CC-BY-NC(-SA) are now allowed alongside CC0, CC-BY, MIT, OFL. Each SA/NC asset is flagged `release_blocker: true` in credits.json so it can be swapped before any public release.
     Still refused: no licence, All Rights Reserved (for example Naturalist's assets), ND, and copying Mojang assets.
152. RUHAN DECISION (2026-10-19 03:07): add true deer.
     - Mule deer (`yearofice:mule_deer`): rare wanderers in eastern Interior districts and Beyond provinces on the Yukon side.
     - Sitka black-tailed deer (`yearofice:sitka_black_tailed_deer`): in coastal Beyond provinces.
     - Neither lives in the home valley by default; a mule deer straying into the valley is a rare sighting.
     - Rig: cervid. Milestone: M6 content wave, behind a flag.
153. MOD REUSE (MODS.md). Code-reuse sources, personal use, licences noted:
     - TerraFirmaCraft (EUPL-1.2): decay, item heat, knapping.
     - Cold Sweat (GPL-3): temperature and room heat.
     - Homeostatic (MIT): wetness and water purity.
     - First Aid New (GPL-3): per-limb damage split.
     - Tide 2 (MPL-2.0): fishing minigame and catch conditions.
     - Snow! Real Magic! (MIT): snow over plants and fences.
     Optional client add-ons, after nightly-matrix tests: LambDynamicLights, Sound Physics Remastered.
     Dev-only and on the first-launch warn list: Jade, WTHIT, JEI, EMI.
     Not adopted: Carry On (copy its code instead), FallingTree, Dynamic Trees, Serene Seasons, Weather2, Tough As Nails, LSO, AmbientSounds, First-person Model, Real Camera.
     Copy MODS.md to docs/reference/mods.md.
154. DEER landed (Land animals › "Deer: mule deer and Sitka black-tailed deer"; 2 registry rows). DIRECTOR RULINGS:
     - A 7th Beyond province kind, `coast` (z ≥ 98,304, two or more rows south of the Interior; shoreline, spruce-hemlock forest, kelp wrack line). Its Weather row: winter +15 °C, summer −3 °C, precipitation ×6.
     - A new `cervid` rig: 35 rigs, 163 entity types.
     - Deer doe weight factor 0.65.
     - The spawn command is `/yoi spawn` everywhere.
     - Flag `yearofice:deer`.
155. FINAL PASS 2 (Director, with the docs export):
     - World map: add the `coast` province kind.
     - Weather: add a `coast` ring row.
     - Bestiary › Rare sightings: add a mule deer straggler row (8% a year, ×1.5 after a mild winter).
     - Land animals roster, Who answers to what, and Mothers and young: add rows for both deer.
     - Dev master inventory and sounds: add the deer ids and `rare_sighting/mule_deer_straggler.json`.
     - Entities: `/yearofice spawn` → `/yoi spawn`.
