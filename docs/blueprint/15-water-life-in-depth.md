# Water life in depth

This tab turns the valley's fish and water animals into Minecraft mobs that hold, feed, spook, spawn and get caught on screen. It adds a mob sheet for every water animal, 21 new species and life-stage systems, and ways to watch, count, keep and stock fish. Numbers in Fishing in depth, Waters and fish and Water, calls and encounters stay as written; plants belong to Plants and fungi.

## What is already covered

This tab adds the layer between the existing systems: the fish as a creature on screen. It references these and does not repeat them.

- **Species and fishing rules.** Fishing in depth (species, bite roll, tackle, the fight, passive gear, stocks, runs, the echo) and Waters and fish (water types, 14 species, runs, carcasses, winterkill, legends, the food web).
- **Animals.** Wood frog, dragonfly, leech, mink, loon and kingfisher in Bestiary; the draw model in Water, calls and encounters; fish wariness in Ideas from viewers › Fish that see you.
- **Engine.** Fish entities in the Active tier, the bobber and the `yearofice:current` attachment: Minecraft engine › Fishing and Rivers, current and water.

**Two names, settled.** The species data folder is `data/yearofice/fish/<id>.json` (RECONCILE 46). A caught fish is an item with the species id, the same id as its entity, carrying the `yearofice:fish_data` component; the engine's `yearofice:fish_<species>` form is withdrawn (RECONCILE 47; Minecraft development › Conflicting ids, row 5).

## Fish as living mobs

A fish entity is a picture of the stock, not the stock itself. It holds in lies, feeds in lanes, rises, schools, spooks, spawns and slows under the ice, and every one of those shows on screen as a sign the player can read.

### The stock is the truth, the fish is its picture

**Projection.** A spot's stock N (Fish stocks) is the authority. `FishProjector` spawns entities when a water body comes within 48 blocks of a player and removes them beyond 64 blocks. They are never saved, and a removed fish leaves N untouched. Only a catch, a kill or a stranding changes N.

**Who gets drawn.** Each spot draws big fish as individuals and small fish as one school per species.

```latex
n_{ind} = \min\left(4,\ \left\lceil \frac{N}{8} \right\rceil\right) \qquad m_{school} = \min(40,\ N)
```

The caps below then trim the totals. Individuals take weights from the size draw in Fishing in depth, so the fish the player sees are the fish the rod would catch. A school's members share one weight band.

**Which fish is which.** Individuals keep a stable id per spot slot from `hash(seed, spot, species, slot)`. So the big grayling at the head of the falls pool is the same fish every visit until it is caught. Its weight is redrawn only when N changes.

### The fish mind

Fish use a light state machine, not the vanilla `Brain`, to keep the cost low. `FishMind` ticks individuals at 10 Hz and schools at 5 Hz; the client interpolates between.

| Activity | Entered when | What it does on screen | Leaves when |
| --- | --- | --- | --- |
| Hold | Default in moving water | Faces upstream in its lie; fins scull; sways 0.1–0.3 m | Food in its lane, a threat, its schedule |
| Lane take | A drift item enters its feeding lane | Darts up to 1 m, takes, slides back in 1–2 s | Done |
| Rise | Hatch intensity H over 0.2 and the species feeds at the surface | Tilts up, takes at the surface, leaves a ring, sinks back | H under 0.2, or wariness 30 |
| Cruise | Still water | Loops 10–40 m along a weed edge or drop-off at 0.2–0.4 m/s | A threat, its schedule |
| Ambush | Pike all day; burbot and lake trout at night | Motionless in cover; strikes 1.5–2 m in 0.15 s | Strike made |
| School | Schooling species | Boids around the school entity | Never; the school changes state instead |
| Bait check | Bait or lure within 3 m, wariness under 30 | Turns, follows 0.5–2 m, noses it | The bite roll: take, or turn away |
| Take | The bite roll succeeds for this fish | Swims to the bait in 0.5–1.5 s, mouth opens, turns down | Strike: Hooked. A miss: Flee 40% of the time |
| Hooked | The strike lands | Driven by the fight step function | Landed or lost |
| Flee | Wariness 50, a predator strike, a missed spear | Bursts to the deepest cell within 20 m with a wake | The settling rule in Fish that see you |
| Spawn | Inside its spawn window at its site | The species' sequence (Spawning the player can watch) | The window ends |
| Torpor | Water under 2 °C and a torpor speed under 1 | Lies near the bottom, moves at the torpor speed | Water over 3 °C |
| Gasp | Dissolved oxygen under 4 mg/L | Hangs under the nearest hole, gills flaring | Oxygen rises, or it dies |
| Stranded | Water shallower than its body depth | Flops toward water at 0.4 m/s | Water reached, grabbed, or dead by its vitality |

### Lies, seams and feeding lanes

**A lie is where food comes to a resting fish.** Each water cell inside a spot's radius gets a lie score S from the current attachment and the depth.

```latex
S = \max(0,\ V_{n} - V_{c}) \cdot \max\left(0,\ 1 - \frac{V_{c}}{1.5}\right) \cdot \min\left(1,\ \frac{D}{0.6}\right) \cdot (1 + 0.5\,c)
```

V\_c is the cell's current in m/s, V\_n the fastest current within 2 m and D the depth in m. c is 1 with overhead cover within 1 m: a cut bank, sweeper, lily pads or ice. Seams, boulder eddies and pool heads score high without being hand-placed. Still water uses cruise loops instead.

**Pecking order.** Fish at a spot are sorted by weight and take lies by score, heaviest first. When the top fish is caught, the next heaviest moves up within 10 game minutes. So the best lie in the best pool always holds the biggest fish there, and the crouch-watch names it once seen: "Big grayling at the head of the pool. About 0.9 kg."

**Feeding lane.** A holding fish watches a strip of drift as wide as the formula below. Lane takes per game minute are 0.2 + 3 × H + 0.5 × d, where d is the drift of the riffle above (0–1, from its larva density). A fish feeding once a minute or more holds 0.3 m under the surface where it shows; one feeding less holds on the bottom.

```latex
w_{lane} = 0.5 + 0.02 \cdot L_{cm} \ \text{m}
```

**Sight-casting.** A bait or fly drifting through a visible holding fish's lane at the current's speed (±20%) sets m\_place to 1.3 instead of 1.0. A fly dragging faster than the current sets it to 0.5, and the fish shows the refusal: it rises, inspects and turns away. A caddis fly skated at dusk is the exception and counts as drag-free.

### Schools

**One server entity per school.** `yearofice:fish_school` holds a species, life stage, member count, centroid, heading, radius and state. The client draws the members by boids (separation 0.3 body lengths, alignment within 1 m, cohesion within 1.5 m) as instanced models with no hitboxes.

| School state | When | On screen |
| --- | --- | --- |
| Mill | Resting in a pool or eddy | A slow ring of fish, flanks catching light |
| Travel | Moving along a shore or upstream | A stream of fish in a line |
| Feed | A hatch, a gut pile, plankton under a light | Spread out; flashes as members turn |
| Compress | A threat within 10 m | Radius halves; members darken as they face the same way |
| Flash expansion | A spook | Members burst 2–4 m outward and regroup in 3–6 s |
| Ball | A loon, merganser or lake trout attacking | A tight sphere near the surface; terns and gulls arrive within 2 game minutes |
| Fountain | A predator passing through | The school splits around it and closes behind |

Nets, the dip net, the mesh scoop and the minnow trap take members from a school's count by the existing gear rules. A school hit by a scoop loses members and enters Flash expansion.

### Rising to hatches

**The ring count stays.** Rise rings per game minute are N ÷ 10 × (1 + 2H), with H from The hatch calendar. That is the existing "one ring a minute per 10 fish, ×3 during a hatch". Each ring now has a fish under it.

**The rise form tells the insect.** The crouch-watch names the form the first three times, then the notebook does; the matching patterns are in The hatch calendar.

| Rise form | What it looks like | What the fish is taking |
| --- | --- | --- |
| Sip | A small ring and a soft "plip" | Mayfly duns, midges, spent spinners |
| Head and tail | The nose, then the dorsal fin, then the tail roll over | Mayfly spinners at dusk |
| Slash | A splashy break with spray | Adult caddis skittering at dusk |
| Bulge | A boil with no break in the surface | Nymphs and pupae just under the film |
| Leap | The fish clears the water | Egg-laying stoneflies and caddis |

### Spooking

The spot's wariness W from Fish that see you drives every entity there. These triggers join that table:

- **Chain flight.** A fish entering Flee adds +15 to every spot within 4 m. One bolting fish can empty a pool.
- **Other hunters.** A beaver tail slap adds +30 within 20 m. An otter hunting a pool adds +40. A pike strike adds +20 for fish within 3 m.
- **Line slap.** A cast charged over 90% lands hard and adds +10 instead of the existing +5.
- **Lantern at night.** Pike and grayling within 4 m add +10 a game minute. Cisco, burbot and sculpin come toward the light instead (Life under the ice).

**Wary but not spooked.** At W 30–49, fish hold 0.3 m deeper, every drag refusal is certain, and a bait check turns away 50% of the time. At 50 the existing Spooked rule applies.

### Winter torpor under ice

**Cold slows most fish, not all.** Under ice each species takes a torpor speed and an under-ice appetite a\_ice. a\_ice multiplies m\_water, so the existing clamp of 0.1–3.0 on the stacked factors still holds, and the existing ×0.7 below 4 °C stays.

| Species | Torpor speed | a\_ice | Where it lies under the ice |
| --- | --- | --- | --- |
| Burbot | ×1.0 | 1.4 | Deep holes; sand bars in the spawn |
| Lake trout, Arctic char | ×0.8 | 1.0 | Drop-offs, cruising just under the ice at dawn |
| Northern pike | ×0.5 | 0.9 | Weed shelf edges; shallow in March |
| Grayling, Dolly Varden, round whitefish | ×0.5; ×1.0 in spring leads | 0.8; 1.0 in spring leads | Deep holes, leads, the hot-spring mixing zone |
| Humpback, broad and pygmy whitefish, cisco | ×0.5 | 0.8 | Loose schools near the bottom |
| Longnose sucker | ×0.2 | 0.2 | Packed in deep holes, almost dormant |
| Lake chub, sculpin, trout-perch | ×0.3 | 0.4 | Under stones and in deep slow water |
| Alaska blackfish | ×0.2 | 1.0 at breathing holes | Sedge breathing holes |

**Seeing through the ice.** Clear black ice under 15 cm thick with no snow on it counts as water of the type's visibility. A player lying flat on it (Shift held, looking down) sees fish below as through a window: char over the shoal, a pike under a weed edge, a school as a slow dark patch.

### From fish in the water to fish on the bank

1. **The bite roll.** The existing bite roll in Conditions picks a species, and the size draw a weight. Nothing about it changes.
2. **The taker.** `FishProjector` hands the bite to the nearest visible fish of that species within 4 m of the bait, using its weight. With none in range, or in water too murky to see, it spawns one unseen under the bobber at the bed.
3. **The take.** The fish enters Take and swims to the bait in 0.5–1.5 s. In clear water the player sees the mouth open and the turn; in murky water, a swirl. The bobber dips and the existing strike window opens.
4. **Hooked.** On a good strike the entity's movement is handed to `HookedFishController`, which runs the existing fight step function on the server. The fish's position follows the fight: a run moves it at the run speed, a jump plays the leap animation and throws spray, Tired rolls it on its side. The line is drawn from the rod tip to the fish's jaw bone.
5. **Landed.** A grab, net or gaff removes the entity, takes 1 from N and gives the item: the species id with the `yearofice:fish_data` component (species, length\_cm, sex, stage, roe\_kg, alive, vitality, caught\_day, spot). Its weight lives only in `yearofice:weight_g`, which the load sum reads (Minecraft development › Conflicting ids, row 8).
6. **Lost.** A snap, spit or bite-through sends the fish to Flee and adds +40 to W. A fish that broke off keeps a `hooked_before` flag for 7 game days: its wariness counts ×1.5, and a fish caught with the old hook in its jaw gives the hook back.
7. **Released.** Using a live fish item on water within 60 s puts it back. With vitality over 40 it swims off; under 40 it rolls belly up and drifts, a free meal for an eagle. A release still counts as caught for 7 days, as in Fish stocks.
8. **Dead fish.** A fish killed and not taken floats for 2 game hours, then sinks. Floating, it is a scent point of strength 0.5 and draws gulls.

### Caps and cost

**Caps per water body** keep a big lake from filling with entities. A river counts each 100 m reach as one body.

| Body size | Individuals | Schools | Ambient water life |
| --- | --- | --- | --- |
| Under 0.1 ha | 3 | 1 | 8 |
| 0.1–1 ha | 6 | 2 | 16 |
| 1–10 ha | 10 | 3 | 24 |
| Over 10 ha, or a 100 m river reach | 12 | 4 | 24 |

**Caps per player.** At most 12 individual fish within 32 blocks, as in Waters and fish, 6 schools within 48 blocks and 160 drawn school members in all. Ambient water life (striders, beetles, frogs, toads, tadpole schools) is capped at 40 within 24 blocks. Farthest first is removed.

**Cost.** Fish and water life take at most 1.5 ms on average and 2.0 ms at peak of the animals' 8 ms line (Minecraft development › Entities and AI, the three-way split; RECONCILE 91). Hatch insects and striders are client particles, never entities. A test below holds the line.

### What the player sees and hears

| Sign | Made by | Seen from | Sound | What it tells |
| --- | --- | --- | --- | --- |
| Rise ring | A fish taking at the surface | 40 m | A "plip" | Surface feeders; the form names the insect |
| Swirl or boil | A fish over 2 kg turning under the surface | 30 m | A soft "whump" | A pike, lake trout or salmon close in |
| V-wake | A fish moving fast in water under 0.5 m | 30 m | — | A fish crossing a shallow, or a beaver or muskrat |
| Bow wave | A big fish pushing water in the shallows | 50 m | — | Pike on the May shelves, salmon on a riffle |
| Nervous water | A school just under a calm surface | 60 m | — | Cisco or chub; shows best looking across glare |
| Flash | School members turning in the sun | 20 m in clear water | — | A school feeding |
| Shadow on the bed | Any fish over pale gravel or sand in sun | 15 m | — | A fish even where its back is camouflaged |
| Tailing | Fish nosing the bottom, tails breaking the surface | 40 m | Splashes | Suckers and whitefish feeding in the shallows |
| Backs out | Spawning fish in water shallower than their depth | 80 m | Thrashing | Pike in the sedge, salmon on the redds |
| Leaping salmon | A run at a falls | 150 m | A slap as each falls back | A run in the pool, and hand-grab chances |
| Porpoising | Salmon or sheefish rolling in a pool | 100 m | A soft roll | Fish holding in that pool |
| Bubble trail | A diving beaver, muskrat, otter or loon | 20 m | A burble | Where it went and where it will surface |
| Strider dimples | Water striders on slack water | 10 m | — | Not a rise: slack water and an eddy |

**Under the water.** With the head under, fog closes at the type's visibility. Light shafts slant in at the sun's angle, silt drifts with the current, and fish hold in silhouette against the brighter surface. Sound is muffled; a riffle above roars low, and a beaver's dive is a dull knock.

## Mob sheets

Every fish and aquatic animal gets the same sheet: body and model, life stages, where it holds each season, what it eats, how wary it is, how it spawns and what the player can do to it. Fight numbers stay where they are, and the sheet points to them.

### Reading a sheet

**One rig per family.** Fish share seven GeckoLib rigs: salmonid, esocid (pike), gadid (burbot), coregonid (whitefish and cisco), catostomid (sucker), small fish (sculpin, chub, stickleback, blackfish, trout-perch) and lamprey (a 10-bone spine chain). Each rig carries the common animations; the sheet lists only the extras.

**Common animations.** `swim.hold`, `swim.cruise`, `swim.burst`, `rise.sip`, `rise.slash`, `turn.flash`, `leap`, `spawn.quiver`, `hooked.thrash`, `tired.roll`, `land.flop`, `gasp`, `dead.drift`.

**Life stages.** A stage sets the texture, the size band and what the fish is worth. Salmon keep the four run stages in Waters and fish; the rest use these.

| Stage | Size | When | Value and rule |
| --- | --- | --- | --- |
| Fry | 2–5 cm | First summer | A school only; bait at most; scooped, not caught |
| Parr | 5–15 cm | Coho, chinook, Dolly Varden, char in their 1–3 years in fresh water; dark bars on the flanks | Not food; taking one counts 0.1 toward the run's h |
| Smolt | 10–18 cm | May 20 – June 25, migrating to sea at night; silver, bars fading | Not food; the smolt night (New water life) |
| Adult | The species' kg range | Outside the spawn | Full value |
| Spawner | The species' kg range | Inside its spawn window at its site | Full color; kcal ×0.85 for repeat spawners; counts toward s |
| Kelt | ×0.8 of its weight | 0–30 game days after spawning, repeat spawners only | Dark and thin; kcal ×0.6; bites ×1.5 |

### Body and model

Hitboxes are width × height in blocks. Minecraft boxes are square in plan, so width is half the adult length, which keeps spear hits honest from any side.

| Species | Id | Hitbox | Form | Extra animations | Stage textures |
| --- | --- | --- | --- | --- | --- |
| Arctic grayling | `yearofice:arctic_grayling` | 0.25 × 0.12 | Individual; fry school | `display.sail` (dorsal raised) | Fry, adult, spawner (purple sail, red-spotted fin), kelt |
| Northern pike | `yearofice:northern_pike` | 0.55 × 0.15 | Individual | `ambush.s_bend`, `strike.lunge` | Fry, adult, spawner, kelt |
| Chinook salmon | `yearofice:chinook_salmon` | 0.55 × 0.25 | Individual; parr school | `spawn.dig`, `spawn.charge` | Parr, smolt, the four run stages |
| Chum salmon | `yearofice:chum_salmon` | 0.40 × 0.20 | Individual | `spawn.dig`, `spawn.charge` | Fry, the four run stages with tiger bars |
| Coho salmon | `yearofice:coho_salmon` | 0.35 × 0.17 | Individual; parr school | `spawn.dig`, `spawn.charge` | Parr, smolt, the four run stages |
| Sockeye salmon | `yearofice:sockeye_salmon` | 0.33 × 0.18; spawner 0.33 × 0.22 | Individual; smolt school | `spawn.dig`, `spawn.charge` | Fry, smolt, the four run stages, green head |
| Arctic char | `yearofice:arctic_char` | 0.30 × 0.12 | Individual | `spawn.hover` | Parr, adult, spawner (orange-red belly), kelt |
| Dolly Varden | `yearofice:dolly_varden` | 0.25 × 0.10 | Individual; parr school | `egg.drift_take` | Parr, adult, spawner, kelt |
| Lake trout | `yearofice:lake_trout` | 0.45 × 0.16 | Individual | `cruise.deep` | Adult, spawner, kelt |
| Burbot | `yearofice:burbot` | 0.40 × 0.12 | Individual | `body.coil`, `spawn.ball` | Adult, spawner |
| Sheefish | `yearofice:sheefish` | 0.55 × 0.20 | Individual | `chase.surface` | Adult, spawner, kelt |
| Humpback whitefish | `yearofice:humpback_whitefish` | 0.25 × 0.14 | School of 5–20 | `feed.tail_up` | Adult, spawner |
| Broad whitefish | `yearofice:broad_whitefish` | 0.33 × 0.16 | School of 5–20 | `feed.tail_up` | Adult, spawner, kelt |
| Round whitefish | `yearofice:round_whitefish` | 0.20 × 0.07 | Loose school | `feed.nose_down` | Adult, spawner |
| Least cisco | `yearofice:least_cisco` | 0.15 × 0.06 | School | — | Adult, spawner |
| Bering cisco | `yearofice:bering_cisco` | 0.20 × 0.08 | School on the run | `roll.surface` | Adult, spawner |
| Pygmy whitefish (new) | `yearofice:pygmy_whitefish` | 0.06 × 0.02 | School | — | Adult |
| Longnose sucker | `yearofice:longnose_sucker` | 0.25 × 0.10 | Loose school | `feed.vacuum`, `spawn.vibrate` | Adult, spawner (red stripe on males), kelt |
| Slimy sculpin | `yearofice:slimy_sculpin` | 0.08 × 0.04 | Individual, ambient | `hop.bottom`, `nest.guard` | Adult, guarding male (dark) |
| Lake chub | `yearofice:lake_chub` | 0.07 × 0.03 | School | — | Adult, spawner (red at the fin bases) |
| Ninespine stickleback | `yearofice:ninespine_stickleback` | 0.04 × 0.02 | School | `nest.fan` | Adult, black nesting male |
| Alaska blackfish | `yearofice:alaska_blackfish` | 0.10 × 0.04 | Individual, ambient | `gulp.air` | Adult |
| Trout-perch (new) | `yearofice:trout_perch` | 0.06 × 0.03 | School, at night | — | Adult |
| Arctic lamprey | `yearofice:arctic_lamprey` | 0.18 × 0.04 | Individual on the run | `attach.sucker`, `swim.eel` | Ammocoete (larva), adult |
| Yukon floater | `yearofice:yukon_floater` | 0.15 × 0.06 | Not an entity: the `filled` state of the `yearofice:mussel_bed` block (S21); the box is the block's | `filter.gape` | — |

### Where each fish holds through the year

"Spot" names are the spot types in Wildlife and fish, Reading the water and Waters and fish.

| Species | May–June | July–August | September–October | November–April |
| --- | --- | --- | --- | --- |
| Arctic grayling | Creek mouths to spawn, then up the creeks; kelts at the mouths in early June | Riffle heads, seams, pools below falls, lake outlets; under willows on windy days | Dropping down to the river; bends and confluence lines | Deep river holes, spring leads, the mixing zone |
| Northern pike | Flooded sedge flats to spawn, then weed shelves | Weed edges, lily-pad edges, oxbow shelves | Weed shelves; drop to 2–3 m as weeds die | Under the ice at shelf edges; shallow in March |
| Chinook salmon | Parr in side channels; smolts leave by June 25 | The run, June 25 – July 31; pools below falls by day | Spawned out; carcasses | Parr in spring leads and beaver ponds |
| Chum salmon | Fry leave for the sea by June 15 | The run; side channels at Salmon Bend | Fall chum in the spring leads | Eggs in the gravel of the leads |
| Coho salmon | Parr in beaver ponds and side streams; smolts leave | Parr in pond edges and bur-reed | The run, August 10 – September 30; clear side streams | Parr in beaver ponds and leads |
| Sockeye salmon | Smolts pour out of the Big Lake outlet | The run, July 20 – August 25 | Spawners on the lake beaches; dead by October | Fry in Big Lake, deep by day, shallow at night |
| Arctic char | Tarns still under ice; deep | Tarn drop-offs; the shoal at dawn | The shoal to spawn, September 10–30 | Under tarn ice to July |
| Dolly Varden | Creek pools in the north range | Plunge pools, undercut banks | Below salmon redds; spawning in October | Spring leads |
| Lake trout | Lake shallows in the ice-out frenzy | Below 10 m; the thermocline edge | Rocky shoals to spawn, late September | Drop-offs; just under the ice at dawn |
| Burbot | Deep river holes; lake drop-offs | Deep and cold; night only | Moving up to the shallows at night | Deep holes, then the sand-bar spawn, January 25 – February 28 |
| Sheefish | Lower river and Big Lake | Big Lake, river bends at dusk | The run to the canyon, September 15 – October 10 | Big Lake and the lower river |
| Humpback whitefish | Lakes, slow river | Lake inlets, slow river | Fall run to lake inlets | Deep lake water |
| Broad whitefish | Oxbows, weed shelves | Oxbow shelves over snails | The run out of the oxbows and up to the canyon | Deep river and lake water |
| Round whitefish | Riffles, gravel runs | Riffles, behind the player's boots | Below redds | Deep river holes |
| Least cisco | Lake surface in the ice-out frenzy | Open lake mid-water | Lake margins at night to spawn, October 1–25 | Mid-water under the ice |
| Bering cisco | Absent | Absent | The run, September 1 – October 15; eddies at dusk | Absent |
| Pygmy whitefish (new) | Deep Big Lake | Below 15 m | Shallow gravel at night to spawn, October 10–31 | Deep Big Lake |
| Longnose sucker | The run up clear creeks, May 25 – June 15 | Weed shelves, slow pools | Slow pools | Packed in deep holes |
| Slimy sculpin | Under riffle stones; nests April 20 – May 31 | Under stones; out on gravel at night | Under stones | Under stones in unfrozen water |
| Lake chub | Creek mouths to spawn | Creek mouths, lake inlets, beaver ponds | The same, schooling up | Deep slow water |
| Ninespine stickleback | Weed shelves; nests in June | Every still water | Weed edges | Under the ice in every still water that keeps oxygen |
| Alaska blackfish | Bog edges as they thaw | Bog pools, tundra ponds | Fattening in the sedge | Under sedge breathing holes |
| Trout-perch (new) | Slough edges at night | Sandy slough shallows at night; deep by day | The same | Deep slow river |
| Arctic lamprey | Ammocoetes in silt beds all year | Ammocoetes | Adults gather below the map | The run, November 20 – December 15; ammocoetes in silt |
| Yukon floater | Soft beds 0.3–2 m; filtering, siphons showing | The same; muskrats dig them | The same | Deeper beds; raked through the ice |

### Diet, wariness and fight

Bait multipliers stay in Fishing in depth › Bait multipliers and Waters and fish › Fight data, plus the new baits in this tab. The wariness multiplier scales every trigger in Fish that see you; the five existing rows are kept.

| Species | Eats | New baits here | Wariness × | Spook rule of its own | Fight profile |
| --- | --- | --- | --- | --- | --- |
| Arctic grayling | Drift insects, terrestrials, scuds, eggs, fry | Grey mayfly 1.3, pale mayfly 1.3, scud 1.3, ant fly 1.2 in windfall | ×1.0 | Refuses drag every time; a spooked pool's biggest fish leaves its lie for 30 game minutes | Fishing in depth |
| Northern pike | Fish, frogs, ducklings, voles | Live trout-perch 1.6, lamprey larva 1.0 | ×0.6 | Ignores shadow; spooks only on a splash within 5 m | Fishing in depth |
| Chinook salmon | Nothing in fresh water | — | ×0.4 in the run | Holds deep; a spooked pool's fish run upstream | Fishing in depth |
| Chum salmon | Nothing in fresh water | — | ×0.4 | Spawners on redds return within 5 game minutes | Fishing in depth |
| Coho salmon | Nothing in fresh water | — | ×0.4 | Jumps when spooked | Fishing in depth |
| Sockeye salmon | Nothing in fresh water | — | ×0.4 | Schools tighter | Waters and fish |
| Arctic char | Snails, scuds, stickleback, midges | Scud 1.0, pond snail 0.8 | ×1.2 | Clear tarn water: shadow range ×1.5 | Waters and fish |
| Dolly Varden | Salmon eggs, drift, sculpin | Lamprey larva 0.8, scud 1.1 | ×0.9 | Behind spawning salmon ×0.5 | Waters and fish |
| Lake trout | Cisco, pygmy whitefish, sculpin | Pygmy whitefish 1.2 dead or live | ×0.7 | Feels footsteps on ice at 15 m, not 10 | Fishing in depth |
| Burbot | Fish, sculpin, lamprey, larvae | Lamprey larva 1.3, trout-perch 1.2 | ×0.3 | Comes to a lantern; spooks only on a splash | Fishing in depth |
| Sheefish | Cisco, chub, fry | — | ×0.8 | Chases a spoon to the surface and turns away when wary | Fishing in depth |
| Humpback whitefish | Snails, midges, caddis | Pond snail 1.0, midge 1.2 | ×1.2 | A school spooks together | Fishing in depth |
| Broad whitefish | Snails, mussels, midges | Pond snail 1.0 (now defined) | ×1.2 | A school spooks together | Waters and fish |
| Round whitefish | Larvae, eggs | Mayfly nymph 1.2 | ×1.0 | Comes back to the boots within 2 game minutes | Waters and fish |
| Least cisco | Plankton, midges | Midge 1.4 for 10 days after ice-out | ×1.0 | Flash expansion; regroups fast | Waters and fish |
| Bering cisco | Little on the run | — | ×0.6 | — | Waters and fish |
| Pygmy whitefish (new) | Plankton, midge larvae | — | ×1.0 | — | Never fought |
| Longnose sucker | Bottom ooze, larvae, snails | Mayfly nymph 0.8 | ×0.8; ×0.3 in the run | — | Fishing in depth |
| Slimy sculpin | Larvae, fish eggs | — | ×0.5 | Freezes on the bed instead of fleeing | Never fought |
| Lake chub | Insects, algae, fry | — | ×0.8 | — | Never fought |
| Ninespine stickleback | Plankton, eggs | — | ×0.5 | Nesting males stand their ground | Never fought |
| Alaska blackfish | Insects, snails, tadpoles | — | ×0.5 | Burrows into the sedge mat | Never fought |
| Trout-perch (new) | Midge larvae, scuds | — | ×0.8; drawn to a lantern | — | Never fought |
| Arctic lamprey | Adults on the run eat nothing; ammocoetes filter silt | — | ×0.3 | Ammocoetes burrow in 2 s | Never fought |

### Spawning the player can watch

Each spawn is a sequence the player can watch from a crouch at 10–20 m with W under 30. Watching a full sequence once writes the species' spawn page in the record book.

| Species | Window and site | What the player sees, in order |
| --- | --- | --- |
| Arctic grayling | Breakup to May 31; creek gravel above the mouths | Males hold territories 2–3 m apart, sails raised. A female enters; the male folds his sail over her back; both quiver 3–5 s and a puff of silt rises |
| Northern pike | Ice-out to May 31; flooded sedge 0.2–0.5 m deep | A big female with 1–3 small males alongside; backs and tails out; a thrash every 20–60 s scatters eggs on the sedge stems |
| Chinook, chum, coho, sockeye | Their spawning grounds, last third of each run | The female turns on her side and beats the gravel (`spawn.dig`) every 20–60 s; the redd grows. Males charge and bite rivals; small jacks dart in at the quiver; Dolly Varden hover below |
| Arctic char | September 10–30; the tarn shoal | Red-bellied males hover over the gravel; a female sinks into a scrape; the pair quivers. Seen through skim ice late in the window |
| Dolly Varden | October; creek gravel below salmon reaches | Like a small salmon spawn, with the female's dig half as strong |
| Lake trout | Late September, at night; rocky lake shoals 1–3 m deep | No redd. Groups of 5–15 sweep over the rocks after dark; eggs fall into the cracks. A lantern shows them |
| Burbot | January 25 – February 28, at night; sand and gravel under ice | A writhing ball of 10–20 fish, 1–2 m across, rolling over the bed (Burbot spawning under the ice) |
| Sheefish | Late September – early October; the canyon gravel | Fish rise to the surface and release eggs in mid-water at dusk; silver flanks break the surface |
| Humpback and broad whitefish | October; lake inlets and the canyon gravel | Pairs rise together at dusk; splashes at the surface |
| Least cisco | October 1–25, at night; lake margins under skim ice | Silver flashes under the ice by moon or lantern |
| Pygmy whitefish (new) | October 10–31, at night; shallow lake gravel | Small schools in 1–2 m of water, flashing |
| Longnose sucker | May 25 – June 15; creek riffles | One female between 2–4 red-striped males; all vibrate 3–5 s; gravel clouds |
| Slimy sculpin | April 20 – May 31; under riffle stones | Turned over, the stone shows an orange egg cluster and a dark male that does not leave it |
| Ninespine stickleback | June; weed beds | Black males build a nest of weed bits and fan it; they chase anything that comes near |
| Arctic lamprey | Upriver of the map in spring | Not on the map; the ammocoetes in the silt are its children |

### Player verbs

Each verb is in Fishing in depth or this tab. "Tickle" and "transplant" are new.

| Species | Spot | Cast | Net | Spear | Trap | Grab | Tickle | Release | Transplant |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Arctic grayling | Rises, shadows | Fly, insect, worm | Gill, weir | Clear water | Weir basket | Shuffle and falls pools | Yes, under banks | Yes | Ponds with an inlet creek |
| Northern pike | Swirls, backs out in May | Live bait, spoon | Gill, darkhouse spear | Weeds and the sedge spawn | Set line | Sedge spawn only | No | Yes | Any pond; see Stocking |
| Chinook salmon | Leaps, porpoising | Roe, spinner | Gill, dip, wheel | Shallows | Wheel | Falls lip | No | Yes, over 40 vitality | No |
| Chum salmon | Leaps, backs out | Roe | Gill, dip, wheel | Shallows | Wheel | Falls lip, redds | No | Yes | No |
| Coho salmon | Leaps | Spinner, roe | Gill, dip | Shallows | Weir | Falls lip | No | Yes | No |
| Sockeye salmon | Red water | Rarely | Dip, gill, wheel | Beaches | Wheel | Beaches | No | Yes | No |
| Arctic char | Shadows through ice | Jig, live stickleback | Gill | Through ice | No | No | No | Yes | Tarns only |
| Dolly Varden | Shadows below redds | Roe, chunk | Gill | Clear water | Weir | No | Yes | Yes | Creek ponds |
| Lake trout | Gulls over cisco schools | Jig deep | Gill | Darkhouse | Set line | No | No | Yes, through the hole | No |
| Burbot | Nothing; lantern at night | Night bait | Gill | Spawn ball by lantern | Set line, tip-up | No | Yes, under stones | Yes | Deep ponds |
| Sheefish | Jumps at dusk | Spoon | Gill, wheel | No | No | No | No | Yes | No |
| Humpback whitefish | Rings, tailing | Small bait | Gill | Shallows | Weir | No | No | Yes | Lakes |
| Broad whitefish | Tailing | Snail, mussel | Gill | No | Wheel | No | No | Yes | Oxbows |
| Round whitefish | At the boots | Larvae | Gill | Yes | Weir | Shuffle, 20% | No | Yes | Creek ponds |
| Least cisco | Nervous water, terns | Small fly | Fine gill | No | No | No | No | No | Lakes, as forage |
| Bering cisco | Rolling at dusk | Rarely | Fine gill, wheel | No | Wheel | No | No | No | No |
| Pygmy whitefish (new) | Gulls over lake trout chasing them | No | Fine gill, deep | No | No | No | No | No | Lakes, as forage |
| Longnose sucker | Tailing, the run | Worm | Gill, dip | Yes | Weir | The run, 50% | No | Yes | Any pond |
| Slimy sculpin | Under stones | No | Scoop | Fork by lantern | Minnow trap | Stone flip, 25% | No | Yes | Creek ponds |
| Lake chub | Schools | No | Scoop | No | Minnow trap | No | No | Yes | Any water |
| Ninespine stickleback | Schools, nests | No | Scoop | No | Minnow trap | No | No | Yes | Any still water |
| Alaska blackfish | Breathing holes | No | Scoop | No | Blackfish trap | Through a breathing hole, 30% | No | Yes | Ponds and bogs |
| Trout-perch (new) | Eyes shining in a lantern | No | Scoop at night | No | Minnow trap at night | No | No | Yes | No |
| Arctic lamprey | Hanging at holes | No | No | No | Lamprey basket | Ammocoetes dug from silt | No | No | No |
| Yukon floater | Muskrat middens, soft beds | — | — | — | — | Feel with feet and hands; mussel rake through ice | — | Yes | Oxbows and lakes with soft beds |

**Stalking.** Every fish the player can see is stalked by the Fish that see you rules: from downstream, crouched, sun in front. Fish seen only as schools or at night (cisco, pygmy whitefish, trout-perch, burbot, lake trout, lamprey) are not stalked; they are read by sign.

**Tickling.** Grayling, Dolly Varden and burbot under a cut bank or flat stone can be taken by hand. The player lies on the bank edge (crouch with Shift and hold RMB on the water) for 20–40 s, stroking the belly, then grabs. It works 30% of the time when Clumsy and 60% when Practiced ("Tickle", N = 20), only with W under 30 and hands above their safe-to limit. A failed grab adds +60 to W.

### Water mammals and amphibians

These sheets cover only the water side; senses, yields and land behaviour stay in Wildlife and fish, Animals in depth and Bestiary.

| Animal | Hitbox | Water animations | Life stages | In the water by season | Player verbs at the water |
| --- | --- | --- | --- | --- | --- |
| Beaver | 0.7 × 0.5 | `swim.surface`, `dive`, `tail_slap`, `tow_branch`, `swim.under` | Kit, yearling, adult | Ponds and dams May – October; under the ice to the feed bed November – April | Watch, lodge snare on a bubble run, arrow at dusk |
| Muskrat | 0.4 × 0.25 | `swim.scull`, `dive`, `carry_reed`, `feed_platform` | Kit, adult | Ponds and sloughs; push-ups all winter | Watch, push-up snare, count push-ups |
| River otter | 0.55 × 0.35 | `porpoise`, `periscope`, `slide`, `roll_snow`, `eat_on_ice` | Pup, adult | Rivers and leads all year; slides at dusk | Watch play, slide snare, read latrines |
| Mink | 0.3 × 0.2 | `bound.edge`, `dive.short`, `carry_prey` | Kit, adult | Banks and leads all year | Track, drowning slide, rob its cache |
| Wood frog | 0.06 × 0.04 | `swim.kick`, `call.sacs`, `float.splay` | Egg mass, tadpole, froglet, adult, frozen | Ponds ice-out to July; litter October – April | Hear, scoop eggs, transplant, thaw, bait |
| Boreal toad | 0.1 × 0.06 | `walk`, `peep`, `burrow` | Egg string, tadpole, toadlet, adult | Hot-spring pools May – September | Watch at night only |
| Diving beetle | 0.03 × 0.02 | `swim.paddle`, `fly.lantern` | Larva, adult | Bog pools May – September | Avoid barefoot; watch it hit the lantern |

### Notes by species

**Arctic grayling.** The valley's sight-fishing teacher. Males' sails reach the adipose fin; females' stop short, so sex shows from the bank. Grayling move between three waters a year: winter holes, spawning creeks at breakup and summer feeding creeks higher up, back down in September. On windy days from July 15 to August 31, terrestrial insects blow onto the water and grayling under overhanging willows bite ×1.3 on insect bait or the ant fly. A fresh grayling smells faintly of thyme; the journal says so once.

**Slimy sculpin.** A big-headed bottom fish that freezes rather than flees. At night it comes out onto the gravel, where a lantern and a two-tine fish fork take 1 every 2 game minutes.

## New water life

Twenty-one new species and life-stage systems fill the water with things to find, watch and use. Each ties to at least two existing systems and shows in the world before the messenger names it.

### At a glance

| New element | Kind | Where | What it gives the player |
| --- | --- | --- | --- |
| Small grey mayfly | Insect, hatch | Clearwater creeks, spring leads | Afternoon rises; nymphs as bait |
| Pale mayfly | Insect, hatch | Clearwater creeks | Dusk spinner falls; head-and-tail rises |
| Lake midge (chironomid) | Insect, hatch | Lakes, oxbows, hot-spring outflow | Cisco and whitefish on midges; midge smoke on shore |
| Fall caddis | Insect, hatch | Clearwater creeks | Dusk slash rises from Dolly Varden |
| Scud | Amphipod | Spring leads, hot-spring mixing zone | Winter bait; fat spring grayling |
| Pond snail | Snail | Pondweed in oxbows and beaver ponds | Whitefish bait (the existing "snail" bait) |
| Fairy shrimp | Crustacean | Fishless tundra ponds and melt pools | Proof a pond has no fish; bait |
| Water strider | Insect | Slack water, eddies | Marks eddies; false rises |
| Predaceous diving beetle | Insect | Bog pools, ponds | Bites waders; crashes into lanterns |
| Dragonfly nymph (deepened) | Life stage | Ponds | Shucks on stems; pike and grayling bait |
| Wood frog breeding | Life stage system | Fishless ponds and bogs | A frost forecast; transplantable eggs |
| Boreal toad | Amphibian | Hot Spring Basin (C2) only | The valley's rarest animal |
| Pygmy whitefish | Fish | Deep Big Lake | Lake trout forage and bait |
| Trout-perch | Fish | Slough sand at night | Pike and burbot bait |
| Salmon young | Life stage system | Redds, side channels, beaver ponds, the lake outlet | The echo made visible; the smolt night |
| Kelts | Life stage | Every repeat spawner | Thin, hungry fish |
| Pike in the flooded sedge | Spawn system | Sedge flats at oxbows and sloughs | Spears and grabs; a year class that can fail |
| The sucker run | Run | Clear creeks off the oxbows | The first easy fish of spring |
| Burbot spawning ball | Spawn system | Sand bars under river and lake ice | The best winter set-line nights |
| Sculpin nests and lamprey larvae | Life stages | Riffle stones; silt beds | Bait, and a stone to put back |
| Salmon carcasses through the winter | Ecology system | Every spawning reach | Winter bait; spring bear food; fat young fish |

### The hatch calendar

**Hatches have names now.** The existing hatch rule in Conditions (×1.5 on flies and insects on cloudy days, June to August, water 8–16 °C) stays as the general hatch. This calendar names which insect is hatching, when, and how strong it is as H from 0 to 1.

| Window | Hatch | Water | Hours | Peak H | Who rises | Pattern |
| --- | --- | --- | --- | --- | --- | --- |
| Ice-out + 0–10 days | Lake midge emergence | Big lake, oxbows | Dawn and dusk | 0.8 | Least cisco, humpback and round whitefish | Midge |
| May 20 – June 20 | Small grey mayfly | Clearwater creeks, spring leads | 13:00–17:00; ×1.5 on overcast days | 0.6 | Grayling, round whitefish | Grey mayfly |
| June 5–20 | Stonefly emergence (exists) | Fast riffles | Nymphs crawl out at night, adults fly at midday | 0.7 | Grayling, Dolly Varden | Stonefly |
| June 25 – July 25 | Pale mayfly spinner fall | Clearwater creeks | 21:00–23:00 | 0.8 | Grayling, whitefish | Pale mayfly |
| July 1 – August 15 | Evening caddis (exists) | Creeks, lake edges | Dusk, 1 game hour | 1.0 | Grayling | Caddis |
| July 15 – August 31 | Windfall terrestrials | Willow banks | Afternoons with wind over 5 m/s | 0.5 | Grayling, chub | Ant |
| August 20 – September 30 | Small grey mayfly, second brood | Creeks | 13:00–17:00 | 0.5 | Grayling | Grey mayfly |
| September 1–25 | Fall caddis | Creeks | Dusk | 0.6 | Dolly Varden, grayling | Caddis |
| November – March | Winter midges | Spring leads, hot-spring outflow | Midday with sun on the water | 0.4 | Grayling and Dolly Varden in leads | Midge |

**Hatch bite factor.** For surface feeders on a fly or insect, m\_hatch = 1 + 0.5 × H replaces the general ×1.5 while a named hatch runs, so H = 1 gives the existing value. With H at 0.5 or more, fish get selective: the matching pattern counts ×1.3 and any other fly ×0.7.

**Patterns are tied from what the player hunts.** The fly recipe in Tackle stays (3 feathers and hair on a small steel hook, workbench, 15 game minutes). A `yearofice:fly_pattern` component records which feathers went in.

| Pattern | Feathers and hair |
| --- | --- |
| Grey mayfly | Duck or ptarmigan grey feathers, hare underfur |
| Pale mayfly | Ptarmigan white with a grouse tail fibre |
| Caddis | Spruce or ruffed grouse brown, moose hair wing |
| Stonefly | Raven black, moose hair |
| Midge | Black cordage thread only, no feathers; on the smallest hook |
| Scud | Grey hare underfur, a thread rib |
| Ant | Two knots of black thread and one raven fibre |

**On screen.** Hatching insects are client particles over the water: grey mayfly duns drifting like little sailboats, pale spinners dancing in columns at dusk, caddis skittering, midges as a grey smoke at the shore that hums. Swallows and terns arrive over a strong hatch within 10 game minutes (Water, calls and encounters).

**Nymphs by kick-net.** A player shuffling a riffle for round whitefish (Waters and fish) with the mesh scoop held below the boots gets 2–5 mayfly nymphs and 0–2 scuds each game minute. Mayfly nymphs are bait ×1.2 for round whitefish and ×1.0 for grayling, and keep 3 days in the leech pouch.

### Scuds, snails and fairy shrimp

**Scuds fatten the spring fish.** Scuds are grey-green amphipods 5–15 mm long that crowd the moss and mare's tail of spring leads and the hot-spring mixing zone, thousands to a square metre. A spruce bough dipped and shaken over the bucket gives 20–60 in 1 game minute; the mesh scoop gives the same.

- **Rule.** Grayling and Dolly Varden caught at spring-lead spots carry kcal per kg ×1.1, because they feed on scuds all winter. Live scuds on the smallest hook are bait ×1.3 for grayling and ×1.1 for Dolly Varden from December to March.
- **Not food.** A thousand scuds weigh 20 g. The messenger says so once, when the player tries to cook them.
- **Readable.** Scuds die in water over 18 °C and in fouled water, so their absence below a latrine is a second water-quality sign beside the mussels.

**Pond snails are the "snail" bait.** Waters and fish lists snail bait for broad whitefish at 1.0, and this defines it. Pond snails cling to pondweed stems in oxbows and beaver ponds. The player picks 4 a game minute from June to September in knee water, or rakes weed out with a pole. They count 1.0 for humpback whitefish and 0.8 for char too, and keep 5 days in a damp bark box. Snails carry the black-spot parasite (Disease and winterkill tells).

**Fairy shrimp mean no fish.** Fairy shrimp are red-tinged shrimp 1–2 cm long that swim upside down in fishless tundra ponds and snowmelt pools from May 25 to July 10. Any fish in a pond eats them out in one season.

- **Rule.** A pond with fairy shrimp holds no fish. The crouch-watch says: "Fairy shrimp. Nothing in this pond eats them." Ducks gather on such ponds in June.
- **Bait.** A scoop gives 50–200. Live, they are grayling bait ×1.2 for 1 game hour.

### Striders, diving beetles, nymphs and leeches

**Water striders mark slack water.** Striders skate on eddies and pool tails from June to September, when water is above 8 °C. They gather where floating food collects, so a cluster marks the slack side of a seam. Their dimples look like tiny rises from 10 m, and the crouch-watch teaches the difference once: "Striders, not fish. But the eddy is there." They are client particles with a 4-dimple shadow on the bed.

**Diving beetles bite and fly.** Predaceous diving beetles live in bog pools and ponds. Their larvae, "water tigers", bite: wading barefoot in a bog pool gives a 5% chance per 5 game minutes of −1 Condition and a yelp. Adults fly at night in August; a lantern within 50 m of a pond draws 1–4 of them, which tick against the glass. Beetle larvae eat tadpoles, so frog survival in beetle ponds is ×0.8.

**Dragonfly nymphs, deepened.** Nymphs stalk the pond bottom all year and are bait as in Bestiary. From June 15 to July 10 they climb sedge and lily stems at night to emerge; by morning the stems carry empty brown shucks, and new adults dry their wings until 10:00. A player who sits by a pond at dawn in that window sees one split its shell: Spirits +2 once a year.

**Leeches, deepened.** Leeches in Bestiary keep their rules. In a pond, they swim to the splash of a wading player within 3 m in 30–60 s, so standing still in warm water is worse than walking through. Pike and burbot set lines baited with a leech count it ×1.5, as written.

### Wood frog breeding

**The chorus, then the eggs.** The wood frog in Bestiary freezes and thaws; this adds its breeding. Frogs breed only in fishless ponds, bogs and beaver backwaters, or ponds holding only blackfish and stickleback.

1. **Chorus.** For 5 days after a pond's ice-out, as in Bestiary. The chorus stops when the air falls below 4 °C, so a pond going silent at dusk means frost tonight. The messenger says it the first time: "The frogs stopped. Cold night coming."
2. **Eggs.** Within 3 days of the chorus, females lay communal egg masses in the sunniest 0.1–0.3 m water of the pond, on sedge stems: 1 block of `yearofice:frog_spawn` per 10 frogs, each holding 600–1,000 eggs.
3. **Tadpoles.** Eggs hatch in 10–20 days by water temperature. Tadpole schools graze the warm shallows for 6–8 weeks and eat mosquito larvae: B for mosquitoes is ×0.9 within 50 m of a pond with tadpoles, June to July.
4. **Froglets.** In the last week of July, the first rainy night after the tadpoles change sends hundreds of 1 cm froglets across the nearest trail. The player who walks it that night hears them pattering. The journal: "It rained frogs."

**Fish end the chorus.** A pond stocked with any fish other than blackfish or stickleback loses 60% of its frogs a year, and its chorus stops when fewer than 10 remain. Stocking a fishless pond is a trade the player sees two springs later.

**Moving frogs.** Egg masses can be scooped into a bucket (1 mass each) and set in another fishless pond. Frogs return to breed where they grew up, so a new chorus starts 2 springs later. It is the easiest "Grow without an end" act in the water.

### The boreal toad at the warm springs

**A relict that lives by the hot spring.** A colony of 20–60 boreal toads lives only in Hot Spring Basin (C2), where the ground stays above 0 °C near the vents all winter. They hibernate in that warm ground and are active from May to September, at night, on the outflow's banks.

- **Breeding.** In mid-May, toads gather in the warm outflow pools at 15–25 °C and lay strings of eggs. Their call is a soft peeping, unlike the frogs' duck-like chuckle.
- **Poison.** A toad's skin glands make it bitter. A tamed wolf that mouths one foams and loses 10 Condition for a day. A player who eats one gets the Sick stomach in Ideas from viewers.
- **Cannot be moved.** A toad carried more than 300 m from the vents dies in its first winter, every time. The rule teaches what keeps it there.
- **Rare sighting.** The first toad seen gives Spirits +5 and the record-book line "A toad, this far north." It joins the rare sightings in Bestiary, always possible at C2 but only at night.

### Pygmy whitefish and trout-perch

**Pygmy whitefish.** A finger-long whitefish of deep, cold Big Lake water below 15 m, and the lake trout's second forage after least cisco. It comes shallow only to spawn on gravel at night from October 10 to 31. A fine gill net set deeper than 15 m takes 0.05 per hour × N/K, with K 500 at a drop-off. The lake trout forage factor in Waters and fish gains a pygmy term:

```latex
m_{forage} = 0.6 + 0.4 \cdot \frac{N_{cisco}}{K_{cisco}} + 0.2 \cdot \frac{N_{pygmy}}{K_{pygmy}}
```

With both at K this is 1.2, and with pygmies at K and cisco at 0.2 it is 0.88 instead of 0.72, so pygmy whitefish soften an overnetted cisco stock. Only Big Lake holds them, so Waters and fish › Tests 6 reads 0.73 ±0.02 there; lakes without them keep the old formula and 0.6.

**Trout-perch.** A small, pale, nearly see-through fish of sandy sloughs, active at night and deep by day. Its eyes shine green in a lantern beam from 5 m. A minnow trap set overnight on slough sand takes 0.2 × q of them. Live, it is pike bait ×1.6 and burbot bait ×1.2; it keeps 1 day in a bucket.

### Salmon young: redds, fry, parr and smolts

**The echo becomes visible.** The echo in Fish stocks decides how many salmon return. Their young now show the player that number years before the run.

- **Eggs.** Each redd is a `yearofice:redd_gravel` block with an `eggs` state from 0 to 3, set by the spawners that dug it. Eggs lie in the gravel from the spawn until April.
- **Trampling.** Each player step on a redd block from the spawn until May 15 counts 2 toward that run's harvest share h. The crouch-watch warns once: "You're on a redd." Moose and bears do not count.
- **Fry.** From April 20 to May 31, fry schools appear along the edges of each spawning reach. Members drawn are min(40, last fall's escapement ÷ 20), so a crowded edge means a strong run in 3–5 years.
- **Parr.** Coho, chinook and Dolly Varden young stay 1–3 years in side channels, spring leads and beaver ponds. Parr schools hold in bur-reed and under cut banks, with dark bars on their flanks.
- **Coho need beavers.** Coho parr winter in beaver ponds. The coho run takes a beaver factor on top of y × e:

```latex
b_{coho} = \mathrm{clamp}\left(0.6 + 0.4 \cdot \frac{P}{P_0},\ 0.6,\ 1.25\right)
```

P is the beaver pond area connected to coho streams in reach now, and P\_0 its area at world creation. Trapping out the colonies drops the coho run 3 years later; letting beavers spread raises it by up to a quarter.

**The smolt night.** On 2–4 nights between May 20 and June 25, drawn per year, sockeye smolts pour out of the Big Lake outlet at dusk in schools of thousands. Mergansers, gulls and terns gather by 20:00. Lake trout and pike at the outlet bite ×2 on small spoons and live chub for those nights, and the messenger says nothing; the birds are the sign.

### Kelts

**Fish that live through spawning.** Grayling, char, Dolly Varden, lake trout, whitefish, sheefish, pike, burbot and sucker spawn again. For 30 game days after a fish's spawn it is a kelt: dark, thin, ×0.8 of its weight, kcal ×0.6, run pull ×0.7 and hungry, biting ×1.5.

- **Where.** Grayling kelts crowd creek mouths in early June; pike kelts sit on the weed shelf; Dolly Varden kelts drop downstream to the spring leads in November.
- **The choice.** A kelt counts toward nothing, since its spawn is done. But a released kelt carries its weight back next year at +10%, while a kept one is poor food. The record book marks kelts with a "k".

### Pike in the flooded sedge

**Pike spawn where the flood reaches.** From ice-out to May 31, pike move into sedge and grass flooded 0.2–0.5 m deep at oxbow and slough margins. Each such flat is a spot of a new type, Sedge flat: base q 0.50, radius 10 m, capacity 20 pike at q = 1, May only.

- **Behaviour.** Females of 3–8 kg spawn with 1–3 smaller males, backs and tails out, thrashing every 20–60 s. Spawning pike bite ×0.3 but ignore the player's shadow, so spears and grabs work: a grab succeeds 40% of the time when Clumsy and 70% when Practiced.
- **Counts as spawners.** A pike taken on a sedge flat in May counts toward that site's s in the spawning-site formula in Fish stocks.
- **The year class.** Eggs stick to the sedge and need 14 days of water. If the spring flood drops off the flat before that (drawn from the flood curve in Water, calls and encounters), the next pike K in that body falls 10% for 4 years. The messenger reads it at the flat: "The water dropped off the sedge. The pike eggs dried."
- **After.** Post-spawn pike feed hard on the weed shelf in June, which is the existing May ×1.5 carried into the first two weeks of June.

### The sucker run

**The first easy fish of spring.** Longnose suckers leave the oxbows and the main river for clear creeks between May 25 and June 15. The run has R\_mean 400 per creek, a 12-day triangle and no echo, because suckers are residents that return; the spawning-site formula in Fish stocks applies instead.

- **Taking them.** At night in the riffles, suckers crowd so thick that a dip net takes 0.4 × run strength a sweep, and a hand grab works 50% of the time.
- **Who else comes.** Spring bears fresh from the den, eagles, otters and mink work the same riffles; the draw model treats the run like a small salmon run at 0.5 strength.
- **Food.** Suckers are 1,000 kcal per kg and bony. Hot-smoked, the bones soften and the fish rates +10%.

### Burbot spawning under the ice

**The winter's strangest sight.** From January 25 to February 28, peaking around February 10, burbot leave the deep holes. They gather at 1–2 spawning grounds per river reach or lake: sand and fine gravel 1–3 m deep under the ice. Main-tab Fish species already gives February; the window now opens in late January.

- **The ball.** At night, 10–20 burbot knot into a writhing ball 1–2 m across that rolls slowly over the bed. A lantern lowered through a hole, or the darkhouse, shows it as a churning grey mass.
- **Numbers.** At a spawning ground, a factor m\_spawn of 2.8 multiplies burbot bites: four times the fish, each biting ×0.7. Like m\_stock and m\_place it sits outside the clamp. The deep holes drop to ×0.4 for the window. Burbot taken there count toward the site's s.
- **Kessler's burbot.** The legend in the dredge pond does not spawn there, so it keeps stealing bait all February.
- **Moment.** At −35 °C on a February night, the player lowers a fat lamp into a hole over the sand bar and watches twenty burbot roll over each other in the light. Then a tip-up flag pops 30 m away.

### Sculpin nests and lamprey larvae

**Put the stone back.** From April 20 to May 31, 1 turned riffle stone in 10 (`yearofice:riffle_cobble`) in a sculpin creek hides a nest. Its underside holds an orange egg cluster, guarded by a dark male. Setting the stone back as it was (RMB within 5 s) costs nothing. Leaving it turned kills the clutch: that spot's sculpin K falls by 1 for a year. The first time the player sets one back, Spirits +1 and the journal: "Put the sculpin's stone back."

**Lamprey ammocoetes.** Arctic lamprey larvae live 4–6 years buried in silt beds at slow creek edges and slough margins, the `yearofice:silt_layer` blocks in Water, calls and encounters. They show as small holes in the silt.

- **Taking.** Digging silt with hands or a shovel gives 1–3 ammocoetes a game minute, all year where the silt is under unfrozen water.
- **Bait.** Burbot ×1.3, pike ×1.0, Dolly Varden ×0.8. They keep 5 days in a damp bark box.
- **The tie.** Their bed holds the next lamprey runs, so a silt bed dug out entirely (over 200 taken) gives that run's R ×0.95 for 5 years.

### Salmon carcasses through the winter

**Carcasses keep feeding after freeze-up.** Waters and fish freezes carcasses in place at freeze-up. Each reach now keeps a frozen count C\_frozen, which falls 2% a week to scavengers.

| When | What happens | Who comes |
| --- | --- | --- |
| Freeze-up to December | Carcasses frozen into shore ice and bars, half under snow | Foxes, wolves and ravens dig; marten and mink tunnel to those under snow |
| December – March | The carcasses that remain are winter food | Bald eagles at open water below Long Falls; ravens follow the foxes |
| Breakup | Carcasses frozen into shore ice float out and lodge on new snags | Gulls return to them first, within 3 days of breakup |
| April – May | Bars thaw | Grizzlies leaving the den visit bars in C3 for the first 10 days when C\_frozen is over 100 |
| The next summer | Caddis and stoneflies grew on the carcasses in the water | Larva density ×1.5 in that reach; grayling and coho parr there draw sizes +10% |

**For the player.** A frozen carcass is dug from a bar in 1 game minute with the hatchet. It is full bait for a cubby set (Traps) all winter and wolf food at half value, never food for the player. A winter trapline along a salmon bar is the cheapest bait in the valley.

### Life under the ice

**Under the ice, the water keeps living.** What the player sees through a hole or black ice:

- **Lantern at a hole.** A lantern or fat lamp hung in a hole at night draws plankton, then cisco and trout-perch, then burbot. Burbot bite ×1.2 at that hole, and cisco flash below it after 20 game minutes. Pike and grayling avoid it.
- **Bubbles in the ice.** Beaver ponds and thaw ponds trap marsh gas from rotting plants as stacks of white discs in the ice (`yearofice:bubble_ice`), 1–3 per 100 m². Chiseling into a stack and holding a lit match or ember to it within 1 s gives a 1 m flare. A face within 0.5 m loses 2 Condition to singed eyebrows. The first flare gives Spirits +3.
- **Otters below.** In an otter's range, an ice hole has a 2% chance per game hour of an otter surfacing in it, snorting, and taking any bait fish or stringer fish within reach. Then it is gone under the ice.

## Mammals and birds under water

Four mammals and the diving birds use the water as their workplace. Their land rules stay in Wildlife and fish, Animals in depth and Bestiary; this is only what they do in and under the water.

### Beaver

**Dives and the lodge.** A beaver swims at 0.7 m/s with its nose and head showing and a V-wake behind. On alarm it slaps its tail (+30 to W within 20 m) and dives for 1–4 game minutes, surfacing 20–60 m away. Its lodge has 2–3 underwater entrances 1–1.5 m deep, each the end of a run worn in the mud.

**The feed bed.** In October each colony sinks 200–600 branches in front of the lodge. The bed is 3–10 m across, its top showing above the water: the "food raft" in Drives and daily schedules. Under the ice, beavers swim from the lodge to the bed and back carrying one stick each trip.

**Bubble runs.** On clear black ice in November and December, each swim leaves a line of white air bubbles frozen into the ice along the run. A lodge snare set on a run with a bubble line catches the passing beaver as written (95%). A snare on a run without one is passed only 30% of nights. Reading the bubbles is the skill.

**On screen.** In summer dusk, a beaver tows a green aspen branch across the pond, its wake a long V. Under the water, a player sees it swim past with its front paws tucked, trailing silver bubbles.

### Muskrat

**Push-ups are winter rooms.** From freeze-up, each muskrat pushes cattail, sedge and pondweed through a hole in the ice into a mound 30–60 cm tall: one push-up per 30 m of shoreline per muskrat. It feeds inside on roots and mussels and breathes there. Push-ups are `yearofice:muskrat_push_up` blocks.

- **Count them.** Push-ups on a pond show its muskrat count. A caved-in push-up means a mink or fox got in, or a trapper did.
- **Summer.** Muskrats build flat feeding platforms of cut cattail and leave mussel middens (Waters and fish). They dive for 1–3 game minutes and swim with the tail sculling, leaving a narrow wake.
- **Under ice.** A muskrat travels between push-ups using air trapped under the ice. A player at a push-up hole sees it pass as a dark shape trailing a bubble.

### River otter

**Otters play, and the play is a sign.** Otter families (a mother and 1–3 pups, April to October) slide 10–30 times at dusk down a mud or snow slide into the water. A player who watches a play session from 30 m or more gets Spirits +3, once a game day. The journal: "Watched the otters slide."

- **Hunting.** An otter chases fish at 1.5–3 m/s under water and dives for 30–120 s. A hunting otter adds +40 to W at its pool and sends schools into Fountain. Its fish stock rules stay as written.
- **Periscoping.** A curious otter stands up in the water, snorts and stares at the player for 3–8 s before diving. It is the otter's tell, and the closest look the player gets.
- **Winter.** Otters keep holes open (Waters and fish), travel between them under the ice and slide across snow between leads. Otter holes and otters surfacing in the player's holes are in Life under the ice.

### Mink

**Mink hunt the bank, not the open water.** A mink runs the water's edge, checking every hole under roots and every stone pile. It dives for 10–20 s at a time and takes sculpin, chub, frogs, muskrat kits and ducklings.

- **Surplus killing.** A mink that finds many prey at once kills more than it eats and caches the rest in a bank hole: 3–10 small fish or frogs. A cache found under the roots gives bait.
- **Tracks.** Bounding pairs of small prints along the edge, and a fish head on the ice by an open lead.
- **Theft.** Its raids on minnow traps, cribs and cleaning spots stay in Bestiary and Waters and fish.

### Loons and diving ducks

**Under the water.** This tab gives the diving birds only their under-water behaviour. The species, their land and air rules and their ids belong to the birds' tab; the ids below are the ones this tab assumes, and the birds' tab's ids win if they differ.

| Bird | Id | Dives for | Depth | Hunts | What it does to fish and the player |
| --- | --- | --- | --- | --- | --- |
| Common loon | `yearofice:common_loon` | 40–60 s | To 10 m | Cisco, stickleback, chub | Chases at 1.5 m/s with its feet; drives schools into a Ball. Surfaces 50–100 m away |
| Pacific loon | `yearofice:pacific_loon` | 30–45 s | To 5 m | Stickleback, fry | Tundra ponds over 1 m deep; its presence means fish (Waters and fish) |
| Common goldeneye | `yearofice:common_goldeneye` | 20–30 s | 1–4 m | Caddis, scuds, snails | Feeds on the bottom; winters at open leads in small numbers |
| Bufflehead | `yearofice:bufflehead` | 10–20 s | 1–2 m | Midge larvae, scuds | Small flocks in shallow ponds in May and September |
| Common merganser | `yearofice:common_merganser` | 20–40 s | 1–3 m | Fry, smolts, chub | Hunts in lines that herd fish; a line of mergansers means a school or the smolt night |

**Nets.** Loons drown in gill nets on 2% of net-days, as written; mergansers do on 1% of net-days in rivers. A drowned bird gives meat and feathers and costs Spirits −2.

## Water plants as habitat

Water plants are where fish hide, feed and spawn, and where water animals build. This section covers only that. What each plant gives to eat, brew or make is in Plants and fungi, which owns the plants and their ids; the ids below follow existing ones where they exist.

| Plant | Id | Where | Fish use it for | Animals use it for | Season |
| --- | --- | --- | --- | --- | --- |
| Pondweed | `yearofice:pond_weed` | Shelves 0.5–2 m in oxbows, beaver ponds, lakes | Pike ambush cover; stickleback nests; snails on the stems | Moose sodium in June–August; ducks; muskrat food | June to the first frost; dies back under the ice |
| Yellow pond-lily | `yearofice:lily_pad` | Still water 0.5–2 m | Shade; pike at the pad edge ×1.3 (exists); fry under the pads | Beaver and muskrat eat the rootstocks; frogs perch; dragonflies emerge on the stems | June 10 to frost |
| Sedges | `yearofice:sedge_mat`, flooded sedge state | Pond margins, bog edges, flooded flats | Pike spawning flats in May; blackfish breathing holes in winter | Frog egg sites; muskrat push-up material; duck nests | All year; flooded in May |
| Horsetail | `yearofice:water_horsetail` (Plants and fungi id) | Shallow lake margins, spring seeps | Grayling and chub fry nursery in the stems | Moose winter food at seeps (exists); muskrat food | Green at seeps all winter |
| Bur-reed | `yearofice:bur_reed` | Slow creek edges, beaver pond inlets | Coho parr cover in its ribbon leaves | Duck seeds in fall; muskrat lodges | June – October |
| Mare's tail | `yearofice:mares_tail` | Clear spring-fed water | Scud beds; cover for grayling in winter leads | Muskrat and moose food in spring leads | Green under open water all year |
| Buckbean | `yearofice:buckbean` | Bog pool edges as floating mats | Blackfish and stickleback cover | Frog egg sites | June – September |
| Cattail | The existing cattail block | Marsh and oxbow edges | Pike and chub cover in flooded stands | Muskrat food and push-ups | All year; stalks stand through winter |

**Weeds and winterkill.** Weed cover w from 0 to 1 per body now scales the oxygen demand c in the winterkill formula by (0.8 + 0.4 × w). At the default w of 0.5 the factor is 1.0, so the existing numbers hold. A weed-choked pond (w = 1) winterkills ×1.2 faster, and a player who rakes weeds out in September (2 game hours per 0.1 ha) lowers w by 0.2.

**Plants as signs.** A clear lane through lily pads points to a lodge (Ideas from viewers › Lily pads). Green mare's tail in January means a spring lead. A sedge flat underwater in May means pike.

## Watching, counting, keeping and stocking

Six interactive systems turn fish knowledge into play: seeing into water, watching fish, counting a run, keeping fish alive, stocking a pond and reading disease.

### Reading the water by sight

**Glare decides what the player sees.** Water reflects more of the sky the flatter the player looks across it. The client works out a glare G for each view ray that hits water, and a fish is visible only inside the remaining visibility.

```latex
R = 0.02 + 0.98\,(1 - \sin\varphi)^{5} \qquad G = \min(1,\ 2.5 \cdot R \cdot \ell \cdot k) \qquad d_{vis} = d_{type} \cdot (1 - G)
```

Here φ is the view ray's angle below the horizontal, ℓ the brightness of what the water reflects, k the player's gear factor and d\_type the type's visibility in Waters and fish. The reflection ray is cast up to 64 blocks to find ℓ.

| What the water reflects | ℓ |
| --- | --- |
| Dark spruce bank or cliff shadow | 0.1 |
| Grey cliff or birch | 0.3 |
| Clear blue sky | 0.6 |
| Overcast sky | 0.8 |
| Snow-covered bank | 0.9 |
| The sun's glitter (within 10° of the sun's mirror point) | 3.0 |

**What the numbers mean.** Looking 10° down across clear-sky water leaves 40% of the visibility; looking 30° down leaves 92%. Looking 10° down against a dark spruce bank leaves 90%. So the player sees fish by looking steeply, by standing high, and by looking where the far bank is dark.

**Gear and posture.** k multiplies glare.

| Gear or posture | k |
| --- | --- |
| Crouch-watching (shading the eyes) | 0.8 |
| A hat tagged `yearofice:brimmed` | 0.85 |
| Slit snow goggles (Clothing) | 0.7, with the view narrowed |
| J.O.'s sunglasses | 0.3 |

**J.O.'s sunglasses.** A pair of polarised aviator sunglasses lies in the glovebox of the 1979 wreck, `yearofice:pilot_sunglasses`. They cut water glare to a third and count as snow goggles for snow blindness without narrowing the view. There is one pair per world; a cracked lens (a fall on rock, 10%) makes them ×0.6.

**Standing high.** A 2 m cut bank or a tree's lower branch roughly doubles φ at the same distance. That is why the fish-watching log and the counting stand below are raised.

**Sun angle.** With the sun above 30° and the bed pale (gravel, sand), fish throw shadows on the bed offset by depth ÷ tan(sun elevation). The shadow is easier to see than the fish. At 64°N the sun stands at most 49° at midsummer, so shadows always fall a little to the side. Over mud or weed, shadows do not show. The crouch-watch teaches it once: "Look for the shadow, not the fish."

**Glare also helps.** Across glare, rise rings and nervous water show best, because they break the reflection. A player who wants rises looks across the water; one who wants fish looks down into it.

### Watching fish from a bank or a log

**Sit and watch.** Near water, the Z radial gains "Watch the water". The player sits and the clock runs as a time-lapse at the hold-to-work rate. Every 10 game minutes of watching gives one observation about the nearest spot within 30 m, chosen from what the player does not know yet.

| Observation | Example messenger line |
| --- | --- |
| Species present and a count band | "About 20 grayling and a few whitefish." |
| The biggest fish and its lie | "The big one holds at the head, by the rock. About 0.9 kg." |
| What they are taking | "They're on the small grey mayflies." |
| Their hours | "They feed hardest from noon to four." |
| Their stage | "Kelts. Thin and dark." |
| A predator or rival | "An otter works this pool at dusk." |

**Better seats.** A fallen log over the water or a cut bank over 1.5 m is a fish-watching seat. Observations come every 6 game minutes, and the glare model raises the eye 1.5 m. A seat the player builds (2 logs pinned over a pool, Bushcraft, 1 game hour) is `yearofice:watching_log`.

**Costs.** Mosquito pressure applies while sitting in summer and the cold clock in winter. A seated watcher adds no wariness until standing.

**The species page.** Each species has six facts in the record book: where, when, food, lie, spawn and sign. All six known gives Spirits +2, and from then on the crouch-watch adds a count band to that species' clue lines. The player becomes a naturalist, which suits Station Nine.

### Counting fish at a weir

**Count a run to know it.** A counting weir is a picket fence across a clear creek with a 0.6 m gate. A pale flash board lies on the bed below the gate, and a 2 m counting stand stands on the bank.

| Piece | Id | Built from | Station, time, kcal | Tier |
| --- | --- | --- | --- | --- |
| Counting weir | `yearofice:counting_weir` | 2 poles and 8 pickets per metre of creek, 4 m cordage per metre | Hatchet, 1 game hour per metre in knee water, 100 kcal per metre | Bushcraft |
| Flash board | `yearofice:flash_board` | 4 bark sheets and 6 stones, or 1 quartz slab | Hands, 20 min, 33 | Start |
| Counting stand | `yearofice:counting_stand` | 4 poles, 2 logs, 4 m cordage | Hatchet, 2 h, 200 | Bushcraft |

**Counting.** From the stand, the player taps the use key each time a fish crosses the flash board during a count session of 20 game minutes, on the close clock. Fish cross singly or in bunches; a missed or double tap counts as an error. The count expands to an estimate of the whole run from the run's triangle and the hour.

```latex
\hat{R} = \frac{c_{session}}{p(t)} \cdot (1 \pm \epsilon) \qquad \epsilon = 0.3 - 0.025 \cdot \min(10,\ n_{sessions})
```

Here p(t) is the share of the run that passes in that session's 20 minutes. After 10 sessions the estimate is within ±5%.

**What it gives.** The messenger shows the run's size and the player's share: "Coho at Discovery Creek: about 380 fish, near normal. You have taken 54 (14%)." That makes the 20% echo threshold visible before the warning. Station Nine's shelf holds a blank escapement sheet; a full season's count fills it, gives Spirits +5, and Pete Lund comments on the radio the next evening.

**The gate.** Open, the weir only counts. Closed, it holds the run below it, and fish stack in the pool as a hand-grab spot at q 0.8. Every fish held counts toward h, as in the sockeye outlet rule, and bears find it after 24 game hours. A closed gate is a trap, and the game treats it as one.

### Keeping fish alive

**Vitality.** Every live fish item carries vitality from 100 to 0 in its `yearofice:fish_data` component. It falls at the container's rate for that fish, and the fish dies at 0. The keeping times in Fishing in depth and Waters and fish are the data; this gives the rule between them.

| Container | Grayling, salmon, char, cisco, stickleback | Pike, burbot, whitefish, sucker, sculpin | Lake chub, blackfish, trout-perch |
| --- | --- | --- | --- |
| Air, in the hand or pack | 15 min | 1 h (the existing sculpin rule) | 1 h |
| Wet moss | 1 h | 2 h | 3 h (existing chub) |
| Gill net | 2 h (existing) | 2 h | 2 h |
| Birch-bark bucket, water unchanged | 2 h (existing stickleback) | 12 h | 3 days (existing chub); 7 days for blackfish (existing) |
| Bucket changed hourly | 6 h (existing stickleback) | 24 h | As unchanged |
| Stocking bucket, changed every 2 h | 6 h (existing) | 12 h | 3 days |
| Stringer in water | 12 h (existing) | 12 h | 12 h |
| Trap basket in water | 24 h (existing) | 24 h | 24 h |
| Live-well box in current | 48 h | 72 h | 7 days |
| Live crib | 7 days, losing 2% kcal a day (existing) | 7 days | Forever (existing chub) |

**Modifiers.** Water above 15 °C halves the remaining time in a bucket or live-well. More fish than a container's capacity divide its time by the overload. A fish handled with dry hands loses 20 vitality, and wet hands lose none.

**Death and the meat clock.** A fish that dies slowly in a container misses the bleed bonus in Handling the catch, and spoils from the moment it dies. A fish clubbed while lively (vitality over 60) counts as bled at once. So keeping fish alive is for bait, stocking and release, not for better meat.

**Two new keepers.**

| Item | Id | What it does | Made from | Station, time, kcal | Tier |
| --- | --- | --- | --- | --- | --- |
| Live-well box | `yearofice:live_well_box` | A 0.6 × 0.4 × 0.4 m holed spruce box with a lid, tied in current; holds 6 fish or 30 bait fish; carried at 3 kg | Split spruce boards from 1 log, 2 m root lashing | Workbench, 2 h, 200 | Bushcraft |
| Canoe live-well | `yearofice:canoe_live_well` | A 20 L bark tub lashed in the canoe; holds 4 fish at bucket rates; a 10 s RMB hold on the tub with the paddle shipped changes the water | 3 bark sheets, 2 m root lashing, 1 glue | Hands, 1 h, 100 | Bushcraft |

**Mesh scoop.** `yearofice:mesh_scoop`, a 0.3 m willow hoop with fine nettle mesh on a 1 m handle, made by hand in 30 game minutes for 50 kcal at the Start tier. It takes nymphs, scuds, shrimp, fry, stickleback and sculpin, and it is the kick-net.

### Stocking a pond

**Stocking extends Habitat work.** The rules there stand: 6 or more live fish of one species into a body of a suitable type; char only into tarns; blackfish only into ponds and bogs. These rules join them.

1. **Oxygen.** The messenger checks the body's lowest winter oxygen from last winter, or from the model if the player has not seen one. If it falls under the species' tolerance, it warns once: "This pond will winterkill grayling."
2. **Spawning ground.** A stocked population grows by its r only where its spawning ground exists. Grayling and Dolly Varden need an inflowing gravel creek, pike a sedge flat or weed shelf, char a gravel shoal; chub, stickleback and blackfish spawn anywhere. Without it, the fish live 5–15 years and age out: a put-and-take pond.
3. **Predators.** Pike stocked with other fish eat them. Each month, each prey species' N falls by 3 × N\_pike × N/(N + 10), stickleback and blackfish in sedge excepted. Six pike in a chub pond leave the chub under 10% of K within 2 years.
4. **Pike stay.** Once a stocked pike population reaches 6 breeding fish, it cannot be fished out with rod and net alone; only a winterkill or a drained beaver pond ends it. The messenger warns once before the release: "Pike won't come out of a pond once they're in."
5. **Escape.** A stocked body with an outlet creek sends 10% of each year's growth downstream. Pike stocked above a grayling creek reach its mouth in 2 years and its grayling K there falls 30%.
6. **Frogs and shrimp.** Stocking ends a pond's frog chorus and fairy shrimp (New water life).
7. **Limits.** Two stockings per body per year. Fish from a body with a disease tell carry it.

**The ambition.** "Stocked water": a stocked pond that holds a fishable stock (N over 0.5 K) three years later. It gives +10 Spirits and joins the fishing ambitions in Waters and fish.

### Disease and winterkill tells

**Most fish problems show before they matter.** Each tell is something the player sees, smells or finds while cleaning a fish.

| Problem | Where and when | The tell | Effect |
| --- | --- | --- | --- |
| Black spot | Chub, sucker, grayling in ponds and slow water with snails and kingfishers | Black specks in the skin and fins | Harmless when cooked; the journal explains once |
| Water mould | Spawned-out salmon (exists), and any fish released with dry hands, 30% | Cotton-white patches | A released fish with it dies in 10 game days; on a dead fish, not food |
| Ichthyophonus | Chinook in years with river water over 16 °C for 5 days in July: 10% of the run | Fish that fail to jump at the falls; white flecks in the flesh when filleted | kcal ×0.9; smoked strips crumble (×0.8 yield); not a human risk |
| Tapeworm cysts | Pike, burbot, whitefish; 20% of fish | Small white cysts in the flesh, seen on filleting at Practiced | Eating raw or lightly smoked risks it, unless frozen below −20 °C for 7 days (Handling the catch) |
| Low oxygen, winter | Weedy still water, February – April | Snow-white ice over a weed pond; the hole smells of rotten eggs under 3 mg/L; fish gasping at the hole | The winterkill model in Waters and fish |
| Low oxygen, summer | Hot-spring mixing pools and tundra ponds over 20 °C on calm afternoons | Fish nosing at the surface at dawn | Grayling leave the pool for 2 game days |

**Reading a hole.** Chiseling a hole now gives a smell line from dissolved oxygen. Over 6 mg/L there is none; at 3–6, "The water smells flat"; under 3, "The water stinks of rotten eggs." It is the cheapest way to know a pond is dying before the fish do.

## Data and build

All of this builds on the Minecraft engine plan: NeoForge 1.21.1 (RECONCILE 118), mod id `yearofice`, Java packages under `com.yearofice` by the lead's map, GeckoLib for every fish and water animal model. Stocks stay numbers in saved data; fish entities are projections near the player.

### Registry ids

| Kind | Ids |
| --- | --- |
| New fish entities and items (same id) | `yearofice:pygmy_whitefish`, `yearofice:trout_perch` |
| School and ambient entities | `yearofice:fish_school`, `yearofice:tadpole_school`, `yearofice:boreal_toad`, `yearofice:diving_beetle` |
| Client particles | `yearofice:hatch_mayfly_grey`, `hatch_mayfly_pale`, `hatch_caddis`, `hatch_stonefly`, `hatch_midge`, `water_strider`, `rise_ring`, `v_wake`, `boil`, `flank_flash`, `bubble_trail` |
| Items | `yearofice:scud`, `pond_snail`, `fairy_shrimp`, `mayfly_nymph`, `lamprey_larva`, `frog_spawn` (item and block), `mesh_scoop`, `live_well_box`, `canoe_live_well`, `pilot_sunglasses`, `two_tine_fork` |
| Blocks and block entities | `yearofice:redd_gravel` (state `eggs` 0–3), `frog_spawn`, `muskrat_push_up`, `beaver_feed_bed`, `bubble_ice`, `counting_weir`, `flash_board`, `counting_stand`, `watching_log`; flooded state `flooded` on the existing sedge block |
| Data components | `yearofice:fish_data`, `yearofice:fly_pattern` |
| Water plants (owned by Plants and fungi) | `yearofice:bur_reed`, `mares_tail`, `buckbean`, with the existing `pond_weed`, `lily_pad`, `sedge_mat`, `water_horsetail` |
| Diving birds (owned by the birds' tab) | `yearofice:common_goldeneye`, `bufflehead`, `common_merganser`, with the existing `common_loon`, `pacific_loon` |
| Sounds | `yearofice:water.rise_plip`, `water.boil`, `water.tail_slap`, `water.otter_snort`, `water.frog_chorus`, `water.toad_peep`, `water.beetle_tick`, `water.midge_hum`, `water.sucker_splash`, `water.flare` |
| Spot types | `yearofice:sedge_flat`, `yearofice:burbot_spawning_ground` |

### Data-pack paths

- `data/yearofice/fish/<id>.json` gains `hitbox`, `rig`, `form` (individual or school), `stages`, `holds` by season, `lie` weights, `wariness_x`, `torpor` (speed, a\_ice), `spawn_behavior`, `keep_class`, `tickle` and `verbs`.
- `data/yearofice/hatch/<id>.json`: window, water types, hours, peak H, weather factor, risers, pattern.
- `data/yearofice/water_life/<id>.json`: invertebrates, amphibians and their rules (habitat, season, collection rate, bait multipliers, keep days).
- `data/yearofice/keep_container/<id>.json`: vitality hours by keep class, capacity and modifiers.
- `data/yearofice/fish_disease/<id>.json`: hosts, conditions, tell, effects.
- `data/yearofice/stocking/<species>.json`: oxygen tolerance, spawning ground tags, predator rule.
- `data/yearofice/recipe/` for the scoop, live-wells, counting weir pieces and watching log; `data/yearofice/tags/block/fish_spawning_gravel.json`, `sedge_flat_plants.json`, `brimmed` under `tags/item/`.

```json
{
  "id": "arctic_grayling",
  "hitbox": { "width": 0.25, "height": 0.12 },
  "rig": "salmonid",
  "form": "individual",
  "school_stage": "fry",
  "stages": ["fry", "adult", "spawner", "kelt"],
  "holds": { "07-08": ["seam", "pool_below_falls", "lake_outlet"], "11-04": ["deep_river_hole", "spring_lead"] },
  "lie": { "cover_bonus": 0.5, "min_depth_m": 0.4 },
  "wariness_x": 1.0,
  "torpor": { "speed": 0.5, "a_ice": 0.8, "spring_lead": { "speed": 1.0, "a_ice": 1.0 } },
  "surface_feeder": true,
  "spawn_behavior": { "sequence": ["territory", "display_sail", "quiver"], "site": "creek_mouth", "from": "breakup", "to": "05-31" },
  "kelt_days": 30,
  "keep_class": "delicate",
  "tickle": true,
  "verbs": ["spot", "stalk", "cast", "net", "spear", "trap", "grab", "tickle", "release", "transplant"]
}
```

### Java classes

| Class | Job |
| --- | --- |
| `ecosystem.fish.ai.FishMind`, `FishActivity`, `LieField` | The fish state machine at 10 Hz and 5 Hz; lie scores per spot from the current attachment, rebuilt when the terrain-change queue touches the spot |
| `ecosystem.fish.FishProjector`, `FishSchoolEntity` | Projection from stocks with body and player caps; the school entity, drawn by the client `client.water.SchoolRenderer` with boids |
| `hunt.fishing.HookedFishController` | The bridge from the fight (`sim.fishing.FightModel`) to the entity (the lead's package map) |
| `ecosystem.fish.FishData`, `Vitality` | The item component codec and the keep model |
| `ecosystem.fish.HatchCalendar`, `Spawning`, `SalmonYoung`, `WinterCarcasses` | Named hatches and patterns; spawn sequences, redds, kelts, sedge flats and the burbot ground; fry, parr, the coho beaver factor and the smolt night; frozen carcasses |
| `ecosystem.fish.FishWatch`, `CountingWeir`, `Stocking`, `FishDisease` | Observations and species pages; count sessions and the estimate; stocking rules; disease tells |
| `client.water.GlareModel` | Glare per ray, the reflection raycast and gear factors; client only |
| `ecosystem.waterlife.*` | Invertebrates, frog breeding, the toad, beaver and muskrat water goals, otter play, mink banking, bubble ice |

All packages sit under `com.yearofice` as Minecraft development › Package and module map lists them (RECONCILE 32, 80).

**Server and client.** The fish mind, projection, vitality, hatches and counting run on the server. Boids, glare, hatch particles, striders and rise rings run on the client from synced state. A school costs one entity and 40 bytes of sync a second.

### Milestones

| Milestone | Ships |
| --- | --- |
| M5 | Fish projection and the fish mind, lies and lanes, schools, the hooked-fish bridge, `fish_data` and vitality, the mesh scoop and buckets, the glare model, tickling, the hatch calendar and fly patterns, scuds and snails |
| M6 | Beaver, muskrat, otter and mink water goals, diving birds under water, frog breeding, striders and beetles, fish watching and species pages |
| M7 | Spawning sequences, kelts, salmon young and the smolt night, the sedge flat, the sucker run, the burbot ball, sculpin nests and ammocoetes, winter carcasses, torpor, bubble ice, disease tells, pygmy whitefish and trout-perch, the boreal toad |
| M8 | Counting weir and stand, live-well box and canoe live-well, watching log, J.O.'s sunglasses |
| M9 | Stocking rules and the "Stocked water" ambition, the coho beaver factor, weed raking |

### GameTests

Each is a NeoForge `@GameTest` in `gametest.WaterLifeTests`, named `yearofice:ecosystem/<test>` (Minecraft development › Conflicting ids, row 22) and batched by milestone, using headless saved-data runs where a test spans game months.

1. **Caps and cost.** Load a q 0.9 falls pool with 50 grayling and a chum school, plus two 2 ha ponds, around one player. There are never more than 12 individuals within 32 blocks or 4 schools in one body, and the fish share of MSPT stays under 1.5 ms over 6,000 ticks.
2. **Pecking order.** In a test pool with one seam, the heaviest of 8 grayling holds the top lie in at least 90% of 200 trials. With it removed, the next heaviest takes the lie within 10 game minutes in at least 90%.
3. **Hatch rises.** With 30 grayling at a spot, rise rings average 9 ±1 a game minute at H = 1 and 3 ±0.5 at H = 0; m\_hatch at H = 1 equals the existing ×1.5 to 0.01.
4. **Glare.** At φ = 10° under clear sky with k = 1, d\_vis is 40 ±2% of d\_type; against a dark spruce bank 90 ±2%; with J.O.'s sunglasses under clear sky 82 ±2%.
5. **Vitality.** A grayling on a stringer dies at 12 ±0.25 game hours. Stickleback in a bucket die at 2 ±0.1 h unchanged and 6 ±0.2 h changed hourly. Chub live 3 days in a bucket and indefinitely in a crib.
6. **Burbot ball and stocking.** At a burbot spawning ground, the mean bite rate on February 10 nights is 2.8 ±0.2 times that ground's January 10 rate. Six pike stocked into a pond with chub at K = 50 leave chub under 5 within 24 game months on at least 9 of 10 seeds. A fishless pond of 60 frogs stocked with grayling falls silent within 2 springs.
