package com.yearofice.build.wood;

import com.yearofice.worldgen.block.YoiLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * {@code build.wood.PlacedLogs} (video 2, A1): any log a player or a machine places gets {@code placed=true} on
 * {@code BlockEvent.EntityPlaceEvent}, in every level (our valley also sets its {@code placed_mask} bit there). And
 * vanilla log mining is off (Trees, felling and firewood: "only a finished felling changes its blocks"): out of
 * creative, a log cannot be punched out; it is cut with hold-to-work.
 */
public final class PlacedLogs {
    private PlacedLogs() {}

    static void placed(BlockEvent.EntityPlaceEvent e) {
        BlockState s = e.getPlacedBlock();
        if (s.getBlock() instanceof YoiLogBlock && !s.getValue(YoiLogBlock.PLACED)) mark(e.getLevel(), e.getPos(), s);
    }

    /** Sets {@code placed=true} on the log at {@code pos} (also the GameTests' way of building a cabin). */
    public static void mark(LevelAccessor level, BlockPos pos, BlockState s) {
        level.setBlock(pos, s.setValue(YoiLogBlock.PLACED, true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    static void breakSpeed(PlayerEvent.BreakSpeed e) {
        if (e.getState().getBlock() instanceof YoiLogBlock && !e.getEntity().getAbilities().instabuild) e.setNewSpeed(0);
    }

    static boolean isLevel(LevelAccessor l) { return l instanceof Level; }
}
