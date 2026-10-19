package com.yearofice.sim.env;

/**
 * The warm-room heat model (Crafting and building in depth › Warm rooms), as pure functions.
 *
 * <pre>
 *   C dT/dt = H − L_air (T − T_out) − L_floor (T − T_ground)
 *   L_air   = Σ U·A + 0.4 m_open + 0.1 m_moss + 40 d_open + 1.5
 * </pre>
 *
 * Units: W, W/°C, Wh/°C, game hours. Room finding (the flood fill, L and C from the {@code heat} data map)
 * stays in {@code build.rooms.RoomFinder}; {@code build.rooms.HeatModel} calls these to step and settle.
 * Reimplemented from the Blueprint; Cold Sweat's hearth was read only to confirm that a room should warm
 * and cool over time rather than snap, which this ODE does by itself.
 */
public final class RoomHeat {
    private RoomHeat() {}

    /** One room's loss and capacity. */
    public record Room(double lAirWPerC, double lFloorWPerC, double capacityWhPerC) {
        public Room {
            if (capacityWhPerC <= 0) throw new IllegalArgumentException("capacity");
        }

        public double lossWPerC() { return lAirWPerC + lFloorWPerC; }

        /** Time constant in game hours. */
        public double tauHours() { return capacityWhPerC / lossWPerC(); }
    }

    /** L_air from its parts. */
    public static double lAir(double sumUA, double openSeamM, double mossSeamM, int openDoors) {
        return sumUA + 0.4 * openSeamM + 0.1 * mossSeamM + 40.0 * openDoors + 1.5;
    }

    /** Ground under a cabin: the 5-day mean air, held between −5 and 8 °C. */
    public static double groundC(double fiveDayMeanAirC) {
        return Math.max(-5, Math.min(8, fiveDayMeanAirC));
    }

    /** The temperature the room settles to with constant inputs. */
    public static double steadyC(Room r, double outC, double groundC, double heatW) {
        return (heatW + r.lAirWPerC() * outC + r.lFloorWPerC() * groundC) / r.lossWPerC();
    }

    /** One game minute, explicit Euler. Stable for any room with τ above a minute. */
    public static double stepMinute(Room r, double roomC, double outC, double groundC, double heatW) {
        double netW = heatW - r.lAirWPerC() * (roomC - outC) - r.lFloorWPerC() * (roomC - groundC);
        return roomC + netW / r.capacityWhPerC() / 60.0;
    }

    /** Closed form for {@code hours} at constant inputs: the off-screen settle on chunk load. */
    public static double settle(Room r, double roomC, double outC, double groundC, double heatW, double hours) {
        double eq = steadyC(r, outC, groundC, heatW);
        return eq + (roomC - eq) * Math.exp(-hours / r.tauHours());
    }

    /** Heaters: share of an armload kept in the room, and in-room power and burn time per setting (×1.0 wood). */
    public enum Heater {
        FIREPLACE(0.40, 640, 7.5, 1600, 3, 2400, 1),
        BARREL_STOVE(0.85, 1000, 10, 2500, 4, 3750, 1.3),
        BOX_STOVE(0.90, 840, 12.5, 2100, 5, 3200, 1.7);

        public final double keptShare;
        private final double[] powerW;
        private final double[] hours;

        Heater(double kept, double lowW, double lowH, double steadyW, double steadyH, double hotW, double hotH) {
            this.keptShare = kept;
            this.powerW = new double[] {lowW, steadyW, hotW};
            this.hours = new double[] {lowH, steadyH, hotH};
        }

        public double powerW(FireSetting s) { return powerW[s.ordinal()]; }

        /** Hours one armload lasts; {@code woodFactor} is the Wood table's multiplier (birch 1.4). */
        public double burnHours(FireSetting s, double woodFactor) { return hours[s.ordinal()] * woodFactor; }
    }

    /** Small heat sources in a room, in W. */
    public static final double PLAYER_W = 100, PLAYER_ASLEEP_W = 70, WOLF_W = 60, FAT_LAMP_W = 30,
            CANDLE_W = 20, LANTERN_W = 40, BANKED_COALS_W = 300, BANKED_COALS_H = 6;

    /** An armload of wood in kWh. */
    public static final double ARMLOAD_KWH = 11.8;

    /** A room counts as warm for the soaked rule at or above this temperature (an interpretation; the Blueprint says only "a warm room"). */
    public static final double WARM_ROOM_C = 10;
}
