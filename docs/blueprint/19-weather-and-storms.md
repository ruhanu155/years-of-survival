# Weather and storms

The valley's weather comes from one model: a year table drawn each May 1, read by the hour and bent by the land. This tab sets that model, a catalogue of fourteen storms with their signs, effects and counterplay, how to read the sky, what each shelter withstands, how storms look and sound, and how it is all built in the mod.

## Where weather lives

**This tab is the one home for weather.** It builds on rules already written in eight places and changes only the numbers listed at the end, each with a reason. Everything here is a pure function of the world seed, the year and the hour, so a chunk loaded today and one loaded after a month agree.

| Rule | Lives in | This tab |
| --- | --- | --- |
| Day chain, offsets, forecast, inversion, cold snaps, snow, freeze-up, breakup | Blueprint › Calendar, daylight and weather | Adds air masses, fronts by the hour, monthly scaling |
| Wind chill, wetness, shelter, fire in rain | Blueprint › Warmth; Fire, water and light | Adds storm damage to pieces |
| Whiteouts, snow trench, thin ice | Blueprint › Danger in detail | Adds ground blizzards and navigation by feel |
| State machine, `env_sync`, sky renderer | Minecraft engine › Weather; Sky and aurora | Fills in the model and hooks |
| Weather card, thunderstorms, fire danger, mist | Every concept from the video | Moves strike logic to `env.phenomena` |
| Sky signs, aurora, ice fog, cold lid, warm wind, windthrow, micro-climates | Land and seasons in play | Adds signs for the new storms |
| Animal reactions to weather | Land animals in depth › Fire, smoke, light, noise and weather | Adds crust, glaze and heat |
| Rare events | Open world in depth › Rare events | Adds the storm of the decade |

## One weather model

The model has four steps: a year table drawn on May 1, hourly fields read from it, local bending by terrain, and storms placed on top. The Environment field (Layer 2) is the only reader the game systems see.

### Air masses and fronts

**Every day carries one air mass.** It is a label derived from the day chain and the anomaly, not a second random draw. It sets wind direction, humidity, cloud base and which precipitation types are possible.

| Air mass | Id | Chosen when (first match wins) | Wind | Humidity | Pressure offset | Brings |
| --- | --- | --- | --- | --- | --- | --- |
| Downslope | `downslope` | A warm-wind day from the year table (Land and seasons in play › The warm wind) | N, off the north range, 8–15 m/s | 30–45% | −4 hPa | Thaw, rime shed, crust after |
| Arctic | `arctic` | November–March, Clear or Cloudy, anomaly −8 °C or lower | NE, 0–3 m/s | 70–80% (over ice) | +12 to +25 hPa | Cold snaps, ice fog, inversions |
| Pacific | `pacific` | Any Wet or Severe day | S to SW, 3–8 m/s | 85–100% | −8 (Wet) or −16 (Severe) hPa | Rain, snow, freezing rain, blizzards |
| Interior heat | `heat` | June–August, Clear or Cloudy, anomaly +4 °C or higher | Variable, 1–4 m/s | 25–40% | +3 hPa | Heat days, thunderstorms, smoke |
| Continental | `continental` | Every other day | W to NE, 1–5 m/s | 50–70% | 0 | Ordinary fair weather |

**A front is a change of air mass.** A warm front is any arrival of `pacific` air, which matches the Blueprint's definition (Clear or Cloudy into Wet or Severe). A cold front is the change from `pacific` back to `arctic` or `continental`, which is where the existing −6 °C winter kick lands.

**Fronts have an hour.** Each front day draws its front hour h\_f from 0 to 23 with `Rng.of(seed, "weather", year, day, "front")`. Temperature, wind and pressure ramp across it instead of jumping at midnight. Precipitation starts within 2 hours of h\_f.

### The generator

**On each May 1 the year table is drawn whole,** as the Blueprint says, from `mulberry32(hash(attemptSeed, "weather", year))`. It stores 365 day records and 8,760 hour records. SavedData `yearofice_weather` keeps only the year number, the carried anomaly, the decade draw and the storm log; the table is rebuilt from the seed on load in under 40 ms.

1. **Day chain.** The four-state Markov chain and its four seasonal tables stand as written.
2. **Anomaly.** a\_d = 0.7 a\_(d−1) + noise of ±4 °C, plus kicks: −6 °C after a winter Severe day, −15 °C in a hard winter, and the storm-of-the-decade kicks below.
3. **Day mean.** The month's normal interpolated to the day, plus a\_d, plus the state offset, re-centred so the long-run monthly mean equals the normal.
4. **Air mass and front hour,** by the rules above.
5. **Amounts.** Each Wet or Severe day draws its precipitation, then the month scales all amounts so the expected total equals the month's normal (below).
6. **Storms.** The storm catalogue places its events on the table, each with its own named stream.
7. **Hours.** Each hour's fields are computed from the day records with the formulas below.

**Hourly temperature.**

```latex
T(d,h) = M_f(d,h) + S_m \cdot c_s \cdot \kappa(h) + \Delta_{storm}(d,h)
```

```latex
M_f(d,h) = M_{d-1} + (M_d - M_{d-1}) \cdot \frac{1}{1 + e^{-(h - h_f)/3}}
```

- M\_d is the day mean; on days without a front, M\_f is simply M\_d.
- S\_m is the month's swing from the climate table; κ(h) is the 24-entry curve in `weather/interior_alaska.json`, coldest just before sunrise, warmest at 15:00.
- c\_s is the sky factor: Clear 1.4 and Cloudy 0.6 as written, Wet 0.5 and Severe 0.3 (new).
- Δ\_storm is a storm's own shape, such as the warm wind's +15 to +25 °C over 6 hours.

**Precipitation amounts.** The state table's fixed ranges give way to monthly amounts. A Wet day draws u ∈ \[0.5, 1.5\] and gets u × k\_m mm; a Severe day gets 3.5 × u × k\_m mm.

```latex
k_m = \frac{P_m}{n_m \,(p_{wet} + 3.5\, p_{severe})}
```

P\_m is the month's normal in mm of water, n\_m its days, and p the chain's long-run share of Wet and Severe days for the season. So k\_m is the mm of an average Wet day: about 3.7 mm in July and 1.1 mm in January. Snow is 1.5 cm per mm of water, the dry interior ratio, and storm-catalogue totals count inside P\_m.

**Winter Severe days come in two kinds.** 40% are snowing blizzards, which take 2.3 times the Severe draw (about 6–20 cm in midwinter, more in November and March). 60% are ground blizzards, which take 0.15 times it (under 2 cm) with wind of 10–20 m/s over loose snow. The mean stays the same, and the 20 m whiteout holds wherever snow is blowing (Storm catalogue).

**Hourly wind and gusts.**

```latex
v(h) = v_d \cdot \delta_m(h) \qquad g(h) = v(h) \cdot G_s + g_{cell}(h)
```

v\_d is drawn in the state's range, δ\_m(h) is the diurnal shape (summer 0.6 at 05:00 to 1.3 at 15:00; November–February flat at 1.0), and G\_s is the gust factor: Clear 1.4, Cloudy 1.5, Wet 1.6, Severe 1.8. g\_cell adds 8–15 m/s at a thunderstorm's gust front. Direction comes from the air mass, veering 20–60° across a front.

**Pressure.** Sea-level pressure is the month's base plus the air-mass offset, ramped over the 12 game hours before h\_f, as the Blueprint's falling barometer says. A rising trend of 3 hPa or more in 3 hours follows every cold front.

**Cloud, humidity and visibility.** Cloud cover follows the Video tab's weather-card bands, plus a summer build of +30% between 13:00 and 19:00 on Clear `heat` days. Relative humidity comes from the air mass and rises to 100% in precipitation. Visibility is the lowest of the state value, the precipitation value, fog, ice fog and smoke.

| Precipitation | Rate | Visibility |
| --- | --- | --- |
| Drizzle or light rain | Under 1 mm/h | 2 km |
| Rain | 1–4 mm/h | 300 m (Blueprint) |
| Heavy rain | Over 4 mm/h | 150 m |
| Light snow | Under 0.5 cm/h | 600 m |
| Snow | 0.5–2 cm/h | 150 m (Blueprint) |
| Snowing blizzard | Any, wind over 12 m/s | 20 m (Blueprint) |
| Ground blizzard | No snow, wind over 10 m/s, loose snow | 50–200 m at eye height, full above the drift layer |
| Hail | Cells | 200 m |

**Precipitation type by the hour.** The Blueprint's day rule (snow below a 1 °C mean) sets the day's class. In the shoulder months, when the day mean is within 3 °C of 1 °C, each hour picks its own type: snow at 0.5 °C or below, sleet from 0.5 to 2 °C, rain above 2 °C. Freezing rain and hail have their own triggers in the Storm catalogue.

### Monthly climate at 64°N

**These are the home valley floor's normals, the targets the generator re-centres on.** They follow interior-Alaska values near Fairbanks from general knowledge, not looked up, and change no Blueprint number except the swings in Changed numbers. Snow depth is open ground at month end, from the balance simulator over 500 seeded winters.

| Month | Mean °C | Swing ±°C | Typical high / low °C | Hourly bounds °C | Precip mm | Fresh snow cm | Snow depth cm | Wind m/s | Pressure hPa | Thunder days | Daylight h (mid-month) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| May | 9.5 | 5 | 14.5 / 4.5 | −12 to 29 | 15 | 1 | 0 | 3.3 | 1013 | 0.3 | 18.6 |
| June | 15.5 | 5 | 20.5 / 10.5 | −2 to 33 | 35 | 0 | 0 | 3.1 | 1011 | 4 | 21.6 |
| July | 17 | 5 | 22 / 12 | 1 to 34 | 55 | 0 | 0 | 2.8 | 1010 | 5 | 20.6 |
| August | 14 | 5 | 19 / 9 | −5 to 31 | 50 | 0 | 0 | 2.7 | 1010 | 2 | 16.8 |
| September | 7.5 | 4 | 11.5 / 3.5 | −15 to 26 | 30 | 2 | 0 | 2.8 | 1012 | 0 | 13.0 |
| October | −3.5 | 4 | 0.5 / −7.5 | −32 to 18 | 22 | 20 | 17 | 2.6 | 1014 | 0 | 9.6 |
| November | −15.5 | 3 | −12.5 / −18.5 | −45 to 8 | 16 | 24 | 33 | 2.2 | 1018 | 0 | 6.1 |
| December | −21 | 3 | −18 / −24 | −50 to 6 | 13 | 20 | 42 | 2.0 | 1021 | 0 | 3.8 |
| January | −23 | 3 | −20 / −26 | −52 to 5 | 13 | 20 | 50 | 2.0 | 1022 | 0 | 4.7 |
| February | −20 | 3 | −17 / −23 | −50 to 7 | 10 | 15 | 51 | 2.3 | 1020 | 0 | 8.0 |
| March | −12 | 6 | −6 / −18 | −45 to 12 | 9 | 13 | 50 (peak 55–70 mid-month) | 2.8 | 1018 | 0 | 11.6 |
| April | −1 | 6 | 5 / −7 | −32 to 20 | 7 | 6 | 5 | 3.2 | 1015 | 0 | 15.1 |

- **Hourly bounds** are soft limits: an hour past a bound is pulled back halfway, so records happen but rarely. Only the storm of the decade may pass them, by up to 4 °C.
- **The year's totals** are 275 mm of water and about 120 cm of fresh snow, with 1.5 cm of snow per mm.
- **January, July and their swings** match the weather entry already in Open world in depth (−23 °C, ±3; 17 °C, ±5).

### Local weather: inversions, gap and ridge winds, lake fog

**The valley bends the hour's weather.** `EnvService.sample` applies these after the valley value and before the Land tab's micro-climate offsets. All are pure functions of position and the hour.

- **Inversion and cold lid.** The Blueprint's rule (+3 °C per 100 m up to +8 °C, then −0.65 °C per 100 m) and the Land tab's cold lid stand unchanged.
- **Ridge exposure.** Wind is multiplied by 1 + 0.4 × clamp((z − z\_floor) / 300 m, 0, 1.5), so a ridge 450 m above the floor gets ×1.6. Above treeline it gets ×1.3 more. Canopy stays ×0.3, as the Environment field says.
- **Gap winds.** When the hour's wind is within 45° of a pass's axis, wind in the pass is ×1.8 and ×1.4 for 2 km below its mouth. Windgap Pass is the home valley's gap; every Interior pass on the World map does the same. The Land tab's cap cloud on Twin Horn is the sign.
- **Drainage wind.** On clear nights with valley wind under 2 m/s, air slides down the floor and creeks at 0.5–1.5 m/s from 21:00 to 09:00. This is the flow that carries the cold lid's scent drain, so the two rules are one wind.
- **Lake steam and flurries.** From October 1 until the big lake freezes over, air 10 °C or more colder than the water raises steam 2–10 m high. With wind of 3–10 m/s, the downwind shore within 2 km gets 1–4 cm of snow while the valley stays clear.

### Climate offsets for the Interior and Beyond

**One sky over the Interior, many beyond it.** The home valley and every Interior district read the same year table, because a front is wider than the 64 km Interior. Each 4 × 4 block of Beyond provinces (about 262 km a side) draws its own year table from `hash(seed, "weather", year, cellX, cellZ)`, blended linearly over 16 km at cell edges, so a long sled trip meets different weather with no seam.

**Fronts travel.** Fronts arrive from the southwest at 40 km/h. A district's front hour is h\_f minus its distance along that axis from the valley centre divided by 40 km/h, so a storm reaches the Varden Range hours before the valley.

| Place | Temperature | Precipitation and snow | Wind | Other |
| --- | --- | --- | --- | --- |
| Interior, any district | Lapse −6.5 °C per 1,000 m in summer; winter inversion as written; cold-air pooling −8 to +8 °C from the Interior plan | As the valley | Ridge and gap rules | Thunder days ×1.3 in the Smoke Hills and other burn districts |
| Interior, range above treeline | As above | ×2.5, snow from September 1 to June 15 | ×1.5 | Ground blizzards ×2 |
| Interior, below a pass | +2 °C on warm-wind days per pass (World map) | — | Gap rule | Warm wind 1 extra day a winter |
| Beyond: range and ice | −3 °C | ×2.5 | ×1.5 | Glacier wind 3–6 m/s down-valley, day and night |
| Beyond: domes and barrens | — | Snow ×0.8 | ×1.4 | Ground blizzards ×2 |
| Beyond: lowland flats and lakes | −2 °C in winter, +1 °C in summer | — | — | Mist and ice fog ×1.5; bugs ×1.3 |
| Beyond: big river | — | — | ×1.3 along the river | Mist ×1.5 |
| Beyond: burn country | +1 °C in summer | — | — | Thunder days ×1.3; smoke days ×2 |
| Beyond: mixed | — | — | — | — |
| Beyond: coast | +15 °C in winter, −3 °C in summer, against the valley | ×6; sea-level snow peaks 60–150 cm | — | The 7th province kind, drawn only south of the Interior at z ≥ 98,304 (World map; RECONCILE 154) |

### Deterministic and replayable

**Nothing about weather depends on who was watching.** Every roll comes from a named stream keyed by year, day, hour, place and event id, never by tick or call order, as the lead's determinism rules require.

1. **Live and caught up are one path.** Derived state (snow layers, crust, glaze, hail cover, flood water) settles straight to the current hour from the table. History (blowdown, broken limbs, strikes, burns) is planned per live 32 m cell at the event's hour and applied through World edits (Layer 9), oldest first, so a chunk loaded a month late gets the same trees down.
2. **Jumps run closed form.** A `ClockEvent.Jump`, such as the SOS flight, settles every storm in the jumped span the same way.
3. **The forecast never moves.** Its blur rolls are keyed by day and lead time (Blueprint), and sky signs by evening (Land).

### Shared sky, per-player clocks

**Weather runs on the world clock and is shared by every player.** Under the solo-first decision it never reads a player's rate, so the co-op rules can come later without touching it. Time-lapse, the close clock and the SOS jump are per player in future co-op; weather, like fires and animals, keeps the shared clock, as the lead's co-op note proposes.

**Day length changes nothing here.** Every rate in this tab is per game minute or game hour, so the world-creation slider (24–72 real minutes, default 24) only stretches real time. Presentation runs in real seconds: rain falls at the same speed on screen whatever the slider says.

## Storm catalogue

Each storm is a data file placed on the year table by its own stream. Counts are for the home valley; lead time is what the messenger's forecast or the radio gives at best, and the sky signs are in Reading the sky.

| Storm | Id | Months | Expected per year | Best lead time | Duration | Peak numbers |
| --- | --- | --- | --- | --- | --- | --- |
| Thunderstorm | `thunderstorm` | June–August | 11 thunder days; 3–6 cells over the player | 1 day (region), 30 min (cell) | Cells 1–3 h | Strike every 4–10 game min within 2 km; gusts to 25 m/s |
| Smoke day | `smoke_day` | June 20–August 31 | 8 (0–25) | 1–2 days (radio) | 1–10 days | Visibility 0.3–10 km |
| Hail | `hail` | June–August | 0.6 over the player | 10–30 min | 5–15 min | Stones 5–30 mm, 1–3 cm cover |
| Windstorm | `windstorm` | September–November, March–May | 3 | 12–36 h | 6–18 h | Mean 12–18, gusts 20–30 m/s |
| Heavy rain | `heavy_rain` | July–August | 1.2 | 1–2 days | 24–48 h | 30–60 mm; creeks +0.5–1.5 m in 6 h |
| Early wet snow | `wet_snow` | September 5–October 10 | 0.5 | 1 day | 6–18 h | 10–30 cm of heavy snow on leaves |
| Freezing rain | `freezing_rain` | October 10–December 10, February 20–April 15 | 0.5; an ice storm (10 mm+) 0.15 | 12–24 h, half the time | 2–12 h | Glaze 2–15 mm |
| Blizzard | `blizzard`, `ground_blizzard` | November–March | 3–5 snowing; 5–7 ground | 1 day | 6–24 h; ground 3–12 h | Wind 12–20 m/s; 20 m sight |
| Cold snap | `cold_snap` | November 15–March 10 | 2–4 | 3 days | 3–14 days | −35 to −50 °C on the floor |
| Warm wind and rain on snow | `warm_wind`, `rain_on_snow` | November–March | 1–3; 1 | 3 days at 60% | 12–48 h | +15–25 °C; 2–10 mm rain on snow |
| Breakup storm and ice jam | `breakup_storm` | April 15–May 10 | Jam yearly; storm 0.4 | 2 days | 1–3 days | River +1–3 m behind the jam |
| Aurora night | `aurora` | September–April | Index 2+ on about 40% of clear nights | 1 day | 22:00–03:00 | Index 0–9 |
| Heat and bug surge | `heat_wave`, `bug_surge` | June 10–July 31 | 1–2 spells | 2 days | 3–6 days | Highs 27–33 °C |
| Storm of the decade | `decade_storm` | By variant | 0.1 | 3 days (radio) | 3–5 days | By variant |

### Thunderstorms, lightning and wildfire ignition

**The Video tab's thunderstorm rules stand:** cells from 14:00 to 20:00, the flash-to-thunder delay, strikes on the highest point in each 32 × 32 cell, the crouch rule, storm-day jobs at ×1.2. This adds where cells go and what they leave.

- **Cells move.** Each Severe summer day places 1–4 cells, 3–8 km wide, moving with the day's wind at 20–40 km/h. A cell passes a point in 30–90 game minutes.
- **Odds.** Thunder is heard in the valley on about 11 days a year: 4 in June, 5 in July, 2 in August. They come from every Severe summer day (about 6), 15% of Wet summer days (a shower with thunder) and 25% of Cloudy days with an anomaly of +2 °C or more (one isolated cell, strikes at half rate). A cell crosses the player's position on 3–6 of them.
- **Player strike.** The chance rule stands; the harm is the Body tab's `lightning` source (−30 Condition and a burn), which replaces the Video tab's −60.
- **The radio mast.** A raised 20 m mast is the highest point in its cell. A strike on it kills the radio 50% of the time unless a ground wire runs to a stake: 2 m of the plane's cable, 10 game minutes.
- **Ignition.** A strike lights a dead or struck spruce 25% of the time after 5 dry days (Video). It escalates to `fire.Wildfire` only in a drawn wildfire year or at Extreme fire danger (RECONCILE 38). Off-screen, live 32 m cells roll ignition on the stand map, with no blocks.
- **Holdover fires.** 10% of lightning ignitions smoulder in duff for 1–6 days before showing smoke, so a fire can appear on a clear day after the storm.
- **Counterplay.** Leave ridges, open water and lone trees when thunder is under 10 km. Cover the woodpile. Ground the mast.

### Smoke days

**Smoke comes from fires the player never sees.** Each summer draws 0–6 distant fires (mean 2.5) in the Interior and Beyond, each burning 5–30 days. A day is a smoke day when the wind blows from a burning fire within 150 km, within 40° of its bearing.

| Smoke | Visibility | Sun and solar charge | Air | Effects |
| --- | --- | --- | --- | --- |
| Light | 3–10 km | Orange sun; charge ×0.6 | High −1 °C | The far range fades; landmarks beyond 5 km vanish |
| Moderate | 1–3 km | Red disc; charge ×0.3 | High −2 °C | Bugs ×0.7; animals smell the player at ×0.7 range |
| Heavy | 300 m–1 km | No shadows; charge ×0.1 | High −4 °C | Sleep quality ×0.9; Spirits −1 per 2 heavy days |

- **Messenger.** Only what the program reports: "Smoke from a fire about 80 km southwest." Pete Lund's drop-day reports place fires to ±5 km.
- **Counterplay.** Plan solar charging around it. Heavy smoke from the north-east with no reported fire is the valley's own fire: check the Fire danger line.

### Hail

**Hail falls from 15% of June and July cells over the player, and 5% in August.** It lasts 5–15 game minutes and covers the ground 1–3 cm deep. The cover melts in 1–3 game hours.

| Stones | Share | Player | Garden (leafed crops) | Berries (ripe) | Pieces |
| --- | --- | --- | --- | --- | --- |
| Pea, 5–10 mm | 70% | Noise only | Yield −10% | −10% | — |
| Marble, 10–20 mm | 25% | Bare head: −1 Condition per 30 game seconds; a hat stops it | −30% | −30% | Hide window torn 20%; tarp holed 10% |
| Large, 20–30 mm | 5% | −1 Condition per 10 game seconds, hat or not, out of cover | −60% | −50% | Hide window torn 60%; bark roof −20 HP per 10 game minutes |

- **The feast after.** Knocked-off berries lie on the ground for 2 days. Grouse and bears come to them, so a hailed slope is a hunting spot.
- **Birds.** Ground nests with eggs on open ground lose 10%; Birds and the air reads the event.
- **Counterplay.** Spruce canopy stops all of it. Rawhide or plank lids over beds (cold frames hold), and a hat.

### Windstorms and blowdown

**A windstorm is a Severe or Wet day with a mean wind of 12–18 m/s and gusts of 20–30 m/s.** Three a year: fall storms off the Pacific, spring storms as the snow goes, and summer downbursts of 5–15 game minutes under a thunderstorm.

- **Blowdown.** The Land tab's windthrow table stands, scaled by gust: chance × (g / 15)³ for g of 15 m/s or more. At 25 m/s that is ×4.6, so an edge tree falls 4.6% of the time per storm. Birches still in leaf in September count ×1.5.
- **Danger.** The Land tab's tree falling within 20 m once per 100 game hours scales the same way. The creak-and-snap cue stands.
- **Player.** Wind chill follows the Warmth rule to its 16 °C cap. Walking into gusts over 20 m/s runs at 80% speed, and a carried tarp or hide sail pulls the player 1 m sideways per gust.
- **Plants and decay.** Windthrown trees join the deadwood decay classes. Their gaps fill with fireweed and birch through the May 1 cover pass.
- **Fire.** Ember radius +1 block per 5 m/s of wind over 5 m/s. A Low fire with no windbreak goes out in gusts over 15 m/s. A wildfire's spread rate doubles.
- **Water.** On lakes over 500 m long, waves swamp a loaded canoe at 5% a game minute in wind over 10 m/s. Bites ×0.7 on open water but ×1.2 on the windward shore, where food is blown in.
- **Ice and snow.** Before freeze-up a windstorm delays lake freeze-over by 2–5 days. In winter it scours lake ice to black ice and builds lee drifts.
- **Animals.** Moose bed in spruce; hares and grouse stay in cover; wolves rest and travel after.
- **Counterplay.** Fell or keep a 20 m buffer at clearing edges (Land), batten down, tie the canoe, keep the fire in a ring behind a reflector.

### Heavy rain, flash floods and river rise

**Heavy rain is a 24–48 game-hour Pacific rain of 30–60 mm, about once a year from July 1 to August 31.** It counts inside the month's precipitation, so the rest of that month runs drier.

```latex
\Delta h_{creek} = 0.02 \cdot R_{24} \qquad \Delta h_{river} = 0.025 \cdot R_{48}
```

R is rain in mm over the hours shown, and Δh in metres. Creeks peak 6 game hours after the heaviest rain and fall in a day. The main river peaks 2–4 days later and falls over 5 days. `FloodController` raises water on the flood mask up to the normal high-water line; only the Long Rain (storm of the decade) reaches the flood-year line in summer.

- **Travel.** A side creek in flood runs at 1.5–2.5 m/s: no wading. Fords on the World map close until it drops.
- **Fish.** Muddy water gives the Blueprint's ×0.4 bite penalty for 3–5 days. Salmon hold in eddies and stop moving upstream until the river falls.
- **Beavers.** A dam takes a `BREACH` op at a 15% chance per 0.5 m of creek rise. The colony rebuilds within 5 days.
- **Ground.** Moisture reaches 100 on every cell; mushroom flushes follow in 5–8 days; trails turn to mud for 3 days.
- **Structures.** Water through a pole-and-bark roof (10% of storm rain, Crafting) soaks the room; nets in current tear.
- **Counterplay.** Pull nets and creek sets on the forecast. Keep caches and the canoe above the high-water marks on the trees.

### Early wet snow

**Once every two years, a Pacific storm drops 10–30 cm of heavy snow between September 5 and October 10, while birch and aspen still hold their leaves.** The snow is at 0 to +1 °C and weighs 2.5 times fresh snow. Most of it melts within 2–5 days.

- **Broken limbs.** Each birch, aspen and alder in leaf (Green, Yellow or early Falling stage) takes a `LIMB_BREAK` op with chance 2% × (s / 10 cm)², where s is the wet snow depth: 8% at 20 cm, 18% at 30 cm. Young birches under 8 m bend into arches instead and stay bent for years.
- **Trails.** Each 100 m of cut trail through broadleaf gets 1–4 fallen limbs: 1 game minute each with the hatchet, and every limb is half an armload of birch.
- **Moose.** Broken crowns are browse at nose height. Moose come to broken stands within 2 days and stay 2 weeks, so the storm makes a hunting ground.
- **Garden and berries.** Unharvested crops above ground lose 30%; potatoes are safe. Lingonberries and cranberries come out unharmed when it melts.
- **Pieces.** Roof load uses ρ 2.5 (Shelter and preparation), so 30 cm on an unbraced roof is a 125% load.
- **Travel.** Snowshoes ball up: 80% speed until knocked clear (1 game second, the use key). Walking in it soaks boots in 30 game minutes.
- **Counterplay.** Shovel or brace roofs the same night. Pick the garden on the forecast.

### Freezing rain and ice storms

**Freezing rain needs warm Pacific air riding over cold air pooled on the floor.** An hour is freezing rain when all of these hold: precipitation from a `pacific` day, valley air from −6 to 0 °C, and an inversion carried over from the night or an `arctic` day before. About one year in two has an event; one in seven has an ice storm of 10 mm or more.

**Benches can get rain while the floor glazes.** Under the lid a bench 30–80 m up runs 1–3 °C warmer, so the same hour can be rain on the bench and glaze on the river flats. A Practiced land reader learns why the bench camp stays clear.

```latex
g_{mm} = 0.7 \cdot \sum_h r_h \cdot e \qquad e = 1 \text{ open},\ 0.3 \text{ under spruce canopy}
```

r\_h is the hour's rate in mm. Glaze melts at 0.5 mm per game hour above 0 °C (twice that in sun) and sublimates 0.2 mm a day below it.

| Glaze | Ground and travel | Trees | Gear and pieces |
| --- | --- | --- | --- |
| 2 mm | Walking slip 1% a second, running 3% (bare-ice rule); sleds ×1.2 on the flats | Twigs ring and tinkle in wind | Firewood left out is wet; bowstrings wet |
| 5 mm | As above; slopes over 15° need creepers or crawling | — | Snares and traps freeze shut: catch ×0.5 until reset; doors stick (30 s to open a hide flap; 10% a plank door jams for 5 game minutes); a flat tarp tears at 8 mm |
| 10 mm | As above | `LIMB_BREAK` on 3% of birch and aspen and 1% of spruce for each 5 mm over 5 mm | Pitched tarps sag |
| 15 mm and over | As above | Limbs fall all night: 1 crack a game minute within 50 m | Roofs carry 1 cm of fresh-snow load per mm |

**The crust that starves.** Glaze or rain on snow leaves an ice layer in the snow model. While an ice layer of 5 mm or more lies under 20 cm or more of snow, it shapes the winter.

- **Caribou and muskox** cannot crater for lichen. A herd moves 20–60 km out of the district; one that stays has winter deaths ×2 in the population step.
- **Moose** break through a surface crust and wolves run on it: wolf kill rate on moose ×1.5 for 14 days after a surface crust forms.
- **Grouse and ptarmigan** that dove into snow to roost are sealed in by a glaze on fresh snow: 5% die, and a player finds them as small frozen birds under a pane of ice.
- **Voles** drown in rain-flooded tunnels under the snow: the vole index falls 40% that winter. Marten, fox and owls hunt harder: marten catches ×1.3 that winter, and owls come to the cabin's light.
- **Counterplay.** Ice creepers (iron tier, Danger in detail), crawling, re-setting traps the next morning, and a covered woodpile. The crust is also a fast road for a sled.

### Blizzards, ground blizzards and whiteout navigation

**Snowing blizzards keep every Blueprint rule:** 6–24 game hours, 20 m sight, drifting, tracks wiped, wind chill, half-range sound. They are now 40% of winter Severe days, with about 6–20 cm of fresh snow in midwinter.

**Ground blizzards are the other 60%.** No snow falls, but wind of 10–20 m/s lifts loose snow on open ground: lake ice, tundra, gravel bars, burns and ridges. The sky stays blue overhead.

| Ground blizzard | Wind 10–14 m/s | Wind 15–20 m/s |
| --- | --- | --- |
| Drift layer height | 2 m | 4 m |
| Sight with the eye inside the layer | 200 m | 50 m |
| Sight with the eye above it (on a drift, ridge or pressure ridge) | Full | Full |
| Tracks filled | 2 game hours | 30 game minutes |
| In forest | No effect beyond 30 m from an edge | Snow bombs from spruce |

**Navigation additions.** The compass, blazes and rope lines in Danger in detail stand. Four more aids work in a whiteout:

- **The trail by feel.** A packed trail under up to 40 cm of drift keeps full walking speed and plays the packed-snow step. Stepping off drops to deep-snow speed and plays `step.snow_soft`, so the player follows the trail by feet.
- **Sastrugi.** Wind-carved ridges on open snow lie along the season's prevailing wind. A Read on them gives a bearing to ±15°.
- **Climb above it.** In a ground blizzard, standing on anything 2–4 m high shows the landmarks again.
- **Sound beacons.** The falls and a running creek still carry at half range, as Danger in detail says.

**Counterplay.** Cross open ground early, before the afternoon wind. Plant trail wands (Land) on lake crossings. Never leave the trail in a ground blizzard.

**B9 Drifts bury fences (Proposed; Ruhan may veto).** From January to March, drifts bridge pens, garden fences and the rack fence, and animals walk over the top. The snow-fence rule already drops drifts behind a fence built for it; every fence now catches its own.

- **Where and when.** Any fence in the yard: the A3 rack fence, garden fences, goat pens and the wolf yard, after each storm over 10 m/s, most often in ground blizzards (above).
- **Effective height.** A fence's effective height is its height minus the snow depth on its higher side, drifts included.

| Animal | Crosses when the effective height is |
| --- | --- |
| Fox, hare, lynx, wolf | 0.6 m or less |
| Moose, caribou | 1.0 m or less (they step over) |
| Goats in a pen | 0.8 m or less (they get out) |

- **Fences catch drifts.** A brush or pole-and-bough fence gathers a 0.5 m lee drift after each storm over 10 m/s, up to its own height. This is the snow-fence rule (Structures and buildables › Snow, ice and trails) applied to every fence.
- **What the player can do.** Shovel the fence line: 10 game minutes per 4 m. Add a rail: 2 poles and 10 game minutes per 2 m. Build on the windward side of where the drift will lie.
- **What the player sees and hears.** The drift is visible against the fence, tracks cross on top, and the messenger reports what came over: "Drift over the rack fence. Fox tracks on top." It names the animal only from tracks the player could read (Tracks and sign).
- **Failure cases.** A fox at the drying rack (A3), goats loose in the bush at dusk, a moose browsing the garden's young willows. Three storms over 10 m/s bury a 1.5 m fence to its top.
- **Ties.** A3 Thieves at the rack (under its world option `[world] rack_thieves`), A2 Door left open, garden and pen fences (Structures and buildables), Raising animals (Ideas from viewers), snow cover passes (Minecraft development › Snow cover and permafrost blocks).
- **Build.** No new registry ids. The snow-cover pass gains a fence-lee drift for blocks tagged `#yearofice:fences` (`world.snow.FenceDrift`), and the animals' path cost reads effective height from the snow layer. Tuning `[world] drift_cross_small_m = 0.6`, `drift_cross_large_m = 1.0`, `drift_cross_goat_m = 0.8`, `fence_drift_m_per_storm = 0.5`. Debug `/yoi snow drift <pos>`. Milestone M8. GameTest `yearofice:world/drift_bridges_fence` (batch `m8`): a 1.2 m pole fence with 0.7 m of drift lets a fox path across and stops a hare's path at 0.5 m of drift; after shoveling, both are blocked.
- **Pillars.** The year changes the map (January to March changes the yard), Everything readable, Scarcity is the clock (the rack is at risk).

### Cold snaps, deep cold and ice fog

**The Blueprint's cold snap (3 or more days at −8 °C anomaly) and the hard winter stand.** This sets how often each band of deep cold is reached on the valley floor, and what the cold does to gear.

| Floor low reached in a winter | Chance in a normal winter | In a hard winter |
| --- | --- | --- |
| −35 °C | 95% | 100% |
| −40 °C | 70% | 100% |
| −45 °C | 30% | 80% |
| −50 °C | 4% | 40% |

- **Ice fog** forms at −35 °C and below by the Land tab's rule; the cabin's own fog bubble makes the camp hard to see from the bench.
- **Tools.** A cold steel edge swung into frozen hardwood below −35 °C chips 1% of swings (−10% of `yearofice:edge`) unless warmed by the stove for 10 game minutes. Frozen rounds split clean 1.5× as often below −25 °C.
- **Hard work** below −40 °C costs energy ×1.2, from breathing the cold.
- **Sleds.** Below −30 °C snow is like sand: sled drag ×1.5.
- **Trees and ice.** Frost cracks split trunks with a rifle-shot sound (`world.frost_crack`); lakes boom (Danger in detail). Ice grows 1 cm a day, as written.
- **Counterplay.** Stay in, stock wood, keep tools by the stove, and hunt big game at the spring seeps (Land animals in depth).

### Warm wind and rain on snow

**The warm wind stays as the Land tab writes it.** Rain on snow is the Blueprint's winter thaw, made into an event: a `pacific` Wet day with a mean above 0 °C, about once a winter, often as a warm wind ends.

- **Crust and ice layer.** 2–10 mm of rain freezes into the snowpack. The surface crust holds a walking player without snowshoes; the buried layer stays in the snow model until melt, with the starving effects in Freezing rain.
- **Overflow** doubles for 7 days, as written.
- **Thaw rot.** Frozen caches that pass 0 °C start their clocks (Decay and time). Each thaw-and-refreeze cycle adds 0.05 to a frozen item's freshness progress, and the tooltip names it: "Thawed and refrozen once."
- **Woodpiles** left uncovered under snow turn wet for 7 days: green-wood heat until dried.
- **Snow structures.** Quinzhees and snow caves follow their 2-days-above-0 °C rule (Structures and buildables › Snow, ice and trails); roofs steeper than 35° shed (Land).
- **Counterplay.** Covered woodpiles, caches on the north side under snow, and a check of every trap after the refreeze.

### Breakup storms and ice jams

**Breakup stages and the yearly jam stand as the Blueprint writes them.** A breakup storm is a warm rain of 10–25 mm or a warm wind during the Rotting stage, in 4 years of 10. It brings breakup 3–7 days early and doubles the chance that the jam lifts water to the flood-year line.

- **Ice shove.** Moving ice pushes up to 30 m onto low banks (`SHOVE` ops). It shears young trees and anything left on the bank.
- **Scar line.** Bark scars 1–3 m up on bank trees mark past shove heights. A Read names it: "Ice reached this high before."
- **Drowned moose.** In 15% of years a moose crossing rotten ice drowns and lodges at the jam. It is a scavenger magnet, read like the winter-killed moose.
- **Travel.** No river crossing for 3–10 days between ice and open water.
- **Counterplay.** Move gear above the scar line, pull tip-ups and nets before the 5-day mean passes 2 °C, as the April deadline says.

### Aurora nights

**The aurora index, its radio and compass effects and the aurora storm stay in Land and seasons in play and Open world in depth.** Weather only gates it: the aurora shows when cloud is 30% or less between 21:00 and 03:00.

**The cold comes with it.** Clear aurora nights in December to February average −30 °C on the floor, under an inversion. Watching for 10 game minutes at −35 °C without a ruffed hood starts frostnip on the face, so the best show in the valley is also a frostbite lesson.

### Heat and bug surges

**A heat spell is 3–6 days of `heat` air with highs of 27–33 °C, once or twice between June 10 and July 31.** The forecast gives it 2 days ahead.

- **Body.** Water drains ×1.2 above 20 °C, as written. Heavy work above 28 °C drains ×1.5 instead and doubles sweat wetness.
- **Food.** The cold curve gives f\_T 15.6 at 30 °C: fresh meat is safe for under half a day.
- **Fish.** Grayling and pike bite ×0.6 from 10:00 to 17:00 and ×1.3 after 21:00. Ponds under 0.5 m deep lose oxygen after 3 hot days and their fish die; ravens and bears come.
- **Animals.** Moose stand in water from 10:00 to 18:00 within 500 m of a lake, an easy stalk by canoe. Caribou climb to snow patches and windy ridges.
- **Plants.** Berries ripen 1 day earlier per hot day, up to 6; garden beds dry by their water state.
- **Fire and storms.** Fire danger climbs by its T\_max term. A thunder day follows within 3 days 60% of the time.
- **Bug surge.** The first warm, calm day after 2 or more Wet days between June 10 and July 25 starts a 3-day surge: the bug formula's date term is ×1.5.
- **Counterplay.** Work through the light night, keep meat in the creek or the cellar, burn smudges.

### Storm of the decade

**One rare event joins Open world in depth's seven.** It rolls on May 1 at 10% from year 2, with at most one in any 7 years. Its variant is drawn with weights 0.4, 0.3 and 0.3.

| Variant | Window | Numbers | What it does | What it leaves |
| --- | --- | --- | --- | --- |
| The Big Snow | November 15–March 15 | A 3-day snowing blizzard, 60–100 cm fresh, gusts to 30 m/s; then −45 °C for 7 days | Every unbraced roof over its load; lee drifts 3 m; moose yard up and 15% die by March | Winter-killed moose chance ×2 that March; doors dug out from inside |
| The Great Glaze | October 15–December 15 | 18–30 game hours of freezing rain, 20–30 mm of glaze | Limbs down across the valley; a crust under the snow until breakup; caribou leave the Interior for the winter | Firewood on the ground for years; a hard trapping winter |
| The Long Rain | July 10–August 31 | 4 days, 120–180 mm | River to the flood-year line in summer; slides on burn slopes and permafrost (`SUBSIDE`, `EXPOSE`); salmon blocked 5–10 days | New bars and cut banks; iron outcrops; a mushroom flush |

- **Signs.** The radio warns 72 game hours ahead: "Program says the biggest storm in years." The magnitude carries ±30% blur. The barometer reads under 970 hPa for the Big Snow and under 980 hPa for the Long Rain; animals go quiet a day before.
- **Legend.** `story.legend` records it by name, "The Big Snow of year 4". Pete Lund and Dana Reyes mention it in later years, and the journal keeps the line.
- **Hidden ambition.** "Weathered it": be in a shelter you built when it ends.

## Reading the sky

Every sign is an honest odds roll on the year table's real future, keyed by evening or hour, as the Land tab's sky signs are. Skill decides how many signs the player notices; the odds never change.

### Clouds

| Sign | When | Predicts | Lead | Odds |
| --- | --- | --- | --- | --- |
| Sun through frosted glass (thick grey high cloud) | After a halo | Rain or snow | 6–12 h | 70% |
| Towering cumulus by 11:00 on a hot, still morning | June–August | Thunder by 17:00 | 6 h | 70% |
| An anvil drifting | Under a cell | The cell's direction, ±30° | 30–90 min | 80% |
| Green-grey cell base | June–August | Hail within 30 min | 30 min | 50% |
| A low dark roll ahead of a cell | June–August | A gust front | 10 min | 85% |
| Low cloud racing from the southwest at 0 °C | October–April | Freezing rain | 12 h | 30% |
| Dark cloud underside over white ice | Winter | Open water below it | Now | 90% |

The ring, sun dogs, light pillars, smoke columns, cap cloud and mare's tails stay in Calendar, daylight and weather and Land and seasons in play.

### Wind shifts

| Shift | Predicts | Lead | Odds |
| --- | --- | --- | --- |
| Backing to south or southwest with the barometer falling | A Pacific front | 12–24 h | 70% |
| Veering to north or northeast with the barometer rising | Clearing and colder | 12 h | 75% |
| A windy winter day falling dead calm at dusk | A cold night under an inversion | 12 h | 80% |
| A warm, gusty north wind in winter | The warm wind is on | Now | 95% |
| Wind dropping, then reversing under a dark cell | A gust front | 10 min | 85% |
| Smoke smell on the wind | A fire upwind within 150 km | Now | 90% |

### Animals

| Sign | When | Predicts | Lead | Odds |
| --- | --- | --- | --- | --- |
| Grouse diving into snow to roost before dusk | Winter | Night below −30 °C | 12 h | 70% |
| Moose moving into spruce at midday | Any | Wind over 12 m/s | 12 h | 55% |
| Caribou bunching and heading for timber | Winter | Blizzard | 1 day | 60% |
| Loons calling through the light night | June–August | Rain | 24 h | 55% |
| Mosquitoes biting hard at midday in still air | June–July | Thunder | 12 h | 55% |

Swallows and ravens low, geese low and loud, and hard midday feeding stay in Land and seasons in play and Creative concepts.

### The barometer

**Two instruments, two levels of detail.** The Trail Messenger has a coarse sensor and shows one word: Rising, Steady or Falling. The aneroid barometer (Items) reads pressure to ±1 hPa and its 3-hour trend, and feeds the Forecast skill.

| Aneroid reading | Predicts | Lead | Odds |
| --- | --- | --- | --- |
| Falling more than 3 hPa in 3 h | Wind over 15 m/s or a Severe day | 12 h | 75% |
| Falling 1–3 hPa in 3 h | A Wet day | 24 h | 65% |
| Under 985 hPa and falling | A Severe day | 12 h | 85% |
| Steady above 1,020 hPa | Fair; in winter, colder | 24 h | 80% |
| Rising more than 3 hPa in 3 h | Clearing, colder, a north wind | 12 h | 75% |
| Above 1,040 hPa in winter | −40 °C or colder on the floor | 2 days | 60% |

### The radio forecast

**The messenger and Dana read the same program forecast.** The messenger receives it at 06:00 and 18:00 when it has battery and signal; Dana reads it at the 19:00 sked and adds days 3–5 at 70% (Open world in depth). Storm warnings carry these rates.

| Storm | Lead | Hit rate | False alarms | Wording |
| --- | --- | --- | --- | --- |
| Blizzard | 24–48 h | 85% | 10% | "Blizzard tomorrow. Visibility 20 m." |
| Windstorm | 12–36 h | 75% | 15% | "High wind tomorrow, gusts to {g} m/s." |
| Heavy rain | 24–48 h | 70%, amount ±40% | 15% | "Heavy rain, {a} mm, creeks rising." |
| Freezing rain | 12–24 h | 50% | 20% | "Rain or freezing rain tonight." |
| Cold snap | 3 days / 1 day | 60% / 80% | 10% | "Cold snap in 3 days." |
| Thunderstorms | 1 day | 80% of thunder days | 20% | "Thunderstorms possible this afternoon." Never a place or time |
| Hail | — | Never alone | — | "Some storms with hail." |
| Storm of the decade | 72 h | 100% | 0% | Magnitude ±30% |

### What the Trail Messenger may show

**The messenger shows only what its owner could plausibly know.**

1. **Forecasts are received data with a time:** "Program forecast, 18:02." With no battery or no signal (aurora index 7 or more, or 40% of the time at 5–6, by the Land tab's rule), it shows the last one with its age.
2. **Its own sensors are a thermometer and the coarse barometer.** Wind speed and gusts appear only as the forecast's numbers.
3. **It never says** where lightning will strike, where hail falls, which tree will fall, how thick glaze is on a roof, or what an animal will do.
4. **Storm distance** comes only from counted flashes, after 3 of them (Every concept from the video).
5. **Danger lines are worded from the senses.** A roof warning reads "Roof creaking under the load," which the player could hear.

## Shelter and preparation

A storm is a test of what the player built before it. Every number here extends the piece table and damage rules in Crafting and building in depth and Structures and buildables.

### What each shelter withstands

| Shelter | Rain | Wind | Hail | Snow and glaze | Blizzard | −40 °C | Lightning | Wildfire |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Fire and debris bed in the open | Fire drops by the rain rule | Low fire out in gusts over 15 m/s | None | — | Dig a trench or die | Fire only | Crouch rule | Run to gravel or water |
| Lean-to with reflector | Half blocked | Boughs stripped at 18 m/s gusts; down at 25 | Stops pea and marble | Roof sags over 30 cm | Wind chill halved; drifts fill it in 2 h | With a Steady fire | As forest | Burns |
| Tarp on a ridge line | Blocked | Tears at 18 m/s gusts; 25 when battened | Marble holes it 10% | Flat: collapses at 15 cm wet snow or 8 mm glaze; pitched sheds | Shakes; drifts in | No | As forest | Burns |
| Snow trench | — | Proof | — | Proof | Proof | +10 °C; candle +3 | Proof | — |
| Quinzhee or snow cave | — | Proof | — | Proof | Proof | By Structures and buildables | Proof | — |
| Repaired outpost cabin | Dry | Proof to 30 m/s | Proof | 60 cm unbraced | Proof | Needs the stove | Proof | Bark roof survives 20% |
| Chinked cabin with a stove | Dry | Proof | Proof | 60 cm, 120 braced | Proof | +25 to +35 °C on wood | Proof | Sod roof survives 70% |
| Root cellar or a rock-face base | Dry | Proof | Proof | Earth roof proof | Proof | Holds 2–4 °C | Proof | Survives 95% |

### Damage per piece

**Snow load gains two densities.** The Structures load formula stands, with ρ 1 for fresh snow and 2 after a thaw day or rain on snow. Wet snow is ρ 2.5, and each mm of glaze adds 1 cm of fresh-snow load.

```latex
load = \frac{s_{fresh} + 2\,s_{thawed} + 2.5\,s_{wet} + g_{mm}}{cap}
```

**Overload is fast.** Up to load 1.2 the existing sag of −5 HP a day stands. Above 1.2 a roof block loses 50 HP a game hour for each 0.2 over 1.0, so a sod roof under 30 cm of wet snow (load 1.25) falls in about 13 game hours.

| Piece | Snow cap (unbraced / braced) | Wind | Hail | Glaze | Fire and smoke |
| --- | --- | --- | --- | --- | --- |
| Pole-and-bark roof (200 HP) | 60 / 120 cm | −30 HP a game hour in gusts over 20 m/s unless battened | Large: −20 HP per 10 game min | Load as above | Flammability 1.5; lets 10% of storm rain in |
| Sod roof (800 HP) | 60 / 120 cm | Proof | Proof | Load as above | Flammability 0 |
| Thatch roof (300 HP) | 60 / 120 cm | −20 HP a game hour over 22 m/s | Marble: −5 HP per 10 game min | Load as above | Flammability 2.0 |
| Hide door flap (100 HP) | — | Torn 10% a game hour over 22 m/s unless tied | — | Stiff: 30 s to open | Leaks smoke out |
| Plank door (400 HP) | — | Proof | Proof | Jams 10% for 5 game min | — |
| Hide window (50 HP) | — | Torn 10% a game hour over 25 m/s | As the hail table | — | — |
| Drying line and racks | — | Items blow 2–6 m downwind over 12 m/s | Bruises fish: −10% freshness life | Items wet | Smoke rack smoke flattens in wind over 8 m/s |
| Woodpile | Snow wets it on a thaw | Cover blows off 20% a game hour over 20 m/s unless weighted with a stone | — | Wet | Spark target |
| Stovepipe or chimney | — | Top under 0.6 m above the ridge: smoke blows back into the room in wind over 8 m/s | — | — | A cold flue back-drafts for 10 game min when lit under an inversion |
| Open fire | Rain rule | Ember radius +1 per 5 m/s over 5; Low out in gusts over 15 m/s | Hail puts out a Low fire | Snow rule | Fuel ×1.5 in wind over 8 m/s unsheltered |
| Canoe on shore | Fills: bail 2 game min | Untied: flipped and moved 5–20 m over 20 m/s, 10% into the water | — | Iced: 1 game min to clear | — |

### Battening down

**One queued job does it all.** "Batten down" runs every battening job within 20 m, in the queue's order, at the work rate. Each piece keeps `battened=true` until the player unties it.

| Job | Time | Protects |
| --- | --- | --- |
| Lash a tarp with 4 extra cords | 5 game min | Tear threshold 18 → 25 m/s |
| Tie a hide door flap and window covers | 2 game min each | No wind tears |
| Weight the woodpile cover with a stone | 1 game min | No cover loss |
| Turn and tie the canoe | 3 game min | No flip |
| Take down the drying line | 2 game min | No blown items |
| Bank the fire behind the reflector | 2 game min | No blow-out; ember radius held |
| Pull nets and creek sets | By their tabs | No tears in floods and slush |
| Shovel the roof | 20 game min per 10 m² | Load back under 1 |
| String a rope line to the woodshed and cache | 1 m of cordage per metre | Whiteout travel at full speed |

### Caught outside

| Situation | Shelter | Build | Value | Watch for |
| --- | --- | --- | --- | --- |
| Blizzard, snow 60 cm or more | Snow trench | 1 game hour with a shovel (Danger in detail) | +10 °C, no wind | Wet hands double the time |
| Blizzard, under big spruce | Tree well | 10 game min clearing the pit at the trunk | +6 °C, no wind | Snow bombs in a warm wind |
| Deep cold, deep snow | Quinzhee or snow cave | By Land and seasons in play | By Structures and buildables | A vent, always |
| Rain or wind, summer | Lean-to | 1 game hour | Half the rain | Opening 90° off the wind, or smoke fills it |
| Wind, any fire | Fire pit 30 cm deep, or 2 logs as a windbreak | 5 game min | Wind effects halved | Dakota hole never sparks |
| Freezing rain | Dense spruce | — | Glaze ×0.3 | Keep tinder and bowstring under the coat |

## Presentation

Weather lives in the world, never on the glass. The only full-screen effects remain the body layers (frost, body\_fx), and the player and their hands always stay visible.

### The storm arc

| Phase | Length | Sky and light | Fog and sight | Sound | Messenger |
| --- | --- | --- | --- | --- | --- |
| Signs | 12–48 game hours | Halo, high streaks, a cap cloud | Full | Far sounds carry | Forecast line, if any |
| Approach | 1–6 game hours | Cloud deck lowers; light drops 20%; summer cells tower and darken | Haze to 2 km | Wind bed rises one step; first far thunder | — |
| Onset | 10–30 game min | Colour drains; a gust front | Closes to the state value over 2 real minutes | A gust roar, then the precipitation bed | One line: "Snow starting. Wind NE 12 m/s." |
| Peak | By storm | Darkest | State value | Full storm bed; danger cues still cut through | Nothing unless asked |
| Wane | 1–3 game hours | Breaks in the cloud | Opens | Bed falls step by step | — |
| Aftermath | 1–3 days | Clear, often colder | Full | Drips, ice tinkle, limb cracks, birds back | One line naming what changed: "Ice on the trees. Watch your footing." |

### What each storm looks like

| Storm | Sky | Particles | World marks |
| --- | --- | --- | --- |
| Thunderstorm | Towers, anvil, green-grey base; flash at most +60% lightmap for 3 ticks | Heavy rain, splashes | `struck_spruce`, smoke wisp from a holdover |
| Hail | Dark cell | Bouncing white stones | `hail_layer` 1–3 cm, melting |
| Windstorm | Fast low cloud | Leaves or snow streamers, dust on bars | Fallen trees with `root_plate` |
| Wet snow | Low white | Big flakes | Bent birches, `fallen_limb` |
| Freezing rain | Low grey | Fine rain; glints on trees | `glaze_layer`; clear-coated trunks by colour handler |
| Ground blizzard | Blue overhead | A 2–4 m layer of streaming snow | Sastrugi, filled tracks |

### Rules for the screen

- **No overlays for weather.** Rain, snow and hail are world particles only; nothing is drawn on the screen surface.
- **The player stays visible.** Particles are culled inside 1.5 m of the camera, and fog never closes nearer than 6 m, so a third-person view always shows the player. Hands and held items are never fogged.
- **Particle caps.** At most 1,200 weather particles alive, spawned within 24 m; ×0.5 at Decreased and ×0.25 at Minimal.
- **Motion.** Gusts over 15 m/s roll the camera ±0.5° and sway the held item; Reduce screen effects turns this off. The vanilla renderer has no leaf sway, so wind shows as streamers, falling leaves, snow bombs and sound; shader packs may add waving leaves.
- **Flashes** follow the accessibility rule: at most +60% lightmap for 3 ticks, 30% with Reduce screen effects, never more than 3 a second.

### Sound beds

**Weather sounds play on the `weather` bus over the ambience beds, and danger cues are never masked.** Each layer crossfades over 8 real seconds when its step changes. Wind has 4 steps (breeze, strong, gale, roar) on spruce, bare birch, open ground and ridges; rain has 2 steps on leaves, bark roof, sod roof, tarp and water. Hail, ice tinkle and limb cracks, the blizzard roar and ground-blizzard hiss, 15 thunder variants by distance, and 3 indoor storm beds by roof type complete the 51 events.

## Minecraft build

Vanilla weather stays off: `doWeatherCycle` is false and biomes have no precipitation, as the lead's game-rule set says. Our weather state lives in SavedData and reaches the client only through payloads.

### Classes and layers

| Class | Package | Layer | Does |
| --- | --- | --- | --- |
| `YearTableBuilder`, `HourlyModel`, `AirMass`, `StormPlacer` | `sim.weather` | Pure model | Draws the year table, places storms, computes hourly fields; no Minecraft classes |
| `Weather` | `story` | Clock and calendar (Layer 1) | Owns `yearofice_weather`, posts `WeatherChangedEvent` hourly, sets the vanilla rain level for sky darkening only |
| `Forecast` | `story` | Clock and calendar | The blurred forecast, storm warnings, the Video tab's card fields |
| `weather.StormDirector`, `weather.DecadeStorm` | `story` | Clock and calendar | Runs storm phases, posts `StormEvent`, submits history ops |
| `reading.SkyLore` | `story` | Clock and calendar | Sign rolls and Read lines, with `client.reading` |
| `wind.WindField`, `phenomena.LakeEffects` | `env` | Environment field (Layer 2) | Ridge, gap, drainage and canopy bending on the 32 m field; lake steam, flurries and breeze |
| `phenomena.Lightning` | `env` | Environment field | Strike targets per 32 × 32 cell by heightmap, player risk, ignition hand-off |
| `GlazePass` | `world.pass` | World edits (Layer 9) | Sets `glaze_layer` and `hail_layer` from the table, derived state |
| `SnowPass` | `world.pass` | World edits | As written; reads the snow model's ice layer for `crust` |
| `FloodController` | `world` | World edits | Summer rises to the normal high-water line; the Long Rain to the flood-year line |
| `Wildfire` | `fire` | — | `Wildfire.ignite(pos, cause)` receives lightning hand-offs |
| `weather.WeatherRenderer`, `weather.BoltRenderer`, `weather.WeatherParticles` | `client` | Client | Rain, snow, hail, ground blizzard, bolts |

**Moves.** The Video tab's `com.yearofice.story.Lightning` becomes `env.phenomena.Lightning`, matching the lead's map (`com.yearofice.weather.*` to `env.phenomena`) and World's `STRIKE` producer. It no longer spawns a vanilla `LightningBolt`: that entity plays its own thunder at a fixed range, so the bolt is drawn by `BoltRenderer` from our payload and thunder goes through `distant_sound` with the distance delay.

### State, sync and events

- **SavedData `yearofice_weather`.** Year, carried anomaly, decade draw and cooldown, the storm log (id, start, peak, end, for the journal) and live cell tracks for the current day. About 2 KB.
- **The year table** is memory only: 365 day records and 8,760 hour records of 16 bytes, about 150 KB, rebuilt from the seed.
- **EnvSample fields added** (lead approval needed): `cloud_pct`, `pressure_hpa`, `pressure_trend`, `humidity_pct`, `air_mass`, `glaze_mm`, `snow_ice_mm`, `smoke` (0–3), and `precip` values `drizzle`, `sleet`, `freezing_rain`, `hail`.
- **`env_sync`** grows from 40 to 48 bytes: gust, cloud, precipitation type, smoke, glaze and the barometer word.
- **New payload `yearofice:strike`.** S → players within 12 km, 14 bytes: position, bolt seed, power. The client draws the bolt; thunder follows as `distant_sound`.
- **New event `StormEvent.Start`, `.Peak`, `.End`** in `api.event` (lead approval needed), posted by `StormDirector`. Ecology reads it for herd moves and crust mortality; the journal and `story.legend` read `.End`.

### Blocks, ops and passes

| Id | Kind | Class | Notes | Milestone |
| --- | --- | --- | --- | --- |
| `glaze_layer` (`layers` 1–3, one per 5 mm) | Layer block on top faces | `world.ice.GlazeLayerBlock` | Slip rule by the bare-ice table; removed by `GlazePass` on melt | M7 |
| `hail_layer` (`layers` 1–2) | Layer block | `world.ice.HailLayerBlock` | Melts in 1–3 game hours | M7 |
| `fallen_limb` | Wood ground block | `world.logs.FallenLimbBlock` | Loot: half an armload of the species' wood | M7 |
| `battened` | Block state on tarp, hide flap and window covers; entity data on the canoe | Building's classes | Set by the Batten down job | M8 |
| `LIMB_BREAK` | New World edits op (world dev approval needed) | Producer `story.weather.StormDirector` | Removes 10–40% of a broadleaf crown's leaf blocks and places 1–3 `fallen_limb` below; allowed on the tree's own blocks and air under it | M7 |

**Existing ops used as written:** `WINDTHROW` (now scaled by gust), `STRIKE`, `BREACH`, `SHOVE`, `SUBSIDE`, `EXPOSE`, `BURN` through `fire.Wildfire`. All go through `WorldEditService.submit`.

**`snow_cover` is unchanged.** Crust is its `crust` state; the buried ice layer lives in `sim.env.SnowModel` per snow class, not in blocks.

### Renderer hooks

- **`YoiSkyEffects.renderSnowAndRain`** returns true and draws our rain, snow, sleet and slanting blizzard by `env_sync` wind; **`tickRain`** spawns splashes and drips.
- **`renderClouds`** draws two decks (high and low) by cloud cover, with tower and anvil billboards on cell days; **`renderSky`** tints for smoke.
- **`ViewportEvent.RenderFog`** sets fog from `visibility_m`. In a ground blizzard it uses the eye's height above the local snow: inside the layer, the layer's sight; above it, full.
- **`ComputeFogColor`** gives smoke its brown and the blizzard its white.
- **Particles** registered in `RegisterParticleProvidersEvent`: `rain_streak`, `snow_flake`, `sleet`, `hail_stone`, `blowing_snow`, `snow_bomb`, `ash_fleck`, `diamond_dust`.
- **Colour handlers** add a clear glaze sheen to leaves and logs when `glaze_mm` is 2 or more, with no block change.
- **Sounds.** 51 new events in the `weather` group, rendered or found under the art and sound rule (RECONCILE 116) with credits.

### Tuning and presets

**All weather dials live in `yearofice-common.toml` under `[weather]`.** A preset sets every dial; a dial set by hand marks it "Custom". GameTests run on Standard.

| Key | Gentle | Standard | Brutal | Unit |
| --- | --- | --- | --- | --- |
| `weather.storm_frequency` | 0.6 | 1.0 | 1.3 | × catalogue odds |
| `weather.storm_severity` | 0.8 | 1.0 | 1.2 | × peak wind, glaze and amounts |
| `weather.forecast_bonus` | 0.10 | 0 | −0.10 | Added to forecast hit rates |
| `weather.lightning_player_risk` | 0.5 | 1.0 | 1.0 | × strike chance on the player |
| `weather.blowdown_scale` | 0.5 | 1.0 | 1.5 | × windthrow and limb chances |
| `weather.whiteout_sight_m` | 40 | 20 | 15 | Snowing-blizzard sight |
| `weather.cold_snap_depth_c` | 0 | 0 | −3 | Added to cold-snap anomalies |
| `weather.decade_storm` | true | true | true | Allow the storm of the decade |
| `weather.crust_mortality` | 1.5 | 2.0 | 2.5 | × caribou and muskox winter deaths under a crust |

### Debug commands

**Every command is `/yoi weather …`, op level 2, and prints what it changed.**

| Command | Does |
| --- | --- |
| `/yoi weather show` | Today's day record, this hour's fields, air mass, front hour and active storms |
| `/yoi weather table <day> [hours]` | Prints a day or its 24 hours from the year table |
| `/yoi weather storm start <id> [hours]` | Starts a catalogue storm now, through `StormDirector` |
| `/yoi weather forecast [days]` | The blurred forecast and the true table side by side |
| `/yoi weather strike <pos>` | One strike at a position, with ignition rolls |
| `/yoi weather glaze <mm>` / `hail <layers>` | Sets glaze or hail cover in loaded chunks |
| `/yoi weather sim <years>` | Runs the JUnit-style balance sim and prints the snow, storm and cold-band summary |
| `/yoi weather decade <variant>` | Forces the storm of the decade |

### Tick cost

**Weather adds about 0.1 ms a tick on average.** The year table builds in 40 ms on the `yearofice-plan` pool on May 1 and on load. `StormDirector` costs 0.05 ms a game minute during storms, each strike 0.1 ms, and `GlazePass` and storm ops run inside the passes' and World ops' existing peaks, never in the same tick as a population step. The client's weather costs 0.4 ms a frame, 1 ms at the 1,200-particle cap.

### Milestones

| Milestone | Ships |
| --- | --- |
| M2 Walkable valley | `env_sync` with the new fields, the sky renderer's cloud decks, fog from `visibility_m` |
| M7 The year | `sim.weather`, `story.Weather`, `Forecast`, `StormDirector`, the climate table, air masses and fronts, local winds; storms except the storm of the decade; `glaze_layer`, `hail_layer`, `fallen_limb`, `LIMB_BREAK`, `GlazePass`; `Lightning` marks; sky reading and barometer odds; storm beds; `[weather]` toml and `/yoi weather`; GameTests 1–7 |
| M8 Building and outposts | Piece damage by wind, hail and glaze; the load formula; Batten down; GameTest 8 |
| M9 Homestead and beyond | Wildfire hand-off and smoke days; crust mortality in populations; Beyond weather cells; the storm of the decade |

### GameTests

1. **`weather/table_replay` (batch `m7`).** Year 3's table built twice, and built again after a save and reload on day 200, gives the same hash over all 8,760 hour records.
2. **`weather/snow_budget` (JUnit, `m7`).** Over 200 seeded winters on open ground, mean depth is 10–20 cm on October 31 and 35–45 cm on December 31, and the mean March peak is 55–70 cm.
3. **`weather/march_crust` (JUnit, `m7`).** Over 100 seeded years, the March crust forms on 3–7 nights on average between March 10 and April 15 on open ground; with the old ±3 °C swing the same seeds give about 1.
4. **`weather/glaze_slip` (`m7`).** A bot running for 1,000 seconds (20,000 ticks) on 5 mm `glaze_layer` slips 20–40 times; with ice creepers, never.
5. **`weather/windthrow_gust` (`m7`).** Over 10,000 tree rolls at 25 m/s gusts, sheltered trees fall at 0.7–1.2% and edge trees at 4.0–5.3%.
6. **`weather/lightning_mast` (`m7`).** In a template with a 20 m mast, a 15 m spruce and flat ground in one cell, 1,000 strikes hit the mast 95–100% of the time; the grounded radio is never damaged, the ungrounded one 45–55% of strikes.
7. **`weather/catchup_equals_live` (`m7`).** A chunk loaded through a freezing-rain day and a windstorm, and one loaded 30 days later, end with the same block hash and the same `edit_log` hash.
8. **`weather/wet_snow_roof` (`m8`).** 30 cm of wet snow on an unbraced sod roof reads load 1.25 and the roof falls in 12–14 game hours; braced, it reads 0.63 and loses no HP.

### Data files

**Weather data sits under `data/yearofice/weather/`, with snake\_case keys and unit suffixes as the validator requires.** Sky signs join the existing `sky_sign/` kind; the storm of the decade joins `event/`.

| Path | Holds |
| --- | --- |
| `weather/interior_alaska.json` | Normals, swings, bounds, the four Markov tables (as the Blueprint), the hourly curve, the daylight table, sky factors, precipitation scaling, air masses and the `ground` block |
| `weather/storm/<id>.json` | One per catalogue storm: windows, odds, trigger, numbers, effects, forecast rates, messenger lines |
| `weather/ring/<id>.json` | Interior district-type and Beyond province-kind offsets |
| `weather/forecast.json` | Lead times, hit rates and false alarms for the messenger, Dana and storm warnings |
| `sky_sign/<id>.json` | The new cloud, wind, animal and barometer signs |
| `event/decade_storm.json` | The rare event and its three variants |

**The climate file, in part.** It also fills the lead's request for the `ground` block (RECONCILE 82): soil temperature at 0.5 m by class, May first.

```json
{
  "normals": {
    "01": { "mean_c": -23, "swing_c": 3, "precip_mm": 13, "wind_ms": 2.0, "pressure_hpa": 1022, "bounds_c": [-52, 5] },
    "07": { "mean_c": 17, "swing_c": 5, "precip_mm": 55, "wind_ms": 2.8, "pressure_hpa": 1010, "bounds_c": [1, 34] }
  },
  "anomaly": { "keep": 0.7, "noise_c": 4, "winter_severe_kick_c": -6 },
  "sky_factor": { "clear": 1.4, "cloudy": 0.6, "wet": 0.5, "severe": 0.3 },
  "precip": { "severe_weight": 3.5, "snow_cm_per_mm": 1.5, "winter_severe_split": { "blizzard": 0.4, "ground_blizzard": 0.6 } },
  "ground": {
    "depth_m": 0.5,
    "monthly_c": {
      "open_soil":   [2, 7, 11, 10, 6, 1, -2, -4, -5, -5, -4, -1],
      "forest_duff": [0, 4, 7, 7, 5, 1, -1, -2, -3, -3, -3, -1],
      "permafrost":  [-1, 0, 2, 2, 1, 0, -2, -4, -6, -6, -5, -3],
      "bog":         [0, 5, 8, 8, 5, 1, -1, -2, -3, -3, -3, -1],
      "south_slope": [5, 10, 13, 12, 7, 1, -2, -4, -5, -5, -3, 1]
    },
    "snow_insulation": { "above_cm": 40, "per_cm_c": 0.05, "max_c": 2 }
  }
}
```

**A storm entry.**

```json
{
  "id": "freezing_rain",
  "windows": [["10-10", "12-10"], ["02-20", "04-15"]],
  "chance_per_year": 0.5,
  "trigger": { "air_mass": "pacific", "valley_air_c": [-6, 0], "needs_inversion": true },
  "duration_h": [2, 12],
  "glaze": { "accretion": 0.7, "canopy": 0.3, "melt_mm_per_h": 0.5, "sublime_mm_per_day": 0.2 },
  "limb_break": { "from_mm": 10, "per_mm": 5, "birch": 0.03, "aspen": 0.03, "spruce": 0.01 },
  "crust": { "ice_layer_mm": 5, "under_snow_cm": 20, "herd_move_km": [20, 60], "ungulate_death_mul": 2.0 },
  "forecast": { "lead_h": [12, 24], "hit": 0.5, "false_alarm": 0.2, "line": "messenger.yearofice.forecast.freezing_rain" },
  "after_line": "messenger.yearofice.weather.glaze_after"
}
```

## Changed numbers

Each change keeps the rule and fixes a number that the rule's own targets contradict, or adds a number where none existed.

| What | Was | Now | Why |
| --- | --- | --- | --- |
| Day–night swing in March–April and September–October | Summer ±5 °C, winter ±3 °C only | ±6 °C and ±4 °C | At ±3 °C (8.4 °C range on a clear day) the March crust's "day above 0 °C, night below −10 °C" fires about once a year; ±6 °C gives 3–7 nights, matching the Blueprint's "fastest walking of the year" |
| Snow and blizzard amounts | 2–8 cm a snow day, 10–25 cm a blizzard | Scaled per month to the precipitation normal at 1.5 cm per mm; about 1–2.5 cm a snow day and 6–20 cm a snowing blizzard in midwinter | The old amounts give about 350 cm of fresh snow and a 190 cm March peak against the Blueprint's own 55–70 cm target; scaled, the sim gives 17, 42 and 59 cm |
| Winter Severe days | All blizzards | 40% snowing blizzards, 60% ground blizzards (under 2 cm) | Keeps 20 m whiteouts inside the snow budget; most interior blizzards are wind over old snow |
| Rain and storm amounts | 2–8 mm, 10–30 mm | u · k\_m and 3.5 u · k\_m: in July about 2–6 mm a Wet day and 6–19 mm a Severe day | The old ranges overshoot July's 55 mm by about half |
| Sky factor on Wet and Severe days | Not set | 0.5 and 0.3 | Overcast precipitation damps the daily range more than cloud alone |
| Windthrow by gust | One rate for every gust of 15 m/s or more | × (g / 15)³ | A 30 m/s storm should fell far more than a 15 m/s one |
| Snow-load densities | ρ 1 fresh, 2 after a thaw | Adds ρ 2.5 wet snow and 1 cm per mm of glaze | Wet snow and glaze are the real roof-breakers |
| Roof overload | −5 HP a day over the cap | Kept to load 1.2; above it 50 HP a game hour per 0.2 over 1.0 | At −5 HP a day a sod roof would take 160 days to fall |
| Lightning on the player | −60 Condition, knocked out 1 game hour (Video tab) | −30 and a burn | The Body tab owns injuries and set the `lightning` source (RECONCILE 90) |
| Lightning drawing | Vanilla `LightningBolt`, visual only | Our `BoltRenderer` and `distant_sound` | The vanilla bolt plays its own thunder with no distance delay |
| Heavy work above 28 °C | Water ×1.2 above 20 °C | ×1.5, sweat wetness ×2 | Heat spells need a cost the player can plan around |
| Bug surge | Not set | Date term ×1.5 for 3 days | The first calm warm day after rain is the worst of the summer |

## Conflicts raised, all settled (RECONCILE 138–140, 145; final pass 2026-10-19)

1. **EnvSample, `env_sync` and `StormEvent`** gain fields and a new event: the lead must approve.
2. **New op `LIMB_BREAK` and blocks `glaze_layer`, `hail_layer`, `fallen_limb`:** the world developer must add them to the op and registry tables.
3. **Rare-event count.** Open world in depth's hidden ambition "Live through each of the seven rare events" becomes eight.
4. **Two sky-reading skills.** Land's Sky lore levels and Creative's L\_sky formula both cap at 80%. Proposed: one skill, Sky lore, with Creative's notebook page as its screen.
5. **Quinzhee values disagree:** Structures gives +25 °C (never above 0 °C) and candle +3; Land gives +18 °C and candle +4. This tab defers to Structures; the Director should rule.
6. **The weather JSON sample in Open world in depth** uses old state names and different summer odds; the Blueprint's four-state table wins, and the sample's keys become snake\_case.
7. **Day-length options.** World's creation table still lists 24, 36 and 48 minutes; RECONCILE 117 sets a 24–72 slider.
8. **Roof warning wording.** Structures' "Roof load 100%" should read "Roof creaking under the load," under the Trail Messenger's plausibility rule.
