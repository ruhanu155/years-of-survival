package com.yearofice.client.story;

import com.yearofice.core.YearOfIce;
import com.yearofice.story.net.StoryNet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** The client half of story: installs the {@code onboarding_sync} sink and forgets it on logout. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class StoryClient {
    private StoryClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        StoryNet.clientSink = ClientOnboarding::accept;
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientOnboarding.clear();
    }
}
