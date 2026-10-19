package com.yearofice.ecosystem.ai;

import com.yearofice.sim.ecosystem.Posture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * What the senses look for in the slice: players (predators join in M6). Each source's ground speed and heading
 * are sampled at most every 5 ticks from its position, so a walking, running, crouching or still player reads
 * the same however it moved. GameTests add their detached test players through {@link #addForTest}.
 */
public final class ThreatSources {
    private ThreatSources() {}

    public record Motion(double speedMps, Vec3 heading) {}

    private record Sample(Vec3 pos, long tick, Motion motion) {}

    private static final Map<LivingEntity, Sample> SAMPLES = Collections.synchronizedMap(new WeakHashMap<>());
    /** Test threats and the only animals each one is a threat to (GameTests run side by side). */
    private static final Map<LivingEntity, Set<Entity>> EXTRA = Collections.synchronizedMap(new WeakHashMap<>());

    public static void addForTest(LivingEntity e, Entity... only) {
        Set<Entity> set = Collections.newSetFromMap(new WeakHashMap<>());
        Collections.addAll(set, only);
        EXTRA.put(e, set);
    }

    public static void removeForTest(LivingEntity e) {
        EXTRA.remove(e);
        SAMPLES.remove(e);
    }

    public static List<LivingEntity> near(ServerLevel level, Vec3 at, double range) { return near(level, at, range, null); }

    /** Players, and test threats meant for {@code animal}, within range. */
    public static List<LivingEntity> near(ServerLevel level, Vec3 at, double range, Entity animal) {
        List<LivingEntity> out = new ArrayList<>();
        double r2 = range * range;
        for (Player p : level.players()) {
            if (!p.isSpectator() && p.isAlive() && p.position().distanceToSqr(at) <= r2) out.add(p);
        }
        synchronized (EXTRA) {
            for (Map.Entry<LivingEntity, Set<Entity>> en : EXTRA.entrySet()) {
                LivingEntity e = en.getKey();
                if (animal != null && !en.getValue().isEmpty() && !en.getValue().contains(animal)) continue;
                if (e.level() == level && e.isAlive() && e.position().distanceToSqr(at) <= r2) out.add(e);
            }
        }
        return out;
    }

    public static Motion motion(LivingEntity e, long gameTime) {
        Sample s = SAMPLES.get(e);
        Vec3 pos = e.position();
        if (s == null) {
            Sample n = new Sample(pos, gameTime, new Motion(0, Vec3.ZERO));
            SAMPLES.put(e, n);
            return n.motion();
        }
        long dt = gameTime - s.tick();
        if (dt < 5) return s.motion();
        Vec3 d = new Vec3(pos.x - s.pos().x, 0, pos.z - s.pos().z);
        double dist = d.length();
        Motion m = dt > 40 ? new Motion(0, Vec3.ZERO) : new Motion(dist / (dt / 20.0), dist > 1e-6 ? d.scale(1 / dist) : Vec3.ZERO);
        SAMPLES.put(e, new Sample(pos, gameTime, m));
        return m;
    }

    public static Posture posture(LivingEntity e, long gameTime) {
        Motion m = motion(e, gameTime);
        return Posture.of(m.speedMps(), e.isCrouching(), e.isSprinting());
    }

    /** Cosine between the source's heading and the line from it to {@code target}: 1 is straight at it. */
    public static double approachCos(LivingEntity e, Vec3 target, long gameTime) {
        Motion m = motion(e, gameTime);
        Vec3 to = new Vec3(target.x - e.getX(), 0, target.z - e.getZ());
        double len = to.length();
        if (len < 1e-6 || m.heading().lengthSqr() < 1e-6) return 0;
        return m.heading().dot(to.scale(1 / len));
    }
}
