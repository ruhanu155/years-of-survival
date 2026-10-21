package com.yearofice.fire;

import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.sim.env.HeatSource;
import com.yearofice.sim.fire.FireHeat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The loaded fire rings and the heat index (Minecraft engine › Fire › Heat): in memory, rebuilt as rings load,
 * never saved. Server thread only.
 */
public final class FireIndex implements FireHeatLookup {
    public static final FireIndex INSTANCE = new FireIndex();

    private final Map<ResourceKey<Level>, Map<BlockPos, FireHeat>> rings = new HashMap<>();

    /** Heat that is not a ring (stoves, M8): in the heat lookups, never in {@link #rings}. */
    private final Map<ResourceKey<Level>, Map<BlockPos, FireHeat>> extras = new HashMap<>();

    private FireIndex() {}

    public void putExtra(Level level, BlockPos pos, FireHeat heat) {
        extras.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.immutable(), heat);
    }

    public void removeExtra(Level level, BlockPos pos) {
        Map<BlockPos, FireHeat> m = extras.get(level.dimension());
        if (m != null) m.remove(pos);
    }

    void put(Level level, BlockPos pos, FireHeat heat) {
        rings.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.immutable(), heat);
    }

    void remove(Level level, BlockPos pos) {
        Map<BlockPos, FireHeat> m = rings.get(level.dimension());
        if (m != null) m.remove(pos);
    }

    void clear() { rings.clear(); extras.clear(); }

    /** Every loaded ring in the level, lit or not. */
    public List<BlockPos> rings(Level level) {
        return List.copyOf(rings.getOrDefault(level.dimension(), Map.of()).keySet());
    }

    /** The nearest loaded ring within {@code radius} blocks, or null. */
    public BlockPos nearest(Level level, double x, double y, double z, double radius) {
        BlockPos best = null;
        double bd = radius * radius;
        for (BlockPos p : rings.getOrDefault(level.dimension(), Map.of()).keySet()) {
            double d = p.distToCenterSqr(x, y, z);
            if (d <= bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    public List<HeatSource> sourcesNear(Level level, double x, double y, double z) {
        List<HeatSource> out = new ArrayList<>();
        for (var m : List.of(rings, extras)) for (FireHeat h : m.getOrDefault(level.dimension(), Map.of()).values()) {
            if (!h.warm()) continue;
            double d = h.distanceTo(x, y, z);
            if (d < FireHeat.ZERO_RADIUS_M) out.add(h.toHeatSource(d, 0));
        }
        return out;
    }

    @Override
    public List<FireHeat> fires(Level level) {
        List<FireHeat> out = new ArrayList<>();
        for (var m : List.of(rings, extras)) for (FireHeat h : m.getOrDefault(level.dimension(), Map.of()).values()) if (h.warm()) out.add(h);
        return out;
    }
}
