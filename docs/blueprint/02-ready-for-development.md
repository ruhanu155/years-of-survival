# Ready for development

This tab is where a developer starts. It says which core loops are ready to build, which decisions are locked, and, for each milestone from M0 to M9, exactly what to read, what to deliver, what tests prove it and which /yoi commands ship with it. Read this tab first, then only the sections a milestone lists.

## Status board

**Every core loop is specified to build.** The reviewers traced each loop from first contact to mastery and closed what they found; the Studio lead's final pass on 2026-10-19 closed the last 6 Forge Labs rows and recorded Ruhan's last three decisions. "Ready with notes" means the loop can be built now and the note says what to watch.

| Loop | Owning tabs › sections | Audit result | Status | Notes |
| --- | --- | --- | --- | --- |
| Food | Blueprint › Food systems (Cooking, Butchering and yields, Spoilage model, Preserving in detail), Survival tuning › Calories and weight, Macros and strength; Items › Food values; Decay and time › Eating decayed food | Reviewer A, Items › Review audit (food loop and body needs): 28 steps, 6 Ready, 20 Fixed, 2 decided since (weight per kcal, RECONCILE 135; stay-out, RECONCILE 148) | Ready | The "Body needs fat." and "You need more food." alert lines are in Macros and strength and What the HUD shows |
| Body and injury | Body, injury and sickness (all); Blueprint › Survival tuning, Health, medicine and nutrition, Collapse and SOS; Minecraft development › Player systems and client › The body attachment, The body tick | Covered by reviewer A's rows (warmth, injuries, collapse: Fixed or Ready); Body tab C1–C12 accepted (RECONCILE 120); P1–P10 signed off in this pass | Ready with notes | No audit table of its own. Where the Body tab and Blueprint › Health differ, the Blueprint wins on numbers (C12). P6 now uses Open world's 10% per 100 m |
| Plants and farming | Plants and fungi (all); Land and seasons in play › Crop farming in play; Blueprint › Foraging and plant knowledge, Crop farming | Reviewer B, Plants and fungi › Review audit: 29 rows, 6 Ready, 22 Fixed, 1 decided (preset columns, RECONCILE 129) | Ready | Berry blocks are one per species, `<species>_bush`, tag `yearofice:berry_bushes` |
| Fishing and swimming | Blueprint › Fishing in depth, Swimming and working in water; Waters and fish; Water life in depth | Reviewer C, Waters and fish › Review audit (fishing and swimming): 33 rows, 5 Ready, 28 Fixed; swim-time bands decided (RECONCILE 123) | Ready | Lake gill nets take pike; deep water draws bigger fish (u^1.6 below 2 m, u^1.3 below 5 m) |
| Tree cutting | Crafting and building in depth › Trees, felling and firewood; Every concept from the video › Firewood drying, Logs, falls and jams | Reviewer D, Crafting › Review audit (Tree cutting; Camp to cabin): 35 rows, 10 Ready, 24 Fixed, 1 decided ("Split all" after 10 armloads, RECONCILE 125) | Ready | Felling and bucking ship in M2; hazards in M6 |
| Fire and camp to cabin | Blueprint › Fire, water and light, Camp, materials and builds; Crafting › Building pieces, Placing and snapping, Warm rooms; Structures and buildables | Reviewer D, same audit block | Ready | The wood slab floor and path piece were added in this pass (Building pieces) |
| Hunting and carcasses | Blueprint › Mechanics check: hunting, Hunting in depth, Butchering and yields; Bestiary › Carcasses and bodies; Land animals in depth | Reviewer E, Bestiary › Review audit (Hunting: from the first snare to the first moose): 24 steps, 8 Ready, 16 Fixed | Ready | Every death makes a carcass; death loot tables are empty |
| Weather | Weather and storms (all); Blueprint › Calendar, daylight and weather | No audit table; its 8 conflicts are settled (RECONCILE 138–140) | Ready with notes | Normals in M2, the full chain and storms in M7; no vanilla `LightningBolt` |
| Decay | Decay and time (all); Blueprint › Spoilage model | Covered by reviewer A's Preserve, Store and Spoilage rows | Ready with notes | No audit table of its own; lazy settling, nothing ticks |
| Wolves and livestock | Open world in depth › Taming a wolf; Ideas from viewers › Taming a wolf without bones, Raising animals; Minecraft development › Taming, livestock and raised young | Reviewer E, Bestiary › Review audit (Animal life): 12 rows, 7 Ready, 5 Fixed; wolf leaving decided 2026-10-19 | Ready with notes | The February leave roll, p = (80 − trust) ÷ 400 a day, was set in this pass; tune it at the M9 human gate |
| World and travel | World map; Open world in depth › Traversal, Outposts; Blueprint › Terrain and hydrology, Biomes and resources, The open world; Minecraft development › World and worldgen | Reviewer F, Video › Review audit (the year): 17 steps, 2 Ready, 13 Fixed, 2 decided | Ready | Home valley first; the Interior and Beyond are M10 and M11 behind the dev border |
| Story and radio | Open world in depth › Story, The radio, Ambitions, Rare events; Creative concepts; Items › Found and salvaged items | Reviewer F, same block (mail, radio contact, story threads, rare events: Fixed) | Ready | Eight rare events, the storm of the decade included |
| The Minecraft interlace | Minecraft engine (all); Minecraft development › Architecture and the interlace | Reviewer G, Minecraft engine › Review audit: 35 rows, 4 Ready, 31 Fixed; "Open decisions for Ruhan from this audit: none" | Ready | The development tab wins on build matters |
| Art and audio | Art and audio production (all) | Director look check (RECONCILE 142): textures pass; generated models are G0 blockouts | Ready with notes | Prototypes A1–A3 must pass before M2.5 ships; G1 needs each species recognisable at 8 m and bull antler palms at least 6 px wide |

**Verdict.** No loop is blocked. Four loops carry notes, and none of the notes stops M0 or M2.5 from starting.

## Forge Labs coverage

**All 322 rows of the concept table in Every concept from the video are Ready.** Reviewer F walked all of them: 273 were Ready, 43 were fixed in review, and 6 were missing in sections whose owners had finished. The final pass fixed those 6.

| Result | Rows | Count |
| --- | --- | --- |
| Ready at review | — | 273 |
| Fixed in review | Listed in the review audit block at the end of Every concept from the video | 43 |
| Fixed in the final pass | 71 "You need more food" (Blueprint › What the HUD shows, landed by G), 95 and 143 wood slabs (Crafting › Building pieces: `slab_floor`, `path`), 185 "Body needs fat" (Blueprint › Macros and strength), 189 pike in a lake gill net (Blueprint › Fishing methods), 198 bigger fish in deep water (Blueprint › Reading the water) | 6 |
| Still open | none | 0 |

All 18 Add and Adapt ideas from Ideas from viewers also have a section with rules, numbers and a build line.

## Decisions locked

**Every decision that is Ruhan's is made.** The table lists each with its date. Decisions dated 2026-10-18 were in the blueprint's fixed facts before the review round began; the rest were taken on 2026-10-19.

| Decision | What is decided | Date | Recorded in |
| --- | --- | --- | --- |
| Engine and loader | A Minecraft Java Edition mod on NeoForge, mod id `yearofice`, package `com.yearofice`; Ruhan's Forge 1.20.1 MDK converted | 2026-10-18 (engine); 2026-10-19 (NeoForge, RECONCILE 118) | Minecraft engine › Platform and project |
| Version pin | Minecraft 1.21.1, NeoForge 21.1.264, Java 21; moved only in its own round after a milestone | 2026-10-19 | Minecraft engine › Version pin |
| Mod list | Standalone: NeoForge, `yearofice`, GeckoLib, Curios; spark in dev only; Sodium, Iris, Distant Horizons optional | 2026-10-19 | Minecraft engine › Libraries |
| Build machines | Both: the cloud builds and tests; Ruhan's linked Mac runs the real client | 2026-10-19 (RECONCILE 114) | Blueprint › Open decisions |
| Co-op | Solo first, co-op after 1.0; time rules co-op-safe now (per-player time-lapse and close clock, sleep only when all sleep, per-player SOS jump) | 2026-10-19 (RECONCILE 115) | Blueprint › Open decisions |
| Day length | Player-set at world creation in steps 24, 30, 36, 40, 45, 48, 60, 72 real minutes, default 24; only the World rate scales | 2026-10-19 (RECONCILE 117, 145) | Blueprint › Open decisions; Minecraft development › World creation options and presets |
| Art and sound | Generated by the studio's tools, plus found assets under CC0, CC-BY, MIT or OFL, and for personal use CC-BY-SA and CC-BY-NC(-SA), each flagged release\_blocker: true so it can be swapped before any public release (no licence, All Rights Reserved, ND and copied Mojang assets are still refused), with credits in `assets/yearofice/credits.json`; vanilla textures referenced, never copied | 2026-10-19 (RECONCILE 116; widened 03:0x, RECONCILE 151) | Art and audio production |
| Deer | True deer added. Mule deer (`yearofice:mule_deer`) wander the eastern Interior and the Yukon-side Beyond provinces; one strays into the home valley in about 8% of years. Sitka black-tailed deer (`yearofice:sitka_black_tailed_deer`) live only in the new `coast` province kind. Neither lives in the valley by default. Cervid rig; M6 content wave behind the flag `yearofice:deer` | 2026-10-19 (RECONCILE 152; rulings 154) | Land animals in depth › Deer; World map › Province plans Beyond; Bestiary › Rare sightings |
| Mod reuse | Code reuse for personal use, licences noted: TerraFirmaCraft (EUPL-1.2) for decay, item heat and knapping; Cold Sweat (GPL-3) for temperature and room heat; Homeostatic (MIT) for wetness and water purity; First Aid New (GPL-3) for the per-limb damage split; Tide 2 (MPL-2.0) for the fishing minigame and catch conditions; Snow! Real Magic! (MIT) for snow over plants and fences. Optional client add-ons after the nightly-matrix tests: LambDynamicLights, Sound Physics Remastered. Dev-only and on the first-launch warn list: Jade, WTHIT, JEI, EMI. Not adopted: Carry On (its code is copied instead), FallingTree, Dynamic Trees, Serene Seasons, Weather2, Tough As Nails, LSO, AmbientSounds, First-person Model, Real Camera | 2026-10-19 (RECONCILE 153) | `docs/reference/mods.md` in the repository (from MODS.md) |
| Release | Personal use only; no public release work; packwiz only for Ruhan's machines; his saves must survive updates | 2026-10-19 (RECONCILE 119) | Minecraft engine › Ruhan's project rules |
| Default mode | Endless is the default; Hardcore is chosen at world creation and locked for that world | 2026-10-19 (RECONCILE 148) | Blueprint › Open decisions (Default mode) |
| SOS stay-out | 14 days (default), 1, 2 or 5 years; the long stays run AgeWorld and ship in M9 | 2026-10-19 (RECONCILE 148) | Blueprint › Open decisions; Decay and time › Time away |
| Wolf leaving | Never on Gentle; warned and recoverable on Standard; for good on Brutal; dial `companion.wolf_leaves` | 2026-10-19 (RECONCILE 148) | Blueprint › Open decisions (The wolf); Open world in depth › Taming a wolf |
| No hostile mobs | Only real animals and the environment threaten the player | 2026-10-18 | Minecraft engine › No monsters |
| World size | Three rings: the 8 km home valley, the 64 km Interior, the endless Beyond; 1 block = 1 m; height 1,968 | 2026-10-18 | World map › Three rings |
| Year length | 365 game days, one per calendar day, starting May 1 | 2026-10-18 | Blueprint › Open decisions (Year length) |
| Seed | A new seed every run, kept fair by the start-site checks | 2026-10-18 | Blueprint › Open decisions (Map) |
| Wolves, not a dog | Wolves are tamable companions and also prey; there are no dogs | 2026-10-18 | Blueprint › The open world (Taming a wolf); Open world in depth › Taming a wolf |
| Inventory model | 9-slot hotbar, off-hand (F swaps), pack rows of 9 opened by packs, weight limits 30/45/120/150 kg | 2026-10-18; keys 2026-10-19 (RECONCILE 5) | Minecraft engine › Inventory and packs, Controls |
| Clothing | 5 zones × inner and outer, warmth in °C, armor plus Curios slots; bare skin safe to −20 °C; frost on 10 parts | 2026-10-18; parts 2026-10-19 (RECONCILE 120 C8) | Blueprint › Moment-to-moment mechanics › Clothing; Minecraft engine › Clothing slots; Body, injury and sickness |
| Project rules | `/yoi` debug commands for every system; all tuning in `yearofice-common.toml` with Gentle, Standard and Brutal; original names and art; the device is the "Trail Messenger" | 2026-10-19 (RECONCILE 118) | Minecraft engine › Ruhan's project rules |

**Director rulings that bind the build** (tuning, not Ruhan's): banded swim time scaled by a hurt torso (RECONCILE 123); "Split all" after 10 armloads (125); the preset columns for `[forage]`, `[garden]` and `[land]` (129); 2,400 kcal a kg, 3,500 on Gentle (135); the weather numbers and the storm of the decade (138); 409 sound events and 12 music events (141); only the World rate scales with day length (145); the 7th Beyond province kind coast, the cervid rig (35 rigs, 163 entity types), a deer doe weight factor of 0.65 and /yoi spawn everywhere (154).

### Still open

**No decision is open for Ruhan.** Three technical choices are left to a test, by design:

- **Portals:** Nether and End are off by default (`yearofice:other_dimensions`); the owner can switch them on.
- **Surface layers:** 1/8 layers for gravel, sand, mud and snow, on or off by the M2 frame test.
- **Held light:** the `client.light.HeldLight` approach under Sodium and Iris, settled by prototype P4 in M3.

## Build order

**Build in this order; each milestone uses only what earlier ones shipped.** Content budgets come from the master inventory (about 1,152 ids, plus the review-round additions) and the entity registry (163 entity types on 35 rigs). `studio/inventory.json` tags every id with its milestone and is the truth; the registry check fails a build that registers an id planned for a later milestone. A reading list is all a developer needs.

| Milestone | Needs | Entity types (new rigs) | Plant blocks | Entity texture sets | Ids, about |
| --- | --- | --- | --- | --- | --- |
| M0 Seeded world | — | 0 | 0 | 0 | 15 |
| M1 Water and set pieces | M0 | 0 | 0 | 0 | 20 |
| M2 Walkable valley | M1 | 0 | 0 | 0 | 150 |
| M2.5 Vertical slice | M2 | 4 (3) | 8 | 5 | 55 |
| M3 Start site, fire and onboarding | M2.5 | 0 | 0 | 0 | 80 |
| M4 Body | M3 | 0 | 0 | 0 | 60 |
| M5 Food and goals | M4 | 35 (10) | 72 | 43 | 285 |
| M6 Danger and big game | M5 | 104 (19) | 0 | 119 | 210 |
| M7 The year | M6 | 17 (2) | 27 | 21 | 120 |
| M8 Building and outposts | M7 | 0 | 0 | 0 | 140 |
| M9 Homestead and beyond | M8 | 3 (1) | 0 | 4 | 32 |

### M0 — Seeded world

- **Goal:** prove determinism and the four valley archetypes on a debug map, before anything is drawn in 3D.
- **Already in the repository** (commit 7cdbede): the Gradle project on the pin; `Mulberry32` and `StageHash`; `YoiConfig` (`preset`, `day_length_min`); `/yoi info`; GameTest `boot_rng_matches_reference` (batch `m0`), JUnit `HashVectorsTest`; `CLAUDE.md`, `README.md`.
- **Read:**
  - This tab › M0, Toolchain (verified), Definition of done.
  - Minecraft engine › Platform and project (Version pin, Ruhan's project rules, Libraries, Repository layout, Code rules, Conventions for every tab); Scale and world › Coordinates and height, The nine stages in Minecraft; Saves and data › Data kinds; Testing and the studio rules › Four layers of tests, SMOKE PASSED, CI; Milestones in Minecraft › M0 — Seeded world.
  - Minecraft development › Architecture and the interlace › Package and module map, Determinism and seeding, Conventions settled; World and worldgen › The nine stages, built, Seed variables: how each is computed and stored; Content pipeline, build and the evolving modpack › Two ways data loads, Data kinds, Schemas and validation at load, The content linter, Master registry inventory, Conflicting ids, Names settled, Repository and Gradle layout, Run configurations, Tests in CI, SMOKE PASSED, Config files and defaults, Risks and the first prototypes (B1, B2, B4).
  - Blueprint › Pipeline at a glance; Terrain and hydrology; Biomes and resources › Valley archetypes; Build milestones and agent roles (M0 build spec, studio rules 4–8).
  - Art and audio production › Pipeline and checks › Linter checks; Data and build › Paths, Milestones (M0 row), Tests (`credits_cover_all`).
- **Ids and data:** about 15 ids: block `deep_ground`, one GeckoLib test entity, the 10 Curios slot types, world preset `year_of_ice`, the built-in packs. Data `data/yearofice/worldgen_params/valley.json` (every default and range of the terrain parameter table) and `archetype/<id>.json` for braided, big lake, canyon and marsh. `config/yearofice-common.toml` with the preset switch. `studio/inventory.json` generated by `tools/doc-inventory` from the doc. Tools: `tools/debugmap` (desktop PNG maps with seed and reroll), `tools/seeds`, the art tools `artgen`, `modelgen`, `soundgen` with the palette.
- **Work, in order:**
  1. Data loaders, the kind registry, codecs with snake\_case and unit suffixes, content flags and retired ids (`data` package).
  2. `worldgen.Noise` (simplex fBm with integer-hashed gradients, no trig) and `worldgen.Pipeline` (each stage declares its inputs and the one layer it writes, per 64-block tile) on the `yearofice-gen` worker pool.
  3. Landform for a 2,048 m map at 2 m cells; the archetype draw, 1 in 4, from `hash(seed, "archetype")`.
  4. Erosion: 60,000 droplets, then thermal erosion at 35°.
  5. The debug map at 1 px = 2 m: seed box, reroll, hillshade, slope view, PNG export; `/yoi seed` and `/yoi map`.
  6. Height hash: heights rounded to whole millimetres as int32, FNV-1a over the array; `tests/hashes.json` for 20 fixed seeds.
  7. `./gradlew smoke`, `tools/lint-gen` (no `Math.sin`, `Math.exp`, `Math.pow`, `java.util.Random` or `ThreadLocalRandom` in `worldgen`), CI.
  8. Prototypes B1 (empty-world pack on Ruhan's Mac), B2 (a species added without code), B4 (lint the blueprint's JSON examples).
- **Tests:**
  - JUnit: `worldgen/height_hash_jvms` (20 seeds match `tests/hashes.json` on Linux, Windows and macOS, x64 and ARM); over 200 seeds each archetype appears 25% ±5%; stages 1–3 finish under 8 s; an erosion parameter change leaves the landform hash unchanged; heights within 120–900 m, valley floor 150 ±30 m; no NaN; `HashVectorsTest`.
  - GameTests (batch `m0`): `boot_rng_matches_reference`, `data/all_kinds_load`, `data/reload_atomic`, `data/sync_hash_matches`, `credits_cover_all`.
  - Lint: `lint-gen` and the content linter pass; B4's report lists only the known camelCase keys and old paths.
  - Human gate: Ruhan sees 8 debug maps (2 per archetype) with labels hidden and names the archetype on at least 6.
- **Debug commands:** `/yoi info`, `/yoi seed`, `/yoi map`, `/yoi data report`, `/yoi dump registries`, `/yoi flag set <id> on|off`, `/yoi preset <name>`.
- **Depends on:** nothing.

### M1 — Water and set pieces

- **Goal:** finish the water and the places worth walking to, still on the debug map.
- **Read:** Minecraft engine › Milestones in Minecraft › M1; Scale and world › Rivers, current and water, Set pieces and landmarks. Minecraft development › World and worldgen › Rivers, current and water, Set pieces and Interior structures as templates, Registry table: worldgen, structures and data. Blueprint › Terrain and hydrology; Biomes and resources › Set pieces, Landmarks; Build milestones and agent roles (M1 build spec). Waters and fish › Water bodies as places (The nine types, New spot types). World map › The home valley map, Map layers.
- **Ids and data:** about 20: `setpiece/<id>.json` with templates in `structure/setpiece/<id>/<variant>.nbt` (7 set pieces, 2–3 variants each); fishing-spot quality bands in `worldgen_params/valley.json`; the POI and spot types. Classes `worldgen.CoarseRivers`, `Lakes`, `Rivers`, `Falls`, `FishSpots`, `RockFaces`, `SetPieces`, `Landmarks`.
- **Tests:** JUnit: every river flows downhill to the edge or a lake; the core river's entry and exit match the coarse net within 32 m; each archetype shows its signature; 40–120 fishing spots and at least 20 shelter candidates a map; every region has a landmark visible from 2 km; no set piece in water or on a slope over 25°; the M1 layer hash matches across JVMs. GameTest `worldgen/setpiece_masked`. Human gate: on 4 seeds Ruhan's first fishing pick is in the generator's top 10 spots on 3.
- **Debug commands:** `/yoi map` gains water, spot and set-piece layers; `/yoi layers`.
- **Depends on:** M0.

### M2 — Walkable valley

- **Goal:** walk the valley in first person with the Trail Messenger as the only map.
- **Read:** Minecraft engine › M2; Scale and world › Terrain to blocks, Ice, snow and permafrost, Explore, don't dig, Vanilla switched off, No monsters; Time and sky (all); Look and sound › The look, Sky and aurora, The first-person body; Player systems › HUD layers, The messenger, Controls, Inventory and packs, Weight and carrying, Work, previews and minigames; Saves and data › What is saved where; Testing and the studio rules › Performance budget. Minecraft development › Layer 1: Clock and calendar, Layer 2: Environment field, Layer 8: Messenger and HUD bus, Layer 9: World edits; The save and sync matrix; Vanilla systems we replace; World and worldgen › The generator: YoiChunkGenerator, YoiBiomeSource and the biome list, Terrain to blocks, Snow cover and permafrost blocks, World creation options and presets, Vanilla worldgen switched off, Random ticks and their replacements, Chunk attachments, Catch-up on chunk load, Performance, Risks and first prototypes; Player systems and client › Inventory and slots, Hand-carried loads and carcasses, The HUD layer stack, Messenger screens, Hold-to-work and the use key, Input and keybinds, First-person feel and camera, Rendering, Networking; Saves across versions. Crafting and building in depth › Trees, felling and firewood (felling and bucking). Blueprint › Moment-to-moment mechanics › Controls and interaction, Inventory and backpack; What the experience needs beyond systems › The messenger map; Build milestones and agent roles (M2 build spec). Weather and storms › Monthly climate at 64°N. Art and audio production › The style guide; The texture generator › Blocks.
- **Ids and data:** about 150: terrain blocks (deep ground, deep rock, face rock, soils, surfaces), the trees, biome JSONs in `worldgen/biome/`, the dimension type and world preset, the no-spawns biome modifier, the Trail Messenger item `yearofice:messenger`, the 15 GUI layers registered, the Curios `back` and `neck` slots, key mappings, `data_maps/item/weight.json` and `props.json`, `action/<id>.json`, `clock/rates.json`, the month normals, hourly curve and daylight table of `weather/interior_alaska.json`, `stump`, `fallen_log`, `chopping_block`, the 16 `<wood>_round` and `<wood>_split` items, the `yearofice:look` pack.
- **Tests:** 60 fps at 12 chunks with the horizon on a mid laptop; no hitch over 50 ms crossing chunks. GameTests: load bands within 2%; map, breadcrumbs and waypoints survive a restart; a server killed mid-save leaves a loadable world; under 20 KB of mod data for 1,000 felled trees; a battery lasts 6.5–7.5 game days of typical use and a clear June day recharges it in 2 h ±10%; `clock/rates_exact` on 24- and 72-minute days; `clock/multiplayer_slowest`; `layers/item_expiry`; `no_monsters_full_night`; `worldgen/no_spill`, `no_nodes_below_surface`, `chunk_budget`; `world/no_random_ticks`; `pack/rows_never_fill`, `takeoff_roundtrip`, `no_walkover_pickup`; `worldgen/coal_seams`. Prototypes B3 (a save across versions) and C1 (HUD stack). Human gate: Ruhan walks from the outpost to the horizon landmark and back on breadcrumbs alone and calls movement and the work ring "not clunky".
- **Debug commands:** `/yoi time day <n>`, `/yoi time rate <rate>`, `/yoi tp region <cell>`, `/yoi show current`, `/yoi tree fell|info`, `/yoi layers`, `/yoi pack`.
- **Depends on:** M1.

### M2.5 — Vertical slice

- **Goal:** one playable loop end to end before the full systems: arrive, make fire, catch and cook food, live through the first night.
- **Read (and nothing else):**
  - Ready for development › M2.5.
  - Minecraft engine › Milestones in Minecraft › M2.5 — Vertical slice; World systems › Fire, Traps and nets, Fishing, Spoilage, Animals; Player systems › Work, previews and minigames.
  - Minecraft development › Layer 5: Decay and freshness; Layer 7: Body; Player systems and client › The body tick, Minigames in Minecraft terms, Hold-to-work and the use key; Entities and AI › Models, rigs and animation controllers, The shared state machine, Tiers, spawning and despawning, Carcass hand-off; World and worldgen › Set pieces and Interior structures as templates (the outpost template).
  - Blueprint › Start site (the guided first night and its prompts); Fire, water and light; Camp, materials and builds › Building a campfire; Survival tuning › The body tick, Calories and weight, Warmth, Water; Hunting in depth › Traps (the trail snare), Field care (small game); Fishing in depth › Fish species (Arctic grayling), Tackle, Fishing methods (The fight, as built); Food systems › Cooking, Spoilage model.
  - Bestiary › Carcasses and bodies › Size classes, Dressing by size class. Land animals in depth › Moose, Snowshoe hare. Birds and the air › Grouse and ptarmigan.
  - Plants and fungi › The plant interaction model › Plants as blocks; the rows of the 8 slice species in Mosses, Grasses and sedges, Ferns, horsetails and clubmoss, Shrubs and Berries.
  - Items › Food values (hare, ptarmigan and grayling rows).
  - Art and audio production › Asset budget per milestone (The placeholder rule, Counts per milestone); Prototypes (A1, A2, A3).
- **Ids and data:** about 55, every one tagged `m2_5` in `studio/inventory.json`:
  - World: preset `yearofice:slice_valley`, a 1,024 × 1,024-block cut of one home-valley region's layers with a creek, one fishing spot, spruce and birch, a willow flat and a gravel bar; the outpost template from M1 as the start site.
  - Entities (4 types on 3 rigs): `snowshoe_hare` (`small_rodent`), `willow_ptarmigan` (`grouse`), `moose` (`ungulate`, walks, browses, alerts and flees, not huntable until M6), `carcass_small`; the fixed-slot spawner `ecosystem.population.SlotSpawner`.
  - Plants (8): `feather_moss`, `haircap_moss`, `sphagnum`, `bluejoint`, `horsetail`, `dwarf_birch`, `green_alder`, `bog_blueberry_bush`.
  - Fire: `fire_ring`; birch-bark tinder, kindling and fuel armloads; matches and the ferro rod; states small fire, Low, Steady, Hot and coals; no bow drill yet.
  - Food: the trail snare block entity with dawn and dusk rolls (`food.traps`, `trap/trail_snare.json`); one rod method (hook, line and bobber with worms, the fight overlay) in `hunt.fishing`; roast and boil on the fire ring; `yearofice:fresh` for raw and cooked meat and fish; `decay/meat.json` and `spoil_days_at_5c` in `data_maps/item/food.json`.
  - Body: `sim.body` steps 1–6, 12 and 13 (calories, warmth and water only; no illness or injury; Condition held at 100).
  - Story: `story.Onboarding` with the objective beacon and its 6 prompts.
  - Config: the `[body]`, `[fire]` and `[fishing]` keys of `yearofice-common.toml`.
  - Art and sound at G2: 3 species, the 8 plant blocks with all phases, 78 blocks, 20 icons, 9 HUD layers, 85 sounds, all from the tools.
- **Work, in order:**
  1. Run prototypes A1, A2 and A3; the Director passes each (A1 toward G1: species recognisable at 8 m, antler palms at least 6 px).
  2. Cut the slice valley and place the outpost.
  3. Fire from parts: tinder, kindling, two armloads, lit with matches or the ferro rod.
  4. The body tick's three meters and their HUD layers.
  5. The trail snare and dawn and dusk rolls; field dressing of `carcass_small`.
  6. The rod, the bite roll at the spot and the fight overlay with the close clock.
  7. Cooking and freshness for raw and cooked meat and fish.
  8. The slot spawner and the three animals; the moose's alert and flee.
  9. The guided first night.
- **Tests (GameTests, batch `m2_5`):**
  1. `fire/slice_fire_from_parts`: bark, kindling and 2 armloads reach a fire in under 10 game minutes and coals in 60 ±10.
  2. `food/slice_snare_hare`: a snare on a hare run catches 2–5 hares over 10 game days, rolled at dawn and dusk only.
  3. `hunt/slice_rod_bite`: worms at the spot give a first bite within 30 game minutes in 90% of 200 runs.
  4. `food/slice_cook_meat`: raw hare roasts to cooked in 20 game minutes; cooked meat lasts its `spoil_days_at_5c` at 5 °C ±5%.
  5. `body/slice_day_balance`: a scripted day of chores burns 2,600–3,200 kcal; a fed, dressed player ends it above 70 warmth and 60 water.
  6. `story/slice_first_night`: a bot following the prompts has a fire before dark and sleeps with warmth above 40.
  7. `ecosystem/slice_moose_flees`: the moose flees a player who closes to 40 m and never charges.
  - Plus every M0–M2 test and the four art checks (`artgen_deterministic`, `palette_clean`, `cube_budget`, `sound_levels`).
  - Human gate (fun): Ruhan plays a fresh slice world for 60 real minutes with no help. It passes if he lights a fire from parts before the first dark, eats cooked hare, ptarmigan or fish by day 3, and says he wants to keep playing. Otherwise the next round tunes the slice before M3 starts.
- **Debug commands:** `/yoi set calories|warmth|water <value>`, `/yoi spawn snowshoe_hare|willow_ptarmigan|moose`, `/yoi trap roll`, `/yoi fire state <state>`, `/yoi food info`, `/yoi decay advance <days>`, `/yoi art zoo slice`, `/yoi art plants <id>`, `/yoi sound play <id> <range_m>`.
- **Depends on:** M2. Each part is the first, smaller form of a system that M3 to M6 complete; nothing is thrown away.

### M3 — Start site, fire and onboarding

- **Goal:** a fair start on every seed, every fire build, and a first night a new player survives unaided.
- **Read:** Minecraft engine › M3; World systems › Fire. Minecraft development › World and worldgen › Set pieces and Interior structures as templates; Player systems and client › Minigames in Minecraft terms (bow drill), Other screens; Architecture › Risks and first prototypes (P3, P4); Content pipeline › Saves across versions, Release channels, Risks and the first prototypes (B5). Blueprint › Start site; Fire, water and light; Health, medicine and nutrition (water and boiling only); Crafting and progression; Camp, materials and builds › Gathering materials, Making camp, Building a campfire. Crafting and building in depth › Tiers and gates, Stations, Recipes, Tool durability, Practice. Items › Item properties; Tools and instruments; Containers, light and signals › Light. Every concept from the video › Fire mishaps and slips of the hand, Fire danger, Fire time and the fire roster. Ideas from viewers › Pitch lights, Foxfire and holdover fires. Art and audio production › Music tied to Spirits; Data and build › Code.
- **Ids and data:** about 80: the starting kit (`loot_table/player/starting_kit.json`) and its data maps; the workbench and the `yearofice:station` recipe type; knapping items; water containers; light items and held light (`env.light`, `client.light.HeldLight`); the `sos_return` bar; the outpost template on the real start site; 3 music pieces; the credits screen.
- **Tests:** JUnit: 50 seeds pass placement within 20 rerolls, with the per-check report. GameTests: all M3 tests in Blueprint › Build milestones and agent roles; `data/migrate_fixtures` runs on every merge from here (first beta 0.3.0). Prototypes B5 (release dry run) and P4 (held light). Human gate: a first-time tester lights a fire before the first night without help, and after any failed fire can say why from the Trail Messenger's line.
- **Debug commands:** `/yoi fire state|fuel`, `/yoi gate open <tier>`, `/yoi seed` (with the start-site check report), `/yoi time rate <rate>`.
- **Depends on:** M2.5.

### M4 — Body

- **Goal:** the whole body model, collapse and the Endless SOS, with every vital change explained.
- **Read:** Body, injury and sickness (all). Blueprint › Survival tuning (all subsections); Moment-to-moment mechanics › Clothing, Health, medicine and nutrition, Spirits. Minecraft engine › M4; Player systems › Vanilla needs replaced, The body attachment, Clothing slots. Minecraft development › Layer 7: Body; Player systems and client › The body attachment, The body tick, Vanilla needs replaced, Collapse, SOS and the flight, The vitals strip and the body figure. Every concept from the video › Stumbles, trips and falls, Exposure time, Rest days, Your own body. Ideas from viewers › Sick stomach, Cauterizing and grime, The pocket stone. Items › Medicine, hygiene and cooking › Medicine and hygiene.
- **Ids and data:** about 60: the `yearofice:body` attachment at schema 2 (`part_hp`, `d_sys`, `part_frost` × 10, `lost_digits`, `crooked`, `injury_seq`), sync `body_sync` and `body_public`; data kinds `body_part/` (10), `injury/` (13), `injury_source/` (29), `illness/` (20), `action/treat_*` (17), `remedy/`, `preset/*.json`, `body/standard.json`; 25 damage types; `left_pack`; the starting clothes and Curios clothing slots; `story.Journal`.
- **Tests:** JUnit: BMI 17 between day 15 and 21 on Standard, about twice as late on Gentle; BMR 1,794 ±1; scurvy on day 30 or 31; `BodyModelTest.bmr_scaling`, `order_deterministic`. GameTests (batch `m4`): Body tab tests 1–7 (from `body/part_split_exact`), `body/vanilla_needs_held`, `fall_routes`, `sos_jump`, `hardcore_end`, `ground_sleep`, `nausea_decay`, `cauterize_stops_bleeding`, `pocket_stone_fade`, `rest_heals_double`, `own_body_sos`, `net/body_sync_rate`. Prototype C3 (body tick at 20 TPS, 1 and 8 players). Human gate: Ruhan plays 2 game days with only the crate kit and can say what caused every warning.
- **Debug commands:** `/yoi body show|part|injure|problem|treat|ill|digits|heal|roll`, `/yoi set calories|bmi|warmth|water|spirits`, `/yoi spirits day`, `/yoi preset`.
- **Depends on:** M3.

### M5 — Food and goals

- **Goal:** every food source of the first months, with freshness, traps, fishing, foraging and the garden, kept honest by a scripted player.
- **Read:** Blueprint › Food systems (all); Crop farming; Moment-to-moment mechanics › Foraging and plant knowledge; Hunting in depth › Traps; Fishing in depth (all); Animals in depth › Populations. Decay and time › One decay model, Cold, snow and the thaw, Eating decayed food, The decay table (Food, Carcasses and gut piles, Plant materials), Data and build. Items › Food values, Containers, Item decay, Theft by animals. Plants and fungi (all). Land and seasons in play › Crop farming in play. Waters and fish (all). Water life in depth › Fish as living mobs, Mob sheets, New water life (the M5 rows). Bestiary › Species sheet; Carcasses and bodies. Land animals in depth › Mob sheets: small game and furbearers. Birds and the air › Grouse and ptarmigan. Every concept from the video › Food, Squirrels, Trap sounds, Snare funnels, UI. Ideas from viewers › Berries, Foraging and the camp kitchen, Hunting and fishing. Minecraft engine › M5. Minecraft development › Layer 4: Ecology, Layer 5: Decay and freshness, Layer 6: Item model, Layer 9: World edits; Entities and AI › Tiers, spawning and despawning, Carcass hand-off, First prototypes; Player systems and client › Minigames in Minecraft terms.
- **Ids and data:** about 285: 35 entity types on 10 new rigs (small game, small mustelids, grouse and ptarmigan, resident fish, schools, wood frog, yellowjacket, carcasses tiny to medium); 72 plant blocks with harvest verbs; trap block entities; rods, tackle and passive fishing blocks; smoke and drying racks; the food cache; garden beds, crop blocks, seeds and the tools `hoe`, `digging_stick`, `shovel`, `hand_quern`; `dish` with `dish_id` and `nutrients`; the 10 named food items; data `fish/`, `plant/`, `crop/`, `prompt/`, `item_family/`.
- **Tests:** a scripted good player keeps BMI above 19 through day 30; over 100 seeds packet barley sown in the window comes up 50–65%, and below 5 °C soil under 25%; every batch `m5` test in Plants and fungi, Waters and fish, Water life, Ideas from viewers, Every concept from the video and Land; a gill net never rolls with 3 segments; the cache loses 2–3% a week to mice, none when ringed; `world/item_lifespan`; `data/data_only_species_spawns`; prototype C2 (the fishing fight); E1–E5 built here on test species. Human gate: Ruhan plays the first game week for an hour and calls it tense, not tedious.
- **Debug commands:** `/yoi food info|age`, `/yoi give food <id> <grams>`, `/yoi decay info|advance|age|climate`, `/yoi trap roll`, `/yoi plant info|phase|patch|identify|year`, `/yoi crop info|germ|grow|frost|ripen`, `/yoi spawn <species>`, `/yoi animal`, plus two to add: `/yoi fish spot <pos>` (stock, q, last catch) and `/yoi fish fight <species> <kg>`.
- **Depends on:** M4.

### M6 — Danger and big game

- **Goal:** animals that act from needs and senses, big game the player earns, and danger the player can read.
- **Read:** Blueprint › Mechanics check: hunting; Animals in depth (Drives and daily schedules, Senses by species, Who hunts whom, Tracks and sign); Hunting in depth (Weapons and arrowheads, The hit-zone model, Setting up the hunt, Field care); Food systems › Butchering and yields; Moment-to-moment mechanics › Danger in detail. Bestiary (all). Land animals in depth (all). Birds and the air (the M6 sheets). Water, calls and encounters › Water draws animals, Calling animals, Animal encounters. Body, injury and sickness › Injury sources, Limping and the walking stick. Every concept from the video › Hooking a bear, Bears in water, Bears and tree stands, Wounds that last. Ideas from viewers › Camouflage, Warmth from a fresh kill, Game drag and hauling before snow, Bears as viewers asked, Trap catch shock. Minecraft engine › M6; World systems › Animals, Senses, scent and wind. Minecraft development › Layer 3: Scent and sound field; Entities and AI (all).
- **Ids and data:** about 210: 104 entity types on 19 new rigs (big game, predators, most birds, `carcass_large`, `carcass_huge`, and the two deer on the `cervid` rig); `data/yearofice/hitzone/<id>.json`; the arrow projectile; bows; calls and blinds; `loot_table/butchering/<id>.json`; the `distant_sound` payload; the deer behind the content flag `yearofice:deer` (on in dev and beta, off in stable), with `rare_sighting/mule_deer_straggler.json` (Land animals in depth › Deer).
- **Tests:** a bear reaches an uncleared fish-cleaning spot within 2 game days on 7 of 10 seeds; no predator chases more than 150 m; the senses GameTests in a flat template with set wind (`senses/*`); the population tests in the JUnit sim; `ecology/*`; Body test 8; every batch `m6` test. Human gate: Ruhan takes a first moose using only sign, wind and the blood trail.
- **Debug commands:** `/yoi spawn`, `/yoi animal`, `/yoi hunt aim|shot`, `/yoi carcass <target> info`, `/yoi herd`, `/yoi flock`, `/yoi haze`, `/yoi kin`, `/yoi show scent|tiers`, `/yoi deer straggler|info`.
- **Depends on:** M5.

### M7 — The year

- **Goal:** seasons, freeze-up, snow, ice, storms and migrations over three simulated years.
- **Read:** Weather and storms (all). Land and seasons in play (all but Crop farming in play). Blueprint › Calendar, daylight and weather; Moment-to-moment mechanics › Clothing (winter gear). Open world in depth › Rare events. Waters and fish › Aquatic events. Water life in depth › Life under the ice. Birds and the air › Migration and the dawn chorus. Decay and time › Cold, snow and the thaw. Water, calls and encounters › Water reshapes the land. Every concept from the video › Environment (all). Minecraft engine › M7; Time and sky; World systems › Weather. Minecraft development › World and worldgen › Snow cover and permafrost blocks, The terrain-change queue in full, Catch-up on chunk load. Art and audio production › Seasonal variants by colour remap.
- **Ids and data:** about 120: 17 entity types on 2 new rigs (lemmings, singing vole, bat, toad, four salmon and their carcass, lamprey, late fish and insects); 27 fungi and aquatic plant blocks, lily pads among them; `glaze_layer`, `hail_layer`, `fallen_limb`; payload `yearofice:strike`; SavedData `yearofice_weather`; `weather/storm/`, `weather/ring/`, `weather/forecast.json`, `event/decade_storm.json`; the other 9 music pieces.
- **Tests:** three simulated years back to back; river ice forms late October to mid November every year; over 100 simulated years each rare event lands within 5 points of its chance; `weather/table_replay`; `world/catchup_equals_live`, `catchup_order_free`, `catchup_budget`, `snow_pass_classes`, `flood_returns`; `story/sky_lore`, `build/quinzhee`, `story/mist_count`, `fishing/tipup_buried`; an ice hole skins over within an hour below −20 °C; fur mittens are safe at −25 °C, not −26 °C. Prototypes W4 and W7. Human gate: a December week on a debug save; winter is dangerous but fair.
- **Debug commands:** `/yoi weather show|forecast|storm|blizzard|temp|glaze|strike|decade|table|sim`, `/yoi ice set <cm>`, `/yoi land read|sky|micro|phenomenon|stand|slide|heave|hazard`, `/yoi migration wave`, `/yoi migrate status`, `/yoi lemming set`, `/yoi insects`, `/yoi chorus`.
- **Depends on:** M6.

### M8 — Building and outposts

- **Goal:** pieces, rooms, the heat model and outposts, tested in winter.
- **Read:** Structures and buildables (all). Crafting and building in depth › Building pieces, Placing and snapping, Warm rooms, Queued jobs, Trees, felling and firewood. Blueprint › Shelter and building; Camp, materials and builds › Building a cabin, The smoker. Minecraft engine › Building (all); M8. Decay and time › Wood, Snow structures, Maintenance loops under the tedium rule. Every concept from the video › Dugouts and rock shelters, Dugout hearth and chimney, Firewood drying, Dressed stone, Camp finishing pieces, Placed lanterns, Raised cache legs. Open world in depth › Outposts, Traversal. Minecraft development › Layer 9: World edits; World and worldgen › Every other world edit.
- **Ids and data:** about 140: piece blocks and frames, `slab_floor`, `path` with `surface`, `log_deck`, `awning`, `dressed_stone`; rooms SavedData; stoves, chimneys and creosote; storage block entities; smokehouse, root cellar, permafrost cellar, dugouts; the raft entity; `data_maps/block/heat.json`, `piece.json`, `decay.json`; `multiblock/` data.
- **Tests:** a finished cabin with a stove holds feels-like above 5 °C on a −30 °C night; stamping a saved cabin takes under 10% of the first build's player time (timed by a bot); `build/dugout_never_yields`, `dugout_heat`, `slab_floor_u`, `path_speed`; `fire/moisture_drying`. Human gate: Ruhan repairs the outpost and lays one wall course without help.
- **Debug commands:** `/yoi struct check|age`, `/yoi show rooms`, `/yoi wood dry`, `/yoi tree mishap`, plus one to add: `/yoi room info` (volume, U, heat balance).
- **Depends on:** M7, since rooms are tested in winter and roofs carry snow.

### M9 — Homestead and beyond

- **Goal:** the iron tier, the wolf and its team, the radio, livestock and a world that answers years of play, across all 16 regions.
- **Read:** Open world in depth › Outposts, Taming a wolf, Ambitions, Story, The radio. Ideas from viewers › The old camp, Raising animals, Taming a wolf without bones. Structures and buildables › Water power, crossings and craft. Crafting and building in depth › Tiers and gates (Iron, Homestead). Blueprint › The open world; Crop farming (the stovehouse). Decay and time › Decay over the long term. Creative concepts › 11. The fur trade, 15. The valley remembers. Items › Keepsakes and story items. World map › Three rings, World size in Minecraft. Minecraft engine › M9. Minecraft development › Taming, livestock and raised young; Legends and named animals; World and worldgen › The dev border.
- **Ids and data:** about 32: `goat`, `reindeer`, `hen` (the `hen` rig); furnace, forge and the iron tier; the canoe entity; the sled team; the radio and supply drops; the stovehouse; the fish wheel; `companion.wolf_leaves`.
- **Tests:** a den pup reaches Companion in 30–60 game days of steady feeding; five simulated years with an active homestead keep mod save data under 5 MB and MSPT inside the budget; the wolf-leaving test (1,000 Februaries at 60 trust: Standard loses a wolf in 70–85% of runs and gets it back in all; Gentle loses none); `world/regrowth_schedule`, `beaver_pond_rise`, `wildfire_escalation`; `old_camp_10y`; `forage/coppice_shoots`, `browse_exclosure`, `goat_toxic`; `fire/controlled_burn`. Human gate: an hour of year 2; Ruhan names a project he wants next without a prompt.
- **Debug commands:** `/yoi wolf leave <test|now>`, `/yoi wolf trust <0-100>`, `/yoi tp region`, plus two to add: `/yoi radio sked|drop` and `/yoi sos stayout <days|years>`.
- **Depends on:** M8. The Interior and Beyond follow as M10 and M11 (World map); test worlds keep the dev border at the home valley's edge until then.

## Toolchain (verified)

**The cloud built a mod on exactly these versions on 2026-10-19.** The first build took about 5 minutes; later builds use the Gradle cache.

| Part | Version |
| --- | --- |
| Minecraft | 1.21.1, Java Edition |
| NeoForge | 21.1.264 |
| ModDevGradle | 2.0.148 |
| Gradle | 8.14.3, wrapper committed |
| Java | 21, set as the Gradle toolchain |
| GeckoLib | 4.9.3 |
| Curios API | 9.5.1+1.21.1 |

**Repository:** ruhanu155/years-of-survival on GitHub. Pinned versions live in `gradle.properties`; change them only in an upgrade round. Branches are `dev/<department>/<topic>` with a pull request into `main`, and `main` always passes.

**Ruhan's Mac:** the folder "Game Development/YearOfIce" is the local copy for running the client. With Homebrew Java 21: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew runClient`.

| Command | Does |
| --- | --- |
| `./gradlew build` | Compiles, runs JUnit, builds the jar |
| `./gradlew runGameTestServer` | Headless server running every GameTest; must end with "All N required tests passed" |
| `./gradlew runClient` | The dev client, on a machine with a display |
| `./gradlew runData` | Datagen into `src/generated/resources` |
| `./gradlew smoke` | Everything SMOKE PASSED needs |

**Two rules.** Automation never accepts the Minecraft EULA on Ruhan's behalf: it uses the GameTest server, not `runServer`. The build needs the NeoForge, Maven Central, Gradle, Mojang, GeckoLib and Curios hosts, which the cloud reached on 2026-10-19.

## Definition of done

**A milestone is done when all of these hold on one frozen build.**

1. `./gradlew smoke` prints SMOKE PASSED: build, JUnit, every GameTest of this and earlier milestones, a dedicated-server boot and a client boot on seed 3141592, and the no-monsters check.
2. GameTests are green: all required tests of the milestone's batch pass, with no test in quarantine for over 7 days.
3. From M3 on, the save migration test `data/migrate_fixtures` passes on every fixture save from every released version.
4. The client smoke's screenshots (day, night, a danger moment, a blizzard, the map and the pack screen, at GUI scales 2 and 4) were looked at by the Director, not only the test result.
5. Ruhan passed the milestone's human gate himself.
6. Docs are updated: CHANGELOG, ROADMAP, DEVGUIDE and PLAYTEST in the Games project, the milestone's ids in `studio/inventory.json`, its `/yoi` commands and `yearofice-common.toml` keys, and `credits.json`.

## Known risks

**Each risk has a prototype that retires it, run before the content that depends on it.** P numbers are the lead's; W the world developer's; B the build developer's; A the art tab's. The entity and client developers' prototypes are listed as E and C to avoid clashing numbers. The lead's P1–P7 had no milestone; the When column sets one here, before the content that needs it.

| Id | Risk | Prototype and pass | When |
| --- | --- | --- | --- |
| P1 | Catch-up hitches on chunk load | 64 chunks after a year away, each under 2 ms | M2 framework, M7 content |
| P2 | Lazy decay drifts from ticked decay | 10,000 random histories within one band of a ticked reference (JUnit) | M2.5 |
| P3 | Rate arbitration feels wrong | 30-minute playtest with the rate shown; two-player LAN test | M3 |
| P4 | Held light under Sodium | Mixin and Sodium spike in the client smoke | M3 |
| P5 | Environment sampling cost | JMH of 100,000 samples; 40 sensing animals | M2 |
| P6 | Records split on crashes | Kill the server at 20 points; count individuals | M5 |
| P7 | Event order between systems | Shuffled listeners give the same game day | M2 |
| W1 | A 1,968-block world | Core at 1,968 and 384: heap, chunk time, fps | Week 1 of M2 |
| W2 | Proto-chunk attachments | Written in `fillFromNoise`, read on load | M2, half a day |
| W3 | Block writes during chunk load | 64 ops and a snow pass on 441 chunks, no desync | M2 |
| W4 | Catch-up after a long absence | `catchup_budget` with the load gate | Before M7 content |
| W5 | Water spilling at chunk borders | `worldgen/no_spill` on all four archetypes | M2 |
| W6 | Pruning edits region files | 10,000 chunks pruned and restored, killed mid-write | M10 |
| W7 | Sodium and Iris with season colours | The season re-mesh under both in the client smoke | M7 |
| W8 | Determinism drift between JVMs | The hash test nightly on all platforms | From M0 |
| W9 | The edit queue grows | 10-year sim; queue under 2 MB | M7 |
| W10 | Another mod's worldgen data | A structure mod loaded; none generate | M2 |
| B1 | The pack does not boot | Empty-world pack on Ruhan's Mac and a clean server | M0 |
| B2 | Content needs code | A species from data, model, sounds and lang only | End of M0 |
| B3 | Saves break across versions | A 0.2.0 save opens in 0.2.1 | M2 |
| B4 | The blueprint's JSON breaks the linter | Every JSON example linted | M0 |
| B5 | Pack install fails | `v0.3.0-beta.0` installs on the Mac; server pack boots | M3 |
| A1 | Generated animals do not read | Slice animals from templates; Director names the silhouettes | Before M2.5 |
| A2 | Blocks look like vanilla taiga | 20 spruce-flat textures at most 70% of vanilla chroma | Before M2.5 |
| A3 | Synthesized sound grates | 31 steps and 5 wind loops; snow bands ordered twice | Before M2.5 |
| E1–E5 | Moose pathing, flocks, salmon runs, record crashes, one body per kill | Entities and AI › First prototypes P1–P5 | M5, before M6 content |
| C1–C4 | HUD stack, fight screen, body tick cost, held light | Player systems and client › P1–P4 | M2, M5, M4, M3 |

Two other risks stay on watch: the spec's size, which the reading lists contain, and Minecraft's EULA and brand rules, which apply even to a personal pack.

## Ready-for-development checklist

- [x] Every core loop has an owning tab, an audit result and a status (Status board).
- [x] All 322 Forge Labs rows are Ready, none open.
- [x] Every Ruhan decision is made and dated; none is open.
- [x] Director rulings are recorded in the tabs they change.
- [x] Each reviewer applied the RECONCILE items in their tabs; the final pass applied 140, 146, 148 and 150.
- [x] The sweep left no camelCase JSON key, no live `zone_frost` or bare `berry_bush` id, and no 10- or 30-year stay-out.
- [x] The toolchain is pinned and a mod built on it in the cloud (2026-10-19).
- [x] The repository exists with the M0 scaffold (ruhanu155/years-of-survival).
- [x] Each milestone has a goal, a reading list, ids, tests, debug commands and dependencies.
- [x] Definition of done is written.
- [x] Every known risk names its prototype.
- [ ] Copy this tab into the repository as `docs/ready-for-development.md`, where `README.md` and `CLAUDE.md` point. Owner: Studio lead, in the first M0 round.
- [ ] Generate `studio/inventory.json` from the doc with `tools/doc-inventory`, including the review-round additions. Owner: build developer, M0.
- [ ] Add the five missing debug commands (`/yoi fish spot`, `/yoi fish fight`, `/yoi room info`, `/yoi radio`, `/yoi sos stayout`) to their tabs' debug tables. Owners: Food and hunting (M5), Building and crafting (M8), World and story (M9), Body and survival (M9).
- [ ] Run A1–A3 and reach G1 for the slice's three animals. Owner: Look, sound and UI, before M2.5 ships.
- [ ] Bring Ruhan the M0 human gate (8 debug maps). Owner: Director.
