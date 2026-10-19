package com.yearofice.client.hud;

import com.yearofice.core.YearOfIce;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * {@code client.hud.HudLayers}: registers the body slice's GUI layers and hides vanilla's hunger and health bars
 * (Minecraft development › The HUD layer stack). There is no vanilla HUD for the body: the vitals strip appears
 * only when a need passes its first warning, and the corner screen only while the Trail Messenger is in a hand.
 * The other 13 layers of the stack land with their systems.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class HudLayers {
    private HudLayers() {}

    public static final ResourceLocation VITALS_STRIP = id("vitals_strip");
    public static final ResourceLocation MESSENGER_CORNER = id("messenger_corner");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path); }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, VITALS_STRIP, new VitalsStripLayer());
        e.registerAbove(VanillaGuiLayers.HOTBAR, MESSENGER_CORNER, new MessengerCornerLayer());
    }

    /** The body owns health and food: their vanilla bars never draw. */
    @SubscribeEvent
    public static void hideVanilla(RenderGuiLayerEvent.Pre e) {
        ResourceLocation n = e.getName();
        if (n.equals(VanillaGuiLayers.PLAYER_HEALTH) || n.equals(VanillaGuiLayers.FOOD_LEVEL)) e.setCanceled(true);
    }
}
