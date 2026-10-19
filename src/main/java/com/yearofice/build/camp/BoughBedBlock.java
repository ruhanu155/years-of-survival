package com.yearofice.build.camp;

import com.mojang.serialization.MapCodec;
import com.yearofice.api.body.SleepService;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * {@code yearofice:bough_bed} (Blueprint › Making camp: "40 spruce boughs on a pole frame, 1 game hour; +5 °C, better
 * sleep"; Survival tuning › Sleep: bed quality 0.95). A placed 1 × 2 block, 25 cm high, built by hold-to-work with
 * spruce boughs ({@link SpruceBoughsItem}); it is never an item. Right-click lies the player down through
 * {@link SleepService}: the vanilla sleeping pose, with no respawn point and no night skip. Its quality and warmth are
 * the {@code yearofice:bed} data-map row; the fire's heat comes through the body's own heat sources.
 *
 * <p>The two halves work like vanilla's bed: {@code facing} points from the foot to the head, the head is where a
 * sleeper lies, breaking either half removes both, and only the head drops the boughs back.
 */
public class BoughBedBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BoughBedBlock> CODEC = simpleCodec(BoughBedBlock::new);
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

    /** The mattress top, in sixteenths: 4/16 m, a thick bough mat on a low frame. */
    public static final double HEIGHT_PX = 4;
    static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, HEIGHT_PX, 16);

    public BoughBedBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT)
                .setValue(OCCUPIED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, PART, OCCUPIED); }

    /** The two states of a bed whose foot faces {@code facing} toward its head. */
    public BlockState foot(Direction facing) { return defaultBlockState().setValue(FACING, facing).setValue(PART, BedPart.FOOT); }

    public BlockState head(Direction facing) { return defaultBlockState().setValue(FACING, facing).setValue(PART, BedPart.HEAD); }

    /** The head half's position for either half at {@code pos}. */
    public static BlockPos headOf(BlockState st, BlockPos pos) {
        return st.getValue(PART) == BedPart.HEAD ? pos : pos.relative(st.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState st, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos head = headOf(st, pos);
        if (!level.getBlockState(head).is(this)) return InteractionResult.CONSUME;
        if (player instanceof ServerPlayer sp) {
            SleepService.Refusal r = SleepService.get().lieDown(sp, head);
            if (r != SleepService.Refusal.NONE) {
                sp.displayClientMessage(Component.translatable("body.yearofice.sleep." + r.name().toLowerCase(Locale.ROOT)), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable LivingEntity sleeper) { return true; }

    @Override
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
        if (state.is(this) && state.getValue(OCCUPIED) != occupied) level.setBlock(pos, state.setValue(OCCUPIED, occupied), 3);
    }

    @Override
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos) { return state.getValue(FACING); }

    @Override
    protected BlockState updateShape(BlockState st, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (dir == neighbour(st)) {
            return other.is(this) && other.getValue(PART) != st.getValue(PART) ? st.setValue(OCCUPIED, other.getValue(OCCUPIED))
                    : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(st, dir, other, level, pos, otherPos);
    }

    /** From this half toward the other. */
    static Direction neighbour(BlockState st) {
        return st.getValue(PART) == BedPart.FOOT ? st.getValue(FACING) : st.getValue(FACING).getOpposite();
    }

    /** As vanilla's bed: a creative player breaking the foot takes the head without a drop. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState st, Player player) {
        if (!level.isClientSide && player.isCreative() && st.getValue(PART) == BedPart.FOOT) {
            BlockPos h = pos.relative(neighbour(st));
            BlockState hs = level.getBlockState(h);
            if (hs.is(this) && hs.getValue(PART) == BedPart.HEAD) {
                level.setBlock(h, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, h, Block.getId(hs));
            }
        }
        return super.playerWillDestroy(level, pos, st, player);
    }

    @Override
    protected VoxelShape getShape(BlockState st, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    public void fallOn(Level level, BlockState st, BlockPos pos, Entity e, float distance) {
        super.fallOn(level, st, pos, e, distance * 0.5f);
    }

    @Override
    protected boolean isPathfindable(BlockState st, PathComputationType type) { return false; }

    @Override
    protected long getSeed(BlockState st, BlockPos pos) {
        BlockPos h = headOf(st, pos);
        return net.minecraft.util.Mth.getSeed(h.getX(), pos.getY(), h.getZ());
    }
}
