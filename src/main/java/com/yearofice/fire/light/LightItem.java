package com.yearofice.fire.light;

import com.yearofice.api.env.LightData;
import com.yearofice.api.env.LightState;
import com.yearofice.api.hud.HudBus;
import com.yearofice.fire.FireRingBlock;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.sim.fire.FireState;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A carried light. A torch or candle is lit at a burning fire (right mouse on the ring) and snuffed with right mouse
 * in the air; the headlamp switches on and off with right mouse. While lit it burns down by game minutes
 * ({@link LightBurn}); {@code env.light} counts it from either hand, and the client draws it.
 */
public class LightItem extends Item {
    public LightItem(Properties props) { super(props); }

    public static @Nullable LightData data(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getItemHolder().getData(LightData.MAP);
    }

    /** True when {@code stack} is lit. */
    public static boolean lit(ItemStack stack) {
        LightState s = stack.get(LightState.LIGHT_STATE.get());
        return s != null && s.lit();
    }

    /** A headlamp-style light has a battery and needs no fire to switch on. */
    public static boolean battery(LightData d) { return d.burnMin() >= com.yearofice.env.light.HeldLights.BATTERY_MIN; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        LightData d = data(ctx.getItemInHand());
        if (d == null || battery(d)) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        if (!(level.getBlockState(ctx.getClickedPos()).getBlock() instanceof FireRingBlock)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof FireRingBlockEntity ring)) return InteractionResult.PASS;
        FireState s = ring.fire().state();
        Player p = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (!(s == FireState.SMALL || s == FireState.FIRE || s == FireState.COALS)) {
            tell(p, "Light it at a burning fire.");
            return InteractionResult.CONSUME;
        }
        if (lit(stack)) return InteractionResult.CONSUME;
        if (LightState.minutesLeft(stack, d) <= 0) {
            tell(p, "It is burned down.");
            return InteractionResult.CONSUME;
        }
        LightState.set(stack, true, LightState.minutesLeft(stack, d));
        level.playSound(null, ctx.getClickedPos(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        LightData d = data(stack);
        if (d == null) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (lit(stack)) {
            LightState.set(stack, false, LightState.minutesLeft(stack, d));
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3f, 1.5f);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        if (battery(d)) {
            if (LightState.minutesLeft(stack, d) <= 0) {
                tell(player, "The battery is flat.");
            } else {
                LightState.set(stack, true, LightState.minutesLeft(stack, d));
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.6f);
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        tell(player, "Light it at a burning fire.");
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return false; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        LightData d = data(stack);
        if (d == null) return;
        int left = LightState.minutesLeft(stack, d);
        if (d.burnMin() > 0) {
            lines.add(Component.translatable(battery(d) ? "tooltip.yearofice.light.battery" : "tooltip.yearofice.light.burn", fmt(left)));
        }
        lines.add(Component.translatable(lit(stack) ? "tooltip.yearofice.light.lit" : "tooltip.yearofice.light.unlit"));
    }

    static String fmt(int minutes) {
        return minutes >= 120 ? (minutes / 60) + " h" : minutes + " min";
    }

    private static void tell(Player p, String line) {
        if (p instanceof ServerPlayer sp && HudBus.installed()) HudBus.get().offerPrompt(sp, "light", line, 2);
    }
}
