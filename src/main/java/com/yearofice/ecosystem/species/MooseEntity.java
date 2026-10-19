package com.yearofice.ecosystem.species;

import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.species.rig.UngulateEntity;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.ecosystem.SeasonalLooks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * {@code yearofice:moose}, passive in the slice (Ready for development M2.5): walks, browses, turns Alert at 30
 * with ears swivelling, and walks off, then trots 200–400 m when a player closes inside 40 m or awareness reaches
 * 70. It never charges, and player damage does nothing until big game lands in M6 ({@code [ecosystem]
 * moose_huntable}). A bull's antlers follow the date.
 */
public class MooseEntity extends UngulateEntity {
    public static final String ID = "yearofice:moose";

    public MooseEntity(EntityType<? extends MooseEntity> type, Level level) { super(type, level); }

    @Override
    public String speciesId() { return ID; }

    @Override
    public boolean huntableNow() { return species().huntable() || EcoConfig.mooseHuntable(); }

    @Override
    protected void updateLooks(int doy) {
        setCoat(SeasonalLooks.Coat.SUMMER.ordinal());
        setAntler(male() ? SeasonalLooks.antlers(doy) : 0);
    }

    @Override
    protected double wanderRadius() { return 16; }

    @Override
    protected String animKey(AnimalState s, int move) {
        if (s == AnimalState.FLEE) return move == MOVE_RUN ? "run" : "walk";
        if (s == AnimalState.ROUTINE && move == MOVE_FEED) return "graze";
        return super.animKey(s, move);
    }
}
