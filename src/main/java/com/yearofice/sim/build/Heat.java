package com.yearofice.sim.build;

import java.util.List;

/**
 * Steady-state heat balance of an enclosed room (Crafting and building in depth › Warm rooms), pure Java.
 *
 * <pre>  G = Σ U_eff·A (walls, roof, floor) + 40·A_open + 0.4·seam_open + 0.1·seam_moss + 1.5      [W/°C]
 *   T_in = (P_mean + G_air·T_out + G_floor·T_ground) / G                       [°C]</pre>
 *
 * The roof's snow load is insulation in series (R = depth / 0.15 W/m·K); wind raises the exposed faces' film
 * coefficient by 6 % per m/s. Floor faces lose to the ground, not the air. Units are W, m, °C.
 */
public final class Heat {
    private Heat() {}

    public enum Part { WALL, ROOF, FLOOR }

    /** One enclosing face group: its area, the material's U (W/m²·°C) and the snow lying on it (roofs), metres. */
    public record Face(Part part, double areaM2, double u, double snowM) {}

    /** Result of a balance: total conductance, the loss at the inside temperature and the settled temperature. */
    public record Balance(double gAirWPerC, double gFloorWPerC, double insideC, double feelsLikeC, double lossW) {
        public double gWPerC() { return gAirWPerC + gFloorWPerC; }
    }

    public static final double OPEN_U = 40, SNOW_K = 0.15, SEAM_W_PER_M = 0.4, MOSS_W_PER_M = 0.1, BASE_LEAK = 1.5, WIND_PER_MS = 0.06;

    /** U of a face in series with snow and with the wind's film. */
    public static double effectiveU(Face f, double windMs) {
        double u = f.u();
        if (f.snowM() > 0) u = 1.0 / (1.0 / u + f.snowM() / SNOW_K);
        return f.part() == Part.FLOOR ? u : u * (1 + WIND_PER_MS * Math.max(0, windMs));
    }

    /**
     * @param openM2  area of openings to the outside (a missing wall piece, an open door, a vent)
     * @param seamM   metres of open (unchinked) seam: 0.4 W/°C each
     * @param sourceW mean heat into the room, W (a stove's output times its duty cycle)
     */
    public static Balance solve(List<Face> faces, double openM2, double seamM, double sourceW, double outC, double groundC, double windMs) {
        return solve(faces, openM2, seamM, 0, sourceW, outC, groundC, windMs);
    }

    /** As above with {@code mossM} metres of seam chinked with moss only (0.1 W/°C each). */
    public static Balance solve(List<Face> faces, double openM2, double seamM, double mossM, double sourceW, double outC, double groundC, double windMs) {
        double gAir = BASE_LEAK + OPEN_U * openM2 + SEAM_W_PER_M * seamM + MOSS_W_PER_M * mossM, gFloor = 0;
        for (Face f : faces) {
            double g = effectiveU(f, windMs) * f.areaM2();
            if (f.part() == Part.FLOOR) gFloor += g; else gAir += g;
        }
        double g = gAir + gFloor;
        double t = (sourceW + gAir * outC + gFloor * groundC) / g;
        return new Balance(gAir, gFloor, t, t, g * t - gAir * outC - gFloor * groundC);
    }
}
