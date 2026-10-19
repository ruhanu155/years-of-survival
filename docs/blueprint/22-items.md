# Items

This tab covers everything the player can own, use or find, and how items wear, combine and go missing. Recipes stay in Crafting and building in depth; the crate, clothing, tackle, weapons and traps stay in the main tab. It adds the item model, about 70 new items, and a master table of 175 items with Minecraft registry ids.

## Item properties

### The eight properties

Every item stack carries up to eight properties. Weight and stack size are fixed per item; the other six change with use, weather and time. Each one shows on the icon or the tooltip, so nothing about an item is hidden.

| Property | Range | Who has it | What changes it | On the icon | In the tooltip |
| --- | --- | --- | --- | --- | --- |
| Weight | kg per unit | Everything | Nothing; water and wet add to it | Nothing; the load meter sums it | "2.5 kg" |
| Stack | 1, 8 kg, 20 or 50 per slot | Everything | Nothing | The count | — |
| Condition | 0–100% | Tools, weapons, clothing, containers, lights | Use, cold, wet, animals, rust | Vanilla durability bar (`minecraft:damage`), green to amber to red; cutting tools add a thin `yearofice:edge` line (0–120) above it | "Condition 64% · edge 22 (dull)" |
| Quality | Crude, Good, Fine | Anything crafted, and found goods | Practice at craft time; rework | A grey notch top left for Crude, nothing for Good, a white corner tick for Fine | "Fine · Woodcraft" |
| Wet | 0–100 | Anything porous: tinder, wood, hide, cloth, paper, food, matches | Rain, water, melting snow, sweat | Droplet outline at 20, filled droplet at 60 | "Wet 70 · dries in 40 min by a fire" |
| Freshness | 100–0 | Food, bait, raw hides | `yearofice:fresh` on its holder's decay clock (Decay and time › The decay clock) | Bottom bar (food has no condition) | "Aging · safe 1.2 days" |
| Temperature | Frozen, Cold, Ambient, Warm | Food, liquids, glue, lard, hides, boots, batteries | Air, fire, the inner pocket | A frost rim when Frozen; an orange glow when Warm | "Frozen solid · thaws in 20 min by a fire" |
| Scent | 0–3 | Meat, fish, guts, lard, bait, fresh hides | Kind of item; a closed container halves it | Nothing | "Strong smell" at 2 or more |

### Quality

**Quality comes from practice.** When a craft finishes, the game rolls its quality from the skill word in the work preview. A dull tool or work in the dark moves the roll one row up.

| Skill word | Crude | Good | Fine |
| --- | --- | --- | --- |
| Clumsy | 60% | 40% | 0% |
| Handy | 15% | 75% | 10% |
| Practiced | 0% | 70% | 30% |

**What quality changes.** Every category has one main stat, and quality scales it. Durability scales the same way for all of them.

| Category | Main stat | Crude | Good | Fine |
| --- | --- | --- | --- | --- |
| All crafted items | Durability | ×0.7 | ×1.0 | ×1.3 |
| Edge tools | Job time | ×1.1 | ×1.0 | ×0.95 |
| Bows | Aim sway | +10% | — | −5%, and arrow speed +3% |
| Arrows | Cast scatter | +20% | — | −10% |
| Garments, blankets | Warmth | ×0.9 | ×1.0 | ×1.1 |
| Containers | Seal | A Crude bucket leaks 0.5 L a game hour; a Crude box lets wet in at half rate | Sealed | Sealed, and mice fail 50% more often |
| Snares | Break chance | +15% | — | −10% |
| Cordage | Rated strength | 2.8 kg, as Clumsy cordage in Tackle | 3.4 kg | 4 kg |

Cordage quality is the same practice rule Tackle already uses, shown as a word. Food has no quality; dishes have doneness. Found factory goods have a fixed quality, set in their loot table.

**A Fine item is worth showing.** A Fine bow, garment or carved bowl placed on a shelf counts as one decoration in the Spirits table. Fine items also go to the year card: "Fine things made: 14."

### Wet

**Gaining wet.** Uncovered rain adds 20 an hour, falling in water sets 100 at once, and snow melting on an item indoors adds 30. Items in a birch-bark box, a sealed tin or a fish-skin bag stay dry. The hide pack sheds half the rain; the crate backpack sheds none.

**Drying.** By a fire an item dries 100 to 0 in 1 game hour, on the drying line in 4, and in the pack on the body at 5 an hour. These are the clothing rates, so one rule covers both.

| Item kind | What wet does |
| --- | --- |
| Tinder, matches, char cloth | Will not catch above 40 (dryness under 60). A match above 20 fails every time |
| Wood items, bowstrings | The existing rules in Bow care and Fire |
| Rawhide, leather | Stretches: lacing sags as in Tool durability. Dried in under 2 game hours by a fire, it stiffens and loses 10 condition unless worked with 20 g of lard |
| Iron and steel | Rusts (see Item decay) |
| Paper: pages, the chart, the paperback | Above 50 and dried by a fire, it cockles. Cosmetic only; story text never becomes unreadable |
| Flour, sugar, tea | Above 30, freshness starts: flour spoils in 10 days, sugar clumps into a lump that still counts |
| Salt | Loses 10% of its weight an hour while above 50 |

### Temperature

**Items keep their own temperature state.** It moves toward the air around them on a frost clock. An item holding water or fat gains frost points each game minute equal to how far the air is below 0 °C, and freezes at 600 points per kg. A 1 kg bottle at −10 °C freezes in 60 game minutes, the main tab's "within an hour"; at −40 °C it takes 15.

**Thawing.** Air above 0 °C takes points off at the same rate. A fire within 2 m counts as +30 °C, so a frozen bottle thaws in 20 game minutes, as Fire, water and light says.

**The inner pocket.** Two slots on the body panel sit inside the coat. Items there hold at +20 °C while warmth is 30 or more. That is where the bottle, the headlamp, the messenger, a bow before a cold shot and a pair of boots before bed belong.

| Frozen item | Effect | Fix |
| --- | --- | --- |
| Water in a bottle or pot | Cannot be drunk; ice gives water at the snow-and-ice rates | Thaw by a fire |
| Glass bottle over 80% full | Breaks: it becomes 3 glass shards | Fill bottles to 70% in winter |
| Meat, whole fish | Cannot be cut or filleted; hatchet only, as in Spoilage model | Thaw 1 h per kg |
| Lard, glue | Lard cannot fry or cure; pitch glue shatters on a strike, so a broadhead glued at −30 °C loosens 10% of the time | 10 game minutes by a fire |
| Boots, moccasins, mukluks put on frozen | Feet's "safe to" falls by 10 °C for 30 game minutes | Keep them in the inner pocket or by the stove |
| Hides | Cannot be scraped | Thaw in a warm room, 4 game hours |
| Batteries | Headlamp rules in Light sources in detail | Inner pocket, 10 game minutes |

**Warm items.** A Warm item fades to Ambient over 30 game minutes. Hot stones and boiled water are already in the main tab. The hot-water bottle in Camp comforts is the one new use.

### How a stack reads

**One tooltip, top to bottom.** Name and quality, then weight, then the changing properties in a fixed order: condition, freshness, wet, temperature, scent, then what it is for. For food that last line is the food line: kcal for the stack, fat and protein in grams, vitamin C in C-days and the stomach fill of one portion (Food values; video row 32), as in "Dried moose · 0.5 kg · Hot-smoked · safe 24 days · 1,440 kcal · fat 21 g, protein 313 g · C 0 · stomach 28%."

"Cast-iron skillet · Good · 2.5 kg · Condition 35%, rusted · Season it with 50 g of lard at a fire · Fries with half the lard."

"Arvid's breakup board · Keepsake · 4 kg · 34 dates, 1938–1971 · Hang it at Station Nine to keep the record going."

**Stacks merge only when they read the same.** Freshness merges in bands of 1/40 of the food's life (2.5 points; the tooltip rounds to 5) and takes the lower value, wet in 20-point bands, and temperature by state. A Crude arrow and a Fine arrow never share a slot.

## Found and salvaged items

### What this adds

The main tab's Set pieces section lists each site's key salvage: the wreck's panels, cable, window and battery; the trapper cabin's stove, stretcher board and trapline board; the camp's pick head, nails, scrap iron, kettle and gold pan; the lookout's binoculars and fire-finder. Those stay as written. The tables below are everything else in each place, with the condition it is found in and what it is for.

Each site's loot is a fixed list plus 1–3 rolled extras, drawn once at world creation from the site's seed. A site never refills. Items marked † lie under debris that takes the stated time to clear.

### Station Nine

The summer crew, Elin and three winters of mice left the station full of small things. Most of it is in the cabin, the yard and the midden behind it.

| Item | Count | Found as | Weight | Use |
| --- | --- | --- | --- | --- |
| Dial thermometer | 1, screwed to the door frame | Good, 100% | 0.2 kg | Reads air, room, water and smoker temperatures with no battery (Tools and instruments) |
| Coleman lantern | 1, on the shelf | Good, 80%; 0.3 L of white gas, 2 spare mantles | 1.6 kg | The brightest light in the valley for about 2 hours; then a globe and frame to salvage |
| Enamel mugs | 2 | Good, chipped | 0.2 kg each | Heats 0.3 L in coals; tea without the pot |
| Elin's hand lens, 10× | 1, on the table beside page 1 | Fine | 0.03 kg | Plant identification and a sun-lit fire |
| Sewing kit in a tin | 1, in the bunk drawer | Good | 0.2 kg | 5 steel needles, 50 m of cotton thread, 20 buttons |
| Duct tape | 1 roll, half used | Good, 8 uses | 0.15 kg | Fast field repair (Item interactions) |
| Tin cans | 6, in the midden | Crude, 60% | 0.05 kg each | Cups, char-cloth tins, can alarms, the birch-tar still |
| Fencing wire, 2.5 mm galvanized | 20 m on the old garden fence; 20 game minutes to unwind | Good | 0.1 kg per m | Heavy snares that never rust, lashings, a beaver lodge snare |
| Plastic jerrycan, 20 L | 1, by the woodshed | Crude: cracked high, holds 15 L | 1.2 kg | Water at the cabin; never on a fire; splits if frozen full |
| Hand ice auger, 15 cm † | 1, under the north roof; 4 roof logs, 1 game hour to clear | Good, 70%, edge 40 (dull) | 3.5 kg | Elin's ice-thickness auger: a hole through 1 m of ice in 15 game minutes |
| Tok's bed | 1, a folded wool army blanket by the stove | Crude, 40% | 1.8 kg | A wool blanket at half warmth; a wolf pup settles on it (Keepsakes) |
| Photograph | 1, pinned above the bunk | Keepsake | — | Elin and a grey pup on the river ice, year 2 |
| Summer crew's paperback | 1, a western with no cover | Good | 0.3 kg | Reading by lamplight (Camp comforts) |

**The Coleman night.** The lantern lights 10 m around it on High and adds 150 W to a room, burning 0.15 L an hour. On Low it lights 7 m for 0.075 L an hour. The 0.3 L in it is two bright hours or four dim ones, and the game says so on pickup: "0.3 L of fuel. About 2 hours on High." Dropping it (Q) breaks a mantle half the time; without a mantle it gives no light.

**Fuel from the wreck.** The wreck's wing tank holds stale avgas in two of its three variants (below). Coleman lanterns ran on it in a pinch, and here too: avgas burns at the same rate but fouls the generator, costing 10% condition an hour. A careful player gets about 8 more hours of bright light from the wreck. That is the one link between the two finds, and players who notice it feel clever.

### The bush-plane wreck

J.O. walked out with the survival kit, so the pouch is nearly empty. What she left is what a pilot would leave.

| Item | Count | Found as | Weight | Use |
| --- | --- | --- | --- | --- |
| Sectional chart, 1978 edition | 1, folded in the door pocket | Good, water-stained | 0.1 kg | The valley's waters from the air (below) |
| Wedding ring | 1, under the rudder pedals; found only by holding E on the cockpit floor for 1 game minute | Keepsake | 0.005 kg | A story item (Keepsakes) |
| Hook tin | 1, in the kit pouch | Good | 0.05 kg | 6 steel hooks and 20 m of braided line rated 9 kg, at condition 50 |
| Police whistle | 1, on the pouch zip | Good | 0.02 kg | A signal audible to 1 km |
| Seat cushions | 2 | Crude, 50% | 0.6 kg each | Foam for a sleeping pad or a pillow |
| Engine oil | 1 can, about 0.5 L | Good | 0.6 kg | Rust-proofing, 10 uses; a smoky lamp fuel |
| Avgas | 3 L in the wing tank, in the gravel-bar and lake variants only; 0.5 L in the treetop one | Stale | 0.7 kg per L | Fire accelerant and Coleman fuel |

**The chart.** Unfolding it on the messenger map (M, then drag the chart onto the map) draws the main river, the big lake and the larger creeks across all 16 regions as faint outlines. It reveals nothing else, so the map still has to be walked. It also marks two gravel bars long enough for a 150 m landing strip, which saves the landing-drop survey in The radio.

**Burn it or keep it.** The chart is also dry paper: one sheet makes 3 tinder bundles at dryness 100. A player who burns it on a wet night in week 2 gets a fire and loses the river map for good. The journal remembers: "Day 11. Burned the pilot's chart to get the fire going. The river is still out there."

**Avgas as accelerant.** Pouring 0.05 L on a laid fire makes tinder catch at 95% even at wet 80. Pouring it on a burning fire gives a minor burn 50% of the time, and the messenger warns once: "Avgas on a lit fire. Stand back first."

### Arvid Lund's cabin

Arvid's tools are worn but good. He was a careful man, and the cabin shows it.

| Item | Count | Found as | Weight | Use |
| --- | --- | --- | --- | --- |
| Cast-iron skillet | 1, on the stove | Good, 35%, rusted | 2.5 kg | Season it (30 game minutes, 50 g lard, at a fire) to 100%; then it fries anywhere with half the lard |
| Spent .30-06 casings | 12, in a jam jar on the shelf | Good | 0.012 kg each | Brass whistles; no firearms exist |
| Crooked knife | 1, on a nail by the stove | Good, edge 50 | 0.15 kg | Carving: bowls, spoons, paddles, snowshoe frames |
| Axe puck | 1, in a tobacco tin | Good | 0.2 kg | The sharpening kit (Tools and instruments) |
| Leghold traps, seized | 2, on the wall | Crude, 10% | 1.1 kg each | Salvage only: 0.3 kg of iron and 1 flat spring each |
| Birch chair, babiche seat | 1 | Crude, 40%, seat sagged | 4 kg | A camp chair; relace it with 6 m of rawhide |
| Breakup board | The wall plank holding his pencilled dates; pried out with the hatchet, 5 game minutes | Keepsake | 4 kg | Carried to Station Nine to continue the record |

**The board comes home.** The 34 breakup dates in Story are written on one plank beside the lynx tally. Prying it out leaves the tally in the door frame. Hung on the station wall by the radio, it becomes the ice record's physical page: each breakup the player logs adds a pencilled line, the first in a new hand.

Each new line gives Spirits +3 and counts as a trophy. Kari Marsh's spring call changes once the board hangs at the station: "She'd have liked that. Arvid's numbers and hers, on one wall."

### The gold-rush mining camp

The miners left heavy iron tools that a survivor can still use, and the bottles and cans of six winters.

| Item | Count | Found as | Weight | Use |
| --- | --- | --- | --- | --- |
| Crosscut saw, 1.8 m, one handle left | 1, hung on the bunkhouse wall | Good, 50%, rusted | 3 kg | Bucks rounds over 35 cm thick, which the bucksaw cannot |
| Froe | 1, in the tool box | Good, 60% | 1.2 kg | Splits planks and shakes with a mallet |
| Adze head | 1 | Good, 70%; needs a tool handle | 1.5 kg | Hews logs flat |
| T-auger, 2.5 cm | 1 | Good, 50% | 1.4 kg | Bores peg holes |
| Round file | 1, in the tool box | Good | 0.1 kg | Sharpens saw teeth and the ice auger |
| Amber bottles | 4–8 in the midden; 1 in 3 cracked | Good or Crude | 0.3 kg each | 0.7 L of water; glass shards for knapping |
| Tin cans | 6, rusted | Crude, 40% | 0.05 kg each | As at the station |
| Ore sacks | 3 | Crude, 50% | 0.4 kg each | A 9-slot sack that carries 15 kg on a pack frame lash point |

### The fire lookout tower

The lookouts read weather and watched for smoke for 32 summers. Their instruments are still on the cab wall.

| Item | Count | Found as | Weight | Use |
| --- | --- | --- | --- | --- |
| Aneroid barometer | 1, on the cab wall | Good, 90% | 0.6 kg | Battery-free forecasting with practice |
| Heliograph mirror, two-sided with a sighting hole | 1, in the desk | Fine | 0.15 kg | Signals to the plane on drop days |
| Pulaski | 1, by the door | Good, 40%, handle cracked | 2.3 kg | An axe and a grub hoe on one handle: firebreaks at double speed |
| Copper ground cable | 10 m on the lightning rod; 20 game minutes with the hatchet | Good | 0.12 kg per m | Lashings and leaders that never rot or rust |
| Canned peaches, 1984 | 3, on the shelf | Each 30% bulged | 0.45 kg each | 400 kcal, sweet, Spirits +5 (once a day); a bulged can carries botulism |
| Wool blanket | 1, moth-eaten | Crude, 50% | 2 kg | +2 °C asleep (the catalogue's new blanket gives +3) |

**Read the can.** A bulged can says so in its tooltip: "Lid bulged. Don't." Eating it anyway gives botulism on every run (Decay and time › Sour fish and botulism; problem `injury/botulism.json`). Boiling the contents 10 game minutes in a pot destroys the toxin, as it does for sour fish, and the tooltip then reads "Boiled. Flat, but safe." A flat can is safe on every run. This is the Everything readable pillar applied to food.

### The roadhouse and Arvid's caches

**The roadhouse at the hot spring.** Besides the guest book and the 60 kg stove:

| Item | Count | Found as | Weight | Use |
| --- | --- | --- | --- | --- |
| Dutch oven with lid | 1 | Good, 60% | 5 kg | Bakes bread and bannock buried in coals |
| Salt crock | 1, with 2 kg of hard rock salt | Good | 2.5 kg | The only salt before the first supply drop |
| Bearpaw snowshoes, lacing rotted | 1 pair | Crude, 20% | 2 kg | Relace with 30 m of rawhide (6 game hours) instead of building frames |

**Arvid's caches.** Each cache keeps its tool upgrade from the main tab's list and adds one extra, drawn without repeats.

| Extra | Found as | Use |
| --- | --- | --- |
| 3 tallow candles | Fine | 12 h of light |
| 6 matches in a sealed tin | Dry | A second chance at a match fire |
| 10 m of babiche (rawhide lacing) | Good, stiff | Snowshoe repairs before the first big hide |
| 1 kg of rock salt in a jar | Good | Salt for smoking |
| Moose-hide mitts | Crude, 30% | Hands safe to −24 °C after a 20-minute repair |

**"Leave the can."** The lard can itself is a 4 L pail with a bail. Taking it costs nothing but the note. Leaving it and putting any one item back turns the cairn into a 6-slot store that wolverines, foxes and ravens cannot open. A player who leaves every can gets a chain of small, safe stores across the old trapline, which is what Arvid built them for.

### Elin's last camp

At Glacier Cirque, beside page 23, lies her tin cup with "E.M." scratched in the base. It is a keepsake. The game puts nothing else of hers in the land.

### What the river brings

**Each breakup sets out 3–6 drift finds** at snags and log jams within 4 km downstream of the core, drawn from the table below. A spring flood doubles the count. They are real objects, placed by the seed, and Pete Lund may mention one: "Saw a blue barrel on the bar below the bend."

| Drift find | Weight | Chance per find | Use |
| --- | --- | --- | --- |
| Driftwood pile | Armloads | 30% | Dry fuel, as in the main tab |
| Canoe paddle, spruce, worn | 1 kg | 10% | A Crude paddle at 50% |
| Orange net floats | 0.05 kg each, 4–8 | 12% | Gill-net floats that never waterlog |
| Blue poly barrel, 55 L, with lid | 4 kg | 6% | A rodent-proof 18-slot barrel at the cabin |
| Tangle of commercial net | 6 kg | 8% | 20 m of mesh: 1 gill-net segment after 2 h of untangling |
| Moose antler shed | 4 kg | 12% | Bone tools and a trophy |
| Plastic jerrycan | 1.2 kg | 8% | As the station's, uncracked |
| Drowned caribou | Carcass | 4%, years 2+ | 30% of its meat still Aging; the hide at rawhide grade |
| An item the player lost upstream | — | Always first | Anything that floats, placed at the next snag downstream |

## Tools and instruments

### Instruments

**Instruments read the world without the battery.** The messenger already shows air, wind, feels-like and the forecast, but each look costs battery. Instruments give some of that for free, and some things the messenger never shows.

| Instrument | Get it | Weight | What it does | Ties to |
| --- | --- | --- | --- | --- |
| Dial thermometer | Station Nine | 0.2 kg | Held: air temperature to 1 °C. Placed in a room, smoker or cellar: that space's temperature on its card. Dipped: water temperature for fishing | Smoking, root cellar, fishing, battery |
| Aneroid barometer | Lookout | 0.6 kg | Reads pressure and its 3-hour trend; feeds the Forecast skill | Weather, fishing's ×1.4 front bonus, battery |
| Compass | Crate | 0.05 kg | As in Danger in detail: a bearing at the bottom of the view | Whiteouts, the chart |
| Binoculars | Lookout | 0.9 kg | 4× zoom, as in Set pieces. Also counts animals for radio requests from 400 m instead of 100 m | Radio logs, hunting plans |
| Hand lens, 10× | Station Nine | 0.03 kg | Plant identification up close in 1 game minute instead of 3. In direct sun above 20° elevation, focuses on char cloth or dry tinder: a tinder flame in 2 game minutes at 80% | Foraging, fire |
| Snow stake | Made: a pole marked every 10 cm | 1 kg | The weather log's snow depth | Radio logs |

**The thermometer in the smoker.** Hot smoking holds 60–80 °C. With the thermometer placed in the smoker, its card shows the temperature and warns at 90 °C, which halves the flare-up loss for that batch. A cold-smoke batch above 30 °C cooks instead of curing; the card says so.

**The Forecast skill.** Reading the barometer between 06:00 and 10:00 and saying a word (Fair, Change, Storm) records a forecast for tomorrow. The game scores it the next morning. A right call is 1 rep and a wrong one 0.5; N is 30.

| Skill word | Your forecast is right | Sky signs needed |
| --- | --- | --- |
| Clumsy | 55% | None |
| Handy | 70% | Ring round the sun or moon adds +5% |
| Practiced | 80%, the messenger's own figure | — |

A Practiced player stops opening the messenger forecast in midwinter, which saves 0.5% battery a look for the radio. The lookout log's rules of thumb count as 3 reps when read.

### Edge and woodworking tools

**Each tool makes one job faster or opens one product.** Tools already in Crafting and building in depth (drawknife, ice chisel, bucksaw, felling axe, maul, files, whetstone) keep their numbers there.

| Tool | Get it | Weight | Durability / edge | What it does |
| --- | --- | --- | --- | --- |
| Crosscut saw | Mining camp | 3 kg | 15,000 / 1,200 | Bucks rounds over 35 cm thick at 0.7 game minutes per 10 cm of diameter, alone; the hatchet takes 3. Needs a file every 300 cuts |
| Froe and mallet | Mining camp; the mallet is carved from a birch knot, 30 game minutes | 1.2 + 0.8 kg | 8,000 / — | Split planks take ×0.6 time and give 8 planks a log instead of 6 |
| Adze | Mining camp head, plus a tool handle | 2 kg | 10,000 / 500 | Hews a log face flat. Hewn wall courses need 30% less chinking, and a plank floor from hewn logs takes ×0.7 time |
| T-auger | Mining camp | 1.4 kg | 4,000 / 300 | Bores 2.5 cm peg holes: shelves, bunks and pegged doors need no nails |
| Hand ice auger | Station Nine | 3.5 kg | 3,000 / 200 | A 15 cm hole through 30 cm of ice in 4 game minutes, 1 m in 15. Only a file restores its edge |
| Crooked knife | Arvid's cabin; or ground from a worn flat file (1 game hour on a whetstone) | 0.15 kg | 4,000 / 300 | Carving at ×0.6 time: bowls, spoons, paddles, snowshoe frames, bow tips |
| Ulu | Made: a 12 cm crescent cut from the crosscut blade or 0.3 kg of iron, in a birch grip | 0.2 kg | 5,000 / 400 | Fish cleaning ×0.7 time; hide scraping ×0.8; cuts frozen fish and meat thin |
| Snow saw | Made: an aluminum panel cut to a 40 cm blade, a birch handle; workbench, 1 game hour | 0.4 kg | 2,000 / — | Cuts snow blocks from wind-packed snow, 1 a game minute |
| Pulaski | Lookout; new handle 1 game hour | 2.3 kg | 12,000 / 500 | Fells as a hatchet; cuts a firebreak at double speed; grubs roots for a garden bed |
| Steel needles | Station sewing kit | — | Never break | Sewing ×0.7 time; a bone needle snaps on 5% of garments |
| Bone awl | Made, as the bone needle | — | 200 holes | Punches leather; moccasins and mukluks need it |
| Sharpening kit (axe puck) | Arvid's cabin | 0.2 kg | 1,000 sharpenings | Restores edge to 100 in 1 game minute, anywhere, no water |
| Leather strop | Made: a 40 cm leather strip glued to a board | 0.3 kg | Never wears | 2 game minutes on a sharp edge (over 90) makes it Keen: edge 120 for the next 100 uses |
| Round file | Mining camp tool box | 0.1 kg | 500 uses | Sharpens saw teeth and the ice auger; flat files cannot |

**Saws need files.** A saw tooth dulls like an edge, and only a file brings it back: 10 game minutes for the crosscut, 3 for the bucksaw. The round file is the only file that does saws, so the mining camp's saw and file come as a pair.

**Snow blocks.** A block is 0.5 × 0.25 × 0.3 m and weighs 10 kg. Stacked into a 1.2 m windbreak around a fire, a ring of 24 blocks counts as a reflector wall on every side (+4 °C) and blocks wind. A snow-block cache on the north side of a hill holds 0 °C into June, as the snow pit does. Blocks melt above 0 °C at 2 cm a day.

## Containers, light and signals

### Containers

| Container | Get it | Slots or volume | Weight | Rule |
| --- | --- | --- | --- | --- |
| Possibles bag | Made: 1 cured pelt, 1 m cordage; 1 game hour | 6 slots, 2 kg | 0.2 kg | A belt slot (Curios). It stays on the body when the pack is dropped, so the fire kit survives a fall through ice |
| Fish-skin bag | Made: 2 salmon skins, sinew; 2 game hours | 3 slots | 0.1 kg | Waterproof; holds pemmican, tinder or matches dry |
| Game bag | Made: 12 m babiche knotted to a net; 2 game hours | Carries 10 kg of meat on a lash point | 0.3 kg | Meat in it cools in air, so it keeps at the meat-pole rate (×0.8) on the walk home; flies cannot reach it |
| Moose-bladder water bag | Made from a moose kill; dried 1 day | 3 L | 0.2 kg | Never on a fire; does not freeze-split |
| Lard pail | An Arvid cache | 4 L or 9 slots | 0.4 kg | A pot with a bail; boils on a cooking stand |
| Tin can | Station, mining camp | 0.4 L | 0.05 kg | Boils on coals; a bail of wire makes it a billy |
| Amber bottle | Mining camp | 0.7 L | 0.3 kg | Breaks if dropped on stone 30%, or frozen over 80% full |
| Ore sack | Mining camp | 9 slots, 15 kg | 0.4 kg | Rots: −10% condition a month wet |
| Jerrycan | Station, drift | 15–20 L | 1.2 kg | Water at camp; carried in both hands when over 5 L |
| Blue poly barrel | Drift | 18 slots | 4 kg | As the crafted barrel, already rodent-proof |

### Light

The main tab's light table stays. These are the additions.

| Light | Get it | Radius | Burns | Note |
| --- | --- | --- | --- | --- |
| Coleman lantern | Station Nine | 10 m High, 7 m Low | 0.15 or 0.075 L white gas or avgas an hour | Adds 150 W to a room; mantles break when dropped |
| Storm lamp | Made: the Coleman's glass globe over a fat lamp; 20 game minutes | 4 m | As the fat lamp | Wind-proof and not smoky: the cabin's Light rating does not drop |
| Fatwood splints | Split from the pitch-soaked heart of an old spruce stump, 10 game minutes for 6 | 4 m | 20 game minutes each | Light at wet 100; the best rain tinder |
| Pitch torch | A stick wrapped in birch bark and 50 g of pitch | 6 m | 45 game minutes | Rain does not put it out; wolves keep 10 m off, as from the bark torch |
| Oil lamp | The fat lamp's dish with engine oil | 3 m | 6 h per 100 mL | As smoky as the fat lamp |

### Signals

**Signals talk to Pete's plane.** Pete Lund flies on drop days and, once the radio works, once a month on his mail run over the valley (the 1st, weather allowing). He reads anything a pilot would.

| Signal | Make it | Seen or heard | What it does |
| --- | --- | --- | --- |
| Heliograph mirror | Lookout | A flash to 15 km in sun | Flashed at the plane on a drop day, the drop lands within 20 m instead of 50 |
| Smoke column | As in Fire, water and light | 8 km for 30 game minutes | The existing drop rule |
| Three fires in a triangle | 3 teepee fires 20 m apart | 2 km at night | The pilot's distress sign: equal to a ground "SOS" |
| Police whistle | Wreck | 1 km | The wolf's Come works to 600 m instead of 300 |
| Brass casing whistle | 1 spent casing; blown across the mouth | 800 m, a shrill note | As the police whistle to 500 m; ravens on a rack lift for 1 game hour |
| Willow whistle | As in Spirits | 300 m | The existing Spirits use |
| Flares | Catalogue | 10 km at night | The existing bear and signal use |

**Ground-to-air signs.** Letters at least 3 m tall in logs, boughs or trampled snow, within 300 m of the station or a marked drop zone. Trampled snow takes 10 game minutes a letter and fills in after 10 cm of snowfall; logs and boughs last.

| Sign | Means | What Pete does |
| --- | --- | --- |
| LL | All well | Answers Dana's "confirm OK" check without the battery |
| V | Need help | The next drop carries one free item worth up to 20 points, once a season |
| X | Need medical help | The next drop carries a free bandage pack and one antibiotic course, once a year |
| SOS | Take me out | The same as the messenger's SOS, on the next flyable day. The analog way out when the battery is dead |
| An arrow | Gone this way | Pete reports it at the next sked: "Saw your arrow pointing upriver" |

Each sign shows on the messenger map as a pin, and Pete's next sked line confirms he read it.

## Medicine, hygiene and cooking

### Medicine and hygiene

**Soap from fat and ash.** The old homestead recipe, in three steps.

1. **Lye.** Pour 4 L of water through 2 kg of birch or willow ash in a birch-bark funnel over the pot; it drips for 1 game day. Spruce ash gives weak lye that will not set.
2. **Boil.** 0.5 kg of lard with the lye on a Low fire, stirred, 3 game hours (tended), 450 kcal.
3. **Set.** Poured into a birch-bark box, it sets in 2 game days. Output: 10 bars, each 10 uses.

| Use | Time | Effect |
| --- | --- | --- |
| Wash hands before a dressing | 1 game minute | The dirty-dressing infection roll falls from 15% to 10% |
| Wash hands after cleaning game or fish | 1 game minute | Removes the 10% gut-illness chance of handling Turning meat |
| Wash body in a warm room or the hot spring | 10 game minutes | Player scent −30% for 1 game day (the hunt's scent model); Spirits +1, at most every 3 days |
| Wash clothes | 20 game minutes a garment, then dry | Smoked-hide scent bonus kept; wet 100 to dry |

**Other medicine and hygiene items.**

| Item | Get it | Effect |
| --- | --- | --- |
| Birch-tar repellent | 4 bark sheets in a sealed tin can over coals, 1 game hour: 50 g, 5 uses | Mosquito bites −50% for 3 game hours; scent +1 while on |
| Hot-water bottle | The steel bottle filled with boiled water and wrapped in a sock | +3 °C asleep for 4 game hours |
| Duct tape | Station Nine, 8 uses | A rain shell, boot or bucket +30% condition in 1 game minute; a splint wrap or a blister patch |
| Tweezers | In the wreck's first-aid tin | Pulls porcupine quills from the wolf in 30 game seconds instead of 2 minutes |
| Sewing kit | Station Nine | Steel needles and 50 m of thread: garment repairs without sinew in summer |
| Engine oil | Wreck, 10 uses | Wiped on an iron tool: no rust for 30 game days |

### Cooking gear

**Gear changes method numbers, never recipes.** The methods and dishes in Cooking stay as written; these items change how and where they can be done.

| Gear | Get it | Weight | What it changes |
| --- | --- | --- | --- |
| Cast-iron skillet | Arvid's cabin | 2.5 kg | Stone fry anywhere on coals, with 10 g of lard per 0.5 kg instead of 20, at ×0.8 time. Unseasoned (below 60%) it sticks like a bare stone |
| Dutch oven | Roadhouse | 5 kg | Coal bake for bread, bannock and stews, 2 kg at a time; the Bread ambition can be met with it |
| Reflector oven | Made: 1 aluminum panel bent to a box; workbench, 2 game hours | 1.2 kg | Bakes flatbread beside any Steady fire, 4 at once, with no stone |
| Enamel mug | Station Nine | 0.2 kg | Heats 0.3 L in coals in 8 game minutes: tea without unpacking the pot |
| Tin-can billy | A tin can and 0.5 m of wire | 0.07 kg | 0.4 L boils in 10 game minutes on coals |
| Carved bowl and spoon | Crooked knife, 1 game hour each | 0.2 kg | A meal eaten from a bowl, sitting, at a table or chair: Spirits +1 once a day |
| Grease tin | Any tin can with a lid | 0.05 kg | Holds 400 g of lard; scent 0 while closed |

### Food values

**Every food has one row, per 100 g.** These are the defaults for `data_maps/item/food.json` (Minecraft development › Layer 6: Item model). Carbs are not stored: they are the kcal left after protein and fat, divided by 4, so kcal = 4 × protein + 9 × fat + 4 × carbs always holds. Vitamin C is given in C-days per 100 g eaten raw (Blueprint › Cooking); the data field `vit_c_mg_per_100g` holds 10 mg for each C-day, a game value, and each cooking method keeps its share. Life is the safe life at 5 °C by state (`spoil_days_at_5c`), which the curve stretches or shrinks with temperature (Decay and time › The decay clock). Whole fish and single pieces count 1 and carry `weight_g`; everything sold by weight counts in 100 g units (RECONCILE 43).

| Food (id) | kcal | Protein g | Fat g | C-days | Sold as | Life at 5 °C, curve | Made or cooked at, time |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Red meat: moose, caribou, sheep, goat, bison, muskox, beaver (`<species>_meat` from `item_family/`) | 115 | 25 | 1.7 | 0 | 100 g units | Raw 6 d, cooked 9 d; `living_rot` k 1.0 | Spit roast 30 min a kg; stone fry 10 min a 0.5 kg; stew 60–90 min |
| Lean meat: hare, squirrel, grouse, ptarmigan, porcupine | 115 | 27 | 0.8 | 0 | Units; small game also as `whole_game` | As red meat | Spit roast 25 min a whole hare; stew |
| Bear meat (`black_bear_meat`, `grizzly_meat`), fall | 115 | 17.5 | 5.0 | 0 | Units | As red meat; must be Done at twice the time or stewed | Spit 60 min a kg; stew |
| Duck and goose meat | 115 | 15.5 | 5.9 | 0 | Units | As red meat | Spit roast; coal bake whole |
| Heart (`heart` + `species`) | 130 | 26 | 2.9 | 0 | Single, `weight_g` | Raw 3.6 d; k 0.6 | Stone fry; stew |
| Liver (`liver` + `species`) | 130 | 24 | 2.8 | 0.4 raw | Single | Raw 1.8 d; k 0.3 | Stone fry Rare (keeps half its C) |
| Kidney (`kidney`) | 100 | 17 | 3.4 | 0 | Single | Raw 2.4 d; k 0.4 | Stone fry; stew |
| Tongue (`tongue`) | 130 | 18 | 6.4 | 0 | Single | Raw 4.8 d; k 0.8 | Stew 90 min |
| Bird giblets (`giblets`) | 130 | 22 | 4.7 | 0 | Units | Raw 1.8 d; k 0.3 | Stew; stone fry |
| Brain (`brain`) | 140 | 11 | 10 | 0 | Single | Raw 1.8 d; k 0.3 | Stone fry; or tans its own hide |
| Blood (`blood`) | 80 | 18 | 0.9 | 0 | In a bark bucket, units | 1.2 d; k 0.2 | Stew into blood broth 30 min |
| Raw fat (`animal_fat`) | 700 | 0 | 77.8 | 0 | Units | Raw 12 d; k 2.0; then rancid | Eaten raw (nausea limit 0.2 kg in 2 h), rendered, or in pemmican |
| Lard (`lard`) | 900 | 0 | 100 | 0 | Units | 180 d cool, 60 d above 15 °C, frozen 2 years; curve `lard` | Render: pot on a Low fire, 60 min a kg; 1 kg fat → 0.7 kg lard + 0.1 kg cracklings |
| Cracklings (`cracklings`) | 550 | 36 | 45 | 0 | Units | 30 d; curve `lard` | From rendering |
| Marrow bone (`marrow_bone`) | 30 (as broth) | 0 | 3.3 | 0 | Single, 1 kg | 9 d; k 1.5 | Split with the hatchet, then stew 4 h; once stewed it is a tool bone and never adds the +15% again |
| Bone broth (`bone_broth`) | 40 | 1 | 4.0 | 0 | In a pot or bottle | Cooked 9 d | Stew 4 h, queueable: 2 kg bones in 2 L give 1.5 L and 600 kcal |
| Dried or smoked meat strips (`dried_meat`, state `dried` or `smoked`) | 288 | 62.5 | 4.25 | 0 | Units, 40% of the fresh weight | Dried 60 d; hot-smoked 30 d; cold-smoked 180 d; salt doubles each | Drying rack, 2 dry days; smoke rack or smokehouse, hot 8–12 h or cold 2–4 d |
| Meat powder (`meat_powder`) | 288 | 62.5 | 4.25 | 0 | Units | 60 d; curve `dried` | Flat stone, 20 min a kg of dried meat |
| Pemmican (`pemmican`, new) | 560 | 30.8 | 46.7 | 0.1 with berries | Units, in a hide or salmon-skin bag | 365 d cool, 180 d above 15 °C; curve `pemmican` | By hand: powder 50, warm lard 45, dried berries 5 by weight; 3 h for 5 kg |
| Whole fish, lean (pike, burbot, grayling, sucker): the species id | Fishing in depth's kcal per kg ÷ 10 (pike 150, burbot 160, grayling 80, sucker 100) | 90% of kcal ÷ 4 | 10% of kcal ÷ 9 | 0.2 raw | Single, `weight_g` | Whole 3.6 d (k 0.6), cleaned 6 d | Spit roast 12 min a fish under 1 kg; coal bake whole; stew heads and frames for +15% |
| Whole fish, oily (salmon, whitefish, cisco, lake trout, char, sheefish) | As above (chum 170, coho 200, sockeye 210, humpback whitefish 150, least cisco 180) | 60% of kcal ÷ 4 | 40% of kcal ÷ 9 | 0.2 raw | Single | As lean fish | As lean fish; split for the smoker |
| Arctic lamprey (`arctic_lamprey`) | 330 | 33 | 22 | 0.2 raw | Single | Whole 3.6 d | Spit or render for `lamprey_oil` |
| Dried or smoked fish (`dried_fish`, new; state `dried` or `smoked`) | 350 | 62 | 11.3 | 0 | Units, 40% of the fillet weight | As dried meat | Split flat on the rack or in the smoker, as dried meat |
| Sour fish (`sour_fish`) | 70 | 12 | 2.4 | 0.2 | Units | 20 d; curve `sour_fish` | Grass pit or bark basket, 10 d at 3–10 °C (Decay and time › Sour fish and botulism) |
| Mussel meat (`mussel_meat`) | 80 | 12 | 2.2 | 0 | Single, 25 g | 1.8 d; k 0.3 | Boil 5 min |
| Wild egg (`wild_egg`) and hen egg (`hen_egg`, new) | 140 | 13 | 9.8 | 0 | Single, 70 g wild duck or 55 g hen | 18 d; k 3.0 | Boil 10 min; coal bake |
| Goat milk (`goat_milk`, new) | 69 | 3.6 | 4.1 | 0.1 | In a bottle or bucket, units | 3 d; k 0.5; 10 d in the ice house | Drunk raw or warmed; counts as water |
| Pilot-bread cracker (`pilot_bread`) | 400 | 10 | 8.9 | 0 | Single, 25 g | Keeps while dry; wet 30 or more: 10 d | Eaten as it is |
| Canned peaches (`canned_peaches`) | 89 | 0.5 | 0.1 | 0.1 | Single, 0.45 kg can | Never while sealed; opened 3 d | Eaten from the can; a bulged can carries botulism |
| Fresh berries (`<species>` fruit items) | 57 | 0.7 | 0.3 | 0.5, or the species value in Plants and fungi | Units | 9 d; k 1.5; then fermented | Picked; dried on a rack 2 d (half the C) |
| Rose hips (`rose_hips`, new) | 160 | 1.6 | 0.3 | 2 | Units | 9 d fresh; dried 180 d | Picked Sept–March; `rose_hip_meal` keeps half |
| Spruce tips (`spruce_tips`, new) | 30 | 1 | 0.3 | 1 | Units | 3 d; k 0.5 | Picked May 20 – June 15; steeped as tea 10 min |
| Greens and leaves (Plants and fungi ids) | 25 | 2 | 0.3 | By species | Units | 3 d; k 0.5 | Raw, or boiled as Plants and fungi requires |
| Mushrooms (Plants and fungi ids) | 25 | 3 | 0.3 | 0 | Units | 1.8 d; k 0.3; dried 180 d | Stew or stone fry; the lookalike rule |
| Potato (`potato`, new) | 77 | 2 | 0.1 | 0.2, kept in any method | Units | 150 d; k 25; root cellar 6 months | Coal bake 40 min; stew |
| Barley, grain (`barley`, new) | 350 | 12.5 | 2.0 | 0 | Units | Keeps while dry; wet 30 or more: 10 d | Stew as porridge 30 min |
| Barley flour (`barley_flour`, new) | 345 | 11 | 2.0 | 0 | Units | As barley | Quern stone; flatbread on the stone, 10 min |
| Cabbage (`cabbage`, new) | 25 | 1.3 | 0.1 | 0.6 | Single, about 1.5 kg | 60 d; k 10 | Raw, stewed, or packed as sauerkraut |
| Sauerkraut (`sauerkraut`, new) | 20 | 1 | 0.1 | 0.6 | Units, in a barrel | Until May 1; curve `sauerkraut` | Barrel, 1.5 kg cabbage and 30 g salt, 21 d |
| Birch sap (`birch_sap`, new) | 4 | 0 | 0 | 0 | In a bucket, units | 3 d, then it turns to `sap_beer` | Tapped at breakup (Plants and fungi › The tapping season) |
| Birch syrup (`birch_syrup`, new) | 210 | 0 | 0 | 0 | In a bottle, units | 365 d | Pot on Steady, 8 h: 40 L sap → 0.4 L (100 : 1) |
| Honey (`bumblebee_honey`) | 300 | 0.3 | 0 | 0 | Units | Never | Taken from a nest (Bestiary) |

**Cooked dishes are one item.** A cooked mix is `yearofice:dish` (single, with `weight_g`) carrying a new component `yearofice:nutrients` (kcal, protein\_g, fat\_g, vit\_c\_mg, water\_ml) summed from its ingredients at the moment it finishes, times the method's factors (stew 1.15 with bones or heads, vitamin C kept by method), plus `yearofice:dish_id` when the notebook names it (camp stew, fish-head soup, akutaq, flatbread, porridge, liver in lard). Its freshness is the cooked state of its shortest-lived ingredient. This is how "any sensible mix works" in Cooking becomes buildable without a data row per mix. The component goes on the lead's Layer 6 list; the absorb step reads `nutrients` before the data map.

**New ids from this table** (Food and hunting, `food.FoodItems`): `pemmican`, `dried_fish`, `hen_egg`, `goat_milk`, `rose_hips`, `spruce_tips`, `potato`, `barley`, `barley_flour`, `cabbage`, `sauerkraut`, `birch_sap`, `birch_syrup`, `dish`, and the components `nutrients` and `dish_id`. Plant foods keep the ids Plants and fungi gives them where it already names one; these rows set only the numbers for ids nobody else defines.

```json
{
  "values": {
    "yearofice:moose_meat": {
      "kcal_per_100g": 115, "protein_g_per_100g": 25, "fat_g_per_100g": 1.7,
      "vit_c_mg_per_100g": 0, "curve": "living_rot",
      "spoil_days_at_5c": { "raw": 6, "cooked": 9, "smoked": 30, "dried": 60 }
    },
    "yearofice:pemmican": {
      "kcal_per_100g": 560, "protein_g_per_100g": 30.8, "fat_g_per_100g": 46.7,
      "vit_c_mg_per_100g": 1, "curve": "pemmican",
      "spoil_days_at_5c": { "dried": 365 }
    }
  }
}
```

**Debug and tests.** `/yoi food info` prints the held stack's row, state, band and clock; `/yoi give food <id> <grams> [state]` makes a stack; tuning that is not per food (the C-day scale, the stomach, the nausea limit) sits in the `[food]` table of `yearofice-common.toml`. GameTests: `yearofice:food/units_match_kcal` (every row's kcal equals 4P + 9F + 4C within 2 kcal, and C ≥ 0), `yearofice:food/render_never_gains` (1 kg of `animal_fat` rendered gives at most 7,000 kcal of products), `yearofice:food/dish_sums` (a camp stew of 1 kg moose with bones gives 1,323 kcal ±1).

## Camp comforts and trade-offs

### Camp comforts

**Comforts pay in sleep, warmth or Spirits.** Each one is small. Together they decide whether a winter feels endurable.

| Comfort | Made from | Time | Weight | Effect |
| --- | --- | --- | --- | --- |
| Pillow | A cloth or hide bag of 0.3 kg cattail fluff, ptarmigan down or seat foam | 30 game minutes | 0.3 kg | Sleep restores 5% more energy |
| Rabbit-skin blanket | 12 hare pelts cut into one spiral strip each, twisted fur-out and woven on a pole frame | 8 game hours | 1.5 kg | +6 °C asleep. Sheds: −1% condition a night; a Fine one sheds half |
| Sleeping pad | 2 seat cushions, or 1 caribou hide | 20 game minutes | 1.2 kg | +2 °C asleep on any bed; on bare snow it counts as a debris bed |
| Camp chair | A birch frame (T-auger and pegs), 6 m rawhide seat | 3 game hours | 4 kg | Resting in it gives +0.4 Condition an hour instead of +0.3; counts as a decoration |
| Bed net | 4 m² of fine fiber mesh or the drop tarp's parachute silk | 2 game hours | 0.3 kg | No mosquito wake-ups asleep under it |
| Paperback | Station Nine | — | 0.3 kg | 30 game minutes of reading by lamplight: Spirits +1, the first 3 reads each winter. A fourth read: "Day 240. Read the western again. He gets the girl. Again." |
| Wooden figure | As in Spirits | — | 0.2 kg | Unchanged |

**The rabbit-skin blanket is the hare cycle's gift.** A boom year fills the line with hares, and 12 pelts become the warmest thing a player can make without big game. In a crash year it may take all winter.

### Heavy but useful

**Every heavy find is a choice of trip.** Each item below has a lighter alternative already in the game; the heavy one saves time or calories every day once it is home.

| Item | Weight | Gives | Lighter alternative | Getting it home |
| --- | --- | --- | --- | --- |
| Cast-iron skillet | 2.5 kg | Half the frying lard, ×0.8 time | A flat cooking stone, free | One trip in the pack |
| Dutch oven | 5 kg | Bread anywhere | The reflector oven, 1.2 kg | A pack trip from the roadhouse, 2,000–6,000 m away |
| Crosscut saw | 3 kg, 1.8 m long | Big rounds bucked alone | The hatchet at 4× the time | A lash point only; it snags in spruce: ×0.9 speed |
| Roadhouse stove | 60 kg | A 90% stove before the Homestead tier | The barrel stove | Sled, or dragged at 30% speed |
| Coleman lantern | 1.6 kg | 10 m of light, 2 hours | Candles at 0.1 kg | Pack |
| Binoculars | 0.9 kg | 4× zoom; counts at 400 m | Walking closer | Neck slot (Curios) |
| Hand ice auger | 3.5 kg | Holes in a quarter of the chisel's time | The ice chisel, also heavy | A lash point |
| Breakup board | 4 kg | Spirits and the record | — | Pack |

## Keepsakes and story items

### Keepsake rules

**Keepsakes are protected.** They weigh almost nothing, never decay, are never stolen by animals, float when dropped in water and survive an Endless-mode SOS. They cannot be salvaged or burned, except the chart, which is a tool first.

**A keepsake shelf.** Keepsakes placed on a shelf count as trophies (double decorations in the Spirits table). Holding E on the shelf at night reads them back: Spirits +1 once a day, with one line drawn from a found item.

### The keepsakes

| Keepsake | Where | What it does |
| --- | --- | --- |
| Elin's logbook pages (24) | As in Story | Each gives what Story lists; all 24 are the one item `yearofice:note` with `yearofice:note_id` `logbook_01` to `logbook_24` (RECONCILE 79), sorted in the notebook |
| Photograph of Elin and Tok | Station Nine, above the bunk | Shown once, it fills in the face Elin never has on screen: only her back and Tok's ears. On the shelf it is a trophy. Kari's year-3 call mentions it if it hangs |
| Tok's bed | Station Nine, by the stove | A tamed pup sleeping on it gets +1 trust a night for its first 14 nights, outside the daily cap. The wolf with the rawhide strip sniffs at it if it is left outside, once a year |
| Tok's collar | As in Story | Unchanged |
| The wedding ring | Wreck, under the pedals | See below |
| J.O.'s windscreen note | Wreck | Can be peeled off and kept; Spirits +1 on the shelf |
| Arvid's breakup board | Arvid's cabin | Carries the ice record, as above |
| Gold vial | Mining camp | As in Set pieces: a trophy |
| Mammoth tusk | Ice cave, or an earthquake slide | A trophy; or carving ivory, which ends its keepsake status |
| Elin's tin cup | Glacier Cirque | A trophy. Drinking tea from it at the station on April 6 gives Spirits +5 and one journal line |
| A woman's wool mitten | The Windgap cache, year 5, as in Story | A trophy; the pair is never found |

**The ring.** It is a plain gold band engraved inside "J.O. & R.O. 6.6.72". Once the radio works, the sked offers one choice: "Send the ring out with Pete." Sent, it goes out on the next drop flight and the ring leaves the game.

Two drops later, the letter in the crate is from a woman in Fairbanks. "My mother walked to the river and a plane picked her up on the 16th. She flew another twelve years. She always said the valley kept her ring." Spirits +10, a journal entry, and the lookout log's line gets a new margin note in the notebook: "Confirmed."

Kept instead, the ring sits on the shelf as a trophy. Either choice is fine, and the journal records it.

## Item interactions

### Combining

**Drop one item onto another to combine.** In the pack, dragging an item onto another runs a 1-step recipe if the pair has one. The cursor shows the result first: "Steel bottle + wool sock → hot-water bottle". These are hands-only and take 1–20 game minutes.

| Item | Onto | Result |
| --- | --- | --- |
| Boiled water in the bottle | Wool sock | Hot-water bottle |
| Lard | Cast-iron skillet | Seasoned skillet, at a fire |
| Engine oil | Any iron tool | Rust-proofed for 30 days |
| Wire, 0.5 m | Tin can | Tin-can billy |
| Cordage, 2 m | 2 tin cans with stones | Can alarm: hung on a trip line 0.5 m high across a path, it rattles at 30 m when a bear, moose or wolf crosses, waking the player |
| Hand lens | Char cloth or tinder | A sun-lit fire, in direct sun only |
| Avgas | A laid fire | Tinder catches at wet 80 |
| Fatwood splint | A tinder bundle | The bundle lights at any wetness |
| Duct tape | Any shell, boot or container | +30% condition |
| Coleman globe | Fat lamp | Storm lamp |
| Spent casing | Cordage | A whistle on a neck lanyard (Curios neck slot) |
| Strop | Any edge tool over 90 | Keen |

### Repairing

**Material decides the repair.** Crafting and building in depth sets each tool's fix. Everything else follows its material.

| Material | Repair | Restores | Limit |
| --- | --- | --- | --- |
| Wood | Pitch glue or hide glue, a knife | +40% | Below 10% it is firewood |
| Hide, leather, cloth | A needle and sinew or thread, as in Clothing | +40% | Rawhide below 10% becomes lacing scraps |
| Iron, steel | A file or whetstone for edge; the forge for full condition | Edge to 100; full at the forge | Cast iron cracks below −40 °C if struck: it cannot be forged, only patched with a nailed tin band (+20%) |
| Tin, aluminum | Fold and nail, or duct tape | +30% | — |
| Glass | Cannot be repaired | — | Breaks into shards |
| Plastic | Duct tape or pitch | +30% | The jerrycan's crack can be sealed with pitch to hold 20 L |

### Salvaging into parts

**Holding E with the hatchet or knife on a found item breaks it into parts.** The preview shows the parts first, and salvage cannot be undone.

| Item | Tool and time | Parts |
| --- | --- | --- |
| Coleman lantern | Knife, 10 game minutes | Glass globe, 0.3 kg tin, 1 m wire bail |
| Leghold trap | Hatchet, 15 game minutes | 0.3 kg iron, 1 flat spring (a spring-pole trigger that never springs empty in wind) |
| Crosscut saw | Hatchet and file, 1 game hour | 4 ulu blanks or 1 snow-saw blade, 0.8 kg steel |
| Seat cushion | Knife, 5 game minutes | 0.3 kg foam, 1 m² vinyl (waterproof patch) |
| Worn garment below 10% | Knife, 5 game minutes | 2–4 cloth scraps: boiled-cloth dressings, char cloth, wicks |
| Amber bottle | A stone, 1 game minute | 3 glass shards: knapped like flint into a scraper or a point that cuts ×1.1 but shatters on bone 30% of the time |
| Tin can | Knife, 5 game minutes | 1 tin strip: spinner blades, cache-leg bands |
| Broken bow | Knife | 2 short staves: throwing sticks or a light-bow stave |
| Ore sack | Knife | 2 m of burlap cordage |
| Jerrycan | Knife, 10 game minutes | 1 m² plastic: a waterproof roof patch (U as bark, sheds all rain) |

### Upgrading quality

**Rework.** A Crude crafted item can be reworked to Good at 50% of its recipe time and 25% of its inputs, once the skill is Handy. The result is certain.

**Finishing.** A Good wooden or hide item can be finished toward Fine once the skill is Practiced: 30% of the recipe time, then a 50% roll. Wood is rubbed with fine sandstone and lard; hide is smoked a second time; iron goes to the forge. A failed finish keeps it Good, and each item gets one try.

### Item decay

**Things wear out even unused.** These rates are slow on purpose: a careful player never notices them, and a careless one sees the warning first.

| Decay | Rate | Warning |
| --- | --- | --- |
| Rust | An iron or steel item left wet over 24 game hours loses 2 edge a day and 1% condition a week; oiled items are immune for 30 days | Orange flecks on the icon; "Rusting" in the tooltip |
| Cordage rot | Plant-fiber cordage and nets stored damp lose 10% strength a month; sinew and rawhide do not | "Rotting" at 20% loss |
| Rawhide cracking | As in Clothing: worked with fat once a season | The existing warning |
| Mice in chests | October–April, in a chest, cache or cellar with fewer than 3 mouse deadfalls within 3 blocks: leather and hide lose 5% condition a week; food loses 2–3% of its units a week (rolled weekly) and the stack gains the `droppings` flag, which gives 20% gut illness a meal until the top is trimmed (1 game minute, −10% of the units). Barrels, sealed tins and the ice house are immune | "Mouse droppings in the chest" in the store view |
| Fuel | White gas and avgas left in an open tin lose 10% a day | "Evaporating" |
| Paper | Pages and the chart never decay; the paperback loses its back cover after 10 wet days | — |

**Lost under snow.** An item dropped on snow is buried after 10 cm of new snowfall. It shows only as a small bump, and once buried it leaves the 7-day dropped-item clock: a World edits op moves it into a `lost_gear` block under the snow (`placed_mask` set, never expires; RECONCILE 36, 89, 97), and the map pin stays as long as the block does. At the spring melt the block opens and every buried item reappears on the ground with a fresh 7-day stamp, so the first warm week of April turns up lost arrows, a mitten and the hatchet head that flew off in January.

### Theft by animals

**Animals take what the rules let them, and every theft leaves sign.** The thief's tracks or feathers are always at the spot, and the item goes somewhere the player can find it.

| Thief | Takes | When | Where it goes |
| --- | --- | --- | --- |
| Gray jay ("camp robber") | Food items under 0.2 kg in open view within 10 m of a fire: crackers, bait, fish chunks, lard, berries | Daylight, all year; 1 item a game hour while nobody is within 5 m | Eaten. A jay fed 3 times by hand becomes Habituated and can be named; a named jay takes nothing |
| Raven | Shiny items left in the open: spoon, spinner, mirror, tin lid, ring, casing | 5% a day per item | Cached in a dead spruce top within 300 m; the spot shows ravens coming and going |
| Red fox | Mitts, socks, small pelts, rawhide scraps and meat under 2 kg left on the ground | 20% a night per item | Its den mouth, 100–800 m away, found by its tracks in snow |
| Wolverine | Whole caches, as in Outposts; also a trap with its catch | As in Outposts | Dragged 50–200 m and buried in snow; the cache food left behind is musked, fit only for the wolf |
| Porcupine | Salty, sweat-soaked wood: tool handles, paddles, the chair, a bow left outside | 30% a night per item within its range | Not taken: gnawed in place, −20% condition |
| Red squirrel | Berries and seeds in an open basket | 10% of the contents a day | Its midden |
| Marten, mice, bears | As in Traps, Decay and time and Danger in detail | Unchanged | Unchanged |

**The porcupine lesson.** A first-time player leaves the hatchet in the chopping block overnight and finds the handle chewed. The messenger line in the morning reads: "Porcupine chewed the hatchet handle. Salt from your hands." From then on the player brings tools inside, and the cabin feels more like a home.

### Items in the river

**Floating and sinking.** Each item has a float flag. Wood, bark, cordage, dry hide, keepsakes, empty bottles with corks, empty cans with lids and the jerrycan float; iron, stone, glass with water, full cans and wet hide sink. A sunk item in water over 1 m deep, or on mud, becomes part of a `lost_gear` block on the bed through a World edits `DRIFT` op (RECONCILE 36). It never expires; wading in at low water, or chiseling through clear ice above it (5 game minutes), gets it back.

**Drift.** A floating item moves at the current's speed and stops at the next snag, log jam, beaver dam or still eddy. Each snag holds it 90% of the time; it slips past the other 10% to the next. An item that drifts out of loaded chunks is carried on by the `DRIFT` op and lands as `lost_gear` at the next snag downstream, which the map pins.

**Freeze-up.** An item drifting at freeze-up becomes a `lost_gear` block frozen into the ice where it is and shows through clear ice as a dark shape. Chiseling it out takes 5 game minutes. At breakup it moves again, so a paddle lost in October can turn up 3 km downstream in May.

## Master items table

The table lists 175 items. Weights are per unit; stack is per slot (50 small, 20 medium, 8 kg food by weight, 1 for tools and single items). Every id is in the `yearofice` namespace. Food values and recipes stay in their own sections.

| id | Category | kg | Stack | Where found or made | Use |
| --- | --- | --- | --- | --- | --- |
| fixed\_blade\_knife | Tool | 0.3 | 1 | Crate | Cutting, carving, butchering |
| hatchet | Tool | 0.9 | 1 | Crate | Felling, splitting, prying iron |
| steel\_bottle | Container | 0.4 | 1 | Crate | 1 L; boils in coals |
| cooking\_pot | Cooking | 0.6 | 1 | Crate | 2 L; stews, boiling |
| pilot\_bread | Food | 0.025 | 50 | Crate (38) | 100 kcal |
| medkit | Medicine | 0.5 | 1 | Crate | 6 bandages, 3 antibiotics, 10 painkillers |
| bear\_spray | Defense | 0.5 | 1 | Crate, catalogue | One use |
| cord | Material | 0.01 per m | 20 | Crate (10 m) | Cordage |
| match | Fire | 0.002 | 50 | Crate (3), caches, catalogue | 95% calm, 50% wind |
| seed\_packet | Seed | 0.05 | 20 | Crate | Potato and barley |
| compass | Instrument | 0.05 | 1 | Crate | Bearing |
| headlamp | Light | 0.1 | 1 | Crate | 20 h |
| plant\_guide | Book | 0.3 | 1 | Crate | Half the local plants |
| solar\_panel\_small | Power | 0.6 | 1 | Crate | Charges one device |
| messenger | HUD | 0.2 | 1 | Crate | The HUD |
| crate\_backpack | Pack | 1.0 | 1 | Crate | 18 slots, 30 kg |
| rain\_shell | Clothing | 0.4 | 1 | Worn | Torso outer |
| hiking\_boots | Clothing | 1.4 | 1 | Worn | Feet outer |
| mosquito\_head\_net | Clothing | 0.05 | 1 | Crate | Bugs −80% on the face |
| birch\_bark | Material | 0.1 | 20 | Birch | Tinder, roofing, containers |
| plant\_fiber | Material | 0.02 | 20 | Fireweed, nettle, willow | Cordage |
| cordage | Material | 0.01 per m | 20 | Made | Lashing, snares, line |
| spruce\_root\_lashing | Material | 0.02 per m | 20 | Made | Lashing poles |
| spruce\_gum | Material | 0.05 | 50 | Wounded spruce | Pitch glue |
| pitch\_glue | Material | 0.05 | 50 | Made | Hafting, seams |
| hide\_glue | Material | 0.1 | 20 | Made | Strong glue, fails wet |
| charcoal | Fuel | 0.5 | 20 | Fires, clamps | Smelting, pitch |
| tinder\_bundle | Fire | 0.05 | 20 | Made | Catches embers |
| char\_cloth | Fire | 0.005 | 50 | Made in a tin | Catches sparks |
| fatwood | Fire | 0.1 | 20 | Old spruce stumps | Lights wet; 20 min of light |
| chaga | Fire | 0.1 | 20 | Birch | Carries an ember 4 h; tea |
| ember\_carrier | Fire | 0.2 | 1 | Made | Holds an ember 6 h |
| bow\_drill | Fire | 0.6 | 1 | Made | Friction fire |
| flint\_and\_steel | Fire | 0.15 | 1 | Made, Iron | 85% fire |
| hand\_lens | Instrument | 0.03 | 1 | Station Nine | Plant ID; sun fire |
| flint\_nodule | Material | 0.4 | 20 | Gravel bars | Knapping |
| glass\_shard | Material | 0.05 | 50 | Salvaged bottles | Knapped scraper or point |
| iron\_nodule | Material | 3.0 | 20 | Rock faces, bog beds | Smelting |
| bloom | Material | 3.0 | 1 | Furnace | Consolidated to bar iron |
| bar\_iron | Material | 0.1 | 50 | Furnace, scrap | Every iron recipe |
| scrap\_iron | Material | 0.5 | 20 | Mining camp | Counts as bar iron |
| square\_nail | Material | 0.025 | 50 | Mining camp, made | Doors, ice picks |
| aluminum\_panel | Salvage | 1.5 | 1 | Wreck (4) | Runners, spoons, stove pipe, snow saw |
| control\_cable | Salvage | 0.05 per m | 20 | Wreck (30 m) | Radio mast, snares |
| plexiglass\_window | Salvage | 2.0 | 1 | Wreck | Cabin or cold-frame window, or cut with the knife into 6 lantern offcuts (30 game minutes, uses it up; RECONCILE 26) |
| plane\_battery | Salvage | 12.0 | 1 | Wreck | Radio power |
| steel\_wool | Fire | 0.05 | 1 | Wreck | Battery fire |
| avgas | Fuel | 0.7 per L | 1 | Wreck tank | Accelerant, lantern fuel |
| engine\_oil | Hygiene | 0.6 | 1 | Wreck | Rust-proofing, lamp oil |
| seat\_cushion | Salvage | 0.6 | 20 | Wreck (2) | Pad, pillow, vinyl |
| sectional\_chart | Instrument | 0.1 | 1 | Wreck | River outlines on the map |
| hook\_tin | Tackle | 0.05 | 1 | Wreck | 6 hooks, 20 m braided line |
| braided\_line | Tackle | 0.002 per m | 20 | Hook tin | 9 kg rated |
| police\_whistle | Signal | 0.02 | 1 | Wreck | 1 km signal |
| tweezers | Medicine | 0.01 | 1 | Wreck first-aid tin | Quills |
| dial\_thermometer | Instrument | 0.2 | 1 | Station Nine | Temperatures without battery |
| coleman\_lantern | Light | 1.6 | 1 | Station Nine | 10 m light |
| lantern\_mantle | Light | 0.001 | 20 | Station Nine (2) | Lantern spare |
| glass\_globe | Salvage | 0.3 | 1 | Coleman salvage | Storm lamp |
| enamel\_mug | Cooking | 0.2 | 1 | Station Nine (2) | Heats 0.3 L |
| sewing\_kit | Tool | 0.2 | 1 | Station Nine | Needles and thread |
| steel\_needle | Tool | 0.001 | 50 | Sewing kit | Sewing ×0.7 |
| cotton\_thread | Material | 0.001 per m | 50 | Sewing kit | Garment repair |
| duct\_tape | Repair | 0.15 | 1 | Station Nine | 8 quick repairs |
| tin\_can | Container | 0.05 | 20 | Station, camp | Cup, tins, alarms |
| tin\_strip | Material | 0.02 | 50 | Salvaged cans | Spinners, bands |
| fencing\_wire | Material | 0.1 per m | 20 | Station Nine (20 m) | Heavy snares, lashing |
| snare\_wire | Material | 0.017 per m | 20 | Caches, catalogue | Wire snares, leaders |
| jerrycan | Container | 1.2 | 1 | Station, drift | 15–20 L water |
| ice\_auger | Tool | 3.5 | 1 | Station Nine | Ice holes in 15 min |
| toks\_bed | Keepsake | 1.8 | 1 | Station Nine | Pup trust; half a blanket |
| photograph\_elin | Keepsake | 0.01 | 1 | Station Nine | Trophy |
| paperback | Comfort | 0.3 | 1 | Station Nine | Spirits by lamplight |
| note | Keepsake | 0.005 | 1 | 24 logbook pages; every found note | Elin's pages and all notes, one id told apart by `note_id` |
| wedding\_ring | Keepsake | 0.005 | 1 | Wreck | The ring's story |
| jo\_note | Keepsake | 0.005 | 1 | Wreck | Trophy |
| cast\_iron\_skillet | Cooking | 2.5 | 1 | Arvid's cabin | Frying with half the lard |
| spent\_casing | Signal | 0.012 | 50 | Arvid's cabin (12) | Brass whistle |
| crooked\_knife | Tool | 0.15 | 1 | Arvid's cabin, made | Carving ×0.6 |
| axe\_puck | Tool | 0.2 | 1 | Arvid's cabin | Edge to 100 in 1 min |
| leghold\_trap | Salvage | 1.1 | 1 | Arvid's cabin (2) | Iron and a spring |
| flat\_spring | Material | 0.1 | 20 | Trap salvage | Wind-proof spring pole |
| birch\_chair | Comfort | 4.0 | 1 | Arvid's cabin, made | Rest and decoration |
| breakup\_board | Keepsake | 4.0 | 1 | Arvid's cabin | The ice record |
| lard\_pail | Container | 0.4 | 1 | Arvid's caches | 4 L pot |
| babiche | Material | 0.02 per m | 20 | Caches, made | Lacing |
| rock\_salt | Food | 0.1 | 8 kg | Roadhouse, caches | Smoking, sauerkraut |
| tallow\_candle | Light | 0.1 | 20 | Made, caches | 4 h |
| moose\_hide\_mitts | Clothing | 0.3 | 1 | Caches | Hands safe to −24 °C |
| crosscut\_saw | Tool | 3.0 | 1 | Mining camp | Big rounds |
| froe | Tool | 1.2 | 1 | Mining camp | Planks ×0.6, 8 a log |
| wooden\_mallet | Tool | 0.8 | 1 | Made | Drives the froe |
| adze\_head | Salvage | 1.5 | 1 | Mining camp | Adze with a handle |
| adze | Tool | 2.0 | 1 | Made | Hewing |
| t\_auger | Tool | 1.4 | 1 | Mining camp | Peg holes |
| round\_file | Tool | 0.1 | 1 | Mining camp | Saw teeth, auger |
| flat\_file | Tool | 0.3 | 1 | Caches, catalogue | Edge to 100 |
| amber\_bottle | Container | 0.3 | 20 | Mining camp | 0.7 L |
| ore\_sack | Container | 0.4 | 1 | Mining camp (3) | 9 slots on a lash point |
| pick\_head | Salvage | 1.5 | 1 | Mining camp | Pick |
| gold\_pan | Tool | 0.6 | 1 | Mining camp | Panning; a wide pan |
| cast\_iron\_kettle | Cooking | 2.0 | 1 | Mining camp | Kettle |
| gold\_vial | Keepsake | 0.05 | 1 | Mining camp | Trophy |
| tin\_lantern | Light | 0.4 | 1 | Mining camp, made | Wind-proof candle |
| aneroid\_barometer | Instrument | 0.6 | 1 | Lookout | Forecast skill |
| heliograph\_mirror | Signal | 0.15 | 1 | Lookout | Flash to 15 km |
| pulaski | Tool | 2.3 | 1 | Lookout | Firebreaks ×2 |
| copper\_cable | Material | 0.12 per m | 20 | Lookout (10 m) | Rot-proof lashing, leaders |
| canned\_peaches | Food | 0.45 | 20 | Lookout (3) | 400 kcal; 30% bulged |
| wool\_blanket | Comfort | 2.0 | 1 | Lookout, catalogue | +2 or +3 °C asleep |
| binoculars | Instrument | 0.9 | 1 | Lookout | 4× zoom |
| dutch\_oven | Cooking | 5.0 | 1 | Roadhouse | Bread in coals |
| bearpaw\_snowshoes | Travel | 2.0 | 1 | Roadhouse | Relaced snowshoes |
| elin\_tin\_cup | Keepsake | 0.1 | 1 | Glacier Cirque | April 6 tea |
| wool\_mitten | Keepsake | 0.1 | 1 | Windgap cache, year 5 | Trophy |
| net\_float | Tackle | 0.05 | 50 | Drift | Gill-net floats |
| poly\_barrel | Container | 4.0 | 1 | Drift | 18 slots, rodent-proof |
| net\_tangle | Salvage | 6.0 | 1 | Drift | 1 gill-net segment |
| antler | Material | 4.0 | 1 | Sheds, kills, drift | Tools, trophy |
| bone\_splinter | Material | 0.05 | 50 | Butchering | Needles, points, gorges |
| sinew | Material | 0.1 | 20 | Big game | Thread, bowstrings |
| rawhide\_strip | Material | 0.05 | 20 | Made | Lashing, calls |
| cured\_pelt | Material | 0.3 | 20 | Made | Garments, bags |
| leather | Material | 2.5 | 1 | Made | Coats, bellows, strops |
| salmon\_skin | Material | 0.1 | 20 | Salmon | Fish-skin bags |
| lard | Food | 0.1 | 8 kg | Rendered | Lamps, curing, frying |
| soap\_bar | Hygiene | 0.1 | 20 | Made | 10 washes |
| lye | Material | 4 L | 1 | Made | Soap |
| birch\_tar | Medicine | 0.05 | 20 | Made in a tin | Bug repellent |
| spruce\_pitch\_salve | Medicine | 0.1 | 1 | Made | 5 uses |
| willow\_bark | Medicine | 0.05 | 50 | Willow | Fever tea |
| usnea | Medicine | 0.02 | 50 | Old spruce | Dressings |
| yarrow | Medicine | 0.02 | 50 | Meadows | Bleeding |
| bandage | Medicine | 0.02 | 20 | Medkit, catalogue | Dressing |
| cloth\_scrap | Material | 0.02 | 50 | Salvaged garments | Dressings, wicks, char cloth |
| splint | Medicine | 0.3 | 20 | Made | Broken bones |
| hot\_water\_bottle | Comfort | 1.4 | 1 | Combined | +3 °C asleep |
| pillow | Comfort | 0.3 | 1 | Made | +5% sleep |
| rabbit\_skin\_blanket | Comfort | 1.5 | 1 | Made, 12 hare pelts | +6 °C asleep |
| sleeping\_pad | Comfort | 1.2 | 1 | Made | +2 °C asleep |
| bed\_net | Comfort | 0.3 | 1 | Made | No bug wake-ups |
| carved\_bowl | Cooking | 0.2 | 1 | Made | Spirits at meals |
| carved\_spoon | Cooking | 0.05 | 1 | Made | Spirits at meals |
| reflector\_oven | Cooking | 1.2 | 1 | Made | Flatbread without a stone |
| tin\_billy | Cooking | 0.07 | 1 | Combined | 0.4 L boils |
| grease\_tin | Container | 0.05 | 1 | Combined | 400 g lard, no scent |
| possibles\_bag | Container | 0.2 | 1 | Made | 6 slots on the belt |
| fish\_skin\_bag | Container | 0.1 | 1 | Made | 3 waterproof slots |
| game\_bag | Container | 0.3 | 1 | Made | 10 kg meat kept cool |
| bladder\_water\_bag | Container | 0.2 | 1 | Made | 3 L |
| birch\_bark\_bucket | Container | 0.3 | 1 | Made | 4 L |
| birch\_bark\_box | Container | 0.3 | 1 | Made | 9 dry slots |
| ulu | Tool | 0.2 | 1 | Made | Fish cleaning ×0.7 |
| snow\_saw | Tool | 0.4 | 1 | Made | Snow blocks |
| snow\_block | Material | 10.0 | 1 | Snow saw | Windbreaks, caches |
| leather\_strop | Tool | 0.3 | 1 | Made | Keen edges |
| bone\_awl | Tool | 0.02 | 1 | Made | Leather holes |
| storm\_lamp | Light | 0.5 | 1 | Made | Clean fat lamp |
| pitch\_torch | Light | 0.3 | 20 | Made | 45 min, rain-proof |
| fat\_lamp | Light | 0.3 | 1 | Made | 6 h per 100 g |
| can\_alarm | Signal | 0.15 | 1 | Combined | Wakes the player |
| casing\_whistle | Signal | 0.02 | 1 | Combined | 800 m |
| snow\_stake | Instrument | 1.0 | 1 | Made | Snow depth log |
| moose\_call | Tool | 0.2 | 1 | Made | Calls bulls |
| ice\_chisel | Tool | 2.5 | 1 | Made, caches | Ice holes |
| drawknife | Tool | 0.7 | 1 | Made, caches | Peeling ×0.5 |
| bucksaw | Tool | 1.5 | 1 | Made, caches | Bucking ×0.5 |
| felling\_axe | Tool | 2.0 | 1 | Made, catalogue | Felling |
| whetstone | Tool | 0.5 | 1 | Made | Edge to 100 |
| river\_stone\_hone | Tool | 0.4 | 1 | Made | Edge to 70 |
| longbow | Weapon | 0.8 | 1 | Made | Big game |
| broadhead\_arrow | Ammo | 0.03 | 12 | Made | Quiver |
| trail\_snare | Trap | 0.05 | 20 | Made | Hares |
| gill\_net\_segment | Trap | 1.5 | 1 | Made | Fish |
| wolf\_harness | Travel | 1.0 | 1 | Made | Sled team |

## Data and build

### Registries and components

**Items.** Every row of the master table is a registered item, ids as listed (`yearofice:cast_iron_skillet`), and every id has a row in `studio/inventory.json`. Each owner package registers its own items with its own `DeferredRegister.Items` (`food.FoodItems`, `build.BuildItems`), so file ownership holds (Minecraft development › Layer 6: Item model). The base classes `YoiItem`, `YoiToolItem` and `YoiFoodItem` live in `item`; tools that act on blocks extend `YoiToolItem`, which reads its numbers from `data_maps/item/tool.json`, not code.

**Static properties live in NeoForge data maps,** one map per concern (Layer 6): `data_maps/item/weight.json` holds `weight_g` (the skillet is `{ "weight_g": 2500 }`), `data_maps/item/props.json` holds `category`, `stack_class`, `floats`, `shiny`, `scent_level`, `frost_kg`, `material`, `max_condition`, `salvage` and `theft`, and every food also has `data_maps/item/food.json` (Food values, below). Designers retune without a rebuild. The vanilla max stack is set from `stack_class` at registration: `single` 1, `small` 50, `medium` 20, `by_weight` 80, because food by weight counts in 100 g units and 8 kg is a stack of 80 (RECONCILE 43). A datagen test fails the build if a registered item lacks a weight or a props entry.

```json
{
  "values": {
    "yearofice:cast_iron_skillet": {
      "category": "cooking",
      "stack_class": "single",
      "floats": false,
      "shiny": false,
      "scent_level": 0,
      "frost_kg": 0,
      "material": "cast_iron",
      "max_condition": 5000,
      "salvage": null,
      "theft": []
    },
    "yearofice:spoon_lure": {
      "category": "tackle",
      "stack_class": "medium",
      "floats": false,
      "shiny": true,
      "scent_level": 0,
      "frost_kg": 0,
      "material": "steel",
      "max_condition": 400,
      "salvage": null,
      "theft": ["yearofice:raven"]
    }
  }
}
```

**Changing properties are data components,** registered with `DeferredRegister.DataComponents` in `item.component.YoiComponents`, each with a codec and a stream codec so they save and sync. Condition uses vanilla's own durability components, so no custom bar is drawn.

| Component | Type | Notes |
| --- | --- | --- |
| `minecraft:damage`, `minecraft:max_damage` | Vanilla durability | Condition. `max_damage` = `max_condition` × the quality factor, set on the stack at craft (RECONCILE 6, 79) |
| `yearofice:edge` | short 0–120 | Cutting tools only; the thin line above the durability bar |
| `yearofice:quality` | enum crude, good, fine | Rolled in `item.QualityRoller` at craft completion |
| `yearofice:wetness` | byte 0–100, clock, stamp | Lazy (below); merges in 20-point bands |
| `yearofice:fresh` | state, clock, `due_dh`, `life_dh`, `curve` | Decay and time › How decay is stored; replaces `freshness` and its 0–1000 scale (RECONCILE 69) |
| `yearofice:item_temp` | frost points, state, clock, stamp | Lazy, as wetness; merges by state |
| `yearofice:weight_g` | grams | Overrides the map for one stack: a 4.1 kg pike, a full pack |
| `yearofice:keepsake` | record: story id, found day | Blocks salvage, theft, decay, burning and expiry |
| `yearofice:note_id` | note entry id | One item `yearofice:note` for every note and the 24 logbook pages (`logbook_01` to `logbook_24`) (RECONCILE 79) |
| `yearofice:fuel_l` | float litres | Coleman, avgas tin, oil can; renamed from `fuel` so it never reads as the wood fuel map (RECONCILE 79) |
| `yearofice:battery` | hundredths of a percent | The messenger and the headlamp |

**Lazy updates keep chests cheap.** No item ticks in a container. Freshness stores its due point on the holder's decay clock (`room:<id>`, `body:<player>`, a fixed cold store); wet and frost store a value, a clock and a stamp. Reading the stack (opening a chest, a tooltip, a merge) settles it from that clock in closed form, walking at most 72 hourly steps (Decay and time › The decay clock). Moving a stack between holders calls `DecayService.rehome`, which ages it by at most one band. Equal components are needed to stack, so `due` is floored to 1/40 of the food's life and wet to 20-point bands before comparing.

**Icons and tooltips.** `RegisterItemDecorationsEvent` adds one `IItemDecorator` per mark: the edge line, the quality notch or tick, the droplet, the frost rim and the warm glow. Tooltip lines come from `ItemTooltipEvent` in the fixed order of How a stack reads, built in `client.item.ItemTooltips` from the components and the `decay_heads` payload, which gives the client the clock values it needs.

### Loot, salvage, theft and drift

**Loot tables** live at `data/yearofice/loot_table/setpiece/<site>.json` and roll once when stage 5 places the structure, keyed to the site seed. Three custom loot functions set the found state: `yearofice:set_condition`, `yearofice:set_quality` and `yearofice:set_fuel`.

```json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "yearofice:coleman_lantern",
          "functions": [
            { "function": "yearofice:set_condition", "value": 0.8 },
            { "function": "yearofice:set_quality", "quality": "good" },
            { "function": "yearofice:set_fuel", "fuel_l": 0.3 }
          ]
        }
      ]
    },
    {
      "rolls": { "type": "minecraft:uniform", "min": 1, "max": 3 },
      "entries": [
        { "type": "minecraft:item", "name": "yearofice:tin_can", "weight": 4,
          "functions": [{ "function": "yearofice:set_condition", "value": 0.6 }] },
        { "type": "minecraft:item", "name": "yearofice:enamel_mug", "weight": 2 }
      ]
    }
  ]
}
```

Files: `station_nine.json`, `plane_wreck.json`, `trapper_cabin.json`, `mining_camp.json`, `lookout.json`, `roadhouse.json`, `trapper_cache.json` (tool upgrade pool and extra pool, both without repeats per world through a `yearofice:world_unique` condition), `elin_last_camp.json` and `river_drift.json`.

**Combining and salvage** are data recipes of two new types, `yearofice:combine` (handled in `Item#overrideOtherStackedOnMe`) and `yearofice:salvage` (hold-to-work on the item), under `data/yearofice/recipe/`. Quality rework and finishing are a third type, `yearofice:rework`.

**Theft** runs as Brain behaviours on each thief species (Minecraft development › Entities and AI): steal, cache-shiny for ravens and gnaw-handle for porcupines, each reading the `theft` and `shiny` fields of `props.json`. Each theft writes a sign marker through World edits and records where the item went in `item.TheftLedger`, saved as level `SavedData`.

**Drift** uses `item.DriftingItemEntity`, an `ItemEntity` that reads `floats`, moves with the `env.water` flow field and parks at snag blocks tagged `#yearofice:snags`. Sunk items, items frozen in at freeze-up and items that leave loaded chunks become `lost_gear` blocks through World edits `DRIFT` ops (RECONCILE 36, 97); at breakup the `DRIFT` op carries frozen-in gear to a snag.

### Milestones and tests

| Part | Milestone |
| --- | --- |
| Data map, condition, quality, stack rules, tooltips, icon marks | M2, with inventory and encumbrance |
| Wet, temperature, the inner pocket (Curios slot `yearofice:inner_pocket`) | M4, with the body |
| Station Nine and wreck loot, combining, signals | M3 |
| Instruments, the Forecast skill, salvage, rework | M5 |
| Theft behaviours, the porcupine, gray jays | M6, with the animals |
| Drift, freeze-in, spring finds under snow | M7, with the year |
| Mining camp, lookout and roadhouse tools, comforts, decay | M8 |
| The ring, the breakup board, keepsake shelf, ground-to-air signs | M9, with the radio loop |

NeoForge GameTests, named `yearofice:item/<test>`, beside the lead's `item/food_units`, `item/fine_is_130`, `item/every_item_has_weight` and `item/bands_merge`:

- *Frost test:* a 1 kg full steel bottle in a −10 °C test chamber reaches Frozen at 60 ±2 game minutes, at −40 °C at 15 ±1, and thaws within 2 m of a Steady fire in 20 ±1.
- *Quality test:* 1,000 simulated crafts at each skill word give Crude, Good and Fine within 3 points of the table; a Fine item's durability is exactly 1.3 times a Good one's.
- *Lazy test:* a chest of 27 food stacks left 10 game days at a logged 5–15 °C lands in the same 1/40 freshness band as a stack ticked every game minute, and opening it costs under 1 ms.
- *Loot test:* over 200 seeds every Station Nine roll holds the thermometer, lantern, auger and photograph; no two caches in one world share a tool upgrade or an extra.
- *Drift test:* a floating paddle dropped at the station in October locks into the ice, and after breakup lands at a snag 0.5–4 km downstream on at least 95% of 100 seeds; a dropped hatchet never moves.
- *Theft test:* a hatchet left in the chopping block for 10 nights inside a porcupine range is gnawed at least once on 9 of 10 seeds; a keepsake left in the open for 30 days is never taken.

**Review audit (food loop and body needs).** Reviewer A, Food and body, 2026-10-19. Each step is traced as a player does it in Minecraft. Keys follow Minecraft engine › Controls: right mouse uses or holds to work, R quick-eats, Q drops, E opens the pack, Z opens the rest radial, M opens the map. Status is Ready, Fixed in this round, or Open (a decision for Ruhan). Test names are GameTests unless marked otherwise.

| Step | Rule, number, id, test | Rule location (tab › section) | Status |
| --- | --- | --- | --- |
| Get: hunting yield | One carcass per kill and no vanilla drops. `loot_table/butchering/<species>.json` in 100 g units. Bull moose 180 kg meat (≈ 239,000 kcal), beaver 6.5 kg (≈ 9,300), hare 1,070, spruce grouse 640, ptarmigan 440 (RECONCILE 132) | Blueprint › Butchering and yields; Bestiary › Parts by species | Fixed (RECONCILE 40, 42, 53) |
| Get: fish | Item = species id with `weight_g`; kcal per kg × weight; stocks deplete | Blueprint › Fishing in depth; Items › Food values | Ready (owner C) |
| Get: plants and crops | Berries per species, rose hips, spruce tips, potato 77 per 100 g, barley 350 | Plants and fungi; Blueprint › Crop farming; Items › Food values | Fixed (ids `potato`, `barley`, `cabbage`, `rose_hips`, `spruce_tips`) |
| Get: eggs, livestock, drops | `wild_egg` in June; `hen_egg` and `goat_milk` from catalogue stock; supply drops bring seeds and salt, never bulk food | Items › Food values; Open world in depth › The radio | Fixed (new ids) |
| Carry | Food counts in 100 g units, stack 80 (8 kg); load bands from Inventory; carcasses by hand (Shift + right mouse), Q drops | Minecraft development › Layer 6; Bestiary › Carrying | Ready |
| Cook: each station | Spit 30 min a kg, stone fry 10 min a 0.5 kg (20 g lard), coal bake 40 min, stew 60–90 min at ×1.15 with bones, render 60 min a kg; doneness ring Raw, Rare, Done, Burnt; hold right mouse at the station | Blueprint › Cooking | Ready |
| Cook: mixed dishes | `yearofice:dish` + new component `yearofice:nutrients` summed at finish; `food/dish_sums` | Items › Food values | Fixed (needs the lead's Layer 6 entry) |
| Preserve: rendering | 1 kg `animal_fat` (7,000) → 0.7 kg `lard` (6,300) + 0.1 kg `cracklings` (550); `food/render_never_gains` | Blueprint › Cooking | Fixed (the 0.8 kg figure gave free kcal) |
| Preserve: drying and smoking | `dried_meat` and `dried_fish` at 40% weight, state `dried` 60 d, hot-smoked 30 d, cold-smoked 180 d | Blueprint › Preserving in detail; The smoker | Fixed (`dried_fish` new) |
| Preserve: salting, freezing | Salt ×2 shelf life, place factor 0.5; below −5 °C decay stops, between 0 and −5 °C L is 30 d; `decay/frozen_stops` | Blueprint › Spoilage model; Decay and time › The cold curve | Ready |
| Preserve: pemmican | 50 : 45 : 5, 560 kcal per 100 g, 365 d; `pemmican` | Blueprint › Preserving in detail; Items › Food values | Fixed (5,000 → 5,600 per kg; id was missing) |
| Preserve: sour fish | 115% of the heads' kcal, 20 d; sealed container 10% botulism, grass pit never; `decay/botulism_container` | Decay and time › Sour fish and botulism | Fixed |
| Store: caches, cellar, ice house | Clock keys `room:<id>`, cellar 3 °C, ice house 1 °C, permafrost cellar −6 °C; mice 2–3% a week Oct–Apr without 3 deadfalls, `droppings` flag 20% gut illness; bears by RECONCILE 23 | Decay and time › How decay is stored; Items › Item decay; Structures › Food and storage | Fixed; ice house value conflicts with the lead (below) |
| Eat | Hold right mouse 3 s a portion, or R; meal up to 1.5 kg; stomach 1.8 kg, empties 7% an hour awake and 4% asleep; prompt "R  Eat dried meat (+6%, stomach 36%)" | Blueprint › The body tick; Minecraft development › Vanilla needs replaced | Fixed (RECONCILE 13) |
| Absorb | Gut factors multiply: gut illness 0.5, parasites 0.85, lean-meat nausea 0.8, fat limit 0.8 | Blueprint › The body tick | Fixed |
| Macros and vitamin C | Grams of protein and fat per 100 g; carbs derived; C-days with 10 mg = 1; methods keep raw 100%, Rare 50%, bake and stew 50%, fry 25%, roast 0%, potatoes always 100% | Items › Food values; Blueprint › Health › Vitamin C | Fixed (two C-day rule sets merged) |
| Spoilage and bad food | Lazy settling on read; 1/40 bands; Fresh to Rotten ladder; tapeworm 5% raw game, trichinella 25% raw bear; gut illness floor 12 h | Blueprint › Spoilage model; Decay and time › Eating decayed food | Fixed (RECONCILE 69, 70, 120 C4, C5) |
| Calories and weight | Burn × BMR ÷ 1,794; Δkg = (absorbed − burned) ÷ 2,400; snow walking ×1.3 / ×1.8; +30 kcal an hour outdoors below −20 °C | Blueprint › Calories and weight | Fixed (RECONCILE 17; snow and cold-air costs new) |
| Water | 3 L reserve, −3.5 an hour, heavy work −5 more, blackout at 0 for 12 h | Blueprint › Water | Ready |
| Energy and sleep | Caps table; 1.25 bolts an hour × quality; sleep rate 180 game s a tick; sleep only when every player sleeps in a later co-op world | Blueprint › Energy, Sleep | Fixed (RECONCILE 115) |
| Warmth and wet | Feels-like formula; dW per minute; soaked rule; per-part frost counters | Blueprint › Warmth; Body, injury and sickness | Ready |
| Bugs | B\_total of four terms; head net source | Blueprint › Bugs; Bestiary; Birds and the air | Fixed (RECONCILE 61) |
| Spirits | Daily clamp −3 to +4; lanterns replace Evening light; sweet +2 daily, named sweets +5 as events | Blueprint › Spirits | Fixed (RECONCILE 25) |
| Injuries and illness | `BodyService.applyInjury` only; ten parts; botulism and cold cough in Health | Blueprint › Health; Body, injury and sickness | Fixed (RECONCILE 66, 77, 90, 120) |
| Collapse and SOS | Closed-form 14-day jump; `left_pack` never expires; drowning by swim-time band | Blueprint › Collapse and SOS | Fixed (RECONCILE 83, 84, 89, 123) |
| Presets and debug | `yearofice-common.toml` presets; `/yoi preset`, `/yoi set`, `/yoi food`, `/yoi decay`, `/yoi spirits`; day length 24–72, default 24 | Blueprint › Realism presets; Decay and time › Code; Items › Food values | Fixed (RECONCILE 117, 118) |
| Endless stay-out choice | 14 days (default), 1, 2 or 5 years on the SOS screen | Blueprint › Open decisions | Decided by Ruhan 2026-10-19 (RECONCILE 148) |
| Weight per kcal | 2,400 kcal a kg makes one lost cache in a bad winter an SOS; 3,500 would make it BMI 18 | Blueprint › Survival tuning | Decided by the Director (RECONCILE 135): 2,400 at Standard and Brutal, 3,500 at Gentle |

**A year at Standard, run by hand.** The competent player of the balance simulator: about 3 heavy hours a day, Normal ration, a moose and a caribou in fall, and the 40% loss floor for spoilage, dependents, mice and raids taken off everything stored. The body is the default 183 cm, 77 kg; BMI 17 is 56.9 kg. At 2,400 kcal a kg, a 1,000 kcal daily gap moves weight 0.42 kg a day.

| Months | Burn a day | Food in a day, and from what | Balance | Weight at the end |
| --- | --- | --- | --- | --- |
| May | 3,300 (camp, fishing, felling begins) | 2,600: 3,800 kcal of crackers, then grayling (about 4 a day), snared hares, spruce tips, breakup ducks | −700 | 68 kg, BMI 20.3; the M5 test's "above 19 by day 30" holds |
| June | 3,300 | 3,300: pike, whitefish from the gill net, wild eggs (about 8,000 kcal a month), hares | 0 | 68 kg |
| July–August | 3,400 | Normal, plus about 40 kg of salmon dried or smoked (≈ 140,000 kcal stored); berries | + up to 500 on Feast days | 72–74 kg |
| September–October | 3,500 (hunting, hauling, chinking) | Normal from fresh meat; bull moose ≈ 239,000 and caribou ≈ 60,000 kcal butchered; rose hips and potatoes stored | 0 to +300 | 74–75 kg |
| November–April | 2,600 on trapline and ice days, 4,000 on hauling days; about 2,900 on average | Stores after the 40% floor (≈ 315,000 kcal), trapline hares at 1,070 each (≈ 160,000 in a normal hare year, 32,000 in a crash) and set lines and tip-ups (≈ 160,000): about 3,510 a day in a normal year, 2,800 in a crash | 0 on Normal, with about 100,000 kcal left at the thaw; −100 in a crash year | 75 kg; about 67 kg (BMI 20.0) after a crash winter |

**What the year shows.** No month is impossible for a competent player, but one is close: a hare crash on top of a cache lost to a wolverine leaves the winter about 80,000 kcal short, which at 2,400 kcal a kg is 33 kg and an SOS by March, unless the player adds about 2 hours a day of set lines and tip-ups (≈ +450 kcal a day). The messenger names that gap in February ("A ration set that reaches April"). No month is trivial: from July to October food is plentiful but rots in 2–9 days unless the player dries, smokes, renders or freezes it, and every full rack draws bears. Winter costs about 45% of the fall's stores plus 3 outdoor hours a day. The fat rule holds: red meat alone gives about 34 g of fat a day (a 38% meter), so lean-meat sickness strikes only on hare and grouse (0.8 g per 100 g) without lard. Vitamin C holds only with frozen raw fish (0.5 C-day a day) plus stored hips or sauerkraut; with neither, scurvy starts on day 31 after the reserve was last full, as Health's test says. Two free-calorie exploits were found and closed: rendering gave 7,750 kcal from 7,000, and sour fish gave 120% of its parts. The stew +15% applies once per bone, because a stewed bone becomes a tool bone.

**Conflicts for other owners.** Minecraft development › Layer 5 (G): `fixed:ice_house` is −4 °C there but 1 °C in Structures and Decay; use 1 °C. Layer 6 (G): add `yearofice:nutrients` and `dish_id`; the master inventory lists `page`, which RECONCILE 79 replaced with `note`. Blueprint › Clothing (G): the head net is not in the crate; it is sewn from the outpost window screen. Crafting › Render fat (D) and The smoker (D): 0.7 kg lard plus 0.1 kg cracklings, and pemmican 5,600 kcal per kg. Every concept from the video (F): Eating fat's "0.8 kg of lard", the 900 kcal raw-fat row and the 5,700 moose-meat day (5,100 at 1,150 a kg); Stomach fill's mealtimes should match Auto-eat's two windows. Wildlife and fish (E): moose ≈ 239,000 and beaver ≈ 9,300 kcal (now done there, RECONCILE 132). Waters and fish › Sour fish (C): defer to Decay and time (115% of the heads, botulism 10% only in a sealed container).
