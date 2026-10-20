package com.yearofice.worldgen.debug;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A small, dependency-free reader and writer for structure-template NBT ({@code structure/**.nbt}): gzipped,
 * big-endian, the vanilla layout ({@code DataVersion}, {@code size}, {@code palette}, {@code blocks},
 * {@code entities}). The template tool writes the M1 placeholders with it and the data tests read them back
 * without starting Minecraft.
 */
public final class StructureNbt {
    private StructureNbt() {}

    /** Minecraft 1.21.1's data version. */
    public static final int DATA_VERSION = 3955;

    /** One block state: name and properties. */
    public record State(String name, Map<String, String> props) {}

    /** A template: size, palette and blocks (x, y, z, palette index). */
    public record Template(int sx, int sy, int sz, List<State> palette, List<int[]> blocks, int dataVersion,
                           Map<Integer, Map<String, String>> nbt) {
        public Template(int sx, int sy, int sz, List<State> palette, List<int[]> blocks, int dataVersion) {
            this(sx, sy, sz, palette, blocks, dataVersion, Map.of());
        }

        /** The string tags of block entity {@code i} (an index into {@code blocks}), or empty. */
        public Map<String, String> nbtOf(int i) { return nbt.getOrDefault(i, Map.of()); }

        public boolean holds(String blockName) {
            for (State s : palette) if (s.name().equals(blockName)) return true;
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------- write

    public static void write(Template t, OutputStream raw) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(buf);
        o.writeByte(10);
        o.writeUTF("");
        // DataVersion
        o.writeByte(3); o.writeUTF("DataVersion"); o.writeInt(t.dataVersion());
        // size
        o.writeByte(9); o.writeUTF("size"); o.writeByte(3); o.writeInt(3);
        o.writeInt(t.sx()); o.writeInt(t.sy()); o.writeInt(t.sz());
        // palette
        o.writeByte(9); o.writeUTF("palette"); o.writeByte(10); o.writeInt(t.palette().size());
        for (State s : t.palette()) {
            o.writeByte(8); o.writeUTF("Name"); o.writeUTF(s.name());
            if (!s.props().isEmpty()) {
                o.writeByte(10); o.writeUTF("Properties");
                for (Map.Entry<String, String> e : s.props().entrySet()) {
                    o.writeByte(8); o.writeUTF(e.getKey()); o.writeUTF(e.getValue());
                }
                o.writeByte(0);
            }
            o.writeByte(0);
        }
        // blocks
        o.writeByte(9); o.writeUTF("blocks"); o.writeByte(10); o.writeInt(t.blocks().size());
        for (int i = 0; i < t.blocks().size(); i++) {
            int[] b = t.blocks().get(i);
            o.writeByte(9); o.writeUTF("pos"); o.writeByte(3); o.writeInt(3);
            o.writeInt(b[0]); o.writeInt(b[1]); o.writeInt(b[2]);
            o.writeByte(3); o.writeUTF("state"); o.writeInt(b[3]);
            Map<String, String> nbt = t.nbtOf(i);
            if (!nbt.isEmpty()) {
                o.writeByte(10); o.writeUTF("nbt");
                for (Map.Entry<String, String> e : nbt.entrySet()) { o.writeByte(8); o.writeUTF(e.getKey()); o.writeUTF(e.getValue()); }
                o.writeByte(0);
            }
            o.writeByte(0);
        }
        // entities (none)
        o.writeByte(9); o.writeUTF("entities"); o.writeByte(10); o.writeInt(0);
        o.writeByte(0);
        o.flush();
        try (GZIPOutputStream gz = new GZIPOutputStream(raw)) {
            gz.write(buf.toByteArray());
        }
    }

    // -------------------------------------------------------------------------------------------- read

    @SuppressWarnings("unchecked")
    public static Template read(InputStream raw) throws IOException {
        DataInputStream in = new DataInputStream(new GZIPInputStream(raw));
        if (in.readByte() != 10) throw new IOException("root is not a compound");
        in.readUTF();
        Map<String, Object> root = compound(in);
        List<Object> size = (List<Object>) root.get("size");
        List<State> palette = new ArrayList<>();
        Object pal = root.get("palette");
        if (pal == null && root.get("palettes") instanceof List<?> ps && !ps.isEmpty()) pal = ps.get(0);
        for (Object o : (List<Object>) pal) {
            Map<String, Object> c = (Map<String, Object>) o;
            Map<String, String> props = new LinkedHashMap<>();
            if (c.get("Properties") instanceof Map<?, ?> m) for (Map.Entry<?, ?> e : m.entrySet()) props.put((String) e.getKey(), (String) e.getValue());
            palette.add(new State((String) c.get("Name"), props));
        }
        List<int[]> blocks = new ArrayList<>();
        Map<Integer, Map<String, String>> nbt = new LinkedHashMap<>();
        for (Object o : (List<Object>) root.get("blocks")) {
            Map<String, Object> c = (Map<String, Object>) o;
            List<Object> pos = (List<Object>) c.get("pos");
            if (c.get("nbt") instanceof Map<?, ?> m) {
                Map<String, String> tags = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) if (e.getValue() instanceof String sv) tags.put((String) e.getKey(), sv);
                nbt.put(blocks.size(), tags);
            }
            blocks.add(new int[] {(Integer) pos.get(0), (Integer) pos.get(1), (Integer) pos.get(2), (Integer) c.get("state")});
        }
        int dv = root.get("DataVersion") instanceof Integer v ? v : 0;
        return new Template((Integer) size.get(0), (Integer) size.get(1), (Integer) size.get(2), palette, blocks, dv, nbt);
    }

    private static Map<String, Object> compound(DataInputStream in) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        while (true) {
            byte t = in.readByte();
            if (t == 0) return m;
            String name = in.readUTF();
            m.put(name, payload(in, t));
        }
    }

    private static Object payload(DataInputStream in, byte t) throws IOException {
        return switch (t) {
            case 1 -> (int) in.readByte();
            case 2 -> (int) in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> { byte[] b = new byte[in.readInt()]; in.readFully(b); yield b; }
            case 8 -> in.readUTF();
            case 9 -> {
                byte et = in.readByte();
                int len = in.readInt();
                List<Object> l = new ArrayList<>();
                for (int i = 0; i < len; i++) l.add(payload(in, et));
                yield l;
            }
            case 10 -> compound(in);
            case 11 -> { int[] a = new int[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readInt(); yield a; }
            case 12 -> { long[] a = new long[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readLong(); yield a; }
            default -> throw new IOException("bad tag " + t);
        };
    }

    /** Builds templates block by block; later blocks replace earlier ones at the same position. */
    public static final class Builder {
        private final int sx, sy, sz;
        private final int[] cells;
        private final Map<Integer, Map<String, String>> tags = new LinkedHashMap<>();
        private final List<State> palette = new ArrayList<>();
        private final Map<String, Integer> index = new LinkedHashMap<>();

        public Builder(int sx, int sy, int sz) {
            this.sx = sx; this.sy = sy; this.sz = sz;
            cells = new int[sx * sy * sz];
            java.util.Arrays.fill(cells, -1);
        }

        private int state(String spec) {
            Integer i = index.get(spec);
            if (i != null) return i;
            String name = spec;
            Map<String, String> props = new LinkedHashMap<>();
            int b = spec.indexOf('[');
            if (b >= 0) {
                name = spec.substring(0, b);
                for (String kv : spec.substring(b + 1, spec.length() - 1).split(",")) {
                    String[] p = kv.split("=");
                    props.put(p[0].trim(), p[1].trim());
                }
            }
            if (!name.contains(":")) name = "minecraft:" + name;
            palette.add(new State(name, props));
            index.put(spec, palette.size() - 1);
            return palette.size() - 1;
        }

        public Builder set(int x, int y, int z, String spec) {
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) return this;
            cells[(y * sz + z) * sx + x] = spec == null ? -1 : state(spec);
            return this;
        }

        /** A string tag on the block entity at a position (a container's {@code LootTable}). */
        public Builder tag(int x, int y, int z, String key, String value) {
            tags.computeIfAbsent((y * sz + z) * sx + x, k -> new LinkedHashMap<>()).put(key, value);
            return this;
        }

        public Builder box(int x0, int y0, int z0, int x1, int y1, int z1, String spec) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) set(x, y, z, spec);
            return this;
        }

        /** Hollow walls of a box (no floor or roof). */
        public Builder walls(int x0, int y0, int z0, int x1, int y1, int z1, String spec) {
            for (int y = y0; y <= y1; y++) {
                for (int x = x0; x <= x1; x++) { set(x, y, z0, spec); set(x, y, z1, spec); }
                for (int z = z0; z <= z1; z++) { set(x0, y, z, spec); set(x1, y, z, spec); }
            }
            return this;
        }

        public Template build() {
            List<int[]> blocks = new ArrayList<>();
            Map<Integer, Map<String, String>> nbt = new LinkedHashMap<>();
            for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) {
                int s = cells[(y * sz + z) * sx + x];
                if (s < 0) continue;
                Map<String, String> t = tags.get((y * sz + z) * sx + x);
                if (t != null) nbt.put(blocks.size(), t);
                blocks.add(new int[] {x, y, z, s});
            }
            return new Template(sx, sy, sz, List.copyOf(palette), blocks, DATA_VERSION, nbt);
        }
    }

}
