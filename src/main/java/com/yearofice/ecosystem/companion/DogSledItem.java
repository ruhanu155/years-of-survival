package com.yearofice.ecosystem.companion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** {@code yearofice:dog_sled}: sets a sled on the ground (built from M8 pieces later). */
public class DogSledItem extends Item {
    public DogSledItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        if (!(c.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos at = c.getClickedPos().relative(c.getClickedFace());
        SledEntity sled = CompanionRegistry.SLED.get().create(level);
        if (sled == null) return InteractionResult.FAIL;
        sled.moveTo(Vec3.atBottomCenterOf(at));
        sled.setYRot(c.getRotation());
        level.addFreshEntity(sled);
        if (c.getPlayer() == null || !c.getPlayer().getAbilities().instabuild) c.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
