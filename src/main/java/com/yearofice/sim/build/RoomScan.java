package com.yearofice.sim.build;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;

/**
 * Flood-fill enclosure detection over a lookup of cells, no Minecraft imports. A room is the set of air cells
 * reachable by face from the seed that is closed (finishes) within {@code maxCells}; every non-air neighbour
 * contributes a face with its U, and openings (an open door, a vent) are leaks that the air passes through
 * without joining the fill. {@code air} keeps lane D's SnowRoom meaning: cells of the enclosed volume.
 */
public final class RoomScan {
    private RoomScan() {}

    public enum Kind { AIR, SOLID, LEAK }

    /** A cell as seen by the scan: its kind and, for SOLID, the U of the piece. */
    public record Cell(Kind kind, double u, double seamM) {
        public Cell(Kind kind, double u) { this(kind, u, 0); }
        public static final Cell AIR = new Cell(Kind.AIR, 0);
        public static final Cell LEAK = new Cell(Kind.LEAK, 0);
        public static Cell solid(double u) { return new Cell(Kind.SOLID, u, 0); }
        public static Cell solid(double u, double seamM) { return new Cell(Kind.SOLID, u, seamM); }
    }

    /** Packs block coordinates into one long (26/12/26 bits, like BlockPos). */
    public static long pack(int x, int y, int z) { return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF); }
    public static int ux(long p) { return (int) (p >> 38) << 6 >> 6; }
    public static int uz(long p) { return (int) (p >> 12 & 0x3FFFFFF) << 6 >> 6; }
    public static int uy(long p) { return (int) (p & 0xFFF) << 20 >> 20; }

    /** @param air the enclosed cells; empty and {@code enclosed} false when the fill ran past the cap */
    public record Result(boolean enclosed, List<Long> air, List<Heat.Face> faces, List<Long> leaks, int volume, double seamM) {
        public Result(boolean enclosed, List<Long> air, List<Heat.Face> faces, List<Long> leaks, int volume) { this(enclosed, air, faces, leaks, volume, 0); }
    }

    private static final int[][] D = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};

    public static Result scan(LongFunction<Cell> world, int sx, int sy, int sz, int maxCells) {
        if (world.apply(pack(sx, sy, sz)).kind() != Kind.AIR) return new Result(false, List.of(), List.of(), List.of(), 0);
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<Long> q = new java.util.ArrayDeque<>();
        List<Long> air = new ArrayList<>(), leaks = new ArrayList<>();
        double[] area = new double[3], ua = new double[3];
        double seam = 0;
        long s = pack(sx, sy, sz);
        seen.add(s);
        q.add(s);
        while (!q.isEmpty()) {
            long c = q.poll();
            if (air.size() >= maxCells) return new Result(false, List.of(), List.of(), List.of(), 0);
            air.add(c);
            int x = ux(c), y = uy(c), z = uz(c);
            for (int[] d : D) {
                long n = pack(x + d[0], y + d[1], z + d[2]);
                Cell cell = world.apply(n);
                switch (cell.kind()) {
                    case AIR -> { if (seen.add(n)) q.add(n); }
                    case LEAK -> leaks.add(n);
                    case SOLID -> {
                        int part = d[1] > 0 ? Heat.Part.ROOF.ordinal() : d[1] < 0 ? Heat.Part.FLOOR.ordinal() : Heat.Part.WALL.ordinal();
                        area[part] += 1;
                        ua[part] += cell.u();
                        if (part == Heat.Part.WALL.ordinal()) seam += cell.seamM();
                    }
                }
            }
        }
        List<Heat.Face> faces = new ArrayList<>();
        for (Heat.Part p : Heat.Part.values()) {
            int i = p.ordinal();
            if (area[i] > 0) faces.add(new Heat.Face(p, area[i], ua[i] / area[i], 0));
        }
        return new Result(true, air, faces, leaks, air.size(), seam);
    }
}
