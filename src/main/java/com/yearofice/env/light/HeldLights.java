package com.yearofice.env.light;

import com.yearofice.api.env.LightData;
import com.yearofice.api.env.LightState;
import com.yearofice.sim.env.HeldLightMath;
import com.yearofice.sim.env.HeldLightMath.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The lights players carry now, per player, refreshed every 10 ticks (Minecraft development › Gameplay light: "held
 * lights come from the data map {@code yearofice:light} for the item in either hand, refreshed per player every 10
 * ticks"). The headlamp slot joins when the head slot lands. {@code env.light.LightField} reads the table; sight
 * sensors and the light cone read that, never the item or the renderer.
 */
public final class HeldLights {
    private HeldLights() {}

    /** Below this air temperature a battery lamp's levels drop by 3 and its battery drains twice as fast. */
    public static final double COLD_C = -30;
    public static final int COLD_DROP = 3;
    /** Lamps with at least this many minutes of burn are battery lamps (the headlamp's 20 hours). */
    public static final int BATTERY_MIN = 600;

    private record Entry(ResourceKey<Level> dim, List<Source> sources) {}

    private static final Map<UUID, Entry> TABLE = new ConcurrentHashMap<>();

    /** The light a stack gives when carried, or null when it is not shining. */
    public static LightData shining(ItemStack stack) {
        if (stack.isEmpty()) return null;
        LightData d = stack.getItemHolder().getData(LightData.MAP);
        if (d == null) return null;
        if (!LightState.shining(stack, d)) return null;
        if (d.burnMin() > 0 && LightState.minutesLeft(stack, d) <= 0) return null;
        return d;
    }

    /** Recomputes one player's carried lights. */
    public static void refresh(ServerPlayer p, double airC) {
        List<Source> out = new ArrayList<>(2);
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(h);
            LightData d = shining(s);
            if (d == null) continue;
            int level = d.level();
            if (d.burnMin() >= BATTERY_MIN && airC < COLD_C) level = Math.max(0, level - COLD_DROP);
            out.add(new Source(eye.x, eye.y, eye.z, level, d.coneDeg(), look.x, look.y, look.z));
        }
        if (out.isEmpty()) TABLE.remove(p.getUUID());
        else TABLE.put(p.getUUID(), new Entry(p.level().dimension(), out));
    }

    public static void forget(UUID id) { TABLE.remove(id); }

    public static void clear() { TABLE.clear(); }

    /** Every carried light in the level that can reach {@code (x, y, z)}, nearest first, at most eight. */
    public static List<Source> near(ServerLevel level, double x, double y, double z) {
        List<Source> out = new ArrayList<>();
        for (Entry e : TABLE.values()) {
            if (e.dim() != level.dimension()) continue;
            for (Source s : e.sources()) {
                double dx = s.x() - x, dy = s.y() - y, dz = s.z() - z;
                if (dx * dx + dy * dy + dz * dz <= HeldLightMath.RANGE * HeldLightMath.RANGE) out.add(s);
            }
        }
        out.sort((a, b) -> Double.compare(d2(a, x, y, z), d2(b, x, y, z)));
        return out.size() <= HeldLightMath.MAX_SOURCES ? out : out.subList(0, HeldLightMath.MAX_SOURCES);
    }

    private static double d2(Source s, double x, double y, double z) {
        double dx = s.x() - x, dy = s.y() - y, dz = s.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
