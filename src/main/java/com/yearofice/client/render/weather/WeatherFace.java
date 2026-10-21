package com.yearofice.client.render.weather;

import com.yearofice.client.clock.ClientClock;
import com.yearofice.core.YearOfIce;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** The weather's face on the client: fog closes in to the synced visibility in snow, whiteout and heavy rain. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class WeatherFace {
    private WeatherFace() {}

    /** Visibility under this (m) pulls the fog in; clear air leaves vanilla fog alone. */
    static final double FOG_BELOW_M = 1500;

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog e) {
        var env = ClientClock.env();
        if (env == null || env.visibilityM() >= FOG_BELOW_M || env.visibilityM() <= 0) return;
        float far = (float) Math.max(10, env.visibilityM());
        e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), far));
        e.setNearPlaneDistance(Math.min(e.getNearPlaneDistance(), far * 0.1f));
        e.setCanceled(true);
    }
}
