package com.yearofice.client.food;

import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.food.cooking.CookingRegistry;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.DecayLadder;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.food.Doneness;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Client side of cooking and freshness: the spit renderer, the food look property and the tooltip freshness line.
 * Nothing here decides anything; it reads the synced components and the client's own D(now), which the decay
 * service computes from the same day time as the server.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class FoodClient {
    private FoodClient() {}

    /** The item property the food models switch on: 0 raw, 0.25 raw aging, 0.5 raw turning or worse, 0.75 cooked, 1 burnt. */
    public static final ResourceLocation LOOK = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "look");

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(CookingRegistry.SPIT_ENTITY.get(), ctx -> new SpitRenderer());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            for (Item item : new Item[] {EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), EcosystemRegistry.WILLOW_PTARMIGAN_MEAT.get(),
                    FishingRegistry.ARCTIC_GRAYLING.get()}) {
                ItemProperties.register(item, LOOK, (stack, level, entity, seed) -> look(stack, level));
            }
        });
        NeoForge.EVENT_BUS.addListener(FoodClient::tooltip);
    }

    static Level clientLevel(ClientLevel level) { return level != null ? level : Minecraft.getInstance().level; }

    /** The look value for a stack: burnt, cooked, or raw by its stage. */
    public static float look(ItemStack stack, ClientLevel cl) {
        Doneness.Ring ring = stack.get(YoiComponents.DONENESS.get());
        if (ring == Doneness.Ring.BURNT) return 1.0f;
        Fresh f = stack.get(YoiComponents.FRESH.get());
        if (ring != null || (f != null && f.state() != FoodState.RAW)) return 0.75f;
        Level level = clientLevel(cl);
        if (f == null || level == null) return 0.0f;
        int stage = DecayLadder.FOOD.stageOf(f.progress(DecayService.get().dNow(level)));
        return stage == 0 ? 0.0f : stage == 1 ? 0.25f : 0.5f;
    }

    /** "Fresh 85 · cooked, done · spoils in 3.2 days" under any food stack that carries freshness. */
    static void tooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        Fresh f = s.get(YoiComponents.FRESH.get());
        Level level = Minecraft.getInstance().level;
        if (f == null || level == null || FoodMap.row(s) == null) return;
        DecayService decay = DecayService.get();
        double p = f.progress(decay.dNow(level));
        int stage = DecayLadder.FOOD.stageOf(p);
        Doneness.Ring ring = s.get(YoiComponents.DONENESS.get());
        String state = f.state() == FoodState.RAW ? "raw" : f.state().key() + (ring == null ? "" : ", " + ring.name().toLowerCase(Locale.ROOT));
        double days = decay.daysUntil(level, s, 0.7);
        Component when = stage >= 2 ? Component.translatable("food.yearofice.tooltip.turned")
                : Double.isInfinite(days) ? Component.translatable("food.yearofice.tooltip.frozen")
                : Component.translatable("food.yearofice.tooltip.spoils_in", String.format(Locale.ROOT, "%.1f", days));
        ChatFormatting colour = stage == 0 ? ChatFormatting.GREEN : stage == 1 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        e.getToolTip().add(Component.translatable("food.yearofice.tooltip.fresh",
                Component.translatable("decay.yearofice.food." + stage), DecayLadder.tooltipFreshness(p), state, when).withStyle(colour));
    }
}
