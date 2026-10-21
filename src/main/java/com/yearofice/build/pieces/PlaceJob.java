package com.yearofice.build.pieces;

import com.yearofice.api.build.PieceData;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkProgressPayload;
import com.yearofice.build.work.WorkStartPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Setting a building piece is hold-to-work (SET_BLOCK): {@code labour_min} from the piece's data row, 600 game
 * deciseconds a game minute. {@code [pieces] instant_place} (Gentle on) skips the hold and lets vanilla place it.
 */
public final class PlaceJob implements WorkJob {
    private final BlockPos at;
    private final Block block;
    private final long needDs;
    private long ds;
    private boolean done;

    PlaceJob(BlockPos at, Block block, double labourMin) {
        this.at = at;
        this.block = block;
        this.needDs = Math.max(1, Math.round(labourMin * 600));
    }

    /** The piece row of a held block item, or null for anything else. */
    public static @Nullable PieceData row(ItemStack held) {
        return held.getItem() instanceof BlockItem bi ? bi.getBlock().builtInRegistryHolder().getData(PieceData.MAP) : null;
    }

    public static @Nullable WorkJob resolve(ServerPlayer p, WorkStartPayload w) {
        if (PiecesConfig.instantPlace()) return null;
        ItemStack held = p.getMainHandItem();
        PieceData row = row(held);
        if (row == null || w.entity() >= 0) return null;
        ServerLevel l = p.serverLevel();
        BlockPos at = w.pos().relative(Direction.from3DDataValue(w.face()));
        Block b = ((BlockItem) held.getItem()).getBlock();
        if (!l.getBlockState(at).canBeReplaced()) return null;
        return new PlaceJob(at, b, row.labourMin());
    }

    @Override
    public boolean valid(ServerPlayer p) {
        return p.getMainHandItem().getItem() instanceof BlockItem bi && bi.getBlock() == block
                && p.serverLevel().getBlockState(at).canBeReplaced() && p.getEyePosition().distanceTo(at.getCenter()) <= 7;
    }

    @Override
    public void work(ServerPlayer p, long step) {
        if (done) return;
        ds += step;
        if (ds < needDs) return;
        done = true;
        ServerLevel l = p.serverLevel();
        BlockState s = block.defaultBlockState();
        l.setBlock(at, s, 3);
        l.playSound(null, at, s.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
        if (!p.getAbilities().instabuild) p.getMainHandItem().shrink(1);
    }

    @Override public boolean finished() { return done; }
    @Override public float share() { return Math.min(1f, ds / (float) needDs); }
    @Override public float sharePerDs() { return 1f / needDs; }
    @Override public WorkProgressPayload.Phase phase() { return WorkProgressPayload.Phase.SET_BLOCK; }
    @Override public int done() { return 0; }
    @Override public int of() { return 0; }
}
