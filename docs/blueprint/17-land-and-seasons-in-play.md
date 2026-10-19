# Land and seasons in play

The valley's sky, ice, slopes and forest change by rules the player can learn, predict and use. This tab adds 38 environment mechanics, from ice fog and frost quakes to stand ages, squirrel larders and snow houses, each with a trigger, numbers, counterplay and a tell. It builds on Terrain and hydrology, Calendar, daylight and weather, Foraging and plant knowledge, Danger in detail and the world responses in The open world, and never restates them.

## How this tab fits

**Already specified, not repeated.** The valley inversion, overflow, the March crust, winter thaws, freeze-up and breakup stages, ice jams and spring floods live in Calendar, daylight and weather. Thin ice, lake booms, whiteouts and the snow trench live in Danger in detail. Regrowth, burn recovery, the 0.5% deadwood rule and beaver dams live in The open world. Wildfire, earthquake and the aurora storm live in Open world in depth.

**Owned by another tab.** Water-driven terrain change (bank erosion, gravel bars, channel shifts, silt, aufeis build-up, beaver flooding, thaw ponds and slumps, falls cutting back) and how water draws animals: see Water, calls and encounters.

**What this tab adds.**

- Two learnable skills: Land reading and Sky lore, with 12 sky signs and the fireweed clock.
- 6 sky and air phenomena, 4 ice phenomena and 5 ground and slope hazards.
- The forest as a system: stand age, spruce beetle, deadwood decay, mushroom flushes, berry and cone swings, lichen.
- 11 new plants and fungi and 3 new dangerous lookalikes.
- 12 ways to use the land as a tool.
- Micro-climates for every block column, and crop farming in play: beds, soil, germination, growth, frost, pests and seed.

**One format.** Every mechanic names its trigger, its numbers, its effect, the counterplay and its tell. Tells come through three channels: the world (blocks, particles, sky), sound, and the messenger. The messenger reports only what the player could know, by the knowledge gate in The open world.

## Reading the land and sky

The land already shows its rules (pillar 5, Everything readable). These two skills make reading itself something the player gets better at. They never add objective markers.

### Two new skills

Both join the Practice table in Crafting and building in depth and follow its five rules: reps capped per day, no decay, and the words Clumsy, Handy and Practiced.

| Skill | One rep is | N | Clumsy | Handy | Practiced |
| --- | --- | --- | --- | --- | --- |
| Land reading | A reading of a feature type not yet read in this 64-block reading cell; a confirmed prediction counts 2 | 40 | One plain line, right 65% of the time | Right 85%; adds a number | Right 97%; adds the cause and a time |
| Sky lore | An evening sky reading whose next day the player lives through | 30 | Notices the strongest sign only; right 55% | Notices 3 signs; right 70% | Notices all signs; right 80%, equal to the messenger's tomorrow |

**The Read key.** A new key, X by default, reads what the crosshair rests on; R stays quick eat (Minecraft development › Input and keybinds). It takes 3 real seconds and 2 game minutes, and writes one line to the notebook's Spots tab. Holding X for 1 more second pins the line on the messenger map.

| Level | Sample line on a slope |
| --- | --- |
| Clumsy | "Snow looks deep here." |
| Handy | "Lee slope, about 35°. Deep new snow." |
| Practiced | "Lee slope, 35°, 40 cm new snow on sugar snow. It can slide for 2 days." |

**What it can read.** Twelve feature types: aspect and slope, micro-climate class, snowpack, avalanche start zone, rockfall fan, frost-heave ground, mud, stand age, beetle stage, bloom state of a patch, mushroom ground, and track age in hoarfrost.

**Wrong readings stay wrong.** A Clumsy line can be mistaken, and the world simply behaves otherwise. One exception protects the player: ice is never read by sight. A Read on ice always says "Can't judge ice by eye. Test it.", so the tap test in Danger in detail stays the only ice reading.

**Confirmation.** A prediction is confirmed when the player is within 100 m as it comes true: a slope slides, the hollow frosts, a patch ripens. The journal logs it: "The hollow froze tonight, as you read it."

### Sky signs

**Signs are honest odds.** Each evening, each sign appears with the chance `sky_signs.json` gives it for tomorrow's real state in the year table. The sky never lies more than its table says. Sky lore decides how many signs the player notices and how well they are combined.

The ring round the sun or moon and mare's tails stay as written in Calendar, daylight and weather. These are new:

| Sign | Shows when | What it predicts | Hit rate |
| --- | --- | --- | --- |
| Sun dogs: bright spots 22° beside a low sun | Clear, air below −15 °C, sun under 20° high | No rise above today's high for 24 h | 80% |
| Light pillars over lamps and chimneys | Calm, below −20 °C, ice crystals in the air | Calm, clear and colder tonight; an inversion forms | 85% |
| Chimney smoke sinks and spreads flat | Any season | A front within 12 h when the barometer falls; otherwise an inversion | 70% |
| Smoke rises straight | Calm, high pressure | Clear tomorrow | 70% |
| Red west sky at sunset | Summer, clear western horizon | Dry tomorrow | 70% |
| Cloud thickens from the southwest after a halo | Any | Wet within 12 h | 75% |
| A cap cloud sitting on Twin Horn | Any | Wind over 10 m/s at Windgap Pass tomorrow; the warm wind 30% of the time in winter | 75% |
| Swallows and ravens flying low; bugs biting hard | June–August | Wet within 12 h | 60% |
| Geese low and loud, heading south | September | Snow within 2 days | 60% |
| Far sounds carry: falls heard 2× farther | Any | Humid air before rain, or an inversion | 65% |
| Snow squeaks underfoot | Air below −10 °C | Air temperature, not weather (below) | — |
| Fireweed at the top of its stalk | July–August | First fall frost (below) | 80% |

**The sound thermometer.** Snow squeaks below −10 °C and squeals below −25 °C. Breath crackles as it freezes below −40 °C. A Handy Sky lore reader estimates air temperature to ±5 °C by sound; Practiced, to ±3 °C, with the messenger off.

**The fireweed clock.** Fireweed opens its flowers from the bottom of the stalk up, from about July 1. Each stalk has 12 flower rings, and each opens after 40 degree-days above 5 °C. When the top ring opens, the first fall frost comes within 10 days in 80% of years. When the stalks turn to cotton, snow that stays is 25–40 days away. The lookout's log carries the rhyme: "When fireweed turns to cotton, summer is soon forgotten."

**Stars and north.** From August 15 to April 30, Polaris stands 64° above the true north horizon on clear nights. A Read on it gives a true bearing, ±1°, with no compass. The messenger then shows a north tick in the view for 10 game minutes.

## Sky and air

### Aurora nights

**Each clear night from September to April draws an aurora index of 0–9.** The mean is 2.5; an aurora storm night draws 7–9. Index 2 or more is visible. These hooks touch only information, so the aurora storm still changes no survival number.

| Index | Radio | Compass | Messenger GPS | Other |
| --- | --- | --- | --- | --- |
| 0–4 | Normal | Steady | ±3 m | — |
| 5–6 | The 19:00 sked fails 40% of the time: static, then nothing | Swings ±5° | ±10 m breadcrumbs | Wolves howl 2× as often |
| 7–9 | No contact all night | Swings ±10° | ±20 m, and 1 in 10 breadcrumbs lost | The arc reads as a bearing (below) |

**The arc as a bearing.** At 64°N the arc lies along magnetic east–west. Its lowest bright point marks magnetic north to ±10°, so the sky works when the compass wanders. Polaris is better, ±1°, when it shows.

**Aurora logs count.** A radio log line "aurora index {n}" counts as one weather log for the season's supply drop. Dana Reyes replies on a static night: "Lot of noise on your end. Your sky must be busy."

**The crackle.** On index 8 or more, a still player outdoors has a 10% chance each night to hear a faint crackle overhead. The journal records it and explains nothing.

### Ice fog

**Trigger.** Air below −35 °C, wind under 1 m/s, and a water source: spring cells, open water below falls, the hot spring outflow, a chiseled hole, or a chimney. A stove cabin burning 1 or more armloads an hour makes its own fog bubble 60 m wide.

**Effects.**

- Sight inside the fog is 30 m. Animal sight ranges fall to 40% too, so the moose yard below the hot spring can be stalked to 30 m.
- Frost settles on clothing: the outer layer gains 5% wetness per game hour outside.
- Scent lingers twice as long (48 game hours instead of 24), but its cone is half as long.
- From any point 50 m or more above the floor, the fog lies as a white lake over every open lead, spring and chimney in sight. A Read from above names them: ice fog maps the thin ice.

**Tell.** Diamond-dust particles that glitter in the headlamp; light pillars; sound carries far through it. Messenger: "−38 °C, calm. Ice fog over open water."

### Flat light

**Trigger.** A Cloudy or Snow day, snow of 10 cm or more, and open ground wider than 100 m: lake ice, tundra, gravel bars, burns. Shadows vanish. The renderer drops block shading on snow, so drifts, bank edges, cornices and pressure ridges disappear.

**Effects.** Walking off an unseen edge uses the fall rules in Danger in detail. A loaded sled tips on an unseen drift 5% per 100 m; reloading takes 5 game minutes.

**Counterplay.** Throw something dark ahead, such as a spruce bough or a mitten (Q, then pick it up): the snow shows where it lands. Plant trail wands, 1 m willow sticks every 20 m, which read at 40 m in flat light and 15 m in a whiteout. A wand costs 1 game minute to cut and place.

### The cold lid

The inversion's temperatures stay as in Calendar, daylight and weather. This adds what the lid does to smoke, scent and sound.

**Trigger.** The inversion is on: a clear winter night, wind under 2 m/s. It lifts when wind passes 3 m/s or the day's high rises 5 °C above the night's low. In December and January it can hold for days.

| Under the lid | Effect |
| --- | --- |
| Smoke | Rises 20–60 m, then spreads flat across the valley as a grey ceiling |
| Scent on the floor | Drains down-valley at 0.5 m/s; cone length ×2 for animals on the floor |
| Scent from a bench | Cone length ×0.5; game on the floor rarely smells a bench camp |
| Sound on the floor | Every sound's range ×2: chopping, the falls, wolves |

**Counterplay.** Hunt from the benches looking down. Split wood on the flats only once the lid lifts. A cabin on the flats is smelled by everything 160 m downstream.

### The warm wind

**Arvid called it the gap wind.** A dry downslope wind pours off the north range 1–3 times a winter, from November to March. Each lasts 12–48 game hours and is drawn in the year table. The forecast flags it on day 3 at 60%, like a storm.

**Numbers.** Air rises 15–25 °C within 6 game hours, to at most +5 °C. Wind runs 8–15 m/s with gusts to 20. Snow shrinks 15% a day on open ground and 30% on south slopes.

**Effects.**

- Spruce shed their rime: under trees, a snow bomb lands within 3 m once a game minute, adding 3% wetness on a hit.
- Frozen caches that pass 0 °C start their spoilage clocks, with the warning in Preserving in detail.
- Overflow doubles for 7 days after.
- Snow slides off roofs steeper than 35°, losing their snow insulation.
- Avalanche hazard on lee slopes gets +2 (Ground and slopes).
- The first night below −10 °C afterward sets a wind crust. It holds a walking player without snowshoes for 3 days, in any month, like the March crust.
- The first warm wind of a winter lifts Spirits by 5.

**Tell.** The cap cloud on Twin Horn the day before; a roar on the ridges heard 3 km off; dripping eaves in January. Messenger: "Warm wind tomorrow. Up to +4 °C, gusts 20 m/s."

### Hoarfrost

**Surface hoar.** On clear calm nights below −15 °C, feathery crystals 5–20 mm long grow on snow and twigs by dawn. They form from about 02:00.

**Track clocks.** A track laid after the hoar formed is crisp and cuts through the crystals. A track laid before has crystals grown inside it. A Handy Land reader dates a track this way: "Made after 02:00 today." A Clumsy one sees only "fresh or old".

**Buried hoar.** Surface hoar covered by new snow becomes a weak layer for 30 days: +1 avalanche hazard on that slope.

**Depth hoar.** When snow is under 30 cm on December 1 and the month runs colder than normal, the base turns to sugar snow. Every slope of 30–45° keeps +1 avalanche hazard all winter. Sugar snow also digs 30% faster, which helps a snow cave.

## Ice

### Frost quakes

**Trigger.** Air drops 15 °C or more within 12 game hours to below −20 °C, with snow under 15 cm and ground wet from rain or a thaw in the last 5 days. A qualifying night gives 0–4 booms between 22:00 and 06:00.

**Effects.** Each boom opens a crack 5–30 m long and 2–5 cm wide in open ground or pond ice. A wall within 10 m of the crack loses 0.5 m of chinking. Animals within 1 km flush 200–400 m, as from a falling tree. A sleeping player wakes.

**Tell.** A gunshot crack with no shaking. The messenger reports earthquakes only, so it stays silent. The journal writes: "A crack like a rifle at 03:10. No quake reported."

**Counterplay.** None is needed. It is a scare and a sign: the ground is wet and freezing hard, so frost heave follows that winter.

### Pressure ridges

**Where.** Lakes over 1 km long, once ice is 20 cm thick: Big Lake and the lake valley's lake.

**Trigger.** A day-to-day swing of 12 °C or more. Each event adds or grows 1 ridge per km² of lake, along a seeded line from shore to shore or island to shore.

**Numbers.** Ridges stand 0.3–2 m high in tilted slabs. A lead 2 m wide on each side refreezes thin: 5–10 cm for 3–7 days after the event.

**Effects.**

- A loaded sled cannot cross a ridge over 1 m. Relay the load (2 game minutes per 20 kg) or chisel a gap (30 game minutes per metre of height).
- The refrozen lead is the cheapest hole on the lake: chiseling takes 20% of the normal time. Set lines along a fresh lead are the fastest way to fish a big lake.
- Snow hides the thin lead. The tap test finds it.

**Tell.** A jumble of blue slabs; groans at dusk as the ice contracts; a lead that shows as a grey line in fresh snow.

### Anchor ice

**Trigger.** Clear nights below −15 °C during the slush-running stage, wind under 3 m/s, in riffles under 1 m deep.

**Effects.**

- Ice grows on the stony bed overnight and lifts off between 10:00 and 14:00, carrying gravel and stones.
- Fish traps, weirs and set lines in a riffle freeze in. Each night they are lost 30% of the time, rafted away to the next snag, where they can be found.
- Grayling and whitefish leave the riffles. Bite chance in the pools below doubles and in the riffles falls to 0 for those days.

**Tell.** The riffle bed turns white-grey; at midday, ice rafts drift past with stones on top; a soft hiss.

### Candle ice

**Trigger.** Rotting lake ice in sunny weather becomes candle ice after 7 days. It is the grey ice of Reading ice in Danger in detail and still counts as a quarter of its thickness.

**Effects.** Wind over 6 m/s pushes candled pans ashore, where they collapse into heaps 1–2 m high along the downwind shore. A heap in shade keeps food at 0–2 °C for 5–10 days: a May icebox for the first catch of open water.

**Tell.** The heaps chime like breaking glass, heard 500 m off. Candles lift out of the ice by hand, 20–40 cm long.

## Ground and slopes

### Mud season

**Trigger.** The 20 days after a cell melts out. The top 10–30 cm thaws over frozen ground.

| Ground | Normal factor | In mud season |
| --- | --- | --- |
| Open ground | 1.0 | 0.75 |
| Worn trail | 1.2 | 0.9 |
| Bog | 0.6 | 0.5, and boots get wet |
| South slope | 1.0 | 1.0 after its first 7 days |

**Effects.** Each pass on a worn path counts double for wear. Tracks print in mud everywhere and last 3 days, so May is a second tracking season after snow. Sleds and the wolf sled cannot be used.

**Tell.** Shiny ground and sucking footsteps. Messenger: "Ground thawing. Mud for about 2 weeks."

### Frost heave and pingos

**Frost heave.** Wet silt, bog and permafrost cells heave each winter. A post or foundation set in them rises 2–5 cm, unevenly.

Each spring, a building on heave ground rolls once:

- 30%: the door jams. It cannot be barred until planed (1 game hour), which matters in the siege rule in Danger in detail.
- 1 m of chinking opens.
- 10%: the stovepipe joint leaks 5 smoke points a game minute until refitted.

**Counterplay.** Build on gravel, bedrock or a bench of dry sand, which never heave. Or lay a gravel pad 0.5 m deep (about 1 t per 4 m², hauled by sled), or set sill logs on stone footings. A Land reading names heave ground: "Wet silt. Posts here will heave."

**Pingos.** Slough Country and Cloudberry Muskeg each hold 0–2 pingos, on old drained lake basins. A pingo is an earth dome 8–25 m high and 50–150 m wide over a core of ice.

- **A lookout in flat country.** Its top reveals 500 m of the messenger map.
- **Dry ground.** It carries birch and the only dry campsite for kilometres. Foxes den on 40% of pingos, with kits in June.
- **A natural freezer.** 30% have a summit crater that shows the ice core. A niche cut into it holds 200 kg of food at −1 °C all summer.
- **A landmark.** Pingo joins the natural landmark table. It passes the 2 km test above black spruce 3–5 m tall.

### Rockfall

**Where.** Rock faces: slopes over 40° and taller than 6 m.

**Trigger.** Any of three conditions:

- A day above 0 °C after a night below −2 °C, mostly in April–May and September–October.
- The day after rain over 5 mm, as the Sheep Crags hazard says.
- The warm wind.

Each face has a 4% chance per trigger day to drop 3–20 stones of 5–50 kg. They fan out to half the face height from its foot.

**Hit.** A player in the fan is hit 30% of the time: −20 Condition, a 30% chance of a broken bone, 10% of being knocked out for 30 game minutes.

**Tell.** A click-clack trickle of pebbles 2–3 s before the big stones, and a dust puff. Stones lying on top of snow mean rockfall since the last snowfall. This cue joins the danger sound table.

**Counterplay.** Work under faces before 10:00, while they are frozen solid. Camp under a face only in a dug-in overhang, which takes no hits.

**Gives.** Fresh angular stone for builds. On an iron face, 5% of falls expose a new nodule, a small third source of renewing iron.

### Small avalanches

**Where.** Open slopes of 30–45° with snow over 50 cm, and fewer than 1 tree per 25 m². That means the north range, the canyon walls, lee sides of ridges and tall cut banks.

**Hazard.** Each start-zone slope scores H once a day:

```latex
H = h_{angle} + 2 \cdot [snow_{48h} > 30\,cm] + 2 \cdot [thaw \lor warmwind] + [depth\ hoar] + [buried\ hoar] + [lee\ loaded]
```

Here h\_angle is 1 at 30–34°, 2 at 35–40° and 1 at 41–45°. At H of 4 or more the slope releases on its own 10% of days. At H of 3 or more, each player crossing of the start zone triggers it 20% of the time.

**Size.** A path 20–150 m long and 10–40 m wide, debris 1–2 m deep.

**Caught.** The player is carried 10–60 m and buried 0.3–1.5 m deep. The close clock runs, and there are 15 game minutes of air.

1. Up to 0.5 m deep, the player digs out: hold right mouse for 2 game minutes.
2. At 0.5–1 m, self-rescue works 50% of the time, and only if a hand was raised (Space held while sliding).
3. Deeper, only a Bonded or Companion wolf can dig the player out, in 1–3 game minutes. Otherwise it is a collapse.

**Tell.** A deep "whumpf" as a layer collapses under the player (H of 3 or more); shooting cracks 2–10 m ahead; fresh slides on slopes of the same aspect; pillows of wind-loaded snow below cornices.

**Counterplay.** Read the slope. Cross one at a time, on ridges or through trees. Wait 3 cold days after a storm, and stay off for 48 game hours after 30 cm of new snow.

**Gives.** Debris is hard snow, the best quarry for snow blocks. In Sheep Crags, 10% of slides carry down a ptarmigan flock or a Dall sheep to scavenge.

### Windthrow

**Trigger.** Gusts of 15 m/s or more in a storm or blizzard. The cut-trail upkeep in Open world in depth and the Burn Hills windfall stay as written. This covers standing live trees.

| Tree | Chance to fall per event |
| --- | --- |
| Sheltered stand | 0.2% |
| Edge tree within 8 m of a clearing of 400 m² or more, including the player's | ×5 |
| Shallow-rooted: permafrost, bog or wet ground | ×3 |
| Beetle-killed, grey stage | ×4 |

**Gives.** Each windthrown tree tears up a root plate 2–4 m wide.

- A root plate serves as a lean-to back wall, halving its poles.
- It exposes 5–20 round stones and, in riparian ground, 1 bucket of clay 30% of the time, even in October.
- Split spruce root for lashing comes off a root plate 2× faster.

**Danger.** In forest during gusts of 15 m/s or more, a tree falls within 20 m once per 100 game hours on average. The creak-and-snap cue applies.

**Counterplay.** Leave a buffer of 20 m of trees round a clearing, or fell the edge trees. Build at least 1.5 tree heights from any edge.

## The forest as a system

### Stand age

**Every forested 32 m cell has a stand age in years and a stand type, kept in the region's stand map (Minecraft development › World and worldgen).** Worldgen sets it: most taiga at 80–200 years, Burn Hills at 5 or 15, riparian ground at 20–60. Age advances each May 1. A burn resets it to 0. Regrowth after felling in The open world follows the same stages.

| Stage | Age | Trees | Main yields | Animals | Fire spread |
| --- | --- | --- | --- | --- | --- |
| Burn | 0 | Black snags | Morels the first spring, dry fuel for 10 years | Three-toed woodpeckers | — |
| Meadow | 1–5 | Snags, aspen suckers | Fireweed shoots ×3; blueberry ×2 in years 2–4 | Hares boom in years 3–6 | ×0.5 |
| Thicket | 6–25 | Aspen, birch, willow 2–8 m | Poles, willow bark, boletes from year 15 | Moose capacity ×2 at 8–25 years; hares ×1.2 | ×0.5, as birch and aspen |
| Young mixed | 26–70 | Birch and aspen over young spruce | Birch bark, chaga, polypore | Grouse ×1.3 | ×0.7 |
| Mature spruce | 71–150 | White spruce over birch | Logs, cones, pitch | Squirrels, marten; beetle risk | ×1.0 |
| Old black spruce | 150+ | Black spruce in deep moss | Usnea ×2, lichen | Caribou in winter | ×1.5 |

**Why it matters.** Old forest wants to burn, and young forest stops fire. A player reading stand age knows where a wildfire will stall. They also know where moose will be in ten years, and why their first clearing holds moose by year 8.

### Spruce beetle

**Trigger.** Two summers in a row with warmth of ×1.1 or more in the year profile, or 20 or more windthrown trees in one chunk, since beetles breed in down logs.

**Effect.** In each region, 1–4 chunks of mature spruce are attacked. Over 2 summers, 30–70% of white spruce over 25 cm die.

| Year | What the player sees | Effect |
| --- | --- | --- |
| 1 | Red-brown frass at trunk bases; beads of pitch on the bark | Pitch gathers 3× faster |
| 2–3 | Needles turn rust-red: the red stage | Fire spread ×1.5 |
| 4+ | Grey trees, bark falling in plates | Standing dry fuel; fire spread ×1.2; windthrow ×4 from year 5 |

**Gives and costs.** Beetle kill refills the ring of easy deadwood near camp that the 0.5% rule drains, so it often lands as a gift in years 3–5. It also drives squirrels out (no cones) and with them marten, and raises fire risk next to the cabin.

**Tell.** Woodpeckers drumming in July; a red patch on the hillside seen from 2 km.

### Deadwood decay

Standing deadwood stays as written. Fallen logs now age through three classes, and old stumps can turn to fatwood.

| Class | Years on the ground | Use | Tell |
| --- | --- | --- | --- |
| Sound | 0–3 | Firewood at 80% heat after 10 days under cover | Bark on, hard |
| Punky | 4–12 | Punk wood: smoke for hides and smudges | Soft, crumbles in the hand |
| Nurse log | 13+ | A cubby set beside it catches marten ×1.5 | Moss and a row of spruce seedlings |

**Punk wood.** Gathered by hand, 2 game minutes per kg. A smudge of punk wood keeps bugs off at 10 m, like green boughs, and lasts 3 game hours unattended. Smoking a hide over punk wood (Hide work) halves its cracking when wet. An ember packed in punk wood lasts 3 game hours, half as long as chaga.

**Fatwood.** Spruce stumps cut or burned 30 or more years ago are soaked with pitch. They stand at Kessler and Doane's camp, Arvid's clearings and old burns. One stump splits into 2 kg, or 20 sticks. A fatwood stick lights from one spark at any dryness, even in rain, and burns hot for 3 game minutes: the wet-weather fire starter.

### Mushroom flushes

**Trigger.** 15 mm or more of rain within 3 days, between July 1 and September 30, with daily means of 8 °C or more. The flush starts 4–6 days later and lasts 6–10 days. A spring flush follows 10 mm within 3 days between May 15 and June 30 at daily means of 6 °C or more; it fruits only aspen oyster and deadly galerina on logs, and morels on burns.

**Yield.** Each forest chunk rolls the flush by stand type: young mixed and thicket ×1.5, mature spruce ×1, old black spruce ×0.5. Species and yields are in New plants and fungi. Morels stay with burns, in spring only.

**Tell.** An earthy smell in the woods; caps pushing through moss. A new prompt, `mushroom_flush` (priority 3): "Heavy rain 5 days ago. Mushroom weather in the birches."

### Squirrel larders

**During a flush, red squirrels hang mushrooms on spruce branches to dry, 1–4 m up.** A squirrel's tree within 15 m of its midden holds 50–300 g of dried mushrooms by September.

**Taking it.** 5 game minutes, with a 2 m pole hook. The squirrel scolds, which alerts game within 50 m.

**The catch.** The haul is 70% boletes, 20% russulas and 10% fly agaric, which squirrels eat safely. Each piece is "unknown mushroom" until inspected. Dried, fly agaric fails the Look check 30% of the time, so the Cut check matters.

### Berry swings

The berry index in the year profile stays. Two local rules sit on top.

**Bloom frost.** A night below −2 °C during a plant's flower stage cuts every frosted patch to ×0.3 for that year. Blueberries flower June 1–25 and cloudberries May 25–June 10. Frost follows micro-climate, so cold sinks freeze first and benches last. In a bloom-frost year, the bog berries in the hollows fail while the bench blueberries hold.

**Drought.** 14 dry days in July cut berries on slopes ×0.7. Bogs are spared.

**Bears follow the good patches.** A local failure moves bears onto the patches that held, which are often the player's. The tell is browned, frost-blackened blossoms on the inspect card.

### Cone crops

**The year profile gains a field, `cone_crop`.** Bust 0.2 (30% of years), normal 1.0 (50%), mast 3.0 (20%). Two masts never come in a row.

| Effect | After a mast | After a bust |
| --- | --- | --- |
| Red squirrel capacity next year | ×1.5 | ×0.6 |
| Marten capacity next winter | ×1.3 | ×0.8 |
| Voles: mouse losses from caches next winter | ×1.5, 3–4.5% a week | ×0.7 |
| Squirrel middens | Twice the usual size | Small |

**Middens mislead.** Middens grow in mast years as well as before hard winters, so a big midden alone is a weak sign of a hard winter. The other two signs in The year leaks out early confirm it.

**Tell.** Spruce tops heavy and brown with cones by July 15, readable from 200 m.

### Lichen and caribou

**Tundra and old black spruce cells store lichen from 0 to 1.** The Caribou Barrens start at 0.6–1.0.

- Each crossing of the herd removes 0.1 from cells on its route. Lichen regrows 0.02 a year, so full recovery takes 50 years.
- Fire sets lichen to 0.
- Each route's draw weight is multiplied by its mean lichen. A route under 0.4 is skipped.

So a tundra fire, the player's or lightning's, can push the herd off a route for decades.

**Famine food.** Reindeer lichen boiled for 1 game hour with a handful of ash gives 90 kcal of carbs per 100 g dry. Eaten raw it causes gut illness. Dry lichen is also tinder, as written.

## New plants and fungi

They join the `plant/` data (Plants and fungi › Data and build) under Foraging and plant knowledge and follow its rules: ripeness stages, patches that remember, the inspect card and the three ways to identify a plant. None is in the pocket guide.

| Plant | Where | Season | Unit | kcal | Vitamin C | Pick time | Keeps | Use |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Birch polypore | Dead standing birch | All year; fresh August–October | 1 bracket, 200 g | 0 | 0 | 3 game minutes with a knife | 1 year dry | Strop, plaster, tinder |
| Birch bolete | Birch stands, young mixed | Flushes, July 10–September 15 | 100 g | 30 | 0 | 2 game minutes | 2 days; dried 1 year | Food; the video's brown mushrooms |
| Orange-cap bolete | Aspen and thickets | Flushes | 100 g | 30 | 0 | 2 game minutes | 2 days; dried 1 year | Food; must be cooked 15 game minutes |
| Shaggy mane | Disturbed ground: worn trails, yards, cleared beds | August–September, after rain | 100 g | 20 | 0 | 1 game minute | 6 game hours, then turns to ink | Food and ink |
| Crowberry | Tundra, bog, burn edges | August to the next June, on the plant under snow | Cup | 45 | +0.5 | 20 game minutes; 40 under snow | 10 days | The only berry at May melt-out |
| Bog blueberry | Bog and tundra | August 1–September 25 | Cup | 80 | +1 | 30 game minutes | 4 days | Ripens 10 days after slope blueberries |
| Sourdock | Wet meadows, banks, beaver meadows | June 1–August 15 | Handful | 25 | +1.5 | 3 game minutes | 2 days; fermented 180 days | The winter green |
| Wild potato | Gravel bars, river terraces; ×2 in braided flats | Roots April 25–June 5 and September 1–October 10 | 250 g root | 230 | 0 | 15 game minutes digging | 14 days; dried 1 year | Carbs |
| Horsetail | Riparian sand, wet ground | Shoots May–June; scouring rush all year | Handful | 20 shoots / 0 stems | 0 | 2 game minutes | 1 year dry | Polishing and scrubbing |
| Devil's club | Only the spray zone of Upriver Canyon and shaded north ravines | Shoots May 20–June 10; stems all year | Handful of shoots | 15 | +0.5 | 3 game minutes | 2 days | Shoots, tea, door charm |
| Plantain | Only on worn trails, yards and old set pieces | June–September | Leaves | 10 | +0.3 | 1 game minute | 3 days; dried 1 year | Poultice |

**Birch polypore.** A strip glued to a board makes a strop: 2 game minutes restore 10 edge after the hone. A thin slice laid on a cut works as a plaster for 24 game hours, a dressing that does not count as clean. Dry polypore dust catches a spark at dryness 80.

**Orange-cap bolete.** The flesh turns black when cooked, which is harmless and shows the art. Eaten undercooked, it gives 1 day of gut illness.

**Shaggy mane grows where the player has walked.** Each worn path or yard cell has a 5% chance per flush to sprout one. Picked caps turn to black ink in 6 game hours. Three caps of ink letter a birch-bark signboard that names a place on the map (+1 Spirits once per sign).

**Crowberry.** Watery and low in value, but it sits ripe on south ridges at melt-out in May, when the larder is lowest.

**Sourdock, fermented.** 5 kg of cooked leaves packed in a sealed birch-bark or wood container keep 180 days. A cup gives +1 vitamin C, which makes a second winter scurvy defense beside rose hips. Its sourness breaks a five-day food streak.

**Wild potato.** Roots dig only in thawed soil, within the two-block dig rule. They taste of carrot and licorice. One of the few carbs on the gravel bars.

**Horsetail.** Scouring rush stems polish wood: Woodcraft jobs run 20% faster with a bundle in the pack. It also scrubs a pot clean of the taint left by spoiled food. Spring shoots are food, but more than 2 handfuls a day cost −5 Condition.

**Devil's club.** Its spines cost −2 Condition and a 20% splinter (a minor cut, with the infection rules) unless picked in gloves. Inner root bark tea gives +2 Spirits, once a day, with Labrador tea's 2-cup limit. A stalk hung over a door is decoration worth +2 Spirits.

**Plantain.** It came with people, so it grows only where people have walked. Each worn trail cell has a 5% chance each summer to gain a patch. A chewed leaf on a blister heals it in 6 game hours instead of a day. On a nettle sting it stops the sting at once.

### Three new lookalikes

Each pair differs on at least two checks of the inspect card, as Foraging and plant knowledge requires.

| Pair | Look | Smell | Cut |
| --- | --- | --- | --- |
| Wild potato / wild sweet pea | Potato: pale pink-lavender flowers 1–1.5 cm long; fine side veins under the leaflets. Sweet pea: bigger, bright magenta flowers; no veins seen | Potato root: sweet, like carrot. Sweet pea: none | Potato: crisp white root. Sweet pea: woody, fibrous |
| Labrador tea / bog rosemary | Tea: rusty wool under rolled leaves, white flower clusters. Rosemary: blue-green leaves, white beneath with no wool, pink bell flowers | Tea: resinous and spicy when crushed. Rosemary: none | — |
| Boletes / poison pax | Boletes: a sponge of pores under the cap. Pax: gills that run down the stem, cap edge rolled under | — | Pax: gills bruise brown at a press. Orange-cap bolete: flesh bruises grey-black |

The pocket guide identifies Labrador tea, so its twin shows as "not Labrador tea" from the start, by the existing rule.

| Plant | Shows after | What happens | What helps |
| --- | --- | --- | --- |
| Wild sweet pea | 1–3 game days | Legs weaken: walk speed ×0.85 and Strength −10 for 5 days per meal. A third meal within 10 days drops walk speed to ×0.6 for 14 days | Stop eating it; it fades |
| Bog rosemary | 1–3 game hours | Slow heart: energy cap 3 for 12 hours, −20 Condition, view sway | Rest and water. Can kill below 25 Condition |
| Poison pax | 6 game hours | First meal: 1 day of gut illness, −10 Condition. Every later meal: −40 Condition over 6 hours and energy cap 2 for a day | Rest. Can kill below 45 Condition. The notebook marks it "made you sick before" |

**The game remembers the first meal.** Poison pax is the one delayed bill that grows: a player who shrugged off the first stomach ache meets the real one on the second meal.

## The land as a tool

Each tool turns a hazard or a season into a resource. All are built with existing tools or with three new items: the snow knife, the ice saw and trail wands.

| Tool | When and where | Cost | Effect | Ties |
| --- | --- | --- | --- | --- |
| Quinzhee | Any snow of 30 cm or more, piled | Pile 2 m high and 3 m wide; let it settle 2 game hours; hollow in 1.5 game hours with a shovel, 3 by hand | +25 °C over the outside air but never above 0 °C inside, no wind; a candle adds +3, a wolf inside +2 (Structures and buildables › Quinzhee) | Warmth, the wolf |
| Snow cave | A lee drift 1.5 m deep or more | 2 game hours with a shovel; sugar snow 30% faster | +20 °C shelter, no wind | Avalanche start zones |
| Snow-block wall | Wind-packed snow: lake ice, tundra, river bars | 30 blocks of 8 kg, 1 game minute each, with the snow knife | A 1.6 m arc that stops all wind within 3 m behind it | The treeless Caribou Barrens |
| Snow banking | Cabin walls, November | 1 game hour per 4 m of wall, 1 m high | Banked wall loses 30% less heat | Warm rooms, rot |
| Ice road | Lake or river ice | Shovel a 3 m lane: 1 game hour per 50 m. Flood it: 20 L per m² below −15 °C | Bare lane ice grows at the full rate. Each flooding adds 2 cm overnight. Sled factor 0.85 on the lane | Sled-safe ice 1–2 weeks early |
| Ice house | Cut February–March from ice 40 cm or thicker | 44 ice cakes of 45 kg (2 t, about 2.2 `ice_block`s of 20 cakes); a 3 × 3 m pit 1 m deep; 30 cm of sawdust, moss or peat | Keeps 300 kg of food at 1–3 °C. 2 t of ice lasts to about September 1 | Spoilage, the salmon run |
| Wind drying | Drying racks facing wind of 3–8 m/s | Placement only | Drying time ×0.7 | Preserving |
| Freeze-drying | December–February, below −20 °C, wind 2 m/s or more, in shade | A drying rack | Strips are dried in 14 days, whole fish in 21, at 40% of fresh weight | Hauling, preserving |
| Glazing | Frozen fish and meat | Dip 3 times in water: 1 game minute per 5 kg | Mice cannot eat glazed items; their scent halves again. The glaze lasts 30 days | Mice, cache scent |
| Sun melt | March 1–April 30, clear, sun above 15°, a south slope | A blackened pot or a dark hide spread with snow | 0.5 L of water a game hour with no fire | Winter water costs firewood |
| Controlled burn | May 1–25, ground wet, wind under 4 m/s | A torch and a 10 m firebreak | Patch resets to meadow (below) | Berries, moose, the garden |

**Quinzhee rules.** Twelve sticks 30 cm long pushed in before hollowing mark the wall thickness. A vent hole is required: a candle with no vent costs 1 Condition an hour of stale air. The ceiling drips above 0 °C inside and collapses after 2 hours above it, so no fire is lit inside. A quinzhee lasts until April.

**Snow banking rots sills.** A bank left past April 15 soaks the bottom wall course: its log decays 2× as fast until the bank is shoveled away (1 game hour).

**Ice house.** It is the pit pattern of Structures and buildables' `yearofice:ice_house`, placed by Crafting and building in depth's rules as a pit with a pole-and-sod roof. The cribbed Iron-tier house there holds 80 cakes (4 blocks) and keeps half its ice to October 1; the pit is the year-1 version. Ice melts 0.8% of its starting mass a day from May on under 30 cm of insulation, and 2.5% under 10 cm. It is the ice cave brought home.

**New items.**

| Item | Recipe | Tier | Use |
| --- | --- | --- | --- |
| Snow knife | A 40 cm blade of bone, antler or split birch; knife; 30 game minutes | Bushcraft | Cuts snow blocks; a block a game minute |
| Ice saw | 0.6 kg iron and a pole; furnace; 2 game hours (Structures and buildables) | Iron | Cuts a 45 kg `ice_cake` in 3 game minutes; the ice chisel takes 6 |
| Trail wand | A 1 m willow stick with the bark peeled at the top; 1 game minute | Start | Reads at 40 m in flat light, 15 m in a whiteout |

### Controlled burns

Fire as land management is a skill the land teaches back. Arvid burned the slope behind his cabin each spring, and a line in his notes says why: "Burn the bench in May, berries in August two years on."

**Rules.**

1. **Window.** May 1–25, after the cell melts out and before green-up. Rain fell within 3 days, or melt-out was under 10 days ago. Wind is under 4 m/s.
2. **Fuel.** Only meadow, thicket edges and grass carry the burn, at the forest-fire spread rate ×0.3.
3. **Break.** A 10 m firebreak, water or bare ground stops it. Any spruce cell it reaches burns at the full rule, which is how a controlled burn becomes a wildfire.
4. **Result.** The burned patch resets to meadow age 1. Blueberry yield ×1.5 in years 2–4, fireweed shoots ×3, and the thicket that follows doubles moose capacity from year 8. A garden bed on burned ground yields 10% more its first year.

**Counterplay and cost.** A torch, 1–3 game hours of watching and the firebreak. A burn the player walks away from rolls ignition like any untended fire.

**Tell.** Low creeping flame in dead grass; white smoke that thins at the break. The journal logs the patch and its age.

### Beaver meadows as hay

Drained beaver meadows, the abandoned-colony variant in Beaver valley, are the best hay ground. Sedge hay cuts at 30 kg a game hour there, against 20 elsewhere, which shortens a goat's 400 kg winter by a third. Beaver flooding itself belongs to Water, calls and encounters.

## Micro-climates

**Every block column carries a micro-climate class.** Worldgen sets it from aspect, slope, curvature, canopy and distance to water, and caches it per 8 m cell. Offsets apply to the air at the player, plant windows, frost nights and snowmelt.

| Class | Rule | Day offset | Clear calm night | Plant window | Frost | Other |
| --- | --- | --- | --- | --- | --- | --- |
| South slope | Aspect 135–225°, slope 10° or more | +2 °C in summer; +4 °C at midday February–April | 0 | 7 days early | Spring frost ends 7 days earlier | Bugs ×0.8; snow melts 1.5× as written |
| North slope | Aspect 315–45°, slope 10° or more | −2 °C | −1 °C | 7 days late | — | Permafrost as written |
| Cold sink | A concave basin, or a floor cell lower than all ground within 50 m | 0 | −3 to −6 °C | 5 days late | 2× as many frost nights; garden season 10–14 days shorter at each end | Ice fog forms here first |
| River corridor | Within 30 m of the main river | −1 °C in summer | +1 °C until freeze-up | — | Fall frost 5 days later | Wind ×1.3 along the river; bugs ×0.7 |
| Lake shore | Within 50 m of a lake | −2 °C until ice-out; +2 °C until freeze-up | +1 °C in fall | Spring 5 days late | Fall frost later | — |
| Bench | 30–80 m above the floor | Inversion as written | — | — | Half as many frost nights | Hears and smells the floor below |
| Under canopy | Spruce cover over 60% | −1 °C at the high | +2 °C | 3 days late | Half as many frost nights | Wind ×0.3; snow 60% as written |
| South face foot | Within 5 m of a south face over 6 m tall | +6 °C at midday February–April | +2 °C | 10 days early | — | Melts out first; in the rockfall fan |
| Spray zone | Within 50 m of a falls over 5 m | −1 °C | +1 °C | — | — | Rime ice on rock and logs in winter (bare-ice slips); humid, so devil's club grows |

```latex
T_{cell}(h) = T_{air}(h) + o_{day} \cdot w_{sun}(h) + o_{night} \cdot c_{calm}(h)
```

Here w\_sun is the sun's height share from the daylight table (0 at night, 1 at the day's highest sun), and c\_calm is 1 on a clear night with wind under 2 m/s, else 0. The HUD's air reading is the player's own cell, so a player who watches it while walking feels the micro-climates.

**What it changes.**

- **Gardens.** Crop farming's frost checks read the cell, not the valley. A bed in a cold sink loses its potatoes two weeks before a bed on the bench.
- **Berries.** Patch windows and bloom frost read the cell, so the same species ripens over 3 weeks across the valley. That spreads the picking season.
- **Camps.** A south-face foot is the warmest work spot in March. A cold sink is the worst bed in the valley.
- **Reading.** A Handy Land reader names the class; a Practiced one adds the night offset: "Frost hollow. About 5 °C colder on clear nights."

## Stories the land tells

These are the moments the systems above should produce on their own. The balance simulator checks that each can happen within 3 years on most seeds.

- **The track with no frost in it.** A wolf track cut clean through the morning's hoar was made after 02:00, so the pack is near. The player brings the wolf in and bars the door.
- **Dug out by the wolf.** A Companion wolf digs the player from a slide in Sheep Crags in 2 game minutes. The journal line is the one players screenshot.
- **The rifle shot at 03:00.** A frost quake wakes the player, who sits up with the spray in hand. Nothing is there. In spring, the door jams on the heave.
- **The frost that chose the bench.** A June bloom frost kills the bog berries in the hollow and spares the bench blueberries. In August the grizzly comes up the bench to the player's patch.
- **The squirrel's mistake.** A dried red cap in a squirrel's larder, eaten without the Cut check, gives 6 hours of rippling sky.
- **Light fish.** Freeze-dried grayling at 40% of fresh weight let the player carry a month of food over Windgap Pass in January.
- **The fog map.** From Lookout Bench at −40 °C, ice fog traces every open lead on the river. The player plans the crossing from above.
- **Arvid's bench.** A spring burn on the slope behind the cabin, done his way, gives the biggest blueberry patch in the valley two summers later.

## Crop farming in play

Blueprint › Crop farming sets the five crops, the sowing windows, the germination formula and the seed economy. This section turns each step into a Minecraft action with a block, an item, a key, a time and a number, so the video's seasonal farming with partial germination can be built as written. Farming stays a once-a-year bet: a first garden adds about 16,000 kcal.

### The garden in Minecraft terms

**One block is one square metre of garden.** Every crop sits on a bed block and is its own block entity, so the year can be checked block by block.

| Thing | Minecraft form | States and data | How the player uses it |
| --- | --- | --- | --- |
| Garden bed | `yearofice:garden_bed`, a soil block made by tilling | `moisture` 0–3, `weeds` 0–3, `soil` (silt, loam, podzol, gravel), `fed` 0–3 | Hold right mouse with a hoe or digging stick on cleared soil |
| Crop | `yearofice:potato_crop`, `barley_crop`, `wheat_crop`, `turnip_crop`, `cabbage_crop`, placed on a bed; block entity `CropBlockEntity` | `stage` 0–5 (sown, sprout, leaf, head or flower, ripe, dead) and `frosted`; the block entity keeps sowing day, seeds sown and up, growth units, dry days and weed weeks | Sow: hold right mouse with seed on a bed, 1 game minute a block |
| Seed | `yearofice:seed_potato` (a 2-eye piece, 50 g), `barley_seed` and `wheat_seed` (20 g, one block), `turnip_seed` (one block), `cabbage_seed` (one tray cell) | Component `yearofice:seed_lot`: year saved, dried, damp | Held in the hotbar while sowing |
| Seedling tray | `yearofice:seed_tray`, 2 split planks and 1 bucket of soil, 20 game minutes, Bushcraft | 12 cells; works in a room at 12 °C or more within 3 blocks of a window, or in the greenhouse | Right mouse to sow or lift a seedling |
| Harvest | `yearofice:potato`, `turnip`, `cabbage` (counted in 100 g units), `grain_sheaf` (species component), then `barley`, `wheat` (100 g units) and `straw` | `yearofice:fresh`, `yearofice:weight_g` | Dig, pull or cut: hold right mouse on a ripe crop |
| Flail | `yearofice:flail`: a 1 m and a 0.6 m stick joined by 0.3 m of rawhide or cordage; knife, 20 game minutes, Bushcraft, 0.8 kg | Vanilla durability | Hold right mouse on a sheaf laid on a plank floor or bark sheet |

**The crouch card.** Crouching (Shift) while looking at a bed opens its card: soil class, soil temperature, moisture, weeds, the expected germination for the seed in hand ("barley here: about 45%, soil 7 °C"), and for a growing crop its stage and "ripe in about 12 days".

### Clearing and soil

**Clearing follows the two-block dig rule.** Soil is worked to 2 blocks down; frozen soil cannot be tilled from the day the soil freezes to 10 days after the cell melts out.

1. **Fell** the trees by the existing felling rules.
2. **Pull stumps.** Hold right mouse on a `stump` block with a shovel or Pulaski: 20 game minutes a stump, 10 with the Pulaski. A stump left 10 years is rotten and pulls in 5.
3. **Strip** moss and roots: 15 game minutes a block with a digging stick, 7 with a hoe. Each block gives 1 moss sheet.
4. **Till**: 6 game minutes a block with a digging stick, 3 with a hoe, 12 by hand. The block becomes `garden_bed`.

Ten square metres of spruce floor with 2 stumps take about 4 game hours with a digging stick and about 2.3 with a hoe, as the Blueprint says. Grass, fireweed and burned ground skip step 3.

| Soil class | Where | Yield | Notes |
| --- | --- | --- | --- |
| Silt | River corridor, beaver meadows, old gravel-bar edges | ×1.2 | The best; floods may cover it (Water, calls and encounters) |
| Loam | Birch–aspen slopes, burns | ×1.0 | Good |
| Podzol | Spruce taiga | ×0.8 | 0.2 kg of ash a block lifts it to ×1.0 for that year |
| Gravel | Bars and terraces | ×0.6 | Dries fast: `moisture` falls 1 more a day |
| Bog, permafrost | Muskeg; north-facing taiga | Fails | Soil stays under 5 °C; bog stays waterlogged |

**Ground that remembers.** Burned ground +10% in its first year (Controlled burns), green-alder ground +15% for 3 years, lupine ground +10% for 2 years (Plants and fungi), rich ground under an old carcass +10% for 3 summers (Decay and time). These and feeding together cap at +30%, the Blueprint's fertility cap.

### Soil temperature and germination

**Each seed rolls once when sown**, with the Blueprint's formula P = g × f\_soil × f\_moisture × f\_depth × f\_age. This section defines each factor's input.

```latex
T_{soil} = \overline{T}_{air,\,7\,days}(cell) + o_{bed}
```

- **Soil temperature.** The 7-day mean air temperature at the cell, which already carries the micro-climate offset, plus the bed offset: tilled bare bed +1 °C, cold frame +6 °C by day, greenhouse +10 °C, manure hotbed +8 °C, untilled forest floor −3 °C. A cell reads 0 °C until 10 days after melt-out. In a normal year a bed reads 7–9 °C on May 15 (×0.7) and 11–13 °C by June 1 (×1.0); a cold sink reads about 2 °C lower.
- **Moisture.** `moisture` 0 is dry (×0.6), 1–2 is right (×1.0), 3 is waterlogged (×0.5). Rain of 5 mm or more sets 3, which drains to 2 the next day on any soil but bog. Each dry day takes 1 off when the high passes 15 °C, otherwise 1 every 2 dry days.
- **Depth.** Seed sown into a bed tilled this season is at the right depth (×1.0). Seed broadcast on a bed not tilled this season lies shallow (×0.7; birds take it). Hilling potatoes before their sprouts show, or tilling over sown seed, buries it (×0.6). The card names the mistake before the player commits.
- **Seed age.** −10% for each year since `seed_lot.year_saved`; −30% more if `damp` is set, which happens when seed spends 7 game days outside a dry container (chest, barrel, crate or sack in a room).

**What a roll means.** A potato block holds one piece, so it comes up or it does not. A barley or wheat block holds 20 g of seed, and its stand s is the share that came up. Sprouts show at the crop's sprout units, 7–14 days after sowing, so an empty block can be re-sown inside the window. On the evening a crop's window closes, the Trail Messenger posts "The planting window for barley has closed." (prompt `garden_window_closed`, priority 2, only while seed for that crop is carried or stored) Sowing after it is allowed, but the crouch card reads "unlikely to ripen before frost" and the crop clock decides.

### Growth units and the crop clock

**Crops grow by growth units, not days.** A day's units are the day's mean air temperature at the cell above 5 °C, ×1.2 in June and July for the midnight sun. A dry spell of 7 days or more adds no units until the bed is watered. The normal-year sums are those in Plants and fungi › Foraging calendar: about 120 in May, 315 in June, 370 in July, 280 in August and 90 in September.

| Crop | Sprouts at | Ripe at | Normal year, from the first sowing date | Yield per block | Frost |
| --- | --- | --- | --- | --- | --- |
| Potato | 60 | 1,100 | Sown May 25, ripe about August 25 (93 days) | 1 kg, about 7 tubers | −1 °C kills the leaves and stops growth; tubers survive in the ground until a −5 °C night |
| Barley | 40 | 950 | Sown May 20, ripe about August 10 (82 days); sown June 10, about August 27 | 0.3 kg grain and 0.5 kg straw at the expected stand | Seedlings take −3 °C; a −2 °C night between 700 and 950 units cuts the grain ×0.2 |
| Wheat | 50 | 1,150 | Sown May 20, ripe about August 31 (103 days) | 0.25 kg grain and 0.5 kg straw | As barley, between 800 and 1,150 units, ×0.1 |
| Turnip | 30 | 700 | Sown May 15, ripe about July 17; sown July 1, about August 29 | 4 roots, 1.6 kg, and 1 handful of greens | Hardy to −6 °C; each night under −1 °C before harvest makes it sweeter (+1 Spirits the first meal) |
| Cabbage | Planted out | 1,000 after planting out | Started in a tray April 1–15; planted out June 1, ripe about August 20 | One 1.5 kg head | Hardy to −4 °C |

**Stand and yield.** A grain block's yield is the table's figure × clamp(0.3 + 0.7 × s ÷ g, 0, 1.2), where g is the crop's base germination, because thin stands tiller. So barley at half its expected stand still gives 65%.

**Harvesting early.** Potatoes can be dug from 500 units, at yield × (units − 500) ÷ 600. Grain cut at 90% of its ripe units gives ×0.7; cut earlier, it is green fodder that dries to hay. A cold year (warmth ×0.9) leaves wheat short of ripe before the frost window in most years, which is the Blueprint's warm-year gamble in numbers.

### Water, weeds and feeding

- **Watering.** Hold right mouse on a bed with a filled pot, bark bucket or water bag: 1 L a block, 1 game minute for every 2 blocks, raising `moisture` by 2. Water comes from the nearest source by the existing water rules. A rain gutter and barrel collect 10 L for each 5 mm of rain. After 7 dry days plants wilt and lose 5% of yield a day until watered (Blueprint).
- **Weeds.** From sprouting to August 31, `weeds` rises 1 a week; each step costs 3% of yield a week. Weeding is a hold-to-work job with a hoe or by hand, 1 game minute a block, queueable like other jobs. Every 5 blocks weeded give 1 handful of chickweed or lambsquarters greens (Plants and fungi).
- **Feeding.** Ash 0.2 kg a block +10%; manure from goats or hens 1 kg a block +15%; fish guts 0.5 kg a block +10%, but a fed bed smells like a cleaning spot to bears for 3 days. Each sets `fed`; together they cap at +30% with the ground bonuses.
- **Worn-out beds.** A bed cropped without feeding loses 10% of yield a year, down to ×0.5. The same crop on the same bed a second year costs another 10%, and potatoes after potatoes 20%. Rotating potato, then barley, then turnip clears the penalty. This keeps a large garden tied to livestock, stove ash and the fish catch.

### Frost

**Frost reads the cell, not the valley.** Each night's low comes from the weather draw plus the cell's micro-climate (Micro-climates above), so a bed in a cold sink loses its potatoes up to two weeks before one on a bench. The last spring frost is drawn between May 20 and June 10 and the first fall frost between August 20 and September 15. The Trail Messenger warns the evening before.

| Protection | Cost | Gain |
| --- | --- | --- |
| Spruce boughs | 1 armload covers 4 blocks; 1 game minute a block to lay, the same to lift | +2 °C for one night; left on a second day, the crop loses that day's units |
| A hide | Covers 2 blocks | +2 °C for one night |
| Windbreak of 2 blocks (Structures and buildables) | Placement | +1 °C on frost nights in its lee |
| Cold frame (Crafting and building in depth) | A 1 × 2 m piece | +6 °C by day; sow 3 weeks early |
| Greenhouse (Structures and buildables) | Homestead | 3 weeks at each end of the season |

**The hard freeze.** A −5 °C night ends the season: every crop still standing dies, and potatoes still in the ground freeze to mush.

### Pests and animals

| Animal | Takes | When | Rule | Stopped by |
| --- | --- | --- | --- | --- |
| Snowshoe hare | Seedlings of every crop | The first 21 days after sprouting | An unfenced bed within 50 m of brush loses 2% of its sprouted blocks a day, ×3 in a hare peak | A brush fence (Crafting and building in depth); a tamed wolf sleeping within 10 m cuts losses ×0.2 |
| Ruffed and spruce grouse | Seed and sprouts | The first 14 days | 1% of blocks a day within 20 m of a forest edge; halved where the bed was tilled | A brush fence |
| Moose | Cabbage, turnip greens, barley heads | July–September | An unfenced bed in a cell moose use is visited on 10% of nights; each visit eats 30% of cabbage and turnip blocks and 10% of grain | A pole fence |
| Voles | Potatoes in the ground; stored grain and roots | September; winter | In a vole year 5% of potato hills lose half; stored grain and roots lose by the cache mice rule | Harvest by September 15; a barrel |
| Black and grizzly bears | Potatoes, turnips, fish-gut beds | September–October | A bed of unharvested potatoes after September 1 draws a bear on 3% of nights within its range; a dig takes 50% | Harvest; the wolf yard (Structures and buildables) |
| Loose hens and goats | Everything green | Any | A loose hen strips 1 block a day, a goat 4 | Pens (Crafting and building in depth) |

Every loss writes to the bed's harvest note ("Hares took 6 blocks of barley in June"), so a failure stays readable.

### Harvest, curing and threshing

1. **Potatoes.** Hold right mouse with a digging stick: 3 game minutes a hill, a shovel ×0.6. A hill drops `potato` in 100 g units. Cure 7 game days in the dark above 5 °C before storing; uncured potatoes keep half as long. Potatoes left 3 game days in daylight turn green: 20% gut illness eaten.
2. **Grain.** Cut with a knife: 2 game minutes a block, giving one `grain_sheaf`. Dry sheaves 5 dry days on a hay rack or drying rack; sheaves left wet 3 days lose 10% a day to mould. Thresh with the flail, 10 game minutes a sheaf, then winnow on a day with wind of 2–8 m/s, 5 game minutes a kg. A sheaf gives the block's grain and 0.5 kg of `straw` for bedding, insoles and thatch.
3. **Turnips.** Pull by hand, 1 game minute a block; the tops are 1 handful of greens.
4. **Cabbage.** Cut with a knife, 1 game minute a head. Sauerkraut is in Blueprint › Preserving in detail.

Grain becomes flour on the hand quern, 1 kg an hour, or in the grist mill (Structures and buildables).

### Storage

| Crop | Where | Keeps | Losses |
| --- | --- | --- | --- |
| Potatoes | Root cellar at 2–4 °C | 6 months (Decay and time) | Freeze to mush at −1 °C in a cabin corner; sprout after 30 days above 10 °C, which still makes seed |
| Potatoes | Potato clamp (Structures and buildables) | Until spring | 15% to rot and voles |
| Grain | Barrel or sack in a dry room | 2 years | Damp grain moulds in 14 days; in a sack on the floor, voles by the cache mice rule |
| Turnips | Root cellar | 4 months; 3 in a clamp | Freeze to mush at −2 °C |
| Cabbage | Root cellar | 3 months | Sauerkraut keeps its vitamin C all winter |

### Seed saving and the second year

- **Potatoes.** Any stored potato sprouts in the dark from April. In May, cut one with a knife into 2 `seed_potato` pieces (1 game minute, base 85%), or plant it whole (90%, twice the stock).
- **Grain.** Winnowed grain dried 2 more days on the rack becomes seed: 100 g of `barley` makes 5 `barley_seed` (one block each) with `seed_lot.dried` set, base 80%. Packet seed is 60% because it is old.
- **Turnip and cabbage seed.** Both are biennial. A root or head kept in the cellar over winter and replanted in May flowers in July, and by August 31 gives seed for 20 blocks of turnip or 12 cabbage cells. From year 2 the supply-drop packet is renewable.
- **How much to keep.** A potato returns about 8 to 1 and barley about 15 to 1 in a good year, so keeping 20–30% of a harvest roughly triples the garden. Eating the seed means no garden next year.

**Why it stays a bet.** A barley block costs about 35 game minutes of work a year (till, sow, weed, cut, thresh) for about 1,000 kcal. A 100-block garden is about 60 game hours of summer labour, in the same weeks as the salmon run and the berries, and returns about a tenth of a year's food. Labour, land, frost, the worn-bed rule and storage cap it, never a counter, which blocks the video's bread exploit.

### Data and build for crops

- **Ids.** Blocks `garden_bed` (existing), `potato_crop`, `barley_crop`, `wheat_crop`, `turnip_crop`, `cabbage_crop` (block entities), `seed_tray` (block entity). Items `seed_potato`, `barley_seed`, `wheat_seed`, `turnip_seed`, `cabbage_seed`, `potato`, `barley`, `wheat`, `turnip`, `cabbage`, `grain_sheaf`, `straw`, `flail`, `hoe` (existing). Component `yearofice:seed_lot`. Tags `yearofice:crops`, `yearofice:tillable_soils`.
- **Data.** `data/yearofice/crop/<id>.json` (the Crop kind); `loot_table/blocks/<crop>.json` by `stage`; `recipe/flail.json`, `recipe/seed_tray.json`, `recipe/rework/seed_potato.json`, `recipe/rework/barley_seed.json`; food, weight and `spoil_days_at_5c` in `data_maps/item/food.json` and `weight.json`.
- **Classes.** `com.yearofice.food.garden.GardenBedBlock`, `CropBlock`, `CropBlockEntity`, `SeedTrayBlockEntity`, `GardenPests`; pure rules in `com.yearofice.sim.garden.Germination`, `GrowthUnits`, `SoilTemperature`. Crops grow on `ClockEvent.Day` and catch up in closed form on `ChunkCaughtUpEvent`; `randomTickSpeed` stays 0.
- **Milestones.** M5: beds, crops, germination, growth units, frost, water, weeds, pests, harvest and seed saving. M8: the root cellar and clamp tie-in. M9: the seed tray, greenhouse and manure hotbed.

**Debug and tuning for crops.** `com.yearofice.food.garden.GardenCommands` registers the `/yoi crop` commands, op level 2, on the bed or crop under the crosshair; each prints the record it read or changed.

| Command | Does |
| --- | --- |
| `/yoi crop info` | Bed: soil class, soil temperature, `moisture`, `weeds`, `fed`, years cropped. Crop: `stage`, growth units, sowing day, seeds sown and up, frost marks, expected yield |
| `/yoi crop germ <crop> <n>` | Dry-runs n germination rolls on this bed and prints the share that came up; changes nothing |
| `/yoi crop grow <units>` | Adds growth units and re-stages the crop |
| `/yoi crop stage <0–5>` | Sets the stage directly |
| `/yoi crop frost <low_c>` | Runs one night at that low over the beds within 16 blocks, with covers counted |
| `/yoi crop year <cold \| normal \| warm>` | Sets this year's warmth factor (0.9, 1.0, 1.1) until May 1 |
| `/yoi crop pest <hare \| grouse \| moose \| vole \| bear \| hen \| goat>` | Forces one visit tonight to the nearest unfenced bed |
| `/yoi crop ripen` | Sets the crop to ripe at its expected yield, for harvest tests |

All garden dials sit in `yearofice-common.toml` under `[garden]`, set by the world's realism preset; a dial set by hand marks it "Custom".

| Key | Gentle | Standard | Brutal | Unit |
| --- | --- | --- | --- | --- |
| `garden.germination_scale` | 1.15 | 1.0 | 0.9 | × the rolled chance, capped at 0.95 |
| `garden.yield_scale` | 1.2 | 1.0 | 0.85 | × yield per block |
| `garden.frost_offset_c` | +1 | 0 | −1 | Added to every night low over a bed |
| `garden.pest_scale` | 0.5 | 1.0 | 1.25 | × every pest chance |
| `garden.worn_bed_loss` | 0.05 | 0.10 | 0.10 | Yield lost a year without feeding |
| `garden.work_time_scale` | 0.75 | 1.0 | 1.0 | × game minutes per garden job |

```json
{
  "id": "yearofice:barley",
  "block": "yearofice:barley_crop",
  "seed": { "item": "yearofice:barley_seed", "weight_g": 20, "base_germination": 0.6, "saved_germination": 0.8 },
  "sow_window": ["05-20", "06-10"],
  "units": { "sprout": 40, "ripe": 950, "frost_sensitive": [700, 950] },
  "frost": { "seedling_min_c": -3, "head_min_c": -2, "head_yield_mult": 0.2, "hard_freeze_c": -5 },
  "yield": { "item": "yearofice:barley", "kg_per_block": 0.3, "straw_kg": 0.5, "stand_formula": "tiller" },
  "pests": { "hare_day_pct": 2, "grouse_day_pct": 1, "moose_visit_pct": 10, "moose_take_pct": 10 },
  "work_min": { "sow": 1, "weed": 1, "cut": 2, "thresh": 10, "winnow_per_kg": 5 }
}
```

**GameTests (batch `m5`, prefix `food/`).**

1. **`yearofice:food/germination_soil`.** Barley sown on May 15 of the normal-year fixture in tilled loam comes up 38–46% over 10,000 seeds; sown June 1, 57–63%; broadcast on June 1 on a bed not tilled that season, 39–45%.
2. **`yearofice:food/crop_clock`.** In the normal-year fixture, potatoes sown May 25 ripen between August 21 and 29, barley sown May 20 between August 6 and 14, and turnips sown July 1 between August 25 and September 2.
3. **`yearofice:food/head_frost`.** A −2 °C night at 800 units leaves barley at 0.20 ±0.02 of its yield; the same night at 300 units costs nothing.
4. **`yearofice:food/first_garden`.** The crate's seed sown in its window with weeding and water gives 12,000–20,000 kcal over 100 normal-year seeds, with a mean of 16,000 ±1,000.
5. **`yearofice:food/garden_caught_up`.** A bed left unloaded from sowing to harvest matches a loaded twin block for block in stage, units and yield, as the lead's `layers/live_equals_caught_up` does.
6. **`yearofice:food/hare_fence`.** At hare index 1.0, an unfenced barley bed within 50 m of brush loses 35–50% of its sprouted blocks in 21 days; a brush-fenced bed loses none.

## Data and build

**Minecraft build.** The NeoForge mod `yearofice`, package `com.yearofice`; classes sit in the lead engineer's packages (Minecraft development › Package and module map). Everything here is data-driven: phenomena, signs, classes and plants are JSON under `data/yearofice/`, and Java classes read them.

**Registry ids.**

| Kind | Ids |
| --- | --- |
| Blocks | `yearofice:pressure_ice`, `yearofice:candle_ice`, `yearofice:anchor_ice`, `yearofice:hoarfrost_layer`, `yearofice:avalanche_debris`, `yearofice:quinzhee_snow`, `yearofice:snow_block`, `yearofice:ice_block`, `yearofice:mud`, `yearofice:pingo_ice`, `yearofice:root_plate`, `yearofice:punk_log`, `yearofice:fatwood_stump`, `yearofice:birch_polypore` (on birch log faces), `yearofice:shaggy_mane`, `yearofice:squirrel_larder`, `yearofice:trail_wand`, `yearofice:ice_house` (multiblock controller) |
| Plant blocks | `yearofice:crowberry_bush`, `yearofice:bog_blueberry_bush` (berry bushes in `yearofice:berry_bushes`; fruit items `crowberry`, `bog_blueberry`), `yearofice:sourdock`, `yearofice:wild_potato`, `yearofice:wild_sweet_pea`, `yearofice:horsetail`, `yearofice:devils_club`, `yearofice:plantain`, `yearofice:bog_rosemary`, `yearofice:birch_bolete`, `yearofice:orange_cap_bolete`, `yearofice:poison_pax` |
| Items | `yearofice:snow_knife`, `yearofice:ice_saw`, `yearofice:ice_cake`, `yearofice:punk_wood`, `yearofice:fatwood_stick`, `yearofice:polypore_strop`, `yearofice:mushroom_ink`, `yearofice:fermented_sourdock`, `yearofice:freeze_dried_strips`, `yearofice:glazed_fish` (a data component on any frozen food item) |
| Particles | `yearofice:diamond_dust`, `yearofice:snow_bomb`, `yearofice:rockfall_dust`, `yearofice:light_pillar` |
| Sounds | `yearofice:world.frost_quake`, `world.whumpf`, `world.rockfall_trickle`, `world.candle_ice_chime`, `world.anchor_ice_hiss`, `world.aurora_crackle`, `world.gap_wind_roar` (the `world.` group, Minecraft development › Conflicting ids row 19) |
| Key mapping | `key.yearofice.read`, default X, registered in `RegisterKeyMappingsEvent` |
| Data attachments | `yearofice:stand` (age, type, beetle stage, lichen): the truth is the region or district stand map, 4 bytes per 32 m cell, and the chunk attachment is a transient copy filled on load; `yearofice:microclimate` (one byte per column, 256 per chunk); `yearofice:hazard` (avalanche H per start zone, per day) |

**Data-pack paths.**

- `data/yearofice/phenomenon/<id>.json`: one file per phenomenon, with trigger, numbers and tells.
- `data/yearofice/sky_sign/<id>.json`: one file per sign, with its chance given tomorrow's state.
- `data/yearofice/microclimate/<class>.json`: one file per class, its rule and offsets.
- `data/yearofice/succession/<stage>.json`: one file per stand stage, its yields and fire factor.
- `data/yearofice/plant/<id>.json`: the 14 new plant and fungus rows, in the Plants and fungi schema; `data/yearofice/crop/<id>.json` for the five crops (Crop farming in play).
- `data/yearofice/worldgen/configured_feature/pingo.json` and `placed_feature/pingo.json`; a biome modifier adds it to `yearofice:slough_country` and `yearofice:cloudberry_muskeg`.
- `data/yearofice/loot_table/gameplay/squirrel_larder.json`, `root_plate.json`, `avalanche_debris.json`.
- `data/yearofice/recipe/snow_knife.json`, `ice_saw.json`, `trail_wand.json`, `polypore_strop.json`.
- `data/yearofice/tags/block/heave_grounds.json`, `avalanche_starts.json`, `spray_zones.json` (tags are plural nouns).

**JSON shapes.**

```json
{
  "id": "yearofice:ice_fog",
  "trigger": { "air_below_c": -35, "wind_below_ms": 1, "source": ["spring", "open_lead", "hot_spring_outflow", "ice_hole", "chimney"] },
  "radius_m": { "spring": 150, "chimney": 30 },
  "effects": { "sight_m": 30, "animal_sight_mult": 0.4, "outer_wet_per_h": 0.05, "scent_decay_h": 48, "scent_cone_mult": 0.5 },
  "tells": { "particle": "yearofice:diamond_dust", "messenger": "{t} °C, calm. Ice fog over open water." }
}
```

```json
{
  "id": "yearofice:sun_dogs",
  "requires": { "sky": "clear", "air_below_c": -15, "sun_below_deg": 20 },
  "chance_given_tomorrow": { "colder_or_same": 0.55, "warmer": 0.14 },
  "predicts": "no_rise_24h",
  "notice": { "clumsy": false, "handy": true, "practiced": true }
}
```

```json
{
  "id": "yearofice:cold_sink",
  "rule": { "curvature": "concave", "lower_than_all_within_m": 50 },
  "offset_c": { "day": 0, "clear_calm_night": [-6, -3] },
  "plant_window_days": 5,
  "frost_night_mult": 2.0,
  "read_line": { "handy": "Frost hollow.", "practiced": "Frost hollow. About {n} °C colder on clear nights." }
}
```

```json
{
  "id": "yearofice:wild_potato",
  "habitat": ["gravel_bar", "river_terrace"],
  "archetype_mult": { "braided_flats": 2.0 },
  "windows": [["04-25", "06-05"], ["09-01", "10-10"]],
  "unit": { "weight_g": 250, "kcal": 230, "vit_c": 0, "time_min": 15, "units_per_clump": 3 },
  "keeps": { "spoil_days_at_5c": 14, "dried_days": 365 },
  "lookalike": "yearofice:wild_sweet_pea",
  "inspect": { "look": "pale_lavender_veined", "smell": "carrot", "cut": "crisp_white" }
}
```

**Java classes.**

| Class | Owns |
| --- | --- |
| `com.yearofice.story.reading.LandReading` (key handling in `client.reading`) | The Read key, the 12 feature readers, accuracy by level, confirmations |
| `com.yearofice.story.reading.SkyLore` | Evening signs from the year table, the sound thermometer, the fireweed clock, Polaris |
| `com.yearofice.env.PhenomenaDirector` | A daily server tick at 06:00 and an hourly check: ice fog, flat light, the cold lid, the gap wind, hoarfrost, frost quakes, aurora index |
| `com.yearofice.world.IceWorks` | Pressure ridges, anchor ice, candle ice |
| `com.yearofice.world.SlopeHazards` | Rockfall, the avalanche score and burial, windthrow |
| `com.yearofice.env.GroundState` | Mud season, frost heave rolls on buildings, pingos |
| `com.yearofice.world.ForestStand` | The stand attachment, succession on May 1 catch-up, beetles, deadwood classes, cone crops, lichen |
| `com.yearofice.forage.FungiFlush` | Flush triggers, yields, squirrel larders |
| `com.yearofice.env.MicroClimate` | The column class, the cell temperature formula, frost and window offsets |
| `com.yearofice.build.snow.SnowWorks` | Quinzhee, snow cave, blocks, banking, the ice road |
| `com.yearofice.food.preserve.ColdStore` | Ice house, freeze-drying, glazing, sun melt |
| `com.yearofice.fire.ControlledBurn` | The burn window, the ×0.3 spread, escape into the fire rule |
| `com.yearofice.client.AuroraSkyRenderer`, `IceFogRenderer`, `FlatLightShading` | Sky, fog through `ViewportEvent.RenderFog`, shading off on snow in flat light |

**Milestones.**

| Milestone | Ships |
| --- | --- |
| M5 — Food and goals | The 14 new plant and fungus rows and lookalikes, micro-climates for gardens and patches, mushroom flushes, squirrel larders, the `mushroom_flush` prompt |
| M6 — Danger and big game | Rockfall and its sound cue, windthrow danger, hoarfrost track dating |
| M7 — The year | Land reading and Sky lore, sky signs, the aurora index, ice fog, flat light, the cold lid, the gap wind, frost quakes, pressure ridges, anchor ice, candle ice, mud season, avalanches, quinzhee, snow cave and snow blocks, freeze-drying, glazing, sun melt, berry swings, cone crops |
| M8 — Building and outposts | Frost heave on buildings, snow banking, the ice road, the ice house, trail wands |
| M9 — Homestead and beyond | Stand age and succession, spruce beetle, deadwood decay and fatwood, lichen and caribou routes, controlled burns, beaver-meadow hay, pingos in streamed regions |

**Debug commands.** `com.yearofice.env.LandCommands` registers the `/yoi land` commands, op level 2, for testing on Ruhan's own worlds. Each acts where the player stands or on the block under the crosshair, and prints the record it read or changed.

| Command | Does |
| --- | --- |
| `/yoi land micro` | The column's micro-climate class, day and night offsets, frost-night multiplier and plant-window shift |
| `/yoi land sky` | Tomorrow's drawn state and every sky sign showing tonight, with its chance |
| `/yoi land phenomenon <id> [start \| stop]` | Starts or ends one phenomenon here (ice fog, flat light, the cold lid, the gap wind, hoarfrost, a frost quake, an aurora night) |
| `/yoi land read <clumsy \| handy \| practiced>` | Sets the player's level for both reading skills |
| `/yoi land stand` | The chunk's `stand` record: age, type, beetle stage, lichen |
| `/yoi land hazard` | Avalanche H for the start zone in view, the face's rockfall rating, windthrow risk tonight |
| `/yoi land slide` | Releases the start zone in view as if it had triggered |
| `/yoi land heave <building>` | Rolls this year's frost heave on the nearest building at once |
| `/yoi land icehouse` | Ice mass left and the day it runs out at the current rate |

**Tuning.** All land dials sit in `yearofice-common.toml` under `[land]`, set by the world's realism preset (Minecraft development › World creation options and presets); a dial set by hand marks it "Custom". GameTests run on Standard.

| Key | Gentle | Standard | Brutal | Unit |
| --- | --- | --- | --- | --- |
| `land.hazard_scale` | 0.5 | 1.0 | 1.25 | × rockfall, avalanche and windthrow chances |
| `land.read_accuracy_bonus` | 0.10 | 0 | −0.05 | Added to every reading and sky-sign accuracy |
| `land.flush_scale` | 1.3 | 1.0 | 0.8 | × mushroom flush yield |
| `land.ice_melt_scale` | 0.8 | 1.0 | 1.1 | × ice-house melt a day |
| `land.mud_days_scale` | 0.75 | 1.0 | 1.0 | × length of mud season |

**Day length.** Every time and rate in this tab is per game minute, hour or day, so the world's day-length option (24–72 real minutes, default 24) rescales real time with no data change.

**Tests (NeoForge GameTests).**

1. **`yearofice:env/cold_sink`** (batch `m5`). On a scripted clear, calm night, a cold-sink column reads 3–6 °C below an open-ground column 60 blocks away. A south-face foot at 12:00 on March 15 reads 5–7 °C above open ground.
2. **`yearofice:world/avalanche`** (batch `m7`). 100 bot crossings of a 38° lee start zone after 40 cm of new snow trigger 15–25 slides. 100 crossings of a 25° slope with the same snow trigger 0.
3. **`yearofice:story/sky_lore`** (batch `m7`). Over 3 simulated years, a Practiced bot's evening readings match the next day's state 75–85% of the time, a Handy bot's 65–75% and a Clumsy bot's 50–60%.
4. **`yearofice:build/quinzhee`** (batch `m7`). At −35 °C air with a candle and a vent, a quinzhee's shelter value reads +28 °C ±1 (inside −7 °C). With no vent, a bot inside loses 8 ±1 Condition over 8 game hours.
5. **`yearofice:fire/controlled_burn`** (batch `m9`). 100 burns lit inside the window with a 10 m break reach spruce in under 5% of runs. 100 lit in a 7-day July dry spell with no break reach spruce in 40% or more.
6. **`yearofice:forage/wild_sweet_pea`** (batch `m5`). Three meals within 10 game days leave a bot's walk speed at 0.60 ±0.02 of normal for 14 game days, then back to 1.00.
