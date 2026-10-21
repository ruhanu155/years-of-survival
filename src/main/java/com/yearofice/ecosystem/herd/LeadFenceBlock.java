package com.yearofice.ecosystem.herd;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code yearofice:lead_fence} (B12): four metres of dead spruce poles and brush as one fence-shaped block. It is a
 * vanilla-fence shape for connecting and collision (it joins {@code minecraft:fences}), but it is brush, not a wall:
 * caribou follow it, they do not break through. The first one a player places opens the ambition "At the fence".
 */
public class LeadFenceBlock extends FenceBlock {
    public LeadFenceBlock(Properties p) { super(p); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer p) com.yearofice.api.story.AmbitionTracker.open(p, com.yearofice.api.story.AmbitionTracker.AT_THE_FENCE);
    }
}
