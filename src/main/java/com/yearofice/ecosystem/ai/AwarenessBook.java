package com.yearofice.ecosystem.ai;

import com.yearofice.sim.ecosystem.Awareness;
import com.yearofice.sim.ecosystem.Posture;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

/**
 * One animal's awareness, a score per source (Minecraft development › The shared state machine: "Awareness is the
 * only door between states"). Sensors add, {@link #decay} takes away when no sense has fired for a second, and
 * {@link #top} is the threat the state machine answers. Server only; not saved (a demoted animal forgets).
 */
public final class AwarenessBook {
    private final Map<UUID, Double> score = new HashMap<>();
    private final Map<UUID, LivingEntity> who = new HashMap<>();
    private long lastSenseTick = Long.MIN_VALUE;

    public void sense(LivingEntity src, double gainPerS, double stimulus, Posture p, double dtS, long tick) {
        UUID id = src.getUUID();
        score.put(id, Awareness.gain(score.getOrDefault(id, 0.0), gainPerS, stimulus, p, dtS));
        who.put(id, src);
        lastSenseTick = tick;
    }

    public void smell(LivingEntity src, double stimulus, Posture p, double dtS, long tick) {
        UUID id = src.getUUID();
        score.put(id, Awareness.smell(score.getOrDefault(id, 0.0), stimulus, p, dtS));
        who.put(id, src);
        lastSenseTick = tick;
    }

    /** A hit, a shot or a flush alarm: straight to the top. */
    public void spike(LivingEntity src, double to, long tick) {
        UUID id = src.getUUID();
        score.put(id, Math.max(score.getOrDefault(id, 0.0), Math.min(Awareness.MAX, to)));
        who.put(id, src);
        lastSenseTick = tick;
    }

    /** One tick of decay, when no sense has fired in the last second. */
    public void decay(long tick) {
        if (tick - lastSenseTick < 20) return;
        Iterator<Map.Entry<UUID, Double>> it = score.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Double> e = it.next();
            double v = Awareness.decay(e.getValue(), 0.05, false);
            if (v <= 0) {
                it.remove();
                who.remove(e.getKey());
            } else {
                e.setValue(v);
            }
        }
    }

    public LivingEntity top() {
        LivingEntity best = null;
        double b = -1;
        for (Map.Entry<UUID, Double> e : score.entrySet()) {
            LivingEntity w = who.get(e.getKey());
            if (w == null || !w.isAlive() || w.isRemoved()) continue;
            if (e.getValue() > b) {
                b = e.getValue();
                best = w;
            }
        }
        return best;
    }

    public double of(LivingEntity e) { return e == null ? 0 : score.getOrDefault(e.getUUID(), 0.0); }

    public double max() {
        double m = 0;
        for (double v : score.values()) m = Math.max(m, v);
        return m;
    }

    public void clear() {
        score.clear();
        who.clear();
    }
}
