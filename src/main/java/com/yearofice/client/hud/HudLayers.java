package com.yearofice.client.hud;

import com.yearofice.core.YearOfIce;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * {@code client.hud.HudLayers} (Layer 8; The HUD layer stack): registers the 16 layers of {@link HudStack} in order,
 * bottom to top, and cancels the vanilla layers they replace.
 *
 * <ul>
 *   <li>Layers 1–3 (headwear, frost, body fx) are the view: they sit just above vanilla's camera overlays, under
 *       the hotbar and crosshair, and still draw with F1.</li>
 *   <li>Layers 4–16 sit together above vanilla's selected item name, in order.</li>
 *   <li>{@code yearofice:messenger_corner} (the Trail Messenger held in a hand, from the body slice) is the device
 *       itself, not a HUD layer: it sits just under the headwear, so the garment and frost draw over it.</li>
 *   <li>Cancelled: health, food, armor, air, experience bar and level, vehicle health, effects and vanilla's
 *       subtitles (captions replace them); the selected item name while a pickup line shows.</li>
 * </ul>
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class HudLayers {
    private HudLayers() {}

    public static final ResourceLocation MESSENGER_CORNER = id("messenger_corner");
    public static final ResourceLocation VITALS_STRIP = id("vitals_strip");

    /** The stack as registered, bottom to top (for the C1 check). */
    public static final List<ResourceLocation> REGISTERED = new ArrayList<>();

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path); }

    static LayeredDraw.Layer layer(String id) {
        return switch (id) {
            case "headwear" -> new StackLayers.Headwear();
            case "frost" -> new StackLayers.Frost();
            case "body_fx" -> new StackLayers.BodyFx();
            case "threat_flash" -> new StackLayers.ThreatFlash();
            case "vitals_strip" -> new VitalsStripLayer();
            case "body_figure" -> new StackLayers.BodyFigure();
            case "load_meter" -> new LoadMeterLayer();
            case "carry_slot" -> new StackLayers.CarrySlot();
            case "pickup_line" -> new StackLayers.PickupLine();
            case "messenger_line" -> new StackLayers.MessengerLine();
            case "prompt_line" -> new StackLayers.PromptLine();
            case "work_ring" -> new StackLayers.WorkRing();
            case "breath_ring" -> new StackLayers.BreathRing();
            case "captions" -> new StackLayers.Captions();
            case "minigame" -> new StackLayers.Minigame();
            case "field_note_card" -> new FieldNoteCardLayer();
            default -> throw new IllegalArgumentException(id);
        };
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent e) {
        REGISTERED.clear();
        e.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, MESSENGER_CORNER, new MessengerCornerLayer());
        ResourceLocation below = MESSENGER_CORNER;
        for (int i = 0; i < HudStack.ORDER.size(); i++) {
            String name = HudStack.ORDER.get(i);
            ResourceLocation rl = id(name);
            ResourceLocation anchor = i == HudStack.VIEW_LAYERS ? VanillaGuiLayers.SELECTED_ITEM_NAME : below;
            e.registerAbove(anchor, rl, layer(name));
            REGISTERED.add(rl);
            below = rl;
        }
    }

    @SubscribeEvent
    public static void hideVanilla(RenderGuiLayerEvent.Pre e) {
        ResourceLocation n = e.getName();
        if (n.equals(VanillaGuiLayers.PLAYER_HEALTH) || n.equals(VanillaGuiLayers.FOOD_LEVEL) || n.equals(VanillaGuiLayers.ARMOR_LEVEL)
                || n.equals(VanillaGuiLayers.AIR_LEVEL) || n.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || n.equals(VanillaGuiLayers.EXPERIENCE_LEVEL) || n.equals(VanillaGuiLayers.VEHICLE_HEALTH)
                || n.equals(VanillaGuiLayers.EFFECTS) || n.equals(VanillaGuiLayers.SUBTITLE_OVERLAY)) {
            e.setCanceled(true);
        } else if (n.equals(VanillaGuiLayers.SELECTED_ITEM_NAME) && (ClientHud.pickup() != null || HudDebug.on())) {
            e.setCanceled(true);
        } else if (n.equals(VanillaGuiLayers.HOTBAR) && HudDebug.mode() == HudDebug.Mode.FULL) {
            // The carry slot replaces the hotbar while a load is carried; the debug frame shows that state.
            e.setCanceled(true);
        }
    }
}
