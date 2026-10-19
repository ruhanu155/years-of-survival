"""Shared geometry helpers for modelgen and preview: box-UV layout, face orientation and the
bone transform convention GeckoLib uses for Bedrock geometry.

Face orientation (Blockbench box UV, which reproduces the vanilla net):
  up    (u0+d,     v0)     w x d   image left = +X side, image bottom = front (-Z)
  down  (u0+d+w,   v0)     w x d   image left = +X side, image top = front
  east  (u0,       v0+d)   d x h   +X side, image right = front
  north (u0+d,     v0+d)   w x h   front, image left = +X side
  west  (u0+d+w,   v0+d)   d x h   -X side, image left = front
  south (u0+2d+w,  v0+d)   w x h   back, image left = -X side
Semantic coordinates used by painters: xf 0..1 from the +X side to the -X side, yf 0..1 from top
to bottom, zf 0..1 from front (-Z) to back (+Z).

Rotation: GeckoLib bakes rotation (-x, -y, z) in an X-mirrored space and applies Z, then Y, then X.
Mapped back to file space that is R = Rz(-z) . Ry(y) . Rx(-x) with right-handed rotations, so a
positive X rotation pitches a forward-pointing bone nose-down, as Bedrock's look_at_target does."""
import math
import numpy as np

FACES = ("up", "down", "east", "north", "west", "south")


def face_rect(face, size, uv):
    w, h, d = size
    u0, v0 = uv
    return {
        "up": (u0 + d, v0, w, d),
        "down": (u0 + d + w, v0, w, d),
        "east": (u0, v0 + d, d, h),
        "north": (u0 + d, v0 + d, w, h),
        "west": (u0 + d + w, v0 + d, d, h),
        "south": (u0 + 2 * d + w, v0 + d, w, h),
    }[face]


def island(size):
    w, h, d = size
    return 2 * (d + w), d + h


def texel_semantic(face, iu, iv, fw, fh):
    """Centre of texel (iu, iv) inside a face region of fw x fh -> (xf, yf, zf); None where unused."""
    a = (iu + 0.5) / fw
    b = (iv + 0.5) / fh
    if face == "east":
        return None, b, 1 - a
    if face == "west":
        return None, b, a
    if face == "north":
        return a, b, None
    if face == "south":
        return 1 - a, b, None
    if face == "up":
        return a, None, 1 - b
    if face == "down":
        return a, None, b
    raise ValueError(face)


def semantic_texel(face, xf, yf, zf, fw, fh):
    def cl(v, n):
        return min(n - 1, max(0, int(math.floor(v * n))))
    if face == "east":
        return cl(1 - zf, fw), cl(yf, fh)
    if face == "west":
        return cl(zf, fw), cl(yf, fh)
    if face == "north":
        return cl(xf, fw), cl(yf, fh)
    if face == "south":
        return cl(1 - xf, fw), cl(yf, fh)
    if face == "up":
        return cl(xf, fw), cl(1 - zf, fh)
    if face == "down":
        return cl(xf, fw), cl(zf, fh)
    raise ValueError(face)


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def rot_matrix(rot):
    x, y, z = (math.radians(v) for v in rot)
    return rz(-z) @ ry(y) @ rx(-x)


def affine(rot, pivot):
    m = np.eye(4)
    r = rot_matrix(rot)
    p = np.array(pivot, float)
    m[:3, :3] = r
    m[:3, 3] = p - r @ p
    return m


def bone_matrices(bones, pose=None):
    """World matrix per bone name. pose maps bone -> dict(rotation, position, scale) added on top."""
    pose = pose or {}
    by = {b["name"]: b for b in bones}
    out = {}

    def get(n):
        if n in out:
            return out[n]
        b = by[n]
        rot = list(b.get("rotation", [0, 0, 0]))
        extra = pose.get(n, {})
        er = extra.get("rotation", [0, 0, 0])
        rot = [rot[i] + er[i] for i in range(3)]
        m = affine(rot, b["pivot"])
        sc = extra.get("scale")
        if sc:
            s = np.eye(4)
            p = np.array(b["pivot"], float)
            s[:3, :3] = np.diag(sc)
            s[:3, 3] = p - np.diag(sc) @ p
            m = m @ s
        pos = extra.get("position")
        if pos:
            t = np.eye(4)
            t[:3, 3] = [-pos[0], pos[1], pos[2]]  # GeckoLib negates X position too
            m = t @ m
        par = b.get("parent")
        out[n] = (get(par) if par else np.eye(4)) @ m
        return out[n]

    for b in bones:
        get(b["name"])
    return out
