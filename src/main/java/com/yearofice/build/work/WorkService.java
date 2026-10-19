package com.yearofice.build.work;

import com.yearofice.api.clock.ClockService;
import com.yearofice.net.YoiNet;
import com.yearofice.sim.time.Rate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

/**
 * {@code build.work.WorkService}: the server half of hold-to-work. A {@code work_start} heartbeat starts or keeps a
 * job on its target; each server tick the job gains the Work rate's game deciseconds (5 game minutes a real second),
 * the player asks the clock for the Work rate (so the world time-lapses with the ring; in co-op the slowest player
 * rules, RECONCILE 115), and the arm swings so the body's activity sampler charges heavy work. A hold that stops
 * beating for 10 ticks, leaves reach or loses its tool ends the job; {@code work_progress} goes out every 5 ticks.
 *
 * <p>GameTest players are not in the player list: they never ask the clock for a rate, so parallel tests never move
 * the shared clock; tests drive jobs with {@link #runFor}.
 */
public final class WorkService {
    private WorkService() {}

    /** Ticks without a heartbeat that end a hold (the client beats every 5). */
    static final int BEAT_TIMEOUT = 10;
    static final int SYNC_EVERY = 5, SWING_EVERY = 8;

    /** Picks a job for a heartbeat's target, or null when there is none (it may post why). */
    public static volatile BiFunction<ServerPlayer, WorkStartPayload, WorkJob> resolver = (p, w) -> null;

    private static final class Active {
        final WorkJob job;
        final long target;
        long lastBeat;
        int ticks;

        Active(WorkJob job, long target, long now) {
            this.job = job;
            this.target = target;
            this.lastBeat = now;
        }
    }

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();
    /** Targets whose job finished this hold: the same hold does not start them again until the key comes up. */
    private static final Map<UUID, Long> SPENT = new HashMap<>();

    /** Game deciseconds of work a tick: the Work rate's. */
    public static long dsPerTick() { return ClockService.installed() ? ClockService.get().dsPerTick(Rate.WORK) : 150; }

    /** A heartbeat from the client. */
    public static synchronized void beat(ServerPlayer p, WorkStartPayload w) {
        long now = p.serverLevel().getGameTime();
        Active a = ACTIVE.get(p.getUUID());
        if (!w.holding()) {
            SPENT.remove(p.getUUID());
            if (a != null) stop(p, a);
            return;
        }
        long key = w.targetKey();
        if (a != null && a.target == key) {
            a.lastBeat = now;
            return;
        }
        if (a != null) stop(p, a);
        Long spent = SPENT.get(p.getUUID());
        if (spent != null && spent == key) return;
        WorkJob job = resolver.apply(p, w);
        if (job == null) return;
        ACTIVE.put(p.getUUID(), new Active(job, key, now));
    }

    public static synchronized @Nullable WorkJob current(ServerPlayer p) {
        Active a = ACTIVE.get(p.getUUID());
        return a == null ? null : a.job;
    }

    private static void stop(ServerPlayer p, Active a) {
        ACTIVE.remove(p.getUUID());
        if (!a.job.finished()) a.job.stopped(p);
        YoiNet.send(p, WorkProgressPayload.idle());
    }

    /** One server tick of every held job. */
    public static synchronized void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        var it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Active a = e.getValue();
            if (p == null || p.isRemoved()) { it.remove(); continue; }
            long now = p.serverLevel().getGameTime();
            if (now - a.lastBeat > BEAT_TIMEOUT || !a.job.valid(p)) {
                it.remove();
                if (!a.job.finished()) a.job.stopped(p);
                YoiNet.send(p, WorkProgressPayload.idle());
                continue;
            }
            ClockService.get().requestRate(p, Rate.WORK, "hold-to-work");
            step(p, a.job, a.ticks++);
            if (a.job.finished()) {
                it.remove();
                SPENT.put(p.getUUID(), a.target);
                YoiNet.send(p, WorkProgressPayload.idle());
            } else if (a.ticks % SYNC_EVERY == 1) {
                YoiNet.send(p, progress(a.job));
            }
        }
    }

    static void step(ServerPlayer p, WorkJob job, int tick) {
        job.work(p, dsPerTick());
        if (tick % SWING_EVERY == 0) p.swing(InteractionHand.MAIN_HAND, true);
    }

    public static WorkProgressPayload progress(WorkJob j) {
        return new WorkProgressPayload(true, j.share(), (float) (j.sharePerDs() * dsPerTick()), (byte) j.phase().ordinal(),
                (short) j.done(), (short) j.of());
    }

    /**
     * Runs {@code job} for up to {@code maxTicks} ticks as if use were held, without the clock (GameTests). Returns the
     * game minutes of work it took, or -1 if it stopped being valid or never finished.
     */
    public static double runFor(ServerPlayer p, WorkJob job, int maxTicks) {
        for (int t = 0; t < maxTicks; t++) {
            if (!job.valid(p)) return -1;
            step(p, job, t);
            if (job.finished()) return (t + 1) * dsPerTick() / 600.0;
        }
        return -1;
    }

    /** Forgets everything (server stop). */
    public static synchronized void reset() {
        ACTIVE.clear();
        SPENT.clear();
    }
}
