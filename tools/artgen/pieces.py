"""pieces: textures, models and blockstates for the M8 building pieces (wall courses, roofs, floors, path, door and
window frames, awning, dressed stone), from the palette ramps. Deterministic.

Usage (from the repository root): python3 -I tools/artgen/pieces.py"""
import json, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
AS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
RAMPS = json.load(open(os.path.join(HERE, "palette.json")))["ramps"]
VERSION = "pieces 1.0"
WRITTEN = []
# name -> (ramp, seed, kind): kind cube | slab_bottom | slab_top | thin
CUBES = {"wall_log": ("bark_spruce", 21, "cube"), "wall_plank": ("wood_cut", 22, "cube"), "wall_sod": ("duff", 23, "cube"),
         "wall_stone": ("rock_schist", 24, "cube"), "roof_pole_sod": ("soil", 25, "cube"), "roof_plank": ("wood_cut", 26, "cube"),
         "roof_shingle": ("bark_birch", 27, "cube"), "slab_floor": ("wood_cut", 28, "slab_bottom"), "log_deck": ("bark_spruce", 29, "cube"),
         "awning": ("duff", 30, "slab_top"), "dressed_stone": ("rock_schist", 31, "cube"), "door_frame": ("wood_cut", 32, "cube"),
         "window_frame": ("wood_cut", 33, "cube")}
PATHS = {"dirt": ("soil", 41), "gravel": ("gravel", 42), "plank": ("wood_cut", 43), "stone": ("rock_schist", 44)}


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def tex(name, ramp, seed):
    rnd = random.Random(seed)
    r = RAMPS[ramp]
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), rgb(r[min(len(r) - 1, max(0, int(rnd.gauss(len(r) / 2, 1.0))))]) + (255,))
    os.makedirs(os.path.join(AS, "textures", "block"), exist_ok=True)
    img.save(os.path.join(AS, "textures", "block", name + ".png"))
    WRITTEN.append("assets/yearofice/textures/block/%s.png" % name)


def w(sub, name, obj):
    p = os.path.join(AS, sub, name + ".json")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        f.write(json.dumps(obj, indent=2) + "\n")
    WRITTEN.append("assets/yearofice/%s/%s.json" % (sub, name))


def build():
    WRITTEN.clear()
    for n, (ramp, seed, kind) in CUBES.items():
        tex(n, ramp, seed)
        t = "yearofice:block/" + n
        parent = {"cube": "minecraft:block/cube_all", "slab_bottom": "minecraft:block/slab", "slab_top": "minecraft:block/slab_top"}[kind]
        tx = {"all": t} if kind == "cube" else {"bottom": t, "top": t, "side": t}
        w("models/block", n, {"parent": parent, "textures": tx})
        if n == "door_frame":
            w("models/block", "door_frame_open", {"parent": "minecraft:block/cube_all", "textures": {"all": t}})
            w("blockstates", n, {"variants": {"open=false": {"model": "yearofice:block/door_frame"}, "open=true": {"model": "yearofice:block/door_frame_open"}}})
        else:
            w("blockstates", n, {"variants": {"": {"model": "yearofice:block/" + n}}})
    states = {}
    for s, (ramp, seed) in PATHS.items():
        tex("path_" + s, ramp, seed)
        w("models/block", "path_" + s, {"parent": "minecraft:block/thin_block", "textures": {"wool": "yearofice:block/path_" + s}})
        states["surface=" + s] = {"model": "yearofice:block/path_" + s}
    w("blockstates", "path", {"variants": states})


def outputs():
    build()
    return list(WRITTEN)


if __name__ == "__main__":
    build()
    print("%s: %d files" % (VERSION, len(WRITTEN)))
