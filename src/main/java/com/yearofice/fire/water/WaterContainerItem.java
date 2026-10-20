package com.yearofice.fire.water;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.ContainerData;
import com.yearofice.api.body.WaterContent;
import com.yearofice.api.hud.HudBus;
import com.yearofice.fire.FireConfig;
import com.yearofice.fire.FireRingBlock;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.sim.body.WaterPurity;
import com.yearofice.sim.fire.Vessel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A thing that holds water. Right mouse on open water fills it (the water keeps where it came from, so a drink of it
 * rolls the same gut illness as a sip from the river until it is boiled); right mouse in the air takes one sip;
 * crouched in the air pours it out. On a fire ring it is the pot (see {@code FireRingBlockEntity}). The server decides;
 * the client only swings.
 */
public class WaterContainerItem extends Item {
    public WaterContainerItem(Properties props) { super(props); }

    /** The container row of {@code stack}, or null when it is not one. */
    public static @Nullable ContainerData data(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getItemHolder().getData(ContainerData.MAP);
    }

    /**
     * Crouched with a container, vanilla skips the block's own use and calls the item's: so pouring on a fire ring
     * (the slip guard in {@code FireRingBlockEntity.pourOn}) lives here. Anything else falls through to {@link #use}.
     */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player p = ctx.getPlayer();
        Level level = ctx.getLevel();
        if (p == null || !p.isShiftKeyDown() || !(level.getBlockState(ctx.getClickedPos()).getBlock() instanceof FireRingBlock))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(ctx.getClickedPos()) instanceof FireRingBlockEntity ring) ring.pourOn(p, ctx.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ContainerData cd = data(stack);
        if (cd == null) return InteractionResultHolder.pass(stack);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        boolean onWater = hit.getType() == HitResult.Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)
                && level.getFluidState(hit.getBlockPos()).isSource();
        Vessel v = WaterContent.read(stack);
        if (onWater && !player.isShiftKeyDown()) {
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) fill(sl, sp, stack, cd, v, hit.getBlockPos());
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (v.isEmpty()) return InteractionResultHolder.pass(stack);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                WaterContent.write(stack, Vessel.empty());
                level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.6f, 1.2f);
                tell(player, "You pour it out.");
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) sip(sp, stack, v);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void fill(ServerLevel level, ServerPlayer p, ItemStack stack, ContainerData cd, Vessel v, BlockPos at) {
        WaterPurity.Source src = BodyService.get().waterSourceAt(level, at);
        if (!v.canTake(src, cd.capacityL())) {
            tell(p, v.boiled() ? "Pour out the boiled water first." : v.litres() >= cd.capacityL() - 1e-9 ? "It is full." : "Pour out first: that is other water.");
            return;
        }
        WaterContent.write(stack, v.isEmpty() ? Vessel.fresh(cd.capacityL(), src) : v.toppedUp(cd.capacityL()));
        level.playSound(null, p.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 1.0f);
        p.swing(InteractionHand.MAIN_HAND, true);
    }

    private static void sip(ServerPlayer p, ItemStack stack, Vessel v) {
        double take = Math.min(v.litres(), FireConfig.tuning().containerSipL());
        BodyService.get().drink(p, take, v.source(), v.boiled());
        WaterContent.write(stack, v.without(take));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        if (v.hot()) tell(p, "Hot water. It warms you.");
        p.swing(InteractionHand.MAIN_HAND, true);
    }

    private static void tell(Player p, String line) {
        if (p instanceof ServerPlayer sp && HudBus.installed()) HudBus.get().offerPrompt(sp, "water", line, 2);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        ContainerData cd = data(stack);
        if (cd == null) return;
        Vessel v = WaterContent.read(stack);
        if (v.isEmpty()) {
            lines.add(Component.translatable("tooltip.yearofice.water.empty", fmt(cd.capacityL())));
            return;
        }
        lines.add(Component.translatable("tooltip.yearofice.water.holds", fmt(v.litres()), fmt(cd.capacityL())));
        lines.add(Component.translatable(v.boiled() ? "tooltip.yearofice.water.boiled" : "tooltip.yearofice.water.unboiled",
                Component.translatable("tooltip.yearofice.water.source." + v.source().name().toLowerCase(java.util.Locale.ROOT))));
        if (v.hot()) lines.add(Component.translatable("tooltip.yearofice.water.hot"));
    }

    private static String fmt(double l) { return l == Math.rint(l) ? Integer.toString((int) l) : String.format(java.util.Locale.ROOT, "%.2f", l); }
}
