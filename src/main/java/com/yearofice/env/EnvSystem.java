package com.yearofice.env;

import com.yearofice.api.env.EnvService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.net.YoiNet;
import com.yearofice.net.payload.EnvSyncPayload;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.weather.DayState;
import com.yearofice.sim.weather.WeatherModel;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Installs {@link EnvService} and runs the env layer's two jobs that are not samples:
 *
 * <ul>
 *   <li><b>Vanilla weather follows ours</b> (Vanilla systems we replace › Weather): {@code doWeatherCycle} is off,
 *       and each game minute the levels with a sky rain while the valley's hour has precipitation and thunder on a
 *       Severe day's rain, only so the sky darkens and the vanilla weather readers that remain agree.</li>
 *   <li><b>{@code env_sync}</b> each game minute to every player: their own sample.</li>
 * </ul>
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class EnvSystem {
    private EnvSystem() {}

    static final EnvServiceImpl ENV = new EnvServiceImpl();
    private static MinecraftServer server;
    private static final Map<ResourceKey<Level>, Integer> SHOWN = new HashMap<>();

    public static void init() { EnvService.install(ENV); }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent e) { e.addListener(new EnvResources()); }

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent e) {
        server = e.getServer();
        ENV.clear();
        SHOWN.clear();
        for (ServerLevel l : server.getAllLevels()) l.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        driveVanilla();
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        server = null;
        ENV.clear();
    }

    @SubscribeEvent
    public static void onMinute(ClockEvent.Minute e) {
        if (server == null) return;
        driveVanilla();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p);
    }

    @SubscribeEvent
    public static void onJump(ClockEvent.Jump e) {
        if (server == null) return;
        driveVanilla();
    }

    static void driveVanilla() {
        if (server == null) return;
        for (ServerLevel l : server.getAllLevels()) {
            if (!l.dimensionType().hasSkyLight()) continue;
            WeatherModel.Hour h = ENV.valley(l);
            boolean rain = h.precip() != Precip.NONE;
            boolean thunder = rain && h.state() == DayState.SEVERE && h.precip().liquid();
            int code = (rain ? 1 : 0) | (thunder ? 2 : 0);
            Integer was = SHOWN.put(l.dimension(), code);
            if (was != null && was == code && l.isRaining() == rain) continue;
            l.setWeatherParameters(rain ? 0 : 12_000, rain ? 12_000 : 0, rain, thunder);
        }
    }

    static void sync(ServerPlayer p) {
        EnvSample s = ENV.sample(p.serverLevel(), p.blockPosition());
        WeatherModel.Hour h = ENV.valley(p.serverLevel());
        YoiNet.send(p, new EnvSyncPayload((short) Math.round(s.airC() * 10), (short) Math.round(s.feelsLikeC() * 10),
                (short) Math.round(s.windMs() * 10), (short) Math.round(s.gustMs() * 10), (short) Math.round(s.windDirDeg()),
                (byte) h.state().ordinal(), (byte) s.precip().ordinal(), (short) Math.round(s.precipMmH() * 10),
                (short) Math.round(s.snowCm()), (int) Math.round(s.visibilityM()), (short) Math.round(s.waterC() * 10)));
    }
}
