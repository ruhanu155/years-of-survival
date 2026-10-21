package com.yearofice.client.minigame.fishing;

import com.yearofice.core.YearOfIce;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.client.hud.MinigameSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registers the bobber's renderer and the fight arc as an owner of the {@code yearofice:minigame} slot (Minecraft
 * development › The HUD layer stack). The layer itself is registered by Look in {@code client.hud.HudLayers}
 * under the same id (RECONCILE 168).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class FishingClientEvents {
    private FishingClientEvents() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(FishingRegistry.BOBBER.get(), BobberRenderer::new);
        for (var t : FishingRegistry.FISH_TYPES.values()) e.registerEntityRenderer(t.get(), FishRenderer::new);
    }

    /** The fight arc is an owner of the one minigame slot; Look registers the layer itself in {@code client.hud}. */
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        MinigameSlot.register("fishing", FishingClient::active, new FishingOverlay());
    }
}
