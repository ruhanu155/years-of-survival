"""fire_ring: writes the yearofice:fire_ring blockstate, its block models and its item model (G0 blockout).

The ring is a multipart: 8 stones always; tinder, kindling and split logs by the `lay` state; ember, flames,
coals and ash by the `burn` state. Textures come from the artgen recipes fire_ring_* (folder block).
Usage (from the repository root): python3 -I tools/artgen/fire_ring.py"""
import json, math, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "fire_ring 0.1.0"
T = "yearofice:block/fire_ring_"


def cube(frm, to, tex, rot=None, faces=("north", "south", "east", "west", "up", "down")):
    e = {"from": frm, "to": to, "faces": {f: {"texture": "#" + tex} for f in faces}}
    if rot:
        e["rotation"] = rot
    return e


def model(textures, elements, ao=True):
    m = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": textures, "elements": elements}
    if not ao:
        m["ambientocclusion"] = False
    return m


def stones():
    out = []
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        cx, cz = 8 + 5.6 * math.cos(a), 8 + 5.6 * math.sin(a)
        h = 3 if i % 2 else 2.5
        w = 2.2 if i % 3 else 2.6
        out.append(cube([round(cx - w, 2), 0, round(cz - w, 2)], [round(cx + w, 2), h, round(cz + w, 2)], "stone"))
    return out


def planes(width, height, tex="flame"):
    lo, hi = 8 - width / 2, 8 + width / 2
    out = []
    for ang in (45, -45):
        e = cube([lo, 0.2, 8], [hi, height, 8], tex, {"origin": [8, 8, 8], "axis": "y", "angle": ang}, ("north", "south"))
        for f in e["faces"].values():
            f["uv"] = [0, 16 - height, 16, 16] if height < 16 else [0, 0, 16, 16]
        out.append(e)
    return out


def models():
    return {
        "fire_ring_base": model({"particle": T + "stone", "stone": T + "stone", "ash": T + "ash"},
                                [cube([3, 0, 3], [13, 0.2, 13], "ash", faces=("up",))] + stones()),
        "fire_ring_tinder": model({"particle": T + "tinder", "tinder": T + "tinder"}, [cube([6, 0.2, 6], [10, 2, 10], "tinder")]),
        "fire_ring_kindling": model({"particle": T + "kindling", "kindling": T + "kindling"}, [
            cube([3.5, 2, 7.5], [12.5, 3, 8.5], "kindling"),
            cube([7.5, 2.2, 3.5], [8.5, 3.2, 12.5], "kindling"),
            cube([3.5, 3, 7.4], [12.5, 4, 8.4], "kindling", {"origin": [8, 3.5, 8], "axis": "y", "angle": 45}),
            cube([3.5, 3.1, 7.6], [12.5, 4.1, 8.6], "kindling", {"origin": [8, 3.5, 8], "axis": "y", "angle": -45})]),
        "fire_ring_fuel": model({"particle": T + "log", "log": T + "log"}, [
            cube([2.5, 3.5, 4.5], [13.5, 6, 7], "log"),
            cube([2.5, 3.5, 9], [13.5, 6, 11.5], "log"),
            cube([2.5, 6, 6.8], [13.5, 8.5, 9.3], "log", {"origin": [8, 7, 8], "axis": "y", "angle": 45})]),
        "fire_ring_ember": model({"particle": T + "coals", "coals": T + "coals"}, [cube([7, 2, 7], [9, 2.6, 9], "coals")], ao=False),
        "fire_ring_flame_tiny": model({"particle": T + "flame", "flame": T + "flame"}, planes(6, 5), ao=False),
        "fire_ring_flame_small": model({"particle": T + "flame", "flame": T + "flame"}, planes(10, 9), ao=False),
        "fire_ring_flame": model({"particle": T + "flame", "flame": T + "flame"}, planes(14, 16), ao=False),
        "fire_ring_coals": model({"particle": T + "coals", "coals": T + "coals"}, [cube([3.5, 0.2, 3.5], [12.5, 1.4, 12.5], "coals")], ao=False),
        "fire_ring_ash": model({"particle": T + "ash", "ash": T + "ash", "coals": T + "coals"}, [
            cube([3.5, 0.2, 3.5], [12.5, 1.6, 12.5], "ash"),
            cube([5.5, 1.6, 5.5], [10.5, 2.4, 10.5], "ash")]),
    }


def blockstate():
    m = lambda name: {"model": "yearofice:block/" + name}
    return {"multipart": [
        {"apply": m("fire_ring_base")},
        {"when": {"lay": "tinder|kindling|fuel", "burn": "out|ember"}, "apply": m("fire_ring_tinder")},
        {"when": {"lay": "kindling|fuel", "burn": "out|ember|tinder"}, "apply": m("fire_ring_kindling")},
        {"when": {"lay": "fuel"}, "apply": m("fire_ring_fuel")},
        {"when": {"burn": "ember"}, "apply": m("fire_ring_ember")},
        {"when": {"burn": "tinder"}, "apply": m("fire_ring_flame_tiny")},
        {"when": {"burn": "small|low"}, "apply": m("fire_ring_flame_small")},
        {"when": {"burn": "steady|hot"}, "apply": m("fire_ring_flame")},
        {"when": {"burn": "coals|low|steady|hot"}, "apply": m("fire_ring_coals")},
        {"when": {"burn": "banked"}, "apply": m("fire_ring_ash")},
    ]}


def outputs():
    """Every file this script writes, relative to src/main/resources (for credits.json)."""
    files = ["assets/yearofice/blockstates/fire_ring.json", "assets/yearofice/models/item/fire_ring.json"]
    files += [f"assets/yearofice/models/block/{n}.json" for n in models()]
    return files


def write():
    for name, mdl in models().items():
        p = os.path.join(ASSETS, "models", "block", name + ".json")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump(mdl, open(p, "w"), indent=1)
    p = os.path.join(ASSETS, "blockstates", "fire_ring.json")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    json.dump(blockstate(), open(p, "w"), indent=1)
    json.dump({"parent": "yearofice:block/fire_ring_base"}, open(os.path.join(ASSETS, "models", "item", "fire_ring.json"), "w"), indent=1)
    print(f"{VERSION}: wrote {len(outputs())} files")


if __name__ == "__main__":
    write()
