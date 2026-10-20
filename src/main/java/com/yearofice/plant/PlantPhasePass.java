package com.yearofice.plant;

import com.yearofice.api.clock.ClockService;
import com.yearofice.sim.plant.PhaseModel;
import com.yearofice.sim.plant.PlantPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Sets {@code phase} on plant blocks from the season's degree-days (Plants and fungi › The season pass). Never from random ticks. */
public final class PlantPhasePass {
    private PlantPhasePass() {}

    /** The phase a species is in today, from the normal-year curve times the year's warmth. */
    public static PlantPhase phaseNow(PlantSpec spec, int doy, boolean snow) {
        return PhaseModel.phaseFor(spec.phases, PhaseModel.ddAt(doy, PlantDials.warmth), doy, snow);
    }

    /** Re-phases every plant block within {@code r} blocks (y +/- 8) of {@code c}; returns how many changed. */
    public static int around(ServerLevel level, BlockPos c, int r) {
        int doy = ClockService.get().dayOfYear();
        int changed = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -8; y <= 8; y++) {
            m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
            if (!level.isLoaded(m)) continue;
            BlockState st = level.getBlockState(m);
            if (!(st.getBlock() instanceof PlantBlock pb)) continue;
            PlantSpec spec = PlantData.get(pb.species());
            if (spec == null) continue;
            PlantBlock.Phase want = PlantBlock.Phase.of(phaseNow(spec, doy, false));
            if (st.getValue(PlantBlock.PHASE) != want) {
                level.setBlock(m, st.setValue(PlantBlock.PHASE, want), 2);
                changed++;
            }
        }
        return changed;
    }
}
