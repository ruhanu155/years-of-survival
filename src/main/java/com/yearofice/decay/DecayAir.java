package com.yearofice.decay;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.net.YoiNet;
import com.yearofice.net.payload.DecayHeadsPayload;
import com.yearofice.sim.decay.AirClock;
import com.yearofice.sim.decay.ColdCurve;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * The slice's one decay clock, {@code air:slice}, on the Environment field (Minecraft development › Layer 5, Clock
 * keys: {@code air:}): the valley floor's hourly air from {@link EnvService#valleyAt}, summed by {@link AirClock}
 * with the {@code decay/meat.json} curve. It replaces the M2.5 stand-in {@code [decay] stand_in_air_c}.
 *
 * <ul>
 *   <li>A level whose valley air is pinned ({@code /yoi env set}, GameTests) runs the whole clock at the pinned air,
 *       D = minutes / 60 · r(T), so D stays a function of time alone; clearing the pin returns to the drawn year.
 *       Pins are for debugging and tests: food read across a pin change moves by the difference.</li>
 *   <li>With no server or no Environment field (pure JUnit, the client's main menu) the clock runs at the 5 °C
 *       reference, rate 1.</li>
 *   <li>Clients get the head, {@code decay_heads} (D, its minute, its rate and the air), on login, whenever the
 *       rate changes (each new hour) and after a clock jump; they predict D linearly between heads, which is exact
 *       inside an hour (Layer 5, Sync).</li>
 * </ul>
 *
 * The year table is summed lazily from day 0 and cached until the next data reload or server start.
 */
public final class DecayAir {
    private DecayAir() {}

    private static volatile @Nullable AirClock clock;
    private static volatile long clockSeed;
    private static double sentD = Double.NaN, sentMinute, sentRate = Double.NaN;

    static void init() {
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> clear());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> clear());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, DecayAir::onMinute);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, DecayAir::onJump);
        NeoForge.EVENT_BUS.addListener(DecayAir::onLogin);
    }

    /** Drops the summed year: a data reload may change the curve or the climate. */
    static synchronized void clear() {
        clock = null;
        sentD = Double.NaN;
        sentRate = Double.NaN;
    }

    /** The level the slice clock reads: the server's overworld, where the valley's weather lives. */
    static @Nullable ServerLevel valleyLevel(@Nullable Level level) {
        MinecraftServer s = level instanceof ServerLevel sl ? sl.getServer() : ServerLifecycleHooks.getCurrentServer();
        return s == null ? null : s.overworld();
    }

    private static double rate(double airC) { return DecayCurves.meat().rate(airC); }

    /** The pinned valley air on {@code level}, or NaN when the year's draw runs. */
    static double pinnedAirC(ServerLevel level) {
        EnvService.WeatherOverride o = EnvService.get().override(level);
        return o == null ? Double.NaN : o.airC();
    }

    private static AirClock clock(ServerLevel level) {
        AirClock c = clock;
        if (c != null && clockSeed == level.getSeed()) return c;
        synchronized (DecayAir.class) {
            if (clock == null || clockSeed != level.getSeed()) {
                clock = new AirClock(h -> EnvService.get().valleyAt(level, h * 60).airC(), DecayAir::rate);
                clockSeed = level.getSeed();
            }
            return clock;
        }
    }

    /** Decay-hours on the slice clock at absolute game minute {@code minute}. */
    public static double dAt(@Nullable Level level, double minute) {
        ServerLevel l = valleyLevel(level);
        if (l == null || !EnvService.installed()) return minute / 60.0 * rate(ColdCurve.REFERENCE_C);
        double pin = pinnedAirC(l);
        if (!Double.isNaN(pin)) return minute / 60.0 * rate(pin);
        return clock(l).dAt(minute);
    }

    /** The slice clock's rate at {@code minute}: decay-hours per game hour. */
    public static double rateAt(@Nullable Level level, double minute) {
        ServerLevel l = valleyLevel(level);
        if (l == null || !EnvService.installed()) return rate(ColdCurve.REFERENCE_C);
        double pin = pinnedAirC(l);
        if (!Double.isNaN(pin)) return rate(pin);
        return clock(l).rateAt(minute);
    }

    /** The air the slice clock reads at {@code minute}, °C. */
    public static double airAt(@Nullable Level level, double minute) {
        ServerLevel l = valleyLevel(level);
        if (l == null || !EnvService.installed()) return ColdCurve.REFERENCE_C;
        double pin = pinnedAirC(l);
        if (!Double.isNaN(pin)) return pin;
        return clock(l).airAt(minute);
    }

    static double nowMinute() { return ClockService.installed() ? ClockService.get().nowDs() / 600.0 : 0; }

    // ---- the head for clients ---------------------------------------------------------------------------------

    static DecayHeadsPayload head(MinecraftServer server) {
        ServerLevel l = server.overworld();
        double m = nowMinute();
        return new DecayHeadsPayload((short) 0, dAt(l, m), m, rateAt(l, m), (float) airAt(l, m));
    }

    private static void onMinute(ClockEvent.Minute e) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.overworld() == null) return;
        DecayHeadsPayload h = head(server);
        boolean send;
        synchronized (DecayAir.class) {
            double predicted = sentD + (h.minute() - sentMinute) / 60.0 * sentRate;
            send = Double.isNaN(sentD) || h.ratePerHour() != sentRate || Math.abs(predicted - h.d()) > 1e-6;
            if (send) {
                sentD = h.d();
                sentMinute = h.minute();
                sentRate = h.ratePerHour();
            }
        }
        if (send) for (ServerPlayer p : server.getPlayerList().getPlayers()) YoiNet.send(p, h);
    }

    private static void onJump(ClockEvent.Jump e) {
        synchronized (DecayAir.class) {
            sentD = Double.NaN;
        }
        onMinute(null);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.server.overworld() != null) YoiNet.send(p, head(p.server));
    }

    // ---- the client's copy --------------------------------------------------------------------------------------

    private static volatile @Nullable DecayHeadsPayload clientHead;

    /** The client's sink for {@code decay_heads} (installed by {@link DecaySystem}). */
    public static void acceptHead(DecayHeadsPayload p) { if (p.clock() == 0) clientHead = p; }

    /** D now on the client: the last head, predicted forward at its rate; the reference rate before any head. */
    static double clientD(double minuteNow) {
        DecayHeadsPayload h = clientHead;
        if (h == null) return minuteNow / 60.0 * rate(ColdCurve.REFERENCE_C);
        return h.d() + (minuteNow - h.minute()) / 60.0 * h.ratePerHour();
    }

    static double clientRate() {
        DecayHeadsPayload h = clientHead;
        return h == null ? rate(ColdCurve.REFERENCE_C) : h.ratePerHour();
    }

    static double clientAirC() {
        DecayHeadsPayload h = clientHead;
        return h == null ? ColdCurve.REFERENCE_C : h.airC();
    }
}
