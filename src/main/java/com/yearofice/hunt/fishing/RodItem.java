package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.HookBait;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * The willow rod and winder ({@code yearofice:rod}). Right mouse held 0–1.5 s and released casts (Blueprint ›
 * Fishing methods › The fight, as built: Cast); right mouse with a bobber out reels it in. Left mouse is the
 * strike and the fight's reel, read by the client and sent as {@code fish_input}. The server decides everything.
 */
public final class RodItem extends Item {
    /** Line condition in % is the rod's damage counted down from this. */
    public static final int LINE_MAX = 100;

    public RodItem(Properties p) {
        super(p.component(FishingRegistry.TACKLE.get(), HookBait.NONE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            BobberEntity out = FishingService.bobberOf(sp);
            if (out != null) {
                if (!out.hooked()) FishingService.reelIn(sp, out, "Reeled in.");
                return InteractionResultHolder.consume(stack);
            }
        } else if (BobberEntity.clientHasBobber(player)) {
            return InteractionResultHolder.consume(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof ServerPlayer sp)) return;
        double heldS = (getUseDuration(stack, entity) - timeLeft) / 20.0;
        FishingService.cast(sp, stack, heldS);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72_000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.SPEAR; }

    public static int lineCondition(ItemStack rod) { return Math.max(0, LINE_MAX - rod.getDamageValue()); }

    public static HookBait bait(ItemStack rod) { return rod.getOrDefault(FishingRegistry.TACKLE.get(), HookBait.NONE); }

    public static void setBait(ItemStack rod, HookBait bait) { rod.set(FishingRegistry.TACKLE.get(), bait); }

    /** Line wear never breaks the rod in the slice: a worn line still holds 70% (Tackle: S_eff at condition 0). */
    public static void wearLine(ItemStack rod, double pct) {
        int d = rod.getDamageValue() + (int) Math.round(pct);
        rod.setDamageValue(Math.max(0, Math.min(LINE_MAX, d)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        HookBait b = bait(stack);
        lines.add(Component.literal(b.present()
                ? String.format(Locale.ROOT, "Bait: %s (%d bites, %d min)", b.bait(), b.bitesLeft(), b.minutesLeft())
                : "Bait: none").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.literal("Line: plant fiber, " + lineCondition(stack) + "%").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
