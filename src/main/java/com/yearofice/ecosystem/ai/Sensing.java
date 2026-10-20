package com.yearofice.ecosystem.ai;

import com.yearofice.api.env.EnvService;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.sim.ecosystem.Awareness;
import com.yearofice.sim.ecosystem.Posture;
import com.yearofice.sim.ecosystem.Senses;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The three senses as Brain sensors (Minecraft development › Brain: sensors): {@code yearofice:sight} every 5
 * ticks, {@code yearofice:hearing} every 10 and {@code yearofice:scent} every 20, each staggered by Minecraft's
 * random first tick. Each adds its share of awareness for the seconds its period covers; the core behaviour
 * decays awareness and picks the threat. No vanilla sensor runs on our animals.
 */
public final class Sensing {
    private Sensing() {}

    abstract static class Base extends Sensor<YoiAnimal> {
        final int period;

        Base(int period) {
            super(period);
            this.period = period;
        }

        double dtS() { return period / 20.0; }

        @Override
        public Set<MemoryModuleType<?>> requires() { return Set.of(EcosystemRegistry.THREAT.get(), EcosystemRegistry.LOOK_TARGET.get()); }
    }

    /** What a blind or stand does to this animal's senses of a player (ConcealmentService, installed by hunt). */
    static com.yearofice.sim.hunt.Blinds.Effect conceal(ServerLevel level, YoiAnimal a, LivingEntity t, boolean inFront) {
        if (!(t instanceof net.minecraft.server.level.ServerPlayer sp)) return com.yearofice.sim.hunt.Blinds.Effect.NONE;
        String c = a.species().carcassClass();
        boolean big = c.equals("large") || c.equals("huge");
        boolean bear = a.speciesId().endsWith("bear") || a.speciesId().endsWith("grizzly");
        return com.yearofice.api.hunt.ConcealmentService.get().effectFor(level, sp, t.distanceTo(a), inFront, big, bear);
    }

    /** Sight: range × posture × cover, inside the field of view, with line of sight through blocks. */
    public static final class Sight extends Base {
        public Sight() { super(5); }

        @Override
        protected void doTick(ServerLevel level, YoiAnimal a) {
            Senses s = a.species().senses();
            Vec3 eye = a.getEyePosition();
            for (LivingEntity t : ThreatSources.near(level, eye, s.sightM(), a)) {
                double cover = cover(level, eye, t.getEyePosition());
                if (cover <= 0) continue;
                if (!Awareness.inView(s.fovDeg(), a.getYHeadRot(), bearing(eye, t.position()))) continue;
                Posture p = ThreatSources.posture(t, level.getGameTime());
                double r = Awareness.sightRange(s, p, 1.0, cover) * conceal(level, a, t, true).sightX();
                double st = Awareness.stimulus(t.distanceTo(a), r);
                if (st > 0) a.awareness().sense(t, Awareness.GAIN_SIGHT, st, p, dtS(), level.getGameTime());
            }
        }
    }

    /** Hearing: footstep noise by posture; no line of sight needed. */
    public static final class Hearing extends Base {
        public Hearing() { super(10); }

        @Override
        protected void doTick(ServerLevel level, YoiAnimal a) {
            Senses s = a.species().senses();
            for (LivingEntity t : ThreatSources.near(level, a.position(), s.hearingM() * Posture.RUN.noise, a)) {
                Posture p = ThreatSources.posture(t, level.getGameTime());
                double st = Awareness.stimulus(t.distanceTo(a), Awareness.hearingRange(s, p) * conceal(level, a, t, false).noiseX());
                if (st > 0) a.awareness().sense(t, Awareness.GAIN_HEARING, st, p, dtS(), level.getGameTime());
            }
        }
    }

    /**
     * Scent: the animal sits in the player's downwind cone (25° each side) within its smell range. The wind is the
     * Environment field's at the animal ({@code EnvService.wind}).
     */
    public static final class Scent extends Base {
        public static final double CONE_DEG = 25;

        public Scent() { super(20); }

        @Override
        protected void doTick(ServerLevel level, YoiAnimal a) {
            if (!a.scentEnabled()) return;
            Senses s = a.species().senses();
            double wind = windToward(level, a);
            for (LivingEntity t : ThreatSources.near(level, a.position(), s.smellM(), a)) {
                double toAnimal = bearing(t.position(), a.position());
                if (Math.abs(Mth.wrapDegrees(toAnimal - wind)) > CONE_DEG) continue;
                var fx = conceal(level, a, t, false);
                if (fx.scentClearM() > 0 && t.distanceTo(a) <= fx.scentClearM()) continue;
                Posture p = ThreatSources.posture(t, level.getGameTime());
                double st = Awareness.stimulus(t.distanceTo(a), s.smellM());
                if (st > 0) a.awareness().smell(t, st, p, dtS(), level.getGameTime());
            }
        }
    }

    /**
     * The yaw the wind at {@code pos} blows toward, in Minecraft degrees. The field gives the compass bearing it
     * blows from (clockwise from north); it blows toward that + 180°, and a compass bearing c is yaw c − 180°
     * (north is yaw 180, south 0), so the yaw toward is the bearing from.
     */
    public static double windTowardDeg(ServerLevel level, BlockPos pos) {
        return Mth.wrapDegrees(EnvService.get().wind(level, pos).fromDeg());
    }

    /** The yaw the wind blows toward at an animal: its test override, else the Environment field's. */
    public static double windToward(ServerLevel level, YoiAnimal a) {
        Double fixed = a.windTowardOverride();
        return fixed != null ? Mth.wrapDegrees(fixed) : windTowardDeg(level, a.blockPosition());
    }

    /** Minecraft yaw from one point toward another. */
    public static double bearing(Vec3 from, Vec3 to) {
        return Mth.atan2(to.z - from.z, to.x - from.x) * (180.0 / Math.PI) - 90.0;
    }

    /**
     * Line of sight in half-metre steps: 0 when a solid block is in the way, 0.5 through leaves (half cover,
     * once), 1 clear.
     */
    public static double cover(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        int steps = (int) Math.ceil(len / 0.5);
        double cover = 1.0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockPos last = null;
        for (int i = 1; i < steps; i++) {
            Vec3 p = from.add(d.scale(i / (double) steps));
            m.set(p.x, p.y, p.z);
            if (m.equals(last)) continue;
            last = m.immutable();
            BlockState st = level.getBlockState(m);
            if (st.isAir()) continue;
            if (st.is(BlockTags.LEAVES)) { cover = 0.5; continue; }
            if (st.canOcclude() && st.isCollisionShapeFullBlock(level, m)) return 0;
        }
        return cover;
    }
}
