# Bestiary

This tab adds 40 species to the 21 in Wildlife and fish and Animals in depth: big game, the small-mammal prey base, raptors and owls, camp birds, water birds, grouse, a frog and the insects. Each one runs on the shared state machine in How animals act and the population model in Populations, and each has one rule that changes how the player lives. It ends with the animals that help the player, the arrival calendar, the sound list, rare sightings and the Minecraft build.

## What is already covered

Do not redo these; they are specified elsewhere and this tab only references them.

- **Existing species (21).** Snowshoe hare, red squirrel, spruce grouse, willow ptarmigan, ducks and geese, porcupine, muskrat, beaver, marten, river otter, red fox, lynx, wolverine, moose, caribou, Dall sheep, wolf, black bear, grizzly, raven, and the cache mice. Their tables are in Wildlife and fish, New species, Senses by species, Who hunts whom, Tracks and sign and Populations.
- **Existing rules this tab builds on.** Awareness 0–100, the 13 states, habituation after 3 days, the hare cycle, carcass succession, raids on food, sign ageing, the encounter director, mosquito pressure B (Bugs), the kingfisher and tern fishing clues (Reading the water), and the 7 rare events (Rare events, in Open world in depth).
- **How new entries fit.** Every new species gets the same `behavior`, `senses`, `sign` and `population_model` blocks and a hit-zone file `data/yearofice/hitzone/<id>.json`, plus a new `helper` block (Animals that help the player). Counts are region limits K, drawn the same way as the core counts.

**Ducks and geese get names, not new rules.** The existing entry keeps its dates and flock rules. Its flocks are now drawn from four named kinds: mallard (40%), green-winged teal (25%), Canada goose (25%) and greater white-fronted goose (10%). Meat and feathers follow the existing duck and goose rows.

## Species sheet

Counts are per region named in "Where"; "core" is B3 Outpost Valley. Yields use the Butchering and yields rates: meat 1,150 kcal per kg, fat 7,000, organs 1,300, for a fall animal.

### Mammals

| Species | Registry id | Where | Per region | Season and schedule | Yield | Danger |
| --- | --- | --- | --- | --- | --- | --- |
| Wood bison | `yearofice:wood_bison` | A3 sedge meadows, C2 winter yard | One herd of 12–20 | All year; grazes dawn and dusk; rut Jul 15 – Sep 1; calves Apr 20 – Jun 1 | Cow: 180 kg meat, 8 kg fat, 10 kg organs, about 276,000 kcal; robe hide 40 kg green, horns, 0.8 kg sinew | Charges more than moose; 30% bluffs |
| Mountain goat | `yearofice:mountain_goat` | A1 steepest south faces of Twin Horn | 3–6 | Day; sheds wool Jun 15 – Jul 31; rut in November | 25 kg meat, 0.5 kg fat, about 33,800 kcal; winter hide with wool, horns | Rockfall from above |
| Coyote | `yearofice:coyote` | Edges, gravel bars, A3, D2 | 0–4; 0 while the wolf pack holds 6 or more | Dusk and night; by day in winter | 5 kg lean meat, 5,750 kcal; pelt | None to the player |
| Northern flying squirrel | `yearofice:flying_squirrel` | Old spruce and birch, core, B2 | 10–20 | Night only, all year | 0.08 kg; not worth taking | None |
| Arctic ground squirrel | `yearofice:ground_squirrel` | Colonies on A2, D1, the B4 Esker | 2–4 colonies of 20–40 | Day; out Apr 15 – Sep 15, then hibernates | 0.45 kg meat; Aug–Sep add 0.08 kg fat: about 1,080 kcal; parka pelt | None |
| Hoary marmot | `yearofice:hoary_marmot` | A1 and B1 boulder fields | 4–8 | Day; out May 10 – Sep 15 | 2.5 kg meat; 1 kg fat by September: about 9,900 kcal; pelt | None |
| Collared pika | `yearofice:pika` | Talus below cliffs, A1, B1, C1 | 10–30 | Day; all year under the rocks | Not taken; its haypile is the yield | None |
| Ermine | `yearofice:ermine` | Everywhere, mostly willow and woodpiles | 6–10 | Any hour; white Oct 15 – Apr 15 | 0.05 kg; white winter pelt | Steals 0.1 kg of meat a day at camp |
| Least weasel | `yearofice:least_weasel` | Meadows and muskeg, C4 | 2–4 | Any hour; under snow in winter | Not taken | None |
| Mink | `yearofice:mink` | Creeks, sloughs, A3, B4 | 3–6 | Dusk and night | 0.5 kg rank meat (wolf food); pelt | Raids minnow traps and the cleaning spot |
| Northern red-backed vole | `yearofice:red_backed_vole` | Forest floor everywhere | Vole index, below | Any hour; under snow all winter | 25 g; marten bait | It is the cache "mouse" |
| Tundra vole | `yearofice:tundra_vole` | Meadows, gardens, A2, A3 | Vole index | Any hour | 40 g; bait | Takes potatoes (Crop farming) |
| Masked shrew | `yearofice:masked_shrew` | Everywhere | Vole index × 0.6 | Any hour; must eat every 2 game hours | 4 g; bait only | Eats fat from caches in winter |
| Little brown bat | `yearofice:little_brown_bat` | Station Nine attic, cliff cracks, A4 shore | One roost of 20–60 | Dusk to dawn; present May 15 – Sep 10 | None | None |

### Birds

| Species | Registry id | Where | Per region | Season and schedule | Yield | Danger |
| --- | --- | --- | --- | --- | --- | --- |
| Bald eagle | `yearofice:bald_eagle` | C3, D3, A4 along the river and lake | 2–6; 10–20 at Salmon Bend in August | Apr 1 – Nov 15; 1–3 winter at open water below Long Falls | 2 kg tough meat; 2 primaries | Takes fish from racks and lines |
| Golden eagle | `yearofice:golden_eagle` | A1, A2, D1 | One pair | Mar 1 – Oct 15 | 2 kg; 2 primaries | Strikes wolf pups under 12 weeks |
| Great horned owl | `yearofice:great_horned_owl` | Forest edges, core | 2–4 pairs | Night; hoots Jan – Mar, nests Feb – May | 0.8 kg; 20 silent feathers | Strikes fur hats in the dark |
| Great gray owl | `yearofice:great_gray_owl` | C4 muskeg and bog edges | 1–3 | Dusk and dawn; by day in winter | 0.6 kg; 20 silent feathers | None |
| Northern hawk owl | `yearofice:hawk_owl` | D2 burn, spruce tops | 1–3 | Day | 0.2 kg | None |
| Gyrfalcon | `yearofice:gyrfalcon` | A2, D1 | 0–1 | Oct 1 – Apr 30 | 1.1 kg | None |
| Gray jay | `yearofice:gray_jay` | Spruce taiga everywhere | One pair or trio per 1 km² | Day, all year; nests in March | 0.07 kg; not worth taking | Steals 50 g of meat per visit |
| Black-capped chickadee | `yearofice:black_capped_chickadee` | Birch, riparian, core | Flocks of 4–12 | Day, all year | None | None |
| Boreal chickadee | `yearofice:boreal_chickadee` | Black spruce, C4, core | Flocks of 3–8, mixed with the above in winter | Day, all year | None | None |
| Common redpoll | `yearofice:common_redpoll` | Birch and alder, everywhere | Flocks of 20–200 | Oct 1 – Apr 30 | 13 g; not taken | None |
| Ruffed grouse | `yearofice:ruffed_grouse` | B2 birch and aspen, riparian | 8–15 | Day; drums Apr 15 – Jun 1 and in late September | 0.5 kg meat, about 600 kcal; 15 feathers | None |
| Sharp-tailed grouse | `yearofice:sharp_tailed_grouse` | D2 burn openings, C4 | One lek of 6–20 | Day; lek dawns Apr 1 – May 15 | 0.6 kg meat, about 700 kcal; 15 feathers | None |
| Sandhill crane | `yearofice:sandhill_crane` | Passes over everywhere; stops in A3 | Flocks of 50–2,000 overhead; 2–20 landed | Apr 25 – May 10 and Aug 25 – Sep 15 | 2.5 kg meat, 0.1 kg fat: about 3,500 kcal; 40 feathers | Spear-bill jab if grabbed wounded |
| Trumpeter swan | `yearofice:trumpeter_swan` | A3 oxbows, A4 | 1–3 pairs | Apr 25 – Oct 20; nests May – Jul | 6 kg meat, 0.3 kg fat: about 9,000 kcal; 60 feathers, 0.2 kg down | Charges canoes near the nest |
| Common loon | `yearofice:common_loon` | A4, lake archetype cores | 1–2 pairs | Ice-out to Sep 30; nests on Loon Island | 2.5 kg fishy meat, about 2,900 kcal; 30 feathers | None |
| Belted kingfisher | `yearofice:belted_kingfisher` | Creek spots with q of 0.5 or more | 1 per good creek spot | May – Sep | None | None |
| Three-toed and black-backed woodpeckers | `yearofice:three_toed_woodpecker`, `yearofice:black_backed_woodpecker` | Beetle-killed spruce, D2 burn | 2–6; ×3 in a burn's first 3 years | Day, all year; drums in April | None | None |
| Snow bunting | `yearofice:snow_bunting` | Gravel bars, ridges, A2 | Flocks of 10–80 | Mar 10 – May 5 and Oct 1 – Nov 1 passes | None | None |

### Amphibian and insects

| Species | Registry id | Where | Count | Season | Yield | Danger |
| --- | --- | --- | --- | --- | --- | --- |
| Wood frog | `yearofice:wood_frog` | Ponds, bogs, beaver ponds | 20–100 per pond | Chorus 5 days after a pond's ice-out; frozen solid Oct 15 – Apr 25 | 10 g; pike bait | None |
| Bumblebee | `yearofice:bumblebee` | Fireweed, willow catkins, berry slopes | Ambient; 0–3 nests per km² | Queens Apr 25; colonies May – Aug | Nest: 20–40 g honey (3,000 kcal per kg), 5 g wax | 3–6 stings if a nest is opened |
| Black fly | particle swarm | Within 50 m of running water | Pressure term | Jun 1 – Aug 15, daytime | None | Bites; adds to bug pressure |
| No-see-um (biting midge) | particle swarm | Within 100 m of still water, calm air | Pressure term | Jul 1 – Aug 20, dusk | None | Passes through the head net |
| Spruce bark beetle | world event + block state | Mature white spruce stands | Outbreak, 0 or 1 region at a time | Flight in June; outbreak 3 years | Standing dry spruce | Raises wildfire risk |
| Dragonfly (darners) | `yearofice:dragonfly` | Ponds, slow water | Ambient; 5–30 per pond | Jun 15 – Aug 31, day; nymphs all year | Nymphs as bait | None |
| Leech | `yearofice:leech` item and effect | Still, warm water: ponds, sloughs, beaver ponds | Per pond | Jun 1 – Sep 15 | Bait for burbot and pike | Attaches when wading |

### Senses and tells

Ranges use the rules in Senses by species: still ×0.5, crouched ×0.33, and the same light, cover and wind modifiers.

| Species | Sees moving at | Hears walking at | Smells at | Tell before it acts |
| --- | --- | --- | --- | --- |
| Wood bison | 80 m | 50 m | 300 m | The herd turns to face the player; bulls paw and roll in dust |
| Mountain goat | 250 m | 40 m | 100 m | Stares down, then side-steps on the ledge |
| Coyote | 80 m | 70 m | 250 m | Sits and looks back over its shoulder |
| Arctic ground squirrel | 60 m | 20 m | — | Stands upright; "sik-sik" chirp |
| Hoary marmot | 150 m | 30 m | 50 m | Sits on a boulder; one long whistle |
| Collared pika | 30 m | 20 m | — | A nasal "eep" from the rocks |
| Ermine, least weasel | 20 m | 20 m | 60 m | Rises on its hind legs, a black tail tip flicks |
| Mink | 30 m | 30 m | 80 m | Hisses, then slips into the bank |
| Bald and golden eagle | 400 m | 60 m | — | Hunches on the perch, then drops off it |
| Great horned owl | 60 m (night ×1) | 80 m | — | Ear tufts flatten; bill clacks |
| Great gray owl | 50 m | 100 m (hunts by ear) | — | Turns its face disc; almost never flees under 5 m |
| Gray jay | 60 m | 40 m | — | Silent glide to a nearby branch |
| Chickadees | 30 m | 20 m | — | "Chick-a-dee" with a counted number of "dee" notes |
| Ruffed grouse | 40 m | 20 m | — | Bobs its head; flushes at 10 m with a roar of wings |
| Sharp-tailed grouse | 120 m on the lek | 30 m | — | A cackle; the lek lifts at once |
| Sandhill crane, trumpeter swan | 300 m | 60 m | — | Heads up; a bugle or honk |
| Common loon | 150 m | 60 m | — | Tremolo call, then dives for 40–60 s |
| Wood frog | 3 m | 3 m | — | Chorus stops within 10 m of the player |

## Mammals in depth

### Wood bison

**A reintroduced herd is the biggest meat on the map, and the most guarded.** Fifteen years before the game, the program behind Station Nine released bison into Slough Country; each animal wears a numbered ear tag. Dr. Elin Marsh's notes list a herd count each spring, so the herd ties the story to the land.

- **Schedule.** The herd grazes sedge meadows in A3 from May to October and moves to the C2 Hot Spring Basin yard when snow passes 60 cm. It walks single file between them along the river, cutting a winter trail.
- **Herd response.** At awareness 70 the cows bunch with calves inside and 1–3 bulls face out. A bull charges a player within 30 m who keeps closing; in the rut (Jul 15 – Sep 1) the line is 50 m. Only 30% of bison charges are bluffs, against 60% for moose.
- **Taking one.** A heart-and-lung hit needs 40 J: the sinew-backed bow at 25 m or less, or two longbow hits. The herd circles a downed animal for 10–30 game minutes before leaving, and returns to the spot for 2 days.
- **Cost.** 180 kg of meat is 2 canoe loads or 2 sled loads. In July it spoils in 2 days at 15 °C, so a summer bison is mostly wasted without a fish camp's racks. Calves come 0.4 per cow per year and adult deaths are 5%, so the herd recovers slowly.
- **Food web.** Wolves test the herd only in snow over 80 cm and take a calf on 10% of tests. Grizzlies take one newborn calf in May in 1 year of 3. Bison wallows become dust baths for ruffed grouse and puddles for wood frogs.
- **Story tie.** Each tag read with X logs "Bison 14, cow, seen day 212" in the notebook. Logging every animal in the herd in one year completes the hidden ambition "Herd count".

**Moment.** In March after a hard snow, the herd's trail is the only packed line across the slough. A player on it travels at summer speed, and wolves use it too.

### Mountain goat

**Goats leave free wool on the willows.** From June 15 to July 31 they shed. Each willow or rock edge within 100 m of their ledges holds a tuft (`yearofice:goat_wool`, 10 g), collected in 5 game seconds; a band sheds about 600 g a season.

- **Use.** 150 g of goat wool felts into goat-wool insoles (Feet, inner, +3 °C, 80% warmth kept wet, 0.1 kg, Bushcraft workbench, 1 game hour). They stack with wool socks only if the boots fit both: hiking boots no, mukluks yes.
- **Danger.** Goats stand above the player. Each goat moving on a ledge over the player has a 5% chance per game minute to knock rocks loose: a falling stone deals 5–15 Condition on a hit, with a clatter 1.5 s ahead.
- **Taking one.** A goat watches downhill like the Dall sheep, and its carcass falls by the same rule (50 m, 30% of the meat lost). Its winter hide (Nov–Mar) with wool makes a sleeping robe of +6 °C.
- **Food web.** Golden eagles take one kid in 1 year of 4. Grizzlies climb to the lick in July.

**Moment.** A player picking wool under Twin Horn hears stones clatter, looks up, and sees a white goat looking down 40 m above.

### Coyote

**Coyotes are newcomers that the wolves hunt.** Coyote K is 4 in a region, set to 0 while the wolf pack holds 6 or more, and wolves kill a coyote they meet on 60% of meetings. A pack crash after the player kills wolves lets coyotes move in within one season.

- **Player.** Coyotes follow a snowshoe trail for up to 2 km and rob snares the way foxes do: after 24 h instead of 48 h. They yip in chorus at dusk, heard 1.5 km off. A coyote answers a howl or a predator squeak: see Water, calls and encounters.
- **Food web.** They take hares, voles, ground squirrels, and the fox's catches. A fox in a coyote's range moves its range 1 km away.
- **Tracks.** 6–7 cm ovals in a straight line, bigger than a fox's and smaller than a wolf's.

**Moment.** The player kills two wolves in December. In February a yipping chorus rises from the gravel bar where the pack used to howl.

### Northern flying squirrel

**The night thief at cubby sets.** A flying squirrel takes cubby bait without a catch on 10% of nights in old spruce, on top of the marten's 15%. It glides 20–40 m between trunks, seen only in torchlight or under a full moon.

- **Food web.** It is the main winter food of great horned and great gray owls in old forest. Its numbers rise with spruce cones: in a big cone year (squirrel middens over 2 m wide) K rises by 50%.
- **Player.** A habituated one visits the suet feeder after dark. Spirits +2 the first time the player watches one glide.

### Arctic ground squirrel

**Ground squirrels are tundra fast food in summer and parka fur in fall.** A colony shows as 20–40 burrow mounds of fresh soil. A snare set in a burrow mouth catches on 40% of day rolls.

- **Hibernation.** Squirrels vanish between Sep 1 and Sep 15 and reappear Apr 15–25. In the burrow their body falls to −3 °C. A burrow is never dug by the player (pillar 3).
- **Fall fat.** August squirrels carry 0.08 kg of fat each, so 10 make 5,600 kcal of fat: a real fat source above the tree line.
- **Parka.** 30 cured ground-squirrel pelts sew into a squirrel parka (Torso, inner, +6 °C, 20% wind block, 60% kept wet, 0.8 kg, Hide tier, 6 game hours). It is light, so a hunter can wear it under the parka.
- **Food web.** Grizzlies dig colonies in September: craters 1 m deep and fan-shaped soil, readable from 300 m. Golden eagles, foxes and coyotes all hunt them.
- **Warning.** The "sik-sik" chirp sounds whenever a raptor, bear or the player comes within 60 m; see Animals that help the player.

**Moment.** On the barrens a sudden chorus of sik-sik rises. The player looks up and sees nothing, then looks back and sees the grizzly digging 150 m away.

### Hoary marmot

**The marmot's whistle is the high country's alarm.** It carries 500 m, louder than any other small animal. A marmot that sees an eagle, bear, wolf or player within 150 m whistles once and goes under its boulder.

- **Taking one.** Marmots feed 40 m from their burrow and return in 4 s. A still, crouched player within 15 m of a burrow mouth gets a shot 30% of the time after 20 game minutes.
- **Fall fat.** By September a marmot holds 1 kg of fat (7,000 kcal), the richest small animal in the game.
- **Food web.** Golden eagles take marmots on 1 day in 10 in summer; their stoop is staged in view by the Hunts in plain view rule.

### Collared pika

**A pika's haypile is the driest tinder in the world.** From July 15 each pika piles grass and flowers under a rock overhang: 1 kg by August 15, 3 kg by September 15. The hay is pika-cured and stays dry in rain.

- **Use.** 0.1 kg of pika hay counts as top-grade tinder: fire catch chance ×1.5 in rain, ×1.2 otherwise. It also fills a mattress (+2 °C asleep).
- **Cost.** Taking more than half a haypile means that pika dies in winter; its colony K falls by 1 the next year. The messenger does not warn. The colony's "eep" calls go quieter the next summer.
- **Tell.** Each pika's haypile shows as a tan block (`yearofice:pika_haypile`) under talus, found by following the "eep".

### Ermine and least weasel

**An ermine that moves into camp guards the cache.** An ermine habituated within 30 m of a cache hunts its mice. Mouse losses at that cache fall from 2–3% a week to 0.5%, with or without deadfalls.

- **Price.** It also steals 0.1 kg of meat a day from open racks, and it kills a snared hare on 10% of nights to drink the blood. It enters the cabin through any gap in the chinking.
- **Pelt.** A white winter pelt (Oct 15 – Apr 15) is trim: an ermine-trim hood adds decoration value 2 and Spirits +1 a week while worn. A brown summer pelt is worth nothing.
- **Weasel box.** A 40 × 15 × 15 cm log box with a 4 cm hole, set by the cache, gives a 50% chance per week that a wild ermine moves in.
- **Least weasel.** The smallest carnivore (35 g) lives under the snow in C4 meadows. It is never trapped. It is seen once a year on average, as a rare-sighting tell.

**Moment.** The player wakes to a thump in the cabin. A white ermine sits on the meat shelf, a mouse in its mouth, and stares back.

### Mink

**Mink rob the fishing kit, not the fish.** A mink visits a minnow trap or net within its den range on 10% of nights and takes 1 fish; on 25% of those visits it empties a minnow trap. It takes 1 fish from the cleaning spot on 30% of nights the guts are left there.

- **Taking one.** The drowning slide in Traps already lists mink. A mink caught gives a prime pelt (worth 2 marten pelts in any trade) and 0.5 kg of rank meat for the tamed wolf.
- **Food web.** Mink take muskrats, voles, frogs and ducklings. A mink in a muskrat pond lowers its muskrat K by 20%.

### Voles and shrews: the prey base

**One number, the vole index V, drives half the predators.** Each region holds V from 0.2 to 2.0. It follows a 4-year cycle whose phase is drawn per seed and is separate from the hare cycle.

| Year in cycle | 1 | 2 | 3 | 4 |
| --- | --- | --- | --- | --- |
| Vole index V | 0.3 | 0.9 | 2.0 | 0.6 |

- **What V changes.** Cache mouse losses scale by V (a peak year makes 2–3% into 4–6%). Garden vole damage scales by V. Fox, ermine and marten litters scale by min(1.5, V). Great gray owls nest only when V is at least 0.9, and lay V × 3 eggs.
- **Red-backed vole.** This is the cache "mouse" of the existing rules: its lang name is "Red-backed vole (mouse)", and messenger lines and prompts say "mice" (RECONCILE 2). Its droppings are the food-poisoning risk in Video checklist.
- **Tundra vole.** It runs in grass tunnels under the snow. It takes potatoes in the garden: V × 3% of the crop a week in September.
- **Masked shrew.** Shrews must eat every 2 game hours. In winter a shrew that reaches a cache eats fat first: 20 g a night. Mouse deadfalls stop shrews too.
- **Reading V.** Fox mousing craters, owl plunge holes, and owl pellets under roosts all scale with V. A player who breaks open 3 pellets reads the year: "Mostly vole bones. A vole year."
- **Abstract tier.** V is not a count of individuals; voles appear only as Bubble-tier entities within 32 blocks (`yearofice:red_backed_vole`), 1 per 100 m² × V.

### Little brown bat

**Bats eat the mosquitoes at Station Nine.** A roost of 20–60 bats lives in the outpost attic from May 15 to September 10. Within 200 m of a roost, mosquito pressure B is ×0.8 from 22:00 to 04:00.

- **Player.** Sealing the attic gap evicts them: B returns to normal, and the roost never comes back in that run. Leaving it costs a smell: Spirits −1 a week while sleeping under a roost unless a ceiling of planks is built.
- **Tell.** At dusk a stream of 20–60 shapes leaves one corner of the roof, with faint clicks played at high pitch.

## Birds in depth

### Bald eagle

**Eagles follow the fish and steal from the player.** Perched eagles on the snags at Salmon Bend mean a run is passing: 10–20 birds in August, seen from 1 km. That makes them a fishing clue like the kingfisher.

- **Theft.** An eagle takes 1 fish from an open drying rack per game day when nobody is within 30 m, the same rule ravens follow for meat. On 2% of hooked fish within 100 m of a perched eagle, the eagle takes the fish off the line before it is landed; the line breaks.
- **Feathers.** Under each roost tree a molted primary drops every 10 game days (May – Sep). A primary splits into 6 fletching feathers of the "large" grade: arrows fletched with them gain 10% range.
- **Food web.** Eagles take spawned-out salmon, fish from otters, ducks, and gut piles thrown in fast current (Handling the catch).

**Moment.** A grayling flashes on the end of the line, and a shadow crosses the water. The rod bends double, then goes slack.

### Golden eagle

**The golden eagle threatens the tamed wolf as a pup.** A wolf pup under 12 weeks old in open ground above the tree line, more than 10 m from the player, is stooped on by the pair on 5% of hours. A stoop is announced by a shadow and a rush of air for 1.5 s; the player's shout or a strike within that time drives it off.

- **Food web.** Golden eagles take marmots, ground squirrels, ptarmigan, fox kits, and one Dall lamb or goat kid in 1 year of 4.
- **Sign.** A whitewashed cliff ledge with a stick nest 2 m across on A1 or D1.

### Great horned owl

**Owl feathers make silent arrows.** Owl flight feathers have soft edges. Arrows fletched with 3 of them make no release noise, so the bow's noise radius from Senses by species is 0; a hare 10 m away does not hear the shot.

- **Hoots.** From January to March pairs duet at dusk, heard 1 km off. The first hoot of the year fires the messenger line "Great horned owls calling: they nest in the coldest month."
- **Hat strike.** Walking at night in January to March within 300 m of a nest with a fur hat on, the player has a 3% chance per game hour to be struck. The hat flies off 2 m, and the head loses its +4 °C until it is picked up. The player takes 3 Condition from talons.
- **Food web.** It takes hares, grouse, ermine, flying squirrels, and young porcupines. It is the only predator that takes porcupines often enough to matter: 1 a year per pair.
- **Pellets.** Grey pellets 6–10 cm under roost trees are read for the vole index.

### Great gray owl

**Plunge holes show where voles are.** A great gray owl hunts by ear and dives through up to 45 cm of snow. It leaves a hole 30 cm wide with wing prints either side, read with X: "Owl strike. Voles under the snow here."

- **Player.** It is the tamest bird in the game: flee distance 5 m, Alert threshold 60. A marten cubby set within 100 m of 3 or more fresh plunge holes catches 1.3 times as often.
- **Vole crash.** In a crash year (V of 0.3 or less) great grays leave the muskeg and hunt trail edges and clearings, including the player's garden, in daylight.

### Northern hawk owl

**The hawk owl hunts beside the player.** It sits on spruce tops in the D2 burn by day. When the player chops wood, clears snow or drags a sled within 30 m, it watches and takes a vole flushed by the work on 30% of game hours. Spirits +1 per day it hunts beside the player, up to 5.

### Gyrfalcon

**A gyrfalcon overhead freezes the ptarmigan.** When the gyrfalcon hunts a willow flat (once every 3–5 winter days in A2 and D1), every ptarmigan flock within 500 m holds tight for 1 game hour. Flush distance falls from 3 m to 1 m, so the throwing stick and the active noose pole work on bird after bird.

- **Tell.** A heavy grey falcon passes low and fast at 30 m height; ptarmigan stop calling. It never lands near the player.
- **Food web.** It takes 1 ptarmigan a day while present; the Abstract tier counts it with the lynx and fox kills.

### Gray jay

**The camp robber steals, and pays it back with warnings.** A pair or trio arrives at any fire, butchering or meal within 10 game minutes in its territory. It takes 50 g of exposed meat per visit and caches it under bark with sticky saliva.

- **Warning.** A jay within 40 m of the player gives a harsh "chuck-chuck" alarm within 5 s when a wolf, bear, lynx, wolverine or raptor comes within 60 m of it. It alarms at the player's tamed wolf too, on 20% of approaches, so it is not perfect.
- **Hand feeding.** After habituation, a jay takes food from a held hand (right-click with fat or meat): Spirits +3 once a day.
- **Winter nesting.** Jays nest in March at −30 °C on the food they cached. Their cached scraps make jays immune to the winter-food shortage, so they are the one bird always at camp.

**Moment.** The player is skinning a hare when the jays go quiet, then explode in alarm at a thicket 50 m away. A lynx steps out, looks, and leaves.

### Black-capped and boreal chickadees

**The number of "dee" notes tells the size of the threat.** Chickadees mob predators, and they add more "dee" notes for smaller, more dangerous ones.

| Threat | "Dee" notes | Meaning |
| --- | --- | --- |
| Moose, bison, the player walking | 2–3 | Big, slow, no danger to a bird |
| Bear, wolf, the tamed wolf | 3–4 | Big predator |
| Lynx, fox, coyote | 4–6 | Middle-sized predator |
| Marten, ermine, mink | 6–9 | Small, fast predator |
| Great horned owl, hawk owl, gyrfalcon | 8–12 | Raptor close by |

- **Reading it.** Once learned, the messenger counts aloud: "Chickadees: 7 dee. Small predator close." A count of 4–6 near the trapline usually means the fox; 6–9 by the cache, the ermine or the marten.
- **Boreal chickadee.** Its call is nasal and slow ("tsik-a-dee-dee"), and it lives only in black spruce. Hearing it tells the player the stand is black spruce: poor firewood, good muskeg berries.
- **Winter.** Chickadees put on 10% of body weight each day and shiver it off each night at −40 °C. A suet feeder keeps a flock of 6–12 at camp from October to March: Spirits +1 a day in December and January, the darkest weeks.

### Common redpoll

**Redpoll flocks rate the birch seed crop.** Flocks of 20–200 work birch and alder catkins from October to April. In a big seed year (1 year in 3, shown by heavy hanging catkins in September) flock sizes double.

- **Player.** Redpolls land on the cabin roof and the fire ring to pick grit and ash. A flock of 200 lifting at once is the loudest sound in a still January morning, heard 200 m.
- **Food web.** They are the gyrfalcon's and hawk owl's winter backup prey.

### Ruffed grouse

**The ruffed grouse is the wary one, not the "fool hen".** Unlike the spruce grouse, it flushes at 10 m with a roar of wings, never freezes in a tree, and needs a light bow or a quick throw. Calls do not draw it.

- **Drumming.** In April and May a cock beats its wings on one log at dawn, a deep accelerating thump heard 400 m. Each cock uses the same drumming log for years, so a blind at it is a sure dawn shot: one bird a week, or it moves.
- **Snow roost.** In winter it dives headfirst into soft snow 30 cm deep and stays under. A player walking within 2 m sets it off in an explosion of powder.
- **Snowshoes.** From November its toes grow comb-like fringes. Its tracks show them: a learned sign that it is a ruffed, not a spruce, grouse.

### Sharp-tailed grouse

**The lek is a fixed dawn hunt with a limit.** From April 1 to May 15, 6–20 birds dance on one open patch each dawn from 05:00 to 07:30. Their stamping and cooing carries 600 m.

- **Hunting rule.** A pole blind (Structures below) 15–20 m from the lek gives a sure light-bow shot. Two birds taken in one week move the lek 300–800 m, and the old one is empty the next year.
- **Moment.** Ten birds stamp and rattle in a circle under a pink sky while the player waits in the blind, not drawing.

### Sandhill crane

**Cranes are the loudest sign of the season turning.** Their rattling bugle carries 3 km. Flocks of 50–2,000 pass high from April 25 to May 10 heading north-west, and back from August 25 to September 15.

- **Player.** The first flock of spring fires "Cranes heading north" and Spirits +5. The first flock of fall fires "Cranes heading south: about 50 days to freeze-up", which ties to the freeze-up estimate.
- **Taking one.** A few stop in A3 meadows for 1–3 days. They see 300 m and walk off at 150 m. A wounded crane jabs with its bill: 10 Condition to a player who grabs it without a finishing blow.

### Trumpeter swan

**A nesting swan attacks the canoe.** Pairs nest on muskrat lodges and islands in A3 and A4 from May to July. A canoe within 30 m of a nest is charged with wing-beats and hisses; the canoe tips 15% of the time if the player does not back off within 5 s.

- **Yield.** 60 feathers, 0.2 kg of down, and 9,000 kcal. 0.4 kg of down from two swans or 20 ducks fills a down vest (Torso, inner, +8 °C, 0.5 kg, 10% kept wet, Hide tier).
- **Food web.** Swans pull pondweed; their feeding raises a pond's weed shelf (Fishing in depth). Wolves and bears take cygnets in July.
- **Tell.** A bugling honk carries 2 km; pairs call together when they land.

### Common loon

**The loon is the lake's voice and its fishing rival.** It takes 0.5 kg of fish a day from Big Lake's spots. The wail at night carries 2 km and is the existing "loon wail" sound.

- **Canoe tell.** A loon's tremolo at the player's canoe means it is within 100 m of the nest on Loon Island. Paddling on costs nothing but the eggs: a pair scared off the nest twice in June abandons it.
- **Stranded in fall.** A loon needs 30–100 m of open water to take off. In freeze-up, a loon caught on a shrinking lead in A4 (10% of years) cannot leave.
- **Rescue.** The player can walk out on 10 cm of ice, throw a hide over it and carry it to open river water. That gives Spirits +10 and the journal line "Carried a loon to open water." Left alone, it is found frozen in 4 days by foxes.

### Belted kingfisher

**The kingfisher clue now has a body.** The Reading the water clue spawns a real kingfisher on a perch over the spot. Its dry rattle carries 200 m. It nests in a burrow in a sand cutbank, often the White Bluff in C3, which marks a cutbank worth searching for iron.

### Woodpeckers

**Flaked bark marks dry firewood.** Three-toed and black-backed woodpeckers flake bark off beetle-killed spruce, leaving red-brown trunks with bark chips in the snow. A flaked spruce dies within 1 year and stands dry within 2.

- **Player.** A flaked spruce felled in its second year is already seasoned firewood: it skips the woodshed drying time. Black-backed woodpeckers arrive in a burn within 1 year of a wildfire and triple for 3 years.
- **Tell.** A slow, hollow drumming in April, and soft tapping all year.

### Snow bunting

**The first returning bird of spring.** Flocks of 10–80 white-winged birds arrive on gravel bars and wind-bared ridges between March 10 and 25, weeks before the snow goes.

- **Player.** The first flock fires "Snow buntings on the gravel bar: spring is coming." Spirits +5, once a year. It is the end of the late-winter Spirits slump.
- **Fall.** Flocks pass back south October 1 to November 1, a tell that snow is near.

## Frog and insects in depth

### Wood frog

**The frog that freezes solid and thaws.** From October 15 to April 25 wood frogs lie frozen under leaf litter: no heartbeat, 65% of their body water ice. A player clearing snow and litter (any snow-clearing or leaf-raking job) finds one per 30 m² cleared in forest within 200 m of a pond.

- **Thaw.** A frozen frog (`yearofice:frozen_wood_frog`) held within 2 m of a fire for 1 game hour thaws and begins to move. Released, it gives Spirits +2 and the journal line "A frozen frog woke up by the fire."
- **Chorus.** For 5 days after a pond's ice goes out, frogs call like distant ducks, heard 300 m. The messenger says "Wood frogs calling: the ponds are open," which tells the player the pond fish are reachable by canoe.
- **Bait.** A live frog on a pike rig gives a bait multiplier of 1.8 from May to July (Tackle).
- **Food web.** Mink, otters, loons, kingfishers and great gray owls eat frogs; frog tadpoles eat mosquito larvae, which feeds into the dragonfly rule below.

### Bumblebees

**Bees set the berry crop.** A bee index from 0.5 to 1.5 is drawn each June from June weather: cold, wet Junes give 0.5–0.8. Every insect-pollinated berry patch's yield is multiplied by it (blueberry, lingonberry, raspberry, fireweed honey flow).

- **First bee.** The first queen on a willow catkin, around April 25, fires "First bumblebee on the willows" and Spirits +3.
- **Nests.** Bumblebees nest in old vole burrows in banks, each a `yearofice:bumblebee_nest` block placed through `WorldEditService.submit`; its buzz at 5 m gives it away. Bees themselves are the `yearofice:bumblebee` particle, not entities (RECONCILE 101). A nest found from July to August holds 20–40 g of honey (60–120 kcal, a sugar treat that gives Spirits +2) and 5 g of wax. Opening it costs 3–6 stings, 1 Condition each, and a swollen hand that slows hold-to-work by 10% for 4 game hours.
- **Bears.** Black bears dig out bee nests in August; the dug bank is sign.

### Black flies and no-see-ums

**Two more bug pressures join the mosquitoes.** The deer and horse flies of Birds and the air › Biting flies make a fourth term (RECONCILE 61). They add to B in Bugs, so the same protections and effects apply, except where noted.

```latex
B_{total} = B_{mosquito} + B_{fly} + B_{midge} + B_{tabanid}
```

| Pest | Date window | Where | Time | Wind cut-off | What stops it | Extra effect |
| --- | --- | --- | --- | --- | --- | --- |
| Black fly | Jun 1 – Aug 15, peak Jun 15 – Jul 10 | Within 50 m of running water | Day, 09:00–19:00 | 6 m/s | Smoke, head net, tucked collar | Bites behind the ears: −1 Spirits per 2 hours exposed |
| No-see-um | Jul 1 – Aug 20 | Within 100 m of still water | Dusk, calm (under 1.5 m/s) | 1.5 m/s | Smoke only; the head net does not stop them | Itching cuts sleep quality ×0.8 if bitten after 20:00 |

The forecast's bug index now names the worst pest: "Bugs tomorrow: Severe (black flies by the river)."

### Spruce bark beetle

**An outbreak kills a forest and leaves a firewood bank.** After two summers with a June over 20 °C average and any windfall stand over 1 ha, one region rolls an outbreak at 30%. Over 3 years it kills 30–70% of mature white spruce in that region.

| Outbreak year | What the player sees | What changes |
| --- | --- | --- |
| 1 | Pitch tubes and sawdust on trunks; woodpeckers flaking | Nothing yet |
| 2 | Needles turn red in patches seen from 1 km | Dead trees are seasoned firewood when felled |
| 3 | Trees stand grey and bare | Wildfire chance for that region ×1.5; windfall in wind over 8 m/s, as in Burn Hills |

- **Build.** Dead trees switch to the block `yearofice:beetle_killed_spruce_log`, with red then grey leaves.
- **Food web.** Woodpecker counts triple; red squirrels lose cone crops (squirrel K −30% for 3 years); moose browse rises in the new openings from year 5.
- **Messenger.** "Red spruce on the north slope. Beetles. That stand will be dry fuel by next fall."

### Dragonflies

**Dragonflies are free mosquito control at ponds.** Within 100 m of a pond with dragonflies, B for mosquitoes is ×0.8 from June 15 to August 31. Each darner is a `yearofice:dragonfly` particle patrolling the pond edge at 1–2 m height, drawn within 32 blocks; it is not an entity (RECONCILE 101).

- **Nymphs.** Sweeping a pond bottom with a mesh scoop (1 game minute) gives 1–3 nymphs, all year except under ice. On a grayling rig they are bait ×1.3.
- **Moment.** At dusk the mosquito cloud around the player's head thins as 6 dragonflies dart through it.

### Leeches

**Wading in warm still water costs blood.** Each 5 game minutes wading in a pond, slough or beaver pond from June to September gives a 30% chance of 1–4 leeches on the legs.

- **Effect.** Each leech gives −1 Spirits when found and −0.5 Condition per game hour attached. They show on the legs when the player looks down, and as a status icon.
- **Removal.** 1 game minute per leech by fingernail, or 10 game seconds each with a hot ember.
- **Bait.** Kept in a wet moss pouch for 7 days, a leech on a set line is burbot and pike bait ×1.5. A meat scrap in a minnow trap left in a pond overnight collects 2–6.

## Animals that help the player

**Wild animals give away other animals.** Each helper is a call or sign that names a threat or a food, once the player has learned it. Learning works like sign: hear the call within 10 game minutes of seeing its cause, twice.

| Helper | Signal | Range | What it reveals | Reliability | Messenger line once learned |
| --- | --- | --- | --- | --- | --- |
| Gray jay | Harsh "chuck-chuck" alarm | 60 m from the jay | Wolf, bear, lynx, wolverine or raptor | 90%; 20% false alarms at the tamed wolf | "Jays alarming to the west." |
| Chickadees | Counted "dee" notes | 30 m from the flock | The threat's size (table above) | ±1 note | "Chickadees: 7 dee. Small predator close." |
| Red squirrel | Scold (existing) | 50 m | Anything moving | Scolds at everything | (existing) |
| Raven | Calls and flies off in one line | Up to 2 km | A carcass the player can open | See the rule below | "A raven calling ahead, leading north." |
| Arctic ground squirrel | "Sik-sik" chirp | 60 m | A raptor (short chirp) or a land predator (long trill) | 95% | "Ground squirrels: land predator." |
| Hoary marmot | One long whistle | 500 m | Eagle, bear, wolf or player | 95% | "Marmot whistle on the scree." |
| Common loon | Tremolo | 100 m | The player is too near its nest | Always | "Loon calling: nest close." |
| Great gray owl | Plunge holes | Seen at 20 m | Voles: a good marten set | Always | "Owl strike. Voles under the snow here." |
| Woodpeckers | Flaked bark | Seen at 50 m | Dry standing firewood | Always | "Flaked spruce: dying, dry by next year." |
| Bald eagle | Birds perched on snags | Seen at 1 km | A salmon run passing | 90% in July – September | "Eagles on the snags: fish running." |
| Gyrfalcon | Ptarmigan go silent | 500 m | Ptarmigan holding tight for 1 game hour | Always | "Falcon over the willows. The ptarmigan are holding." |
| Wood frog | Chorus | 300 m | The pond's ice is out | Always | "Wood frogs calling: the ponds are open." |

**Ravens lead to carcasses.** This adds to Ravens tell the truth. A raven that has seen the player butcher 3 times becomes the camp raven, and it gets the Habituated flag without being fed.

1. When a carcass the player did not make lies within 2 km, the camp raven flies to within 60 m of the player on 50% of mornings.
2. It calls, flies 150–250 m toward the carcass, perches, and calls again.
3. If the player follows, it repeats until the carcass; if not, it gives up after 3 tries.
4. At the carcass it waits for the player to open the hide, then feeds on the scraps. Leaving 1 kg for it keeps the bond; taking all of it ends the leading for 30 days.

**Helpers are honest but not complete.** No helper calls for a still, unseen animal: a bedded bear upwind silences nobody. Wind over 6 m/s halves every call's range, and a river within 30 m halves it again.

## Seasonal arrivals and departures

**The year in animals, from May 1.** Each first arrival fires one messenger seasonal alert and, where marked, Spirits.

| Month | Arrives or starts | Leaves or ends | Messenger line, first time each year |
| --- | --- | --- | --- |
| May | Loons at ice-out; wood frog chorus; ducks and geese May 5–25; little brown bats May 15; marmots out May 10; bison calving ends Jun 1 | Snow buntings by May 5; cranes north by May 10 | "Loons back on the big lake." |
| June | Black flies Jun 1; mosquitoes Jun 10; dragonflies Jun 15; goat shedding Jun 15; beetle flight | Ruffed grouse drumming ends Jun 1 | "Black flies along the river." |
| July | No-see-ums Jul 1; bison rut Jul 15; pika haying Jul 15; bumblebee nests full | Goat shedding Jul 31 | "Bison bulls bellowing in the slough." |
| August | Eagles gather at Salmon Bend; cranes south Aug 25 | Bison rut Sep 1; black flies Aug 15; no-see-ums Aug 20 | "Cranes heading south: about 50 days to freeze-up." |
| September | Ruffed grouse fall drumming; redpolls and gyrfalcon by Oct 1 | Ground squirrels Sep 1–15; marmots Sep 15; bats Sep 10; loons Sep 30; cranes Sep 15 | "Ground squirrels have gone under." |
| October | Redpoll flocks; gyrfalcon on the barrens; snow bunting pass Oct 1 – Nov 1; ermine white Oct 15; frogs freeze Oct 15 | Golden eagles Oct 15; swans Oct 20 | "Ermine turning white." |
| November | Goat rut; bald eagles at open water below Long Falls only | Bald eagles elsewhere Nov 15 | — |
| December | Chickadees at the feeder; voles under the snow | — | — |
| January | Great horned owls hoot | — | "Great horned owls calling: they nest in the coldest month." |
| February | Owls nest | — | — |
| March | Golden eagles return Mar 1; gray jays nest; snow buntings Mar 10–25 | — | "Snow buntings on the gravel bar: spring is coming." |
| April | Sharp-tailed leks Apr 1; ground squirrels out Apr 15; ruffed grouse drum Apr 15; bison calving Apr 20; first bumblebee Apr 25; frogs thaw Apr 25; cranes and swans Apr 25 | Ermine brown Apr 15; gyrfalcon and redpolls by Apr 30 | "First bumblebee on the willows." |

**Spirits from the calendar.** Snow bunting +5, cranes in spring +5, first bumblebee +3. Each counts once a year, so the arrivals mark the year like a clock the player can hear.

## Animal sounds

**Every new species has at least one sound that names it.** All are Minecraft sound events under `yearofice:entity.<species>.<call>`, defined in `assets/yearofice/sounds.json`. The first 8 animal calls in Sound stay as they are. Among them is the wolf's dusk howl, `entity.wolf.howl`: one voice carries 2 km and a chorus 5 km, and Land animals in depth › Wolf turns it into a voice count.

| Sound event | Species | When | Carries | Meaning |
| --- | --- | --- | --- | --- |
| `entity.wood_bison.bellow` | Wood bison | Rut, Jul 15 – Sep 1 | 2 km | Bulls fighting; keep 50 m off |
| `entity.wood_bison.snort` | Wood bison | Warn state | 80 m | A charge comes next |
| `entity.mountain_goat.rockfall` | Mountain goat | Moving above the player | 150 m | Look up and step aside |
| `entity.coyote.yip_chorus` | Coyote | Dusk | 1.5 km | Coyotes in the region; wolves are few |
| `entity.ground_squirrel.chirp` | Arctic ground squirrel | Alarm | 300 m | Short: raptor. Trill: land predator |
| `entity.hoary_marmot.whistle` | Hoary marmot | Alarm | 500 m | A predator or the player in the high country |
| `entity.pika.eep` | Collared pika | Day, July – September | 100 m | A haypile nearby |
| `entity.bald_eagle.chatter` | Bald eagle | At fish and roosts | 500 m | Fish running |
| `entity.great_horned_owl.hoot` | Great horned owl | Dusk, Jan – Mar | 1 km | A nest nearby; guard the hat |
| `entity.great_gray_owl.whoop` | Great gray owl | Night, Feb – Apr | 400 m | A nesting year: voles are up |
| `entity.gray_jay.alarm` | Gray jay | Alarm | 300 m | A predator within 60 m of the jay |
| `entity.gray_jay.whistle` | Gray jay | Routine at camp | 150 m | Jays are coming for scraps |
| `entity.black_capped_chickadee.dee` | Both chickadees | Mobbing | 150 m | Count the notes |
| `entity.common_redpoll.flock_lift` | Common redpoll | Flushed | 200 m | Something disturbed the flock |
| `entity.ruffed_grouse.drum` | Ruffed grouse | Dawn, Apr 15 – Jun 1 | 400 m | The drumming log |
| `entity.sharp_tailed_grouse.lek` | Sharp-tailed grouse | Dawn, Apr 1 – May 15 | 600 m | The lek |
| `entity.sandhill_crane.bugle` | Sandhill crane | Migration | 3 km | The season is turning |
| `entity.trumpeter_swan.honk` | Trumpeter swan | Landing, alarm | 2 km | Swans on the water; a nest if repeated |
| `entity.common_loon.tremolo` | Common loon | Alarm | 1 km | Nest close |
| `entity.belted_kingfisher.rattle` | Belted kingfisher | Day, May – Sep | 200 m | A good creek spot |
| `entity.three_toed_woodpecker.drum` | Woodpeckers | April | 500 m | Dead or dying spruce |
| `entity.snow_bunting.twitter` | Snow bunting | March | 150 m | Spring is coming |
| `entity.wood_frog.chorus` | Wood frog | 5 days after ice-out | 300 m | The pond is open |
| `ambient.bumblebee.buzz` | Bumblebee | Day, May – Aug | 5 m | A nest if it repeats at one bank |
| `entity.little_brown_bat.click` | Little brown bat | Dusk | 20 m | The roost is active |
| `ambient.black_fly.swarm` | Black fly | Day, near running water | 3 m | Black-fly pressure |
| `ambient.no_see_um.itch` | No-see-um | Dusk, calm | Player only | Midge pressure: smoke only |

## Rare sightings

**Small events that people tell friends about.** These are separate from the 8 rare events: each rolls once on May 1 against its chance, changes little, and pays in Spirits, a journal photo and a story. A sighting never makes food easy.

| Sighting | Chance per year | Where and when | What happens | Reward or cost |
| --- | --- | --- | --- | --- |
| The white moose | 2% | A3 or the core, any season | One cow moose is leucistic, pure white, for the rest of the run. It shares one slot with the White Cow legend, so a run never has both (Minecraft development › Legends and named animals) | Spirits +10 on first sight. Pete Lund on the radio: "Grandpa Arvid saw a white cow in '58. He never shot her." Killing her gives a white hide trophy and Spirits −10 for 30 days |
| A black wolf | 10% | Nov – Feb, the core | The year's lone disperser is black instead of grey; it shares one slot with the Black Wolf legend | It can be the taming candidate in Taming a wolf; a tamed black wolf is the same in every stat |
| Wolverine at the cache in daylight | 15% of years a wolverine is in the region | Dec – Mar | The player arrives to see it working the cache for 2 game minutes before it notices | A clear 20 m shot, or a chance to watch; Spirits +5 for watching to the end |
| A cross or silver fox | 8% | Any fox range | One fox is a cross fox; a quarter of those are silver | Its pelt has decoration value 4 against 1 for a red fox |
| A cinnamon black bear | 10% | B2 berry slopes, Aug – Sep | A black bear with a red-brown coat | Players mistake it for a grizzly; the track sizes tell them apart |
| Lynx kittens at play | 10% in hare-high years | Jun – Aug, spruce thicket | 2–3 kittens tumble on a log for 3 game minutes while the mother watches | Spirits +5; a journal photo |
| Wolves swimming the river | 10% | September, at a caribou ford | The pack swims across behind the herd | Shows the ford the herd uses next year |
| Great gray owl irruption | 8% | Winter after a vole crash | 5–10 great grays hunt the C4 muskeg and the core clearings by day | Spirits +3; plunge holes everywhere |
| Two bulls locked dead | 2% | Sep 20 – Oct 31, the core or A3 | Two bull moose died with antlers locked; found by ravens | About 450,000 kcal if reached within 3 days; removes 2 bulls from the region's count |
| Northern lights and loons | 5% | The last loon night, late September, A4 | An aurora over a calling loon pair | Spirits +5, stacking with the aurora storm event |
| A mule deer straggler | 8% (×1.5 after a winter whose snow peaked under 45 cm); never two years running; only while the Interior holds mule deer | Jun 1 – Oct 31; enters across D3's east edge along the Ninemile and stays 3–20 game days in willow and burn edges of D3, C3, B3 or A3 | Always a yearling buck. He walks out west (60%) or wolves kill him (40%); no deer breeds in the valley. Flag `yearofice:deer`; data `rare_sighting/mule_deer_straggler.json` (Land animals in depth › Deer) | Spirits +5 for the first mule deer and +3 more for the straggler; Pete Lund on the radio: "Grandpa Arvid wrote up a mule deer on the upper Ninemile in '64." |

## Structures and items

### New structures

| Structure | Registry id | Tier | Materials | Work | Effect |
| --- | --- | --- | --- | --- | --- |
| Suet feeder | `yearofice:suet_feeder` | Bushcraft | 1 pole, 0.2 kg fat, 1 m cordage | 10 game minutes | Draws chickadees, jays, redpolls and flying squirrels within 30 m; Spirits +1 a day in Dec – Jan; the fat lasts 10 days |
| Pole blind | `yearofice:pole_blind` | Bushcraft | 6 poles, 30 spruce boughs | 1 game hour | At a lek, drumming log or pond: player counts as still and crouched; sight ranges ×0.33 |
| Weasel box | `yearofice:weasel_box` | Bushcraft | 1 log, hatchet | 30 game minutes | Within 5 m of a cache: 50% a week that an ermine moves in |
| Moss leech pouch | `yearofice:leech_pouch` | Start | 0.1 kg wet moss, 1 scraped small pelt | 5 game minutes | Keeps up to 10 leeches or nymphs alive 7 days |

### New items

| Item | Registry id | From | Used for |
| --- | --- | --- | --- |
| Goat wool | `yearofice:goat_wool` | Willow tufts near goat ledges, Jun 15 – Jul 31 | Goat-wool insoles (150 g) |
| Goat-wool insoles | `yearofice:goat_wool_insoles` | Workbench, 1 game hour | Feet, inner, +3 °C |
| Squirrel parka | `yearofice:squirrel_parka` | 30 ground-squirrel pelts, 6 game hours | Torso, inner, +6 °C |
| Down vest | `yearofice:down_vest` | 0.4 kg down, 1 scraped hide | Torso, inner, +8 °C |
| Bison robe | `yearofice:bison_robe` | 1 cured bison hide | Bedding: +10 °C asleep, 6 kg |
| Owl feather | `yearofice:owl_feather` | Great horned and great gray owls; molt finds under roosts | Silent fletching: release noise 0 |
| Eagle primary | `yearofice:eagle_primary` | Molts under roosts | 6 large fletching: +10% arrow range |
| Pika hay | `yearofice:pika_hay` | Haypiles | Tinder ×1.5 in rain; mattress filling |
| Ermine pelt | `yearofice:ermine_pelt` | Winter ermine | Hood trim, decoration value 2 |
| Bumblebee honey | `yearofice:bumblebee_honey` | Nests, Jul – Aug | 60–120 kcal, Spirits +2 |
| Frozen wood frog | `yearofice:frozen_wood_frog` | Cleared litter, Oct – Apr | Thaw by a fire; pike bait |
| Dragonfly nymph | `yearofice:dragonfly_nymph` | Pond sweep | Grayling bait ×1.3 |
| Leech | `yearofice:leech` | Wading, minnow traps | Burbot and pike bait ×1.5 |
| Owl pellet | `yearofice:owl_pellet` | Under roosts | Read the vole index |

## Data and build

**Every species is one data file, one GeckoLib entity type and one butchering table.** The population model owns spawning, so no new species uses vanilla natural spawning; vanilla spawns stay removed by the existing biome modifiers.

### Registry ids and classes

- **Entities (34).** `yearofice:wood_bison`, `mountain_goat`, `coyote`, `flying_squirrel`, `ground_squirrel`, `hoary_marmot`, `pika`, `ermine`, `least_weasel`, `mink`, `red_backed_vole`, `tundra_vole`, `masked_shrew`, `little_brown_bat`, `bald_eagle`, `golden_eagle`, `great_horned_owl`, `great_gray_owl`, `hawk_owl`, `gyrfalcon`, `gray_jay`, `black_capped_chickadee`, `boreal_chickadee`, `common_redpoll`, `ruffed_grouse`, `sharp_tailed_grouse`, `sandhill_crane`, `trumpeter_swan`, `common_loon`, `belted_kingfisher`, `three_toed_woodpecker`, `black_backed_woodpecker`, `snow_bunting`, `wood_frog`.
- **Not entities.** Black flies and no-see-ums are particle swarms (`yearofice:black_fly_swarm`, `yearofice:midge_swarm`) driven by `BugPressure`. The bark beetle is a world event plus the block `yearofice:beetle_killed_spruce_log`. Leeches are an item plus the mob effect `yearofice:leeches`. Bumblebees and dragonflies are particles (`yearofice:bumblebee`, `yearofice:dragonfly`) drawn within 32 blocks, and a bee nest is the block `yearofice:bumblebee_nest` (RECONCILE 101).
- **Java.** Each animal is `com.yearofice.ecosystem.species.<Name>Entity` on the rig bases of Minecraft development › Entities and AI (`YoiAnimal extends PathfinderMob implements GeoEntity`), rendered by `<Name>Renderer extends GeoEntityRenderer`. Birds are `ecosystem.species.bird.<Name>Entity` on `YoiBird` (flight controller, perch, flock); small mammals use `SmallMammal` (burrow, snow-tunnel and hibernate activities). No vanilla goal runs; species that differ only in data use the rig class with a species id.
- **Systems.** `com.yearofice.ecosystem.AlarmCalls` (helpers and dee counts), `VoleIndex`, `ArrivalCalendar`, `RareSightings`, `BeetleOutbreak`, `CampRaven`; `com.yearofice.body.BugPressure` gains the fly, midge, dragonfly and bat terms.
- **Assets.** `assets/yearofice/geo/entity/<id>.geo.json`, `assets/yearofice/animations/entity/<id>.animation.json`, `assets/yearofice/textures/entity/<id>/<variant>_<coat>.png`; winter coats (ermine, ptarmigan-style) swap textures by date, not models.

### Data-pack paths

- Species: `data/yearofice/species/<id>.json`, read by the bootstrap reader (its `register` block) and a reload listener; the datapack registry is withdrawn (RECONCILE 6, 101).
- Loot: `data/yearofice/loot_table/entities/<id>.json` stays an empty file and `doMobLoot` is false, because the death pipeline makes the carcass. `data/yearofice/loot_table/butchering/<id>.json` gives every part, pooled by `yearofice:dressing_step`, and the species file names it in `butcher_table` (RECONCILE 41).
- Tags: `data/yearofice/tags/entity_type/alarm_callers.json`, `prey_base.json`, `raptors.json`, `hibernators.json`, `camp_thieves.json` (shared with Birds and the air).
- Calendar and sightings: `data/yearofice/arrival/<id>.json` and `data/yearofice/rare_sighting/<id>.json`.
- Spawn placement: `RegisterSpawnPlacementsEvent` sets each entity's placement rule (on ground, in water, on leaves); `PopulationSpawner` calls it when a chunk enters the Nearby tier.

```json
{
  "id": "yearofice:gray_jay",
  "regions": ["*"], "habitat": ["spruce_taiga", "birch_slope"],
  "population": { "per": "territory_km2", "range": [2, 3] },
  "schedule": { "active": ["day"], "nest": ["03-01", "04-15"] },
  "senses": { "sight": 60, "hearing": 40, "smell": 0, "fov": 300 },
  "behavior": { "alert": 30, "flee": 10, "habituate_days": 3, "steal": { "kg_per_visit": 0.05, "arrive_min": 10 } },
  "helper": {
    "signal": "yearofice:entity.gray_jay.alarm",
    "triggers": ["#yearofice:large_predators", "#yearofice:raptors"],
    "radius": 60, "delay_s": 5, "reliability": 0.9,
    "false_alarm": { "tamed_wolf": 0.2 }
  },
  "sign": { "track": null, "scat": "small_white", "other": ["bark_cache"] },
  "population_model": { "k_core": 6, "births": { "dates": ["04-20"], "young": 3, "juv_survival": 0.4 }, "adult_deaths_per_year": 0.25, "migration": 0.05, "tracks_sex": false },
  "butcher_table": "yearofice:butchering/gray_jay"
}
```

```json
{
  "type": "yearofice:butchering",
  "pools": [
    { "rolls": 1, "conditions": [ { "condition": "yearofice:dressing_step", "step": "joint" } ],
      "entries": [ { "type": "item", "name": "yearofice:bison_meat", "functions": [ { "function": "set_count", "count": 1800 }, { "function": "yearofice:carcass_condition" }, { "function": "yearofice:copy_freshness" } ] } ] },
    { "rolls": 1, "conditions": [ { "condition": "yearofice:dressing_step", "step": "skin" } ],
      "entries": [ { "type": "item", "name": "yearofice:bison_hide", "functions": [ { "function": "yearofice:pelt_grade" } ] } ] },
    { "rolls": 1, "conditions": [ { "condition": "yearofice:dressing_step", "step": "skin" } ],
      "entries": [ { "type": "item", "name": "yearofice:animal_fat", "functions": [ { "function": "yearofice:season_fat", "fall": 80, "spring": 12 }, { "function": "yearofice:copy_freshness" } ] } ] }
  ]
}
```

The second block is `data/yearofice/loot_table/butchering/wood_bison.json`; counts are 100 g units (RECONCILE 43), so 1,800 is 180 kg of meat, and each pool names its `yearofice:dressing_step`.

### Milestones

- **M5 (Food and goals).** Ground squirrel, marmot, ruffed and sharp-tailed grouse, mink, the new baits (frog, nymph, leech) and the pole blind.
- **M6 (animals in depth).** All 34 new entity types with their data files and butchering tables (bumblebee and dragonfly are particles); the vole index; helpers and the camp raven; the suet feeder and weasel box. The M6 data check now also fails if a `helper` block is missing on any species tagged `#yearofice:alarm_callers`.
- **M7 (The year).** The arrival calendar, migration flocks, hibernation, black flies and no-see-ums, bats and dragonflies, the bark beetle outbreak, frozen frogs and the rare sightings.

### GameTests

Each is a NeoForge `@GameTest` in `com.yearofice.gametest.BestiaryGameTests`, named `yearofice:ecosystem/<test>`, run headless with `./gradlew runGameTestServer`.

1. **Dee counts separate threats.** Over 1,000 mobbing events, mean "dee" notes for raptors and small carnivores is at least 6, for moose, bison and the player at most 3, and fewer than 10% of events fall in the other class's band.
2. **Jays warn in time.** A wolf entity moved within 60 m of a gray jay that is within 40 m of a test player triggers the alarm sound within 5 s in 87–93% of 1,000 trials, matching its 0.9 reliability.
3. **Frogs freeze and thaw.** A wood frog held below −2 °C for 150 game days survives on at least 95% of 1,000 runs; held 2 m from a lit fire, it reaches the moving state within 1 game hour ±5 game minutes.
4. **The bison herd holds and falls.** Over 20 simulated years with no player the herd stays between 8 and 30; taking 1 cow a year drops it below 10 by year 10 on at least 9 of 10 seeds.
5. **The vole index drives owls.** Over 40 simulated years, great gray owl nesting happens only in years with V of 0.9 or more, and cache mouse losses in V = 2.0 years are 1.8–2.2 times those in V = 1.0 years.
6. **Arrivals land on their dates.** Over 100 seeds, the first snow bunting falls between March 10 and 25 and the first spring crane between April 25 and May 10 on every seed, and each fires its messenger line exactly once a year.

## Carcasses and bodies

**A killed animal leaves its whole body, never a drop.** Every species in the game, from a 4 g shrew to a 540 kg bison, dies into a carcass that lies where it fell, cools, stiffens, bloats, rots and ends as bones. The player picks up, carries, drags or works it in place, and dressing it is the only way to get meat, organs, fat, hide and the rest. This section covers all species in the Blueprint, this tab, Waters and fish, and Water, calls and encounters.

**What it builds on.** Butchering and yields (kinds of parts and kcal rates), Field care (big-game steps, deadlines, the gut pile, the predator clock, the haul), The hit-zone model, Spoilage model, Carcass succession in Who hunts whom, Traps (catches spoil and freeze), Handling the catch (fish parts), Winter's dead, Game guts and Wounds that last in Every concept from the video, and Warmth from a fresh kill and Game drag and hauling before snow in Ideas from viewers. Those numbers stand; this section adds the body itself.

### The carcass entity

**A carcass is a thing in the world, not a corpse animation.** When an animal dies, its living entity is removed at once and a carcass entity of the same species takes its place, in the same spot and facing. The carcass has no health, no AI and no hurt reaction. It holds one record: species, sex, age class, time and cause of death, core temperature, freshness, rigor phase, a damage map, the dressing steps done, and any wound record.

**The death pose.** A dying animal plays a 1–2 s collapse taken from what it was doing: a running animal tumbles forward, a standing one buckles at the knees, a bird folds and drops, a swimming one rolls belly-up. The carcass then shows one of three poses from its body plan: on its side (most mammals), on its chest (bears, beavers and porcupines 50% of the time), or wings spread (birds).

**The settle, without a ragdoll.** The carcass snaps to the ground under its four corners and takes the slope's pitch and roll, with the belly downhill. It never clips into blocks. On ground steeper than 30° it slides down the fall line until the slope eases, up to 50 m.

- **Falls cost meat.** A slide or a drop over 10 m loses 0.6% of the meat per metre, up to the 30% of the existing ledge rule for Dall sheep and mountain goats. The loss is marked as bloodshot on the damage map.
- **Into water.** A carcass that falls into water follows the float rules in Carcasses in water, below.

**Reading a carcass.** Looking at one shows a single line: species, sex and age, and its stage, such as "Bull moose, prime. Still warm inside." Use with a knife opens the carcass card: a body outline with the damage map coloured, the steps done and next, the Field care deadlines and the parts already taken. A carcass the player did not make adds the cause line from Winter's dead.

**The kill pile.** Parts from dressing go into the pack when they fit. What does not fit stays with the carcass in a 27-slot kill pile, opened with use, and nothing is ever dropped as loose items. The kill pile spoils with the carcass and is eaten by scavengers in the order below, so a part left behind is still there, or partly gone, on the next trip.

### From warm to bones

**A carcass runs one clock with three flags.** Freshness follows the Spoilage model. The flags Warm, Stiff and Frozen sit on top of it and change what the player can do. The stages show on the model, so the player reads them from 10 m.

| Stage or flag | When | What the player sees and smells | What it changes |
| --- | --- | --- | --- |
| Warm | Core temperature 20 °C or more (the τ table in Warmth from a fresh kill) | Limp; steam from the mouth and any wound below 0 °C | Warm hands works; the meat clock runs fastest inside |
| Cooling | Core under 20 °C, before rigor | Limp; eyes dull | Normal work |
| Stiff (rigor) | From onset to release, by size class below | Legs locked straight; a bird's wings will not fold | Position ×2 time, skin and joint ×1.25, no bleeding |
| Relaxed | After rigor | Limp again | Normal work |
| Bloated | Freshness under 30 (Turning), not frozen | Belly swollen 15%, legs splayed; flies above 10 °C | Organs spoiled; meat Turning; scent ×2 |
| Rotting | Freshness under 10 | Fur slipping in patches, dark skin, a fly cloud and maggots above 5 °C | Meat only bait; hide lost; scent ×3 |
| Bones | Meat gone to scavengers or rot | A skeleton on the ground | Becomes a bone pile (Bones and antlers on the land) |
| Frozen | Core below −5 °C | Frost on the fur; a hollow knock on use | Clock stops; hatchet only; hide cannot come off (Field care) |

**The whole-carcass clock.** An unopened carcass spoils like an uncleaned whole fish, k = 0.6, times a size factor s for the heat held in a big body. Gutting removes both: the cavity cools and the meat runs on the plain meat clock, k = 1.0.

```latex
t_{bloat} = L(T) \cdot k \cdot s \qquad k_{unopened} = 0.6,\ k_{gutted} = 1.0 \qquad s = 1.0\ (\text{tiny, small}),\ 0.9\ (\text{medium}),\ 0.75\ (\text{large}),\ 0.6\ (\text{huge})
```

Rotting starts at 1.29 × t\_bloat and the meat is gone to rot at 1.43 × t\_bloat. With no scavengers, the soft tissue then takes 20 game days above 10 °C, or 40 at 0–10 °C, to leave bare bones. Below 0 °C that pauses.

| Air | L (Spoilage model) | Small, unopened: bloats after | Huge, unopened: bloats after | Any gutted carcass: Turning after |
| --- | --- | --- | --- | --- |
| 20 °C | 1.2 days | 17 game hours | 10 game hours | 28 game hours |
| 15 °C | 2.0 days | 29 hours | 17 hours | 2.0 days |
| 10 °C | 3.5 days | 2.1 days | 30 hours | 3.5 days |
| 5 °C | 6.0 days | 3.6 days | 2.2 days | 6.0 days |
| 0 °C | 10.4 days | 6.2 days | 3.7 days | 10.4 days |
| −3 °C (half-frozen) | 30 days | 18 days | 11 days | 30 days |
| −5 °C and below | Frozen | Stops | Stops | Stops |

The Field care deadlines still apply to large and huge kills, and the "bone sour" rule hits the deep hindquarter long before the whole carcass bloats. A summer moose left whole is all Turning at the doubled deadline, 8 game hours above 10 °C, long before it swells at 17.

**Rigor by size.** Onset is counted from death with the core above 10 °C, and doubled when the air is below 0 °C. A carcass that freezes before onset freezes limp in its death pose and never stiffens.

| Class | Onset | Lasts |
| --- | --- | --- |
| Tiny, small | 1 game hour | 10 game hours |
| Medium | 2 | 18 |
| Large | 3 | 24 |
| Huge | 4 | 36 |

Below 0 °C air the duration is ×1.5. The working lesson is the old one: gut and skin while it is limp, or wait until it lets go.

### Size classes

**Size decides how the body moves, never what it yields.** Class comes from whole weight, which sets the carry options below. Weights are for a prime adult in fall. The other sex weighs 0.8 as much (a bison bull 1.5 times the cow), yearlings 0.5, and young of the year grow from birth weight to 0.3 of an adult by October 1.

| Class | Whole weight | Examples | What the player can do |
| --- | --- | --- | --- |
| Tiny | Under 0.5 kg | Voles, shrews, red squirrel, ermine, chickadees, jays, redpolls, teal, terns, mew gull | Pocket it: it goes into the pack as an item |
| Small | 0.5–5 kg | Hare, grouse, ptarmigan, ducks, geese, marten, muskrat, mink, ground squirrel, raven, owls, eagles, loons, crane | Carry in the hands, hang a brace of up to 3 from a lash point, or put it in a game bag |
| Medium | 5–40 kg | Fox, porcupine, marmot, otter, lynx, coyote, wolverine, beaver, swan, wolf, a June calf | Carry in the hands, on the shoulders (10 kg and up), or lashed whole to the pack frame |
| Large | 40–160 kg | Dall sheep, mountain goat, caribou, black bear, a fall moose calf | Drag it: bare on a rope, or on a travois, hide drag, sled or canoe within their limits |
| Huge | Over 160 kg | Moose cow and bull, grizzly, wood bison | Roll it only; dress it where it lies, over several trips |

**Edges.** A carcass partly dressed weighs what is left on it, so it can drop a class. A gutted caribou (110 to 92 kg) still drags.

### Carrying, dragging and setting down

**The video's controls are the game's controls.** Sneak plus use with an empty main hand (Shift and right mouse) takes hold of a carcass. Sneak plus use on the ground while carrying sets it down gently. Q drops it at once, as with any hand-carried load. The prompt line names the action and the weight: "Shift + RMB  Pick up beaver (18 kg)".

| Action | Input | Time | Notes |
| --- | --- | --- | --- |
| Pocket (tiny) | Use, no sneak needed | Instant | Into the pack as an item; stacks to 16 of one species and freshness band |
| Pick up (small, medium) | Sneak + use, empty hand | Small instant; medium 1.5 real s heave | The carry slot replaces the hotbar (Minecraft engine › Weight and carrying) |
| Shoulder (medium, 10 kg and up) | Sneak + use again while carrying | 3 real s | Moves the carcass from the arms to across the shoulders |
| Lash to the frame (small brace, medium) | Use on a worn pack frame from the carry slot | Small 1 game minute; medium 5 game minutes and 2 m of cordage | A brace of 3 small takes 1 lash point; a medium carcass takes all 3 |
| Take the drag rope (large) | Sneak + hold use with 2 m of cordage in the hotbar or off-hand | 2 real s | No cordage: drag by antlers, horns or legs at 70% of rope speed, and only up to 90 kg |
| Load onto a travois, sled or canoe | Use while dragging, at the vehicle | 3 real s, 40 kcal | Refused over the vehicle's limit: "Too heavy for the travois: gut it or halve it" |
| Roll (huge) | Sneak + hold use | The Position step of Field care | Turns it on its back or side; never moves it |
| Set down | Sneak + use on a block | 1 real s | Placed facing the player, no damage |
| Drop | Q | Instant | A carcass of 5 kg or more loses 2% of its meat as bruising |

**Speed and calories.** The carcass weight joins the load, so the existing bands decide speed: under 15 kg full, 15–30 kg 85%, 30–45 kg 65% with no running, over 45 kg drag only at 30%. An 18 kg beaver on top of a 10 kg pack is the video's slow walk home at 85%.

| Mode | Speed | kcal | Noise | Other costs |
| --- | --- | --- | --- | --- |
| Small in the hands | Load band | Load band | Footsteps ×1.0 | No tools or bow |
| Medium in the hands | Load band, at most 85% | Load band +10% | Footsteps ×1.3; brush crashes heard at 40 m | The body blocks the lower 25% of the view; no sprint |
| Medium on the shoulders | Load band | Load band | Footsteps ×1.1 | Heave costs 0.5 kcal per kg; a Warm carcass adds +3 °C feels-like to the torso zone |
| Lashed on the pack frame | Load band, frame limit 45 kg | Load band | Footsteps ×1.0 | Hands free for the bow; lashing time |
| Bare drag on a rope | Snow 30%, wet moss and grass 25%, forest floor 20%, gravel and rock 15% | The travois formula with f × 1.5 | A scrape heard at 40 m | Hide grade −10 per 100 m over forest floor or gravel |
| Travois, hide drag, sled, canoe | As in Game drag and hauling before snow and the sled rules | As there | As there | The drag line and its scent trail |

**Blood on the trail.** A carcass carried or dragged within 2 game hours of death drips. It leaves blood drops every 10 m (fading by the blood-trail rules) and a scent trail of strength 0.5 for medium and 1 for large, which joins the drag line rule.

**Dropping it for a bear.** A bear that charges a player carrying meat stops at a dropped carcass 70% of the time and feeds instead. Q is therefore the right answer, and the journal says so after the first time.

### Dressing by size class

**Each step is a hold-to-work job on the carcass card.** Holding use with a knife runs the next step; the card lets the player skip or choose (bleed or not, pluck or skin, careful or fast gutting, bone out or not). Times are at practice 0 and fall up to 40% with practice. A cleaning spot cuts small and medium times by 30%, as it does for fish.

**Dressing happens where the carcass lies.** Small and medium game can be dressed on any ground, not only at the cleaning spot. The cleaning spot is still faster and keeps the scent point in one known place.

| Step | Tools | Tiny | Small | Medium | Large | Huge |
| --- | --- | --- | --- | --- | --- | --- |
| 1. Bleed (optional) | Knife; a pot or cup in the off-hand keeps the blood | — | 0.5 game min | 2 | 3 | 5 |
| 2. Gut | Knife | Clean: 1 game min for steps 2–4 together | Hare 3 (Butchering and yields) | Beaver 10 | Sheep 20, caribou 25, black bear 35 (Field care) | Moose 45 (Field care) |
| 3. Skin or pluck | Knife; hands to pluck | — | Hare 5; bird pluck 4 or skin 1 | Beaver 25 | 20–45 | 45 |
| 4. Joint or quarter | Knife; hatchet to split pelvis and brisket on large and huge (without it +50% time) | — | Hare 5, or left whole | Beaver 20 | 30–60 | 100 |
| 5. Bone out (optional) | Knife | — | — | 10 | 30 per quarter | 30 per quarter |

**Species without a listed time** scale from the nearest reference animal (hare 1.5 kg, beaver 18 kg, Dall sheep 70 kg, bull moose 450 kg):

```latex
t_{step} = t_{ref} \cdot \left(\frac{W}{W_{ref}}\right)^{0.6}
```

A lynx (11 kg) skins in 25 × (11/18)^0.6 ≈ 19 game minutes, and a wood bison cow (540 kg) quarters in about 112.

**What each step gives.**

| Step | Yields | Rules |
| --- | --- | --- |
| Bleed | Blood, 1% of whole weight, only into a held container | Only within 30 game minutes of death and before Stiff. Meat from a bled carcass keeps 15% longer. A heart-and-lung kill has bled inside and needs no bleeding; a head, spine or trap kill left unbled keeps 10% shorter |
| Gut | Heart, liver, kidneys (birds: heart, liver, gizzard); 30% of the fat as kidney fat; glands (castor, musk); game guts | Guts go to the pack as `yearofice:game_guts` (0.5 kg each, up to 4); the rest becomes a `yearofice:gut_pile` block where the gutting was done |
| Skin or pluck | Hide, pelt, or feathers and down; 70% of the fat as back fat; quills; the beaver tail | Plucked birds keep their skin and its fat; skinned birds lose the feathers |
| Joint or quarter | Meat (small and medium into the pack by weight; large and huge as hand-carried quarters, as in Field care); backstrap sinew; the head: tongue, brain, skull, teeth, antlers or horns | A head taken off whole is a 25 kg hand load on a moose (the Field care trophy load) |
| Bone out | Boned meat, marrow bones; the frame stays at the kill | Bone weights and the faster spoiling of boned meat are as in Field care |

**Frozen carcasses.** Only the hatchet works on a frozen carcass, at 3 times the time, and the hide is lost (Field care). A small frozen carcass thaws by a fire as in Butchering and yields; a medium one thaws in 1 game hour per 3 kg within 3 m of a Hot fire.

### The damage map

**Every carcass remembers where it was hurt.** The body is 8 regions (head, neck, the four quarters, loin, ribs) plus organ slots and the hide. Each region holds its meat in kilograms, marked clean, bloodshot, tainted, sour, eaten or frozen. The carcass card colours them, so a player sees what a bad shot cost before cutting.

| Cause | Where it lands | Loss |
| --- | --- | --- |
| Broadhead wound channel | The regions on the arrow's path | 0.2 kg bloodshot for medium, 0.5 kg for large and huge; small game loses 20% to a broadhead body hit, none to a blunt to the head |
| Shoulder strike (the shoulder rule) | That front quarter | 15% bloodshot |
| Heart-and-lung hit | Heart slot | The heart is lost 50% of the time |
| Liver hit | Liver slot | The liver is lost |
| Gut hit | Loin and hindquarters; liver and kidneys | 20% of the meat tainted (the existing rule); each of liver and kidneys tainted 50% of the time |
| Late gutting (after 1 hour) | Loin and hindquarters | 25% sour; the larger of this and a gut hit applies, never both |
| Late quartering ("bone sour") | The deep hindquarter | 30% Turning; the whole carcass at double the deadline |
| A fall over 10 m | The quarters on the side that hit | Bloodshot, as in The settle above |
| Scavengers | By the eating order below | Eaten |
| Hide | Each arrow hole −5 grade; dragging and scavenger damage as listed; slipping hair above 5 °C after 12 hours (Butchering and yields) | Grade feeds pelt grades in The fur trade |

**Tainted meat is not lost, it is risky.** Tainted cuts carry a 20% gut-illness chance cooked and 40% raw. Trimming them away (1 game minute per kg) turns them into bait, which is the careful player's choice.

### Parts by species

**Every species has one parts row.** Meat, fat and organ figures match Butchering and yields and the Species sheet above; this table adds the whole weight, the class and every named part. Weights are in kilograms for a prime fall adult of the sex named; the other sex weighs 0.8 as much, except a bison bull at 1.5 times the cow. Whole weight minus the listed parts is blood, hooves, trim and the frame left at the kill. For a 450 kg bull moose that remainder is 145 kg: about 27 kg of blood and 118 kg of frame, skull, lower legs and trim, once its 15 kg of antlers are counted. Meat is boneless weight, 40% of the bull (RECONCILE 53). An individual's parts scale with its own live weight, so a 600 kg bull gives 600/450 of its row; each row's whole weight lies inside the weight range in Land animals in depth (a 110 kg caribou bull, a 38 kg wolf).

**Mammals.**

| Species | Whole | Class | Meat | Fat (fall) | Organs | Cover | Other parts | Guts |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Masked shrew | 0.004 | Tiny | — | — | — | — | Whole, as bait | — |
| Red-backed vole, tundra vole | 0.025, 0.04 | Tiny | — | — | — | — | Whole, as marten bait | — |
| Little brown bat | 0.009 | Tiny | — | — | — | — | None | — |
| Least weasel | 0.04 | Tiny | — | — | — | Pelt, not traded | — | — |
| Ermine | 0.08 | Tiny | — | — | — | Pelt; white Oct 15 – Apr 15 | — | — |
| Northern flying squirrel | 0.12 | Tiny | 0.08 | — | — | Pelt, not traded | — | — |
| Collared pika | 0.15 | Tiny | — | — | — | — | None | — |
| Red squirrel | 0.32 | Tiny | 0.22 | — | — | Small pelt | Tail hair for flies | 0.04 |
| Arctic ground squirrel | 0.8 | Small | 0.45 | 0.08 (Aug–Sep) | 0.03 | Parka pelt | — | 0.1 |
| Marten | 1.0 | Small | 0.4, rank | — | 0.03 | Pelt 0.25 | — | 0.1 |
| Mink | 1.0 | Small | 0.5, rank | — | 0.03 | Prime pelt 0.1 | 2 musk glands | 0.1 |
| Muskrat | 1.1 | Small | 0.45 | 0.01 | 0.02 | Pelt 0.15 | 2 musk glands (lure for mink and fox) | 0.1 |
| Snowshoe hare | 1.5 | Small | 0.87 | — | 0.05: heart, liver, kidneys | Pelt 0.1; white Oct – Apr | 4 feet; 0.15 kg soup bones | 0.2 |
| Red fox | 5.5 | Medium | 2.0 | 0.05 | 0.15 | Pelt 0.6, tail on | — | 0.6 |
| Hoary marmot | 6 | Medium | 2.5 | 1.0 | 0.15 | Pelt 0.5 | — | 0.6 |
| Porcupine | 7 | Medium | 2.5 | 0.27 | 0.2 | — | 200 quills, only if skinned | 1.0 |
| River otter | 9 | Medium | 3.0, fishy | 0.3 | 0.2 | Pelt 1.0 | — | 1.0 |
| Lynx | 11 | Medium | 4.5, pale and mild | 0.1 | 0.3 | Pelt 1.2 | 18 claws | 1.2 |
| Coyote | 12 | Medium | 5.0, lean | — | 0.3 | Pelt 1.0 | — | 1.2 |
| Wolverine | 14 | Medium | 5.0, rank; cooked only | 0.2 | 0.25 | Pelt 1.5, the ruff | 20 claws | 1.4 |
| Beaver | 18 | Medium | 6.5 | 0.2, in the tail | 0.3: heart 0.08, liver 0.2, kidneys 0.02 | Pelt 1.5 | Tail 1.0; 2 castor glands 0.12; 2 oil sacs; 4 incisors | 2.0 |
| Wolf | 38 | Medium | 16.5 | 0.05 | 0.8: heart 0.25, liver 0.45, kidneys 0.1 | Pelt 3.0 | Skull, 4 canines, sinew 0.1 | 3.0 |
| Goat (homestead) | 45 | Large | 18 | 1.0 | 0.9 | Hide 3 | Horns, brain 0.12 | 5 |
| Dall sheep, ram | 70 | Large | 23 | 0.3 | 1.2: heart 0.3, liver 0.7, kidneys 0.1, tongue 0.1 | Hide 5 | Curled horns, sinew 0.2, marrow bones 2, brain 0.15 | 9 |
| Mountain goat | 80 | Large | 25 | 0.5 | 1.0: heart 0.25, liver 0.6, kidneys 0.1, tongue 0.05 | Hide 6, with wool Nov – Mar | Horns, sinew 0.2, marrow bones 2, brain 0.15 | 10 |
| Black bear, boar | 90 | Large | 30 | 2.0 | 1.2: heart 0.35, liver 0.65, kidneys 0.2 | Hide 10 | 20 claws, skull, sinew 0.2, brain 0.25 | 8 |
| Caribou, bull | 110 | Large | 44 | 1.0 | 2.0: heart 0.7, liver 1.0, kidneys 0.2, tongue 0.1 | Hide 6 | Antlers (both sexes), back sinew 0.3, marrow bones 3, brain 0.3 | 15 |
| Grizzly, boar | 200 | Huge | 48 | 3.2 | 1.8: heart 0.5, liver 1.0, kidneys 0.3 | Hide 25 | 20 claws, skull, sinew 0.3, brain 0.35 | 16 |
| Moose, bull | 450 | Huge | 180 | 3.0 | 8.3: heart 2.5, liver 4, kidneys 0.8, tongue 1 | Hide 30 | Antlers, nose 1.5 (counts as meat), sinew 0.5, marrow bones 8, brain 0.45, 4 hooves | 60 |
| Wood bison, cow | 540 | Huge | 180 | 8 | 10: heart 2.5, liver 5, kidneys 1, tongue 1.5 | Robe hide 40 | Horns, sinew 0.8, marrow bones 12, brain 0.5, the numbered ear tag | 45 |

**Birds.** Birds yield heart, liver and gizzard together as "giblets"; the column gives their weight.

| Species | Whole | Class | Meat | Fat | Giblets | Cover | Other parts | Guts |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Chickadees, redpoll, snow bunting, tree swallow, dipper, kingfisher, woodpeckers | 0.01–0.15 | Tiny | — | — | — | 2–5 feathers | None | — |
| Gray jay | 0.07 | Tiny | — | — | — | 4 feathers | None | — |
| Arctic tern | 0.11 | Tiny | — | — | — | 5 feathers | None | — |
| Black-billed magpie (new, below) | 0.18 | Tiny | — | — | — | 10 feathers, 2 long tail feathers | None | — |
| Northern hawk owl | 0.3 | Tiny | 0.2 | — | — | 10 silent feathers | — | 0.02 |
| Green-winged teal | 0.35 | Tiny | 0.2 | 0.01 | 0.02 | 10 feathers, down 0.01 | — | 0.04 |
| Mew gull | 0.4 | Tiny | 0.2, fishy | — | 0.02 | 10 feathers | — | 0.04 |
| Willow ptarmigan | 0.55 | Small | 0.35 | — | 0.03 | 12 feathers | Feathered feet | 0.06 |
| Ruffed grouse | 0.62 | Small | 0.5 | — | 0.03 | 15 feathers | Tail fan; crop | 0.08 |
| Spruce grouse | 0.65 | Small | 0.52 | — | 0.03 | 15 feathers | Crop | 0.08 |
| Sharp-tailed grouse | 0.85 | Small | 0.6 | — | 0.03 | 15 feathers | Crop | 0.1 |
| Great gray owl | 1.0 | Small | 0.6 | — | 0.03 | 20 silent feathers | Talons | 0.1 |
| Mallard | 1.1 | Small | 0.75 | 0.02 | 0.05 | 20 feathers, down 0.03 | — | 0.12 |
| Raven | 1.2 | Small | 0.5, tough | — | 0.04 | 20 black feathers | — | 0.1 |
| Great horned owl | 1.5 | Small | 0.8 | — | 0.04 | 20 silent feathers | Talons | 0.15 |
| Gyrfalcon | 1.6 | Small | 1.1 | — | 0.04 | 15 feathers | Talons | 0.15 |
| Osprey | 1.6 | Small | 0.6, fishy | — | 0.04 | 20 feathers | Talons | 0.15 |
| Pacific loon | 1.7 | Small | 0.9, fishy | — | 0.05 | 20 feathers | — | 0.15 |
| Hen (homestead) | 1.8 | Small | 0.9 | 0.1 | 0.06 | 30 feathers | — | 0.15 |
| White-fronted goose | 2.7 | Small | 1.6 | 0.14 | 0.08 | 30 feathers, down 0.06 | — | 0.3 |
| Canada goose | 3.8 | Small | 2.3 | 0.19 | 0.1 | 30 feathers, down 0.08 | — | 0.4 |
| Sandhill crane | 4.0 | Small | 2.5 | 0.1 | 0.1 | 40 feathers | — | 0.4 |
| Golden eagle | 4.5 | Small | 2.0 | — | 0.08 | 2 primaries, 30 feathers | Talons | 0.3 |
| Common loon | 4.5 | Small | 2.5, fishy | — | 0.1 | 30 feathers | — | 0.4 |
| Bald eagle | 4.7 | Small | 2.0, tough | — | 0.08 | 2 primaries, 30 feathers | Talons | 0.3 |
| Trumpeter swan | 11 | Medium | 6 | 0.3 | 0.3 | 60 feathers, down 0.2 | 10 wing quills | 1.0 |

**No carcass.** Bumblebees, dragonflies, black flies and midges leave nothing; a wood frog becomes its existing item. Fish are covered in Fish bodies, below.

### Special handling

- **Bear meat and trichinella.** At death, 25% of bears carry trichinella, the existing raw-bear chance, rolled once per carcass. Nothing shows. Every cut of that bear, its organs and its fat carry the flag, so all bear meat follows the existing rule: Done for twice the time, or stewed; freezing never cures it. Fat rendered on a Hot fire for 1 game hour is safe.
- **Hare liver spots.** In the crash year of the hare cycle, 3% of hares show white spots on the liver at the gut step: tularemia. The card says "Spotted liver. Leave it." Eating any part of that hare gives Fever 3–5 days later 50% of the time; gutting it bare-handed with a cut finger, 10%.
- **Beaver tail.** The tail is scaly skin over fat and gristle. Singed over coals for 10 game minutes the skin blisters off, leaving 0.2 kg of fat and 0.8 kg of tail meat. It is the beaver's fat; skip it and a beaver is lean food.
- **Castor or oil.** At the gut step the card shows two pairs of sacs. The paler pair is castor, the trap lure. At practice 0 a cut into an oil sac (10%) taints the castor, which then lures at half strength. The oil sacs dress boots: wet resistance of the feet zone +20% for 10 game days.
- **Beaver incisors.** The 4 orange incisors make a carving gouge for spoons and bowls (Creative concepts › 7. Crafts for their own sake) at 1.5 times knife speed.
- **Porcupine.** Singe it in the fire (5 game minutes, existing) and the quills burn off. Skin it from the belly instead (20 game minutes) and 200 quills come off whole. Carried bare-handed, a porcupine puts 2–5 quills in the hand 30% of the time: −2 Condition and hand work ×1.2 until pulled, 1 game minute per 5 quills. Mittens or gloves stop it.
- **Birds: pluck or skin.** Plucking keeps the feathers and the fatty skin; skinning is 4 times faster and loses both (existing choice). Ducks and geese give their down in a second 2-minute pass after the feathers. A grouse's crop tells what it fed on and where: "Crop full of birch buds." That points to the birch slope where the flock feeds.
- **Rank meat.** Mink, marten, wolverine, otter, raven, loons, gulls and osprey eat at full kcal but cost Spirits −1 per meal unless stewed. The tamed wolf eats them at full value and is glad of them.
- **Musk on the hands.** Dressing a wolverine or mink bare-handed leaves musk on the hands: scent strength +1 around the player for 1 game day, until washed as for bloody hands.
- **Caribou hide by date.** From June 1 to August 14, when the warble larvae cut out, a caribou or reindeer hide carries 30–60 holes: pelt grade −40%, bedding and rawhide only. From August 15 to September 30 it is the best clothing hide in the valley, with short dense hair. From November 1 the hair breaks easily: −20%. This is the settled rule (Minecraft development › Settled across tabs, S13), carried by `yearofice:pelt_grade`; Land animals in depth › Caribou bot flies and Birds and the air › Flies on the caribou point here.
- **Moose antlers by date.** In velvet until August 25 – September 5 (no use), then hard until shed: prime bulls shed December 1 – January 15 and young bulls January 15 – March 31 (Land animals in depth › Seasonal looks; Bones and antlers on the land).
- **The bison ear tag.** Every bison carcass gives its numbered tag at the skin step, logged as in Wood bison ("Bison 14, cow, taken day 240"), and the herd count drops by one in the notebook.
- **Brains tan hides.** One animal's brain counts as the "0.5 kg lard or brain" of smoke-tanned leather for its own hide (Crafting and building in depth). It spoils like liver.
- **Blood.** Kept blood (`yearofice:blood`) spoils like liver and gives 800 kcal per kg cooked into blood soup or, with flour, into blood pancakes. It is never drunk raw.
- **Marrow bones.** Cracked beside a fire (2 game minutes per bone), each kilogram of bone gives 0.1 kg of marrow fat at 7,000 kcal per kg, or goes whole into bone broth (Ideas from viewers).
- **Ravens are not game.** Killing a raven ends the camp raven bond and every raven lead for 60 game days, and costs Spirits −3. Killing a songbird, a pika or a bat gives nothing and costs Spirits −1, once a day.

### Fish bodies

**A caught fish stays whole until it is cleaned.** Landed, it is one item of its species carrying its own weight, freshness and body state, as in the video. The cleaning steps and the parts are those of Handling the catch; this adds size handling, rigor and what a whole fish does while it waits.

| Size | Examples | Carried | Cleaning |
| --- | --- | --- | --- |
| Under 0.1 kg | Stickleback, lake chub, slimy sculpin, Alaska blackfish, small least cisco | Pack item; stacks to 20 by species and freshness band | No gutting step: eaten whole, fried or dried whole (dried whole: 1 day on the rack), or used as bait |
| 0.1–3 kg | Grayling, round whitefish, ciscos, Dolly Varden, char, sucker, small burbot | Pack item, one per slot | Handling the catch steps |
| 3–12 kg | Pike, chum, coho, sockeye, lake trout, sheefish, burbot, broad whitefish | Pack item, one per slot; its weight counts | Handling the catch, +1 game minute per kg over 3 kg |
| Over 12 kg | Chinook over 12 kg, Old Hookjaw, the Slough Log, the Lake Mother | Hand-carried through the gills, like a medium carcass; or lashed to the pack frame | As above; a cleaning spot is worth walking to |

**Special bodies.** An arctic lamprey has no gutting step and goes whole into rendering (Waters and fish). A Yukon floater is shucked, not cleaned. A spawned-out salmon is a `yearofice:salmon_carcass`, never food (Spawned-out salmon feed the valley).

**What happens while it waits.**

- **Alive.** A fish on a stringer is alive and does not spoil for 12 game hours (existing). Out of water it dies in 2 game minutes unless struck (One blow for a fish).
- **Rigor.** A dead fish stiffens 1 game hour after death, or 2 if bled and below 5 °C. It stays stiff 12 game hours at 10 °C and 24 near 0 °C. Filleting in rigor takes 1.5 times as long and the flesh share falls from 55% to 50%, as fillets tear. Cleaned before rigor or after it, the fish gives the full share.
- **Belly burn.** An ungutted fish held above 10 °C for over 12 game hours digests its own belly wall: flesh −10% and its spoilage k falls from 0.6 to 0.4.
- **Freezing.** Below −10 °C a whole fish freezes in an hour and stacks like cordwood (existing). Frozen, it skips rigor entirely.

**Fish found dead.** High-graded salmon leftovers are fillet meat for 6 game hours (Ideas from viewers). Winterkill bodies rise at ice-out, bait at best (Waters and fish › Winterkill). An eagle flushed off a fish on the ice drops it 30% of the time.

### Carcasses in water

**Bodies float or sink by kind and stage.** This matters for the duck shot on the slough, the beaver shot at its lodge and the moose shot standing in a pond.

| Body | In water | Drift | Getting it out |
| --- | --- | --- | --- |
| Tiny and small birds, beaver, muskrat, otter | Float | Drifts with the current at 60% of its speed; lodges on snags tagged `yearofice:snags` | Wade, reach with a 3 m pole (hold use 5 real s within 3 m), or send the Companion wolf with Come within 40 m |
| Other tiny and small mammals | Float for 1 game hour, then sink | As above, then on the bed | Wade in water under 1 m |
| Medium mammals | Sink in water over 1 m deep | On the bed | Drag rope from the bank within 10 m: 30 real s per metre, 150 kcal |
| Large and huge | Lie where they fell in water under 1.5 m; sink deeper | Do not drift until bloated | Dressed in the water, or rope as above for large; a huge one sunk deep is out of reach |
| Any bloated body | Rises and floats when water is above 4 °C | Drifts and lodges | Not food |

**A carcass in cold water cools fast.** Its τ is ×0.3, so a moose lying in a 6 °C creek keeps longer than one on the bank. Every step done in water takes twice as long, and the hands follow the cold-water and wet rules of Swimming and working in water.

**Under ice.** A body that sinks before freeze-up stays down until breakup, then rises bloated and drifts. These are some of the spring carcasses on the bars.

### Carcasses in the world

**The player finds bodies they did not make.** These sources already exist and use this entity: Winter's dead, the Winter-killed moose rare event (Open world in depth), the Drowned caribou find (Items), spawned-out salmon and winterkill (Waters and fish), predator kills (Who hunts whom) and staged predator kills (Water, calls and encounters). The rules below add how kills by other animals look, and a few new causes.

| Source | Where and when | What is left | What it tells |
| --- | --- | --- | --- |
| Wolf kill, large or huge prey | Where a pack's daily kill lands within 2 km of the player; most in Nov – Apr | 40–70% eaten at the first meal; the rest frozen in winter. The pack returns every 1–2 nights until it is gone, 6–12 days | Tracks in a ring, blood on the snow, ravens. Scavenging between visits is the big winter food windfall, and the pack's schedule is the risk |
| Lynx kill of a hare | Hare thickets, any month | Fur tufts, the feet and a smear of blood | A lynx works this thicket: set a cubby |
| Fox cache | Under 5 cm of snow or moss within 300 m of a fox range | A whole grouse, hare or vole, fresh for 1–3 game days | The video's dead grouse on the trail. The fox comes back at dusk; finding it gone, it caches 200 m farther on next time |
| Raptor plucking post | A stump or rock where an owl or the gyrfalcon ate a grouse or ptarmigan | A ring of feathers, no body | Which owl or falcon hunts here |
| Bear kill | Moose calves in June, salmon in August, carrion | Covered with soil and sticks, a bear bedded within 30 m (existing) | Leave. The existing "never contest a bear" rule applies |
| Rut fight | October; 1% per region per rut | A bull moose dead of goring, 0–2 days old | Antlers worth having. Once in 10 runs, two bulls dead with locked antlers: a rare sighting worth Spirits +5 |
| Fall from a ledge | Sheep and goat ranges, March – May avalanche season | A sheep or goat at the foot of a chute, frozen or fresh | The meat loss of a fall applies; eagles find it first |
| Wildfire | Inside a burned area, the week after a fire | A charred carcass: hide lost, outer 20% of meat burned, the rest cooked and good for 2 game days | Free meat in a black forest, for whoever comes first |

**Caps.** Found carcasses come only from deaths the population model already counts, so nothing extra dies. At most 6 found carcasses per region at once, and at most 2 large or huge.

### Scavengers and the order of eating

**The existing succession sets who comes and when; this sets what they eat.** Each scavenger takes from the damage map and the kill pile in its own order. That is why a carcass found after 2 days reads like a story.

| Scavenger | Arrives | Eats, in order | Rate | Sign |
| --- | --- | --- | --- | --- |
| Blowflies | Within 1 game hour above 10 °C | Eggs on eyes, mouth and wounds; maggots after 24–48 game hours | Speeds Rotting ×1.3 on uncovered meat | A buzzing cloud heard at 10 m |
| Ravens, 2–6 | 0–2 game hours (existing) | Eyes, then the gut pile, then wound edges and exposed meat | 1 kg per raven per day (existing) | Calling and circling, seen at 1 km |
| Black-billed magpie (new), 1–6 | 1–4 game hours, after the ravens | Gut pile, kill-pile scraps; carries pieces off to cache | 0.2 kg per bird per day | Chattering heard at 150 m |
| Gray jays, chickadees | Daytime, all year; chickadees in winter only | Jays: 50 g of meat per visit (existing). Chickadees: 5 g of fat per visit | Small | Jays carry pieces away; chickadees cling to the ribs |
| Bald or golden eagle | Day; bald eagles Nov – Mar at open water and on lake ice, golden Mar – Oct | Exposed meat on open ground or ice | 0.5 kg per day | A big bird that lifts off heavily at 100 m |
| Fox, marten, ermine | 6–12 game hours, at night (existing) | Organs, soft hindquarter, then the kill pile | 0.5 kg each a night (existing) | Small tracks and chewed edges |
| Wolves | 12–36 game hours, Nov – Apr (existing) | Hindquarters, organs, ribs; they break leg bones for marrow | 8 kg per wolf per visit (existing) | Trampled snow, crushed bones |
| Wolverine | With wolves or alone, any month | Drags off 5 kg pieces and caches them (existing); eats frozen meat and hide | As existing | Wide loping tracks; a carcass moved 10–50 m |
| Bears | Within 24 game hours, Apr – Oct (existing) | Everything, hide included; covers the rest and beds within 30 m | 15 kg a day (existing) | A mound of soil and moss |
| Voles, shrews, red squirrels | Days later, all year | Fat and scraps on the frame; later the bones | Small | Gnawed bone ends |

**What a found carcass looks like.** The model hides the bones of eaten regions and swaps in an opened texture, so a wolf kill shows its missing hindquarters from 20 m. The cause line adds who fed: "Wolves fed here, two nights ago. A wolverine since."

**The black-billed magpie (new species).** A long-tailed black-and-white corvid along the main river from the C3 and D3 bars to Station Nine: 0–6 per region, all year, day only, `yearofice:black_billed_magpie`. It follows ravens to carcasses and steals 30 g per visit from uncovered drying racks with nobody within 30 m. It habituates like a raven. Birds and the air › Woodpeckers and corvids extends this sheet with its hitbox, nest, sounds and camp thefts; this is its only id (RECONCILE 45, 57). Its chatter at a carcass is heard at 150 m, a nearer and noisier clue than the ravens' call.

### Bones and antlers on the land

**Bones stay, and the land fills with readable remains.** When a carcass reaches Bones, it becomes a `yearofice:bone_pile` block for its body plan, with the skull, antlers or horns in place.

- **Bone piles last 2 years.** They grey, scatter over 2 m in the second year, then vanish. Each pile can be searched once (hold use, 1 game minute) for 1–4 bones, the skull, and any antlers or horns.
- **Antlers and horns last 4 years.** From the second year, rodents and porcupines gnaw them: value halved, and an antler tine still serves for pressure flaking (Mechanics check: hunting).
- **Shed antlers.** Each bull moose sheds its pair inside its winter yard, prime bulls December 1 – January 15 and young bulls January 15 – March 31, the two antlers 0–200 m apart. Under snow they appear at melt; April and May are shed-hunting season. Each is a `yearofice:shed_antler` block with the bull's antler size that drops the item `yearofice:antler` with a species component (Conflicting ids row 10), worth logging as sign of where the big bulls winter.
- **Skulls.** A skull from a bone pile or a dressed head goes on a shelf as a trophy (Creative concepts › 8. The cabin collection, Skulls and Sheds and horns sets). A wolf skull is one of Whitlock's specimens.

### Carcasses as bait and as sign

**As bait.** A carcass or gut pile is a bear bait station (Setting up the hunt). In winter, up to 6 snares within 20 m of one get bait ×2 for wolves, wolverines and foxes: the trapper's carcass set. Bones draw nothing.

**As sign.** Its stage dates it, the eaten regions name the feeders, and the tracks say who comes back. Ravens lead to it (Animals that help the player; Raven leads in Water, calls and encounters). The messenger never lists carcasses the player did not make.

### Carcasses and the camp

**A body near the bed is a promise to the bears.** Each carcass is a scent point that bears path to within 400 m (Bear rules), with a strength by class and stage.

| Class | Fresh | Bloated | Rotting |
| --- | --- | --- | --- |
| Tiny | 0 | 0.2 | 0.3 |
| Small | 0.5 | 1 | 1.5 |
| Medium | 1 | 2 | 3 |
| Large | 2 | 4 | 6 |
| Huge | 3 | 6 | 9 |

A gut pile adds the existing triple strength, and a covered or hung carcass halves its point.

**Rules at camp.**

1. **Bears.** A carcass a bear finds within 100 m of a camp counts as food found there, so the camp's nightly bear-visit chance rises 15 points (Camp bears in How animals act). A bear that covers it beds within 30 m, which may be beside the cabin door.
2. **Flies.** A carcass above 10 °C within 30 m of camp adds +0.1 to bug pressure B there and makes fresh meat on open racks fly-blown 2 times faster.
3. **Smell.** A rotting carcass upwind within 30 m costs Spirits −1 a day.
4. **Hanging small game.** A hare or grouse hung whole in its skin on the meat pole keeps at the meat-pole rate, freezes below −5 °C, and in winter can wait weeks before skinning, thawed first by the existing rule.
5. **The wolf.** A Companion wolf unattended near a carcass eats 1 kg per game hour of it. Told to Guard it, it keeps ravens, magpies and foxes off and howls when a bear or wolverine comes.
6. **The morning prompt.** The existing scent\_by\_bed line now covers carcasses the player left: "A hare carcass 12 m from the bed last night. Fox tracks at the woodpile."

### In Minecraft: carcasses

**One base class, five entity types.** The carcass is `com.yearofice.ecosystem.carcass.CarcassEntity extends Entity implements GeoEntity`. It is not a `LivingEntity`, so it has no health, hurt time, knockback or AI by construction. Each size class registers its own type so hitboxes and caps differ.

| Entity type | Hitbox (blocks) | Per chunk | Notes |
| --- | --- | --- | --- |
| `yearofice:carcass_tiny` | 0.3 × 0.15 | 6 with small | Pocketed into `yearofice:whole_game` |
| `yearofice:carcass_small` | 0.5 × 0.3 | 6 with tiny | Hands, brace, game bag |
| `yearofice:carcass_medium` | 1.0 × 0.5 | 2 with large and huge | Hands, shoulders, frame |
| `yearofice:carcass_large` | 1.6 × 0.8 | 2 with medium and huge | Drag rope and vehicles |
| `yearofice:carcass_huge` | 2.6 × 1.2 | 2 with medium and large | Roll only |

All five sit in the entity tag `#yearofice:carcasses`, with `yearofice:salmon_carcass` from Waters and fish. The `yearofice:carcass` id named in Every concept from the video is this tag and base class, not a sixth type.

**Death to carcass.** `ecosystem.wound.DeathPipeline` runs the collapse animation for 20–40 ticks with AI off, then `CarcassFactory.fromAnimal` builds the record and spawns the carcass of the species' class. The living entity is discarded without vanilla's death tick, drops or experience (No vanilla mob mechanics, below). Abstract-tier deaths that the found-carcass rules keep become records in the region file and spawn as entities when their chunk loads.

**The model.** The carcass renderer `CarcassRenderer extends GeoEntityRenderer<CarcassEntity>` reuses each species' own geo model and texture from `assets/yearofice/geo/entity/<id>.geo.json`. It plays one of the species' dead poses, `animation.<id>.dead_side`, `dead_chest` or `dead_wings`, as a frozen single-frame animation.

- **Stage by bone and texture.** Bloated scales the belly bone to 1.15. Skinned swaps to `<id>_skinned.png`. Rotting adds a `_rot` overlay. Removed regions hide their bones (a quartered moose has no `leg_hind_left`).
- **Settle.** Pitch and roll come from the 4 ground heights under the hitbox, applied to the root bone. Once settled, `noPhysics` is set and the entity ticks no movement.
- **Bones.** At Bones the entity is removed and places `yearofice:bone_pile` with block state `body_plan` (cervid, bovid, ursid, canid, mustelid, rodent, lagomorph, bird) and `age` 0–3. Eight skeleton models cover every species.

**The record.** The carcass's server data is the entity attachment `yearofice:carcass`, a codec-backed `CarcassRecord`. The `core_temp` named in Ideas from viewers is its `core_temp_c` field (RECONCILE 41, 101). The client gets only what it draws, by `SynchedEntityData`: species, pose, stage, bloat, skinned flag, hidden-region mask, frozen flag.

**Carried and lashed.** A carcass in the hands or on the shoulders is the existing `yearofice:carry` attachment holding the `CarcassRecord` and a mode. The entity is removed while carried and rebuilt on set-down, so a carried carcass costs nothing on the server.

- **First person.** `RenderHandEvent` cancels the hands and draws the species model in a `carried_arms` or `carried_shoulders` pose with GeckoLib's object renderer.
- **Third person.** A render layer added in `EntityRenderersEvent.AddLayers` draws the same pose on other players' models.
- **Pocketed and lashed.** Tiny and small carcasses become the item `yearofice:whole_game` with the data component `yearofice:carcass` (the same codec). A brace on a lash point renders from the pack frame's Curios renderer.

**Spoilage and lifecycle.** At death the carcass's `yearofice:fresh` is stamped on the Decay and freshness layer (`DecayService.stamp`, with the carcass k and size factor) and settles lazily when read, as all food does (RECONCILE 70); nothing ticks it. `CarcassLifecycle` reads it for the stage and flags, so an unloaded carcass is already right when its chunk loads.

**Scavenging.** `com.yearofice.ecosystem.Scavenging` applies the eating order to the damage map. Within 160 m of a player it stages the real feeders as Active entities, under the 40-animal cap; farther out it works on the record alone.

**The species data.** Each species file in `data/yearofice/species/<id>.json` gains a `carcass` block, and the M6 data check fails if it is missing.

```json
"carcass": {
  "whole_kg": 18, "class": "medium", "body_plan": "rodent", "tau_h": 2, "floats": true,
  "poses": ["dead_side", "dead_chest"],
  "steps": { "bleed": 2, "gut": 10, "skin": 25, "joint": 20, "bone_out": 10 },
  "parts": {
    "meat": 6.5, "fat": { "fall": 0.2, "spring": 0.05, "at": "skin" },
    "organs": { "heart": 0.08, "liver": 0.2, "kidney": 0.02 },
    "cover": { "item": "yearofice:beaver_pelt", "kg": 1.5 },
    "extras": [
      { "item": "yearofice:beaver_tail", "kg": 1.0, "at": "skin" },
      { "item": "yearofice:castor", "count": 2, "at": "gut", "risk": "oil_sac" },
      { "item": "yearofice:incisor", "count": 4, "at": "joint" }
    ],
    "guts_kg": 2.0
  },
  "rules": ["beaver_tail", "castor_or_oil"]
}
```

**Loot tables per step.** `data/yearofice/loot_table/butchering/<id>.json` keeps the Bestiary's one file per species. Each pool carries the condition `yearofice:dressing_step` ("bleed", "gut", "skin", "joint", "bone\_out", or "clean" for tiny), so one file holds every stage. Two functions finish each roll:

- `yearofice:carcass_condition` scales the count by the clean share of the region the pool names, and marks tainted or sour cuts on their `yearofice:fresh` component.
- `yearofice:copy_freshness` gives every part the carcass's freshness; the existing `yearofice:season_fat` and the fur trade's `yearofice:pelt_grade` stay as they are.

**Performance caps.**

- **Per chunk.** 6 tiny and small carcasses and 2 larger ones. A new carcass over the cap turns the oldest tiny or small one into a fur-tuft or feather decal and drops it to a record.
- **Per region.** 24 carcass entities loaded at most, and 48 records. Records in the Bones stage go first.
- **Cost.** A settled carcass ticks only a 20-tick water check and the hourly lifecycle step: 64 loaded carcasses must add under 0.5 ms per tick.
- **Despawn.** Carcasses never despawn by distance; they leave only by being carried, eaten, or turning to bones.

**Ids added.** Entities `yearofice:carcass_tiny`, `carcass_small`, `carcass_medium`, `carcass_large`, `carcass_huge`, `yearofice:black_billed_magpie`. Blocks `yearofice:bone_pile`, `yearofice:shed_antler`. Items `yearofice:whole_game`, `blood`, `beaver_tail`, `castor`, `oil_sac`, `incisor`, `brain`, `marrow_bone`, `skull`, `antler`, `horn`, `claw`, `canine_tooth`, `talon`, `quills`, `down`, `musk_gland`, `giblets`, `heart`, `liver`, `kidney`, `tongue`, `bison_tag`; organs and skulls carry the component `yearofice:species`. Attachment `yearofice:carcass`. Data component `yearofice:carcass` on `whole_game`; the fish-body fields (alive, bled, gutted, rigor, belly burn) join `yearofice:fish_data` (Conflicting ids row 8), so `fish_body` is not a separate component. Loot condition `yearofice:dressing_step`; loot functions `yearofice:carcass_condition`, `yearofice:copy_freshness`.

**Java.** `com.yearofice.ecosystem.carcass`: `CarcassEntity`, `CarcassRecord`, `CarcassFactory`, `CarcassLifecycle`, `DamageMap`, `CarryHandler` (inputs, modes, speed, noise), `CarcassWater` (float, sink, drift), `FoundCarcasses`, `BonePile`; `hunt.butcher.Dressing` (steps, tools, the time formula); `ecosystem.Scavenging`; client `CarcassRenderer`, `CarriedCarcassLayer`, `CarcassCardScreen`.

**Milestones.** M5: the carcass entity for tiny, small and medium with the small-game species; pocket, hands, shoulders and frame; dressing steps 1–4; fish bodies. M6: large and huge, drag rope and vehicles, the damage map, bone out, scavenging and the eating order, found kills, scent by class. M7: rigor and freezing by season, under-ice bodies, shed antlers, bone piles by year, winterkill bodies, the magpie.

### Carcass GameTests

Each is a NeoForge `@GameTest` in `com.yearofice.gametest.CarcassGameTests`.

1. **No drops, one body.** For every type in `#yearofice:animals`, a test arrow kill leaves zero item entities and zero experience orbs within 8 blocks after 40 ticks, and exactly one carcass of the species' class.
2. **The beaver is heavy.** A player with a 10 kg pack picks up a beaver with sneak and use and walks at 85% ±2% of walking speed; with an empty pack at 100% ±2%. A bull moose cannot be picked up and the prompt reads "Too heavy: dress it here."
3. **The clock by temperature.** Unopened at a constant 15 °C, a hare bloats at 27–31 game hours and a bull moose at 16–18. Gutted, a moose reaches Turning at 46–50 hours. At −10 °C, both are unchanged after 30 game days.
4. **Yields add up.** A clean heart-and-lung bull moose dressed through all steps on time gives 180 kg of meat, 3 kg of fat, 8.3 kg of organs and a 30 kg hide, each within 0.1 kg. With a gut hit it gives 144.0 kg of clean meat and 36.0 kg tainted, never less.
5. **The eating order.** A September moose carcass with no player, near a bear range: ravens arrive within 2 game hours on at least 90% of 1,000 runs, no fox or marten feeds before 6 game hours, and 55–65 kg is gone after 3 days, matching Carcass succession.
6. **Caps hold.** Ten small carcasses created in one chunk leave at most 6 entities and the rest as records; 64 loaded carcasses add under 0.5 ms per tick.
7. **Bones last.** A bull moose carcass left alone reaches a `bone_pile` with antlers; the pile is gone after 730 game days, while the antlers stay to 1,460 and lose half their value after 365.
8. **Fish wait whole.** A caught 4.0 kg pike stays one item weighing 4.0 kg until cleaned; cleaned in rigor it takes 1.5 times as long and gives 2.0 kg of flesh instead of 2.2.
9. **Float and sink.** A mallard shot over 2 m of moving water floats and lodges on a snag block; a caribou shot in 2 m of still water sinks and rises only after bloating above 4 °C.

## No vanilla mob mechanics

**No Minecraft mob rule survives in Year of Ice.** Our animals are not reskinned cows: they never drop loot, flash red, heal themselves, breed on wheat or follow a held carrot. The Minecraft engine tab already removes vanilla mobs (Vanilla switched off, No monsters); this section removes vanilla mob behaviour from our own animals too, and names what replaces each rule.

**How it is done, in one paragraph.** Every animal extends `YoiAnimal extends PathfinderMob`, never `Animal`, `AgeableMob`, `TamableAnimal` or `NeutralMob`, so breeding, taming, anger and growth code is absent by construction. Behaviour is our own `Brain` (Minecraft engine › Animals) with no vanilla goals. Damage to animals is cancelled in `LivingIncomingDamageEvent` and handed to the hit-zone resolver, which decides wounds and death. All animals register in `MobCategory.CREATURE`, so Peaceful difficulty never deletes a bear.

### The list

| # | Vanilla behaviour | In Year of Ice | NeoForge mechanism |
| --- | --- | --- | --- |
| 1 | Items drop the moment a mob dies | A whole carcass (Carcasses and bodies) | `LivingDropsEvent` cancelled for `#yearofice:animals`; entity loot tables kept as empty files so data packs cannot fill them; game rule `doMobLoot` false |
| 2 | XP orbs from kills, breeding, fishing, ores and furnaces; the XP bar and levels | Practice levels per action and the journal | `LivingExperienceDropEvent` set to 0; `BlockDropsEvent` experience 0; `PlayerXpEvent.XpChange` and `PickupXp` cancelled; `minecraft:experience_orb` in `yearofice:banned_vanilla`; XP bar removed (HUD layers) |
| 3 | Red hurt flash, hurt sound and knockback on every hit | The animal reacts by hit zone: hunch and kick, limp, run (The hit-zone model); blood drops, no tint | Damage cancelled before vanilla applies it, so `hurtTime` and the hurt event never fire; `LivingKnockBackEvent` cancelled; GeckoLib overlay fixed at no-hurt |
| 4 | Invulnerability frames after a hit (10 ticks) | Two arrows a second apart both count | `invulnerableTime` held at 0 on our animals |
| 5 | Health bars and damage numbers; bigger mobs simply take more hits | Kill thresholds in joules and hit zones; no health is shown or used | Health attribute held at max; death only through the hit-zone resolver or the mod's own causes (drowning, starvation, falls, fire, lightning, old age); `/kill` and `/damage` reach the death pipeline with cause `command`, so test tools still work |
| 6 | Health regenerating by itself (wolves on food, horses over time) | Wounds that last: 10–20 game days to heal, with a limp in the tracks | `LivingHealEvent` cancelled unless the source is `WoundHealer` |
| 7 | The player's hearts regenerating from a full food bar | Condition and injuries in the body model | `naturalRegeneration` false; `LivingHealEvent` cancelled on players; health held at 20 (Vanilla needs replaced) |
| 8 | The death tip-over, red tint and puff of smoke | A 1–2 s collapse into a death pose | `die` and `tickDeath` overridden and each renderer's `getDeathMaxRotation` returns 0, so vanilla's tip-over never plays; entity event 60 (death smoke) never broadcast |
| 9 | Breeding by feeding wheat, seeds or carrots; love hearts; babies in 5 minutes | Births on the calendar from the population model; tame wolves breed in February (Taming a wolf); homestead goats and hens by their own season | No `Animal` subclass, so no `isFood` or love mode; `BabyEntitySpawnEvent` cancelled for any non-mod cause |
| 10 | Babies growing up in 20 minutes | Real growth: calves to 0.3 of adult by October, adults at 2–4 years by species | Age class in the record, stepped by the calendar; no `AgeableMob` |
| 11 | Animals following a player who holds their food | Lured state by bait, scent and calls; carrying fresh meat draws bears and wolves instead | No `TemptGoal`; the Brain has no tempt sensor |
| 12 | Panic running in random directions when hit | Flee to cover for the species' flee distance, away from the threat | No `PanicGoal`; the Flee activity |
| 13 | Leads on any passive animal; fence-post tethers | Cordage leads only the Companion wolf, harnessed wolves, livestock, raised caribou or moose calves with bond 60 or more, and towed vehicles (travois, hide drag, sled). No other animal takes a lead | Only those types implement `Leashable`; `minecraft:lead` recipe hidden; lead use on `#yearofice:wild` cancelled in `PlayerInteractEvent.EntityInteract` |
| 14 | Saddles and riding wild animals (horses, pigs, striders) | No mounts. Wolves carry packs and pull sleds; the player rides only the canoe and raft | `EntityMountEvent` cancelled unless the vehicle is in `#yearofice:vehicles`; no `Saddleable`; saddle item hidden |
| 15 | Taming by feeding bones, with instant hearts | Wolf trust over 32+ game days (Taming a wolf) | `minecraft:wolf` banned; `yearofice:wolf` has its own trust record |
| 16 | Mobs despawning when the player walks 32–128 blocks away | Tier demotion: the animal becomes a Nearby or Abstract record and the same animal returns | `removeWhenFarAway` false; `checkDespawn` empty; wild animals return `shouldBeSaved` false, so a demoted body is never saved twice; only livestock, raised young, tamed wolves and carcasses set `requiresCustomPersistence` |
| 17 | Spawning on grass by light level, in darkness, at chunk generation, from spawners | Fixed population slots drawn from region counts | `doMobSpawning` false; `neoforge:remove_spawns` biome modifier; `FinalizeSpawnEvent` cancels any spawn not flagged by `PopulationSpawner` |
| 18 | Spawner blocks, trial spawners, spawn eggs | None in survival | No vanilla structures; spawner, trial spawner and every spawn egg hidden from survival tabs and loot |
| 19 | Mob farms: kill chambers, auto-collection, breeding pens | Nothing to farm. Animals come from K and the birth calendar; killing at one spot empties it | No item drops for hoppers to collect; populations are the only source; trap rolls read range overlap, not trap count |
| 20 | Name tags make a mob permanent | "Name it" for a habituated animal (How animals act) and Known animals; every animal is already permanent | Name tag hidden; use on entities cancelled |
| 21 | Mob heads and skulls as rare drops | Skulls, antlers and horns from dressing and bone piles | Vanilla head and skull items hidden; no loot table gives them |
| 22 | Villagers, wandering traders, iron and snow golems | Nobody lives in the valley. People are voices on the radio | All in `yearofice:banned_vanilla`; golem patterns spawn nothing that survives `EntityJoinLevelEvent` |
| 23 | Trading for emeralds | The fur trade and catalogue points on landing drops (Creative concepts › 11. The fur trade) | No `Merchant` entities; emeralds and emerald ore never generate |
| 24 | Neutral-mob anger: wolf packs turning on an attacker, mother polar bears | Awareness, Warn, Charge and the species charge rules | No `NeutralMob`; `universalAnger` false |
| 25 | Random ambient sounds every few seconds | Calls by schedule, state and season (Animal sounds) | `playAmbientSound` does nothing and `getAmbientSoundInterval` is set very high; sounds played by the Brain through `SoundBus.play` |
| 26 | Animals killed by fire drop cooked meat | A charred carcass with hide lost and 20% burned (Carcasses in the world) | Rule 1; the fire cause written to the record |
| 27 | Fall damage by block height for every mob | Animals avoid edges; a fall over 6 m wounds, and the carcass fall rule applies to the dead | `LivingFallEvent` cancelled for our animals; `FallRules` writes the wound |
| 28 | Entity cramming: 24 mobs in a block start dying | A bison herd can stand packed in a winter yard | Game rule `maxEntityCramming` 0 |
| 29 | Mobs picking up items off the ground | Theft by named rules only: jays, ermine, magpies, wolverines and bears take food | `canPickUpLoot` false; theft runs on server rules |
| 30 | Damage to the player scaling with difficulty | One body model for everyone; presets set the realism | Every mod damage type uses `"scaling": "never"`; the server calls `setDifficultyLocked(true)` at Normal on start, because a world preset cannot lock it |
| 31 | Vanilla fishing loot: treasure, junk, XP | Fish from stocks as whole bodies (Fish bodies) | Vanilla rod hidden; `ItemFishedEvent` cancelled; fishing loot tables emptied |
| 32 | Mob caps: 70 hostile and 10 passive per player | 40 Active and 200 Nearby animals | The tier manager's caps (Minecraft engine › Animals) |

**New game rules for the world preset.** `doMobLoot` false and `maxEntityCramming` 0 join the engine's game-rule table; `universalAnger` stays false. `doEntityDrops` stays true, because canoes and sleds drop their contents when broken. Dropped items last 7 game days and the `left_pack` block never expires (RECONCILE 89). Rows 33–50 (breath, suffocation, shoving, boats, enchantments, potions, sweeping, eggs and snowballs, lightning, look goals, bush damage, path types, buckets, shears, dropped items, baby size, tracking ranges and spawn eggs) are in Minecraft development › No vanilla mob mechanics: the audit, with its no-vanilla test 10.

### No-vanilla GameTests

Each is a `@GameTest` in `com.yearofice.gametest.NoVanillaGameTests`; together they cover every row.

1. **A body only (rows 1, 2, 8, 26).** Each animal killed by arrow, fire and command leaves zero item entities, zero experience orbs, no death smoke and one carcass. Player XP stays 0 after a game day of mining, cooking and fishing.
2. **No flash, no shove (3–5).** Two arrows 20 ticks apart into a hare and a moose both reach the hit-zone resolver; `hurtTime` stays 0, knockback velocity 0, health at maximum.
3. **No self-healing (6, 7).** A leg-wounded wolf and a player down 10 Condition, fed and rested a game day: only `WoundHealer` moves the wound; player health stays 20.
4. **No breeding, tempting or growing (9–12).** A moose, a hare and a grouse beside a player holding every vanilla food for 6,000 ticks: no love state, no new entity, no approach inside their flee rules, no age change.
5. **No leads, saddles, tags or taming (13–15, 20).** Each item used on each wild type attaches, mounts, renames and tames nothing.
6. **Nothing despawns or spawns (16–19).** An Active moose with the player 300 m away becomes a Nearby record and the region count holds. Dark and lit 32 × 32 grass platforms stay empty for a game day. 200 generated chunks hold no spawner.
7. **No people, trade or heads (21–23).** A summoned villager, trader and golem are removed on join; no loot table names a head, skull item or emerald.
8. **No anger, ambient chatter, falls or cramming (24, 25, 27, 28).** Pack wolves charge only by their own rules after one is shot; no off-schedule ambient sound in a game hour; a hare dropped 10 m takes no vanilla damage; 30 bison in a 2 × 2 pen take none.
9. **No pickups, scaling or vanilla fish (29–32).** Animals cross 20 dropped items and take none; with difficulty forced to Peaceful, Easy and Hard, a bear attack costs the same Condition and the bear stays; a vanilla rod catches nothing; Active animals never pass 40.

**Review audit (Hunting: from the first snare to the first moose).** Reviewer E traced the loop as a player plays it. Keys follow Minecraft development › Player systems and client: Shift stalks and crawls, right mouse holds work and draws the bow, left mouse strikes, X reads sign, G whistles, Q drops, V talks and opens the voice wheel. Status: Ready means specified with numbers, an id and a test; Fixed means specified in this round; Open means Ruhan decides.

| Step | Rule and numbers | Rule location | Build | Status |
| --- | --- | --- | --- | --- |
| 1. First snare | 1 m cordage, set on a trail cell (×4); dawn and dusk rolls; hare p\_s 0.025 | Blueprint › Traps; Wildlife and fish › Game trails | `food.traps`; `trap` block in `data_maps/item/props.json` | Fixed (paths, keys) |
| 2. Traps while away | Missed rolls settle within 300 m or on save, seeded per trap and roll time; theft after 48 h at 35% a day | Blueprint › Traps › Traps keep working while the player is away | `food.traps` catch-up; same-catches test | Ready |
| 3. Deadfalls | Rock at least 10× the target; 90% kill; mouse deadfalls at caches | Blueprint › Traps | Same | Ready |
| 4. Small game | Stick, rock, blunt; 2 J kills; a blunt on anything over 8 J only angers it | Weapons and arrowheads; The hit-zone model › Small game | `sim.hunt.HitResolver` | Fixed (Video row 181) |
| 5. Weapons | Light bow 40 strength, 22 J; longbow 70, 37.5 J; sinew-backed 85, 50 J; flint, bone, iron heads | Weapons and arrowheads; Mechanics check › The build sheet | `hunt`; `weapon` block in `data_maps/item/props.json` | Fixed (snake\_case, paths) |
| 6. Strength gate | Condition × clamp((BMI − 17) ÷ 6, 0, 1); under the bow's need the draw caps at 60% | How the player hunts › Strength | `sim.hunt.Aim`; 70 / 72 kg test | Ready |
| 7. Knife edge | `yearofice:edge` 0–120; hone to 70, whetstone 100, strop Keen 120 | Mechanics check › Knife edge; Items | Item components | Fixed (was 0–100, hone 80) |
| 8. Scouting and sign | X taps to read and holds to pin; age tables by ground; learned by sighting within 10 game minutes | Tracks and sign | `ecosystem.sign` | Fixed (E to X) |
| 9. Wind and scent | 15° plume; thermals up 06:00–11:00, down 17:00–22:00; fresh meat ×1.5 | Senses by species › Smell | Brain sensors; Scent and sound field | Ready |
| 10. Awareness | Gains 40 / 25 / 15 a second, decay 2 (5 in cover); 30 Alert, 70 response | Senses by species › Awareness; Minecraft development › The shared state machine | `sim.animal.Awareness`, `ChargeGate` | Fixed (one rule) |
| 11. Stalking | Shift at 40% speed; noise × surface; sticks snap on 1% of crouched metres | How the player hunts | Noise into the Scent and sound field | Ready |
| 12. Calls and blinds | Cow moan right mouse 2–4 s, grunt a tap, rake 3–6 s on willow; rut factor by date; blind, stand, bait station, berry patch | Setting up the hunt; Water, calls and encounters | `call.moose_cow`, `call.moose_grunt`, `call.rake` | Fixed (keys; Video row 260) |
| 13. Draw and flight | Hold 6 s, then triple sway; drop table; holdover | How the player hunts | `sim.hunt.Ballistics`; 1.23 m at 25 m | Ready |
| 14. Hit zones | Ellipsoids per pose; moose 30 J, bison 40 J, grizzly 36 J | The hit-zone model; Minecraft development › Hitboxes | `data/yearofice/hitzone/<id>.json`; `ecosystem.wound.HitAdapter` | Fixed (paths, missing thresholds) |
| 15. Wounded flight and blood | Zone table; waits 15 min to 6 h; pushing adds 2 h and halves the trail | The hit-zone model › Blood trail data | `wound` memory, `WoundHealer` | Ready |
| 16. Death | 20–40 tick collapse, no drops; a carcass entity of the size class | In Minecraft: carcasses; Minecraft development › Carcass hand-off | `ecosystem.wound.DeathPipeline` | Ready |
| 17. No vanilla drops | Death tables empty, `doMobLoot` false, rows 1–50 | No vanilla mob mechanics | `NoVanillaGameTests` | Fixed (Land tab's death tables) |
| 18. Field dressing | 8 steps, a moose about 4 game hours at practice 0; deadlines by air temperature | Blueprint › Field care | `hunt.butcher.Dressing` | Fixed (code path) |
| 19. Quarter and haul | 40 kg frame: hinds 4 × 27 kg, fronts 2 × 40, loose 37, hide 30: 8 trips, 9 with the head; boned, 5 loads | Field care › The haul | `ecosystem.carcass.CarryHandler` | Fixed (180 kg bull) |
| 20. Sledding | Sled 120 kg: a winter moose in 2 trips boned, 3 bone-in; hide drag 80 kg at 30% | Field care › Winter hauling | `yearofice:sled`, `hide_drag` | Fixed |
| 21. Yields add up | Bull 450 kg = meat 180 + fat 3 + organs 8.3 + hide 30 + guts 60 + antlers 15 + small parts 9 + blood 27 + frame 118; 238,790 kcal at 1,150 / 7,000 / 1,300 a kg | Parts by species; Blueprint › Butchering and yields rates | `loot_table/butchering/<id>.json` in 100 g units; linter pass 3 check 4 | Fixed here; Blueprint › Butchering and yields still says 103 kg (Food owner) |
| 22. Hide, sinew, bone | Green hide scraped within 3 days above 5 °C; rawhide 2 days, smoke-tan 6; caribou hide by date (S13); bone piles 2 years | Field care › The hide; Caribou hide by date | `yearofice:pelt_grade` | Fixed (S13 restored) |
| 23. Predators at the kill | Bear 4% a game hour (×2 at dusk) April–October within 1.5 km; wolves 3% November–April; 60 kg gut pile dragged 50 m | Field care › The predator clock; Who hunts whom › Carcass succession | `CarcassLifecycle`, `Scavenging` | Fixed (gut pile 25 to 60 kg) |
| 24. Debug and tuning | `/yoi hunt aim`; `/yoi hunt shot <species> <zone> <J>` runs one resolver shot; `/yoi carcass <target> info`; `/yoi trap <pos> info` and `settle`; `[hunt]`: `sway_scale`, `strength_gate`, `blood_fade_h` (2), `trap_catch_scale`, `predator_clock_scale` | This block; RECONCILE 118 | `yearofice-common.toml` | Fixed (new) |

**The hunting year.** May–June: snares, grouse and fish, while calving cows and sows with cubs make big game dangerous. July–August: the 4-hour quarter deadline, flies and yellowjackets at the racks; a first moose is possible from week 3 but hard to keep. September–October: the rut calls, the best moose window, with bears in hyperphagia driving the predator clock. November–March: the trapline loop, wolf tests, frozen carcasses (hatchet, 3× time) and sled hauls; lean March moose make fat the scarce thing. April: bears leave dens lean and bolder. No season is dead. Exploits checked: an unwatched trapline catches exactly what a watched one does, catches are capped by n and `respawn_days`, and harvest takes from N at once, so there is no free food.

**Review audit (Animal life).**

| Requirement | Where | Status |
| --- | --- | --- |
| Drives | Blueprint › Drives and daily schedules (moose reference rates); species files hold the rest. A species with no rates of its own takes its body plan's default: ungulates as the moose; carnivores hunger 0.06, thirst 0.04, rest 0.08; small mammals and birds hunger 0.15, rest 0.05 a game hour | Fixed (defaults) |
| Senses | Blueprint › Senses by species (19 species); `senses` blocks in Land animals in depth and the species sheets here; Birds and the air › flush distance | Ready |
| Population | Blueprint › Populations; Land animals in depth › Spawn rules; Birds and the air › egg pressure | Ready |
| Seasons | Seasonal drives; Land animals in depth › Seasonal looks; Birds and the air › The migration calendar | Ready |
| Who hunts whom | Blueprint › Who hunts whom; Land animals in depth › Scenes between species; each bird sheet's food web | Ready |
| Id, rig, butchering file | Every entity id in this tab, Land animals in depth and Birds and the air is in Minecraft development › The entity registry | Ready |
| Weights against parts rows | An animal's parts scale with its own live weight; Land's caribou and wolf ranges now contain the parts rows | Fixed |
| Wolf taming without bones | Ideas from viewers › Taming a wolf without bones, clamped by Open world in depth's daily trust cap; `yearofice:trust`; G and V commands (Minecraft development › Taming) | Ready |
| Livestock and orphans | Minecraft development › Taming, livestock and raised young; prices from the radio catalogue only (3 hens and a rooster 100, 2 goats 150, 2 reindeer 180); Land animals in depth › Mothers and young (hiders are not orphans) | Ready |
| One owner per insect and the magpie | Land owns ground nests and the `yellowjacket` entity; Birds owns flying insects, `yellowjacket_swarm` and `mourning_cloak`; one `black_billed_magpie` | Fixed |
| Plant–animal hooks | Browse and brooming, hare girdling, push-up tubers, seed rain, sapsicles, sap-bucket raids, bear scat seeding and goat toxicity live in Plants and fungi › Animals and plants; Land and Birds link there | Fixed (RECONCILE 67, 130) |
| Speeds | Player 3.0 / 5.5 / 1.0 m/s; swim moose 1.8, caribou 1.6, grizzly 1.5, black bear 1.4, wolf 1.2 | Fixed |
