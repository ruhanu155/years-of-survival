"""artgen: recipe JSON -> 16 x 16 textures inside the palette, plus item model JSON.

Usage (from the repository root):
  python3 -I tools/artgen/artgen.py build [--kinds item,block,plant] [ids...]
  python3 -I tools/artgen/artgen.py check      palette lint + every generated texture against the rules
  python3 -I tools/artgen/artgen.py lint       palette ramps only

Recipe kinds:
  icon        shapes (poly, ellipse, ring, line, rect, pixels) filled with ramp steps; lit rim from the top
              left, shadow rim bottom right, 1 px #1a1512 outline outside the silhouette (Art spec, Item icons)
  found_icon  a found CC0/CC-BY sprite under art-src/found/, recoloured colour by colour onto ramp steps
              (`map`), edited (`edits`), re-outlined; provenance goes to credits.json
  block, plant  the prototype renderers (tiling faces, plant phase sprites)
Every random draw seeds from SHA-256 of the recipe id and the layer, so output is byte-identical."""
import json, os, sys, math, hashlib, glob
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import palette_lib as P  # noqa: E402

VERSION = "artgen 0.4.0"
RAMPS = P.RAMPS
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice")
OUT_RGB = P.hexrgb(P.OUTLINE)


def seed_of(*parts):
    return int(hashlib.sha256("|".join(map(str, parts)).encode()).hexdigest()[:8], 16)


def rgba(ramp, step):
    return P.hexrgb(RAMPS[ramp][int(np.clip(step, 0, 4))]) + (255,)


# ----------------------------------------------------------------------------- icons
def shape_mask(sh, n=16):
    m = Image.new("L", (n, n), 0)
    d = ImageDraw.Draw(m)
    k = sh["kind"]
    if k == "poly":
        d.polygon([tuple(p) for p in sh["points"]], fill=255)
    elif k == "ellipse":
        d.ellipse(sh["box"], fill=255)
    elif k == "ring":
        d.ellipse(sh["box"], outline=255, width=sh.get("width", 1))
    elif k == "line":
        d.line([tuple(p) for p in sh["points"]], fill=255, width=sh.get("width", 1))
    elif k == "rect":
        d.rectangle(sh["box"], fill=255)
    elif k == "pixels":
        for p in sh["points"]:
            d.point(tuple(p), fill=255)
    return np.array(m) > 0


def finish(ramp_map, step, n=16, rim=True, outline=True, keep=None):
    """Turn ramp and step maps into RGBA: lit rim +1 step top left, shadow rim -1 bottom right,
    then the outline 1 px outside the silhouette. `keep` marks pixels whose step is final."""
    alpha = ramp_map != ""
    out = np.zeros((n, n, 4), np.uint8)
    for y in range(n):
        for x in range(n):
            if not alpha[y, x]:
                continue
            if ramp_map[y, x] == "@outline":
                out[y, x] = OUT_RGB + (255,)
                continue
            s = step[y, x]
            if rim and not (keep is not None and keep[y, x]):
                edge = lambda yy, xx: not (0 <= yy < n and 0 <= xx < n) or not alpha[yy, xx] or ramp_map[yy, xx] == "@outline"
                if edge(y - 1, x) or edge(y, x - 1):
                    s += 1
                elif edge(y + 1, x) or edge(y, x + 1):
                    s -= 1
            out[y, x] = rgba(ramp_map[y, x], s)
    if outline:
        for y in range(n):
            for x in range(n):
                if alpha[y, x]:
                    continue
                if any(0 <= y + dy < n and 0 <= x + dx < n and alpha[y + dy, x + dx] and ramp_map[y + dy, x + dx] != "@outline"
                       for dy, dx in ((0, 1), (1, 0), (0, -1), (-1, 0))):
                    out[y, x] = OUT_RGB + (255,)
    return Image.fromarray(out, "RGBA")


def draw_shapes(r, shapes, ramp_map, step, keep, n=16):
    for si, sh in enumerate(shapes):
        mm = shape_mask(sh, n)
        if sh.get("erase"):
            ramp_map[mm] = ""
            continue
        ramp_map[mm] = sh["ramp"]
        step[mm] = sh.get("step", 2)
        keep[mm] = sh.get("flat", False)
        if sh.get("noise"):  # +-1 step dither inside the shape, at most a quarter of its pixels
            rng = np.random.default_rng(seed_of(r["id"], si))
            k = int(mm.sum())
            step[mm] += (rng.random(k) < sh["noise"]).astype(int) * rng.choice([-1, 1], k)
        if sh.get("speckle"):
            rng = np.random.default_rng(seed_of(r["id"], si, "sp"))
            sp = sh["speckle"]
            hit = mm & (rng.random((n, n)) < sp["density"])
            ramp_map[hit] = sp.get("ramp", sh["ramp"])
            step[hit] = sp["step"]
            keep[hit] = True


def render_icon(r):
    n = 16
    ramp_map = np.full((n, n), "", object)
    step = np.zeros((n, n), int)
    keep = np.zeros((n, n), bool)
    draw_shapes(r, r["shapes"], ramp_map, step, keep, n)
    return finish(ramp_map, step, n, rim=r.get("rim", True), keep=keep)


def render_found(r):
    """Recolour a found sprite: every source colour maps to a ramp step (or the outline), so the
    found shading and pixel clusters survive but only palette entries are written."""
    src = Image.open(os.path.join(ROOT, r["source"])).convert("RGBA")
    n = 16
    if src.width > n or src.height > n:
        f = max(src.width, src.height) / n
        src = src.resize((round(src.width / f), round(src.height / f)), Image.NEAREST)
    canvas = Image.new("RGBA", (n, n))
    ox = (n - src.width) // 2 + r.get("offset", [0, 0])[0]
    oy = (n - src.height) // 2 + r.get("offset", [0, 0])[1]
    canvas.paste(src, (ox, oy))
    if r.get("flip"):
        canvas = canvas.transpose(Image.FLIP_LEFT_RIGHT)
    a = np.array(canvas)
    ramp_map = np.full((n, n), "", object)
    step = np.zeros((n, n), int)
    keep = np.ones((n, n), bool)
    cmap = {k.lower(): v for k, v in r["map"].items()}
    for y in range(n):
        for x in range(n):
            if a[y, x, 3] < 128:
                continue
            h = "#%02x%02x%02x" % tuple(a[y, x, :3])
            if h not in cmap:
                raise SystemExit(f"{r['id']}: source colour {h} has no mapping")
            v = cmap[h]
            if v == "outline":
                ramp_map[y, x] = "@outline"
            elif v is None:
                continue
            else:
                ramp_map[y, x], step[y, x] = v[0], v[1]
    draw_shapes(r, r.get("edits", []), ramp_map, step, keep, n)
    return finish(ramp_map, step, n, rim=False, keep=keep)


# ----------------------------------------------------------------------------- blocks and plants (prototype)
def value_noise(n, scale, octaves, rng):
    out = np.zeros((n, n))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        cells = max(1, int(scale * 2 ** o))
        g = rng.random((cells, cells))
        ys = np.arange(n) * cells / n
        y0 = np.floor(ys).astype(int)
        fy = ys - y0
        y1 = (y0 + 1) % cells
        fy = fy * fy * (3 - 2 * fy)
        a_ = g[y0][:, y0]; b_ = g[y0][:, y1]; c_ = g[y1][:, y0]; d_ = g[y1][:, y1]
        fx = fy[None, :]; fyy = fy[:, None]
        out += ((a_ * (1 - fx) + b_ * fx) * (1 - fyy) + (c_ * (1 - fx) + d_ * fx) * fyy) * amp
        tot += amp
        amp *= 0.5
    return out / tot


def render_block(r):
    n = r.get("size", 16)
    idx = np.zeros((n, n), int)
    ramp_map = np.full((n, n), r["layers"][0]["ramp"], object)
    for li, L in enumerate(r["layers"]):
        rng = np.random.default_rng(seed_of(r["id"], li))
        t = L["type"]
        if t == "noise":
            f = value_noise(n, L.get("scale", 4), L.get("octaves", 2), rng)
            if L.get("stretch") == "vertical":
                f = np.repeat(f.mean(axis=0, keepdims=True), n, 0) * 0.7 + f * 0.3
            f = (f - f.min()) / max(1e-6, np.ptp(f))
            lo, hi = L.get("steps", [0, 4])
            idx = np.clip(np.round(lo + f * (hi - lo)), lo, hi).astype(int)
            ramp_map[:] = L["ramp"]
        elif t == "furrows":
            x = int(rng.integers(0, L["spacing"]))
            while x < n:
                for y in range(n):
                    xx = (x + (rng.integers(-L.get("jitter", 1), L.get("jitter", 1) + 1) if rng.random() < 0.3 else 0)) % n
                    idx[y, xx] = L.get("step", 0)
                x += L["spacing"] + int(rng.integers(0, 2))
        elif t == "plates":
            for _ in range(L["count"]):
                y = int(rng.integers(0, n)); x0 = int(rng.integers(0, n)); w = int(rng.integers(2, 5))
                for k in range(w):
                    idx[y, (x0 + k) % n] = L.get("step", 1)
        elif t == "rings":
            cx = cy = (n - 1) / 2
            yy, xx = np.mgrid[0:n, 0:n]
            dd = np.hypot(yy - cy, xx - cx) + value_noise(n, 3, 1, rng) * 1.2
            ring = (np.floor(dd / L["spacing"]) % 2 == 0)
            idx = np.where(ring, L["steps"][0], L["steps"][1])
            ramp_map[:] = L["ramp"]
            idx[dd > n / 2 - 0.5] = 0
            ramp_map[dd > n / 2 - 0.5] = L.get("bark_ramp", L["ramp"])
        elif t == "speckle":
            m = rng.random((n, n)) < L["density"]
            idx[m] = L["step"]
            ramp_map[m] = L["ramp"]
    img = Image.new("RGBA", (n, n))
    px = img.load()
    for y in range(n):
        for x in range(n):
            px[x, y] = rgba(ramp_map[y, x], idx[y, x])
    return img


# ----------------------------------------------------------------------------- driver
def remap_image(img, remap):
    a = np.array(img)
    lut = {}
    for src, dst in remap.items():
        for i, h in enumerate(RAMPS[src]):
            lut[P.hexrgb(h)] = P.hexrgb(RAMPS[dst][i])
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            if a[y, x, 3]:
                c = tuple(int(v) for v in a[y, x, :3])
                if c in lut:
                    a[y, x, :3] = lut[c]
    return Image.fromarray(a, "RGBA")


def item_model(name, parent):
    return {"parent": parent, "textures": {"layer0": f"yearofice:item/{name}"}}


def build(kinds=("item",), only=None):
    report = []
    for kind in kinds:
        for fn in sorted(glob.glob(os.path.join(HERE, "recipes", kind, "*.json"))):
            r = json.load(open(fn))
            name = r["id"].split(":")[1]
            if only and name not in only:
                continue
            imgs = {}
            if r["kind"] == "icon":
                imgs[f"{r.get('folder', 'item')}/{name}"] = render_icon(r)
            elif r["kind"] == "found_icon":
                imgs[f"{r.get('folder', 'item')}/{name}"] = render_found(r)
            elif r["kind"] == "block":
                for face, fr in r["faces"].items():
                    imgs[f"block/{name}_{face}"] = render_block({**fr, "id": r["id"] + face})
            else:
                continue
            base_key = next(iter(imgs))
            for var, remap in r.get("variants", {}).items():
                if isinstance(remap, dict) and all(k in RAMPS for k in remap):
                    imgs[f"{base_key}_{var}"] = remap_image(imgs[base_key], remap)
            for k, im in imgs.items():
                p = os.path.join(ASSETS, "textures", k + ".png")
                os.makedirs(os.path.dirname(p), exist_ok=True)
                im.save(p)
                ok, nc, ramps, probs = P.check_image(im)
                if r.get("min_area"):
                    pass
                report.append((k, ok, nc, ramps, probs))
            if r.get("folder", "item") == "item":
                mp = os.path.join(ASSETS, "models", "item", name + ".json")
                os.makedirs(os.path.dirname(mp), exist_ok=True)
                json.dump(item_model(name, r.get("model_parent", "minecraft:item/generated")), open(mp, "w"), indent=1)
    bad = 0
    for k, ok, nc, ramps, probs in report:
        print(f"{'PASS' if ok else 'FAIL'} {k} colours={nc} ramps={','.join(ramps)} {'; '.join(probs)}")
        bad += not ok
    print(f"{len(report)} textures, {bad} failures")
    return bad


def check():
    bad = 0
    errs = P.lint_all()
    for e in errs:
        print("PALETTE", e)
    bad += len(errs)
    for p in sorted(glob.glob(os.path.join(ASSETS, "textures", "**", "*.png"), recursive=True)):
        im = Image.open(p)
        entity = "/entity/" in p
        ok, nc, ramps, probs = P.check_image(im)
        a = np.array(im.convert("RGBA"))[..., 3]
        if "/item/" in p and (a > 0).sum() < 30:
            ok = False
            probs.append("icon silhouette under 30 px")
        if not ok:
            bad += 1
            print("FAIL", os.path.relpath(p, ROOT), "; ".join(probs))
    print("check:", "PASS" if not bad else f"{bad} failures")
    return bad


if __name__ == "__main__":
    a = sys.argv[1:] or ["build"]
    if a[0] == "lint":
        e = P.lint_all()
        print("\n".join(e) or "palette PASS")
        sys.exit(bool(e))
    if a[0] == "check":
        sys.exit(1 if check() else 0)
    kinds = ("item",)
    if "--kinds" in a:
        i = a.index("--kinds")
        kinds = tuple(a[i + 1].split(","))
        a = a[:i] + a[i + 2:]
    sys.exit(1 if build(kinds, set(a[1:]) or None) else 0)
