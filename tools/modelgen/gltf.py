"""Minimal glTF 2.0 / GLB reader (NumPy only) for `modelgen import`.

Reads node hierarchy, skinned mesh positions, joints, weights and vertex colours
or material colours. No external dependencies, so the import step runs in the
same pinned environment as the generators."""
import json, struct, os
import numpy as np

COMP = {5120: np.int8, 5121: np.uint8, 5122: np.int16, 5123: np.uint16, 5125: np.uint32, 5126: np.float32}
NCOMP = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}


def load(path):
    data = open(path, "rb").read()
    if data[:4] == b"glTF":
        n = struct.unpack("<I", data[12:16])[0]
        j = json.loads(data[20:20 + n])
        off = 20 + n
        bl = struct.unpack("<I", data[off:off + 4])[0]
        bins = [data[off + 8:off + 8 + bl]]
    else:
        j = json.loads(data)
        bins = [open(os.path.join(os.path.dirname(path), b["uri"]), "rb").read() for b in j["buffers"]]
    return Gltf(j, bins)


def quat_mat(q):
    x, y, z, w = q
    return np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                     [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                     [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])


class Gltf:
    def __init__(self, j, bins):
        self.j, self.bins = j, bins
        self.nodes = j["nodes"]
        self.parent = {}
        for i, nd in enumerate(self.nodes):
            for c in nd.get("children", []):
                self.parent[c] = i
        self.world = [None] * len(self.nodes)
        for i in range(len(self.nodes)):
            self._world(i)

    def accessor(self, idx):
        a = self.j["accessors"][idx]
        bv = self.j["bufferViews"][a["bufferView"]]
        buf = self.bins[bv.get("buffer", 0)]
        dt = np.dtype(COMP[a["componentType"]])
        nc = NCOMP[a["type"]]
        start = bv.get("byteOffset", 0) + a.get("byteOffset", 0)
        stride = bv.get("byteStride", 0) or dt.itemsize * nc
        raw = np.frombuffer(buf, np.uint8, count=stride * (a["count"] - 1) + dt.itemsize * nc, offset=start)
        out = np.lib.stride_tricks.as_strided(raw, (a["count"], dt.itemsize * nc), (stride, 1)).copy()
        arr = out.view(dt).reshape(a["count"], nc).astype(np.float64)
        if a.get("normalized"):
            arr /= np.iinfo(dt).max
        return arr

    def local(self, i):
        nd = self.nodes[i]
        if "matrix" in nd:
            return np.array(nd["matrix"], float).reshape(4, 4).T
        m = np.eye(4)
        r = quat_mat(nd.get("rotation", [0, 0, 0, 1]))
        s = np.diag(nd.get("scale", [1, 1, 1]))
        m[:3, :3] = r @ s
        m[:3, 3] = nd.get("translation", [0, 0, 0])
        return m

    def _world(self, i):
        if self.world[i] is None:
            p = self.parent.get(i)
            self.world[i] = (self._world(p) if p is not None else np.eye(4)) @ self.local(i)
        return self.world[i]

    def name(self, i):
        return self.nodes[i].get("name", f"node{i}")

    def joint_positions(self):
        """World-space bind position of every joint of the first skin, by name."""
        skin = self.j["skins"][0]
        return {self.name(n): self.world[n][:3, 3].copy() for n in skin["joints"]}

    def skinned_vertices(self):
        """All mesh vertices in world space with their dominant joint name and colour."""
        out_p, out_j, out_c = [], [], []
        skin = self.j.get("skins", [None])[0]
        joints = skin["joints"] if skin else []
        for ni, nd in enumerate(self.nodes):
            if "mesh" not in nd:
                continue
            mesh = self.j["meshes"][nd["mesh"]]
            for prim in mesh["primitives"]:
                at = prim["attributes"]
                pos = self.accessor(at["POSITION"])
                if "JOINTS_0" in at and skin:
                    # skinned: positions are in bind space of the skin; glTF says the mesh node transform is ignored
                    jw = self.accessor(at["WEIGHTS_0"]); jj = self.accessor(at["JOINTS_0"]).astype(int)
                    dom = jj[np.arange(len(jj)), jw.argmax(1)]
                    names = [self.name(joints[k]) for k in dom]
                    # bind pose: world = jointWorld . inverseBind . v (identical for every influence in bind pose)
                    ibm = self.accessor(skin["inverseBindMatrices"]).reshape(-1, 4, 4).transpose(0, 2, 1)
                    mats = np.stack([self.world[joints[k]] @ ibm[k] for k in range(len(joints))])
                    h = np.c_[pos, np.ones(len(pos))]
                    wp = np.einsum("nij,nj->ni", mats[dom], h)[:, :3]
                else:
                    m = self.world[ni]
                    wp = pos @ m[:3, :3].T + m[:3, 3]
                    names = [self.name(ni)] * len(pos)
                if "COLOR_0" in at:
                    col = self.accessor(at["COLOR_0"])[:, :3]
                else:
                    mat = self.j["materials"][prim["material"]] if "material" in prim else {}
                    base = mat.get("pbrMetallicRoughness", {}).get("baseColorFactor", [0.5, 0.5, 0.5, 1])[:3]
                    col = np.tile(base, (len(pos), 1))
                out_p.append(wp); out_j += names; out_c.append(col)
        return np.concatenate(out_p), np.array(out_j), np.concatenate(out_c)
