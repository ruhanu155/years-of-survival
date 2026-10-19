# Decay and time

Everything in the valley ages on one clock: meat turns, hides slip, iron rusts, logs go punky and roofs fall in. This tab joins the blueprint's scattered decay rules into one model and fills the gaps, from frozen moose to 30-year-old camps. Cold is the hinge: summer is the rot season, winter is the freezer, and the spring thaw restarts every clock.

## How this tab fits

**Decay already exists in pieces; this tab owns the model, not every number.** The rules below stay where they are. This tab names each one as an instance of the shared clock and adopts its numbers unless the row says otherwise.

| Existing rule | Where it lives | Number adopted |
| --- | --- | --- |
| Food freshness, L(T), food k, place m, stages, preserved shelf lives | Blueprint › Spoilage model, Preserving in detail | All, unchanged |
| Organ timers, gutting deadline, quartering above 10 °C, bone sour, green hide 3 days | Blueprint › Field care | All; green hide extended to 0–5 °C at half rate |
| Pelt slips after 12 game hours above 5 °C on the carcass | Blueprint › Butchering and yields | Unchanged |
| Carcass stages, t\_bloat, size factor, rigor, bones in 20 or 40 days, bone piles 2 years, antlers 4 | Bestiary › Carcasses and bodies | All, unchanged |
| Garment wear, repair 40%, hides worked with fat once a season | Blueprint › Clothing | Unchanged |
| Tool durability and edge, cold wear | Crafting and building in depth › Tool durability | Unchanged |
| Rust, cordage rot, mice in chests, fuel evaporation, buried items | Items › Item decay | Unchanged |
| Rot rates 10% and 2% a year, multipliers, the four rot words, Sagging | Structures and buildables › Rot, Structural support | Kept; the soil multiplier is split in two (below) |
| Deadwood classes Sound, Punky, Nurse log; fatwood stumps | Land and seasons in play › Deadwood decay | Unchanged; now derived from the formula |
| Firewood moisture, punky rounds after 1 wet year | Every concept from the video › Firewood drying | Unchanged |
| Gut items and gut piles | Every concept from the video › Game guts | Unchanged |
| Winter's dead | Every concept from the video › Winter's dead | Kept, with bones lasting 2 years, not 1 |
| Old camps at 1, 2, 3, 5 and 10 years | Creative concepts › 15. The valley remembers | Unchanged; extended to 20 and 30 |
| The old camp option, aged 10 years | Ideas from viewers › The old camp | Kept; its log figure becomes a computed result |
| Regrowth, worn trails, cut trails, chinking and sod upkeep | Blueprint › The open world; Open world in depth › Outposts | Unchanged |
| Quinzhee life, ice house melt | Structures and buildables › Snow, ice and trails, Food and storage | Unchanged |

**What this tab adds.**

- One decay clock with three drivers (living rot, chemical, physical) and seven factors.
- The cold curve: how decay slows through fall, stops in deep winter, and restarts at the thaw.
- A decay table for 14 classes of thing, each with stages, reference times, accelerants, stoppers and what it becomes.
- The effects of eating food at each freshness stage, using the existing illness names.
- Nine resources made by decay, decay tells, and maintenance loops under the tedium rule.
- The valley after 2, 5, 10 and 30 years (the SOS stay-out of 1, 2 or 5 years, the 10-year old camp and set-piece ages), and the world's slow cycles.
- One storage design for Minecraft: lazy item stamps, block decay settled in closed form, area decay in chunk attachments.

## One decay model

### The decay clock

**Every decaying thing carries one number, p, from 0 to 1.** At 0 it is new; at 1 it has become something else. Food freshness is the same number read the other way: freshness = 100 × (1 − p).

**Three drivers move p.** Each decay class in data picks one.

| Driver | What it models | Main factors | Examples |
| --- | --- | --- | --- |
| Living rot | Microbes, fungi, insects | Temperature, moisture, ground contact, bark, pests | Food, carcasses, hides, wood, cordage, hay, fur |
| Chemical | Rust and oxidation, rancid fat, evaporation | Liquid water, temperature, protection | Iron, tin, lard past its life, avgas |
| Physical | Wear, frost, rain, melt, load | Uses, freeze-thaw cycles, rain millimetres, melt degree-days, snow load | Tools, garments, mud bricks, stone, snow, ice |

**Seven factors scale the rate.** Each class lists which apply, and every factor not listed is 1.

| Factor | Symbol | Values |
| --- | --- | --- |
| Temperature | f\_T | The cold curve below, for living rot; 0 below 0 °C for rust |
| Moisture | f\_W | Dry 0.5, damp 1, wet 1.5–2, bog or splash zone 4, waterlogged with no air 0.2 |
| Sun | f\_S | Food in sun 1.5 (the existing m); fabric and rawhide in summer sun 1.5; wood and stone 1 (sun only greys them) |
| Ground | f\_G | Off the ground 1; on soil 1.5; on soil within 3 blocks of a heated room 3; on soil in forest shade 0.9 |
| Biota | f\_B | Flies on uncovered meat above 10 °C 1.5; bark on wood 5; moths on stored fur and wool in summer 1; mice in an unguarded chest (the Items rate) |
| Use | f\_U | One use takes 1 ÷ durability (Tool durability) |
| Protection | f\_P | Salt 0.5 on food; oil on iron 0 for 30 days; a charred post end 0.5; a roof and dry walls 0.5; frozen 0 |

**The formula family.** One line covers every class. τ is the class's reference life, from p = 0 to p = 1. Reference conditions are 5 °C, damp, shade, off the ground, no pests and no protection.

```latex
\frac{dp}{dt} = \frac{g(p)}{\tau_c} \cdot f_T(T) \cdot f_W \cdot f_S \cdot f_G \cdot f_B \cdot f_P \qquad g(p) = \begin{cases} 1 & \text{linear classes} \\ 1 - p & \text{proportional classes} \end{cases} \qquad \Delta p_{use} = \frac{1}{N_{uses}}
```

**Two shapes, one family.** Linear classes lose the same amount each hour: food, hides, hay, metal, clay, snow. Proportional classes lose a share of what is left: wood, bark, bone and antler. A proportional class never quite reaches 1, so its last stage starts at 0.97.

**Food keeps its own form.** The Spoilage model's safe life L is the time to p = 0.7, so τ = L ÷ 0.7 at 5 °C. Its place factor m is the product of f\_W, f\_S and f\_G. The two forms give the same numbers to the freshness point.

**The decay-day.** Slow classes count time in decay-days: one day at 5 °C, damp. The Standard valley year sums to about 390 decay-days, almost all of them from May to September. So "10% a year" in Structures means 10% per 390 decay-days, and a warm year rots more than a cold one.

| Month | Mean air | f\_T | Decay-days |
| --- | --- | --- | --- |
| May | 9 °C | 1.55 | 48 |
| June | 15 °C | 3.0 | 90 |
| July | 17 °C | 3.74 | 116 |
| August | 14 °C | 2.69 | 83 |
| September | 8 °C | 1.39 | 42 |
| October and April | About −2 °C | 0.2 | 12 |
| November to March | −12 to −22 °C | 0 | 0 |

### Stages and looks

**Every class has five stage slots, and each class names them.** The thresholds come from the source rule, so food keeps its 60, 30 and 10 freshness lines and wood keeps Structures' quartiles. A stage always shows three ways: a word, a look and, for smelly classes, a smell line.

| Class | 1 | 2 | 3 | 4 | 5 (p = 1) | Thresholds on p |
| --- | --- | --- | --- | --- | --- | --- |
| Food | Fresh | Aging | Turning | Spoiled | Rotten | 0.4, 0.7, 0.9, 1.0 |
| Carcass | Fresh | Bloated | Rotting | Bones | Bone pile | Bestiary times |
| Hide and fur | Green | Tainted | Slipping | Rotten | Lost | 0.3, 0.6, 0.9, 1.0 |
| Built wood | Sound | Weathered | Soft | Rotten | Collapsed | 0.25, 0.5, 0.75; collapse at u or 0.97 |
| Deadfall | Sound | Punky | Nurse log | Moss mound | Soil | 0.3, 0.7, 0.9, 0.97 |
| Iron and tin | Bright | Tarnished | Rusted | Pitted | Rusted through | 0.1, 0.3, 0.6, 1.0 |
| Fabric, leather | Good | Worn | Frayed | Rags | Scraps | 0.3, 0.7, 0.9, 1.0 |
| Plant material | Fresh | Brown | Brittle | Rotten | Duff | 0.3, 0.6, 0.9, 1.0 |
| Unfired clay, mud brick | Sound | Pitted | Eroded | Slumped | Mud | 0.25, 0.5, 0.75, 1.0 |
| Snow and ice | Firm | Glazed | Sagging | Thin | Melted | Class rules |

**Looks are textures, not effects.** Each slot has an item texture variant or a block state variant: meat reddens, browns, then greys green; wood bleaches, then darkens and splits; iron goes orange, then flaky black. A slot change never happens silently on something the player owns: the tooltip word changes the same tick.

### Catch-up and determinism

**Nothing decays by being watched.** A thing seen every minute and a thing in an unloaded chunk for 3 years must end in the same state. Three rules guarantee it.

1. **Closed form.** Each class integrates its factors over the climate record, not over ticks. The weather stage draws each year's 365 days on May 1, so every past hour's temperature, rain and snow depth is known.
2. **Prefix sums.** On May 1 the game builds running totals per environment class: decay-days, wet days, rain millimetres, freeze-thaw cycles, melt degree-days and sun hours. Any interval then costs two lookups: total at the end minus total at the start.
3. **Thresholds, not rolls.** Anything that happens once, such as a roof block falling, draws its own threshold u when placed. The draw is hash(seed, "decay", position, placed day). It happens when p passes u. No roll depends on when the player looked.

**Environment classes.** Things decay at the temperature of where they sit. The classes are open air, forest shade, under snow, heated room, unheated building, root cellar, ice house, snow pit, permafrost cellar, river and lake.

A heated room reads its own hourly record. A thing that changes place settles first, then carries on in the new class.

## Cold, snow and the thaw

**Cold is the valley's preservative, and the calendar decides how much of it there is.** In the video, the moose meat that would rot in about 2 summer days read "spoils in 58 days" in the deep cold. Year of Ice keeps that feeling: decay slows steeply through the fall, stops in deep winter, and every clock restarts at breakup.

### The cold curve

**Living rot follows one temperature factor.** It is the Spoilage model's curve, written as a rate: each 10 °C colder divides the rate by 3, until the water in the food freezes.

```latex
f_T(T) = \begin{cases} 3^{(T-5)/10} & T \ge 0\,°C \\ 0.2 & -5 \le T < 0\,°C \ (\text{half-frozen}) \\ 0 & T < -5\,°C \ (\text{frozen}) \end{cases}
```

**What it means for a moose quarter.** Gutted meat in a closed cache, k = 1.0, m = 1.0. Turning is the end of safe eating; Rotten is p = 1.

| Air where the meat sits | f\_T | Safe until Turning | Rotten after | When the valley sits here |
| --- | --- | --- | --- | --- |
| 20 °C | 5.2 | 1.2 days | 1.7 days | A July afternoon, or a heated cabin all year |
| 15 °C | 3.0 | 2.0 days | 2.9 days | Summer mean |
| 10 °C | 1.7 | 3.5 days | 4.9 days | Early June, early September |
| 5 °C | 1.0 | 6.0 days | 8.6 days | Late September; the under-floor cellar |
| 0 °C | 0.58 | 10.4 days | 14.9 days | Mid-October; a snow pit |
| −1 to −5 °C | 0.2 | 30 days | 43 days | Late October; the ice cave's air |
| Below −5 °C | 0 | Stops | Stops | November to early April outdoors |

**Frozen means stopped, and the tooltip turns into a thaw countdown.** A frozen item reads "Frozen · keeps until the thaw, about 58 days". The figure is the days until the store's normal thaw date plus the safe life left at the cool rate. A fresh quarter hung on February 18 shows about 58 days, as in the video.

- **Normal thaw date.** The first day the 7-day mean air passes −5 °C in the climate normal: about April 10 for a meat pole in the open, April 25 for a north-side snow-covered cache, never for the permafrost cellar.
- **The real year can differ.** The countdown uses the normal, not the drawn year, so it never leaks the weather. Once the 3-day forecast shows a thaw, the line changes: "Thaw in the forecast. Spoils in about 9 days."

### Snow as a cold store

**Snow over food damps the air.** A store under 30 cm of snow or more sits at the 7-day mean of the air, capped at 0 °C. The hour's air no longer reaches it. That is why the old trappers buried meat in drifts.

| Situation | Open meat pole | Cache under 30 cm or more of snow |
| --- | --- | --- |
| Midwinter, −25 °C nights, −18 °C days | Frozen | Frozen |
| A 2-day winter thaw at +3 °C inside a −15 °C week (The warm wind) | The surface thaws for about 15 hours: 8% of safe life lost | 7-day mean near −11 °C: stays frozen |
| Early April, days +2 °C, nights −12 °C | Thawing by day once the mean passes −5 °C | Frozen until the 7-day mean passes −5 °C, about 2 weeks later |
| May, snow gone from the open | 9 °C mean: 3.9 days safe | North-side drift still holds 0 °C: 10.4 days |

**The snow pit stays as written.** A snow pit holds 0 °C while snow remains, into June on the north side of a hill (Preserving in detail). Snow cover and the pit are the same physics; the pit is snow packed on purpose.

### The warm cabin trap

**The warmest place in the valley is the worst place for meat.** A heated room is its own environment class at the room's temperature. The same January quarter keeps for months outside and barely a day by the stove.

| Where a January quarter sits | Its temperature | Safe life |
| --- | --- | --- |
| Meat pole or raised cache outside | −25 °C | Stopped until the thaw |
| Unheated cold porch or woodshed | About −15 °C | Stopped |
| Under-floor cellar of a heated cabin | 2–6 °C | 5–8 days |
| Shelf in the heated cabin | 20 °C | 1.2 days |
| By the stove, within 2 m | 30 °C | Under 0.5 days |

**The fix is a habit, not a chore.** Thaw only a day's food by the fire (1 game hour per kg, Spoilage model). The store view flags any frozen food carried into a warm room: "Moose, 12 kg, thawing indoors. Eat or return it by tomorrow." Auto-eat takes indoor food first.

### The fall slowdown

**Cold buys time at the kill.** The kill date decides how long the meat waits, which is the other half of the rut-fat trade in Butchering and yields.

| Kill date | Typical mean air | Gutted meat, safe | What it costs |
| --- | --- | --- | --- |
| September 10 | 9 °C | 3.9 days | Flies above 10 °C at midday; quarter within 4 hours |
| October 1 | 3 °C | 7.5 days | Short days for the haul |
| October 20 | −3 °C | 30 days | A rutted-out bull carries 50% of its fat |
| November 5 | −10 °C | Freezes overnight | Frozen meat needs the hatchet; the hide freezes on |

### The thaw

**Breakup restarts every clock at once.** The existing rule stands (A winter of frozen meat, in The open world). When air first passes −5 °C in April, every unsmoked frozen item starts at the cool rate of 6 days at 5 °C. It must be smoked, dried or eaten within a week.

This is the year's food pressure point.

**What the player can do about it.** Each answer ties to a system the player built, so the thaw rewards a year of preparation.

| Answer | Capacity | Time | Keeps it for |
| --- | --- | --- | --- |
| Cold smoke in the smokehouse | 60 kg a batch | 2–4 game days | 180 days |
| Dry on racks in April wind | 15 kg a rack | 2 dry days; April is often dry | 60 days |
| Move it to the ice house | 27 slots at 1 °C | Hauling only | About 9 days per load, all summer |
| Move it to the permafrost cellar | 45 slots at −6 °C | Hauling only | Frozen all summer |
| North-side snow pit | 27 slots | 1 game hour to dig in a drift | 0 °C into June |
| Render the fat | 1 kg an hour | Low fire | Lard: 180 days |
| Feed the wolf team, set bear bait, or Feast | Any | — | Calories now, not later |

**Messenger lines.** The existing warning fires once per store: "Above freezing for 2 days: 140 kg of meat in the north cache will start to age." A new morning prompt, `thaw_coming` (priority 1), fires on the first forecast thaw: "Thaw in 3 days. 140 kg frozen in the north cache. The smokehouse holds 60."

## Eating decayed food

**Decayed food pays now and bills later, through the illnesses that already exist.** The effects are gut illness, parasites and the nausea meter (Health, medicine and nutrition; Ideas from viewers › Sick stomach). "Food poisoning" in the video's wording is gut illness in the rules. One narrow new effect, botulism, covers sealed fermented fish.

### The existing effects, in one place

| Effect | Onset | What it does | What helps |
| --- | --- | --- | --- |
| Gut illness | 1–2 game days for spoiled food or mouse droppings (20% a meal from a stack flagged with droppings, Items › Item decay) | 2 days: half the calories absorbed, 1 extra litre of water drained a day, energy cap 7, woken twice a night, fever on day 1, nausea +15 a game hour for 12 hours | Boiled water, broth, tea, a full night in bed; water under 30 adds a day. Remedies never cut it below 12 game hours (RECONCILE 120 C5) |
| Nausea and vomiting | At once | At 50 the view sways; at 100 the player vomits: 50% of the last 3 hours' calories and 0.5 L of water lost | Rest; tea |
| Parasites, tapeworm | 3–5 game days | 15% fewer calories absorbed until treated | Supply-drop medicine |
| Parasites, trichinella | 3–5 game days | As tapeworm, plus −10 Condition and Condition capped at 90 | Supply-drop medicine |

**Weakness makes it worse (new).** Eating while Condition is under 50 or BMI under 18.5 multiplies any gut-illness chance by 1.25, to at most 95%. The illness also lasts 12 game hours longer. It stacks with the existing water rule. A starving player is the one most tempted by Turning meat, and the one it hurts most.

### Chances by food and stage

**One ladder per food family.** Fresh and Aging follow the Spoilage model as written. The new numbers fill the cells it left open: raw Turning meat, spoiled plant foods, stale smoked food and rancid fat.

| Food | Fresh | Aging | Turning | Spoiled | Rotten |
| --- | --- | --- | --- | --- | --- |
| Game meat and organs, cooked | Safe | Safe | 30% gut illness; 15% if stewed | 80% gut illness | Removed |
| Game meat and organs, raw | Tapeworm: game 5%; trichinella: bear 25% (RECONCILE 120 C4) | Parasites ×2; +10% gut illness | Parasites ×2; 50% gut illness; nausea +40 at once (new) | 80% gut illness; nausea +60 at once (new) | Removed |
| Fish, cooked | Safe | Safe | 30% gut illness; 15% in fish-head soup | 80% gut illness | Removed |
| Fish, raw | Tapeworm 2%; 0% after 7 days below −20 °C | Tapeworm ×2; +10% gut illness | As raw meat | As raw meat | Removed |
| Bear meat | Done at twice the time, or stewed; freezing never makes it safe | As game | As game | As game | Removed |
| Cooked dishes and leftovers | Safe | Safe | 30% gut illness | 80% gut illness | Removed |
| Eggs | Safe cooked | Safe cooked | 30% gut illness | 80% gut illness | Removed |
| Fresh berries | Safe | Safe | Fermented: nausea +40 and 20% gut illness for 12 hours (Berry country) | Mouldy: 50% gut illness (new) | Removed |
| Greens, spruce tips | Safe | Vitamin C ×0.5 (new) | 30% gut illness; vitamin C 0 | Slimy: removed | Removed |
| Mushrooms | Lookalike rules | 10% gut illness raw | 30% gut illness, even cooked (new) | 80% gut illness | Removed |
| Potatoes, roots | Safe | Safe | Soft: 20% of the weight cut away as rot, then safe | Removed | Removed |

**Preserved food fails differently.** It never jumps to 80%, because smoke, drying and fat keep most microbes out.

| Preserved food | Past its shelf life ("Stale") | Stored damp ("Mouldy") |
| --- | --- | --- |
| Hot-smoked, cold-smoked, dried strips | Calories ×0.9; 10% gut illness | 30% gut illness. Trimming the mould (1 game minute) loses 20% of the weight and drops it to 10% |
| Pemmican, lard, rendered fat | Rancid: no gut illness; nausea +30 a meal, calories ×0.8, Spirits −1 | As stale |
| Flour, sugar, tea | Items › Wet: flour spoils in 10 days above wet 30 | — |
| Sauerkraut | After May 1: soft and sour, 10% gut illness | — |

### What cooking fixes and what it does not

| Risk | Rare or Done cooking | A boil of 10 game minutes or more | Freezing |
| --- | --- | --- | --- |
| Parasites, game and fish | Rare halves it; Done removes it | Removes it | Fish: removed after 7 days below −20 °C |
| Trichinella in bear | Only Done at twice the time | Stewing removes it | Never |
| Aging food's raw gut-illness chance | Removes it | Removes it | — |
| Turning food | Down to 30% | Stew: down to 15% | — |
| Spoiled food | No help: 80% | No help: 80% | — |
| Rancid fat | No help | No help | Stops it getting worse |
| Mouse droppings | No help | No help | — |
| Botulism toxin | No help | Destroys it | No help |

**The lesson is the old one.** Heat kills living things but not most of the poisons they leave, so stew halves the Turning risk and does nothing for Spoiled meat. Botulism's toxin is the one a long boil breaks.

### Sour fish and botulism

**Fermented fish is a real northern food, and it has one deadly way to go wrong.** Fish heads, eggs or whole small fish fermented cool in a grass-lined pit or a birch-bark basket become sour fish. Sealed airtight, they can grow botulism.

1. **Making it.** Layer 10 heads or 2 kg of eggs in grass, in a 1-block pit or a birch-bark basket. Hold them at 3–10 °C for 10 game days. The result keeps 20 days and gives 115% of the parts' calories, as a stew of the same heads does (Blueprint › Cooking), with the heads' vitamin C kept. It needs no salt, smoke or firewood.
2. **The wrong container.** Packed in a tin with its lid, a glass jar, the jerrycan or anything plastic, each batch rolls 10% toxic. A grass pit or bark basket never does, because air reaches it.
3. **The only tell.** Toxic sour fish smells exactly like good sour fish; the sniff says "Sour, as it should be." A tin batch shows a bulging lid on its block model and icon. The container is the warning.
4. **Eating it.** Onset after 12–36 game hours. Nausea +60, energy capped at 3, and −10 Condition a game day for 5 days. No medicine treats it; rest in a bed halves each day's loss. A weak player can reach Condition 0 and the SOS rules.
5. **The fix.** Boiling the batch 10 game minutes before eating destroys the toxin, but it tastes flat and earns no first-dish Spirits.

### How the player tells the stage

**Everything readable applies to food.** Five tells work without any knowledge, and the player never has to guess from colour alone.

| Stage | Tooltip line | Icon | Smell line in the world | Sniff (sneak and use, 1 real second) |
| --- | --- | --- | --- | --- |
| Fresh | "Fresh · safe 4.2 days" | Natural colour, green bar | None | "Smells clean." |
| Aging | "Aging · safe 1.1 days" | Slightly dull, amber bar | None | "Fine. Cook it." |
| Turning | "Turning · cook it or bait it" | Brown edge, red bar, one wavy line over the icon | One faint grey wave above it at 3 m | "Sour. Turning." |
| Spoiled | "Spoiled · too far gone" | Grey-green, two wavy lines | Two green waves and, above 10 °C, a fly speck cloud | "Rotten. Don't." |
| Rotten | Removed | — | A scent point for 24 game hours | — |

**Unknown food reads "Unknown".** Meat from a found carcass, a fox cache or a raided store shows "Unknown freshness" until sniffed or cut. The carcass inspect line from Winter's dead ("Thawed. Rotten.") counts as a sniff.

**The morning prompt still names the worst item** ("Moose liver turns tonight"), and auto-eat never picks Turning or worse food unless the ration is Starvation. Eating Turning food by hand asks once: "Turning. Eat anyway?"

## The decay table

**Every class of thing, on one page each.** Times are at reference conditions: 5 °C, damp, shade, off the ground, no pests, unprotected, unless the row says otherwise. Slow classes give valley years of about 390 decay-days. The Becomes column is what p = 1 turns into; nothing simply vanishes unless the row says so.

### Food

**The Spoilage model owns these numbers.** This table restates them as decay classes and adds the fermented and frozen rows.

| Food | Stages | Safe life at 5 °C (to Turning) | Speeds it | Stops or slows it | Becomes |
| --- | --- | --- | --- | --- | --- |
| Raw meat, cleaned fish | Fresh, Aging, Turning, Spoiled, Rotten | 6 days (k 1.0); boned ×1.2 faster | Warmth, sun ×1.5, ground ×1.3, flies ×1.5 above 10 °C | Hanging in moving air ×0.8; salt ×0.5; a 12-hour dry crust stops flies; frozen below −5 °C | Spoiled: bait and wolf food at 70%. Rotten: a 24-hour scent point, then compost |
| Organs | As meat | Liver 1.8, kidney 2.4, heart 3.6, tongue 4.8 days | As meat | As meat | As meat |
| Whole uncleaned fish | As meat | 3.6 days (k 0.6); belly burn above 10 °C after 12 hours | As meat | Stringer: alive and not spoiling for 12 hours | As meat; maggots above 10 °C |
| Cooked meat or fish | As meat | 9 days (k 1.5) | As meat | As meat | As meat |
| Hot-smoked | Fixed life, then Stale or Mouldy | 30 days cool and dry; 14 above 15 °C | Damp halves it, then mould | Salt doubles it; frozen stops it | Stale food, then bait |
| Cold-smoked, air-dried | As hot-smoked | 180 days; 90 above 15 °C | Damp halves it | As hot-smoked | As hot-smoked |
| Dried strips | As hot-smoked | 60 days; 30 above 15 °C | Rain on the rack: +6 hours of drying an hour; damp halves it | As hot-smoked | As hot-smoked |
| Pemmican | Fixed life, then Rancid | 365 days; 180 above 15 °C or damp | Mice in an unguarded store | Hide or salmon-skin bag; frozen | Rancid pemmican: wolf food at full value |
| Lard | Fixed life, then Rancid | 180 days; 60 above 15 °C | Light and warmth | Frozen; a sealed tin | Rancid lard: lamp fuel and soap at full value |
| Frozen anything | Frozen flag over its stage | Stops below −5 °C; 30 days half-frozen from −5 to 0 °C | Each warm spell; a heated room | Snow cover; permafrost cellar; ice floor | Resumes its stage at the thaw |
| Sauerkraut | Fermenting 21 days, then Ready | Until May 1 | Warmth above 15 °C | The barrel | Soft kraut: 10% gut illness |
| Sour fish (new) | Fermenting 10 days, then Ready, then Spoiled | 20 days at 3–10 °C | Above 10 °C halves it | Frozen | Bait; botulism risk only in sealed containers |
| Fermented berries | Berry country rule | When Turning above 0 °C | Warmth | Freezing; drying to leather | Mash for berry leather |
| Potatoes, roots | As food | 150 days; 6 months in a 3 °C cellar | Light (greening), freezing (mush) | Root cellar 2–4 °C; clamp under 40 cm of snow | Sprouted seed potatoes in May (planting stock) |

**Ice floors freeze food.** Food laid directly on a glare-ice floor counts as −6 °C, whatever the air. This keeps the ice cave's promise of a summer freezer while its air stays at −2 °C.

### Carcasses and gut piles

**The Bestiary owns the carcass clock; the decay model runs it.** An unopened carcass is the class `carcass_unopened` with k = 0.6 × the size factor s; gutting switches it to `meat` at k = 1.0. Every number below is from Bestiary › Carcasses and bodies or Every concept from the video.

| Thing | Stages | Reference times | Speeds it | Stops it | Becomes |
| --- | --- | --- | --- | --- | --- |
| Unopened carcass | Warm, Cooling, Stiff, Relaxed, Bloated, Rotting, Bones | Bloats at L(T) × 0.6 × s; Rotting at 1.29 × t\_bloat; meat gone at 1.43 × t\_bloat | Warmth; flies ×1.3 on Rotting; a big body's held heat | Below −5 °C core; cold water τ ×0.3 | Bone pile |
| Gutted carcass | Meat clock | Turning at L(T) | As meat | As meat | Quarters, or a bone pile |
| Soft tissue to bones | Rotting to Bones | 20 game days above 10 °C, 40 at 0–10 °C | Scavengers, by the eating order | Below 0 °C | `yearofice:bone_pile` |
| Bone pile | Fresh bone, grey, scattered | 2 years | Rodents gnaw from year 1 | — | Gone; leaves rich ground (Decay makes resources) |
| Antlers and horns | Whole, gnawed | 4 years; value halved from year 2 | Porcupines, voles | Taken indoors: never decay | Gone |
| Gut item | Meat clock | Turning after 1 game day above 5 °C | Warmth | Frozen keeps all winter as bait | Bait; garden |
| Gut pile block | Fresh, steaming, eaten | Rots away in 3 days in summer; lasts about 30 in winter while scavengers eat it | Ravens 1 kg a day each | Freezing stops rot, not scavengers | Rich ground for a summer |
| Winter's dead | Frozen, thawed, scavenged, bones | Meat edible only with under 48 game hours above 0 °C | The thaw | Snow cover | Bones last 2 years, as bone piles (not 1) |

### Hides and pelts

**A hide is food for microbes until it is dry, salted, frozen or tanned.** Its stages are Green, Tainted, Slipping, Rotten and Lost. Tainted hides still scrape; Slipping hides lose hair in patches and are good only for rawhide.

| Hide state | Reference life | Speeds it | Stops or slows it | Becomes |
| --- | --- | --- | --- | --- |
| Pelt still on the carcass | Slips after 12 game hours above 5 °C (existing) | Warmth; a big body's heat | Skinning; freezing on below −5 °C | Slipped hide: rawhide only |
| Green hide, off the carcass | Rots after 3 days above 5 °C (Field care); 6 days at 0–5 °C (new) | Folded flesh side in ×2; on the ground ×1.3 | Frozen below −5 °C waits; salt; scraping and drying | Rotten hide: bait and garden fertility |
| Salted green hide (new) | Holds 60 days | Rain washes the salt: each wet day −10 days | 0.1 kg salt per kg of hide: 3 kg for a moose, 20 g for a hare pelt | Rawhide or leather when worked |
| Scraped and dried rawhide | Stable while dry | Soaked over 3 days above 5 °C: rots on the green-hide clock (new); mice 5% a winter week in chests | Dry storage; a barrel | Lacing scraps below 10% condition |
| Stretched, dried fur pelt | Stable while cold and dry | Summer storage above 10 °C outside a sealed chest or barrel: moths and beetles cost 1 pelt grade per 60 days (new) | Airing in sun 1 game hour a month; the smokehouse loft; a Labrador tea bundle in the chest; winter outside | A moth-eaten pelt: mitten liners |
| Smoke-tanned leather | No rot; wear only | Fire: it scorches | — | Garment scraps |
| Ash-lye bath (new) | Slips the hair on purpose in 3 game days | Warmth | — | A hairless hide ready for rawhide or buckskin, with no hair scraping |

**Fur in garments.** A fur garment worn or stored wet 60 or more for 3 game days above 10 °C slips in patches: −20 condition, and its warmth falls in proportion. Dried by a fire before that, it loses nothing.

### Plant materials

| Material | Stages | Reference life | Speeds it | Stops or slows it | Becomes |
| --- | --- | --- | --- | --- | --- |
| Cut grass, cured hay | Fresh, Brown, Brittle, Rotten, Duff | Cured hay in a capped stack: 1 year. Uncured, wet: Rotten in 30 days | Rain on an uncapped stack; ground contact | Capping with bark or a pole roof; a pole floor under the stack | Mulch and compost |
| Wet hay that heats (new) | — | A stack built above 30% moisture heats for 10 game days | Size over 4 blocks | Building it dry; opening it to air | 1% a game day of catching fire inside: smoke from the stack first, 12 hours of warning |
| Spruce boughs on a bed | Fresh, Brown, Brittle | 30 days in summer, 90 in winter | Warmth | Frozen | Bed warmth falls +5 to +3 °C at Brown, +1 at Brittle |
| Birch bark sheets | Sound, Weathered, Brittle | 15 years on a roof; 30 years dry indoors | Ground contact | Dry storage | Tinder and roof patches |
| Spruce bark sheets | As birch | 3 years on a roof | Ground contact | — | Duff |
| Plant-fiber cordage and nets | Good, Weak, Rotten | Stored damp: −10% strength a month (Items) | A net left in water all summer: −5% a week (new) | Pulled and dried weekly: no loss; sinew and rawhide do not rot | Tinder and char-cloth fluff |
| Willow and spruce-root baskets | Sound to Rotten | 10 years dry; 2 years on the ground | Wet storage | Indoors dry | Kindling |
| Birch-bark boxes and containers | Sound, Cracked, Leaking | 20 years; pitch seams crack if struck below −30 °C | Fire | Repitching, 10 game minutes | Tinder |
| Moss chinking | Packed, Gapped | Redo each fall in years 1–2, then every 3 years (The open world) | Squirrels and voles pulling it, September–October | Clay daub over it | Unchinked seam |

### Wood

**One rule set covers logs, deadfall, stumps and every wood block.** The rates are Structures' base rates per valley year: an unpeeled spruce log loses 10% of what is left, a peeled one 2%. Wood is a proportional class, so it keeps 90% after a year at 10% and 37% after ten. The ground multiplier is now split in two, so forest deadfall and cabin sills follow one formula.

```latex
\mathrm{HP}(t) = e^{-r\,t} \qquad r = 0.02 \cdot f_{bark} \cdot f_{species} \cdot f_{size} \cdot f_G \cdot f_W \cdot f_P \ \text{per valley year} \qquad f_{bark} = 5 \text{ unpeeled},\ 1 \text{ peeled}
```

| Factor | Values |
| --- | --- |
| Species | Spruce 1, tamarack 0.6, aspen 2.5, birch 3, cottonwood 3, willow and alder 3 |
| Size | Stump 0.5; log 1; pole or plank under 15 cm 2; split firewood and rounds 12; sticks and boughs 20 |
| Ground (f\_G) | Off the ground 1; on moss 1; on soil 1.5; on soil within 3 blocks of a heated room 3 (Structures' ×3: drip line and warm soil); on soil in forest shade 0.9 |
| Moisture (f\_W) | Under a roof and dry 0.5; open air 1; sitting in wet moss 2; snow banked past April 15 2; bog or splash zone 4; fully under water 0.2; frozen in permafrost 0 |
| Protection (f\_P) | Post end charred 10 game minutes 0.5 of the ground rate; tin wrap 1 |

**What the rule gives.** The worked rows match the existing tabs.

| Wood | r a year | Stages reached | Matches |
| --- | --- | --- | --- |
| Forest deadfall, spruce, bark on | 0.02 × 5 × 0.9 = 0.09 | Punky at 4 years, nurse log at 13, moss mound at 26, soil at 39 | Land and seasons › Deadwood decay |
| Split firewood rounds on bare ground, wet | 0.02 × 12 × 1.5 = 0.36 | Punky in 1 year: heat ×0.6 | Firewood drying |
| Peeled spruce sill on soil by the heated cabin | 0.02 × 3 = 0.06 | Weathered at 5 years, Soft at 12, Rotten at 23 | Structures › Rot |
| Peeled spruce sill on a corner stone | 0.02 | Soft at 35 years, Rotten at 69 | Structures › Rot |
| Unpeeled sill on soil by the heated cabin | 0.1 × 3 = 0.3 | Rotten in under 5 years | The "peel your logs" lesson |
| Peeled wall log, open air | 0.02 | Weathered at 14 years, Soft at 35 | Arvid's upper walls after 55 years: Soft |
| Corduroy poles on bog moss | 0.02 × 2 × 4 = 0.16 | Rotten at about 9 years | Traversal: rots in 8 years |
| Boardwalk sleepers in wet moss | 0.02 × 2 × 2 = 0.08 | Rotten at 17 years; the planks above outlast them | Structures › Boardwalk: 15 years |
| Drowned spruce in a beaver pond | 0.02 × 5 × 0.2 = 0.02 | Standing snags for 50 years and more | The beaver valley's dead forest |
| Wood frozen in permafrost or tailings | 0 | Indefinite | The miners' sluice boards, sound at the frozen end |

**Stumps go two ways.** A spruce stump's sapwood rots in about 5 years while its heartwood fills with pitch: 30 years after felling or burning it is a fatwood stump (Land).

A birch stump has no pitch: r = 0.02 × 5 × 3 × 0.9 × 0.5 = 0.135. It is Punky at 3 years, crumbling by 9 and gone by 26. It grows tinder conk and grubs on the way. Every stump greys within its first year.

**Seasoned firewood decays only when it gets wet.** Moisture follows Firewood drying. Under cover it never rots; uncovered on the ground it is punky in 1 year and soil in about 10.

**Roofs are wood with load.** A load-bearing block falls when its p passes its own threshold u, drawn between 0.35 and 0.55. It carries weight, so it fails before it is fully rotten. Non-load blocks collapse at Rotten.

| Roof | Maintained | Unmaintained | Upkeep |
| --- | --- | --- | --- |
| Sod over poles and birch bark | Poles r = 0.04, kept dry by the sod | Sod erodes in 5 years; then leaks raise f\_W to 2: r = 0.08. Half the roof is down at 10 years and all of it by 13 | Re-sod every 5 years, 1 game day for the first cabin's 18 m² (The open world) |
| Shingle | 25-year life (Structures) | Shingles lift after 15 years: 1% of blocks lose a course per storm over 15 m/s | Replace lifted shingles; re-roof at 25 years |
| Pole and bark | Bark life: 3 years spruce, 15 birch | Leaks once the bark is Brittle | Re-bark |

### Metal

**Iron needs liquid water to rust.** Below 0 °C rust stops, so most rust happens on wet summer days. Carried and stored items follow Items › Item decay: wet over 24 game hours costs 2 edge a day and 1% condition a week. Placed and abandoned metal follows the slow table below.

| Metal thing | Stages | Rate a year (linear) | Speeds it | Stops it | Becomes |
| --- | --- | --- | --- | --- | --- |
| Iron tool, trap or wire on the ground outdoors | Bright, Tarnished, Rusted, Pitted, Rusted through | 3%: Tarnished at 3 years, Rusted at 10, Pitted at 20, through at 33 | Wet soil; ash paste; salt | Oil or lard on a carried item, 30 days | Rusted-through iron: scrap at 50% weight for the bloomery |
| Iron off the ground outdoors, or in a roofless ruin | As above | 1% | Rain pooling on it | Hanging it under an overhang | As above |
| Iron indoors, dry | As above | 0.2% | — | — | As above |
| Stove in a cold building | As above | 6% while the roof leaks and the pipe still stands, since rain runs down it; 1% once roof and pipe are down | A missing pipe cap | A cap; a fire now and then; cast iron ×0.3 | Works until Pitted, smoking 5% more at Rusted; 2 kg bar iron repairs it (the old camp's "rusted stove") |
| Tin plate (cans, cache bands, lanterns) | As above | 1.5% off the ground, 4.5% on it: through at 22 years | Ground contact | Indoors dry: 0.2% | Flakes |
| Snare wire and steel cable | As above | As iron | — | — | At Pitted, wire breaks on 30% of catches |
| Aluminium (the plane's skin) | Bright, Dull, Chalky | 0.2%: chalky in 47 years, never through | Steel rivets: the panel edge pits | — | Still the best sled-runner metal |
| Brass and copper (casings, the ring) | Bright, Green | Green in 2 years outdoors | — | Polishing with sand, 1 game minute | — |

**Rust takes the shine.** A spoon lure, spinner or tin lid that reaches Tarnished is no longer shiny: ravens stop stealing it and the lure's flash bonus falls by 10%. Sand polishes it back in 1 game minute.

### Stone, clay and mud brick

| Material | Stages | Reference rate | Speeds it | Stops it | Becomes |
| --- | --- | --- | --- | --- | --- |
| Granite, quartz, river cobble | — | No decay | — | — | — |
| Sandstone, shale | Sound, Cracked | Each spring with 20 or more freeze-thaw cycles, a wet block cracks at 1%: −50% HP | Water standing on it | A roof; dry footings | Gravel |
| Wet river stone in a fire | — | Cracks and spits (Building a campfire) | — | Dry stones | Gravel |
| Fired brick (kiln) | Sound, Spalled | 0.5% a spring if wet | As sandstone | A cap stone | Brick rubble |
| Unfired mud brick | Sound, Pitted, Eroded, Slumped, Mud | −2% HP per 10 mm of rain on an exposed face | Wind-driven rain on the weather side ×1.5 | An overhang or a cap stone ×0.1; a clay wash renewed yearly ×0.5 | Mud and clay, reusable |
| Clay daub on chinking | Packed, Washed | −1% per 10 mm of rain | Weather side | An overhang | Bare moss seam |
| Fire ring stones | Clean, Mossy | Moss at 2 years unused (Creative concepts) | Shade | Use | An old fire ring |

A mud-brick chimney without a cap loses about one course a summer to rain: 200 mm of summer rain on its top face is 40% of a block's HP. That is why the video's chimney needs a stone cap.

### Fabric and clothing

**Wear comes from use; rot and moths come from storage.** Use wear and the 40% repair stay as Clothing writes them.

| Garment or cloth | Wear from | Storage decay | Stops it | Becomes |
| --- | --- | --- | --- | --- |
| Any garment | Use, thorns, falls, attacks (Clothing) | — | Repair with needle and sinew | Scraps at 10% (Items › Salvaging) |
| Cloth or hide stored wet 60 or more in a closed container above 5 °C (new) | — | Mildew after 2 game days: −2% condition a day, "musty" in the tooltip | Drying before storing | Mildewed cloth; washing with soap clears the smell |
| Fur and wool in a room or an unsealed container above 10 °C, June to August (Birds and the air › Flying insects) | — | Moths: −2% condition a month; "moth-eaten" at 20% lost; fur warmth falls in step | A sealed (Good or Fine) chest or barrel; airing on the drying line 1 game hour a month; the smokehouse loft; a Labrador tea bundle in the chest | Liners and patches |
| Rawhide garments | Cracking | Worked with fat once a season (Clothing) | Fat | Lacing scraps |
| Red flagging on trail markers | — | Sun fades it to pink in 1 summer and white in 3 | Replacing it | Whiteout sighting falls from 40 to 30 blocks once white |

### Fires, embers and ash

**A fire decays after Out, too.** The fire state machine (Fire, water and light) ends at Out. Three ground states follow.

| State | Lasts | What it does | Becomes |
| --- | --- | --- | --- |
| Warm ash | 3 game hours after Out | +2 °C within 1 m; melts snow on it; steams in rain | Cold ash |
| Cold ash | Until rain or a year | Holds the ash the fire made: 0.1 kg per armload burned | Leached ash after 10 mm of rain: no lye left |
| Old fire ring | Moss at 2 years, a mossy circle at 5, a birch sapling at 10 (Creative concepts) | Inspect: "An old fire ring" | Pole birch by year 20 |

**Ash builds up.** A fire ring with over 3 kg of ash in it is choked: heat ×0.9 until it is scooped out (E hold, 5 real seconds), which gives the ash as items. A stove's ash pan holds 4 kg and is part of Mend all.

### Snow structures

| Structure | How it decays | Tell | Fix |
| --- | --- | --- | --- |
| Quinzhee | Lasts until 2 days above 0 °C (existing). Each occupied night with a candle sags the roof 1 cm; sun above −5 °C sags it 2 cm a day. When the roof is thinner than the 30 cm depth sticks, the existing 10% nightly drop-in chance applies | The depth sticks show through the ceiling as dark dots | Pile 30 cm of fresh snow on top: 30 game minutes |
| Quinzhee glaze | Every 7 occupied nights the inner wall glazes to ice: its +25 °C becomes +22 and it drips | A shine on the walls | Scrape it: 20 game minutes |
| Snow-block windbreak or wall | Loses 1 block of height per 2 days above 0 °C; wind over 15 m/s scours 10% | Rounded tops | Re-stack |
| Ice house | Insulated ice −2% a summer week; bare −10%; a blocked drain doubles it (Structures) | Water on the floor | Clear the drain; repack sawdust |
| Snow pit | Holds 0 °C until its snow is gone (Preserving) | Grey, sunken snow | Re-pack from a drift |
| Ice road and ice bridge | Lost at breakup; poles drift to snags | — | Rebuilt each fall |

### Trails, clearings and stumps

**The land closes over what the player opens, by the existing rules.** Worn paths drop a stage after 60 days without a pass; a cut trail unused for 3 years regrows to brush at ×1.1; cleared ground follows Regrowth (fireweed in year 1, birch at 2, spruce at 6). Plants and their patches regrow by the Plants and fungi tab.

**Blazes heal but stay readable (new detail).** A blaze cut on a living tree weeps pitch for a summer, greys by year 1, and scars over as a "catface" by year 20. It still works for navigation. Arvid's 1938–1971 blazes survive as catfaces along his old line; Land reading at Handy shows them at 20 m.

### Abandoned camps and outposts

**An unvisited camp ages by its pieces, not by a script.** Each piece runs its own class, so a well-built camp ages better than a careless one. The years-1-to-10 looks match Creative concepts › 15. The valley remembers.

| Piece | After 1 year | After 5 years | After 20 years |
| --- | --- | --- | --- |
| Bough bed, debris bed | Brown, needles dropped | A rotten mat under moss | Gone |
| Lean-to | Poles grey, boughs brown | Fallen since year 3; a marten den 30% a year | A heap of punky poles; fireweed |
| Fire ring | Cold ash, leached | A mossy circle | A birch pole 4 m tall in the ring |
| Meat pole, drying racks | Lashings rotten; the pole sags | Down | Duff |
| Raised cache on peeled legs on stones | Sound; the ladder pole grey | Sound; tin bands Tarnished | Legs standing, floor poles Soft and half fallen in, bands Rusted but holding |
| Woodpile | Covered: sound. Uncovered: punky | Punky; grubs; a red squirrel midden after 60 days | A moss ridge |
| Garden bed | Weeds and fireweed | Willow 2 m | Birch thicket |
| Sod-roofed cabin, peeled walls on corner stones | Chinking gaps; mice in the bunk; porcupine chews the door handle | Sod eroded, first leaks, 1–2 roof blocks Sagging; swallows under the eaves; a porcupine den 20% a year | Roof fully down since year 13; walls standing, Weathered, the top course Soft under the fallen sod; a birch 5 m tall through the floor; a black bear dens under the floor 10% a year |
| The same cabin with unpeeled sills on soil, lived in 3 years | Sills Soft | Sills Rotten; the walls lean; the door won't close | Bottom courses gone; two walls fallen outward |
| Stove left inside | Bright | Tarnished once the roof leaks | Rusted: 2 kg bar iron to repair |
| Tools left in the cabin | Bright | Bright; handles chewed by porcupines | Tarnished; handles punky where rain reached them |
| Food stores | Gone to mice and bears by the first fall, unless frozen or sealed | Gone | Gone |
| Hides and garments | In a chest, mice take them in the first winter (Items: 5% a winter week); in a barrel, moths only: 70% | Barrel: 40% | Scraps |
| Trails | Worn trail back to a faint path | Cut trails brushed in | A game trail where the path ran to water |

**Animals move in, by existing rules.** A fallen lean-to becomes a marten den 30% a year, squirrels midden in a woodshed left 60 days, and porcupines chew salty wood (Creative concepts). New here: a roofless cabin hosts a porcupine den 20% a year from year 2. After 15 years, a black bear dens under a collapsed floor 10% a year.

### The set pieces' own past

**Every set piece is authored new and aged at worldgen.** Stage 5 places the template. The AgeWorld pass (Data and build) then runs the decay model from the year of abandonment to the present, on a seeded stand-in climate. Ages count back from the story's present, which story data sets as `present_year`.

| Set piece | Abandoned | Age | What decay leaves, and why |
| --- | --- | --- | --- |
| Station Nine | Unwintered 1 year; built 15 years ago | 1 and 15 | North half of the roof broken by 90 cm of snow; wet, rotten old woodpile; scrap barrel rusted but sound (painted steel, indoor-dry) |
| Bush-plane wreck | 1979 | About 47 | Aluminium skin chalky but whole; steel rivets rusted, so 1 panel in 4 has pitted holes (75% salvage); plexiglass yellowed (light ×0.8); seat cushions shredded by mice (Crude, 50%) |
| Arvid Lund's cabin | 1971 | About 55 | Sod roof caved by year 13; bottom 2 courses on soil Rotten, upper walls Soft; the cache stands because its legs were peeled and set on stones; the stove rusted. The lynx-tally notches sit above 1.2 m on the door frame, out of reach of the porcupines that chewed the frame below |
| Fire lookout | 1984 | About 42 | The timber variant stands on stone footings; the collapsed variant is its sills on soil |
| Roadhouse | 1931 | About 95 | Walls down to sills in moss; the 60 kg cast-iron stove Rusted but whole, because cast iron is thick and works until Pitted |
| Kessler and Doane's camp | 1912 | About 114 | Bunkhouse collapsed; sluice boards sound where frozen in tailings, gone where they lay in the creek; Kessler's grave board leans on a cairn, its carved letters standing proud where the soft wood between them weathered away |
| Ice cave | 1909 hooks | About 117 | Hooks Pitted but holding; "D. 1909" sharp, because nothing rots at −2 °C |

**Reading the past is the payoff.** A player who knows the decay rules can date things by looking: a stump with fatwood is 30 years old, a catface blaze over 20, a chalky panel decades. The journal's Land reading lines use the same numbers.

### Player blueprints and placed decor

**Blueprints never decay.** A saved blueprint is a plan, not a thing, so stamping it in year 10 places new frames. What decays is what gets built.

| Placed thing | Indoors, heated or dry | Outdoors |
| --- | --- | --- |
| Antlers, skulls, horns | Never | Bone-pile rule: gnawed from year 2, gone at 4 |
| Hide rug | Mice in winter as Items; walked wear 1% a month | Rots as rawhide |
| Fine carved items, birch-bark boxes, baskets | Never | Their class above |
| Tin lantern | Indoor tin rate | 1.5% a year, faster on the ground |
| Keepsakes | Never (Items › Keepsakes) | Never |
| Mounted trophies on a wall | Never | — |

## Decay as gameplay

### Decay makes resources

**Nothing rots into nothing.** Every end state in the decay table feeds another system, so a careful player harvests decay instead of only fighting it.

| From | Becomes | Where and when | Use | Ties |
| --- | --- | --- | --- | --- |
| Punky logs and stumps | Punk wood; sawyer grubs and ant brood | Deadfall 4–13 years old; rounds left wet a year | Hide smoke, smudges, ember carrying (Land); grub food and bait (Ideas from viewers) | Wood decay, fishing, hides |
| Spruce stumps | Fatwood | 30 years after felling or burning | The wet-weather fire starter (Land) | Fire, the old camps' clearings |
| Carcass and bone pile | Bones, skull, antlers; then rich ground | At Bones; rich ground for 3 summers on the 3 × 3 blocks under a large or huge carcass (new) | Tools and trophies; on rich ground grass and fireweed grow ×2, which moose and hares graze; a garden bed dug there gets +10% yield | Bestiary, garden, animals |
| Fly-blown meat or carcass above 10 °C | Maggots (new): 20 g a search, E hold 1 game minute | Rotting stage | Bait: grayling and whitefish bites ×1.2 for 2 game days, then they pupate | Fishing, flies |
| Spoiled food | Bait and wolf food at 70% (existing) | Spoiled stage | Snares, cubbies, the bear stand, the team | Traps, taming |
| Rancid fat | Lamp fuel and soap at full value | After its shelf life | Light; soap (Items › Soap from fat and ash) | Light, hygiene |
| Ash | Lye, fertiliser, traction, early melt, deodoriser | 0.1 kg per armload burned | Below | Fire, garden, hides, hygiene |
| Rotten food, guts, hides, hay, manure | Compost | The compost heap (new, below) | Garden fertility | Garden, scent, bears |
| Rusted-through iron | Scrap at 50% weight | Old camps, the mining camp | Bloomery feed | Iron tier |

**Ash, the most useful leftover.** Cold, unleached ash is collected from a ring or stove ash pan.

- **Lye and soap.** Water poured through 2 kg of ash in a bark funnel gives lye for the soap recipe in Items.
- **Fertiliser.** 1 kg per garden bed counts toward the existing 30% fertility cap.
- **Traction.** 0.5 kg scattered on 10 m of icy path stops slips for 3 game days or until 2 cm of new snow.
- **Early melt.** 1 kg spread on a snow-covered bed in April clears it 7 days early: an earlier frost-free soil reading for the garden.
- **The outhouse.** A scoop after use halves its scent point and its flies.
- **Dehairing.** 3 kg of ash in the hide yard's soaking pit, with 20 L of water, slips a hide's hair in 3 game days. Hair scraping then takes ×0.3 time: decay put to work.

**The compost heap (new).** A Bushcraft pole bin, 2 × 2 × 1 blocks: 4 poles, 8 m of cordage, 30 game minutes, 225 kcal. It takes spoiled food, rotten meat, guts, rotten hides, hay, manure, sawdust and ash.

1. **Rot.** Contents turn to compost in 60 decay-days, so most of one summer. Frozen contents wait.
2. **Yield.** 1 compost item per 10 kg of input; 2 items in a bed count toward the 30% fertility cap.
3. **Risk.** Meat, fish or guts in an uncovered heap make it a scent point of strength 1. In a bear range a bear digs it 10% of nights. Twenty centimetres of soil or 1 kg of ash on top stops both.

### Smell and scavenger draw

**Decay is the main source of scent in camp.** Scent points follow Senses by species and the Bear rules; decay sets their strength. One multiplier covers every smelly class.

| Stage | Scent multiplier | Who it draws |
| --- | --- | --- |
| Fresh | ×1 | Ravens and jays to meat in view; foxes at night |
| Aging | ×1 | As Fresh |
| Turning, Bloated | ×2 | Flies above 10 °C; bears within 400 m |
| Spoiled, Rotting | ×3 | All of the above, and wolverines in winter |
| Rotten | A 24-hour point, then 0 | Flies, then nothing |

**Modifiers.** A closed container halves scent, frozen food halves its radius (existing), and a carcass covered or hung halves its point (Bestiary). The camp layout rules in Making camp do the rest: anything rotting near the bed brings the bear to the bed.

### Decay tells

**Every stage shows in five channels, and colour is never the only one.** Food tells are in How the player tells the stage; these cover everything else.

| Channel | What it shows | Examples |
| --- | --- | --- |
| Word | The stage word in the tooltip or the 2-second look | "Soft", "Pitted", "Slipping", "Glazed", "Moth-eaten" |
| Texture | Item icon variant or block state variant | Grey checked wood; black-flaked iron; patchy fur; sagging snow |
| Particles | Smell lines over smelly classes from Turning on; flies above 10 °C; drip under Sagging roofs | Two green waves over a rotting hide; a fly speck cloud over a gut pile |
| Sound | A material sound on contact or nearby | A hollow thud on punky wood; creaks from a Sagging roof; buzzing at 10 m; a crunch under rusted tin |
| Messenger | The upkeep page and the morning prompts | "Roof creaking under the load." "The bear hide on the frame is slipping." |

**A new morning prompt, `decay_due` (priority 2).** It names the single most urgent decay among things the player owns, at most once a day: "Green moose hide: scrape it by tomorrow evening." It never names something the player has not seen, by the knowledge gate in The open world.

### Maintenance loops under the tedium rule

**Pillar 2 caps upkeep, and decay obeys it.** A year-3 homestead spends 40–70 game hours a year on structure upkeep (Structures). Decay upkeep outside structures, for tools, hides, garments, nets and stores, must stay under 15 game hours a year. Every loop has a batch action, and every loop gets faster with a tool, a station or practice.

| Loop | Interval | Single job | Batch or automatic form | Skipped, it costs |
| --- | --- | --- | --- | --- |
| Oil iron tools | 30 days in the wet season | 1 game minute a tool | A tool rack (new, 4 poles and pegs, 10 tools): oiling the rack takes 5 game minutes for all; racked tools are off the ground and dry | Rust: edge and condition |
| Re-chink | Each fall, years 1–2; then every 3 years | 6 seam metres an hour | Mend all (Structures) | Drafts, more firewood |
| Re-sod the roof | Every 5 years | 1 game day for 18 m² | Mend all | Leaks, then a falling roof |
| Air furs and woollens | Monthly, June to August | 1 game hour, unattended | Hang them on the drying line or hide-yard frame on a sunny day: airing counts automatically | Moths |
| Dry nets | Weekly in open water | Pull and hang: part of checking the net | A net rack at the landing: a net pulled there dries itself | 5% strength a week |
| Work rawhide with fat | Once a season (Clothing) | 20 g of lard, 5 game minutes | All rawhide on the body panel in one action | Cracking |
| Cap the haystack and woodpile | Once, at building | — | A capped stack never needs it again | Mould, punky wood |
| Empty ash | About every 30 armloads | 5 real seconds | Ash pan in Mend all | Heat ×0.9 |
| Patch shingles and replace tin bands | When the upkeep page lists them | Per piece | Mend all | Leaks; wolverines |

**The tedium check.** The balance simulator's year-3 homestead logs every decay chore. A build fails if any loop recurs more than once a game week per item class. It also fails if the non-structure total passes 15 game hours a year.

### How decay serves pillars 4 and 6

**Pillar 4: the year changes the map.** Decay is seasonal, so the calendar shows on everything the player owns.

| Season | What decay does | What the player feels |
| --- | --- | --- |
| May–June | Clocks restart; buried items and winter's dead melt out; flies hatch | The thaw scramble; free finds in the snowmelt |
| July–August | The rot season: about 200 of the year's 390 decay-days | Meat lasts 2 days; smoke and dry everything |
| September | Hide season and the cooling race | Hunt late, keep longer, lose fat |
| October–April | The freezer: living rot stops; only physical decay works (snow load, frost, mice) | The cache becomes a bank; the cabin becomes the danger |

**Pillar 6: the world answers the player.** The player's own history shows on the land through decay. A kill site greens up for three summers; the first trail camp's ring grows a birch; a careless woodpile turns to grubs; the cabin a player abandons in year 2 is a roofless shell with a porcupine in it by year 10. The journal's Places remember lines (Creative concepts) fire at all of them.

## Decay over the long term

### Time away

**Three things bring the player back to an aged home.** The Endless SOS (14 days, existing), the Old camp option (always 10 years, Ideas from viewers), and the longer absence Ruhan approved on 2026-10-19. All three use the same AgeWorld pass, so the same home looks the same after the same time, whichever way the player left.

**Longer absence (decided by Ruhan, 2026-10-19).** In Endless only, the SOS screen offers "Stay out" for 14 days (default), 1, 2 or 5 years. The 10- and 30-year columns below serve the old camp and set-piece aging only. The missing years are drawn from the seed like any other, and populations run in the simulator core. The player returns on Pete's flight with the starting kit.

The journal writes one line per year away, from the world's events: "Year 6: a fire on the Burn Hills. Year 7: the beavers dammed the low creek."

### The home after 2, 5, 10 and 30 years

**What the player finds, by the rules above.** The example is the year-3 homestead (Structures › Example homesteads): a peeled-log stove cabin on corner stones with a sod roof, a raised cache, a smokehouse, a woodshed and a garden.

| What | 2 years | 5 years | 10 years | 30 years |
| --- | --- | --- | --- | --- |
| Cabin roof (sod) | Sound; fireweed on the sod | Sod eroded; first leaks | Half fallen in | Gone; a birch through the floor |
| Cabin walls | Chinking gaps | Gaps; porcupine-chewed corners | Sound; the top course Soft under fallen sod | Weathered and standing, roofless |
| Stove and tools | Bright | Tarnished | Rusted: 2 kg bar iron | Stove Pitted; tools Tarnished, handles gone |
| Raised cache | Sound; stores gone to mice | Sound; bands Tarnished | Floor poles Weathered | Floor fallen; legs standing |
| Smokehouse and racks | Racks down | Smokehouse roof leaking | Smokehouse fallen | A mossy clay flue trench |
| Woodshed and wood | Sound; dry wood | A squirrel midden | Roof down; wood punky | A moss ridge |
| Garden | Weeds and fireweed | Willow 2 m | Birch thicket | Young mixed forest |
| Clearing (stand age) | Meadow | Meadow, hares booming | Thicket: moose capacity ×2 | Young mixed: grouse ×1.3 |
| Worn trails | Faint paths | Game trails | Game trails | Game trails to the old water hole |
| Tamed wolf | Gone wild; if trust was 80 or more, 50% chance it greets the player in a home-range pack and returns at 40 trust | Wild; a 10% chance of a grey-faced wolf that does not flee | Gone | A pack still dens on the ridge |
| Valley | Moose back near K | Bog iron re-formed (3–5 years); deadwood ring near camp refilled | A hare cycle passed; beaver dams one stage higher or abandoned | Lichen half recovered; old burns now thickets |

**Endless's 14 days** run the same pass at its smallest scale: spoilage, mice, raids and fires going out, as Collapse and SOS says. **The Old camp** uses the 10-year column, with one change: the 30% rotten-log figure in Ideas from viewers becomes a computed result. A cabin built of unpeeled logs on soil sills reaches it; a peeled cabin on corner stones keeps nearly all its logs.

### The world's own slow cycles

**Decay is the fast half of the valley's long cycles.** These owners already set the slow half; this tab only lines them up on one clock.

| Cycle | Period | Owner |
| --- | --- | --- |
| Hare and lynx | About 10 years | Blueprint › Populations |
| Stand age and fire return | Burn to mature spruce in 70–150 years | Land and seasons › Stand age |
| Spruce beetle | After 2 warm summers, or windthrow | Land and seasons › Spruce beetle |
| Deadfall to soil | 39 years | This tab, Wood |
| Beaver colonies | Dam, pond, abandonment, meadow over 10–30 years | Blueprint › The open world; Water, calls and encounters |
| Lichen recovery | 50 years | Land and seasons › Lichen and caribou |
| Bog iron | 3–5 years | Blueprint › The open world |
| Channel shifts, bank erosion, thaw slumps | Years to decades | Water, calls and encounters |

## Data and build

### How decay is stored

**Nothing ticks to decay.** Every decaying thing stores a value and the game minute it was stamped, and settles in closed form when something reads it. Reads happen on interaction, on chunk load and on the day pass, so the cost follows what the player touches.

| What | Minecraft form | Settled when |
| --- | --- | --- |
| Food, bait and raw hides | Data component `yearofice:fresh` as Minecraft development › Layer 5 defines it: `state` (raw, cooked, smoked, dried), `clock` (a handle to the holder's decay clock), `due_dh` and `life_dh`, with `due` floored to 1/40 of life so stacks merge; one added field, `known`, is false for found food until sniffed or cut | The stack is used, moved, merged, eaten, or its container opens |
| Other items | Data component `yearofice:decay` (new): class id, `due_dh` and `life_dh` on the holder's clock as for `fresh`, and a `protected_until` minute (oil, salt). Rust, rot and moths settle into condition, which is vanilla durability, and `yearofice:edge` | As food |
| Use wear | Vanilla durability, as the engine sets condition | On use, as now |
| Firewood moisture | `yearofice:moisture` (Every concept from the video) | As food |
| Carried items | The lead's `body:<player>` clock (air at the player) and `pocket:<player>` clock (+20 °C while warmth is 30 or more); their heads live in SavedData `yearofice_decay`, so no separate attachment is needed | Rehomed in the body's hourly pass and on every move |
| Containers | The `room:<id>` clock of the room the block entity sits in, or a fixed cold-store clock: root cellar 3 °C, under-floor cellar 2–6 °C by the room, ice house 1 °C, ice cave −2 °C (its glare-ice floor −6 °C), permafrost cellar −6 °C (−2 °C on a south slope) | On open, and by formula while unloaded |
| Placed blocks without a block entity | Block state `decay` 0–4 for the look; curve, clock key, due and threshold u in the lead's sparse chunk attachment `yearofice:decay_marks` | The `ClockEvent.Day` pass, interaction, chunk load |
| Placed blocks with a block entity | Fields on the block entity (HP, due, stamp, u), as Structures' controllers already hold | As above |
| Area decay | The chunk attachment `yearofice:area_decay` (new): per 8 m cell, the last-visited day, rich-ground expiry, ash-scatter expiry; trail wear (2 m cells) and regrowth dates stay where World and worldgen keeps them | Chunk load |
| Carcasses | The `yearofice:carcass` entity attachment's own `fresh` (Bestiary; Layer 5) | Its hourly lifecycle step, which calls the same core |

**Display without ticking.** The client draws the freshness or decay bar from the stamp and a synced thermal summary. It uses the same pure function as the server. The server settles only when something changes, so a pack of 36 food stacks costs nothing per tick.

**The day pass chooses when, never what.** `randomTickSpeed` is 0 (World and worldgen › Random ticks and their replacements), so no block decays on a random tick. On `ClockEvent.Day` each loaded chunk's `yearofice:decay_marks` past due advance one stage through `BlockDecay.settle`, which computes p from the mark and the climate sums. A player touching a block settles it at once, so a roof block falls on the day its threshold passes or the moment it is used.

A daily pass on `ClockEvent.Day` settles any load-bearing block whose predicted threshold falls that day, so collapses never wait on chance.

**Catch-up on chunk load.** `ChunkEvent.Load` queues the chunk in `DecaySettler`. It settles stamps, block entities and area cells at 2 ms per chunk, spread over ticks, inside the engine's budget. The result equals a live run, because every interval is two prefix-sum lookups and every one-off event is a threshold.

### Climate prefix sums

**One table makes every catch-up cheap.** `sim.decay.DecayClimate` keeps running totals per environment class inside the lead's SavedData `yearofice_decay` (Layer 5): decay-days, wet days, rain millimetres, freeze-thaw cycles, melt degree-days and snow-covered days. It is built on May 1 from the year table. It keeps daily totals for this year and last, and monthly totals before that: about 100 KB after 30 years.

### Data files

| Data | Path | Holds |
| --- | --- | --- |
| Decay classes and curves | `data/yearofice/decay/<curve>.json`, entries only (Minecraft development › Data kinds) | Driver, shape, τ or r, factors used, stage thresholds and names, becomes, scent by stage, tells. Food curves: `living_rot` (meat, fish, organs, cooked food, eggs, berries, greens, mushrooms, roots, each with its k), and the fixed-life curves `hot_smoked`, `cold_smoked`, `dried`, `pemmican`, `lard`, `sauerkraut`, `sour_fish` |
| Cold curve | `data/yearofice/decay/living_rot.json`, a curve entry like the others | The f\_T table by whole degree. Environment classes are the lead's clock keys (Layer 5); the snow cap is a rule of the `snow:<region>:<band>` key; the heated room reads `room:<id>` |
| Food numbers | `data/yearofice/data_maps/item/food.json` | `kcal_per_100g`, macros in g per 100 g, `vit_c_mg_per_100g` (10 mg = 1 C-day), `spoil_days_at_5c` by state and `curve`, per Items › Food values (RECONCILE 82) |
| Item to class | `data/yearofice/data_maps/item/decay.json` | Class per non-food item (iron, cordage, fur, rawhide) |
| Block to class | `data/yearofice/data_maps/block/decay.json` | Class, species, size, load-bearing flag per block |
| Set-piece ages | `data/yearofice/setpiece/<id>.json`, new field `abandoned_year`; `present_year` in story data | Ages for AgeWorld |

Structures' `aging/materials.json` folds into these files: rot multipliers into the decay classes and per-block numbers into `data_maps/block/decay.json`, beside `piece.json`.

```json
{
  "id": "yearofice:spruce_log_peeled",
  "driver": "living_rot",
  "shape": "proportional",
  "r_per_year": 0.02,
  "factors": {
    "species": 1.0,
    "size": { "log": 1, "pole": 2, "split": 12, "stump": 0.5 },
    "ground": { "off": 1, "moss": 1, "soil": 1.5, "soil_forest_shade": 0.9, "soil_heated_3_blocks": 3 },
    "moisture": { "roofed_dry": 0.5, "open": 1, "wet_moss": 2, "snow_bank_after_apr15": 2, "bog": 4, "submerged": 0.2, "permafrost": 0 },
    "protection": { "charred_end": 0.5 }
  },
  "ladder": { "names": ["sound", "weathered", "soft", "rotten", "collapsed"], "p": [0.25, 0.5, 0.75, 0.97] },
  "load_threshold_u": [0.35, 0.55],
  "becomes": { "block": "yearofice:punk_log", "loot": "yearofice:blocks/rotten_spruce_log" },
  "tells": { "look": "decay_state", "sound": "yearofice:decay.punky_thud", "smell": false }
}
```

### Code

**The core is pure.** Formulas live in `com.yearofice.sim.decay` with no Minecraft imports: `DecayCore` (the formula family and settling), `ColdCurve`, `DecayClimate`, `DecayLadder`, `Thresholds` (hash(seed, "decay", pos, day) draws) and `AgeWorld` (multi-year closed form).

**Adapters.** These live in `com.yearofice.decay`. They are `ItemDecay` (components, merge bands), `BlockDecay` (states, the day pass, marks), `AreaDecay`, `DecaySettler` (chunk-load queue), `AgeWorldRunner` (time away, the old camp, set-piece aging), `CompostHeapBlock`, `AshLayerBlock`, `ToolRackBlock`, `NetRackBlock`, `SourFishPit`. Botulism is a `problems` entry in `yearofice:body` read from `injury/botulism.json`, like gut illness, so it needs no new field.

**Existing classes become clients.** `food.Spoilage`, Structures' `RotModel` and Bestiary's `CarcassLifecycle` call `DecayCore` instead of their own math. `UpkeepLedger` reads each structure's predicted threshold dates from it, which is how the upkeep page knows a roof's due year. Debug (RECONCILE 118): `/yoi decay info` prints the looked-at block's or held stack's curve, clock, due and p; `/yoi decay age <days>` ages the looked-at block, container or held stack by the closed form; `/yoi decay climate` prints this year's decay-days per clock key. Tuning lives in the `[decay]` table of `yearofice-common.toml`: the spoilage-speed slider (×0.5, ×1.0, ×1.5 by preset), the frozen threshold (−5 °C) and the band count (40).

**Client.** `client.decay.DecayDecorators` (bars and stage marks), `SmellLineParticle`, `FlyCloudParticle`, and the stage texture variants under `assets/yearofice/textures/item/decay/` and `block/decay/`.

**New ids.** Blocks `yearofice:compost_heap`, `tool_rack`, `net_rack`, `ash_layer` (state `warm`, `cold`, `leached`), `sour_fish_pit`. Items `yearofice:ash`, `lye`, `compost`, `maggots`, `sour_fish`, `labrador_tea_bundle`. Component `yearofice:decay`. Attachment `yearofice:area_decay`; placed blocks use the lead's `yearofice:decay_marks` and carried items the body clocks, so the draft's `thermal_log` and `decay_stamps` are withdrawn. Item data map `yearofice:decay` (`data_maps/item/decay.json`) beside the block one. Problem `injury/botulism.json`. Particles `yearofice:smell_line`, `fly_cloud`. Sounds `yearofice:decay.punky_thud`, `decay.flies`, `decay.tin_crunch`. Prompts `thaw_coming`, `decay_due`. All of these go into `studio/inventory.json` (RECONCILE 103).

### Performance caps

| Item | Cap |
| --- | --- |
| Stamped blocks per chunk | 512; beyond that, blocks of one kind share a group stamp |
| Chunk-load settling | 2 ms per chunk, spread over ticks (engine budget) |
| Day-pass settles | Spread over ticks, inside the Decay layer's 0.2 ms a tick (Minecraft development › The server tick and its budget) |
| Smell lines and fly clouds | 16 emitters and 8 clouds within 32 blocks, nearest first |
| AgeWorld | 30 years over the home valley's edited chunks in under 10 s, behind a progress screen |
| Save data for decay | Under 600 KB after 5 simulated years, inside the 5 MB budget |

### Milestones

| Milestone | Ships |
| --- | --- |
| M1 | Set pieces as hand-aged templates matching the past-decay table |
| M4 | The body and pocket clocks; weakness and the existing illnesses wired to food |
| M5 | `DecayCore`, the cold curve, food on the shared model, the eating ladder, the sniff, tells, sour fish and botulism, maggots, the compost heap |
| M6 | Hides and pelts, salt, ash-lye dehairing, carcass integration, scent by stage |
| M7 | Snow cover as a cold store, the thaw countdown, `thaw_coming`, snow structures, bones lasting 2 years |
| M8 | Wood, metal, stone, clay and fabric for placed blocks; roofs and thresholds; ash; tool and net racks; `decay_due`; the tedium check |
| M9 | AgeWorld: set-piece aging replaces the hand-aged templates; the old camp; the longer absence (1, 2 or 5 years); area decay and rich ground |

### GameTests

Each is a NeoForge GameTest named `yearofice:decay/<test>`, waiting on game time through the clock.

1. **`cold_curve`** (batch `m5`). Meat in a closed chest reaches Turning at 48 ±1 game hours at 15 °C, 144 ±2 at 5 °C and 720 ±12 at −3 °C. At −10 °C it loses nothing in 120 days. A fresh quarter frozen on February 18 shows a thaw countdown of 56–60 days.
2. **`catchup_equals_live`** (batch `m8`). A fixture chunk with 40 decaying things in 8 classes is aged 30 game days live and, separately, unloaded for 30 days. Every thing ends in the same stage, p agrees within 0.001, and the same roof blocks have fallen.
3. **`wood_rates`** (batch `m8`). Spruce deadfall in forest shade turns Punky at 4.0 ±0.2 valley years and becomes a nurse log at 13.4 ±0.5. A peeled sill on soil beside a heated room is Rotten at 23 ±1 years; on a corner stone it is Soft at 35 ±1.
4. **`snow_buffer`** (batch `m7`). A −15 °C week holds a 2-day spell at +3 °C. Meat on an open pole loses 6–10% of its safe life; meat under 40 cm of snow loses none.
5. **`eating_ladder`** (batch `m5`). Over 10,000 raw Turning meals at Condition 80, gut illness hits 50% ±1.5 and each adds nausea 40 at once. At Condition 40 the rate is 62.5% ±1.5. Stewed Turning meat gives gut illness 15% ±1.
6. **`botulism_container`** (batch `m5`). Over 1,000 sour-fish batches, none in a grass pit is toxic and 10% ±2 in a lidded tin are; boiling a toxic batch 10 game minutes gives no cases.
7. **`old_camp_10y`** (batch `m9`). The first-cabin template aged 10 years over 50 seeds has 40–60% of its sod roof blocks down. The same seed gives an identical block hash. Built peeled on corner stones, under 5% of its logs are Rotten; built unpeeled on soil sills, 25–35% are.
8. **`upkeep_budget`** (simulator, `m8`). The year-3 homestead spends 8–15 game hours a year on decay chores outside structures. No loop recurs more than once a game week per item class.
