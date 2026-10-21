package com.yearofice.story;

import com.yearofice.story.debug.OnboardingCommands;
import com.yearofice.story.net.StoryNet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Wires the story package for the slice: the {@code yearofice:onboarding} attachment, the
 * {@code yearofice:onboarding_sync} payload, {@code /yoi onboarding} and config reloads. {@link Onboarding}
 * subscribes its own game listeners. Called once from the mod constructor.
 */
public final class StorySystem {
    private StorySystem() {}

    public static void init(IEventBus modBus) {
        StoryAttachments.register(modBus);
        com.yearofice.story.trophy.TrophyRegistry.register(modBus);
        modBus.addListener(StoryNet::register);
        modBus.addListener((ModConfigEvent.Loading e) -> StoryConfig.invalidate());
        modBus.addListener((ModConfigEvent.Reloading e) -> StoryConfig.invalidate());
        NeoForge.EVENT_BUS.addListener(OnboardingCommands::register);
        NeoForge.EVENT_BUS.addListener(FirstKills::onDeath);
    }
}
