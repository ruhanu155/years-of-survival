package com.yearofice.ecosystem;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Birds (The entity class tree). The slice's ground birds walk on vanilla navigation and fly a scripted flush;
 * the 11-state {@code FlightController}, perches and flocks land in M5–M6.
 */
public abstract class YoiBird extends YoiAnimal {
    protected YoiBird(EntityType<? extends YoiBird> type, Level level) { super(type, level); }
}
