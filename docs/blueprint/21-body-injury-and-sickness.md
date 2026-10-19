# Body, injury and sickness

This tab gives the body ten parts, each with its own health from 0 to 100, so an injury lands somewhere and changes what that limb can do. It gathers every illness in the design into one register and settles how the body's states run in Minecraft. Condition, its bands, Collapse and SOS, and every problem's written cause, timer and treatment stay exactly as Blueprint › Health, medicine and nutrition sets them.

## How this tab fits

**It extends three things and replaces none.** Blueprint › Injuries said an injury "picks a body part"; this tab defines the parts, the pick and what the part then does. Blueprint › Clothing's body figure gains a health layer. Minecraft development › Player systems and client › The body attachment gains per-part fields, and `BodyService.applyInjury` stays the only path for injuries.

### What stays as written

- **Condition** is still 0–100, heals by the table in Health, medicine and nutrition, sets the Fine, Hurt and Badly hurt bands, and collapses at 0 under the SOS rules.
- **Every problem** keeps its cause, stages, Condition losses, timers, one treatment and recovery time. This tab only says which parts each can land on and what it adds there.
- **Every source** in Danger in detail › Damage sources keeps its Condition loss and its injury roll. This tab adds the part weights.
- **Strength** is still Condition × clamp((BMI − 17)/6, 0, 1). Parts change how a bow or axe is used, not Strength itself. Strength also scales the load bands from 0.8× to 1.15× and sets the frames' minimum (video 2, A11: Minecraft development › Player systems and client › Strength carries more), so losing BMI shrinks what the player can carry.
- **Body states are fields of `yearofice:body`**, not mob effects (RECONCILE 19, settled in Player systems and client).

### Where each video concept lives

Every Forge Labs concept that touches the body has one home in the per-part model.

| # | Concept (Every concept from the video) | Home in this tab |
| --- | --- | --- |
| 5 | The medkit heals wounds | Treatments by part: the bandage, 3 s on a limb |
| 87 | Stepping in the fire gives a burn | Injury sources: fire hits the feet 60%, legs 40%; a foot burn slows walking |
| 88 | Sitting in water to cool a burn | Treatments by part: a leg or foot burn cools by sitting in knee-deep water |
| 94 | An infected wound causes blackouts until bandaged | Sickness register: infection stage 2; proposal P3 |
| 127 | Foot blisters | Problems by part: blisters on the feet (written) and the hands (proposed) |
| 137 | Getting hurt just walking around | Falls, stumbles and slips: the stumble outcomes land on feet and legs |
| 139 | Several wounds at once; plant-fibre bandages | Several wounds at once; the plant-fibre wrap as a binding |
| 203 | Foot injuries; wanting socks | Feet: wet socks raise blister and frost risk; a dry spare pair is the fix |
| 224 | Scurvy | Sickness register; reopened cuts return on the part they healed on |
| 245 | Food poisoning and a gut infection | Sickness register: gut illness, which is "food poisoning" in the video's words |
| 311 | Snowshoes to stop frostbite on the feet | Feet: deep snow over the boot tops wets the feet unless on snowshoes |
| 316 | Scurvy raises the calorie burn | Sickness register: Weakness +10%, Slowness +20% (Scurvy's cost) |
| 317 | Four days of rest by the fire to heal frostbite | Rest days double part healing; superficial frostbite's 7 days become 3.5 |
| — | Stumbles, trips and falls | Falls, stumbles and slips |
| — | Rest days | Part health and Condition: Rest days double the healing pool |
| — | Scurvy's cost | Sickness register |
| — | The adrenaline crash | How parts combine: the burst lifts Sore and Hurt leg limits, never a break |

## The ten body parts

### Parts, zones and shares

**Ten parts, five zones, one set of shares.** Each part belongs to one Clothing zone, and its share of the body is its zone's share split by size. The zone sums are Clothing's own: torso 40%, legs 25%, feet 15%, head 10%, hands 10%.

| Part id | Clothing zone | Share s | Sub-places named in lines | Floor | Main jobs |
| --- | --- | --- | --- | --- | --- |
| `head` | Head | 0.10 | Scalp, face, ear, eye | 10 | Sight, balance, hearing |
| `torso` | Torso | 0.26 | Chest, ribs, back, belly | 10 | Breath, carrying, stamina |
| `left_arm`, `right_arm` | Torso | 0.07 each | Upper arm, elbow, forearm | 0 | Power: swings, draw, pulls, strokes |
| `left_hand`, `right_hand` | Hands | 0.05 each | Wrist, palm, fingers, thumb | 0 | Fine work, grip, knife |
| `left_leg`, `right_leg` | Legs | 0.125 each | Thigh, knee, shin | 0 | Gait, climbing, jumping |
| `left_foot`, `right_foot` | Feet | 0.075 each | Ankle, heel, toes | 0 | Gait, footing |

**Main and off side.** The main side follows Minecraft's Main Hand option (right by default). "Off hand" in the existing rules means the other side: a knife slip lands on the off hand, and the hatchet's front leg is the off-side leg.

**Sub-places are words, not parts.** A hit also rolls a sub-place for the line ("Cut, left forearm"), as the Vitals screen already writes it. It changes nothing in the rules except where a problem says so: a sprain on a foot is the ankle, on a hand the wrist.

### Part health and Condition

**Each part keeps a health h from 0 to 100.** It starts at 100 and only injury events and a few local problems lower it. Everything else that hurts the body, such as bleeding, illness, starvation and poison, goes into one systemic pool, D.

```latex
Condition = 100 - D_{sys} - \sum_{p} s_p \,(100 - h_p)
```

**An event's loss is split, never added to.** An event that costs L Condition hits part p for up to 3L of part health. The part's weighted share of that counts toward Condition; the rest goes to the systemic pool. So the Condition loss is exactly L, as Danger in detail lists it.

```latex
\Delta h_p = \min(3L,\ h_p - floor_p), \qquad \Delta D_{sys} = L - s_p\,\Delta h_p
```

The gain of 3 and the largest share (torso, 0.26) keep the systemic part positive, because 3 × 0.26 = 0.78. A hit that would push a part below its floor sends the overflow to the systemic pool.

| Event | L | Part hit | Part health after | Condition after |
| --- | --- | --- | --- | --- |
| Bear bite, defending | 8 | Left arm | 76 (Sore) | 92 |
| Bear swipe, predatory | 15 | Torso | 55 (Hurt) | 85 |
| Moose stomp | 20 | Torso | 40 (Hurt) | 80 |
| Hatchet glance | 10 | Left leg | 70 (Sore) | 90 |

**Which ongoing losses are local.** Two problems hurt their part as they run: infection stage 1 (−10 a day) and a break left unsplinted (−5 a day), each split by the same rule. Bleeding, fever, blood poisoning, gut illness, poisoning, botulism, hypothermia and starvation are systemic.

**Healing fills every pool by the same fraction.** Each game hour the existing healing rate R (Condition back per game hour, with its underweight, scurvy and Rest day factors) is shared over all damage in proportion. A part is whole again exactly when Condition is.

```latex
f = \frac{R}{100 - Condition}, \qquad D_{sys} \leftarrow D_{sys}(1-f), \qquad (100 - h_p) \leftarrow (100 - h_p)(1-f)
```

**When healing runs.** Health, medicine and nutrition says Condition returns "while no problem is active", and also that bleeding, infection and warmth under 30 stop healing completely. This tab reads it as one rule: healing runs unless a wound is bleeding, an infection is active, or warmth is under 30 (conflict C1). A splinted break or a dressed cut does not stop it.

**Floors keep collapse where it is.** The head and torso cannot fall below 10, so no single part ever causes a collapse. Condition 0 is still the only collapse from injury, with the SOS rules unchanged.

**The Endless return.** A player flown out comes back with every part at 100 and the systemic pool at 20, which is Condition 80 as written. Problems are cleared; lost fingers or toes and crooked bones stay.

### Bands and part effects

**Part health sets a band, and each band changes what the part can do.** The bands are Fine 80–100, Sore 60–79, Hurt 30–59, Badly hurt 1–29 and Useless 0. Head and torso stop at Badly hurt, because of their floor.

**Legs and feet.** Each side uses the worse of its leg and foot.

| Band | Walk speed | Sprint | Climbing and step-up | Jump | Stumble chance |
| --- | --- | --- | --- | --- | --- |
| Fine | ×1.0 | Yes | Yes | ×1.0 | ×1 |
| Sore | ×0.95 | Yes | Yes | ×1.0 | ×1.5 |
| Hurt | ×0.8 | Ends after 10 s | Climb time ×2 | ×0.7 | ×2 |
| Badly hurt | ×0.6 | No | No tree climbing | ×0.4 | ×3 |
| Useless | ×0.3, a hop | No | No | 0 | ×3 |

**Arms.** These set power work. A bow, axe, maul, paddle or bow drill is two-handed.

| Band | Swing time (axe, hatchet, splitting, paddle) | Bow draw force | Aim sway | Pull and haul time | Swim stroke |
| --- | --- | --- | --- | --- | --- |
| Fine | ×1 | ×1 | +0 | ×1 | ×1 |
| Sore | ×1.1 | ×0.95 | +5% | ×1.1 | ×1 |
| Hurt | ×1.3 | ×0.8 | +15% | ×1.4 | ×0.85 |
| Badly hurt | No two-handed tool | No bow | +30% | ×2 | ×0.7 |
| Useless | Holds nothing | No bow | — | Other arm only, ×2.5 | ×0.6 |

**Hands.** These set fine work: the bow drill, knapping, snares, cordage, sewing, net mending and tying.

| Band | Fine-work timing window | Hand-job time (knife, sewing, nets) | Aim sway | Grip |
| --- | --- | --- | --- | --- |
| Fine | ×1 | ×1 | +0 | — |
| Sore | ×0.9 | ×1.1 | +0 | — |
| Hurt | ×0.75 | ×1.3 | +10% | Drops the held item on a stumble fall |
| Badly hurt | No fine work with this hand | ×1.6 | +20% | Cannot hold a bow or an axe |
| Useless | — | Other hand only, ×2 | — | Holds nothing |

A window of ×0.75 is the same squeeze shivering gives. Both together give ×0.56, because different causes multiply.

**Torso.**

| Band | Sprint energy | Carry limits | Heavy-work energy | Useful time in water | Breath |
| --- | --- | --- | --- | --- | --- |
| Fine | ×1 | ×1 | ×1 | 10 game minutes | — |
| Sore | ×1.25 | ×1 | ×1.1 | 10 | — |
| Hurt | ×1.5; ends after 10 s | ×0.8 | ×1.2 | 8 | Catches on a sprint |
| Badly hurt | No sprint | ×0.6 | ×1.5 | 6 | Ragged; a cough when carrying |

Carry limits scale every weight threshold in Inventory and backpack: a Hurt torso turns the 30 kg backpack into 24 kg and the amber load mark from 15 to 12 kg.

**Head.**

| Band | Sight range | Dizziness | Hearing range | Blackout |
| --- | --- | --- | --- | --- |
| Fine | ×1 | — | ×1 | — |
| Sore | ×1 | — | ×1 | — |
| Hurt | ×0.9 | ±1° view sway on fast turns | ×0.9 | — |
| Badly hurt | ×0.75, blurred edges | ±2.5° sway | ×0.75 | 1% a game hour awake, 10 game minutes down; any new head hit knocks out for 30 game minutes |

Sight range sets the fog distance and the 10 m plant-naming range; hearing range scales the radius of sound cues.

### How parts combine

**On one part, the worst effect wins.** For each function a part gives, its factor is the lower of its band and every problem on it. A sprained ankle (half speed, as written) on a Fine foot gives ×0.5, not less.

**Across parts, the chain rules apply.**

1. **Walking.** Walk = min(left side, right side) × 0.9 when both sides are below 1. The limp shows when the sides differ by 0.1 or more.
2. **Two-handed work.** The job uses the lower factor of the two arms for swings and the two hands for fine work.
3. **The bow.** Draw force comes from the main arm. Sway adds the off arm's and off hand's sway. Any arm or hand at Badly hurt blocks the bow.
4. **One-handed work.** A knife, rod, spoon or torch uses the main hand. With the main hand Useless, the off hand does it at ×2.
5. **Global factors multiply on top.** The Condition bands, energy under 3, sleep debt, hypothermia's Slowed, scurvy's Slowness, water depth, snow and load all apply as now.
6. **Limits.** Walk speed never falls below ×0.25. A job never takes more than 3 times its time from all causes together. Aim sway adds up to at most +80%.

**The bow check.** The existing rule needs Strength 70 for the longbow. With parts, the draw succeeds when Strength × draw force is 70 or more. A healthy player at Strength 100 with a Hurt main arm draws at 80. A thin player at Strength 85 with the same arm draws at 68 and gets the existing stop at 60% with shaking.

**The adrenaline burst.** For its 10 s on land (15 in water), the burst ignores Sore and Hurt leg and torso limits, sprains and blisters. It never lifts a break or a Useless part. Running the burst on a sprain turns a mild sprain into a bad one 25% of the time (new). The crash in The adrenaline crash follows unchanged.

**Rest days.** On a Rest day the healing rate R doubles after the underweight halving, by the Studio detail in Rest days. Part health heals with it, so a rest by the fire mends the limbs as fast as Condition.

## Injuries on the body

### Problems by part

**Each problem keeps its written effect and gains a place.** The "written" column is Health, medicine and nutrition, the Stumbles entry or the Land animals tab, unchanged. The "added" column is what the part model adds. "One band lower" means the part acts as the next band down for its own functions while the problem lasts.

| Problem | Can occur on | Written effect | Added on that part |
| --- | --- | --- | --- |
| Cut, minor | All 10 parts | Bleeds 1 a game minute until dressed; heals in 3 days | A dressed hand cut: fine-work window ×0.9 until healed |
| Cut, deep | All 10 parts | Bleeds 3 a game minute | While open: the part acts one band lower. A head cut bleeds into the eye: sight ×0.9 until dressed |
| Infection | The part of the cut, bad burn or blister it grew from | Stage 1 −10 a day; stage 2 fever and blackouts; stage 3 blood poisoning −20 a day | Stage 1 loss is local and the part acts one band lower. Stage 3 draws red streaks up the limb |
| Burn, minor | All parts | Work 20% slower, 2 days | Hand or arm: that side's jobs ×1.2. Foot or leg: walking ×0.8. Torso or head: the global ×1.2 |
| Burn, bad | All parts | Also an open wound, 4 days | As minor, and infection rules as a cut on that part |
| Sprain, mild and bad | Hand (wrist), arm (elbow), leg (knee), foot (ankle) | Half speed; 2 or 5 days; the timer runs only once wrapped | On a leg or foot: walking ×0.5, as written. On a hand or arm: that side's jobs take twice as long, and no bow or axe |
| Broken bone | Arm (forearm), hand, leg (shin), foot, torso (ribs) | Arm: no two-handed tool. Leg: no running, walking 60%, no climbing. Splinted 30 days; unsplinted −5 a day; crooked after 3 days | Hand counts as an arm break; foot as a leg break. Ribs are new: see below |
| Frostbite | Head (face, ears), hands, feet; legs and torso only when their zone is below its limit | Frostnip, superficial (clumsy hands or slow feet, 7 days), deep (14 days, a finger or toe lost) | Superficial: hands window ×0.75 and hand jobs ×1.3; feet walking ×0.85; face −1 Spirits a day. Deep: hands no fine work, jobs ×1.6; feet walking ×0.7 |
| Blisters | Feet (written); hands (proposed P4) | Feet: 85% walking; a bandage heals in a day; left 2 days, a minor cut | Hands: hand jobs and swings ×1.1 |
| Bruise | All parts | The stumble's "foot injury": −10% speed for 12 game hours; a bandage or a night's rest | Legs or feet: walking ×0.9 for 12 game hours. Elsewhere: one band lower for 12 game hours. Arnica salve heals it 25% faster |
| Swelling (stings) | Hands, head, arms | Hold-to-work 10% slower for 4 game hours (Land animals) | On the head an eye swells: sight ×0.9 for 4 game hours |
| Quills | Hand that grabbed, or a leg | −3 Condition; 20 game minutes to pull | Until pulled, the part is Hurt for its functions |
| Leeches | Legs, feet | −0.5 Condition a game hour each (Bestiary) | None beyond the loss, which is systemic |
| Head knock | Head | Knocked out 30 game minutes (widowmaker) | New name for what a head hit leaves: Hurt-band head effects for 24 game hours; a second knock in that time knocks out for 30 game minutes |
| Snow blindness | Head (eyes) | Sight 30 m and no aim beyond 10 m for 24–48 game hours | Shown on the head; nothing added |

**Broken ribs.** A torso break is the one new variant. It allows walking but no sprint, sets carry limits ×0.6 and heavy-work energy ×1.5, and makes a cough on every lift over 15 kg. The treatment is a rib wrap, not a splint; unwrapped it costs −5 a day like any unsplinted break and sets badly after 3 days, which leaves carry limits ×0.95 for good. It heals in 30 days, the same as any splinted break.

**What garments add.** The existing rule that a bear or wolf strike tears the torso layer now tears the outer garment of the hit part's zone, −15% condition. A thick outer layer there (hide coat, parka, hide pants, mukluks, fur mittens or fur hat) turns a rolled minor cut from a bite or claw into a bruise 25% of the time (new).

### Injury sources

**Every source rolls its part first, then its problem.** The Condition loss and the problem roll are Danger in detail's, unchanged; the part weights are new. Part damage follows the split rule (3 × L on the hit part). "Off" and "main" are sides.

| Source | Condition | Hit-part weights, % | Problem roll (existing) |
| --- | --- | --- | --- |
| Knife slip, cleaning with warmth under 30 | −3 | Off hand 85 (fingers 60, palm 25), main hand 5, off arm 10 | Minor cut, 5% a job |
| Hatchet glance on a knot, energy under 3 | −10 | Off leg 50, main leg 20, off foot 15, main foot 10, off hand 5 | Deep cut, 2% a swing |
| Standing in fire | −5 a second | Feet 60, legs 40, by the foot that stepped in | Bad burn after 2 s; minor before |
| Boiling water spilled | −10 | Hands 50, legs 25, feet 25 | Bad burn |
| Hot pot, stone or glowing tool held | 0 | Hand that held it 100 | Minor burn |
| Cauterizing | −8 | The cut's part | Bad burn (Ideas from viewers) |
| Lightning | −30 | Torso 40, head 20, arms 20, legs 20 | Burn (Thunderstorms) |
| Bear swipe, predatory | −15 | Head 20, torso 35, arms 30, legs 15 | Deep cut 50% |
| Bear bite, defending | −8 | By posture, below | Deep cut 30%, broken bone 10% |
| Wolf lunge | −10 | Legs 45, arms 35 (forearm raised), hands 10, torso 10 | Cut 50% |
| Beaver bite | −6 | Legs 70, hands 20, arms 10 | Cut 70% |
| Porcupine grabbed bare-handed | −3 | The grabbing hand 90, its arm 10 | Quills |
| Moose stomp | −20 a stomp | By posture, below | Broken bone 40% |
| Falling tree, direct | −40 | Head 15, torso 35, legs 30, arms 20 | Broken bone 60% |
| Glancing branch | −15 | Arms 35, torso 25, head 20, legs 20 | Sprain 50% |
| Widowmaker | −25 | Head 70, torso 20, arms 10 | Knocked out 30%, which leaves a head knock |
| Slip running on bare ice | −3 | Torso 25, hands 25 (wrist), legs 20, arms 15, head 15 | Sprain 10% |
| Stings, yellowjackets or bees | −1 each | Hands 50, head 30, arms 20 | Swelling |
| Leech | −0.5 a game hour | Legs 70, feet 30 | Leeches |

**Small sources.** Kickback from an un-notched lean (−20): torso 50, arms 30, head 20, a bruise. Horse fly (−0.5): bare arms 40, legs 30, head 30. Water-tiger larva (−1) and nettle (−1): the foot or hand that touched. Spray blown back (−5): head, blinded 30 s. Seizure fall (−10): head 30, arms 30, torso 20, legs 20, a bruise.

**Pairs split evenly.** A weight given for "arms", "legs", "hands" or "feet" splits 50/50 between sides unless the row names a side.

**Posture decides where an animal lands.** Danger in detail teaches play dead for a defending grizzly and curl up for a moose. The weights make that lesson visible on the body figure: playing dead takes the bites on the back and pack, not the face.

| Posture | Bear bite, defending | Moose stomp |
| --- | --- | --- |
| Standing or fighting | Arms 40, legs 25, torso 25, head 10 | Torso 40, legs 25, arms 20, head 15 |
| Face down, hands behind the neck, pack on (Shift held) | First bite goes to the pack, as written. Then torso (back) 45, legs 30, arms 20, head 5 | Torso (back) 50, legs 30, arms 20, head 0 |
| Curled (Shift held, moose) | As face down | Torso (back) 50, arms 30, legs 20, head 0 |

**Drowning has no hit part.** It stays a collapse by the water clock. The parts change the clock: a Hurt torso cuts useful time from 10 game minutes to 8, a Badly hurt one to 6, and Hurt or worse arms slow the stroke.

**Thin ice has no hit part either.** The fall-through runs as written. The pull uses both arms' pull factor, and the hands, wet and on the ice edge, run their frost counters at the wet limit of −10 °C.

**Frostbite has its own source: the part's frost counter.** Each part runs the existing frostnip timer against its own safe-to limit.

```latex
t_{nip,p} = \frac{300}{T_{limit,p} - T_{feel,zone(p)}} \ \text{game minutes}
```

Both hands normally share the hands zone's limit. They differ when one hand is wet (a hand in the water pulling a net or at an ice hole uses the wet limit, −10 °C) or holds bare metal below −20 °C (the limit counts 5 °C warmer, new). The feet differ when one boot is wet.

### Falls, stumbles and slips

**Falls by height.** The effective height rule (h\_eff = h\_drop − 3 × snow depth) and the problems are Danger in detail's. The Condition losses for 3–12 m are new, because the existing table gives none and a part needs a loss to move (conflict C10).

| Effective height | Condition | Problem (existing) | Parts hit |
| --- | --- | --- | --- |
| 2–3 m | 0 | Bruise 30% (new) | 1 part: feet 60, legs 25, hands 15 |
| Over 3 m | −4 (new) | Sprain | 1 part: feet 45, legs 25, hands 20 (catching yourself), arms 10 |
| Over 6 m | −10 (new) | Broken bone | 2 parts, rolled apart: feet 35, legs 35, hands 10, arms 10, torso 10 |
| Over 12 m | −60 | Broken bone | 3 parts: legs 30, feet 25, torso 20, arms 15, head 10; the break lands on the first |
| Over 20 m | Collapse | — | — |

With 2 or 3 parts, L is split evenly across them before the 3L part rule, so a 12 m fall costs exactly 60.

**Stumbles.** The roll and its outcomes are the Stumbles, trips and falls entry and its Studio detail. The parts:

| Outcome | Problem | Parts |
| --- | --- | --- |
| Stumble (70%) | None | — |
| Foot injury (20%) | Bruise: a bruised toe or a turned ankle, −10% speed for 12 game hours | Foot 75, leg (knee) 25 |
| Fall (8%) | 5–20 Condition; the held item drops | Hands 30, legs 25, torso 15, arms 15, feet 10, head 5 |
| Sprain (2%) | Sprain, mild | Foot (ankle) 60, leg (knee) 20, hand (wrist) 20 |

**Injured parts raise the roll.** The stumble chance p takes the worse leg-or-foot band's stumble factor (×1.5 to ×3) on top of its five existing multipliers. A sprained ankle on a scree slope is how a bad week gets worse.

**Feet, socks and snowshoes.** Wet feet raise blisters by the written rule (2 km in a day at 50%). Two new rules finish concepts 203 and 311.

- **Snow in the boots.** Walking in snow over 40 cm deep without snowshoes, in boots or moccasins, wets the feet zone's inner layer 10% per 100 m, the one rate in Open world in depth › Traversal. Mukluks and snowshoes stop it. A day of breaking trail without snowshoes soaks the socks, which drops the feet to the wet limit and starts frostbite.
- **The spare pair.** Dry socks swapped by a fire take 20 s and clear the feet zone's wetness, by Clothing's dressing rule. A second pair of wool socks or hare-fur liners in the pack is the fix the creator asked for.

### Several wounds at once

**Problems never merge into one number.** Each wound keeps its part, its stage, its own bleed and its own timer, up to the record's 16. A bear contact with three bites can leave a deep cut on the back, a cut on the left forearm and a broken right hand at once.

1. **Bleeding adds up.** Two deep cuts and a minor one bleed 7 Condition a game minute, on the close clock.
2. **Triage order.** Lists and the treat action order problems by bleed rate, then time until worse (the 12-hour infection mark, a rewarm deadline, the crooked mark), then effect size.
3. **Treat all.** One choice in the self-care radial runs every treatment the pack allows in that order, each with its own hold. It stops with a line when an item runs out: "Out of bandages. Right shin still bleeding."
4. **Shock.** With total bleeding of 4 a game minute or more, or 3 or more open wounds, the view greys 20% and the heartbeat plays. Shock changes no rule; it is the body telling the player to stop and dress wounds.
5. **The 17th problem.** A new minor problem on a part that already has the same one is folded into it: the timer resets to the later of the two.

**Plant-fibre bandages.** A dressing is a pad held by a binding. Plant fibre makes the binding when cloth and bandages are gone: a fibre wrap is 1 m of nettle or fireweed fibre (Plants and fungi › Plant fibres), twisted in 2 game minutes, 10 g. It holds a moss, sphagnum, usnea or puffball pad as well as a cloth strip does. On a hand or foot, a fibre wrap loosens after 8 game hours of work, and the dressing then counts as dirty.

### Limping and the walking stick

**The limp is a fact other systems read.** It shows when the two walking sides differ by 0.1 or more, or any leg or foot problem has a gait effect. Severity is 1 at a side factor of 0.8 or more, 2 from 0.5 to 0.8, and 3 below 0.5.

- **Seen and heard.** The head bob dips on the bad side; one footstep sounds soft and one hard; the bad side's prints drag, drawn by the track decals in Tracks and sign.
- **Other players.** `yearofice:body_public` already carries limp; it gains the side and severity.
- **Wolves.** A limp of severity 3 counts as "weak" for the winter wolf test, like Condition under 40 (proposal P5).

**The walking stick takes weight.** Its existing effects hold: a sprain's or blister's half speed becomes 75%, and a splinted leg's 60% becomes 70%. The same halving applies to bruises and to band losses on the worse side.

### Treatments by part

**One treatment per problem, done where the problem is.** The treatments are the written ones; this table sets what each part needs. Hold times are in the Self-care section below.

| Treatment | Parts | Made from | Effect |
| --- | --- | --- | --- |
| Dressing: pad and binding | Any | Pads: medkit bandage (pad and binding in one), boiled cloth, dry moss, sphagnum, usnea, puffball powder, yarrow added. Bindings: bandage, cloth strip, plant-fibre wrap | As the dressing table. A back wound is dressed blind: clean for half the time. Feet need boots and socks off first |
| Splint | Arm, leg (written); hand; foot | Arm or leg: 2 sticks, 2 m cordage, moss pad (written). Hand: 1 short stick, 0.5 m cordage. Foot: a bark sheet, 2 m cordage, moss pad | Starts the 30-day timer |
| Sling | Arm or hand, splinted | 1 cloth scrap or hide scrap, 2 game minutes | The arm holds nothing. No re-break on falls; lifts the pain factor on sleep for that problem |
| Rib wrap | Torso, broken ribs | 2 cloth scraps or 1.5 m hide strip, 5 game minutes | Starts the 30-day timer; stops the −5 a day |
| Sprain wrap | Hand, arm, leg, foot | Bandage, cloth strip or rawhide, 1 game minute (written) | Starts the sprain timer |
| Rewarm | Hands, feet, face | Hands: armpits or the wolf (written). Feet: boots off, against the wolf, a wrapped warm stone or a warm room (new). Face: hood up, hands over it (new) | Stops frostbite advancing; frostnip clears |
| Cool a burn | Any | Within 10 game minutes. Hands and arms: hold at water or pour 0.5 L. Feet and legs: sit in knee-deep water (Z, Sit), concept 88. Torso: sit in water to the chest | Halves recovery (written). Sitting soaks the zones it covers, so warmth pays |
| Salve | The part treated | Spruce pitch, poplar bud, fir balsam, arnica, tamarack | One use per part per application |
| Pull quills, remove leeches | Hand, leg; legs, feet | Fingers; fingernail or an ember (written) | Clears them |

**Splinted arms jar loose.** Without a sling, a stumble fall or a fall over 2 m re-breaks a splinted arm or hand 25% of the time, and its 30-day timer starts again. The line says so the first time: "Fell on the splinted arm. Use a sling."

**Painkillers lift speed, not blocks.** As written, they remove the speed penalties of sprains, burns and breaks for 6 game hours. A broken leg still cannot run, and a broken arm still cannot draw a bow.

### Permanent marks

**Lost fingers and toes.** Deep frostbite takes one digit from the part it froze, as written: the little finger first, then ring, middle and index, and the thumb last; the little toe first and the big toe last. The effects stay global: crafting 5% slower per finger and walking 3% slower per toe, counting at most two of each.

**Crooked bones.** A break left unsplinted 3 days sets crooked: 60 days to heal and 5% slower use of that limb for good, as written. On the part model that is a permanent ×0.95 on the part's functions: walking for a leg or foot, jobs and swings for an arm or hand, carry limits for ribs.

**What the player sees.** The Health panel draws lost digits and a crooked mark on the figure. The first-person hand shows a stub where a finger was. Both survive the SOS flight, as written.

## Sickness register

**Every illness in the design, in one place, with its numbers as written.** The register adds no new illness and changes no number. Where two tabs disagree, the row says so and the conflict list below proposes a fix. Each row is one file in `data/yearofice/illness/`.

### The register

| Illness (id) | Source tab › section | Cause | Incubation | Stages and symptoms | Cure | Duration |
| --- | --- | --- | --- | --- | --- | --- |
| Gut illness (`gut_illness`); the video's "food poisoning" | Blueprint › Health, medicine and nutrition; Decay and time › Eating decayed food; Ideas from viewers › Sick stomach | Unboiled water (15/30/40% by preset); the decay ladder; mouse droppings; false chanterelle, false morel | Water 1–3 game days; food 1–2 | Absorbed ×0.5, +1 L water a day, energy cap 7, woken twice a night, fever day 1, nausea +15/h for 12 h | Boiled water, rest, fluids; broth absorbs 100%; bed −12 h; extra litres −4 h each (to −12); geranium −12 h; buckbean −6 h | 2 game days; +1 day if water under 30; +12 h if eaten weak; bad water again restarts it |
| Tapeworm (`tapeworm`) | Blueprint › Health; Decay and time › Chances by food and stage | Raw fish: 2% fresh, ×2 Aging; 0% after 7 days below −20 °C | 3–5 game days | Calories absorbed ×0.85 | Supply-drop medicine, one dose | Until treated |
| Trichinella (`trichinella`) | Blueprint › Health; Decay and time | Raw bear meat 25% (raw game: conflict C4); freezing never helps; Done at twice the time or stewing prevents | 3–5 game days | ×0.85 absorption, −10 Condition once, Condition capped at 90 | Supply-drop medicine; wormwood tea 1 cup a day for 7 days (Plants and fungi › Remedies and doses) | Until treated |
| Botulism (`botulism`) | Decay and time › Sour fish and botulism | Sour fish packed in a sealed tin, jar, jerrycan or plastic: 10% a batch | 12–36 game hours | Nausea +60, energy cap 3, −10 Condition a game day | None. Rest in a bed halves each day's loss. Boiling the batch 10 game minutes first destroys the toxin | 5 game days |
| Scurvy (`scurvy`) | Blueprint › Health; Every concept from the video › Scurvy's cost | Vitamin C reserve at 0 | Day 1 at an empty reserve (30 days from full) | 1, days 1–10: energy cap 8, wounds heal at half speed. 2 "Weakness", days 11–30: energy cap 6, healed cuts reopen 5% a day on the part they healed on (the record keeps healed cuts 30 days), burn +10%. 3 "Slowness", day 31 on: joint pain, walking 70%, burn +20% | Vitamin C food (rose hips +3, berries +1 to +2, spruce-tip tea +1, raw liver +1, raw or frozen fish +0.5). The burn ends the day the reserve is above 0 | Effects end 7 days after the fix. Stage 3 is a Hardcore severe condition |
| Wound infection (`infection`) | Blueprint › Health › Stages of each problem | A cut undressed 12 game hours; a dirty dressing, 15% every 6 game hours (20% Grimy; 10% with hands washed, Items); a blister left 2 days; a bad burn | 12 game hours | 1, hours 12–36: red and hot, −10 Condition a day (local). 2, days 2–4: fever, 2% blackout a game hour for 30 game minutes | Clean and redress (5 game minutes) stops stage 1. Salve on a cleaned stage 1 heals it in 3 days. Antibiotics cure any stage in 5 days. Willow-bark tea removes blackouts for 8 game hours | Stage 2 becomes stage 3 on day 5 |
| Blood poisoning (`blood_poisoning`) | Blueprint › Health (infection stage 3) | Infection reaching day 5 | — | −20 Condition a day; red streaks up the limb (this tab) | Antibiotics, 5 days | Until cured; 24 h at this stage is a Hardcore severe condition |
| Fever (`fever`) | Ideas from viewers › Sick stomach | Infection stage 2; gut illness, day 1 | With its cause | A 3-hour cycle: hot 2 h (the inner torso layer wets 10% an hour under an outer layer), chills 1 h (feels-like −6 °C). +1 L water a day; sleep recovery ×0.7 | Willow-bark tea removes it for 8 game hours; curing the cause ends it | With its cause |
| Nausea and vomiting (`nausea`) | Ideas from viewers › Sick stomach; Decay and time | The meter's sources: gut illness, poison plants, fermented berries, refeeding, tea and painkiller overuse, Turning or Spoiled raw meat, rancid fat, botulism | At once | 50: swirl icon and sway. 100: vomit, losing 50% of the last 3 hours' calories and 0.5 L water; back to 20; Spirits −2 | −10 an hour awake, −15 asleep; tea or broth −5, mint tea −30, buckbean tea −20 | Until the meter falls |
| Lean-meat sickness (`lean_meat`) | Blueprint › Macros and strength | Fat under 30% and protein over 45% of calories for 3 days | 3 days | Energy cap 6. From day 6, nausea: calories absorbed ×0.8 | 2 days with fat at 60% or more | Until cured |
| Hypothermia (`hypothermia`) | Blueprint › Warmth | Feels-like under 10 °C | By the warmth formula | Shivering 30–50 (+150 to +300 kcal/h, fine window ×0.75); Slowed 10–30 (70% walk, work 50% slower, sway); collapse risk 0–10; collapse at 0 | Fire, warm room, dry layers, hot drinks +5 | Until warm |
| Frostbite (`frostbite`) | Blueprint › Health; this tab, per part | A part below its safe-to limit | 300 ÷ (limit − feels-like) game minutes | Frostnip, superficial, deep, each one more period | Slow rewarming; never within 1 m of a fire; refreezing jumps a stage | Frostnip gone in 1 h if rewarmed; superficial 7 days; deep 14 days and a digit. Deep not rewarmed in 6 h is a Hardcore severe condition |
| Snow blindness (`snow_blindness`) | Blueprint › Health › Two problems the table did not list | Over 3 game hours on snow under clear sun above 15°, March to mid-May | That night | Sight 30 m; no aim beyond 10 m | Rest indoors halves it; slit goggles prevent it | 24–48 game hours |
| Cold cough (`cold_cough`) | Plants and fungi › Remedies and doses | 2 game hours of hard work in air below −30 °C | At once | A dry cough; −1 Spirits a day | Coltsfoot or Iceland moss tea cancels that day's loss | 3 game days |
| Dehydration (`dehydration`) | Blueprint › Water | Water under 60 | — | Dehydrated 15–30: cap 7, work +10%. Severe 0–15: cap 4. At 0: blackout 12 game hours, −10 Condition | Drink | Until drunk |
| Exhaustion (`exhaustion`) | Blueprint › Energy; Sleep | Energy under 1 bolt; sleep debt | — | Blackout 2% a game minute; awake 24/48/72 h: cap 7, work +30%, microsleeps | Sleep 4 hours or more | Until slept |
| Coal-air headache (`coal_air`) | Every concept from the video › Coal seams | Coal fire in a closed room, no chimney | 2 game hours | Headache; from 4 h, −5 Condition an hour, no sleep | Fresh air | While exposed |
| Plant poisoning (`poison_<plant>`) | Blueprint › Foraging and plant knowledge; Plants and fungi › Poisonings | One of 15 toxic plants or fungi | 0–24 game hours | By plant; destroying angel −60 over 3 days, with a false recovery on day 2 | Charcoal within 30 game minutes halves some; vomiting within 2 h halves the rest | By plant |
| Taste-test sickness (`taste_sick`) | Blueprint › Foraging; Plants and fungi › The tasting test | A toxic bite or staged step | 1 game hour | −2 to −25 Condition; energy cap 5 | None | 24 game hours |
| Remedy overuse (`overuse_<remedy>`) | Blueprint › Remedies; Plants and fungi › Remedies and doses | Over a daily limit | At once | Painkillers over 3: cap 7 a day. Labrador tea over 2: −5 and nausea +50. Wormwood: 5% seizure a day | Stop | 1 day |

**Heat illness is not in the design, and stays out.** Air above 20 °C already raises water drain ×1.2, so a hot July day bills through water. A heat rule would add a chore without a lesson.

**Ketosis is a status, not an illness.** Ketosis, Keto-adapted and Re-adapting (Every concept from the video › Ketosis) show as statuses and are listed under Status effects in Minecraft.

### Blackouts in one place

**Every blackout in the design.** The video's blackouts came from an infected wound, but seven other rules can drop the player. Each is a field of the body record, so the HUD can tell them apart.

| Cause | Chance | Down for | Wakes at |
| --- | --- | --- | --- |
| Infection stage 2 | 2% a game hour | 30 game minutes | As before |
| Energy under 1 bolt | 2% a game minute | 1 game hour | 1.5 bolts |
| Water at 0 | Certain | 12 game hours | Water 10, Condition −10 |
| Widowmaker | 30% of hits | 30 game minutes | With a head knock |
| Cauterizing under 15 Condition | 50% | 30 game minutes | Bleeding already stopped |
| A head hit while the head is Badly hurt, or a second knock within 24 h | Certain | 30 game minutes | With a head knock |
| Head Badly hurt, awake | 1% a game hour | 10 game minutes | As before |
| Water hemlock seizures | Every few minutes for 2 hours | Seconds | — |

**A blackout in water is drowning,** by Collapse and SOS. A blackout outdoors below 0 °C keeps the cold working, as the water blackout already says.

### Conflicts in the sickness rules

- **C2. Sphagnum dressing.** Blueprint's dressing table: dry moss stops bleeding in 5 game minutes and is clean for 12 h. Plants and fungi: a sphagnum dressing is clean for 24 h with bleeding halved. Proposed: dried sphagnum is its own pad, clean 24 h, stopping bleeding in 5 game minutes at half rate meanwhile; plain dry moss stays at 12 h.
- **C3. Wormwood's reach.** Plants and fungi says it "clears parasites from raw bear meat: absorption back to 100% on day 7". Proposed: day 7 clears trichinella fully, including the Condition cap of 90. Tapeworm stays a medicine-only cure.
- **C4. Raw game parasites.** Decay's ladder gives raw game a 5% parasite chance but names no parasite. Proposed: it is a tapeworm (×0.85 absorption), and trichinella stays bear-only.
- **C5. Gut illness can shrink to nothing.** Bed −12 h, drinking −12 h, geranium −12 h and buckbean −6 h add to 42 h of a 48 h illness. Proposed: a floor of 12 game hours.
- **C6. The nausea mob effect.** The Body department's Sick stomach Studio detail says the mob effect `yearofice:nausea` drives the icon. Player systems and client drops every mob effect. Settled: no mob effect; the icon reads the body record.

## Status effects in Minecraft

### Body state, not mob effects

**Decision: every body status is derived state in `yearofice:body`; no `yearofice:` mob effect is registered.** This follows RECONCILE 19 as settled in Player systems and client. `BodyTicker` already owns every timer, and a mob effect would add a second timer that milk clears. A sprain also needs a part, a stage and a wrap flag, which an amplifier cannot hold.

**Vanilla effects are off for players.** On NeoForge 1.21.1, `MobEffectEvent.Applicable` sets `Result.DO_NOT_APPLY` for every effect on a player in survival. Milk, potions, suspicious stew and beacons are banned items or blocks in `yearofice:banned_vanilla`. The `minecraft:effects` layer stays cancelled, and the inventory's effect list never shows.

**Movement uses attribute modifiers, not effects.** Gait results go out as modifiers on the player, refreshed in the Derive step when they change by 1% or more. The ids below are the 1.21.1 attribute names; 1.21.2 drops the `generic.` and `player.` prefixes.

| Modifier id | Attribute | Set from |
| --- | --- | --- |
| `yearofice:injury` | `minecraft:generic.movement_speed`, multiply total | The walking chain, the stick, limits |
| `yearofice:injury_jump` | `minecraft:generic.jump_strength`, multiply total | The worse leg-or-foot band |
| `yearofice:injury_break` | `minecraft:player.block_break_speed`, multiply total | Arm swing factors, for the few jobs that are block breaking |
| `yearofice:injury_step` | `minecraft:generic.step_height`, add | −0.4 at Badly hurt legs, so a 1-block step needs a jump |

### The status list

**Statuses are a 32-bit set worked out each Derive step.** They are not saved; the record's fields rebuild them on load. Each has a 9 × 9 glyph that reads without colour, under `textures/gui/status/`. The glyphs come from the 16 px texture generator in Art and audio production, so no found art is needed.

| Status id | On when | Glyph | Shown on |
| --- | --- | --- | --- |
| `bleeding` | Any wound bleeding | Two falling drops | Strip (new icon, before Warmth), figure, Vitals |
| `shock` | Bleeding 4 a game minute or more, or 3 open wounds | Drop in a broken ring | Figure; grey view |
| `infected` | Infection stage 1 or 2 | Ring with a dot | Figure on the part, Vitals |
| `blood_poisoning` | Infection stage 3 | Ring with three spokes | Figure, Vitals |
| `fever` | Fever cycle active | Thermometer with waves | Strip (new icon), Vitals |
| `nausea` | Nausea 50 or more | Swirl (existing) | Strip |
| `gut_illness` | Gut illness active | Knot | Vitals |
| `parasites` | Tapeworm or trichinella | Worm | Vitals |
| `botulism` | Botulism active | Bulging tin | Vitals; named "Something you ate" until the player knows the batch |
| `scurvy_1`, `scurvy_2`, `scurvy_3` | Scurvy stage | Tooth; one, two or three marks | Vitals ("Scurvy (early)", "Weakness", "Slowness") |
| `lean_meat` | Lean-meat sickness | Bare bone | Vitals |
| `ketosis`, `keto_adapted`, `re_adapting` | Ketosis states | Leaf, plain or ringed | Vitals; Keto-adapted in a calm shade |
| `limping` | Limp severity 1–3 | Boot with one to three bars | Figure beside the legs |
| `splinted` | Any splint, sling or rib wrap | Two parallel bars | Figure on the part |
| `head_knock` | Head knock active | Star | Figure on the head |
| `dizzy` | Head Hurt or worse | Spiral | Figure on the head |
| `snow_blind` | Snow blindness | Eye with rays | Vitals; the wash in `body_fx` |
| `frostbite` | Any part past frostnip | Existing vermilion hatch | Figure on the part |
| `cold_cough` | Cold cough | Two short arcs | Vitals |
| `poisoned` | Any plant poisoning | Leaf with a cross | Vitals; named only if identified |
| `painkiller` | Painkiller active | Pill | Vitals, with hours left |
| `rest_day` | A Rest day running | Hearth | Vitals (existing) |
| `leeches` | Leeches attached | Comma | Figure on the leg |

**The strip stays short.** Only two icons join the strip: Bleeding and Fever, because both need action within minutes. Every other status lives on the body figure or the Vitals page, so the strip keeps its rule of showing only what needs attention now.

### What is synced

**`yearofice:body_sync` carries what the owner's screens draw.** It already goes out on a 1% change or once a game hour; injuries add a send at once on any new problem or treatment.

| Added to `body_sync` | Size |
| --- | --- |
| Part health, 10 values | 10 bytes |
| Problems: part, type, stage, flags (dressed, splinted, sling, wrapped, cooled, dirty), minutes to the next mark | 6 bytes each, at most 16 |
| Status set | 4 bytes |
| Walking factor, limp side and severity, bow block, fine-work block | 3 bytes |
| Lost digits and crooked marks | 4 bytes |

**`yearofice:body_public` carries what others see.** Limp side and severity, a sling on which arm, a bandaged-parts mask of 10 bits, bleeding (for drips) and shiver. It goes out on change only.

**Nothing private leaks.** Illness and the problem list never go to other players; they see a limp, a sling, a bandage and drips.

## The body screen and HUD

### One figure, two layers

**The HUD body figure keeps its place and gains parts.** It is still the 24 × 48 GUI-pixel outline at the top left (layer `yearofice:body_figure`). It is now drawn as 10 parts in the proportions of a Minecraft skin: head 8 × 8, torso 8 × 12, each arm 4 × 12, hand 4 × 4, leg 4 × 10, foot 4 × 4.

- **Layer 1, clothing.** The zone fill is the Clothing figure as written: blue with a snowflake for cold, a droplet for wet, vermilion hatching for frostnip or frostbite, a crack for a worn garment. The torso zone's tint covers the torso and both arms.
- **Layer 2, health.** Lost part health shows as a dark fill rising from the bottom edge of the part, in proportion: a leg at 40 is 60% filled. It is a shape, so it reads without colour.
- **Problem glyphs.** One 5 × 5 glyph sits on each part with a problem: slash (cut), double slash (deep cut), ring and dot (infection), flame (burn), zigzag (sprain), split bar (break), two bars (splinted), circle (blister), dot (bruise), three ticks (quills), comma (leeches), star (head knock).
- **Untreated means blinking.** A glyph whose treatment is due blinks at 1 Hz, inside the photosensitivity rule of 3 Hz. A treated glyph is steady.
- **Timers.** A thin ring around a glyph empties toward its next mark: a dressing change, the 12-hour infection mark, a rewarm deadline, or the 3-day crooked mark.

**When it shows.** The figure appears when a clothing zone needs attention (as written), when any part is below 80, or when any problem is active. It stays while pinned. With the battery at 0 it goes dark with the strip, and the body cues remain.

### The Health panel

**The pack screen's Body panel gets a third view.** Its toggle row reads Clothing, Health and Camouflage; Clothing and Camouflage are as written in Clothing and Player systems and client.

1. **The figure.** The same 10 parts at full size, 96 × 192 GUI pixels, with health fill, glyphs and timers, plus lost digits and crooked marks.
2. **Hover a part.** A card shows the part's health and band, then one line per function it changes: "Left leg · 55 · Hurt. Walking ×0.8 · sprint 10 s · climbing slow."
3. **Problem rows.** Under the figure, problems in triage order, one row each: "Right shin · Deep cut · Dressed (usnea), clean 31 h · Next: change in 31 h." Each row ends with its one treatment and a Treat button that starts the hold, if the item is in the pack or storage within 8 m.
4. **Days left.** Each healing problem shows days to healed, and a second figure for Rest days: "Sprain, left ankle: 3 days · 1.5 on Rest days."
5. **The Vitals page** on the Trail Messenger shows the same rows as text, as written ("Cut, left forearm, bleeding. Bandage now: medkit has 4"), without the figure.

**Class.** `client.pack.BodyHealthView` inside the existing `PackScreen` Body panel; the HUD figure stays `client.hud.BodyFigureLayer`.

### No-HUD body cues

**The body tells the player where it hurts with no Trail Messenger.** These run whether the battery is full or dead, because they are the view, not the HUD.

| Part | Cue | When |
| --- | --- | --- |
| Head | Ears ring for 60 s; the view sways on fast turns; edges blur | A head hit; Hurt; Badly hurt |
| Head | One eye's side of the view darkens 15% | A swollen eye, a head cut bleeding into the eye |
| Torso | Breath catches on a sprint; a cough when lifting | Hurt torso; broken ribs |
| Arms | The swing slows and shakes at the top; the bow arm trembles | Sore or worse; Hurt or worse |
| Hands | Bandaged fingers on the first-person hand; a dropped snare or knot with a small sound | A dressed hand; Hurt hand on fine work |
| Hands | A stub where a finger was; a sling across the chest | A lost finger; a sling |
| Legs and feet | Uneven head bob and uneven footsteps | Limp |
| Feet | A wet squelch on every second step | Blister |
| Any bleeding part | Drips on the ground and red specks in snow, one every 2 s per Condition a minute | Bleeding |
| Any infected part | Red streaks drawn up the limb in first person when it is in view | Blood poisoning |

**Blood is sign.** Drips are decals by the sign rules in Tracks and sign and a scent trail by the scent rules, so a bleeding player draws ravens and wolves just as a wounded moose does.

## Self-care and feel

### The treat action

**Holding a treatment item and use treats the most urgent part it fits.** A bandage in hand goes to the worst bleeding wound; a splint kit to the unsplinted break; mittens off and use with empty hands rewarms the coldest frostnipped part. The prompt line names it first: "Hold  Bandage right shin (deep cut). Medkit 5."

**Sneak and use opens the self-care radial.** The radial shows the 10 parts around a small figure, lit where the held item helps, plus "All" at the centre. Releasing on a part starts that hold; "All" runs Treat all from Several wounds at once.

**The rules of the hold.**

1. Bandaging is the one hold allowed while bleeding, as written. Every other treatment waits until no wound bleeds.
2. A hold breaks on damage, a threat warning or the player moving more than 1 block. The ring keeps its progress for 10 real seconds.
3. Treating a hand uses the other hand, so its hold takes longer. Treating a foot takes the boots off first.
4. Every hold lasts at least 1 real second, so no treatment happens on a tap.

### Hold-to-work times

**Real seconds follow the active clock.** Most treatments are work time-lapse jobs (5 game minutes a real second). The bandage's 3 real seconds is written and fixed; it runs on the close clock while bleeding.

**Every body rate is per game minute.** Bleeding, infection marks, frost points and healing are defined per game minute, so the world option for day length (24–72 real minutes, default 24) rescales them with no second table. The real holds below are for the default day.

**Holds are co-op-safe.** A treatment's time-lapse charges game minutes to the treated player's own body record, and the world clock stays shared. Treating another player is a co-op feature for after 1.0 and is not built.

| Treatment | Game time | Real hold | Notes |
| --- | --- | --- | --- |
| Bandage | — | Arm, leg, off hand 3 s (written); head or chest 4 s; main hand 5 s; back 6 s; foot 3 s plus 10 s per garment off | The one hold allowed while bleeding |
| Clean and redress | 5 game minutes | 1 s | Needs boiled water |
| Salve on a part | 1 game minute | 1 s | One use from the tin |
| Splint | 10 game minutes; hand 3 | 2 s; hand 1 s | 15 game minutes (3 s) on one's own arm |
| Sling | 2 game minutes | 1 s | — |
| Rib wrap | 5 game minutes | 1 s | — |
| Sprain wrap | 1 game minute | 1 s | Written |
| Rewarm hands; face | 10; 5 game minutes | 2 s; 1 s | Hands written |
| Rewarm feet | 20 game minutes | 4 s | Boots off; needs a warm place or the wolf |
| Cool a burn | 5 game minutes | 1 s | Within 10 game minutes of the burn |
| Pull quills | 20 game minutes | 4 s | Written |
| Remove a leech | 1 game minute each; 10 game seconds with an ember | 1 s | Written |
| Cauterize | 2 game minutes to heat, then the press | 2 s press | Ideas from viewers › Cauterizing |

**A full treatment after a bear contact takes about 8 real seconds.** Two deep cuts and a broken hand: 3 s and 3 s of bandaging, then a 1 s hand splint and a 1 s sling. Bleeding stops after the first 6 seconds, about 36 game seconds on the close clock.

### The first injury of a run

**The first problem teaches the loop once.** Whatever it is, usually a knife slip or a stumble bruise, it runs this sequence.

1. The Trail Messenger line names the part and the treatment: "Cut, left hand. Bleeding 1 a minute. Bandage it."
2. The body figure pins itself for 2 game hours, and the left hand's glyph blinks.
3. The tip shows once: "Hold use with a bandage to dress it. Sneak and use to choose where."
4. After the bandage, the line reads "Bleeding stopped. Dressing clean for 24 h." The ring around the glyph starts to empty.
5. The journal writes "First blood. My own." The Health panel's tab glows until the player opens it once.

**The first of each kind gets one tip.** The first sprain teaches the wrap, the first break the splint, the first frostnip the armpits. Gentle repeats tips every time, Standard once and again after 24 hours clear, Brutal once per run, by the Warnings row in Realism presets.

### Trail Messenger and journal lines

**Body lines are typed `body`, with keys `messenger.yearofice.body.<id>`.** They name a part and a treatment, never a cause the player could not know. A plant is named only if identified, and sour fish is "something you ate". The Trail Messenger never reports trap catches; nothing in this tab changes that.

| Id | Line |
| --- | --- |
| `cut` | "Cut, left hand. Bleeding 1 a minute. Bandage it." |
| `several` | "Three wounds. Bleeding 7 a minute. The thigh first." |
| `bleed_stopped` | "Bleeding stopped. Dressing clean for 24 h." |
| `dressing_due` | "Left hand: change the dressing within 2 h." |
| `infected` | "The cut on your left hand is red and hot. Clean it and redress." |
| `sprain` | "Turned your right ankle. Wrap it." |
| `break` | "Left forearm is broken. Splint it: 2 sticks, 2 m of cord, moss." |
| `burn` | "Burned your right foot. Cool it in water, now." |
| `frostnip` | "Fingers, left hand: white and numb. Warm them now." |
| `digit_lost` | "You lost the little finger of your left hand." |
| `blister` | "Blister, right heel. Bandage it, or dry socks." |
| `head_knock` | "Hit your head. Ears ringing." |
| `something_ate` | "Weak and sick. Something you ate." |

**Journal lines are firsts.** "First blood. My own." "Splinted my own arm with two sticks and a length of cord." "Lost a finger to the cold. Day 214." "Sat in the creek with a burned foot. It worked." A run's year card counts wounds dressed, bones set and digits lost.

### The tedium guard

**Injury must cost hours, not attention.** These rules keep the loop short.

- **Treat all.** One hold handles every wound in order.
- **Batched changes.** Dressing changes due within 2 game hours of each other are offered as one, in the morning prompt at camp.
- **Quiet small things.** Bruises and swelling heal with no line, only a glyph. A part above 80 with no problem draws nothing.
- **Presets.** Bleeding and infection speed scale with the existing slider (×0.5 Gentle, ×1.5 Brutal), stored in `yearofice-common.toml`. Part damage itself does not scale, so the body teaches the same lessons on every preset.
- **Storage reach.** Treat buttons draw items from storage within 8 m, like station crafting, so a player at camp never digs through chests for a bandage.

## Data and build

### The body record, extended

**The per-part fields join `yearofice:body`; there is no second attachment.** The record in Player systems and client keeps every field. It gains these, each with a codec default, and its `schema` goes from 1 to 2 with an upgrade that sets every part to 100.

| Field | Type | Range | Notes |
| --- | --- | --- | --- |
| `part_hp` | byte × 10 | 0–100 | Order: head, torso, left\_arm, right\_arm, left\_hand, right\_hand, left\_leg, right\_leg, left\_foot, right\_foot |
| `d_sys` | float | 0–100 | The systemic pool; Condition stays the stored, synced value and is checked against the formula each step |
| `part_frost` | short × 10 | Frost points | Replaces `zone_frost` × 5; a zone's value is the highest of its parts |
| `lost_digits` | short | Bits 0–4 left fingers, 5–9 right fingers; a second short for toes | Permanent |
| `crooked` | short | 10 bits, one per part | Permanent |
| `injury_seq` | int | Counter | Seeds each part roll |
| `problems` | list, at most 16 | Gains `sub`, `pad`, `binding`, `clean_until`, `splint`, `sling`, `wrap`, `cooled`, `started`, `next_mark` | `part` already exists |

Saved size grows by about 120 bytes, to about 1.2 KB a player.

```json
{
  "schema": 2,
  "condition": 82.0,
  "d_sys": 12.57,
  "part_hp": [100, 100, 76, 100, 100, 100, 70, 100, 100, 100],
  "part_frost": [0, 0, 0, 0, 12, 12, 0, 0, 30, 30],
  "lost_digits": [0, 0],
  "crooked": 0,
  "injury_seq": 41,
  "problems": [
    { "id": "cut", "part": "left_arm", "sub": "forearm", "stage": 2, "pad": "usnea", "binding": "fibre_wrap", "clean_until": 20220, "started": 17340, "next_mark": 20220 },
    { "id": "cut", "part": "left_leg", "sub": "shin", "stage": 2, "pad": "dry_moss", "binding": "fibre_wrap", "clean_until": 18020, "started": 17300, "next_mark": 18020 }
  ]
}
```

The example is consistent before any healing: a bite on the left arm (−8) and a hatchet glance on the left leg (−10) leave `d_sys` at 6.32 + 6.25 = 12.57, and 100 − 12.57 − 1.68 − 3.75 = 82.0.

**Where parts enter the tick.** The 13 steps of The body tick keep their order. Step 6 runs `part_frost` per part, step 8 the split and the healing fraction, step 9 the local problem losses, and step 10 the bands, chains, limp, statuses and modifiers.

**The injury path.** `BodyService.applyInjury(player, injury, cause)` is the only way in, for animals, falls, tools and fire alike.

1. `InjuryService` reads the `injury_source` file named by the spec, or takes the spec's own loss.
2. `PartRoller` picks the part with `hash(seed, player, "part", injury_seq)` from the weights, the posture and any part the caller forces.
3. The split rule writes the part and the pool; the problem roll uses `hash(seed, player, "problem", injury_seq)`.
4. The problem joins the list in triage order; a tear hits the zone's outer garment.
5. `VitalChangedEvent`, the HUD line, the journal first and an immediate `body_sync` go out. The close clock starts if a wound bleeds.

### Data paths

**Every kind is singular and one entry per file,** by the lead's convention 6. The brief's `injury_sources` therefore becomes `injury_source`.

| Path | Holds | Count |
| --- | --- | --- |
| `data/yearofice/body/standard.json` | Gains a `parts` block: shares, gain 3, floors, band edges | 1 (existing file) |
| `data/yearofice/body_part/<part>.json` | Zone, side, share, floor, function factors per band | 10 |
| `data/yearofice/injury/<problem>.json` | Problem types: allowed parts, stages, effects by part group, treatment action ids | 13 |
| `data/yearofice/injury_source/<id>.json` | Condition loss, part weights, posture weights, problem roll, damage type, garment tear | 29 |
| `data/yearofice/illness/<id>.json` | The register: cause, incubation, stages, cure, duration, source tab | 20 |
| `data/yearofice/remedy/<id>.json` | Existing; gains `parts` (where it may be applied) | Existing |
| `data/yearofice/action/treat_<id>.json` | Treatment holds: time, items, parts, guards | 17 |
| `data/yearofice/damage_type/<id>.json` | Damage types, all `"scaling": "never"` | 25 |
| `data/yearofice/data_maps/damage_type/injury_source.json` | Every routed damage type, ours and vanilla, to its `injury_source` | 1 |

```json
{
  "condition_loss": 8,
  "parts": { "arms": 40, "legs": 25, "torso": 25, "head": 10 },
  "posture": {
    "face_down": { "first_hit": "pack", "parts": { "torso": 45, "legs": 30, "arms": 20, "head": 5 } }
  },
  "problems": [ { "id": "cut", "stage": 2, "chance": 0.3 }, { "id": "broken_bone", "chance": 0.1 } ],
  "damage_type": "yearofice:bear_bite",
  "tears_outer": 0.15,
  "close_clock": true
}
```

### Damage types and tags

**Damage types exist for routing and records, not for health.** Vanilla health stays at 20 and every player damage is cancelled into `DamageRouter`, as written. Our animals and tools call `applyInjury` directly with a `DamageSource` of our type, so the run record, `/damage` tests and logs all name the real cause.

**Our 25 types:** `knife_slip`, `hatchet_glance`, `fire_step`, `boiling_water`, `hot_object`, `cauterize`, `bear_swipe`, `bear_bite`, `wolf_lunge`, `beaver_bite`, `porcupine_quill`, `moose_stomp`, `falling_tree`, `branch`, `widowmaker`, `kickback`, `ice_slip`, `stumble`, `sting`, `insect_bite`, `leech`, `nettle`, `spray_blowback`, `seizure` and `exposure` (frost, for the record only). Each is `{"message_id": "yearofice.<id>", "exhaustion": 0.0, "scaling": "never"}`, with `"effects": "burning"` on the burn types.

| Tag | Holds | Read by |
| --- | --- | --- |
| `#yearofice:body_routed` | Ours, plus vanilla `fall`, `in_fire`, `on_fire`, `lightning_bolt`, `hot_floor`, `generic_kill`, `fell_out_of_world` | `DamageRouter`; anything outside it logs one warning, as written |
| `#yearofice:systemic` | `generic_kill`, `fell_out_of_world`, `exposure` | No part roll |
| `#yearofice:animal_strike` | The six animal types | Garment tears, the posture weights, the encounter journal |
| `#yearofice:burns` | `fire_step`, `boiling_water`, `hot_object`, `cauterize`, vanilla fire types, `lightning_bolt` | The cool-a-burn window |
| `#minecraft:bypasses_armor`, `#minecraft:bypasses_cooldown`, `#minecraft:no_knockback`, `#minecraft:bypasses_enchantments` | All of ours | Vanilla's checks, so no invulnerability frame swallows a second sting |
| `#minecraft:is_fire`, `#minecraft:is_fall` | The burn types; `stumble` | Vanilla code that tests them |

If NeoForge 1.21.1 refuses a data map on the damage type registry (it is a datapack registry), the same table moves to `data/yearofice/body/damage_routes.json`, read by our own listener.

### Java classes

| Class | Side | Does |
| --- | --- | --- |
| `sim.body.PartModel` | Both, pure | The split rule, the healing fraction, bands, factors, chains, limits; no Minecraft types |
| `body.injury.InjuryService` | Server | Implements `BodyService.applyInjury`; the five steps above |
| `body.injury.PartRoller` | Server | Weighted, seeded part pick |
| `body.injury.ProblemType`, `Problem`, `Triage` | Server | Problem data, the per-problem record, urgency order |
| `body.DamageRouter` | Server | Existing; vanilla types to `injury_source` through the data map |
| `body.Frost` | Server | Per-part frost counters and the frost problems |
| `body.illness.IllnessType`, `IllnessStepper` | Server | The register's files and their timers |
| `body.care.Treatments`, `TreatAll` | Server | Treatment holds through `build.work.WorkService`; checks part, item and guards |
| `body.status.StatusDeriver` | Server | The 27 statuses into the sync set |
| `body.Gait` | Server | The four attribute modifiers and the limp |
| `client.hud.BodyFigureLayer` | Client | Existing layer: parts, fill, glyphs, timers |
| `client.pack.BodyHealthView` | Client | The Health view of the Body panel |

**Also:** `body.part.BodyPart` (the 10-part enum), `body.injury.InjurySpec` (source, loss override, forced part, posture), `client.screen.SelfCareRadial`, `client.render.WoundOverlay` (bandages, sling, stubs, streaks), and the limp bob and dizziness in the existing `client.camera.BodyCamera`.

**`BodyService` gains** `partHealth(player, part)`, `factor(player, function)`, `canUse(player, hands)` (for bows, axes and fine work) and `limp(player)`. Bows, stations, minigames and travel call these instead of reading the record.

**Cost.** The part work adds about 3 µs to a body step: 10 bands, 6 chains, one formula check, no allocation. That stays inside the 15 µs step budget.

### Tuning and debug

**All body tuning lives in `yearofice-common.toml` under `[body]`, with the three presets.** Data files hold the shapes (weights, problem maps); the toml holds the dials. A preset sets every dial at once, and a dial set by hand marks the preset "Custom". GameTests run on Standard.

| Key | Gentle | Standard | Brutal | Unit |
| --- | --- | --- | --- | --- |
| `body.bleed_scale` | 0.5 | 1.0 | 1.5 | × Condition a game minute |
| `body.infection_scale` | 0.5 | 1.0 | 1.5 | × stage speed |
| `body.part_gain` | 3 | 3 | 3 | Part loss per Condition point |
| `body.core_floor` | 10 | 10 | 10 | Lowest health for head and torso |
| `body.digit_loss` | false | true | true | Deep frostbite can take digits |
| `body.tips` | every | once\_24h | once\_run | First-time tips |

**Every body system has a `/yoi` command,** op level 2, for testing on Ruhan's own worlds. Each prints the record change it made.

| Command | Does |
| --- | --- |
| `/yoi body show [player]` | Prints Condition, `d_sys`, the 10 parts, bands, problems with timers, statuses |
| `/yoi body part <part> <0-100>` | Sets one part's health and re-derives Condition |
| `/yoi body injure <injury_source> [part]` | Runs `BodyService.applyInjury` with an optional forced part |
| `/yoi body problem add <problem> <part> [stage]` | Adds a problem directly; `remove <index>` takes it off |
| `/yoi body treat <problem_index>` | Runs the treatment hold at once, items not needed |
| `/yoi body ill <illness> [stage]` | Starts an illness; `cure <illness>` ends it |
| `/yoi body digits <hand_or_foot> <mask>` | Sets lost digits |
| `/yoi body heal` | Every part 100, problems and illnesses cleared, Condition 100 |
| `/yoi body roll <injury_source> <n>` | Dry-runs n part and problem rolls and prints the counts; changes nothing |

### Milestones

| Milestone | Ships |
| --- | --- |
| M4 Body | Parts, the split and healing rules, the `[body]` toml block and `/yoi body` commands, bands, chains, gait modifiers, the problem map, tool, fire, fall, stumble and ice-slip sources, per-part frost, treatments, the self-care radial, the figure and Health view, the register, statuses, GameTests 1–7 |
| M5 Food and goals | The botulism trigger with sour fish; leeches and water tigers with water life; fine-work factors in knapping and cordage scoring |
| M6 Danger and big game | Animal sources, posture weights, garment tears, the limp in the wolf test (P5), GameTest 8 |
| M7 The year | Snow in the boots, snowshoes' foot rule, lightning burns, the cold cough, snow blindness on the head |

### GameTests

1. **`body/part_split_exact` (batch `m4`).** A forced `bear_bite` (L 8) on the left arm of a fresh player leaves Condition 92.00 ±0.01, left arm 76 and `d_sys` 6.32 ±0.01.
2. **`body/parts_heal_with_condition` (`m4`).** After that bite, a fed player asleep in a warm bed reaches Condition 100 and every part 100 within the same game minute, after 8 game hours ±1 minute.
3. **`body/source_weights` (`m4`).** Over 1,000 forced knife slips with the right hand as main, 80–90% land on the left hand. Over 500 falls at an effective 4 m, each gives one sprain and 65–75% land on a foot or leg; at 8 m, each hits 2 parts and costs exactly 10.
4. **`body/sprain_limp_and_stick` (`m4`).** A mild sprain on the right ankle sets the `yearofice:injury` modifier to ×0.50 ±0.01 and `body_public` limp to right, severity 2. A walking stick in the off hand raises it to ×0.75 ±0.01.
5. **`body/broken_arm_blocks` (`m4`).** A broken left forearm refuses the bow draw and the axe and allows the knife. Splinted, from day 11 it allows the rod. With no sling, 100 stumble falls re-break it 20–30 times; with a sling, none.
6. **`body/treat_all_order` (`m4`).** Two deep cuts and a minor cut with 3 bandages: Treat all stops every bleed within 9–10 real seconds, the deep cuts first. With 2 bandages, the minor cut is still bleeding and the out-of-bandages line fires once.
7. **`body/digit_survives_sos` (`m4`).** Deep frostbite on the left hand left 6 game hours takes the little finger and makes crafting ×1.05. After the SOS return the finger is still gone, every part is 100 and Condition is 80.
8. **`ecosystem/play_dead_parts` (`m6`).** Over 500 defensive grizzly contacts with the player face down and a pack on, the first bite hits the pack every time, and under 8% of later bites land on the head; standing, 8–12% do.

### Conflicts and proposals

**Conflicts with existing rules, each with a proposed fix.** C2 to C6 sit with the sickness register.

- **C1. When Condition heals.** Read as: healing runs unless a wound bleeds, an infection is active or warmth is under 30.
- **C7. Data path.** `injury_sources` becomes the singular `injury_source/`, by convention 6.
- **C8. Frost counters.** `zone_frost` × 5 becomes `part_frost` × 10; the zone value is derived.
- **C9. Burns by part.** "Work 20% slower" is read per part: hands and arms slow that side's jobs, feet and legs slow walking, torso and head keep the global rule.
- **C10. Falls with no loss.** Falls over 3 m and 6 m get −4 and −10, so a part can move.
- **C11. Swim time.** A Hurt or Badly hurt torso cuts useful time in water from 10 game minutes to 8 or 6. This moves a drowning number and needs the Director's sign-off.
- **C12. Reviewer A's Health pass.** RECONCILE 112: reviewer A also edits Blueprint › Health and injuries. Where its numbers differ from this tab's written column, the Blueprint wins and this tab's tables follow.

**Proposals that add a rule, for sign-off.** P1 the part model (Clothing shares, gain 3, floors of 10). P2 broken ribs. P3, for concept 94: a stage 2 infection cleaned, salved, padded with usnea and kept clean stops advancing; its blackouts end after 24 game hours and it heals in 7 days, with antibiotics still the faster cure. P4 hand blisters: 3 game hours of chopping or paddling bare-handed, 30%. P5 a severity 3 limp counts as weak for wolves. P6 snow over 40 cm in boots wets the feet 10% per 100 m (Open world in depth › Traversal). P7 a thick outer layer turns a bite's minor cut into a bruise 25% of the time. P8 bare metal below −20 °C warms a hand's frost limit by 5 °C. P9 the adrenaline burst on a mild sprain makes it bad 25% of the time. P10 bruises for kickbacks, seizure falls and 2–3 m falls. Signed off by the Studio lead in the Ready-for-development pass (2026-10-19): P1–P10 are accepted as written, with P5 as the Director ruled (RECONCILE 134) and P6 at Open world's 10% per 100 m, so one rule serves both tabs.
