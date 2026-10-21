package com.yearofice.story.keepsake;

import com.yearofice.api.clock.ClockService;
import com.yearofice.story.Journal;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A keepsake: weighs almost nothing, never decays; use reads it, and the first time it is carried it leaves a journal clip. */
public class KeepsakeItem extends Item {
    public KeepsakeItem(Properties p) { super(p); }

    static String idOf(ItemStack s) { return s.getOrDefault(KeepsakeRegistry.NOTE_ID.get(), ""); }

    @Override
    public Component getName(ItemStack stack) {
        Keepsakes.Def d = Keepsakes.get(idOf(stack));
        return d == null ? super.getName(stack) : Component.literal(d.name());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        Keepsakes.Def d = Keepsakes.get(idOf(s));
        if (d == null) return InteractionResultHolder.pass(s);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(d.name()));
            d.read().forEach(l -> sp.sendSystemMessage(Component.literal(l)));
            clip(sp, d);
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide || !(holder instanceof ServerPlayer sp) || level.getGameTime() % 40 != 0) return;
        Keepsakes.Def d = Keepsakes.get(idOf(stack));
        if (d != null) clip(sp, d);
    }

    /** One journal clip per keepsake per player. */
    static void clip(ServerPlayer p, Keepsakes.Def d) {
        if (Journal.entries(p).stream().anyMatch(e -> "keepsake".equals(e.kind()) && d.id().equals(e.cause()))) return;
        long minute = ClockService.installed() ? ClockService.get().nowMinute() : 0;
        Journal.record(p, "keepsake", d.id(), d.clip(), minute, (int) Math.floorDiv(minute, 1440L));
    }
}
