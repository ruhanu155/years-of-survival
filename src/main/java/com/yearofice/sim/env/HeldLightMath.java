package com.yearofice.sim.env;

import java.util.List;

/**
 * Held light as a number (Minecraft development › Gameplay light; Held light): a source of {@code level} lights the
 * block it is in at that level and each block farther one less, down to 0, the way vanilla block light falls. A source
 * with a cone lights only inside it. Gameplay light at a place is the largest of sky light, vanilla block light and
 * these. Pure Java, so the server's {@code env.light} and the client's {@code HeldLight} share the arithmetic.
 */
public final class HeldLightMath {
    private HeldLightMath() {}

    /** At most this many sources count near a place, nearest first (Engine: "up to 8 sources within 16 blocks"). */
    public static final int MAX_SOURCES = 8;
    public static final double RANGE = 16;

    /**
     * A light being carried. {@code dirX/Y/Z} is the unit look vector and {@code coneDeg} the full cone angle; 360
     * (or more) means a lantern's all-round light and the direction is ignored.
     */
    public record Source(double x, double y, double z, int level, int coneDeg, double dirX, double dirY, double dirZ) {
        public static Source around(double x, double y, double z, int level) { return new Source(x, y, z, level, 360, 0, 0, 1); }
    }

    /** The light {@code s} gives at a point (a block centre): level minus the distance in blocks, 0 outside the cone. */
    public static int at(Source s, double px, double py, double pz) {
        double dx = px - s.x(), dy = py - s.y(), dz = pz - s.z();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d > RANGE) return 0;
        if (s.coneDeg() < 360 && d > 1.0) {
            double cos = (dx * s.dirX() + dy * s.dirY() + dz * s.dirZ()) / d;
            if (cos < Math.cos(Math.toRadians(s.coneDeg() / 2.0))) return 0;
        }
        return Math.max(0, s.level() - (int) Math.ceil(d - 1e-9));
    }

    /** The largest light any of {@code sources} gives at the point; only the first {@link #MAX_SOURCES} count. */
    public static int at(List<Source> sources, double px, double py, double pz) {
        int best = 0;
        int n = Math.min(MAX_SOURCES, sources.size());
        for (int i = 0; i < n; i++) best = Math.max(best, at(sources.get(i), px, py, pz));
        return best;
    }
}
