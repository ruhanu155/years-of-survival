package com.yearofice.ecosystem;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Land mammals (The entity class tree): ground navigation and gaits. The slice runs vanilla ground navigation;
 * {@code YoiGroundNavigation}, path profiles and the trail graph land in M5–M6.
 */
public abstract class LandAnimal extends YoiAnimal {
    protected LandAnimal(EntityType<? extends LandAnimal> type, Level level) { super(type, level); }
}
