package com.yearofice.body;

import com.yearofice.body.env.VanillaEnvSampler;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.body.ColdWater;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Step 1's activity class: the highest seen in the ticks since the last body step, in the Blueprint's order
 * sleep, rest, walk, heavy work, swim, sprint (Minecraft development › The body tick). Swinging a hand counts as
 * work until {@code build.work.WorkService} reports real jobs.
 */
public final class ActivitySampler {

    private static final class Window {
        Activity max = null;
        boolean slept = false;
        int ticks = 0, sleepTicks = 0;
        Activity awakeMax = null;
        boolean moved = false;
        Vec3 lastPos = null;
    }

    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    /** The rank used for "highest": sleep < rest < walk < heavy < swim < run. */
    public static int rank(Activity a) {
        return switch (a) {
            case SLEEP -> 0;
            case REST -> 1;
            case WALK -> 2;
            case HEAVY -> 3;
            case SWIM -> 4;
            case RUN -> 5;
        };
    }

    public void sample(ServerPlayer p) {
        Window w = windows.computeIfAbsent(p.getUUID(), k -> new Window());
        Vec3 pos = p.position();
        double dxz = w.lastPos == null ? 0 : Math.hypot(pos.x - w.lastPos.x, pos.z - w.lastPos.z);
        w.lastPos = pos;
        Activity a;
        if (p.isSleeping()) {
            w.slept = true;
            w.sleepTicks++;
            a = Activity.SLEEP;
        } else if (p.isSprinting()) {
            a = Activity.RUN;
        } else if (VanillaEnvSampler.depth(p) == ColdWater.Depth.SWIM) {
            a = Activity.SWIM;
        } else if (p.swinging) {
            a = Activity.HEAVY;
        } else if (dxz > 0.02 && !p.isPassenger()) {
            a = Activity.WALK;
        } else {
            a = Activity.REST;
        }
        w.ticks++;
        if (a != Activity.SLEEP && (w.awakeMax == null || rank(a) > rank(w.awakeMax))) w.awakeMax = a;
        if (dxz > 0.02) w.moved = true;
        if (w.max == null || rank(a) > rank(w.max)) w.max = a;
    }

    /** The window's result, and a fresh window. */
    public record Result(Activity activity, boolean asleep, boolean moving, int sleepTicks, int ticks, Activity awakeActivity) {
        public Result(Activity activity, boolean asleep, boolean moving) { this(activity, asleep, moving, asleep ? 1 : 0, 1, Activity.REST); }
    }

    public Result take(ServerPlayer p) {
        Window w = windows.get(p.getUUID());
        if (w == null || w.max == null) return new Result(p.isSleeping() ? Activity.SLEEP : Activity.REST, p.isSleeping(), false);
        Activity a = w.max;
        boolean asleep = w.slept && rank(a) <= rank(Activity.REST);
        Result r = new Result(asleep ? Activity.SLEEP : (a == Activity.SLEEP ? Activity.REST : a), asleep, w.moved,
                w.sleepTicks, w.ticks, w.awakeMax == null ? Activity.REST : w.awakeMax);
        w.ticks = 0;
        w.sleepTicks = 0;
        w.awakeMax = null;
        w.max = null;
        w.slept = false;
        w.moved = false;
        return r;
    }

    public void forget(UUID id) { windows.remove(id); }
}
