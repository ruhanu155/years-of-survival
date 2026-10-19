# Birds and the air

The sky over the valley holds 78 birds and 18 flying insects; 45 birds and 10 insects are new here. This tab gives every bird one Minecraft flight brain and a mob sheet at build depth. Its systems make birds a calendar, an alarm, a forecast, a larder and a thief. It builds on the Bestiary's states, populations and helpers, and changes none of their rules.

## What is already covered

**33 birds and 8 flying insects exist; this tab gives each a full mob sheet and adds 45 birds and 10 insects.** Rules already written stay where they are. This tab adds flight, bodies, nests, sounds and play, and references the home section by name.

| Group | Existing species | Registry id | Defined in |
| --- | --- | --- | --- |
| Grouse | Spruce grouse, willow ptarmigan, ruffed grouse, sharp-tailed grouse | `yearofice:spruce_grouse`, `willow_ptarmigan` (first named here), `ruffed_grouse`, `sharp_tailed_grouse` | Blueprint › New species; Bestiary › Ruffed grouse, Sharp-tailed grouse |
| Waterfowl | Mallard, green-winged teal, Canada goose, greater white-fronted goose, trumpeter swan | `yearofice:mallard`, `green_winged_teal`, `canada_goose`, `white_fronted_goose` (all first named here), `trumpeter_swan` | Bestiary › What is already covered; Trumpeter swan |
| Loons | Common loon, Pacific loon | `yearofice:common_loon`, `pacific_loon` | Bestiary › Common loon; Waters and fish |
| Crane | Sandhill crane | `yearofice:sandhill_crane` | Bestiary › Sandhill crane |
| Water edge | Mew gull, Arctic tern, belted kingfisher, American dipper, tree swallow, cliff swallow | `yearofice:mew_gull`, `arctic_tern`, `belted_kingfisher`, `american_dipper`, `tree_swallow`, `cliff_swallow` (first named here) | Waters and fish; Water calls and encounters; Creative concepts › Animals move in |
| Day raptors | Bald eagle, golden eagle, osprey, gyrfalcon | `yearofice:bald_eagle`, `golden_eagle`, `osprey`, `gyrfalcon` | Bestiary; Water calls and encounters |
| Owls | Great horned, great gray, northern hawk owl | `yearofice:great_horned_owl`, `great_gray_owl`, `hawk_owl` | Bestiary |
| Woodpeckers | American three-toed, black-backed | `yearofice:three_toed_woodpecker`, `black_backed_woodpecker` | Bestiary › Woodpeckers |
| Corvids | Raven, gray jay | `yearofice:raven` (first named here), `gray_jay` | Blueprint › Ravens tell the truth; Bestiary › Gray jay |
| Songbirds | Black-capped and boreal chickadees, common redpoll, snow bunting | `yearofice:black_capped_chickadee`, `boreal_chickadee`, `common_redpoll`, `snow_bunting` | Bestiary |
| Insects | Mosquito, black fly, no-see-um, bumblebee, spruce bark beetle, dragonfly, caddisfly adults, stonefly adults | Pressure terms, `yearofice:black_fly_swarm`, `midge_swarm`, the `bumblebee` and `dragonfly` particles (RECONCILE 101), the bark-beetle event, the hatch windows | Blueprint › Bugs; Bestiary › Frog and insects in depth; Waters and fish › Caddisfly larva, Stonefly nymph |

**Two id conflicts are settled here.** The jay is one entity, `yearofice:gray_jay`; Creative concepts' `canada_jay` is an alias and its data file folds into `gray_jay.json`. Species classes follow the lead's package map: birds are `com.yearofice.ecosystem.species.bird.<Name>Entity` on `YoiBird`, and species that differ only in data use the rig class with a species id (Minecraft development › The entity class tree).

**Not added.** Steller's jay is a coastal bird and absent inland, so it stays out. Hummingbirds and the coastal sea ducks are left out for the same reason. Domestic hens stay with the homestead rules; birds here only prey on them.

## Flight in Minecraft

**Every bird runs one shared flight brain, `YoiBird`, with species numbers in data.** Vanilla flying navigation is built for bees and parrots over short hops, so it is not used. Birds steer by waypoints and short raycasts instead, which is cheap and reads as real flight from 200 m.

### Flight states

**Eleven states cover every bird.** They sit beside the 13 states in Blueprint › How animals act. Awareness, Alert, Flee and Habituated work as written; the flight state only decides how the body moves.

| State | Entered when | Movement | Leaves when |
| --- | --- | --- | --- |
| Perched | Default for perching species | Still on a perch point; idle, preen and look loops | A drive picks a target, or awareness reaches the flush value |
| Ground | Feeding on the ground or ice | Walk or hop at 0.3–1.2 m/s; peck loop | Same as Perched |
| Swim | Waterfowl, loons, grebes, phalaropes, gulls | Paddle at 0.5–1.5 m/s; tip-up or dive for food | Takeoff, Dive or haul-out |
| Takeoff | A target over 20 m away, or a flush | Vertical burst (grouse, songbirds), jump (dabblers) or a run on water (divers, loons, swans) | Height 3 m reached: Flap |
| Flap | Travel | Cruise speed, climb up to 2 m/s | At cruise height in lift: Glide or Soar; near target: Land |
| Glide | Descent or level flight in lift | No flapping; sink by glide ratio | Lift lost: Flap |
| Soar | A thermal or ridge lift over 0.5 m/s | Circles 30–60 m wide, climbing at the lift rate | Height reached or lift gone |
| Hover | Rough-legged hawks, ospreys, harriers, terns, kingfishers, hawk and short-eared owls | Holds a point 5–30 m up for 3–20 s | Strike: Dive; nothing seen: Flap |
| Dive | A strike or a plunge | Falcon stoop up to 90 m/s; plunge into water; swim dive for 10–60 s | Hit or miss: Takeoff or surface |
| Land | Within 6 m of the target | Flare, legs down, 1 s | On the perch, ground or water |
| Flock | Member of a flock entity | Moves as part of the flock (below) | The flock splits or lands |

**Steering.** A bird flies toward a waypoint and samples 3 rays ahead at 4, 8 and 16 blocks. A blocked ray turns it up to 40° a second, or climbs it. Cruise height follows the canopy: 8–20 m above the `MOTION_BLOCKING` heightmap for travel, 60–200 m for migration.

**Perch points.** `PerchIndex` scans each chunk on load and keeps up to 16 perch points: spruce tips, snags, posts, roof ridges, boulders and cliff ledges. Blocks that count are in the tag `yearofice:perch_blocks`. A perch point keeps its last 3 users, so an eagle snag gains whitewash and pellets under it over weeks.

### Flight numbers by body plan

| Body plan | Cruise | Sprint | Takeoff | Glide ratio | Turn | Soars |
| --- | --- | --- | --- | --- | --- | --- |
| Grouse and ptarmigan | 12 m/s | 20 m/s burst, 3 s | Vertical roar, 1 s | 4 | Wide | No |
| Dabbling ducks | 18 m/s | 25 m/s | Jump from water, 0.5 s | 6 | Medium | No |
| Diving ducks, loons, grebes | 20 m/s | 28 m/s | Run on water 10–100 m | 4 | Wide | No |
| Geese and swans | 16 m/s | 22 m/s | Run 10–20 m | 10 | Wide | Rarely |
| Crane | 15 m/s | 20 m/s | 3 running steps | 15 | Wide | Yes, in kettles |
| Shorebirds | 15 m/s | 22 m/s | Jump, 0.3 s | 8 | Tight | No |
| Gulls and terns | 10 m/s | 15 m/s | Jump | 14 | Tight | Yes |
| Eagles, buteos, osprey, harrier | 12 m/s | 30 m/s stoop | 2 flaps from the perch | 18 | Medium | Yes |
| Falcons and accipiters | 15 m/s | 90 m/s stoop (peregrine), 25 m/s dash | Drop from the perch | 10 | Tight | Falcons yes |
| Owls | 8 m/s | 12 m/s | Silent drop | 9 | Medium | No |
| Woodpeckers, corvids | 10 m/s | 14 m/s | Jump | 8 | Medium | Raven yes |
| Songbirds | 9 m/s | 13 m/s | Jump | 4 | Tight | No |

**Runways matter.** A loon needs 30–100 m of open water to take off (Bestiary › Common loon), a swan 20 m, a scaup 15 m. Each species' `takeoff_run_m` is checked against open water or clear ground ahead; a bird with no runway dives or runs instead of flying. That is why a loon on a shrinking lead is trapped and a swan on a small pond can be cornered by canoe.

### Flocks

**A flock is one server entity, not 200 mobs.** `BirdFlockEntity` holds a count, a species, a centre, a velocity and a seed. The client draws the members as boids around that centre from the seed, so every player sees the same shape.

| Form | Server cost | Members | When | Can be shot |
| --- | --- | --- | --- | --- |
| Single bird | 1 Active slot, full brain | 1 | Raptors, owls, loons, cranes landed, any nesting bird | Yes |
| Ground flock | 1 Active slot | 4–40 | Ptarmigan, redpolls, waxwings, buntings, ducks on water | Yes: a shot picks the member nearest the ray |
| Air flock | 1 Active slot | 2–2,000 | Ducks and geese in transit, shorebirds, swallows | Yes, below 60 m height |
| Sky flock | None; client only | 20–2,000 | Migration passing over at 80–600 m | No, too high |

**Flock rules.** Members keep 1–3 body lengths apart, match heading within 15°, and turn as a wave from the side nearest a threat. Geese and cranes fly in a V or line with 4–6 m spacing; shorebirds and redpolls fly in tight balls that flash as they turn.

**Splitting.** A shot, a raptor strike or a landing at a new site can split a flock into two entities, each with its share of the count. Below 4 members a flock dissolves into single birds. A landed ground flock promotes up to 8 members to real entities within 32 m of the player; the rest stay drawn.

**Kills and the count.** A member killed is removed from the flock's count and from the region's population at once. The dead member becomes a carcass entity of its size class where it falls, and a tiny one is pocketed whole as `yearofice:whole_game` (Minecraft development S10), with the drift rules for water in Blueprint › Swimming and working in water.

### Lift: thermals and ridges

**Soaring birds read the air, and the air is computed.** Each hour `AirLift` builds a lift field on the 32 m wind grid from sun height, surface type, cloud cover and wind.

```latex
w_{thermal} = 2.5 \cdot s(h_{sun}) \cdot k_{surface} \cdot (1 - c_{cloud}) \cdot [T_{air} > 5]
```

```latex
w_{ridge} = v_{wind} \cdot \sin(\theta_{slope}) \cdot [\text{windward}]
```

s is 0–1 from sun height (0 below 15°, 1 above 40°). k is 1.0 for gravel bars, 0.9 for a burn, 0.8 for south slopes, 0.6 for tundra, 0.2 for snow and 0 for water. Thermals run from about 10:00 to 17:00 in May to August.

- **Who uses it.** Eagles, buteos, harriers, cranes, gulls and ravens enter Soar where lift exceeds 0.5 m/s. They climb at the lift rate and glide out at their glide ratio, so they cross the valley without flapping.
- **What the player sees.** Eagles kettling over the C3 gravel bars at noon, ravens tumbling on the ridge wind at Twin Horn, cranes rising in a 400-bird spiral before they move on.
- **What it tells.** Soaring birds over a slope mean wind on that slope, which the player can use for stalking: the messenger names it once learned, "Ravens on the ridge wind. Wind is up the west face."

### Migration flyways

**Migrants follow lines the map already has.** At world creation `Flyways` traces 2–3 splines across the 8 km home valley, continued as straight lines across the Interior and the Beyond (World map › three rings): the main river valley, the Windgap Pass saddle (D1), and the Big Lake to Slough Country line (A4 to A3). Spring flocks fly them north-west, fall flocks south-east.

1. **Passage.** Each migration wave (Migration and the dawn chorus, below) releases sky flocks along a flyway at 80–600 m. They pass over in 2–8 real minutes and play their calls through the distant-sound payload.
2. **Stopover.** On each pass a flock rolls to land at a stopover site on its line: open water for ducks, A3 meadows for cranes, gravel bars for buntings and longspurs. Landing chance is 20% in fair weather and 70% under a headwind over 8 m/s or rain, so storms put birds down.
3. **Fallout.** A spring day with snow or a north wind over 10 m/s after a south-wind run is a fallout day. Every wave in progress lands within 1 km of the flyway, so the slough fills with ducks and the willows with warblers for 1–2 days.
4. **Departure.** Landed flocks leave at dusk on the next day with a following wind under 6 m/s, which is when the player hears them lift.

### How birds react to the player

**Flush distance is the bird's answer to the player.** Each species has a base flush distance; posture, approach angle and habituation scale it.

```latex
d_{flush} = d_{base} \cdot m_{posture} \cdot m_{approach} \cdot m_{season} \cdot m_{habit}
```

m\_posture is 1.0 walking, 1.6 running, 0.6 crouched and 0.3 still. m\_approach is 1.5 head-on and 0.7 on a tangent, which rewards walking past birds instead of at them. m\_season is 0.5 for a hen on eggs and 1.5 for a hunted flock in September. m\_habit is 0.2 once Habituated.

**The flush chain gives the player away.** A bird that flushes with an alarm call adds 15 awareness to every animal within 40 m, mammals included. Jays, magpies, ravens, yellowlegs and terns add 25. A careless walk through a nesting meadow alerts the moose at its far edge before the player sees it.

**Alarm calls feed AlarmCalls.** New callers join the helper table in Bestiary › Animals that help the player with their own rows (Reading birds, below). The rules stay: learning a call takes two hearings within 10 game minutes of seeing the cause, and wind over 6 m/s halves the range.

**Mobbing finds the owl.** Each game hour a songbird within 40 m of a roosting owl, hawk or shrike notices it with chance 0.3. A mobbing bird draws others within 60 m at 0.6 every 30 s, up to 15 birds.

- **What the player hears.** A rising knot of chickadee "dee" notes, kinglet chatter, robin "tut-tut" and jay screams, carried 150 m as `yearofice:ambient.mobbing`.
- **What it reveals.** The mob circles within 3 m of the owl, so following it finds the bird, its pellets and often a molted feather. The owl leaves after 10–20 game minutes or when the player comes within 15 m.
- **Pishing.** The player's voice call "pish" (Calls the player can make, below) doubles the join rate. Pishing near an unseen owl in daylight is the best way to find one.

### Performance caps

**Birds have their own share of the animal budget.** The engine tab caps 40 Active and 200 Nearby animals. Active animals share 8 ms a tick on average, and birds get 2.5 ms of it, 3.0 ms at peak (Minecraft development › Performance; RECONCILE 91).

| Bird tier | Cap | Tick rate | Counts toward |
| --- | --- | --- | --- |
| Active single birds | 14 | Every tick within 64 m; every 4 ticks beyond | The 40 Active |
| Active flocks | 6 entities | Every 2 ticks | The 40 Active, 1 each |
| Near songbirds | 10 within 32 m | Every 4 ticks, a reduced brain (perch, hop, sing, flush) | Their own cap |
| Drawn flock members | 400 models total | Client only | Client frame |
| Sky flocks | 4 at once | Client only, from a server schedule | Nothing |
| Chorus voices | 12 of the 32 ambience voices | Client only | Ambience cap |

- **Far members.** Drawn members beyond 48 m render as 2-frame sprites in one batched draw. Beyond 160 m a flock is a smudge of 8–40 dots.
- **Demotion.** Birds demote at the Bestiary's 180 m, farthest first, except a bird the player is aiming at, sketching or stalking.
- **Singers.** A songbird heard but beyond 32 m is only a chorus voice. It becomes a near songbird when the player walks within 32 m, using the same slot from `hash(seed, region, species, slot)`, so the singer is really on that spruce.

## How every mob sheet works

**Seventy-eight birds share 15 rigs, one nest system and one set of player verbs.** Each sheet below gives only what differs: hitbox, rig specials, plumage, nest, eggs, sounds, verbs, food web and one moment. The rules in this section apply to every bird unless its sheet says otherwise.

### Rigs and the shared animation set

**One GeckoLib model per body plan, scaled and textured per species.** Every rig carries the shared set: `perch`, `walk` or `hop`, `peck`, `preen`, `look`, `alert`, `takeoff`, `flap`, `glide`, `land`, `die`. Water rigs add `swim` and `dive`. Specials are listed per rig.

| Rig | Species | Bones | Special animations |
| --- | --- | --- | --- |
| `grouse` | Spruce, ruffed, sharp-tailed grouse; willow and white-tailed ptarmigan | 14 | `display_fan`, `drum`, `lek_dance`, `snow_dive`, `broken_wing`, `bud_feed` (in a willow) |
| `dabbler` | Mallard, teal, pintail, wigeon, shoveler | 14 | `tip_up`, `jump_takeoff`, `head_pump`, `filter_skim` (shoveler), `broken_wing` |
| `diver_duck` | Scaup, bufflehead, goldeneye, harlequin, merganser | 14 | `run_takeoff`, `head_throw` (goldeneye), `rapid_ride` (harlequin rides current), `fish_carry` |
| `goose_swan` | Canada and white-fronted geese, trumpeter swan | 18 | `graze`, `hiss`, `wing_threat`, `charge_swim`, `nest_sit` |
| `loon_grebe` | Common and Pacific loons, horned and red-necked grebes | 16 | `long_run_takeoff`, `rear_tremolo`, `chick_carry`, `weed_dance` (grebes) |
| `crane` | Sandhill crane | 20 | `dance_leap`, `bow`, `stick_toss`, `unison_call`, `kettle` |
| `shorebird` | Snipe, yellowlegs, spotted sandpiper, phalarope, golden-plover | 14 | `probe`, `teeter`, `spin`, `winnow_dive`, `spruce_alarm`, `broken_wing` |
| `gull_tern` | Mew gull, Arctic tern | 14 | `hover`, `plunge`, `head_strike` |
| `raptor_soarer` | Bald and golden eagles, osprey, rough-legged hawk, harrier | 18 | `soar`, `hover`, `stoop`, `talon_grab`, `mantle`, `plunge`, `sky_dance` |
| `raptor_fast` | Gyrfalcon, peregrine, merlin, goshawk, sharp-shinned hawk | 16 | `dash`, `stoop`, `bind`, `mantle`, `nest_strike` |
| `owl` | All 7 owls | 16 | `head_swivel` (270°), `snow_plunge`, `wing_clap`, `hoot_throat`, `hover` |
| `woodpecker` | Three-toed, black-backed, flicker | 14 | `trunk_climb`, `drum`, `chisel`, `flake_bark`, `anting`, `bound_flight` |
| `corvid` | Raven, gray jay, magpie | 16 | `cache`, `carry`, `roll` (raven), `snow_slide` (raven), `tail_flick` |
| `kingfisher_dipper` | Kingfisher, dipper | 12 | `hover`, `plunge`, `fish_beat`, `dip_bob`, `underwater_walk` |
| `songbird` | Every small perching bird | 12 | `perch_sing`, `sally`, `hang`, `cone_pry`, `song_flight`, `impale`, `scratch`, `skim_drink` |

**Plumage swaps textures, not models.** Each species lists texture sets with date windows in `plumage`; the renderer picks by calendar date and sex. A moulting week blends two textures with a 50% patch mask, so a ptarmigan in October shows white wings on a brown body.

**Death.** `die` plays a 0.6 s fold, then `ecosystem.wound.DeathPipeline` replaces the bird with a carcass entity of its size class, with no drops (Minecraft development › Carcass hand-off). Birds shot in flight fall on a ballistic arc from their velocity; on water they drift by the current rules.

### Nests and eggs

**A nest is a block entity with a life.** `yearofice:bird_nest` is placed by the species' nest rule on the first day of its laying window. Its `nest_type` sets the model: ground cup, scrape, water platform, stick platform, cavity, burrow, mud cup or floating mat.

| Stage | Length | What the player finds | Taking eggs |
| --- | --- | --- | --- |
| Building | 2–7 days | An empty lined cup or bare platform | Nothing to take |
| Laying | 1 egg a day (2 for grouse) | Cold eggs, no hen in daytime for ducks and grouse | Up to a quarter of the eggs (rounded down) and the hen lays on; one more and she abandons |
| Incubating | Species value, 18–36 days | A sitting bird that flushes at 1 m (ground nesters) or its flush distance | Each egg taken: 50% she abandons; touching the nest without taking: 10% |
| Brooding | Until fledging | Chicks or ducklings with the adult; down and shell membranes left in the nest | No eggs; the down is free to take after the brood leaves |
| Old | Until the next spring | A weathered nest, sometimes reused | Down, feathers, a sketch |

**Finding nests.** A sitting bird is part of the ground until it flushes or the player inspects within 2 m. Then the messenger pins it: "Nest: pintail, 7 eggs." Ground nests flush at 1 m with a broken-wing display (Blueprint › Drives and daily schedules), which is the tell.

**Eggs are one item.** `yearofice:wild_egg` carries the component `yearofice:egg` with the species and the day incubation began. Eggs use the existing spoilage row (rate 3.0, 18 days). Egg calories follow mass at 1.5 kcal per gram.

**The float test.** Holding right mouse with an egg over water takes 3 game seconds.

| Result | Incubation day | As food |
| --- | --- | --- |
| Sinks and lies flat | 0–3 | Full kcal |
| Sinks and stands on end | 4–12 | ×0.8 kcal; the journal notes "a developed egg" |
| Floats | 13 or more, or addled | 40% chance of the food-poisoning illness in Blueprint › Health, medicine and nutrition |

A lit lantern at night shows the same result on inspect, by candling. Eggs never appear rotten-looking, so the test is the only safe read.

**Renesting.** Ducks, grouse, ptarmigan, shorebirds and sparrows that lose a nest before June 10 renest once within 400 m, with 2 fewer eggs. Swans, cranes, loons and raptors do not renest that year.

### What the player does with birds

**Seven verbs, the same for every bird.** Each sheet says which apply and gives the species' numbers.

| Verb | How | What it gives |
| --- | --- | --- |
| Observe | Crouch still 5 s with the bird in view | Its name at Study level 1; behaviors count toward level 3 (Creative concepts › Field study) |
| Identify by call | Hear it, then see it calling within 10 game minutes, twice | The call is learned: the messenger names the singer, and at Study 3 marks its bearing within 150 m |
| Sketch | The field book, 3 game minutes | Study level 4; each sheet lists its sketchable behaviors |
| Call in | A voice call or a call item (Water calls and encounters › Bird calls and decoys, plus the new calls below) | Draws the species toward the player |
| Hunt | Throwing stick, blunt arrow, broadhead, snares, noose pole, perch snare, net | Meat, fat, feathers, down |
| Collect | Pluck, pick up molts, take nest down and eggs | Fletching, down, decoration, food |
| Read | Watch what it does | A fact about weather, predators, water, fish, berries or carrion (Reading birds, below) |

**Blunt arrows and small birds.** A blunt hit on head or body kills any bird under 1.5 kg (Blueprint › The hit-zone model). Birds under 100 g give 15–90 kcal and 2–4 game feathers; they are emergency winter food, not a living.

**The perch snare.** A new Start-tier trap: a 30 cm stick with 3 sinew or hair nooses, set on a feeder, a berry bush or a frozen carcass. It catches songbirds, jays and grosbeaks at 0.15 a day per perching species present. A named or habituated bird is never caught; the snare skips it.

### Calls the player can make

**Four new voice calls join the call table.** They use the rhythm bar and q score in Water calls and encounters › How every call works, with the same overcalling rules.

| Call | Input | Draws | Window | Range |
| --- | --- | --- | --- | --- |
| Pish | Hold V while looking level with the empty hand, in short bursts | Chickadees, kinglets, warblers, juncos, sparrows and jays within 30 m come to 3–8 m, 60% each; doubles mobbing joins | All year by day | 30 m |
| Loon wail | Voice wheel segment (hold V): cupped hands, one long rising note | A loon within 1 km answers 50%; a pair answers together 20% | Ice-out to Sep 30, dusk and night | 1 km |
| Crane rattle | Voice wheel segment (hold V): a throat roll, 3 s | Landed cranes within 400 m answer 60%; a passing flock low enough circles once 15% | Migration windows | 400 m |
| Owl trill | Voice wheel segment (hold V): a quick whistle run of 10–15 notes | A boreal owl within 300 m answers 40% Feb–Apr nights; songbirds mob the sound by day 30% | February–April | 300 m |

**Answering is free; landing is not.** A call can make a bird reply or circle, but birds only land by their own rules. Calling at setting wings or a nest counts as overcalling.

## Grouse and ptarmigan

**Five ground birds that feed the player all year.** Spruce and ruffed grouse hold the forest, sharp-tails the burns, and two ptarmigan the willows and the crags. Their rules in Blueprint › New species and the Bestiary stand; this sheet adds bodies, nests, sounds and the white-tailed ptarmigan.

| Species | Registry id | Hitbox w × h | Mass | Plumage and moult | Where, count, dates |
| --- | --- | --- | --- | --- | --- |
| Spruce grouse | `yearofice:spruce_grouse` | 0.45 × 0.45 | 0.52 kg | Male black throat, red comb; hen barred brown; one moult Jul–Sep | Taiga everywhere, 15–25 per map, all year |
| Ruffed grouse | `yearofice:ruffed_grouse` | 0.45 × 0.45 | 0.6 kg | Grey phase 80%, red phase 20%; toe fringes Nov–Apr | B2 birch, riparian, all year |
| Sharp-tailed grouse | `yearofice:sharp_tailed_grouse` | 0.45 × 0.5 | 0.8 kg | Pale speckled; purple neck sacs on the lek | D2, C4, all year |
| Willow ptarmigan | `yearofice:willow_ptarmigan` | 0.42 × 0.42 | 0.6 kg | White Oct 20 – Apr 20; male red-brown head May–Jul; moult blends over 21 days | Flocks of 10–30; A2 tundra in summer, willow flats in winter |
| White-tailed ptarmigan (new) | `yearofice:white_tailed_ptarmigan` | 0.35 × 0.35 | 0.35 kg | White Oct 15 – May 1, mottled grey summer; white tail all year | A1 and C1 above 1,000 m, 6–20; all year on the crags |

| Species | Nest | Laying window | Eggs | Incubation | Hen |
| --- | --- | --- | --- | --- | --- |
| Spruce grouse | Ground cup under a low spruce bough | May 15 – Jun 5 | 4–7, 22 g, 33 kcal | 23 days | Flushes at 1 m |
| Ruffed grouse | Ground cup at the base of a birch or a log | May 10 – Jun 1 | 9–12, 22 g | 24 days | Flushes at 1 m with a hiss |
| Sharp-tailed grouse | Grass cup within 1 km of the lek | May 10 – Jun 1 | 10–12, 25 g | 23 days | Flushes at 2 m |
| Willow ptarmigan | Scrape in dwarf birch on the tundra | May 25 – Jun 10 | 6–10, 22 g | 22 days | The cock guards; both perform `broken_wing` |
| White-tailed ptarmigan | Scrape among rocks above 1,000 m | Jun 1 – 20 | 4–7, 20 g | 23 days | Sits until touched: the tamest nest in the game |

| Sound event | Species | When | Carries | Meaning |
| --- | --- | --- | --- | --- |
| `entity.spruce_grouse.flutter` | Spruce grouse | Male display flight, May | 80 m | A displaying male; the hen cluck works now |
| `entity.spruce_grouse.cluck` | Spruce grouse | Alert | 20 m | It has seen the player and is freezing |
| `entity.ruffed_grouse.flush_roar` | Ruffed grouse | Flush | 60 m | Gone; it lands 50–100 m off in a tree |
| `entity.willow_ptarmigan.go_back` | Willow ptarmigan | Dawn, Apr–May; flush | 600 m | A territorial cock; "go-back, go-back" |
| `entity.white_tailed_ptarmigan.cluck` | White-tailed ptarmigan | Alert | 30 m | On the scree, close and invisible |
| `entity.grouse.wing_whir` | All | Takeoff | 40 m | Shared takeoff sound |

| Species | Take with | Yield | Sketch behaviors | Call in | Diet | Eaten by |
| --- | --- | --- | --- | --- | --- | --- |
| Spruce grouse | Stick, blunt, noose pole, snare | 600 kcal, 15 feathers | Display fan, needle feeding, freeze, hen with brood | Hen cluck, wing flap | Spruce needles, berries | Goshawk, lynx, fox, great horned owl |
| Ruffed grouse | Blunt, light bow | 600 kcal, 15 feathers | Drumming, budding, snow dive | None | Aspen buds, catkins | Goshawk, lynx, owls |
| Sharp-tailed grouse | Light bow from the pole blind | 700 kcal, 15 feathers | Lek dance, sac display | None | Buds, seeds, grasshoppers | Harrier, merlin, fox |
| Willow ptarmigan | Stick, snare, noose pole, blunt | 400 kcal, 12 feathers, 4 soft | Snow roost, budding, cock's "go-back" | "Go-back" call | Willow buds | Gyrfalcon, golden eagle, fox, lynx |
| White-tailed ptarmigan | Hand, active noose, stick | 250 kcal, 8 feathers | Snow burrow, rock freeze | None | Willow and heather buds | Golden eagle, gyrfalcon |

- **Spruce grouse.** The "fool hen" freezes and is taken with a stick (Blueprint › Weapons and arrowheads). In winter it eats only spruce needles, so its crop holds green needles and its meat tastes of resin: −1 to the meal-variety count, never 0 kcal. *Moment:* a hen leads 6 chicks across the trail in July, and every chick freezes on her cluck, invisible on the moss.
- **Ruffed grouse.** Its rules are in the Bestiary. New: its winter food is aspen and birch buds after 15:00, and a bird budding in a bare aspen at dusk shows against the sky from 100 m. *Moment:* the player steps over a snow hummock and a bird explodes from under the boot.
- **Sharp-tailed grouse.** The lek is in the Bestiary. New: its tracks circle a dancing ground so tightly that in April the lek shows as a trampled ring of snow from 200 m. *Moment:* a merlin strikes a dancing male and the whole lek drops flat for 10 minutes.
- **Willow ptarmigan.** Snow roosts, flock flushes and the gyrfalcon freeze are in Blueprint and Bestiary. New: in April a flock of 100–300 gathers in valley willows before moving to the tundra, a single day of easy birds (`bud_feed` across the whole flat). A ptarmigan's feathered feet (the "snowshoe" sign) give 4 game feathers marked "soft", the down for the pillow in the Items tab. *Moment:* a cock in half-white plumage sits on a snowbank in May shouting at the player's boots.
- **White-tailed ptarmigan (new).** The smallest grouse lives only on the high crags, where it eats willow buds above the snow and never leaves. Its flush distance is 1 m, so a player on the scree at −30 °C can take one by hand or by the active noose pole. It is the reason to carry 2 m of cordage on the iron climb to C1. *Moment:* a white rock on the scree blinks.

## Waterfowl

**Thirteen ducks, geese and a swan make the lakes busy from breakup to freeze-up.** Arrival, decoys, calls, the molting lake and the floating noose log are written elsewhere (Blueprint › Setting up the hunt; Water calls and encounters). This section adds eight new ducks and the swan's nest.

| Species | Registry id | Hitbox | Mass | Plumage and moult | Where, count, dates |
| --- | --- | --- | --- | --- | --- |
| Mallard | `yearofice:mallard` | 0.55 × 0.45 | 1.2 kg | Drake green head Sep–Jun; eclipse brown Jul–Aug, flightless 25 days | 40% of flocks; nests on every archetype |
| Green-winged teal | `yearofice:green_winged_teal` | 0.4 × 0.35 | 0.35 kg | Drake chestnut head; eclipse Jul–Aug | 25% of flocks; ponds |
| Canada goose | `yearofice:canada_goose` | 0.75 × 0.8 | 4 kg | Black neck, white chinstrap; moult Jul 1 – Aug 10 | 25% of flocks; A3 islands |
| White-fronted goose | `yearofice:white_fronted_goose` | 0.7 × 0.75 | 2.5 kg | White face, barred belly | 10% of flocks; passage only |
| Trumpeter swan | `yearofice:trumpeter_swan` | 1.0 × 1.1 | 11 kg | All white; cygnets grey to October | A3, A4, 1–3 pairs |
| Northern pintail (new) | `yearofice:northern_pintail` | 0.55 × 0.45 | 0.9 kg | Drake brown head, white neck stripe, 10 cm tail pin; eclipse Jul–Aug | Flooded meadows, A3; first duck back, Apr 25 – Sep 25 |
| American wigeon (new) | `yearofice:american_wigeon` | 0.5 × 0.4 | 0.8 kg | Drake white crown, green eye band | Lakes and sedge edges, A3, A4; May 5 – Sep 30 |
| Northern shoveler (new) | `yearofice:northern_shoveler` | 0.5 × 0.4 | 0.6 kg | Spoon bill; drake green head, rusty sides | Shallow ponds, B4; May 5 – Sep 20 |
| Lesser scaup (new) | `yearofice:lesser_scaup` | 0.5 × 0.4 | 0.8 kg | Drake black and grey, purple head gloss | Big Lake, tundra ponds; May 10 – Oct 5; the latest nester |
| Bufflehead (new) | `yearofice:bufflehead` | 0.35 × 0.35 | 0.4 kg | Drake white head patch | Small lakes, B4 ponds; May 1 – Oct 15 |
| Common goldeneye (new) | `yearofice:common_goldeneye` | 0.5 × 0.45 | 1.0 kg | Drake white cheek spot; whistling wings | Open leads from Apr 10; 4–12 winter below Long Falls in 1 year in 3 |
| Harlequin duck (new) | `yearofice:harlequin_duck` | 0.45 × 0.4 | 0.6 kg | Drake slate blue with white harlequin marks | Fast clear creeks, D3 canyon, 2–6 pairs; May 15 – Aug 15 |
| Common merganser (new) | `yearofice:common_merganser` | 0.6 × 0.45 | 1.5 kg | Drake white with green head; serrated bill | The main river and Big Lake, 4–12; Apr 20 – Nov 10 |

| Species | Nest | Laying | Eggs | Incubation | Down in nest |
| --- | --- | --- | --- | --- | --- |
| Mallard, teal, wigeon, shoveler, pintail | Ground cup in sedge within 200 m of water | May 15 – Jun 15 (pintail from May 5) | 6–10; 50 g (teal 26 g), 75 kcal | 23–28 days | 5 g |
| Lesser scaup | Ground cup on an island | Jun 10 – 30 | 8–10, 55 g | 25 days | 6 g |
| Bufflehead, goldeneye | Tree cavity: an old flicker hole or a nest box | May 5 – 30 | 8–10, 50–65 g | 30 days | 8 g |
| Harlequin | Ground under willow on a creek bank | Jun 1 – 20 | 5–7, 60 g | 28 days | 6 g |
| Merganser | Cavity or rock crevice by the river | May 10 – Jun 5 | 8–11, 70 g | 32 days | 8 g |
| Canada goose | Mound on an island or a muskrat lodge | May 10 – 30 | 4–7, 160 g, 240 kcal | 28 days | 15 g |
| Trumpeter swan | 2 m mound of sedge on a lodge or islet | May 1 – 25 | 4–6, 320 g, 480 kcal | 33 days | 25 g |

| Sound event | Species | When | Carries | Meaning |
| --- | --- | --- | --- | --- |
| `entity.mallard.quack` | Mallard | Alarm, social | 300 m | Ducks on that water |
| `entity.green_winged_teal.whistle` | Teal | Flight | 200 m | A small flock overhead, fast |
| `entity.canada_goose.honk` | Canada goose | Flight, landing | 2 km | Geese moving; low and loud in September means snow (Land and seasons) |
| `entity.white_fronted_goose.laugh` | White-fronted goose | High passage | 2 km | "Specklebellies" going over |
| `entity.northern_pintail.whistle` | Pintail | Drake display, April | 300 m | The first ducks are back |
| `entity.american_wigeon.whistle` | Wigeon | Flocks on water | 300 m | Three-note "whee-whew"; grazing on the shore |
| `entity.common_goldeneye.wing_whistle` | Goldeneye | Flight | 150 m | A ringing whistle in fog: open water ahead |
| `entity.common_goldeneye.display` | Goldeneye | Head-throw, Apr–May | 200 m | A buzzy "zeee-at" over a lead |
| `entity.harlequin_duck.squeak` | Harlequin | Social | 80 m | Over the creek noise, a mouse-like squeak |
| `entity.common_merganser.croak` | Merganser | Alarm | 150 m | Fish-eaters on the reach |
| `entity.trumpeter_swan.hiss` | Swan | Nest defence | 40 m | Back the canoe off now |

**Verbs and food web.** All ducks are taken with a broadhead or blunt over decoys, the floating noose log in the moult, or the wolf's fetch. Yields use the existing duck row scaled by mass: 1,150 kcal per kg of meat at 55% of body mass, 20 feathers and 0.02 kg down per duck.

| Species | Sketch behaviors | Call in | Diet | Eaten by |
| --- | --- | --- | --- | --- |
| Mallard, teal | Tip-up, head pump, hen with ducklings | Duck call | Seeds, snails, insects | Fox, mink, pike, eagle, peregrine |
| Geese | Grazing, V formation, gander guard | Goose flute, mound decoys | Sedge, horsetail, berries | Fox, eagle, wolf, grizzly at the moult |
| Swan | Pondweed pull, pair bugle, cygnet ride | None | Pondweed, sedge roots | Wolf, bear (cygnets) |
| Pintail, wigeon, shoveler | Display, grazing, filter circle | Duck call (×0.7 for shoveler) | Seeds, sedge, invertebrates | Fox, harrier, mink |
| Scaup, bufflehead, goldeneye | Dive, raft, head-throw, duckling jump | Duck call ×0.5 | Snails, larvae, amphipods | Eagle, mink, pike (ducklings) |
| Harlequin | Rapid ride, boulder preen | None | Stonefly and caddis larvae | Eagle, mink, goshawk |
| Merganser | Group drive, fish carry | None | Small fish, fry | Eagle, goshawk |

- **Mallard, teal, Canada and white-fronted geese.** Existing rules stand. New: mallards are the only ducks that stay on the hot-spring outflow (C2) into December, 2–6 of them, a winter duck for a hard week. *Moment:* the player's decoy spread pulls a teal flock that drops in like a thrown fist of gravel. In September a goose family walks the A3 bar at dawn, and the gander stands guard on a log while five graze.
- **Trumpeter swan.** The canoe charge is in the Bestiary. New: the nest mound is seen from 300 m as a white bird on a brown hummock. Taking a swan egg brings the cob's charge at once, on foot or by canoe, and the pair abandons 50%. Cygnets are flightless until early October; a late pair caught by freeze-up loses them. *Moment:* in October a family of five swans walks off a freezing oxbow at dusk, cygnets grey behind the white.
- **Northern pintail (new).** The first duck back, on flooded meadows while the lakes are still ice. Its early nests sit in last year's open sedge, so spring floods and early bears take many. It renests more than any duck: 90% after a loss. *Food web:* seeds and insects; prey of foxes, harriers and peregrines. *Moment:* on April 28 a pair circles the smoking breakup ice, the drake's pin tail sharp against the snow.
- **American wigeon (new).** It grazes sedge and the garden on shore like a goose, 20–30 m from water. A wigeon flock on the garden bed eats 0.2 kg of greens a day in May and June (Blueprint › Crop farming), so the brush fence matters. *Moment:* the player looks out at dawn to find twelve wigeon on the garden bed.
- **Northern shoveler (new).** It filters pond water with its spoon bill, swimming in tight circles in pairs. Shovelers on a pond mean a rich pond: mosquito larvae, snails and the pike that eat ducklings. *Moment:* six shovelers spin in a ring on a beaver pond, heads down, like a slow wheel.
- **Lesser scaup (new).** A diving duck that rafts by the hundred on Big Lake in late September before leaving. It dives 3–6 m for snails and amphipods, so a scaup raft marks a weedy shoal where whitefish feed. It is the last duck to nest, so its eggs are still fresh in late June. *Moment:* a raft of 400 scaup lifts off the lake with a roar heard at the ice-shack site.
- **Bufflehead (new).** The smallest diving duck nests in old flicker holes 2–6 m up in aspen by ponds. Ducklings jump to the ground on day 1, then walk up to 500 m to water. *Moment:* the player watches seven ducklings drop one by one from an aspen and bounce on the moss.
- **Common goldeneye (new).** The first duck on open leads in April, and the only duck some winters below Long Falls. Its wing whistle carries 150 m and is a water sign in fog or the dark of December. It nests in cavities and takes nest boxes (Eggs, down and feathers, below). *Moment:* a December night at −35 °C, and a whistle of wings passes over the canyon in the dark.
- **Harlequin duck (new).** It rides the white water of the D3 canyon and fast clear creeks, diving for stonefly and caddis larvae. Harlequins mean clear, fast, food-rich water: grayling and Dolly Varden water with a stonefly emergence (Waters and fish). It is never on glacial water. *Moment:* a drake surfaces in the tail of a rapid, climbs onto a boulder, and preens in the spray.
- **Common merganser (new).** A fish-eater that hunts in groups, driving grayling and whitefish into the shallows. A group of 6 or more working a reach takes 1 kg of fish a day from its spots (Blueprint › Fish stocks). Fish flee to the shallows ahead of them, where a dip net or a spear finds them for 10 game minutes. *Food web:* small fish and salmon fry; eagles and goshawks take mergansers. *Moment:* eight mergansers line up across the river and swim upstream in unison, and the water ahead of them boils with fry.

## Loons, grebes, crane and shorebirds

**The voices of the lakes and the wet tundra.** Loons and the crane exist; two grebes and five shorebirds are new. Shorebirds are small meat but big signs: they alarm, display and tell the player about water and the season.

| Species | Registry id | Hitbox | Mass | Plumage | Nest and eggs | Where, dates |
| --- | --- | --- | --- | --- | --- | --- |
| Common loon | `yearofice:common_loon` | 0.7 × 0.55 | 4.5 kg | Black and white checker; grey Oct | Shore mat on Loon Island, Jun 1–20; 2 eggs, 150 g, 29 days | A4, ice-out to Sep 30 |
| Pacific loon | `yearofice:pacific_loon` | 0.6 × 0.5 | 2 kg | Grey nape, black throat | Shore mat on tundra ponds; 2 eggs, 100 g | A2 ponds, Big Lake |
| Horned grebe (new) | `yearofice:horned_grebe` | 0.35 × 0.35 | 0.45 kg | Golden "horns" May–Aug; grey after | Floating weed mat anchored in sedge, Jun 1–25; 4–6 eggs, 22 g, 24 days | Beaver and tundra ponds, May 10 – Sep 30 |
| Red-necked grebe (new) | `yearofice:red_necked_grebe` | 0.45 × 0.45 | 1 kg | Rufous neck, white cheek | Floating mat on lake edges, May 25 – Jun 15; 3–5 eggs, 40 g | A3 oxbows, A4, May 5 – Oct 1 |
| Sandhill crane | `yearofice:sandhill_crane` | 0.7 × 1.2 | 3.5 kg | Grey with red crown; rust-stained in summer | 1 pair in 3 years nests in A3: sedge mound; 2 eggs, 180 g, 30 days | Passage Apr 25 – May 10, Aug 25 – Sep 15 |
| Wilson's snipe (new) | `yearofice:wilsons_snipe` | 0.3 × 0.25 | 0.11 kg | Striped brown | Ground cup in wet sedge, May 20 – Jun 15; 4 eggs, 16 g | Bogs and wet meadows everywhere; May 5 – Sep 20 |
| Lesser yellowlegs (new) | `yearofice:lesser_yellowlegs` | 0.3 × 0.35 | 0.09 kg | Grey speckled, bright yellow legs | Ground scrape near muskeg spruce, May 20 – Jun 10; 4 eggs | C4, B4, pond edges; May 5 – Aug 15 |
| Spotted sandpiper (new) | `yearofice:spotted_sandpiper` | 0.25 × 0.25 | 0.045 kg | Spotted breast May–Jul | Ground cup on a gravel bar above the waterline, Jun 1–20; 4 eggs | River bars, C3, creeks; May 15 – Aug 31 |
| Red-necked phalarope (new) | `yearofice:red_necked_phalarope` | 0.25 × 0.25 | 0.035 kg | Female brighter: red neck; male dull | Male incubates a sedge cup, Jun 5–20; 4 eggs | A2 tundra ponds; May 25 – Aug 10 |
| American golden-plover (new) | `yearofice:american_golden_plover` | 0.3 × 0.3 | 0.15 kg | Black belly, gold-spangled back May–Jul | Lichen scrape on dry tundra, Jun 1–20; 4 eggs, 30 g | A2, D1 tundra; May 15 – Aug 25 |

| Sound event | Species | When | Carries | Meaning |
| --- | --- | --- | --- | --- |
| `entity.pacific_loon.croak` | Pacific loon | Flight between ponds | 500 m | The ponds hold fish |
| `entity.red_necked_grebe.bray` | Red-necked grebe | Pair duet, May–Jun | 800 m | A wailing, braying duet; a nesting pair |
| `entity.horned_grebe.trill` | Horned grebe | Display | 200 m | A small pond with fish fry |
| `entity.sandhill_crane.unison` | Crane | Landed pair or dance | 2 km | Cranes on the meadow, not just passing |
| `entity.wilsons_snipe.winnow` | Snipe | Display dives, 21:00–04:00, May 10 – Jul 10 | 1 km | A hollow "hu-hu-hu-hu" from the sky; wet ground below |
| `entity.wilsons_snipe.scaipe` | Snipe | Flush | 60 m | A rasping "scaipe" and a zigzag flight |
| `entity.lesser_yellowlegs.alarm` | Yellowlegs | Nest season, from a spruce tip | 300 m | "Tew-tew-tew": the player is being announced |
| `entity.spotted_sandpiper.peet` | Spotted sandpiper | Flush | 100 m | Flicking flight low over the water |
| `entity.red_necked_phalarope.twit` | Phalarope | Feeding | 50 m | Soft twitters on a pond |
| `entity.american_golden_plover.whistle` | Golden-plover | Alarm, nest | 400 m | A sad "queedle"; a nest within 100 m |

| Species | Take with | Yield | Sketch behaviors | Call in | Diet | Eaten by |
| --- | --- | --- | --- | --- | --- | --- |
| Loons | Gill net (2% of net-days), broadhead | 2,900 kcal, 30 feathers, 2 long primaries | Rear tremolo, chick carry, dive | Loon wail | Fish, 200 a day | Eagles (chicks), pike (chicks) |
| Grebes | Blunt, net | 300–700 kcal, 8 feathers | Weed dance, chick carry | None | Fry, insects, own feathers | Mink, pike, eagle |
| Crane | Longbow from a blind at the meadow | 3,500 kcal, 40 feathers, 4 long primaries | Dance leap, bow, stick toss, kettle | Crane rattle | Roots, voles, berries, frogs | Fox (chicks), golden eagle |
| Shorebirds | Blunt arrow, ground snare at a pond edge | 60–200 kcal, 3–6 feathers | Probe, teeter, spin, winnow dive, broken wing | None | Larvae, worms, insects | Merlin, peregrine, fox, short-eared owl |

- **Common and Pacific loons.** Their rules are in the Bestiary and Waters and fish. New: the loon wail call means a player can sit at a lake at dusk and talk with a pair. *Moment:* the player wails once at Big Lake and three loons answer from three bays, then a fourth from beyond the hill.
- **Horned grebe (new).** It dives for fry in small ponds and eats its own feathers to pad its stomach against fish bones. A grebe on a pond means fry and small fish are there, so a minnow trap will fill. *Moment:* a grebe sinks out of sight without a ripple as the player raises the bow.
- **Red-necked grebe (new).** Pairs bray and wail like a donkey in a well, heard 800 m in May. Chicks ride on the parents' backs for 10 days. A pair on a lake with no loons means pike there, because pike take the chicks that fall off. *Moment:* three striped chicks peek from under a parent's wings as it swims past the canoe.
- **Sandhill crane.** Migration and the yield are in the Bestiary. New: landed flocks dance in the A3 meadows on 40% of stopover mornings. Birds leap 2 m, bow, toss sticks and call in unison, 5–30 birds at once, for 5–15 game minutes. Watching a full dance from cover gives Spirits +3 and a sketchable behavior. *Moment:* a crane picks up a stick, throws it in the air, and fifty birds start leaping.
- **Wilson's snipe (new).** At dusk and through the bright June nights, males dive from 60 m and their outer tail feathers hum. The sound means wet, soft ground below: a bog, not a trail. *Moment:* sitting at the fire at midnight in June, the player hears the winnowing circle overhead and never sees the bird.
- **Lesser yellowlegs (new).** In nesting season it perches on muskeg spruce tips and yelps at anything within 150 m. That is a flush chain with a +25 call, so moose at a C4 pond know the player is coming. A player who moves while it is quiet and freezes while it calls lowers the alarm. *Moment:* a moose lifts its head from the pond at 200 m, ears swivelling toward the yelping bird on the spruce above the player.
- **Spotted sandpiper (new).** It teeters on every gravel bar and nests on bars above the waterline. A June flood (Blueprint › Calendar, daylight and weather) drowns nests below the new line, and pairs renest higher. Its presence marks a bar the flood leaves alone that year. *Moment:* a bird runs ahead of the player along the bar, stopping every 10 m to bob, until it flies back past in a low arc.
- **Red-necked phalarope (new).** Females court and leave; males sit the eggs. Phalaropes spin on tundra ponds to lift larvae to the surface, 1 turn a second. Spinning birds on a pond mean a mosquito hatch from that pond within 7 days. *Moment:* five tiny birds whirl on a black pond like tops.
- **American golden-plover (new).** It nests on dry tundra and leads the player away with a broken-wing act. Following the act for 50 m takes the player off the nest; ignoring it and searching the spot finds the 4 eggs in 1–3 game minutes. *Moment:* a gold-and-black bird drags a "broken" wing ahead of the player across the Barrens, then flies off whole.

## Birds of the water's edge

**The fishing birds and the swallows.** Gulls, the tern, kingfisher, dipper and two swallows exist; the bank swallow is new. Each is a fishing or weather sign first (Waters and fish › Who eats fish; Water calls and encounters).

| Species | Registry id | Hitbox | Mass | Plumage | Nest and eggs | Where, dates |
| --- | --- | --- | --- | --- | --- | --- |
| Mew gull | `yearofice:mew_gull` | 0.5 × 0.45 | 0.4 kg | White and grey; brown first-year | Colony on gravel islands and stumps, May 25 – Jun 15; 3 eggs, 55 g | Lakes and the river, May – Sep |
| Arctic tern | `yearofice:arctic_tern` | 0.4 × 0.3 | 0.11 kg | Black cap, red bill | Gravel scrape, colony of 10–40, Jun 5–25; 2 eggs | Lakes, the big river, May 20 – Aug 20 |
| Belted kingfisher | `yearofice:belted_kingfisher` | 0.35 × 0.35 | 0.15 kg | Blue-grey crest; female rusty belt | Burrow 1–2 m into a cutbank, Jun 1–20; 6–7 eggs | Creek spots, May – Sep |
| American dipper | `yearofice:american_dipper` | 0.25 × 0.25 | 0.06 kg | Slate grey; white eyelid flash | Moss ball on a falls ledge, Apr 20 – May 15; 4 eggs | Falls, spring leads, all year |
| Tree swallow | `yearofice:tree_swallow` | 0.2 × 0.2 | 0.02 kg | Iridescent blue-green | Cavity or nest box, Jun 1–15; 5 eggs | Still water, May 15 – Aug 10 |
| Cliff swallow | `yearofice:cliff_swallow` | 0.2 × 0.2 | 0.022 kg | Pale rump, buff forehead | Mud gourd under eaves or a cliff, Jun 5–25; 4 eggs | Cabin eaves from year 2, D3 cliffs |
| Bank swallow (new) | `yearofice:bank_swallow` | 0.18 × 0.18 | 0.014 kg | Brown with a breast band | Burrow colony of 50–300 in a sand or silt cutbank, Jun 1–20; 4–5 eggs | The White Bluff (C3) and fresh cutbanks; May 20 – Aug 5 |

| Sound event | Species | When | Carries | Meaning |
| --- | --- | --- | --- | --- |
| `entity.mew_gull.mew` | Mew gull | Flocks over fish | 1 km | A school or a gut pile (existing rule) |
| `entity.arctic_tern.kek` | Arctic tern | Colony defence | 200 m | A colony; the player's head is about to be struck |
| `entity.american_dipper.song` | Dipper | All year, loudest Jan–Mar | 50 m (existing) | Open water |
| `entity.tree_swallow.chirp` | Tree swallow | Day | 60 m | Insects over still water |
| `entity.bank_swallow.chatter` | Bank swallow | Colony | 150 m | A colony bank: dry, crumbling, unsafe to stand under |

- **Mew gull and Arctic tern.** Fishing rules are in Waters and fish. New: a tern colony on a gravel bar dive-strikes the player's head within 20 m of the nests, 1 Condition per strike, 2 strikes a game minute. Eggs are taken at that cost. Gull eggs (55 g, 80 kcal) are the one egg harvest that does no lasting harm: a gull colony renests 100%. *Moment:* a tern hovers a metre above the player's hat, screaming, while its chick crouches between two stones.
- **Belted kingfisher and dipper.** Existing rules stand. New: the dipper sketch behavior "walking under the water" counts only from November to March, so its level 3 takes a winter. *Moment:* a kingfisher beats a grayling on its branch three times before swallowing it head first. A dipper sings from an ice shelf at −38 °C, the only song in the canyon.
- **Tree and cliff swallows.** Their ambient and eave rules stand. New: both use nest boxes and eaves the player builds (Eggs, down and feathers). *Moment:* the first swallow pair of June inspects the new nest box on the cabin wall for an hour, then flies away; the next day they bring grass.
- **Bank swallow (new).** It digs a colony of 50–300 burrows in the top metre of a vertical cutbank. A colony marks a bank that is eroding now (Water calls and encounters › Rivers move): standing under it risks a slump. When the bank retreats past the colony, the birds move to the next fresh cut, so the colony is a map of where the river is cutting. *Moment:* the river takes a 10 m slab of the White Bluff in July, and the swallows wheel over the gap for an hour.

## Day raptors

**Ten hunters that shape what the player sees.** Four exist; six are new. Each is a predator in the Who hunts whom budget, and most give the player a sign or a free meal.

| Species | Registry id | Hitbox | Mass | Plumage | Nest and eggs | Where, dates |
| --- | --- | --- | --- | --- | --- | --- |
| Bald eagle | `yearofice:bald_eagle` | 0.8 × 0.9 | 4.5 kg | White head from year 5; brown juveniles | Stick nest 2 m wide in a riverside cottonwood or spruce, Apr 15 – May 5; 2 eggs | River and lake, Apr – Nov |
| Golden eagle | `yearofice:golden_eagle` | 0.8 × 0.9 | 4.5 kg | Golden nape | Cliff ledge nest, Apr 1–20; 2 eggs | A1, A2, D1, Mar – Oct |
| Osprey | `yearofice:osprey` | 0.7 × 0.6 | 1.6 kg | White below, dark eye stripe | Stick nest on a dead snag top over water, May 10–25; 3 eggs | Lakes with fish, May – Sep |
| Gyrfalcon | `yearofice:gyrfalcon` | 0.6 × 0.6 | 1.4 kg | Grey phase; white phase 5% | Not in the valley | A2, D1, Oct – Apr |
| Northern goshawk (new) | `yearofice:northern_goshawk` | 0.6 × 0.6 | 1.0 kg | Slate grey, white eyebrow; brown juveniles | Stick nest in a big birch or spruce, Apr 15 – May 10; 3 eggs | Mixed forest, B2, core, all year; 1–3 pairs |
| Sharp-shinned hawk (new) | `yearofice:sharp_shinned_hawk` | 0.35 × 0.35 | 0.15 kg | Blue-grey, barred rusty breast | Spruce nest, May 20 – Jun 10; 4–5 eggs | Forest edges, May – Sep |
| Rough-legged hawk (new) | `yearofice:rough_legged_hawk` | 0.65 × 0.6 | 1.0 kg | Pale head, black belly band, feathered legs | Cliff ledges in A1 and D1 in vole years only, May; 3–5 eggs | Passage Apr 15 – May 10, Sep 1 – Oct 15 |
| Northern harrier (new) | `yearofice:northern_harrier` | 0.55 × 0.5 | 0.45 kg | Grey male, brown female, white rump, owl-like face | Ground nest in muskeg sedge, May 20 – Jun 10; 4–5 eggs | C4, A3, May 1 – Sep 15 |
| Merlin (new) | `yearofice:merlin` | 0.35 × 0.3 | 0.2 kg | Dark, streaked; male blue-grey | Old magpie or raven nest, May 15 – Jun 5; 4 eggs | Edges, the core, Apr 20 – Sep 20 |
| Peregrine falcon (new) | `yearofice:peregrine_falcon` | 0.5 × 0.5 | 0.8 kg | Slate back, black "helmet" | Cliff scrape: Long Falls canyon (D3) or the White Bluff (C3), May 10 – 30; 3–4 eggs | 1–2 pairs, Apr 25 – Sep 25 |

| Sound event | Species | When | Carries | Meaning |
| --- | --- | --- | --- | --- |
| `entity.osprey.whistle` | Osprey | Over the nest | 300 m | A nest on the snag; fish in the lake |
| `entity.northern_goshawk.kak` | Goshawk | Nest defence, Apr – Jul | 300 m | A rapid "kak-kak-kak": a strike is coming |
| `entity.sharp_shinned_hawk.kik` | Sharp-shin | Alarm | 100 m | A small hawk near the feeder |
| `entity.rough_legged_hawk.scream` | Rough-legged hawk | Passage | 400 m | Hawks moving south: snow within 10 days, 60% |
| `entity.northern_harrier.chatter` | Harrier | Sky dance, May | 300 m | A nest in the muskeg |
| `entity.merlin.ki` | Merlin | Chases | 300 m | A fast falcon after songbirds |
| `entity.peregrine_falcon.wail` | Peregrine | Cliff, nest season | 600 m | "Kak-kak-kak" from the canyon wall |
| `entity.raptor.stoop_rush` | Falcons, eagles | Dive within 30 m | 30 m | Wind tearing; a strike |

| Species | Take with | Yield | Sketch behaviors | Diet | Takes from the player |
| --- | --- | --- | --- | --- | --- |
| Goshawk | Broadhead at its nest (defends) | 900 kcal, 12 feathers | Dash, mantle, nest strike | Grouse, hares, squirrels, ducks | Hens, a hare in a snare |
| Sharp-shin | Blunt | 120 kcal | Feeder ambush | Songbirds | Feeder birds |
| Rough-legged | Broadhead | 900 kcal | Hover, perch on the Sitting Stone | Voles, lemmings | Nothing |
| Harrier | Broadhead | 350 kcal | Quartering, sky dance, food pass | Voles, ducklings, songbirds | Nothing |
| Merlin | Blunt | 150 kcal | Chase, raven harassment | Waxwings, redpolls, dragonflies | Nothing |
| Peregrine | Broadhead | 600 kcal, 12 feathers | Stoop, bind, cliff display | Ducks, shorebirds | Ducks the player wounded |

- **Bald eagle, golden eagle, osprey, gyrfalcon.** Their rules are in the Bestiary and Water calls and encounters. New: eagle and osprey nests reuse their tree every year and grow 0.3 m a year, so an old nest is a landmark for decades. *Moment:* an osprey hovers, folds and hits the water 40 m from the canoe, rising with a whitefish, and an eagle drops off a snag after it. On the Barrens a golden eagle drops on a marmot 50 m from the player, and the whole scree whistles. In January a gyrfalcon sits on the Sitting Stone, and the willow flat below goes silent.
- **Northern goshawk (new).** The forest's top bird hunter follows the grouse and hare cycle: pairs nest only in years with grouse K above 60%. Within 50 m of its nest from April to July it strikes the player's head from behind. Each strike, 20% a game minute, costs 3 Condition and knocks the hat off. *Food web:* grouse fall 20% in its territory. *Moment:* a grey shape crosses the trail at knee height, and a spruce grouse that was 10 m ahead is gone in a puff of feathers.
- **Sharp-shinned hawk (new).** It ambushes the suet feeder: once every 3–6 days when a flock of 6 or more feeds. The flock freezes for 10 game minutes, then scatters for 1 game hour, and a puff of feathers marks a kill. *Moment:* every chickadee at the feeder goes still at once, and a small hawk lands on the woodpile.
- **Rough-legged hawk (new).** It passes through in spring and fall and hovers over meadows hunting voles. Hovering hawks count as a vole sign: 3 or more over A3 in a day mean V of 1.3 or more. Fall passage leads snow by 3–10 days 60% of the time. *Moment:* a hawk hangs motionless over the meadow in a 10 m/s wind, its feathered legs dangling.
- **Northern harrier (new).** It quarters low over muskeg and meadows with a white rump flashing. Males sky-dance in May, a looping rollercoaster 30–100 m up. The male passes food to the female in midair over the nest, which shows the nest to a watching player. *Moment:* a harrier drops a vole, and the female flips upside down to catch it.
- **Merlin (new).** A fierce small falcon that chases redpolls, waxwings and dragonflies, and harasses ravens and eagles near its nest. It nests in old magpie or raven nests, so it follows the magpies the homestead draws. *Moment:* a merlin chases a waxwing flock in a twisting ball over the cabin, and one waxwing drops into the snow at the player's feet, alive and stunned.
- **Peregrine falcon (new).** Pairs nest on the canyon walls and the White Bluff. It stoops on ducks at up to 90 m/s, seen as a dark dart and heard as a rush. When it hits a duck over water within 50 m of the player, it abandons the kill 40% of the time, and a duck falls from the sky. *Moment:* the player sets decoys, a teal flock swings in, and a falcon takes the lead bird a metre above the decoys.

## Owls

**Seven owls, three existing and four new, split the night and the vole years between them.** Owl flight feathers make silent arrows (Bestiary › Great horned owl), and every owl is a mobbing target by day.

| Species | Registry id | Hitbox | Mass | Plumage | Nest and eggs | Where, dates |
| --- | --- | --- | --- | --- | --- | --- |
| Great horned owl | `yearofice:great_horned_owl` | 0.5 × 0.6 | 1.5 kg | Ear tufts; pale subarctic form | Old raven or hawk nest, Feb 20 – Mar 20; 2–3 eggs | Forest edges, all year |
| Great gray owl | `yearofice:great_gray_owl` | 0.55 × 0.7 | 1.1 kg | Grey face disc, yellow eyes | Broken spruce top, Apr 1–20, only at V ≥ 0.9; 3 eggs | C4 muskeg, all year |
| Northern hawk owl | `yearofice:hawk_owl` | 0.4 × 0.45 | 0.3 kg | Long tail, barred | Burned snag cavity, Apr 10–30; 5–8 eggs | D2 burn, all year |
| Boreal owl (new) | `yearofice:boreal_owl` | 0.3 × 0.3 | 0.13 kg | Brown with a black-framed white face | Old flicker hole or nest box, Apr 1–25; 3–6 eggs | Old spruce and aspen, core, B2; all year |
| Northern saw-whet owl (new) | `yearofice:saw_whet_owl` | 0.25 × 0.25 | 0.08 kg | Streaked rusty | Cavity, May; 4–6 eggs | A rare stray: present 1 year in 4, 1–2 birds |
| Short-eared owl (new) | `yearofice:short_eared_owl` | 0.4 × 0.4 | 0.35 kg | Tawny, black wrists | Ground scrape in sedge, May 15 – Jun 5; 5–9 eggs in vole years | A3, C4, A2 meadows; May – Sep, and only at V ≥ 1.0 |
| Snowy owl (new) | `yearofice:snowy_owl` | 0.6 × 0.6 | 2.0 kg | White male, barred female | Not in the valley | A2 and D1 tundra, Nov – Mar, in 1 year in 4 |

| Sound event | Species | When | Carries | Meaning |
| --- | --- | --- | --- | --- |
| `entity.boreal_owl.staccato` | Boreal owl | Nights, Feb – Apr | 1 km | A trill of 10–15 notes, like a snipe on the ground; a cavity nearby |
| `entity.saw_whet_owl.toot` | Saw-whet | Nights, Apr – May | 300 m | A rare bird; journal line and Spirits +3 on first hearing |
| `entity.short_eared_owl.bark` | Short-eared owl | Nest alarm | 200 m | A nest in the meadow |
| `entity.short_eared_owl.wing_clap` | Short-eared owl | Display, May | 150 m | Wing claps under a hovering bird |
| `entity.snowy_owl.hoot` | Snowy owl | Rare | 3 km on still tundra | A lemming year on the Barrens |
| `ambient.mobbing` | Songbirds at an owl | Day | 150 m | An owl or hawk sitting within 3 m of the noise |

| Species | Take with | Yield | Sketch behaviors | Diet | Moment |
| --- | --- | --- | --- | --- | --- |
| Boreal owl | Blunt | 20 owl feathers | Cavity peek, prey cache, staccato song | Red-backed voles, redpolls | In March at −25 °C, the player's owl trill gets an answer from the aspen at the woodshed |
| Saw-whet | Not taken by the perch snare | 10 owl feathers | Roosting at eye level | Voles, mice | A tiny owl sits on a spruce bough at face height and does not move as the player walks past |
| Short-eared owl | Broadhead | 20 owl feathers | Hover, wing clap, ground-nest flush | Voles | At dusk over the A3 meadow, four owls bounce over the grass like big moths |
| Snowy owl | Longbow | 1,500 kcal, 30 owl feathers, decoration value 3 | Perch on the Sitting Stone, pounce | Voles, ptarmigan | A white shape on the Sitting Stone turns its head as the sled passes 300 m off |

- **Great horned, great gray and hawk owls.** Their rules stand. New: a great horned owl takes 1 hen a week from an open run at night and the hen house door rule stops it. A great gray's plunge holes are now also found by the tamed wolf, which noses them (it learns from the owl). *Moment:* a hawk owl drops from a spruce tip to the snow 10 m from the player's axe, and flies back up with the vole the chopping flushed.
- **Boreal owl (new).** A small owl of old spruce that roosts within 2 m of the trunk by day, the commonest mobbing target. It nests in old flicker holes, so it lives where flickers have been. Its staccato song in February to April is the late-winter night sound.
- **Northern saw-whet owl (new).** A rare stray at the edge of its range. It counts as a rare sighting: Spirits +3 on the first one, and the radio gets a field note worth 2 points.
- **Short-eared owl (new).** A day-flying owl of meadows in vole years, the vole crash's counterpart to the great gray's irruption. Its ground nest is found by its barking alarm. Its 25 g eggs are never worth taking, and taking them costs next year's vole control in the garden.
- **Snowy owl (new).** It arrives on the Barrens after a lemming peak elsewhere and sits on high points in daylight. It sees 600 m and leaves at 200 m. It is a trophy and a sight, not a food plan.

## Woodpeckers and corvids

**Three woodpeckers make the holes that half the forest's birds nest in; three corvids run the camp.** The flicker and the magpie are new.

| Species | Registry id | Hitbox | Mass | Plumage | Nest and eggs | Where, dates |
| --- | --- | --- | --- | --- | --- | --- |
| Three-toed woodpecker | `yearofice:three_toed_woodpecker` | 0.25 × 0.3 | 0.06 kg | Barred back; male yellow cap | Excavates a new hole each May; 4 eggs | Beetle spruce, burns, all year |
| Black-backed woodpecker | `yearofice:black_backed_woodpecker` | 0.25 × 0.3 | 0.07 kg | Black back; male yellow cap | New hole in a burned snag, May; 4 eggs | Burns ×3 for 3 years |
| Northern flicker (new) | `yearofice:northern_flicker` | 0.35 × 0.35 | 0.14 kg | Yellow-shafted wings and tail, black bib | Excavates in aspen or balsam poplar, May 15 – Jun 5; 6–8 eggs | Edges, B2, the core; May 1 – Sep 20 |
| Raven | `yearofice:raven` | 0.5 × 0.5 | 1.2 kg | Glossy black; throat hackles | Cliff ledge or tall spruce, Mar 15 – Apr 15; 4–6 eggs | Everywhere, all year |
| Gray jay | `yearofice:gray_jay` | 0.3 × 0.3 | 0.07 kg | Grey, white face; dark grey juveniles | Spruce nest in March at −30 °C; 3 eggs | Spruce taiga, all year |
| Black-billed magpie (new) | `yearofice:black_billed_magpie` | 0.35 × 0.45 | 0.18 kg | Black and white, 25 cm tail | Domed stick nest 1 m tall in willow or spruce, Apr 15 – May 10; 6 eggs | A3 and C3 at start, 0–4; spreads to the homestead |

| Sound event | Species | When | Carries | Meaning |
| --- | --- | --- | --- | --- |
| `entity.northern_flicker.wicka` | Flicker | Spring | 400 m | "Wick-wick-wick": a pair claiming an aspen |
| `entity.northern_flicker.drum_metal` | Flicker | Dawn, May | 600 m | Drumming on the station chimney cap or the radio mast |
| `entity.raven.knock` | Raven | Pair talk | 300 m | Ravens nesting on that cliff |
| `entity.raven.yell` | Raven | Juveniles at a carcass, winter | 1 km | A carcass and young ravens calling others in |
| `entity.black_billed_magpie.chatter` | Magpie | Alarm, carrion | 300 m | A predator, or meat on the ground |

- **Woodpeckers.** The flaked-bark rule is in the Bestiary. New: each woodpecker hole sets the `cavity` block state on its log. Old cavities house the boreal owl, goldeneye, bufflehead, merganser, tree swallow and flying squirrel. Felling a cavity tree removes a nest site for 5 years. *Moment:* the player fells a dead aspen, and a flying squirrel and two boreal chickadees burst from three different holes.
- **Northern flicker (new).** A ground-feeding woodpecker that eats ants, so it hops on anthills and the player's path. In May it drums on metal: at dawn on the station's stovepipe cap, heard inside as a machine-gun rattle. Its yellow-shafted feathers join the collection (Eggs, down and feathers). *Moment:* 05:10 in May, and the station roof erupts in drumming; the player goes out to find a flicker on the chimney, shouting.
- **Raven.** The sign rules, the camp raven and raven leads stand (Blueprint › Ravens tell the truth; Bestiary; Water calls and encounters). New: ravens play. They roll on fresh snow, slide down the station roof, and drop and catch sticks in ridge wind. Play is a sketch behavior and gives Spirits +1 the first time each winter. *Moment:* two ravens fly upside down past the cabin on a windy March day, calling.
- **Gray jay.** The theft, warnings and hand-feeding stand. New: juveniles in June are dark grey, and a family of 4–5 at camp is the first sign that the jays bred.
- **Black-billed magpie (new).** Magpies follow people. Each year of a homestead with a midden or a meat pole, a pair settles within 1 km at 40%, up to 4 pairs. They steal (Birds that steal from camp), chatter at predators like jays, and their old nests house merlins. *Moment:* the third spring at Station Nine, a magpie lands on the meat pole, and the jays scream at it.

## Songbirds

**Twenty small birds fill the chorus, the feeder and the berry bushes.** Four exist; sixteen are new. Most are taken only in a hard winter, but every one is a voice, a sign or a sketch. All use the `songbird` rig, scaled to the hitbox.

| Species | Registry id | Hitbox | Mass | Plumage | Nest and eggs | Where, dates |
| --- | --- | --- | --- | --- | --- | --- |
| Black-capped chickadee | `yearofice:black_capped_chickadee` | 0.15 × 0.15 | 11 g | Black cap and bib | Soft wood cavity, May 15 – Jun 5; 6–8 eggs | Birch, all year |
| Boreal chickadee | `yearofice:boreal_chickadee` | 0.15 × 0.15 | 10 g | Brown cap | Cavity in black spruce; 6 eggs | Black spruce, all year |
| Common redpoll | `yearofice:common_redpoll` | 0.15 × 0.15 | 13 g | Red cap, black chin | Shrub cup, May 15 – Jun 10; 5 eggs | Oct – Apr flocks; some breed in willows |
| Snow bunting | `yearofice:snow_bunting` | 0.18 × 0.18 | 40 g | White with black wing tips | Breeds north of the map | Passage Mar 10 – May 5, Oct 1 – Nov 1 |
| Bohemian waxwing (new) | `yearofice:bohemian_waxwing` | 0.2 × 0.2 | 55 g | Crest, red wax wing tips, yellow tail band | Spruce cup, Jun 10–30; 5 eggs | Flocks of 20–300, Sep 15 – Dec 15; pairs in summer |
| Pine grosbeak (new) | `yearofice:pine_grosbeak` | 0.22 × 0.22 | 56 g | Male rose-red, female olive-gold | Spruce cup, Jun 1–20; 4 eggs | Taiga all year; 4–20 in winter flocks |
| White-winged crossbill (new) | `yearofice:white_winged_crossbill` | 0.18 × 0.18 | 26 g | Crossed bill; male pink, white wing bars | Spruce cup in winter, Jan – Mar, in cone years only; 3 eggs | Nomadic; 0 or 20–200 per region by cone crop |
| Varied thrush (new) | `yearofice:varied_thrush` | 0.22 × 0.22 | 78 g | Orange and slate, black breast band | Spruce cup near water, Jun 1–20; 3–4 eggs | Wet old spruce, D3, creeks; Apr 25 – Oct 10 |
| Hermit thrush (new) | `yearofice:hermit_thrush` | 0.2 × 0.2 | 30 g | Brown, rusty tail | Ground cup, Jun 5–25; 4 eggs | Dry spruce and birch, B2; May 10 – Sep 15 |
| Swainson's thrush (new) | `yearofice:swainsons_thrush` | 0.2 × 0.2 | 30 g | Olive, buff eye-ring | Shrub cup, Jun 5–25; 4 eggs | Riparian willow and birch; May 20 – Aug 31 |
| American robin (new) | `yearofice:american_robin` | 0.22 × 0.22 | 77 g | Brick-red breast | Mud-lined cup on a branch or the cabin's eave beam, May 15 – Jun 10; 4 blue eggs | Everywhere open; Apr 25 – Sep 30 |
| Yellow-rumped warbler (new) | `yearofice:yellow_rumped_warbler` | 0.15 × 0.15 | 12 g | Yellow rump and side patches | Spruce cup, Jun 1–20; 4–5 eggs | Spruce, May 1 – Sep 20 |
| Ruby-crowned kinglet (new) | `yearofice:ruby_crowned_kinglet` | 0.12 × 0.12 | 6 g | Olive; male hidden red crest | Hanging moss cup high in spruce, Jun 1–20; 7–9 eggs | Spruce, May 1 – Sep 25 |
| Dark-eyed junco (new) | `yearofice:dark_eyed_junco` | 0.16 × 0.16 | 19 g | Slate grey, white outer tail | Ground cup under a root plate or bank, May 20 – Jun 15; 4 eggs | Everywhere; Apr 20 – Oct 1 |
| Fox sparrow (new) | `yearofice:fox_sparrow` | 0.18 × 0.18 | 32 g | Rusty, heavily spotted | Ground or low willow cup, Jun 1–20; 4 eggs | Willow and alder thickets; May 1 – Sep 20 |
| White-crowned sparrow (new) | `yearofice:white_crowned_sparrow` | 0.17 × 0.17 | 28 g | Black-and-white crown stripes | Ground cup in willow, Jun 1–20; 4 eggs | Willow edges, the clearing; May 5 – Sep 15 |
| Lapland longspur (new) | `yearofice:lapland_longspur` | 0.17 × 0.17 | 27 g | Male black face, chestnut nape | Tundra cup, Jun 1–20; 5 eggs | A2, D1; passage with buntings Apr 20 – May 15, Aug 20 – Sep 30 |
| Horned lark (new) | `yearofice:horned_lark` | 0.17 × 0.17 | 32 g | Black "horns" and mask | Bare-ground scrape on wind-scoured ridges, May 15 – Jun 5; 4 eggs | A2, D1, the Esker; Apr 15 – Sep 30 |
| Rusty blackbird (new) | `yearofice:rusty_blackbird` | 0.22 × 0.22 | 60 g | Male black, rusty in fall | Cup over water in willow or spruce, May 20 – Jun 10; 4–5 eggs | Beaver ponds and muskeg, B4, C4; Apr 25 – Sep 30 |
| Northern shrike (new) | `yearofice:northern_shrike` | 0.22 × 0.22 | 65 g | Grey, black mask, hooked bill | Breeds north of the map | Oct 1 – Apr 15, 1–2 per region |

| Sound event | Species | When | Carries | Meaning |
| --- | --- | --- | --- | --- |
| `entity.bohemian_waxwing.trill` | Waxwing | Flocks | 200 m | A buzzing trill: berries being stripped |
| `entity.pine_grosbeak.whistle` | Pine grosbeak | Winter flocks | 150 m | Soft whistles; tame birds on the buds |
| `entity.white_winged_crossbill.song` | Crossbill | Jan – Mar | 300 m | A cone year: squirrels will boom |
| `entity.varied_thrush.buzz` | Varied thrush | Dawn, dusk, fog | 500 m | One long buzzing note; in midday it means fog or rain coming |
| `entity.hermit_thrush.flute` | Hermit thrush | Dusk | 400 m | A clear flute phrase, then a higher one |
| `entity.swainsons_thrush.spiral` | Swainson's thrush | Dusk and dawn | 400 m | An upward-spiralling phrase |
| `entity.american_robin.song` | Robin | First light | 300 m | "Cheerily-cheer-up"; the chorus is starting |
| `entity.american_robin.tut` | Robin | Alarm | 100 m | A mob or a predator on the ground |
| `entity.yellow_rumped_warbler.trill` | Warbler | May – Jul | 150 m | Spruce full of insects |
| `entity.ruby_crowned_kinglet.song` | Kinglet | May – Jun | 300 m | Loud from a 6 g bird; mobbing joins |
| `entity.dark_eyed_junco.trill` | Junco | Apr – Jul | 200 m | A dry trill from a spruce tip |
| `entity.dark_eyed_junco.smack` | Junco | Flush | 30 m | White tail flashes |
| `entity.fox_sparrow.song` | Fox sparrow | May – Jun | 300 m | A rich song from willow |
| `entity.white_crowned_sparrow.song` | White-crowned sparrow | All night May 20 – Jul 10 | 300 m | "The June alarm clock" |
| `entity.lapland_longspur.song_flight` | Longspur | Tundra, Jun | 300 m | A tinkling song as it parachutes down |
| `entity.horned_lark.tinkle` | Horned lark | Ridges, Apr – Jun | 200 m | Bare ground ahead |
| `entity.rusty_blackbird.creak` | Rusty blackbird | Apr – Jun | 300 m | A creaking hinge: a beaver pond |
| `entity.northern_shrike.chatter` | Shrike | Winter | 150 m | A shrike's larder is near |

| Species | Take with | Yield | Sketch behaviors | Diet | Eaten by |
| --- | --- | --- | --- | --- | --- |
| Chickadees, kinglet, warbler | Not worth taking (under 15 g) | — | Hang, cache, glean, mob | Insects, seeds, suet | Shrike, sharp-shin, boreal owl |
| Redpoll, junco, sparrows, longspur, lark | Perch snare, blunt | 20–40 kcal, 2 feathers | Flock lift, scratch, song flight | Seeds, insects | Merlin, shrike, harrier |
| Waxwing, robin, thrushes | Perch snare at berries, blunt | 50–90 kcal, 3 feathers | Berry toss, sally, dusk song | Berries, insects, worms | Merlin, sharp-shin, goshawk |
| Pine grosbeak | Blunt, perch snare | 70 kcal, 4 feathers | Bud feed, ash pecking | Buds, seeds, berries | Goshawk, shrike |
| Crossbill | Perch snare at salt or ash | 30 kcal | Cone pry, winter nest | Spruce seed | Merlin, marten |
| Rusty blackbird | Blunt | 70 kcal | Wade-feeding | Aquatic insects, small fish | Mink, merlin |
| Shrike | Blunt | 80 kcal | Impale, hover | Voles, redpolls | Goshawk |

**Sheets in brief.** Each new songbird has one rule that makes it matter, and one moment.

- **Chickadees, redpoll, snow bunting.** Existing rules stand: dee counts, the feeder, catkin crops, the spring clock. *Moment:* at −42 °C a chickadee lands on the player's mitten at the feeder, and its feet are warm. A redpoll flock dives into the snow to roost at dusk, and the bank goes still.
- **Bohemian waxwing (new).** Flocks arrive in mid-September and strip berry patches: highbush cranberry, rose hips, juniper and the rare mountain ash on B2. Flocks strip an unpicked patch at the Plants and fungi rate, 5% a day after its peak, so a patch left a month is three-quarters gone; the seeds they drop under perches are that tab's seed rain (Plants and fungi › Animals and plants). After frost, fermented berries make them drunk: flight errors ×5 for 1 game hour. *Moment:* a waxwing flies into the cabin window and lies in the snow; warmed in the hands for 2 game minutes, it flies off (Spirits +3).
- **Pine grosbeak (new).** The tamest bird in the valley: flush distance 2 m, 1 m after habituation. It pecks ash at the fire ring for salt and grit. It is the one songbird worth a blunt arrow in February, and the hand-feeding rule works for it like the jay's. *Moment:* a red male feeds on willow buds a metre from the player's face, ignoring the bow.
- **White-winged crossbill (new).** Nomads that come only in spruce cone years (1 year in 3, shown by heavy cones in August), and then nest in January at −30 °C. A crossbill winter means red squirrel K +30% the next year, and marten +20% after that. *Moment:* in a February cold snap, the spruces sing.
- **Varied thrush (new).** A single long buzzing note from wet old spruce. Singing at midday instead of dawn and dusk predicts fog or rain within 12 game hours, right 65% of the time. *Moment:* the canyon is full of fog, and one note hangs in it, then another from far away.
- **Hermit and Swainson's thrushes (new).** The dusk songs of dry and wet woods, the sound of summer evenings. Hermit thrush means dry spruce-birch benches (good firewood, few bugs); Swainson's means riparian willow (moose, bugs). Both give Spirits +1 on a dusk with 3 or more heard, once a day.
- **American robin (new).** The first robin (around April 25) gives Spirits +2 and the messenger line "A robin on the bare ground." Robins pull worms from the garden bed and the wet trail. They nest on the cabin's eave beam if one is free. *Moment:* a robin pair builds on the woodshed lintel, and four blue eggs sit at eye level for two weeks.
- **Yellow-rumped warbler (new).** The first warbler back, eating spruce budworm and aphids. Warblers in the spruce near the garden cut insect damage on cabbage by 20% (Blueprint › Crop farming). *Moment:* a fallout day after a May snowstorm puts 30 warblers in one willow by the station.
- **Ruby-crowned kinglet (new).** A 6 g bird with a song heard 300 m. It is the loudest member of a mob, so kinglet chatter in a spruce at midday often means an owl. *Moment:* the male flashes his hidden red crest at the player's pishing.
- **Dark-eyed junco (new).** It nests on the ground in the hollow of a root plate or a bank. Its white outer tail flashes on every flush, the commonest "something moved" in the forest. Juncos and sparrows take shallow-sown seed (the existing ×0.7 "birds take it" rule now has faces). *Moment:* the player lifts a fallen root plate and finds 4 speckled eggs in a grass cup.
- **Fox sparrow (new).** It scratches leaf litter with both feet. Fox sparrows scratching in a spot mean the litter is thawed 2 cm down there, so frozen wood frogs are waking (Bestiary › Wood frog). *Moment:* a rustle like a large animal in dry leaves turns out to be one sparrow.
- **White-crowned sparrow (new).** It sings all through the bright nights of June. A player sleeping in a tent or at a fire within 30 m of a singing male wakes at 02:00–04:00 on 20% of nights. After 5 nights in a row they are used to it. *Moment:* "Woke at 03:10: a sparrow singing." The messenger records it like a cold wake (Blueprint › Sleep).
- **Lapland longspur and horned lark (new).** Tundra birds of the Barrens and the ridges. Horned larks sing over bare, wind-scoured ground from mid-April and nest there first, so a singing lark means a snow-free ridge: the fastest walking line in spring. Longspurs pass in flocks with snow buntings. *Moment:* a longspur rises from the tundra singing, then parachutes down on spread wings.
- **Rusty blackbird (new).** A declining bird of beaver ponds and muskeg. It needs shallow water with willows; when a beaver pond drains to meadow (Water calls and encounters › Beavers, ponds and thaw), its pair leaves. Its creaking song marks a living beaver pond from 300 m. *Moment:* the dam fails in year 4, and the next spring the creaking is gone.
- **Northern shrike (new).** A winter predator of songbirds and voles that impales its prey on spruce twigs and rose thorns. A shrike larder of 2–6 frozen voles and redpolls is a findable sign: the vole index reads from it like an owl pellet. It also takes small catches from mouse traps at the cache, 1 a week. *Moment:* the player finds three frozen voles spiked on a rose bush in a row, like a shop window.

## Flying insects

**Ten new insects, each a fishing clue, a pest, a thief or a small wonder.** The mosquito, black fly, no-see-um, bumblebee, bark beetle, dragonfly and the caddis and stonefly hatches stay as written. Swarms are client particles driven by server data, as the Bestiary's black flies are; only the yellowjacket nest is a block.

| Insect | Registry id | Form | Window | Where | What it does |
| --- | --- | --- | --- | --- | --- |
| Mayfly | `yearofice:mayfly_hatch` | Particle hatch and spinner fall | Jun 10 – Jul 10, calm evenings above 12 °C, 20:00–23:00 | Lakes, oxbows, slow river reaches | Grayling and whitefish rise ×1.7 for 45 game minutes; terns, gulls and swallows feed over it |
| Lantern moths | `yearofice:moth_swarm` | Particles at light | Jul 25 – Sep 15, nights above 5 °C under 2 m/s wind | Any lit lantern, fat lamp or fire | 5–40 moths in 10 blocks; bats hunt them; moths are grayling bait ×1.2 |
| Deer fly | `yearofice:deer_fly` | Circling particle with a voice | Jun 25 – Aug 5, sun, above 18 °C, 10:00–17:00 | Forest edges, trails | Circles the head; bites the neck. Adds to bug pressure |
| Horse fly | `yearofice:horse_fly` | Circling particle with a voice | Jul 1 – Aug 10, same weather | Meadows, A3, shores | Bites arms and legs; the head net does nothing |
| Caribou warble fly | `yearofice:warble_fly` | Ambient over herds | Jul 1 – Aug 15, above 13 °C, under 6 m/s wind | A2 caribou range | Herds run into the wind and bunch on snow patches; holes in hides taken June 1 – August 14 (Bestiary › Caribou hide by date, S13) |
| Caribou nose bot fly | `yearofice:nose_bot_fly` | Ambient over herds | Same | Same | Caribou snort, shake and hold muzzles low; a herd stands nose-down on gravel |
| Mourning cloak | `yearofice:mourning_cloak` | Small entity | First sunny day at 8 °C or more after Apr 1; adults overwinter | Woodpiles, loose bark | The first butterfly of the year, often over snow |
| Snow fly | `yearofice:snow_fly` | Small entity, wingless | Nov – Mar, snow surface at −6 to 0 °C | Forest snow | An insect walking on snow; a curiosity |
| Aerial yellowjacket | `yearofice:yellowjacket_nest` block (paper variant) and the `yearofice:yellowjacket_swarm` particle; the stinging nest guards are the Land tab's `yearofice:yellowjacket` entity (Conflicting ids row 11) | Paper nest the size of a football, in willow, spruce or under eaves | Nests May – Sep; rack raids Jul 15 – Sep 15 (Land animals in depth › Yellowjackets) | Paper nests in willow, spruce or eaves: up to half of the Land tab's 0–4 nests per km² | Takes fish and meat from racks; stings |
| Blow fly | `yearofice:blow_fly_swarm` | Particles over meat | Above 8 °C, May – Sep | Any exposed meat, gut pile or carcass | The face of fly-blow in Blueprint › Spoilage model; its buzz finds lost carcasses |

### Hatches and the fly box

**Mayflies make the lake fishing that caddis make on the creeks.** On a hatch evening, duns rise off the water as pale particles, and 1 game hour later spinners fall flat on it. Rise rings (Blueprint › Reading the water) triple during the fall.

- **Matching the hatch.** A fly tied with pale duck flank feathers (any duck, 3 game feathers, the existing fly recipe) counts ×1.3 on top of the hatch bonus during a mayfly hatch. A dark grouse-hackle fly counts ×1.3 in the caddis hatch instead.
- **Birds over the hatch.** Terns, gulls and swallows dipping over a calm bay at dusk name the hatch from 300 m before the rises show. The messenger, once learned: "Swallows working the bay. A hatch is coming off."

**Moths come with the dark.** The white nights end around July 28; from then a lantern at the door draws moths, then little brown bats from the station roost (Bestiary › Little brown bat). It is the first sign of fall the player can see at night.

- **Clothes moths.** A cousin indoors: wool and fur stored in a room above 10 °C from June to August lose 2% durability a month. A sealed chest, or a bundle of dried Labrador tea in it, stops it. This is why the crate's wool blanket is moth-eaten (Items tab).

### Biting flies

**Deer and horse flies are the hot-day pest.** They join the bug pressure sum from the Bestiary as one more term.

```latex
B_{total} = B_{mosquito} + B_{fly} + B_{midge} + B_{tabanid}
```

| Pest | Wind cut-off | What stops it | Extra effect |
| --- | --- | --- | --- |
| Deer fly | 5 m/s | Head net 80%; smoke ×0.5 | 1–3 circling the head; −1 Spirits per 2 hours exposed |
| Horse fly | 6 m/s | Outer layers on arms and legs; smoke ×0.5; water | A bite is −0.5 Condition; the buzz is loud and directional |

**The swat.** A circling fly can be swatted with the empty hand (left-click when its buzz is in front): 60% kill, 90% with Practice. Each kill clears that fly for 10 game minutes. Dragonflies take one tabanid every 2 game minutes within 20 m of a pond.

**Animals feel them too.** On tabanid days moose stand in ponds at twice the existing rate (Blueprint › Drives and daily schedules), and the tamed wolf snaps at them and lies in shade. A wolf team is not worked on a tabanid afternoon.

### Flies on the caribou

**Warble and bot flies drive the summer herd.** On fly days a caribou herd on A2 runs into the wind for 200–800 m, then bunches on the nearest snow patch or wind-bared ridge. A herd standing on a snow patch is a fixed target for 1–3 game hours, like a moose in a pond.

- **Hides.** Warble larvae grow under the skin over winter and cut out in June. The hide rule is the Bestiary's (Caribou hide by date, settled as S13): a hide taken June 1 – August 14 carries 30–60 holes and is graded −40%; one taken August 15 – September 30 is clean, the parka window.
- **Tell.** A herd running with heads low and no wolves in sight; caribou snorting and stamping on gravel. The messenger: "The herd is running from flies. They will stop on the snow."

### Small wonders

- **Mourning cloak.** It overwinters as an adult in woodpiles and bark. Splitting firewood from November to March finds one per 200 splits, frozen, as the item `yearofice:dormant_butterfly` (Land animals in depth adds its keeping rule); it thaws by the fire in 30 game minutes like the wood frog. The first one flying each spring, often over snow, gives Spirits +2 and "A butterfly over the snow."
- **Snow fly.** A wingless crane fly that walks on the snow surface between −6 and 0 °C. It is a study curiosity with one behavior and a sketch. The first each winter gives Spirits +1 and the journal line "An insect walking on the snow at −4 °C."

### Yellowjackets and blow flies

**Yellowjackets are August's camp thieves.** The raid numbers are the Land tab's (Land animals in depth › Yellowjackets): from July 15 to September 15 a drying rack within 300 m of a nest draws 5–30 foragers, loses 0.05 kg a day and costs 1 sting per 30 game minutes of work there (1 Condition, the bee swelling rule). This tab adds the aerial paper nest and the foraging particle `yearofice:yellowjacket_swarm`.

1. **Find the nest.** Follow foragers back: they fly a straight line, and the paper nest shows within 30 m.
2. **Deal with it.** Douse or burn it at night, when foragers are home (no stings), or knock it down by day (2–6 stings).
3. **Use it.** The comb holds 30–80 grubs, grayling bait ×1.6 for 3 game days. Black bears tear nests open in September, and the shredded paper is sign.
4. **Avoid it.** The smudge-roofed fish rack (Waters and fish) keeps yellowjackets off at ×0.2.

**Blow flies find what the player lost.** A wounded animal that died out of sight raises a buzzing column of flies above 8 °C within 6 game hours. The buzz carries 10 m and the column shows from 20 m in sun, so a crouch-watch in the right willow patch names it: "Flies over the willows. Something dead." It ties the hit-zone tracking rules in Blueprint › The hit-zone model to a last chance.

| Sound event | Insect | Carries | Meaning |
| --- | --- | --- | --- |
| `ambient.mayfly.rises` | Mayfly hatch | 60 m | Soft rises all over a calm bay |
| `ambient.moth.flutter` | Lantern moths | 3 m | Wings ticking on the lantern glass |
| `ambient.deer_fly.circle` | Deer fly | 3 m, player only | A buzzing halo |
| `ambient.horse_fly.buzz` | Horse fly | 6 m | A loud drone that lands |
| `ambient.yellowjacket.nest` | Yellowjackets | 5 m | A hum from a willow: the nest |
| `ambient.blow_fly.buzz` | Blow fly | 10 m | Meat or a carcass close by |

## Migration and the dawn chorus

**Birds are the year's loudest clock.** The Bestiary's arrival table stays; this section turns all 78 birds into seven waves, a monthly soundscape, and a dawn chorus that changes with month and weather.

### The migration calendar

**Each species draws its arrival within ±5 days of its mean, and spring warmth moves the open-water waves.** A breakup 7 days early pulls waves 2 and 3 forward 5 days; a late one pushes them back. Fallout days (Flight in Minecraft › Migration flyways) bunch arrivals.

| Wave | Window | Arrives or passes | Sound of the wave | Messenger line, first time | Spirits |
| --- | --- | --- | --- | --- | --- |
| 1. Late-winter hints | Mar 1 – 31 | Golden eagles Mar 1; snow buntings Mar 10–25; ravens and jays nest; crossbills sing in cone years | Woodpecker drums, boreal owl staccato, bunting twitter | "Snow buntings on the gravel bar: spring is coming." (existing) | +5 (existing) |
| 2. Open water | Apr 10 – 30 | Goldeneyes on leads Apr 10; horned larks Apr 15; rough-legged hawks Apr 15; juncos and longspurs Apr 20; mergansers Apr 20; pintails, robins, varied thrushes, rusty blackbirds, peregrines Apr 25; cranes and swans Apr 25 | Wing whistles over the leads, crane bugles, the first robin | "A robin on the bare ground." | +2 robin; +5 cranes (existing) |
| 3. The duck flood | May 1 – 20 | Ducks and geese May 5–20; loons at ice-out; buffleheads, harriers, ospreys, warblers, kinglets, fox sparrows May 1; white-crowned sparrows, grebes, snipe, yellowlegs May 5; scaup May 10; hermit thrush May 10 | Quacks and honks all night, snipe winnowing, yellowlegs yelping | "Snipe winnowing over the bog." | +2 the first night of winnowing |
| 4. The late ones | May 15 – Jun 5 | Tree swallows, spotted sandpipers, golden-plovers, harlequins May 15; terns, bank swallows, Swainson's thrush May 20; phalaropes May 25; cliff swallows Jun 1 | The full chorus; thrushes at dusk | "Swallows back over the pond." | +2 |
| 5. Early leavers | Jul 25 – Aug 25 | Swallows Aug 5–10; phalaropes Aug 10; yellowlegs and harlequins Aug 15; terns Aug 20; young birds wander | Begging calls, then quiet | "The swallows have gone." | −1, once |
| 6. The big pass | Aug 25 – Sep 30 | Cranes south Aug 25 – Sep 15; geese and ducks Sep 5–25; robins and thrushes in flocks; rough-legged hawks Sep 1; waxwings Sep 15; longspurs pass | Goose skeins day and night, crane flocks at 3 km | "Cranes heading south: about 50 days to freeze-up." (existing) | — |
| 7. Freeze-up stragglers | Oct 1 – Nov 10 | Redpolls, shrikes and gyrfalcons Oct 1; scaup Oct 5; goldeneyes and buffleheads Oct 15; swans Oct 20; mergansers Nov 10; snowy owls in irruption years | Swans calling at dusk, redpoll flocks, then silence | "Swans leaving the oxbows." | — |

**Sky traffic.** In the peak weeks of waves 3 and 6, 4–12 sky flocks pass over each game day, mostly at dusk and in the night. A player out at 23:00 on September 15 hears geese going over every few real minutes.

### Sounds of the season

**The ambience bed changes by month, built from who is present.** The client mixes it from the species list and the chorus rules below, capped at the 12 chorus voices.

| Month | What the valley sounds like |
| --- | --- |
| November – February | Ravens, jays, chickadees, redpolls, the dipper; great horned owls from January; silence below −35 °C except ravens |
| March – April | Woodpecker drumming, boreal owl staccato, snow buntings, ptarmigan "go-back" at dawn, the first robins and juncos |
| May | The full chorus at dawn, snipe winnowing all night, ducks and geese on every pond, cranes overhead, frogs |
| June | White-crowned sparrows singing through the white nights, loons, thrushes at dusk, yellowlegs alarms; the loudest month |
| July | Quieter; begging young, loons, horse flies; the chorus halves |
| August | Flocking calls, geese practising flights, bank swallows leaving; moths at the lantern |
| September – October | Geese and cranes going over, swans at dusk, waxwing trills, chickadee flocks; redpolls arrive |

### The dawn chorus

**Every singing bird within 300 m adds to the chorus, so each camp has its own dawn.** Singers come from the real slots of resident species, so a birch-bench camp sounds of hermit thrush and a slough camp of snipe and blackbirds.

```latex
C = \sum_{s \in S_{300}} n_s \cdot v_s(month) \cdot g(t - t_{dawn}) \cdot w_{weather}
```

- **n and v.** n is the number of singing males of species s; v is its song month curve, 0–1 (robin peaks in May, white-crowned sparrow in June, varied thrush April to June).
- **Timing g.** From May 15 to July 28 there is no dark, so g peaks at 03:30 and the chorus runs 02:30–05:30. In April, early May and August it starts 45 minutes before sunrise and lasts 2 hours.
- **Weather w.** Rain ×0.3, wind over 8 m/s ×0.4, a May cold snap below −5 °C ×0.2, fog ×1.0 (the varied thrush ×1.5), the first clear dawn after a storm ×1.3.
- **Silence.** A hunting goshawk, owl or shrike within 40 m silences singers near it for 5 game minutes. A sudden silence in a loud chorus is a predator sign; the earthquake's warning (Open world in depth › Rare events) is birds lifting all at once.

**Listening is a verb.** Sitting still at dawn for 10 game minutes logs the chorus in the notebook: "Dawn chorus, Day 34: 11 species." Each species already Identified is named; others show as "unknown song". The first dawn listened each day gives Spirits +1, counted as a quiet hour.

## Reading birds

**New signs join the helper and sky-sign tables.** Each is learned by the existing rule: see the sign and its cause within 10 game minutes, twice, or sketch it once for sky signs.

| Sign | Means | Range | True | Messenger line once learned |
| --- | --- | --- | --- | --- |
| Lesser yellowlegs yelping from a spruce tip | The player, or a predator, is near its nest; animals within 150 m are alerted | 300 m | Always | "Yellowlegs calling. Everything here knows you're coming." |
| Magpies chattering over one spot | Meat on the ground, or a predator on it | 300 m | 80% | "Magpies over the willows. Something dead, or something eating." |
| A mob of songbirds in one spruce by day | An owl or hawk within 3 m of the noise | 150 m | 90% | "Birds mobbing in the spruce. An owl." |
| Sudden silence in the dawn chorus | A hunting raptor within 40 m | 40 m | 70% | "The birds went quiet." |
| Swallows, terns and gulls dipping over calm water at dusk | A mayfly or caddis hatch in 1 game hour | 300 m | 75% | "Swallows working the bay. A hatch is coming off." |
| Goldeneye wing whistle in fog or dark | Open water ahead | 150 m | 90% | "Goldeneyes. Open water ahead." |
| Harlequins in a creek | Clear, fast water with big stonefly hatches | 100 m | Always | "Harlequins on the creek: clear water, good grayling." |
| Rough-legged hawks moving south | Snow within 3–10 days | 400 m | 60% | "Rough-legs heading south. Snow coming." |
| Hovering hawks or short-eared owls over a meadow | A vole year | 500 m | 85% | "Hawks hovering on the meadow. Voles are up." |
| Crossbills singing in winter | A spruce cone year: squirrels and martens up next year | 300 m | Always | "Crossbills nesting. A cone year." |
| Varied thrush singing at midday | Fog or rain within 12 game hours | 500 m | 65% | "Varied thrush at noon. Weather coming." |
| Waxwings trilling in a patch | Berries being stripped; pick today | 200 m | Always | "Waxwings on the cranberries. Pick them this week." |
| Spinning phalaropes on a pond | A mosquito hatch from that pond within 7 days | 50 m | 70% | "Phalaropes spinning. Bugs coming off this pond." |
| Fox sparrows scratching | Litter thawed 2 cm; frogs waking | 30 m | 80% | — |
| Horned lark singing on a ridge | Bare, wind-scoured ground: the fast spring line | 200 m | Always | — |
| A shrike's larder on a bush | Its hunting ground; the vole count read like a pellet | 20 m | Always | "Shrike larder. Four voles this week." |
| Ravens playing in ridge wind | Strong wind on that face | 1 km | 80% | "Ravens on the ridge wind." |

**Birds never lie, but they never tell everything.** The helper rule stands: no bird calls for a still, unseen animal, and wind halves every range.

## Eggs, down and feathers

**Birds give four resources, each with a season and a limit.** Eggs are spring calories, down is winter warmth, feathers are arrows, and a few feathers are beauty.

| Resource | Item | From | Season | Yield | Used for |
| --- | --- | --- | --- | --- | --- |
| Wild eggs | `yearofice:wild_egg` | Duck, goose, swan, grouse, gull, crane, loon nests | May 5 – Jun 30 | 30–480 kcal each (Nests and eggs) | Food; the float test |
| Nest down | `yearofice:down` (existing) | Hatched duck, goose and swan nests | Jun 15 – Jul 31 | 5–25 g a nest | The down vest (Bestiary › New items), pillows, mitten lining |
| Plucked down | `yearofice:down` | Ducks 20 g and swans 200 g (existing); geese 50 g (new) | Hunting seasons | As listed | Same |
| Game feathers | `yearofice:game_feather` (existing) | Every bird plucked; molts on water (Every concept from the video › Feathers on the water) | All year | 2–40 per bird | Blunt and broadhead fletching |
| Long primaries | `yearofice:long_primary` (new) | Swans, geese, cranes, loons: 2–4 a bird; molts on the molting lake | Jul – Sep | Each splits into 4 large fletching | Arrows +5% range; the eagle primary stays +10% |
| Owl feathers | `yearofice:owl_feather` (existing) | All 7 owls | All year | 10–30 | Silent fletching |
| Decorative feathers | `yearofice:feather` with component `yearofice:feather_kind` | 8 new kinds: flicker, waxwing, harlequin, pintail, magpie, peregrine, short-eared owl, snowy owl | By species | 1 per find | Feathers II, a second set in Creative concepts' collection; decoration value 1–3 |

**A down summer.** The A3 slough holds 20–40 duck and goose nests. Walking it in late June, after hatching, gives 0.15–0.3 kg of nest down without harm, most of a vest when added to the molting-lake feathers. Taking down from a nest still being incubated lowers its hatch chance from 70% to 50%, because the eggs chill.

**Egg pressure is tracked per species and region.** E is the share of the year's laid eggs the player took.

```latex
pairs_{next} = pairs \cdot \left(1 - \max(0,\ E - 0.25)\right)
```

Up to a quarter of a species' eggs in a region costs nothing next year, because renesting covers it. Beyond that, next spring has fewer pairs, and the messenger says so at their arrival: "Fewer pintail on the slough than last year." Gull eggs are exempt up to 0.5, because colonies renest fully.

### Nest boxes and perch posts

**Three small builds that bring birds to camp.** Each is a Bushcraft-tier piece on the existing build rules.

| Build | Registry id | Materials | Work | Draws | Effect |
| --- | --- | --- | --- | --- | --- |
| Small nest box | `yearofice:nest_box_small` | 4 split planks or bark slabs, pegs | 30 game minutes | Tree swallows, chickadees | Occupied 70% the first June; a swallow pair cuts mosquito B ×0.9 within 30 m |
| Duck box | `yearofice:nest_box_large` | 6 planks, 1 m cordage, 0.1 kg wood shavings | 1 game hour, on a pole over or within 30 m of a pond | Goldeneye, bufflehead, boreal owl | Occupied 40% a year; 8–10 eggs a clutch; down after hatching; a boreal owl in it takes 30 voles a summer within 200 m |
| Perch post | `yearofice:perch_post` | 1 pole 3 m, 1 crossbar | 15 game minutes | Hawk owl, short-eared owl, rough-legged hawk, harrier, shrike | In or beside the garden, vole and hare damage ×0.7 (Blueprint › Crop farming); a raptor on it is a sketch from the cabin door |

## Birds that steal from camp

**Thieves add to the ones already written: jays, ravens and eagles.** Every theft leaves sign, as the Items tab requires: feathers, droppings, a beak mark, a missing item at a known spot.

| Thief | Takes | Rule | Sign | Counterplay |
| --- | --- | --- | --- | --- |
| Gray jay, raven, bald eagle | Meat, fish, shiny items | Existing (Bestiary; Creative concepts › Camp jays and the raven deal; Blueprint › Preserving in detail) | Existing | Existing |
| Raven (new rule) | One food item from an unlashed pack left outside | 20% a game hour when nobody is within 30 m; the flap is pulled open | Raven tracks in snow, the item's wrapper 20–50 m off | Lash the pack, hang it, or keep it inside |
| Black-billed magpie | 30 g of meat a visit; pecks a hide on the stretcher; eggs from the hen house | Hide unattended 2 game hours: hide grade −1; 1 egg a week while the hen house door is open | Peck marks, a black-and-white tail feather | Cover hides with boughs; close the hen house; magpies flee a tamed wolf |
| Mew gull | Fish at the cleaning spot, fillets on the bank, fish in an unwatched canoe | Within 2 game hours of being left, 50% each | Screaming flock; droppings on the gunwale | Clean fish in the water and carry the catch up at once |
| Chickadees | Fat from hanging meat and hides | 10 g a day per flock | Tiny peck holes in fat | A cheesecloth or bark cover; or a suet feeder, which they prefer |
| Goshawk, great horned owl | Hens | Goshawk by day, 1 a week from an open run; owl by night | A pile of feathers, plucked, or a headless hen | A roofed run; the hen house shut at dusk |
| Northern shrike | Voles from mouse traps at the cache | 1 a week | The vole on a thorn nearby | None needed: it is doing the traps' job |
| Waxwings, robins, grosbeaks | Berries from patches the player is saving | 5% of an unpicked patch a day after peak (Plants and fungi › Animals and plants) | Trilling flocks; stripped stems | Pick before October; net a bush near camp |
| Wigeon, juncos, sparrows | Greens and seed from the garden | Existing ×0.7 shallow-seed loss; 0.2 kg of greens a day from a wigeon flock | Webbed or small tracks | Brush fence; correct sowing depth |

**Moment.** In August the player returns from a 2-day hunt. The drying rack hums with yellowjackets, two magpies stand on the hide, and a gray jay on the woodpile holds a strip of fish, looking straight at them.

## Data and build

**Every bird is one species file, one GeckoLib rig and one texture set; every insect is a particle type or a small entity.** The population model owns spawning, as in the Bestiary; no vanilla spawner runs. All classes follow the Minecraft engine tab's code rules: formulas in `com.yearofice.sim`, adapters elsewhere, client code fenced.

### Registry ids

| Kind | Ids |
| --- | --- |
| New bird entities (45) | `yearofice:white_tailed_ptarmigan`, `northern_pintail`, `american_wigeon`, `northern_shoveler`, `lesser_scaup`, `bufflehead`, `common_goldeneye`, `harlequin_duck`, `common_merganser`, `horned_grebe`, `red_necked_grebe`, `wilsons_snipe`, `lesser_yellowlegs`, `spotted_sandpiper`, `red_necked_phalarope`, `american_golden_plover`, `bank_swallow`, `northern_goshawk`, `sharp_shinned_hawk`, `rough_legged_hawk`, `northern_harrier`, `merlin`, `peregrine_falcon`, `boreal_owl`, `saw_whet_owl`, `short_eared_owl`, `snowy_owl`, `northern_flicker`, `black_billed_magpie`, `bohemian_waxwing`, `pine_grosbeak`, `white_winged_crossbill`, `varied_thrush`, `hermit_thrush`, `swainsons_thrush`, `american_robin`, `yellow_rumped_warbler`, `ruby_crowned_kinglet`, `dark_eyed_junco`, `fox_sparrow`, `white_crowned_sparrow`, `lapland_longspur`, `horned_lark`, `rusty_blackbird`, `northern_shrike` |
| Existing birds given ids here (7) | `yearofice:willow_ptarmigan`, `mallard`, `green_winged_teal`, `canada_goose`, `white_fronted_goose`, `raven`, `cliff_swallow` |
| Other entities | `yearofice:bird_flock` (flock), `yearofice:mourning_cloak`, `yearofice:snow_fly` |
| Particle types | `yearofice:mayfly_hatch`, `moth_swarm`, `deer_fly`, `horse_fly`, `warble_fly`, `nose_bot_fly`, `yellowjacket_swarm`, `blow_fly_swarm`; the Bestiary's `bumblebee` and `dragonfly` are particles too (RECONCILE 101) |
| Blocks and block entities | `yearofice:bird_nest` (block entity, property `nest_type`), `yellowjacket_nest`, `nest_box_small`, `nest_box_large`, `perch_post`; block state `cavity` on spruce, aspen, birch and poplar logs |
| Items | `yearofice:wild_egg`, `long_primary`, `feather`, `perch_snare`; calls `call.pish`, `call.loon_wail`, `call.crane_rattle`, `call.owl_trill` |
| Data components | `yearofice:egg` (species, incubation start day), `yearofice:feather_kind` |
| Tags | Entity types: `yearofice:birds`, `flock_birds`, `waterbirds`, `cavity_nesters`, `mobbers`, `mobbing_targets`, `egg_layers`, `camp_thieves`; blocks: `yearofice:perch_blocks`, `cavity_trees` |
| Sound events | `yearofice:entity.<species>.<call>` as listed in each sheet (67 new); `yearofice:ambient.mobbing`; the 6 insect sounds |

### Java modules

| Class | Does |
| --- | --- |
| `com.yearofice.ecosystem.species.bird.YoiBird` | Base bird: extends the shared animal base, implements `GeoEntity`; holds the flight state and the plumage lookup |
| `com.yearofice.ecosystem.species.bird.FlightController` | The 11 flight states, steering rays, runway checks, landing |
| `com.yearofice.ecosystem.species.bird.BirdFlockEntity` | Count, centre, seed, splitting, member promotion, kills against the population |
| `com.yearofice.ecosystem.air.PerchIndex` | Up to 16 perch points per chunk, built on chunk load, saved with the chunk |
| `com.yearofice.sim.air.AirLift` | The thermal and ridge lift field, hourly, on the 32 m wind grid |
| `com.yearofice.ecosystem.air.Flyways` | Flyway splines at world creation; stopover sites; fallout days |
| `com.yearofice.story.MigrationSchedule` | The 7 waves, per-species arrival draws, sky-flock schedule sent to clients |
| `com.yearofice.client.audio.ChorusMixer` | The dawn chorus and monthly bed, 12 voices |
| `com.yearofice.ecosystem.NestBlockEntity` | Nest stages, eggs, abandonment, renesting, down |
| `com.yearofice.sim.EggPressure` | E per species and region; next year's pairs |
| `com.yearofice.ecosystem.AlarmCalls` (extended) | Flush chain, mobbing, the new helper rows |
| `com.yearofice.ecosystem.CampBirds` (extended) | The new thieves |
| `com.yearofice.body.BugPressure` (extended) | The tabanid term and the swat |
| `com.yearofice.ecosystem.insects.InsectHatches`, `insects.CaribouFlies` (shared with Land; S14), `insects.YellowjacketNest` (the nest block entity that Land's `InsectColonies` steps) | Hatches, moths, herd harassment and hide holes, the nest block |

**Client and server.** Flight, flocks, nests and thefts are server-side. Boids, far sprites, sky flocks, insect particles and the chorus are client-side, fed by small payloads: `FlockStatePayload` (centre, velocity, count, seed every 10 ticks) and `SkyFlockPayload` (a flyway, a start time, a species).

### Data files

| Path | Holds |
| --- | --- |
| `data/yearofice/species/<id>.json` | The Bestiary's blocks plus five new ones: `flight`, `flush`, `plumage`, `nest`, `song` |
| `data/yearofice/rig/<rig>.json` | Bone list and animation names per rig; scale per species sits in its species file |
| `data/yearofice/migration/<wave>.json` | The 7 waves: window, species with mean dates, sky-flock counts |
| `data/yearofice/hatch/<id>.json` | Insect windows, weather gates, effects |
| `data/yearofice/loot_table/butchering/<id>.json` | Meat, fat, feathers, long primaries, down |
| `data/yearofice/recipe/` | Perch snare, nest boxes, perch post, hatch-matching flies |
| `data/yearofice/hitzone/<id>.json` | One small box set per bird pose: perched, swimming, flying |
| `assets/yearofice/geo/entity/rig_<rig>.geo.json`, `animations/entity/<id>.animation.json` (merged from the rig set and the species' extras), `textures/entity/<id>/<plumage>.png` | Shared rigs, per-species textures by plumage window |

```json
{
  "id": "yearofice:northern_pintail",
  "rig": "dabbler", "scale": 0.95, "hitbox_m": [0.55, 0.45], "mass_kg": 0.9,
  "regions": ["A3", "B3", "B4"], "habitat": ["flooded_meadow", "sedge_pond"],
  "population": { "per": "region_pairs", "range": [6, 14] },
  "flight": { "cruise_mps": 18, "sprint_mps": 25, "takeoff": "jump", "takeoff_run_m": 0, "glide_ratio": 6, "soars": false, "flocks": true },
  "flush": { "base_m": 60, "hen_on_nest_m": 1, "alarm_bonus": 15 },
  "plumage": [
    { "texture": "drake_breeding", "sex": "m", "from": "09-20", "to": "06-30" },
    { "texture": "drake_eclipse", "sex": "m", "from": "07-01", "to": "09-19", "flightless_days": 25 },
    { "texture": "hen", "sex": "f" }
  ],
  "nest": { "type": "ground_cup", "lay": ["05-05", "06-15"], "eggs": [6, 10], "egg_g": 50, "incubation_days": 23, "down_g": 5, "renest": 0.9 },
  "song": { "events": ["yearofice:entity.northern_pintail.whistle"], "month_curve": [0,0,0,0.6,1,0.4,0,0,0.2,0,0,0], "dawn": true },
  "arrival": { "wave": "open_water", "mean": "04-27", "leave_mean": "09-20" },
  "senses": { "sight": 150, "hearing": 50, "smell": 0, "fov": 320 },
  "breath_s": 30, "butcher_table": "yearofice:butchering/northern_pintail"
}
```

```json
{
  "id": "yearofice:duck_flood",
  "window": ["05-01", "05-20"],
  "breakup_shift": 0.7,
  "species": [
    { "id": "yearofice:mallard", "mean": "05-06" },
    { "id": "yearofice:wilsons_snipe", "mean": "05-06" },
    { "id": "yearofice:lesser_scaup", "mean": "05-10" }
  ],
  "sky_flocks_per_day": [4, 12],
  "messenger": "messenger.yearofice.snipe_winnowing",
  "spirits": 2
}
```

The second block is `data/yearofice/migration/duck_flood.json`; `breakup_shift` is the fraction of the breakup's offset from its mean that the wave copies.

### Milestones

- **M5 (Food and goals).** The white-tailed ptarmigan, the perch snare, wild eggs with the float test, grouse and ptarmigan nests, decorative feathers.
- **M6 (Danger and big game).** `YoiBird`, the flight controller, flocks, perch index and lift; all 15 rigs and 78 bird entities; flush chain, mobbing and the new helper rows; the four voice calls; nests for every species; the new thieves; nest boxes and the perch post; 67 bird sounds.
- **M7 (The year).** Migration waves, flyways, stopovers and fallout; sky flocks; plumage by date and moults; the dawn chorus and the monthly bed; egg pressure across years; all 10 insects; caribou flies and hide holes.

### GameTests

Each is a NeoForge `@GameTest` in `com.yearofice.gametest.BirdGameTests`, batch named by milestone, or a JUnit sim where marked. Ids use the `yearofice:ecosystem/` prefix (Conflicting ids row 22), and the tests run headless with `./gradlew runGameTestServer`.

1. **`yearofice:ecosystem/flush_distance`.** A ruffed grouse (base 10 m) approached head-on at a walk flushes at 13.5–16.5 m, and approached crouched on a tangent at 3.8–4.6 m, in at least 95% of 200 trials each.
2. **`yearofice:ecosystem/egg_float`.** Across 300 eggs, every egg at incubation day 0–3 sinks flat, day 4–12 stands, and day 13 or more floats.
3. **`yearofice:ecosystem/clutch_rules`.** Over 1,000 laying-stage nests, taking a quarter of the eggs (rounded down) never causes abandonment and one egg more always does; taking one egg from an incubating nest abandons 45–55%.
4. **`yearofice:ecosystem/mobbing_finds_owl`.** A boreal owl in a spruce with 8 chickadees within 40 m draws a mob within 2 game hours in 85–95% of 500 trials, and the mob's centre lies within 3 m of the owl.
5. **`yearofice:ecosystem/flock_budget`.** 6 flocks of 300 members and 14 single birds in a test template keep the birds' share of the tick at 3 ms or less and MSPT under 30 ms over 6,000 ticks.
6. **`yearofice:ecosystem/egg_pressure` (JUnit sim).** Over 20 simulated years on 10 seeds, taking 25% of pintail eggs a year keeps A3 pairs within 10% of the no-take run, and taking 60% drops them below half by year 8 on at least 9 seeds.

### Debug commands and tuning

**Every bird system has a `/yoi` debug command, and its global numbers sit in `yearofice-common.toml`** (RECONCILE 118). Per-species numbers stay in the species files.

| Command | Does |
| --- | --- |
| `/yoi bird <target> info` | Flight state, perch, awareness, flush distance now, nest, plumage window |
| `/yoi flock <id> info`, `split` | Count, centre, seed and members promoted; forces a split |
| `/yoi migration wave <id>`, `fallout` | Releases a wave's sky flocks now; makes today a fallout day |
| `/yoi nest <pos> info` | Stage, eggs, incubation day, abandonment and renest chances |
| `/yoi lift show` | Draws the thermal and ridge lift field around the player |
| `/yoi chorus` | Lists the chorus voices playing and the monthly bed |

**Tuning.** Under `[birds]`: `flush_scale` (1.0), `egg_free_share` (0.25, the egg-pressure allowance), `sky_flock_cap` (4), `thief_rate_scale` (1.0) and `chorus_voices` (12). The realism presets set them together. Every rate is per game minute, hour or day, so the day-length world option (24–72 real minutes, default 24; RECONCILE 117) rescales them; sky-flock passes and the dawn chorus keep their real-time lengths.
