"""modelgen import: measure a found glTF animal (Quaternius-style armature) into proportion ratios
that species files use as their base. The found mesh itself is never shipped: only these numbers,
which the species file scales to the animal's real shoulder height and then reproportions.

Usage: python3 -I tools/modelgen/import_glb.py <model.glb> <out measure.json> [--shoulder-m 0.8]"""
import json, sys, os
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gltf  # noqa: E402

GROUPS = {  # joint-name prefixes -> body part
    "torso": ("Body", "Back", "Torso", "Hips", "Spine", "Chest"),
    "neck": ("Neck",), "head": ("Head",), "ear": ("Ear",), "tail": ("Tail",),
    "front_leg": ("FrontShoulder", "FrontUpperLeg", "FrontLowerLeg", "FF."),
    "hind_leg": ("BackShoulder", "BackLeg", "BackUpperLeg", "BackLowerLeg", "FFB."),
}


def part(name):
    for g, pre in GROUPS.items():
        if any(name.startswith(p) for p in pre):
            return g
    return None


def measure(path, shoulder_m=None):
    g = gltf.load(path)
    P, J, _ = g.skinned_vertices()
    up = 1
    ext = P.max(0) - P.min(0)
    length_axis = 0 if ext[0] > ext[2] else 2
    groups = {}
    for gname in GROUPS:
        sel = np.array([part(j) == gname for j in J])
        if sel.any():
            groups[gname] = P[sel]
    ground = P[:, up].min()
    torso = groups["torso"]
    tl = np.percentile(torso[:, length_axis], [3, 97])
    torso_len = tl[1] - tl[0]
    shoulder = np.percentile(groups["front_leg"][:, up], 97) - ground if "front_leg" in groups else None
    back_top = np.percentile(torso[:, up], 97) - ground
    belly = np.percentile(torso[:, up], 3) - ground
    out = {"source": os.path.basename(path), "length_axis": "xyz"[length_axis],
           "torso_len_over_back_height": round(torso_len / back_top, 3),
           "belly_height_over_back_height": round(belly / back_top, 3),
           "chest_depth_over_back_height": round((back_top - belly) / back_top, 3)}
    for gname in ("neck", "head", "ear", "tail"):
        if gname in groups:
            q = groups[gname]
            e = q.max(0) - q.min(0)
            out[f"{gname}_extent_over_back_height"] = [round(v / back_top, 3) for v in (e[0], e[1], e[2])]
    if "head" in groups:
        out["head_top_over_back_height"] = round((np.percentile(groups["head"][:, up], 97) - ground) / back_top, 3)
    if shoulder_m:
        out["px_per_source_unit_at_shoulder"] = round(shoulder_m * 16 / back_top, 4)
        out["suggested_px"] = {"back_top": round(shoulder_m * 16, 2), "belly": round(belly / back_top * shoulder_m * 16, 2),
                               "torso_len": round(torso_len / back_top * shoulder_m * 16, 2)}
    return out


if __name__ == "__main__":
    a = sys.argv[1:]
    sm = float(a[a.index("--shoulder-m") + 1]) if "--shoulder-m" in a else None
    res = measure(a[0], sm)
    os.makedirs(os.path.dirname(a[1]) or ".", exist_ok=True)
    json.dump(res, open(a[1], "w"), indent=1)
    print(json.dumps(res, indent=1))
