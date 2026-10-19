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
        NeoForge.EVENT_BUS.addListener(YoiCommands::register);
        com.yearofice.body.BodySystem.init(modBus);
        com.yearofice.fire.FireSystem.init(modBus);
        com.yearofice.hunt.fishing.FishingSystem.init(modBus);
        LOG.info("Year of Ice loading (preset {})", "from config");
    }
}
