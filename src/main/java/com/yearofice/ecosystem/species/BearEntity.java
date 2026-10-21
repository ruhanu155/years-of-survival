package com.yearofice.ecosystem.species;

import com.yearofice.ecosystem.species.rig.PredatorEntity;
import com.yearofice.sim.ecosystem.AnimalState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Black bear and grizzly (Land animals in depth › Grizzly and black bear): the ursid rig. {@code stand} plays during
 * Alert: a standing bear is sniffing, not threatening. A bear dens from October 20 to April 20 and does not
 * appear then; between, it is a scavenger first and a hunter of small prey second.
 */
public class BearEntity extends PredatorEntity {
    public BearEntity(EntityType<? extends BearEntity> type, Level level) { super(type, level); }

    @Override
    protected double wanderRadius() { return 40; }

    private long nextFishTick;
    private int fishCaught;

    public int fishCaught() { return fishCaught; }

    /** At the edge of a spot with fish, a hungry bear takes one every ten seconds (WaterLife.takeFish) and is a little less hungry. */
    @Override
    protected void tickWater(net.minecraft.server.level.ServerLevel level) {
        super.tickWater(level);
        net.minecraft.core.BlockPos edge = waterEdge();
        long t = level.getGameTime();
        if (edge == null || t < nextFishTick || driveState().hunger <= 0.2 || distanceToSqr(edge.getX() + 0.5, getY(), edge.getZ() + 0.5) >= 16) return;
        nextFishTick = t + 200;
        for (var spot : com.yearofice.hunt.fishing.WaterLife.spotsNear(level, edge, 12)) {
            if (com.yearofice.hunt.fishing.WaterLife.takeFish(level, spot) != null) {
                fishCaught++;
                driveState().hunger = Math.max(0, driveState().hunger - 0.15);
                return;
            }
        }
    }

    @Override
    protected String animKey(AnimalState s, int move) {
        if (s == AnimalState.ALERT) return "stand";
        return super.animKey(s, move);
    }
}
