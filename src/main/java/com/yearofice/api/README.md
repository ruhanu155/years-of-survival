# Shared services map (`com.yearofice.api`)

For agents: use these services instead of reading their impls. Get each with `X.get()`; impls install at mod construction. Server side only unless noted. Studio owns `api`; changing a signature needs a studio PR.

## body (owner: body)
Owns the player's vitals, eating, drinking, injuries, sleep. `BodyService.get()` / `SleepService.get()`.
```java
double warmth(ServerPlayer p); double condition(ServerPlayer p); double strength(ServerPlayer p);
double bmi(ServerPlayer p); double energy(ServerPlayer p); double water(ServerPlayer p);
BodyReadout readout(ServerPlayer p); boolean isSafeToStage(ServerPlayer p);
void applyInjury(ServerPlayer p, InjurySpec injury, String cause);
boolean eat(ServerPlayer p, ItemStack stack, double grams);
void drink(ServerPlayer p, double litres, WaterPurity.Source source, boolean boiled);
WaterPurity.Source waterSourceAt(ServerLevel level, BlockPos pos);
void wake(ServerPlayer p, String cause);
// SleepService
Refusal lieDown(ServerPlayer p, BlockPos head);  Refusal lieDown(ServerPlayer p, BlockPos head, long minute);
Spot spot(ServerPlayer p);  @Nullable String check(ServerPlayer p, long minute);
```
- Call `eat`/`drink`/`applyInjury` for kcal, water and harm; never write `yearofice:body`.
- Every injury (animal, fall, tool, fire) goes through `applyInjury(InjurySpec.of(source))`.
- Data: `ContainerData.MAP` (item `container`), `WaterContent` component, `BedData.MAP` (block `bed`, owned by building).

## clock
Owns game time (`game_ds`), calendar, sun, moon, world rate. `ClockService.get()`; client reads `ClientTime.minuteD()`.
```java
long nowDs(); long nowMinute(); long absDay(); int minuteOfDay(); int dayOfYear(); int year();
Rate rate(); Rate rate(ServerPlayer p); int dsPerTick(); int dsPerTick(Rate rate); int dayLengthMin();
Sun sun(); double sunElevation(); double daylightHours(long absDay);
long dawnMinute(long absDay); long duskMinute(long absDay); double moonPhase();
void requestRate(ServerPlayer p, Rate rate, String reason);
void setDay(long day); void setNow(long gameDs); void advance(long minutes);
void setDayLength(int minutes); void forceRate(Rate rate); Rate forcedRate();
```
- Request rates every tick you need one (`requestRate`); never set dayTime. World runs at the slowest player's rate.
- `setDay/setNow/advance/forceRate/setDayLength` are admin/test only.
- Stamps are absolute minutes; ask `nowMinute()`, not world ticks.

## env
Owns weather, air/feels-like temp, wind, water/ground temp, snow, light. `EnvService.get()`.
```java
EnvSample sample(ServerLevel l, BlockPos pos);
EnvSample sampleAbstract(ServerLevel l, double x, double y, double z, long minute);
double airC(ServerLevel l, BlockPos pos); double feelsLikeC(ServerLevel l, BlockPos pos);
double groundC(ServerLevel l, BlockPos pos, double depthM);
double waterC(ServerLevel l, BlockPos pos, WaterKind kind); double waterCAt(ServerLevel l, WaterKind kind, long minute);
Wind wind(ServerLevel l, BlockPos pos); Precip precip(ServerLevel l, BlockPos pos);
double snowCm(ServerLevel l, BlockPos pos); double visibilityM(ServerLevel l, BlockPos pos); int light(ServerLevel l, BlockPos pos);
WeatherModel.Hour valley(ServerLevel l); WeatherModel.Hour valleyAt(ServerLevel l, long minute);
WeatherModel.Day day(ServerLevel l, long absDay);
List<HourAir> hourlySeries(ServerLevel l, BlockPos pos, long fromDs, long toDs); boolean heavy(ServerLevel l);
void setOverride(ServerLevel l, WeatherOverride o); WeatherOverride override(ServerLevel l);
void fixFirstDay(ServerLevel l, WeatherModel.FixedDay day);
```
- Read weather here; never recompute it. Use `sampleAbstract`/`hourlySeries` for catch-up after a `ClockEvent.Jump`.
- `setOverride`/`fixFirstDay` are debug and story-start only.
- Carried lights: `LightData.MAP` (item `light`) and `LightState` component; `LightState.shining(stack, data)`.

## hud
Owns every word the server tells a player. `HudBus.get()`; `FieldNoteApi`; `MessengerAccess`.
```java
void post(ServerPlayer p, HudLine line);
void offerPrompt(ServerPlayer p, String key, String text, int priority); void clearPrompt(ServerPlayer p, String key);
List<HudLine> journal(ServerPlayer p); List<HudLine> sent(ServerPlayer p); List<HudLine> held(ServerPlayer p);
static HudLine HudLine.of(HudLineType type, String id, String source, HudArg... args);  // .withPriority(int)
HudArg.fixed(double v, String unit) / of(long) / item(id) / species(id) / text(s) / key(langKey)
FieldNoteApi.trigger(ServerPlayer p, String noteId);
MessengerAccess.battery(ServerPlayer p) / live(ServerPlayer p)
```
- Post lines via `HudBus`; lang key pattern `messenger.yearofice.<type>.<id>`; max 6 args; units: kg g c m km cm mm l ml kcal pct min h ms.
- Never send chat or action-bar text directly.
- Add the lang entry with the line.

## item
Owns carried weight and load bands. `LoadService.get()`.
```java
double kg(Player p); double maxKg(Player p); int openRows(Player p);
LoadBands.Band band(ServerPlayer p); double kcalFactor(ServerPlayer p);
```
- Body applies `kcalFactor`; other systems only read it.

## fire
Owns fire state and heat. No service; events plus a heat lookup the body reads.
```java
List<HeatSource> sourcesNear(Level l, double x, double y, double z);  List<FireHeat> fires(Level l);  // FireHeatLookup
```
- Warmth reads fires through `FireHeatLookup`, never the fire package. React to `FireStateEvent`.
- Fire work costs the body by posting `FireEffortEvent`; body charges it.

## decay
Owns food freshness (`yearofice:fresh`). `DecayService.get()`.
```java
double dAt(double gameMinute); double dNow(Level l);
@Nullable Fresh stamp(Level l, ItemStack s, FoodState state); @Nullable Fresh settle(Level l, ItemStack s);
double progress(Level l, ItemStack s); int stage(Level l, ItemStack s);
double daysUntil(Level l, ItemStack s, double target);
void changeState(Level l, ItemStack s, FoodState state); void age(Level l, ItemStack s, double days); double airC(Level l);
```
- Stamp new food with `stamp`; never edit `fresh` yourself. Stage: 0 Fresh, 1 Aging, 2 Turning, 3 Spoiled, 4 Rotten.
- Cooking calls `changeState`, which keeps progress.

## world
Owns read access to fishing spots. `FishSpotService.get()`.
```java
List<Spot> spotsNear(ServerLevel l, BlockPos pos, double radius);
Optional<Spot> spotAt(ServerLevel l, BlockPos pos);   // default method, nearest containing spot
Optional<Spot> sliceSpot(ServerLevel l);
```
- Read only. Worldgen fills spots; ask here instead of scanning water.

## build
Owns bed and building data maps only. `BedData(quality, sleepWarmthC)` in `BedData.MAP` (block `bed`); the body reads it for sleep.
- Add beds as data rows, not code.

## Events (NeoForge bus unless noted)
- `ClockEvent.Minute(first,count)`, `Hour`, `Day`, `Dawn`, `Dusk`, `Jump(fromDs,toDs)`: fired by `story.clock.ClockServiceImpl`. Jump on >60 min or admin set: run closed-form catch-up.
- `RateChangedEvent(from,to)`: fired by the clock when the world rate changes.
- `PromptOfferEvent(player,key,text,priority)`: fired by `HudBusImpl` on `offerPrompt`; cancellable, cancelling hides the prompt.
- `FireStateEvent(level,pos,from,to,heat,line)`: fired by `fire.FireRingBlockEntity`.
- `FireEffortEvent(player,activity,kcal,gameMinutes)`: fired by `fire.BowDrillService`; body listens.

## Where things live (package -> owner)
Under `com.yearofice.`

| package | owner |
|---|---|
| `core` | studio |
| `api` | studio |
| `net` | studio |
| `item` | studio |
| `data` | build |
| `sim` | studio |
| `sim.body` | body |
| `sim.fire` | body |
| `sim.forage` | body |
| `sim.food` | food |
| `sim.decay` | food |
| `sim.fishing` | food |
| `sim.hunt` | food |
| `sim.minigame` | food |
| `sim.env` | story |
| `sim.story` | story |
| `sim.terrain` | worldgen |
| `sim.ecosystem` | ecosystem |
| `sim.build` | building |
| `worldgen` | worldgen |
| `world` | worldgen |
| `env` | story |
| `story` | story |
| `ecosystem` | ecosystem |
| `hunt` | food |
| `food` | food |
| `food.traps` | ecosystem |
| `decay` | food |
| `forage` | body |
| `body` | body |
| `fire` | body |
| `build` | building |
| `travel` | building |
| `hud` | look |
| `sound` | look |
| `client` | look |
| `client.horizon` | worldgen |
| `client.render.entity` | ecosystem |
| `client.minigame.bowdrill` | body |
| `client.minigame.fishing` | food |
| `gametest` | testers |

## hunt (owner: hunt)
`ConcealmentService.get().effectFor(level, player, animalDistM, animalInFront, bigPrey, bear)` returns `sim.hunt.Blinds.Effect` (sightX, noiseX, scentX, scentClearM, viewX) for the blind or tree stand the player is in; ecosystem multiplies the animal's senses by it.
Events on `NeoForge.EVENT_BUS`: `CallSoundedEvent` (a call was sounded; `pRespond(species, listener, season, hour, shy, director)` gives each answer's odds, ecosystem rolls them), `AnimalWoundedEvent` (an arrow wounded but did not kill; wound state is `hunt.wound.WoundTracker`), `EncounterStateEvent` (bear strike/pinned, posted by `hunt.danger.Encounters`).
