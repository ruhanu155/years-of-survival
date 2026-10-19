"""world_blocks: block models and blockstates for the M2 world blocks (Minecraft development › Registry table:
terrain, water, ice and snow; plants and trees), on vanilla parent models only (no vanilla textures). Textures
come from the block recipes under recipes/block/. Writes every file and lists them for credits.py.

Usage (from the repository root): python3 -I tools/artgen/world_blocks.py"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
AS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
VERSION = "world_blocks 1.0"
WRITTEN = []


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(json.dumps(obj, indent=2) + "\n")
    WRITTEN.append(os.path.relpath(path, os.path.join(ROOT, "src", "main", "resources")).replace(os.sep, "/"))


def build():
    WRITTEN.clear()
    T=lambda n:'yearofice:block/'+n
    ALL=['deep_ground','deep_rock','face_rock','iron_outcrop','coal_seam','rust_stain','silt','loess','till','river_sand','gravel','peat','clay_bank','frozen_soil','scree_stones']
    SURF=['forest_duff','moss_mat','grass_turf','tundra_turf','lichen_mat']
    LOGS=['white_spruce_log','black_spruce_log','birch_log']
    LEAVES=['white_spruce_leaves','black_spruce_leaves','birch_leaves','willow_brush']
    def simple(n, model): w(AS + '/blockstates/%s.json'%n, {"variants":{"":{"model":"yearofice:block/"+model}}})
    for n in ALL:
        w(AS + '/models/block/%s.json'%n, {"parent":"minecraft:block/cube_all","textures":{"all":T(n+'_all')}})
        simple(n,n)
    for n in SURF:
        w(AS + '/models/block/%s.json'%n, {"parent":"minecraft:block/cube_bottom_top","textures":{"top":T(n+'_top'),"side":T(n+'_side'),"bottom":T('silt_all')}})
        simple(n,n)
    def pillar(n, model):
        w(AS + '/blockstates/%s.json'%n, {"variants":{"axis=y":{"model":model},"axis=z":{"model":model,"x":90},"axis=x":{"model":model,"x":90,"y":90}}})
    for n in LOGS:
        w(AS + '/models/block/%s.json'%n, {"parent":"minecraft:block/cube_column","textures":{"end":T(n+'_top'),"side":T(n+'_side')}})
        pillar(n,'yearofice:block/'+n)
    w(AS + '/models/block/fallen_log.json', {"parent":"minecraft:block/cube_column","textures":{"end":T('fallen_log_top'),"side":T('fallen_log_side')}})
    pillar('fallen_log','yearofice:block/fallen_log')
    for n in LEAVES:
        w(AS + '/models/block/%s.json'%n, {"parent":"minecraft:block/cube_all","render_type":"minecraft:cutout_mipped","textures":{"all":T(n+'_all')}})
        simple(n,n)
    def el(fr,to,tex,uv=None):
        faces={}
        for f in ['north','south','east','west','up','down']:
            faces[f]={"texture":"#"+tex[f] if isinstance(tex,dict) else "#"+tex}
        return {"from":fr,"to":to,"faces":faces}
    w(AS + '/models/block/riffle_cobble.json', {"parent":"minecraft:block/block","textures":{"particle":T('riffle_cobble_all'),"t":T('riffle_cobble_all')},
      "elements":[el([0,0,0],[16,6,16],"t")]})
    simple('riffle_cobble','riffle_cobble')
    w(AS + '/models/block/shrub_stem.json', {"parent":"minecraft:block/block","render_type":"minecraft:cutout","textures":{"particle":T('shrub_stem_all'),"t":T('shrub_stem_all')},
      "elements":[el([6,0,6],[10,16,10],"t")]})
    simple('shrub_stem','shrub_stem')
    st={"north":"s","south":"s","east":"s","west":"s","up":"e","down":"e"}
    w(AS + '/models/block/stump.json', {"parent":"minecraft:block/block","textures":{"particle":T('stump_side'),"s":T('stump_side'),"e":T('stump_top')},
      "elements":[el([1,0,1],[15,10,15],st)]})
    w(AS + '/blockstates/stump.json', {"variants":{"age=%d"%a:{"model":"yearofice:block/stump"} for a in range(4)}})
    for k in range(1,9):
        if k<8: w(AS + '/models/block/snow_cover_%d.json'%k, {"parent":"minecraft:block/snow_height%d"%(2*k),"textures":{"particle":T('snow_cover_all'),"texture":T('snow_cover_all')}})
    w(AS + '/models/block/snow_cover_8.json', {"parent":"minecraft:block/cube_all","textures":{"all":T('snow_cover_all')}})
    w(AS + '/blockstates/snow_cover.json', {"variants":{"layers=%d"%k:{"model":"yearofice:block/snow_cover_%d"%k} for k in range(1,9)}})
    return list(WRITTEN)


def outputs():
    return build()


if __name__ == "__main__":
    print(len(build()), "files")
