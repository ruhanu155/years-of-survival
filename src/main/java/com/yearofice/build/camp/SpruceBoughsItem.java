package com.yearofice.build.camp;

import com.yearofice.build.BuildConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * {@code yearofice:spruce_boughs}: the lower branches of a living spruce, cut by holding use on its leaves
 * ({@link BoughCutting}). Holding use on the ground with {@code bough_bed_boughs} of them builds a bough bed there
 * ({@link BedBuilding}), a hold-to-work job of {@code bough_bed_min} game minutes.
 */
public class SpruceBoughsItem extends Item {
    public SpruceBoughsItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player p = ctx.getPlayer();
        if (p == null) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        Direction facing = p.getDirection();
        BlockPos foot = BedBuilding.footFor(level, ctx.getClickedPos(), ctx.getClickedFace());
        String why = BedBuilding.problem(level, foot, facing, p);
        if (why != null) {
            if (p instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("building.yearofice." + why, BuildConfig.bedBoughs(), held(p)), true);
            }
            return InteractionResult.FAIL;
        }
        if (p instanceof ServerPlayer sp) BedBuilding.begin(sp, foot, facing);
        p.startUsingItem(ctx.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return Work.ticks(BuildConfig.bedMin()); }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BRUSH; }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer sp && !BedBuilding.tick(sp)) sp.stopUsingItem();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) BedBuilding.finish(sp, stack);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (entity instanceof ServerPlayer sp) BedBuilding.cancel(sp);
    }

    /** Boughs the player carries. */
    public static int held(Player p) { return BoughCutting.carried(p); }

    /** Takes {@code n} boughs: from the held stack first, then the rest of the inventory. */
    public static void take(Player p, ItemStack held, int n) {
        int fromHand = Math.min(n, held.getCount());
        held.shrink(fromHand);
        int left = n - fromHand;
        for (int i = 0; i < p.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s == held || !s.is(com.yearofice.build.BuildRegistry.SPRUCE_BOUGHS.get())) continue;
            int k = Math.min(left, s.getCount());
            s.shrink(k);
            left -= k;
        }
    }
}
