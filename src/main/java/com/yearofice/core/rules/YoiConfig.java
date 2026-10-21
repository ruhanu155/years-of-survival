package com.yearofice.core.rules;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * yearofice-common.toml. Every tuning number lives here, grouped by system, with a preset
 * (Gentle / Standard / Brutal) that sets the defaults. GameTests always run on Standard.
 */
public final class YoiConfig {
    public enum Preset { GENTLE, STANDARD, BRUTAL }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Preset> PRESET;
    public static final ModConfigSpec.IntValue DAY_LENGTH_MIN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("general");
        PRESET = b.comment("Realism preset. Sets every slider's default; sliders can still be moved.")
                .defineEnum("preset", Preset.STANDARD);
        DAY_LENGTH_MIN = b.comment("Default real minutes per game day for new worlds (24-72; steps 24,30,36,40,45,48,60,72).")
                .defineInRange("day_length_min", 24, 24, 72);
        b.pop();
        com.yearofice.story.clock.ClockConfig.define(b);
        com.yearofice.item.inventory.PackConfig.define(b);
        com.yearofice.body.BodyConfig.define(b);
        com.yearofice.body.illness.WellnessConfig.define(b);
        com.yearofice.body.sleep.SleepConfig.define(b);
        com.yearofice.body.injury.InjuryConfig.define(b); // lane A (M4): [injury]
        com.yearofice.body.SosConfig.define(b);
        com.yearofice.fire.FireConfig.define(b);
        com.yearofice.hunt.fishing.FishingConfig.define(b);
        com.yearofice.ecosystem.EcoConfig.define(b);
        com.yearofice.ecosystem.bird.BirdConfig.define(b);
        com.yearofice.decay.DecayConfig.define(b);
        com.yearofice.food.FoodConfig.define(b);
        com.yearofice.story.StoryConfig.define(b);
        com.yearofice.build.wood.WoodConfig.define(b);
        com.yearofice.build.outpost.OutpostConfig.define(b);
        com.yearofice.build.BuildConfig.define(b);
        com.yearofice.plant.ForageConfig.define(b);
        com.yearofice.crop.GardenConfig.define(b);
        com.yearofice.hunt.HuntConfig.define(b);
        com.yearofice.env.weather.WeatherConfig.define(b); // lane M7 A: [weather]
        com.yearofice.ecosystem.small.LifeConfig.define(b);
        com.yearofice.world.season.SeasonConfig.define(b); // M7 lane B: [season] [ice] [snow]
        com.yearofice.hunt.HuntConfig.define(b);
        com.yearofice.env.weather.WeatherConfig.define(b); // lane M7 A: [weather]
        com.yearofice.build.craft.CraftConfig.define(b);
        SPEC = b.build();
    }

    private YoiConfig() {}
}
