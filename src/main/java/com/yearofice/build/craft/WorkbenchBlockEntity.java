package com.yearofice.build.craft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The workbench's block entity ({@code yearofice:workbench}). It holds nothing yet: storage within 10 blocks is M8's. */
public class WorkbenchBlockEntity extends BlockEntity {
    public WorkbenchBlockEntity(BlockPos pos, BlockState state) { super(CraftRegistry.WORKBENCH_ENTITY.get(), pos, state); }
}
