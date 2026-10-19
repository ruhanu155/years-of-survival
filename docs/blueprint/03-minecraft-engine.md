# Minecraft engine

Year of Ice is built as a Minecraft Java Edition mod on NeoForge, mod id `yearofice`, with GeckoLib and Curios API. This tab maps every engine-dependent part of the blueprint onto Minecraft and is the authority wherever another tab says JavaScript, browser, WebGL, GLSL, IndexedDB, Playwright, `js/` modules, the starter kit or the 0.5 m grid. The design itself stands as written; only how it is built changes. Where this tab and the Minecraft development tab disagree, the development tab wins; blocks changed in the 19 October review to match it end with "(review: Minecraft development › …)".

## Platform and project

The mod is pinned to Minecraft 1.21.1 on NeoForge. It is built solo first, with time rules kept co-op-safe so co-op can follow after 1.0, and it is for Ruhan's personal use only, with no public release (RECONCILE 115, 118, 119). It must still boot and run on a dedicated server. All game rules live in plain Java that never touches Minecraft classes, so the balance simulator and most tests run in milliseconds.

### Version pin

**The pin: NeoForge 1.21.1 (decided 19 October, RECONCILE 118).** 1.21.1 is the long-lived modding version, and GeckoLib and Curios both have mature releases for it. The M0 round writes every exact version into `gradle.properties` and `gradle/libs.versions.toml`, taking the newest stable NeoForge 21.1 build on that day.

**The pin never moves mid-milestone.** A move to a newer line, such as 26.1 (Java 25, no obfuscation, renamed GUI and stack APIs), is its own round after a milestone ships, by Minecraft development › Moving Minecraft or NeoForge, and only once NeoForge, GeckoLib and Curios all have stable releases for it.

| Part | Pinned | Notes |
| --- | --- | --- |
| Minecraft | 1.21.1, Java Edition | The modding line every library supports |
| NeoForge | 21.1.264 | Loader; events, registries, attachments, data maps, payloads, GUI layers, GameTests |
| Java | 21 | Set as the Gradle toolchain, so a machine with another JDK downloads 21 |
| Gradle | 8.14.3 with ModDevGradle 2.0.148 | Wrapper committed; Parchment mappings for 1.21.1 give parameter names |
| GeckoLib | 4.9.3 for NeoForge 1.21.1 | Animals and animated items |
| Curios API | 9.5.1+1.21.1 | Clothing layers, hands, belt, back, neck and pocket slots |

The cloud workspace built a test mod on exactly these versions on 19 October 2026; the first build took about 5 minutes (RECONCILE 122). A later NeoForge 21.1 patch build may replace 21.1.264 at the start of any milestone.

API names in this tab are the 1.21.1 names (for example `GuiGraphics`, `RegisterGuiLayersEvent` and annotated GameTest holders). **The build needs the network.** Gradle must reach maven.neoforged.net, Maven Central, services.gradle.org, plugins.gradle.org, libraries.minecraft.net, Mojang's piston and resources hosts, and the GeckoLib and Curios mavens. Until Ruhan allows those in the cloud workspace, builds and tests run on his linked computer, which also runs the real client (RECONCILE 104, 114). The Forge 1.20.1 MDK in his YearOfIce folder is converted to this pin at M0, and the rules of its CLAUDE.md carry over as Ruhan's project rules, below.

### Ruhan's project rules

These come from the CLAUDE.md in Ruhan's YearOfIce project folder and bind every system and every milestone (RECONCILE 116, 118, 119).

1. **Personal use only.** The mod and pack are built for Ruhan's own machines. There is no public release work: no Modrinth or CurseForge pages, no public release channels, no public pack README and no warning screens for other people's mod lists. packwiz stays only to install and update the pack on Ruhan's Mac and to version it, and his own worlds must survive every update (Minecraft development › Saves across versions).
2. **A `/yoi` debug command for every system.** Each system registers its subcommands under `/yoi` in `RegisterCommandsEvent` through `core.debug.YoiCommands`, at permission level 2. A system's milestone is not done until its commands exist and one GameTest calls each. Examples: `/yoi set calories 1500`, `/yoi set bmi 17.5`, `/yoi set warmth 40`, `/yoi time day 200`, `/yoi time rate sleep`, `/yoi spawn moose`, `/yoi weather blizzard 6h`, `/yoi ice set 30cm`, `/yoi fire state coals`, `/yoi trap roll`, `/yoi decay advance 3d`, `/yoi tp region B3`, and `/yoi show rooms|scent|tiers|current` for debug overlays drawn by `client.debug`. Commands write through each layer's own service, so they obey the one-writer rule and post the same events as play. A world in which any `set`, `time` or `spawn` command ran is flagged `yearofice_world.debug_used`, and its journal and year cards say so.
3. **All tuning in `yearofice-common.toml`, with presets.** Every number a player might want to change (burn and drain rates, warmth, bleeding, decay speed, bite and catch chances, animal counts and boldness, weather severity, night darkness, battery drain, dropped-item life) is a key in `config/yearofice-common.toml`, grouped by system (`[body]`, `[decay]`, `[ecosystem]`, `[weather]`, `[fire]`, `[fishing]`, `[ui]`). Three presets set every key at once: Gentle, Standard and Brutal. `preset = "standard"` picks one; a key set by hand overrides it and shows the preset as "Custom". Data files hold the base numbers and shapes; config keys are multipliers or overrides on them, so `/reload` and data packs keep working. A world copies the values it was created with into `yearofice_world`, so a later config edit changes it only when the player chooses "Apply config to this world". GameTests run on Standard.
4. **Names and art stay original.** Every name, text, texture, model and sound is our own: generated by the studio's scripts, or a found asset under CC0, CC-BY or MIT with a credits entry (Art and audio production). No other game's or mod's names or assets. Vanilla textures may be referenced at runtime, for example a model pointing at `minecraft:block/stone`, but are never copied into the jar.
5. **The messenger is the "Trail Messenger".** That is its in-world name: lang key `item.yearofice.messenger` reads "Trail Messenger", the id stays `yearofice:messenger`, and the word "messenger" in every tab means this device.

### Libraries

| Library | Kind | Why |
| --- | --- | --- |
| NeoForge | Loader, required | Events, registries, data attachments, data maps, payloads, GUI layers, GameTests |
| GeckoLib | Required | Animal models, animations and animated items (bow drill, rod) |
| Curios API | Required | Inner clothing layers, hands, belt, back and neck slots |
| spark | Dev and CI only, not shipped | TPS, MSPT and allocation profiles for the performance test |
| Distant Horizons | Optional client mod, recommended | Real far terrain for the 2 km landmark rule; the built-in horizon mesh covers players without it |
| Sodium and Iris (NeoForge ports) | Optional compatibility targets | Many players run them; the mod must not break under them (see Risks) |

No other runtime dependency is allowed without the Director's sign-off. Mixins are allowed only from a whitelist in `yearofice.mixins.json`, at most 8, each with a test.

### Repository layout

```text
years-of-survival/   (GitHub ruhanu155/years-of-survival; local copy on Ruhan's Mac: Game Development/YearOfIce)
  build.gradle, settings.gradle, gradle.properties   (pinned versions)
  src/main/java/com/yearofice/<system>/...           (mod code)
  src/main/java/com/yearofice/sim/...                (pure Java rules, no Minecraft imports)
  src/main/java/com/yearofice/client/...             (client-only code)
  src/main/resources/META-INF/neoforge.mods.toml
  src/main/resources/assets/yearofice/               (blockstates, models, textures, geo, animations, sounds, lang, shaders)
  src/main/resources/data/yearofice/                 (hand-written data)
  src/generated/resources/                           (datagen output, committed)
  src/test/java/                                     (JUnit: sim, worldgen, data validation)
  src/main/java/com/yearofice/gametest/              (GameTests)
  tools/                                             (sound render, seed tools, sim CLI, screenshot checker)
  docs/                                              (Markdown export of the blueprint; ready-for-development.md first)
```

The Games project keeps the blueprint and briefs under `yearofice/`; the mod repository replaces `year-of-ice/` and the starter kit.

### Code rules

- **Pure core, thin adapters.** Every formula (body tick, heat model, weather chain, spoilage, germination, populations) lives in `com.yearofice.sim` with no Minecraft imports. Adapters in the system packages read the world, call the core and write results back.
- **Server decides, client shows.** Game state changes only on the logical server. The client renders, plays sound and sends inputs.
- **Client code is fenced.** Anything that touches rendering, screens or input sits in `com.yearofice.client` and registers through `Dist.CLIENT` subscribers. A dedicated-server boot test catches a leak.
- **Events replace the hook bus.** Each blueprint hook becomes a NeoForge event class in `com.yearofice.api.event`, posted on `NeoForge.EVENT_BUS` (table below). `ask()` becomes a read-only service method, such as `BodyService.warmth(player)`.
- **Data, not code.** Species, items' numbers, recipes, set pieces, weather and events stay in data files, as the blueprint requires.

| Blueprint hook | Event class |
| --- | --- |
| `world.minute`, `hour`, `day`, `dawn`, `dusk` | `ClockEvent.Minute` (with a count when several pass in one tick), `.Hour`, `.Day`, `.Dawn`, `.Dusk` |
| `weather.changed` | `WeatherChangedEvent` |
| `fire.state` | `FireStateEvent` |
| `scent.created` | `ScentEvent` |
| `body.vital` | `VitalChangedEvent` |
| `animal.killed` | `AnimalKilledEvent` |
| `item.crafted` | `StationCraftEvent` |
| `piece.placed`, `piece.removed` | NeoForge `BlockEvent.EntityPlaceEvent` and `BreakEvent`, filtered by tag `yearofice:piece` |
| `room.changed` | `RoomChangedEvent` |
| `ice.changed` | `IceChangedEvent` |
| `trap.caught`, `trap.checked` | `TrapEvent.Caught`, `TrapEvent.Checked` |
| `prompt.offer`, `journal.first` | `PromptOfferEvent`, `JournalFirstEvent` |

### Conventions for every tab

All content tabs follow these, so their "Data and build" sections line up.

| Thing | Convention | Example |
| --- | --- | --- |
| Registry id | `yearofice:<snake_case>` | `yearofice:moose`, `yearofice:smokehouse` |
| Content data | `data/yearofice/<kind>/<id>.json`, one entry per file, loaded by our data loaders | `data/yearofice/species/moose.json` |
| Vanilla data kinds | Minecraft's singular folders | `data/yearofice/recipe/`, `loot_table/`, `tags/block/`, `structure/` |
| Per-item and per-block numbers | NeoForge data maps | `data/yearofice/data_maps/item/weight.json` |
| JSON fields | snake\_case with a unit suffix | `home_range_m`, `weight_kg`, `warmth_c`, `spoil_days_at_5c` |
| Java | `com.yearofice.<system>`, one class per thing | `com.yearofice.ecosystem.species.MooseEntity` |
| Lang keys | Minecraft style; messenger lines get their own prefix | `entity.yearofice.moose`, `messenger.yearofice.first_bear` |
| Sound events | `yearofice:<group>.<name>` | `yearofice:entity.grizzly.huff` |
| GameTests | `yearofice:<system>/<test>`, batch named by milestone | `yearofice:fire/banked_coals_6h`, batch `m3` |
| Tags | `yearofice:<plural_noun>` | `yearofice:room_walls`, `yearofice:freezing_soils` |
| Debug commands | `/yoi <system> ...` for every system, op level 2 (Ruhan's project rules) | `/yoi set calories 1500` |
| Tuning | A key in `yearofice-common.toml` under the system's table, set by the Gentle, Standard and Brutal presets | `[body] bleed_scale = 1.0` |

## Scale and world

One block is one metre, and one tick is 1/20 of a real second. The world has three rings: the 8,192 × 8,192 block home valley of the blueprint's 16 regions, a designed 65.5 km Interior of 256 districts around it, and procedural wilderness beyond it out to Minecraft's default limit (World map tab).

### Size words

The five size words change meaning where Minecraft has its own units.

| Name | Size | Use |
| --- | --- | --- |
| Cell | 2 × 2 blocks | One height sample in the generator layers, as before |
| Chunk | 16 × 16 blocks, full height | Minecraft's loading and save unit; replaces the 64 m chunk |
| Tile | 64 × 64 blocks (4 × 4 chunks) | Generator layer storage and the messenger map tile; the old chunk's role in the layers |
| Region | 2,048 × 2,048 blocks (128 × 128 chunks, 16 `.mca` files) | Generation unit, SavedData unit, one of the 16 cells A1–D4 |
| World | Home valley: 8,192 × 8,192 blocks = 16 regions | Ring 1 of 3; the Interior's 4,096-block districts and Beyond are in the World map tab |

### Coordinates and height

**No small border.** The vanilla world border stays at its default, 59,999,968 blocks wide. The home valley is centred on 0, 0, so its x and z run from −4,096 to +4,095; the Interior runs to ±32,768 and Beyond lies past it (World map tab).

**Regions on the grid.** Column A–D is `floor((x + 4096) / 2048)`, row 1–4 is `floor((z + 4096) / 2048)`. North is −z, as in Minecraft, so row 1 is the north range. The core B3 covers x −2,048 to −1, z 0 to 2,047.

**Height is elevation.** The mod's dimension type sets `min_y` 64 and `height` 1,968, so the top build height is y 2,031, Minecraft's maximum, for the Interior's peaks up to 2,010 m (World map tab). Block y equals elevation in metres: the valley floor sits near y 150 and Twin Horn near y 1,400.

**Why so tall.** The world runs from 100 m at the Grey River to 2,010 m on Mount Varden. Sections that are solid deep ground or empty air store one value and draw nothing, so the cost is in the surface sections only. The M2 frame test decides; the fallback is in Risks.

**Clouds.** The dimension's cloud height is 1,800, above every peak. Storm and blizzard days draw a low cloud deck from the custom sky renderer at 400–700 m.

**One dimension.** A world preset `yearofice:year_of_ice` (in `data/yearofice/worldgen/world_preset/`) defines only the overworld. Nether and End are off by default (see No monsters).

### The nine stages in Minecraft

Stages 1–7 run in Java worker threads, write per-region layers and never touch blocks. A custom `ChunkGenerator` (`yearofice:valley`, class `com.yearofice.worldgen.YoiChunkGenerator`) turns layers into blocks when Minecraft asks for a chunk.

| # | Stage | Minecraft mechanism | Class |
| --- | --- | --- | --- |
| 1 | Seed | Our seed = hash(world seed, "yearofice"); attempt k is stored in SavedData `yearofice_world` | `worldgen.SeedStage` |
| 2 | Landform | Region layer: height and hardness at 2 m cells, coarse 32 m field for the world | `worldgen.Landform` |
| 3 | Erosion | Region layer: eroded height, sediment depth | `worldgen.Erosion` |
| 4 | Water and rock | Region layer: water type, surface y, flow direction, speed, falls, pools, fishing spots, rock faces | `worldgen.Hydrology` |
| 5 | Places | Point lists: set pieces, landmarks, caches, licks, footprint masks | `worldgen.Places` |
| 6 | Cover | Our cover per 2 m cell; the Minecraft biome per 4 m quart via `YoiBiomeSource` (`yearofice:valley`); tree and node lists | `worldgen.Cover` |
| 7 | Life and start | Ranges, trails, dens, lodges, start site (core only), stored in SavedData | `worldgen.Life`, `worldgen.StartSite` |
| 8 | Weather | Runtime: the year table, rebuilt from seed and year on load, never saved | `story.WeatherYear` |
| 9 | Edits | Minecraft's own chunk saving. Player edits are block changes and are saved with the chunk; no edit lists | Vanilla |

**How a chunk is built.** `fillFromNoise` writes deep ground, rock, soil and surface from the layers. `buildSurface` lays moss, gravel, sand and mud by cover. `applyBiomeDecoration` places trees, plants, nodes and set pieces from the stage lists that fall in the chunk and its neighbours. Water blocks come from stage 4's surface y, never from a sea level.

**Biomes.** Each blueprint biome is a Minecraft biome JSON in `data/yearofice/worldgen/biome/` with no mob spawners, no vanilla features and `has_precipitation` false. Gameplay reads our 2 m cover cell, not the Minecraft biome, which only sets tint, fog colour and ambience.

**Trees.** Trees are placed by Java tree builders (`worldgen.trees.WhiteSpruce`, `Birch`, `Willow`, `Alder`, `BlackSpruce`, `Aspen`, plus a deadwood variant), sized by species and a seeded draw. Each tree records its id in a chunk data attachment so felling, regrowth and the "readable trees" card can name it.

**Streaming regions.** Minecraft asks for chunks on demand. When the player comes within 600 blocks of a region whose layers are not cached, a background task builds its layers. A chunk request that arrives first waits on that task in the world-generation thread, never on the main thread.

**Layer cache.** Region layers are kept in an LRU of 6 regions in memory and cached on disk under `<world>/yearofice/layers/<cell>.bin`. The cache can always be deleted; it is rebuilt from the seed.

**Determinism.** Same seed and same generator version give the same layers on every machine.

- Generation uses integer hashing and `+ − × ÷` and `Math.sqrt` only (all exactly rounded in Java). `StrictMath` is allowed where trig is unavoidable; `Math.sin`, `Math.exp`, `Math.pow`, `java.util.Random` and `ThreadLocalRandom` are banned in `com.yearofice.worldgen`.
- Each stage draws from `Mulberry32(hash(attemptSeed, stageName))`, ported from the blueprint. Runtime rolls use `hash(seed, system, day)` and catch-up rolls `hash(seed, chunkPos, "catchup", day)`.
- Parallel work inside a stage must write disjoint cells. No result may depend on `HashMap` iteration order.
- A lint test (`tools/lint-gen`) fails the build on any banned call in the worldgen package.

**Rerolls.** A failed check in stages 1–7 at world creation retries attempt k + 1, up to 20, while Minecraft shows its world-loading screen with our stage names. A region that fails later retries with the five sub-seeds the blueprint gives.

**Generator versions.** A generated chunk never regenerates, so land under a cabin never moves. Ungenerated chunks of an old save must still match their neighbours, so every released generator version stays in the jar (`worldgen.v1`, `v2`...) and `yearofice_world` names the one a world uses.

### Terrain to blocks

**From 2 m cells to 1 m columns.** Each column's surface height is the bilinear blend of its four nearest cells, rounded to the block. Slopes become steps of 1 block. The player's step height is vanilla's 0.6, raised to 1.0 on blocks tagged `#yearofice:terrain_steps` (the generated ground surfaces), so walking up the land is smooth while building pieces still need a jump; a walking stick gives 1.0 on other blocks too (RECONCILE 37, 83; review: Minecraft development › Player systems and client).

**Ground layers.** Top down, every column holds:

1. The surface block by cover: moss, forest duff, grass, gravel, sand, mud, peat or bare rock.
2. Soil blocks to a total of 2 diggable blocks (only 1 in permafrost; see below).
3. `yearofice:deep_ground` for everything below: unbreakable, no drops, looks like compact grey till.

**Rock faces.** Cells over 40° with hardness over 0.6 become rock. The outer 1-block shell is `yearofice:face_rock` (breakable with a pick or hatchet, drops angular stone); everything behind it is `yearofice:deep_rock` (unbreakable). Iron outcrops are `yearofice:iron_outcrop` blocks in the shell, with a rust-stain surface block on the scree below.

**Surface layers.** Gravel, sand, mud and snow may use 1/8-block layer blocks where the 2 m field says the true surface sits between blocks. This smooths bars and banks at a geometry cost; the M2 frame test decides whether it stays on.

### Rivers, current and water

**Water blocks.** Rivers and lakes are vanilla water source blocks up to stage 4's surface y. Falls are vanilla flowing water columns, which already look and sound right. Riffles step the surface down by 1 block where the bed drops.

**Vanilla water has no current.** Source blocks have no flow, so the mod adds one. Stage 4's flow direction and speed V (0.3–3.0 m/s) are written per column into a chunk data attachment `yearofice:current` (one byte for direction in 64 steps, one for speed in 0.05 m/s steps).

**Who is pushed.** Every tick, anything in a water block with current gets V added to its motion along the flow, scaled by how deep it sits.

| Thing | Where the push runs | Rule |
| --- | --- | --- |
| The local player | Client (player motion is client-side) | Push from the synced current; swimming and wading speeds from Swimming |
| Other players, animals | Server | Same push; GeckoLib animals swim across at their species speed |
| Dropped items | Server | Full push; items under 1 kg catch on snags (blocks tagged `yearofice:snags`) |
| Raft, canoe, floating logs | Server, or the client of the paddler | Push minus paddling; logs jam at snags, as in Building |

The client gets the current with each chunk: on `ChunkWatchEvent.Sent` the server sends a `yearofice:current_chunk` payload of 512 bytes. A test checks a 20 m crossing of a 1.5 m/s river lands 25–35 m downstream.

**Showing current.** Foam particles (`yearofice:river_foam`) drift along the flow on the surface within 32 blocks, denser on riffles. The river sound pitch follows width.

**Flood lines.** Normal and flood high water are stored per river column. A spring flood places water blocks up to the line in loaded chunks over 6 game hours and removes them as it drops; unloaded chunks settle on load. The mud mark is a decal on trunks at the normal line.

### Ice, snow and permafrost

**Ice per water body.** Stage 8 already computes thickness per water body each day. SavedData `yearofice_ice` keeps today's thickness per body; blocks only show it.

| Ice state | Block | Walk rule |
| --- | --- | --- |
| Skim | `yearofice:skim_ice` on the surface layer, gone by noon | Breaks under anything |
| Thin, under 10 cm | `yearofice:river_ice` with `stage=thin` | Breaks by the thin-ice rules in Danger in detail |
| Safe | `stage=safe`, whiter, with snow on top | Holds a player at 10 cm, a loaded sled at 20 cm |
| Rotten | `stage=rotten`, grey | Counts as half its thickness |

Thin spots (below falls, springs, outsides of fast bends) are flagged per column in the current attachment, so the same block can be safe in one place and thin in the next. When ice breaks, the blocks within 1–2 m turn to water and the self-rescue starts.

**Freeze and breakup passes.** When a body changes state, loaded chunks on it update 4 chunks a tick. A chunk that loads later compares its stored ice day (chunk attachment `yearofice:season_stamp`) with today and catches up in one pass.

**Snow as one block.** Snow is `yearofice:snow_cover` with `layers` 1–8 (12.5 cm each, so 8 layers is 1 m) and `crust` true or false. Powder has collision of 1 layer only, so the player sinks in and the deep-snow speed rule applies above 40 cm. A crust has full collision, which is the March crust holding a walker until 11:00.

**Snow by class.** The four snow classes stay. Each column's class (open, under spruce, south slope, lee drift) is fixed at generation and stored in the season-stamp attachment, so a snow pass sets the right layer count with no search.

**Snow passes.** After each snow day, melt day or settle step, loaded chunks are rewritten nearest first, at most 6 chunks a tick. Tracks in snow are decals, not block changes (see Tracks and sign).

**Permafrost.** Permafrost columns hold 1 diggable soil block over `yearofice:frozen_soil`, which a pick takes at a quarter speed and which yields nothing useful. Gardens there fail by the Crop farming rules.

**Frozen ground.** From October to breakup, blocks tagged `yearofice:freezing_soils` cannot be dug: `PlayerEvent.BreakSpeed` sets their speed to 0 and the prompt says "Frozen ground." No blocks change.

### Explore, don't dig

Pillar 3 is enforced four ways, and the generator test from the main tab still runs.

1. **Nothing is below.** Under 2 soil blocks lies unbreakable deep ground; behind 1 rock block lies unbreakable deep rock. No resource node is ever placed below the generated surface.
2. **Only the shell breaks.** `face_rock` and `iron_outcrop` exist only on the outer shell, so the player can take stone and iron but never tunnel.
3. **Tools gate the rest.** `canHarvestBlock` and `BreakSpeed` events apply the tool rules (a pick takes a nodule in 5 game minutes, the hatchet in 10).
4. **Dug features are pieces.** A root cellar, latrine or Dakota hole is a building piece that clears its own footprint of soil blocks only. Deep ground may be cleared only inside a validated dugout (capped at 4 × 2 × 6 m), root cellar (permafrost cellar) or chimney footprint, level with or above the mouth floor, with no drops (RECONCILE 12, 20).

*Test (GameTest `worldgen/no_nodes_below_surface`):* over 200 seeds, 0 resource blocks have deep ground or deep rock above them, and every seed has 12–18 iron clusters.

### Set pieces and landmarks

**Templates.** Each set piece variant is a structure template (NBT) at `data/yearofice/structure/setpiece/<id>/<variant>.nbt`, built in a creative test world and saved with a structure block. Containers in it point to loot tables at `data/yearofice/loot_table/setpiece/<id>.json`.

**Placement.** Stage 5 still picks every position. A custom structure type `yearofice:setpiece` with a custom placement asks the places layer whether a set piece starts in a chunk, so no vanilla spacing or salt is used. The footprint mask flattens the ground in `fillFromNoise` before the template lands.

**Notes and pages.** Logbook pages and story notes are one item, `yearofice:note`, whose data component `yearofice:note_id` names the entry in `data/yearofice/note/`. A template holds the item in a container or on a lectern-like `yearofice:note_spot` block.

**The outpost.** The outpost is the template `setpiece/outpost/day_one`, rotated so the door faces the landmark. The cabin is 5 × 6 blocks outside and 3 × 4 inside (see Building).

**Landmarks from 2 km.** Minecraft draws at most 32 chunks (512 m). Two things carry the 2 km rule:

- **The horizon mesh** (built in, M2). The client receives the home valley's coarse 32 m height field (256 × 256 values, about 130 KB compressed) and the Interior plan's 128 m height (World map tab) once at login. `client.horizon.HorizonRenderer` draws it as a low-poly ring from the edge of the render distance out to 24 km, coloured by cover and season, with each region's landmark as a simple silhouette model on top.
- **Distant Horizons** (optional). Where it is installed, its far terrain replaces the horizon mesh, which then draws only landmark silhouettes beyond its range.

The 2 km test is unchanged and runs on the layers, not on screenshots.

### Vanilla switched off

The world preset sets these at creation, and `core.rules.GameRuleEnforcer` re-applies them on every server start. Minecraft development › Vanilla systems we replace is the master list; this table matches it (review: rows added to match it).

| Game rule or switch | Value | Why |
| --- | --- | --- |
| `doDaylightCycle`, `doWeatherCycle` | false | The mod drives time and weather |
| `doMobSpawning`, `doPatrolSpawning`, `doTraderSpawning`, `doInsomnia` | false; `disableRaids` true | Animals come from populations; no phantoms, patrols, traders or raids |
| `doFireTick` | false | Vanilla fire spread off; our fire has its own rules |
| `naturalRegeneration` | false | Healing is the body model's |
| `doMobLoot` | false; `doEntityDrops` true | Death loot tables are empty and every death makes a carcass; block and container drops still work (RECONCILE 41, 44) |
| `mobGriefing`, `universalAnger` | false | Our animals change blocks only through World edits |
| `freezeDamage` | false | Cold is the body model's |
| `announceAdvancements` | false | Ambitions replace advancements |
| `randomTickSpeed` | 0 | Crops, snow, ice and leaves follow the calendar passes, not random ticks |
| `maxEntityCramming` | 0 | A winter yard or a caribou herd packs tight without vanilla's crush damage (RECONCILE 44) |
| `keepInventory` | true | Vanilla death never runs; on collapse `SosFlow` moves everything carried except worn clothing and the messenger into a `yearofice:left_pack` block that never expires (RECONCILE 84) |
| `spawnRadius` | 0 | The return point is the outpost gravel bar |
| `playersSleepingPercentage` | 100 | Sleep skips only when every player sleeps (RECONCILE 115) |
| `yearofice:first_person_only`, `reducedDebugInfo` | true | Third person sees over cover; F3 shows no coordinates. Developer worlds turn both off |
| Difficulty | Normal, locked | Every mod damage type uses `"scaling": "never"` (RECONCILE 44) |
| Vanilla recipes and advancements | Filtered by the built-in data pack `yearofice:vanilla_filter` | The station recipes and ambitions replace them |
| Vanilla items | Unobtainable in survival: buckets, flint and steel, torches, shields, bone meal, spyglass, maps, boats; vanilla `lily_pad`, `mud`, `packed_mud`, `mud_bricks` hidden for ours (RECONCILE 34) | Recipes filtered, no loot table names them, hidden from creative tabs; bucket use on a fluid is cancelled |
| Vanilla entities | Removed on `EntityJoinLevelEvent` if in tag `yearofice:banned_vanilla` (all vanilla mobs) or not in our namespace | Belt and braces with empty spawners |
| Dropped items | Last 7 game days by the drop's game-minute stamp, not 5 real minutes; `#yearofice:never_expires` and `left_pack` never expire; sunk gear becomes `lost_gear` (RECONCILE 36, DIRECTOR RULING 89) | `ItemEntity` lifespan set to maximum on join; `ItemExpireEvent` and a chunk-load check read the stamp |

### No monsters

**The owner's rule: there are no hostile mobs.** The only threats are real animals (bears, moose, wolves, wolverines) and the environment: cold, water, ice, weather, falls and hunger. No change to this rule ships without Ruhan's yes.

**What is off.**

- Every vanilla monster: zombies, skeletons, creepers, spiders, endermen, witches, slimes, drowned and phantoms.
- Pillager patrols and raids, and wandering traders.
- Vanilla passive and neutral mobs: cows, pigs, sheep, chickens, horses, villagers and vanilla wolves. The mod's own animals replace them; the tamable wolf is `yearofice:wolf`, never `minecraft:wolf`.

**How it is done.**

1. **No spawners.** The mod's biome JSONs list no spawners. A NeoForge biome modifier, `data/yearofice/neoforge/biome_modifier/no_vanilla_spawns.json` of type `neoforge:remove_spawns`, strips all vanilla entity types from every biome, including any biome another data pack adds.
2. **Game rules.** `doMobSpawning` false (our animals come from populations, not vanilla spawning), `doInsomnia` false (no phantoms), `doPatrolSpawning` false, `doTraderSpawning` false. Raids cannot start, since there are no villages or pillagers.
3. **A catch-all.** `EntityJoinLevelEvent` cancels any entity in tag `yearofice:banned_vanilla`, which holds every vanilla mob type. Spawn eggs and commands for them do nothing outside creative test worlds.
4. **No vanilla structures.** The chunk generator places no villages, monuments, mansions, outposts, strongholds or dungeons, so no spawner blocks exist.
5. **mobGriefing.** It stays false. Our animals' effects on blocks (beavers felling trees and building dams, bears tearing caches and doors) run through the mod's own code, which does not read this rule.

**Portals, a reversible default.** Nether and End are off by default: the world preset holds only the overworld, `BlockEvent.PortalSpawnEvent` is cancelled so no portal ignites, and no stronghold or end portal frame generates. A world-creation option `yearofice:other_dimensions` (default off) adds them back; the owner can flip the default.

**Test (GameTest `ecosystem/no_monsters_full_night`).** In a test world, for every one of the mod's biomes, the clock runs one full December night (17 game hours of dark) at the sleep rate with a player present. It asserts zero entities of any `minecraft:` mob type and zero entities in `yearofice:banned_vanilla`. A second test tries to light a portal frame and asserts no portal block appears.

## Time and sky

The blueprint's clock is kept exactly; Minecraft's own clock only draws the sky. The mod advances game time by a rate per tick and writes `dayTime` so vanilla lighting matches the true sun.

### The clock

**Game time.** SavedData `yearofice_clock` holds `game_ds`, game deciseconds since 00:00 on May 1 of year 1, as a long (review: Minecraft development › Layer 1). **Day length is a world option** (RECONCILE 117): the create screen offers 24, 30, 36, 40, 45, 48, 60 or 72 real minutes a game day, default 24, saved as `yearofice_world.day_length_min`. At the default a game day is 28,800 ticks, a game hour 1,200 and a game minute 20; on a day of L minutes each is × L ÷ 24, so a 48-minute day has 57,600 ticks.

| Rate | Game time per real second | Game seconds per tick | Runs when |
| --- | --- | --- | --- |
| World clock | 1 minute | 3 | Default |
| Travel | 12 seconds | 0.6 | Moving with no job running |
| Close clock | 6 seconds | 0.3 | Minigames, a drawn bow, an alerted predator within 160 blocks, bleeding, in water, soaked below 0 °C |
| Work time-lapse | 5 minutes | 15 | A hold-to-work ring is filling |
| Work until done | 20 minutes | 60 | A queued job runs with Z |
| Sleep, wait by the fire | 60 minutes | 180 | Sleeping or waiting |

**Only the World rate scales with day length (RECONCILE 145).** The table's real-time columns are for the 24-minute day. On a day of L minutes the World rate is 720 ÷ L game deciseconds a tick, with L one of 24, 30, 36, 40, 45, 48, 60 or 72, so the division is always exact. Travel (6 ds a tick), Close (3), Work (150), Until done (600) and Sleep (1,800) stay fixed at every day length, so a bear charge, a time-lapse and a night's sleep feel the same on any world. Every number in every tab is stated per game minute, hour or day, so drains, fires, spoilage and growth keep their game-time values. The rates live in `data/yearofice/clock/rates.json`.

**Steps per tick.** Each server tick the clock adds its rate and posts `ClockEvent.Minute` once with the number of minutes crossed. The body, fires, rooms and spoilage step once per crossed minute, or in closed form when more than 10 cross at once.

**Rates and ticks.** Animals, projectiles and physics always run at 20 ticks a second. That is why the close clock exists: a bear charge plays in real time while needs barely move.

**Multiplayer, co-op-safe (RECONCILE 115).** Solo first, co-op after 1.0. The clock is one per world and every rate is requested per player through `TimeService.requestRate(player, rate, reason)`: a player's time-lapse and close clock are that player's own requests, and in single-player the world follows them. On a server the world rate is the slowest any player asks for, sleep skips only when every player sleeps, and the SOS jump is per player. The time-debt design for co-op is recorded in Minecraft development › Layer 1 as future work, not built in M0–M9.

### Driving vanilla time

**dayTime follows the sun.** Every tick the mod writes `level.setDayTime` so that vanilla's sun angle gives the same sky-light level as the true sun elevation from the daylight table. A 181-entry lookup (elevation −90° to +90° in 1° steps) maps elevation to vanilla time, using the morning half while the sun rises and the afternoon half while it sets.

**The midnight sun.** From about May 15 to July 28 the sun never drops below −6°, so vanilla sky light never reaches night. The custom sky renderer draws the sun on its true azimuth, low on the northern horizon at midnight.

**Sleeping.** Vanilla beds and their time skip are gone. Our beds (`yearofice:debris_bed`, `bough_bed`, `bunk`) put the player in the sleeping pose; `CanPlayerSleepEvent` allows any hour, and `SleepFinishedTimeEvent` cancels vanilla's skip. The sleep rate and the blueprint's wake rules run the night.

**Pausing.** In single-player the pause menu pauses everything. On Gentle the pack and messenger screens report `isPauseScreen` true, so they pause the world as the preset says.

### Calendar, daylight and moon

**The calendar stays data.** `data/yearofice/weather/interior_alaska.json` holds the month normals, the Markov tables, the 24-entry hourly curve and the 365-row daylight table (sunrise, sunset, and sun elevation and azimuth each hour at 64°N). Runtime code never computes sun angles.

**The moon.** The moon's phase follows a 29.53-day cycle from a seeded start, its rise and height from a second table. Vanilla's 8-phase moon is not used; the sky renderer draws ours.

**Night light.** `DimensionSpecialEffects.adjustLightmapColors` sets night brightness so sight matches the light-level table: 40 m under a full moon on snow, 8 m moonless, plus 10 m with an active aurora. The brightness option still works for players who need it.

### Seasons on screen

**Visual stages.** The client keeps a season stage from the synced date and the year's drawn dates: snow, melt, green-up, summer, turning, bare, first snow. Each change triggers a gradual re-mesh, nearest sections first, spread over 30 real seconds.

| What changes | How |
| --- | --- |
| Grass, moss and willow colour | `RegisterColorHandlersEvent.Block` colour handlers read the stage and the column's cover |
| Birch and aspen leaves | Colour handler for green and yellow; when bare, a wrapping baked model returns no leaf quads |
| Fireweed, berries, sedge | Block states set by the calendar pass (`ripe`, `seeded`), since they matter to gameplay |
| Snow cover | Real `snow_cover` blocks (above) |
| Ice | Real ice blocks (above) |

## Look and sound

### The look

**The open decision "Look" is resolved.** The game looks like Minecraft: 1 m blocks with the mod's own 16 × 16 textures in a muted naturalistic palette, GeckoLib animals at the same 16 pixels per block, and the custom sky. Shaders are optional through Iris; the game must look finished without them. The Director ticks the box at M0. Every texture, model and animation is made by the studio's generator scripts or is a licensed found asset, as Art and audio production sets out; no human artist is needed.

**The resource pack.** The mod's own blocks carry their textures in `assets/yearofice/`. The built-in resource pack `yearofice:look` ("Year of Ice look", added with `AddPackFindersEvent`, always on) holds the sky, lightmap, fonts, GUI frames and our own textures for the few vanilla blocks the world still uses, such as water. Those textures are generated to our palette, never copies of Mojang's files; a model may still point at a vanilla texture at runtime (RECONCILE 116; review: Minecraft development › The evolving modpack).

**Art budget.** The tab Art and audio production owns the asset list, the generators and the budget per milestone, counted from the master inventory (Minecraft development › Master registry inventory): about 190 texture sets for 163 entity types on 35 GeckoLib rigs, 107 plant blocks with their phases, about 150 other blocks, about 120 item icons, 15 HUD layers, 409 sound events and 12 music events (RECONCILE 141). Block and item art is 16 px; GeckoLib models use 16 pixels per block. Milestones in Minecraft, below, gives each milestone's share.

### Cold you can see

| Effect | Mechanism |
| --- | --- |
| Frost from the corners, tunnel vision | GUI layer `yearofice:frost`, a frost texture whose coverage follows the blueprint's 5/20/40/70% |
| Blue tint, colour wash | A post effect when available; otherwise a tint in the frost layer (Iris may disable post effects) |
| Camera tremor, sinking camera | `ViewportEvent.ComputeCameraAngles` and an eye-offset in the camera setup |
| Slowed steps, turning lag | Movement attribute modifiers and a mouse-sensitivity scale on the client |
| Breath fog | Particle `yearofice:breath` from the head of players, wolves and big animals, by air temperature and exertion |
| Frost on the hood edge | A thin texture in the frost layer at the top of the screen |
| Rewarming | The frost layer clears from the centre over 20 s |

"Reduce screen effects" halves the frost layer and turns off tremor, as written.

### Sky and aurora

**The custom sky.** `DimensionSpecialEffects` for the dimension (registered with `RegisterDimensionSpecialEffectsEvent`) draws the sky, sun, moon, stars, the low cloud deck and the aurora. Fog distance and colour come from `ViewportEvent.RenderFog` and `ComputeFogColor`, so a blizzard closes to 20 m.

**The aurora.** On aurora nights from the event data, animated curtain ribbons draw with additive blending between 100 and 300 km of virtual height. Strength follows the event's storm level; the forecast's "aurora chance" reads the same data.

**Weather on screen.** Biomes have no precipitation, so vanilla never draws rain or snow. The mod's `renderSnowAndRain` draws rain, snow and blizzard by our state and wind; splash and drip particles follow.

### The first-person body

**Hands.** Minecraft's first-person arm and held item are the base. `RenderHandEvent` draws the left arm even when the off-hand is empty, so both hands show, and draws gloves or mittens from the hand slots.

**Item animations.** Tools with real motion (bow drill, rod bend, cupped hands blowing on tinder, the knife draw-cut) are GeckoLib animated items. Simple swings use `IClientItemExtensions.applyForgeHandTransform`, with swing speed read from the work rate, so a tired swing visibly slows.

**Body numbers.** The eye stays at Minecraft's 1.62 m to keep poses and collision vanilla. Field of view stays the player's option, with 75° suggested on first launch.

**Hand-carried loads.** A log or quarter on the shoulder is drawn from the player's `yearofice:carry` attachment; the camera drops 10 cm and tilts 5°, as written.

### Sound

**Every sound is a file.** Minecraft plays `.ogg` files, so each sound is either rendered at build time from a synthesis recipe in `tools/soundgen/` or is a found recording under CC0, CC-BY or MIT with its row in the one credits file, `assets/yearofice/credits.json` (RECONCILE 116; Art and audio production). Our own field recordings may replace any of them later.

**Files.** Sounds sit in `assets/yearofice/sounds/<group>/<name>.ogg`, listed in `assets/yearofice/sounds.json`, registered as `SoundEvent`s with ids like `yearofice:animal.wolf_howl`.

**Danger at range.** Vanilla attenuation ends at 16 blocks times volume. Sounds with a range over 128 blocks (a howl at 3 km, ice cracking at 500 m) go out as a `yearofice:distant_sound` payload to every player in range. The client plays them with its own distance curve and direction, which also feeds the caption "\[Huff, close, left\]".

**Ambience.** Region and cover ambience beds play from a client manager by cover type, wind and weather, capped at 32 voices as before. Beds are stereo 60 s seamless loops; every other sound is mono so Minecraft can position it.

## Player systems

### Vanilla needs replaced

**Hunger.** Vanilla food level is ignored: the mod holds it at full and hides its bar. Eating any food runs our absorb step instead of vanilla's.

**Health.** Vanilla health stays at its maximum of 20 and is never shown. `LivingIncomingDamageEvent` cancels all damage to players and hands it to the body model as an injury or a Condition loss. Falls become sprains by the blueprint's rules.

**Death.** The body model decides collapse. In Endless the SOS flow runs; in Hardcore the world ends and the "why I left" line is asked. `LivingDeathEvent` is cancelled for any cause our model did not start.

**Endless SOS.** `body.SosFlow` runs the collapse (4 real seconds), the SOS flight screen and a closed-form jump of 14 game days for that player, about 2 real seconds, using the same catch-up rolls the sleep rate would have used (RECONCILE 83). Everything carried except worn clothing and the messenger stays at the collapse spot in a `yearofice:left_pack` block that never expires (RECONCILE 84). The player returns on the outpost gravel bar at BMI 19, Condition 80, with the starting kit, and every placed block is intact (M4 test). On a server the jump is per player (RECONCILE 115; review: Minecraft development › Collapse, SOS and the flight).

**Sprint and air.** Vanilla sprint stays as the run key, with our speed and energy cost. Air supply is hidden and replaced by the breath ring; `oxygen_bonus` gives the 30-second breath.

### The body attachment

The body record from The body tick is a player data attachment `yearofice:body` with a codec, `copyOnDeath` true. Its codec is versioned, and grime, nausea and fever are fields of it, not separate attachments or mob effects (RECONCILE 19); Body, injury and sickness adds the ten body parts' health to the same record. It is written only on the server, once per game minute, by `com.yearofice.body.BodyTicker`, which calls `sim.body.BodyModel.step`.

The owning client gets a `yearofice:body_sync` payload when any value changes by more than 1% or once per game hour. Other players see only what the world shows (breath, limp, shivering).

**Other player attachments.**

| Attachment | Holds |
| --- | --- |
| `yearofice:body` | The body record |
| `yearofice:pack_rules` | Unlocked pack rows, load in kg, hand-carry state |
| `yearofice:carry` | The hand-carried load (log, quarter, carcass, stone) |
| `yearofice:practice` | Practice level per action id |
| `yearofice:known` | Plants identified, recipes learned, notes found, gates open |
| `yearofice:map` | Revealed 8 m cells (2 bits each: seen, sighted), stored per 512-block tile only where something is revealed (World map tab), breadcrumbs, 20 waypoints |
| `yearofice:journal` | Journal lines, weight graph, year cards, first-bear flag |
| `yearofice:jobs` | The queue of up to 8 jobs |
| `yearofice:work` | The current hold-to-work job and its progress |
| `yearofice:tool_memory` | The last tool used for each job, so a repeated job picks it first |
| `yearofice:study` | Study, collection and footage progress (story.study) |

### HUD layers

Vanilla layers cancelled in `RenderGuiLayerEvent.Pre`: health, food, armor, air, experience bar and level, vehicle health and effects. Kept: hotbar, off-hand, crosshair, selected item name, chat, boss bar, title and the player list; vanilla subtitles are replaced by our `captions` layer. `client.hud.HudLayers` adds 15 layers in `RegisterGuiLayersEvent`, bottom to top (review: Minecraft development › Player systems and client › The HUD layer stack):

| # | Layer | Blueprint element | Where |
| --- | --- | --- | --- |
| 1 | `yearofice:headwear` | Head-net mesh, goggle slits, hood rim | Full screen, under the frost |
| 2 | `yearofice:frost` | Cold overlay, hood frost | Full screen |
| 3 | `yearofice:body_fx` | Fever shimmer, sickness dim, blackout fade, snow-blindness wash, bug specks | Full screen |
| 4 | `yearofice:threat_flash` | Pack-screen edge flash | Screen edge |
| 5 | `yearofice:vitals_strip` | Vitals strip icons and the exposure line | Top right; empty when all is well |
| 6 | `yearofice:body_figure` | Clothing and body-part figure | Top left, only when a zone or part needs attention |
| 7 | `yearofice:load_meter` | Load meter "22 / 30 kg" | Right of the hotbar |
| 8 | `yearofice:carry_slot` | The wide slot for a carried load | Over the hotbar |
| 9 | `yearofice:pickup_line` | "+3 birch bark" | Above the hotbar |
| 10 | `yearofice:messenger_line` | One unprompted Trail Messenger line | Top centre |
| 11 | `yearofice:prompt_line` | "RMB  Pick up flint (0.4 kg)", with the bound key's real name; row 2 for look readouts | Below the crosshair |
| 12 | `yearofice:work_ring` | Progress ring and clock face | At the crosshair |
| 13 | `yearofice:breath_ring` | Breath under water; hides the work ring | At the crosshair |
| 14 | `yearofice:captions` | Directional captions "\[Huff, close, left\]" | Bottom right |
| 15 | `yearofice:minigame` | Bow drill gauges, cordage marker, pull-and-brace, fishing tension arc, the call rhythm bar and the sling beat, one owner at a time | Lower centre |

F1 hides layers 4–15; layers 1–3 still draw, because they are the view, not the HUD. All 15 together stay under 0.3 ms a frame. Body, injury and sickness adds two strip icons, Bleeding and Fever, inside layer 5.

The objective beacon of the guided first night is a world-space light column drawn in `RenderLevelStageEvent`, not a GUI layer. When the messenger's battery is 0 the strip and the map pins vanish, as written.

### The messenger

**An item.** `yearofice:messenger`, called the Trail Messenger in the world (Ruhan's project rules), sits in one of the two Curios `neck` slots or in a hand. Its battery is a data component `yearofice:battery` in hundredths of a percent, drained by the server per game minute by the blueprint's rates; the Map page costs 4% a game hour while open (RECONCILE 83).

**The screen.** M opens `client.messenger.MessengerScreen`, a non-pausing screen (pausing on Gentle) with the main tab's pages: Vitals, Map, Forecast, Prompts, Notebook, Stores, Fires, Ambitions, Journal, Radio and SOS. Q and E or the wheel turn pages, 1–0 jump to one, M or Esc closes, and holding M for 1 s swaps °C and °F for 5 s. The left hand raises the device for 0.3 s first, so the bow cannot be drawn while it is open (review: Minecraft development › Messenger screens).

**The map.** The server renders map tiles of 64 × 64 blocks (1 pixel per 2 blocks) from the generator layers plus block changes, and sends only revealed tiles on request (`yearofice:map_tile` payloads). Pins for traps, nets and set pieces come from server records.

**The base from above.** Minecraft keeps no history, so on the first of each month the server renders a 256 × 256 colour snapshot of the home area and stores it in the journal (about 10 KB compressed). The slider steps through the snapshots.

### Controls

Minecraft's defaults win where the two collide, so a Minecraft player's hands already know the game. The blueprint's own actions get free keys, registered as NeoForge key mappings in the category "Year of Ice" and rebindable. Every dual key uses one threshold: under 0.25 s is a tap, longer is a hold, and any hold can be set to toggle. The full table with mapping ids is Minecraft development › Input and keybinds (review: rows changed to match it, RECONCILE 5, 31, 60).

| Action | Blueprint key | Default in this mod |
| --- | --- | --- |
| Move, jump | W A S D, Space | Same; Space held at a climbable spruce climbs |
| Run | Shift | Ctrl (sprint), or double-tap W |
| Crouch and stalk | C | Shift (sneak); also leaves a seat |
| Use: pick up, open, inspect, drink | E tap | Right mouse tap |
| Work (hold-to-work ring) | E hold | Right mouse hold, from 0.25 s |
| Tool or weapon: swing, chop, reel | Left mouse | Left mouse |
| Draw the bow, cast | Left mouse hold | Right mouse hold, as vanilla bows and rods |
| Place a building piece | Right mouse | Right mouse |
| Take hold of a load, shoulder it, set it down | — | Shift + right mouse with an empty main hand |
| Hotbar | 1–9, wheel | Same |
| Off-hand swap | X | F |
| Pack, gear, crafting | Tab | E |
| Eat the quick-slot food | Q | R (`key.yearofice.quick_eat`); right-mouse hold with food also eats |
| Drop | G | Q, Ctrl + Q for a stack; Q also drops a hand-carried load at once |
| Trail Messenger | M | M; hold 1 s swaps units |
| Rest: sit, wait, sleep | Z | Z; tapped during a work ring, work until done |
| Whistle for the wolf, wolf wheel | F | G tap, G hold |
| Talk, voice wheel (shout, howl, pish) | V | V tap, V hold |
| Bear spray | B | B |
| Light, beam mode | L | L tap, L hold 0.5 s (vanilla's advancements key is unbound on first launch) |
| Read the land or sky | R (Land and seasons) | X tap; X hold pins the line on the map |
| Binoculars | — | C hold, from the neck slot |
| Open coat / mittens off | One key each | O / N |
| Rotate a piece 90° | Wheel, Shift | Left Alt + wheel |
| Blueprint mode | — | K |
| Pause | Esc | Esc |

Assist options (hold to toggle, Assisted and Automatic minigames, captions) are a client config screen through NeoForge's built-in configuration screen. Touch controls are dropped.

### Inventory and packs

The blueprint's inventory already copies Minecraft's, so it maps directly onto the player's 36 main slots.

| Blueprint | Minecraft |
| --- | --- |
| Hotbar, 9 slots (belt and pockets) | Vanilla hotbar, slots 0–8 |
| Off-hand slot | Vanilla off-hand, slot 40 |
| Pack rows of 9 | Vanilla main slots 9–35 as rows A, B, C, opened by the worn pack's `rows` in `data_maps/item/pack.json` |
| No pack: 9 slots, 5 kg | Rows locked; hotbar only |
| Crate backpack: 18 slots, 30 kg | Rows A and B |
| Hide pack: 27 slots, 35 kg | All 3 rows |
| Pack frame: 27 slots, 45 kg | All 3 rows plus 3 lash slots shown in the pack screen |
| Chest 27, barrel 18, food cache 27 | Our container block entities |
| Sled 36, canoe 36, wolf saddlebags 6 | Container entities and a wolf inventory |

**Locked rows.** One mixin on `Inventory` limits free-slot search to unlocked slots, so pickups and shift-clicks never land in a locked row. The pack screen does not draw locked slots.

**Taking off a pack.** The pack item sits in the Curios `back` slot. Taking it off moves the contents of rows 9–35 into the pack item's `minecraft:container` component; putting it on moves them back. A dropped pack keeps its contents and floats or sinks by the Swimming rules.

**The pack screen.** E opens `client.pack.PackScreen` over the server menu `item.inventory.PackMenu` (RECONCILE 83). It shows the four panels: body, pack grid with the hotbar as the bottom row, crafting from the pack and storage within 8 m, and an open container. Vanilla's inventory screen is replaced on `ScreenEvent.Opening`, so the 2 × 2 crafting grid never shows.

**Stacks.** Stack sizes come from bulk, using the `max_stack_size` component (up to 99). Food by weight counts in 100 g units, so the blueprint's 8 kg slot is a stack of 80.

**Quiver and belt.** The belt is three typed Curios slots, `sheath` (knife), `quiver` and `belt` (one pouch: possibles bag, pebble pouch or game bag), each checked by its tag (RECONCILE 83). The quiver holds 12 arrows in its container component; `LivingGetProjectileEvent` feeds bows from it first, then the off-hand, so arrows never sit in the grid.

### Weight and carrying

**Weight is data.** Each item's weight in grams comes from the data map `yearofice:weight` (`data/yearofice/data_maps/item/weight.json`). A stack can override it with the component `yearofice:weight_g` (a fish of 4.1 kg, a pack full of gear).

**Load.** The server sums the hotbar, off-hand, worn gear, pack contents and lashed items each time the inventory changes. Load bands apply as movement-speed attribute modifiers (`yearofice:load`) and as a kcal multiplier in the body tick.

**Hand-carried loads.** A log (40 kg), quarter or carcass is not an item. Picking one up writes the `carry` attachment, removes the world object, shows the wide carry slot and blocks tools. Shift + right mouse with an empty main hand takes hold, Shift + right mouse again shoulders a medium carcass (3 s), Shift + right mouse on a block sets it down (1 s), and Q drops it at once as the world object again. Taking the drag rope needs 2 m of cordage in the hotbar or off-hand.

**Movement numbers.** Speeds are attribute modifiers on top of vanilla, so other mods' effects still stack.

| Movement | Blueprint | Mechanism |
| --- | --- | --- |
| Walk | 3.0 m/s | Base movement speed 0.0695 (vanilla 0.1 gives 4.32 m/s) |
| Run | 5.5 m/s | Sprint plus modifier `yearofice:gait` to ×1.83 of walk |
| Crouch | 1.2 m/s | Sneak at 40%, matching vanilla's 30% plus a modifier |
| Step | Smooth slopes | `step_height` 0.6; 1.0 on `#yearofice:terrain_steps` or with a walking stick |
| Jump | Mantle 1.2 m ledges | Vanilla jump (1.25 blocks) |
| Swim | 1.0 m/s, crawl 1.4 | `swim_speed` attribute modifiers by cold, load, clothes |
| Slope | 70% over 25° | Modifier from the 4-block height gradient |

### Clothing slots

Five zones with inner and outer layers make ten clothing slots. The four vanilla armor slots carry the outer layers of head, torso, legs and feet; Curios carries the rest.

| Zone and layer | Slot |
| --- | --- |
| Head outer (fur hat, head net, goggles) | Vanilla head |
| Torso outer (shell, poncho, coat, parka) | Vanilla chest |
| Legs outer (hide pants) | Vanilla legs |
| Feet outer (boots, moccasins, mukluks) | Vanilla feet |
| Head inner, torso inner, legs inner, feet inner | Curios `yearofice:head_inner`, `torso_inner`, `legs_inner`, `feet_inner`, 1 each |
| Hands inner, hands outer | Curios `yearofice:hands_inner`, `hands_outer`, 1 each; drawn on the first-person hands |
| Back (the pack) | Curios `yearofice:back`, 1 |
| Belt (knife, quiver, one pouch) | Curios `yearofice:sheath`, `quiver`, `belt`, 1 each |
| Neck (Trail Messenger, binoculars, lanyard whistle) | Curios `yearofice:neck`, 2 |
| Inner pocket (bottle, batteries, pocket stones, boots before bed) | Curios `yearofice:inner_pocket`, 2; items hold at +20 °C while warmth is 30 or more |
| Snowshoes, over the boots | Curios `yearofice:snowshoes`, 1; 20 s to put on or take off |

The ten clothing slots are the first six rows; the rest carry gear (review: Minecraft development › Inventory and slots).

Slot types are data in `data/yearofice/curios/slots/` and assigned to the player in `data/yearofice/curios/entities/player.json`.

**Garment numbers.** A garment's warmth, wind block, kept-when-wet, noise and scent come from the data map `yearofice:wear`. Wetness is a component `yearofice:wetness`; condition uses vanilla durability, so its bar is the familiar one. Armor points are 0, and the armor bar is hidden. The wolverine ruff is a component on the parka, not a slot.

### Work, previews and minigames

**Hold-to-work.** Holding right mouse on a target for 0.25 s (a shorter press is a tap) sends `yearofice:work_start` (target, action id). The server checks the action from `data/yearofice/action/<id>.json`, shows the work preview line first, and advances progress at the work rate. Releasing sends `work_stop`; progress stays on that block or entity for 1 game day.

**Interruptions.** Any danger alert ends the job on the server in the same tick and starts the close clock. The client drops the ring when the next `work_progress` payload says stopped, within one tick.

**Minigames.** Minigames run on the client for responsiveness and send their result. The server checks plausibility (duration, energy, wetness) and applies the outcome.

| Minigame | Form |
| --- | --- |
| Bow drill | A non-pausing transparent screen over the world; mouse or A/D strokes, right mouse pressure |
| Knapping | A screen with the core rendered in 3D, turned by the mouse |
| Cordage, pull and brace, blowing on tinder | In-world overlay in the `minigame` layer, driven by the use key |
| Fishing fight | In-world overlay; reel on left mouse, ease off on release, walk to follow |
| Splitting | In-world reticle on the round's end grain |
| Self-rescue on ice | In-world overlay with the pull-and-brace rhythm |

**Stations.** The vanilla crafting table is gone. Hands, fire, workbench, furnace, forge and canoe bed are stations; workbench, furnace and forge are block entities with menus. Station recipes use the custom recipe type `yearofice:station` (see Saves and data).

## World systems

### Animals

**Entities.** Every species is a GeckoLib entity (`GeoEntity`) extending `PathfinderMob`, in `com.yearofice.ecosystem.species`. Models and animations sit under `assets/yearofice/` in the folders the pinned GeckoLib version expects. Hitbox sizes follow real animals: moose 1.4 × 2.3 blocks, grizzly 1.3 × 1.5, wolf 0.8 × 0.9, hare 0.4 × 0.4.

**Brains.** Behaviour uses Minecraft's `Brain` system. The blueprint's agent states (rest, feed, drink, travel, alert, flee, hunt, charge) are activities; drives pick the activity each game minute. Custom sensors (`yearofice:sight`, `hearing`, `scent`) write memories such as `yearofice:threat` and `yearofice:food_target`.

**Trails.** Stage 7's trails are node lists. Active animals prefer them through a path-cost malus off trail; Nearby animals move along them by teleport steps.

**Tiers mapped to Minecraft.** The blueprint's three tiers below are the outline. Minecraft development › Entities and AI › Tiers, spawning and despawning splits them into eight forms (abstract counts, records, Nearby entities, Active, group members, bubbles, projections and impostors) under the same caps.

| Blueprint tier | Minecraft form | Rate |
| --- | --- | --- |
| Active, under 150 m | Real entities with full Brain, inside simulation distance (set to 10 chunks, 160 blocks) | Every tick |
| Nearby, 150–600 m | Real entities with AI off where their chunk is loaded (so the player sees a far herd), or records in SavedData where it is not; moved along trails once per game minute | Once per game minute |
| Abstract | Counts in SavedData `yearofice_populations` | Once per game hour |

Promotion at 150 m and demotion at 180 m stay. At most 40 Active and 200 Nearby, farthest demoted first; the wolf and anything stalking the player never count. Our entities never despawn on their own; only demotion removes them.

**Spawning.** No vanilla spawner runs. `ecosystem.PopulationSpawner` draws individuals from a region's count into fixed slots from `hash(seed, region, species, slot)`, so the same moose returns to the same willow flat.

**Hit zones.** GeckoLib bones exist only on the client, so the server uses hit-zone boxes in entity space from `data/yearofice/hitzone/<species>.json`, with one set per pose (standing, bedded, running).

**Arrows.** Arrows are a custom projectile `yearofice:arrow` at 50 m/s with drop and branch deflection on leaf blocks, as in Hunting in depth.

### Senses, scent and wind

**Wind.** The weather system gives one wind vector per hour, plus gusts. Valleys channel it: cover and terrain bend the direction by up to 45° on the 32 m field.

**Scent.** A scent cone runs downwind from the player; an animal smells the player if it sits in the cone within its species range scaled by wind speed. Scent points (kills, cleaning spots, smokers, caches) live in SavedData `yearofice_scent` on a 16 m spatial hash with a 24-hour decay, and bears path to strong points within 400 m.

**Sight and hearing.** Sight uses line-of-sight raycasts against blocks, with leaves counting as half cover, and at night the light from `EnvService.light` (sky, block and held light), never the held item or the renderer (request 87). Noise from actions (`noise_m` in action data) and footsteps by surface reaches hearing sensors.

### Populations

SavedData `yearofice_populations` holds counts per region and species, the birth calendar and the hare cycle phase. One file per region (`yearofice_region_<cell>`) holds that region's ranges, dens, lodges, beaver dams and outposts, so a region's state loads and saves as one unit. Records are tiered (DIRECTOR RULING 91): big game, predators, named and Known animals keep a full record for life; small game, small birds and fish keep a slot record only within 600 m of a player, and their count is the truth elsewhere.

### Traps and nets

**Block entities.** Snares, deadfalls, minnow traps, nets, weirs and tip-ups are blocks with block entities in `com.yearofice.food.traps`. Each stores when it was set, its last rolled dawn or dusk, bait, condition and any catch.

**Rolls.** Loaded traps roll on `ClockEvent.Dawn` and `Dusk`. A trap in an unloaded chunk does not tick, so on chunk load its `onLoad` runs one seeded roll per missed dawn and dusk with `hash(seed, pos, day, period)`, then the 48-hour theft rule. Reloading never rerolls a catch.

**Showing a catch.** The catch is a dead animal model on the block entity's renderer. The messenger never reports it.

### Fire

**One block, one machine.** A fire is `yearofice:fire_ring` (or `fire_pit` without stones) with a block entity in `com.yearofice.fire` that runs the blueprint's state machine: ember, tinder flame, small fire, fire, coals, banked, out. It steps per game minute, so fuel and timers follow the clock.

**Fuel and settings.** Fuel is a queue of armloads with wood type from `data_maps/item/fuel.json` and moisture from each piece's `yearofice:moisture` component, so seasoning is per stack (RECONCILE 127; Crafting and building in depth › Trees, felling and firewood). Low, Steady and Hot set heat, light and burn rate as written.

**Light.** The block's light level follows the state and setting: small fire 9, Low 9, Steady 12, Hot 15, coals 6, banked 0. Minecraft light falls 1 per block, which gives radii close to the blueprint's 5, 8 and 12 m.

**Heat.** Every lit fire registers in a heat index (in memory, rebuilt on load) that the warmth step reads: up to +25 °C within 3 m fading to 0 at 8 m.

**Spread.** Vanilla fire spread is off. Our spread rule runs per game hour on packed fires and places `yearofice:flame` blocks on burnable pieces within 2 m; flames deal 50 HP a game minute to wood pieces and burn out by data. A duff fire uses `yearofice:smoulder` blocks; with wind over 5 m/s it hands over to the wildfire event, which burns on 32 m cells off-screen and as flames near the player.

**Smoke.** Particles show the state: thin smoke for small fires, a column for green-wood teepees (raised to 120 blocks for the 8 km signal), sparks on Hot. Room smoke is part of the room model.

### Warm rooms

**Finding a room.** A flood fill runs over air blocks from the player's position or a heater, with 1 m cells replacing the 0.5 m grid. Every boundary face must be a block tagged `yearofice:room_walls` (walls, closed doors, windows, rock) and every floor cell needs a block tagged `yearofice:room_roofs` within 4 blocks above. A room may hold up to 50 floor cells and 200 air blocks.

**Openings.** Any air path out of the room means it is not a room, since the smallest gap is now 1 m. It then counts as shelter (+5 °C) with no heat model, as written.

**Cache.** Rooms live in SavedData `yearofice_rooms` with their block set. Place, break and door-state events inside a room's bounds plus 1 mark it dirty; it is rebuilt at most once a second.

**Heat model.** The crafting tab's formula stays. U and C per block face come from the data map `yearofice:heat` (`data/yearofice/data_maps/block/heat.json`); each exposed face counts 1 m². Chinking is a block state on log walls (`chink=none|moss|clay`), and seam metres per block come from the same map.

**Off-screen rooms.** A room in an unloaded chunk settles in closed form on load, from the hourly temperature record and its fuel, as the Engine rules say.

### Spoilage

**A component.** Food carries `yearofice:fresh`: state (raw, cooked, smoked, dried, frozen), progress from 0 to 1 in steps of 1/40, and the game minute it was last evaluated. Steps of 1/40 let vanilla stack merging work: food of the same kind and freshness band stacks.

**Evaluation is lazy (RECONCILE 70).** Nothing ticks. The Decay and freshness layer settles a stack when it is read (a tooltip, a stack merge, eating, a container opening, a chunk load) by integrating the temperature of where it sat since its stamp:

1. In a player's inventory, at the feels-like air or room temperature from the Environment field's hourly series.
2. In a container, at the container's temperature: room, cellar 2–4 °C, ice cave −2 °C (food laid on its glare-ice floor −6 °C, RECONCILE 75), or air.
3. Below −5 °C decay stops, and the tooltip shows the thaw countdown instead (RECONCILE 68).

**Showing it.** An item decorator (`RegisterItemDecorationsEvent`) draws the freshness bar under the icon, green to amber to red. The tooltip shows "spoils in 0.7 days".

### Weather

**One state machine.** `com.yearofice.story.Weather` runs the blueprint's chain from the year table: state, hourly temperature, wind, precipitation, forecast with its seeded blur. SavedData keeps only the year number and the carried anomaly; the table rebuilds from the seed.

**On the client.** A `yearofice:env_sync` payload each game minute carries air temperature, feels-like, wind vector, state, snow depth here and the season stage. Rain and snow drawing, fog, wind sound and the sky read it.

**Vanilla rain level.** The server sets the level's rain and thunder levels only for sky darkening and thunder flashes. Thunder sound is ours, heard from 10 km by the distant-sound rule.

**Blizzards.** Fog at 20 m, slanting snow, drifting by the lee class, and tracks wiped (sign decals removed in loaded chunks, and by date on load).

### Farming

**Crop blocks.** Barley, potato and the other crops are custom crop blocks with block entities in `com.yearofice.food.garden`. Each stores the sowing minute, the germination result (rolled once with the blueprint's formula), degree-days, water and weeds.

**No random ticks.** Growth advances on `ClockEvent.Day` from the day's temperature, and in closed form on chunk load. Frost kills by the day's low; stages show as block states.

**Garden beds.** `yearofice:garden_bed` is a dug soil block with a bed state; a cold frame or the hot-spring zone shifts its temperature by the blueprint's numbers.

### Fishing

**Rod and bobber.** The rod is `yearofice:rod` with tackle as components. Casting throws a custom bobber entity `yearofice:bobber`, which replaces vanilla's fishing hook.

**Bites.** While the bobber sits in water, a bite rolls each game minute from the fishing spot under it (stage 4's spots, stored in region layers), conditions, bait and the fish stock. Species and size come from `data/yearofice/fish/<id>.json`.

**The fight.** The fight runs as the in-world overlay with the fight profile from data, on the close clock. A landed fish is the item with the species id (`yearofice:arctic_grayling`), carrying `yearofice:fish_data` and its weight in `yearofice:weight_g` (DIRECTOR RULING 101).

**Fish in the water.** Visible fish are small GeckoLib entities near spots in the Active tier, drawn from the stock. They hold in pools, school below falls, and can be speared or grabbed in the salmon run.

**Passive gear.** Set lines, gill nets, minnow traps and weirs are trap block entities (above). Ice fishing uses `yearofice:ice_hole`, a block that skins over by the ice rules.

### Swimming and cold water

**Movement.** Vanilla swimming stays, with our speeds by attribute modifiers: cold shock halves stroke speed, a pack over 15 kg or soaked winter clothes halve it again. The current push applies on top.

**The cold-water clock.** The body model runs the entry gasp, cold shock and 10 useful minutes on the close clock, as written. Water temperature by month comes from the weather data and the water body.

**Sweepers.** Fallen trees in a river are blocks tagged `yearofice:sweepers`. A swimmer pushed into one from upstream starts the pull-and-brace overlay.

**Under ice.** Ice blocks are solid, so there is no surfacing except at the hole. The breath ring counts down.

### Tracks and sign

**Decals, not blocks.** Tracks, blood drops, droppings, rubs and the player's footprints are sign records in a chunk data attachment `yearofice:sign`: kind, species, position, yaw, game minute. The client draws them as ground decals in `RenderLevelStageEvent` within 48 blocks.

**Ageing.** Each kind fades by its blueprint timer (blood gone at 2 game hours, tracks in snow until the next snowfall or wind). Records past their age are dropped on save, so the attachment stays small.

## Building

### The 1 m grid

Minecraft's 1 m block grid replaces the 0.5 m grid. Vertical half steps use slabs and stairs; the 0.1 m fine grid and free placement are dropped. Piece costs are scaled per block so the first cabin keeps its 160-hour schedule.

| Blueprint piece | Block form |
| --- | --- |
| Corner stone, gravel pad, log crib | One full block each; a crib reaches 1 block of drop, two stacked reach 2 |
| Sill log, ridge pole, purlin | Horizontal log blocks with `axis`, one per metre |
| Log wall course (1 m × 0.28 m) | `yearofice:log_wall`, a full block holding 3.3 course-metres: 0.83 peeled log, 50 game minutes, 373 kcal, 3.3 m of seam |
| Gable log | `log_gable` stairs |
| Roof panels (pole-and-bark, sod, thatch) | Block, slab and stairs of each, costed at 0.6 of the m² value per block |
| Plank wall, pole-and-bark wall | Full blocks, 1 m × 1 m |
| Floors | Plank floor block replacing the top soil block; dirt floor is the ground |
| Doors, hide flap | Two-block door blocks; barred state on the plank door |
| Hide window, plexiglass window, shutter | Pane-style blocks; shutter as a trapdoor |
| Fireplace | A 2 × 1 × 2 multiblock with a controller block entity |
| Barrel stove, box stove | One block with a block entity each |
| Mud-brick chimney, stovepipe | Chimney blocks; a thin pipe block with a roof-collar state |
| Chinking | `chink` state on `log_wall` |
| Shelf, bunk, roof brace, trophy mount | Furniture blocks; the bunk is two blocks long like a bed |
| Raised cache, woodshed, root cellar, hen house, goat shed | Built from pieces, recognised by a pattern check; the root cellar clears soil only |
| Log bridge, skid trail | Log and plank blocks allowed over water; a path block |

**Support.** The support numbers stay, per block: foundations 100, each log wall block 7, roof and floor 20 per metre from the nearest wall, ridge, purlin or post. A piece below 0 cannot be placed. There is no collapse; pieces fail only by damage.

**Piece damage.** Most blocks have no block entity. Damage is kept in SavedData `yearofice_piece_damage` keyed by position, written only when a piece is damaged and cleared when repaired.

### Placing and ghosts

1. Selecting a piece in the hotbar shows a ghost within 6 blocks, drawn translucent in `RenderLevelStageEvent`: white valid, amber with a warning, red with a reason.
2. Left Alt plus the wheel turns it in 90° steps.
3. Right mouse places a frame block `yearofice:frame` whose block entity names the target piece and the materials still missing.
4. Hold right mouse at the frame to build it, pulling materials from the pack and storage within 10 blocks. The frame turns into the piece.
5. The 1-hour full refund and the 50% salvage stay.

The amber warnings (flood line, dead tree overhead, smell upwind, wood near fire, chimney too low) run in the placement check.

### Blueprints and queued jobs

**Saving.** In build mode the player marks two corners; up to 400 piece blocks inside are saved as a structure template in `<world>/generated/yearofice/structure/blueprint/<player>/<name>.nbt`.

**Stamping.** Stamping places frame blocks for every piece, turned in 90° steps, and lists missing materials on the messenger. "Build all" queues the frames foundations first, under the queued-job rules.

**The queue.** `yearofice:jobs` holds up to 8 jobs. A running queue sets the clock to the work-until-done rate and stops for the interrupts the crafting tab lists.

### The first cabin in blocks

The 3.6 × 4.2 m cabin becomes 5 × 6 blocks outside and 3 × 4 inside, the same 12 m² of floor. Walls are 2 blocks high, with gables under a ridge.

| Part | Blocks | Blueprint equivalent |
| --- | --- | --- |
| Wall blocks after door and window | 33 | 109 course-metres, about 27 game hours to lay |
| Seam to chink | 109 m | 109 m, 11 game hours |
| Roof blocks | 30 | 18 m² of sod roof, 25 game hours |
| Logs for walls | 28 | Part of the 45 wall logs |

The M8 room test keeps its numbers (+26 to +32 °C on 2 armloads at −30 °C). If the 1 m geometry misses them, the Building sub-agent retunes C and the base leak in `heat.json`, never the test.

## Saves and data

### What is saved where

Minecraft's world folder is the save. IndexedDB, the 5-store layout and the `.yoi.json.gz` export are retired; a world is exported as its folder.

| Blueprint store | Minecraft form |
| --- | --- |
| meta (seed, versions, mode, realism) | SavedData `yearofice_world` |
| state (clock, year profile, populations, outposts, radio) | SavedData `yearofice_clock`, `yearofice_populations`, `yearofice_region_<cell>`, `yearofice_radio` |
| chunks (felled trees, pieces, ground items, worn trails) | Minecraft chunks; mod data in chunk attachments (`current`, `season_stamp`, `sign`, tree ids, trail wear per 2 m cell in `trail_wear` (RECONCILE 96)) |
| player | Player data attachments (above) |
| journal | The `journal` attachment, plus monthly snapshots |
| backups | Minecraft writes `.dat` files atomically; the mod keeps 3 rotated copies of its SavedData under `<world>/yearofice/backups/` |

**Autosave.** Minecraft autosaves every 5 minutes; the mod also forces a save on sleep, on crossing a region border and before any fast-forward.

**Save budget.** The mod's own data (SavedData and attachments) must stay under 5 MB after five years, the old budget. Minecraft's region files are outside it: they must stay under 2 GB with all 16 home-valley regions generated, plus under 1.5 GB of pruned Interior and Beyond chunks (World map tab), using LZ4 region compression where the pinned version offers it.

### Data kinds

Content is loaded by our data loaders (`AddReloadListenerEvent`), one JSON per entry, validated by codecs at load. A bad entry fails with the file, the entry id and the field path, as the open-world tab requires. Minecraft development › Data kinds is the full list of kinds with owners and milestones; this table is the outline (review: rows changed to match it).

| Kind | Path | Replaces |
| --- | --- | --- |
| Species | `data/yearofice/species/<id>.json` | `species.json` |
| Fish | `data/yearofice/fish/<id>.json` | `fish.json` |
| Plants, crops | `plant/`, `crop/` | `plants.json`, `crops.json` |
| Set pieces | `setpiece/<id>.json` plus `structure/setpiece/<id>/` | `setpieces.json` |
| Regions, archetypes, worldgen | `region/`, `archetype/`, `worldgen_params/valley.json` | `regions.json`, `archetypes.json`, `worldgen.json` |
| Weather | `weather/interior_alaska.json` | `weather.json` |
| Events, ambitions, prompts, notes | `event/`, `ambition/`, `prompt/`, `note/` | `events.json`, `ambitions.json`, `prompts.json`, `notes.json` |
| Actions | `action/<id>.json` | `actions.json` |
| Body, presets, injuries, remedies | `body/standard.json`, `preset/`, `injury/`, `remedy/` | `body.json` and the rest |
| Hit zones, butchering, taming, drops | `hitzone/`, `taming/`, `drop/`; yields in `loot_table/butchering/<species>.json` (the `butcher/` kind is retired, RECONCILE 79) | Same names |
| Recipes | `recipe/<id>.json`, type `yearofice:station` | `recipes.json` |
| Item and block numbers | `data_maps/item/weight.json`, `wear.json`, `fuel.json`, `food.json`; `data_maps/block/heat.json`, `piece.json` | Fields inside `items.json`, `pieces.json`, `heat.json`, `fuels.json` |
| Loot | `loot_table/blocks/`, `loot_table/setpiece/`, `loot_table/cache/` | Loot fields in set pieces |
| Tags | `tags/block/`, `tags/item/`, `tags/entity_type/` | Tag arrays |
| Item families, content flags, retired ids | `item_family/<species>.json`, `flag/<id>.json`, `retired/<kind>.json` | New kinds (DIRECTOR RULING 101) |
| Multiblocks, decay curves, valley memory | `multiblock/<id>.json`, `decay/<curve>.json`, `valley_memory/<id>.json` | Doubled paths in Structures and Creative's `decay/` folder (RECONCILE 22, 76, 101) |

**Synced to the client.** Kinds the client needs (species names, fish, plants, notes, item numbers) are sent in a `yearofice:data_sync` payload on login and after `/reload`. Data maps sync by NeoForge's own rules.

**Worldgen data is frozen.** Regions, archetypes, set-piece placement and worldgen parameters are copied into `yearofice_world` when a world is created, so a later `/reload` or mod update never moves land.

**Why not datapack registries.** Datapack registries load only at world start and live under `data/yearofice/yearofice/<kind>/`. Our loaders keep the simpler path and reload with `/reload`. Minecraft's own registries (biomes, structures, world preset, dimension type) stay datapack registries.

**The condition language.** `data/expr.js` becomes `com.yearofice.data.expr`, the same grammar and facades, parsed at load, never evaluated as code.

**Recipe entry.**

```json
{
  "type": "yearofice:station",
  "station": "yearofice:workbench",
  "tier": "bushcraft",
  "inputs": [ { "item": "yearofice:seasoned_stave", "count": 1 }, { "item": "yearofice:bowstring", "count": 1 } ],
  "tool": "yearofice:knife",
  "minutes": 240,
  "effort": "light",
  "kcal": 400,
  "skill": "woodcraft",
  "taught_by": null,
  "result": { "id": "yearofice:longbow", "count": 1 }
}
```

**Species entry.**

```json
{
  "entity": "yearofice:snowshoe_hare",
  "habitat": ["riparian", "spruce_taiga"],
  "population": [25, 40],
  "home_range_m": 150,
  "active": ["dawn", "dusk", "night"],
  "weather": { "rain": 0.4, "snow": 1.0, "clear": 1.0 },
  "behavior": "flee",
  "uses_trails": true,
  "yield": { "meat_kcal": 1000, "fat_pct": 2, "items": ["yearofice:hare_pelt"] },
  "respawn_days": 6
}
```

**Data map entry.**

```json
{
  "values": {
    "yearofice:birch_bark": { "weight_g": 100, "wet_takes_h": 4 },
    "yearofice:hide_coat": { "weight_g": 3500 }
  }
}
```

## Testing and the studio rules

### Four layers of tests

| Layer | Runs | Command | Covers |
| --- | --- | --- | --- |
| Unit and sim | JUnit, no Minecraft | `./gradlew test` | Every formula, worldgen stages and hashes, the balance simulator (one game year per real minute or faster), data validation |
| GameTests | Headless server | `./gradlew runGameTestServer` | Blocks, entities, fire, traps, rooms, ice, inventory, recipes in a real world |
| Client smoke | A real client under a virtual display | `./gradlew runClient` with `-Dyearofice.smoke=true` | Boot, world creation, HUD, screens, screenshots |
| Server boot | Dedicated server | `./gradlew runServer` with a 600-tick auto-stop | No client classes on the server |

**GameTests.** On the 1.21.1 pin, tests are annotated holder classes (`@GameTestHolder("yearofice")` with `@PrefixGameTestTemplate(false)`), batched by milestone: `m0` to `m9`, plus `m2_5` for the vertical slice. Their templates live in `data/yearofice/structure/gametest/`. Each test waits on game time through the clock, never on fixed tick counts, matching the old "wait on game time" rule.

**The balance simulator.** `tools/sim` runs `com.yearofice.sim` headless with scripted players (starvation, good player, three years, 100 years of events, five-year homestead), writing a CSV row per game day.

**Playtest bots.** Playwright is retired. Bots are server-side fake players (`FakePlayer` with scripted inputs) in GameTests and in a long-run harness, with the three personas: Newcomer, Wanderer, Grinder. Each logs real time per chore and every collapse with its cause.

**Screenshots.** The client smoke takes 1280 × 720 screenshots of day, night, a danger moment, a blizzard and the messenger at GUI scales 2 and 4. The Director looks at them, which replaces the phone-width check.

### SMOKE PASSED

The studio rule stands: nothing ships without it. The smoke command `./gradlew smoke` now prints "SMOKE PASSED" only when all of these pass:

1. `build` compiles, datagen output matches the committed files, and the lint finds no banned call in worldgen.
2. Every JUnit test passes.
3. `runGameTestServer` passes every GameTest of the current and earlier milestones.
4. The dedicated server boots seed 3141592, runs 600 ticks and stops with no error or warning from `yearofice` in the log.
5. The client boots the same seed, runs 600 ticks of play, and logs no error, no unregistered event, no NaN in the body record, and no unhandled payload.
6. **No monsters.** During both boots, a counter of every entity that joins the level reports zero vanilla mob types and zero entities in `yearofice:banned_vanilla`, and the `no_monsters_full_night` GameTest has passed.

Only one client test runs at a time, as before. Minecraft development › SMOKE PASSED adds items 7–12: a clean data report, the registry inventory diff, every fixture save loading, the performance gates, the 12 screenshots, and the pack installing into a clean folder on Ruhan's machine.

### Performance budget

| Item | Budget |
| --- | --- |
| Server tick | 20 TPS; MSPT average under 30 ms and never over 50 ms in normal play with 40 Active and 200 Nearby animals |
| Mod share of a tick | Body and clock 1 ms, animals 8 ms (land 4.0, birds 2.5, fish 1.5; DIRECTOR RULING 91), rooms and fires 2 ms, snow and ice passes 3 ms, catch-up 2 ms per chunk spread over ticks |
| Client frame | 60 fps on a mid laptop at 12-chunk render distance plus the horizon mesh, with and without Sodium |
| Hitches | None over 50 ms on chunk loads, region pre-warm, season re-mesh or a snow pass |
| Region layers | Under 3 s per region on a background thread |
| World creation | Core layers and the start site in under 8 s before the loading screen ends |
| Memory | Server heap under 3 GB, client under 4 GB with the default render distance |
| Mod save data | Under 5 MB after five simulated years |

spark measures the server lines in CI; a frame-time logger in the client smoke measures the rest.

### CI

Every push runs `./gradlew smoke` on Linux. Nightly, and before each milestone sign-off, the worldgen hash test runs on Linux, Windows and macOS (x64 and ARM) and must give the same hashes, replacing "Chrome and Firefox". `tests/hashes.json` keeps layer hashes for 20 fixed seeds per generator version.

### Studio rules that change

| Rule | Was | Now |
| --- | --- | --- |
| 4 File ownership | Files under `year-of-ice/` folders | Java packages and data folders, by the table below |
| 6 Saves stay compatible | New state under `S.ext.<area>` | New state in new attachments or SavedData fields with codec defaults; ids are never renamed |
| 7 One source of truth for the ground | Generation only in JavaScript | Generation only in `com.yearofice.worldgen` on the server; the client never computes terrain, only draws what it is sent |
| 8 Nothing ships without SMOKE PASSED | Browser smoke at 960 × 540 | `./gradlew smoke` as above, including the no-monsters check |
| 9 Look before shipping | Day, night, danger, phone width | Day, night, danger, blizzard, messenger at two GUI scales |

**Ownership by package.** Each department owns the top-level packages of the lead engineer's package map (Minecraft development › Package and module map, RECONCILE 80) and their data folders. `core`, `api`, `net` and `item` belong to the studio lead and `data` to the build developer; CODEOWNERS is generated from `studio/ownership.json` (review: table replaced to match the map).

| Department | Packages | Data |
| --- | --- | --- |
| Studio lead | `core` (with `core.debug` for `/yoi`), `api`, `net`, `item` (with `item.inventory`, written by the client developer) | Item data maps (`weight`, `props`, `food`, `wear`, `fuel`, `tool`, `light`), `flag/`, `retired/` |
| Build developer | `data` | The kind registry, `tools/content-lint`, `pack/`, `studio/` |
| World generation | `worldgen`, `world`, `client.horizon` | `worldgen_params`, `archetype`, `region`, `district`, `setpiece`, `structure/setpiece`, `worldgen/`, `terrain_process`, `water_feature`, `succession` |
| World and story | `story`, `env` | `clock`, `weather`, `event`, `ambition`, `prompt`, `note`, `phenomenon`, `sky_sign`, `microclimate`, `legend`, `study` |
| Ecosystem | `ecosystem` | `species`, `hitzone`, `taming`, `call`, `encounter`, `scent`, `senses`, `index`, `migration`, `hatch`, `rig`, `arrival` |
| Food and hunting | `hunt`, `food`, `decay` | `fish`, `water_life`, `fish_run`, `item_family`, `decay`, `loot_table/butchering` |
| Body and survival | `body`, `fire`, `forage` | `body`, `preset`, `injury`, `remedy`, `plant`, `crop`, `plant_sign`, `dye` |
| Building and crafting | `build`, `travel` | `recipe`, `action`, `multiblock`, `aging`, `livestock`, the heat, piece and decay block maps |
| Look, sound and UI | `hud`, `sound`, `client` (except `client.horizon`) | `assets/yearofice/`, `curios/` |
| Testers | `gametest`, `sim` scripts, `tools/` | `structure/gametest` |

## Milestones in Minecraft

Each milestone keeps its goal, its main test and its human test. The JavaScript module lists are replaced by the packages, registries, data and GameTests below. Out-of-scope lines stay as written unless noted. **The order is buildable:** each milestone uses only what earlier ones shipped, as its Needs line says. **Content budgets** come from the master inventory (about 1,152 ids) and the entity registry (163 entity types on 35 rigs); `studio/inventory.json` tags every id with its milestone, and the registry check fails a build that registers an id planned for a later milestone (Minecraft development › The content linter). Every milestone also ships the `/yoi` commands and `yearofice-common.toml` keys of what it delivers, and the art and sound of its content by the budget in Art and audio production, generated placeholders first.

**Order and content budget.** Entity types and rigs follow Minecraft development › The entity registry; "new rigs" counts rigs first used in that milestone. Entity texture sets include seasonal and age variants. Ids are planning budgets; the inventory's milestone tags are the truth.

| Milestone | Needs | Entity types (new rigs) | Plant blocks | Entity texture sets | Ids, about |
| --- | --- | --- | --- | --- | --- |
| M0 Seeded world | — | 0 | 0 | 0 | 15 |
| M1 Water and set pieces | M0 | 0 | 0 | 0 | 20 |
| M2 Walkable valley | M1 | 0 | 0 (trees are blocks) | 0 | 150 |
| M2.5 Vertical slice | M2 | 4: `snowshoe_hare`, `willow_ptarmigan`, `moose` (passive), `carcass_small` (3: `small_rodent`, `grouse`, `ungulate`) | 8 | 5 | 55 |
| M3 Start site, fire and onboarding | M2.5 | 0 | 0 | 0 | 80 |
| M4 Body | M3 | 0 | 0 | 0 | 60 |
| M5 Food and goals | M4 | 35 (10) | 72 | 43 | 285 |
| M6 Danger and big game | M5 | 102 (18) | 0 | 117 | 195 |
| M7 The year | M6 | 17 (2: `bat`, lamprey) | 27 (fungi and aquatics) | 21 | 120 |
| M8 Building and outposts | M7 | 0 | 0 | 0 | 140 |
| M9 Homestead and beyond | M8 | 3: `goat`, `reindeer`, `hen` (1: `hen`) | 0 | 4 | 32 |
| **Total** |  | **161 (34)** | **107** | **190** | **1,152** |

### M0 — Seeded world

- **Goal:** prove determinism and the four archetypes before anything is drawn.
- **Needs:** nothing; Ruhan's Forge 1.20.1 MDK is converted to the 1.21.1 pin in this round.
- **Deliver:** the Gradle project on the 1.21.1 pin; `core.Mulberry32` and `core.StageHash` (FNV-1a); `worldgen.Noise`, `Pipeline`, `Archetype`, `Landform`, `Erosion`; `data/yearofice/worldgen_params/valley.json`, `archetype/*.json`; the data loaders, kind registry, content flags and retired ids (Minecraft development › Content pipeline); `core.debug.YoiCommands` with `/yoi seed` and `/yoi map`; `config/yearofice-common.toml` with the preset switch; `tools/debugmap` (a desktop PNG renderer of the layers with seed and reroll); the B1 empty-world pack built and run on Ruhan's linked computer.
- **Tests (JUnit and GameTests):** the same seed gives the same height hash on Linux, Windows and macOS JVMs; over 200 seeds each archetype appears 25% ±5%; stages 1–3 finish under 8 s; changing an erosion parameter leaves the landform hash unchanged; heights within 120–900 m in the core and the floor at 150 ±30 m; no NaN; the lint passes; `data/all_kinds_load`, `data/reload_atomic`, `data/sync_hash_matches`.
- **Human test:** unchanged (8 debug maps, name 6).

### M1 — Water and set pieces

- **Deliver:** `worldgen.CoarseRivers`, `Lakes`, `Rivers`, `Falls`, `FishSpots`, `RockFaces`, `SetPieces`, `Landmarks`; the first set-piece templates in `structure/setpiece/`; `setpiece/*.json`; debug-map layers.
- **Tests (JUnit):** the M1 tests as written, with "across browsers" read as "across JVMs".
- **Human test:** unchanged.

### M2 — Walkable valley

- **Needs:** M1.
- **Deliver:** `YoiChunkGenerator`, `YoiBiomeSource`, the dimension type and world preset; biome JSONs and the no-spawns biome modifier; tree builders; terrain blocks (deep ground, deep rock, face rock, soils, surfaces); the current attachment and push; the custom sky and lightmap; the clock with per-player rate requests, the day-length option and the `dayTime` mapping; the weather file's month normals, hourly curve and daylight table, which the Environment field samples for air, wind and light (the Markov chain, forecast and storms wait for M7); the catch-up framework (`world.catchup`, `ChunkCaughtUpEvent`, `season_stamp`, `LoadGate`), because the SOS jump in M4 needs it; the horizon mesh; movement attributes; hold-to-work with previews; felling and bucking; weight, load bands, hand carry, locked pack rows and the Curios `back` and `neck` slots; the Trail Messenger item, screen, map tiles and battery; the HUD bus and all 15 GUI layers registered (each draws once its system ships); the dropped-item rule; key mappings; the `yearofice:look` pack; its sounds by the art budget.
- **Tests:** 60 fps at 12 chunks with the horizon on a mid laptop; no hitch over 50 ms crossing chunks or pre-warming a region (client smoke frame log). GameTests: the four load bands within 2% of the table; map reveal, breadcrumbs and waypoints survive a server restart; killing the server mid-save leaves a loadable world; a typical-use bot drains a battery in 6.5–7.5 game days; a clear June day charges 0 to 100% in 2 h ±10%, a clear December day adds 10–20%; `clock/rates_exact` on the 24- and 72-minute days; `clock/multiplayer_slowest`; `layers/item_expiry`; `no_monsters_full_night` passes. Dropped: the 200 KB save test for 1,000 felled trees; it becomes "mod save data grows under 20 KB for 1,000 felled trees".
- **Human test:** unchanged.

### M2.5 — Vertical slice

- **Goal:** one playable loop end to end before the full systems, so the fun is tested early: arrive, make fire, catch and cook food, and live through the first night (RECONCILE 106). Its content matches the asset budget in Art and audio production.
- **Needs:** M2. Each part is the first, smaller form of a system that M3 to M6 complete; nothing here is thrown away.
- **Deliver:** a 1 km test valley, world preset `yearofice:slice_valley`, cut from one home-valley region's layers (1,024 × 1,024 blocks with a creek and one fishing spot, spruce and birch, a willow flat and a gravel bar), with the outpost template from M1 placed as the start site; fire from parts in `fire` (birch-bark tinder, kindling and fuel armloads, lit with matches or the ferro rod, no bow drill yet; small fire to coals; Low, Steady and Hot); the trail snare block entity with dawn and dusk rolls in `food.traps`; one rod method (hook, line and bobber with worms, the fight overlay) in `hunt.fishing`; cooking on the fire ring (roast and boil) with `yearofice:fresh` for raw and cooked meat and fish, `decay/meat.json` and `spoil_days_at_5c`; the body tick for calories, warmth and water only (`sim.body` steps 1–6, 12 and 13; no illness or injuries, Condition held at 100); from a fixed-slot spawner (`ecosystem.population.SlotSpawner`, the first form of `PopulationSpawner`) the snowshoe hare on the `small_rodent` rig and the willow ptarmigan on the `grouse` rig, both snared or hit with a thrown stone, with `carcass_small` and field dressing, and one moose on the `ungulate` rig that walks, browses, alerts and flees but cannot be hunted until M6; the guided first night (`story.Onboarding` with the objective beacon and its 6 prompts); the test valley's 8 plant blocks (`feather_moss`, `haircap_moss`, `sphagnum`, `bluejoint`, `horsetail`, `dwarf_birch`, `green_alder`, `bog_blueberry_bush`); `/yoi set calories|warmth|water`, `/yoi spawn snowshoe_hare|willow_ptarmigan|moose`, `/yoi trap roll` and `/yoi fire state`; the `[body]`, `[fire]` and `[fishing]` config keys; generated art and sound for all of it.
- **Tests (GameTests, batch `m2_5`):** `fire/slice_fire_from_parts` (bark, kindling and 2 armloads reach a fire in under 10 game minutes and coals in 60 ±10); `food/slice_snare_hare` (a snare on a hare run catches 2–5 hares over 10 game days, rolled at dawn and dusk only); `hunt/slice_rod_bite` (worms at the spot give a first bite within 30 game minutes in 90% of 200 runs); `food/slice_cook_meat` (raw hare roasts to cooked in 20 game minutes, and cooked meat lasts its `spoil_days_at_5c` at 5 °C ±5%); `body/slice_day_balance` (a scripted day of chores burns 2,600–3,200 kcal, and a fed, dressed player ends it above 70 warmth and 60 water); `story/slice_first_night` (a bot following the prompts has a fire before dark and sleeps with warmth above 40); `ecosystem/slice_moose_flees` (the moose flees a player who closes to 40 m and never charges).
- **Human gate (fun):** Ruhan plays a fresh slice world for 60 real minutes with no help. It passes if he lights a fire from parts before the first dark, eats cooked hare, ptarmigan or fish by day 3, and says he wants to keep playing. Otherwise the next round tunes the slice before M3 starts.

### M3 — Start site, fire and onboarding

- **Needs:** M2.5.
- **Deliver:** `worldgen.Trails`, `Ranges`, `StartSite`, the outpost template and the `sos_return` bar; `build.Stations`, the `yearofice:station` recipe type and the workbench; the rest of the `fire` package (every build, spread, banking, smoke, the fire roster, the bow-drill screen); knapping; water containers and boiling; light items and held light (`env.light`, `client.light.HeldLight`); `story.Onboarding` moved onto the real start site; the starting kit items and their data maps; their sounds by the art budget.
- **Tests (GameTests):** all M3 tests as written. The 50-seed placement test and the per-check report run in JUnit on layers.
- **Human test:** unchanged.

### M4 — Body

- **Needs:** M3.
- **Deliver:** `sim.body` (every formula and all 13 steps of the tick); `body` adapters (`BodyTicker`, `DamageRouter`, `SosFlow` with the closed-form 14-day jump and the `left_pack` block, `BodyService.wake`); `food.Eating` and drinking; the `body` attachment with the ten body parts of Body, injury and sickness, and its sync; illness, injuries and treatments; vitals strip, body figure, `body_fx` and frost layers; `body/standard.json`, `preset/*.json`, `injury/`, `remedy/`; the weather file's `ground` block for sleeping on the ground; the starting clothes with wear data and the Curios clothing slots; `story.Journal`; `/yoi body` and the `[body]` presets.
- **Tests:** JUnit sim for starvation (BMI 17 between day 15 and 21 on Standard, about twice as late on Gentle), BMR 1,794 ±1, scurvy on day 30 or 31, dehydration blackout. GameTests: an Endless SOS returns the player after 14 game days at BMI 19 with the kit, the carried gear in a `left_pack` at the collapse spot and every placed block intact, the jump taking under 3 real seconds; a Hardcore SOS ends the world and writes the line; every vital change over 5 points in a game hour posts a cause; a soaked player loses 6 warmth a minute; Body, injury and sickness tests 1–7.
- **Human test:** unchanged.

### M5 — Food and goals

- **Needs:** M4.
- **Deliver:** the Decay and freshness layer complete (lazy settling, every curve, the tooltip); cooking at fire stations; the World edits op queue with its first producers (`DECAY` for gut piles, `DRIFT` for sunk gear); Ecology's core (populations, region records, tiers, slot records, `PopulationSpawner` replacing the slice's spawner, harvest) and the scent points; 35 more entity types on 10 new rigs, the M5 rows of the entity registry less the slice's hare and ptarmigan (small game, the small mustelids, grouse and ptarmigan, the resident fish, schools, the wood frog, the yellowjacket, carcasses tiny to medium); 72 plant blocks with their harvest verbs; trap block entities with dawn and dusk rolls and chunk-load catch-up; throwing and the light bow; rod methods, tackle components and passive fishing blocks; smoke and drying racks; the food cache; garden beds and crop blocks; prompts, logbook notes and the notebook tab; `fish/`, `plant/`, `crop/`, `prompt/`, `item_family/` data.
- **Tests (GameTests and JUnit):** all M5 tests as written, the 10,000-roll ratios in JUnit, the placed-block checks (a gill net never rolls with 3 segments; the cache loses 2–3% a week to mice, none when ringed) in GameTests, `data/data_only_species_spawns`.
- **Human gate:** unchanged.

### M6 — Danger and big game

- **Needs:** M5.
- **Deliver:** the full Brain setup, sensors and memories; the eight tier forms with promotion, demotion, records and group members; the senses on the scent and sound field; sign decals; predation; the encounter director and first-3-days bear; 102 more entity types on 18 new rigs, the M6 rows of the entity registry less the slice's moose, which now becomes huntable (big game, predators, most birds, `carcass_large` and `carcass_huge`); hit-zone data and the arrow projectile; bows with the strength cap; calls and blinds; butchering by `loot_table/butchering/` and hauling; thin-ice breaking and self-rescue; felling hazards; the distant-sound payload.
- **Tests:** all M6 tests as written; the senses tests run as GameTests in a flat test template with set wind; the population tests in the JUnit sim.
- **Human test:** unchanged.

### M7 — The year

- **Needs:** M6.
- **Deliver:** `story.Weather` with the full chain, forecast and storms, `Seasons`, `Alerts`, `Events`, `Ambitions`, `YearCard` (the clock, calendar and sun shipped in M2); the models `sim.env.SnowModel` and `IceModel` with the passes `world.pass.SnowPass` and `IcePass` on snow cover and ice blocks; hydrology ops in the op queue; season stages, colour handlers and the leaf model; weather drawing and fog; the aurora; ice holes and tip-ups; hides and winter gear; salmon run and caribou migration in populations; winter wolves; 17 more entity types on 2 new rigs (lemmings and the singing vole, the bat, the toad, the four salmon and their carcass, the lamprey, late fish and insects); the 27 fungi and aquatic plant blocks, lily pads among them.
- **Tests:** all M7 tests as written. The three-year and 100-year runs are JUnit sims; "an ice hole skins over within an hour below −20 °C" and "fur mittens safe at −25 °C, not −26 °C" are GameTests. Added: a snow pass after a 25 cm snowfall keeps MSPT under 50 ms.
- **Human test:** unchanged.

### M8 — Building and outposts

- **Needs:** M7, since rooms are tested in winter and roofs carry snow.
- **Deliver:** piece blocks and frames, ghosts, support check, refunds; rooms SavedData and the heat model; outpost repair; firewood items, woodpiles and the woodshed; stoves, chimneys, smoke and creosote; storage block entities and crafting from storage; smokehouse, cold smoke, root cellar and the permafrost cellar; dugouts; blueprint save and stamp; the job queue; floating logs and the raft entity; outposts, cut trails and bridges; cabin ageing with `data_maps/block/decay.json`; `data_maps/block/heat.json` and `piece.json`; `multiblock/` data; their sounds by the art budget.
- **Tests (GameTests):** all M8 tests as written, on the 5 × 6 block cabin template. "Stamping a saved cabin takes under 10% of the first build's player time" is timed by a bot.
- **Human test:** unchanged.

### M9 — Homestead and beyond

- **Needs:** M8.
- **Deliver:** all 16 regions with pre-warm (no streaming code beyond the layer task, since Minecraft loads chunks); world responses (regrowth by date, worn trails, beaver dams, wildfire, bears that learn); furnace, forge and the iron tier; the canoe entity; wolf taming and the companion; the sled team; the radio and supply drops; goats, reindeer and hens (3 entity types and the hen rig); the stovehouse; the fish wheel. The Interior and Beyond follow in the new M10 and M11 (World map tab); until then test worlds use a development border at the home valley's edge.
- **Tests:** all M9 tests as written, with "heights match within 0.1 m across region borders" checked on layers, and "a region builds in under 3 s with no tick over 50 ms" on the server. The five-year homestead run keeps mod save data under 5 MB and MSPT inside the budget.
- **Out of scope:** co-op until after 1.0 (RECONCILE 115); player-versus-player; any public release (RECONCILE 119).
- **Human test:** unchanged.

## What changes in the main tab

| Main-tab section | What is superseded | Replacement here |
| --- | --- | --- |
| Scope and pillars (rules every stage follows) | Browser streaming per 64 m chunk; mulberry32 "in every browser"; the five size words | Scale and world (size words, determinism) |
| Scope and pillars (three kinds of content) | Per-chunk edit lists; JSON under `data/` | Saves and data (Minecraft chunks store player-made changes) |
| What the experience needs beyond systems | Synthesized audio in `audio.js`; drawn hands | Look and sound |
| Pipeline at a glance | Web Worker stages, budgets in the browser, stage 9 edit lists | The nine stages in Minecraft |
| Terrain and hydrology | 2 m cells as the walked surface; "identical in every browser" | Terrain to blocks; Rivers, current and water |
| Biomes and resources | Placement into JS layers; "digging down finds nothing" as a convention | Explore, don't dig; Set pieces and landmarks |
| Start site | The outpost as code | The outpost template, 5 × 6 blocks |
| Calendar, daylight and weather | Runtime sun and weather in JS | Time and sky; Weather |
| Survival tuning, the body tick | `body/*.js`, `data/body.json` | The body attachment; `sim.body` |
| What the HUD shows | HTML HUD | HUD layers |
| Moment-to-moment mechanics (time rates) | Rates per real second in a browser loop | The clock (ticks per rate) |
| Controls and interaction | Keyboard and touch table | Controls (Minecraft defaults win; no touch) |
| The body on screen | Camera 1.70 m, walk 3.0 m/s by code | Movement numbers; eye at 1.62 m |
| Inventory and backpack | `S.ext.inventory`; HTML pack screen | Inventory and packs; Weight and carrying |
| Clothing | Ten slots on a custom body panel | Clothing slots (vanilla armor plus Curios) |
| Swimming and working in water | Depth and current read from JS layers | Rivers, current and water; Swimming and cold water |
| Fire, water and light | `fire/fire.js` state machine | Fire (block entity), light levels |
| Danger in detail (scope of threats) | Nothing stated about monsters | No monsters: real animals and the environment only |
| Shelter and building | 0.5 m grid, free placement, fine grid | The 1 m grid; Placing and ghosts |
| Camp, materials and builds; Building a cabin | Piece sizes in 0.5 m units; 3.6 × 4.2 m cabin | The 1 m grid; The first cabin in blocks |
| Data formats | One JSON file per kind, camelCase | Data kinds (one file per entry, snake\_case) |
| Engine rules | Three tiers by frame; IndexedDB; JS-only generation and GLSL | Animals (tiers mapped to simulation distance); What is saved where; Determinism |
| Build milestones and agent roles | `year-of-ice/` on the starter kit and Three.js; module lists; `smoke.mjs`, `lint-gen.mjs` | Platform and project; Milestones in Minecraft; SMOKE PASSED |
| Studio rules 4, 6, 7, 8, 9 | Browser wording | Studio rules that change |
| Code layout; File ownership | Folders of ES modules | Ownership by package |
| Sub-agents (testers) | Node simulator, Playwright bots | Four layers of tests |
| Open decisions: Look | Three render options | Resolved: Minecraft blocks, own resource pack, Iris optional |
| Open world tab: Data formats, Engine rules | JSON Schema, IndexedDB stores, browser budgets | Data kinds; What is saved where; Performance budget |
| Crafting tab: building pieces, warm rooms | 0.5 m sizes and grid flood fill | The 1 m grid; Warm rooms |

## Risks and limits

| Risk | Why it matters | Mitigation |
| --- | --- | --- |
| Voxel terrain against a smooth valley | 1 m steps make slopes jagged and slope rules coarse | Step height 1.0 on terrain blocks; slopes measured on the 2 m field, not blocks; optional 1/8 surface layers for gravel, sand, mud and snow, decided by the M2 frame test |
| A 1,968-block-tall world | More sections to light and track; some mods assume 384 | Solid and empty sections are single-valued; M2 measures it. Fallback: compress only elevation above 700 m by 0.5, keeping 1 block = 1 m horizontally and in the valley |
| Render distance of 512 m | Landmarks must read from 2 km | The horizon mesh with landmark silhouettes; Distant Horizons where installed |
| Block-scale animals in forests | A 1.4-wide moose cannot path through 1-block gaps | Tree spacing of 3 m or more keeps gaps; trail nodes guide large animals; a stuck animal teleports one trail node when unseen |
| Many entities | 40 Active and 200 Nearby animals plus fish could cost the tick | Nearby entities have no AI; fish only near spots; the caps and farthest-first demotion; spark in CI |
| Multiplayer time | One clock per world, rates per player | Rates requested per player, the slowest wins on a server; sleep only when all sleep; the SOS jump per player; co-op after 1.0 (RECONCILE 115) |
| Client-side player motion | Current, slopes and load must agree on both sides | The current is synced per chunk; modifiers are attributes, which Minecraft syncs |
| Vanilla water has no current, ice is per block | Physics of rivers is invented | The current attachment; ice state per water body, blocks only display it |
| Snow and ice passes rewrite many blocks | A snowfall touches every loaded column | Amortized passes of 4–6 chunks a tick; snow by class so no search; tracks as decals |
| Season colour changes need a re-mesh | A full re-mesh hitches | Gradual re-mesh over 30 s, nearest first, at most 8 times a year |
| Sodium and Iris | They replace chunk meshing and shaders | Colour handlers and the leaf model follow the public APIs; post effects have an overlay fallback; both run in the client smoke |
| Mod compatibility in general | Other mods add mobs, ores, recipes | The mod is a total conversion; the world preset, biome modifier and filters remove vanilla content; compatibility with other content mods is not a goal |
| Minecraft habits | Players expect to dig down, punch trees, craft anywhere, and fear the night | Deep ground is unbreakable and looks it; the first-night prompts teach stations and the hatchet in the first 10 minutes; nights are dangerous through cold, dark and wolves, not monsters |
| Save size | Region files grow with exploration | The 2 GB ceiling, LZ4 compression, the mod data budget kept at 5 MB |
| Gamma and brightness options | Players can brighten dark nights | The lightmap sets night levels; brightness stays for accessibility, by design |
| Version churn | Minecraft and NeoForge change APIs yearly | Pure `sim` core; thin adapters; the 1.21.1 pin, moved only in its own round after a milestone; the rename list in DEVGUIDE |
| GeckoLib on the server | Bone positions are client-only | Hit zones and bite reach from data boxes on the server |

## Open decisions after the switch

- [x] **Look:** Minecraft blocks with the mod's resource pack, GeckoLib animals, Iris optional. Decided by the engine choice.
- [x] **No monsters:** decided by the owner. Only real animals and the environment threaten the player.
- [ ] **Portals:** Nether and End off by default; the owner can reverse it with the `yearofice:other_dimensions` default.
- [x] **Game day length:** decided (RECONCILE 117). A world-creation option in steps from 24 to 72 real minutes, default 24; only the World rate scales (The clock, RECONCILE 145).
- [x] **Version pin:** decided (RECONCILE 118). NeoForge 1.21.1, Java 21, ModDevGradle 2.x (Version pin).
- [x] **Co-op:** decided (RECONCILE 115). Solo first, co-op after 1.0; the time rules are co-op-safe now, and the time-debt design waits in Minecraft development › Layer 1. **Release:** decided (RECONCILE 119). Personal use only; no public release work.
- [x] **Default mode, SOS stay-out and the wolf:** decided by Ruhan on 2026-10-19 (RECONCILE 148). Endless is the default and Hardcore is chosen at world creation, then locked for that world. The SOS screen offers Stay out for 14 days (default), 1, 2 or 5 years. A tamed wolf never leaves on Gentle, is warned and recoverable on Standard, and can leave for good on Brutal (Blueprint › Open decisions).
- [ ] **Surface layers:** 1/8 layers for gravel, sand, mud and snow on or off. Decided by the M2 frame test.

**Review audit (Minecraft interlace, player hands and HUD, milestones).** Reviewer G, 19 October. Status: Ready (already specified), Fixed in this round, or Open decision for Ruhan. Every row names its rule's home; the Minecraft development tab wins on build matters.

| System | Owner, layers, mechanisms | Saved and synced | Milestone and GameTest | Status |
| --- | --- | --- | --- | --- |
| Clock and day length | `story.clock`; writes Layer 1; `ServerTickEvent.Pre`, `setDayTime`; World rate 720 ÷ L ds a tick, other rates fixed | `yearofice_clock`, `yearofice_world.day_length_min`; `clock_sync` | M2; `clock/rates_exact`, `clock/multiplayer_slowest` | Fixed (RECONCILE 115, 117, 145) |
| Body tick | `body`, `sim.body`; reads Layers 1, 2, 5, 6, writes 7, 8, 3; damage cancelled into `DamageRouter`; attribute modifiers | `yearofice:body` (schema 2, parts), `copyOnDeath`; `body_sync`, `body_public` | M2.5 (kcal, warmth, water), M4; `body/closed_form_matches` | Fixed (`applyInjury` only, RULING 90) |
| Waking a sleeper | `ecosystem.encounter.SleeperWatch` calls `BodyService.wake` every 20 ticks | Nothing saved | M4 and M6; `ecosystem/sleeper_wakes_wolf_30m` | Fixed (request 85) |
| Collapse and SOS | `body.SosFlow`; Clock jump per player; World edits place `left_pack` | `left_pack` block, `placed_mask` | M4; SOS returns at BMI 19 in under 3 real s | Fixed (RECONCILE 83, 84) |
| Inventory and weight | `item.inventory`; Item model to Body load band; one `Inventory` mixin | 41 vanilla slots, Curios slots, `pack_rules`, `carry` | M2; load bands within 2% | Fixed (typed belt, neck 2, pocket 2, snowshoes) |
| Dropped items | `item`; `ItemEntity` lifespan, `ItemExpireEvent` | In the chunk | M2; `layers/item_expiry` | Fixed (7 game days, no `ground_pile`) |
| HUD and messenger | `hud`, `client.hud`, `hud.messenger`; Layer 8 | Journal only; `hud_line`, `map_tile` | M2 to M6; `hud/close_clock_filter`, `hud/battery_zero_to_journal` | Fixed (15 layers, Trail Messenger) |
| Decay | `decay`; Layer 5, lazy on read | `fresh`, `yearofice_decay`; `decay_heads` | M2.5 (meat), M5; `layers/live_equals_caught_up` | Fixed (RECONCILE 70) |
| World edits and catch-up | `world.change`, `world.catchup`, `LoadGate` | `yearofice_terrain`, `season_stamp`, `placed_mask` | M2 catch-up, M5 ops, M7 hydrology; `world/catchup_order` | Fixed (request 98) |
| Ecology records | `ecosystem`; tiered records, 8 ms split | Region records, slot records | M5 core, M6 senses; `layers/budget` | Fixed (RULING 91) |
| Weather and storms | `story.Weather`, `sim.weather`, `env.phenomena.Lightning`; `StormEvent` | `yearofice_weather`; `env_sync` 48 B, `strike` | M2 normals, M7 storms | Fixed (RECONCILE 138) |
| Debug and tuning | `core.debug.YoiCommands`; `yearofice-common.toml` presets | Config copied into `yearofice_world`; `debug_used` flag | Every milestone; one GameTest per command | Fixed (RECONCILE 118) |

**Vanilla gaps decided in this round:** buckets, flint and steel, torches, shields, bone meal, spyglass, maps and boats unobtainable; item despawn at 7 game days; `keepInventory` with `left_pack`; subtitles replaced by `captions`; F3 coordinates hidden; third person returned to first; Curios' own inventory button hidden; the 2 × 2 grid and recipe book replaced; no mob effects for body states; sleep only when all players sleep; vanilla lily pad and mud hidden; `/summon` refused in favour of `/yoi spawn`. None left open.

| Hands and HUD step | Keys and screen | Rule and numbers | Location | Status |
| --- | --- | --- | --- | --- |
| Look and pick up | RMB tap within 3 blocks | Prompt "RMB  Pick up flint (0.4 kg)"; no walk-over pickup except `#yearofice:auto_pickup` | Development › Inventory and slots | Fixed (main tab keys) |
| Hotbar and off-hand | 1–9, wheel, F | 9 slots are belt and pockets, 5 kg with no pack | Inventory and packs | Fixed (X → F) |
| Pack screen | E | `PackScreen` over `item.inventory.PackMenu`; rows from `pack.json`; world runs except on Gentle | Inventory and packs | Fixed |
| Weight | — | Bands 15, 30, 45 kg; meter amber 15, red 30; pickup past `max_kg` refused | Blueprint › Controls; Development › Inventory | Ready |
| Hand carry | Shift + RMB, Q | Carry slot; tools blocked; camera down 10 cm | Weight and carrying | Fixed |
| Hold-to-work | RMB hold from 0.25 s; Z | `work_progress` every 5 ticks; any danger stops it in the same tick | Work, previews and minigames | Fixed |
| Clothing | E, Body panel | 5 zones × inner and outer: vanilla armor outer, Curios the rest | Clothing slots | Fixed |
| HUD stack | F1 hides 4–15 | 15 layers under 0.3 ms a frame; Bleeding and Fever strip icons | HUD layers | Fixed |
| Trail Messenger | M; Q and E pages; hold M | Map 4% a game hour; idle 5.5% a game day | The messenger | Fixed (RECONCILE 83) |
| Voice and wolf | V tap and hold; G tap and hold | 12-segment voice wheel | Development › Other screens | Fixed (RECONCILE 124, 133) |
| Read, light, spray | X, L, B | Read moved off R | Controls | Fixed (RECONCILE 5) |
| Drop and loss | Q, Ctrl + Q | 7 game days; collapse leaves a `left_pack` | Blueprint › Inventory and backpack | Fixed |
| Mastery | Hold-to-toggle; practice dots | Each level −4% job time, to 40% | Blueprint › Controls | Ready |

| Milestone | Needs, and why first | Status |
| --- | --- | --- |
| M0, M1 | Nothing; layers, then water on them | Ready |
| M2 | M1 water; it carries the catch-up framework for M4's SOS jump, weather normals for the slice's body tick, and Curios `back` and `neck` for pack rows and the messenger | Fixed |
| M2.5 vertical slice | M2's clock, HUD, inventory and hold-to-work; first forms of fire, traps, fishing, decay, body and spawner | Fixed (new) |
| M3 | M2.5 fire; the outpost template from M1 | Fixed |
| M4 | M3 heat index and fire; the weather file's `ground` block | Fixed |
| M5 | M4 eating and stomach; Ecology core and the first World edits ops arrive here | Fixed |
| M6 | M5 Ecology core, scent points and small carcasses | Fixed |
| M7 | M6 populations for runs and migration; M2 catch-up for snow and ice passes; M5 op queue for hydrology | Fixed |
| M8 | M7 snow loads and winter room tests | Fixed |
| M9, M10, M11 | M8 pens and buildings, M6 wolves; then the Interior and Beyond (World map tab) | Ready |

**Forge Labs rows in this area.** Video rows 7, 147 and 241 (limited inventory, choices, off-hand overflow): Ready. Row 16 (the messenger hub): Fixed. Rows 20, 21 and 179 (status and vitals on the device): Ready. Row 71 ("You need more food"): Fixed, alert line in Blueprint › What the HUD shows. Row 130 (a rock in the off-hand): Fixed, F swaps. Row 240 (a heavy carcass slows walking): Ready. Row 311 (snowshoes): Fixed, Curios `snowshoes` slot.

**Open decisions for Ruhan from this audit:** none.
