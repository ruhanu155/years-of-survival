package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.Draws;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishingTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Where the slice's worms come from (Blueprint › Fishing in depth › Bait: "Dug in riparian or bog soil: 2–6 per 10
 * min, doubled for a day after rain; none once the ground freezes in October"). The dig is a hold-to-work job on
 * soil with a digging tool once Player systems' work ring lands; until then a soil block broken within a few blocks
 * of water turns up 0–2 worms, twice that while it rains, and none from October to April.
 */
public final class WormDigging {
    private WormDigging() {}

    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getPlayer() instanceof ServerPlayer p) || p.isCreative()) return;
        int n = wormsAt(level, e.getPos(), e.getState(), FishingConfig.tuning());
        if (n <= 0) return;
        BlockPos at = e.getPos();
        level.addFreshEntity(new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
                new ItemStack(FishingRegistry.WORM.get(), n)));
    }

    /** Worms a broken block gives: 0 unless it is soil near water in the thawed months. */
    public static int wormsAt(ServerLevel level, BlockPos pos, BlockState state, FishingTuning t) {
        if (!state.is(BlockTags.DIRT) || !nearWater(level, pos, t.wormDigRange())) return 0;
        long minute = FishingClock.minute(level.getServer());
        int month = FishSeason.month(FishSeason.dayOfYear(FishingClock.day(minute)));
        if (month >= 10 || month <= 4) return 0;
        int max = com.yearofice.api.env.EnvService.get().precip(level, pos).liquid() ? 2 * t.wormDigMax() : t.wormDigMax();
        double u = Draws.u(level.getSeed() ^ Draws.name("worm"), pos.asLong(), level.getGameTime(), 1);
        return (int) Math.floor(u * (max + 1));
    }

    static boolean nearWater(ServerLevel level, BlockPos pos, int r) {
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-r, -2, -r), pos.offset(r, 1, r))) {
            if (level.getFluidState(q).is(FluidTags.WATER)) return true;
        }
        return false;
    }
}
