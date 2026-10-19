package com.yearofice.sim.fishing;

/**
 * The slice's fishing-spot rule until stage 4's spots reach the region layers (Ready for development › M2.5: "one
 * fishing spot"; Minecraft engine › Fishing, Bites): a spot is water at least {@code width} × {@code width}
 * columns across and at least {@code minDepthM} deep in every one of those columns.
 *
 * <p>Depth is measured from the water's surface, 1 block = 1 m: a column of n water blocks whose top block's
 * fluid stands {@code topHeight} high is (n − 1) + topHeight deep (a still source tops out at 8/9 of a block, so
 * two blocks are 1.89 m and one is 0.89 m).
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class SpotRule {
    private SpotRule() {}

    /** Water depth (m) of the column at (x, z) under a surface at the bobber's level; 0 when it is not water. */
    @FunctionalInterface
    public interface Columns {
        double depthM(int x, int z);
    }

    public static double columnDepthM(int waterBlocks, double topHeight) {
        return waterBlocks <= 0 ? 0.0 : (waterBlocks - 1) + topHeight;
    }

    /** True when some width × width square of columns containing (x, z) is all at least minDepthM deep. */
    public static boolean isSpot(Columns c, int x, int z, int width, double minDepthM) {
        if (width <= 1) return c.depthM(x, z) >= minDepthM;
        for (int ox = 0; ox < width; ox++) {
            for (int oz = 0; oz < width; oz++) {
                if (square(c, x - ox, z - oz, width, minDepthM)) return true;
            }
        }
        return false;
    }

    private static boolean square(Columns c, int x0, int z0, int width, double minDepthM) {
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < width; dz++) {
                if (c.depthM(x0 + dx, z0 + dz) < minDepthM) return false;
            }
        }
        return true;
    }
}
