package com.yearofice.hunt.sign;

import com.yearofice.sim.hunt.Sign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One piece of animal sign on the ground. Right mouse reads it ({@link SignService#read}); a scheduled tick ages it away. */
public final class SignBlock extends Block implements EntityBlock {
    public final Sign.Kind kind;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 0.5, 15);

    public SignBlock(Properties p, Sign.Kind kind) {
        super(p);
        this.kind = kind;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SignBlockEntity(pos, state); }

    @Override
    protected BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) {
        return d == Direction.DOWN && l.getBlockState(p.below()).isAir() ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : s;
    }

    @Override
    protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) { SignService.review(l, p); }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) {
        if (pl instanceof ServerPlayer sp) {
            Sign.Read read = SignService.read(sp.serverLevel(), p, sp);
            if (read != null) sp.displayClientMessage(net.minecraft.network.chat.Component.literal(read.text()), true);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
