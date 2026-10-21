"""ice_blocks: textures, models and blockstates for skim_ice, river_ice (stage thin|safe|rotten) and ice_hole
(Minecraft engine > Ice, snow and permafrost), from the palette's ice and snow ramps. Deterministic.

Usage (from the repository root): python3 -I tools/artgen/ice_blocks.py"""
import json, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
AS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
RAMPS = json.load(open(os.path.join(HERE, "palette.json")))["ramps"]


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def tex(name, ramp, alpha, seed, speck=None):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = rgb(ramp[min(len(ramp) - 1, max(0, int(rnd.gauss(2, 0.9))))])
            img.putpixel((x, y), c + (alpha,))
    if speck:
        for _ in range(10):
            img.putpixel((rnd.randrange(16), rnd.randrange(16)), rgb(speck) + (min(255, alpha + 30),))
    os.makedirs(os.path.join(AS, "textures", "block"), exist_ok=True)
    img.save(os.path.join(AS, "textures", "block", name + "_all.png"))


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(json.dumps(obj, indent=2) + "\n")


def build():
    ice, snow = RAMPS["ice"], RAMPS["snow"]
    tex("skim_ice", ice[2:], 150, 11, snow[4])
    tex("river_ice_thin", ice[1:], 210, 12, snow[3])
    tex("river_ice_safe", snow[2:], 235, 13, ice[4])
    tex("river_ice_rotten", [snow[0], snow[1], ice[2], snow[1], ice[1]], 235, 14)
    tex("ice_hole", ice[:3], 230, 15)
    for n in ["skim_ice", "river_ice_thin", "river_ice_safe", "river_ice_rotten", "ice_hole"]:
        w(AS + "/models/block/%s.json" % n, {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                              "textures": {"all": "yearofice:block/%s_all" % n}})
    w(AS + "/blockstates/skim_ice.json", {"variants": {"": {"model": "yearofice:block/skim_ice"}}})
    w(AS + "/blockstates/ice_hole.json", {"variants": {"": {"model": "yearofice:block/ice_hole"}}})
    w(AS + "/blockstates/river_ice.json", {"variants": {"stage=%s" % s: {"model": "yearofice:block/river_ice_" + s}
                                                        for s in ("thin", "safe", "rotten")}})


if __name__ == "__main__":
    build()
