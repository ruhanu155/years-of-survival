package com.yearofice.sim.fire;

import com.yearofice.sim.env.FireSetting;

/**
 * Stove rules (Blueprint › Fire, water and light › Smoke indoors; Crafting and building in depth › Warm rooms). No
 * Minecraft imports. A stove's kind numbers come from {@code data/yearofice/multiblock/<id>.json}, never from code.
 */
public final class Stove {
    private Stove() {}

    /** One stove kind: minutes an x1.0 armload lasts at Steady with full draft, share of the fire's energy that heats the room, and the pipe range. */
    public record Kind(String id, double armloadMin, double efficiency, int maxArmloads, double minChimneyM, double idealChimneyM) {}

    /** Creosote 0-100. The messenger warns at 60; a Hot hour above 70 risks a chimney fire. */
    public static final double CREOSOTE_WARN = 60, CREOSOTE_FIRE = 70, CREOSOTE_MAX = 100;
    /** Chance per game hour of a chimney fire on Hot above {@link #CREOSOTE_FIRE}. */
    public static final double CHIMNEY_FIRE_PER_H = 0.03;
    /** Creosote per game hour burning on Low. */
    public static final double LOW_HOUR_CREOSOTE = 3;
    /** Share of a Hot fire's heat that stays in the room (the rest goes up the chimney). */
    public static final double HOT_ROOM_SHARE = 0.5;
    /** Draft of a stove with no chimney at all: it burns, but the smoke is in the room. */
    public static final double UNFLUED_DRAFT = 0.3;
    /** Smoke the room gains per game minute from an unflued fire, percent (Smoke indoors: 10% a game minute). */
    public static final double SMOKE_PCT_PER_MIN = 10;

    /**
     * Draft factor from the pipe's height above the firebox. No pipe: {@link #UNFLUED_DRAFT}. Under the minimum the
     * stove sulks (0.5 at the minimum, linear from the unflued value). At the ideal height it is 1.0; each metre past
     * adds 0.1, to 1.3.
     */
    public static double draft(Kind k, double chimneyM) {
        if (chimneyM <= 0) return UNFLUED_DRAFT;
        if (chimneyM < k.minChimneyM()) return UNFLUED_DRAFT + (0.5 - UNFLUED_DRAFT) * chimneyM / k.minChimneyM();
        if (chimneyM < k.idealChimneyM()) return 0.5 + 0.5 * (chimneyM - k.minChimneyM()) / (k.idealChimneyM() - k.minChimneyM());
        return Math.min(1.3, 1.0 + 0.1 * (chimneyM - k.idealChimneyM()));
    }

    /** True when smoke leaves by a pipe: any pipe at all, open at the top. */
    public static boolean flued(double chimneyM, boolean topOpen) { return chimneyM > 0 && topOpen; }

    /** Game minutes one armload of this wood lasts at this setting and draft: a stronger draft burns it faster. */
    public static double minutesPerArmload(Kind k, FireSetting s, double draft, Wood w) {
        return k.armloadMin() * w.burnFactor / (FireRules.fuelUse(s) * Math.max(0.05, draft));
    }

    /** Heat to the room in watts while burning this wood at this moisture. */
    public static double outputW(Kind k, FireSetting s, double draft, Wood w, int moisturePct) {
        double perH = FireRules.KWH_PER_ARMLOAD * 1000.0 / (minutesPerArmload(k, s, draft, w) / 60.0);
        // Warm rooms › Heat sources: Hot burns three times as fast and loses half of it up the chimney
        double hotLoss = s == FireSetting.HOT ? HOT_ROOM_SHARE : 1.0;
        return perH * k.efficiency() * w.heatFactor * FireRules.moistureHeat(moisturePct) * hotLoss;
    }

    /**
     * Creosote one whole armload adds (Smoke indoors): seasoned birch 1, seasoned spruce (and the rest) 2, green wood
     * 5; between seasoned and green the figure climbs in a straight line, so wet wood is always worse.
     */
    public static double creosotePerArmload(Wood w, int moisturePct) {
        double seasoned = w == Wood.BIRCH ? 1 : 2;
        if (moisturePct < FireRules.SEASONED_MOISTURE) return seasoned;
        if (moisturePct >= FireRules.GREEN_MOISTURE) return 5;
        double f = (moisturePct - FireRules.SEASONED_MOISTURE) / (double) (FireRules.GREEN_MOISTURE - FireRules.SEASONED_MOISTURE);
        return seasoned + (5 - seasoned) * f;
    }

    /** Chance of a chimney fire in one game hour at this creosote and setting. */
    public static double chimneyFireChance(double creosote, FireSetting s) {
        return creosote > CREOSOTE_FIRE && s == FireSetting.HOT ? CHIMNEY_FIRE_PER_H : 0;
    }
}
