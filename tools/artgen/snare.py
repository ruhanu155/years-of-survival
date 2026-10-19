"""snare: writes the yearofice:snare blockstate and block models (G0 blockout), and the item models of the
ecosystem slice's game items (whole_game and the hare pelt), which reuse the art wave's sprites.

The snare is a wire loop 10 cm across a fist's height above the ground, hung from a cross stick between two guide
sticks (Blueprint > Traps). `sprung` closes the loop. Textures are vanilla references, never copies.
Usage (from the repository root): python3 -I tools/artgen/snare.py"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "snare 0.1.0"
STICK = "minecraft:block/stripped_spruce_log"
WIRE = "minecraft:block/iron_block"

# item id -> the art wave's sprite it shows until its own icon is drawn
# (the two meats moved to tools/artgen/cooking.py, whose models switch between raw, cooked and burnt)
ITEMS = {"whole_game": "raw_hare", "hare_pelt": "raw_hide"}


def cube(frm, to, tex):
    return {"from": frm, "to": to, "faces": {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "up", "down")}}


def sticks():
    return [cube([3, 0, 7.5], [4, 7, 8.5], "stick"), cube([12, 0, 7.5], [13, 7, 8.5], "stick"),
            cube([3, 7, 7.5], [13, 8, 8.5], "stick")]


def loop_open():
    t = 0.35
    return [cube([7.82, 4.6, 7.82], [8.18, 7, 8.18], "wire"),
            cube([6.4, 1.6, 7.82], [9.6, 1.6 + t, 8.18], "wire"), cube([6.4, 4.6 - t, 7.82], [9.6, 4.6, 8.18], "wire"),
            cube([6.4, 1.6, 7.82], [6.4 + t, 4.6, 8.18], "wire"), cube([9.6 - t, 1.6, 7.82], [9.6, 4.6, 8.18], "wire")]


def loop_sprung():
    return [cube([7.82, 1.2, 7.82], [8.18, 7, 8.18], "wire"), cube([7.5, 1.0, 7.5], [8.5, 1.8, 8.5], "wire")]


def model(elements):
    return {"parent": "minecraft:block/block", "textures": {"particle": STICK, "stick": STICK, "wire": WIRE}, "elements": elements}


def files():
    out = {"blockstates/snare.json": {"variants": {
        f"facing={f},sprung={s}": {"model": f"yearofice:block/snare{'_sprung' if s == 'true' else ''}", "y": y}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for s in ("false", "true")}},
        "models/block/snare.json": model(sticks() + loop_open()),
        "models/block/snare_sprung.json": model(sticks() + loop_sprung())}
    for item, sprite in ITEMS.items():
        out[f"models/item/{item}.json"] = {"parent": f"yearofice:item/{sprite}"}
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
    print(f"snare: wrote {len(files())} files")
