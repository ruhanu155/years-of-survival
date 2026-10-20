package com.yearofice.crop;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A crop on a bed: {@code stage} 0 sown, 1 sprout, 2 leaf, 3 head or flower, 4 ripe, 5 dead; {@code frosted}. The block entity holds the numbers. */
public final class CropBlock extends BaseEntityBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 5);
    public static final BooleanProperty FROSTED = BooleanProperty.create("frosted");
    public static final MapCodec<CropBlock> CODEC = simpleCodec(CropBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

    public CropBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(FROSTED, false));
    }

    public static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.CROP).noOcclusion().noLootTable();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(STAGE, FROSTED); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CropBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return l.getBlockState(p.below()).getBlock() instanceof GardenBedBlock; }

    @Override
    protected boolean isRandomlyTicking(BlockState s) { return false; }
}
