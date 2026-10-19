package com.yearofice.worldgen.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;

/**
 * A standing log of one species. {@code placed} (video 2, A1; Trees, felling and firewood › Placed logs never fall)
 * is false on every log the world grew and true on any log a player placed ({@code build.wood.PlacedLogs}); the fall
 * search never counts or crosses a placed log. The {@code dead}, {@code ants} and {@code scar} states join later.
 */
public class YoiLogBlock extends RotatedPillarBlock {
    public static final BooleanProperty PLACED = BooleanProperty.create("placed");

    public YoiLogBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(PLACED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(PLACED);
    }

    static BlockBehaviour.Properties props(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(2.0F).sound(SoundType.WOOD);
    }
}
