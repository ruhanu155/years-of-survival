# Land animals in depth

This tab is the build sheet for every ground animal: 35 existing species and 18 new ones, the two deer among them, each with hitbox, rig, animations, looks, states, verbs, sounds and sign. It adds the systems that make the land feel alive: yards, herds, packs, families, scenes between species, and reactions to fire, noise, weather and the camp. It ends with the Minecraft build.

## How this tab fits

**This tab owns ground animals.** Birds, fish and water life, and plants each have their own tab, written in parallel. Semi-aquatic mammals (beaver, muskrat, river otter, mink) get their land half here: hitbox, rig, land gaits, dens and land sign. Their swimming, diving and fishing belong to the water tabs.

**It builds on these and does not repeat them.**

- Blueprint › How animals act, Drives and daily schedules, Senses by species, Who hunts whom, Tracks and sign, Populations.
- Bestiary › its species sheets, Animals that help the player and Rare sightings. Its new sections Carcasses and bodies (what a dead animal becomes) and No vanilla mob mechanics (which vanilla behaviour is off) are referenced, not redone.
- Water, calls and encounters › the draw model, every call, the encounter director, Known animals.
- Ideas from viewers › Raising animals and Taming a wolf without bones; Open world in depth › Taming a wolf.
- Minecraft engine › Animals, Senses, scent and wind, Populations, Tracks and sign.

**Units.** 1 block = 1 m, so speeds in m/s are blocks per second. Hitboxes are width × height in blocks. Minecraft's box is square in plan, so a long body (moose, bison) gets its length from the hit-zone boxes in `data/yearofice/hitzone/<id>.json`, not from the box.

**Three facts shape the cast.** Interior Alaska has no snakes, no moles and no ticks. Moose here carry no winter ticks, so the pale, hair-rubbed "ghost moose" never appears and there is no tick mechanic. The ground cast is small and northern, so each species has to earn its place.

## Every land animal

### The roster and sheet

**This is the complete list.** "Source" names where the species was first specified; "New" means this tab. Gaits are walk / trot or lope / run, in m/s. The player walks at 3.0, runs at 5.5 and swims at 1.0 m/s, so anything over 5.5 outruns them (Minecraft development › Entities and AI, S5). Swim speeds follow the settled table there: moose 1.8, caribou and reindeer 1.6, grizzly 1.5, black bear 1.4, wolf 1.2 m/s (RECONCILE 33).

| Species | Id | Hitbox W × H | Rig | Gaits m/s | Swim | Group | Seasonal look | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Moose | `yearofice:moose` | Bull 1.4 × 2.3; cow 1.3 × 2.1; calf 0.8 × 1.3 | Ungulate | 1.6 / 4 / 14 | 1.8 | Alone; yards of 2–5 in deep snow | Antler cycle, spring moult, velvet strips | Blueprint |
| Caribou | `yearofice:caribou` | 1.0 × 1.5 | Ungulate | 1.5 / 4.5 / 18 | 1.6 | Herd | Antlers on both sexes, white mane on fall bulls | Blueprint |
| Mule deer | `yearofice:mule_deer` | Buck 0.8 × 1.35; doe 0.7 × 1.25; fawn from 0.35 × 0.6 | Cervid | 1.4 / 3.5 / 13; stott 10 | 1.2 | Doe, fawns and yearlings 2–6; bachelor bucks 2–4; yards of 3–15 | Antler cycle, red summer and grey winter coat, spotted fawns | New (Deer, RECONCILE 152) |
| Sitka black-tailed deer | `yearofice:sitka_black_tailed_deer` | Buck 0.7 × 1.15; doe 0.6 × 1.05; fawn from 0.3 × 0.5 | Cervid | 1.3 / 3.2 / 12 | 1.4 | Doe and fawns 1–3; bucks alone | Antler cycle, rust summer and dark winter coat, spotted fawns | New (Deer, RECONCILE 152) |
| Dall sheep | `yearofice:dall_sheep` | 0.7 × 1.0 | Ungulate | 1.2 / 3 / 13 | 0.8 | Ram bands, ewe bands | Horn rings | Blueprint |
| Mountain goat | `yearofice:mountain_goat` | 0.8 × 1.1 | Ungulate | 1.0 / 2.5 / 9 | 0.8 | Band of 3–6 | Wool shed Jun 15 – Jul 31 | Bestiary |
| Wood bison | `yearofice:wood_bison` | 1.6 × 1.9 | Ungulate | 1.4 / 4 / 15 | 1.5 | Herd of 12–20 | Spring shed | Bestiary |
| Muskox | `yearofice:muskox` | 1.3 × 1.5 | Ungulate | 1.2 / 3.5 / 14 | 1.0 | Herd of 8–25 | Qiviut shed May 15 – Jul 15 | New |
| Reindeer | `yearofice:reindeer` | 1.0 × 1.4 | Ungulate | As caribou | 1.6 | Livestock pair | As caribou | Viewers |
| Goat | `yearofice:goat` | 0.9 × 1.3 | Ungulate | 1.0 / 2.5 / 8 | 0.6 | Livestock pair | None | Viewers |
| Wolf | `yearofice:wolf` | 0.8 × 0.9; pup 0.4 × 0.4 | Canid | 1.8 / 3.5 / 15 | 1.2 | Pack of 4–12 | Winter coat | Blueprint |
| Coyote | `yearofice:coyote` | 0.6 × 0.65 | Canid | 1.5 / 3 / 13 | 1.0 | Pair or family | Winter coat | Bestiary |
| Red fox | `yearofice:red_fox` | 0.5 × 0.5 | Canid | 1.3 / 2.8 / 13 | 0.8 | Alone; family Apr – Sep | Red, cross and silver morphs | Blueprint |
| Arctic fox | `yearofice:arctic_fox` | 0.45 × 0.45 | Canid | 1.2 / 2.5 / 12 | 0.7 | Pair | White Oct 20 – May 10 | New |
| Lynx | `yearofice:lynx` | 0.6 × 0.65 | Felid | 1.2 / 2.5 / 13 for 60 m | 1.0 | Alone; mother and kittens | Long winter fur | Blueprint |
| Wolverine | `yearofice:wolverine` | 0.6 × 0.5 | Long mustelid | 1.3 / 4.5 / 11 | 0.9 | Alone; mother and kits | None | Blueprint |
| Black bear | `yearofice:black_bear` | 1.0 × 1.1; standing 0.8 × 1.8 | Ursid | 1.3 / 3.5 / 13 | 1.4 | Alone; sow and cubs | Fat scale; cinnamon morph | Blueprint |
| Grizzly | `yearofice:grizzly` | 1.3 × 1.5; standing 1.0 × 2.4 | Ursid | 1.4 / 4 / 15 | 1.5 | Alone; sow and cubs | Fat scale; rubbed spring coat | Blueprint |
| Fisher | `yearofice:fisher` | 0.45 × 0.35 | Long mustelid | 1.0 / 3 / 9 | 0.6 | Alone | Prime winter coat | New |
| Marten | `yearofice:marten` | 0.35 × 0.3 | Long mustelid | 1.0 / 3 / 8 | 0.5 | Alone | Prime winter coat | Blueprint |
| Ermine | `yearofice:ermine` | 0.2 × 0.15 | Long mustelid | 0.8 / 2.5 / 6 | 0.4 | Alone | White Oct 15 – Apr 15 | Bestiary |
| Least weasel | `yearofice:least_weasel` | 0.15 × 0.1 | Long mustelid | 0.7 / 2 / 5 | — | Alone | White Oct 20 – Apr 10 | Bestiary |
| Mink | `yearofice:mink` | 0.3 × 0.2 | Long mustelid | 0.9 / 2.5 / 6 | 1.2 | Alone | None | Bestiary |
| River otter | `yearofice:river_otter` | 0.5 × 0.35 | Long mustelid | 0.9 / 2.5 / 8 sliding | 3.0 | Family of 2–4 | None | Blueprint |
| Beaver | `yearofice:beaver` | 0.6 × 0.45 | Large rodent | 0.6 / 1.2 / 2 | 2.0 | Colony | None | Blueprint |
| Muskrat | `yearofice:muskrat` | 0.35 × 0.25 | Large rodent | 0.5 / 1 / 2 | 1.5 | Alone | None | Blueprint |
| Porcupine | `yearofice:porcupine` | 0.5 × 0.45 | Porcupine | 0.4 / 0.6 / 0.9 | 0.4 | Alone | Quills raised as a pose | Blueprint |
| Snowshoe hare | `yearofice:snowshoe_hare` | 0.4 × 0.4 | Lagomorph | 1.0 / 3 / 12 | 0.6 | Alone | Brown, then white Oct – Apr | Blueprint |
| Alaska hare | `yearofice:alaska_hare` | 0.5 × 0.5 | Lagomorph | 1.2 / 4 / 18 | 0.6 | Winter bands of 5–20 | White with black ear tips in winter | New |
| Red squirrel | `yearofice:red_squirrel` | 0.25 × 0.25 | Small rodent, climber | 1.0 / 2.5 / 5; climbs 2 | 0.3 | Alone | Ear tufts in winter | Blueprint |
| Flying squirrel | `yearofice:flying_squirrel` | 0.25 × 0.2 | Small rodent, glider | 0.8 / — / 3; glides 8 | — | Winter nest groups of 3–6 | None | Bestiary |
| Arctic ground squirrel | `yearofice:ground_squirrel` | 0.3 × 0.3; upright 0.25 × 0.45 | Small rodent | 0.8 / 2.5 / 5 | — | Colony | None | Bestiary |
| Hoary marmot | `yearofice:hoary_marmot` | 0.45 × 0.4 | Large rodent | 0.6 / 1.5 / 4 | — | Colony | Fat scale | Bestiary |
| Woodchuck | `yearofice:woodchuck` | 0.4 × 0.35 | Large rodent | 0.6 / 1.5 / 4 | 0.4 | Alone | Fat scale | New |
| Collared pika | `yearofice:pika` | 0.2 × 0.15 | Small rodent | 0.6 / 1.5 / 3 | — | Colony | None | Bestiary |
| Red-backed vole, tundra vole | `yearofice:red_backed_vole`, `yearofice:tundra_vole` | 0.15 × 0.1 | Small rodent | 0.4 / 1 / 2 | — | Alone | None | Bestiary |
| Singing vole | `yearofice:singing_vole` | 0.15 × 0.1 | Small rodent | 0.4 / 1 / 2 | — | Colony | None | New |
| Brown lemming | `yearofice:brown_lemming` | 0.15 × 0.12 | Small rodent | 0.5 / 1.2 / 2 | 0.3 | Alone | None | New |
| Collared lemming | `yearofice:collared_lemming` | 0.15 × 0.12 | Small rodent | 0.5 / 1.2 / 2 | 0.3 | Alone | White in winter, forked winter claws | New |
| Northern bog lemming | `yearofice:bog_lemming` | 0.15 × 0.1 | Small rodent | 0.4 / 1 / 2 | 0.3 | Alone | None | New |
| Meadow jumping mouse | `yearofice:jumping_mouse` | 0.15 × 0.12 | Small rodent, leaper | 0.4 / — / 3 in 1.5 m leaps | 0.3 | Alone | Fat ball in September | New |
| Masked shrew | `yearofice:masked_shrew` | 0.1 × 0.08 | Small rodent rig | 0.5 / 1 / 2 | — | Alone | None | Bestiary |
| Little brown bat | `yearofice:little_brown_bat` | 0.25 × 0.15 | Bat | Flies 6 | — | Roost of 20–60 | None | Bestiary |
| Wood frog | `yearofice:wood_frog` | 0.06 × 0.04, pick margin 0.15 (S8) | Frog | Hops 1 | 0.5 | Pond chorus | Frozen state Oct – Apr | Bestiary |
| Hen | `yearofice:hen` | 0.4 × 0.7 | Bird, ground | 0.8 / — / 3 | — | Livestock flock | None | Viewers |

**Insects and spiders** are listed in The small and the many. Only four insects are entities, and only within 8 blocks: `yearofice:wolf_spider` and `yellowjacket` here, `mourning_cloak` and `snow_fly` in Birds and the air (Conflicting ids rows 11–13). Bumblebees, dragonflies, bot flies, snow fleas and ant swarms are particles only (RECONCILE 101).

### Rigs and animations

**Nine rigs carry every animal.** Species on one rig share bone names, so one animation file serves the rig and a species adds only its extras. GeckoLib bones are client-only, as the engine tab says; the server never reads them.

| Rig | Bones | Species |
| --- | --- | --- |
| Ungulate | root, body, chest, neck\_1–2, head, jaw, ear\_l/r, horn\_l/r or antler\_l/r (each with palm and 6 tine children), bell (moose), tail, 4 legs × upper, lower, hoof; 30 bones | Moose, caribou, Dall sheep, mountain goat, bison, muskox, reindeer, livestock goat |
| Cervid | The Ungulate rig's 30 bones with the same names, plus tail\_1–2 (the flag), antler\_l/r each as a beam with up to 5 tine children (brow, g2–g5), and a neck\_swell scale bone; 38 | Mule deer, Sitka black-tailed deer (Deer, below) |
| Canid | root, chest, hips, neck, head, jaw, ear\_l/r, tail\_1–3, 4 legs × upper, lower, paw; 22 | Wolf, coyote, both foxes |
| Felid | Canid set with ear tufts, 1 tail bone and wider paws; 21 | Lynx |
| Ursid | root, chest, hips, hump, neck, head, jaw, ear\_l/r, tail, 4 legs × upper, lower, paw; 20 | Both bears |
| Long mustelid | root, spine\_1–3, neck, head, jaw, ear\_l/r, tail\_1–2, 4 legs × upper, paw; 18 | Wolverine, fisher, marten, ermine, weasel, mink, otter |
| Large rodent | root, body, head, jaw, ear\_l/r, tail (flat for beaver), 4 legs × 2; 13 | Beaver, muskrat, marmot, woodchuck |
| Small rodent and lagomorph | root, body, head, ear\_l/r, whisker, front legs × 2, hind legs × 3, tail\_1–3; 15 | Hares, squirrels, pika, voles, lemmings, jumping mouse, shrew |
| Porcupine | root, body, quill\_shell, head, tail, 4 legs × 2; 12 | Porcupine |

The bat, frog, hen and insects keep the small rigs already in the Bestiary build.

**One animation set, named the same on every rig.** A rig without a gait skips it; the controller falls back to the next one down.

| Animation | Loop | Plays when | Notes |
| --- | --- | --- | --- |
| `idle`, `idle_look` | Yes | Routine, standing | `idle_look` every 8–20 s: head turn, ear flick |
| `graze` / `browse` / `forage` | Yes | Feeding | Grazers lower the head; moose `browse` reaches 1–2.5 m; carnivores `forage` sniff and dig |
| `walk`, `trot`, `gallop` | Yes | By speed: under 2, 2–6, over 6 m/s | Small rodents and mustelids use `bound` for trot and gallop |
| `alert` | Hold | Alert state | The species tell from Senses by species: ears, stopped chewing, head up |
| `freeze` | Hold | Freeze state | Hare, porcupine, small rodents |
| `flee` | Yes | Flee | The gallop with flagged tail and ears back |
| `warn` | Once | Warn state | Huff and jaw-pop, ears back, paw, tail slap |
| `charge` | Yes | Charge | Head low; bears and bison at full run |
| `fight_*` | Once | Fights and attacks | `fight_spar` (antlers, horns), `fight_bite`, `fight_swipe`, `fight_rear` |
| `bed_down`, `sleep`, `get_up` | Once, yes, once | Rest drive | Curled for canids and mustelids, sternal for ungulates |
| `swim` | Yes | In water over 0.8 m | Head and back above water |
| `die` | Once | Death | 20–40 ticks with AI off, run by `ecosystem.wound.DeathPipeline`; a carcass entity of the species' size class replaces the body with no drops (Bestiary › Carcasses and bodies) |
| Extras | Mixed | Species rules | `rut_thrash`, `scrape`, `howl`, `pounce`, `dig`, `climb`, `stand`, `nurse`, `play`, `shake`, `paw_snow`, `ring_face_out`, `slide` |

**Controllers.** Each entity runs four GeckoLib controllers. `base` plays gait and state; `ears_tail` flicks every 2–6 s, faster in bug season; `action` plays one-shots (warn, howl, fight, die); `head` takes its angle in code from the `yearofice:look_target` memory. Gaits blend over 4 ticks, so a moose breaking into a gallop does not pop.

### Seasonal looks

**Looks change by date and body, not by new models.** Coats swap textures on their dates; antlers and fat change bone scale. Every change is readable from 100 m with binoculars, so the land says what season and condition an animal is in.

| Look | Species | Dates and rule | How it is drawn |
| --- | --- | --- | --- |
| Antler cycle | Moose bulls | Prime bulls shed Dec 1 – Jan 15, young bulls Jan 15 – Mar 31; regrowth from Apr 15; velvet until Aug 25 – Sep 5; hard antler to shedding | `antler_*` bones scale 0 to 1 by day; spread set by age class (Moose); velvet and bloody-strip textures |
| Antler cycle | Caribou | Adult bulls shed Nov 1 – Dec 15 after the Oct 1–20 rut; young bulls by April; cows keep theirs until calving in late May | Same bones; cows carry small antlers all winter, so an antlered caribou in March is a cow |
| Horn rings | Dall sheep, goat, bison, muskox | One ring a year; Dall rams reach three-quarter curl at 5–6 years and full curl at 7–8 | Horn mesh chosen by age class; rings in texture |
| Winter white | Hares, ermine, least weasel, arctic fox, collared lemming | On the dates in the sheet, over 20 days of mottled texture | 3 textures: summer, mottled, winter. A white hare on brown ground in a late-snow year is seen at ×1.5 sight range |
| Moult and shed | Moose May 15 – Jun 30, bison May – Jun, goat, muskox | Patchy coat | Moult texture; shed tufts drop as items where the species allows (goat wool, qiviut) |
| Winter fluff | Wolf, coyote, foxes, lynx | Oct 15 – Apr 15 | Body and neck bones scale ×1.1; lynx paws ×1.2 |
| Fat scale | Bears, marmot, woodchuck, ground squirrel | From the species' fat reserve, 0 to 1 | Body bone scale 0.9 (lean spring) to 1.15 (October). A fat bear reads fat |
| Rutted-out bull | Moose, caribou | Late Oct – Nov | Neck bones ×0.95, ribs texture. Matches the 50% fat rule in Butchering and yields |
| Young growth | All with young | Birth to adult size on the species' growth curve | Root scale from 0.3 to 1; calf texture until its first winter |

## Mob sheets: big game

Each block adds what the existing tabs lack: body and age, the state machine's species numbers, the verbs, the sounds and the sign. Numbers already set elsewhere are named, not repeated. Verbs are defined in Interaction verbs below; new species have their full block in New species.

### Moose

**Body.** Alaska moose are the largest deer. Live weight is bull 450–600 kg, cow 350–450 kg, calf 15 kg at birth and 150 kg by October; yields are the 450 kg bull's parts row in Bestiary › Parts by species (180 kg meat, RECONCILE 53), and an individual's parts scale with its own live weight. Age shows in the antlers, so the inspect card reads age class from the spread.

| Age class | Antlers | Spread | Rules that use it |
| --- | --- | --- | --- |
| Yearling | Spikes or forks | 30–60 cm | Driven off by its mother on May 15 |
| Young bull, 2–4 | Small palms | 80–120 cm | Hangs up at 100 m on a grunt (Water, calls and encounters) |
| Prime bull, 5–10 | Wide palms, 10–16 tines a side | 130–180 cm | Comes to fight on a grunt; wins wallows |
| Old bull, 11+ | Narrower, ragged palms | 120–150 cm | Sheds first; lean after the rut |

**States.** Routine walk 1.6 m/s; Flee trots at 4, gallops at 14 for up to 400 m, then trots. Snow over 90 cm halves every gait; off packed trails it costs the moose 4× energy, so it keeps to them (Moose yards). Alert holds 5–20 s with ears swivelling. Charges follow How animals act; in March with snow over 90 cm the packed-trail charge distance rises from 10 to 15 m. A bedded moose stands in 2 s and faces the stimulus before it runs.

**Verbs.** All but trapping (Who answers to what). Bait and feed mean felled browse in winter; hazing works beyond 30 m.

**Sounds.** Cow moan, bull grunt, calf bleat, warn grunt, antler clash, thrash, a stick-snap footfall (80 m) and the water stream of a lifting head; ranges in Data and build › Sounds.

**Sign.** The catalogue in Tracks and sign, plus the browse line, velvet strips and shed antlers (the `yearofice:shed_antler` block, which drops the item `yearofice:antler` with a species component; 10–20 kg a pair; Conflicting ids row 10) from Sign to learn from.

### Caribou

**Body.** Bull 110–160 kg, cow 80–110 kg, as in the Interior's small mountain herds (the parts row in Bestiary › Parts by species is a 110 kg prime bull), calf 6 kg at birth. Caribou are the fastest animal on the map at 18 m/s and swim high on hollow hair at 1.6 m/s, so river crossings are fast.

**States.** The herd runs the herd flow in Caribou herds. One animal's Alert spreads to every neighbour within 15 m after 0.3–0.8 s, so a wave crosses the herd. Flee is 500 m, then milling for 2–5 game minutes, then the route resumes. A single caribou that cannot see what alarmed it circles downwind to scent it (the curious caribou in Animal encounters). From July 1 to August 15, warble and nose bot flies drive it (Caribou bot flies, below, for the herd and hide; Birds and the air › Flies on the caribou owns the flying insects). Rut in the Interior is October 1–20: bulls chase cows and grunt, with hunger weight 0.3.

**Verbs.** Never baited or trapped, except by the one lead-fence snare (B12, Proposed; Caribou herds); hazing a herd only splits it. The call is the arm-antler trick.

**Sounds.** Rut grunt, the ankle click of a walking herd (30 m), calf bawl, the herd's run (600 m).

**Sign.** As catalogued, plus winter feeding craters, sheds on the barrens, and July herds on snow patches.

### Deer: mule deer and Sitka black-tailed deer

**Two true deer live at the edges of the map, never in the home valley.** Mule deer (`yearofice:mule_deer`) wander the eastern Interior and the Yukon-side provinces Beyond; Sitka black-tailed deer (`yearofice:sitka_black_tailed_deer`) live only in coastal provinces Beyond (RECONCILE 152). Both run on the new cervid rig and ship in the M6 content wave behind the flag `yearofice:deer`.

**Why they matter.** Deer are the first big game a light bow can kill, they leave the cleanest buckskin, and a mule deer in the valley is a sighting players talk about. They add no danger to the player; a doe's hooves and a buck in rut matter only to the tamed wolf.

**Where and when.** Counts are region or district limits K, drawn as in Populations. Interior districts use the seed-default codes from World map; columns J–P lie east of the home valley.

|  | Mule deer | Sitka black-tailed deer |
| --- | --- | --- |
| Interior | Eastern districts only: Upper Ninemile (J7–N9), Halfway Hot Springs (M9), Kettle Hills (L10–O12), the Varden Foothills east of column L (L12–P13). K 0–2 per district, 6 for the whole Interior | None |
| Beyond | Provinces east of the Interior (province column px ≥ 1, the Yukon side) of kinds burn country, mixed, and domes and barrens. 0.8 per km² of foothill benches, burns 5–25 years old and dome flanks below treeline | Provinces of the new `coast` kind only (Where the coast is, below). 3 per km² of forest within 3 km of the shore |
| Summer, Jun 1 – Sep 15 | Subalpine meadows and dome flanks at 700–1,100 m, burns, berry slopes | Subalpine and alpine meadows up to 600 m above the shore |
| Rut | Nov 10 – Dec 10, peak Nov 25 | Oct 25 – Nov 25, peak Nov 10 |
| Winter, Oct 15 – May 15 | South-facing benches under open spruce and aspen at 300–700 m; wind-scoured ridges | Old spruce and hemlock below 150 m; the beach fringe and wrack line when snow passes 40 cm |
| Fawns | Jun 5 – Jun 25 | May 25 – Jun 15 |
| Active | Dawn and dusk; beds on slopes from 10:00 to 17:00 | Dawn, dusk and low tide on winter beaches |
| Home valley | Never by default; the straggler below | Never |

**The valley straggler.** Each May 1, if the Interior's mule deer N is above 0, roll 8% for a straggler year (×1.5 after a winter whose snow peaked under 45 cm). The straggler is always a yearling buck driven off by his mother, as real dispersers are. He walks in along the Ninemile across D3's east edge between June 1 and October 31, stays 3–20 game days in willow and burn edges of D3, C3, B3 or A3, then walks out west or dies.

- **Limits.** Never two straggler years in a row. He is a record flagged `straggler`, not part of any home region's K, and no deer breeds in the valley.
- **Odds over a run.** At 8% a year, a 5-year run sees one 34% of the time and a 10-year run 57%.
- **His fate.** If not taken, wolves kill him on 40% of stays (his tracks end in a kill site), and 60% walk out.

**Where the coast is.** The six Beyond province kinds hold no sea, so the Sitka deer needs a seventh kind, `coast`, owned by World map. Ecosystem's requirements: it is drawn only for provinces two or more rows south of the Interior (z ≥ 98,304, about 98 km from Station Nine, past the Varden Range), with a shoreline, spruce and hemlock forest and a wrack line of washed-up kelp blocks. Weather needs one row for it: winter +15 °C and summer −3 °C against the valley, precipitation ×6, sea-level snow peaks of 60–150 cm. Until the kind ships, Sitka deer K is 0 everywhere and only its GameTest province holds them.

**Body.** Weights are live, prime fall adults; the Bestiary's other-sex rule is replaced by a doe factor of 0.65, because deer differ more by sex than moose do.

|  | Mule deer | Sitka black-tailed deer |
| --- | --- | --- |
| Live weight | Buck 80–120 kg (prime 100); doe 55–75 kg (65); fawn 3.5 kg at birth, 30 kg by Oct 1 | Buck 55–85 kg (prime 65); doe 35–50 kg (42); fawn 2.8 kg at birth, 19 kg by Oct 1 |
| Shoulder height, body length | Buck 1.0 m, 1.6 m; doe 0.9 m, 1.45 m | Buck 0.85 m, 1.35 m; doe 0.75 m, 1.2 m |
| Hitbox W × H | Buck 0.8 × 1.35; doe 0.7 × 1.25; fawn scales from 0.35 × 0.6 by growth | Buck 0.7 × 1.15; doe 0.6 × 1.05; fawn from 0.3 × 0.5 |
| Gaits m/s | 1.4 / 3.5 / 13; stott 10 in 5–7 m bounds | 1.3 / 3.2 / 12; no stott |
| Swim | 1.2 | 1.4; swims between islands and out from a beach |
| Step height | 1.0; 1.5 on rock and while stotting | 1.0 |
| Group | Doe, fawns and yearlings 2–6; bucks alone or in bachelor groups of 2–4; yards of 3–15 | Doe and fawns 1–3; bucks alone |
| Life | Up to 12 years; deaths 20% a year | Up to 10 years; deaths 18% a year |

**The cervid rig.** A new rig, the 35th, built from the Ungulate rig's 30 bones with the same names, so the shared ungulate animation set plays on it. It adds 8 bones for 38: `tail_1` and `tail_2` (the flag), `antler_l` and `antler_r` each as a `beam` with up to 5 tine children (`brow`, `g2`–`g5`), and a `neck_swell` scale bone. Base class `UngulateEntity`; no new Java base class.

| Parameter | Mule deer | Sitka black-tailed deer |
| --- | --- | --- |
| Ear length, bone scale against caribou | 25 cm, ×1.6; held out sideways | 15 cm, ×1.0 |
| Tail | 20 cm rope tail, white with a black tip; hangs over a cream rump patch 25 cm wide | 22 cm broad tail, black-brown on top, white below; raised as a white flag in flight |
| Antler branching | Dichotomous: the beam forks, then each fork forks again | Forked, smaller: one fork on the beam and short eye guards |
| Rut neck swell | `neck_swell` ×1.15, Oct 25 – Dec 15 | ×1.1, Oct 10 – Nov 30 |
| Extra animations | `stott`, `look_back`, `tail_flag`, `stamp`, `lip_curl`, `rub`, `spar`, `fawn_drop` | `tail_flag`, `stamp`, `whistle_snort`, `lip_curl`, `rub`, `spar`, `kelp_feed`, `fawn_drop` |

**Antlers by age class.** Bucks only. Growth starts April 15 for both species; the inspect card reads age class from the point count and spread.

| Age class | Mule deer | Sitka black-tailed deer |
| --- | --- | --- |
| Fawn | Buttons 1–2 cm from November | Buttons 1 cm from November |
| Yearling | Spikes or small forks, spread 20–35 cm | Spikes 10–20 cm |
| Young buck, 2–3 | 3 × 3 or small 4 × 4, 45–55 cm | 2 × 2 forks, 25–35 cm |
| Prime buck, 4–7 | 4 × 4 plus eye guards of 2–5 cm, beams 55–65 cm, spread 60–75 cm; a pair weighs 2–3 kg | 3 × 3 plus small eye guards, dark brown, spread 35–45 cm; 0.6–1.0 kg a pair |
| Old buck, 8+ | Heavy, with extra points, spread 50–60 cm | Heavy 2 × 2 or 3 × 3, about 35 cm |
| Cycle | Velvet to Aug 25 – Sep 10; hard to shedding; prime bucks shed Jan 1 – Feb 15, young Feb 1 – Mar 15 | Velvet to Aug 20 – Sep 5; shed Dec 15 – Feb 28 |

Does never grow antlers, so an antlered deer in February is a buck that has not yet shed, unlike the antlered caribou cow.

**Coats.** Coat textures swap by date over 20 days of moult, as in Seasonal looks. Hex values are the mid tone of each Art ramp.

| Coat | Mule deer | Sitka black-tailed deer |
| --- | --- | --- |
| Summer, Jun 15 – Aug 31 | Red-tawny `#9a6a44`; short, thin hair | Rust-brown `#8a4f2f` |
| Fall moult | Sep 1 – Sep 30 | Sep 1 – Sep 25 |
| Winter, Oct 1 – Apr 30 | Brown-grey `#7d7468`, hollow hair; dark forehead band on bucks `#4a3f36` | Dark grey-brown `#4e463f`, almost black when wet |
| Spring moult | May 1 – Jun 15, ragged patches | May 1 – Jun 10 |
| Fixed marks | Cream rump `#e8e0cc`; tail tip `#1e1a17`; pale grey face and white throat patch | Tail top `#26201c`; white tail underside and throat patch |
| Fawn | Rufous `#a0643a` with 2 rows of white spots; spots fade Aug 10 – Sep 1 | Darker rufous `#7f4a2c` with spots; fade Aug 5 – Aug 25 |

Textures are `textures/entity/<id>/<variant>_<coat>.png` with variants `buck`, `doe`, `fawn` and coats `summer`, `winter`, `moult`; velvet is the render layer `<id>/antler_velvet.png`. At 8 m in a screenshot, a mule deer must read by its ears and white rump, a Sitka deer by its compact body and black tail (the G1 gate, RECONCILE 142).

**Drives and schedule.** Hunger, rest, safety, young and rut run as in How animals act. Deer browse willow, rose, alder and birch tips and graze sedge and fireweed. They feed 05:00–09:00 and 18:00–23:00 and bed on slopes in between, where morning thermals bring scent up to them.

**Senses.** Big ears make the mule deer the best listener among big game.

| Species | Sees a moving player at | Hears walking at | Smells downwind at | Tell before it bolts |
| --- | --- | --- | --- | --- |
| Mule deer | 120 m | 80 m | 300 m | Both ears swing to the sound, then a foot stamp |
| Sitka black-tailed deer | 50 m, in forest | 60 m | 250 m | Tail flicks, a stamp, then a whistle-snort |

**Mule deer stott.** A fleeing mule deer on slopes, rock or deadfall bounds on all four feet at once: 5–7 m a bound, rising up to 1.2 m, 10 m/s. It clears 1.5-block brush and logs and turns up to 40° each bound, so a chaser on foot or a wolf on rough ground loses ground. On flat open ground it gallops at 13 m/s instead.

**The look-back.** After 100–250 m of flight, a mule deer stops on a rise and looks back for 3–8 s on 70% of flights. That is the hunter's second chance at a still animal. A Sitka deer never stotts; it bounds into cover with its tail flagged, stops 40–60 m in, and circles downwind.

**Rut.** Bucks follow does with the head low, grunt, and curl the lip at doe urine. Hunger weight falls to 0.3. Bucks rub saplings and spar from late September; two prime bucks fight for 10–60 s, heard at 400 m (mule) or 300 m (Sitka). A rutting buck takes the grunt call (Verbs) and walks to within 20 m.

**Yarding in deep snow.** Deer manage about half the snow depth a moose can, so snow governs their winter.

- **Mule deer.** Once snow passes 50 cm, 3–15 deer gather on a south-facing bench under open spruce, or on a wind-scoured ridge, and keep 200–600 m of packed trail. Off-trail travel costs them 5× energy, so a yard deer flees only 30–80 m, on a trail. Snow over 70 cm for 30 days kills 40% of the fawns and 8% of adults.
- **Sitka deer.** Once sea-level snow passes 40 cm, they move under old-growth canopy, where snow lies at half depth, and onto the beach fringe. At low tide they walk the wrack line and eat kelp (`kelp_feed`). Snow over 60 cm at sea level for 20 days kills 50% of the fawns and 15% of adults; the carcasses lie on the beaches in March for bears and the player.
- **The player.** A snowshoe trail into a yard joins its trail net in 2 days. Yard deer are lean (25% fat) from February.

**Mothers and young.** Both are hiders. A fawn lies flat (`fawn_drop`) in grass or under a spruce for its first 3–4 weeks while the doe feeds 100–300 m away and returns 3–4 times a day to nurse. Adult does have twins 70% of the time (Sitka 50%), yearling does one.

- **A found fawn is not an orphan.** It is counted orphaned only after 1 missed day of visits (Mothers and young, rule 2). It does not move when found and bleats only if touched.
- **The doe fights small predators.** She stamps and strikes with the front hooves at a coyote, fox, lynx or the tamed wolf within 5 m of her fawn: 40 J a strike, on the wolf's legs or chest. A tamed wolf that finds a fawn hide is struck 30% of the time. She never charges the player.

**Predators.** Rates are kills per 30 game days in one region or district at full deer density, as in Who hunts whom.

| Predator | Takes | Season | Rate | How it plays |
| --- | --- | --- | --- | --- |
| Wolf pack | Adults and fawns | All year; mostly deep snow | 2 deer, ×2 with snow over 60 cm | Mule deer stott to slopes over 30° and stand at bay on ledges: wolves win 15% of chases, 35% in deep snow. Sitka deer run into water and swim out: 20%, 45% in deep snow |
| Lynx | Fawns; adults on crust | Fawns Jun 5 – Jul 15; adults Feb – Mar in snow over 60 cm | 0.5 fawns; 0.1 adults | A lynx finds a fawn hide by scent on 1 search in 4 |
| Black bear | Fawns | Jun 1 – Jul 10 | 0.8 fawns | Bears quarter downwind through fawning slopes |
| Grizzly | Fawns; yarded or winter-weak adults; beach carcasses | Fawns in June; carcasses in March and April | 1.0 fawns; 0.1 adults | On the coast, bears answer the fawn bleat (Verbs) |
| Coyote | Fawns; adults in deep snow | June; Jan – Mar | 0.5 fawns; 0.2 adults | Mule deer does mob coyotes: 2–4 does stamp and chase one off |

**Verbs.** Observe, Track, Hunt, Scare, Follow and Sketch work as for caribou (Who answers to what). They are never trapped or hazed, and never fed: winter feeding kills deer.

- **Call.** The grunt call (squeak-call reed, 2 game minutes) draws a rutting buck within 600 m 30% of the time per call. The fawn-bleat call draws a doe with fawns within 300 m to 30 m, June to August, 50% of the time. Each bleat also rolls for every bear within 600 m: black bear 15%, grizzly 8%. On the coast it is how players meet bears.
- **Bait.** A natural lick draws deer at dawn on 40% of days, May to August, under the lick rules in Setting up the hunt.
- **Raise.** An orphaned fawn under 4 weeks can be raised under Raising animals: 0.5 L of milk or broth 4 times a day until 8 weeks. A raised buck turns dangerous in his second rut: he strikes at the player within 3 m (60 J) from Nov 1 to Dec 15.

**Hunting.** Zones are ellipsoids in `data/yearofice/hitzone/<id>.json`, scaled from the moose layout to a 1.0 m shoulder; the Sitka file is the mule deer's ×0.85.

| Zone | Mule deer buck | Sitka buck |
| --- | --- | --- |
| Heart and lungs | 26 × 24 cm, low in the chest behind the front leg; front 30% behind the shoulder blade | 22 × 20 cm |
| Liver | 16 × 14 cm, just behind | 14 × 12 cm |
| Spine | 6 cm strip along the back | 5 cm |
| Brain | 7 cm | 6 cm |
| Legs | Below 0.5 m | Below 0.42 m |
| Gut | The rest | The rest |
| Kill threshold | 22 J | 18 J |

**What the thresholds mean.** A longbow (37.5 J) kills a mule deer out to about 35 m and a Sitka deer to about 38 m. A light bow (22 J, 15 m) never kills a mule deer with flint, since 22 J does not beat 22, but kills one with an iron head (25.3 J) within 18 m. With flint it kills a Sitka deer out to 19 m, so the Sitka deer is the largest game a light bow takes.

**Shooting a stotting deer.** The body rises and falls up to 1.2 m every 0.6 s, so the zones move with it. A shot at a stotting deer is a gut or leg hit 70% of the time. The look-back is the right moment.

**Blood trail.** The Blueprint's blood-trail table holds, with two deer rules.

- **Hair at the hit.** At the spot of the hit the card reads the cut hair: hollow grey hair means high on the back, coarse dark hair the chest, white hair the belly (a gut hit). It tells the wait time before the trail is followed.
- **Wounded flight.** A deer hit in the liver or gut runs downhill and beds in thick cover facing its back trail. A pushed deer runs 300–800 m and its death timer grows by 2 game hours, as for every species.

**Carcass and butchering.** Both are Large carcasses (40–160 kg) that drag on a rope, travois or sled; a fall fawn is Medium. Kcal follow Butchering and yields: meat 1,150 per kg, fat 7,000, organs 1,300.

| Species | Whole | Class | Meat | Fat (fall) | Organs | Cover | Other parts | Guts | Total kcal |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Mule deer, buck | 100 | Large | 40 | 1.5 | 1.9: heart 0.5, liver 1.0, kidneys 0.25, tongue 0.15 | Hide 5 | Antlers (Sep – Dec), back sinew 0.25, marrow bones 2.5, brain 0.25 | 13 | About 59,000 |
| Mule deer, doe | 65 | Large | 26 | 1.0 | 1.2 | Hide 3.5 | Sinew 0.15, marrow bones 1.6, brain 0.2 | 8.5 | About 38,500 |
| Sitka deer, buck | 65 | Large | 26 | 1.2 | 1.3: heart 0.35, liver 0.7, kidneys 0.15, tongue 0.1 | Hide 3.5 | Antlers, sinew 0.15, marrow bones 1.6, brain 0.18 | 9 | About 40,000 |
| Sitka deer, doe | 42 | Large | 17 | 0.8 | 0.85 | Hide 2.5 | Sinew 0.1, marrow bones 1.0, brain 0.15 | 6 | About 26,300 |

- **Season.** Fat is for September to November. Late winter leaves 25% (a March mule buck is about 51,100 kcal), and a rutted-out buck in December 50% (about 53,700).
- **Times.** Scaled from the Dall sheep reference (Dressing by size class): gut 25 game minutes for a mule buck, 19 for a Sitka buck; skin 37 and 29; quarter 56 and 43. Without a hatchet, quartering takes 50% longer.
- **Spoilage.** Deer meat is red meat: raw 6 days at 5 °C. In August at 15 °C a mule buck's 40 kg spoils in 2 days, so a summer deer far from camp is a drying-rack job.

**The hide.** Deer hide makes the softest leather in the game, and its grade follows the date.

| Dates | Hair | Best use |
| --- | --- | --- |
| Jun 1 – Aug 31 | Short red summer hair, thin | Hair-off: smoke-tanned buckskin at full grade; hair-on −30% |
| Sep 1 – Nov 30 | Prime fall coat | Full grade both ways |
| Dec 1 – May 31 | Hollow, brittle winter hair | Leather full grade; hair-on −40%, sheds 5% of its warmth a month in use |

A mule deer hide gives 1.1 m² of leather and a Sitka hide 0.8 m², on the smoke-tanned recipe in Crafting and building in depth; the animal's own brain tans it. Deer carry no warble flies, so summer hides have no holes. A hair-on winter hide makes a sleeping pad of +4 °C.

**Tracks and sign.** They join the sign catalogue in Tracks and sign and start unknown in the notebook.

| Species | Tracks | Scat and beds | Other sign | What it tells the player |
| --- | --- | --- | --- | --- |
| Mule deer | Pointed heart-shaped hoof, 6–8.5 cm doe, 7.5–9 cm buck, dewclaws in snow and mud; stotting groups of 4 prints bunched, 5–7 m apart | Piles of 50–80 pellets 1.2–1.6 cm, dimpled at one end; beds 1.0–1.3 m ovals on slopes, melted into snow | Browse snapped ragged at 0.3–1.5 m, below the moose line; buck rubs on 2–5 cm saplings at 0.4–1.0 m, Sep – Nov; sheds | Deer browse sits low and moose browse high; bunched groups mean a stotting deer, so look uphill |
| Sitka black-tailed deer | Narrow hoof 5–7 cm | Pellets 1.0–1.3 cm; beds under hemlock | Trails along the beach fringe; kelp pulled and chewed above the tide; rubs at 0.3–0.8 m | Fresh beach trails in deep snow mean deer feed here at the next low tide |

Shed antlers lie as the `shed_antler` block with species state `mule_deer` or `sitka_black_tailed_deer`.

**Sounds.** Ranges in blocks, volume = range ÷ 16, as in Data and build › Sounds.

- **Mule deer:** `snort` 100, `stamp` 30, `grunt` 60, `fawn_bleat` 120, `antler_clash` 400, `stott` 60 (the 4-hoof thud of each bound).
- **Sitka black-tailed deer:** `whistle_snort` 150, `stamp` 30, `grunt` 50, `fawn_bleat` 100, `antler_clash` 300.

**The first sighting.** Each species' first sight fires once per world: Spirits +5, a journal photo, and the species added to the Field sketch list.

| Moment | Mule deer (the valley straggler) | Sitka black-tailed deer |
| --- | --- | --- |
| Before the sight | Tracks on a D3 gravel bar, read as "Too small for moose. Not caribou." | Pellets and pulled kelp on the wrack line |
| Trail Messenger | "A deer. Big ears, white rump. A long way from home." | "Small dark deer on the beach, eating kelp at low tide." |
| Journal | "A mule deer on the Ninemile bar, ears like a mule's. Nobody's seen one here in years." | "Black-tailed deer on the tide line, pulling kelp. The sea feeds them when the snow won't." |
| Extra | Radio, once: Pete Lund says "Grandpa Arvid wrote up a mule deer on the upper Ninemile in '64. He never saw another." Spirits +3 more for the valley straggler | The first winter sighting is usually at low tide in snow over 40 cm |

A mule deer met first in the Interior east uses the same lines, minus Pete's call and the extra Spirits.

**Data and build.** These follow Minecraft development › Entities and AI and Adding a species end to end.

- **Ids.** Entities `yearofice:mule_deer`, `yearofice:sitka_black_tailed_deer`. Item families: `mule_deer_meat`, `mule_deer_hide`, `sitka_black_tailed_deer_meat`, `sitka_black_tailed_deer_hide`; heart, liver, kidney, tongue, `antler` and `brain` carry the species component. Rig id `cervid`. Flag `yearofice:deer`.
- **Data.** `data/yearofice/species/<id>.json`, `data/yearofice/loot_table/butchering/<id>.json`, `data/yearofice/hitzone/<id>.json`, `data/yearofice/item_family/<id>.json`, `data/yearofice/rare_sighting/mule_deer_straggler.json`, `data/yearofice/flag/deer.json`.
- **Assets.** `assets/yearofice/geo/entity/<id>.geo.json` (own model each), `animations/entity/rig_cervid.animation.json` plus `<id>_extra.animation.json`, merged by `AnimationMergeProvider`; textures as in Coats; sounds `entity.<id>.<call>`.
- **Java.** Both use `UngulateEntity` with the species id; no species class. `ecosystem.species.rig.CervidMoves` adds the stott gait and the look-back activity `yearofice:look_back`. `RareSightings` gains the straggler; `Yarding` reuses `YardManager` with the deer depths.
- **Spawning.** Only `PopulationSpawner`, ON\_GROUND placement, Active cap group big game. A species with `requires_flag` off spawns nothing (Content flags).
- **Milestone.** M6 content wave. The flag defaults on in dev and beta, off in stable.

```json
{
  "entity": "yearofice:mule_deer",
  "requires_flag": "yearofice:deer",
  "register": { "body_plan": "cervid", "hitbox_m": { "buck": [0.8, 1.35], "doe": [0.7, 1.25] }, "placement": "on_ground", "category": "creature" },
  "habitat": { "interior_districts": ["upper_ninemile", "halfway_hot_springs", "kettle_hills", "varden_foothills_east"], "beyond": { "province_kinds": ["burn_country", "mixed", "domes_and_barrens"], "min_px": 1, "per_km2": 0.8, "cover": ["foothill_benches", "burn_5_25", "dome_flank"] } },
  "population_model": { "k_interior": 6, "k_per_district_max": 2, "births": { "dates": ["06-05", "06-25"], "young": 1.6, "juv_survival": 0.55 }, "adult_deaths_per_year": 0.2, "migration": 0.1, "tracks_sex": true, "doe_weight_factor": 0.65 },
  "gaits_mps": { "walk": 1.4, "trot": 3.5, "run": 13, "stott": 10, "swim": 1.2, "step_height_m": 1.0 },
  "senses": { "sight_m": 120, "hearing_m": 80, "smell_m": 300, "fov_deg": 310 },
  "behavior": { "stott": { "bound_m": [5, 7], "rise_m": 1.2, "turn_deg": 40 }, "look_back": { "after_m": [100, 250], "hold_s": [3, 8], "chance": 0.7 }, "rut": ["11-10", "12-10"], "yard": { "snow_cm": 50, "group": [3, 15], "trail_m": [200, 600], "flee_m": [30, 80], "off_trail_cost": 5 } },
  "young": { "plan": "hider", "hide_days": [21, 28], "visits_per_day": [3, 4], "twins": 0.7, "defend": { "targets": ["#yearofice:small_predators", "yearofice:wolf"], "radius_m": 5, "strike_j": 40 } },
  "straggler": { "chance_per_year": 0.08, "mild_winter_factor": 1.5, "window": ["06-01", "10-31"], "stay_days": [3, 20], "entry": "D3_east", "no_repeat_years": 1 },
  "kill_threshold_j": 22,
  "butcher_table": "yearofice:butchering/mule_deer",
  "sign": { "track": "hoof_heart_8cm", "other": ["low_browse", "rub_low", "stott_group"] }
}
```

```json
{
  "species": "yearofice:mule_deer",
  "poses": {
    "standing": [
      { "zone": "heart_lungs", "center_m": [0, 0.7, 0.3], "radii_m": [0.13, 0.12, 0.14], "shoulder_front": 0.3 },
      { "zone": "liver", "center_m": [0, 0.72, 0.08], "radii_m": [0.08, 0.07, 0.08] },
      { "zone": "spine", "center_m": [0, 1.0, 0], "radii_m": [0.03, 0.03, 0.6] },
      { "zone": "brain", "center_m": [0, 1.3, 0.85], "radii_m": [0.035, 0.035, 0.035] },
      { "zone": "gut", "center_m": [0, 0.72, -0.25], "radii_m": [0.22, 0.22, 0.4] },
      { "zone": "legs", "below_y_m": 0.5 }
    ],
    "bedded": { "from": "standing", "lower_m": 0.45 }
  },
  "kill_threshold_j": 22,
  "bloodshot": { "heart_lungs": ["ribs", "front_left", "front_right"], "gut": ["loin", "hind_left", "hind_right"] }
}
```

```json
{
  "type": "yearofice:butchering",
  "pools": [
    { "rolls": 1, "conditions": [ { "condition": "yearofice:dressing_step", "step": "joint" } ],
      "entries": [ { "type": "item", "name": "yearofice:mule_deer_meat", "functions": [ { "function": "set_count", "count": 400 }, { "function": "yearofice:copy_freshness" } ] } ] },
    { "rolls": 1, "conditions": [ { "condition": "yearofice:dressing_step", "step": "skin" } ],
      "entries": [ { "type": "item", "name": "yearofice:mule_deer_hide", "functions": [ { "function": "yearofice:pelt_grade" } ] } ] },
    { "rolls": 1, "conditions": [ { "condition": "yearofice:dressing_step", "step": "skin" } ],
      "entries": [ { "type": "item", "name": "yearofice:animal_fat", "functions": [ { "function": "yearofice:season_fat", "fall": 15, "spring": 4 } ] } ] }
  ]
}
```

Counts are 100 g units, so 400 is 40 kg; the doe and Sitka tables scale by the parts rows. Organs, antlers and the rest follow the gut and joint steps as for every Large carcass.

```json
{ "id": "yearofice:deer", "milestone": "m6", "default": { "dev": true, "beta": true, "stable": false }, "affects_worldgen": false, "reversible": true, "description": "Mule deer and Sitka black-tailed deer (Land animals in depth › Deer)" }
```

**Commands.** `/yoi spawn <species> [count] [buck|doe|fawn] [age]` writes records first and then promotes them, so the deer are counted. With the flag off it refuses ("Flag yearofice:deer is off") unless `force` is added in a creative test world. `/yoi deer straggler` starts a straggler year now; `/yoi deer info <target>` prints age class, antler stage, coat, fat, yard and straggler flag.

**GameTests.** In `com.yearofice.gametest.ecosystem.DeerGameTests`, waiting on game time through the clock.

1. **The straggler is rare and walks in from the east.** Over 10,000 seeded years with the flag on and Interior mule deer N above 0, a straggler year comes in 7.5–8.5% of eligible normal-winter years and 11.3–12.7% of eligible mild ones (a year is eligible when the one before had no straggler). None follows a straggler year, every arrival is between June 1 and October 31 across D3's east edge, and the home valley holds 0 deer on all other days. With the flag off, 0 straggler years and 0 deer spawned over 7 game days (extends `flag_off_hides_species`).
2. **Stott, look-back and the light bow.** Over 1,000 trials, a mule deer alarmed at 60 m on a 20° slope stotts with bounds of 5–7 m and stops to look back between 100 and 250 m on 65–75% of flights; a Sitka deer never stotts. In 1,000 broadside heart-and-lung hits at 15 m, a full-draw light bow with flint kills every Sitka deer and no mule deer, and with iron kills every mule deer.

### Dall sheep

**Body.** Ram 70–110 kg, ewe 50 kg, lamb 4 kg. Horn rings tell age: count them through binoculars at 300 m or less. A ram's curl reads three-quarter at 5–6 years and full at 7–8.

**States.** Escape terrain is any slope over 35° within 50 m, as written. Sheep get a step height of 1.5 blocks on rock and climb 2-block ledges, so they leave the player behind on scree at 13 m/s. Ewes with lambs keep to slopes over 45° from May 25 to July 15. Alert is the band looking down; Flee climbs 100 m. Ram rut (Nov 10 – Dec 10) adds head clashes every 5–20 game minutes, heard at 1 km.

**Verbs.** Lick baiting, no calls. **Sounds.** Horn clash (1 km), lamb bleat (150 m), scree under hooves (200 m).

**Sign.** As catalogued, plus rings counted on a found horn (age at death) and white hair on rocks at bed ledges.

### Mountain goat and wood bison

**Mountain goat.** The Bestiary block stands. Step height is 2 blocks on rock. Kids keep to the nanny's uphill side, so she stands between them and danger.

**Wood bison.** The Bestiary block stands. Bison roll in dust and mud and leave a 3 m bare bowl. In the rut a bull walks at a cow's flank, and its charge line stays the Bestiary's 50 m.

## Mob sheets: predators

### Wolf

**Body.** Male 38–55 kg, female 30–45 kg (the parts row in Bestiary › Parts by species is a 38 kg prime male), pups 0.5 kg at birth. The tamed wolf is the same entity with the trust attachment.

**States.** A pack trots at 3.5 m/s for hours; a chase runs 15 m/s for at most 150 m. Roles, hunts and the sentinel are in Wolf packs and roles. A lone wolf avoids pack scent posts. In a test, a shout delays the rush by 60 s but does not end it.

**Verbs.** All but feeding; following is by tracks; taming only for the three kinds in Taming a wolf without bones.

**Sounds.** Howl and chorus (one voice per wolf), the den alarm bark-howl, pup yips, the warn growl.

**Sign.** As catalogued, plus howl counting: after 10 learned howls the messenger counts voices ("about 6 voices").

### Grizzly and black bear

**Body.** Grizzly boar 180–300 kg, sow 110–180 kg; black bear boar 90–140 kg, sow 55–90 kg; cubs 0.4 kg at birth in the den. The fat scale draws condition, so a lean May bear and a round October bear look different at 200 m.

**States.** Both gallop at 13–15 m/s; a grizzly swims at 1.5 m/s and a black bear at 1.4 (Bears in water; RECONCILE 33). `stand` plays during Alert: a standing bear is sniffing, not threatening. A black bear sow woofs her cubs up a tree and waits at the trunk. A grizzly sow keeps cubs at heel.

**Verbs.** No bear is ever trapped: no set in Traps holds one, and a bear eats the bait and breaks the set.

**Sounds.** Huff and jaw-pop, the sow's woof, cub bawl, fight roar (600 m), digging and tearing.

**Sign.** As catalogued, plus dug middens in spring, torn ant stumps in June and dug wasp nests in August.

### Lynx

**Body.** 9–14 kg; kittens 0.2 kg. Big winter paws spread its weight, so it runs on snow that stops a fox.

**States.** A pounce is 3 bounds over 6–10 m at 13 m/s; a lynx never chases more than 60 m. It sits watching a caller at 40–80 m (Water, calls and encounters). In March, the mating season, lynx yowl at night and travel twice as far.

**Deepened: kittens.** Kittens are born June 1 in a windfall den, 3 a litter or none in a hare-low year (Populations). From August they follow the mother single file and learn by watching her hunt. They stay with her until March, so a winter lynx track with 2–3 small tracks beside it is a family. The mother never charges. If she is taken between June and December, her kittens die within 10 game days. A lynx kitten is not a raisable orphan.

**Sounds.** March yowl (800 m), warn hiss, kitten mew.

**Sign.** As catalogued, plus cached hares under snow with a lynx bed within 20 m.

### Wolverine

**Body.** Male 12–18 kg, female 8–12 kg, kits 0.1 kg. It lopes at 4.5 m/s for hours and covers 30–50 km a day.

**States.** It climbs trees and meat poles (Blueprint › New species). It holds its ground at food within 5 m, as written. It follows a wolf pack's trail on 30% of winter days in the same region, arriving at their kills 12–36 game hours after.

**Deepened: kits.** A female digs a snow den 2–3 m deep in a north-slope drift at treeline. Two kits are born March 15. Until May she hunts within 5 km of the den, and her cache raids rise from 60% to 90% a day. The kits travel with her from late May and leave in August–September.

**Sounds.** Growl and tearing at a cache (60 m), kit whines at the snow den. **Sign.** As catalogued, plus the snow den hole and musky food caches in snow.

### Red fox and coyote

**Red fox.** 4–6 kg; kits 0.1 kg. A mousing pounce plays `pounce` with a 1 m arc into snow.

- **Deepened: colour morphs.** The rare-sighting roll stays (8% of years, a quarter of those silver). A morphed fox breeds true: each of its kits is the same morph 50% of the time. Once one appears, a valley can carry cross foxes for years. Cross fox: red with a black cross over the shoulders and black legs. Silver: black with silver-tipped guard hairs. Pelt decoration values stay 4 against 1.
- **Family.** Kits play at the den mouth at dusk from June 1 to July 15. The vixen barks every 3–5 s at a player within 80 m, and moves the kits that night if the player stays 10 game minutes.
- **Sounds.** Bark (300 m), the vixen's February scream (500 m), kit whimpers.
- **Sign.** As catalogued, plus den mounds ringed with bright green grass from the den's waste, seen from 300 m in June.

**Coyote.** The Bestiary block stands. A pair dens in a bank like a fox, with 5 pups on April 25.

## Mob sheets: small game and furbearers

### Snowshoe hare

**States.** A hare in a form freezes at awareness 30 and bolts zigzag at 12 m/s inside its flush distance (How animals act). It runs a loop and comes back to its runway within 10 game minutes, so a waiting player sees it again. Packed runways in snow make every hare trail a snare site with the 4× bonus.

**Deepened: leverets.** Leverets are born furred with eyes open, 4 a litter (Populations). The doe leaves each one alone in a different form and visits once a day at dusk to nurse for 5 game minutes. So a lone leveret in daylight is not an orphan. It counts as orphaned only when no doe has come for 2 dusks, which happens to 15% of the leverets a player finds. Watching at dusk is how the player tells; taking one with a living doe is taking young from a living mother (Raising orphaned young).

**Sounds.** Foot-thump alarm (20 m), the scream of a caught hare (200 m).

**Sign.** As catalogued, plus gnaw height (Sign to learn from).

### Red squirrel, flying squirrel and ground squirrels

**Red squirrel.** The Squirrels (Deepened) escape rules stand. If its nest tree is felled or climbed, a mother carries her 4 kits one by one to a new nest within 50 m, 1 every 2 game minutes. Cones dropping in cutting season are heard at 40 m.

**Flying squirrel.** The Bestiary block stands. Below −20 °C, 3–6 share one nest cavity.

**Arctic ground squirrel and hoary marmot.** The Bestiary blocks stand. Both stand upright at Alert (the `stand` pose), fatten visibly through summer, and never surface inside their hibernation dates.

### Porcupine

**States.** Freeze turns its back and plays `quill_shell` at scale 1.3 with a teeth-chatter. Anything that strikes it is quilled, as written. It climbs at 0.3 m/s and sits in one spruce for days in winter.

**Deepened: porcupettes.** One porcupette is born May 20, quilled; the quills harden within 1 game hour. By day it hides at the base of its mother's feeding tree while she sits aloft, and they meet at dusk. It follows her for 4 months. A porcupette found alone at a trunk base is not an orphan; the mother is usually in the tree above (look up).

**Sounds.** Teeth chatter (warn), October courting whines (60 m), gnawing at night (30 m).

**Sign.** As catalogued, plus chewed outhouse seats and paddle handles.

### Marten, ermine and least weasel

**Marten.** Curious, as written. It runs logs and bridges rather than open snow, and rests in squirrel middens below −25 °C. Its pelt is prime November 15 – February 28.

**Ermine and least weasel.** The Bestiary blocks stand. Both hunt under the snow and pop up 3–10 m away, leaving paired prints that vanish into holes.

### The land half of beaver, muskrat, otter and mink

**Beaver.** On land it walks at 0.6 m/s, stays within 100 m of water and flees straight to it. It cuts with `forage` at the trunk and drags branches by the jaw bone.

**Muskrat.** On land only at push-ups and feeding platforms, at 0.5 m/s. What a push-up holds (plants and cached arrowhead tubers) is in Plants and fungi › Animals and plants.

**River otter.** It slides banks at 8 m/s (the `slide` extra). A family slides the same bank 5–15 times in a session, which makes the slide sign.

**Mink.** Runs the bank at 2.5 m/s and into every hole. Its land sign is the pinch-waisted bound in snow along the water's edge.

**Verbs on land.** All four can be observed, tracked, baited, trapped, hunted, scared and sketched; the water tabs own the rest.

## Mob sheets: the small and the many

**Small animals are a bubble, not a census.** Voles, shrews, lemmings and the jumping mouse are entities only within 32 blocks of a player, at a density per 100 m² times their index (the Bestiary's vole rule). The four insect entities exist only within 8 blocks; every other insect is a particle (RECONCILE 101).

**Shared small-mammal states.** Routine forages in runways at 0.4–1 m/s. At awareness 30 they freeze for 1–3 s; at 70 they dart to the nearest hole, runway or snow tunnel within 3 m. Under snow deeper than 20 cm they are invisible and untouchable, except to an ermine, a fox's pounce or an owl.

| Species | Verbs | Sounds | Sign |
| --- | --- | --- | --- |
| Collared pika | Observe, sketch; take hay | `entity.pika.eep` (existing) | Haypiles under talus |
| Red-backed and tundra voles | Observe, trap (mouse deadfall), sketch | `entity.red_backed_vole.squeak`, `entity.tundra_vole.squeak` (3 m) | Runways and cut grass at snowmelt; droppings in caches |
| Masked shrew | Observe, trap (mouse deadfall), sketch | `entity.masked_shrew.twitter` (2 m) | Tiny tracks with a tail line on fresh snow |
| Little brown bat | Observe, sketch | `entity.little_brown_bat.click` (existing) | Droppings under the roost |
| Wood frog | Observe, take as bait, sketch | `entity.wood_frog.chorus` (existing) | Chorus; frozen frogs under litter |
| Bumblebee | Observe, take a nest, scare off with smoke, sketch | `ambient.bumblebee.buzz` (existing); bees are the `yearofice:bumblebee` particle and a nest is the `bumblebee_nest` block (RECONCILE 101) | A dug bank where a bear took a nest |
| Hen, goat, reindeer | Care rules in Raising animals | `entity.hen.cluck`, `entity.goat.bleat`, `entity.reindeer.grunt` | Tracks in the pen |

The ten new small animals and six insects follow in New species.

## New species

Sixteen species join the cast, each placed where interior Alaska supports it and each with one rule that changes play. Three are range-edge animals (Alaska hare, collared lemming, fisher) whose real ranges barely reach the Interior; they are kept rare and flagged so data can cut them. Counts are region limits K, drawn as in Populations.

| Species | Id | Where | Count | Season | Yield | The rule that makes it matter |
| --- | --- | --- | --- | --- | --- | --- |
| Muskox | `yearofice:muskox` | Hundredmile Barrens | One herd of 20–40 | All year | Cow: 60 kg meat, 6 kg fat, 3 kg organs, about 115,000 kcal; robe hide; horns; qiviut | Forms a ring instead of fleeing; sheds the warmest wool in the game |
| Arctic fox | `yearofice:arctic_fox` | Beyond's north tundra; the barrens after a lemming crash | 0–6 in the Interior in crash winters | Nov – Apr in the Interior | 2.5 kg rank meat; white pelt | Bold thief that follows bears and wolves |
| Fisher | `yearofice:fisher` | Interior east; a rare disperser in the core | 0–3 in the Interior | All year | 3 kg rank meat; dark pelt | The only animal that kills porcupines |
| Alaska hare | `yearofice:alaska_hare` | Tundra above 750 m | 5–20 per tundra district | All year | 2.6 kg meat, 0.15 kg fat, about 4,000 kcal; large pelt | Big, open-country, no cycle |
| Woodchuck | `yearofice:woodchuck` | South-facing birch benches | 2–6 per region | Apr 20 – Sep 30 | 2.2 kg meat, 0.8 kg fat by September, about 8,100 kcal; pelt | Raids gardens from below |
| Brown lemming | `yearofice:brown_lemming` | Tundra and alpine snow beds | Per 100 m² × L | All year | 50 g; bait | Drives the lemming index L |
| Collared lemming | `yearofice:collared_lemming` | Beyond's north-west tundra; the Antler Tors, rare | Per 100 m² × L × 0.2 | All year | 50 g | The only rodent that turns white |
| Northern bog lemming | `yearofice:bog_lemming` | Deep sphagnum bogs | 0.2 per 100 m² of bog | All year | 30 g | Its runways mark deep, dangerous moss |
| Singing vole | `yearofice:singing_vole` | Tundra and alpine meadows | Colonies of 6–20 | All year | 40 g | Makes haypiles and trills alarms |
| Meadow jumping mouse | `yearofice:jumping_mouse` | Valley meadows and sedge edges | 1 per 200 m² of meadow | Jun 1 – Aug 31 awake | 20 g | Sleeps 9 months in the soil the player digs |
| Wolf spider | `yearofice:wolf_spider` | Moss and gravel everywhere | Ambient | May 1 – Sep 30; warm winter days on snow | None | Its eyes shine green in a torch beam |
| Carpenter ant | Log block state `ants=none\|colony`, no entity | Punk wood over 30 cm, and wet log pieces | Colonies | May – Sep | Brood (Grubs and ant brood) | Eats the player's sill logs |
| Yellowjacket | `yearofice:yellowjacket` | Vole holes, spruce, eaves | 0–4 nests per km² | Jun – Sep | Paper nest tinder | Raids the drying racks |
| Caribou bot flies | `yearofice:warble_fly`, `nose_bot_fly` particles (Birds and the air) | With caribou and reindeer | Swarm per herd | Jul 1 – Aug 15 | Warble larvae | Drive herds onto ridges; hole the hides |
| Snow flea | `yearofice:snow_flea` particle | Snow at tree bases | Particle swarm | Mar 15 – Apr 30 | None | Warns that the snow will rot |
| Mourning cloak | `yearofice:mourning_cloak` (Birds and the air owns it) | Birch stands; woodpiles in winter | Ambient | Apr 1 – Aug | None | The first butterfly, at running sap |

### Muskox

**A reintroduced herd that stands and faces you.** The program that released the wood bison put muskox on the Hundredmile Barrens 12 years before the game. Each wears an ear tag that logs like a bison's. The herd winters on scoured ridges by the Antler Tors, where snow lies under 30 cm, and summers in creek willow.

- **Ring defence.** At awareness 70 toward wolves, a bear or a player within 60 m, the adults ring the calves, facing out, within 10 s. They do not flee. A bull charges out 10–20 m at a threat inside 20 m, then backs in; 40% of these charges are bluffs.
- **Breaking the ring.** A player who stays inside 30 m for 5 game minutes breaks it half the time: the herd runs 300 m and re-forms. Wolves get a calf only when a ring breaks.
- **Qiviut.** From May 15 to July 15 the herd sheds underwool on willow and tussocks within 300 m of its range. Each tuft of `yearofice:qiviut` is 10 g and takes 5 game seconds; a season holds about 1.5 kg. 100 g felts into a qiviut liner (Bushcraft workbench, 1 game hour): hat or mitt inner, +4 °C, 90% kept wet, 0.05 kg, the warmest material per gram in the game.
- **Cost.** Births are 0.6 calves per cow, so each cow taken shows in the herd for a decade. A rain-on-snow winter kills 20% of the herd, leaving carcasses on the barrens in April for bears and the player.
- **Rut.** August 1 – September 15: bulls crash heads, heard 2 km off.
- **Screen.** First sight: "Muskox, in a ring on the ridge. They don't run." Sign: round 12–14 cm hooves, qiviut on willows, craters broken through wind crust. Scaring only tightens the ring.

### Arctic fox

**A white fox that walks in after a crash.** Arctic foxes live on Beyond's north tundra. In the winter after a lemming crash (L of 0.3 or less), 2–6 wander onto the Hundredmile Barrens and Bald Domes from November to April. In 10% of those winters one reaches the home valley's A2.

- **Bold.** It comes to 5 m of a still player as a Curious animal, and it raids caches and racks at twice a red fox's rate. It shadows wolves and grizzlies at 100–300 m for scraps.
- **Red foxes kill it.** Where a red fox range overlaps, the arctic fox moves off; a meeting kills it 30% of the time.
- **Coat.** White October 20 – May 10, brown-grey in summer. The white pelt has decoration value 3 and makes a hood trim.
- **Screen.** "A white fox on the barrens. Not a red one." Rapid yaps carry 300 m. Tracks 4–5 cm, furred and blurred.

### Fisher

**A dark weasel the size of a cat, and the porcupine's only killer.** The fisher is a range-edge animal. 0–3 live in the Interior's east (Upper Ninemile, Kettle Hills, Varden Foothills), and in 5% of years one crosses the core between December and March.

- **Porcupine kills.** It circles a porcupine on the ground for 20–40 game minutes, biting at the face, and kills on 1 try in 3. It eats from the belly and leaves the skin turned inside out with the quills. Porcupine K in a fisher's range falls 30%.
- **Trapline thief.** It takes a caught marten from a set at 40% a day after 24 hours, and breaks marten cubbies 30% of the time. Taken in a 25 cm cubby mouth or a deadfall with a 50 kg rock; p\_s 0.03.
- **The lookalike lesson.** Fisher 3–6 kg, near-black, tracks 6–9 cm in paired bounds; marten 0.7–1.3 kg, orange-brown with a buff throat, tracks 4–5 cm. Before the player learns it, its sign reads "Marten? Too big."
- **Screen.** Sign: "Porcupine skin, turned inside out. Something killed it from the belly." Pelt decoration value 3.

### Alaska hare

**Twice the size of a snowshoe hare, in country with no cover.** It lives on tundra above 750 m: the Hundredmile Barrens, the Bald Domes, Beyond's north-west tundra, and in 1 year in 5 the home valley's A2. Its real range lies west; this edge population is a design choice.

- **No cycle.** K holds steady (×0.9–1.1 a year), so it is the high country's reliable meat when snowshoe hares crash.
- **Open-ground escape.** It sits up 0.7 m tall to look and flushes at 40–80 m. It runs the first 3 bounds upright, then at 18 m/s. In winter, bands of 5–20 dig to willow on scoured slopes.
- **Taking it.** Sling or bow from a blind at a willow patch. Snares on its willow trails roll at 0.6 × the snowshoe rate. One pelt does the work of two snowshoe pelts.
- **Screen.** "Hare, but twice the size." Hind tracks 15–18 cm.

### Woodchuck

**A fat burrower that eats the garden from below.** Woodchucks live on south-facing birch benches: B2 and C2 at home, the Upper Ninemile and the Varden Foothills. They are awake April 20 – September 30.

- **Burrows.** Each has 2–3 mouths with a fan of fresh soil; old burrows house foxes and ermine.
- **The garden.** A woodchuck within 150 m of a garden takes 10% of its leafy crops and seedlings a week. It digs under any fence. A buried skirt of poles 0.3 m deep stops it: +0.5 game hours of work per 10 m of fence.
- **Fat.** By September it carries 0.8 kg of fat (5,600 kcal), a valley fat source in the lean weeks before the rut.
- **Escape.** It whistles and climbs 2–3 m up a low tree when chased.
- **Taking it.** Snare at a burrow mouth, 30% per day roll; bow.
- **Screen.** "Something is eating the cabbages from the bottom up. Fresh soil by the alders." Its whistle carries 200 m.

### Lemmings

**One new index, L, drives the tundra.** Each tundra and alpine district, plus the home valley's A1, A2 and D1, holds L from 0.2 to 2.5. It runs a 4-year cycle with its own seeded phase, separate from the vole index V.

| Year in cycle | 1 | 2 | 3 | 4 |
| --- | --- | --- | --- | --- |
| Lemming index L | 0.2 | 0.7 | 2.5 | 0.3 |

- **Brown lemming.** Snow beds and wet tundra. In a peak May it runs across snow and lake ice by day, and some drown: fish in tundra ponds bite ×1.3 that month. At L of 2 or more it stops fleeing and squeaks at the player's boot from 1 m. Grass winter nests 15 cm across appear at snowmelt; their count reads L.
- **Collared lemming.** The only rodent that turns white (October 25 – May 15), with forked winter claws for digging crust. Rare in the Interior; a sketch of a white one is a rare plate.
- **Northern bog lemming.** Deep sphagnum only, not cyclic. Its runways, with clipped sedge and bright green droppings, appear only where moss is over 0.5 m deep. Once learned, they mark bog holes.
- **What L changes.** Arctic fox litters are 4 + 8 × L, and the crash after a peak sends them south. On tundra, ermine and fox litters use min(1.5, L) in place of V. The birds tab's tundra raptors can read L.
- **Use.** Mouse deadfalls take them; a lemming is pike bait ×1.3.

### Singing vole and meadow jumping mouse

**Singing vole.** Colonies of 6–20 live in tundra and alpine meadows. From August 1 each builds haypiles of 0.1–0.4 kg under rocks: tinder ×1.3 in rain, with the pika hay rule's cost. It trills at foxes, ermine and raptors within 30 m, a helper: "Voles trilling: a fox close."

**Meadow jumping mouse.** Awake only June 1 – August 31; it sleeps 9 months, the longest of any animal in the game.

- **Summer.** Scything sedge or walking a meadow at dusk flushes mice that leap 1–1.5 m in 3–4 bounds and then freeze. Its tracks show a long tail line.
- **The sleeper in the soil.** Digging meadow soil from September to May (garden beds, a root cellar, the 2 blocks pillar 3 allows) finds a curled, cold mouse once per 40 m²: `yearofice:sleeping_jumping_mouse`. Reburying it gives Spirits +1 and "Put the mouse back to bed." Carried indoors it wakes, and released in winter it dies.

### Wolf spider

**The ground at night is full of eyes.** Wolf spiders hunt on moss and gravel from May to September, and on snow on winter days above −2 °C.

- **Eyeshine.** In a torch or lantern beam at night, moss within 6 m shows 5–30 tiny green sparkles per 10 m². The first time, Spirits +2 and the journal line "The moss is full of tiny green lights."
- **Life.** Females carry white egg sacs in June and spiderlings on the back in July: two sketch plates.
- **Use.** A pinch of spiders is grayling bait ×1.1. They do not bite in the game.
- **Build.** Client particles `yearofice:spider_eyeshine` drawn only in the player's light cone; a 0.1 × 0.05 entity within 8 m for sketching.

### Carpenter ants

**Ants live in punk wood, including the cabin's.** Colonies sit in dead logs and stumps over 30 cm across and 5 years old. They also take any player log piece that touches soil or stays wet: sill logs on bare ground, a woodpile's bottom row, dock cribs.

- **Colonising.** Each soil-touching log piece rolls 2% a month from May to September, ×2 within 30 m of a stump colony. A stone foundation or peeled, dried logs give 0%.
- **Damage.** A colony in a structural log costs it 2% of its HP a month. Colonised woodpile armloads lose 10% fuel value and spread to the row above.
- **Killing a colony.** Replace the log, or pour 2 L of boiling water into the frass hole: 50% a pour.
- **Swarm day.** On one calm June afternoon above 20 °C, winged ants fly from every colony from 14:00 to 17:00. Ants fall on the water, so grayling rise ×1.5 that afternoon.
- **Food web.** Black bears tear open 1 ant stump a day in June. Brood is food under Grubs and ant brood.
- **Screen.** "Sawdust under the sill log. Carpenter ants." Block state `ants=none|colony` on log blocks.

### Yellowjackets

**Wasps that come for the drying fish.** Nests are paper balls in spruce or under eaves, or ground nests in old vole holes: 0–4 per km² of meadow and bank. A queen starts each in May; by late August it holds 800 workers.

- **Racks.** From July 15 to September 15, a drying rack within 300 m of a nest draws 5–30 wasps. The rack loses 0.05 kg a day, and work there costs 1 sting per 30 game minutes. A running smoker stops them at that rack.
- **Stepping on a nest.** Walking or clearing over a ground nest gives 5–15 stings in 10 s, 1 Condition each. A swollen hand slows hold-to-work 10% for 4 game hours, as for bees.
- **Ending one.** At night below 10 °C the workers are inside: a pot or soil over the hole kills a ground nest. Frost ends every nest by mid-October, and an old paper nest is tinder ×1.3.
- **Screen.** Workers in and out of a hole every 1–2 s, seen at 3 m; `entity.yellowjacket.buzz` (5 m). Bears dig nests in August and leave paper scraps.

### Caribou bot flies

**Flies that move the herd and spoil the hide.** Warble flies and nose bots (`yearofice:warble_fly` and `nose_bot_fly`, particles owned by Birds and the air › Flies on the caribou) harass caribou and reindeer from July 1 to August 15. This section owns the herd and hide rules (RECONCILE 57; S14). They fly 10:00–18:00 on days above 13 °C with wind under 6 m/s, and never bite people.

- **The herd.** Harassed caribou bunch tight, run in bursts of 200–500 m and stand on snow patches, aufeis and windy ridges, feeding half as much. On a hot, calm July day the herd is on the high ground.
- **The hide.** Larvae hole the back. The hide rule is the Bestiary's (Caribou hide by date; settled as S13 in Minecraft development › Settled across tabs): a hide taken June 1 – August 14, when the larvae cut out, carries 30–60 holes and is graded −40%, good for bedding and rawhide but not a parka; August 15 – September 30 is the parka window at full grade; from November 1 the hair breaks, −20%. The grade is the fur trade's `yearofice:pelt_grade`.
- **Larvae.** Skinning a caribou from April to June gives 50–150 warble larvae (`yearofice:warble_larva`, 1 g, 4 kcal each), eaten fried or used as grayling bait ×1.3. Journal: "Fried warble grubs. Better than they sound."
- **Reindeer.** Penned reindeer in July lose 2 fed a day unless a smoke smudge burns in the pen.
- **Screen.** `ambient.bot_fly.drone` within 20 m of a harassed herd.

### Snow flea and mourning cloak

**Snow flea.** Springtails swarm on snow from March 15 to April 30 on calm, sunny days above −2 °C, as dark pepper round tree trunks and in old prints. They mean the snowpack has warmed through. Rotten snow follows in 3–7 days, and snowshoe and sled travel slow. Once learned: "Snow fleas in the tracks. The snow will rot within the week." Spirits +2 on the first sighting each year.

**Mourning cloak.** Birds and the air › Small wonders owns this insect: the entity `yearofice:mourning_cloak`, its first flight (the first sunny day at 8 °C or more after April 1) and its winter find rate (1 frozen adult per 200 splits). This tab adds two rules. One on a birch drinking sap from broken twigs means the sap runs, and the winter find is the item `yearofice:dormant_butterfly`. Put back in the woodpile, it lives and gives Spirits +1; kept in the cabin, it wakes, beats at the window and dies in 3 days.

## Interaction verbs

**Twelve verbs work on animals, and each one is a rule, not a menu item.** Observe, track, call, bait, trap and hunt are already specified; this section adds scare off, haze, feed, follow and sketch, and sets which verb works on whom. Taming stays the wolf's alone, and raising stays the orphans' in Raising animals; the vanilla shortcuts are off (Bestiary › No vanilla mob mechanics).

### Who answers to what

Y works; — does not; T tame; R raise an orphan; L livestock care. Insects and the smallest animals are in their own sheets.

| Species | Observe | Track | Call | Bait | Trap | Hunt | Scare | Haze | Feed | Follow | Sketch | Tame or raise |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Moose | Y | Y | Y | Browse, lick | — | Y | Y | Y | Browse | Y | Y | R |
| Caribou | Y | Y | Y | — | — | Y | Y | — | — | Y | Y | R |
| Mule deer, Sitka black-tailed deer | Y | Y | Grunt, fawn bleat | Lick | — | Y | Y | — | — (winter feeding kills deer) | Y | Y | R |
| Dall sheep, mountain goat | Y | Y | — | Lick | — | Y | Y | — | — | Y | Y | — |
| Wood bison, muskox | Y | Y | — | — | — | Y | Y | — | — | Y | Y | — |
| Wolf | Y | Y | Y | Y | Y | Y | Y | Y | — | Y | Y | T |
| Coyote, red fox, arctic fox | Y | Y | Y | Y | Y | Y | Y | Y | Red fox | Y | Y | R (red fox kit) |
| Lynx | Y | Y | Y | Y | Y | Y | Y | — | — | Y | Y | — |
| Wolverine | Y | Y | Y | Y | Y | Y | — | Y | — | Y | Y | — |
| Black bear, grizzly | Y | Y | Y | Y | — | Y | Y | Y | — | Y | Y | — |
| Fisher, marten | Y | Y | Y | Y | Y | Y | Y | — | Marten | Y | Y | — |
| Ermine | Y | Y | Y | Y | Y | Y | Y | — | Y | — | Y | — |
| Mink, otter, beaver, muskrat | Y | Y | Otter, beaver | Y | Y | Y | Y | Mink, otter | — | Y | Y | — |
| Porcupine | Y | Y | — | Salt | — | Y | Y | Y | — | Y | Y | — |
| Snowshoe hare | Y | Y | Y | Willow | Y | Y | Y | — | Y | Y | Y | R |
| Alaska hare | Y | Y | — | — | Y | Y | Y | — | — | Y | Y | — |
| Red squirrel, flying squirrel | Y | Red | — | Y | Red | Red | Y | — | Y | — | Y | — |
| Ground squirrel, marmot, woodchuck | Y | Y | — | Seeds, greens | Y | Y | Y | Woodchuck | Ground squirrel | — | Y | — |
| Hen, goat, reindeer | Y | — | — | — | — | Y | — | — | L | — | Y | L |

### Scare off

**One loud act sends most animals away, at a price.** Scaring is the voice wheel's Shout (V), a clap, or banging a pot (hold use with a pot in hand). A flare from a supply drop is the loudest.

| Act | Radius | Every animal inside |
| --- | --- | --- |
| Shout or clap | 60 m | +30 awareness |
| Pot bang | 120 m | +40 awareness |
| Flare | 300 m | +60 awareness |

**How each answers once aware.** Prey take their flee response. Wolves retreat 90% of the time unless a test is running. A black bear leaves 85% of the time, 40% if it is food-conditioned. A grizzly leaves 60% of the time beyond 50 m; at a carcass or with cubs it goes to Warn 70% of the time instead. A moose leaves 50% of the time; a calving cow or rutting bull inside 50 m goes to Warn 40% of the time. Foxes and marten at food leave 70%, a wolverine 20%.

**The price.** A scare adds 0.3 disturbance to every water feature in its radius (Water, calls and encounters › The player at the water). Every big animal that heard it gets the Senses memory for that spot. Scaring a bear off a carcass the player wants never works in time: the bear has already fed.

### Haze

**Hazing teaches an animal that camp is a bad place.** A haze event needs an animal within 100 m of a bed, cache or fire. The player scares it from within 50 m, then advances 10 m, hits it with a thrown rock, or waves a torch at it at night.

- **Bears.** Each haze event lowers that bear's camp-visit chance by 20 points for 30 game days, to at most −60 in all. It also moves the bear's routes 100 m farther from camp. This runs against the +15 per food find in Camp bears. A bear that has found food 3 or more times is food-conditioned: hazing works at half strength. Hazing a sow with cubs inside 40 m, or a bear on a carcass, adds 20% to its charge chance.
- **Wolves.** A hazed pack avoids the camp for 14 days and does not run a test there in that time.
- **Foxes, coyotes, mink, otters, ravens and jays.** 7 days.
- **Moose and woodchucks.** A moose on a trail beyond 30 m leaves it 70% of the time. A hazed woodchuck shifts its feeding 100 m from the garden for 10 days.
- **Screen.** No meter shows. The journal records the event, and the next morning's tracks tell whether the bear came back.

### Feed

**Feeding is a choice with consequences.** Dropping food on purpose near a wild animal, or leaving it out where one feeds, is feeding.

- **Carnivores** (bears, foxes, coyotes, wolverines, marten): each feeding counts as a food find. Bears gain +15 camp-visit points; a fed red fox becomes a camp fox that steals 0.2 kg a night. Only the wolf-taming rules turn feeding into trust.
- **Birds, squirrels and ermine** at a suet feeder or meat scrap: Habituated after 1 day instead of 3.
- **Moose and felled browse.** A birch, aspen or willow top felled between November and March is 20 kg of browse. Within 500 m of a moose yard it draws a moose 40% of nights for 3 nights. This is how a winter camp gets a moose at the woodlot edge; it also puts a hungry moose on the camp trail.
- **Hares and cut willow.** A pile of cut willow draws hares within 150 m, which makes the pile a snare site at ×1.5 bait.

### Follow

**Shadowing an unaware animal maps its life.** The player keeps 60–200 m from a big animal (moose, caribou, sheep, bison, muskox, bear) for 30 game minutes while its awareness stays under 30. The messenger map then pins its next stop: "Cow moose, likely bedding at the willow island by 13:00." Three sessions on the same individual pin its home range ring and count as one encounter toward Known.

- **Getting caught.** Awareness passing 30 ends the session and sets the Senses memory, so a careless follower teaches the animal.
- **Packs and small carnivores** are followed by their tracks instead (Tracks and sign).
- **Research.** A completed follow logs 1 point under Wildlife count.

### Sketch

**The field notebook is the camera.** Holding right mouse (work) with the notebook in hand, aimed at an animal within 60 m (200 m with binoculars) draws a sketch over 20 game minutes. Its awareness must stay under 30, and it must stay in view 70% of that time.

- **Plates.** Each species has a plate, and each behaviour a further plate: feeding, bedded, nursing, fighting, calling, swimming, rut, play, kill. There are 160 plates in all.
- **What it gives.** The first plate of a species teaches its sign at once and logs 5 research points; each new behaviour plate logs 1. Sketches log at most 3 points a day. The rare plates (white moose, silver fox, white lemming, lynx kittens) give Spirits +5.
- **Screen.** The sketch fills line by line on the notebook page in the off hand; a twitch of the animal smudges a line and adds 1 game minute.

### Observe

**Watching tells what the animal is.** Watching an unaware animal for 60 game seconds, within 30 m or 300 m with binoculars, fills its inspect card. It shows species, sex, age class, condition from 1 (lean) to 5 (fat), and any old wound. The card's condition comes from the fat scale the model draws, so the player can learn to read it without the card.

## Herds, packs and families

### Moose yards

**Deep snow gathers moose into yards.** From December 15 to March 31, once snow passes 70 cm, moose with overlapping ranges gather in a yard. A yard is a willow flat, a 10–30-year burn, a river island or a hot spring basin. A home-valley yard holds 2–5 moose; an Interior yard at a hot spring holds 5–20.

- **The trail net.** A yard keeps 300–1,000 m of packed trail. Moose walk the trails at 1.6 m/s and avoid deep snow, which costs them 4× energy.
- **Short flight.** A yard moose flees only 50–100 m, along a trail, instead of the usual 200–400 m. It saves energy for the winter.
- **Browse.** Each moose eats 20 kg of browse a day. Browse share, the browse line and brooming follow Plants and fungi › Animals and plants (over 50% browse 3 winters running brooms a stem); a yard whose willows are broomed moves 1–2 km the next winter.
- **Wolves.** A pack whose territory holds a yard hunts it in snow over 60 cm (the staged wolves-and-moose hunt in Who hunts whom).
- **The player.** A snowshoe trail into a yard joins the trail net in 3 days. Standoffs on trails (Animal encounters) are twice as likely inside a yard. March yard moose are lean (25% fat), so the meat is a calorie answer but not a fat one.

### Caribou herds

**A herd moves like water around a still player.** Each herd has a lead cow on the route's waypoints and followers that keep 3–8 m apart, steered by three forces: follow the neighbour ahead, keep spacing, and avoid obstacles. A still, crouched player is an obstacle.

1. **Flowing past.** A herd walking toward a still player splits at 30–60 m and flows around in two streams, closing again 50 m behind. Curious individuals step out to 20 m to look.
2. **The wave.** A moving player within 100 m, or the player's scent, alarms one animal. Its neighbours within 15 m copy it after 0.3–0.8 s, so the alarm crosses the herd as a visible wave in 2–4 s.
3. **The bolt.** The alarmed herd runs 500 m, mills for 2–5 game minutes and resumes the route. A herd bolted 3 times in a day shifts its route by 1–3 km for the rest of the passage.
4. **At a ford.** The herd bunches on the bank for 5–20 game minutes until the lead cow enters, then all follow at a run. Spooked on the bank, it holds back 1–2 game hours.

**Seasons.** Bulls travel apart from June to August. Cows with calves lead the fall passage; bulls follow 1–3 days behind after the rut.

**Thousands in Minecraft.** The Interior's herd of 800–3,000 is a herd controller, not 3,000 entities. Up to 60 animals nearest the player are group members, real entities with a 4 Hz follower brain inside 96 blocks (S16); the rest are drawn by the client as instanced, AI-free impostors beyond 96 blocks, moving with the controller's flow field.

**B12 The caribou lead fence (Proposed; Ruhan may veto).** A multi-year build that funnels a migrating band past the player's blind. It is a solo-scale version of the Gwich'in drift fences, kilometres long, that led caribou to corrals; the docs credit them as the source, and the in-game names stay plain: lead fence and mouth.

- **Where and when.** On a caribou migration trail in the Caribou Barrens or wherever a herd's route crosses the home valley's edge. Bands use it in the spring window (April–May) and the fall window (September–October).
- **Pieces.** A lead-fence segment (`yearofice:lead_fence`, Bushcraft) is 4 m of 3 dead spruce poles and brush, built in 30 game minutes. Two wings meet at a mouth 15–20 m wide, and an existing pole blind or box blind (`yearofice:pole_blind`, `yearofice:box_blind`) stands at it.
- **How caribou respond.** A band travelling the route meets a wing within 30° of its heading and follows it toward the mouth at a walk, unless it scents the player: wind toward the band gives 0%.

| Wing length | Bands that follow |
| --- | --- |
| 20 segments (80 m) | 50% |
| 50 segments (200 m) | 80% |

At the mouth, animals pass 10–20 m from the blind for 30–90 s, the shot window. The band bolts after the first shot, under the herd rules above.

- **One snare, not hundreds.** A single caribou snare of six-strand babiche (`yearofice:caribou_snare`) can hang in the mouth: 18 m of babiche and 2 game hours. It holds one animal on 15% of band passes. This is the one exception to the caribou rule "never baited or trapped" (Caribou, above).
- **Upkeep and learning.** Snow load drops 5% of segments each winter, and windthrow drops more. Poles rot out in 8–10 years. A fence where 3 or more caribou were taken in one season sees its route used half as often the next year (Populations, local refill).
- **What the player sees and hears.** The lead cow turning along the wing, the ankle click of a walking herd at 30 m, and the band bunching at the mouth. The messenger's herd card names the fence: "Band of 40 following the north wing."
- **Failure cases.** Wind toward the band: none follow. A bolted band shifts its route 1–3 km for the rest of the passage, as written. Gaps from snow load let bands leak through. Overuse halves next year's traffic.
- **Ties.** Caribou herds (the flow, the wave and the bolt), Setting up the hunt (blinds, wind), Traps (babiche and the snare), Populations, Decay and time (pole rot), the migration calendar.
- **Ambition.** "At the fence": take a caribou at a lead fence you built (`ambition/at_the_fence.json`).
- **Build.** Block `yearofice:lead_fence` (Building and crafting, a fence-shaped block in `#yearofice:fences`); block entity `yearofice:caribou_snare` (Food and hunting, with the trap set, `trap/caribou_snare.json`). `ecosystem.herd.LeadFence` adds the wings as a steering force in the herd controller's flow field. Tuning `[hunt] lead_fence_follow_80m = 0.5`, `lead_fence_follow_200m = 0.8`, `caribou_snare_hold = 0.15`, `fence_learn_kills = 3`. Debug `/yoi herd fence <pos>`. Milestone M9, with migrations from M7 and pieces from M8. GameTest `yearofice:hunt/lead_fence_funnels` (batch `m9`): of 50 simulated bands crossing an 80 m-a-wing fence in neutral wind, 40–60% pass within 20 m of the blind; with wind toward the bands, none do.
- **Pillars.** Grow without an end (a multi-year build), The year changes the map (the migrations), Explore, don't dig (find the routes), Scarcity is the clock (one or two animals a pass).

### Wolf packs and roles

**Every wolf has a role, and the role decides what it does.** A pack is a breeding pair, 0–3 yearlings and the year's pups, 4–12 in all.

| Role | Count | Does |
| --- | --- | --- |
| Breeding female | 1 | Chooses den and rendezvous sites; leads travel 50% of the time; first at a kill |
| Breeding male | 1 | Leads travel 30% of the time; first to rush in a test; first at a kill |
| Yearling | 0–3 | Breaks trail in deep snow; flanks in a chase; eats after the breeders; disperses November – February |
| Pup | 0–6 | At the den, then the rendezvous site until October; travels with the pack from November |
| Sentinel | 1 at a time | Sits on a rise within 100 m of a resting pack or a kill; sees at ×1.3 range |

- **Travel.** Single file, 2–5 m apart, each wolf stepping in the leader's track until the line splits. In snow over 40 cm, the trail-breaker changes every 300–800 m.
- **Testing a moose.** The pack closes to 20 m. A moose that stands its ground is tested for 2–10 game minutes and left 70% of the time. One that runs is chased: two flankers 20–40 m to the sides, the rest behind.
- **Kill odds in a staged hunt.** Calf 40%; prime cow 8%; prime bull 5%; old or lean March moose 25%; all ×2 in snow over 80 cm. The abstract tier keeps its own kill rates.
- **At the kill.** The breeders feed for 1–2 game hours while yearlings wait at 20–50 m. Pups from August are fed at the rendezvous site.
- **Borders.** Territory edges carry twice the scent posts of the middle, and packs avoid the 1 km strip where two territories meet. Moose in that strip are ×1.3 as common, so a border is a good place to hunt.

### Mothers and young

**Every species with young follows one of three plans: followers, hiders and denners.** The plan decides where the young is when the player meets the mother.

| Species | Born | Young | Plan | Mother defends | With mother until |
| --- | --- | --- | --- | --- | --- |
| Moose | May 20 – Jun 10 | 1, twins 20% | Follower; hidden its first 3 days | Charges within 30 m to Aug | Driven off May 15 next year |
| Caribou | Late May on the barrens | 1 | Follower within 1 hour | Flees with it; never charges | Weaned by fall; stays in the herd |
| Mule deer | Jun 5 – Jun 25 | 1, twins 70% (yearling does 1) | Hider for 3–4 weeks; the doe feeds 100–300 m away and nurses 3–4 times a day | Stamps and strikes at a coyote, fox, lynx or the tamed wolf within 5 m (40 J); never the player | Weaned at 8 weeks; yearling bucks driven off at the next fawning |
| Sitka black-tailed deer | May 25 – Jun 15 | 1, twins 50% (yearling does 1) | Hider, as the mule deer | As the mule deer | As the mule deer |
| Dall sheep, mountain goat | May 25 | 1 | Follower on cliffs | Puts herself between; no charge | 1 year |
| Wood bison, muskox | Apr 20 – Jun 10 | 1 | Follower in the herd | The herd circle or ring | 1–2 years |
| Wolf | May 1 | 4–6 | Denner, then rendezvous | Bark-howls within 100 m of the den; moves pups if disturbed | 10–22 months |
| Grizzly | In the den; out in May | 1–3 | Follower at heel | Charges within 40 m | 2.5 years |
| Black bear | In the den; out in May | 1–3 | Follower; sent up trees | Charges within 20 m of a cub | 1.5 years |
| Lynx | Jun 1 | 0–4 | Denner, then follower | Never charges | March |
| Red fox, coyote, arctic fox | Apr 25 | 4–8 | Denner | Barks within 80 m; moves kits | September |
| Wolverine | Mar 15 | 2 | Snow denner | Never charges | August – September |
| Marten, fisher, ermine | Apr – May | 2–6 | Denner in a cavity | None | Late summer |
| Porcupine | May 20 | 1 | Hider at the tree base | Quills | 4 months |
| Snowshoe hare | Jun 1, Jul 10, Aug 20 | 4 | Hider, nursed once at dusk | None | 4 weeks |
| Beaver, muskrat, otter | Apr – Jun | 2–6 | In the lodge or den | Beaver tail-slap and bite rule | 1–2 years |
| Squirrels, marmot, woodchuck | May – Jun | 3–6 | Nest or burrow | Red squirrel carries kits away | Late summer |

**Four rules cover them all.**

1. **Young call first.** A calf bleat, cub bawl, kit whimper or pup yip plays at 60–150 m before the young is in view, which is the Mother with young encounter's cue.
2. **Hiders are not orphans.** A lone hare leveret, porcupette, 1–3-day-old moose calf or deer fawn under 4 weeks has a mother within 100 m (a deer doe within 300 m). The game counts a hider as orphaned only after 2 missed visits (2 dusks for hares, 1 day for a moose calf or a deer fawn). Watching for the visit is how the player knows.
3. **Separation calls.** Young separated from the mother call every 10–30 s for 2 game hours. The mother comes back to the last place she saw them. A player standing at that place is where she arrives.
4. **Losing the mother.** Young of followers and denners die within 10 game days without her, unless they are a raisable orphan in Raising animals. The population model counts them as juvenile deaths.

## The land's animals together

### Scenes between species

**Animals use each other, and each use is a sign the player can read.** These run in the Active tier when both animals are present; off-screen they shift the kill and theft rates already set. Hunts in plain view and the encounter director may stage any of them.

| Scene | Who | When and where | What happens | What the player gains |
| --- | --- | --- | --- | --- |
| The fox and the digging bear | Red fox, grizzly | Aug – Sep, ground-squirrel colonies on domes and barrens | On 30% of a grizzly's digs, a fox sits 20–40 m off and catches 1 in 5 squirrels that bolt from other holes | A fox sitting and staring at one spot means something big is there |
| Ravens with wolves | Raven, wolf pack | Nov – Mar, any pack | Ravens follow the pack on 70% of travel days and reach its kills within 10 game minutes, not 2 hours. They tug wolves' tails and hop off | Raven calls moving across the sky mark the pack's travel line from 1 km |
| Jays on a bedded moose | Gray jay; black-billed magpie (`yearofice:black_billed_magpie`, Bestiary) | Mar – May, moose in jay territory | On 20% of bedded moose, a jay lands on the back and pulls moulting hair for its nest | A jay hopping on a "log" in the willows is a bedded moose |
| The wolverine behind the pack | Wolverine, wolves | Nov – Mar | Follows the pack's trail on 30% of days; reaches kills 12–36 hours after them | Wolverine tracks on a wolf trail mean a kill ahead |
| Grizzly takes the kill | Grizzly, wolves | Apr – May, Sep – Oct | Half of wolf kills inside a grizzly's range are taken over. The pack circles and nips for 5–20 game minutes, then gives way | A bear on a wolf kill: the "never contest a bear" case |
| Black bear gives way | Black bear, grizzly | Berry season | A black bear leaves a patch or climbs when a grizzly is within 500 m | A black bear up a tree in August: look for the grizzly |
| Fisher and porcupine | Fisher, porcupine | Winter, Interior east | The circling kill in Fisher | The inside-out skin sign |
| Lynx kills a fox | Lynx, red fox | Hare-low years (hare multiplier under 0.5) | Each lynx kills 1 fox per 60 game days | Fewer fox thefts on the line in a hare crash |
| Hares on the moose trails | Snowshoe hare, moose, player | Snow over 40 cm | Hares run moose trails and the player's packed snowshoe trail | Snares on packed trails get the 4× trail bonus |
| Squirrels scold the marten | Red squirrel, marten | Any day | A squirrel scolds a marten passing within 50 m for 30–90 s, then flees up | A scold with no player near means a marten or lynx |
| Bears open middens | Both bears, red squirrel | Apr – May | A bear digs out 1–3 middens a day for stored cones | Dug middens show a bear's spring route |
| Arctic fox behind a grizzly | Arctic fox, grizzly | Interior crash winters and spring | Shadows at 100–300 m for scraps | A white dot following a brown one on the barrens |

### Fire, smoke, light, noise and weather

**Every animal reacts to the player's tools.** Radii are for a calm night; by day they halve. Habituated animals ignore them.

| Stimulus | Moose, caribou, sheep, bison, muskox | Bears | Wolves, coyotes | Small game | Fox, marten, ermine, jays |
| --- | --- | --- | --- | --- | --- |
| Lit campfire | Keep 120 m off; caribou 200 m, sheep 300 m | Never within 8 m; come to 30 m if cooking smell draws them | Circle at 30–50 m; never within 8 m | Keep 20 m off | Come to 10 m |
| Smoke plume (30 m downwind of a fire) | The player's scent in the plume reaches ×0.5 | Smoke from cooking or a smoker is a scent point | Same as prey | — | — |
| Wildfire within 2 km | Move 2–5 km away at a trot, crosswind of the front | Same | Same | Burrowers survive 70%, hares 50% | Den animals move young |
| Torch or lantern at night | Freeze 3–5 s in the beam, then leave | Ignore it unless it moves toward them | A lit torch ends a test or chase | Hares freeze 3–6 s | Eyeshine only |
| Chopping, felling, hammering | Each game hour of loud work adds 0.2 noise disturbance within 300 m, halving every 8 game hours; above 0.5, they route around | No effect | Note the camp; avoid by day | Freeze, then return | Come to look 20% of the time |
| Thunder | Bed down for 30 game minutes after the first strike | Bed down | Rest | Freeze | Hide |
| Blizzard | Moose bed in spruce; caribou lie down; muskox face into the wind | Denned, or bed | Rest; travel after it ends | In forms and burrows | Under snow |
| Below −40 °C | Moose active 2 hours a day; all big game near spring seeps | Denned | Hunt by day | Hares only at night | Ermine and voles under snow only |
| The player's camp | Keep 100 m off for 3 days after it is built, then 50 m; 100 m again while a fire burns | By the camp-bear and haze rules | By the test and haze rules | Habituation rules | Habituation rules |

**Animals are the first wildfire warning.** In the game hour before smoke reaches the player, 1–3 big animals pass within 100 m, moving crosswind away from the front. A moose trotting past at midday with no player cause is the land's warning.

### Animals and the player's camp

**Animals use what the player builds.** Each structure is a point of interest that the `StructureSensor` finds, so animals change their routes to it.

| Structure | Who uses it | How | Effect on the player |
| --- | --- | --- | --- |
| Packed snowshoe trail and cut trails | Wolves, lynx, foxes, coyotes, marten, hares, moose | After 3 days' use in snow over 40 cm a trail counts as a game-trail cell (cost 0.3). Wolves use it on 60% of their travel nights in its territory | Tracks on top of the player's own; wolves at the cabin door; standoffs on the trail; snares on it get the 4× bonus |
| Log bridges | Marten, fisher, ermine, foxes, squirrels on any bridge; bears and wolves on bridges 0.5 m wide or more | A bridge is a trail node; moose and caribou never use bridges and keep to fords | A pole set for marten on a bridge works at ×1.5 placement |
| Woodpile | Ermine, red-backed voles, red squirrels, hares, ants, wolf spiders, mourning cloaks | Ermine move in at 25% a week (half a weasel box); voles nest if it is within 5 m of a cache; squirrels cache cones in it on 40% of falls; hares shelter under it below −30 °C | Voles raise cache losses; ermine lower them; ants spoil the bottom row (Carpenter ants) |
| Latrine (outhouse) | Porcupines, moose, caribou | Porcupines chew the seat and door for salt: −10 durability on 25% of nights a porcupine's range covers camp. Urine spots draw moose within 300 m on 10% of days from May to July | Gnawing at night; a porcupine in the outhouse is an Animal in camp encounter |
| Cabin roof and attic | Red squirrels, flying squirrels, bats, bumblebees | Squirrels nest in a moss roof on 20% of falls; bumblebees nest in moss insulation on 10% of summers | Scampering at night; bees at the eaves |
| Canoe and paddles | Porcupines | Chew gunwales and paddle grips under the salt rule (Animal encounters) | −20 durability to one, unless stored under a roof |
| Garden | Hares, grouse, moose, voles, woodchucks, bears | The existing thieves plus the woodchuck | Fences, the buried skirt, a guarding wolf |
| Clearings and old camps | Moose, hares, grouse | Willow and birch regrow in cleared ground in 2–5 years | The camp grows browse: more moose near home in year 3 than year 1 |
| Meat pole, cache, racks, smoker | Every thief in Raids on the player's food, plus yellowjackets and arctic foxes | As set there | As set there |

### Sign to learn from

**New sign, each with a reading.** These join the catalogue in Tracks and sign and are learned by its rules. The reading is what the messenger says once the sign is learned.

| Sign | Where and when | Reading |
| --- | --- | --- |
| Browse line and broomed willows: twigs torn off rough at 1–2.5 m (Plants and fungi › Animals and plants) | Moose yards and river flats, all year | "Browse line. Moose have wintered here hard." Moose at or over K |
| Hare gnaw height and girdling on bark (Plants and fungi › Animals and plants) | Birch and willow, seen in summer | "Hares chewed here at snow level, about 90 cm." Last winter's deepest snow |
| Velvet strips on alders | Aug 25 – Sep 5 | "Velvet on the alders. A bull, this week." |
| Shed antlers | Dec – Mar; chewed by squirrels and porcupines when old | "A shed. Chewed: it fell last winter or before." |
| Green den mounds | Fox and coyote dens, June | "Bright grass on a mound. A den." |
| Doubled scent posts | Pack territory edges | "Many scent posts. A pack's border." |
| Porcupine skin turned inside out | Fisher range | Fisher kill |
| Lemming nests at snowmelt | Tundra and alpine | "Lemming nests everywhere. A big year coming for foxes." Reads L |
| Bog lemming runways | Deep sphagnum | Deep moss, bog holes |
| Qiviut on willows | Muskox summer range | Muskox within 300 m |
| Caribou on snow patches in July | Ridges and snow beds | Bot flies; the herd is high |
| Frass under a log | Punk wood, the player's sill | Carpenter ants |
| Wasp traffic at a hole | Banks and meadows, Jul – Sep | Yellowjacket nest: step around |
| Snow den hole with musk | North-slope drifts, Feb – May | Wolverine kits |
| Torn stump in June | Ant stumps | A black bear's route |
| Dark pepper on spring snow | Tree bases, Mar – Apr | Snow fleas: the snow will rot |

## Data and build

**One species file, one rig, one Brain.** Everything here follows the engine tab's conventions: our own data loaders under `data/yearofice/`, GeckoLib entities in `com.yearofice.ecosystem`, spawning only through the population model, and NeoForge GameTests.

### Registry ids and classes

- **New entities (12).** `yearofice:muskox`, `arctic_fox`, `fisher`, `alaska_hare`, `woodchuck`, `brown_lemming`, `collared_lemming`, `bog_lemming`, `singing_vole`, `jumping_mouse`, `wolf_spider`, `yellowjacket` (the stinging nest guards; its flying swarm is the Birds tab's `yellowjacket_swarm` particle). `mourning_cloak` belongs to Birds and the air (Conflicting ids rows 11 and 13).
- **Not entities.** `yearofice:carpenter_ant` is the log block state `ants=none|colony` plus worker particles; `yearofice:snow_flea` is a particle swarm; the caribou flies are Birds and the air's `warble_fly` and `nose_bot_fly` particles, stepped by `ecosystem.insects.CaribouFlies` (S14); `yearofice:spider_eyeshine` is a client particle.
- **New blocks.** `yearofice:yellowjacket_nest` (ground and paper variants, block entity holding worker count), `yearofice:den_mound` (fox, coyote and woodchuck mouths), `yearofice:snow_den` (wolverine). All three are placed and removed through World edits (`WorldEditService.submit`), never by direct block writes (RECONCILE 99).
- **New items.** `yearofice:qiviut`, `qiviut_liner`, `warble_larva`, `sleeping_jumping_mouse`, `dormant_butterfly`, `paper_nest`, `fisher_pelt`, `arctic_fox_pelt`, `alaska_hare_pelt`, `woodchuck_pelt`, `muskox_hide`, `muskox_horn`.
- **Entity classes.** `com.yearofice.ecosystem.species.<Name>Entity extends YoiAnimal implements GeoEntity`, where `YoiAnimal extends PathfinderMob`. Each rig has an abstract base in `ecosystem.species.rig`: `UngulateEntity`, `CanidEntity`, `FelidEntity`, `UrsidEntity`, `MustelidEntity`, `LargeRodentEntity`, `SmallRodentEntity`, `LagomorphEntity`, `PorcupineEntity`, all under `LandAnimal`. Voles, shrews, lemmings, pika and the jumping mouse use `SmallMammal`; hens, goats and reindeer use `Livestock` (Minecraft development › The entity class tree).
- **Systems.** In `com.yearofice.ecosystem`: `herd.HerdController` (caribou, bison, muskox, sheep bands), `pack.PackController` (roles, sentinel, borders), `kin.KinTracker` (mothers, young, hiders, separation), `YardManager`, `LemmingIndex`, `CampReactions` (fire, light, noise disturbance), `StructureUse`, `Shedding` (qiviut, goat wool, antlers), `looks.SeasonalLooks`, `insects.InsectColonies` (ants, yellowjackets), `insects.CaribouFlies` (shared with Birds and the air; S14), `verbs.Scare`, `verbs.Haze`, `verbs.Follow`, `verbs.Observe`. The sketch runs in `com.yearofice.story.FieldSketch`.
- **Client.** `<Name>Renderer extends GeoEntityRenderer`, models `rig.<Rig>Model extends DefaultedEntityGeoModel`, and `client.herd.HerdImpostorRenderer` for far caribou (instanced, no AI).

### Model and animation files

- **Geometry.** `assets/yearofice/geo/entity/<id>.geo.json`, one per species, with the rig's bone names.
- **Animations.** `assets/yearofice/animations/entity/rig_<rig>.animation.json` holds the shared set; `<id>_extra.animation.json` holds a species' extras. The datagen task `AnimationMergeProvider` merges them into `<id>.animation.json`, because a GeckoLib model points at one animation file.
- **Textures.** `assets/yearofice/textures/entity/<id>/<variant>_<coat>.png`, with coats `summer`, `mottled`, `winter`, `moult` as the species needs; variants such as `bull`, `cow`, `calf`, `cross`, `silver`, `cinnamon`. Velvet and eyes are render layers: `<id>/antler_velvet.png` and the emissive `<id>/eyes.png`.
- **Looks on the wire.** `SeasonalLooks` computes each entity's look on the server once a game hour and syncs four bytes through `SynchedEntityData`: coat (0–3), antler stage (0–255), fat (0–255), growth (0–255). The model applies them in `setCustomAnimations` as texture choice and bone scales, so no client clock is trusted.

```json
"looks": [
  { "kind": "coat", "summer": ["05-20", "09-25"], "winter": ["10-25", "04-15"], "blend_days": 20 },
  { "kind": "antler", "sex": "male", "bones": ["antler_l", "antler_r"],
    "shed": { "prime": ["12-01", "01-15"], "young": ["01-15", "03-31"] },
    "grow": "04-15", "velvet_off": ["08-25", "09-05"], "spread_by_age": [0.4, 0.7, 1.0, 0.8] },
  { "kind": "fat_scale", "bones": ["body", "chest", "hips"], "min": 0.9, "max": 1.15 }
]
```

### Spawn rules and the population model

**No vanilla spawner runs; the population model places every animal.** `PopulationSpawner` draws individuals from a region's count into seeded slots, as the engine tab sets. This tab adds four rules.

1. **Placement types.** `RegisterSpawnPlacementsEvent` registers `yearofice:burrow` (ground squirrels, marmot, woodchuck, lemmings, voles: at a burrow POI), `yearofice:trunk` (squirrels, marten, porcupine: on a log or leaf block within 1 m of a trunk), `yearofice:snow_den` (wolverine kits) and ON\_GROUND for the rest.
2. **Group caps in the Active tier.** Inside the 40 Active, big game may use at most 12, predators 8 and medium mammals 12. These are ceilings, not reservations, and with the birds' 14 and 6 they sum past 40 on purpose (S17). Outside the 40 sit the small-animal bubble (up to 30 within 32 blocks) and insects (up to 12 within 8 blocks), which do not count toward the 40.
3. **Seasons gate spawning.** Hibernators never spawn awake inside their sleep window; a hibernating individual is a record in its burrow, not an entity. Arctic foxes spawn in the Interior only in the winter after L of 0.3 or less.
4. **Herds and packs spawn whole.** A herd or pack enters the Nearby tier as one controller, which then promotes members by distance. Caribou beyond the 60 real entities stay impostors.

| New species | K | Born | Young | Juvenile survival | Adult deaths a year | Migration c | Respawn days |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Muskox | 40, the Interior herd | Apr 20 – Jun 10 | 0.6 | 60% | 8% | 0.05 | — |
| Arctic fox | Beyond: 1 per 20 km² of tundra | May 15 (Beyond only) | 4 + 8 × L | 30% | 40% | 0.2 | — |
| Fisher | 3 in the Interior | Apr 1 | 2.5 | 50% | 20% | 0.1 | — |
| Alaska hare | 15 per tundra district | Jun 10 | 6 | 40% | 40% | 0.05 | 10 |
| Woodchuck | 6 per region | May 15 | 4 | 50% | 25% | 0.05 | 14 |
| Lemmings, singing vole, jumping mouse | Index-driven (L or V), no count | — | — | — | — | — | — |

```json
{
  "entity": "yearofice:muskox",
  "habitat": ["barrens", "upland_domes"],
  "population": [20, 40],
  "home_range_m": 8000,
  "active": ["dawn", "day", "dusk"],
  "rig": "ungulate",
  "gaits": { "walk": 1.2, "trot": 3.5, "run": 14, "swim": 1.0, "step_height": 1.0 },
  "group": { "kind": "herd", "size": [8, 25], "defense": "ring", "ring_radius_m": 4 },
  "behavior": { "alert": 30, "ring_at": 70, "ring_trigger_m": 60, "sortie_m": [10, 20], "bluff": 0.4, "ring_break_after_min": 5, "ring_break_chance": 0.5 },
  "senses": { "sight": 150, "hearing": 40, "smell": 200, "fov": 300 },
  "young": { "plan": "follower", "defend": "ring" },
  "shed": { "item": "yearofice:qiviut", "window": ["05-15", "07-15"], "radius_m": 300, "season_kg": 1.5 },
  "sign": { "track": "hoof_round_13cm", "other": ["qiviut_tuft", "crust_crater"] },
  "population_model": { "k_interior": 40, "births": { "dates": ["04-20", "06-10"], "young": 0.6, "juv_survival": 0.6 }, "adult_deaths_per_year": 0.08, "migration": 0.05, "tracks_sex": true },
  "breath_s": 60, "butcher_table": "yearofice:butchering/muskox"
}
```

### Brain parts

**Memories.** New ones join the engine and water tabs' set.

| Memory | Type | Written by | Used for |
| --- | --- | --- | --- |
| `yearofice:herd_id`, `yearofice:herd_leader` | UUID | `HerdController` | Flow, wave, ring |
| `yearofice:pack_role` | Enum: breeder\_f, breeder\_m, yearling, pup, sentinel | `PackController` | Travel order, tests, feeding order |
| `yearofice:mother`, `yearofice:young` | UUID, UUID list | `KinTracker` | Defence radius, hider visits, separation calls |
| `yearofice:den_pos`, `yearofice:yard_pos` | GlobalPos | Region file, `YardManager` | Denning, yarding |
| `yearofice:fire_avoid` | BlockPos and radius | `FireSensor` | Keep-off radius |
| `yearofice:camp_aversion_until`, `yearofice:hazed_count`, `yearofice:food_conditioned` | Game time, int, int | `Haze`, feeding events | Camp visits |
| `yearofice:follow_other` | UUID | `ScavengerSensor` | Fox behind a bear, wolverine behind wolves |
| `yearofice:look_target` | Entity or pos | Every sensor | The head controller |

**Sensors.** Each runs on its own period to stay inside the land animals' 4.0 ms share of the 8 ms animal budget (Minecraft development › Performance; RECONCILE 91): `HerdSensor` (neighbours within 16 blocks, every 10 ticks), `KinSensor` (every 20 ticks), `FireSensor` (the heat index, every 40 ticks), `LightSensor` (the player's light cone, every 10 ticks at night only), `StructureSensor` (structure POIs within 64 blocks, every 200 ticks), `ScavengerSensor` (predators and carcasses within 300 blocks, every 200 ticks).

**Activities.** In priority order above Routine: `yearofice:ring_defense`, `yearofice:defend_young`, `yearofice:herd_flow`, `yearofice:yard`, `yearofice:follow_other`, `yearofice:nurse`. `yearofice:hibernate` replaces Routine inside a sleep window. The existing Flee, Warn and Charge keep their places.

### Sounds

**Every event named in the sheets,** under `yearofice:entity.<species>.<call>` in `assets/yearofice/sounds.json`, with volume equal to range ÷ 16 as in Water, calls and encounters › In Minecraft: calls. Ranges are in blocks and are used as given: `SoundBus.play` plays the event and emits its noise into the Scent and sound field, and the old 1,024-block cap is dropped (RECONCILE 79). Existing calls and Bestiary sounds keep their entries.

- **Moose:** `calf_bleat` 120, `warn_grunt` 40, `antler_clash` 1,000, `thrash` 300, `stick_snap` 80, `water_stream` 30.
- **Caribou:** `grunt` 200, `tendon_click` 30, `calf_bawl` 100, `herd_run` 600. **Dall sheep:** `clash` 1,000, `bleat` 150, `scree` 200. **Mountain goat:** `bleat` 100.
- **Muskox:** `bellow` 2,000, `clash` 2,000, `snort` 60.
- **Wolf:** `chorus` 5,000, `bark_howl` 400, `pup_yip` 200, `growl` 30.
- **Bears:** `huff` 40, `jaw_pop` 40, `woof` 100, `cub_bawl` 150, `roar` 600, `dig` 80, `tear` 120.
- **Lynx:** `yowl` 800, `hiss` 10, `kitten_mew` 40. **Wolverine:** `growl` 60, `tear` 60, `kit_whine` 30.
- **Foxes:** `red_fox.bark` 300, `red_fox.scream` 500, `red_fox.kit_whimper` 30, `arctic_fox.bark` 300. **Fisher:** `growl` 20. **Marten:** `chuckle` 20.
- **Small game:** `snowshoe_hare.thump` 20, `snowshoe_hare.scream` 200, `alaska_hare.thump` 20, `red_squirrel.scold` 50, `red_squirrel.cone_drop` 40, `porcupine.teeth_chatter` 10, `porcupine.whine` 60, `porcupine.gnaw` 30, `woodchuck.whistle` 200, `woodchuck.chatter` 20.
- **The small and the many:** `red_backed_vole.squeak` and `tundra_vole.squeak` 3, `masked_shrew.twitter` 2, `brown_lemming.squeak`, `collared_lemming.squeak` and `bog_lemming.squeak` 5, `singing_vole.trill` 30, `jumping_mouse.drum` 5, `yellowjacket.buzz` 5, `ambient.bot_fly.drone` 20, `hen.cluck` 20, `hen.rooster_crow` 300 at dawn, `goat.bleat` 60, `reindeer.grunt` 40, `reindeer.bell_clank` 60 while a belled reindeer moves. Gaps filled from Minecraft development › Gaps the review found: `ermine.trill` and `least_weasel.trill` 15 (the alarm tell), `mink.hiss` 10, `muskrat.plop` 20 (diving at alarm), `flying_squirrel.chirp` 20 (night, at the nest tree); the wolf spider is silent by design (RECONCILE 95).

### Loot tables and carcasses

- **Death.** `data/yearofice/loot_table/entities/<id>.json` stays an empty file and `doMobLoot` is false (S9, RECONCILE 41). `ecosystem.wound.DeathPipeline` turns every death into a carcass entity of the species' size class; a tiny carcass is pocketed whole as `yearofice:whole_game` with its species (a lemming, a vole).
- **Butchering.** `data/yearofice/loot_table/butchering/<id>.json`, with the Bestiary's `yearofice:season_fat` function. Hide quality by date is the fur trade's `yearofice:pelt_grade` function, not a separate `hide_quality` (S13): caribou and reindeer −40% from June 1 to August 14 for warble holes and −20% from November 1; winter pelts prime by the dates in the sheets.
- **Shedding.** `data/yearofice/loot_table/shed/<id>.json` (qiviut, goat wool, antler sheds), rolled once a game day by `Shedding` into tuft or shed items on the ground of the range.

### Milestones

- **M5 (Food and goals).** Woodchuck and Alaska hare; jumping mouse finds; warble larvae; yellowjackets at the racks; Observe and Sketch.
- **M6 (Danger and big game).** Every rig, animation set and gait in the mob sheets; Scare, Haze, Feed and Follow; wolf pack roles; caribou herd flow and impostors; the muskox ring; mothers and young; fisher and arctic fox.
- **M7 (The year).** Seasonal looks; moose yards; the lemming index; bot flies, snow fleas, the mourning cloak, wolf spiders and the ant swarm day; hibernation windows.
- **M8 (Building and outposts).** `StructureUse` (trails, bridges, woodpile, latrine, roof, canoe, clearings) and carpenter ants in log pieces.

### GameTests

Each is a NeoForge `@GameTest` in `com.yearofice.gametest.LandAnimalGameTests`, named `yearofice:ecosystem/<test>` (Conflicting ids row 22), waiting on game time through the clock, and run headless with `./gradlew runGameTestServer`.

1. **The herd flows around a still player.** 40 caribou walking at a still, crouched test player pass with none inside 20 m on at least 95% of 200 trials and close ranks within 80 m behind. A walking player 60 m away with the wind blowing to the herd makes at least 90% of the herd bolt within 4 s.
2. **Yard moose save energy.** In 90 cm of snow, a yard moose approached to 40 m flees 50–100 m on at least 95% of 500 trials, with at least 90% of its steps on packed trail cells.
3. **Hiders are not orphans.** Over 1,000 simulated leverets, the orphan flag is never set while the doe keeps visiting at dusk, and is always set after 2 missed dusks. Between 10% and 20% of found leverets end up orphaned.
4. **Fire holds them off.** Over 1,000 simulated nights with a lit fire, no moose, bear or wolf comes within 8 m, and the median moose distance is at least 120 m.
5. **Hazing beats two food finds.** A bear with 2 food finds (camp-visit chance 30%) hazed 3 times makes no camp visit in the next 30 game days on at least 99% of 500 seeds.
6. **Ants need soil.** Over 10 simulated years, a sill log on a stone foundation never gets a colony on 100 seeds. A log touching soil gets one by year 5 on 40% ±8% of seeds, and on 64% ±8% within 30 m of a stump colony.

### Debug commands and tuning

**Every system in this tab has a `/yoi` debug command, and its global numbers sit in `yearofice-common.toml`** (Ruhan's MDK rules, RECONCILE 118). `/yoi` is the debug root and needs operator permission (level 2); `/yearofice` stays the long form for operator commands.

| Command | Does |
| --- | --- |
| `/yoi animal <target> info` | Prints the record: species, sex, age class, tier, awareness per source, activity, drives, wound, fat, trust or bond |
| `/yoi animal <target> aware <0–100>` | Sets awareness toward the nearest player, to test Alert, the species response and the charge gate |
| `/yoi herd <id> info`, `bolt`, `ring` | Leader, members, impostors; forces the alert wave or the muskox ring |
| `/yoi pack <id> info`, `test <player>` | Roles, territory and hunt plan; starts a winter test on a player |
| `/yoi kin <target>` | Mother, young, hider visits and the orphan flag |
| `/yoi looks <target> <MM-DD>` | The coat, antler stage, fat and growth that date would give |
| `/yoi yard`, `/yoi lemming set <L>`, `/yoi insects <kind> on\|off` | Yard members and trail net; the lemming index; insect colonies and particles |
| `/yoi haze <bear> info` | Camp-visit chance, haze count and the food-conditioned flag |
| `/yoi spawn <species> [count]` | Counted test spawns (Minecraft development › Tiers, spawning and despawning) |

**Tuning.** Per-species numbers stay in the species files, so a data pack can change one animal. The global multipliers live under `[ecosystem]` in `yearofice-common.toml`: `animal_numbers` (0.5–2.0, the realism slider), `predator_boldness` (1.0), `charge_chance_scale` (1.0), `scare_radius_scale` (1.0), `haze_days_scale` (1.0) and `insect_pressure` (1.0). The realism presets (Blueprint › Realism presets) set them together.

**Day length.** Every rate in this tab is per game minute, hour or day, so the world option for day length (24–72 real minutes, default 24; RECONCILE 117) rescales them all. Only animation times and the close-clock moments are in real seconds.
