"""Placeholder 16x16 item art for the B11 hot tent kit (original, flat colours). Usage: python3 -I tools/artgen/tent_items.py"""
import json, struct, zlib

T = "src/main/resources/assets/yearofice/textures/item/"; M = "src/main/resources/assets/yearofice/models/item/"

def png(path, pix):
    raw = b"".join(b"\x00" + bytes(sum(([*c, 255] if c else [0, 0, 0, 0] for c in row), [])) for row in pix)
    def ch(t, d): return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    open(path, "wb").write(b"\x89PNG\r\n\x1a\n" + ch(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) + ch(b"IDAT", zlib.compress(raw)) + ch(b"IEND", b""))

def grid(): return [[None] * 16 for _ in range(16)]
def rect(g, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): g[y][x] = c

items = {}
g = grid(); rect(g, 2, 5, 13, 13, (196, 184, 150)); rect(g, 6, 3, 9, 4, (176, 164, 132)); rect(g, 7, 9, 8, 13, (120, 108, 84)); items["canvas_wall_tent"] = g
g = grid(); rect(g, 4, 4, 11, 11, (150, 150, 156)); rect(g, 7, 7, 8, 8, (40, 40, 44)); items["stove_jack"] = g
g = grid(); rect(g, 3, 6, 12, 13, (70, 70, 76)); rect(g, 5, 2, 6, 6, (110, 110, 116)); rect(g, 5, 9, 10, 11, (190, 80, 30)); items["tent_stove"] = g
g = grid(); rect(g, 5, 5, 10, 13, (160, 160, 166)); rect(g, 5, 5, 10, 6, (120, 120, 126)); rect(g, 7, 2, 8, 5, (100, 100, 106)); rect(g, 6, 9, 9, 11, (210, 110, 40)); items["can_stove"] = g
g = grid(); rect(g, 3, 3, 12, 12, (60, 60, 64))
for i in range(3, 13, 2): rect(g, i, 3, i, 12, (150, 150, 156)); rect(g, 3, i, 12, i, (150, 150, 156))
items["spark_screen"] = g
for k, g in items.items():
    png(T + k + ".png", g)
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "yearofice:item/" + k}}, open(M + k + ".json", "w"), indent=1)
