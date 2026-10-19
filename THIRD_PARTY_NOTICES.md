# Third-party notices

Year of Ice is a personal-use mod. Code adapted from other projects is listed here so a public release stays possible: every file marked `release_blocker: true` carries a copyleft licence and must be rewritten or swapped before any public release. Full licence texts are in `licenses/`.

## Cold Sweat

- **Project:** Cold Sweat, by Momo-Softworks (MikulDev)
- **URL:** https://github.com/Momo-Softworks/Cold-Sweat
- **Commit:** `4ebc97e37e95168819f3d17ed40708dc7a6f2fa7` (branch `1.21-FG`)
- **Licence:** GPL-3.0 (`licenses/cold-sweat.txt`). The repository's own LICENSE file holds only the header and the GPL preamble, so the full FSF text is appended. Its README mentions amendments for mods that only call the Cold Sweat API; they are not in the LICENSE file at this commit, and none of our files call that API.

| Our file | How | release_blocker |
| --- | --- | --- |
| `src/main/java/com/yearofice/sim/env/HeatIndex.java` | Adapted from `api/temperature/modifier/BlockTempModifier.java` and `util/math/CSMath.java` (`blend`): straight-line distance fade, division by (blocks between + 1), clamped stacking. Numbers are the Blueprint's. | true |
| `src/main/java/com/yearofice/sim/body/Clothing.java` | Reimplemented, no code copied. Cold Sweat's per-slot insulation sum was read; our formula is the Blueprint's. | false |
| `src/main/java/com/yearofice/sim/env/RoomHeat.java` | Reimplemented from the Blueprint's heat model, no code copied. `common/blockentity/HearthBlockEntity.java` was read for reference only. | false |

## Homeostatic

- **Project:** Homeostatic, by Wendall Cada (wendall911)
- **URL:** https://github.com/wendall911/Homeostatic
- **Commit:** `934c96cc0af2631c7d9d0122e621fceebd420b64` (branch `1.21.1`)
- **Licence:** MIT (`licenses/homeostatic.txt`)

| Our file | How | release_blocker |
| --- | --- | --- |
| `src/main/java/com/yearofice/sim/body/WetnessModel.java` | Adapted from `common/wetness/WetnessInfo.java`, `util/WetnessHelper.java`, `common/temperature/EnvironmentData.java` and `BodyTemperature.java`: the moisture source ladder, waterproof gear cutting gain, drying only while nothing wets. Per-slot layers and all rates are ours. | false |
| `src/main/java/com/yearofice/sim/body/WaterPurity.java` | Adapted from `util/WaterHelper.java` and `common/Hydration.java`: purity per source, one illness roll per drink. Classes, chances and boiling are the Blueprint's. | false |

## First Aid New

- **Project:** First Aid New, by maoruiQa, a fork of First Aid by ichttt
- **URL:** https://github.com/maoruiQa/FIrst-Aid-New
- **Commit:** `30ef21f9b603b0e0190ba9e33647568b448cebaa` (branch `main`, module `neoforge1.21.1`)
- **Licence:** GPL-3.0 (`licenses/first-aid-new.txt`)

| Our file | How | release_blocker |
| --- | --- | --- |
| `src/main/java/com/yearofice/sim/body/part/Injuries.java` | Reimplemented from understanding of `common/damagesystem/distribution/*` (data-driven source-to-part tables, paired parts, floors, distinct parts per event). No code copied; the algorithm differs (one weighted roll, overflow to the systemic pool, seeded Mulberry32). | false |
| `src/main/java/com/yearofice/sim/body/part/InjurySource.java` | Reimplemented, no code copied. First Aid's `damage_distributions/*.json` shape was read; our keys follow the Body tab's `injury_source/` files. | false |
