# Water, calls and encounters

Water is the valley's slow builder and its busiest meeting place. This tab covers four things: water reshaping the land, water drawing animals, calling animals, and staged encounters. It extends Terrain and hydrology, Animals in depth and Setting up the hunt in the Blueprint tab, and Rare events in Open world in depth.

## Water reshapes the land

The river is never the same two years running. Outside bends eat their banks, inside bends grow gravel, floods lay down silt and driftwood, and ice scours what the water missed. Every change follows a fixed rate and shows a warning a season ahead, so a reader of the land can build where the water will not go.

### When the land changes

**Change ticks.** Land changes are applied on five calendar ticks, not every day. Floods and ice jams still play live when the player is there (Calendar, daylight and weather), and the ticks only settle their lasting marks.

| Tick | When | What it settles |
| --- | --- | --- |
| Breakup | The day high water falls after the river ice moves out | The breakup share of erosion, bar growth, ice shove, channel shifts, oxbow cutoffs, dam failures, falls retreat, flood deposits |
| Midsummer | July 15 | Thaw slumps, gully totals from the spring storms, pond-edge growth |
| Freeze-up | The day the main river freezes over | The open-season share of erosion and bars, beaver dam growth, thaw subsidence, pond silting |
| Winter steps | Every 10 days from December 1 to March 31 | Aufeis growth, spring and lead size |
| Storm day | Any day the weather is a storm, or heavy rain on bare ground | Gullies and mudflows on bare slopes |

**Two shares a year.** Half of a bank's yearly erosion lands at Breakup and half at Freeze-up. A flood year triples the Breakup share, so most big changes arrive in one week in May.

### Rivers move

**Cut banks retreat.** Each bank cell on an outside bend (the cut banks in Terrain and hydrology) carries an erosion counter. The counter gains R blocks a year, and one block column falls in each time it passes 1.0.

```latex
R = r_{bank} \cdot k_{bend} \cdot \left(0.5 \cdot f_{flood} + 0.5\right)
```

| Term | Values |
| --- | --- |
| r\_bank, by bank material | Silt or sand 1.0 block a year; gravel 0.6; clay (White Bluff) 0.3; frozen silt 0.5, plus slumps; rock 0 |
| k\_bend, bend radius in river widths | Under 2: ×1.5. 2 to 3.5: ×1.0. 3.5 to 5: ×0.5. Over 5: 0 |
| f\_flood | 1 in a normal breakup, 3 in a flood year |

A silt cut bank at a tight bend loses about 1.5 blocks in a normal year and 3 in a flood year. Over a 10-year run that is 15–20 m of bank: a cabin built 10 m from the edge in year 1 is on the edge by year 6.

**The bank warns first.** When a column's counter passes 0.6, the block at water level is removed, so the top overhangs: the undercut. Exposed roots show on the face, and a line of loose dirt (a tension crack) appears 1–2 blocks back from the edge. The column falls at the next tick that pushes the counter past 1.0, with a splash heard 200 m away.

**Trees ride the bank in.** A tree on a falling column drops into the river as a sweeper, crown downstream, rooted to the bank for 1–3 years. Sweepers are the strainers in Swimming and working in water, and they catch drift, so a new sweeper is also a new snag for salvage.

**Point bars grow.** Each bend's inside bar gains 0.8 blocks of gravel for every block the paired cut bank loses, so the channel keeps its width and walks sideways. New bar is bare gravel at normal high water minus 1 block. Willow seedlings take it after 2 summers and make a thicket by year 5, which is new moose browse and hare cover.

**Braids switch.** In the braided flats, each braid channel has a 20% chance at Breakup (40% in a flood year) to jump to a new path across the bar. The old channel becomes a string of pools that hold 1–4 stranded grayling or whitefish each for 10 days, then dry gravel. The new path is drawn toward the cells where aufeis formed that winter 60% of the time, so last winter's ice field marks next summer's channel.

**Oxbows cut off.** Each loop on the main river tracks its neck: the land width between its two arms. Erosion on both arms narrows it each year, and the river cuts through when the neck is thin enough.

| Neck width | Cutoff chance at Breakup |
| --- | --- |
| Over 4 blocks | 0 |
| 3–4 blocks | 30% in a normal year; 100% in a flood year |
| 2 blocks or less | 100% |

On a cutoff the river carves a chute one channel width across the neck. Both ends of the old loop silt shut within a year, and the loop becomes an oxbow lake. Pike move in during year 1 (fishing spot quality 0.3, rising 0.1 a year to 0.6), a weed shelf forms by year 2, and it becomes a weed pond in Water draws animals.

**One ripe loop.** The oxbow marsh archetype places one loop in the core with a neck of 8–14 blocks, so a cutoff lands in years 3–8 on most seeds. In a flood year before the cutoff, water runs across the neck for a day: the river trying the neck.

### Floods, jams and ice

**Spring floods leave marks.** Spring flood timing, height and rewards are in Rare events (Open world in depth). This tab adds what the water does to blocks and pieces as it falls.

- **Silt.** Every flooded cell where the flood current was under 0.5 m/s gets one silt layer (`yearofice:silt_layer`). Silt is the best garden ground (Crop farming) and counts as clay at half strength for chinking.
- **Driftwood.** A flood year leaves 4–12 driftwood piles per km of river at bar heads, snags and the flood-line edge; a normal breakup leaves 1–4. Each pile holds 2–6 dry logs that burn at once.
- **Pools.** Closed hollows keep their water when the river drops. These flood pools hold the stranded fish from Rare events and dry out in 10–20 days.

**What a flood does to pieces.** Pieces below the water line take these results; anything raised over the water line is untouched.

| Piece | Result |
| --- | --- |
| Fire ring | Doused; ash washed out, stones stay |
| Bough or debris bed | Washed away |
| Lean-to, drying rack, smoke rack | Standing in still water; in current over 1 m/s, knocked down 50% of the time and its poles drift. Meat on a submerged rack is ruined |
| Ground chest, barrel, ground cache | Light and wooden items float out and drift to snags; iron and stone stay put under the silt and are dug back out; food is soaked and spoils at 3× its rate |
| Woodpile | Logs float off to the nearest snag; the rest turns wet and needs 14 days in a woodshed |
| Cabin | Floor pieces −30 HP, stove out, chinking washed out, floor items drift unless shelved above the water |
| Canoe or raft on the bank | Floats away to a snag unless tied |
| Garden bed | Silted (+10% next yield); in June, crops under water for more than a day die |

**Jams pick the same bends.** Each seed fixes 1–3 jam sites: the tightest bends, island heads and the canyon mouth. The jams in Calendar, daylight and weather are drawn from these sites, the tightest most often, so the same bend jams most years.

**Ice shove.** A jam pushes ice onto the bank for 6–15 blocks at its upstream end, piling `yearofice:shove_ice` 1–3 blocks high. Shove ice snaps saplings and bushes and crushes placed pieces in its path (−60 HP; racks and lean-tos are destroyed). It melts by June 1–20 on open ground and lasts into July in shade, as free cold storage.

**Scars record the height.** Trunks the shove hits get bark scars 1–2.5 m above the bank, which stay for good. A bend with scarred spruce has jammed before, and the scar height shows how high the ice came.

**The release wave.** When a jam lets go, the reach below it rises 1 block for 2 game hours within the first 1 km. Anything on a low bar there is swept to the snags. The jam pool scours 1 block deeper, and its fishing spot quality rises 0.1, up to 1.0.

**Aufeis builds ice fields.** On creeks with springs or overflow patches (Calendar, daylight and weather), each winter step with a daily mean below −15 °C adds one aufeis layer (`yearofice:aufeis`, 1/8 block) per 2 cold days. The field spreads over creek-floor cells within 1 block of the creek's height. Fields reach 0.5–2 m thick and 50–300 m long by March.

| Season | What an aufeis field does |
| --- | --- |
| Winter | Flat, fast travel (×1.0, like river ice), but fresh overflow on top on 20% of days, with the wet-feet rules |
| Spring | Melts 1 layer every 4 days from May 1; fields over 1 m last into July |
| Summer | A bug-free cold spot (mosquito pressure ×0.2 on it) where moose stand on hot days. Meat laid on it keeps at the cool spoilage rate |

**Aufeis swallows low pieces.** Any piece on the field's cells gets its layers too. A door with over 0.5 m of aufeis against it is frozen shut until chopped free (30 game minutes). In summer, a scoured, bleached gravel flat with snapped willow stems marks where it forms every winter.

**Springs stay open.** The spring cells from Terrain and hydrology keep 2–10 blocks of open water all winter at 2–4 °C, ringed by 5 blocks of thin ice. Below −20 °C each spring steams, a column visible from 300 m, so a player can find winter water without melting snow.

- **Water.** Drinking spring water costs no firewood, and unboiled spring water rolls illness at half the river's chance.
- **Seeps.** Snow lies at half depth within 10 blocks of a spring, and horsetail stays green there: winter moose food.
- **Leads.** Below every falls and over springs on the main river, a lead of open water 2–20 blocks long stays open all winter. Leads are the winter water features in Water draws animals.

### Beavers, ponds and thaw

**Snags stand, then fall.** Trees drowned by a beaver pond (Open world in depth, world responses) stand as dead snags (`yearofice:snag_log`) for 5–15 years, then fall into the pond. Each 5 fallen snags add 0.05 to the pond's fishing spot quality, up to +0.2. Standing snags are dry firewood reached over the pond ice in winter, about 10 armloads per pond.

**Dams break.** At each Breakup, a dam of stage s fails with chance 2% × s, or 6% × s in a flood year, capped at 25%. A dam whose colony is gone loses one stage every 2 years and fails at stage 0.

**The failure pulse.** A failing dam raises the creek below it 1 block for 3–6 game hours over the next 1 km, sweeping anything on the creek floor to the snags. The pond's fish spill downstream: its spot quality drops to 0, and the creek spot below gains 0.2 for 30 days.

**Beaver meadows.** A drained pond floor follows a fixed succession. It is the clearest example of the pillar "The year changes the map" over a long run.

| Years after draining | Ground | What it gives |
| --- | --- | --- |
| 0–1 | Pond mud (`yearofice:pond_mud`), bog speed | Every print stays crisp for 3 days: the best tracking ground in the valley |
| 2–3 | Sedge and grass meadow | Hay for goats (Homestead livestock); voles, so foxes mouse here |
| 4–15 | Willow thicket | Peak moose browse and hare cover |
| 30 on | Spruce | Ordinary taiga |

From year 5, a colony in the region rebuilds a dam on the meadow 30% of the time each year, and the cycle starts again.

**The pond leveler.** The player can fix a beaver pond's height without trapping the colony. A hollow log laid through the dam crest (2 green logs, 2 game hours, Bushcraft) holds the pond one stage below its current stage. The beavers plug it 30% of the time each summer; the player clears it in 10 game minutes.

**Ponds silt in.** Ponds under 2,000 m² that beavers do not hold creep inward from the edge. At each Freeze-up, every edge water block turns to a sedge mat (`yearofice:sedge_mat`) with a 10% chance. A sedge mat walks at bog speed, and each step has a 5% chance to break through into knee-deep water.

**Permafrost thaws under people.** On permafrost cells (Biomes and resources), each summer adds to a thaw index T. At the Freeze-up tick, a cell with T of 2 or more sinks one block, and T resets to 0.

| Disturbance on the cell | T each summer |
| --- | --- |
| Moss and trees cleared | +1 |
| Burned | +1 |
| Under a heated room with no air gap | +2 |
| Worn path or trail | +0.5 |
| Moss left in place, a crib foundation with an air gap, or a 1 m gravel pad | Cancels all of the above |

**Thaw ponds.** A sunken cell inside a closed hollow fills with water by the next breakup and becomes a thaw pond. It adds still water (bugs, ducks), and over a few years it can drown the clearing it came from.

**Heave and sag.** A piece on a sinking cell drops out of level: its foundation loses 40 HP, doors take 2 s to open, the stove smokes on 10% of fires and the room loses 2 °C. Releveling takes 1 game hour per foundation corner with a lever pole.

**Thaw slumps.** A permafrost slope over 15° on a cut bank or lake shore can start to slump: 20% in a summer with warmth ×1.1 or more, or the summer after a fire. At most one slump starts per region a year.

- **Growth.** The headwall retreats 1–3 blocks at each Midsummer and Freeze-up tick for 3–8 years, then grows over.
- **Mud.** Slump mud (`yearofice:slump_mud`) flows to the water. The water below runs at +0.3 turbidity all summer, with the bite penalty in Fishing in depth.
- **Ground ice.** The headwall exposes clear ground ice 1–3 blocks tall. A block cut with the hatchet (10 game minutes) gives 8 ice chunks of 2.5 L each, and ground ice in an ice cellar holds it at 0 °C into August.
- **Bones.** Each slump year has a 10% chance to expose an Ice Age bone: a bison skull, horse jaw or mammoth tooth, a trophy worth +5 Spirits once.
- **Danger.** Each game minute on slump mud within 5 blocks of the headwall has a 10% chance to slide the player 3–6 blocks: soaked legs and −5 Condition.

### Slopes and falls

**Bare slopes gully.** A cell is bare when it is burned (first 2 summers), cleared with stumps pulled, a garden bed, or a worn path or trail. On a slope over 10°, a bare cell with 4 or more bare cells upslope of it on the flow line can gully on a storm day.

| Day | Gully chance per eligible cell |
| --- | --- |
| Heavy rain | 5% |
| Storm | 20% |
| Storm, burned slope over 15°, first summer after the fire | 30% |

A gully removes one soil block, down to rock or 2 blocks at most, which keeps the "soil 2 down" rule. The soil lands as a silt fan at the slope foot. A worn trail that gullies loses its speed bonus and walks at ×0.8 in mud.

**Mudflows dam creeks.** A burned reach above a creek sends a mudflow into it on 5% of storm days. The flow dams the creek, and a small pond fills behind it over 5 days, like a small earthquake lake from Rare events.

**Gullies find cabins.** A gully that reaches the uphill wall of a cabin floods its floor on every rain day after: items on the floor get wet. A ditch or water bar above the cabin stops it.

**Soft falls cut back.** A falls on rock with hardness under 0.6 loses its lip block at Breakup 5% of the time, or 15% in a flood year. The lip and its pool move one block upstream, and boulders land in the old pool, which becomes a riffle with grayling. Falls on hard rock never move in play.

### What is at risk and how the land warns

**Risk by place.**

| Place | Process | What is at risk | Lead time the land gives |
| --- | --- | --- | --- |
| Within 10 blocks of a cut-bank edge | Bank retreat | Anything on the edge, about one block of ground a year | One tick: the undercut and tension crack |
| Below the normal mud line | Every breakup's high water | Ground stores, beds, fire rings, boats | A year: the mud line on trunks |
| Below the flood-year line | Spring floods | Everything in the piece table above | A year, plus 48 h from the jam warning |
| A jam bend's banks, 15 blocks back | Ice shove | Racks, lean-tos, saplings | A year: bark scars on the trunks |
| Low bars in braided flats | Braid switching | Bar camps, set lines, caches | A winter: aufeis on the bar |
| Creek floors with springs | Aufeis | Doors, ground caches, sleds left out | A summer: scoured, bleached gravel |
| A creek below a dam at stage 3–4 | Dam failure | Creek-floor camps, weirs, nets | Water trickling over the crest and wet ground below the dam |
| Permafrost under a warm cabin | Thaw and sag | Foundations, doors, stove | One summer: the floor feels soft, and spruce nearby lean (drunken spruce) |
| Bare slopes | Gullies, mudflows | Trails, gardens, the cabin's uphill side | The first storm: rills one block wide |

**Two lines on the trees.** The normal mud line on trunks is in Terrain and hydrology. The flood-year line shows as a fainter silt band with driftwood lodged in alder forks about 1.5 m above it, so a careful player sees both.

**The messenger knows only what the player saw.** It compares the player's own pins and site checks, as the knowledge gate in The open world requires. Two prompts join the prompt data.

| Id | Fires when | Text | Priority |
| --- | --- | --- | --- |
| bank\_retreat | A pinned piece or store is within 4 blocks of a bank edge the player has seen | "The bank by the north cache lost 2 m since fall. 4 m left." | 2 |
| dam\_full | The player has seen water over a dam crest that sits above a pinned camp | "The beaver dam above camp is running over its crest." | 2 |

**The site check reads the bank.** A seventh result joins the six in Making camp: "Bank edge: 6 m from an undercut bank." It fails under 10 blocks from any cut-bank edge.

### What the player can do

| Work | Build | Time | Effect | Limits |
| --- | --- | --- | --- | --- |
| Choose the site | The site check | — | Above the flood-year line and 10+ blocks from a cut bank avoids every risk above | The only defense that costs nothing |
| Crib wall | Per 2 m of bank: 2 green logs and 30 stones | 2 game hours | Stops erosion on those bank cells; halves ice-shove reach behind it | Rots in 8 years, like corduroy. In a flood year a crib on a bend tighter than 2 widths fails 20% of the time |
| Riprap | 40 stones per 2 m of bank, from the bars | 1.5 game hours | Halves erosion on those cells | Never rots; a flood year moves 10% of it |
| Dike | Soil and sod, 1 block high on a 2-block base | 1 game hour per 2 m | Keeps water out up to its crest | Both ends must tie into higher ground. Overtopped, it fails 50% of the time each day. Not buildable from October to breakup (frozen ground) |
| Drainage ditch | One block deep and wide, falling 1 block per 20 m | 30 game minutes per 2 m | Drains a thaw pond or wet site; lowers the water table 1 block within 4 blocks over 14 days; diverts a gully | Thawed ground only |
| Water bar | One log set at 30° across a trail | 20 game minutes | Stops gullies on 10 m of sloped trail below it | One per 10 m of slope |
| Willow staking | 10 willow cuttings per 10 m² | 30 game minutes, May–June | Stops gullies there after one summer; grows browse | Only in thawed, moist soil |
| Air-gap foundation | A crib foundation with a gap, or a 1 m gravel pad | As in Shelter and building | Stops heated-room thaw on permafrost | Built before the room, or by lifting the cabin (4 game days) |
| Pond leveler | A hollow log through the dam | 2 game hours | Holds a beaver pond one stage lower | Plugged 30% of summers |
| Move camp | Dismantle and rebuild | Refunds as in Crafting and building in depth | The answer for tents, racks and caches | A cabin does not move, so siting comes first |

**Protection is a player edit.** Crib walls, riprap, dikes and ditches are saved with the player's edits, and the change planner reads them, so they change what the water does from then on.

### In Minecraft: the terrain-change queue

**One queue per level.** `world.change.TerrainChangeQueue` is a `SavedData` named `yearofice_terrain`. At each change tick, the pure planner `sim.terrain.HydroPlanner` walks the eligible cells of the hydrology layer (cut banks, bars, necks, dams, springs, permafrost and bare slopes), computes each cell's changes, and stores them as `ChangeOp` records keyed by `ChunkPos`.

1. **Plan.** At the tick, plan ops for every eligible cell in the world, loaded or not. Planning reads only the seed, the hydrology layer and saved player history, so it is cheap: a few thousand cells.
2. **Apply loaded chunks.** Ops in loaded chunks apply within the next game hour, under a server budget of 256 block sets per tick.
3. **Catch up on load.** `ChunkEvent.Load` applies every pending op for that chunk, oldest tick first, before the chunk is sent to a player.
4. **Prune.** Applied ops are deleted. A chunk keeps at most 3 years of unapplied ops; older ones are merged into their net result.

**Block-level edit rules.** Each op is one of nine kinds, and each kind may touch only certain blocks.

| Op | Does | Allowed on |
| --- | --- | --- |
| UNDERCUT | Removes the block at water level under a bank top | Blocks in `#yearofice:erodible_bank` |
| COLLAPSE | Removes a bank column; the tree on it becomes a sweeper | Same, plus logs and leaves of that tree |
| DEPOSIT | Adds gravel, sand, silt layer or driftwood pile | Air or water over natural ground |
| SUBSIDE | Lowers a column by one block; fills with water if inside a closed hollow | Natural soil on permafrost cells |
| GULLY | Removes one soil block, at most 2 deep | Natural soil |
| SHOVE | Places shove ice | Air, plants, snow |
| AUFEIS | Adds or removes one aufeis layer | Air, snow, existing aufeis |
| SNAG | Turns a drowned tree into snag logs, or fells a snag into the pond | Logs and leaves of a tree in the pond |
| VEGETATE | Places willow, sedge or grass for succession | Natural ground with air above |

**Never the player's blocks.** A chunk attachment `yearofice:placed_mask` records every block a player or a building piece placed. No op edits a masked block. Persistent ground blocks such as `left_pack` and `lost_gear` set the mask too (RECONCILE 97), and animal works (dams, lodges, push-ups, redds, dens) reach blocks only through `WorldEditService.submit` (RECONCILE 99). Damage to pieces goes through the piece HP rules, and items move through the drift system.

**Caps per chunk.** At most 64 permanent block edits per chunk per tick, and 8 per storm day for gullies. Ops over the cap carry to the next tick, so a world never reshapes faster than it can save.

**Floods use real water.** `FloodController` raises the river 1 block per 3 game hours while a flood or jam runs. It places water sources only on the flood mask: cells below the current level and connected to the river by a flood fill on 2 m cells, so placed water never spills. Placed sources carry a flood flag in a chunk attachment and are removed top-down as the water falls; closed hollows keep theirs as flood pools. Flood current for the silt and knock-down rules is read from the `yearofice:current` attachment.

**No neighbor storms.** Edits use the client-update and known-shape flags (`Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE`), so a deposit does not cascade physics. Lighting is relit once per chunk after its batch.

**Seen or unseen.** Collapses, shove and dam failures may happen in view, with their sound and a dirt or ice particle burst. Deposits, succession and subsidence wait until no player within 64 blocks faces the cell, for up to one game hour.

**Determinism.** Each op's roll comes from its own generator seeded by `hash(seed, "terrain", year, tick, cellX, cellZ, process)`. No roll depends on load order or tick timing. The same seed and generator version plus the same saved player history give the same land, block for block.

## Water draws animals

Every animal needs water, and many need what grows in it or lives near it. Each water feature sends out a draw that rises and falls by season and hour, and animals fold it into their drives. A player who learns which water pulls what, and when, finds game without luck.

### Water features

Each feature is a point of interest with a center, 1–4 approach points where trails meet the water, and a draw radius. Approach points are where tracks pile up and where blinds go.

| Feature | Where it forms | Draw radius | Changes over time |
| --- | --- | --- | --- |
| Weed pond | Pond or lake shelf 0.5–1.5 m deep with pond weeds, lilies and horsetail | 1,500 m for moose | Oxbows and beaver ponds become weed ponds by year 2 |
| Riffle | Shallow gravel runs on the main river and creeks; salmon spawn on them | 3,000 m for bears in a run | Jam scour and falls retreat make new ones |
| Ford | Wadeable water on a game trail (under 0.8 m, under 1.2 m/s) | 1,000 m; 5,000 m for caribou on the route | Braid switching moves fords each spring |
| Falls pool | The pool at the foot of each falls | 2,000 m | Moves upstream as soft falls cut back |
| Creek mouth | Where a creek meets the river or a lake | 800 m | Bars grow across it |
| Beaver pond | Behind each dam, stage 1 or more | 800 m | Grows and fails with the dam |
| Molting lake | The big lake, or the largest oxbow on other archetypes; one per world | 4,000 m for waterfowl | Fixed |
| Winter open water | Springs, leads below falls, aufeis edges | 1,500 m | Size set at each winter step |
| Mineral lick | The lick set piece | 2,000 m | Fixed |
| Wallow | Rut pits a bull digs within 300 m of water, September 10 – October 10 | 1,000 m in the rut | Made new each rut; old ones stay as mud holes |
| Bug refuge | Wind-swept gravel bars, summer aufeis, lake shores with an onshore wind | 600 m | Moves with wind each hour |
| Still surface | Any pond, slough or lake | 200 m | — |

### Who comes to which water

Weights run from 0 to 1. Windows are the dates the weight applies; outside them it is 0 unless the row says so.

| Feature | Species and weight | Window | Hours | Why |
| --- | --- | --- | --- | --- |
| Weed pond | Moose 1.0; muskrat 0.6; ducks 0.5; beaver 0.4 | Moose June 10 – August 15 | Dawn, dusk, and hot middays | Pond weeds hold far more sodium than browse, and moose crave it after winter |
| Riffle | Grizzly 1.0; bald eagle 0.8; black bear 0.5; raven 0.6; otter 0.5; mink 0.4 | Salmon run windows; eagles and ravens stay 3 weeks after for spawned-out fish | Bears dawn and dusk; eagles midday | Salmon on the gravel |
| Ford | Caribou 1.0 on the route; moose 0.4; wolves 0.5; grizzly 0.3 | Caribou in September; others all year | Any | Crossing, and for wolves, ambush |
| Falls pool | Grizzly 0.8 in August; otter 0.6; dipper 1.0 in winter; eagle 0.4 | All year | Any | Fish stacked below the drop |
| Creek mouth | Otter 0.5; mink 0.6; grizzly 0.5 in the coho run | All year | Dusk | Grayling and coho |
| Beaver pond | Beaver 1.0; muskrat 0.8; mink 0.6; moose 0.6; nesting ducks 0.6; otter 0.4; osprey 0.3 | Open water | Dusk and night for the rodents | Food, cover and perches on snags |
| Molting lake | Molting geese and ducks 1.0; red fox 0.5; bald eagle 0.5; grizzly 0.2 | July 1 – August 10 | Day | Flightless birds |
| Winter open water | Dipper 1.0; otter 1.0; mink 0.8; raven 0.5; wolves 0.4; red fox 0.4; moose 0.3 at seeps | November – March | Any | The only liquid water and the only fishing holes |
| Mineral lick | Moose 1.0 (May 20 – July 15), 0.5 (July 16 – August 31); Dall sheep at the cliff lick | As set | Dawn and dusk | Salt |
| Wallow | Cow moose 0.8; rival bulls 0.6 | September 10 – October 10 | Dawn, dusk, night | The rut |
| Bug refuge | Moose 0.5; black bear 0.2 | June 10 – July 31 | 11:00–17:00 | Escape from mosquitoes |
| Still surface | Tree swallows, dragonflies, ospreys over lakes with fish | Swallows May 15 – August 10; dragonflies June 15 – August 15; ospreys May 1 – September 15 | Midday | Insects and fish |

**Mink, ospreys, eagles and dippers.** This tab gives these four only their water role. Mink (`yearofice:mink`), bald eagle (`yearofice:bald_eagle`) and dipper (`yearofice:american_dipper`) are defined in the Waters and fish tab; the osprey (`yearofice:osprey`) is new here. Each one tells the player something about the water.

- **Osprey.** A dive marks a fish: the dive point is within 20 m of a fishing spot of quality 0.5 or more. When a bald eagle is near, it robs the osprey 10% of the time, and the dropped fish lands on the shore: a whole whitefish from the sky.
- **Bald eagle.** It perches on snags over riffles and gut-pile drift (Handling the catch). Two or more eagles sitting together mean spawned-out salmon below.
- **Dipper.** A small grey bird that walks under winter creeks and sings in January. Its song means open water within 50 m, which is drinkable winter water.
- **Mink.** It runs the bank by day as often as by night. It visits a minnow trap, net, live crib or stringer in its range on 10% of nights and takes 1 fish under 1 kg, emptying a minnow trap on 25% of those visits (RECONCILE 50; the Bestiary owns the rule). It is taken in the drowning slide from Traps.

### The draw model

Each feature f sends a draw G to each animal of species s at distance d, by date, hour and weather. Disturbance X is the player's mark on the place, below.

```latex
G_{f,s} = b_{f,s} \cdot m_{f,s}(\text{date}) \cdot h_{f,s}(\text{hour}) \cdot \omega(\text{weather}) \cdot (1 - X_f) \cdot \max\left(0,\ 1 - \frac{d}{R_{f,s}}\right)
```

b is the weight in the table, m and h are 0–1 window shapes with a 5-day ramp at each end, and R is the draw radius. Weather ω is 1, except rain ×0.7 for birds and bugs, and heat over 20 °C ×1.5 for weed ponds and bug refuges.

**The draw feeds the drives.** Each feature serves one drive from Drives and daily schedules: thirst, hunger, safety (bug refuges) or the seasonal drive (wallows, the molt, salmon). The strongest feature's draw adds to that drive's utility.

```latex
U_d' = U_d + 0.5 \cdot \max_f G_{f,s}
```

The animal then goes to that feature's nearest approach point instead of its default point for the drive, by game trails. A draw of 0.6 or more can pull an animal up to 20% past its home range edge, which is how salmon bring bears from the next valley.

**A sodium drive for moose.** From May 20 to August 31, moose carry one extra drive, sodium, rising 0.03 a game hour. Feeding at a weed pond lowers it 0.2 a game hour and a lick 0.5. This is why the existing 60% lick visit rate (Setting up the hunt) splits into 80% of days from May 20 to July 15 and 40% after, a season mean of 62%, the Blueprint's 60% (Minecraft development › Entities and AI, S18).

**Off-screen.** In the Nearby tier, individuals are placed "where their drives would have it" (Populations). The draw sets that place: an animal is at a feature in the hour with probability in proportion to its G.

### Moments the draw makes

**Stalk on the dive.** A moose feeding on pond weeds puts its head under for 15–40 s, then lifts it for 10–20 s to chew and look. While its head is under, it cannot see and hears at ×0.3. A water-streaming sound comes 1 s before the head rises, so a player can walk, or wade, between dives and freeze on the cue.

**The edge pause.** Moose, caribou, wolves and bears stop at the cover edge for 20–90 s before stepping out to water, looking at ×1.2 sight range. Drinking then puts the head down for 8–15 s at a time, with no sight forward. The pause is when the player must not move; the drink is the shot.

**Bugs drive moose into water.** The existing hot-day rule in Drives and daily schedules sends a moose into water 0.5–1.5 m deep. It now picks the nearest weed pond in range, so the dive stalk applies. In wind over 4 m/s it picks a bug refuge instead, standing on a bar or summer aufeis.

**Winter's only water.** From November to March, open water concentrates life. Otters fish from leads and travel between them on slides; mink hunt the edges; wolves and foxes patrol the ice for both. Moose dig horsetail at spring seeps, so a steaming spring in January often has fresh moose tracks.

**Swallows read the weather.** Swallows hunting within 1 m of still water mean a rain day within 12 game hours, right 70% of the time. Dragonflies over still water at sunny, calm middays cut mosquito pressure B ×0.8 within 20 m, so a sunny pond shore at noon is better than the forest edge at dusk.

**The molting lake.** From July 1 to August 10, 10–30 non-breeding geese and ducks lose their flight feathers on the molting lake. They keep 50 m or more off shore and crowd to the far side when the player walks the beach.

- **Taking them.** Herding a flock into a cove with the canoe and taking 1–3 birds by hand or net is one drive. Each drive empties the lake for 5 days, and each bird taken lowers next year's molters by 1.
- **Feathers.** Molted flight feathers wash to the downwind shore: 20–60 a week from July 5 to August 10, ready for fletching.
- **Predators.** Foxes and eagles hunt the molters, so the lake is a good place for staged hunts.

### The player at the water

**Disturbance.** Each feature keeps a disturbance X from 0 to 1, which scales its draw by (1 − X). The player's presence raises it, and it halves every 8 game hours at the water's edge.

| Player at the feature | X added |
| --- | --- |
| Standing or walking within 30 m, per game hour | 0.2 |
| Still in a blind or on a stand within 30 m, per game hour | 0.05 |
| A fire within 60 m, per game hour | 0.5 |
| A night slept within 100 m | 0.4 |
| Guts, blood or a kill within 30 m | 0.5, plus the scent point it makes |

The half-life is 4 game hours after rain and 12 in calm, humid weather. Ground scent in mud within 3 blocks of water lasts 6 game hours, not the 2 in Senses by species.

**Camping spooks the draw.** A bed, outpost or fire ring within 100 m holds X at 0.7 or more for moose, bears, wolves and caribou while it is in use and for 3 days after the last night. For beaver, muskrat and mink the floor is 0.3, because they habituate. This is the water reason behind "water close but not too close" in Making camp.

**Reading a feature.** Sitting still with a feature in view for 30 game minutes logs a water note in the notebook: species seen and hour. After 3 notes at one feature, the messenger shows its busy hours for those species, built only from what the player saw.

**Sign at the water.** Approach points carry the most sign in the valley: prints in mud, beds in nearby willow, and cropped lily pads floating at weed ponds. Lily pads are a new sign type: green and floating under a day old, browning at 1–3 days.

**Blinds at approach points.** A ground blind at an approach point gets the edge pause in its favor. An animal pausing at the edge looks out over the open water 70% of the time, not back along the trail.

### In Minecraft: water features

**Marker blocks make the POIs.** Each feature gets an invisible marker block, `yearofice:water_marker`, with a `feature` block-state property, set 3 blocks under its center so it is never dug. Each property value is one `PoiType` (`yearofice:poi_weed_pond` and so on), so Brain behaviors find features through the `PoiManager`, like vanilla villagers find beds.

**A registry holds the rest.** `ecosystem.water.WaterFeatureRegistry` (`SavedData`, `yearofice_water_features`; `yearofice_water` is the world developer's water-body list, RECONCILE 96) stores each feature's id, approach points, radius, disturbance X and its dates. Worldgen places markers from the hydrology layer. The terrain-change queue adds and removes dynamic ones: new oxbows, beaver ponds by stage, thaw ponds, flood pools, wallows and winter leads.

**Brain parts.**

- **Sensor.** `WaterDrawSensor` runs every 200 ticks (10 game minutes), reads features within 128 blocks from the `PoiManager` and the rest from the registry, and writes the best target to memory `yearofice:water_target`.
- **Memories.** `yearofice:water_target` (GlobalPos and feature id), `yearofice:sodium` (float), `yearofice:last_drank` (game time), `yearofice:head_under` (boolean).
- **Behaviors.** `WaterVisitBehavior` walks to the approach point; `EdgePauseBehavior` holds and scans; `HeadUnderFeedBehavior` runs the dive cycle; `StandInWaterBehavior` holds a moose in a pond.
- **Animation.** `head_under` is synced by an entity data accessor, so the GeckoLib controller plays `feed_submerged` and the streaming-water sound 1 s before `feed_surface`.

**Swallows and dragonflies are cheap.** Dragonflies are client particles spawned near still-surface features the server lists to the client. Swallows are a light flying ambient entity with flock steering and no other AI, capped at 12 per feature.

## Calling animals

A call is a lie told in an animal's language. Each one is a craftable item or a voice action with its own rhythm, the animals it draws, a season it works in, odds and a risk. Bad calls and overcalling teach animals, so the best caller calls least.

### How every call works

**One formula for all calls.** The moose formula in Setting up the hunt becomes one case of the general rule.

```latex
P_{respond} = p_s \cdot S_s(\text{date}) \cdot H_s(\text{hour}) \cdot q \cdot c_s \cdot E_s \cdot F\left(\frac{d}{R_{eff}}\right)
```

| Term | Meaning |
| --- | --- |
| p\_s | The call's base odds for species s, from the call table |
| S\_s, H\_s | Season and hour factors; for the cow moan, S is the existing rut factor and H is 1 |
| q | Call quality from the mini-interaction, 0.3–1.2 |
| c\_s | Call-shyness: 1 normally, 0.3 when shy |
| E\_s | The encounter director's multiplier (Populations) |
| F | Falloff: linear (1 − d/R\_eff) for moose, ducks and geese; flat (1 inside R\_eff) for the squeaks, as written in Setting up the hunt |

At q = 1, no shyness and calm air, the cow moan is exactly the existing formula, so its calling test stands.

**Calls carry by wind, ground and weather.** R\_eff is the call's range times three factors, capped at 1.5 times the range. The hearing modifiers in Senses by species still apply: wind over 4 m/s halves, rain ×0.6, a river within 30 m halves.

| Factor | Values |
| --- | --- |
| Wind direction, listener from the caller | Downwind ×1.3; crosswind ×1.0; upwind ×0.6 |
| Ground between them | Open water, bog or lake ice ×1.4; tundra or gravel bar ×1.2; birch–aspen ×0.9; spruce taiga ×0.7; a ridge crest between ×0.5 |
| Air | Calm dawn or dusk on the valley floor (an inversion) ×1.2 |

A moan across a pond on a calm dusk would reach 800 × 1.4 × 1.2 = 1,344 m, so it hits the 1,200 m cap. The same moan in spruce with a 3 m/s crosswind reaches 560 m.

**Quality comes from the hands.** Each call has a pattern of holds and taps shown as a thin bar at the crosshair. q is 1 minus half the mean timing error over the tolerance, clamped to 0.3–1.0, then scaled by a practice factor.

- **Practice.** The factor is 0.8 for a call's first 10 uses, 1.0 from 10 to 29, and 1.2 from 30. A seasoned caller gets more than the book's odds.
- **Interruptions.** A call stopped halfway counts at q 0.3, which is a bad call (call-shy below).
- **Animals answer by sound.** A responding animal's answer plays from its real position, and the messenger's wind view marks it, as for the bull grunt in Setting up the hunt.

### The calls

| Call | Made from | Input | Draws | Window | Odds and range at q 1 | Risk |
| --- | --- | --- | --- | --- | --- | --- |
| Cow moan | Moose call (recipe in Crafting and building in depth) | Hold 2–4 s; look down in the last 0.5 s for the falling end (+0.1 q) | Bulls; cows within 400 m 15% of the time, and a bull follows a cow in 30% | Rut factor, September 1 – October 20 | 0.4 × rut, linear to 800 m | Charge inside 25 m (How animals act) |
| Bull grunt | Same call | 2–3 taps 0.5 s apart | A bull already coming walks 1.5× faster; an adult bull (age 5+) within 500 m comes to fight at 0.25 × rut | Rut | 500 m | Young bulls (2–4) hang up at 100 m 60% of the time; charge chance inside 25 m rises from 20% to 30% |
| Raking | Moose shoulder blade or antler; a stick works at q 0.6 | Hold use (right mouse) with it on willow or alder 3–6 s | Brings a hung-up bull on 60% (existing); alone, 0.15 × rut within 300 m | Rut | 300 m | Game within 100 m gains +30 awareness |
| Water pour | A pot or bark bucket with 2 L or more | Hold 4–8 s from standing height | A hung-up bull steps in 50% of the time; once per approach, stacking with raking | Rut | 400 m | Uses the water |
| Wading splash | Feet | 3 slow steps in knee water over 5 s | In the rut, a bull within 300 m at 0.2 × rut; in July at a weed pond, a curious cow 10% | Rut; July | 300 m | Wet boots |
| Distress squeak | Squeak call (recipe in Crafting and building in depth) | 3 wails of 2–3 s, then 30–60 s silent; up to 5 sets | Fox 50%, lynx 25%, wolves 10%, bear 10% April–October (existing); coyote 15% where present; wolverine 5% in winter to 600 m; great horned owl 10% at night; ravens 20% by day | All year; best at dusk | Flat to 300 m | Bears and winter wolves come too; squealing over 20 s without a break is q 0.5 |
| Mouse squeak | Lips | 4–8 short kissing squeaks while crouched | Marten 40% within 60 m; ermine 50% within 30 m; fox 20% within 100 m, 30% in winter | All year; dusk and night | Flat | None; overuse makes the locals shy for 3 days |
| Hen cluck | Voice | Soft clucks, 1 a second for 10 s | May 1 – June 15: a male spruce grouse within 80 m flies down and displays 40% of the time, fanned, for 30 s at 10 m. September 1 – October 15: a flushed brood regroups to it 30% | As listed | 80 m | Grouse shy for a day after a bad call |
| Wing flap | A dried grouse wing, from cleaning a grouse | 3 quick slaps on the thigh | Spring males 50% within 100 m | May 1 – June 15 | 100 m | — |
| Ptarmigan "go-back" | Voice | 4 rising croaks | Territorial cock ptarmigan 30% within 150 m | April – May | 150 m | The flock flushes if it misses |
| Duck call | Bark reed in a carved spruce barrel; workbench, 1 game hour | Hail: 5 falling quacks at a far flock. Chuckle: rapid taps while it circles | +15 points on the decoy landing odds when timed right | Migration windows; local ducks all summer at ×0.3 | Linear to 400 m | Calling while birds set their wings: −20 points |
| Goose flute | A hollow bird bone or spruce tube with a reed; workbench, 1.5 game hours | Honk-cluck pairs, slowing as the flock turns | +15 points with goose decoys | Migration windows | Linear to 600 m | Same as ducks |
| Wolf howl | Voice: Howl on the voice wheel (V hold) | One 3–6 s rising and falling howl | Packs within 5 km answer: 70% April 20 – July 15 at dusk; 50% July 16 – October; 30% November – April. Night ×1.2, day ×0.5 | All year | 5 km | Below |
| Owl hoot | Voice: cupped hands | 5 hoots, the third longest | A locator: roosting grouse within 60 m cluck 60%; squirrels scold 40%; hares within 40 m freeze 10 s; a great horned owl answers 50% January – March; wolves howl back 10% at night | Dusk and night | 600 m | Grouse shy for a day |
| Tail slap | A paddle or flat split board on the water | One hard slap | The colony's adult within 150 m swims to 10–20 m and circles, slapping back, 50% of the time | May – October, dusk | 150 m | Everything else within 150 m gains +40 awareness; moose leave the pond (X +0.5). The bite rule near a lodge in May–June still applies |
| Raven quork | Voice: a throaty knock | 2–3 knocks, a pause, 2 more | Winter only: see raven leads | November – March | 500 m | None |
| Wolf whistle | The G whistle, or the willow whistle from Spirits | Two notes | The tamed wolf; see the whistle | All year | 300 m, or 600 m with the willow whistle | Wild game within 100 m gains +30 awareness |

### Moose calling, extended

**The rut sequence.** The rhythm in Setting up the hunt still holds: one moan, a 15–30 minute wait, then soft grunts once he answers. The new pieces let the player finish a hung-up bull without calling again.

1. Moan from a site with water or open ground on its downwind side.
2. On an answer, wait. Grunt only if he goes quiet for 10 game minutes.
3. If he hangs up at 60–120 m, rake (60%), then pour water (50%), not a second moan.
4. If he circles downwind, a pond on that side makes him show himself, or wade in, at 30–40 m.

**A bull in the water.** A moose shot standing in water over 0.8 m deep dies in the water. Hauling 450 kg to shore takes 30 m of cordage to a tree and 2 game hours in the water, under the cold-water rules; the canoe tows it at ×0.3 speed. Field care applies once it is ashore, and cold water keeps it fresh for 2 game days.

**Cows are part of the call.** In the rut, a cow that comes to a moan brings any bull tending her 30% of the time, trailing her by 50–150 m. Shooting the cow costs the future moose in Populations; letting her pass keeps the bull coming.

**Wrong season.** A cow moan outside September 1 – October 20 brings no bull, as the calling test requires. It makes every moose within 400 m call-shy for 7 days. In calving season (May 20 – June 15), a cow with a calf within 200 m comes in to check 20% of the time, in her defensive state.

### Predator and small-game calls

**Predators come their own way.** Each species answers a squeak in its own style, so the player learns who is coming from how it comes.

- **Fox.** Straight in at a trot within 2–6 game minutes, often to 10 m.
- **Lynx.** Sneaks to 40–80 m and sits watching for up to 10 game minutes. It is seen at ×0.3 sight range while it sits, so most callers never know it came.
- **Wolves.** Come together at a trot, fanning out at 100 m. In winter, a squeak counts as a "test" trigger when the test rules hold.
- **Bear.** Comes slowly and does not stop at the edge. If the squeak stops and the bear does not wind the player, it loses interest in 2 game minutes 60% of the time.
- **Owl.** At night a great horned owl swoops at the sound 10% of the time: wings past the head at 1 m, no harm.

**Set up to see.** A squeak site needs 50 m of sight line on the downwind side, because 60% of predators circle to that side first. Sitting with the back to a large trunk halves the chance of being seen from behind.

**The mouse squeak.** A marten that answers climbs down to 10 m and stands up on a branch for 5 s: the shot window. In winter, foxes mousing in snow (Who hunts whom) turn to it from 100 m.

### Bird calls and decoys

**Decoys set the odds; calls tune them.** The decoy rule in Setting up the hunt stands: 6 or more decoys within 20 m of a blind land a passing flock 40% of the time, or 10% with none. A well-timed call adds 15 points and a badly timed one takes 20.

**Birds land into the wind.** They come in flying upwind, so the open landing hole sits on the downwind edge of the spread, in front of the blind. A spread with the wind at the birds' tails gets ×0.5.

| Decoy | Build | Weight | Rules |
| --- | --- | --- | --- |
| Carved wood | As in Setting up the hunt, 1 game hour | 1 kg | Lasts for years |
| Birch bark | Bark and two sticks, 20 game minutes | 0.2 kg | Blows over in wind above 6 m/s (counts as half) |
| Mound | Mud and grass on a bar or shore, 10 game minutes | — | Geese only; washes out in rain |
| Taken bird | A bird propped on a stick | — | Counts as 2 decoys; spoils in a day |

### Voice calls: howl, hoot, raven and slap

**The howl reveals a pack.** An answering pack shows on the wind view as a bearing (±15°) and a band: near (under 1 km), middle or far. It is the cheapest way to learn where the pack is this week.

- **Winter risk.** From November to March, a pack within 1 km comes to check 20% of the time. It circles at 100–200 m, the Shadowing step in Danger in detail, and goes further only if the test conditions hold.
- **The den.** Howling within 1 km of the den in denning season makes the pack move the pups that night 50% of the time. The den is then empty for the year, and the taming window moves to a new den 1–3 km away, found by sign.
- **Too much.** After 3 howls in 7 days, packs stop answering for 5 days.
- **The tamed wolf** howls along, as with the willow whistle in Spirits.

**Raven leads.** From November to March, when a carcass the player has not found lies within 3 km, a raven that answers the quork leads 40% of the time. It flies 150–250 m ahead, lands on snags and calls, and waits up to 2 game minutes. It gives up if the player falls 400 m behind.

**The raven share.** Leaving 1 kg of offal on a stump at a kill adds 10 points to future leads, up to 30. It also gives +2 Spirits once per kill. With no carcass in range, answering ravens circle once and leave.

**The owl hoot is a locator.** It costs nothing and makes hidden animals speak. The best use is at dusk in fall: clucking grouse show where they roost, so the player returns at dawn.

### The wolf whistle

**The whistle extends.** Tap G whistles Come and holding G opens the command wheel (Taming a wolf, with the whistle on G as the Minecraft engine tab sets). The willow whistle from Spirits doubles the range to 600 m and takes the wind factors like any call.

**Whistle before food.** A Tolerant wolf that hears the whistle before 10 feedings comes to it 50% of the time, before it is Bonded. Bonded wolves come at the obey chance in Taming a wolf.

**One pattern per wolf.** With a team, each wolf learns one of 6 two-note patterns in 5 sessions. A named pattern before Come calls only that wolf, so a sled team can be sorted at the yard.

### Call-shy animals

**Animals learn from bad calls.** A call that fails one of these rules makes the animals that heard it call-shy: c\_s drops to 0.3 for N days, then climbs back to 1 over N more days.

- A bad call: q under 0.5, or a call cut short.
- The wrong season for that call.
- Overcalling: more than one moan per 10 game minutes, more than 5 squeak sets in 30 minutes, or calling at birds setting their wings.
- Coming to a call and then winding or seeing the player.
- Being shot at within 100 m of a call.

| Species | N days | Shy applies to |
| --- | --- | --- |
| Moose | 7; 14 if shot at | The individual (persistent id) |
| Wolves | 10 | The pack |
| Bears | 20 | The individual |
| Lynx | 10 | The individual |
| Red fox | 5 | The individual |
| Marten, ermine | 3 | The home-range cell |
| Beaver | 3 | The colony |
| Grouse, ptarmigan | 1 | The home-range cell |
| Ducks and geese | 3 | The flock |
| Ravens | 2 | The local ravens |

**The educated responder.** A shy animal that hears a call ignores it 70% of the time. The other 30% it comes in silently, downwind, and never answers: the bull that appears behind the blind, the bear that comes to a squeak without a sound. The senses memory in Senses by species stacks with this.

### In Minecraft: calls

**Call items.** Every made call works on the item's use action (right mouse), with no new key. Each is an item with `UseAnim.TOOT_HORN`, a use duration of 72,000 ticks and `releaseUsing` reading the hold length. Voice calls are entries on the voice key (V) wheel, sent as the same payload with no item. The wheel's segments are in Minecraft development › Player systems and client (Shout, Howl, Owl hoot, Quork, Pish, Clap); this tab adds Mouse squeak, Hen cluck and Go-back, so the wheel holds nine. V tap stays Talk (RECONCILE 31, 60). The grouse wing, tail slap and wading splash are item or movement actions, not voice.

**The rhythm bar is client-side.** `CallRhythmOverlay` is a renderer inside the `yearofice:minigame` GUI layer, which `client.hud.MinigameSlot` grants to one owner at a time, so it never stacks with a fishing fight or a bow drill (RECONCILE 8). It times the player's inputs against the call's pattern and sends `CallPerformedPayload(callId, q, pos)` to the server.

**The server checks the call.** The server rebuilds the hold time from its own use ticks and clamps q to the practice cap stored in the item's data component `yearofice:call_practice`. A payload that disagrees by more than 0.2 is cut to q 0.3.

**The broadcaster reaches past loaded chunks.** `CallBroadcaster` computes R\_eff from the weather state and the ground under 4 points on the line, then rolls every listener.

1. Loaded animals within R\_eff: rolled at once; responders get memory `yearofice:heard_call`.
2. Nearby-tier individuals beyond loaded chunks: rolled from the population model. A responder walks its virtual path at its approach speed.
3. Materialize: a virtual responder becomes an entity when it comes within 128 blocks of a player, at its virtual position, still walking in.

**Brain parts.** Memories `yearofice:heard_call` (pos, call id, q, game time) and `yearofice:call_shy_until` (game time). Behaviors `CallApproachBehavior` (stop-and-listen every 50–100 m), `HangUpBehavior`, `CircleDownwindBehavior` and `AnswerCallBehavior`. Activity `yearofice:respond_to_call` sits above Routine and below Flee.

**Sound events.** A sound is sent with volume equal to its range divided by 16 and `attenuation_distance` 16 in `sounds.json`, so the client hears it as far as the AI does. Beyond 128 blocks the `_far` variant plays, filtered thin.

| Sound event | Range in blocks |
| --- | --- |
| `yearofice:call.moose_cow`, `call.water_pour` | 800, 400 |
| `yearofice:call.moose_grunt`, `call.rake` | 500, 300 |
| `yearofice:call.squeak_distress`, `call.squeak_mouse` | 300, 60 |
| `yearofice:call.grouse_hen`, `call.grouse_wing`, `call.ptarmigan` | 80, 100, 150 |
| `yearofice:call.duck`, `call.goose` | 400, 600 |
| `yearofice:call.howl`, `call.owl`, `call.raven` | 5,000 by the distant-sound payload (the 1,024-block cap is dropped, RECONCILE 79), 600, 500 |
| `yearofice:call.tail_slap`, `call.whistle` | 150, 300 or 600 |
| `yearofice:entity.moose.answer`, `entity.wolf.answer_howl` | 800, 3,000 by the distant-sound payload |
| `yearofice:entity.great_horned_owl.hoot`, `entity.american_dipper.song`, `entity.osprey.dive` (mob sounds are `entity.<species>.<call>`, Conflicting ids row 20) | 800, 50, 100 |

## Animal encounters

Encounters are the moments players retell. The director on top of Populations decides when a meeting may happen and stages it with real animals from the region's count, so nothing extra is created. It keeps danger rare, spaces it out and leaves quiet days, while the animals themselves decide what happens next.

### The director's rules

**What it counts.** Every encounter counts toward the budget, whether the director staged it or the player walked into it. The early scare, staged hunts (Who hunts whom), wolf tests and bear camp visits keep their own rules and count too.

**Danger levels.** Each encounter type has a danger level: 0 calm, 1 tense, 2 dangerous.

1. **Slots.** At 06:00, or on waking, the director opens 0–2 slots for the day: up to 2 from May to October, 1 from November to April.
2. **Danger spacing.** Staged danger-2 encounters are at least 48 game hours apart. An unstaged one, such as a bear the player walks into, always happens and restarts that clock.
3. **After danger, calm.** For 24 game hours after any danger-2 encounter, only calm types can be staged.
4. **Quiet days.** At least 1 day in every 4 has no staged encounter, and 2 in every 7 from November to April.
5. **Variety.** No species is staged twice within 3 days. Animals that come to a feature the player is watching are the draw, not the director, and are always allowed.
6. **Body guard.** Nothing dangerous is staged while Condition or warmth is under 30, during the first 7 nights, or in water.
7. **Real animals only.** A slot takes an individual from the Nearby tier whose home range covers the spot. If there is none, the slot lapses.
8. **Opportunity, not force.** Each slot waits for its trigger during the day. A slot that has not fired by 22:00 lapses; only the early scare is forced.

**Choosing a type.** Each slot picks a type by weight: the share of that type's season target still unmet, times 2 if its trigger place is within 300 m of the player now. Cooldowns from the type table remove types until they clear.

### Staging

**Every staged animal is placed by the same rules.**

1. **Out of sight.** The animal is placed where a ray from the player's eyes to three points on its body is blocked, or beyond 160 blocks. It is never placed in view.
2. **At the edge of view.** It enters view at 80–150 m on open ground, 30–60 m in forest, or 40–120 m across water.
3. **Wind by intent.** Watching types (sighting, predator on a kill, fighting, the fishing grizzly) put the animal upwind 80% of the time, so it cannot smell the player. Tension types (stalking, close pass from behind) put it downwind, because it is following scent.
4. **Sound first.** A cue from its real position plays 3–15 s before it comes into view: a splash, a snapped stick, a grunt, a squirrel scolding, ravens, a hare's scream.
5. **Never close.** It is never revealed inside 25 m. Camp visitors and night-trail animals start 120 m out and walk in.
6. **Then it is free.** Once revealed, it runs the normal AI. Left alone, it goes back to its routine within 2–20 game minutes and leaves along its trail.

**B7 Talk as you walk (Proposed; Ruhan may veto).** A voice toggle that keeps bears and moose from being surprised in thick brush, at the cost of scaring game. Berry pickers in bear country talk, sing or clap so a bear hears them coming; the game makes that a choice with a price.

- **Where and when.** Anywhere, but it matters in berry patches, willow and alder from July 15 to September 15, when grizzlies feed in the same brush as the player (Danger in detail; Plants and fungi › Berries).
- **The toggle.** Double-tap V. The survivor talks, hums or claps every 15 game seconds until V is double-tapped again. The existing V tap (Talk) and V hold (the voice wheel, Shout by default) are unchanged (Minecraft development › Input and keybinds).

| Ground | Heard at |
| --- | --- |
| Open ground | 80 m |
| Dense cover: willow, alder, a berry patch | 50 m |
| Beside a riffle or rapids | Half the distance |

- **What it does.** A bear or moose that hears the player before it sees or smells them turns Alert and avoids: it moves off 50–150 m. That happens 85% of the time, or 70% for a sow with cubs, and for that animal the "Surprised inside 25 m" trigger (Blueprint › Danger in detail › How a bear decides) cannot fire.
- **The cost.** Every animal within the heard radius gains +40 awareness. Grouse and ptarmigan flush. No call works. Stalking is impossible. Picking speed is ×0.95. So it is a choice between safety and game.
- **The director.** It may still stage the berry-patch bear, but with talk on, the bear starts Aware at 50 m or more, never inside 25 m (Staging rule 5 already keeps staged animals beyond 25 m; this covers the unstaged ones).
- **What the player sees and hears.** The survivor's own low voice, hum or clap (`body.talk_walk`, with `body.hum` as one of its variants), a small mouth glyph by the load meter while it is on, and bears turning away at the edge of sight. The warn-once `talk_in_brush` fires on entering a berry patch or alder thicket inside a grizzly range in the season: "Thick brush. Talk as you walk (V V)."
- **Failure cases.** 15% of bears (30% of sows with cubs) do not move off, and the normal bear decision runs. Beside rapids the voice carries half as far. Leaving it on while hunting empties the patch of game.
- **Ties.** Danger in detail (How a bear decides, bear spray in the off-hand), the encounter director, Layer 3: Scent and sound field, Senses by species, calls (no call works while talking), Plants and fungi (picking speed).
- **Build.** No new registry ids beyond the sound. The keybind adds a double-tap to the V mapping; the toggle rides the existing `yearofice:talk` payload with a `walk` flag. `ecosystem.encounter.TalkWalk` emits a sound-field event every 15 game seconds at the radii above. Data `prompt/talk_in_brush.json`. Sound `body.talk_walk`. Tuning `[danger] talk_radius_open_m = 80`, `talk_radius_cover_m = 50`, `talk_avoid_chance = 0.85`, `talk_avoid_chance_sow = 0.70`. Debug `/yoi danger talk`. Milestone M6. GameTest `yearofice:encounter/talk_prevents_surprise` (batch `m6`): 100 runs down a willow corridor toward an unaware grizzly; with talk on, Defensive inside 25 m happens in 5% of runs or fewer and the bear moves off in 80% or more; with talk off, the baseline holds.
- **Pillars.** Everything readable (the player chooses their own sound), Scarcity is the clock (safety costs game).

### Encounter types

Targets are encounters per season: spring (April 15 – May 31), summer (June – August), fall (September – October) and winter (November – April 14).

| Type | Danger | Trigger | Targets Sp / Su / Fa / Wi | Staging | Player options | Outcomes and rewards | Cooldown |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Sighting | 0 | Player outdoors with a 150 m sight line | 4 / 6 / 5 / 3 | Big game crossing a bar, sheep on a ledge, a fox on the ice | Watch, glass, plan a stalk | A "seen" journal entry; first sightings count for Firsts | 1 day |
| Close pass | 1 | Player still (fishing, sitting, in a blind) for 10 game minutes | 2 / 3 / 3 / 2 | Walks past at 15–40 m, upwind of the player | Hold still (awareness rolls every 5 s), or shoot | +3 Spirits if it passes unaware | 3 days |
| Standoff | 1 | Player on a trail, bridge or narrow place | 1 / 2 / 2 / 1 | The animal on the same trail ahead, round a bend, at 20–50 m | Back off and detour, talk calmly, wait (it leaves within 1–5 game minutes 70% of the time), or push it to Warn | +2 Spirits for a clean retreat | 4 days |
| Bluff charge | 2 | An escalated standoff or mother; never staged cold | 1 / 1 / 1 / 0 | Comes from the Warn and Charge rules | Stand for a bear, run for a moose, spray | A Near miss entry | 7 days |
| Real charge | 2 | Only from the charge rules; never staged | 0 staged | — | As in Danger in detail | — | No danger staged for 10 days after |
| Stalking the player | 2, or 1 for a lynx | Winter wolf test conditions; a lynx at winter dusk | 0 / 0 / 0 / 1 lynx, plus tests | A lynx follows at 50–80 m for 5–15 game minutes; wolves by the test rules | Turn and face it; a lynx leaves when seen | Tension with no harm from the lynx | 7 days |
| Animal in trouble | 1 | Near water, ice or deep snow | 1 / 1 / 1 / 1 | A moose bogged in overflow, a moose through thin ice at freeze-up, a fox caught in a rusted 1930s snare, a moose calf bawling alone | Help, take, or leave | Freeing it: +5 Spirits and a known animal (a freed fox bites for −3 Condition 30% of the time). Taking it: meat or pelt | 10 days |
| Mother with young | 1; 2 inside 50 m | Moose May 20 – August; sows May – July; fox kits and grouse broods in June | 2 / 3 / 0 / 0 | The young are heard first (calf bleat, cub bawl) at 60–120 m | Back away, or watch from far off | +3 Spirits, and a calf the player can watch grow | 5 days |
| Two animals fighting | 0 far, 1 close | Rut bulls at a wallow; rams in November; bears at a riffle; wolves and a bear or wolverine at a kill; an eagle robbing an osprey | 0 / 1 / 2 / 1 | Heard first: antler clash to 1 km, roars, growls | Watch, close in, or take the winner | +5 Spirits; a broken tine or shed antler left behind; the rut winner charges inside 30 m | 7 days |
| Predator on a kill | 1, or 2 for a bear | A kill from the region's budget (Who hunts whom) | 1 / 1 / 1 / 2 | Ravens heard first, then the predator at 80–200 m | Wait and scavenge, drive wolves off with fire, never contest a bear | Meat by the carcass succession rules | As staged hunts |
| Curious animal | 0 | Player still in the animal's habitat | 2 / 3 / 3 / 2 | Marten (existing), otter peeking from water, ermine, a young fox, a gray jay at camp, a caribou in September | Hold still; it comes closer. A caribou circles to wind the player, and an arm held over the head like an antler brings it 30 m closer 30% of the time | +2 Spirits; its sign is learned | 2 days |
| Animal in camp | 1, or 2 for a bear | Food left out, a habituated animal, a porcupine or wolverine range over camp | 1 / 2 / 2 / 2 | Arrives at night or while the player is away; found by sound (gnawing, a clatter) | Shout it off, take it, or tolerate it | Losses, meat, or a named animal | 3 days; bears by the camp rules |
| On the trail at night | 2 for bear, moose or wolf | Travel on a trail after dark | 1 / 1 / 2 / 2 | Eyeshine in the light at 20–40 m, then a sound | Stop, talk calmly, raise the light, detour | Tension; a Near miss if it charges | 4 days |
| Crossing paths at water | 1 | Player at a water feature in its busy hours | 3 / 4 / 3 / 1 | The animal arrives at the far approach point and makes its edge pause | Watch, dive-stalk, shoot, let it drink | A hunting chance; water notes | 2 days |

**Porcupines love salt.** A porcupine whose range covers camp chews sweaty tool handles left outside: −20 durability to one handle on 20% of nights. Tools hung under a roof or inside are safe.

**Eyeshine tells the species.** In a lit circle, moose eyes glow dull amber high off the ground, wolves green-gold at knee height, bears orange at waist height, and foxes and lynx small and bright. The player learns to read eyes before shapes.

### Six moments

These read as scripted but are only the rules above meeting each other. Each lists its pieces, so tests can build it.

**Cow and calf in the willows.** June, a willow island beside a weed pond. The calf bleats at 60 m, then the cow's ears show above the willows at 40 m. Closing inside 50 m brings the Warn: ears back, hackles, a grunt. The right move is to back off and circle 150 m. In some Junes a grizzly works the same island (Who hunts whom), and the cow charges it instead. *Pieces:* mother with young, the weed pond draw, moose charge rules, calf predation.

**A grizzly fishing the riffle.** August dawn at a riffle. Splashes and the slap of a fish carry 150 m before the bear is in view. It stands in the riffle facing upstream, so a watcher downstream on the far bank is outside its sight, and river noise halves its hearing. Half-eaten fish wash to the snags below (Who hunts whom). A second bear arriving starts a fight. *Pieces:* riffle draw, salmon run, staging upwind, two animals fighting.

**Wolves run caribou onto the ice.** A straggler band (below) on the north edge in early November, with the big lake under 10+ cm of clear ice and little snow. The pack pushes 4–8 caribou onto the glare ice, where caribou run at 40% speed and wolves at 70%. The chase lasts 2–4 game minutes and ends in a kill half the time, 200–600 m off shore. Ravens come within the hour. *Pieces:* predator on a kill, ice growth, the straggler rule, carcass succession, thin-ice rules for a player who walks out.

**The straggler band.** In a year whose caribou route crosses the core's north edge, a band of 4–8 caribou stays in the north tundra regions until November 20, 25% of the time. When no band stays, the same chase plays out with a moose on river ice in late winter, from the deep-snow staged hunt in Who hunts whom.

**A lynx takes a hare in front of the player.** Winter dusk, a still player in hare cover, twice as likely in a hare-peak year. The player sees the hare first, feeding at 20–30 m. The lynx is seen only at ×0.3 sight range until it moves; then three bounds, and the hare escapes 40% of the time (Who hunts whom). *Pieces:* close pass, staged hunts, lynx senses, the hare cycle.

**A wolverine at the cache.** A raid in progress (Who hunts whom) when the player arrives within its game hour. Tearing and a low growl carry 60 m. The wolverine holds its ground at the food, under the rules in How animals act. The player shoots it for the best ruff in the game, drives it off (it drags away 5 kg), or sets a deadfall at the cache for its return. *Pieces:* animal in camp, cache raids, wolverine behavior, traps.

**A bull answers across the pond at dusk.** Late September, 19:00, calm, a pond on the caller's downwind side. One moan carries the full 1,200 m over the water. A bull answers from 600 m and comes in grunting and thrashing willows, then hangs up on the far shore at 90 m. A rake and a pour bring him into the water to 35 m, chest deep. Shooting him there means hauling him from the water; letting him walk out is the other ending. *Pieces:* the cow moan, call carry over water, hang-up, raking and pour, the bull in the water.

### Known animals

**Big animals become characters.** A moose, bear, wolf, lynx or wolverine met in 3 encounters (2 for bears) becomes Known, and the messenger offers "Name it." It gets a visible mark from its id: a torn ear, an odd antler, a pale saddle, a scar. The journal then tracks its story, and its antlers grow each year.

**Known animals die on-screen.** The rule in Who hunts whom for named animals extends to Known ones. An off-screen kill of a Known animal becomes a staged attempt the next time the player is within 100 m.

### The encounter journal

**The messenger stays silent in the moment.** No distances, no warnings and no prompts during an encounter, beyond the alerts the Blueprint already defines (the first bear's line, danger interruptions). The encounter ends when the animal is 150 m away and out of sight for 2 game minutes, or dead. Then the journal writes one entry, and the messenger chimes once the next time it is opened.

**Entry text.** One or two sentences in the journal's voice: date and time, species and who, place by name, closest distance, what the player did and how it ended. It never states a hidden number.

- "Sep 24, 19:12. Bull moose answered across the beaver pond. Came to 34 m in the shallows after the second rake. Let him walk."
- "Jun 8, 06:40. Cow and calf in the willow island at Salmon Bend, 45 m. She laid her ears back. Backed off."
- "Nov 3, 14:20. Five wolves ran two caribou onto the lake ice. One went down 400 m out. Ravens by 15:00."

**Entry record.**

```json
{
  "id": "enc-y1-142",
  "day": 147,
  "time": "19:12",
  "type": "crossing_water",
  "danger": 1,
  "species": "yearofice:moose",
  "individual": "moose#3f2a",
  "name": "Pond Bull",
  "place": { "feature": "beaver_pond_07", "region": "C3" },
  "closest_m": 34,
  "duration_min": 26,
  "player_actions": ["call_moan", "rake", "rake"],
  "outcome": "let_go",
  "weather": { "temp_c": 3, "wind_ms": 1.2, "wind_from": "NW" },
  "spirits": 3,
  "staged": false
}
```

**The year card counts them.** The May 1 year card adds one line of encounter counts: "41 encounters: 12 moose, 3 bluff charges, 1 lynx kill seen." The map keeps a small pin at each entry's place for 30 days.

### In Minecraft: encounters

**The director is per player.** `EncounterDirector` is a `SavedData` (`yearofice_encounters`) with one record per player: slots, cooldowns, the danger clock, quiet-day history and season counts. The game ships solo first; the record is per player so co-op can be added after 1.0 without changing it (RECONCILE 115).

**Staging uses the virtual agents.** `EncounterStager` picks an individual from the Nearby-tier simulation, finds a hidden position with `ClipContext` rays from the player's eyes, and materializes the entity 20–40 blocks beyond its reveal point so it walks into view. The sound cue plays when it is 10–20 s from line of sight.

**Roles bias the brain.** Memory `yearofice:encounter_role` holds one of `PASS_BY`, `BLOCK_TRAIL`, `CURIOUS`, `WATCH_ME` or `NONE`. Each role adds one behavior (`PassByBehavior` paths 15–40 m from the player on the upwind side) and clears itself after the reveal or 10 game minutes.

**Eyeshine.** GeckoLib's glow layer draws an emissive eye texture only when the entity sits inside the player's light cone within 40 blocks and the light level is under 5.

**The journal.** `EncounterJournal` writes the record to the player's journal data and renders the text from the `encounter_lines` templates in the lang file.

## Data and build

### Registry ids

| Kind | Ids |
| --- | --- |
| Blocks | `yearofice:silt_layer`, `driftwood_pile`, `shove_ice`, `aufeis`, `snag_log`, `pond_mud`, `sedge_mat`, `slump_mud`, `ground_ice`, `ice_scarred_spruce_log`, `crib_wall`, `riprap`, `sod_dike`, `pond_leveler`, `water_bar`, `water_marker`. The plants `pond_weed`, `horsetail` and `water_horsetail` are Plants and fungi ids |
| Items | `yearofice:moose_call`, `squeak_call` (existing); `moose_scapula`, `grouse_wing`, `duck_call`, `goose_flute`, `decoy_wood`, `decoy_bark`, `decoy_mound`, `ice_chunk`, `ice_age_bone` |
| Entities (water roles) | Reused from the Waters and fish tab: `yearofice:mink`, `bald_eagle`, `american_dipper`, `salmon_carcass`. New here: `yearofice:osprey`, `tree_swallow` (ambient). Other species by their Bestiary ids |
| POI types | `yearofice:poi_weed_pond`, `poi_riffle`, `poi_ford`, `poi_falls_pool`, `poi_creek_mouth`, `poi_beaver_pond`, `poi_molting_lake`, `poi_open_water`, `poi_lick`, `poi_wallow`, `poi_bug_refuge`, `poi_still_surface` |
| Memories | `yearofice:water_target`, `sodium`, `last_drank`, `head_under`, `heard_call`, `call_shy_until`, `encounter_role` |
| Sensors and activities | `yearofice:water_draw_sensor`, `call_sensor`; activities `water_visit`, `respond_to_call`, `staged_encounter` |
| Attachments | Chunk: `yearofice:placed_mask`, `flood_flags`, `erosion_counters`; read: `yearofice:current` (river current) and `yearofice:sign` (tracks in flood mud and pond mud) |
| Payloads | `yearofice:call_performed` (client to server) |
| Data component | `yearofice:call_practice` (uses, cap) on call items |

### Data files

| Path | Holds |
| --- | --- |
| `data/yearofice/terrain_process/<id>.json` | One file per process in Water reshapes the land: rates, ticks, caps |
| `data/yearofice/data_maps/block/bank_erodibility.json` | Bank retreat in blocks a year per block (silt and sand 1.0, gravel 0.6, clay 0.3, frozen silt 0.5) |
| `data/yearofice/water_feature/<id>.json` | One file per feature type: radius, windows, the drive it serves |
| `data/yearofice/species/<id>.json` | A new `water_draw` block per species (weights and hours) |
| `data/yearofice/call/<id>.json` | One file per call |
| `data/yearofice/data_maps/item/call_practice.json` | Practice factors per call item |
| `data/yearofice/encounter/<id>.json`, `data/yearofice/encounter_director/default.json` | Encounter types and director rules |
| `data/yearofice/prompt/bank_retreat.json`, `dam_full.json` | The two new prompts |
| `data/yearofice/recipe/<id>.json` | Duck call, goose flute, decoys, pond leveler, crib wall, riprap, dike, water bar |
| `data/yearofice/tags/block/erodible_bank.json` | Blocks that cut banks lose |
| `data/yearofice/worldgen/placed_feature/water_markers.json` | Marker placement from the hydrology layer |
| `assets/yearofice/sounds.json`, `assets/yearofice/lang/en_us.json` | Call and answer sounds; `encounter_lines` templates |

### JSON shapes

```json
{
  "process": "cut_bank_erosion",
  "ticks": { "breakup": 0.5, "freeze_up": 0.5 },
  "rate_blocks_per_year": "data_maps/block/bank_erodibility",
  "bend": [ { "max_radius_widths": 2, "k": 1.5 }, { "max_radius_widths": 3.5, "k": 1.0 }, { "max_radius_widths": 5, "k": 0.5 } ],
  "flood_year_factor": 3.0,
  "undercut_at": 0.6,
  "bar_share": 0.8,
  "cap_edits_per_chunk_per_tick": 64
}
```

```json
"water_draw": {
  "weed_pond": { "weight": 1.0, "window": ["06-10", "08-15"], "hours": "dawn,dusk,hot_midday", "drive": "sodium" },
  "mineral_lick": { "weight": 1.0, "window": ["05-20", "07-15"], "after": { "weight": 0.5, "until": "08-31" }, "drive": "sodium" },
  "bug_refuge": { "weight": 0.5, "window": ["06-10", "07-31"], "hours": "11-17", "drive": "safety" },
  "disturbance_floor_near_camp": 0.7
}
```

```json
{
  "id": "yearofice:cow_moan",
  "item": "yearofice:moose_call",
  "input": { "kind": "hold", "seconds": [2, 4], "flourish": "look_down_last_0.5s" },
  "responders": { "yearofice:moose": { "p": 0.4, "season": "rut_factor", "falloff": "linear", "sex": "male", "cow_chance": 0.15 } },
  "range_m": 800,
  "min_interval_min": 10,
  "wrong_season": { "shy_days": 7, "radius_m": 400 },
  "sound": "yearofice:call.moose_cow"
}
```

```json
{
  "id": "mother_with_young",
  "danger": 1,
  "danger_inside_m": 50,
  "targets": { "spring": 2, "summer": 3, "fall": 0, "winter": 0 },
  "cooldown_days": 5,
  "staging": { "reveal": "sound_first", "reveal_m": [60, 120], "wind": "upwind" },
  "species": ["yearofice:moose", "yearofice:grizzly", "yearofice:black_bear", "yearofice:red_fox", "yearofice:spruce_grouse"]
}
```

### Java modules

| Package | Classes |
| --- | --- |
| `sim.terrain` (pure Java) | `HydroPlanner`, `ChangeOp`, `OpMerger` |
| `world.change` | `TerrainChangeQueue` (SavedData `yearofice_terrain`), `ChangeApplier`, `FloodController`, `IceJamController`, `AufeisController`, `PermafrostController`, `ProtectionWorks` |
| `ecosystem.water` | `WaterFeature`, `WaterFeatureRegistry` (SavedData `yearofice_water_features`), `WaterDrawSensor`, `WaterVisitBehavior`, `EdgePauseBehavior`, `HeadUnderFeedBehavior`, `StandInWaterBehavior`, `Disturbance` |
| `hunt.call` | `CallDef`, `CallItem`, `CallPerformedPayload`, `CallBroadcaster`, `CallApproachBehavior`, `HangUpBehavior`, `CallShyness`, `RavenLead` |
| `client.hud` | `CallRhythmOverlay` (a renderer in the `minigame` layer); `client.render` the eyeshine glow layer |
| `ecosystem.encounter` | `EncounterDirector`, `EncounterType`, `EncounterStager`, `EncounterJournal`, `KnownAnimals`, `PassByBehavior` |

All under `com.yearofice`, as Minecraft development › Package and module map sets them (RECONCILE 32, 80; the lead moved the queue's SavedData and appliers to `world.change`).

### Milestones

| Milestone | Ships |
| --- | --- |
| M6, Danger and big game | Water features and the draw, the sodium drive, dive stalking and the edge pause, every call and decoy, call-shyness, the encounter director, staging, the journal, known animals |
| M7, The year | Flood block rules and the piece table, jam sites and ice shove, aufeis, springs and leads, the molting lake, swallows and dragonflies, the straggler band |
| M9, Homestead and beyond | The terrain-change queue's long processes: erosion, bars, braid switching, oxbow cutoffs, dam failure and beaver meadows, pond silting, permafrost thaw and slumps, gullies, falls retreat, and all protection works |

### Tests

Each is a NeoForge GameTest named `yearofice:<system>/<test>` and batched by milestone: tests 4–6 in the M6 batch, 3 in M7, 1–2 in M9. Simulations run their pure-Java models inside the test, so each finishes in under 60 real seconds.

1. **`yearofice:sim/bank_retreat`.** On the `bend_silt` template (silt bank, radius 2.5 widths), 10 simulated years with flood years 3 and 7: the outside bank retreats 11–13 blocks, the inside bar gains at least 80% of that, no `placed_mask` block changes, and no chunk takes more than 64 edits in a tick.
2. **`yearofice:sim/terrain_determinism`.** The same seed and history run twice, with chunk load order shuffled, give identical SHA-256 hashes of all applied ops. Adding one crib wall changes ops only within 8 blocks of it.
3. **`yearofice:sim/flood_marks`.** On the `river_flat` template, a flood-year jam lifts water to the flood-year line (+3.5 blocks) within 12 game hours. After it falls, at least 80% of flooded cells with current under 0.5 m/s carry silt; a ground chest's wooden items sit at the snag fence; a cache raised 2.5 m keeps 100% of its contents.
4. **`yearofice:ecosystem/water_draw`.** Over 30 simulated July days, a moose whose bed is 400 m from a weed pond visits it on at least 20 days. With the player sleeping 60 m from the pond, it visits on 6 days or fewer. A player walking at 25 m during head-under dives stays undetected in at least 95% of 200 trials.
5. **`yearofice:hunt/call_response`.** A distress squeak at dusk, calm, q 1.0, brings a fox 200 m away within 15 game minutes in 45–55% of 400 trials, and 12–18% when the fox is call-shy. The existing moose test still gives 30–50% at q 1.0.
6. **`yearofice:ecosystem/encounter_director`.** Over 100 simulated summers, no two staged danger-2 encounters fall within 48 game hours, every 4-day window holds a quiet day, each type's count lands within 25% of its target, and no staged animal exists outside its region's count.
