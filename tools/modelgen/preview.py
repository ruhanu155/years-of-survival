"""preview: render a GeckoLib .geo.json with its texture to PNG (side, front, 3/4, black silhouette),
at the size the model fills on a 1080p screen at 70 degrees FOV from 8 m (the Small-tier zoo
check distance), and at close range. Same box-UV and rotation conventions as modelgen (geo.py).

Usage:
  python3 -I tools/modelgen/preview.py one <geo.json> <texture.png> <out.png> [--anim file name t]
  python3 -I tools/modelgen/preview.py sheet <sheet.json> <out.png>
A sheet.json is {"title": str, "rows": [{"label", "geo", "texture", "ground": "summer"|"snow",
"anim": [file, name, t]?}]} with paths relative to the repository root."""
import json, math, os, sys
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import geo as G  # noqa: E402

# 1080 px tall view, vertical FOV 70 deg: px per metre at distance D is 1080 / (2 D tan 35deg)
def px_per_m(dist_m):
    return 1080.0 / (2 * dist_m * math.tan(math.radians(35)))


VIEWS = {"side": (0, 6), "front": (90, 6), "3/4": (45, 14), "back3/4": (135, 14)}
GROUND = {"summer": (78, 84, 58), "snow": (200, 209, 219), "white": (236, 236, 232), "sky": (150, 165, 175)}
LIGHT = np.array([0.35, 1.0, -0.55]) / np.linalg.norm([0.35, 1.0, -0.55])


def camera(az, el):
    a, e = math.radians(az), math.radians(el)
    c = np.array([math.cos(e) * math.cos(a), math.sin(e), -math.cos(e) * math.sin(a)])
    f = -c
    r = np.cross(f, [0, 1, 0])
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    return r, u, c


def face_frames(o, s):
    x0, y0, z0 = o
    w, h, d = s
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    # (C0, E1, E2, normal, semantic mapping(a, b) -> (xf, yf, zf))
    return {
        "up": ((x1, y1, z0), (-w, 0, 0), (0, 0, d), (0, 1, 0), lambda a, b: (a, None, b)),
        "down": ((x1, y0, z0), (-w, 0, 0), (0, 0, d), (0, -1, 0), lambda a, b: (a, None, b)),
        "east": ((x1, y1, z0), (0, 0, d), (0, -h, 0), (1, 0, 0), lambda a, b: (None, b, a)),
        "west": ((x0, y1, z0), (0, 0, d), (0, -h, 0), (-1, 0, 0), lambda a, b: (None, b, a)),
        "north": ((x1, y1, z0), (-w, 0, 0), (0, -h, 0), (0, 0, -1), lambda a, b: (a, b, None)),
        "south": ((x1, y1, z1), (-w, 0, 0), (0, -h, 0), (0, 0, 1), lambda a, b: (a, b, None)),
    }


def sample_anim(path, name, t):
    a = json.load(open(path))["animations"][name]
    pose = {}
    for bn, chans in a["bones"].items():
        for ch, keys in chans.items():
            ks = sorted(keys, key=float)
            vals = [keys[k]["post"] if isinstance(keys[k], dict) else keys[k] for k in ks]
            ts = [float(k) for k in ks]
            if t <= ts[0]:
                v = vals[0]
            elif t >= ts[-1]:
                v = vals[-1]
            else:
                i = max(j for j in range(len(ts)) if ts[j] <= t)
                f = (t - ts[i]) / (ts[i + 1] - ts[i])
                v = [vals[i][k] * (1 - f) + vals[i + 1][k] * f for k in range(3)]
            pose.setdefault(bn, {})[ch] = v
    return pose


def render(geo, tex, view="3/4", scale=6.0, pose=None, silhouette=False, bg=None, margin=6, hide=()):
    gb = geo["minecraft:geometry"][0]
    tw, th = gb["description"]["texture_width"], gb["description"]["texture_height"]
    T = np.array(tex.convert("RGBA"))
    if T.shape[1] != tw or T.shape[0] != th:
        raise ValueError("texture size mismatch")
    bones = gb["bones"]
    mats = G.bone_matrices(bones, pose)
    az, el = VIEWS[view] if isinstance(view, str) else view
    r, u, c = camera(az, el)
    faces = []
    for b in bones:
        if any(b["name"].startswith(hh) for hh in hide):
            continue
        Mw = mats[b["name"]]
        for cb in b.get("cubes", []):
            Mc = Mw
            if "rotation" in cb:
                Mc = Mw @ G.affine(cb["rotation"], cb["pivot"])
            inf = cb.get("inflate", 0.0)
            o_ = [v - inf for v in cb["origin"]]
            s_ = [v + 2 * inf for v in cb["size"]]
            for fname, (C0, E1, E2, N, sem) in face_frames(o_, s_).items():
                p0 = Mc[:3, :3] @ np.array(C0) + Mc[:3, 3]
                e1 = Mc[:3, :3] @ np.array(E1, float)
                e2 = Mc[:3, :3] @ np.array(E2, float)
                n = Mc[:3, :3] @ np.array(N, float)
                if n @ c <= 1e-6:
                    continue
                faces.append((fname, p0, e1, e2, n, sem, cb))
    if not faces:
        return Image.new("RGBA", (8, 8))
    pts = []
    for f in faces:
        for a in (0, 1):
            for bb in (0, 1):
                q = f[1] + a * f[2] + bb * f[3]
                pts.append((q @ r, q @ u))
    pts = np.array(pts)
    mnx, mny = pts.min(0)
    mxx, mxy = pts.max(0)
    W = int(math.ceil((mxx - mnx) * scale)) + 2 * margin
    H = int(math.ceil((mxy - mny) * scale)) + 2 * margin
    col = np.zeros((H, W, 4), np.uint8)
    zb = np.full((H, W), -1e9)
    for fname, p0, e1, e2, n, sem, cb in faces:
        s0 = np.array([(p0 @ r - mnx) * scale + margin, (mxy - p0 @ u) * scale + margin])
        s1 = np.array([e1 @ r * scale, -(e1 @ u) * scale])
        s2 = np.array([e2 @ r * scale, -(e2 @ u) * scale])
        A = np.array([s1, s2]).T
        det = np.linalg.det(A)
        if abs(det) < 1e-6:
            continue
        Ai = np.linalg.inv(A)
        corners = np.array([s0, s0 + s1, s0 + s2, s0 + s1 + s2])
        x0, y0 = np.floor(corners.min(0)).astype(int)
        x1, y1 = np.ceil(corners.max(0)).astype(int)
        x0, y0 = max(0, x0), max(0, y0)
        x1, y1 = min(W, x1), min(H, y1)
        if x1 <= x0 or y1 <= y0:
            continue
        yy, xx = np.mgrid[y0:y1, x0:x1]
        q = np.stack([xx + 0.5 - s0[0], yy + 0.5 - s0[1]], -1) @ Ai.T
        a, bb = q[..., 0], q[..., 1]
        inside = (a >= -1e-6) & (a <= 1 + 1e-6) & (bb >= -1e-6) & (bb <= 1 + 1e-6)
        if not inside.any():
            continue
        depth = p0 @ c + a * (e1 @ c) + bb * (e2 @ c)
        upd = inside & (depth > zb[y0:y1, x0:x1])
        if not upd.any():
            continue
        w_, h_, d_ = cb["size"]
        region = fname
        if cb.get("mirror") and fname in ("east", "west"):
            region = "west" if fname == "east" else "east"
        u0, v0, fw, fh = G.face_rect(region, cb["size"], cb["uv"])
        ac = np.clip(a, 0, 0.9999)
        bc = np.clip(bb, 0, 0.9999)
        xf, yf, zf = sem(ac, bc)
        if region in ("east",):
            iu = np.floor((1 - zf) * fw)
            iv = np.floor(yf * fh)
        elif region == "west":
            iu = np.floor(zf * fw)
            iv = np.floor(yf * fh)
        elif region == "north":
            iu = np.floor(xf * fw)
            iv = np.floor(yf * fh)
        elif region == "south":
            iu = np.floor((1 - xf) * fw)
            iv = np.floor(yf * fh)
        elif region == "up":
            iu = np.floor(xf * fw)
            iv = np.floor((1 - zf) * fh)
        else:
            iu = np.floor(xf * fw)
            iv = np.floor(zf * fh)
        if cb.get("mirror"):
            iu = fw - 1 - iu
        iu = np.clip(iu, 0, fw - 1).astype(int) + u0
        iv = np.clip(iv, 0, fh - 1).astype(int) + v0
        texel = T[iv, iu]
        opaque = texel[..., 3] > 0
        upd &= opaque
        nn = n / np.linalg.norm(n)
        shade = 0.62 + 0.38 * max(0.0, float(nn @ LIGHT)) + (0.08 if nn[1] > 0.5 else 0)
        rgb = np.clip(texel[..., :3] * min(1.0, shade), 0, 255)
        sub_c = col[y0:y1, x0:x1]
        sub_z = zb[y0:y1, x0:x1]
        sub_c[upd, :3] = rgb[upd]
        sub_c[upd, 3] = 255
        sub_z[upd] = depth[upd]
    img = Image.fromarray(col, "RGBA")
    if silhouette:
        al = np.array(img)[..., 3] > 0
        out = np.zeros((H, W, 4), np.uint8)
        out[al] = (20, 20, 20, 255)
        img = Image.fromarray(out, "RGBA")
    if bg:
        base = Image.new("RGBA", img.size, GROUND.get(bg, bg) + (255,) if isinstance(bg, str) else bg)
        base.alpha_composite(img)
        img = base
    return img


def model_extent_m(geo):
    xs, ys, zs = [], [], []
    for b in geo["minecraft:geometry"][0]["bones"]:
        for c in b.get("cubes", []):
            for i, arr in enumerate((xs, ys, zs)):
                arr += [c["origin"][i], c["origin"][i] + c["size"][i]]
    return max(max(ys) - min(ys), max(zs) - min(zs), max(xs) - min(xs)) / 16.0


def sheet(spec, out):
    rows = spec["rows"]
    S8 = px_per_m(8.0) / 16.0  # px per model pixel at 8 m
    tiles = []
    for row in rows:
        geo = json.load(open(os.path.join(ROOT, row["geo"])))
        tex = Image.open(os.path.join(ROOT, row["texture"]))
        ground = row.get("ground", "summer")
        hide = tuple(row.get("hide", ()))
        ext = model_extent_m(geo)
        close = max(S8 * 1.5, min(S8 * 6, 260.0 / (ext * 16)))
        pose = None
        if row.get("anim"):
            f, n, t = row["anim"]
            pose = sample_anim(os.path.join(ROOT, f), n, t)
        imgs = [
            ("side 8 m", render(geo, tex, "side", S8, bg=ground, hide=hide)),
            ("front 8 m", render(geo, tex, "front", S8, bg=ground, hide=hide)),
            ("3/4 8 m", render(geo, tex, "3/4", S8, bg=ground, hide=hide)),
            ("silhouette 8 m", render(geo, tex, "side", S8, silhouette=True, bg="white", hide=hide)),
            ("3/4 close", render(geo, tex, "3/4", close, bg="sky", hide=hide)),
            ("rear 3/4 close" if not pose else f"{row['anim'][1].split('.')[-1]} pose",
             render(geo, tex, "back3/4" if not pose else "side", close, bg="sky", pose=pose, hide=hide)),
        ]
        tiles.append((row["label"], imgs))
    pad = 8
    colw = [max(t[1][i][1].width for t in tiles) for i in range(6)]
    rowh = [max(im.height for _, im in t[1]) + 16 for t in tiles]
    W = sum(colw) + pad * 7
    H = sum(rowh) + pad * (len(tiles) + 1) + 30 + 16 * len(tiles)
    sh = Image.new("RGB", (W, H), (34, 36, 38))
    d = ImageDraw.Draw(sh)
    d.text((pad, 8), spec.get("title", ""), fill=(235, 235, 235))
    y = 30
    for (label, imgs), rh in zip(tiles, rowh):
        d.text((pad, y), label, fill=(240, 220, 160))
        y += 16
        x = pad
        for i, (cap, im) in enumerate(imgs):
            sh.paste(im.convert("RGB"), (x, y + 14), im)
            d.text((x, y), cap, fill=(170, 170, 170))
            x += colw[i] + pad
        y += rh + pad
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sh.save(out)
    return sh.size


def icon_sheet(out, title="Item sprites (16 x 16) at GUI scale 4, with GUI scale 1 inset"):
    """Every item icon at scale 4 on a slot background, its 1x size beside it, grouped by name."""
    base = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice", "textures", "item")
    files = sorted(f for f in os.listdir(base) if f.endswith(".png"))
    load = os.path.join(base, "load")
    if os.path.isdir(load):
        files += ["load/" + f for f in sorted(os.listdir(load)) if f.endswith(".png")]
    cols, cw, ch = 8, 132, 104
    rows = (len(files) + cols - 1) // cols
    sh = Image.new("RGB", (cols * cw + 8, rows * ch + 34), (34, 36, 38))
    d = ImageDraw.Draw(sh)
    d.text((8, 8), title, fill=(235, 235, 235))
    for i, f in enumerate(files):
        im = Image.open(os.path.join(base, f)).convert("RGBA")
        x, y = 8 + (i % cols) * cw, 30 + (i // cols) * ch
        d.rectangle((x, y, x + 71, y + 71), fill=(139, 139, 139))
        d.rectangle((x + 2, y + 2, x + 69, y + 69), fill=(118, 118, 118))
        big = im.resize((64, 64), Image.NEAREST)
        sh.paste(big, (x + 4, y + 4), big)
        sh.paste(im, (x + 80, y + 4), im)
        d.text((x, y + 76), f[:-4][:21], fill=(200, 200, 190))
    sh.save(out)
    return sh.size


def main(argv):
    if argv[0] == "one":
        geo = json.load(open(argv[1]))
        tex = Image.open(argv[2])
        pose = None
        if "--anim" in argv:
            i = argv.index("--anim")
            pose = sample_anim(argv[i + 1], argv[i + 2], float(argv[i + 3]))
        S8 = px_per_m(8.0) / 16.0
        ims = [render(geo, tex, v, S8 * 3, pose=pose, bg="sky") for v in ("side", "front", "3/4")]
        W = sum(i.width for i in ims) + 20
        Hh = max(i.height for i in ims)
        out = Image.new("RGB", (W, Hh), (34, 36, 38))
        x = 0
        for im in ims:
            out.paste(im.convert("RGB"), (x, 0))
            x += im.width + 10
        out.save(argv[3])
    elif argv[0] == "icons":
        print(icon_sheet(argv[1]))
    elif argv[0] == "sheets":  # every contact sheet in tools/modelgen/sheets plus the item sheet
        for f in sorted(os.listdir(os.path.join(HERE, "sheets"))):
            spec = json.load(open(os.path.join(HERE, "sheets", f)))
            print(f, sheet(spec, os.path.join(ROOT, spec["out"])))
        print("items", icon_sheet(os.path.join(ROOT, "docs", "art", "previews", "items.png")))
    elif argv[0] == "sheet":
        print(sheet(json.load(open(argv[1])), argv[2]))


if __name__ == "__main__":
    main(sys.argv[1:])
