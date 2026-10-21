package com.yearofice.ecosystem.encounter;

import com.yearofice.core.rules.YoiConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [world]} options the player turns on or off (RECONCILE 170): {@code animals_enter_open_doors} (A2) and
 * {@code rack_thieves} (A3, moved here from {@code [ecosystem]}). On by default in every preset; a runtime override
 * ({@code /yoi world set}) stands in for the world's own copy until world creation options land.
 */
public final class WorldToggles {
    private WorldToggles() {}

    public static ModConfigSpec.BooleanValue ENTER_DOORS;
    public static ModConfigSpec.BooleanValue RACK_THIEVES;
    private static volatile Boolean doorsOverride, racksOverride;

    public static void define(ModConfigSpec.Builder b) {
        b.comment("World options (Minecraft development > World creation options and presets). On in every preset.").push("world");
        ENTER_DOORS = b.comment("Animals enter open doors and the door_open warning fires (Structures and buildables > Door left open).")
                .define("animals_enter_open_doors", true);
        RACK_THIEVES = b.comment("Animals raid the camp's stores and racks (Structures and buildables > Thieves at the rack).")
                .define("rack_thieves", true);
        b.pop();
    }

    private static boolean loaded() { return YoiConfig.SPEC.isLoaded(); }

    public static boolean animalsEnterOpenDoors() {
        if (doorsOverride != null) return doorsOverride;
        return !loaded() || ENTER_DOORS == null || ENTER_DOORS.get();
    }

    public static boolean rackThieves() {
        if (racksOverride != null) return racksOverride;
        return !loaded() || RACK_THIEVES == null || RACK_THIEVES.get();
    }

    public static void setDoors(Boolean on) { doorsOverride = on; }

    public static void setRacks(Boolean on) { racksOverride = on; }
}
