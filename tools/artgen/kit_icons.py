"""kit_icons: 16x16 icons for the kit and story items that shared the ferro-rod sprite (bandage, bear spray, climbing skins,
compass, knife, hide coat, medkit, moccasins, head net, note, pilot bread, plant guide, radio battery, seed packet, skates,
skis, solar panel). Same scheme as garments.py: run-length rows, digits = ramp A, a-e = ramp B, v-z = ramp C, o = outline.
Usage (from the repository root): python3 -I tools/artgen/kit_icons.py [--check]"""
import os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "kit_icons 0.1.0"
sys.path.insert(0, HERE)
import palette_lib as P

E = "................"
KNIFE = [E, "..............o.", ".............oao", "............oa2o", "...........oa22o", "..........oa22o.", ".........oa22o..", "........oa22o...", ".......oa22o....", "......ooo2o.....", ".....o333o......", "....o333o.......", "...o333o........", "..o333o.........", "..oooo..........", E]
BANDAGE = [E, E, "....oooooooo....", "...o44444444o...", "..o4433333344o..", "..o4433333344o..", "..o4444444444o..", "..o4444444444o..", "..o4433333344o..", "..o4433333344o..", "...o44444444o...", "....oooooooo....", E, E, E, E]
MEDKIT = [E, E, "....oooooooo....", "...o4444444o....", "..oaaaaaaaaaao..", "..oaaaa22aaaao..", "..oaaaa22aaaao..", "..oaa222222aao..", "..oaa222222aao..", "..oaaaa22aaaao..", "..oaaaa22aaaao..", "..oaaaaaaaaaao..", "..oooooooooooo..", E, E, E]
SPRAY = [E, "......oooo......", ".....o3333o.....", ".....o3oo3o.....", "......o33o......", ".....o2222o.....", "....o222222o....", "....o2aaaa2o....", "....o2abba2o....", "....o2abba2o....", "....o2aaaa2o....", "....o222222o....", "....o222222o....", "....o111111o....", "....oooooooo....", E]
COMPASS = [E, E, "......oooo......", "....oo4444oo....", "...o44444444o...", "..o4444a44444o..", "..o444aa44444o..", "..o44aaa4a444o..", "..o4444aaa444o..", "..o44444aa444o..", "..o444444a444o..", "...o44444444o...", "....oo4444oo....", "......oooo......", E, E]
COAT = [E, ".....oooooo.....", "....o222222o....", "...o22o22o22o...", "..o2222o22222o..", ".o222a222a2222o.", ".o22o2a2a2o222o.", ".o22o222a2o222o.", ".o22o222a2o222o.", ".o22o222a2o222o.", "..oo.o22a22o.oo.", ".....o22a22o....", ".....o22a22o....", ".....o11111o....", ".....ooooooo....", E]
MOCC = [E, E, E, E, "......oooo......", "....oo3333oo....", "...o33a33a33o...", "..o333333333o...", "..o33333333333o.", ".o222222222222o.", ".o222222222222o.", "..o11111111111o.", "..oooooooooooo..", E, E, E]
NET = [E, "......oooo......", ".....o2222o.....", "....o222222o....", "...o22222222o...", "...o2a2a2a2ao...", "...oa2a2a2a2o...", "...o2a2a2a2ao...", "...oa2a2a2a2o...", "...o2a2a2a2ao...", "...oa2a2a2a2o...", "...o2a2a2a2ao...", "....o222222o....", ".....oooooo.....", E, E]
NOTE = [E, "....ooooooo.....", "...o4444444oo...", "...o4444444o4o..", "...o4444444ooo..", "...o433333444o..", "...o444444444o..", "...o43333334.o..", "...o444444444o..", "...o433334444o..", "...o444444444o..", "...o433333344o..", "...o444444444o..", "...ooooooooooo..", E, E]
CRACKER = [E, E, E, "....oooooooo....", "...o44444444o...", "..o4443444344o..", "..o4444444444o..", "..o4434443444o..", "..o4444444444o..", "..o4443444344o..", "..o4444444444o..", "...o44444444o...", "....oooooooo....", E, E, E]
GUIDE = [E, "...oooooooooo...", "..oaaaaaaaaaao..", "..oa44444444ao..", "..oa4a44444a4o..", "..oa44aaa444ao..", "..oa4aa4aa44ao..", "..oa44444444ao..", "..oa4bbbbbb4ao..", "..oa44444444ao..", "..oa4bbbb444ao..", "..oa44444444ao..", "..oaaaaaaaaaao..", "..oooooooooooo..", E, E]
BATTERY = [E, E, "......oo........", ".....o33o.......", "....oooooooo....", "...o33333333o...", "...o3aaaaaa3o...", "...o3aaaaaa3o...", "...o3aaaaaa3o...", "...o3aaaaaa3o...", "...o3aaaaaa3o...", "...o33333333o...", "...o11111111o...", "...oooooooooo...", E, E]
SEEDS = [E, "....oooooooo....", "...o44444444o...", "...o4aaaaaa4o...", "...o4a4444a4o...", "...o4a4bb4a4o...", "...o4a4bb4a4o...", "...o4a4444a4o...", "...o4aaaaaa4o...", "...o44444444o...", "...o44444444o...", "...o44444444o...", "...oooooooooo...", E, E, E]
SKATES = [E, E, E, E, "...oooo.........", "..o3333oooo.....", "..o33333333o....", "...o3333333o....", "....o333333o....", ".....o3333o.....", ".....oooooo.....", ".....oaaaaooo...", "....oaaaaaaaao..", "....oooooooooo..", E, E]
SKIS = [E, ".oo..........oo.", "o33o........o33o", "o33o........o33o", "o33o........o33o", "o33o...oo...o33o", "o33o..o33o..o33o", "o33o..o33o..o33o", "o3ao..o33o..oa3o", "o3ao..o33o..oa3o", "o33o..o33o..o33o", "o33o..o33o..o33o", "o33o..o33o..o33o", "o33o...oo...o33o", "o33o........o33o", ".oo..........oo."]
SKINS = [E, E, E, "..oooooooooooo..", ".oaaaaaaaaaaaao.", ".oa2a2a2a2a2a2o.", ".o2a2a2a2a2a2ao.", ".oaaaaaaaaaaaao.", "..oooooooooooo..", E, "..oooooooooooo..", ".oaaaaaaaaaaaao.", ".oa2a2a2a2a2a2o.", ".oaaaaaaaaaaaao.", "..oooooooooooo..", E]
PANEL = [E, "..oooooooooooo..", ".oaaaaaaaaaaaao.", ".oa11o11o11o1ao.", ".oa11o11o11o1ao.", ".oaoooooooooaao.", ".oa11o11o11o1ao.", ".oa11o11o11o1ao.", ".oaoooooooooaao.", ".oa11o11o11o1ao.", ".oa11o11o11o1ao.", ".oaaaaaaaaaaaao.", "..oooooooooooo..", "......o33o......", "......oooo......", E]

ITEMS = {
    "fixed_blade_knife": ("steel", "wood_cut", None, KNIFE),
    "bandage": ("bark_birch", "fat_cream", None, BANDAGE),
    "medkit": ("snow", "berry_red", None, MEDKIT),
    "bear_spray": ("plastic_orange", "snow", None, SPRAY),
    "compass": ("steel", "berry_red", None, COMPASS),
    "hide_coat": ("hide_raw", "fur_tan", None, COAT),
    "moccasins": ("hide_raw", "fur_tan", None, MOCC),
    "mosquito_head_net": ("rock_schist", "lichen_pale", None, NET),
    "note": ("bark_birch", "soil", None, NOTE),
    "pilot_bread": ("fat_cream", "wood_cut", None, CRACKER),
    "plant_guide": ("leaf_green", "bark_birch", None, GUIDE),
    "radio_battery": ("steel", "flower_yellow", None, BATTERY),
    "seed_packet": ("bark_birch", "leaf_green", None, SEEDS),
    "skates": ("steel", "hide_raw", None, SKATES),
    "skis": ("wood_cut", "berry_red", None, SKIS),
    "climbing_skins": ("fur_wolf_pale", "fur_hare_summer", None, SKINS),
    "solar_panel_small": ("ice", "steel", None, PANEL),
}


def paint(a, b, c, rows):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    ra, rb, rc = P.RAMPS[a], P.RAMPS[b] if b else None, P.RAMPS[c] if c else None
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch == ".": continue
            if ch == "o": col = P.hexrgb(P.OUTLINE)
            elif ch.isdigit(): col = P.hexrgb(ra[min(int(ch), 4)])
            elif ch in "abcde": col = P.hexrgb(rb["abcde".index(ch)])
            else: col = P.hexrgb(rc["vwxyz".index(ch)])
            img.putpixel((x, y), col + (255,))
    return img


def main(check=False):
    bad = 0
    for name, (a, b, c, rows) in ITEMS.items():
        img = paint(a, b, c, rows)
        ok, ncol, ramps, probs = P.check_image(img)
        if not ok: bad += 1; print(name, probs)
        if check: continue
        img.save(os.path.join(ASSETS, "textures", "item", name + ".png"))
        with open(os.path.join(ASSETS, "models", "item", name + ".json"), "w") as f:
            f.write('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "yearofice:item/%s"\n  }\n}\n' % name)
    print(f"{len(ITEMS)} icons, {bad} off-spec")
    return bad


if __name__ == "__main__":
    sys.exit(1 if main("--check" in sys.argv) else 0)
