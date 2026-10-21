package com.yearofice.ecosystem.small;

import com.yearofice.ecosystem.carcass.CarcassEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A spawned-out salmon (Waters and fish › Aquatic events): a carcass that floats, drifts with the current and feeds the valley's scavengers. */
public class SalmonCarcassEntity extends CarcassEntity {
    public SalmonCarcassEntity(EntityType<? extends SalmonCarcassEntity> type, Level level) { super(type, level); }

    /** A spent body rots away after ten game days; the scavengers take it sooner. */
    static final long LIFE_MIN = 10L * com.yearofice.sim.time.Calendar.MINUTES_PER_DAY;

    @Override
    public void tick() {
        if (!level().isClientSide && tickCount % 200 == 0
                && com.yearofice.ecosystem.clock.SliceClock.minute(level()) - record().deathMinute() > LIFE_MIN) {
            discard();
            return;
        }
        if (!isInWater()) {
            super.tick();
            return;
        }
        baseTick();
        Vec3 v = getDeltaMovement().add(0, 0.03, 0).multiply(0.8, 0.6, 0.8);
        move(MoverType.SELF, v);
        setDeltaMovement(v);
    }
}
