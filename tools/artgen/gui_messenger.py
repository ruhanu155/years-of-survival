"""gui_messenger: the Trail Messenger's GUI textures and the vitals strip icons, painted from palette ramps only.

Usage (from the repository root):
  python3 -I tools/artgen/gui_messenger.py          write the textures
  python3 -I tools/artgen/gui_messenger.py --check  write, then run the palette rules on them

Outputs (assets/yearofice/textures/...):
  gui/messenger/device.png   256 x 256  the full-screen rugged handheld: orange body, iron bezel and buttons, LCD
  gui/messenger/corner.png    96 x 80   the small device shown in the screen corner while it is held
  gui/sprites/hud/<icon>.png   9 x 9    vitals strip icons: warmth, wet, water, calories, energy, condition

Layout constants are mirrored in client.ui.messenger.MessengerLayout; change both together.
Everything is drawn with integer geometry, so the output is byte-identical on every run."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import palette_lib as P  # noqa: E402

TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "yearofice", "textures")
OUT = P.hexrgb(P.OUTLINE) + (255,)


def c(ramp, step):
    return P.hexrgb(P.RAMPS[ramp][step]) + (255,)


def rrect(d, box, r, fill, outline=None):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline)


# --------------------------------------------------------------------------------------------- the full device
# Body 24..231 x 6..251; LCD 36..219 x 30..186 (184 x 157); buttons below.
DEVICE = dict(body=(24, 6, 231, 251), bezel=(31, 24, 224, 192), lcd=(36, 30, 219, 186))


def device():
    im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    bx0, by0, bx1, by1 = DEVICE["body"]
    # stub antenna, top right
    rrect(d, (186, 0, 200, by0 + 8), 3, c("iron", 2), OUT)
    d.line((189, 2, 189, by0 + 4), fill=c("iron", 3))
    # body: outline, shadow, fill, highlight
    rrect(d, (bx0, by0, bx1, by1), 14, OUT)
    rrect(d, (bx0 + 1, by0 + 1, bx1 - 1, by1 - 1), 13, c("plastic_orange", 1))
    rrect(d, (bx0 + 1, by0 + 1, bx1 - 3, by1 - 3), 13, c("plastic_orange", 2))
    d.arc((bx0 + 2, by0 + 2, bx0 + 28, by0 + 28), 180, 270, fill=c("plastic_orange", 3))
    d.line((bx0 + 14, by0 + 2, bx1 - 16, by0 + 2), fill=c("plastic_orange", 3))
    d.line((bx0 + 2, by0 + 14, bx0 + 2, by1 - 16), fill=c("plastic_orange", 3))
    # rubber corner bumpers
    for (x, y) in ((bx0, by0), (bx1 - 18, by0), (bx0, by1 - 18), (bx1 - 18, by1 - 18)):
        rrect(d, (x, y, x + 18, y + 18), 8, c("iron", 1), OUT)
        d.point((x + 5, y + 5), fill=c("iron", 2))
    # screw heads between the bumpers
    for (x, y) in ((bx0 + 6, by0 + 120), (bx1 - 9, by0 + 120)):
        d.ellipse((x, y, x + 3, y + 3), fill=c("iron", 2), outline=OUT)
    # bezel and LCD
    rrect(d, DEVICE["bezel"], 6, c("iron", 0), OUT)
    lx0, ly0, lx1, ly1 = DEVICE["lcd"]
    d.rectangle((lx0, ly0, lx1, ly1), fill=c("lichen_pale", 3))
    d.line((lx0, ly0, lx1, ly0), fill=c("lichen_pale", 1))
    d.line((lx0, ly0, lx0, ly1), fill=c("lichen_pale", 1))
    d.line((lx0 + 1, ly0 + 1, lx1, ly0 + 1), fill=c("lichen_pale", 2))
    # keypad: d-pad left, two round keys right, the SOS key under a guard in the middle
    cx, cy = 76, 220
    rrect(d, (cx - 22, cy - 7, cx + 22, cy + 7), 3, c("iron", 1), OUT)
    rrect(d, (cx - 7, cy - 22, cx + 7, cy + 22), 3, c("iron", 1), OUT)
    d.rectangle((cx - 21, cy - 6, cx + 21, cy + 6), fill=c("iron", 1))
    d.point((cx, cy), fill=c("iron", 3))
    for (x, y) in ((cx - 15, cy), (cx + 15, cy), (cx, cy - 15), (cx, cy + 15)):
        d.point((x, y), fill=c("iron", 3))
    for kx in (164, 196):
        d.ellipse((kx - 11, cy - 11, kx + 11, cy + 11), fill=c("iron", 2), outline=OUT)
        d.arc((kx - 9, cy - 9, kx + 9, cy + 9), 200, 290, fill=c("iron", 4))
    rrect(d, (113, 206, 143, 234), 4, c("iron", 0), OUT)
    rrect(d, (118, 211, 138, 229), 3, c("plastic_orange", 3), OUT)
    # grip ridges on the lower body
    for y in range(240, 246, 3):
        d.line((100, y, 156, y), fill=c("plastic_orange", 1))
    return im


# --------------------------------------------------------------------------------------------- the corner device
CORNER = dict(body=(2, 2, 93, 77), lcd=(9, 9, 86, 64))


def corner():
    im = Image.new("RGBA", (96, 80), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    bx0, by0, bx1, by1 = CORNER["body"]
    rrect(d, (bx0, by0, bx1, by1), 7, OUT)
    rrect(d, (bx0 + 1, by0 + 1, bx1 - 1, by1 - 1), 6, c("plastic_orange", 1))
    rrect(d, (bx0 + 1, by0 + 1, bx1 - 2, by1 - 2), 6, c("plastic_orange", 2))
    d.line((bx0 + 6, by0 + 1, bx1 - 7, by0 + 1), fill=c("plastic_orange", 3))
    lx0, ly0, lx1, ly1 = CORNER["lcd"]
    d.rectangle((lx0 - 2, ly0 - 2, lx1 + 2, ly1 + 2), fill=c("iron", 0), outline=OUT)
    d.rectangle((lx0, ly0, lx1, ly1), fill=c("lichen_pale", 3))
    d.line((lx0, ly0, lx1, ly0), fill=c("lichen_pale", 1))
    for kx in (30, 48, 66):
        d.ellipse((kx - 3, 68, kx + 3, 74), fill=c("iron", 2), outline=OUT)
    return im


# --------------------------------------------------------------------------------------------- strip icons, 9 x 9
def icon(pixels, ramps):
    """pixels: 9 strings; '.' clear, 'o' outline, digits 0-4 index into ramps[0], letters a-e into ramps[1]."""
    im = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
    for y, row in enumerate(pixels):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch == "o":
                im.putpixel((x, y), OUT)
            elif ch.isdigit():
                im.putpixel((x, y), c(ramps[0], int(ch)))
            else:
                im.putpixel((x, y), c(ramps[1], "abcde".index(ch)))
    return im


ICONS = {
    # thermometer, bulb at the bottom; fills down as warmth falls
    "warmth": (["...ooo...",
                "...odo...",
                "...odo...",
                "...o2o...",
                "...o2o...",
                "..o222o..",
                ".o22322o.",
                ".o22222o.",
                "..ooooo.."], ("berry_red", "snow")),
    # a falling drop
    "wet": (["....o....",
             "...o3o...",
             "...o3o...",
             "..o343o..",
             "..o333o..",
             ".o32333o.",
             ".o32233o.",
             "..o222o..",
             "...ooo..."], ("ice", "snow")),
    # a cup
    "water": ([".........",
               ".ooooooo.",
               ".o34443o.",
               ".obbbbbo.",
               ".obcbbbo.",
               ".obbbbbo.",
               "..obbbo..",
               "..ooooo..",
               "........."], ("ice", "wood_cut")),
    # a bowl with food
    "calories": ([".........",
                  "...232...",
                  "..23432..",
                  "oooooooo.",
                  "obddddbo.",
                  ".obbbbo..",
                  "..obbo...",
                  "...oo....",
                  "........."], ("meat_cooked", "wood_cut")),
    # a bolt
    "energy": (["....ooo..",
                "...o43o..",
                "..o43o...",
                ".o4333oo.",
                ".oo3332o.",
                "...o32o..",
                "..o32o...",
                "..o2o....",
                "..oo....."], ("flower_yellow", "snow")),
    # a cross
    "condition": ([".........",
                   "...ooo...",
                   "...o2o...",
                   ".ooo2ooo.",
                   ".o22222o.",
                   ".ooo2ooo.",
                   "...o2o...",
                   "...ooo...",
                   "........."], ("berry_red", "snow")),
}


def main():
    out = []
    os.makedirs(os.path.join(TEX, "gui", "messenger"), exist_ok=True)
    os.makedirs(os.path.join(TEX, "gui", "sprites", "hud"), exist_ok=True)
    p = os.path.join(TEX, "gui", "messenger", "device.png")
    device().save(p, optimize=False)
    out.append(p)
    p = os.path.join(TEX, "gui", "messenger", "corner.png")
    corner().save(p, optimize=False)
    out.append(p)
    for name, (px, ramps) in ICONS.items():
        p = os.path.join(TEX, "gui", "sprites", "hud", name + ".png")
        icon(px, ramps).save(p, optimize=False)
        out.append(p)
    bad = 0
    for p in out:
        ok, nc, ramps, probs = P.check_image(Image.open(p))
        print(("PASS" if ok else "FAIL"), os.path.relpath(p, ROOT), f"colours={nc}", ",".join(ramps), "; ".join(probs))
        bad += not ok
    if "--check" in sys.argv and bad:
        sys.exit(1)


if __name__ == "__main__":
    main()
