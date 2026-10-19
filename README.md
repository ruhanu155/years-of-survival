# Year of Ice

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

## Layout

- `src/main/java/com/yearofice/<package>`: one package per system, following the lead engineer's package map (Minecraft development › Architecture and the interlace).
- `src/main/java/com/yearofice/sim`: pure rules with no Minecraft imports, unit-tested in JUnit.
- `src/main/java/com/yearofice/gametest`: GameTests, batched by milestone (`m0`, `m2_5`, …).
- `src/main/resources/data/yearofice/<kind>/`: content as data.
- `docs/`: the blueprint export, decisions and the reconcile log.

## Milestones

M0 seeded world → M1 water and set pieces → M2 walkable valley → **M2.5 vertical slice** → M3 start site, fire and onboarding → M4 body → M5 food and goals → M6 danger and big game → M7 the year → M8 building and outposts → M9 homestead and beyond. See `docs/ready-for-development.md`.
