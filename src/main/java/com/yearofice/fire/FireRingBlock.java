package com.yearofice.fire;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code yearofice:fire_ring}: a ring of 8 stones on cleared ground, the base every fire is built in
 * (Blueprint › Building a campfire). Right mouse with a part adds it (tinder, kindling, an armload), with a match or
 * the bow drill lights it, and with an empty hand blows on an ember, sets Low, Steady or Hot, or (crouched) banks
 * coals or takes the last part back off an unlit lay. The block entity runs the state machine.
 */
public class FireRingBlock extends BaseEntityBlock {
    public static final MapCodec<FireRingBlock> CODEC = simpleCodec(FireRingBlock::new);
    public static final EnumProperty<FireBurn> BURN = EnumProperty.create("burn", FireBurn.class);
    public static final EnumProperty<FireLay> LAY = EnumProperty.create("lay", FireLay.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public FireRingBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(BURN, FireBurn.OUT).setValue(LAY, FireLay.NONE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(BURN, LAY); }

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
        if (dir == Direction.DOWN && !canSurvive(state, level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, other, level, pos, otherPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FireRingBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, FireRegistry.FIRE_RING_ENTITY.get(), FireRingBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!FireParts.handled(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FireRingBlockEntity be)) return ItemInteractionResult.FAIL;
        return be.useItem(player, hand, stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FireRingBlockEntity be)) return InteractionResult.FAIL;
        return be.useEmptyHand(player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof FireRingBlockEntity be) be.removed();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Smoke by state, sparks on Hot, crackle while flaming (Minecraft engine › Fire › Smoke). Client only. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        FireBurn burn = state.getValue(BURN);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.25, z = pos.getZ() + 0.5;
        switch (burn) {
            case OUT, BANKED -> {
                if (burn == FireBurn.BANKED && r.nextInt(12) == 0)
                    level.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 0.3, y, z + (r.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
                return;
            }
            case EMBER -> {
                if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.015, 0);
                return;
            }
            case COALS -> {
                if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 0.4, y, z + (r.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
            }
            case TINDER, SMALL -> {
                level.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 0.2, y + 0.2, z + (r.nextDouble() - 0.5) * 0.2, 0, 0.03, 0);
            }
            case LOW, STEADY, HOT -> {
                if (r.nextInt(burn == FireBurn.LOW ? 4 : 2) == 0)
                    level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x + (r.nextDouble() - 0.5) * 0.3,
                            y + 0.4, z + (r.nextDouble() - 0.5) * 0.3, 0, 0.06, 0);
                if (burn == FireBurn.HOT && r.nextInt(3) == 0)
                    level.addParticle(ParticleTypes.LAVA, x, y + 0.3, z, 0, 0, 0);
                if (r.nextInt(5) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x + (r.nextDouble() - 0.5) * 0.4, y + 0.2, z + (r.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
            }
        }
        if (burn.state.flaming() || burn == FireBurn.COALS) {
            int odds = burn == FireBurn.HOT ? 6 : burn == FireBurn.COALS ? 30 : 12;
            if (r.nextInt(odds) == 0)
                level.playLocalSound(x, y, z, FireRegistry.CRACKLE.get(), SoundSource.BLOCKS,
                        0.5f + r.nextFloat() * 0.5f, 0.8f + r.nextFloat() * 0.4f, false);
        }
    }
}
