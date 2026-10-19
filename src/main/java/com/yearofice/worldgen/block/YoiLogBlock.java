package com.yearofice.worldgen.block;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * A standing log of one species. The {@code dead}, {@code ants} and {@code scar} states and felling (the falling
 * tree, rounds and splits) join with Crafting and building's trees and firewood (M2 lane B).
 */
public class YoiLogBlock extends RotatedPillarBlock {
    public YoiLogBlock(Properties p) { super(p); }

    static BlockBehaviour.Properties props(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(2.0F).sound(SoundType.WOOD);
    }
}
