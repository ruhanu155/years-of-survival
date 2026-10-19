# Year of Ice

[![build](https://github.com/ruhanu155/years-of-survival/actions/workflows/build.yml/badge.svg)](https://github.com/ruhanu155/years-of-survival/actions/workflows/build.yml)

An endless, first-person hunter-gatherer survival game set in an Alaskan valley, built as a Minecraft Java mod on NeoForge. It recreates and extends Forge Labs' "1 Year in Alaska in ULTRA REALISTIC Minecraft" for Ruhan's own play (personal use).

The design is the "Year of Ice — Generation Blueprint" doc in the claude.ai Games project. Markdown copies of every tab live in `docs/`. Start with `docs/ready-for-development.md`.

## Versions (pinned at M0)

| Part | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264 (ModDevGradle 2.0.148) |
| Java | 21 |
| Gradle | 8.14.3 (wrapper) |
| GeckoLib | 4.9.3 |
| Curios API | 9.5.1+1.21.1 |

## Commands

```bash
./gradlew build              # compile, unit tests (JUnit), jar
./gradlew runGameTestServer  # headless server running every GameTest
./gradlew runClient          # dev client (on a machine with a display)
./gradlew runData            # datagen into src/generated/resources
```

On a Mac with Homebrew Java 21: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew runClient`.

In game, operators use `/yoi info`. Every system adds its own `/yoi` debug commands.

## Running checks

`./gradlew build` is what CI's `smoke` job runs first. It compiles, runs every JUnit test and `contentLint`, and builds the jar. Then CI runs `./gradlew runGameTestServer` and passes only if the log says "All N required tests passed" and never says "failed". A second job, `hashes`, runs the JUnit hash tests on macOS and Windows so the golden hashes recorded on Linux hold on every OS. Workflow: `.github/workflows/build.yml`.

```bash
./gradlew build                      # compile, JUnit (with the inventory check), contentLint, jar
./gradlew runGameTestServer          # every GameTest; must end with "All N required tests passed"
./gradlew contentLint                # the content linter alone; report in build/content-lint/report.txt
./gradlew test --tests '*Hash*'      # the hash tests the macOS and Windows job runs
./gradlew docInventory               # regenerate studio/inventory.json from the doc and studio/inventory-rules.json
./gradlew codeowners                 # regenerate .github/CODEOWNERS from studio/ownership.json
```

`contentLint` checks the following:

- Data keys under `data/yearofice/` are snake_case, never camelCase, and carry unit suffixes (`weight_kg`, not `weight` or `weight_kilograms`).
- Data and asset file names are snake_case.
- Every asset file has exactly one `credits.json` entry. Every found asset has an allowed licence and its licence file.
- The jar holds no Mojang assets: nothing under `assets/minecraft/`, and no file byte-identical to a vanilla resource.
- `.github/CODEOWNERS` matches `studio/ownership.json`.

`InventoryTest` (JUnit) fails when the code or data registers a `yearofice:` id that `studio/inventory.json` does not plan, or registers a retired one. Add a new id to the inventory in the same pull request: put it in the doc or in `studio/inventory-rules.json`, then run `./gradlew docInventory`.

## Layout

- `src/main/java/com/yearofice/<package>`: one package per system, following the lead engineer's package map (Minecraft development › Architecture and the interlace).
- `src/main/java/com/yearofice/sim`: pure rules with no Minecraft imports, unit-tested in JUnit.
- `src/main/java/com/yearofice/gametest`: GameTests, batched by milestone (`m0`, `m2_5`, …).
- `src/main/resources/data/yearofice/<kind>/`: content as data.
- `docs/`: the blueprint export, decisions and the reconcile log.
- `studio/`: `inventory.json` (every planned id with its type, milestone and owner), `inventory-rules.json`, and `ownership.json` (departments to packages and data folders).
- `tools/content-lint`: the content linter, the doc inventory and the CODEOWNERS generator.

## Milestones

M0 seeded world → M1 water and set pieces → M2 walkable valley → **M2.5 vertical slice** → M3 start site, fire and onboarding → M4 body → M5 food and goals → M6 danger and big game → M7 the year → M8 building and outposts → M9 homestead and beyond. See `docs/ready-for-development.md`.
