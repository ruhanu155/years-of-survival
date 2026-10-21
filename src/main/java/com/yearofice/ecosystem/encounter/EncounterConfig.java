package com.yearofice.ecosystem.encounter;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.ecosystem.TalkWalk;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Tuning of door entry ({@code [ecosystem] door_entry_scale}) and talk as you walk ({@code [danger] talk_*}). */
public final class EncounterConfig {
    private EncounterConfig() {}

    public record DoorTuning(double doorEntryScale) {}

    public static final PresetConfig<DoorTuning> DOOR = new PresetConfig<>(DoorTuning.class, p -> switch (p) {
        case GENTLE -> new DoorTuning(0.5);
        case STANDARD -> new DoorTuning(1.0);
        case BRUTAL -> new DoorTuning(1.5);
    }, Map.of("doorEntryScale", "Multiplies the hourly door-entry chances (fox 5%, wolf 2%, bear 3%)."));

    private static ModConfigSpec.DoubleValue openM, coverM, avoid, avoidSow;

    public static void define(ModConfigSpec.Builder b) {
        WorldToggles.define(b);
        com.yearofice.food.grease.GreaseConfig.define(b);
        b.comment("Door entry (Structures and buildables > Door left open).").push("ecosystem");
        DOOR.define(b);
        b.pop();
        b.comment("Danger: talk as you walk (Water, calls and encounters > B7).").push("danger");
        openM = b.defineInRange("talk_radius_open_m", 80.0, 1.0, 400.0);
        coverM = b.defineInRange("talk_radius_cover_m", 50.0, 1.0, 400.0);
        avoid = b.defineInRange("talk_avoid_chance", 0.85, 0.0, 1.0);
        avoidSow = b.defineInRange("talk_avoid_chance_sow", 0.70, 0.0, 1.0);
        b.pop();
    }

    public static double doorScale() { return DOOR.tuning().doorEntryScale(); }

    public static TalkWalk.Tuning talk() {
        if (!YoiConfig.SPEC.isLoaded() || openM == null) return TalkWalk.Tuning.standard();
        return new TalkWalk.Tuning(openM.get(), coverM.get(), avoid.get(), avoidSow.get());
    }

    public static void invalidate() { DOOR.invalidate(); }
}
