package com.yearofice.client.craft;

import com.yearofice.build.craft.StationNet;
import com.yearofice.core.YearOfIce;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** The client half of crafting: installs the {@code station_list} sink, which opens the station screen. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class CraftClient {
    private CraftClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        StationNet.sink = p -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.setScreen(new StationScreen(p));
        };
    }
}
