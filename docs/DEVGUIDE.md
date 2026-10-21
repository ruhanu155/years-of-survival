# Developer guide

Read CLAUDE.md first, then docs/ready-for-development.md for your milestone. Do not read all of docs/.

## Build and test
- `./gradlew build` runs JUnit. It must pass.
- `./gradlew runGameTestServer` must end with "All N required tests passed". Never accept the Minecraft EULA for Ruhan; automation uses the GameTest server, never `runServer`.
- Local GameTests, your area only: `tools/gametest.sh -Pgametest.filter=<prefix>/` (slot-locked, two slots). Never raw `runGameTestServer` while other lanes run.
- `tools/prepush.sh` before every push: ignored files under src/, `docInventory`, `contentLint`, build.
- Data-only change: `./gradlew contentLint` is enough.
- New id: `tools/newid.sh <type> <id> <owner> <milestone> "<basis>"`. Never hand-edit studio/inventory.json.
- Opt-in: `./gradlew test -Pstartsite.full` (50-seed claim, about 20 min); `-Pstartsite.report --tests '*StartSiteReportTest*'` writes build/start-sites.tsv for seeds 1-50.
- Pinned versions live in gradle.properties; change them only in an upgrade round. From M3 on the save-migration test also runs.
- exit 137 from gradle or a GameTest is an OOM kill, not a failure. Wait for other lanes and retry once.

## Package layout (src/main/java/com/yearofice)
- `sim`: formulas, no Minecraft imports. Body, weather, decay, fishing, hunt sign, build heat, etc.
- `api`: the only way systems talk. Services and events. Read src/main/java/com/yearofice/api/README.md before touching one.
- System packages (adapters): body, build, crop, decay, ecosystem, env, fire, food, hud, hunt, item, plant, story, travel, world, worldgen.
- `core`, `data` (save migration), `net`, `sound`, `gametest`.
- `client`: client-only code. Nothing else imports it.
- Content is data: data/yearofice/<kind>/<id>.json, snake_case keys, unit suffixes (weight_kg).

## Rules
- Server decides, client shows.
- Systems talk only through api services and events. Each piece of shared state has one writing package.
- Ids are yearofice:<snake_case>. After 0.3.0 never rename; retire with an alias.
- Every system ships /yoi debug commands. Every tuning number goes in yearofice-common.toml with Gentle, Standard, Brutal presets. GameTests run on Standard.
- No hostile mobs, no vanilla mob drops (every death makes a carcass), no digging below the 2-block soil layer.
- Names and art original. Found assets need a redistributable licence and an entry in assets/yearofice/credits.json.
- Registered is not obtainable is not usable. Every id needs a source a player can reach without /yoi and a player-facing interaction. Sweep tests (StationInputSourceSweepTest, FeatureSourceSweepTest, ItemModelCoverageTest, AmbitionTriggerSweepTest) fail on drift.
- Ponytail ladder: does it need to exist, does the codebase have it, JDK, Minecraft/NeoForge, a dependency, one line, then minimum code. Fix at the root.

## GameTest isolation
- Shared static overrides (ValleyPool.testPool, windOverride, calmOverride, streamOriginOverride) are saved and restored at test end. Never null them.
- Statistical tests pin the roll stream (ShelterEntry.streamOriginOverride) and calm weather (WeatherSystem.calmOverride). Batch layout moves absolutePos.
- Never write blocks outside your own template. Floors go through EcoGameTests.stone (refcounted).
- Entities get their own test group; neighbours' predators and caribou leak otherwise.
- Far-region tests force-load their own chunks (EcoGameTests.force) before laying floors and release at the end.
- A prefix filter still runs every non-generator test in another batch layout. worldgen_no_spill, start_station_nine, slice_valley are layout-sensitive: confirm on CI.
- chase_cap_150m can fail when the wolf catches prey before 150 m. Make prey uncatchable or assert the cap separately.
- GameTest-forced chunks load neighbours as border chunks.

## Git
- Branch dev/<department>/<topic>, PR into main. main always passes.
- Before the final squash: `git fetch origin`, then `git merge origin/main`, then `git reset --soft origin/main && git commit`. A stale fetch reverts everything merged since (#107). Check `git diff --stat origin/main` for unexpected deletions.
- After a stacked base PR is squash-merged, rebuild with reset --soft or cherry-pick.
- Shared files (YearOfIce.java, YoiConfig, lang/en_us.json, inventory-rules.json, credits): append at the end of the block, never reorder.
- Commits end with the Co-Authored-By and Claude-Session trailers.

## Box rules (8 GB, shared)
- One client at a time. With no builder lane compiling or testing, two clients may run: `JAVA_TOOL_OPTIONS="-Xmx2200m"`, free memory at least 2400 MB, at most one other runClient.
- The client world smoke runs only with at most one other gradle JVM alive.
- Never pkill broadly.
- Proxy drivers: open craft gates the way a player does (lit fire for bushcraft, furnace for iron), do not sneak at blocks, aim the camera, wait for meshing, read `/yoi craft why` on a refusal. 20 ticks is one game minute.

## /yoi commands index
Generated by grepping `Commands.literal(...)` under src/main/java; each row is a registering class and the literal words it uses (subcommand tree flattened, order not kept). Run `/yoi` in game with tab completion for the real tree.

| Class (under com.yearofice) | Literals |
| --- | --- |
| `body/clothing/WearCommand` |  wear dress |
| `body/debug/BodyCommands` |  yoi body show part time forward chores still sos set env clear air none rain snow |
| `body/illness/IllnessCommands` |  yoi body ill cure vitc hair face grease gloves beard preset |
| `body/injury/InjuryCommands` |  yoi body injure treat heal digits roll stumble strain |
| `build/craft/CraftCommands` |  yoi gate open state practice show set add craft list tiers start now tool info edge hone |
| `build/debug/BuildCommands` |  bed info place sleep wake boughs |
| `build/dugout/DugoutCommands` |  dugout check template |
| `build/metal/MetalSystem` |  yoi info |
| `build/outpost/OutpostSystem` |  spawn struct place check age |
| `build/pieces/PieceCommands` |  yoi struct save stamp info rooms |
| `build/store/StoreCommands` |  yoi info |
| `build/wood/WoodCommands` |  yoi tree mishap info fell wood info forecast |
| `client/ui/credits/CreditsAccess` |  yoicredits |
| `core/debug/YoiCommands` |  yoi info |
| `crop/CropCommands` |  crop info germ grow frost ripen stovehouse |
| `data/migrate/MigrateSystem` |  yoi status |
| `ecosystem/ai/ScentCommands` |  yoi list |
| `ecosystem/bird/BirdCommands` |  flock status clear |
| `ecosystem/chorus/ChorusCommands` |  chorus auto set |
| `ecosystem/companion/CompanionCommands` |  yoi wolf info pup orphan follow stay hunt trust name leave test now return sled spawn info hitch fence |
| `ecosystem/debug/BigGameCommands` |  yoi carcass info herd kin haze show scent tiers deer straggler info |
| `ecosystem/debug/EcoCommands` |  yoi spawn animal list eco density info |
| `ecosystem/livestock/LivestockCommands` |  livestock spawn young feed status |
| `ecosystem/migration/MigrationCommands` |  yoi migration wave shift status |
| `ecosystem/pool/PoolCommands` |  yoi pool list show pull |
| `ecosystem/raid/RaidCommands` |  yoi set status |
| `ecosystem/small/LifeCommands` |  yoi lemming clear set insects winterkill hardness auto run |
| `ecosystem/theft/TheftCommands` |  yoi theft status toggle watch roll |
| `env/EnvCommands` |  yoi env day clear set air wind precip |
| `env/weather/WeatherCommands` |  weather show forecast storm blizzard temp glaze strike decade table sim |
| `fire/debug/FireCommands` |  fire info state fuel light bank advance weather drill skill build danger spread pot roster held kit |
| `fire/stove/StoveCommands` |  yoi stove info light douse sweep setting check sweep |
| `food/debug/FoodCommands` |  yoi info advance |
| `food/debug/PreserveCommands` |  yoi food rack age food decay info climate age |
| `food/sourdough/SourdoughCommands` |  starter state age bake |
| `food/traps/debug/TrapCommands` |  info roll yoi |
| `hud/fieldnote/FieldNotes` |  fieldnote list fire reset |
| `hunt/debug/HuntCommands` |  hunt aim shot last track wound give bear wolf |
| `hunt/fishing/debug/FishingCommands` |  fish spot fight gear place check settle bite info kit |
| `hunt/fishing/debug/WatersCommands` |  waters run draw take |
| `hunt/sign/SignCommands` |  sign list age clear |
| `item/inventory/PackCommands` |  yoi pack give wear autopickup |
| `plant/PlantCommands` |  plant info phase patch patch reset strip health identify year |
| `story/ambition/AmbitionEvents` |  ambitions |
| `story/clock/ClockCommands` |  yoi time day rate daylength |
| `story/debug/OnboardingCommands` |  onboarding status skip reset |
| `story/keepsake/NamedAnimals` |  keepsake give |
| `story/radio/RadioCommands` |  drop radio sked |
| `story/radio/StayOut` |  days years default yoi stayout |
| `story/rare/SkyCommands` |  yoi sky |
| `world/longterm/LongTermHooks` |  longterm burn camp dam status |
| `world/regions/DevBorder` |  border |
| `world/terrain/LandCommands` |  land read stand heave hazard yoi ice set |
| `worldgen/debug/WorldCommands` |  region show current slice seed |
| `worldgen/debug/WorldgenCommands` |  worldgen seed hash layers rivers lakes falls spots faces landmarks setpiece list info place map |
