# Year of Ice — Generation Blueprint

Oct 18, 2026 · @Ruhan

The whole world of *Year of Ice* is generated from one seed through nine deterministic stages, so any agent can rebuild, test or tune one stage without touching the others. The engine is Minecraft: the game is a Minecraft Java Edition mod on NeoForge, and the Minecraft engine tab says how every system is built and overrides any browser, JavaScript or WebGL detail in this tab. There are no hostile mobs: no zombies, skeletons, creepers or other monsters, so every threat is a real animal or the land itself. Premise: a lone survivor at an abandoned Alaskan outpost lives off the land by hunting, fishing, trapping and keeping warm. Like Minecraft, there is no set ending: real 365-day years keep turning while the player grows from a campfire to a homestead that reshapes the valley.

## Scope and pillars

This blueprint covers everything that is generated rather than hand-placed: the map, its water and forests, resources, wildlife, the start site, the calendar and weather. It also fixes the survival numbers those systems feed, so generation and gameplay are tuned against the same targets.

**Pillars the generator must serve**

1. **Scarcity is the clock.** Food is hard to find by design; the map must never put easy calories next to the start.
2. **Respect the player's time.** Any chore that stops being interesting must get faster through tools, practice or blueprints. Tedium is the most common complaint players have about survival games, so this outranks realism.
3. **Explore, don't dig.** Iron and other key materials sit on the surface in specific terrain, so the player has to walk the land.
4. **The year changes the map.** Seasons alter temperature, daylight, ice, snow cover and which animals are active.
5. **Everything readable.** A player who studies the land (game trails, pools below falls, rock faces) should find food and shelter more often than one who wanders.
6. **Grow without an end.** There is no final goal. Every season should open a bigger project (a smokehouse, a boat, a trapline, a forge) and the world should visibly answer what the player builds and takes.

**Rules every stage follows**

- Deterministic: same seed + same version = same world, byte for byte. One seeded RNG (mulberry32) per stage, derived as `hash(seed, stageName)`, so changing one stage never reshuffles another.
- Layered: each stage reads earlier layers and writes its own grid or point list; no stage edits another's output.
- Tiled: layers are stored per 64 × 64-block tile (4 × 4 Minecraft chunks), so the server builds only what lies near a player (Minecraft engine › Size words).
- Validated: each stage ships a check that fails loudly (see Start site) so agents can reroll or fix a bad seed.
- Data-driven: species, biomes, items and recipes live in data-pack files, one per entry at data/yearofice/\<kind>/\<id>.json with snake\_case keys (Minecraft development › Data kinds), not in code.

**Three kinds of content.** Everything in the world is generated, authored or player-made, and each kind has one owner and one place it is stored.

| Kind | Examples | Made by | Stored as |
| --- | --- | --- | --- |
| Generated | Height, water, biomes, trees, iron, home ranges, trails, the day's weather | Stages 1–8 from the seed | Nothing; rebuilt from seed + generator version |
| Authored | Set-piece layouts, species, items, recipes, messenger lines, logbook pages | Data files | Data-pack files under data/yearofice/\<kind>`/` |
| Player-made | Felled trees, placed pieces, worn trails, items moved or dropped | The player | Minecraft chunk saves plus our attachments and SavedData (stage 9; Minecraft engine › What is saved where) |

**Five words for size.** Code, data and briefs use only these names for scale.

| Name | Size | Use |
| --- | --- | --- |
| Cell | 2 m × 2 m | One height sample |
| Chunk | 16 × 16 blocks, full height | Minecraft's loading and save unit; the old 64 m chunk is now the 64 × 64-block tile that stores generator layers |
| Core valley | 2 km × 2 km | Detailed at world creation; holds the start site |
| Region | 2 km × 2 km | Generated on the server's worldgen threads when a player comes within 600 blocks |
| World | Home valley: 8,192 × 8,192 blocks = 16 regions, ring 1 of 3 (World map › Three rings) | The core valley is one of the 16 |

**Every pillar has a test.** A pillar no test checks is only a wish. The balance simulator runs these on every build, next to the milestone tests.

| Pillar | Generator rule that serves it | Automated check |
| --- | --- | --- |
| Scarcity is the clock | No fishing spot of quality 0.8 or more within 150 m of the start; iron 400–1,000 m away | A scripted player who never leaves 150 m of the start falls below BMI 19 by day 30 on at least 9 of 10 seeds |
| Respect the player's time | Every repeat job has a faster method reachable in a later tier | The tedium check at every milestone |
| Explore, don't dig | Iron only on surface rock above 250 m, bog beds and cut banks; nothing below ground | Over 200 seeds: 0 resource nodes below the ground surface; 12–18 iron clusters on every seed |
| The year changes the map | Stage 8 writes ice, snow and activity windows for each day | M7: river ice forms between late October and mid-November in 3 simulated years |
| Everything readable | Trails, pools, rock faces and rust stains are drawn on the ground | A "reader" bot that walks toward visible sign finds its first 5,000 kcal at least 1.5× faster than a random walker over 50 seeds |
| Grow without an end | Renewing iron, regrowth, streamed regions, yearly rare events | A 5-year simulation never runs out of iron nodes or standing firewood within 2 km of the start |

## A playthrough

The first year should feel like the video: a desperate first week, a summer of getting ahead, a brutal winter that tests the stockpile, and a thaw that pays it off. Every later year starts from a stronger base with a bigger project.

&#91;embedded content: first-year playthrough · 8 moments · design target\]

| # | When | What the player does | What it requires | Built in |
| --- | --- | --- | --- | --- |
| 1 | Days 1–7, May | Opens the crate, lights a bow-drill fire before the cold night, follows the messenger's morning prompts, watches the crackers run out, loses the first fish on the bank, meets a bear at the river | Start site, onboarding, fire and bow-drill minigame, sleep rules, warmth and wet, calories and BMI, fishing fight, bear warnings and spray, morning prompts | M3–M6 |
| 2 | Weeks 2–4 | Lands a pike on live bait, gets the first hare from a trail snare, repairs the cabin, walks to the fire-lookout tower seen from the door and finds surface iron on the way | Minnow traps, snare placement, building pieces, set pieces and horizon landmarks, messenger map, surface iron | M1, M5, M8 |
| 3 | June–August | Keeps a smoky fire against the mosquitoes, works the salmon run, dries and smokes fish, plants the garden, cuts next winter's 30 trees, takes a wolf pup from a den while the pack is out hunting | Mosquitoes and smoke, seasonal fish events, preserving, farming, armload firewood, wolf taming, discoveries | M4, M5, M7, M9 |
| 4 | September–October | Builds a caribou camp on the tundra, stalks a moose downwind in the rut with a birch-bark call, follows its blood trail, hauls it home on a pack frame while the half-grown wolf growls at an approaching bear; the river freezes | Outposts, senses and wind, calling, arrow drop, blood trails, butchering, pack frame, wolf warnings, scent points, river ice | M6–M8 |
| 5 | November–January | Lives through −40 °C and 4 hours of light: ice fishing, a trapline on snowshoes, the stove cabin keeping the wood chore to 2 armloads a day, wolves testing a weak player | Cold model, snow depth, ice, winter gear, warm rooms, wolf pack AI with chase break-off, night lighting | M6–M8 |
| 6 | February–March | Rations pemmican as stores run low, then finds a winter-killed moose and races the wolves to it | Store and spoilage view, rationing, rare events, late-winter scarcity tuning | M4, M7 |
| 7 | April | Hears the ice crack at breakup; a spring flood washes out the low cache but exposes new iron; earns "See the thaw" | Breakup, thin ice, rare events, renewable iron, ambitions | M7 |
| 8 | Year 2 onward | Paddles a canoe upriver to new regions, stamps a second cabin from a saved blueprint, lights the forge, raises a wolf sled team, gets goats and hens in a supply drop | Traversal, blueprints, outposts, iron and forge tier, wolf team, radio loop, livestock, streamed regions | M8, M9 |

**Guaranteed, likely or possible.** The table is a design target, not a script. Testers grade each moment by how sure the world makes it. A missing moment is then either a bug or bad luck, and the label says which.

| # | Guaranteed by the world | Likely (share of 100 seeds, good scripted player) | Possible, never forced |
| --- | --- | --- | --- |
| 1 | The 12 start checks; the guided first night; a bear in view within the first 3 days | First fish by day 4 on 80% | Losing a fish on the bank |
| 2 | A landmark visible from the door; iron 400–1,000 m away; pike in every lake and slough | First pike by day 21 on 70%; first hare by day 14 on 85% | Finding iron on the first walk to the landmark |
| 3 | A salmon run every year; one wolf den on every seed | Den found by June 30 on 50% | Taking a pup |
| 4 | One moose range within 1,200 m; the rut every September | Moose taken by October 31 on 60% | A caribou herd (0–8 per year); the wolf warning off a bear |
| 5 | Freeze-up every year; nights below −30 °C every winter | A night below −40 °C in about 40% of winters | Wolves testing the player |
| 6 | Late-winter scarcity from the tuning targets below | Stores under 40,000 kcal by March 31 on 70% | A winter-killed moose (30% a year) |
| 7 | Breakup every April | Ice-crack sounds heard on 100% | A spring flood (25% a year) and the iron it exposes |
| 8 | Outer regions stream in as the player walks | A second cabin by the end of year 2 on 60% | Goats and hens, which need the radio loop |

**Checkpoints for a good player.** The balance simulator's scripted good player must land inside these bands on Standard. Landing outside a band flags the systems in that moment's "What it requires" column, not the player.

| End of moment | Date (game day) | Weight | Stored food | Firewood | Shelter |
| --- | --- | --- | --- | --- | --- |
| 1 | May 7 (7) | 70–73 kg | Under 1 day | Cut daily | Fire, debris bed, lean-to |
| 2 | May 28 (28) | 67–71 kg | 1–3 days | Cut daily | Outpost cabin repaired (tier 2) |
| 3 | August 31 (123) | 69–73 kg | 60,000–120,000 kcal | 33 trees split and covered (330 armloads) | Log cabin walls up |
| 4 | October 31 (184) | 71–75 kg | 200,000–300,000 kcal | 330 armloads seasoned | Chinked cabin with a stove (tier 3), raised cache |
| 5 | January 31 (276) | 68–72 kg | 80,000–160,000 kcal | 130–170 armloads left | Same |
| 6 | March 31 (335) | 64–68 kg | 0–40,000 kcal | 10–50 armloads left | Same |
| 7 | April 30 (365) | 65–69 kg | Spring foods only | 0–20 armloads | Same |

The first week loses about 5 kg: crackers give 3,800 kcal against roughly 19,600 kcal burned. The year ends near 67 kg, close to the video's 77 to 67 kg. The 33-tree target is the doc's 30 trees a winter plus a 10% margin for April.

**Year 2 and later.** Each year should open one big project that the previous year made possible, and each project moves the pressure somewhere new. Morning prompts and ambitions point at these; none is required.

| Year | Big project | Made possible by | New pressure it creates |
| --- | --- | --- | --- |
| 2 | Canoe and the upriver regions | Birch bark and spruce gum stocked; homestead tier | Two bases to stock; a capsized canoe soaks everything |
| 2 | Second cabin from a saved blueprint | The first cabin | Twice the firewood; a store to defend at each base |
| 2–3 | Forge and iron tools | Iron hauled from the north range | Charcoal burning; trips to the outcrops |
| 3 | Fish camp with a fish wheel and smokehouse | Hide tier, a summer of fish | The run shrinks four years later if the wheel runs all season |
| 3–4 | Wolf sled team | Three Companion wolves | 3–6 kg of meat or fish a day for the team |
| 4+ | Goats and hens from supply drops | Radio repaired, seasons of logs | Barley for feed; a warm barn; predators at the pen |
| Any | Map all 16 regions | Snowshoes, outposts | Winter trips far from a warm room |

## What the experience needs beyond systems

The systems in this doc make the playthrough possible; these make it feel like the video. None has its own section yet, so each gets a first target here.

| Need | Why the playthrough needs it | First target |
| --- | --- | --- |
| A first-person body | Chopping, fishing and the bow drill are felt through the hands | Visible hands and held tool; swing, reel and draw animations; breath fog below 0 °C |
| Cold you can see | The player should feel cold before reading a number | Frost creeping in from the screen edges, a shivering camera, slowed steps when hypothermic |
| Sound | Danger is heard first: a bear's huff, a wolf howl, ice cracking | About 60 sound events, generated or found under a licence that allows redistribution: wind, river, fire, footsteps by surface, 8 animal calls |
| Onboarding | The first hour decides whether a player stays (studio lesson) | Messenger-led tasks with an objective beacon through the first night; keycap prompts |
| Pacing tools | A 365-day year needs ways past the dull hours | Sleep, wait by the fire, and "work until done" on long jobs with the clock sped up |
| One early scare | The video's best moments are surprises at the water | A bear always appears at a distance within the first 3 days; only its timing is scripted |
| Content volume | Each moment needs its own things to see and hold | About 10 animals, 60 items, 25 building pieces, set dressing for 16 regions |
| A record of the run | Long runs need memory | Messenger journal: auto-logged firsts, a weight graph, the base seen from above over time |

**Navigation.** The messenger shows a map that fills in as the player explores, with a breadcrumb track and up to 20 waypoints. After the first night, the world itself shows no objective markers.

### The first-person body

The hands are always on screen in the lower third, holding what the hotbar selects. Every action that costs calories has an animation whose speed matches the work rate, so a tired player sees the swing slow down.

| Action | Animation | Length | What it shows |
| --- | --- | --- | --- |
| Idle | Hands sway with breathing | 4 s cycle; 2 s when cold or after running | Breath rate tracks exertion |
| Walk, run | Head bob | 1.8 Hz walk, 2.6 Hz run; 30% less in snowshoes | Load: bob drops and slows over 15 kg |
| Chop | Hatchet swing and recoil | 0.8 s; 1.2 s below 3 energy bolts | A knot jars the hands and adds a swing |
| Knife work | Draw-cut loop | Loops until the progress ring fills | Hands bloody after gutting until washed |
| Draw the bow | String to the cheek | 1 s to full draw | Below the draw-weight strength, it stops at 60% and shakes |
| Cast and reel | Charge back, flick, hand over hand | Charge up to 1.5 s | The rod tip bends with line tension |
| Bow drill | Stroke cycle | 0.5 s per stroke, synced to the rhythm minigame | Smoke rises from the fireboard at the right rhythm |
| Blow on tinder | Hands cupped to the face | While the key is held | The ember brightens with each breath |
| Carry a log | Log on the shoulder | — | Camera 10 cm lower, tilted 5°; both hands busy |
| Eat, drink | Hand to mouth; bottle tipped | 2 s | Bottle ice rattles below −10 °C |

**Breath fog.** Below 0 °C a puff shows every 3.5 s walking and every 1.5 s running. Below −20 °C it is denser and lingers 2 s. After 10 game minutes outside below −30 °C, frost rims the hood edge at the top of the screen.

**Skin and clothes.** Hands turn red at frostbite risk and waxy white when frostbitten. Wet sleeves are darker and drip. The player's own footprints stay behind them in mud and snow, as the animals' do.

### Cold you can see

The screen tells the player they are cold before the messenger does. Effects are keyed to feels-like temperature and the three hypothermia stages from Survival tuning.

| State | Screen | Camera and body | Sound |
| --- | --- | --- | --- |
| Feels-like below 10 °C, warmth falling | Frost creeps 5% in from the corners | Breath fog | Wind a little louder |
| Shivering (warmth under 30) | Frost 20%, faint blue tint | 0.3° camera tremor; aim sway from shaking hands | Chattering teeth, shaky breath |
| Slowed | Frost 40%, colour 40% washed out | Steps at 70% speed; turning lags | Ambience muffled; slow heartbeat |
| Collapse close | Frost 70%, tunnel vision | Camera sinks 20 cm; stumbles | Heartbeat and wind only |
| Rewarming at a fire | Frost melts from the centre out over 20 s | Tremor fades over 60 s | Fire crackle comes up |

A "reduce screen effects" option halves the frost and removes the tremor. The messenger vitals still show the same state, so nothing is lost.

### Sound

The first target is 60 Minecraft sound events in the `yearofice` namespace (Minecraft engine › Look and sound). Each is made by the studio's synthesis scripts or taken from a library whose licence allows redistribution in a modpack (CC0, CC-BY or MIT) and listed in the credits file; vanilla sounds may be played at runtime but are never copied into the jar (Ruhan, 2026-10-19). Licensed field recordings may replace the ambience beds, as in the video; the danger sounds stay generated so their timing is exact.

| Group | Count | Sounds |
| --- | --- | --- |
| Wind and weather | 8 | Breeze in spruce, strong wind, blizzard roar, rain on leaves, rain on a roof or tarp, thunder, mosquito whine, dawn birdsong bed |
| Water | 6 | River (pitch by width), creek riffle, waterfall, lake lapping, splash, underwater muffle |
| Ice | 4 | Lake boom on cold nights, creak underfoot on thin ice, breakup grinding, chisel strike |
| Fire | 5 | Catch, crackle by fire state, wet stone pop, hiss in rain, coals settling |
| Footsteps | 10 | Gravel, forest duff, bog squelch, dry leaves, powder snow, crust crunch, ice, wood floor, wading, stick snap |
| Tools and work | 10 | Hatchet chop, tree crack and fall, split, knife cut and scrape, bow creak, arrow release, arrow hit, bow-drill stroke, line cast and reel, mallet on peg |
| Animal calls | 8 | Bear huff and jaw-pop, wolf howl, wolf growl, moose grunt, beaver tail slap, squirrel scold, raven call, loon wail |
| Body | 5 | Breathing by exertion, chattering teeth, heartbeat, eating, drinking |
| Messenger | 4 | Prompt chime, alert, low battery, map open |

**Danger is heard first.** Each danger sound carries farther than its source can be seen in forest. A bear's huff carries 60 m, a moose grunt 100 m, ice cracking 500 m and a wolf howl 3 km. The waterfall is heard from 300 m, which is how a player finds the best fishing pool without a map.

### Pacing tools

At the default day length a game hour is one real minute, so skipping time must be easy but never a way to skip a cost. The speeds below are multiples of that default world clock: Sleep and Wait run 1 game hour per real second and Work until done 20 game minutes, and they stay fixed when the player picks a longer day, which only slows the world clock (Open decisions). Calories, water, warmth, spoilage and fire fuel all accrue at the true game rate during any skip.

| Tool | Start it | Clock speed | Runs until |
| --- | --- | --- | --- |
| Sleep | Z, when energy is below 7 or it is night | 60× | Rested, or woken by the Sleep rules (cold, wet, hunger, rain without a roof, an animal) |
| Wait by the fire | Z within 3 m of a fire | 60× | The chosen 1–8 h, "until dawn" or "until the fire needs wood" |
| Work until done | Hold E on a queued job, then Z | 20× | The queue empties or materials run out |

**Every skip stops for the same reasons.** A predator comes within 150 m or other game within 50 m. A fire drops a state, or the weather turns to rain, storm, snow or blizzard. A new hypothermia stage, a thirst or hunger warning, or any messenger alert also stops it.

Skipping cannot start while a predator is aware of the player, while in water or while bleeding.

### The early scare

A bear always appears at a distance in the first 3 days. Only the moment is scripted; once it appears, it is an ordinary bear running the normal AI.

1. The bear is drawn from the one home range the start check requires to overlap the site. It is a grizzly if that range is a grizzly's, otherwise a black bear.
2. The window opens at 06:00 on day 2 and closes at 21:00 on day 3. It never opens during the guided first night.
3. It triggers the first time the player is within 60 m of the main river or lake. The spot must have a clear view 80–150 m along the bank or across the water.
4. The bear spawns there, unaware and foraging head-down, with the wind blowing from the bear to the player so it cannot smell them.
5. The messenger shows two lines: "Bear across the river, {d} m. It hasn't seen you." / "Back away. Keep the spray in reach."
6. It forages for 10–20 game minutes, then walks off along its range. If the player closes to 50 m, it notices them and the encounter table in Danger in detail applies.
7. If nothing has triggered by 20:00 on day 3, the bear appears 120 m from the player. It stands beside the cleaning spot if there is one, otherwise on the nearest bank.

The journal logs it as "First bear." The bear then keeps its normal home range, so the player knows where it lives.

### Content volume

The first target fixes what each count means, so art, data and code agents build the same list.

- **10 animals:** the eight species in How animals act (snowshoe hare, red squirrel, spruce grouse, beaver, moose, caribou, wolf, grizzly), plus black bear and raven. The other ten land with M6.
- **25 building pieces:** foundation, log wall course, floor, roof panel, door, hide window, stove, chimney, shelf, bed; debris bed, bough bed, lean-to, reflector wall, cooking stand, drying line, woodpile, chopping block, cleaning spot, meat pole, latrine, chest; fire ring, smoke rack, raised cache.
- **60 items:** 12 tools, 8 weapons and ammunition, 14 foods, 16 materials, 6 clothing pieces, 4 containers and lights. The crate kit counts toward these.
- **Set dressing for 16 regions:** one prop kit per biome (8 kits: driftwood piles, sphagnum hummocks, beaver-chewed stumps, cone middens, lichen rocks and so on), plus the set pieces in Biomes and resources.

### The run journal

The journal is a page in the messenger that writes itself. It never asks the player to type, except the one-line "why I left" after an SOS.

| Entry | When it is written | Example |
| --- | --- | --- |
| Day line | Every morning | "May 14. Rain, 4 °C to 11 °C. 72.4 kg. Hare in the north snare." |
| First | The first of each species caught or seen, each building, each tier | "First pike: 4.1 kg, the lake inlet." |
| Near miss | Warmth or Condition under 20, a bluff charge, a fall through ice | "Through the ice at the bend. Fire in 9 minutes." |
| Ambition | Each ambition earned | "See the thaw." |
| Year card | Each May 1 | The video-style card from Gameplay ideas |

**Weight graph.** One weight per day, kept for every year of the run (2 bytes a day). The graph marks SOS flights, rare events and the year's coldest day.

**The base from above.** Every edit in the save already carries its game day. So the journal can render the home area as it stood on any date.

It shows a 256 m × 256 m top-down view at each month's start, with a slider. No images are stored, so the 5 MB save budget holds.

### The messenger map

The map reveals 60 m around the player on foot. From any point 100 m above its surroundings, including the lookout tower, it reveals 1,000 m.

Breadcrumbs drop every 20 m and fade after 30 game days. Traps, nets, tip-ups, found set pieces and logged fishing spots pin themselves; the player adds up to 20 waypoints from 6 icons.

**Battery cost.** Vitals, forecast and prompts draw 0.25% an hour, breadcrumbs 0.2% an hour, and the open GPS map 3% an hour. A week of normal use with about 8 hours of map time spends one full charge, matching the 7-day battery.

## Lessons from survival and simulation players

Players' biggest complaint about survival games is tedium: repeating the same chore at the same slow speed after it stopped being interesting. The second is a world that runs itself or goes quiet. Nineteen lessons from player discussions are now built into this doc; two were left out on purpose.

| Lesson | Heard from | Change made, and where |
| --- | --- | --- |
| Repeat work must get faster by mid and late game | Reddit (several), The Forest reviews | Firewood in armloads (about 30 trees a winter), blueprints, queued jobs, practice, floating logs downriver (Crafting, Shelter) |
| A real threat you sometimes have to run from, but not a nuisance | Reddit, The Forest reviews | Predators break off chases, warn before charging, and can be turned down in settings (How animals act, Realism settings) |
| Needs that keep you alert without constant feeding | Reddit | One or two real meals a day are enough (Food systems) |
| Always a goal; good sims generate goals from the world | Reddit, Unity thread | Morning prompts built from the world's state (The open world) |
| Balance what runs by itself against what the player does | Unity thread | Fish wheel, wolf team and blueprints automate, while the pressure moves to new needs (The open world) |
| Finite resources hurt long play | Unity thread | Iron renews from bogs and floods (Biomes and resources) |
| Progress must not make survival irrelevant | Survival-design essay | "Survival, not thrival" pressure shifts (The open world) |
| Sleep should be planned, not a free time skip | Survival-design essay | Sleep rules: only when tired, woken by cold or danger (Survival tuning) |
| Systems that collide make the best stories (Valheim's smoke) | Reddit | Systems that push on each other; summer mosquitoes (The open world, Calendar) |
| Procedural maps can feel samey and empty | Reddit (a long debate) | Valley archetypes, hand-made set pieces, ±20% yields, a landmark on every horizon (Biomes and resources) |
| Travel should get better and stay fun; connect your bases | Reddit | Traversal table, outposts, cut trails and bridges (The open world) |
| Not feeling alone; companions that act alive | Reddit | Wolf taming (The open world) |
| Reward everything; every upgrade needs a target | Reddit | Crafting rules (Crafting and progression) |
| Quality of life: storage, sorting, gear slots, optional grid | Reddit | QoL rule (Crafting and progression) |
| Clear early guidance and a way to navigate | The Forest reviews | Messenger map with breadcrumbs and waypoints (above) |
| Clunky building and lost progress | The Forest reviews | Ghost preview, self-levelling foundations, refunds; 3 rolling save backups (Shelter, Engine rules) |
| Let players choose how real it is | Unity thread | Presets and sliders (Survival tuning) |
| The endgame is a comfortable life to keep up | Reddit | Garden, wolf team, goats and hens from supply drops (Crafting diagram) |
| Surprise keeps long play fresh | Unity thread | Rare events table (The open world) |

**Left out on purpose:** player-versus-player (solo first; co-op after 1.0 stays cooperative) and physics-based building (Minecraft blocks do not fall as rigid bodies, so it would be a large custom system; revisit after M8).

Sources: [What makes a good survival game? (r/SurvivalGaming, text pasted by you)](https://www.reddit.com/r/SurvivalGaming/comments/1tlgsio/what_makes_a_good_survival_game_examples_of/) · [What makes a good simulation game? (Unity Discussions)](https://discussions.unity.com/t/what-makes-a-good-simulation-game/554351) · [Three Pillars of Great Survival Games](https://tripharrison.substack.com/p/three-pillars-of-great-survival-games) · [The Forest review summary (Vaporlens)](https://vaporlens.app/app/242760/the_forest.md)

## Gameplay ideas from the video

The video's best moments come from design choices, not from any single mechanic. Forge Labs states several of them on camera: the body is the gate to big game, the mod makes the player vary what they hunt, and the pace is slow on purpose. Year of Ice adopts all of these except the 100-day year; it keeps 365 days.

| Idea | What the video showed | How Year of Ice uses it |
| --- | --- | --- |
| The body gates progression | The heavy bow could not be fully drawn at low strength, so the first bear arrow only wounded it. Months of squirrels, fish and berries were spent building enough strength for a moose. | Every bow has a draw weight. Below the strength it needs, full draw is capped at 60%: arrows fly short, wound instead of kill, and the bow shakes. Strength comes from Condition and body weight, so small game is the road to big game, not filler. |
| Variety is enforced | After the beaver, no beaver spawned again; the mod rations animal opportunities so the player keeps changing what they do. | An encounter director sits on top of the population model. After a big kill, that species' encounter chance in the home range drops for 30–60 days, so fishing, traps, foraging and the garden carry the gap. |
| Seasons are the clock you feel | Falling leaves, shorter days and the moose rut made him anxious that winter was coming. | Visible tells for each season (leaf color, darkness at 5 pm, first skim ice) and a messenger estimate of days until freeze-up. |
| Small mistakes are the drama | Forgetting to drink until he blacked out, leaving bow staves on the fire, forgetting bait and the snare, letting fires die. | Nothing is silently automated, but everything warns once. The cost of a mistake is hours and calories, not a lost save. |
| Desperate trades with delayed bills | Raw squirrel, a mystery mushroom, dirty water, raw fish for vitamin C, straight animal fat. | Risky food pays now and bills later: parasites and food poisoning show 1–5 days after eating, so the player has to remember what they did. |
| The world hands out small miracles | Feathers snagged on felled trees, a salmon grabbed by hand on the waterfall, a dead grouse on the trail, spawned-out salmon drifting downstream. | Floating items drift and collect at snags and log jams. Rare finds are seeded along rivers and trails, and they are always real objects the player can pick up. |
| Play with the world physically | Felling trees across a river as bridges, logs rolling over the falls, a falling tree smashing the ice. | Trees fall with physics, logs float and jam, and fallen trunks become bridges or dams. Rolling a rock down a slope is allowed just because it is fun. |
| Wild animals become characters | Peeper the rabbit got used to him, was hunted by a wolverine, and survived the winter. | Any animal that stays near camp for a few days habituates, stops fleeing and can be named. Predators hunt prey in plain view, and the player decides whether to step in. |
| Pride in the base | Wood slabs over cobble, lanterns, a mud-brick chimney, a moose-head trophy, a woodshed built "because I like it." | Decoration and trophies raise Spirits. A woodshed dries firewood twice as fast, so the build he wanted to justify gets a reason. |
| A slow, quiet pace | He chose a relaxed video over a fast one; long bobber-watching let his mind wander. | Long quiet stretches are allowed. The ambience uses real field recordings, as the video used wolf howls from his own Arctic trail camera. |
| Tapping out is an ending | A BMI of 17, severe frostbite or scurvy would end the run; he talked about knowing when "this is enough." | Hardcore mode extracts the player at BMI 17 or from an untreated severe condition. Any mode can call SOS from the messenger, with a one-line "why I left" entry in the journal. |
| Real dates | He noticed when the in-game date matched the real one, and when he had missed Christmas. | An optional real-date mode starts the world on the real date and shows holidays and birthdays in the messenger. A game day stays 24–72 real minutes long (Open decisions), so the two calendars drift apart and the messenger shows both dates. |
| A year in numbers | Weight from 170 to 147.6 lb, 10 fish, the biggest a 15.7 lb sockeye, coldest −35.7 °C on day 70. | Each May 1 the messenger shows a year card: weight in and out, kills and catches, the biggest fish, the coldest day, kilometres walked, and near-death moments. |
| Group survival, last one standing | A planned multiplayer version where nobody knows who is still in. | Recorded as the co-op design for after 1.0 (Ruhan, 2026-10-19: solo first): separate valleys, hidden status, and the last one in wins. |
| One engine, many places | The same mod would move to other environments, such as Africa. | Biomes, species and weather are data files, so a new region is a data pack, not a new game. |

## Video checklist

All 19 chapters were checked against this blueprint. Most of the video's mechanics were already covered, including the messenger and its vitals, the bow drill, fire states and spread, the bushcraft bed, surface-only iron, gill nets and minnow traps, the fishing minigame, bear spray, wounds and bandages, smoking, spoilage that slows in the cold, the thaw, snowshoes and ice fishing. The rows below were missing or only implied, and each now has a rule.

| Mechanic | Chapter | Rule in Year of Ice |
| --- | --- | --- |
| Auto-eat from the food cache | 13, 16, 19 | Food in a cache is eaten automatically at a chosen ration: Starvation, Lean, Normal or Feast. The cache shows days left at that ration. Feast builds strength fastest before a big hunt; Lean stretches stores but loses weight. |
| Mice in the cache | 19 | In winter, mice get into any cache without traps around it. They eat 2–3% of the food a week and leave droppings that cause food poisoning. Three deadfall traps (flint trigger) around a cache stop them. |
| Scent from stored meat | 18, 19 | A full cache gives "strong scent may draw bears" in fall and draws wolves and wolverines in winter. Lower rations and smoked rather than raw meat cut the scent. |
| Wolves besiege the shelter | 19 | A hungry pack drawn by scent circles the shelter and batters the door. Striking a wolf makes it run; fire and a barred door hold them off. |
| Fire spreads from a packed hearth | 3, 19 | A fire "packed too full" warns once, then may spread to nearby wood pieces while the player is away. A stone or cave shelter takes damage only outside. |
| Hardwood and softwood | 19 | Softwood (spruce) lights fast and burns out quickly. Hardwood (birch) holds a fire through a −40 °C night. Fire settings run from Low to Steady to Hot. |
| Carry the carcass | 16, 17 | Small and medium carcasses (beaver, fox, a lamb) are carried whole with an empty hand at walking pace, then set down and dressed with a knife. |
| Cuts and organs | 16, 17 | A big kill yields meat, fat, heart, liver, kidney and tongue, each with its own spoil timer. Liver goes first: 0.7 days for liver against 2.3 for meat on a cool fall day. |
| Rendered fat makes cured hide | 17, 18 | Fat is rendered in the pot, then rubbed into scraped hides to cure them. Four scraped small pelts make a poncho (Clothing); cured moose hide makes a coat, bear hide makes pants and a hat, beaver makes a hat. |
| One arrow per craft | 12 | Each arrow is crafted singly and must be recovered. Blunt arrows for small game need game feathers from loons, grouse or ptarmigan; broadheads for big game need flint or iron. Bow staves season on the fire before use. |
| Throwing rocks | 12, 14 | Any fist-sized stone is a throwing weapon that stuns squirrels and grouse. Felling a tree with a squirrel in it works, but costs more calories than the squirrel gives. |
| Grabbing fish by hand | 12 | Salmon jumping a falls or stranded in a shallow pool can be grabbed by hand. |
| Baited snares | 11 | A cricket, berries or fish guts on a snare raise its catch chance by 50%. |
| Moose call and tree stand | 17 | A birch-bark moose call (rawhide strips, cordage, a dowel) can pull a bull from 800 m during the rut. A tree stand on ladders hides the player's scent and outline. After a hit, the player waits, then follows blood sign; a wounded moose may get up and charge. |
| A kill draws predators | 17, 18 | A carcass draws bears, wolves and wolverines within a day. A chest placed at the kill site holds meat while the player makes trips home. A carcass is also bear bait for a hunter waiting in a tree. |
| Seasonal alerts | 16, 18 | The messenger flags shifts in behavior: "Moose rut has begun: bulls aggressive," "Bears feeding heavily," "Mosquitoes hatching," "Blizzard and cold snap for 3 days." |
| Blisters | 12, 16 | Walking far in wet boots gives a blister that slows the player until it is bandaged. Socks or dry boots prevent it. |
| Blacking out from thirst | 15, 16 | Severe dehydration knocks the player out for half a day, wherever they are. |
| Adrenaline sprint | 13 | A weak player who cannot run gets one 10-second sprint when chased by a bear. |
| The raft | 16 | Logs and cordage make a raft that drifts with the current and is poled upstream. Fighting a big fish or a beaver from it can tip a weak player into deep water, where they can drown. |
| Beavers shape the river | 16, 19 | Beaver dams grow over seasons and flood new ponds. Beavers are territorial: they slap their tails as a warning, then bite. |
| Raw fish for vitamin C | 19 | Raw fresh fish gives a little vitamin C, which cooking destroys. Each raw meal has a chance of parasites that show days later. Spruce tips return in spring. |
| Mushrooms near camp | 14 | In late summer, brown mushrooms come up around a camp. Stew gives 180 kcal and some water; a lookalike causes stomach trouble. |
| Headlamp in deep cold | 18, 19 | Battery light fades below −30 °C, so a lantern or a fire matters in midwinter. |
| Sleep debt | 18 | Staying up through the night to process a kill builds sleep debt; after 2 days without sleep, every task slows. |
| Losing track of traps | 19 | Every trap, net and tip-up is pinned on the messenger map, because he forgot how many he had set. |
| Spawned-out salmon | 16 | In late fall, dead salmon drift downstream. They are "too far gone" to eat, but they draw bears and eagles to the river. |

Source: [1 Year in Alaska in ULTRA REALISTIC Minecraft](https://www.youtube.com/watch?v=jccqEa0Qd40), Forge Labs, full transcript of all 19 chapters.

## Pipeline at a glance

&#91;embedded content: generation pipeline · 7 creation stages, 2 runtime stages\]

Stages 1–7 run on Java worker threads (the yearofice-gen pool) when a world is created, and for each region as the player first comes within 600 blocks (Minecraft engine › The nine stages in Minecraft). Stages 8 and 9 run during play and never change the generated layers; they only add the day's weather and the catch-up of loaded chunks. In Minecraft, stage 9 is Minecraft's own chunk saving, with no edit lists.

**The nine stages.** The `stageName` column is the string hashed into each stage's RNG, `mulberry32(hash(attemptSeed, stageName))`. Budgets are for a mid laptop, on the server's worker threads.

| # | Stage (`stageName`) | Runs | Reads | Writes | Budget | First built in |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Seed (`seed`) | Creation | User seed, version, attempt number | World parameters record | Under 1 ms | M0 |
| 2 | Landform (`landform`) | Creation, then per region | Stage 1 | Height, hardness, coarse world height | 0.3 s | M0 |
| 3 | Erosion (`erosion`) | Creation, then per region | Stage 2 | Eroded height, sediment depth | 2–4 s | M0 |
| 4 | Water and rock (`water`) | Creation, then per region | Stages 1–3 | Water bodies, rivers, falls, fishing spots, flood lines, rock faces | 0.5 s | M1 |
| 5 | Places (`places`) | Creation, then per region | Stages 1–4 | Set pieces, landmarks, caches, licks, footprint masks | 0.3 s | M1 |
| 6 | Cover (`cover`) | Creation, then per region | Stages 1–5 | Biomes, trees, plant patches, resource nodes | 0.8 s | M2 |
| 7 | Life and start (`life`) | Creation; start site in the core only | Stages 1–6 | Home ranges, trails, dens, lodges, start site | 0.4 s | M3, M6 |
| 8 | Weather (`weather`) | Each May 1, then daily | Stage 1, stage 4 water bodies, year number | Year table; daily snow, ice and daylight | 20 ms a year | M2, M7 |
| 9 | Edits (`edits`) | On chunk load and every change | All layers, the save | The visible chunk; the save's edit lists | Under 5 ms a chunk | M2, M8 |

World creation must finish in under 8 s with a progress bar naming each stage. A streamed region must finish in under 3 s.

**Each stage in detail.** Every stage reads only what its row names and writes only its own outputs. Parameters live in `worldgen_params/valley.json`; the algorithm details for stages 2–4 are in Terrain and hydrology, and for stage 7 in Wildlife and fish.

| # | Inputs | Algorithm | Outputs | Check (fails the attempt) |
| --- | --- | --- | --- | --- |
| 1 Seed | Player seed (text or a random 32-bit number), generator version, attempt k | attemptSeed = hash(seed, k). Draw the archetype (equal odds), the main river's entry and exit edges, its virtual upstream catchment (64–200 km²), the hare-cycle phase (year 0–9) and a role for each outer region | A frozen parameters record | Every value inside its range; over 200 seeds each archetype appears 25% ±5% (M0) |
| 2 Landform | Parameters | Coarse height for the whole world at 32 m (250 × 250), then the core at 2 m by Terrain step 1 and archetype shaping. Hardness (0–1) from its own noise, raised under the ridged mask | Height (Float32, 1,024²), hardness (Uint8), coarse height | No NaN; elevation 120–900 m; valley floor 130–170 m; same height hash in Chrome and Firefox (M0) |
| 3 Erosion | Height, hardness | Droplet erosion, then thermal erosion on soft cells | Eroded height, sediment depth (Uint8, 0.1 m steps) | No NaN; eroded and deposited volume within 2%; hard cells keep slopes over 40° |
| 4 Water and rock | Eroded height, sediment, coarse height | Coarse world river network first (D8 at 32 m), fixing border crossings and inflow. Then Terrain steps 3–6, braids or oxbows, flood lines, cut banks and fishing spots | Water type, water surface, flow direction, catchment, current; bed carve depth in its own layer; lists of falls, pools, fishing spots, springs, rock faces, water bodies | Water surface never rises downstream; every river reaches the edge or a lake; one outlet per lake; the archetype's minimum falls; at least 20 shelter faces |
| 5 Places | Stages 1–4, region roles | A world-wide landmark pass on coarse height, then each set piece in `setpieces.json` in priority order takes its best-fitting cell that keeps spacing | Set pieces (id, variant, position, rotation, footprint mask), landmarks, trapper caches, mineral licks | No set piece in water or on a slope over 25°; no footprints overlap; a landmark visible from 2 km in every region (M1) |
| 6 Cover | Stages 1–5 | The biome rule table, Poisson-disc trees, seasonal plant patches, resource nodes | Biome per cell (Uint8); lists of trees, patches, iron clusters, bog iron beds, flint, clay, chaga, deadwood | 12–18 iron clusters; no node below ground; deadwood 3% ±0.5 of trees; the core holds open water, riparian, spruce taiga and rock face |
| 7 Life and start | Stages 1–6 | Home ranges, summer and winter trails, dens, lodges and ponds; then, in the core only, the start-site score and checks, the outpost and its kit | Range, trail, den and lodge lists; pond overlays; the start-site record | Species counts inside the per-map ranges; all 12 start checks |
| 8 Weather | Parameters, water-body list, year number | Each May 1, draw 365 day records plus the year's run sizes, frost dates and event dates; each day, update snow per class and ice per water body | A year table of about 12 KB, rebuilt on load and never saved | M7 ice dates; over 100 simulated years each monthly mean within 1.5 °C of normal |
| 9 Edits | Layers, the save | Build a loading chunk from the layers, hide what lies under footprint masks, apply edits in order, then run catch-up rolls | The visible chunk; appended edits (chunk, game day, minute, type, data) | Save and reload give the same state hash for every chunk; a 5-year save under 5 MB (M9) |

**Region roles.** Roles guarantee what the open world promises. There is always at least one alpine tundra region (caribou), one canyon region and one old mining creek. There is one big-lake region, or two when the core is not the lake archetype; the four north-edge regions are always mountains, and the rest are mixed.

**Catch-up rolls are seeded.** Stage 9's rolls use `hash(attemptSeed, "edits", chunk, day)`. So reloading a save never rerolls a trap's catch or a cache raid.

**Rerolls.** A failed check in any of stages 1–7 at world creation starts attempt k + 1, sharing the start site's budget of 20. If attempt 20 fails, the game shows "World generation failed" with the seed and the failing check, and logs both. Over 50 seeds, every world must pass within 20 attempts (M3).

**Streaming regions.** A region runs stages 2–7 (without the start site) from the global coarse layers and its own area only. It never reads a neighbour's detailed layers, so regions come out the same in any visiting order.

The last 64 m inside each border blends detailed height toward a profile computed from coarse height. Each river must cross the border within 16 m of its coarse crossing point.

A region that fails a check cannot reroll the world, which already exists. It retries with sub-seed hash(attemptSeed, stageName, rx, ry, j) for j = 1–5, which gives the same answer on every machine.

**Footprint masks.** A stage that needs bare ground (a set piece, the outpost, a beaver pond) writes a mask in its own layer. The chunk builder hides any earlier point (a tree, a plant, a node) under a later mask, so no stage edits another stage's list.

## Terrain and hydrology

These stages build each 2,048-block region of the 8.2 km home valley, the core region B3 at world creation and the other 15 on approach (World map is the authority on world size; the 64 km Interior is in The open world; Minecraft development › World and worldgen builds them). The core is a river valley: mountains along the north edge, a main river running roughly east–west through the middle, and side creeks feeding it. That layout guarantees cliffs, falls, lakes and flat riverbanks within walking distance of each other.

1. **Landform.** Height = base noise + ridged mountain noise masked to the north third + a broad valley trough that the main river will follow. Domain-warp the result so ridges and banks don't look grid-aligned.
2. **Erosion.** Run droplet hydraulic erosion, then thermal erosion at a 35° talus angle. This makes real gullies, gravel bars and scree slopes instead of noise blobs.
3. **Depressions to lakes.** Priority-flood fill; any filled basin deeper than 1.5 m and larger than 2,000 m² becomes a lake, smaller ones become bog.
4. **Rivers.** D8 flow direction and flow accumulation; a cell becomes river once its catchment passes the threshold. Width grows with the square root of catchment; carve the channel and set depth from width.
5. **Falls and pools.** Where a river drops more than 3 m over 6 m of run, mark a waterfall and a pool at its foot. Pools, bends and lake inlets become fish-holding spots (see Wildlife and fish).
6. **Rock exposure.** Slopes above 40° become bare rock faces; faces taller than 6 m are shelter candidates (dig-in bases like the one in the source video).
7. **Ice and snow layers.** Not baked in: written per day by the calendar stage (river freeze-up, snow depth).

| Parameter | Default | Range | Notes |
| --- | --- | --- | --- |
| Map size | 2,048 × 2,048 m | fixed | One home-valley region; 16 make the valley, and Interior districts use 4,096 blocks at 4 m cells (World map) |
| Height cell | 2 m | 1–4 m | 1,024² floats ≈ 4 MB at 2 m |
| Chunk size | 64 m | fixed | 32 × 32 height cells per 64-block layer tile, which is 4 × 4 Minecraft chunks |
| Elevation range | 120–900 m | — | Valley floor \~150 m |
| Base noise scale / octaves | 1/1,200 m, 5 | — | Simplex fBm |
| Ridged noise scale / octaves | 1/700 m, 4 | — | North third only, feathered 300 m |
| Domain warp amplitude | 150 m | 50–300 m |  |
| Erosion droplets | 60,000 | 20k–200k | Runs on the `yearofice-gen` layer pool (`worldgen.Erosion`, 2–3 s a region), never on Minecraft's chunk workers |
| River catchment threshold | 20,000 m² | 10k–80k m² | Lower = more creeks |
| Main river width | 12–25 m | — | Must be swimmable but tiring |
| Lake minimum | 1.5 m deep, 2,000 m² | — | Smaller basins = bog |
| Waterfall rule | 3 m drop in 6 m run | — | Pool radius 4–8 m at the foot |
| Shelter rock face | slope > 40°, height > 6 m | — | Flag top 20 by height and aspect |

**Archetype shaping.** Stage 2 reads the archetype from stage 1 and changes the trough, the main river's path and the mountains. The numbers below are the defaults in `data/yearofice/archetype/<id>.json`, with the shared ones in `worldgen_params/valley.json`.

| Parameter | Braided flats | Lake valley | Canyon | Oxbow marsh |
| --- | --- | --- | --- | --- |
| Valley floor width | 400–700 m | 300–500 m | 60–150 m in the canyon reach | 600–1,000 m |
| Floor gradient | 0.2–0.4% | 0.1–0.2% | 0.6–1.2% | 0.05–0.15% |
| River path swing | 40–80 m | 60–120 m | 20–40 m | 150–250 m |
| Channels | 3–6 braids across a 150–400 m gravel belt | 1 | 1 | 1, plus 2–4 oxbow lakes |
| Ridged mountains | North third | North third | North half; rock walls 20–60 m high along a 600–1,200 m reach | North quarter |
| Special shaping | Sediment doubled on the floor | A basin 600–1,200 × 250–500 m, 8–25 m deep, north of the river | 4–8 rock steps of 3–8 m on the river and creeks | Floor flattened to ±1 m across 60% of its width |
| Minimum falls | 1 | 2 | 4 | 1 |

The canyon reach covers at most half of the core's river length, so flat banks for the start site still exist.

**The main river's path.** Stage 2 lays a polyline from the entry edge to the exit edge, one point every 16 m, inside the middle third of the map. Its sideways offset is two octaves of 1D noise at 1/400 m and 1/150 m, scaled by the archetype's swing. The trough is carved around the line:

```latex
z_{trough}(d) = -h_{t} \cdot \left(1 - \left(\frac{d}{w/2}\right)^{2}\right) \quad \text{for } d < \frac{w}{2}
```

Here d is the distance to the path, w the floor width and h\_t a trough depth of 20–60 m. Only squares are used, so the result is identical on every platform and JVM.

**Hardness keeps the cliffs.** Thermal erosion at 35° would flatten every slope steeper than that, leaving no rock faces. Cells with hardness above 0.6 skip thermal erosion and erode at 25% of the droplet rate. Hardness rises under the ridged mask, so the north range keeps its faces while soil slopes settle to scree.

**Erosion parameters.**

| Parameter | Value | Note |
| --- | --- | --- |
| Droplet lifetime | 30 steps | One step is one cell |
| Inertia | 0.05 | Higher makes straighter gullies |
| Sediment capacity | 4 | Times speed times slope |
| Erosion and deposition rates | 0.3 each | Spread over a 3-cell radius |
| Evaporation | 1% a step | — |
| Thermal passes | 50 | Moves half the excess over 1.4 m height difference per 2 m cell (35°) |

**Lakes and bogs.** Priority flood sets each basin's water surface; depth is surface minus ground. Basins at least 1.5 m deep and 2,000 m² become lakes, each with exactly one outlet at its lowest rim cell. Smaller basins, and flat ground (under 2°) within 200 m of water with sediment over 1 m, become bog.

Every lake gets a weed shelf (0–2 m deep, the first 20 m from shore) and, where depth falls more than 0.3 m per metre, a drop-off. Both become fishing spot candidates.

**River geometry.** Channel width, depth and current come from catchment A (m², including the main river's virtual inflow) and channel slope S, smoothed over 100 m.

```latex
W = \max(0.8,\ 0.0015 \cdot \sqrt{A})
```

```latex
D = 0.3 + 0.08 \cdot W
```

```latex
V = \min(3.0,\ \max(0.3,\ 0.6 + 150 \cdot S))
```

W and D are in metres and V in metres per second. A creek draining 1 km² is 1.5 m wide; the main river at 64–278 km² is 12–25 m wide and 1.3–2.3 m deep.

Water under 0.8 m deep and under 1.2 m/s can be waded, at 90%, 70% or 50% speed by depth class (Swimming and working in water). Anything deeper or faster must be swum, so the main river is always a swim.

**Braids.** In the braided archetype, wherever the floor gradient is under 0.4%, the main river splits into 3–6 channels across its gravel belt. Each channel is a seeded wandering path that splits from and rejoins the others, carrying an equal share of A. Ground between channels becomes gravel bar.

**Oxbows.** In the marsh archetype, stage 2 checks the river path for loops. Where two points more than 6 river widths apart along the path come within 1.5 widths of each other, the loop is cut off and carved 2–3 m deep. Stage 4's priority flood fills it as an oxbow lake; if fewer than 2 form, stage 2 raises the swing by 20% and retries, up to 3 times.

**Falls guarantee.** If the archetype's minimum falls count is not met, stage 4 picks the largest north creek where it leaves the ridged mountains. It deepens the bed carve over the 30 m downstream by 3–5 m, tapered, which makes a step and its pool. Stage 4 writes only its own carve layer, so height stays untouched.

**Flood lines and banks.** Each river cell stores two levels above its surface. Normal high water is +2 m on the main river and +0.6 m on creeks; flood-year high water is +3.5 m and +1.2 m.

The normal line is the mud mark drawn on tree trunks that the cabin site check reads. Outside bends tighter than 5 widths with a bank over 1 m become cut banks, which hold clay and flood-exposed iron. Inside bends, braid islands and creek mouths become gravel bars.

**Springs and thin water.** Stage 4 flags 1–2 spring cells per creek longer than 500 m, and every cell within 30 m below a falls. Stage 8 keeps these thin or open in winter, which makes the thin-ice traps in Danger in detail.

## Biomes and resources

Biomes come from four inputs per cell: elevation, slope, distance to water and sun exposure (south-facing slopes are warmer and drier). Each cell gets one biome by the first rule that matches, top to bottom.

| Biome | Rule (first match wins) | Trees and ground | Main resources |
| --- | --- | --- | --- |
| Open water | River or lake cell | — | Fish, water, ice in winter |
| Gravel bar | Within 8 m of river, slope < 5° | Bare gravel, driftwood | Driftwood (dry fuel), flint-like stone |
| Bog / muskeg | Filled basin < 1.5 m deep, or flat and wet | Sphagnum, stunted black spruce | Cranberry, fiber plants, peat |
| Riparian | Within 40 m of water, slope < 20° | Willow, birch, alder | Birch bark (tinder), willow shoots, game trails, beaver sites |
| Spruce taiga | Default lowland | Dense white spruce | Spruce tips, resin, logs, squirrels |
| Birch–aspen slope | South-facing slope 10–30° | Birch, aspen | Birch bark, berries, grouse |
| Rock face / scree | Slope > 40° or thermal-erosion talus | Bare rock | Surface iron, stone, shelter candidates |
| Alpine tundra | Elevation > 650 m | Low shrubs, lichen | Blueberry, open sightlines, caribou range |

**Resource placement**

- **Surface iron:** 12–18 outcrop clusters in the home valley (the Interior adds 60–120 by district type, World map), only on rock-face and scree cells above 250 m, at least 400 m from the start site. Each cluster holds 3–8 nodes. Nothing generates underground; digging down finds nothing useful. Iron is renewable over years: bog iron re-forms in bogs every 3–5 years, and spring floods expose 1–3 new outcrops a year.
- **Trees:** Poisson-disc scatter per biome with a spacing range (spruce taiga 3–5 m, riparian 4–7 m). About 3% of trees are standing deadwood, which is dry fuel.
- **Plants:** berries, fiber plants and spruce tips use a seasonal ripeness window from degree-days (Plants and fungi › Phases through the year), not a fixed spawn. Every species is its own block with phase and taken states (Plants and fungi › Plants as blocks).
- **Worms:** found by digging riparian or bog soil; yield doubles for a day after rain.
- **Loot:** the outpost holds the starting kit; 2–3 old trapper caches elsewhere hold one tool upgrade each.

**Seeds must feel different, not just look different.** Players praise new seeds for fresh exploration but complain that procedural maps feel samey and empty. Three rules answer that:

1. **Valley archetype.** Each seed draws one of four layouts for the core valley: braided river on gravel flats, a big lake fed by a creek, a narrow canyon river with many falls, or a wide marshy river with oxbow lakes. Each shifts what food and travel are easy, so the first month plays differently.
2. **Hand-made set pieces, placed by rules.** Authored places with 2–3 variants each are dropped where the terrain fits: the bush-plane wreck, the old trapper's cabin, a gold-rush mining camp, a fire lookout tower, a hot spring that stays open in winter, an ice cave, a beaver valley. Each region gets at least one landmark tall or bright enough to see from 2 km, so there is always something on the horizon to walk toward.
3. **Nothing is uniform.** Every resource node, fish spot and animal range gets ±20% on its yield, so a good spot is a real discovery.

### Valley archetypes

The archetype decides which food and travel come easy in the first month, so two seeds teach different lessons first. Terrain and hydrology has the shaping numbers; this is what each one looks and plays like.

|  | Braided flats | Lake valley | Canyon | Oxbow marsh |
| --- | --- | --- | --- | --- |
| First sight from the door | Grey gravel 400 m wide, braided channels, willow islands | Flat water through spruce, loons calling | A rock gorge with white water and the roar of falls | A wide brown river, sedge meadows, ponds to the horizon |
| Easy early | Fire: driftwood and flint everywhere; long sight lines | Pike and whitefish from the shore; ducks in May | Fishing in pools below falls; dig-in rock shelters | Moose, muskrat, ducks, cattail roots |
| Hard early | Deep pools are rare, so few pike | Firewood: the lake shore is wet black spruce | Flat ground and crossings | Mosquitoes 50% denser; slow wet walking |
| Summer travel | Fast along the bars; channels shift after breakup | Slow round the shore through bog | Along the rim; crossings need a felled log | Bog at 70% speed; boots stay wet |
| Winter travel | Wind-packed bars; overflow on side channels | The lake is a highway once it is 10 cm thick | Ice bridges at the falls stay thin | The best of the four: all of it frozen flat |
| It teaches first | Fire and reading moving water | Fishing and ice | Shelter and felling trees as bridges | Patience, bugs and winter travel |

**Braided flats.** The river spreads into 3–6 channels that braid around willow islands where moose bed at midday. Driftwood piles at the head of every bar give dry fuel on day one, and flint lies on the bars at twice the usual density. Breakup rearranges the bars each spring, so set lines and low camps can be lost, and the new bars carry fresh driftwood and sometimes iron.

**Lake valley.** A lake 600–1,200 m long sits north of the river, fed by a creek from the mountains and drained by a short outlet. The inlet and outlet are the best early fishing, and the deep end holds lake trout for the first winter through the ice. Wet black spruce rings the shore, so good firewood means a walk up to the birch slopes.

**Canyon.** For up to a kilometre the river runs between rock walls 20–60 m high, with 4–8 falls and a deep pool under each. Salmon jump the falls in July and can be grabbed by hand, as in the video, and there are more shelter faces than anywhere else. Cold air drains down the gorge at night (2 °C colder), and in winter the walls shade the floor for weeks.

**Oxbow marsh.** The river loops slowly through a floor 600–1,000 m wide, leaving 2–4 oxbow lakes full of pike. Moose, muskrat and ducks are thick, cattail roots give real carbs, and cranberries cover the bog. Summer is a slog through wet ground and mosquitoes; winter opens all of it flat and fast.

### Set pieces

Each set piece is authored once in `data/yearofice/setpiece/<id>.json` with 2–3 variants and placed by stage 5 where its fit rule matches. Every one teaches one thing, holds loot the player can carry, and tells its own small story through objects and short notes. Several stories touch each other, so a player who finds them all can piece the valley's past together.

Stage 5 runs before the start site exists, so the distance column is not a placement rule. It is what the start-site score and checks usually produce, and the M1 debug map reports it per seed.

| Set piece | Per world | Fits where | Typical distance from the start | Landmark from 2 km |
| --- | --- | --- | --- | --- |
| Bush-plane wreck | 1 | Gravel bar, ridge clearing or lake beach, slope under 10° | 600–3,000 m | Yes: an orange-and-white tail |
| Old trapper's cabin | 1–2 | Creek bench in spruce, 200+ m from the main river | 800–4,000 m | No |
| Gold-rush mining camp | 1–2 | A side creek, always one in the old mining creek region | 1,500+ m | The dredge variant (14 m tall) |
| Fire lookout tower | 1–2 | The highest bald ridge top in its region, top slope under 15° | 1,200–2,500 m | Yes: 10–14 m on a ridge |
| Hot spring | 1 | Foot of a rock face or a slope break, outside the core | 2,000–6,000 m | In winter only: a steam plume below −15 °C |
| Ice cave | 1 | North-facing rock face over 10 m tall, above 500 m | 2,000+ m | No |
| Beaver valley | 1–2 | Side creek at 1–3% gradient through riparian or bog, 400–1,200 m long | 500+ m | No |
| Mineral lick | 2–4 | Cut bank or seep with clay, in riparian or bog | 300+ m | No |
| Trapper caches | 2–3 | Within 30 m of a game trail, on a rise | 400+ m | No |

**Bush-plane wreck.** A single-engine, high-wing bush plane from 1979 lies with its cockpit door hanging open.

*Variants:* nose-down on a gravel bar, tail high. In the treetops of a spruce slope, wings sheared off. Half-sunk on a lake beach.

*Inside:* 4 aluminum skin panels (sled runners, spoon lures) and 30 m of control cable (snare wire, radio repair). A plexiglass side window makes a cabin window. The tool pouch holds the battery and a pad of steel wool, beside a first-aid tin (2 bandages) and 1 logbook page.

The battery is flat and needs 3 sunny days on the crate's solar panel before it works. The panel cannot charge the messenger meanwhile, which forces a choice.

*Teaches:* salvage, and that the world's metal is finite until iron. In the gravel-bar variant, a mud line on the fuselage teaches the flood line.

*Story:* a note is taped inside the windscreen: "Sept 14, 1979. Fog came down on the bar. Gear's gone, radio's dead. Walking downriver. — J.O."

Her fate is not shown here. The lookout's log has the next line of it.

**Old trapper's cabin.** *Variants:* a log cabin with a caved sod roof and a cache on four legs still standing. A dugout cut into a south bank, only its front wall and door showing. A small line cabin, 2 × 2.5 m, with a stovepipe through the roof.

*Inside:* a rusted stove, a stretcher board for pelts, the trapline board, and one tool upgrade from the trapper-cache list below. The trapline board is a plank with the old sets scratched on it. Reading it pins 6–10 good marten and lynx sites on the messenger map.

The cache on legs wears tin bands, the trick that stops wolverines.

*Teaches:* the trapline loop, the raised cache, and the sod roof the player will build later.

*Story:* Arvid Lund trapped this country from 1938 to 1971. On the door frame he cut one notch for each lynx, winter by winter; the tally rises and falls on a 10-year beat. That beat is the hare cycle in Populations, and a player who reads it can predict the lean years.

His last note is pinned by the stove: "Hares gone again. Lynx will be thin next winter. Moving the line up to the beaver creek."

**Gold-rush mining camp.** *Variants:* a placer camp with sluice boxes, tailing piles and a collapsed bunkhouse. A prospect adit under a small headframe, its tunnel caved in 6 m from the mouth. A rusted bucket dredge in its own pond, 14 m tall.

*Inside:* a steel pick head, a keg of 40 square nails and 10 kg of scrap iron. There is also a cast-iron kettle, a gold pan and 1–2 logbook pages.

The pick head makes a real pick: the crate hatchet pries an iron nodule in 10 game minutes, the pick in 5. Two nails make ice picks.

The gold pan works. Panning the creek for an hour yields a few flakes, worth nothing out here. A vial of gold on a cabin shelf counts as a trophy and lifts Spirits, and that is all it does.

*Teaches:* found metal, the iron hunt, and how little gold matters to a survivor. The adit is shelter, not a mine; nothing lies deeper.

*Story:* a ledger records the claim of Kessler and Doane, 1906–1912. The weekly ounces fall from 14 to under 1.

The last line, in a different hand: "Doane gone to town for the winter. I'll hold the claim." Behind the bunkhouse a board reads "J. Kessler, 1874–1911".

**Fire lookout tower.** *Variants:* a 10 m timber tower with a glassed cab on a ridge. A ground-level cab on a bald rock summit. A collapsed tower beside its stone footings, only the stair stub standing.

*Inside:* a cot, a small cast-iron stove (warm shelter for a night), binoculars, the fire-finder table, the lookout's log and 1 logbook page. Binoculars give 4× zoom while C is held, from the neck slot. Using the fire-finder reveals the messenger map 1,000 m around the tower and names the valley's features, so the map gains place names.

*Teaches:* reading the land from above, landmarks and weather signs.

The log's margins hold rules of thumb that are true in this game's weather. "Ring round the sun, rain within a day." "Mare's tails in the west, wind tomorrow."

*Story:* the tower was staffed every summer from 1952 to 1984.

One entry stands out: "Sept 16, 1979. Signal fire on the river bar, 10 km west. Reported by radio. Plane went over at 14:10." So J.O. was seen, and probably picked up.

**Hot spring.** *Variants:* a steaming pool on a gravel terrace beside a ruined roadhouse. A string of small pools in a bog meadow. One deep pool at the foot of a rock face.

*What it does:* the source is 42 °C and the main pool 38 °C. Soaking restores warmth to full in 10 game minutes and lifts Spirits. But the player leaves soaked, so at −30 °C they need a fire within minutes.

The outflow keeps 100–300 m of creek open all winter, which draws moose, ducks and otters.

The ground within 30 m never freezes. A garden bed there can be sown 3 weeks before the window opens and harvested 3 weeks after the first frost.

*Inside the roadhouse:* a guest book and a 60 kg cast-iron stove. Outside, a feral rhubarb patch (`yearofice:feral_rhubarb`) gives 2 kg of stalks in June: vitamin C and something sour. Its leaves are toxic, −15 Condition a meal (Plants and fungi › Food herbs). The stove is a real heater, but it has to be dragged home at 30% speed or hauled by sled.

*Teaches:* microclimates, and that being warm and wet is a trap.

*Story:* the roadhouse fed the winter sled trail between the mining camps from 1908 to 1931. The guest book holds about 300 names; Doane signs in for three winters running.

**Ice cave.** *Variants:* a cold-trap cave at the foot of a north-facing cliff, where winter air pools and ice lasts all summer. A cave under a small snowfield, only where the region rises above 800 m. A tunnel the miners cut into a frozen bank.

*What it does:* the air inside stays at −2 °C all year, so food stored there keeps as in a natural freezer, even in July. A door built across the mouth makes it bear-proof, the same as a raised cache. The floor is glare ice: running is a sprain risk, and icicles drop from the roof when the day's high is above 10 °C.

*Inside:* rusted meat hooks in the ceiling, "D. 1909" carved by the door, old bones, and a mammoth tusk frozen into the back wall. Freeing the tusk takes 2 game hours with a hatchet.

It is ivory for carving hooks, needles and toggles, or a trophy for the wall.

*Teaches:* cold as a tool, and the freezer as a reason for an outpost far from home.

*Story:* Doane kept the claim's meat here. The tusk was here 30,000 years before him.

**Beaver valley.** *Variants:* a staircase of 4–7 dams and ponds down a side creek. One great dam 100 m long, its pond full of drowned spruce. An abandoned colony whose ponds have drained to a grass meadow.

*What it holds:* 1–2 of the map's beaver colonies with their lodges, and pike in the ponds. Grayling hold below each dam, and muskrat, ducks and moose feed in the shallows.

The drowned spruce are standing deadwood with dry tops. Peeled beaver sticks on the banks are dry kindling and straight arrow shafts.

*Teaches:* beaver behaviour (the tail slap, then a bite), lodge snares, and that dams are bridges. Breaking a dam drains the pond below it in 2 game hours, stranding fish to grab by hand. The beavers rebuild it in 2 nights.

*Story:* the meadow variant is an old pond the beavers left. In every variant, the ruin of Arvid Lund's last line cabin stands in the newest pond, water to its windowsill. His steel stretcher frame hangs on the wall, and a notebook page pinned beside it is the recipe for a birch-bark canoe.

**Mineral lick.** *Variants:* a grey clay bank pocked with hoof prints; a muddy seep with a white salt crust. Moose and Dall sheep visit daily, at dawn or dusk, so it is the best ambush site in its region. It is also a clay source.

*Teaches:* patience and the ground blind.

**Trapper caches.** Arvid's small caches: a lard can under a rock cairn, or a box nailed 3 m up a spruce. Each holds one tool upgrade, drawn without repeats from this list:

| Upgrade | What it changes |
| --- | --- |
| Coil of snare wire, 30 m | 15 wire snares, which break on 5% of catches instead of 30% |
| Flat file | Sharpens edge tools twice as fast as the river-stone hone |
| Ice chisel head | Halves the time to chisel an ice hole |
| Drawknife | Halves log-peeling time |
| Bucksaw blade | Halves bucking time for firewood rounds |
| Jar of wheat seed | 100 g of seed, 5 `wheat_seed` for 5 m², the video's crop, at the base germination in Crop farming |

### Landmarks

Each of the 16 home-valley regions holds at least one landmark that passes the 2 km test.

Picture a 2 km circle around it with 8 points at 1.7 m eye height. It counts as visible if its top 3 m clears terrain and canopy from at least 3 of them. Set-piece landmarks are placed first; a region without one gets a natural landmark.

| Natural landmark | Look | Fits where |
| --- | --- | --- |
| Granite tor | A rock tower 10–25 m high on a ridge | Ridge tops in hard rock |
| White cascade | A falls streaking down a rock face over 20 m tall | Creeks leaving the north range |
| Snowfield | A white patch on a north peak all summer | Peaks over 850 m |
| Burn ridge | Silver dead snags along a skyline | A region with an old burn |

The start score prefers a door view of the lookout tower (see Start site), as in the playthrough. The hot spring's steam plume counts only in winter, so its region still needs a year-round landmark.

### Other resources

**Surface iron in detail.** An outcrop shows as rust-orange rock with a rust stain running 2–6 m down the scree below it, readable from 150 m. Each node is a 3 kg nodule; a cluster's nodes do not regrow.

Renewal has two sources. Stage 6 flags 2–4 bog iron beds, each an orange film over a bog pool. A bed gives 4–6 nodules picked by hand from the bottom, and re-forms 3–5 years after the last is taken.

Each breakup also exposes 1–3 new outcrops on cut banks, which stage 8 places from that year's draw.

| Resource | Where and how much | Note |
| --- | --- | --- |
| Flint | Gravel bars: one 0.5 kg nodule per 100 m² of bar; twice that in braided flats | Knapped as in Gathering materials |
| Clay | 6–12 deposits per region on cut banks, bog edges and licks; 20 buckets each | Not diggable from October to breakup |
| Stone | Round river stones on bars; angular stones on scree | Wet river stones crack in a fire ring |
| Peat | Bog cells, cut in summer, dried 10 days | Burns low and smoky: smoker fuel, long banked fires |
| Chaga | 1 in 12 mature birch, on the north side of the trunk | Taking half lets it regrow in 5 years |
| Cattail | Lake and pond edges under 1 m deep | Three times as common in the oxbow marsh |
| Standing deadwood | 3% of trees; 40% in an old burn; most trees in a beaver pond | Dry fuel the day it is cut |

**Permafrost.** Stage 6 flags north-facing taiga slopes and bog as permafrost: only the top 50 cm thaws in summer. Gardens there fail, as Crop farming says, and the ground stays wet and cold all year. The plant guide marks it by its stunted black spruce, so the player can read it before clearing a bed.

## Wildlife and fish

Animals are not spawned at random. Each species gets a home range generated at world creation, and moves between bedding, food and water along game trails that are visible on the ground. Populations are small and only refill slowly, so overhunting one area matters.

**Game trails.** Pick bedding spots (dense taiga, willow thickets), forage spots (riparian, birch slopes, bog edges) and water access points. Connect them with least-cost paths where cost rises with slope and deep snow. Trails are drawn as trampled ground; snares placed on a trail cell get a 4× catch bonus.

| Species | Habitat | Per map | Active | Behavior | Yield (approx.) |
| --- | --- | --- | --- | --- | --- |
| Snowshoe hare | Willow, taiga edges | 25–40 | Dawn, dusk, night | Flees; uses trails | 1,070 kcal meat, pelt |
| Red squirrel | Spruce taiga | 30–50 | Day | Climbs; blunt arrows only | 250 kcal |
| Spruce grouse | Taiga, birch slopes | 15–25 | Day | Freezes, then flushes | 640 kcal, feathers |
| Willow ptarmigan | Willow flats, tundra | Flocks of 10–30 | Day | Freezes, flushes as a flock | 440 kcal, feathers |
| Ducks and geese | Lakes, marshes | Migrating flocks | Day; May and September only | Wary on open water | 1,000–4,000 kcal, down |
| Porcupine | Forest | 6–10 | Night; slow by day | Defends with quills | 5,000 kcal, fat |
| Muskrat | Bog ponds | 10–20 | Dusk, night | Dives | 600 kcal, pelt |
| Beaver | Riparian, makes ponds | 2–4 colonies | Dusk, night | Dives; trapped at lodge | 9,300 kcal, fat, pelt |
| Marten | Old spruce | 4–8 | Night | Curious; takes bait | Pelt |
| River otter | Rivers | 2–3 | Day, dusk | Takes fish | Pelt |
| Red fox | Edges, gravel bars | 4–6 | Dusk, night | Raids snares | Pelt |
| Lynx | Spruce thickets | 2–4 | Dusk, night | Secretive; follows the hares | Pelt |
| Wolverine | High country | 0–1 | Any time | Raids caches and traplines | Frost-shedding fur |
| Moose | Riparian, bog, willow | 3–5 | Dawn, dusk | Charges if close or a cow with calf | 239,000 kcal for a 450 kg bull (180 kg meat, Bestiary › Parts by species), hide, sinew |
| Caribou | Alpine tundra | 0–8 (migration) | Day | Herds pass through in fall | 60,000 kcal, hide |
| Dall sheep | North-range cliffs | 6–12 | Day | Watches from the cliffs | 30,000 kcal, horns |
| Wolf | Whole map; 1 pack with a den | 4–6 | Night, more in winter | Avoids early; tests a weak player in winter; pups and yearlings can be tamed | 20,000 kcal lean meat, pelt |
| Black bear | Forest, berry slopes | 3–5 | Day; dens Nov–Apr | Avoids people; climbs trees | 50,000 kcal, fat, hide |
| Grizzly bear | Riparian, falls, berries | 2–3 | Spring to fall; dens in winter | Investigates food smells; charges if surprised | 80,000 kcal, fat, hide |
| Raven | Everywhere | Many | Day | Circles carcasses | None; a sign to read |

**Bear rules.** Cleaning fish or game, or leaving meat out, creates a scent point that decays over 24 game hours. Bears path toward scent within 400 m. Dens are generated on high slopes at least 500 m from the start; bears den from late October to April.

**Fish.** Fishing spots are generated from hydrology: pools below falls (best), outside of river bends, lake inlets and outlets, and beaver ponds. Each spot gets a quality score from 0 to 1.

| Fish | Where | Season | Notes |
| --- | --- | --- | --- |
| Arctic grayling | Clear riffles, pools | May–Sep | Small, common, insect bait |
| Northern pike | Lakes, slow water | All year | Large; live bait and a wire leader |
| Chinook salmon | Main river | Late June–July | The biggest fish; nets and spears |
| Chum salmon | Main river | July–August | The main run; the best wolf food |
| Coho salmon | Clear side streams | August–September | The late run |
| Burbot | Deep river holes | All year, best Nov–Mar | Night set lines through the ice |
| Humpback whitefish | Lakes, slow river | All year | Best in gill nets |
| Sheefish | Big river, big lake | Summer and fall | A trophy on spoons |
| Lake trout | Deep water in the big lake | All year | Best through the ice |
| Longnose sucker | Slow water | May–Sep | Easy and bony |
| Slimy sculpin | Rocky shallows | All year | Bait only, from minnow traps |

How each fish behaves, bites and fights is in Fishing in depth.

Bite chance per minute at a spot:

```latex
P_{bite} = b_{species} \cdot q_{spot} \cdot m_{bait} \cdot m_{time} \cdot m_{weather} \cdot m_{season}
```

Suggested multipliers: dawn and dusk 1.5, midday 0.7; overcast 1.2, storm 0.5; wrong bait 0.2, live bait for pike 2.0. Gill nets only roll once four or more net segments are joined, and their chance scales with how much of the river width they cover.

**Home ranges at world creation.** Stage 7 places one range per individual or group, centred by weighted sampling on a habitat-suitability map smoothed over the range radius. Ranges of the same species keep at least one radius apart. Each range then picks 2–4 bedding spots, 2–4 forage spots and 1–2 water points, the best-scoring cells inside it.

| Species | Range unit | Radius | Placement notes |
| --- | --- | --- | --- |
| Snowshoe hare | Individual | 150 m | Willow and taiga edges |
| Red squirrel | Individual | 60 m | Centred on a cone midden, drawn on the ground |
| Spruce grouse | Individual | 250 m | Taiga and birch slopes |
| Willow ptarmigan | Flock | 800 m | Tundra in summer, valley willow flats in winter |
| Porcupine | Individual | 300 m | Forest; marks trees by chewing the bark |
| Muskrat | Individual | 50 m | Around a pond lodge |
| Beaver | Colony | 400 m of watercourse | A lodge and 1–3 dams; writes a pond overlay |
| Marten | Individual | 600 m | Old spruce |
| River otter | Individual | 3 km of river | Slides on banks near fishing spots |
| Red fox | Individual | 1 km | Edges and gravel bars |
| Lynx | Individual | 2 km | Spruce thickets with hares |
| Wolverine | Individual | All high country | 0–1 per world |
| Moose | Individual | 1.5 km | Riparian and bog in summer; willow yards in winter |
| Caribou | Herd path | — | A migration corridor across the tundra region, crossing rivers at 1–2 fords |
| Dall sheep | Band | 1 km | North cliffs, always within 50 m of slopes over 35° |
| Wolf | Pack | The whole 8 km home valley | One den, see below |
| Black bear | Individual | 1.5 km | Forest and berry slopes |
| Grizzly bear | Individual | 3 km | Each range includes the best falls pool in reach |

Ducks, geese and ravens have no ranges: flocks arrive on the dates in Calendar, and ravens spawn over kills and gut piles.

**Game trails.** Trails are planned as least-cost paths on an 8 m grid (then drawn and worn on 2 m cells, `yearofice:trail_wear`; RECONCILE 96) between each range's bedding, forage and water points, largest animals first (moose, bears, wolves, then hares). The cost of entering a cell:

```latex
c = \max(0.3,\ 1 + 12 \cdot s^{2} + 30 \cdot w + 4 \cdot n - 0.5 \cdot t)
```

Here s is the slope (rise over run) and w is 1 for water deeper than 0.8 m. In the winter variant only, n is 1 for open or high ground that drifts deep. The term t is 1 where an earlier trail already runs, which makes trails merge into a network.

Stage 7 computes a summer and a winter variant; stage 8 switches to the winter one when snow passes 20 cm. A trail cell used by three or more species draws wider and darker, so main trails read from a distance.

**Fords.** Where a river is wade-deep (under 0.8 m and under 1.2 m/s), trails converge on a ford. Caribou cross at fords, which makes the ambush site in How animals act.

**Dens and lodges.** Each bear gets a den on a 25–40° slope above 300 m, facing north or east where snow drifts deep. The wolf pack gets one den in a sandy, south-facing bank (sediment over 1 m, slope 15–30°) within 300 m of water. Its rendezvous meadow lies 300–600 m away.

Dens are placed before the start site, whose check keeps every den at least 500 m off; the wolf den usually lands 1,000–3,000 m away. Beaver lodges sit in their ponds; muskrat push-ups appear on pond ice in winter.

**Fishing spot quality.** Stage 4 finds candidate spots from hydrology, and each gets a quality q from 0 to 1:

| Spot type | Base q | Holds |
| --- | --- | --- |
| Pool below falls | 0.70 | Grayling; salmon during runs; burbot in winter |
| Lake inlet | 0.60 | Grayling, pike, whitefish |
| Creek mouth | 0.55 | Grayling; coho during their run |
| Lake drop-off (over 8 m deep) | 0.55 | Lake trout, burbot |
| Outside of a bend | 0.50 | Grayling, whitefish, sheefish |
| Lake outlet | 0.50 | Grayling, whitefish |
| Weed shelf | 0.45 | Pike |
| Beaver pond | 0.40 | Pike; grayling below the dam |

```latex
q = \min(1,\ (q_{base} + 0.1 \cdot [D > 2\,m] + 0.05 \cdot [\text{within } 300\,m \text{ below a lake or falls}]) \cdot r)
```

The factor r is the ±20% variation, drawn between 0.8 and 1.2 per spot. A deep pool below falls can reach 0.96, the best water in the game. Each spot's starting stock is q times its type's capacity in `its fish file (data/yearofice/fish/<id>.json)`, and Fish stocks governs it from then on.

**Check.** Stage 7 fails the attempt if any species count leaves its per-map range or any range centre has no path to water. In the core, 2–6 spots must score 0.8 or more, so the start check can find good water that is not on the doorstep.

## Start site

The player wakes at an abandoned research outpost on a flat riverbank: a half-collapsed cabin, a cold firepit, a broken radio and a small supply crate. The generator scores every flat riverbank cell, takes the best, and if no site passes every check below it tries the next attempt seed, up to 20 (attemptSeed = hash(seed, k); Minecraft's level seed never changes).

| Check | Rule | Why |
| --- | --- | --- |
| Water | River or lake within 60 m | Day-one water must be reachable |
| Flat ground | 20 × 20 m area with slope < 8° | Room for the cabin and a camp |
| Shelter option | Rock face over 6 m tall within 350 m | Lets the player build the dig-in base |
| Firewood | At least 40 trees and 2 deadwood within 80 m | A fire must be possible on day one |
| Tinder | At least 3 birch within 150 m | Bark for the first bow-drill fire |
| Fishing | A spot with quality ≥ 0.5 within 500 m, none ≥ 0.8 within 150 m | Food exists, but not on the doorstep |
| Game trail | At least one trail within 300 m | Snares have a sensible target |
| Iron | Nearest outcrop 400–1,000 m away, not in line of sight | Forces the first exploration trip |
| Landmark | A landmark visible from the cabin door | Gives the first walk a direction |
| Set piece | One set piece within 1,500 m | An early discovery: logbook pages, salvage |
| Bear distance | No den within 500 m; one bear home range overlaps the site | Danger is present, not constant |
| Moose | One moose range within 1,200 m | A big-game goal the player can see early |

**Starting kit** (in the crate): fixed-blade knife, hatchet, steel water bottle, one cooking pot, 38 pilot-bread crackers at 100 kcal each, one medkit, one can of bear spray (single use), 10 m of cord, matches × 3 (after that, bow drill only), a seed packet (potato and barley), a compass, a headlamp, a pocket plant guide covering about half the local plants, a small solar panel, and the handheld satellite messenger, called the Trail Messenger in the world, that acts as the HUD.

**Choosing among passing sites.** Every flat riverbank cell that passes all 12 checks is scored, and the highest score wins; ties go to the lower cell hash.

```latex
S = \min\left(1, \frac{n_{trees}}{80}\right) + 0.5 \cdot \min\left(1, \frac{n_{birch}}{10}\right) + 0.5 \cdot \left(1 - \frac{d_{face}}{350}\right) + 0.5 \cdot f + 0.3 \cdot a + 0.4 \cdot v
```

Here n\_trees counts trees within 80 m, n\_birch birch within 150 m, and d\_face is the distance to the nearest shelter face in metres. The flood term f is 1 when the cabin floor is 2–4 m above summer water, 0.5 at 4–6 m and 0 otherwise.

The aspect term a runs from 0 (facing north) to 1 (facing south). The view term v is 1 when a lookout tower stands 0.5° or more above the horizon from the door, and 0.7 for any other landmark.

The flood term puts the cabin above normal high water, but low enough that a flood year reaches the ground nearby. That is how the playthrough's low cache washes out.

**The outpost layout.** The outpost is one authored set piece placed on the winning cell, with the door turned toward the landmark.

| Piece | Where | State on day 1 | What it is for |
| --- | --- | --- | --- |
| Cabin, 5 × 6 blocks (the outpost template in the Minecraft engine tab) | On the cell | North half of the roof caved in; the south-east corner is dry | Tier 1 shelter in the corner until repaired to tier 2 (2 game days, 20 logs) |
| Bunk | Dry corner | Bare boards | Where the player wakes; the debris bed goes on it |
| Supply crate | Dry corner | Closed | The starting kit |
| Radio | On a plank table | No battery; mast down | The radio loop, later |
| Radio mast, 8 m | Lying across the yard | Bent at one joint | Repair needs iron and the plane's wire |
| Logbook page 1 | On the table | — | The first page; teaches the pocket guide's tracks appendix |
| Firepit | 6 m from the door, toward the water | A ring of 8 stones full of old ash | The first fire; no ring to build |
| Old woodpile | Against the north wall | 2 armloads of rotten, wet wood | Teaches that wet wood fails |
| Weather post | 10 m from the cabin | Seized wind cups, cracked rain gauge | Weather logs for the radio loop |
| Scrap barrel | Behind the cabin | Rusted, sound | The barrel stove in the iron tier |

**The guided first night.** On a player's first world it is on by default; after that it is a switch on the new-world screen. It runs from waking until the first sleep, using one objective beacon (a soft light column visible through trees, one at a time) and keycap prompts. After the first sleep, the beacon and all prompts are gone for good.

**Day 1 rules.** These hold during the guided night only.

- The player wakes at 12:00 on May 1. Day 1 weather is fixed at cloudy, 7 °C high, −3 °C low, wind 2 m/s, no precipitation; the chain starts from it.
- The clock runs at half speed until the first fire is lit or 22:00, whichever comes first. That gives about 20 real minutes of daylight work at the default day length, and proportionally more on a longer day.
- No predator enters the Active tier within 300 m of the outpost until 06:00 on day 2.
- Hypothermia cannot pass the shivering stage. The player can be miserable but not collapse.

**Messenger voice.** Lines are second person, at most 12 words, and always give the number when there is one. There are no exclamation marks and no jokes. Each line shows once; the Guide tab in the messenger (M) replays the current step.

**The steps.** Each row names the beacon's target, the exact lines in order, the keycap shown and what completes the step. A line in braces is filled from the world.

| # | Step | Beacon | Messenger says | Keycap | Done when |
| --- | --- | --- | --- | --- | --- |
| 1 | Wake | None | "Messenger online. Day 1, May 1, 12:00." / "Air 7 °C. Tonight −3 °C. You need a fire before dark." | M | Messenger opened, or after 20 s |
| 2 | The crate | Crate | "There's a supply crate in the dry corner. Open it." / On opening: "38 crackers, 100 kcal each. Under two days of food." / "Three matches. Save them. The bow drill never runs out." | Right mouse | Crate opened |
| 3 | Gear up | None | "Put the knife and hatchet on your belt." | E (pack and gear) | Both on the body diagram |
| 4 | Water | Nearest bank | "Fill the bottle at the river." / "Don't drink it yet. Boil it first." | Right mouse | Bottle full |
| 5 | Tinder | Nearest birch | "Peel bark from that birch. It burns even wet." / After the first sheet: "Take a sheet, not the ring. A ringed tree dies." | Hold right mouse | 3 sheets of bark |
| 6 | Dry wood | Nearest dead standing spruce | "Grey spruce with no bark is dead and dry. Fell it." / After bucking: "Six armloads of dry wood. The old woodpile is rotten." | Hold right mouse (work preview shows) | Tree felled and bucked |
| 7 | Bow drill | None | When cord and a stick are held: "You can make a bow drill. Spindle, board, bow, your cord." | E, then the recipe | Bow drill crafted (20 game minutes) |
| 8 | Lay the fire | Firepit | "Clear the old ash." / "Tinder bundle first, then twigs, then split wood." | Hold right mouse; aim to place each layer | Three layers laid |
| 9 | Light it | Firepit | "Long, even strokes. Keep pressure on the socket." / On an ember: "Ember. Into the tinder, now." / "Blow on it." / "Twigs now, before the flame dies." | Mouse or A/D strokes in rhythm, right mouse for pressure; tap right mouse to blow | Fire reaches the small-fire state |
| 10 | Boil water | Firepit | "Build a cooking stand over the fire. Two forked sticks, one crossbar." / When the pot hangs: "Boil it 5 minutes, then drink." | E | First drink of boiled water |
| 11 | Eat | None | "Eat something. Keep count." | R (quick-eat) | Anything eaten |
| 12 | Bed | Bunk | "Pile spruce boughs and grass on the bunk." / When built: "Off the boards, you'll sleep 3 °C warmer." | E (debris bed) | Bed built |
| 13 | Fuel for the night | Firepit | "Stack wood by the fire. It feeds itself within 3 m." / "Lay two long logs. A long fire lasts 3 hours an armload." | E (woodpile) | Woodpile with 3+ armloads; long fire laid |
| 14 | Sleep | None, or the snare site | "Sleep when you're ready. Cold or an animal will wake you." / If still light: "You have daylight left. A snare on the trail {d} m {bearing}." | Z | The player sleeps; the snare is optional |
| 15 | First morning | Removed for good | "Day 2, May 2. Low last night −3 °C." / "Guidance off. The map fills in as you walk." / "From the door: {landmark}, {km} km {bearing}." / "{n} crackers left. Food is your job now." | M | Morning prompts begin |

**Lines for things that go wrong.** Each fires at most once in the guided night.

| Trigger | Line |
| --- | --- |
| Drinks unboiled water | "Unboiled water. You may be sick in a day or two." |
| Eats a sixth cracker on day 1 | "That's 6 crackers today. {n} left." |
| More than 400 m from the outpost before a fire | "You're {d} m from the outpost. Sunset 21:45." |
| Strikes a match before trying the drill | "Match used. Two left. The drill is free." |
| First failed drill try | "Ember died. Slower, steadier strokes." |
| Second failed try | "Wood may be damp. Grey dead spruce is driest." |
| Third failed try, or 20:30 with no fire | "Sunset 21:45. Use a match if you have to." |
| Tinder dryness under 60 | "Tinder's damp. Birch bark burns wet; grass won't." |
| 23:00 with no fire | "No fire. Stay in the dry corner. Sunrise 05:35." |

**Proving it works.** Beyond the M3 test, the playtest bots log the game time each step finishes. Ninety percent of bot runs must finish step 13 before sunset at 21:45. The median first-time human tester must light the first fire within 25 real minutes of waking.

## Calendar, daylight and weather

The game year starts on May 1 at spring thaw and the generator aims each month at interior-Alaska normals, so winter is about 40 °C colder than summer and nearly dark.

&#91;embedded content: Approximate interior-Alaska normals near 64°N (Fairbanks area), from general knowledge, not looked up; tune in weather.json\]

- **Calendar.** 365 game days, one per calendar day, starting May 1. At the default 24 real minutes per game day a year is about 146 hours of play (438 at the longest setting, 72 minutes; Open decisions), and then the next year begins with the same seasons but a new weather draw.
- **Daily temperature** = the month's normal, interpolated day to day, plus a day–night swing (±5 °C in summer, ±3 °C in winter, coldest just before dawn), plus a weather anomaly that carries over (each day keeps 70% of yesterday's anomaly plus new noise of about ±4 °C), so cold snaps last several days.
- **Weather states:** clear, cloudy, rain, storm, snow, blizzard. A daily Markov chain with one transition table per season in the weather table, `data/yearofice/weather/interior_alaska.json`; rain becomes snow when the day's mean is below 1 °C.
- **Forecast.** The messenger shows tomorrow's real state with 80% accuracy and a two-day outlook at 60%, so planning pays but can still go wrong.
- **Daylight** comes from sun position at 64°N and drives lighting, animal activity windows and how long the player can safely work.
- **Snow** accumulates on snow days and melts above 0 °C; over 40 cm deep it cuts walking speed by 40% and makes animal tracks visible.
- **River ice.** Freeze-up starts once the 5-day mean stays below −8 °C; ice grows about 1 cm per day below −10 °C and is safe to walk at 10 cm. Breakup comes when the 5-day mean rises above 2 °C, with a thin-ice warning period first.
- **Seasonal events:** mosquito swarms June to July (smoke and netting beat them), berries ripen July to September, the salmon run is mid-July to August, caribou pass through in September, bears den from late October, and burbot bite through the ice all winter.

**The year table.** On each May 1, stage 8 draws all 365 days at once from `mulberry32(hash(attemptSeed, "weather", year))`. Because the whole year exists ahead of time, the forecast reads real future days, and the balance simulator can run a year in milliseconds. Year 1's May 1 is fixed by the Start site rules; every other day is drawn.

**Weather states as a chain of four.** The Markov chain runs over four states: Clear, Cloudy, Wet and Severe. A Wet day is rain or snow, and a Severe day is a storm or a blizzard. The day's mean picks which: below 1 °C means snow or blizzard.

That keeps the six named states with only four rows per season.

| From → to | Clear | Cloudy | Wet | Severe |
| --- | --- | --- | --- | --- |
| **Spring (Apr 1 – May 31)** |  |  |  |  |
| Clear | 0.50 | 0.30 | 0.17 | 0.03 |
| Cloudy | 0.30 | 0.40 | 0.25 | 0.05 |
| Wet | 0.25 | 0.35 | 0.35 | 0.05 |
| Severe | 0.30 | 0.40 | 0.25 | 0.05 |
| **Summer (Jun 1 – Aug 15)** |  |  |  |  |
| Clear | 0.50 | 0.30 | 0.15 | 0.05 |
| Cloudy | 0.30 | 0.35 | 0.28 | 0.07 |
| Wet | 0.20 | 0.35 | 0.37 | 0.08 |
| Severe | 0.30 | 0.40 | 0.25 | 0.05 |
| **Fall (Aug 16 – Oct 31)** |  |  |  |  |
| Clear | 0.40 | 0.35 | 0.20 | 0.05 |
| Cloudy | 0.20 | 0.40 | 0.33 | 0.07 |
| Wet | 0.15 | 0.35 | 0.42 | 0.08 |
| Severe | 0.20 | 0.40 | 0.35 | 0.05 |
| **Winter (Nov 1 – Mar 31)** |  |  |  |  |
| Clear | 0.60 | 0.28 | 0.10 | 0.02 |
| Cloudy | 0.30 | 0.45 | 0.20 | 0.05 |
| Wet | 0.20 | 0.40 | 0.32 | 0.08 |
| Severe | 0.20 | 0.30 | 0.15 | 0.35 |

Winter blizzards stay a second day 35% of the time, so a 3-day blizzard happens a few times a decade.

**What each state does.**

| State | Wind | Precipitation | Mean temperature offset (summer / winter) | Visibility | Solar charge | Bites |
| --- | --- | --- | --- | --- | --- | --- |
| Clear | 0–5 m/s, mean 2 | None | +2 °C / −3 °C | Full | 100% | Bright midday ×0.6 in shallows |
| Cloudy | 1–6 m/s, mean 3 | None | −1 °C / +2 °C | Full | 40% | ×1.2 |
| Rain | 2–8 m/s, mean 4 | 2–8 mm | −3 °C | 300 m | 20% | ×1.2 light; muddy water after |
| Snow | 2–8 m/s, mean 4 | 2–8 cm fresh | +3 °C (winter) | 150 m | 20% | — |
| Storm | 10–20 m/s gusts | 10–30 mm, afternoon thunder | −4 °C | 150 m | 10% | ×0.5 |
| Blizzard | 12–20 m/s | 10–25 cm fresh, drifting | 0 °C, then a cold kick | 20 m | 0% | ×0.5 |

Clear winter skies are colder and clear summer skies warmer. The offsets are re-centred so the long-run monthly mean still equals the month's normal in the weather table.

**Daily temperature by the hour.** Hourly air temperature is the day's mean plus the swing times a 24-entry curve in the weather table, coldest just before sunrise and warmest at 15:00. The curve is a table, so no sine is needed. Clear days use 1.4× the swing and cloudy days 0.6×.

**The valley inversion.** On clear winter nights with wind under 2 m/s, cold air pools on the valley floor. Temperature then rises 3 °C per 100 m of height above the floor, up to +8 °C, before the normal lapse of −0.65 °C per 100 m takes over. A cabin on a bench 50 m above the river is about 1.5 °C warmer on those nights, and the river flats are the coldest place in the valley.

**Fronts and pressure.** A front is any day the chain moves from Clear or Cloudy into Wet or Severe. Pressure falls for the 12 game hours before it, which gives the ×1.4 bite bonus in Fishing in depth, and the messenger's barometer reads "falling". In winter, the day after a Severe day gets a −6 °C anomaly kick on top of the carried anomaly: the arctic air behind the blizzard.

**Forecast.** The forecast reads the year table and blurs it with seeded rolls, one per day and lead time, so reopening the messenger never changes it.

| Lead | State shown | Temperature error | Severe flags |
| --- | --- | --- | --- |
| Tomorrow | The true state 80% of the time; otherwise one step away in the order Clear–Cloudy–Wet–Severe | ±2 °C | Shown |
| Day 2 outlook | True 60%; otherwise one step away | ±4 °C | Shown |
| Day 3 | Not shown | — | Cold snap, blizzard or storm flagged at 60%, with a 10% false-alarm rate |

The day-3 flag is what lets morning prompts say "Cold snap in 3 days". Sample forecast: "Tomorrow: snow, −18 °C to −26 °C, wind NE 6 m/s. Outlook: clear, colder."

**Sky signs.** Without battery, the sky still forecasts. A ring round the sun or moon appears on 70% of the evenings before a Wet or Severe day and 5% of other evenings.

High streaked cloud shows the evening before a windy day. The lookout's log teaches both.

**Cold snaps.** A cold snap is 3 or more days with the anomaly at −8 °C or below, or any forecast low under −25 °C. The anomaly process gives 2–4 a winter.

The alert reads "Cold snap for {n} days. Low {t} °C." When a blizzard leads into one: "Blizzard and cold snap for 3 days."

**The hard winter.** When the year's rare-event draw includes it, the anomaly gets an extra −15 °C for 30 straight days. The start is drawn between December 1 and January 29, so the stretch always ends by February 28 (Open world in depth › Rare events). A January normal near −23 °C then means nights past −40 °C.

The alert on the first day: "Arctic air settling in. Weeks below −40 °C."

**Storms.** Summer Severe days build in the afternoon, between 14:00 and 20:00, with thunder heard 10 km off. When a year draws the wildfire event, lightning starts it in dry spruce taiga on the first storm between June 10 and August 20 that follows at least 7 dry days, with wind over 4 m/s (Open world in depth › Rare events). Any other lightning fire spreads into a wildfire only on a day of Extreme fire danger; a fire the player lit always can (RECONCILE 38). Storm detail is in Weather and storms.

**Blizzards.** Visibility drops to 20 m and fresh snow drifts double in the lee of trees, cabins and banks. Tracks and blood trails are wiped, and wind chill applies on top.

The forecast flag gives a day's warning most of the time: "Blizzard tomorrow. Visibility 20 m." (see Weather and storms)

**Winter thaws.** About once a winter, a Wet day lands with a mean above 0 °C. Rain on snow leaves a crust, and creek overflow spreads under the snow for a week after.

**Snow on the ground.** Snow depth is tracked per day for four snow classes, not per cell, and each cell reads its class.

| Class | Collects | Melts |
| --- | --- | --- |
| Open ground | 100% of fresh snow | 0.4 cm per °C of daily high above 0 °C |
| Under spruce | 60% | Half as fast |
| South slope | 100% | 1.5× as fast |
| Lee drift | 150% on days with wind over 8 m/s | Half as fast |

Snow settles by 1% of its depth a day. On open ground the targets are 10–20 cm by October 31 and 35–45 cm by December 31, near the 40 cm deep-snow threshold. Depth peaks at 55–70 cm in March.

The valley floor melts out between April 25 and May 15; north slopes keep it into June and the alpine into July.

**The March crust.** From March 10 to April 15, a night below −10 °C after a day above 0 °C leaves a crust on open ground. The crust holds a walking player without snowshoes until 11:00, then softens. Early-morning travel in March is the year's fastest walking.

**Freeze-up, in stages.** Water freezes by class, using the water bodies stage 4 listed.

| Stage | Rule | Messenger |
| --- | --- | --- |
| Skim ice | First night below −5 °C: ponds glaze, melting by noon | "Skim ice on the ponds this morning." |
| Ponds freeze over | 3-day mean below −3 °C | — |
| Big lake freezes over | 5-day mean below −5 °C, about a week before the river | "The lake has frozen over. Thin ice." |
| Slush running | Main river, daily mean below −5 °C, for 3–7 days | "Slush running in the river. Pull your nets." |
| River freezes over | 5-day mean below −8 °C | "River frozen over. Under 5 cm. Stay off it." |
| Safe to walk | Ice estimate reaches 10 cm | "Ice estimate: about 10 cm on still water." |

Nets left in during slush tear, and a canoe in slush moves at half speed. From September 15 the messenger shows "About {n} days until freeze-up", worked from the normals and the forecast.

**Ice growth.** Ice starts at 2 cm on freeze-over day and grows by daily mean, capped at 110 cm on lakes and 90 cm on the main river.

| Daily mean | Growth |
| --- | --- |
| −2 °C to −10 °C | 0.5 cm a day |
| Below −10 °C | 1 cm a day |
| Any, with over 20 cm of snow on the ice | Half the above |

Thin spots are fixed by stage 4's flags: within 30 m below a falls, ice never passes 5 cm, and spring cells stay open. On the outside of fast bends, ice is half the estimate. The estimate is for still water, so the player tests by chopping a hole: 5 game minutes for 10 cm with the hatchet.

**Overflow.** From January to March, water pushes up through cracks onto creek and lake-edge ice under the snow. Stage 8 places 1–3 overflow patches per kilometre of creek, each 10–50 m long.

Stepping in soaks the feet, which is frostbite in minutes below −20 °C, and ices up snowshoes to 70% speed until scraped. The tells are a grey sag in the snow and steam on very cold mornings.

**Breakup, in stages.** Rotting starts when the 5-day mean rises above −2 °C. Ice then loses 1 cm a day, or 2 cm on sunny days.

Rotten ice counts as half its thickness for the 5, 10 and 20 cm safety limits. Rotten ice looks thicker than it is, which is the trap.

| Stage | Rule | Messenger |
| --- | --- | --- |
| Rotting | 5-day mean above −2 °C; 10–20 days before breakup | "Ice is rotting. It looks thicker than it is." |
| Breakup | 5-day mean above 2 °C; river ice moves out over 1–3 days | "Ice moving on the river. Stay off it." |
| Ice jam | Each breakup jams at 1–2 bends or the canyon for 6–24 game hours; water behind it rises to normal high water | "Ice jam at the bend. Water rising." |
| Spring flood | In a flood year, the jam lifts water to the flood-year line for 1–2 days | "Flood. Water rising toward camp." |
| Lakes clear | 7–14 days after the river | — |

After breakup the river runs high and muddy for 5–10 days, with the ×0.4 bite penalty. The year's new iron outcrops appear on cut banks once the water drops.

**Daylight.** Sunrise and sunset for each day at 64°N are a 365-row table read by `sim.time.Sun` (Minecraft development › The clock), so runtime code never computes sun angles from sine. The longest day (June 21) has about 21.8 hours of daylight and the shortest (December 21) about 3.7. From about May 15 to July 28, twilight lasts all night and there is no dark.

The table also stores the sun's height each hour, for lighting and the solar panel.

**Seasonal dates.** Each year draws its own dates inside these windows. Alerts fire once, on the day.

| Event | Window | Rule | Alert |
| --- | --- | --- | --- |
| Birch sap runs | Apr 10 – May 5 | Days above 3 °C, nights below 0 °C | "Birch sap is running." |
| Ducks and geese arrive | May 5 – 20 | Flocks on lakes and oxbows | "Geese on the lake." |
| Birch leaves out | May 15 – 25 | — | — |
| Last spring frost | May 20 – Jun 10 | See Crop farming | "Frost likely tonight, {t} °C." |
| Mosquito hatch | Jun 8 – 18; swarms end Jul 25 – Aug 5 | Still air near water and bog | "Mosquitoes hatching." |
| King salmon in | Late June | Runs through July | "King salmon are in the river." |
| Chum run | Mid-July | Runs through August | "Chum salmon running at the bend." |
| Bears feed heavily | Aug 15 – Oct 15 | Berries and salmon | "Bears feeding heavily." |
| First fall frost | Aug 20 – Sep 15 | See Crop farming | "Frost likely tonight, {t} °C." |
| Leaves turn | Aug 25 – Sep 10 | Birch yellow, then bare in 2 weeks | — |
| Caribou pass | 5–10 days in Sep 5 – 30 | In years with a herd | "Caribou moving on the tundra." |
| Moose rut | Sep 10 – Oct 10 | Bulls answer calls | "Moose rut has begun: bulls aggressive." |
| Ducks and geese leave | Sep 5 – 25 | — | — |
| Snow that stays | Oct 1 – 20 | Lowlands | — |
| Bears den | Oct 20 – Nov 5 | — | "Bears are denning." |
| Freeze-up | Late Oct – mid Nov | Stages above | As above |
| Burbot spawn (January 25 – February 28, peak February 10); wolf breeding | February | — | — |
| Snow peak | March | — | — |
| Breakup | Apr 15 – May 10 | Stages above | As above |

### A year of work

Every month should mix three or four kinds of work, and no single chore should own a month. That variety is what keeps a long year from turning into grind.

&#91;embedded content: design target · 12 activities × 12 months\]

The quietest stretch is December to February, when work moves to the ice, the trapline and indoor crafting by fat lamp. That is where spirits, light and the wolf matter most.

**Month by month.** Each month has a deadline that the next month depends on. Missing it never ends the run, but it makes a later month harder in a way the messenger can name.

| Month | Main work | Done by the end of the month | If it slips |
| --- | --- | --- | --- |
| May | Camp, rod fishing, snares, spruce tips and birch sap, clearing garden beds | Lean-to camp; 5+ snares on trails; barley sown from May 20 | No food ahead when the cabin work starts |
| June | Potatoes in by June 15, repairing the outpost cabin, felling firewood, pike and grayling | 15 of the 33 firewood trees felled and split; longbow stave seasoning | Wood cut late is still green in winter |
| July | King then chum salmon, smoking and drying, first berries, cabin walls | All 33 trees split and covered by July 31; 10 kg of dried fish | Green wood gives half the heat and twice the smoke |
| August | Chum and coho, blueberries, mushrooms, cabin roof, smoke rack or smokehouse | Roof on before the fall rains; potatoes dug before the first hard freeze | A wet cabin; crops lost at −5 °C |
| September | Moose rut hunt, caribou camp, lingonberries and rose hips, ducks, hides, chinking | Raised cache built; clay dug for the stove before the ground freezes in October; root cellar filled | No stove or chinking until spring; a raided cache |
| October | Last hunts, rendering fat and pemmican, hauling wood into the woodshed, sewing winter gear | Stove and chimney working; 330 armloads under cover; mittens and hat made | 5 armloads a day instead of 2; frostbite in November |
| November | Ice fishing once the ice is 10 cm, setting the trapline, burbot set lines | A trapline of 10+ sets on the map | No winter food beyond stores |
| December | Trapline, ice fishing, indoor crafting by fat lamp | Mukluks and parka finished | Short trips only below −30 °C |
| January | Trapline, ice holes, guarding stores (mice, wolverines, wolves) | Three deadfalls around every cache | Mice take 2–3% of the food a week |
| February | Rationing, burbot spawn set lines, tracking moose in deep snow | A ration set that reaches April | Weight falls toward BMI 19 |
| March | Sled-hauling next summer's building logs, ptarmigan, crust travel | Logs for the summer's builds skidded to camp | Summer building waits on hauling |
| April | Pulling nets and tip-ups, birch sap, shed antlers, moving low caches | Nets out before the 5-day mean passes 2 °C; caches above the flood line | Torn nets; a flooded cache |

**Seasoning sets the summer.** Split firewood under cover seasons in 90 days with a daily mean above 5 °C, and twice as fast in a woodshed. In interior summers that means wood must be cut by July 31, or by mid-August with a woodshed, to burn at full heat in winter.

The messenger counts it down from June 1: "Firewood: 140 of 330 armloads cut. 61 days to season."

**Variety is tested.** Every job carries one activity tag: fishing, hunting, trapping, foraging, farming, woodcutting, building, preserving, hides and sewing, crafting, exploring or hauling. The balance simulator checks its good player month by month.

The top tag may take at most 45% of work hours, and at least 3 tags must take 10% or more. A month that fails names the system that is too slow, so the fix is a faster tool, not less content.

**A December day.** With 3.7–4 hours of light, the day splits cleanly between outside and in.

| Hours | Where | Work |
| --- | --- | --- |
| 10:30–14:30 | Outside, in daylight | Run the trapline (2 h), check ice holes and tip-ups (1 h), carry 2 armloads to the stove (0.5 h) |
| 14:30–21:00 | Inside, by the fat lamp | Crafting from the list below |
| 21:00–07:00 | Bunk | Sleep, woken only by cold or wolves |
| 07:00–10:30 | Inside | Eat, mend, process the catch, feed the wolf |

A 6-hour lamp evening burns 100 g of lard, about 900 kcal of fat that the player chose not to eat. That ties light to the larder in the darkest month.

**Winter crafting list.** These jobs fill the indoor hours; all can be queued and run with Work until done.

| Job | Time | Needs |
| --- | --- | --- |
| Fur mittens | 3 h | 4 hare pelts, sinew |
| Mukluks | 6 h | Moose hide, fur liner, sinew |
| Parka | 12 h | 2 caribou hides or 1 bear hide |
| Lacing snowshoes | 5 h | Birch frame, rawhide |
| Mending a gill-net segment | 2 h | Cordage |
| Six arrows | 3 h | Shafts, fletching, heads, pitch glue |
| Rendering 5 kg of fat | 2 h | Fat, the pot, a fire |
| 5 kg of pemmican | 3 h | Dried meat, lard, berries |
| Ten tallow candles | 1 h | Moose or bear fat |
| Carving decoys or a trophy mount | 4 h | Birch, a knife |

## Survival tuning

When body weight falls to BMI 17 the messenger sends an SOS. In Endless mode (the default) the player is flown out, returns 14 game days later at BMI 19 with only the starting kit, and finds the world and everything built still standing. In Hardcore mode the SOS ends the world, as in the source mod. Hypothermia collapse works the same way. With the default body, that is about 20 kg of loss, tuned so a player eating nothing hits it in roughly 18 game days.

**Default body:** 183 cm, 77 kg, age 25 (BMI 23.0). Base burn uses the Mifflin–St Jeor equation:

```latex
BMR = 10 \cdot kg + 6.25 \cdot cm - 5 \cdot age + 5 \approx 1794\ \text{kcal/day}
```

Weight change per day = (kcal absorbed − kcal burned) ÷ 2,400 kg, stepped each game minute by the body tick. Real fat is about 7,700 kcal per kg; 2,400 is a game-speed value so a few bad days hurt.

| Activity | kcal per game hour | Energy cost |
| --- | --- | --- |
| Sleeping | 60 | Restores |
| Resting, fishing, by the fire | 75 | Very low |
| Walking | 250 | Low |
| Carrying a heavy load | 400 | Medium |
| Chopping, digging, building | 450 | High |
| Swimming | 550 | Very high |
| Shivering (cold) | +150 to +300 | Adds to the above |

**Energy (0–10 bolts).** Sleep restores up to 10, capped lower when calories, fat or water are low. Below 3 bolts every task takes 50% longer and falls get likelier; below 1 there is a blackout chance each minute, which in water means drowning risk.

**Sleep.** The player can sleep only when energy is below 7 or it is night, so the day has to be planned. Cold, wet, hunger, rain without a roof, or an animal close by wakes them early. A bed in a warm room gives full rest; the bare ground gives 70%.

**Macros.** Carbs, fat and protein each track 0–100%. Lean meat alone (hare, grouse) caps energy at 6 bolts after a few days, so fat from beaver, bear and fish matters, and low fat in winter adds a cold penalty.

**Warmth.** Feels-like temperature = air − wind chill − wet penalty (up to 10 °C when soaked), then add clothing insulation, shelter (+5 to +15 °C) and fire (up to +25 °C within 3 m, fading to 0 at 8 m). Below 10 °C feels-like, cold builds toward three hypothermia stages: shivering, slowed, collapse.

**Water.** 2.5 L per day, plus 1 L on heavy work days. Unboiled water carries a 30% chance of gut illness, which halves calories absorbed for two days.

**Bugs.** From mid-June to late July, mosquitoes swarm in still air near water and bog, with black flies (June to mid-August), no-see-ums (July to late August) and deer and horse flies on hot days adding to the same pressure. Exposure cuts sleep quality by 30% and adds slight aim sway; smoke within 6 m, a head net, or wind over 4 m/s keeps them off.

**Injuries.** Wound (bleeds until bandaged), infection (an untreated wound after 12 game hours; causes blackouts), burn (standing in fire), frostbite (exposed skin below −20 °C), sprain (falls). Each has one clear treatment the messenger names. Body, injury and sickness places each one on one of ten body parts.

**Realism settings.** Presets (Gentle, Standard, Brutal) plus sliders for calorie burn, cold, spoilage speed, animal numbers, predators on or off, and game-day length (a world option in steps from 24 to 72 real minutes, default 24, which slows only the World clock), so each player can skip the parts they find dull. The numbers in this doc are Standard.

### The body tick

The body is one model that updates once per game minute, in a fixed order, and writes one record that the HUD, the journal and the save all read. In the build it is `body.BodyTicker` on `ClockEvent.Minute`, writing the player attachment `yearofice:body`, with every threshold below in `data/yearofice/body/standard.json` and its tuning mirror in `yearofice-common.toml`. The six steps here are the design view; Minecraft development › Player systems and client › The body tick splits them into 13 without reordering them: sample, burn, stomach and absorb, weight, water, warmth, energy, Condition, problems, derive, Spirits, check and sync.

1. **Burn.** The activity rate from the table, scaled by BMR ÷ 1,794 and the preset, plus shivering, plus a Rest-day healing cost of 10% of BMR a day while a problem heals.
2. **Absorb.** Food eaten this minute fills the stomach (1.8 kg; stew and broth fill at half their mass, water and tea not at all; it empties 7% a game hour awake and 4% asleep). Its kcal are absorbed times the gut factors, which multiply: 1.0 healthy, 0.5 with gut illness, 0.85 with parasites, 0.8 with lean-meat nausea, 0.8 past the raw-fat limit.
3. **Weight.** Change by (absorbed − burned) ÷ 2,400 kg.
4. **Water, warmth, energy, Condition,** in that order, each reading the earlier results.
5. **Problems.** Advance every injury and illness timer.
6. **Spirits** add up during the day and apply once, when the player sleeps.

```json
{
  "schema": 1, "height_cm": 183, "sex": 0, "kg": 71.4,
  "kcal_in":  [0,0,0,0,0,0,0,650,0,0,0,0,450,0,0,0,0,0,1000,0,0,0,0,0],
  "kcal_out": [60,60,60,60,60,60,75,250,450,450,450,250,75,450,450,450,250,250,75,75,75,60,60,60],
  "stomach_kg": 0.6, "water": 58, "warmth": 74, "energy": 6.2,
  "awake_min": 570, "asleep": false, "sleep_q": 0.0, "condition": 88,
  "vit_c_days": 21, "lean_days": 0, "keto_days": 0,
  "nausea": 0, "fever_phase": 0, "grime": 12,
  "spirits": 55, "spirits_day": 2, "sos": 0,
  "problems": [ { "id": "cut", "part": "left_arm", "stage": 1, "timer": 1412, "dressed": true, "infected": false } ]
}
```

### Calories and weight

**Rates are totals.** The activity table already includes the base burn, at the default body's BMR of 1,794 kcal. Every rate scales with BMR ÷ 1,794, recomputed from the current weight, so a thin body burns less and starvation slows near the end, as it does in life (RECONCILE 17). Snow and cold add to the rate: walking costs ×1.3 on snowshoes or in 20–50 cm of snow and ×1.8 breaking trail over 50 cm without snowshoes (packed trail and the March crust count as bare ground), and outdoors below −20 °C a warm, dressed body adds +30 kcal an hour of non-shivering heat. Those two rules make a winter trapline day cost about 3,000 kcal and a hauling day 4,000 or more.

```latex
kcal_{hour} = rate_{activity} \cdot \frac{BMR}{1794} \cdot m_{preset} + kcal_{shiver}
```

**Body weight bands.** The vitals screen shows weight in kg and BMI to one decimal, as the video did.

| BMI | Word shown | Effects |
| --- | --- | --- |
| 25 and up | Heavy | +1 °C feels-like from body fat; no other effect |
| 20–25 | Healthy | None |
| 18.5–20 | Thin | Strength drops with BMI (see Macros and strength); −1 °C feels-like |
| 17–18.5 | Underweight | Energy cap 7; −2 °C feels-like; Condition heals at half rate; Spirits −1 a day |
| 17 or less | — | SOS (see Collapse and SOS) |

### Energy

Energy (0–10 bolts) drains while awake and refills only by sleep. A full working day should take the player from 10 to about 4 by evening.

| Doing | Bolts per game hour |
| --- | --- |
| Resting, fishing, sitting by the fire | −0.2 |
| Walking | −0.4 |
| Heavy work or a heavy load | −0.6 |
| Swimming | −1.0 |
| Sprinting | −0.05 per game minute (3 an hour) |

**Energy cap.** Sleep refills only up to the lowest cap that applies. Each cap shows as a dimmed segment of the bolt bar, with its cause named on the vitals screen.

| Cause | Cap |
| --- | --- |
| Last 24 h's balance below −1,500 kcal | 8 |
| Underweight (BMI under 18.5) | 7 |
| Water below 30 / below 15 | 7 / 4 |
| Gut illness | 7 |
| Lean-meat sickness (see Macros) | 6 |
| Scurvy stage 1 / stage 2 | 8 / 6 |
| Awake 24 h / 48 h / 72 h | 7 / 5 / 3 |

**Low energy.** Below 3 bolts every task takes 50% longer, the screen loses 30% of its color, and slips on slopes over 20° are three times as likely. Below 1, a blackout rolls at 2% per game minute: the player drops for 1 game hour and wakes at 1.5 bolts. A blackout in water, or energy reaching 0 while swimming, is drowning (see Collapse and SOS).

### Sleep

**When the player can sleep.** Energy below 7, or the clock between 21:00 and 06:00. The clock rule, not the sun, defines night, because the June sun barely sets at 64°N. Z opens a radial: Sit, Wait by the fire (1–12 h), Sleep until rested, Sleep until dawn.

**Restoring.** Sleep restores 1.25 bolts per game hour times quality, so 8 hours in a good bed refill 10. Asleep, the world runs at the sleep rate (180 game seconds a tick); in a later co-op world it runs at that rate only while every player sleeps (RECONCILE 115).

```latex
q = q_{bed} \cdot m_{bugs} \cdot m_{spirits} \cdot m_{pain} \cdot m_{light}
```

| Factor | Values |
| --- | --- |
| Bed | Bare ground 0.70, debris bed 0.85, bough bed 0.95, bunk with mattress 1.0; any bed in a warm room (10 °C or more) 1.0 |
| Bugs | 0.7 when exposed (see Bugs), 0.85 under a head net only |
| Spirits | Bright 1.1, Steady 1.0, Low 0.9 |
| Pain | 0.8 with an untreated injury or gut illness; painkillers remove it |
| Light | 0.9 in the open under the midnight sun; 1.0 in a cabin with a hide window |

**What wakes the player.** Each cause wakes them at once and names itself: "Woke at 03:10: cold. The fire is down to banked coals."

- Warmth below 40, or wetness above 60%.
- Hunger: after 4 hours of sleep when the last 24 h's balance is below −2,000 kcal.
- Thirst: water below 15.
- Rain on a bed without a roof, which also soaks the bed.
- A predator in the Active tier within 60 m, or any large animal within 30 m. A Bonded wolf growls first, giving 10 real seconds.
- Smoke in the room above 50%, or fire spreading within 10 m.

**Sleep debt.** Hours awake count from the last sleep of 4 hours or more. At 24 hours the energy cap drops to 7, at 48 hours every task takes 30% longer (the video's all-night butchering), and at 72 hours the screen blinks into 1-second microsleeps every few minutes. Any 4-hour sleep clears the debt.

**Sleeping cold.** Body heat drops in sleep, so feels-like counts 2 °C lower while asleep, before the bed's own insulation is added. A player who sleeps without a fire on a May night near 0 °C, in the starting clothes, wakes cold between 02:00 and 05:00.

### Macros and strength

**What the meters mean.** Each macro meter is the last 72 game hours of intake divided by three days of its target, capped at 100%. Daily targets are protein 75 g (300 kcal), fat 90 g (800 kcal) and carbs 150 g (600 kcal); any other calories count toward energy only.

| When | Effect |
| --- | --- |
| Protein below 40% | Condition falls 1 a day and heals 25% slower, which lowers strength |
| Fat meter below 30% (under about 27 g a day) and protein over 45% of the day's calories, for 3 days | Lean-meat sickness: energy cap 6. From day 6, nausea: 20% fewer calories absorbed. Cured by 2 days with fat at 60% or more |
| Fat below 30% and air below −10 °C | −3 °C feels-like |
| Carbs below 30% for 3 days and fat below 60% | Heavy work takes 10% longer. With fat at 60% or more for 10 days the body adapts and the penalty goes |

That last rule is how a winter on fish, fat and meat can work, as it does for people in the north, while lean hare alone cannot. Alert line: from the first day both lean-meat conditions hold (fat meter below 30% and protein over 45% of the day's calories), the Trail Messenger posts "Body needs fat." once a game day, so the player has 2 days' warning before the sickness starts on day 3. It stops when the fat meter is back at 30% or more, or, once the sickness has started, at its cure (video row 185).

**Strength (0–100).** Strength is not stored or trained. The body tick works it out each minute from Condition and body weight, so small game builds the body for big game through weight and health, as in the video.

```latex
Strength = Condition \cdot \mathrm{clamp}\left(\frac{BMI - 17}{6},\ 0,\ 1\right)
```

The default body (Condition 100, BMI 23) starts at 100. Each bow's requirement, and what happens below it, is in How the player hunts and in Weapons and arrowheads. Food reaches strength in two ways: eating raises BMI, and the protein and parasite rules act on Condition. The Feast ration gains weight fastest, which is why it comes before a big hunt.

### Warmth

Warmth is a 0–100 meter, starting at 100. Feels-like temperature, built from the rule above, sets how fast it moves.

```latex
T_{feel} = T_{air} - C_{wind} - 10w + I_{clothing} \cdot (1 - 0.7w) + S_{shelter} + F_{fire} + A_{activity} + B_{body}
```

- **Wind chill:** C = 0.8 °C per m/s of wind, at most 16 °C, only when air is below 10 °C. Dense spruce, a lean-to or a reflector wall halves it.
- **Wetness:** w runs 0–1, read from the worn layers.
- **Clothing:** I is the summed warmth of what is worn, after wind block and the warmth each garment keeps when wet. Every garment value, including the starting clothes, is in Clothing. The 0.7 factor above is the default when a garment gives no kept-warmth value.
- **Shelter:** a lean-to +3, a snow trench +10, the repaired outpost cabin +8, an unheated chinked cabin +15. Inside a closed room, the room's temperature from Warm rooms replaces air and wind drops out.
- **Fire:** by setting within 3 m (Low +10, Steady +18, Hot +25), fading in a straight line to 0 at 8 m.
- **Activity:** walking +3, heavy work or a load +6, running +8.
- **Body:** the BMI band's modifier and the low-fat winter penalty.

**How warmth moves,** per game minute:

```latex
\frac{dW}{dt} = \begin{cases} -0.02\,(10 - T_{feel}) & T_{feel} < 10 \\ 0 & 10 \le T_{feel} \le 18 \\ +0.05\,(T_{feel} - 18) & T_{feel} > 18 \end{cases}
```

On top of that: a hot drink +5 at once, a hot meal +3, raw snow −8 per 0.5 L, very cold water −2 per 0.5 L when warmth is under 50. In water, warmth falls 6 a minute. Out of water but soaked (wetness over 70%) with air below 0 °C, it falls at least 6 a minute until the player is within 3 m of a fire or in a warm room. That is the 10–15 minute race to a fire after thin ice.

**Sweat and drying.** How fast garments wet from sweat and dry by a fire, on a line or indoors is set in Clothing. The body only reads the resulting w each minute, plus the soaked rule above.

**Stages.**

| Warmth | Stage | Effects | What the player sees |
| --- | --- | --- | --- |
| 70–100 | Warm | None | Nothing |
| 50–70 | Cool | None yet | Frost starts at the screen corners at 60; breath fog whenever air is below 0 °C |
| 30–50 | Shivering | +150 kcal/h at 50, rising to +300 at 30; fine tasks (bow drill, knapping, tying snares) get a 25% smaller timing window | Camera shiver, teeth chatter, frost covers a third of the screen edge |
| 10–30 | Slowed | 70% walk speed, no running, work 50% slower, aim sway (the "shaking hands below 30"); shivering stays at +300 | Heavy footsteps, slowed head turns, frost at half the screen |
| 0–10 | Collapse risk | A stumble and fall every 30 real seconds of movement | Vision greys from the edges; heartbeat audio |
| 0 | Collapse | Hypothermia collapse: SOS rules | — |

**Frostbite limits.** Each zone freezes when feels-like drops below its "safe to" temperature, which comes from what is worn on it (see Clothing). Bare skin is safe to −20 °C, the doc's exposed-skin rule. The timer and stages are in Health, medicine and nutrition.

### Water

Water is a 0–100 meter where 100 is a 3 L reserve, so 1 point is 30 mL and a 0.25 L sip restores 8 points.

| Drain | Points per game hour |
| --- | --- |
| Base, awake or asleep (2.5 L a day) | −3.5 |
| Each hour of heavy work (the extra 1 L on a work day) | −5 more |
| Air above 20 °C | All drains ×1.2 |
| Gut illness | −1.4 more (1 L a day) |

Food carries water too: a bowl of stew +10, a cup of berries +3, a cup of tea +8.

| Water | State | Effects |
| --- | --- | --- |
| 60–100 | Fine | None |
| 30–60 | Thirsty | An icon on the strip; no penalty |
| 15–30 | Dehydrated | Energy cap 7, work 10% slower, a slow headache pulse at the screen edge |
| 0–15 | Severe | Energy cap 4, vision narrows by 20%, sleep is broken |
| 0 | Blackout | Down for 12 game hours where they stand (the video's blackout); wake at 10 water and −10 Condition. Outdoors below 0 °C the cold keeps working on them during that time |

A resting player who never drinks blacks out after about 29 game hours; on a heavy work day after about 15.

### Bugs

Bug pressure is a sum of four terms, B\_total = B\_mosquito + B\_fly + B\_midge + B\_tabanid (RECONCILE 61). The mosquito term B below is computed for the player's cell every game minute; black flies and no-see-ums are set in Bestiary › Black flies and no-see-ums, deer and horse flies in Birds and the air › Biting flies. The player is exposed when B\_total is 0.3 or more and nothing protects them.

```latex
B = b_{date} \cdot h_{habitat} \cdot t_{time} \cdot \max\left(0,\ 1 - \frac{v_{wind}}{4}\right) \cdot f_{temp}
```

| Term | Values |
| --- | --- |
| Date | 0 before June 10; rises to 1 by June 20; 1 to July 15; falls to 0 by July 31 |
| Habitat | Bog 1.0, within 100 m of still water 0.8, tundra 0.6, riparian 0.6, spruce taiga 0.5, birch slope 0.4, gravel bar 0.3 |
| Time | Dawn and dusk 1.5, night 1.0, midday 0.6 |
| Temperature | 0 below 8 °C, 1 above 12 °C, a straight line between |

**Protection.** Smoke within 6 m, or 10 m from a smudge (green boughs on any fire). A head net, sewn from the outpost cabin's window screen (one only, 10 game minutes), or later from fine fibre mesh (Clothing); it does not stop no-see-ums. Crushed yarrow rubbed on skin halves B for 2 game hours. Indoors with the door shut and a smudge burning, B is 0.

**Effects of exposure.** Sleep quality ×0.7, aim sway +10%, a slap animation every 20 real seconds that breaks a held draw, and −1 Spirits per 3 hours exposed. The head net removes the sway and the Spirits loss but darkens the view: sight range −10% at dusk. The sound is a whine whose volume follows B, with dark specks drifting across the view above 0.6.

**Bugs on the HUD.** The strip shows a mosquito icon while exposed. The forecast adds a bug index for tomorrow (Low, High, Severe) from expected wind and temperature, and the season alert "Mosquitoes hatching" fires the first day B passes 0.3 anywhere in the home range.

### Injuries

Every injury event carries a source, a Condition loss and an injury roll, listed in Danger in detail. The roll picks a body part (weighted by the source: a fall hits legs, a knife slip hits the off hand) and a problem from Health, medicine and nutrition. The problem then runs on its own timer and shows on the body figure (see Clothing), with its one treatment named on the vitals screen. Body, injury and sickness defines the ten parts, their health, the part weights per source and every problem by part; `BodyService.applyInjury` is the only way in, animal attacks included (RECONCILE 90).

### Collapse and SOS

| Trigger | Endless (default) | Hardcore |
| --- | --- | --- |
| BMI 17 or less | Messenger sends SOS automatically | The world ends |
| Warmth 0 (hypothermia collapse) | Collapse, then automatic SOS | The world ends |
| Condition 0 | Collapse, then automatic SOS | The world ends |
| Drowning (blackout in water, energy 0 while swimming, or the useful swim time running out: banded by water temperature in Swimming and working in water, ×0.8 with a Hurt torso and ×0.6 Badly hurt; RECONCILE 120, 123) | The messenger's water alarm sends SOS; the player is found on the bank | The world ends |
| An untreated severe condition: infection stage 3 for 24 h, a broken bone left unsplinted 3 days, deep frostbite not rewarmed in 6 h, scurvy stage 3 | No SOS; the condition keeps hurting | Extraction: the world ends |
| Player calls SOS (hold the SOS button 3 real seconds, then confirm) | Flown out, same as above | The world ends |

**The Endless sequence.**

1. The screen fades to white noise and rotor sound. The messenger shows "SOS sent 14:22, day 143."
2. The player writes the one-line "why I left" entry, or skips it. The game fills in the cause if skipped ("Hypothermia, north range").
3. The clock jumps 14 game days in closed form and every loaded chunk runs its catch-up for the gap, in about 2 real seconds: spoilage, mice, raids, fires going out, traps, crops and frost (Minecraft development › Collapse, SOS and the flight). In a later co-op world the jump is per player (RECONCILE 83, 115). The tamed wolf follows its normal trust rules, so a long absence can cost it. Endless spares the save, not the wolf.
4. Everything carried except worn clothing and the messenger goes into a `left_pack` block at the collapse spot (the nearest bank after drowning), shown as a "pack left behind" pin on the map. The block never expires and keeps its chunk from being pruned; animals take food from it by the cache theft rules, and nothing else is lost (RECONCILE 84, 89, 97).
5. The player returns by bush plane to the gravel bar below the outpost on day +14. They are at BMI 19, Condition 80, warmth, water and energy full, Spirits 40, vitamin C reserve 30 days, with only the starting kit. Lost fingers or toes stay lost.
6. The journal logs the flight, and the year card counts "flights out."

**Hardcore end.** The world closes, and its journal and year cards stay readable from the title screen as a run record.

### Realism presets

A preset sets every slider at once; the player can then move any slider. Changes mid-run are allowed and logged in the journal with the date. Endless or Hardcore is chosen separately at world creation. The three presets and every slider's default live in `yearofice-common.toml` (one `[presets.gentle]`, `[presets.standard]` and `[presets.brutal]` table each); the world's own choice is saved in `yearofice_world`, and `/yoi preset <name>` or `/yoi set <slider> <value>` changes it in a running world, logged like any other change (RECONCILE 118). Every rate in this doc is per game minute or game hour, so the day-length option changes how long a game day lasts in real time, never what it costs the body (RECONCILE 117).

| Setting | Gentle | Standard | Brutal | Slider range |
| --- | --- | --- | --- | --- |
| Calorie burn | ×0.5 | ×1.0 | ×1.25 | ×0.25–1.5 |
| Cold (warmth loss rate) | ×0.5 | ×1.0 | ×1.3 | ×0.25–1.5 |
| Water need | ×0.75 | ×1.0 | ×1.2 | ×0.5–1.5 |
| Unboiled water illness chance | 15% | 30% | 40% | 0–50% |
| Bleeding and infection speed | ×0.5 | ×1.0 | ×1.5 | ×0.25–2 |
| Spoilage speed | ×0.5 | ×1.0 | ×1.5 | ×0.25–2 |
| Animal numbers | ×1.5 | ×1.0 | ×0.7 | ×0.5–2 |
| Predators | Shy: never start a charge | On | On, wolves test the player at a higher threshold of health | Off, Shy, On |
| Pack and messenger pause the world | Yes | No | No | On or off |
| Warnings | Repeat every time | Once, then again after 24 h clear | Once per run | — |
| Game-day length | A world option set at creation, default 24 real minutes in every preset; only the World clock scales with it, at 720 ÷ L game deciseconds a tick, while Travel, Close, Work and Sleep keep their rates (RECONCILE 117, 145) |  |  | 24, 30, 36, 40, 45, 48, 60 or 72 real minutes |

Chase break-off at 150 m and the warning before every charge hold in every preset. With Gentle's ×0.5 burn, a player eating nothing reaches BMI 17 in about 36 days, which is the "roughly twice as late" in the M4 test; Brutal reaches it in about 14.

### What the HUD shows

The HUD is the messenger, and its place on screen is set in Inventory and backpack. This table sets only what the vitals strip shows; the clothing body figure, which tints cold, wet or worn zones, is in Clothing. The strip is always on and costs almost no battery, and it stays empty when all is well: an icon appears only when its need crosses the first warning, and stays until it is fixed.

| Need | Strip icon appears | Full vitals screen shows | Body cue with no HUD |
| --- | --- | --- | --- |
| Calories | 24 h balance below −1,000 kcal | Today in and out, weight, BMI, the weight graph, and stomach fill in percent, which also shows on the eat prompt ("R  Eat dried meat (+6%, stomach 36%)"); there is no hunger line (RECONCILE 13) | Stomach growl; the hands look thinner below BMI 18.5. The Trail Messenger posts the alert line "You need more food." once a game day while the 24 h balance stays below −1,000 kcal or BMI is under 18.5 (video row 71) |
| Water | Below 60 | Meter, liters drunk today | Dry swallow sound, headache pulse |
| Energy | Below 5 bolts | Bolt bar with dimmed capped segments and their causes | Yawns, color loss below 3 |
| Warmth | Below 70 | Meter, feels-like and each term of it | Frost edges, shiver, breath fog |
| Wet | Above 30% | Overall wetness and time to dry here; per-zone detail is on the Clothing body figure | Dripping sound, darker sleeves on the hands |
| Condition | Below 60, or any active problem; Bleeding and Fever get their own strip icons (Body, injury and sickness › The status list) | Each problem's stage, its body zone and its one treatment | Limp, heartbeat below 30 |
| Spirits | Only when Low | One word: Bright, Steady or Low | Music and humming change (see Spirits) |
| Bugs | While exposed | Bug index now and tomorrow | Whine and specks |

When the battery dies, the strip goes dark and only the body cues remain. That is designed: a player in December who let the messenger drain reads the body, not numbers.

**Tests.** The M4 starvation test stands. Add: a soaked player at −20 °C with no fire reaches warmth 0 in 13–17 game minutes; a resting player who never drinks blacks out at 26–32 game hours; an all-hare diet (fat under 30%) hits the energy cap of 6 on day 3 or 4; a fed, working player at BMI 19 on the Feast ration regains full longbow draw within 10–14 days; smoke within 6 m sets bug exposure to zero in every habitat.

## Moment-to-moment mechanics

These are the actions the player performs hundreds of times, so each must be quick to do, honest about its cost, and tied to the body model above.

**Four time rates.** The world clock runs 1 game minute per real second at the default day length of 24 real minutes; a world may set 24, 30, 36, 40, 45, 48, 60 or 72 minutes at creation, which slows only this World clock (720 ÷ L game deciseconds a tick); Travel, Work, Close and Sleep keep the rates in the table (RECONCILE 117, 145). Travel runs slower, so 1 km on foot costs about 1 game hour. Hold-to-work jobs charge their game minutes as a time-lapse, so a 15-minute gutting fills its ring in 3 real seconds. When danger or fine handwork needs real-time reactions, the close clock slows the world to 1 game minute per 10 real seconds.

| Rate | Game time per real second | Runs when |
| --- | --- | --- |
| World clock | 1 minute | Stalking, waiting, watching a bobber, camp chores |
| Travel | 12 seconds (1 game minute per 5 real seconds) | Moving on foot, snowshoes, sled, raft or canoe with no job running |
| Work time-lapse | 5 minutes; 20 minutes in "work until done" | A hold-to-work ring is filling, or a queued job is running |
| Close clock | 6 seconds | Any minigame (bow drill, knapping, the fishing fight, the self-rescue), a drawn bow, an alerted predator in the Active tier, bleeding, in water, soaked below 0 °C |

The close clock ends 10 real seconds after its last cause clears. Needs, sun, spoilage and animal schedules all slow with it, so a bear encounter or a fish fight costs minutes, not an hour.

**Units in these sections.** A timer written in seconds (s) is real time. A timer written in game minutes or hours runs on whichever rate is active. So the bow drill's 60–90 s of strokes is real time and charges about 6–9 game minutes, which is where its 60 kcal a try comes from.

**Every action is one record.** Each action the player can perform is one file, `data/yearofice/action/<id>.json`, read by `build.work.WorkService`, so tuning never touches code.

```json
{
  "id": "split_round",
  "input": "lmb",
  "real_s": 1.2,
  "game_min": 3,
  "activity": "heavy_work",
  "noise_m": 25,
  "needs_light": true,
  "practice": { "max_speedup": 0.4, "reps_to_max": 30 },
  "warn_once": ["wet_round", "low_energy_axe"]
}
```

**Warn once.** Every mistake the game will punish has a warning id. It fires the first time its condition becomes true, names the fix in one line, and fires again only after the condition has been false for 24 game hours. Warnings never block an action.

**Interruptions.** Any danger alert (a huff, a growl, ice cracking, a tree creaking) ends hold-to-work, "work until done", waiting and sleep at once. The tool drops to the ready position and the close clock starts.

### Controls and interaction

Tapping does the obvious thing; holding does the work. The keys are Minecraft's defaults, as in Minecraft development › Input and keybinds, all rebindable, with one 0.25 s tap-or-hold threshold; touch is dropped. Where later sections still write E for use, Q for eat, G for drop, Tab for the pack or X for the off-hand, read right mouse, R, Q, E and F (RECONCILE 5, 31). Before any job starts, a work preview states its cost: "Fell spruce: 6 swings, about 0.4 h, 180 kcal."

| Key (Minecraft default) | Touch (dropped in Minecraft) | Action |
| --- | --- | --- |
| W A S D | Left stick | Move |
| Ctrl, or double-tap W | Double-tap stick | Run (costs energy) |
| Shift | Crouch button | Crouch and stalk |
| Space | Jump button | Jump, climb low ledges |
| RMB tap | Use button | Pick up, open, inspect, drink at water |
| RMB hold (0.25 s) | Hold Use | Work: chop, dig, gut, build, with a progress ring |
| Left mouse | Action button | Tool or weapon: swing, chop, strike, reel |
| Right mouse | Aim button | Hold: draw the bow or cast the line; tap: place a building piece |
| 1–9 | Hotbar | Hotbar slots (mouse wheel cycles; F swaps the off-hand item) |
| R | Eat button | Eat or drink the quick-slot food |
| E | Pack button | Pack, gear and crafting |
| M | Messenger button | Map, forecast, notebook, prompts, ambitions |
| Z | Rest button | Sit, wait or sleep |
| G | Whistle button | Call the tamed wolf |
| Q | Drop button | Drop what is in the hands |

**Inventory by weight.** The pack has both slots and weight (see Inventory and backpack): slots limit how many kinds of things fit, and weight limits how fast the player moves. Logs, carcass quarters and big stones are carried in the hands instead of the pack, which blocks tool use until they are dropped (one key, instantly, as when a bear appears).

| Load | Effect |
| --- | --- |
| Under 15 kg | Full speed |
| 15–30 kg | 85% speed, 20% more calories burned |
| 30–45 kg | 65% speed, 50% more calories, no running |
| Over 45 kg | Drag only, at 30% speed |

Carrying capacity grows with gear and with Strength, which scales these bands from 0.8× to 1.15× (Minecraft development › Strength carries more): the crate backpack holds 30 kg, a pack frame 45 kg, a sled 120 kg on snow, a canoe 150 kg on water.

**The messenger's battery.** The messenger lasts about 7 days on a charge and recharges from the small solar panel in the crate in 2 hours of sun. The forecast and vitals use almost nothing; the GPS map uses the most. In the dark of December one sunny day gives only about a day of charge, so in winter the map is used sparingly and landmarks matter more.

**Added keys.** Four actions in the danger and light rules need their own keys. F is the off-hand swap and X reads the land and sky (tap; hold pins the line on the map). V tap is a calm 3 s line, and V hold opens the voice wheel with Shout as its default.

| Keyboard | Touch | Action |
| --- | --- | --- |
| V (tap / hold) | Voice button | Talk calmly / shout |
| B | Spray button | Ready the bear spray, wherever it is carried |
| L | Light button | Headlamp on or off, or raise the torch or lantern |
| Esc | Menu button | Pause menu (the only screen that always pauses) |

**The body on screen.** The eye sits at Minecraft's 1.62 m, with a 75° field of view suggested on first launch. Both hands are always drawn: the right hand holds the tool or weapon, and the left holds the messenger, a torch, a lantern or the spray. Looking down shows legs and boots, which show wet, frost and snow. Head bob is 2 cm at a walk and can be switched off.

**Moving.** Walk is 3.0 m/s, run 5.5 m/s, crouch 1.2 m/s (40%), swim 1.0 m/s. Slopes over 25° cut walking to 70%; over 35° only rock with handholds can be climbed. Jump clears 0.6 m and mantles a 1.2 m ledge in 0.8 s. Footsteps change by surface: moss, gravel, snow crust, river ice, plank floor.

**How each interaction feels.**

| Action | Input | Real time | What the player sees and hears | Fails when |
| --- | --- | --- | --- | --- |
| Pick up | RMB tap | 0.4 s | The hand reaches; the item's weight flashes on the strip | Load would pass 45 kg: "Too heavy to carry; drag it?" |
| Drink at water | RMB tap, crouched at the edge | 3 s per 0.5 L | Cupped hands, gulps, ripples | Water below 4 °C costs warmth when cold; unboiled rolls illness |
| Eat | R | 3 s | A bite, chewing; the calorie line on the strip ticks up | Food frozen solid: "Thaw it first" (10 game minutes by the fire) |
| Inspect | RMB tap on a plant, track or sign | 0.5 s | A close-up card with Look, Smell and Cut checks (see Foraging) | — |
| Chop or fell | LMB per swing | 1.1 s | Overhead swing, chips, a thunk felt as a 40 ms rumble | A swing off the notch counts half; a knot glances off |
| Split | LMB at the chopping block | 1.2 s | The reticle shows the end-grain crack; a clean hit pops the halves apart | Off the crack by more than 3 cm: a stuck blade, 1 s to free |
| Hold-to-work | RMB hold | Ring fills at 5 game minutes per s | A ring at the crosshair, the job's sound loop | Releasing pauses; progress stays on that object for 1 game day |
| Draw the bow | RMB hold, release | 1 s to full draw | String creak; the view narrows 10% at full draw | Past 6 s the arm shakes; below the strength needed the draw stops at 60% |
| Throw a stone | RMB hold, release | 0.5 s | An arc preview for the first 10 throws, then none | — |
| Cast | RMB hold, release | 0.3–2 s | Rod bends to the charge; the bobber flies and plops | Snagged on brush behind: 1 game minute to free |
| Climb a tree | Space hold at a spruce with branches below 2 m | 1 m per s, up to 4 m | Hands on bark, needles brushing the view | Energy under 2, a hand-carried load, or a broken arm |
| Blaze a trunk | RMB hold with the hatchet | 2 s | A pale scar of wood, visible 20 m in a whiteout | — |
| Talk / shout | V tap / V hold | 1 s | The survivor's voice, low or loud | Shouting carries 200 m and spooks all game in it |
| Whistle | G | 1 s | Two-note whistle; the wolf's ears turn | Below Bonded, or beyond 300 m |
| Drop | Q | Instant | The carried thing falls | — |

**Hold-to-work rules.** A work preview always shows first: time, kcal, the station, and anything missing. Holding fills the ring as a time-lapse with a small clock face beside it. Tapping Z while the ring fills switches to "work until done" at 20 game minutes per real second; it stops when the job ends, at any danger alert, at warmth below 40, at energy below 2, or when the light fails. Hold-to-work is blocked while bleeding, except for bandaging.

**Practice.** Each action has a practice level shown as 0–10 dots in the notebook. Each level cuts its time 4%, up to 40%. Some minigames shrink as practice rises, listed with each one below.

**The bow drill, step by step.**

1. **Ready.** With a spindle, fireboard, bow, socket and a tinder bundle, hold E at a cleared spot. The survivor kneels, puts a foot on the board and seats the spindle (2 s). A new board needs a starter pit and a notch first: 10 s of strokes, then a knife cut (3 s).
2. **Stroke.** Move the mouse left and right, or A and D, or drag on touch, to saw the bow. A beat marker pulses at 1.5 strokes a second. A stroke within 0.15 s of the beat adds full heat; off the beat adds a third.
3. **Press.** Hold RMB for downward pressure, shown as a gauge with a green band at 40–70%. Too little gives no heat. Too much stalls the bow and the string slips off (−15 heat, 2 s to reset).
4. **Smoke.** The heat bar fills from 0 to 100 and cools 8 points a second without strokes. Thin smoke starts at 40, thick smoke and dark dust in the notch at 75.
5. **Ember.** At 100 a red glow shows in the dust pile. Stop, lift the spindle (E tap) and tip the ember into the tinder bundle (E tap, 2 s). The ember lasts 60 s; wind over 6 m/s halves that unless the player crouches behind cover.
6. **Blow.** Hold E to blow in puffs of 1–1.5 s. A breath gauge turns red past 1.5 s, and a puff into the red scatters the ember. Three to five good puffs bring a flame, unless the tinder is below 60 dryness.
7. **Lay it.** The burning bundle lasts 2 game minutes (about 20 real seconds on the close clock). Place it under the kindling (E tap) and the fire lay takes over.

A board below 60 dryness caps the heat at 60: plenty of smoke, never an ember. Warmth below 50 shrinks the beat window by a quarter, below 30 by half; mittens make the drill impossible. Energy below 2 ends the attempt. Each try costs 60 kcal whether it works or not. A fireboard holds 8 notches and a spindle lasts 15 fires. By the tenth fire the stroke and press steps merge into one 6-second hold that fails only on wet wood.

**Blowing a flame from coals or a match.** The same breath gauge, without the drill. Banked coals need one puff after fresh tinder is laid on them (5 s in all). A match lights on the strike roll from the fire table, then needs two puffs.

**Knapping flint.**

1. Hold a flint core and a hammerstone (E hold at any flat rock). The core turns in hand with the mouse.
2. Pick a strike point on the edge. Good platforms glint; the reticle turns white over one.
3. Set the angle by dragging: a guide arc shows 60–80° as good.
4. Strike (LMB). On a platform within the good angle, a flake comes off and the shape changes. Off by more than 20°, a crack line appears; a second bad strike on that side shatters the core 50% of the time.
5. Four good flakes make a broadhead blank; two more finish it.

Practice widens the good angle to 50–85° and removes the shatter chance on the first bad strike.

**Twisting cordage.** Hold E with fiber in hand. A tension marker drifts; release and press again in rhythm to keep it centered while the twist count climbs. Each metre takes 2 game minutes. The cordage's strength is the average centering, so practiced cordage holds more and snaps less on snares and lashings.

**Pull and brace.** Rolling a log up skid poles, or pulling one out of a jam, is hold LMB to pull and release when the rope shakes to brace. Holding through the shake slips the log back one step; three slips drop it.

**The fishing fight on the controls.** Hold LMB to reel and release to ease off; on touch, hold the Action button. A walk key toward or away from the fish moves along the bank to follow a big one. The phases and timings are in Food systems.

**Assist options.** Every hold can be set to toggle. The bow drill, knapping and cordage can be set to Assisted (wider windows) or Automatic (a hold with the same success chance). Danger sounds get captions with direction and range, such as "\[Huff, close, left\]."

**The pack.** The hotbar, off-hand slot, pack grid and pack screen are specified in Inventory and backpack. The only rule added here: the world keeps running while the pack is open, except on Gentle, so opening it next to a bear is a real choice.

**The messenger, screen by screen.** M raises the device into the left hand; the right hand keeps one-handed items, so the bow cannot be drawn while it is up. Q and E switch screens.

| Screen | What it shows | Battery |
| --- | --- | --- |
| Vitals | Every need from the HUD table, weight graph, active problems with their one treatment ("Cut, left forearm, bleeding. Bandage now: medkit has 4") | None beyond the idle drain |
| Map | Explored land, breadcrumbs, up to 20 waypoints, a pin for every trap, net and tip-up with time since checked, found patches and fishing spots, the "pack left behind" pin | 4% per game hour open |
| Forecast | Tomorrow (80% accurate), two-day outlook (60%), wind, frost warnings, bug index, daylight hours, days to freeze-up or breakup, aurora chance tonight | 0.5% per look |
| Prompts | This morning's prompts, ranked | None |
| Notebook | Tabs: Recipes, Plants, Sign, Spots, Logbook pages, Practice | None |
| Stores | Each cache: contents, spoil timers, ration setting, days left at that ration, mouse warnings | None |
| Ambitions | Earned and open ambitions | None |
| Journal | Nightly lines, auto-logged firsts, the player's own lines, the base from above by month, year cards | None |
| Radio | Shown once the mast works: the daily log form and the supply-drop tracker; runs on the mast's battery | None |
| SOS | One button: hold 3 real seconds, then confirm | 5% |

**Battery numbers.** The idle drain (strip, clock, breadcrumbs) is 5.5% a game day, so a typical day with 2 hours of map use is about 14%, which is the 7-day charge. The solar panel, unfolded in sun, charges 50% an hour with the sun high, scaled by sun elevation and ×0.25 under cloud; snow on it blocks it. That gives full in 2 hours in summer and about 14% from a clear December day. Below −20 °C a messenger not worn inside the coat drains twice as fast and its screen lags. At 20% it warns once; at 5% the map shuts off; at 0 the strip goes dark.

**Tests.** A first-time tester lights the first bow-drill fire within 3 tries (the M3 test). The work ring never stalls when crossing a chunk; every danger alert drops the ring within one frame; switching any hold to toggle works in every minigame.

### Inventory and backpack

The inventory works like Minecraft's: a 9-slot hotbar, and a backpack grid that adds rows of 9, with real weight on top. Slots decide how many different things fit; weight decides how fast the player moves and how many calories each step burns. That keeps the video's best inventory moments: dropping the fishing rod to carry beaver meat home, and spending a long time choosing what to take on the moose hunt.

**The HUD.** Only four things stay on screen, so the land stays the picture. A fifth, the clothing body figure, appears only when a body zone needs attention (see Clothing). Everything else lives on the messenger or in the pack.

| Element | Where | What it shows |
| --- | --- | --- |
| Hotbar | Bottom centre, 9 slots, as in Minecraft | Item icon, stack count, a condition or freshness bar; the active slot is outlined. Keys 1–9 or the mouse wheel pick a slot. The 9 slots are the belt and pockets, so they work even without a pack |
| Off-hand slot | Left of the hotbar | A torch, lantern, rock, bear spray or the messenger. F swaps it with the active slot |
| Load meter | Right of the hotbar | A pack icon with "22 / 30 kg"; it turns amber at 15 kg and red at 30 kg, matching the load levels above |
| Pickup line | Above the hotbar | "+3 birch bark" for 2 seconds, or "Pack full: 2 left on the ground" |

When the player looks at something usable, a one-line prompt appears near the crosshair with its key and weight: "RMB  Pick up flint (0.4 kg)". Hands-carried loads (a log, a quarter, a carcass) replace the hotbar with a single wide slot until they are dropped.

**The pack screen (E).** It opens over the world in under 100 ms, and the world keeps running at normal speed on Standard and Brutal. On Gentle it pauses. A noise or threat within 30 m flashes the screen edge so the player can close it. It has four panels.

1. **Body:** gear slots for head, torso, legs, hands, feet, back (the pack), three belt slots (knife sheath, quiver, one pouch), two neck slots (the Trail Messenger, binoculars), two inner pockets and the snowshoes, as Curios slots (Minecraft engine › Clothing slots). Warmth and wetness for each slot show on the body diagram.
2. **Pack grid:** rows of 9 slots from whatever is on the back, with the 9-slot hotbar as the bottom row, the total weight and a sort button. Items move between the grid and the hotbar by dragging, or by hovering and pressing 1–9, as in Minecraft.
3. **Crafting:** recipes the player can make from the pack plus any storage within 8 m, from the recipe notebook. Missing inputs are greyed with their names.
4. **Container:** when a chest, barrel, cache or sled is open, its grid sits beside the pack.

**Packs and containers.**

| Holder | Slots | Max weight | How it is made or found |
| --- | --- | --- | --- |
| Hotbar only, no pack | 9 (the hotbar) | 5 kg | The start, before the crate is opened |
| Crate backpack | 18 (2 rows) | 30 kg | Starting kit |
| Hide pack | 27 (3 rows) | 35 kg | Hide tier: 2 cured hides, cordage, a bone toggle |
| Pack frame | 27 (3 rows) plus 3 lash points | 45 kg | Bushcraft: a spruce frame, cordage, a hide strap. Lash points take one long item each: a log, a quarter, a bow |
| Wolf saddlebags | 6 | 10 kg | Hide tier, once the wolf is Bonded |
| Sled | 36 (4 rows) | 120 kg on snow | Hide tier |
| Canoe | 36 (4 rows) | 150 kg on water | Hide tier |
| Chest | 27 (3 rows, like a Minecraft chest) | No limit | Logs or planks, 30 game minutes. Mice and bears can get in |
| Barrel | 18 (2 rows) | No limit | Split staves and hide lid. Rodent-proof, as the video used them to sort meat, building supplies and fishing gear |
| Food cache | 27 (3 rows) | No limit | Raised cache; feeds the player automatically at the chosen ration |
| Woodshed | Armloads only | 40 armloads | Dries firewood twice as fast |

**Stacks.** Items of one kind share a slot up to a stack size set by their bulk. Food stacks only with food of the same freshness, so spoilage stays honest.

| Kind | Per slot | Examples |
| --- | --- | --- |
| Small | 50 | Tinder, feathers, berries, crickets, flint flakes |
| Medium | 20 | Fiber, birch bark sheets, cordage (1 m each), arrowheads, snares |
| Food by weight | 8 kg | Meat cuts, fillets, smoked strips, potatoes |
| Liquids | By container | 1 L bottle, 2 L pot, 4 L birch-bark bucket |
| Tools, weapons, clothing | 1 | Knife, hatchet, bows, rod, coat |
| Arrows | 12 in the quiver | Kept in the quiver slot, not the grid; each arrow is crafted singly, as in the video |

**What each icon tells.** A thin bar under each icon shows condition for tools and freshness for food, from green to amber to red. Hovering, or a long press on touch, gives the details: "Raw moose liver · 1.2 kg · 1,600 kcal · spoils in 0.7 days". Wet items carry a droplet mark, and wet tinder will not catch.

**Moving things.** Drag and drop moves an item; Shift-click sends a stack to the open container; right-click splits a stack in half. The sort button groups the pack by kind, and "Stash" sends every item of the kinds already in a container into it. Auto-pickup can be switched on for arrows, feathers and flint flakes, so recovering a missed arrow is one walk, not a search.

**Dropping and the ground.** Q drops what is in the hands; Ctrl + Q drops the whole selected stack. Dropped items stay where they fall for 7 game days and show on the messenger map as long; keepsakes, notes, the messenger and packs never expire, and gear that sinks becomes a `lost_gear` block on the bed (DIRECTOR RULING 89). Meat left on the ground draws foxes, ravens and bears. Items dropped in deep water sink; light items in moving water drift and catch on snags and log jams, the same rule that piled feathers on the video's felled trees.

**Full pack.** A full pack never blocks a pickup silently. The item stays on the ground with a marker, the pickup line says what did not fit, and one key opens the pack with that item held, ready to swap with something.

**SOS and the pack.** In Endless mode, everything carried except worn clothing and the messenger stays where the player fell, in a `left_pack` block that never expires and is pinned on the map; animals can raid its food by the cache rules (RECONCILE 84; Minecraft development › Collapse, SOS and the flight). In Hardcore the world ends.

**Data and build.** Item numbers live in data maps (`data_maps/item/weight.json`, `props.json`, `pack.json`); inventory state is vanilla's 41 slots plus the Curios slots and the `pack_rules` and `carry` attachments; the screens are `client.pack.PackScreen` over `item.inventory.PackMenu`, owned by Look, sound and UI (Minecraft engine › Inventory and packs). It ships in M2 with carrying and encumbrance.

```json
{
  "id": "birch_bark",
  "name": "Birch bark",
  "weight_kg": 0.1,
  "stack": 20,
  "slot": "pack",
  "tags": ["tinder", "roofing"],
  "wet_takes_h": 4
}
```

Tests: the pack opens in under 100 ms on a mid laptop; a 40-game-hour bot run never loses an item between pack, containers and ground; every action works by mouse and keyboard, and every hold can be set to toggle (touch is dropped).

### Clothing

Clothing is the shelter the player carries. Every garment adds warmth in °C to the feels-like temperature, blocks some wind, soaks or sheds water, and keeps one body zone from freezing. As in the video, each new garment is a milestone of the year: a poncho of small pelts for fall, a moose-hide coat in October, then bear pants and a fur hat before deep winter.

**Zones and layers.** The body has five zones, and each zone has an inner and an outer slot, so ten clothing slots sit on the pack screen's body panel. In Minecraft the four vanilla armor slots carry the outer head, torso, legs and feet layers, and Curios carries the rest (Minecraft engine › Clothing slots). Inner layers trap warmth; outer layers stop wind and water. A zone's share of the whole body sets how much its gear counts: torso 40%, legs 25%, feet 15%, head 10%, hands 10%.

**Warmth.** Clothing warmth is added to feels-like temperature (see Warmth in Survival tuning). Each garment loses warmth as it gets wet or worn:

```latex
W_{clothing} = \sum_{g} w_g \cdot c_g \cdot \left(1 - m_g\,(1 - k_g)\right)
```

Here w is the garment's warmth, c is 1 above 30% condition and 0.5 below it, m is wetness from 0 to 1, and k is the share of warmth it keeps when soaked. Wind chill is multiplied by (1 − wind block), with wind block averaged over the zones by their share.

**Garments.**

| Garment | Zone and layer | Warmth | Wind block | Warmth kept wet | Weight | Made from | Tier |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Synthetic base layer | Torso, inner | +3 °C | 0% | 80% | 0.2 kg | Worn at the start | Start |
| Fleece jacket | Torso, inner | +5 °C | 10% | 70% | 0.5 kg | Worn at the start | Start |
| Rain shell | Torso, outer | +1 °C | 90% | Sheds rain until below 50% condition | 0.4 kg | Worn at the start | Start |
| Work pants | Legs, inner | +2 °C | 20% | 30% | 0.6 kg | Worn at the start | Start |
| Wool socks | Feet, inner | +2 °C | 0% | 80% | 0.1 kg | Worn at the start | Start |
| Hiking boots | Feet, outer | +2 °C | 50% | Waterproof until below 50% condition | 1.4 kg | Worn at the start | Start |
| Wool beanie | Head, inner | +2 °C | 30% | 80% | 0.1 kg | Worn at the start | Start |
| Work gloves | Hands, inner | +1 °C | 20% | 20% | 0.1 kg | Worn at the start | Start |
| Mosquito head net | Head, outer | 0 | 0% | n/a | 0.05 kg | Sewn from the outpost's window screen, or fine fiber mesh | Start |
| Moccasins | Feet, outer | +1 °C | 30% | 30% | 0.4 kg | 1 scraped hide, sinew | Hide |
| Small-pelt poncho | Torso, outer | +5 °C | 40% | 60% | 1.5 kg | 4 scraped hides, cordage | Bushcraft |
| Hide coat | Torso, outer | +12 °C | 90% | 50% | 3.5 kg | 9 cured hides, scraped moose hide, rawhide | Hide |
| Parka | Torso, outer | +15 °C | 95% | 50% | 4 kg | 2 caribou hides or 1 bear hide, sinew | Hide |
| Wolverine ruff | Parka hood | +2 °C | Face safe to −45 °C | Sheds frost | 0.2 kg | 1 wolverine pelt | Hide |
| Hide pants | Legs, outer | +8 °C | 90% | 50% | 2.5 kg | 5 cured hides, rawhide | Hide |
| Fur hat | Head, outer | +4 °C | 80% | 60% | 0.4 kg | 1 beaver or bear pelt | Hide |
| Fur mittens | Hands, outer | +4 °C | 90% | 50% | 0.3 kg | 4 hare pelts, sinew | Hide |
| Hare-fur liners | Feet, inner | +3 °C | 0% | 60% | 0.2 kg | 2 hare pelts | Hide |
| Mukluks | Feet, outer | +6 °C | 90% | 40% | 1.2 kg | Moose hide, fur liner, sinew | Hide |
| Snow goggles | Head, outer | 0 | 0% | n/a | 0.05 kg | Bone or wood with two slits | Bushcraft |

**Safe-to temperatures.** Hands, feet and face can freeze before the core gets cold. Each of those zones has a "safe to" temperature from its gear: bare skin is safe to −20 °C feels-like (−10 °C when wet), work gloves to −22 °C, and fur mittens to −25 °C. Hiking boots with wool socks are safe to −22 °C and mukluks to −35 °C. A fur hat keeps the ears safe to −30 °C, and a ruffed parka keeps the face safe to −45 °C. Below a zone's safe temperature, frostnip and then frostbite build on that zone, as set out in Health.

**Side effects.** Clothing changes more than warmth.

- **Noise:** a rain shell rustles and adds 20% to stalking noise; moccasins and fur cut it by 30%.
- **Scent:** smoked hide clothing hides 20% of the player's scent from animals.
- **Sweat:** running or chopping with more than +20 °C of clothing above −10 °C wets the inner layers by 10% a minute. Opening the coat (one key) stops it, the old rule of not sweating in the cold.
- **Bulk:** a parka and mittens slow tool swings by 10% and bow draw by 15%; mittens come off with one key for fine work, at the risk of cold hands.
- **Glare:** without snow goggles, bright snow in March and April causes snow blindness: the screen washes white for a day.
- **Bugs:** the head net cuts mosquito bites on the face by 80%, but narrows the view slightly.

**Wet and drying.** Rain, falling in water, melting snow on the body and sweat all raise wetness. Clothing dries by the fire in about 1 game hour, on the drying line in about 4, and slowly on the body. Below −10 °C, snow can be beaten out of fur before it melts, which keeps it dry. A soaked inner layer is the most common way a strong player gets hypothermia.

**Wear and repair.** Garments lose condition from use, thorns, falls and animal attacks; a bear or wolf strike tears the torso layer. Below 30% condition, a garment gives half its warmth and leaks wind. Repair needs a bone needle (bushcraft) and sinew or cordage, takes 20 game minutes, and restores 40% condition. Hides that get wet and dry stiff need working with fat once a season, or they crack.

**Dressing.** Each garment takes 10 seconds to put on or take off, 20 seconds with cold hands. Layers can be swapped by the fire so the wet set dries while the dry set is worn.

**The clothing HUD.** Clothing shows in two places.

- **The body figure** is a small outline of the body at the top left of the screen. It appears whenever a zone is below its safe temperature, wet over 30%, or under 30% condition, and stays while the player pins it. Each zone is tinted: blue when it is too cold, with a droplet mark when wet, red when frostnip or frostbite has started, and a crack mark when worn. A small arrow beside it shows whether the body is warming or cooling.
- **The body panel** on the pack screen shows the ten clothing slots on a full-size figure. Hovering a slot shows that garment's warmth, wind block, wetness and condition. A line under the figure gives the totals, for example "Clothing +27 °C · wind block 85% · hands safe to −25 °C · feet safe to −15 °C". The weakest zone is outlined, so the player knows which garment to make next.

The messenger still gives the honest numbers: air, wind and feels-like temperature on its vitals screen. The body figure only shows where the problem is.

**How the wardrobe grows over a year.**

| Season | Typical set | Clothing warmth | Comfortable down to |
| --- | --- | --- | --- |
| May to August | Starting clothes, head net, later moccasins | About +11 °C | About 0 °C |
| September | Add the small-pelt poncho | About +16 °C | About −5 °C |
| October to November | Hide coat, hide pants, fur hat, mittens | About +33 °C | About −25 °C |
| December to February | Parka with ruff, mukluks, liners, pants, hat, mittens | About +45 °C | About −40 °C with a fire nearby |

**Data and build.** Garment numbers are entries in the data map `data_maps/item/wear.json`; worn clothing sits in the vanilla armor and Curios slots, condition is vanilla durability, and wetness is the `yearofice:wetness` component. The warmth formula ships in M4 with the body model, the starting clothes and the body figure. Hide-tier garments ship with the hide tier.

```json
{
  "id": "hide_coat",
  "name": "Hide coat",
  "weight_kg": 3.5,
  "stack": 1,
  "slot": "wear",
  "wear": { "zone": "torso", "layer": "outer", "warmth_c": 12, "wind_block": 0.9, "kept_when_wet": 0.5, "noise": 0, "scent": -0.2 },
  "condition_days": 240
}
```

Tests: at −30 °C with no wind, the October set holds feels-like above the hypothermia line while the player works; a soaked fleece gives exactly 70% of its warmth; the body figure tints the right zone within 1 second of a change.

### Swimming and working in water

Water is a workplace and the most dangerous place in the valley. Nets, minnow traps, weirs, cattails, a salmon grabbed on the falls and feathers caught on a log all mean getting wet. In the video, cold water and low strength were what came closest to drowning him. Swimming follows cold-water physiology: a gasp on entry, about 10 game minutes of useful movement, then the arms give out.

**Depth decides what the player can do.** Depth and current come from the hydrology stage, so every pool and riffle behaves the same on every visit.

| Depth | Movement | Hands and tools | Notes |
| --- | --- | --- | --- |
| Ankle, under 0.3 m | 90% speed | All tools | Boots wet through after 2 game minutes |
| Knee, 0.3–0.6 m | 70% speed | All tools | Minnow traps, cattail roots, grabbing fish in pools |
| Thigh, 0.6–0.8 m | 50% speed, no running | Tools but no bow | Wade-setting nets and weirs. In current, the knock-down chance and wading pole from Danger in detail apply |
| Over 0.8 m, or current over 1.2 m/s | Swimming | Nothing in hand; the held item is dropped | The main river is always a swim |

**Swimming controls.** W swims toward the view at 1.0 m/s. Space rises and Shift sinks, as in vanilla. Ctrl held and tapped in rhythm with the stroke sound gives a 1.4 m/s crawl at twice the energy cost; missing the beat drops back to the slow stroke. Diving holds breath for 30 real seconds, shown as a ring that empties at the crosshair. Under water, the view blurs to 1–3 m, or 0.5 m after rain, and sound goes muffled.

**Cold water, minute by minute.** The close clock runs whenever the player is in water.

1. **Entry (first 3 real seconds).** A gasp. If the head goes under during it, the player swallows water and coughs for 5 seconds at half speed.
2. **Cold shock (first game minute).** Breathing is ragged and stroke speed is halved. Calm comes faster for a player who entered slowly by wading.
3. **Useful time (up to 10 game minutes).** Warmth falls 3 a game minute in 8–12 °C water and 6 a minute below 4 °C. At the end of the useful time (10 game minutes in water under 8 °C, 20 at 8–15 °C, untimed above 15 °C; Waters and fish › Swimming, wading and cold water in Minecraft) the arms fail and the player drowns, which counts as a collapse (SOS in Endless, the end of the world in Hardcore). An injured torso shortens it: Hurt cuts 10 game minutes to 8, Badly hurt to 6, and a 20-minute band to 16 and 12 (RECONCILE 120; the part bands are in Body, injury and sickness).
4. **After getting out.** The soaked rule applies. Below 0 °C, a fire must be reached within 10–15 minutes.

**Water temperature by month.**

| Month | Rivers and creeks | Lake surface |
| --- | --- | --- |
| May | 2–4 °C, snowmelt | 2–4 °C, ice going out |
| June | 6–8 °C | 8–12 °C |
| July–August | 10–14 °C | 14–17 °C |
| September | 6–8 °C | 8–10 °C |
| October | 2–4 °C | 4–6 °C, then ice |

A swim in May is almost as dangerous as one in winter. July and August are the safe months for water work.

**Strength, weight and clothes.** Each condition below halves swimming speed and doubles its energy cost. Several stack.

- A pack heavier than 15 kg. Q held 0.5 s drops it. A dropped pack floats for 30 real seconds, then sinks if it weighs more than 20 kg. Light items drift and catch on snags.
- Soaked winter clothing. A soaked parka and hide pants add about 8 kg of drag. Boots come off from the pack screen's body panel in 2 real seconds and are lost unless tied to the pack.
- Energy below 2, or strength below 40. If energy reaches 0 in water, the player starts to sink and has 5 real seconds to reach footing or grab something that floats.

**Current.** A swimmer drifts downstream at the current's speed. Crossing a 20 m river with a 1.5 m/s current at 1.0 m/s lands the player about 30 m downstream, so a crossing is planned from upstream of the landing spot. Swimming at 45° into the current shortens the drift and costs more energy. Eddies behind boulders and below points have no current and are places to rest. A sweeper or strainer (a fallen tree lying in the current) pins a swimmer who touches it from upstream: the player has 10 real seconds of a pull-and-brace game to get free, or drowns.

**Getting out.** Gravel bars and low banks are easy. A cut bank over 1 m needs a handhold, such as a root or a branch: hold RMB on it for 3 seconds to climb, and it fails with numb hands below 30 warmth. Getting out onto an ice edge uses the self-rescue in Danger in detail, where ice picks halve the time.

**Working in water.** Every water job can be done the wet way or, once the right thing is built, the dry way.

| Job | Wet way | Dry way |
| --- | --- | --- |
| Set a gill net (4 joined segments) | Wade the net out across an eddy in thigh water: 20 game minutes, about 25 warmth in summer water. Or swim the far line across a channel | From a raft or canoe; or a pull line (30 m of cordage on two stakes) that runs the net out from shore like a clothesline |
| Check a gill net | Wade out and lift it: 10 game minutes in the water | Haul it in on the pull line from the bank: 5 game minutes, completely dry |
| Minnow trap | Set and check in knee water | A trap on a line tied to a bank root |
| Fish weir or fence | Built by wading in a creek, knee to thigh deep: 3 game hours of stones and stakes, done in July or August | None; it is built once and then fished from the bank |
| Grab a fish | Wade into a pool or the foot of the falls during the salmon run | A spear or gaff from a rock |
| Recover a dropped tool | Dive in clear water less than 2 m deep; murky or deeper water loses it | A pole with a hook for items under 1 m |
| Collect drift (feathers, logs, a lost pack) | Swim or wade to the snag | A log bridge or the raft |

**Under ice.** There is no surfacing under ice, so going under it is fatal unless the player gets back to the hole within the breath ring. In winter, nets are set through holes chopped in the ice with a long pole, and the player never enters the water.

**Help from the wolf.** A Companion wolf swims at 1.2 m/s. On the Fetch command it brings back floating game such as ducks, and a drifting arrow or mitten. It cannot pull the player out. No player outswims a moose (1.8 m/s), grizzly (1.5) or black bear (1.4) (Minecraft development › Entities and AI, swim speeds); water is an escape only across current over 2.5 m/s, which bears will not enter (Every concept from the video › Bears in water).

**What the player sees.** When the head goes under, the breath ring appears. The clothing body figure turns blue with droplets, zone by zone, as the water reaches each one. The messenger's water alarm counts down the useful time, then sends an SOS. Getting out shows a single line: "Soaked. Fire within 12 minutes." The time is worked out from the air temperature.

**Data and build.** Swim rules are `body.swim.SwimState` with the pure `sim.body.ColdWater`, reading depth from blocks and current from `yearofice:current`; nets and weirs are `food.traps` block entities (Waters and fish › Swimming, wading and cold water in Minecraft, and Passive gear while away). Swimming movement ships in M2, cold water in M4, and water work in M5.

Tests:

- Crossing a 20 m river with a 1.5 m/s current at 1.0 m/s lands 25–35 m downstream.
- In 4 °C water, a player at energy 2 in summer clothes reaches a bank 15 m away, but not in a soaked parka with a 20 kg pack.
- A net on a pull line can be set and checked with zero wetness.
- A dropped knife in 1.5 m of clear water can be recovered; in murky water it is lost.

### Fire, water and light

Fire is the center of every day, so it has states, builds and fuels that the player learns to manage, not a single on/off switch.

**Fire states.** An ember from the bow drill catches in tinder within 60 s or dies. A tinder flame needs kindling within 2 minutes. A small fire boils water in about half an hour and warms the player within 3 m. Fed with logs, it burns down to a bed of coals, the best heat for cooking and smoking. Banking the coals under ash at night keeps them alive for 6 hours, so the morning fire relights without the bow drill.

| Build | Needs | Burns per armload | Best for |
| --- | --- | --- | --- |
| Teepee | Sticks and split wood | 1 h, hot | Boiling water fast, signaling |
| Long fire | Two parallel logs | 3 h, steady | Sleeping beside it outdoors |
| Dakota hole | A dug pit and a draft tunnel | 2 h, low smoke | Wind, rain, saving wood |
| Stove | A built stove | 4 h, 85% of heat stays in the room | The winter cabin |

**Fuel.** Seasoned wood burns at full heat. Green wood gives half the heat and twice the smoke, which is useless indoors but ideal for smoking meat. Wet wood may not catch at all. Birch bark and spruce pitch light even when wet. Heavy rain drops an uncovered fire one state every 10 minutes.

**Carrying fire.** A chunk of chaga or tinder fungus holds a live ember for 4 hours, so a fire can be carried to a new camp instead of drilled again.

**Water.**

| Source | Safe to drink | Note |
| --- | --- | --- |
| River, lake, creek | After boiling 5 game minutes | Unboiled: 30% chance of gut illness |
| Rain caught on a tarp | Yes | Only on rainy days |
| Snow and ice | After melting | Eating snow raw costs warmth (8 per half liter) |
| Hot spring | Yes | A set piece that stays open all winter |

Containers: the steel bottle (1 L) and pot (2 L) from the crate; a birch-bark bucket (4 L) that cannot go on a fire but can be boiled with hot stones; an iron kettle later. Outside below −10 °C a bottle freezes within an hour unless it is carried inside the coat.

**Light.** With 4 hours of daylight in December, light becomes a resource. Crafting in the dark takes twice as long, and aiming is limited to what a light source shows.

| Light | Made from | Lasts | Note |
| --- | --- | --- | --- |
| Campfire | Fuel | While fed | Lights 8 m around it |
| Birch-bark torch | Bark and a stick | 15 min | Carried; wolves keep their distance |
| Fat lamp | Clay dish, lard, moss wick | 6 h per 100 g of lard | Indoor light for winter crafting |
| Tallow candle | Moose or bear fat | 4 h | Cleaner than a fat lamp |
| Headlamp | The crate | 20 h in total | Batteries only come back with supply drops |

**The fire state machine.** `com.yearofice.fire.FireRingBlockEntity` (the block entity of yearofice:fire\_ring, Minecraft engine › Fire) runs one state per fire, steps each game minute and posts `fire.state` on every change. Heat is the bonus within 3 blocks, fading to 0 at 8. For testing, /yoi fire state \<state> and /yoi fire fuel \<armloads> set a fire; burn rates and settings are tuned in the \[fire\] section of yearofice-common.toml.

| State | Comes from | Heat | Light radius | Lasts | Next |
| --- | --- | --- | --- | --- | --- |
| Ember | Bow drill, a carried ember, a spark into char cloth | 0 | 0 | 60 s | Tip into tinder and blow |
| Tinder flame | A blown ember or a match | +2 °C | 2 m | 2 game minutes | Lay it under kindling |
| Small fire | Kindling caught | +10 °C | 5 m | 20 game minutes on its kindling | Add one armload |
| Fire | Fed split wood or logs | By setting | By setting | The build's time × wood × setting | Feed it, or let it burn down |
| Coals | A fire that has burned at least one armload, left unfed | +15 °C | 3 m | 1 game hour | Cook, smoke, bank or feed |
| Banked | Coals covered with ash (hold right mouse, 3 s) | 0 | 0 | 6 game hours | Fresh tinder and one puff: Small fire in 5 s |
| Out | Any timer running out, rain, snow or water | 0 | 0 | — | Start over |

**Settings.** The build table's times are at Steady. The setting is chosen on the fire's card (right mouse tap on the fire), and the HUD reads it as "Fire, Low".

| Setting | Heat within 3 m | Light radius | Fuel use | Note |
| --- | --- | --- | --- | --- |
| Low | +10 °C | 5 m | ×0.4 (lasts 2.5 times as long) | Snow puts it out |
| Steady | +18 °C | 8 m | ×1 | The default |
| Hot | +25 °C | 12 m | ×3 | Boils fastest; sparks start the spread rule when packed |

**Wood.** An armload is about 15 kg, carried in both hands. Each wood multiplies the build's burn time and heat.

| Wood | Lights | Burn time | Heat | Smoke | Best for |
| --- | --- | --- | --- | --- | --- |
| Dead standing spruce | From a small fire | ×0.7 | ×1.0 | Low | Day-one fuel |
| Seasoned spruce | From a small fire | ×0.7 | ×1.0 | Low | Quick heat |
| Seasoned birch | Needs a Fire or coals | ×1.4 | ×1.1 | Low | The night fire and the stove |
| Driftwood | From a small fire | ×0.6 | ×0.9 | Low | Gravel-bar camps |
| Willow, alder | From a Fire | ×0.8 | ×0.9 | Sweet | Smoking meat |
| Green, any kind | Only on a Hot fire | ×1.0 | ×0.5 | ×2 | Smoke, smudges; never indoors |
| Wet surface | Dries 10 game minutes on a Fire first, giving no heat | — | 0 while drying | ×2 | Nothing |

A birch armload on Low in a stove lasts 4 × 1.4 × 2.5 = 14 game hours, which is how one load holds a cabin through a −40 °C night.

**Seasoning.** Every piece of wood carries a moisture, the component `yearofice:moisture` (0–100%): green wood is 50%, dead standing spruce 18%, and seasoned means under 20%. Split wood loses 1.5 points a summer day on a covered rack and 3.0 in a woodshed, slower in fall and winter, and unsplit logs dry about half as fast (Every concept from the video › Firewood drying). So green wood split into a woodshed is seasoned in 10 summer days or 25 fall days, and wood split after October 15 dries only by the stove. That makes "cut next winter's wood this summer" a rule the numbers enforce; the chain from felling to the woodshed is in Crafting and building in depth › Trees, felling and firewood.

**Rain and snow.** Heavy rain drops an uncovered fire one state every 10 game minutes; light rain one every 30. A lean-to, roof or tarp over it cancels that. Snowfall puts out a Low fire in 20 game minutes and slows the others by a quarter.

**Smoke indoors.** A fire in a room without a chimney raises the room's smoke 10% a game minute. Above 50% the player coughs, sight drops to 5 m and Condition falls 1 a game hour; above 80% it falls 5 an hour and wakes a sleeper. An open door clears it in 5 game minutes. Creosote runs 0–100 per chimney: each armload adds 1 for seasoned birch, 2 for seasoned spruce and 5 for green wood, and each hour on Low adds 3. The messenger warns at 60, and above 70 each hour on Hot has a 3% chance of a chimney fire (Crafting and building in depth › Warm rooms). Sweeping with a spruce-bough brush (30 game minutes) resets it.

**Spread.** An open fire holding more than 3 armloads, or Hot with more than 2, is packed too full. It warns once ("Packed too full: it may spread"). After that, each game hour the player is more than 20 m away, a spark has a 10% chance to land on any burnable piece within 2 m: a woodpile, a lean-to, a bough bed. With no stone ring, after 3 dry days in summer, duff within 2 m also catches at 5% an hour. A duff fire smolders for 1 game hour, then grows; with wind over 5 m/s it becomes a forest fire, the open world's wildfire event. One poured pot of water kills a burning piece; kicking dirt (hold right mouse with an empty hand, 5 s) kills a duff fire.

**Fire as a signal.** A night fire is visible from 2 km, so a camp can be found in the dark. A teepee loaded with green boughs sends a smoke column visible from 8 km for 30 game minutes. Lit before a supply drop, it lands the drop within 50 m instead of 300 m.

**Fire and animals.** Wolves and bears keep 15 m from a Steady or Hot fire at night, and any chase ends at one. Moose ignore fire. A carried torch keeps wolves 10 m off.

**Carrying fire.** A chunk of chaga holds an ember 4 hours on its own. An ember carrier (a birch-bark tube with dry moss around a chaga coal, 15 game minutes to make) holds it 6 hours, the figure in the campfire table. Either relights a fire at 90%.

**Boiling.**

| Heat | 1 L to a boil | Note |
| --- | --- | --- |
| Small fire | 30 game minutes |  |
| Steady fire | 20 |  |
| Hot fire or teepee | 12 |  |
| Coals | 20 | Easiest to cook on |
| Stove | 15 |  |

Water is safe after 5 more minutes at a rolling boil. A full 2 L pot takes 1.6 times as long. A pot needs a cooking stand or a flat stone at the fire's edge; set straight in the fire it tips 10% of the time. Boiled water stays hot (+5 warmth a cup) for 30 game minutes.

**Snow and ice for water.** A pot packed with snow gives 0.5 L; a pot of chipped lake ice gives 1.6 L, so chopping ice is three times quicker. A dry pot of snow scorches unless a little water is added first, and scorched water tastes of smoke (−1 Spirits). Melting a pot takes 15 game minutes on a Steady fire.

**Hot stones.** Five fist-sized stones heat in 20 game minutes and are moved with two-stick tongs (hold right mouse) into the birch-bark bucket; 4 L boils in 10 game minutes. A stone still wet from the river has a 10% chance to crack and spit an ember (a minor burn at 1 m).

**Water quality.** The 30% illness chance for unboiled water is the base for moving and open water. Two places double it to 60%: beaver ponds, and anywhere within 100 m downstream of a latrine, a cleaning spot or a carcass. Rain, melted snow, birch sap and the hot spring are always safe. Illness from water shows 1–3 days later, so the player has to remember where they drank.

**Carrying water.** The steel bottle (1 L) can sit in coals to boil or thaw; the pot holds 2 L; the birch-bark bucket 4 L; the iron kettle 4 L. A 40 L barrel at the cabin freezes if the room drops below 0 °C overnight. A bottle carried inside the coat never freezes while warmth is 30 or more; a frozen bottle thaws in 20 game minutes by a fire.

**Light levels.** A place is lit if a light source covers it or the sun is above the horizon. Outside that, sight depends on the night.

| Night | Sight range |
| --- | --- |
| Civil twilight (sun 0–6° below) | 60% of day sight |
| Full moon on snow | 40 m |
| Half moon on snow, or full moon without snow | 20 m |
| Aurora active | +10 m |
| Moonless, or overcast | 8 m; 5 m in forest |

Crafting outside a lit area takes twice as long, as the rule above says, and an animal beyond sight range shows only as a silhouette against snow or sky that cannot be aimed at. Stepping out of a fire's light leaves the eyes unadapted: sight is halved for 10 real seconds.

**Light sources in detail.**

| Light | Radius | Burns | Note |
| --- | --- | --- | --- |
| Headlamp | A 25 m beam at 30°, or an 8 m wide mode | 20 h in total | Below −30 °C output halves and the battery drains twice as fast; 10 game minutes inside the coat restores it |
| Birch-bark torch | 6 m | 15 game minutes | Dropped on dry grass in summer, it can start a fire |
| Fat lamp | 3 m | 6 h per 100 g of lard | Smoky: lowers the cabin's Light rating by one step |
| Tallow candle | 2 m | 4 h | Clean |
| Burbot-oil lamp | 3 m | 8 h per 100 g of oil | The fat lamp's dish, with oil rendered from burbot liver |
| Lantern (glazed, or pierced tin) | Light 13, about 9 blocks; pierced tin 11, about 7 | 8 h per 100 g of lard or burbot oil | Wind-proof; carried in the off hand (F swaps). One in the gold-rush mining camp, then the Iron tier: the glazed lantern uses the wreck's 6 plexiglass offcuts, the pierced tin lantern 4 tin cans (Crafting and building in depth › Tier 4) |
| Messenger screen | 1 m | 2% battery an hour | The last light when everything else is out |

**Tests.** A packed-too-full fire left alone for 6 game hours next to a woodpile sets it alight in 4 to 6 runs of 10; heavy rain takes an uncovered Steady fire to Out within 30 game minutes; one birch armload on Low in a stove lasts 13–15 game hours.

### Health, medicine and nutrition

Health is a Condition score from 0 to 100 that injuries and illness pull down and rest pulls back up; at 0 the player collapses and the SOS rules apply. Each problem has a named cause, a visible effect and one clear treatment, so a setback teaches rather than just punishes.

| Problem | Cause | Effect | Treatment | Recovery |
| --- | --- | --- | --- | --- |
| Cut | Knife slips, falls, claws | Bleeds 1 Condition a minute | Bandage, or a boiled-cloth or moss dressing | 3 days |
| Infection | A cut left 12 h or a dirty dressing | Fever, blackouts, −10 Condition a day | Clean and redress; spruce-pitch salve; antibiotics | 5 days |
| Burn | Fire, boiling water | Work 20% slower | Cool water within 10 minutes, then salve | 4 days |
| Sprain | Falls, overloaded on slopes | Half speed | Rest and a wrap | 2–5 days |
| Broken bone | Big falls, a moose stomp, a bear | No running or no use of one arm | Splint from sticks and cordage | 30 days |
| Frostbite | Exposed or wet skin below −20 °C | Clumsy hands or slow feet | Slow rewarming; severe cases cost a toe or finger for good | 7 days |
| Gut illness | Unboiled water, Turning or Spoiled food, raw Aging meat, mouse droppings (Decay and time › Eating decayed food) | Half the calories absorbed, dehydration | Boiled water, rest, fluids | 2 days |
| Parasites | Raw bear meat | 15% fewer calories absorbed | Medicine from a supply drop; wormwood tea clears trichinella in 7 days (Plants and fungi). Raw fish and raw game other than bear carry tapeworm instead | Until treated |
| Scurvy | 30 days without vitamin C | Slow healing, fatigue, then bleeding gums | Spruce tips, rose hips, berries, raw fish or liver | 7 days after the fix |

The crate medkit holds 6 bandages, 3 antibiotic courses and 10 painkillers; after that, medicine comes from the land:

| Remedy | Source | Use |
| --- | --- | --- |
| Spruce-pitch salve | Spruce resin and fat | Antiseptic for cuts and burns |
| Willow-bark tea | Willow | Lowers pain and fever |
| Yarrow | Meadows, summer | Stops bleeding faster |
| Usnea lichen | Old spruce | Antibacterial dressing |
| Chaga tea | Birch fungus | Lifts spirits |

**Nutrition beyond calories.** Besides carbs, fat and protein, the body tracks vitamin C. Winters on meat and dried fish alone lead to scurvy unless the player stored rose hips, lingonberries or spruce needles, or eats raw fish and liver. Eating the same food five days running lowers spirits (see below), which rewards a varied larder.

**Condition in numbers.** Condition starts at 100 and returns on its own at the rates below. Healing stops completely only while a wound bleeds, an infection is active or warmth is under 30; a dressed cut or a splinted break heals as Condition does, and each body part heals by the same fraction (Body, injury and sickness › Part health and Condition).

| State | Condition back per game hour |
| --- | --- |
| Asleep in a bed, fed (24 h balance above −1,000 kcal) and warm (warmth 70 or more) | +1.0 |
| Awake and resting | +0.3 |
| Working | +0.1 |
| Underweight, or scurvy | Each halves the above |

| Condition | Band | Effects |
| --- | --- | --- |
| 60–100 | Fine | None |
| 30–60 | Hurt | Work 10% slower; sprints end after 10 real seconds |
| 10–30 | Badly hurt | No running, work 30% slower, aim sway +20%, heartbeat audio |
| 0 | Collapse | SOS rules |

**Stages of each problem.** The table above names cause and treatment; this one sets how each gets worse. Stage 3 is the "untreated severe condition" that ends a Hardcore run.

| Problem | Stage 1 | Stage 2 | Stage 3 (severe) |
| --- | --- | --- | --- |
| Cut | Minor (knife slip, branch): bleeds 1 Condition a game minute | Deep (claws, a hatchet glance, a fall on rock): 3 a game minute | — |
| Infection | Hours 12–36: red and hot; −10 Condition a day | Days 2–4: fever, 2% blackout chance a game hour (30 game minutes down) | Day 5 on: blood poisoning, −20 a day |
| Burn | Minor (a hot pot, a stone): work 20% slower, 2 days | Bad (2 s or more in fire, a boiling spill): also an open wound, 4 days | — |
| Sprain | Mild: half speed, 2 days | Bad: half speed, 5 days | — |
| Broken bone | Splinted: 30 days | Unsplinted: −5 Condition a day | Unsplinted 3 days: sets crooked |
| Frostbite | Frostnip: white and numb, gone if rewarmed within 1 game hour | Superficial: blisters; clumsy hands or slow feet, 7 days | Deep: black skin, 14 days and a finger or toe lost; severe if not rewarmed in 6 h |
| Scurvy | Days 1–10 at an empty reserve: fatigue, energy cap 8, wounds heal at half speed | Days 11–30 ("Weakness"): bleeding gums, energy cap 6, healed cuts reopen at 5% a day, calorie burn +10% | Day 31 on ("Slowness"): joint pain, 70% walk speed, calorie burn +20% |

**Cuts and dressings.** Bandaging takes 3 real seconds and is the one hold-to-work job allowed while bleeding. Every dressing is clean for a time, after which it is dirty and rolls 15% infection every 6 game hours until changed.

| Dressing | Stops bleeding | Clean for | Note |
| --- | --- | --- | --- |
| Medkit bandage | At once | 24 h | Six in the crate |
| Boiled cloth (shirt strips boiled 5 game minutes, dried) | In 2 game minutes | 24 h | Unlimited, but costs a shirt's warmth after 6 strips |
| Dry moss, dried a day first (feather or haircap). Dried sphagnum is its own pad: clean 24 h, and it halves bleeding while it works (Plants and fungi) | In 5 game minutes | 12 h | Must be dried a day first |
| Usnea dressing | In 5 game minutes | 48 h | Halves the infection roll |
| Yarrow added to any dressing | At once | — | Fresh in summer, dried all year |

A cut never dressed for 12 game hours becomes infected, as the table says. A cut heals in 3 days of being kept clean, and the vitals screen shows a timer for the next change.

**Infection.** Cleaning and redressing (boiled water and a new dressing, 5 game minutes) stops stage 1 from advancing. Spruce-pitch salve on a cleaned stage 1 heals it in 3 days. An antibiotic course cures any stage in 5 days and stops it worsening from the first dose; the medkit holds 3 courses, after which they come only from supply drops. Willow-bark tea removes the blackout chance for 8 game hours.

**Burns.** Cool water within 10 game minutes halves recovery (pour from a bottle, or E hold at water). Salve on a bad burn prevents infection and takes a day off healing.

**Sprains.** Wrapping (a bandage, cloth strip or rawhide, 1 game minute) is the treatment; without it the timer does not run. A day walking under 1 km counts double toward recovery; a day carrying over 30 kg counts zero.

**Broken bones.** A leg break stops running, cuts walking to 60% and blocks climbing. An arm break blocks every two-handed tool: the bow, the axe, the bow drill. A splint takes 2 straight sticks, 2 m of cordage, a pad of moss and 10 game minutes. From day 11 the limits ease: 80% walking, or light one-hand work with the bad arm (knife, rod) but still no axe or bow. A bone left unsplinted 3 days sets crooked: 60 days to heal and 5% slower use of that limb for good.

**Frostbite.** A zone colder than its "safe to" limit (from Clothing) gets frostnip after a number of game minutes set by how far below the limit it is:

```latex
t_{nip} = \frac{300}{T_{limit} - T_{feel}} \ \text{game minutes}
```

So 10 °C past the limit gives 30 minutes, 30 °C gives 10. Each further period of the same length advances one stage. Treatment is slow rewarming: a warm room, or hands tucked in the armpits or against the wolf (E hold, 10 game minutes). Holding a frostbitten part within 1 m of a fire burns the numb skin. A part thawed and then frozen again jumps one stage, so it is better not to thaw feet in the field without a fire to keep them warm. Each finger lost makes crafting 5% slower and each toe makes walking 3% slower, counting at most two of each.

**Gut illness.** Unboiled or dirty water shows in 1–3 days, spoiled meat or mouse droppings in 1–2. For 2 days it halves absorbed calories, drains 1 extra litre of water a day, caps energy at 7 and wakes the player twice a night. Letting water fall below 30 during it adds a day. Drinking bad water again restarts the clock. Rest and remedies shorten it (a night in bed −12 h; Plants and fungi › Remedies and doses), but never below 12 game hours. A meal from a stack flagged with mouse droppings rolls 20% (Items › Item decay).

**Parasites.** They show 3–5 days after a raw meal. Raw fish and raw game other than bear carry a tapeworm (15% fewer calories absorbed). Raw bear meat carries trichinella, which also costs 10 Condition and keeps Condition from rising above 90 until treated. The supply-drop medicine is a single dose that clears both; wormwood tea, a cup a day for 7 days, clears trichinella and its cap of 90 but never tapeworm (Plants and fungi). **Botulism** comes only from sour fish packed airtight or a bulged can: onset 12–36 game hours, nausea +60, energy cap 3, −10 Condition a day for 5 days, and no medicine; rest in a bed halves each day's loss, and a 10-minute boil before eating destroys the toxin (Decay and time › Sour fish and botulism; RECONCILE 77). Fish frozen 7 days below −20 °C carries no parasites and keeps its vitamin C, so frozen raw fish is the safe winter source of vitamin C. Freezing never makes bear meat safe.

**Vitamin C.** The body holds a reserve of 0–30 days. It falls 1 a day and fills from food by its C-days (Cooking › Vitamin C in food; Items › Food values lists every food). Each method keeps the share in Cooking's method table: raw 100%, Rare half, coal bake and stew 50%, stone fry 25%, roast 0%. Potatoes keep all of theirs in any method, because the skin holds it, and sauerkraut is eaten raw from the barrel.

| Food | Reserve added |
| --- | --- |
| Rose hips, a 150 g handful | +3 days (1 per 50 g); dried hips and rose-hip meal keep half |
| Fresh berries, a 150 g cup | By species, +0.5 to +2.5 (Plants and fungi); 1 per 200 g where no species value is set |
| Spruce-tip tea, one cup | +1 from fresh May–June tips; +0.5 from old needles |
| Raw liver, 0.25 kg | +1 (Rare: half) |
| Raw or frozen raw fish, 0.25 kg | +0.5 |
| Potatoes, 1 kg, cooked any way | +2 |
| Sauerkraut, a 170 g serving | +1 |
| Dried berries | Half their fresh value |

When the reserve reaches 0, scurvy stage 1 starts. Recovery is the table's 7 days after the reserve is above 0 again.

**Two problems the table did not list.**

- **Blisters.** Walking more than 2 km in a day with wet feet gives a 50% chance of a blister (85% walk speed), as do the first 5 km in new mukluks. A bandage heals it in a day. Left 2 days, it becomes a minor cut with the infection rules. Dry socks prevent it (see Clothing for socks and drying).
- **Snow blindness.** From March to mid-May, more than 3 game hours outside on snow under clear sun, with the sun above 15°, brings it on that night. For 24–48 game hours sight is blurred and too bright: 30 m range, no aiming beyond 10 m. Resting indoors halves the time. Slit goggles carved from wood or antler (knife, 30 game minutes, bushcraft tier) prevent it. **Cold cough** (Plants and fungi › Remedies and doses): 2 game hours of hard work in air below −30 °C bring a dry cough, −1 Spirits a day for 3 days; coltsfoot or Iceland moss tea cancels that day's loss (RECONCILE 66). No fungus can be taste-tested; the test refuses every mushroom and bracket (Plants and fungi › The tasting test). A pressed herbarium sheet is a fourth way to identify a plant, beside the guide, the logbook pages and seeing three growth stages (Plants and fungi › Dyes, fibres and the herbarium).

**Remedies, made and used.**

| Remedy | Made from | Prep | Dose and effect | Limit |
| --- | --- | --- | --- | --- |
| Spruce-pitch salve | 50 g pitch, 50 g lard | 20 game minutes at a fire | One use: antiseptic on a cut or burn | A tin holds 5 uses |
| Willow-bark tea | Inner bark of willow | 10 game minutes boiled | Fever and blackouts gone for 8 h; pain penalties halved for 4 h | Bitter: −1 Spirits |
| Yarrow | Leaves and flowers | None; dry for winter | Stops bleeding at once on a dressing; halves bugs for 2 h on skin | — |
| Usnea | Lichen from old spruce | Dried 1 day | A 48 h clean dressing | — |
| Chaga tea | 20 g chaga | 10 game minutes simmered | +2 Spirits | Once a day counts |
| Labrador tea | Leaves | 5 game minutes steeped | +1 Spirits, +8 water | Over 2 cups a day: nausea, −5 Condition |
| Painkillers | Medkit (10) | — | Removes the speed penalties of sprains, burns and breaks for 6 h; heals nothing | Over 3 a day: stomach upset, energy cap 7 for a day |

**Food data for the macros.** The macro meters in Survival tuning read each food's grams of protein and fat from `data_maps/item/food.json`; carbs are the remaining kcal ÷ 4. The rows below are a summary per serving or per kg. Items › Food values gives every food per 100 g, with its vitamin C, freshness curve, station and cook time.

| Food | kcal | Carbs / fat / protein, share of kcal | Vitamin C |
| --- | --- | --- | --- |
| Pilot-bread cracker, 25 g | 100 | 70 / 20 / 10 | 0 |
| Hare, whole (0.87 kg meat, 0.05 kg organs) | 1,070 | 0 / 6 / 94 | Raw liver only |
| Spruce grouse or ptarmigan | 640 / 440 | 0 / 10 / 90 | 0 |
| Beaver (6.5 kg meat, 0.2 kg tail fat, 0.3 kg organs; RECONCILE 40, 132) | About 9,300; the 0.8 kg of tail meat adds 920 | 0 / 24 / 76 | Raw liver only |
| Moose, caribou, sheep and bear meat, per kg | 1,150 | 0 / 13 / 87; bear in fall 0 / 39 / 61 | 0 |
| Raw fat (`animal_fat`), per kg | 7,000 | 0 / 100 / 0 | 0 |
| Chum salmon, per kg whole | 1,700 | 0 / 40 / 60 | Raw: 1 C-day per 0.5 kg |
| Pike, per kg whole | 1,500 | 0 / 10 / 90 | Raw: 1 C-day per 0.5 kg |
| Lard, per 100 g | 900 | 0 / 100 / 0 | 0 |
| Pemmican, per kg | 5,600 | 3 / 75 / 22 | 0, or 0.1 C-day per 100 g with dried berries |
| Potato, per kg | 770 | 90 / 1 / 9 | 2 C-days, kept in any method |
| Barley, per kg dry | 3,500 | 80 / 5 / 15 | 0 |
| Berries, a 150 g cup | 85 | 90 / 5 / 5 | By species, 0.5–2.5 C-days (Plants and fungi) |

**Variety.** The five-day rule counts a day's "main food": the item that gave more than half its calories. The same main food five days running costs Spirits each further day. A side of something else (chives, berries, tea, salt) breaks the streak, so seasoning matters as much as a new animal.

**Tests.** A cut left undressed becomes infected at 12 game hours on every run; an all-dried-fish winter with no rose hips or raw fish starts scurvy on day 31 ±1 after the reserve was last full; a splinted leg allows running again on day 31; frostbite at 10 °C past a hand's limit reaches frostnip at 30 game minutes.

### Foraging and plant knowledge

Plants are the main source of carbs and vitamin C, and some of them can kill. Knowing which is which is a skill the player builds, so the forest gets richer the better they read it. Every species, verb and number in Minecraft terms is in Plants and fungi; the year's plant work is its Foraging calendar.

| Plant | Season | Gives | Note |
| --- | --- | --- | --- |
| Birch sap | Mid-April to early May | Sugar water, a little carbs | Tapped at breakup; the year's first sweet thing |
| Spruce tips | From bud burst, about May 25, to about June 20 (60–330 degree-days) | Vitamin C, tea | Before bud burst the card reads "Tips not out yet"; older needles still work for tea all year |
| Fireweed | Shoots May–June | Greens; fiber for cordage later | Floods burned ground the summer after a fire |
| Cattail roots | Wherever water is open | Starch, real carbs | One of the few big carb sources |
| Wild chives | June | Flavor, small vitamin C | Has a deadly lookalike (below) |
| Morels | Spring after a fire | Food, high value | Has a lookalike (below) |
| Cloudberries | July, in bogs | Vitamin C, carbs | Short season |
| Blueberries | July–August | Carbs, vitamin C | Bears share the patches |
| Lingonberries | August–October | Carbs, vitamin C | Keep on the bush under snow into winter |
| Rose hips | August–March | Vitamin C | The main winter scurvy defense |
| Highbush cranberry | September–winter | Vitamin C | Sour; better cooked |
| Chaga | All year, on birch | Tea, carries an ember | Slow to regrow; take a little |
| Labrador tea | All year | Tea | Mildly toxic in large amounts |

**Dangerous lookalikes.**

| Dangerous plant | Mistaken for | Effect if eaten |
| --- | --- | --- |
| Water hemlock | Wild celery, cow parsnip by water | Seizures; can kill within hours |
| Death camas | Wild chives | Severe illness, possible death |
| False morel | Morels | Serious illness |
| Baneberry | Highbush cranberry | Vomiting, dizziness |
| Fly agaric | Any large mushroom | Vomiting, hallucinations |

**Identifying plants.** A plant shows as "unknown" in the notebook until it is identified. The crate's pocket guide covers about half the local plants; the rest are learned from the researcher's logbook pages, by seeing a plant in three growth stages (shoot, flower, fruit), from a mounted herbarium sheet (Plants and fungi › The herbarium), or by a taste test: a small bite, then an hour's wait. No fungus can be taste-tested (Plants and fungi › The tasting test). A taste test on a toxic plant costs Condition and a sick day but never kills; eating an unknown plant as a meal can. Identified plants can never be mistaken again.

**Patches remember.** Each patch yields ±20% from its seed value and regrows each season. Stripping a patch bare cuts next year's yield by half.

**Plant data.** Each plant is a file at `data/yearofice/plant/<id>.json`; the `com.yearofice.forage` package owns patches, ripeness, identification and poisoning (Plants and fungi › Data and build). These are the default yields, sized to the 50–300 kcal an hour that foraging gives in Food systems.

| Plant | Unit | kcal | Vitamin C | Pick time per unit | Keeps fresh | Preserved as |
| --- | --- | --- | --- | --- | --- | --- |
| Birch sap | 1 L | 40 | 0 | One tap gives 4 L a day | 2 days; safe to drink unboiled | Syrup at 100:1 by boiling (40 L make 0.4 L) |
| Spruce tips | Handful | 20 | +1 as tea | 2 game minutes | 3 days | Dried for tea |
| Fireweed shoots | Handful | 30 | +0.5 | 3 game minutes | 2 days | — |
| Cattail roots | 250 g | 250 | 0 | 15 game minutes, wading | 5 days | Dried flour |
| Wild chives | Handful | 10 | +0.3 | 2 game minutes | 4 days | Dried; breaks a food streak |
| Morels | 100 g | 30 | 0 | 5 game minutes | 3 days | Dried; +3 Spirits a meal |
| Cloudberries | Cup | 85 | +2 | 30 game minutes | 3 days | Frozen or dried |
| Blueberries | Cup | 85 | +1 | 30 game minutes | 4 days | Frozen or dried |
| Lingonberries | Cup | 85 | +1 | 25 game minutes; 50 under snow | 30 days | Keep on the bush into winter |
| Rose hips | Handful | 50 | +3 | 10 game minutes | On the bush to March | Dried for tea |
| Highbush cranberry | Cup | 70 | +1 | 20 game minutes | 30 days | Better cooked; raw costs −1 Spirits |
| Chaga | 200 g | 0 | 0 | 10 game minutes with the hatchet | 1 year dry | Tea, ember carrier |
| Labrador tea | Handful of leaves | 0 | 0 | 2 game minutes | 1 year dry | Tea |

Berries go into a container (a birch-bark basket holds 16 cups). R (quick eat) while picking eats from the hand. Rose hips eaten whole cost −1 Spirits for the itchy seeds unless split first.

**Ripeness.** Each plant has four visible stages: shoot, flower, fruit and past. The windows in the plant table shift by up to 10 days each year from the summer's degree-days, so a cold year's berries come late. The plant guide shows each identified plant's expected window and the patch's current stage when inspected. When a mapped patch ripens, the Trail Messenger's `berries_known` prompt says so ("Blueberries ripe on the south bench you mapped"; The open world › Sample prompts), and `spruce_tips` does the same for the first tips.

**Patches.** A patch holds a number of units set by its seed value (±20%). The first time a patch is picked it is pinned on the map. Picking more than 70% of it in one season counts as stripped and halves next year's yield. The patch card shows "60% picked." In August, berry patches are on the black and grizzly bears' feeding list, so a patch with fresh scat or torn bushes is one to leave for a day.

**Inspecting a plant.** A right-mouse tap opens a close-up card with three checks, each taking 1–2 real seconds. Look shows the leaves, stem and flower or fruit up close. Smell names the scent. Cut shows the inside of a stem, root or cap. Each lookalike pair differs on at least two checks, and the art must show the difference.

| Pair | Look | Smell | Cut |
| --- | --- | --- | --- |
| Wild chives / death camas | Chives: purple flowers, round hollow leaves. Camas: cream flower clusters, flat V-shaped leaves | Chives: onion. Camas: none | Chives: hollow leaf. Camas: solid leaf |
| Wild celery or cow parsnip / water hemlock | Parsnip: huge hairy leaves, hairy stem. Hemlock: smooth stem with purple streaks, finely cut leaves | Hemlock: raw parsnip | Hemlock: root shows hollow chambers and yellow oily sap |
| Morel / false morel | Morel: a cap of pits like honeycomb. False: wrinkled like a brain, reddish brown | — | Morel: hollow from tip to stem. False: stuffed with cottony fibers |
| Highbush cranberry / baneberry | Cranberry: a shrub, flat clusters of red berries, three-lobed leaves. Baneberry: one plant, a tight cluster on a single stalk, white or red berries with a black dot | Cranberry: musty | — |
| Fly agaric / any large mushroom | Red or orange cap with white spots; white gills and a ring on the stem | — | White flesh |

**Knowing a plant.** Each plant's notebook entry is Unknown, Partly known or Identified.

- **The pocket guide** identifies 12 plants on sight: spruce, birch, willow, fireweed, blueberry, lingonberry, rose, cattail, nettle, yarrow, Labrador tea and chaga. It does not cover wild chives, morels, cloudberries, highbush cranberry, usnea, wild celery, cow parsnip or any of the five dangerous plants.
- **Logbook pages** each teach one or two plants. The researcher's pages are the only page source for the five dangerous plants, which is why reading them early pays. A mounted herbarium sheet identifies its plant too, the fourth way (Plants and fungi › The herbarium).
- **Three growth stages.** Inspecting a plant at shoot, flower and fruit identifies it. The entry is Partly known after one or two, and shows the stages still needed.
- **Taste test.** On an unknown plant that is not a fungus, the card offers "Taste a small bite." A one-hour timer runs on the messenger while the player does other things. A safe plant is identified. A toxic one costs Condition and a sick day (energy cap 5 for 24 game hours) but never kills: −10 for most, −25 for water hemlock and death camas.

An identified plant shows its name at 10 m under the crosshair; an unknown one shows "plant." Once the safe twin is identified, its lookalike shows "not wild chives," so knowing one plant protects against the other.

**Poisoning.** Eating a plant as a meal applies its full effect. The messenger names the plant only if it was identified; otherwise it shows "Something you ate."

| Plant | Shows after | What happens | What helps |
| --- | --- | --- | --- |
| Water hemlock | 15–60 game minutes | Seizures: the player falls and the view shakes every few minutes; −10 Condition every 10 game minutes for 2 hours | Charcoal crushed in water within 30 game minutes halves the loss. Can kill |
| Death camas | 1–8 game hours | Vomiting and a slow heart: energy cap 2, −40 Condition over 12 hours | Rest and water. Can kill a player already below 40 Condition |
| False morel | 6–12 game hours | −30 Condition and gut illness for 3 days | Rest and water. Cooking does not make it safe |
| Baneberry | 1 game hour | Vomiting and dizziness: −15 Condition and 12 hours of view sway | Rest |
| Fly agaric | 1 game hour | Vomiting, −10 Condition, and 6 hours of hallucination: the sky ripples and wolf calls sound from nowhere | Stay put; it passes |

**Teas.** A cup takes 0.25 L of boiled water and 5–10 game minutes. Every hot cup gives +5 warmth and +8 water on top of the plant's effect: spruce tips and rose hips for vitamin C, chaga and Labrador tea for Spirits, willow bark as medicine. Plants and fungi › Plant medicine adds 18 remedies, among them coltsfoot tea for the cold cough.

**Nettle.** Picking nettle bare-handed costs 1 Condition and 1 game minute of stinging, shown as twitching fingers. Gloves or mittens prevent it, and cooked or dried nettle no longer stings.

**Tests.** Every lookalike pair differs on at least two checks in the inspect card; no taste test lowers Condition below 1; over 100 seeds the pocket guide identifies 12 of the 25 plants on this page (Plants and fungi adds 87, none in the guide); a patch stripped above 70% yields 45–55% of its seed value the next year.

### Spirits

A year alone in the dark needs a mood, but not a second death meter. Spirits run from 0 to 100, move slowly, never kill, and never take control away from the player. The messenger shows a single word (Bright, Steady, Low) rather than a number.

| Raises spirits | Lowers spirits |
| --- | --- |
| A warm fire at night | Weeks of short days in midwinter |
| A varied meal; something sweet | The same food five days running |
| Sunlight, especially in winter | Long stretches cold or wet |
| Time with the tamed wolf | Injury, illness, losing the wolf |
| Finishing a build or an ambition (+10) | Repeated failed hunts |
| An aurora night, chaga tea | A raided cache |

Above 70, work goes 10% faster and sleep restores more. Below 30, work slows by 10%, sleep is lighter and aim shakes a little. Everything that lifts spirits is something the player would want to do anyway, so it nudges toward a good life rather than adding a chore.

**How Spirits moves.** Spirits starts at 60. Daily sources add up through the day and apply once when the player sleeps, with the day's total clamped to −3 to +4. Events apply at once and are not clamped. When Spirits is below 30 and the day's total is 0 or more, it gets +1 extra, so a player is never stuck at the bottom.

| Daily source | Change | Counts when |
| --- | --- | --- |
| Warm night | +1 | Slept in a warm room or within 3 m of a fire, and woke at warmth 70 or more |
| A varied day | +1 | Three or more different foods eaten |
| Something sweet | +2 | Berries, syrup or sap |
| Sun | +3 in winter, +1 otherwise | One game hour outdoors in direct sun (Nov–Feb), or a clear day spent outside |
| Time with the wolf | +2 | 10 game minutes within 3 m of it, sitting or petting (E hold) |
| Evening light | +1 | A lamp, fire or lantern lit 3 hours after dark within 16 blocks of the bed while awake. Placed lanterns replace this source, never add to it: the first gives this +1, each more +1, up to +3, inside the daily clamp of +4 (RECONCILE 25; Every concept from the video › Lanterns) |
| Chaga tea / Labrador tea | +2 / +1 | Once a day each |
| A decorated sleeping room | +1 per 2 pieces, up to +3 | Trophies count double |
| Quiet fishing | +1 | An hour with a line in, caught or not |
| A named animal seen | +1 | Any habituated, named animal |
| Short days | −1 under 7 h of daylight, −2 under 5 h | November to January |
| Same food | −2 | From the sixth day of the same main food |
| Cold or wet stretches | −1 per 6 game hours at warmth under 50 or wetness over 50%, up to −3 | Any day |
| Each active injury or illness | −1 | Any day |
| Solitude | −1 | After 14 days with no company (see below) |
| Hungry or thin | −1 | Day's balance under −1,500 kcal, or BMI under 19 |
| Mosquitoes | −1 per 3 hours exposed | June and July |

| Event | Change |
| --- | --- |
| An ambition or a build finished | +10 |
| A first (first pike, first moose, first night above freezing indoors) | +5 |
| A big catch: a fish over 10 kg or any big game | +6 |
| Watching the aurora for 1 game minute outdoors / an aurora storm | +5 / +8 |
| A soak in the hot spring, or a sweat bath | +6, once a day |
| A new logbook page read | +2 |
| A supply drop landing | +8 |
| Naming an animal | +2, once per animal |
| The solstice fire on December 21 | +5, once a year |
| Sun's back: the first day the sun clears the ridge at the home cabin | +10, once a year |
| Breakup heard at the home river | +5, once a year |
| A named animal survives the winter (checked May 1) | +5 |
| A raided cache | −8 |
| A tool lost to the river or a bear | −4 |
| A failed hunt (game fled, or a wounded animal lost); the third in a row | −2; −5 |
| A meal of spruce-smoked meat | −1 |
| The wolf hurt / the wolf lost | −5 / −25 |
| A named animal killed | −5 |
| Waking soaked on bare ground in the rain | −2 |

**Calibration.** A winter of short days, the same dried fish and no helpers loses about 3 a day, so it reaches Low in 8–14 days. The same winter with a lamp, chaga, a varied larder and the wolf gains 3–4 a day and stays Bright. That gap is the whole design: Spirits is the reason to do the things that make a life.

**Effects by band.**

| Band | Spirits | Effects | What the player notices |
| --- | --- | --- | --- |
| Bright | Over 70 | Work 10% faster, sleep restores 10% more, practice builds 25% faster | The survivor hums while working; the music theme plays at dawn |
| Steady | 30–70 | None | Ordinary sound and color |
| Low | Under 30 | Work 10% slower, sleep restores 10% less, woken by noises from twice as far, aim sway +15% | Footsteps and breathing louder, no humming, music only by the fire |
| Low, deep | Under 15 | The Low effects, nothing more | Colors 15% duller; journal lines get shorter |

Below 15 nothing else gets worse. Spirits never causes an SOS, never forces an action and never offers the SOS button. The word on the vitals screen stays Low.

**Company and the year alone.** There are no people in the valley, so company is whatever answers back. Company resets the solitude counter: 10 minutes with the wolf, a reply on the radio after each daily log, a supply drop, or a named animal seen. Before the radio and the wolf, the only company is animals that habituate near camp. Inspecting a habituated animal offers "Name it," and from then on the journal follows it: when it visits, when it sheds its winter coat, when a predator takes it.

**Activities that help.** Each one is something the player would do anyway.

| Activity | Needs | Time | Spirits |
| --- | --- | --- | --- |
| Sit by the fire after dark (Z) | A fire | 30 game minutes | Counts as the warm night and evening light |
| Whittle a figure | Knife, a birch round | 1 game hour | +2 as a build; makes a decoration |
| Write a line in the journal | The messenger | Real time; the clock keeps running | +1, once a day |
| Play the willow whistle | A whistle cut from a willow shoot in May or June, when the bark slips | 10 game minutes | +1, once a day; the wolf howls along above 50 trust |
| Watch the aurora | A clear night with the forecast's aurora chance | 1 game minute | +5 |
| Tend the garden | A bed | Any weeding or watering job | +1 in summer |
| Read logbook pages | Pages found | 2 game minutes each | +2 per new page |
| Light the solstice fire | A teepee of 3 armloads on December 21 | 1 game hour | +5, and a journal entry |

**The journal's voice.** Each night at sleep, `story.Journal` writes one line in the survivor's voice. It picks the day's top event by weight (a first, then a kill or catch, then danger, then weather, then routine) and a template for the current band. Templates fill tokens such as {wolf}, {name} and {kg}. The player can edit any line or add their own.

| Band or moment | Sample line |
| --- | --- |
| First scare | "Day 3. Saw a grizzly across the river. It never looked up. I didn't sleep much." |
| Bright | "Day 47. Pike on live bait, the biggest thing I've caught. Fish-head soup tonight." |
| Bright | "Day 101. Forty kilos of chum on the racks and the woodpile up to the eaves. Good week." |
| Bright | "Day 152. {wolf} brought a hare back and dropped it at the door. I said thank you out loud." |
| Steady | "Day 23. Rain all day. Reset four snares and ate the last of the crackers." |
| Steady | "Day 118. Frost on the barley last night. Cut what was left." |
| Steady | "Day 199. Eleven centimetres at the bend. Tomorrow I walk on the river." |
| Solitude | "Day 180. Said my own name out loud to hear it. It sounded like someone else's." |
| Solitude | "Day 207. Made a list of everyone I'll call when I get out. Put {wolf} at the bottom, then moved {wolf} to the top." |
| Low | "Day 222. Four hours of light. Didn't go out." |
| Low | "Day 226. Dried fish again. I can't remember what bread tastes like." |
| Low | "Day 229. Talked to the stove for a while. It listens." |
| Low | "Day 231. Nothing in the snares. Nothing moved out there today but me." |
| Low, deep | "Day 238. Cold." Then: "Day 239. Same." |
| Solstice | "Day 235. Longest night. Built the fire big and stayed up to see the light come back." |
| Sun's back | "Day 262. The sun cleared the ridge at noon and touched the cabin wall. I stood in it until it went." |
| Named animal | "Day 270. {name} was at the woodpile again. Still white. Still here." |
| Breakup | "Day 352. Heard the river before I saw it. Open water at the bend." |
| Year card | "Day 365. One year. 77 kg in, {kg} kg now. Still here." |

**What the messenger says.** The messenger never says how the player feels. When Spirits has been Low for 3 days, one morning prompt is drawn from real world state that points at a helper: "Clear tomorrow: sun on the south bank 11:40–14:10." "Aurora likely tonight after 23:00." "Chaga in stores: 3 pieces." "{wolf} has not been fed today." "Rose hips under the snow at the east-slope patch." Only one such prompt shows a day.

**Build and test.** `body.Spirits` sums the day's events from the event bus in step 11 of the body tick and applies the clamped total at the next sleep; every source and value lives in the `spirits` block of `data/yearofice/body/standard.json`, with its tuning mirror in `yearofice-common.toml`. Debug: `/yoi set spirits <0–100>` and `/yoi spirits day`, which prints the day's running sum by source. *Test:* a scripted December with a stove, dried fish only and no helpers reaches Low between day 8 and day 14; the same December with a lamp, chaga tea every second day, three foods a day and the wolf never drops below 50; no path through the rules lowers Spirits below 0 or blocks an input.

### Danger in detail

Every deadly situation has warning signs and a right response the player can learn, so a death feels earned, never random.

**Bear encounters.**

| Situation | Signs | Right response | Wrong response |
| --- | --- | --- | --- |
| Bear unaware, at a distance | Foraging, head down | Back away upwind and detour | Walking closer for a look |
| Bear notices the player | Stands up, sniffs, stares | Talk calmly, back away slowly, ready the spray | Running, which triggers a chase |
| Bluff charge | Huff, jaw-pop, ears back, then a rush that stops short | Stand ground; spray at 8 m | Running |
| Contact | Knocked down | Grizzly: lie flat and protect the neck; if it keeps attacking, fight back | Fighting a bear that is only defending |
| Bear on a carcass | Feeding, covering it with dirt | Give it up and leave | Contesting the kill |

Tools: one can of bear spray from the crate, a flare from a supply drop, fire, noise, and the tamed wolf's early warning. Grizzlies climb poorly, so a tall tree is an escape, as in the source video.

**Thin ice.** Ice under 5 cm breaks under the player, 10 cm holds a person, 20 cm holds a loaded sled. Fresh snow hides thin spots; current and springs keep ice thin in winter. Falling in gives about a minute of cold shock to climb out (hold to pull; ice picks halve the time), then warmth drops 6 points a minute while soaked, so a fire must be reached within 10–15 minutes. Ice picks are made from wood and two nails.

**Felling trees.** The notch sets the fall direction. A tree landing on the player breaks a bone; a dead top (a widowmaker) can drop on whoever is swinging below it.

**The river.** Cold water steals breath for the first minute. Current pushes downstream, and swimming burns energy fast. A capsized canoe loses anything not tied in.

**Whiteouts and getting lost.** In a blizzard visibility falls to 20 m. Breadcrumbs on the messenger map still work if the battery holds; otherwise the player navigates by the compass from the crate, known landmarks and their own cut trails.

**Falls.** A drop over 3 m sprains, over 6 m breaks a bone; deep snow cushions both.

**Danger is heard first.** Every threat has a sound cue the player can learn, shown as a caption when captions are on. The cue always comes before the harm.

| Cue | Means | Heard at |
| --- | --- | --- |
| Huff and jaw-pop | A defensive bear, about to bluff charge | 30 m |
| Low grunt, ears back, hackles up | A moose about to charge | 40 m |
| Growl, or a bark-howl | Wolves close and testing | 60 m |
| One long howl at dusk | A pack in the area | 2 km |
| Tail slap on water | A beaver warning; a bite follows if the player wades closer | 50 m |
| Squirrel scolding | Something is moving near: maybe the player, maybe a predator | 50 m |
| Ravens calling over one spot | A carcass, and bears on the way | 500 m |
| A sharp crack underfoot | Ice 5–10 cm: stop, lie down, crawl back | Underfoot |
| A long boom across a lake | Ice expanding in hard cold: thick and safe | 2 km |
| Creak, then a snap from a trunk | A tree falling within 1.5 s | 20 m |

**How a bear decides.** The shared state machine (`sim.animal.Awareness` and `ChargeGate`, Minecraft development › Entities and AI) runs every bear through these states, with the bear's numbers in the `behavior` block of `data/yearofice/species/<id>.json`. The player's window is the time between the cue and the harm.

| Bear state | Triggered by | What it does | The player's window |
| --- | --- | --- | --- |
| Unaware | Player outside its senses | Forages along its range | All the time needed to detour upwind |
| Alert | Senses the player | Stands, sniffs and stares for 5–15 s | 10 s: talk calmly (V tap) and back away. That makes it drop and leave within 30 s 70% of the time |
| Curious | A young bear, or food scent with no threat felt | Circles closer, to about 20 m | Shout (V hold), stand on something high; it leaves 80% of the time |
| Defensive | Surprised inside 25 m, a sow with cubs inside 40 m, or a bear on a carcass inside 50 m (How animals act › Species responses) | Huff, jaw-pop, ears back, then a charge | Stand still. A defensive charge at a still player stops short 3–10 m away 85% of the time |
| Chasing | The player ran, or a bluff turned real | Runs the player down | Ends at 150 m or at a fire. Otherwise contact |
| Predatory | Rare: a hungry spring bear, or one that has learned camp food; more often black bears | Follows quietly and comes in with no huff or jaw-pop | Fight at once; never play dead |

A sow with cubs in May and June is always Defensive inside 40 m and turns Alert at once inside 80 m. A bear that has fed on a camp's food twice moves one step toward Curious with that camp for the rest of the year.

**Using the bear spray.**

1. B readies the can in 1 s if it is in the off-hand slot or on the hotbar; from the pack grid it takes 4 s. The onboarding moves it to the off-hand slot and says why.
2. The first LMB press flips the safety clip (0.3 s).
3. Holding LMB sprays. The can empties in 7 s into a cloud 8 m long and 2 m wide that rises as it goes, so aim at the ground in front of the bear.
4. A bear inside the cloud between 3 and 8 m turns and leaves 95% of the time.
5. Wind over 4 m/s toward the player blows the cloud back: 30 s blinded and −5 Condition. A side wind moves the cloud 1 m for every 2 m/s.
6. Once triggered, the can is spent, which is the single use.

**Contact with a bear.**

1. The player is knocked down and the view falls to the ground.
2. Against a grizzly that was defending, holding Shift (crouch) plays dead: face down, hands behind the neck, pack on the back. The pack takes the first bite.
3. A defensive grizzly bites 2–4 times over 10–20 real seconds, then leaves (clips: Minecraft development › Encounter clips). Each bite is −8 Condition with a 30% chance of a deep cut and 10% of a broken bone.
4. Stay down 30 real seconds after it leaves. Getting up while it is within 30 m brings it back half the time.
5. If it keeps attacking past the fourth bite, or it was a black bear or a predatory bear, the sound changes from roaring to quiet feeding sounds. Then fight: LMB strikes the face with whatever is in hand. Each hit has a 25% chance to drive it off, 40% with the hatchet.

**Trees as escape.** A grizzly can reach 3 m standing and climbs poorly, so 4 m up a spruce (hold Space at a climbable trunk) is safe from it, as in the video. A black bear climbs better than the player. Climbing away from a black bear is a mistake the game lets the player make once.

**The first bear.** The director guarantees one in the first 3 days. The first time the player reaches the river in daylight after the first night, or by noon on day 3 at the latest, a grizzly appears 120–200 m away across the water or downstream. It is Unaware, foraging, and downwind of the player. The messenger buzzes once with the only distance it will ever give: "Bear, 150 m downstream. It hasn't seen you." If the player watches, the bear stands, sniffs the air and walks off within 2 game hours. It approaches only if the player crosses to within 50 m. The journal logs the day-3 line in Spirits.

**Moose.** A moose charge is the opposite lesson to a bear's: running is right. A cow with a calf (May–August) inside 30 m, a rutting bull (September–October) inside 25 m that sees the player standing, a wounded moose pressed inside 15 m, or a deep-snow moose met on a packed trail inside 10 m (15 m in March over 90 cm of snow) goes to Warn and then charges; 60% of moose charges are bluffs (How animals act › Species responses). Signs are ears laid back, hackles up on the hump, lip-licking and a low grunt. The right response is to back off, then run and put a tree or a building between; most charges end after 20–30 m. A moose that makes contact stomps: −20 Condition per stomp with a 40% chance of a broken bone, until the player curls up (hold Shift) and it leaves after 5–10 real seconds.

**Wolves testing a weak player.** In winter a pack tests a player who is weak or carrying fresh meat, in three steps.

| Step | Distance | What the player sees | Right response |
| --- | --- | --- | --- |
| Shadowing | 100–200 m | Howls close by, tracks laid over the player's own | Keep moving toward camp; don't run |
| Circling | 30–50 m | Glimpses between trees, a wolf crossing the trail ahead | Face them, stand tall, light a torch |
| Testing | 10–15 m | One wolf rushes and stops; the pack watches | Shout (V hold), throw a stone (a hit sends it running), wave the torch, back toward a fire or a tree |

A test starts by the rule in How animals act (November–March, a pack of 3 or more, two of: Condition under 40, energy under 3 bolts, fresh meat carried, snow over 40 cm, night). It turns into a lunge only if the player runs, turns their back, or stands without a torch or fire while the rushing wolf closes to 5 m. A lone wolf (a disperser, November–February) never tests; it shadows a camp with meat at 50–100 m, and rushes only a player under 30 Condition at night, with the same lunge. A lunge is −10 Condition with a 50% chance of a cut. Striking or burning the wolf ends it. Running turns testing into a chase, which ends at 150 m or a fire.

**The siege.** A pack drawn by scent batters the shelter door, once every 10 game minutes. A hide flap gives at once. An unbarred plank door gives on the third hit. A barred door holds all night. The pack leaves at dawn, or as soon as one wolf is struck at the door.

**Smaller animals.** A beaver bites after its tail slap if the player stays within 5 m in water (−6 Condition, 70% a cut). A porcupine grabbed bare-handed gives −3 Condition and quills that take 20 game minutes to pull; quills in the wolf take the same and cost it 5 trust if the player does not pull them. A wolverine at a cache growls and stands its ground but never charges a player. Lynx and foxes are no danger.

**Reading ice.** Clear black ice counts at its full thickness. White snow-ice counts as half; grey, slushy ice at breakup as a quarter. Tapping ahead with a pole, the hatchet or an ice chisel (hold right mouse, 2 s) gives a sound and a messenger reading such as "About 8 cm, clear," saved as a pin. Ice stays thin at inflows and outflows, on the outside of bends, over springs and around beaver lodges. Crawling (Shift held on ice) counts the ice as 1.5 times thicker, which is how the player backs off after a warning crack.

**Overflow.** Water can sit on top of the ice under the snow, near springs and after heavy snow. It shows as darker, sagging snow and a slushy squelch. Stepping in soaks the feet, which starts the frostbite timer for feet at the wet limit, and freezes slush to snowshoes: 50% speed until scraped (1 game minute).

**Self-rescue, step by step.** Falling through starts the close clock, so each game minute is 10 real seconds.

1. **Cold shock.** The first game minute: a white flash, gasping, and no control. Thrashing keys does nothing; the lesson is to wait it out.
2. **Turn back.** Face the way the player came, because that ice held them. The edge on that side sounds solid; any other edge breaks twice as often.
3. **Pull.** Hold right mouse to pull and brace (RECONCILE 124). The bar fills in 3 game minutes, or 1.5 with ice picks. A pack over 15 kg doubles the time unless dropped (Q); a dropped pack floats 30 real seconds and can be hooked back with a pole. Each edge that breaks drops the bar by 20%.
4. **Strength.** The player has 10 game minutes of usable strength in the water. At the end, they drown, which is a collapse.
5. **Roll.** Once out, hold Shift to crawl 3 m from the hole. Standing up closer than that breaks through again half the time.
6. **Warm.** The soaked rule now takes 6 warmth a game minute. A fire within 10–15 game minutes saves them. A fire kit in a salmon-skin bag stays dry; rolling in dry powder snow below −15 °C (a Shift crawl, 5 real seconds, once) wicks off 20% of the wetness.

**The river.**

| Water | Current | Rule |
| --- | --- | --- |
| Side creek | 0.5–1 m/s | Wadeable to the thigh |
| Main river | 1–2 m/s | Thigh-deep wading has a 5% chance a second of a knock-down; a wading pole halves it. Swimming upstream is impossible |
| Rapids below falls | 3 m/s | Swept; no wading |

In the water, warmth falls 3 a game minute in summer water (8–12 °C) and 6 a minute below 4 °C. A pack over 15 kg halves swimming speed and doubles its energy cost until dropped. A sweeper (a fallen tree in the current) pins anyone swept into it: 10 real seconds holding right mouse to pull and brace over it, or drowning. In a canoe, loading offers "Lash all" (10 game minutes) or a lash per item; on a capsize, unlashed light items float off and collect at the next snag, and heavy ones (iron, stones, tools) sink.

**Felling.** A tree falls within 30° of its notch, pushed by its lean and by wind over 5 m/s, and creaks 1.5 s before it goes. The danger zone is one tree height along the fall line; the safe path is 45° back from it. A direct hit is −40 Condition with a 60% chance of a broken bone; a glancing branch is −15 with a 50% sprain. Each swing at a dead-topped tree has a 3% chance the top drops: −25 Condition and a 30% chance of being knocked out for 30 game minutes. A leaning tree cut without a notch splits up the trunk and kicks back 20% of the time (−20). A hung-up tree drops 5% each second the player stands under it. The safe fix is a rope pull from 10 m (hold right mouse, 5 s); felling the tree it rests on sends both down unpredictably 40% of the time.

**Whiteouts.** A blizzard lasts 6–24 game hours, cuts sight to 20 m, hides every landmark and muffles all sound to half range. Footprints fill in within 30 game minutes. Three things still work:

- **The compass**, held in a quick slot, shows a bearing at the bottom of the view.
- **Blazes** cut on trunks show pale at 20 m.
- **A rope line** (1 m of cordage per metre) strung between cabin, woodshed and cache lets the player hold right mouse on it and walk at full speed without seeing.

Caught in the open, a player in snow 60 cm or deeper can dig a snow trench: 1 game hour with a shovel, 2 by hand with wet hands. It counts as +10 °C shelter with no wind, and a candle inside adds +3.

**Falls.** Snow cushions a fall at 3 m of height per metre of depth. Running on bare ice has a 3% chance each second of a slip (−3 Condition, 10% sprain); ice creepers (nails through rawhide straps, iron tier) stop it.

```latex
h_{eff} = h_{drop} - 3 \cdot d_{snow}
```

| Effective height | Result |
| --- | --- |
| Over 3 m | Sprain |
| Over 6 m | Broken bone |
| Over 12 m | Broken bone and −60 Condition |
| Over 20 m | Collapse |

**The adrenaline sprint.** A player who cannot run (energy under 1.5, Slowed, or Condition under 30) gets one 10-second sprint per encounter when a bear or moose charges or a pack chases. Afterward energy drops 1 bolt and aim sway is +30% for 60 real seconds.

**Damage sources.** Every source rolls one injury from this table, on the body part the source favors, and every row goes through `BodyService.applyInjury(player, injury, cause)`, the only injury path, animal attacks included (RECONCILE 90).

| Source | Condition | Injury roll |
| --- | --- | --- |
| Bear bite (defending) | −8 | Deep cut 30%, broken bone 10% |
| Bear swipe (predatory) | −15 | Deep cut 50% |
| Moose stomp | −20 | Broken bone 40% |
| Wolf lunge | −10 | Cut 50% |
| Beaver bite | −6 | Cut 70% |
| Falling tree, direct | −40 | Broken bone 60% |
| Widowmaker | −25 | Knocked out 30% |
| Knife slip while cleaning with warmth under 30 | −3 | Minor cut, 5% a job |
| Hatchet glance on a knot with energy under 3 | −10 | Deep cut to the leg, 2% a swing |
| Standing in fire | −5 a second | Bad burn after 2 s |
| Boiling water spilled | −10 | Bad burn |

**Tests.** A still bot facing 100 defensive grizzly charges is contacted 10–20 times; a running bot is chased every time and never past 150 m; with ice picks, a fresh bot climbs out within 1.5 game minutes in 9 of 10 runs; the spray turns 90–99 of 100 bears inside its cloud; a bot standing in the felling danger zone is hit on every fall and one at 45° back never is.

## Food systems

No single food source keeps a working body above BMI 19, so the player always runs two or three at once. A working day burns 2,500–3,500 kcal; a winter day of hauling and cold burns 4,000 or more. Foods are sized so one or two real meals a day are enough: a pike is a day's food, a moose is weeks. Wild eggs in June, hens and goat milk once the radio catalogue sells them, supply drops and the spring and fall bird migrations fill the gaps; every food's numbers per 100 g are in Items › Food values.

| Source | Opens with | Typical yield | Best season | Main risk |
| --- | --- | --- | --- | --- |
| Foraging | Hands | 50–300 kcal per hour picking; almost no fat | Spruce tips May; berries Jul–Sep | Time spent for little food |
| Snares | Wire or cordage | Hare ≈ 1,070 kcal | Fall and winter | Catch stolen if not checked in 48 h |
| Rod fishing | Hook, line, bait | Grayling 250–950, pike 1,500–15,000 kcal (Fishing in depth, kcal per kg) | May–Oct; ice fishing Nov–Apr | Lost fish and bait; bears at the water |
| Passive fishing | Gill net (4 segments), minnow trap | Whitefish 1,500 kcal per catch | Open water | Nets tear in breakup ice |
| Bow hunting | Light bow, arrows | Grouse 640, beaver about 9,300 kcal | All year | Wounded animal runs; noise spooks game |
| Big game | Longbow, flint broadheads | Bull moose ≈ 239,000, grizzly ≈ 80,000, black bear ≈ 50,000 kcal | Fall (fat), late winter (tracks) | Charges; days of butchering; spoilage |
| Farming | Seed packet from the crate | Potato and barley harvest in late Aug | Plant late May–June | Frost; 60% germination; saved seed only |

**Fishing minigame.** Five phases, each with a clear failure:

1. **Cast:** hold to charge distance; the bobber lands where the arc ends. Current drifts it downstream until a sinker is added.
2. **Wait:** a bite rolls each game minute from the bite-chance formula above. The rod tip twitches before a real bite.
3. **Strike:** the bobber dips; the player has 0.6 s to set the hook, or the fish takes the bait.
4. **Fight:** a tension bar. Reel while the fish tires, ease off when it runs. Tension at 100% snaps the line; slack for 3 s lets it spit the hook.
5. **Land:** walk the fish into shallow water and press to grab it. A fish left flopping on the bank for 4 s can flip back in, as happened in the video.

**Snares.** Catch chance rolls at dusk and dawn. Placement multipliers: on a game trail ×4, beside cover ×1.5, checked from within 5 m more than once a day ×0.5 (human scent). Anything left over 48 h may be taken by a fox or wolf.

**Hunting.** Animals sense the player three ways:

- **Sight:** a cone of 60–150 m by species; movement is seen at twice the range of a still player.
- **Hearing:** sprinting carries 30 m, walking 10 m, crouching 3 m; snapping a stick adds a 20 m burst.
- **Smell:** a scent cone 80 m long trails downwind of the player; the messenger shows wind direction.

Arrows drop with distance. A vital hit kills or leaves a short blood trail; a gut or leg hit leaves a long one. Blood trails fade over 2 game hours, so tracking is a race.

**Butchering.** Needs a knife and time: hare 15 min, beaver 1 h, moose 6–8 h over several hauls. Yields meat, fat, hide, sinew, bone and antler. Gutting creates a bear scent point, so big kills are butchered fast or far from camp.

**Cooking.** Raw meat carries a parasite chance (bear 25%, other game 5%, fish 2%). Roasting is fast; stewing in a pot recovers 15% more calories from bones and heads (fish-head soup). Rendering fat makes lard.

**Preserving.** Every food has a spoilage clock tied to air temperature:

| Method | Needs | Keeps fresh meat for |
| --- | --- | --- |
| None, warm day (15 °C) | — | 2 days |
| None, cool (5 °C) | — | 6 days |
| Natural freezer (below −5 °C) | Winter | Until the thaw |
| Smoking | Smoke rack or smokehouse, 1 game day | 30 days |
| Drying | Drying rack, 2 dry sunny days | 60 days |
| Pemmican | Dried meat + lard + berries | 1 year; highest kcal per kg |
| Root cellar | Dug cellar | Potatoes and roots through winter |

Any food store within 400 m of a bear home range draws bear visits; a raised cache keeps them out only with its deck underside 3 m or more above the ground the bear stands on and its legs banded (RECONCILE 23; Structures and buildables).

### Cooking

**Cooking is a method applied to ingredients.** The pot, spit and stone hold no fixed recipes. Each method multiplies what its ingredients carry, so any sensible mix works, and a named dish is a mix the notebook recognizes and names.

| Method | Station | Time | Calories | Parasites | Vitamin C kept | Extra |
| --- | --- | --- | --- | --- | --- | --- |
| Raw | Hands, knife | 5 game minutes | 100% | Full chance: bear 25%, other game 5%, fish 2% | 100% | Fresh or Aging food only |
| Spit or stick roast | Spit over coals | 30 game minutes per kg; whole hare 25, fish 12 | 100% | None when Done | 0% | +10 warmth for 30 game minutes |
| Stone fry | Flat cooking stone | 10 game minutes per 0.5 kg | 100%, plus any lard used | None when Done | 25% | Fastest; needs 20 g of lard per 0.5 kg or it sticks and burns |
| Coal bake | Wrapped in clay or birch bark, buried in coals | 40 game minutes | 100% | None | 50% | Cannot burn on a Low or Steady fire; good for potatoes and whole fish |
| Stew or boil | Pot on a cooking stand | 60–90 game minutes | 115% when bones, heads or frames go in | None | 50%, kept in the broth | 1 L water per kg; returns 0.5 L of drinking water per kg; +15 warmth |
| Render | Pot on a Low fire | 60 game minutes per kg of fat | Lard keeps 90% of the energy | — | — | 1 kg raw fat (7,000 kcal) gives 0.7 kg lard (6,300) and 0.1 kg cracklings (550); 0.2 kg of water cooks off, so rendering never adds calories |

**Doneness.** Food on a spit, stone or in coals shows a color ring: Raw, Rare, Done, Burnt. Rare is pulled at 50–90% of cook time; it halves the parasite chance and keeps half the vitamin C, which makes rare liver a real winter choice. Burnt comes at 150% of cook time (75% on a Hot fire), keeps 70% of the calories and costs 2 Spirits.

**Bear meat is the exception.** It must be cooked to Done for twice the normal time, or stewed. Freezing never makes bear meat safe. Raw fish stored below −20 °C for 7 game days is parasite-free and keeps its vitamin C, so frozen fish shaved thin (stroganina) is the safe winter source of vitamin C on a meat diet.

**Vitamin C in food** is counted in C-days: one C-day covers one day toward the 30-day scurvy limit in Health. Raw fish gives 1 C-day per 0.5 kg, raw liver 1 per 0.25 kg, rose hips 1 per 50 g, fresh berries 1 per 200 g unless Plants and fungi sets the species, fresh spruce tips 1 per 100 g (old needles half), and potatoes 2 per kg in any method, because the skin holds it. The method's "Vitamin C kept" share applies.

**Meals.** One meal holds up to 1.5 kg and 2,500 kcal and takes 15 game minutes. The first time the player eats a named dish, Spirits rise 3 at once. Anything sweet counts as the daily source "Something sweet" (+2, inside the daily clamp); the sweet dishes that name their own +5 (akutaq, birch syrup, canned peaches) give it as an event, once a day, instead. The variety rule counts the main ingredient's family (moose, hare, pike, barley): if one family gives over 80% of the day's calories five days running, Spirits fall 3 a day until another family takes over.

| Dish | Ingredients | Method | Gives | Note |
| --- | --- | --- | --- | --- |
| Fish-head soup | 4–6 heads and frames, 1 L water | Stew, 45 game minutes | About 300 kcal from parts otherwise thrown away, 0.5 L water | The video's soup |
| Marrow broth | 2 kg split leg bones from big game, 2 L water | Stew, 4 game hours, queueable | 600 kcal, mostly fat; 1.5 L water; +5 Spirits | Leftover bone still goes to tools |
| Camp stew | 1 kg meat with bones, roots or greens, 1 L water | Stew | Meat ×1.15 plus the vegetables; +15 warmth | Turning meat stewed has half the gut-illness chance |
| Liver in lard | 0.5 kg liver, 20 g lard | Stone fry, Rare | 800 kcal, 1 C-day | Eaten the day of the kill; see organ timers below |
| Barley porridge | 200 g barley, 0.5 L water | Stew, 30 game minutes | 700 kcal of carbs | A year-2 breakfast |
| Flatbread | 250 g barley flour, 30 g lard | Stone fry | 1,100 kcal; keeps 5 days | Trail food; flour needs a quern stone (bushcraft) |
| Baked potatoes | 1 kg potatoes | Coal bake | 770 kcal, 2 C-days |  |
| Akutaq | 0.5 kg lard, 0.3 kg berries, a handful of snow | Whipped by hand, 20 game minutes | 4,650 kcal, 1.5 C-days, +5 Spirits once a day | The Alaskan "ice cream"; a midwinter treat |
| Birch syrup | 40 L of birch sap | Pot on Steady, 8 game hours, queueable | 0.4 L of syrup, 1,100 kcal in 8 servings, +5 Spirits once a day | The year's first sweet thing, at breakup |
| Spruce or rose-hip tea | Needles or 50 g hips, 0.3 L water | Steeped 10 game minutes, not boiled | 0.3 L water, +5 warmth, 1 C-day from fresh spruce tips (0.5 from old needles) or 1 from 50 g of hips | A hard boil destroys the vitamin C |
| Mushroom stew | King or birch bolete, hedgehog mushroom, chanterelle or shaggy mane, and water; poison pax is the brown lookalike of the boletes (Land and seasons in play › New plants and fungi; Plants and fungi › Mushrooms) | Stew | 180 kcal and some water | The lookalike rule applies |
| Sauerkraut | 1.5 kg cabbage, 30 g salt | Packed in a barrel for 21 game days | 100 kcal and 3 C-days per 0.5 kg; keeps until May | Needs salt: the crate's crock, a supply drop or a mineral lick (Open world in depth › Salt from the mineral lick) |

### Butchering and yields

**Every carcass yields the same kinds of parts.** Meat carries 1,150 kcal per kg, raw fat 7,000, organs 1,300. The totals in Wildlife and fish are these parts added up for a fall animal.

| Species | Meat | Fat (fall) | Organs | Hide or pelt | Extras |
| --- | --- | --- | --- | --- | --- |
| Red squirrel | 0.22 kg | — | — | Small pelt | Tail hair for flies |
| Willow ptarmigan | 0.35 kg | — | Heart, gizzard | — | 12 feathers |
| Spruce grouse | 0.52 kg | — | Heart, gizzard | — | 15 feathers |
| Snowshoe hare | 0.87 kg | — | 0.05 kg | Pelt | — |
| Muskrat | 0.45 kg | 0.01 kg | — | Pelt | — |
| Duck / goose | 0.75 / 2.3 kg | 0.02 / 0.19 kg | Heart, liver | — | 20 / 30 feathers, down |
| Beaver (18 kg whole) | 6.5 kg | 0.2 kg, the tail | 0.3 kg | Pelt | Castor glands (trap lure); the tail also gives 0.8 kg of tail meat |
| Porcupine | 2.5 kg | 0.27 kg | 0.2 kg | — | 200 quills |
| Wolf | 16.5 kg | 0.05 kg | 0.8 kg | Pelt | 0.1 kg sinew |
| Dall sheep | 23 kg | 0.3 kg | 1.2 kg | Hide | Horns, 0.2 kg sinew |
| Caribou | 44 kg | 1.0 kg | 2.0 kg | Hide | Antlers, 0.3 kg sinew |
| Black bear | 30 kg | 2.0 kg | 1.2 kg | Hide | Claws, 0.2 kg sinew |
| Grizzly | 48 kg | 3.2 kg | 1.8 kg | Hide | Claws, 0.3 kg sinew |
| Moose (bull, 450 kg whole) | 180 kg | 3.0 kg | 8.3 kg: heart 2.5, liver 4, kidneys 0.8, tongue 1 | Hide, 30 kg green | Antlers, 0.5 kg sinew, 8 kg marrow bones |

**Season changes fat.** The fat column is for September to November. Bears leaving the den carry 15% of it, late-winter moose and caribou 25%, and a rutted-out bull in late October 50%. A March bull is still about 223,000 kcal, but nearly all lean, which is why fat runs out before calories do (Bestiary › Parts by species; RECONCILE 53).

**Steps at camp.** Small and medium game can be dressed anywhere with the knife, one hold-to-work step at a time; at the cleaning spot (Making camp) every step is 30% faster (RECONCILE 42):

1. **Skin or pluck.** Hare 5 game minutes, squirrel 3, beaver 25 (slow, for the pelt). A porcupine is singed in the fire for 5 minutes first. Birds are plucked in 4 minutes, keeping the feathers, or skinned in 1 minute, losing them: a real choice when arrows need fletching.
2. **Gut.** Hare 3 game minutes, beaver 10. Guts go to bait, the wolf or the fire. A cut into the gut or bladder (10% at practice 0, 1% at full practice) taints 10% of the meat.
3. **Cut.** Left whole for the spit, or jointed into 4–6 pieces for the pot or smoker: 5 game minutes for a hare, 20 for a beaver.
4. **Keep the extras.** Feathers, castor, tail fat, sinew and bones drop as separate items. An extra not picked up stays in the kill pile, where scavengers and the decay table take it (RECONCILE 42).

**Skin within 12 hours.** Above 5 °C a pelt left on the carcass over 12 game hours slips its hair and is good only for rawhide. Below −5 °C it freezes on; a frozen small carcass thaws by a fire (1 game hour for a hare) before it can be skinned. Big game is handled at the kill (Field care).

### Spoilage model

**Freshness.** Every food item carries freshness from 100 to 0, falling at a rate set by temperature, the kind of food, and where it sits. Nothing ticks: the stack stores its due point on its holder's decay clock and is settled when read, opened, moved or eaten (RECONCILE 70; Decay and time › The decay clock). L is the safe life: the time from 100 to 30, the end of the last safe stage.

```latex
L(T) = 6 \cdot 3^{(5 - T)/10}\ \text{days} \qquad \Delta F_{hour} = -\frac{70}{24 \cdot L(T) \cdot k_{food}} \cdot m_{place}
```

T is the temperature where the food sits: air, room, cellar. L is 2 days at 15 °C and 6 at 5 °C, the two anchor rows of the Preserving table; it is 10.4 days at 0 °C. Between 0 and −5 °C food is half-frozen and L is 30 days; below −5 °C freshness stops. The code reads L from a table of whole degrees, not from pow().

| Food | k | Safe life at 5 °C |
| --- | --- | --- |
| Meat, cleaned fish | 1.0 | 6 days |
| Uncleaned whole fish | 0.6 | 3.6 days |
| Liver | 0.3 | 1.8 days |
| Kidney | 0.4 | 2.4 days |
| Heart | 0.6 | 3.6 days |
| Tongue | 0.8 | 4.8 days |
| Raw fat | 2.0 | 12 days |
| Cooked meat or fish | 1.5 | 9 days |
| Fresh berries | 1.5 | 9 days |
| Greens, spruce tips | 0.5 | 3 days |
| Mushrooms | 0.3 | 1.8 days |
| Eggs | 3.0 | 18 days |
| Potatoes, roots | 25 | 150 days; 6 months in a 3 °C root cellar |

On a fall day averaging about 14 °C this gives meat 2.3 days and liver 0.7, as the Video checklist requires.

| Where it sits | m |
| --- | --- |
| Hung in shade with moving air (meat pole, raised cache) | 0.8 |
| In a closed chest or cache | 1.0 |
| On the ground | 1.3; above 10 °C, flies add ×1.5 to uncovered fresh meat or fish |
| In sun | 1.5 |
| Inside a heated room | 1.0, with T as the room's temperature: meat in a 20 °C cabin is safe for barely a day |
| Salted | 0.5 |
| In a snow pit | T held at 0 °C while snow remains |

**Freshness stages.**

| Stage | Freshness | Eating it | Other effects |
| --- | --- | --- | --- |
| Fresh | 100–60 | Normal | Can be smoked, dried or frozen |
| Aging | 60–30 | Fine cooked; raw doubles the parasite chance and adds a 10% gut-illness chance | Can still be smoked or dried |
| Turning | 30–10 | Cooked: 30% gut illness, 15% if stewed | Scent point at double strength; cannot be preserved |
| Spoiled | 10–1 | 80% gut illness | "Too far gone": bait, and food for the tamed wolf at 70% of its value |
| Rotten | 0 | Removed | Leaves a scent point for 24 game hours |

**Preserved food uses fixed shelf lives.** Smoked, dried and rendered food ignores the formula while stored under 15 °C and dry, and counts down its own life instead. Below −5 °C none of it counts down. Salt doubles every figure.

| Food | Cool and dry | Stored above 15 °C | Stored damp |
| --- | --- | --- | --- |
| Hot-smoked meat or fish | 30 days | 14 days | Half, then mold |
| Cold-smoked and air-dried | 180 days | 90 days | Half |
| Dried strips | 60 days | 30 days | Half |
| Pemmican | 365 days | 180 days | 180 days |
| Lard | 180 days | 60 days | 180 days |
| Sauerkraut | Until May 1 | — | — |

**Frozen food.** Food below −5 °C is frozen: freshness stops, and thawing above 0 °C restarts it where it stopped. Refreezing does no harm. Frozen meat cannot be cut with the knife; it is chopped with the hatchet (2 game minutes per kg) or thawed by the fire (1 game hour per kg).

**The store view.** Opening any cache, chest or the pack lists food by item: weight, kcal, a freshness bar, and days left from the store's temperature over the 3-day forecast. The top line reads "38 days at Normal, 61 at Lean." Items that turn within 2 days are flagged amber, and the worst one joins the morning prompts: "Moose liver turns tonight."

**Auto-eat.** At the morning meal (06:00–09:00) and the evening meal (18:00–21:00), a player within 30 m of a cache eats from it at the chosen ration. Food closest to turning goes first unless it is pinned "Keep." Away from camp the player eats from the pack by hand (R, quick-eat, eats the selected food; RECONCILE 5).

| Ration | Eats per day | Effect |
| --- | --- | --- |
| Starvation | 500 kcal | Stretches the very last stores |
| Lean | 1,500 kcal | Loses about 0.5 kg on a working day |
| Normal | Yesterday's burn | Holds weight |
| Feast | Yesterday's burn + 1,000 kcal | Gains about 0.4 kg a day; with fat at 20% or more of calories, rebuilds strength fastest (How the player hunts) |

### Preserving in detail

**Drying.** Strips 1 cm thick go on a drying rack (bushcraft: 4 poles, a willow lattice, cordage; 1 game hour; holds 15 kg fresh). A batch needs 2 dry days of sun or wind; each uncovered hour of rain adds 6 hours, and a full day of rain sends it back to the start. Above 15 °C flies lay eggs on it unless a smoky fire burns within 3 m.

**Dried weight.** Dried meat weighs 40% of fresh and keeps every calorie: about 2,900 kcal per kg. An unwatched rack is raided by foxes, jays, magpies, ravens and martens by the hourly rolls in Structures and buildables › Thieves at the rack, when nobody is within 30 m.

**Pemmican.** Dried meat is pounded to powder on a flat stone (20 game minutes per kg), then mixed with warm lard and dried berries at 50 : 45 : 5 by weight. It is packed in hide or salmon-skin bags and gives about 5,600 kcal per kg (half dried meat at 2,875, 45% lard at 9,000, 5% dried berries). Without berries it still keeps, but gives no vitamin C and no sweet bonus.

**Rendering.** Fat is rendered on a Low fire; a Hot fire scorches it and halves the lard. Lard lights lamps (100 g for 6 hours), cures hides, and makes pemmican and akutaq. Lard is also how a lean-meat diet stays above the 6-bolt energy cap in Survival tuning.

**Freezing.** From freeze-up, a cache in shade is a freezer while air stays below −5 °C. Each warm spell above 0 °C restarts the clock, and the messenger warns once: "Above freezing for 2 days: 140 kg of meat in the north cache will start to age." A snow pit or ice cellar on the north side of a hill holds 0 °C into June.

**Build and test.** Data: each food has an entry in `data_maps/item/food.json` with `kcal_per_100g`, `protein_g_per_100g`, `fat_g_per_100g`, `vit_c_mg_per_100g` (10 mg is 1 C-day), `spoil_days_at_5c` by state and its `curve`; the curves live in `data/yearofice/decay/<curve>.json`, and cooking methods and named dishes are recipes under `data/yearofice/recipe/`. Code: `decay.DecayService` (freshness settled on read, catch-up by formula), `food.Cooking` (methods, doneness, dish recognition), `food.Eating` with `food.Stomach`, and `food.Rations` (auto-eat). Debug: `/yoi food info` (the held stack's due, band and clock), `/yoi food age <days>` and `/yoi set calories <kcal>`. *Tests* (`yearofice:decay/…`, `yearofice:food/…`): meat held in a closed chest at 15 °C reaches Turning in 48 ±1 game hours and at 5 °C in 144 ±2; liver turns 3.3 times faster than meat at the same temperature; meat below −5 °C loses nothing in 120 days; catch-up after 10 unloaded days lands in the same 1/40 band as a live run.

## Crop farming

Farming is a once-a-year bet, not a food engine. As in the video, there is one short planting window (late May to mid-June), seeds come up well below 100%, frost closes both ends of the season, and the only seed is a single packet. A first garden that goes well adds about 16,000 kcal, roughly 5 days of food. Its real value is carbs, a little vitamin C, and seed for a bigger garden next year. Every step as a Minecraft block, key and number is in Land and seasons in play › Crop farming in play.

**Crops.** The crate packet holds potato and barley. Other crops come from trapper caches, supply drops and trade, so each seed shows up in a different year.

| Crop | Where the seed comes from | Sow | Days to ripe | Base germination | Yield | Frost | Use |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Potato | Crate packet: 12 seed pieces | May 25 – Jun 15 | 90–110 | 85% (a piece needs 2 eyes) | About 1 kg per plant, 770 kcal/kg | −1 °C kills the leaves; tubers survive until the ground freezes | Carbs; stores 6 months in a root cellar |
| Barley | Crate packet: 200 g of seed for 10 m² (10 `barley_seed`) | May 20 – Jun 10 | 75–90 | 60% from the packet; 80% from well-dried saved seed | About 0.3 kg grain per m², 3,500 kcal/kg | Seedlings shrug off light frost; ripening heads are ruined | Porridge, flatbread, feed for hens; straw for bedding and thatch |
| Wheat | Trapper cache (the video's crop) | May 20 – Jun 5 | 100–115 | 55% | About 0.25 kg per m² | The least hardy; ripens only in a warm year | Bread; a year-2 goal |
| Turnip | Supply drop | May 15 – Jul 1, so two sowings fit | 55–65 | 75% | About 0.4 kg per plant | Hardy; sweeter after frost | Greens and roots; vitamin C |
| Cabbage | Supply drop; started indoors in April | Plant out Jun 1–15 | 80 | 65% as seedlings | About 1.5 kg per head | Hardy down to −4 °C | Sauerkraut, the only food that keeps vitamin C all winter |

&#91;embedded content: crop calendar · 5 crops, 2 frost windows · design target\]

Turnips and barley ripen well clear of the fall frost; a late-sown potato or any wheat runs into it, which is why wheat is a warm-year gamble.

**Germination.** Each seed rolls once when it is sown. The base rate from the table is multiplied by four factors the player can read before sowing:

```latex
P_{germ} = g_{crop} \cdot f_{soil\,temp} \cdot f_{moisture} \cdot f_{depth} \cdot f_{age}
```

- **Soil temperature:** below 5 °C ×0.3, because the seed rots; 5–10 °C ×0.7; 10–25 °C ×1.0. Sowing too early is the most common mistake.
- **Moisture:** dry ground ×0.6 unless watered; waterlogged ground ×0.5.
- **Depth:** the right depth ×1.0; too shallow ×0.7, because birds take it; too deep ×0.6.
- **Seed age:** −10% for each year stored; −30% more if it was kept damp.

Sprouts show 7–14 days after sowing, so empty spots are visible while there is still time to sow again inside the window. On the evening a crop's window closes, the Trail Messenger posts "The planting window for barley has closed." Seed can still be sown after it, but the crouch card reads "unlikely to ripen before frost" and the crop clock decides. When the player crouches (Shift) at a bed, the plant guide shows the expected rate ("barley here: about 45%, soil 7 °C").

**Growing.** Growth counts degree-days above 5 °C, so a cold summer from the weather draw pushes harvest late and closer to the first frost. The midnight sun speeds growth by 20% in June and July.

- **Ground:** beds need cleared, tilled soil. River silt is best, birch-slope loam is good, and bog or permafrost ground fails. Clearing 10 m² of stumps takes about 4 game hours. A wooden or antler hoe from the bushcraft tier cuts it to about 2.3 game hours.
- **Water:** rain counts. After 7 dry days plants wilt and lose 5% yield a day until the player carries water (a pot holds 2 L; a bed needs 10 L).
- **Weeds:** a bed loses 3% yield a week unless weeded. Weeding takes 1 game minute a block, 10 for a 10 m² bed, and can be queued like other jobs, so it never becomes a daily chore.
- **Thieves:** hares and grouse eat seedlings, moose browse cabbage, voles take potatoes, and bears dig them in fall. A brush fence stops hares and grouse; a pole fence stops moose. A tamed wolf sleeping by the garden keeps hares away.
- **Fertility:** fish guts, ash and manure from goats or hens raise yield by up to 30% but do not change germination.

**Frost.** The last spring frost is drawn between May 20 and June 10 and the first fall frost between August 20 and September 15. The Trail Messenger warns the evening before ("Frost likely tonight, −2 °C"). Spruce boughs or a hide over a bed buy +2 °C for one night. Unharvested crops are lost at the first hard freeze of −5 °C.

**Harvest and storage.** Potatoes are dug, cured for a week, then cellared. Barley and wheat are cut, dried on a rack, threshed with a flail and winnowed on a windy day. A root cellar (dug into a hillside, shelter tier) holds 2–4 °C all winter. Potatoes left in a cabin corner freeze to mush at −1 °C.

**Saved seed is the long game.** A potato returns about 8 to 1, and barley about 15 to 1 in a good year. Keeping 20–30% of a harvest as dried seed lets the garden roughly triple each year. Eating the seed means no garden next year. Growth is capped by labor, land, frost and storage, not by a rule, which blocks the video's "bread exploit" while still rewarding a farmer over several years.

| Year | Typical garden | What it adds |
| --- | --- | --- |
| 1 | One packet: 12 potatoes and 10 m² of barley | About 12 kg of potatoes and 2 kg of grain, plus seed |
| 2 | 40 potatoes, 40 m² of grain, maybe turnips | A month of carbs; hens become worth feeding |
| 3+ | A fenced homestead plot, a cold frame or stovehouse adding 3 weeks at each end | Grain and roots for winter, feed for goats and hens |

**Failure is readable.** After harvest the messenger writes a one-line note per bed, such as "Barley: 41% came up (cold soil, sown May 18); frost on Aug 24 took the heads." That way a failed farm like the one in the video teaches the player something instead of just disappearing.

## Mechanics check: hunting

Hunting works end to end after six fixes. A first moose needs a longbow, flint-tipped arrows, a sharp knife and a pack frame, all made in the bushcraft tier from things found in the valley. The check traced every part back to the land and found six places where the blueprint asked for something the player could not yet make.

### What a first moose requires

&#91;embedded content: first moose requirements · 8 materials, 4 crafted items\]

| Part | How the player gets it | Earliest | Gap found, and the fix now in the blueprint |
| --- | --- | --- | --- |
| Stave | Fell a straight birch, split it, dry it 10 game days by the fire | Week 2 | None |
| Bowstring | Twist cordage from fireweed, nettle or willow inner bark | Day 2 | None |
| Arrow shafts | Cut willow shoots and straighten them over the fire | Week 1 | None |
| Fletching | Grouse feathers | Week 1 | Grouse needed a bow to get. **Fix:** grouse can be taken with a thrown stick or a neck snare on a low branch, and unfletched arrows fly true to 10 m |
| Broadheads | Knap flint from gravel bars; bone after the first big game; iron later | Week 1 | Only iron broadheads existed. **Fix:** flint broadheads in the bushcraft tier |
| Glue | Spruce pitch melted with charcoal | Week 1 | No glue recipe. **Fix:** pitch glue added, from the existing spruce gum |
| Big-game bow | Longbow from the stave and string | Week 2 | The heavy bow sat in the hide tier, which opens only after a first big kill. **Fix:** the longbow moves to bushcraft; a sinew-backed bow becomes the hide-tier upgrade |
| Edge upkeep | Hone the crate knife on a flat river stone | Day 1 | The whetstone was iron tier, but the knife dulls from day 1. **Fix:** a river-stone hone in bushcraft |
| Hauling | Pack frame from spruce poles and cordage | Week 1 | The sled sits in the hide tier. **Fix:** a pack frame in bushcraft; a first moose comes home in 8–9 trips (Field care › The haul) or gets a cache at the kill site |

**The build sheet.** Every part of the first-moose kit has an exact recipe in `data/yearofice/recipe/<id>.json`. All of it comes from the start site's surroundings, so the checks in Start site (birch, gravel bars, a game trail) guarantee it can be made.

| Item | Inputs | Station and tools | Time | Notes |
| --- | --- | --- | --- | --- |
| Longbow stave | One quarter of a straight birch 15–20 cm thick | Hatchet, a wooden wedge | 1 game hour to fell and split, then 10 game days within 3 m of a burning fire | A stave left in the fire burns, as in the video; the messenger warns once |
| Bowstring | 2 m of plant-fiber cordage, triple-twisted and rubbed with pitch | Hands | 20 game minutes | Wet string stretches: −15% arrow speed until dry |
| Longbow | Seasoned stave, bowstring | Knife, workbench | 3 game hours of tillering | A tillering minigame: shave, test-draw, shave again. A tiller off by more than 10% breaks the bow on its first full draw |
| Pitch glue | 50 g spruce gum, 10 g charcoal, a pinch of fiber | Fire | 10 game minutes | Enough for 10 arrows |
| Flint broadhead | 1 flint nodule | Hammerstone, and an antler tine for pressure flaking (a pointed stone works at half speed) | 10 game minutes | Knapping minigame; 30% of cores shatter at practice 0, 5% at full practice |
| Broadhead arrow | Willow shaft, 3 feathers, flint head, pitch glue, 30 cm of sinew or fine cordage | Knife, fire | 25 game minutes; 15 with practice | One arrow per craft |
| River-stone hone | A flat fine-grained stone from the river | Hands | 2 game minutes to find | Honing takes 5 game minutes |
| Pack frame | 3 spruce poles of 1.2 m, 4 crossbars, 8 m cordage, 2 bark or rawhide straps | Knife, hatchet | 2 game hours | Carries 40 kg a trip |

**Knife edge.** The knife's edge is the `yearofice:edge` component, 0 to 120; its condition is vanilla durability (RECONCILE 79). It loses 1 per game hour of cutting work; butchering a moose costs about 15. Below 40 every cutting job takes 50% longer; the river-stone hone restores it to 70, a whetstone to 100, and a leather strop on an edge over 90 makes it Keen, 120 for the next 100 uses (Items).

**The hunt kit card.** Once the player holds a longbow, the messenger notebook shows a kit card with six checks: longbow, at least 6 broadhead arrows, knife edge above 60, pack frame, 10 m of cordage, and a meat pole or cache at camp. It is advice, not a gate; a hunter can leave with half of it and learn why the rest mattered.

**When the first moose is realistic.** The parts exist by week 2, but the strength gate (How the player hunts) usually pushes the first moose to weeks 3–6. The 38 crackers run out in about 10 days and weight falls; a default 183 cm body under about 71 kg cannot fully draw the longbow. A run of fish, fat and Feast rations brings the draw back, which is the video's "small game is the road to big game."

**Build and test.** *Test:* over 50 seeds, a scripted player builds the whole kit from land materials by game day 14, and every input traces to a node within 1,500 m of the start site.

### How the player hunts

A hunt is a chain of twelve actions, and each can fail in a way the player can learn from.

| Action | How it works | Fails when |
| --- | --- | --- |
| Read sign | Look at tracks, scat, beds and feathers; the messenger names the animal and how fresh the sign is | Rain or new snow wipes sign out |
| Stalk | Crouch: 40% speed, noise carries 3 m; the messenger shows wind direction and where scent drifts | A dry stick underfoot snaps, heard 20 m away |
| Call | Birch-bark moose call in the rut (Sep–Oct); a squeak brings hares to a stop | Calling outside the rut, or too often, so the bull hangs up out of range |
| Ambush | Sit still at a trail, river crossing or brush blind; player noise drops to zero | Scent drifts across the trail |
| Draw and aim | Full draw in 1 s; holding costs energy; sway grows with cold, low energy and a racing heart after sprinting | Holding past 6 s, or shaking hands below 30 warmth |
| Shoot | Arrows fly about 50 m/s with drop; hit zones are vital, gut, leg or miss | Branches deflect; past 25 m a longbow loses killing power |
| Recover arrows | Walk to them; 70% can be reused | Lost in snow or water |
| Track | Follow blood and prints; blood fades over 2 game hours, prints last in mud and snow | Following too soon pushes a wounded animal to run again |
| Field-dress | Gut at the kill with the knife; leaves a bear scent point | Doing it near camp |
| Quarter and haul | Pack frame carries 40 kg a trip; a sled carries 120 kg on snow | Warm weather spoils meat during a long haul |
| Cache | Meat pole or raised cache out of a bear's reach | A ground cache gets raided |
| Process hide | Scrape and dry for rawhide in 2 days; smoke-tan for leather in 6 | Left wet, it rots |

**Strength.** Strength comes from Condition and body weight, as Gameplay ideas fixes, by this rule:

```latex
Strength = Condition \cdot \mathrm{clamp}\left(\frac{BMI - 17}{6},\ 0,\ 1\right)
```

The default body (BMI 23, Condition 100) starts at 100. The light bow needs 40, the longbow 70 and the sinew-backed bow 85; below that, full draw caps at 60%. An injury that drops Condition to 80 raises the longbow's weight floor from about 71 kg to about 75 kg for the default body.

**Draw and hold.** Draw takes the bow's ready time (Weapons and arrowheads); below 3 bolts it takes 50% longer. Holding costs 0.02 bolts per second. At 6 s the arm shakes: the player must shoot with triple sway or let down (tap right mouse).

**Sway.** The aim point wanders in a circle whose radius at 20 m is:

```latex
r_{cm} = 4 \cdot (1 + c_{cold} + c_{energy} + c_{heart} + c_{hold} + c_{other}) \cdot (1 - 0.4 \cdot practice)
```

- c\_cold = 1 below 30 warmth (shaking hands).
- c\_energy = 0.5 below 3 bolts.
- c\_heart = 1.5 just after a sprint, falling to 0 over 20 s.
- c\_hold = 0 for the first 4 s of a hold, then 0.5 per second.
- c\_other = 0.25 each for Spirits below 30, mosquitoes and a wet bowstring; 1.0 at a strength-capped draw.

Practice runs from 0 to 1 over the first 200 shots, including shots at a hide target at camp, which cost no arrows.

**Arrow flight.** Arrows fly at the bow's speed under 9.8 m/s² gravity, with no sight pins. The player learns holdover: at 20 m the arrow tip sits about 0.8 m above where the longbow arrow lands. A partial draw cuts speed by the square root of the draw fraction.

| Bow | Arrow speed | Drop at 10 m | 15 m | 20 m | 25 m | 35 m |
| --- | --- | --- | --- | --- | --- | --- |
| Light bow | 42 m/s | 0.28 m | 0.63 m | — | — | — |
| Longbow | 50 m/s | 0.20 m | 0.44 m | 0.78 m | 1.23 m | — |
| Longbow at 60% draw | 39 m/s | 0.32 m | 0.73 m | 1.29 m | 2.01 m | — |
| Sinew-backed bow | 58 m/s | 0.15 m | 0.33 m | 0.58 m | 0.91 m | 1.78 m |

**Noise by surface.** The base noise radii from Food systems (sprint 30 m, walk 10 m, crouch 3 m, stick snap 20 m) are multiplied by the surface. Each species then multiplies again by its hearing factor (Senses by species).

| Surface | Noise × | Note |
| --- | --- | --- |
| Moss, wet ground, powder snow | 0.5 | The quiet way through |
| Bare soil, worn trail | 1.0 |  |
| Gravel bar, shallow water | 1.5 | Splashing carries |
| Dry leaves, September–October | 2.0 | Fall stalking is loud |
| Crusted snow below −15 °C | 2.5 | Every step squeaks |

On the forest floor, a stick snaps on 5% of walking metres and 1% of crouching metres, never on trails or moss. Crouched, dry sticks are visible on the ground and can be stepped over. Wind over 4 m/s halves all noise, rain cuts it to 0.6, and a river within 30 m halves it.

**Recovering arrows.** The reuse shares in Weapons and arrowheads are for arrows that hit. A miss into turf or wood is found and reusable 95% of the time; into snow it is found only half the time unless the player saw where it went, and into water it is lost unless it drifts to a snag.

**Build and test.** Code: `sim.hunt.Aim` (strength, sway) and `sim.hunt.Ballistics` (flight, drop), pure rules in the `hunt` package's layer (Food and hunting); debug `/yoi hunt aim` prints the live sway terms, and `[hunt]` in `yearofice-common.toml` holds `sway_scale` and `strength_gate`. *Tests:* a full-draw longbow arrow lands 1.23 ±0.05 m below the line at 25 m; a default body at 70 kg and Condition 100 is capped at 60% draw on the longbow and is not capped at 72 kg.

### How animals act

Every species runs the same small set of states; species differ in schedule, senses, and what tips them from fleeing to charging. Scent, bait and calls can also pull an animal out of its routine toward the player. Predators give up a chase after 150 m or at a fire, and every charge is announced first by a sound: a bear's huff and jaw-pop, a moose's grunt, a wolf's growl.

&#91;embedded content: animal behavior states · shared by every species\]

| Species | Active | Best sense | Meets the player | Season changes | How it is taken |
| --- | --- | --- | --- | --- | --- |
| Snowshoe hare | Dawn, dusk, night | Hearing | Freezes, then zigzags to cover | White coat Oct–Apr; numbers boom and crash over years | Trail snare; blunt arrow |
| Red squirrel | Day | Sight | Scolds (alerts game within 50 m), runs up a trunk | Caches cones in fall | Pole snare; blunt arrow |
| Spruce grouse | Day | Sight | Freezes and lets the player close; flushes at 3 m | Roosts in snow in winter | Thrown stick, neck snare, blunt arrow |
| Beaver | Dusk, night | Hearing | Tail-slap alarm, then dives | Under the ice in its lodge Nov–Apr | Wire snare at the lodge or dam; arrow at dusk |
| Moose | Dawn, dusk; beds in willow at midday | Hearing, smell | Usually walks off; a cow with a calf (May–Aug) or a rutting bull (Sep–Oct) charges | Bulls answer calls in the rut; herds yard up in deep snow | Downwind stalk or rut calling; longbow under 25 m |
| Caribou | Day, in a herd | Sight | The whole herd bolts together | Passes through in September only | Ambush at a river crossing |
| Wolf | Night, as a pack | All three | Avoids a healthy player; in winter tests one who is weak or carrying meat | Follows the moose; bolder in deep snow | Longbow or wire snare on their trails; fire and noise drive a pack off; pups can be tamed |
| Grizzly | Day, dusk | Smell | Investigates scent points; charges if surprised close or guarding a carcass | Feeds hard Aug–Oct; dens Nov–Apr; learns which camps leave food out | Avoid; spray; a longbow kill is possible but risky |

**Awareness drives every state change.** Each animal keeps an awareness score from 0 to 100 toward the player and toward each nearby predator, filled by the senses (Senses by species). At 30 it turns Alert; at 70 it takes its species' response; at 100 it knows exactly where the player is.

| State | Entered when | What it does | Leaves when |
| --- | --- | --- | --- |
| Routine | Default | Acts on its strongest drive: feed, drink, travel, rest (Drives and daily schedules) | Awareness reaches 30 |
| Alert | Awareness 30 or more | Stops, shows its tell, faces the stimulus for 5–20 s | Reaches 70: species response. Drops under 20: Routine, wary for 10 game minutes |
| Freeze | Hare, grouse, ptarmigan or porcupine at 30 or more | Holds still | Player inside its flush distance: Flee |
| Flee | Prey at 70, or any animal hit | Runs from the stimulus toward cover, for its flee distance | Distance reached: hides, then Routine after 30–60 game minutes |
| Investigate | Bears, wolves, foxes, wolverines and ravens drawn by scent or noise; a curious marten | Walks toward the source, stops to sniff, circles to get downwind | Finds food: Feed. Sees the player within 50 m: Alert |
| Lured | A call, bait or scent point within its lure range | Moves toward it on a curving path, checking the wind | Spooked: Flee. Arrives: Feed, or Warn if it finds a rival |
| Stalk | A predator hunting prey or testing a weak player | Closes from downwind through cover at crouch speed | Within strike distance: Chase |
| Chase | A predator's strike, or a charge that keeps going | Sprints | 150 m from the chase's start, a fire within 8 m, a lit torch, or the prey escapes |
| Warn | Bear, moose, beaver or wolf about to charge | Sound and posture for 1.5–3 s: huff and jaw-pop, grunt with ears back, tail slap, growl | Player backs away: Alert. Player holds or closes: Charge |
| Charge | After Warn | Rushes; a bluff stops 3–8 m short | Bluff ends: Warn again. Contact: Attack |
| Attack | Contact | Damage by species, ends by the rules in Danger in detail | Player still and protected (grizzly), struck (wolf), or sprayed |
| Feed on kill | At a carcass | Eats; a bear covers it with dirt and beds within 30 m | Carcass gone, or driven off |
| Wounded | Hit and alive | Runs and beds by hit zone (The hit-zone model) | Dies, or heals in 10–20 days from a leg hit |

A surprise never skips Warn; it only shortens it to 1 s. That keeps the rule above that every charge is announced first.

**Species responses at awareness 70.**

| Species | Response | Flee distance | Charges when | Share of charges that are bluffs |
| --- | --- | --- | --- | --- |
| Snowshoe hare | Zigzags to cover | 20–40 m | Never | — |
| Grouse, ptarmigan | Freezes; flushes at 3 m | 30 m flight, to a tree | Never | — |
| Moose | Walks off, then trots | 200–400 m | Cow with calf within 30 m (May–Aug); rutting bull within 25 m that sees the player standing; wounded and pressed within 15 m; a deep-snow moose met on a packed trail within 10 m | 60% |
| Caribou | The herd bolts | 500 m | Never | — |
| Dall sheep | Climbs | 100 m up the cliff | Never | — |
| Wild wolf | Retreats from a healthy player, regroups | 100–200 m | A test (below) | 70% on the first rush |
| Black bear | Runs or climbs | 100 m | Defending cubs within 20 m | 90% |
| Grizzly | Stands and sniffs, then leaves or investigates | 200 m | Surprised within 25 m; at a carcass within 50 m; a sow with cubs within 40 m | 70% |
| Beaver | Tail slap, dives | Under water | Player in the water within 5 m of the lodge in May–June | 80%, then bites |
| Porcupine | Turns its back, raises quills | — | Never; whatever strikes it is quilled | — |
| Lynx, fox, marten, otter | Slips away | 50–100 m | Never | — |
| Wolverine | Holds its ground at food | — | Cornered at food within 5 m | 50% |

**Wolves test a weak player.** From November to March, a pack of 3 or more tests the player when two of these hold: Condition under 40, energy under 3 bolts, carrying fresh meat, snow over 40 cm, night. A test is 2–5 game minutes of circling at 30–50 m, then one wolf rushes to 5 m. A strike, a torch, or a fire within 8 m ends it; a healthy player is never tested.

**Habituation.** A hare, squirrel, grouse, ptarmigan, porcupine, muskrat, beaver, fox or raven that sees the player on 3 separate days within 60 m of the campfire, unharmed, becomes Habituated. Its Alert threshold rises from 30 to 80 and its flee distance drops to 5 m, and the messenger offers "Name it." Harming it, or the tamed wolf chasing it, clears the flag.

**Camp bears.** Each time a bear finds food at a camp, that camp's nightly bear-visit chance rises 15 points, up to 60%. It falls 10 points a week while nothing is found. This is the rule behind "bears learn" in The open world.

**Build and test.** Code: the shared state machine of Minecraft development › Entities and AI (`sim.animal.Awareness`, `ChargeGate`, Brain activities in `ecosystem`); each species' thresholds, flee distances and charge rules sit in a `behavior` block in `data/yearofice/species/<id>.json`. *Tests:* across 1,000 simulated encounters every Charge follows a Warn of at least 1 s; no predator chases past 150 m; a healthy player at full Condition is never tested by wolves.

## Animals in depth

Animals act from needs, not scripts: each one weighs four drives against the time of day, the weather and the season, and the land's animals act on each other. That gives the player signs to read and chances to exploit.

### New species

Twelve species join the eight above, so every habitat and season has something to find, eat, fear or trap.

| Species | Where | Per map | What it means for the player |
| --- | --- | --- | --- |
| Black bear | Forest, berry slopes | 3–5 | Commoner and smaller than the grizzly; climbs trees; 50,000 kcal and fat |
| Lynx | Spruce thickets | 2–4 | Rarely seen; follows the hare cycle; prized pelt |
| Red fox | Edges, gravel bars | 4–6 | Steals from snares; pelt |
| Wolverine | High country | 0–1 | Breaks into caches and robs traplines; its fur sheds frost, the best parka ruff |
| Porcupine | Forest | 6–10 | Slow and easy to take with a stick: food for a bad week. Quills injure the wolf |
| Muskrat | Bog ponds | 10–20 | Small meat and a warm pelt; trapped at its lodges in winter |
| River otter | Rivers | 2–3 | Competes at the best fishing holes; pelt |
| Marten | Old spruce | 4–8 | The classic trapline catch; warm trim |
| Willow ptarmigan | Willow flats, tundra | Flocks of 10–30 | White in winter; winter bird food by snare or thrown stick |
| Ducks and geese | Lakes, marshes | Migrating flocks | Only in May and September; eggs in June |
| Dall sheep | North-range cliffs | 6–12 | A hard climb and a long shot; 30,000 kcal |
| Raven | Everywhere | Many | Not food. Circles kills and gut piles: a sign of meat, and of bears coming |

**Each new species has one rule that makes it matter.** Home ranges are radii around a home point drawn at world creation; yields are in Food systems.

| Species | Home range | Daily pattern | The rule that makes it matter | Taken by |
| --- | --- | --- | --- | --- |
| Black bear | 3 km | Feeds dawn and dusk; dens late Oct–Apr under roots or a windfall | Climbs: a tree is no escape from a black bear. Spring bears leave the den lean, with 15% of fall fat | Longbow from a tree stand over bait |
| Lynx | 2 km, centered on the best hare cover | Hunts dusk and night | Its number follows last year's hares; it never approaches the player | Cubby set with castor lure |
| Red fox | 1.5 km | Dusk, night; hunts in daylight in winter | Runs trap to trap and takes any catch left over 48 h | Snare or deadfall at a scent post; squeak call |
| Wolverine | 15 km, crossing regions | Any time; visits the core every 10–20 days in winter | Breaks a ground cache in 1 game hour and a chest in 2; climbs, so a meat pole does not stop it. A wolverine ruff keeps the face safe from frostbite to −40 °C | Deadfall or snare at a raided cache |
| Porcupine | 0.5 km | Night; in winter sits in one spruce for days | Taken with a stick in three blows to the nose. A wolf that bites one takes 10–40 quills: −15 Condition and no hunting until pulled (1 game minute per 5 quills) | Stick, blunt arrow |
| Muskrat | 0.3 km | Dusk, night | Builds push-ups of cattail on the ice in winter, each a trap site | Light-wire lodge snare at a push-up |
| River otter | 10 km of river | Day and dusk | Takes one fish from a set line or net in its range on 20% of nights; fish stocks at its holes drop 2% a week | Snare at its slide |
| Marten | 1 km in old spruce | Night | Curious: comes to 10 m of a still, crouched player on 30% of dusks; steals cubby bait without a catch 15% of the time | Cubby set, deadfall |
| Willow ptarmigan | 2 km of willow flats | Day; roosts in snow below −20 °C | White Oct–Apr. Snow roosts flush at 2 m in a single burst. A flock flushes together and lands 100–300 m away | Thrown stick, trail snare in willow, noose pole |
| Ducks and geese | Lakes and marshes | Day | Present only May 5–25 and September 5–30, in flocks of 8–40. Two to four pairs stay to nest: 4–8 eggs in June, and a raided nest is abandoned | Decoys and a blind; eggs by hand |
| Dall sheep | 3 km of north-range cliffs | Day; rams and ewes apart until November | Watches downhill, so it is stalked from above. A carcass on a ledge can fall 50 m and lose 30% of its meat | Sinew-backed bow at 30–35 m; mineral lick in June–July |
| Raven | Everywhere; 2–6 in view | Day | Circles any carcass or gut pile within 2 game hours, seen from 1 km and heard at 300 m. In winter it follows wolves and the player | Not taken; read as sign |

**Ravens tell the truth.** Ravens circling a spot the player did not make mean a predator kill 70% of the time and a winter-killed moose 10% of the time. The other 20% is a gut pile or old carrion. Ravens over a fresh spot also mean bears or wolves are likely within a day.

**Build and test.** Each species gets a file `data/yearofice/species/<id>.json` with `behavior`, `senses`, `sign` and `population_model` blocks defined in the sections below, its hit zones in `data/yearofice/hitzone/<id>.json` and its parts in `data/yearofice/loot_table/butchering/<id>.json` (RECONCILE 6, 41, 81). *Test:* every species in the Wildlife table has a complete entry, and the M6 data check fails if any block is missing.

### Drives and daily schedules

Each animal scores four drives every game hour (hunger, thirst, rest, safety), plus a seasonal one (the rut, nesting, denning), and acts on the strongest. Daily routines emerge from that: a moose feeds in willow at dawn, beds in shade at midday, and drinks at dusk. Weather bends them:

- Rain: hares and grouse hunker in cover; moose keep feeding.
- Hot, buggy days: moose stand in ponds to escape mosquitoes.
- Cold snaps: moose yard up in thick spruce; ptarmigan burrow into snow.
- Wind: noise is masked, so animals turn skittish and the player can close distance.

**Scoring drives.** Each drive is a value from 0 to 1 that rises each game hour at a species rate and falls while the animal acts on it. The animal picks the drive with the highest utility; it switches only when another beats the current one by 0.1, so it does not dither.

```latex
U_d = w_d(\text{species, hour, season, weather}) \cdot D_d \qquad \text{act on } \arg\max_d U_d
```

Safety overrides everything once it reaches 0.7. Safety comes from awareness, a predator within its sense range, and distance from cover. Each drive maps to a point type in the home range: hunger to forage spots, thirst to water access, rest to bedding, the seasonal drive to the rut ground, nest or den. Travel between them follows game trails, so the routine emerges from the drives rather than a script.

| Drive | Moose: rises per game hour | Falls per game hour while acting |
| --- | --- | --- |
| Hunger | 0.08 | 0.25 while feeding |
| Thirst | 0.04 | 1.0 at water |
| Rest | 0.06 awake | 0.20 while bedded |
| Seasonal (rut) | 0.10 in the window | — |

Every species has its own rates in `data/yearofice/species/<id>.json`; the moose row is the reference.

**Default day by species.** The weights w\_d tilt each species toward a shape of day.

| Species | Dawn | Day | Dusk | Night | Winter |
| --- | --- | --- | --- | --- | --- |
| Moose | Feeds in willow | Beds in shade 4–6 h | Drinks, then feeds | Feeds and beds in turns | Yards in thick spruce; feeds 4 h a day in cold snaps |
| Caribou | Grazes | Grazes on the move | Grazes | Rests | Gone |
| Snowshoe hare | Feeds | Rests in a form under cover | Feeds | Feeds | Same, in a white coat |
| Red squirrel | Feeds, caches | Feeds, scolds | Rests | Nest | Stays in midden tunnels below −30 °C |
| Grouse | Feeds on the ground | Feeds in spruce; dust-bathes in summer | Roosts | Roosts | Roosts in snow from 15:00 |
| Beaver | Returns to the lodge | Lodge | Cuts trees, repairs the dam | Works | Under the ice, eats from its food raft |
| Black bear, grizzly | Feeds or fishes | Beds | Feeds; grizzlies work the falls in August | Travels | Dens |
| Wolf | Ends the hunt, rests | Rests at the rendezvous site | Howls, starts the hunt | Hunts, travels 20–40 km | Hunts by day too in deep snow |
| Lynx, marten, fox | Rests | Rests | Hunts | Hunts | Fox also hunts by day |

**Seasonal drives.** Dates are fixed by the calendar, and the messenger announces each one once as a seasonal alert.

| Drive | Species | Window | Effect | Messenger line |
| --- | --- | --- | --- | --- |
| Calving | Moose | May 20 – Jun 15 | Cows hide in willow islands; calves at heel until August | "Moose calving: give willow islands room." |
| Nesting | Grouse, ptarmigan, ducks | May 15 – Jun 30 | Hens sit tight and flush at 1 m with a broken-wing display | — |
| Denning | Wolves | Apr 20 – Jul 15 | Pack returns to the den at dawn and dusk; the taming window | "Wolves howling near the den at dusk." |
| Hyperphagia | Bears | Aug 1 – Oct 20 | Hunger weight ×2; feeds up to 20 h a day | "Bears feeding heavily." |
| Caching | Red squirrel | Aug 15 – Oct 15 | Cuts cones; middens grow | — |
| Migration | Caribou | Sep 1–30 | Herds cross the tundra and river crossings | "Caribou on the move to the north-east." |
| Migration | Ducks, geese | May 5–25, Sep 5–30 | Flocks on lakes | "Geese landing on the big lake." |
| Rut | Moose | Sep 10 – Oct 10 | Bulls stop feeding (hunger weight 0.3), travel, answer calls | "Moose rut has begun: bulls aggressive." |
| Denning | Bears | Oct 20 – Apr 20 | Goes to its den and sleeps | "Bears are denning." |
| Rut | Dall sheep | Nov 10 – Dec 10 | Rams clash; the sound carries 1 km | — |
| Dispersal | Wolf yearlings | Nov 1 – Feb 28 | Lone yearlings wander (Taming a wolf) | — |

**Weather bends the day.** These are exact multipliers on the existing rules.

- **Rain:** hare and grouse hunger weight ×0.4, matching the species entry's `"rain": 0.4`; they stay in cover. Moose keep feeding.
- **Hot and buggy** (above 20 °C in mosquito season): each hour from 11:00 to 17:00 a moose has a 40% chance to stand in a pond 0.5–1.5 m deep, a fixed ambush target.
- **Cold snap** (below −30 °C): moose are active 4 hours a day; grouse and ptarmigan roost in snow from 15:00; hares feed only at night.
- **Wind over 6 m/s:** prey Alert thresholds fall by 10, so animals are skittish, while player noise is halved.
- **Snow over 80 cm:** moose keep to packed trails, and wolves count as bolder for the test rule.
- **Full moon on snow:** hares feed 30% less; lynx hunt 30% more.

**Build and test.** Code: `sim.animal.Drives` (pure scoring) feeding the Brain's `routine` activity in the Active tier; Nearby bodies and records follow their trail schedule once per game minute, and the Abstract tier uses only population counts. *Tests:* over 30 simulated July days, a moose is bedded for 55–70% of the hours from 11:00 to 16:00 and visits water at dusk on at least 80% of days; no animal changes action more than 6 times in a game hour.

### Senses by species

| Species | Sees a moving player at | Hears walking at | Smells downwind at | Tell before it bolts |
| --- | --- | --- | --- | --- |
| Moose | 60 m | 40 m | 150 m | Ears swivel; it stops chewing |
| Caribou | 150 m | 30 m | 100 m | One head comes up, then the herd runs |
| Dall sheep | 300 m | 40 m | 100 m | The band looks down, then climbs |
| Snowshoe hare | 20 m | 15 m | 20 m | Freezes, ears up |
| Grouse, ptarmigan | 30 m | 10 m | — | Neck stretches, soft clucking |
| Grizzly | 50 m | 60 m | 1,500 m | Stands and sniffs |
| Black bear | 50 m | 60 m | 1,000 m | Stands and huffs |
| Wolf | 100 m | 80 m | 400 m | Stares; the pack regroups |
| Lynx | 80 m | 40 m | 60 m | Crouches and vanishes |

A still player is seen at half these ranges, a crouching one at a third. Scent drifts in a cone downwind; on calm days, morning thermals carry it uphill and evening air carries it downhill, so the time of day decides which slope to hunt from.

**The rest of the cast.**

| Species | Sees a moving player at | Hears walking at | Smells downwind at | Tell before it bolts |
| --- | --- | --- | --- | --- |
| Beaver | 30 m | 40 m | 60 m | Tail slap |
| Red squirrel | 40 m | 20 m | — | Chatters; the alarm alerts game within 50 m |
| Porcupine | 10 m | 10 m | 30 m | Quills rise |
| Red fox | 60 m | 60 m | 200 m | Sits up, ears forward |
| Marten | 40 m | 30 m | 80 m | Stands up on the branch |
| River otter | 50 m | 40 m | 100 m | Snorts and dives |
| Wolverine | 40 m | 50 m | 1,000 m | Rears on its hind legs |
| Muskrat | 20 m | 20 m | — | Dives with a splash |
| Ducks and geese | 150 m | 50 m | — | Heads up; the flock swims off, then lifts |
| Raven | 200 m | 60 m | — | Calls, and others gather |

**Awareness.** Each sense gives a stimulus s from 0 to 1, where s = 1 − d ÷ R and R is that sense's effective range after the modifiers below. Awareness follows the build rule in Minecraft development › The shared state machine: each sense adds its gain (sight 40, hearing 25, scent 15 a second at contact) times s and the posture factor (1.5 running, 0.33 crouched), and awareness decays 2 points a second with no sense firing, 5 in cover.

```latex
\frac{dA}{dt} = \sum_{s} g_s \cdot s_s \cdot m_s - 2 \cdot [\text{no sense}] \qquad g_{sight} = 40,\ g_{hearing} = 25,\ g_{scent} = 15
```

Scent is the exception: a player's plume reaching an animal sets awareness to at least 60 at once ("the moose winds you"), then the normal rise continues.

**Sight.** Prey see 300° around them and predators 200°, so a player directly behind an animal is unseen. The table's moving range is multiplied by posture (still ×0.5, crouching ×0.33, both ×0.17), light, cover and skyline:

- Dusk and dawn ×0.7; night ×0.3, except wolves and lynx at ×0.7.
- Fog, snowfall or blizzard: capped at the visibility (20 m in a blizzard).
- Each dense bush or tree trunk in the line of sight ×0.5.
- A player outlined against open sky on a ridge ×1.5.

**Hearing.** Each species has a hearing factor equal to its "hears walking at" range divided by 10 m. A moose has factor 4, so it hears a crouching step at 12 m and a snapped stick at 80 m. Player noise is the base radius in Food systems × the surface (How the player hunts) × this factor; wind over 4 m/s halves it, rain cuts it to 0.6, and a river within 30 m halves it.

**Smell.** The plume is a cone with a 15° half-angle downwind of the player. Its reach is the species' smell range multiplied by:

| Player state | Plume × |
| --- | --- |
| Normal | 1.0 |
| Carrying fresh meat or fish | 1.5; ×3 for bears and wolverines |
| Sweating after an hour of hard work | 1.2 |
| Clothes smoked by a campfire in the last 6 game hours | 0.7 |
| On a tree stand | Does not reach the ground within 40 m |

Steady wind from 1 to 6 m/s gives the full reach; over 8 m/s the plume breaks up and reaches half as far. Under 1 m/s the existing thermal rule holds: uphill 06:00–11:00, downhill 17:00–22:00, and in between the direction swirls every 2 game minutes, which is the dangerous hour to stalk.

**Ground scent.** The player's own track carries scent for 2 game hours. Moose and hares crossing it raise awareness by 30; wolves and bears follow it.

**Memory.** An animal that fled from the player keeps 25% longer sense ranges within 100 m of that spot for one game day. One that was shot at and missed keeps 50% longer ranges for 3 game days: the educated bull.

**The messenger's wind view** draws the plume on the map from the player or any waypoint, with the hour-by-hour thermal forecast: "Calm until noon: scent drifting uphill to the north-east, about 150 m."

**Build and test.** Code: the sight, hearing and scent sensors of Minecraft development › Brain, reading the Scent and sound field; each species' ranges, hearing factor and field of view sit in a `senses` block in `data/yearofice/species/<id>.json`. *Tests:* a still, crouched player 18 m crosswind of a feeding moose stays undetected for 60 s; the same player walking is detected within 2 s; a player 140 m directly upwind triggers Alert within 1 s.

### Who hunts whom

| Predator or scavenger | Takes | Sign the player can read | What it means |
| --- | --- | --- | --- |
| Wolves | Moose, caribou, beaver, hares | Tracks in a single line, howls at dusk, ravens over a kill | A kill to scavenge; moose shift away |
| Grizzly | Salmon at falls, berries, moose calves in June, carrion | Torn logs, dug-up ground, berry-filled scat | Avoid the falls at dusk in August |
| Black bear | Berries, carrion | Claw marks on birch bark | Same, with less danger |
| Lynx | Hares | Big round tracks, hare kills in snow | Lynx numbers lag the hares by a year |
| Red fox | Hares, grouse, voles, snared catches | Tracks running trap to trap | Check snares daily |
| Wolverine | Carcasses, caches, trapline catches | A broken cache, wide loping tracks | Raise caches on legs wrapped in metal |
| Ravens | Anything dead | Circling and calling | Meat nearby, and bears on the way |
| Otter | Fish | Slides down banks | Competition at a fishing hole |

**Predation off-screen.** In the Abstract tier, predators take prey from each region's counts once a game day. Each pair has a kill rate per 30 game days at full prey density, scaled by how much prey is left:

```latex
kills = a \cdot \frac{1.3 \cdot N/K}{N/K + 0.3}
```

| Predator | Kills per 30 game days in one region, at full prey density | Season |
| --- | --- | --- |
| Wolf pack (whole pack) | 0.15 moose; 12 hares; 1 beaver (May–Oct); 4 caribou where a herd passes in September | Moose: 0.05 in May–Oct, mostly calves |
| Lynx | 5 hares, 2 grouse or ptarmigan | All year |
| Red fox | 4 hares, 3 grouse or ptarmigan | All year |
| Grizzly | 0.3 moose calves | June only |
| Black bear | 0.1 moose calves | June only |
| Marten | 2 squirrels, 1 grouse | All year |
| Wolverine | 1 hare; otherwise carrion | All year |
| Otter | Fish only: 2% of a fishing spot's stock a week | Open water |

These are starting values; the balance simulator tunes them to the population targets in Populations.

**Hunts in plain view.** On a game day when the player spends 4 or more hours outdoors, there is a 10% chance (25% in winter) that one of the day's predator kills is staged within 300 m of the player in daylight. It comes from the region's kill budget, so nothing extra dies.

| Staged hunt | What the player sees | Player choices |
| --- | --- | --- |
| Lynx and hare | A lynx crouched beside a hare trail, then three bounds; the hare escapes 40% of the time | Shoot the lynx for its pelt, or let it eat |
| Wolves and moose | In snow over 60 cm, 3–5 wolves hold a moose against a river bank for 10–30 minutes; the moose stands them off half the time | Wait for the pack to eat and leave (6–12 game hours, and they return), or drive them off with fire |
| Grizzly at the falls | In August, a salmon caught every 5–15 game minutes; half-eaten fish wash to the snags below | Collect the fish downstream, out of the bear's sight |
| Grizzly and calf | In June, a grizzly working a willow island and a cow moose charging it | Stay clear; both are dangerous |
| Marten and squirrel | A chase up and around a spruce trunk | A pelt for a quick arrow |
| Fox mousing | Pounces into snow on a still day | Shows the fox's route, which often runs past the player's snares |

**Named animals die on-screen.** When Abstract predation would take a named, habituated animal, the kill is cancelled and turned into a staged attempt the next time the player is within 100 m of it. The player always sees the wolverine come for Peeper and decides whether to step in.

**Carcass succession.** Any carcass, the player's or a predator's, draws scavengers in this order:

| Time after death | Who comes | What it eats |
| --- | --- | --- |
| 0–2 game hours | Ravens, 2–6 | 1 kg of exposed meat per raven per day; gut piles first |
| 6–12 hours, at night | Fox, marten | 0.5 kg each a night |
| 12–36 hours, Nov–Apr | Wolves if the pack is within 5 km; a wolverine if one is in the region | 8 kg per wolf per visit; a wolverine drags off 5 kg and caches it |
| Within 24 hours, Apr–Oct | Bears, by the scent rules | 15 kg a day; covers the rest and beds within 30 m |

Left unguarded for 3 days in September, a moose loses about 60 kg to ravens and one bear. Ways to keep it are in Field care.

**Prey respond to predators.** After a wolf kill in a region, moose sightings there drop to 60% for 14 game days and moose bed in thicker cover. In a lynx-high year, hares freeze longer and flush at half distance.

**Raids on the player's food.**

| Thief | Takes | Rule |
| --- | --- | --- |
| Fox, wolf, raven | A snared or trapped catch | After 48 h, 35% per extra game day |
| Wolverine | Catches, baits, cached food | 60% per day while in the region; robs every trap along a line in order |
| Otter | Fish on set lines and in nets | 20% of nights, one fish |
| Marten | Cubby bait | 15% of visits, without a catch |
| Ravens | Meat on open drying racks | 3% a game hour by day, 0.5 kg or less, with nobody within 30 m (Structures and buildables › Thieves at the rack) |
| Mice | Cache food in winter | 2–3% a week without deadfalls (Video checklist) |

**Build and test.** Code: `sim.animal.Predation` (daily kills in Ecology and staged chases), `Scavenging` (arrivals at carcass records) and the encounter director's staging (Water, calls and encounters). *Tests:* over 10 simulated years with no player, the core region always holds at least 2 moose and at least 5 hares; a named animal is never killed off-screen while the player is in its region; ravens reach 90% of carcasses within 2 game hours.

### Tracks and sign

Every animal leaves tracks in mud, sand and snow, plus scat, beds, rubs and feeding sign. Age shows in the detail: crisp edges and wet scat mean under an hour, softened edges mean hours, a crust of frost or blown snow means a day or more. Each species' sign starts unknown in the notebook and is learned by seeing it next to the animal, from logbook pages, or from the plant guide's tracks appendix. A trail of tracks can be followed until it crosses rock or ice; the tamed wolf can pick it back up.

**The sign catalogue.** Every species leaves four kinds of sign, each with its own reading. Sizes are adult print lengths.

| Species | Tracks | Scat and beds | Other sign | What it tells the player |
| --- | --- | --- | --- | --- |
| Moose | Heart-shaped split hoof, 12–14 cm cow, 15–17 cm bull; dewclaws show in mud and snow; walking stride 1.3–1.6 m | Summer: soft clumps. Fall to spring: piles of 80–150 oval pellets, 1.5–2.5 cm. Beds: 2 m ovals in willow or snow | Willow and birch tips snapped ragged at 1–2.5 m (no upper front teeth). Bull antler rubs on saplings Aug–Oct; rut pits pawed and urine-soaked in September | Bull tracks plus fresh rubs mean a bull to call in the rut; pellet piles show how long it fed there |
| Caribou | Rounded crescent hooves, 9–12 cm, toes splayed wide | Pellets 1–1.5 cm | Ground churned in a broad band; feeding craters dug in snow; shed antlers | Herd direction, and the river crossing it will use |
| Dall sheep | Blocky hooves, 7–9 cm, on ledges and scree | Pellets; beds scraped into cliff-edge hollows | Trampled clay at a lick | Which ledges it uses at which hour |
| Snowshoe hare | Large hind feet, 10–15 cm, landing ahead of the small front feet in a Y-shaped group | Round pea-sized pellets. Forms: shallow beds under spruce | Packed runways in snow; willow twigs cut cleanly at 45°, at snow height | A runway is a snare site; pellet density shows the hare-cycle phase |
| Red squirrel | Small paired bounds, tree to tree | Tiny pellets | Middens: piles of cone scales 1–3 m wide at a spruce base | A pole-snare site |
| Spruce grouse | Three-toed, 5 cm, wandering | Cylindrical droppings with white caps | Snow roost holes with droppings and wing marks where it burst out; summer dust baths | Where to look on a cold afternoon |
| Willow ptarmigan | Blurred 5 cm prints from feathered feet; flock tracks merging | Droppings in snow burrows | Willow buds clipped | A flock within 300 m |
| Ducks and geese | Webbed prints at mud edges | Droppings at resting spots | Feathers; in June, nests lined with down | Where decoys work; eggs in June |
| Porcupine | Pigeon-toed waddle, pebbly 7–9 cm soles, a trough in snow | Curved pellets in piles below a den tree | Bark stripped high in spruce with toothmarks; nipped twigs below; quills | Easy meat in that tree for days, and pitch on the wounded trunks |
| Beaver | Webbed hind feet, 15 cm, with a tail drag | — | Chewed stumps and chips (white and wet when fresh, grey when old); slides; dams, lodges, mud scent mounds | Fresh chips, or fresh mud on the lodge in October, mean an active colony for a lodge snare |
| Muskrat | Hand-like 5 cm prints with a tail line | — | Push-ups on the ice; feeding platforms of cut cattail | A lodge-snare site |
| River otter | Webbed 6–8 cm, in bounds | Latrines full of fish scales | Slides 3–5 m long down banks and over snow | Fish are here, and so is the competition |
| Marten | 4–5 cm in paired bounds, tree to tree | Twisted scat with fur or berries | — | A cubby set nearby will draw it |
| Red fox | A neat single line of 5 cm ovals | Small twisted scat on stumps and rocks | Scent posts with musky urine in winter; mousing craters in snow | Tracks running trap to trap mean the snare thief |
| Lynx | Round 9–10 cm, no claw marks, single line | Buried scat | Hare kills: fur and blood in snow, partly buried | A hare high; lynx numbers are up |
| Wolverine | Five toes with claws, 10–12 cm, in loping groups of 3–4 | Musky scat on a raided cache | Torn caches, scattered gear | Raise the caches now |
| Wolf | Ovals with claws, 10–13 cm, in one line; a pack steps in one track until it splits | Thick, ropy scat full of hair and bone | Scent posts with ground scraped behind; ring beds on ridges; howls at dusk; a dug den in a sandy bank with bones | Count the wolves where the line splits; a den means the taming window |
| Black bear | Hind 18–22 cm; short claws close to the toes; toes in an arc | Scat full of berries or grass; day beds | Claw marks up birch and aspen bark; torn logs and turned rocks | Same caution, less danger |
| Grizzly | Hind 25–30 cm; front claws 5–10 cm ahead of toes in a straight line | Berry or fish scat | Dug-up ground; salmon remains at the falls; hair on rub trees; a carcass covered with soil and sticks | A covered carcass means a bear bedded within 30 m: leave now |
| Raven | — | — | Circling and calls; feathers and droppings at kills | Meat nearby, and bears on the way |

**How sign ages.** Each sign object stores the game hour it was made. What it looks like follows from its age, the ground it is on, and the weather since.

| Ground | Under 1 hour | 1–6 hours | 6–24 hours | 1–3 days | Gone |
| --- | --- | --- | --- | --- | --- |
| Mud | Crisp walls; clear water in the print | Softened edges; water muddy or gone | Dry rim | Cracked; insect tracks across it | After 5 mm of rain, or 5 days |
| Sand, gravel | Sharp, damp grains | Grains dried and slid | Rounded | Faint | After 1 windy day, or rain |
| Snow | Crisp edges, loose crystals inside | Rounded edges | Frost crust inside | Blown in, glazed | Under new snow: 2 cm hides small tracks, 10 cm hides a moose's |
| Moss, leaf litter | Pressed moss, wet torn leaves | Moss springing back | Only bent stems | — | After 1 day |

- **Scat:** glossy and wet under 1 hour, and steaming below 0 °C; a dull skin at 1–12 hours; crusted at 12 hours to 3 days; dry and crumbling after that. Below −10 °C it freezes within an hour, so soft scat in deep cold is very fresh.
- **Beds:** a glazed hollow in snow means it was left 2–6 hours ago; an unglazed, soft bed is under 2 hours. Pressed grass springs back in 6 hours.
- **Blood and kills:** bright red under 1 hour, dark at 1–12 hours, brown and dry after. On snow it stays bright until new snow covers it. Ravens still at a kill mean it is under 2 days old.

**What the messenger says.** Tapping X (Read) on a sign reads it. Unknown sign gives shape only: "Hoof print, large, split. Unknown animal." Learned sign gives species and age band: "Moose, bull, walking north-east, 1–6 hours." After 10 readings of one species' sign, the age narrows to one figure ("about 3 hours"), and the sex and size of moose, bears and wolves are added, plus the number of wolves where a pack's line splits.

**Learning sign.** A species' sign is learned when the player sees that animal within 50 m within 10 game minutes of reading its sign, or watches it leave sign. Each researcher's logbook page teaches the sign of two species. The guide's tracks appendix covers the tracks, not the other sign, of hare, squirrel, grouse, moose, black bear, grizzly and wolf.

**Following a track.** Holding X on a learned print for 2 s reads it, pins it on the map and faintly lifts the next 3–5 prints for 20 s; there are no arrows. A line is lost on rock, ice and water, and where prints are past their "gone" time. A Bonded wolf picks it back up from scent up to 6 hours old on bare ground, 24 hours on snow, and 2 hours after rain.

**Generating sign.** Animals in the Active and Nearby tiers drop prints each stride on soft ground and one scat or feeding sign per 2 game hours of activity. When a chunk loads, sign for the last 72 hours is back-filled along trails from the region's population, using the catch-up rule in Engine rules. A chunk holds at most 400 sign objects; the oldest go first.

**Build and test.** Code: `ecosystem.sign` (prints and scat drawn from the chunk's sign attachment, back-filled by the catch-up rule); each species' track size, gait, scat rate and sign types sit in a `sign` block in `data/yearofice/species/<id>.json`. *Tests:* 5 mm of rain removes every mud print in a test chunk; 10 cm of new snow hides moose tracks and 2 cm hides hare tracks; a back-filled chunk shows sign within ±20% of the count a live run produces.

### Populations

Each region holds a population per species that grows toward the habitat's limit and drops with hunting, predators and hard winters. Young arrive on a real calendar: bear cubs leave dens in May, moose calves and wolf pups come in late May to June, and hares have up to three litters a summer. Hares follow a roughly 10-year boom and bust whose phase is drawn per seed; lynx and foxes rise and fall a year behind them. Moose recover from overhunting at a few percent a year, hares within a season, so what the player takes shapes the valley for years.

**Counts live per region.** Each 2 km region holds a count N and a limit K for each species. In the core valley, K is the top of the Wildlife table's "Per map" range, and the starting N is drawn inside that range, then given the ±20% rule from Biomes and resources. Other regions scale K by their habitat area. The realism slider "animal numbers" multiplies every K by 0.5–2.0.

**The daily update** runs at 00:00 in the Abstract tier, in this order:

1. **Natural deaths.** Each individual dies with chance μ ÷ 365, μ being its species' adult deaths per year in the table below.
2. **Predation.** Kills come from the rates in Who hunts whom.
3. **Births.** On a species' birth date, young are added by the formula below. Each juvenile dies at its species' juvenile rate, spread evenly, until October 1.
4. **Immigration.** On the first of each month, mobile species move toward empty neighboring regions.
5. **Hunting.** Every kill by the player subtracts 1 at once, from the right sex for species that track sex.

```latex
B = \mathrm{round}\left(F \cdot y \cdot \max\left(0,\ 1 - \frac{N}{K}\right)\right) \qquad I = \mathrm{round}\left(c \cdot \left(\overline{N/K}_{neighbors} - \frac{N}{K}\right) \cdot K\right)
```

F is breeding females (half of N unless sex is tracked) and y the young per female. The migration rate c is 0.1 for moose, wolves, bears, lynx, fox and wolverine and 0.05 for everything else; I is only applied when positive.

| Species | Born | Young per breeding female | Juvenile survival to Oct 1 | Adult deaths a year | Recovery target |
| --- | --- | --- | --- | --- | --- |
| Snowshoe hare | Jun 1, Jul 10, Aug 20 | 4 per litter | 25% | 60% | From 20% of K back to K in one summer |
| Red squirrel | May 15 | 4 | 40% | 35% | One year |
| Spruce grouse | Jun 10 | 6 | 30% | 40% | 1–2 years |
| Willow ptarmigan | Jun 15 | 8 | 25% | 50% | One year |
| Muskrat | Jun 1, Jul 15 | 6 | 30% | 50% | One year |
| Beaver | Jun 1 | 3 kits per colony | 60% | 10% | A colony over 8 splits and builds a new dam |
| Porcupine | May 20 | 1 | 60% | 10% | 3–4 years |
| Marten | Apr 25 | 3 | 40% | 30% | 2 years |
| Red fox | Apr 25 | 5 | 30% | 30% | 2 years, following the hares |
| Lynx | Jun 1 | 3; none when the hare multiplier is under 0.5 | 40% | 15% | One year behind the hares |
| River otter | Apr 15 | 2 | 50% | 15% | 3 years |
| Wolverine | Mar 15 | 2 | 40% | 15% | Immigration only in the core |
| Moose | May 20 – Jun 10 | 1.2 (twins 20%) | 50%, after the June bears | 8% | From half of K to 90% in 6–10 years |
| Dall sheep | May 25 | 0.8 | 50% | 10% | 5–8 years |
| Wolf | May 1 at the den | 5 | 40% | 15% | A pack rebuilt within 2 years |
| Black bear | In the den; out in May | 2 every 2 years | 50% | 6% | 8–12 years |
| Grizzly | In the den; out in May | 2 every 3 years | 40% | 5% | 12 years or more; immigration comes first |

**The hare cycle.** Hare K is multiplied each year by a fixed 10-step table, starting at a phase drawn per seed. The Wildlife table's 25–40 hares are the middle years.

| Year in cycle | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Hare K × | 0.3 | 0.5 | 0.9 | 1.5 | 2.2 | 2.5 | 1.8 | 0.9 | 0.4 | 0.25 |

Lynx K uses last year's multiplier clamped to 0.5–1.5. Fox K uses 0.7 + 0.3 × last year's multiplier. Wolf pack K is 4–6, rising to 8 when the hare multiplier is above 2 and the region's moose are at K.

**Special cases.**

- **Caribou** have no resident count. Each year a herd of 40–200 is drawn for the tundra regions, with 0–8 crossing the core in September. Each year has a 20% chance that the route moves to another tundra region (Years turn), and caribou the player takes are subtracted from next year's herd.
- **Wolf dispersal:** each November 1, a pack over 6 loses one yearling. Half the time it wanders the core until February as the lone yearling in Taming a wolf. If the player kills the breeding pair, the pack scatters into lone wolves and a new pack forms from immigrants within 1–2 years.
- **Hard winters:** the hard-winter rare event adds extra deaths on March 31: moose 15%, hares 20%, grouse 10%, sheep 10%. The winter-killed moose rare event removes one moose from the region where its carcass is placed.
- **Sex tracking:** moose, caribou, sheep, bears and wolves track males and females, and births count females only. Taking a cow moose costs about three future moose over 10 years, and the year card lists cows taken.

**Local refill.** When a trap or the player removes a small animal, its home range sits empty for the species' `respawn_days` from the species file (6 for hares). After that, a neighbor or a juvenile takes it over if the region's N allows. That is why a snare that just caught a hare goes quiet for a week.

**The encounter director.** The director never changes N; it changes where individuals appear. Within 1.5 km of the player's most-used bed, each species carries an encounter multiplier E. After a big kill (moose, caribou, Dall sheep, either bear, wolf, beaver) that species' E drops to 0.3 and climbs linearly back to 1.0 over 30–60 days, drawn per kill. Small game has no director, because its N already drops.

**From counts to animals.** When a chunk enters the Nearby tier, the region creates individuals equal to N × the share of its habitat within 600 m. Each is placed where its drives would have it at that hour, and folds back into N when the player leaves. Big-game individuals keep a persistent id with sex, age and wariness memory, so the bull the player missed is the same bull tomorrow.

```json
"population_model": {
  "k_core": 5,
  "births": { "dates": ["05-20", "06-10"], "young": 1.2, "juv_survival": 0.5 },
  "adult_deaths_per_year": 0.08,
  "migration": 0.1,
  "tracks_sex": true,
  "director": { "drop": 0.3, "recover_days": [30, 60] }
}
```

**Build and test.** Code: Ecology's daily step in the Abstract tier (`EcologyService`, Minecraft development › Tiers), with pure formulas in `sim.animal`. The block above is named `population_model` so it does not clash with the existing `population` range. *Tests:* across 100 simulated years with no player, every species except hares stays between 30% and 130% of its K; hare peaks are at least 8 times their troughs; lynx peaks lag hare peaks by 1 ±1 year. If the player takes 3 of 5 core moose in year 1, the count stays below its start until at least year 4 and is back within 1 of it by year 12.

## Hunting in depth

A good hunt is won before the shot: picking the place, the wind and the hour. The shot itself is short and decides how much meat comes home.

### Weapons and arrowheads

| Weapon | Tier | Effective range | Ready time | Best for |
| --- | --- | --- | --- | --- |
| Throwing stick | Start | 8 m | Instant | Grouse, ptarmigan, resting hares, porcupine |
| Spear | Bushcraft | 2 m thrust, 10 m throw | Instant | Fish in shallows, finishing wounded game; not a bear weapon |
| Light bow with blunts | Bushcraft | 15 m | 0.6 s | Squirrels, grouse, hares |
| Longbow | Bushcraft | 25 m | 1 s | Moose, caribou, wolves, black bear |
| Fish arrow on a line | Bushcraft | 8 m | 1 s | Pike and salmon in clear shallows |
| Sinew-backed bow | Hide and bone | 35 m | 1 s | Dall sheep, long shots, grizzly |

| Arrowhead | Effect | Reuse |
| --- | --- | --- |
| Blunt (wood) | Stuns small game without spoiling meat | Usually |
| Flint broadhead | Cuts well; shatters on bone 15% of the time | 60% |
| Bone point | Tough, cuts less, so longer blood trails | 80% |
| Iron broadhead | Best cutting and penetration | 90% |

**Weapon stats.** Arrow energy is ½ m v², and the kill thresholds it is tested against are in The hit-zone model. The longbow's recipe is in What a first moose requires.

| Weapon | Strength needed | Arrow or throw | Energy | Made from | Time |
| --- | --- | --- | --- | --- | --- |
| Throwing stick | — | 60 cm stick, about 15 m/s | Kills grouse and ptarmigan; stuns a hare for 5 s | A birch or spruce branch | 2 game minutes |
| Throwing rock | — | 0.3–0.5 kg stone | Stuns squirrels and grouse | Any fist-sized stone | — |
| Spear | — | 2 m shaft; thrust 2 m, throw 10 m | A thrust into a downed or snared animal's vitals counts as a heart-and-lung hit. A thrown spear kills fish and hares; on big game it is never more than a gut hit | Willow or spruce shaft, a flint or bone point, pitch glue, cordage. A fire-hardened tip alone works for fish | 45 game minutes; 20 fire-hardened |
| Light bow | 40 | 25 g arrow at 42 m/s | 22 J | A willow or birch sapling seasoned 3 game days, bowstring | 1.5 game hours |
| Longbow | 70 | 30 g arrow at 50 m/s | 37.5 J | See the build sheet | 3 game hours plus seasoning |
| Fish arrow on a line | Bow's own | Barbed bone point on 6 m of line tied to the bow | Kills a fish it hits | A bone point, line, any bow | 30 game minutes |
| Sinew-backed bow | 85 | 30 g arrow at 58 m/s | 50 J | A seasoned longbow, 0.3 kg of shredded sinew, hide glue | 2 game hours to lay, 5 game days to cure |

**Fish arrows and refraction.** A fish looks higher and closer than it is, so the player aims low. Under water an arrow loses 80% of its speed in the first metre, so fish deeper than 0.8 m cannot be shot.

**Bow care.**

- **Unstringing:** the bow unstrings itself when stowed in the pack (3 s). Left strung over 12 game hours, it loses 5% arrow speed for good, up to 15%.
- **Deep cold:** below −30 °C, a full draw on a bow not warmed inside the coat for 10 game minutes has a 2% chance per shot to crack it.
- **Wet:** a wet plant-fiber string costs 15% speed; a wet sinew-backed bow loses 20% until dried by a fire.
- **Wear:** bows lose 0.2 condition per shot. Below 30 condition each shot has a 5% chance to break the bow; a string below 20 snaps.

**Arrow parts.** One arrow per craft, in four steps. Shaft: cut, peel and straighten over the fire in a three-bend minigame, 10 game minutes. Nock: 2. Fletching: split, trim, glue and wrap 3 feathers, 8. Head: 5. Practice cuts the total by up to 40%.

**Feathers.** A grouse gives 15 feathers (5 arrows), a ptarmigan 12 (4), a duck 20 (6), a goose 30 (10). Unfletched arrows fly true to 10 m; beyond that they yaw, with triple sway and 30% less energy. Wet fletching adds 20% sway until dry.

| Arrowhead | Made from | Time | Energy factor | Rule |
| --- | --- | --- | --- | --- |
| Blunt (wood) | A knob carved on the shaft | 5 game minutes | ×0 for penetration | Kills or stuns small game; no meat spoiled |
| Flint broadhead | Flint nodule | 10 game minutes | ×1.0 | Shatters on bone 15% of the time (see shoulder rule) |
| Bone point | A leg bone of big game, ground on a stone | 20 game minutes | ×0.85 | Never shatters |
| Iron broadhead | 50 g of bloom iron | 15 game minutes at the forge | ×1.15 | Passes through the shoulder |

```json
{
  "id": "longbow",
  "tier": "bushcraft",
  "strength_req": 70,
  "arrow_speed_mps": 50,
  "arrow_mass_g": 30,
  "draw_time_s": 1.0,
  "max_hold_s": 6,
  "effective_range_m": 25,
  "condition_per_shot": 0.2
}
```

**Build and test.** Code: `hunt` (bows, arrows, heads and the sling; Food and hunting); each weapon's numbers are its entry in `data_maps/item/props.json` with a `weapon` block like the one above (Names settled), and bow condition is vanilla durability. *Tests:* in 100 broadside heart-and-lung hits on a moose at 20 m with a full-draw longbow and flint heads, none falls below the kill threshold; at 60% draw, every vital hit becomes a wound.

### The hit-zone model

There are no hit markers. Where the arrow lands decides what happens next, and broadside shots open the vitals; head-on shots almost never do.

| Zone hit | What the animal does | Time to death | Blood trail | Meat recovered |
| --- | --- | --- | --- | --- |
| Heart and lungs | Runs 50–150 m and drops | Under 1 minute | Heavy, bright, frothy | All |
| Liver | Beds down after 200–400 m | 1–3 hours | Dark | All, if found that day |
| Gut | Runs far, beds again and again | 6–12 hours | Sparse, with stomach matter | Loses about 20%; spoils faster |
| Leg | Limps away | Survives | Spotty, soon gone | None |
| Head or neck | Drops at once, or a clean miss | Instant | None | All |

**Reading the reaction.** A hunched kick and a hard run mean a body hit; a dangling leg means a leg hit. Wounded animals often stop at the edge of cover and look back, which is the moment for a second arrow. Pushing a gut-shot animal too soon makes it run for hours.

**Zones are shapes on the body.** Each species has ellipsoid zones in its own body frame, stored in `data/yearofice/hitzone/<id>.json` (Minecraft development › Hitboxes, hit zones and the damage map). The arrow's path is traced through them, and the first zone it enters decides the outcome. For a bull moose:

| Zone | Size and place |
| --- | --- |
| Heart and lungs | 50 × 45 cm, the lower half of the chest just behind the front leg; the front 30% sits behind the shoulder blade |
| Liver | 30 × 25 cm, just behind the heart and lungs |
| Spine | An 8 cm strip along the back: drops at once, counted as the head-or-neck row |
| Brain | 10 cm |
| Legs | Everything below the brisket line |
| Gut | The rest of the body |

A neck hit that misses the spine is the "clean miss" in the table: a light trail that stops, and an animal that lives.

**Will it reach the vitals?** A broadhead reaching a vital zone kills only if its effective energy beats the species' threshold. Otherwise it wounds, behaving as a gut hit for timing and trail. That is how the video's first bear arrow only wounded the bear.

```latex
E_{eff} = E \cdot f_{head} \cdot f_{range} \cdot f_{angle} \qquad f_{range} = \max\left(0,\ 1 - 0.04 \cdot (d - d_{effective})\right)\ \text{for}\ d > d_{effective}
```

| Shot angle | f\_angle |
| --- | --- |
| Broadside or quartering away | 1.0 |
| Quartering toward | 0.6 |
| Head-on | 0.3 |
| Straight away | Vitals cannot be reached; always a gut hit |

| Species | Kill threshold |
| --- | --- |
| Hare, squirrel, grouse, ptarmigan, ducks | 2 J; a blunt kills with a head or body hit |
| Beaver, porcupine, muskrat, marten, fox, otter | 8 J |
| Lynx | 10 J |
| Wolf, wolverine | 18 J |
| Dall sheep | 24 J |
| Caribou | 26 J |
| Black bear | 28 J |
| Moose | 30 J |
| Grizzly | 36 J |

Species missing here take their thresholds from Minecraft development › Hitboxes: wood bison 40 J, muskox 34 J, reindeer 26 J, mountain goat 24 J, livestock goat 22 J, coyote 12 J, fisher, arctic fox, woodchuck and marmot 8 J. A full-draw longbow (37.5 J) kills a moose out to about 30 m and a grizzly only within about 26 m with flint (29 m with iron), and never with bone. That is the "possible but risky" in How animals act. The sinew-backed bow's 50 J opens long shots and grizzlies.

**The shoulder rule.** An arrow entering the front 30% of the heart and lungs strikes the shoulder blade. A flint head shatters there 15% of the time, giving a shallow wound that behaves as a leg hit. Bone heads lose their 15% as usual, and iron heads pass through.

**Blood trail data.** Drops are placed along the wounded animal's path, and they fade over 2 game hours. Rain halves that, and snow doubles it.

| Zone hit | Drops per 10 m | Look | Wait before following |
| --- | --- | --- | --- |
| Heart and lungs | 30, both sides of the trail | Bright red, pink froth | 15 game minutes |
| Liver | 10 | Dark red | 2 game hours |
| Gut | 3 | Sparse, with green stomach matter; beds every 300 m | 6 game hours |
| Leg | 5 for the first 100 m, then none | Spotty | The animal lives; tracks only |

**Pushing a wounded animal.** If the player comes inside a bedded wounded animal's flee distance before its wait time is up, it runs again 300–800 m. Its death timer grows by 2 game hours and its trail thins by half.

**A downed animal can still fight.** If a bedded moose hit in the liver or gut is still alive when the player closes to 15 m, it gets up and charges 30% of the time, a bear 50%. A spear thrust or a second arrow at 2–10 m to the vitals finishes it.

**Second arrows and misses.** After a body hit, the animal stops at the edge of cover and looks back for 3–6 s on 60% of hits. A miss that passes close over the back makes the animal jump and turn Alert; a moose then stands for 2–4 s on half of misses, the classic second chance.

**Meat losses do not stack.** The gut row's 20% and the late-gutting quarter in Field care never add together: the larger one applies, then normal spoilage.

**Small game.** A blunt hit on head or body kills a squirrel, grouse or ptarmigan, and a hare within 15 m. A hare hit in the hindquarters or beyond 15 m is stunned 3 s, then runs 20 m to cover. A blunt on anything with a threshold over 8 J only angers it: no wound, awareness straight to 100, and a bear or moose goes to Warn (Video row 181).

**Build and test.** Code: `sim.hunt.HitResolver` (pure) and `ecosystem.wound.HitAdapter`, which runs from the arrow's `onHitEntity` and from spear and knife use; every vanilla damage to animals is cancelled (Minecraft development › Hitboxes). *Tests:* 1,000 random broadside shots on a moose at 20 m land in each zone in proportion to its projected area, ±3%; fewer than 10% of head-on shots reach the heart and lungs; at 60% draw a longbow never kills a black bear outright.

### Setting up the hunt

| Setup | How it works | Good for |
| --- | --- | --- |
| Ground blind | Brush and boughs at a trail or crossing; the player is invisible while still | Moose, caribou crossings |
| Tree stand | Poles lashed 3 m up a spruce; scent carries over the animals and the view widens | Moose trails, black bear |
| Mineral lick | A natural lick (a set piece) draws moose and sheep daily | Patient ambush |
| Moose call | Birch-bark cone; bulls answer in the rut (September–October) | Bull moose |
| Squeak call | Mimics a hurt hare | Foxes, lynx, wolves |
| Decoys | Carved birch-bark birds on a lake | Ducks and geese in migration |
| Wolf drive | The tamed wolf circles and pushes game toward the player | Hares, ptarmigan, later moose |

**Build sheet.**

| Setup | Build | Time | What it does in numbers | Limits |
| --- | --- | --- | --- | --- |
| Ground blind | 20 spruce boughs, 4 stakes, 3 m cordage | 40 game minutes | A still player inside is unseen. Drawing a bow shows movement at half range to animals within 15 m in front. Noise ×0.5; scent unchanged | A blind under 1 game day old only halves sight for moose, caribou and sheep. Collapses under 40 cm of snow unless cleared |
| Tree stand | 2 poles of 4 m with 6 rungs lashed as a ladder, a 4-pole platform, 12 m cordage, on a spruce over 30 cm thick | 3 game hours | Platform at 3 m. Scent does not reach the ground within 40 m; prey see the player at ×0.3, bears at ×0.7; view distance ×1.5 | Icy rungs below 0 °C with snow: 10% fall chance when climbing. Falling asleep in the stand drops the player 3 m (a sprain). No protection from black bears |
| Bait station | A carcass, gut pile or 5 kg of Turning fish, 15–20 m from a stand; or a ripe berry patch (August 1 – September 20) within 40 m of a stand, which draws a black bear at dusk on 30% of days and a grizzly on 15% with no bait carried in (Video row 260) | 10 game minutes | Bears path to it by the scent rules; a black bear comes within 1–3 days, 60% of the time at dusk. Once a bear takes from it, it returns each dusk until the bait is gone | Never within 400 m of camp |
| Mineral lick | A set piece | — | Moose visit at dawn or dusk on 60% of days from May to August; sheep visit the cliff lick daily in June–July | Hunted twice in a week, a lick loses its draw for 14 days |
| Moose call | One 40 × 30 cm birch-bark sheet rolled into a cone, rawhide strips, cordage, a dowel | 30 game minutes | See calling below | Bulls only, September–October |
| Squeak call | A birch-bark reed, or the lips | 2 game minutes | See calling below | — |
| Decoys | Carved birch-bark or wood birds | 1 game hour each | With 6 or more decoys set within 20 m of a blind, a passing flock lands within 30 m 40% of the time; with none, 10% | Migration windows only; a shot flushes the flock for 2 game hours |

**Calling a bull.** The call has three sounds: a long cow moan (hold right mouse 2–4 s with the call), a short bull grunt (tap right mouse), and raking brush (hold right mouse 3–6 s on willow or alder with the call or a moose shoulder blade; Water, calls and encounters). Each cow moan rolls once for every bull within 800 m:

```latex
P_{respond} = 0.4 \cdot rut(date) \cdot \left(1 - \frac{d}{800}\right)
```

The rut factor is 0.3 from September 1–9, 1.0 from September 10 to October 5, 0.4 from October 6–20, and 0 otherwise.

**What a bull does.**

1. Answers with a grunt that the messenger places on the wind view, heard to 800 m.
2. Walks in at about 1 m/s, stopping every 50–100 m to listen.
3. In the last 150 m, circles to get downwind half the time, unless water, an open bog or a cliff blocks that side.
4. Hangs up at 60–120 m half the time if the player called within the last 10 game minutes. Raking brush brings a hung-up bull on in 60% of tries.
5. Inside 25 m, a bull that sees the player standing charges 20% of the time (How animals act).

The best rhythm is one moan, a 15–30 minute wait, then soft grunts once he answers. A site with water or open ground on its downwind side forces the bull to show himself.

**The squeak.** A held squeak (5 s) imitates a hurt hare. Within 300 m at dusk it draws a fox 50% of the time, a lynx 25%, and wolves 10%, as a pack and dangerous in winter. From April to October it also draws a bear 10% of the time. A tap squeak still stops a running hare for 2 s.

**Wolf drive.** A Bonded or better wolf drives game when the player whistles twice (G twice) while aiming at a spot. The wolf circles 100–200 m out and pushes game along the line toward the player. Hares come 60% of the time and ptarmigan flocks flush toward the player 70%.

**Holding a moose at bay.** That takes a Companion wolf with trust at 90 or more, and works 40% of the time; below that trust, the wolf just chases the moose off. Each minute a moose is held, the wolf has a 10% chance of being stomped and injured.

**The hunt plan.** In the messenger, marking a spot as a stand or blind draws its scent plume for each hour of the next day from the forecast wind and thermals. Hours when the plume crosses the marked trail are shaded: "Stand at the beaver pond: good 06:00–10:00 and after 18:00; scent crosses the trail at midday."

**Build and test.** Code: `hunt` call items (`call.moose_cow`, `call.moose_grunt`, `call.rake`) and blinds, stands and decoys, with the answer roll in Water, calls and encounters' draw model. *Tests:* in the rut, a moan every 20 game minutes from 600 m brings a bull within 50 m on 30–50% of 2-hour hunts over 200 simulated hunts; calling every 3 minutes at least halves that rate; calling in July never brings a bull.

### Traps

| Trap | Catches | Built from | Tip |
| --- | --- | --- | --- |
| Trail snare | Hares, ptarmigan | Cordage or wire | On a trail it catches 4 times as often |
| Pole snare | Squirrels | A leaning pole and wire loops | Beside a midden of spruce cones |
| Deadfall (figure-4) | Marten, fox, squirrels | A flat rock and three notched sticks | Needs no wire at all |
| Cubby set | Marten, lynx | Spruce boughs around bait | Bait draws furbearers from 100 m |
| Lodge snare | Beaver | Heavy wire | Under the ice at the lodge in winter |
| Bird noose pole | Grouse, ptarmigan | Cordage loops on a pole | Grouse step into them while feeding |

**The trapline.** Every trap shows on the messenger map with a timer since it was last checked. Snow buries snares and they need resetting; a catch left more than 48 hours may be eaten by foxes or a wolverine. Running a line of 20 traps on snowshoes is the core winter loop.

**Each trap's build, set, trigger and check.**

| Trap | Build | Set | Trigger | Check and reset |
| --- | --- | --- | --- | --- |
| Trail snare | 1 m of cordage or wire, a slip knot or a wire lock; 3 game minutes | On a trail cell: loop 10 cm across for hares (8 cm for ptarmigan), its bottom a fist's height (8–10 cm) above ground or snow, with guide sticks either side; 2 game minutes | A catch at a dawn or dusk roll. Wire kills at once; cordage holds a struggling animal and breaks on 30% of catches, leaving fur on the loop | Visible from 3 m; reset 1 game minute; raise the loop after snowfall |
| Pole snare | A 2.5 m pole leaning at 45° against a midden tree, with 4–6 wire loops 6 cm across along its top; 10 game minutes | Within 2 m of a squirrel midden, reaching the first branch | Each loop rolls separately: up to 3 squirrels a roll | 30 seconds per loop |
| Figure-4 deadfall | Three notched sticks (upright, diagonal, bait stick) and a flat rock at least 10 times the target's weight; 15 game minutes | Rock propped on the 4, bait on the bait stick; 5 game minutes. At practice 0 the rock slips 15% of the time: a cut finger and 2 Condition | The animal tugs the bait and the rock drops: 90% kill for the right size, 50% for anything heavier than a tenth of the rock | Reset 5 game minutes; the sticks last 10 triggers |
| Mouse deadfall | A flat stone on a flint-flake trigger; 5 game minutes | Beside a cache; three around it stop winter mice | A roll each winter night | Reset 1 game minute; each mouse is marten bait |
| Cubby set | 30 spruce boughs pushed into snow or soil in a V 40 cm high and 60 cm deep, a snare or deadfall in the mouth, bait at the back; 20 game minutes | Marten: 20 cm mouth near old spruce. Lynx: 30 cm mouth in a hare thicket, castor lure, a feather hung as a flash | Bait draws marten, lynx and fox from 100 m; a marten steals the bait without a catch 15% of the time | Rebait every 4 days; below −20 °C frozen bait carries half as far |
| Lodge snare | 1.5 m of heavy wire on a 3 m dry pole, with a bundle of fresh aspen or willow sticks; 15 game minutes | Summer: at a dam crossover or slide, 5 cm under water. Winter: chisel a hole 30 cm from the lodge (20 game minutes through 30 cm of ice) and set the pole so the loop sits on the run before the entrance. A light-wire version goes at a muskrat push-up | A passing beaver drowns in the set 95% of the time | Every 2 days; a hole checked daily reopens in 5 game minutes |
| Bird noose pole | A 2 m pole with 6 cordage nooses 8 cm across on a crossbar; 15 game minutes. The active version is one noose on a 3 m stick | Passive: across a grouse feeding area or a willow flat. Active: slid over the head of a frozen grouse within 3 m | Passive: a dawn roll. Active: a timed press while the noose is over the head | 1 game minute |

**Spring pole.** Any snare can be tied to a bent sapling (10 game minutes). It lifts the catch 1 m: cordage then kills as surely as wire, theft after 48 h falls to 30% of normal, and snow cannot bury it. In wind over 8 m/s it springs empty on 10% of nights.

**Catch chance.** Each roll asks every animal of the target species whose home range covers the trap. That makes catches depend on the population model: a hare boom fills the line and a crash empties it.

```latex
P_{catch} = 1 - \left(1 - p_s \cdot m_{place} \cdot m_{bait} \cdot m_{scent} \cdot m_{weather}\right)^{n}
```

| Target | p\_s per animal per roll |
| --- | --- |
| Hare (trail snare) | 0.025 |
| Ptarmigan | 0.02 |
| Squirrel (per pole loop) | 0.05 |
| Grouse (noose pole) | 0.04 |
| Marten | 0.06 |
| Fox | 0.03 |
| Lynx | 0.02 |
| Muskrat | 0.10 |
| Beaver | 0.15 |

The multipliers are the existing rules: on a trail ×4, beside cover ×1.5, baited ×1.5, human scent from checking within 5 m more than once a day ×0.5. Weather adds ×0.5 below −30 °C and ×0.5 on a night of fresh snowfall. n comes from the region's population, so a trap whose last hare was just caught waits out the species' `respawn_days`.

**Snow and theft.** Ground sets (trail snares, deadfalls, cubbies) stop rolling once 10 cm of snow has fallen since they were set, until reset. A catch left over 48 hours is taken with a 35% chance per extra game day, 60% while a wolverine is in the region. The thief leaves sign, so the check reads "Taken: fox tracks, 1–6 hours."

**Catches spoil and freeze.** A dead catch starts its freshness clock at air temperature and freezes below −5 °C. Its pelt slips above 5 °C after 12 hours (Butchering and yields).

**What a check shows.**

| State | At the trap | Messenger pin |
| --- | --- | --- |
| Set | Loop open, bait in place | Hours since the last check: green under 24, amber 24–48, red over 48 |
| Sprung, empty | Loop closed; fur on it if a catch broke free | Grey |
| Catch | The animal | Bold |
| Taken | Scraps and tracks | Red |
| Buried | Snow over the set | Blue |
| Broken | Snapped cordage or a fallen pole | Red |

**Wear.** A cordage snare lasts 5 catches and a wire snare 20; deadfall sticks last 10 triggers, and the rock never wears out.

**Running the line.** Traps can be saved as a named trapline route. Walking it shows a needle on the messenger toward the next trap within 200 m, which costs almost no battery; each check is still done by hand. A 20-trap line at 100 m spacing takes about 2 game hours on snowshoes, checks included, and about 600 kcal. In a middle hare year it yields 1–3 catches a day: the winter loop.

**Build and test.** Code: `food.traps` (Food and hunting); each trap's numbers sit in a `trap` block of its entry in `data_maps/item/props.json` (target species, p\_s, durability, ground set or not), and a set trap is a block entity. *Tests:* a baited trail snare on a trail with one hare in range catches it within 5 nights in 70–85% of trials, and the same snare unbaited off the trail in under 30%; 10 cm of snowfall stops ground sets until reset; a catch left 4 days is taken at least 55% of the time.

**Ground or water: where a trap goes decides what it catches.** Every trap is placed like a building piece, with a ghost preview. The surface under the crosshair decides whether it is a ground set or a water set. The preview turns green on a valid spot and names the likely target and how good the spot is ("Trail snare · hare · on a trail: good"). It turns red with the reason on a bad one ("Current too strong", "Too deep", "Frozen ground"). Some traps work both ways: the same wire snare catches a hare on a trail and a beaver 5 cm under water at a dam crossover.

| Trap | Ground set | Water set | Water placement rule |
| --- | --- | --- | --- |
| Snare (cordage or wire) | Trail snare: hares, ptarmigan | Lodge or slide snare: beaver. Push-up snare: muskrat. Drowning slide: otter, mink | 5 cm under the surface at a dam crossover, slide or run, within 5 m of a lodge or push-up |
| Floating noose log | — | Ducks and geese in summer, when moulting birds can't fly | A log anchored in a pond or slough, nooses along its top; still water only |
| Pole snare | Squirrels at a midden | — | — |
| Deadfall, cubby, bird noose pole | Marten, fox, lynx, grouse, mice | — | — |
| Minnow trap | — | Minnows for bait, small grayling | 0.2–0.6 m deep, slow water, baited with fish guts as in the video |
| Willow fish basket | — | Grayling, whitefish, burbot under winter ice | 0.3–0.8 m deep in current of 0.2–0.8 m/s, mouth facing downstream; best in a weir gap |
| Gill net (4 joined segments) | — | Whitefish, salmon, pike | 0.8–3 m deep in still or slow water under 0.3 m/s; set from a raft, by wading, on a pull line, or under ice |
| Set line | — | Pike, burbot, lake trout | Baited hooks on a line staked to the bank; any depth near shore; through a hole in winter |

Any water set in current over 1.2 m/s washes away within a game day. Spring floods can strand a set on dry ground or pull it into deep water, and breakup ice tears nets. Getting to a water set uses the wet or dry ways in Swimming and working in water. Bites, fights and passive fishing gear are covered in Fishing methods.

**Traps keep working while the player is away.** Every set trap catches whether the player stands beside it or is 5 km away, and the results are the same either way.

1. **Each trap keeps a small record:** type, position, ground or water, bait, the time it was set, the time it was last checked, and its condition.
2. **Catch rolls run on a schedule.** Ground sets roll at dawn and dusk, plus once at night for night hunters such as marten and lynx. Water sets roll every 6 game hours, and gill nets roll as Fishing methods sets out. Each roll uses the catch-chance formula above with that day's population and weather.
3. **Rolls are settled on return.** When the trap is in a far tier, the engine settles every missed roll the moment the player comes within 300 m or the game saves. It uses the stored weather for each roll, so a blizzard night counts as a blizzard night. Rolls are seeded per trap and per roll time, so the result never depends on where the player was.
4. **A trap holds what it catches.** A snare holds one animal, a pole snare one per loop, a fish basket up to 6 fish, a gill net about 1 fish per segment a day, and a set line 1 per hook. A full trap stops rolling.
5. **A held catch has its own clocks.** On land it spoils at air temperature and freezes below −5 °C. In water it spoils at water temperature, which keeps it fresh longer. Fish in a basket stay alive for 24 game hours, but fish in a gill net die within 2. Theft follows the 48-hour rule above. Otters and mink rob nets, and bears tear open baskets.
6. **Bait runs out.** It lasts 4 days on land and 2 days in water before it washes out.

**What the player knows.** The messenger never reports a catch: it has no cameras, so a trap has to be walked to and checked. The world gives tells instead.

- **Sound:** within 150 m, a snare snapping shut, a struggling hare or a flapping grouse. This is the video's "I think one of my snares just went off."
- **Birds:** ravens circling over a dead catch, seen from 500 m in daylight; gulls over a full net in summer.
- **The wolf:** a Bonded or Companion wolf turns toward a trap with a catch within 200 m and whines.
- **The map pin:** time since the last check, coloured as in the check table above.

On Gentle only, an option called "Trapper's sense" marks a pin with a dot once a catch is probably waiting, after 12 game hours.

**Checking, resetting and moving.** At the trap, right mouse collects the catch, holding right mouse resets the trap in 1 game minute, and baiting it is right mouse with the bait in hand. Holding right mouse with an empty hand while crouched (Shift) picks the trap up so it can be placed somewhere else. The check also writes a line to the trapline log: "Snare 7 · hare · caught at dusk yesterday · fresh".

**A trapline morning.** A typical early-winter round has 8 trail snares, 2 cubby deadfalls, a basket in the weir and a gill net under the ice. It takes about 2.5 game hours on snowshoes. It might bring back 2 hares, a marten, 4 grayling and a whitefish, plus one snare buried by snow and one theft with fox tracks beside it.

**Data and build.** The `trap` block in `data_maps/item/props.json` gains `surfaces` (ground, water or both), `depth_m` (minimum and maximum), `current_max_mps` and `holds`. Off-screen catches are settled in `food.traps` through the engine's catch-up, seeded per trap and roll time.

Tests:

- With the same seed and the same weather, a trapline left alone for 3 days returns exactly the same catches as one watched the whole time.
- A basket set in current faster than 0.8 m/s, or a gill net in current faster than 0.3 m/s, is refused with its reason in the preview.
- A water-set catch at 4 °C stays fresh at least twice as long as the same catch on land at 10 °C.
- No messenger screen ever shows that a trap has caught something before it is checked, except Trapper's sense on Gentle.

### Field care

Meat has to be cared for quickly. Gutting within an hour of the kill keeps all the meat; later costs a quarter of it. Above 10 °C a big animal must be quartered within 4 hours and hung in shade, or it spoils. In winter it freezes solid on its own, which is easy to keep but slow to butcher. Clean kills mean more meat, so the game rewards good shots without lecturing.

**Big game, step by step.** Each step is a hold-to-work job with a progress ring and can be queued. Times are at practice 0; practice cuts them by up to 40%.

| Step | How | Moose | Caribou | Black bear | Dall sheep |
| --- | --- | --- | --- | --- | --- |
| 1. Wait and approach | Wait by hit zone (The hit-zone model) | — | — | — | — |
| 2. Position | Roll onto its back and prop it with logs; a lone player dresses a moose on its side | 5 game minutes | 3 | 3 | 2 |
| 3. Gut | Open from pelvis to breastbone without cutting the gut, cut the diaphragm, roll the gut pile out | 45 | 25 | 35 | 20 |
| 4. Organs | Heart, liver, kidneys and tongue into a bag | 15 | 10 | 10 | 5 |
| 5. Prop open | Sticks spread the ribs; carcass moved into shade if possible | 5 | 5 | 5 | 5 |
| 6. Skin | Hide off in one piece, laid hair-down as a clean work sheet | 45 | 30 | 45 | 20 |
| 7. Quarter | Front legs off at the shoulder, hind legs at the hip joint; backstraps, tenderloins, neck and rib meat cut loose | 100 | 50 | 60 | 30 |
| 8. Hang or lay out | Quarters on a meat pole in shade, or on brush off the ground | 20 | 15 | 15 | 10 |
| At the kill |  | About 4 game hours | 2.3 | 2.9 | 1.5 |

Boning and cutting strips for the smoker at camp adds 2–4 hours for a moose, which makes the 6–8 hours in Food systems. At practice 0, a lone hunter who starts within 15 minutes has quarters off a moose 3.8 hours after the kill: just inside the 4-hour rule.

**Careful or fast gutting.** A toggle on the gut step: fast halves the time but cuts the gut 20% of the time (2% at full practice). A cut taints 5% of the meat with a gut-illness risk. Butchering a moose costs about 15 knife edge, so the hone comes along.

**Deadlines by air temperature.** Times count from the kill, and "quarter by" means the quarters are off the carcass. The messenger warns once at the kill: "Gut within the hour, quarter by 21:40."

| Air | Gut by | Quarter by | Then keeps, hung in shade |
| --- | --- | --- | --- |
| Above 10 °C | 1 hour | 4 hours (the existing rule) | 2.5 days at 15 °C, 4.3 at 10 °C (Spoilage model) |
| 0–10 °C | 1 hour | 12 hours | 4.3–13 days |
| −10 to 0 °C | 1 hour | 8 hours, before the hide freezes on | Frozen; keeps until the thaw |
| Below −10 °C | 1 hour | 4 hours; a moose freezes solid in 18 hours | Frozen; keeps until the thaw |

- **Late gutting:** after 1 hour, 25% of the meat is lost (the existing rule), and the larger of this and a gut hit's 20% applies.
- **Late quartering:** missing the deadline turns 30% of the meat (the deep hindquarter) to Turning, the "bone sour" rule. Missing it by double turns the whole carcass to Turning.
- **Frozen carcass:** once frozen, the hide cannot come off and is lost. Quartering takes the hatchet and three times as long. Caribou freeze solid in 10 hours, sheep in 8, a hare in 2.
- **Crust:** quarters hung in moving air for 12 hours grow a dry crust that keeps flies off, so the fly multiplier stops applying.

**The gut pile.** A bull moose's gut pile weighs 60 kg (Bestiary › Parts by species) and makes a scent point at triple strength. Dragging it 50 m downwind (5 game minutes) means a bear that comes eats the pile first, for 2–4 game hours. Ravens find it within 2 hours.

**The predator clock.** From April to October, where a bear range lies within 1.5 km, a bear arrives with a 4% chance each game hour, doubled at dusk; that is about two in three within a day. From November to April, wolves arrive with 3% an hour.

| Defense | Build | Stops | Does not stop |
| --- | --- | --- | --- |
| Fire at the kill | A teepee fire, 10 game minutes | Wolves and foxes stay 8 m out | A bear at a carcass ignores it half the time |
| Kill-site chest | Built on site, 30 game minutes; holds 100 kg | Ravens, foxes, wolves | A bear or wolverine breaks it in 2 game hours |
| Meat pole at the kill | 30 game minutes, 4 m up | Bears, wolves, foxes | Wolverines climb it; ravens peck uncovered meat unless boughs are laid over it |

**The haul.** A bull moose comes home in these loads on a pack frame that carries 40 kg a trip:

| Load | Weight on the frame | Meat in it |
| --- | --- | --- |
| Hindquarters, split into four loads | 27 kg each | 22.5 kg each |
| Front quarters, one load each | 40 kg each | 32 kg each |
| Loose meat, organs and fat | 24 kg | 26 kg meat (backstraps, tenderloins, neck, ribs), 8.3 kg organs, 3 kg fat; 37 kg on the frame |
| Hide | 30 kg | — |
| Head and antlers (optional trophy) | 25 kg | — |

That is 8 trips without the head and 9 with it. A weak player who also splits the front quarters to stay under 30 kg makes 10. The meat adds up to the 180 kg of a 450 kg bull in Bestiary › Parts by species: 90 kg in the hinds, 64 kg in the fronts and 26 kg loose (RECONCILE 53).

**Boning out.** Boning a quarter at the kill takes 30 game minutes and removes about 9 kg of bone from a hind and 8 from a front. The moose's meat, organs and fat (191 kg) then fit in 5 loads of about 38 kg. The cost is 2 more hours at the kill while the predator clock runs, and boned meat spoils 1.2 times faster because more of it is cut surface.

**Haul time.** On a cleared route, a load takes about 20 game minutes per km at the 65% loaded speed, and the walk back empty about 12. Loaded hours burn 400 kcal and empty hours 250.

**Winter hauling.** A sled carries 120 kg on snow, so a winter moose comes home in 2 trips boned out, or 3 bone-in with the hide. With no sled, the fresh hide laid hair-down drags 80 kg on snow at 30% speed, the "drag only" row of the load table.

**At camp.** Quarters go onto the meat pole or into the raised cache. Above 0 °C, smoking or drying should start within 2 days: a bull moose fills the smokehouse about 3.5 times, or the smoke rack about 18 times.

**The hide.** A green hide must be scraped within 3 days above 5 °C or it rots and is lost. Below −5 °C it freezes and waits. Scraping and drying then follows the existing 2-day rawhide and 6-day smoke-tan rule.

**A first moose, as it plays.**

1. September 22, 17:40, 11 °C. A bull is hit broadside at 22 m, 1.8 km from camp; it runs 90 m and drops. The player waits 15 minutes.
2. 17:55. The messenger warns: "Gut within the hour, quarter by 21:40." Gutting is done at 18:45 and the gut pile is dragged downwind. Ravens are circling by 19:30.
3. 21:30. Skinned and quartered by headlamp with 10 minutes to spare; by 21:50 the quarters hang on a meat pole lashed between two spruce. A small fire keeps the foxes off.
4. 22:00. The first hind half-quarter goes home: 27 kg, 36 minutes of walking at 65% speed. The liver is eaten that night, fried in lard.
5. September 23, 06:00. Sleep debt from the night. A grizzly has fed on the gut pile and bedded near it; the player waits it out from the far side of the creek.
6. September 23–24. Seven more trips. A cold front drops the night to −2 °C and buys time. Two smokehouse loads start on the 24th.

**Build and test.** Code: `hunt.butcher.Dressing` (steps and deadlines; step times sit in the species file's `carcass` block), `ecosystem.carcass.CarryHandler` (loads) and the kill-site predator clock in `ecosystem.carcass.CarcassLifecycle`; debug `/yoi carcass <target> info` prints stage, core temperature, freshness and the predator clock. *Tests:* a moose gutted at 30 minutes and quartered at 3.5 hours on a 12 °C day keeps all 180 kg of meat Fresh; quartered at 5 hours, 54 kg is Turning; a hide drag moves 80 kg on snow at 30% walking speed; over 1,000 simulated fall kills near a bear range, 55–70% draw a bear within 24 game hours.

## Fishing in depth

Fishing is the steadiest food in the valley, and every species behaves, bites and fights differently, so catching more means learning each fish and reading the water.

### Fish species

| Fish | Where | When | Takes | Size | How it fights | Food |
| --- | --- | --- | --- | --- | --- | --- |
| Arctic grayling | Clear riffles, creeks below lakes | May–Sep; spawns in creeks at breakup | Insects, worms, small flies | 0.3–1.2 kg | Jumps, quick short runs | ≈ 500 kcal |
| Northern pike | Weedy lakes, sloughs | All year; in the shallows in May | Minnows, fish chunks, spoons | 1–10 kg, rarely 15 | Head-shakes; teeth cut line without a wire leader | 2,000–15,000 kcal |
| Chinook (king) salmon | Main river channel | Late June–July | Roe, spinners; mostly nets and spears | 8–20 kg | Long heavy runs | 20,000+ kcal |
| Chum salmon | Main river, side channels | July–August | Nets, spears, the fish wheel | 3–6 kg | Strong, dogged | 6,000–10,000 kcal; the best wolf food |
| Coho salmon | Clear side streams | August–September | Spinners, roe | 3–5 kg | Acrobatic | ≈ 8,000 kcal |
| Burbot | Deep river holes | All year, best Nov–Mar; spawns under ice in Feb | Fish chunks on night set lines | 1–4 kg | Slow, heavy, twists | ≈ 4,000 kcal; liver oil for lamps |
| Humpback whitefish | Lakes, slow river | All year; fall spawning runs | Small baits; mostly nets | 0.5–2 kg | Weak | ≈ 1,500 kcal |
| Sheefish | Big river, the big lake | Summer and fall | Spoons, big baits | 3–15 kg | Strong, jumps | 15,000+ kcal; a trophy |
| Lake trout | Deep water in the big lake | All year; best through the ice | Jigs, fish chunks fished deep | 2–10 kg | Dives deep and holds | ≈ 8,000 kcal |
| Longnose sucker | Slow water | May–Sep | Worms | 0.5–1.5 kg | Weak | ≈ 1,000 kcal; easy but bony |
| Slimy sculpin | Rocky shallows | All year | Minnow traps only | Tiny | — | Bait for burbot and lake trout |

**Species data.** Each row above is one entry in `data/yearofice/fish/<id>.json`, and the caught item and the entity share the species id (RECONCILE 46, 47; every species with its ids is in Waters and fish › Species ledger). The bite roll in Wildlife and fish and the fight in Fishing methods read only these numbers, so no species is hard-coded.

| Fish | b (bites per min) | kcal per kg | Run pull (kg per kg of fish) | Runs every | Run length | Jump per run | Special move | Tires in (s at full pressure) | Bite-through without leader |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Arctic grayling | 0.12 | 800 | 1.2 | 2–5 s | 0.5–1.5 s | 30% | — | 8 | 0% |
| Northern pike | 0.015 | 1,500 | 0.6 | 4–9 s | 1.5–3 s | 5% | Head-shake every 2–5 s, spike ×1.4 | 25 | 50% |
| Chinook salmon | 0.01 | 1,600 | 0.5 | 8–14 s | 6–10 s | 10% | Long run | 90 | 0% |
| Chum salmon | 0.02 | 1,700 | 0.5 | 6–12 s | 2–4 s | 5% | Bores and holds | 60 | 0% |
| Coho salmon | 0.04 | 2,000 | 0.6 | 4–8 s | 1.5–3 s | 40% | — | 45 | 0% |
| Burbot | 0.03 | 1,600 | 0.35 | 10–20 s | 1–2 s | 0% | Twist: spike ×1.2 every 2 s | 30 | 10% |
| Humpback whitefish | 0.03 | 1,500 | 0.4 | 8–15 s | 1–2 s | 0% | — | 10 | 0% |
| Sheefish | 0.005 | 1,600 | 0.6 | 5–9 s | 2–4 s | 35% | — | 50 | 0% |
| Lake trout | 0.01 | 1,600 | 0.45 | 8–15 s | 2–5 s | 0% | Dive and hold | 40 | 5% |
| Longnose sucker | 0.08 | 1,000 | 0.4 | 10–20 s | 1 s | 0% | — | 6 | 0% |

**Bait multipliers.** These fill m\_bait in the bite formula. Any bait not listed for a species counts 0.2, the doc's wrong-bait value.

| Fish | Baits and multipliers |
| --- | --- |
| Arctic grayling | Fly 1.2, insect 1.0, worm 0.6, roe 0.6 |
| Northern pike | Live minnow 2.0, spoon 1.2, fish chunk 1.0 |
| Chinook salmon | Roe 1.0, spinner 1.0, spoon 0.6 |
| Chum salmon | Roe 1.0, spinner 0.6 |
| Coho salmon | Spinner 1.2, roe 1.0, spoon 0.6 |
| Burbot | Fish chunk 1.0, live sculpin 1.0, guts 0.6 |
| Humpback whitefish | Insect 1.0, roe 1.0, worm 0.6 |
| Sheefish | Spoon 1.0, live minnow 0.6 |
| Lake trout | Jig 1.0, fish chunk 1.0, live sculpin 0.6 |
| Longnose sucker | Worm 1.0, insect 0.6 |

**Size draw.** A hooked fish's weight is drawn from the spot's RNG and skews small:

```latex
kg = kg_{min} + (kg_{max} - kg_{min}) \cdot u^{2} \cdot \left(0.7 + 0.3 \cdot \frac{N}{K}\right)
```

Here u is uniform from 0 to 1 and N/K is the spot's stock fraction from Fish stocks, so a fished-down spot gives smaller fish. At spots with q of 0.7 or more, 2% of pike and sheefish draw from a trophy band instead: pike 10–15 kg, sheefish 12–15 kg. Baits deeper than 2 m and 5 m draw bigger fish (Waters and fish › Rules the video rows needed).

**Food value.** The kcal per kg column times live weight gives the flesh eaten roasted. It reproduces the Food column above: a 4 kg pike is 6,000 kcal, a 14 kg chinook 22,400. Heads and frames stewed add 15% (Food systems), and roe and burbot liver count separately (Handling the catch). Salmon in the last third of their run window are lean: kcal ×0.7.

**Season presence.** m\_season is 1.0 inside a species' When window and 0.5 in its first and last month. Outside it, salmon are absent (0) and resident fish count 0.3 at that spot, because they have moved (Fish stocks, winter shift). Pike in May weed shelves count 1.5.

```json
{
  "id": "arctic_grayling",
  "kg": [0.3, 1.2],
  "bite_base": 0.12,
  "kcal_per_kg": 800,
  "run_pull_x": 1.2,
  "run_every_s": [2, 5],
  "run_length_s": [0.5, 1.5],
  "jump_chance": 0.30,
  "head_shake": null,
  "dive": false,
  "tire_per_s": 0.125,
  "bite_through_no_leader": 0,
  "bait": { "fly": 1.2, "insect": 1.0, "worm": 0.6, "roe": 0.6 },
  "season": { "from": "05-01", "to": "09-30" },
  "spawn": { "site": "creek_mouth", "from": "04-25", "to": "05-31" },
  "parasite": "plain"
}
```

The field tire\_per\_s is 1 divided by the "Tires in" column. Build: `ecosystem.fish.FishSpecies` reads `fish/<id>.json` through the bootstrap reader and the reload listener, and the linter rejects an entry missing any field or using a camelCase key. *Test:* a 10,000-roll check of the size draw gives a mean pike of 4.0 ±0.2 kg at a full stock, and no fish outside its kg range except trophies.

### Reading the water

Fish hold where current brings food and they can rest: pools below falls, seams between fast and slow water, eddies behind boulders, the outside of bends, lake inlets and outlets, weed edges for pike, and drop-offs for lake trout. Deep holes and drop-offs also hold bigger fish: in the size draw, a bait more than 2 m deep uses u^1.6 instead of u^2 and one over 5 m deep uses u^1.3, so a 4.0 kg mean pike averages about 4.5 kg on a deep drop-off, while shallow water still gives more bites (Waters and fish › Rules the video rows needed; video row 198). The surface gives clues: rings from rising fish, a jumping fish, terns and kingfishers diving, an otter slide, a bear standing at the falls in August. Good spots the player finds are logged in the messenger notebook.

**Three minor spot types.** The spot table in Wildlife and fish lists the major spots. The same stage adds three minor types that the paragraph above names, with the same q formula and ±20%:

| Minor spot | Generated where | Base q | Radius | Holds |
| --- | --- | --- | --- | --- |
| Seam | Every 80–150 m along a river run, where a fast and a slow cell meet | 0.30 | 3 m | Grayling, whitefish |
| Boulder eddy | Behind each boulder left by erosion in water over 0.6 m deep | 0.30 | 2 m | Grayling |
| Deep river hole | Main-river cells over 3 m deep | 0.50 | 6 m | Burbot, whitefish, sucker; burbot best Nov–Mar |

Major spots get a radius too: 4–8 m for a pool below falls (the falls rule), 5 m for creek mouths and bends, 6 m for inlets and outlets, 8 m for weed shelves and drop-offs, and the whole pond for a beaver pond.

**Where the bait sits.** The bite roll gets one more factor, m\_place: 1.0 with the bait inside a spot's radius, 0.3 within twice the radius, and 0.05 elsewhere. So a cast into featureless water rarely pays, and reading the water is the skill that fills the larder.

**Clues the player can see.** Each clue is a real object or effect in the world, spawned from the spot's state:

| Clue | Appears when | Seen from | What it means |
| --- | --- | --- | --- |
| Rise rings | Grayling or whitefish feeding at the surface; one ring a minute per 10 fish in stock, ×3 during a hatch | 40 m | Surface feeders; more rings mean a fuller stock |
| Jumping fish | A salmon run passing, or sheefish on summer evenings | 150 m | A run is in that stretch |
| Diving terns | Lakes and the big river, May–August, over spots with q of 0.6 or more | 300 m | Small fish near the surface, and predators below them |
| Kingfisher | Creek spots with q of 0.5 or more, May–September | 100 m | A good creek spot |
| Otter slide | A mud chute on the bank at the otter's home spot | 20 m | A good spot, and a rival taking 1 fish a day |
| Bear at the falls | August, during the chum run | 500 m | Salmon in the pool, and danger |
| Fish shadows | Turbidity under 0.3, within 8 m, player still for 2 s, sun not in the player's face | 8 m | Shapes over 1 kg; needed to aim a spear |
| Weed beds and lily pads | Summer, on weed shelves | 50 m | A pike edge |
| Open leads in winter ice | Over inflows and springs | 100 m | Oxygen, so fish gather; also thin ice |

**Watching the water.** Crouching still for 5 s while looking at water within 40 m names the nearest clue. The messenger says it in one line: "Rise rings at the seam. Grayling feeding." The spot then gets a notebook entry and a map pin.

**Learning a spot's worth.** After three catches at one spot, its pin shows a quality word from q: Poor (under 0.3), Fair (0.3–0.6), Good (0.6–0.8) or Excellent (over 0.8). The pin also lists the species caught, the bait that worked best, the last catch date and the stock word from Fish stocks. Fishing pins are their own map layer and do not use the 20 waypoints.

**Seasons move the fish.** From December to March a spot under 1.5 m deep keeps 30% of its fish. The rest move to the nearest deep hole, drop-off or inflow, whose capacity doubles for the winter (Fish stocks). Pins show the shift once a fish is caught in the new place, so the summer map must be relearned for the ice.

Build: `hunt.fishing.BiteModel` (m\_place, watching), with clue spawning in `ecosystem.fish.SpotClues`. *Test:* on 20 seeds, a bot casting only into spot radii catches at least 5 times as many fish per hour as one casting at random open water.

### Conditions

| Condition | Effect on bites |
| --- | --- |
| Dawn and dusk | ×1.5 for most fish |
| Night | ×2 for burbot; ×0.5 for grayling |
| Overcast or light rain | ×1.2; insect hatches bring grayling to the surface |
| Air pressure falling before a storm | ×1.4 for a few hours, so the forecast is a fishing tool |
| Bright midday sun | ×0.6 in the shallows; fish go deep |
| High, muddy water after rain or breakup | ×0.4, except bait on the bottom for burbot |
| Water under 4 °C | Slower bites, sluggish fights |
| A spawning run at that spot | ×3, but late-run salmon are lean |

**How the table fills the formula.** The bite roll in Wildlife and fish gains three factors from this doc: m\_water (below), m\_stock (Fish stocks) and m\_place (Reading the water).

```latex
P_{bite} = b_{species} \cdot q_{spot} \cdot m_{bait} \cdot m_{time} \cdot m_{weather} \cdot m_{season} \cdot m_{water} \cdot m_{stock} \cdot m_{place}
```

The product of m\_time, m\_weather, m\_season and m\_water is clamped to 0.1–3.0, so stacked bonuses never run away. Each species at a spot rolls separately each game minute; the first success is the bite.

| Factor | Values |
| --- | --- |
| m\_time | Dawn and dusk (the 2 game hours around sunrise and sunset) 1.5; 11:00–15:00 0.7, or 0.6 at spots under 1.5 m deep on clear days; night (sun below −6°): burbot 2.0, grayling 0.5, others 0.8; otherwise 1.0 |
| m\_weather | Clear 1.0; cloudy or rain 1.2; storm 0.5; snow 1.0; blizzard 0.3. During the 12 game hours of falling pressure before a front, 1.4 replaces the value |
| m\_season | From Fish species: 1.0 in window, 0.5 in its first and last month, ×3 at a spawning run |
| m\_water | Turbidity factor × temperature factor, below |

Midnight sun matters here: from late May to mid-July the sun never drops below −6°, so there are no night hours. Burbot set lines are a fall and winter job.

**Water temperature.** Water follows the 5-day mean air temperature, floored at 0.5 °C. Creeks run 2 °C colder than that and lakes 2 °C warmer in summer, and water under ice holds at 1–3 °C. Below 4 °C bites are ×0.7, run pull ×0.7, and fish tire ×0.8 as fast, so fights are soft but long. Above 18 °C, grayling in shallow spots bite ×0.6 and lake trout hold only at drop-offs.

**Turbidity.** Each water body has turbidity from 0 to 1. It rises 0.3 for a rain day and 0.6 for a storm day, jumps to 1.0 at breakup, and falls 0.15 a dry day; lakes move at half that speed.

```latex
m_{turbid} = 1 - 0.6 \cdot turbidity
```

Fully muddy water gives the table's ×0.4. Bait on the bottom for burbot ignores turbidity. Spears, fish arrows and fish shadows need turbidity under 0.3.

**Insect hatch.** On cloudy or rain days from June to August, with water at 8–16 °C, grayling and whitefish bite ×1.5 on flies and insects. Rise rings triple, which is how the player sees a hatch.

**Under the ice.** Dawn and dusk give 1.2 instead of 1.5, because light under ice is dim. Snow over 30 cm on the ice darkens the water: lake trout ×0.8, burbot ×1.2.

**A fishing forecast.** The messenger's forecast adds one word for tomorrow: Poor, Fair, Good or Excellent. It comes from tomorrow's best hour at the player's three most-used spots, using the forecast weather, so it is right as often as the forecast (80%). Example: "Fishing tomorrow: Good. Pressure falling after noon."

Build: `hunt.fishing.BiteModel` reads water state from the Environment field (`env.water`) and weather from `story`. *Test:* over 1,000 simulated days, the mean bite rate on falling-pressure afternoons is 1.4 ±0.05 times the same hour on steady days.

### Tackle

| Tackle | Holds | Made from | Note |
| --- | --- | --- | --- |
| Plant-fiber line | 4 kg | Fireweed or nettle cordage | Weaker when wet |
| Sinew line | 7 kg | Sinew from big game | Hide-and-bone tier |
| Monofilament | 10 kg | Supply drop only | The best line |
| Wire leader | — | Snare wire | Without one, a pike bites through half the time |
| Bone gorge | — | Bone sliver | Swallowed; loses 20% of fish on the strike |
| Steel hook | — | Iron tier | Loses 5% |
| Spoon lure | — | Tin or aluminum from the plane wreck | Pike and sheefish |
| Fly | — | Feathers and hair on a small hook | Grayling |

A willow rod flexes and soaks up sudden jerks; a hand line does not, so line snaps come sooner. With no reel, a big fish is fought by walking the bank to follow it.

**What the fight reads from tackle.** Four numbers: the line's strength, the rod's flex, the retrieve speed and the hook's loss on the strike. Tension 100% on the fight's arc is always the line's strength right now:

```latex
S_{eff} = S_{rated} \cdot w_{wet} \cdot \left(0.7 + 0.3 \cdot \frac{condition}{100}\right)
```

**Lines.**

| Line | Holds dry and new | Wet | Wear per fight | Length | Made from | Station, time, kcal |
| --- | --- | --- | --- | --- | --- | --- |
| Plant-fiber | 4 kg | ×0.75 | −3% condition; −9% if the arc reached red | 20 m on a rod | 20 m of cordage, twisted tight | Hands, 45 min, 75 kcal |
| Sinew | 7 kg | ×0.85 | −2% | 15 m | 0.2 kg dried sinew | Hands, 1 h, 100 kcal |
| Monofilament | 10 kg | ×1.0 | −1% | 100 m spool | Supply drop only | — |

A line wets after 10 minutes in use or any rain, and dries in an hour by a fire. Plant-fiber strength also depends on Cordage practice: line twisted while Clumsy rates 2.8 kg, and 4 kg only when Practiced (Crafting and progression). A snapped line loses everything past the break, hook and bait included.

**Rods and retrieve.**

| Gear | Flex | Cast range | Line carried | Retrieve speed | Retrieve pull | Made from | Station, time, kcal | Tier |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Hand line | 0 | 8 m, thrown | 30 m on a winder stick | 0.5 m/s, hand over hand | +1.5 kg | 30 m cordage (or the crate's 10 m cord) on a stick | Hands, 5 min, 8 kcal | Start |
| Willow rod and winder | 0.3 | 15 m | 20 m, plus 10 m on the winder | 0.7 m/s | +1.5 kg | A 3 m willow shoot, 6 bark line guides, 0.5 m cordage | Knife, 15 min, 25 kcal | Start |
| Willow rod and wooden reel | 0.3 | 20 m | 40 m | 1.0 m/s | +1.0 kg | A birch block, 0.1 kg iron axle | Workbench, 3 h, 300 kcal | Homestead |
| Steel rod and spinning reel | 0.5 | 35 m | 100 m | 1.4 m/s | +1.0 kg, with drag | Supply drop only | — | — |

Flex is the share of every tension spike the rod soaks up. Holding rod lift adds 0.2 more flex but halves retrieve speed. Below −20 °C a willow rod turns brittle: each spike over 80% has a 2% chance to snap it.

**Drag.** Only the spinning reel has one. It pays out line whenever tension passes the drag setting (default 80% of S\_eff; the mouse wheel moves it in 10% steps). Only spikes faster than the drag, from jumps and head-shakes, can then snap the line.

**Hooks.**

| Hook | Loses on the strike | Strike window after the dip | Made from | Station, time, kcal | Tier |
| --- | --- | --- | --- | --- | --- |
| Wood gorge | 30% | 0.2–0.8 s | A spruce splinter | Knife, 3 min, 5 kcal | Start |
| Bone gorge | 20% | 0.2–0.8 s | A bone sliver | Knife, 10 min, 17 kcal | Bushcraft |
| Steel hook | 5% | 0–0.6 s | 0.1 kg iron makes 10 | Furnace, 1 h, 450 kcal | Iron |

A gorge must be swallowed, so the strike comes a beat late. Once set, a gorge holds through 6 s of slack instead of 3, its one advantage.

**Terminal tackle and lures.**

| Item | Effect | Made from | Station, time, kcal | Tier |
| --- | --- | --- | --- | --- |
| Wire leader | No bite-through; sheefish and lake trout bite ×0.8 in clear water | 0.3 m snare wire | Hands, 2 min, 3 kcal | Start, once wire is held |
| Bobber | Shows a bite at range. Without one, only the rod-tip twitch shows it and the window shrinks to 0.4 s | A roll of birch bark | Hands, 2 min, 3 kcal | Start |
| Stone sinker | Holds bait on the bottom in current up to 1 m/s; needed for burbot and suckers | A grooved pebble | Hands, 3 min, 5 kcal | Start |
| Spoon | Pike, sheefish; jigged for lake trout | 0.1 kg cut from an aluminum panel, 1 steel hook | Workbench, 30 min, 50 kcal | Iron |
| Spinner | Chinook, coho | A tin or aluminum blade on a wire shaft, 1 steel hook | Workbench, 40 min, 67 kcal | Iron |
| Fly | Grayling and whitefish; lasts 10 fish | 3 feathers and hair on a small steel hook | Workbench, 15 min, 25 kcal | Iron |
| Jig | Lake trout and burbot through the ice | A bone or antler body, 1 steel hook | Workbench, 20 min, 33 kcal | Iron |

**Landing gear.**

| Item | Effect | Made from | Station, time, kcal | Tier |
| --- | --- | --- | --- | --- |
| Club | Secures a flopping fish at once | Any stick | — | Start |
| Stringer | Keeps up to 6 fish alive in the water for 12 h, so they do not spoil; otters raid it at 5% an hour | 1 m cordage | Hands, 2 min, 3 kcal | Start |
| Landing net | Lands from 3 m at any depth: 95% on a tired fish | A 0.5 m willow hoop, 6 m cordage | Hands, 1 h, 100 kcal | Bushcraft |
| Gaff | Lands fish to 20 kg from a bank or an ice hole: 90% | An iron hook on a 1.5 m pole | Furnace, 30 min, 225 kcal | Iron |

**Bait.**

| Bait | Where it comes from | Lasts on the hook |
| --- | --- | --- |
| Worm | Dug in riparian or bog soil: 2–6 per 10 min, doubled for a day after rain; none once the ground freezes in October | 2 bites or 30 min |
| Insect | Grasshoppers by hand in meadows, June–August, 1 per 2 min | 1 bite or 20 min |
| Fish chunk | 0.1 kg of any fish | 3 bites or 2 h |
| Roe | One cluster from a female salmon; dried a day, 3 bites | 2 bites or 1 h |
| Live sculpin or minnow | The minnow trap; kept alive in a bucket | Until taken; dies in 1 h out of water |
| Guts | Cleaning fish or game | 2 bites |
| Spoon, spinner, fly, jig | Lures | Not used up; lost with a snapped line |

A strike outside the window leaves the bait gone 60% of the time.

Build: `hunt.fishing.Tackle` and the `yearofice:tackle` component; tackle ids, recipes and keys are in Waters and fish › Fishing ids, tackle and keys. *Test:* a line rated 4 kg snaps in the fight simulator at 4.0 kg dry and new, 3.0 kg wet, and 2.1 kg wet at condition 0.

### Fishing methods

| Method | Effort | Best for | Note |
| --- | --- | --- | --- |
| Rod and line | Active | Everything | The fishing minigame |
| Hand line | Active | Ice holes, dropping deep | Short casts only |
| Set line | Passive, overnight | Burbot, pike | Check daily; otters and bears steal from it |
| Gill net | Passive | Whitefish, salmon | Needs 4 joined segments; tears in moving ice. A coarse net set along a lake weed edge also takes northern pike (c 0.03 an hour; Gill nets below; video row 189) |
| Fish trap or rock weir | Passive | Grayling, whitefish in creeks | A weir across a creek mouth funnels fish into a willow basket |
| Spear or fish arrow | Active, by sight | Salmon in shallows, pike in weeds | Clear water only |
| Fish wheel | Passive, big | Salmon runs | Homestead tier; can overharvest a run |

**Ice fishing.** Chiseling a hole takes about 20 minutes through 30 cm of ice and over an hour through a meter. Below −20 °C an open hole skins over within an hour. The player jigs actively, or sets tip-ups: lines under the ice with a spruce-bough flag that pops up on a bite. In winter, fish gather in deep holes and near inflows, where the water carries more oxygen.

**Fight profiles.** Each species sets how the minigame plays: how hard and how often it runs, its chance to jump (which throws slack), the pike's head-shakes, and the deep dives of lake trout and burbot. A grayling is quick and forgiving; a 10 kg pike on plant-fiber line is a real test.

**The fight, as built.** This spells out the five phases in Food systems as a control scheme. The Java build reads keyboard and mouse only; the touch column is kept for a later port. From the strike until the fish is landed or lost, the game clock runs at a tenth of its speed for that player (the close clock, kept per player so co-op can follow after 1.0; RECONCILE 115). Without that, a one-minute fight would cost a game hour at the default 24 real minutes a day (the world option allows 24–72; RECONCILE 117).

| Phase | Keyboard and mouse | Touch | What the player sees and hears |
| --- | --- | --- | --- |
| Cast | Hold right mouse 0–1.5 s to charge; release to cast | Hold Action, release | The rod loads back as it charges; the bobber or lure lands where the arc ends, scattered ±10% in range and ±5° |
| Wait | Mouse moves the rod tip; tap left mouse to twitch the bait. With a lure, hold left mouse to retrieve | Drag to move the tip; hold Action to retrieve | Bobber and rod tip; a nibble shakes the bobber without pulling it under |
| Strike | Click left mouse inside the hook's window after the dip | Tap Action | The bobber goes under; with a lure, a hard tug |
| Fight | Hold left mouse to retrieve, release to give line; mouse left and right set side pressure; hold right mouse to lift the rod; W A S D walk the bank at half speed | Hold Action; drag left and right; a second finger lifts | Tension arc, rod bend, the line's angle in the water, splashes |
| Land | Right mouse to grab or net when in reach; right mouse again, or left mouse with the fish priest, within 4 s on the bank to secure | Use button | A keycap prompt: "Grab" or "Net" |

A nibble is a false bite. Striking at one spooks the spot: bites there ×0.5 for 10 game minutes.

**Fish states.** A hooked fish runs a small state machine, stepped 20 times a real second in lockstep on client and server (`sim.fishing.FightModel`, Minecraft development › Player systems and client):

1. **Hold.** It pulls 0.3 × its kg and swims away at 0.2 m/s.
2. **Run.** Starts at random at the species' "Runs every" rate, scaled by stamina. Pull is run\_pull\_x × kg × (0.4 + 0.6 × stamina), at 1.0 m/s plus 0.1 m/s per kg, up to 3 m/s.
3. **Jump.** Rolled at each run's start. The fish is airborne for 0.8 s with zero tension, then lands with a spike.
4. **Head-shake, twist or dive.** The species' special move, from Fish species. A dive pulls at 0.6 of run pull toward the bottom, and the retrieve gains nothing during it unless the rod is lifted.
5. **Tired.** Below 15% stamina: no runs, pull 0.1 × kg, and the fish rolls to show its flank. Under 10% tension for 5 s, it recovers to Hold.

**Tension.** Retrieving adds the tackle's retrieve pull to the fish's pull. Giving line lets it slip at the fish's swim speed under 0.4 of the fish's pull (0.5 for a hand line through the fingers). With all line out, tension is 1.5 × the fish's pull whatever the player does, so the player must walk after a big fish, as Tackle says.

```latex
T = \begin{cases} P_{fish} + R_{pull} & \text{retrieving} \\ g \cdot P_{fish} & \text{giving line} \\ 1.5 \cdot P_{fish} & \text{all line out} \end{cases}
```

**Spikes.** A run's start (×1.3), a jump's landing (×1.6, or ×1.3 if the player was giving line), a pike's head-shake (×1.4) and a burbot's twist (×1.2) multiply tension for 0.15 s. Flex soaks up its share of the excess:

```latex
T_{spike} = T \cdot \left(1 + (x - 1)(1 - flex)\right)
```

**Stamina.** Stamina falls with pressure and rises without it. Side pressure matters: holding the rod angled against the fish's direction tires it 1.5 times as fast, in line 1.0, and to the same side 0.5. The line's angle in the water shows which way the fish is heading.

```latex
\frac{d\,stamina}{dt} = - tire \cdot \frac{T}{runPull_x \cdot kg} \cdot a_{side}
```

Below 20% of the fish's run pull, stamina recovers at a quarter of that rate. A weak player (under 3 energy bolts) retrieves at ×0.7 speed, and hands below their safe-to limit (Clothing) retrieve ×0.8 and lose 0.1 s from the strike window.

**How a fish is lost.**

| Loss | Rule |
| --- | --- |
| Snap | Tension reaches 100% of S\_eff |
| Spit | Tension under 5% for 3 s in a row (6 s with a gorge); 1 s of tension resets the count |
| Bite-through | Rolled at the hook set from the species' bite-through chance unless a wire leader is on; it happens at the first head-shake |
| Snag | A diving fish given line for over 3 s at a spot with snags wraps the line. The player walks 5 m to either side and retrieves: 60% frees it, otherwise the line parts |
| Flop-back | On the bank the fish flops toward the water at 0.4 m/s; if it is not secured within 4 s and is within 1.5 m of the water, it is gone |

**Landing.** The fish must be tired and within 2 m in water under 0.5 m deep. Grabbing a tired fish works 90% of the time, an untired one 30%, and a failed grab triggers a run. A landing net or gaff extends this as in Tackle. Through ice, a hole chiseled to 20 cm passes fish up to 5 kg; bigger fish need a 30 cm hole or a gaff.

**What the player sees.** A thin arc around the crosshair shows tension from 0 to 100%: white under 70%, amber to 90%, red above, and a flash on each spike. Time in the red wears the line three times as fast. Slack turns the arc grey, with one tick filling per second toward the spit.

There is no stamina bar. The fish's state shows in the world: line hiss and a wake on a run, spray on a jump, a silver flank when tired. The first-person rod bends with tension and creaks above 70%. "18 / 20 m" shows under the arc only when more than 75% of the line is out.

**Assist.** Realism settings add a fishing assist, on by default in Gentle. It shades the safe band on the arc, sets side pressure automatically and makes fish tire 30% faster.

**Passive gear.** Each trap or net rolls on its own and is settled in closed form at dawn, dusk and chunk catch-up when the player is away (Waters and fish › Passive gear while away):

| Gear | Rolls | Catch chance or rate | Holds | Rules |
| --- | --- | --- | --- | --- |
| Set line | Each game hour, per baited hook (up to 5) | min(0.5, 30 × P\_bite) | 1 fish a hook | Worm bait lasts 12 h, chunks 24 h. A hooked fish escapes at 10% an hour (5% on a gorge). Otters and bears steal at 5% an hour where their range covers the spot. Checking takes 2 game minutes a hook |
| Gill net | Each game hour | Poisson with mean c × q × coverage × m\_season × N/K | 8 fish a segment | See below |
| Weir and basket | Each game hour | Mean 0.2 × q × m\_season × N/K, grayling and whitefish | 15 | Empty twice a day; otters raid at 5% an hour |
| Minnow trap | Each game hour | 0.15 × q sculpin | 10 | Sculpin stay alive as live bait |
| Fish wheel | Each game hour | Mean 1.0 × q × today's passage ÷ a mean run's peak passage | 30 in the box | Empty every 12 h; a full box is a scent point |
| Tip-up | Each game minute, as rod fishing ×0.8 | The flag pops up, visible from 200 m | 1 | Reach it within 30 game minutes or the fish escapes 50% of the time; burbot hold on |

**Gill nets.** A segment is 3 m long and 1.5 m deep, knotted in fine mesh (5 cm) or coarse (12 cm). Coverage is the joined length over the channel's width, or over 30 m in a lake, capped at 1. Fewer than 4 joined segments never roll, as Wildlife and fish says.

| Species | c per hour | Mesh |
| --- | --- | --- |
| Humpback whitefish | 0.10 | Fine |
| Longnose sucker | 0.05 | Fine |
| Arctic grayling | 0.02 | Fine |
| Chum salmon | 0.10 | Coarse |
| Coho salmon | 0.06 | Coarse |
| Chinook salmon | 0.03 | Coarse |
| Northern pike | 0.03 | Coarse |
| Sheefish | 0.02 | Coarse |

A fish dead in the net over 12 h has a 25% chance of being eaten by an otter or bear, and spoils on the meat clock at water temperature. A segment tears 5% of days in high water (turbidity over 0.6), 10% when it catches a beaver or otter, and always in moving ice. Mending takes 30 game minutes and 2 m of cordage. Setting from shore with an anchor stone and a pole takes 30 minutes a segment, 15 from a raft or canoe. Under ice, the net goes in through holes 3 m apart, two a segment, pulled through with a line.

**Ice fishing in practice.** The hatchet chisels at the rate above (20 minutes through 30 cm); an ice chisel halves it. Below −20 °C, reopening a skinned hole takes 1 minute with a bark ladle, or 3 by hand with wet mittens. A player can run up to 6 tip-ups, each pinned on the map. Jigging is a left-mouse tap: one every 2–4 s gives lake trout and burbot ×1.3, while tapping faster than once a second gives ×0.7.

**Spear and fish arrow.** The fish is deeper than it looks. Its apparent depth is 0.75 of its true depth, so the player must aim low, and the game does not correct for it. A spear thrust takes 0.25 s. A walking player spooks fish within 2 m, a crouched or still one within 1 m. A hit holds 70% of the time on a flint point, 75% on bone and 90% on iron.

**Hand-grab.** At a falls pool with q of 0.7 or more during a run, salmon jump every 5–20 s. Standing at the lip, pressing right mouse within 0.4 s of a jump's peak catches one: 40% when Clumsy, 70% when Practiced at fishing. Fish stranded in a drained pool or below a broken dam are always caught.

Build: `sim.fishing.FightModel` is the pure step(state, input, dt) that client and server both run at 20 steps a second (payloads `fish_input`, `fish_state`, `fish_result`); `hunt.fishing` holds the hooked fish and ice fishing, `food.traps` the passive gear and the wheel's box, and `client.minigame` draws the arc in the `minigame` layer. *Test:* the step function, fed recorded inputs, gives identical outcomes on client and server, with 150 and 300 ms of added delay (JUnit `hunt/fight_lockstep`). A bot that always holds retrieve loses at least 60% of pike over 5 kg on plant-fiber line to snaps.

### Handling the catch

A fish bled and cleaned within an hour keeps best; whole fish spoil on the same clock as meat. Every part has a use: fillets to eat or dry, heads for soup, roe for food and bait, guts for bait (and bear scent), salmon skin for waterproof bags, bones for hooks, and burbot liver for lamp oil.

**Cleaning steps.**

| Step | Base time | Tool | Effect |
| --- | --- | --- | --- |
| Kill and bleed | 0.5 game min | Club or knife | Cut gills within 10 min of landing: shelf life +25% |
| Gut | 2 game min | Knife | Gutted fish spoil on the meat clock; whole fish spoil twice as fast |
| Fillet or split | 3 game min, plus 1 per kg over 3 kg | Knife | Fillets to dry, split fish to smoke |
| Skin | 2 game min | Knife | Salmon skin for bags |

Gutting plus filleting is the 5-minute clean in Crafting and progression. Practice at most cuts it by 40% and a cleaning spot (Camp pieces) by 30%, so a practiced player at the cleaning spot cleans a fish in 2 minutes. A dull knife makes it ×1.5. A frozen fish takes 3 times as long, unless it thaws first by a fire, 1 game hour per kg.

**What a fish yields**, as a share of live weight:

| Part | Share | Use |
| --- | --- | --- |
| Flesh | 55% | Eating, drying, smoking; carries the species' kcal |
| Head | 10% | Soup |
| Frame (bones and tail) | 15% | Soup; a pike or salmon frame gives 3 bone slivers for gorges and needles |
| Guts | 10% | Bait (one per 0.1 kg), garden fertility, wolf food at half value |
| Roe | 10–15% of a female salmon in the run, or a whitefish in fall; half of fish are female | Food at 1,400 kcal per kg, and bait |
| Liver | 10% of a burbot | Lamp oil, or eaten raw |
| Skin | 5% | Bags; otherwise left on the fillets |

**Burbot liver.** A 3 kg burbot has a 0.3 kg liver. Eaten raw it gives 1,500 kcal and counts as a vitamin C meal (Health). Rendered by the fire for 1 game hour it gives 0.14 kg of oil, which burns in a fat lamp at the lard rate: 8 hours of winter light.

**Guts and scent.** Cleaning makes a scent point (Bear rules), of strength 1 per kg of guts and heads left lying. Throwing them in fast current removes 80% of it but draws eagles downstream. Burying them 30 cm deep removes 50%, and burning them on a Hot fire or feeding them to the wolf removes all of it.

**Keeping fish.** A fish on a stringer stays alive and does not spoil for 12 h. Below −10 °C a whole fish freezes in an hour; frozen fish stack in a cache like cordwood and keep while they stay below −5 °C, where decay stops (Decay and time). Frozen raw fish eaten thin counts as raw fish for vitamin C: 1 C-day per 0.5 kg (Cooking). Fish frozen below −20 °C for 7 days carries no parasite risk.

**Salmon-skin bag.** Four salmon skins, 5 m of sinew thread and a bone needle; workbench, 2 game hours, 200 kcal. It holds 2 L and stays dry through rain and a capsized canoe. Tinder, char cloth and the messenger go in it.

**Feeding the wolf.** A kilogram of fish counts as a kilogram of meat toward the wolf's 1–2 kg a day. Chum is the best wolf food because a dried chum keeps for months and packs well on a sled.

Build: `hunt.butcher` handles fish parts too (ids in Waters and fish › Fishing ids, tackle and keys); cleaning is right mouse held with a knife. *Test:* 30 fish cleaned at a cleaning spot over 6 or more game days bring the time to 2.1 ±0.1 game minutes; the same 30 on bare ground stop at 3.0.

### Fish stocks

Each fishing spot holds a stock that every catch lowers and that refills over a season. Netting a creek mouth during the grayling spawn crashes the next year's run there. Salmon runs vary in size every year, and a fish wheel run all season shrinks the run four years later, when that year's offspring would have returned.

**Stock per spot.** Each spot holds a count N per species, with a capacity K. Wildlife and fish sets the starting stock at q times the type's capacity, so K is that value. A catch removes one fish, and a released fish counts as caught for 7 days.

| Spot type | Capacity per species at q = 1 |
| --- | --- |
| Pool below falls | Grayling 60, burbot 8 |
| Lake inlet or outlet | Grayling 20, whitefish 40, pike 6 |
| Creek mouth | Grayling 30 (×5 during the spawn) |
| Outside of a bend | Grayling 30, whitefish 20, sheefish 3 |
| Lake drop-off | Lake trout 12, burbot 6 |
| Weed shelf | Pike 14, sucker 30 |
| Beaver pond | Pike 8, grayling 20 below the dam |
| Seam, boulder eddy | Grayling 10, whitefish 6 |
| Deep river hole | Burbot 20, whitefish 25, sucker 15 |

**Effect on bites.** Rod fishing feels a thinning stock gently at first:

```latex
m_{stock} = \sqrt{\frac{N}{K}}
```

Nets, weirs and the fish wheel scale by N/K directly. They take fish without regard to the bite, which is why passive gear empties a spot fastest.

**Regrowth.** On the first day of each month, each stock grows toward K and gains a little from neighbouring water:

```latex
N \leftarrow \min\left(K,\ N + r \cdot N \cdot \left(1 - \frac{N}{K}\right) + i \cdot K\right)
```

| Species | r per month |
| --- | --- |
| Grayling, sucker | 0.5 |
| Whitefish | 0.3 |
| Pike | 0.25 |
| Burbot | 0.2 |
| Sheefish | 0.1 |
| Lake trout | 0.08 |

Immigration i is 0.05 for river spots, 0.02 for lake spots and 0 for a beaver pond with no outlet. From November to April, growth is ×0.25. A grayling pool fished down to 30% is back above 80% in about three months. Lake trout at a drop-off take two to three years, so the big lake rewards a light hand.

**Spawning sites.** Creek mouths hold grayling from breakup to the end of May, lake inlets hold whitefish in their fall run (September–October), and pike crowd weed shelves in May. A fish taken at a spawning site inside its spawn window counts toward that site's spawners taken, s. Next year's spawn capacity there is:

```latex
K_{spawn,next} = K_{spawn} \cdot \max\left(0.2,\ 1 - 2 \cdot \frac{s}{K_{spawn}}\right)
```

So netting half a grayling run at a creek mouth leaves next year's run there at 20%. It climbs back by 20% of K a year once the netting stops.

**Salmon runs.** Salmon have no resident stock. Each year draws a run size per species:

```latex
R = R_{mean} \cdot y \cdot e
```

| Species | R\_mean (fish passing within reach) | Window | Peak | Returns after |
| --- | --- | --- | --- | --- |
| Chinook | 200 | June 25 – July 31 | July 10 | 5 years |
| Chum | 1,500 | July 15 – August 31 | August 5 | 4 years |
| Coho | 400 | August 10 – September 30 | September 5 | 3 years |

The year factor y comes from the year's RNG, triangular from 0.5 to 1.6 with a mean of 1.0. Daily passage follows a triangle across the window, peaking on the peak date. Run strength is today's passage over the peak passage, and it sets the ×3 run bonus and the wheel's catch.

**The echo.** The harvest share h is every salmon the player took from a run, by any gear, divided by that run's R. It sets the factor e for the run that returns that many years later:

```latex
e = \max\left(0.4,\ 1 - 1.5 \cdot \max(0,\ h - 0.2)\right)
```

Taking up to a fifth of a run costs nothing later. A fish wheel turning all season takes about 30% of a chum run, so the run four years on is about 15% smaller. A wheel plus nets can halve it. The echo does not compound, because strays from other rivers refill a thin run.

The messenger warns once when h passes 0.2: "You have taken a fifth of this chum run." The salmon alert names the run's size from y × e: "Chum run at the bend: weak this year."

**Other takers.** An otter takes 1 fish a day from its home spot, and bears at the falls take about 5 salmon a day in August. Neither counts toward h. In a hard winter (the rare event), a beaver pond under 2 m deep loses 80% of its fish by March.

**Stock words.** Each pin shows Good above 0.7 of K, Falling at 0.4–0.7 and Poor below 0.4. A morning prompt fires when a used spot reaches Poor and an unfished one lies within 1 km: "Bites are slowing at the falls pool. The bend downstream is untouched."

```json
{
  "id": "spot_12_07_03",
  "type": "pool_below_falls",
  "q": 0.86,
  "radius_m": 6,
  "snags": 0.4,
  "stock": {
    "arctic_grayling": { "k": 52, "n": 31 },
    "burbot": { "k": 7, "n": 7 }
  },
  "spawn": null
}
```

Only spots the player has fished are written to the save as edits; untouched spots regenerate from the seed. Build: `ecosystem.fish.RunManager` for runs; spot stocks live in the ecology records, and every take calls `EcologyService.harvest`. *Tests:* a bot taking 10 grayling a day from one q 0.8 pool halves its bite rate within 14 game days, and the pool is back above 80% within 100 days of stopping. Over 100 simulated years with no player, the mean chum run is 1,500 ±5%; with a wheel run every season it settles 10–25% lower and never below 40%.

## Crafting and progression

Progression has five tiers, and each opens when the land gives the player something new, not when a menu says so.

&#91;embedded content: crafting tiers · 5 tiers, 4 gates\]

**How recipes unlock.** There is no tech-tree screen. A recipe appears in the messenger's notebook the first time the player holds any one of its inputs, as in Minecraft's recipe book. Each recipe names its station: hands, workbench, fire, furnace or forge.

**Better tools mean less effort, not new powers.** Upgrades cut the swings, minutes and calories a job costs:

| Job | Early tool | Upgraded tool |
| --- | --- | --- |
| Fell a spruce | Hatchet, 6 swings (stone axe 10) | Felling axe, 4 swings |
| Split a log | Hatchet, 3 strikes | Maul, 1 strike |
| Start a fire | Bow drill minigame | Flint and steel, 90% first try |
| Set a hook | Bone hook, loses 20% of strikes | Steel hook, loses 5% |
| Hold a snared animal | Cordage snare, breaks on 30% of catches | Wire snare, breaks on 5% |

Tools wear out and break; a hone or whetstone restores the edge meter (0–120), and a new handle or the forge restores durability. Losing a tool to the river or a bear raid is a real setback, not a respawn.

**Practice.** Repeated actions get up to 40% quicker: by the tenth bow-drill fire the minigame shrinks to a short hold, and cleaning a fish drops from 5 to 2 game minutes. Practice never lowers calories, cold or hunger, so the needs stay the clock. Long jobs (6 arrows, a wall course, quartering) can be queued and run at the work-until-done rate, 20 game minutes a real second.

**Crafting rules from player feedback.**

- **Nothing becomes obsolete.** Spruce, bark, cordage and the bow drill stay useful in every tier; a new tier adds activities and never removes one.
- **Every activity pays twice.** Hunting gives food and hides for gear; fishing gives food, bait and bones for hooks; exploring gives logbook pages that teach recipes, and ambitions.
- **Every upgrade has a target.** No tool is crafted for its own sake: the sinew-backed bow is for grizzlies and long shots, the parka for tundra trips, the canoe for regions upriver.
- **Repeat work gets faster.** Any building can be saved as a blueprint and stamped again, drawing materials from nearby storage. A fifth cabin wall takes a fraction of the first one's effort.
- **Quality of life.** Crafting pulls from nearby storage, storage can auto-sort, gear sits on a body diagram instead of in the bag, and pieces snap to Minecraft's 1 m block grid, with slabs and stairs for half steps.

**Materials.**

| Material | Where it comes from | Used for |
| --- | --- | --- |
| Logs, poles and firewood (green, then seasoned) | Felled spruce, birch, aspen and poplar, read by diameter and lean (Crafting and building in depth › Trees, felling and firewood) | Building, firewood |
| Birch bark | Birch trunks, once per tree per year | Tinder, roofing, containers, canoe skin |
| Plant fiber and inner bark | Willow, fireweed, nettle | Cordage, nets, snares |
| Spruce gum | Wounded spruce | Glue, canoe seams, fire extender |
| Stone and flint | Gravel bars, scree | Fire ring, stone tools, flint and steel |
| Clay | Creek banks, bog edges | Furnace, chinking, pots |
| Moss | Bog and forest floor | Chinking, insulation, dry tinder |
| Surface iron | Rock faces above 250 m | Everything in the iron tier |
| Hide, sinew, bone, antler, fat | Animals | Gear, bows, hooks, lard |
| Aluminum and wire | The bush-plane wreck | Radio repair, sled runners, snares |

## Shelter and building

Building snaps to Minecraft's 1 m block grid, but every piece costs logs, calories and game hours, so a base grows over weeks. A shelter's value is how much warmer and drier it keeps the player than the air outside.

| Tier | Example | Warmer than outside | Typical cost |
| --- | --- | --- | --- |
| 1 | Debris bed and lean-to by a fire | +3 °C, half the rain blocked | 1 game hour, sticks and boughs |
| 2 | Repaired outpost cabin | +8 °C, dry | 2 game days, 20 logs |
| 3 | Chinked log cabin with a stove | +25 to +35 °C while the stove burns | 2–3 game weeks, 120 logs, moss and clay |
| 4 | Cabin plus cache, smokehouse, woodshed, root cellar | Same, with food that keeps | A summer of work |
| 5 | Homestead: workshop, forge, greenhouse, boat landing | Same; work moves indoors in winter | Open-ended |

**Pieces.** Foundation, log wall course (1 m long), floor, roof panel, door, hide window, stove, chimney, shelf, bed. Each piece has an insulation value and a durability; snow on the roof adds insulation, and a roof without a chimney fills the room with smoke.

**Building controls.** Pieces show a ghost preview before placing, corner stones, gravel pads and cribs fill up to 2 blocks of drop on slopes, and anything placed can be taken down for a full refund within 1 game hour. Logs can be floated downriver to camp instead of carried.

**Warm rooms.** When walls, roof and door close a space, the game flood-fills it on the grid and treats it as one room. Room temperature = outside air + stove heat ÷ (the room's heat loss through its pieces). Gaps in chinking and open doors raise the loss.

**Logs are the bottleneck.** A green spruce log weighs about 40 kg and slows the player to 60% speed; one is carried at a time until a sled or a skid trail exists. Green wood must be split and dried under cover before it burns well: 10 days in a summer woodshed, 25 in fall, while unsplit logs in the open take a summer, so next winter's firewood is cut and split by September. Firewood is counted in armloads: one 20–30 cm tree gives 10, and a chinked cabin burns about 2 a winter day while a drafty one burns 5. A good cabin needs about 30 trees a winter and a poor one 75, so better building is the main way to shrink the wood chore, and the felling axe, bucksaw and maul cut a tree's work from 3.1 game hours to 1.2 (Crafting and building in depth › Trees, felling and firewood).

**Winter gear.** Clothing pieces stack warmth and protect body parts from frostbite:

| Item | Made from | Effect |
| --- | --- | --- |
| Fur mittens | 4 hare pelts, sinew | Hands safe to −25 °C |
| Beaver hat | 1 beaver pelt | +4 °C, ears safe to −30 °C |
| Mukluks | Moose hide, fur liner, sinew | Feet safe to −35 °C |
| Parka | 2 caribou hides or 1 bear hide | +15 °C |
| Snowshoes | Birch frame, rawhide | 90% walking speed in deep snow instead of 60% |

Clothing gets wet and wears out; wet clothing loses most of its warmth until dried. The full garment list, layers and the clothing HUD are in Clothing.

Full recipes, tiers, building pieces and the heat model: Crafting and building in depth

## Camp, materials and builds

Every structure is built from things the player gathers by hand, and each kind of gathering is its own small game with a right and wrong way to do it. Doing it right is faster and cheaper in calories, so skill shows in the numbers. Gathering costs about 300–500 kcal per game hour, which ties building to the food clock as it did in the video, where building alone made him lose weight.

### Gathering materials

| Material | Action | Tool and time | Yield | What makes it a game |
| --- | --- | --- | --- | --- |
| Sticks and deadfall | Pick up from the ground, snap over a knee | Hands, 2 s each | Kindling, short fuel | Dead lower branches on spruce are dry even after rain; ground sticks are wet for a day |
| Dead standing spruce | Fell and buck a grey, barkless tree | Hatchet, 20 game minutes | 6 armloads of dry firewood | The only firewood that burns well the day it is cut, so finding these is the early-game skill |
| Green logs | Fell: notch on the side it should fall to, then back-cut | For a 25 cm trunk: crate hatchet 6 swings, 24 game minutes; felling axe 4 swings, 10 minutes; then limb 10 and buck | One building log, 4 m and 40 kg | The tree falls toward the notch. A bad notch drops it on the player, hangs it up in another tree, barber-chairs it, or sends it into the river (Crafting and building in depth › Trees, felling and firewood) |
| Split firewood | Set a round on the chopping block and swing | Hatchet or maul, 1 swing if clean | 1 armload per 4 rounds | Aim at a crack in the end grain to split it in one swing; knots take three. Wet or green rounds split badly |
| Birch bark | Score with a knife and peel a sheet | Knife, 1 game minute | Tinder, roofing, containers | Once per tree per year. Peeling all the way round kills the tree, and the grove stops giving bark |
| Plant fiber | Strip fireweed or nettle stalks, or willow inner bark | Hands or knife, 3 game minutes | Fiber for cordage | Fireweed is best after the first frost; willow bark is best in spring when the sap runs |
| Cordage | Twist and reverse-wrap fiber; or split spruce roots for lashing | Hands, 2 game minutes per metre | 1 m of cordage | Practice raises strength. Weak cordage snaps on a snare or a lashing at the worst time |
| Spruce pitch | Scrape hardened gum from wounded spruce, melt with charcoal and fiber | Knife, a fire | Glue, seams, fire extender | Bears and porcupines wound trees, so pitch is found near their sign |
| Stone and flint | Pick from gravel bars and scree | Hands | Fire ring, tools, strikers | Flint must be knapped: strike the edge at the right angle to take a flake. A bad strike shatters the core |
| Clay | Dig at a cut creek bank | Shovel or digging stick, 10 game minutes | 1 bucket | Clay alone cracks when it dries; mixed with sand or grass it holds. Frozen ground can't be dug from October |
| Moss | Pull sheets from the forest floor or bog | Hands, 1 game minute | Chinking, insulation, dry tinder | Wet moss must dry for a day before it insulates |
| Mud bricks | Mix clay, mud and grass, press in a frame, dry in the sun | Frame, 3 dry days | Chimney and fireplace bricks | Rain before they dry ruins the batch; the video's chimney was mud brick |
| Surface iron | Pry nodules from rock faces above 250 m | Pick, 5 game minutes per nodule | Ore for the bloomery | Heavy (3 kg per nodule) and far from camp, so every trip is planned |
| Antler and bone | Find shed antlers in spring, or keep them from a kill | Hands | Hoes, needles, hooks, handles | Sheds are only on the ground in April and May before rodents gnaw them |

**Readable trees.** The plant guide tells trees apart at a glance: green spruce (building, fast fuel), dead standing spruce (dry fuel now), birch (bark and long-burning hardwood), willow (fiber, smoker fuel) and alder (smoker fuel). The video's lesson that softwood burns out while hardwood lasts the night is built into the fuel numbers.

### Making camp

A camp is a set of placed pieces, and where each piece sits matters as much as what it is. The first camp goes up in the first game day; it grows into a working camp over the first week, and the cabin comes later.

**Picking a site.** Holding the messenger and looking at the ground runs a site check with six readable results. None blocks building; each one that fails costs something later.

- **Flat and dry:** a slope over 10° or a hollow that pools rain soaks the bed.
- **Water close but not too close:** 30–100 m from the river. Closer means cold air, bugs and spring flood; farther means long water trips.
- **Out of the wind:** a spruce stand or rock on the north side keeps the fire alive and halves the chill.
- **Wood within 100 m:** dead standing spruce nearby saves hours every day.
- **Nothing overhead:** a dead tree or a hung branch over the bed can fall in a storm.
- **Off the game trail:** camping on a trail brings moose and bears through at night.

**Camp pieces.**

| Piece | Materials | Time | What it does |
| --- | --- | --- | --- |
| Debris bed | 2 armloads of leaves, grass or boughs | 20 game minutes | Off the cold ground: +3 °C while sleeping |
| Bough bed | 40 spruce boughs on a pole frame | 1 game hour | +5 °C, better sleep |
| Lean-to | 6 poles, cordage, boughs or bark on the roof | 1 game hour | Blocks half the rain and wind; facing the fire, it traps heat |
| Reflector wall | 4 stacked green logs behind the fire | 30 game minutes | Throws heat back at the bed: +4 °C |
| Cooking stand | A tripod or two forked sticks and a crossbar | 15 game minutes | Hangs the pot over the fire; the video's needed iron, here it needs only wood |
| Drying line | Cordage between two trees, near the fire | 5 game minutes | Dries clothes, socks and tinder in half the time |
| Woodpile | A raised log rack under bark | 20 game minutes | Keeps 10 armloads dry. The fire pulls fuel from it automatically when within 3 m |
| Chopping block | A round of 30 cm or more set on end, or any stump | 2 game minutes | Where rounds are split; splitting on the ground takes 30% longer |
| Cleaning spot | A flat stone or log | 2 game minutes | Where fish and game are cleaned. Placed at least 50 m downwind of the bed |
| Meat pole or food hang | A pole lashed between two trees, 4 m up | 30 game minutes | Keeps food from bears until the cache is built |
| Latrine | A dug hole | 10 game minutes | Placed downstream of the drinking spot, or the water source turns dirty |
| Chest or barrel | Logs or split planks | 30 game minutes | Storage beyond the pack; the video's camp used them to sort meat, building supplies and fishing gear |

&#91;embedded content: camp layout · 9 pieces around one fire · design target\]

The fire is the hub. Anything that smells (fish scraps, the smoker, the cache) sits downwind and away from the bed, and clean water is taken upstream of everything.

**How a camp grows.** Night 1 is a fire and a debris bed. Week 1 adds a lean-to with a reflector, a cooking stand, a drying line, a woodpile and a chopping block. Month 1 adds a smoker, a raised cache and a garden bed, and either the outpost cabin gets repaired or a cabin site gets picked. Each upgrade is optional, but each one gives back time or warmth every day after.

**Bad layouts are punished, readably.** Cleaning fish beside the bed brings a bear to the bed, as in the video. A woodpile downwind of the fire can catch a spark. Meat left on the ground draws foxes the first night. The messenger names the cause in the morning ("The bear came for the fish scraps by your bed"), so the player learns the layout from the land, not from a menu.

### Building a campfire

A campfire is built in layers, and each layer is something the player gathered. A fire with every layer right lights on the first try; skipping one is why it smokes and dies. The fire builds above (teepee, long fire, Dakota hole, stove) are shapes laid on top of these layers.

1. **Clear the ground.** Scrape 1 m to bare soil. On snow, lay a platform of green logs first, or the fire sinks and drowns itself.
2. **Ring it.** A ring of 8 stones stops embers from spreading into duff and moss. River stones still wet from the water can crack and spit when heated.
3. **Tinder bundle.** A fist-sized nest of birch-bark shavings, dry grass, fireweed fluff or hanging lichen. Each tinder has a dryness from 0 to 100; below 60 it will not catch. The bundle is crafted in hand, as in the video.
4. **Kindling.** Two handfuls of pencil-thin dead spruce twigs, then thumb-thick sticks.
5. **Fuel.** Wrist-thick split wood, then logs once there are coals.
6. **Light it** with one of the methods below, then blow on it (hold right mouse) to grow the flame.

| Method | Needs | Success | Notes |
| --- | --- | --- | --- |
| Matches | The 3 in the crate | 95% in calm air, 50% in wind | Three uses, then gone. Spent learning the bow drill is a waste |
| Bow drill | Spindle, fireboard, bow, socket, cordage; dry spruce or willow | Rhythm minigame: about 70% for a fresh player, 95% with practice | 60–90 s of steady strokes. Costs 60 kcal a try; fails with wet wood or very low energy |
| Flint and steel | Flint plus an iron striker, char cloth or fungus | 85% | The main method once there is iron. Char cloth is made in a closed tin on the fire |
| Carried ember | A coal packed in birch fungus or moss | 90%; lasts 4 hours in chaga, 6 in an ember carrier | Moves fire between camps without starting over |
| Battery and steel wool | The plane's battery and steel wool | 99% | A one-time find, kept for emergencies |

**Tending.** The fire has a fuel bar and a setting: Low, Steady or Hot, as in the video. A Hot fire boils water fast but eats wood three times as fast. Packing it too full shows "may spread" once, and then it can jump to anything within 2 m while the player is away, which burned the video's shelter. Banked coals under ash last the night and relight in seconds. Rain halves fire life unless there is a roof or a lean-to over it, and snow can put out a Low fire.

**What attaches to a fire.** A cooking stand or spit, a flat cooking stone, a drying rack, a reflector wall and, once built, a smoker. Placing them is a snap to the fire ring, so a camp kitchen comes together in a few clicks once the pieces exist.

### The smoker

Smoking turns a kill that would rot in 2 days into food that keeps for weeks or months, which is what turns a lucky week into a winter store. As in the video, a smoker burns firewood, works slowly, and gives food that lasts far longer than plain cooked meat. There are two builds and two methods.

| Build | Tier | Materials | Build time | Holds | Notes |
| --- | --- | --- | --- | --- | --- |
| Smoke rack | Bushcraft | 3 poles of 2.5 m, a willow lattice, cordage, spruce boughs or birch bark as a cover | 2 game hours | 10 kg of meat or fish | A teepee over a small smoky fire. Easy to build, loses heat and smoke in wind |
| Smokehouse | Hide tier | A 2 × 2 m log or plank shed, a fire pit 3 m away, a covered trench flue, clay to seal | 3 game days | 60 kg | The fire sits outside, so cold smoke is possible. A whole moose fits in two loads |

**Hot and cold smoking.**

| Method | Smoke temperature | Time | Keeps for | Best for |
| --- | --- | --- | --- | --- |
| Hot smoke | 60–80 °C | 8–12 game hours | 2–4 weeks | Fish, small game, eating soon |
| Cold smoke | Under 30 °C | 2–4 game days, then air drying | 3–6 months | Winter stores; needs the smokehouse |

**Doing it right.** Meat is cut into strips about 1 cm thick and fish are split and opened flat. Salt from the crate's crock, a supply drop or a mineral lick doubles shelf life but is optional (Open world in depth › Salt from the mineral lick). The fire must smolder, not flame. Green alder or willow gives the best smoke, birch is fine, and spruce makes the meat bitter and costs −1 Spirits at every meal. Smoked meat loses about 60% of its weight as water but keeps its calories, so a smoked moose is far easier to carry and store.

**What goes wrong.** A flaming fire cooks the outside and leaves the middle raw, losing 20% of the batch. A fire that goes out in summer lets flies lay eggs, and the batch spoils. The smoke carries: a full smoker draws bears from 1 km, so it is placed 30–50 m from the bed but in view of it. Each batch has to be checked and its fire fed every 4 game hours, so the player can queue the job and leave, but not for a whole day.

**Pemmican.** Dried meat pounded to powder and mixed with rendered fat and berries keeps for a year and packs about 5,600 kcal per kg. It is the food for long trips and the end of winter.

### Building a cabin

A first log cabin of 3.6 × 4.2 m takes about 160 game hours of work, roughly 20 working days, and burns about 70,000 kcal beyond normal living. That is three weeks of extra food, so the cabin is really a food problem: the player stocks a cache or smokes a moose before starting, or stops halfway when the weight drops. It needs about 45 wall logs and 75 smaller poles.

1. **Site and foundation** (6 game hours). The site check above plus two more results: level within 30 cm, and above the spring flood line, which shows as a mud mark on tree trunks. Four large corner stones or a gravel pad go down, and two sill logs are laid level on them.
2. **Fell and limb** (30 game hours). Straight spruce 25–30 cm thick from a stand within 200 m. Each felling is the notch-and-back-cut game above. Limbs come off with the hatchet and are saved as kindling.
3. **Haul** (25 game hours). A green log weighs 40 kg and moves one at a time. A cleared skid trail halves the time, rolling logs downhill is free, and in winter a sled on snow carries three at once. Real cabin builders cut in winter for this reason.
4. **Peel** (11 game hours). Bark comes off with a hatchet or a drawknife. Peeled logs dry faster, rot at 2% a year against 10%, and do not fill with beetles. From May 15 to July 15 a log peels in half the time and gives 2 bark sheets.
5. **Notch and lay the walls** (30 game hours). Each log is rolled onto the wall, scribed against the one below, rolled back, notched, and dropped in place. A saddle notch needs only a hatchet; a tighter dovetail needs iron tools. Butt ends alternate so the walls stay level. Above head height the player rolls logs up two skid poles with a rope, a short pull-and-brace game where a slip drops the log (10% a log, 2% when Practiced at Log building, halved with a cant hook).
6. **Openings** (4 game hours). The door and one window are cut through the finished walls, and frames are pegged in to hold the cut ends.
7. **Roof** (25 game hours). A ridge pole and purlins on log gables, then a layer of poles, then overlapping birch bark, then 20–30 cm of moss and sod. This is the old Alaskan sod roof: warm, heavy, and leak-proof once it settles.
8. **Chink** (12 game hours). Moss is packed between the logs from inside and daubed with clay outside. Every unchinked metre is a draft the cabin card lists.
9. **Door and window** (8 game hours). The door is split planks riven with wedges, on carved wood or rawhide hinges, with a bar inside against wolves. The window is scraped rawhide or a plexiglass panel from the plane wreck.
10. **Heat** (12 game hours). A stone and clay fireplace in the bushcraft tier, or a barrel stove made from scrap at the outpost in the iron tier. A chimney burning green spruce builds creosote; left uncleaned for a winter, it can catch fire.
11. **Inside** (6 game hours). A bunk with a bough mattress, shelves, pegs for drying clothes, and a fat lamp. Decoration and trophies add Spirits.

**The cabin card.** Standing inside shows four ratings that come straight from how it was built: Draft (notch fit and chinking), Insulation (roof depth, floor, snow on the roof), Light (windows and lamps) and Heat (fire type and room size). A tight, well-roofed cabin reaches the +25 to +35 °C in the tier table; a drafty one reaches half that and burns 5 armloads a day instead of 2.

**The cabin changes with the years.** Green logs shrink about 5% over the first year, so chinking gaps open by the next fall and need redoing. Snow on the roof insulates, but more than 60 cm on an unbraced roof makes it sag and drip. The sod roof greens over in spring. A second cabin is far quicker: the first one can be saved as a blueprint, and practice speeds every step.

## The open world

There is no win screen. Like Minecraft, the game is the player and the world acting on each other: the valley keeps turning through years, answers what the player builds and takes, and always offers a bigger project.

**A bigger world, streamed.** The 2 km valley around the outpost is the detailed core. The home valley extends to 8 km × 8 km in 2 km regions, generated on the server's worldgen threads when a player comes within 600 blocks of an unbuilt region, and the world goes on past it into the 65 km Interior and the endless Beyond (World map › Three rings). A coarse river network for the whole 8 km map is computed once at world creation (32 m cells), so rivers cross region borders without seams. Farther regions hold what the valley lacks: open tundra with caribou, a big lake, a canyon, an old mining creek.

**Region grid.** The 8 km home valley (ring 1 of 3; see the World map tab) is a 4 × 4 grid of 2 km regions. Columns run A to D from west to east and rows 1 to 4 from north to south; the detailed core is always B3. The seed mirrors the whole layout east to west on half of all seeds, and names and roles follow the mirror.

**The fixed frame.** The main river enters at the east edge in D3 and leaves at the west edge in A3, so upriver is east. Rows 1–2 are the north range and its benches (450–1,400 m), row 3 is the river corridor (120–600 m), and row 4 is a southern lowland of lakes, ponds and muskeg (100–500 m). Three tributaries join the main river: the North Fork from the glacier in B1 through B2 into the core, Discovery Creek from D4 into D3, and the Big Lake outlet from A4 into A3.

**Region roles.** The table is the default layout. The role pass in stage 5 keeps row 1 as mountains and row 3 on the river network, and always places the tundra, canyon, mining-creek and big-lake roles; the other roles may swap cells to fit the terrain. When the core is not the lake archetype, a second big lake takes the C4 cell.

**The sixteen regions.** On-foot times are one way from the outpost, at the travel rate in the traversal rules below plus 30% for winding ground.

| Cell | Region | Terrain | What it holds that the core lacks | Landmark seen from 2 km | Main hazard | From the outpost |
| --- | --- | --- | --- | --- | --- | --- |
| A1 | Sheep Crags | Cliffs and scree, 900–1,400 m | The Dall sheep band, a mineral lick, 2–4 iron clusters | Twin Horn, a split summit | Falls on scree; rockfall the day after rain | 4.5 km, about 6.5 game h |
| B1 | Glacier Cirque | A remnant glacier in a rock bowl, 1,000–1,400 m | The ice cave, the North Fork's source, the home pack's den in some years | The white glacier tongue | Meltwater surges on July afternoons; snow from late August | 4 km, about 6 h |
| C1 | Iron Range | Rust-stained rock faces, 700–1,300 m | 8–12 iron clusters, the richest in the world; the iron camp site | Red Wall, a 200 m cliff | Wolverines raid any cache; cold wind | 4.5 km, about 6.5 h |
| D1 | Windgap Pass | A wind-scoured saddle, 800–1,200 m | One of the three caribou routes; a wolverine den; the way out to the Interior | A surveyor's cairn with a steel mast | Wind over 10 m/s on half of winter days; whiteouts | 5.7 km, about 8 h |
| A2 | Caribou Barrens | Rolling alpine tundra, 650–900 m | The caribou herd in September (60–150 animals), ptarmigan flocks, blueberries, the Barrens pack | The Sitting Stone, a house-sized boulder | No trees: no firewood and no windbreak | 2.8 km, about 4 h |
| B2 | Lookout Bench | South-facing birch and aspen benches, 450–750 m | The fire lookout tower, the best berry slopes | The lookout tower | Black and grizzly bears in August and September | 2 km, about 3 h |
| C2 | Hot Spring Basin | A sheltered basin, 400–650 m | The hot spring, open water all winter, a moose winter yard | A steam plume below −15 °C, plus a granite tor all year | Thin ice where warm outflow meets the creek | 2.8 km, about 4 h |
| D2 | Burn Hills | A burn 5–15 years old, 350–700 m | Thousands of standing dead spruce, fireweed, the hare cycle's peak, lynx | The Black Snags, a ridge of burned spars | Windfall: dead trees drop in wind over 8 m/s | 4.5 km, about 6.5 h |
| A3 | Slough Country | A wide marshy river with oxbows, 120–200 m | Ducks and geese, muskrat, sheefish, the most moose | The smokestack of a sunken sternwheeler | The worst mosquitoes; spring flood | 2 km, about 3 h |
| B3 | Outpost Valley | The detailed core, one of four archetypes | The start site | Varies by seed | — | — |
| C3 | Salmon Bend | A big gravel-bar bend of the main river, 150–250 m | The densest salmon run, the old fish camp, the best fish-camp site | The White Bluff, a 40 m clay cutbank | Grizzlies at the riffles in July and August | 2 km, about 3 h |
| D3 | Upriver Canyon | A rock gorge with falls, 250–600 m | Long Falls and its pools (the best chinook water), coho side streams, a 400 m portage | The spray of Long Falls | Drowning; open leads below the falls all winter | 4 km, about 6 h |
| A4 | Big Lake | A lake 2–3 km long and up to 40 m deep, 140–180 m | Lake trout, sheefish, big pike, whitefish; the ice-shack site | Loon Island and its lone spruce | Waves over 6 m/s wind swamp a canoe; pressure ridges in winter | 2.8 km, about 4 h |
| B4 | Beaver Valley | A chain of beaver ponds, 150–300 m | 3–6 beaver colonies, bog iron, muskrat, otter | The Esker, a gravel ridge 30 m high | Ponds that rise and drown trails | 2 km, about 3 h |
| C4 | Cloudberry Muskeg | Black spruce muskeg, 150–250 m | Cloudberries, cranberries, peat; a second big lake when the core is not the lake archetype | A burn ridge of silver snags | Waist-deep bog holes that soak the player | 2.8 km, about 4 h |
| D4 | Discovery Creek | An old gold creek with tailings, 200–500 m | Kessler and Doane's mining camp, flint-like stone in the tailings | The 14 m dredge, or a granite tor when another variant is drawn | Prospect pits 3 m deep; rusted metal cuts that infect | 4.5 km, about 6.5 h |

**Set pieces in the regions.** Set pieces keep the fit rules and typical distances in Biomes and resources; the table only says where each usually lands. Landmarks named here that are not in the natural landmark table (Twin Horn, Red Wall, the surveyor's cairn, the Sitting Stone, the sternwheeler stack, the White Bluff, Loon Island, the Esker) join it as region-specific kinds.

**Three wolf packs.** The home pack ranges over columns B and C of rows 1–3 and dens at least 500 m from the outpost. The Barrens pack holds column A and follows the caribou. The Upriver pack holds column D. Each pack has 4–6 wolves, and a tamed wolf is challenged inside any territory but the home pack's once it is grown.

**Height changes the weather.** From May to October air cools 6.5 °C per 1,000 m above the valley floor. From November to February, calm clear days build an inversion: the benches 300–500 m above the floor run up to 8 °C warmer than the river flats. A winter cabin on Lookout Bench burns less wood than one on the river, but hauls water farther.

**Streaming a region.** When a player comes within 600 blocks of an unbuilt region, the server runs stages 2–7 for it at 2 m cells with 20,000 erosion droplets, in under 3 s off the main thread (World map › Generating the three rings). The region's fine detail blends into the world's 32 m coarse height and river network over 64 m at each border, so no cliff or river steps at a seam. The coarse field also draws the far horizon, so every landmark is visible before its region exists in detail.

**Variants and yields.** Each region draws one of 2–3 variants: Big Lake as one lake or two linked lakes, Salmon Bend with the run on the north or south bar, Burn Hills burned 5 or 15 years before. Populations use the core's density per km² of matching habitat, and every node, spot and range gets the ±20% yield rule.

**Discovery and mapping.** The first entry into a region shows its name on the messenger with one line about what can be seen from the border ("Salmon Bend: a wide bend with long gravel bars"). Map reveal follows the navigation rules: 60 m around the player on foot, 1,000 m from any point 100 m above its surroundings. A region counts as mapped at 40% of its 1,024 map tiles (64 × 64 blocks each).

**The world answers the player.**

- Cleared ground regrows: fireweed and grass the first summer, birch after 2 years, spruce after 6.
- Paths the player walks often become worn trails that speed travel and that animals avoid.
- Overhunted areas empty out and refill over seasons; a hare boom draws lynx and wolves.
- Beaver dams flood low ground and create new fishing ponds.
- A fire left untended in dry weather can start a forest fire; the burn grows morels and berries the next year.
- Bears learn: a camp that leaves food out gets visited more often.

**Exact rules.** Each response is a World edits op (`world.change`) settled by `world.catchup.CatchUp` when a chunk loads (Minecraft development › World and worldgen), so it costs nothing while the player is away.

| Response | Trigger | Rule | What the player sees |
| --- | --- | --- | --- |
| Regrowth | A tree felled or ground cleared | Stage counts from the clearing date: the first June 1 after brings fireweed and grass; 2 years, birch and willow saplings (fiber and shoots, no logs); 6 years, spruce saplings. Birch reaches pole size at 8 years and log size at 20; spruce at 30. A garden bed, building footprint, worn trail or fire ring stops regrowth on its cells | Stumps grey within a year; the clearing around the cabin is a birch thicket by year 3 |
| Worn paths | Player passes over a 2 m cell | 20 passes within 30 days make a worn path (speed ×1.1); 60 make a worn trail (×1.2, bare dirt). 60 days without a pass drops one stage. Fresh snow over 10 cm hides a trail until 5 more passes | Desire lines between the door, the water and the woodpile |
| Trails and animals | A worn trail | By day, hares, grouse and moose avoid cells within 15 m of it. In snow over 40 cm, wolves and lynx travel on it, because packed snow is easier | Wolf tracks on the player's own trail in January |
| Overhunting | Kills in a region | Populations follow the formula below. Taking over 30% of a region's population of one species in a season halves that species' encounter chance there for a year | Fewer hares near camp; hunts start farther out |
| Predators follow prey | Hare density over 1.5× the region's mean | The next year, lynx and fox capacity there rises 50% and the nearest wolf pack patrols it 30% more | More lynx tracks the winter after a boom |
| Beaver dams | Each colony, each summer | Dam stage 0–4 grows one stage a year while 30 or more willow, aspen or birch stand within 100 m. Each stage raises the pond 0.5 m and floods the cells below it. Stage 2 adds a fishing spot of quality 0.3–0.6. Drowned trees become standing deadwood a year later. A breached dam (2 game hours with a hatchet) is rebuilt in 3 nights unless the colony is trapped out | "Water rising at the beaver pond: the low trail floods in about 10 days." |
| Forest fire | An untended fire | Ignition and spread as below | A smoke column, then a black burn |
| Burn recovery | A burned cell | The spring after: morels (that spring only) and fireweed. Years 2–4: blueberry yield ×2. Years 3–6: hare capacity ×1.5. Burned spruce stands as dry fuel for 10 years, then falls | The burn becomes the best berry and firewood ground |
| Deadwood | Each living spruce, each year | 0.5% die standing from beetles, wind and winter, so the ring of easy deadwood near camp refills slowly | A few grey spruce reappear near camp each spring |
| Bears learn | A bear gets food at a camp | The camp's reward count rises by 1. A bear in range visits on 2% × (1 + count) of nights. The count falls 1 per 30 days without food. At 3 it is a problem bear: it comes by day, ignores noise and a lit fire, and half its bluff charges become real. Spray resets the count to 0 and keeps it away 30 days | "The same grizzly came back for the third time." |

**Population change.** Applied at the Abstract tier's rate, with r spread across the year:

```latex
N_{y+1} = N_y + r \cdot N_y \left(1 - \frac{N_y}{K}\right) - H_y
```

N is the region's count, K its habitat capacity, H the year's kills by the player and predators. Values of r: hare 1.5 (K follows the 10-year cycle), grouse and ptarmigan 0.8, marten and fox 0.5, beaver and muskrat 0.4, wolf 0.3, caribou herd 0.08, black and grizzly bear 0.06, moose and Dall sheep 0.05.

**Ignition.** Rolled each game hour for a burning fire with no player within 30 m, June to August, with no snow on the ground:

```latex
P_{ign} = p_{state} \cdot f_{ring} \cdot f_{dry} \cdot f_{wind} \cdot f_{packed}
```

- p\_state: Hot 1.5%, Steady 0.4%, Low 0.1%, banked 0.
- f\_ring: 1 inside a stone ring on cleared ground; 8 with no ring.
- f\_dry: 0 if it rained in the last 2 days; 0.5 after 2–4 dry days; 1 after 5 or more.
- f\_wind: 1 under 4 m/s; 2 at 4–8 m/s; 3 above 8 m/s.
- f\_packed: 5 when the fire shows "packed too full".

**Spread.** A burning 8 m cell lights each neighbor at 30% per 10 game minutes, ×3 downwind, ×0.3 upwind and ×2 uphill. Birch and aspen cells take ×0.5, bog ×0.1 unless 14 days dry. Water, gravel bars, rock, cleared ground and any hour of rain stop it. Far from the player the same rule runs on 32 m cells.

*Test:* over 100 runs, an untended Hot fire with no ring in a 7-day July dry spell and 6 m/s wind starts a forest fire before it burns out in 40–80% of runs; in a stone ring on cleared ground, under 10%.

**Years turn.** Each year repeats the seasons with a new weather draw and a new salmon-run size. Year 2 onward adds slow events: a hard winter every few years, a big berry year, a caribou herd that changes its route.

**The year profile.** At 00:00 on May 1 the world draws the coming year from `hash(seed, "year", n)`. A year is fixed the moment it starts, so reloading cannot reroll it.

| Field | Range | Effect |
| --- | --- | --- |
| Winter | Mild, normal or hard | Mild: December–February anomaly +4 °C. Hard: the hard-winter event below |
| Summer warmth | ×0.8 to ×1.2 degree-days | Harvest timing; wheat ripens only at ×1.05 or above |
| Berry index | 0.5–1.5, or 2.0 in a big berry year | Patch yields; bear camp visits divided by the index |
| Salmon runs | Chinook, chum and coho, each ×0.3 to ×2.0 | Drawn log-normal around 1.0, then cut by any fish wheel run four years earlier |
| Hare phase | One step along the 10-year cycle | Hare, then lynx and fox capacity |
| Caribou route | The Barrens, Windgap Pass, or the core's north edge | Where the herd crosses in September; 70% chance it repeats last year's route |
| Frost dates | Last spring frost May 20–June 10; first fall frost August 20–September 15 | The garden window, as in Crop farming |
| Rare events | Each rolled on its own | See the rare events rules below |

**Year 1 follows the playthrough.** As the summary above says, the slow events start in year 2. In year 1 the hard winter and big berry year are not rolled, salmon runs stay between ×0.8 and ×1.2, and the herd uses the Barrens route.

**The year leaks out early.** Each profile shows itself through signs a season ahead, so a reader of the land can prepare.

| Sign | When | What it means |
| --- | --- | --- |
| Heavy white bloom on the blueberry slopes | Mid-June | Big berry year |
| Hares turning white before October 1; squirrel middens twice the usual size; snow on the peaks in August | August–September | Hard winter likely |
| Geese leaving before September 10; birch leaves turned by August 20 | Late August | Freeze-up 1–2 weeks early |
| Few fish at the bend in the first 3 days of a run | Run start | A small run (×0.5 or less) |
| The program's season outlook on the radio, 60% right | First sked of June and of September | Summer warmth, then winter severity |

**What carries over.** Everything persists from year to year: populations, regrowth, beaver dams, burns, worn trails, bears' camp counts, fish stocks, and iron re-formed in bogs or exposed by floods. Buildings age: sod roofs need a day of repair every 5 years, and chinking needs redoing each fall in years 1–2 and every 3 years after.

**The ice record.** Each freeze-up and breakup at the station crossing is written into the messenger's ice record beside the historical dates from the story: Arvid Lund's 34 years (1938–1971) and the station's 15. The historical dates are generated per seed from the same weather model, so they agree with the valley's climate. The record is shown as a chart of breakup day by year, and logging it is the backbone of the radio loop.

**Year turnover.** On May 1 the messenger shows the year card (above), then one line: "Year 2 begins. Last breakup: April 27, 3 days earlier than the record's average."

**Systems that push on each other.** Most good moments come from two systems colliding, so every system must touch at least two others:

- Rain puts out an open fire; a fire indoors needs a chimney or the smoke chokes the player.
- Smoke drives off the June–July mosquito swarms that ruin sleep, and the same smoke cures meat.
- Snow insulates a roof, shows fresh tracks, and slows every step without snowshoes.
- Wind carries the player's scent to game and carries a campfire's sparks into dry forest.
- River ice opens winter travel and ice fishing, and thin ice at breakup is a trap.
- A forest fire destroys a trapline but grows morels and berries the next year.

**Collisions as rules.** Each collision is one GameTest in `gametest.collision`, named `collision/<id>`, that lists the two or more packages it joins and plays a scripted scenario that proves it. *Test:* a check fails the build if any system package appears in fewer than two collision tests.

| Collision | Rule |
| --- | --- |
| Rain and fire | Heavy rain drops an uncovered fire one state every 10 game minutes, light rain every 30. A lean-to or roof over the ring cancels both. Snow puts out a Low fire |
| Indoor fire and smoke | A closed room with a fire and no chimney gains 10 smoke points a game minute, up to 100. Above 50, sleep is impossible and aim sways; above 80, Condition falls 1 every 10 game minutes. An open door clears 20 points a minute; a chimney holds the room at 0 |
| Green wood and chimneys | Every armload of green wood burned indoors adds 1 creosote point; 40 points without cleaning (1 game hour with a spruce brush) gives a 2% chance a day of a chimney fire |
| Smoke, bugs and meat | A smoky fire keeps mosquitoes off within 6 m. Meat or fish hung within 2 m above that fire smokes at 25% of a smoke rack's rate, so a bug fire also cures a little food |
| Snow and roofs | Each 10 cm of snow on a roof adds +1 °C to the room, up to +6 °C at 60 cm. Above 60 cm an unbraced roof sags and drips. Shoveling a roof takes 1 game hour |
| Snow and sign | Fresh snow over 2 cm wipes every track. Tracks laid after it last until the next snowfall or wind over 8 m/s, so the morning after a snowfall every print is less than a day old |
| Wind, scent and sound | Between 2 and 6 m/s the scent cone is 80 m. Above 6 m/s it reaches 120 m but animals catch it only half the time, and walking noise carries half as far |
| Wind and sparks | A Hot outdoor fire in wind over 6 m/s throws sparks 4 m downwind. A woodpile, bough bed or dry duff there catches at 5% a game hour during a dry spell |
| River ice and water | Ice opens winter travel and ice fishing. Once the river freezes, water comes from a chiseled hole (it skins over within an hour below −20 °C) or from melted snow at 0.1 armload of wood per liter, so winter water costs firewood |
| Cold, spoilage and scent | Below −5 °C stored meat freezes and stops spoiling, and a frozen cache's scent radius halves. Wolves and wolverines still find it, as in the cache rules |
| Inversion and camp height | On calm winter days the river flats run up to 8 °C colder than the benches, so the same cabin burns about one armload a day more on the flats |
| Fire and the trapline | A forest fire destroys every trap and outpost store in burned cells, then grows morels, berries and hares on them for years |
| Beavers and the land | A rising pond floods trails, gardens and caches below its new level, and adds a fishing spot |
| Salmon, bears and camps | During a run, grizzly visits to riffles and the bend triple. A bear fishing the river visits camps half as often, but drying racks within 200 m of a riffle draw it anyway |
| Hare cycle and snares | At a hare peak snares catch twice as often, and foxes raid unchecked snares twice as often. In a crash year, lynx come to cubby sets and snares |
| Wolves, moose and ravens | A pack's kill draws ravens within a day; moose shift 1 km away from it for 7 days. Snow over 60 cm or a crust lets wolves outrun moose, so more kills are found in late winter |
| Dependents and fishing | Each tamed wolf eats 1–2 kg a day, so a team turns the salmon run and the fish wheel from a bonus into a necessity |
| Flood, camps and iron | A spring flood drowns stores below the flood line, and leaves new gravel bars, driftwood and 1–3 new iron outcrops |

**The world hands out goals.** Like a SimCity advisor, the messenger turns the world's state into a short list of situational prompts each morning, derived from real numbers: "Cold snap in 3 days; 9 armloads of wood stacked, about 12 needed." "Salmon expected at the bend in 5 days." "The cache roof is leaking." None is required; they keep a long sandbox from going quiet.

**How the list is built.** Prompts live in `data/yearofice/prompt/<id>.json` and run in `story.prompt.PromptService`, shown on the Trail Messenger's Prompts page.

1. **When.** The list is built when the player wakes from a night's sleep, or at 06:00 if they stayed up. Onboarding fills the slots through night 1, so prompts start on the morning of day 2.
2. **Evaluate.** Every rule's `when` condition is tested against the world state in the condition language (data.expr; Open world in depth › Data formats). A rule on cooldown is skipped: 3 days by default, doubled after the player dismisses it.
3. **Knowledge gate.** A rule may read only what the player could know: the forecast, places seen, pinned traps, their own stores and body, identified sign, and facts from found notes. State fields marked `hidden` (unseen animals, unvisited set pieces, the year profile) are refused by the schema when the file loads.
4. **Rank.** Priority 1 is danger within 24 game hours; 2 is a deadline within 7 days; 3 is an opportunity open now; 4 is upkeep or a long project. Within a priority, the nearest deadline wins. Only one prompt per topic is shown: food, fire and wood, water, shelter, garden, traps, wolf, travel, radio, body.
5. **Show.** Up to 3 prompts. If none fired, one priority-4 suggestion from the long-project pool fills the list, so it is never empty.
6. **Resolve.** A prompt clears itself when its condition turns false, and the journal logs it as done. Pinning a prompt makes it a messenger waypoint or task; dismissing it starts the doubled cooldown. No prompt gives a reward, so none turns into a chore.

**Wording rules.** Under 90 characters. The fact comes first and the gap second. Numbers are rounded to what the player could judge: whole armloads, whole days, kilograms to 1. No exclamation marks and no "you should".

**Sample prompts.** These sit beside the three in the summary above.

| Id | Fires when | Text | Priority |
| --- | --- | --- | --- |
| thin\_ice\_route | The player's usual crossing measured under 10 cm | "Ice at the crossing was 7 cm yesterday. It holds a person at 10." | 1 |
| scent\_by\_bed | A scent point formed within 30 m of the bed last night | "Fish scraps 12 m from the bed last night. Grizzly tracks at the bank." | 1 |
| water\_short | Under 2 L drunk yesterday | "You drank 1.2 L yesterday. The body needs 2.5 L, more on work days." | 1 |
| cut\_undressed | A cut over 6 game hours without a dressing | "The cut on your hand is 8 h undressed. Infection sets in at 12." | 1 |
| smoker\_out | A loaded smoker with its fire out | "The smoker fire is out. 6 kg of chum spoils by evening." | 1 |
| spoils\_today | Over 2 kg of stored food spoils within 24 h | "4 kg of moose liver and heart spoil by tonight." | 1 |
| frost\_tonight | Frost forecast and an uncovered bed with crops | "Frost likely tonight, −2 °C. The barley bed is uncovered." | 1 |
| blizzard\_line | Blizzard tomorrow and a pinned trap over 1 km out | "Blizzard tomorrow, 20 m visibility. The trapline takes about 3 h." | 1 |
| wolf\_hungry | The tamed wolf ate under 0.5 kg yesterday | "{wolf} ate nothing yesterday. Trust is falling." | 1 |
| sleep\_debt | Awake over 30 game hours | "Awake 30 hours. Every task slows after 2 days without sleep." | 1 |
| snare\_due | A pinned snare 36–48 h unchecked | "Snare 4 by the willow flat is 40 h unchecked. Foxes take catches after 48." | 2 |
| net\_before\_ice | Freeze-up estimate under 21 days and a net in the river | "River freeze-up in about 18 days. The gill net is still in." | 2 |
| cache\_below\_flood | Breakup estimate under 14 days and a store below the flood line | "Breakup in about 10 days. The low cache sits under last year's mud line." | 2 |
| rut\_soon | Rut within 5 days and moose sign identified | "Moose rut begins in about 4 days. Bulls will answer a call." | 2 |
| vitamin\_c | 18 or more days without vitamin C and a known ripe patch | "No vitamin C in 18 days. Rose hips by the gravel bar are ripe." | 2 |
| wet\_parka | A worn clothing piece damp and tonight's low under −20 °C | "Your parka is still damp. Tonight drops to −28 °C." | 2 |
| wood\_next\_winter | After July 1, seasoning wood under 70% of next winter's estimate | "Next winter needs about 30 trees. 8 are cut and seasoning." | 2 |
| garden\_dry | 6 dry days and a planted bed | "Six dry days. The potato bed wilts tomorrow without 10 L of water." | 2 |
| garden\_window\_closed | A crop's sowing window closed while its seed is carried or stored | "The planting window for barley has closed." | 2 |
| mice\_in\_cache | Droppings seen in a cache with fewer than 3 deadfalls around it | "Mouse droppings in the cache. No deadfalls set around it." | 2 |
| messenger\_low | Messenger battery under 1 day and sun tomorrow | "Messenger battery at 1 day. Sun forecast tomorrow: set out the panel." | 2 |
| headlamp\_low | Headlamp under 3 h and no lamp or candle made | "Headlamp at 3 h. No fat lamp or candle made yet." | 2 |
| chimney\_creosote | 40 creosote points | "The chimney has burned green spruce for weeks. Clean it before it catches." | 2 |
| drop\_today | A supply drop due today | "Supply drop at the lake flats today, 12:00 to 15:00. Mark the zone." | 2 |
| radio\_sked | Radio working and unreported logs | "Radio sked at 19:00. 6 of 7 days logged this week." | 3 |
| berries\_known | A mapped patch has ripened | "Blueberries ripe on the south bench you mapped. Bears share the patches." | 3 |
| caribou\_week | The week the herd crossed last year | "The herd crossed the Barrens this week last year." | 3 |
| ravens\_over | Ravens seen circling within 2 km yesterday | "Ravens circling 2 km north since yesterday." | 3 |
| pressure\_drop | Pressure falling before a forecast storm | "Pressure falling before tomorrow's storm. Fish bite better for a few hours." | 3 |
| spruce\_tips | Spruce tips in season and none picked | "Spruce tips are out: the first vitamin C of the year." | 3 |
| sap\_running | Birch sap window open | "Birch sap is running. Tap a tree for the year's first sweet thing." | 3 |
| aurora\_tonight | Aurora storm tonight and a clear forecast | "Strong aurora tonight after 22:00, clear sky." | 3 |
| shed\_antlers | April–May and antler sign identified | "Shed antlers lie in the willows until the voles find them." | 3 |
| stave\_ready | A stave has dried 10 game days | "The birch stave by the fire has dried 10 days. It is ready to work." | 3 |
| chinking\_open | The cabin's heat rating fell 3 °C or more since spring | "The cabin holds 4 °C less than in spring. About 3 m of chinking has opened." | 4 |
| weeds | A bed unweeded 9 days | "The potato bed has gone 9 days unweeded. About 3% lost a week." | 4 |
| outpost\_unvisited | An outpost store with food unvisited 60 days | "The fish camp cache is 62 days unvisited, with 18 kg inside." | 4 |
| idle\_project | No other prompt fired | "No prompts today. The smokehouse needs 14 more logs." | 4 |

**Survival, not thrival.** Progress must never make the needs irrelevant. As food gets easier, the pressure moves: from finding food to keeping it (spoilage, raids by bears that learn the camp), to feeding more (a wolf team eats meat every day; bigger builds burn more calories), to bigger risks (winter trips to the tundra, a hard winter every few years, a fish wheel that thins next year's run if it runs all season).

**No hidden scaling.** The game never raises burn rates, spawn rates or animal boldness because the player is doing well. Every shift below comes from a mechanic the player can see and act on.

| As the player gains | Pressure moves to | The rule that does it |
| --- | --- | --- |
| A first full cache (over 10 days of food) | Keeping it | Cache scent radius = 100 m + 5 m per 10 kg of raw meat; smoked or dried food counts a third, frozen food half. Bears path to scent within 400 m and learn camps (problem bears); mice take 2–3% a week in winter |
| A winter of frozen meat | The thaw | When air first passes −5 °C in April, every unsmoked frozen item starts its spoilage clock at the cool rate (6 days at 5 °C). A frozen moose must be smoked, dried or eaten within a week of breakup |
| A stove cabin | Wood | At −40 °C a chinked cabin burns 3 armloads a day, not 2. Standing deadwood near camp is used up in year 1 and refills at 0.5% a year, so each winter's wood comes from farther away |
| A tamed wolf, then a team | Feeding more | Each wolf eats 1 kg a day resting and 2 kg working. A team of three needs about 1,100–2,200 kg of meat or fish a year, more than the player eats |
| Goats and hens | Summer haying and grain | A goat eats 2 kg of hay a day through about 200 days of snow: 400 kg, cut at 20 kg a game hour. A hen eats 0.1 kg of grain or dried fish a day in winter; summer forage covers May to September |
| Bigger builds | Calories | Building burns 300–500 kcal a game hour; a cabin costs about 70,000 kcal beyond living |
| A hunted-out home range | Distance | The encounter director and populations thin game near camp, so by year 2 a moose hunt starts 1–3 km out, at about 1 game hour and 250 kcal per km on foot |
| Every ambition beyond year one | Risk | Open ambitions point at colder, farther and steeper places: Windgap Pass in January, the canyon above Long Falls, the Iron Range |
| A good year | Bad years | A 20% hard winter and runs as small as ×0.3 mean a homestead must store about 30% more than one normal year uses |

**The upkeep floor.** The balance simulator runs an established year-3 homestead: a stove cabin, root cellar, smokehouse, a team of three wolves, two goats, three hens and a rooster and a 50 m² garden.

1. It must still need at least 4 game hours a day of food and fuel work averaged over winter, and 6 over summer.
2. Dependents, spoilage, mice and raids must take at least 40% of all the food it produces in a year.
3. No single chore may break the tedium check.

If a test fails, the fix is a new pressure from this table, never a higher number on an old one.

**Rare events.** Each event is rolled on its own every year against its chance, about 1–2 surprises a year on average (1.7 from year 2) so no two years play alike:

| Event | Chance per year | What changes |
| --- | --- | --- |
| Wildfire | 15% | A region burns; trapline and trails lost; morels and berries the next summer |
| Spring flood | 25% | Low camps flooded; new gravel bars, new iron exposed |
| Hard winter | 20% | A month below −40 °C; wolves bolder |
| Big berry year | 25% | Bears fatten on the hills and stay away from camp |
| Winter-killed moose found | 30% | Free meat if reached before the bears and wolves |
| Earthquake | 5% | A landslide reshapes a slope and can dam a creek into a new lake |
| Aurora storm | 40% | A night of bright aurora; no gameplay change, just beauty |
| Storm of the decade | 10% from year 2, at most one in 7 years | The Big Snow, the Great Glaze or the Long Rain, warned 72 game hours ahead; the valley's worst weather in years (Weather and storms) |

**Outposts.** Later bases have reasons to exist away from home: a fish camp at the salmon bend, a caribou camp on the tundra in September, a cabin by the iron in the north range, an ice-fishing shack on the big lake. Each is a warm stop and a store, and linked outposts become the player's own network.

**Traversal gets better and stays useful.** Each step has a trade-off, so early ways of moving never become pointless:

| Way to move | Gained by | Good for | Trade-off |
| --- | --- | --- | --- |
| On foot | Start | Stalking, steep ground | Slow; deep snow cuts speed to 60% |
| Snowshoes | Hide tier | Deep snow at 90% speed | Clumsy on bare ground and slopes |
| Birch-bark canoe | Homestead tier | Fast downriver; hauls 150 kg | Upstream is slow, tiring work; capsizing soaks everything |
| Wolf sled | Three tamed wolves (see below) | Long winter trips, 120 kg loads | Each wolf eats 1–2 kg of meat or fish a day and may bolt after game |
| Cut trails and log bridges | Building | Permanent fast routes between outposts | Hours of clearing per 100 m |

**Taming a wolf.** There are no dogs. Wolves fill that role, so the same animal can be a threat, food, or a companion depending on what the player does. A tamed wolf is still a wolf: it needs meat, keeps some wild instinct, and can be lost.

There are three ways to start:

- **A den pup (late May–June).** Follow pack tracks to the den in spring and take one pup while the adults are out hunting at dawn or dusk. If they return, the pack attacks; afterward the den moves and the pack hunts near camp for weeks.
- **An orphan.** Pups left after the player kills or drives off the parents can be taken without risk, but whatever is left of that pack stays hostile.
- **A lone yearling (winter).** A young wolf driven out of its pack may trail the player for scraps. It can be won over by feeding it from a distance over many days.

Trust (0–100) decides how the wolf behaves:

| Stage | Trust | Behavior | How to move up |
| --- | --- | --- | --- |
| Wary | 0–25 | Follows at 50–100 m; takes food left on the ground | Leave food; never approach faster than a walk |
| Tolerant | 25–50 | Eats within 10 m; flees if the player sprints or shouts | Feed it every day; sit near it |
| Bonded | 50–80 | Follows, comes to a whistle, growls at bears and wolves, tracks blood trails | Share kills; never strike it |
| Companion | 80–100 | Carries a 10 kg pack, pins hares, holds a moose at bay, can be harnessed | Feed 1–2 kg of meat or fish a day; shelter it in winter |

Trust falls 5 a day without food, 30 if the wolf is struck, and 10 when the player kills wolves in its sight. Below 25 it may raid the food cache. Below 10 it leaves, by preset (Ruhan, 2026-10-19): on Gentle it never leaves and sulks in camp at 10 trust, refusing commands; on Standard it returns within 10–20 game days at 40 trust; on Brutal it leaves for good.

**Wild instinct.** In February, the breeding season, a tamed wolf under 80 trust may leave to join a pack: never on Gentle; warned in late January and recoverable on Standard; for good on Brutal (Ruhan, 2026-10-19; the roll and the warning are in Open world in depth › Taming a wolf). Wild packs challenge a tamed wolf inside their territory, and a wolf that is not called back will chase game on its own.

**A wolf team.** Three or more Companion wolves can be harnessed to a sled. They pull well but bolt after caribou and moose unless trust is above 90, so a team is the long-term reward of years of care, not a quick upgrade.

**Wolves are still prey and predator.** Wild wolves keep every predator behavior above and can be taken with a longbow or a wire snare on their trails. A wolf gives about 20,000 kcal of lean meat and a pelt for a warm hood and mittens. Killing pack members can scatter a pack or, in a hard winter, make the survivors bolder around camp.

**Ambitions instead of an ending.** The messenger keeps a list of milestones that unlock as the player grows, like Minecraft advancements. None is required.

| Stage | Example ambitions |
| --- | --- |
| First days | First fire by bow drill; first fish; first night above freezing indoors |
| First summer | Catch a pike; dry 10 kg of fish; season a winter's firewood |
| First winter | Survive −40 °C; ice-fish through 1 m of ice; snowshoe 10 km |
| Year one | See the thaw; take a moose; keep BMI above 20 for 30 days |
| Beyond | Light the forge; launch a canoe; run a 20-snare trapline; map all 16 regions |

**Story found, not told.** The outpost was a research station, and its story is scattered through the world: the last researcher's logbook pages, an old trapper's cabin, a wrecked bush plane with salvageable aluminum and wire. None of it ends the game.

**The radio.** Repairing the radio mast (iron, wire from the plane, a working battery) restores contact. The player can then log daily weather and wildlife counts for the research program, and each season of logs earns one supply drop: seeds, salt, a spare tool or a coil of wire. It is an optional loop that gives long-term purpose, not a rescue.

Full rules for rare events, outposts, traversal, wolf taming, ambitions, the story, the radio, data formats and engine rules: Open world in depth

## Data formats

Engine note: in the Minecraft build these files become data-pack JSON under `data/yearofice/` (see the Minecraft engine tab). All content lives in JSON under `data/yearofice/<kind>/<id>.json`, one entry per file, with snake\_case keys and unit suffixes (Minecraft development › Data kinds); per-item and per-block numbers are data maps. Set pieces, rare events and morning prompts live in `setpiece/`, `event/` and `prompt/`. Code reads these; agents add content by editing data, not systems. A generated world is saved as the seed plus its generator version and day length in yearofice\_world, and Minecraft's chunks store the player's changes.

Species entry:

```json
{
  "id": "snowshoe_hare",
  "habitat": ["riparian", "spruce_taiga"],
  "population": [25, 40],
  "home_range_m": 150,
  "active": ["dawn", "dusk", "night"],
  "weather": { "rain": 0.4, "snow": 1.0, "clear": 1.0 },
  "behavior": "flee",
  "uses_trails": true,
  "yield": { "meat_kcal": 1000, "fat_pct": 2, "items": ["hare_pelt"] },
  "respawn_days": 6
}
```

Recipe entry (crafting costs calories, so time and effort are part of the data):

```json
{
  "id": "bow_drill",
  "inputs": { "stick": 2, "cordage": 1, "spruce_board": 1 },
  "tool": "knife",
  "minutes": 20,
  "effort": "medium",
  "unlocks": ["fire_start_minigame"]
}
```

World record (what a save file holds):

```json
{
  "seed": 3141592,
  "generator": "v1",
  "day": 1,
  "date": "05-01",
  "player": { "kg": 77, "cm": 183, "energy": 10, "pos": [1024, 151, 980] },
  "day_length_min": 24
}
```

Morning-prompt rule (a prompt fires only when its condition is true in the world state):

```json
{
  "id": "wood_before_cold",
  "when": "forecast.minC(3) < -25 && stores.armloads < need.armloads(3)",
  "text": "Cold snap in {days} days; {have} armloads stacked, about {need} needed.",
  "priority": 2
}
```

## Engine rules

Four rules keep an endless, streamed world fast, stable and cheap to build. Each one answers a risk found in review. Engine note: in Minecraft the tiers run per tick (20 a second) inside simulation distance, with the eight forms of Minecraft development › Entities and AI › Tiers, spawning and despawning.

**Off-screen simulation in three tiers.**

| Tier | Range from player | What runs | Update rate |
| --- | --- | --- | --- |
| Active | Under 150 m | Full AI, pathing, senses, physics | Every frame |
| Nearby | 150–600 m | Position along trails, eating, sleeping | Once per game minute |
| Abstract | Everywhere else | Population count per region and species | Once per game hour |

Animals become individuals only when the player comes close, drawn from their region's population. Traps, nets, spoilage, regrowth and fires far away are settled by catch-up rolls for the elapsed game hours when the player returns, not simulated live. A tamed wolf, and any animal stalking or following the player, always stays in the Active tier.

**Saves and generator versions.**

- A save is the Minecraft world folder: the seed, generator version and day length in `yearofice_world`, the player's attachments, and Minecraft's own chunks, which hold every felled tree, placed piece and dropped item; there are no edit lists.
- A save pins its generator version. Changing the generator never moves land under an existing cabin; old versions stay runnable, and mod data upgrades by versioned codecs and id aliases, so every save from 0.3.0 opens in every later version (Minecraft development › Saves across versions).
- The mod's own data (SavedData and attachments) stays under 5 MB after five years of play, and the mod keeps 3 rotated copies of it under `<world>/yearofice/backups/`, so a bug or a bad death never costs more than a few minutes.

**One source of truth for the ground.** Generation runs only in `com.yearofice.worldgen` on the server, using integer hashing and plain + − × ÷ and `Math.sqrt`, which give identical results on every JVM; `Math.sin`, `exp`, `pow` and `java.util.Random` are banned there by `tools/lint-gen`. The client never computes terrain: it draws the blocks and the horizon height field the server sends, so physics and pictures can never disagree. This retires the studio lesson about mismatched JS and GLSL terrain.

**Per-agent briefs.** Agents never read this whole doc. Each gets a brief of two pages or less in the Games project under `yearofice/briefs/`:

| Brief | Sections it carries |
| --- | --- |
| World generation | Pipeline, Terrain and hydrology, Biomes and resources (seed variety, set pieces), Start site, Engine rules |
| Ecosystem | Wildlife and fish, Animals in depth, How animals act, Taming a wolf, Engine rules (off-screen tiers) |
| Food and hunting | Food systems, Mechanics check: hunting, Hunting in depth, Fishing in depth |
| Body and survival | Survival tuning (sleep, bugs, realism settings), Moment-to-moment mechanics |
| Building and crafting | Crafting and progression, Shelter and building, The open world (outposts, traversal) |
| World and story | Calendar and weather, A year of work, The open world (prompts, rare events, ambitions, story, radio) |
| Look, sound and UI | What the experience needs, Navigation, Controls and interaction |
| Testers | Pillars, A playthrough, Lessons from players, the milestone tests and the tedium check, current PLAYTEST notes |

## Build milestones and agent roles

Build in this order; each milestone ships only when its test passes. Every milestone also passes the tedium check: no single chore fills more than 5 real minutes in a row without a faster way already reachable by that point. Engine note: the build is a Minecraft Java Edition mod on NeoForge 1.21.1 (mod id `yearofice`), for Ruhan's personal use. Minecraft engine › Milestones in Minecraft is the build order: it adds the M2.5 vertical slice between M2 and M3 and gives every milestone its packages, data, content budget and GameTests. The milestone goals and tests below still stand, and the Packages and Data rows of each build spec name Minecraft packages and data kinds.

1. **M0 — Seeded world.** Stages 1–3 (seed, landform, erosion) for all four valley archetypes, with a top-down debug map. *Test:* the same seed gives an identical height hash on Linux, Windows and macOS JVMs; over 200 seeds each archetype appears 25% ±5% of the time.
2. **M1 — Water and set pieces.** Lakes, rivers, falls, fishing spots, rock faces, set-piece placement and horizon landmarks. *Test:* every river flows downhill to the edge or a lake; every region has a landmark visible from 2 km; no set piece sits in water or on a slope over 25°.
3. **M2 — Walkable valley.** First-person movement, chunk streaming, trees, biomes, day/night, the messenger map with breadcrumbs, controls with work previews, carrying and encumbrance, the messenger's battery. *Test:* 60 fps on a mid laptop with 512 m view distance; no hitch over 50 ms when crossing chunks.
4. **M3 — Start site, fire and onboarding.** Outpost placement with all 12 checks, starting kit, bow-drill fire, fire states and builds, banked coals, water sources, light sources, the guided first night. *Test:* 50 seeds pass placement within 20 rerolls; a first-time tester lights a fire before the first night without outside help.
5. **M4 — Body.** Calories, BMI, Endless and Hardcore modes, energy, sleep rules, warmth, water, wet, bugs, injuries and medicine, vitamin C, spirits, realism presets, messenger HUD. *Test:* a scripted player eating nothing reaches BMI 17 between day 15 and 21 on Standard and roughly twice as late on Gentle.
6. **M5 — Food and goals.** Snares, deadfalls and the trapline, the fishing minigame with species fight profiles, tackle, set lines, weirs, gill nets, minnow traps, hare and squirrel, cooking, smoking, foraging and plant identification, the crop garden with germination, frost and saved seed, the auto-eat cache with rations and mice, morning prompts. *Test:* a scripted "good" player keeps BMI above 19 through day 30. Over 100 seeds, packet barley sown inside the window comes up 50–65%, and barley sown into soil below 5 °C comes up under 25%. *Human gate:* you play the first game week for an hour and call it tense, not tedious, before M6 starts.
7. **M6 — Danger and big game.** Bears with scent, moose, the 12 new species, drives and senses, tracks and sign, populations, the hit-zone model, blinds and calls, the moose call, draw weight against strength, the encounter director, the longbow and flint broadheads, butchering, pack frame, predator warnings and chase break-off, bear-encounter responses, thin ice and self-rescue, felling hazards. *Test:* a bear reaches an uncleared fish-cleaning spot within 2 game days on 7 of 10 seeds; no predator chases more than 150 m.
8. **M7 — The year.** Seasons, freeze-up, snow, ice fishing, winter gear, salmon run, caribou migration, mosquitoes, seasonal alerts, rare events, the year card. *Test:* three simulated years run back to back; river ice forms between late Oct and mid Nov every year; over 100 simulated years each rare event occurs within 5 points of its listed chance.
9. **M8 — Building and outposts.** Cabin repair, log walls, chinking, stove, cache, smokehouse, drying racks, root cellar, woodshed, raft, blueprints, quality-of-life rules, outposts, cut trails and bridges. *Test:* a finished cabin with a stove holds feels-like above 5 °C on a −30 °C night; stamping a saved cabin blueprint takes under 10% of the player time the first build took.
10. **M9 — Homestead and beyond.** Forge, a stovehouse that lengthens the garden season, canoe, wolf taming and a wolf sled team, goats and hens, radio loop, streamed 8 km regions, world responses. *Test:* a den pup reaches Companion in 30–60 game days of steady feeding, and five simulated years with an active homestead keep the save under 5 MB and the frame rate inside M2's budget.

| Agent | Owns | Sub-agents under it |
| --- | --- | --- |
| Director (the main chat) | The only agent Ruhan talks to. Turns his requests into milestone goals, plans and sizes every round, briefs the studio lead, enforces the pillars and cost rules, signs off each milestone, and brings Ruhan only the decisions that are his | Studio lead, and through it all 8 departments and 24 sub-agents |
| Studio lead | Running each round day to day: assigning work to departments, merging, the smoke test, check-ins so nothing stalls, and a merge report to the Director | — |
| World generation | Stages 1–7, debug map, seed tests | Terrain and water; Biomes and resources; Set pieces and seeds |
| Ecosystem | Animal behavior, populations, wolves | Animal AI; Populations and sign; Wolf taming |
| Food and hunting | Hunting, trapping and fishing gameplay | Hunting; Trapping; Fishing |
| Body and survival | The body model, fire, health | Body model; Fire and water; Health and plants |
| Building and crafting | Crafting, building, travel | Crafting; Building; Outposts and travel |
| World and story | Weather, goals, story | Weather and events; Goals and ambitions; Story and radio |
| Look, sound and UI | The messenger HUD, art, audio | Messenger HUD; Art; Audio |
| Testers | Proving each build works and is fun | Balance simulator; Playtest bots; Design review |

**The Director.** A new top role sits between Ruhan and the studio lead and manages every agent and sub-agent. Ruhan talks only to the Director; the Director never writes game code itself.

- **Owns the plan:** the blueprint, the canon and the milestone order. Changes to the pillars need Ruhan's yes.
- **Plans each round:** picks one focused goal, chooses which departments run, sizes the work to the studio cost rules, and writes the round brief the studio lead hands out.
- **Holds the gates:** reads the studio lead's merge report, the smoke test and the testers' results, and checks the work against the pillars and the tedium rule before anything reaches Ruhan.
- **Settles conflicts:** reassigns file ownership, pauses or stops any agent, and breaks ties between departments.
- **Escalates only what is Ruhan's:** the open decisions below, each sent with options and a recommendation, never a bare question.
- **Reports:** after each round, two or three plain sentences to Ruhan on what changed and how to try it.

**Studio rules.** The Director keeps these rules, briefs every agent on them, and checks them at each gate. An agent that breaks one has its work sent back; it is not merged with a note to fix later. Most rules come from the studio playbook, learned on Emberveil and Hashfire.

| # | Rule | How the Director enforces it |
| --- | --- | --- |
| 1 | **One focused goal per round.** No "do everything" rounds. | The round brief names one goal and a done test. Anything else goes to the roadmap queue. |
| 2 | **Size the team to the task.** A fix is done in the main chat; a feature round is 2–3 specialists plus the studio lead; fan-outs of 5 or more only when Ruhan asks. | The Director sizes every round before it starts and records the size in the brief. |
| 3 | **Point agents at exact files and docs.** No agent reads the whole project. | Each brief lists the blueprint sections and files to read, from the per-agent briefs table. |
| 4 | **Strict file ownership.** Each department owns its Java packages and data folders (Minecraft engine › Ownership by package); other systems reach them only through the shared layers' services in `com.yearofice.api`. | `studio/ownership.json` generates CODEOWNERS; the ArchUnit test fails any import across system packages, and a diff touching a path its ticket does not list is rejected. |
| 5 | **Targeted edits only.** No script or agent rewrites a whole file. | The studio lead's merge report lists edits per file; a whole-file rewrite is reverted. |
| 6 | **Saves stay compatible.** New state goes into new attachment or SavedData fields with codec defaults; ids are never renamed, and a retired id stays as an alias. | Every fixture save from every release loads and passes the migration checks at every merge (Minecraft development › Saves across versions). |
| 7 | **One source of truth for the ground.** World generation runs only in `com.yearofice.worldgen` on the server; the client never computes terrain, it only draws what it is sent. | `tools/lint-gen` fails any banned call in `worldgen`, and terrain code outside `worldgen` and `world` fails review. |
| 8 | **Nothing ships without SMOKE PASSED.** One command, `./gradlew smoke`: build, every JUnit test, every GameTest of this and earlier milestones, a dedicated-server boot and a client boot under a virtual display on seed 3141592, and the no-monsters check. Tests wait on game time, never on tick counts, and only one client test runs at a time. | The Director reads the smoke output itself before sign-off. |
| 9 | **Look before shipping.** The client smoke's screenshots of day, night, a danger moment, a blizzard, the messenger map and the pack screen, at GUI scales 2 and 4, are checked by eye. | The Director looks at the screenshots, not just the test result. |
| 10 | **Pillars and tedium decide ties.** Any feature that loses to a pillar is cut or changed; no chore over 5 real minutes without a faster way in reach. | The design-review tester scores each round against the 6 pillars and the tedium check. |
| 11 | **Testers work on a frozen snapshot** and score 1–10 so rounds can be compared. | The Director compares scores with the last round; a drop blocks the release. |
| 12 | **Cheap check-ins.** Agents notify when done; at most one fallback check-in every 60–120 minutes; nightly builds stay paused unless Ruhan wants them. | The Director owns every schedule and cancels idle ones. |
| 13 | **Batch feedback.** Playtest notes are collected and handled in one round, not one round per note. | Notes go to PLAYTEST; the Director picks the batch. |
| 14 | **Docs stay current.** CHANGELOG, ROADMAP, DEVGUIDE and PLAYTEST are updated in the Games project after every release, and saved before long runs in case the container restarts. | The release is not reported to Ruhan until the docs are saved. |
| 15 | **Ruhan decides, the studio recommends.** Open decisions, pillar changes and anything costly or irreversible go to Ruhan with options and one recommendation. | The Director is the only agent that messages Ruhan. |
| 16 | **Lessons are written down.** Any problem that cost an hour or more becomes a new line in the studio playbook. | The Director adds the line at the end of the round. |

**Changing the rules.** The Director can propose a new rule or a change after any round, with the problem it fixes. Ruhan approves it, and the Director updates this table and the studio playbook.

**Code layout.** The game is a NeoForge 1.21.1 mod built with Gradle in the repository `ruhanu155/years-of-survival` (Minecraft engine › Repository layout). Its code lives in the top-level packages of `com.yearofice` (Minecraft development › Package and module map): pure rules in `sim` with no Minecraft imports, client code in `client`, and every package owned by one department.

| Packages | Holds | Owner |
| --- | --- | --- |
| `core`, `api`, `net`, `item` | Mod entry, scheduler and budgets, game rules, hashing (`Mulberry32`, `StageHash`), `/yoi` commands; service interfaces and events; payloads; data components and item data maps | Studio lead; changes by review only |
| `worldgen`, `world`, `client.horizon` | Generation stages 1–7 on worker threads, the chunk generator, World edits and catch-up, the horizon mesh, `tools/debugmap` | World generation |
| `ecosystem` | Animal agents, senses, populations, scent, wolves | Ecosystem |
| `hunt`, `food`, `decay` | Weapons, hit zones, butchering, traps, fishing, cooking, preserving, the cache, decay and freshness | Food and hunting |
| `body`, `fire`, `forage` | Body model, fire, water, light, health, plants, movement and load | Body and survival |
| `build`, `travel` | Crafting, jobs, pieces, rooms, cabins, outposts, boats, sleds | Building and crafting |
| `story`, `env` | Clock, calendar, weather, the Environment field, ice, events, prompts, ambitions, journal, radio | World and story |
| `hud`, `sound`, `client` | Trail Messenger and HUD, rendering, generated and licensed art and audio | Look, sound and UI |
| `data`, and `data/yearofice/` in the jar | Loaders, codecs and the linter (build developer); each data kind is owned by the department that reads it most | Per kind |
| `gametest`, `tools/` | GameTests, the balance simulator, bots, recorded hashes | Testers |

**Who signs off.** The Director plans each milestone's rounds from the build order in its spec and signs the milestone off to Ruhan once every automated test passes on one frozen copy. The studio lead runs the departments through those rounds day to day. Ruhan plays the human test himself; a milestone is not shipped until he has.

**Every milestone also ships these.**

- `./gradlew smoke`: builds, runs JUnit and the GameTests, boots seed 3141592 on a dedicated server and a client for 600 ticks each, and fails on any error, unregistered event, unhandled payload or NaN in the body record. A clean run prints the SMOKE PASSED line the studio rules require.
- `tools/lint-gen`: fails the build if `com.yearofice.worldgen` uses `Math.sin`, `Math.exp`, `Math.pow`, `java.util.Random` or `ThreadLocalRandom`.
- `tests/hashes.json`: layer hashes for 20 fixed seeds per generator version. Any change to them needs a version bump, and old versions must still reproduce their hashes.
- From M2 on, the M2 frame budget; from M3 on, the tedium check, timed by the playtest bots.
- A CHANGELOG entry and fresh PLAYTEST notes, written on the frozen build (see Sub-agents).

Art and sound arrive with the systems that need them, by the per-milestone budget in Art and audio production (409 sound events and 12 music events in all, RECONCILE 141), generated or licensed with credits; placeholders first.

**M0 build spec.** Prove determinism and the four archetypes before anything is drawn in 3D.

| Part | Spec |
| --- | --- |
| Packages | `core.hash` (`Mulberry32`; `StageHash`, 32-bit FNV-1a over `hash(seed, stageName)`); `worldgen.Noise` (simplex fBm with integer-hashed gradients, no trig); `worldgen.Pipeline` (stage runner: each stage declares its inputs and the one layer it writes, stored per 64-block tile); the `yearofice-gen` worker pool; `worldgen.Archetype`, `Landform`, `Erosion`; `tools/debugmap` (desktop PNG maps with seed and reroll); `tools/seeds` |
| Data | `data/yearofice/worldgen_params/valley.json` (every default and range in the terrain parameter table); `archetype/<id>.json` for braided, big lake, canyon and marsh (trough width, river gradient, basin bias, falls bias, meander strength) |
| Build order | (1) RNG and stage hashing. (2) Noise and landform for a 2,048 m map at 2 m cells. (3) Archetype draw, 1 in 4, from `hash(seed, "archetype")`, shaping the trough. (4) 60,000 erosion droplets, then thermal erosion at 35°. (5) Debug map at 1 px = 2 m: seed box, reroll, hillshade, slope view, PNG export. (6) Height hash: heights rounded to whole millimetres as int32, FNV-1a over the array |
| Automated tests | The M0 test above, plus: stages 1–3 finish in under 8 s in the worker on a mid laptop; changing an erosion parameter leaves the landform layer hash unchanged; all heights fall within 120–900 m and the valley floor sits at 150 ±30 m; no NaN; lint-gen passes |
| Human test | The owner sees 8 debug maps (2 per archetype) with labels hidden and names the archetype on at least 6 |
| Out of scope | Rivers and lakes (M1), biomes and trees (M2), any 3D view, the 8 km home valley |

**M1 build spec.** Finish the water and the places worth walking to, still on the debug map.

| Part | Spec |
| --- | --- |
| Packages | `worldgen.CoarseRivers` (the 8 km network at 32 m cells, built now so M9 regions need no migration); `worldgen.Lakes` (priority flood; lake or bog by the 1.5 m and 2,000 m² rule); `worldgen.Rivers` (D8, accumulation, the 20,000 m² threshold, width from √catchment, carving); `worldgen.Falls`, `FishSpots`, `RockFaces`, `SetPieces`; `worldgen.Landmarks` (line of sight on the height grid); a debug-map layer for each |
| Data | `setpiece/<id>.json` with templates in `structure/setpiece/<id>/<variant>.nbt` (7 set pieces with 2–3 variants each and terrain placement rules); fishing-spot quality bands in `worldgen_params/valley.json` (pool below falls 0.7–1.0, lake inlet or outlet 0.5–0.8, outside of a bend 0.4–0.7, beaver pond 0.3–0.6, then ±20%, clamped to 0–1) |
| Build order | (1) Coarse 8 km rivers, so the core river enters and leaves where the wider world expects. (2) Lakes and bogs. (3) Rivers carved into the core. (4) Falls and pools. (5) Fishing spots with quality. (6) Rock faces and the top 20 shelter candidates. (7) Set pieces. (8) Landmarks |
| Automated tests | The M1 test above, plus: the core river's entry and exit match the coarse net within 32 m; each archetype shows its signature (braided: 3 or more channels over at least 500 m; big lake: one lake of 0.2 km² or more; canyon: 4 or more falls; marsh: 2 or more oxbow lakes); 40–120 fishing spots and at least 20 shelter candidates per map; the full M1 layer hash matches across JVMs |
| Human test | On 4 seeds the owner marks where he would fish first from the debug map; his pick is among the generator's top 10 spots on 3 of 4 |
| Out of scope | Water rendering, ice and snow (M7), beaver dams that grow (M9), iron and other resources (M2), animals |

Set pieces are placed only from layers that exist by M1 (height, slope, aspect, water distance), never from biomes:

```json
{
  "id": "fire_lookout",
  "variants": ["tower_intact", "tower_leaning", "tower_burned"],
  "place": { "min_elev_m": 400, "max_slope_deg": 25, "aspect": "any", "min_water_dist_m": 50 },
  "landmark": { "height_m": 22, "visible_from_m": 2000 },
  "per_map": [0, 1],
  "loot": ["note"]
}
```

**M2 build spec.** Make the valley walkable, with the messenger as the only map.

| Part | Spec |
| --- | --- |
| Packages | `worldgen.YoiChunkGenerator`, `YoiBiomeSource`, `worldgen.Cover` and the tree builders (Poisson-disc trees, 3% deadwood, iron clusters, plant patches, trapper caches); `world.catchup` (`CatchUp`, `LoadGate`); `client.horizon.HorizonRenderer` (Minecraft streams the chunks; the horizon carries the 2 km rule); `client.sky` (sun at 64°N), `client.render` and `client.camera` (both hands, eye at 1.62 m); `client.input` (key mappings, Minecraft defaults); movement attributes (walk, run, crouch, jump, swim); `build.work.WorkService` (tap and hold, progress ring, work preview); felling and bucking into logs and armloads (`build.wood.FellingService`); `item.inventory` (weight, hand carry, load bands, locked pack rows); `hud.messenger` and `client.messenger` (shell, battery, map tiles, reveal, breadcrumbs, 20 waypoints); `story.clock` (per-player rates, day length); saves are Minecraft's world folder plus 3 rotated copies of our SavedData |
| Data | Biome JSONs in `worldgen/biome/`; `data_maps/item/weight.json` and `props.json` (first 10 items with weight and condition); `action/<id>.json`; `clock/rates.json`; the month normals, hourly curve and daylight table of `weather/interior_alaska.json` |
| Build order | (1) Streaming and terrain from the worker's textures. (2) Biomes and tree scatter. (3) First-person movement and collision. (4) Sky and day–night. (5) Controls with work previews. (6) Felling and bucking. (7) Weight inventory and load bands. (8) Messenger map. (9) Battery and solar panel. (10) Saves |
| Automated tests | The M2 test above, plus: a bot measures the four load-band speeds within 2% of the table; map reveal, breadcrumbs and waypoints survive a reload; mod save data grows under 20 KB for 1,000 felled trees; killing the server mid-save leaves the previous save loadable; a typical-use bot drains a full battery in 6.5–7.5 game days; a clear June day charges 0 to 100% in 2 h ±10%; a clear December day adds 10–20% |
| Human test | The owner walks from the outpost to the horizon landmark and back on breadcrumbs alone, and calls movement and the work ring "not clunky" |
| Out of scope | Needs and vitals (M4), animals (M5 and M6), building pieces, weather states other than clear and cloudy, felling hazards (M6) |

**M3 build spec.** Put the player at the outpost and get them through the first night by fire.

| Part | Spec |
| --- | --- |
| Packages | `worldgen.Trails` (least-cost game trails); `worldgen.Ranges` (home ranges and dens as data, no animals yet); `worldgen.StartSite` (scoring, the 12 checks, up to 20 rerolls, a per-check report); the outpost template `setpiece/outpost/day_one` (cabin ruin, firepit, radio, crate); `build.stations` and the `yearofice:station` recipe type (the recipe book that opens when an input is held, stations, practice up to 40%, queued jobs); the `fire` package (the fire-ring block entity with the six layers and four builds; the state machine, fuel bar, Low, Steady and Hot, the packed-too-full warning and spread); the bow-drill screen `client.minigame.BowDrillScreen`; water containers and boiling; `fire.light` (light items) with `env.light`; `story.Onboarding` (messenger tasks and the objective beacon until the first sunrise); fire sounds |
| Data | The starting kit in `loot_table/player/starting_kit.json` and its items' data maps (exactly as listed in Start site); station recipes in `recipe/` for the bow drill, tinder bundle, birch-bark torch and cordage; `data_maps/item/fuel.json` (burn time and heat per fuel and build); the onboarding steps as `prompt/` entries (open the crate, drink, peel bark, ring the fire, light it, stack 2 armloads, make a debris bed, sleep) |
| Build order | (1) Trails and ranges, which the checks need. (2) Start-site search with a debug-map overlay of each check. (3) Outpost and crate. (4) Crafting core. (5) Campfire layers and fire states. (6) Matches and the bow drill. (7) Builds, settings and banking. (8) Water and boiling. (9) Light sources. (10) Onboarding |
| Automated tests | The M3 test above, plus: the report names the failing check for every rerolled seed; an ember with no tinder dies at 60 s; a tinder flame with no kindling dies at 2 minutes; banked coals relight at 6 h and not at 6 h 10 min; a 2 L pot boils on a Steady small fire in 30 ±5 game minutes; heavy rain drops an uncovered fire one state per 10 game minutes; matches light 95% in calm air and 50% in wind over 1,000 rolls; an average-rhythm bot lights the bow drill 65–75% of the time, and its time per fire falls 40% by the tenth |
| Human test | The M3 tester test, plus: after any failed fire, the tester can say why from the messenger's line (wet tinder, no kindling, wind) |
| Out of scope | Cooking food (M5), cold acting on the body (M4; M3 only shows the fire's warmth radius), the stove (M8), flint and steel (M9) |

**M4 build spec.** Turn on the body, so every hour costs something and every warning has a cause.

| Part | Spec |
| --- | --- |
| Packages | `sim.body.BodyModel` (Mifflin–St Jeor, the activity table, weight change ÷ 2,400, BMI, macros; energy; sleep; warmth with feels-like, wet, the three hypothermia stages and shivering kcal; hydration with the half-day blackout; bugs; nutrition with vitamin C, scurvy and the five-day same-food rule; Spirits) and `sim.body.PartModel`; `body` (`BodyTicker`, `DamageRouter`, `SosFlow` for Endless and Hardcore, injuries, illness, medicine and remedies); presets in `core.rules`; the vitals strip and body figure in `client.hud`; `client.camera` with the `frost` and `body_fx` layers (frost edges, shiver, breath fog below 0 °C, slowed steps); `story.Journal` (auto-logged firsts, the weight graph) |
| Data | `body/standard.json` (activity kcal, thresholds, recovery days); `preset/<id>.json` (Gentle, Standard, Brutal; Standard holds this doc's numbers) with the `[body]` keys of `yearofice-common.toml`; `injury/`, `injury_source/`, `illness/`, `body_part/`, `remedy/` |
| Build order | (1) Clock-driven metabolism and weight. (2) Water. (3) Energy and sleep. (4) Warmth and wet. (5) Injuries, Condition and the medkit. (6) Macros and vitamin C. (7) Spirits. (8) Bugs. (9) SOS in both modes. (10) Presets and sliders. (11) HUD, cold effects and journal |
| Automated tests | The M4 test above, plus: BMR for the default body is 1,794 kcal ±1; a soaked player loses 6 warmth a minute, as in Danger in detail; an Endless SOS returns the player after 14 game days at BMI 19 with only the starting kit and every built piece intact; a Hardcore SOS closes the world and writes the "why I left" journal line; scurvy starts on day 30 or 31 without vitamin C; every vital change of more than 5 points in a game hour logs a cause line (100% coverage); a dehydration sim blacks out for 12 game hours |
| Human test | The owner plays 2 game days with only the crate kit and, at every warning, can say what caused it |
| Out of scope | Food beyond the crackers (M5), winter gear (M7), cabin heat (M8); parasite rules live here, but raw meat that triggers them arrives in M5 and M6 |

**M5 build spec.** Give the player enough ways to eat that a good week is earned, then hand out goals.

| Part | Spec |
| --- | --- |
| Packages | `decay` (freshness clocks by temperature, per cut, settled lazily on read); `food.Cooking` (roast, stew +15%, render fat); `forage` (plant blocks, ripeness, identification, taste tests, lookalikes); `food.traps` (snares, deadfalls, theft after 48 h, dusk and dawn rolls, trapline pins, and the passive set lines, weirs, gill nets and minnow traps); small game in `ecosystem` on Ecology's core (hare, squirrel, grouse and ptarmigan: freeze and flee only); throwing (rocks, sticks) and the light bow (blunts, no strength cap yet) in `hunt`; `hunt.fishing` (the five phases, tackle components, the lockstep fight); `food.preserve` (smoke rack, drying rack, hot smoke); `food.cache` (rations, days left, mice); `food.garden` (germination formula, degree-days, frost draw, water, weeds, thieves, seed saving); prompts and the logbook in `story`; the notebook page and the tension arc in `client` |
| Data | `fish/<id>.json` (fight profiles, example below); `plant/<id>.json`; `crop/<id>.json`; `prompt/` (at least 15 prompts); `decay/<curve>.json`; station recipes for snares, deadfalls, rod, gorge, net segments, minnow trap, weir basket and both racks |
| Build order | (1) Spoilage and cooking. (2) Foraging and identification. (3) Snares, deadfalls and the trapline map. (4) Small game and throwing. (5) Rod fishing and tackle. (6) Passive fishing. (7) Racks and hot smoking. (8) The cache. (9) The garden. (10) Morning prompts, logbook pages and the notebook |
| Automated tests | The M5 tests above, plus: snares on a trail catch 4 times as often as off it, ±10% over 10,000 rolls; meat spoils in 2 days at 15 °C and 6 at 5 °C; liver lasts 0.7 days against meat's 2.3 on a cool fall day; a scripted average reeler lands a 1 kg grayling in 90% or more of fights and a 10 kg pike on plant-fiber line in 25–45%; a gill net never rolls with 3 segments; a stew returns 15% more kcal than a roast; no taste test kills in 10,000 trials per toxic plant; on a winter fixture an unguarded cache loses 2–3% a week to mice and one ringed by 3 deadfalls loses none; the cache's days-left matches the sim within 1 day; every prompt fires on its fixture state and stays silent on its counter-fixture |
| Human test | The M5 human gate above |
| Out of scope | Big game and the strength cap (M6), ice fishing (M7), the smokehouse, cold smoke and root cellar (M8), the fish wheel and hens (M9). Pemmican's recipe exists, but its lard mostly comes from M6 kills |

Fight profiles are data. Pull is in kg-force per kg of fish; tension 100% is the line's rated hold, a willow rod absorbs 30% of each spike, and wet plant-fiber line holds 25% less:

```json
{
  "id": "northern_pike",
  "kg": [1, 10],
  "kg_rare": 15,
  "run_pull_x": 0.6,
  "run_every_s": [4, 9],
  "run_length_s": [1.5, 3],
  "jump_chance": 0.05,
  "head_shake": { "every_s": [2, 5], "spike_x": 1.4 },
  "dive": false,
  "tire_per_s": 0.04,
  "bite_through_no_leader": 0.5,
  "bait": { "live_minnow": 2.0, "fish_chunk": 1.0, "spoon": 1.2, "worm": 0.2 }
}
```

**M6 build spec.** Fill the valley with animals that act from needs, and make big game and danger readable.

| Part | Spec |
| --- | --- |
| Packages | The `ecosystem` Brain parts (the shared state set, drives, activities); `ecosystem.senses` (sight, hearing, scent; scent points with 24 h decay, bears path within 400 m); `ecosystem.tier` (Active, Nearby, Abstract, records and catch-up rolls); `ecosystem.population` (regional counts, birth calendar, the hare cycle); sign decals; `ecosystem.foodweb` (predation); `ecosystem.encounter` (the director with the 30–60 day lull and the first-3-days bear; warnings, chase break-off at 150 m or a fire, bear responses, the adrenaline sprint); `ecosystem.species` classes only where data is not enough (bear, moose, wolf pack); bows in `hunt` (draw weight against strength, the 60% cap, sway, the 6 s hold), the `yearofice:arrow` projectile (50 m/s with drop, branch deflection), hit zones, blood trails, calls and blinds; `hunt.butcher` (cuts, organs, spoil timers, carrying whole) and hauling (pack frame, kill-site chest); thin ice and felling hazards through `BodyService.applyInjury`; animal sounds |
| Data | `species/<id>.json` with senses (102 entity types in M6, the moose having arrived in the M2.5 slice; Minecraft engine › Milestones in Minecraft); `hitzone/<species>.json`; `loot_table/butchering/<species>.json` in 100 g units, with step times in the species `carcass` block; weapons and arrowheads in the item data maps |
| Build order | (1) Agent states and tiers. (2) Drives and senses. (3) Scent points and bears. (4) Moose. (5) The other species from data. (6) Predation, populations and sign. (7) Strength cap, ballistics, hit zones and blood. (8) Calls and blinds. (9) Butchering and hauling. (10) Encounter director and the first-3-days bear. (11) Chases, warnings and bear responses. (12) Thin ice and felling hazards |
| Automated tests | The M6 test above, plus: a downwind stalker is never smelled; a moose smells an upwind player at 150 m ±10%; a still player is seen at half range and a crouching one at a third; a heart-lung hit drops a moose within 50–150 m; blood sign is gone at 2 game hours; after a moose kill, moose encounters in that range stay under 20% of base for 30 days; on all of 100 seeds a bear is seen by the end of day 3, never closer than 60 m; every charge follows its warning sound; ice under 5 cm breaks, 10 cm holds a player, 20 cm holds a loaded sled; a fresh body cannot reach full draw on the longbow, and the scripted good player can by day 60–120; the hare cycle peaks every 8–12 simulated years; taking 2 moose a year from one range for 5 years lowers its count |
| Human test | The owner takes a first moose using only sign, wind and the blood trail, and calls any death or near-death "my fault, and I saw it coming" |
| Out of scope | Winter wolf behavior and the siege (M7), wolf taming (M9), hide processing and hide-tier gear such as the sinew-backed bow (M7) |

**M7 build spec.** Run the full year, so winter is the test the summer prepared for.

| Part | Spec |
| --- | --- |
| Packages | `story.Calendar` (365 days from May 1, month normals, the day–night swing, 70% anomaly carry-over; shipped with the clock in M2); `story.Weather` (seasonal Markov tables, rain to snow below 1 °C, forecast at 80% and 60%); `sim.time.Sun` (daylight at 64°N); `sim.env.SnowModel` and `IceModel` with `world.pass.SnowPass` and `IcePass` (freeze-up, 1 cm a day below −10 °C, breakup and its warning); seasons, alerts, events (each rare event rolled on May 1), ambitions, the year card and the optional real-calendar mode in `story`; ice holes, skin-over, jigging and tip-ups in `hunt.fishing` and `food.traps`; hides in `hunt` (scrape, rawhide, fat cure, smoke-tan); winter clothing, frostbite per body part and wet clothing in `body` and `item`; salmon runs, caribou migration and the winter pack in `ecosystem` (run size, spawned-out drift, testing a weak player, the siege); leaf colour, snow, ice and the aurora in `client.season` and `client.sky`; winter sounds |
| Data | The Markov tables of `weather/interior_alaska.json` (its 12 month normals shipped in M2); `event/`; season windows in `season/` tables; `ambition/`; clothing and hides in the wear data map and station recipes |
| Build order | (1) Calendar and temperature. (2) Weather and forecast. (3) Daylight. (4) Snow. (5) River ice. (6) Seasonal events and alerts. (7) Hides and winter gear. (8) Ice fishing. (9) Salmon run and caribou. (10) Winter wolves. (11) Rare events. (12) Ambitions, year card and real-date mode |
| Automated tests | The M7 test above, plus: over 30 simulated years each month's mean is within 2 °C of the weather file's normals; tomorrow's forecast is right 80% ±3% and the outlook 60% ±3%; Dec 21 has 3.5–4.5 h of daylight; breakup falls between Apr 15 and May 10 in 9 of 10 years; snow passes 40 cm by Jan 15 in 8 of 10 years; an ice hole skins over within an hour below −20 °C; fur mittens keep hands safe at −25 °C and not at −26 °C; every year-card number matches the save's logs |
| Human test | On a debug save with a tier 2 shelter and a stocked cache, the owner plays a December week for an hour; he always knows his next task, and calls winter dangerous but fair |
| Out of scope | The chinked stove cabin (M8), wolf taming (M9), radio supply drops (M9), regrowth after a wildfire (M9; M7 only burns and marks the region) |

**M8 build spec.** Let the player build a home that shrinks the wood chore, and copy it.

| Part | Spec |
| --- | --- |
| Packages | `build` on the 1 m grid (Minecraft engine › The 1 m grid): ghosts, frames and the 1-hour refund; pieces and support; `build.rooms` (flood fill, room temperature, the heat model); outpost repair and the 11 cabin steps (scribing, the roll-up game, chinking, the cabin card); firewood (armloads, seasoning by `yearofice:moisture`, the woodshed's 2× drying, auto-feed from a woodpile within 3 m); stoves and chimneys in `fire` (smoke without one); storage block entities and crafting from storage within 10 m; structures (smokehouse, drying racks, root cellar, woodshed, latrine); cold smoke in `food.preserve`; blueprints as structure templates; the job queue; ageing in `decay` (5% log shrink, roof snow and sag over 60 cm, sod greening, creosote); outposts; `travel` (cut trails, skid trails, log bridges, worn paths, floating logs and jams, the raft) |
| Data | `data_maps/block/heat.json` and `piece.json` (insulation, durability, materials and game hours for every piece); `multiblock/<id>.json`; `aging/`; blueprints saved as structure templates in the world, not in data |
| Build order | (1) Grid, ghost and refunds. (2) Pieces and rooms. (3) Outpost repair. (4) Firewood. (5) Stove and chimney. (6) The log cabin and cabin card. (7) Storage and quality-of-life rules. (8) Food structures. (9) Blueprints and the job queue. (10) Floating logs and the raft. (11) Outposts, trails and bridges. (12) Cabin aging |
| Automated tests | The M8 test above, plus: every test layout reads as a closed room, and removing one wall piece opens it; an open door drops room temperature by at least 5 °C on a −30 °C night; a simulated winter burns 2 ±0.5 armloads a day in the chinked cabin and 5 ±0.5 in a drafty one; a piece taken down within 1 game hour refunds 100%; the woodshed seasons wood at twice the open-air rate; the root cellar holds 2–4 °C all winter; a log dropped upstream drifts to the first snag or to camp; a bot's first cabin takes 160 ±20 game hours |
| Human test | The owner repairs the outpost and lays one wall course without help, and calls the ghost preview and refunds fair |
| Out of scope | The forge and iron tools (M9), the stovehouse, the canoe, physics-based building (left out on purpose) |

**M9 build spec.** Open the wider world and the long-term projects that keep later years fresh.

| Part | Spec |
| --- | --- |
| Packages | All 16 home-valley regions with pre-warm in `worldgen` (layers built on the worker pool within 600 blocks); world responses in `world` and `ecosystem` (regrowth, worn trails, beaver dams, forest fire, bears that learn); the forge and bloomery in `build` (ore to bloom to bar, then iron-tier recipes); the canoe in `travel`; taming and the companion in `ecosystem` (the three starts, trust, decay, the February rule; follow, whistle, warnings, tracking, the 10 kg pack, pinning hares, holding a moose, the wolf drive); the sled team in `travel` (team of 3 or more, bolting below 90 trust); the radio (mast repair, daily logs) and supply drops in `story`; livestock care in `food.livestock` with the animals in `ecosystem.species` (goats, hens, feed, eggs, milk, manure); the stovehouse; the fish wheel |
| Data | `region/<cell>.json` (16 regions with archetype and biome bias: tundra, big lake, canyon, mining creek); `taming/<species>.json`; `drop/` (supply-drop table); `livestock/<species>.json`; iron-tier recipes |
| Build order | (1) Region streaming. (2) World responses. (3) Forge and the iron tier. (4) Canoe. (5) Wolf taming and the companion. (6) Sled team. (7) Radio and supply drops. (8) Livestock. (9) Stovehouse. (10) Fish wheel |
| Automated tests | The M9 test above, plus: heights match within 0.1 m and rivers continue across every region border; a region builds in under 3 s with no frame over 50 ms; trust falls 5 a day without food and 30 when struck; a wolf under 25 trust raids the cache and under 10 leaves (never on Gentle, returning in 10–20 days on Standard, for good on Brutal); the February rule fires only below 80 trust and never on Gentle; one full season of logs yields exactly one supply drop; a season-long fish wheel shrinks the run 4 years later; cleared ground shows fireweed in the first summer, birch at 2 years and spruce at 6; bog iron re-forms in 3–5 years and floods expose 1–3 outcrops a year |
| Human test | The owner plays an hour of year 2 and names a project he wants next without a prompt |
| Out of scope | Co-op, physics-based building, other region packs, player-versus-player |

### Sub-agents

Each department agent leads three specialist sub-agents, so every system has an owner deep enough to get it right.

&#91;embedded content: studio org chart · 1 lead, 8 departments, 24 sub-agents\]

**How rounds use them.** The sub-agents are a roster, not a standing team. Following the studio playbook's cost rules, a feature round wakes only the two or three sub-agents whose files it touches.

- Each sub-agent owns named classes and data folders (for example `ecosystem.senses`), reads only its department's brief, and changes other systems only through the shared layers' services.
- Each department agent merges its sub-agents' work and runs that department's tests before handing it to the studio lead.
- The testers' sub-agents work on a frozen copy of the build:
  - The balance simulator runs scripted years for the numeric milestone tests.
  - The playtest bots play the first hours like a new player.
  - Design review scores each build against the pillars and the tedium check.

**File ownership.** The studio rules give every package and data folder one owner; this table names them inside each department (Minecraft engine › Ownership by package and `studio/ownership.json` are the authority). A sub-agent reaches anything it does not own through the shared layers' services. `core`, `api`, `net` and `item` belong to the studio lead.

| Department | Sub-agent | Owns |
| --- | --- | --- |
| World generation | Terrain and water | `worldgen.Noise`, `Landform`, `Erosion`, `CoarseRivers`, `Lakes`, `Rivers`, `Falls`, region layers; `worldgen_params/valley.json` |
| World generation | Biomes and resources | `worldgen.Cover`, the tree builders, `Trails`, `Ranges`, `FishSpots`; biome JSONs |
| World generation | Set pieces and seeds | `worldgen.Archetype`, `SetPieces`, `Landmarks`, `StartSite`, the outpost template; `tools/debugmap`, `tools/seeds`; `archetype/`, `setpiece/` (placement) |
| Ecosystem | Animal AI | The `ecosystem` Brain parts, `ecosystem.senses`, `encounter`, `species`; `species/` |
| Ecosystem | Populations and sign | `ecosystem/tiers`, `populations`, `sign`, `predation`, `director`, `scent`, `salmon`, `migration` |
| Ecosystem | Wolf taming | The winter pack, taming and the companion in `ecosystem`; `taming/` |
| Food and hunting | Hunting | `hunt` (bows, arrows, calls, blinds, `hunt.butcher`); `hitzone/`, `loot_table/butchering/` |
| Food and hunting | Trapping | `food/traps`, `cache`, `spoilage`, `cooking`, `preserve` |
| Food and hunting | Fishing | `hunt.fishing` (the fight, tackle, ice fishing, the fish wheel) and passive gear in `food.traps`; `fish/` |
| Body and survival | Body model | `sim.body`, `body` (ticker, sleep, warmth, hydration, bugs, nutrition, Spirits, `SosFlow`), presets; carry rules in `item.inventory` by review; `body/standard.json`, `preset/` |
| Body and survival | Fire and water | `fire`; `data_maps/item/fuel.json` |
| Body and survival | Health and plants | Injuries, illness and medicine in `body`, thin-ice and felling injuries, `forage`, `food.garden`; `injury/`, `injury_source/`, `illness/`, `remedy/`, `plant/`, `crop/` |
| Building and crafting | Crafting | Stations, recipes, jobs, blueprints, storage, forge and gear in `build`; `build.work`; `build.wood.FellingService`; `recipe/`, `action/`; item numbers by review |
| Building and crafting | Building | Pieces, frames, rooms, the cabin, ageing, structures, firewood and the stovehouse in `build`; livestock care in `food.livestock`; `data_maps/block/piece.json`, `heat.json`, `multiblock/` |
| Building and crafting | Outposts and travel | `build/outposts`; `travel/*` |
| World and story | Weather and events | `story.clock`, `Calendar`, `Weather`, seasons, events, the real date; the snow and ice models in `sim.env` (passes in `world.pass` by review); `weather/`, `event/`, `season/` |
| World and story | Goals and ambitions | Prompts, alerts, ambitions, the journal, the year card and onboarding in `story`; `prompt/`, `ambition/` |
| World and story | Story and radio | The logbook, radio and supply drops in `story`; `drop/`; the text and loot of `setpiece/` |
| Look, sound and UI | Messenger HUD | `hud`, `client.hud`, `client.screen`, `client.messenger`, `client.pack`, `client.input` |
| Look, sound and UI | Art | `client.render`, `client.sky`, `client.season`; the texture and model generators (Art and audio production) |
| Look, sound and UI | Audio | `sound`, `client.audio`; `tools/soundgen`; `sounds.json` and the credits file |
| Testers | Balance simulator | `tools/sim` over `com.yearofice.sim`; `tests/hashes.json` |
| Testers | Playtest bots | `gametest` and its fake-player bots, `tools/lint-gen`, `./gradlew smoke` |
| Testers | Design review | The scoring section of PLAYTEST notes |

**Round procedure.** A round takes the next feature from the current milestone's build order. Goal, team size and check-ins follow the studio rules.

1. The Director picks the feature and approves a round ticket (below) naming its sub-agents, files, hooks, numbers and tests.
2. The studio lead wakes the named sub-agents through their department agents.
3. Each sub-agent reads its department brief, the ticket and its own files. It never reads the blueprint.
4. The sub-agent builds, runs its unit tests and the smoke test, then writes its handoff.
5. The department agent merges, runs the department's tests and the smoke test, and settles any hook conflict.
6. The studio lead merges into `main` of the mod repository `ruhanu155/years-of-survival` and tags a frozen copy as `round/M<n>-r<k>` (for example `round/M5-r4`).
7. The testers named in the ticket run on the frozen copy and log results in the PLAYTEST notes.
8. The studio lead reports the round to the Director. A failure becomes the next round's ticket.
9. When every test in a milestone's spec passes on one frozen copy, the Director signs the milestone off to Ruhan, who plays its human test.

A ticket that breaks the studio rules on team size or file ownership is split by the Director before any work starts.

**Round ticket.** Tickets are JSON, so tools can check them against the ownership table before work starts:

```json
{
  "round": "M5-r4",
  "feature": "Snares and the trapline map",
  "agents": ["food.traps", "client.map"],
  "files": {
    "food.traps": ["src/main/java/com/yearofice/food/traps/", "src/main/resources/data/yearofice/recipe/"],
    "client.map": ["src/main/java/com/yearofice/client/map/"]
  },
  "hooks": { "emit": ["trap.caught", "trap.checked"], "listen": ["world.dawn", "world.dusk"] },
  "numbers": ["on trail x4", "beside cover x1.5", "checked within 5 m more than once a day x0.5", "taken after 48 h"],
  "tests": ["yearofice:food/snare_trail_ratio", "yearofice:food/snare_theft_48h", "smoke"],
  "out_of_scope": ["lodge snares under ice", "wire snares for wolves"]
}
```

**The hook bus, in Minecraft.** Each hook is a NeoForge event class in `com.yearofice.api.event`, posted on `NeoForge.EVENT_BUS` on the server, and `ask()` becomes a read-only service method such as `BodyService.warmth(player)` (Minecraft engine › Code rules; Minecraft development › Events and hooks). The smoke test fails on any posted event without a registered class. The table keeps the hook names; the second column gives the event and its package.

| Hook | Emitted by | Payload | Main listeners |
| --- | --- | --- | --- |
| `world.minute`, `world.hour`, `world.day`, `world.dawn`, `world.dusk` | `ClockEvent` from `story.clock` | Game time | Anything with a timer |
| `weather.changed` | `WeatherChangedEvent` from `story.Weather` | State, °C, wind m/s | Fire, body, animals, render |
| `fire.state` | `FireStateEvent` from `fire` | Fire id, state, position, heat | Body warmth, animals, onboarding |
| `scent.created` | Hunting, trapping, fishing | Position, strength, decay hours | `ecosystem.senses` (`ScentEvent`) |
| `body.vital` | `VitalChangedEvent` from `body` | Vital, value, cause line | HUD, journal, prompts |
| `animal.killed` | `AnimalKilledEvent` from `ecosystem.wound`, for kills by `hunt` and `food.traps` | Species, position, hit zone | Director, populations, journal, ambitions |
| `item.crafted` | `StationCraftEvent` from `build.stations` | Item id, count | Practice, notebook, ambitions |
| `piece.placed`, `piece.removed` | NeoForge `BlockEvent.EntityPlaceEvent` and `BreakEvent` on `#yearofice:pieces` | Piece id, position | Rooms, save |
| `room.changed` | `RoomChangedEvent` from `build.rooms` | Room id, °C, heat loss | Body warmth, cabin card |
| `ice.changed` | `IceChangedEvent` from `env.water` | Water body, thickness cm | Thin ice, ice fishing, travel |
| `trap.caught`, `trap.checked` | `TrapEvent` from `food.traps` | Trap id, species | Map pins, journal |
| `prompt.offer` | Any department | Prompt id, numbers, priority | `HudBus.offerPrompt` (`PromptOfferEvent`) |
| `journal.first` | Any department | Key, text | `story.Journal` (`JournalFirstEvent`) |

**Department briefs.** Each brief is at most two pages in `yearofice/briefs/`, named after its department. It has six fixed parts: goal, numbers (the tables copied verbatim from the sections listed in Engine rules), files owned, hooks emitted and heard, tests, and what is out of scope until which milestone. Each brief carries the blueprint date it was cut from. When a number changes in a carried section, the studio lead reissues that brief before the next round that touches it, and the Director checks it against the blueprint.

**Handoff.** A sub-agent's handoff is at most 300 words, in five parts:

1. Files changed.
2. Events added or changed, each with its class in `api.event`.
3. Tests run, each with pass or fail.
4. Every number tuned, old to new, with the reason.
5. Open issues for the department agent.

The department agent adds the merge, its own test results and any conflict it settled, then passes it to the studio lead. The studio lead folds the round's handoffs into one report for the Director.

**Testers in detail.**

- **Balance simulator.** `tools/sim` runs the pure models of `com.yearofice.sim` (body, food, ecosystem and calendar) headless on the JVM, with no Minecraft running, at no less than one game year per real minute. It holds a named script for every numeric test (starvation, good player, three years, 100 years of events, five-year homestead) and writes one CSV row per game day plus a pass or fail line.
- **Playtest bots.** Server-side fake players with scripted inputs, in GameTests and a long-run harness, play three personas. The Newcomer follows the beacon and every prompt, the Wanderer ignores prompts and explores, and the Grinder repeats the cheapest chore to probe the tedium check. Each logs real time per chore and every collapse with its cause.
- **Design review.** Scores each frozen copy on the 1–10 scale from the studio rules, once per pillar and once for the tedium check, so the Director sees which pillar slipped. It writes three lines into the PLAYTEST notes: the best moment, the worst chore and one fix.

## Open decisions

- [x] **Look:** decided: Minecraft blocks with the mod's own resource pack, Iris shaders optional (Minecraft engine tab). Art and sound, decided 2026-10-19: generated by the studio's scripts, plus found models, textures and sounds whose licence allows redistribution in a modpack (CC0, CC-BY or MIT), listed in a credits file; vanilla textures may be referenced at runtime but are never copied into the jar.
- [x] **Map:** a new seed every run, with the start-site checks keeping every seed fair.
- [x] **Year length:** a full 365 days, one per calendar day.
- [x] **Game day length:** decided 2026-10-19: the player sets it at world creation, 24–72 real minutes, default 24. Every rate is defined per game minute, so the setting only rescales the world clock; it is stored as a world option in yearofice\_world.
- [x] **Default mode:** decided 2026-10-19: Endless (flown out and back after an SOS) is the default; Hardcore is chosen at world creation and locked for that world. The Endless SOS screen offers "Stay out" for 14 days (default), 1, 2 or 5 years (RECONCILE 78, 148).
- [x] **The wolf:** decided 2026-10-19, by realism preset: a tamed wolf never leaves on Gentle; on Standard it is warned and recoverable; on Brutal it can leave for good (RECONCILE 148).
- [x] **Co-op later:** decided 2026-10-19: solo first, co-op after 1.0. Time rules stay co-op-safe now: time-lapse and the close clock run per player, sleep skips only when every player sleeps, and the SOS jump is per player.

Also decided by Ruhan on 2026-10-19: the loader is NeoForge 1.21.1 (Java 21, ModDevGradle 2.x, GeckoLib and Curios), converted from the Forge 1.20.1 MDK in his YearOfIce folder. Every system gets /yoi debug commands, all tuning lives in yearofice-common.toml with presets, and the messenger's in-world name is the Trail Messenger. The mod is for his personal use only: no public release work, packwiz only to install the pack on his own machine, and his own worlds must survive every update. Builds and tests run in the cloud once the build domains are allowed, and the real client runs on his linked computer. Each decision below keeps its options and trade-offs as the record. None is still open: Ruhan decided the default mode, the SOS stay-out choice and the wolf on 2026-10-19. Under the studio rules Ruhan decides. The Director brings each one to him before the milestone it blocks, and records his answer here as a ticked box.

**Look.** Decided: the engine is Minecraft, so the look is Minecraft blocks with the mod's own resource pack and optional Iris shaders (Minecraft engine tab). The table is kept as the record of the choice made before the engine switch.

| Option | Gains | Costs |
| --- | --- | --- |
| Hybrid pixel and glyph (the studio's raymarcher) | Cheapest; reuses tested code; low resolution hides thin set dressing across 16 regions | Thousands of trees at 512 m view is the hardest case for a raymarcher at 60 fps; small sign (blood drops, tracks, ice cracks) is hard to read at glyph size |
| Stylized low-poly meshes | Instanced trees and terrain are routine in a mesh engine; meshes can be generated in code, so 60 items and 25 pieces need no artist; sign reads as decals | A new pipeline to write; a flat look needs strong light, fog and frost effects to feel cold |
| Toward realistic | Closest to the video's shader pack | Most build time and frame time; realistic animals and hands need assets the studio cannot make; first to miss the 60 fps budget |

Superseded by the Minecraft engine. The old acceptance bar carries over to the resource pack: a blood trail reads clearly at 15 m in vanilla lighting and under Iris.

**Game day length.** Decided on 2026-10-19: a world-creation setting from 24 to 72 real minutes, default 24. The table shows what each length gives and costs.

| Option | Real hours per year | Gains | Costs |
| --- | --- | --- | --- |
| 24 min (current default) | About 146 | Seasons change every 12 real hours of play; every number in this doc is tuned to it | Real-time actions cost game time fast: 1 real second is 1 game minute, so a stalk or a fish fight eats an hour |
| 36 min | About 219 | More gets done per daylight hour; December's 4 hours of light last 6 real minutes instead of 4 | A year runs past 200 hours; summer drags |
| 48–72 min | About 292–438 | Days feel close to human pace | Seasons crawl, and repeat chores take more real minutes, which strains the tedium check |

Body, food and spoilage numbers are in game time, so they hold at any length; only real-time actions shift their game cost. The setting changes only the World clock rate: 720 ÷ L game deciseconds per tick for a day of L real minutes, so L takes the whole-number steps 24, 30, 36, 40, 45, 48, 60 and 72. The Close, Travel, Work, Until-done and Sleep rates are fixed, so a stalk, a kilometre on foot and a night's sleep cost the same game time at every length. It is stored as a world option in yearofice\_world, and every tuning value stays per game minute in yearofice-common.toml. The M5 human gate still plays one game week at 24 and one at 36 to confirm the default.

**Default mode.** Decided on 2026-10-19; it blocked the SOS step of M4.

| Option | Gains | Costs |
| --- | --- | --- |
| Endless default, Hardcore optional (as written) | Long builds never vanish; fits "grow without an end" and the lost-progress complaint | An SOS can feel like a free respawn unless the time away hurts |
| Hardcore default | Matches the source mod; every winter carries full stakes | One mistake ends 100 or more hours; most players quit after the first loss |
| No default: the player chooses at world creation | The player owns the stakes | One more choice before the first hour, which a new player cannot judge |

Decided on 2026-10-19 (Ruhan): Endless, with Hardcore chosen at world creation and locked for that world. Stay out: 14 days (default), 1, 2 or 5 years; the longer stays run the AgeWorld pass (Decay and time › Time away) and ship in M9. The 14 days away run on catch-up rolls (spoilage, mice, raids, fires going out), so an SOS costs a real share of stores without costing the base.

**The wolf.** Decided on 2026-10-19; it blocked the taming round of M9.

| Option | Gains | Costs |
| --- | --- | --- |
| As written: below 80 trust it may leave in February | Keeps the wolf wild; February gets a stake | Months of care can vanish in one roll; a three-wolf team becomes years of risk |
| Warned and recoverable | The messenger warns in late January when trust is under 80 ("Restless; keep it close"). Daily feeding and a warm shelter lower the chance. A wolf that leaves may return within 10–20 days at 40 trust | More rules to tune and test |
| Never leaves | Simplest; nothing is lost | The wolf becomes a pet, and "a tamed wolf is still a wolf" weakens |

Decided on 2026-10-19 (Ruhan): never leaving on Gentle, warned and recoverable on Standard, and leaving for good on Brutal. That ties the choice to the realism presets instead of a new menu. The M9 test (Companion in 30–60 game days) holds under all three.

**Co-op later.** Decided on 2026-10-19: solo first, co-op after 1.0. The save format still ships in M2 with the fields reserved.

| Option | Gains | Costs |
| --- | --- | --- |
| Single-player save, nothing reserved | Simplest | Adding player ids to every edit later forces a save migration |
| Single-player now, fields reserved | A `players` array with one entry and a player id on each edit, about a day of work. The video's separate-valley design (own seed each, hidden status, last one in wins) then needs only a small shared status record | Fields that sit unused for years |
| Build co-op now | None until co-op is wanted | Netcode, sync and server costs; clashes with streaming and catch-up simulation; outside the single-player first year |

Taken: single-player now with fields reserved. Every edit record and player attachment already keys on the player's UUID, so co-op needs no save migration. Time rules are co-op-safe from M2: time-lapse and the close clock run per player, sleep skips only when every player sleeps, and the SOS 14-day jump applies to that player only. RECONCILE 9 and the lead's time-debt proposal (Minecraft development › The clock) are the recorded co-op design, not built before 1.0. The co-op build is planned after 1.0, starting from those rules.
