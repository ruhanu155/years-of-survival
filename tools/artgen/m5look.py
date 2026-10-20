"""m5look: M5 art for the food, fishing and trap ids that shipped without a model. Writes artgen recipes
(item icons, block tiles) and the block models, blockstates and item models they need, then run
`python3 -I tools/artgen/artgen.py build` to draw the textures. Original art, palette ramps only; no credits
entries because nothing is found or imported. Usage: python3 -I tools/artgen/m5look.py && python3 -I tools/artgen/artgen.py build"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")


def put(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, "w"), indent=1)


def sh(kind, ramp, step, **kw):
    return {"kind": kind, "ramp": ramp, "step": step, **kw}


def icon(name, shapes, folder="item"):
    put(os.path.join(HERE, "recipes", "item", name + ".json"), {"id": "yearofice:" + name, "kind": "icon", "shapes": shapes})


def sp(ramp, step, density):
    return {"ramp": ramp, "step": step, "density": density}


def cup(name, liquid_ramp, liquid_step):
    icon(name, [sh("rect", "bark_birch", 3, box=[4, 6, 11, 13]),
                sh("rect", "bark_spruce", 2, box=[4, 6, 11, 6]),
                sh("rect", liquid_ramp, liquid_step, box=[5, 6, 10, 8]),
                sh("line", "bark_birch", 2, points=[[11, 8], [13, 8], [13, 11], [11, 11]])])


def items():
    icon("hen_egg", [sh("ellipse", "fat_cream", 3, box=[4, 2, 11, 13], speckle=sp("bone", 2, 0.12))])
    icon("goat_milk", [sh("rect", "hide_raw", 2, box=[4, 5, 11, 13]), sh("rect", "bark_spruce", 2, box=[6, 2, 9, 5]),
                       sh("rect", "fat_cream", 4, box=[5, 6, 10, 8], flat=True), sh("line", "bark_spruce", 1, points=[[4, 9], [11, 9]])])
    icon("barley_flour", [sh("poly", "fat_cream", 2, points=[[3, 6], [12, 6], [13, 13], [2, 13]], speckle=sp("fat_cream", 4, 0.3)),
                          sh("rect", "fat_cream", 3, box=[5, 3, 10, 6]), sh("line", "bark_spruce", 2, points=[[5, 6], [10, 6]])])
    icon("sauerkraut", [sh("ellipse", "bark_tamarack", 2, box=[2, 7, 13, 14]), sh("ellipse", "leaf_silver", 3, box=[3, 6, 12, 10], speckle=sp("lichen_pale", 4, 0.3))])
    cup("birch_sap", "ice", 4)
    cup("birch_syrup", "rust", 3)
    icon("dish", [sh("ellipse", "wood_cut", 2, box=[1, 6, 14, 14]), sh("ellipse", "meat_cooked", 2, box=[3, 6, 12, 10]),
                  sh("ellipse", "fat_cream", 3, box=[5, 7, 8, 8], flat=True)])
    icon("gold_pan", [sh("ellipse", "iron", 2, box=[1, 4, 13, 13]), sh("ellipse", "iron", 1, box=[3, 6, 11, 11]),
                      sh("line", "iron", 3, points=[[12, 3], [14, 1]], width=2), sh("pixels", "rust", 4, points=[[6, 8], [8, 9], [5, 9]], flat=True)])
    icon("birch_pan", [sh("ellipse", "bark_birch", 3, box=[1, 4, 13, 13]), sh("ellipse", "bark_birch", 1, box=[3, 6, 11, 11], speckle=sp("gravel", 3, 0.2)),
                       sh("line", "bark_spruce", 2, points=[[12, 3], [14, 1]], width=2)])
    icon("black_sand", [sh("poly", "fur_black", 3, points=[[2, 13], [5, 8], [8, 5], [11, 8], [14, 13]], speckle=sp("gravel", 3, 0.25)),
                        sh("pixels", "rust", 4, points=[[7, 9], [10, 11], [5, 11]], flat=True)])


def tile(name, ramp, steps, scale=2, bark=False, extra=None):
    layers = [{"type": "noise", "ramp": ramp, "scale": scale, "octaves": 3, "steps": steps}]
    if bark:
        layers[0]["stretch"] = "vertical"
        layers.append({"type": "furrows", "spacing": 4, "jitter": 1, "step": 0})
    if extra:
        layers.append(extra)
    put(os.path.join(HERE, "recipes", "block", name + ".json"),
        {"id": "yearofice:" + name, "kind": "block", "class": "ground", "faces": {"all": {"layers": layers}}, "folder": "block"})


def cube(frm, to, tex, rot=None):
    c = {"from": frm, "to": to, "faces": {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "up", "down")}}
    if rot:
        c["rotation"] = rot
    return c


def model(name, textures, elements):
    put(os.path.join(ASSETS, "models", "block", name + ".json"),
        {"parent": "minecraft:block/block", "textures": {"particle": next(iter(textures.values())), **textures}, "elements": elements})


def blocks():
    tile("rack_pole", "wood_cut", [1, 3], bark=True)
    tile("smoke_rack_pole", "bark_spruce", [0, 3], bark=True)
    tile("deadfall_stone", "rock_schist", [1, 4], scale=3, extra={"type": "speckle", "ramp": "rock_schist", "step": 0, "density": 0.06})
    tile("placer_gravel", "gravel", [1, 4], scale=4, extra={"type": "speckle", "ramp": "fur_black", "step": 2, "density": 0.18})
    # food_cache: side bark, top rings (same layers as the stump recipe)
    put(os.path.join(HERE, "recipes", "block", "food_cache.json"), {"id": "yearofice:food_cache", "kind": "block", "class": "bark", "faces": {
        "side": {"layers": [{"type": "noise", "ramp": "bark_spruce", "scale": 2, "octaves": 3, "steps": [1, 4], "stretch": "vertical"}, {"type": "furrows", "spacing": 4, "jitter": 1, "step": 0}]},
        "top": {"layers": [{"type": "rings", "ramp": "wood_cut", "spacing": 1.6, "steps": [1, 3], "bark_ramp": "bark_spruce"}]}}, "folder": "block"})
    T = lambda n: "yearofice:block/" + n

    def rack(name, tex, h):
        el = [cube([1, 0, 1], [3, h, 3], "p"), cube([13, 0, 1], [15, h, 3], "p"), cube([1, 0, 13], [3, h, 15], "p"), cube([13, 0, 13], [15, h, 15], "p"),
              cube([1, h - 2, 1], [15, h, 3], "p"), cube([1, h - 2, 13], [15, h, 15], "p"),
              cube([1, h - 5, 1], [3, h - 3, 15], "p"), cube([13, h - 5, 1], [15, h - 3, 15], "p")]
        el += [cube([x, h - 1, 3], [x + 1, h, 13], "p") for x in (4, 7, 10)]
        model(name, {"p": T(tex + "_all")}, el)
        put(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "yearofice:block/" + name}}})
    rack("drying_rack", "rack_pole", 14)
    rack("smoke_rack", "smoke_rack_pole", 16)
    stick = "yearofice:block/fallen_log_side"
    model("food_cache", {"s": T("food_cache_side"), "t": T("food_cache_top")}, [
        {"from": [1, 0, 2], "to": [15, 9, 14], "faces": {"north": {"texture": "#s"}, "south": {"texture": "#s"}, "east": {"texture": "#s"}, "west": {"texture": "#s"}, "up": {"texture": "#t"}, "down": {"texture": "#t"}}},
        {"from": [0, 9, 1], "to": [16, 11, 15], "faces": {"north": {"texture": "#s"}, "south": {"texture": "#s"}, "east": {"texture": "#s"}, "west": {"texture": "#s"}, "up": {"texture": "#t"}, "down": {"texture": "#t"}}},
        {"from": [4, 11, 6], "to": [12, 12, 10], "faces": {f: {"texture": "#s"} for f in ("north", "south", "east", "west", "up")}}])
    put(os.path.join(ASSETS, "blockstates", "food_cache.json"), {"variants": {"": {"model": "yearofice:block/food_cache"}}})

    def deadfall(name, k, mouse):
        s = k  # scale of the slab
        lo, hi = 8 - 7 * s, 8 + 7 * s
        up = [cube([lo, 4, lo], [hi, 4 + 3 * s, hi], "st", {"origin": [8, 4, 8], "axis": "x", "angle": 22.5}),
              cube([7.5, 0, 7.5], [8.5, 5, 8.5], "k"), cube([7.5, 0, 4], [8.5, 1, 7.5], "k"), cube([7.5, 3.5, 4.5], [8.5, 4.5, 8], "k")]
        down = [cube([lo, 0, lo], [hi, 3 * s, hi], "st"), cube([7.5, 0, 3], [8.5, 1, 5], "k"), cube([7.5, 0, 11], [8.5, 1, 13], "k")]
        tx = {"st": T("deadfall_stone_all"), "k": stick}
        model(name, tx, up)
        model(name + "_sprung", tx, down)
        put(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"sprung=false": {"model": "yearofice:block/" + name}, "sprung=true": {"model": "yearofice:block/" + name + "_sprung"}}})
        put(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "yearofice:block/" + name})
    deadfall("deadfall", 1.0, False)
    deadfall("mouse_deadfall", 0.45, True)
    # snare funnel: two rows of stakes narrowing to a gap, a guide fence
    st = []
    for i in range(6):
        st.append(cube([1 + i * 1.6, 0, 2 + i * 1.3], [2 + i * 1.6, 7, 3 + i * 1.3], "k"))
        st.append(cube([14 - i * 1.6, 0, 2 + i * 1.3], [15 - i * 1.6, 7, 3 + i * 1.3], "k"))
    st.append(cube([5, 5, 9.5], [11, 6, 10.5], "k"))
    model("snare_funnel", {"k": stick}, st)
    put(os.path.join(ASSETS, "blockstates", "snare_funnel.json"), {"variants": {"": {"model": "yearofice:block/snare_funnel"}}})
    put(os.path.join(ASSETS, "models", "item", "snare_funnel.json"), {"parent": "yearofice:block/snare_funnel"})
    model("placer_gravel", {"a": T("placer_gravel_all")}, [cube([0, 0, 0], [16, 16, 16], "a")])
    put(os.path.join(ASSETS, "blockstates", "placer_gravel.json"), {"variants": {"": {"model": "yearofice:block/placer_gravel"}}})
    put(os.path.join(ASSETS, "models", "item", "placer_gravel.json"), {"parent": "yearofice:block/placer_gravel"})


if __name__ == "__main__":
    items()
    blocks()
