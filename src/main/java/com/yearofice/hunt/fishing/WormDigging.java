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
 * Where the slice's worms come from (soil near water, or the forest floor under trees at a lower rate) (Blueprint › Fishing in depth › Bait: "Dug in riparian or bog soil: 2–6 per 10
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
        if (!isSoil(state)) return 0;
        int base = nearWater(level, pos, t.wormDigRange()) ? t.wormDigMax() : underTrees(level, pos) ? t.wormForestMax() : 0;
        if (base <= 0) return 0;
        long minute = FishingClock.minute(level.getServer());
        int month = FishSeason.month(FishSeason.dayOfYear(FishingClock.day(minute)));
        if (month >= 10 || month <= 4) return 0;
        int max = com.yearofice.api.env.EnvService.get().precip(level, pos).liquid() ? 2 * base : base;
        double u = Draws.u(level.getSeed() ^ Draws.name("worm"), pos.asLong(), level.getGameTime(), 1);
        return (int) Math.floor(u * (max + 1));
    }

    static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> DIGGABLE = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("yearofice", "diggable_soil"));

    /** Soil a worm lives in: vanilla dirt, or the valley's diggable soils but not bare gravel, sand, scree or rust stain. */
    public static boolean isSoil(BlockState state) {
        if (state.is(BlockTags.DIRT)) return true;
        if (!state.is(DIGGABLE)) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return !(id.equals("gravel") || id.equals("river_sand") || id.equals("scree_stones") || id.equals("rust_stain"));
    }

    /** True if this soil can turn up worms at all (near water, or under trees), whatever the month. */
    public static boolean diggable(ServerLevel level, BlockPos pos, BlockState state, FishingTuning t) {
        return isSoil(state) && (nearWater(level, pos, t.wormDigRange()) && t.wormDigMax() > 0 || underTrees(level, pos) && t.wormForestMax() > 0);
    }

    /** Forest floor: a log or leaves within 3 blocks sideways and 1-8 above the soil. */
    static boolean underTrees(ServerLevel level, BlockPos pos) {
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-3, 1, -3), pos.offset(3, 8, 3))) {
            var st = level.getBlockState(q);
            if (st.is(BlockTags.LOGS) || st.is(BlockTags.LEAVES) || st.getBlock() instanceof com.yearofice.worldgen.block.YoiLogBlock
                    || st.getBlock() instanceof com.yearofice.worldgen.block.YoiLeavesBlock) return true;
        }
        return false;
    }

    static boolean nearWater(ServerLevel level, BlockPos pos, int r) {
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-r, -2, -r), pos.offset(r, 1, r))) {
            if (level.getFluidState(q).is(FluidTags.WATER)) return true;
        }
        return false;
    }
}
