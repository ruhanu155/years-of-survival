# World map

The world is no longer an 8 km square inside a border. It is three rings: the home valley of 16 designed regions around Station Nine, a designed 65 km Interior of 256 districts around it, and endless procedural wilderness beyond, out to Minecraft's default limit. This tab draws the maps, lists every district, names every value the seed sets and how each one changes play, and gives the Minecraft rules for building all three rings.

## Three rings

**The point.** The home valley stays exactly as designed, and it is now the middle of a real region. Every name, rule and number for the 16 regions A1–D4 stays as written in The open world. The Interior adds the big country the home river drains into, and Beyond makes sure no walk ever ends at a wall.

| Ring | Extent in blocks | Size | Made of | Built | What it holds |
| --- | --- | --- | --- | --- | --- |
| 1 Home valley | x and z −4,096 to +4,095 | 8.2 × 8.2 km, 67 km² | 16 regions of 2,048 blocks (A1–D4) | Layers at 2 m cells; the core B3 at world creation, the rest on approach | Station Nine, the start, the story's heart, every rule in the main tab |
| 2 Interior | −32,768 to +32,767 | 65.5 × 65.5 km, 4,295 km² | 256 districts of 4,096 blocks; the home valley is the central 2 × 2 (H8, I8, H9, I9) | The Interior plan at world creation; district layers at 4 m cells on approach | Authored geography placed by seed: a glacial river, a range, lakes, tundra, burns, a ghost town, 30–40 set pieces |
| 3 Beyond | Out to the vanilla world border, ±29,999,984 | Endless in practice | Provinces of 65,536 blocks, each 16 × 16 districts | A province plan on approach; district layers at 4 m cells | The same biomes, plants, animals and fish; natural features only, no story set pieces or notes |

**Why 4,096-block districts.** A district is exactly 2 × 2 home-valley regions, 256 × 256 chunks and 8 × 8 Minecraft region files. So the home valley is the Interior's central 2 × 2 with no seam, and the Interior spans ±32,768, not ±32,000.

**District codes.** Columns A–P run west to east and rows 1–16 north to south, as in the home valley. The code is column = floor((x + 32,768) / 4,096) and row = floor((z + 32,768) / 4,096) + 1. The messenger writes "District K10" for districts and "Region B3" inside the home valley, so the two grids never read alike.

**The frame that never moves.** The home river leaves the home valley at its west edge and joins the Grey River at the Forks, so upriver is always east, as now. The Varden Range is always the southern skyline and the uplands always lie north and east. On the half of seeds where the home valley mirrors east to west, the whole Interior mirrors with it, so the Forks is always downriver.

**Scale in play.** At the travel rate, 1 km on foot is about 1.1 game hours and 5.5 real minutes. The travel rate is fixed, so these figures hold at every day length the player picks (24–72 real minutes; Blueprint › Open decisions). Crossing the Interior is about 70 game hours on foot, 6 real hours of walking. The home valley is a season's world; the Interior is a life's; Beyond is the frontier that outposts, the canoe and the sled push into.

## The Interior map

The Interior is a 65 km square of interior-Alaska country around the home valley. The map below is the seed-default layout; every named feature moves within its rules on other seeds (What the seed decides).

&#91;embedded content: The Interior, seed-default layout · 65.5 km, 256 districts; seeds rearrange every named feature within its rules\]

**Reading it.** North is up and each grid square is one district, 4.1 km on a side. The Grey River runs from the Varden Glacier in the south-east, along the range foot, and out at the west edge; the home river (the Ninemile) joins it at the Forks. The caribou corridor crosses the north from the Hundredmile Barrens toward the Lichen Benches, and the Marrow Trail links the old riverboat landing to the ghost camp through the home valley.

## The home valley map

The 8 km home valley keeps its 16 regions and every rule in The open world. This map places what those rules generate, at the positions the seed-default layout gives them.

&#91;embedded content: The home valley, seed-default layout · 8.2 km, 16 regions; seeds rearrange set pieces, dens and routes within their rules\]

**Reading it.** Each square is a 2 km region with its cell code. The three wolf territories follow the columns: the Barrens pack in column A, the home pack across B and C in rows 1–3, the Upriver pack in column D. The dashed caribou lines are the three crossing routes the year profile chooses between; the Marrow Trail crosses the valley along the north benches past the hot spring roadhouse.

**What the map shows that the tables did not.** The home river is now the Ninemile, a named river with a source and a mouth. Windgap Pass (D1) and the Caribou Barrens (A2) are where the Interior's caribou corridor enters the valley. Slough Country (A3) is the doorway to the Forks, 7 km west of Station Nine.

## Real-world reference

**Where it is.** The map is modelled on interior Alaska around 64°N: the Tanana valley country between the Yukon–Tanana uplands to the north and the Alaska Range to the south. Every name is fictional, and the numbers below are general knowledge, not surveyed; tune them in data.

| Real feature | What the map borrows | Game version | Change for play |
| --- | --- | --- | --- |
| A braided glacial river like the Tanana, fed by Alaska Range glaciers, flowing north-west to the Yukon | A grey, silty river 1–3 km across its gravel belt, many channels, high water in July from glacier melt, low and clear in winter | The Grey River, 100–400 m elevation, 4–12 channels | Shorter (about 90 km in the Interior) and narrower so it can be crossed by canoe in under an hour |
| The Alaska Range, peaks of 2,000–6,000 m with large glaciers | A wall of peaks on the southern skyline, glaciers down to the foothills, a few high passes | The Varden Range, peaks of 1,600–2,010 m, 3–6 glaciers | Capped at 2,010 m by Minecraft's build limit (y 2,031); glaciers scaled to 2–8 km long |
| The Yukon–Tanana uplands: rounded domes, treeline at 750–900 m, tundra summits | Bald domes with long views, tussock and lichen tundra, fire lookouts on the tops | The Bald Domes and the Hundredmile Barrens, 600–1,310 m | Denser landmarks: a lookout or tor on every third district |
| A migratory caribou herd of tens of thousands that crosses the uplands each fall | A herd that calves in the high barrens and winters in lichen woodland | The Hundredmile herd, 800–3,000 animals | Smaller, and its corridor always touches the home valley |
| Permafrost lowlands like the Tanana and Minto flats: black spruce, muskeg, thermokarst lakes, sloughs | Wet flats that are slow in summer and a highway in winter | The Sinkwater Flats and the Loon Lakes | Permafrost on 30–60% of lowland, set by seed |
| Hot springs in the uplands, once roadhouse stops | A steaming pool on an old trail | Halfway Hot Springs, plus the home valley's spring in C2 | Two in the Interior at most |
| Gold-rush districts of 1902–1920 with dredges, ditches and ghost camps | A mined-out creek country with a ghost town | The Kettle Hills district and Marrow City (1906–1918) | Holds the iron and tools the home valley lacks |
| Winter sled trails with roadhouses every 30–50 km; an early Army telegraph line | Trails, mileposts, roadhouse ruins, poles with wire | The Marrow Trail and the Signal Line | Wire to salvage, a trail to follow |
| Lightning fires that burn large areas of black spruce every summer | A patchwork of burns of every age | The Smoke Hills and 2–5 burns elsewhere | Burn ages set by seed and growing each year |

**The climate it borrows.** Interior Alaska is continental and dry: winters reach −40 °C in the valley bottoms while the domes stay 5–10 °C warmer under the inversion. Summers reach 25–30 °C, with lightning in late June and July. Snowfall is light in the flats (60–80 cm on the ground by March) and heavy in the range.

&#91;embedded content: North-to-south elevation profile through Station Nine · seed default, 65.5 km\]

**What the profile shows.** The Interior falls from the domes to the home river, rises only a little over the lakes, drops to the Grey River and then climbs 1,600 m to the range crest in 15 km. Treeline sits at 750–900 m, so the domes and range are open country with long views and no firewood. Winter cold drains into the flats; the benches above them are the warm places to build.

## District catalogue

Each of the 256 districts has one type, which sets its terrain, cover and rules, and most belong to a named area that sets its story and set pieces. The 4 home-valley districts keep their 16 regions and are not typed. A small named area can sit inside a large one, such as Black Gate Canyon in the Upper Ninemile; the smaller one wins on the district card.

### District types

| Type | Elevation | Terrain and cover | Lives there | Grows there | Resources | Hazards | Travel, summer / winter |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Icefield | 900–2,010 m | Glacier ice, moraine, rock islands; no trees | Ptarmigan, a passing wolverine | Nothing | Clear meltwater, moraine stone, iron on rock islands, natural ice caves | Crevasses hidden under snow; glacier wind 6–15 m/s; whiteouts | 0.5 on crevassed ice, 1.0 on bare ice with ice creepers / 0.6 roped and probing |
| Alpine range | 1,000–2,010 m | Cliffs, scree, snowfields | Dall sheep, mountain goats, hoary marmot, pika, golden eagle | Moss campion, lichen | The richest iron (6–12 clusters a district), flint-like chert, shelter faces | Rockfall, small avalanches, falls | 0.5 up / 0.4 in snow over 1 m |
| High pass | 900–1,300 m | A saddle of scree and tundra | Caribou in a strong year, wolverine | Low willow | The only ways south into Beyond | Wind over 10 m/s on most winter days | 0.8 / 0.6 |
| Foothill benches | 300–900 m | South-facing birch and aspen, old burns | Black bear, ruffed and spruce grouse, moose in winter | Blueberry, lowbush cranberry, rose hips, morels after fire | The best firewood and canoe birch | Bears in berry season | 0.8 / 0.75 |
| Upland domes | 650–1,310 m | Rounded tops, tundra above 750–900 m, spruce on the lower flanks | Ptarmigan, arctic ground squirrel, grizzly, caribou on passage | Blueberry, crowberry, lichen | Long views, lookout sites, tors for landmarks | No firewood or windbreak above treeline | 1.0 / 0.9 wind-packed |
| Barrens | 600–1,100 m | Tussock and lichen tundra, tundra ponds | The Hundredmile herd May–August, wolves, grizzly, sandhill cranes | Cloudberry, blueberry, cotton grass | Antler sheds, the herd itself | Tussocks; summer bugs ×1.5; no shelter | 0.7 in tussocks / 1.0 |
| Lichen woodland | 200–500 m | Open spruce on sandy terraces over reindeer lichen | The herd November–April, marten, red squirrel | Lichen (slow: 30 years to regrow after fire) | Dry deadwood, easy walking | Few; cold-air pooling at night | 0.9 / 1.0 |
| Black spruce flats | 100–200 m | Stunted spruce, muskeg, thermokarst ponds over permafrost | Hare, lynx, moose at the edges, wood frog | Cranberry, cloudberry, Labrador tea | Peat, bog iron, ice-rich ground for an ice cellar | Bog holes, drunken trees, frost heave; −45 °C nights | 0.6, boots wet / 1.1 on frozen muskeg |
| Sloughs | 100–150 m | Side channels, oxbows, sedge and willow | Ducks, geese, muskrat, beaver, moose, pike, sheefish | Cattail, sedge, willow | Most moose per km², waterfowl in May and September | Mosquitoes ×1.5; soft banks | 0.6 or canoe / 1.0 on ice |
| Lake country | 150–300 m | 12–30 lakes in spruce and birch, eskers between | Loons, pike, whitefish, lake trout in deep lakes, otter | Bog blueberry, wild rhubarb on old sites | Fish all year, ice roads, supply-drop zones | Thin ice at inlets; wind waves | 0.75 or canoe / 1.2 on lake ice |
| Glacial river | 100–400 m | The Grey's gravel belt, channels, poplar and willow islands | Moose on the islands, beaver, fall chum, eagles | Willow, balsam poplar, horsetail | Driftwood, gravel bars with chert, washed-out iron | Silty water, quicksand silt, July high water, winter overflow | 1.0 on bars, canoe across / 1.2 on the ice road |
| Clear tributary | 150–600 m | A spring-fed creek in spruce | Spawning salmon, grizzly, otter, dipper | Willow, alder | The best salmon water; open water all winter at springs | Grizzlies at the riffles; thin ice | 0.75 / 0.8 |
| Canyon | 200–600 m | A rock gorge with falls | Dippers, mink, grayling | Moss, fern | Shelter faces, falls pools | Drowning, rockfall, ice bridges that fail | 0.5 on the rim; portage only / 0.7 |
| Burn | Any lowland | Spruce killed by fire, by age class | Hare and lynx in regrowth, moose in 10–30-year burns | Morels (year 1), fireweed, blueberry ×2 (years 2–4) | Thousands of dry standing snags | Windfall in wind over 8 m/s; ash mud in rain | 0.7 in fresh deadfall / 0.8 |
| Hot spring basin | 300–700 m | A sheltered basin round a spring | Moose winter yard, ducks all winter, otter | Early greens, rhubarb at old roadhouses | Open water, a warm soak, a long garden season | Thin ice on the outflow | 0.8 / 0.8 |
| Old mining district | 200–700 m | Tailings, dredge ponds, ditches, cabins | Beaver in the dredge ponds, grayling, ravens | Fireweed and willow on tailings | Scrap iron, tools, nails, stoves, wire | Shafts and pits 3–8 m deep, rusted metal cuts | 0.75 / 0.8 |
| River valley | 120–600 m | A spruce-and-birch valley round a river, like the home valley's row 3 | The home valley's mix | The home valley's mix | Everything at the home valley's densities | The home valley's | 0.75 / 1.0 on river ice |

**Silt.** The Grey's water carries glacial flour from May to September. It is safe to drink only after standing 6 game hours in a pot, or drawn from a clear side channel; fish cannot be seen in it, so sight-fishing and spears fail there.

### The north: domes, barrens and burns

| Named area | Districts (seed default) | Type | Elevation | Landmark, from 4 km | Set pieces | What it gives | Seasons |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Smoke Hills | A1–D4 | Burn (three burns: 6, 28 and 64 years old) on low hills | 300–900 m | Cinder Knob Lookout on a bald knob | Cinder Knob Lookout; 2 of Arvid Lund's outer line cabins | The best hare, lynx and marten country; morels and berries on the 6-year burn; dry snags for a decade | Lightning fires start here first, late June to July; the freshest burn can reburn |
| Lichen Benches | A5–F6 | Lichen woodland on sand terraces | 200–500 m | The Sand Cliffs, 40 m bluffs over the Grey | None | The Hundredmile herd from November to April: winter caribou close to home | Quiet in summer; the herd arrives with the first 30 cm of snow |
| Bald Domes | E1–K7 | Upland domes | 650–1,310 m | Old Baldy, 1,310 m, with Dome Lookout | Dome Lookout and its survey map; a stone cairn at each summit | Views of 30 km, blueberries, ground squirrels, the corridor's middle | Snow from mid-September; wind-packed and fast in winter |
| Hundredmile Barrens | L1–P7 | Barrens | 600–1,100 m | The Antler Tors, three granite towers 20 m high | None (open country) | The herd's summer range: calving in late May, bulls in August, antler sheds in spring | Bugs are worst here in July; −40 °C wind in winter, no shelter |
| Sawtooth Ridge | M5–O6 | Alpine range in miniature | 900–1,250 m | The Sawtooth, a serrated rock crest | Sawtooth Lookout | Iron (3–6 clusters), Dall sheep, the eastern view | Snow from early September |
| Ninemile Lakes | O7–P8 | Lake country at the Ninemile's source | 600–750 m | The Twin Lakes seen from the ridge | None | Lake trout and grayling, loons; the Ninemile's headwaters | Ice from early October to early June, the longest ice in the Interior |

### The east: the upper Ninemile and the Kettle Hills

| Named area | Districts (seed default) | Type | Elevation | Landmark, from 4 km | Set pieces | What it gives | Seasons |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Upper Ninemile | J7–N9, less K8–K9 and M9 | River valley | 200–600 m | The Ninemile's white cutbanks | 1 of Arvid's outer line cabins; the Signal Line's east relay cabin | The home valley's mix with fewer people-signs: more moose and marten | Freeze-up 1 week before the home valley |
| Black Gate Canyon | K8–K9 | Canyon | 250–550 m | Spray from Gate Falls, a 9 m fall | None | A second canyon of falls and pools; grayling and burbot | The portage (700 m) is the only way up the river; ice bridges fail every March |
| Halfway Hot Springs | M9 | Hot spring basin | 450 m | A steam plume below −15 °C; the roadhouse's chimney all year | Halfway Roadhouse and the springs | A 56 °C source and 40 °C pool, open water, an early garden, a warm stop at mile 31 of the Marrow Trail | The moose yard fills in January |
| Kettle Hills | L10–O12 | Old mining district | 300–700 m | The Marrow dredge, 16 m tall, in its pond | Marrow City ghost camp; 2 prospect adits; the dredge | Iron tools and scrap: an anvil, a forge's tongs, wire, stoves, nails | Gravel ditches flood in May; the camp is snowed to the windowsills by February |

### The south: lakes, the Grey and the Varden Range

| Named area | Districts (seed default) | Type | Elevation | Landmark, from 4 km | Set pieces | What it gives | Seasons |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Loon Lakes | G10–J11 | Lake country | 150–260 m | Long Lake, 3.5 km long, seen from any esker | A float-plane dock on Long Lake (supply drops land here once the radio works) | 16 lakes over 2,000 m² at seed default, pike and whitefish everywhere, lake trout in the 3 deepest | The first ice in the Interior, safe by late October; breakup late May |
| Varden Foothills | A12–P13 | Foothill benches | 300–900 m | Grey Bluff, a 120 m bluff over the river | Grey Bluff Lookout | The best birch and berries in the Interior, black bear, grouse | Chinook winds from the passes melt the snow in mid-winter (Land and seasons in play, The warm wind) |
| Varden Range | A14–P16 | Alpine range and icefield | 1,000–2,010 m | Mount Varden, 2,010 m, visible from every district | None; natural ice caves | Iron, sheep, goats, the Grey's ice | Snow on the peaks all year; avalanche season March–April |
| Varden Glacier | N14–O16 | Icefield | 400–1,900 m | The glacier's terminus, a 40 m ice face | None | The Grey River's source, ice caves, the coldest wind | Melt surges on July afternoons raise the Grey 0.5–1 m |
| Fang Glacier and Fang Lake | D14–E16 | Icefield with an ice-dammed lake | 700–1,700 m | The Anvil, a 1,880 m flat-topped peak | None | A lake that drains under the ice | The lake bursts out in 1 year of 4, in August (What the seed decides) |
| Bonnet Glacier | I15–J16 | Icefield | 900–1,800 m | The ice tongue seen from Grey Bluff | None | The source of Bonnet Creek, a short grey tributary | — |
| Ptarmigan Pass | G14 | High pass | 1,050 m | A survey mast on the saddle | None | A way south into Beyond | Open on foot July to September; a sled route in March on crust |
| Sled Gap | K14 | High pass | 1,180 m | Mileposts of the old trail over the range | Milepost 45 and a collapsed shelter cabin | The old winter route south; the Signal Line crosses here | Passable only on winter crust or in August |
| Redwater Creek | I12–I14 | Clear tributary | 150–700 m | Red-stained rock at its mouth | An old fish wheel and drying racks | The Interior's best chinook spawning water, late June to August | Grizzlies at its riffles every July |
| Clearwater Slough | K12 | Clear tributary, spring-fed | 160 m | Steam over open water below −25 °C | None | Coho in September–October; open water all winter; burbot | Never freezes in its upper 2 km |
| Chum Flats | L13 | Glacial river with upwelling | 180 m | Eagles on the bars in October | None | The fall chum run, October to early November; 30–60 eagles | Fish spawn under the first ice |

### The west: the flats, the Forks and the Landing

| Named area | Districts (seed default) | Type | Elevation | Landmark, from 4 km | Set pieces | What it gives | Seasons |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Sinkwater Flats | A7–F11 | Black spruce flats, with sloughs | 100–200 m | None in summer; the Sand Cliffs and the range from any rise | The Signal Line's west relay cabin | Hare, lynx, cranberries, bog iron, thermokarst ponds with pike | Slow, wet going in summer; the fastest country in the Interior in winter; the coldest nights |
| The Sloughs | D9–F10 | Sloughs | 100–140 m | The Landing's wireless mast | None | Moose, waterfowl, muskrat, the wood bison's summer reach from A3 | Mosquitoes from June 5 to July 25 |
| Grey River Landing | C7 | Glacial river bank, with an airstrip | 105 m | The warehouse's tin roof | Grey River Landing: trading post, warehouse, riverboat landing, gravel airstrip | J.O.'s 1978 fuel cache, a boat winch, tin and glass, the trader's ledger | The airstrip floods in July; supply drops can land here |
| The Forks | G9 | Glacial river and river valley | 115 m | The Forks Roadhouse's two-storey ruin | Forks Roadhouse; J.O.'s signal camp on the bar | The meeting of the two rivers: fish passing in every run, the crossing of the Marrow Trail | The Grey's ice is safe 2–3 weeks after the Ninemile's |
| The Grey River | B7 to O15, diagonal | Glacial river | 100–400 m | Its grey braids from any height | — | Driftwood, travel, the salmon highway | Breakup ice jams in May; overflow in winter (Water, calls and encounters) |

### Set pieces of the Interior

Set pieces keep the home valley's rules (Biomes and resources, Set pieces): authored templates, 2–3 variants, placed where the fit rule matches. Interior set pieces never sit within 6 km of Station Nine, so the home valley's discoveries come first.

| Set piece | Per Interior | Fits where | Default place | What it gives | Story |
| --- | --- | --- | --- | --- | --- |
| Grey River Landing | 1 | A high outside bank on the Grey, 15–30 km downriver of the Forks | C7 | A boat winch (pulls a log or canoe up a bank), 6 sheets of roofing tin, 20 glass jars, a cast-iron cookstove, J.O.'s fuel cache (2 jerrycans, 40 L of avgas: a fire accelerant that lights wet wood at once) | The trader's ledger, 1905–1952, lists sales to Arvid Lund, Kessler and Doane. A notice on the wall: "Sternwheeler Grey Belle lost in the Ninemile sloughs, June 1931" (the A3 stack) |
| Forks Roadhouse | 1 | The confluence of the home river and the Grey | G9 | A 2-storey log ruin, a sound root cellar, a 40 L copper boiler, a stable with harness hardware (sled fittings) | Milepost 12 of the Marrow Trail; the guest register's last page is 1936 |
| J.O.'s signal camp | 1 | A Grey River bar 1–3 km below the Forks, 8–11 km west of the Lookout Bench tower | G9 bar | A burned-out fire ring of 3 rings, a tin can with a note, a sock of pilot biscuits gone to dust | "Sept 16, 1979. Fire lit on the bar at dawn. Hope the lookout sees. — J.O." It completes the lookout log's line in the home valley |
| Halfway Roadhouse and hot springs | 1 | A hot spring on or near the Marrow Trail | M9 | A guest bunkhouse, a log bathhouse over a 40 °C pool, a feral rhubarb bed (3 kg in June) | Milepost 31; Doane's initials in the bathhouse beam, "W.D. 1914" |
| Marrow City | 1 | A wide creek flat in the old mining district, 15–30 km from Station Nine | N11 | 8–20 buildings: an assay office (balance scales for weighing food to 1 g), a blacksmith shop (a 60 kg anvil and tongs: the forge's work at ×1.5), a hotel, a store, a cemetery, the dredge | The camp of 1906–1918, 400 people at its peak. Doane's cabin holds his last note, 1918: "Kessler's claim still mine. Nobody left to sell it to." |
| Prospect adit | 2 | A rock face in the old mining district | L10, O12 | Shelter, a cold room at 2 °C, an ore cart with 20 kg of scrap iron | Caved 6–12 m in; nothing deeper (pillar 3) |
| Fire lookout | 3–4 | The highest bald top in its area, in sight of at least one other lookout | Cinder Knob (D2), Dome (G3), Sawtooth (N5), Grey Bluff (H12) | As the home valley's lookout, plus the Interior fire-finder (The map in play) | Staffed 1952–1984 like the Lookout Bench tower; each log has the same day's weather, so the four logs agree |
| Arvid's outer line cabins | 2–4 | Creek benches in spruce, 8–20 km apart along a line | B2, D4, L8 | A 2 × 2.5 m line cabin, a stretcher board, a cache of 1 trapper-cache upgrade each | The outer line he ran from 1946 to 1962; the notches carry on the home cabin's lynx tally |
| Signal Line relay cabin | 2 | On the Signal Line, 25–40 km apart | E8, L9 | A relay cabin, a dry-cell battery case, 200 m of salvageable wire near each | The Army line of 1904–1921; poles still stand every 60 m along the trail |
| Fish wheel ruin | 1–2 | A clear tributary's mouth or an upwelling reach | Redwater mouth (I12) | A wheel axle and baskets: the fish wheel's iron fittings, saving its 4 kg of iron | A carved date, 1938, and the wheel's owner's mark |
| Float-plane dock | 1 | The largest lake in the lake country | Long Lake (H10) | A plank dock, a fuel drum stand | Where supply drops land once the radio works, as the "lake flats" zone |

**Natural features.** Ice caves, mineral licks, beaver valleys and hot springs also generate as natural features without objects: 6–12 licks, 3–6 beaver valleys and 2–4 natural ice caves in the Interior. These are the only kind that also appear Beyond.

### Wildlife across the Interior

**Densities, not counts.** The home valley keeps its per-map counts. Each Interior district gets populations from its habitat area times a density, then ±20%, created the first time the player comes within 1 km.

| Species | Interior density | Interior total at seed default | Where it concentrates |
| --- | --- | --- | --- |
| Moose | 0.3 per km² of willow, burn and slough | About 900 | Sloughs, 10–30-year burns, the Grey's islands; winter yards at hot springs |
| Caribou (Hundredmile herd) | One herd | 800–3,000 | Barrens in summer, the corridor in September, Lichen Benches in winter |
| Dall sheep | 3–6 bands of 8–25 | About 90 | Varden Range and Sawtooth Ridge |
| Mountain goat | 1–3 bands of 5–12 | About 20 | Varden cliffs above the glaciers |
| Grizzly bear | 1 per 40 km² | About 100 | Tributaries in July–August, barrens in June, berry slopes in fall |
| Black bear | 1 per 25 km² of forest | About 110 | Foothill benches, Upper Ninemile |
| Wolf | 5–7 packs of 4–12 | About 50 | Territories of 250–700 km² (What the seed decides) |
| Wolverine | 1 per 300 km² | 10–15 | Range, barrens, the Kettle Hills camp |
| Lynx and snowshoe hare | Follow the hare cycle by district | — | Burns and flats; the cycle's peak travels (What the seed decides) |
| Beaver | 1 colony per 3 km of slow creek | About 120 colonies | Sloughs, lake outlets, dredge ponds |
| Salmon | Runs per tributary | — | Redwater (chinook), Chum Flats (fall chum), Clearwater Slough (coho), the Ninemile (a share of each) |

## What the seed decides

Every map variable is drawn once at world creation from `hash(attemptSeed, "interior", name)`, inside the range below, then checked by the fairness rules. Living variables are drawn each year from the year profile. Nothing is rerolled on reload.

### The shape of the land

| Variable | Range | How it is drawn | What it changes in play |
| --- | --- | --- | --- |
| Valley archetype | Braided flats, lake valley, canyon, oxbow marsh | Equal odds (as now) | The home valley's first month, as in Valley archetypes; also the Ninemile's character for 10 km downriver to the Forks |
| Mirror | Off or on | 50% (as now); the Interior mirrors with the home valley | Which way the player turns for the Forks; nothing else |
| Grey River course | Source glacier: east, centre or west of the range; path offset ±6 km; Forks 5–12 km downriver of the home valley | The source by equal odds, then a 1D noise path at 1/20 km along the range foot | Where the big river is, how far the Forks is, which districts lie across it (cut off in summer without a canoe) |
| Braid belt | 0.8–2.5 km wide, 4–12 channels | Width from the glacier area; channels from belt width | Wide belts: fast travel on bars, easy summer fords at low water in late August, more driftwood. Narrow belts: deeper, faster channels that always need a canoe |
| Range height | Highest peak 1,700–2,010 m | Uniform | How much of the range is icefield; how far Mount Varden is seen (40–60 km) |
| Passes | 2–4, at 900–1,300 m | Placed at the range's lowest saddles, at least 12 km apart | The ways south into Beyond; the chinook wind's doors (each pass adds +2 °C to winter warm-wind days in the 2 districts below it) |
| Glaciers | 3–6, 20–120 km² in all; termini at 400–900 m | Count uniform; area log-normal | Grey River summer height and silt; ice caves; the glacier wind's reach (2–6 km below a terminus) |
| Ice-dammed lake | 0–1 | 60% of seeds | An August outburst flood in 1 year of 4: the Grey rises 2–3 m in 12 game hours, drowns low camps and leaves new bars with driftwood and iron |
| Glacier surge | 0–1 surging glacier | 1 seed in 3 | Every 8–15 years its ice advances 300–800 m in one summer, cracks, roars, and blocks a pass or a tributary for 2 years |
| Uplands | Dome height 900–1,310 m; treeline 750–900 m | Uniform | How much of the north is open tundra; where firewood ends |
| Canyon | 1 canyon, on the upper Ninemile or on a range tributary | 70% / 30% | Where the second portage is; which fishing water is hard to reach |

### Water and ground

| Variable | Range | How it is drawn | What it changes in play |
| --- | --- | --- | --- |
| Lake count | 12–30 lakes over 2,000 m² in lake country; 2–5 over 1 km² | Poisson on the plan's basins | Fishing options, ice roads, drop zones; more lakes mean a faster winter network |
| Permafrost extent | 30–60% of lowland; 80% of north-facing slopes over 15° | A single fraction, then per-cell by aspect, cover and soil | Digging (1 block of soil), gardens fail, ice cellars work, frost heave tilts cabins (Structures and buildables, Permafrost and frost heave), summer bog slows travel |
| Thermokarst | 0.5–2 new ponds a year per flats district | Yearly, near disturbed ground | Ponds swallow trails and grow pike water; a cleared cabin site on permafrost slumps in 3–5 years |
| Clear tributaries | 3–6, each given chinook, chum or coho | Placed on spring-fed creeks; runs assigned by distance from the sea (the west edge) | Where the salmon are; at least one chinook creek within 20 km of Station Nine |
| Ninemile run share | 10–25% of each Interior run enters the home river | Uniform per species | The home valley's run sizes; overfishing at the Forks or with a wheel at Redwater cuts the home run 4 years later |
| Hot springs | 1–2 in the Interior | Placed at slope breaks on faults (plan noise) | A warm stop, an early garden, a moose yard |
| Soil | Silt and loess on the uplands, gravel on bars, peat in flats, till on moraines | By landform and age | What can be dug, where clay is (cut banks in silt), where gardens grow best (south-facing loess) |

### Fire, forest and people's past

| Variable | Range | How it is drawn | What it changes in play |
| --- | --- | --- | --- |
| Burns | 2–5 burns of 20–400 km², ages from {2–5, 6–15, 16–40, 41–80} years | Count uniform, one age class each, shapes from wind-driven spread on the plan | Hares, lynx, moose, morels, berries and snags by stand age (Land and seasons in play, Stand age); where firewood is easy |
| Lightning fires | 0–4 a summer, in years with 5+ dry days and storms | The year profile, placed on black spruce and old burns | New burns grow each summer; smoke closes views; a fire can reach a trapline or outpost (the forest-fire rules) |
| Mining district | One, 8–25 km from Station Nine, in the foothills or uplands | Best-fit to creek flats with gravel | Where the anvil and tools are; how long the first iron-tier trip is |
| Ghost camp size | 8–20 buildings | Uniform | How much scrap, glass and tin there is; 1 stove per 4 buildings |
| Marrow Trail | One route, Landing to the ghost camp, through the home valley's hot spring cell | Least-cost path on the plan, then mileposts every 1.6 km | A cleared line that is faster in winter (×1.15 on snow); roadhouse stops every 15–20 km |
| Signal Line | Follows the trail for 60–80% of its length | Along the trail, cut straight across bends | Copper wire: 200 m by each relay cabin and 30 m a km along the poles, 5 kg of wire in all; insulators for a lamp chimney |
| Lookouts | 3–4 in the Interior | Highest bald tops, each in sight of another | How much of the Interior can be named from above; the survey map's location |
| Arvid's outer line | 2–4 line cabins | On a least-cost line from his home cabin, 8–20 km apart | Free shelter on long trips; trapper-cache upgrades |

### Life and the living year

| Variable | Range | How it is drawn | What it changes in play |
| --- | --- | --- | --- |
| Caribou corridor | One of 3 Interior paths; herd 800–3,000 | Path at creation; herd size uniform | Which districts see the herd in September; the year profile's route then picks one of the 3 home-valley crossings |
| Hare cycle wave | Base phase 0–9 (as now) plus a lag of 0–2 years across the Interior, in a seeded direction | Phase per district = base + lag × distance along the direction ÷ 65 km | A crash at home can be a boom 30 km away: lynx, foxes and wolves shift, and long traplines pay where short ones fail |
| Wolf territories | 5–7 Interior packs of 250–700 km², plus the 3 home packs | Voronoi cells on prey habitat, dens on sandy south banks | Where a tamed wolf is challenged; where caribou kills are found; how many lone yearlings disperse toward the home valley |
| Bear dens | 1 per bear | High slopes as in Wildlife and fish | Where not to camp in October and April |
| Iron | 60–120 clusters in the Interior, mostly in the range and Kettle Hills; 6–12 bog iron beds in the flats | As in Biomes and resources, by district type | The home valley's 12–18 clusters never regrow, so a homestead's later iron comes from the Interior and the forge pulls the player out |
| Grey River breakup | April 25 to May 15, after the Ninemile's | Year profile, from the spring warmth | Ice jams at the Forks flood the Sloughs on 1 year in 3 |
| Glacier melt | July peak ×0.7 to ×1.4 | Year profile, from summer warmth | Summer fords open or close; the Grey's silt; outburst odds |
| Salmon runs | Per tributary, ×0.3 to ×2.0 | Year profile (as now), now per creek | Which fish camp pays this year |
| Berry index | 0.5–2.0, plus ±0.3 per named area | Year profile (as now), plus a local draw | Where bears are; which slopes to camp near |

### Fairness rules

The seed rearranges the Interior, but every world must offer the same kinds of trip at the same kinds of distance. A failed check retries the Interior plan with sub-seed `hash(attemptSeed, "interior", j)` for j = 1–10; the home valley is never rerolled for the Interior.

1. **The home valley comes first.** All 12 start-site checks still run inside the home valley only, and no Interior set piece lies within 6 km of Station Nine.
2. **Within 12 km of Station Nine:** the Forks, a lake of 1 km² or more, and a point on the caribou corridor.
3. **Within 25 km:** a lookout, the old mining district, a hot spring (the home valley's counts), the Grey River's ice road, and a clear tributary with a chinook run.
4. **Within 35 km:** a glacier terminus and a pass.
5. **Reachable.** Every district is reachable on foot from Station Nine in winter. In summer, districts across the Grey are reachable by canoe, or on foot at a summer ford in late August in at least 2 places.
6. **Landmarks.** Every district has a landmark that passes a 4 km version of the 2 km test, and Mount Varden or the range crest is visible from at least 90% of the Interior's high points.
7. **Water and wood.** No point in the Interior is more than 3 km from water. Every district below treeline has firewood within 1 km; above it, willow brush within 3 km.
8. **Spacing.** Set pieces keep 1 km apart; roadhouses 15–20 km; lookouts 10 km.
9. **Drawable.** The plan's river network never rises downstream, every lake has one outlet, and every river reaches the Grey or the edge.

## Map layers

The map is a stack of layers, each written by one generator stage and read by named systems. In the home valley they sit at 2 m cells; in the Interior and Beyond at 4 m cells, with a 128 m plan layer under the whole Interior.

| Layer | Holds | Written by | Feeds |
| --- | --- | --- | --- |
| Elevation | Surface height; the 128 m plan height for the Interior | Landform, erosion | Air temperature (6.5 °C per 1,000 m in summer), the inversion in winter, treeline, travel cost, view distance, map reveal from high points |
| Hardness | Rock hardness 0–1 | Landform | Rock faces, cliffs, where iron and shelter faces are, rockfall |
| Hydrology | Water type, surface, flow direction, speed, catchment, flood lines | Water and rock | Drinking water, current, fishing spots, fords, floods, travel by canoe, where animals drink (Water, calls and encounters) |
| Glacier | Ice extent and thickness, crevasse fields, terminus | Interior plan | Glacier wind, ice caves, the Grey's summer level, surges |
| Ice | Thickness per water body, thin flags | Weather, daily | Winter travel, ice fishing, thin-ice danger, the Grey's overflow |
| Permafrost | Frozen or not; active-layer depth 0.4–1.5 m | Cover, by aspect and soil | Digging, building settlement, gardens, ice cellars, thermokarst, drunken trees |
| Soil | Gravel, sand, silt, loess, peat, till, clay | Cover | What can be dug, clay sources, garden growth, frost heave |
| Biome and cover | One of the blueprint's biomes or district cover classes | Cover | Trees, plants, nodes, tint and ambience |
| Stand age | Years since fire or clearing per 32 m cell | Cover, then yearly | Hares, lynx, moose, berries, morels, snags, regrowth |
| Fire history | Burn polygons with dates | Interior plan, then fire events | Stand age, the messenger's burn shading, lightning targets |
| Snow class | Open, under spruce, south slope, lee drift | Cover | Snow depth per day, crust, tracks, travel |
| Wind exposure | Shelter 0–1, glacier-wind zones, pass jets | Interior plan | Feels-like temperature, scent cones, blizzards' bite, where a camp is safe |
| Cold-air pooling | Night and inversion offset per cell, −8 to +8 °C | Interior plan | Where to build, frost dates, the −45 °C flats |
| Wildlife ranges | Home ranges, territories, the corridor, dens, lodges | Life | Encounters, tracks, traps, hunting |
| Fishing spots | Spot list with quality and type | Water and rock | Fishing, bears at riffles, eagles |
| Travel cost | Derived: ground factor × slope × snow × water | Computed on load, not stored | Animal trails, Travel to, the messenger's route estimate |
| Places | Set pieces, natural features, landmarks, trail and line | Places | Discovery, story, loot, navigation |
| Player map | Seen, sighted and charted grades; names; pins | The player, during play | The messenger map (The map in play) |

**One rule ties them.** A system reads layers only through `worldgen.Layers` at a block position. The same call works in all three rings, so no gameplay system knows which ring it is in.

## The map in play

### The messenger map

**Fog of war, in three grades.** The messenger map starts blank outside what the player has seen. Each 8 m cell has a grade, and the map draws each grade differently.

| Grade | How it is gained | What the map shows |
| --- | --- | --- |
| Seen | Within 60 m on foot; 1,000 m from any point 100 m above its surroundings (as now); 1,500 m from a lookout cab | Full detail: terrain shading, water, cover, trails, features, the date it was seen |
| Sighted | From any point 300 m above its surroundings, out to 6 km, at 128 m resolution; from a canoe, both banks to 200 m | Coarse shape: ridges, water, forest or open, as a grey wash |
| Charted | Read from a paper map | Lines and names drawn in the paper map's own style, with its date |

**The map is dated.** Each seen cell keeps the game day it was seen. A burn, a moved channel or a drained lake stays as it was until the player sees it again, and old cells fade a shade per year. "Seen 412 days ago" shows on inspect.

**Region and district cards.** Entering a home region shows its name and one line, as now. Entering an Interior district shows its area's name and one line for the area's first district only: "Loon Lakes: lakes in birch and spruce, joined by gravel eskers." Beyond, the line is "Uncharted country, 41 km from Station Nine."

**Mapped.** A home region counts as mapped at 40% seen, as now. An Interior district counts at 25% seen, and a named area when every district in it counts. The ambitions "Map the Interior" (all 26 named areas) and "Map 100 districts" join the Beyond ambitions in Open world in depth.

**Zoom.** The map has four zooms: 250 m, 2 km, 8 km and 65 km across. At 65 km, unseen districts show only charted lines, and the home valley is a small square in the middle.

### Paper maps

**Paper maps fill the map with lines, not truth.** Each paper map is an item that charts a set of features when read, in its own year. Where the land has changed since, the chart is wrong, and the messenger shows the difference only once the player sees the place.

| Map | Found at | Charts | Where it is wrong | What it adds |
| --- | --- | --- | --- | --- |
| J.O.'s sectional chart, 1979 | The wreck's map pocket (home valley) | The whole Interior and 10 km beyond: rivers, lakes over 1 km², peaks with heights, glaciers, the Landing airstrip, Marrow City, the Marrow Trail | Glaciers drawn 1–2 km longer; the Grey's channels as they were; 1979 burns only | J.O.'s pencil: a line from the Landing strip to the valley, "fuel 1978" at the Landing, radio frequencies in the margin, and a magnetic declination of 27° E |
| The lookout survey map, 1952 | Dome Lookout | Contours every 30 m for 12 km round the lookout, all summits named | Lakes drained or grown since | Elevation for planning climbs; the Bald Domes' names |
| The Marrow Trail map | Forks Roadhouse | The trail with every milepost, both roadhouses, the Signal Line and the relay cabins | The bridges are gone | Trail distances for Travel to planning once a route is linked |
| Kessler's claim map | Marrow City assay office | The Kettle Hills creeks, ditches, adits and the ice tunnel at Discovery Creek | Ditches collapsed | Every prospect pit and shaft as a hazard mark |
| Arvid's trapline board | His home cabin (as now) | 6–10 marten and lynx sites | Burns since | As now; the outer line cabins are scratched at its edge |

**Declination.** The compass in the kit points to magnetic north, which lies 15° east of true north here; the messenger map is drawn to true north. While the messenger works it corrects for the player. With a dead battery, walking a compass bearing uncorrected drifts 260 m a kilometre, and J.O.'s chart says 27°, which was right in 1979 and is wrong now.

### Making the map your own

- **Names.** The player names any feature, as in Creative concepts, Naming the land; Interior names count against the same 200.
- **Pins and lines.** Up to 60 pins, of which 20 can be waypoints on the HUD (Minecraft engine, the map attachment), and 20 drawn lines (a route, a boundary, a trapline). A drawn route shows its length and a time estimate from the travel-cost layer.
- **The hand map.** At a workbench, the player copies part of the messenger map to `yearofice:hand_map`: 1 sheet of birch bark and 20 game minutes per district, or per home region. The hand map works with no battery and shows the copied area as charted.
- **Cairn notes.** A hand map left in a cairn can be found by a player on the same seed (Creative concepts, Cairn mail).

### Finding the way

- **The southern wall.** Mount Varden and the range crest are on the skyline from 90% of high points. Facing the range is facing south, with no compass at all.
- **The sun.** At 64°N the sun at midday stands due south: 49° high in June and 3° in December. In June it rises in the north-east and sets in the north-west.
- **Lookouts.** The Interior fire-finder at each lookout marks a bearing line to every other lookout and every landmark in sight, and names every named area whose landmark it can see. Four lookouts chart the names of most of the Interior.
- **Rivers.** Every water in the Interior runs to the Grey and the Grey runs west. Follow water downstream and you reach the Grey; follow the Grey up and you reach the Forks.
- **Lost.** In a whiteout, fog or flat light (Land and seasons in play), seen cells stop being added beyond 20 m and the messenger's breadcrumb is the way home. With a dead battery, there is only the hand map, the compass and the land.

## Travel at Interior scale

**Distances from Station Nine.** Straight-line distance at seed default, then game hours on foot at about 1.5 game hours a straight km for winding summer ground. Sled times are for a wolf team on river ice or a packed trail, about 0.7 game hours a straight km (0.6 h per km of route, Open world in depth › Traversal), and a team pulls at most 8 hours a day. Canoe times use 0.6 h per km of river downstream and about 1.3 km of river per straight km.

| Place | Distance | On foot, summer | Best winter way | Best summer way |
| --- | --- | --- | --- | --- |
| The Forks | 7 km | About 11 game h | Sled down the home river, about 5 h | Canoe downriver, about 5.5 h |
| Loon Lakes (Long Lake) | 5 km | About 8 h | Snowshoes across lake ice, about 8 h | On foot along the eskers |
| Black Gate Canyon | 12 km | About 18 h | Sled up the home river, about 8 h | On foot; the canoe stops at Long Falls |
| Redwater Creek mouth | 15 km | About 23 h | Sled on the Grey's ice road | Canoe down to the Forks, then up the Grey's side channels |
| Grey Bluff Lookout | 15 km | About 23 h | Sled via the Forks and the Grey | Across the Grey by canoe, then climb |
| Halfway Hot Springs | 21 km | About 31 h | The Marrow Trail by sled, about 14 h | On foot on the trail, 2 days |
| Grey River Landing | 23 km | About 34 h | Sled on the Grey, about 16 h | Canoe downriver, about 17 h over 2 days |
| Dome Lookout | 23 km | About 35 h | Snowshoes over wind-packed tundra | On foot, 2–3 days |
| Marrow City | 26 km | About 39 h | The Marrow Trail by sled, about 17 h | On foot, 3 days |
| Hundredmile Barrens (centre) | 29 km | About 44 h | Sled with a team, about 20 h | On foot, 3–4 days |
| Varden Glacier terminus | 32 km | About 48 h | Sled up the Grey's ice, about 22 h | On foot, 4 days, two crossings |
| Mount Varden | 33 km | 5 days with the climb | Only on spring crust | July–August |
| The Interior's edge | 31–34 km | 4–5 days | 3 days by team | A week, or by canoe downriver to the west edge in 2 days |

**What the distances do.** The home valley can be crossed in a day; the Interior cannot. A 25 km trip is 3 days on foot with 2 nights out, about 10,000 kcal and 5 kg of food carried. That is why outposts, the canoe and the sled exist (Open world in depth, Outposts and Traversal), and the Interior is where they pay off.

**Rivers as roads.** The Grey runs at 1.5–2.5 m/s in summer, so a canoe goes down it fast and never comes back up it; the way home is on foot, by the side sloughs, or in winter on the ice. The Ninemile can be paddled up at about 1.1 h a km against its 1.2 m/s current, and at 0.8 h a km on its flat backwaters (Open world in depth › Traversal). A canoe cache at the Forks, and another at the Landing, turn the river into a loop.

**The frontier.** Beyond the Interior edge the messenger says "Leaving the charted Interior. Beyond here the map is blank." There is no wall and no damage. The supply drops and Pete's fly-overs stay inside the Interior; the radio reaches 40 km from Station Nine's mast, and an outpost mast adds 40 km more. The Endless SOS still flies the player home to Station Nine from anywhere, so the risk Beyond is losing outposts and gear left far away, not the run.

## World size in Minecraft

### Coordinates and height

**Centre and rings.** The three rings share centre 0, 0. `worldgen.RingRouter.ring(x, z)` returns 1 inside ±4,096, 2 inside ±32,768 and 3 beyond. Home regions keep `floor((x + 4096) / 2048)`; districts use `floor((x + 32768) / 4096)`; provinces use `Math.floorDiv(x + 32768, 65536)`, so province 0, 0 is the Interior.

**Height.** The dimension type keeps `min_y` 64 and raises `height` to 1,968, so the top build height is y 2,031, Minecraft's maximum. Block y is still elevation in metres: the Grey's mouth sits near y 100 and Mount Varden at y 2,010. Sections of solid ground and empty air are single-valued, so the extra 496 blocks cost almost nothing (Minecraft engine, Risks and limits).

**Far coordinates.** Generation works in 64-bit province indices and local integer coordinates inside a province, so the same district builds the same way at x = 1,000 or x = 20,000,000. Minecraft renders relative to the camera, so precision is not a problem within the vanilla limit.

### Generating the three rings

| Ring | At world creation | On approach | Cell size | Budget |
| --- | --- | --- | --- | --- |
| 1 Home valley | The 32 m coarse field, the core B3's layers, the start site | Each region within 600 blocks: stages 2–7 | 2 m | As now: core under 8 s, a region under 3 s |
| 2 Interior | The Interior plan: 512 × 512 plan cells at 128 m (height, the river network, glaciers, burns, district types, named areas, set-piece and route positions, territories) | Each district within 800 blocks: stages 2–7 at 4 m, blended to the plan over 128 m at its edges | 4 m | Plan under 2 s, run in parallel with the core so creation stays under 8 s; a district under 3 s |
| 3 Beyond | Nothing | A province plan when the player comes within 16 km of it; then districts as in ring 2 | 4 m | A province plan under 2 s; a district under 3 s |

**The home valley inside the plan.** The plan cells under the home valley are filled from its own 32 m coarse field, so the home river's inflow at D3 and outflow at A3 match the plan's river network, which is fixed first. The home river's virtual catchment (64–200 km²) becomes the upper Ninemile's real catchment.

**District layers.** A district runs the same stages as a home region at 4 m cells (1,024 × 1,024, the same cost as a 2 m region) with 30,000 erosion droplets. Block-level detail adds 1 m noise of ±0.5 m on soil cover only. Rivers cross district edges within 16 m of their plan crossing, and heights match within 0.1 m, as in Pipeline at a glance.

**Province plans Beyond.** Each province draws one of 7 province kinds (range and ice, domes and barrens, lowland flats and lakes, big river, burn country, mixed, coast) from `hash(seed, "province", px, pz)`. The 7th kind, `coast`, is drawn only for provinces two or more province rows south of the Interior (z ≥ 98,304, about 98 km from Station Nine, past the Varden Range): a shoreline, spruce and hemlock forest, and a wrack line of washed-up kelp blocks; it is the only home of the Sitka black-tailed deer (Land animals in depth › Deer; RECONCILE 154). Its edges are fixed by edge contracts so neighbours agree without reading each other:

1. A continental slope field, a very low-frequency noise of 1/400 km, is sampled identically by both sides of every edge.
2. Each edge draws 0–3 river crossings from `hash(seed, "edge", edge id)`, flowing from the higher to the lower side of the slope field.
3. Each province solves its own drainage with those inflows and outflows fixed, so rivers join across edges with no seam.
4. Big rivers (catchment over 2,000 km²) cross at most one edge in four, so Beyond has long rivers, not a maze.

**Determinism.** Every rule in Minecraft engine, The nine stages in Minecraft, holds in all rings: integer hashing, banned calls, disjoint parallel writes. A district's layers depend only on the seed, the version, its plan and its own area, so they come out the same in any visiting order.

**Populations far away.** Ring 2 and 3 populations are created lazily when the player first comes within 1 km of a district, at the habitat density, and then run on the Abstract tier like the home regions. An unvisited district has no stored state at all.

### Where structures may spawn

| Kind | Ring 1 | Ring 2 | Ring 3 |
| --- | --- | --- | --- |
| Story set pieces (`yearofice:setpiece`) | Yes | Yes | Never |
| Natural features (`yearofice:natural_feature`): licks, beaver valleys, cold-trap caves, hot spring pools | Yes | Yes | Yes, at Interior densities |
| Landmarks (tors, cascades, snowfields, burn ridges) | Yes | Yes, 4 km test | Yes, one a district |
| Vanilla structures | Never | Never | Never |
| Player building | Anywhere | Anywhere | Anywhere |

Every set-piece footprint lies inside one district and keeps 64 blocks from its edge, so no template ever needs a neighbour's layers.

### Performance and storage

**Layer cache.** Region and district layers share one LRU of 10 entries in memory (6 home regions and 4 districts at most) and a disk cache at `<world>/yearofice/layers/`. The Interior plan is 6 MB in memory and is always loaded; a province plan is the same size and at most 2 are loaded.

**Fast travel and generation.** A wolf team runs at about 6 m/s, which needs about 9.4 new chunks a second at a 12-chunk view: 0.375 chunks of forward travel a second across a 25-chunk front (RECONCILE 96). District layers are built 800 blocks ahead in the travel direction, so a 30 km sled run never waits on a district.

**The horizon.** At login the client receives the home valley's 32 m field and the Interior plan's 128 m height, about 650 KB compressed. `client.horizon.HorizonRenderer` draws them out to 24 km, with landmark silhouettes; the Varden Range is drawn as a silhouette from anywhere in the Interior. Beyond, the nearest province plans are streamed the same way.

**Save size.** The home valley keeps its 2 GB ceiling. Interior and Beyond chunks exist only where the player has been, and unedited ones are pruned: a chunk outside the home valley with no player edit and unvisited for 60 game days is deleted by `worldgen.ChunkPruner` and rebuilt identically when next loaded, because generation is deterministic and the season stamp catches it up. Interior and Beyond chunk files stay under 1.5 GB.

**The player map in storage.** Revealed cells are stored per 512-block tile, only for tiles with anything revealed: 2 bits per 8 m cell (seen, sighted), 1 KB a tile raw. A 5-year game that has seen 600 km² holds about 2.5 MB raw, under 400 KB run-length compressed, inside the 5 MB mod-data budget in the `yearofice:map` attachment's tile store.

### The world border

**Left at the vanilla default.** The mod never sets the world border; it stays at Minecraft's default of 59,999,968 blocks wide. The old 8,192-block border is gone, and so is the line "The valley ends here. Turn back."

**Development builds only.** Until the Interior ships (M10), test worlds set a border at the home valley's edge from the world-creation flag `yearofice:dev_border`, which play builds hide. A GameTest fails the play build if any world created without the flag has a border smaller than the default.

## Data and build

### Data files

| Path | Holds |
| --- | --- |
| `data/yearofice/region/*.json` | The 16 home regions, unchanged (Open world in depth, Region entry) |
| `data/yearofice/interior/plan.json` | Every range in What the seed decides: river, glaciers, passes, lakes, permafrost, burns, routes |
| `data/yearofice/district_type/*.json` | The 17 district types: cover rules, densities, hazards, travel factors, landmark kinds |
| `data/yearofice/district/*.json` | The 26 named areas: type, default districts, fit rules for placement, set pieces, first-entry line |
| `data/yearofice/province_kind/*.json` | The 7 Beyond province kinds, coast included, and their type mixes |
| `data/yearofice/setpiece/*.json` | Interior set pieces with the `ring` field (1, 2) and fit rules |
| `data/yearofice/natural_feature/*.json` | Licks, beaver valleys, cold-trap caves, hot spring pools |
| `data/yearofice/paper_map/*.json` | The 5 paper maps: what each charts, its year, its errors |
| `data/yearofice/structure/setpiece/<id>/<variant>.nbt` | Templates, as now |
| `data/yearofice/worldgen/structure/natural_feature.json` | The natural feature structure |

**A district type.**

```json
{
  "elevation_m": [100, 200],
  "cover": { "black_spruce": 0.55, "muskeg": 0.3, "thermokarst_pond": 0.1, "birch": 0.05 },
  "permafrost": { "from_plan": true, "min_fraction": 0.6 },
  "soil": ["peat", "silt"],
  "travel": { "summer": 0.6, "winter": 1.1, "wet_boots": true },
  "density_per_km2": { "yearofice:snowshoe_hare": 8, "yearofice:lynx": 0.08, "yearofice:moose": 0.2 },
  "nodes": { "bog_iron_bed": [0, 2], "cranberry_patch": [20, 40] },
  "hazards": ["bog_hole", "drunken_trees", "frost_heave"],
  "landmark_kinds": ["sand_cliff", "lone_tor"],
  "cold_air_pool_c": -6
}
```

**A named area.**

```json
{
  "name_key": "district.yearofice.loon_lakes",
  "type": "yearofice:lake_country",
  "default_districts": ["G10", "H10", "I10", "J10", "G11", "H11", "I11", "J11"],
  "fit": { "between": ["home_valley_south", "grey_river"], "max_from_station_km": 12, "min_lakes": 12 },
  "setpieces": ["yearofice:float_plane_dock"],
  "landmark": { "kind": "long_lake", "visible_km": 4 },
  "first_entry_key": "district.yearofice.loon_lakes.first_entry"
}
```

**The plan's ranges.**

```json
{
  "grey_river": { "source": ["east", "centre", "west"], "path_offset_km": [-6, 6], "forks_km": [5, 12], "belt_km": [0.8, 2.5], "channels": [4, 12] },
  "range": { "peak_m": [1700, 2010], "passes": [2, 4], "pass_m": [900, 1300], "glaciers": [3, 6], "glacier_km2": [20, 120] },
  "ice_dammed_lake": { "chance": 0.6, "outburst_per_year": 0.25 },
  "lakes": { "count": [12, 30], "over_km2": [2, 5] },
  "permafrost_lowland": [0.3, 0.6],
  "burns": { "count": [2, 5], "area_km2": [20, 400], "age_classes": [[2, 5], [6, 15], [16, 40], [41, 80]] },
  "tributaries": { "count": [3, 6], "ninemile_share": [0.1, 0.25] },
  "caribou": { "paths": 3, "herd": [800, 3000] },
  "wolves": { "packs": [5, 7], "territory_km2": [250, 700] },
  "hare_wave": { "lag_years": [0, 2] },
  "guarantees": { "forks_km": 12, "lookout_km": 25, "glacier_km": 35 }
}
```

### Classes

| Class | Does |
| --- | --- |
| `worldgen.RingRouter` | Which ring, region, district and province a block lies in |
| `worldgen.interior.InteriorPlan` | Builds the 128 m plan at world creation and runs the fairness checks |
| `worldgen.interior.MacroRivers` | The Grey, the Ninemile and the tributaries on the plan; glaciers and the outburst lake |
| `worldgen.interior.DistrictRoles` | Types and named areas onto districts by fit rules |
| `worldgen.interior.DistrictLayers` | Stages 2–7 for one district at 4 m cells, blended to the plan |
| `worldgen.beyond.ProvincePlan`, `EdgeContracts` | Province kinds, the continental slope field and river crossings |
| `worldgen.places.InteriorPlaces` | Set pieces, natural features, landmarks, the Marrow Trail and the Signal Line |
| `worldgen.ChunkPruner` | Deletes unedited chunks outside the home valley |
| `ecosystem.DistrictPopulations` | Lazy populations by density; the hare wave; the herd's corridor |
| `story.PaperMaps` | Reading a paper map writes charted cells and names |
| `client.map.MapTiles` | The tile store and the four zooms |
| `client.horizon.InteriorHorizon` | The 24 km horizon from the plan |

### Milestones

- **M0–M1, ring 1.** Unchanged, except that M1 now builds the Interior plan's river network and height first, because the home river's inflow, outflow and the horizon depend on it. M1 adds `InteriorPlan`, `MacroRivers` and the plan check to the debug map.
- **M2–M9.** Unchanged. Test worlds use `yearofice:dev_border` at the home valley edge, and the horizon shows the Interior plan from M2.
- **M10 — The Interior (new).** `DistrictRoles`, `DistrictLayers`, `InteriorPlaces`, all 17 district types and 26 named areas, the Interior set pieces and paper maps, lazy populations, the map's grades and zooms, the pruner. The dev border is removed. Human test: a tester plans and makes a 25 km trip to Marrow City with only the map, and names the 5 places they would build an outpost.
- **M11 — Beyond (new).** `ProvincePlan`, `EdgeContracts`, natural features Beyond, province horizon streaming. Human test: a tester travels 20 km past the Interior edge by sled and cannot tell where the Interior ended except by the missing set pieces.

### GameTests and checks

1. **Plan determinism (JUnit).** The same seed gives the same Interior plan hash on Linux, Windows and macOS; over 200 seeds every plan passes all 9 fairness rules within 10 sub-seeds, and each glacier source appears 33% ±6%.
2. **Guarantees (JUnit).** Over 200 seeds the Forks is within 12 km of Station Nine, a lookout within 25 km and a glacier terminus within 35 km in 100% of worlds.
3. **Seams (GameTest `worldgen/district_seams`).** Heights match within 0.1 m and every river crosses within 16 m of its plan crossing across 50 district edges, 10 home-to-district edges and 10 province edges.
4. **No story Beyond (JUnit).** Over 100 provinces sampled out to ±1,000,000 blocks, 0 set-piece starts and 0 notes; natural features within ±30% of Interior densities.
5. **Far build (GameTest `worldgen/far_district`).** A district at x = 20,000,000 builds in under 3 s on the server with the same layer hash as JUnit computes, and a 30 km sled run at 6 m/s keeps every tick under 50 ms.
6. **Prune and rebuild (GameTest `worldgen/prune_identical`).** A pruned unedited chunk reloads with the same block hash; an edited one is never pruned.
7. **No small border (GameTest `world/default_border`).** A world created without `yearofice:dev_border` reports a border of 59,999,968 blocks.
8. **Map store (JUnit sim).** A 5-year run that sees 600 km² keeps the compressed map tile store under 500 KB, and reveal grades survive a server restart.
