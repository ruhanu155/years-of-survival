# Year of Ice: working rules for Claude

**New here? Read `AGENTS.md` first** (the handoff: state, open work, where everything is).

Read `docs/ready-for-development.md` first, then only the doc sections the current milestone lists. Don't read all of `docs/`.

## Build and test
- `./gradlew build` runs JUnit. `./gradlew runGameTestServer` must end with "All N required tests passed". Never accept the Minecraft EULA on Ruhan's behalf: use the GameTest server, not `runServer`, in automation.
- Nothing merges without both passing. From M3 on, the save-migration test also runs.
- Pinned versions live in `gradle.properties`. Change them only in an upgrade round.

## Code rules (from the blueprint)
- Server decides, client shows. Client-only code goes in `com.yearofice.client`.
- Formulas live in `com.yearofice.sim` with no Minecraft imports. Adapters live in system packages.
- Systems talk only through `com.yearofice.api` services and events. Each piece of shared state has one writing package.
- Content is data under `data/yearofice/<kind>/<id>.json`, with snake_case keys and unit suffixes (`weight_kg`). Per-item numbers go in data maps.
- Ids are `yearofice:<snake_case>`. Never rename an id after 0.3.0; retire it with an alias.
- Every system ships `/yoi` debug commands. Every tuning number goes in `yearofice-common.toml`, with Gentle, Standard and Brutal presets. GameTests run on Standard.
- No hostile mobs, no vanilla mob drops (every death makes a carcass), no digging below the 2-block soil layer.
- Names and art stay original. Found assets need a licence allowing redistribution and an entry in `assets/yearofice/credits.json`.

## Git
- Branch `dev/<department>/<topic>`, then a pull request into `main`. `main` always passes.
