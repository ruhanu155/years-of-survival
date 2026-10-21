package com.yearofice.ecosystem.small;

import com.yearofice.api.env.EnvService;
import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.spawn.Habitats;
import com.yearofice.sim.life.Bugs;
import com.yearofice.sim.life.LifeTuning;
import com.yearofice.sim.time.Calendar;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Places and removes the biting-insect swarms (Blueprint › Bugs: B = date x habitat x time x wind x temperature).
 * Every second, for each player, the kind with the highest pressure at the player's cell gets a swarm 3 to 7 m away
 * while B is 0.3 or more, up to the cap. {@code /yoi insects on|off|auto} pins swarms always on, always off, or back to
 * the calendar. Original Year of Ice code.
 */
public final class InsectService {
    private InsectService() {}

    public enum Mode { AUTO, ON, OFF }

    public static volatile Mode mode = Mode.AUTO;
    /** Bites since the server started, for {@code /yoi insects} and the GameTests. */
    public static volatile long bitesTotal;

    public static void clear() {
        mode = Mode.AUTO;
        bitesTotal = 0;
    }

    /** A fire within 6 m keeps every swarm off (a smoky fire against the mosquitoes). */
    public static boolean smoked(ServerLevel level, Vec3 at) {
        for (var s : FireHeatLookup.INSTANCE.sourcesNear(level, at.x, at.y, at.z)) if (s.distanceM() <= 6) return true;
        return false;
    }

    static String habitatAt(ServerLevel level, BlockPos pos) {
        BlockPos g = Habitats.surface(level, pos.getX(), pos.getZ());
        if (Habitats.is(level, g, "pond_edge")) return "pond_edge";
        if (Habitats.is(level, g, "bank")) return "bank";
        if (Habitats.is(level, g, "forest")) return "forest";
        return "tundra";
    }

    /** Bug pressure of one kind at a place now; the pinned mode overrides the calendar. */
    public static double pressureAt(ServerLevel level, BlockPos pos, Bugs.Kind k) {
        if (mode == Mode.OFF) return 0;
        if (mode == Mode.ON) return 1.0;
        long minute = SliceClock.minute(level);
        int doy = Calendar.dayOfYearOfMinute(minute);
        double wind = 0, air = 12;
        if (EnvService.installed()) {
            EnvService env = EnvService.get();
            wind = env.wind(level, pos).ms();
            air = env.airC(level, pos);
        }
        return Bugs.pressure(k, doy, habitatAt(level, pos), Calendar.minuteOfDayOfMinute(minute), wind, air);
    }

    public static EntityType<InsectSwarmEntity> type(Bugs.Kind k) {
        return switch (k) {
            case MOSQUITO -> SmallLifeRegistry.MOSQUITO.get();
            case BLACK_FLY -> SmallLifeRegistry.BLACK_FLY.get();
            case NO_SEE_UM -> SmallLifeRegistry.NO_SEE_UM.get();
        };
    }

    public static List<InsectSwarmEntity> near(ServerLevel level, BlockPos at, double r) {
        return level.getEntitiesOfClass(InsectSwarmEntity.class, new AABB(at).inflate(r));
    }

    /** Places one swarm of {@code k} 3 to 7 m from the player; null when the level refuses. */
    public static InsectSwarmEntity place(ServerLevel level, ServerPlayer p, Bugs.Kind k) {
        InsectSwarmEntity e = type(k).create(level);
        if (e == null) return null;
        double ang = level.random.nextDouble() * Math.PI * 2, r = 3 + level.random.nextDouble() * 4;
        e.setKind(k);
        e.moveTo(p.getX() + Math.cos(ang) * r, p.getY() + 1.5, p.getZ() + Math.sin(ang) * r);
        return level.addFreshEntity(e) ? e : null;
    }

    public static void onTick(ServerTickEvent.Post ev) {
        if (ev.getServer().getTickCount() % 20 != 11) return;
        ServerLevel level = ev.getServer().overworld();
        LifeTuning t = LifeConfig.tuning();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || mode == Mode.OFF) continue;
            int have = near(level, p.blockPosition(), t.insectRangeM() + 4).size();
            if (have >= t.insectCap() || smoked(level, p.position())) continue;
            Bugs.Kind best = null;
            double bb = 0.3;
            for (Bugs.Kind k : Bugs.Kind.values()) {
                double b = pressureAt(level, p.blockPosition(), k);
                if (b >= bb) {
                    bb = b;
                    best = k;
                }
            }
            if (best != null) place(level, p, best);
        }
    }
}
