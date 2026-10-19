package com.yearofice.gametest;

import com.yearofice.core.YearOfIce;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GameTestTicker;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The GameTest server's wall-time report (RECONCILE 185: one GameTest at most 60 s, the whole run at most 10
 * minutes in CI). When a test finishes, its own run time ({@link GameTestInfo#getRunTime}, from its first tick to its
 * result) is logged as {@code [gametest-time] <ms> <name>}; over 30 s it is a warning, over the 60 s limit an error.
 * Active only on the GameTest server; nothing runs in play.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class GameTestTimings {
    private GameTestTimings() {}

    public static final long WARN_MS = 30_000, LIMIT_MS = 60_000;

    /** Tests seen running: the ticker drops a test in the tick it finishes, so the report holds its own references. */
    private static final Set<GameTestInfo> TRACKED = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static Field infos;
    private static boolean broken;

    @SuppressWarnings("unchecked")
    private static Collection<GameTestInfo> running() {
        if (broken) return java.util.List.of();
        try {
            if (infos == null) {
                infos = GameTestTicker.class.getDeclaredField("testInfos");
                infos.setAccessible(true);
            }
            return (Collection<GameTestInfo>) infos.get(GameTestTicker.SINGLETON);
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            YearOfIce.LOG.warn("[gametest-time] cannot read the GameTest ticker ({}); no timings this run", e.toString());
            return java.util.List.of();
        }
    }

    /** Before the ticker runs: a batch's tests join the ticker in one tick and first tick in the next. */
    @SubscribeEvent
    public static void onTickStart(ServerTickEvent.Pre e) {
        if (e.getServer() instanceof GameTestServer) TRACKED.addAll(running());
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (!(e.getServer() instanceof GameTestServer)) return;
        for (GameTestInfo t : java.util.List.copyOf(TRACKED)) {
            if (!t.isDone()) continue;
            TRACKED.remove(t);
            long ms = t.getRunTime();
            String line = String.format(java.util.Locale.ROOT, "[gametest-time] %6d ms %s%s", ms, t.getTestName(), t.hasFailed() ? " (failed)" : "");
            if (ms > LIMIT_MS) YearOfIce.LOG.error("{} over the {} s limit", line, LIMIT_MS / 1000);
            else if (ms > WARN_MS) YearOfIce.LOG.warn(line);
            else YearOfIce.LOG.info(line);
        }
    }
}
