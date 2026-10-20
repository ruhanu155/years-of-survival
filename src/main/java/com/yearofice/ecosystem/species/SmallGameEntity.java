package com.yearofice.ecosystem.species;

import com.yearofice.ecosystem.LandAnimal;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.ecosystem.SeasonalLooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The shared small-game and furbearer body (Land animals in depth › Mob sheets: small game and furbearers): red
 * squirrel, marten, ermine, mink, muskrat, beaver, porcupine and wood frog on the small-rodent, large-rodent and
 * small-mustelid rigs. Everything that differs between them is in {@code data/yearofice/species/<id>.json}: gaits,
 * senses, flee distances, habitat and carcass. Its registry id names the species; the generic
 * {@code yearofice:critter} body takes its species from data instead (a data-only species, Minecraft development ›
 * Data-only species), which is why the species is synced.
 */
public class SmallGameEntity extends LandAnimal {
    static final EntityDataAccessor<String> SPECIES = SynchedEntityData.defineId(SmallGameEntity.class, EntityDataSerializers.STRING);

    public SmallGameEntity(EntityType<? extends SmallGameEntity> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(SPECIES, "");
    }

    /** Gives a generic body its species (the {@code critter} type only). */
    public void setSpecies(String id) { entityData.set(SPECIES, id); }

    @Override
    public String speciesId() {
        String s = entityData.get(SPECIES);
        return s.isEmpty() ? BuiltInRegistries.ENTITY_TYPE.getKey(getType()).toString() : s;
    }

    @Override
    protected void updateLooks(int doy) {
        if (speciesPath().equals("ermine")) setCoat(SeasonalLooks.ermine(doy).ordinal());
    }

    @Override
    protected String animKey(AnimalState s, int move) {
        if (s == AnimalState.FREEZE) return "alert";
        return super.animKey(s, move);
    }
}
