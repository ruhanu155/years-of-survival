"""bough_bed: writes the yearofice:bough_bed blockstate and its two block models (G0 placeholder).

The bed is a 1 x 2 mat of laid spruce boughs, 25 cm high, on two side poles (Blueprint > Making camp: "40 spruce
boughs on a pole frame"). The models face north (the head toward -Z); the blockstate turns them by `facing`. The
mat uses the artgen recipe textures (recipes/block/bough_bed.json); the poles reference vanilla's stripped spruce log
texture, never a copy.
Usage (from the repository root): python3 -I tools/artgen/bough_bed.py"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "bough_bed 0.1.0"
POLE = "minecraft:block/stripped_spruce_log"
TOP = "yearofice:block/bough_bed_top"
SIDE = "yearofice:block/bough_bed_side"


def box(frm, to, tex, top=None):
    faces = {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "down")}
    faces["up"] = {"texture": "#" + (top or tex)}
    return {"from": frm, "to": to, "faces": faces}


def half(head):
    els = [box([0, 0, 0], [2, 2, 16], "pole"), box([14, 0, 0], [16, 2, 16], "pole"),
           box([1, 1, 0], [15, 4, 16], "side", "top")]
    if head:
        els.append(box([2, 4, 1], [14, 5, 7], "side", "top"))
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": TOP, "top": TOP, "side": SIDE, "pole": POLE}, "elements": els}


def files():
    rot = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    return {
        "blockstates/bough_bed.json": {"variants": {
            f"facing={f},occupied={o},part={p}": {"model": f"yearofice:block/bough_bed_{p}", "y": y}
            for f, y in rot for o in ("false", "true") for p in ("foot", "head")}},
        "models/block/bough_bed_foot.json": half(False),
        "models/block/bough_bed_head.json": half(True),
    }


def outputs():
    return ["assets/yearofice/" + k for k in files()]


if __name__ == "__main__":
    for rel_path, doc in files().items():
        p = os.path.join(ASSETS, rel_path)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w") as f:
            json.dump(doc, f, indent=1)
            f.write("\n")
    print(f"{VERSION}: {len(files())} files")
