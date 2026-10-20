"""m6_waters: the M6 lane C art that is not an entity rig: thirteen fish skins and cards (each a recolour of an
existing skin by a fixed tint), the kit rod icon, the fish weir stake, fish wheel and tip-up blocks, and the bird
meat item model. Deterministic: same input, same bytes.
Usage (from the repository root): python3 -I tools/artgen/m6_waters.py"""
import json, os, random, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "m6_waters 0.1.0"
sys.path.insert(0, HERE)

# species: (base skin, (r, g, b) multiplier, (r, g, b) add)
FISH = {
    "sockeye_salmon": ("chum_salmon", (1.25, 0.55, 0.55), (10, 0, 0)),
    "arctic_char": ("dolly_varden", (1.2, 1.0, 0.7), (12, 6, 0)),
    "broad_whitefish": ("humpback_whitefish", (1.1, 1.05, 0.95), (6, 4, 0)),
    "round_whitefish": ("humpback_whitefish", (0.95, 1.0, 1.05), (0, 2, 6)),
    "least_cisco": ("arctic_grayling", (1.2, 1.25, 1.3), (14, 14, 14)),
    "bering_cisco": ("arctic_grayling", (1.1, 1.15, 1.3), (8, 8, 16)),
    "sheefish": ("northern_pike", (1.25, 1.3, 1.3), (22, 22, 22)),
    "longnose_sucker": ("burbot", (1.1, 0.9, 0.7), (10, 4, 0)),
    "slimy_sculpin": ("burbot", (0.8, 0.8, 0.75), (0, 0, 0)),
    "lake_chub": ("arctic_grayling", (0.85, 0.95, 0.75), (0, 6, 0)),
    "ninespine_stickleback": ("arctic_grayling", (0.8, 0.85, 0.7), (0, 4, 0)),
    "alaska_blackfish": ("burbot", (0.55, 0.6, 0.45), (0, 0, 0)),
    "arctic_lamprey": ("burbot", (0.9, 0.85, 0.7), (6, 4, 0)),
}
BLOCKS = {"weir_stake": ((6, 0, 6), (10, 16, 10)), "fish_wheel": ((0, 0, 0), (16, 16, 16)), "tip_up": ((5, 0, 5), (11, 12, 11))}


def tint(im, mul, add):
    out = im.copy()
    px = out.load()
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            px[x, y] = tuple(max(0, min(255, int(c * m + d))) for c, m, d in zip((r, g, b), mul, add)) + (a,)
    return out


def block_texture(name):
    import palette_lib as P
    from PIL import Image
    ramp = [P.hexrgb(h) for h in P.RAMPS["wood_cut" if name != "weir_stake" else "bark_spruce"]]
    rnd = random.Random(sum(map(ord, name)))
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            band = (x // 4 + (y // 8 if name == "fish_wheel" else 0)) % len(ramp)
            step = max(0, min(len(ramp) - 1, band if name != "tip_up" else rnd.choice([1, 2, 2, 3])))
            im.putpixel((x, y), tuple(ramp[step]) + (255,))
    if name == "tip_up":
        for y in range(0, 4):
            for x in range(9, 15):
                im.putpixel((x, y), (190, 40, 30, 255))
    return im


def block_files(name):
    (x0, y0, z0), (x1, y1, z1) = BLOCKS[name]
    faces = {k: {"texture": "#all"} for k in ("down", "up", "north", "south", "east", "west")}
    return {
        f"blockstates/{name}.json": json.dumps({"variants": {"": {"model": f"yearofice:block/{name}"}}}, indent=1),
        f"models/block/{name}.json": json.dumps({"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                 "textures": {"all": f"yearofice:block/{name}", "particle": f"yearofice:block/{name}"},
                                                 "elements": [{"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}]}, indent=1),
        f"models/item/{name}.json": json.dumps({"parent": f"yearofice:block/{name}"}, indent=1),
    }


def outputs():
    out = []
    for f in FISH:
        out += [f"assets/yearofice/textures/entity/fish/{f}.png", f"assets/yearofice/textures/gui/fish/{f}.png", f"assets/yearofice/models/item/{f}.json"]
    for b in BLOCKS:
        out += [f"assets/yearofice/{k}" for k in block_files(b)] + [f"assets/yearofice/textures/block/{b}.png"]
    out += ["assets/yearofice/models/item/kit_rod.json", "assets/yearofice/textures/item/kit_rod.png", "assets/yearofice/models/item/bird_meat.json"]
    return sorted(out)


def write(rel, data):
    p = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    if isinstance(data, str):
        open(p, "w").write(data + "\n")
    else:
        data.save(p)


def main():
    from PIL import Image
    for f, (base, mul, add) in FISH.items():
        for kind in ("textures/entity/fish", "textures/gui/fish"):
            im = Image.open(os.path.join(ASSETS, kind, base + ".png")).convert("RGBA")
            write(f"{kind}/{f}.png", tint(im, mul, add))
        write(f"models/item/{f}.json", json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "yearofice:item/arctic_grayling"}}, indent=2))
    for b in BLOCKS:
        for k, v in block_files(b).items():
            write(k, v)
        write(f"textures/block/{b}.png", block_texture(b))
    write("textures/item/kit_rod.png", tint(Image.open(os.path.join(ASSETS, "textures/item/rod.png")).convert("RGBA"), (0.85, 0.95, 1.1), (0, 6, 14)))
    write("models/item/kit_rod.json", json.dumps({"parent": "minecraft:item/handheld_rod", "textures": {"layer0": "yearofice:item/kit_rod"}}, indent=1))
    with open(os.path.join(ASSETS, "models/item/willow_ptarmigan_meat.json")) as f:
        write("models/item/bird_meat.json", f.read().rstrip("\n"))


if __name__ == "__main__":
    main()
