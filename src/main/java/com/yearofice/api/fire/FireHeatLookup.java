package com.yearofice.api.fire;

import com.yearofice.sim.env.HeatSource;
import com.yearofice.sim.fire.FireHeat;
import java.util.List;
import net.minecraft.world.level.Level;

/**
 * How the warmth step reads fires without depending on the fire package (Minecraft engine › Fire › Heat): every lit
 * fire is in an in-memory heat index, rebuilt as rings load. {@link #INSTANCE} is set by the fire system at start-up;
 * the body team's {@code EnvService.heatBonusC} reads it and feeds the sources to {@code sim.env.HeatIndex}.
 */
public interface FireHeatLookup {
    /** Every warm fire within 8 m of the point, as heat sources at their distances. */
    List<HeatSource> sourcesNear(Level level, double x, double y, double z);

    /** Every fire in the level that gives heat, as plain records. */
    List<FireHeat> fires(Level level);

    /** The live lookup; an empty one until the fire system starts. */
    Holder INSTANCE = new Holder();

    final class Holder implements FireHeatLookup {
        private volatile FireHeatLookup delegate;

        public void set(FireHeatLookup d) { delegate = d; }

        @Override
        public List<HeatSource> sourcesNear(Level level, double x, double y, double z) {
            FireHeatLookup d = delegate;
            return d == null ? List.of() : d.sourcesNear(level, x, y, z);
        }

        @Override
        public List<FireHeat> fires(Level level) {
            FireHeatLookup d = delegate;
            return d == null ? List.of() : d.fires(level);
        }
    }
}
