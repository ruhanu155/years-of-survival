package com.yearofice.ecosystem.small;

import com.yearofice.ecosystem.species.SmallGameEntity;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.life.LemmingCycle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The brown, collared and bog lemmings (Land animals in depth › Lemmings) on the small-mammal body. The collared
 * lemming turns white October 25 to May 15; in a lemming year (index L of 2 or more) a lemming stops fleeing.
 */
public class LemmingEntity extends SmallGameEntity {
    public LemmingEntity(EntityType<? extends LemmingEntity> type, Level level) { super(type, level); }

    @Override
    protected void updateLooks(int doy) {
        if (speciesPath().equals("collared_lemming")) setCoat(LemmingCycle.collaredCoat(doy));
    }

    @Override
    public void think(ServerLevel level) {
        if (state == AnimalState.ROUTINE && speciesPath().equals("brown_lemming") && LemmingIndex.bold(level)) return;
        super.think(level);
    }
}
