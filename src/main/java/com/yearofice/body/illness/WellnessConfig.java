package com.yearofice.body.illness;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.WellnessTuning;
import java.util.EnumMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [body_wellness]} of {@code yearofice-common.toml}: the lane B dials (illness length, Rest-day healing,
 * sunburn and windburn hours, grease, beard) with a sub-table per preset ({@code gentle}, {@code standard},
 * {@code brutal}); {@code general.preset} picks the column. {@link com.yearofice.sim.body.Wellness#tuningSource}
 * reads it once the file has loaded.
 */
public final class WellnessConfig {
    private WellnessConfig() {}

    private static final String[] KEYS = {"illness_scale", "rest_heal", "sunburn_hours", "windburn_hours",
            "face_grease_hours", "glove_grease_days", "glove_grease_wet_mult", "beard_stage_days",
            "beard_face_bonus_c", "beard_ice_temp_c", "grease_face_bonus_c"};
    private static final String[] COMMENTS = {
            "x on the duration of timed illnesses the body steps.",
            "x on Condition healing on a Rest day (rest_heals_double).",
            "Game hours of spring sun on snow before a bare face is sunburned (B13).",
            "Game hours of hard wind below -10 C before a bare face is windburned (B13).",
            "Game hours face grease protects (B13).",
            "Game days glove grease lasts (B13).",
            "x on a greased glove's wetness gain (B13).",
            "Game days a beard takes to grow one stage (B14).",
            "Degrees colder the face is safe with a Full-or-longer beard (B14).",
            "Air temperature below which breath ices a Full beard (B14).",
            "Degrees colder the face is safe with grease on (B13)."};

    private static final Map<YoiConfig.Preset, ModConfigSpec.DoubleValue[]> VALUES = new EnumMap<>(YoiConfig.Preset.class);

    private static double[] values(WellnessTuning t) {
        return new double[] {t.illnessScale(), t.restHeal(), t.sunburnHours(), t.windburnHours(), t.faceGreaseHours(),
                t.gloveGreaseDays(), t.gloveWetMult(), t.beardStageDays(), t.beardFaceBonusC(), t.beardIceC(),
                t.greaseFaceBonusC()};
    }

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Body, lane B: illness, Rest days, skin and hair. One sub-table per preset.").push("body_wellness");
        for (YoiConfig.Preset p : YoiConfig.Preset.values()) {
            double[] v = values(WellnessTuning.of(p.name()));
            b.push(p.name().toLowerCase(java.util.Locale.ROOT));
            ModConfigSpec.DoubleValue[] vals = new ModConfigSpec.DoubleValue[KEYS.length];
            for (int i = 0; i < KEYS.length; i++) {
                vals[i] = b.comment(COMMENTS[i]).defineInRange(KEYS[i], v[i], -100.0, 10_000.0);
            }
            VALUES.put(p, vals);
            b.pop();
        }
        b.pop();
    }

    /** The dials in force: the preset's defaults until the file has loaded. */
    public static WellnessTuning current(BodyTuning t) {
        if (VALUES.isEmpty() || !YoiConfig.SPEC.isLoaded()) return WellnessTuning.of(t.preset());
        ModConfigSpec.DoubleValue[] v = VALUES.get(YoiConfig.PRESET.get());
        return new WellnessTuning(v[0].get(), v[1].get(), v[2].get(), v[3].get(), v[4].get(), v[5].get(), v[6].get(),
                v[7].get(), v[8].get(), v[9].get(), v[10].get());
    }
}
