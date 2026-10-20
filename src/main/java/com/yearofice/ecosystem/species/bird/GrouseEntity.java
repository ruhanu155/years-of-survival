package com.yearofice.ecosystem.species.bird;

import com.yearofice.sim.ecosystem.SeasonalLooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Spruce grouse, ruffed grouse and rock ptarmigan on the grouse rig (Birds and the air › Grouse and ptarmigan): the
 * willow ptarmigan's walk, freeze and vertical flush, with each species' numbers from its species file. The two
 * grouse keep one plumage all year (their texture is by sex); the rock ptarmigan whitens and moults like the willow
 * ptarmigan.
 */
public class GrouseEntity extends WillowPtarmiganEntity {
    public GrouseEntity(EntityType<? extends GrouseEntity> type, Level level) { super(type, level); }

    @Override
    public String speciesId() { return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).toString(); }

    @Override
    protected void updateLooks(int doy) {
        if (speciesPath().equals("rock_ptarmigan")) setCoat(SeasonalLooks.ptarmigan(doy).ordinal());
    }
}
