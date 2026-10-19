package com.yearofice.client.minigame.fishing;

import com.yearofice.core.YearOfIce;
import com.yearofice.hunt.fishing.FishingRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Registers the bobber's renderer and the {@code minigame} GUI layer (Minecraft development › The HUD layer stack:
 * the fishing arc draws in {@code minigame}). The fishing fight is the layer's first user; when Look, sound and UI
 * build the full stack in {@code client.hud}, the layer's registration moves there under the same id.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class FishingClientEvents {
    private FishingClientEvents() {}

    public static final ResourceLocation MINIGAME = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "minigame");

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(FishingRegistry.BOBBER.get(), BobberRenderer::new);
    }

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.CROSSHAIR, MINIGAME, new FishingOverlay());
    }
}
