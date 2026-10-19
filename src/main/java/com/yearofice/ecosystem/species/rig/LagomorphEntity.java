package com.yearofice.ecosystem.species.rig;

import com.yearofice.ecosystem.LandAnimal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The small-rodent and lagomorph rig's base (hares; Land animals in depth › Rigs and animations). */
public abstract class LagomorphEntity extends LandAnimal {
    protected LagomorphEntity(EntityType<? extends LagomorphEntity> type, Level level) { super(type, level); }
}
