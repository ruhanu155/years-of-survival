package com.yearofice.plant;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.Block;

/** {@code yearofice:salt_crust}: the white crust of a mineral lick; {@code crust} 0-4 is the grams left in 250 g steps. */
public final class SaltCrustBlock extends BaseEntityBlock {
    public static final IntegerProperty CRUST = IntegerProperty.create("crust", 0, 4);
    public static final MapCodec<SaltCrustBlock> CODEC = simpleCodec(SaltCrustBlock::new);

    public SaltCrustBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(CRUST, 4));
    }

    public static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.6F).sound(SoundType.GRAVEL).noLootTable();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CRUST); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SaltCrustBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
}
