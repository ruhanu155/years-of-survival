package com.yearofice.client.clock;

import com.yearofice.api.clock.ClientTime;
import com.yearofice.core.YearOfIce;
import com.yearofice.net.YoiNet;
import com.yearofice.net.payload.ClockSyncPayload;
import com.yearofice.net.payload.EnvSyncPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The client's copy of the clock and its own environment sample: the last {@code clock_sync}, predicted forward by
 * its ds a tick between syncs, and the last {@code env_sync}. Client code reads time and weather only here (or
 * through {@link ClientTime} from common code).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class ClientClock {
    private ClientClock() {}

    private static volatile @Nullable ClockSyncPayload clock;
    private static volatile @Nullable EnvSyncPayload env;
    private static volatile long predictedDs;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        YoiNet.clockSink = ClientClock::accept;
        YoiNet.envSink = ClientClock::acceptEnv;
        ClientTime.nowDs = ClientClock::nowDs;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        ClockSyncPayload c = clock;
        if (c != null) predictedDs += c.dsPerTick();
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut e) {
        clock = null;
        env = null;
        predictedDs = 0;
    }

    public static void accept(ClockSyncPayload p) {
        clock = p;
        predictedDs = p.gameDs();
    }

    public static void acceptEnv(EnvSyncPayload p) { env = p; }

    public static long nowDs() { return predictedDs; }

    public static @Nullable ClockSyncPayload last() { return clock; }

    public static @Nullable EnvSyncPayload env() { return env; }
}
