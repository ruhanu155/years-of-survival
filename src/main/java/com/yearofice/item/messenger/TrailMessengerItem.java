package com.yearofice.item.messenger;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Trail Messenger: held in a hand it shows the corner screen (client HUD); use (right-click) opens the full
 * messenger screen. The screen is client-only, so the item calls a hook the client installs at start-up, and a
 * dedicated server never loads a client class.
 */
public class TrailMessengerItem extends Item {
    /** Installed by the client; opens the messenger screen. */
    public static volatile Runnable openScreen = () -> { };

    public TrailMessengerItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) openScreen.run();
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static int battery(ItemStack stack) {
        Integer b = stack.get(MessengerItems.BATTERY.get());
        return b == null ? 0 : b;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.yearofice.messenger.battery", battery(stack) / 100));
        tooltip.add(Component.translatable("item.yearofice.messenger.hint"));
    }
}
