package com.yearofice.food.traps;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code yearofice:deadfall} (the figure-4: three notched sticks and a flat rock) and {@code yearofice:mouse_deadfall}
 * (a flat stone on a flint-flake trigger, set beside a cache). Rolls at dawn and dusk like the snare; {@code sprung}
 * shows the fallen rock. Right mouse checks it (takes the catch, or resets a sprung trap); crouched with an empty hand
 * it is picked up.
 */
public class DeadfallBlock extends BaseEntityBlock {
    public static final MapCodec<DeadfallBlock> CODEC = simpleCodec(p -> new DeadfallBlock(p, false));
    public static final BooleanProperty SPRUNG = BooleanProperty.create("sprung");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 7, 14);
    private final boolean mouse;

    public DeadfallBlock(Properties props, boolean mouse) {
        super(props);
        this.mouse = mouse;
        registerDefaultState(stateDefinition.any().setValue(SPRUNG, false));
    }

    public boolean mouse() { return mouse; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(SPRUNG); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (dir == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, other, level, pos, otherPos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof DeadfallBlockEntity be) be.set();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DeadfallBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return null; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof DeadfallBlockEntity be)) return InteractionResult.FAIL;
        return be.use(player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(new ItemStack(asItem())); }
}
