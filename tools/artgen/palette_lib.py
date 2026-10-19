"""Palette access, ramp generation and the palette rules of the Art spec (style guide > Palette rules).

Every generator imports this module, so a texture can only ever be painted from ramp entries."""
import json, os, colorsys

HERE = os.path.dirname(os.path.abspath(__file__))
PAL_PATH = os.path.join(HERE, "palette.json")
OUTLINE = "#1a1512"


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def rgbhex(c):
    return "#%02x%02x%02x" % tuple(int(round(v)) for v in c)


def load():
    pal = json.load(open(PAL_PATH))
    ramps = {}
    kind = {}
    for group, k in (("ramps", "material"), ("accent", "accent"), ("emissive", "emissive")):
        for name, steps in pal.get(group, {}).items():
            ramps[name] = steps
            kind[name] = k
    return pal, ramps, kind


PAL, RAMPS, KIND = load()


def lin(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def unlin(v):
    v = max(0.0, min(1.0, v))
    return 255 * (12.92 * v if v <= 0.0031308 else 1.055 * v ** (1 / 2.4) - 0.055)


def luminance(rgb):
    r, g, b = (lin(v) for v in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def chroma(rgb):
    return (max(rgb) - min(rgb)) / 255.0


def hue_deg(rgb):
    h, _, _ = colorsys.rgb_to_hsv(*(v / 255.0 for v in rgb))
    return h * 360


def hue_diff(a, b):
    d = (a - b) % 360
    return min(d, 360 - d)


def make_ramp(base_hex, ratio=1.55, drift_deg=4.0, sat_edge=0.88):
    """Five steps around a step-2 base: luminance x ratio per step, hue drifting toward
    blue-violet in the shadows and toward yellow in the lights, saturation easing at both ends."""
    base = hexrgb(base_hex)
    h, s, v = colorsys.rgb_to_hsv(*(x / 255.0 for x in base))
    out = []
    for k in range(5):
        d = k - 2
        target_h = 250.0 if d < 0 else 55.0
        hh = h * 360
        diff = ((target_h - hh + 180) % 360) - 180
        hh = (hh + max(-abs(drift_deg * d), min(abs(drift_deg * d), diff))) % 360
        ss = s * (sat_edge ** abs(d))
        r, g, b = colorsys.hsv_to_rgb(hh / 360, ss, v)
        # scale in linear light to hit the luminance ratio
        f = ratio ** d
        rgb = [unlin(lin(c * 255) * f) for c in (r, g, b)]
        out.append(rgbhex(rgb))
    return out


def lint_ramp(name, steps, kind="material"):
    errs = []
    cols = [hexrgb(h) for h in steps]
    if len(cols) != 5:
        errs.append(f"{name}: {len(cols)} steps, need 5")
    if kind != "emissive":
        lim = 0.45 if kind == "material" else 0.70
        for i, c in enumerate(cols):
            if chroma(c) > lim + 1e-9:
                errs.append(f"{name}[{i}] chroma {chroma(c):.2f} > {lim}")
        for i in range(1, len(cols)):
            if name in ("bark_birch", "bark_aspen") and i == 1:
                continue
            a, b = luminance(cols[i - 1]), luminance(cols[i])
            r = b / max(a, 1e-6)
            if not (1.15 - 0.01 <= r <= 2.5 + 0.01):  # 0.01 for 8-bit rounding
                errs.append(f"{name}[{i - 1}->{i}] luminance ratio {r:.2f} outside 1.15-2.5")
        hues = [hue_deg(c) for c in cols if chroma(c) >= 0.08]
        if len(hues) >= 2:
            span = max(hue_diff(x, y) for x in hues for y in hues)
            if span > 20 + 1e-6:
                errs.append(f"{name}: hue drift {span:.0f} deg > 20")
    for c in cols:
        if c in ((0, 0, 0), (255, 255, 255)):
            errs.append(f"{name}: pure black or white")
    return errs


def lint_all():
    errs = []
    for n, s in RAMPS.items():
        errs += lint_ramp(n, s, KIND[n])
    return errs


def allowed_colours():
    return {hexrgb(h) for r in RAMPS.values() for h in r} | {hexrgb(OUTLINE)}


COLOUR_RAMP = {}
for _n, _s in RAMPS.items():
    for _i, _h in enumerate(_s):
        COLOUR_RAMP.setdefault(hexrgb(_h), (_n, _i))


def check_image(img, max_ramps=3, max_colours=12, outline_ok=True):
    """Rule 4, 5 and 6 on one RGBA image: returns (ok, colours, ramps, problems)."""
    probs = []
    cols = {p[:3] for p in img.convert("RGBA").getdata() if p[3] > 0}
    alphas = {p[3] for p in img.convert("RGBA").getdata()}
    if alphas - {0, 255}:
        probs.append("partial alpha")
    allowed = allowed_colours()
    bad = cols - allowed
    if bad:
        probs.append(f"{len(bad)} off-palette colours")
    ramps = {COLOUR_RAMP[c][0] for c in cols if c in COLOUR_RAMP and c != hexrgb(OUTLINE)}
    if len(ramps) > max_ramps:
        probs.append(f"{len(ramps)} ramps > {max_ramps}")
    if len(cols) > max_colours:
        probs.append(f"{len(cols)} colours > {max_colours}")
    return not probs, len(cols), sorted(ramps), probs
