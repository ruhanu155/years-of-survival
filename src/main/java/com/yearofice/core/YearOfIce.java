package com.yearofice.core;

import com.mojang.logging.LogUtils;
import com.yearofice.core.debug.YoiCommands;
import com.yearofice.core.rules.YoiConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/** Mod entry. Systems register here and reach each other only through com.yearofice.api. */
@Mod(YearOfIce.MOD_ID)
public final class YearOfIce {
    public static final String MOD_ID = "yearofice";
    public static final Logger LOG = LogUtils.getLogger();

    public YearOfIce(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, YoiConfig.SPEC, "yearofice-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, com.yearofice.hud.fieldnote.FieldNoteOptions.SPEC, "yearofice-client.toml");
        NeoForge.EVENT_BUS.addListener(YoiCommands::register);
        // The shared layers first: every system below reads time and weather through them.
        // Id aliases, the world's version gate and backup, legacy conversions (Saves across versions).
        com.yearofice.data.migrate.MigrateSystem.init(modBus);
        com.yearofice.story.clock.ClockSystem.init();
        com.yearofice.env.EnvSystem.init();
        com.yearofice.env.weather.WeatherSystem.init(modBus);
        com.yearofice.hud.HudSystem.init();
        com.yearofice.hud.fieldnote.FieldNotes.init(modBus);
        com.yearofice.item.component.YoiComponents.register(modBus);
        com.yearofice.decay.DecaySystem.init(modBus);
        com.yearofice.item.inventory.PackSystem.init(modBus);
        com.yearofice.body.BodySystem.init(modBus);
        com.yearofice.fire.FireSystem.init(modBus);
        com.yearofice.worldgen.gen.WorldgenRegistry.init(modBus);
        com.yearofice.hunt.fishing.FishingSystem.init(modBus);
        com.yearofice.hunt.HuntSystem.init(modBus);
        com.yearofice.ecosystem.EcosystemSystem.init(modBus);
        com.yearofice.food.FoodSystem.init(modBus);
        com.yearofice.story.StorySystem.init(modBus);
        com.yearofice.build.wood.WoodSystem.init(modBus);
        com.yearofice.build.BuildSystem.init(modBus);
        com.yearofice.plant.PlantSystem.init(modBus);
        com.yearofice.crop.CropSystem.init(modBus);
        com.yearofice.world.season.SeasonSystem.init(modBus);
        com.yearofice.ecosystem.bird.BirdSystem.init(modBus);
        com.yearofice.travel.TravelSystem.init(modBus);
        LOG.info("Year of Ice loading (preset {})", "from config");
    }
}
