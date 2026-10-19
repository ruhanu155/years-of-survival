"""cooking: writes the yearofice:spit blockstate and block model (G0 blockout) and the item models of the slice's
foods whose look changes as they age and cook: snowshoe_hare_meat, willow_ptarmigan_meat and arctic_grayling.

The spit is two forked sticks standing in the fire ring below (they reach 12 px down into the ring's block) and a
crossbar 5 px up, where client.food.SpitRenderer turns the food (Blueprint > Cooking: spit or stick roast).
Each food model switches on the item property yearofice:look (client.food.FoodClient): 0 raw, 0.25 raw aging,
0.5 raw turning or worse, 0.75 cooked, 1 burnt. The sprites are the art wave's (raw_hare, cooked_hare...) and
artgen's ramp-remap variants (cooked_hare_burnt, arctic_grayling_cooked...). Textures are vanilla references,
never copies.
Usage (from the repository root): python3 -I tools/artgen/cooking.py"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "cooking 0.1.0"
STICK = "minecraft:block/stripped_spruce_log"

# item -> sprites for (raw, aging, spoiled, cooked, burnt); None reuses the raw sprite
FOODS = {
    "snowshoe_hare_meat": ("raw_hare", "raw_hare_aging", "raw_hare_spoiled", "cooked_hare", "cooked_hare_burnt"),
    "willow_ptarmigan_meat": ("raw_grouse", "raw_grouse_aging", "raw_grouse_spoiled", "cooked_grouse", "cooked_grouse_burnt"),
    "arctic_grayling": ("arctic_grayling", None, None, "arctic_grayling_cooked", "arctic_grayling_burnt"),
}
LOOKS = (("aging", 0.25), ("spoiled", 0.5), ("cooked", 0.75), ("burnt", 1.0))


def cube(frm, to, tex="stick"):
    return {"from": frm, "to": to, "faces": {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "up", "down")}}


def spit_model():
    els = []
    for x in (2.5, 12.5):
        els.append(cube([x, -12, 7.5], [x + 1, 5, 8.5]))           # upright, footed in the ring
        els.append(cube([x - 1, 5, 7.5], [x, 7, 8.5]))             # fork, outer tine
        els.append(cube([x + 1, 5, 7.5], [x + 2, 7, 8.5]))         # fork, inner tine
    els.append(cube([0.5, 5, 7.6], [15.5, 5.8, 8.4]))             # the crossbar the food turns on
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": STICK, "stick": STICK}, "elements": els}


def item(sprite):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"yearofice:item/{sprite}"}}


def files():
    out = {"blockstates/spit.json": {"variants": {"": {"model": "yearofice:block/spit"}}},
           "models/block/spit.json": spit_model()}
    for name, sprites in FOODS.items():
        base = item(sprites[0])
        overrides = []
        for (look, value), sprite in zip(LOOKS, sprites[1:]):
            sub = f"{name}_{look}"
            out[f"models/item/{sub}.json"] = item(sprite or sprites[0])
            overrides.append({"predicate": {"yearofice:look": value}, "model": f"yearofice:item/{sub}"})
        base["overrides"] = overrides
        out[f"models/item/{name}.json"] = base
    return out


def outputs():
    return ["assets/yearofice/" + k for k in files()]


if __name__ == "__main__":
    for rel, doc in files().items():
        p = os.path.join(ASSETS, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w") as f:
            json.dump(doc, f, indent=1)
            f.write("\n")
    print(f"{VERSION}: wrote {len(files())} files")
