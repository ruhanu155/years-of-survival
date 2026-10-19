package com.yearofice.food.cooking;

import com.mojang.serialization.MapCodec;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code yearofice:spit}: two forked sticks and a crossbar standing in a fire ring, the food on the crossbar. It
 * stands only on a fire ring; right mouse with food puts it on, with an empty hand takes it off; breaking the spit
 * drops the food. It drops nothing of its own: the sticks are gathered with the camp (crafted spits are M3's stations).
 */
public class SpitBlock extends BaseEntityBlock {
    public static final MapCodec<SpitBlock> CODEC = simpleCodec(SpitBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 6, 15, 7, 10);

    public SpitBlock(Properties props) { super(props); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return RingHeat.isRing(level.getBlockState(pos.below())); }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (dir == Direction.DOWN && !RingHeat.isRing(other)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, other, level, pos, otherPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SpitBlockEntity(pos, state); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SpitBlockEntity be)) return ItemInteractionResult.FAIL;
        if (!be.empty()) {
            if (player instanceof ServerPlayer sp) HudBus.get().post(sp, HudLine.of(HudLineType.PICKUP, "spit_full", "food"));
            return ItemInteractionResult.CONSUME;
        }
        int n = be.putOn(stack);
        if (n == 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.5f, 1.2f);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SpitBlockEntity be) || be.empty()) return InteractionResult.PASS;
        ItemStack out = be.takeOff();
        if (!player.getInventory().add(out)) player.drop(out, false);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) { return List.of(); }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SpitBlockEntity be) be.dropLoad();
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Fat dripping into the fire while something cooks over it. Client only, from the synced ring. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!(level.getBlockEntity(pos) instanceof SpitBlockEntity be) || be.empty()) return;
        if (r.nextInt(6) == 0) level.addParticle(ParticleTypes.FALLING_DRIPSTONE_LAVA, pos.getX() + 0.35 + r.nextDouble() * 0.3,
                pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0, 0);
        if (be.ring() == com.yearofice.sim.food.Doneness.Ring.BURNT && r.nextInt(3) == 0)
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 0, 0.03, 0);
    }
}
