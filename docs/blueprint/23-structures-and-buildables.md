# Structures and buildables

This tab adds forty-nine structures, vehicles and buildables for the home site and the trail, each sized in Minecraft blocks and costed in game hours and kcal. It also sets the building rules the other tabs leave open: support, snow load, fire, animals, rot, permafrost and repair. Pieces already in Crafting and building in depth, Camp, materials and builds, Traps and the Outposts section of Open world in depth are referenced, never repeated.

## Building in Minecraft

### Three kinds of buildable

Everything here is a piece, a multiblock or a vehicle. Sizes are whole blocks, written W × D × H, with H counting the roof. The 0.5 m pieces of the other tabs are mapped onto blocks and slabs by the Minecraft engine tab.

| Kind | Minecraft form | Examples | How it is placed |
| --- | --- | --- | --- |
| Piece | One block, often with a block entity for HP and age | Shingle roof, boardwalk plank, snow fence | A frame block, then hold right mouse, as in Placing and snapping |
| Multiblock | A controller block entity plus a pattern of member blocks | Banya, kiln, ice house, grist mill | Members are placed freely; the controller checks the pattern |
| Vehicle | An entity built on a jig or bed block | Kayak, toboggan, moose-skin boat | Built on its station; leaves as an entity with an inventory |

**Controller rule.** Each multiblock has one controller block, such as `yearofice:banya_heater`. Its block entity stores the pattern id, rotation, member positions, HP, state and owed upkeep. It checks the pattern when placed, when any block in its bounding box changes, and every 100 ticks.

**A broken pattern pauses the structure.** The controller names the missing member in the messenger: "Banya: no door on the east wall." Nothing is lost; the structure resumes when the block returns.

**Frames.** A frame is `yearofice:frame`, a block with no collision that renders as the white or amber ghost. Its block entity holds the target block state and the materials still owed. Holding right mouse converts it once materials are reserved from the pack and storage within 10 blocks.

**Blueprints are structure templates.** A saved blueprint is a vanilla `StructureTemplate` holding block states and no items, saved at `<world>/generated/yearofice/structure/blueprint/<player>/<name>.nbt` (K opens blueprint mode, Crafting and building in depth › Placing and snapping). Every multiblock in this tab also ships a built-in template at `data/yearofice/structure/blueprint/<id>.nbt`. It appears in the notebook once its recipe is known, and stamping it places frames.

### Structural support

**Support is computed, not stored per block.** `SupportGraph` runs a breadth-first pass from ground-contact blocks when a block within 16 blocks is placed or destroyed. Results are cached per chunk section as a data attachment, so no blockstate carries a number.

The existing rule stands: foundations give 100 and pieces spend it, and below 0 the ghost turns red. In blocks the costs are these.

| Block class | Examples | Cost | Notes |
| --- | --- | --- | --- |
| Ground contact | Corner stone, gravel pad, crib, stone wall | Gives 100 | Soil or gravel under it; snow and ice give 40 |
| Wall | Log wall block (3.3 course-metres), plank wall, stone | 7 per block up | A log wall 3 blocks high spends 21 |
| Post or prop | Pole, post, `yearofice:prop_post` | 5 per block up | Props add a new path down |
| Beam | Ridge, purlin, sill, stringer, dock joist | 10 per block across, up to 4 blocks | The 4.5 m ridge rule; beyond 4 blocks, 40 per block |
| Light span | Pole-and-bark, shingle, plank floor, boardwalk | 20 per block across | From the nearest wall, beam or post |
| Heavy span | Sod roof, stone slab, ice block, hay | 40 per block across | Why sod roofs need purlins every 2 blocks |

**Load-bearing blocks cannot be pulled out.** Taking one down while it carries others is refused with a red ghost: "Holds up 6 blocks. Prop it first." The prop costs 1 pole and 5 game minutes.

**Damage can still remove support.** When fire, a bear or rot destroys a block, the blocks that lose their path to ground turn Sagging. They show a crack overlay, drip in rain and lose 50 HP a game hour. A prop placed in time saves them; at 0 HP they drop as items. There is still no physics collapse.

### Snow load

**Roof snow is the world's snow block.** It is `yearofice:snow_cover` with `layers` 1–8 of 12.5 cm, as on the ground (Minecraft development › World and worldgen › Snow cover and permafrost blocks), never vanilla snow. So the 60 cm limit in Shelter and building is 5 layers and the braced 120 cm is 10: a full first block with a second `snow_cover` of 2 layers on it. Snow falls on roofs at the day's snowfall through `world.pass.SnowPass`.

```latex
load = \frac{s_{cm} \cdot \rho}{cap} \qquad \rho = 1 \text{ fresh},\ 2 \text{ after a thaw day or rain on snow}
```

cap is 60 cm unbraced and 120 braced, as before. A thaw day turns 30 cm of snow into a full 60 cm load overnight, which is the classic late-winter collapse.

- **Pitch sheds snow.** A roof of stair blocks at 1:1 sheds all snow above 30 cm on any day above −5 °C or with wind over 8 m/s. A shingle roof at 1:1 sheds above 20 cm. Flat sod and pole roofs never shed.
- **Drift.** The downwind half of a roof collects ×1.5 snow after a wind over 6 m/s.
- **Warnings.** At load 0.8 the roof creaks once an hour inside. At 1.0 the Trail Messenger says: "Roof creaking under the load. Shovel or brace it."
- **Collapse.** A roof block at 0 HP drops with its snow into the room. Anyone under it loses 20 Condition, and the room stops being a room in the heat model.
- **Shed snow piles up.** Snow slid off a roof lands against the wall below. It counts as the shoveled snow bank in Warm rooms, cutting that wall's U by 30%.

### Fire risk

Vanilla `doFireTick` is off. `EmberSpread` replaces it, and the existing rules stand: wood in contact with flame loses 50 HP a game minute, and the chimney fire follows Creosote.

| Source | Ember radius | Chance per game hour that an ember lands on a flammable block |
| --- | --- | --- |
| Campfire at Steady | 2 blocks | 1% |
| Campfire at Hot | 3 blocks | 4% |
| Campfire packed too full ("may spread") | 3 blocks | 10% |
| Stove with pipe through a clay collar | — | 0% |
| Pipe through a wood roof with no collar | 1 block | 2% at Hot |
| Banya heater with wood within 1 block | 1 block | 2% per firing |
| Kiln or charcoal burn | 4 blocks | 3% |

**Flammability.** An ember ignites a block with chance flammability × dryness, where dryness is 1.0 dry, 0.4 after rain and 0 under snow. Values: shingle 1.2, bark or birch-bark roof 1.5, hay and thatch 2.0, dry spruce wall 1.0, green log 0.3, sod, clay, stone and ice 0.

**Spread.** A burning block lights each flammable neighbour with chance 0.3 × flammability every 30 game seconds. One 4 L bucket of water or one shovel of snow or soil puts out one burning block in 3 game seconds.

**Firebreaks.** Cleared ground (gravel, garden, mown grass or bare soil) 10 blocks wide around a building halves wildfire embers reaching it. A sod roof is immune. That is why old Alaskan cabins wore sod, and the year card says so when a cabin survives a fire.

### Animal damage

The door and cache rules in Shelter and building stand: a wolf does 10 HP a hit and a bear 50 a swipe. These are new.

| Animal | What it damages | Amount | When | What stops it |
| --- | --- | --- | --- | --- |
| Porcupine | Any wood touched by sweat or urine: outhouse seat, banya bench, tool handles left out, harness, door handles | 30 HP a night | Nights, March–October | Tin wrap; a wolf on Guard; hanging tools; killing it (3,000 kcal) |
| Red squirrel and vole | Moss chinking pulled for nests | 1 m of seam turns unchinked a month | September–October | Clay daub over the moss |
| Bear | Sod roofs of caches and ice houses, cellar lids, smokehouses; chests and barrels on the ground (50 HP a swipe, emptied at 0, the video's smashed chests) | 1 roof block opened a visit | Mostly April–May | Height, iron bands, a wolf yard within 30 blocks |
| Moose | Haystacks (10 kg a night); fence posts rubbed in the rut | −20 HP a night to a pole fence | Hay: November–April; rut: September | A pole fence ring at 2 blocks high |
| Beaver | Birch and aspen within 30 blocks of its pond; mill races and culverts plugged | 1 tree a week; a race closed 10% of weeks | Open water | Trapping the colony; a wire cage on key trees (3 m of snare wire, 5 game minutes a tree; Crafting and building in depth › Trees, felling and firewood) |
| Snowshoe hare | Bark of young willow and birch in windbreaks and hedges | 10 HP a night | Winter, worst in a peak hare year | A brush ring |
| Wild wolf pack | Wolf yard, hide yard, gut barrel | Tests the yard 5% of weeks in a hard winter | Winter | Barred doghouses; a fire in the yard |

Porcupine gnawing is the surprise players tell friends about. The outhouse seat goes first, then the axe left in the chopping block. The messenger's morning line names it: "A porcupine chewed the axe handle. 40% left."

**Door left open (video 2, A2; RECONCILE 170).** An open door or hatch lets animals into a building, and the game warns once before dark. In the second video a wolf walked in through an open cabin door, and an Alaskan viewer wrote "I always forget to shut the doors".

**The dusk warning.** Thirty game minutes before civil dusk, the building the player last slept in is checked: the room in `yearofice_rooms` that holds their last bed. If any of its doors or hatches is open, warn-once id `door_open` fires: "Cabin door open. Close it before dark." Within 100 m it shows on the messenger and as a prompt near the crosshair; farther away it is a messenger line only, or a journal line without the device. Warn once's rule holds: it fires again only after the doors have been shut for 24 game hours.

**Who comes in.** An open door is the `DOOR_OPEN` path type (Minecraft development › Pathfinding in voxel terrain), so animals can path through it. While the player is more than 20 m away, each open building rolls once a game hour. Scent is on the Items scale of 0–3.

| Animal | When | Condition | Chance per game hour | What happens |
| --- | --- | --- | --- | --- |
| Red fox, marten, ermine | Night | Food scent 0.3 or more inside | 5% | Takes one food item of 1 kg or less and leaves |
| Wolf | Any time | The pack's hunger drive is 0.6 or more, the pack is within 300 m and food is inside | 2% | Stays inside until driven out by noise or fire |
| Grizzly or black bear | Any time, outside the den months | Scent 0.6 or more | 3% | Stays until driven out; opens chests and barrels by the bear rule above (50 HP a swipe, emptied at 0) |

**What the player finds.** Every entry leaves fresh tracks on the threshold and a messenger line: "Fox tracks at the cabin door. 0.4 kg of dried fish gone." A wolf or bear still inside follows the existing standoff rules (Blueprint › Danger in detail): it defends only when cornered and never hunts the player. A shout, a banged pot or a lit torch at the door drives it out in 1–3 game minutes, by those rules.

**Failure cases.** A hide door flap counts as closed but wolves still tear it (Crafting and building in depth › Building pieces). A door left open while the player sleeps inside rolls nothing, since the player is within 20 m. A bear inside at night makes the cabin a standoff at the doorway, the danger the warning exists to prevent.

**The world option.** Entries roll only while `[world] animals_enter_open_doors` is on. It is on by default in every preset and can be changed after world creation (Minecraft development › World creation options and presets). Off, no animal enters and `door_open` never fires; heat still leaks through an open door (Warm rooms).

**Ambition.** The existing "Held the door" gains a twin, "Shut tight": no animal enters any of the player's buildings from November 1 to March 31 (Open world in depth › Ambitions).

**Ties.** Rooms and doors (Warm rooms), scent (Minecraft development › Layer 3), wolf packs and bear dens (Land animals in depth), warn once and the messenger, and the year: longer nights mean more night hours to roll.

**Build.** `build.rooms.DoorWatch` checks the slept-in room at dusk and posts `door_open` through `HudBus`; `ecosystem.raid.ShelterEntry` rolls entries, with the odds in `sim.ecology.RaidOdds` (no Minecraft imports). Brain activity `yearofice:raid_shelter` and memory `yearofice:open_door_target`. Tuning `[ecosystem] door_entry_scale` (Gentle 0.5, Standard 1.0, Brutal 1.5); world option `[world] animals_enter_open_doors = true`. Ambition `data/yearofice/ambition/shut_tight.json`. Debug `/yoi raid door <species>`. Owner Ecosystem. Milestone M6.

**GameTests.** `yearofice:ecology/door_entry_rates` (M6): an open cabin holding 2 kg of dried fish inside a fox range, with the player 50 m away, has a fox entry in 5% ±0.5 of 10,000 night hours, none by day and none with the door shut. `yearofice:ecology/door_entry_toggle` (M6): with the option off, 1,000 nights give 0 entries and no `door_open` line. `yearofice:hud/door_open_warn` (M6): the warning fires 30 ±1 game minutes before civil dusk and not again until the door has been shut 24 game hours.

### Rot

The base rates stand: an unpeeled log loses 10% of its remaining HP a year and a peeled one 2%, so an unpeeled log keeps about 37% after 10 years. `RotModel` multiplies them once a game day.

| Condition | Multiplier |
| --- | --- |
| Wood block directly on soil | ×1.5; ×3 within 3 blocks of a heated room (drip line and warm soil, the sill case); ×0.9 on soil in forest shade (Decay and time › Ground) |
| On a corner stone, gravel or a crib | ×1 |
| Post end charred in a fire for 10 game minutes before setting | ×0.5 of the ground-contact rate |
| In bog or splash zone, wet and aired | ×4 |
| Fully under water, no air | ×0.2 |
| Under a roof and dry | ×0.5 |
| Birch | ×3 |
| Spruce | ×1 |

**Rot is readable.** Looking at a block for 2 seconds shows one word: Sound over 75% HP, Weathered 50–75, Soft 25–50, Rotten under 25. Rotten birch grows tinder conk, which the carried-ember rule in Building a campfire uses. Soft punky wood is the best fuel for the brain-tanning smoke below.

### Permafrost and frost heave

Stage 6 flags permafrost ground, where only the top 50 cm thaws. Black spruce, tussocks and leaning trees mark it, as in Other resources. `PermafrostSettlement` tracks each foundation corner of every heated room.

```latex
s_{year} = 4\,\text{cm} \cdot \frac{\bar{T}_{room}}{20\,°C} \cdot m_{floor}
```

m\_floor is 1.0 for a floor on the ground, 0.5 over 1 block of gravel pad or 3 moss sheets per block, and 0 over a 1-block ventilated crawl space on cribs. Corners settle unevenly: the corner nearest the stove settles ×1.5.

| Tilt (largest corner difference) | Effect | Fix |
| --- | --- | --- |
| 5 cm | The door sticks: opening takes 2 s | — |
| 10 cm | 2 m of chinking per wall turns unchinked | Re-chink |
| 20 cm | Windows crack (plexiglass survives); the stovepipe joint leaks 5% smoke a game minute | Re-seat the pipe, 30 min |
| 30 cm | Walls lose 10% HP a month | Jack and shim: 4 h and 1 log per corner |

**Frost heave.** Riverside silt and bog edges are flagged frost-susceptible. A post set less than 2 blocks deep there rises 3 cm each winter, so a fence opens a gap after 3 winters. Set at the full 2-block dig limit with gravel backfill, it rises 0.5 cm.

The site check adds an amber warning on permafrost: "Frozen ground. A heated floor settles about 4 cm a year." The fix is in the warning, so a player reads the land before building.

### Repair and upkeep

The repair rule stands: hold right mouse on a damaged block, spend 25% of its materials, restore 50% of its HP. These are new.

- **Inspect.** Looking at a controller for 2 seconds shows its card: HP, rot word, snow load, tilt and the next upkeep date.
- **The upkeep page.** The messenger lists every structure's due work by date: re-chink on September 1, regum the canoe, re-sod a roof, sweep the chimney, shovel roofs, pull the dock planks before breakup, relash a frame.
- **Mend all.** One button queues every repair within 50 blocks, filling materials from storage within 50 blocks, as blueprints do. The clock runs at the work-until-done rate under the queued-job rules.
- **Practice.** Repairs count as reps of the skill that built the piece, usually Log building.
- **Budget.** A year-3 homestead should spend 40–70 game hours a year on structure upkeep. That work counts toward the upkeep floor in The open world.

## Warmth, water and hygiene

Building time is costed at the high rate of 450 kcal a game hour unless a row says light or medium work. Tiers are the crafting tiers.

| Structure | Tier | Blocks | Materials | Build steps | Time | kcal |
| --- | --- | --- | --- | --- | --- | --- |
| Banya (smoke) | Bushcraft | 5 × 4 × 3 | 14 wall logs, 10 poles, 6 bark sheets, 6 m² sod, 60 river stones, 1 bucket clay, 4 planks, a hide flap | Corner stones and sills; walls 2 blocks high; stone heater and clay bed; bench; roof and sod | 36 h | 16,200 |
| Banya chimney upgrade | Iron | Same | 4 kg bar iron firebox, 2 m stovepipe, plank door | Set the firebox in the heater; pipe and collar; hang the door | 6 h | 2,700 |
| Log trail shelter | Bushcraft | 4 × 3 × 3 | 10 logs, 8 poles, 4 bark sheets, 20 boughs | Sills on stones; 3 walls; sloped roof; bough floor | 10 h | 4,500 |
| Outhouse | Bushcraft | 2 × 2 × 3 | A 2-block pit, 4 posts, 6 planks or 10 poles, 2 bark sheets, 1 seat plank | Dig the pit; set posts; walls and roof; seat | 4 h | 1,800 |
| Rain catchment | Bushcraft | Gutter 1 block per roof edge; barrel 1 × 1 | Bark gutter: 1 bark sheet and 2 pegs per block; a barrel sealed with 0.5 kg pitch | Hang gutters on the eave; place the barrel under the low end | 20 min per block; 1 h barrel | 150 per block; 450 |
| Gravel well | Iron | Shaft 1 × 1 × 2; well house 2 × 2 × 2 | 8 poles for the crib, 2 buckets gravel, 4 planks, 2 moss sheets, a bucket on 3 m cordage | Dig 2 blocks in thawed gravel; crib it; gravel floor; lidded well house | 10 h | 4,500 |
| Water-hole box | Bushcraft | 1 × 1 × 1 | 4 poles, 2 moss sheets, 1 bark sheet | Frame over a chiseled hole; moss lid; bark cover | 20 min | 150 |

**Banya.** A small log sweat house: a hot room of 2 × 2 blocks and a dressing room of 1 × 2, the Russian and Finnish settlers' design. It is the "sweat bath" in the Spirits table, built.

- **Heat.** Two armloads heat the hot room to 70 °C in 2 game hours as a smoke banya, or to 80 °C in 1.5 hours with the chimney. Ladling water on the stones (right mouse with a bucket) adds a +5 °C steam burst.
- **A session.** 20 game minutes inside sets warmth to 100 and body wetness to 0. It gives Spirits +6 once a day and Condition +3, and the next sleep recovers energy 10% faster.
- **Scent.** After a session, the player's scent is ×0.4 for 24 game hours. Senses by species reads it, and the trapping human-scent penalty eases from ×0.5 to ×0.8. Hunters bathe before the rut.
- **Snow roll.** Rolling in snow 30 cm deep within 5 blocks after a session (right mouse on the snow) gives Spirits +2 more.
- **Smoke banya danger.** While heating it holds 100% smoke. It must be vented for 30 game minutes with the door open. Entering early applies the smoky-room rule and a 5% blackout chance per game minute after 5 minutes.
- **Too long.** Over 40 game minutes costs 10 water and blurs the screen edges.
- **Emergent smoker.** Fish hung in a smoke banya's rafters during a firing get 10 kg of cold smoke, a quarter of a smokehouse day.
- **Upkeep.** 2 heater stones crack every 10 firings; wet river stones crack 3 at once. Porcupines chew the bench. Wood within 1 block of the heater takes the ember rule above, so banyas burn down more than any other building.
- **On screen.** Steam particles, the hiss of a ladle, blackened walls inside a smoke banya. The messenger: "Banya 78 °C. Vent first: smoke 40%."

**Log trail shelter.** A three-sided log shelter with an open front to a fire and reflector, permanent where a lean-to is not. It gives +6 °C asleep by the fire (+9 with the reflector), blocks all rain, and counts as the roofed piece of a trail camp outpost. Its logs last 20 years where a lean-to's poles last 2.

**Outhouse.** It replaces the latrine. Placed 30 blocks from water and downhill of it, it never fouls a source; a pit lasts 300 days, then a new one is dug in 2 h. A cup of stove ash a week keeps flies off, so bugs within 10 blocks stay at the base rate. Below −30 °C a bare seat costs Spirits −1 a visit, and a hare-pelt seat cover removes it.

**Rain catchment.** One millimetre of rain on 1 m² of roof gives 1 L. A 4 × 5 cabin roof in a 10 mm storm fills a 100 L barrel. Water from a bark, shingle or plank roof counts as clean for 2 days; from a sod roof it is brown and dirty, as in Water. A full barrel left below −10 °C splits: drain it in October, or lose half its HP. It covers the garden's 10 L per bed in a dry week.

**Gravel well.** Permafrost holds no water, so a well works only in thawed ground: river gravel within 30 blocks of a river or lake, or birch and willow ground. On permafrost the ghost is red: "Frozen at 0.5 m. No water here." It seeps 20 L an hour in summer and 5 in winter. The lidded house keeps it open to −40 °C and saves the 20-minute daily ice hole. Within 30 blocks of an outhouse, latrine or cleaning spot its water is dirty.

**Water-hole box.** A mossed lid over an ice hole. The hole skins over 2 cm a night instead of closing within an hour below −20 °C, as Fishing methods says. Reopening takes 2 game minutes instead of 20, which cuts the winter water chore by about 6 game hours a month.

## Snow, ice and trails

| Structure | Tier | Blocks | Materials | Build steps | Time | kcal |
| --- | --- | --- | --- | --- | --- | --- |
| Quinzhee | Start | 3 × 3 × 2 | 9 piled blocks of snow, 12 sticks 30 cm long | Pile snow 2 blocks high; wait 2 h to set; push in depth sticks; hollow out to the sticks; poke a vent | 2 h with a shovel, 4 by hand, plus 2 h waiting | 900 or 1,800 |
| Windbreak | Start | Up to 10 × 1 × 2 | 10 boughs per block, or 8 snow blocks cut from wind-packed snow | Stake a line across the wind; weave boughs or stack blocks | 15 min per block | 113 per block |
| Snow fence | Bushcraft | Up to 20 × 1 × 2 | Per block: 1 pole, 6 boughs, 1 m cordage | Set poles every block; weave boughs to half open | 10 min per block | 75 per block |
| Ice road | Iron | 3 blocks wide, any length | Shovel, ice chisel, birch-bark bucket; 1 spruce sapling marker per 30 blocks | Clear snow off a lane; chisel a hole; flood the lane by bucket each cold night; stand markers | 1 h per 30 m cleared; 1 h per 10 m flooded | 450 per hour |
| Ice bridge | Iron | 3 × river width | 1 pole per block of length, laid across; flooding as the ice road | Lay poles on 5 cm ice; flood over them nightly | 2 h plus 1 h a night | 900, then 450 a night |
| Boardwalk | Iron | 1 block wide | Per 10 blocks: 10 planks, 6 sleeper poles, 40 wooden pegs | Lay sleepers on the moss every 2 blocks; peg planks across | 3 h per 10 blocks | 1,350 |
| Stepping-stone ford | Start | 1 stone per block of riffle | 30 kg stones, carried in the hands | Carry and set one stone per block across water under 0.6 m | 10 min a stone | 75 a stone |
| Trail markers | Start | 1 block each | Tripod: 3 poles and a strip of red flagging from the crate. Cairn: 5 stones | Stand or stack at 30-block spacing | 5 min each | 38 |

**Quinzhee.** A hollowed snow pile, the best emergency shelter in deep cold. It needs 20 cm of snow on the ground and air at −5 °C or colder to set. Inside it holds T\_out + 25 °C but never above 0 °C, so −40 outside is −15 inside, and a candle adds +3 as in the snow trench. It lasts until two days above 0 °C, and a wolf cannot dig in through a snow-block door. A wall dug thinner than the 30 cm sticks has a 10% chance a night to drop in: 1 game minute to dig out, and the player is soaked.

**Windbreak.** In its lee, for 5 times its height, wind is ×0.3. A fire there lasts 50% longer in wind, and the camp site check's "Out of the wind" passes. Behind a 2-block windbreak, a garden bed is 1 °C warmer on frost nights, enough to save some seedlings on the evening of a −1 °C warning.

**Snow fence.** A fence half open to the wind drops a drift 5–10 blocks downwind, 1.5 m deep, after each storm over 10 m/s. Placed 10 blocks upwind of a trail or door, it keeps them clear. The drift is free material: a quinzhee, the ice house's snow cover, or a bank that cuts a cabin wall's U by 30%. Three storms bury it, and raising it takes 1 game hour.

**Ice road.** Snow insulates ice, so a cleared lane grows 2 cm a day below −10 °C instead of 1. Each flooding adds 2 cm overnight below −20 °C. The road opens to walking at 10 cm, the sled and wolf team at 20, and Pete's landing at 40 cm over 300 m, about 3 weeks sooner than raw river ice. Markers show at 40 m in a whiteout, against 20 for a blaze. Flooding within 50 blocks of a spring makes overflow, as in Danger in detail.

**Ice bridge.** Poles frozen into the ice spread load, so a sled crosses at 15 cm instead of 20. It is how a homestead crosses the main river at freeze-up a week early. It is lost at every breakup, and the poles drift to the next snag.

**Boardwalk.** Corduroy (Traversal) presses the moss flat, which thaws the permafrost under it: it sinks 10 cm a year and is wet by year 4. A boardwalk on sleepers keeps the moss whole and does not sink. It walks at ×1.2, carries sleds, and rots in 15 years against corduroy's 8. Beaver ponds that rise (Beaver Valley) can float it off.

**Stepping-stone ford.** A dry crossing at ×0.8 over a riffle under 0.6 m. Each spring flood moves 50% of the stones downstream, and the messenger lists the gaps.

**Trail markers.** On tundra and lakes there are no trees to blaze. Each marker in sight during a whiteout shows as a tick on the compass, so a marked route walks at ×0.8 instead of blind, as Whiteouts and getting lost describes.

## Food and storage

| Structure | Tier | Blocks | Materials | Build steps | Time | kcal |
| --- | --- | --- | --- | --- | --- | --- |
| Ice house | Iron | 4 × 4 × 3 | 20 logs, 12 poles, 8 m² sod roof, a plank door, 6 buckets sawdust or 24 moss sheets; 80 ice cakes | Cribbed walls on gravel; drain floor of poles over gravel; roof; in March cut and haul cakes; pack 30 cm of sawdust round and over | 24 h build; 11 h of ice | 10,800; 4,950 |
| Permafrost cellar | Iron | 2 × 3 × 2, below ground | Pick; 8 logs for the ceiling; 12 moss sheets; 4 m² sod; a plank hatch | Dig 12 blocks of frozen silt (2 deep) with the pick; log ceiling; 3 moss sheets per block over it; sod; hatch | 15 h | 6,750 |
| Under-floor cellar | Bushcraft | 2 × 2 × 1 under a floor | Shovel; 4 poles; 4 planks for a trapdoor | Before the floor: dig the pit; pole the sides; trapdoor in the floor | 3 h | 1,350 |
| Potato clamp | Start | 2 × 2 × 1 | 4 kg straw or grass, soil | Bed straw on drained ground; pile roots; 20 cm straw; 20 cm soil; a straw vent | 1 h | 450 |
| Fish-cutting shed | Bushcraft | 3 × 2 × 2 | 6 posts, 6 poles, 3 bark sheets, 2 planks for the table, a gut chute of 2 split logs or a barrel | Posts at the water's edge; bark roof; table; chute into fast water | 5 h | 2,250 |

**Ice house.** Lake ice cut in March keeps fresh food cold through summer. It closes the gap between the April thaw (Survival, not thrival) and the smokehouse.

1. **Cut.** An ice cake is 0.4 × 0.4 × 0.3 m and 45 kg. With the ice chisel it takes 6 game minutes, with an ice saw 3. Twenty cakes fill 1 block, and the house holds 4 blocks.
2. **Haul.** A cake carries like a green log, 2 ride the hand sled, and a wolf team takes 5. Building beside the lake is the real saving.
3. **Pack.** Each ice block needs 30 cm of insulation on every side: sawdust from the sawpit, or moss.
4. **Melt.** Insulated ice loses 2% a summer week, so 4 blocks still hold about 56% on October 1. With bare walls it loses 10% a week and is gone by July.
5. **Keep.** The 2 free blocks hold 27 slots at 1 °C, about 10 days of meat life on the Spoilage curve. It chills goat milk and makes fresh salmon last the run.

The meltwater drain keeps the ice dry; a blocked drain doubles melt. Food inside counts as frozen for the scent radius. Bears still open the sod roof in May on 10% of visits.

**Permafrost cellar.** One of the two exceptions to "Explore, don't dig", with the dugout (Every concept from the video › Dugouts and rock shelters, capped at 4 × 2 × 6 m): only on permafrost-flagged ground, only 2 blocks deep, level with or above the mouth floor, with no drops, and only with the pick, at 45 game minutes a block. The cellar floor holds −6 °C all year under a whole moss cover on a north slope, so meat stays frozen through summer. On a south slope or under thin moss it holds −2 °C and food thaws. It holds 45 slots. Each summer day the hatch stands open over 10 game minutes adds +0.2 °C, a heated building within 5 blocks adds +1 °C a month, and meltwater floods it once it passes 0 °C.

**Under-floor cellar.** A pit under a heated room. It holds 2–6 °C while the room stays above 10 °C, with 18 slots of roots that never freeze. If the room goes unheated for 2 days below −20 °C outside it falls to −1 °C, and potatoes freeze to mush. On permafrost it speeds the room's settlement ×1.5. Voles take 2% a week without a deadfall beside it.

**Potato clamp.** The year-1 store before a root cellar. It holds 0–4 °C until the ground freezes, then stays above freezing only under 40 cm of snow, which a snow fence can supply. It loses 15% to rot and voles by spring.

**Fish-cutting shed.** The fish camp's kitchen, which Outposts lists only as a cleaning spot. Cleaning at its table takes ×0.7 time, and a smudge under the roof keeps flies off cut fish. Guts sent down the chute into current over 1 m/s leave no scent point at the shed. They do draw grizzlies to the next riffle downstream within 3 days, a new bear spot the player made. A gut barrel instead holds 10 kg of offal for wolves or the garden's fertility.

**Drying rack and smoke rack, as blocks.** The Forge Labs dried and smoked meat runs on two racks, specified in Blueprint › Preserving in detail (drying) and The smoker (the smoke rack). This adds their ids and build lines; every number there stands.

| Rack | Id | Blocks | Holds | Rule it runs | Data |
| --- | --- | --- | --- | --- | --- |
| Drying rack | `yearofice:drying_rack` | 2 × 1 × 2 | 15 kg fresh strips | 1 cm strips dry in 2 dry days of sun or wind; each uncovered rain hour adds 6 h; a full day of rain restarts the batch; above 15 °C flies lay eggs unless a smoky fire burns within 3 m | `data/yearofice/food/drying_rack.json` |
| Smoke rack | `yearofice:smoke_rack` | 1 × 1 × 3, snapped over a fire ring | 10 kg of meat or fish | Hot smoke at 60–80 °C for 8–12 game hours; loses heat and smoke in wind over 8 m/s (Weather and storms) | `data/yearofice/food/smoke_rack.json` |

Both are block entities in `food.preserve` (`DryingRackBlockEntity`, `SmokeRackBlockEntity`), owned by Food and hunting, in M5. Strips go on as `dried_meat` or `dried_fish` (Items › Food values) and take state `dried` or `smoked` when done; the shelf lives are Decay and time's (dried 60 days, hot-smoked 30, salt doubles each). The smoke rack reads its heat from the fire ring's state, so a dying fire stalls the batch. Debug `/yoi food rack` prints the batch, hours left and rain hours. GameTests: `yearofice:food/drying_rack_two_dry_days` (M5): 15 kg of moose strips on a rack in dry July air reach `dried` in 48 ±1 game hours, and in 54 ±1 with one uncovered rain hour. `yearofice:food/hot_smoke_keeps_30_days` (M5): hot-smoked strips from the smoke rack, kept dry at 5 °C, stay above Stale for 30 ±1 game days, and 60 ±1 when salted.

**Thieves at the rack (video 2, A3; RECONCILE 170).** Meat, fish and hides left out to dry are raided by real animals, and every theft leaves sign. In the second video foxes stole drying hides until the rack was fenced. The rolls cover the `drying_rack`, the `smoke_rack`, hides on a `stretcher_board`, strips on a `smudge_fish_rack` and quarters on a meat pole. Each rolls once a game hour while the player is more than 30 m away.

| Thief | When | Chance per game hour | Takes | Where it goes |
| --- | --- | --- | --- | --- |
| Red fox | Night | 4% | One hide of 2 kg or less, or a meat strip | Dragged 20–60 m and cached under moss or snow as a `yearofice:theft_cache`; tracks lead to it; 70% recoverable within 2 game days, after that eaten |
| Gray jay or black-billed magpie | Day | 8% | One fish or meat strip of 0.2 kg or less; never hides | Eaten |
| Raven | Day | 3% | Meat of 0.5 kg or less | Eaten |
| Marten | Night, within 300 m of spruce | 2% | Meat or fish | Eaten |

**Protection.**

- A 2-block pole fence with a closed gate stops foxes. An open gate stops nothing.
- Martens climb, so only a roofed rack stops them.
- A roof, or a lit smoker within 4 blocks, halves the bird rolls.
- A tamed wolf within 20 m multiplies every roll by 0.25.
- With the player within 30 m every chance is 0. A named jay (Items › Theft by animals) takes nothing.

**What the player sees.** Fox or marten prints, scratch marks and droppings, or feathers lie under the rack. The messenger reports each theft once: "Fox tracks at the rack. One hide missing." A fox's drag mark runs to the cache, a moss or snow mound that RMB opens. A cache found after 2 game days is empty.

**Failure cases.** A hide dragged through rain or snow arrives wet (Items › Wet). A rack inside a fence with the gate left open is unguarded. A meat pole 4 m up stops foxes and martens only if they cannot climb to it; the marten still can, as Blueprint › Field care says of wolverines.

**Rules.** These rolls replace the Blueprint's flat raven loss on drying racks (0.5 kg per raven a day) wherever both would apply. Items › Theft by animals still covers food and gear on the ground and the camp-robber jay near a fire. Unloaded racks catch up on chunk load by replaying the missed hourly rolls from the same seeded stream, so a rack left a week loses what it would have lost live.

**The world option.** Rolls run only while `[world] rack_thieves` is on: on by default in every preset and changeable after world creation (Minecraft development › World creation options and presets). Off, racks lose nothing to thieves; rain, flies and spoilage still apply.

**Ties.** Preserving (the racks above), Decay and time, Items › Theft by animals, fences and gates, the tamed wolf (Open world in depth › Taming a wolf), and sign and tracks (Blueprint › Tracks and sign). Fences and drying sheds now earn their place.

**Build.** `ecosystem.raid.RackThieves` rolls each rack, with the odds in `sim.ecology.RaidOdds`; the thief's visit is resolved by roll, and sign decals and the cache are written as world edits. Brain activity `yearofice:rack_raid` and memory `yearofice:rack_target` drive a thief the player can see when within sight range. Block entity `yearofice:theft_cache` holds the stolen stack and its day. Tuning `[ecosystem] rack_theft_scale` (Gentle 0.5, Standard 1.0, Brutal 1.5); world option `[world] rack_thieves = true`. Debug `/yoi raid rack <thief>`. Owner Ecosystem. Milestone M6.

**GameTests.** `yearofice:ecology/rack_thief_rates` (M6): over 10,000 simulated night hours an open rack with a hide loses it to a fox in 4% ±0.4 of hours; behind a 2-block fence with the gate closed, in 0; with a tamed wolf within 20 m, in 1% ±0.2. `yearofice:ecology/rack_thieves_toggle` (M6): with the option off, 1,000 game days of unattended racks lose nothing to thieves, and turning it on mid-world takes effect at the next game hour.

## Hides, fire, earth and metal

| Structure | Tier | Blocks | Materials | Build steps | Time | kcal |
| --- | --- | --- | --- | --- | --- | --- |
| Hide yard | Bushcraft | 6 × 4 × 3 | 2 pole frames (4 poles, 8 m cordage each), a fleshing beam (1 smooth log on 2 legs), a soaking pit lined with 2 bark sheets | Lash frames; peel and set the beam; dig and line the pit | 5 h | 2,250 |
| Brain-tanning setup | Hide and bone | Adds 3 blocks to the hide yard | A clay or iron pot, a wringing post, a staking post with a blunt bone edge, a smoke tube: 3 poles and a sewn hide skirt over a 1-block smudge pit | Set both posts; dig the smudge pit; sew the tube | 4 h | 1,800 |
| Charcoal pit | Bushcraft | 2 × 2 × 1, dug | 10 armloads, 2 m² sod, a shovel | Dig the pit; stack wood on end; light; cover with sod and soil | 3 h + 30 h sealed | 1,350 |
| Clay kiln | Bushcraft | 2 × 2 × 3 | 60 mud bricks, 4 buckets clay, 20 stones | Stone base and firebox; brick chamber; clay dome and chimney; dry 3 days | 10 h + 3 d | 4,500 |
| Smithy | Homestead | 5 × 4 × 3 | Woodshed frame, plank walls on 3 sides, a stone floor of 40 stones, a quench trough (1 split log) | Frame and roof over the existing forge; trough; tool rack | 14 h | 6,300 |
| Sawpit | Iron | 6 × 2 × 3 | A 1-block pit 6 long, 2 trestles of 4 posts, a spring pole of 6 m, a whipsaw blade | Dig the pit; set trestles; anchor the spring pole; hang the saw | 8 h | 3,600 |
| Shaving horse | Iron | 1 × 2 × 1 | 1 split log, 4 legs, 1 pivoting clamp arm | Bore legs; peg the clamp | 3 h | 750 (medium) |

**New tools for these.** Whipsaw blade: 1.2 kg iron, furnace, 6 h, 2,700 kcal. Froe: 0.5 kg iron, furnace, 1 h, 450. Ice saw: 0.6 kg iron and a pole, furnace, 2 h, 900. Grindstone: a sandstone wheel shaped in 4 h, a 1.5 kg iron axle, a crank frame.

**Hide yard.** The pole frame that Hide and bone recipes ask for, as a placed station. Two hides work at once, and scraping on the beam is ×0.7 time. A hide drying on a frame is a small scent point of 50 m. Rain on a drying hide adds a day, and a pup in the yard chews it under the existing 10% rule.

**Brain-tanning setup.** Smoke-tanned leather (6 h + 6 d) made the old way, faster and less forgiving.

1. Flesh and grain the hide on the beam (2 h, medium work).
2. Soak it in the animal's own brain mashed in warm water, 1 game hour.
3. Wring it on the post, then work it over the staking post while it dries (3 h). A break of more than 30 game minutes leaves 30% of the hide stiff rawhide.
4. Smoke each side for 1 h in the tube over punky rotten wood.

The setup cuts the 6 days to 3. Its leather dries soft after a soaking, which mukluks and the wolf harness need.

**Charcoal pit.** The lazy clamp. Sealed underground it needs no 4-hour checks and never flares, but yields 12 kg against the clamp's 15. On 10% of burns half the wood stays brown, and those brands burn again next time. It cannot be dug in frozen ground, so it runs May to September.

**Clay kiln.** Firing in coals cracks 25% of pieces; the kiln cracks 5%. A firing holds 9 slots of dried greenware and takes 3 armloads and 6 h at Hot. Greenware dried under 2 days bursts 50% of the time, with a pop. Pottery is a new skill (N 20, one rep per piece fired), with these new recipes:

| Recipe | Inputs | Station | Time | kcal | Output |
| --- | --- | --- | --- | --- | --- |
| Clay pot | 0.5 bucket clay with sand | Hands, kiln | 40 min + firing | 67 | 2 L pot; cooks like the crate pot, breaks if dropped (5%) |
| Storage crock | 1 bucket clay, a plank lid | Hands, kiln | 1 h + firing | 100 | 9 slots that mice cannot enter |
| Sauerkraut crock | 1 bucket clay, a weight stone | Hands, kiln | 1 h + firing | 100 | Ferments 8 cabbages in 3 weeks; keeps vitamin C all winter |
| Fired brick | 1 mud brick | Kiln | + firing, 12 a load | — | HP 1,200 against mud brick's 800; rain cannot spoil it |
| Clay flue pipe | 0.5 bucket clay | Hands, kiln | 30 min + firing | 50 | 1 m of smokehouse trench flue that never burns |

**Smithy.** A roof and walls around the forge, so rain never pauses it, the station rule in Stations. With the quench trough beside it, a steel edge (Tier 5) takes 1.5 h instead of 2. The tool rack hangs 9 tools, which count as in the pack for any recipe within 10 blocks and are out of porcupine reach.

**Sawpit.** A solo whipsaw: the 6 m spring pole lifts the saw after each pull stroke, the trick that replaces the second sawyer. A log gives 12 sawn planks in 3 h and 1,350 kcal, plus 1 bucket of sawdust. A sawn plank counts as 1.5 split planks in any recipe, so the pit makes 18 plank-equivalents where splitting makes 6. Sawn plank walls have U 1.2 against 1.5. Sawdust is the ice house's insulation.

**Shaving horse and froe.** The horse is a woodcraft station: handles, paddles, snowshoe strips and toboggan boards take ×0.7 time at it. With the froe, one straight, knot-free spruce round (1 tree in 3) rives 40 shingles in 2 h.

| Roof piece | Materials per block | Time | kcal | U | HP | Span | Sheds snow | Life |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Shingle roof | 25 shingles, 2 lath poles | 30 min | 225 | 1.0 | 300 | 2 blocks from a purlin, light | Above 20 cm at 1:1 | 25 years |

Shingles weigh a tenth of sod and need no heavy-span purlins. Rain off them is clean for the catchment. They burn at 1.2 flammability, the trade against sod's warmth and fire immunity.

## Water power, crossings and craft

| Structure | Tier | Blocks | Materials | Build steps | Time | kcal |
| --- | --- | --- | --- | --- | --- | --- |
| Mill race and wheel | Homestead | Race up to 30 × 1; wheel 1 × 3 × 3 | 2 log cribs of 40 stones for the head dam, 30 split planks for the flume, an axle log, 24 planks for buckets, 1 kg iron for bearings | Crib dam above a 1 m drop; plank flume to the wheel pit; axle on stone bearings; hang the wheel | 44 h | 19,800 |
| Grist mill | Homestead | 3 × 3 × 3 on the axle | 2 millstones of 150 kg from a rock face, a plank millhouse, a hopper | Quarry and shape stones (8 h each); haul by sled; set the bed stone; gear the runner to the axle | 32 h | 14,400 |
| Trip hammer | Homestead | 1 × 4 × 2 on the axle | A 4 m birch helve, a 6 kg iron head, an anvil stone, cams pegged to the axle | Set the anvil stone; pivot the helve; peg cams | 12 h | 5,400 |
| Dock | Bushcraft or Iron | 1 × 4 deck, a 2 × 2 crib | A crib of 6 logs filled with 30 stones; 8 planks; pegs or 20 nails | Sink the crib in water up to 1.5 m; stringers to the bank; plank the deck | 8 h | 3,600 |
| Boat landing and canoe rack | Homestead | Skidway 1 × 4; rack 2 × 6 × 2 | 6 log rollers, 4 posts, 4 poles, 3 bark sheets | Bed the rollers up the bank; posts and crossbars; bark roof | 7 h | 3,150 |
| Steam box and bending jigs | Hide and bone | 3 × 1 × 1 | 4 planks, a pot or kettle on a fire; each jig: 1 split log with pegs | Seal the box with pitch over the pot; carve each jig | 4 h; 1 h a jig | 1,000; 250 a jig (medium) |

**Mill race and wheel.** A homestead project on a creek with a 1 m drop over 10 blocks and 0.3 m³ a second of flow, readable from the riffle. One axle drives the grist mill and the trip hammer, not both at once. It runs from breakup to freeze-up, roughly mid-May to mid-October, and stops at the first skim ice.

- **Silt.** The race silts 10% a month; clearing takes 2 game hours, a queued upkeep job.
- **Beavers.** A colony within 1 km plugs the race on 10% of open-water weeks. Water rises behind the plug and floods any boardwalk upstream.
- **Flood.** In a spring-flood year the wheel is torn away unless lifted out first (2 h). The messenger warns at breakup: "Lift the wheel. Flood risk 25% this spring."

**Grist mill.** It grinds 8 kg of barley an hour without the player. A hand quern (2 flat stones, shaped in 2 h) grinds 1 kg an hour at light work, so 1 kg of flour costs 100 kcal of the 3,500 it holds. Flour makes flatbread and sourdough, Lorraine's recipe. The mill also grinds dried fish into fish meal for hens, which lets them lay through one more cold month.

**Trip hammer.** Consolidating a bloom takes 1 h and 300 kcal instead of 2 h and 900. Drawing out bar for a box stove takes ×0.6 time. Its thud carries 1 km, and the encounter director keeps moose and caribou 300 blocks away while it runs, so forging days are not hunting days.

**Dock.** Loading a canoe or raft from it is dry and draws from storage within 10 blocks. The dock end reaches 1.5 m water, where pike hold in summer. At breakup, shore ice shoves a fixed deck off 40% of years unless its planks are pulled (2 h). The crib stays.

**Boat landing and canoe rack.** Rolling a canoe up the 4-block skidway takes 1 game minute instead of 10. A birch-bark canoe stored on the rack loses no hull points over winter; left on the ground it loses 20 to frost and snow load. In the sun and rain of summer the rack doubles the canoe's regum interval from 20 days of use to 40.

**Steam box and bending jigs.** Wood steamed 1 h per 2.5 cm of thickness bends on a jig. Jigs exist for snowshoes, sled runners, toboggan boards and canoe ribs. With the jig, snowshoes take 3.5 h instead of 6 and bending breaks 5% of strips instead of 20%.

### Vehicles

| Vehicle | Tier | Blocks | Materials | Build steps | Time | kcal | Speed | Carries |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Toboggan | Hide and bone | 1 × 4 | 2 birch boards 4 m long, 4 crossbars, 8 m rawhide | Steam the front curl on the jig; lash crossbars; lash a load line | 6 h | 1,500 (medium) | 0.6 in deep powder and 0.6 packed; the sled is 0.45 and 0.7 | 80 kg, 27 slots |
| Skin-on-frame kayak | Hide and bone | 1 × 5 | 30 split spruce strips, 20 m sinew thread, 2 caribou hides or 1 moose hide, 0.5 kg lard | Lash the frame; stretch and sew the wet skin; dry 2 days; oil it | 30 h + 2 d | 7,500 (medium) | 1.5 on flat water | 60 kg, 18 slots |
| Moose-skin boat | Hide and bone | 3 × 8 | 3 green moose hides, 40 spruce ribs and stringers, 30 m sinew, 2 kg pitch or lard | Build the frame on a gravel bar; sew hides wet over it; seal seams | 24 h | 10,800 | Drifts at the current; poled 0.2 | 400 kg |

**Toboggan.** The flat-bottomed Athabascan sled, which floats on powder where runners sink. It is the best freighter on a snowshoe trail before the trail packs. One wolf of 12 months or more can pull it alone.

**Kayak.** A water craft a year before the canoe. It weighs 15 kg, so portages walk at 0.9. After 8 h in the water the skin softens to ×0.9 speed until dried 12 h. A skin left in the sun cracks, losing 10 hull points a day, and a pup chews it under the hide rule. It is silent, so ducks flush at half range.

**Moose-skin boat.** Upper Kuskokwim and Tanana people built these each spring to float a winter's goods downriver. It goes downstream only. At the end the hides are taken off and dried as 3 big rawhides, so nothing is wasted. It is the one-trip way to bring a caribou camp's meat or an iron camp's blooms home in May.

## Animals, signals and pride

| Structure | Tier | Blocks | Materials | Build steps | Time | kcal |
| --- | --- | --- | --- | --- | --- | --- |
| Wolf yard | Homestead | 3 × 3 per wolf | Per wolf: a doghouse (bough or 6 planks, 2 kg straw), a post, a 3-block tether of 0.5 kg iron chain or 4 m rawhide on a swivel | Set posts 3 blocks apart; tether; doghouse door away from the wind | 2 h per wolf | 900 |
| Harness shed and dog pot | Homestead | 3 × 2 × 2 | Woodshed frame, 6 pegs, the iron kettle on a fire ring | Frame and roof; pegs; fire ring and stand | 5 h | 2,250 |
| Haystack and hay loft | Homestead | Stack 3 × 3 × 3; loft 1 block over a shed | Stack: 9 poles and a pole-fence ring. Loft: 6 poles over the shed's ceiling | Pole platform; stack in a cone; fence ring. Loft: floor the ceiling | 4 h stack; 3 h loft | 1,800; 1,350 |
| Barn heat kit | Homestead | Inside a shed or hen house | 20 cm of barley straw per block, a fat lamp, a clay waterer on a lamp | Lay deep litter; hang the lamp; set the waterer | 1 h | 450 |
| Greenhouse | Homestead | 5 × 3 × 3 | 30 sawn or 45 split planks, 6 window panes (or the plexiglass window, or 6 rawhides), a box stove or clay stove, 3 raised beds | Plank frame on stones; south wall and roof of panes; stove and pipe; beds | 30 h | 13,500 |
| Signal beacon | Start | 3 × 3 × 2 | 3 armloads, 20 green boughs, 2 bark sheets as a cover | Stack a teepee on a bar or ridge; boughs ready on top; bark over it | 1 h | 450 |
| Spotting tower | Bushcraft or Iron | 3 × 3 × 9 | 4 poles of 8 m, 16 brace poles, 30 m cordage or 40 nails, 4 planks, 2 bark sheets | Set 4 legs 1 block deep; brace in X every 2 blocks; ladder; platform and roof | 24 h | 10,800 |
| Box blind | Iron | 2 × 2 × 2 on skids | 16 planks, 2 skid logs, 2 shutters, a fat lamp | Frame on skids; walls; shooting ports with shutters | 8 h | 3,600 |
| Workshop | Iron | A warm room of 12 or more floor blocks | At least 4 stations inside: workbench, shaving horse, grindstone, potter's bench, sewing table with a lamp, tool rack | Build the room; place the stations | By pieces | — |
| Trophy wall | Bushcraft | 4 × 1 × 2 on a wall | 5 or more trophy mounts, antler racks or framed pelts | Hang them within one 4 × 2 wall area | 1 h each | 450 each |
| Radio shack | Iron | 2 × 3 × 2, or a cabin corner | A desk of 2 planks, a moss-lined battery box, a south-wall panel rack of 4 poles; the station's radio set | Build or wall off the room; desk; battery box; panel rack at 60° tilt | 6 h | 2,700 |
| Wire antenna mast | Iron | Two spruces over 12 m, 20 blocks apart | The 20 m of control cable from the station mast, 2 bark insulators, 10 m cordage, a 1 m iron ground rod (0.5 kg) | Climb with the ladder; rig the cable between the trees; drive the ground rod | 4 h | 1,800 |

**Wolf yard.** The "yard" that Team rules require for a new member's 14 days. Wolves 3 blocks apart cannot fight, which removes the 20% harness-fight roll during those days. A doghouse with straw is the roofed spot that saves 5 Condition a night below −30 °C. A rawhide tether is chewed through on 5% of nights; chain is not. The yard is a 200 m scent point, and in a hard winter a wild pack tests it on 5% of weeks. The tame wolves howl at anything inside 150 m: "The wolves are howling north. Something at 120 m."

**Harness shed and dog pot.** A harness dried on pegs for 6 h wears at the normal rate; stored wet it wears ×2. The dog pot is the musher's cooker: fish, fat and cooked barley or fish meal stewed together. Up to 25% of a wolf's daily ration can then be grain, so the garden feeds the team a little and the meat pressure in Survival, not thrival eases only by that share.

**Haystack and hay loft.** Hay dries 3 days in the field before stacking. A stack holds 600 kg, about a goat and a half for a winter. Hay rained on before stacking loses 20%, and hay stacked wet molds 30% and heats: 2% of wet stacks catch fire by themselves within a month. Unfenced, a moose eats 10 kg a night from November. A loft full of hay over a goat shed halves the roof's U, so the barn stays warmer on its own.

**Barn heat kit.** Hens lay by the light factor (0 at 10 h of light, rising to 1 at 14 h; the lamp counts) times the temperature factor (0 below 0 °C, 0.5 at 0–5 °C, 1 above 5 °C), as in Building pieces. Deep litter composts and gives 20 W a block after 3 weeks if turned monthly (20 min). A goat gives 80 W, a hen 10, a fat lamp 30 and its light. A 3 × 2 block shed for two goats and four hens with litter, lamp and a hay loft makes about 350 W. Its L of about 15 W/°C holds +23 °C over outside, which is −7 °C on a −30 °C day. So winter eggs need a stove at about 1 armload a day at Low, or a shed built against the cabin's wall to share heat. The clay waterer on a lamp keeps water open.

**Greenhouse.** The stovehouse that the main tab names, specified. It adds the existing 3 weeks at each end of the season and lets cabbage and turnip seedlings start in April. Glass gives 100% growth, plexiglass 90%, rawhide 50%. A manure hotbed (0.5 m of fresh goat manure under a bed) warms its soil +8 °C for 3 weeks, so that bed is sown 4 weeks early. Panes break above 30 cm of snow, so its roof is shoveled first. An April night at −10 °C costs about 1 armload.

**Signal beacon.** A fire stacked and kept dry under bark, lit in 1 game minute. Green boughs on top give smoke seen 20 km off on a calm day; at night the fire shows 15 km. Lit on a drop day, it lets Pete drop within 50 blocks of it. Without one, 20% of drops land in trees and each item takes 30% damage. Three beacons in a triangle are the distress signal, J.O.'s in 1979. On a ridge in a dry July with wind over 6 m/s, a lit beacon starts a wildfire 3% of the time.

**Spotting tower.** From its platform the messenger map reveals 2 km around and the wildlife count reads species to 1 km, so the radio's count log fills faster. Wildfire smoke is seen 2 days before the forecast names it, and the caribou crossing on the Barrens shows if the tower is in sight of it. Above 10 m/s of wind it sways and aiming is off. The tree stand's icy-rung rule applies to its ladder, and lightning hits it in 0.5% of thunderstorms.

**Box blind.** A heated blind for long winter waits at a bait or a winter-killed moose. With the lamp it is a room: player 100 W plus lamp 30 W holds about +10 °C over outside. With the shutters closed it is unseen and quiet. An open port counts as the ground blind's drawing rule, and lamp smoke adds ×1.2 scent. Dragged on skids by a sled or wolves, it is set up in 30 minutes. After 7 days in place it gives full effect to every species, where a fresh ground blind only halves sight for moose, caribou and sheep.

**Workshop.** A warm room with 4 or more stations. The job queue grows from 8 to 12, and darkness and weather never pause jobs inside. A sewing table with 2 lamps removes the dark penalty (m\_light) at night. The grindstone restores an edge to 100 in 1 game minute and removes the 20 edge of a cold chip. Winter work moves indoors here, as shelter tier 5 promised.

**Trophy wall.** Five trophies hung in one 4 × 2 area raise the decorated-room Spirits cap from +3 to +4. It is labelled with the biggest fish's weight and the year card date. Two decor pieces are new:

- **Pelt rug.** A cured big hide on the floor lowers that block's floor U from 0.35 to 0.25, which counts as a decoration too.
- **Notch board.** A plank where every lynx, moose and wolf the player takes is notched automatically, like Arvid's door frame. Each new notch is Spirits +1. In year 5 the notches rise and fall with the hares, as his did.

**Radio shack and wire antenna.** The radio set can leave Station Nine for home. It needs a battery box (below −20 °C a bare battery holds half its charge) and a mast at home. The wire antenna reuses the station mast's 20 m of cable, so the station radio goes silent: a real choice. Height sets reach.

| Antenna height | Daily sked with Dana | Lorraine's evening net (150 km) |
| --- | --- | --- |
| Under 6 m | 50% of skeds fail | Never |
| 6–9 m | Always | 50% of nights |
| Over 9 m | Always | Always |

The panel rack tilted at 60° on a south wall charges ×1.5 in March and October, when the sun is low. Without the ground rod, each summer thunderstorm has a 2% chance to burn out the set: 4 h and 1 tin to repair.

## Example homesteads

These are the balance simulator's reference builds on Standard, one good player each. Blocks are blocks placed, frames excluded. Hours and kcal are building only, from the tables above and in Crafting and building in depth.

### Year 1: the first winter (May 1 to April 30)

| # | When | Build | Blocks | Game hours | kcal |
| --- | --- | --- | --- | --- | --- |
| 1 | May 1–3 | Repair the outpost cabin; fire ring; woodpile; chopping block | 20 | 16 | 7,200 |
| 2 | May | Outhouse; rain catchment on the outpost roof | 14 | 7 | 3,000 |
| 3 | June | Smoke rack; hide yard; 3 drying racks | 28 | 10 | 4,500 |
| 4 | June–July | Bloomery and charcoal pit at the iron (Iron camp) | 12 | 19 | 8,550 |
| 5 | July–August | First cabin, 3.6 × 4.2 m, sod roof, fireplace or barrel stove | 96 | 160 | 70,000 |
| 6 | August | Woodshed; raised cache | 44 | 24 | 10,800 |
| 7 | September | Potato clamp; under-floor cellar dug before the floor | 7 | 4 | 1,800 |
| 8 | September | Clay kiln, for storage crocks against mice | 12 | 10 | 4,500 |
| 9 | October | Smoke banya | 46 | 36 | 16,200 |
| 10 | November | Snow fence on the trail; water-hole box | 21 | 4 | 1,800 |
| 11 | February | Trail markers to the trapline; one quinzhee practised | 20 | 4 | 1,800 |
|  |  | **Total** | **320** | **294** | **130,150** |

The cabin alone is over half the year's building and calories. The banya goes up only after the cabin, the first build that is wanted rather than needed.

### Year 3: the working homestead

Built on year 1, adding these in year 2 and year 3.

| # | When | Build | Blocks | Game hours | kcal |
| --- | --- | --- | --- | --- | --- |
| 1 | Year 2, May | Root cellar into the hillside | 30 | 32 | 14,400 |
| 2 | Year 2, June | Smokehouse | 24 | 24 | 10,800 |
| 3 | Year 2, July | Sawpit; shaving horse and froe | 15 | 11 | 4,350 |
| 4 | Year 2, August | Brain-tanning setup; banya chimney upgrade | 6 | 10 | 4,500 |
| 5 | Year 2, September | Dock; stepping-stone ford of 8 stones | 22 | 9 | 4,200 |
| 6 | Year 2, March | Ice house, built in fall, filled in March | 52 | 35 | 15,750 |
| 7 | Year 3, May | Wolf yard for 3 wolves; harness shed | 33 | 11 | 4,950 |
| 8 | Year 3, June | Goat shed and hen house as one barn; hay loft; barn heat kit | 40 | 28 | 12,600 |
| 9 | Year 3, July | Haystack; 40 m pole fence round the garden | 47 | 14 | 6,300 |
| 10 | Year 3, August | Gravel well by the river | 9 | 10 | 4,500 |
| 11 | Year 3, September | Workshop: second room on the cabin, with 4 stations | 60 | 45 | 20,250 |
| 12 | Year 3, October | Radio shack in the workshop; wire antenna | 10 | 10 | 4,500 |
|  |  | **Total added** | **348** | **239** | **107,100** |
|  |  | **Homestead total** | **668** | **533** | **237,250** |

This is the upkeep floor's year-3 homestead, plus the ice house, well and workshop. The workshop and well buy back about 2 game hours of winter chores a day.

### Year 5: the homestead on the creek

| # | When | Build | Blocks | Game hours | kcal |
| --- | --- | --- | --- | --- | --- |
| 1 | Year 4, May | Greenhouse with a box stove | 45 | 30 | 13,500 |
| 2 | Year 4, June | Boat landing and canoe rack; canoe bed | 20 | 10 | 4,500 |
| 3 | Year 4, July | Rive 600 shingles; re-roof the cabin (24 blocks) | 24 | 42 | 12,900 |
| 4 | Year 4, August | Spotting tower on the ridge | 60 | 24 | 10,800 |
| 5 | Year 4, December | Ice road to the big lake, flooded weekly | 0 | 20 | 9,000 |
| 6 | Year 5, May | Mill race and wheel | 70 | 44 | 19,800 |
| 7 | Year 5, June | Grist mill | 27 | 32 | 14,400 |
| 8 | Year 5, July | Smithy round the forge; trip hammer | 70 | 26 | 11,700 |
| 9 | Year 5, August | Permafrost cellar on the north slope | 12 | 15 | 6,750 |
| 10 | Year 5, August | Boardwalk 60 blocks to the bog fishing lake | 60 | 18 | 8,100 |
| 11 | Year 5, September | Box blind; trophy wall; notch board | 20 | 13 | 5,850 |
|  |  | **Total added** | **408** | **274** | **117,300** |
|  |  | **Homestead total** | **1,076** | **807** | **354,550** |

By year 5 the player builds fewer hours a year than in year 1, but upkeep rises: about 70 game hours a year of re-chinking, re-sodding, race clearing and dock planks. Water power and the cellars move pressure from food to hauling ice, hay and millstones, as Survival, not thrival asks.

## Data and build

**Registry ids and code.** Blocks and block entities live in `com.yearofice.build`; vehicles in `com.yearofice.travel`. Structure data loads by reload listener from `data/yearofice/multiblock/<id>.json` (Minecraft development › Two ways data loads), per-block numbers sit in the data map `data_maps/block/piece.json`, and templates in `data/yearofice/structure/blueprint/<id>.nbt`. Debug: `/yoi struct check` prints the looked-at controller's pattern, HP, rot word, snow load and tilt, and `/yoi struct age <days>` runs the day pass on it. Tuning (snow caps, ember chances, rot and settlement rates) lives in the `[structures]` section of `yearofice-common.toml`, with the presets' values.

| Structure | Controller or entity id | Java class | Milestone |
| --- | --- | --- | --- |
| Support, snow load, rot, permafrost, fire, animal damage, upkeep | — | `SupportGraph`, `SnowLoad`, `RotModel`, `PermafrostSettlement`, `EmberSpread`, `AnimalDamage`, `UpkeepLedger` | M8 |
| Banya | `yearofice:banya_heater` | `BanyaBlockEntity` | M8 |
| Trail shelter, outhouse, rain catchment, water-hole box | `yearofice:trail_shelter`, `yearofice:outhouse`, `yearofice:rain_gutter`, `yearofice:water_hole_box` | `SimpleStructureBlockEntity` | M8 |
| Gravel well | `yearofice:well` | `WellBlockEntity` | M8 |
| Quinzhee, windbreak, snow fence, ice road, ice bridge, trail markers | `yearofice:quinzhee`, `yearofice:snow_fence`, `yearofice:ice_lane` | `SnowStructures`, `IceLaneSavedData` | M7 |
| Boardwalk, ford, dock | `yearofice:boardwalk`, `yearofice:ford_stone`, `yearofice:dock_crib` | `CrossingBlock` | M8 |
| Ice house, permafrost cellar, under-floor cellar, potato clamp | `yearofice:ice_house`, `yearofice:permafrost_cellar`, `yearofice:floor_cellar`, `yearofice:clamp` | `ColdStoreBlockEntity` | M8 |
| Fish-cutting shed | `yearofice:cutting_table` | `CuttingTableBlockEntity` | M5 |
| Hide yard, brain-tanning setup | `yearofice:fleshing_beam` | `HideYardBlockEntity` | M6 |
| Charcoal pit, clay kiln | `yearofice:charcoal_pit`, `yearofice:kiln_firebox` | `KilnBlockEntity` | M8 |
| Sawpit, shaving horse, steam box | `yearofice:sawpit`, `yearofice:shaving_horse`, `yearofice:steam_box` | `WoodStationBlockEntity` | M8 |
| Mill race, wheel, grist mill, trip hammer, smithy | `yearofice:water_wheel`, `yearofice:millstone`, `yearofice:trip_hammer`, `yearofice:smithy` | `WaterWheelBlockEntity`, `PoweredStationBlockEntity` | M9 |
| Toboggan, kayak, moose-skin boat | `yearofice:toboggan`, `yearofice:kayak`, `yearofice:moose_skin_boat` | `TobogganEntity`, `KayakEntity`, `SkinBoatEntity` | M7, M8, M9 |
| Wolf yard, harness shed, haystack, barn heat kit, greenhouse | `yearofice:wolf_post`, `yearofice:dog_pot`, `yearofice:haystack`, `yearofice:greenhouse` | `YardBlockEntity`, `BarnHeat`, `GreenhouseBlockEntity` | M9 |
| Signal beacon, spotting tower, box blind | `yearofice:signal_beacon`, `yearofice:tower_platform`, `yearofice:box_blind` | `BeaconBlockEntity`, `TowerBlockEntity` | M6, M8, M9 |
| Workshop, trophy wall, notch board, radio shack, wire antenna | `yearofice:grindstone`, `yearofice:notch_board`, `yearofice:radio_desk`, `yearofice:antenna_anchor` | `WorkshopRoom`, `TrophyWall`, `RadioShackBlockEntity` | M9 |

**Multiblock JSON.** One file per structure. The pattern uses vanilla-style layer strings and a key, like data-pack patterns.

```json
{
  "id": "yearofice:banya",
  "tier": "bushcraft",
  "controller": "yearofice:banya_heater",
  "size": [5, 4, 3],
  "pattern": [
    ["LLLLL", "LSSDL", "LHBDL", "LLLLL"],
    ["LLLLL", "L..DL", "L.BDL", "LLDLL"],
    ["RRRRR", "RRRRR", "RRRRR", "RRRRR"]
  ],
  "key": {
    "L": "#yearofice:log_walls",
    "H": "yearofice:banya_heater",
    "B": "yearofice:bench",
    "S": "#yearofice:stone_floor",
    "D": "#yearofice:dressing_floor",
    "R": "#yearofice:roof_any"
  },
  "materials": { "wall_log": 14, "pole": 10, "bark_sheet": 6, "sod_m2": 6, "river_stone": 60, "clay_bucket": 1, "plank": 4, "hide_flap": 1 },
  "steps": ["foundation", "walls", "heater", "bench", "roof"],
  "work_h": 36,
  "effort": "high",
  "skill": "log_building",
  "heat": { "armloads": 2, "heat_up_h": 2.0, "target_c": 70, "smoke": true, "vent_min": 30 },
  "effects": { "spirits": 6, "condition": 3, "scent_mult": 0.4, "scent_h": 24 },
  "upkeep": [{ "kind": "stone_crack", "every": "10_firings", "count": 2 }],
  "fire": { "ember_radius_blocks": 1, "chance_per_firing": 0.02 },
  "blueprint": "yearofice:blueprint/banya",
  "milestone": "M8"
}
```

**Aging data.** Per-material numbers are data maps: the rot multiplier in `data_maps/block/decay.json`, flammability, span class and HP in `data_maps/block/piece.json`, and U in `data_maps/block/heat.json`. The animal table is `data/yearofice/decay/animal_damage.json`, as rows of species, target tag, HP, months and stoppers. Ticks: ageing is the Decay layer's day pass (`randomTickSpeed` 0), and unloaded structures catch up in closed form when their chunk loads, as Engine rules require.

**GameTests.** Each runs in a NeoForge `@GameTest` structure template under `data/yearofice/structure/gametest/`, driving the daily aging tick directly so days pass in a few server ticks.

- *Snow load:* a 4 × 4 unbraced pole roof with 5 layers (62.5 cm) loses 5 HP a block a day; with a brace it carries 9 layers for 30 days with no HP loss; 2 layers (25 cm) after a thaw day reads load 0.8–0.9.
- *Banya:* at −30 °C outside, 2 armloads bring the hot room of the smoke banya to at least 70 °C within 2.0 game hours, and entering before the door has been open 30 game minutes applies the smoky-room rule.
- *Permafrost:* a cabin heated to a mean 20 °C, floor on permafrost ground, settles 3–5 cm in one year; the same cabin on a 1-block crawl space settles 0, and on 3 moss sheets per block 1.5–2.5 cm.
- *Ice house:* 4 blocks of ice packed in 30 cm of sawdust keep at least 50% on October 1 in 9 of 10 simulated summers; with bare walls, under 5% remains on August 1.
- *Fire:* over 1,000 simulated game hours, a Steady campfire 2 blocks from a dry spruce wall lights it 5–15 times, and a wall under snow or a sod roof is never lit.
- *Mill:* at 0.3 m³ a second the grist mill grinds 7–9 kg an hour, stops within one game day of the first skim ice, and a beaver colony within 1 km plugs the race on 5–15% of open-water weeks over 10 years.
