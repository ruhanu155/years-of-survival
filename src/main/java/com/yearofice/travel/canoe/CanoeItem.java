package com.yearofice.travel.canoe;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** The carried canoe (the portage): use toward water or the bank sets it down as the entity. */
public class CanoeItem extends Item {
    public CanoeItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(held);
        if (!level.isClientSide) {
            CanoeEntity c = CanoeRegistry.CANOE_ENTITY.get().create(level);
            if (c == null) return InteractionResultHolder.fail(held);
            c.moveTo(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, player.getYRot(), 0);
            level.addFreshEntity(c);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
