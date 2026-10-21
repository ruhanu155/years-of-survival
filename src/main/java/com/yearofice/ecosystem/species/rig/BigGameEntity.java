package com.yearofice.ecosystem.species.rig;

import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.ecosystem.SeasonalLooks;
import com.yearofice.sim.time.Calendar;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Caribou and mule deer (the ungulate and cervid rigs): the species comes from the registry key, everything else from
 * its file. A herd's alarm spreads: when one animal is Alert or fleeing, every herd-mate of its kind within 15 m
 * has its awareness of the same threat raised to the species' Alert level within about half a second (Caribou
 * herds: "a wave crosses the herd").
 */
public class BigGameEntity extends UngulateEntity {
    public static final double SPREAD_M = 15;

    public BigGameEntity(EntityType<? extends BigGameEntity> type, Level level) { super(type, level); }

    @Override
    public String speciesId() { return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).toString(); }

    @Override
    protected double wanderRadius() { return 32; }

    /** Herd-mates of the same kind within the spread distance, this animal excluded. */
    public List<BigGameEntity> herd() {
        return level().getEntitiesOfClass(BigGameEntity.class, new AABB(position(), position()).inflate(SPREAD_M),
                b -> b != this && b.isAlive() && java.util.Objects.equals(b.testGroup(), testGroup()) && b.speciesId().equals(speciesId()) && b.distanceToSqr(this) <= SPREAD_M * SPREAD_M);
    }

    @Override
    public void tickState(ServerLevel level, AnimalState s) {
        super.tickState(level, s);
        if ((s == AnimalState.ALERT || s == AnimalState.FLEE) && tickCount % 10 == 0) {
            var th = awareness().top();
            if (th != null) {
                double a = species().behavior().alertAt();
                for (BigGameEntity mate : herd()) if (mate.awareness().of(th) < a) mate.awareness().spike(th, a + 1, level.getGameTime());
            }
        }
    }

    @Override
    protected void updateLooks(int doy) {
        boolean winter = Calendar.within(doy, Calendar.dayOfYear(10, 15), Calendar.dayOfYear(5, 15));
        setCoat((winter ? SeasonalLooks.Coat.WINTER : SeasonalLooks.Coat.SUMMER).ordinal());
        setAntler(male() || speciesId().endsWith("caribou") ? SeasonalLooks.antlers(doy) : 0);
    }

    @Override
    protected String animKey(AnimalState s, int move) {
        if (s == AnimalState.FLEE) return move == MOVE_RUN ? "run" : "walk";
        if (s == AnimalState.ROUTINE && move == MOVE_FEED) return "graze";
        return super.animKey(s, move);
    }

}
