package com.yearofice.food.traps;

import com.yearofice.ecosystem.spawn.Habitats;
import com.yearofice.ecosystem.species.SnowshoeHareEntity;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.sim.ecosystem.Snare;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reads a snare's set from the blocks around it when it is placed (Blueprint › Traps; Every concept from the
 * video › Snare funnels):
 *
 * <ul>
 *   <li>On a trail: the snare stands on a path block ({@code dirt_path}) or in a run between cover on both sides
 *       (hares run through willow and spruce edges). The World department's trail cells take over when stage 7
 *       ships.</li>
 *   <li>Beside cover: cover within 2 blocks.</li>
 *   <li>A funnel: cover, logs or rocks within 1 block on both sides, across the snare's facing.</li>
 *   <li>Habitat: the share of hare habitat within 32 blocks scales the hares in range.</li>
 * </ul>
 * Bait and human scent land with M5.
 */
public final class SnareSite {
    private SnareSite() {}

    public record Reading(Snare.SetQuality quality, double habitatShare) {}

    public static Reading read(ServerLevel level, BlockPos pos, Direction facing) {
        Direction side = facing.getClockWise();
        boolean funnel = wall(level, pos.relative(side)) && wall(level, pos.relative(side.getOpposite()));
        boolean runway = funnel || (coverNear(level, pos.relative(side, 2)) && coverNear(level, pos.relative(side.getOpposite(), 2)));
        boolean trail = level.getBlockState(pos.below()).is(Blocks.DIRT_PATH) || runway;
        boolean cover = false;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 2, 2))) {
            if (Habitats.isCover(level.getBlockState(p))) { cover = true; break; }
        }
        double share = Habitats.share(level, pos, 32, SpeciesBook.get(SnowshoeHareEntity.ID).habitat(), 32, level.getSeed());
        return new Reading(new Snare.SetQuality(trail, cover, funnel, false, 1, 1), share);
    }

    static boolean wall(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return Habitats.isCover(s) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.STONE) || s.is(Blocks.MOSSY_COBBLESTONE);
    }

    static boolean coverNear(ServerLevel level, BlockPos p) {
        return Habitats.isCover(level.getBlockState(p)) || Habitats.isCover(level.getBlockState(p.above()));
    }
}
