package com.yearofice.sim.build;

import java.util.List;

/**
 * Steady-state heat balance of an enclosed room (Crafting and building in depth › Warm rooms), pure Java.
 *
 * <pre>  G = Σ U_eff·A (walls, roof, floor) + 40·A_open + 0.4·seam_m + 1.5      [W/°C]
 *   T_in = (P + G_air·T_out + G_floor·T_ground) / G                       [°C]</pre>
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

    /**
     * Chimney draw (Warm rooms › Heat sources: Hot "loses half of it up the chimney"): a fire burning P kW delivered to the
     * room passes air through its firebox in proportion to P and the flue gas leaves hotter in proportion to P, so what goes up
     * the pipe grows as P squared. Counted as an air conductance of {@value #DRAW_W_PER_C_PER_KW2} W/°C per kW squared (about
     * 65 W/°C for an 8 kW tin stove on a 5 m pipe, 17 for a 4 kW one, 2 for a Low fire).
     */
    public static final double DRAW_W_PER_C_PER_KW2 = 1.0;

    /** The chimney-draw conductance (W/°C) of {@code firedW} watts of burning fires and stoves in the room. */
    public static double drawWPerC(double firedW) { return drawWPerC(firedW, DRAW_W_PER_C_PER_KW2); }

    public static double drawWPerC(double firedW, double coef) { double kw = Math.max(0, firedW) / 1000.0; return coef * kw * kw; }

    public static final double OPEN_U = 40, SNOW_K = 0.15, SEAM_W_PER_M = 0.4, BASE_LEAK = 1.5, WIND_PER_MS = 0.06;

    /** U of a face in series with snow and with the wind's film. */
    public static double effectiveU(Face f, double windMs) {
        double u = f.u();
        if (f.snowM() > 0) u = 1.0 / (1.0 / u + f.snowM() / SNOW_K);
        return f.part() == Part.FLOOR ? u : u * (1 + WIND_PER_MS * Math.max(0, windMs));
    }

    /**
     * @param openM2  area of openings to the outside (a missing wall piece, an open door, a vent)
     * @param seamM   metres of open seam (gaps between pieces)
     * @param sourceW heat sources inside, W
     */
    public static Balance solve(List<Face> faces, double openM2, double seamM, double sourceW, double outC, double groundC, double windMs) {
        return solve(faces, openM2, seamM, sourceW, 0, outC, groundC, windMs);
    }

    /** As above, with {@code firedW} of the sources burning fuel: their {@link #drawWPerC} is a further air loss. */
    public static Balance solve(List<Face> faces, double openM2, double seamM, double sourceW, double firedW, double outC, double groundC, double windMs) {
        return solve(faces, openM2, seamM, sourceW, firedW, DRAW_W_PER_C_PER_KW2, outC, groundC, windMs);
    }

    /** As above with the chimney-draw coefficient (the {@code [pieces]} dial) given. */
    public static Balance solve(List<Face> faces, double openM2, double seamM, double sourceW, double firedW, double drawCoef, double outC, double groundC, double windMs) {
        double gAir = BASE_LEAK + OPEN_U * openM2 + SEAM_W_PER_M * seamM + drawWPerC(firedW, drawCoef), gFloor = 0;
        for (Face f : faces) {
            double g = effectiveU(f, windMs) * f.areaM2();
            if (f.part() == Part.FLOOR) gFloor += g; else gAir += g;
        }
        double g = gAir + gFloor;
        double t = (sourceW + gAir * outC + gFloor * groundC) / g;
        return new Balance(gAir, gFloor, t, t, g * t - gAir * outC - gFloor * groundC);
    }
}
