# Crafting and building in depth

The full recipe lists, tiers, tools, practice rules, building pieces and the warm-room heat model. The summaries stay in the main blueprint tab under Crafting and progression and Shelter and building; this tab carries the full specifications.

## Crafting and progression

### Tiers and gates

The five tiers are Start, Bushcraft, Hide and bone, Iron and Homestead. The four gates between them are events in the world, not menu unlocks.

| Tier | Gate that opens it | Typical date | Stations it adds | What it is for |
| --- | --- | --- | --- | --- |
| 1. Start | A new world | Day 1 | Hands | The crate's tools, cordage, snares, the bow drill |
| 2. Bushcraft | The first fire the player lights, by match or bow drill | Day 1 | Fire, workbench | Bows and arrows, flint, pitch, tackle, nets, the pack frame |
| 3. Hide and bone | The first sinew in hand, from a wolf, Dall sheep, caribou, either bear or a moose | September–October, year 1 | None new; hide frames and smoke | Sinew, big rawhide, leather, winter clothes, snowshoes, the sled |
| 4. Iron | The first iron in hand: a nodule, bog iron, or scrap iron from the mining camp | Weeks 2–4 | Furnace | Iron tools, the striker, steel hooks, the barrel stove, the radio |
| 5. Homestead | The first river breakup the player lives through while owning a warm room with a stove or fireplace | April, year 1 | Forge, canoe bed | Forge goods, the canoe, the fish wheel, wolf harness, livestock |

**Gate rules.**

- Gates open independently. Iron usually opens months before Hide and bone, and nothing breaks if it does.
- A tier's recipe appears in the notebook once its gate is open and the player holds any input, as above.
- Gates never close. They stay open through an Endless-mode SOS.
- Each gate shows one Trail Messenger line and a notebook page; `/yoi gate open <tier>` opens one for testing. Iron: "Iron ore. A clay furnace and charcoal could smelt it."

**Taught recipes.** Five recipes, marked † below, are learned only from a found page, even inside an open tier. The bloomery furnace and the fish wheel come from the mining camp's logbook pages. The smokehouse comes from the trapper's cabin, the canoe from Arvid's page in the beaver valley, and the radio mast from logbook page 1 in the outpost. Every page is the one item `yearofice:note` with a `yearofice:note_id` (the logbook pages are `logbook_01` to `logbook_24`); reading it adds the recipe for good. If a seed lacks the set piece, its page lies in the outpost cabin instead.

### Stations

| Station | Built from | Rules |
| --- | --- | --- |
| Hands | — | Anywhere; tools in the pack count |
| Fire | Any fire at the small-fire state or coals | For heat jobs: straightening shafts, pitch, rendering, firing clay. Each armload burned to coals leaves 2 charcoal. Block `yearofice:fire_ring` |
| Workbench | Bushcraft recipe | Use within 2 blocks. Pulls inputs from storage within 10 blocks. Under a roof it works in rain. Block entity `yearofice:workbench` with a menu |
| Furnace | Iron recipe † | Smelting and hot iron work on an anvil stone, a 30 kg flat stone set beside it. Must stand 3 blocks from wood pieces. Controller `yearofice:bloomery` |
| Forge | Homestead recipe | Does all furnace work at ×0.7 time, plus forge recipes and full repairs. Controller `yearofice:forge` |
| Canoe bed | Homestead recipe | A form on a flat bank; only for the canoe. Block `yearofice:canoe_bed` |

### Recipes

Every recipe costs time and calories. kcal is the effort rate times the base time, before practice. The rates are light 100 kcal per hour (sitting work: carving, twisting, sewing), medium 250 (standing work: scraping, knapping, shaping) and high 450 (chopping, hammering, pumping bellows, as in the activity table).

Processes that run by themselves (seasoning, drying, curing, firing clay, a charcoal burn) cost nothing after setup and show as "+ 2 d" (game days) or "+ 4 h". Camp pieces and fire builds are in Camp, materials and builds, cabin pieces in Shelter and building, clothing in Clothing, fishing tackle in Tackle, food in Food systems, and remedies in Health.

**Tier 1: Start.**

| Recipe | Inputs | Station and tool | Time | kcal | Output |
| --- | --- | --- | --- | --- | --- |
| Tinder bundle | 1 birch-bark sheet, or 2 handfuls of dry grass, fireweed fluff or lichen | Hands, knife | 2 min | 3 | 1 bundle, as dry as its driest input |
| Cordage | 2 plant fiber | Hands | 2 min | 3 | 1 m |
| Root lashing | 1 bundle of spruce roots, dug in 5 min | Hands, knife | 4 min | 7 | 2 m; lashes poles, too stiff for snares |
| Fireboard | 1 dead spruce round | Hatchet, knife | 10 min | 75 | 1 spruce board |
| Bow drill | 2 sticks, 1 cordage, 1 spruce board | Hands, knife | 20 min | 83 | 1 bow drill |
| Throwing stick | 1 birch branch | Knife | 5 min | 8 | 1 |
| Digging stick | 1 stick | Knife | 5 min | 8 | 1 |
| Trail snare | 1 m cordage or 2 m snare wire, 2 sticks | Knife | 5 min | 8 | 1 |
| Lodge snare | 4 m snare wire, 1 pole | Knife | 15 min | 25 | 1 beaver snare |
| Pole snare | 1 pole, 6 m snare wire for 3 loops | Knife | 10 min | 17 | 1 |
| Bird noose pole | 1 pole, 2 m cordage | Knife | 10 min | 17 | 1 |
| Figure-4 deadfall | 3 sticks, 1 flat rock | Knife | 10 min | 17 | 1 |
| Birch-bark torch | 1 bark sheet, 1 stick | Hands | 1 min | 2 | 15 min of light |
| Splint | 2 sticks, 2 m cordage | Hands | 5 min | 8 | Treats a broken bone |

**More Start recipes.** These make inputs that the tables assume. A hammerstone is any fist-sized rough stone, picked up, not made.

| Recipe | Inputs | Station and tool | Time | kcal | Output |
| --- | --- | --- | --- | --- | --- |
| Stakes | 1 stick | Knife | 2 min | 3 | 2 stakes |
| Wooden axe | 1 birch billet with a fire-hardened edge, 1 m root lashing | Hands, knife, fire | 30 min | 50 | Wooden axe: fells trees up to 12 cm (poles and saplings); the video's first axe, and the start for an Ironless pledge |
| Cut boughs | The lower branches of a living spruce | Knife or hatchet | 1 min | 4 | 10 spruce boughs; the tree loses 0.02 vigor (Plants and fungi › Trees) |
| Cut shoots | A willow or alder stool | Knife | 1 min | 4 | 10 willow shoots; from November to March 30% crack when bent |
| Dig sand | The edge of a gravel bar, thawed | Hands or shovel | 5 min | 21 | 1 bucket of sand for clay mixes and the bloomery |

**Tier 2: Bushcraft.**

| Recipe | Inputs | Station and tool | Time | kcal | Output |
| --- | --- | --- | --- | --- | --- |
| Workbench | 2 split log halves, 4 stakes | Hatchet | 1 h | 450 | Workbench station |
| River-stone hone | 1 flat fine-grained river stone | Hands | 15 min | 63 | Hone; restores edge to 70 |
| Tool handle | 1 split birch billet | Workbench, knife | 1 h | 100 | 1 handle |
| Wooden wedges | 1 birch stick | Knife | 10 min | 17 | 2 wedges |
| Split planks | 1 green log, 2 wedges | Hatchet | 2 h | 900 | 6 planks |
| Pitch glue | 1 spruce gum, 1 charcoal, 1 plant fiber | Fire | 10 min | 17 | 3 glue |
| Render fat | 1 kg fat | Fire, pot | 1 h, tended | 75 | 0.7 kg lard (6,300 kcal) plus 0.1 kg cracklings, never more than the raw fat's 7,000 kcal (700 per 0.1 kg raw, 900 per 0.1 kg lard) |
| Flint knife | 1 flint core | Hands, hammerstone | 10 min | 42 | 1 flint knife |
| Flint broadhead | 1 flint core makes up to 3 | Hands, hammerstone | 10 min each | 42 each | 1 head |
| Stone axe | 1 hard stone knapped to a head, 1 handle, 2 m cordage, 1 glue | Workbench | 1.5 h | 375 | Stone axe |
| Wooden shovel | 1 split plank | Workbench, knife | 1 h | 250 | Shovel |
| Hoe | 1 forked birch branch or 1 antler | Knife | 40 min | 67 | Hoe |
| Arrow shaft | 1 willow shoot | Fire, knife | 15 min | 25 | 1 straight shaft |
| Blunt arrow | 1 shaft, 3 game feathers (or none: true to 10 m), 1/3 glue | Workbench | 20 min | 33 | 1 |
| Broadhead arrow | 1 shaft, 1 flint, bone or iron head, 3 feathers, 1/3 glue, 0.2 m cordage or sinew | Workbench | 25 min | 42 | 1 |
| Bow stave | 1 straight birch 2 m long | Hatchet | 1 h + 10 d seasoning 1–3 m from a fire | 450 | 1 seasoned stave |
| Bowstring | 3 m cordage | Hands | 20 min | 33 | 1 string |
| Light bow | 1 birch sapling stave (+ 3 d seasoning), 1 bowstring | Workbench, knife | 2 h | 200 | Light bow |
| Longbow | 1 seasoned stave, 1 bowstring | Workbench, knife | 4 h | 400 | Longbow |
| Spear | 1 spruce pole 2 m, 1 flint head or a fire-hardened tip, 1/3 glue, 0.5 m cordage | Workbench, fire | 40 min | 67 | Spear |
| Fish arrow | 1 shaft, 1 barbed bone or flint point, 10 m cordage | Workbench | 40 min | 67 | Fish arrow on a line |
| Pack frame | 4 spruce poles, 6 m cordage | Hatchet | 1.5 h | 375 | Pack frame: 27 slots plus 3 lash points |
| Raft | 6 green logs, 20 m cordage or root lashing, 2 poles | Hatchet | 4 h | 1,800 | Raft |
| Birch-bark bucket | 2 bark sheets, 1 m root lashing, 1 glue | Hands | 40 min | 67 | 4 L bucket |
| Birch-bark box | 2 bark sheets, 2 m root lashing | Hands | 30 min | 50 | 9 slots, kept dry |
| Clay dish | 0.2 bucket clay, grass | Fire, fired in coals | 20 min + 4 h | 33 | 1 dish |
| Fat lamp | 1 clay dish, 100 g lard, 1 moss wick | Hands | 5 min | 8 | 6 h of light |
| Tallow candle | 0.1 kg rendered moose or bear fat, 0.2 m cordage | Fire | 20 min | 33 | 4 h of light |
| Bone needle | 1 bone sliver | Knife, hone | 20 min | 33 | Needle and awl |
| Small rawhide | 1 hare, muskrat or beaver pelt | Knife, hoop frame | 30 min + 2 d drying | 125 | 4 rawhide strips |
| Cured pelt | 1 small pelt, 0.1 kg lard | Hands | 30 min + 1 d | 125 | 1 cured pelt |
| Moose call | 1 bark sheet, 1 rawhide strip, 0.5 m cordage, 1 stick | Knife | 1 h | 100 | Moose call |
| Squeak call | 1 bark strip, 2 twigs | Knife | 10 min | 17 | Squeak call |
| Minnow trap | 20 willow shoots, 3 m cordage | Hands | 1 h | 100 | Minnow trap |
| Weir basket | 40 willow shoots, 5 m cordage | Hands | 2 h | 200 | Basket for a weir |
| Gill net segment | 30 m cordage, 2 bark floats, 2 stone sinkers | Hands | 4 h | 400 | 1 segment, fine or coarse mesh |
| Set line | 15 m cordage, 5 gorges, 1 anchor stone | Hands | 20 min | 33 | 1 set line |
| Ice picks | 2 sticks, 2 nails (the mining camp's keg of 40, or the catalogue) | Knife | 15 min | 25 | Ice picks |
| Brick frame | 4 split sticks | Knife | 15 min | 25 | Mud-brick frame |

**More Bushcraft recipes.** The hoop and pole frames are the frames the hide recipes name.

| Recipe | Inputs | Station and tool | Time | kcal | Output |
| --- | --- | --- | --- | --- | --- |
| Hoop frame | 4 willow shoots, 1 m cordage | Hands | 10 min | 17 | Hoop frame for small rawhide and cured pelts |
| Pole frame | 4 poles, 8 m cordage | Hatchet | 1 h | 450 | Pole frame for big hides; two make the hide yard (Structures and buildables › Hides, fire, earth and metal) |
| Log boom | 6 poles, 6 m root lashing, 4 stakes | Hatchet | 1 h | 450 | A floating boom across an eddy or channel up to 12 blocks wide; it stops every drift log, so logs floated down to camp are caught |
| Wire-line gill net | 10 m snare wire for the head and foot lines, 20 m cordage for the mesh, 2 bark floats, 2 stone sinkers | Hands | 4 h | 400 | 1 segment whose lines never rot; the mesh wears like a cordage net. The video's wire net |
| Walkway | 2 posts and 4 poles or 2 split planks per 2 m | Hatchet | 20 min per 2 m | 150 per 2 m | A ramp at up to 1:2 to a cache deck or dock. It stops 1 block short of a cache deck, bridged by a 2-pole ladder taken up at night, or wolverines use it |

A stave inside the fire ring instead of beside it chars and is ruined. The messenger warns once: "The stave is in the fire. Move it back."

**Tier 3: Hide and bone.**

| Recipe | Inputs | Station and tool | Time | kcal | Output |
| --- | --- | --- | --- | --- | --- |
| Sinew thread | 0.1 kg dried sinew | Hands | 30 min | 50 | 10 m thread |
| Sinew bowstring | 0.1 kg dried sinew | Hands | 40 min | 67 | 1 string |
| Big rawhide | 1 big hide | Knife, pole frame | 3 h + 2 d | 750 | 1 sheet, or 40 m of lacing from a moose (20 from a caribou) |
| Fur-on hide | 1 big hide, 0.5 kg lard or the animal's brain | Knife, pole frame | 4 h + 3 d | 1,000 | 1 cured fur hide |
| Smoke-tanned leather | 1 big hide, 0.5 kg lard or brain | Pole frame, smoke rack | 6 h + 6 d | 1,500 | 1 leather; dries soft after a soaking |
| Hide glue | 1 kg hide scraps | Fire, pot | 6 h, tended | 450 | 0.3 kg glue; stronger than pitch, fails when wet |
| Bone point | 1 bone splinter | Knife, hone | 30 min | 50 | 1 point |
| Sinew-backed bow | 1 longbow, 0.3 kg dried sinew, 0.2 kg hide glue | Workbench | 5 h + 5 d curing | 500 | Sinew-backed bow |
| Snowshoes | 2 split birch strips steamed 1 h, 30 m rawhide lacing | Workbench, fire | 6 h | 600 | Snowshoes |
| Sled | 4 split birch planks steamed at the bow, 10 m rawhide, 2 crossbars | Workbench, fire | 8 h | 2,000 | Sled: 36 slots |
| Smokehouse † | As in The smoker | — | 3 game days | — | Smokehouse |

**Tier 4: Iron.**

| Recipe | Inputs | Station and tool | Time | kcal | Output |
| --- | --- | --- | --- | --- | --- |
| Charcoal clamp | 10 armloads of wood, 2 m² of sod | Shovel | 2 h + 24 h burn, tended every 4 h | 1,150 | 15 kg charcoal |
| Clay tuyere | 0.5 bucket clay, grass | Fire, fired | 30 min + 4 h | 50 | 1 tuyere |
| Bag bellows | 2 small rawhides or 1 big, 2 planks, 2 m cordage | Workbench | 2 h | 200 | Bellows, 70% forge heat |
| Bloomery furnace † | 6 buckets clay mixed with sand and grass, 30 stones, 1 tuyere, 1 bellows | Hands | 16 h + 2 d drying | 7,200 | Furnace; relined every 20 smelts (4 buckets clay, 4 h) |
| Smelt | 5 nodules (15 kg), 20 kg charcoal | Furnace | 6 h of pumping | 2,700 | 1 bloom, 3 kg |
| Consolidate | 1 bloom, 3 kg charcoal | Furnace, anvil stone, hammerstone | 2 h | 900 | 2.0 kg bar iron (2.5 kg when Practiced) |
| Iron striker | 0.1 kg iron | Furnace | 30 min | 225 | Striker for flint and steel |
| Char cloth | 1 cloth scrap, 1 tin | Fire | 20 min | 33 | 20 char cloth |
| Steel hooks | 0.1 kg iron | Furnace | 1 h | 450 | 10 hooks |
| Iron broadhead | 0.05 kg iron | Furnace | 20 min | 150 | 1 head |
| Nails | 0.5 kg iron | Furnace | 1 h | 450 | 20 nails |
| Iron wedges | 0.6 kg iron | Furnace | 1 h | 450 | 2 wedges; planks split twice as fast |
| Hinges and latch | 0.3 kg iron | Furnace | 1 h | 450 | 1 set |
| Felling axe | 1.5 kg iron, 1 handle | Furnace, workbench | 4 h + 1 h | 1,900 | Felling axe |
| Maul | 3 kg iron, 1 handle | Furnace, workbench | 4 h + 1 h | 1,900 | Maul |
| Pick | 1.5 kg iron, 1 handle | Furnace, workbench | 3 h + 1 h | 1,450 | Pick: 5 min a nodule, against the hatchet's 10 |
| Iron shovel | 1 kg iron, 1 handle | Furnace, workbench | 2 h + 1 h | 1,000 | Digs twice as fast |
| Drawknife | 0.5 kg iron, 2 handles | Furnace | 2 h | 900 | Halves peeling time |
| Ice chisel | 1 kg iron, a 2 m pole | Furnace | 2 h | 900 | Halves ice-hole time |
| Whetstone | 1 fine sandstone block, 2 kg, taken with the pick in 5 game minutes from the sandstone band beside any coal seam | Workbench, any iron tool | 1 h | 250 | Restores edge to 100 |
| Barrel stove | The outpost's scrap barrel, 0.5 kg iron, 1 aluminum panel rolled into 2 m of pipe | Workbench | 6 h | 1,500 | Stove and pipe |
| Lantern | 0.5 kg aluminum or tin, 1 plexiglass offcut (the wreck's broken windscreen cuts into 6), 0.2 kg iron | Workbench | 2 h | 200 | 8 h per 100 g of lard or burbot oil; wind does not blow it out |
| Cache-leg guards | 16 tin cans, or 2 kg of salvaged sheet (1 aluminum panel), 8 nails | Hatchet | 1 h | 250 | A set of 4 tin bands, 60 cm wide, for 4 peeled legs (Every concept from the video › Raised cache legs) |
| Radio mast repair † | 2 kg bar iron as a sleeve, 15 m of the plane's control cable, the plane's charged battery | Workbench, then on site | 16 h | 4,000 | Working radio |

**More Iron recipes.** The pierced lantern is what lights a camp once the 6 plexiglass offcuts are used.

| Recipe | Inputs | Station and tool | Time | kcal | Output |
| --- | --- | --- | --- | --- | --- |
| Pierced tin lantern | 4 tin cans, 0.1 kg iron for the bail, a nail as the punch | Workbench | 1.5 h | 150 | A lantern with no glazing: light level 11 (about 7 blocks) against the glazed lantern's 13; wind-proof; 8 h per 100 g of lard |
| Felling wedge | 0.4 kg iron | Furnace | 30 min | 225 | Driven into the back cut (right mouse with the hatchet): turns a fall up to 10° further and prevents a barber chair |
| Cant hook | 0.5 kg iron, 1 birch pole of 1.5 m | Furnace, workbench | 1.5 h | 600 | Rolls a log on the ground or up skid poles in half the time; skid-pole slips halve |

The mining camp's 10 kg of scrap iron counts as bar iron. A drawknife, ice chisel head and bucksaw blade can also turn up in trapper caches.

**Tier 5: Homestead.**

| Recipe | Inputs | Station and tool | Time | kcal | Output |
| --- | --- | --- | --- | --- | --- |
| Leather bellows | 1 smoke-tanned leather, 2 planks | Workbench | 3 h | 300 | Bellows, full forge heat |
| Forge | 8 kg bar iron for the anvil, 40 mud bricks, 2 buckets clay, 1 bellows, a spruce stump | Hands | 24 h | 10,800 | Forge station |
| Steel edge | 1 iron edge tool, 0.5 kg charcoal | Forge | 2 h | 900 | That tool loses edge at half the rate, and a whetstone or grindstone sharpens it to 120 |
| Iron kettle | 2 kg iron | Forge | 4 h | 1,800 | 4 L kettle; boils in half the pot's time |
| Box stove | 15 kg iron | Forge | 16 h | 7,200 | Box stove |
| Stovepipe | 1 kg iron | Forge | 2 h | 900 | 1 m of pipe |
| Bucksaw | 0.6 kg iron, a birch frame, 2 m cordage | Forge, workbench | 3 h | 1,000 | Halves bucking time |
| Sled runners | 2 kg iron | Forge | 3 h | 1,350 | The sled pulls 30% easier |
| Wolf harness | 1 leather, 5 m sinew thread, 0.1 kg iron for buckles | Workbench | 3 h | 300 | 1 harness |
| Canoe bed | 12 stakes, 2 poles | Hatchet | 3 h | 1,350 | Canoe form on a flat bank |
| Birch-bark canoe † | 12 bark sheets from birch over 40 cm thick, 40 split spruce ribs, 60 m root lashing, 3 kg pitch, 2 gunwale poles | Canoe bed | 80 h | 20,000 | Canoe: 36 slots |
| Paddle | 1 split spruce plank | Workbench | 2 h | 200 | Paddle |
| Fish wheel † | 2 willow baskets 2 m wide, 1 axle log, 4 plank paddles, 12 poles, 200 stones for 2 cribs | A bank with current over 1 m/s and 1.5 m depth | 32 h | 14,400 | Fish wheel |

Livestock buildings, the cold frame and the stovehouse are pieces in Shelter and building.

### Tool durability

Every tool has two meters. Durability is vanilla durability (`minecraft:damage` against `max_damage`, the familiar bar), counting uses from new to broken. Edge is the component `yearofice:edge`, 0–120, on cutting tools only: a sharpened tool reads 100 and each use costs 100 ÷ the Edge column, so that column is the uses from sharp to dull. A use is one swing, strike, shot or fire attempt, or one game minute of cutting, scraping or digging.

- **Dull.** Below 30 edge, jobs take ×1.5 time and ×1.3 kcal, and the work preview says "dull". The river-stone hone restores edge to 70 in 3 game minutes, the whetstone to 100 in 2, and the trapper's flat file to 100 in 1.
- **Worn.** At 25% durability the tool warns once. At 0 it breaks into the parts listed.
- **Cold.** Below −30 °C, wooden handles and stone heads wear twice as fast. Below −40 °C an iron edge chips on 1% of swings, losing 20 edge.
- **Lost.** Iron and stone sink; anything in water over 1.5 m deep or muddy is gone. Wood floats and drifts to snags, so a lost spear or paddle may turn up downstream.
- **Repair.** A new handle restores a tool to full. The forge restores any iron tool to full. Stone and flint heads cannot be mended, only knapped again.

| Tool | Tier | Durability | Edge | Breaks into | Fix |
| --- | --- | --- | --- | --- | --- |
| Fixed-blade knife (crate) | Start | 6,000 | 400 | Blade and a cracked handle | New handle and glue, 1 h |
| Hatchet (crate) | Start | 5,000 | 600 | Head and a broken handle | Tool handle, 1 h |
| Bow drill | Start | Spindle 5 fires, fireboard 8, string 30 | — | The worn part | Replace the part, 5–10 min |
| Digging stick | Start | 150 | — | Nothing | Make another |
| Flint knife | Bushcraft | 300 | 100 | Nothing | Knap another |
| Stone axe | Bushcraft | 800 | 150 | Handle | Knap a new head, 30 min |
| Wooden shovel | Bushcraft | 400 | — | Nothing | Make another |
| Hoe | Bushcraft | 900 | — | Nothing | Make another |
| Spear | Bushcraft | 200 thrusts | 50 | Shaft and point | Re-point, 15 min |
| Light bow | Bushcraft | 1,500 shots | — | Nothing | Make another |
| Longbow | Bushcraft | 1,000 shots | — | Nothing | Make another |
| Plant-fiber bowstring | Bushcraft | 150 shots, twice the wear when wet | — | — | New string |
| Pack frame | Bushcraft | 300 loaded trips | — | Poles | Relash, 20 min, 2 m cordage |
| Sinew bowstring | Hide and bone | 400 shots | — | — | New string |
| Sinew-backed bow | Hide and bone | 2,000 shots; −30% draw while wet | — | Longbow | New backing, 3 h |
| Snowshoes | Hide and bone | 400 km; lacing sags to 80% effect while wet | — | Frames | Relace, 2 h, 10 m lacing |
| Sled | Hide and bone | 1,500 km | — | Planks | Replace a plank, 2 h |
| Flint and steel | Iron | Striker 2,000 strikes, flint edge 200 | — | Flint | Knock a fresh edge, 1 min |
| Felling axe | Iron | 20,000 | 800 | Head and handle | Handle 1 h, or the forge |
| Maul | Iron | 20,000 | — | Head and handle | Handle 1 h |
| Pick | Iron | 3,000 | — | Head and handle | Handle 1 h |
| Drawknife | Iron | 10,000 | 600 | — | Forge |
| Ice chisel | Iron | 5,000 | 300 | — | Forge |
| Bucksaw | Homestead | 10,000 | 1,000 | Blade and frame | New frame, 1 h |
| Canoe | Homestead | 100 hull points; a rock strike costs 10 | — | — | Patch with bark and pitch, 15 min, +20 |

Snares, nets, lines, arrows and clothing wear by the rules in their own sections. The wooden axe lasts 120 uses with an edge of 40 and breaks into nothing; the cant hook lasts 5,000 rolls and an iron felling wedge 2,000 drives. A crate knife gets about one hone per moose. A winter of firewood plus a first cabin breaks the crate hatchet's handle once or twice.

### Practice

**Rules.** Each skill keeps a count n of reps, and N reps make it Practiced. Practice speeds a job up to 40% and never changes its calories:

```latex
t = t_{base} \cdot m_{tool} \cdot m_{light} \cdot m_{body} \cdot \left(1 - 0.4 \cdot \frac{\min(n, N)}{N}\right)
```

```latex
kcal = rate_{effort} \cdot t_{base} \cdot m_{tool} \cdot m_{light} \cdot m_{body}
```

Here m\_light is 2 in the dark, and m\_body is the product of the body's slowdowns: energy below 3 bolts ×1.5, Spirits above 70 ×0.9 and below 30 ×1.1, plus sleep debt. A practiced job burns the same calories in less time, so the needs stay the clock.

1. A finished job counts 1 rep; a failed attempt counts 0.5.
2. A skill gains at most N ÷ 5 reps a game day (at least 2), so Practiced takes at least 5 days of real use.
3. Each item in a queued batch counts as a rep. A piece taken down for its full refund takes its reps back, so building and refunding earns nothing.
4. Practice never decays. It survives an Endless-mode SOS; a new Hardcore world starts at zero.
5. The work preview shows one word: Clumsy (under a third of N), Handy, or Practiced. "Fell spruce: 6 swings, about 0.3 h, 180 kcal (Handy)."

| Skill | One rep is | N | Besides speed, at Practiced |
| --- | --- | --- | --- |
| Fire | A fire lit by friction or flint | 10 | Bow-drill success 70% → 95%; at 10 fires the stroke minigame becomes a 3 s hold |
| Cordage | 1 m twisted | 40 | Strength 70% → 100% of rated |
| Knapping | A flint piece | 25 | Core shatters 25% → 5% |
| Woodcraft | A carved or shaped item | 30 | — |
| Felling | A tree down | 30 | Bad notch (the tree goes the wrong way) 20% → 3%; from Handy the fall preview also shows a knocked snag's ±30° cone (Trees, felling and firewood) |
| Splitting | A round split | 60 | The end-grain crack shows wider, so one-swing splits come easier. From 10 reps the hover shows a stack's exact moisture, the Video tab's "firewood" practice |
| Fish cleaning | A fish | 30 | The skin comes off whole |
| Butchering | Small animal 1, beaver or porcupine 3, big game 10 | 40 | — |
| Hide work | A hide scraped, cured or tanned | 15 | Holes 15% → 2%; a holed hide gives 70% of its leather |
| Sewing | A garment or a repair | 8 | Garments last 25% longer |
| Trapping | A trap set | 30 | Human-scent penalty within 5 m eases from ×0.5 to ×0.7 |
| Netting | A segment knotted or a tear mended | 8 | — |
| Log building | A wall-course metre laid, or a metre chinked | 80 | Skid-pole slips 10% → 2%; tighter notches lower Draft |
| Smoking | A batch | 10 | Flare-up loss 30% → 5% |
| Smithing | A smelt or a forged item | 20 | Bloom yield 2.0 → 2.5 kg |
| Fishing | A fish landed | 50 | Strike window +0.2 s; cast scatter ±10% → ±4% |

The bow drill's 3-second hold is a change to the real-time minigame. Its game time still follows the formula.

### Queued jobs

1. A queue, the attachment `yearofice:jobs`, holds up to 8 jobs (12 in a workshop). Each is a recipe times a count up to 20, or a set of placed building frames.
2. Inputs are reserved from the pack and storage within 10 blocks when a job is queued.
3. While queued work runs and nothing interrupts it, the clock runs at the work-until-done rate, 20 game minutes a real second (Blueprint › Controls and interaction).
4. Work pauses, keeping its progress, when any of these happens: an animal that has noticed the player enters the Active tier; feels-like falls to the shivering stage; energy drops below 3 bolts; hunger or thirst gives its warning; a fire under a process drops a state; night falls on a job that needs light when none is lit; rain falls on a station without a roof.
5. Processes that need tending (the smoker, a charcoal clamp) run without the player but warn at each check. A clamp left untended past a 4-hour check has a 30% chance to flare and lose half its charcoal.

### Trees, felling and firewood

**Trees are structures, not blocks to punch.** A standing tree is one record in the chunk attachment `yearofice:trees` (Minecraft development › World and worldgen › Chunk attachments): species, base, height, vigor, state, age and decay class. Its trunk column runs from the base to its height; climbers attach to the same column (Entities and AI › Pathfinding). Vanilla log mining is off: LMB swings a tool at the tree, and only a finished felling changes its blocks.

**Reading a tree.** Looking at a trunk within 6 blocks for 1 s shows the tree card. Diameter and lean are derived, so the tree record needs no new field.

| Line | Rule | Example |
| --- | --- | --- |
| Species and state | From the record | "White spruce, green" or "Dead, dry" |
| Diameter | d = k × height in m, in cm; k is white spruce 1.6, black spruce 1.1, birch 1.5, aspen 1.5, balsam poplar 2.0, tamarack 1.3 | "26 cm" |
| Lean | The tree builder's offset: 0–8° from hash(seed, tree), drunken trees on permafrost 5–20° | "Leans 12° east" |
| Use | Diameter band and straightness (1 spruce in 3 is straight) | "Wall log", "Pole", "Firewood" |
| Fall arrow | Notch side, lean and wind | An arrow on the ground |

**Which tree for which job.**

| Want | Tree | Diameter | Yield |
| --- | --- | --- | --- |
| Wall log, sill, ridge, purlin | Straight white spruce | 25–30 cm | 1 log of 4 m (40 kg green), 1 pole from the top, 4 armloads |
| Pole | Spruce or black spruce sapling | 8–12 cm | 1 pole of 4 m, 8 kg; black spruce poles last 1.5 times as long |
| Firewood today | Dead standing spruce | 15–25 cm | 6 armloads at 18% moisture |
| Firewood for next winter | Birch or green spruce | 20–30 cm | 10 armloads at 50%; seasoned birch burns twice as long as spruce (Blueprint › Fire, water and light › Wood) |
| Bow stave | Straight birch | 8–12 cm | 1 stave of 2 m |
| Canoe bark | Birch | Over 40 cm (1 birch in 20) | 2 sheets of 1 × 1 m; the canoe's 12 sheets take 6 trees |
| Shingles | Straight, knot-free spruce | 30 cm or more | 40 shingles a round with the froe (Structures and buildables) |
| Smoker wood | Willow, alder | Brush stems | Cut, not felled: 1 armload per 10 stems, 10 game minutes |

**Tools.** A swing is one LMB press of 1.1 real seconds that advances the clock like a work step. Swings scale with the trunk: swings = ceiling of S × (d ÷ 25 cm)², with S the tool's count at 25 cm.

| Tool | Tier | Fells up to | S, swings at 25 cm | Game minutes a swing | Limb a tree | Buck, per armload | Split, strikes a round |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wooden axe | Start | 12 cm | 15 (3 at 10 cm) | 4 | 10 min | Cannot | Cannot |
| Crate hatchet | Start | 45 cm | 6 | 4 | 10 min | 3 min | 3 (1 on the crack) |
| Stone axe | Bushcraft | 30 cm | 10 | 4 | 12 min | 4 min | 4 |
| Felling axe | Iron | Any | 4 | 2.5 | 5 min | 2 min | 2 |
| Bucksaw | Homestead | Saplings only | — | — | — | 1.5 min | — |
| Maul | Iron | — | — | — | — | — | 1 |

So the preview reads "Fell spruce: 6 swings, about 0.4 h, 180 kcal" at 450 kcal an hour, as in Controls. A 40 cm spruce takes the hatchet 16 swings. Practice (Felling, Splitting) shortens each swing up to 40% and never cuts kcal.

**Felling, step by step.**

1. **Aim the fall.** Holding an axe and looking at a trunk shows the fall arrow, pointing at the player. Left Alt plus the wheel turns it in 15° steps, up to 90° either way. The first swing locks it.
2. **Notch.** The first third of the swings (2 of 6 with the hatchet) cut the notch on the arrow side. A swing that lands more than 0.3 block off the marked notch counts half.
3. **Back cut.** The rest are struck from the far side, marked on the trunk; the player walks round. Swings from the notch side count nothing.
4. **Fall.** The creak (`tree.creak`) plays 1 s before the last swing lands. The tree becomes `yearofice:falling_tree` and turns about its base in 1.5–3.0 s, by the World table (Every concept from the video › Logs, falls and jams). It lands as one `felled_log` entity per 4 m of usable trunk, with its crown as limbs still on.
5. **Where it goes.** With a full notch, no lean over 10° and wind under 8 m/s, it lands within ±5° of the arrow. A lean over 10° pulls the line toward the lean by the lean minus 10°, when the arrow is more than 45° off it. Wind over 8 m/s from behind adds up to 10° downwind.

**What goes wrong.**

| Mishap | When | Chance | Result | Counter |
| --- | --- | --- | --- | --- |
| Wrong way | Every fell rolls the Felling skill's bad notch; a short notch or an arrow over 60° against a lean doubles it | 20% Clumsy to 3% Practiced | Falls along the lean, ±30° | Notch fully; fell with the lean; a felling wedge |
| Hang-up | The fall line passes within 1 block of a living trunk taller than half the felled tree | 50% | It lodges at 30–60° in the other crown as a `felled_log` in state `hung` | Lever it off with a pole (hold right mouse 10 game minutes, pull and brace), fell the holding tree (both fall), or wait for wind over 12 m/s (10% a day) |
| Under a hung tree | Standing in its fall line | 2% each game minute | It drops: the widowmaker injury (Body, injury and sickness: −25, head 70%, knocked out 30%) | Stay out of the line; the line shows red |
| Barber chair | Lean over 15° and the back cut started before the notch was done, or every swing struck from one side | 30% | The trunk splits 2–3 m up and the butt kicks back 1 block: the kickback injury (Body, injury and sickness: −20, a bruise) and a knock-down within 2 blocks behind the stump | Finish the notch; a felling wedge; the log becomes 2 split halves and half its firewood |
| Snag knocked down | A dead tree in the fall line | 60%, chain of 2 at most | Ideas from viewers › Snag knock-down | The red preview and the pop |
| Falls on the player | In the trunk line or the crown's 3-block fan when it lands | — | A trunk of 20 cm or more: falling tree, direct (Body, injury and sickness: −40, broken bone 60%); a thinner trunk or the crown's fan: glancing branch (−15, sprain 50%); a knock-down either way. All go through BodyService.applyInjury | The creak; step 2 blocks sideways |
| Into water or onto ice | The line ends in water | — | A `drift_log`; ice under 15 cm breaks (Logs, falls and jams) | Fell along the bank; a log boom downstream |
| Onto a piece | The line crosses a placed piece | — | The piece takes 100 HP at 10 cm, 250 at 25, 400 at 40 | Amber "Piece in the fall line" |

A squirrel in a felled tree leaps clear 70% of the time (Every concept from the video › Squirrels). Each swing is a 300 m noise for the senses (`NoiseEmittedEvent`), so a woodlot is not a hunting spot.

**Limbing and bucking.**

- **Limb.** Hold right mouse on a felled log: 10 game minutes with the hatchet, 5 with the felling axe. A spruce gives 40 boughs (a bough bed), 6 sticks and, over 8 m tall, 1 pole from the top. Birch gives 6 sticks and 2 forked branches (the throwing stick and hoe recipes).
- **Buck.** Hold right mouse on a limbed log and pick "4 m log" or "rounds". A 4 m cut is 4 game minutes with the hatchet, 2 with the bucksaw. Rounds take the tool's minutes per armload: 4 rounds of 0.5 m make one armload.
- **Rounds and split wood** are items: `yearofice:<wood>_round` and `yearofice:<wood>_split`, 3.75 kg each, stacking to 4, so 4 make the 15 kg armload. Recipes name these pieces: a split birch piece is a billet; any straight split rives into 4 strips or ribs with the knife at the workbench in 10 game minutes; and a log up to 2 m long halves along its length with the hatchet in 20 game minutes into the 2 split log halves the workbench needs. Wood is spruce, birch, aspen, poplar, tamarack, willow, alder or drift. Each carries `yearofice:moisture`.
- **Felled logs left lying.** A `felled_log` not worked for 7 game days settles into `fallen_log` blocks, which buck the same way and rot by the Decay layer, so entities never pile up.

**Splitting.** Any stump is a chopping block, and so is a round of 30 cm or more set on end (`yearofice:chopping_block`, right mouse, 2 game minutes). A round set on it (right mouse) shows the end-grain reticle; LMB on the crack splits it in 1 strike, elsewhere in the tool's count (Controls). Each strike is 1 game minute; on bare ground with no block, 30% longer. Below −15 °C frozen rounds split in half the time. After the first 10 armloads, "Split all" (hold right mouse on the block) queues every round within 10 blocks at the tool's average, so a winter's wood is not 3,000 clicks.

**Carrying and moving wood.**

| Load | How | Speed and cost |
| --- | --- | --- |
| 4 m green log, 40 kg | Shoulder carry, Shift + right mouse | ×0.6, tools blocked, one at a time |
| Heavier log (over 30 cm, or 6 m) | Drag rope, Shift + right mouse held with 2 m cordage | 0.5 m/s at 600 kcal an hour; snags on brush and rocks |
| Any log on a skid trail | Drag | Twice as fast |
| Downhill | Push it (hold right mouse 2 s) on a slope over 15° | Free; it stops on the flat or at a trunk |
| On snow | Sled, 3 logs; toboggan or a wolf team, 2 | The real reason cabin logs were cut in winter |
| By water | Roll it in: a `drift_log` | A log boom below catches it |
| Split wood | In the pack | Crate backpack 2 armloads, pack frame 3, sled 8 |

**Drying, seasoning and the woodshed.** Moisture follows Every concept from the video › Firewood drying: seasoned means under 20%, and a woodshed dries split wood at twice a rack's rate. The rule the numbers make: green wood (50%) split into the woodshed needs 30 points, so 10 days in summer and 25 in September or October; wood split after October 15 is not seasoned until about mid-December, so late wood dries by the stove (−4 a day, 4 armloads at most within 3 blocks). Seasoned wood never rots under cover (Decay and time).

**Armloads burned per day at −30 °C.**

| Setup | Armloads a day | Winter of 150 heating days |
| --- | --- | --- |
| Open long fire, sleeping beside it | 8 | Not a winter plan |
| Lean-to with reflector, long fire on Low at night | 5 | 75 trees |
| Repaired outpost cabin, barrel stove | 3 | 45 trees |
| Chinked first cabin, barrel stove on Steady | 2 (3 at −40 °C) | 30 trees |
| Unchinked cabin | 5 | 75 trees |
| Dugout, fireplace on Low | 4 | 60 trees |
| Cooking, smoker or banya on top | 1–3 more | — |

**The winter's wood in hours.** One 10-armload tree with the crate hatchet is fell 24 min, limb 10, buck 30 and split 120: about 3.1 game hours and 1,380 kcal. Thirty trees are about 92 game hours and 41,000 kcal. With the felling axe, bucksaw and maul it is 10 + 5 + 15 + 40 minutes, about 1.2 hours a tree and 35 a winter. That is the wood chore's upgrade path.

**Logs for building.** Peel with the hatchet in 15 game minutes or the drawknife in 8. From May 15 to July 15 the sap runs: a log peels in half the time and gives 2 spruce bark sheets (`yearofice:spruce_bark`); outside it the bark tears and gives none. Peeled logs rot at 2% a year against 10%. Logs stacked on a `log_deck` under bark dry at the rack rate; one laid below 25% shrinks 2% instead of 5%, so only 40% of its seams reopen on September 1.

**Bark.** A bark sheet in a building recipe is spruce or birch bark; containers and canoes need birch (`yearofice:birch_bark`), one sheet a tree a year, and peeling all round kills it.

**Stumps and regrowth.** Felling leaves `yearofice:stump`, which greys through `age` 0–3 by its felling day (`StumpPass`). Every stump is a chopping block (Splitting), and the forge needs a spruce stump. Clearing a stump for a garden takes 1 game hour with a shovel. A dead spruce stump left 10 years becomes a `fatwood_stump`, and any stump becomes a `rotten_stump` with grubs at 15. Regrowth follows The open world's table: fireweed the first June, birch and willow saplings in 2 years, spruce in 6; birch poles at 8 years and logs at 20, spruce logs at 30. Willow and alder stools coppice every 3 years and aspen suckers from the roots (Plants and fungi › Trees). The near stand thins first: after 3 winters the nearest firewood tree is about 150 m from a cabin, which is what the skid trail and the sled are for.

**Beavers fell trees too.** A colony fells 1 aspen, poplar, willow or birch of up to 30 cm a week of open water, within 30 blocks of its pond. The tree falls toward the water 70% of the time and leaves a pencil-point `stump` with `gnawed=true`. Its trunk lies as `fallen_log` blocks that the beavers cut and tow away over 5 days, so a fresh beaver tree near the bank is 4 free armloads of green wood. A beaver tree can hang up or land on a piece like any other. A cage of 3 m of snare wire (5 game minutes) saves a tree. `ecosystem.species.beaver.DamWork` submits it as a `FALL` op through `WorldEditService.submit`.

**Bears and falling trees.** A grizzly shoving a stand tree under 20 cm topples it after 3 shoves (Every concept from the video › Bears and tree stands). It uses the same `falling_tree` entity and damage table, with the fall line pointing away from the bear, and the player falls by the 3–5 m fall rules. Trees of 20–30 cm are only shaken, those over 30 cm are safe, and bears fell nothing else.

**On screen and sound.** Chips fly on each swing, the notch shows pale on the bark, and the fall arrow turns amber for a crown in the line and red for a snag or a piece. Trail Messenger lines: "Fall line east. A birch is in the way." · "Hung up. Lever it or leave it." · "Barber chair: the trunk split." Sounds: `tree.chop`, `tree.creak`, `tree.fall`, `tree.split`, `tree.hang_creak`, with the existing `tree.snag_pop` and `tree.shove_creak`.

**Build.** `build.wood.FellingService` reads the tree record, scores swings, rolls the mishaps, clears the trunk column and leaves, marks the record felled with its day and spawns `world.logs.FallingTreeEntity`, which lands as `world.logs.FelledLogEntity`. The work actions are `data/yearofice/action/fell.json`, `limb.json`, `buck.json`, `split.json` and `peel.json`. Tool numbers (S, minutes a swing, limb, buck and split rates) are in `data_maps/item/tool.json`, wood in `data_maps/item/fuel.json`, and each tree's k and yields in a `wood` block of its `plant/<id>.json`. Milestones: felling, limbing and bucking M2; felling hazards M6; split all, moisture and the woodshed M8. Debug: `/yoi tree info` prints the record, diameter, lean and fall line; `/yoi tree fell <yaw>` fells the looked-at tree; `/yoi tree mishap <kind>` forces a mishap; `/yoi wood dry <days>` steps moisture. Tuning lives in the `[wood]` section of `yearofice-common.toml` (swing scale, mishap chances, drying rates), with the presets' values. Every time here is in game minutes, so the day-length world option rescales it.

**GameTests.**

1. `yearofice:build/fell_swings` (M2): the crate hatchet fells a 25 cm white spruce in 6 swings and 24 ±1 game minutes at Clumsy; the felling axe in 4 swings and 10 minutes; a 40 cm spruce takes the hatchet 16.
2. `yearofice:build/fall_line` (M6): 100 fellings with a full notch, no lean and calm air land within ±5° of the arrow; with a 15° lean and the arrow 90° off it, a Clumsy feller drops 40% ±5 along the lean.
3. `yearofice:build/barber_chair` (M6): a back cut begun before the notch on a 20° lean barber-chairs 30% ±4 of 1,000 fells; with a felling wedge, 0.
4. `yearofice:build/hang_up` (M6): a fall line within 1 block of a taller living trunk hangs up 50% ±4; a player held in a hung tree's line is hit within 50 game minutes in 64% ±5 of runs.
5. `yearofice:build/winter_wood` (M8): a bot heating the chinked cabin at −30 °C for 150 days burns 290–310 armloads; its wood work takes 85–100 game hours with the crate hatchet and 32–40 with the felling axe, bucksaw and maul.
6. `yearofice:ecosystem/beaver_felling` (M9): a colony with 40 aspen within 30 blocks fells 1 ±0.2 a week of open water, none over 30 cm.

### Data and tests

```json
{
  "type": "yearofice:station",
  "tier": "bushcraft",
  "station": "yearofice:workbench",
  "ingredients": [
    { "item": "yearofice:seasoned_stave", "count": 1 },
    { "item": "yearofice:bowstring", "count": 1 }
  ],
  "tool": "#yearofice:knives",
  "work_min": 240,
  "effort": "light",
  "kcal": 400,
  "skill": "woodcraft",
  "taught_by": null,
  "result": { "id": "yearofice:longbow", "count": 1 }
}
```

Each recipe is `data/yearofice/recipe/station/<id>.json` of the recipe type `yearofice:station`; joining, pounding, reworking and salvage use the types `combine`, `pounding`, `rework` and `salvage`. Tool numbers are data maps: `max_condition` in `data_maps/item/props.json`, and job-time multipliers, swings and `edge_loss` per use in `data_maps/item/tool.json`; wood is in `data_maps/item/fuel.json`. Classes: `build.stations` (station block entities and menus, posting `StationCraftEvent`), `build.craft.Gates` (tiers, unlocks, previews), `build.craft.PracticeService` (attachment `yearofice:practice`), `build.craft.ToolWear` (durability and `yearofice:edge`) and `build.work.JobQueue` (attachment `yearofice:jobs`). Milestones follow the engine: stations, Start and Bushcraft M3; felling and bucking M2; hides and winter gear M7; firewood, woodpiles and the woodshed M8; the furnace, forge, Iron and Homestead M9.

- *Graph test:* every recipe input can be traced to the land, the crate, a set piece, a supply drop or another recipe in a tier whose gate can open. No orphan items, and no cycles except the hammerstone.
- *Practice test:* 10 bow-drill fires over 5 or more days bring success to 95% and game time to ×0.6 of the first fire's.
- *Wear test:* a simulated year of 33 firewood trees and one first cabin breaks the crate hatchet's handle 1–2 times.
- *Gate test:* over 100 runs of the good-player bot, Iron opens by day 30, Hide and bone by day 185 and Homestead in April on at least 90%.

## Shelter and building

### Building pieces

The 12 camp pieces in Camp, materials and builds and the pieces below make up the building set. Time and kcal cover placing and building the piece at the high rate of 450 kcal an hour. Felling, hauling and peeling logs are counted per log, as in Gathering materials and Building a cabin.

U is heat loss in W per m² per °C (game values for the room model below), and HP is how much damage a piece takes.

| Piece | Size | Materials | Time | kcal | U | HP | Note |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Corner stone | 0.5 × 0.5 m | 1 large stone, 30 kg, carried in the hands | 20 min | 150 | — | 2,000 | Stacks to 0.5 m on a slope |
| Gravel pad | 1 × 1 m | 4 buckets of gravel | 20 min | 150 | — | 2,000 | Soft or wet ground |
| Log crib | 1 × 1 m | 1 green log | 1 h | 450 | — | 1,500 | Foundation over 0.5–1.5 m of drop |
| Sill log | 4 m | 1 peeled log | 1 h | 450 | — | 1,500 | Walls start on it |
| Log wall course | 1 m long, 0.28 m high | 0.25 peeled log | 15 min with a saddle notch, 25 with a dovetail (iron tools) | 113 | 0.5 | 600 | 7 courses make a 2 m wall |
| Gable log | 1 m | 0.25 log | 20 min | 150 | 0.5 | 600 | Closes the wall under a ridge |
| Ridge pole or purlin | 4 m | 1 log | 1 h | 450 | — | 1,500 | Spans up to 4.5 m |
| Pole-and-bark roof | 1 × 1 m | 4 poles, 1 bark sheet | 30 min | 225 | 2.0 | 200 | Lets 10% of storm rain through |
| Sod roof | 1 × 1 m | 4 poles, 1 bark sheet, 1 moss sheet, 6 shovels of sod | 1.4 h | 630 | 0.4 | 800 | 300 kg per m²: needs a ridge and purlins |
| Thatch roof | 1 × 1 m | 4 poles, 4 kg barley or grass straw | 45 min | 340 | 0.6 | 300 | Homestead; straw from the garden |
| Plank wall | 1 × 2 m | 4 split planks | 30 min | 225 | 1.5 | 300 | Sheds and inner walls |
| Pole-and-bark wall | 1 × 2 m | 6 poles, 2 bark sheets | 20 min | 150 | 3.0 | 150 | Woodshed and lean-to sides |
| Dirt floor | 1 × 1 m | — | 10 min to level | 75 | 0.6 | — | The default |
| Plank floor | 1 × 1 m | 2 split planks over 1 moss sheet | 30 min | 225 | 0.35 | 300 | Warmer floor |
| Wood slab floor (`yearofice:slab_floor`) | 1 × 1 m, one block cell on the 1 m grid; a half-block slab 0.5 m thick | 1 `yearofice:<wood>_split` adzed flat on one face (a puncheon), laid on levelled ground or straight over a stone floor | 15 min with the hatchet or adze | 113 | 0.45 | 250 | The video's wood slabs over cobble (rows 95, 143). Replaces a dirt or stone floor block in place; the split's wood sets the block's `wood` state (8 woods). Warmer than dirt (U 0.6) or stone underfoot; 9 or more laid cells in one room count once as decoration for Spirits. Milestone M8 with the other pieces; GameTest `yearofice:build/slab_floor_u`: a 3 × 4 room floored in slabs loses 25% less heat through the floor than dirt, ±2% |
| Plank door | 0.8 × 2 m | 6 split planks, carved, rawhide or iron hinges, a bar | 6 h | 2,700 | 1.5 | 400 | Barred, it holds wolves |
| Hide door flap | 0.8 × 2 m | 1 big rawhide or 3 small | 30 min | 225 | 3.0 | 100 | Quick; leaks 5 W/°C even closed; wolves tear it |
| Hide window | 0.5 × 0.8 m | 1 rawhide, 2 poles | 2 h | 900 | 4.0 | 50 | 30% light |
| Plexiglass window | 0.6 × 0.6 m | The plane's side window, 2 poles | 1 h | 450 | 3.0 | 150 | 80% light; the wreck has one side window, and only its broken windscreen cuts into lantern offcuts |
| Shutter | Per window | 2 planks | 20 min | 150 | 1.5 shut | 200 | Close it at night |
| Fireplace | 1.2 × 1 m | 60 stones, 4 buckets clay | 12 h | 5,400 | — | 3,000 | Bushcraft heat |
| Barrel stove | 0.6 × 0.9 m | Crafted, Iron | 2 h to install | 900 | — | 1,000 |  |
| Box stove | 0.5 × 0.8 m | Crafted, Homestead; or the roadhouse's 60 kg cast-iron stove | 2 h | 900 | — | 2,000 |  |
| Mud-brick chimney | 1 m | 12 mud bricks, 0.5 bucket clay | 1 h | 450 | — | 800 | For a fireplace |
| Stovepipe | 1 m | Crafted | 20 min, plus a clay collar where it meets the roof | 150 | — | 400 | For a stove |
| Chinking | 1 m of seam | 0.2 moss sheet inside, 0.05 bucket clay outside | 3 + 3 min | 45 | Closes the seam | — | Cabin step 8 |
| Shelf | 1 m | 2 planks, 4 pegs | 30 min | 225 | — | 100 | 9 slots; a decoration slot |
| Bunk | 1 × 2 m | 4 poles, 6 m cordage, 40 boughs | 2 h | 900 | — | 200 | Full rest in a warm room |
| Roof brace | A post | 1 pole | 20 min | 150 | — | 600 | Doubles the snow a roof carries, 60 to 120 cm |
| Raised cache | 1.5 × 1.5 m on 3 m legs | 4 posts, 12 logs, 6 planks, a ladder of 2 poles | 16 h | 7,200 | — | 1,200 | 27 slots. A grizzly cannot reach a deck whose underside is 3 m or more above the ground it stands on. Peeled legs with a set of 4 bands set right are wolverine-proof; only snow deep enough to bring a band within reach lets one climb, at 20% a visit. The legs may be 3 or 4 living trees cut at 3 m, the video's platform cache, which climbers use until they are peeled and banded |
| Woodshed | 3 × 2 m | 6 posts, 8 poles, pole-and-bark roof | 8 h | 3,600 | — | 600 | Holds 60 armloads; seasons wood twice as fast |
| Root cellar | 2 × 2 m into a hillside | 20 logs, 4 m² of sod roof, a plank door | 32 h | 14,400 | 0.3 | 2,000 | Holds 2–4 °C; 54 slots of roots; can't be dug in frozen ground |
| Cold frame | 1 × 2 m | 4 planks, the plexiglass window or 2 rawhide lids | 2 h | 900 | — | 200 | Bed inside is 6 °C warmer by day; sow 3 weeks early |
| Brush fence | 2 m | 10 bundles of brush | 15 min | 113 | — | 50 | Stops hares and grouse |
| Pole fence | 2 m | 2 posts, 3 poles | 30 min | 225 | — | 200 | Stops moose |
| Hen house | 1.5 × 1.5 m | A room of any walls, a roof, a door, 2 perches | 8 h | 3,600 | By pieces | — | Homestead; laying is the light factor (0 at 10 h of light rising to 1 at 14 h; a lamp counts) times the temperature factor (0 below 0 °C, 0.5 at 0–5 °C, 1 above 5 °C); feed 0.1 kg of grain a hen a day at 5 °C or more, 0.15 kg below, and 1 kg of fish meal stands for 0.75 kg of grain |
| Goat shed | 3 × 2 m | A room, a door, a hay rack | 16 h | 7,200 | By pieces | — | Homestead |
| Log bridge | Up to 8 m span | 2 logs, 4 m of plank deck | 3 h | 1,350 | — | 1,000 | A permanent crossing |
| Skid trail | 10 m | Cleared brush, 6 skid poles | 1 h | 450 | — | — | Halves hauling time on it |
| Path (`yearofice:path`) | 1 block per metre on the 1 m grid; replaces the top surface block, like a floor | Gravel: 1 bucket of bar gravel a block. Plank: 1 `yearofice:<wood>_split` a block | 5 min a block | 38 | — | 300 gravel, 150 plank | State `surface` gravel or plank (row 143's path). Stumble chance 0, no mud slowdown, walking ×1.1 (Every concept from the video › Camp finishing pieces); under 40 cm of snow the packed-trail rules take over; a plank path on soil rots at ×3. Milestone M8; GameTest `yearofice:build/path_speed`: a bot walks 100 m of gravel path 10% faster than bare ground, ±1%, with 0 stumbles in 1,000 crossings |
| Trophy mount | — | A head, antlers or a pelt | 1 h | 450 | — | — | Decoration for Spirits |

A rock face over 6 m tall counts as a wall of U 0.3, so the dig-in base needs only three built walls and a roof. These numbers rebuild the first cabin's schedule. It has about 109 wall-course metres (27 h to lay), 18 m² of sod roof (25 h) and 109 m of seam to chink (11 h).

The shelter tiers 1–5 in the main tab are levels of comfort, not crafting tiers. Shelter tiers 1–2 need Start tools. Tier 3 needs a fireplace (Bushcraft) or a stove (Iron), tier 4's smokehouse needs Hide and bone, and tier 5 needs Homestead.

**Damage.**

- **Fire.** Wood pieces in contact with flames lose 50 HP a game minute; stone and clay pieces take none.
- **Animals.** A wolf battering a door does 10 HP a hit. A bear does 50 HP a swipe at doors, windows and ground caches.
- **Rot.** Unpeeled logs lose 10% of their remaining HP a year and peeled ones 2%, settled continuously, so an unpeeled log keeps about 37% after 10 years. The site factors in Structures and buildables › Rot multiply these rates.
- **Snow load.** Over 60 cm on an unbraced roof, the roof sags and drips: −5 HP a day per panel until shoveled or braced. Shoveling takes 20 min per 10 m².
- **Shrinkage.** In a cabin's first year its logs shrink 5%. On the next September 1, every chinked metre turns back to moss-only.
- **Flood.** A spring-flood event does 200 HP to every piece below the flood line and washes out its storage.

### Placing and snapping

1. Select a piece in the hotbar, or open the pack (E) and pick it on the Build tab of `PackScreen`.
2. A ghost follows the aim point within 6 blocks, drawn translucent: white means valid, amber valid with a warning it names, red invalid with a reason it names.
3. Left Alt plus the mouse wheel turns it in 90° steps. Stairs and slabs take their facing from the aim, as in vanilla.
4. Right mouse places a frame, `yearofice:frame`: a block with no collision whose block entity names the piece and the materials still owed.
5. Hold right mouse at the frame to build it. It pulls materials from the pack and from storage within 10 blocks, shows the work ring, and can be queued.
6. A finished piece refunds fully if taken down within 1 game hour, and its practice reps go back with it. After that, taking it down with the hatchet (hold right mouse) returns 50% of its materials, and whole logs from pieces above 50% HP.

**Snapping.**

- **Grid.** Every piece sits on Minecraft's 1 m block grid; slabs and stairs give the half steps. The 0.1 m fine grid and free placement are dropped (Minecraft engine › The 1 m grid). The 3.6 × 4.2 m cabin is 5 × 6 blocks outside and 3 × 4 inside.
- **Where pieces go.** A sill or crib takes a foundation block below it. A log wall block, 3.3 course-metres, goes on a sill or another log wall; a door or window replaces the wall blocks it cuts. A ridge or purlin takes roof blocks, and any roof block takes a chimney or a stovepipe with its collar.
- **Support.** Every piece must trace down to the ground. Foundations give 100; each log wall block up costs 7, a post 5, a beam 10 per block across, and roof and floor blocks 20 (sod 40) per block from the nearest wall, beam or post (Structures and buildables › Structural support). Below 0 the ghost turns red: "Needs support within 2 blocks." There is no physics collapse; pieces fail only by the damage rules above.
- **Levelling.** A corner stone, gravel pad or crib fills 1 block of drop, and two stacked cribs fill 2. Ground steeper than 25° is red.
- **Clearance.** No piece in a trunk column or within 1 block of one (fell the tree first), no overlaps, and nothing in a water block except bridges, weirs, the dock, the log boom, the fish wheel and landing pieces.

**Amber warnings** make the camp site check specific to each piece:

| Warning | When |
| --- | --- |
| "Below the flood line" | Under the mud mark on nearby trunks |
| "Dead tree overhead" | Within the fall length of a dead tree or hung branch |
| "Smell upwind of the bed" | A cleaning spot, smoker or cache upwind of a bed within 50 m |
| "Wood near the fire" | A wood piece within 2 m of a fire ring |
| "Chimney too low" | A chimney top under 0.6 m above the ridge; smoke blows back in winds over 6 m/s |

**Blueprints.** K opens blueprint mode. The player marks two corners, and up to 400 placed piece blocks inside are saved as a structure template at `<world>/generated/yearofice/structure/blueprint/<player>/<name>.nbt`. Stamping one places every frame at once, turned in 90° steps. Frames fill from storage within 50 blocks, and the messenger lists what is missing. "Build all" queues the frames in order, foundations first, under the queued-job rules in Crafting and progression.

**Repairs.** Holding right mouse on a damaged piece spends 25% of its materials to restore 50% of its HP. Damage to blocks without a block entity is kept in the SavedData `yearofice_piece_damage`, written only while a piece is damaged (Minecraft engine › The 1 m grid).

### Warm rooms

**Finding a room.** The flood fill runs over air blocks on 1 m cells, from the player or a heater. Every boundary face must be a block tagged `yearofice:room_walls` (walls, closed doors, windows, rock and earth faces), and every floor cell needs a block tagged `yearofice:room_roofs` within 4 blocks above. A room holds up to 50 floor cells and 200 air blocks, and inner walls split rooms. A space with any air path out is not a room: it counts as a shelter (+5 °C in the warmth formula) with no heat model. Rooms live in the SavedData `yearofice_rooms` and rebuild at most once a real second after a place, break or door change inside them.

**The heat model.** Each room has one temperature, which changes every game minute:

```latex
C \frac{dT_{room}}{dt} = H - L_{air} (T_{room} - T_{out}) - L_{floor} (T_{room} - T_{ground})
```

```latex
L_{air} = \sum U_i A_i + 0.4 \cdot m_{open} + 0.1 \cdot m_{moss} + 40 \cdot d_{open} + 1.5
```

The sum runs over walls, roof, doors and windows. m\_open counts unchinked seam metres, m\_moss counts seams chinked only with moss, and d\_open counts open doors. The 1.5 W/°C is a base leak. L\_floor is the floor area times its U. The ground under a cabin is the 5-day mean air temperature, held between −5 and 8 °C, so in winter the floor loses less than the walls.

**Heat sources.**

| Source | Heat stays in the room | Low | Steady | Hot |
| --- | --- | --- | --- | --- |
| Fireplace | 40% | 7.5 h an armload, 0.64 kW | 3 h, 1.6 kW | 1 h, 2.4 kW |
| Barrel stove | 85% | 10 h, 1.0 kW | 4 h, 2.5 kW | 1.3 h, 3.75 kW |
| Box stove or cast-iron stove | 90% | 12.5 h, 0.84 kW | 5 h, 2.1 kW | 1.7 h, 3.2 kW |

An armload holds 11.8 kWh, and the share kept in the room is in the second column. Times are for a ×1.0 wood; the Wood table in Blueprint › Fire, water and light multiplies them, and Low burns at 0.4 of Steady's rate as its Settings table says, so a birch armload on Low in the barrel stove lasts 4 × 1.4 ÷ 0.4 = 14 game hours. Hot burns wood three times as fast as Steady and loses half of it up the chimney, so it is for warming a cold room quickly. Banked coals keep a stove giving 0.3 kW for 6 hours without fuel.

Other sources: the player 100 W (70 asleep), a tamed wolf 60 W, a fat lamp 30 W, a candle 20 W, a lantern 40 W. Sun through a plexiglass window adds 150 W per m² in direct sun, through rawhide 50.

**Heat capacity.** C sets how slowly a room warms and cools. A log wall-course metre holds 2.5 Wh/°C, a m² of sod roof 3, a dirt floor m² 2, a plank piece 0.5, a fireplace 40 and a rock face 10 per m². So a stone fireplace keeps radiating after the fire dies, and a dig-in base holds its warmth.

**The first cabin, worked through.** Fully chinked with a sod roof, a plank floor and a barrel stove, the 3.6 × 4.2 m cabin has L\_air of about 27 W/°C, L\_floor of 5 and C of about 356 Wh/°C. That gives a time constant of 11 game hours. On 2 armloads a day at Steady, the room swings between about +20 and +38 °C above outside, the "+25 to +35 °C while the stove burns" in the main tab's tier table.

Left unchinked, the same cabin's L\_air rises to about 71 W/°C. On 2 armloads it holds only about +13 °C, and it needs 5 a day to match the chinked cabin, which is the doc's 2 against 5. The repaired outpost cabin with no fire and the player inside holds about +8 °C on a −30 °C day, its tier-2 value.

**Feels-like indoors.** Inside a room, the warmth formula takes the room's temperature as the air, with no wind chill and no shelter bonus. A burning stove adds up to +10 °C within 1.5 m and a fireplace +15 within 2 m, fading to 0 at 3 m. These replace the open fire's +25. Bedding adds its sleeping warmth from Clothing.

**Smoke.** A stove or fireplace with no connected chimney, or an open fire in a closed room, adds 10% smoke a game minute. At 50% the room is smoky: the player coughs, sight drops to 5 blocks, Condition falls 1 a game hour, sleep cannot start and a grey haze covers the screen. Above 80% Condition falls 5 an hour and a sleeper wakes. An open door clears 20% a game minute. These match Blueprint › Fire, water and light › Smoke indoors.

**Creosote.** Each chimney tracks creosote from 0 to 100: +1 an armload of seasoned birch, +2 seasoned spruce, +5 green wood, and +3 an hour of Low. The messenger warns once at 60: "Chimney needs cleaning. Creosote is building." Above 70, each hour on Hot has a 3% chance of a chimney fire, which burns the roof panel around the pipe until the player chokes the stove (hold right mouse on its damper). A spruce-bough brush on a pole cleans it to 0 in 30 game minutes.

**Snow on the roof.** Snow cuts roof loss by 2% per cm, down to 40% of the roof's U at 30 cm. A roof with U over 1.0 above a room warmer than 0 °C melts its snow 2 cm a day, so a poor roof loses its blanket and grows icicles. Snow shoveled against a wall (20 min a side) cuts that wall's U by 30% until the thaw.

**Rooms side by side.** Heat passes between neighbouring rooms through the shared wall's U times area, and 40 W/°C through an open inner door.

**The cabin card, in numbers.**

| Rating | From | Words |
| --- | --- | --- |
| Draft | Seam, door and base leaks | Tight under 5 W/°C, Fair 5–20, Drafty over 20 |
| Insulation | Conductive loss per m² of floor | Good under 2.0, Fair 2.0–3.5, Poor over 3.5 |
| Light | Window area and lamps | Bright, Dim, Dark |
| Heat | Steady-state rise at Steady | "+31 °C at Steady" |

The card also estimates armloads a day at the 5-day forecast's temperature, the number the morning prompt quotes.

Off-screen rooms settle in closed form when their chunk loads, from the hourly temperature record and the fuel (Minecraft engine › Warm rooms). Data: the data map `data_maps/block/heat.json` holds U, C and seam metres per block face and the heater rows; chinking is the `chink` state (`none`, `moss`, `clay`) on `yearofice:log_wall`. Classes: `build.rooms.RoomFinder` (flood fill, L and C) and `build.rooms.HeatModel`, which post `RoomChangedEvent`; `fire.StoveBlockEntity` (heaters, smoke, creosote). Roof snow is `snow_cover` from `world.pass.SnowPass`; shrinkage and rot come from the Decay layer through `data_maps/block/decay.json`. Milestone M8.

- *Room test:* on a constant −30 °C, the chinked first cabin on 2 armloads a day at Steady averages +26 to +32 °C above outside and never drops below +18.
- *Draft test:* the unchinked cabin needs 4–6 armloads a day to match it.
- *Outpost test:* the repaired, unheated outpost cabin with the player inside holds +6 to +10 °C above a −30 °C day.

**Winter gear.** It lives in the Clothing section: layers, body slots, warmth, wind and wet values, wear, repair and each garment's recipe. Snowshoes and the sled stay in Crafting and progression. A warm room changes clothing only through its temperature, which the warmth formula reads as the air.

**Review audit (Tree cutting; Camp to cabin).** Reviewer D, Build and craft, pre-development round. Keys are the engine table's: LMB swing, right mouse use and hold-to-work, E pack, Left Alt + wheel to turn, K blueprint mode, F off-hand.

| Step (tree cutting) | Rule, number and test | Rule location | Status |
| --- | --- | --- | --- |
| Find and read a tree | Tree card after 1 s within 6 blocks; d = k × height; lean 0–8°, 5–20° on permafrost | Crafting and building in depth › Trees, felling and firewood › Reading a tree | Fixed in this round |
| Pick the tree for the job | Wall log 25–30 cm straight white spruce; pole 8–12 cm; dead spruce 18%; canoe birch over 40 cm | Same › Which tree for which job | Fixed in this round |
| Tool tiers | Wooden axe 12 cm, crate hatchet S 6, stone axe 10, felling axe 4; swings = S × (d ÷ 25)²; `build/fell_swings` | Same › Tools; Recipes › More Start recipes; Tier 4 | Fixed in this round |
| Aim, notch, back cut | Fall arrow, Left Alt + wheel 15°; notch = first third; back cut from far side; ±5° with a full notch | Same › Felling, step by step; `build/fall_line` | Fixed in this round |
| Falling tree | `falling_tree` 1.5–3 s, lands as `felled_log` per 4 m; into water as `drift_log`; ice under 15 cm breaks | Same; Every concept from the video › Logs, falls and jams | Ready |
| Dangers | Wrong way 20% → 3%; hang-up 50%; under a hung tree 2%/min; barber chair 30%; hit 10/25/40 Condition | Same › What goes wrong; `build/barber_chair`, `build/hang_up` | Fixed in this round |
| Limb, buck, halve | Limb 10 min (5 felling axe); 4 m log or rounds; 4 rounds an armload; halves for the workbench | Same › Limbing and bucking | Fixed in this round |
| Split | Chopping block (any stump, or a 30 cm round, 2 min); crack 1 strike; Split all after 10 armloads; ground ×1.3 | Same › Splitting; Blueprint › Making camp | Fixed in this round |
| Carry and skid | Shoulder 40 kg ×0.6; drag rope 0.5 m/s; skid trail ×2; sled 3 logs; log boom | Same › Carrying and moving wood; Building pieces › Skid trail; More Bushcraft › Log boom | Fixed in this round |
| Dry and store | `yearofice:moisture`; seasoned under 20%; woodshed −3.0 a summer day; after Oct 15 only by the stove | Same › Drying; Blueprint › Fire › Seasoning; Video › Firewood drying | Fixed in this round |
| Burn | 2 armloads a day in the chinked cabin, 5 unchinked; Low = 0.4 of Steady; birch on Low 14 h | Same › Armloads burned; Warm rooms › Heat sources; `build/winter_wood` | Fixed in this round |
| Build with logs | Peel 15 min (drawknife 8), May 15–Jul 15 half time + 2 bark sheets; peeled rot 2%/yr; log deck | Same › Logs for building; Building pieces | Fixed in this round |
| Stumps and regrowth | `stump` age 0–3; fatwood at 10 yrs; rotten with grubs at 15; regrowth table; near stand 150 m after 3 winters | Same › Stumps and regrowth | Fixed in this round |
| Beavers fell trees | 1 tree ≤30 cm a week of open water within 30 blocks; 70% toward water; `FALL` op; wire cage | Same › Beavers fell trees too; Structures › Animal damage | Fixed in this round |
| Bear pushes tree | Grizzly topples a stand tree under 20 cm in 3 shoves; same entity and damage | Same › Bears and falling trees; Video › Bears and tree stands | Ready |
| Upgrade path | Hatchet 3.1 h a tree → felling axe, bucksaw, maul 1.2 h; 92 h → 35 h a winter | Same › The winter's wood in hours | Fixed in this round |
| Debug and tuning | `/yoi tree info`, `fell`, `mishap`; `/yoi wood dry`; `[wood]` in `yearofice-common.toml` | Same › Build | Fixed in this round |

| Step (camp to cabin) | Rule, number and test | Rule location | Status |
| --- | --- | --- | --- |
| Gather | 300–500 kcal a game hour; sticks, dead spruce, bark, fiber, roots, stone, clay, moss, sand | Blueprint › Gathering materials; Recipes › More Start recipes (sand, boughs, shoots) | Ready |
| Site check | Six results, plus flood line and level within 30 cm for a cabin; amber warnings per piece | Blueprint › Making camp; Placing and snapping › Amber warnings | Ready |
| Fire from parts | Clear, ring, tinder (dryness 60+), kindling, fuel, light, blow (hold right mouse) | Blueprint › Building a campfire | Fixed in this round |
| Fire states | Ember 60 s, tinder 2 min, small 20 min, coals 1 h, banked 6 h; `yearofice:fire_ring`; `/yoi fire` | Blueprint › Fire, water and light; Minecraft engine › Fire | Fixed in this round |
| Banking and settings | Bank by holding right mouse 3 s; setting on the fire's card; HUD "Fire, Low" | Blueprint › Fire, water and light › Settings | Fixed in this round |
| Camp pieces | Debris bed, bough bed, lean-to, reflector, stand, line, woodpile, chopping block, cleaning spot, meat pole, latrine, chest | Blueprint › Making camp | Fixed in this round |
| Smoker | Rack (Bushcraft) and smokehouse † (Hide and bone); checks every 4 game hours; spruce smoke −1 Spirits a meal | Blueprint › The smoker; Queued jobs 5 | Fixed in this round |
| Cache | Deck underside 3 m or more; 4 bands; 3–4 living-tree legs until peeled and banded; walkway stops 1 block short | Building pieces › Raised cache; More Bushcraft › Walkway | Ready |
| Woodshed | 60 armloads; dries ×2 | Building pieces › Woodshed; Trees › Drying | Ready |
| Cabin, log by log | 1 m grid; 5 × 6 blocks; log wall block = 3.3 course-metres, 50 min; ghost, frame, hold right mouse; skid-pole slip 10% → 2% | Placing and snapping; Minecraft engine › The 1 m grid; Blueprint › Building a cabin | Fixed in this round |
| Queued jobs and blueprints | Queue of 8 (12 in a workshop) at the work-until-done rate; K, two corners, 400 blocks | Queued jobs; Placing and snapping › Blueprints | Ready |
| Roof, chimney, door, stove | Sod roof needs ridge and purlins; chimney 0.6 m above ridge; plank door 6 h; barrel stove 2 h to install | Building pieces; Structures › Structural support | Ready |
| Warm rooms and chinking | Flood fill over 1 m cells, 50 floor cells; chinked +26 to +32 °C on 2 armloads; unchinked needs 5 | Warm rooms | Ready |
| Upkeep and decay | Shrinkage re-opens seams on Sept 1; rot 10% of remaining HP (37% at 10 years) × site factors; snow\_cover roof load | Building pieces › Damage; Structures › Rot, Snow load, Repair | Fixed in this round |
| Fire risk | Packed fire 10% an hour within 2 m; creosote +1/+2/+5, chimney fire 3%/h above 70 on Hot | Blueprint › Spread; Warm rooms › Creosote; Structures › Fire risk | Fixed in this round |
| Recipe graph | Every input made by something: added billet, strips, ribs, log halves, birch branches, chopping block, stakes, sand | Trees › Limbing and bucking; Recipes; Data and tests › Graph test | Fixed in this round |
| Tier order | Hatchet (Start) → bow drill → fire opens Bushcraft (workbench, stone axe) → iron (felling axe, maul, wedge, cant hook) → Homestead bucksaw | Tiers and gates; Tool durability | Ready |
| Wood upgrade | Split all after 10 armloads; felling axe, bucksaw, maul | Trees | Decided by the Director (RECONCILE 125): "Split all" unlocks after the first 10 armloads split by hand; every strike before that stays manual |
