package com.yearofice.client.hud;

import com.yearofice.core.YearOfIce;
import com.yearofice.net.YoiNet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Installs the HUD bus's client sink and clears the lines on logout. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class HudClient {
    private HudClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        YoiNet.hudSink = ClientHud::accept;
        if ("hud".equals(System.getenv("YEAROFICE_SMOKE"))) HudDebug.set(HudDebug.Mode.CYCLE);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientHud.clear();
    }
}
