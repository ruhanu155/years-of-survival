#!/usr/bin/env python3
"""Placeholder art for the M6 hunt ids (bows, arrow, arrowhead, spear, calls, blind, stand, blood). Original 16x16 pixel sketches drawn here;
writes textures, models, blockstates, block loot tables, lang keys and credits entries. Idempotent. Run from the repo root."""
import json, os
from PIL import Image, ImageDraw

A = "src/main/resources/assets/yearofice"
D = "src/main/resources/data/yearofice"

def px(name, kind, draw):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im); draw(d)
    os.makedirs(f"{A}/textures/{kind}", exist_ok=True); im.save(f"{A}/textures/{kind}/{name}.png")

WOOD, DARK, STRING, STONE, BONE, STEEL, RED = (140, 98, 57), (92, 64, 38), (222, 214, 190), (120, 120, 126), (226, 218, 196), (170, 176, 186), (140, 20, 20)

def bow(col):
    def f(d):
        d.arc((1, 0, 15, 15), 270, 90, fill=col, width=2); d.line((8, 1, 8, 14), fill=STRING)
    return f

def arrow(d):
    d.line((2, 13, 13, 2), fill=WOOD, width=1); d.polygon([(13, 2), (15, 1), (14, 4)], fill=STONE); d.line((2, 13, 1, 11), fill=STRING); d.line((3, 14, 1, 14), fill=STRING)

def spear(d):
    d.line((2, 14, 12, 4), fill=WOOD, width=2); d.polygon([(12, 4), (15, 0), (14, 5)], fill=STONE)

def call(d):
    d.polygon([(2, 12), (6, 4), (14, 8), (13, 13)], fill=(196, 176, 140)); d.line((6, 4, 14, 8), fill=DARK)

def squeak(d):
    d.rectangle((6, 3, 9, 13), fill=(196, 176, 140)); d.line((7, 3, 7, 13), fill=DARK)

def head(d):
    d.polygon([(8, 1), (12, 11), (8, 14), (4, 11)], fill=STONE); d.line((8, 1, 8, 14), fill=DARK)

ITEMS = {"self_bow": bow(WOOD), "kit_bow": bow(DARK), "longbow": bow((120, 84, 48)), "spear": spear, "arrow": arrow, "arrowhead": head,
         "moose_call": call, "squeak_call": squeak}
NAMES = {"self_bow": "Self Bow", "kit_bow": "Kit Bow", "longbow": "Longbow", "spear": "Spear", "arrow": "Arrow", "arrowhead": "Arrowhead",
         "moose_call": "Moose Call", "squeak_call": "Squeak Call", "pole_blind": "Pole Blind", "tree_stand": "Tree Stand", "blood_trail": "Blood"}

def js(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, "w"), indent=1)

entries = []
def credit(aid, files):
    entries.append({"asset_id": aid, "files": files, "source_type": "generated", "licence": "project",
                    "note": "Original pixel art drawn for Year of Ice (M6 lane B hunt), no found assets", "generator_version": "hand script (PIL)",
                    "recipe": "tools/artgen/hunt_placeholders.py", "release_blocker": False})

for n, f in ITEMS.items():
    px(n, "item", f)
    js(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"yearofice:item/{n}"}})
    credit(f"tex_{n}", [f"assets/yearofice/textures/item/{n}.png", f"assets/yearofice/models/item/{n}.json"])

def tex_block(name, fill, line):
    def f(d):
        d.rectangle((0, 0, 15, 15), fill=fill)
        for i in range(0, 16, 4): d.line((i, 0, i, 15), fill=line)
    px(name, "block", f)

tex_block("pole_blind", (58, 92, 52), (36, 62, 34))
tex_block("tree_stand", WOOD, DARK)
js(f"{A}/models/block/pole_blind.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "yearofice:block/pole_blind"}})
js(f"{A}/models/block/tree_stand.json", {"parent": "minecraft:block/block", "textures": {"all": "yearofice:block/tree_stand", "particle": "yearofice:block/tree_stand"},
   "elements": [{"from": [0, 0, 0], "to": [16, 8, 16], "faces": {k: {"texture": "#all"} for k in ["down", "up", "north", "south", "east", "west"]}}]})
for b in ("pole_blind", "tree_stand"):
    js(f"{A}/models/item/{b}.json", {"parent": f"yearofice:block/{b}"})
    js(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"yearofice:{b}"}],
       "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": f"yearofice:blocks/{b}"})
js(f"{A}/blockstates/pole_blind.json", {"variants": {"fresh=true": {"model": "yearofice:block/pole_blind"}, "fresh=false": {"model": "yearofice:block/pole_blind"}}})
js(f"{A}/blockstates/tree_stand.json", {"variants": {"": {"model": "yearofice:block/tree_stand"}}})
credit("tex_pole_blind", ["assets/yearofice/textures/block/pole_blind.png", "assets/yearofice/models/block/pole_blind.json", "assets/yearofice/models/item/pole_blind.json", "assets/yearofice/blockstates/pole_blind.json"])
credit("tex_tree_stand", ["assets/yearofice/textures/block/tree_stand.png", "assets/yearofice/models/block/tree_stand.json", "assets/yearofice/models/item/tree_stand.json", "assets/yearofice/blockstates/tree_stand.json"])

bf, bfiles, variants = None, [], {}
for kind, col in (("bright", (190, 16, 24, 230)), ("dark", (96, 12, 16, 230)), ("gut", (92, 80, 28, 230))):
    def f(d, col=col):
        d.rectangle((0, 0, 15, 15), fill=(0, 0, 0, 0)); d.ellipse((2, 3, 13, 12), fill=col); d.ellipse((5, 5, 10, 9), fill=(col[0] // 2, col[1], col[2], 230))
    px(f"blood_{kind}", "block", f)
    bfiles.append(f"assets/yearofice/textures/block/blood_{kind}.png")
    for amt, (lo, hi) in ((1, (6, 10)), (2, (4, 12)), (3, (2, 14))):
        js(f"{A}/models/block/blood_{kind}_{amt}.json", {"parent": "minecraft:block/block", "textures": {"all": f"yearofice:block/blood_{kind}", "particle": f"yearofice:block/blood_{kind}"},
           "elements": [{"from": [lo, 0, lo], "to": [hi, 0.4, hi], "faces": {"up": {"texture": "#all"}, "down": {"texture": "#all"}}}]})
        bfiles.append(f"assets/yearofice/models/block/blood_{kind}_{amt}.json")
        variants[f"amount={amt},kind={kind}"] = {"model": f"yearofice:block/blood_{kind}_{amt}"}
js(f"{A}/blockstates/blood_trail.json", {"variants": variants})
js(f"{A}/models/item/blood_trail.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "yearofice:block/blood_bright"}})
credit("tex_blood_trail", bfiles + ["assets/yearofice/blockstates/blood_trail.json", "assets/yearofice/models/item/blood_trail.json"])

# credits (append-only) and lang (append-only)
cp = f"{A}/credits.json"; c = json.load(open(cp)); have = {e.get("asset_id") for e in c["entries"]}
for e in entries:
    if e["asset_id"] not in have: c["entries"].append(e)
json.dump(c, open(cp, "w"), indent=2)
lp = f"{A}/lang/en_us.json"; s = open(lp).read().rstrip()
add = {f"item.yearofice.{k}": v for k, v in NAMES.items() if k not in ("pole_blind", "tree_stand", "blood_trail")}
add.update({"block.yearofice.pole_blind": "Pole Blind", "block.yearofice.tree_stand": "Tree Stand", "block.yearofice.blood_trail": "Blood", "entity.yearofice.arrow": "Arrow"})
cur = json.loads(s)
extra = {k: v for k, v in add.items() if k not in cur}
if extra:
    body = s[: s.rindex("}")].rstrip()
    body += "".join(f',\n  {json.dumps(k)}: {json.dumps(v)}' for k, v in extra.items()) + "\n}\n"
    open(lp, "w").write(body)
