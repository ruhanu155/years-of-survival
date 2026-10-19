# Art and audio production

Every texture, model, animation and sound comes from scripts in the repository or from found assets licensed for redistribution. There is no human artist: generators turn recipe JSON into 16 px textures, GeckoLib rigs and Ogg files during datagen, and the output is committed. This tab sets the style rules, the three generators, the found-asset rules, the sound plan, the budget per milestone and the checks.

## How this tab fits

**The decision.** Ruhan chose "Generated only and use any models you can find aswell for art and textures" (RECONCILE 116). This tab builds that decision. It answers Director item 105: who makes the assets, with what tools, what placeholders each milestone allows, and how sounds are sourced and credited.

**What it supersedes.** The engine tab's art budget and `tools/render-sounds` give way to this tab, as does the hand-made Blockbench step of "Adding a species end to end". Data and build › Settled across tabs lists each change.

**What it keeps.** Minecraft engine › The look stands: 16 × 16 textures, GeckoLib at 16 px per block, a muted palette and the built-in look pack. Paths follow Entities and AI › Models, rigs and animation controllers. Audio routing, captions, footstep bands and ambience beds stay as Player systems and client › The Audio bus sets them.

**Personal use still keeps credits.** The pack is for Ruhan's own machine (RECONCILE 119). Credits stay anyway: CC-BY requires them even in private copies, and a clean provenance record costs one JSON line per asset.

### What has to be made

The counts come from the master registry inventory in Content pipeline, build and the evolving modpack, plus the tabs' sound sheets. They are the targets every budget below adds up to.

| Asset kind | Count |
| --- | --- |
| Mob types | 155 |
| Other entities (carcasses, vehicles, thrown things) | 21 |
| GeckoLib rigs | 35 |
| Entity texture sets | 192 |
| Plant blocks with phases | 107 |
| Plant sprites | About 2,100 |
| Other blocks | 190 registered, 176 drawn |
| Items | 226 |
| Item icon recipes | 118 |
| HUD layers | 15 |
| HUD sprites | 13 strip icons, the 24 × 48 body figure, 4 frost masks, 3 headwear masks, gauges |
| Sound events | 409 |
| Music pieces | 12 |

**Where the counts come from.** Mobs, blocks, plants and items are the master inventory's; rigs are Entities and AI's; HUD layers are Player systems and client's. The 192 texture sets are 155 species bases, 25 sex and colour-morph patterns and 12 legend and life-stage sets. Of the 190 other blocks, 14 are technical and get no art of their own.

**The sound count is larger than the brief's 220.** Every tab together names 318 distinct event ids outside the beds. They are 156 species calls, 31 steps, 28 body sounds, 21 player calls and 82 world, plant, trap, tool and UI events. With the 69 ambience events and the 22 wind, water, fire, tool and ice ids this tab names, the budget below plans 409.

## The style guide

**Scripts follow numbers, not taste.** Every rule here is a check a script can run. The palette lives in `tools/artgen/palette.json`; the generators read it and the linter enforces it, so a texture that breaks a rule fails the build.

### Hex ramps per material class

**Five steps per ramp, dark to light.** A ramp is one material under neutral daylight. Step 2 is the base colour; steps 0 and 1 are shadow and crevice; steps 3 and 4 are lit faces and highlights.

| Ramp | 0 | 1 | 2 | 3 | 4 |
| --- | --- | --- | --- | --- | --- |
| `bark_spruce` | #2b221c | #3d3027 | #544234 | #6b5644 | #857059 |
| `bark_tamarack` | #3a2219 | #552f22 | #6f412e | #8a573f | #a37358 |
| `bark_birch` | #3a3633 | #8c8780 | #bdb8ae | #d9d4c8 | #ece8de |
| `bark_aspen` | #4a4a3e | #7d8170 | #a3a891 | #c0c4ae | #d6d9c6 |
| `wood_cut` | #5e4630 | #7f6143 | #9c7c58 | #b8996f | #d0b58a |
| `needle_green` | #1c2b20 | #26392a | #324a35 | #435e44 | #587456 |
| `needle_tamarack` | #2f3d1f | #45592b | #5d7536 | #7a9145 | #98ab5c |
| `leaf_green` | #26331f | #364829 | #4a5f35 | #617744 | #7c9056 |
| `leaf_silver` | #4a5048 | #6c7468 | #8e9787 | #adb5a5 | #c8cfc0 |
| `stem_red` | #3b1d18 | #57281f | #743629 | #8f4835 | #a65f49 |
| `soil` | #241c16 | #33281f | #45372b | #594838 | #6e5b48 |
| `duff` | #2a2219 | #3c3022 | #50412e | #66543c | #7d6a4e |
| `rock_schist` | #34363a | #4a4d51 | #62666a | #7b7f82 | #96999b |
| `gravel` | #3e3b37 | #5a554e | #78716a | #958e85 | #b0aaa1 |
| `moss_green` | #2a3318 | #3b4720 | #4f5e2b | #667637 | #7f8e47 |
| `sphagnum` | #4a3a22 | #6b5a2e | #8f7d3f | #a99253 | #c2a96c |
| `lichen_pale` | #6a6c5e | #8a8d7a | #a6a993 | #bec1aa | #d3d5c0 |
| `snow` | #8e9aa8 | #aeb9c5 | #c8d1db | #dde4eb | #eef2f6 |
| `ice` | #2f4a5c | #45667a | #5f8396 | #80a2b2 | #a6c2cf |
| `fur_moose` | #1d1612 | #2d221b | #3f3026 | #554133 | #6d5644 |
| `fur_hare_summer` | #3a2e22 | #57452f | #76603f | #937a53 | #ad9469 |
| `fur_white` | #8f949a | #b2b6ba | #cdd0d2 | #e2e4e5 | #f1f2f2 |
| `feather_ptarmigan_summer` | #2e241a | #4a3a28 | #6a5438 | #8a7049 | #a98c5f |
| `meat_raw` | #3d1414 | #5c1d1c | #7d2a26 | #9a3d35 | #b4574b |
| `fat_cream` | #8a7a5c | #a8977a | #c2b295 | #d6c9ad | #e6dcc5 |
| `bone` | #7a7264 | #9c9383 | #b9b0a0 | #d0c8b8 | #e3ddd0 |
| `iron` | #25272a | #393c40 | #50545a | #6a6f75 | #878c92 |
| `rust` | #3b1f12 | #5a2e17 | #7a401e | #975628 | #a86d3e |

**Accent ramps** are for fruit, flowers and autumn: `berry_red` (step 2 #9c2516), `berry_blue` (#353d63), `flower_magenta` (#93355f), `flower_white` (#c2c4b5), `flower_yellow` (#b08c21), `needle_gold` (#a07a2a) and `leaf_fall` (#93512a). The emissive ramp `fire` (#d0640f) is only for flame, embers and glow layers. Full ramps are in `palette.json`.

**Ramps grow by recipe, not by hand.** A new species adds its own ramps (`fur_lynx`, `feather_mallard_drake`) to `palette.json` in its pull request. The linter checks each new ramp against the rules below, so the palette stays one family.

### Palette rules

1. **Chroma.** Chroma is the largest RGB channel minus the smallest, on 0–1. Material ramps stay at or under 0.45; accent ramps at or under 0.70; emissive ramps are exempt.
2. **Value steps.** Each step's relative luminance is 1.15 to 2.5 times the step before. Birch and aspen bark are exempt at step 0, which is the black lenticel mark.
3. **Hue drift.** Shadows lean toward blue-violet and highlights toward yellow, by at most 20° of hue across a ramp. Near-greys (chroma under 0.08) are exempt, since their hue is unstable.
4. **Colours per texture.** A texture uses at most 3 ramps plus its outline colour, and at most 12 distinct colours.
5. **Off-palette pixels.** None. Every opaque pixel matches a ramp entry exactly. The prototype tool already passes this on all 29 of its test textures.
6. **Never pure.** No #000000 and no #ffffff. The darkest allowed colour is the item outline #1a1512; the lightest is `snow` step 4.

### Shading, noise and outline rules at 16 × 16

**Blocks tile; items and sprites stand alone.** The two follow different rules.

| Rule | Blocks (tiling faces) | Plant sprites | Items and icons | HUD sprites |
| --- | --- | --- | --- | --- |
| Size | 16 × 16; 16 × 32 for 2-tall plants | 16 × 16 per block of height | 16 × 16 | 9 × 9 icons; 24 × 48 body figure |
| Tiling | Seamless on all four edges; noise is tileable | No | No | No |
| Light | None baked in; Minecraft shades faces | From above: top pixel of each leaf one step lighter | From the top left: lit rim +1 step, shadow rim −1 step | Flat |
| Outline | None | None | 1 px outside the silhouette in #1a1512 | 1 px in the ramp's step 0 |
| Noise | 2–3 octaves of tileable value noise, quantised to 3–4 steps | Leaf placement jitter only | Optional ±1-step dither, at most 25% of pixels | None |
| Pattern scale | Features at least 2 px wide; no single-pixel checkerboard | Stems 1 px; berries 1 × 2 px | Features at least 2 px | Glyph strokes 1 px |
| Transparency | Opaque, except leaves and glass-like ice (cutout, alpha 0 or 255) | Cutout | Cutout | Cutout |

**Grain direction is a rule.** Bark and log sides run vertical; plank and cut faces run along the long axis; end grain uses rings. Snow and soil use isotropic noise. Ice uses long low-contrast streaks plus 1–3 pale crack lines.

**Contrast inside a texture.** Block faces keep their darkest and lightest pixels within 3 ramp steps of each other, so a wall of them does not shimmer. Items may use the full 5 steps plus the outline.

### GeckoLib cube budgets per size class

**Tiers come from body length.** Every entity is drawn at 16 px per metre, the same as blocks, so a texel on a moose is a texel on a log. A feature smaller than 1 px (a vole's ear) is still drawn at 1 px.

| Tier | Body length | Examples | Cubes, max | Bones, max | Texture atlas | Screen-shot check distance |
| --- | --- | --- | --- | --- | --- | --- |
| Tiny | Under 0.15 m | Shrew, voles, sculpin, insects | 12 | 10 | 32 × 32 | 3 m |
| Small | 0.15–0.6 m | Hare, ptarmigan, squirrels, ducks, grayling | 24 | 20 | 64 × 32 | 8 m |
| Medium | 0.6–1.4 m | Fox, lynx, beaver, eagle, salmon, pike | 40 | 26 | 64 × 64 | 15 m |
| Large | 1.4–2.2 m | Wolf, caribou, black bear, sheep, crane | 60 | 32 | 128 × 128 | 30 m |
| Huge | Over 2.2 m | Moose, grizzly, bison, muskox | 80 | 40 | 256 × 128 | 60 m |

**What counts.** Antlers and horns count: a bull moose at full palm spends about 14 of its 80 cubes. Seasonal fluff and fat are bone scales, never extra cubes. Carcasses reuse the species model in a dead pose.

### Seasonal variants by colour remap

**A variant is the base texture with ramps swapped.** The painter draws a species or block once in its base ramps. `artgen` writes each variant by replacing ramp entries step for step, so shading and pattern stay identical and only colour changes.

| Variant family | Base | Remap |
| --- | --- | --- |
| Fall leaves | `leaf_green` | `leaf_fall` (birch, aspen, willow: `flower_yellow`) |
| Tamarack gold | `needle_tamarack` | `needle_gold`, Sep 15 – Oct 5 |
| Winter coat | Summer fur ramp | `fur_white`; mottled = a seeded 50% pixel mask of both |
| Ptarmigan | `feather_ptarmigan_summer` | `fur_white` on body; wings stay white all year |
| Moult | Coat ramp | Steps 2–3 shifted one step darker in a patch mask |
| Breeding plumage | Hen pattern | Drake or male pattern set (a pattern set, not a remap) |
| Spawning colour | Fish base | `berry_red` on flank steps 2–3 (sockeye), `stem_red` belly (char) |
| Snow-dusted | Any top face | Top 3 rows replaced by `snow` steps 2–4, seeded edge |
| Decay stages | Fresh ramp | Food: `meat_raw` → brown → grey-green; wood: bleach then darken; iron → `rust` |

**Colour handlers stay for tinted blocks.** Grass, moss and leaf blocks keep the runtime tint from `assets/yearofice/season/palette.json` that Player systems and client › Rendering sets. Remaps are for everything a handler cannot tint: entities, items, plant sprites and fixed-colour blocks. The two never stack on one texture.

### The HUD look

**HUD sprites are drawn by the same tool.** `artgen` icon recipes make the 13 strip icons (9 × 9) and the body figure (24 × 48). They follow the glyph list in Player systems and client › The vitals strip and the body figure. The frost layer uses 4 masks at 480 × 270, one per coverage stage, grown from the corners by seeded dendritic noise.

**Frames and gauges are 9-slice.** The minigame gauges, the work ring and the messenger frame are 9-slice sprites in `iron` and `wood_cut` ramps. Text uses the vanilla font; no custom font is shipped.

## The texture generator

**`tools/artgen/` turns recipes into PNGs.** It is Python 3.12 with Pillow and NumPy, pinned in `tools/requirements.txt`. One recipe file per texture family lives in `tools/artgen/recipes/<kind>/<id>.json`; the output goes to `src/generated/resources/assets/yearofice/textures/`. A working prototype of the core already renders the three examples below with 0 palette failures.

**Determinism is a rule.** Every random draw seeds from SHA-256 of the recipe id, the face and the layer index. Two runs on any machine give byte-identical PNGs, so `checkDatagen` can diff them.

### Recipe JSON

**A recipe is layers over a ramp.** Each layer draws into a step map (which ramp step each pixel takes) and a ramp map (which ramp it uses). The last pass turns the two maps into colours, so a recipe can never leave the palette.

**Fields.** `id` names the block, item or plant; `kind` (`block`, `plant`, `icon`, `entity`, `hud`) picks the renderer; `class` sets the material's contrast and pattern limits. `faces` maps each face to a layer list. A layer has a `type`, a `ramp`, the `steps` it maps onto and its pattern numbers (`scale`, `octaves`, `spacing`, `density`).

**Layer types and variants.** The types are `noise`, `furrows`, `plates`, `rings`, `speckle`, `streaks`, `cracks`, `scales`, `spots` and `mask`. `variants` maps variant names to remaps; `seed_salt` changes the look without renaming the id.

**Three examples.** The prototype ran these recipes; the `variants` lines are the planned additions. The tamarack log tiles on its sides and draws rings on its ends:

```json
{
  "id": "yearofice:tamarack_log",
  "kind": "block",
  "class": "bark",
  "faces": {
    "side": { "layers": [
      { "type": "noise", "ramp": "bark_tamarack", "scale": 2, "octaves": 3, "stretch": "vertical", "steps": [1, 4] },
      { "type": "furrows", "spacing": 4, "jitter": 1, "step": 0 },
      { "type": "plates", "count": 6, "step": 1 } ] },
    "top": { "layers": [
      { "type": "rings", "ramp": "wood_cut", "spacing": 1.6, "steps": [2, 3], "bark_ramp": "bark_tamarack" } ] }
  },
  "variants": { "snow_dusted": { "top_rows": 3, "ramp": "snow" } }
}
```

The soapberry bush is a form recipe with 8 phases. `taken` removes fruit first and then leaves, so a picked bush shows bare stems as Plants and fungi requires:

```json
{
  "id": "yearofice:soapberry_bush",
  "kind": "plant",
  "form": { "type": "berry_shrub", "stems": 5, "spread_deg": 110, "height_px": 12, "droop_deg": 12,
            "stem_ramp": "stem_red", "leaf_two_tone": "leaf_silver", "fruit_glint": true },
  "phases": {
    "dormant":   { "leaves": 0 },
    "buried":    { "leaves": 0, "snow_cap": true },
    "bud":       { "leaves": 6,  "leaf_ramp": "moss_green" },
    "leaf":      { "leaves": 22, "leaf_ramp": "leaf_green" },
    "flower":    { "leaves": 22, "leaf_ramp": "leaf_green", "fruits": 5,  "fruit_ramp": "flower_yellow" },
    "fruit":     { "leaves": 20, "leaf_ramp": "leaf_green", "fruits": 14, "fruit_ramp": "berry_red" },
    "seed":      { "leaves": 18, "leaf_ramp": "leaf_green", "fruits": 4,  "fruit_ramp": "berry_red" },
    "senescent": { "leaves": 12, "leaf_ramp": "leaf_fall" }
  }
}
```

The moose quarter is an icon recipe. It is not a registry item: it is the load drawn in `yearofice:carry_slot` while a quarter rides the `carry` attachment, so it lives at `textures/item/load/moose_quarter.png`:

```json
{
  "id": "yearofice:moose_quarter",
  "kind": "icon",
  "folder": "item/load",
  "shapes": [
    { "kind": "poly", "ramp": "meat_raw", "step": 2, "noise": 0.25,
      "points": [[3,4],[8,2],[12,4],[13,8],[11,11],[8,12],[5,11],[3,8]] },
    { "kind": "poly", "ramp": "fat_cream", "step": 2, "points": [[3,4],[8,2],[9,3],[4,5]] },
    { "kind": "line", "ramp": "bone", "step": 3, "width": 2, "points": [[9,11],[12,14]] },
    { "kind": "ellipse", "ramp": "bone", "step": 4, "box": [11,13,13,15] },
    { "kind": "line", "ramp": "fur_moose", "step": 2, "points": [[12,5],[13,8]] }
  ],
  "variants": { "aging": { "meat_raw": "meat_aging" }, "spoiled": { "meat_raw": "meat_spoiled" } }
}
```

The decay ramps `meat_aging` and `meat_spoiled` join the palette with the food recipes, under the same chroma rules.

### Plant phase sprites from form recipes

**Ten parametric forms draw all 107 plant blocks.** A form is a small drawing program: stems as polylines from the base, then leaves, flowers and fruit placed along them by seeded positions. A species file sets the numbers; the form never changes per species.

| Form | Plants it draws | Key parameters |
| --- | --- | --- |
| `herb_cross` | Herbs, grasses, sedges, ferns under 1.2 m | `stems`, `spread_deg`, `height_px`, `leaf_shape` (blade, oval, frond, whorl), `flower_shape` (spike, umbel, cup, head) |
| `tall_herb` | 2-block plants: monkshood, false hellebore, devil's club, tall larkspur | As `herb_cross`, 16 × 32, with `upper_share` |
| `berry_shrub` | Low shrubs and berry bushes | `stems`, `droop_deg`, `leaf_two_tone`, `fruit_glint`, `fruit_cluster` |
| `shrub_leaf` | Leaf blocks of willows, alders and dogwood on `shrub_stem` | `leaf_density`, `catkin` (willows), `twig_ramp` |
| `moss_carpet` | Mosses, as a top-face texture | `strand_len`, `clump_noise`, two ramps for tip and base |
| `lichen_decal` | Lichens on rock and bark | `rosette_count`, `lobe_px`, `edge_step` |
| `bracket` | Bracket fungi, 4 sizes | `shelf_count`, `zone_bands`, `pore_ramp` |
| `mushroom` | Mushrooms, 4 ages | `cap_w`, `cap_shape`, `stem_h`, `gill_ramp`, `rot_mask` |
| `aquatic_float` | Lily pad, buckbean leaves, pond weed on the surface | `pad_count`, `notch`, `flower_float` |
| `aquatic_emergent` | Bulrush, horsetails, bur reed, wild calla | `stems`, `waterline_px`, `head_shape` |

**At most 20 sprites per plant.** Each of the seven visible phases gets a sprite. Leaf, flower, fruit and seed have something to take, so each gets `taken` 0–3: 16 sprites. The 3 other phases and 1 shared `buried` snow cap bring the most to 20.

**Fungi differ.** Mushrooms use `age` 0–3 and brackets `size` 0–3 instead.

**Totals.** The 107 plants come to about 2,100 sprites and 1.1 MB of PNG. They fit the block atlas with room to spare: 2,100 sprites of 16 × 16 is under a quarter of a 2,048 × 2,048 page.

**Block models are generated with them.** `artgen` also writes the blockstate and model JSON for each plant, one model per (`phase`, `taken`) pair, using vanilla's `minecraft:block/cross` parent by reference. Phase changes are block-state changes, as Plants and fungi sets, so no gameplay rule reads a tint.

### Item icons

**Three routes make the 226 icons.** Each item names its route in `tools/artgen/recipes/item/<id>.json`.

| Route | Count | How | Examples |
| --- | --- | --- | --- |
| Icon recipe | 118 | Shapes (polygon, ellipse, line, mask) filled with ramp steps; lit rim, shadow rim and outline by rule | `knife`, `longbow`, `messenger`, `spile`, `rod`, `cast_iron_skillet` |
| Family recolour | 72 | One template per family, repainted with the species ramp and an optional mark | 10 pelts and hides, 9 feathers, 12 organs and glands, 9 teeth, claws and horns, 9 berries, 13 baits, 10 meats and fish products |
| Render | 36 | An orthographic render of the block or entity model at 3/4 view, downsampled to 16 × 16 and requantised to the palette | `whole_game`, `lemming`, `frozen_wood_frog`, `yukon_floater`, `wild_egg`, `plant_clump`, `travois`, the decoys |

**Food icons carry their decay stages.** Each food icon gets the five stage slots of Decay and time as remap variants, chosen by an item model property from the `fresh` component. Soiled food adds the existing 1-pixel dark specks as a `soiled` overlay.

### Blocks

**176 of the 190 non-plant blocks are drawn.** The 14 technical blocks (for example `dev_border` and `dugout_air`) use one shared generated texture or none. The rest fall into recipe groups.

**Five recipe groups cover them.** Logs (11 log blocks, 2 leaf blocks and snags) use `furrows`, `plates` and `rings`, with decay remaps for punk and rotten wood. Ground and rock use isotropic noise, `speckle` and `cracks`. The ten ice forms use `streaks` and `cracks` on the `ice` ramp, cutout where clear.

**Built pieces and stations.** Built pieces use cut-wood and stone recipes with joint lines; set-piece boards and cairns are their weathered remaps. The 71 stations are baked models with generated textures, with GeckoLib only where a tab asks for motion.

**Vanilla blocks the game reuses** (water, some stone) are retextured in the "Year of Ice look" pack with `artgen` output. The tool never reads a vanilla PNG, not even as input.

## The model generator

**`tools/modelgen/` builds every rig from a template and a parameter file.** A template is a Python function that lays out bones and cubes from body measurements in metres. A species file gives the measurements, the paint and the gait. The output is a GeckoLib `.geo.json`, a painted texture and a merged `.animation.json`, at the paths Entities and AI sets.

**The prototype already works.** It builds a moose (36 bones, 34 cubes), a snowshoe hare (21 bones, 19 cubes) and a willow ptarmigan (13 bones, 11 cubes). All three pass validation, animations included.

### Rig templates for all 35 rigs

**Seven template functions cover 35 rigs.** Rigs in one family share a function and differ by a preset of default proportions. Every rig owns a `mark` bone for the Known-animal and legend marks.

| Template | Rigs | Bones | Tiers | Species |
| --- | --- | --- | --- | --- |
| `quadruped` | Ungulate | 30 + antler or horn children | Large, huge | 8 |
| `quadruped` | Cervid | 38: the Ungulate's 30 plus `tail_1`–`2`, antler beams with up to 5 tines and `neck_swell` (Land animals in depth › Deer) | Large | 2 |
| `quadruped` | Canid, felid | 22, 21 | Medium, large | 4, 1 |
| `quadruped` | Ursid | 20 (with `hump`) | Large, huge | 2 |
| `quadruped` | Large rodent, porcupine | 13, 12 | Small, medium | 4, 1 |
| `quadruped` | Small rodent and lagomorph | 15 (folded hind legs, long foot) | Tiny, small | 14 |
| `long_body` | Long mustelid | 18 (`spine_1`–`3`) | Tiny to medium | 7 |
| `small_land` | Bat, frog, hen | 8–12 | Tiny, small | 1, 2, 1 |
| `bird` | Grouse, dabbler, diver duck, goose and swan, loon and grebe, crane, shorebird, gull and tern, raptor soarer, raptor fast, owl, woodpecker, corvid, kingfisher and dipper, songbird | 12–20 | Tiny to large | 5, 5, 5, 3, 4, 1, 5, 2, 5, 5, 7, 3, 3, 2, 23 |
| `fish` | Salmonid, esocid, gadid, coregonid, catostomid, small fish | 6–14 | Tiny to medium | 8, 1, 1, 7, 1, 5 |
| `eel` | Lamprey | 8 (a 6-segment chain) | Small | 1 |
| `critter` | Insect rig | 6 | Tiny | 5 |

The rigs add up to 35: 8 + 1 + 3 + 15 + 6 + 1 + 1 by template. The species column adds to 154. `yukon_floater` is the 155th mob: a static 1-bone shell model, not a rig.

### Shared bone names

**One name list, used by every template and every animation.** Animations bind by name, so a shared animation set works on any species of the rig, and a missing bone fails validation, not the game.

- **Trunk and head:** `root`, `body`, `chest`, `hips`, `hump`, `spine_1`–`3`, `neck` or `neck_1`–`2`, `head`, `jaw`, `ear_l`, `ear_r`, `whisker`, `beak`, `bell`, `mark`.
- **Limbs and tail:** `leg_<fl, fr, hl, hr>_<upper, lower, foot>`; birds `leg_l`, `leg_r`; `tail` or `tail_1`–`3`; `wing_l`, `wing_r`, `wing_l_tip`, `wing_r_tip`.
- **Head gear and fins:** `antler_<side>`, `antler_<side>_palm`, `antler_<side>_tine_<n>`, `horn_l`, `horn_r`; `fin_dorsal`, `fin_pec_l`, `fin_pec_r`, `fin_tail`, `gill_l`, `gill_r`; `seg_1`–`6` (lamprey); `quill_shell`, `sac`.

**The foot is always `foot`.** Land animals in depth says "hoof" for ungulates and "paw" for others; one name lets the shared `walk` play on every quadruped. The entity developer only reads bone names in `setCustomAnimations` for antler and fat scales, so the rename touches no gameplay.

### Species parameter files and UV painting

**One file per species in `tools/modelgen/species/<id>.json`.** It holds the template, the tier, body measurements, the paint map and the gait. The species data file in `data/yearofice/species/` stays the gameplay truth; `modelgen` reads only its hitbox, to check the model fits it within 10%.

```json
{
  "id": "moose", "template": "quadruped", "rig": "ungulate", "tier": "huge",
  "body_len_m": 2.0, "body_h_m": 0.85, "body_w_m": 0.7, "chest": true, "hump_h_m": 0.15,
  "leg_len_m": 1.15, "leg_w_m": 0.16, "neck_len_m": 0.45, "neck_angle_deg": 20,
  "head_len_m": 0.65, "head_w_m": 0.3, "head_drop_m": -0.1, "ear_len_m": 0.22, "bell_m": 0.25,
  "tail_len_m": 0.1, "tail_bones": 1,
  "antler": { "beam_m": 0.25, "palm_m": 0.4, "tines": 4 },
  "gait": { "walk_s": 1.6, "walk_amp": 22, "run": "trot", "run_s": 0.9, "run_amp": 35, "knee": 30 },
  "paint": { "body": ["fur_moose", 2], "chest": ["fur_moose", 1], "bell": ["fur_moose", 0],
             "leg_*_lower": ["fur_white", 1], "antler_*": ["bone", 2] },
  "patterns": [ { "kind": "countershade", "bones": ["body"], "belly_step": -1 } ],
  "variants": { "cow": { "drop_bones": ["antler_*"] }, "calf": { "ramps": { "fur_moose": "fur_calf" } } }
}
```

**Painting follows box UV.** A shelf packer lays each cube's six faces on the atlas. The painter then fills each face from the bone's ramp and step.

1. Top faces take step +1 and bottom faces step −1, so light comes from above on every animal.
2. Side faces get 2-octave noise at ±1 step, with the grain along the body's long axis.
3. Patterns draw over the faces: `countershade` (lighter belly), `saddle`, `stripes`, `spots`, `bars` (fish parr marks), `speckle` (grouse), `mask` (face masks such as the harlequin duck's).
4. Eyes are 1 × 1 px in step 0, with a step-4 catch-light on medium and larger tiers. Night eyeshine is the emissive `eyes.png` layer.
5. Variants (`summer`, `mottled`, `winter`, `moult`, `bull`, `cow`, `calf`, `cross`, `silver`) come from the remaps of the style guide, written as `textures/entity/<id>/<variant>_<coat>.png`.

### Procedural animations from gait parameters

**The base set is computed, not keyframed by hand.** `modelgen` writes `rig_<rig>.animation.json` with the shared set for each rig and species overrides from the gait block. Hand-keyed extras (`rut_thrash`, `drum`, `lek_dance`, and the encounter clips of RECONCILE 171: `huff_jaw_pop`, `stand_sniff`, `bluff_charge`, `swat`, `pin_bite`, `feed`, `flinch`, `retreat_look_back` and the black bear's `climb` for both bears; `ears_back_hackles`, `charge`, `front_stomp` and `bluff_stop` for the moose; `lunge_snap` for the wolf and coyote, whose `circle` is procedural from the walk) live in `<id>_extra.animation.json`, and `AnimationMergeProvider` merges both as Entities and AI sets (Minecraft development › Encounter clips). The player's hand clips `cover_neck`, `push_up`, `desperate_strike`, `catch_fall` and `stagger` are GeckoLib hand animations, keyed by hand as well.

**Legs follow a phase table.** Each upper leg bone swings as a sine with its own phase offset. The lower bone flexes only on the forward swing.

```latex
\theta_{upper}(t) = A \sin\left(2\pi\left(\frac{t}{T} + \phi_{leg}\right)\right), \qquad
\theta_{lower}(t) = \max\left(0,\ -K \cos\left(2\pi\left(\frac{t}{T} + \phi_{leg}\right)\right)\right)
```

| Gait | Front left | Front right | Hind left | Hind right | Used for |
| --- | --- | --- | --- | --- | --- |
| Walk | 0.25 | 0.75 | 0.00 | 0.50 | All quadrupeds under 2 m/s |
| Trot | 0.00 | 0.50 | 0.50 | 0.00 | Canids, ungulates 2–6 m/s; moose run |
| Gallop | 0.60 | 0.70 | 0.00 | 0.10 | Ungulates, canids, bears over 6 m/s |
| Bound | 0.50 | 0.50 | 0.00 | 0.00 | Hares, squirrels, mustelids |

| Animation | Rule |
| --- | --- |
| `walk`, `run` | Gait table with the species' period, amplitude `A` and knee `K`; body bobs at twice the stride rate; neck counter-swings 4–8° |
| `idle`, `graze`, `alert` | Breathing scale 1.00–1.02 over 6 s; neck down 55–60° with a 2 Hz chew; head up 25° and ears forward in 0.4 s, then hold |
| `flee`, `swim` | The run with tail up and ears back; the trot table 1.3 times slower at the waterline |
| `fly`, fish `swim` | Wings ±70° and −40° at the species' flap rate, `glide` holds 10°; a body wave rising 3× toward the tail |
| `death` | Roll 90° and drop over 1.5 s, held on the last frame for the carcass hand-off |

### Validation

**`modelgen --check` runs in `contentLint`.** A model that fails any line stops the build.

1. Every rig bone exists; no duplicate names; every parent exists.
2. Cubes and bones within the tier budget.
3. UVs inside the atlas, with no overlapping faces.
4. Every bone an animation names exists.
5. The bounding box fits the species hitbox within 10%.
6. The rig's whole shared set is present; loops close within 0.5°.
7. Every named texture variant exists and passes the palette check.

### The Blockbench fallback

**Blockbench is for the cases a template cannot reach.** A legend's odd antler, the porcupine's quill shell or a set-piece prop can be edited by hand in Blockbench with the GeckoLib plugin.

1. Run `modelgen --export <id>` to write the generated model as the starting file.
2. Edit in Blockbench; keep every bone name.
3. Save to `tools/modelgen/overrides/<id>.geo.json` with a line in `overrides/README.json` saying why.
4. `modelgen` copies the override instead of generating, and still runs all seven checks on it.

## Found assets

**Found assets fill gaps the generators draw badly.** Animal calls, a few hard surfaces and reference models are where a found file beats a recipe. Every found file passes the same import procedure, is reduced to our palette and format, and gets a credits entry before it can merge.

### Licences

| Licence | Allowed | Conditions |
| --- | --- | --- |
| CC0, public domain (including US federal government works) | Yes | Record the source anyway |
| CC-BY 3.0 or 4.0 | Yes | Credit line with author, title, source type and licence, in `credits.json` and the in-game credits screen |
| MIT, BSD-2, BSD-3, zlib | Yes | Keep the licence text in `licenses/<asset_id>.txt` |
| SIL Open Font License | Yes, fonts only | Not planned; the vanilla font is used |
| CC-BY-SA | Yes, for personal use (RECONCILE 151) | Credit line as for CC-BY, and `release_blocker: true` in `credits.json`, so the asset is swapped before any public release |
| Any NC (non-commercial) licence, including CC-BY-NC and CC-BY-NC-SA | Yes, for personal use (RECONCILE 151) | Credit line, and `release_blocker: true` in `credits.json`, so the asset is swapped before any public release |
| Any ND (no derivatives) licence | No | ND forbids the palette conversion |
| "Free for personal use", "royalty free" without a licence text, unknown or missing licence, All Rights Reserved (for example Naturalist's assets) | No | No file without a licence text |
| Assets from other mods, resource packs or games | No | Only with written permission from the author, filed in `licenses/` |

**Minecraft's own assets are referenced at runtime only.** A model may name `minecraft:block/cross` as its parent, or a texture by its resource location, because Minecraft supplies it at runtime. No vanilla PNG, OGG or model file is extracted, edited, committed, shipped or used as generator input. The linter rejects any file under `assets/minecraft/` in the repository except the "Year of Ice look" overrides, and those must be `artgen` output.

### Source types

**Where found assets come from, by type.** No links are kept in the doc; the credits file records the exact source of each file.

- CC0 texture libraries (scanned bark, rock, snow), only as colour-sampling input to `artgen`, never pasted.
- CC0 and CC-BY low-poly model libraries, as proportion references and Blockbench starting points.
- CC0 and CC-BY nature-sound archives with species-labelled recordings.
- Public-domain recordings from government wildlife and park agencies.
- Open-source game asset collections released under CC0 or CC-BY.
- Open-weight generative models whose licence allows redistributing outputs (see below).

**Generative models are a tool, not a source of finished art.** Ruhan's "use any models you can find" includes image and audio models whose output terms allow redistribution. Their output is found input: it passes the import procedure and is always reduced to 16 px and our ramps, or to our audio chain.

**Prompt and credit rules.** Prompts never name a living artist, another game or another mod. The credits entry records the model name, its licence, the prompt and the seed.

### Import procedure

1. **Provenance.** Save the original untouched in `assets-src/found/<asset_id>/original.<ext>` (Git LFS) with `source.json`: source type, title, author, original URL, retrieval date and licence id.
2. **Licence file.** Copy the licence text or the CC deed name to `licenses/<asset_id>.txt`. No licence text, no import.
3. **Check the licence against the table.** The importer script `tools/import-asset` refuses NC, ND, SA, unknown and personal-use licences by id.
4. **Convert.** `artgen import` downscales textures to 16 px per metre, quantises them to the nearest ramp steps (CIEDE2000) and applies the target kind's rules. `modelgen import` remeshes models to cubes on the rig's bones. Sounds go through the processing chain.
5. **Review.** The converted file shows in the CI gallery next to its source; the Director approves it under studio rule 9.
6. **Credit.** `tools/import-asset` writes the `credits.json` entry and adds the asset id to the output file's sidecar, `<file>.prov.json`.

### The credits file

**One file, `assets/yearofice/credits.json`, for art and sound.** It replaces the sound-only `assets/yearofice/sounds/credits.json` named in Player systems and client. Generated assets are listed too, with `source_type: "generated"` and the recipe path, so every shipped file has exactly one entry.

```json
{
  "schema": 1,
  "entries": [
    {
      "asset_id": "snd_wilsons_snipe_winnow_01",
      "files": ["assets/yearofice/sounds/entity/wilsons_snipe/winnow1.ogg"],
      "source_type": "nature_sound_archive",
      "title": "Wilson's snipe winnowing display",
      "author": "Recordist name as given",
      "licence": "CC-BY-4.0",
      "licence_file": "licenses/snd_wilsons_snipe_winnow_01.txt",
      "retrieved": "2026-11-02",
      "original_sha256": "9f2c…",
      "changes": "Trimmed to 2.4 s, mono, 44.1 kHz, high-pass 300 Hz, normalised to −16 LUFS",
      "credit_line": "Wilson's snipe winnowing by <author>, CC BY 4.0"
    },
    {
      "asset_id": "tex_tamarack_log",
      "files": ["assets/yearofice/textures/block/tamarack_log_side.png", "assets/yearofice/textures/block/tamarack_log_top.png"],
      "source_type": "generated",
      "recipe": "tools/artgen/recipes/block/tamarack_log.json",
      "licence": "project",
      "generator_version": "artgen 0.3.0"
    }
  ]
}
```

**Allowed values.** `source_type` is one of `generated`, `generative_model`, `texture_library`, `model_library`, `nature_sound_archive`, `agency_recording`, `game_asset_collection` or `permission`. `licence` is one of `project`, `CC0-1.0`, `CC-BY-3.0`, `CC-BY-4.0`, `CC-BY-SA-3.0`, `CC-BY-SA-4.0`, `CC-BY-NC-4.0`, `CC-BY-NC-SA-4.0`, `MIT`, `BSD-2-Clause`, `BSD-3-Clause`, `Zlib`, `OFL-1.1` or `permission`; an SA or NC entry adds `release_blocker: true` (RECONCILE 151); a generative-model entry adds `model`, `prompt` and `seed`.

### The provenance check in CI

**`tools/provenance` runs inside `contentLint`.** It fails the build on any of these.

1. A file under `assets/yearofice/` or the look pack with no `credits.json` entry.
2. An entry whose file is missing, or a file listed twice.
3. A licence outside the allowed list, or a found asset without its licence file or original.
4. An original whose hash does not match its entry.
5. A CC-BY entry without a credit line.
6. Any file under `assets/minecraft/` that is not `artgen` output. Any file whose hash matches a vanilla asset in the local client jar, checked wherever the jar is present.

## Sound

**Most sounds are synthesized; animal calls are found first.** Wind, water, fire, footsteps, ice, tools, body sounds and the UI come from recipes, because their timing and range must be exact. Animal calls come from CC0, CC-BY and public-domain recordings where good ones exist. Every species also has a synthesized fallback, so no event is ever silent.

**`tools/soundgen/` renders at build time.** Minecraft plays only Ogg files, so nothing is synthesized at runtime. The prototype renders the four snow footstep bands and a wind loop through the full chain in 6 s on one core.

### Synthesis recipes

**A recipe names a kind and its numbers.** Kinds are small Python functions over NumPy: filtered noise, resonators, glides and envelopes. Recipes live in `tools/soundgen/recipes/<group>/<name>.json`.

```json
{ "id": "yearofice:step.snow_squeak", "kind": "footstep", "variants": 4, "length_s": 0.4, "lufs": -18,
  "body_hz": 800, "body_q": 1.2, "decay_s": 0.04,
  "grain_hz": 4000, "grain_density": 0.02, "grain_gain": 0.6,
  "squeak_hz": 1150, "squeak_s": 0.09, "squeak_gain": 0.35, "highpass_hz": 100 }
```

| Group | Kind | How it is built | Key numbers |
| --- | --- | --- | --- |
| Wind | `wind` | Pink noise through a band-pass whose centre and level follow a seeded gust walk; low-pass rumble under it; 2 s crossfade for a seamless loop | Centre 250–900 Hz (breeze) to 150–1,600 Hz (blizzard); gust floor 0.25 |
| Water | `water` | Many short band-passed noise grains (bubbles) with random pitch 300–3,000 Hz over a noise bed | Grains per second: creek 120, river 400, lake lap 6 |
| Fire | `fire` | Crackle: Poisson clicks through a 2–6 kHz resonator; roar: low-passed noise at 80–300 Hz; pops: single decaying resonator hits | Clicks per second: small 6, steady 14, roar 30 |
| Footsteps | `footstep` | Heel and toe noise bursts 60–110 ms apart through a body band-pass, plus grain clicks and, in the cold bands, a gliding squeak tone | Body 450–900 Hz; grain density 0.004 (soft) to 0.03 (crunch); squeak 1,150 Hz at −15 °C, 1,500 Hz below −30 °C |
| Ice | `ice_crack` | A broadband click into a dispersive chain (a falling chirp from 4 kHz to 200 Hz over 0.3–1.5 s), plus a low boom for big sheets | Chirp length scales with crack length; boom 40–80 Hz |
| Axe and saw | `tool_hit`, `saw` | Axe: an impulse into two wood resonators (log body 120–250 Hz, blade 2–3 kHz) with a 30 ms noise chip; saw: band-passed noise modulated at the stroke rate | Chop decay 0.15 s; saw stroke 0.6 s, teeth buzz 1.8–3.5 kHz |
| Body | `body` | Breath: shaped noise through 2 formants; heartbeat: two low thumps; chew: short clicks; teeth chatter: 12 Hz click train | Breath rate from the body tick |
| UI and messenger | `ui` | Sine and triangle tones with short envelopes, in one key (D) so they never clash | 2–3 notes, under 400 ms |
| Plants, traps, decay | `foley` | Snaps, rustles and thuds from noise grains and resonators | Per event |

**Twenty-two of these ids are new.** The engine tab's unnamed "60 rendered sounds" get these names: 5 `weather.wind_*`, 5 `fire.*`, 4 `work.*`, 3 `ice.*` and 5 `water.*` loops and splashes. They join `studio/inventory.json` in the M2 ticket, which makes the total 409 events.

### Animal calls

**Found recordings first, synthesis as the floor.** Sources are the CC0 and CC-BY nature-sound archives and agency recordings; each call event also names a synth fallback recipe. The fallback ships in M5 or the species' milestone; a found recording replaces it by file swap when one passes review.

| Animals | Events | Expected found share | Synth fallback |
| --- | --- | --- | --- |
| Birds, 78 species | 83 | About 90% | By call type, below |
| Big game: moose, caribou, sheep, goat, bison, muskox | 22 | About 70% | Grunt, bleat, bellow; `clash` as two bone resonators |
| Predators: wolf, coyote, foxes, bears, lynx, wolverine | 24 | About 60% | Howl glide, bark, huff, growl, yowl |
| Small game and furbearers | 14 | About 50% | Chatter, whistle, thump, scream |
| Voles, shrews, lemmings, jumping mouse | 6 | 0% | Short squeaks and twitters at 3–6 kHz |
| Frogs and toad | 3 | About 70% | Duck-like clack for wood frog; trill for toad |
| Insects | 4 | 0% | Buzz and drone recipes |
| Fish and water mammals | Splashes, snorts, tail slaps | 0% | Splash and slap recipes |

**Synth fallbacks by call type.** Whistles are sine glides with light FM; trills are tones with 15–40 Hz amplitude modulation; thrush flutes are two detuned sines with breath noise. Honks and quacks are sawtooth pulses through 2 formants; hoots are 200–450 Hz sines; drums are accelerating 40–80 Hz thumps.

**Mammal fallbacks.** Howls glide 300–700 Hz with 5 Hz vibrato, layered 3–6 voices for a chorus. Grunts and bellows are 60–150 Hz pulse trains through a vocal formant. Huffs are low-passed noise bursts, and chatter is a 10–20 Hz click train.

**Legend calls are pitched variants.** One-Eye's huff and the Black Wolf's howl are the species files shifted down 3 semitones with a longer tail. They need no extra source.

### The processing chain

**Every sound, found or synthesized, goes through one chain.** `soundgen` runs it; found files enter at step 2.

1. Render or import at the source rate, in 32-bit float.
2. Remove DC; high-pass at 30–300 Hz by group; resample to 44.1 kHz.
3. Mix to mono. Ambience beds are the one exception: they stay stereo, because they are not positioned, as Player systems and client › The Audio bus already rules.
4. Trim silence to under 20 ms at the head; 5 ms fade-out on one-shots.
5. **Loops.** Minecraft repeats a whole file, with no loop points inside it. So a loop file is cut at zero crossings, and its last 2 s are crossfaded into its head. The end then flows into the start without a click.
6. **Loudness.** Normalise to the group's target below, measured as EBU R128 integrated loudness. Clips shorter than 1 s are measured padded with silence to 1 s, because R128 gates in 400 ms blocks.
7. **Peak.** True peak at or below −1 dBFS. Peak wins: a clip that cannot reach its target under the peak limit may sit up to 3 LU below it.
8. **Encode.** Ogg Vorbis at quality 5 for mono one-shots (about 80 kbps); quality 0 for stereo beds (about 64 kbps).
9. **Variants.** 2–4 files per one-shot event, chosen at random by `sounds.json`; ±5% pitch from the bus on top.

**Loudness targets (LUFS).** Danger cues −14, always 6 dB or more above the bed. Animal calls −16, with range set by the event volume. Footsteps run from −20 on soft snow to −16 below −30 °C.

**The rest.** Tools −17, UI and messenger −20, body −22, weather loops and beds −23, music −24.

**The prototype meets these.** Its 8 footstep files land within 2.4 LU of target, peaking at or below −0.9 dBFS before encoding. The wind loop hits −23.0 LUFS.

**Size budget.** Beds are 60 s loops: 64 beds at 64 kbps come to about 31 MB. One-shots and spot loops add about 8 MB, and music about 6 MB, so the sound folder stays under 45 MB.

### Every sound group and its source

| Group | Events | Source |
| --- | --- | --- |
| `entity.*` species calls | 156 | Found first, synth fallback |
| `step.*` | 31 | Synth |
| `body.*` | 28 | Synth |
| `call.*` player calls | 21 | Synth (they are human imitations) |
| `water.*` spot sounds | 12, plus 5 new loops and splashes | Synth |
| `ambient.*` (insect drones, mobbing) | 11 | Synth |
| `ambient.bed.*` and indoor, dawn chorus, under water | 69 | Synth beds now; the dawn chorus mixes found bird calls |
| `world.*` | 9 | Synth |
| `weather.*` | 8, plus 5 new wind loops | Synth; thunder from noise and a 30 Hz rumble |
| `plant.*` | 8 | Synth foley |
| `trap.*` | 7 | Synth foley |
| `messenger.*`, `ui.*` | 11 | Synth tones |
| `item.*`, `fishing.*`, `work.*`, `build.*`, `tree.*`, `fire.*`, `ice.*`, `decay.*`, `bug.*` | 16, plus 12 new fire, work and ice ids | Synth |
| Music | 12 pieces | Generative, rendered |

**New sound ids (RECONCILE 170, 171).** Look owns them all; each ships a synth fallback first.

- **Encounter, M4 (player):** `body.breath_panic` (fast breath through the nose), `body.hit_grunt`, `body.bite_crunch` (muffled), `item.pack_tear` (fabric tearing on the first bite).
- **Encounter, M6 (animals):** `entity.grizzly.jaw_pop`, `entity.black_bear.huff` and `entity.black_bear.jaw_pop`, beside the existing `entity.grizzly.huff`. The Director's names `animal.bear.huff`, `animal.bear.jaw_pop`, `player.breath_panic`, `player.hit_grunt` and `gear.pack_tear` map onto these, following the `entity.<species>.*`, `body.*` and `item.*` groups above.
- **Video 2:** `carry.box_lift`, `carry.box_set`, `ui.field_note_card` (M3); `work.crust_scrape`, `work.pan_swirl` (M5); `work.sluice_wash` (M9).

**New sound ids, research round B (Proposed; Ruhan may veto; RECONCILE 175).** Look owns them all; each ships a synth fallback first, in the existing groups. B7's `voice.talk_walk` is named `body.talk_walk` here, since the groups have no `voice.*`.

- **M4 (body):** `body.skin_sting` (B13), `body.back_twinge` (B15, played with the existing `body.grunt`).
- **M6:** `work.bone_crush` (B5), `body.talk_walk` (B7, with `body.hum` as one variant).
- **M7 (travel and ice):** `step.skate`, `ice.blade_ring`, `ice.blade_sing`, `ice.blade_crackle` (B1); `step.ski_glide`, `step.ski_squeal` (B2).
- **M8:** `world.frost_drip`, `world.icicle_crack` (B8).

### Music tied to Spirits

**Music is generated offline and plays rarely.** Twelve pieces of 1–3 minutes come from `tools/soundgen/music/`: four seasons, each with a dawn theme, a day piece and a fire piece. A seeded phrase grammar writes modal lines (Dorian and Aeolian, 50–70 beats a minute). Synthesized plucked string, bowed drone and wooden flute play them.

| Spirits | Music | Humming |
| --- | --- | --- |
| Bright (over 70) | The season's dawn theme at dawn; day pieces with a gap of 15–30 real minutes | The survivor hums while working (`body.hum`), using the same melody generator through a voice formant |
| Steady (30–70) | Day pieces only, gap 25–45 real minutes | None |
| Low (under 30) | Only the fire piece, only within 4 m of a lit fire | None; footsteps and breathing are louder, as the Blueprint says (the bus adds 2 dB) |

**Danger silences music.** Any danger cue or the close clock fades music out over 3 s, and none starts again for 5 real minutes. Vanilla music is replaced through `SelectMusicEvent`, as Player systems and client sets.

## Asset budget per milestone

### The placeholder rule

**Every asset carries a grade.** The grade sits in the file's `.prov.json` sidecar and in the CI gallery.

| Grade | What it is | Made by |
| --- | --- | --- |
| G0, placeholder | The rig preset or a generic recipe with the species' or material's main ramp: right size, silhouette class and palette; one variant | `artgen` and `modelgen` defaults, no species tuning |
| G1, generated | The species or block recipe with all variants, animations and sounds; passes every check | The owning department, through the tools |
| G2, approved | G1 that the Director passed in the gallery against the look tests | Director sign-off |

**The rule.**

1. An id may ship at G0 only in a milestone before its own, behind its content flag.
2. In its own milestone an id ships at G1 or better.
3. By the end of the next milestone it is G2, or it carries a ticket and a date.
4. Never a missing texture: no pink-and-black pixel in any shot, and the linter fails an unresolved texture path.
5. Never a silent event: a G0 sound is a synth fallback, never an empty file.
6. Never a vanilla texture as a stand-in.

**The vertical slice is all G2.** M2.5 is the slice before M5 that Director item 106 asks for.

**What the slice holds.** This tab assumes it holds one valley region, the start site and fire, and raw and cooked meat (request 82). Its animals are moose, snowshoe hare and willow ptarmigan. Its art is finished, not placeholder, so the slice answers "does it look right" as well as "does it play".

### Counts per milestone

**Cumulative counts, checked against the master inventory.** Mob counts are exact: each of the 153 inventory mobs and the 2 deer (155 in all) was mapped to its rig and milestone from Entities and AI › The entity registry. The other columns end on the inventory totals; their split across milestones is this tab's plan.

| Milestone | Mobs | Rigs | Entity texture sets | Plant blocks | Other blocks drawn | Item icons | HUD layers | Sound events | Music |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| M0 Seeded world | 0 | 0 | 0 | 0 | 24 | 0 | 0 | 0 | 0 |
| M1 Water and set pieces | 0 | 0 | 0 | 0 | 46 | 0 | 0 | 10 | 0 |
| M2 Walkable valley | 0 | 0 | 0 | 20, `leaf` phase only | 66 | 0 | 0 | 54 | 0 |
| M2.5 Vertical slice | 3 | 3 | 3 | 20, all phases | 78 | 20 | 9 | 85 | 0 |
| M3 Start site, fire and onboarding | 3 | 3 | 3 | 20 | 90 | 50 | 10 | 100 | 3 |
| M4 Body | 3 | 3 | 3 | 20 | 96 | 65 | 13 | 118 | 3 |
| M5 Food and goals | 39 | 13 | 46 | 107 | 120 | 140 | 13 | 183 | 3 |
| M6 Danger and big game | 139 | 32 | 167 | 107 | 140 | 190 | 14 | 313 | 3 |
| M7 The year | 152 | 34 | 182 | 107, with `buried` | 160 | 210 | 15 | 400 | 12 |
| M8 Building and outposts | 152 | 34 | 182 | 107 | 176 | 220 | 15 | 405 | 12 |
| M9 Homestead and beyond | 155 | 35 | 192 | 107 | 176 | 226 | 15 | 409 | 12 |

**Where the mob counts come from.** M2.5 pulls `moose`, `snowshoe_hare` and `willow_ptarmigan` forward. M5 adds 36: 11 small mammals and furbearers, the wood frog, 4 grouse and ptarmigan, 20 fish, `yukon_floater` and `yellowjacket`. M6 adds 100: 7 big-game species (the two deer behind the flag yearofice:deer), 11 predators, 9 other mammals, the diving beetle and 73 birds.

**The last two steps.** M7 adds 13: lemmings, singing vole, bat, toad, sockeye, pygmy whitefish, trout-perch, lamprey and 3 critters. M9 adds goat, reindeer and hen.

**HUD layers by milestone.** The slice has `prompt_line`, `pickup_line`, `work_ring`, `load_meter`, `carry_slot`, `messenger_line`, `vitals_strip`, `frost` and `captions`. M3 adds `minigame` for the bow drill, and M4 adds `body_figure`, `body_fx` and `breath_ring`. M6 adds `threat_flash`; M7 adds `headwear` for the head net.

**What the slice's 85 sounds are.** It keeps the 54 from M1 and M2: water loops, world sounds, 31 steps, 5 wind loops and 8 summer beds. It adds 9 calls, 10 body sounds, 5 fire, 4 tool and 3 carcass sounds.

**M6 is the largest step.** It adds 100 mobs, but 73 are birds on 14 shared rigs. Most of its work is species files and paint maps, not new templates.

**Additions from video 2 and the encounter clips (RECONCILE 170, 171).** These come on top of the counts table above.

| Milestone | Item icons | Other blocks drawn | HUD layers | Sound events | Clips |
| --- | --- | --- | --- | --- | --- |
| M2 | — | — | — | — | The pack-outline GUI sprite (A9) |
| M3 | — | — | +1, `field_note_card` | +3: `carry.box_lift`, `carry.box_set`, `ui.field_note_card` | — |
| M4 | +1, `large_pack_frame` | — | — | +4: `body.breath_panic`, `body.hit_grunt`, `body.bite_crunch`, `item.pack_tear` | 5 player hand clips |
| M5 | +4: `lick_crust`, `lick_salt`, `black_sand`, `birch_pan` | +3: `drying_rack`, `smoke_rack`, `placer_gravel` | — | +2: `work.crust_scrape`, `work.pan_swirl` | — |
| M6 | +1, `trophy_plaque` | +2: `theft_cache`, `trophy_mount` | — | +3: `entity.grizzly.jaw_pop`, `entity.black_bear.huff`, `entity.black_bear.jaw_pop` | 25 animal clips: grizzly 8, black bear 9, moose 4, wolf and coyote 2 each |
| M9 | — | +1, `sluice_box` | — | +1, `work.sluice_wash` | — |

## Pipeline and checks

### Generators inside datagen

**`./gradlew artgen modelgen soundgen` run before datagen, and their output is committed.** The three are `Exec` tasks that call the Python tools. `runData` depends on all three, so one datagen run rebuilds every asset. Output lands in `src/generated/resources/`, which Content pipeline already commits and checks with `checkDatagen`.

**What each writes.** `artgen` writes block, item, entity and GUI textures plus plant blockstates and models (about 40 s for all). `modelgen` writes `geo/entity/`, the rig animation files and entity textures (about 20 s). `soundgen` writes the Ogg files, the generated `sounds.json` entries and subtitle keys (about 3 min cold).

**The check.** `provenance` checks the lot in under 5 s.

**Incremental by hash.** Each task keeps `build/assetgen/<task>.hashes.json`: the SHA-256 of each recipe, the palette and the tool version. Only changed recipes rerun, so a one-species change takes seconds.

**Ogg files are checked by their source, not their bytes.** Vorbis encoders differ by platform, so `checkDatagen` compares the hash of the WAV before encoding, stored in the sidecar, not the `.ogg` itself. PNGs are compared byte for byte.

**The toolchain is pinned.** `tools/requirements.txt` pins Python 3.12, Pillow and NumPy; the Gradle task builds a virtual environment in `build/venv` once. FFmpeg with libvorbis is the one system tool. Blockbench is never needed for a build.

### Linter checks

**These join the content linter's 15 checks in Content pipeline, numbered on from them.**

16. Every texture a model, blockstate or item model names exists; no unresolved path anywhere.
17. Every opaque pixel is a palette entry; at most 3 ramps plus outline per texture; at most 12 colours.
18. Block faces tile seamlessly: the difference between opposite edges is no larger than inside the face.
19. Every geometry passes `modelgen --check` (bones, cube budget by tier, UVs, hitbox fit, animation bones).
20. Every entity atlas is within its tier's size; total entity texture memory under 8 MB.
21. Every plant block has a model for each (`phase`, `taken`) pair it can reach, and every sprite it names.
22. Every `.ogg` is mono 44.1 kHz (stereo only under `ambient/bed/`), true peak at or below −1 dBFS, and loudness within its group's window.
23. Every asset file has exactly one `credits.json` entry and a grade in its sidecar; the grade meets the placeholder rule for the current milestone.
24. No file under `assets/minecraft/` except look-pack `artgen` output.

### Golden images and limits

**The screenshot script gains four art boards.** Content pipeline's 12 shots stay. `tools/shots/script.json` adds four fixed scenes built by `/yoi art` commands, so they never depend on spawning luck.

| Board | What it shows | Pass rule |
| --- | --- | --- |
| Zoo | Every species of the current milestone in a lineup on snow and on summer ground, side and three-quarter view, at its tier's check distance | Similarity 0.90 or more to the golden image per tile; no missing-texture pixel |
| Plant wall | Each plant in each phase and `taken` 0 and 3, in rows | Similarity 0.92 or more per tile |
| Icon sheet | Every item icon at GUI scales 2 and 4, plus each food item's five decay stages | Similarity 0.95 or more; every icon's silhouette area at least 30 px |
| Biome strip | 20 blocks of one biome in a 5 × 4 patch, at noon and dusk, summer and winter | Similarity 0.92 or more; mean luminance within 10% of the golden |

A failed board marks the pull request "look needed", as the existing screenshot checks do. The Director approves new golden images in the gallery.

**Limits per entity tier.** The cube, bone and atlas caps are the style guide's. On top, at most 60 tiny, 40 small, 24 medium, 16 large and 8 huge models draw at once. Groups beyond 96 m become impostors, as Entities and AI sets.

**Impostors are generated too.** `modelgen` renders an 8-direction sprite sheet per species and stage for herds, flocks and schools.

**The frame budget for models.** With every tier at its cap, entity rendering stays under 3 ms a frame on the CI GPU runner. That fits inside Content pipeline's 60 fps gate.

## Prototypes

**Three prototypes prove the pipeline before M2.5.** Each runs the real tools on real recipes and ends with a look test the Director scores pass or fail. Numbers are checked by script; the Director's calls are yes-or-no questions with a written reason.

### A1: moose, hare and ptarmigan, generated

**Goal.** Prove that `modelgen` and `artgen` make animals that read as themselves with no hand work. The three are the slice's animals, on three different templates.

**Steps.** Write the three species files and ramps, then run `./gradlew modelgen artgen`. Build the zoo board with `/yoi art zoo slice`: two views, summer ground and snow, every coat and animation.

| Test | Pass |
| --- | --- |
| Checks | All 7 `modelgen` checks pass; 0 off-palette pixels |
| Budget | Moose at most 80 cubes, hare and ptarmigan at most 24 |
| Scale | Moose shoulder 1.9 m ± 10% beside the player; hare under 0.35 m tall sitting |
| Feet | Foot slide under 0.1 block per walk cycle at the species' walk speed |
| Loops | Every looping animation starts and ends within 0.5° on every bone |
| Camouflage | Winter hare and winter ptarmigan on snow: body-to-ground mean luminance gap 0.08 or less. Summer coats on snow: 0.30 or more |
| Director: silhouette | From black-filled silhouettes at the check distance, the Director names all three species, and picks the moose out of a lineup with the caribou and muskox presets |
| Director: motion | The moose's trot reads as a moose (high-stepping, head level), not a horse; the hare bounds; the ptarmigan flushes with whirring wings |

**Fail means** a template or gait rule changes, never a hand-edited model. A Blockbench override is allowed only after two failed template fixes, with a written reason.

### A2: 20 block textures for one biome

**Goal.** Prove the palette and the block rules give a world that looks like interior Alaska spruce flats, not vanilla taiga. The 20 are listed here.

- Wood: `white_spruce_log`, `black_spruce_log`, `birch_log`, `aspen_log`, `tamarack_log`, `tamarack_leaves`, `snag_log`, `punk_log`, `rotten_stump`, `fatwood_stump`.
- Ground: `root_plate`, `cone_midden`, `snow_cover`, `frozen_soil`, `mud`, `path`, `face_rock`.
- Carpets: `feather_moss`, `stair_step_moss`, `map_lichen`.

**Steps.** Run `./gradlew artgen` on the 20 recipes. Build the strip and a 64 × 64 m patch with `/yoi art biome spruce_flats`, then shoot noon and dusk in summer and winter.

| Test | Pass |
| --- | --- |
| Palette | 0 off-palette pixels; at most 3 ramps per texture |
| Tiling | A 3 × 3 tiling of each face at 1080p shows no seam: edge difference no larger than the face's own pixel-to-pixel difference |
| Contrast | No block face spans more than 3 ramp steps |
| Muting | The noon summer frame's mean chroma is at most 70% of a vanilla taiga frame shot from the same camera |
| Director: logs | From 4 m, the Director names all 5 live log kinds correctly |
| Director: place | Asked "where is this?", the answer is "boreal forest in Alaska or the Yukon", not "Minecraft taiga" |

### A3: footsteps and wind, synthesized

**Goal.** Prove synthesis can carry the most-heard sounds in the game. The prototype in this tab already renders the four snow bands and a wind loop; A3 runs them in game through the Audio bus.

**Steps.** Render the 31 `step.*` events and 5 wind loops. Walk 200 m on each snow band (set with `/yoi weather temp`) and on duff, moss, gravel, ice and bog. Then stand through a 5-minute wind ramp from calm to blizzard.

| Test | Pass |
| --- | --- |
| Chain | Every file mono 44.1 kHz, true peak at or below −1 dBFS, loudness within its window |
| Bands | The squeak bands show a tonal peak in 900–2,000 Hz at least 4 times the band's mean; soft and crunch bands do not |
| Seams | 10 consecutive wind loops show no click: level step at the seam under 1 dB |
| Size | All 31 step events with 4 variants each under 1 MB of Ogg |
| Director: bands | Played the four snow bands in a shuffled order twice, the Director orders them from warm to cold correctly both times |
| Director: fatigue | After 20 real minutes of walking and wind, nothing clicks, repeats audibly or grates |

**What passing buys.** With A1–A3 green, M2.5 can be built entirely from tools, and the same three tools carry every later milestone.

## Data and build

### Paths

- `tools/artgen/` (`artgen.py`, `palette.json`, `forms/`, `recipes/<kind>/<id>.json`), `tools/modelgen/` (`templates/`, `species/<id>.json`, `overrides/`) and `tools/soundgen/` (`recipes/<group>/<name>.json`, `music/`).
- `tools/import-asset` and `tools/provenance`; found originals in `assets-src/found/<asset_id>/` (Git LFS) and licence texts in `licenses/`.
- Output in `src/generated/resources/assets/yearofice/`, committed; `assets/yearofice/credits.json`, generated and checked. Only `build/assetgen/` hashes stay uncommitted.

### Code

**Little runtime code: the work happens at build time.**

| Class | Owns |
| --- | --- |
| `com.yearofice.datagen.GeneratedAssetsProvider` | Reads the tools' manifests; writes `sounds.json` entries, subtitle keys and plant blockstates into datagen output |
| `com.yearofice.client.audio.MusicDirector` | Picks music by Spirits, season and time; silences it on danger; hooks `SelectMusicEvent` |
| `com.yearofice.client.screen.CreditsScreen` | Lists `credits.json` credit lines, opened from the title screen and the pause menu |
| `com.yearofice.debug.ArtCommands` | The `/yoi art` and `/yoi sound` commands below |

### Debug commands and tuning

**Every art and sound system has `/yoi` commands**, as Ruhan's rules require (RECONCILE 118).

| Command | Does |
| --- | --- |
| `/yoi art zoo <milestone or rig>` | Builds the zoo board in front of the player |
| `/yoi art plants <id or all>` | Builds the plant wall, every phase and `taken` 0–3 |
| `/yoi art icons` | Opens the icon sheet screen with decay stages |
| `/yoi art biome <cover>` | Builds the 5 × 4 biome strip and a 64 m test patch |
| `/yoi art season <stage>` | Forces the client's season stage for remaps and tints |
| `/yoi art grade` | Lists assets in view with their grade and source type |
| `/yoi sound play <id> <range_m>` | Plays an event at a range, through the bus and captions |
| `/yoi sound bed <cover> <season> <day or night>` | Forces an ambience bed |
| `/yoi sound music <piece or off>` | Plays or stops a music piece, ignoring Spirits |

**Tuning lives in `yearofice-common.toml`.** The `[audio]` section holds `music_gap_min_real_min` (Bright 15, Steady 25), `music_gap_max_real_min` (30, 45), `danger_music_silence_real_min` (5), `bed_crossfade_cover_s` (4), `bed_crossfade_time_s` (8), `indoor_bed_drop_db` (12) and `low_spirits_step_boost_db` (2). The Gentle, Standard and Brutal presets leave audio unchanged. Art has no runtime tuning: looks change only through recipes.

### Registry entries this tab adds

**Sound events (22), to `studio/inventory.json` at M2 and M2.5.** `weather.wind_calm`, `weather.wind_breeze`, `weather.wind_strong`, `weather.wind_blizzard`, `weather.wind_gust`, `water.creek_loop`, `water.river_loop`, `water.lake_lap`, `water.splash_small`, `water.splash_large`, `fire.crackle_small`, `fire.crackle_steady`, `fire.roar`, `fire.pop`, `fire.hiss_wet`, `work.axe_chop`, `work.axe_split`, `work.saw_stroke`, `work.knife_cut`, `ice.crack_near`, `ice.crack_far`, `ice.boom`.

**Music (12).** `music.<season>_<dawn, day or fire>` for the four seasons, registered as sound events in the `music` category; 3 at M3 and the rest at M7.

**No new blocks, items or entities.** `moose_quarter` is a carry-load texture, not an item.

### Milestones

| Milestone | Ships |
| --- | --- |
| M0 | The three tools, the palette, the linter checks 16–24, the provenance check, `credits.json`, the Gradle tasks; terrain block recipes |
| Before M2.5 | Prototypes A1, A2 and A3, each passed by the Director |
| M2.5 | The slice at G2: 3 species, the 8 slice plant blocks with all phases (Minecraft engine › M2.5, RECONCILE 141), 78 blocks, 20 icons, 9 HUD layers, 85 sounds |
| M3 | Credits screen, the first 3 music pieces, the minigame HUD art |
| M5–M9 | Each department's assets through the same tools, per the budget table |

### Tests

**Five numeric checks, run as JUnit tests on the tools' output and as GameTests where the game is needed.**

1. **`artgen_deterministic`.** Running `artgen` twice on all recipes gives byte-identical PNGs: 0 differing files.
2. **`palette_clean`.** Over every generated texture, off-palette pixels = 0 and colours per texture ≤ 12.
3. **`cube_budget`.** For every geometry, cubes ≤ the tier cap (12, 24, 40, 60, 80) and the hitbox fit is within 10%.
4. **`sound_levels`.** Every `.ogg` peaks at or below −1 dBFS, with loudness from 3 LU under to 2 LU over its target. Every non-bed file is mono at 44.1 kHz.
5. **`credits_cover_all`** (GameTest batch `m0`). Every loaded `yearofice` asset file has one `credits.json` entry: 0 missing, 0 duplicates. The block and entity atlases hold 0 missing-texture sprites after load.

### Settled across tabs

| Item | Was | Now |
| --- | --- | --- |
| Engine art budget | 120 block textures, 60 icons, 20 animal models | 176 drawn blocks, 107 plants (about 2,100 sprites), 226 icons, 35 rigs, 192 texture sets |
| Sound tool path | `tools/render-sounds`, recipes in `tools/sounds/` | `tools/soundgen/` |
| Adding a species, step 4 | Blockbench by hand; `geo/<id>.geo.json`, `textures/entity/<id>.png`, `rig/rodent.json` | `modelgen` from `species/<id>.json`; paths as Entities and AI sets (`geo/entity/`, `textures/entity/<id>/`); Blockbench as fallback |
| Sound credits | `assets/yearofice/sounds/credits.json`, own recordings or CC0 only | One `assets/yearofice/credits.json`; CC0, public domain and CC-BY allowed (RECONCILE 116); CC-BY-SA and CC-BY-NC(-SA) too for personal use, flagged release\_blocker (RECONCILE 151) |
| Sound format | Sources at 48 kHz | Any source rate; output 44.1 kHz mono, beds stereo |
| Bed length | 60–120 s | 60 s, to keep beds near 31 MB |
| Foot bone | "hoof" and "paw" | `foot` on every quadruped |
| Critter entities | 7 in Entities and AI | 5 in the inventory: `bumblebee` and `dragonfly` are particles there; the critter rig serves 5 until the Director settles it |
| Sound event total | About 220 in the brief | 409 |
| Release art | Store pages and a public README (Content pipeline) | Dropped by RECONCILE 119; credits stay for the licences |
