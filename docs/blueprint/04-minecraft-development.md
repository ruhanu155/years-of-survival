# Minecraft development

The main developers' pass over the Minecraft build: how every concept in the blueprint is implemented in our NeoForge mod, how the systems interlock through shared layers, what vanilla still does that we replace, and how the game ships and grows as an evolving modpack. The Minecraft engine tab sets the conventions; this tab is the developers' working plan on top of it.

## Architecture and the interlace

Every system in the mod plugs into ten shared layers and reaches other systems only through them. This section defines each layer: owner package, data shape, storage, update rate, sync rule, read and write API, and the systems that use it. It then fixes the package map, the tick budget, threading, determinism, the event list, the save and sync matrix, the vanilla systems we replace, and the naming rules that settle the Director's reconcile list. The other four developer sections use these layer names exactly as written here.

### The rule of the interlace

**Layers, not calls.** A system reads another system's state only through a shared layer's service in `com.yearofice.api`. It never imports another system's package. An ArchUnit test (test scope only) fails the build on any import from one system package into another.

**One contract per layer.** Each layer has five parts, always in the same places.

| Part | Where | Rule |
| --- | --- | --- |
| Service interface | `com.yearofice.api.<layer>.<Name>Service` | Read methods are pure queries; write methods are requests the owner applies on the main thread |
| Implementation | The owner package | One class, bound at mod construction in `core.Services`; other code gets it from `YoiApi.<layer>()` |
| Model | `com.yearofice.sim.<layer>` | Every formula; no Minecraft imports; JUnit-tested |
| State | SavedData, attachment or data component | Codec for saving, stream codec for sync, codec defaults for every new field |
| Events | `com.yearofice.api.event` | Posted on `NeoForge.EVENT_BUS`, server side only |

**One writer.** Each piece of shared state has exactly one writing package. Anyone else asks through the service, for example `DecayService.rehome(...)` or `WorldEditService.submit(...)`. The owner validates the request, applies it and posts the event.

**The client never decides.** The client holds read-only mirrors that payloads fill. It renders, plays sound and sends inputs, as the engine tab requires.

### Package and module map

**Top-level packages.** These are the only top-level packages under `com.yearofice`. A new one needs the lead engineer's entry in this table.

| Package | Owner | Holds | Shared layer it owns |
| --- | --- | --- | --- |
| `core` | Studio lead | Mod entry `YearOfIce`, `core.tick.ServerScheduler`, budgets, `core.rules` (game rules, presets, difficulty lock), `core.vanilla` (suppression), `core.hash` (`Mulberry32`, `StageHash`), `core.seed` | — |
| `api` | Studio lead | Service interfaces, event classes, shared records (`EnvSample`, `HudLine`, `ChangeOp`) | The contract of all ten |
| `net` | Studio lead | Payload registration, `SyncFlusher` | — |
| `item` | Studio lead | Data components, item data maps, base item classes, `QualityRoller`, loot functions | Item model |
| `data` | Build dev | Reload listeners, codecs, the validator, `data.expr` | — |
| `sim` | Each owner, by subpackage | Pure Java rules | The model of every layer |
| `worldgen` | World generation | Stages 1–7, chunk generator, biome source, tree builders, set pieces | — |
| `world` | World generation | Runtime world: change queue, block passes, catch-up, floods, drift, forest stands, the placed mask | World edits |
| `env` | World and story | Environment field, heat index, light field, water bodies, micro-climate, phenomena | Environment field |
| `story` | World and story | Clock, calendar, sun, moon, weather, forecast, events, ambitions, radio, journal, onboarding, prompts, notes, legends, study, place names, fur trade | Clock and calendar |
| `ecosystem` | Ecosystem | Species entities, Brain parts, senses, tiers, populations, spawning, encounters, food web, wounds and hit zones, carcasses, living fish, birds, water life, livestock entities | Scent and sound field; Ecology |
| `hunt` | Food and hunting | Bows, arrows, sling, projectiles, calls, blinds, camouflage, butchering, fishing gear and the fight | — |
| `food` | Food and hunting | Eating, stomach, cooking, preservation, traps (`food.traps`), garden, caches, livestock care | — |
| `decay` | Food and hunting | Decay clocks, freshness, block ageing | Decay and freshness |
| `forage` | Body and survival | Plant blocks, harvest verbs, berry bushes, herbarium | — |
| `body` | Body and survival | Body attachment, ticker, injuries, illness, death and SOS | Body |
| `fire` | Body and survival | Fire block entities, spread, smoke, light items | — |
| `build` | Building and crafting | Pieces, frames, support, rooms and the heat model, stations, recipes, multiblocks, snow structures, devices | — |
| `travel` | Building and crafting | Sled, canoe, raft, snowshoes, drags | — |
| `hud` | Look, sound and UI | Line types, `HudBus`, prompt offers (common code) | Messenger and HUD bus |
| `sound` | Look, sound and UI | Sound event registry, `SoundBus`, propagation (common code) | Sound events and the audio bus |
| `client` | Look, sound and UI; `client.horizon` World generation | Everything drawn or heard | — |
| `gametest` | Testers | GameTests | — |

**Packages written elsewhere, normalised.** Tabs used other package names; these mappings are final (reconcile 16 and 32). Class names stay; only the package changes.

| Written in a tab | Becomes |
| --- | --- |
| `com.yearofice.entity.animal.*` (Bestiary) | `ecosystem.species.*`; base class `YoiAnimal` there |
| `com.yearofice.animal.*` (Video) | `ecosystem.species.beaver.DamWork`; `ecosystem.foodweb.WinterDead` |
| `com.yearofice.carcass.*` (Bestiary) | `ecosystem.carcass.*`; `Dressing` moves to `hunt.butcher` |
| `com.yearofice.fish.*` (Waters, Water life, Viewers) | `ecosystem.fish.*`; `HookedFishController` moves to `hunt.fishing` |
| `com.yearofice.fishing.*`, `com.yearofice.hunting.Traps` (Video) | `hunt.fishing.*`; `food.traps.*` |
| `com.yearofice.homestead.*` | `food.livestock.*`; the animals stay in `ecosystem.species` |
| `com.yearofice.land.*` (Land and seasons) | `MicroClimate`, `GroundState`, `PhenomenaDirector` → `env`; `IceWorks`, `SlopeHazards`, `ForestStand` → `world`; `SnowWorks` → `build.snow`; `ColdStore` → `food.preserve`; `ControlledBurn` → `fire`; `FungiFlush` → `forage`; `LandReading`, `SkyLore` → `story.reading` with `client.reading` |
| `com.yearofice.weather.*`, `com.yearofice.water.*` | `env.phenomena.*`; `env.water.*` (water bodies, oxygen) |
| `com.yearofice.ui.*` | `client.ui.*`; server checks such as `HoldGuards` move to `build.work` |
| `com.yearofice.rules.*` | `core.rules.*` |
| `com.yearofice.light.*` | `fire.light.*` for light items; the light field is `env.light` |
| `com.yearofice.legend`, `ritual`, `pledge`, `study`, `naming`, `collection`, `trade`, `ice` | `story.legend`, `story.ritual`, `story.pledge`, `story.study`, `story.naming`, `story.collection`, `story.trade`, `story.BreakupBoard` |
| `com.yearofice.camera.*` | `build.device.TrailCameraBlockEntity`; `FootageScorer` → `story.study` |
| `com.yearofice.seed`, `craft`, `coop`, `test` | `core.seed`; `item.QualityRoller`; `story.coop` (after 1.0, RECONCILE 115); `gametest` |
| `com.yearofice.sim.TerrainChangeQueue` and friends (Water calls) | Planners stay in `sim.terrain` (`HydroPlanner`, `ChangeOp`); the SavedData and appliers move to `world.change`, because SavedData is a Minecraft class |
| `story.Snow`, `story.Ice` (engine M7 list) | Models `sim.env.SnowModel`, `IceModel`; block passes `world.pass.SnowPass`, `IcePass`. Story owns time and events; block writes belong to the world layer so they share catch-up and the placed mask |

### The layer stack

**Four bands, one direction.** Gameplay systems call down into the shared layers; the layers rest on the base. Nothing calls up, and the client sits outside the server and reads payloads only.

&#91;embedded content: the layer stack · client, systems, ten shared layers, base\]

The shared band is the interlace: the eleven gameplay systems above it never call each other, and the client reads only what the server sends.

**Which layer reads which.** The reads form no cycle: no layer reads a layer that reads it back. Writes go only through the owner's service.

| Layer | Reads | Read by |
| --- | --- | --- |
| 1 Clock and calendar | — | Every system |
| 2 Environment field | Clock | Body, fire, decay, ecosystem, forage, garden and traps, rooms, world passes, fishing; the client through `env_sync` |
| 3 Scent and sound field | Clock, Environment (wind, rain, light, visibility) | Ecosystem sensors, calls, camouflage, caches, cleaning spots, smokers |
| 4 Ecology | Clock, Environment, Scent and sound, Body (the encounter guard), Decay (carcass stages) | Ecosystem, hunting, trap rolls, story events and the journal |
| 5 Decay and freshness | Clock, Environment | Food, butchering, carcasses, forage, piece ageing, bone piles and deadwood, tooltips |
| 6 Item model | Decay, Clock | Every system that holds items |
| 7 Body | Clock, Environment, Item, Decay | HUD, encounters, journal, work rates, the bow strength cap |
| 8 Messenger and HUD bus | Clock (close-clock filter), Item (battery) | Every server system that tells the player something |
| 9 World edits | Clock, Environment (snow, ice, flood state) | Hydrology, beavers, wildfire, regrowth, forest stands, story events |
| 10 Sound events and the audio bus | Scent and sound (noise from sound), Environment (propagation) | Every system that makes a sound |

### Layer 1: Clock and calendar

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| World and story | `story.clock`; model `sim.time` | SavedData `yearofice_clock` | Every tick | `yearofice:clock_sync` every 20 ticks and on rate change |

**Integer game time.** The clock stores `game_ds`, game deciseconds since 00:00 on May 1 of year 1, as a long. Every rate is defined per game minute, as a whole number of deciseconds per tick at the 24-minute day. Day length L is a world-creation option, 24–72 real minutes, default 24, stored in `yearofice_world` (RECONCILE 117); only the World rate scales, at 720 ÷ L deciseconds a tick with L one of 24, 30, 36, 40, 45, 48, 60 or 72, so the division is exact, while Travel 6, Close 3, Work 150, Until done 600 and Sleep 1,800 ds a tick stay fixed (RECONCILE 145), so no rounding ever drifts and two machines agree to the decisecond.

| Rate | Game ds per tick | Game time per real second | Chosen when |
| --- | --- | --- | --- |
| Close | 3 | 6 s | Minigame, drawn bow, alerted predator within 160 blocks, bleeding, in water, soaked below 0 °C |
| Travel | 6 | 12 s | Moving with no job |
| World | 30 | 1 min | Default |
| Work | 150 | 5 min | A hold-to-work ring is filling |
| Until done | 600 | 20 min | A queued job runs (Z) |
| Sleep and wait | 1,800 | 60 min | Sleeping or waiting by the fire |

**Choosing the rate.** Each tick, systems call `TimeService.requestRate(player, rate, reason)`. The `RateArbiter` picks per player in this order: close, then work, then until done, then sleep, then travel, then world. The rates stay data in `data/yearofice/clock/rates.json`, so the day-length option (RECONCILE 117) rescales real time without any code change.

**Calendar.** `sim.time.Calendar` gives year, day of year 0–364 (May 1 is day 0), month and day; there are no leap years. Hour and minute come from `game_ds`.

**Sun and moon.** `sim.time.Sun` reads elevation and azimuth from the 365-row daylight table and interpolates by minute. `sim.time.Moon` gives the phase on a 29.53-day cycle from the offset `hash(seed, "moon")`, and rise and height from its table. Runtime code never computes trig for the sky.

**Dawn and dusk.** Dawn is the minute the sun climbs through −6°, dusk the minute it sinks through −6°. In the midnight-sun weeks, when the sun never sinks that far, dawn fires 120 minutes after the day's lowest sun and dusk 120 minutes before it. Traps, birds and the chorus key off these two events only.

**Clock events.** `ClockEvent.Minute` fires at most once a tick, carrying the first minute and the count crossed. `Hour`, `Day` (at 00:00), `Dawn` and `Dusk` follow. A step of more than 60 game minutes in one tick (the SOS flight, an admin time set) posts `ClockEvent.Jump(fromDs, toDs)` instead, and every listener runs its closed form.

**Driving vanilla.** The clock writes `level.setDayTime` each tick from the sun elevation, as the engine tab sets. Vanilla's own day counter and moon phase are never read.

**API.** `TimeService`: `nowDs()`, `nowMinute()`, `minuteOfDay()`, `dayOfYear()`, `year()`, `rate()`, `sun()`, `moon()`, `daylightHours(day)`, `minutesSince(stamp)`, `requestRate(player, rate, reason)`. Users: every system.

**Multiplayer rule.** One clock per world, and every rate is requested per player, so the rules stay co-op-safe (RECONCILE 115: solo first, co-op later). A player's time-lapse and close clock are that player's own requests; in single-player the world follows them. On a server the world rate is the slowest any player requests, sleep skips only when every player sleeps, and the SOS jump is a per-player closed form (Player systems and client › Collapse, SOS and the flight).

**Future co-op design, not built in M0–M9 (reconcile 9, RECONCILE 115).** Recorded for co-op after 1.0: the world clock stays shared at the slowest rate. A player whose job wants a faster rate runs it on a personal time debt: the job advances at the work rate and its game minutes are charged to that player's body (kcal, fatigue, thirst), while fires, spoilage, weather and animals keep the shared clock. The debt is capped at 8 game hours, shows as "+1:20" on that player's clock, and blocks sleeping until world time catches up.

**Cost and tests.** Under 0.05 ms a tick. GameTests: `clock/rates_exact` (one real minute at each rate adds exactly 60 × 20 × the rate in ds, and, for the World rate only, exactly a third of that on a 72-minute day), `clock/dawn_fallback_june` (June 21 gets one Dawn and one Dusk), `clock/multiplayer_slowest` (a walker and a worker give the travel rate), `clock/jump_matches_steps` (a 14-day jump and 14 days of steps leave the same body, fires and food within 0.5%).

### Layer 2: Environment field

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| World and story | `env`; model `sim.env` | Mostly computed; chunk attachment `yearofice:microclimate`; SavedData `yearofice_ice`, `yearofice_water` | Hourly epoch, lazy per section | `yearofice:env_sync` each game minute |

**One question, one answer.** "How cold, wet, windy, snowy, icy and bright is it here?" has one answer, `EnvService.sample(level, pos)`. Body, fire, decay, animals, plants and the HUD all ask it and never compute their own.

**The sample.** `EnvSample` is a record of these fields.

| Field | Unit | Source |
| --- | --- | --- |
| `air_c` | °C | Weather's valley air for the hour, lapse and inversion as written, micro-climate offset (Land and seasons in play › Micro-climates); the room temperature when the position is in a room |
| `feels_like_c` | °C | Air with wind chill at 1.5 m, before clothing |
| `ground_c` | °C at 0.5 m | Ground normals by class and month (new `ground` block in `weather/interior_alaska.json`) |
| `water_c` | °C | Per water body and day, from `env.water` |
| `moisture` | 0–100 | Rain of the last 3 days, snowmelt, mud season and drainage class, per 32 m cell per day |
| `wind_dir_deg`, `wind_ms`, `gust_ms` | °, m/s | The hourly wind bent by terrain and cover on the 32 m field; canopy ×0.3 |
| `snow_cm`, `crust` | cm, bool | The snow model by column class; the block itself only where a player dug or packed it |
| `ice_cm` | cm | The water body's thickness × thin-spot flag (current attachment) × 0.5 when rotten |
| `precip`, `precip_mm_h` | state, mm/h | Weather state and rate; types rain, drizzle, snow, sleet, freezing\_rain and hail (Weather and storms, RECONCILE 138) |
| `visibility_m` | m | Fog, snowfall, blizzard (20 m), ice fog, mist |
| `light` | 0–15 | Gameplay light (below) |
| `heat_bonus_c` | °C | The heat index: fires up to +25 °C within 3 m, fading to 0 at 8 m |
| `fire_danger` | 0–4 | Low to Extreme, from moisture, wind and days since rain |
| `room_id` | int | 0 when outside any room |
| `cloud_pct` | % | Weather's cloud cover for the hour (Weather and storms) |
| `pressure_hpa`, `pressure_trend` | hPa; falling, steady, rising | The storm model's pressure and its 3-hour trend, read by the forecast and animal behaviour |
| `humidity_pct` | % | Relative humidity of the hour's air mass |
| `air_mass` | enum | The hour's air mass (Weather and storms) |
| `glaze_mm`, `snow_ice_mm` | mm | Ice on surfaces from freezing rain, and packed snow-ice, per column |
| `smoke` | 0–3 | Wildfire smoke level at the position |

**Cached per chunk section.** `env.SectionCache` holds, for each loaded section, the hour's base air at the section's middle height, its wind vector, precipitation, visibility and sky light, about 48 bytes. A sample adds the column's micro-climate offset from the chunk attachment, then the heat index and room. A section recomputes lazily on first sample after the env epoch moves.

**What moves the epoch.** `ClockEvent.Hour`, `WeatherChangedEvent`, a snow or ice pass day, and `RoomChangedEvent` (rooms only). Nothing else invalidates the cache.

**Unloaded positions.** `sampleAbstract(regionKey, x, y, z)` computes the same fields from generator layers and the year table with no chunk, except block light, which is 0. Decay's air clocks, ecology and chunk catch-up use it. A GameTest holds loaded and abstract samples within 0.05 °C.

**Gameplay light.** `env.light.LightField` gives light 0–15 as the largest of three values: sky light from sun, moon, aurora and cloud (the engine's light table: full moon on snow 40 m sight, moonless 8 m, aurora +10 m); vanilla block light at the position; and held lights. Held lights come from the data map `yearofice:light` (level, cone degrees, battery use) for the item in either hand or the headlamp slot, refreshed per player every 10 ticks. Sight sensors, eyeshine and the light cone read this value, never the renderer.

**Heat sources.** Fires, stoves and hot springs call `EnvService.registerHeat(pos, radius_m, bonus_c, kind)` when lit and remove themselves when out. The index lives in memory and is rebuilt on load from the block entities.

**API.** `EnvService`: `sample`, `airC`, `feelsLikeC`, `groundC(pos, depth_m)`, `waterC`, `moisture`, `wind`, `snowCm`, `iceCm`, `light`, `visibilityM`, `fireDanger`, `heatBonusC`, `sampleAbstract`, `registerHeat`, `removeHeat`. Writers: `story.Weather` sets the hour's state; `env.water` sets ice and water temperature daily; worldgen writes the micro-climate attachment once.

**Sync.** `env_sync` carries the player's own sample (air, feels-like, wind, state, snow here, visibility, season stage), plus the forecast block (cloud, precipitation, snow for 3 leads) only when it changes. About 48 bytes a game minute, with the storm and humidity fields (RECONCILE 138).

**Cost and tests.** 0.5 ms a tick on average; a cached sample under 1 µs, a section rebuild under 20 µs. GameTests: `env/loaded_equals_abstract`, `env/cache_rebuilds_once_an_hour` (10,000 samples in one hour trigger one rebuild per section), `env/room_overrides_air`, `env/cold_sink_night` (−3 to −6 °C below the floor on a clear calm night), `env/held_lantern` (light 12 at the holder, 0 change to vanilla block light).

### Layer 3: Scent and sound field

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| Ecosystem | `ecosystem.senses`; model `sim.senses` | SavedData `yearofice_scent`; noise ring buffer in memory | Scent hourly; noise per event | None (a dev overlay only) |

**Scent points.** A point is `{id, pos, kind, s0, created_ds, half_life_h, source}` with strength 0–3, the Items scale. Points sit on a 16 m spatial hash per level. Kinds and defaults are data in `data/yearofice/scent/<kind>.json`: `half_life_h`, `rain_factor`, `max_range_m` and `bears_path` (bears path to strong points within 400 m).

```latex
s(t) = s_0 \cdot 2^{-\Delta t / h_{1/2}} \qquad h_{1/2,\,rain} = 0.5\,h_{1/2} \qquad h_{1/2,\,ice\ fog} = 48\ \text{h}
```

**Live sources.** A carcass, cache, smoker, latrine, gut pile or food container owns its point and refreshes `s0` each game hour from its own state: item scent levels times amount, halved in a closed container (Items › The eight properties). A point whose source is gone decays by the formula and is pruned below 0.05.

**The player's scent.** The body tick computes the player's strength each game minute from grime and exertion (`yearofice:body`), garment scent (`yearofice:wear`), the scent of carried items and blood on the hands. `ScentService.playerStrength(player)` returns the cached value.

**Cones, not stored.** Any emitter casts a cone downwind. Its half-angle and length come from `data/yearofice/senses/scent.json`: wide and short in light wind, narrow and long in steady wind, and a round pool under 0.5 m/s. Length scales with the species' smell range and the emitter's strength. A query `smells(level, pos, senses)` walks the hash cells within the longest cone and returns `ScentHit(source, strength, bearing)` in under 50 µs.

**Noise events.** `NoiseService.emit(level, pos, radius_m, kind, source)` sends one transient event. Footsteps are batched: one event per player every 10 ticks, its radius by surface, gait, load and the snow-temperature factor. Action noise comes from `noise_m` in action data.

**Propagation.** One function, `sim.sound.Propagation.effectiveRadius`, scales every noise and every sound alike, so the player hears as far as the animals do. Its factors live in `data/yearofice/senses/propagation.json`: downwind and upwind, rain, snowfall, mist ×1.5, air below −30 °C ×1.3, and a ridge on the 32 m line between source and listener halving the radius.

**Delivery to animals.** Loaded animals: events go into a per-level ring buffer of the last 100 ticks, indexed by chunk, and each `HearingSensor` reads it every 10 ticks. Nearby-tier records: the service rolls each record inside the effective radius once per event and shifts its virtual state (alert, flee). Calls use the same radius through `hunt.CallBroadcaster`, which adds its own response rolls (Water, calls and encounters › In Minecraft: calls).

**Sight is not a field.** Sensors raycast against blocks, leaves as half cover, with `EnvService.light`, `visibilityM` and `hunt.Camouflage.sightMultiplier`.

**API.** `ScentService`: `addPoint`, `upsertSource(key, pos, s0)`, `removeSource`, `smells`, `strongestNear(pos, radius, kind)`, `playerStrength`. `NoiseService`: `emit`, `heardBy(entity)`. Users: every sensor, calls, caches, cleaning spots, smokers, carcasses, bait, the body.

**Cost and tests.** 0.3 ms a tick, sensor queries billed to the animal budget. GameTests: `senses/cone_downwind` (a hare 50 m downwind in 3 m/s smells the player, one 50 m upwind does not), `senses/calm_pool`, `senses/rain_halves_life`, `senses/noise_reach` (an axe noise of 300 m reaches a moose at 280 m in calm air, not at 320 m, and not at 160 m in rain), `senses/carcass_point_follows_stage`.

### Layer 4: Ecology

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| Ecosystem | `ecosystem.population`, `.tier`, `.spawn`, `.encounter`, `.foodweb`; model `sim.ecology` | SavedData `yearofice_populations`, `yearofice_region_<key>`, `yearofice_encounters` | Active every tick; Nearby each game minute; Abstract each game hour; populations daily | Entities only |

**The records are the animals.** Records are tiered (DIRECTOR RULING 91). Big game, predators, named and Known animals and legends keep a full record for life in their region file: id, species, sex, age class, home slot, position, state, wound list, name and legend flag. Small game, small birds and fish keep a 24-byte slot record only while their chunk is within 600 m of a player; elsewhere their count is the truth (Entities and AI › The animal record and what is saved). A region key is a home cell A1–D4 or an Interior district id (World map tab). An entity is only the record's picture while the animal is near a player.

**Entity link.** An Active or Nearby entity carries the attachment `yearofice:individual` (region key, record id). Wild animal entities are never saved with chunks (`shouldBeSaved` returns false). The tier manager writes the entity's state back to its record on demotion, each game hour and on level save, so a crash can lose at most an hour of one animal's wandering, never duplicate it.

**What is saved as an entity.** Carcasses, the companion wolf, livestock (hens, goats, reindeer), the sled team in harness, and boats are ordinary saved entities. They are not drawn from populations.

**Tiers.** As the engine sets: Active under 150 m with the full Brain, Nearby 150–600 m with AI off or as records, Abstract as counts. Promotion at 150 m, demotion at 180 m, caps 40 Active and 200 Nearby, farthest first. Nearby records move along trails in slices of 1/20 per tick, so each moves once a game minute.

**Population step.** `sim.ecology.PopulationModel.daily(region, day)` runs births on the species' calendar dates, deaths, migration and harvest, then the food-web hooks. Indices (hare cycle H, vole V, lemming L) step on May 1 from `data/yearofice/index/<id>.json`. A whole year of unloaded regions runs on a worker thread from an immutable snapshot and lands on the main thread.

**Food-web hooks.** Each hook implements `FoodWebHook.daily(RegionState, day)` and is registered in `ecosystem.foodweb.FoodWeb`.

| Hook | Reads | Writes |
| --- | --- | --- |
| Predation | Predator and prey counts, diets in species files, snow depth | Counts; found-kill records that Decay starts |
| Scavenging | Carcass records and stages | Damage maps, feeder visits (Bestiary › Scavengers and the order of eating) |
| Browse and berries | Moose and bear counts, plant patch totals | Patch health, read by forage blocks |
| Fish stocks | Spawning, hatches, ice | Stock per water body, read by fishing |
| Indices | H, V, L | Lynx, fox, owl and raptor breeding |
| Harvest | Player takes reported through the service | Counts, so trap rolls read what is really left |

**Harvest closes the loop.** Every take calls `EcologyService.harvest(regionKey, species or patch, amount)`: a trap catch, a kill, a landed fish, a kilogram of berries. Nothing in the game creates an animal outside the population model.

**Encounters.** `EncounterDirector` (Water, calls and encounters › In Minecraft: encounters) is per player in `yearofice_encounters`. It reads `BodyService.isSafeToStage` (Condition and warmth over 30, past the first 7 nights, not in water), the clock and the Nearby records, and asks `TierManager.materialize(record, pos, role)`.

**API.** `EcologyService`: `count(regionKey, species)`, `index(id)`, `records(pos, radius, filter)`, `harvest`, `reportDeath(record, cause)`, `materialize`, `stageRequest`, `patchHealth(patchId)`. Events: `PopulationEvent.Birth`, `.Death`, `PredationEvent`, `IndexChangedEvent`, `EncounterEvent.Staged`, `.Revealed`, `.Ended`, `AnimalKilledEvent`.

**Cost and tests.** Tiers, Nearby, Abstract and daily steps 2 ms a tick on average; Active animals 8 ms (below). GameTests: `ecology/record_roundtrip` (100 promotions and demotions keep the id and the position within 1 block), `ecology/crash_no_duplicate` (a server killed mid-hour restarts with the same count), `ecology/harvest_lowers_count`, `ecology/no_wild_entity_in_chunk_nbt`, `ecology/encounter_guard`.

### Layer 5: Decay and freshness

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| Food and hunting | `decay`; model `sim.decay` | Component `yearofice:fresh`; SavedData `yearofice_decay`; chunk attachment `yearofice:decay_marks`; the carcass record | Clock heads hourly; items settle on read | `yearofice:decay_heads` hourly and on menu open |

**One clock for everything that rots.** Food, bait, hides, carcasses, meat on racks, bark roofs, sod, log pieces in soil, bone piles and deadwood all age on one kind of clock. A stack, a block and a carcass in the same place at the same temperature age the same.

**A decay clock.** A clock is a named temperature history with three running integrals. D counts decay-hours, one hour at 5 °C being 1.0. F counts frost degree-minutes, signed. W counts drying hours, open and sheltered.

```latex
D_c(t) = \sum_{h} r\big(T_c(h)\big) \qquad r(T) = \frac{L(5\,^{\circ}\mathrm{C})}{L(T)} \qquad r(20) = 5.0,\ r(10) = 1.71,\ r(0) = 0.58,\ r(-3) = 0.2,\ r(\le -5) = 0
```

L(T) is the Spoilage model's table (the Bestiary's copy: 1.2 days at 20 °C to 10.4 at 0 °C). Other curves (hides, berries, soft tissue to bones, wood in soil) live in `data/yearofice/decay/<curve>.json` as temperature-to-rate points.

**Clock keys.** Every holder resolves to one key.

| Key | Temperature | Stored |
| --- | --- | --- |
| `air:<region>:<band>:<class>` | `sampleAbstract` at the region, 100 m elevation band and micro-climate class | No: computed from the year table, cumulative per day cached at load |
| `room:<id>` | The room model's hourly temperature | Head in `yearofice_decay` |
| `body:<player>`, `pocket:<player>` | Air at the player; the inner pocket at +20 °C while warmth is 30 or more | Head in `yearofice_decay` |
| `water:<body>`, `snow:<region>:<band>` | Water body temperature; under-snow ground | No: computed |
| `fixed:cellar`, `fixed:ice_cave`, `fixed:ice_house` | 3 °C, −2 °C, 1 °C (Structures and Decay and time, RECONCILE 137) | No |

**Freshness on a stack.** `yearofice:fresh` holds `state` (raw, cooked, smoked, dried), `clock` (an int handle into the save's key table), `due_dh` and `life_dh`. Life is `spoil_days_at_5c × 24` from the food data map, times any k (the unopened carcass's 0.6, an egg's rate). Frozen is not a state: it is `yearofice:item_temp`, so a thawed fish spoils again.

```latex
p = 1 - \frac{due - D_c(now)}{life} \qquad due' = D_{new}(now) + \big(due - D_{old}(now)\big) \qquad due_q = \left\lfloor \frac{due}{life/40} \right\rfloor \cdot \frac{life}{40}
```

**Bands make stacks merge.** `due` is floored to a grid of life/40 on the clock's own axis, so two stacks that reach the same band on the same clock are equal and merge in vanilla code. Flooring takes the worse value, as Items asks, and a move between holders ages food by at most one band (2.5 points). The tooltip rounds to 5 points.

**Rehoming.** Moving a stack between holders calls `DecayService.rehome`. Our container block entities do it in `setItem`; the player inventory is rehomed in the body's hourly pass; a dropped `ItemEntity` takes the air, water or snow key on join and on each hourly check. An unloaded chest needs no work: its clock keeps counting by formula, so loaded and unloaded storage give the same answer exactly.

**Wet and temperature.** `yearofice:wetness` and `yearofice:item_temp` store a value, a clock and a stamp. Settling walks the clock's hourly series since the stamp, at most 72 steps, then saturates. This keeps the Items tab's frost clock (600 points per kg) and drying rates.

**Blocks.** `yearofice:decay_marks` is a sparse chunk attachment: position, curve, clock key and due. Placing a piece with a `yearofice:decay` data-map entry adds a mark. On `ClockEvent.Day` for loaded chunks and in catch-up for the rest, a mark past due advances the block one `age` state and posts `DecayEvent.StageChanged`.

**Carcasses.** The `CarcassRecord` keeps its own `fresh` with k and the size factor s (Bestiary › From warm to bones). Gutting changes k and recomputes `due` at the same progress. A second track on the same clock, curve `soft_tissue`, runs bloat to bones. Rigor and core temperature stay their own closed forms.

**API.** `DecayService`: `clockAt(level, pos)`, `stamp(stack, clock, curve)`, `rehome(stack, from, to)`, `progress(stack)`, `settle(stack)`, `mark(pos, curve)`, `stage(pos)`. Users: food, butchering, carcasses, forage, build (piece ageing), world (bone piles, deadwood), tooltips.

**Sync.** The client needs D(now) only for the clocks on screen. `decay_heads` sends the body, pocket and open container clocks (D and its hourly rate, 16 bytes each) on menu open and each game hour; the client computes every bar with the same formula.

**Cost and tests.** 0.2 ms a tick. GameTests: `decay/loaded_equals_unloaded` (27 stacks, 10 game days, the same band exactly), `decay/rehome_one_band` (100 moves age food by at most one band each), `decay/one_clock` (meat in a pack, on a rack and in a carcass at 10 °C reach Turning within 1 game hour of each other at the same k), `decay/frozen_stops` (−10 °C for 30 days leaves progress unchanged), `decay/marks_catch_up`.

### Layer 6: Item model

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| Studio lead | `item`; component types in `item.component.YoiComponents` | Data components on stacks; data maps | On use, craft and settle | Stack stream codecs; data maps by NeoForge's sync |

**Static numbers are data maps.** One map per concern, so the hot paths read small maps.

| Data map | File | Fields |
| --- | --- | --- |
| `yearofice:weight` | `data_maps/item/weight.json` | `weight_g` |
| `yearofice:props` | `data_maps/item/props.json` | `category`, `stack_class`, `floats`, `shiny`, `scent_level`, `frost_kg`, `material`, `max_condition`, `salvage`, `theft` |
| `yearofice:food` | `data_maps/item/food.json` | `kcal_per_100g`, `protein_g_per_100g`, `fat_g_per_100g`, `vit_c_mg_per_100g`, `spoil_days_at_5c` by state, `curve` |
| `yearofice:wear` | `data_maps/item/wear.json` | `warmth_c`, `wind_block`, `kept_wet`, `noise_m`, `scent_level` |
| `yearofice:fuel` | `data_maps/item/fuel.json` | Wood type, `kg`, `burn_min` |
| `yearofice:tool` | `data_maps/item/tool.json` | Job-time multipliers, `edge_loss` per use |
| `yearofice:light` | `data_maps/item/light.json` | `level`, `cone_deg`, `battery_per_h` |

**Changing state is components.** Each has a codec and a stream codec in `YoiComponents`.

| Component | Holds | Rule |
| --- | --- | --- |
| `minecraft:damage`, `minecraft:max_damage` | Condition | Vanilla durability, so the bar is the familiar one; `max_damage` = `max_condition` × quality factor, set on the stack at craft |
| `yearofice:edge` | 0–120 | Cutting tools only |
| `yearofice:quality` | crude, good, fine | Rolled by `item.QualityRoller` at craft completion |
| `yearofice:fresh` | State, clock, due, life | Layer 5 |
| `yearofice:wetness` | 0–100, clock, stamp | Lazy; merges in 20-point bands |
| `yearofice:item_temp` | Frost points, state, clock, stamp | Lazy; merges by state |
| `yearofice:weight_g` | Grams | Overrides the map for one stack (a 4.1 kg pike, a full pack) |
| `yearofice:keepsake` | Story id, found day | Blocks salvage, theft, decay, burning and expiry |
| `yearofice:note_id` | Note entry id | All notes and the 24 logbook pages are the one item `yearofice:note` |
| `yearofice:species` | Species id | Organs, skulls, hides, `whole_game` |
| `yearofice:fuel_l` | Litres | Lamp fuel tins; renamed from `yearofice:fuel` so it never reads as the wood map |
| `yearofice:battery` | Hundredths of a percent | The messenger and headlamp |
| `yearofice:nutrients` | Protein, fat, carbs and vitamin C per unit for mixed foods | Set at craft from the inputs; read by the body's absorb step (RECONCILE 137) |
| `yearofice:dish_id` | The recipe id of a cooked dish | Names the item `yearofice:dish`, so one item carries every stew and mix (RECONCILE 137) |

**Food units (reconcile 43).** Food sold by weight counts in 100 g units: one item is 100 g, a stack of 80 is 8 kg. Single-piece food (a whole fish, an egg, `whole_game`) counts 1 and carries `weight_g`. One formula serves both: kcal = grams ÷ 100 × `kcal_per_100g`. Every loot count, butchering pool and recipe uses units, so the Bestiary's 180 kg of bison meat is `set_count` 1,800.

**Stack classes.** `single` 1, `by_weight` 80, `medium` 20, `small` 50, set as `max_stack_size` at registration from `stack_class`.

**Load.** `item.LoadCalculator` sums weight over hotbar, off-hand, worn gear, pack and lashed slots on each inventory change, as the engine sets.

**Registration.** Each owner package registers its own items with its own `DeferredRegister.Items` (`food.FoodItems`, `build.BuildItems`), so file ownership holds. Base classes (`YoiItem`, `YoiToolItem`, `YoiFoodItem`) and all components live in `item`. A datagen test checks every registered item has a weight and a props entry.

**Tooltips.** One order everywhere (Items › How a stack reads): name and quality, weight, condition, freshness, wet, temperature, scent, use. Built in `client.item.ItemTooltips` from the components and the decay heads.

**Tests.** `item/food_units` (1.0 kg of moose meat is count 10 and its kcal match the map), `item/fine_is_130` (a Fine stack's `max_damage` is exactly 1.3 times a Good one's), `item/every_item_has_weight`, `item/bands_merge`.

### Layer 7: Body

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| Body and survival | `body`; model `sim.body.BodyModel` | Player attachment `yearofice:body`, `copyOnDeath` true | Once per crossed game minute; closed form above 10 | `yearofice:body_sync` to the owner; `yearofice:body_public` to trackers |

**One record.** `yearofice:body` holds every vital in groups: energy (fat and lean kg, the day's kcal balance), stomach (`stomach_kg`, contents), hydration, warmth (core and five skin zones), fatigue and sleep debt, Condition, injuries, illness (nausea, fever, scurvy days, ketosis), grime, Spirits, rest-day counters and exposure time. Grime and nausea are fields here, not separate attachments (reconcile 19).

**The tick.** `body.BodyTicker` listens to `ClockEvent.Minute`. For each player it gathers the inputs and calls `BodyModel.step(record, inputs, minutes)` once, which loops for up to 10 minutes and switches to closed form above that.

1. Environment: `EnvService.sample` at the player (air, wind, precipitation, heat bonus, room).
2. Clothing: wear data and settled garment wetness, zone by zone.
3. Activity: gait, load from `pack_rules`, effort from the `yearofice:work` action.
4. Stomach: digestion into kcal, water and nutrients; the freshness of eaten food sets illness risk.
5. Step: warmth balance, energy, fatigue, injuries, illness, Strength, Spirits.
6. Results: movement attribute modifiers (`yearofice:load`, `gait`, `cold`, `injury`), scent strength for Layer 3, HUD lines through Layer 8, and `VitalChangedEvent` with a cause for any change over 5 points in a game hour.

**Order in the minute.** Fires and rooms step before the body each minute, so the heat index the body reads is this minute's.

**Damage and death.** As the engine sets: `LivingIncomingDamageEvent` hands all player damage to `body.DamageRouter`, which writes an injury or a Condition loss. The model alone decides collapse, posts `BodyCollapseEvent`, and runs Endless SOS or the Hardcore end.

**API.** `BodyService`: `warmth`, `condition`, `strength`, `bmi`, `canDraw(drawWeight)`, `effort`, `scent`, `isSafeToStage`, `applyInjury(player, injury, cause)`, `eat(player, stack, grams)`, `drink(player, litres, source)`. `wake(player, cause)` ends sleep or waiting at once and posts a `warn` line; a predator cause also requests the close clock. Ecology calls it, because Body may not read Ecology (request 85). `applyInjury` is the only way any injury reaches a player, animal attacks included (DIRECTOR RULING 90). Users: HUD, encounters, journal, work rates, bows, eating, drinking, medicine, Ecology's sleeper watch.

**Sync.** `body_sync` goes to the owner when any value moves by more than 1% or once a game hour. `body_public` carries limp, shiver, breath rate, grime step and carry pose to players who track this one, on change only.

**Cost and tests.** 0.3 ms a tick per player. GameTests: `body/closed_form_matches` (600 minutes stepped and closed form within 0.5%), `body/cause_posted`, plus the engine's M4 tests.

### Layer 8: Messenger and HUD bus

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| Look, sound and UI | `hud` (common), `client.hud` | Journal lines in `yearofice:journal`; nothing else saved | Flushed at the end of each tick | `yearofice:hud_line` per line |

**Every word goes through one bus.** A server system that tells the player anything calls `HudBus.post(player, HudLine)`. A `HudLine` is a type, a priority, a lang key, up to 6 typed arguments (int, fixed-point with a unit tag, item id, species id) and a source. The client formats numbers through `client.ui.Units`, metric first.

**Line types.** These are the Look, sound and UI department's types plus the three the HUD already had. Lang keys are `messenger.yearofice.<type>.<id>`.

| Type | Channel | Priority | Example |
| --- | --- | --- | --- |
| `pickup` | `pickup_line` | 1 | "+3 birch bark" |
| `prompt` | `prompt_line`, row 1 | 2 | "Storm from 15:00. Under-roof jobs: 3 queued." |
| `readout` | `prompt_line`, row 2 | 1 | "Ice 12 cm" |
| `alert` | `messenger_line` | 2 | "Thunderstorms tomorrow afternoon." |
| `body` | `messenger_line` | 2 | "Weight −0.3 kg (68.9 kg). BMI 20.6. Strength 61." |
| `warn` | `messenger_line` | 3 | "Dark in 1 hour. Camp is 1.8 km." |
| `season`, `look`, `card` | `messenger_line` | 1 | "The valley is greening." |
| `event`, `nearmiss` | `messenger_line` | 2 | "Bears gathering at the falls." |
| `journal` | Journal page only | 0 | "Felled a tree for a squirrel: about even." |

**Bus rules.**

- The messenger channel needs the device in the neck slot or a hand and battery above 0; otherwise its lines go to the journal only.
- During the close clock only priority 3 shows; the rest wait.
- A queue of 3, each line 4 s plus 0.05 s a character, at most 8 s.
- The same key and arguments within 10 game minutes are dropped.
- **No trap catches.** Any line whose source is a trap is refused: an exception in development builds, a drop in release.

**GUI layers.** `client.hud.HudLayers` registers every layer in `RegisterGuiLayersEvent` in this order, bottom to top. Vanilla health, food, armor, air, experience and vehicle health are cancelled in `RenderGuiLayerEvent.Pre`.

| # | Layer | Where |
| --- | --- | --- |
| 1 | `yearofice:headwear` | Full screen, under the frost (client, request 85) |
| 2 | `yearofice:frost` | Full screen |
| 3 | `yearofice:body_fx` | Full screen |
| 4 | `yearofice:threat_flash` | Screen edge |
| 5 | `yearofice:vitals_strip` | Top right |
| 6 | `yearofice:body_figure` | Top left |
| 7 | `yearofice:load_meter` | Right of the hotbar |
| 8 | `yearofice:carry_slot` | Over the hotbar |
| 9 | `yearofice:pickup_line` | Above the hotbar |
| 10 | `yearofice:messenger_line` | Top centre |
| 11 | `yearofice:prompt_line` | Near the crosshair, two rows |
| 12 | `yearofice:work_ring` | Crosshair |
| 13 | `yearofice:breath_ring` | Crosshair; hides the work ring while shown |
| 14 | `yearofice:captions` | Bottom right; replaces vanilla subtitles (client, request 85) |
| 15 | `yearofice:minigame` | Lower centre |
| 16 | `yearofice:field_note_card` | Bottom left; from M3 (video 2, A8: Creative concepts › Field-note cards) |

**One minigame slot (reconcile 8).** `yearofice:minigame` hosts the bow drill gauges, tension bar, cordage marker, pull-and-brace, fishing fight, sling beat and the call rhythm bar. `CallRhythmOverlay` is a renderer inside this layer, not its own layer. `client.hud.MinigameSlot` grants the slot to one owner at a time, so two overlays can never stack.

**API.** `HudBus`: `post`, `offerPrompt(player, key, text, priority)`, `clearPrompt`. Events: `PromptOfferEvent`, `JournalFirstEvent`. Users: every server system with something to say.

**Tests.** `hud/no_trap_lines`, `hud/close_clock_filter` (a priority-2 line during a bear charge shows only after the close clock ends), `hud/battery_zero_to_journal`.

### Layer 9: World edits

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| World generation | `world.change`, `world.pass`, `world.catchup`; planners in `sim.terrain` | SavedData `yearofice_terrain`; chunk attachments `season_stamp`, `placed_mask`, `flood_flags`, `erosion_counters`, `trees`, `stand`, `decay_marks` | Ops within a game hour; passes amortised; catch-up on chunk load | Vanilla block updates |

**Two kinds of world change.** Every block the world changes on its own goes through this layer, in one of two forms.

| Form | For | How |
| --- | --- | --- |
| Op queue | Discrete, seeded, permanent edits: the nine hydrology ops, beaver dams, wildfire burn, windthrow, `LIMB_BREAK` (storm limb fall, RECONCILE 138), rockfall, avalanche debris, pingos, frost heave, beetle kill, succession | `ChangeOp` records keyed by `ChunkPos` in `yearofice_terrain` (Water, calls and encounters › In Minecraft: the terrain-change queue) |
| Pass | State that is a pure function of date and column: snow layers, ice blocks, flood level, season block states (`ripe`, `seeded`, leaf stage), mud season | A `ChunkPass` with a kind; the chunk's `season_stamp` records the last day applied per kind |

**Never the player's blocks.** `yearofice:placed_mask` marks every block a player or a building piece placed. It is set on `BlockEvent.EntityPlaceEvent` and by frames, and cleared on break. It is stored per chunk only for sections that hold a placed block. No op or pass writes a masked block.

**Catch-up on chunk load.** On `ChunkEvent.Load` for a saved chunk, `world.catchup.CatchUp` runs four steps in a fixed order.

1. Terrain ops, oldest first.
2. Passes, each from its stamped day to today.
3. `ChunkCaughtUpEvent(chunk, fromDay, toDay)`: traps roll missed dawns and dusks, crops grow, fires and rooms settle, decay marks advance.
4. Ecology materialises animals whose records sit in the chunk and are within the Active radius.

**Pre-planned diffs.** Within 600 blocks of a player, a worker thread folds a chunk's pending ops into one net diff ahead of time. Loading then only sets blocks. Ops older than 3 years are already merged into their result (Water, calls and encounters).

**Writing blocks.** Edits use `Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE`, so nothing cascades, and the chunk is relit once after its batch.

**API.** `WorldEditService`: `submit(ChangeOp)`, `registerPass(kind, pass)`, `isPlayerPlaced(pos)`, `pendingFor(chunkPos)`. Events: `TerrainChangedEvent(chunkPos, ops)`, `ChunkCaughtUpEvent`. Users: hydrology, beavers, fire (wildfire), forage (regrowth), world (stands, snow, ice), fish lie fields (rebuilt on `TerrainChangedEvent`), map tiles, water features.

**Cost and tests.** Ops at most 256 block sets a tick and 64 permanent edits a chunk a tick; passes 4–6 chunks a tick; catch-up under 2 ms a chunk. GameTests: `world/catchup_equals_live` (a chunk kept loaded for a simulated year and one caught up on load match block for block), `world/placed_mask_respected`, `world/catchup_order` (a trap under a new gravel deposit rolls after the deposit lands).

### Layer 10: Sound events and the audio bus

| Owner | Package | State | Update | Sync |
| --- | --- | --- | --- | --- |
| Look, sound and UI | `sound` (common), `client.audio` | None saved | Per event | Vanilla sound packets up to 128 blocks; `yearofice:distant_sound` beyond |

**Ids.** `yearofice:<group>.<name>`. Animal voices are `entity.<species>.<call>`, player-made calls `call.<name>`, messenger chimes `messenger.<name>`. Groups otherwise follow Look, sound and UI's table, plus `bug`, `build`, `plant`, `tree` and `ui` from the content tabs (fish sounds use `fish`, never `fishing`): `weather`, `world`, `water`, `ice`, `fire`, `step`, `work`, `trap`, `weapon`, `item`, `fish`, `body`, `travel`, `ambient`. Subtitles are `subtitles.yearofice.<group>.<name>`.

**Registered by group.** `sound.YoiSounds` holds one `DeferredRegister.SoundEvents` holder class per group. `sounds.json` is generated from `tools/sounds/` by datagen, each with a range in blocks.

**One call to play.** `SoundBus.play(level, pos, id, rangeM, flags)` routes by range.

| Range | Route | Client |
| --- | --- | --- |
| Up to 128 blocks | Vanilla packet, volume = range ÷ 16 | Vanilla attenuation |
| Over 128 blocks | `distant_sound` payload to every player within the range, with no cap | `client.audio.DistantSounds` plays the `_far` variant with its own distance curve, direction and delay (distance ÷ 340 m/s) |

The 1,024-block cap in earlier tabs came from vanilla volume limits; the payload route has none, so a 5 km wolf chorus reaches 5 km.

**Sound makes noise.** Unless flagged `NO_AI` (UI and messenger sounds), every play also emits a noise event of the same range through Layer 3. Designers set a range once, and the AI and the ear agree.

**The audio bus on the client.** `client.audio.AudioBus` caps 32 voices in six buses: ambience beds (8), weather, animals, danger, body and work, UI. Danger cues are never culled, loops are culled first, and no music plays over a danger sound. Range modifiers (mist, deep cold) come from `sim.sound.Propagation`, the same function the animals use.

**API.** `SoundBus`: `play`, `playTo(player, id)`, `playDistant`. Users: every system that makes a sound.

**Tests.** `sound/distant_payload_range` (a 3 km howl reaches a player at 2.9 km, not at 3.1 km), `sound/sound_makes_noise`, `sound/danger_never_culled` (40 loops playing, a bear huff still sounds).

### The server tick and its budget

**Two hooks.** `ServerTickEvent.Pre` advances the clock and resolves the rate, so everything in the tick sees the new time. `ServerTickEvent.Post` runs `core.tick.ServerScheduler`, whose phases follow in a fixed order. Entity ticks, including every Brain, run between the two in vanilla's level tick.

**Phase order.** 1 clock and rate; (vanilla level tick: entities, block entities, chunk loading); 2 minute pipeline: fires and rooms, then body, then decay heads; 3 hourly and daily work in slices: environment epoch, scent prune, Nearby and Abstract tiers, population step; 4 world edits: ops, passes, catch-up queue; 5 HUD bus and sync flush.

**Budget at 20 TPS.** A tick has 50 ms. The target is MSPT under 30 ms on average, so the mod's share is about 22 ms and vanilla keeps the rest.

| System | Rate | Average ms | Peak ms |
| --- | --- | --- | --- |
| Clock and rate | Every tick | 0.05 | 0.1 |
| Environment | Lazy, hourly epoch | 0.5 | 2 |
| Scent and noise | Hourly prune, per event | 0.3 | 1 |
| Tiers, Nearby, Abstract, populations | Minute and hour slices, daily step | 2 | 4 |
| Active animals (Brain, sensors, pathing), birds and fish | Every tick | 8 | 10 |
| Encounters | Each game minute | 0.05 | 0.2 |
| Body | Each game minute | 0.3 per player | 1 |
| Decay | Hourly | 0.2 | 1 |
| Fires and rooms | Each game minute | 1 | 2 |
| Carcasses | Hourly step, 20-tick water check | 0.5 | 0.5 |
| Traps, crops, other block entities | Dawn, dusk, day | 0.3 | 1 |
| Current push | Every tick | 0.5 | 1 |
| World ops | Per op | 1 | 2 |
| Snow, ice and season passes | Pass days | 0.5 | 3 |
| Catch-up | Per loaded chunk | 0.5 | 2 a chunk |
| HUD and sync flush | Every tick | 0.3 | 1 |
| **Total** |  | **about 17** | **under 22 in any one tick** |

**Enforcement.** `core.tick.Budget` times each sliced phase with `System.nanoTime`; a phase that runs over its peak stops and carries the rest of its queue to the next tick. Peaks of separate phases are staggered: passes never run in the same tick as a daily population step. spark measures the totals in CI, as the engine sets.

### Threading rules

1. **The main server thread owns the world.** Only it changes blocks, entities, attachments and SavedData.
2. **Two worker pools.** `yearofice-gen` runs worldgen layers. `yearofice-plan` runs pure planning: terrain diffs, unloaded population years, map tiles, monthly snapshots.
3. **Snapshots in, results out.** A worker gets an immutable snapshot and returns a result with the epoch it was built on. The main thread applies it through `server.execute` and drops it if the epoch has moved.
4. **No world reads off-thread.** Workers never call `Level` methods; chunk generation follows Minecraft's own contract only.
5. **Payloads land on the main thread.** Every payload handler is registered with the default main-thread handling; none opts into the network thread.
6. **The render thread reads mirrors.** Client mirrors are written on the client main thread when a payload arrives and only read while rendering.
7. **The sim is stateless.** `com.yearofice.sim` has no static mutable state, so workers and JUnit can call it freely.
8. **No locks in the tick, no parallel streams on the main thread.**

**Rule 9: the load gate (request 98).** A chunk is never sent to a player before its catch-up has finished. `world.catchup.LoadGate` holds the chunk's send after `ChunkEvent.Load` and releases it when ops, passes and `ChunkCaughtUpEvent` are done, within 2 ticks; a player who outruns it sees the edge of the loaded area, never a chunk that changes in front of them.

### Determinism and seeding

- **Worldgen stays exact.** The engine's rules stand: integer hashing, `+ − × ÷` and `Math.sqrt`, `StrictMath` for trig, no `java.util.Random`, no `HashMap` iteration in results, and the lint in `tools/lint-gen`.
- **Named streams.** Every runtime roll comes from `core.hash.Rng.of(seed, system, keys...)`, for example `Rng.of(seed, "trap", pos, day, period)`. Keys are positions, record ids and game days, never tick numbers or call order.
- **Vanilla random is cosmetic.** `level.random` and entity randoms are allowed only for particles, sound pitch and animation offsets.
- **Live equals caught up.** A system's catch-up uses the same rolls and the same closed forms as its live step, so a chunk loaded all year and one loaded at the end agree. Every system that catches up ships a GameTest that proves it.
- **Ordered outputs.** Collections that feed saves or rolls are sorted or `LinkedHashMap`; floats come from the sim only.
- **What is not deterministic.** Animal paths in the Active tier depend on frame timing and the player. Their outcomes (a kill, a theft, a staged encounter) are rolled from named streams, so the story of a year repeats even when the footage does not.

### Events and hooks

All events are server-side classes in `com.yearofice.api.event`, posted on `NeoForge.EVENT_BUS`. Listeners are registered with explicit `EventPriority` only when order matters, and the table names that order.

| Event | Posted by | When | Main listeners | Cancellable |
| --- | --- | --- | --- | --- |
| `ClockEvent.Minute`, `.Hour`, `.Day`, `.Dawn`, `.Dusk` | `story.clock` | As the clock crosses them | Fires and rooms (HIGH), body (NORMAL), decay (LOW), traps, crops | No |
| `ClockEvent.Jump` | `story.clock` | Over 60 minutes in one tick | Every system with a closed form | No |
| `RateChangedEvent` | `story.clock` | The world rate changes | Client sync, ambience | No |
| `WeatherChangedEvent` | `story.Weather` | Hourly state change | Environment epoch, fires, animals | No |
| `StormEvent.Start`, `.Peak`, `.End` | `story.Weather` | A storm from the Weather and storms tab begins, peaks or ends | Fires, rooms, animals, `LIMB_BREAK` ops, the forecast, journal | No |
| `FireStateEvent` | `fire` | A fire changes state | Heat index, fire roster, scent (smoke) | No |
| `RoomChangedEvent` | `build.rooms` | A room forms, breaks or changes temperature band | Environment, decay (room clocks), containers | No |
| `IceChangedEvent` | `env.water` | A body's ice state changes | Ice pass, drift, fishing | No |
| `TerrainChangedEvent` | `world.change` | Ops applied in a chunk | Fish lie fields, map tiles, water features, POIs | No |
| `ChunkCaughtUpEvent` | `world.catchup` | After ops and passes on load | Traps, crops, fires, rooms, decay marks | No |
| `ScentEvent` | `ecosystem.senses` | A point is created | Bears, scavengers | No |
| `NoiseEmittedEvent` | `ecosystem.senses` | A noise is emitted | Debug overlay, tests | No |
| `PopulationEvent.Birth`, `.Death` | `ecosystem.population` | Daily step | Journal, legends | No |
| `PredationEvent` | `ecosystem.foodweb` | An abstract kill | Found carcasses, scent | No |
| `IndexChangedEvent` | `ecosystem.population` | May 1 | Breeding, story events | No |
| `EncounterEvent.Staged`, `.Revealed`, `.Ended` | `ecosystem.encounter` | Director slots | Journal, sound cue, close clock | Staged only |
| `AnimalKilledEvent` | `ecosystem.wound` | A kill by any cause | Carcass factory, ecology, journal, legends | No |
| `CarcassEvent.Created`, `.Dressed`, `DecayEvent.StageChanged` | `ecosystem.carcass`, `decay` | Lifecycle steps | Scent, scavengers, renderer sync | No |
| `TrapEvent.Caught`, `.Checked` | `food.traps` | Rolls, checks | Ecology harvest, journal | No |
| `StationCraftEvent` | `build.stations` | A craft completes | Quality, practice, journal | No |
| `VitalChangedEvent`, `BodyCollapseEvent` | `body` | Body step | HUD, journal, SOS | No |
| `PromptOfferEvent`, `JournalFirstEvent` | Any system, `story.Journal` | Offers and firsts | HUD bus | Yes |

There is no `ChangeTickEvent`: `world.change.ChangeTicks` derives the five change ticks from `IceChangedEvent` (Breakup, Freeze-up), `WeatherChangedEvent` (Storm day) and `ClockEvent.Day` (Midsummer and Winter steps), so one source decides each (request 98). Vanilla events we listen to are listed in each layer and in the vanilla table below; the main ones are `LivingIncomingDamageEvent`, `EntityJoinLevelEvent`, `ChunkEvent.Load`, `BlockEvent.EntityPlaceEvent`, `BlockEvent.BreakEvent`, `RenderGuiLayerEvent.Pre` and `ItemExpireEvent`.

### The save and sync matrix

| State | Kind | Scope | Saved | Synced to client |
| --- | --- | --- | --- | --- |
| `yearofice_world` | SavedData | World | Yes | Seed facts in `data_sync` |
| `yearofice_clock` | SavedData | World | Yes | `clock_sync` |
| `yearofice_populations`, `yearofice_region_<key>`, `yearofice_encounters` | SavedData | World, region, player | Yes | No |
| `yearofice_scent` | SavedData | Level | Yes | No |
| `yearofice_decay` | SavedData | Level | Yes | `decay_heads` for clocks on screen |
| `yearofice_ice`, `yearofice_water` | SavedData | Level | Yes | Through blocks and `env_sync` |
| `yearofice_terrain` | SavedData | Level | Yes | Through blocks |
| `yearofice_rooms`, `yearofice_piece_damage`, `yearofice_markers`, `yearofice_radio` | SavedData | Level | Yes | Map pins and screens by request |
| `yearofice:body` | Player attachment | Player | Yes, `copyOnDeath` | `body_sync` (owner), `body_public` (trackers) |
| `pack_rules`, `carry`, `practice`, `known`, `map`, `journal`, `jobs`, `work`, `tool_memory`, `study` | Player attachments | Player | Yes | Owner, by their own payloads |
| `yearofice:individual`, `carcass`, `legend`, `wariness` | Entity attachments | Entity | Carcass and legend yes; individual lives in the record | Through `SynchedEntityData` fields |
| `current`, `season_stamp`, `sign`, `placed_mask`, `microclimate`, `trees`, `stand`, `hazard`, `flood_flags`, `erosion_counters`, `decay_marks`, `trail_wear` | Chunk attachments | Chunk | Yes | `current_chunk`, sign decals within 48 blocks; the rest server only |
| Environment section cache, heat index, noise buffer, held lights | Memory | Level | No, rebuilt | `env_sync` |
| Region layers | Disk cache | Region | Cache only, rebuilt from seed | Horizon mesh at login |
| Item components | Stack | Stack | With the stack | Stream codecs |
| Data kinds, data maps | Data packs | Server | No | `data_sync`, NeoForge data-map sync |

**Rules.** Every new field gets a codec default, ids are never renamed, and the 5 MB mod-data budget holds (engine tab). Per-section data such as support results are arrays inside a chunk attachment, since attachments hang on chunks, not sections.

### Vanilla systems we replace

This is the master list. Rows already written in the engine tab and the Bestiary's No vanilla mob mechanics are summarised and point there.

| Vanilla system | What replaces it | Mechanism | Owner |
| --- | --- | --- | --- |
| Hunger and saturation | Stomach, kcal and the body model | Food level held full and its bar hidden; eating runs `BodyService.eat` | Body |
| Health hearts and regeneration | Condition and injuries | Health held at 20; `LivingIncomingDamageEvent` routes all damage; `naturalRegeneration` false; `LivingHealEvent` cancelled | Body |
| Sleep and the night skip | Our beds and the sleep rate | `CanPlayerSleepEvent` any hour; `SleepFinishedTimeEvent` cancels the skip; `playersSleepingPercentage` 100 | Body, story |
| Death and respawn | Endless SOS or the Hardcore end | `LivingDeathEvent` cancelled for causes our model did not start; the respawn screen never shows; `keepInventory` true | Body |
| Weather | The year table's weather chain | `doWeatherCycle` false; biomes without precipitation; rain level used only for sky darkening | Story, env |
| Time and the moon | Our clock, sun and moon | `doDaylightCycle` false; `dayTime` written from the sun | Story |
| Mob spawning and despawning | Populations and tiers | `doMobSpawning` false, the `remove_spawns` biome modifier, `FinalizeSpawnEvent`; Bestiary rows 16–19 | Ecosystem |
| Mob loot, XP, breeding, taming, leads, mounts | Carcasses, practice, the calendar, trust | Bestiary › No vanilla mob mechanics, rows 1–32; `doMobLoot` false, `maxEntityCramming` 0 (reconcile 44) | Ecosystem |
| Item despawn after 5 real minutes | Dropped items last 7 game days (reconcile 36) | `ItemEntity` lifespan set to max on join; `ItemExpireEvent` and a chunk-load check compare the drop's game-minute stamp; keepsakes, notes, the messenger and packs never expire; sunk gear becomes a `yearofice:lost_gear` block | Item |
| Fire spread and lightning fire | Our fire and spread rules | `doFireTick` false; no vanilla `LightningBolt` is spawned; a strike places our smoulder, which escalates to wildfire only if the year drew wildfire or fire danger is Extreme; player fires always can (reconcile 38) | Fire |
| Crafting table, 2 × 2 grid, recipe book | Stations and the pack screen | Vanilla recipes filtered; inventory screen replaced on `ScreenEvent.Opening`; recipe type `yearofice:station` | Build |
| Furnace, blast furnace, smoker | The fire, bloomery furnace and forge stations | Vanilla blocks hidden and their recipes filtered; our block entities with menus | Build, fire |
| Light | Vanilla block light kept; sky light driven by the sun; gameplay light from `env.light` | Fires and lanterns emit vanilla block light; held lights below | Env, client |
| Beds and spawn points | `debris_bed`, `bough_bed`, `bunk` | No respawn anchor; `spawnRadius` 0 at the outpost bunk | Body, build |
| Villages, traders, golems, raids | Nobody lives in the valley; people are radio voices | No vanilla structures; `banned_vanilla`; `disableRaids` true | Worldgen, ecosystem |
| Structures, dungeons, strongholds | Set pieces | Structure type `yearofice:setpiece`; no vanilla structure sets | Worldgen |
| Nether, End, portals | One overworld | World preset; `PortalSpawnEvent` cancelled; `yearofice:other_dimensions` off | Worldgen |
| Random ticks: crops, grass, leaves, ice, snow | Calendar passes | `randomTickSpeed` 0; World edits passes; crops on `ClockEvent.Day` | World, food |
| Powder snow freezing, drowning, fall damage | Cold, the breath ring, sprains | `freezeDamage` false; damage routed to the body | Body |
| Experience, enchanting, anvils, brewing | Practice and quality | XP cancelled (Bestiary row 2); tables, anvils and stands hidden | Item |
| Vanilla fishing loot | Fish stocks | Vanilla rod hidden; `ItemFishedEvent` cancelled | Hunt |
| Maps, compasses, clocks | The messenger | Items hidden; the messenger map and forecast | UI |
| Boats, minecarts, horses | Canoe, raft, sled | Hidden; mounting only `#yearofice:vehicles` | Travel |
| Chests, barrels, shulker boxes, ender chests | Our containers | Hidden; `YoiContainerBlockEntity` rehomes decay | Build |
| Vanilla lily pad, mud, packed mud, mud bricks | Our versions | Hidden from creative tabs and loot; recipes filtered (reconcile 34) | World, build |
| Difficulty | Presets | Difficulty locked to Normal; every mod damage type `"scaling": "never"` (reconcile 44) | Core |
| Advancements | Ambitions and the journal | `minecraft:advancement/` filtered; `announceAdvancements` false | Story |

**The full game-rule set.** The world preset sets and `core.rules.GameRuleEnforcer` re-applies on server start: `doDaylightCycle`, `doWeatherCycle`, `doMobSpawning`, `doPatrolSpawning`, `doTraderSpawning`, `doInsomnia`, `doFireTick`, `naturalRegeneration`, `doMobLoot`, `mobGriefing`, `universalAnger`, `freezeDamage`, `announceAdvancements` all false; `disableRaids`, `keepInventory`, `doEntityDrops` true; `randomTickSpeed` 0; `spawnRadius` 0; `maxEntityCramming` 0; `playersSleepingPercentage` 100. Difficulty Normal, locked. Two of ours join them: `yearofice:first_person_only` true and `reducedDebugInfo` true (request 85; developer worlds turn both off). **Every other vanilla item** (buckets, flint and steel, torches, shields, ender pearls, elytra, bone meal, spyglass) cannot be had in survival: its recipe is filtered, no loot table of ours names it, it is hidden from creative tabs, and `PlayerInteractEvent.RightClickBlock` cancels any bucket use on a fluid source, so no player moves water blocks.

**Held light (reconcile 35).** Gameplay light never depends on rendering: it comes from `env.light`, so animals see a lantern the same with any renderer. Rendering is client-only and places no light blocks, since those would cost light-engine updates and break the placed mask.

1. If a dynamic-lights mod with a NeoForge API is installed, `client.light.HeldLight` registers our light items with it.
2. Otherwise, on the vanilla renderer, one whitelisted mixin raises block light in the light lookup used for meshing and entity lighting, for up to 8 sources within 16 blocks. Sections in a source's radius rebuild when it crosses a block boundary, at most 10 times a second.
3. Under Sodium without such a mod, held light lights entities and the held item but not terrain, until prototype P4 shows a safe path through Sodium's own light pipeline.

### Conventions settled

These close the naming and convention items of the reconcile list. Content tabs change their text to match when the Director applies the list.

| # | Item | Decision |
| --- | --- | --- |
| 6 | Data paths and component names | `data/yearofice/<kind>/<id>.json`, one entry per file, kind a singular snake\_case noun; never `data/yearofice/yearofice/`. Our reload listeners, not datapack registries, except Minecraft's own kinds. `yearofice:fresh`, not `freshness`; condition is vanilla durability, edge is `yearofice:edge` |
| 6 | Paths renamed | `yearofice/species/` → `species/`; `yearofice/arrivals/` → `arrival/`; `yearofice/rare_sightings/` → `rare_sighting/`; `fish_species/` → `fish/`; `item_props.json` → `data_maps/item/props.json`; `presets/` → `preset/`; `land/phenomena/` → `phenomenon/`; `land/sky_signs.json` → `sky_sign/<id>.json`; `land/microclimate.json` → `microclimate/<class>.json`; `land/succession.json` → `succession/<stage>.json` |
| 18 | Plants path and JSON case | `data/yearofice/plant/<id>.json`; every JSON field is snake\_case with a unit suffix (`whole_kg`, `tau_hours`, `population_model`). The validator rejects camelCase keys |
| 21 | Heat data | Data map `data_maps/block/heat.json` only |
| 22 | Multiblocks and pieces | `data/yearofice/multiblock/<id>.json` for multiblocks; `data_maps/block/piece.json` for per-block numbers; built-in blueprints at `structure/blueprint/<id>.nbt` |
| 16, 32 | Packages | The package map above: `forage` for plants, `food` for eating and stomach, `food.traps` for traps, `ecosystem` for animals, `hunt` for weapons, calls, butchering and fishing gear |
| 19 | Player attachments | Grime and nausea are fields of `yearofice:body` |
| 41 | Carcass naming and loot | `yearofice:carcass` is the entity tag `#yearofice:carcasses` and the base class `ecosystem.carcass.CarcassEntity` over `carcass_tiny` to `carcass_huge`. Entity death loot tables are empty; every death makes a carcass, tiny ones pocket into `whole_game`. Yields are `loot_table/butchering/<species>.json`, pooled by `yearofice:dressing_step`. The engine's `butcher/` data kind is retired: step times live in the species `carcass` block. `core_temp` is the record's `core_temp_c` field (RECONCILE 101) |
| 43 | Food units | 100 g units for everything sold by weight, in loot, recipes and butchering; `kcal_per_100g` in the food map |
| 8 | Call rhythm | Inside the `minigame` layer, one slot |
| 9 | Multiplayer time | Layer 1's multiplayer rule; the time debt is the future co-op design (RECONCILE 115) |
| 34, 36, 38, 44 | Vanilla rules | In Vanilla systems we replace |
| 35 | Held light | Above |
| — | Sound ids | `entity.<species>.<call>` for animals (the engine's `animal.bear_huff` becomes `entity.grizzly.huff`; Water calls' `moose.answer` becomes `entity.moose.answer`); groups for the rest (Land's `frost_quake` becomes `world.frost_quake`) |
| — | Lang keys | Messenger lines `messenger.yearofice.<type>.<id>`; subtitles `subtitles.yearofice.<group>.<name>` |
| — | Notes and pages | One item `yearofice:note` with `yearofice:note_id`; the 24 logbook pages are note ids `logbook_01` to `logbook_24` |

Items left to other developers: 5, 13, 31, 37 and 39 (keys and screens) to the client developer; 12 and 20 (the dig exceptions) to the world developer; the content numbers (3, 10, 14, 23–30, 33, 40, 42) to their departments. Every request between the developer sections is settled in Requests between developers, settled, below.

### Requests between developers, settled

**Every request one developer section made of another is decided here (review round, 19 October).** The decision is binding; the owning section's text was changed to match, and the requests lists at the end of each section stay as the record of what was asked.

| RECONCILE | From → to | Request | Decision | Lives in |
| --- | --- | --- | --- | --- |
| 82 | Lead → world and story | A `ground` block in `weather/interior_alaska.json` | One row per soil class (silt, sand, gravel, peat, till, rock, permafrost) × month: `mean_c_at_0_5m`, `mean_c_at_1m`, `amplitude_c`, `frozen_from_day`, `thawed_from_day`. `EnvService.groundC(pos, depth_m)` interpolates linearly between 0.5 and 1 m and holds the 1 m value below. Owner World and story; ships M4 with the body (ground sleeping) and is read by the root cellar in M8 | Layer 2 |
| 82 | Lead → food and hunting | Decay curves and `spoil_days_at_5c` | `decay/<curve>.json` holds `points` as \[°C, rate\] pairs, rate 1.0 at 5 °C, linear between points; `spoil_days_at_5c` is per state (`raw`, `cooked`, `smoked`, `dried`) in `data_maps/item/food.json`, with `curve` naming the curve. The linter fails a food item missing either. Ships M2.5 (raw and cooked meat), complete M5 | Layer 5, Layer 6 |
| 82, 85 | Lead ↔ client | GUI layer order; `headwear` and `captions` | 15 layers in the client's order (Player systems and client › The HUD layer stack): `headwear` first, `captions` 14th, `minigame` last; the call rhythm bar and the sling beat are renderers inside the one `minigame` slot | Layer 8 |
| 85 | Client → lead | `item.inventory` package | Confirmed: a subpackage of the studio lead's `item`, written by the client developer, every change reviewed by the studio lead | Package and module map |
| 85 | Client → lead | `BodyService.wake(player, cause)` | Added. Ecology's `ecosystem.encounter.SleeperWatch` checks each sleeping or waiting player every 20 ticks and calls it when a predator or an animal over 100 kg comes within that species' `wake_m` (species file; grizzly 40, wolf 30, moose 25, default 20). Wake posts a `warn` line with the cause and, for a predator, requests the close clock | Layer 7; Entities › Interlace |
| 85 | Client → lead | Game rules `yearofice:first_person_only`, `reducedDebugInfo` | Both true in the full game-rule set; developer worlds turn them off | Vanilla systems we replace |
| 85, 9 | Client → lead | Co-op time proposals | Recorded as the future co-op design, not built in M0–M9 (RECONCILE 115). Built now: rates requested per player, sleep only when all sleep, a per-player SOS jump | Layer 1 |
| 86 | Client → world | Hourly temperature record for closed-form body steps | `EnvService.hourlySeries(pos, fromDs, toDs)` returns air, feels-like and wind per game hour from `sampleAbstract` (year table, elevation band, micro-climate class), with the room's record when the player sat in a room. The body and decay closed forms read it; a 14-day series is 336 values, built once per jump | Layer 2 |
| 86 | Client → world | `lost_gear` placement | A `DRIFT` op places it on the bed below where the item sank, or on the lowest bed cell within 8 blocks; one block holds 27 stacks and overflow takes the next cell downstream. It sets `placed_mask`, never expires and blocks pruning | World edits › Every other world edit |
| 86 | Client → world | The outpost bar return position | `yearofice_world.params.sos_return`: a dry gravel-bar block below the outpost, above the flood-year line, chosen by `worldgen.StartSite` with the start site | Seed variables |
| 87, 90 | Client → entities | One injury path | `BodyService.applyInjury(player, injury, cause)` only; `BodyService.strike` is removed (DIRECTOR RULING 90) | Animal-to-animal interactions |
| 87 | Client → entities | `carried_arms` and `carried_shoulders` poses | In the rig animation set of every species with a tiny, small or medium carcass; large and huge carcasses are carried as quarters, which use plain baked models | Models, rigs and animation controllers |
| 87 | Client → entities | Night-sight input | Sight sensors read `EnvService.light` (sky, block and held light), never the held item or the renderer | Brain: sensors |
| 88 | Client → build | Lints, sound pipeline, client smoke | Linter checks 7, 8 and 15 extended: a credits line per `.ogg`, no literal units in lang values, a codec default for every attachment field. `tools/soundgen` (Art and audio production) writes `.ogg` files with their rows in `assets/yearofice/credits.json`. SMOKE PASSED item 11 adds the 15-layer HUD shots and the flash scan | The content linter; SMOKE PASSED |
| 89, 97 | World, entities → client | One dropped-item rule | 7 game days by the drop's game-minute stamp; no `ground_pile` and no merging; `#yearofice:never_expires` and `left_pack` never expire; persistent ground blocks set `placed_mask` and block pruning (DIRECTOR RULING 89) | Vanilla systems we replace |
| 91 | Entities → lead | Tiered records and the 8 ms split | Accepted: full records for big game, predators, named and Known animals; slot records for small game, small birds and fish near players; land 4.0, birds 2.5, fish 1.5 ms (DIRECTOR RULING 91) | Layer 4; Entities › Performance |
| 92 | Entities → build | Body plans | The `register` block's `body_plan` names one of the 35 rigs, which fixes the base class; the 13 coarse names are retired | Adding content as data, without code |
| 93 | Entities → world | `brush` blocks | Shrub stems and brush from `forage.ShrubStoolBlock`, placed by the tree builders, tag `#yearofice:brush`: empty entity collision, slowdown inside (moose ×0.9, bear ×0.85, wolf ×0.8, player ×0.6), a 10 m crash noise for anything over 40 kg | Registry table: plants, trees and fungi |
| 93 | Entities → world | Trunk columns per chunk | The `trees` attachment's base, height and species give each trunk column; `ClimbControl` reads them, with no extra store | Chunk attachments |
| 93 | Entities → world | The trail graph | Stage 7 writes nodes (position, kind summer or winter) and edges (cost) into `yearofice_region_<key>.trails`, loaded with the region record; `ecosystem` reads it through `worldgen.Layers` | The nine stages, built |
| 93 | Entities → world, building | Path types on pieces, pens and fire | World blocks: the path-type table in World and worldgen. Pieces (Building and crafting): walls and closed doors `BLOCKED`, open doors `DOOR_OPEN`, pen gates `BLOCKED` to livestock and `DOOR_OPEN` to the player. Fire: a lit `fire_ring` is `DAMAGE_FIRE` | Path types for world blocks |
| 93 | Entities → world | Air points under ice | `env.water.WaterBodyIndex.airPoints(bodyId)` lists ice holes, leads, lodges and push-ups; `IcePass` and the placement of holes, lodges and push-ups update it | Rivers, current and water |
| 94 | Entities → client | LOD, impostors, render layers, payloads | The client developer owns `client.anim.AnimLod` and `LodGeoModel`, the instanced `HerdImpostorRenderer` and flock and school renderers, and `KnownMarkLayer` and `EyeshineLayer`; the entity developer supplies the models and bone names. Payloads `herd_state`, `flock_state`, `school_state` (server to client, under 64 B, every 10–20 ticks), `aim_member` and `wolf_command` (client to server) join the network table: 33 payloads | Networking |
| 98 | World → lead | Change ticks | Derived by `world.change.ChangeTicks`; no `ChangeTickEvent` | Events and hooks |
| 98 | World → lead | `LoadGate` | Threading rule 9: a chunk is not sent to any player until its catch-up has finished; `world.catchup.LoadGate` holds the send and releases it within 2 ticks | Threading rules |
| 100 | World → build | Datagen, template test, hashes, generator versions | Datagen providers in `data.gen` write blockstates, models, loot tables and tags for the 150-odd world blocks; GameTest `data/templates_valid` fails a template with a jigsaw block, a banned vanilla block, a container without a loot table or a piece over 48 × 48 × 32; layer hashes run nightly on Linux, Windows and macOS (x64 and ARM). A world keeps the generator version it was created with for ever; a new version applies to new worlds only, and content behind an `affects_worldgen` flag needs a new world | Tests in CI; Content flags |

**Milestone moves this table makes.** The catch-up framework (`world.catchup`, `ChunkCaughtUpEvent`, `season_stamp`, `LoadGate`) ships in M2, because the SOS jump in M4 needs it. The World edits op queue (`yearofice_terrain`) ships in M5 with its first producers (`DECAY` for gut piles, `DRIFT` for sunk gear); hydrology ops follow in M7. Ecology's core (populations, region records, tiers, slot records, `PopulationSpawner`, harvest) and the Scent and sound field's points ship in M5 with small game and carcasses; full senses, encounters and big game stay in M6. Minecraft engine › Milestones in Minecraft carries the whole order.

### Registry entries for the shared layers

| Id | Type | Class | Data file | Milestone |
| --- | --- | --- | --- | --- |
| `yearofice_clock` | SavedData | `story.clock.ClockData` | `data/yearofice/clock/rates.json` | M2 |
| `yearofice:clock_sync` | Payload | `net.ClockSyncPayload` | — | M2 |
| `ClockEvent`, `RateChangedEvent` | Events | `api.event` | — | M2 |
| `yearofice:microclimate` | Chunk attachment | `env.MicroClimate` | `microclimate/<class>.json` | M2 |
| `yearofice:env_sync` | Payload | `net.EnvSyncPayload` | — | M2 |
| `yearofice:light` | Item data map | `env.light.LightField` | `data_maps/item/light.json` | M3 |
| `yearofice_ice`, `yearofice_water` | SavedData | `env.water.IceData`, `WaterBodyData` | `weather/interior_alaska.json` | M7 |
| `yearofice_scent` | SavedData | `ecosystem.senses.ScentData` | `scent/<kind>.json`, `senses/scent.json`, `senses/propagation.json` | M5 (points), M6 (senses) |
| `yearofice_populations`, `yearofice_region_<key>`, `yearofice_encounters` | SavedData | `ecosystem.population.PopulationData`, `RegionData`, `ecosystem.encounter.EncounterDirector` | `species/`, `index/`, `encounter/` | M5 (populations, regions), M6 (encounters) |
| `yearofice:individual` | Entity attachment | `ecosystem.tier.IndividualLink` | — | M6 |
| `yearofice_decay` | SavedData | `decay.DecayClocks` | `decay/<curve>.json` | M5 |
| `yearofice:decay_marks` | Chunk attachment | `decay.DecayMarks` | `data_maps/block/decay.json` | M8 |
| `yearofice:decay_heads` | Payload | `net.DecayHeadsPayload` | — | M5 |
| `yearofice:fresh`, `edge`, `quality`, `wetness`, `item_temp`, `weight_g`, `keepsake`, `note_id`, `species`, `fuel_l`, `battery` | Data components | `item.component.YoiComponents` | — | M2–M5 |
| `yearofice:weight`, `props`, `food`, `wear`, `fuel`, `tool` | Item data maps | `item.YoiDataMaps` | `data_maps/item/*.json` | M2–M4 |
| `yearofice:body` | Player attachment | `body.BodyRecord` | `body/standard.json` | M4 |
| `yearofice:body_sync`, `body_public` | Payloads | `net.BodySyncPayload`, `BodyPublicPayload` | — | M4 |
| `yearofice:hud_line` | Payload | `net.HudLinePayload` | `assets/yearofice/lang/en_us.json` | M2 |
| 15 GUI layers (Layer 8 list, with the client's headwear and captions) | GUI layers | `client.hud.HudLayers` | — | M2–M6 |
| `yearofice_terrain` | SavedData | `world.change.TerrainChangeQueue` | — | M5 (first ops), M7 (hydrology) |
| `yearofice:season_stamp`, `placed_mask`, `flood_flags`, `erosion_counters`, `trees`, `stand` | Chunk attachments | `world.*` | — | M2–M9 |
| `TerrainChangedEvent`, `ChunkCaughtUpEvent` | Events | `api.event` | — | M5 |
| `yearofice:distant_sound` | Payload | `net.DistantSoundPayload` | `assets/yearofice/sounds.json` | M2 |
| `yearofice:lost_gear` | Block and block entity | `item.LostGearBlock` | `loot_table/blocks/lost_gear.json` (empty) | M5 |
| `yearofice:never_expires`, `yearofice:carcasses`, `yearofice:vehicles`, `yearofice:banned_vanilla` | Tags | — | `tags/item/`, `tags/entity_type/` | M2–M6 |
| Game rules and difficulty lock | World preset and enforcer | `core.rules.GameRuleEnforcer` | `worldgen/world_preset/year_of_ice.json` | M2 |
| Mixin `HeldLightMixin` | Mixin (whitelist slot 2 of 8) | `client.light.HeldLight` | `yearofice.mixins.json` | M3 |
| `yearofice:glaze_layer` (1–3 layers), `hail_layer` (1–2 layers), `fallen_limb`; `battened` state on tarps, hide flaps and window covers | Blocks and block state | `env.phenomena`, `world.pass`; `battened` in `build` | `weather/storm/<id>.json` | M7 |
| `yearofice:strike` | Payload, 14 B | `net.StrikePayload` from `env.phenomena.Lightning` | — | M7 |
| `yearofice_weather` | SavedData | `story.Weather` with model `sim.weather` | `weather/forecast.json`, `weather/ring/<id>.json` | M7 |

### Shared-layer GameTests

All sit in `com.yearofice.gametest.LayerGameTests`, batch by milestone, and wait on game time through the clock. Each layer above names its own; these are the cross-layer ones.

1. **`layers/one_question`.** At 20 positions, body, decay, fire and the HUD read the same air temperature within 0.01 °C in the same tick.
2. **`layers/live_equals_caught_up`.** Two copies of a test chunk with a trap, a crop, a chest of meat, a fire and a snow column: one loaded for 30 game days, one unloaded and then loaded. Every block, catch and band match.
3. **`layers/no_cross_imports`.** The ArchUnit rule: no class in one system package imports another system package.
4. **`layers/budget`.** With 40 Active and 200 Nearby animals, 64 carcasses and a snow pass, average MSPT stays under 30 ms and no tick passes 50 ms.
5. **`layers/item_expiry`.** A dropped knife is there after 6 game days and gone after 8; a keepsake is there after 30; a pack sunk in a lake becomes `lost_gear`.
6. **`layers/deterministic_year`.** Two runs of the same seed with the same scripted bot give the same trap catches, populations and terrain hashes after one game year.

### Risks and first prototypes

| Risk | Why it matters | First prototype |
| --- | --- | --- |
| Catch-up hitches on chunk load | A year away means many ops and passes per chunk, and flying in loads dozens of chunks a tick | P1: load 64 chunks after a simulated year away and log the worst tick; pre-planned diffs must keep each chunk under 2 ms |
| Lazy decay drifts from ticked decay | Players would see chests and packs disagree | P2: a JUnit property test of 10,000 random move histories against a minute-by-minute reference, within one band |
| Rate arbitration feels wrong | Walking slower than standing confuses players; a busy server never time-lapses | P3: a 30-minute playtest of the start site with the rate shown on screen, then the per-player rate rules in a two-player LAN test |
| Held light under Sodium | Many players run Sodium; dark nights without a visible lantern would break the night | P4: the mixin on vanilla and a Sodium light-pipeline spike, both in the client smoke |
| Environment sampling cost | Sensors and the body call it thousands of times a second | P5: a JMH benchmark of 100,000 samples and a GameTest of 40 sensing animals |
| Entity and record split on crashes | A crash between write-back and save could duplicate or lose animals | P6: kill the server process at 20 random points and count individuals after restart |
| Event order between systems | A listener order bug changes results silently | Priorities fixed in the event table; P7: a GameTest that shuffles listener registration and compares a game day |
| Version churn at the pin | 26.1 renamed some GUI and stack APIs | Every NeoForge type is wrapped in our api records; the M0 rename list covers the rest |

## World and worldgen

This section is the world developer's build plan: the chunk generator and biome source for the three rings, how the layers become blocks, every way the world changes after generation, and the chunk data that carries it. It builds on Minecraft engine › Scale and world and World map › World size in Minecraft and changes neither, except where a line below says "Decision" and why. One rule runs through all of it: a chunk on any day is its generated blocks, plus its recorded history, plus what the date says it should look like.

### What this section owns

**Packages.** The world developer owns `com.yearofice.worldgen` (generation, stages 1–7), `com.yearofice.world` (runtime world state: `world.change`, `world.pass`, `world.catchup`, attachments, stands, floods, drift) and `client.horizon`, as the lead engineer's package map sets. Plant blocks (`forage`) belong to Body and survival and the Environment field (`env`) to World and story; this section places plants at generation and runs their passes in catch-up, but does not own their rules.

**Packages from other tabs.** The lead's mapping holds: the Land tab's `IceWorks`, `SlopeHazards` and `ForestStand` move to `world.land`, while `MicroClimate`, `GroundState` and `PhenomenaDirector` go to `env`, `SnowWorks` to `build.snow` and `FungiFlush` to `forage`. The Video tab's `world.season`, `world.ice`, `world.logs` and `world.water` are kept as subpackages of `world`; its `world.gen.CoalSeams` becomes `worldgen.features.CoalSeams`, since it runs at generation.

**Plants live in `com.yearofice.forage` (RECONCILE 16).** The Plants and fungi tab and the Video tab write `com.yearofice.food.forage`; the lead's map makes it `forage`, so those classes read as `forage.PlantBlock`, `forage.BerryBushBlock` and so on, with the Plants tab's class names. Its `land.CoverPass` reads as `forage.CoverPass`, which submits regrowth ops to World edits.

**Decision: the queue is split pure and adapter.** The Water tab puts `TerrainChangeQueue` (a SavedData) in `com.yearofice.sim`, which may not import Minecraft. The pure planner and records stay there as `sim.terrain.HydroPlanner`, `sim.terrain.ChangeOp` and `sim.terrain.OpRoll`. The record `ChangeOp` is shared in `api`; the SavedData and the block writer become `world.change.TerrainChangeQueue` and `world.change.ChangeApplier`, as the lead's map says.

| Package | Holds | Side |
| --- | --- | --- |
| `worldgen` | `YoiChunkGenerator`, `YoiBiomeSource`, `RingRouter`, `Layers`, the stage classes, `LayerCache`, `LayerTask` | Server only |
| `worldgen.interior`, `worldgen.beyond` | The plan, district layers, province plans and edge contracts (World map) | Server only |
| `worldgen.features`, `worldgen.trees`, `worldgen.structure` | Layer-gated features, tree builders, set-piece structures, processors | Server only |
| `world.change` | The World edits layer: `WorldEditService`, queue, applier, mask, caps | Server only |
| `world.catchup` | `CatchUp`, `LoadGate`, pre-planned diffs | Server only |
| `world.pass` | Derived-state passes: snow, ice, flood water, plant phase, lily pads, leaf litter, trail wear | Server only |
| `world.attach` | Every chunk attachment type, its codec and its payload | Both (codecs) |
| `world.land`, `world.season`, `world.ice`, `world.water`, `world.logs` | Stands, ice works, slope hazards, leaf stages, lily pads, logs and drift | Server only |
| `sim.terrain`, `sim.snow`, `sim.cover` | Pure planners and formulas, no Minecraft imports | Pure |

### The interlace

**The world reads and writes the shared layers by name.** Architecture and the interlace defines each layer; the world reaches them only through `YoiApi.<layer>()`, and this table says what the world side does with each.

| Shared layer | The world reads | The world writes |
| --- | --- | --- |
| Clock and calendar | `ClockEvent.Day`, `.Hour`, `.Jump` to run passes; the five change ticks, which `world.change.ChangeTicks` derives: Breakup and Freeze-up from `IceChangedEvent`, Storm day from `WeatherChangedEvent`, Midsummer and Winter steps from `ClockEvent.Day` | Nothing; passes never move time |
| Environment field | Today's snow depth per class, ice per water body, flood stage, air temperature by micro-climate class, wind, degree-days, season stage | The column data it samples: snow class, micro-climate class, shelter, cold-air offset, permafrost, water body id (at generation, then edits) |
| Scent and sound field | Nothing at generation | Sound sources for collapses, shove, dam failure, rockfall and falls (positions and loudness); scent points for flood-drowned carcasses |
| Ecology | Populations, beaver colonies, browse, trampling and animal-made sign | Habitat masks and capacity from cover; POI markers for water features, dens, lodges and licks; stand age and patch health that capacity reads |
| Decay and freshness | `decay_marks` stages, deadwood classes, snag fall, gut pile to bone pile timers | Nothing; Decay submits `DECAY` and `FALL` ops to World edits |
| Item model | `placed_by` on block entities; drift rules | Driftwood piles, `lost_gear` containers, flood-moved stacks, loot tables for set-piece containers |
| Body | Nothing | Nothing directly; the Body reads the Environment field, which reads world columns |
| Messenger and HUD bus | Nothing | Region and district cards on entry, `bank_retreat`, `dam_full`, wildfire and breakup lines, the "Leaving the charted Interior" line |
| World edits | It is this layer: one queue, one applier, one mask | Every lasting block change after generation |
| Sound events and the audio bus | Ambience per biome, river loudness by width | `SoundBus.play` for edits seen in view (collapses, shove, rockfall, dam failure), which also emits their noise |

**One sampling call.** Every gameplay system reads ground through `worldgen.Layers.at(level, pos)` or the chunk attachments, never through the Minecraft biome. The same call answers in all three rings, as World map › Map layers requires.

### The generator: YoiChunkGenerator

**Registration.** `YoiChunkGenerator extends ChunkGenerator`, registered as `yearofice:valley` with its `MapCodec` in a `DeferredRegister` on `Registries.CHUNK_GENERATOR`. Its codec holds the biome source and a `GenSettings` record. `GenSettings` is the world-creation options (World creation, below), serialised into `level.dat` with the dimension, so a dedicated server reads the same values.

**What each method does.**

| Method | Our implementation | Cost per chunk |
| --- | --- | --- |
| `fillFromNoise` | Builds every column from the layers: deep ground, rock shell, soil, surface, water, footprint flattening. Solid sections are written as single-value containers, never block by block | Under 1.5 ms |
| `buildSurface` | Lays surface blocks by cover and soil (moss mat, duff, turf, gravel, sand, mud, peat), the rust stain below iron, and 1/8 layers where the 2 m field asks | Under 0.5 ms |
| `applyCarvers` | Empty. No caves, no ravines | 0 |
| `createStructures` | Vanilla's loop, but only structure sets in the `yearofice` namespace are offered; any other set is skipped | Under 0.1 ms |
| `applyBiomeDecoration` | Runs our placed features only, in step order, then the layer features (trees, plants, nodes, water markers) from the stage lists | Under 3 ms |
| `spawnOriginalMobs` | Empty. Animals come from populations (Ecology) | 0 |
| `getBaseHeight`, `getBaseColumn` | Read the surface and water height straight from the layers, so structure placement and heightmaps never run noise | Under 2 µs a column |
| `getSeaLevel` | Returns 100, the Grey's mouth. Nothing in our code reads it; it only keeps vanilla's spawn and fish logic sane | 0 |
| `getMinY`, `getGenDepth` | 64 and 1,968, from the dimension type | 0 |
| `addDebugScreenInfo` | F3 lines: ring, region or district code, cover, soil, snow class, micro-climate, water body, current, permafrost, generator version | Client only |

**Threads.** Minecraft calls `fillFromNoise` and decoration on its world-generation workers. Layers come from `LayerCache`. A request for a region or district not yet built joins its `LayerTask` future on that worker, never on the main thread, as the engine's streaming rule says.

**Heightmaps.** `fillFromNoise` primes `WORLD_SURFACE_WG` and `OCEAN_FLOOR_WG` from the layers in one pass with `Heightmap.primeHeightmaps`, so no later step scans columns.

**Single-value sections.** A section entirely below the lowest surface minus 3 blocks is replaced by a new `LevelChunkSection` whose block container holds only `deep_ground` (or `deep_rock` under rock cells). A section entirely above the highest surface stays air. Under the valley floor at y 150, 4 of the 5 sections are single-valued, so the 1,968-block height costs memory only near the surface.

### Density functions or our height field

**Decision: our own height field, no density functions.** We do not extend `NoiseBasedChunkGenerator` and ship no `noise_settings`. Reasons, in order:

1. **Determinism.** Vanilla's noise router uses `Math` functions the engine bans in worldgen. Our layers use integer hashing and the allowed operations, so every JVM gives the same land.
2. **The layers are already the truth.** Stages 2–4 produce height, water and rock for gameplay (the messenger map, travel cost, animal trails, the 2 km test). A second 3D noise would disagree with them.
3. **No overhangs needed.** Pillar 3 forbids caves, and the world's only overhangs are authored (`rock_overhang`, ice caves, adits). A 2D height field with templates covers them.
4. **Speed.** A column read from layers costs a bilinear blend and a few lookups; a density function costs dozens of noise samples per block.

**What we keep from vanilla.** The chunk status pipeline, structure starts and references, `StructureTemplate` placement, heightmaps, the light engine, `ProtoChunk` and region-file saving. Mods that query `ChunkGenerator.getBaseHeight` still get correct answers.

### YoiBiomeSource and the biome list

**Registration.** `YoiBiomeSource extends BiomeSource`, type `yearofice:valley` on `Registries.BIOME_SOURCE`. `getNoiseBiome(qx, qy, qz, sampler)` ignores y and the climate sampler. It reads the cover class at the quart's centre (4 m) from `Layers` and maps it to a biome holder through a 256-entry array, so the call is lock-free and constant time. `collectPossibleBiomes` returns the 20 biomes below.

**Biomes set only the look.** Each biome JSON in `data/yearofice/worldgen/biome/` sets tint, fog, water colour, ambience and mood sound. Gameplay reads our cover cell. Every biome has `has_precipitation` false, `temperature` 0.8, empty `features` except our own, empty `carvers` and empty `spawners`.

**Why temperature 0.8.** Vanilla's precipitation tick freezes water and lays snow in any loaded chunk whose biome is cold, independent of `randomTickSpeed`. A warm biome value turns that off; our colour handlers override vanilla's temperature-based grass and foliage tints, so 0.8 is invisible.

| Biome id | Cover classes mapped to it | Ambience loop |
| --- | --- | --- |
| `yearofice:open_river` | River cells | `yearofice:ambient.river_wide` by width |
| `yearofice:lake` | Lake and pond cells | `ambient.lake_lap` |
| `yearofice:gravel_bar` | Bars, braid islands | `ambient.bar_wind` |
| `yearofice:riparian` | Willow, alder, poplar banks | `ambient.riparian` |
| `yearofice:spruce_taiga` | White spruce | `ambient.taiga` |
| `yearofice:black_spruce_flats` | Stunted spruce on permafrost | `ambient.flats` |
| `yearofice:muskeg` | Bog, sphagnum | `ambient.bog` |
| `yearofice:cloudberry_muskeg` | Cloudberry bog | `ambient.bog` |
| `yearofice:slough_country` | Sloughs, sedge, oxbows | `ambient.slough` |
| `yearofice:birch_aspen_slope` | South slopes | `ambient.birch` |
| `yearofice:foothill_benches` | Bench birch and burns at 300–900 m | `ambient.birch` |
| `yearofice:lichen_woodland` | Open spruce on lichen | `ambient.taiga_open` |
| `yearofice:burn` | Any burn by age (tint by stand age) | `ambient.burn` |
| `yearofice:alpine_tundra` | Above treeline, home valley | `ambient.tundra_wind` |
| `yearofice:upland_barrens` | Barrens and domes | `ambient.tundra_wind` |
| `yearofice:rock_scree` | Faces and scree | `ambient.rock_wind` |
| `yearofice:alpine_range` | Range above 1,000 m | `ambient.high_wind` |
| `yearofice:icefield` | Glacier and moraine | `ambient.glacier` |
| `yearofice:hot_spring_basin` | Basins round springs | `ambient.steam` |
| `yearofice:tailings` | Old mining ground | `ambient.tailings` |

The Land tab's biome modifier for pingos targets `yearofice:slough_country` and `yearofice:cloudberry_muskeg`, which are both above.

### The three rings in the generator

**One router, three providers.** `RingRouter.ring(x, z)` picks the provider. Each provider answers the same `LayerView` interface (height, hardness, water, cover, soil, snow class, micro-climate, permafrost, places), so the column builder never branches on ring.

| Ring | Provider | Layer unit | Cell | Built when | Kept |
| --- | --- | --- | --- | --- | --- |
| 1 Home valley | `RegionLayers` | Region, 2,048 blocks | 2 m | Core at creation; others within 600 blocks | LRU in memory; disk cache |
| 2 Interior | `DistrictLayers` over `InteriorPlan` | District, 4,096 blocks | 4 m | Within 800 blocks, 800 blocks ahead in the travel direction | Same LRU; plan always loaded |
| 3 Beyond | `DistrictLayers` over `ProvincePlan` | District | 4 m | Province plan within 16 km; districts as ring 2 | Same LRU; 2 province plans |

**Blending.** Ring 1 regions blend to the 32 m coarse field over their last 64 blocks. Ring 2 and 3 districts blend to the plan's 128 m height over their last 128 blocks. Rivers cross every edge within 16 m of their contract point, so the column builder sees no seam. The seam GameTest is World map test 3.

**4 m cells at 1 m columns.** District layers interpolate height bilinearly to the block, as in ring 1, plus the World map tab's ±0.5 m noise on soil cover only. Rivers and lake shores are re-traced at 1 m from the 4 m flow field, so a 6 m creek is 6 blocks wide, not 4 or 8.

**LayerTask scheduling.** A `LayerScheduler` runs every 20 ticks. It projects each player 800 blocks along their mean velocity of the last 30 s and queues any unbuilt unit within 600 blocks of the player or 300 blocks of the projected point. Tasks run on the lead's `yearofice-gen` pool, never on Minecraft's chunk workers, so a layer build cannot starve chunk saving.

### The nine stages, built

**Each stage has one class, one store and one test.** The table extends the engine's; stage names stay the hashed `stageName` strings of Pipeline at a glance.

| # | Stage | Class | Writes (per region or district) | Bytes in memory | Store | Budget | First test |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Seed | `worldgen.SeedStage` | The frozen parameters record (below) | About 4 KB | SavedData `yearofice_world` | Under 1 ms | JUnit `worldgen/seed_params_ranges` |
| 2 | Landform | `worldgen.Landform` | Height (float32), hardness (u8); the 32 m coarse field once | 5 MB | Layer cache | 0.3 s | `worldgen/height_hash_jvms` |
| 3 | Erosion | `worldgen.Erosion` | Eroded height (replaces), sediment depth (u8, 0.1 m) | 1 MB | Layer cache | 2–3 s | `worldgen/erosion_mass_2pct` |
| 4 | Water and rock | `worldgen.Hydrology` | Water type (u8), surface (int16 dm), flow dir and speed (u8 each), body id (u16), flood lines (u8), carve (u8); falls, pools, spots, springs, faces, bodies | 7 MB | Layer cache; body list in `yearofice_water` | 0.5 s | `worldgen/water_never_rises` |
| 5 | Places | `worldgen.Places` | Set pieces, natural features, landmarks, caches, licks, lines; footprint masks (bitset) | Under 0.5 MB | Layer cache; starts in region SavedData | 0.3 s | `worldgen/places_fit_and_spacing` |
| 6 | Cover | `worldgen.Cover` | Cover class, soil class, snow class (2 bits), micro-climate class, permafrost and active layer; tree, plant-patch and node lists | 4 MB | Layer cache | 0.8 s | `worldgen/cover_rules` |
| 7 | Life and start | `worldgen.Life`, `worldgen.StartSite` | Ranges, trails, dens, lodges, ponds; the start record (core only) | Under 0.5 MB | Region SavedData `yearofice_region_<key>` | 0.4 s | `worldgen/start_checks_50_seeds` |
| 8 | Weather | `story.WeatherYear` (Environment field) | The year table | 12 KB a year | Rebuilt, never saved | 20 ms a year | `story/ice_dates` |
| 9 | Edits | `world.change.*`, `world.pass.*`, `world.catchup.*` | Block changes after generation | — | Chunks and attachments | Under 2 ms a chunk on load | `world/catchup_equals_live` |

**Layer cache on disk.** Stages 2–6 for a unit are written to `<world>/yearofice/layers/<code>-v<gen>.bin`, one LZ4 frame per layer, about 3 MB per region. A missing or stale file is rebuilt from the seed; the file name carries the generator version so a version bump never reads old layers.

**What is stored and what is rebuilt.** Layers are a cache. The parameters record (with the start site, the outpost bunk and the SOS return position), the region SavedData (ranges, dens, lodges, set-piece starts), the water bodies in `yearofice_water` and everything in World edits are save data, because play changes them.

### Terrain to blocks

**The column builder.** `worldgen.ColumnBuilder.build(view, x, z, column)` runs once per column in `fillFromNoise`, top-down in its result but bottom-up in writing.

1. Read the blended surface height h and round to block y\_s.
2. If a footprint mask covers the column, set y\_s to the footprint's floor y and mark the column flattened.
3. Read cover, soil, hardness and permafrost.
4. Rock cell (slope over 40°, hardness over 0.6): `deep_rock` up to y\_s. A second pass over the chunk turns every rock block whose top or side touches air or water into `face_rock`, or `iron_outcrop` where the node list says. Done.
5. Otherwise: deep ground up to y\_s − d, where d is 2 soil blocks, or 1 on permafrost.
6. Soil blocks by soil class for d blocks; on permafrost `frozen_soil` replaces the lower one.
7. Leave y\_s for `buildSurface`, which writes the surface block by cover.
8. Water column: if the water surface w is above y\_s, fill water source blocks from y\_s + 1 to w, after the bed carve.

**Block palette.**

| Soil class | Soil block (2 down) | Surface blocks by cover |
| --- | --- | --- |
| Silt, loess | `yearofice:silt` | `grass_turf` (meadow, riparian), `forest_duff` (taiga), `moss_mat` (shade) |
| Sand | `yearofice:river_sand` | `river_sand`, `lichen_mat` (lichen woodland) |
| Gravel | `yearofice:gravel` | `gravel` (bars), `scree_stones` (talus) |
| Peat | `yearofice:peat` | `sphagnum` mat over `peat` (muskeg), `tussock_turf` (barrens) |
| Till | `yearofice:till` | `tundra_turf`, `moss_mat` |
| Clay | `yearofice:clay_bank` on cut banks and licks | Lick crust `yearofice:salt_crust` |
| Tailings | `yearofice:tailings_gravel` | Bare, or `fireweed` patches |

**Decision: no vanilla terrain blocks.** Every ground block is ours. Vanilla sand and gravel fall under gravity, which an undercut bank would set off in a cascade; vanilla dirt and grass have spread rules and loot we do not want. Ours never fall, carry our loot tables, and sit in the tags the rules read (`yearofice:terrain_steps`, `yearofice:freezing_soils`, `yearofice:erodible_bank`, `yearofice:diggable_soil`, `yearofice:rough_ground`).

**Decision: the terrain step tag (RECONCILE 37).** `yearofice:terrain_steps` holds every natural surface, soil, snow, ice and plant-mat block above: the 1.0 step height applies on them. Placed pieces, rock faces and logs are not in it, so the walking stick still helps on rock and over deadfall.

**Surface 1/8 layers.** Where the 2 m field's true surface sits more than 0.25 block above y\_s on gravel, sand, mud or snow, `buildSurface` adds a layer block (`gravel_layer`, `sand_layer`, `mud_layer`) with layers 1–7. The M2 frame test keeps or drops them, as the engine says; the column builder reads one flag in `GenSettings`.

**Rock shell and deep rock.** `face_rock` is the only breakable rock and exists only where a rock column's side or top touches air or water. `deep_rock` and `deep_ground` have destroy time −1, no loot table and blast resistance 3,600,000. `iron_outcrop` and `coal_seam` are placed only on the shell (`yearofice:shell_only` placement modifier); `flint_nodule` lies loose on gravel bars.

**Decision: the dig exceptions on the world side (RECONCILE 12 and 20).** Deep ground breaks only inside a dig permit: a validated dugout, root cellar (permafrost cellar) or chimney footprint, at or above the mouth floor, with no drops. Permits live in the region SavedData as boxes written by the Building and crafting department's piece validation. `PlayerEvent.BreakSpeed` reads them; the block's loot table stays empty, so even a permitted break drops nothing.

### Rivers, current and water

**Water placement never spills.** `ColumnBuilder` places a water source only where every horizontal neighbour at that y is water, solid, or the riffle lip one block down. A riffle step leaves one flowing-water lip block, which is the look the engine wants. Falls are flowing columns from the lip to the pool. Test `worldgen/no_spill` loads a 512 × 512 river region and asserts no fluid tick outside falls and lips after 200 ticks.

**The current attachment.** `yearofice:current` is stored only on chunks with water. Its layout is a 256-bit presence mask, then one 6-byte record per water column.

| Bytes | Field | Unit |
| --- | --- | --- |
| 1 | Flow direction | 64 steps; 255 is still water |
| 1 | Speed V | 0.05 m/s steps, 0–3.0 |
| 1 | Flags | Thin ice, spring, lead, falls pool, ford, riffle, silty, sweeper reach |
| 2 | Water body id | Index into the region's body list |
| 1 | Flood lines | Normal high water (high nibble) and flood-year line (low nibble), in 0.25 m above the surface |

**Synced to the client.** On `ChunkWatchEvent.Sent` the server sends `yearofice:current_chunk` with the mask, direction, speed and flags: at most 800 bytes. Body ids and flood lines stay on the server. The push itself is in Minecraft engine › Rivers, current and water.

**The water body list.** Stage 4 writes each body (id, kind: river reach, lake, pond, slough, spring run; area, mean depth, outlet, the ice model's inputs) into `yearofice_water`, owned by the Environment field's `env.water`. Today's ice per body is in `yearofice_ice`. A beaver pond or thaw pond made in play is registered there with a new id when World edits creates it. The Water tab's water-feature registry, which it also calls `yearofice_water`, is renamed `yearofice_water_features` so the two never collide.

**Ice on blocks.** `world.pass.IcePass` sets the top water block of each column to the body's ice state: `skim_ice`, `river_ice` with `stage=thin|safe|rotten`, or water. Columns flagged thin, spring or lead take the next weaker state, so the thin-spot rule is per column. Ice is one block thick on screen whatever its true thickness; thickness lives in the body record.

**River ice under snow.** `snow_cover` sits on `river_ice` like on ground. Overflow days (Calendar, daylight and weather) set `wet=true` on the snow cover above flagged columns, which the wet-feet rule reads and the texture shows as grey slush.

**Floods use the flood flags.** `FloodController` raises water only on the flood mask, as the Water tab says. Each column it raises stores the count of placed layers in `yearofice:flood_flags`, so the fall removes exactly what it added. The mask is built so every edge cell borders ground at or above the water, so placed water can never flow out.

### Snow cover and permafrost blocks

**One snow block.** `yearofice:snow_cover` has `layers` 1–8 (12.5 cm each), `crust` true or false and `wet` true or false. It has no block entity. Collision is 1 layer for powder and full height for crust, as the engine says.

**Over 1 m.** Snow deeper than 8 layers stacks a second `snow_cover` on a full first block. Lee drifts can reach 3 blocks. `snow_block` (Land tab) is the cut, placed building block, never generated.

**Decision: roof snow is snow\_cover too.** The Structures tab says roof snow uses vanilla snow layers. It uses `snow_cover` instead, with the same 12.5 cm layers, so the roof-load rule, the shed rule and the snow pass share one block. Vanilla `minecraft:snow` is hidden and banned.

**The snow pass.** `world.pass.SnowPass` runs after each snow, melt or settle step from the Environment field. For each column it reads the snow class from `yearofice:season_stamp` and sets the layer count to the day's depth for that class, rounded to the layer, with the micro-climate offset applied to melt. It writes only columns whose count changed, nearest chunks first, at most 6 chunks a tick (engine budget).

| Snow class | Depth rule | Where |
| --- | --- | --- |
| Open | The day's depth | Meadows, bars, lake ice, clearings |
| Under spruce | ×0.6, starts 1 snowfall late | Within 2 blocks of a spruce trunk; read from the tree list at generation |
| South slope | Melts ×1.5 from March | Aspect class at generation |
| Lee drift | ×1.8 after wind days | Downwind of ridges and wall pieces; recomputed when a wall piece is placed |

**Dug and packed snow.** The Environment field reads snow depth from the model, except where a player dug, packed or built (Architecture › Layer 2). Such a column is recorded in `season_stamp` with the day and the depth left; the pass then lays only snow fallen since that day on top, so a shovelled path stays shovelled until the next snowfall and a packed trail keeps its crust.

**Snow on plants.** A plant block at or under the snow surface gets `phase=buried` from the plant pass, and the snow block sits above it. Plant blocks are never deleted by snow.

**Permafrost.** Stage 6 writes a permafrost bit and an active-layer depth (0.4–1.5 m) per cell. On those columns the lower soil block is `yearofice:frozen_soil`: diggable only with a pick at a quarter speed, yields nothing useful, and is never in `diggable_soil`. Black spruce, `tussock_turf` and drunken trees (a tree builder lean of 5–20°) mark it on the surface.

**Ground ice.** `yearofice:ground_ice` appears only where a thaw slump or cut bank exposes it (World edits op `EXPOSE`). It is the clear ice the Water tab's slump headwalls give.

**Frozen ground.** From freeze-up to breakup the Environment field sets a level-wide `ground_frozen` flag. `BreakSpeed` sets soil blocks in `yearofice:freezing_soils` to 0 and the prompt says "Frozen ground." No block changes, as the engine says.

### Set pieces and Interior structures as templates

**Three structure types, one placement.** Every authored place goes through Minecraft's structure pipeline, but the Places stage decides where.

| Structure type | Registry | Used for | Rings |
| --- | --- | --- | --- |
| `yearofice:setpiece` | `Registries.STRUCTURE_TYPE` | Story set pieces with notes, loot and a fit rule | 1, 2 |
| `yearofice:natural_feature` | Same | Licks, beaver valleys, cold-trap caves, hot spring pools, ice caves without objects | 1, 2, 3 |
| `yearofice:landmark` | Same | Tors, the Antler Tors, the Sand Cliffs, cascades, burn ridges | 1, 2, 3 |

**The placement.** `yearofice:places_layer` is a `StructurePlacementType`. Its `isStructureChunk` asks the places layer whether a start of that structure set lands in the chunk, so no vanilla spacing, separation or salt is used. One structure set per type, in `data/yearofice/worldgen/structure_set/`, lists every structure of that type.

**One start, many pieces.** `SetPieceStructure.findGenerationPoint` reads the place record: id, variant, origin, rotation, mirror and, for multi-building places, the piece list. Each piece is a `TemplatePiece` (`yearofice:template_piece` on `Registries.STRUCTURE_PIECE`) holding one template. Marrow City is one start with 8–20 pieces laid out by `worldgen.places.TownLayout`; jigsaw pools are not used, because the layout must follow creek flats and fairness rules, not connectors.

**Fit rules stay data.** Each set piece's `data/yearofice/setpiece/<id>.json` keeps the blueprint's fit rule, count per ring, spacing and priority. The Places stage evaluates them on layers; the structure type only stamps.

| Field | Meaning | Example |
| --- | --- | --- |
| `ring` | Rings it may appear in | `[1]` for the bush-plane wreck, `[2]` for Marrow City |
| `count` | Per home valley or per Interior | `[1, 2]` |
| `fit` | Layer predicates: cover, slope, elevation, distance to water, aspect, district type | `{ "cover": ["spruce_taiga"], "creek_bench": true, "min_from_river_m": 200 }` |
| `spacing_m` | From any other set piece | 1000 |
| `priority` | Placement order | Wreck 90, lookout 80, cabin 70 |
| `variants` | Template ids and weights | `outpost/day_one`, `cabin/sod_roof` |
| `landmark` | Whether it counts for the 2 km or 4 km test, and its silhouette model | `{ "test_km": 2, "silhouette": "yearofice:silhouette/lookout" }` |
| `abandoned_year` | The year the place was left, for the age-world processor (Decay and time) | 1971 for Arvid's cabin |

**Template rules.** Templates are authored in a creative test world and saved with structure blocks to `data/yearofice/structure/setpiece/<id>/<variant>.nbt`.

- Footprints of at most 48 × 48 blocks per piece and 32 blocks tall; a lookout tower is one 9 × 9 × 18 piece.
- No air blocks saved outside the building, so terrain shows through ("ignore air" on save).
- Containers name loot tables by a data marker (`loot:setpiece/arvid_cabin`); notes by `note:<id>`, placed as `yearofice:note` in the container or on a `note_spot`.
- Jigsaw blocks are banned; a data validation test fails any template that holds one.

**Processors.** Each set piece has a processor list in `data/yearofice/worldgen/processor_list/<id>.json`.

| Processor | Type id | Does |
| --- | --- | --- |
| Snap to ground | `yearofice:snap_to_ground` | Drops foundation blocks to the flattened floor; fills gaps below with crib or stone, never deep ground |
| Age world | `yearofice:age_world` | Calls the Decay layer's `sim.decay.AgeWorld` from `abandoned_year` to `present_year`: wood stages, caved sod, rusted iron, by the Decay tab's past-decay table. Until M9 it only sets the hand-aged templates' stamps |
| Loot seed | `yearofice:loot_seed` | Sets each container's loot table seed from `hash(seed, "loot", place id, block pos)`, so a reroll of the chunk gives the same loot |
| Mask | `yearofice:mark_placed` | Sets the `placed_mask` bit for every block it places, so World edits never erode a set piece |
| Season blank | `yearofice:season_blank` | Strips any snow, ice or plant phase saved into the template; the first settle pass adds today's |

**Footprints and lines.** Stage 5's footprint mask is read in `fillFromNoise` (step 2 of the column builder), so the ground is flat before the template lands. Linear places (the Marrow Trail mileposts every 1.6 km, the Signal Line poles every 60 m, the cut trail line) are a `yearofice:line_feature` placed feature that reads the line's polyline from the places layer, not structures.

**Interior structures.** Each Interior set piece of World map › Set pieces of the Interior is a `setpiece` with `ring: [2]`.

| Set piece | Template ids | Pieces | Footprint |
| --- | --- | --- | --- |
| Grey River Landing | `landing/warehouse`, `landing/winch`, `landing/store` | 3 | 40 × 30 |
| Forks Roadhouse | `forks_roadhouse/ruin`, `/stable`, `/cellar` | 3 | 28 × 22 |
| J.O.'s signal camp | `signal_camp/bar` | 1 | 12 × 12 |
| Halfway Roadhouse and hot springs | `halfway/bunkhouse`, `/bathhouse`, `/rhubarb_bed` | 3 | 34 × 26 |
| Marrow City | `marrow/assay`, `/smithy`, `/hotel`, `/store`, `/cabin_a`–`cabin_f`, `/cemetery`, `/dredge` | 8–20 | Up to 220 × 160 |
| Prospect adit | `adit/headframe`, `adit/caved` | 1 | 10 × 16 into the face |
| Fire lookout (Interior) | `lookout/interior_tower`, `/ground_cab` | 1 | 9 × 9 |
| Arvid's outer line cabin | `line_cabin/a`, `/b` | 1 | 6 × 6 |
| Signal Line relay cabin | `relay_cabin/a` | 1 | 8 × 8 |
| Fish wheel ruin | `fish_wheel_ruin/a` | 1 | 10 × 8 on the bank |
| Float-plane dock | `float_dock/a` | 1 | 6 × 18 over water |

**Natural features and landmarks.** These have no notes and no loot. Beyond, they appear at Interior densities from the province plan, and the "No story Beyond" test (World map test 4) checks no setpiece start ever lands there.

### Seed variables: how each is computed and stored

**Three kinds of seeded value.** Frozen at creation, drawn each year, or rolled per event. None is ever rerolled on reload.

| Kind | Drawn from | Stored | Rebuilt |
| --- | --- | --- | --- |
| World parameters | `hash(attemptSeed, "seed", name)` for the home valley; `hash(attemptSeed, "interior", name)` for the Interior plan | Scalars in `yearofice_world.params` | Never; geometry (river polylines, glacier shapes) is recomputed from the scalars |
| Year profile | `hash(seed, "year", n)` at 00:00 May 1 | Not saved, as the engine says; only the year number and carried anomaly | On load, from seed, n, carried anomaly and saved history (kills, fish-wheel takes) |
| Event rolls | `hash(seed, system, day, ...)` | The result is history: an op, a catch or a population change | Never rerolled |

**Our seed.** `seed = hash(levelSeed, "yearofice")` with the FNV-1a `StageHash`. `attemptSeed = hash(seed, k)` for the attempt k that passed; k is stored. The level seed is Minecraft's, so `/seed` and the seed box work as players expect.

**The parameters record.** Every World map variable is a named field, with its draw and its range in `data/yearofice/interior/plan.json` (frozen into the world at creation). The record also stores each region's and district's layer hash once built, so a later build that differs is caught.

| Variable | Draw | Stored as | Read by |
| --- | --- | --- | --- |
| Valley archetype | Equal odds of 4 | Enum | Landform, Cover, Messenger first-month lines |
| Mirror | 50% | Bool | All stages; the Interior mirrors with it |
| Home river entry and exit edges, virtual catchment | Stage 1 | Edge codes, km² | Hydrology, the Interior plan's Ninemile |
| Hare phase | 0–9 | Int | Ecology |
| Region roles | Stage 1 rules | 16 enums | Landform, Places |
| Grey River source, path offset, Forks distance | Equal odds; uniform; uniform | Enum, km, km | `MacroRivers` |
| Braid belt width, channels | From glacier area | km, int | `MacroRivers`, district Hydrology |
| Range peak height, passes, glaciers, termini | Uniform; placed at lowest saddles; log-normal area | m; list of plan cells; list | Plan, horizon, Environment field (glacier wind, chinook doors) |
| Ice-dammed lake, glacier surge | 60%; 1 in 3 | Bool and glacier id | Environment field event rolls |
| Dome height, treeline | Uniform | m | Cover |
| Canyon place | 70/30 | Enum | Plan |
| Lake count and big lakes | Poisson on basins | Counts; lake ids in the plan | Plan |
| Permafrost fraction | Uniform 0.3–0.6 | Float | Cover, per cell by aspect |
| Tributaries and their runs, Ninemile shares | Placed on spring creeks; uniform | List with species; floats | Ecology (fish stocks) |
| Hot springs, burns, mining district, ghost camp size | As World map | Lists; ints | Places, Cover, stand age |
| Marrow Trail, Signal Line, lookouts, Arvid's outer line | Least-cost paths and fits | Plan-cell polylines' seeds and endpoints | Places, lines |
| Caribou corridor, herd size | One of 3; uniform | Enum, int | Ecology |
| Hare wave lag and direction | Uniform | Years, degrees | Ecology |
| Wolf packs and territories | Voronoi seeds on prey habitat | Seed points | Ecology |
| Interior sub-seed j | Fairness retries 1–10 | Int | Plan |

**Frozen data.** `region/`, `archetype/`, `district_type/`, `district/`, `setpiece/`, `natural_feature/`, `interior/plan.json` and `worldgen_params/valley.json` are copied into `yearofice_world` at creation, as the engine's "worldgen data is frozen" rule says. A `/reload` changes plants, species and recipes, never land.

**Decision: snake\_case in worldgen data (RECONCILE 6).** The World map tab's JSON examples (`elevationM`, `densityPerKm2`, `greyRiver`) and the Land tab's (`airBelowC`, `radiusM`) are read as `elevation_m`, `density_per_km2`, `grey_river`, `air_below_c`, `radius_m`. The codecs accept only snake\_case.

### World creation options and presets

**The world preset.** `data/yearofice/worldgen/world_preset/year_of_ice.json` defines only the overworld: dimension type `yearofice:valley`, generator `yearofice:valley` with biome source `yearofice:valley`. Our data pack replaces `data/minecraft/tags/worldgen/world_preset/normal.json` with `replace: true` listing only ours, and a client listener on `ScreenEvent.Init.Post` selects it by default in the Create World screen.

**The dimension type.** `data/yearofice/dimension_type/valley.json`: `min_y` 64, `height` 1,968, `logical_height` 1,968, `has_skylight` true, `has_ceiling` false, `natural` true, `ambient_light` 0, `bed_works` false, `respawn_anchor_works` false, `has_raids` false, `piglin_safe` false, `monster_spawn_light_level` 0, `monster_spawn_block_light_limit` 0, `coordinate_scale` 1, `effects` `yearofice:valley` (sky renderer, registered with `RegisterDimensionSpecialEffectsEvent`).

**Options the player sets.** The "Customize" button opens our screen, registered with `RegisterPresetEditorsEvent`. Its values fill `GenSettings` in the generator codec, so they are saved with the dimension and copied into `yearofice_world`.

| Option | Values | Default | Who sees it |
| --- | --- | --- | --- |
| Realism preset | Gentle, Standard, Brutal | Standard | Everyone |
| Mode | Endless, Hardcore (vanilla's Hardcore toggle maps to it); locked for the world once it is created (Ruhan, 2026-10-19) | Endless | Everyone |
| Day length | Player-set: 24, 30, 36, 40, 45, 48, 60 or 72 real minutes; only the World rate scales (RECONCILE 145) | 24 | Everyone; saved in yearofice\_world.day\_length\_min, rates per game minute (RECONCILE 117) |
| Other dimensions | Off, On (`yearofice:other_dimensions`) | Off | Everyone |
| Surface layers | On, Off | By the M2 frame test | Everyone |
| Force archetype | Random, or one of 4 | Random | Dev builds only |
| Force mirror | Random, Off, On | Random | Dev builds only |
| Development border | Off, On (`yearofice:dev_border`) | On until M10 | Dev builds only |
| Layer debug | Off, On (F3 lines and `/yoi layers` command) | Off | Dev builds only |

"Dev builds" means `FMLEnvironment.production` is false or the JVM flag `-Dyearofice.dev=true` is set.

**Game rules at creation.** The world preset carries the lead's full game-rule set (Architecture › Vanilla systems we replace, RECONCILE 44), which `core.rules.GameRuleEnforcer` re-applies on start. The world side adds `spawnChunkRadius` 0 where the pin has it, so no spawn chunks stay loaded.

**Dedicated servers.** `server.properties` uses `level-type=yearofice\:year_of_ice` and `generator-settings` with the same JSON fields as the screen, for example `{"realism":"standard","mode":"endless"}`.

**World toggles (RECONCILE 170).** Two world options from Ruhan's video 2 approval are set per world and, unlike mode, can be changed after creation.

| Option | Key | Default | Where it is set | What it does |
| --- | --- | --- | --- | --- |
| Animals enter open doors | `[world] animals_enter_open_doors` | On in every preset | The Customize screen; later `/yoi world set animals_enter_open_doors on\|off` | Door entry rolls and the `door_open` warning (Structures and buildables › Door left open) |
| Rack thieves | `[world] rack_thieves` | On in every preset | The Customize screen; later `/yoi world set rack_thieves on\|off` | Theft rolls at racks (Structures and buildables › Thieves at the rack) |

The values live in `yearofice_world` with the other world choices; the `[world]` table in `serverconfig/yearofice-server.toml` gives only the defaults offered, as Config files and defaults requires. A change takes effect at the next game hour and is written to the journal. Presets never change them, so GameTests on Standard run with both on. Class `core.rules.WorldToggles`; GameTests `ecology/door_entry_toggle` and `ecology/rack_thieves_toggle`.

**Other presets.** `yearofice:test_flat` (hidden): a flat valley floor at y 150 with no layers beyond a 4-chunk river, for GameTests and template authoring. `yearofice:layer_preview` (dev only): generates layers and draws the debug map to the loading screen without building chunks.

### The dev border

**Only in dev builds.** With `yearofice:dev_border` on, the server sets the world border to 8,192 blocks centred on 0, 0 at creation and on every start, and the messenger shows "Development border" instead of the frontier line. Release builds hide the option, and the `world/default_border` GameTest fails the build if a world made without it has a border under 59,999,968 blocks (World map test 7). The border goes at M10, when the Interior ships.

### Vanilla worldgen switched off

**Everything vanilla could still generate, and how it is stopped.**

| Vanilla thing | Stopped by |
| --- | --- |
| Noise terrain, aquifers, ore veins | Our generator; no `noise_settings` |
| Caves and ravines | `applyCarvers` empty; biomes list no carvers |
| Ores, lakes, springs, geodes, fossils, dungeons, monster rooms | Our biomes list only our placed features; `neoforge:remove_features` biome modifier `data/yearofice/neoforge/biome_modifier/no_foreign_features.json` strips any non-`yearofice` feature another pack adds to our biomes |
| Villages, outposts, monuments, mansions, strongholds, mineshafts, ruined portals, trail ruins, ancient cities | `createStructures` offers only `yearofice` structure sets |
| Vanilla trees, flowers, grass, kelp, snow and freeze-top features | Not in our biomes |
| Precipitation freezing water and laying snow | Biome `temperature` 0.8 and `has_precipitation` false |
| Lightning bolts | No biome reports rain, so `isRainingAt` is false; our thunder is drawn and heard only |
| Mob spawning at generation | `spawnOriginalMobs` empty; no spawners |
| Nether and End | Not in the preset; portal ignition cancelled (engine) |
| Vanilla `lily_pad`, `mud`, `packed_mud`, `mud_bricks`, `snow`, `snow_block`, `ice`, `packed_ice`, `sand`, `gravel`, `dirt`, `grass_block`, logs and leaves | Never placed; hidden from creative tabs and recipe viewers by `BuildCreativeModeTabContentsEvent`, and their recipes filtered (RECONCILE 34). A tag `yearofice:banned_vanilla_blocks` lists them; a data test fails any template or feature that uses one |

### Random ticks and their replacements

**`randomTickSpeed` is 0, so nothing vanilla ticks at random.** Each behaviour we need is driven by the clock instead.

| Behaviour random ticks used to drive | Our replacement | When |
| --- | --- | --- |
| Crop and sapling growth | Crop block entities on `ClockEvent.Day` (Food and hunting department); saplings by the May 1 cover pass | Daily; yearly |
| Leaf decay after felling | The felling code removes the tree's leaves from the tree record at once; no orphan leaves exist | On fell |
| Snow and ice melt and freeze | `SnowPass`, `IcePass` | After each Environment field step |
| Grass and moss spread | The cover pass's `REGROW` ops | May 1 |
| Farmland drying | Garden bed block entity water state | Daily |
| Plant ripening | `PlantPhasePass` | Daily |
| Fire burning out and spreading | Our fire block entity and `EmberSpread` (Fire); wildfire producer | Per game minute; per game hour |
| Vine, kelp, bamboo growth | Not used | — |
| Copper, dripstone, moss blocks, budding blocks | Not used | — |

**Decision: Decay settles by due day, not by random tick.** The Decay and time tab has decaying blocks settle in `randomTick` "at the default random tick speed". With `randomTickSpeed` at 0 that never fires. Instead, `BlockDecay` computes each stamped block's next stage day from the climate prefix sums when it is stamped, and the lead's `decay_marks` attachment keeps each mark's due day. A `ClockEvent.Day` pass settles only chunks whose due day has come, and chunk load settles the rest. The result is the same, with no dependence on chance.

**A guard test.** `world/no_random_ticks`: with `randomTickSpeed` forced to 3 for 1,000 ticks in a test world of every one of our blocks, no block state changes. Our blocks return false from `isRandomlyTicking`, so a player who changes the rule changes nothing.

### World edits: two kinds of change

**Every block change after generation is one of two kinds.** The split decides how it is stored and how it catches up.

| Kind | What it is | Examples | Stored as | Catch-up |
| --- | --- | --- | --- | --- |
| Derived state | A pure function of the date, the year table, the column data and a ledger | Snow layers, ice stage, flood water, plant phase, lily pads, leaf litter, worn-trail stage, `wet` snow | Nothing but the inputs; the attachment `season_stamp` records the last day applied | Settle straight to today in one pass, whatever the gap |
| History | A path-dependent change that a later state cannot be computed without | Bank erosion, bars, cutoffs, gullies, dams, burns, regrowth, snags falling, rot failures, windthrow, avalanches, iron renewal | `ChangeOp` records in the World edits queue until applied; applied ops leave only their blocks and the `edit_log` count | Apply pending ops oldest first, then settle derived state |

**The chunk on any day.**

```latex
\mathrm{chunk}(D) = \mathrm{settle}_D\Big(\mathrm{apply}_{\le D}\big(\mathrm{generate}(seed, v),\ \mathrm{ops}\big),\ \mathrm{ledgers}\Big)
```

generate is the generator at version v, ops are the history ops in canonical order up to day D, and settle is every derived-state pass for day D. A loaded chunk and a chunk loaded after a gap must reach the same blocks; the `world/catchup_equals_live` GameTest checks it.

### The terrain-change queue in full

**One queue for every history producer.** The Water tab's queue is kept and widened: `world.change.TerrainChangeQueue` (SavedData `yearofice_terrain`) holds ops from every producer, not only water. Producers in other packages call `WorldEditService.submit(ChangeOp)`; the pure planners live in `sim.terrain`.

**The op record.** 20 bytes, plus an optional payload for multi-block ops.

| Field | Bytes | Meaning |
| --- | --- | --- |
| Chunk pos | 8 | Ops are keyed per chunk; an op never touches another chunk |
| Game day | 4 | The tick it belongs to |
| Producer and kind | 2 | One of the op kinds below |
| Local column and y | 3 | 4 bits x, 4 bits z, 16 bits y |
| Argument | 2 | Count, block id index, stage or tree index |
| Roll seed | 1 | Index into the op's seeded stream, `hash(seed, "terrain", year, tick, cellX, cellZ, process)` |

**Op kinds.** The Water tab's nine stay as written. These join them, each with the blocks it may touch.

| Op | Producer | Does | Allowed on |
| --- | --- | --- | --- |
| `BURN` | Wildfire, controlled burn | Leaves to air, logs to `charred_log`, surface to `ash_layer` over `burnt_duff`, plants removed, tree record to dead standing | Natural blocks, unmasked |
| `REGROW` | Cover pass (May 1) | Places the next succession step: fireweed, grass, `sapling` stage blocks, a young tree from a tree builder | Air over natural ground, not on worn trail, garden bed, footprint or fire ring cells |
| `TREE_DIE` | Decay (deadwood 0.5% a year), beetles, girdling | Strips leaves, sets the log `dead=true` | The tree's own blocks |
| `FALL` | Decay | A dead standing tree falls along the wind: logs become `fallen_log` on the ground, crown removed | The tree's blocks and air in its fall path; never masked blocks (a fall into a piece becomes piece damage instead) |
| `DAM` | Ecology (beaver colony) | Adds one `beaver_dam` block | Water or air on the dam line |
| `POND` | Ecology, after a dam stage changes | Raises or lowers the pond by one block on the pond mask | Air and water on the mask |
| `BREACH` | The player's breach action, dam failure | Removes 1–3 dam blocks; queues `POND` down | Dam blocks |
| `STRIKE` | Lightning (Environment field) | A spruce becomes `struck_spruce`; may start a fire by RECONCILE 38 | One tree |
| `WINDTHROW` | Land: windthrow | Tree falls with its roots: `root_plate` block at the base, soil hole of 1 block | The tree, one soil block |
| `ROCKFALL` | Land: rockfall | Places `loose_boulder` blocks on scree below a face | Air on scree |
| `AVALANCHE` | Land: avalanches | Places `avalanche_debris` layers in the runout; snaps saplings | Air, snow, plants |
| `EXPOSE` | Floods, breakup, slumps | Adds a new `iron_outcrop` on a cut bank, `ground_ice` on a headwall, an Ice Age bone item, float coal | Bank face blocks |
| `NODE` | Iron renewal | Re-forms a bog iron bed 3–5 years after its last nodule | The bed's pool floor |
| `DRIFT` | Breakup | Places `drift_log` and `driftwood_pile`; turns lost items into `lost_gear` blocks | Air and water over natural ground |
| `THERMOKARST` | Permafrost | A new thaw pond: `SUBSIDE` over a 2–6 cell patch, then water | Natural soil |
| `DECAY` | Decay | A block past its threshold changes stage or falls: a placed piece at 0 HP drops as items by the Building rules; a gut pile becomes a bone pile; a fallen log becomes `punk_log` | Masked blocks, and blocks in `#yearofice:decays`; only this producer |

**The four steps stay.** Plan at the tick, apply loaded chunks within the next game hour at 256 block sets per tick, catch up on load, prune (Water tab). Caps stay: 64 permanent edits per chunk per tick, 8 per storm day for gullies, overflow carried to the next tick.

**Where processes run.** Planning walks only cells whose layers exist and that are "live": the whole home valley, plus every Interior or Beyond district the player has come within 1 km of, the same rule as lazy populations. A district never visited has no ops and no history. This keeps the queue finite in an endless world.

**Merging.** A chunk keeps at most 3 years of pending ops. Older ones collapse to their net result by kind: erosion counters sum, deposits add, a dam's stages net out, a burn followed by several regrowth steps keeps the burn and only the latest step. `sim.terrain.OpMerger` is pure and has its own JUnit test: merged and unmerged queues give the same block hash.

**Seen or unseen.** As the Water tab says, collapses, shove, dam failures, rockfall, windthrow and avalanches may play in view with sound and particles; deposits, regrowth and subsidence wait until no player within 64 blocks faces the cell, for up to one game hour.

**Writing blocks.** Live application uses `level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)`. Catch-up on load writes through the `LevelChunk` directly (below), because calling `level.setBlock` inside a chunk's own load can ask for that chunk again.

### Every other world edit

**Each design rule that changes blocks, and where it runs.**

| World edit | Source tab › section | Kind | Producer or pass | Blocks it writes | Milestone |
| --- | --- | --- | --- | --- | --- |
| Beaver dams growing, ponds rising, drowned trees | Blueprint › The open world; Video › Dams you can watch | History | `ecosystem.species.beaver.DamWork` submits `DAM`, `POND`, `SNAG` | `beaver_dam`, water, `snag_log` | M9 |
| Dam breach by the player | Blueprint › Beaver valley | History | `BREACH` from the hatchet action | Dam blocks, then pond water | M9 |
| Beaver meadow succession | Water › Beavers, ponds and thaw | History | `VEGETATE` at Breakup | `pond_mud`, sedge, willow, spruce | M9 |
| Wildfire and burns | Blueprint › Forest fire; Land › Controlled burns | History | `fire.Wildfire` submits `BURN` on 8 m cells near players and on 32 m cells off-screen | `charred_log`, `ash_layer`, `burnt_duff` | M7 (marks), M9 (spread and regrowth) |
| Lightning | Video › Thunderstorms | History | `STRIKE` | `struck_spruce` | M7 |
| Regrowth after clearing and fire | Blueprint › Regrowth; Plants › The cover pass | History | `forage.CoverPass` on May 1 submits `REGROW` | Plant blocks, saplings, young trees | M9 |
| Worn paths and trails | Blueprint › Worn paths | Derived from the `trail_wear` ledger | `world.pass.TrailPass` daily | Surface `trodden=true`, then `worn_ground` | M9 |
| Decay of placed blocks | Structures › Rot; Decay layer | History | `DECAY` when a piece's HP reaches 0 | Masked piece blocks to items | M8 |
| Deadwood dying and falling | Blueprint › Deadwood; Land › Deadwood decay | History | `TREE_DIE`, `FALL` | Logs, `fallen_log`, `punk_log` by decay class | M7 |
| Stumps greying | Blueprint › Regrowth | Derived from the felling day | `TrailPass` sibling `StumpPass` | `stump` `age` 0–3 | M8 |
| Leaf colour | Engine › Seasons on screen | Client only | Colour handlers and the bare-leaf model | None | M7 |
| Leaf litter | Video › Leaf fall | Derived | `world.season.LeafStage` pass | `leaf_litter` layer | M7 |
| Lily pads | Video › Lily pads | Derived | `world.water.LilyPads` pass on still-water markers | `lily_pad` `age` | M7 (RECONCILE: the Video tab's text says M5; the table's M7 wins, since the pass needs the season stages) |
| Plant phases, fireweed `ripe`, berries | Plants › Phases through the year | Derived, with the `patches` ledger | `forage.PlantPhasePass` daily | `phase`, `taken`, mushroom `age` | M5 |
| Mushroom flushes | Land › Mushroom flushes | Derived from the year table and rain | Flush pass places and removes mushroom blocks on flush cells | Mushroom blocks | M5 |
| Snow, crust, overflow slush | Engine › Ice, snow and permafrost | Derived | `SnowPass` | `snow_cover` | M7 |
| River and lake ice | Same | Derived | `IcePass` | Ice blocks | M7 |
| Floods and jams' water | Water › Floods, jams and ice | Derived from the year table | `FloodController` | Water on the flood mask, `flood_flags` | M7 |
| Flood marks: silt, driftwood, pools | Same | History | `DEPOSIT`, `DRIFT` at the Breakup tick | `silt_layer`, `driftwood_pile` | M7 |
| Lost gear on breakup | Video › Sunk gear | History | `DRIFT` with an item payload | `lost_gear` | M7 |
| Shove ice, aufeis, springs and leads | Water › Floods, jams and ice | Shove and aufeis history; springs derived | `SHOVE`, `AUFEIS`; `IcePass` | `shove_ice`, `aufeis` | M7 |
| Pressure ridges, candle ice, anchor ice, hoarfrost | Land › Ice; Hoarfrost | Derived | `world.land.IceWorks`, `env.PhenomenaDirector` | `pressure_ice`, `candle_ice`, `anchor_ice`, `hoarfrost_layer` | M7 |
| Mud season | Land › Mud season | Derived | Mud pass sets `wet=true` on soil surfaces | Block state only | M7 |
| Frost heave, pingos | Land › Frost heave and pingos | Pingos generated; heave on pieces is piece damage | Stage 6 feature; Building's settlement | `pingo_ice` | M8 |
| Thermokarst ponds, slumps | Water › Beavers, ponds and thaw | History | `SUBSIDE`, `THERMOKARST`, `EXPOSE` | Soil, water, `slump_mud`, `ground_ice` | M9 |
| Rockfall, avalanches, windthrow | Land › Ground and slopes | History | `ROCKFALL`, `AVALANCHE`, `WINDTHROW` | `loose_boulder`, `avalanche_debris`, `root_plate` | M6–M7 |
| Iron renewal | Blueprint › Other resources | History | `NODE`, `EXPOSE` | `bog_iron_bed`, `iron_outcrop` | M9 |
| Animal-made blocks: dens, push-ups, redds, nests, middens | Land animals, Water life, Birds tabs | History, owned by Ecology | Ecology submits ops of kind `DEPOSIT` or `VEGETATE` with its block id | `den_mound`, `muskrat_push_up`, `redd_gravel`, `bird_nest`, `cone_midden` | Per their tabs |
| Gut piles and bone piles | Bestiary › Carcasses and bodies; Decay and time | History, owned by Decay | `DECAY` on blocks in `#yearofice:decays` | `gut_pile` to `bone_pile` | M5–M6 |
| Abandoned camps ageing | Decay and time › Abandoned camps and outposts | History, owned by Decay | `DECAY` on masked pieces past their thresholds | The pieces' own stages | M8–M9 |

**Decision: wildfire escalation (RECONCILE 38).** A `STRIKE` that lights a tree hands over to the wildfire producer only if the year profile drew a wildfire year or the fire danger index is Extreme that day. A fire the player started can always escalate, by the ignition formula in The open world.

**Decision: dropped items (RECONCILE 36).** An `ItemEntity` keeps the game minute it was dropped in its persistent data. `ItemExpireEvent` adds life while fewer than 7 game days have passed on the clock, so time in unloaded chunks counts. On chunk load, items past 7 game days are removed. An item that sinks in water over 1 m deep, or is under water at breakup, becomes part of a `lost_gear` block on the bed through a `DRIFT` op, never an entity.

### Chunk attachments

**Every per-chunk record the world keeps.** All are NeoForge data attachments on `LevelChunk`, registered in a `DeferredRegister` on `NeoForgeRegistries.Keys.ATTACHMENT_TYPES` in `world.attach.WorldAttachments`, each with a `Codec` and a default of "absent". A chunk with no water, no history and no player work carries only `season_stamp`.

| Attachment | Layout | Typical size | Saved | Synced | Written by | Read by |
| --- | --- | --- | --- | --- | --- | --- |
| `yearofice:season_stamp` | Last settled game day and minute; ice day; snow class (2 bits a column); dug and packed snow columns; last loaded day; generator version | 80 B | Yes | No | Every pass; chunk load | All passes, `ChunkPruner` |
| `yearofice:current` | Presence mask and 6 bytes per water column (above) | 0–1.6 KB | Yes | Payload `current_chunk` on `ChunkWatchEvent.Sent` | Generation; World edits when channels move | Push, fishing, Environment field, swimming, ice |
| `yearofice:sign` | Records: kind, species, position, yaw, game minute (engine) | 0–4 KB | Yes, aged out on save | Payload `sign_chunk` within 48 blocks | Ecology, the player, Decay | Tracking, client decals |
| `yearofice:stand` | Transient view: stand type, age, beetle stage, lichen fraction for the chunk's 32 m cell | 4 B | No (see decision) | No | Filled on load from the region stand map | Ecology, Cover, fire |
| `yearofice:microclimate` (owner `env`) | One byte per column: class (4 bits) and offset step (4 bits) | 256 B | Yes | No | Generation writes it once; `REGROW`, `BURN` and clearing re-class columns through `env` | Environment field, plant pass, gardens |
| `yearofice:hazard` | Start zones in the chunk: id, avalanche H, last day, last release | 0–64 B | Yes | No | `SlopeHazards` daily | Avalanche roll, messenger warnings |
| `yearofice:flood_flags` | Per raised column: base y and placed layer count | 0–512 B while flooded | Yes | No | `FloodController` | Flood fall, catch-up |
| `yearofice:placed_mask` | Per section with any placed block: a 4,096-bit set | 0; 512 B per touched section | Yes | No | Block place events, `mark_placed` processor, frames | Every op; `ChunkPruner` |
| `yearofice:erosion_counters` | Sparse: column, bank counter (u8 in 1/256 block), thaw index T (u8), bare-upslope count (u8) | 0–300 B | Yes | No | `HydroPlanner` results at each change tick | Planner, site check |
| `yearofice:trees` | Tree records whose base is in the chunk: species, base, height, vigor (u8), state, age, decay class, girdle and tap flags | 4–40 B per tree, about 1 KB in forest | Yes | No | Tree builders, felling, `TREE_DIE`, `FALL`, `REGROW`, plant work | Felling, readable trees, Decay, Ecology browse |
| `yearofice:patches` | Patch records: patch id, species, health H, picked fraction p, last harvest day, tended year, dug fraction | 12 B per patch | Yes | No | Generation seeds; harvest verbs; animals | `PlantPhasePass`, Ecology, the patch card |
| `yearofice:trail_wear` | Sparse 2 m cells: passes in the last 30 days (u8), last pass day (u16), stage (2 bits) | 0–400 B | Yes | No | Movement of players, sleds and trail animals | `TrailPass`, travel speed, Ecology avoidance |
| `yearofice:decay_marks` (owner `decay`) | Sparse marks: position, curve, clock key, due (Architecture › Layer 5); the Decay tab's stamps and area cells fold into it | 0–6 KB (cap 512) | Yes | No | Decay | Decay's day pass, `ChunkCaughtUpEvent` |
| `yearofice:edit_log` | Count of history ops applied, and a rolling hash of them | 12 B | Yes | No | `ChangeApplier` | Determinism tests, `ChunkPruner` |

**Decision: stand age lives in the region, not the chunk.** The Land tab puts `yearofice:stand` on each chunk. Fire spread and succession run off-screen on 32 m cells, so the truth is a stand map in the region or district SavedData (4 bytes per 32 m cell, 16 KB a region). The chunk attachment of the same name is a transient copy filled on load, so nothing can disagree.

**Decision: trail wear on 2 m cells.** The engine says 8 m cells; the Blueprint's worn-path rule counts passes per 2 m cell, and a worn trail 8 blocks wide would look wrong. 2 m cells win; the attachment is sparse, so the cost is only on walked ground.

**Decision: custom payloads, not attachment auto-sync.** Only `current` and `sign` reach the client, as small payloads on chunk send. That works the same on either version pin; no other world attachment is needed client-side, because the client draws seasons from `env_sync`.

**Attachments during generation.** Generation writes `current`, `microclimate`, `trees`, `patches` and the snow class while the chunk is still a `ProtoChunk`. If the pinned NeoForge version does not carry proto-chunk attachments over to the `LevelChunk`, the same values are rebuilt from layers on the chunk's first `ChunkEvent.Load` with `isNewChunk` true; they are deterministic, so both routes give the same bytes. The first prototype below settles which.

### Catch-up on chunk load

**The order on load.** `world.catchup.CatchUp` listens to `ChunkEvent.Load` at `EventPriority.HIGH` and runs the lead's four steps, with the world's detail, on the main thread before the chunk is sent to any player.

1. **New chunk.** If `isNewChunk`, write any missing generation attachments and mark the chunk unsaved.
2. **Terrain ops.** Apply every pending op, oldest tick first, through `LevelChunk.setBlockState(pos, state, false)`, collecting changed positions. Within 600 blocks of a player the `yearofice-plan` pool has already folded them into one net diff.
3. **Passes.** Each registered pass settles from its stamped day to today: ice, flood water, snow, plant phases and patches, lily pads, leaf litter, trail and stump stages, mud state. Then `season_stamp` and `edit_log` are written.
4. **Light and heightmaps.** Heightmaps update for changed columns; one light check per changed position is queued for the next tick.
5. **`ChunkCaughtUpEvent(chunk, fromDay, toDay)`.** Traps, crops, fires, rooms and decay marks settle, so they see the settled snow, ice and water.
6. **Ecology.** Animals whose records sit in the chunk within the Active radius are materialised.

**Bounded work.** History per chunk is capped by the 3-year merge, and every derived pass is one sweep over 256 columns. The engine's budget is 2 ms per chunk spread over ticks. When more than 8 chunks load in one tick, steps 2–3 for the farthest ones move to the next tick, and those chunks are held back from sending until done, through a send gate in `world.catchup.LoadGate`.

**Determinism rules.**

- Every roll is drawn from `hash(seed, producer, day, cell, process)`; nothing reads `level.random`, tick timing or load order.
- Canonical order of ops is (game day, tick kind, producer priority, cell x, cell z, sequence). Two ops in one chunk on one day always apply in this order.
- Derived passes are idempotent: running one twice for the same day changes nothing.
- A live world applies the same ops within an hour; the result differs from catch-up only if the player edited the same blocks in that hour, which the mask makes impossible for history ops.

**Tests.** `world/catchup_equals_live`: two copies of a seeded 8 × 8 chunk area, one kept loaded for 400 game days, one unloaded for the same span and then loaded, give equal block hashes and equal attachment hashes. `world/catchup_order_free`: loading 64 chunks in 10 shuffled orders gives one hash. `world/catchup_budget`: returning after 3 years to 441 chunks never takes a tick over 50 ms.

### Performance

**Budgets for the world.**

| Item | Budget | How it is met |
| --- | --- | --- |
| Generation per chunk | Average under 5 ms on one worker; under 15 ms for a chunk holding a template | Single-value sections; layers precomputed; no noise per block |
| Region layers | Under 3 s; district layers under 3 s; Interior plan under 2 s | As World map; layer threads separate from chunk workers |
| Chunk rate at sled speed | 10 chunks a second at a 12-chunk view | See below |
| Catch-up on load | Under 2 ms a chunk, at most 8 chunks a tick | Load gate |
| Snow and ice passes | 6 chunks a tick; a full 441-chunk area in under 4 real seconds | Snow class stored; changed columns only |
| Change ticks | Planning under 200 ms on the layer thread; apply 256 block sets a tick | Pure planner; caps |
| Region SavedData | Under 400 KB a home region after 5 years | Sparse lists |

**Correction to the chunk rate.** World map › Fast travel says a team at 6 m/s needs 2–3 new chunks a second. At a 12-chunk view the front edge is 25 chunks wide and the team crosses a chunk every 2.7 s, so it needs about 9.4 chunks a second. At 5 ms a chunk that is under 50 ms of worker time a second, inside budget; the district pre-build 800 blocks ahead still gives 133 s of warning.

**The Interior's coarse fields.** Off-screen systems never read 4 m layers. The Interior plan at 128 m carries height, district type, glacier, burns, wind exposure and cold-air pooling. Each visited district also keeps a 32 m field (128 × 128 cells, 8 bytes each, 128 KB) in its region SavedData `yearofice_region_<key>` (the lead's key: a home cell or a district code): stand age, fire history, snow class mix, micro-climate class mix and habitat capacity. Off-screen wildfire spread, populations and stand succession run on these 32 m cells; the 4 m layers are built only when the player comes near.

**Pruning Interior and Beyond chunks.** `worldgen.ChunkPruner` deletes chunks outside the home valley that can be rebuilt exactly.

1. A chunk is prunable when its `placed_mask` is empty, its `edit_log` count is 0, it holds no saved entities, and its last loaded day is over 60 game days ago.
2. Pruning runs offline: on `ServerStoppedEvent` and on `ServerAboutToStartEvent`, before any level loads. It rewrites the region file header (`.mca`), clearing the chunk's offset entry, and does the same for the matching `entities/` and `poi/` files. Files more than half empty are compacted.
3. A pruned chunk regenerates identically, and the settle pass brings it to today.

**Wild animals never save inside chunks.** Architecture › Layer 4 sets `shouldBeSaved` false for wild animal entities, so most chunks hold no entities and stay prunable. Saved entities (carcasses, the companion wolf, livestock, boats and sleds) and persistent ground blocks (`left_pack`, `lost_gear`), which set `placed_mask`, mark a chunk unprunable. There is no `ground_pile` (RECONCILE 97).

**Save size.** Home valley chunks: about 16,000 chunks a region, 6–10 KB each compressed with LZ4, under 2 GB for all 16 regions. Kept Interior chunks are those with history (river banks, ponds, burns near the player) or player work; at 1.5 GB that allows about 200,000, far more than a five-year game visits.

### Vanilla gaps in the world, decided

| Vanilla behaviour still present | Decision |
| --- | --- |
| Fluid ticks on water and flowing water | Kept; generation and floods place water so nothing spills (test `worldgen/no_spill`) |
| Snow golem, frost walker and other enchantment snow and ice | No enchantments ship; frost walker is filtered with vanilla enchantments |
| Bone meal | Hidden and banned; no growth shortcut exists |
| Pistons and redstone moving terrain | Hidden and banned |
| Explosions | Not used; the blast resistance of deep ground stops any modded one |
| Player structure blocks and `/fill`, `/setblock` | Creative and command use only; World edits treat command changes as unmasked natural blocks unless placed by a player |
| Spawn chunks kept loaded | Ours keep none; the outpost is not ticked while the player is away (catch-up covers it) |
| Map items (`filled_map`) | Hidden; the messenger map replaces them |

### Registry table: terrain, water, ice and snow

**Every world block the design names, with its build entry.** Rows that share a class list their ids together. Block items exist only where the Type column says "item"; every other world block drops what its loot table says and has no item form.

| Id | Type | Class | Data file | Milestone |
| --- | --- | --- | --- | --- |
| `deep_ground`, `deep_rock` | Block, unbreakable | `worldgen.block.DeepBlock` | No loot table | M2 |
| `face_rock` | Block | `worldgen.block.FaceRockBlock` | `loot_table/blocks/face_rock.json` (angular stone) | M2 |
| `iron_outcrop`, `rust_stain` | Block; outcrop has `nodules` 0–8 | `worldgen.block.IronOutcropBlock` | `loot_table/blocks/iron_outcrop.json` | M2 |
| `bog_iron_bed` | Block, `nodules` 0–6 | `worldgen.block.BogIronBlock` | `loot_table/blocks/bog_iron_bed.json` | M5 |
| `coal_seam` (`remaining` 0–6, `burning`), `float_coal` item | Block | `worldgen.block.CoalSeamBlock` | `worldgen/placed_feature/coal_seam.json` | M2 |
| `flint_nodule`, `river_stones`, `scree_stones` | Pickup surface blocks | `worldgen.block.PickupBlock` | `loot_table/blocks/*.json` | M2 |
| `silt`, `loess`, `till`, `river_sand`, `gravel`, `peat`, `clay_bank`, `tailings_gravel`, `frozen_soil` | Soil blocks (`wet` state) | `worldgen.block.SoilBlock` | `tags/block/diggable_soil.json`, `freezing_soils.json`, `erodible_bank.json`, `terrain_steps.json` | M2 |
| `forest_duff`, `moss_mat`, `grass_turf`, `tundra_turf`, `tussock_turf`, `lichen_mat` | Surface blocks (`trodden`, `wet`) | `worldgen.block.SurfaceBlock` | Same tags; colour handler | M2 |
| `gravel_layer`, `sand_layer`, `mud_layer` | Layer blocks 1–7 | `worldgen.block.GroundLayerBlock` | — | M2 (if kept) |
| `mud`, `pond_mud`, `slump_mud`, `silt_layer`, `ash_layer` (`warm`, `cold`, `leached`, from Decay and time), `burnt_duff`, `worn_ground` | Ground blocks | `worldgen.block.SoilBlock` subclasses | `tags/block/rough_ground.json` | M7–M9 |
| `salt_crust` | Surface block at licks | `worldgen.block.SurfaceBlock` | — | M2, with the licks (RECONCILE 167, 170) |
| `snow_cover` (`layers` 1–8, `crust`, `wet`) | Block | `world.snow.SnowCoverBlock` | — | M7 |
| `skim_ice`, `river_ice` (`stage`), `shove_ice`, `aufeis` (layers), `pressure_ice`, `candle_ice`, `anchor_ice`, `frazil_slush`, `ground_ice`, `pingo_ice` | Ice blocks | `world.ice.*IceBlock` | `phenomenon/<id>.json` | M7 |
| `ice_block` | Cut ice, item | `build.snow.CutIceBlock` | `data_maps/item/weight.json` (900 kg) | M8 |
| `hoarfrost_layer` | Layer on surfaces | `world.land.HoarfrostBlock` | — | M7 |
| `avalanche_debris`, `loose_boulder`, `root_plate` | Hazard blocks | `world.land.*` | `loot_table/gameplay/avalanche_debris.json`, `root_plate.json` | M6–M7 |
| `quinzhee_snow`, `snow_block` | Built snow, item for `snow_block` | `build.snow.SnowWorks` | `recipe/snow_knife.json` | M7 |
| `water_marker` (`feature`) | Invisible marker, POI per value | `ecosystem.water.WaterMarkerBlock` | `worldgen/placed_feature/water_markers.json` | M6 |
| `driftwood_pile`, `drift_log`, `log_jam`, `snag_log`, `fallen_log`, `charred_log`, `punk_log`, `struck_spruce` | Wood ground blocks | `world.logs.*` | `loot_table/blocks/*.json` | M2–M9 |
| `lost_gear` | Waterlogged container, block entity | `item.LostGearBlock` (lead's registry); placed by World edits `DRIFT` ops | `loot_table/blocks/lost_gear.json` (empty) | M5 |
| `beaver_dam` | Block | `ecosystem.species.beaver.DamBlock` | — | M9 |
| `beaver_lodge` | Block entity holding colony id | `ecosystem.species.beaver.LodgeBlock` | Placed by stage 7 | M6 |
| `crib_wall`, `riprap`, `sod_dike`, `pond_leveler`, `water_bar` | Protection pieces | `build.protect.*` (Building and crafting) | `recipe/*.json` | M9 |
| `squirrel_larder` | Block entity with loot | `forage.LarderBlock` | `loot_table/gameplay/squirrel_larder.json` | M5 |
| `note_spot` | Block | `story.NoteSpotBlock` | `note/*.json` | M1 |

### Registry table: plants, trees and fungi

**One class per plant form, one block per species.** The classes are Body and survival's (`forage`); worldgen places them. All carry `phase` (8 values) and `taken` (0–3), set by `PlantPhasePass`. Data rows live at `data/yearofice/plant/<id>.json` (RECONCILE 18; the Land tab's `plants/` folder reads as this one).

| Ids | Form | Class | Milestone |
| --- | --- | --- | --- |
| `white_spruce_log` and `_leaves`, `black_spruce_log` and `_leaves`, `birch_log` and `_leaves`, `aspen_log` and `_leaves`, `tamarack_log` and `_leaves`, `balsam_poplar_log` and `_leaves`, `subalpine_fir_log` and `_leaves`; log states `dead`, `ants`, `scar`; `stump` (`age`); `sapling` (`species`, `stage` 0–3) | Tree blocks from builders `worldgen.trees.WhiteSpruce`, `BlackSpruce`, `Birch`, `Aspen`, `Tamarack`, `BalsamPoplar`, `SubalpineFir`, `Deadwood` | `forage.tree.YoiLogBlock`, `YoiLeavesBlock` | M2 (first four), M5 (rest) |
| `feltleaf_willow`, `diamondleaf_willow`, `barclay_willow`, `bebb_willow`, `green_alder`, `thinleaf_alder`, `red_osier_dogwood`, with `shrub_stem` | Tall shrub: stems and brush blocks, one shrub record per stool in `trees`; the brush is in tag `yearofice:brush` with empty entity collision and the slowdown the Entities section sets | `forage.ShrubStoolBlock` | M5 |
| `dwarf_birch`, `sweetgale`, `beauverd_spirea`, `leatherleaf`, `shrubby_cinquefoil`, `common_juniper`, `mountain_heather`, `alpine_bearberry`, `mountain_avens`, `bog_rosemary`, `labrador_tea`, `devils_club` | Low shrub, partial collision ×0.85 | `forage.LowShrubBlock` | M5 |
| `blueberry_bush`, `lingonberry_bush`, `cloudberry_bush`, `raspberry_bush`, `highbush_cranberry_bush`, `wild_rose_bush`, `red_currant_bush`, `bearberry_bush`, `bog_cranberry_bush`, `soapberry_bush`, `nagoonberry_bush`, `skunk_currant_bush`, `black_currant_bush`, `wild_strawberry_bush`, `bunchberry_bush`, `crowberry_bush`, `bog_blueberry_bush` | Berry bush, no item, tag `yearofice:berry_bushes` | `forage.BerryBushBlock` | M5 |
| `fireweed`, `dwarf_fireweed`, `wild_chives`, `nettle`, `yarrow`, `wild_celery`, `cow_parsnip`, `dandelion`, `pineapple_weed`, `plantain`, `sourdock`, `wild_potato`, `wild_sweet_pea`, `wormwood`, `wild_rhubarb`, `feral_rhubarb`, `valerian`, `tall_bluebells`, `sweet_coltsfoot`, `roseroot`, `pasqueflower`, `northern_geranium`, `northern_bedstraw`, `mountain_sorrel`, `lambsquarters`, `jacobs_ladder`, `goldenrod`, `field_mint`, `chickweed`, `arnica`, `arctic_lupine`, `alpine_bistort` | Herb cross block | `forage.PlantBlock` | M5 |
| `monkshood`, `tall_larkspur`, `false_hellebore`, `water_hemlock`, `death_camas`, `baneberry` | Dangerous herbs; 2-tall where over 1.2 m | `forage.PlantBlock`, `forage.TallPlantBlock` | M5 |
| `sweetgrass`, `foxtail_barley`, `cottongrass`, `bluejoint`, `wood_fern`, `ostrich_fern`, `lady_fern`, `stiff_clubmoss`, `horsetail` | Grass, sedge, fern | `forage.PlantBlock` | M5 |
| `cattail`, `bulrush`, `bur_reed`, `water_sedge`, `water_horsetail`, `mares_tail`, `buckbean`, `arrowhead`, `bladderwort`, `wild_calla`, `pond_weed`, `marsh_marigold`, `sundew` | Aquatic and wet ground; waterlogged where in water | `forage.AquaticPlantBlock` | M5 |
| `lily_pad` (`age`) | Floating surface block, no collision, tag `yearofice:slows_boats` | `world.water.LilyPadBlock` | M7 |
| `sphagnum`, `stair_step_moss`, `haircap_moss`, `feather_moss`, `fire_moss`, `reindeer_lichen`, `iceland_moss` | Mat (carpet-height) block | `forage.MatBlock` | M5 |
| `rock_tripe`, `map_lichen`, `sunburst_lichen` | Face decal block on rock and bark | `forage.FaceLichenBlock` | M5 |
| `tinder_conk`, `red_belted_conk`, `aspen_bracket`, `birch_polypore`, `chaga`, `aspen_oyster`, `orange_jelly` | Face fungus on log sides, `size` 0–3 | `forage.BracketBlock` | M5 |
| `morel`, `false_morel`, `fly_agaric`, `king_bolete`, `birch_bolete`, `orange_cap_bolete`, `hedgehog_mushroom`, `chanterelle`, `false_chanterelle`, `puffball`, `destroying_angel`, `deadly_galerina`, `hawks_wing`, `tamarack_jack`, `shaggy_mane`, `poison_pax` | Mushroom cross block, `age` 0–3, placed by flushes | `forage.MushroomBlock` | M5 |
| `sedge_mat` | Floating mat over water | `forage.MatBlock` | M9 |

**Plants are placed by data.** Stage 6 lays out patches per species from each `plant/<id>.json` habitat, so the patch ledger's seed values exist before any block. Each species also has `worldgen/placed_feature/<id>.json` of feature `yearofice:plant_patches` with the `yearofice:layer_mask` modifier, added to its biomes by a `neoforge:add_features` biome modifier, as the Plants tab asks. A content pack adds a plant with three files and no code. `feral_rhubarb` (the roadhouse beds, toxic leaves) and `wild_rhubarb` (native) are two blocks (RECONCILE 65).

**Decision: berry bush names and the regrow counter (RECONCILE 15).** Every berry block id ends in `_bush`, so the Plants tab's `nagoonberry`, `wild_strawberry`, `bunchberry` and the Land tab's `crowberry`, `bog_blueberry` read as `nagoonberry_bush` and so on. `BerryBushBlock` carries `phase` and `taken`; the 0–8 regrow count lives in the `patches` ledger, not a block state, because it changes daily in unloaded chunks and does not need its own model.

**Decision: the ice block unit (RECONCILE 3).** `ice_block` is 1 m³ of cut ice, 900 kg, made of 20 cakes of 45 kg (Structures). The Land tab's "22 ice blocks (2 t)" means 2 t of ice: 44 cakes, about 2.2 blocks. The Building and crafting department sets the ice house's count in those units.

### Registry table: worldgen, structures and data

| Id | Type | Class | Data file | Milestone |
| --- | --- | --- | --- | --- |
| `yearofice:valley` | Chunk generator | `worldgen.YoiChunkGenerator` | In the world preset | M2 |
| `yearofice:valley` | Biome source | `worldgen.YoiBiomeSource` | Same | M2 |
| `yearofice:valley` | Dimension type | — | `dimension_type/valley.json` | M2 |
| `yearofice:year_of_ice`, `test_flat`, `layer_preview` | World presets | `worldgen.preset.*` | `worldgen/world_preset/*.json`; `data/minecraft/tags/worldgen/world_preset/normal.json` | M2 |
| The 20 biomes | Biome | — | `worldgen/biome/*.json` | M2 |
| `yearofice:setpiece`, `natural_feature`, `landmark` | Structure types | `worldgen.structure.SetPieceStructure`, `NaturalFeatureStructure`, `LandmarkStructure` | `worldgen/structure/<id>.json` per place; `worldgen/structure_set/setpieces.json`, `natural_features.json`, `landmarks.json` | M1 |
| `yearofice:places_layer` | Structure placement type | `worldgen.structure.PlacesLayerPlacement` | In the structure sets | M1 |
| `yearofice:template_piece` | Structure piece type | `worldgen.structure.TemplatePiece` | — | M1 |
| `snap_to_ground`, `age_world`, `loot_seed`, `mark_placed`, `season_blank` | Structure processors | `worldgen.structure.processor.*` | `worldgen/processor_list/<id>.json` | M1 |
| `layer_trees`, `plant_patches`, `layer_nodes`, `water_markers`, `lily_pads`, `coal_seam`, `rock_overhang`, `pingo`, `line_feature`, `natural_landmark` | Features | `worldgen.features.*` | `worldgen/configured_feature/*.json`, `placed_feature/*.json` | M2 (trees, nodes, coal, overhang), M5 (plant patches), M6 (water markers), M7 (lily pads), M8 (pingo), M10 (lines, Interior landmarks) |
| `yearofice:layer_mask`, `shell_only`, `on_water`, `in_district_type` | Placement modifier types | `worldgen.features.placement.*` | Used in placed features | M2 |
| `no_vanilla_spawns`, `no_foreign_features` | Biome modifiers | NeoForge built-ins | `neoforge/biome_modifier/*.json` | M2 |
| The setpiece templates | Structure NBT | — | `structure/setpiece/<id>/<variant>.nbt` | M1 (home), M10 (Interior) |
| `poi_weed_pond` and the other 11 water POIs, `poi_lick`, `poi_den`, `poi_lodge` | POI types | `ecosystem.water.WaterPois` | — | M6 |
| `yearofice:current_chunk`, `sign_chunk` | Payloads, server to client | `world.attach.CurrentPayload`, `SignPayload` | — | M2, M6 |
| `yearofice_world`, `yearofice_terrain`, `yearofice_water_features` | SavedData | `worldgen.WorldParams`, `world.change.TerrainChangeQueue`, `ecosystem.water.WaterFeatureRegistry` | — | M0–M9 |
| Stand map, set-piece starts, dig permits, district 32 m field | Fields in the lead's `yearofice_region_<key>` | `worldgen.RegionWorld` | — | M2–M10 |
| `diggable_soil`, `freezing_soils`, `erodible_bank`, `terrain_steps`, `rough_ground`, `snags`, `sweepers`, `berry_bushes`, `brush`, `slows_boats`, `decays`, `banned_vanilla_blocks`, `heave_ground`, `avalanche_start`, `micro_spray_zone` | Block tags | — | `tags/block/*.json` | M2–M8 |
| `bank_erodibility`, `flammability`, `snow_class_cover` | Block data maps (Decay keeps its own `decay.json`) | Read by World edits | `data_maps/block/*.json` | M7–M9 |
| Terrain processes | Our data kind | `sim.terrain.ProcessDef` | `terrain_process/<id>.json` | M7–M9 |
| Region, district type, named area, province kind, plan, set piece, natural feature, paper map | Our data kinds | `worldgen.data.*` | `region/`, `district_type/`, `district/`, `province_kind/`, `interior/plan.json`, `setpiece/`, `natural_feature/`, `paper_map/` | M0–M11 |
| Stand succession, micro-climate rules, phenomena | Our data kinds (lead's paths) | `world.land.ForestStand`, `env.MicroClimate`, `env.PhenomenaDirector` | `succession/<stage>.json`, `microclimate/<class>.json`, `phenomenon/<id>.json` | M5–M7 |

### GameTests and checks for the world

Each is `yearofice:<system>/<test>`, batched by milestone. JUnit tests run on layers with no world.

1. **`worldgen/height_hash_jvms` (JUnit, M0).** 20 fixed seeds give the hashes in `tests/hashes.json` on Linux, Windows and macOS, x64 and ARM.
2. **`worldgen/no_spill` (GameTest, M2).** A 512 × 512 river area has no fluid tick outside falls and lips after 200 ticks, and no water block above any column's stage 4 surface.
3. **`worldgen/no_nodes_below_surface` (GameTest, M2).** The engine's test, plus: every `face_rock` touches air or water, and no `iron_outcrop` or `coal_seam` sits behind the shell.
4. **`worldgen/chunk_budget` (GameTest, M2).** 2,000 chunks of the core generate at an average under 5 ms and a 99th percentile under 15 ms on the CI machine.
5. **`worldgen/setpiece_masked` (GameTest, M1).** Every block placed by a set piece template has its `placed_mask` bit, and 10 simulated years of terrain ops change none of them.
6. **`world/no_random_ticks` (GameTest, M2).** As above.
7. **`world/catchup_equals_live`, `catchup_order_free`, `catchup_budget` (GameTest, M7).** As above.
8. **`world/snow_pass_classes` (GameTest, M7).** After a 25 cm snowfall, open columns hold 2 layers, under-spruce columns 1, lee columns 4 after a 7 m/s wind; MSPT stays under 50 ms.
9. **`world/flood_returns` (GameTest, M7).** After a flood falls, every block counted in `flood_flags` is removed and no water remains outside closed hollows.
10. **`world/wildfire_escalation` (JUnit sim, M9).** Over 1,000 lightning strikes in non-wildfire years with danger below Extreme, 0 escalate; in wildfire years, 20–40%.
11. **`world/regrowth_schedule` (GameTest, M9).** A cleared 20 × 20 area shows fireweed on the first June 1, birch saplings at 2 years, spruce saplings at 6, and none on worn-trail or footprint cells.
12. **`world/prune_identical` (GameTest, M10).** World map test 6, plus: a chunk with one applied history op is never pruned.
13. **`world/item_lifespan` (GameTest, M5).** A dropped item in an unloaded chunk for 6 game days is still there on load; at 8 game days it is gone.
14. **`world/beaver_pond_rise` (GameTest, M9).** A colony with 40 willows in reach raises its dam two stages in two years and its pond by one block (0.5 m a stage), and the pond's new water never leaves its mask.

### Path types for world blocks

**The Entities section asks the world for path types.** Every world block answers NeoForge's `IBlockExtension.getBlockPathType(state, level, pos, mob)` with a vanilla `PathType`; species maluses on top are the Entities developer's.

| Blocks | Path type | Note |
| --- | --- | --- |
| Soils, surfaces, gravel, sand, `worn_ground`, mats | `WALKABLE` | Mud, `pond_mud` and `slump_mud` add malus 2 for heavy species |
| `snow_cover` | `WALKABLE` | Depth sinking and crust come from the Environment field, not the path type |
| `river_ice`, `skim_ice`, `aufeis` | `WALKABLE` | Thin and rotten ice add malus 8 for moose and caribou (Entities); `skim_ice` is `WATER` for anything over 20 kg |
| Brush (`#yearofice:brush`), low shrubs, berry bushes | `WALKABLE` | Empty entity collision; the slowdown and noise are the Entities section's |
| `face_rock`, `deep_rock`, logs, `fallen_log`, `loose_boulder`, `root_plate` | `BLOCKED` | Animals step over 1-block logs by their step height |
| `lily_pad`, `sedge_mat`, aquatic plants | `WATER` | The mat's 5% break-through is the Water tab's |
| `beaver_dam` | `WALKABLE` | Dams are bridges, as the Blueprint says |
| Lit `flame`, `smoulder`, `ash_layer` with `warm` | `DAMAGE_FIRE` | With the fire sensor's keep-off radius |

### RECONCILE items settled here

The lead engineer settled the naming and vanilla-rule items; this section owns 12 and 20 and applies the rest on the world side.

| No. | Settled as |
| --- | --- |
| 12, 20 | Dig permits on deep ground for dugouts, root (permafrost) cellars and chimney footprints, at or above the mouth floor, no drops (Terrain to blocks) |
| 3 | `ice_block` is 900 kg, 20 cakes of 45 kg; the Land tab's 2 t is 44 cakes |
| 11 | World size, rings and height follow the World map tab throughout |
| 15, 64 | Berry blocks `yearofice:<species>_bush`, class `forage.BerryBushBlock`; the 0–8 regrow count is a `patches` ledger field |
| 37 | `yearofice:terrain_steps` holds natural surfaces only |
| 52, 65 | Plants tab ids win; `feral_rhubarb` and `wild_rhubarb` are separate blocks |
| 6, 16, 18, 63 | Applied: worldgen and plant data paths and snake\_case fields; plants in `forage`; the lead's package map |
| 34, 36, 38, 44 | Applied on the world side: vanilla terrain blocks banned from templates and features; `lost_gear` placed by `DRIFT` ops; `STRIKE` escalation by the lead's rule; the preset carries the lead's game rules |

**Other conflicts found and decided here.** Decay advances block marks on its day pass and in catch-up, not on random ticks, which are off (Decay and time). Roof snow is `snow_cover`, not vanilla snow layers (Structures). Stand age lives in the region stand map (Land). Trail wear uses 2 m cells (engine; the Entities section's "8 m cells" reads as 2 m). Lily pads ship in M7 (Video). Water bodies are `yearofice_water`; the Water tab's feature registry becomes `yearofice_water_features`. The sled chunk rate is about 9.4 a second, not 2–3 (World map).

### Risks and first prototypes

Prototype numbers are W1–W10, so they never clash with the lead's P1–P7.

| Risk | Why it matters | First prototype to retire it |
| --- | --- | --- |
| A 1,968-block world under our generator | Lighting and memory across 123 sections | W1: generate the core at both heights (1,968 and 384 with the engine's fallback compression); measure heap, chunk time and client fps at 12 chunks. Week 1 of M2 |
| Proto-chunk attachments | Generation data may not reach the `LevelChunk` | W2: one attachment written in `fillFromNoise`, read in `ChunkEvent.Load`, on the pinned version. Half a day |
| Block writes during `ChunkEvent.Load` | Re-entrant chunk loads or light glitches | W3: apply 64 ops and a full snow pass in the load event on 441 chunks; check for deadlocks, missing light and client desync. M2 |
| Catch-up hitches after a long absence | A 3-year return could stall a tick | W4: the `catchup_budget` test with the load gate and pre-planned diffs, before M7 content lands; it shares the lead's P1 harness |
| Water spilling at chunk borders | Fluid ticks cascade, rivers drain or flood | W5: the `no_spill` test on all four archetypes. M2 |
| Pruning edits region files | A bad header write loses chunks | W6: prune and restore 10,000 chunks on a copy, compare hashes, kill the process mid-write; the pruner writes a new file and renames, never in place. M10 |
| Sodium and Iris with our colour handlers and leaf model | Season colours could break | W7: the season re-mesh under both, in the client smoke. M7 |
| Determinism drift between JVMs | Different land on different machines | W8: the hash test nightly on all platforms from M0, plus the worldgen lint |
| The queue growing without bound | Save size and load time | W9: a 10-year JUnit sim of every producer over the home valley; queue under 2 MB and edit counts within caps |
| Another mod's worldgen data | Foreign features or structures appear | W10: load a popular structure mod in the client smoke and assert none generate |

### What the world developer needs from the others

- **Lead engineer.** Confirm `world.change.ChangeTicks` deriving the five change ticks from `IceChangedEvent`, `WeatherChangedEvent` and `ClockEvent.Day`, or add a `ChangeTickEvent` to the event table; add `world.catchup.LoadGate` (holding a chunk's send until its catch-up finishes) to the threading rules.
- **Entities and AI.** Ecology's blocks (dens, push-ups, redds, nests, middens) submitted as World edits ops, not direct `setBlock`; trail wear read as 2 m cells; path-type maluses on the table above.
- **Player systems and client.** One dropped-item rule: the lead says items last 7 game days, the client section says they never despawn and merge into `ground_pile`. The world side works with either, but `ground_pile` blocks must set `placed_mask` and block pruning. Also the PresetEditor screen, the F3 lines, and the `current_chunk` and `sign_chunk` handlers.
- **Content pipeline and build.** Datagen for the 150-odd world blocks (blockstates, models, loot tables, tags), the template validation test (no jigsaw, no banned vanilla block), layer hashes in CI on all platforms, and the save rule for a generator version bump (old chunks kept, new chunks on the new version, with matching seams).
- **Building and crafting department.** Dig permit boxes from piece validation; piece HP at 0 submitted as `DECAY` ops; `placed_mask` bits set by frames and pieces.

## Entities and AI

Every living thing in the valley is one of ten entity base classes, driven by one Brain setup and one shared state machine, and placed only by the population model. This section is the entity developer's build plan for all 163 entity types across the eight tabs that define animals. It is also the owner's mob review: what each system needs, what was missing, and which cross-tab numbers are now settled.

### Scope, counts and what this section settles

**The cast in entities.** The tabs define 157 living and group entity types plus 6 carcass types, 163 in all, and about 22 client particle types (bumblebee and dragonfly are particles, DIRECTOR RULING 101) for the small and the many. The table counts them by group; the full list is in The entity registry, below.

| Group | Entity types | Defined in |
| --- | --- | --- |
| Land roster | 46: 43 mammals (2 of them livestock; the mule deer and Sitka black-tailed deer behind the flag yearofice:deer), the bat, the wood frog and the hen | Blueprint, Bestiary, Land animals in depth, Ideas from viewers |
| Birds | 78 species plus `yearofice:bird_flock` | Bestiary, Birds and the air, Waters and fish, Water, calls and encounters |
| Fish | 24 species plus `fish_school` and `tadpole_school` | Waters and fish, Water life in depth |
| Small water life | `boreal_toad` | Water life in depth |
| Insects and spiders as entities | 5, with `diving_beetle` | Bestiary, Land animals in depth, Birds and the air, Water life in depth |
| Carcasses | `carcass_tiny`, `carcass_small`, `carcass_medium`, `carcass_large`, `carcass_huge`, `salmon_carcass` | Bestiary › Carcasses and bodies, Waters and fish |

**Settled here.** This section resolves, as decisions, Director items 1, 2, 10, 30, 32, 33, 36, 40, 41, 43, 44 and 45, and the entity parts of item 6. It also settles 24 new conflicts found in the review (Settled across tabs, below). Where a decision changes another tab, that tab keeps its design and changes only its build line.

**Reads and writes.** Entities sit on the ten shared layers of Architecture and the interlace and reach them only through `YoiApi`. The Interlace table, below, names every read and write.

### The entity class tree

**Ten base classes cover every entity.** Each sits in `com.yearofice.ecosystem` and registers through one `DeferredRegister<EntityType<?>>` in `ecosystem.EcosystemEntities`. Living animals extend `PathfinderMob` through `YoiAnimal`, never `Animal`, `AgeableMob`, `TamableAnimal` or `NeutralMob` (Bestiary › No vanilla mob mechanics). Cheap group things extend plain `Entity`.

| Class | Extends | Used by | Adds | Server cost each |
| --- | --- | --- | --- | --- |
| `YoiAnimal` | `PathfinderMob`, `GeoEntity` | Every living animal | Animal record link, Brain, awareness, hit zones, wound, tier hooks, all no-vanilla overrides | — (abstract) |
| `LandAnimal` | `YoiAnimal` | 41 land mammals, wood frog, boreal toad | `YoiGroundNavigation`, gaits, path profile, swim and climb controls | 0.10–0.20 ms Active |
| `SmallMammal` | `LandAnimal` | Voles, shrews, lemmings, jumping mouse, pika | Bubble tier, a 6-behaviour brain, burrow and snow-tunnel moves | 0.02 ms |
| `Livestock` | `LandAnimal` | Hen, goat, reindeer | Care record, bond, pen home, calendar breeding, `Leashable` | 0.05 ms |
| `YoiBird` | `YoiAnimal` | 78 birds, the bat | `FlightController` with 11 flight states, perch, plumage | 0.10 ms near, 0.03 ms far |
| `BirdFlockEntity` | `Entity` | `yearofice:bird_flock` | Count, centre, velocity, seed; client boids | 0.04 ms |
| `YoiFish` | `YoiAnimal` | 24 fish | `FishMind` at 10 Hz instead of a Brain, swim move control | 0.015 ms |
| `CritterEntity` | `YoiAnimal` | 5 insects and spiders | No Brain; a scripted 4 Hz mover inside 8 blocks | 0.005 ms |
| `SchoolEntity` | `Entity` | `fish_school`, `tadpole_school`, run schools | Count, centroid, heading, state; client boids | 0.01 ms |
| `CarcassEntity` | `Entity`, `GeoEntity` | 5 carcass types, `salmon_carcass` | The carcass record (Bestiary › In Minecraft: carcasses) | 0.008 ms |

**Rig bases.** `LandAnimal` has abstract rig subclasses in `ecosystem.species.rig`: `UngulateEntity`, `CanidEntity`, `FelidEntity`, `UrsidEntity`, `MustelidEntity`, `LargeRodentEntity`, `SmallRodentEntity`, `LagomorphEntity`, `PorcupineEntity`. Species classes are `ecosystem.species.<Name>Entity`. Birds are `ecosystem.species.bird.<Name>Entity`, insects `ecosystem.species.insect.<Name>Entity`, and fish `ecosystem.fish.<Name>Entity`, per the lead's package map. Species whose only differences are data use the rig class directly with a species id, so a chickadee needs no Java class of its own.

**Insects.** Mourning cloak, wolf spider, yellowjacket, diving beetle and snow fly are critters: an 8-block life radius, one hit box and no hit zones. Beyond 8 blocks the yellowjacket draws as the separate particle `yellowjacket_swarm` (Conflicting ids row 11), and the others are not drawn. Bumblebees and dragonflies are particles only, never `CritterEntity` (DIRECTOR RULING 101).

**Not entities.** Bumblebees and dragonflies (the dragonfly keeps its `wing_rattle` sound at 3 m), biting flies, hatches, moths, bot flies, blow flies, snow fleas, water striders and spider eyeshine are client particles. Server pressure numbers drive them. Carpenter ants are a log block state. The bark beetle is an event plus a block. The Yukon floater is a state of `yearofice:mussel_bed`, not an entity.

### The animal record and what is saved

**The record is the animal; the entity is its body near the player.** Ecology keeps every wild individual as a record in its region file (`yearofice_region_<key>`). Big game, predators, named and Known animals and legends carry the full `AnimalRecord` below for life. Small game, small birds and fish carry a 24-byte slot record (species, slot, position, state) while their chunk is within 600 m; beyond that their count in Ecology is the truth.

| Field | Type | Notes |
| --- | --- | --- |
| `id` | UUID from `hash(seed, region, species, slot)` | Stable for life; the bull the player missed is the same bull |
| `species`, `sex`, `born_day`, `age_class` | Ids and ints | Age class drives scale, antlers, texture |
| `pos`, `activity`, `home_slot` | Block pos, activity id, slot index | Where the drives would have it now |
| `fat`, `condition` | 0–255 each | Seasonal looks and yields |
| `wound` | Zone, minute, bleed rate, death minute | Wounds that last 10–20 days |
| `damage` | 8 regions × state | Becomes the carcass damage map |
| `memory` | Wariness of the player, `hooked_before`, `call_shy_until`, hazed count, food conditioned | Survives demotion |
| `kin`, `group` | Mother, young, herd or pack id, role | For family and pack rules |
| `legend`, `known`, `mark` | Legend id, Known flag, mark seed | Exempt from caps |

**Wild entities are never saved into chunks.** `YoiAnimal#shouldBeSaved` returns false, and the body carries the attachment `yearofice:individual` (region key, record id). `ecosystem.tier.TierManager` writes its state back to the record on demotion, each game hour and on level save, and discards the body on chunk unload. This makes duplicates impossible: a world killed mid-save holds one record and no body. Livestock, raised young, tamed wolves and carcasses are the exceptions; they save with their chunk as ordinary entities, flagged `requiresCustomPersistence`.

**Synced.** The client gets only what it draws, through `SynchedEntityData`: species variant, sex, age scale, the 4 look bytes (coat, antler, fat, growth), activity id for animation, the legend or Known mark seed, and a `yearofice:look_target` entity id. Everything else stays on the server.

### Models, rigs and animation controllers

**Thirty-five GeckoLib rigs carry 159 living entity types.** Species on one rig share bone names and one animation set. The engine tab's art budget of 20 animal models is superseded: the cast needs 35 rigs (the 35th is the deer's `cervid`, RECONCILE 154), about 190 species texture sets and 5 carcass skeleton plans.

| Rig family | Rigs | Bones | Species |
| --- | --- | --- | --- |
| Land (Land animals in depth › Rigs and animations) | Ungulate, cervid, canid, felid, ursid, long mustelid, large rodent, small rodent and lagomorph, porcupine | 12–30 | 43 mammals, livestock and the two deer included |
| Small land rigs (Bestiary) | Bat, frog, hen | 8–12 | Bat, wood frog, boreal toad, hen |
| Birds (Birds and the air › Rigs) | Grouse, dabbler, diver duck, goose and swan, loon and grebe, crane, shorebird, gull and tern, raptor soarer, raptor fast, owl, woodpecker, corvid, kingfisher and dipper, songbird | 12–20 | 78 birds |
| Fish (Water life in depth › Reading a sheet) | Salmonid, esocid, gadid, coregonid, catostomid, small fish, lamprey | 6–14 | 24 fish |
| Critter | One 6-bone insect rig | 6 | 5 insects and spiders |

**Files.** Land species have their own geometry, `assets/yearofice/geo/entity/<id>.geo.json`, since body shapes differ. Birds, fish and critters share `geo/entity/rig_<rig>.geo.json` with a per-species `scale` from the species file. Animations sit in `animations/entity/rig_<rig>.animation.json` plus `<id>_extra.animation.json`; the datagen task `AnimationMergeProvider` merges them into `<id>.animation.json`, because one GeckoLib model points at one animation file. Every species with a tiny, small or medium carcass also carries the `carried_arms` and `carried_shoulders` poses in its rig set, which the client draws for a carried carcass (request 87); large and huge carcasses travel as quarters on plain baked models. Textures are `textures/entity/<id>/<variant>_<coat>.png`.

**Controllers.** Every animal registers its controllers in `registerControllers(AnimatableManager.ControllerRegistrar)`.

| Controller | Plays | Driven by | Rigs |
| --- | --- | --- | --- |
| `base` | Gait, state loop: idle, graze, walk, trot, gallop, swim, flap, glide, hold | Synced activity id and speed, 4-tick blend | All |
| `action` | One-shots: warn, howl, fight, takeoff, strike, die; the encounter clips below | `triggerAnim` from the server (`TriggerableAnim`), one packet per event | All |
| `ears_tail` | Ear flicks and tail swishes every 2–6 s, faster in bug season | Client timer seeded by entity id | Land rigs |
| `head` | Head and neck yaw toward the look target | `setCustomAnimations` reading `look_target`, no keyframes | Land and bird rigs |
| `wings` | Flap rate, fold, mantle | Flight state and vertical speed | Bird rigs |
| `fins` | Fin scull, gill rate | Current speed at the fish | Fish rigs |

**Encounter clips (RECONCILE 171).** The bear, moose and wolf encounters in Blueprint › Danger in detail get hand-keyed clips on the `action` controller, so a contact reads on screen as it does in the Forge Labs video. They live in each species' `<id>_extra.animation.json` (`grizzly`, `black_bear`, `moose`, `wolf`, `coyote`), made in Blockbench or as modelgen `_extra` files (Art and audio production › The model generator), and the server fires them with `triggerAnim` from the standoff and contact states in `ecosystem.ai`.

| Rig | Clip | Length | Fired by |
| --- | --- | --- | --- |
| Ursid | `huff_jaw_pop` | 1.2 s | Warn: the huff and jaw-pop |
| Ursid | `stand_sniff` | 3 s | Alert at range: stands to scent the player |
| Ursid | `bluff_charge` | Run loop, then a 0.6 s skid-stop | A charge that is a bluff |
| Ursid | `swat` | 0.5 s | Contact: the knock-down |
| Ursid | `pin_bite` | Loop of 1.5 s per bite | Each bite: head down, forepaws planted over the player's position |
| Ursid | `feed` | Loop | Feeding on a carcass, and the predatory turn past the fourth bite |
| Ursid | `flinch` | 0.3 s | A fight-back hit |
| Ursid | `retreat_look_back` | 2 s | Leaving after contact, or driven off |
| Ursid | `climb` | Loop | Black bear only, up a trunk |
| Ungulate (moose) | `ears_back_hackles` | 1 s | Warn |
| Ungulate (moose) | `charge` | Run loop | A charge |
| Ungulate (moose) | `front_stomp` | 0.6 s | Contact: each stomp |
| Ungulate (moose) | `bluff_stop` | 0.8 s | A bluff that stops short |
| Canid | `circle` | Procedural from the walk | Wolves testing a weak player |
| Canid | `lunge_snap` | 0.5 s | A test lunge or a cornered snap |
| Canid | `howl` | As in `action` | Unchanged |

**Lining up the pin.** At contact the server sets the bear's yaw and position so `pin_bite` puts its head over the player's body at ground level, and it holds them for the loop. The player's side of the same sequence is in First-person feel and camera.

| Step (Contact with a bear) | Bear clip | Player | Sound |
| --- | --- | --- | --- |
| 1. Knocked down | `swat` | Knock-down camera; hands fly out of view | `entity.grizzly.huff`, `body.hit_grunt` |
| 2. Play dead (hold Shift) | `pin_bite` loop | Face-down roll, 85% dark, hand clip `cover_neck` | First bite: `item.pack_tear` |
| 3. Each bite | `pin_bite` | Bite jolt | `body.bite_crunch`, `body.breath_panic` |
| 4. It leaves; stay down 30 s | `retreat_look_back` | Hold timer, after warn-once `stay_down` | Breathing |
| 5. Feeding turn, fight back | `feed`, then `flinch` on a hit | Swing clip `desperate_strike` | The roar turns to quiet feeding sounds |
| Get up | — | Hand clip `push_up` | — |

**Build.** Clips: ursid and moose in M6 with big game, canid in M6. Tests: GameTest `yearofice:encounter/bear_contact_sequence` (M6): a defensive grizzly bites 2–4 times, its triggers arrive in the clip order of the table, and holding Shift plays dead; `modelgen --check` proves every encounter clip's bones exist on the ursid, ungulate and canid rigs.

**Death plays once and hands off.** `die` runs 20–40 ticks with AI off; on its last frame the server builds the carcass (Carcass hand-off, below). GeckoLib's tip-over is off: each renderer returns 0 from `getDeathMaxRotation`, and the red overlay never shows because `hurtTime` stays 0.

**Looks on the wire.** `SeasonalLooks` sets the 4 look bytes once a game hour from the Clock and calendar (Land animals in depth › Seasonal looks). Bird plumage and fish stage use the same byte slots: plumage window index and life stage. The client never computes a date.

**Legends and Known animals.** A legend uses a texture variant (`one_eye`, `white`, `black`, `giant`) and the `mark` bone. Known animals get a mark from their id: torn ear, odd antler, pale saddle or scar, drawn as a render layer `KnownMarkLayer` with 12 overlay textures per rig.

### Brain: sensors, memories and activities

**One Brain provider per body plan, built from data.** `ecosystem.ai.YoiBrain.provider(species)` lists the memories and sensors a species needs, and `YoiBrain.makeBrain` adds activities from its `behavior` block. Memory types, sensor types, activities and schedules register through `DeferredRegister` on `Registries.MEMORY_MODULE_TYPE`, `SENSOR_TYPE`, `ACTIVITY` and `SCHEDULE`. No vanilla goal or vanilla sensor runs on our animals; `Mob#registerGoals` is empty.

**Sensors.** Each runs on its own period, staggered by entity id so 40 animals never sense on the same tick. Periods double between 64 and 150 blocks (Performance, below).

| Sensor | Period (ticks) | Range | Reads | Writes |
| --- | --- | --- | --- | --- |
| `yearofice:sight` | 5 | Species `sight` × light × cover × posture | Line-of-sight rays against blocks, leaves as half cover, the light level from the Environment field | `visible_threats`, `visible_prey`, awareness gain |
| `yearofice:hearing` | 10 | Species `hearing` × surface | Noise events from the Scent and sound field (`noise_m` of actions, footsteps, calls, alarms) | `heard_noise`, `heard_call`, awareness gain |
| `yearofice:scent` | 20 | Species `smell` × wind speed | The player's downwind cone and scent points from the Scent and sound field | `smelled_threat`, `food_target`, awareness gain |
| `yearofice:awareness` | 5 | — | The three sensors above | `awareness` per source (0–100), `threat`, `look_target` |
| `yearofice:herd`, `kin` | 10, 20 | 16 blocks | Group members | `herd_leader`, `mother`, `young` |
| `yearofice:fire` | 40 | 64 blocks | The heat index and lit light sources | `fire_avoid` |
| `yearofice:light` | 10, night only | Player light cone | Held light and lanterns | Eyeshine flag, `fire_avoid` for lantern-shy species |
| `yearofice:water_draw` | 200 | Species draw range | Water features (Water, calls and encounters › The draw model) | `water_target`, `sodium` |
| `yearofice:call` | On event | Call range | `CallPerformedPayload` broadcasts | `heard_call`, `call_shy_until` |
| `yearofice:scavenger` | 200 | 300 blocks | Carcass records and predators | `food_target`, `follow_other` |
| `yearofice:structure` | 200 | 64 blocks | Structure POIs: caches, racks, pens, doors, latrines | `camp_target` |
| `yearofice:perch` | 40 | 64 blocks | `PerchIndex` points (birds) | `perch_target` |

**Memories.** The full set, joining the engine's, Land animals in depth's and Water, calls and encounters' lists. All are `MemoryModuleType` registrations with a codec only where the value must survive demotion (marked S); those copy into the record.

| Memory | Type | Memory | Type |
| --- | --- | --- | --- |
| `awareness` | Map of source to 0–100 | `threat`, `food_target`, `look_target` | Entity or pos |
| `wound` (S) | Zone, minute, bleed | `flee_from`, `chase_start` | Pos |
| `home_slot` (S), `den_pos` (S), `yard_pos` (S) | Global pos | `trail_path` | Trail node list |
| `herd_id` (S), `herd_leader`, `pack_role` (S) | UUID, enum | `mother` (S), `young` (S) | UUID, UUID list |
| `fire_avoid` | Pos and radius | `camp_target` | Pos |
| `camp_aversion_until` (S), `hazed_count` (S), `food_conditioned` (S) | Game minute, ints | `follow_other` | UUID |
| `water_target`, `sodium` (S), `last_drank` | Pos, float, minute | `head_under` | Bool |
| `heard_call`, `call_shy_until` (S) | Call id, minute | `encounter_role` | Enum: pass\_by, block\_trail, curious, watch\_me, none |
| `perch_target` | Perch point | `trust` | Read from the wolf's attachment |

**Activities.** The 13 states of Blueprint › How animals act are activities, with the added ones from the other tabs. Vanilla's `CORE` activity holds the always-on behaviours: look, swim, move along path, wound bleeding, breath. A priority list picks the active one; drives choose inside Routine.

| Priority | Activity | Source |
| --- | --- | --- |
| 1 | `yearofice:attack`, `charge`, `warn` | Blueprint |
| 2 | `yearofice:flee`, `freeze`, `wounded` | Blueprint |
| 3 | `yearofice:ring_defense`, `defend_young` | Land animals in depth |
| 4 | `yearofice:chase`, `stalk` | Blueprint |
| 5 | `yearofice:staged_encounter` | Water, calls and encounters |
| 6 | `yearofice:alert` | Blueprint |
| 7 | `yearofice:feed_on_kill`, `investigate`, `lured`, `respond_to_call` | Blueprint, Water, calls and encounters |
| 8 | `yearofice:herd_flow`, `yard`, `follow_other`, `nurse` | Land animals in depth |
| 9 | `yearofice:water_visit` | Water, calls and encounters |
| 10 | `yearofice:routine` (feed, drink, travel, rest); `yearofice:hibernate` replaces it in a sleep window | Blueprint, Land animals in depth |
| — | Wolf only: `companion_follow`, `guard`, `fetch`, `haul`, `come` | Open world in depth, Taming |
| — | Livestock only: `pen_routine`, `graze_out`, `go_home` | Ideas from viewers › Raising animals |

**Drives tick on the Clock and calendar, senses on ticks.** Sensors and awareness run in real ticks, because animals always move at 20 ticks a second. Drives (hunger, thirst, rest, sodium, rut, fear of camp) step once per game minute on `ClockEvent.Minute`, in closed form when several minutes pass. The pure model `sim.animal.Drives.step(record, minutes, env)` returns the Routine sub-activity, so the balance simulator runs the same code headless.

### The shared state machine

**Awareness is the only door between states.** `sim.animal.Awareness` holds one score per source. Gains per tick come from the sensors; decay is 2 points a real second when no sense fires, 5 in cover. Thresholds stay 30 (Alert), 70 (species response) and 100 (exact position).

```latex
\Delta a = \sum_{s \in \{sight,\,hearing,\,scent\}} g_s \cdot \left(1 - \frac{d}{R_s}\right) \cdot m_s \qquad g_{sight} = 40,\ g_{hearing} = 25,\ g_{scent} = 15\ \text{per second at } d = 0
```

R\_s is the species range for that sense after posture, light, wind and cover; m\_s is 1.5 for a running player and 0.33 crouched. A still, crouched player at half range with the wind in their face reaches 70 in about 7 s; the same player walking upright reaches it in about 2 s.

| State | Activity | Animation | Sound | Exit |
| --- | --- | --- | --- | --- |
| Routine | `routine` + drive | idle, graze, browse, walk | Species routine calls by schedule | Awareness 30 |
| Alert | `alert` | `alert`, head to threat | The species tell | 70: response; under 20: Routine, wary 10 game minutes |
| Freeze | `freeze` | `freeze` | None | Inside flush distance: Flee |
| Flee | `flee` | `flee`, `gallop` | Alarm, crash | Flee distance reached |
| Investigate | `investigate` | `walk`, sniff | — | Food found or player seen within 50 m |
| Lured | `lured`, `respond_to_call` | `walk` on a curving path | Answers | Spooked or arrived |
| Stalk | `stalk` | `walk` crouched | Silent | Strike distance |
| Chase | `chase` | `gallop` | Run sound | 150 m from start, fire within 8 m, torch, escape |
| Warn | `warn` | `warn` | Huff and jaw-pop, grunt, growl, tail slap | Backs off: Alert. Holds: Charge |
| Charge | `charge` | `charge` | Run, roar | Bluff stops 3–8 m short; contact: Attack |
| Attack | `attack` | `fight_*` | Roar, bite | Danger in detail rules |
| Feed on kill | `feed_on_kill` | `forage`, `tear` | Tearing | Carcass gone or driven off |
| Wounded | `wounded` | Limp layer on gait | — | Dies or heals |

**The charge gate.** `ChargeGate` refuses to start Charge unless Warn ran at least 30 ticks (1.5 s) within the last 80, or 20 ticks (1 s) on a surprise. This enforces "every charge is announced" in code, not in data, and the Blueprint's 1,000-encounter test runs against it.

**Close clock.** While an animal is in Stalk, Chase, Warn or Charge against a player within 160 blocks, it calls `TimeService.requestRate(player, CLOSE, "predator")` each tick; leaving those states ends the request. Hold-to-work jobs stop in the same tick (Minecraft engine › Work, previews and minigames).

### Pathfinding in voxel terrain

**One node evaluator, many profiles.** `YoiNodeEvaluator extends WalkNodeEvaluator` reads a `PathProfile` from the species file and NeoForge's `IBlockExtension#getPathType` on our blocks. Vanilla clearance already uses `floor(width + 1)` blocks, so a 1.4-block moose needs a 2 × 2 gap and 3 blocks of height; the generator's 3 m minimum tree spacing keeps 2 m gaps open.

| Profile field | Moose | Caribou | Grizzly | Wolf | Hare | Red squirrel |
| --- | --- | --- | --- | --- | --- | --- |
| `step_height` | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0, climbs |
| `max_drop` | 2 | 2 | 3 | 3 | 2 | 6 |
| `wade_depth_m` | 1.6 | 1.0 | 1.0 | 0.6 | 0.1 | 0 |
| `swims` | Yes, 1.8 m/s | Yes, 1.6 | Yes, 1.5 | Yes, 1.2 | 0.6, avoids | 0.3, avoids |
| `brush` | Pushes through | Avoids | Pushes through | Through | Under | Over |
| `snow_sink_cm` per 10 cm | 3 | 5 | 6 | 8 | 1 | 1 |
| `max_nodes` | 400 | 300 | 400 | 300 | 150 | 150 |

**Willows and alder are brush, not walls.** Willow, alder and dwarf birch thickets are `yearofice:brush` blocks from the world developer's tree builders. Their collision shape is empty for entities, and `entityInside` slows whatever is inside: moose ×0.9, bear ×0.85, wolf ×0.8, player ×0.6. Anything over 40 kg crashing through makes a 10 m noise; species that avoid brush pay path malus 2. Trunks stay solid. So a moose beds in a willow flat and walks out of it the way a moose does.

**Long trips use the trail graph.** Stage 7 trails are a node graph in the region file. `TrailGraph.route(from, to)` runs A\* on trail nodes (cheap, precomputed costs), and vanilla A\* only plans each hop of 32 blocks or less between nodes. A path is recomputed only when its target moves over 4 blocks or every 40 ticks, and at most 4 path searches run per server tick across all animals; the rest queue.

**Stuck rule.** An animal that makes under 0.5 m of progress in 60 ticks on an active path re-plans once. Still stuck and unseen (outside every player's view cone, over 24 blocks away), it teleports to the next trail node, as the engine tab sets. A seen stuck animal turns and picks a new goal instead, so the player never sees a blink.

**Snow and yards.** Snow depth comes from the Environment field per column. Each 10 cm sinks the animal by its `snow_sink_cm` and slows gaits; over 90 cm a moose's gaits halve. Packed cells come from the trail-wear attachment (2 m cells, RECONCILE 96); yard moose path with off-trail cost ×4 in deep snow, which the Land tab's yard test measures. A crust (`crust=true`) carries foxes, lynx and wolves at full speed and breaks under moose and caribou.

**Caribou on open tundra steer by flow field.** `HerdController` gives each herd one leader path and a 4 m flow field along its corridor, rebuilt every 100 ticks. Members follow it with boids rules (separation 2 m, alignment within 8 m) and cost no A\* at all. Fords are `poi_ford` points; the leader crosses only where depth is under 1 m and current under 1.2 m/s, or swims at its swim speed against the current push.

**Swimming and diving.** Surface swimmers (moose, caribou, bears, wolves, foxes, lynx, hares in need) use `YoiAmphibiousNavigation` and float with the head clear; the engine's current push applies at their swim speed. Divers (beaver, muskrat, otter, mink, loons, ducks, grebes, dipper) get `DiveControl`: a breath timer from `dive_s`, a dive target, and surfacing only at open water. Under ice the engine's solid ice blocks hold them down, so they surface only at holes, leads, lodges and push-ups, which `WaterBodyIndex` lists as air points.

**Climbing.** Climbers (red and flying squirrels, marten, porcupine, black bear, cubs of both bears, wolverine, lynx, fisher) use the tree ids the generator stores per chunk. `ClimbControl` attaches to a trunk column, sets `onClimbable` true and moves vertically at the species climb speed (squirrel 2 m/s, marten 1.5, black bear 0.5, porcupine 0.3). Leaf blocks hold branch nodes; squirrels jump trunk to trunk up to 2 m, and the flying squirrel glides at a ratio of 3 to a trunk up to 8 m away. Dall sheep and mountain goats get `step_height` 1.5 and 2.0 on blocks tagged `yearofice:rock_steps` through an attribute modifier while standing on them.

**Flight.** Birds never use `FlyingPathNavigation`. `FlightController` steers by waypoints and 3 rays at 4, 8 and 16 blocks (Birds and the air › Flight states); gravity is off while flying and back on when perched, swimming or walking. Bats use the same controller with the bat body plan: cruise 6 m/s, tight turns, no gliding.

**Pieces, fences, fire and ice.** The world developer gives our blocks path types: closed doors, walls and pens are `BLOCKED`; open doors `DOOR_OPEN`; gates in pens block livestock; a lit `fire_ring` is `DAMAGE_FIRE` with the fire sensor's keep-off radius on top. Thin and rotten ice from the Environment field adds malus 8 for moose and caribou; wolves chasing caribou onto glare ice get the Water, calls and encounters speed multipliers (caribou 0.4, wolf 0.7).

**Swim speeds, settled (Director item 33).** One table replaces the per-tab values.

| Species | Swim m/s | Species | Swim m/s |
| --- | --- | --- | --- |
| Moose | 1.8 | Beaver | 0.7 surface, 1.5 under water |
| Caribou, reindeer | 1.6 | Muskrat | 0.5 surface, 1.0 under |
| Grizzly | 1.5 | River otter | 1.0 surface, 3.0 burst under |
| Black bear, wood bison | 1.4 | Mink | 0.8 surface, 1.2 under |
| Wolf, coyote | 1.2 | Dall sheep, mountain goat, red fox | 0.8 |
| Muskox, lynx, wolverine | 1.0 | Player | 1.0, crawl 1.4 |

### Hitboxes, hit zones and the damage map

**The box is for collision; the zones are for shots.** Minecraft's box is square in plan, so a long body's length lives in its hit zones. Boxes change by pose through `getDefaultDimensions(Pose)` and `refreshDimensions`: a grizzly is 1.3 × 1.5 on all fours and 1.0 × 2.4 standing. Young scale the box by age class through `EntityDimensions.scale`, so a calf needs no separate entity type.

**Hit-zone data.** `data/yearofice/hitzone/<species>.json` holds ellipsoids in the entity's body frame, one set per pose: standing, bedded, running, swimming, flying, perched. The server transforms the projectile's segment into body space by the entity's yaw and pitch and takes the first zone entered (Blueprint › The hit-zone model). GeckoLib bones are never read on the server.

```json
{
  "species": "yearofice:moose",
  "poses": {
    "standing": [
      { "zone": "heart_lungs", "center_m": [0, 1.35, 0.55], "radii_m": [0.25, 0.225, 0.25], "shoulder_front": 0.3 },
      { "zone": "liver", "center_m": [0, 1.35, 0.2], "radii_m": [0.15, 0.125, 0.15] },
      { "zone": "spine", "center_m": [0, 2.0, 0], "radii_m": [0.04, 0.04, 1.1] },
      { "zone": "brain", "center_m": [0, 2.1, 1.55], "radii_m": [0.05, 0.05, 0.05] },
      { "zone": "gut", "center_m": [0, 1.4, -0.25], "radii_m": [0.45, 0.45, 0.7] },
      { "zone": "legs", "below_y_m": 1.0 }
    ],
    "bedded": { "from": "standing", "lower_m": 0.9 }
  },
  "kill_threshold_j": 30,
  "bloodshot": { "heart_lungs": ["ribs", "front_left", "front_right"], "gut": ["loin", "hind_left", "hind_right"] }
}
```

**The resolver is pure.** `sim.hunt.HitResolver.resolve(shot, zones, threshold)` returns the zone, energy, outcome and the regions marked bloodshot. The adapter `ecosystem.wound.HitAdapter` runs from our arrow's `onHitEntity`, from spear and knife use through `AttackEntityEvent`, and from animal attacks. Every vanilla damage to our animals is cancelled in `LivingIncomingDamageEvent`; the only ways to die are the resolver, `FallRules` (over 6 m), drowning by the animal's own breath, wildfire, lightning, starvation in Ecology and old age.

**Tiny targets are clickable.** Entities under 0.25 blocks wide get a pick margin of 0.15 blocks through `getPickRadius`, so a vole, a frog or a perched redpoll can be inspected or hit with a blunt. Hit zones do not grow; a miss still misses.

**Kill thresholds for species the tabs left without one.** The Blueprint table stands; these fill the gaps at the same energy scale.

| Species | Kill threshold | Species | Kill threshold |
| --- | --- | --- | --- |
| Wood bison | 40 J | Coyote | 12 J |
| Muskox | 34 J | Fisher, arctic fox, woodchuck, hoary marmot | 8 J |
| Mountain goat | 24 J | Alaska hare, ground squirrel, lemmings, voles | 2 J; a blunt kills |
| Reindeer | 26 J | Trumpeter swan | 8 J |
| Livestock goat | 22 J | Geese, loons, crane, eagles, owls over 1.5 kg | 4–6 J |

**The damage map is born alive.** Each hit, bite, fall or scavenger bite writes into the record's 8-region damage map at once. When the animal dies, the map copies straight into the carcass record, so a wolf-killed moose shows its torn hindquarters and a shoulder-struck arrow shows bloodshot on the carcass card.

**Wounds that last.** A non-lethal hit writes the `wound` memory and record field: zone, minute, bleed rate and a death minute for liver and gut hits. `WoundHealer` is the only code that heals; a leg wound heals in 10–20 game days and shows as a limp layer on the gait animation and limp tracks in the sign attachment.

### Animal-to-animal interactions

**Every interaction between two animals runs on the same awareness, Brain and resolver as one with the player.** A wolf is a threat source to a moose exactly as the player is.

**Predation where the player can see it.** Inside the Active tier, a predator enters Stalk and Chase on its prey and the prey runs its Flee. `sim.animal.Predation.chase(pred, prey, snow, ground, condition)` rolls each chase's outcome once at its start, from the rates in Blueprint › Who hunts whom. Then it plays out: a success ends in a bite through the resolver, a failure in a broken-off chase at 150 m. Wolves hamstring and open hindquarters; lynx bite the neck; bears swipe the back. The attack profile is a species block, so the damage map reads true. Against the player, the same profile calls `BodyService.applyInjury(player, injury, cause)` with the zone and joules in the injury, never vanilla damage.

```json
"attack": { "kind": "bite", "energy_j": 60, "zones": ["legs", "gut"], "regions": ["hind_left", "hind_right"], "reach_m": 1.2, "cooldown_ticks": 20 }
```

**Predation where the player cannot see it.** Nearby and Abstract kills come from the daily Who hunts whom rates in Ecology. Each kill of a large or huge animal within 2 km of the player becomes a found-carcass record (Bestiary › Carcasses in the world), capped at 6 per region and 2 large or huge. The encounter director may stage the next one in view instead (Water, calls and encounters › Staging).

**Scavenging.** `Scavenging` applies the Bestiary's eating order to carcass records every game hour. Within 160 blocks of a player it materializes the real feeders as Active entities, ravens and magpies as a ground flock, under the Active cap; farther out it changes only the record.

**Herds.** One `HerdController` per herd in region data: caribou, wood bison, muskox, Dall sheep bands and mountain goat bands. It owns the leader, the flow field, the alert wave (a neighbour within 15 m turns Alert after 0.3–0.8 s), the muskox ring and impostor counts. Members are entities only inside the herd budget (Tiers, below).

**Packs.** `PackController` holds roles (breeding pair, yearlings, pups, sentinel), the territory border as scent posts in the Scent and sound field, the hunt plan and the winter test of a weak player. A lone wolf avoids scent posts. Killing the breeding pair scatters the pack by the Populations rule.

**Families.** `KinTracker` keeps mother and young. Hiders (leverets, moose calves, porcupettes) sit as records in their spot until the mother's dusk visit; a mother's defend radius is a `defend_young` trigger; a dead mother sets `orphan` on her young for the raising rules.

**Alarms cross species.** A flushing bird, a scolding squirrel or a marmot whistle emits an alarm event in the Scent and sound field with its range. Every animal whose hearing covers it gains 15 awareness (25 for jays, magpies, ravens, yellowlegs and terns), which is the flush chain in Birds and the air. Every call is played through `SoundBus.play`, which emits the noise itself, so the ear and the animals agree. The player learns helpers through `HudBus` lines (Messenger and HUD bus) in Bestiary › Animals that help the player.

**Who gives way.** At a carcass, a grizzly displaces everything, a black bear displaces wolves only alone, wolves displace a wolverine 50% of the time, and a fox or arctic fox follows a bear at 30–60 m (`follow_other`). These are a priority list in `sim.animal.Dominance`, read by `feed_on_kill`.

### Tiers, spawning and despawning

**Eight forms, one truth.** Ecology's counts are the truth; every form below is a view of them. Distances are from the nearest player.

| Form | What it is | Distance | Tick | Saved | Cap |
| --- | --- | --- | --- | --- | --- |
| Abstract | A count N per region and species | Anywhere | Daily at 00:00, hourly for runs | `yearofice_populations` | — |
| Record | One `AnimalRecord` with no entity | Unloaded chunks within 600 m, and named animals anywhere | Once per game minute along trails | Region file | 200 Nearby, records included |
| Nearby entity | A real body with AI off | 150–600 m in loaded chunks | Moved per game minute | Never | Inside the 200 |
| Active | Full Brain | Under 150 m; demoted at 180 m | Every tick | Never (record written back) | 40 |
| Group member | A herd or flock follower with a 4 Hz follower brain | Under 96 blocks of the player | 4 Hz | Never | 60 per herd, 8 per flock |
| Bubble | Small mammals, critters | Under 32 blocks (critters 8) | 4 Hz | Never | 30 small mammals, 12 critters |
| Projection | Fish and water life | Water body within 48 blocks; removed at 64 | 10 Hz individuals, 5 Hz schools | Never | Water life in depth › Caps and cost |
| Impostor | Client-drawn members with no server entity | Herd beyond 96 blocks, flock and school members, sky flocks | Client frame | Never | 400 bird members, 160 school members (600 in a run reach), herds by LOD |

**Active caps are ceilings, not reservations.** The engine's 40 Active stays the total. Inside it, big game may use at most 12, predators 8, medium mammals 12, single birds 14 and flocks 6; these sum past 40 on purpose, so a quiet slough can hold 14 birds and a winter yard 12 moose. When the total passes 40, the farthest animal that is not stalking, wounded, aimed at, sketched, Known or a legend demotes first. The wolf companion and livestock never count.

**Promotion.** `TierManager` runs every 20 ticks. A record or Nearby entity within 150 m of a player promotes: its body appears at the record's position facing its path, with its activity resumed. Demotion at 180 m writes the record back. The 30 m hysteresis stops flicker at the edge.

**Spawning is placing a record's body.** `PopulationSpawner` draws individuals from N into seeded slots, `hash(seed, region, species, slot)`, when a chunk enters the Nearby band, so the same moose returns to the same willow flat. It places each by the species' placement type, registered in `RegisterSpawnPlacementsEvent`: `ON_GROUND`, `IN_WATER`, `yearofice:burrow`, `yearofice:trunk`, `yearofice:snow_den`, `yearofice:perch` and `yearofice:nest`. Every body it creates carries a transient `yoi_spawned` flag; `FinalizeSpawnEvent` cancels any mob spawn without it, which catches spawn eggs, `/summon` and other mods.

**Seasons and habitat gate slots.** Hibernators in their window are records in their burrow, never entities. Migrants exist only between their arrival and departure draws on the Clock and calendar. A slot whose habitat cell was cleared by World edits (a felled willow flat, a drained pond) moves to the nearest valid cell within the home range, or is dropped from K by Ecology.

**Nothing despawns by distance.** `removeWhenFarAway` returns false and `checkDespawn` does nothing. A body leaves the world only by demotion, death, being carried (carcasses) or being projected out (fish).

**Records and saves.** Full records are about 1,600 in the home valley at K, 220 bytes each, so about 350 KB. Slot records exist only near a player and cost under 20 KB at any time. This keeps the mod's save data inside the engine's 5 MB budget.

**Commands.** `/yoi spawn <species> [count]` writes records first and then promotes them, so test spawns are counted. `/kill` on our animals runs the death pipeline with cause `command` and takes 1 from N. Vanilla `/summon` of our types is refused outside creative test worlds.

### Carcass hand-off

**Death is a pipeline, and nothing drops.** Every cause runs the same seven steps in `ecosystem.wound.DeathPipeline`.

1. **Cause.** The resolver, `FallRules`, drowning, fire, lightning, starvation, old age or a command names the cause and the killer.
2. **Event.** `AnimalKilledEvent` posts on `NeoForge.EVENT_BUS` with species, cause, killer and position. The journal, the encounter director, Known animals, legends and the Messenger and HUD bus listen.
3. **Collapse.** AI turns off, the `die` animation triggers for 20–40 ticks, and a bird falls on a ballistic arc from its velocity.
4. **Record.** `CarcassFactory.fromAnimal` builds the `CarcassRecord`: species, sex, age class, cause, minute, the live damage map, wound, core temperature from the animal's body temperature, fat, the trichinella roll for bears, legend id and mark.
5. **Body.** The living entity is discarded with no drops, experience or death smoke, and a carcass entity of the species' size class spawns in the same spot and facing (Bestiary › The carcass entity). Birds become carcass entities, not item entities, which settles the Birds tab's wording.
6. **Counts.** A player kill calls `EcologyService.harvest`, which takes 1 from N at once, from the right sex; any other cause calls `reportDeath`. Predator and natural deaths in the Abstract tier were already counted by the daily step.
7. **Decay and scent.** The carcass record's `fresh` is stamped on the Decay and freshness layer (`DecayService.stamp`, with its k, size factor and the `soft_tissue` curve), and the carcass upserts its scent source (`ScentService.upsertSource`) by class and stage.

**Loot tables, settled (Director items 41 and 43).** Entity death tables `data/yearofice/loot_table/entities/<id>.json` are empty for every animal and `doMobLoot` is false. All parts come from `data/yearofice/loot_table/butchering/<id>.json`, one file per species, its pools gated by `yearofice:dressing_step`. The Land tab's `entities/` death tables and the species field `loot` retire; the species file names its table in `butcher_table`. Counts are in 100 g stack units: the Bestiary's bison example sets 1,800 meat units, not 180.

**Beaver meat, settled (Director item 40).** The beaver's carcass block reads `meat` 6.5 kg, `whole_kg` 18; the tail stays a separate 1.0 kg part.

**Fish.** A landed fish is already its body: an item with `yearofice:fish_data`. A fish killed and not taken is an item entity of its species with `alive` false that floats 2 game hours, then sinks and is removed; N was reduced when it died. Spawned-out salmon are `salmon_carcass` records in the run manager, with at most 40 entities per 100 m reach and the rest records that drift and lodge by date on load.

### Taming, livestock and raised young

**Only three kinds of animal live with the player, and none of them obeys like a vanilla pet.** The wolf is a companion, livestock are kept, and raised orphans stay half wild (Ideas from viewers › Raising animals, Taming a wolf without bones).

**The wolf.** One entity type, `yearofice:wolf`, for wild, pack, lone and tamed wolves. Its stage and trust live in the entity attachment `yearofice:trust` (stage, value 0–100, today's gains per action, last feeding minute). The per-action limits of Taming a wolf without bones apply first; the Open world in depth daily trust cap then clamps the day's total (Director item 30). The trust table's wording "once a day, 10 m" in Open world in depth points to the per-action table.

- **Commands.** G (whistle) and V (voice) send `yearofice:wolf_command` payloads; the server maps them to the `come`, `companion_follow`, `guard`, `fetch` and `haul` activities.
- **Pack and sled.** Saddlebags are a 6-slot container in the wolf's attachment. Harnessed wolves pull `yearofice:sled` through a `HarnessLink` that implements `Leashable`; up to 4 wolves pull, each adding its pull to the sled's limit.
- **Breeding.** A tamed female comes into season in February; a tamed or wild male within 1 km may sire a litter of 4–6 born May 1. The Black Wolf legend's rule plugs in here.
- **Leaving, by preset (Ruhan, 2026-10-19).** The dial `companion.wolf_leaves` in `yearofice-common.toml` is `never` on Gentle, `recoverable` on Standard and `permanent` on Brutal. The February roll is p = (80 − trust) ÷ 400 a game day, halved after a feed and a night under a roof (Open world in depth › Taming a wolf). A recoverable wolf that leaves keeps its entity id and `yearofice:trust`, and is held as a named wild record with a `return_day` 10–20 days out; it walks back into camp at 40 trust. Debug: `/yoi wolf leave <test|now>` and `/yoi wolf trust <0-100>`.
- **Saved.** A tamed wolf is not a wild record: it saves with its chunk, or with the player when it is following within 32 blocks at logout, so it never gets lost on a chunk unload.

**Livestock.** `Livestock` entities (`yearofice:hen`, `goat`, `reindeer`) save with their chunk and carry `yearofice:care`: fed, watered and warm 0–100, bond, home pen, sex, age and produce timers.

| Rule | Mechanism | Numbers |
| --- | --- | --- |
| Arrival | A radio drop spawns them at the drop site with the `yoi_spawned` flag | Prices from the radio catalogue only: 3 hens and a rooster 100 points, 2 goats 150, 2 reindeer 180 (Director item 28) |
| Needs | `ClockEvent.Hour` steps care from troughs (block entities) and the pen's room temperature from Warm rooms | Below 30 no produce; below 10 Condition loss; 0 for 2 game days: death |
| Hen laying | `sim.livestock.Laying` | Light factor 0 at 10 h of light rising to 1 at 14 h; temperature factor 0 below 0 °C, 0.5 at 0–5 °C, 1 above 5 °C; feed 0.1 kg a day at 5 °C or more, 0.15 kg below; fish meal counts 0.75 kg of grain per kg (Director item 29) |
| Goat milk | Hold-to-work milking action, no bucket on an entity | 1.5–2 L a day for 240 days after a May kidding (Director item 28) |
| Reindeer | Pack 35 kg; harnessed to a small sled 90 kg on snow | Walks off with wild caribou passing within 500 m in the fall migration if the paddock gate is open |
| Breeding | On the calendar from the species file, never from food | Kids in May, chicks from a broody hen in 21 days, a reindeer calf most springs |
| Predators | One nightly roll per pen when predators are in the region | Brush 25%, poles 8%, log stockade 2%; ×0.3 with a Bonded wolf near the pen |

**The rooster is a hen.** `yearofice:hen` with sex male draws the rooster texture and adds `entity.hen.rooster_crow` (300 m, dawn), a sound the tabs left out.

**Raised orphans.** A wild young with `orphan` set can be raised; it keeps its wild species id and gains `yearofice:bond`. Ecology removes it from the wild count while it lives in camp and returns it when it leaves, as Raising animals requires. Bond 60 or more lets it follow, come when called and take a lead; below 30 it wanders and may turn wild for good.

**Leads, amended.** No-vanilla row 13 now reads: cordage leads the Companion wolf, harnessed wolves, livestock and raised caribou or moose calves with bond 60 or more, plus towed vehicles. Only those implement `Leashable`. Every other animal still refuses a lead.

### Legends and named animals

**One legend system for animals and fish.** Creative concepts' five legends, Waters and fish's five legend fish, emergent legends, the rawhide-strip wolf and Known animals share one record and one model rule.

| Kind | Data | Where it lives | Placed by |
| --- | --- | --- | --- |
| Animal legend | `data/yearofice/legend/<id>.json`, `kind: animal` | `yearofice_legends` SavedData plus the animal's record | `LegendManager`: 3 of the 5 per world from `hash(seed, "legends")` |
| Legend fish | The same folder, `kind: fish`; Waters and fish's `legend_fish/` folder folds into it | The spot's individual slot | `LegendManager`, into its spot at world creation |
| Emergent legend | `data/yearofice/legend_rule/<species>.json` | The individual's record or fish slot | `EmergentLegendWatcher` on May 1 |
| Known animal | None: 3 encounters (2 for bears) | The record's `known` flag and mark seed | `KnownAnimals` |
| Habituated and named | The habituation rule | Record or slot flag | `Habituation` |

**Rules every legend keeps.** A legend is an ordinary animal of its species with a fixed weight and age. Its record never folds back into a count, and it is exempt from the Active cap's demotion order and the encounter director's post-kill dip. Packages follow the lead's map: `story.legend.LegendManager` and `EmergentLegendWatcher`, with `ecosystem.encounter.KnownAnimals`. The record's `legend` field is the truth; the entity attachment `yearofice:legend` mirrors it while the body exists, so the renderer and the carcass can read it.

**Rare sightings and legends share slots.** The White Cow and the Bestiary's white moose are the same animal: when `LegendManager` places the White Cow, the white-moose sighting does not roll that run; when it does not, the 2% sighting gives an unnamed white cow. The Black Wolf and the black-wolf sighting follow the same rule. The rawhide-strip wolf is a Known-style named record in the home pack with its trust capped at 25, as Open world in depth sets.

**On screen.** Legends use their texture variant and the `mark` bone; One-Eye's huff and the Black Wolf's howl are pitched variants of the species sound (`entity.grizzly.huff_legend`, `entity.wolf.howl_single`). The messenger never names a legend.

### No vanilla mob mechanics: the audit

**All 32 rows are implementable, with seven corrections.** The table gives the correction for each row that needed one; rows not listed stand as written in Bestiary › No vanilla mob mechanics.

| Row | Correction |
| --- | --- |
| 1 | Entity death tables stay as empty files so datapacks cannot fill them; `LivingDropsEvent` is cancelled anyway |
| 5 | `/kill` and `/damage` reach `LivingDeathEvent` without our resolver; the pipeline catches them with cause `command` instead of cancelling, so test tools still work |
| 8 | Override `die`, `tickDeath` and the renderer's `getDeathMaxRotation`; `deathTime` would otherwise still drive vanilla's tip-over on the client |
| 13 | Leads widened to livestock, harnessed wolves and bonded raised young (Taming, above) |
| 16 | Add `shouldBeSaved` false for wild animals; without it a demoted moose would also be written into its chunk and return twice |
| 25 | Override `playAmbientSound` to do nothing; the interval alone still lets vanilla play once on spawn |
| 30 | The world preset cannot lock difficulty; the server calls `setDifficultyLocked(true)` at Normal on start |

**Gaps: vanilla mob rules the list missed.** Each is now a row, with its replacement and mechanism.

| # | Vanilla behaviour | In Year of Ice | Mechanism |
| --- | --- | --- | --- |
| 33 | Land mobs drown after 15 s under water; fish suffocate on land at vanilla rates | Breath by species `breath_s` (moose 60, beaver 900, loon 120); fish vitality on land from Water life in depth | `LivingBreatheEvent` sets air use per species; drowning routed to the death pipeline |
| 34 | Suffocation damage when a box clips a block | A big animal clipped by a snow pass, a felled tree or a flood is nudged to the nearest free column | `inWall` damage cancelled; `PushOut` moves it up to 3 blocks |
| 35 | Players shove mobs by walking into them; mobs shove each other | A player cannot push a moose; herd members push each other gently | `isPushable` false for animals over 40 kg against players; `doPush` scaled by mass ratio |
| 36 | Boats and minecarts pick up any mob that touches them | Nothing boards by accident | Vanilla boats and minecarts hidden; our canoe, raft and sled accept only players, the Companion wolf and loads in `canAddPassenger` |
| 37 | Enchantments change damage and drops (Sharpness, Smite, Bane, Looting, Power, Flame, Fire Aspect, Piercing) | Arrow energy and hit zones only | Enchanting table, anvil and books hidden; the resolver ignores enchantment effects; enchantment recipes and loot filtered |
| 38 | Potions, tipped and spectral arrows put effects and glowing on mobs | Only the mod's own effects on the player | Brewing stand, potions and tipped arrows hidden; `MobEffectEvent.Applicable` denies vanilla effects on our animals |
| 39 | Sweeping melee hits several mobs; attack cooldown scales damage | One target, energy from the weapon data | Sweep cancelled (`SweepAttackEvent` where the pinned version has it, otherwise the attack event); melee goes to the resolver |
| 40 | Thrown eggs hatch chicks; snowballs knock mobs back | Eggs are food; snow is a block | Vanilla egg and snowball hidden; `wild_egg` and hen eggs are not throwable; snow cover drops nothing throwable |
| 41 | Lightning converts or kills mobs in vanilla ways | A strike is a burn wound or a death with cause `lightning` | `EntityStruckByLightningEvent` cancelled and routed to the resolver |
| 42 | Wander, look-at-player and stare goals | Animals look at a threat only when Alert | No goals; the `head` controller reads `look_target` |
| 43 | Sweet berry bushes, cactus and powder snow hurt or freeze mobs | Bushes slow but never hurt; cold kills animals only through Ecology's winter mortality | Our bushes and brush deal no damage; vanilla powder snow hidden; `canFreeze` false |
| 44 | Vanilla path types: mobs open doors, avoid only vanilla danger blocks | Doors are walls to animals; bears breach doors and caches only by rule | Our node evaluator and `getPathType` on our blocks (Pathfinding, above); breaching goes through World edits |
| 45 | Buckets capture fish and axolotls; buckets milk cows | Live fish go in the Water life in depth containers; goats are milked by a work action | Fish and axolotl buckets hidden; `PlayerInteractEvent.EntityInteract` cancels bucket use on entities |
| 46 | Shears on sheep, brushes on armadillos | Wool and qiviut shed onto the range | No entity responds to shears or brushes; shedding rolls daily (Land animals in depth › Loot tables and carcasses) |
| 47 | Dropped items despawn after 5 real minutes (Director item 36) | Dropped items last 7 game days, as the map pins do; animals take them only by the theft rules | Architecture and the interlace › Vanilla systems we replace: lifespan max on join, `ItemExpireEvent` against the drop's game-minute stamp; sunk gear becomes `lost_gear` |
| 48 | Babies are half-size copies that grow in 20 minutes | Age class from the record sets scale, texture and box | `isBaby` unused; `getDefaultDimensions` scales by age class |
| 49 | Vanilla tracking ranges and update rates per category | Big animals visible to the edge of the view, small ones near | `clientTrackingRange` and `updateInterval` per type (Performance, below) |
| 50 | Spawn eggs and `/summon` add uncounted animals | Test spawns are counted | `FinalizeSpawnEvent` without the `yoi_spawned` flag cancels; `/yoi spawn` writes records first |

**The no-vanilla tests grow by one.** `NoVanillaGameTests` adds test 10 for rows 33–50: a moose held under water dies at 60 s ±1 by our breath with cause `drowning`; a player walking into a moose for 200 ticks moves it 0 blocks; a vanilla boat dropped on 5 hares carries none; an enchanted sword and a tipped arrow change no outcome; a dropped stone is still there after 6 game days of sleep and gone after 8; a summoned moose without the flag never joins the level.

### The entity registry

**Every entity type, by base class and rig.** All ids are `yearofice:<id>`. Model and animation paths are under `assets/yearofice/`; species data is `data/yearofice/species/<id>.json` for land animals and birds and `data/yearofice/fish/<id>.json` for fish; hit zones are `data/yearofice/hitzone/<id>.json`; butchering is `data/yearofice/loot_table/butchering/<id>.json`. "Own" means a geometry file per species, `geo/entity/<id>.geo.json`; "shared" means `geo/entity/rig_<rig>.geo.json` with a scale. Animations always resolve to the merged `animations/entity/<id>.animation.json`. The build developer's bootstrap reader registers each type from its species file's `register` block; the base class and rig named here are that block's body plan, so the build section's 13 coarse body plans map onto these 10 classes and 35 rigs.

| Entities | Base class, rig | Model | Animation set | Spawn source | Butchering | Milestone |
| --- | --- | --- | --- | --- | --- | --- |
| `moose`, `caribou`, `dall_sheep` | `UngulateEntity` | Own | Ungulate set + `rut_thrash`, `scrape`, `paw_snow` | `PopulationSpawner`; caribou and sheep bands by `HerdController` | Yes, per species | M6 |
| `mule_deer` | `UngulateEntity`, cervid rig (new, the 35th) | Own | Ungulate set + `stott`, `look_back`, `tail_flag`, `stamp`, `lip_curl`, `rub`, `spar`, `fawn_drop` | `PopulationSpawner` slots in the eastern Interior and Yukon-side Beyond provinces; the valley straggler by `RareSightings`; `requires_flag` `yearofice:deer` | Yes | M6 content wave, behind the flag (Land animals in depth › Deer) |
| `sitka_black_tailed_deer` | `UngulateEntity`, cervid rig | Own | Ungulate set + `tail_flag`, `stamp`, `whistle_snort`, `lip_curl`, `rub`, `spar`, `kelp_feed`, `fawn_drop` | `PopulationSpawner` slots in coastal Beyond provinces only (needs World map's `coast` province kind; K 0 until it ships); `requires_flag` `yearofice:deer` | Yes | M6 content wave, behind the flag |
| `mountain_goat`, `wood_bison`, `muskox` | `UngulateEntity` | Own | Ungulate set + `ring_face_out` (muskox), dust roll (bison) | `HerdController` bands and herds | Yes | M6 |
| `goat`, `reindeer` | `Livestock`, ungulate rig | Own | Ungulate set + `milk_stand` | Radio drop, then calendar births | Yes | M9 (pens M8) |
| `wolf`, `coyote`, `red_fox`, `arctic_fox` | `CanidEntity` | Own | Canid set + `howl`, `pounce`, `play` | Wolf by `PackController`; others by slots | Yes | M6 |
| `lynx` | `FelidEntity` | Own | Felid set + `pounce`, `yowl` | Slots | Yes | M6 |
| `black_bear`, `grizzly` | `UrsidEntity` | Own | Ursid set + `stand`, `dig`, `climb`, `cover_carcass` | Slots; dens by Ecology | Yes | M6 |
| `wolverine`, `fisher`, `marten`, `ermine`, `least_weasel`, `mink`, `river_otter` | `MustelidEntity` | Own | Long-mustelid set + `bound`, `slide` (otter), `cache` | Slots; `trunk` placement for marten and fisher | Yes; ermine and weasel pelt only | Marten, mink, ermine M5; others M6 |
| `beaver`, `muskrat`, `hoary_marmot`, `woodchuck` | `LargeRodentEntity` | Own | Large-rodent set + `tail_slap`, `tow_branch`, `dive`, `stand` | Colonies and burrows (`burrow` placement) | Yes | Marmot, woodchuck M5; beaver, muskrat M6 |
| `porcupine` | `PorcupineEntity` | Own | Porcupine set + `quill_shell`, `climb` | `trunk` placement | Yes | M6 |
| `snowshoe_hare`, `alaska_hare` | `LagomorphEntity` | Own | Small-rodent set + `thump`, zigzag `flee` | Slots; `respawn_days` refill | Yes | M5 |
| `red_squirrel`, `flying_squirrel`, `ground_squirrel` | `SmallRodentEntity` | Own | Small-rodent set + `climb`, `glide`, `stand`, `cone_drop` | `trunk` and `burrow` placement | Red and ground squirrel only | Red and ground squirrel M5; flying squirrel M6 |
| `pika`, `red_backed_vole`, `tundra_vole`, `singing_vole`, `masked_shrew`, `brown_lemming`, `collared_lemming`, `bog_lemming`, `jumping_mouse` | `SmallMammal`, small-rodent rig | Shared | Small-rodent set + `dig`, `haypile` (pika, singing vole), `leap` (jumping mouse) | Bubble from the vole index V and lemming index L | None; tiny carcass pocketed whole | Voles, shrew, pika M6; lemmings, singing vole M7; jumping-mouse finds M5 |
| `little_brown_bat` | `YoiBird`, bat rig | Own | `hang`, `flap`, `swoop`, `click` | Roost slots, May 15 – Sep 10 | None | M7 |
| `wood_frog`, `boreal_toad` | `LandAnimal`, frog rig | Shared | `hop`, `swim.kick`, `call.sacs`, `frozen` | Pond chorus slots; toad at the warm springs | None; frog becomes its item | Frog M5 as bait, chorus M6; toad M7 |
| `hen` | `Livestock`, hen rig | Own | `peck`, `scratch`, `roost`, `crow` | Radio drop, broody hatches | Yes | M9 |
| `spruce_grouse`, `ruffed_grouse`, `sharp_tailed_grouse`, `willow_ptarmigan`, `white_tailed_ptarmigan` | `YoiBird`, grouse | Shared | Bird set + `display_fan`, `drum`, `lek_dance`, `snow_dive`, `broken_wing`, `bud_feed` | Slots; ground flocks for ptarmigan | Yes | M5 |
| `mallard`, `green_winged_teal`, `northern_pintail`, `american_wigeon`, `northern_shoveler` | `YoiBird`, dabbler | Shared | `tip_up`, `jump_takeoff`, `head_pump`, `filter_skim`, `broken_wing` | Pairs and ground flocks; migration waves | Yes | M6 |
| `lesser_scaup`, `bufflehead`, `common_goldeneye`, `harlequin_duck`, `common_merganser` | `YoiBird`, diver duck | Shared | `run_takeoff`, `head_throw`, `rapid_ride`, `fish_carry`, `dive` | Pairs; migration waves | Yes | M6 |
| `canada_goose`, `white_fronted_goose`, `trumpeter_swan` | `YoiBird`, goose and swan | Shared | `graze`, `hiss`, `wing_threat`, `charge_swim`, `nest_sit` | Pairs, flocks, sky flocks | Yes | M6 |
| `common_loon`, `pacific_loon`, `horned_grebe`, `red_necked_grebe` | `YoiBird`, loon and grebe | Shared | `long_run_takeoff`, `rear_tremolo`, `chick_carry`, `weed_dance`, `dive` | Pairs at ice-out | Yes | M6 |
| `sandhill_crane` | `YoiBird`, crane | Shared | `dance_leap`, `bow`, `stick_toss`, `unison_call`, `kettle` | Sky flocks; landed flocks in A3 | Yes | M6 |
| `wilsons_snipe`, `lesser_yellowlegs`, `spotted_sandpiper`, `red_necked_phalarope`, `american_golden_plover` | `YoiBird`, shorebird | Shared | `probe`, `teeter`, `spin`, `winnow_dive`, `broken_wing` | Pairs; migration waves | Tiny: pocketed | M6 |
| `mew_gull`, `arctic_tern` | `YoiBird`, gull and tern | Shared | `hover`, `plunge`, `head_strike` | Colonies; flocks over runs | Gull yes; tern pocketed | M6 |
| `bald_eagle`, `golden_eagle`, `osprey`, `rough_legged_hawk`, `northern_harrier` | `YoiBird`, raptor soarer | Shared | `soar`, `hover`, `stoop`, `talon_grab`, `mantle`, `plunge`, `sky_dance` | Pairs; eagles at runs by carcass count | Yes | M6 |
| `gyrfalcon`, `peregrine_falcon`, `merlin`, `northern_goshawk`, `sharp_shinned_hawk` | `YoiBird`, raptor fast | Shared | `dash`, `stoop`, `bind`, `mantle`, `nest_strike` | Pairs; gyrfalcon Oct – Apr | Yes | M6 |
| `great_horned_owl`, `great_gray_owl`, `hawk_owl`, `boreal_owl`, `saw_whet_owl`, `short_eared_owl`, `snowy_owl` | `YoiBird`, owl | Shared | `head_swivel`, `snow_plunge`, `wing_clap`, `hoot_throat`, `hover` | Pairs; irruptions by the vole index | Yes | M6 |
| `three_toed_woodpecker`, `black_backed_woodpecker`, `northern_flicker` | `YoiBird`, woodpecker | Shared | `trunk_climb`, `drum`, `chisel`, `flake_bark`, `anting` | Beetle-killed and burned stands | Pocketed | M6 |
| `raven`, `gray_jay`, `black_billed_magpie` | `YoiBird`, corvid | Shared | `cache`, `carry`, `roll`, `snow_slide`, `tail_flick` | Territories; camp birds by `CampBirds` | Raven yes; jay, magpie pocketed | M6 |
| `belted_kingfisher`, `american_dipper` | `YoiBird`, kingfisher and dipper | Shared | `hover`, `plunge`, `fish_beat`, `dip_bob`, `underwater_walk` | Creek spots | Pocketed | M6 |
| `black_capped_chickadee`, `boreal_chickadee`, `common_redpoll`, `snow_bunting`, `tree_swallow`, `cliff_swallow`, `bank_swallow`, `bohemian_waxwing`, `pine_grosbeak`, `white_winged_crossbill`, `varied_thrush`, `hermit_thrush`, `swainsons_thrush`, `american_robin`, `yellow_rumped_warbler`, `ruby_crowned_kinglet`, `dark_eyed_junco`, `fox_sparrow`, `white_crowned_sparrow`, `lapland_longspur`, `horned_lark`, `rusty_blackbird`, `northern_shrike` | `YoiBird`, songbird | Shared | `perch_sing`, `sally`, `hang`, `cone_pry`, `song_flight`, `impale` (shrike), `scratch`, `skim_drink` | Near-songbird slots; flocks; migration waves | Pocketed | M6; migrants' arrival M7 |
| `bird_flock` | `BirdFlockEntity` | Members drawn from their species rig | Boids; members play the species `flap` and `glide` | Flock slots and migration waves | Members become carcasses | M6 |
| `arctic_grayling`, `chinook_salmon`, `chum_salmon`, `coho_salmon`, `sockeye_salmon`, `arctic_char`, `dolly_varden`, `lake_trout` | `YoiFish`, salmonid | Shared | Fish set + `display.sail`, `spawn.dig`, `spawn.charge`, `spawn.hover`, `egg.drift_take`, `cruise.deep` | `FishProjector` from spot stocks; runs by `RunManager` | Cleaning pools in the same file | Residents M5; sockeye and runs M7 |
| `northern_pike` | `YoiFish`, esocid | Shared | `ambush.s_bend`, `strike.lunge` | Projection | Yes | M5 |
| `burbot` | `YoiFish`, gadid | Shared | `body.coil`, `spawn.ball` | Projection | Yes | M5 |
| `sheefish`, `humpback_whitefish`, `broad_whitefish`, `round_whitefish`, `least_cisco`, `bering_cisco`, `pygmy_whitefish` | `YoiFish`, coregonid | Shared | `chase.surface`, `feed.tail_up`, `feed.nose_down`, `roll.surface` | Projection; schools | Yes | M5; fall runs, pygmy whitefish M7 |
| `longnose_sucker` | `YoiFish`, catostomid | Shared | `feed.vacuum`, `spawn.vibrate` | Projection; sucker run | Yes | M5 |
| `slimy_sculpin`, `lake_chub`, `ninespine_stickleback`, `alaska_blackfish`, `trout_perch` | `YoiFish`, small fish | Shared | `hop.bottom`, `nest.guard`, `nest.fan`, `gulp.air` | Projection; schools; minnow traps | Eaten whole | M5; trout-perch M7 |
| `arctic_lamprey` | `YoiFish`, lamprey | Shared | `attach.sucker`, `swim.eel` | The lamprey run | Whole, to rendering | M7 |
| `fish_school`, `tadpole_school` | `SchoolEntity` | Members from the species rig | Boids | Projection | Members by gear rules | M5; tadpoles M6 |
| `mourning_cloak`, `wolf_spider`, `yellowjacket`, `snow_fly`, `diving_beetle` | `CritterEntity`, insect rig | Shared | `fly`, `crawl`, `hover`, `sting` | Bubble near flowers, ponds, nests, snow | None | wolf spider, mourning cloak, snow fly M7; yellowjacket M5 at racks; beetle M6 |
| `carcass_tiny`, `carcass_small`, `carcass_medium`, `carcass_large`, `carcass_huge` | `CarcassEntity` | The species' own model in a dead pose | `dead_side`, `dead_chest`, `dead_wings` | The death pipeline; found-carcass records | The species file | Tiny to medium M5; large, huge M6 |
| `salmon_carcass` | `CarcassEntity` | Salmonid rig, spawned-out texture | `dead.drift` | `RunManager` | None: bait and fertility only | M7 |

**Interfaces owned elsewhere.** `yearofice:arrow`, `bobber`, `canoe`, `raft`, `sled`, `travois` and `hide_drag` are owned by other developers; this section needs only their `canAddPassenger` and harness rules (no-vanilla row 36, Taming above).

### Settled across tabs

**The review found 24 conflicts beyond the Director's list.** Each line gives the winner; the losing tab changes only its build line.

| # | Conflict | Decision |
| --- | --- | --- |
| S1 | Java package: Bestiary `com.yearofice.entity.animal`, Video and Viewers `animal.*`, Water life `com.yearofice.fish.ai` | Land animals, birds and critters in `com.yearofice.ecosystem.species.*`; fish entities and minds in `ecosystem.fish`; hooked fish, gear and the fight in `hunt.fishing` (the lead's package map, Director item 32) |
| S2 | Species data: Bestiary's datapack registry at `data/yearofice/yearofice/species/` | Our reload listener at `data/yearofice/species/<id>.json`, snake\_case fields (`k_core`, `juv_survival`, `adult_deaths_per_year`), per the engine tab (Director item 6) |
| S3 | Fish data: `fish_species/` (Waters, Water life) against `fish/` (engine) | `data/yearofice/fish/<id>.json` |
| S4 | Caught fish item: `yearofice:fish_<species>` (engine) against the species id (Waters, Water life) | The species id in the item registry, as vanilla does with `minecraft:cod`; component `yearofice:fish_data` |
| S5 | Player speed in Land animals in depth (walk 4.3, sprint 5.6 m/s) | Engine numbers: walk 3.0, run 5.5, swim 1.0; an animal over 5.5 m/s outruns the player |
| S6 | Swim speeds per tab | The settled table in Pathfinding (Director item 33) |
| S7 | Beaver box 0.6 × 0.45 (Land) against 0.7 × 0.5 (Water life); muskrat 0.35 against 0.4 wide | Land tab's boxes; length lives in the hit zones |
| S8 | Wood frog box 0.15 × 0.1 (Land) against 0.06 × 0.04 (Water life) | 0.06 × 0.04 with the 0.15 pick margin |
| S9 | Death loot: Land tab's `loot_table/entities/` giving the carcass or the item | Empty death tables; the death pipeline makes the carcass (Director item 41) |
| S10 | Birds tab: a dead bird becomes a "carcass item entity" | A carcass entity of its class; tiny ones pocket as `whole_game` |
| S11 | Engine data kind `butcher/` against `loot_table/butchering/` | Loot tables, with the species `carcass` block for times and parts; `butcher/` retires |
| S12 | Creative concepts' `canada_jay` entity and its theft of 0.1 kg an hour | `gray_jay`, 50 g a visit (Bestiary); `canada_jay` is a lang alias only (Director item 1) |
| S13 | Caribou hide by date: Land tab ×0.5 January – August 19 against Bestiary's dates | Bestiary: −40% June 1 – August 14, best August 15 – September 30, −20% after November 1; `hide_quality` folds into `pelt_grade` |
| S14 | Caribou bot flies: Land tab `caribou_bot_fly` against Birds tab `warble_fly` and `nose_bot_fly` | The Birds tab's two particle ids; one class `ecosystem.insects.CaribouFlies` replaces `BotFlyHarassment` |
| S15 | Yellowjacket as a Land entity and a Birds particle | Entity `yellowjacket` (Land animals) and a separate particle `yellowjacket_swarm` (Birds and the air), never one id in two registries; one nest block `yellowjacket_nest` with ground and paper variants |
| S16 | Up to 60 real caribou (Land) against 40 Active (engine) | Group members are their own form with a 60-per-herd cap and a 4 Hz follower brain (Tiers) |
| S17 | Land and bird group caps sum past 40 | Ceilings inside 40, not reservations |
| S18 | Moose sodium drive to August 15 against the lick weight to August 31 | Both end August 31: 80% of days May 20 – July 15, 40% July 16 – August 31, a season mean of 62%, matching the Blueprint's 60% (Director item 10) |
| S19 | Legends package `com.yearofice.legend` and Waters' `legend_fish/` | `story.legend` (the lead's map), one `legend/` folder with `kind` |
| S20 | White Cow legend against the white-moose rare sighting; Black Wolf against the black-wolf sighting | One slot each (Legends) |
| S21 | Yukon floater as an ambient entity (Water life) against an item (Waters) | A state of `mussel_bed` plus the item |
| S22 | Engine art budget of 20 models | 35 rigs and about 190 texture sets (Models) |
| S23 | Livestock prices and hen feed in Ideas from viewers | Radio catalogue prices and Director item 29 feed numbers (Taming) |
| S24 | The cache "mice" | `red_backed_vole` is the entity; messenger lines and prompts say "mice", and the lang name is "Red-backed vole (mouse)" (Director item 2) |

**Also confirmed.** The gray jay, ermine, great horned owl and coyote are defined in the Bestiary, and Water, calls and encounters uses those ids (Director item 10). The straggler caribou band is drawn from that year's migrating herd count, never added to it. The black-billed magpie is `yearofice:black_billed_magpie` in both the Bestiary and Birds and the air (Director item 45). Animals register in `MobCategory.CREATURE`, fish in `MobCategory.WATER_AMBIENT` (DIRECTOR RULING 101), carcasses and groups in `MobCategory.MISC`, every mod damage type uses `"scaling": "never"`, and `doMobLoot` false and `maxEntityCramming` 0 join the game-rule table (Director item 44).

### Gaps the review found

**Species lacking a sound.** Each gets one event under `yearofice:entity.<species>.<call>`, so every animal the player can see can also be heard or is deliberately silent.

| Species | New event | Range | When |
| --- | --- | --- | --- |
| Ermine, least weasel | `trill` | 15 m | Alarm, the tell |
| Mink | `hiss` | 10 m | The Bestiary's tell |
| Muskrat | `plop` | 20 m | Diving at alarm |
| Flying squirrel | `chirp` | 20 m | Night, at the nest tree |
| Hen (rooster) | `rooster_crow` | 300 m | Dawn |
| Dragonfly | `wing_rattle` | 3 m | Passing close |
| Reindeer (belled) | `bell_clank` | 60 m | Moving, so a loose reindeer can be found |

Silent by design: wolf spider, mourning cloak and the fish, whose signs are the water sounds in Water life in depth.

**Species lacking behaviour numbers.** Livestock had no response to predators or to getting loose, and four wild species had gaps.

- **Goat, loose.** Alert at 30 m to predators; flees to its pen or toward the player within 200 m; butts a fox or lynx at 1 m (4 J, no damage to the player).
- **Reindeer, loose.** Behaves as a caribou with flee distance 150 m; returns to the paddock at dusk if bond is 60 or more.
- **Hen, loose.** Flutters 5 m up to a perch; roosts at dusk; a hen outside at night is the fox's.
- **Fisher, arctic fox, woodchuck.** Their kill thresholds were missing (filled in Hitboxes, above).
- **Muskox and bison calves.** Calf hider or follower plans were missing: both are followers, inside the ring or at the cow's flank.

**Systems the tabs implied but did not specify.**

- **Young as one entity.** Calves, cubs, pups, kits and leverets are the parent species with an age class, never separate ids.
- **Air and breath for every diver.** `breath_s` per species in the species file, with dive targets and under-ice air points.
- **Pick margin for tiny animals.** So voles, frogs and redpolls can be clicked.
- **One dropped-item rule** shared with the player systems (no-vanilla row 47).
- **Commands that respect counts** (Tiers › Commands).

### Performance: caps, throttling and level of detail

**The animals' 8 ms is split three ways.** Architecture and the interlace › The server tick and its budget gives Active animals, birds and fish 8 ms on average and 10 ms at peak; tiers and populations (2 ms) and carcasses (0.5 ms) have their own lines there. `ecosystem.AnimalScheduler` meters each share with `System.nanoTime` per tick through `core.tick.Budget`.

| Share | Average | Peak | Holds |
| --- | --- | --- | --- |
| Land animals | 4.0 ms | 5.0 ms | Up to 32 Active mammals, 60 herd members, 30 small mammals, 12 critters |
| Birds | 2.5 ms | 3.0 ms | 14 single birds, 6 flocks, 10 near songbirds (Birds and the air › Performance caps) |
| Fish and water life | 1.5 ms | 2.0 ms | Water life in depth › Caps and cost |

**AI throttling by distance.** Every Active animal moves every tick; what thins out is thinking.

| Distance | Brain behaviours | Sensors | Path searches |
| --- | --- | --- | --- |
| Under 64 blocks | Every tick | At their periods | On demand, 4 a tick in all |
| 64–150 blocks | Every 2 ticks | Periods doubled | Only on a new goal |
| Group members | 4 Hz follower brain | Leader senses for the group | None; flow field |
| Nearby | None | None | Trail graph once per game minute |

**The overload valve.** If the land share passes 5.0 ms on 3 ticks in a row, the farthest Active animals not exempt from demotion skip their Brain tick (motion continues through `MoveControl`) until the share falls under 4.0 ms. Exempt: anything stalking, charging, wounded, aimed at, sketched, Known, a legend, the companion wolf.

**Tracking and sync.** Each type sets `clientTrackingRange` (in chunks) and `updateInterval` (ticks) in its `EntityType.Builder`.

| Type group | Tracking range | Update interval | Notes |
| --- | --- | --- | --- |
| Big game, bears, wolves | 10 | 2 | Seen to the 160-block simulation edge |
| Medium mammals | 8 | 3 | — |
| Small mammals, critters | 4 | 3 | — |
| Single birds | 10 | 2 | Raptors 12 |
| `bird_flock`, schools | 12, 4 | 10 | Shape comes from `FlockStatePayload` and school state every 10 ticks |
| Fish | 4 | 2 | — |
| Carcasses | 6 | 20 | They do not move once settled |

**Client animation level of detail.** `client.anim.AnimLod` gives each animal a level from its distance and screen size; the custom `LodGeoModel extends DefaultedEntityGeoModel` skips `handleAnimations` on frames the level skips and reuses the last bone pose.

| Level | Distance | Controllers | Animation rate | Render |
| --- | --- | --- | --- | --- |
| 0 | Under 24 blocks | All | Every frame | Full model, render layers (eyes, velvet, marks) |
| 1 | 24–48 | `base`, `action` | Every frame | Full model; ear, tail and head off |
| 2 | 48–96 | `base` | 20 Hz | Full model, no layers |
| 3 | 96–160 | `base` frozen to a looping pose | 5 Hz | Herds, flocks and schools become impostors here |
| Impostor | Beyond 96 for groups | None | Instanced sway | `HerdImpostorRenderer`, flock sprites, school instances; one draw per species and stage |

**Instanced members.** Flock and school members and far herd animals draw through one instanced buffer per species and texture, built on the vanilla `RenderType` path so Sodium and Iris keep working. A frame-time logger in the client smoke caps them at 1.5 ms for 400 bird members and 3 ms for a 600-fish run reach.

### First prototypes

**Five prototypes retire the five biggest entity risks before M6 content starts.** Each is a GameTest template plus a client-smoke scene, built in M5 on test species.

**P1: a moose in a willow flat.** The template is 96 × 96 blocks of willow brush at 60% cover with white spruce at 3–4 m spacing. It has two creeks 1.2 m deep, a 2-block cut bank, and 0 cm and 90 cm snow variants with a packed yard trail.

1. A bull moose (1.4 × 2.3) walks to 20 seeded targets: at least 95% reached within 600 ticks each, never stuck over 60 ticks, and no `inWall` event.
2. Path searches average under 0.4 ms and never exceed 2 ms; trail-graph routes are used for any target over 32 blocks.
3. In 90 cm snow, at least 90% of its steps stay on packed cells, and approached to 40 m it flees 50–100 m (Land animals in depth › GameTests 2).
4. Walking through brush emits a crash noise event heard at 10 m, and the player crossing the same flat moves at 60% speed.

**P2: a flock of 40 ptarmigan.** Template: a willow flat in February with 40 cm of powder.

1. One `bird_flock` of 40 promotes 8 real birds within 32 m of a walking player and holds the other 32 as drawn members.
2. A player walking head-on flushes the flock at 0.9–1.1 × its flush distance; the takeoff wave starts on the side nearest the player; the flock lands 50–200 m off and snow-dives.
3. A blunt arrow kills the member nearest the ray; the count drops to 39 at once, the region's N drops by 1, and one tiny carcass lies where it fell.
4. Server cost for the flock and its 8 birds stays under 0.3 ms; `FlockStatePayload` is under 64 bytes twice a second; two clients draw member positions within 0.5 m of each other from the shared seed.

**P3: a salmon run of 600 fish.** Template: a 300 m reach below a 2 m falls in the sockeye peak, 600 fish passing in a game day.

1. On screen at peak: 12 individual salmon entities within 32 blocks (spearable, grabbable, hookable), up to 4 run schools, and drawn members up to the 600 run-reach cap, with 2 leaps a second at the falls lip.
2. **Promote on aim.** When the player's ray within 8 m meets a drawn member, the client sends `yearofice:aim_member` (school id, member index, position). The server checks the position lies inside the school's radius and swaps that member into an individual entity within 1 tick, so a spear or a grab always has a real fish to hit; the school's count drops by 1. Flocks use the same rule for a drawn bird.
3. The fish share of MSPT stays under 1.5 ms over 6,000 ticks; the client run draw stays under 3 ms a frame at render distance 12.
4. Passage, holding and escapement match `RunManager`'s numbers within 1%: what the player sees is the run's count, and the carcass test in Waters and fish still passes.

**P4: records survive anything.** This is the entity side of the lead's P6 and `ecology/crash_no_duplicate`. 40 moose and 6 wolves are promoted and demoted 1,000 times at random while the server is killed mid-save 100 times. Every load holds exactly 46 records and 0 duplicate bodies, and each moose returns to its slot.

**P5: a kill makes one body.** Arrows into the 8 rigs' test species at 20 random angles: 0 item drops, 0 experience orbs, one carcass of the right class, and a damage map whose bloodshot regions match the zones the resolver named.

### Interlace

**What entities read from and write to each shared layer.**

| Layer | Entities read | Entities write |
| --- | --- | --- |
| Clock and calendar | Game minute for drives, hour for looks and care, day for births and Ecology, dates for seasons, migration and hibernation windows | `TimeService.requestRate` for the close clock from Stalk, Chase, Warn and Charge |
| Environment field | `EnvService.sample` and `light`: air and water temperature, wind vector, snow depth and crust, ice state, light level, current per column, water body type and oxygen | Nothing directly; trampled snow and yard wear go through World edits |
| Scent and sound field | `ScentService.smells`, `playerStrength` and `NoiseService.heardBy`: the player's scent cone, scent points (kills, gut piles, camps, caches, pens), noise events, alarm calls, calls performed | `ScentService.upsertSource` for scent posts (wolf borders, bear rubs), carcasses by class and stage; `NoiseService.emit` for alarms, brush and fights |
| Ecology | `EcologyService.count`, `index`, `records`: N, K, indices (hare cycle, vole V, lemming L), slot lists, migration draws, legend placements | `harvest` and `reportDeath` for kills and deaths, orphan flags, raised animals leaving the count, `PredationEvent` outcomes |
| Decay and freshness | Carcass, bone pile and sign stages | New carcasses, gut piles, sign records (tracks, blood, beds, browse), shed antlers and tufts |
| Item model | Item weights and components for theft, bait and pocketed carcasses | `whole_game`, `fish_data`, parts with `fresh` and `pelt_grade`, eggs |
| Body | `BodyService.condition`, `scent`, `isSafeToStage`: the player's Condition, energy, BMI band, carried meat, posture and noise for the wolf test and predator choices | `BodyService.applyInjury` for attacks, quills, stings and leeches, the only injury path (DIRECTOR RULING 90); `BodyService.wake(player, cause)` when a predator or an animal over 100 kg comes within its species `wake_m` of a sleeper (`ecosystem.encounter.SleeperWatch`, every 20 ticks) |
| Messenger and HUD bus | Which helpers and calls the player has learned | `HudBus.post` for helper lines, first-arrival lines, Known-animal and name prompts; never trap catches, never legend names |
| World edits | Felled trees, built pieces, pens, doors, fences and caches as path types and targets | Beaver cuts and dams, browse lines, bear damage to caches and doors, wallows, dust bowls, nests, push-ups, trail wear, all as `WorldEditService.submit(ChangeOp)` |
| Sound events and the audio bus | — | Every animal call through `SoundBus.play`, which also emits the matching noise; calls over 128 blocks go as `yearofice:distant_sound` |

### Systems at build depth

| System | NeoForge mechanisms | Server and client | Saved | Synced | Tick cost | Tests |
| --- | --- | --- | --- | --- | --- | --- |
| Entity types | `DeferredRegister<EntityType<?>>`, `EntityAttributeCreationEvent`, `RegisterSpawnPlacementsEvent`, `EntityRenderersEvent.RegisterRenderers` | Types both sides; renderers and models client | — | Type ids | — | Registry validation in JUnit |
| Brain and state machine | Memory, sensor, activity and schedule registries; `Brain.Provider` | Server | S-marked memories into records | Activity id, look target | 0.10–0.20 ms per Active animal | `charge_gate`, Blueprint state tests |
| Tiers and spawning | `ChunkEvent.Load` and `Unload`, `EntityJoinLevelEvent`, `EntityLeaveLevelEvent`, `FinalizeSpawnEvent`, `ServerTickEvent.Post` | Server | Records in region files, counts in `yearofice_populations` | — | 0.3 ms per 20 ticks | P4, `active_cap` |
| Navigation | `YoiNodeEvaluator`, `IBlockExtension#getPathType`, custom navigations | Server | — | Motion | 4 searches a tick | P1 |
| Hit zones and combat | `LivingIncomingDamageEvent`, `AttackEntityEvent`, projectile `onHitEntity`, `LivingKnockBackEvent` | Server; debug overlay client | Wounds in records | Blood sign through Decay and freshness | Per shot | P5, Blueprint hit-zone tests |
| Groups | `HerdController`, `PackController`, flock and school entities, payloads `HerdStatePayload`, `FlockStatePayload` | Server rules; boids and impostors client | Controllers in region files | Group state every 10–20 ticks | Inside each share | P2, P3, Land tab test 1 |
| Death and carcasses | `AnimalKilledEvent`, `CarcassEvent.Created`, `LivingDropsEvent` cancelled, `LivingExperienceDropEvent` 0 | Server; dead poses client | Carcass entities and records | Carcass synched data | 0.008 ms each | P5, Bestiary carcass tests |
| Taming and livestock | Entity attachments `trust`, `care`, `bond`; `Leashable`; payload `wolf_command` | Server | With chunk or player | Stage looks, harness | 0.05 ms each | Viewers taming tests, `livestock_laying` |
| Legends | `yearofice_legends` SavedData; render layer and `mark` bone | Server; layer client | SavedData and records | Variant, mark seed | Negligible | `legend_slot`, Creative tests 3–4 |
| Looks | `SynchedEntityData` 4 bytes; colour and texture choice in the model | Server computes; client draws | Records | 4 bytes | Hourly | Land tab looks |
| Animal sound | `SoundEvent` registry, distant-sound payload | Server picks; client plays | — | Sound packets | — | Captions in client smoke |

### Entity GameTests

Each is in `com.yearofice.gametest.EntityGameTests`, named `yearofice:ecosystem/<test>`, batched by milestone and waiting on game time through the Clock and calendar.

1. **The prototypes (M5).** `willow_moose`, `ptarmigan_flock_40`, `salmon_run_600` (on a test run in M5, the real runs in M7), `tier_round_trip` and `one_body` assert the P1–P5 pass numbers.
2. **`charge_gate` (M6).** Over 1,000 staged bear and moose charges, every Charge follows at least 20 ticks of Warn, and at least 30 when the bear or moose was not surprised.
3. **`active_cap` (M6).** With 80 candidates around one player, Active never exceeds 40, no group passes its ceiling, and a stalking wolf is never the one demoted.
4. **`swim_speeds` (M6).** Each species in the settled table crosses 20 m of still water at its speed ±5%; the current test of the engine tab holds for a swimming moose.
5. **`alarm_chain` (M6).** A flushing jay within 40 m of a bedded moose raises its awareness by 25 ±1 in one event.
6. **`legend_slot` (M7).** Over 200 seeds, a world with the White Cow never rolls the white-moose sighting, and one without it rolls it on 2% ±1%.
7. **`livestock_laying` (M9).** Hens at 14 h of light and 6 °C lay at the full rate; at 12 h and 3 °C at 0.25 of it; at −2 °C none.
8. **`anim_lod` (client smoke, M6).** 40 animals, 6 flocks of 300 and a 600-fish reach keep the entity share of the frame under 6 ms at render distance 12, with and without Sodium.

### Risks and the prototype that retires each

| Risk | Why it matters | Retired by |
| --- | --- | --- |
| Big bodies in block forests | A moose stuck on a birch or blinking through a trunk breaks the illusion at once | P1, the brush block and the trail graph |
| Entity count | Herds, flocks and runs are the valley's spectacle and the tick's biggest cost | P2, P3 and the three-way split of the 8 ms animal line |
| Duplicates and lost animals | Records plus chunk saves can clone or lose the bull the player has followed for a week | P4 and `shouldBeSaved` false |
| Hits that disagree with the model | A hit zone that does not sit where the drawn heart is makes the hunt feel unfair | P5 and a client debug overlay that draws each species' zones on its model; the build check fails if a zone pokes outside the model's bounds |
| Animation cost on the client | 35 rigs with 6 controllers at 40 animals plus impostors | `anim_lod` and the LOD levels |
| Boids that differ between players | Two players aiming at different ducks | P2's seeded members and promote-on-aim on the server |
| GeckoLib changes between versions | Controller and trigger APIs move between releases | The pin rule; all GeckoLib calls behind `client.anim` and `ecosystem.anim` adapters |
| Predation that empties the valley | Real chases plus daily rates could double-count | Predation in view uses the same daily rate budget: a staged kill is taken from the day's Abstract kills, never added |

### What the entity developer needs from the others

- **Lead engineer.** Sign-off on the three-way split of the 8 ms animal line and on slot records for small game, small birds and fish (Ecology keeps every individual; this section keeps full records only for big game and named animals). Settled by DIRECTOR RULINGS 89–91 (7-game-day items, applyInjury only, tiered records); the two calls as asked were: dropped items (the lead's 7 game days against the client section's never-despawn ground piles; this section follows the lead) and player injury from animals (`BodyService.applyInjury` in the lead's API against `BodyService.strike` in the client section; this section uses `applyInjury`).
- **World developer.** The `brush` block family and its collision rule; tree ids and trunk columns per chunk; the Stage 7 trail graph as a loaded structure; `getPathType` on pieces, pens and fire; `WaterBodyIndex` air points under ice; terrain-change queue entries for beaver and bear edits.
- **Client developer.** The `LodGeoModel` hook, the instanced impostor renderers, `KnownMarkLayer` and the eyeshine layer, distant-sound captions, and payload registration for herd, flock, school and wolf command payloads.
- **Build developer.** `AnimationMergeProvider` in datagen; load-time validators for species, fish, hitzone, carcass, legend and livestock files (missing `carcass`, `attack`, `breath_s` or `butcher_table` fails the build); the GameTest templates for P1–P5; an art pipeline sheet for 35 rigs and their texture sets.

## Player systems and client

This section builds everything the player is and touches: the body and its tick, the replacement of vanilla needs and death, the inventory and carried loads, every screen and HUD layer, input, the first-person camera, client rendering, the Audio bus, the network payloads and accessibility. It follows the Minecraft engine tab and changes it only where a rule below says why. Shared layers carry the names the lead engineer fixes in Architecture and the interlace: Clock and calendar, Environment field, Scent and sound field, Ecology, Decay and freshness, Item model, Body, Messenger and HUD bus, World edits, and Sound events and the audio bus. Other systems reach them only through YoiApi.\<layer>().

### Packages and the client–server split

**One rule decides where code lives.** The server owns every number that changes the game. The client draws, plays sound, reads input and predicts motion. Pure formulas sit in `com.yearofice.sim` and run on both sides where prediction needs them.

| Package | Side | Holds |
| --- | --- | --- |
| `sim.body`, `sim.fishing`, `sim.minigame` | Both (pure Java) | `BodyModel`, `ExposureSolver`, `StumbleModel`, `FightModel`, bow drill and cordage scoring |
| `body` | Server | `BodyTicker`, `DamageRouter`, `SosFlow`, `Drinking`, `Nausea`, `Grime`, `Fever`, `RestDay`, `Adrenaline`, `MorningReport`; the Body layer's service `api.body.BodyService` |
| `food` | Server | `Eating`, `Stomach`, `YoiFoodItem`; they write intake through `BodyService.eat` and `drink` |
| `item.inventory` | Server | `PackMenu`, `PackRules`, `LoadCalculator`, `CarryHandler`, `Pickup`, `QuiverFeed` (inside the studio lead's `item` package) |
| `build.work` | Server | `WorkService` (hold-to-work), `MinigameJudge`, `HoldGuards` (server half), as the lead's map puts them |
| `hud.messenger` | Server | `Battery`, `PageAccess`, `MapTileServer`, `ToolMemory`, beside `HudBus` |
| `fire.light` | Server | Light items; their levels in `data_maps/item/light.json` feed `env.light` |
| `sound` | Server | `SoundBus`, the server half of the audio bus |
| `client.hud`, `client.screen`, `client.messenger`, `client.pack`, `client.minigame` | Client | Every GUI layer and screen below |
| `client.ui`, `client.input`, `client.camera`, `client.render`, `client.light`, `client.audio`, `client.map`, `client.season`, `client.sky` | Client | `Units`, key mappings, `BodyCamera`, decals, `HeldLight`, `AudioBus`, `MapTiles`, colour handlers, sky |
| `net.payload` | Both | Payload records and stream codecs (the studio lead owns `net`; this section names them) |

**Normalised names.** These follow the lead's package map. `ui.*` classes go to `client.ui.*` (`client.ui.Units`, `client.ui.MorningLine`, the client half of `client.ui.HoldGuards`); the server half of `HoldGuards` goes to `build.work`; `ui.ToolMemory` becomes `hud.messenger.ToolMemory`. The engine's `pack.PackMenu` becomes `item.inventory.PackMenu`, and `body.Eating` becomes `food.Eating`, since the lead gives eating and the stomach to `food`.

### The interlace

Each player system reads and writes shared layers only through their services, never another system's internals.

| System | Reads | Writes |
| --- | --- | --- |
| Body tick | Clock and calendar (crossed minutes, rate), Environment field (air, wind, precipitation, room and water temperature, fire heat, snow depth, bug pressure), Item model (worn garments, wetness, condition, item temperature, food data), Decay and freshness (food state at eating) | Body (the record), Messenger and HUD bus (warnings, the morning line), Scent and sound field (grime and clothing scent, breath noise) |
| Collapse and SOS | Body, Clock and calendar, World edits (outpost bar position) | Clock and calendar (the 14-day jump), World edits (the left pack), Decay and freshness (catch-up on loaded chunks), Messenger and HUD bus (journal) |
| Inventory and weight | Item model (weight, stack, freshness bands), Decay and freshness (freshness, garment wear) | Body (load band), Scent and sound field (carried meat scent) |
| Carrying and carcasses | Ecology (the carcass record), Item model | Scent and sound field (blood trail, drag scrape), World edits (sign records for drag furrows) |
| Hold-to-work and minigames | Clock and calendar, Body (energy, warmth, hand zone), Environment field (light, wind), Item model (tool condition, tinder wetness) | Clock and calendar (work and close rates), Body (activity class, kcal), Scent and sound field (`noise_m`), Decay and freshness (tool wear) |
| HUD and messenger | Body, Environment field, Messenger and HUD bus, Item model (battery) | Nothing outside the client, except page battery drain on the server |
| Held light | Item model (light item, battery), Environment field (cold halves output) | Environment field (`env.light` adds the held light to gameplay light); the client draws it through `client.light.HeldLight`, by the lead's Held light rule |
| Footsteps and voice | Environment field (snow temperature), World edits (block under the feet) | Sound events and the audio bus, Scent and sound field (noise radius) |
| Rendering | Environment field (season stage, weather, aurora), World edits (sign records) | Nothing |

**Waking a sleeper.** Body may not read Ecology, because Ecology reads Body (the lead's no-cycle rule). So the predator and large-animal wake rules run in Ecology, which calls `BodyService.wake(player, cause)`; the body tick never looks for animals.

### The body attachment

**One record per player.** `yearofice:body` is a player data attachment with a versioned codec (`schema` 1), `copyOnDeath` true, written only by `body.BodyTicker`. RECONCILE 19 is settled here: grime, nausea and fever are fields of this record, not separate attachments and not mob effects.

| Field | Type | Range or unit | Notes |
| --- | --- | --- | --- |
| `height_cm`, `sex` | short, byte | 150–200; 0 or 1 | From the first-join Body screen; `bmr` is derived, never saved |
| `kg` | float | kg | BMI derived from `kg` and `height_cm` |
| `kcal_in`, `kcal_out` | short × 24 | Hourly bins, ring buffer | The 24 h balance and the morning line |
| `macro_g` | short × 72 × 3 | Grams per hour of carbs, fat, protein | The 72 h meters |
| `stomach_kg` | float | 0 to capacity | Capacity from `food/stomach.json` and the preset |
| `water` | float | 0–100 | 1 point = 30 mL |
| `warmth` | float | 0–100 | Stage derived |
| `part_frost` | short × 10 | Frost points per body part (Body, injury and sickness C8, schema 2) | Same order as part\_hp: head, torso, left and right arm, hand, leg and foot. A zone's value (head, torso, hands, legs, feet) is the highest of its parts; safe-to from worn gear |
| `energy` | float | 0–10 bolts | The cap is derived each minute |
| `awake_min`, `asleep`, `sleep_q` | int, bool, float | Game minutes; 0–1 | Sleep debt from `awake_min` |
| `condition` | float | 0–100 | Strength derived, never stored |
| `problems` | list, at most 16 | id, part, stage, timer, dressed, infected | Injuries and illnesses from `injury/` and `remedy/` data |
| `vit_c_days`, `lean_days`, `keto_days` | float, byte, byte | Days | Scurvy, lean-meat sickness, adaptation |
| `nausea`, `fever_phase`, `grime` | byte, byte, byte | 0–100; 0–2; 0–100 | Sick stomach, fever cycle, grime |
| `spirits`, `spirits_day` | byte, short | 0–100; the day's sum | Applied on sleep |
| `adrenaline` | byte, int | Phase, encounter id | The crash |
| `rest_run`, `rest_work_min`, `rest_away_min` | byte, short, short | — | Rest days |
| `sos` | byte | 0 none, 1 collapsing, 2 away, 3 returning | The SOS state machine |

Saved size is about 1.1 KB a player, mostly the macro bins.

### The body tick

**When it runs.** `BodyTicker` listens to `ClockEvent.Minute`. At the world clock that is one step every 20 ticks; at the sleep rate (180 game seconds a tick) it is 3 steps a tick. When more than 10 minutes cross in one tick, the model steps hourly in closed form with the Environment field's hourly record. Creative and spectator players are not stepped.

**The order, each game minute.** It extends the blueprint's six steps; nothing reorders them.

1. **Sample.** The minute's activity class is the highest seen in its 20 ticks (sleep, rest, walk, load, heavy work, swim, sprint). Environment, worn clothing and load are read once.
2. **Burn.** The activity rate times BMR ÷ 1,794 times the preset factor, plus shivering, plus the Rest-day healing cost of 10% of BMR a day while a problem heals. This replaces kg ÷ 77 (RECONCILE 17).
3. **Stomach and absorb.** Food eaten this minute adds its mass (stew and broth at 50%, water and tea 0). Absorbed kcal is kcal times the gut factors, which multiply: gut illness 0.5, parasites 0.85, lean-meat nausea 0.8, the fat-limit loss 0.8. The stomach empties 7% an hour awake and 4% asleep.
4. **Weight.** Δkg = (absorbed − burned) ÷ 2,400.
5. **Water.** Drains by the Water table, plus 1 L a day with fever; food water adds.
6. **Warmth.** Feels-like by the Warmth formula, then dW; the soaked rule; zone frost points against each zone's safe-to limit.
7. **Energy.** Drain by activity, sprint −0.05 a minute, the cap from the cap table; at under 1 bolt the blackout roll `hash(seed, player, "blackout", minute)` at 2%.
8. **Condition.** Problem losses, the daily protein rule at 00:00, healing per hour by band and Rest day.
9. **Problems.** Timers advance; nausea falls 10 an hour with no active source and vomits at 100; the fever cycle steps; grime adds its daily +8 at 00:00 if over 12 of the last 24 hours were outdoors.
10. **Derive.** Strength, BMI band, energy cap, exposure minutes (`sim.body.ExposureSolver`), bug exposure.
11. **Spirits.** The day's sum grows; it applies at the next sleep.
12. **Check.** Collapse triggers hand over to `SosFlow`. Any vital moving over 5 points in a game hour posts `VitalChangedEvent` with its cause.
13. **Sync.** `yearofice:body_sync` goes out if any shown value moved over 1% since the last send, or once a game hour.

```latex
kcal_{min} = \frac{rate_{activity}}{60} \cdot \frac{BMR}{1794} \cdot m_{preset} + \frac{kcal_{shiver}}{60}, \qquad Strength = Condition \cdot \mathrm{clamp}\left(\frac{BMI - 17}{6}, 0, 1\right)
```

**Cost.** One step must take under 15 µs and allocate nothing; `BodyModel.step` works on a mutable record. With 8 players at the sleep rate that is 24 steps a tick, about 0.36 ms, inside the engine's 1 ms budget for body and clock.

**Raw fat and food units.** The absorb step reads kcal from `data_maps/item/food.json` per 100 g unit: raw fat 700, lard 900, cracklings 550 (RECONCILE 14). Food stacks count 100 g units, so loot and butchering counts are in units of 100 g (RECONCILE 43).

### Vanilla needs replaced

| Vanilla | What we do | Mechanism |
| --- | --- | --- |
| Food level and saturation | Held at 20 and 0 every tick, so sprint and regen never read it; the bar is hidden | `PlayerTickEvent.Post` sets `FoodData`; layer `minecraft:food_level` cancelled |
| Eating | Our foods carry no `minecraft:food` component. `food.YoiFoodItem` uses `UseAnim.EAT`, a use time from data (3 s per portion), and calls `food.Eating` on finish | The eat prompt reads "R  Eat dried meat (+6%, stomach 36%)" (RECONCILE 13) |
| Health and hearts | Held at 20; never shown | `LivingIncomingDamageEvent` cancels every player damage and hands it to `body.DamageRouter` |
| Natural regeneration | Off | Game rule `naturalRegeneration` false; `LivingHealEvent` cancelled on players |
| Damage scaling by difficulty | Never | Every mod damage type uses `"scaling": "never"`; difficulty locked to Normal (RECONCILE 44) |
| Air bubbles | Hidden; the breath ring and the water clock replace them | Layer `minecraft:air_level` cancelled; air held full except under ice, where the body model owns the count |
| Sprint | Kept on Ctrl, with our speed and cost | Food is always 20, so vanilla allows it; we block it below 1 bolt, when Slowed, at 30 kg or more, while carrying medium or larger, in the crash, or with a broken leg: client `MovementInputUpdateEvent` clears it, and the server removes the `yearofice:gait` modifier if a blocked sprint arrives |
| Beds and the night skip | Our beds only; any hour; no skip | Bed blocks return `isBed` true; `CanPlayerSleepEvent` applies our rules; `CanContinueSleepingEvent` runs the wake rules; `SleepFinishedTimeEvent` cancelled |
| Beds set the spawn | Never | `PlayerSetSpawnEvent` cancelled; the return point is always the outpost bar |
| Death screen and respawn | None; collapse runs `SosFlow` | `LivingDeathEvent` cancelled for any cause `SosFlow` did not start |
| Status effects | No mob effects are used for body states; the vanilla effects HUD is hidden | Layer `minecraft:effects` cancelled; the viewers tab's `yearofice:nausea` and `fever` effects are dropped (RECONCILE 19) |

**Sleeping on bare ground.** Vanilla wakes any sleeper whose sleeping position holds no bed. Sleeping on the ground places `yearofice:ground_bed`, an invisible block with no collision, at the feet; it is removed on waking and never saved. Debris beds, bough beds and bunks are real blocks.

**Sitting and waiting.** Sit and Wait by the fire mount the player on `yearofice:seat`, an invisible vehicle entity in `#yearofice:vehicles`, so the sitting pose is vanilla's. Dismount (Shift) ends the wait.

**Damage routing.** `DamageRouter` maps every damage type to the body model. Our animals' strikes never use vanilla damage; they call `BodyService.applyInjury(player, injury, cause)` directly.

| Damage type | Becomes |
| --- | --- |
| `fall` | The height table in Danger in detail: sprain, fracture, or a fall outcome; under 2 blocks nothing |
| `in_fire`, `on_fire` | A burn on the touching zone; standing in our flame blocks only |
| `lightning_bolt` | A burn and Condition −30 (Thunderstorms in the video tab) |
| `drown` | Nothing; the water clock in the body model decides |
| `generic_kill` (`/kill`), `fell_out_of_world` | Collapse with cause "command" or "fell", then `SosFlow` |
| Anything else | Logged once as a warning with the type id, then ignored; the M4 smoke fails on any such warning from our own content |

### Collapse, SOS and the flight

**`SosFlow` is a server state machine.** Triggers come from the Collapse and SOS table: BMI 17, warmth 0, Condition 0, drowning, or the SOS hold. An untreated severe condition triggers it only in Hardcore.

1. **Collapsing (4 real seconds).** Input is blocked, the camera sinks and greys (`BodyCamera`), and the close clock runs. Any carried load drops as its world object.
2. **SOS sent.** The client opens `client.screen.SosFlightScreen`: white noise and rotor sound, "SOS sent 14:22, day 143", and the one-line "why I left" field (60 characters, skippable). The player is set to spectator mode, frozen and hidden.
3. **Away.** The player's clock jumps 14 game days in closed form (per player; in single-player the world jumps with it), and every loaded chunk runs its chunk-load catch-up for the gap. The engine tab ran the 14 days at the sleep rate, which would hold the player on a screen for 5.6 real minutes; the jump uses the same catch-up rolls in about 2 seconds, so the outcome is the same and pillar 2 holds. In Endless the SOS screen also offers Stay out for 1, 2 or 5 years (Ruhan, 2026-10-19); those run AgeWorldRunner over the gap (Decay and time › Time away), return the player on Pete's flight with the starting kit, and ship in M9.
4. **The left pack.** Everything carried except worn clothing and the messenger goes into `yearofice:left_pack`, a block entity at the collapse spot (or the nearest bank for drowning), pinned on the map. Animals take food from it by the cache theft rules. This follows Collapse and SOS and the M4 test; the Inventory section's "keeps the pack and gear" sentence is superseded.
5. **Return.** On day +14 the player stands on the gravel bar below the outpost in survival mode at BMI 19, Condition 80, warmth, water and energy full, Spirits 40, vitamin C 30 days, with the starting kit (`loot_table/player/starting_kit.json`). Lost fingers or toes stay.
6. **Journal.** The flight, the cause and the line are written; the year card counts "flights out".

**Battery and SOS.** An automatic SOS on collapse always goes out, because it is Endless's promise. The manual SOS page needs 5% battery, as written.

**Hardcore.** The world is flagged ended in `yearofice_world`. The player becomes a spectator, `RunEndedScreen` shows the cause and the year cards, and a run record is written to `<world>/yearofice/run_record.json` and copied to `<game dir>/yearofice/records/`. The title screen gains a "Run records" button (`ScreenEvent.Init.Post`) that opens `RunRecordsScreen`. Reopening an ended world shows the record and allows spectating only. Vanilla's Hardcore toggle in Create World is our Hardcore mode; Endless is vanilla's Survival.

**Multiplayer, co-op later.** On a server, 14 game days of shared time is 5.6 real hours at the 24-minute day. The jump is per player (RECONCILE 115): only that player's body and left pack pass 14 days in closed form; in single-player the world clock jumps with them. The co-op rule (the away player waits 1 game day and returns with the same losses) is the future co-op design, built after 1.0; until then multiplayer is unsupported.

### Inventory and slots

**Slot map.** Vanilla's 41 player slots are kept and gated; Curios adds the rest.

| Slots | Holds | Rule |
| --- | --- | --- |
| 0–8, hotbar | The belt and pockets | Always open; 5 kg limit with no pack |
| 9–17, 18–26, 27–35 | Pack rows A, B, C | Opened by the worn pack's `rows` |
| 36–39, armor | Feet, legs, torso, head outer layers | Armor points 0; durability is condition |
| 40, off-hand | Torch, lantern, spray, messenger, walking stick | F swaps |
| Curios `head_inner`, `torso_inner`, `legs_inner`, `feet_inner` | Inner layers | 1 each |
| Curios `hands_inner`, `hands_outer` | Gloves, mittens | 1 each; drawn on the first-person hands |
| Curios `back` | The pack | 1 |
| Curios `sheath`, `quiver`, `belt` | Knife; quiver; one pouch (possibles bag, pebble pouch, game bag) | 1 each |
| Curios `neck` | Messenger, binoculars, lanyard whistle | 2 |
| Curios `inner_pocket` | Bottle, batteries, pocket stones, boots before bed | 2; items hold at +20 °C while warmth is 30 or more |
| Curios snowshoes | Snowshoes, worn over the boots | 1; on snow over 20 cm the deep-snow slowdown becomes ×0.9 and the feet stay out of powder; ×0.8 on bare ground and slopes over 15°; 20 s to put on or take off (review round; Open world in depth › Traversal) |

**Why three belt slot types.** The engine gave `belt` 2 slots for sheath and quiver, but Curios validates by slot type, not slot index, and the items and hunting tabs add pouches. Typed slots `sheath`, `quiver` and `belt` keep each one honest with a tag (`yearofice:sheathable`, `yearofice:quivers`, `yearofice:pouches`).

**Pack rows from data.** The data map `data_maps/item/pack.json` gives each pack `rows`, `max_kg`, `lash_points` and `sheds_rain`. The engine's one `Inventory` mixin limits free-slot search to open rows.

| Pack | `rows` | `max_kg` | `lash_points` |
| --- | --- | --- | --- |
| None | 0 | 5 | 0 |
| Crate backpack | 2 | 30 | 0 |
| Hide pack | 3 | 35 | 0 |
| Pack frame | 3 | 45 | 3 |

**Taking a pack off.** Rows 9–35 move into the pack's `minecraft:container` component, and lashed loads into its `yearofice:lashed` component. A 100-cycle GameTest checks nothing is lost.

**Weight.** `item.inventory.LoadCalculator` sums the Item model's weight over every slot, Curios slots, lashed loads and the carried load, on `ContainerChangedEvent`-style hooks and Curios change events, not per tick. The load meter reads "22 / 30 kg" against the pack's `max_kg`. A pickup into the pack that passes `max_kg` is refused; any pickup that passes 45 kg total is refused with "Too heavy to carry; drag it?".

**Stacks.** `max_stack_size` from bulk: small 50, medium 20, food in 100 g units up to 80 (8 kg), tools and clothing 1. Food stacks only within one freshness band of 1/40 (engine Spoilage).

**The quiver.** A Curios `quiver` item with a 12-slot container component. `LivingGetProjectileEvent` feeds bows from it first, then the off-hand; arrows never sit in the grid.

**Lash points.** The pack screen shows 3 lash slots when a frame is worn. Each holds one long item (a bow, a pole) or one carry record (a log, a quarter, a small brace, a medium carcass using all 3). They count toward load and render from the frame's Curios renderer.

**No walk-over pickup.** Vanilla collects items by walking over them; the blueprint picks up by use. `ItemEntityPickupEvent.Pre` refuses pickup unless the item is in `#yearofice:auto_pickup` (arrows, feathers, flint flakes) and the option is on, or the player sent `yearofice:pickup`. Item entities are not hit by Minecraft's crosshair, so the client ray-tests item boxes within 3 blocks and shows the prompt "RMB  Pick up flint (0.4 kg)".

**Full pack.** A refused pickup leaves the item with a marker and the line "Pack full: 2 left on the ground". Pressing E within 3 s opens the pack with that stack on the cursor.

**Dropped items.** The lead's rule (Architecture › Vanilla systems we replace) stands: a dropped item lasts 7 game days by its game-minute stamp, items in `#yearofice:never_expires` (keepsakes, notes, the messenger, packs) never expire, and sunk gear becomes `lost_gear`. The map pin lasts the same 7 days. Everything in a `left_pack` never expires.

**No ghost slots (video 2, A9; RECONCILE 170).** A locked row is never drawn, in the pack screen or any container screen. The second video drew the 27 locked slots greyed while the player had no pack, and a viewer asked why they could not use them.

- **No pack.** `PackScreen` draws only the 9 belt slots and a pack outline in place of the grid, with the line "No pack: 9 belt slots, 5 kg. A pack adds rows."
- **With a pack.** Exactly its `rows` appear: 2 for the crate backpack, 3 for the hide pack and the frames. Nothing greyed, nothing padded.
- **Container screens.** A chest, barrel, cache or sled panel beside the pack shows the player's side the same way, so a shift-click can only land in a drawn row.
- **Server side.** `item.inventory.PackMenu` adds `Slot` objects only for open rows, so the menu itself has no locked slots to click, and the engine's `Inventory` mixin still limits free-slot search (Minecraft engine › Inventory and packs).
- **Ties.** Pack rows from data (above), the Curios `back` slot, the load meter and every container block.
- **Build.** `client.pack.PackScreen` and `item.inventory.PackMenu`; no new registry ids. Owner Studio lead. Milestone M2, with the pack. GameTest `yearofice:pack/no_ghost_rows` (M2): the menu holds 9 player grid slots with no pack, 27 with the crate backpack and 36 with a 3-row pack, and a shift-click from a chest never lands outside them. Client smoke shots `client/pack_screen_no_pack` and `client/pack_screen_rows` are compared with golden images (Art and audio production › Golden images and limits).

### Hand-carried loads and carcasses

**The `carry` attachment.** It holds one load: a log, a quarter, a stone, or a carcass record from the Bestiary's Carcasses and bodies, with a mode (`arms`, `shoulders`, `drag`). While it is set:

- the hotbar layer is cancelled and `yearofice:carry_slot` draws one wide slot with the load's name and weight;
- tool, bow and placement inputs are cancelled in `InputEvent.InteractionKeyMappingTriggered`;
- load bands and the carry speeds come from the carcass table, through the `yearofice:load` modifier;
- the camera drops 10 cm and tilts 5° on the shoulder, and a medium carcass in the arms masks the lower 25% of the view.

**Inputs.** Shift + RMB with an empty main hand takes hold; Shift + RMB again shoulders a medium carcass (3 s); Shift + RMB on a block sets it down (1 s); Q drops it at once. Taking the drag rope is Shift + RMB hold with 2 m of cordage in the hotbar or off-hand.

**Drawing it.** First person: `RenderHandEvent` cancels the hands and draws the species' GeckoLib model in the `carried_arms` or `carried_shoulders` pose. Third person: a render layer from `EntityRenderersEvent.AddLayers`. Logs and quarters use plain baked models in the same two poses.

**SOS and loads.** A carried load always drops on collapse, as the blueprint says.

**Lift a full box (video 2, A7; RECONCILE 170).** A chest, barrel or crate of 30 kg or less, contents included, can be picked up whole and carried. The second video used a carry mod to move full chests; this is built in, with no Carry On dependency, and capped by weight.

- **Which boxes.** Blocks in `#yearofice:liftable_containers`: the supply crate (6 kg empty) from M3, and the chest (12 kg), barrel (8 kg) and blue poly barrel (4 kg) when storage blocks ship in M8. The raised cache, the sled and every multiblock are not boxes.
- **Lift.** Shift + RMB held 1 s with an empty main hand. A plain RMB still opens the box, so the hold is Shift-gated. The box becomes the `carry` load in mode `arms`, its contents kept in the record as a `minecraft:container` component, and the carry slot reads "Chest · 24 kg · 19 stacks".
- **Weight.** Total weight is the box plus `LoadCalculator`'s sum of its contents. Over 30 kg it is refused: "Too heavy to lift: 38 kg. Take out 8 kg." From M4 the cap scales with the Strength factor below (24 kg at Strength 0, 34.5 at 100). The carry speeds and load bands apply as for any hand-carried load.
- **Frozen down.** A box that has stood 24 game hours on frozen ground with air below 0 °C, outside a room above 0 °C, is frozen to the ground. Hold LMB 2 s with a hatchet, knife or ice chisel to chip it free first; the prompt reads "Frozen down. Chip it free."
- **Set down.** Shift + RMB on a block (1 s) places it back with its contents and facing. Q sets it down at once in front of the player; with no room there it is refused, "No room to set it down". On collapse it is set down where the player falls, never spilled.
- **Failure cases.** It cannot be set on water or on a carried sled. Contents keep their freshness clocks while carried, so a box of meat carried in July still ages.
- **Ties.** Hand-carried loads (above), the Item model's weights, the load bands, storage blocks, and the outposts: moving a cache between camps is one trip.
- **Build.** `item.inventory.BoxCarry` (lift, chip and set-down rules) on the existing `yearofice:carry` attachment; tag `#yearofice:liftable_containers`; empty weights in `data_maps/item/weight.json`; sounds `yearofice:carry.box_lift` and `yearofice:carry.box_set`. Owner Studio lead. Milestone M3. GameTests `yearofice:pack/lift_box_30kg` (M3): a chest with 18 kg inside (30 kg in all) lifts after a 1 s hold, one with 18.1 kg is refused with the line, and a frozen box needs the 2 s chip first. `yearofice:pack/box_keeps_contents` (M3): 100 lift-and-set cycles of a box with 27 mixed stacks lose no item, keep every freshness stamp and keep facing.

**Strength carries more (video 2, A11; RECONCILE 170).** The load bands grow with the body's Strength, so eating well and getting strong lets the player carry more, and starving shrinks them. Ruhan asked to "increase the slots a person has maybe by eating more and becoming stronger". The belt stays 9 slots; more slots come from bigger packs, and bigger packs need strength.

**The bands, named.** Blueprint › Inventory by weight's four bands get names and scale by a factor f:

| Band | Load at f = 1 | Effect |
| --- | --- | --- |
| Light | Under 15 kg | Full speed |
| Loaded | 15–30 kg | 85% speed, 20% more calories |
| Heavy | 30–45 kg | 65% speed, 50% more calories, no running |
| Overloaded | Over 45 kg | Drag only, at 30% speed |

```latex
f = \left(0.8 + 0.35 \cdot \frac{Strength}{100}\right) \cdot \left(1 + 0.10 \cdot \frac{\min(n, 300)}{300}\right)
```

Every band edge, and the 45 kg pickup refusal in Inventory and slots, is multiplied by f. Strength is read from `BodyService.strength` (Condition × clamp((BMI − 17)/6, 0, 1)), so f runs from 0.8 at Strength 0 to 1.15 at Strength 100; Strength 57 gives the table as written.

**Hauling.** A Practice skill (Crafting and building in depth › Practice): one rep is 100 m walked in the Heavy band, N is 300, and Practiced adds +10% to f. At the daily cap of 60 reps that is at least 5 days; in normal play, meat seasons and wood hauling take about one game year.

**Packs need strength.** `data_maps/item/pack.json` gains `min_strength`. Below it the pack's usable `max_kg` falls to 30, and anything over 30 kg in that pack counts as Overloaded.

| Pack | `rows` | `max_kg` | `min_strength` |
| --- | --- | --- | --- |
| Crate backpack | 2 | 30 | 0 |
| Hide pack | 3 | 35 | 0 |
| Pack frame | 3 + 3 lash points | 45 | 40 |
| Large pack frame (new, `yearofice:large_pack_frame`) | 3 + 3 lash points | 55 | 60 |

The large pack frame is Hide and bone tier: 6 spruce poles, 2 cured hides, 10 m of cordage and a hide hip belt, 6 game hours at the workbench, 3 kg. Rows stay at 3 because vanilla's main inventory has 27 slots; its gain is weight, which only a strong, practiced body can use (at f = 1.265 the Overloaded edge is 56.9 kg).

**What the player reads.** The load meter marks the band edges as ticks, which move as Strength changes. The daily body line adds the bands: "Weight −0.3 kg (68.9 kg). BMI 20.6. Strength 61. Heavy from 30.4 kg." Wearing a frame below its minimum: "Too weak for the frame: 30 kg before it drags you. Strength 34 of 40."

**Failure cases.** A player at BMI 19 and full Condition has Strength 33, f 0.92, and Heavy from 27.5 kg, too weak for the pack frame. That is the readable cost of starving. Injuries lower Condition and so Strength the same way; a Hurt arm changes tools, not bands (Body, injury and sickness).

**Ties.** Body (Strength, BMI, Condition), Practice, packs, hand-carried loads and lifted boxes, kcal burn by band, and the Overloaded sprain risk on slopes (Blueprint › Danger in detail).

**Build.** `sim.body.LoadBands` (pure: f and the band edges); `item.inventory.LoadCalculator` reads it; `build.craft.PracticeService` gains the skill `hauling`; `data_maps/item/pack.json` gains `min_strength`; item `yearofice:large_pack_frame`; tuning `[body] strength_band_min = 0.8`, `strength_band_max = 1.15`, `hauling_bonus_pct = 10`. Owner Studio lead (Body and survival for Strength). Milestones: the named bands with M2 lane B (f fixed at 1); Strength scaling, Hauling, `min_strength` and the large pack frame in M4. GameTests `yearofice:pack/strength_scales_bands` (M4): at Strength 0 the edges are 12, 24 and 36 kg ±2%, at 100 they are 17.25, 34.5 and 51.75 kg, and at 57 within 1% of the table. `yearofice:pack/hauling_practice` (M4): 300 reps take at least 5 game days and add exactly +10%; a 100 m walk that leaves the Heavy band counts nothing. `yearofice:pack/pack_min_strength` (M4): a Strength-35 player with 32 kg in the pack frame is Overloaded, and at Strength 40 Heavy.

### The HUD layer stack

All layers register in `RegisterGuiLayersEvent`. Vanilla health, food, armor, air, experience, vehicle health and effects are cancelled in `RenderGuiLayerEvent.Pre`; hotbar, crosshair, selected item name, chat, boss bar, title and the player list stay.

**Draw order, bottom to top.** These are the 15 layers of Architecture › Layer 8, including the two this section added: `headwear`, under the frost, and `captions`, which replaces vanilla's subtitles for accessibility.

| # | Layer | Shows | Anchor | Shown when |
| --- | --- | --- | --- | --- |
| 1 | `yearofice:headwear` | Head-net mesh, goggle slits, hood rim | Full screen | The garment is worn |
| 2 | `yearofice:frost` | Frost by warmth stage, hood frost below −30 °C | Full screen | Warmth under 60 |
| 3 | `yearofice:body_fx` | Fever shimmer, sickness dim, blackout fade, kneel tint, colour loss below 3 bolts, bug specks, snow-blindness wash, brush-up sprigs | Full screen | Each by its rule |
| 4 | `yearofice:threat_flash` | Edge pulse while a screen is open and a threat is within 30 m | Screen edge | Pack, messenger or station open |
| 5 | `yearofice:vitals_strip` | Need icons, exposure line | Top right | An icon past its first warning |
| 6 | `yearofice:body_figure` | Five zones | Top left | A zone needs attention, or pinned |
| 7 | `yearofice:load_meter` | "22 / 30 kg" | Right of the hotbar | Always, amber at 15 kg, red at 30 kg |
| 8 | `yearofice:carry_slot` | The wide slot | Over the hotbar | A load is carried |
| 9 | `yearofice:pickup_line` | "+3 birch bark" | Above the hotbar | 2 s per pickup |
| 10 | `yearofice:messenger_line` | One unprompted line | Top centre | From the Messenger and HUD bus |
| 11 | `yearofice:prompt_line` | Key, action, weight; second row for look readouts | Below the crosshair | Looking at something usable |
| 12 | `yearofice:work_ring` | Ring and clock face | Crosshair | A job is filling |
| 13 | `yearofice:breath_ring` | Breath under water | Crosshair | Submerged |
| 14 | `yearofice:captions` | Directional captions (replaces vanilla subtitles) | Bottom right | Subtitles option on |
| 15 | `yearofice:minigame` | Bow drill gauges, cordage marker, pull-and-brace, the call rhythm bar, the sling beat, the fishing tension arc | Lower centre; the arc at the crosshair | A minigame or call runs |

**One owner for the lower centre.** As the lead settles RECONCILE 8, `CallRhythmOverlay` is a renderer inside the `minigame` layer, and `client.hud.MinigameSlot` gives the slot to one owner at a time: a minigame, the call rhythm bar or the sling beat.

**Clashes with vanilla.**

- The selected item name sits where the pickup line goes. While a pickup line shows, the item name is suppressed for its 2 s.
- System toasts appear top right. The vitals strip hides while a toast is on screen (at most 5 s).
- F1 hides HUD layers 4–15. Layers 1–3 still draw, because they are the view, not the HUD.
- With the battery at 0, layers 5, 6 and 10 go dark and map pins vanish; the body cues remain.

**Budget.** All 15 layers together under 0.3 ms a frame at GUI scale 3, with no allocation per frame. Text is cached per string and re-laid only when it changes.

### The vitals strip and the body figure

**Strip icons.** 9 × 9 GUI pixels, right to left in this order, each with a shape that reads without colour.

| Icon | Appears | Glyph |
| --- | --- | --- |
| Bleeding | Any wound bleeding (Body, injury and sickness › The status list) | Two falling drops |
| Fever | Fever cycle active | Thermometer with waves |
| Warmth | Below 70 | Thermometer; fills down |
| Wet | Above 30% | Droplet |
| Water | Below 60 | Cup |
| Calories | 24 h balance below −1,000 kcal | Bowl |
| Energy | Below 5 bolts | Bolt with dimmed capped segments |
| Condition | Below 60 or any problem | Cross |
| Stomach | Above 70%, below 15%, or eating blocked | Sack bar |
| Nausea | 50 or more | Swirl |
| Bugs | Exposed | Mosquito |
| Spirits | Low only | Down-turned arc |
| Fire danger | High or Extreme with a player fire lit | Flame with ring |
| Headache | Coal air | Jagged arc |
| Battery | Below 20% | Battery outline |

The exposure line ("Outside like this: 40 min.") sits under the strip when air feels-like is below −20 °C outdoors. An option, "Numbers on the strip", adds the value under each icon; it is off by default.

**The body figure.** A 24 × 48 GUI-pixel outline, top left. Each zone carries a fill and a glyph so colour is never the only cue: cold is blue with a snowflake, wet adds a droplet, frostnip or frostbite is vermilion with diagonal hatching, worn under 30% adds a crack. An arrow beside it shows warming or cooling. It tints the right zone within 1 second of a change.

**The camouflage view.** The pack screen's Body panel has a Camouflage toggle. It fills the outer layers by class, each with a pattern as well as a word: Bright (stripes), Neutral (plain), Drab (dots), Snow smock (white with a snowflake), plus "On snow now" when the smock's snow rule applies. No sight distance is shown; animals' alert distance shows only through behavior, as Camouflage says. Brush-up sprigs draw in `body_fx` at the top corners; the head net's mesh and the goggles' slits draw in `headwear`.

### Messenger screens

**The Trail Messenger: one screen, many pages.** `client.messenger.MessengerScreen` opens on M, is non-pausing (pausing on Gentle), and raises the device in the left hand first (0.3 s), so the bow cannot be drawn. Q and E or the mouse wheel change pages; 1–0 jump to a page; M or Esc closes. Holding M for 1 s swaps °C and °F for 5 s.

| Page | Class | Battery while open | Data |
| --- | --- | --- | --- |
| Vitals | `VitalsPage` | None beyond idle | `body_sync`; the stomach percent sits here (RECONCILE 13); statuses Ketosis, Keto-adapted, Weakness, the grime word, Rest day; the partner line in Expedition |
| Map | `MapPage` | 4% a game hour | `map_tile` payloads, pins, breadcrumbs, waypoints |
| Forecast | `ForecastPage` | 0.5% a look | `env_sync` forecast fields; the daylight line on top (RECONCILE 13); card percentages in 10% steps from the shown state (RECONCILE 39) |
| Prompts | `PromptsPage` | None | The morning line first |
| Notebook | `NotebookPage` | None | Recipes, Plants, Sign, Spots, Logbook, Practice |
| Stores | `StoresPage` | None | Cache records |
| Fires | `FiresPage` | None | `fire_roster` |
| Ambitions | `AmbitionsPage` | None | Known attachment |
| Journal | `JournalPage` | None | Journal attachment, monthly snapshots |
| Radio | `RadioPage` | Mast battery | Shown once the mast works |
| SOS | `SosPage` | 5% | Hold 3 s, then confirm |

The 4% a game hour for the map follows Controls and interaction, which matches the engine's 7-day battery test; the 3% in The messenger map is superseded.

**Messenger lines.** Every unprompted line comes through the Messenger and HUD bus (HudBus) as a typed line (`alert`, `body`, `warn`, `season`, `event`, `look`, `card`, `prompt`, `nearmiss`, `journal`) with lang key `messenger.yearofice.<type>.<id>` and slots filled by `client.ui.Units`. The layer shows one at a time for 4 s plus 0.05 s a character, at most 8 s, with a queue of 3. During the close clock only danger lines show. The journal type never reaches the HUD.

### Other screens

| Screen | Class | Opens with | Pauses | Server side |
| --- | --- | --- | --- | --- |
| Pack | `client.pack.PackScreen` | E | Gentle only | `item.inventory.PackMenu`: 41 vanilla slots, Curios slots as `CurioSlot`, 3 lash slots, the open container, crafting panel from pack plus storage within 8 m |
| Workbench, furnace, forge | `client.screen.StationScreen` with a station tab | RMB on the block | No | `build.StationMenu` per station; recipe list from known `yearofice:station` recipes, missing inputs greyed, Queue adds a job (at most 8) |
| Mortar | `client.screen.PoundingScreen` | RMB on the mortar | No | `food.Pounding` menu for `yearofice:pounding` |
| Bow drill | `client.minigame.BowDrillScreen` | RMB hold with the kit at a cleared spot | No | `build.work.MinigameJudge` |
| Knapping | `client.minigame.KnappingScreen` | RMB hold at a flat rock with core and hammerstone | No | `build.work.MinigameJudge` |
| Rest radial | `client.screen.RestRadial` | Z | No | Sit, Wait by the fire 1–12 h, Sleep until rested, Sleep until dawn, Rest today; "Sleeping here: about 3 h before cold wakes you" |
| Voice wheel | `client.screen.VoiceWheel` | V hold | No | 12 segments: Shout, Howl, Owl hoot, Quork, Pish, Clap, Mouse squeak, Hen cluck, Go-back, Loon wail, Crane rattle, Owl trill (RECONCILE 124, 133) |
| Wolf wheel | `client.screen.WolfWheel` | G hold | No | Commands from Taming a wolf |
| Inspect card | `client.screen.InspectCard` | RMB tap on a plant, track or sign | No | Look, Smell and Cut checks |
| Carcass and kill cards | `client.screen.CarcassCardScreen` | RMB on a carcass | No | Dressing steps |
| SOS flight | `client.screen.SosFlightScreen` | `SosFlow` | Input blocked | The "why I left" line |
| Run ended, run records | `RunEndedScreen`, `RunRecordsScreen` | Hardcore end; title screen | Yes | Run record files |
| First run | `client.screen.FirstRunScreen` | First join of each world | Yes | Body (height, weight, sex), realism preset, comfort settings, units, palette |

**Curios' own screen.** Curios adds its own inventory button; our pack screen hosts its slots instead, so the button is turned off in our default Curios client config and hidden on `ScreenEvent.Init.Post` if a player turns it back on.

**Vanilla's inventory.** `ScreenEvent.Opening` replaces `InventoryScreen` with `PackScreen`, so the 2 × 2 grid and the recipe book never show. Creative mode keeps vanilla's creative screen for testing.

### Minigames in Minecraft terms

**Who simulates what.** Outcome-only minigames run on the client and the server judges plausibility, as the engine says. The fishing fight decides a valuable outcome over many seconds, so it runs in lockstep instead.

| Minigame | Host | Input | Simulation | Payloads | Close clock |
| --- | --- | --- | --- | --- | --- |
| Fishing fight | Overlay; tension arc at the crosshair | LMB hold reel, release give line; mouse sideways sets side pressure; RMB hold lifts the rod; wheel sets drag; W A S D walk the bank at half speed | Lockstep: `sim.fishing.FightModel` on both sides, 20 steps a second | `fish_input` (1 byte a tick, batched every 4 ticks), `fish_state` (server, 5 a second), `fish_result` | Yes |
| Bow drill | Non-pausing transparent screen | Mouse or A/D strokes; RMB pressure; the blueprint's E taps are RMB taps | Client; judged | `minigame_result` (heat curve summary, duration, strokes) | Yes |
| Knapping | Screen with the core in 3D | Mouse turns the core and sets the angle; LMB strikes | Client; judged | `minigame_result` (strike list) | Yes |
| Cordage | Overlay | RMB press and release in rhythm | Client; judged | `minigame_result` (mean centering) | Yes |
| Pull and brace, self-rescue | Overlay | RMB hold to pull, release on the shake | Server-led: the shake times come from the server | `work_progress` | Yes |
| Splitting | World reticle on the end grain | LMB | Server checks the hit point | `swing` | No |
| Call rhythm | A renderer in the `minigame` layer | Use key holds and taps; V wheel for voice calls | Client; the server rebuilds hold time from use ticks | `call_performed` | No |
| Sling | Overlay pulse with Assisted minigames only | RMB hold whirl, release | Server | `swing` | No |

**The fight at 20 steps.** The blueprint ticks fish states 30 times a real second. Minecraft ticks 20, so the model steps 20 times a second with the same per-second rates, and the client interpolates the arc between steps.

**Judging.** `build.work.MinigameJudge` rejects a result whose duration is under 70% of the fastest legal run, whose energy or wetness disagrees with the body record, or whose scores exceed the practice cap. A rejected result counts as a failed attempt and costs its kcal.

### Hold-to-work and the use key

**Tap or hold.** The use key (RMB) is read in `InputEvent.InteractionKeyMappingTriggered` and a client tick. Under 0.25 s it is a tap (pick up, open, inspect, drink). At 0.25 s, if the target has a work action, the client sends `work_start` and cancels vanilla's repeat-use; the ring starts.

**The ring.** `work_ring` draws a 20-pixel ring and a clock face showing game time racing at the time-lapse rate. The server sends `work_progress` every 5 ticks; the client predicts at the action's rate between them and snaps on each payload. A danger stop arrives in the next payload, so the ring drops within one tick of the server's decision. Z tapped while the ring fills switches to work until done.

**Breaking blocks.** Vanilla LMB mining stays only for blocks in `#yearofice:swing_break` (face rock, iron outcrop, ice, frozen soil), timed by `BreakSpeed`. Every other block's LMB is a tool swing (`swing` payload) or nothing; digging soil is a hold-to-work job on RMB with a digging tool, so it gets a work preview.

**Hold guards.** The 1 s guards from Fire mishaps (smothering, unboiled sips, pouring) draw a short amber ring. The server re-checks hold length in `work_start`.

### Input and keybinds

**The rule.** Minecraft's defaults win; our actions take free keys in the category "Year of Ice", all rebindable through `RegisterKeyMappingsEvent`.

| Action | Default | Key mapping id | Note |
| --- | --- | --- | --- |
| Move, jump | W A S D, Space | Vanilla | Space held at a climbable spruce climbs |
| Sprint | Ctrl, or double-tap W | Vanilla | Blocked by our rules above |
| Crouch and stalk | Shift | Vanilla sneak | Also dismounts a seat |
| Use, pick up, inspect, drink | RMB tap | Vanilla use | Item pickup by our ray |
| Work | RMB hold | Vanilla use | 0.25 s threshold |
| Swing, chop, strike, reel | LMB | Vanilla attack | — |
| Draw the bow, cast | RMB hold | Vanilla use | — |
| Hotbar | 1–9, wheel | Vanilla | Flute notes on 1–6 while the flute is raised; wheel sets drag in a fight |
| Off-hand swap | F | Vanilla | — |
| Pack | E | Vanilla inventory | Opens `PackScreen` |
| Quick eat | R | `key.yearofice.quick_eat` | Eats one portion of the marked quick food, else the leftmost hotbar food |
| Drop | Q; Ctrl + Q for a stack | Vanilla | Drops a carried load at once; 0.5 s hold over water from a canoe |
| Messenger | M | `key.yearofice.messenger` | Hold 1 s swaps units |
| Rest | Z | `key.yearofice.rest` | Tap during a ring: work until done |
| Whistle, wolf wheel | G tap, G hold | `key.yearofice.whistle` | — |
| Talk, voice wheel | V tap, V hold | `key.yearofice.voice` | Releasing the wheel without moving is Shout |
| Bear spray | B | `key.yearofice.spray` | Readies it from wherever it is carried |
| Light | L tap; L hold 0.5 s | `key.yearofice.light` | Hold switches headlamp beam and wide mode |
| Read | X tap; X hold 1 s more | `key.yearofice.read` | Hold pins the line on the map |
| Binoculars | C hold | `key.yearofice.binoculars` | From the neck slot |
| Open coat, mittens off | O, N | `key.yearofice.open_coat`, `mittens` | — |
| Rotate a piece | Left Alt + wheel | `key.yearofice.rotate_modifier` | — |
| Blueprint mode | K | `key.yearofice.blueprint` | Mark two corners to save |

**Conflicts settled (RECONCILE 5 and 31).**

| Key | Claims | Decision |
| --- | --- | --- |
| R | Engine quick eat; Land's Read key | Quick eat keeps R; Read moves to X |
| F | Blueprint whistle; engine off-hand | Off-hand, as vanilla; the whistle is G |
| G | Blueprint drop; engine whistle | Whistle; drop is Q everywhere, so the items tab's "Dropping it (G)" reads Q |
| V | Talk, shout, voice calls, wolf talk, pish | Tap is Talk (one 3 s calm line, the unit the wolf rules count); hold opens the voice wheel with Shout as the default and Pish as a segment. Empty-hand RMB on an animal within 20 m is a Talk alias only when the ray hits it, sent as `yearofice:talk` |
| B | Bear spray | No clash; vanilla's narrator is Ctrl + B |
| X | Blueprint off-hand | Freed by F; now Read. Vanilla's creative "load hotbar" on X is unbound on first launch |
| C | Blueprint crouch | Crouch is Shift; C is binoculars. Vanilla's creative "save hotbar" on C is unbound on first launch |
| L | Vanilla advancements | Unbound on first launch, as the engine says |

**Tap and hold everywhere.** One threshold, 0.25 s, for every dual key. Every hold can be set to toggle in the client config (`holdToToggle`), as the blueprint's assist options require.

**Third person.** F5 works, but the world option `yearofice:first_person_only` (default on) returns the camera to first person each tick, because third person sees over cover and breaks stalking. Screenshots and creative test worlds turn it off.

**Debug screen.** The game rule `reducedDebugInfo` is set true, so F3 shows no coordinates and the messenger map stays the only map. Developer worlds turn it off.

**Controllers and touch.** Touch is dropped. There is no built-in controller layer; every action is a `KeyMapping`, so controller mods that bind key mappings (such as Controlify) work. Radial wheels accept mouse-delta direction, which those mods emulate from a stick. Holds-to-toggle covers triggers that do not hold well.

### First-person feel and camera

**Hands.** `RenderHandEvent` draws both arms even when the off-hand is empty. Skin and glove layers come from the hand slots. The hand texture has 3 grime steps (grime 60, 80, and nails first), a wet-sleeve darkening, bloody hands after gutting until washed, frostbite red and waxy white, and a 0.92 arm-width scale below BMI 18.5.

**Tool animations.**

| Motion | How | Speed |
| --- | --- | --- |
| Swing, chop, split | `IClientItemExtensions.applyForgeHandTransform` curves | From the action's real seconds, ×1.5 below 3 bolts |
| Bow drill, rod bend, blowing on tinder, knife draw-cut, eating fat | GeckoLib animated items | Synced to the minigame beat or the work rate |
| Bow draw | Hand transform plus 10% FOV narrowing at full draw | Shakes below the strength need and past 6 s |
| Belt-tightening, head shake when full, hands steaming | Short GeckoLib hand clips | Triggered by `body_sync` events |

**Camera effects.** `client.camera.BodyCamera` runs every camera change through `ViewportEvent.ComputeCameraAngles` and one whitelisted mixin, `CameraMixin`, which adds an eye offset after vanilla's camera setup. The camera entity never changes. Vanilla view bobbing is replaced by ours on the same option, so its 2 cm amplitude and load rules hold.

| State | Effect | Mechanism | Option |
| --- | --- | --- | --- |
| Shivering | 0.3° tremor | Angles | Off with Reduce screen effects |
| Slowed | Turning lag, mouse sensitivity ×0.7 | Client sensitivity scale | Halved with Reduce |
| Collapse close | Sinking 20 cm, tunnel vision | Eye offset; frost 70% | Frost halved with Reduce |
| Stumble, fall (an animal's knock-down: the encounter rows below) | Dip 0.15 m, pitch 35°, roll up to 40° | Angles and offset | Roll halved with Reduce |
| Nausea | Slow sway, period 6 s, up to 2° | Angles | Sickness visuals Full or Soft |
| Fever, concussion | Edge shimmer; blur | `body_fx`; vanilla `blur` post chain when no shader pack is active, otherwise a soft vignette | Sickness visuals |
| Vomiting | Kneel 4 s | Eye offset 0.6 m down | Soft replaces it with a dim and a cough; Off with a text line |
| Snow blindness | White wash rising over 20 s, held a day at 40% | `body_fx` | Never a flash |
| Load on the shoulder | 10 cm lower, 5° tilt | Offset and roll | — |
| Knock-down by an animal (RECONCILE 171) | 0.4 s fall: pitch to the sky, roll 25°, dip 1.4 m to ground eye height; hands fly out of view; the held item drops by the fall rules | EncounterCamera: angles and offset | Roll halved with Reduce |
| Play dead (hold Shift while down) | Roll face-down over 0.6 s; view 85% dark with moss or snow at the edges; both hands over the back of the neck | Angles, body\_fx mask, hand clip cover\_neck | Roll halved with Reduce |
| Bite or stomp jolt | 0.15 s shake of 3° and a red vignette pulse, with a muffled crunch | Angles, body\_fx | Shake halved with Reduce; the pulse obeys the flash cap |
| Stay down, get up | Breathing; a 30 s hold timer after the bear leaves, shown only once warn-once stay\_down has fired; releasing Shift after it rolls back up over 1.2 s, hands pushing off the ground | Angles and offset, hand clip push\_up | Roll halved with Reduce |
| Fight back, crawl | LMB while down or pinned swings desperate\_strike with the held item or a fist; W while down after a moose stomp crawls at 0.5 m/s, the right answer to a moose | Hand clip; eye offset at ground height | — |

**Encounter camera and hand clips (RECONCILE 171).** The player stays in first person (`yearofice:first_person_only` stays on), and the vanilla player model is not GeckoLib, so an encounter is shown through the camera and the first-person hands. The animal's side is in Entities and AI › Encounter clips.

| Hand clip | Length | Plays on |
| --- | --- | --- |
| `cover_neck` | Held | Play dead: both hands over the back of the neck |
| `push_up` | 1.2 s | Getting up after the stay-down timer |
| `desperate_strike` | 0.5 s | LMB while down or pinned, with the held item or a fist |
| `catch_fall` | 0.3 s | A fall: hands forward into view (Body, injury and sickness › Falls, stumbles and slips) |
| `stagger` | 0.4 s sway | A stumble |

**Build.** `client.camera.EncounterCamera` extends `BodyCamera` through the same `CameraMixin`; the hand clips are GeckoLib hand animations in `client.render.hands`, fired by `body_sync` events. Warn-once id `stay_down`. Sounds `yearofice:body.breath_panic`, `body.hit_grunt`, `body.bite_crunch` and `item.pack_tear`. Owner Look, sound and UI. Milestone M4, with the body and the BodyCamera mixin; the bear sequence runs end to end from M6. Client screenshot check `encounter_play_dead` (M4): the view is at least 85% dark and both hands show. The Reduce screen effects option halves every roll and shake here, and red pulses obey the flash cap (Accessibility).

**Field of view.** Vanilla widens and narrows FOV with movement speed, so our load and cold modifiers would squeeze the view. `ComputeFovModifierEvent` removes the share from `yearofice:*` speed modifiers, keeps vanilla's sprint widening, and adds the bow's 10% and the binoculars' 4× zoom.

**Breath fog.** Particle `yearofice:breath`, spawned by the client from the head of every visible player, wolf and big animal. Below 0 °C: a puff every 4 s idle, 3.5 s walking, 1.5 s running; below −20 °C denser, lasting 2 s. In first person the puff starts 0.3 m ahead of the eye and is skipped at the Minimal particle setting. Other players' exertion comes from `body_public`.

### Rendering

**Seasonal tints.** `client.season.SeasonState` holds the stage from `env_sync` and the year's drawn dates. Colour handlers (`RegisterColorHandlersEvent.Block` and `.Item`) read the stage and the column's cover class from `assets/yearofice/season/palette.json` (cover × stage → RGB). A stage change re-meshes nearest sections first over 30 real seconds. Gameplay states (ripe berries, seeded fireweed) are block-state models with their own shapes, so no rule depends on tint.

**Sky and aurora.** `client.sky.YoiSkyEffects` overrides `renderSky`, `renderClouds` and `adjustLightmapColors`. The sun and moon use the true elevation and azimuth from the daylight table; the moon its 29.53-day phase. The aurora is 3–6 ribbon meshes with the core shader `yearofice:aurora` (registered in `RegisterShadersEvent`), additive, with brightness changes slower than 0.5 Hz. Under an Iris shader pack that draws its own sky, the ribbons may not show, so aurora nights also tint the night sky green through `ComputeFogColor`.

**Held light.** The lead settles RECONCILE 35 (Architecture › Vanilla systems we replace): gameplay light comes from `env.light`, and rendering is client-only, with no light blocks. `client.light.HeldLight` registers our light items with a dynamic-lights mod when one with a NeoForge API is installed; otherwise the lead's `HeldLightMixin` raises block light for up to 8 sources within 16 blocks on the vanilla renderer, and under Sodium it lights entities and the held item until the lead's P4 shows a terrain path. This section adds the levels, in `data_maps/item/light.json`, read by both `env.light` and the client.

| Light | Level | Gives | Note |
| --- | --- | --- | --- |
| Headlamp, beam | 11 at the look ray's hit, up to 25 blocks; 5 at the player | A 25 m reach on what it points at | L hold switches mode |
| Headlamp, wide | 12 | About 8 m | — |
| Birch-bark torch; pitch torch | 10; 8 | About 6 m; 4 m | — |
| Tin lantern | 8 | About 4 m | Wind-proof |
| Candle in hand | 6 | About 2 m | — |
| Messenger screen | 1 | The last light | 2% battery an hour |

Below −30 °C the headlamp's levels drop by 3, matching the halved output. The first-person torch and lantern models also carry an emissive glow layer, so a held light reads as lit under every renderer, even where terrain is not yet lit.

**Compatibility matrix.**

| Feature | Vanilla | Sodium | Iris with a shader pack |
| --- | --- | --- | --- |
| Colour handlers, bare-leaf model | Yes | Yes (public APIs) | Yes |
| Held light | Terrain and entities (mixin) | Entities and the held item, terrain after P4 | The pack's own handheld light, plus `env.light` for gameplay |
| Frost, body effects, headwear | GUI layers | Yes | Yes |
| Blur and blue wash post effects | Post chain | Yes | Replaced by the vignette and tint fallback |
| Custom sky and aurora | Yes | Yes | The pack may draw its own sky; the fog-colour fallback stays |
| Sign decals | Yes | Yes | Drawn in the entity stage so the pack treats them as entities |
| Water shadow decal | Yes | Yes | Off when the pack casts shadows |

**The map renderer.** The server renders tiles of 64 × 64 blocks at 1 pixel per 2 blocks: 32 × 32 pixels in a 64-colour palette, about 1 KB raw and 400 bytes compressed. Coarser zooms use 512-block mip tiles at 16 blocks a pixel, and the 65 km zoom uses the Interior's 128 m plan. `client.map.MapTiles` keeps a memory cache of 4,096 tiles in 1,024 × 1,024 `DynamicTexture` atlas pages and a disk cache under `<game dir>/yearofice/mapcache/<world id>/`. On opening the map, the client requests missing visible tiles (at most 64 per request); the server answers at most 16 a tick. Seen, sighted and charted grades, and each cell's seen day for the yearly fade, come in the tile payload. Pins, breadcrumbs, routes and charted lines are drawn as vectors over the tiles.

**Decals for tracks and sign.** `client.render.SignDecalRenderer` draws sign records within 48 blocks: at most 768 quads in one vertex buffer, rebuilt only when nearby records change. Quads sit on the top surface (the snow layer's height), use polygon offset, read one decal atlas, and fade by age. The player's own footprints are written by the server every 1.5 m on snow and mud, capped at 256 records a chunk.

### The Audio bus

**One entry point.** Every sound goes through the Sound events and the audio bus layer: `sound.SoundBus` on the server and `client.audio.AudioBus` on the client. The bus picks the category, the distance curve, the caption and the voice budget.

| Our group | Minecraft category | Why |
| --- | --- | --- |
| Danger cues (huff, charge, ice crack, tree creak, soil trickle) | `hostile`, relabelled "Danger" | Unused otherwise, since there are no monsters; one slider for the sounds that keep the player alive |
| Animals, calls | `neutral` | — |
| Footsteps, body, tools | `players` | — |
| Weather, wind | `weather` | — |
| Ambience beds | `ambient` | — |
| Messenger, radio voices | `voice`, relabelled "Messenger and radio" | — |
| Spirits music | `music` | Vanilla music is replaced through `SelectMusicEvent` |

**Voices.** Minecraft allows about 247 static and 8 streaming sources. Ambience beds stream, at most 4 at once (two crossfading pairs). Our own cap of 32 voices culls loops first, then distant animals; danger cues are never culled. The first-run screen warns when the Danger slider is under 50%.

**Footsteps by surface.** Step sounds come from each block's `SoundType` through NeoForge's context-aware `getSoundType(state, level, pos, entity)`. For `snow_cover` it returns the band for the current air temperature: from the Environment field on the server and `env_sync` on the client. The server reads the same band as the noise factor.

| Surface | Step event | Noise factor |
| --- | --- | --- |
| Gravel, scree | `step.gravel` | 1.3 |
| Forest duff, moss, dry moss | `step.duff`, `step.moss`, `step.dry_moss` | 0.8, 0.7, 1.0 |
| Bog, mud | `step.bog`, `step.mud` | 1.2 |
| Dry leaves, leaf litter | `step.leaf_litter` | 1.5 |
| Grass, sand | `step.grass`, `step.sand` | 0.9 |
| Rock | `step.rock` | 1.0 |
| Plank floor, log | `step.wood_floor`, `step.log` | 1.2 |
| River and lake ice | `step.ice`; under 10 cm `step.ice_thin` (a creak) | 1.0 |
| Loose snow above −5 °C | `step.snow_soft` | 1.0 |
| −5 to −15 °C | `step.snow_crunch` | 1.5 |
| −15 to −30 °C | `step.snow_squeak` | 2.0 |
| Below −30 °C | `step.snow_squeak_loud`; voices and axe blows carry 30% farther | 2.5 |
| Crust (any temperature; squeaks below −15 °C) | `step.crust_crunch` or `step.snow_squeak` | 2.5 |
| Wading, by depth | `step.wade_shallow`, `step.wade_deep` | 1.5 |
| Deadfall underfoot | `step.stick_snap`, 1 step in 8 | 3.0 for that step |

Moccasins and fur footwear play at 0.7 volume and noise; a heavy load plays slower and 10% louder; snowshoes add `step.snowshoe`; the walking stick adds `step.stick_tap`. Vanilla plays no steps while sneaking; that stays, and the server's crouch noise rule still applies.

**Ambience beds.** `client.audio.AmbienceManager` samples the player's 32 m cell every 2 s. A bed is one event per cover kit × season × day or night: 8 × 4 × 2 = 64 streaming loops of 60–120 s, plus the dawn chorus (May to July), 3 indoor beds (cabin calm, stove, storm on the walls) and 1 under water. Each bed carries 0–3 spot layers (a woodpecker, a far raven) on seeded intervals. Crossfades take 4 s on a cover change and 8 s on a weather or time change. In a room the outdoor bed drops 12 dB. Vanilla's underwater loop is replaced in the built-in resource pack.

**Field recordings.** Recordings replace the rendered beds by file swap under the same event ids. Rules:

- 48 kHz sources, encoded to Ogg Vorbis quality 6; beds are stereo 60 s seamless loops; spot and positional sounds mono, because Minecraft only positions mono files.
- Beds at −23 LUFS integrated; danger cues peak 6 dB above the bed so they always cut through.
- Only interior Alaska species and weather. A bed never contains a danger sound or a call of an animal not present, so "Danger is heard first" stays honest.
- Own recordings, generated sounds, or found sounds whose licence allows redistribution in a modpack (CC0, CC-BY or MIT; RECONCILE 116), each logged with its licence and credit in the one credits file, `assets/yearofice/credits.json` (Art and audio production, RECONCILE 141).

**Captions.** `yearofice:captions` replaces vanilla's subtitle overlay under the same "Show Subtitles" option. Each caption reads `subtitles.yearofice.<id>` plus a range word (close under 30 m, near to 100 m, far beyond) and one of 8 directions relative to the view: "\[Huff, close, left\]". Danger captions stay 3 s and draw on an amber field with an exclamation glyph.

**Sound events in this area.** The engine's 60 rendered sounds and the 66 added by the look, sound and UI department stay. The player systems add or own these:

| Group | Events |
| --- | --- |
| `step` (26) | `gravel`, `duff`, `moss`, `dry_moss`, `bog`, `mud`, `leaf_litter`, `grass`, `sand`, `rock`, `wood_floor`, `log`, `ice`, `ice_thin`, `snow_soft`, `snow_crunch`, `snow_squeak`, `snow_squeak_loud`, `crust_crunch`, `wade_shallow`, `wade_deep`, `stick_snap`, `stick_ground`, `stick_tap`, `snowshoe`, `belt_creak` |
| `body` (27) | `breath_walk`, `breath_run`, `breath_ragged`, `teeth_chatter`, `heartbeat_slow`, `heartbeat_fast`, `eat_chew`, `eat_fat`, `drink_gulp`, `drink_bottle_ice`, `stomach_growl`, `stomach_gurgle`, `retch`, `cough`, `dry_swallow`, `yawn`, `relief_breath`, `stumble_scuff`, `slide`, `impact`, `fall_thud`, `grunt`, `sear`, `wash`, `mosquito_slap`, `hum`, `head_shake` |
| `messenger` (7) | `prompt_chime`, `alert`, `low_battery`, `map_open`, `raise`, `page`, `sos_beep` |
| `ui` (4) | `hold_guard`, `minigame_good`, `minigame_bad`, `queue_added` |
| `ambient` (69) | `bed.<cover>_<season>_<day or night>` (64), `dawn_chorus`, `indoor_calm`, `indoor_stove`, `indoor_storm`, `underwater` |

That is 133 events, about 190 files and 30 MB with the beds; the beds are the bulk, so they ship as streams. The whole mod has 409 sound events plus 12 music events, listed with their sources in Art and audio production (RECONCILE 141).

### Networking

All payloads register in `RegisterPayloadHandlersEvent` with one versioned registrar ("1"), as `CustomPacketPayload` records with stream codecs. Handlers run on the main thread through the handler context. Sizes are after Minecraft's compression.

| Payload | Direction | Size | Rate |
| --- | --- | --- | --- |
| `body_sync` | S → owner | 60–140 B (changed fields only, full every game hour) | On change over 1%; about 1 a second at most |
| `body_public` | S → trackers | 6 B (shiver, limp side, breath strength, carry pose, grime step, steam) | On change |
| `env_sync` | S → player | 40 B, plus 9 B forecast when it changes | Each game minute |
| `current_chunk` | S → player | 512 B | On chunk watch |
| `sign_chunk`, `sign_delta` | S → player | 8 B a record | On watch; deltas as they happen |
| `distant_sound` | S → players in range | 16 B | Per sound |
| `map_tile_request`, `map_tile` | Both | 12 B; 400 B a tile | On map open; at most 16 tiles a tick |
| `work_start`, `work_stop` | C → S | 20 B | Per job |
| `work_progress` | S → C | 10 B | Every 5 ticks while working |
| `swing`, `pickup`, `talk`, `key_action` | C → S | 6–20 B | Per input (`key_action` carries light, spray, whistle, coat, mittens, read, rest choice) |
| `minigame_result` | C → S | 30–200 B | Per attempt |
| `fish_input`, `fish_state`, `fish_result` | C → S, S → C, S → C | 4 B, 24 B, 12 B | 5 a second each during a fight |
| `call_performed` | C → S | 16 B | Per call |
| `carry_state` | S → owner and trackers | 30 B | On change |
| `hud_line` | S → owner | 40 B | Per line |
| `morning_report` | S → owner | 30 B | Once a game day |
| `fire_roster_request`, `fire_roster` | Both | 40 B an entry | On open, then every 5 real seconds while open |
| `partner_status` | S → partner | 30 B | Once a game minute, Expedition only |
| `data_sync` | S → player | Varies | Login and `/reload` |
| herd\_state, flock\_state, school\_state | S → trackers | Under 64 B | Every 10–20 ticks per group (request 94) |
| aim\_member, wolf\_command | C → S | 12–16 B | Per input (request 94) |

**Budget.** A walking player costs under 0.3 KB a second down and 0.1 KB up outside the map; a fishing fight adds 0.15 KB a second each way. 8 players stay under 4 KB a second in total.

**What the client may claim.** Only motion (as vanilla), input frames and minigame summaries. Everything else is computed on the server: the client never sends a body value, a weight or a catch.

### Accessibility

**Photosensitivity.** Nothing in the game flashes more than 3 times a second. The thunder flash brightens the lightmap at most +60% for 3 ticks, never a white GUI fill, and 30% with Reduce screen effects. The aurora, fire flicker, threat flash (at most 1 Hz) and the 72-hour microsleeps (0.5 s fades, never a cut) follow the same rule. Snow blindness rises over 20 s. A client test scans 10 minutes of recorded frames from the danger and thunder scenes and fails on any luminance swing over 10% repeated more than 3 times in a second.

**Captions.** Every sound event has a subtitle key; the data lint fails on a missing one. Danger captions carry range and direction. An option, "Danger direction marks", adds a small chevron at the screen edge toward each danger cue.

**Colour.** No rule reads from colour alone: icons and body-figure zones carry glyphs, bars carry length, and ripe plants change shape. The default HUD palette uses colour-blind-safe hues: blue #0072B2 (cold), sky blue #56B4E9 (wet), orange #E69F00 (warning), vermilion #D55E00 (danger), bluish green #009E73 (fresh). An option, "Palette: high contrast", swaps the freshness and condition bars to blue, orange and vermilion with lightness steps of at least 20%. The tip-up flag and eyeshine read by brightness, motion and height off the ground as well as hue.

**Comfort.** Reduce screen effects (frost halved, no tremor, roll halved), Sickness visuals (Full, Soft, Off), holds-to-toggle, Assisted and Automatic minigames, and the units all sit on the first-run screen and in the client config `yearofice-client.toml`.

**Text.** Every HUD string scales with the GUI scale and fits 40% longer translations; the client smoke screenshots the longest lang file at GUI scales 2 and 4.

### Vanilla gaps in the player and client

Everything vanilla still does in this area, decided.

| Vanilla behaviour | Decision |
| --- | --- |
| Walk-over item pickup | Off, except the auto-pickup tag with the option on |
| Item entities despawn after 5 minutes | After 7 game days instead, by the lead's rule; `#yearofice:never_expires` items stay |
| Experience bar, levels, enchanting | Hidden and unused; XP is zeroed by the entity rules |
| Armor bar and armor points | Hidden; armor points 0 on every garment |
| Item tooltips with attack damage and speed | Hidden on our items; the tooltip follows the Item model's fixed order |
| Attack cooldown indicator | Off by default; our swing speed comes from the work rate |
| Mob effects HUD and inventory effect list | Hidden; no body state is a mob effect |
| FOV change with speed modifiers | Our modifiers' share removed |
| View bobbing | Replaced by ours under the same option |
| Vanilla music and cave mood sounds | Replaced through `SelectMusicEvent`; our biomes have no mood sound |
| Underwater ambience loop | Replaced in the built-in resource pack |
| Item pickup pop | Kept, quieter, in the resource pack |
| Eating and drinking sounds | Our food items return our sounds |
| Saved hotbars (C and X) | Unbound on first launch; creative only anyway |
| Advancements screen and toasts | Filtered by the engine; L unbound |
| Recipe book and 2 × 2 grid | Never shown |
| Tutorial hints ("Press E to open your inventory") | Tutorial step set to none on first launch |
| F3 coordinates | Hidden with `reducedDebugInfo` |
| Third-person cameras | Locked by `first_person_only` |
| Spawn point messages, "You may not rest now" | Never sent: beds never set spawn, and our sleep rules give their own reasons in the prompt line |
| Chat and system messages | Kept for multiplayer and commands; nothing in play uses chat |
| Spectator mode during the away phase | Used, hidden, with the screen covering the view |
| Hardcore's death screen | Replaced by `RunEndedScreen` |
| Minecraft's compass, clock, maps, spyglass | Hidden from survival; our compass and binoculars replace them |
| Sneak hides steps | Kept |
| Step height 0.6 | Kept as the base; 1.0 on blocks in `#yearofice:terrain_steps` by a client and server attribute modifier, and 1.0 on any block with the walking stick, walking under 30 kg (RECONCILE 37) |
| Swim speed | Player 1.0 m/s by attribute, crawl 1.4, as the swim table (RECONCILE 33) |

### Client and player registry table

| Id | Type | Class | Data or asset file | Milestone |
| --- | --- | --- | --- | --- |
| `yearofice:body` | Player attachment | `body.BodyRecord` | `body/standard.json`, `preset/*.json` | M4 |
| `yearofice:pack_rules`, `carry`, `practice`, `known`, `map`, `journal`, `jobs`, `work` | Player attachments | `item.inventory.PackRules`, `item.inventory.CarryRecord`, others per engine | — | M2–M5 |
| `yearofice:tool_memory` | Player attachment | `hud.messenger.ToolMemory` | — | M5 |
| `yearofice:battery`, `lashed`, `weight_g`, `wetness`, `fresh` | Data components | Item model codecs | — | M2–M5 |
| `yearofice:pack`, `weight`, `wear`, `food`, `light`, `camo_class` | Item data maps | `item.inventory.PackData`, others | `data_maps/item/<name>.json` | M2–M6 |
| `yearofice:head_inner`, `torso_inner`, `legs_inner`, `feet_inner`, `hands_inner`, `hands_outer`, `back`, `sheath`, `quiver`, `belt`, `neck`, `inner_pocket` | Curios slot types | — | `curios/slots/<id>.json`, `curios/entities/player.json` | M2 (back, neck), M4 (clothing, pocket), M5 (belt), M7 (`snowshoes`, the 13th slot type) |
| `yearofice:ground_bed` | Block | `body.GroundBedBlock` | — | M4 |
| `yearofice:left_pack` | Block with block entity | `body.LeftPackBlock` | `loot_table/player/starting_kit.json` | M4 |
| `yearofice:seat` | Entity | `body.SeatEntity` | — | M3 |
| `yearofice:pack` | Menu type | `item.inventory.PackMenu` | — | M2 |
| `yearofice:station`, `pounding` | Menu types | `build.StationMenu`, `food.PoundingMenu` | `recipe/*.json` | M3, M5 |
| 15 GUI layers: the lead's 13 plus `headwear` and `captions` | GUI layers | `client.hud.HudLayers` | `textures/gui/hud/*.png` | M2 (prompt, pickup, load, carry, work, messenger line, captions), M4 (vitals, body figure, frost, body fx, headwear, breath), M5 (minigame), M6 (threat flash) |
| `MessengerScreen` and 11 pages | Screen | `client.messenger.*` | `lang/*.json` keys `messenger.yearofice.*` | M2 (map, vitals), M4 (SOS), M5 (stores, fires, notebook, prompts), M7 (forecast card), M9 (radio) |
| `PackScreen`, `StationScreen`, `PoundingScreen`, radials, cards, `SosFlightScreen`, `RunEndedScreen`, `RunRecordsScreen`, `FirstRunScreen` | Screens | `client.screen.*`, `client.pack.*` | — | M2–M6 |
| `BowDrillScreen`, `KnappingScreen`, fight, cordage, pull-and-brace overlays | Minigames | `client.minigame.*`, `sim.fishing.FightModel` | `fish/<id>.json` fight profiles | M3, M5, M6 |
| 13 key mappings | `KeyMapping` | `client.input.YoiKeys` | `lang` keys `key.yearofice.*` | M2 |
| `yearofice:breath`, `tool_glint` | Particle types | `client.render.particle.*` | `particles/*.json` | M2, M4 |
| `yearofice:aurora` | Core shader | `client.sky.AuroraRenderer` | `shaders/core/aurora.json` | M7 |
| `YoiSkyEffects` | Dimension effects | `client.sky.YoiSkyEffects` | — | M2 |
| Season palette | Colour handlers | `client.season.SeasonColors` | `season/palette.json` | M7 |
| Sign decals | Renderer | `client.render.SignDecalRenderer` | `textures/decal/*.png` | M6 |
| Map tiles | Renderer | `client.map.MapTiles`, `hud.messenger.MapTileServer` | — | M2 |
| `CameraMixin` | Mixin (1 of 8) | `client.camera.mixin.CameraMixin` | `yearofice.mixins.json` | M4 |
| 133 sound events in the table above | Sound events | `sound.YoiSounds` | `sounds.json`, `sounds/<group>/*.ogg`, `credits.json` | M2 (12), M4 (body), M7 (snow bands, beds) |
| 33 payloads in the network table (28 plus the five entity payloads of request 94) | Payloads | `net.payload.*` | — | M2–M6 |
| `yearofice:first_person_only` | World option (game rule) | `core.rules.YoiGameRules` | — | M2 |
| Client config | Config | `client.YoiClientConfig` | `yearofice-client.toml` (`tempUnit`, `massUnit`, `holdToToggle`, `reduceScreenEffects`, `sicknessVisuals`, `palette`, `dangerMarks`, `stripNumbers`, `morningDetail`, `autoPickup`) | M2 |
| `#yearofice:auto_pickup`, `swing_break`, `terrain_steps`, `sheathable`, `quivers`, `pouches` | Tags | — | `tags/item/`, `tags/block/` | M2–M5 |

### GameTests and client tests

| Test | Kind | Pass when |
| --- | --- | --- |
| `yearofice:body/vanilla_needs_held` (batch `m4`) | GameTest | After a game day of sprinting, falls of 3 blocks and eating, food level reads 20, health 20, no vanilla regen tick fires |
| `yearofice:body/fall_routes` (`m4`) | GameTest | A 4-block fall gives one sprain or fracture by the table and no health change |
| `yearofice:body/sos_jump` (`m4`) | GameTest | A collapse returns the player after 14 game days at BMI 19 with the kit, the left pack at the spot, every placed block intact, in under 5 real seconds |
| `yearofice:body/hardcore_end` (`m4`) | GameTest | The world flag is set, the player is a spectator, and the run record exists |
| `yearofice:body/ground_sleep` (`m4`) | GameTest | A player sleeps 8 game hours on bare ground and no `ground_bed` block remains after waking |
| `BodyModelTest.bmr_scaling` | JUnit | At 77 kg and 183 cm the per-hour burn equals the activity table; at BMR 1,600 it is 0.892 of it, ±0.1% |
| `BodyModelTest.order_deterministic` | JUnit | 10,000 random minutes give the same record from the same inputs on every JVM |
| `yearofice:pack/rows_never_fill` (`m2`) | GameTest | 500 pickups with a crate backpack never land in row C |
| `yearofice:pack/takeoff_roundtrip` (`m2`) | GameTest | 100 off-and-on cycles keep every stack and component |
| `yearofice:pack/no_walkover_pickup` (`m2`) | GameTest | Walking across 20 dropped items picks none up; with auto-pickup on, only tagged items |
| `yearofice:pack/quiver_feeds_bow` (`m5`) | GameTest | 12 shots draw from the quiver, then the off-hand |
| `yearofice:pack/carry_blocks_tools` (`m5`) | GameTest | With a carcass carried, swings, bow draws and placements do nothing; Q drops it in 1 tick |
| `yearofice:net/body_sync_rate` (`m4`) | GameTest | An idle player at the world clock gets at most 1 `body_sync` a second and one full record per game hour |
| `FightModelTest.lockstep` | JUnit | 10,000 seeded fights with recorded inputs give identical outcomes from the client and server copies |
| `SnowStepTest.bands` | JUnit | The step event and noise factor match the four bands at −4.9, −5.1, −15.1 and −30.1 °C |
| `KeyMapTest.no_default_conflicts` | Client JUnit | No `yearofice` default collides with a vanilla default after the first-launch unbinds |
| HUD stack screenshots | Client smoke | All 15 layers drawn with test data at GUI scales 1–4 and 854 × 480 to 1920 × 1080, no overlap in the anchor table |
| Flash scan | Client smoke | The photosensitivity scan above passes |

### Risks and first prototypes

| Risk | Why it matters | Guard |
| --- | --- | --- |
| Held light under Sodium | A dark night with no visible lantern breaks the night | The emissive glow on held models now; the lead's P4 for terrain |
| Client prediction drift | The work ring and the fight must feel instant over a 150 ms ping | Prediction with server snaps; lockstep for the fight |
| Curios and the row mixin against other mods | Other inventory mods may fight the gated rows | Total-conversion stance (engine Risks); the mixin is tested by the 500-pickup test |
| Iris and post effects | Shader packs disable vanilla post chains and may hide the sky | Every post effect has a GUI-layer fallback; aurora has the fog tint |
| Streaming sound cap | 8 streaming sources; music and beds share them | At most 4 beds at once |
| HUD clutter | 15 layers on a 854 × 480 window | Empty-when-well rule; the anchor table; screenshot tests at 4 scales |
| Minigame cheating | Client-side minigames could be scripted | The judge's duration and cap checks; the fight is server-run |
| Third person and F3 as exploits | They break stalking and the map | `first_person_only` and `reducedDebugInfo` |
| Players expect the vanilla death screen | A cancelled death can feel like a bug | The collapse sequence starts in the same tick with sound and camera |

**P1. The HUD layer stack (M2, 3 days).** A branch registers all 15 layers with a debug data source that cycles every state each 2 s. It runs under vanilla, Sodium and Iris with a shader pack at GUI scales 1–4. Pass: no overlap between anchors, under 0.3 ms a frame for the stack, F1 leaves layers 1–3, and the battery-0 state blanks exactly layers 5, 6 and 10.

**P2. The fishing-fight screen (M5, 5 days).** `FightModel` in `sim.fishing` with the five phases and the profiles of grayling, pike 10 kg and lake trout; the overlay arc, rod bend and the close clock; lockstep payloads. Run with an artificial 150 ms and 300 ms delay. Pass: the arc never lags input by more than one frame on the client; the server outcome matches the client's on 100 of 100 recorded fights; a new tester lands a grayling within 3 tries and loses a 10 kg pike on plant-fiber line at least once in 5.

**P3. The body tick at 20 TPS, 1 player and 8 players (M4, 3 days).** A JMH benchmark of `BodyModel.step`, then a GameTest server with 1 and 8 fake players doing scripted days (walk, chop, swim, sleep) at the world clock and the sleep rate, profiled with spark. Pass: one step under 15 µs with no allocation; 8 players at the sleep rate add under 0.4 ms a tick; `body_sync` stays under 1 a second a player; the SOS jump for one player finishes in under 5 real seconds with 2,000 loaded chunks.

**P4. Held light.** The lead runs it (Architecture › Risks and first prototypes). This section's pass line: 8 players with lit headlamps in a night forest keep 60 fps on vanilla and Sodium, and every light level in the table reads within 1 of its value in `env.light`.

### Settled reconciliation items and requests

**Settled here.** 5 (keys: Read to X, drop on Q, whistle on G, off-hand on F), 13 (stomach percent on Vitals and the eat prompt; daylight line on Forecast; thunder flash as a capped lightmap; "Fire, Low"), 14 and 43 (food read per 100 g unit with raw fat 700 and lard 900), 17 (burn scaled by BMR ÷ 1,794), 19 (grime, nausea and fever in `yearofice:body`; no mob effects), 31 (V tap Talk, V hold wheel, the empty-hand alias), 33 (player swim 1.0 m/s), 37 (step height and the walking stick), 39 (forecast percentages shown in 10% steps from the shown state), 44 (damage scaling never, difficulty Normal, in the damage router). Items 8, 35 and 36 follow the lead's decisions; this section adds the light levels and the persistent left pack.

**Changes to the engine tab, with reasons.** The SOS away phase is a closed-form jump, not 5.6 real minutes at the sleep rate. The belt becomes three typed Curios slots, and neck and inner pocket get 2 slots each. The map drains 4% a game hour. Base step height is 0.6 with 1.0 on terrain, so placed walls stay walls. `pack.PackMenu` becomes `item.inventory.PackMenu` under the lead's package map.

**Needed from the other developers (all settled in Architecture and the interlace › Requests between developers, settled).**

- **Lead engineer.** Confirm `item.inventory` as a subpackage of `item` for the inventory code; add `headwear` and `captions` to the Layer 8 list; `BodyService` needs `wake(player, cause)` beside `eat`, `drink` and `applyInjury`; add `yearofice:first_person_only` and `reducedDebugInfo` true to the game-rule table; the `net` package owns the 33 payloads above; the co-op rule for SOS (1 game day away) and the work-time charge (RECONCILE 9) are the future co-op design, built after 1.0 (RECONCILE 115).
- **World developer.** The hourly temperature record for closed-form body steps; `lost_gear` placement for items that sink; the outpost bar's return position in `yearofice_world`.
- **Entity developer.** `BodyService.applyInjury` as the only path for animal attacks on players; the `carried_arms` and `carried_shoulders` poses in every species' geo file; `env.light` (not the held item) as the night-sight input; the predator wake check calling `BodyService.wake`.
- **Build developer.** The data lints: a subtitle key for every sound event, no literal units in lang files, a codec default for every `yearofice:body` field; the 133 sound events and the credits file in the asset pipeline; the client smoke's flash scan and HUD screenshots.

## Content pipeline, build and the evolving modpack

This section is the build developer's plan: how content gets from a department's JSON file into a running world, how every id in the blueprint becomes one registry entry, and how the mod ships and keeps growing as a modpack without breaking a single save. It builds on the Minecraft engine tab (Platform and project, Saves and data, Testing and the studio rules) and on Architecture and the interlace, and changes them only where it says why. The shared layers carry the lead engineer's names: Clock and calendar, Environment field, Scent and sound field, Ecology, Decay and freshness, Item model, Body, Messenger and HUD bus, World edits, and Sound events and the audio bus. The `data` package is this section's; every other package follows the lead's package map.

### What this section decides

- **Content loads through our reload listeners,** one JSON per entry under `data/yearofice/<kind>/`. Datapack registries are used only for Minecraft's own kinds. The Bestiary's `yearofice:species` datapack registry is withdrawn.
- **Code registries are filled from the jar at startup.** A species, plant or fish file in the jar registers its entity type, blocks, items and sound events, so a department can add one without Java.
- **Every load is validated in three passes** (parse, link, check) and fails loudly with one report file. A content linter runs the same checks in CI.
- **One master inventory** of 1,152 `yearofice:` ids, generated from every tab, is the checklist the registry dump is compared against on every build.
- **The mod and the pack use semantic versions** in lockstep (pack MAJOR.MINOR = mod MAJOR.MINOR), build on three private channels (dev, beta, stable) for Ruhan's own installs, with no public release (RECONCILE 119), and promise that every save from 0.3.0 opens in every later version.
- **Saves upgrade by versioned codecs and registry aliases,** tested on committed fixture saves from every release.
- **New features arrive behind content flags,** off by default on stable, fixed per world when they touch generation.

**RECONCILE items.** The lead engineer's Conventions settled decides 6, 16, 18, 19, 21, 22, 32, 34, 35, 41, 43 and 44; this section builds those decisions into the loaders, the linter, the inventory and the pack, and adds the paths and ids they did not list. It settles 1, 7 (rows 32 and 38) and 15 itself, plus the 27 id conflicts in Conflicting ids.

### Two ways data loads

Minecraft offers two paths for JSON data. We use each for what it is good at.

| Path | What uses it | Loaded | Reloads with `/reload` | Synced to clients |
| --- | --- | --- | --- | --- |
| Our reload listeners | Every content kind in Data kinds (species, fish, plant, recipe numbers, events and the rest) | Server start and `/reload` | Yes | Only kinds marked synced, by `yearofice:data_sync` |
| Datapack registries | Minecraft's and NeoForge's own kinds: biome, structure, structure set, template pool, placed and configured feature, world preset, dimension type, noise settings, damage type, biome modifier | World load only | No | By vanilla's registry sync |
| Bootstrap reader | The registration half of species, plant, fish, item-family and flag files, read from the jar | Mod construction, once | No; a change needs a restart | Both sides read the same jar |

**Why not datapack registries for content.** They load once per world, so a balance change needs a world restart; tuning by `/reload` is the departments' main loop. They also force the doubled folder `data/yearofice/yearofice/<kind>/`, which caused six of the path conflicts in RECONCILE 6. Their two strengths, client sync and holder references, we cover with `data_sync` and checked id references at link time.

**Why a bootstrap reader.** Minecraft freezes its code registries (blocks, items, entity types, sound events) before any data pack loads. A data-only species therefore cannot come from a data pack. It can come from the mod's own jar, which `com.yearofice.data.Bootstrap` reads during mod construction through `ModList.get().getModFileById("yearofice").getFile()`. A world's data packs may tune a species; only the jar may add one.

**One file, two readers.** A species file has a `register` block, read by the bootstrap reader, and everything else, read by the reload listener. The reload listener refuses a file whose `register` block differs from what was registered at startup, with the line "Restart needed: register block changed".

**Reload is atomic.** `prepare` runs on the reload worker: it parses and validates every kind into a new snapshot. `apply` swaps the snapshot in on the server thread in one assignment. Systems never cache entries; they hold ids and look up per use through `Kinds.<kind>().get(id)`.

**A failed reload keeps the old data.** In dev builds the server also logs the report at error level and sends it to every operator in chat. On the first load of a world, a failure stops the world from opening and the screen names the report file.

**Frozen kinds.** Region, archetype, district, district type, interior plan, province kind, natural feature, setpiece placement and `worldgen_params` are copied into SavedData `yearofice_world` at world creation, as the engine tab requires. A later `/reload` re-validates them but never changes the copy.

**Synced kinds.** Species names and calls, fish, plant phases and verbs, notes, prompts, phenomena tells and dye colours go to the client. `data_sync` is sent in parts of at most 512 KB, each with a CRC32, and ends with a SHA-256 of the whole snapshot. The client answers with `yearofice:data_hash`; a mismatch disconnects with "Data out of step: rejoin". Data maps sync by NeoForge's `synced` flag.

### Data kinds

Every top-level folder under `data/yearofice/` must be declared in the code registry `yearofice:data_kind` (`com.yearofice.data.KindRegistry`). A folder that is not declared fails the linter. A kind is either **entries** (one JSON per entry, the id is the file path) or **tables** (named singleton files, each with its own codec).

| Kind | Path | Mode | Owner | Synced | Frozen | Milestone |
| --- | --- | --- | --- | --- | --- | --- |
| Species | `species/<id>.json` | Entries + bootstrap | Ecosystem | Yes | No | M5–M6 |
| Fish | `fish/<id>.json` | Entries + bootstrap | Food and hunting | Yes | No | M5 |
| Plant | `plant/<id>.json` | Entries + bootstrap | Body and survival (forage) | Yes | No | M5 |
| Crop | `crop/<id>.json` | Entries + bootstrap | Body and survival | Yes | No | M5 |
| Water life | `water_life/<id>.json` | Entries + bootstrap | Food and hunting | Yes | No | M7 |
| Set piece | `setpiece/<id>.json` with `structure/setpiece/<id>/<variant>.nbt` | Entries | World generation | No | Placement yes | M1 |
| Region, archetype | `region/<cell>.json`, `archetype/<id>.json` | Entries | World generation | No | Yes | M0 |
| District, district type, province kind, interior | `district/`, `district_type/`, `province_kind/`, `interior/plan.json` | Entries; interior is a table | World generation | No | Yes | M10 |
| Natural feature, paper map | `natural_feature/<id>.json`, `paper_map/<id>.json` | Entries | World generation | Paper maps yes | Features yes | M9–M10 |
| Worldgen parameters | `worldgen_params/valley.json` | Table | World generation | No | Yes | M0 |
| Micro-climate, succession | `microclimate/<class>.json`, `succession/<stage>.json` | Entries | World and story (env); World generation (world) | No | No | M2–M9 |
| Clock rates | `clock/rates.json` | Table | World and story | Yes | No | M2 |
| Weather | `weather/interior_alaska.json` | Table | World and story | No | No | M2 (normals, hourly curve, daylight), M7 (chain, forecast) |
| Storms, rings, forecast, decade storm | `weather/storm/<id>.json`, `weather/ring/<id>.json`, `weather/forecast.json`, `event/decade_storm.json` | Entries, tables | World and story | Forecast yes | No | M7 |
| Phenomenon, sky sign | `phenomenon/<id>.json`, `sky_sign/<id>.json` | Entries | World and story | Yes | No | M7 |
| Event | `event/<id>.json` | Entries | World and story | No | No | M7 |
| Ambition, prompt, note | `ambition/`, `prompt/`, `note/` | Entries | World and story | Prompts and notes | No | M3–M5 |
| Field-note card (video 2, A8) | `field_note/<id>.json` | Entries | Look, sound and UI | Yes | No | M3 |
| Thread, legend, legend rule | `thread/`, `legend/`, `legend_rule/` | Entries | World and story | No | No | M7–M9 |
| Action | `action/<id>.json` | Entries | Building and crafting | Yes | No | M2 |
| Body, preset, injury, remedy | `body/standard.json`, `preset/`, `injury/`, `remedy/` | Table, entries | Body and survival | Presets yes | No | M4 |
| Body part, injury source, illness | `body_part/<part>.json`, `injury_source/<id>.json`, `illness/<id>.json`, `action/treat_<id>.json` (Body, injury and sickness, RECONCILE 121) | Entries | Body and survival | No | No | M4 |
| Damage type | `damage_type/<id>.json` (25, all `"scaling": "never"`) and `data_maps/damage_type/injury_source.json` | Datapack registry | Body and survival | Vanilla | At world load | M4 |
| Food, fire, ice, season, trap, forage tables | `food/stomach.json`, `fire/danger.json`, `ice/thickness.json`, `season/leaf_stages.json`, `trap/trail_snare.json`, `forage/berry_bushes.json` | Tables | Owning department | No | No | M4–M7 |
| Hit zone, taming | `hitzone/<species>.json`, `taming/<species>.json` (the engine's `butcher/` is retired: step times live in the species `carcass` block) | Entries | Ecosystem | No | No | M6–M9 |
| Call, encounter, encounter director | `call/`, `encounter/`, `encounter_director/default.json` | Entries, table | Ecosystem | Calls yes | No | M6 |
| Water feature, terrain process, water type | `water_feature/`, `terrain_process/`, `water_type/` | Entries | World generation | No | Water types yes | M7–M9 |
| Hatch, migration, rig | `hatch/`, `migration/`, `rig/` | Entries | Ecosystem | Rigs yes | No | M6–M7 |
| Fish run, aquatic event, stocking, keep container, fish disease | `fish_run/`, `aquatic_event/`, `stocking/`, `keep_container/`, `fish_disease/` | Entries | Food and hunting | No | No | M7–M9 |
| Arrival, rare sighting | `arrival/<id>.json`, `rare_sighting/<id>.json` | Entries | Ecosystem | No | No | M7 |
| Scent, senses, index | `scent/<kind>.json`, `senses/scent.json`, `senses/propagation.json`, `index/<id>.json` | Entries, tables | Ecosystem | No | No | M5 (points), M6 (senses) |
| Plant sign, dye | `plant_sign/`, `dye/` | Entries | Body and survival | Yes | No | M7–M8 |
| Livestock | `livestock/<species>.json` | Entries | Building and crafting | No | No | M9 |
| Multiblock, aging | `multiblock/<id>.json`, `aging/materials.json`, `aging/animal_damage.json` | Entries, tables | Building and crafting | No | No | M8 |
| Decay curve | `decay/<curve>.json` | Entries | Food and hunting (Decay and freshness) | No | No | M5 |
| Study, collection, footage, fur market, breakup, pledge, weekly twist, ritual template, valley memory | `study/`, `collection/`, `footage/`, `fur_market/`, `breakup/`, `pledge/`, `weekly_twist/`, `ritual_template/`, `valley_memory/` | Entries or tables | World and story | Collections yes | No | M5–M9 |
| Item family | `item_family/<species>.json` | Entries + bootstrap | Food and hunting | Yes | No | M5 |
| Content flag | `flag/<id>.json` | Entries, bootstrap only | Studio lead | Yes | Per world | M0 |
| Retired ids | `retired/<kind>.json` | Tables | Studio lead | Yes | No | M0 |
| Recipes | `recipe/<type>/<id>.json` (subfolders by type allowed) | Vanilla recipe manager | Building and crafting | Vanilla | No | M3 |
| Loot tables | `loot_table/blocks/`, `butchering/`, `setpiece/`, `cache/`, `gameplay/`, `shed/` | Vanilla loot manager | Owning department | No | No | M3–M6 |
| Tags | `tags/block/`, `tags/item/`, `tags/entity_type/`, `tags/worldgen/biome/` | Vanilla tags | Owning department | Vanilla | No | M2 |
| Data maps | Item: `weight`, `props`, `food`, `wear`, `fuel`, `tool`, `light`, `camo_class`; block: `heat`, `piece`, `decay` | NeoForge data maps | Studio lead (item maps); owning department | Per map | No | M2 |
| Worldgen | `worldgen/biome/`, `worldgen/world_preset/`, `worldgen/structure/`, `worldgen/placed_feature/`, `worldgen/configured_feature/`, `neoforge/biome_modifier/` | Datapack registries | World generation | Vanilla | Yes, at world load | M2 |
| Curios | `curios/slots/`, `curios/entities/player.json` | Curios API | Look, sound and UI | Curios | No | M2 (back, neck), M4 (clothing) |
| GameTest templates | `structure/gametest/<system>/<test>.nbt` | Vanilla structures | Testers | No | No | Every milestone |

**Path rules.** The namespace appears once. The kind folder is singular. Files and fields are snake\_case, and number fields carry a unit suffix (`_kg`, `_m`, `_c`, `_h`, `_days`, `_pct`). Tables and entries never share a folder; `fire/`, for example, holds tables only.

### Schemas and validation at load

**One codec per kind.** Each kind has a schema class in `com.yearofice.data.schema` that builds a `MapCodec` and declares its rules: ranges, units, enums, id references and invariants. The same schema drives the reload listener, the bootstrap reader, the linter and the generated reference page.

**Pass 1, parse.** Every file is decoded with the codec in strict mode.

- Unknown keys are errors, never ignored. A camelCase key is an error with its snake\_case form as the fix (`wholeKg` becomes `whole_kg`).
- A number outside its declared range is an error (`population` minimum above maximum, a probability over 1, a temperature below −70 °C).
- Condition strings (the `expr` language) are parsed here and their facades checked.

**Pass 2, link.** After every kind has parsed, each id reference is resolved: `IdRef<Block>`, `IdRef<Item>`, `IdRef<EntityType>`, `IdRef<SoundEvent>` against code registries, and `KindRef<Species>` and the rest against the new snapshot. A retired id resolves to its replacement from `retired/<kind>.json` with a warning.

**Pass 3, check.** Each schema's invariants run with the whole snapshot in view. Examples:

1. Every species with `uses_trails` has at least one archetype that generates trails in its habitat.
2. Every fish has a length-weight curve that rises across its size range, and a fight profile.
3. Every recipe's station exists and its tier is one of Start, Bushcraft, Hide and bone, Iron, Homestead.
4. Weighed food counts in 100 g units in every loot table and recipe, and a butchering table's total lies within 5% of the species file's yield in kg (RECONCILE 43).
5. Every note id named by a set-piece loot table exists, and every prompt's trigger event exists.
6. Every entry with `requires_flag` names a declared flag.

**Fail loudly, once, with a report.** Errors never stop at the first one; the loader finishes all three passes and writes everything it found. The report goes to `logs/yearofice-data-report.txt` and `.json`, and the first 10 lines go to the log at error level.

```text
Year of Ice data report: 3 errors, 2 warnings (0.7.0-beta.2, 46 kinds, 2,318 files)
ERROR  species/lynx.json  $.population  [40, 25]: minimum is above maximum
ERROR  fish/arctic_char.json  $.lengthWeight  unknown key; use "length_weight"
ERROR  loot_table/butchering/moose.json  pools[2]  meat totals 385 units (38.5 kg); species yield is 385 kg; counts are 100 g units
WARN   note/arvid_cache.json  $.loot  yearofice:fat is retired; resolved to yearofice:animal_fat
WARN   prompt/first_snare.json  $.line  86 characters; the messenger line limit is 80
```

**What fails where.**

| Where | Error | Warning |
| --- | --- | --- |
| Bootstrap at game start | The game stops before the title screen with the report path | Logged |
| First load of a world | The world does not open; the screen names the report | Logged |
| `/reload` | Old data kept; report to operators | Logged |
| CI linter | The build fails | The build fails on `main`; allowed on a dev branch |

### Adding content as data, without code

A department adds most content with files only. Java is needed only for a new behaviour, a new block form or a new body plan.

| Content | Files a department writes | Code needed |
| --- | --- | --- |
| A species on an existing body plan | `species/<id>.json`, model, animations, textures, sounds, lang, hit zones, butchering loot, spawn data | None |
| A species with a unique behaviour (dams, caches, tree shaking) | The same, plus a `class` named in `register` | One Java subclass |
| A fish or a water animal | `fish/<id>.json` or `water_life/<id>.json`, model, texture, sounds | None |
| A plant of an existing form | `plant/<id>.json`, block models and textures for its 8 phases, loot by verb, placement feature | None |
| A recipe, loot table, tag or data-map number | The vanilla or NeoForge file | None |
| A set piece | Template NBT, `setpiece/<id>.json`, container loot, notes | None |
| An event, prompt, note, ambition, phenomenon | The entry file and its lang keys | None, if the trigger exists |
| A new item family (pelts, meats, bones of a new species) | `item_family/<species>.json` naming the parts | None |

**Body plans.** A body plan is one of the entity developer's 35 GeckoLib rigs, and the rig fixes its base class, one of the 10 in Entities and AI › The entity class tree (RECONCILE 92). The `register` block's `body_plan` names the rig id, for example `small_rodent`, `ungulate` or `songbird`. The 13 coarse names of the first draft are retired and map like this: `quadruped_large` to `ungulate` or `ursid`; `quadruped_medium` to `canid`, `felid` or `ungulate`; `mustelid` to `long_mustelid`; `rodent` to `large_rodent` or `small_rodent`; `hare` to `small_rodent` (the shared small-rodent and lagomorph rig); `bat` to `bat`; `bird_ground` to `grouse` or `shorebird`; `bird_perching` to `songbird`, `corvid`, `woodpecker` or `kingfisher_dipper`; `bird_water` to `dabbler`, `diver_duck`, `goose_swan`, `loon_grebe`, `gull_tern` or `crane`; `raptor` to `raptor_soarer`, `raptor_fast` or `owl`; `fish` to the 7 fish rigs; `amphibian` to `frog`; `invertebrate` to `critter`. Each rig's `rig/<rig>.json` names the bones its animations need.

**The register block.**

```json
{
  "register": {
    "entity": "yearofice:singing_vole",
    "body_plan": "rodent",
    "class": null,
    "hitbox_m": [0.2, 0.15],
    "category": "creature",
    "spawn_placement": "yearofice:burrow",
    "model": "yearofice:geo/singing_vole.geo.json",
    "items": ["yearofice:singing_vole"],
    "sounds": ["entity.singing_vole.alarm", "entity.singing_vole.squeak"],
    "carcass_class": null
  },
  "habitat": ["alpine_tundra", "dry_meadow"],
  "population": [40, 120],
  "home_range_m": 20
}
```

The bootstrap reader creates the `EntityType`, the item, the sound events and the default attributes from this block. Datagen then writes the lang stubs, `sounds.json` entries and item models, so the department fills in text and sound files, not boilerplate.

### The content linter

**One tool, two places.** `tools/content-lint` is the same validator as the loaders, run offline over `src/main/resources/data`, `src/generated/resources` and `assets/yearofice`. Gradle runs it as `contentLint`, a dependency of `check`, so every push and every `smoke` runs it. A department can run it alone in about 5 s.

**Checks beyond the three passes.**

1. Every top-level data folder is a declared kind; no `data/yearofice/yearofice/`.
2. Every id, file name, JSON key, block-state property and tag is snake\_case; block-state properties are never camelCase (Minecraft rejects them at startup).
3. Tags are plural nouns (`yearofice:pieces`, not `piece`).
4. Every registered block has a blockstate, a model, a lang key and a loot table (an empty table is allowed when the tab says the block drops nothing).
5. Every mob has a species file, a GeckoLib model and animation file, a hit-zone file, at least one sound event, a lang key and either spawn data or `"spawned": false`.
6. Every item has entries in `data_maps/item/weight.json` and `props.json`; every food item has `food.json`; every garment has `wear.json`; every fuel has `fuel.json`.
7. Every sound event in the registry has a `sounds.json` entry, at least one `.ogg`, and a subtitle key; every `.ogg` is used.
8. Every lang key the code or data names exists in `en_us.json`; messenger lines are at most 80 characters (4 s plus 0.05 s a character, capped at 8 s).
9. Every texture, model and animation file is referenced by something.
10. Weighed food in loot and recipes counts in 100 g units and matches the species yield (pass 3, check 4).
11. No file names a vanilla id from `#yearofice:banned_vanilla` or the hidden vanilla blocks (RECONCILE 34).
12. Every GameTest named in a milestone list exists and carries that milestone's batch.
13. The registry dump matches the master inventory (below).
14. Mod save data and synced data stay under budget: `data_sync` under 4 MB for the whole snapshot.
15. Every class sits in a top-level package of the lead engineer's package map.

**The registry dump.** A GameTest server run writes `build/registries.json` (every `yearofice:` id in every registry, with its class and milestone tag). `tools/registry-inventory` compares it with `studio/inventory.json`, the master list kept in the repository.

| Case | Result |
| --- | --- |
| An id is registered but not in the inventory | Build fails: "unplanned id"; add it to the inventory in the same pull request |
| An inventory id has no registration and its milestone is the current one or earlier | Build fails: "planned id missing" |
| An inventory id has no registration and its milestone is later | Allowed |
| A retired id is registered as anything but an alias | Build fails |
| The same id has two different meanings in two registries | Build fails unless the inventory marks it as one thing in two forms (a fish entity and its item) |

**Keeping the inventory in step with the doc.** `tools/doc-inventory` reads the blueprint's tab exports and lists every `yearofice:` id with the tabs that name it. The Director runs it after each review round; a new id in the doc and not in `studio/inventory.json` becomes a ticket for its department.

### Master registry inventory

**How it was made.** On 19 October 2026 every `yearofice:` id was taken from the 17 tabs of the doc and the four tabs still being drafted (Land animals in depth, Birds and the air, Water life in depth, Plants and fungi), including ids listed after a first one in the same cell ("`yearofice:birds`, `flock_birds`"). Each was given one type from its context, deduplicated, checked for conflicts and renamed by the table after the lists, which gives 1,152 entries. Decay and time had no draft yet and is not counted. Ids written without the namespace in prose are not counted; the linter catches them when they reach code.

| Type | Count | Registered by |
| --- | --- | --- |
| Mobs | 153 | Bootstrap reader from `species/`, `fish/`, `water_life/` |
| Other entities | 21 | `ecosystem` and `travel` entity registers (`DeferredRegister.Entities`) |
| Blocks with a block entity | 71 | `DeferredRegister.Blocks` plus `BlockEntityType`, in each owner package |
| Blocks | 119 | `DeferredRegister.Blocks`, in each owner package |
| Plant blocks | 107 | Bootstrap reader from `plant/` |
| Items | 226 | Each owner package's `DeferredRegister.Items`; species parts from `item_family/` |
| Sound events | 89 | `sound` package; species calls from bootstrap |
| Particle types | 43 | `DeferredRegister` for `ParticleType` |
| Data attachments | 28 | `NeoForgeRegistries.ATTACHMENT_TYPES` |
| Data components | 20 | `item.component.YoiComponents` |
| Payloads | 13 | `net`, through `RegisterPayloadHandlersEvent` |
| GUI layers | 13 | `RegisterGuiLayersEvent` |
| Memories, sensors, activities | 24, 7, 7 | Vanilla `MEMORY_MODULE_TYPE`, `SENSOR_TYPE`, `ACTIVITY` registries |
| Data maps | 7 | `RegisterDataMapTypesEvent` |
| Loot functions and conditions | 8, 3 | `LOOT_FUNCTION_TYPE`, `LOOT_CONDITION_TYPE` |
| Recipe types | 5 | `RECIPE_TYPE` and `RECIPE_SERIALIZER` |
| Mob effects | 3 | `MOB_EFFECT` |
| Attribute modifier ids | 3 | Ids only, no registry |
| Curios slot types | 10 | Curios data |
| Tags | 58 | Data files |
| Biomes | 11 | Datapack registry |
| Worldgen and game hooks | 9 | Chunk generator, biome source, structure and placement types, world preset, world options |
| POI and fishing-spot types | 14 | `POI_TYPES`; spot types as data |
| Multiblocks | 2 | `multiblock/` data |
| GameTests | 78 | Test functions per the pinned version |
| **Total** | **1,152** |  |

All ids below are in the `yearofice:` namespace. Retired and renamed ids are left out of these lists and appear in Conflicting ids. Ids the developer sections of this tab add (for example the lead engineer's `species` and `fuel_l` components, the `props`, `tool`, `light` and `decay` data maps, and the `clock_sync`, `decay_heads`, `body_public` and `hud_line` payloads) enter `studio/inventory.json` from those sections' registry tables, not from this grep.

**Mobs (153).** alaska\_blackfish, alaska\_hare, american\_dipper, american\_golden\_plover, american\_robin, american\_wigeon, arctic\_char, arctic\_fox, arctic\_grayling, arctic\_lamprey, arctic\_tern, bald\_eagle, bank\_swallow, beaver, belted\_kingfisher, bering\_cisco, black\_backed\_woodpecker, black\_bear, black\_billed\_magpie, black\_capped\_chickadee, bog\_lemming, bohemian\_waxwing, boreal\_chickadee, boreal\_owl, boreal\_toad, broad\_whitefish, brown\_lemming, bufflehead, burbot, canada\_goose, caribou, chinook\_salmon, chum\_salmon, cliff\_swallow, coho\_salmon, collared\_lemming, common\_goldeneye, common\_loon, common\_merganser, common\_redpoll, coyote, dall\_sheep, dark\_eyed\_junco, diving\_beetle, dolly\_varden, ermine, fisher, flying\_squirrel, fox\_sparrow, goat, golden\_eagle, gray\_jay, great\_gray\_owl, great\_horned\_owl, green\_winged\_teal, grizzly, ground\_squirrel, gyrfalcon, harlequin\_duck, hawk\_owl, hen, hermit\_thrush, hoary\_marmot, horned\_grebe, horned\_lark, humpback\_whitefish, jumping\_mouse, lake\_chub, lake\_trout, lapland\_longspur, least\_cisco, least\_weasel, lesser\_scaup, lesser\_yellowlegs, little\_brown\_bat, longnose\_sucker, lynx, mallard, marten, masked\_shrew, merlin, mew\_gull, mink, moose, mountain\_goat, mourning\_cloak, muskox, muskrat, ninespine\_stickleback, northern\_flicker, northern\_goshawk, northern\_harrier, northern\_pike, northern\_pintail, northern\_shoveler, northern\_shrike, osprey, pacific\_loon, peregrine\_falcon, pika, pine\_grosbeak, porcupine, pygmy\_whitefish, raven, red\_backed\_vole, red\_fox, red\_necked\_grebe, red\_necked\_phalarope, red\_squirrel, reindeer, river\_otter, rough\_legged\_hawk, round\_whitefish, ruby\_crowned\_kinglet, ruffed\_grouse, rusty\_blackbird, sandhill\_crane, saw\_whet\_owl, sharp\_shinned\_hawk, sharp\_tailed\_grouse, sheefish, short\_eared\_owl, singing\_vole, slimy\_sculpin, snow\_bunting, snow\_fly, snowshoe\_hare, snowy\_owl, sockeye\_salmon, spotted\_sandpiper, spruce\_grouse, swainsons\_thrush, three\_toed\_woodpecker, tree\_swallow, trout\_perch, trumpeter\_swan, tundra\_vole, varied\_thrush, white\_crowned\_sparrow, white\_fronted\_goose, white\_tailed\_ptarmigan, white\_winged\_crossbill, willow\_ptarmigan, wilsons\_snipe, wolf, wolf\_spider, wolverine, wood\_bison, wood\_frog, woodchuck, yellow\_rumped\_warbler, yellowjacket; `yukon_floater` is not a mob but a `mussel_bed` state (S21), so 152 mobs are registered.

**Other entities (21).** arrow, bird\_flock, bobber, carcass\_huge, carcass\_large, carcass\_medium, carcass\_small, carcass\_tiny, drift\_log, falling\_tree, felled\_log, fish\_school, hide\_drag, kayak, loose\_boulder, moose\_skin\_boat, salmon\_carcass, sling\_stone, tadpole\_school, toboggan, travois.

**Blocks with a block entity (71).** antenna\_anchor, banya\_heater, bird\_nest, blackfish\_trap, box\_blind, breakup\_tripod, brooder, charcoal\_pit, cirque\_camera, clamp, counting\_weir, cutting\_table, den\_mound, dip\_net\_platform, dog\_pot, dugout, dye\_vat, feed\_trough, fire\_ring, fish\_pit, fleshing\_beam, floor\_cellar, frame, greenhouse, grindstone, ground\_fire, hay\_rack, haystack, hearth\_stone, herbarium\_cabinet, ice\_hole, ice\_house, kiln\_firebox, lamprey\_basket, lamprey\_fence, lantern, live\_crib, lost\_gear, millstone, name\_board, nest\_box, notch\_board, outhouse, permafrost\_cellar, radio\_desk, rain\_gutter, redd\_gravel, salmon\_leftover, sap\_tap, sawpit, shaving\_horse, signal\_beacon, smithy, smokehouse, smudge\_fish\_rack, station\_clock, steam\_box, stone\_mortar, tip\_up, tower\_platform, trail\_camera, trail\_shelter, trip\_hammer, water\_hole\_box, water\_trough, water\_wheel, well, wolf\_post, wood\_mortar, workbench, yellowjacket\_nest.

**Blocks without a block entity (119).** anchor\_ice, aspen\_log, aufeis, avalanche\_debris, awning, balsam\_poplar\_leaves, balsam\_poplar\_log, bear\_den, beaver\_dam, beaver\_feed\_bed, beetle\_killed\_spruce\_log, bench, birch\_log, black\_spruce\_log, boardwalk, bone\_pile, bough\_bed, bouquet, bubble\_ice, bunk, cache\_leg, cairn, candle\_ice, clinker, coal\_seam, cone\_midden, crib\_wall, darkhouse\_cover, debris\_bed, deep\_ground, deep\_rock, dev\_border, dock\_crib, dressed\_stone, dressed\_stone\_wall, driftwood\_pile, dugout\_air, face\_rock, fatwood\_stump, flame, ford\_stone, foxfire, frazil\_slush, frog\_spawn, frozen\_soil, garden\_bed, ground\_ice, gut\_pile, hearth\_throat, hoarfrost\_layer, holdover\_fire, ice\_block, ice\_lantern, ice\_scarred\_spruce\_log, ice\_sculpture, iron\_outcrop, lining, log\_deck, log\_jam, log\_wall, marker\_post, mud, mud\_clod, muskrat\_push\_up, mussel\_bed, nest\_box\_large, nest\_box\_small, note\_spot, old\_camp\_board, packed\_mud, path, perch\_post, pika\_haypile, pingo\_ice, pond\_leveler, pond\_mud, pot\_crane, pressure\_ice, prop\_post, punk\_log, quinzhee\_snow, retaining\_wall, riffle\_cobble, riprap, river\_ice, rock\_overhang, root\_plate, rotten\_spruce\_log, rotten\_stump, shed\_antler, shoring\_cap, shoring\_post, shove\_ice, shrub\_stem, side\_flue, silt\_layer, skim\_ice, slump, slump\_mud, smoulder, snag\_log, snow\_block, snow\_cover, snow\_den, snow\_fence, sod\_dike, spearing\_hole, spoil, spruce\_log, squirrel\_larder, struck\_spruce, tamarack\_leaves, tamarack\_log, trail\_wand, tree\_stand, wall\_hook, water\_bar, water\_marker, white\_spruce\_log.

**Plant blocks (107).** alpine\_bearberry\_bush, alpine\_bistort, arctic\_lupine, arnica, aspen\_bracket, aspen\_oyster, barclay\_willow, bearberry\_bush, beauverd\_spirea, bebb\_willow, birch\_bolete, birch\_polypore, black\_currant\_bush, bladderwort, bluejoint, bog\_blueberry\_bush, bog\_cranberry\_bush, bog\_rosemary, buckbean, bulrush, bunchberry, bur\_reed, chanterelle, chickweed, common\_juniper, cottongrass, crowberry\_bush, dandelion, deadly\_galerina, destroying\_angel, devils\_club, diamondleaf\_willow, dwarf\_birch, dwarf\_fireweed, false\_chanterelle, false\_hellebore, feather\_moss, feltleaf\_willow, field\_mint, fire\_moss, foxtail\_barley, goldenrod, green\_alder, haircap\_moss, hawks\_wing, hedgehog\_mushroom, horsetail, iceland\_moss, jacobs\_ladder, king\_bolete, lady\_fern, lambsquarters, leatherleaf, lily\_pad, map\_lichen, mares\_tail, marsh\_marigold, monkshood, mountain\_avens, mountain\_heather, mountain\_sorrel, nagoonberry\_bush, northern\_bedstraw, northern\_geranium, orange\_cap\_bolete, orange\_jelly, ostrich\_fern, pasqueflower, pineapple\_weed, plantain, poison\_pax, pond\_weed, puffball, raspberry\_bush, red\_belted\_conk, red\_currant\_bush, red\_osier\_dogwood, rock\_tripe, roseroot, sedge\_mat, shaggy\_mane, shrubby\_cinquefoil, skunk\_currant\_bush, soapberry\_bush, sourdock, sphagnum, stair\_step\_moss, stiff\_clubmoss, sunburst\_lichen, sundew, sweet\_coltsfoot, sweetgale, sweetgrass, tall\_bluebells, tall\_larkspur, tamarack\_jack, thinleaf\_alder, valerian, water\_horsetail, water\_sedge, wild\_calla, wild\_potato, wild\_rhubarb, wild\_strawberry\_bush, wild\_sweet\_pea, wood\_fern, wormwood.

**Multiblocks (2).** banya, quinzhee.

**Items (226).** alaska\_hare\_pelt, alpine\_bearberry, amadou, animal\_fat, ant\_brood, antler, arctic\_fox\_pelt, arnica\_salve, arrowhead, arvid\_hook, bark\_basket, bark\_bucket, bead\_pouch, bearberry, beaver\_pelt, beaver\_tail, berry\_leather, berry\_rake, birch\_bark, bison\_hide, bison\_meat, bison\_robe, bison\_tag, blood, bog\_blueberry, bog\_cranberry, bone\_broth, bowstring, brain, bulrush\_mat, bumblebee\_honey, caddis\_larva, camcorder, canine\_tooth, canoe\_live\_well, carry\_basket, carved\_staff, cast\_iron\_skillet, castor, charcoal\_stick, claw, clay\_shot, coal\_lump, coleman\_lantern, coltsfoot\_salt, counting\_stand, cracklings, crowberry, dandelion\_coffee, decoy\_bark, decoy\_mound, decoy\_wood, diamond\_willow\_stave, dip\_net, dormant\_butterfly, down, down\_vest, dragonfly\_nymph, dried\_meat, duck\_call, dye\_bath, eagle\_primary, enamel\_mug, ermine\_pelt, fairy\_shrimp, fatwood\_stick, feather, fermented\_fireweed, fermented\_sourdock, fermented\_willow\_leaves, field\_book, film\_cartridge, fish\_decoy, fish\_powder, fish\_priest, fish\_record\_book, fisher\_pelt, flash\_board, flat\_stone, float\_coal, freeze\_dried\_strips, freshwater\_pearl, frog\_spawn, frozen\_wood\_frog, game\_feather, game\_guts, giblets, glazed\_fish, goat\_wool, goat\_wool\_insoles, goose\_flute, goose\_primary, grass\_insoles, grouse\_wing, grub\_bait\_paste, hand\_map, hare\_pelt, heart, herbarium\_sheet, hide\_coat, horn, ice\_age\_bone, ice\_cake, ice\_chunk, ice\_saw, incisor, inner\_bark, iron\_water, kidney, knife, lamprey\_larva, lamprey\_oil, lard, leech, leech\_pouch, lemming, lily\_seeds, live\_well\_box, liver, long\_primary, longbow, marrow\_bone, mayfly\_nymph, measuring\_stick, meat\_powder, mesh\_scoop, messenger, moose\_call, moose\_scapula, mushroom\_ink, musk\_gland, muskox\_hide, muskox\_horn, mussel\_meat, mussel\_rake, mussel\_shell, nagoonberry, nettle\_twine, note, nursing\_bottle, ochre, oil\_sac, owl\_feather, owl\_pellet, paper\_nest, pebble, pebble\_pouch, peeled\_stick, pencil, perch\_snare, pestle, pika\_hay, pilot\_sunglasses, pitch, pitch\_candle, pitch\_torch, plant\_clump, plant\_press, plant\_specimen, pocket\_stone, pole\_blind, polypore\_strop, pond\_snail, poplar\_bud\_salve, punk\_wood, qiviut, qiviut\_liner, quill\_band, quills, red\_currant, retted\_fibre, rod, rose\_hip\_meal, rough\_stone, sap\_beer, sap\_pan, sawyer\_grub, scud, seasoned\_stave, shell\_scraper, shell\_spoon, shoe\_grass, skull, sleeping\_jumping\_mouse, sling, snow\_knife, snow\_smock, soapberry, soapberry\_foam, sour\_fish, spile, spoon\_lure, spruce\_flute, squeak\_call, squirrel\_parka, steelyard, stocking\_bucket, stone\_chip, stone\_chisel, stonefly\_nymph, story\_map, stretcher\_board, suet\_feeder, super8\_camera, sweetgrass\_braid, syrup\_preserve, talon, tin\_can, tinder\_conk, tongue, travois, two\_tine\_fork, walking\_stick, warble\_larva, watching\_log, weasel\_box, whole\_game, wild\_egg, wild\_seed, wild\_strawberry, willow\_cutting, woodchuck\_pelt, worm, wormwood\_tea, yukon\_floater.

**Sound events (89).** ambient.mobbing, body.heartbeat\_fast, body.sear, body.stomach\_gurgle, body.stumble\_scuff, bug.whine\_single, build.soil\_trickle, build.stone\_tap, call.crane\_rattle, call.duck, call.grouse\_hen, call.howl, call.loon\_wail, call.moose\_cow, call.moose\_grunt, call.owl\_trill, call.pish, call.squeak\_distress, call.tail\_slap, entity.gray\_jay.alarm, entity.great\_horned\_owl.hoot, entity.grizzly.huff, entity.grizzly.swim, entity.moose.answer, entity.northern\_pintail.whistle, entity.wolf.howl, fire.holdover\_hiss, fishing.reel\_scream, ice.frazil\_hiss, item.sling\_hit, item.sling\_miss, item.sling\_release, item.sling\_whirl, plant.coppice, plant.dig, plant.peel, plant.pick, plant.press\_creak, plant.sap\_drip, plant.strip, plant.uproot, step.dry\_moss, step.leaf\_litter, step.snow\_crunch, step.snow\_soft, step.snow\_squeak, step.snow\_squeak\_loud, step.stick\_ground, step.stick\_ice\_dull, step.stick\_ice\_hollow, step.stick\_ice\_solid, step.stick\_probe\_water, step.stick\_rock, trap.deadfall, trap.lodge, trap.snare\_bird, trap.snare\_hare, trap.tipup\_flag, tree.shove\_creak, tree.snag\_pop, water.beetle\_tick, water.boil, water.dam\_trickle, water.flare, water.frog\_chorus, water.midge\_hum, water.otter\_snort, water.paddle\_in\_pads, water.rise\_plip, water.sucker\_splash, water.tail\_slap, water.toad\_peep, weather.ear\_ring, weather.hair\_prickle, weather.rain\_heavy\_canopy, weather.rain\_heavy\_roof, weather.strike\_crack, weather.thunder\_far, weather.thunder\_near, weather.tree\_split, work.scrape\_resin, world.anchor\_ice\_hiss, world.aurora\_crackle, world.candle\_ice\_chime, world.frost\_quake, world.gap\_wind\_roar, world.ice\_sing, world.rockfall\_trickle, world.whumpf.

**Particle types (43).** black\_fly\_swarm, blow\_fly\_swarm, boil, breath, bubble\_trail, bumblebee, caribou\_bot\_fly, carpenter\_ant, deer\_fly, den\_breath, diamond\_dust, dragonfly, flank\_flash, glint, halo, hatch\_caddis, hatch\_mayfly\_grey, hatch\_mayfly\_pale, hatch\_midge, hatch\_stonefly, horse\_fly, leaf\_fall, leaf\_litter, light\_pillar, midge\_swarm, mist, moth\_swarm, nose\_bot\_fly, plant\_fluff, rise\_ring, river\_foam, rockfall\_dust, snow\_bomb, snow\_flea, soil\_dust, spider\_eyeshine, spore\_puff, sun\_dog, tool\_glint, v\_wake, warble\_fly, water\_strider, yellowjacket\_swarm.

**Data attachments (28).** body, bond, carcass, carry, current, erosion\_counters, flood\_flags, hazard, ice\_lane, jobs, journal, known, legend, livestock, map, microclimate, pack\_rules, patches, placed\_mask, practice, season\_stamp, sign, stand, study, tool\_memory, trust, wariness, work.

**Data components (20).** battery, call\_practice, dye, edge, egg, feather\_kind, fish\_data, fly\_pattern, fresh, heat\_c, item\_temp, keepsake, moisture, note\_id, pelt\_grade, quality, shot\_day, specimen, weight\_g, wetness.

**Payloads (13).** body\_sync, call\_performed, current\_chunk, data\_sync, distant\_sound, env\_sync, fire\_roster, fire\_roster\_request, map\_tile, morning\_report, partner\_status, talk, work\_start.

**GUI layers (13).** body\_figure, body\_fx, breath\_ring, carry\_slot, frost, load\_meter, messenger\_line, minigame, pickup\_line, prompt\_line, threat\_flash, vitals\_strip, work\_ring; `headwear` and `captions` come from Player systems and client, so 15 are registered.

**Memory types (24).** call\_shy\_until, camp\_aversion\_until, den\_pos, encounter\_role, fire\_avoid, follow\_other, food\_conditioned, food\_target, hazed\_count, head\_under, heard\_call, herd\_id, herd\_leader, hooked\_by, last\_drank, look\_target, mother, offered\_food, pack\_role, sodium, threat, water\_target, yard\_pos, young.

**Sensor types (7).** call\_sensor, hearing, scent, sight, staged\_encounter, water\_draw\_sensor, water\_visit.

**Activities (7).** defend\_young, herd\_flow, hibernate, nurse, respond\_to\_call, ring\_defense, yard.

**Data maps (7).** camo\_class, food, fuel, heat, piece, wear, weight.

**Loot functions (8).** carcass\_condition, copy\_freshness, hide\_quality, pelt\_grade, season\_fat, set\_condition, set\_fuel, set\_quality.

**Loot conditions (3).** dressing\_step, flag, world\_unique.

**Recipe types (5).** combine, pounding, rework, salvage, station.

**Mob effects (3).** fever, leeches, nausea, all three retired: body states are fields and statuses of `yearofice:body`, never mob effects (RECONCILE 19, 120; Body, injury and sickness), so none is registered.

**Attribute modifier ids (3).** adrenaline\_crash, gait, load.

**Curios slot types (10).** back, belt, feet\_inner, hands\_inner, hands\_outer, head\_inner, inner\_pocket, legs\_inner, neck, torso\_inner; `sheath`, `quiver` and `snowshoes` come from Player systems and client, so 13 are registered.

**Tags (58).** alarm\_callers, animals, banned\_vanilla, banned\_vanilla\_items, berry\_bushes, birds, bright\_outer, brimmed, browse\_plants, camp\_thieves, carcasses, cavity\_nesters, cavity\_trees, climbable\_trunks, coppice\_plants, drab\_outer, drag\_obstacles, dressing\_floor, dyestuffs, earth\_faces, egg\_layers, erodible\_bank, flock\_birds, freezing\_soils, funnel\_materials, goat\_toxic, large\_predators, log\_walls, markers, mobbers, mobbing\_targets, mordants, mud\_sources, no\_carry, no\_taste\_test, perch\_blocks, pieces, rakeable\_berries, raptors, roof\_any, room\_passable, room\_roofs, room\_walls, rough\_ground, skin\_toxic, slows\_boats, smoke\_woods, snags, standing\_snags, stone\_floor, sweepers, terrain\_steps, transplantable\_plants, vehicles, waterbirds, weighed\_food, wild, wolf\_food.

**Biomes (11).** beaver\_pond, big\_lake, bog\_pool, clearwater\_creek, cloudberry\_muskeg, glacial\_river, hot\_spring\_outflow, oxbow\_lake, slough\_country, spring\_lead, tundra\_pond.

**Worldgen and game hooks (9).** burrow, flag, natural\_feature, other\_dimensions, setpiece, shell\_only, trunk, valley, year\_of\_ice.

**POI and spot types (14).** burbot\_spawning\_ground, poi\_beaver\_pond, poi\_bug\_refuge, poi\_creek\_mouth, poi\_falls\_pool, poi\_ford, poi\_lick, poi\_molting\_lake, poi\_open\_water, poi\_riffle, poi\_still\_surface, poi\_wallow, poi\_weed\_pond, sedge\_flat.

**GameTests (78).** Listed in the tabs that define them; prefixes are normalised by row 22 of Conflicting ids.

**Added in the review round (RECONCILE 103, 121, 127).** These enter `studio/inventory.json` with their milestones; they are outside the 1,152 counted above.

- **Trees and firewood (Crafting and building in depth › Trees, felling and firewood), M2 and M3:** blocks `stump` (states `age` 0–3 and `gnawed`), `fallen_log`, `chopping_block`; the `felled_log` entity gains the state `hung`; items `<wood>_round` and `<wood>_split` for the 8 woods (`spruce`, `birch`, `aspen`, `poplar`, `tamarack`, `willow`, `alder`, `drift`), 16 ids, each carrying `yearofice:moisture`, and `spruce_bark`; actions `action/fell`, `limb`, `buck`, `split`, `peel`; sounds `tree.chop`, `tree.creak`, `tree.fall`, `tree.split`, `tree.hang_creak`; classes `build.wood.FellingService`, `world.logs.FallingTreeEntity`, `world.logs.FelledLogEntity`.
- **Body, injury and sickness, M4:** data kinds `body_part/` (10), `injury/` (13), `injury_source/` (29), `illness/` (20), `action/treat_*` (17); 25 damage types and the `data_maps/damage_type/injury_source.json` map; tags `#yearofice:body_routed`, `systemic`, `animal_strike`, `burns`; attribute modifier ids `injury`, `injury_jump`, `injury_break`, `injury_step`; body record schema 2 fields `part_hp`, `d_sys`, `part_frost` (replacing `zone_frost`), `lost_digits`, `crooked`, `injury_seq`; `BodyService.partHealth`, `factor`, `canUse`, `limp`; synced through `body_sync` and `body_public`.
- **Crops and gardens (reviewer B, RECONCILE 130), M5:** blocks `potato_crop`, `barley_crop`, `wheat_crop`, `turnip_crop`, `cabbage_crop`, `seed_tray`, `feral_rhubarb`; items `seed_potato`, `barley_seed`, `wheat_seed`, `turnip_seed`, `cabbage_seed`, `potato`, `barley`, `wheat`, `turnip`, `cabbage`, `grain_sheaf`, `straw`, `flail`, and the tools `hoe`, `digging_stick`, `shovel` and `hand_quern`; component `yearofice:seed_lot`; tags `#yearofice:crops` and `#yearofice:tillable_soils`; `/yoi` command classes `forage.ForageCommands`, `food.garden.GardenCommands` and `env.LandCommands`.
- **Food and dishes (RECONCILE 137), M5:** item `dish` with the `dish_id` and `nutrients` components; content flag `droppings`; food items `pemmican`, `dried_fish`, `hen_egg`, `goat_milk`, `rose_hips`, `spruce_tips`, `barley_flour`, `sauerkraut`, `birch_sap`, `birch_syrup`. The list's `page` is retired: logbook pages are the one item `note` with `note_id`.
- **Final pass (RECONCILE 146), M8:** block `slab_floor` (state `wood`, the 8 woods; Crafting and building in depth › Building pieces); the existing `path` gains the state `surface` (`gravel`, `plank`); GameTests `build/slab_floor_u` and `build/path_speed`.
- **Deer (RECONCILE 152, 154, 155), M6, behind the content flag `yearofice:deer`:** mobs `mule_deer` and `sitka_black_tailed_deer` on the new `cervid` rig (the 35th; 163 entity types in all); items `mule_deer_meat`, `mule_deer_hide`, `sitka_black_tailed_deer_meat`, `sitka_black_tailed_deer_hide`, with heart, liver, kidney, tongue, `antler` and `brain` taking the species component; sound events `entity.mule_deer.snort`, `.stamp`, `.grunt`, `.fawn_bleat`, `.antler_clash`, `.stott` and `entity.sitka_black_tailed_deer.whistle_snort`, `.stamp`, `.grunt`, `.fawn_bleat`, `.antler_clash`; activity `look_back`; `shed_antler` gains the species states `mule_deer` and `sitka_black_tailed_deer`; data `species/`, `loot_table/butchering/`, `hitzone/` and `item_family/` for both ids, `rare_sighting/mule_deer_straggler.json` and `flag/deer.json`; GameTests `DeerGameTests` (2) (Land animals in depth › Deer).
- **Video 2 specs and the racks (RECONCILE 170, the Forge Labs racks), M2–M9:** tag `fell_stops` (A1, M2); activities `raid_shelter` and `rack_raid`, memory types `open_door_target` and `rack_target`, block `theft_cache` (A2, A3, M6); block `drying_rack` and `smoke_rack` (Food, M5); items `lick_crust` and `lick_salt`, tag `salt` (A4, M5), with the surface block `salt_crust` moved to M2 with the licks; component `trophy_size`, block `trophy_mount`, item `trophy_plaque` (A5, A6, M6); tag `liftable_containers` and sounds `carry.box_lift`, `carry.box_set` (A7, M3); GUI layer `field_note_card` and sound `ui.field_note_card` (A8, M3); items `black_sand` and `birch_pan`, block `placer_gravel`, sounds `work.crust_scrape` and `work.pan_swirl` (A4, A10, M5); block `sluice_box` and sound `work.sluice_wash` (A10, M9); item `large_pack_frame` (A11, M4). A9 adds no ids.
- **Encounter clips (RECONCILE 171), M4 and M6:** sounds `body.breath_panic`, `body.hit_grunt`, `body.bite_crunch`, `item.pack_tear` (M4, the player) and `entity.grizzly.jaw_pop`, `entity.black_bear.huff`, `entity.black_bear.jaw_pop` (M6, the bears); the clips themselves are animation files, not registry ids (Entities and AI › Encounter clips).

### Conflicting ids

Each row is decided. "Alias" means the old id stays mapped to the new one forever through `DeferredRegister#addAlias`, so nothing saved under it is lost; before the first beta (0.3.0) nothing is saved, so most rows are plain renames.

| # | Conflict | Tabs | Decision |
| --- | --- | --- | --- |
| 1 | `canada_jay` and `gray_jay`, one bird | Creative, Bestiary | `gray_jay` (RECONCILE 1); `canada_jay` is a lang alias only |
| 2 | `berry_bush` as one block with a `species` property, and one block per species | Video, Viewers, Plants | One block per species named `<species>_bush`; the fruit item is `<species>`; no `berry_bush` id (RECONCILE 15). Renames: `nagoonberry`, `crowberry`, `bog_blueberry`, `bog_cranberry`, `bearberry`, `alpine_bearberry`, `red_currant`, `wild_strawberry` blocks gain `_bush`; their fruit items keep the bare name |
| 3 | `carcass` as a sixth entity | Video, Bestiary | Not an entity: `#yearofice:carcasses` is the entity tag over `carcass_tiny` to `carcass_huge` and `salmon_carcass`; `yearofice:carcass` is the entity attachment holding the carcass record (RECONCILE 41) |
| 4 | `core_temp`, `grime`, `nausea_level` as separate attachments | Viewers | `core_temp` is a field of the carcass record; `grime` and `nausea` are fields of `yearofice:body` (RECONCILE 19, 41, as the lead settles them) |
| 5 | Landed fish as `fish_<species>` or as the species id | Engine, Waters, Water life | The species id, as vanilla does for `minecraft:salmon`: entity `yearofice:arctic_char` and item `yearofice:arctic_char` are one thing in two forms. `fish_<species>` is withdrawn |
| 6 | `fresh` and `freshness` components | Engine, Items, Bestiary | `yearofice:fresh` as Decay and freshness defines it (state, clock, due, life); `freshness` is withdrawn (RECONCILE 6) |
| 7 | `condition` as a component (0–1000 plus edge) and as vanilla durability | Items, Engine | Vanilla `minecraft:damage` and `max_damage` carry condition, so the bar is vanilla's; the edge is `yearofice:edge` (0–120) (RECONCILE 6) |
| 8 | Fish weight in `fish_data.weight_kg` and stack weight in `weight_g` | Water life, Engine | Weight lives only in `yearofice:weight_g`, which the load sum reads; `fish_data` keeps species, length, sex, stage, roe, vitality, day and spot |
| 9 | `fat` and `animal_fat`, one raw fat | Bestiary, Video | `animal_fat` (raw, 700 kcal per 0.1 kg), `lard` (900 kcal per 0.1 kg), `cracklings`; `fat` aliases to `animal_fat` |
| 10 | `moose_antler_shed` item and `shed_antler` block | Land animals, Bestiary | Block `shed_antler` (with `species` state) lies in the world and drops item `antler` with a species component; `moose_antler_shed` is withdrawn |
| 11 | `yellowjacket` as an entity and as a particle type | Land animals, Birds | Entity `yellowjacket` (nest guards that sting); the swarm particle is `yellowjacket_swarm` |
| 12 | `bumblebee` and `dragonfly` as entities and as swarm particles | Bestiary, Birds | Particle types; only `mourning_cloak`, `snow_fly`, `wolf_spider` and `yellowjacket` are insect entities, to keep the entity cap for animals |
| 13 | `mourning_cloak` defined twice | Birds, Land animals | One entity; Birds and the air owns flying insects, Land animals refers to it |
| 14 | `mayfly_hatch` and `hatch_mayfly_grey`, `hatch_mayfly_pale` | Birds, Water life | Water life's `hatch_<insect>` particles; `mayfly_hatch` is withdrawn |
| 15 | `sun_dog` particle and `sun_dogs` data id; `sky_sign/`, `land/sky_signs.json`, `land/phenomena/` and `phenomenon/` for one idea | Creative, Land | Particle `sun_dog`; phenomena in `phenomenon/<id>.json`, sky signs in `sky_sign/<id>.json`, as the lead settles the paths |
| 16 | `leech` as item and effect | Bestiary | Item `leech`; the attached state is the body status `leeches`, not a mob effect (Body, injury and sickness) |
| 17 | `piece` and `marker` as singular tags | Engine, Video | `pieces`, `markers`; also `browse_plants`, `coppice_plants`, `transplantable_plants`, `standing_snags` |
| 18 | `curio/<name>` item ids with a slash | Creative | Flat `curio_<name>`; slashes are for data paths only |
| 19 | Sound ids without a group | Land, Creative | The `world.` group, as the lead settles it: `world.frost_quake`, `world.whumpf`, `world.rockfall_trickle`, `world.candle_ice_chime`, `world.anchor_ice_hiss`, `world.ice_sing`, `world.aurora_crackle`, `world.gap_wind_roar` |
| 20 | Mob sounds under `animal.*`, `moose.*`, `owl.*` and under `entity.<species>.<call>` | Engine, Bestiary, Land animals | `entity.<species>.<call>` for every mob (vanilla's form): `animal.bear_huff` becomes `entity.grizzly.huff`, `moose.answer` becomes `entity.moose.answer`, `owl.hoot` becomes `entity.great_horned_owl.hoot`. Non-mob groups stay (`step.`, `trap.`, `weather.`) |
| 21 | Butchering loot at `loot_table/entities/muskox` and `loot_table/butchering/<id>` | Land animals, Bestiary | `loot_table/butchering/<species>.json` with context `yearofice:butchering`; death loot tables stay empty (RECONCILE 41) |
| 22 | GameTest prefixes `fishing/`, `animal/`, `water_life/`, `birds/` | Several | The prefix is a top-level package of the lead's map or `layers/`: `fishing/` becomes `hunt/`; `fish/`, `water_life/`, `animal/` and `birds/` become `ecosystem/` |
| 23 | Species as a datapack registry under `data/yearofice/yearofice/species/` | Bestiary | `data/yearofice/species/` by reload listener and bootstrap (Two ways data loads) |
| 24 | Vanilla `lily_pad`, `mud`, `packed_mud`, `mud_bricks` beside ours | Engine, Land, Video | Ours stay; vanilla's are hidden from creative tabs, their recipes filtered, and their item ids added to `#yearofice:banned_vanilla_items` (RECONCILE 34) |
| 25 | Block-state and JSON fields in camelCase (`regrowDay`, `falseAlarm`, `coreTemp` and 171 more) | 11 tabs | snake\_case with unit suffixes; the linter names each with its fix (RECONCILE 18) |
| 26 | `ice_block` (Land) and the ice cake (Structures) | Land, Structures | Item `ice_cake` (45 kg) and block `ice_block` (20 cakes, 900 kg) (RECONCILE 3 units) |
| 27 | `decay/` for decay curves and for the valley memory | Lead, Creative | Curves keep `decay/<curve>.json` (Decay and freshness); Creative's valley memory moves to `valley_memory/<id>.json` |

**Same id, two registries, one thing.** These are allowed and marked in the inventory: fish and water-animal species (entity and item), `frog_spawn` (block and item), `travois` (entity and item), `pelt_grade` (component and the loot function that sets it), `station` (recipe type and the station block tag).

### Names settled

**Data paths (RECONCILE 6, 18, 21, 22).** Every path below moves; the linter rejects the old form.

| Old path | Tab | New path |
| --- | --- | --- |
| `data/yearofice/yearofice/species/` | Bestiary | `data/yearofice/species/` |
| `data/yearofice/yearofice/multiblock/`, `build/pieces/` | Structures, Video | `data/yearofice/multiblock/<id>.json`; per-block numbers in `data_maps/block/piece.json`; built-in blueprints in `structure/blueprint/<id>.nbt` |
| `data/yearofice/yearofice/aging/` | Structures | `data/yearofice/aging/` |
| `data/yearofice/yearofice/arrivals/`, `yearofice/rare_sightings/` | Bestiary | `arrival/`, `rare_sighting/` |
| `data/yearofice/yearofice/plants/`, `plants/` | Viewers, Video, Land | `plant/` |
| `fish_species/` | Waters, Water life | `fish/` |
| `item_props.json` | Items | `data_maps/item/props.json` |
| `presets/expedition.json` | Video | `preset/expedition.json` |
| `heat/faces.json` | Video | `data_maps/block/heat.json`, with the `earth` face as the entry for `#yearofice:earth_faces` |
| `land/phenomena/`, `land/sky_signs.json`, `land/microclimate.json`, `land/succession.json` | Land | `phenomenon/`, `sky_sign/<id>.json`, `microclimate/<class>.json`, `succession/<stage>.json` |
| `decay/*.json` (valley memory) | Creative | `valley_memory/<id>.json` |
| `legend_fish/` | Waters | `legend/<id>.json` with `"kind": "fish"` |
| `loot_table/entities/<id>` | Land animals | `loot_table/butchering/<id>.json` |
| `butcher/` | Engine | Retired; step times in the species `carcass` block |

**JSON keys.** All keys are snake\_case with a unit suffix. The tabs hold 174 camelCase keys in their JSON examples (for example `wholeKg`, `tauHours`, `daysAt5C`, `lengthWeight`, `kcalPerKg`). The mechanical rule is: split at capitals, lower-case, and turn a trailing unit word into its suffix (`Kg` to `_kg`, `Hours` to `_h`, `Days` to `_days`, `C` to `_c`, `M` to `_m`, `Pct` to `_pct`). The linter applies it as its suggested fix, so `daysAt5C` becomes `days_at_5c`.

**Components and food units (RECONCILE 6, 43).** As the lead's Item model layer sets: condition is vanilla durability, freshness is `yearofice:fresh`, weight is `yearofice:weight_g`, and food sold by weight counts in 100 g units in every loot table, butchering pool and recipe. The Bestiary's `"set_count": 180` for bison meat is therefore 1,800. The linter catches the old form with pass 3, check 4, which compares a table's total with the species yield:

```json
{ "type": "item", "name": "yearofice:bison_meat",
  "functions": [ { "function": "set_count", "count": 1800 }, { "function": "yearofice:copy_freshness" } ] }
```

**Attachments (RECONCILE 19, 41).** As the lead settles them: player state is one `yearofice:body` record with `grime` and `nausea` fields, and carcass state is one `yearofice:carcass` record with its `core_temp` field. Wolf trust and raised-animal bond stay separate attachments, because other entities carry them.

**Packages (RECONCILE 16, 32).** The lead engineer's Package and module map is final, including its table of tab packages that move. The build enforces it three ways: the ArchUnit test `layers/no_cross_imports`, `studio/ownership.json` (from which CODEOWNERS is generated), and linter check 15 (every class sits in a listed top-level package).

**Vanilla blocks we replace (RECONCILE 34).** A built-in data pack `yearofice:vanilla_filter`, always on and listed first, holds the `filter` block from the engine tab plus `minecraft:recipe/mud_bricks*`, `packed_mud`, and any recipe that outputs `minecraft:lily_pad`, `mud`, `packed_mud` or `mud_bricks`. `BuildCreativeModeTabContentsEvent` removes those four items from the vanilla tabs; `#yearofice:banned_vanilla_items` lists them so a loot table that drops one fails the linter.

### Repository and Gradle layout

The engine tab's layout stands. The build adds these folders.

```text
year-of-ice-mod/
  gradle/libs.versions.toml            (every version, by the pin rule)
  gradle.properties                    (mod_version, minecraft_version, neo_version, channel)
  pack/                                (packwiz pack: pack.toml, index.toml, mods/*.pw.toml, config/, defaultconfigs/)
  pack/server/                         (server pack extras: server.properties template, start scripts, user_jvm_args.txt)
  studio/ownership.json                (departments to packages and data folders; CODEOWNERS is generated from it)
  studio/inventory.json                (the master registry inventory)
  studio/tickets/                      (round tickets, M5-r4.json)
  src/test/resources/saves/            (fixture saves per release, Git LFS)
  perf/history.csv                     (one row per main-branch build)
  tools/content-lint, tools/registry-inventory, tools/doc-inventory, tools/shots, tools/perf
  .github/workflows/ci.yml, nightly.yml, release.yml
```

**The version pin decides Java.** The pin is 1.21.1 (RECONCILE 118): Java 21 and Gradle 8.x with ModDevGradle 2.x. The toolchain is set in Gradle, so a machine with the wrong JDK downloads the right one.

```text
# gradle.properties (the 1.21.1 pin; the M0 round writes the exact build numbers)
mod_id=yearofice
mod_version=0.0.1
mod_group_id=com.yearofice
minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
neo_version=21.1.264
neo_version_range=[21.1.264,)
geckolib_version=4.9.3
curios_version=9.5.1+1.21.1
java_version=21
channel=dev
org.gradle.jvmargs=-Xmx4G
org.gradle.caching=true
```

**`build.gradle` in outline.**

1. Plugins: `java-library`, `net.neoforged.moddev` (ModDevGradle 2.x). No publishing plugins: the mod is for personal use (RECONCILE 119).
2. Repositories: Maven Central, the NeoForged maven, GeckoLib's maven (`dl.cloudsmith.io/public/geckolib3/geckolib/maven/`), Curios' maven (`maven.theillusivec4.top`).
3. Dependencies: `implementation "software.bernie.geckolib:geckolib-neoforge-${minecraft_version}:${geckolib_version}"`; `compileOnly` Curios' API jar and `runtimeOnly` the full Curios jar; spark as `runtimeOnly` in the dev and CI runs only; JUnit 5 and ArchUnit for `test`.
4. `neoForge { version = neo_version; parchment { ... } }` with Parchment for 1.21.1; `mods { yearofice { sourceSet sourceSets.main } }`.
5. Generated resources: `sourceSets.main.resources.srcDir 'src/generated/resources'`, and a `checkDatagen` task that reruns datagen and fails on a diff.
6. `jar` manifest: `Implementation-Version`, `YearOfIce-Channel`, `YearOfIce-Save-Version` and the Git commit.

**`neoforge.mods.toml` dependencies.** NeoForge `required` at `neo_version_range`; Minecraft at `minecraft_version_range`; `geckolib` and `curios` `required`, ordered `AFTER`; Sodium, Iris and Distant Horizons `optional`; the hard-incompatible list from Other mods as `incompatible` with a reason string the launcher shows.

### Run configurations

| Run | ModDevGradle type | Extra settings | Used by |
| --- | --- | --- | --- |
| `client` | `client` | Dev world seed 3141592 offered on the create screen | Everyone |
| `server` | `server` | `--nogui`, `level-type=yearofice:year_of_ice` | Server checks |
| `gameTestServer` | `gameTestServer` | `neoforge.enabledGameTestNamespaces=yearofice`; `yearofice.batches` to pick milestones | CI, departments |
| `data` (1.21.1) or `clientData` and `serverData` (1.21.4 and later) | Data | `--all --output src/generated/resources --existing src/main/resources` | Datagen |
| `smokeClient` | `client` | `-Dyearofice.smoke=true`, `-Dyearofice.shots=tools/shots/script.json`, fixed GUI scales | CI under a virtual display |
| `perfServer` | `server` | spark on, the bot harness, `-Dyearofice.perf=true` | CI performance gate |
| `migrationServer` | `server` | `-Dyearofice.fixture=<save>`; 600 ticks then stop | CI migration test |

### Tests in CI

**The pipeline.** Every push to any branch runs the first eight steps; `main` adds the last two.

1. `build`, `checkDatagen` and the worldgen lint.
2. `contentLint` and the registry inventory diff.
3. `test` (JUnit: sim, worldgen, schemas, codecs, migrations as pure functions; the ArchUnit package rule).
4. `runGameTestServer` for the current milestone and every earlier one.
5. Dedicated-server boot, 600 ticks, seed 3141592.
6. `smokeClient` under Xvfb: boot, world creation, 600 ticks of scripted play, the screenshot script.
7. Migration test on every fixture save.
8. Performance gate on `perfServer`.
9. Package: the mod jar, the packwiz pack and the server pack.
10. Keep the build as the dev channel's CI artifact for Ruhan's dev install, and append to `perf/history.csv`.

**GameTests.** Required tests must pass; there are no optional ones in a milestone batch. A test that fails 2 of 20 reruns is flaky and goes to a `quarantine` batch only with the studio lead's approval and a ticket, for at most 7 days. Tests wait on game minutes from Clock and calendar, never on tick counts, as the engine tab requires. A full `runGameTestServer` must finish in under 15 real minutes on the CI machine; a slower batch is split.

**Nightly.** The worldgen hash test on Linux, Windows and macOS (x64 and ARM), the client smoke on Windows, and the long-run bot harness (one game year at the sleep rate with the three personas).

**Screenshot checks.** `tools/shots/script.json` fixes the seed, the date, the hour, the weather state, the camera position and the GUI scale, and turns off random particle seeds. It takes 12 shots: day, night, a danger moment (a grizzly at 15 m), a blizzard, the messenger map, the pack screen, the vitals strip at Hypothermic, the frost overlay, a cabin interior at night, the aurora, and the messenger at GUI scales 2 and 4.

| Check | Rule |
| --- | --- |
| Against the golden image | Structural similarity 0.92 or more on the HUD crop, 0.80 or more on the full frame |
| Night really dark | Mean luminance of the night shot under 0.08 (0–1), and under 0.12 with gamma at its maximum |
| Nothing missing | No pink-and-black missing-texture pixels; no frame where the HUD crop is blank when a layer should draw |
| Under Sodium and Iris | The day and night shots again with each; similarity 0.85 or more to the plain run |

A failed comparison does not block a merge by itself; it marks the pull request "look needed". The Director looks at the CI gallery page (an HTML artifact with old, new and difference images) under studio rule 9 and either approves the new golden images or sends the work back.

**Performance gates.** The engine tab's budget and the lead's tick budget, measured by spark and the client frame logger, with a regression rule on top.

| Gate | Pass | Measured in |
| --- | --- | --- |
| Server tick | MSPT average under 30 ms, maximum under 50 ms, over 10 real minutes with 40 Active and 200 Nearby animals | `perfServer` with the bot harness; the lead's `layers/budget` GameTest |
| Mod share | Each system within its line of the tick budget | spark sampler, `com.yearofice` frames |
| Client frame | 60 fps at 12 chunks with the horizon, with and without Sodium | `smokeClient` frame log on the CI GPU runner |
| Hitches | None over 50 ms on chunk loads, region pre-warm, a snow pass, a reload | Both logs |
| Memory | Server heap under 3 GB, client under 4 GB | JVM metrics |
| Save data | Mod data under 5 MB after the five-year sim; `data_sync` under 4 MB | Saved world size |
| Reload | `/reload` of all kinds under 2 s on the server thread swap, under 6 s in total | Timer in `data.ReloadTimer` |
| Regression | No gate worse by more than 10% than the median of the last 10 `main` builds | `perf/history.csv` |

### SMOKE PASSED

The studio rule stands: nothing ships without it. `./gradlew smoke` prints "SMOKE PASSED" only when all of these pass. Items 1 to 6 are the engine tab's; 7 to 12 are new.

1. `build` compiles, datagen output matches the committed files, and the worldgen lint finds no banned call.
2. Every JUnit test passes, the ArchUnit package rule included.
3. `runGameTestServer` passes every GameTest of the current and earlier milestones.
4. The dedicated server boots seed 3141592, runs 600 ticks and stops with no error or warning from `yearofice`.
5. The client boots the same seed, runs 600 ticks, and logs no error, no unregistered event, no NaN in the body record and no unhandled payload.
6. No monsters: zero vanilla mob types and zero `#yearofice:banned_vanilla` entities joined during either boot, and `no_monsters_full_night` passed.
7. The data report has zero errors, and zero warnings on `main`.
8. The registry dump matches `studio/inventory.json` for the current milestone.
9. Every fixture save from every released version loads, runs 600 ticks and passes the migration checks.
10. The performance gates pass, including the 10% regression rule.
11. The 12 screenshots exist and none is blank, pink-and-black or missing a HUD layer; golden-image differences are listed for the Director.
12. The packwiz pack installs into a clean instance folder, as on Ruhan's Mac, and boots to the title screen, and the server pack boots to "Done".

On a developer machine `./gradlew smoke -Pquick` skips 10 and 12 and prints "SMOKE PASSED (quick)", which does not count for a merge.

### The evolving modpack

**The game ships as a modpack, not as one jar.** Minecraft is the engine and it keeps moving; the pack is how the studio adds content, moves versions and keeps Ruhan's valleys. The pack is defined in `pack/` with packwiz, used only to install and update it on Ruhan's Mac (`packwiz-installer` in a Prism Launcher instance, from a private URL or a local folder) and to version it; nothing is published (RECONCILE 119).

**Pack composition.**

| Part | Side | Required | Notes |
| --- | --- | --- | --- |
| NeoForge | Both | Yes | The pinned version; the pack's `pack.toml` names it |
| `yearofice` core mod | Both | Yes | All code, data and assets; also carries the two built-in packs below |
| GeckoLib | Both | Yes | Animals, animated items |
| Curios API | Both | Yes | Clothing layers, belt, back, neck, inner pocket |
| Built-in resource pack `yearofice:look` | Client | Yes, always on | The Year of Ice look: sky, lightmap, fonts, GUI frames, vanilla-block overrides. Registered with `AddPackFindersEvent` as a required pack on top, so a player's own resource pack can sit above it but not remove it |
| Built-in data pack `yearofice:vanilla_filter` | Server | Yes, always on | Filters vanilla recipes, advancements and the four replaced blocks |
| Configs | Both | Yes | `config/yearofice-client.toml`, `yearofice-common.toml`, `defaultconfigs/yearofice-server.toml`, plus Curios' and Sodium's tuned files |
| Sodium, Iris | Client | Optional, on by default in the pack | Official NeoForge builds; tested every night |
| Distant Horizons | Client | Optional, off by default | Far terrain for the 2 km landmark rule on strong machines |
| A shader pack for Iris | Client | Optional, off | Only one that passes the screenshot checks; none is chosen yet |

spark stays out of the pack (engine tab: dev and CI only). The pack never ships a mod that adds blocks, items, mobs or recipes.

### Versions and changelogs

**The mod uses semantic versions,** written `MAJOR.MINOR.PATCH+mc<version>`, for example `0.5.2+mc1.21.1`.

| Part | Goes up when | Save promise |
| --- | --- | --- |
| MAJOR | 1.0.0 is the first stable release after M9; after that, only for a Minecraft line move that cannot keep saves (never planned) | Stated in the changelog with the migration path |
| MINOR | A milestone or a content drop ships: new ids, new data kinds, new attachments or SavedData fields, a new generator version | Every older save from 0.3.0 on opens and migrates |
| PATCH | Fixes and number changes that touch no saved schema | Saves move both ways between patches of one minor |

**Before 1.0, the minor is the milestone.** M2 ships as 0.2.0 and M9 as 0.9.0. Nothing before 0.3.0 (M3, the first beta) is promised to load later.

**The pack's MAJOR.MINOR always equals the core mod's.** Only its PATCH moves on its own, for config defaults or optional mods, so "pack 0.5.4" always means "mod 0.5.x". The pack's `pack.toml` version follows `0.5.4`.

**One changelog per release.** CI builds `CHANGELOG.md` from `Changelog:` trailers in commit messages, grouped by department, plus four generated parts. Studio rule 14 then saves it in the Games project.

```text
## 0.5.0+mc1.21.1 (M5, Food and goals), 2027-03-14
Food and hunting: Snares, deadfalls and the trapline map. Gill nets roll at dawn and dusk.
Body and survival: Plant blocks with phase and taken; the staged taste test.
New ids: 212 (see inventory diff). Retired ids: 3, all aliased (fat -> animal_fat, ...).
Save notes: save_version 3 -> 4. yearofice:fresh gains curve (default "meat"). Old worlds upgrade on open.
Content flags: yearofice:plants_wave2 added, off on stable.
Requires: NeoForge 21.1.x, GeckoLib 4.x, Curios 9.x.
```

### Release channels

| Channel | Built from | Where it goes | Promise |
| --- | --- | --- | --- |
| dev | Every merge to `main` | A CI artifact; Ruhan's linked computer can install it for testing | SMOKE PASSED; no save promise before 0.3.0 |
| beta | A milestone candidate tag (`v0.5.0-beta.1`) after the Director's sign-off | A second Prism instance on Ruhan's Mac for the milestone's human test | Saves carry forward to every later beta and stable |
| stable | A beta that Ruhan has played (the milestone's human test) with no save-breaking problem for 7 days | Ruhan's everyday instance | Full save promise; flags at stable defaults |

**Personal use only (RECONCILE 119).** Ruhan decided the mod and pack are for his own machines. There are no Modrinth or CurseForge pages, no public channels, no publishing tasks and no public pack README; the Ideas from viewers row 32 stays a later business question, and going public would need its own round.

**What a release run does.** `release.yml` runs on a version tag: full smoke, then the fixture save for this version is made by a bot (a 2-game-year homestead from the persona script) and committed, then the jar, the packwiz pack and the server pack are stored as that channel's build for Ruhan's instances, and the changelog is saved in the Games project.

### The server pack

**Contents.** The NeoForge installer and a script that runs `--installServer`; the server-side mods only (core, GeckoLib, Curios); `defaultconfigs/yearofice-server.toml`; `server.properties` with the values below; `user_jvm_args.txt` with `-Xms2G -Xmx4G`; `run.sh` and `run.bat`; a short `NOTES.txt` for Ruhan with the backup advice. packwiz marks each mod's side, so client-only mods never enter it.

| `server.properties` key | Value | Why |
| --- | --- | --- |
| `level-type` | `yearofice\:year_of_ice` | Our world preset |
| `difficulty` | `normal` | Locked by `core.rules` (the lead's Vanilla systems we replace) |
| `spawn-protection` | `0` | The player wakes at the outpost bunk inside it |
| `allow-nether` | `false` | Portals off by default |
| `view-distance`, `simulation-distance` | `12`, `10` | The engine tab's numbers |
| `spawn-monsters`, `spawn-animals`, `spawn-npcs` | `false` | Belt and braces; populations spawn our animals |
| `sync-chunk-writes` | `true` | Safer saves |

**Single-player first.** The server pack exists because the mod must boot on a dedicated server; LAN and server play stay unsupported until co-op ships after 1.0, though the time rules are already co-op-safe (RECONCILE 115).

### Saves across versions

**The promise.** From 0.3.0 on, every save opens in every later version of the same MAJOR, with nothing lost. A save never opens in an older version: `yearofice_world.save_version` above what the jar knows stops the load with "This world was saved by a newer Year of Ice (0.7). Update the mod to open it."

**Five tools, one per kind of saved thing.**

| Saved thing | How it upgrades | Where |
| --- | --- | --- |
| Block, item and entity ids | A retired id stays mapped forever by `DeferredRegister#addAlias(old, new)`; it is never re-used | `data.migrate.Aliases`, listed in `retired/<kind>.json` |
| Block-state properties | Values are append-only; a property is never removed while a released block uses it | Linter rule |
| Data components, attachments, SavedData | Versioned codecs: a record starts with no version field (v1); its first change adds `v` and a step from v1 to v2 on the raw `Dynamic`, before the codec decodes | `data.migrate.Versioned`, used by every owner's codec |
| Retired component and attachment types | They stay registered as legacy types; a converter rewrites them on player login, container block-entity load and item-entity load | `data.migrate.LegacyConverter` |
| Terrain | Never migrated: every released generator version stays in the jar (engine tab), and frozen worldgen data stays in `yearofice_world` | `worldgen.vN` |

**Why our own steps, not DataFixerUpper.** Minecraft's fixer upgrades vanilla's parts of a save and keeps running for them. Mod data has no hook into it, and its schemas are heavy to write. A versioned codec step is a pure function from one map to another, unit-tested in JUnit with no Minecraft running.

```json
{ "migration": "yearofice:fresh", "from": 1, "to": 2,
  "add": { "curve": "meat" }, "rename": { "due": "due_dh" } }
```

Simple steps (add a field with a default, rename, scale a number, map enum values) are data like the line above in `data/yearofice/retired/migrations.json`; anything else is a Java step class named in that file.

**Migration runs lazily and is backed up first.** Chunks, block entities and items upgrade as they load, so a five-year world opens as fast as a new one. Before the first load of an older save, the mod copies `level.dat`, `playerdata/` and `<world>/yearofice/` to `<world>/yearofice/backups/pre-<old version>/`, and shows "This world will be upgraded from 0.6 to 0.7. A backup of your progress is kept. Region files are not copied; use Backup in the world menu for a full copy." with Open and Back.

**The migration test.** `src/test/resources/saves/<version>/` holds three fixtures per release: a fresh world after the guided first night, a 2-game-year homestead, and a world mid-SOS. Each is under 40 MB (Git LFS). For every fixture, `migrationServer` loads it with the new jar, runs 600 ticks and checks:

1. Zero codec errors and zero unknown-id warnings; every alias resolves.
2. The body record matches the fixture's: BMI within 0.01, every field present.
3. Every inventory and container holds the same items by id after aliasing, the same counts and the same freshness bands.
4. The rooms count, fire states and trap states match.
5. A hash of 1,024 fixed block columns around the home site is unchanged: no chunk regenerated, no placed block moved.
6. Mod save data is within the 5 MB budget.
7. Loading the migrated world a second time changes nothing (migration is idempotent).

### Content flags

**New features can ship off.** A content flag is a file in `data/yearofice/flag/<id>.json`, read by the bootstrap reader, so its blocks and items are registered on every build. The flag decides whether players meet the content.

```json
{
  "id": "yearofice:birds_wave2",
  "milestone": "m7",
  "default": { "dev": true, "beta": true, "stable": false },
  "affects_worldgen": false,
  "reversible": true,
  "description": "The 45 new birds of Birds and the air"
}
```

**How a flag hides content.** Registries are global, so content is gated where it is met, never at load.

| Content | Gate |
| --- | --- |
| Spawns | The population spawner skips species with an off `requires_flag` |
| Worldgen features | Placement modifier `yearofice:flag` |
| Recipes | The station matcher skips recipes with an off flag |
| Loot entries | Loot condition `yearofice:flag` |
| Data entries (events, prompts, notes, phenomena) | Kind accessors return only entries whose flag is on in this world |
| Creative tabs and recipe viewers | Items of off flags are left out |

**Flags are per world.** At world creation the channel's defaults (or the server config's overrides) are written into `yearofice_world.flags`. An operator can change a reversible flag with `/yoi flag set <id> on|off`. A flag with `affects_worldgen` true is fixed at creation, because land under a cabin must never change; turning such content on in an old world needs a new generator version, by the engine tab's rule.

**Flags retire.** When a feature reaches stable and its default has been on for one minor version, its flag file is deleted and its gates are removed in code. A world that had it off then gets it on, which the changelog states.

### Moving Minecraft or NeoForge

The engine tab's pin rule stands: one pin per milestone, a move is its own round after a milestone ships. The round runs in this order on branch `upgrade/mc-<version>`.

1. Check that NeoForge has a stable build and GeckoLib and Curios have releases for the target; if either is missing, stop.
2. Bump `gradle.properties` and `libs.versions.toml`; switch the Java toolchain if the line needs it.
3. Write the rename list into DEVGUIDE as the compiler finds each break; adapters and the lead's `api` records change, `com.yearofice.sim` must not.
4. Check the things a move can break for us: the 1,968-block dimension height, the GameTest registration form (annotations up to 1.21.4, `test_instance` data from 1.21.5), the data-gen run names, `AddReloadListenerEvent` and payload registration names, item component names, the GUI layer API.
5. Run the full smoke and every fixture save from the old line; vanilla's fixer upgrades vanilla's part, our versioned codecs ours.
6. Ship to beta for one full milestone; stable only after that.
7. The old line's last minor gets patch fixes for one more milestone on `release/<minor>-mc<old>`, then stops. Saves cannot move back to it.

### Other mods

**The mod is a total conversion.** Content mods are unsupported; client cosmetics and performance mods are welcome. The rules below are advice for what Ruhan adds to his own pack (RECONCILE 119).

| Class | Examples | Rule |
| --- | --- | --- |
| Allowed and tested | Sodium, Iris, Distant Horizons, spark, key-binding and mod-menu tools, Iris shader packs that pass the shots | In the nightly matrix; bugs with them are ours |
| Allowed, not tested | Other client-only cosmetics (capes, zoom, screenshot tools) | Bugs closed unless they also happen without the mod |
| Left out | Minimaps and world maps (they undo the messenger and Everything readable), teleport mods, mods that add hostile mobs, ores or mining, recipe-tree tech mods | Not added to Ruhan's pack; if one is present, one log warning names it and the pillar it breaks, with no screen; the game runs |
| Blocked (`incompatible` in `neoforge.mods.toml`) | Mods that replace the player inventory screen, the hunger and health model, or vanilla time and weather control | The launcher refuses with our reason string |
| Patched | Sodium (colour handlers, fog), Iris (fog and frost uniforms, overlay fallback), Distant Horizons (horizon mesh off where it runs) | Only in `client.compat.<modid>`, loaded when `ModList.get().isLoaded(...)`, through the other mod's public API; never a mixin into another mod |

**Foreign mobs.** The engine's catch-all removes vanilla mobs. Mobs from other mods are removed too: `EntityJoinLevelEvent` cancels any `Mob` whose type is not in the `yearofice` namespace, unless it is in `#yearofice:allowed_foreign_mobs` (empty by default, editable in a data pack).

**Held light.** RECONCILE 35 is settled in Architecture and the interlace › Vanilla systems we replace: gameplay light comes from `env.light`, rendering is client-only, and a dynamic-lights mod with a NeoForge API is used when present. For the pack this means three rules: no dynamic-lights mod is bundled, the `held_light` client option (`auto` or `off`) lets a player turn the vanilla-renderer mixin off, and the nightly matrix runs the night shots with Sodium alone, with Sodium and Iris, and with the vanilla renderer.

### Config files and defaults

NeoForge's `ModConfigSpec` gives three files; every key has a default that suits a first-time player, and the in-game config screen edits the client file.

| File | Scope | Keys and defaults |
| --- | --- | --- |
| `config/yearofice-client.toml` | One player | `temp_unit = "c"`, `mass_unit = "kg"`, `captions = true`, `reduce_screen_effects = false`, `hold_to_toggle = false`, `minigames = "manual"` (or `assisted`, `automatic`), `horizon_mesh = true`, `horizon_km = 24`, `surface_layers = "auto"`, `held_light = "auto"`, `mist_particles_max = 120`, `first_launch_unbind_advancements_key = true` |
| `config/yearofice-common.toml` | The install | `preset = "standard"` (`gentle`, `standard` or `brutal`; a key set by hand shows "Custom"); one table per system, `[body]`, `[decay]`, `[ecosystem]`, `[weather]`, `[fire]`, `[fishing]`, `[ui]`, holding every tuning dial (Minecraft engine › Ruhan's project rules); `data_report = true`, `strict_data = false` (true in dev builds), `layer_cache_regions = 6`, `log_level = "info"` |
| `serverconfig/yearofice-server.toml` | One world (defaults from `defaultconfigs/`) | `default_mode = "endless"`, `default_preset = "standard"`, `active_cap = 40`, `nearby_cap = 200`, `snow_pass_chunks_per_tick = 6`, `ice_pass_chunks_per_tick = 4`, `dropped_item_days = 7`, `other_dimensions = false`, `flag_overrides = {}`, `allow_multiplayer = false` |

**World choices are not config.** Mode, realism preset, seed and flags are chosen on the create screen and saved in `yearofice_world`; the server config only gives the defaults offered.

**Game rules.** The full game-rule set and the difficulty lock are the lead engineer's (Vanilla systems we replace, RECONCILE 44). The pack's configs never override them, and the server pack's `server.properties` above agrees with them.

### Regions as data packs, later

The Ideas from viewers tab's row 38 reads "Later: regions as data packs" (RECONCILE 7). The pipeline already allows it. A region pack is a data pack with `region/`, `archetype/`, `setpiece/`, `structure/setpiece/`, `natural_feature/` and `worldgen/biome/` files and a `pack.mcmeta` entry `"yearofice": { "generator": "v1", "kind": "region_pack" }`. It is chosen on the create screen, copied into `yearofice_world` with the rest of the frozen data, and checked by the same three passes before the world is created. It cannot add blocks or animals (only the jar can), so it reuses the valley's species, plants and pieces.

### The studio workflow in code

**Branches and tags.**

| Branch or tag | Holds | Rule |
| --- | --- | --- |
| `main` | Always SMOKE PASSED | Merges by pull request only; no direct pushes |
| `dev/<department>/<topic>` | One round's work for one department | Lives at most one round; rebased on `main` before merge |
| `round/M5-r4` tag | The frozen copy testers run on | Made by the studio lead at step 6 of the round procedure |
| `milestone/m5` | The candidate after feature freeze | Only fixes; it becomes the beta tag |
| `v0.5.0-beta.1`, `v0.5.0` | Releases | Made by `release.yml` |
| `upgrade/mc-<version>` | A version move | Its own round (Moving Minecraft or NeoForge) |

**File ownership is enforced, not trusted (studio rule 4).** `studio/ownership.json` maps every package and data folder to one department, from the engine tab's ownership table and the lead engineer's package map. CI generates `.github/CODEOWNERS` from it, so a pull request touching `ecosystem/` needs an Ecosystem reviewer. `core`, `api`, `net` and `item` need the studio lead; `data` needs the build developer. The build files, `pack/`, `studio/` and `tools/content-lint` belong to the build developer under the studio lead.

**The round ticket is checked against the diff.** A round's ticket lives in `studio/tickets/<round>.json` in the engine tab's JSON form, with Java packages and data folders in `files`. CI fails a pull request whose diff touches a path the ticket does not list, which is how the Director's "a ticket that breaks ownership is split" rule becomes a check.

**Reviews follow the studio rules.**

| Studio rule | Check in code |
| --- | --- |
| 1 One focused goal per round | One ticket per pull request; the title names it |
| 4 Strict file ownership | CODEOWNERS approvals; the ticket-to-diff check; the ArchUnit package rule |
| 5 Targeted edits only | A pull request that rewrites over 80% of a file over 200 lines needs the studio lead's note |
| 6 Saves stay compatible | Migration test on every fixture; new state only in new fields with defaults; ids never renamed after 0.3.0 |
| 7 One source of truth for the ground | The worldgen lint; no terrain code outside `worldgen` and `world` |
| 8 Nothing ships without SMOKE PASSED | Branch protection requires the smoke job |
| 9 Look before shipping | The screenshot gallery; "look needed" blocks the release, not the merge |
| 11 Testers work on a frozen snapshot | Testers run on the round tag only |
| 14 Docs stay current | `release.yml` fails if CHANGELOG, DEVGUIDE or ROADMAP were not touched since the last release |

### Adding a species end to end

The checklist a department follows for one new animal, here the singing vole from Land animals in depth. Every step names its file; nothing outside these needs to change.

1. **Ticket.** The studio lead opens `studio/tickets/M6-rN.json` naming the species, the Ecosystem department and the files below.
2. **Inventory.** Add `yearofice:singing_vole` (entity, item) and its sound events to `studio/inventory.json` with milestone `m6`.
3. **Species file.** `data/yearofice/species/singing_vole.json` with the `register` block (body plan `rodent`, hitbox, spawn placement `yearofice:burrow`, sounds) and the tuning fields from the tab, in snake\_case with units.
4. **Model and animations.** `assets/yearofice/geo/singing_vole.geo.json` and `animations/singing_vole.animation.json` on the `rodent` rig's bone names (`rig/rodent.json`), made in Blockbench with the GeckoLib plugin; texture in `textures/entity/singing_vole.png`.
5. **Sounds.** Ogg files in `assets/yearofice/sounds/entity/singing_vole/`, mono, 44.1 kHz, peak −1 dBFS; datagen writes the `sounds.json` entries and subtitle keys from the register block.
6. **Text.** `en_us.json`: the entity name, the item name, the subtitles, and any messenger lines (80 characters at most).
7. **Hit zones.** `data/yearofice/hitzone/singing_vole.json`, one box set per pose.
8. **Yield.** A small animal becomes its own item: `item_family/singing_vole.json` and its numbers in `data_maps/item/weight.json`, `props.json` and `food.json`. A large one names its carcass class and gets `loot_table/butchering/singing_vole.json`, with counts in 100 g units.
9. **Tags.** Add it to the entity tags the tab names (for example prey of `#yearofice:raptors`, cache thieves).
10. **Spawning.** Its habitat and population in the species file; the archetypes that hold it already list the habitat.
11. **Lint.** `./gradlew contentLint` until the report is clean.
12. **GameTests.** At least two in `gametest/ecosystem/`: one for a number from the tab (a population band or a behaviour time) and `data_only_species_spawns` extended to cover it.
13. **Look.** Add the species to the screenshot script if it has a new silhouette; the Director checks the gallery.
14. **Flag.** If the species belongs to a wave that ships later, name its flag in `requires_flag`.
15. **Smoke and merge.** `./gradlew smoke`, a pull request with the Ecosystem reviewer and, for a new body plan only, the studio lead.

### The interlace

The pipeline feeds every shared layer and checks what each layer reads. This table is the build side of the lead engineer's ten layers, by their names in Architecture and the interlace.

| Shared layer | Data kinds that feed it | What the pipeline checks | Saved state it migrates |
| --- | --- | --- | --- |
| Clock and calendar | `clock/rates.json`, `season/` tables, the day-length setting | Rates match the engine's rate table; tests wait on game minutes | `yearofice_clock` |
| Environment field | `weather/`, `phenomenon/`, `sky_sign/`, `microclimate/`, `water_type/`, the heat and light data maps | Every phenomenon's trigger reads a field the layer exposes | `microclimate`, `current`, `season_stamp`, `yearofice_ice`, `yearofice_water` |
| Scent and sound field | `scent/`, `senses/`, `noise_m` in `action/` | Every scent source has a decay; every noise has a range | `yearofice_scent` |
| Ecology | `species/`, `fish/`, `water_life/`, `plant/`, `index/`, `migration/`, `hatch/`, `stocking/`, `encounter/` | Habitats exist in some archetype; populations and calendars in range | `yearofice_populations`, `yearofice_region_<key>`, `individual`, `patches` |
| Decay and freshness | `decay/<curve>.json`, `aging/`, `spoil_days_at_5c` in the food map, the block `decay` map | Every food has a spoil rate and a curve; every aged piece has a curve | `fresh`, `yearofice_decay`, `decay_marks` |
| Item model | Data maps `weight`, `props`, `food`, `wear`, `fuel`, `tool`, `light`; `item_family/` | Every item has a weight and props; weighed food counts in 100 g units | Every component, by versioned codecs |
| Body | `body/`, `preset/`, `injury/`, `remedy/` | Presets cover every body field; remedies name real injuries | `yearofice:body` |
| Messenger and HUD bus | `prompt/`, `note/`, event lines, lang | Lines exist and fit 80 characters; prompts name real triggers | `known`, `journal`, `map` |
| World edits | `terrain_process/`, `multiblock/`, `setpiece/`, `succession/` | Ops never touch blocks in `placed_mask`; multiblocks name real pieces | `yearofice_terrain`, `placed_mask`, `flood_flags`, `erosion_counters`, `trees`, `stand` |
| Sound events and the audio bus | Sound events, `call/`, `sounds.json` | Every event has files and a subtitle; calls name real sound events | None |

### Vanilla gaps in this area

The lead engineer's Vanilla systems we replace is the master list. These are the rows on the data, build and shipping side that it does not cover, or covers and this section builds.

| Vanilla behaviour | Decision |
| --- | --- |
| Vanilla recipes and advancements (the lead's rows) | Built as the `yearofice:vanilla_filter` data pack, listed first and always on |
| Vanilla loot tables for vanilla blocks that still exist (water, ice, snow, bedrock) | Overridden with empty tables in `data/minecraft/loot_table/blocks/` where a player can reach the block |
| Vanilla tags others read (`minecraft:logs`, `minecraft:leaves`, `minecraft:dirt`) | Our blocks are added, so tools and other mods' checks behave |
| Vanilla creative tabs | Vanilla items hidden except those our worlds use; our tab per department; creative is a test mode |
| World presets list on the create screen | Ours is the default and first; the others stay for testing behind "More world options" |
| The experimental-features warning | Never shown: we add no experimental datapack registries |
| "Optimize World" | Runs vanilla's fixer only; our data upgrades lazily on load, as written in Saves across versions |
| The 5-minute item despawn | Owned by the Item model layer (RECONCILE 36); the pipeline only lints `dropped_item_days` |
| Player resource packs over ours | Allowed above `yearofice:look`; the screenshot checks run without them |
| `/reload` | Reloads our kinds too, atomically; frozen kinds are only re-checked |

### Registry entries this section adds

| Id | Type | Class | Data file | Milestone |
| --- | --- | --- | --- | --- |
| `yearofice:data_kind` | Code registry | `data.KindRegistry` | `data/yearofice/<kind>/` | M0 |
| `yearofice:flag` | Data kind, loot condition, placement modifier | `data.flags.Flags`, `FlagCondition`, `FlagPlacement` | `flag/<id>.json` | M0 |
| `yearofice:butchering` | Loot context param set | `data.loot.ButcheringContext` | `loot_table/butchering/<id>.json` | M5 |
| `yearofice:data_sync`, `data_hash`, `flag_sync` | Payloads | `net.DataSyncPayload`, `DataHashPayload`, `FlagSyncPayload` | None | M0 |
| `yearofice:look`, `yearofice:vanilla_filter` | Built-in resource and data pack | `data.packs.BuiltInPacks` | `resourcepacks/look/`, `datapacks/vanilla_filter/` in the jar | M2 |
| `#yearofice:weighed_food`, `#yearofice:banned_vanilla_items`, `#yearofice:allowed_foreign_mobs` | Tags | Data | `tags/item/`, `tags/entity_type/` | M2–M5 |
| `/yoi data report`, `/yoi flag`, `/yoi dump registries`, `/yoi migrate status` | Commands | `data.DataCommands` | None | M0 |
| `yearofice_world.save_version`, `.flags`, `.version_history` | SavedData fields | `core.WorldMeta` (fields added by `data`) | None | M0 |
| GameTests `yearofice:data/all_kinds_load`, `data/reload_atomic`, `data/sync_hash_matches`, `data/flag_off_hides_species`, `data/data_only_species_spawns`, `data/migrate_fixtures` | GameTests | `gametest.DataTests` | `structure/gametest/data/` | M0–M5 |

**What the data GameTests assert.**

1. `all_kinds_load` (m0): every kind loads with zero errors; the snapshot holds the expected entry count per kind within 0.
2. `reload_atomic` (m0): a reload with one broken file keeps every old entry; a good reload swaps all kinds in one tick.
3. `sync_hash_matches` (m0): a client's hash after `data_sync` equals the server's; a 4 MB snapshot syncs in under 2 s on loopback.
4. `flag_off_hides_species` (m2): with a flag off, a 7-game-day run spawns 0 of its species, and its recipes and loot never appear; switched on, the species appears within its respawn days.
5. `data_only_species_spawns` (m5): the test species defined only in data spawns, walks a trail, takes damage in its hit zones and makes its call.

### Risks and the first prototypes

The build prototypes are numbered B1 to B5 so they do not clash with the lead engineer's P1 to P7.

| Risk | Why it matters | Mitigation | Prototype that retires it |
| --- | --- | --- | --- |
| Data-only species need registration before data packs load | The whole "departments add content as data" promise rests on it | The bootstrap reader reads the jar at mod construction | B2 |
| Library lag on a new Minecraft line | GeckoLib or Curios may trail NeoForge by weeks | The pin rule; the upgrade round stops if a library is missing | B1 on the 1.21.1 pin |
| A save breaks after release | Players lose a valley they built for months | Aliases, versioned codecs, fixture saves, pre-migration backup, refusal to open newer saves | B3 |
| 174 camelCase keys and 14 old paths in the tabs | Each would fail strict parsing | The linter's suggested fixes; one conversion pass when the data is first written | B4 |
| Reloads that half-apply | Animals with new data and blocks with old | Snapshot swap in one assignment; systems hold ids, not entries | `reload_atomic` |
| Pack install fails on Ruhan's Mac | A player cannot start | The packwiz pack and the server pack install in CI from a clean folder | B5 |
| Held light under shaders | Iris or a dynamic-light mod may double the light | The lead's held-light rules; the `held_light` option; three renderers in the nightly matrix | The lead's P4, run inside B1 |
| Content flags multiply test cases | Every flag doubles a test's states | Flags retire one minor after stable; CI tests the dev and stable defaults only | `flag_off_hides_species` |
| The inventory drifts from the doc | Ids appear in tabs with no build entry | `tools/doc-inventory` after every review round; unplanned ids fail CI | Ongoing |

**B1, the empty-world pack (M0).** A `.mrpack` with NeoForge, `yearofice`, GeckoLib and Curios, and nothing else.

1. The mod registers one block (`yearofice:deep_ground`), one GeckoLib test entity, the ten Curios slots, the world preset and the built-in packs.
2. Install it on Ruhan's Mac with `packwiz-installer` into a clean Prism Launcher instance; install the server pack in a clean folder.
3. Pass: the client reaches the title screen and creates a world of our preset; the server prints "Done"; the test entity animates; the Curios slots show in the pack screen; the log has 0 errors from `yearofice`; Sodium and Iris added on top change nothing in the shots. Do it on the 1.21.1 pin.

**B2, a species added without code (M0, end of round).** Add `data/yearofice/species/test_vole.json` with a `register` block on the `rodent` plan, a Blockbench model, two sounds and a lang file, with no Java change.

1. Pass: the game starts; `/yoi spawn test_vole` works; it walks, squeaks with subtitles, shows its name, and the linter passes.
2. Then change its `home_range_m` and `/reload`: the new range applies without a restart. Change its hitbox: the reload refuses with "Restart needed: register block changed".
3. Break the file on purpose (camelCase key, population minimum above maximum, a missing sound file): the game stops before the title screen with all three errors in one report.

**B3, a save across versions (M2).** Build 0.2.0, make a save with a `fresh` stack, a renamed block and a body record; build 0.2.1 with one versioned-codec step, one alias and one retired component. Pass: the migration test's seven checks hold, the backup folder exists, and the old jar refuses the new save with the update message.

**B4, lint the blueprint (M0).** Extract every JSON example block from all tabs into `tools/content-lint/fixtures/` and lint them with schemas built from the tabs. Pass: the report lists the camelCase keys and old paths of Names settled and nothing else; each department fixes its own before writing real data.

**B5, the release dry run (M3).** Tag `v0.3.0-beta.0` on a branch: the release workflow makes the fixtures, the changelog, the packwiz pack and the server pack. Pass: the pack installs on Ruhan's Mac and the server pack boots, and the changelog lists new ids, save notes and flags.
