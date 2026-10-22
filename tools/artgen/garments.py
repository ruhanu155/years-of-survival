"""garments: 16x16 item icons for the 13 clothing items (the 7 start-kit garments plus the made ones), each painted
from at most three palette ramps plus the outline, deterministic from the drawing below (no random seeds).

Every icon is a small silhouette drawn with run-length rows ('.' transparent, 'o' outline, digits = ramp A steps,
letters a-e = ramp B steps, letters v-z = ramp C steps). Rows are 16 characters; the picture is centred.
Usage (from the repository root): python3 -I tools/artgen/garments.py [--check]"""
import os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "garments 0.1.0"
sys.path.insert(0, HERE)
import palette_lib as P

# name: (rampA, rampB, rampC, rows)
JACKET = [
    "................",
    ".....oooooo.....",
    "....o222222o....",
    "...o22o22o22o...",
    "..o2222o222a2o..",
    ".o22a2222222a2o.",
    ".o2a2o2222o2a2o.",
    ".o2a2o2222o2a2o.",
    ".oa22o2222o22ao.",
    "..oo.o2222o.oo..",
    ".....o2222o.....",
    ".....o2222o.....",
    ".....o2222o.....",
    ".....o1111o.....",
    ".....oooooo.....",
    "................",
]
PANTS = [
    "................",
    "....oooooooo....",
    "...o33333333o...",
    "...o32222223o...",
    "...o32o22o23o...",
    "...o322o22223o..",
    "...o322o22223o..",
    "...o222o2222o...",
    "...o222o2222o...",
    "...o222o2222o...",
    "...o222o2222o...",
    "...o222o2222o...",
    "...o111o1111o...",
    "...ooooo.oooo...",
    "................",
    "................",
]
BOOT = [
    "................",
    "................",
    "......oooo......",
    ".....o3333o.....",
    ".....o3c3co.....",
    ".....o3333o.....",
    ".....o3c3co.....",
    ".....o3333o.....",
    ".....o3333ooo...",
    ".....o22222aao..",
    "....o222222aao..",
    "...o2222222aao..",
    "..o111111111ao..",
    "..oooooooooooo..",
    "................",
    "................",
]
SOCK = [
    "................",
    "................",
    "......oooo......",
    ".....o4444o.....",
    ".....o3333o.....",
    ".....o3333o.....",
    ".....o3333o.....",
    ".....o3333o.....",
    ".....o3333oo....",
    ".....o33333ao...",
    "....o333333ao...",
    "...o2222222ao...",
    "...o222222aao...",
    "....oooooooo....",
    "................",
    "................",
]
BEANIE = [
    "................",
    "................",
    "......oooo......",
    ".....o4334o.....",
    "....o333333o....",
    "...o33333333o...",
    "...o33333333o...",
    "..o3333333333o..",
    "..o3333333333o..",
    "..o2323232323o..",
    "..o3232323232o..",
    "..o2323232323o..",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
]
FUR_HAT = [
    "................",
    "................",
    "......oooo......",
    ".....o2222o.....",
    "....o222222o....",
    "...o22222222o...",
    "..oaaaaaaaaaao..",
    ".oabbbbbbbbbbao.",
    ".oabcccccccbbao.",
    ".oabcccccccbbao.",
    ".oabbbbbbbbbbao.",
    "..oaaaaaaaaaao..",
    "...oo......oo...",
    "................",
    "................",
    "................",
]
GLOVE = [
    "................",
    "........oo......",
    ".......o33o.....",
    "....oo.o33o.oo..",
    "...o33oo33oo33o.",
    "...o3333333333o.",
    "...o3333333333o.",
    "..oo3333333333o.",
    ".o33333333333o..",
    ".o333333333333o.",
    "..o22222222222o.",
    "...o2222222222o.",
    "...o222222222o..",
    "....o1111111o...",
    "....ooooooooo...",
    "................",
]
MITTEN = [
    "................",
    "................",
    ".......oooo.....",
    "......o3333o....",
    ".....o333333o...",
    "..oo.o333333o...",
    ".o33o3333333o...",
    ".o333333333o....",
    ".o3333333333o...",
    "..o3333333333o..",
    "...o22222222o...",
    "...oaaaaaaaao...",
    "...obbbbbbbbo...",
    "...oaaaaaaaao...",
    "....oooooooo....",
    "................",
]
PARKA = [
    "................",
    ".....oooooo.....",
    "....oaaaaaao....",
    "...oa2b22b2ao...",
    "..o2a222222a2o..",
    ".o22a222222a22o.",
    ".o2a2o2v22o2a2o.",
    ".o2a2o2v22o2a2o.",
    ".o2a2o2v22o2a2o.",
    ".oa22o2v22o22ao.",
    "..oo.o2v22o.oo..",
    ".....o2v22o.....",
    ".....o2v22o.....",
    ".....oaaaao.....",
    ".....oooooo.....",
    "................",
]
SHELL = [
    "................",
    ".....oooooo.....",
    "....o333333o....",
    "...o33o33o33o...",
    "..o3333o33333o..",
    ".o333333333333o.",
    ".o33o333w333o3o.",
    ".o33o333w333o3o.",
    ".o33o333w333o3o.",
    ".o3o.o33w33o.3o.",
    "..o..o33w33o..o.",
    ".....o33w33o....",
    ".....o33w33o....",
    ".....o2222o.....",
    ".....oooooo.....",
    "................",
]
LINER = [
    "................",
    "................",
    "......oooo......",
    ".....o4444o.....",
    ".....o4334o.....",
    ".....o4334o.....",
    ".....o4334o.....",
    ".....o4334o.....",
    ".....o4334oo....",
    ".....o43334ao...",
    "....o433333ao...",
    "...o3333333ao...",
    "...o333333aao...",
    "....oooooooo....",
    "................",
    "................",
]
MUKLUK = [
    "................",
    "................",
    "......oooo......",
    ".....o3333o.....",
    ".....oaaaao.....",
    ".....o3333o.....",
    ".....o3333o.....",
    ".....oaaaao.....",
    ".....o3333ooo...",
    ".....o3333322o..",
    "....o33333322o..",
    "...o333333322o..",
    "..o222222222o...",
    "..oooooooooo....",
    "................",
    "................",
]

GARMENTS = {
    # start kit (Outfit.START_IDS)
    "fleece_jacket": ("berry_blue", "ice", None, JACKET),
    "rain_shell": ("plastic_orange", "steel", "rock_schist", SHELL),
    "work_pants": ("fur_tan", "soil", None, PANTS),
    "wool_socks": ("lichen_pale", "fur_wolf_pale", None, SOCK),
    "hiking_boots": ("fur_hare_summer", "rock_schist", "soil", BOOT),
    "wool_beanie": ("berry_cran", "comb_red", None, BEANIE),
    "work_gloves": ("hide_raw", "fur_tan", None, GLOVE),
    # made garments
    "parka": ("fur_wolf_pale", "fur_white", "hide_raw", PARKA),
    "fur_mittens": ("fur_hare_summer", "fur_white", None, MITTEN),
    "fur_hat": ("fur_hare_summer", "fur_white", "fur_tan", FUR_HAT),
    "hide_pants": ("hide_raw", "fur_tan", None, PANTS),
    "mukluks": ("hide_raw", "fur_white", None, MUKLUK),
    "hare_fur_liners": ("fur_white", "fur_hare_summer", None, LINER),
}


def paint(a, b, c, rows):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ra, rb, rc = P.RAMPS[a], P.RAMPS[b] if b else None, P.RAMPS[c] if c else None
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch == "o":
                col = P.hexrgb(P.OUTLINE)
            elif ch.isdigit():
                col = P.hexrgb(ra[min(int(ch), 4)])
            elif ch in "abcde":
                col = P.hexrgb(rb["abcde".index(ch)])
            else:
                col = P.hexrgb(rc["vwxyz".index(ch)])
            img.putpixel((x, y), col + (255,))
    return img


def outputs():
    return sorted(f"assets/yearofice/{k}/item/{n}.{e}" for n in GARMENTS for k, e in (("textures", "png"), ("models", "json")))


def main(check=False):
    bad = 0
    for name, (a, b, c, rows) in GARMENTS.items():
        img = paint(a, b, c, rows)
        ok, ncol, ramps, probs = P.check_image(img)
        if not ok:
            bad += 1
            print(name, probs)
        if check:
            continue
        img.save(os.path.join(ASSETS, "textures", "item", name + ".png"))
        with open(os.path.join(ASSETS, "models", "item", name + ".json"), "w") as f:
            # head: scale 0 so a garment in the vanilla head slot is not drawn as a floating prop (WornLayer paints it instead)
            f.write('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "yearofice:item/%s"\n  },\n'
                    '  "display": {\n    "head": {\n      "scale": [0, 0, 0]\n    }\n  }\n}\n' % name)
    print(f"{len(GARMENTS)} garments, {bad} off-spec")
    return bad


if __name__ == "__main__":
    sys.exit(1 if main("--check" in sys.argv) else 0)
