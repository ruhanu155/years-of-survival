package com.yearofice.sim.fire;

import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.env.HeatSource;

/**
 * One fire as a heat source, in plain data: where it is (block coordinates), its state and setting, the warmth it
 * gives within 3 m in °C and its output in kW. The warmth step reads these through {@link #toHeatSource}, which
 * gives {@link com.yearofice.sim.env.HeatIndex} the open-fire shape it already knows: the full bonus within 3 m,
 * fading in a straight line to 0 at 8 m (Blueprint › Fire, water and light; Minecraft engine › Fire › Heat).
 */
public record FireHeat(int x, int y, int z, FireState state, FireSetting setting, double bonusC, double kW) {

    public static final double FULL_RADIUS_M = 3;
    public static final double ZERO_RADIUS_M = 8;

    public static FireHeat of(int x, int y, int z, Fire fire, FireTuning t) {
        return new FireHeat(x, y, z, fire.state(), fire.setting(), fire.heatC(), fire.kW(t));
    }

    /** True when the fire gives any heat at all. */
    public boolean warm() { return bonusC > 0; }

    /** The heat source as seen from a point {@code distanceM} away, behind {@code occluders} solid blocks. */
    public HeatSource toHeatSource(double distanceM, int occluders) {
        return new HeatSource(HeatSource.Kind.OPEN_FIRE, bonusC, FULL_RADIUS_M, ZERO_RADIUS_M, distanceM, occluders);
    }

    /** Distance in metres from the fire's centre (block centre, 0.2 m up) to a point. */
    public double distanceTo(double px, double py, double pz) {
        double dx = px - (x + 0.5), dy = py - (y + 0.2), dz = pz - (z + 0.5);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
