# Year of Ice: mods and libraries worth reusing

Checked 2026-10-19 against the Modrinth API (`/v2/project`, `/v2/project/{slug}/version` filtered to `loaders=neoforge`, `game_versions=1.21.1`) and each repo's LICENSE file, fetched from raw.githubusercontent.com. CurseForge was used where a mod is not on Modrinth. "Last release" means the newest file on any version. The date in the NeoForge column is the newest NeoForge 1.21.1 build.

**Licence rule used here.** Year of Ice is for personal use, so GPL, LGPL, EUPL, MPL, MIT, BSD, Apache, CC-BY, CC-BY-SA and CC-BY-NC are all usable. Copyleft (GPL, LGPL, EUPL, MPL, CC-BY-SA, CC-BY-NC-SA) only adds obligations if Ruhan distributes the game: then the derived code must be released under the same licence. All Rights Reserved (ARR), ND and no-licence material is **reference only**.

**Design rule that changes many verdicts.** DV5 › Other mods already blocks mods that replace the inventory screen, the hunger and health model, or time and weather. It also says the pack never ships a mod that adds blocks, items, mobs or recipes. So most gameplay mods can only be **Adapt code** or **Reference only**, never **Depend**. The only real dependencies are libraries and client add-ons.

**Where the licence sources disagree**, this file follows the repo LICENSE and flags the mismatch:
- Ecliptic Seasons: the repo says BSD-3 for code and CC BY-NC-SA 4.0 for resources, but Modrinth lists ARR.
- Alex's Mobs Continued: the repo says LGPL-3.0, but Modrinth lists GPL-3.0.
- Thirst Was Taken, Legendary Survival Overhaul and Weather2: the repo LICENSE is only the Forge MDK boilerplate, so their Modrinth or CurseForge declaration governs.

## 1. Body and survival (Body layer, `body`, Environment field heat index)

| mod | what it gives | licence (code / assets) | NeoForge 1.21.1? | last release | verdict | why |
| --- | --- | --- | --- | --- | --- | --- |
| Tough As Nails (Glitchfiend) | Temperature from biome, altitude and nearby blocks; thirst with water purity; HUD | ARR / ARR (repo LICENSE "All rights reserved") | Yes, 10.1.0.13, 2024-10-22 (no 1.21.1 update since) | 2026-10-17 (26.x) | Reference only | ARR. Its design informs the Body ticker and heat index. As a dependency it would be blocked anyway, because it replaces needs |
| Cold Sweat (Momo-Softworks) | Full temperature model: world temp from biome, depth, time and season; block heat sources scanned around the player; insulation per armour piece; hearth that warms a room | GPL-3.0-or-later, plus a section 7 exception: projects that only call its API stay outside the GPL / GPL-3.0 (copying its assets brings the GPL with them) | Yes, 2.4.4, 2026-10-16 | 2026-10-16 | Adapt code | The most complete open temperature engine. Its block-source scan and insulation sums map onto the Environment field (`env` heat index, micro-climate) and the room heat model in `build`. Copying code makes those files GPL-3 |
| Homeostatic (wendall911) | Temperature, wetness, thirst and water purity; light code | MIT / MIT (repo LICENSE) | Yes, 1.21.1-2.12.0.0, 2026-06-26 | 2026-09-30 | Adapt code | A lighter take on the same systems, under MIT, so it can be copied with no copyleft. Good for wetness and the purity rules in `body` |
| First Aid (ichttt) | Per-limb health: head, body, arms, legs, feet; damage distribution per source; bandages, splints | GPL-3.0 / GPL-3.0 | No (newest is 1.20.1, 2024-07-14, per CurseForge) | 2024-07-14 | Reference only | Dead on 1.21.1. Use the fork below |
| First Aid New (maoruiQa) | Fork of First Aid, ported forward | GPL-3.0-only / GPL-3.0 (repo LICENSE) | Yes, 1.3.4, 2026-10-11 | 2026-10-11 | Adapt code | Its damage-source-to-limb distribution tables and the `LivingIncomingDamageEvent` routing match Body › injuries and the player hit zones. Copied files become GPL-3 |
| Legendary Survival Overhaul | Temperature, thirst, per-body-part health, seasons hooks | ARR (Modrinth); repo LICENSE is only the Forge MDK boilerplate / ARR | Yes, 2.4.7.2, 2026-08-09 | 2026-08-09 | Reference only | No usable licence. Worth a look for how it combines all three systems in one HUD |
| Survive | Temperature, thirst, energy | CC-BY-NC-ND-4.0, no public source / same | Yes, 10.2.0, 2026-08-07 | 2026-08-07 | Reference only | ND forbids derivatives, and there is no source |
| Thirst Was Taken (ghen-git) | Thirst, purity levels, drinking from water blocks, canteens | MIT on Modrinth; repo LICENSE is only the Forge MDK boilerplate / MIT on Modrinth | Yes, 2.1.5, 2025-09-21 | 2025-09-21 | Reference only | Stalled for a year, and the licence is declared only on Modrinth. Use Thirst Was Taken 2 or Homeostatic for code |
| Thirst Was Taken 2 (n1ght3r) | Active GPL rewrite of the same | GPL-3.0-only / GPL-3.0 | Yes, 1.9.0, 2026-10-15 | 2026-10-15 | Reference only | Our water model (bottle, boiling, illness) is already specified. Read its purity-to-illness tuning only |

## 2. Seasons and weather (Clock and calendar, `story` weather chain, `env.phenomena`, `world.pass.SnowPass`, `client.season`)

| mod | what it gives | licence (code / assets) | NeoForge 1.21.1? | last release | verdict | why |
| --- | --- | --- | --- | --- | --- | --- |
| Serene Seasons (Glitchfiend) | Seasonal cycle, foliage tints, crop fertility by season | ARR / ARR (repo LICENSE) | Yes, 10.1.0.9, 2026-09-05 | 2026-09-21 | Reference only | ARR, and it drives vanilla time and weather, which DV5 blocks. Its tint approach is already mirrored in `client.season.SeasonState` |
| Ecliptic Seasons (TeamTeaMC) | 24 solar terms; snow that builds up and melts on terrain; seasonal colour; crop rules; works with Sodium | BSD-3 code / CC BY-NC-SA 4.0 resources (repo LICENSE on `master`); Modrinth says ARR | Yes, 0.15.4.1, 2026-10-16 | 2026-10-16 | Adapt code (with care) | The closest open match to "the year changes the map": snow build-up and melt as a block pass, plus season tints that survive Sodium. Maps to `SnowModel`/`SnowPass` and the palette re-mesh. The README says "please do not port arbitrarily", so ask before reusing large parts. Resources are non-commercial share-alike |
| Weather Storms & Tornadoes / Weather2 (Corosus) | Wind, storm fronts, tornadoes, sandstorms, volumetric clouds | ARR (CurseForge); repo Corosauce/weather2 has only the Forge MDK LICENSE / ARR | Yes, 2.8.7, 2026-05-24; needs CoroUtil (ARR) | 2026-05-24 | Reference only | No usable licence, and it controls weather, which the design forbids (the year table owns it). Study its wind field and front movement for `env.phenomena` |
| Primal Winter (alcatrazEscapee) | Permanent winter: snow and ice everywhere, frozen biomes | MIT / MIT (repo LICENSE) | Yes, 6.0.2, 2024-10-14 | 2024-10-14 | Adapt code | Unmaintained for two years, but MIT and small. Its biome-modifier and "snow under everything" placement tricks help `worldgen` snow cover and permafrost blocks |
| Snow! Real Magic! (Snownee), extra find | Snow layers that sit on plants, slabs and fences without deleting them; snow builds up and melts; snowy textures for many blocks | MIT / MIT (repo LICENSE) | Yes, 12.2.3, 2026-09-27 | 2026-09-29 | Adapt code | Solves the hardest part of `snow_cover`: snow over grass, saplings and fences while keeping the block under it. MIT, active, clean |
| PMWeatherAPI, extra find | API for PMWeather storms | GPL-3.0-only | Yes, 0.16.4.1, 2026-02-26 | 2026-02-26 | Avoid | It is an API for another weather mod. Nothing for us |

## 3. Trees and wood (World edits, `yearofice:trees` records, felling, `StumpPass`, Regrowth, `worldgen` tree builders)

| mod | what it gives | licence (code / assets) | NeoForge 1.21.1? | last release | verdict | why |
| --- | --- | --- | --- | --- | --- | --- |
| FallingTree (RakambdaOrg) | Breaks a whole tree when you cut one log; tree detection by flood fill; leaf handling | LGPL-3.0-only / LGPL-3.0 | Yes, 1.21.1.11, 2025-08-01 | 2026-10-10 (26.x) | Reference only | We already keep tree records, so the felling code knows the tree without searching for it. Instant whole-tree felling also skips our felling work. Its leaf clean-up is a useful check against our `FALL` op |
| HT's TreeChop (hammertater), extra find | Chop counts per tree: the trunk visibly thins with each chop, then the tree falls; the count scales with tool and tree size | MIT / MIT (repo LICENSE) | Yes, 0.19.3, 2025-03-02 | 2025-03-03 | Adapt code | Fits "respect time": felling is visible work that gets faster with better tools and practice. Its "chopped log" blockstate is a cheap way to show progress. Stale but MIT |
| Dynamic Trees | Trees that grow branch by branch, drop seeds and spread; procedural trunks | MIT / MIT (repo LICENSE) | Yes, 1.7.2, 2026-06-10 | 2026-07-11 | Reference only | It replaces every tree with its own block types, which fights our tree builders and the placed mask. Its growth model (soil life, light, years to mature) informs Regrowth and "grow without end" |
| TerraFirmaCraft wood code | Per-species logs, stumps, saplings that take days to grow, tree generation from templates | EUPL-1.2 / EUPL-1.2 (sounds CC0 unless noted; README) | Yes, 4.2.12, 2026-10-14 | 2026-10-14 | Adapt code | The sapling growth timer counts calendar days and has a catch-up step, much like our `REGROW` and `ClockEvent.Day`. EUPL is copyleft |
| No Tree Punching (alcatrazEscapee) | No punching logs; flint knapping and tools; the mattock; splitting logs into planks with an axe | MIT / MIT (repo LICENSE) | No (newest 1.20.1, 2023-06-21) | 2023-06-21 | Adapt code | MIT and small. The "you need a tool for this" gating and the axe-on-log splitting map to `forage` harvest verbs and the first stations. It would need porting to 1.21.1 |
| No Tree Punching (no-log-punch, different author) | Port of the same idea | MIT; needs flib | Yes, 2.0.1, 2026-08-06 | 2026-08-26 | Reference only | Thin rewrite; the original has the better code |
| No Tree Punching Remastered | Same idea, rewritten | GPL-3.0-only | Yes, 1.1, 2026-06-18 | 2026-06-18 | Reference only | Nothing the original lacks |

## 4. Primitive crafting, food decay and nutrition (Decay and freshness, `food`, `item.QualityRoller`, `build` stations)

| mod | what it gives | licence (code / assets) | NeoForge 1.21.1? | last release | verdict | why |
| --- | --- | --- | --- | --- | --- | --- |
| TerraFirmaCraft (decay, heat, nutrition, knapping) | Food decay stored as a creation date on the item stack, with traits (salted, smoked, dried, preserved) that scale the rate; item heat with cooling over time; five nutrient groups that cap max health; the knapping screen | EUPL-1.2 / EUPL-1.2 (FastNoiseLite MIT; some sounds CC-BY-4.0) | Yes, 4.2.12, 2026-10-14; requires Patchouli | 2026-10-14 | Adapt code | The best open match for the Decay and freshness layer. A creation date plus a trait multiplier gives the closed-form decay our catch-up needs, and stacks merge by rounding dates. Its heat capability maps to cooking and the forge. The knapping grid fits the first flint step. EUPL-1.2 is copyleft and GPL-compatible. Never depend on it: it replaces worldgen and the whole game |
| Farmer's Delight (vectorwing) | Cooking pot, skillet, cutting board, stove; meals that give buffs | MIT / MIT (repo LICENSE covers the whole repo) | Yes, 1.3.4, 2026-08-29 | 2026-08-29 | Reference only | It adds items and recipes, which the pack rule forbids. Its cooking-pot block entity (bowl slot, heat source below, progress) is a tidy reference for `build` stations |
| Spice of Life: Onion (CreativeMD) | Foods give less as you repeat them; a food history | LGPL-3.0-only / LGPL-3.0 | Yes, 1.5.7, 2026-10-08 | 2026-10-08 | Reference only | We have a stomach and kcal model already. The diminishing-returns curve is worth reading if food variety is tuned later |
| Diet (TheIllusiveC4) | Five food groups worked out from recipe ingredients; effects by group balance | LGPL-3.0-or-later / same | No (newest 1.20.1, 2023-10-24) | 2023-10-24 | Reference only | Dead on NeoForge. Deriving groups from recipe ingredients is a clever trick for data content |
| Nutritional Balance (dannydjdk), extra find | Nutrient groups with deficiency and excess effects | GPL-3.0-or-later / same | Yes, 7.0.3, 2026-04-25 | 2026-09-26 | Reference only | Only if the design adds nutrients beyond kcal |
| Hardcore Torches, extra find | Torches and lanterns burn out, need fuel, and go out in rain | CC0-1.0 / CC0 | Yes, 2.7, 2026-09-17 | 2026-09-17 | Adapt code | Public domain. The burn-time and relight rules fit `fire.light` items (birch-bark torch, pitch torch, tin lantern) |

## 5. Carrying and hauling (`item.inventory.CarryHandler`, `carry` attachment, `carry_slot`)

| mod | what it gives | licence (code / assets) | NeoForge 1.21.1? | last release | verdict | why |
| --- | --- | --- | --- | --- | --- | --- |
| Carry On (Tschipp) | Pick up blocks (with their block-entity data) and entities, carried in front with arm poses, in first and third person | LGPL-3.0-only / LGPL-3.0 | Yes, 2.2.6, 2026-07-26 | 2026-09-27 | Adapt code | Its block-entity save and restore when picking up, plus the carried-pose arm rendering, map onto the `carry` attachment and the `carried_arms` pose. As a dependency it would bypass load bands and the 45 kg rule, so copy code only. LGPL copied files stay LGPL |
| MT Inventory Weight, extra find | Inventory weight and over-encumbrance | CC0-1.0 | No NeoForge 1.21.1 build | 2026-06-13 | Reference only | `LoadCalculator` is already specified |

## 6. Fishing and animals (`hunt.fishing`, `ecosystem.fish`, `ecosystem.species`, Brain parts, populations)

| mod | what it gives | licence (code / assets) | NeoForge 1.21.1? | last release | verdict | why |
| --- | --- | --- | --- | --- | --- | --- |
| Aquaculture 2 | Rods with parts, bait, many fish species, fish by biome | ARR; the page says "cannot decompile the code or reuse the artwork" / ARR | Yes, 2.7.21, 2026-04-09 | 2026-06-03 | Reference only | Explicitly closed. Its rod-parts idea is a design reference only |
| Tide 2 (Lightning-64), extra find | Fishing minigame, a fish journal, bait, catches by depth, biome, weather and time of day, fish that show on the line | MPL-2.0 / MPL-2.0 (repo LICENSE.txt) | Yes, 2.1.1, 2026-08-03; needs Cloth Config | 2026-08-03 | Adapt code | The best open fishing-fight code. Its catch conditions map to Fish stocks, and the minigame maps to the fight. MPL copyleft applies only to the files you copy |
| Fishing Real, extra find | Caught fish arrive as live, flopping entities, not items | MIT / MIT | Yes, 1.9.1, 2025-04-26 | 2026-05-28 | Reference only | We land living fish already (`ecosystem.fish`); small code to compare |
| Fish of Thieves, extra find | Fish variants by place, time and weather | MIT / MIT | Yes, 21.1.2.2, 2026-09-30 | 2026-09-30 | Reference only | Variant spawn conditions as data |
| Better Animals AI | Not found | — | — | — | — | No mod with this name on Modrinth, and search did not find it on CurseForge. The nearest is Better Animals Plus: CurseForge says the source is under a custom CC BY-NC-SA licence, its assets are reserved, and its newest build is 1.19.2 from 2023-01-08, with no NeoForge. Reference only |
| Animal Crops (KnightMiner) | Crops that grow animals | MIT | No (newest 1.18.2, Forge) | 2022-12-30 | Avoid | Nothing maps to the design |
| Critters and Companions (bonsaistudi0s) | GeckoLib animals (otter, red panda, ferret, owl, and more) with sleep, foraging and fleeing | MIT code / ARR assets (repo LICENSE.md) | Yes, 2.7.0, 2026-08-14; needs GeckoLib, Architectury, YACL | 2026-08-19 | Adapt code | The cleanest MIT GeckoLib animal code on 1.21.1. Its entity, GeckoLib controllers and behaviour goals are a template for `YoiAnimal` and the animation controllers. Do not take any models or textures |
| Naturalist (Starfish Studios) | 47 animals: deer that flee, bears that eat berries and sleep, birds | MIT code / ARR resources (repo LICENSE) | Yes, 2.0.5, 2026-09-28 (no declared GeckoLib dependency) | 2026-09-28 | Reference only | Its behaviours make a good AI reference, and the code is MIT, but our Brain parts are already specified |
| Alex's Mobs Continued | Large animal set, Citadel-based models | LGPL-3.0 (repo) or GPL-3.0 (Modrinth) / same | Yes, 2.2.3, 2026-10-16; needs CodxLib | 2026-10-16 | Reference only | No GeckoLib; Citadel animation. Only for behaviour ideas |
| Untamed Wilds (unofficial port) | Realistic wildlife with hunger, sleep, herds and species ranges | GPL-3.0-only | Yes, 3.0.1, 2026-02-15; needs a Citadel port | 2026-02-15 | Reference only | Herd and range logic informs populations and tiers. Uses Citadel, not GeckoLib |
| Respawning Animals (Fuzss), extra find | Animals spawn and despawn like other mobs instead of staying forever | MPL-2.0 | Yes, 21.1.2, 2025-08-12 | 2026-09-26 | Reference only | Our populations model replaces this |
| Cosy Critters & Creepy Crawlies, extra find | Client-only birds, bugs and moths as particles-like effects | MIT | Yes, 0.3.3, 2026-07-07 (client only) | 2026-07-07 | Reference only | Ambient life with no gameplay; our birds are real animals. Its client-side flock code could inform distant bird rendering |

## 7. Journal and book UI (`story` journal, messenger screens, `story.study`, herbarium)

| mod | what it gives | licence (code / assets) | NeoForge 1.21.1? | last release | verdict | why |
| --- | --- | --- | --- | --- | --- | --- |
| Patchouli (Vazkii) | Data-driven book screens: pages, entries, categories, images, multiblock previews | CC-BY-NC-SA-3.0 for the whole repo / same | Yes, 1.21.1-93, 2026-03-08 | 2026-07-10 | Reference only | Books are static and unlock by advancements, which we filter out. Our journal is written by events. NC-SA is fine for personal use, but the content model does not fit |
| Modonomicon (klikli-dev) | Book screens with quest-tree category maps, conditions and unlocks, multiblock pages | MIT code / CC-BY-SA-4.0 assets (README › Licensing) | Yes, 1.120.7, 2026-09-25 | 2026-10-13 | Adapt code | MIT code. The book renderer (page layout, text flow, image and spotlight pages) could save time on the journal and herbarium pages. Drive it from our `journal` attachment, not its unlock conditions |
| GuideME (AppliedEnergistics) | Markdown guidebook with live 3D scenes and search | LGPL-3.0 (embedded parts MIT and Apache-2.0) | Yes, 21.1.19, 2026-09-16 | 2026-10-16 | Reference only | Strong Markdown rendering and in-game 3D structure previews. It suits a static manual, not a dynamic journal |
| Field Guide (evanbones), extra find | Discover plants, animals and monsters by looking through a spyglass; collect entries | MIT | Yes, 1.22.1, 2026-10-19; needs Immersive Overlays and Item Descriptions | 2026-10-19 | Reference only | The "study through binoculars, then the entry appears" loop is a close match for `story.study` and the herbarium. Read its look-and-confirm code |
| Knowlogy Book | Simple guide book | GPL-3.0-only | Yes, 0.9.1, 2025-07-09 | 2025-07-09 | Avoid | Nothing Modonomicon lacks |

## 8. Client feel (optional client add-ons; DV4 › Rendering, Audio bus, First-person feel)

| mod | what it gives | licence (code / assets) | NeoForge 1.21.1? | last release | verdict | why |
| --- | --- | --- | --- | --- | --- | --- |
| Sound Physics Remastered (henkelmax) | Reverb, occlusion and air absorption by ray tracing at the OpenAL level | GPL-3.0-only | Yes, 1.5.1, 2025-09-25 | 2026-09-19 (26.x) | Optional client add-on | It works below our Audio bus, so cave and forest echo comes free. Test that danger cues (ice crack, huff) still reach the player through occlusion. Add it to the nightly matrix before allowing it |
| AmbientSounds (CreativeMD) | Biome, time and weather ambience beds, data-driven | LGPL-3.0-only (sounds not separately licensed in the repo) | Yes, 6.3.10, 2026-10-08 | 2026-10-08 | Avoid | It would double our ambience beds and break the 4-stream and 32-voice budget. Its engine is a reference at most |
| LambDynamicLights (LambdAurora) | Light from held and dropped items and entities; a NeoForge API with JSON item light sources | Lambda License: source may be shared and modified under the same licence, but sharing binaries needs the author's written approval | Yes, 4.8.11, 2026-08-31 | 2026-09-15 | Optional client add-on | This is step 1 of the held-light plan (RECONCILE 35): `HeldLight` registers our light items with it. Fine for personal use. If the pack is ever shared, link to it rather than bundling it |
| Sodium Dynamic Lights | Dynamic lights inside Sodium's pipeline | MIT | Yes, 1.0.10, 2025-01-02 | 2025-04-10 | Reference only | Stale. MIT code is a reference for prototype P4 (lighting terrain under Sodium) |
| Distant Horizons | Far terrain LOD | LGPL-3.0-only | Yes, 3.3.3, 2026-09-29 | 2026-09-29 | Optional client add-on | Already planned; off by default, patched only through its API |
| Fresh Animations | Resource pack animating vanilla mobs | Custom terms: no redistribution, no unedited assets in other packs | Resource pack; needs EMF and ETF (EMF has a NeoForge 1.21.1 build, 2026-10-15) | 2026-04-01 | Avoid | Vanilla mobs are removed, so there is nothing for it to animate. Its terms forbid reuse |
| First-person Model (tr7zw) | Shows the player's body in first person | MIT; requires Not Enough Animations (tr7zw Protective License) | Yes, 2.7.3, 2026-09-27 | 2026-09-27 | Avoid | Conflicts with our `RenderHandEvent` hands (grime, gloves, frostbite) and the carry poses. Its dependency is under a restrictive licence |
| Real Camera (xTracr) | Moves the camera to the model's head bone, so first person shows the body | MIT | Yes, 0.7.8-beta, 2026-06-28 | 2026-09-25 | Avoid | Conflicts with `BodyCamera` and `CameraMixin`, and would double the eye offsets and bobbing. MIT code is a reference if a body view is ever wanted |

## 9. Dev tools

| mod | what it gives | licence (code / assets) | NeoForge 1.21.1? | last release | verdict | why |
| --- | --- | --- | --- | --- | --- | --- |
| spark (lucko) | CPU, tick and memory profiler | GPL-3.0-only | Yes, 1.10.124, 2025-02-23 | 2026-09-19 | Depend (dev and CI only) | Already planned for the tick budget checks. Stays out of the player pack |
| KubeJS | JavaScript scripting for recipes, events and items | LGPL-3.0-only | Yes, 2101.7.2, 2026-09-09 | 2026-10-15 | Avoid | Our data pipeline (codecs, validator, linter) already covers content as data. A second scripting layer would bypass the linter |
| Jade (Snownee) | Hover HUD naming the block or entity, with health and contents | CC-BY-NC-SA-4.0 | Yes, 15.10.6, 2026-08-06 | 2026-10-13 | Depend (dev only) / Avoid in pack | Handy for checking blockstates and ages during dev. For players it labels everything, which undoes "Everything readable". Put it on the warned list |
| WTHIT | Same as Jade | CC-BY-NC-SA-4.0 | Yes, 12.10.2, 2026-03-24 | 2026-07-26 | Avoid | Jade is more active. Same pillar conflict |
| JEI (mezz) | Item and recipe browser | MIT | Yes, 19.64.0.455, 2026-10-16 | 2026-10-18 | Avoid | No recipe trees, and it would show recipes the journal is meant to teach |
| EMI | Item and recipe browser | MIT | Yes, 1.1.24, 2026-05-13 | 2026-05-13 | Depend (dev only), optional | A quick way to browse every `yearofice` registry entry in a dev run. Never in the pack |

Libraries already in use, checked in passing: GeckoLib is MIT (Yes, 4.9.3, 2026-09-16). Curios API is LGPL-3.0-or-later (Yes, 9.5.1, 2025-05-14).

## Top recommendations (at most 6)

1. **TerraFirmaCraft: adapt its decay, heat and knapping code** (EUPL-1.2, NeoForge 1.21.1, active). Item-stack creation dates plus trait multipliers are the Decay and freshness layer in closed form, with stack merging solved. The heat capability serves cooking and the forge, and the knapping screen serves the first flint step. This saves the most time of anything here. EUPL is copyleft if the game is ever shared.
2. **Cold Sweat for the temperature model, Homeostatic for code you can copy freely.** Use Cold Sweat (GPL-3.0) for block heat sources, insulation and room warming. Use Homeostatic (MIT) for wetness and purity. Together they cover the Environment field heat index and the body's cold.
3. **First Aid New: adapt its damage-to-limb distribution** (GPL-3.0, active on 1.21.1) for Body injuries and the player hit zones.
4. **Tide 2: adapt the fishing fight and catch conditions** (MPL-2.0, NeoForge 1.21.1) for `hunt.fishing` and Fish stocks.
5. **Snow! Real Magic!: adapt the snow-over-plants blocks** (MIT, active), with Ecliptic Seasons (BSD-3 code) as a reference for snow build-up and melt. This is the core of "the year changes the map", and it is the hardest rendering case.
6. **Optional client add-ons: LambDynamicLights** (held-light plan step 1, through its NeoForge API) **and Sound Physics Remastered** (free reverb and occlusion under the Audio bus). Test both in the nightly matrix first.

Also worth a small adaptation: Carry On's pickup and carried-pose code (LGPL), the Critters and Companions GeckoLib animal template (MIT code only), the Modonomicon book renderer (MIT), Hardcore Torches' burn-out rules (CC0) and TreeChop's chop counts (MIT).

## Conflicts with the pillars

- **Everything readable.** Jade and WTHIT name every block and animal on hover. JEI and EMI show recipes the journal is meant to teach. Keep them dev-only and add them to the first-launch warning list.
- **Scarcity and respect time.** Carry On as a dependency would bypass load bands and the 45 kg rule. FallingTree's instant whole-tree fell skips the felling work that tools and practice are meant to speed up.
- **The year changes the map.** Serene Seasons, Weather2, Primal Winter and Ecliptic Seasons as dependencies, or Tough As Nails, Cold Sweat and Legendary Survival Overhaul, would each take over time, weather or needs. DV5 already blocks these, so use their code or designs only.
- **Grow without end, plus the placed mask.** Dynamic Trees replaces every tree with its own blocks and fights the tree records and worldgen tree builders. Use it as a design reference only.
- **Client feel.** AmbientSounds would double the ambience beds and break the voice budget. First-person Model and Real Camera fight `BodyCamera`, the drawn hands and the carry poses. Fresh Animations has no vanilla mobs left to animate.
- **Journal.** Patchouli and Modonomicon unlock entries by advancements, which we filter out. Only the renderer is reusable, driven by our `journal` attachment.
