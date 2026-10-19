# Plants and fungi

Every plant, tree, shrub, moss, lichen and fungus in the valley is a block that changes through the year and answers to eight harvest verbs. This tab gives them one interaction model, adds 87 species rows that fit interior Alaska at 64°N, 83 of them new to the blueprint, and turns the forest floor into seasonal work: tapping, gum, bark, roots, dyes, fibre, medicine and a herbarium. It reuses every plant rule already written and changes none of them.

## How this tab fits

**What already exists stays as written.** The species below are owned by other sections; this tab only gives them the shared block model and harvest verbs.

| Where | Plants already specified |
| --- | --- |
| Blueprint › Foraging and plant knowledge | Birch sap, spruce tips, fireweed, cattail, wild chives, morel, cloudberry, blueberry, lingonberry (low-bush cranberry), wild rose, highbush cranberry, chaga, Labrador tea, nettle, yarrow, usnea (old man's beard), willow bark, wild celery, cow parsnip; the five dangerous plants: water hemlock, death camas, false morel, baneberry, fly agaric |
| Blueprint › Crop farming | Potato, barley, wheat, turnip, cabbage |
| Blueprint › Biomes and resources; Gathering materials | White spruce, black spruce, birch, aspen, willow, alder (as tree builders), peat, moss sheets, birch bark, spruce pitch and roots, plant fibre |
| Land and seasons in play › New plants and fungi | Birch polypore, birch bolete, orange-cap bolete, shaggy mane, crowberry, bog blueberry, sourdock, wild potato, horsetail, devil's club, plantain; wild sweet pea, bog rosemary, poison pax; reindeer lichen (famine food and the lichen index) |
| Ideas from viewers › Berry country, Meadow teas, Needle tea | Wild raspberry, nagoonberry, northern red currant, bearberry, bog cranberry, pineapple weed, dandelion, rose petals, subalpine fir, the berry rake |
| Every concept from the video › Berry patches, Lily pads | berry bushes, one block per species (no item form, no planting), yellow pond-lily `yearofice:lily_pad` and its seeds `yearofice:lily_seeds` |
| Water, calls and encounters | Pond weed, sedge mat, sedge hay, willow staking |
| Ideas from viewers › Foxfire | Glowing rotten birch and aspen |

**Rules reused, never restated.** Ripeness stages, patches that remember (±20% seed value, stripped above 70% halves next year), the inspect card (Look, Smell, Cut), the three ways to identify a plant (this tab adds the herbarium as a fourth), the taste test, the pocket guide's 12 plants, peak week, fermented fruit, the berry rake, stand age and succession, mushroom flushes, berry swings, crop germination and the two-block dig rule.

**Two conventions this tab settles.**

- **Data path.** Plant rows live at `data/yearofice/plant/<id>.json`, the singular folder from Minecraft engine › Data kinds. The Land tab uses it too; there is no `plants/` folder.
- **Berry blocks.** There is no `yearofice:berry_bush` block. Each berry is its own block named `<species>_bush` (`yearofice:soapberry_bush`, `yearofice:nagoonberry_bush`), all sharing the class `com.yearofice.forage.BerryBushBlock`, all tagged `yearofice:berry_bushes`, none with a `BlockItem`. The fruit item keeps the bare name (`yearofice:nagoonberry`).

**Units and names.** Vitamin C figures in this tab are C-days per unit (Blueprint › Cooking: one C-day covers one day of the 30-day scurvy limit); a species' own figure overrides Cooking's generic per-gram rate. "The messenger" is the HUD information screen, the Trail Messenger in the world.

## The plant interaction model

### Plants as blocks

**One clump, one block.** A herb, grass or low shrub block is a clump about 1 m across: one patch of fireweed is 20–60 clump blocks. Trees stay multi-block, built by the tree builders, with the tree id in the chunk attachment.

| Plant form | Minecraft form | Example |
| --- | --- | --- |
| Herb, grass, sedge, fern | Cross-model block on soil, 1 block tall; 2-block `DoublePlantBlock` above 1.2 m | `yearofice:monkshood` (2 tall) |
| Low shrub and berry | Bush block, 1 block, partial collision (slows walking ×0.85) | `yearofice:soapberry_bush` |
| Tall shrub (willow, alder, dogwood) | 2–4 block stems of `yearofice:shrub_stem` with leaf blocks; one shrub id per stool | `yearofice:feltleaf_willow` |
| Tree | Log and leaf blocks from a tree builder; tree id and vigor in the chunk attachment | `yearofice:tamarack_log` |
| Moss and lichen | Surface layer block, like a carpet; lichen on rock and bark is a face decal block | `yearofice:sphagnum`, `yearofice:rock_tripe` |
| Bracket fungus | Face block on a log side | `yearofice:tinder_conk` |
| Mushroom | Small cross block, placed by the flush | `yearofice:king_bolete` |
| Aquatic | Waterlogged block in 0.1–1.5 m of water, or a floating surface block | `yearofice:buckbean` |

**Block states.** Every plant block carries two properties, set by the server and never by random ticks (`randomTickSpeed` stays 0).

- `phase`: `dormant`, `buried`, `bud`, `leaf`, `flower`, `fruit`, `seed`, `senescent`. Eight values.
- `taken`: 0–3. 0 is untouched, 3 is stripped bare. It drives the texture: a picked bush shows bare stems.

Mushrooms add `age` 0–3 (button, prime, old, rotting). Bracket fungi add `size` 0–3. Berry bushes add `ripe` 0–2 (green, pink, ripe), read only in the `fruit` phase; their 0–8 day regrow count lives in the `patches` ledger, not a block state (Minecraft development › World and worldgen).

### Phases through the year

**Phases follow degree-days, not dates.** Each species lists a degree-day sum above 5 °C for each phase, counted from its cell's melt-out. A cold summer makes everything late, as the berry swings already require.

| Phase | Starts when | What the player sees | Typical harvest |
| --- | --- | --- | --- |
| Dormant | Leaf drop or first hard freeze of −5 °C | Bare twigs, brown stalks | Bark, twigs, roots before freeze |
| Buried | Snow layers reach the plant's height | Nothing, or a twig tip; the inspect card reads "under snow" | Clear snow 1 game minute, as for lingonberry |
| Bud | Melt-out plus 0–5 days | Swollen buds, red stems, shoots 5 cm | Shoots, buds, sap |
| Leaf | Species sum, usually 30–80 degree-days | Full leaves | Greens, leaves for tea |
| Flower | Species sum | Flowers; bees on the bee index | Flowers, specimens for the press |
| Fruit | Species sum | Ripe fruit, peak week in the middle | Berries, seeds, mushrooms |
| Seed | Fruit plus 10–20 days | Cotton, pods, dry heads | Seed for propagation, grain |
| Senescent | First fall frost, or day length under 12 h | Yellow, red or brown leaves | Roots at full starch, late berries |

**The season pass.** `PlantPhasePass` runs on `ClockEvent.Day` for loaded chunks and in closed form on chunk load. It reads the chunk's micro-climate offset, so frost hollows lag 5 days, as Land and seasons in play › Micro-climates says.

**Snow-buried plants.** A buried plant cannot be seen from 10 m and shows no name under the crosshair. Animals that dig (caribou, moose in shallow snow, grouse, voles) reach it; the player clears it by hand.

**On screen and sound.** Phase changes re-mesh with the season stages. Seed heads release a client particle (`yearofice:plant_fluff`) in wind over 4 m/s: fireweed and cottongrass cotton, poplar fluff in June.

### Harvest verbs

**Eight verbs cover every plant.** Each one changes the block, the patch ledger and the plant's future. The verb is chosen by the tool in hand and the plant's phase; the crosshair shows it ("Hold right mouse: dig wild potato"); right mouse held is the work key, as in Minecraft development › Hold-to-work and the use key.

| Verb | Tool | Takes | What it does to the plant | Patch effect | Comes back |
| --- | --- | --- | --- | --- | --- |
| Pick | Hand | Fruit, flowers, leaves, shoots, one unit at a time | `taken` +1 per 33% of the clump | Counts 1.0 toward stripped | Refruits in 5–8 days in season (existing); leaves in 14 days, once a season |
| Strip | Hand, berry rake | Everything of one part on a clump at once, ×0.4 time | `taken` 3 | Counts 1.5 toward stripped (the rake rule) | Next year only |
| Peel | Knife | Outer bark sheets, inner bark strips | Scar: tree vigor −0.1; a full ring girdles it (dies in 2 years) | — | Bark once per tree per year (existing) |
| Cut | Knife, hatchet | Stems, leaves, hay, shoots; the root stays | Clump to `leaf` with `taken` 2 | Counts 0.5 | Herbs regrow 50% in 14 game days in June–July; none after August 15 |
| Dig | Digging stick, shovel; two-block rule | Roots, tubers, rhizomes, seedlings | Removes the clump unless a third is left (root division) | A patch with over 50% of clumps dug yields ×0.6 next year | From left pieces, or from seed |
| Tap | Spile and bucket | Sap | Tree vigor −0.05 for a year | — | Same tree every spring |
| Coppice | Hatchet or axe, dormant season | A stem cut 20–30 cm above ground | Stool resprouts 6–12 shoots next June | — | Poles in 3 years |
| Uproot | Hand or shovel | The whole plant | The clump is gone; a transplantable plant drops a clump item | One clump fewer until seeded | Never by itself for berries: the spot is empty 3 years |

**Hands, digging stick, shovel.** Digging by hand takes ×2 the digging-stick time; a shovel takes ×0.6. Frozen soil cannot be dug, as for clay, from the day the soil freezes to thaw.

**Yield per action.** Every clump holds a set number of units per flush, `units_per_clump` in its plant file. One Pick, Cut or Dig hold takes 1 unit in the time its row lists, and `taken` rises by 1 at each third of the capacity.

| Plant kind | Units per clump a flush | Example |
| --- | --- | --- |
| Herb, green, flower | 3 handfuls | Sourdock: 3 handfuls at 3 game minutes each |
| Root and tuber | 3 roots; the last one is what root division leaves | Wild potato: 3 × 250 g at 15 game minutes each |
| Berry bush | 1–1.7 cups (0.15–0.25 kg), as Every concept from the video › Berry patches sets | Blueberry: about 1.5 cups at 30 game minutes a cup |
| Mushroom | 1 cap block | King bolete: 200–600 g |
| Moss and lichen | 1 sheet or 100 g | Sphagnum: 1 sheet a game minute |
| Bracket fungus | 1 bracket, sized by `size` | Tinder conk: 400 g at `size` 3 |

A patch's season total is clumps × units per clump × S × H × f\_year. A 40-clump sourdock patch at S 1.0 gives 120 handfuls, 3,000 kcal over the season, and a 40-bush blueberry patch about 60 cups in its first flush and about 120 over the season's three flushes (Every concept from the video › Berry patches). Rates at the clump run high (wild potato about 900 kcal an hour, greens about 500, berries about 170); Food systems' 50–300 kcal an hour is the average over a whole round with search and walking between patches, and a root patch is dug out in a few hours. A row that gives no time takes its kind's default: a handful of leaves, shoots or needles 2 game minutes, a handful of flowers 3, a shoot or switch 1 and a pole 3, a moss sheet 1, a mushroom cap 1, a bracket 3; a plant whose row lists only a sign has no harvest, only its inspect card. A row that gives no months is harvestable every thaw month, May to October; bark, roots and poles follow Bark by season and Root digging seasons.

**Gloves matter.** Nettle (existing), devil's club (existing), wild raspberry (existing), monkshood root, arnica on cuts, pasqueflower sap, marsh marigold sap and juniper all bill bare hands. The card names the cost before the player commits.

**What the player hears.** Pick: a soft snap. Strip: the rake's rattle or leaves tearing. Peel: a long rip. Cut: a slice and a rustle. Dig: soil and root fibres tearing. Coppice: three hatchet blows and a stem's whip. Uproot: a sucking pull, then dirt pattering.

### Regrowth and patch health

**Every patch keeps a health number H.** H runs from 0.2 to 1.3 and multiplies the patch's seed value S. It sits in the chunk attachment `yearofice:patches` with the picked fraction p for this season and the last harvest day.

```latex
Y_{season} = S \cdot H \cdot f_{year} \qquad
H_{next} = \begin{cases} 0.5 \pm 0.05 & p > 0.7 \\ \min(H_{cap},\ H + 0.25) & p \le 0.7 \end{cases}
```

- **f\_year** is the year factor already written: berry index, bloom frost, drought, burn bonuses.
- **H\_cap** is 1.0, or 1.2 for a tended patch. Tending means cutting competing brush inside the patch (1 game hour per 10 clumps), once a year. Arvid's notes: "Clear the alder off the currants and they pay you back."
- **The stripped rule is unchanged.** The first case is the existing 45–55% rule; the second is how a patch climbs back: 0.5, 0.75, then 1.0 by the third year.

**Herbs, roots and mosses heal on their own clocks.**

| Plant kind | After a cut | After a dig or uproot | After fire |
| --- | --- | --- | --- |
| Leafy herb (sourdock, chickweed) | 50% in 14 days, once a season | Lost unless a third is left | Back in 1–2 years |
| Root crop (wild potato, bistort, fern root) | — | ×0.6 next year if over 50% of clumps dug | Back in 2 years |
| Shrub (willow, alder, dogwood) | Coppice shoots in 1 year | Stool dies | Resprouts the next June |
| Moss | — | Sphagnum and stair-step moss: 10 years | Fire moss in 1 year, feather moss in 30 |
| Lichen | — | Rock tripe and Iceland moss: 30 years; reindeer lichen: the existing 50 years | 0 |
| Fungus | Picking never harms the next flush | Brackets regrow in 3–5 years (chaga's existing 5) | Morels the first spring (existing) |

**Trees keep vigor V.** V runs 0–1 in the tree record. Tapping costs 0.05 for a year, a bark scar 0.1, a pitch blaze 0.1, a moose-broken top 0.2. A tree under 0.3 drops 30% of its leaves early and gives half the bark, sap and cones. At 0 it dies standing and becomes deadwood.

**Readable health.** The patch card adds one line: "Health: good" (H 0.9 or more), "recovering" (0.6–0.9) or "worn" (under 0.6). The map pin dims for a worn patch.

### Propagation

**Three ways to move a plant, and one wall.** The video's creator was firm: berries are eaten where they grow. Everything in `yearofice:berry_bushes` has no item form, no seed item and no cutting. Every other plant follows its tag.

| Method | Plants | How | Success | Time to harvest |
| --- | --- | --- | --- | --- |
| Seed | Fireweed, dwarf fireweed, lambsquarters, chickweed, dandelion, plantain, pineapple weed, bluejoint, lupine, valerian, spruce, birch, tamarack | Pick at `seed` phase as `yearofice:wild_seed` with a species component; sow on a garden bed or broadcast on bare ground | The crop formula with the plant's own base rate (30–70%); wild seed sown in spring without a winter in the cold scores ×0.2 | 1 year for herbs; trees see below |
| Cutting | Willows, balsam poplar, red-osier dogwood | 30 cm dormant stem pushed into moist thawed soil, October to May | Willow and poplar 80%, dogwood 50%; ×0.3 on dry ground | Coppice poles in 3 years |
| Root division | Wild chives, field mint, wild rhubarb, sourdock, yarrow, fireweed, horsetail, sweetgrass, cattail, valerian | Dig a clump, leave a third, replant the rest within 1 game day | 70% in May and September, 30% in summer, 0 in frozen ground | Next season |
| Transplant | Seedlings of spruce, birch, tamarack and aspen under 0.5 m | Dig with a soil ball (shovel), replant within 6 game hours | 60% in May, 30% later | A 4 m tree in 15–20 years |

**Not moved at all.** All berry bushes (including wild rose, highbush cranberry, soapberry, currants, bunchberry and wild strawberry), common juniper, all lichens, all mosses except as building sheets, and all fungi but one. Breaking a berry bush drops sticks and leaves its spot empty for 3 years, as the video tab says.

**The one fungus.** A fresh aspen log laid against a log fruiting with aspen oysters, in shade, for two summers, fruits with a 40% chance and gives 0.3–0.6 kg a flush for 4 years. It is a slow, small gift, not a farm.

**Garden beds take wild herbs.** A wild herb bed of chives, mint, sourdock and rhubarb near the cabin is legal and readable. Each spreads 1 block a year into bare neighbours, and the existing weed rule applies. Yields stay at the wild per-clump numbers, so scarcity holds.

### Animals and plants

**Animals eat, trample and spread plants on the same ledger the player uses.** Each animal's feeding writes to the patch or tree record; the player reads the result as sign.

| Animal | Eats | When | What it does | Sign | Spreads |
| --- | --- | --- | --- | --- | --- |
| Moose | Willow, aspen, birch and poplar twigs; water horsetail, buckbean and pond weed in summer | All year; aquatic June–August | Removes up to 60% of a shrub's new growth a winter. Three heavy winters "broom" a willow: dense twiggy top, catkins ×0.5 | Twigs torn off rough at 1–2.5 m; tops snapped by ridden-down stems | Seeds in dung, rarely |
| Snowshoe hare | Willow, birch and aspen twigs; spruce seedlings | Winter | In a hare peak, 30% of saplings under 2 m are girdled at snow height | Clean 45° cuts below 0.6 m above the snow | — |
| Beaver | Aspen, balsam poplar, willow, birch within 100 m of water; pond-lily and bur-reed roots | All year | Fells trees; the pond drowns spruce into standing deadwood | Pencil-point stumps, peeled sticks, chips | Cut willow pushed into mud roots 20% of the time |
| Black and grizzly bear | Horsetail and sedge in May; soapberry in July; blueberry and crowberry in August; wild potato, bistort and arrowhead roots in spring and fall | April–October | Tears bushes (`taken` 3); digs root patches (dug ×0.6) | Torn bushes, purple scat, dug turf | Each berry scat on bare ground has 2% to start a seedling patch in 3 years |
| Ruffed and spruce grouse | Aspen and birch buds; spruce needles; berries | Buds in winter, berries in fall | Light; never harms a patch | Bud scales on snow, droppings under aspens | Berry seeds |
| Willow and rock ptarmigan | Willow and dwarf birch buds and twigs; bistort bulbils | Winter, spring | Clips tundra shrubs back 10% a winter | Snow-roost holes, clipped tips | — |
| Red squirrel | Spruce cones; mushrooms; birch sap | All year | Cone middens; larders (existing); bites birch twigs for sap in April | Middens, "sapsicles" of frozen sap on twigs | Buried cones sprout spruce at midden edges |
| Voles | Seeds, roots, bark at the snow base, stored tubers | Winter | Girdle 5% of shrubs under 1 m in vole years; eat cached arrowhead | Tunnels in melting snow, gnawed bark | Spread spores of ground fungi |
| Caribou | Reindeer lichen; sedges, willow leaves and mushrooms in August | Herd passes | The existing lichen rule; eat 50% of a bolete flush where they graze | Cratered snow, cropped sedge | — |
| Wood bison | Sedges, bluejoint | All year | Grazes meadows short; trampled cells lose 20% of hay | Wallows, flattened grass | Seeds on coats along their winter trail |
| Muskrat | Cattail, bulrush, arrowhead, mare's tail | All year | Builds push-ups of plants; caches arrowhead tubers | Push-ups on pond ice | — |
| Waxwings and thrushes | Rose hips, highbush cranberry, juniper, crowberry | Fall and spring | Strip 5% of an unpicked patch a day after peak (existing) | Flocks, purple droppings | Seeds dropped under perches: a pole fence grows rose and cranberry seedlings beneath it in 5 years |
| Porcupine | Inner bark of spruce, birch and tamarack crowns | Winter | Girdles tree tops (vigor −0.2) and wounds bark | Gnawed upper trunk, quill-shed bark | Pitch flows at the wounds (existing pitch rule) |

**Trampling.** A plant block on a cell crossed 30 times in a season (by the player, a sled or an animal on a trail) drops to `taken` 3. After 2 seasons it is a worn cell, which is where plantain, pineapple weed, foxtail barley and shaggy mane appear under the existing rules.

**What it gives the player.** Readable browse tells a hunter where moose fed last winter. Beaver ponds and bear-dug turf tell where the roots are. Bird perches plant the next berry patch, and the player can choose where the perches stand.

**Goats and toxic plants.** The tag `yearofice:goat_toxic` holds monkshood, tall larkspur, water hemlock, false hellebore, wild calla, bog rosemary, leatherleaf and arctic lupine. A penned or tethered goat eats one such clump within reach on 10% of days while one grows in its pen, and always when hay holding one is fed.

| Plant eaten | Effect on the goat | What the player sees |
| --- | --- | --- |
| Monkshood, tall larkspur, water hemlock | Dies within 6 game hours | Staggering, then down; the journal names the plant only if it is identified |
| False hellebore, wild calla, bog rosemary | Milk 0 for 3 game days; the goat loses 30% of its condition | Bloat, groaning, head low |
| Leatherleaf, arctic lupine | Milk −50% for 2 game days | Drooling |

Weeding a pen of every toxic clump (2 game minutes each, gloves for monkshood) clears it for the season; the roots come back next May. Hay cut from wet meadows carries monkshood or larkspur in 5% of bundles unless the meadow was walked and weeded first, and its card reads "mixed hay" until checked. A mounted monkshood or larkspur sheet makes that check name the plant. Ties: livestock care (Crafting and building in depth), goat milk, hay, the herbarium.

## Trees

### The existing trees, verb by verb

**The six tree builders get their full harvest list.** Numbers already written elsewhere are referenced, not repeated.

| Tree | Pick | Peel | Tap | Cut and coppice | Dig | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| White spruce `yearofice:white_spruce_log` | Tips from bud burst at 60 degree-days (about May 25) until 330 (about June 20), the existing May–June window; before that the card reads "Tips not out yet" and gives nothing; cones July–September, 20 a game minute, for squirrel bait and seed | Inner bark May 15 – June 30 (existing); bark sheets May 15 – July 15 | No | Boughs for beds and smudges; does not coppice | Roots for lashing, June–September (below) | Gum and pitch (Plant work through the year) |
| Black spruce `yearofice:black_spruce_log` | Tips, smaller (×0.6); cones stay closed until fire | Thin bark, poor sheets | No | Poles 5–10 cm, dense and slow-grown: the best snowshoe and rack poles | Roots in moss, easy to pull: 1 m a game minute | Layers: a lower branch in moss roots into a new tree, so black spruce stands creep |
| Birch `yearofice:birch_log` | Catkins and buds in April (grouse food; tea, +0.5 Spirits) | Outer bark by season (below); inner bark (existing) | Yes: the tapping season | Coppices weakly: 50% of stools die after a second cut | — | Chaga 1 in 12 (existing); tinder conk and polypore on dead trees |
| Aspen `yearofice:aspen_log` | Buds in winter | Inner bark May–June: 120 kcal per 250 g, bitter | No | Never coppices from the stump: cut aspen sends 20–60 root suckers within 10 m | — | Whitish powder on the bark rubs off as a sunscreen: snow blindness onset +1 game hour |
| Willow (any) | Leaves, catkins | Inner bark: fibre in spring, tea all year (existing) | No | Coppices strongly: every 3 years for ever | Cuttings root 80% | Six species below; the old generic willow becomes feltleaf willow on riverbanks |
| Alder (any) | Cones in fall (dye) | Inner bark for dye, all year | No | Coppices strongly | — | Two species below; fixes nitrogen |
| Subalpine fir (existing) | Needles for tea (existing) | Resin blisters on smooth bark: 5 blisters a game minute | No | Boughs: the softest bed, +5% sleep recovery | — | Mountain regions only |

### New trees

| Species and id | Where and when | Spot it; lookalike | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Tamarack `yearofice:tamarack_log`, `tamarack_leaves` | Bog margins and wet flats with black spruce under 400 m; Sloughs and Lake country, 1 tree in 15 at muskeg edges. Soft green needles May 20; gold September 15 – October 5; bare all winter | The only conifer that drops its needles: tufts of 15–30 soft needles on knobby spurs, cones 1.5 cm. In winter it looks dead. A player who fells one for dry fuel gets green wood (dryness 30) | Rot-resistant: sills and posts decay ×0.5 under Structures and buildables › Rot. Roots split finer than spruce: bark sewing and basket rims. Bent green poles for toboggans and sled runners. Inner bark poultice on a burn: heals 25% faster, once a day | Log 4 m and 35 kg; roots 3 m a tree a year, 4 game minutes a metre | Porcupines in winter; spruce grouse eat needles; tamarack jacks grow only under it |
| Balsam poplar `yearofice:balsam_poplar_log`, `balsam_poplar_leaves` | River islands, gravel bars and floodplains; the tallest trees in the valley, 25–30 m and up to 90 cm across. Buds sticky March–May; fluff flies June 10–25; yellow early September | Deeply furrowed grey bark; buds smell of balsam from 50 m in spring. Aspen has smooth pale bark and round trembling leaves | Bud salve (Plant medicine). Thick bark of dead trees carves into net floats and bobbers: 3 floats a chunk. Soft dry wood is the best bow-drill set: success +15%. June fluff catches a spark at dryness 70. Spring inner bark: 140 kcal per 250 g, sweet. Light fuel: heat ×0.6. Interior people's smoke wood (Smoke woods) | Buds 50 g in 5 game minutes from low branches or fresh windfall; a bark chunk 0.5 kg in 3 game minutes | Beavers' and moose's winter favourite; bald eagles nest in the largest; cuttings root 80% |

**Poplar fluff tells the river.** When the fluff drifts on the water, king salmon are in the river within 5 days in 70% of years (Plant signs).

### Willows and alders by species

**The generic willow and alder split into six species.** Each keeps the existing willow and alder uses; the table gives only what is different. The tree builders take a `species` parameter.

| Species and id | Where and when | Spot it | What is different | Animals |
| --- | --- | --- | --- | --- |
| Feltleaf willow `yearofice:feltleaf_willow` | River bars, creek bottoms, burns; 3–8 m. Catkins April 20 – May 10 | Thick white felt under the leaves; fat buds; stems to 15 cm | Stems 8–12 cm make tent poles and rack rails; the strongest willow-bark fibre (quality +1 step) | Moose browse it first (weight ×2); willow ptarmigan winter in its flats |
| Diamondleaf willow `yearofice:diamondleaf_willow` | Bog edges, tundra, alpine; 0.5–2 m | Shiny diamond-shaped leaves, red twigs | Young leaves June 1–25 are food: a handful 15 kcal, +1.5 vitamin C, bitter (−1 Spirits raw). Packed in rendered fat or fish oil, 2 kg keep 180 days at +1 vitamin C a cup: the third winter scurvy defense | Ptarmigan buds; caribou leaves in summer |
| Barclay willow `yearofice:barclay_willow` | Subalpine meadows and avalanche paths, 400–800 m | Leaves turn black when dried, the herbarium tell; "willow roses", galls like small cones at twig tips | Shoots are the most flexible: basket and fish-trap work ×0.8 time. Dry galls are tinder at dryness 60 | Moose at timberline in summer |
| Bebb willow (diamond willow) `yearofice:bebb_willow` | Birch–aspen slopes and burn edges | Grey hairy leaves; 1 stem in 8 has diamond-shaped sunken cankers | Diamond stems peeled and seasoned 30 days carve into a Fine walking stick or carved staff (Ideas from viewers › The walking stick): +2 Spirits once, trade value ×3 | Hares girdle it in peak years |
| Green alder `yearofice:green_alder` | Slopes above 400 m, avalanche paths, burn edges; 1–3 m tangles | Sticky glossy leaves, clustered small cones | Tangles slow walking ×0.6. Fixes nitrogen: a garden bed on cleared green-alder ground yields +15% for 3 years. Green alder sticks make the most smoke | Snowshoe hares shelter in it; grouse |
| Thinleaf alder `yearofice:thinleaf_alder` | Riverbanks and sloughs; 3–8 m | Grey bark with orange flecks; cut bark turns orange-red in 2 minutes | The salmon smoke wood (Smoke woods). Inner bark gives orange-red to brown dye. Catkins in April shed yellow pollen: "the alders are smoking" | Beavers use it when aspen is gone; a moose reads it as poor food |

## Shrubs and berries

### Shrubs

| Species and id | Where and when | Spot it; lookalike | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Dwarf birch `yearofice:dwarf_birch` | Tundra, bog, alpine heath; 0.3–1 m. Leaves copper-red in early September | Round toothed leaves 1 cm across, resin dots on twigs | The fuel where no trees grow: an armload in 10 game minutes, heat ×0.7, burns fast. Leaves give a yellow-green dye | Armload 4 kg; regrows in 3 years | Ptarmigan buds; caribou leaves |
| Leatherleaf `yearofice:leatherleaf` | Sphagnum bog mats. White bell flowers in a row under the twig, May 5–25 | Leathery evergreen leaves with rusty scales beneath, no wool. Lookalike of Labrador tea (New lookalikes) | None as food. Its bells are the first bog flowers: the mat has thawed 20 cm (Plant signs). Tea is toxic | — | Hares and grouse in winter |
| Sweetgale `yearofice:sweetgale` | Lake, slough and bog edges; 0.5–1.5 m. Catkins before leaves in May | Wedge-shaped leaves toothed at the tip, resin dots; sweet resin smell when crushed | Leaves on a smudge or strewn under a bed: bugs −50% within 10 m for 3 game hours. Flavours fish and sap beer (+1 Spirits). Yellow dye from catkins. Fixes nitrogen | A handful in 2 game minutes; leaves June–September | Moose ignore it |
| Shrubby cinquefoil `yearofice:shrubby_cinquefoil` | Gravel bars, river terraces, fens; 0.3–1 m. Yellow 5-petal flowers June 15 – August 31 | Shreddy bark; small leaves of 5 narrow silky leaflets | The all-summer trail tea: +1 Spirits once a day, and +0.5 vitamin C a cup with no daily limit. Shredded bark is tinder at dryness 70 | A handful in 2 game minutes | Moose and hares avoid it |
| Common juniper `yearofice:common_juniper` | Dry south bluffs and gravel terraces; low mats 0.3–1 m. Berries take 2 years: green and blue on one plant | Sharp needles in whorls of 3 with a white stripe; blue waxy berries | 5 berries season meat or fish: breaks the five-day food streak. Tea 1 cup a day: +1 Spirits; a second cup the same day costs −5 Condition (kidneys). Boughs on a fire make a clean, sharp smoke: the cabin's smoke smell clears 2× faster | 20 ripe berries in 5 game minutes, gloves or −1 Condition | Thrushes and grouse spread it; not transplantable |
| Red-osier dogwood `yearofice:red_osier_dogwood` | Riverbanks, sloughs, beaver meadows; 1–3 m | Blood-red stems in winter, readable at 100 m; opposite leaves with veins curving to the tip; white berries | Straight 1-year shoots make arrow shafts: 4 shafts a game hour, Good quality. Basket rims. Berries are bitter: 20 kcal a cup, −2 Spirits | Shoots October–April | Moose browse ×1.5; cuttings root 50% |
| Beauverd spirea `yearofice:beauverd_spirea` | Open spruce woods and alpine slopes; 0.3–0.8 m. Flat white-pink flower heads July | Toothed oval leaves; flowers in a flat-topped cluster | Flower tea for pain (Plant medicine) | A handful of flowers in 3 game minutes | Bumblebees |
| Mountain heather `yearofice:mountain_heather` | Alpine heath above 650 m, snowbeds | Four rows of scale leaves on each stem; white nodding bells in June | Resinous: a handful lights at dryness 40, even after rain. It is the tundra's fatwood. Low heat | A handful in 1 game minute; regrows in 10 years | Caribou walk over it; ptarmigan shelter |

### Berries

**All of these are in `yearofice:berry_bushes`.** They keep the berry rules: patches, peak week, fermenting, bears at the patch, no transplanting.

| Species and id | Where and when | Spot it; lookalike | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Soapberry `yearofice:soapberry_bush` | Dry open spruce woods, gravel terraces, south bluffs. Fruit July 1 – August 10 | Opposite oval leaves green above, silvery below with rusty dots; translucent red-orange berries. One of the red-berry trio with highbush cranberry and baneberry | Raw: bitter, 50 kcal and +1.5 vitamin C a cup, −1 Spirits. Whipped with 0.5 L water and a spoon of syrup or berries for 10 game minutes: a foam of 4 servings, +4 Spirits, counted once a week. Over 2 cups a day: gut illness 1 day (saponins) | Cup in 20 game minutes; the rake does not work (berries burst) | The main July bear food: bear food weight ×1.5 at a soapberry patch in July |
| Northern black currant `yearofice:black_currant_bush` | Wet creek bottoms, alder thickets, beaver meadows. Fruit July 20 – August 25 | Maple-like leaves with yellow resin dots; musky smell when crushed | The richest berry: 70 kcal and +2.5 vitamin C a cup. Leaves dried for tea: +1 Spirits. Sets jam without syrup | Cup in 25 game minutes | Bears, grouse |
| Skunk currant `yearofice:skunk_currant_bush` | Burns 5–20 years old, rocky slopes. Fruit July 15 – August 20 | Bristly red berries in drooping strings; skunky smell crushed | 50 kcal and +1 vitamin C a cup; bristles cost −1 Spirits raw, none in berry leather | Cup in 25 game minutes | Bears, thrushes |
| Alpine bearberry `yearofice:alpine_bearberry_bush` | Tundra and alpine heath. Fruit August 1–31 | Net-veined leaves that turn scarlet in late August, the first fall colour on the mountain; black juicy berries. The existing bearberry has red, mealy fruit and evergreen leaves | 40 kcal and +0.5 vitamin C a cup; bland | Cup in 25 game minutes | Ptarmigan, bears, foxes |
| Bunchberry `yearofice:bunchberry_bush` | Mossy spruce and birch floor, everywhere in old forest. Four white bracts in June; red berry bunches August 10 – September 30 | A 15 cm plant: one whorl of 6 leaves with curved veins, a tight cluster of red berries. Baneberry is a taller herb with berries along one stalk | 40 kcal and +0.5 vitamin C a cup; bland and seedy. Rich in pectin: added to berry leather (1 cup in 4), the sheets dry in half the time | Cup in 30 game minutes | Spruce grouse eat it all fall |
| Wild strawberry `yearofice:wild_strawberry_bush` | Dry south slopes, burn edges, gravel terraces under 400 m; 2–5 patches a region. Fruit June 25 – July 20 | Three toothed leaflets, red runners; berries 1 cm | 50 kcal and +2 vitamin C a cup; +3 Spirits the first cup each year. Leaves dried for tea: +0.5 vitamin C | Cup in 40 game minutes | Ground squirrels, grouse, bears |

**Soapberry foam is the valley's treat.** It is the only food the player whips. The card shows the bowl rising as right mouse is held, and it fails if the bowl has any fat in it, as real soapberry foam does.

## Herbs and flowers

### Food herbs

| Species and id | Where and when | Spot it; lookalike | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Dwarf fireweed (river beauty) `yearofice:dwarf_fireweed` | New gravel bars and silt of the glacial river and braided flats. Flowers July 1 – August 15 | Lower than fireweed, fleshy blue-green leaves, magenta flowers 3–4 cm, twice fireweed's size | Leaves and shoots June: 25 kcal and +1 vitamin C a handful, raw or cooked. Its bar sign (Plant signs) | Handful in 3 game minutes | Caribou and moose crop it |
| Alaska wild rhubarb `yearofice:wild_rhubarb` | Riverbanks, terraces, ravines; 1–2 m. Stems June 1–30; cream flower plumes July | Papery sheaths at each stem joint; sour juice. Lookalike: false hellebore shoots in May (New lookalikes). It is not the roadhouse's feral garden rhubarb | Peeled stems raw or stewed: 20 kcal and +1.5 vitamin C a handful; sour, breaks the food streak. Young leaves cooked as greens. The roadhouse's feral rhubarb is its own block, `yearofice:feral_rhubarb`: its stalks eat as wild rhubarb's, but its leaves are toxic, −15 Condition a meal, and the card warns of it once the plant is identified | Handful of stems in 3 game minutes; root division | Bears in June |
| Chickweed `yearofice:chickweed` | Only within 50 m of old gardens (Station Nine, the roadhouse, Arvid's cabin) and the player's own beds. May–September | Tiny white star flowers with split petals; one line of hairs down the stem | Mild greens: 15 kcal and +1 vitamin C a handful, raw. Regrows 10 days after a cut, 3 cuts a season | Handful in 2 game minutes | Grouse, hens; counts as a garden weed |
| Lambsquarters `yearofice:lambsquarters` | Rich disturbed soil: garden beds, middens, burned cabin sites. Leaves June–July; seed September | White mealy dust on young leaves; goosefoot-shaped leaves | Leaves cooked: 30 kcal and +1.5 vitamin C a handful; more than 3 raw handfuls a day cost −2 Condition. Seeds stripped and winnowed: 100 g in 10 game minutes, 370 kcal, carbs and protein. Weeding a garden bed yields 2 handfuls | As listed | Hens, voles |
| Alpine bistort `yearofice:alpine_bistort` | Meadows and tundra above 300 m. Flowers June–July | A slim spike: white flowers above, purple bulbils below | Bulbils raw: 30 kcal a handful, nutty. Rhizome dug spring or fall: 50 g, 60 kcal, raw or roasted | Handful in 4 game minutes; root in 3 | Ptarmigan, voles, bears dig it |
| Mountain sorrel `yearofice:mountain_sorrel` | Alpine seeps, rock crevices, snowbed edges. June–August | Kidney-shaped fleshy leaves; red papery seed clusters | Lemony: 10 kcal and +2 vitamin C a handful, the richest green. Over 4 handfuls a day: −3 Condition (oxalate) | Handful in 2 game minutes | Pikas pile it in their haypiles; marmots |
| Tall bluebells `yearofice:tall_bluebells` | Birch woods, alder thickets, meadows. June–July | Pink buds opening to blue hanging bells; rough hairy leaves | Flowers and young leaves raw: 10 kcal and +0.5 vitamin C a handful; a garnish worth +1 Spirits once a day | Handful in 2 game minutes | Bumblebees (the bee index) |
| Arctic sweet coltsfoot `yearofice:sweet_coltsfoot` | Wet meadows, seeps, trail edges. Flower heads at snowmelt, April 25 – May 20, before the leaves | Pink-white fuzzy heads on scaly stalks; later big arrow-shaped leaves white beneath | Salt: 1 kg of dried leaves burned on a flat stone gives 50 g of salty ash. It seasons food and breaks the food streak, but does not cure meat. Cough tea (Plant medicine) | 1 kg of leaves in 10 game minutes; drying 1 day | Moose and hares eat the first leaves |

### Medicine, craft and dye herbs

| Species and id | Where and when | Spot it; lookalike | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Northern geranium `yearofice:northern_geranium` | Meadows and birch woods. Flowers June 15 – July 31 | Pink-violet open 5-petal flowers; soft-hairy deeply lobed leaves. Lookalike: monkshood leaves | Root tea for diarrhoea (Plant medicine) | Root in 5 game minutes digging | Moose and bears graze it |
| Field mint `yearofice:field_mint` | Wet riverbanks, slough edges, beaver meadows. Leaves June–September; lilac flowers July | Square stem; flowers in whorls at the leaf joints; mint smell (the smell check) | Tea for nausea (Plant medicine). Flavours any dish: +1 Spirits once a day | Handful in 2 game minutes; root division | — |
| Capitate valerian `yearofice:valerian` | Moist meadows and tundra. Flowers June | Round pink-white flower heads; root smells of old socks | Root tea for sleep (Plant medicine) | Root in 5 game minutes, spring or fall | Caribou graze the leaves |
| Roseroot `yearofice:roseroot` | Alpine ledges and scree near seeps, 600 m and up. June–August | Fleshy blue-green leaves crowded up the stem; dark red flower heads. The cut root smells of roses | Leaves raw: 15 kcal and +1 vitamin C a handful. Root tea against fatigue (Plant medicine) | Root in 10 game minutes on a ledge | Pikas, marmots, mountain goats |
| Arnica `yearofice:arnica` | Dry meadows, alpine, burns. Flowers July | Yellow daisy heads 4–6 cm; opposite leaves in pairs | Flower salve for sprains and bruises, on unbroken skin only (Plant medicine). Toxic eaten | 20 flower heads in 5 game minutes | Ignored by grazers |
| Tilesius wormwood ("stinkweed") `yearofice:wormwood` | River bars, burns, disturbed ground. June–September | Silvery deeply cut leaves white beneath; strong sage smell | Bitter tea against parasites; leaves on banya stones ease aches; a smudge additive (Plant medicine) | Handful in 2 game minutes | Moose avoid it |
| Northern bedstraw `yearofice:northern_bedstraw` | Meadows, gravel, open woods. Flowers July | Leaves in whorls of 4; frothy white flower clusters; sweet hay smell dried | Roots give the only true red dye. Dried stems stuff a mattress: sleep recovery +5% | 100 g of roots in 15 game minutes; stems in 10 | — |
| Northern goldenrod `yearofice:goldenrod` | Meadows, burns, alpine edges. Flowers August | Dense yellow spikes on single stems | Flowers give a bright yellow dye. Late flowers feed the bumblebees: a region with 10 or more goldenrod patches adds 0.05 to its next June bee index | 1 kg of flowers in 15 game minutes | Bees and late butterflies |
| Mountain avens `yearofice:mountain_avens` | Gravel bars, alpine ridges, limy ground. Flowers June; feathery seed plumes July | Mats of small scalloped leaves, white beneath; 8-petal white flowers | Leaf tea: +1 Spirits, settles digestion (nausea −10). The flowers track the sun (Plant signs) | Handful in 2 game minutes | Ptarmigan eat the buds |
| Tall Jacob's ladder `yearofice:jacobs_ladder` | Wet meadows and willow edges. Flowers June–July | Leaflets in a ladder; sky-blue flowers; skunky when crushed | A wildflower bouquet (Plant signs › Flowers indoors). Marks the best hay ground | — | — |

### Dangerous herbs

**Five herbs that hurt.** Their effects are in New lookalikes and poisons. Only logbook pages and the herbarium teach them; the pocket guide does not.

| Species and id | Where and when | Spot it; lookalike | Why it matters | Animals |
| --- | --- | --- | --- | --- |
| Monkshood `yearofice:monkshood` | Wet meadows, willow thickets, alpine meadows; 2 blocks tall. Flowers July 10 – August 31 | Dark blue hooded flowers; glossy deeply cut leaves; pale tuberous roots. Lookalike: northern geranium | The deadliest plant after water hemlock. Digging the root bare-handed costs −3 Condition and numb fingers for 1 game hour. It is never an arrow poison: meat it touches is poisoned, so the station refuses the recipe | Goats eat it (10% a day in a pen with it); only bumblebees work its flowers |
| Tall larkspur `yearofice:tall_larkspur` | Moist meadows and willow, often beside monkshood. Flowers July | Blue spurred flowers up a tall stem; leaves like monkshood's | Toxic; same lookalike group as monkshood | Kills goats; moose avoid it |
| False hellebore `yearofice:false_hellebore` | Wet subalpine meadows of the southern mountain regions, 500–900 m. Shoots May 15 – June 10; green flower plumes July | Broad pleated leaves with parallel veins, wrapped round the stem. Lookalike: wild rhubarb shoots | Highly toxic; its shoots come up beside the first edible greens | Nothing eats it |
| Arctic lupine `yearofice:arctic_lupine` | Gravel bars, burns, alpine. Flowers June; pods July–August | Blue pea flowers; leaves of 6–8 leaflets like a hand | Seeds are toxic; pods look like the wild potato's and wild sweet pea's. Fixes nitrogen: beds on lupine ground +10% for 2 years | Ptarmigan eat the leaves safely |
| Pasqueflower `yearofice:pasqueflower` | Dry south bluffs and gravel terraces. Flowers April 20 – May 15, before its leaves | Furry lavender cups close to the ground; feathery seed heads later | Toxic and blistering; its bloom is the first spring sign (Plant signs) | Nothing eats it |

## Grasses, sedges, ferns and horsetails

### Grasses and sedges

| Species and id | Where and when | Spot it | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Bluejoint `yearofice:bluejoint` | Wet meadows, burns, beaver meadows, terraces: the valley's main grass, 1–1.5 m | Blue-green leaves; purple-tinged open heads in July | Hay: cut July 1–31 for full feed value; after August 15 ×0.6. Insoles: a fist of grass in each mukluk gives +1 °C to the feet and dries overnight by the stove. Thatch under sod. Its dead mat carries spring fire ×2 and smothers tree seedlings, which slows succession on a burn by 5 years | Existing hay rates; insoles 2 game minutes | Voles nest under the mat; bison graze it |
| Tussock cottongrass `yearofice:cottongrass` | Muskeg and tundra tussocks. Cotton heads June 10 – July 10 | One white cotton tuft per stem on knee-high tussocks | Stem bases April–May: 20 kcal a handful, sweet. Cotton: fat-lamp wicks (smoke −20%), tinder at dryness 60, mitten stuffing (+1 °C hands from 200 g, 2 game hours picking) | Cotton 20 g a game minute | Geese and caribou eat the bases; tussocks slow walking (existing) |
| Water sedge `yearofice:water_sedge` | Marsh, pond edges, beaver meadows | Triangular stems ("sedges have edges"); dark brown spikes | Sedge hay (existing). Shoe grass: combed and softened sedge stuffed in boots gives +2 °C to the feet and wicks wet away 2× faster; replace every 7 game days. Softening takes 10 game minutes of beating per pair | Shoe grass for one pair in 15 game minutes | Geese, bison, moose |
| Softstem bulrush `yearofice:bulrush` | Lake and slough shallows 0.3–1.5 m | Round soft grey-green stems to 2 m; brown spikelets near the tip | Woven sleeping mat: ground cold through a bed −30%. Bags and roof underlay. Rhizome starch like cattail: 200 kcal per 250 g, 15 game minutes wading | Mat needs 300 stems, 2 game hours cutting, 3 weaving | Muskrat, nesting ducks |
| Foxtail barley `yearofice:foxtail_barley` | Disturbed ground, salt licks, roadhouse yards, worn trails. Heads July–September | Silky purple-tan bushy heads with long awns | None. Hay with more than 10% foxtail injures goats' mouths: milk −20% for a week. A tamed wolf running through it in August has 5% a day to get an awn in an ear: head-shaking, −10% stamina until it is pulled (hold right mouse on the wolf, 1 game minute) | — | Spreads on worn cells under the plantain rule |
| Sweetgrass `yearofice:sweetgrass` | Moist meadows and lake edges | Shiny leaves, purple at the base; vanilla smell (the smell check) | Braids: 30 blades, 10 game minutes. A braid smouldered in the cabin gives +1 Spirits that evening and bugs −20% in the room, once a day | 30 blades in 5 game minutes; root division | — |

### Ferns, horsetails and clubmoss

| Species and id | Where and when | Spot it; lookalike | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Ostrich fern `yearofice:ostrich_fern` | Silty riverbanks and islands under poplar and alder, in the lower river regions. Fiddleheads May 20 – June 10 | Smooth stem with a deep U-shaped groove inside; brown papery scales that rub off. Lookalikes: lady and wood fern fiddleheads | Fiddleheads boiled 15 game minutes: 35 kcal and +1 vitamin C per 100 g. Raw or undercooked: 1 day of gut illness. Taking more than 3 of a crown's 6 fiddleheads kills the crown | 100 g in 4 game minutes | Moose and bears graze ferns lightly |
| Lady fern `yearofice:lady_fern` | Moist woods, ravines, devil's club ground | Fronds narrowing at both ends; fiddleheads with dark scales, no deep groove | Rhizome roasted in fall: 90 kcal per 100 g. Fiddleheads are bitter: −1 Spirits | Rhizome 100 g in 8 game minutes | — |
| Spinulose wood fern `yearofice:wood_fern` | Spruce and birch forest floor, all regions | Broad triangular fronds that stay green into October | "Fern root": rhizome roasted, September–October and April: 100 kcal per 100 g, starch. Fronds pack fish in a basket: spoilage ×0.8 on the walk home. Fronds are bedding | Rhizome 100 g in 10 game minutes | Bears dig it in spring |
| Water horsetail `yearofice:water_horsetail` | Shallow pond and slough edges in 0.2–1 m of water | Hollow jointed green stems with few or no branches | Shoots as horsetail, under its 2-handful limit. Marks a firm bottom: wading where it grows never sinks into silt | — | Moose in summer; muskrat |
| Stiff clubmoss `yearofice:stiff_clubmoss` | Mossy spruce forest. Spore cones August–September | Bottlebrush stems 10–20 cm along a creeping runner | The dye mordant (Dyes): 200 g a bath. Spore dust keeps a blister or rash dry: heals 25% faster. A pinch thrown into a flame flashes a 1 m fireball for a second: +1 Spirits the first time | 200 g in 5 game minutes; regrows in 5 years | — |

## Mosses and lichens

### Mosses

**The generic moss sheet splits by species.** Moss sheets in Gathering materials keep their chinking and insulation use; each species adds something.

| Species and id | Where and when | Spot it | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Sphagnum `yearofice:sphagnum` | Bog and muskeg mats, all year; frozen in place November–April | Cushions of red, green and brown heads like tiny stars; holds 20× its weight in water | A dried handful is its own dressing pad: clean for 24 game hours, it stops bleeding in 5 game minutes at half the rate meanwhile (Body, injury and sickness › C2). Mitten and boot stuffing. Packing for fish and berries (spoilage ×0.8). Dry, it is tinder at dryness 70 | 1 sheet a game minute (existing); dries 1 day | Bog berries grow from it; moose sink in it |
| Stair-step moss `yearofice:stair_step_moss` | Mature spruce forest floor | Fronds stacked in steps, one new step each year | Count the steps: years since the floor was burned or felled, up to 15 (Plant signs). The best chinking: walls chinked with it lose 5% less heat than with mixed moss | 1 sheet a game minute | — |
| Red-stemmed feather moss `yearofice:feather_moss` | The forest carpet in spruce and mixed stands | Red stems, fern-like green branches | Bedding: +2% sleep recovery. On a day with fire danger High or above, a dry feather-moss floor carries ground fire ×1.2 | 1 sheet a game minute | Voles tunnel under it |
| Haircap moss `yearofice:haircap_moss` | Burns, bog edges, sandy ground | Stiff dark stems 10–20 cm with star-shaped tips | Stems twist into a Clumsy cord that never rots (1 m in 4 game minutes). Bundled into a broom: sweeping a floor drops cabin dirt and lice risk | Bundle in 3 game minutes | — |
| Fire moss `yearofice:fire_moss` | Burns 1–5 years old, bare mineral soil, old sod roofs | Carpets that turn purple-red with capsules in June | A burn-age sign: fire moss with no fireweed yet means burned last year, so morels this spring | — | — |

### Lichens

**Reindeer lichen stays as written.** The Land tab's lichen index, famine food and caribou routes cover it; the inspect card now names it "caribou lichen" too. Four lichens join it.

| Species and id | Where and when | Spot it | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Rock tripe `yearofice:rock_tripe` | Boulders, rock faces and scree above 300 m; all year | Leathery dark discs fixed at one central point; crisp dry, rubbery wet | Famine food: soaked and boiled twice for 1 game hour, 100 g dry gives 120 kcal of carbs and thickens a stew. Boiled once: 1 day of gut illness | 100 g in 5 game minutes when wet; dry, it shatters (×2 time). Regrows in 30 years | Mountain goats, pikas |
| Iceland moss `yearofice:iceland_moss` | Tundra, alpine heath, old black spruce openings | Brown-green strap lobes with fringed edges, pale beneath | Soaked overnight in ash water, then boiled: 100 g dry gives 250 kcal, no longer bitter. Dried and pounded in the mortar, it stretches barley flour 1 : 1 for bread. Cough tea (Plant medicine) | 100 g in 6 game minutes. Regrows in 30 years | Caribou in winter |
| Sunburst lichen `yearofice:sunburst_lichen` | Orange crust on rocks and old wood where birds and mammals sit and leave droppings | Bright orange-yellow rosettes | A sign: an orange boulder top is a raven or raptor perch, or a marmot and pika lookout; an orange streak down a cliff marks a nest above. Orange dye after 14 game days steeped in stale urine (−2 Spirits while it ferments indoors) | 50 g in 10 game minutes scraping | Gyrfalcons, eagles and ravens make it |
| Map lichen `yearofice:map_lichen` | Rock faces and stable boulders | Yellow-green crust in patches with black borders | A clock: it grows about 0.5 mm a year once established. A grey face with none fell or was scoured in the last 50 years, and the Rockfall rule rates that face ×2. Patches on Kessler and Doane's tailings reach 4 cm | — | — |

## Fungi

### Brackets

**Bracket fungi grow on log faces, all year.** Tinder conk is used by the carried-ember and rot rules already; this is its first full row.

| Species and id | Where | Spot it; lookalike | Uses | Yield and time | Effect on trees |
| --- | --- | --- | --- | --- | --- |
| Tinder conk `yearofice:tinder_conk` | Dead and dying birch; rotten birch blocks (Structures and buildables › Rot) | Grey hoof-shaped bracket with a hard crust and brown pores. Lookalikes: red-belted conk, birch polypore | Amadou: the inner layer boiled 1 game hour in ash lye, pounded and dried. An amadou strip takes a spark from flint and steel at dryness 30, in any weather, and holds an ember 4 hours (existing) | One 400 g conk gives 50 g of amadou, 10 strips, in 2 game hours of work | The birch is already dead |
| Red-belted conk `yearofice:red_belted_conk` | Dead spruce, stumps, living spruce with heart rot | Hard bracket with a red-orange band near the edge and white pores | Carries an ember 2 hours. A living spruce with one is hollow inside: it falls the wrong way 30% more often (Gathering materials › green logs) and its logs rot ×1.5 | 1 conk in 3 game minutes | Marks a dying spruce |
| Aspen bracket `yearofice:aspen_bracket` | Living aspen | Black, deeply cracked hoof brackets low on the trunk | A sign: the aspen is hollow-hearted. Hollow trunks hold woodpecker and flying-squirrel nests, and a hollow log makes a marten box in half the time | — | Heart rot; the tree snaps in wind ×2 |

### Mushrooms

**Mushrooms fruit in the existing flushes.** Kcal are for fresh mushrooms; drying keeps the kcal and cuts the weight by 90%. Raw mushrooms give 25% (Ideas from viewers › Raw mushrooms).

| Species and id | Where and when | Spot it; lookalike | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| King bolete `yearofice:king_bolete` | Sandy well-drained birch and spruce slopes; flushes, August mostly; 1 bolete in 5 | Fat club stem with a white net on top; brown bun cap; white pores turning yellow-olive. Lookalike: poison pax only by colour, never by pores | 100 g: 30 kcal. Dried: +3 Spirits a meal, as morels. Large caps are maggoty 40% of the time from the second day of a flush: the Cut check shows tunnels; eating them costs −1 Spirits | 100 g in 2 game minutes; caps weigh 200–600 g | Squirrels dry them; caribou eat them |
| Hedgehog mushroom `yearofice:hedgehog_mushroom` | Mossy spruce and birch forest; August–September | Tan cap with soft spines underneath instead of gills or pores | 100 g: 25 kcal; firm, keeps 3 days. No lookalike in the valley: the first mushroom Arvid's logbook teaches | 100 g in 3 game minutes | Squirrels, voles |
| Chanterelle `yearofice:chanterelle` | Scarce inland: 0–2 patches a region, old birch-spruce stands on south slopes; August | Egg-yellow; blunt forked ridges running down the stem, not gills; apricot smell. Lookalike: false chanterelle | 100 g: 30 kcal; +3 Spirits the first meal each year | 100 g in 4 game minutes | — |
| False chanterelle `yearofice:false_chanterelle` | Rotting conifer wood and duff; August–September | Orange with true thin crowded gills; no smell | Mildly toxic (New lookalikes and poisons) | — | — |
| Tamarack jack `yearofice:tamarack_jack` | Only under tamarack; August–September | Slimy yellow-orange cap, yellow pores, a ring on the stem | 100 g: 20 kcal after the slimy skin is peeled (1 game minute); unpeeled, 10% chance of 1 day of gut illness | 100 g in 3 game minutes | Squirrels |
| Aspen oyster `yearofice:aspen_oyster` | Dead aspen and balsam poplar logs; May–June and September | White shelves in clusters, gills running down a short side stem. Lookalike: deadly galerina on the same logs | 100 g: 30 kcal. The one fungus that can be spread (Propagation) | 100 g in 2 game minutes | Beetles riddle old ones |
| Deadly galerina `yearofice:deadly_galerina` | Rotting spruce and aspen logs; May–June and August–October | Small honey-brown caps 2–4 cm, a thin ring, rusty-brown spore print | Deadly (New lookalikes and poisons) | — | — |
| Destroying angel `yearofice:destroying_angel` | Birch and mixed woods; August | All white: white gills, a skirt-like ring, and a sac round the stem base hidden in moss. Lookalikes: a young puffball, shaggy mane | Deadly (New lookalikes and poisons) | — | — |
| Gem-studded puffball `yearofice:puffball` | Trails, disturbed ground, mossy logs; August–September | White pear shape covered in small cone spines; no stem or gills | Edible while pure white and solid inside: 100 g, 25 kcal. Old brown puffballs puff spore smoke when kicked; the dry powder dusted on a cut stops bleeding like yarrow | 100 g in 2 game minutes; each one must be cut (the Cut check) | Voles |
| Orange jelly `yearofice:orange_jelly` | Dead spruce wood after rain; all thaw months | Bright orange blobs that swell wet and shrink to a crust dry | Tasteless, 5 kcal; thickens soup. A sign: swollen jelly means rain in the last 24 hours, so wood on that log is wet | — | — |
| Hawk's wing `yearofice:hawks_wing` | Under spruce on sandy ground; August | Large brown cap with raised dark feather-like scales; spines beneath | Bitter: −2 Spirits and 10% gut illness eaten. Simmered with iron water it gives the only blue-green dye | 1 kg in 10 game minutes | — |

**No fungus takes the taste test.** See The tasting test.

## Aquatic plants

**Water plants are harvested wading or from a raft or canoe.** Wading follows Swimming and working in water, so a cold-water harvest costs warmth. Yellow pond-lily (`yearofice:lily_pad`) and pond weed stay as written; pond-lily seeds are new below.

| Species and id | Where and when | Spot it; lookalike | Uses | Yield and time | Animals |
| --- | --- | --- | --- | --- | --- |
| Buckbean `yearofice:buckbean` | Bog pools and pond edges in 0.1–0.5 m of water. Flowers June | Three oval leaflets like a bean held above water; fringed white-pink flowers | Bitter leaf tea for the stomach (Plant medicine). Rhizome famine flour: dried, pounded, soaked 24 game hours, baked: 100 g gives 250 kcal, −1 Spirits | Rhizome 250 g in 15 game minutes wading | Moose in summer |
| Marsh marigold `yearofice:marsh_marigold` | Seeps, wet meadows, pond and creek edges. Flowers May 10 – June 5 | Glossy round heart-shaped leaves; waxy yellow cups | Toxic raw. Leaves boiled in two waters (10 game minutes each, water changed) are safe greens: 20 kcal and +1 vitamin C a handful. Its bloom says pond edges are open (Plant signs) | Handful in 2 game minutes | — |
| Mare's tail `yearofice:mares_tail` | Ponds and sloughs, emergent in 0.2–1 m | Narrow leaves in whorls round a single stem, like a bottlebrush. Water horsetail has no leaves, only joints | Tender tips May–June raw: 10 kcal and +0.5 vitamin C a handful | Handful in 3 game minutes | Muskrat, geese; part of every push-up |
| Narrowleaf bur-reed `yearofice:bur_reed` | Pond and slough shallows | Ribbon leaves; spiky green balls on a zigzag stem | Tubers September–October: 150 kcal per 250 g, cooked | 250 g in 15 game minutes wading | Ducks eat the seeds; beavers the roots |
| Arrowhead `yearofice:arrowhead` | Soft mud bottoms of sloughs and oxbows in 0.2–0.8 m. Flowers July; tubers September – freeze-up | Arrow-shaped leaves; white 3-petal flowers in rings | "Duck potato": tubers at 20–40 cm in the mud float up when the player treads the bottom (hold right mouse, 15 game minutes, bare feet in cold water). 250 g cooked: 250 kcal. A muskrat push-up opened through the ice in October–November holds 0.5–1.5 kg of cached tubers | As listed | Muskrats cache them; ducks, swans |
| Common bladderwort `yearofice:bladderwort` | Still pond water | Floating mats of fine leaves with tiny bladders; yellow snapdragon flowers held up in July | A sign: ponds with it hold pike fry and dragonfly nymphs, the best bait source. Its bladders snap at insects | — | Fry shelter in it |
| Round-leaf sundew `yearofice:sundew` | Wet sphagnum in bogs | Tiny red rosettes of round leaves with sticky shining hairs | A sign: the inspect card counts insects stuck on it, which reads the day's bug index | — | It eats mosquitoes |
| Wild calla `yearofice:wild_calla` | Bog pools and beaver-pond margins | Glossy heart-shaped leaves; white hood in June; red berry clusters in August | Toxic raw: mouth burning, −10 Condition. The rhizome dried 14 game days, pounded and boiled gives famine flour: 100 g, 300 kcal | Rhizome 250 g in 15 game minutes wading | — |
| Pond-lily seeds (existing plant) | Yellow pond-lily ponds. Pods August–September | Green pods behind the yellow cups | Seeds `yearofice:lily_seeds`, parched at a fire 5 game minutes, pop like corn: 350 kcal per 100 g, the richest wild seed, keeping 1 year dry (Every concept from the video › Lily pads). The pods are reached by raft or wading to the chest | 100 g in 20 game minutes | Moose eat the pads; beavers the rhizomes |

## New lookalikes and poisons

### Pairs

**Every pair differs on at least two checks**, as Foraging and plant knowledge requires. A safe twin, once identified, shows its lookalike as "not Labrador tea" or the like.

| Pair | Look | Smell | Cut |
| --- | --- | --- | --- |
| Labrador tea / leatherleaf | Tea: rusty wool under rolled leaves. Leatherleaf: flat leaves with rusty scales, no wool; bells hang in a row | Tea: resinous. Leatherleaf: none | — |
| Northern geranium / monkshood | Geranium: open 5-petal pink flowers, soft-hairy leaves. Monkshood: hooded blue flowers, glossy hairless leaves | — | Geranium: red-brown woody rhizome. Monkshood: pale turnip-like tuber; the lip tingles if touched to it |
| Wild rhubarb shoots / false hellebore | Rhubarb: papery sheath at each joint, red flecks. Hellebore: pleated leaves with parallel veins, no sheath | Rhubarb: sour. Hellebore: none | Rhubarb: hollowish stem with sour juice. Hellebore: solid, dry |
| Aspen oyster / deadly galerina | Oyster: white shelves, gills running down, no ring. Galerina: small brown caps on thin stems with a ring | Oyster: faint anise. Galerina: mealy | A cap left on bark 1 game hour leaves a print: oyster white, galerina rusty brown |
| Puffball / destroying angel button | Puffball: spiny, no stem. Angel: smooth egg in a cup at the base | — | Puffball: solid white all through. Angel: the outline of cap, gills and stem inside |
| Shaggy mane / destroying angel | Mane: shaggy scaly cylinder cap, no cup at the base. Angel: smooth white cap with a ring and a cup | — | Mane: hollow stem; gills turn black. Angel: solid stem; gills stay white |
| Chanterelle / false chanterelle | Chanterelle: blunt forked ridges. False: thin crowded true gills, darker orange | Chanterelle: apricot. False: none | Chanterelle: white flesh that tears like string cheese. False: thin orange flesh |
| Ostrich fern / lady and wood fern | Ostrich: smooth stem, deep U groove. Others: scaly or hairy stems, shallow groove | — | Ostrich: U-shaped section. Others: V or flat |

**Mixed logs.** Aspen oysters and galerina fruit on the same logs. Using Strip on a log with both sweeps galerina into the basket 30% of the time unless every cluster was inspected first. The basket's card then reads "mixed mushrooms" until each piece is checked.

### Poisonings

**The messenger names a plant only if it was identified**, by the existing rule. Effects stack with Sick stomach.

| Plant | Shows after | What happens | What helps |
| --- | --- | --- | --- |
| Monkshood | 10–60 game minutes | Numb mouth and hands, slow and uneven heart: energy cap 2, −50 Condition over 4 game hours, view pulses | Charcoal in water within 30 game minutes halves it. Can kill below 55 Condition |
| Tall larkspur | 1–3 game hours | Weak muscles: walk speed ×0.6 for 12 game hours, −30 Condition | Rest. Can kill below 30 |
| False hellebore | 30–120 game minutes | Vomiting, slow heart, dizziness: −35 Condition, energy cap 2 for 12 game hours | Charcoal halves it. Can kill below 35 |
| Leatherleaf tea | 1 game hour | Nausea +30, −5 Condition | Stop drinking it |
| Arctic lupine seeds | 2 game hours | Racing heart and shaking: −20 Condition, view sway 6 game hours | Rest |
| Pasqueflower, raw marsh marigold, raw wild calla | At once | Burning mouth and blisters: −10 Condition, eating costs −1 Spirits for a day | Stop; it passes |
| Arnica eaten | 1–2 game hours | Racing heart: −20 Condition, energy cap 4 for 8 game hours | Rest |
| False chanterelle | 3 game hours | 1 day of gut illness | Rest and water |
| Destroying angel | 6–24 game hours, then a false recovery on day 2 | −60 Condition over 3 game days; energy cap 2 | Nothing in the valley. An SOS within 24 game hours of eating halves the loss. Can kill below 60 |
| Deadly galerina | 6–24 game hours | As destroying angel, −50 Condition | As destroying angel. Can kill below 50 |

**The worst bill is late.** Amatoxin mushrooms show nothing for up to a game day, then feel better on day 2. The journal writes "Something you ate yesterday" so the player has to remember the basket.

## Plant work through the year

### The tapping season

**April's sweet work.** Birch sap runs from the `sap_running` alert (window April 10 – May 5) until the birch buds burst, about 3 weeks. The existing numbers stay: 4 L a tap a day on average, 40 kcal a litre, 2 days fresh, the existing boil-down to syrup at 100:1 (40 L of sap make 0.4 L of syrup, 1,100 kcal, Blueprint › Cooking). This entry adds the tap, the daily swing, the tree and what to make.

**Making a tap.**

1. **Spile.** Carve a 15 cm tube from red-osier dogwood or alder, pushing out the soft pith: knife, 10 game minutes. Or fold a birch-bark spout: 2 game minutes, half the flow. Item `yearofice:spile`, 0.02 kg.
2. **Bucket.** A birch-bark bucket of 4 L (`yearofice:bark_bucket`, 1 bark sheet and 1 m spruce root, 20 game minutes) or any pot or can.
3. **Hole.** On a birch 20 cm across or more, on the south side 1 m up: knife or awl, 3 game minutes, 4 cm deep. One tap under 40 cm, two at 40 cm or more.
4. **Hang.** Spile in, bucket on a cord or a notch. The block `yearofice:sap_tap` sits on the log face and holds the bucket's fill.

**Flow follows the freeze and thaw.** Each day's flow per tap is the base times a weather factor from that day's low and high.

| Night low | Day high | Factor | Typical litres |
| --- | --- | --- | --- |
| −2 °C or colder | +4 °C or warmer | 1.5 | 6 |
| 0 °C or colder | +2 °C or warmer | 1.0 | 4 |
| Above 0 °C | Any | 0.3 | 1.2 |
| Any | Below 0 °C | 0 | 0; the spile freezes |

**Ice is free boiling.** Sap in a bucket freezes on a cold night. Tossing the ice skin (right mouse on the bucket, 1 game minute) leaves sap 1.3× as sweet, so it boils down in 0.77 of the time. The card says "Tossed 1 L of ice."

**The season ends in the bud.** When birch leaves are 7 days off, sap turns cloudy. Syrup from it tastes "buddy": −2 Spirits a serving. The messenger warns once: "Sap is clouding. The run is ending."

**Overflow and checking.** A 4 L bucket fills in a day on a good day. Unchecked sap overflows and is lost. Checking a line of 6 taps takes about 15 game minutes, a natural morning round.

**What to make.**

| Product | Input | Time | Result |
| --- | --- | --- | --- |
| Sap water | Sap, straight from the bucket | — | Safe unboiled water (existing) with 40 kcal a litre |
| Syrup | Sap | The existing boil-down, queueable on a Steady fire | Syrup for preserves and Spirits (existing) |
| Sap beer | 4 L sap, 1 cup berries or 100 g barley, sweetgale optional | 7 game days at 15 °C in a sealed container | 4 L, +3 Spirits a cup, once a day; no other effect |
| Sap pan | Salvaged roadhouse sheet metal, or 2 kg iron at the forge | 2 game hours | `yearofice:sap_pan`: a placed evaporator that boils 3× as fast as a pot over one armload an hour |

**The tree remembers.** A tap costs the birch 0.05 vigor for a year and the hole heals by the next April. Tapping a birch under 20 cm, or three taps on one tree, costs 0.2 and browns its crown that summer. Each tapped birch rolls 2% a year to start a chaga, so an old sugar bush grows its own tea.

**Animals.** Red squirrels bite birch twigs and lick the frozen drips, so "sapsicles" on low twigs mark a good sap tree. A black bear out of its den knocks over unguarded buckets within 200 m of camp with a 5% chance a night.

**On screen and sound.** A steady drip into bark, faster on warm afternoons; ice rings in the morning bucket; steam rolling off the sap pan. Messenger: "Sap: 22 L today from 5 taps."

**Ties.** Weather and the year table; containers; cooking and fire tending; preserves and Spirits; chaga; squirrels and bears.

### Spruce gum and pitch

**Gum is pitch the tree has hardened.** Gathering materials covers scraping pitch and making glue; Ideas from viewers covers pitch lights. This entry adds the kinds and the blaze.

| Kind | Where | Spot it | Use | Yield |
| --- | --- | --- | --- | --- |
| Gum tears | Old wounds on white spruce | Amber drops, hard outside, soft at the heart | Chewing gum: +1 Spirits once a day. Fresh soft gum sticks to the teeth: −1 Spirits | 10 g a game minute |
| Soft pitch | Fresh flows within 1 year of a wound | Clear, sticky, runny in sun | Salve (existing), seams | 20–80 g a wound |
| Hard pitch | Wounds 2+ years old; beetle trees (existing ×3) | Opaque yellow-white crust | Glue and pitch lights (existing) | 20–80 g a wound |
| Fir balsam | Resin blisters on subalpine fir bark | Smooth bark with raised blisters | Antiseptic: 5 blisters squeezed on a cut count as one pitch-salve use | 5 blisters a game minute |

**The blaze.** A hatchet face 30 × 15 cm cut into a living spruce (2 game minutes, vigor −0.1) flows 50 g of soft pitch in 30 days from May to August, and none in winter. A tree takes 3 blazes; a fourth kills it. Arvid's blazed pitch trees still stand along his trail, which is how the player finds his line.

**On screen.** A blaze shows bare yellow wood that weeps amber over 30 days. Chewing gum plays a quiet chew on the body view.

### Smoke woods

**Each wood adds a flavour and a risk to the smokehouse.** The smoker and smokehouse rules stay; the wood is a modifier on each batch.

| Wood | Smoke | Heat | Flavour | Batch effect |
| --- | --- | --- | --- | --- |
| Thinleaf or green alder | Thick, sweet | Medium | The classic salmon smoke | +2 Spirits the first meal of each batch |
| Balsam poplar, dry | Light, mild | Low | Mild | Punky poplar smoulders without flame: flare-up loss −50% |
| Willow | Light | Low, burns fast | Neutral | Fuel used ×1.3 |
| Birch | Hot, sooty | High | Sharp | Flare-up risk +50%; soot on the fish |
| Spruce | Black, resinous | High | Turpentine | −2 Spirits a meal from that batch |
| Sweetgale or juniper sprigs on the coals | Aromatic | — | Herbal | +1 Spirits, added to any wood |

**Green beats dry for cold smoke.** Green alder gives 2× the smoke and half the heat of dry, which keeps cold smoke under its temperature limit without tending. The smokehouse card shows the wood and its flavour.

### Bark by season

**Bark comes off well only in its season.** The existing once-a-tree-a-year rule for birch bark stays.

| Bark | Best window | Outside it | Yield and time | Use | Cost to the tree |
| --- | --- | --- | --- | --- | --- |
| Birch outer bark | June 1 – July 31: sheets slip off whole, Good quality | August–September: 30% of sheets tear, 2 game minutes. February–March: "winter bark" warmed by a fire peels thick, with a dark inner face for scratched designs (+1 Spirits a piece hung indoors) | 1 sheet 0.6 × 1 m a game minute in season | Canoes, roofs, baskets, buckets (existing) | Vigor −0.1; a full ring kills |
| Spruce bark sheets | May 15 – July 15 | Will not peel | 1 sheet 1 × 2 m in 10 game minutes, from a felled tree or a standing one it kills | Roofing under sod; a crude bark canoe | The tree dies if ringed |
| Alder inner bark | All year; strongest dye in spring | — | 500 g in 10 game minutes | Dye | Vigor −0.1 |
| Balsam poplar bark | Dead trees, all year | — | A float chunk in 3 game minutes | Floats, carving | — |
| Juniper bark | All year | — | A handful in 2 game minutes | Shredded tinder at dryness 70 | — |

### Root digging seasons

**Roots are best before the shoot and after the frost.** Starch moves into the root in fall and out into the shoot in spring.

| Season | Window | Root value | Why |
| --- | --- | --- | --- |
| Spring | Melt-out of the cell to its `leaf` phase | ×1.0 | Full stores, soft soil |
| Summer | `leaf` to `senescent` | ×0.6 kcal, woody, 50% more digging time | Starch is in the leaves |
| Fall | `senescent` to the day the soil freezes | ×1.1 | Full stores, sweetest after frost |
| Winter | Frozen soil | Cannot dig, except arrowhead from a muskrat push-up | The existing frozen-ground rule |

**Which roots.** Wild potato (existing windows), alpine bistort, lady and wood fern, bur-reed, arrowhead, cattail and bulrush (any open-water month, as written), buckbean, wild calla, valerian, northern geranium, dandelion (existing September), bedstraw for dye, and spruce and tamarack roots for lashing.

**Spruce roots for lashing.** In moss or sand, June–September, pull and split surface roots: 1 m in 3 game minutes for white spruce, 1 m a game minute in black spruce moss, 4 game minutes for tamarack's finer root. Boiled 10 game minutes, the bark slips off and the root splits clean. Lashing from split root rots slowly: cordage rot ×0.3.

### Drying and storing plants

**Plant food dries on the existing drying rack or indoors.** The rack is the one in Preserving in detail. A bundle can also hang from a wall hook in a heated room.

| Part | Prep | Rack, dry weather | Heated room, 15 °C or more | Dried weight | Keeps dried |
| --- | --- | --- | --- | --- | --- |
| Leaves, flowers, tips, needles | Bundled | 1 day | 1 game day | ×0.15 | 1 year |
| Mushrooms | Sliced, 1 game minute per 100 g | 2 days | 1 game day | ×0.1 | 1 year; boletes and morels +3 Spirits a meal (existing) |
| Berries | Spread on bark sheets, or made into berry leather (existing) | 3 days | 2 game days | ×0.2 | 1 year |
| Roots and tubers | Sliced 1 cm | 3 days | 2 game days | ×0.3 | 1 year; pounded into flour in the mortar |
| Seeds | Spread and turned once | 2 days | 1 game day | ×0.9 | 2 years |

Rain on an uncovered rack adds 6 hours of drying for each hour, as for meat. Dried plants keep every kcal. Their vitamin C halves (Health, medicine and nutrition), except rose hips and spruce needles, which keep it.

**Fresh plant food keeps by family.** Each plant file sets `spoil_days_at_5c` for the Spoilage model. These are the defaults; the Decay and time tab says what a spoiled one does.

| Family | Days at 5 °C | Exceptions |
| --- | --- | --- |
| Soft berries: blueberry, raspberry, soapberry, currants, strawberry | 4 | Cloudberry 3 (existing) |
| Hard berries: lingonberry, bog cranberry, highbush cranberry, crowberry | 30 | Crowberry 10 (Land and seasons in play) |
| Greens, shoots, flowers, fiddleheads | 2 | Spruce tips 3, wild chives and chickweed 4 |
| Mushrooms | 2 | Morels 3, hedgehog 3, shaggy mane 6 game hours |
| Roots and tubers | 14 | Cattail 5 (existing); potatoes follow Crop farming |

**Barrels and freezing.** Hard berries packed in water in a sealed barrel keep 180 days at 0–5 °C: the lingonberry barrel of the calendar. Below −5 °C every plant food stops decaying, by the Blueprint rule. Thawed soft berries are mush: cooked, they keep their kcal; raw, they cost −1 Spirits.

## Dyes, fibres and the herbarium

### Dyes

**Plant dye changes how a garment reads to animals and to the player.** It ties Clothing to Ideas from viewers › Camouflage: a dyed outer layer takes a class at once instead of after 30 days of wear.

**The dye vat.** `yearofice:dye_vat` is a placed 20 L tub: a wooden or bark tub heated by stone boiling (5 river stones heated 30 game minutes), or the roadhouse's salvaged washtub set on a fire. One bath dyes one garment or 4 m of cordage.

1. **Gather** the dyestuff: 500 g to 1 kg a bath (table).
2. **Mordant.** Simmer the garment 1 game hour with 200 g of dried stiff clubmoss, or add iron water (rusty iron or a bog-iron nodule soaked 3 game days) to darken. No mordant halves fastness.
3. **Dye.** Simmer 1 game hour, tended, then cool overnight in the bath.
4. **Dry** on a rack 1 game day. A wet dyed garment counts as wet under Items › Wet.

| Dyestuff | Colour | Mordant | Fastness, months in sun | Camouflage class of the layer |
| --- | --- | --- | --- | --- |
| Thinleaf alder inner bark | Orange-brown | Clubmoss | 12 | Drab |
| Alder cones with iron water | Charcoal grey | Iron | 18 | Drab |
| Labrador tea leaves | Tan | Clubmoss | 10 | Drab |
| Spruce cones or bark | Brown | None or iron | 12 | Drab |
| Dwarf birch leaves | Yellow-green | Clubmoss | 8 | Drab |
| Hawk's wing mushroom | Blue-green grey | Iron | 10 | Drab |
| Northern bedstraw roots | Red | Clubmoss | 14 | Neutral |
| Goldenrod flowers | Bright yellow | Clubmoss | 8 | Neutral |
| Sweetgale catkins | Yellow | Clubmoss | 6 | Neutral |
| Sunburst lichen | Orange | Urine (14 days) | 12 | Neutral |
| Blueberries | Purple | None | 2, then grey | Neutral, then Drab |

**Fading.** Each month in sun past its fastness, a dyed layer fades one step toward undyed. Washing (Items › Medicine and hygiene) costs one month. The tooltip reads "Alder brown, holding" or "fading".

**Why dye.** A drab-dyed parka and leggings give the Drab ×0.85 sight range from the first day. A red or yellow hat gives +1 Spirits a day, counted once. Dyed quills, babiche and cordage feed the quill band (Creative tab) and trade.

**On screen.** The vat's water takes the colour; the garment icon and body figure show it. Stone boiling hisses; the vat steams.

### Plant fibres for cordage and nets

**Each fibre suits one job.** Gathering materials' fibre and cordage rules, and the cordage quality words in Items › Quality, stay. A fibre sets the starting quality step and how it rots.

| Fibre | Source | Best season | Prep | Quality step | Rot when damp | Best for |
| --- | --- | --- | --- | --- | --- | --- |
| Nettle | Stalks after they brown, August 15 – September 30 | Late summer | Retted, broken, scutched, spun | +1 step | Existing 10% a month | Fine twine: snares, fishing line, gill nets |
| Fireweed | Stalks after the first frost (existing) | Fall | Split, dried, spun | Base | Existing | General cordage |
| Feltleaf willow inner bark | May–June (existing) | Spring | Stripped, twisted | +1 step for willow | ×1.5 | Lashing, fish traps |
| Balsam poplar inner bark | May–June | Spring | Stripped, boiled 30 game minutes | Base | ×1.5 | Basket weaving, soft ties |
| Spruce or tamarack root | June–September | Thawed soil | Boiled, split | +1 step | ×0.3 | Sewing bark, canoe lashing, sled joints |
| Bluejoint and sedge | July–August | Summer | Twisted wet | −1 step | ×2 | Thatch ties, mats, insoles |
| Bulrush | July–September | Summer | Dried 2 days, woven | Mats only | ×1.5 | Sleeping mats, bags |
| Haircap moss | All thaw months | Any | Twisted | Clumsy | Never rots | Wet-weather ties |

**Retting.** Pond retting: bundles sunk in still water 7–10 game days. Dew retting: spread on grass 14–21 game days. Retting ponds smell: −1 Spirits within 10 m. Then breaking and scutching: 10 game minutes a kg on a log with a wooden blade.

**Nets from nettle.** A 20 m gill-net segment needs about 400 m of fine twine: 0.8 kg of nettle fibre from about 800 stalks, 4 game hours of cutting and 16 of spinning and knotting. Spinning and knotting are queued jobs that run by the stove in winter. It is the plant route to the existing `gill_net_segment` without salvaged mesh.

### The herbarium

**Dr. Elin Marsh was pressing plants.** In Station Nine's back room stands her herbarium cabinet, `yearofice:herbarium_cabinet`, with 23 of 120 sheets filled and the rest labelled in her hand. Her last sheet is empty: "Aconitum, Aug 3, the meadow above the lake." It points to her last camp.

**The plant press.** `yearofice:plant_press`: 2 split boards, 4 birch-bark sheets, 2 m cordage; workbench, 30 game minutes, Bushcraft, 1.2 kg. It holds 6 specimens.

1. **Collect** a fresh plant in flower or fruit (right mouse held, 1 game minute, a knife for woody plants). A specimen item records species, date and place.
2. **Press** for 3 game days in the press. Damp weather adds a day; a press left in rain molds its specimens.
3. **Mount** at the cabinet: 5 game minutes. The sheet item `yearofice:herbarium_sheet` goes into its labelled slot.

**What a sheet gives.**

- **Knowledge.** A mounted sheet identifies its species for good, the fourth way to identify a plant. A dangerous plant can be pressed with gloves and never tasted.
- **Spirits.** +2 per new sheet, and +10 when a family row is complete (the 6 willows; every berry; every edible mushroom).
- **Reading.** A complete willow row adds "browse read": the inspect card on any willow shows last winter's moose browse in %. A complete fungus row shows the next flush date on the mushroom prompt.
- **Radio points.** Each summer Whitlock's weekly requests include 4 missing records ("Press a buckbean in flower for the series"), worth the usual 6 points each. Any other new sheet reported at the sked is 1 point, capped at 2 a week, inside the radio's 200-point credit cap. Whitlock's existing weekly plant confirmation still counts as identification.

**Elin's notes.** Each of her 23 sheets carries a one-line note, read in her voice on mounting, and three of them mark plants near where she was last seen. The journal logs "Elin's herbarium: 41 of 120".

**On screen and sound.** The cabinet's drawers fill with pale sheets; a full row gets a brass label. Pressing creaks the boards; mounting rustles paper.

## Plant signs

### The plant calendar on the ground

**Plants are the valley's clock.** The fireweed clock stays as written in Land and seasons in play › Sky signs. These signs join it under the Land reading skill: Clumsy players see the plant, Handy players get the meaning, Practiced players get the number.

| Sign | When | What it means | Holds in |
| --- | --- | --- | --- |
| Pasqueflowers on a south bluff | April 20 – May 15 | Green-up in 8–12 days | 80% of years |
| Coltsfoot heads at a seep | April 25 – May 20 | Meltwater is running; breakup within 10 days | 70% |
| Willow catkins silver, then yellow with pollen | April 20 – May 15 | Silver: sap still running. Yellow: sap ends within 7 days; ducks are back | 75% |
| Leatherleaf bells in the bog | May 5–25 | The bog mat has thawed 20 cm: walkable, and cloudberries flower within 10 days | 80% |
| Marsh marigold at the pond edge | May 10 – June 5 | Pond edges open; frogs calling within 3 days | 85% |
| Birch leaves the size of a squirrel's ear | Leaves-out plus 3 days | Soil is near 8 °C: sow potatoes now, germination factor 0.7 to 1.0 within days | 75% |
| Poplar fluff on the water | June 10–25 | King salmon in within 5 days; mosquitoes at their peak | 70% |
| Dryas flowers all facing one way | Sunny days in June | The flowers face the sun: at 12:00 they point south | Always on clear days |
| Alpine bearberry scarlet on the ridge | August 20 – September 5 | Frost has come to the mountain; the valley floor follows in 7–14 days | 70% |
| Highbush cranberry's musty smell in the woods | Mid-September | Leaves are down within 7 days | 75% |
| Tamarack gold in the muskeg | September 15 – October 5 | Freeze-up in 25–35 days | 70% |

**Reading the ground's age.** Four plants tell how long since the land was disturbed. The Read key reports them.

| Plant | Reads | Range |
| --- | --- | --- |
| Fire moss without fireweed | Burned last year | 1 year |
| Dwarf fireweed on a gravel bar; then willow seedlings; then alder | Age of the bar since the last flood moved it | 2–6, 6–15, 15+ years |
| Stair-step moss steps | Years since fire or felling | Up to 15 |
| Map lichen size | Age of a rock surface | 20–200 years |

**Small daily signs.** Swollen orange jelly fungus means rain in the last 24 hours, so deadwood on that log is wet. The sundew's catch reads the bug index. Sunburst lichen marks perches and dens. Sapsicles mark a sweet birch.

**On screen.** Each sign has a messenger line the first time it is read each year, such as "Pasqueflowers on the bluff. Green-up in about 10 days." The journal's phenology list (Open world in depth › The radio) gains these firsts.

### Flowers indoors

**A bouquet in the cabin.** Any 6 flowering stems in a can, jar or birch-bark cup make a placed `yearofice:bouquet`: +1 Spirits a day while fresh (4 game days), counted once. Fireweed, bluebells, Jacob's ladder, lupine, geranium and goldenrod all count. A bouquet of monkshood counts too, and a goat that reaches it eats it.

## Plant medicine

### Remedies and doses

**Every plant remedy has a dose, a limit and a risk.** The existing remedies (pitch salve, willow-bark tea, yarrow, usnea, chaga, Labrador tea) stay. A cup is 0.25 L under the tea rule (+5 warmth, +8 water).

| Remedy | Plant and part | Prep | Dose | Effect | Limit and risk |
| --- | --- | --- | --- | --- | --- |
| Poplar bud salve | Balsam poplar buds, 50 g, with 50 g lard | Steeped warm 2 game hours | One use on a cut, burn or blister; a tin holds 5 | Antiseptic as pitch salve; blisters heal in 8 game hours instead of a day | None |
| Wormwood tea | Tilesius wormwood leaves | Steeped 5 game minutes | 1 cup a day for 7 days | Clears parasites from raw bear meat: absorption back to 100% on day 7 | Over 2 cups a day, or over 10 days in a row: 5% a day of a seizure (as water hemlock's fall, −10 Condition) |
| Wormwood on banya stones | Fresh leaves | Laid on the stones | One banya | Pain penalties of sprains −50% for 6 game hours after | None |
| Geranium root tea | Northern geranium root | Boiled 10 game minutes | 1 cup up to 3 times a day | Gut illness: water loss halved, illness ends 12 game hours sooner | A 4th cup: −1 Spirits (cramps) |
| Mint tea | Field mint leaves | Steeped 5 game minutes | 1 cup | Nausea −30 (Sick stomach); +1 Spirits | Counts once a day for Spirits |
| Valerian tea | Valerian root | Steeped 10 game minutes | 1 cup at bedtime | Falls asleep 45 game minutes sooner; sleep recovery +10% | Energy cap 8 until 08:00 next day; after 7 nights in a row the effect halves |
| Roseroot tea | Roseroot root | Boiled 10 game minutes | 1 cup a day | Energy cost of work −10% for 6 game hours | A second cup: falls asleep 60 game minutes later that night |
| Spirea flower tea | Beauverd spirea flowers | Steeped 5 game minutes | 1 cup | Pain penalties −25% for 2 game hours | Counts once with willow-bark tea; they do not stack |
| Arnica salve | 20 arnica heads in 50 g lard | Steeped warm 2 game hours | Rubbed on twice a day | Sprains and bruises heal 25% faster | On an open cut: −5 Condition. Never eaten |
| Coltsfoot tea | Dried coltsfoot leaves | Steeped 5 game minutes | 1 cup a day | Cancels the cold cough's −1 Spirits for that day | 7 or more days in a row: −10 Condition (liver) |
| Iceland moss tea | Iceland moss, soaked | Boiled 10 game minutes | 1 cup | As coltsfoot tea, with no limit | Bitter unless soaked: −1 Spirits |
| Buckbean tea | Buckbean leaves | Steeped 10 game minutes | 1 cup a day | Gut illness 6 game hours shorter; appetite back (nausea −20) | A 2nd cup: vomiting, nausea +40 |
| Sphagnum dressing | Dried sphagnum | — | One dressing | Its own pad: clean for 24 game hours; stops bleeding in 5 game minutes, at half the rate meanwhile | Wet sphagnum does not count |
| Puffball powder | Old puffball spores | Dried | A pinch on a cut | Stops bleeding at once, as yarrow | Not clean: infection roll as an unclean dressing |
| Clubmoss powder | Stiff clubmoss spores | Dried | A pinch | A blister or rash heals 25% faster | None |
| Tamarack poultice | Tamarack inner bark | Pounded, warmed | Once a day on a burn | Burns heal 25% faster | None |
| Juniper tea | 5 juniper berries | Steeped 5 game minutes | 1 cup a day | +1 Spirits | A second cup the same day: −5 Condition |
| Fir balsam | Subalpine fir blisters | Squeezed on | 5 blisters | One antiseptic use, as pitch salve | None |

**The cold cough.** New with this tab: 2 game hours of hard work in air below −30 °C bring a dry cough, −1 Spirits a day for 3 game days. Coltsfoot and Iceland moss tea cancel it day by day.

**Remedies in data.** Each row is a file in `data/yearofice/remedy/`, the existing folder, with `dose_per_day`, `course_days` and a `risk` block.

**The medicine card.** Holding a remedy shows today's count: "Wormwood: day 4 of 7, 1 cup today." The journal logs a course so the player can see whether it worked.

## The tasting test

### What a bite can and cannot tell

**The existing taste test stays.** One small bite, one game hour, never fatal, identifies a safe plant. This entry adds a staged version and two hard rules.

**The staged test.** For a careful player, the card offers four steps instead of one bite. Each step stops early if the plant reacts, for a smaller cost.

| Step | Time | Catches | Cost if it reacts |
| --- | --- | --- | --- |
| Skin: rub on the wrist | 15 game minutes | Blistering sap: pasqueflower, marsh marigold, wild calla, arnica | −2 Condition, red wrist 6 game hours |
| Lip | 5 game minutes | Burning and numbing: monkshood, larkspur | −3 Condition |
| Tongue, held 15 game minutes, spat out | 15 game minutes | Bitter alkaloids: false hellebore, lupine seeds, water hemlock | −5 Condition |
| Small bite, swallowed | 1 game hour | Everything else the existing test catches | The existing −10, or −25 for water hemlock and death camas |

A plant that passes all four is identified, as now. The staged test costs 35 game minutes more and usually catches a toxic plant at a quarter of the cost.

**Hard rule one: no mushroom takes the test.** On any fungus the option reads "No bite tells a deadly mushroom. Learn it by Look and Cut." Amatoxins show nothing for a day, so a bite would teach the wrong lesson.

**Hard rule two: a lookalike group shares a warning.** A plant whose lookalike has hurt the player shows "Something like this made you sick" until it is identified, by the existing poison-pax memory rule.

**On screen.** Each step plays its own small animation on the body view: the wrist, the lip, a spit. The messenger timer counts down the step.

## How the plant cover changes over years

### The cover pass

**Plant cover is redrawn once a year, on May 1.** `CoverPass` runs with the existing stand-age advance (Land and seasons in play › Stand age). It reads each 8 m cell's cover, the year's pressures, and writes plant blocks for the new season. Unloaded cells update in closed form on load.

**Four forces reshape the cover.** Each has a number the player can read and change.

| Force | What it does to plants | Number | Readable as |
| --- | --- | --- | --- |
| Fire | Resets stand age to 0 (existing); then fire moss year 1, fireweed and morels years 1–3, raspberry and strawberry years 2–10, aspen and willow suckers years 2–25 | Bluejoint mats slow tree return by 5 years where they take over | Black snags, purple fire moss, a fireweed sea |
| Beavers | Fell aspen, poplar and willow within 100 m of their pond; flood spruce to deadwood; when they leave, the pond drains to sedge and bluejoint meadow (existing) | Within 100 m, aspen cover falls 15% a year while the colony holds | Pencil stumps; a grey ring of drowned spruce |
| Moose browse | Each willow, aspen and birch stem under 3 m loses current growth to browse. Over 50% browse 3 winters running brooms it: catkins and seed ×0.5, the stem stays under 3 m | Browse share per cell = moose-days × 0.25 ÷ browse stems, capped at 0.9 | Broomed willows; a fenced plot grows 2 m taller in 5 years |
| The player | Clearing, felling, coppicing, burning, trails, yards, gardens, planting | Every verb above writes to the same ledger | The map's cover layer; the year card |

**Moose and the player make each other.** A clearing grows a willow and aspen thicket by year 6 (existing succession), which doubles moose capacity from year 8. More moose then broom that thicket within 5 years unless wolves, the player's hunting or a fence hold them down. A player who hunts the clearing keeps the browse and the moose both.

**The exclosure.** A pole fence 10 × 10 m (Structures and buildables) round young willow shows the browse effect in 3 summers: the willows inside stand 2 m taller and bloom. It is a garden for poles, fish-trap shoots and ptarmigan bait, and the clearest lesson in the game about grazing.

**The player's yard.** Around any camp used 30+ days a year, worn cells grow plantain, pineapple weed, dandelion, chickweed, foxtail barley, shaggy mane and lambsquarters under the existing worn-trail rules. A five-year homestead is ringed by people plants that a first-day camp does not have. Pulling weeds near the cabin is never required.

**Coppice rotation.** A stand of willow or alder cut in 3 blocks, one a year, gives poles and smoke wood for ever. The journal suggests it once, after the 50th stem cut: "Cut in thirds and the willows never run out."

### The year card

**The year card adds a cover line.** It counts what changed: "This year: 2 ha cleared, 140 willows coppiced, 1 burn of 0.4 ha, 18 spruce planted, 6 patches at good health."

## Foraging calendar

**The year starts May 1, as every run does.** Bold marks the month's main calorie source from plants. Dates shift by up to 10 days with the summer's degree-days.

| Month | Food and drink | Medicine, craft and fuel | Signs | Plant work |
| --- | --- | --- | --- | --- |
| May | **Birch sap to early May**; crowberry and bog cranberry left on the vine; spruce tips; fireweed, coltsfoot and cottongrass shoots; wild potato roots; fiddleheads from May 20; diamondleaf willow buds; aspen oysters | Poplar buds; spring inner bark from May 15; willow fibre; spruce bark sheets from May 15 | Pasqueflowers, coltsfoot, willow catkins, leatherleaf bells, marsh marigold, squirrel-ear birch leaves | Controlled burns May 1–25 (existing); root division; willow cuttings; sow wild seed; transplant seedlings |
| June | **Wild chives, sourdock, wild rhubarb, chickweed, bluebells, diamondleaf leaves**; mountain sorrel and roseroot up high; wild strawberries from June 25 | Birch bark peels whole from June 1; yarrow, cinquefoil and spirea teas; sweetgrass; spruce roots | Poplar fluff on the water; dryas facing the sun | Press spring flowers; blaze pitch trees; pick fireweed leaf for fermenting from June 15 |
| July | **Cloudberries, soapberries, wild strawberries, raspberries from July 20, currants from July 15, nagoonberries** | Bluejoint hay July 1–31; arnica, valerian, geranium flowers; cottongrass cotton | The fireweed clock starts (existing); mushroom weather after rain | Hay; dye with bedstraw and alder; ret nettle later; whip soapberries |
| August | **Blueberries, bog blueberries, crowberries, black currants, bunchberries; boletes, hedgehogs, chanterelles; lambsquarters** | Nettle stalks from August 15; goldenrod and hawk's wing dyes; birch polypore | Alpine bearberry scarlet; bears at the peak patches (existing) | Rake the low bushes; berry leather; dry mushrooms; press fruiting specimens |
| September | **Lingonberries, highbush cranberries, rose hips, bog cranberries; arrowhead and bur-reed tubers; pond-lily seeds; wild potato and fern roots**; lambsquarters seed | Nettle and fireweed fibre; dandelion roots; spruce cones | Highbush cranberry smell; tamarack gold from September 15 | Fall root digging; lingonberry barrels; seed collecting |
| October | Lingonberries and rose hips on the bush; last roots before the soil freezes; muskrat push-ups for arrowhead | Coppice season opens; dye vat by the stove | Snow that stays (existing) | Cut coppice stools; retting done; spin twine indoors |
| November | Frozen berries from the cache; rose hips; lingonberries under snow | Chaga, tinder conk and amadou; spruce gum tears | Plants go to `buried` | Twine and net knotting; herbarium mounting |
| December | Stored food; famine lichens if the cache runs low | Usnea, rock tripe and Iceland moss for the medicine shelf | — | Indoor jobs: baskets, nets, dye |
| January | Stored food; fermented sourdock and willow leaves for vitamin C | Fir balsam; spruce gum | — | Plan the garden; carve diamond willow |
| February | Stored food; rose hips | Winter birch bark from fires; coppice | — | Peel winter bark; cut poles |
| March | Stored food; crowberries on bare south ridges | Pitch from beetle trees | Snow off the south slopes (existing) | Carve spiles; make bark buckets |
| April | **Birch sap from the alert (April 10 – May 5)**; crowberries at melt-out; poplar buds | Spiles, buckets, sap pan | Sap running; sapsicles; pasqueflowers from April 20 | Tap the sugar bush; boil syrup; brew sap beer |

**Winter is the plant gap.** From November to March only stored plants, rose hips, frozen or water-kept berries, fermented greens and famine lichens give plant food. That gap is why every summer system above ends in a way to keep something.

**The garden runs on the same clock.** Plant phases count degree-days above 5 °C from the cell's melt-out; crops count the same sum from the day they are sown, ×1.2 in June and July for the midnight sun. A normal year gives about 120 degree-days in May, 315 in June, 370 in July, 280 in August and 90 in September, so soapberries ripen near July 1 and the fireweed clock reaches the top ring near August 15. Sowing follows the same signs: turnips from May 15, barley from May 20, potatoes from May 25 when birch leaves are the size of a squirrel's ear; harvest runs from early August (barley, turnips) to the first −5 °C night (potatoes). The dates and numbers are in Blueprint › Crop farming and Land and seasons in play › Crop farming in play.

## Data and build

### Registry ids

**Every new species is one block.** Food and material parts are items named after the block's species, such as `yearofice:soapberry` (fruit) from `yearofice:soapberry_bush`.

| Kind | Ids |
| --- | --- |
| Plant blocks | The 87 ids in the catalogue tables above (Trees through Aquatic plants), plus `yearofice:shrub_stem` for tall-shrub stems. All use the `yearofice:` namespace |
| Placed blocks and stations | `sap_tap`, `sap_pan`, `dye_vat`, `herbarium_cabinet`, `bouquet` |
| Items | `spile`, `bark_bucket`, `plant_press`, `plant_specimen`, `herbarium_sheet`, `lily_seeds`, the fruit and part items named after each species (`soapberry`, `nagoonberry`, `crowberry`), `wild_seed` (species component), `plant_clump` (species component), `willow_cutting`, `amadou`, `poplar_bud_salve`, `arnica_salve`, `coltsfoot_salt`, `nettle_twine`, `retted_fibre`, `bulrush_mat`, `shoe_grass`, `grass_insoles`, `sweetgrass_braid`, `fermented_willow_leaves`, `sap_beer`, `soapberry_foam`, `diamond_willow_stave`, `iron_water`, `dye_bath` (colour component) |
| Data components | `yearofice:dye` (colour, fastness months, applied day); `yearofice:specimen` (species, date, place) |
| Block states | `phase` (8 values) and `taken` (0–3) on every plant; `age` on mushrooms; `size` on brackets; `fill_l` on `sap_tap` |
| Tags | `yearofice:berry_bushes` (no item, no seed, no cutting), `yearofice:transplantable_plants`, `yearofice:coppice_plants`, `yearofice:browse_plants`, `yearofice:smoke_woods`, `yearofice:dyestuffs`, `yearofice:mordants`, `yearofice:no_taste_test` (all fungi), `yearofice:skin_toxic`, `yearofice:goat_toxic` |
| Chunk attachments | `yearofice:patches` (patch id, species, S, H, p, last day); tree vigor joins the existing tree record |
| Particles and sounds | `yearofice:plant_fluff`, `yearofice:spore_puff`; `yearofice:plant.pick`, `plant.strip`, `plant.peel`, `plant.dig`, `plant.coppice`, `plant.uproot`, `plant.sap_drip`, `plant.press_creak` |

### Data-pack paths

- `data/yearofice/plant/<id>.json`: one file per species, the 87 new rows and the existing ones.
- `data/yearofice/remedy/<id>.json`: the 18 plant remedies.
- `data/yearofice/plant_sign/<id>.json`: the 11 calendar signs and 4 age readers.
- `data/yearofice/dye/<id>.json`: dyestuff, colour, mordant and fastness.
- `data/yearofice/recipe/`: `spile`, `bark_bucket`, `sap_pan`, `plant_press`, `dye_vat`, `amadou`, `poplar_bud_salve`, `arnica_salve`, `nettle_twine`, `bulrush_mat`, `soapberry_foam`, `sap_beer`, `fermented_willow_leaves`.
- `data/yearofice/loot_table/blocks/<id>.json`: what each verb drops, by `phase` and `taken`.
- `data/yearofice/worldgen/placed_feature/<id>.json`: placement by cover, stand age and micro-climate; a biome modifier per biome.
- `data/yearofice/data_maps/item/food.json`, `weight.json`: kcal, macros, vitamin C and weight for each plant item.

### JSON shape

```json
{
  "id": "yearofice:soapberry_bush",
  "form": "berry_shrub",
  "tags": ["yearofice:berry_bushes"],
  "habitat": ["spruce_taiga_open", "gravel_terrace", "south_bluff"],
  "phases_dd": { "bud": 0, "leaf": 40, "flower": 90, "fruit": 420, "seed": 640, "senescent": "first_frost" },
  "verbs": {
    "pick": { "part": "yearofice:soapberry", "unit": "cup", "time_min": 20, "units_per_clump": 1.3, "stripped_weight": 1.0 },
    "strip": { "allowed": false, "reason": "berries_burst" },
    "uproot": { "drops": "minecraft:stick", "empty_years": 3 }
  },
  "food": { "kcal": 50, "carb_fat_protein_pct": [88, 4, 8], "vit_c": 1.5, "raw_spirits": -1, "max_cups_per_day": 2, "spoil_days_at_5c": 4, "dried_weight_mult": 0.2 },
  "patch": { "clumps": [20, 60], "regrow_days": [5, 8], "flush_mults": [1.0], "bear_weight": 1.5, "bear_months": [7] },
  "identify": { "pocket_guide": false, "logbook": true, "stages": ["leaf", "flower", "fruit"] },
  "inspect": { "look": "silver_rusty_underside_red_translucent", "smell": "bitter", "cut": "none" },
  "lookalike_group": "red_berry_trio",
  "herbarium_row": "berries"
}
```

### Java classes

| Class | Owns |
| --- | --- |
| `com.yearofice.forage.PlantBlock`, `BerryBushBlock`, `ShrubStoolBlock`, `MushroomBlock`, `BracketBlock`, `AquaticPlantBlock` | Block classes with `phase` and `taken`; no `BlockItem` for berry bushes |
| `com.yearofice.forage.PlantPhasePass` | Phases from degree-days and micro-climate on `ClockEvent.Day`; closed-form catch-up on chunk load |
| `com.yearofice.forage.HarvestVerbs` | The eight verbs, tool checks, glove costs, loot by state |
| `com.yearofice.forage.PatchLedger` | The `patches` attachment, H and p, the stripped rule, tending |
| `com.yearofice.forage.Propagation` | Seeds, cuttings, root division, transplants, the aspen-oyster log |
| `com.yearofice.forage.SapTapBlockEntity` | Flow by the day's low and high, ice tossing, buddy sap, overflow |
| `com.yearofice.forage.Browse` | Animal feeding writes: browse share, brooming, girdling, bear digging, seed spread from scat and perches |
| `com.yearofice.forage.TasteTest` | The staged test and the no-fungus rule |
| `com.yearofice.body.PlantRemedies` | Courses, daily counts and risks for the remedy files |
| `com.yearofice.build.DyeVatBlockEntity` | Baths, mordants, fading, the camouflage class |
| `com.yearofice.forage.Herbarium` (the forage package holds the herbarium in the lead's map) | Elin's cabinet, sheets, rows, the radio's missing records |
| `com.yearofice.story.reading.PlantSigns` | The calendar signs and age readers, through the existing Read key |
| `com.yearofice.forage.CoverPass` | The May 1 cover redraw with fire, beavers, browse and the player |

### Milestones

| Milestone | Ships |
| --- | --- |
| M5 — Food and goals | Plant blocks with `phase` and `taken`; the eight verbs; the patch ledger; the 87 plant rows; new lookalike pairs and poisonings; the staged taste test; plant remedies; the plant press and herbarium |
| M7 — The year | Degree-day phases and snow burial; the tapping season and sap pan; plant signs; bark and root seasons; the foraging calendar on the Trail Messenger |
| M8 — Building and outposts | Dye vat and dyes; fibres, retting and nettle nets; smoke woods in the smokehouse; bouquets |
| M9 — Homestead and beyond | Propagation and the wild herb bed; coppice rotation; browse, brooming and the exclosure; seed spread by bears and birds; the cover pass |

### Debug commands and tuning

**Every plant system has a `/yoi plant` command**, op level 2, registered by `com.yearofice.forage.ForageCommands` on `RegisterCommandsEvent`, for testing on Ruhan's own worlds. Each acts on the block under the crosshair unless a position is given, and prints the record it read or changed.

| Command | Does |
| --- | --- |
| `/yoi plant info` | Species, `phase`, `taken`, `ripe`, the cell's degree-day sum and micro-climate offset, the patch's S, H, p, units left and last harvest day |
| `/yoi plant phase <phase> [patch]` | Sets the clump's phase, or every clump of its patch, and re-meshes |
| `/yoi plant dd <sum>` | Sets the chunk's degree-day sum and runs `PlantPhasePass` on loaded chunks |
| `/yoi plant patch reset`, `strip`, `health <0.2–1.3>` | Rewrites the `patches` ledger entry |
| `/yoi plant identify <species \| all \| none> [player]` | Sets the notebook entry: Unknown, Identified |
| `/yoi plant flush <species> [days]` | Starts a mushroom flush on the cell's logs and duff |
| `/yoi plant year <berry_index \| cone_crop> <value>` | Overrides this year's factor until the next May 1 |
| `/yoi plant poison <species> [player]` | Applies the plant's meal effect at once, for timing tests |
| `/yoi plant sap <low_c> <high_c>` | Runs one sap day on the tap under the crosshair and prints litres and ice |
| `/yoi plant herbarium <species>` | Adds a mounted sheet to the nearest cabinet |

**All plant tuning lives in `yearofice-common.toml` under `[forage]`, with the three presets.** The plant files hold each species' numbers; the toml holds the dials that scale them. The realism preset chosen at world creation (Minecraft development › World creation options and presets) sets every dial at once, and a dial set by hand marks the preset "Custom", as for `[body]`. GameTests run on Standard.

| Key | Gentle | Standard | Brutal | Unit |
| --- | --- | --- | --- | --- |
| `forage.yield_scale` | 1.3 | 1.0 | 0.8 | × units per clump |
| `forage.work_time_scale` | 0.75 | 1.0 | 1.0 | × game minutes per action |
| `forage.regrow_scale` | 0.75 | 1.0 | 1.25 | × regrow and refruit days |
| `forage.stripped_threshold` | 0.85 | 0.7 | 0.6 | Picked share that halves next year |
| `forage.poison_scale` | 0.5 | 1.0 | 1.0 | × Condition lost to plant poisons |
| `forage.window_jitter_days` | 10 | 10 | 14 | Most a year can shift a ripeness window |
| `forage.sap_scale` | 1.2 | 1.0 | 0.9 | × litres a tap a day |
| `forage.spoil_scale` | 1.25 | 1.0 | 0.85 | × `spoil_days_at_5c` of plant food |

**Day length.** Every time in this tab is in game minutes and every rate is per game minute or game day, so the world's day-length option (24–72 real minutes, default 24, stored in `yearofice_world`) rescales real time with no data change. At the default, 1 game minute is 1 real second outside time-lapse; hold-to-work jobs run at the time-lapse rate of Minecraft development › Hold-to-work and the use key.

### GameTests

1. **`yearofice:forage/berry_bush_no_item`** (batch `m5`). Breaking each block in `yearofice:berry_bushes` 100 times drops 0 items of itself and 0 seeds. The cell stays empty for 3 simulated years, then regrows only if the patch survives.
2. **`yearofice:forage/patch_recovery`** (batch `m5`). In neutral years (year factor 1.0), a patch picked to 75% yields 45–55% of S the next year, 70–80% the year after, and 95–105% in year 3 with no picking.
3. **`yearofice:forage/sap_flow`** (batch `m7`). A 30 cm birch with one tap gives 5.5–6.5 L on a −5 °C night and +6 °C day, 3.5–4.5 L on 0 °C and +3 °C, and under 1.5 L after a +1 °C night. Tossing the ice cuts the boil time to 0.77 ±0.02.
4. **`yearofice:forage/coppice_shoots`** (batch `m9`). A feltleaf willow stool cut on October 15 shows 6–12 shoots on June 15 and poles of 3–4 m by the third May 1; a birch stool cut twice dies in 45–55% of 100 trials.
5. **`yearofice:forage/no_mushroom_taste`** (batch `m5`). The taste option is absent on every block in `yearofice:no_taste_test`. A bot that eats a destroying angel shows no effect for 6–24 game hours, then loses 60 ±2 Condition over 3 game days.
6. **`yearofice:forage/browse_exclosure`** (batch `m9`). In a cell of 8 feltleaf willow stems with 20 moose-days a winter (browse share 0.63), unfenced willow stays under 3 m after 5 simulated years and fenced willow in the same cell reaches 4.5–5.5 m.

7) **`yearofice:forage/units_per_action`** (batch `m5`). A sourdock clump picked 3 times gives 3 handfuls in 9 game minutes and ends at `taken` 3; a fourth pick gives nothing.
8) **`yearofice:forage/spruce_tips_window`** (batch `m7`). A white spruce gives no tips below 60 degree-days and 1 handful a pick from 60 to 330.
9) **`yearofice:forage/drying_rack`** (batch `m5`). 1 kg of sliced king bolete on a rack in 2 dry days weighs 100 ±5 g and holds 300 ±1 kcal; each uncovered hour of rain adds 6.0 ±0.1 hours.
10) **`yearofice:forage/goat_toxic`** (batch `m9`). Over 1,000 pen-days with one monkshood clump, a goat eats it on 9–11% of days; in a weeded pen, on none.

**Review audit (Plants: find to regrowth; Farming: clearing to the second year).** Reviewer B, October 19. Each step as the player does it in Minecraft, with the rule's home. Status: Ready, Fixed in this round, or Open decision for Ruhan.

| Step | Keys and screen | Rule, number and test | Rule location | Status |
| --- | --- | --- | --- | --- |
| Find a plant | Walk; the crosshair names an identified plant at 10 m, an unknown one "plant" | One clump a block, 20–60 clumps a patch; placed by cover, stand age and micro-climate | Plants and fungi › Plants as blocks; Data-pack paths | Ready |
| Read its season | Inspect card phase line; `berries_known` and `spruce_tips` prompts | 8 phases from degree-days above 5 °C; spruce tips 60–330 dd; test `forage/spruce_tips_window` | Plants and fungi › Phases through the year; Blueprint › Foraging and plant knowledge | Fixed in this round |
| Inspect | Right-mouse tap opens `InspectCard`: Look, Smell, Cut, 1–2 s each | Every lookalike pair differs on 2 checks or more (16 pairs: 5 Blueprint, 8 here, 3 in Land) | Blueprint › Foraging; Plants and fungi › Pairs | Fixed in this round |
| Identify | Notebook entry Unknown, Partly known, Identified | Pocket guide 12; logbook pages; 3 stages; taste test (never fungi, tag `no_taste_test`); herbarium sheet, the fourth way | Blueprint › Foraging; Plants and fungi › The herbarium, The tasting test | Fixed in this round |
| Harvest | Right mouse held: Pick, Strip, Peel, Cut, Dig, Tap, Coppice, Uproot by tool and phase | `units_per_clump` per flush; default times for rows without one; gloves bill bare hands; test `forage/units_per_action` | Plants and fungi › Harvest verbs | Fixed in this round |
| Eat from the hand | R (quick eat) while picking | One portion; raw costs as each row says | Blueprint › Foraging | Fixed in this round |
| Patch health | Patch card "60% picked", "Health: good" | H 0.2–1.3; picked above 0.7 sets H 0.5; test `forage/patch_recovery` | Plants and fungi › Regrowth and patch health | Ready |
| Carry | Bark basket 16 cups; berry rake Strip ×0.4 time | Rake counts 1.5 toward stripped | Blueprint › Foraging; Ideas from viewers › Berry country | Ready |
| Process | Drying rack, wall hook, pot, dye vat, plant press, retting pond | Drying table (leaves 1 day ×0.15 … seeds ×0.9); teas 0.25 L, 5–10 game minutes; 18 remedies; nettle twine; dyes and mordants; syrup 100:1 | Plants and fungi › Drying and storing plants, Remedies and doses, Plant fibres, Dyes; Blueprint › Cooking | Fixed in this round |
| Store and decay | Item tooltip with `yearofice:fresh` | `spoil_days_at_5c` by family (soft berries 4, hard berries 30, greens 2, mushrooms 2, roots 14); water barrel 180 days at 0–5 °C; no decay below −5 °C | Plants and fungi › Drying and storing plants; Decay and time | Fixed in this round |
| Poison | Delayed bill; the Trail Messenger names only identified plants | 15 toxic plants and fungi; amatoxins 6–24 h, false recovery on day 2 | Plants and fungi › Poisonings; Body, injury and sickness › Sickness register | Ready |
| Regrow | Bare stems, then fruit again | Refruit 5–8 days (count in the `patches` ledger, not a block state); flushes 1.0, 0.6, 0.36; herbs 50% in 14 days; cover pass May 1 (`forage.CoverPass`) | Plants and fungi › Regrowth; Every concept from the video › Berry patches | Fixed in this round |
| Propagate | Seed, cutting, root division, transplant; never a berry bush | Success 30–80% by method and month; test `forage/berry_bush_no_item` | Plants and fungi › Propagation | Ready |
| Debug and tune | `/yoi plant …`; `[forage]` in `yearofice-common.toml` | 10 commands; 8 dials × Gentle, Standard, Brutal | Plants and fungi › Debug commands and tuning | Fixed in this round |

| Step | Keys and screen | Rule, number and test | Rule location | Status |
| --- | --- | --- | --- | --- |
| Get seed | Crate packet; cache jar; supply drops | 12 seed potatoes and 200 g barley (10 blocks); wheat jar 100 g (5 blocks); turnip and cabbage from drops | Blueprint › Crop farming; Biomes and resources › Trapper caches | Fixed in this round |
| Clear ground | Right mouse held on stump, moss, soil | Stump 20 game minutes (Pulaski 10); strip 15 (hoe 7); till 6 (hoe 3); 10 m² with 2 stumps about 4 game hours, 2.3 with a hoe | Land and seasons in play › Clearing and soil | Fixed in this round |
| Read the bed | Shift on a bed opens the crouch card | Soil class ×0.6–1.2; T\_soil = 7-day air mean + bed offset; expected germination for the seed in hand | Land › Soil temperature and germination | Fixed in this round |
| Sow | Right mouse held with seed, 1 game minute a block | Windows May 15 – July 1 by crop; `garden_window_closed` prompt; late sowing allowed, the clock decides | Blueprint › Crop farming; Land › What a roll means | Fixed in this round |
| Germinate | Sprouts in 7–14 days | P = g × f\_soil × f\_moisture × f\_depth × f\_age; test `food/germination_soil` | Blueprint › Crop farming; Land › Soil temperature and germination | Fixed in this round |
| Grow | Crop stage 0–5 on the card, "ripe in about 12 days" | Growth units above 5 °C, ×1.2 June–July; 7 dry days add none; test `food/crop_clock` | Land › Growth units and the crop clock; Plants and fungi › Foraging calendar | Fixed in this round |
| Water, weed, feed | Right mouse held with a pot, bucket or water bag; hoe or hand | 1 L a block; weeds 3% a week; ash, manure, fish guts to +30%; worn beds −10% a year; `garden_dry` prompt | Land › Water, weeds and feeding | Fixed in this round |
| Frost | Trail Messenger warning the evening before | Last spring frost May 20 – June 10, first fall frost August 20 – September 15; boughs +2 °C; −5 °C ends the season; test `food/head_frost` | Land › Frost; Blueprint › Crop farming | Fixed in this round |
| Pests | Harvest note per bed | Hares 2% of sprouted blocks a day (×3 in a peak), grouse 1%, moose 10% of nights, voles, bears, loose stock; test `food/hare_fence` | Land › Pests and animals | Fixed in this round |
| Harvest and thresh | Right mouse held; flail on a sheaf; wind 2–8 m/s to winnow | Potatoes 3 game minutes a hill; grain 2 a block, threshing 10 a sheaf; cure potatoes 7 days | Land › Harvest, curing and threshing | Fixed in this round |
| Mill | Hand quern 1 kg an hour; grist mill | Flour for bread and porridge (barley porridge 700 kcal) | Blueprint › Cooking; Structures and buildables | Ready (quern id not yet in the master inventory) |
| Store | Root cellar, clamp, barrel | Potatoes 6 months at 2–4 °C; grain 2 years dry; turnips 4 months | Land › Storage; Decay and time | Fixed in this round |
| Save seed, year 2 | Knife on a sprouted potato; dried grain | Potato pieces 85%, saved grain 80%, packet 60%; biennials give seed in year 2; keep 20–30% to triple | Land › Seed saving and the second year; Blueprint › Crop farming | Fixed in this round |
| Debug and tune | `/yoi crop …`; `[garden]` | 8 commands; 6 dials × presets; test `food/garden_caught_up` | Land › Data and build for crops | Fixed in this round |
| Preset values | — | The Gentle and Brutal columns for `[forage]`, `[garden]` and `[land]` | Plants and fungi; Land and seasons in play | Decided by the Director (RECONCILE 129): accepted as tuning |

**The plant year, walked.** May: sap ends about May 5, then wild potato and cattail roots, shoots and the first greens; tips from about May 25; sowing May 15 – June 10. June: greens peak, the garden is weeded and watered. July: soapberries, cloudberries, strawberries, currants, bluejoint hay, the first turnips. August: the berry peak and boletes, barley and turnips in, every frost night guarded. September: lingonberries, rose hips, cranberries, roots and lily seeds, potatoes dug before the −5 °C night. October: last roots, coppice, retting. November–March: stored food, rose hips, needle tea, fermented greens and famine lichens, with indoor twine, nets, baskets and herbarium work. April: tapping. No month is empty and none is free: the spring roots that looked like a free lunch run 900 kcal an hour only at the clump, and a patch is dug out in hours.

**Forge Labs rows in this area.** 38 and 82 (seasons, germination, seed cap): Blueprint › Crop farming and Land › Crop farming in play, Ready. 66 (tips not ready): Fixed. 120 (blueberry season begins): `berries_known`, Ready. 124 (window closed): `garden_window_closed`, Fixed. 149, 150, 237 (refruit, no moving, unripe): Video › Berry patches and Plants and fungi, Ready, with the Video tab's build line still to rename. 151 (blueberries 45 kcal, no fat): 85 kcal a 0.15 kg cup, no fat, so a half-cup pick reads 45, Ready. 167 and 182 (brown mushroom, stew round the base): shaggy mane on worn ground and poison pax as the brown lookalike (Land › New plants and fungi), Ready. 180 and 232 (lingonberries for scurvy): Ready. 225 (lily pads): Video › Lily pads, Ready.
