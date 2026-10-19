"""modelgen: rig template + species parameter file -> GeckoLib .geo.json, painted textures and
.animation.json, plus the seven checks of the Art spec (The model generator > Validation).

Usage (from the repository root):
  python3 -I tools/modelgen/modelgen.py build [species ...]   write every species (or the named ones)
  python3 -I tools/modelgen/modelgen.py check                 validate what is on disk, exit 1 on failure

Units: species files are in model pixels, 16 px = 1 m = 1 block, the same texel size as blocks.
Coordinates: y up from the ground, the animal faces -Z (north), +X is its right side.
Every random draw seeds from SHA-256 of (species, cube, face, layer) so output is byte-identical."""
import json, os, sys, math, hashlib, fnmatch, copy
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "artgen"))
import geo as G  # noqa: E402
import palette_lib as P  # noqa: E402

VERSION = "modelgen 0.4.0"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
SPECIES_DIR = os.path.join(HERE, "species")
TIERS = {  # cubes, bones, atlas, check distance m
    "tiny": (12, 10, (32, 32), 3), "small": (24, 20, (64, 32), 8), "medium": (40, 26, (64, 64), 15),
    "large": (60, 32, (128, 128), 30), "huge": (80, 40, (256, 128), 60)}
LEG_BONES = [f"leg_{l}_{s}" for l in ("fl", "fr", "hl", "hr") for s in ("upper", "lower", "foot")]
RIG_BONES = {
    "ursid": ["root", "body", "chest", "hips", "hump", "neck", "head", "jaw", "ear_l", "ear_r", "tail", "mark"] + LEG_BONES,
    "ungulate": ["root", "body", "chest", "hips", "hump", "neck", "head", "jaw", "ear_l", "ear_r", "tail", "mark"] + LEG_BONES,
    "cervid": ["root", "body", "chest", "hips", "hump", "neck", "neck_swell", "head", "jaw", "ear_l", "ear_r", "tail_1", "tail_2",
               "mark"] + LEG_BONES,
    "canid": ["root", "body", "chest", "hips", "neck", "head", "jaw", "ear_l", "ear_r", "tail_1", "tail_2", "mark"] + LEG_BONES,
    "large_rodent": ["root", "body", "hips", "head", "jaw", "ear_l", "ear_r", "tail", "mark"] + LEG_BONES,
    "small_rodent": ["root", "body", "hips", "head", "ear_l", "ear_r", "mark", "leg_fl_upper", "leg_fr_upper",
                     "leg_hl_upper", "leg_hr_upper", "leg_hl_foot", "leg_hr_foot"],
    "grouse": ["root", "body", "neck", "head", "beak", "tail", "wing_l", "wing_r", "wing_l_tip", "wing_r_tip",
               "leg_l", "leg_r", "mark"],
}
BONE_CAP = {"cervid": 38}  # the deer sheet (T21) sets 38 bones for the cervid rig, over the Large tier's 32
SHARED_SET = {  # the rig's shared animation set (check 6)
    "cervid": ["idle", "walk", "run", "graze", "alert", "flee", "swim", "death"],
    "ursid": ["idle", "walk", "run", "forage", "alert", "swim", "death"],
    "ungulate": ["idle", "walk", "run", "graze", "alert", "flee", "swim", "death"],
    "canid": ["idle", "walk", "run", "forage", "alert", "death"],
    "large_rodent": ["idle", "walk", "run", "forage", "alert", "swim", "death"],
    "small_rodent": ["idle", "walk", "run", "forage", "alert", "flee", "death"],
    "grouse": ["idle", "walk", "run", "forage", "alert", "fly", "glide", "death"],
}


def seed(*parts):
    return int(hashlib.sha256("|".join(map(str, parts)).encode()).hexdigest()[:8], 16)


# ----------------------------------------------------------------------------- model building
class Model:
    def __init__(self, gid, tier, rig):
        self.gid, self.tier, self.rig = gid, tier, rig
        self.bones, self.by = [], {}

    def bone(self, name, parent=None, pivot=(0, 0, 0), rot=None):
        b = {"name": name, "pivot": [round(float(v), 3) for v in pivot], "_cubes": []}
        if parent:
            b["parent"] = parent
        if rot and any(rot):
            b["rotation"] = [round(float(v), 3) for v in rot]
        self.bones.append(b)
        self.by[name] = b
        return b

    def cube(self, bone, origin, size, paint, share=None, mirror=False, key=None, rot=None, rpivot=None):
        # sizes round up so neighbouring parts overlap instead of leaving half-pixel gaps
        size = [max(1, int(math.ceil(float(v) - 0.01))) for v in size]
        c = {"origin": [round(float(v) * 2) / 2 for v in origin], "size": size, "paint": paint,
             "share": share, "mirror": mirror, "key": key or f"{bone}#{len(self.by[bone]['_cubes'])}"}
        if rot:
            c["rotation"] = [round(float(v), 3) for v in rot]
            c["pivot"] = [round(float(v), 3) for v in rpivot]
        self.by[bone]["_cubes"].append(c)
        return c

    def cubes(self):
        for b in self.bones:
            for c in b["_cubes"]:
                yield b, c


def box(x0, x1, y0, y1, z0, z1):
    """origin, size from extents"""
    return [x0, y0, z0], [x1 - x0, y1 - y0, z1 - z0]


def centred(w, y0, h, z0, d, x=0.0):
    return [x - w / 2, y0, z0], [w, h, d]


def paint_for(p, bone, default=None):
    """Species paint map: bone glob -> paint dict. First match in file order wins."""
    for pat, val in p.get("paint", {}).items():
        if fnmatch.fnmatch(bone, pat):
            return copy.deepcopy(val)
    return copy.deepcopy(default or p["paint"].get("*"))


def quadruped(p):
    """Mammal template. Torso segments, hump, neck, head (skull, muzzle, nose, jaw, ears, bell,
    antlers), four three-part legs and a tail chain, all placed from the species file."""
    m = Model(p["geo_id"], p["tier"], p["rig"])
    rig = p["rig"]
    t = p["torso"]
    hip = p["legs"]["hind"]
    front = p["legs"]["front"]
    body_pivot = [0, hip["top"], hip["z"]]
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", body_pivot)
    has_chest = "chest" in RIG_BONES[rig]
    if has_chest:
        m.bone("chest", "body", [0, front["top"], front["z"]])
    m.bone("hips", "body", [0, hip["top"], hip["z"]])
    if "hump" in RIG_BONES[rig]:
        m.bone("hump", "chest" if has_chest else "body", [0, front["top"], front["z"]])
    for seg in t:
        bn = seg.get("bone", "body")
        o, s = centred(seg["w"], seg["y"][0], seg["y"][1] - seg["y"][0], seg["z"][0], seg["z"][1] - seg["z"][0])
        m.cube(bn, o, s, seg.get("paint") or paint_for(p, bn))
    # neck and head
    nk = p.get("neck")
    head_parent = "body"
    hd = p["head"]
    if nk:
        npv = [0, nk["y0"], nk["z0"]]
        m.bone("neck", "chest" if has_chest else "body", npv, [-nk["angle"], 0, 0])
        o, s = centred(nk["w"], nk["y0"] - nk["h"] / 2, nk["h"], nk["z0"] - nk["len"], nk["len"] + nk.get("overlap", 1))
        neck_bone = "neck"
        if "neck_swell" in RIG_BONES[rig]:
            m.bone("neck_swell", "neck", [0, nk["y0"], nk["z0"] - nk["len"] / 2])
            neck_bone = "neck_swell"
        m.cube(neck_bone, o, s, paint_for(p, "neck"))
        for extra in nk.get("ruff", []):
            o, s = centred(extra["w"], nk["y0"] - nk["h"] / 2 + extra.get("dy", 0), extra["h"],
                           nk["z0"] - nk["len"] + extra.get("dz", 0), extra["len"])
            m.cube(neck_bone, o, s, extra.get("paint") or paint_for(p, "neck"))
        hp = [0, nk["y0"], nk["z0"] - nk["len"]]
        head_parent = "neck"
        head_rot = nk["angle"] + hd.get("pitch", 0)
    else:
        hp = hd["pivot"]
        head_rot = hd.get("pitch", 0)
    m.bone("head", head_parent, hp, [head_rot, 0, 0])
    sk = hd["skull"]
    sk_front = hp[2] - sk["len"] + sk.get("back", 1)
    sk_top = hp[1] + sk.get("dy", 0) + sk["h"] / 2
    o, s = centred(sk["w"], sk_top - sk["h"], sk["h"], sk_front, sk["len"])
    skull_paint = paint_for(p, "head")
    eye = hd.get("eye")
    if eye:
        skull_paint.setdefault("pixels", []).append({"faces": "sides", "z": eye["z"], "y": eye["y"],
                                                     "ramp": eye["ramp"], "step": eye["step"]})
        if eye.get("shine"):
            skull_paint["pixels"].append({"faces": "sides", "z": eye["z"] + eye["shine"][0], "y": eye["y"] + eye["shine"][1],
                                          "ramp": eye["shine"][2], "step": eye["shine"][3]})
    m.cube("head", o, s, skull_paint)
    for extra in hd.get("skull_extra", []):
        o, s = centred(extra["w"], sk_top - sk["h"] + extra.get("dy", 0), extra["h"], sk_front + extra.get("dz", 0), extra["len"])
        m.cube("head", o, s, extra.get("paint") or paint_for(p, "head"))
    mz = hd.get("muzzle")
    mz_front = sk_front
    if mz:
        mtop = sk_top + mz.get("dy", 0)
        mz_front = sk_front - mz["len"]
        o, s = centred(mz["w"], mtop - mz["h"], mz["h"], mz_front, mz["len"] + 1)
        m.cube("head", o, s, paint_for(p, "muzzle") if "muzzle" in "".join(p["paint"]) else paint_for(p, "head"))
        for extra in mz.get("extra", []):
            o, s = centred(extra["w"], mtop - mz["h"] + extra.get("dy", 0), extra["h"], mz_front + extra.get("dz", 0), extra["len"])
            m.cube("head", o, s, extra.get("paint") or paint_for(p, "muzzle"))
        nose = hd.get("nose")
        if nose:
            o, s = centred(nose["w"], mtop - nose["h"] + nose.get("dy", 0), nose["h"], mz_front - nose.get("len", 0.5) + nose.get("dz", 0), nose.get("len", 1))
            m.cube("head", o, s, nose["paint"])
    jw = hd.get("jaw")
    if jw:
        jy = sk_top + (mz.get("dy", 0) if mz else 0) - (mz["h"] if mz else sk["h"])
        m.bone("jaw", "head", [0, jy, sk_front + jw.get("pivot_dz", 2)])
        o, s = centred(jw["w"], jy - jw["h"] + jw.get("dy", 0), jw["h"], mz_front + jw.get("dz", 0), jw["len"])
        m.cube("jaw", o, s, paint_for(p, "jaw"))
    er = hd["ears"]
    for side, sx in (("l", -1), ("r", 1)):
        ex = sx * er["x"]
        ez = sk_front + er["z"]
        ey = sk_top + er.get("dy", 0)
        m.bone(f"ear_{side}", "head", [ex, ey, ez + er["d"] / 2], [er.get("lay", 0), 0, sx * er.get("out", 0)])
        o, s = centred(er["w"], ey, er["h"], ez, er["d"], x=ex)
        m.cube(f"ear_{side}", o, s, paint_for(p, "ear_*"), share=("ear_r#0" if side == "l" else None), mirror=(side == "l"))
    bl = hd.get("bell")
    if bl:
        by = sk_top - sk["h"] + bl.get("dy", 0)
        bz = sk_front + bl["z"]
        m.bone("bell", "head", [0, by, bz])
        o, s = centred(bl["w"], by - bl["h"], bl["h"], bz, bl["d"])
        m.cube("bell", o, s, paint_for(p, "bell"))
    an = hd.get("antlers")
    if an:
        build_antlers(m, p, an, sk_front, sk_top, sk["w"])
    m.bone("mark", "head", [0, sk_top + 1, sk_front + sk["len"] / 2])
    # legs
    for leg, spec in (("fl", front), ("fr", front), ("hl", hip), ("hr", hip)):
        sx = -1 if leg.endswith("l") else 1
        x = sx * spec["x"]
        z = spec["z"]
        top = spec["top"]
        up, lo, ft = spec["upper"], spec["lower"], spec["foot"]
        parent = ("chest" if has_chest else "body") if leg[0] == "f" else "hips"
        y_knee = top - up["len"]
        y_ankle = ft["h"]
        own = leg in ("fr", "hr")
        twin = "fr" if leg[0] == "f" else "hr"
        m.bone(f"leg_{leg}_upper", parent, [x, top, z])
        o, s = centred(up["w"], y_knee - up.get("overlap", 0), up["len"] + up.get("overlap", 0) + up.get("above", 0), z - up["d"] / 2 + up.get("dz", 0), up["d"], x=x)
        m.cube(f"leg_{leg}_upper", o, s, paint_for(p, f"leg_{leg}_upper"), share=None if own else f"leg_{twin}_upper#0", mirror=not own)
        if f"leg_{leg}_lower" in RIG_BONES[rig]:
            m.bone(f"leg_{leg}_lower", f"leg_{leg}_upper", [x, y_knee, z + lo.get("dz", 0)])
            o, s = centred(lo["w"], y_ankle, y_knee - y_ankle, z - lo["d"] / 2 + lo.get("dz", 0), lo["d"], x=x)
            m.cube(f"leg_{leg}_lower", o, s, paint_for(p, f"leg_{leg}_lower"), share=None if own else f"leg_{twin}_lower#0", mirror=not own)
            foot_parent = f"leg_{leg}_lower"
        else:
            foot_parent = f"leg_{leg}_upper"
        if f"leg_{leg}_foot" in RIG_BONES[rig]:
            m.bone(f"leg_{leg}_foot", foot_parent, [x, y_ankle, z + lo.get("dz", 0)])
            o, s = centred(ft["w"], 0, ft["h"], z + lo.get("dz", 0) - ft["fwd"], ft["len"], x=x)
            m.cube(f"leg_{leg}_foot", o, s, paint_for(p, f"leg_{leg}_foot"), share=None if own else f"leg_{twin}_foot#0", mirror=not own)
            for extra in ft.get("extra", []):
                o, s = centred(extra["w"], extra.get("y", 0), extra["h"], z + lo.get("dz", 0) - ft["fwd"] + extra.get("dz", 0), extra["len"], x=x)
                k = len(m.by[f"leg_{leg}_foot"]["_cubes"])
                m.cube(f"leg_{leg}_foot", o, s, extra["paint"], share=None if own else f"leg_{twin}_foot#{k}", mirror=not own)
    # tail chain
    tl = p.get("tail")
    if tl:
        names = ["tail"] if len(tl["segments"]) == 1 else [f"tail_{i + 1}" for i in range(len(tl["segments"]))]
        z, y = tl["z0"], tl["y0"]
        parent = "hips"
        for i, (nm, seg) in enumerate(zip(names, tl["segments"])):
            ang = tl["angle"] if i == 0 else seg.get("bend", 0)
            # a backward-pointing bone rises with +X, so a drooping tail takes a negative rotation
            m.bone(nm, parent, [0, y, z], [-ang, 0, 0])
            o, s = centred(seg["w"], y - seg["h"] / 2 + seg.get("dy", 0), seg["h"], z - seg.get("overlap", 0), seg["len"] + seg.get("overlap", 0))
            m.cube(nm, o, s, seg.get("paint") or paint_for(p, nm))
            for extra in seg.get("extra", []):
                o, s = centred(extra["w"], y - extra["h"] / 2 + extra.get("dy", 0), extra["h"], z + extra.get("dz", 0), extra["len"])
                m.cube(nm, o, s, extra["paint"])
            z += seg["len"]
            parent = nm
    for e in p.get("extra_cubes", []):
        o, s = box(*e["box"]) if "box" in e else (e["origin"], e["size"])
        m.cube(e["bone"], o, s, e.get("paint") or paint_for(p, e["bone"]))
    return m


def build_antlers(m, p, an, sk_front, sk_top, sk_w):
    """Antlers per side: a beam out of the skull, then either a palm plate with tines along its
    rim (moose) or a forked beam (deer). Palm and tines are cubes on the palm bone, so the
    entity can scale the whole rack by age through one bone."""
    pt = paint_for(p, "antler_*")
    for side, sx in (("l", -1), ("r", 1)):
        bx = sx * (sk_w / 2 - 0.5)
        by = sk_top - 0.5
        bz = sk_front + an["z"]
        own = side == "r"
        m.bone(f"antler_{side}", "head", [bx, by, bz], [an.get("tilt_back", 0), 0, -sx * an.get("rise", 0)])
        bl = an["beam"]
        x0 = bx if sx > 0 else bx - bl
        o, s = box(x0, x0 + bl, by, by + an.get("beam_h", 1), bz - an.get("beam_d", 1) / 2, bz + an.get("beam_d", 1) / 2)
        m.cube(f"antler_{side}", o, s, pt, share=None if own else "antler_r#0", mirror=not own)
        ex = bx + sx * bl
        if an.get("palm"):
            pa = an["palm"]
            m.bone(f"antler_{side}_palm", f"antler_{side}", [ex, by, bz], [pa.get("cup", 0), 0, -sx * pa["tilt"]])
            px0 = ex - (0 if sx > 0 else pa["len"])
            o, s = box(px0, px0 + pa["len"], by, by + 1, bz - pa["d"] + pa.get("back", 2), bz + pa.get("back", 2))
            m.cube(f"antler_{side}_palm", o, s, pt, share=None if own else "antler_r_palm#0", mirror=not own)
            for i, tn in enumerate(pa.get("tines", [])):
                # tine: [x offset along palm from beam end, z offset from palm front, height, along x?]
                tx = ex + sx * tn[0] - (1 if sx < 0 else 0)
                tz = bz - pa["d"] + pa.get("back", 2) + tn[1]
                o, s = box(tx, tx + 1, by + 1, by + 1 + tn[2], tz, tz + 1)
                m.cube(f"antler_{side}_palm", o, s, pt, share=None if own else f"antler_r_palm#{i + 1}", mirror=not own)
        if an.get("fork"):
            # cervid: the beam carries tine bones brow, g2-g5 (deer sheet), so the entity can hide tines by age class
            counts = {}
            for seg in an["fork"]:
                bn = f"antler_{side}" if seg["bone"] == "beam" else f"antler_{side}_{seg['bone']}"
                twin = "antler_r" if seg["bone"] == "beam" else f"antler_r_{seg['bone']}"
                x0s = ex + sx * seg["x"] - (seg.get("w", 1) if sx < 0 else 0)
                if bn not in m.by:
                    m.bone(bn, f"antler_{side}", [x0s + seg.get("w", 1) / 2, by + seg["y"], bz + seg["z"] + seg.get("d", 1) / 2])
                o, s = box(x0s, x0s + seg.get("w", 1), by + seg["y"], by + seg["y"] + seg["h"], bz + seg["z"], bz + seg["z"] + seg.get("d", 1))
                k = len(m.by[bn]["_cubes"])
                m.cube(bn, o, s, seg.get("paint") or pt, share=None if own else f"{twin}#{k}", mirror=not own)


def bird(p):
    """Grouse-family template: plump body and breast, short neck, head and beak, fan tail,
    folded wings as side plates (the wing bone spreads them for flight), short legs."""
    m = Model(p["geo_id"], p["tier"], p["rig"])
    b = p["body"]
    lg = p["legs"]
    m.bone("root", None, [0, 0, 0])
    m.bone("body", "root", [0, lg["h"] + b["h"] / 2, 0], [b.get("pitch", 0), 0, 0])
    y0 = lg["h"]
    o, s = centred(b["w"], y0, b["h"], -b["len"] / 2, b["len"])
    m.cube("body", o, s, paint_for(p, "body"))
    for e in b.get("extra", []):
        o, s = centred(e["w"], y0 + e.get("dy", 0), e["h"], -b["len"] / 2 + e.get("dz", 0), e["len"])
        m.cube("body", o, s, e.get("paint") or paint_for(p, "breast"))
    nk = p["neck"]
    nz = -b["len"] / 2 + nk["z"]
    ny = y0 + b["h"] - nk.get("sink", 1)
    m.bone("neck", "body", [0, ny, nz])
    o, s = centred(nk["w"], ny, nk["h"], nz - nk["d"] / 2, nk["d"])
    m.cube("neck", o, s, paint_for(p, "neck"))
    hd = p["head"]
    hy = ny + nk["h"] - 1
    m.bone("head", "neck", [0, hy, nz])
    o, s = centred(hd["w"], hy, hd["h"], nz - hd["len"] + hd.get("back", 1), hd["len"])
    hpaint = paint_for(p, "head")
    for px in hd.get("pixels", []):
        hpaint.setdefault("pixels", []).append(px)
    m.cube("head", o, s, hpaint)
    bk = p["beak"]
    bz = nz - hd["len"] + hd.get("back", 1)
    m.bone("beak", "head", [0, hy + bk.get("y", 1), bz])
    o, s = centred(bk["w"], hy + bk.get("y", 1), bk["h"], bz - bk["len"], bk["len"])
    m.cube("beak", o, s, paint_for(p, "beak"))
    tl = p["tail"]
    tz = b["len"] / 2 - tl.get("inset", 1)
    ty = y0 + b["h"] - tl.get("drop", 1)
    m.bone("tail", "body", [0, ty, tz], [-tl.get("angle", 0), 0, 0])
    o, s = centred(tl["w"], ty - tl["h"] / 2, tl["h"], tz, tl["len"])
    m.cube("tail", o, s, paint_for(p, "tail"))
    wg = p["wings"]
    for side, sx in (("l", -1), ("r", 1)):
        wx = sx * (b["w"] / 2 + 0.5 - wg.get("inset", 0))
        wy = y0 + b["h"] - wg.get("top", 1)
        wz = -b["len"] / 2 + wg["z"]
        own = side == "r"
        m.bone(f"wing_{side}", "body", [wx, wy, wz])
        o, s = box(wx - 0.5, wx + 0.5, wy - wg["h"], wy, wz, wz + wg["len"])
        m.cube(f"wing_{side}", o, s, paint_for(p, "wing_*"), share=None if own else "wing_r#0", mirror=not own)
        m.bone(f"wing_{side}_tip", f"wing_{side}", [wx, wy, wz + wg["len"]])
        o, s = box(wx - 0.5, wx + 0.5, wy - wg["tip_h"], wy, wz + wg["len"], wz + wg["len"] + wg["tip_len"])
        m.cube(f"wing_{side}_tip", o, s, paint_for(p, "wing_*_tip"), share=None if own else "wing_r_tip#0", mirror=not own)
        lx = sx * lg["x"]
        m.bone(f"leg_{side}", "body", [lx, y0 + 0.5, lg["z"]])
        o, s = centred(lg["w"], 0, y0 + 0.5, lg["z"] - lg["d"] / 2, lg["d"], x=lx)
        m.cube(f"leg_{side}", o, s, paint_for(p, "leg_*"), share=None if own else "leg_r#0", mirror=not own)
        if lg.get("foot"):
            ft = lg["foot"]
            o, s = centred(ft["w"], 0, ft["h"], lg["z"] - ft["len"] + ft.get("back", 1), ft["len"], x=lx)
            m.cube(f"leg_{side}", o, s, paint_for(p, "foot") if "foot" in p["paint"] else paint_for(p, "leg_*"),
                   share=None if own else "leg_r#1", mirror=not own)
    m.bone("mark", "head", [0, hy + hd["h"], nz])
    for e in p.get("extra_cubes", []):
        o, s = box(*e["box"]) if "box" in e else (e["origin"], e["size"])
        m.cube(e["bone"], o, s, e.get("paint") or paint_for(p, e["bone"]))
    return m


TEMPLATES = {"quadruped": quadruped, "bird": bird}


def fix_coplanar(m):
    """Overlapping coplanar faces flicker in game (z-fighting). Shrink the smaller cube of each
    such pair by a hair through Bedrock's `inflate`, which leaves the UV layout untouched.
    Only cubes whose bones carry no rest rotation (directly or by a parent) are compared."""
    rotated = set()
    for b in m.bones:
        if b.get("rotation") or (b.get("parent") in rotated):
            rotated.add(b["name"])
    cs = [(b, c) for b, c in m.cubes() if b["name"] not in rotated]
    def ext(c):
        o, s = c["origin"], c["size"]
        return [(o[i], o[i] + s[i]) for i in range(3)]
    n = 0
    for i in range(len(cs)):
        for j in range(i + 1, len(cs)):
            a, b = ext(cs[i][1]), ext(cs[j][1])
            hit = False
            for ax in range(3):
                others = [k for k in range(3) if k != ax]
                if all(min(a[k][1], b[k][1]) - max(a[k][0], b[k][0]) > 0.01 for k in others):
                    for ea in a[ax]:
                        for eb in b[ax]:
                            if abs(ea - eb) < 1e-6:
                                # same plane, both faces point the same way only if both are min or both max
                                if (ea == a[ax][0]) == (eb == b[ax][0]):
                                    hit = True
            if hit:
                va = np.prod(cs[i][1]["size"])
                vb = np.prod(cs[j][1]["size"])
                small = cs[i][1] if va <= vb else cs[j][1]
                small["inflate"] = round(small.get("inflate", 0) - 0.03, 3)
                n += 1
    return n


# ----------------------------------------------------------------------------- UV packing
def pack_uv(m):
    tw, th = TIERS[m.tier][2]
    keyed = {c["key"]: c for _, c in m.cubes()}
    owners = [c for _, c in m.cubes() if not c["share"]]
    owners.sort(key=lambda c: (-G.island(c["size"])[1], -G.island(c["size"])[0]))
    x = y = row = 0
    for c in owners:
        uw, uh = G.island(c["size"])
        if x + uw > tw:
            x, y, row = 0, y + row, 0
        c["uv"] = [x, y]
        x += uw
        row = max(row, uh)
    for _, c in m.cubes():
        if c["share"]:
            src = keyed[c["share"]]
            if src["size"] != c["size"]:
                raise ValueError(f"{m.gid}: shared UV size mismatch {c['key']} vs {c['share']}")
            c["uv"] = list(src["uv"])
    return y + row <= th, y + row


# ----------------------------------------------------------------------------- painting
SIDE_GROUPS = {"sides": ("east", "west", "north", "south"), "lr": ("east", "west"), "all": G.FACES,
               "top": ("up",), "bottom": ("down",), "front": ("north",), "back": ("south",),
               "flanks": ("east", "west")}


def face_list(spec):
    if isinstance(spec, list):
        out = []
        for s in spec:
            out += face_list(s)
        return out
    return list(SIDE_GROUPS.get(spec, (spec,)))


def in_range(v, r):
    return r is None or v is None or (r[0] <= v <= r[1])


def paint_texture(m, p, coat_remap=None, coat_name="", mask_remap=None, only=None, extra=None):
    tw, th = TIERS[m.tier][2]
    ramp = np.full((th, tw), "", object)
    step = np.zeros((th, tw), int)
    for b, c in m.cubes():
        if c["share"]:
            continue
        if only and not any(fnmatch.fnmatch(b["name"], o) for o in only):
            continue
        pt = c["paint"]
        if only and "overlay_paint" in p:
            pt = p["overlay_paint"]
        for pat, regs in (extra or {}).items():
            if fnmatch.fnmatch(b["name"], pat):
                pt = dict(pt, regions=list(pt.get("regions", [])) + regs)
        base_r, base_s = pt["ramp"], pt["step"]
        w, h, d = c["size"]
        for face in G.FACES:
            u0, v0, fw, fh = G.face_rect(face, c["size"], c["uv"])
            rng = np.random.default_rng(seed(m.gid, c["key"], face))
            # two-octave value noise along the body's grain, quantised to -1, 0, +1
            n1 = rng.random((fh, fw))
            n2 = rng.random((max(1, fh // 2 + 1), max(1, fw // 2 + 1)))
            n2 = np.kron(n2, np.ones((2, 2)))[:fh, :fw]
            nz = 0.55 * n1 + 0.45 * n2
            amt = pt.get("noise", 0.22)
            for iv in range(fh):
                for iu in range(fw):
                    xf, yf, zf = G.texel_semantic(face, iu, iv, fw, fh)
                    r, s = base_r, base_s
                    s += {"up": 1, "down": -1}.get(face, 0) * pt.get("light", 1)
                    for rg in pt.get("regions", []):
                        if face not in face_list(rg.get("faces", "all")):
                            continue
                        if not (in_range(xf, rg.get("x")) and in_range(yf, rg.get("y")) and in_range(zf, rg.get("z"))):
                            continue
                        if "speckle" in rg:
                            if seed(m.gid, c["key"], face, iu, iv, "sp") % 1000 >= rg["speckle"] * 1000:
                                continue
                        if "ramp" in rg:
                            r = rg["ramp"]
                        if "step" in rg:
                            s = rg["step"] + ({"up": 1, "down": -1}.get(face, 0) if rg.get("lit") else 0)
                        s += rg.get("dstep", 0)
                    if amt and nz[iv, iu] > 1 - amt / 2:
                        s += 1
                    elif amt and nz[iv, iu] < amt / 2:
                        s -= 1
                    ramp[v0 + iv, u0 + iu] = r
                    step[v0 + iv, u0 + iu] = s
            for px in pt.get("pixels", []):
                if face not in face_list(px.get("faces", "all")):
                    continue
                iu, iv = G.semantic_texel(face, px.get("x", 0.5), px.get("y", 0.5), px.get("z", 0.5), fw, fh)
                ramp[v0 + iv, u0 + iu] = px["ramp"]
                step[v0 + iv, u0 + iu] = px["step"]
    step = np.clip(step, 0, 4)
    if coat_remap:
        for a, bb in coat_remap.items():
            ramp[ramp == a] = bb
    if mask_remap:  # mottled coats: a seeded 50% mask of two ramps
        rng = np.random.default_rng(seed(m.gid, coat_name, "mottle"))
        mk = rng.random(ramp.shape) < mask_remap.get("share", 0.5)
        mk = np.kron(mk[::2, ::2], np.ones((2, 2), bool))[:ramp.shape[0], :ramp.shape[1]]
        for a, bb in mask_remap["remap"].items():
            ramp[(ramp == a) & mk] = bb
    reduce_colours(ramp, step, 12)
    img = np.zeros((th, tw, 4), np.uint8)
    for yy in range(th):
        for xx in range(tw):
            if ramp[yy, xx]:
                img[yy, xx] = P.hexrgb(P.RAMPS[ramp[yy, xx]][step[yy, xx]]) + (255,)
    return Image.fromarray(img, "RGBA")


def reduce_colours(ramp, step, limit):
    """Rule 4: at most `limit` colours. Merge the rarest colour into its nearest used neighbour step."""
    while True:
        used = {}
        for r, s in zip(ramp.ravel(), step.ravel()):
            if r:
                used[(r, int(s))] = used.get((r, int(s)), 0) + 1
        if len(used) <= limit:
            return
        (r, s), _ = min(used.items(), key=lambda kv: kv[1])
        cands = [(abs(s2 - s), -used[(r2, s2)], s2) for (r2, s2) in used if r2 == r and s2 != s]
        if not cands:
            other = max(used.items(), key=lambda kv: kv[1])[0]
            nr, ns = other
        else:
            nr, ns = r, min(cands)[2]
        sel = (ramp == r) & (step == s)
        ramp[sel] = nr
        step[sel] = ns


# ----------------------------------------------------------------------------- animation
GAITS = {"walk": {"fl": 0.25, "fr": 0.75, "hl": 0.0, "hr": 0.5}, "trot": {"fl": 0.0, "fr": 0.5, "hl": 0.5, "hr": 0.0},
         "gallop": {"fl": 0.6, "fr": 0.7, "hl": 0.0, "hr": 0.1}, "bound": {"fl": 0.5, "fr": 0.5, "hl": 0.0, "hr": 0.0}}


def kf(t):
    return f"{t:.4f}".rstrip("0").rstrip(".") if t else "0.0"


def cyc(period, fn, n=8):
    """Keyframes for one closed cycle; fn(k in 0..1) -> [x,y,z]. Last key equals the first."""
    keys = {}
    for i in range(n + 1):
        k = i / n
        v = fn(0.0 if i == n else k)
        keys[kf(k * period)] = {"post": [round(a, 2) for a in v], "lerp_mode": "catmullrom"}
    return keys


def animations(p, m):
    names = {b["name"] for b in m.bones}
    g = p["gait"]
    out = {}
    rig = p["rig"]
    sin, cos, pi = math.sin, math.cos, math.pi

    def legs(kind, period, amp, knee, bob, neck, pitch=0.0):
        a = {"loop": True, "animation_length": period, "bones": {}}
        for leg, ph in GAITS[kind].items():
            if f"leg_{leg}_upper" not in names:
                continue
            A = amp * (g.get("hind_amp", 1.0) if leg[0] == "h" else 1.0)
            a["bones"][f"leg_{leg}_upper"] = {"rotation": cyc(period, lambda k, ph=ph, A=A: [A * sin(2 * pi * (k + ph)), 0, 0])}
            if f"leg_{leg}_lower" in names:
                sign = 1 if leg[0] == "f" else -0.7  # front knees fold the foot back, hocks swing it forward
                a["bones"][f"leg_{leg}_lower"] = {"rotation": cyc(period, lambda k, ph=ph: [sign * max(0.0, -knee * cos(2 * pi * (k + ph))), 0, 0])}
            if f"leg_{leg}_foot" in names and leg[0] == "h" and rig == "small_rodent":
                a["bones"][f"leg_{leg}_foot"] = {"rotation": cyc(period, lambda k, ph=ph: [-0.6 * A * sin(2 * pi * (k + ph)), 0, 0])}
        a["bones"]["body"] = {"position": cyc(period, lambda k: [0, bob * abs(sin(2 * pi * k)), 0])}
        if pitch:
            a["bones"]["body"]["rotation"] = cyc(period, lambda k: [pitch * sin(2 * pi * k), 0, 0])
        if "neck" in names:
            a["bones"]["neck"] = {"rotation": cyc(period, lambda k: [neck * sin(4 * pi * k + 1), 0, 0])}
        elif "head" in names:
            a["bones"]["head"] = {"rotation": cyc(period, lambda k: [neck * sin(4 * pi * k + 1), 0, 0])}
        return a

    def tailbones():
        return [n for n in ("tail", "tail_1", "tail_2", "tail_3") if n in names]

    if rig == "grouse":
        T = g["walk_s"]
        out["walk"] = {"loop": True, "animation_length": T, "bones": {
            "leg_l": {"rotation": cyc(T, lambda k: [30 * sin(2 * pi * k), 0, 0])},
            "leg_r": {"rotation": cyc(T, lambda k: [-30 * sin(2 * pi * k), 0, 0])},
            "neck": {"rotation": cyc(T, lambda k: [12 * sin(4 * pi * k), 0, 0])},
            "body": {"position": cyc(T, lambda k: [0, 0.3 * abs(sin(2 * pi * k)), 0])}}}
        T2 = g["walk_s"] * 0.55
        out["run"] = {"loop": True, "animation_length": T2, "bones": {
            "leg_l": {"rotation": cyc(T2, lambda k: [45 * sin(2 * pi * k), 0, 0])},
            "leg_r": {"rotation": cyc(T2, lambda k: [-45 * sin(2 * pi * k), 0, 0])},
            "body": {"rotation": cyc(T2, lambda k: [12, 0, 0])},
            "neck": {"rotation": cyc(T2, lambda k: [-15 + 8 * sin(4 * pi * k), 0, 0])},
            "wing_l": {"rotation": cyc(T2, lambda k: [0, 0, 10])}, "wing_r": {"rotation": cyc(T2, lambda k: [0, 0, -10])}}}
        F = round(1 / g["flap_hz"], 4)
        # folded wings hang along the flanks; z rotation lifts them out to the horizontal and beats them
        out["fly"] = {"loop": True, "animation_length": F, "bones": {
            "wing_l": {"rotation": cyc(F, lambda k: [0, 0, 90 + 55 * cos(2 * pi * k)], 4)},
            "wing_r": {"rotation": cyc(F, lambda k: [0, 0, -(90 + 55 * cos(2 * pi * k))], 4)},
            "wing_l_tip": {"rotation": cyc(F, lambda k: [0, 0, 20 * cos(2 * pi * k)], 4)},
            "wing_r_tip": {"rotation": cyc(F, lambda k: [0, 0, -20 * cos(2 * pi * k)], 4)},
            "leg_l": {"rotation": cyc(F, lambda k: [70, 0, 0], 2)}, "leg_r": {"rotation": cyc(F, lambda k: [70, 0, 0], 2)},
            "tail": {"rotation": cyc(F, lambda k: [10, 0, 0], 2)}}}
        out["glide"] = {"loop": True, "animation_length": 2.0, "bones": {
            "wing_l": {"rotation": cyc(2.0, lambda k: [0, 0, 80 + 3 * sin(2 * pi * k)], 4)},
            "wing_r": {"rotation": cyc(2.0, lambda k: [0, 0, -80 - 3 * sin(2 * pi * k)], 4)},
            "leg_l": {"rotation": cyc(2.0, lambda k: [70, 0, 0], 2)}, "leg_r": {"rotation": cyc(2.0, lambda k: [70, 0, 0], 2)}}}
        out["idle"] = {"loop": True, "animation_length": 6.0, "bones": {
            "head": {"rotation": {"0.0": [0, 0, 0], "1.5": [0, 30, 0], "2.0": [0, 30, 0], "3.5": [0, -25, 0], "4.5": [0, -25, 0], "6.0": [0, 0, 0]}},
            "body": {"scale": {"0.0": [1, 1, 1], "3.0": [1.02, 1.02, 1.0], "6.0": [1, 1, 1]}}}}
        out["forage"] = {"loop": True, "animation_length": 1.2, "bones": {
            "neck": {"rotation": {"0.0": [50, 0, 0], "0.3": [62, 0, 0], "0.6": [50, 0, 0], "0.9": [62, 0, 0], "1.2": [50, 0, 0]}},
            "body": {"rotation": {"0.0": [15, 0, 0], "1.2": [15, 0, 0]}}}}
        out["alert"] = {"loop": "hold_on_last_frame", "animation_length": 0.4, "bones": {
            "neck": {"rotation": {"0.0": [0, 0, 0], "0.4": [-25, 0, 0]}},
            "body": {"position": {"0.0": [0, 0, 0], "0.4": [0, 0.5, 0]}},
            "tail": {"rotation": {"0.0": [0, 0, 0], "0.4": [35, 0, 0]}}}}
    else:
        out["walk"] = legs("walk", g["walk_s"], g["walk_amp"], g["knee"], g.get("bob", 0.3), g.get("neck_swing", 5))
        out["run"] = legs(g.get("run", "gallop"), g["run_s"], g["run_amp"], g["knee"] * 1.4, g.get("bob", 0.3) * 2.5,
                          g.get("neck_swing", 5) * 1.5, pitch=g.get("run_pitch", 0))
        if rig in ("ungulate", "cervid") and g.get("run") == "trot":
            # moose trot: high-stepping, head level (Art spec A1 motion test)
            out["run"]["bones"]["neck"] = {"rotation": cyc(g["run_s"], lambda k: [2 * sin(4 * pi * k), 0, 0])}
        idle = {"loop": True, "animation_length": 6.0, "bones": {
            "body": {"scale": {"0.0": [1, 1, 1], "3.0": [1.02, 1.02, 1.0], "6.0": [1, 1, 1]}}}}
        if "ear_l" in names:
            idle["bones"]["ear_l"] = {"rotation": {"0.0": [0, 0, 0], "4.2": [0, 0, 0], "4.35": [-15, 0, -10], "4.6": [0, 0, 0], "6.0": [0, 0, 0]}}
        for tb in tailbones()[:1]:
            idle["bones"][tb] = {"rotation": cyc(6.0, lambda k: [0, 6 * sin(2 * pi * k), 0], 4)}
        out["idle"] = idle
        down = g.get("graze_deg", 55)
        chew = {"jaw": {"rotation": cyc(0.5, lambda k: [8 * max(0, sin(2 * pi * k)), 0, 0], 4)}} if "jaw" in names else {}
        graze_b = {"neck" if "neck" in names else "head": {"rotation": {"0.0": [down, 0, 0], "1.5": [down + 5, 4, 0], "3.0": [down, 0, 0]}}}
        if "jaw" in names:
            graze_b["jaw"] = {"rotation": {kf(i * 0.25): [8 if i % 2 else 0, 0, 0] for i in range(13)}}
        name = "graze" if rig in ("ungulate", "cervid") else "forage"
        out[name] = {"loop": True, "animation_length": 3.0, "bones": graze_b}
        al = {"neck" if "neck" in names else "head": {"rotation": {"0.0": [0, 0, 0], "0.4": [-g.get("alert_deg", 25), 0, 0]}}}
        for e in ("ear_l", "ear_r"):
            if e in names:
                al[e] = {"rotation": {"0.0": [0, 0, 0], "0.4": [20, 0, 0]}}
        if rig == "small_rodent":
            # sit up tall: body pitched up about the haunch
            al["body"] = {"rotation": {"0.0": [0, 0, 0], "0.4": [-35, 0, 0]}}
            for leg in ("leg_hl_upper", "leg_hr_upper"):
                al[leg] = {"rotation": {"0.0": [0, 0, 0], "0.4": [35, 0, 0]}}
        out["alert"] = {"loop": "hold_on_last_frame", "animation_length": 0.4, "bones": al}
        if rig in ("ungulate", "cervid", "small_rodent"):
            fl = copy.deepcopy(out["run"])
            for tb in tailbones()[:1]:
                fl["bones"][tb] = {"rotation": cyc(fl["animation_length"], lambda k: [g.get("flag_deg", 60), 0, 0], 2)}
            for e in ("ear_l", "ear_r"):
                if e in names:
                    fl["bones"][e] = {"rotation": cyc(fl["animation_length"], lambda k: [-25, 0, 0], 2)}
            out["flee"] = fl
        if rig in ("ungulate", "cervid", "ursid", "large_rodent"):
            sw = legs("trot", g["walk_s"] * 1.3, g["walk_amp"] * 0.8, g["knee"], 0, 2)
            out["swim"] = sw
        if rig == "ursid":
            # stand on the hind feet: body pitches up about the hips, hips counter-rotate
            out["stand"] = {"loop": "hold_on_last_frame", "animation_length": 0.8, "bones": {
                "body": {"rotation": {"0.0": [0, 0, 0], "0.8": [-62, 0, 0]}},
                "hips": {"rotation": {"0.0": [0, 0, 0], "0.8": [62, 0, 0]}},
                "leg_fl_upper": {"rotation": {"0.0": [0, 0, 0], "0.8": [40, 0, -10]}},
                "leg_fr_upper": {"rotation": {"0.0": [0, 0, 0], "0.8": [40, 0, 10]}},
                "neck": {"rotation": {"0.0": [0, 0, 0], "0.8": [45, 0, 0]}}}}
        if rig == "large_rodent":
            out["tail_slap"] = {"loop": False, "animation_length": 0.5, "bones": {
                "tail": {"rotation": {"0.0": [0, 0, 0], "0.15": [70, 0, 0], "0.3": [-15, 0, 0], "0.5": [0, 0, 0]}}}}
    out["death"] = {"loop": "hold_on_last_frame", "animation_length": 1.5, "bones": {"root": {
        "rotation": {"0.0": [0, 0, 0], "1.5": [0, 0, 90]},
        "position": {"0.0": [0, 0, 0], "1.5": [0, -p.get("death_drop", 2), 0]}}}}
    return out


# ----------------------------------------------------------------------------- output
def geo_json(m):
    tw, th = TIERS[m.tier][2]
    bones = []
    for b in m.bones:
        ob = {k: v for k, v in b.items() if not k.startswith("_")}
        cs = []
        for c in b["_cubes"]:
            oc = {"origin": c["origin"], "size": c["size"], "uv": c["uv"]}
            if c["mirror"]:
                oc["mirror"] = True
            if c.get("inflate"):
                oc["inflate"] = c["inflate"]
            if "rotation" in c:
                oc["rotation"] = c["rotation"]
                oc["pivot"] = c["pivot"]
            cs.append(oc)
        if cs:
            ob["cubes"] = cs
        bones.append(ob)
    xs = [v for _, c in m.cubes() for v in (c["origin"][0], c["origin"][0] + c["size"][0])]
    ys = [v for _, c in m.cubes() for v in (c["origin"][1], c["origin"][1] + c["size"][1])]
    return {"format_version": "1.12.0", "minecraft:geometry": [{"description": {
        "identifier": f"geometry.yearofice.{m.gid}", "texture_width": tw, "texture_height": th,
        "visible_bounds_width": math.ceil(max(map(abs, xs)) * 2 / 16 + 2),
        "visible_bounds_height": math.ceil(max(ys) / 16 + 1), "visible_bounds_offset": [0, max(ys) / 32, 0]},
        "bones": bones}]}


GEOM_TREES = ("torso", "neck", "head", "legs", "tail", "body", "wings", "beak", "extra_cubes")
ANGLE_KEYS = {"angle", "pitch", "out", "lay", "rise", "tilt", "tilt_back", "cup", "bend", "eye", "paint"}


def scale_geom(v, f):
    """Scale every length in a geometry subtree; angles, eye positions and paint stay."""
    if isinstance(v, dict):
        return {k: (vv if k in ANGLE_KEYS else scale_geom(vv, f)) for k, vv in v.items()}
    if isinstance(v, list):
        return [scale_geom(x, f) for x in v]
    if isinstance(v, (int, float)) and not isinstance(v, bool):
        return round(v * f, 3)
    return v


def load_species(sid):
    p = json.load(open(os.path.join(SPECIES_DIR, sid + ".json")))
    if "inherit" in p:
        base = load_species(p["inherit"])
        if "scale" in p:
            for k in GEOM_TREES:
                if k in base:
                    base[k] = scale_geom(base[k], p["scale"])
        p = deep_merge(base, {k: v for k, v in p.items() if k not in ("inherit", "scale")})
    for k in [k for k, v in p.items() if v is None]:
        del p[k]
    return p


def deep_merge(a, b):
    out = copy.deepcopy(a)
    for k, v in b.items():
        if v is None:
            out.pop(k, None)
            continue
        if isinstance(v, dict) and isinstance(out.get(k), dict) and not v.get("_replace"):
            out[k] = deep_merge(out[k], v)
        else:
            out[k] = copy.deepcopy(v) if not (isinstance(v, dict) and v.get("_replace")) else {kk: vv for kk, vv in v.items() if kk != "_replace"}
    return out


def build(sid, write=True):
    p = load_species(sid)
    m = TEMPLATES[p["template"]](p)
    fix_coplanar(m)
    ok, used = pack_uv(m)
    if not ok:
        raise SystemExit(f"{sid}: UV overflow, needs {used} rows of {TIERS[m.tier][2][1]}")
    geo = geo_json(m)
    anim_id = p.get("anim_id", p["geo_id"])
    anims = {"format_version": "1.8.0", "animations": {f"animation.{anim_id}.{k}": v for k, v in animations(p, m).items()}}
    textures = {}
    for var, vs in p["textures"].items():
        textures[var] = paint_texture(m, p, vs.get("remap"), var, vs.get("mottle"), vs.get("only_bones"), vs.get("extra_regions"))
    if write:
        gp = os.path.join(ASSETS, "geo", "entity", f"{p['geo_id']}.geo.json")
        ap = os.path.join(ASSETS, "animations", "entity", f"{anim_id}.animation.json")
        os.makedirs(os.path.dirname(gp), exist_ok=True)
        os.makedirs(os.path.dirname(ap), exist_ok=True)
        json.dump(geo, open(gp, "w"), indent=1)
        json.dump(anims, open(ap, "w"), indent=1)
        for also in p.get("anim_copies", []):
            cp = {"format_version": "1.8.0", "animations": {k.replace(f"animation.{anim_id}.", f"animation.{also}."): v
                                                            for k, v in anims["animations"].items()}}
            json.dump(cp, open(os.path.join(ASSETS, "animations", "entity", f"{also}.animation.json"), "w"), indent=1)
        for var, img in textures.items():
            tp = os.path.join(ASSETS, "textures", "entity", p["texture_dir"], f"{var}.png")
            os.makedirs(os.path.dirname(tp), exist_ok=True)
            img.save(tp, optimize=False)
        side = {"species": sid, "grade": p.get("grade", "G1"), "generator": VERSION, "base": p.get("base"),
                "geo": f"geo/entity/{p['geo_id']}.geo.json", "textures": sorted(p["textures"])}
        json.dump(side, open(gp.replace(".geo.json", ".prov.json"), "w"), indent=1)
    return p, m, geo, anims, textures


# ----------------------------------------------------------------------------- validation
def validate(p, geo, anims, textures):
    errs = []
    gb = geo["minecraft:geometry"][0]
    bones = gb["bones"]
    names = [b["name"] for b in bones]
    cubes, bmax, (tw, th), _ = TIERS[p["tier"]]
    bmax = BONE_CAP.get(p["rig"], bmax)
    # 1 bones
    for r in RIG_BONES[p["rig"]]:
        if r not in names:
            errs.append(f"missing rig bone {r}")
    if len(set(names)) != len(names):
        errs.append("duplicate bone names")
    for b in bones:
        if b.get("parent") and b["parent"] not in names:
            errs.append(f"{b['name']}: parent {b['parent']} missing")
    # 2 budget
    n = sum(len(b.get("cubes", [])) for b in bones)
    if n > cubes:
        errs.append(f"cubes {n} > {cubes}")
    if len(bones) > bmax:
        errs.append(f"bones {len(bones)} > {bmax}")
    # 3 UVs inside the atlas, owners not overlapping
    occ = np.zeros((th, tw), bool)
    seen = set()
    for b in bones:
        for c in b.get("cubes", []):
            uw, uh = G.island(c["size"])
            u, v = c["uv"]
            if u < 0 or v < 0 or u + uw > tw or v + uh > th:
                errs.append(f"{b['name']}: UV outside atlas")
                continue
            k = (u, v)
            if k in seen:
                continue
            seen.add(k)
            if occ[v:v + uh, u:u + uw].any():
                errs.append(f"{b['name']}: UV overlap")
            occ[v:v + uh, u:u + uw] = True
    # 4 animation bones; 6 loops close
    for an, a in anims["animations"].items():
        for bn, ch in a["bones"].items():
            if bn not in names:
                errs.append(f"{an}: unknown bone {bn}")
            if a["loop"] is True:
                for chan, keys in ch.items():
                    ks = sorted(keys, key=float)
                    v0 = keys[ks[0]]
                    v1 = keys[ks[-1]]
                    v0 = v0["post"] if isinstance(v0, dict) else v0
                    v1 = v1["post"] if isinstance(v1, dict) else v1
                    if chan == "rotation" and max(abs(x - y) for x, y in zip(v0, v1)) > 0.5:
                        errs.append(f"{an}/{bn}: loop does not close")
    have = {k.split(".")[-1] for k in anims["animations"]}
    for need in SHARED_SET[p["rig"]]:
        if need not in have:
            errs.append(f"shared animation {need} missing")
    # 5 hitbox fit (height and width, antlers excluded)
    hb = p.get("hitbox")
    if hb:
        ys, xs = [], []
        for b in bones:
            if b["name"].startswith("antler"):
                continue
            for c in b.get("cubes", []):
                ys += [c["origin"][1] + c["size"][1]]
                xs += [c["origin"][0], c["origin"][0] + c["size"][0]]
        hgt = max(ys) / 16.0
        wid = (max(xs) - min(xs)) / 16.0
        # the model must fit its collision box: at most 10% over it in height and width
        if hgt > hb[1] * 1.10:
            errs.append(f"height {hgt:.2f} m over hitbox {hb[1]} m by more than 10%")
        if wid > hb[0] * 1.10:
            errs.append(f"width {wid:.2f} m over hitbox {hb[0]} m by more than 10%")
    # 7 textures pass the palette check
    for var, img in textures.items():
        ok, ncol, ramps, probs = P.check_image(img, max_ramps=3, max_colours=12)
        if not ok:
            errs.append(f"texture {var}: {'; '.join(probs)}")
        if img.size != (tw, th):
            errs.append(f"texture {var}: size {img.size} != {(tw, th)}")
    return n, len(bones), errs


def all_species():
    return sorted(f[:-5] for f in os.listdir(SPECIES_DIR) if f.endswith(".json") and not f.startswith("_"))


def main(argv):
    cmd = argv[0] if argv else "build"
    ids = argv[1:] or all_species()
    bad = 0
    for sid in ids:
        p, m, geo, anims, tex = build(sid, write=(cmd == "build"))
        n, nb, errs = validate(p, geo, anims, tex)
        print(f"{'PASS' if not errs else 'FAIL'} {sid}: tier={p['tier']} cubes={n}/{TIERS[p['tier']][0]} bones={nb}/{BONE_CAP.get(p['rig'], TIERS[p['tier']][1])} "
              f"anims={len(anims['animations'])} textures={len(tex)}" + ("" if not errs else "\n   " + "\n   ".join(errs)))
        bad += bool(errs)
    if bad:
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv[1:])
