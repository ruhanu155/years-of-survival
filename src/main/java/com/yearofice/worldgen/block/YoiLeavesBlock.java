package com.yearofice.worldgen.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Needles and leaves. Never decay at random: felling removes a tree's leaves from its record at once
 * (Random ticks and their replacements), so there are no orphan leaves to tick.
 */
public class YoiLeavesBlock extends Block {
    public YoiLeavesBlock(Properties p) { super(p); }

    static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.GRASS).noOcclusion()
                .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.DESTROY)
                .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false);
    }
}
