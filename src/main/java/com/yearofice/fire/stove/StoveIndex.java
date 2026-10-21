package com.yearofice.fire.stove;

import com.yearofice.api.fire.StoveHeatLookup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Every lit, loaded stove with its output in watts: what lane A's room heat model reads. Server thread only. */
public final class StoveIndex implements StoveHeatLookup {
    public static final StoveIndex INSTANCE = new StoveIndex();

    private final Map<ResourceKey<Level>, Map<BlockPos, StoveHeat>> lit = new HashMap<>();

    private StoveIndex() {}

    void set(Level level, BlockPos pos, double watts, boolean flued) {
        Map<BlockPos, StoveHeat> m = lit.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        if (watts > 0) m.put(pos.immutable(), new StoveHeat(pos.immutable(), watts, flued));
        else m.remove(pos);
        // lane A reads fires(level) with kW, and the body's warmth reads sourcesNear: a lit stove is a fire there too
        if (watts > 0) com.yearofice.fire.FireIndex.INSTANCE.putExtra(level, pos, new com.yearofice.sim.fire.FireHeat(pos.getX(), pos.getY(), pos.getZ(),
                com.yearofice.sim.fire.FireState.FIRE, com.yearofice.sim.env.FireSetting.STEADY, Math.min(8, watts / 1000.0 * 1.5), watts / 1000.0));
        else com.yearofice.fire.FireIndex.INSTANCE.removeExtra(level, pos);
    }

    void remove(Level level, BlockPos pos) { set(level, pos, 0, true); }

    public void clear() { lit.clear(); }

    @Override
    public List<StoveHeat> near(Level level, BlockPos pos, double radiusM) {
        List<StoveHeat> out = new ArrayList<>();
        for (StoveHeat h : lit.getOrDefault(level.dimension(), Map.of()).values()) {
            if (h.pos().distSqr(pos) <= radiusM * radiusM) out.add(h);
        }
        return out;
    }
}
