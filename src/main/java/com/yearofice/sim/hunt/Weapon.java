package com.yearofice.sim.hunt;

import com.google.gson.JsonObject;

/** One weapon's numbers (data {@code weapon/<id>.json}, Blueprint › Weapons and arrowheads). */
public record Weapon(String id, String tier, double strengthReq, double arrowSpeedMps, double arrowMassG, double drawTimeS,
                     double maxHoldS, double effectiveRangeM, double conditionPerShot, double thrustReachM) {

    public static Weapon parse(JsonObject o) {
        return new Weapon(o.get("id").getAsString(), o.get("tier").getAsString(), o.get("strength_req").getAsDouble(),
                o.get("arrow_speed_mps").getAsDouble(), o.get("arrow_mass_g").getAsDouble(), o.get("draw_time_s").getAsDouble(),
                o.get("max_hold_s").getAsDouble(), o.get("effective_range_m").getAsDouble(), o.get("condition_per_shot").getAsDouble(),
                o.has("thrust_reach_m") ? o.get("thrust_reach_m").getAsDouble() : 0);
    }

    /** Full-draw arrow energy: ½ m v². Longbow 37.5 J, light bow 22 J, sinew-backed bow about 50 J. */
    public double fullEnergyJ() { return 0.5 * arrowMassG / 1000.0 * arrowSpeedMps * arrowSpeedMps; }

    /** Draw fraction reachable: 1 at or above the requirement, capped at {@code cap} (60%) below it. */
    public double maxDraw(double strength, double cap) { return strength >= strengthReq ? 1.0 : cap; }

    /** Speed at a draw fraction: linear in draw, so energy goes with its square. */
    public double speedAt(double draw) { return arrowSpeedMps * clamp(draw); }

    public double energyAt(double draw) { double v = speedAt(draw); return 0.5 * arrowMassG / 1000.0 * v * v; }

    /** Seconds to draw: the ready time, x1.5 below 3 bolts. */
    public double drawSeconds(double energyBolts, HuntTuning t) { return drawTimeS * (energyBolts < 3 ? t.lowEnergyDrawX() : 1.0); }

    /** Sway radius in cm at {@code distM}: the 20 m figure scaled linearly, tripled once held past the shake. */
    public double swayCm(double distM, double modifiers, double practice, double heldS, HuntTuning t) {
        double r = t.swayCmAt20m() * (1 + modifiers) * (1 - 0.4 * practice) * distM / 20.0;
        return heldS > maxHoldS ? r * 3 : r;
    }

    private static double clamp(double d) { return Math.max(0, Math.min(1, d)); }
}
