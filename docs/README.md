# Year of Ice: design docs

These are Markdown exports of the "Year of Ice — Generation Blueprint" doc, taken on 2026-10-19 after the final review pass. The live doc is https://claude.ai/code/artifact/0f31ea9e-882b-4bc8-8554-69123e777660. It wins wherever a copy here differs.

**Start with [ready-for-development.md](ready-for-development.md).** It is a copy of the Ready for development tab. It lists the locked decisions and, for each milestone, what to read, build and test. Read only the sections your milestone lists.

## Blueprint tabs

Files are in the doc's tab order. Where tabs disagree on build matters, Minecraft development wins.

| # | Tab | What it holds |
| --- | --- | --- |
| 01 | [Blueprint](blueprint/01-blueprint.md) | The main design: fixed facts, survival systems, food, hunting, fishing, shelter, the year, the open world and open decisions |
| 02 | [Ready for development](blueprint/02-ready-for-development.md) | Where a developer starts: locked decisions, core-loop status, milestones M0–M9 with reading lists, tests and `/yoi` commands |
| 03 | [Minecraft engine](blueprint/03-minecraft-engine.md) | How the design maps onto a NeoForge 1.21.1 mod: platform, version pin, libraries, controls, project rules |
| 04 | [Minecraft development](blueprint/04-minecraft-development.md) | The binding build plan: layers, packages, ids, data paths, entities and AI, world gen, client, the master registry inventory |
| 05 | [World map](blueprint/05-world-map.md) | The three rings (home valley, 64 km Interior, endless Beyond), districts, named areas, province kinds and world generation |
| 06 | [Every concept from the video](blueprint/06-every-concept-from-the-video.md) | Every Forge Labs concept, chapter by chapter, with where it is specified and the rules for any that were missing |
| 07 | [Ideas from viewers](blueprint/07-ideas-from-viewers.md) | 75 viewer ideas ranked against the blueprint, with full rules for the ones adopted or adapted |
| 08 | [Art and audio production](blueprint/08-art-and-audio-production.md) | Generated textures, rigs and sounds, asset counts, licences for found assets and the credits file |
| 09 | [Open world in depth](blueprint/09-open-world-in-depth.md) | Full rules for the open world, story, rare events, taming a wolf and engine data |
| 10 | [Crafting and building in depth](blueprint/10-crafting-and-building-in-depth.md) | Recipes, tiers, tools, practice, building pieces, trees and firewood, and the warm-room heat model |
| 11 | [Bestiary](blueprint/11-bestiary.md) | 40 added species, parts and yields, carcasses, the arrival calendar, rare sightings and their build |
| 12 | [Land animals in depth](blueprint/12-land-animals-in-depth.md) | The build sheet for every ground animal, the deer included: rigs, looks, verbs, herds, packs, mothers and young |
| 13 | [Birds and the air](blueprint/13-birds-and-the-air.md) | 78 birds and 18 flying insects with flight brains, mob sheets, migration and biting flies |
| 14 | [Waters and fish](blueprint/14-waters-and-fish.md) | Nine water types, aquatic species, runs and ice events, legendary fish and fishing structures |
| 15 | [Water life in depth](blueprint/15-water-life-in-depth.md) | Mob sheets for fish and water animals, life stages, and ways to watch, count, keep and stock fish |
| 16 | [Water, calls and encounters](blueprint/16-water-calls-and-encounters.md) | Water reshaping the land, water drawing animals, calling animals and staged encounters |
| 17 | [Land and seasons in play](blueprint/17-land-and-seasons-in-play.md) | 38 environment mechanics, from ice fog and frost quakes to stand ages and snow shelters |
| 18 | [Plants and fungi](blueprint/18-plants-and-fungi.md) | Every plant, tree, moss, lichen and fungus as a seasonal block, with eight harvest verbs and crops |
| 19 | [Weather and storms](blueprint/19-weather-and-storms.md) | The weather model, fourteen storms, sky reading, shelters and climate offsets for the Interior and Beyond |
| 20 | [Decay and time](blueprint/20-decay-and-time.md) | One ageing clock for food, hides, iron, wood and camps, and time away |
| 21 | [Body, injury and sickness](blueprint/21-body-injury-and-sickness.md) | Ten body parts with their own health, injuries, the illness register and treatment |
| 22 | [Items](blueprint/22-items.md) | The item model, wear, combining and loss, and about 70 new items |
| 23 | [Structures and buildables](blueprint/23-structures-and-buildables.md) | Forty-nine structures, vehicles and buildables, and the building rules: support, snow load, fire, rot |
| 24 | [Creative concepts](blueprint/24-creative-concepts.md) | Eighteen long-term concepts: knowing the land, legends, rituals, trade, mysteries, memory, ways to play |

## Also here

- [decisions/RECONCILE.md](decisions/RECONCILE.md): the numbered cross-tab fixes, Director rulings and Ruhan's decisions (items 1–155).
- [reference/mods.md](reference/mods.md): the mods and libraries checked for reuse, with licences, and what was adopted (RECONCILE 153).
- [studio/](studio/): the briefs the studio worked from (TAB, DEPT, DEV, REVIEW, BORROW) and RESUME. Their paths are relative to the studio's working folder `v2/`, which is not in this repository.
