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

    @Override
    protected String animKey(AnimalState s, int move) {
        if (s == AnimalState.ALERT) return "stand";
        return super.animKey(s, move);
    }
}
