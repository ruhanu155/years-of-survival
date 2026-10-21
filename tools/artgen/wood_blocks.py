"""wood_blocks: the chopping block's blockstate, block models and item model (M2 felling, M3 workbench; Crafting and building in
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
    # The workbench (M3 stations): a split-log top on four posts, on the chopping block's generated textures.
    def box(a, b, up="#s"):
        f = {d: {"texture": "#s"} for d in ("north", "south", "east", "west", "down")}
        f["up"] = {"texture": up}
        return {"from": a, "to": b, "faces": f}
    bench = {"parent": "minecraft:block/block",
             "textures": {"particle": "yearofice:block/chopping_block_side", "s": "yearofice:block/chopping_block_side",
                          "e": "yearofice:block/chopping_block_top"},
             "elements": [box([0, 10, 1], [16, 14, 15], "#e"), box([1, 0, 2], [4, 10, 5]), box([12, 0, 2], [15, 10, 5]),
                          box([1, 0, 11], [4, 10, 14]), box([12, 0, 11], [15, 10, 14])]}
    return {"blockstates/workbench.json": {"variants": {"": {"model": "yearofice:block/workbench"}}},
            "models/block/workbench.json": bench,
            "models/item/workbench.json": {"parent": "yearofice:block/workbench"},
            "blockstates/chopping_block.json": {"variants": {"stump=false": {"model": "yearofice:block/chopping_block"},
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
