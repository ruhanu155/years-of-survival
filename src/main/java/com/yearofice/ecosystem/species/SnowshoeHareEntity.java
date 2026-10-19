package com.yearofice.ecosystem.species;

import com.yearofice.ecosystem.species.rig.LagomorphEntity;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.ecosystem.SeasonalLooks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * {@code yearofice:snowshoe_hare} (Land animals in depth › Snowshoe hare): grazes in its runways, freezes at
 * awareness 30, bolts zigzag at 12 m/s inside its flush distance for 20–40 m, then hides; brown in summer and white
 * Oct – Apr over 20 days of mottled coat.
 */
public class SnowshoeHareEntity extends LagomorphEntity {
    public static final String ID = "yearofice:snowshoe_hare";

    public SnowshoeHareEntity(EntityType<? extends SnowshoeHareEntity> type, Level level) { super(type, level); }

    @Override
    public String speciesId() { return ID; }

    @Override
    protected void updateLooks(int doy) { setCoat(SeasonalLooks.hare(doy).ordinal()); }

    @Override
    protected String animKey(AnimalState s, int move) {
        if (s == AnimalState.FREEZE) return "alert";
        return super.animKey(s, move);
    }
}
