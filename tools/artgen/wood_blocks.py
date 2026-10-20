"""wood_blocks: the chopping block's blockstate, block models and item model (M2 felling; Crafting and building in
depth > Trees, felling and firewood > Splitting). A round set on end, 12 px across and 10 high, with the generated
bark and ring textures (recipes/block/chopping_block.json); on a stump (`stump=true`) it keeps the stump's model.
Usage (from the repository root): python3 -I tools/artgen/wood_blocks.py"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "wood_blocks 0.1.0"


def files():
    faces = {f: {"texture": "#s"} for f in ("north", "south", "east", "west")}
    faces["up"] = {"texture": "#e"}
    faces["down"] = {"texture": "#e"}
    block = {"parent": "minecraft:block/block",
             "textures": {"particle": "yearofice:block/chopping_block_side", "s": "yearofice:block/chopping_block_side",
                          "e": "yearofice:block/chopping_block_top"},
             "elements": [{"from": [2, 0, 2], "to": [14, 10, 14], "faces": faces}]}
    return {"blockstates/chopping_block.json": {"variants": {"stump=false": {"model": "yearofice:block/chopping_block"},
                                                             "stump=true": {"model": "yearofice:block/stump"}}},
            "models/block/chopping_block.json": block,
            "models/item/chopping_block.json": {"parent": "yearofice:block/chopping_block"}}


def outputs():
    return ["assets/yearofice/" + k for k in files()]


if __name__ == "__main__":
    for k, v in files().items():
        p = os.path.join(ASSETS, k)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump(v, open(p, "w"), indent=1)
    print(len(files()), "files")
