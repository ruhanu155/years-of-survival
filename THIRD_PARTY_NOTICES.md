# Third-party notices

Year of Ice borrows algorithms from open-source mods for its pure simulation cores (`com.yearofice.sim.*`). Each
entry below names the project, the pinned commit, the licence, and which of our files derive from it. Files marked
`release_blocker: true` carry copyleft terms and must be relicensed, replaced or published under those terms before
any public release. Ruhan's use is personal.

Full licence texts are in `licenses/`.

## TerraFirmaCraft

- Project: TerraFirmaCraft
- URL: https://github.com/TerraFirmaCraft/TerraFirmaCraft
- Branch and commit: `1.21.x` @ `c3b4d9536b82e5a9746dd793c586720a74108057`
- Licence: EUPL-1.2 (`licenses/terrafirmacraft.txt`)
- Borrowed by: Food and hunting department, borrowed-cores round

| Our file | Source path | How | release_blocker |
| --- | --- | --- | --- |
| `src/main/java/com/yearofice/sim/decay/Fresh.java` | `common/component/food/FoodCapability.java`, `FoodComponent.java`, `IFood.java` | Adapted: the creation-date-plus-modifier design, the preserve-the-proportion re-dating derivation, merge by the older date, never-decays and rotten flags, sanitise on read. Rewritten on our decay-hour axis; no lines copied | true |
| `src/main/java/com/yearofice/sim/decay/FreshTrait.java` | `common/component/food/FoodTraits.java`, `FoodTrait.java` | Adapted idea: named traits whose rate multipliers multiply. Our traits and numbers; no lines copied | true |
| `src/main/java/com/yearofice/sim/decay/ItemTemperature.java` | `common/component/heat/HeatComponent.java`, `HeatCapability.java` | Adapted design: a stored value plus a stamp, moved linearly and settled lazily on read, rate scaled by capacity. Our frost-clock rules; no lines copied | true |
| `src/main/java/com/yearofice/sim/minigame/knapping/KnappingPattern.java` | `util/data/KnappingPattern.java` | Closely adapted code: the bitset grid, row parsing and the shifted, mirrored subset match. Codecs removed, made immutable | true |
| `src/main/java/com/yearofice/sim/food/Doneness.java` | `common/component/heat/HeatCapability.java` | Reimplemented from understanding (value plus stamp settled on read); no code copied, listed for transparency | false |

## Tide 2

- Project: Tide (Lightning-64)
- URL: https://github.com/Lightning-64/Tide
- Branch and commit: `1.21.1` @ `9cfd0e170e047aa4bf049241584a056cfc81696c`
- Licence: Apache-2.0 (`licenses/tide.txt`). The repository's `LICENSE` at this commit and its `gradle.properties`
  both say Apache License 2.0; the research table (MODS.md) lists MPL-2.0, which does not match this commit.
- Borrowed by: Food and hunting department, borrowed-cores round

| Our file | Source path | How | release_blocker |
| --- | --- | --- | --- |
| `src/main/java/com/yearofice/sim/fishing/BiteModel.java` | `common/.../data/loot/TideFishingPredicate.java`, `DepthLayer.java`, `IsNightPredicate.java` | Design reference only: conditions read at the hook as one composable record. No code copied | false |
| `src/main/java/com/yearofice/sim/fishing/FightModel.java` | `common/.../data/minigame/FishCatchMinigame.java`, `client/gui/overlays/CatchMinigameOverlay.java` | Design reference only: explicit win, fail and timeout outcomes and the cancel-when-the-hook-is-gone guard. The fight itself is our tension model; no code copied | false |

The Apache-2.0 full text in `licenses/tide.txt` comes from `github/choosealicense.com@f66c5dd01866041688edd87f658b6d5ac3304f1b`.
