package com.yearofice.client.ui.messenger;

import com.yearofice.body.net.BodyNet;
import com.yearofice.body.net.DrinkPayload;
import com.yearofice.client.hud.ClientBody;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.item.messenger.TrailMessengerItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The client half of the messenger and of drinking: installs the item's open hook and the {@code body_sync} sink,
 * reads the M key, and turns an empty-hand use on open water into a {@code yearofice:drink} request.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class MessengerClient {
    private MessengerClient() {}

    /** The M key, registered with the other 12 in {@code client.input.YoiKeys}. */
    public static final KeyMapping OPEN = com.yearofice.client.input.YoiKeys.MESSENGER;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        TrailMessengerItem.openScreen = MessengerClient::open;
        BodyNet.clientSink = ClientBody::accept;
        BodyNet.sosSink = pl -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new com.yearofice.client.body.SosFlightScreen(pl)));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) mc.setScreen(new MessengerScreen());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            boolean neck = false;
            for (int i = 0; i < com.yearofice.item.inventory.PackSlots.size(mc.player, com.yearofice.item.inventory.PackSlots.NECK); i++) {
                neck |= com.yearofice.item.inventory.PackSlots.get(mc.player, com.yearofice.item.inventory.PackSlots.NECK, i).is(MessengerItems.MESSENGER.get());
            }
            if (neck || mc.player.getInventory().contains(new net.minecraft.world.item.ItemStack(MessengerItems.MESSENGER.get()))) {
                open();
            } else {
                mc.player.displayClientMessage(Component.translatable("messenger.yearofice.warn.no_messenger"), true);
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientBody.clear();
    }

    /** Empty main hand, use key, looking at a water source within reach: ask the server for a sip. */
    @SubscribeEvent
    public static void use(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isUseItem() || e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || !p.getMainHandItem().isEmpty() || p.isSpectator()) return;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) return;
        HitResult hit = p.pick(p.blockInteractionRange(), 1.0f, true);
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) return;
        FluidState f = mc.level.getFluidState(bh.getBlockPos());
        if (!f.is(FluidTags.WATER) || !f.isSource()) return;
        PacketDistributor.sendToServer(DrinkPayload.INSTANCE);
        e.setSwingHand(false);
        e.setCanceled(true);
    }
}
