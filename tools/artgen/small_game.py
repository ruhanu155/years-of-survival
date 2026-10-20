"""small_game: the M5 lane A art that is not an entity: the gut pile (blockstate, four stage models, texture) and the
item models of the nine new game meats and the pelts.

The gut pile is a flat mound on one 16x16 texture drawn from the palette's meat_raw ramp, deterministic from a fixed
seed. The meats reuse the art wave's sprites: a mammal's meat shows the hare's raw/aging/spoiled/cooked/burnt models
and a bird's the ptarmigan's, switching on the item property yearofice:look exactly as cooking.py's models do, until
each meat has its own icons. Pelts show the raw hide sprite.
Usage (from the repository root): python3 -I tools/artgen/small_game.py"""
import json, os, random, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "small_game 0.1.0"
sys.path.insert(0, HERE)

MAMMAL_MEATS = ["red_squirrel", "marten", "mink", "muskrat", "porcupine", "beaver"]
BIRD_MEATS = ["spruce_grouse", "ruffed_grouse", "rock_ptarmigan"]
PELTS = ["marten_pelt", "mink_pelt", "muskrat_pelt", "red_squirrel_pelt", "ermine_pelt", "beaver_pelt"]


def item_models():
    out = {}
    for name in MAMMAL_MEATS + BIRD_MEATS:
        src = "willow_ptarmigan_meat" if name in BIRD_MEATS else "snowshoe_hare_meat"
        with open(os.path.join(ASSETS, "models", "item", src + ".json")) as f:
            out[f"models/item/{name}_meat.json"] = f.read()
    for name in PELTS:
        out[f"models/item/{name}.json"] = '{\n "parent": "yearofice:item/raw_hide"\n}\n'
    return out


def block_files():
    out = {"blockstates/gut_pile.json": json.dumps({"variants": {f"age={i}": {"model": f"yearofice:block/gut_pile_{i}"} for i in range(4)}}, indent=2)}
    for i in range(4):
        h = [1.5, 2, 2, 1][i]
        out[f"models/block/gut_pile_{i}.json"] = json.dumps({
            "parent": "minecraft:block/block",
            "textures": {"all": "yearofice:block/gut_pile", "particle": "yearofice:block/gut_pile"},
            "elements": [{"from": [1 + i, 0, 1 + i], "to": [15 - i, h * 1.5, 15 - i],
                          "faces": {k: {"uv": [0, 0, 16, 16], "texture": "#all"} for k in ("north", "south", "east", "west", "up", "down")}}]}, indent=2)
    return out


def outputs():
    return ["assets/yearofice/" + k for k in sorted(list(item_models()) + list(block_files()) + ["textures/block/gut_pile.png"])]


def gut_pile_texture():
    import palette_lib as P
    from PIL import Image
    cols = [P.hexrgb(h) for h in P.RAMPS["meat_raw"]]
    rnd = random.Random(2026)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), tuple(cols[rnd.choice([0, 1, 1, 2, 2, 3])]) + (255,))
    return im


if __name__ == "__main__":
    for rel, text in {**item_models(), **block_files()}.items():
        p = os.path.join(ASSETS, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w") as f:
            f.write(text if text.endswith("\n") else text + "\n")
    p = os.path.join(ASSETS, "textures", "block", "gut_pile.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    gut_pile_texture().save(p)
    print(f"small_game: wrote {len(outputs())} files")
