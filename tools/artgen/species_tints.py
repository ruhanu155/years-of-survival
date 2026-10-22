"""species_tints: per-species icons for the hides, pelts, fish and organs that shared one base sprite. The base sprite's
main ramp is remapped step for step onto the species' own palette ramp (fur_black for the black bear hide, fur_fox for
the fox pelt, berry_red for a sockeye...), so every species reads as itself in the pack with no new drawing.
Usage (from the repository root): python3 -I tools/artgen/species_tints.py"""
import os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "species_tints 0.1.0"
sys.path.insert(0, HERE)
import palette_lib as P

# target item: (base sprite, ramp the base's main ramp is remapped to)
TINTS = {
    # hides and pelts (base raw_hide, ramp hide_raw)
    "black_bear_hide": ("raw_hide", "fur_black"), "grizzly_hide": ("raw_hide", "fur_grizzly"), "moose_hide": ("raw_hide", "fur_moose"),
    "caribou_hide": ("raw_hide", "fur_calf"), "reindeer_hide": ("raw_hide", "fur_calf"), "mule_deer_hide": ("raw_hide", "fur_mule_winter"),
    "goat_hide": ("raw_hide", "fur_white"), "wolf_pelt": ("raw_hide", "fur_wolf"), "coyote_pelt": ("raw_hide", "fur_wolf_pale"),
    "red_fox_pelt": ("raw_hide", "fur_fox"), "lynx_pelt": ("raw_hide", "fur_mule_summer"), "wolverine_pelt": ("raw_hide", "fur_cinnamon"),
    "beaver_pelt": ("raw_hide", "fur_beaver"), "marten_pelt": ("raw_hide", "fur_tan"), "mink_pelt": ("raw_hide", "fur_black"),
    "muskrat_pelt": ("raw_hide", "fur_beaver"), "ermine_pelt": ("raw_hide", "fur_white"), "hare_pelt": ("raw_hide", "fur_hare_summer"),
    "red_squirrel_pelt": ("raw_hide", "fur_squirrel"), "antler": ("raw_hide", "bone"), "claw": ("raw_hide", "bone"), "marrow_bone": ("raw_hide", "bone"),
    # organs (base raw_moose_meat, ramp meat_raw)
    "heart": ("raw_moose_meat", "berry_cran"), "liver": ("raw_moose_meat", "comb_red"), "kidney": ("raw_moose_meat", "berry_lingon"), "tongue": ("raw_moose_meat", "meat_aging"),
    # fish (base arctic_grayling)
    "sockeye_salmon": ("arctic_grayling", "berry_red"), "chinook_salmon": ("arctic_grayling", "berry_lingon"), "coho_salmon": ("arctic_grayling", "hip_orange"),
    "chum_salmon": ("arctic_grayling", "lichen_pale"), "northern_pike": ("arctic_grayling", "moss_green"), "burbot": ("arctic_grayling", "soil"),
    "arctic_char": ("arctic_grayling", "comb_red"), "dolly_varden": ("arctic_grayling", "berry_cran"), "lake_trout": ("arctic_grayling", "sphagnum"),
    "sheefish": ("arctic_grayling", "snow"), "broad_whitefish": ("arctic_grayling", "fur_white"), "humpback_whitefish": ("arctic_grayling", "fur_white"),
    "round_whitefish": ("arctic_grayling", "bark_birch"), "least_cisco": ("arctic_grayling", "steel"), "bering_cisco": ("arctic_grayling", "steel"),
    "lake_chub": ("arctic_grayling", "rock_schist"), "longnose_sucker": ("arctic_grayling", "fur_tan"), "slimy_sculpin": ("arctic_grayling", "duff"),
    "ninespine_stickleback": ("arctic_grayling", "ice"), "alaska_blackfish": ("arctic_grayling", "fur_black"), "arctic_lamprey": ("arctic_grayling", "hide_raw"),
    # small-game meats (base raw_hare, ramp meat_raw) and birds (base raw_grouse)
    "beaver_meat": ("raw_hare", "meat_aging"), "muskrat_meat": ("raw_hare", "meat_aging"), "porcupine_meat": ("raw_hare", "fat_cream"),
}


def main_ramp(img):
    counts = {}
    for p in img.getdata():
        if p[3] == 0: continue
        r = P.COLOUR_RAMP.get(p[:3])
        if r and r[0] != "outline": counts[r[0]] = counts.get(r[0], 0) + 1
    return max(counts, key=counts.get)


def tint(base, ramp):
    img = Image.open(os.path.join(ASSETS, "textures", "item", base + ".png")).convert("RGBA")
    src = main_ramp(img)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p[3] == 0: continue
            r = P.COLOUR_RAMP.get(p[:3])
            if r and r[0] == src: out.putpixel((x, y), P.hexrgb(P.RAMPS[ramp][r[1]]) + (255,))
            else: out.putpixel((x, y), p)
    return out


def main():
    bad = 0
    for name, (base, ramp) in TINTS.items():
        img = tint(base, ramp)
        ok, n, ramps, probs = P.check_image(img)
        if not ok: bad += 1; print(name, probs)
        img.save(os.path.join(ASSETS, "textures", "item", name + ".png"))
        mp = os.path.join(ASSETS, "models", "item", name + ".json")
        with open(mp) as f: cur = f.read()
        if '"layer0"' in cur:
            import json
            m = json.loads(cur); m.setdefault("textures", {})["layer0"] = "yearofice:item/" + name
            with open(mp, "w") as f: f.write(json.dumps(m, indent=2) + "\n")
        else:
            with open(mp, "w") as f:
                f.write('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "yearofice:item/%s"\n  }\n}\n' % name)
    print(f"{len(TINTS)} tints, {bad} off-spec")
    return bad


if __name__ == "__main__":
    sys.exit(1 if main() else 0)
