package com.yearofice.story;

import com.yearofice.core.YearOfIce;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The story package's attachments: {@code yearofice:onboarding} for the guided first night. */
public final class StoryAttachments {
    private StoryAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);

    public static final Supplier<AttachmentType<OnboardingRecord>> ONBOARDING = TYPES.register("onboarding",
            () -> AttachmentType.builder(OnboardingRecord::new).serialize(OnboardingRecord.CODEC).copyOnDeath().build());

    public static OnboardingRecord of(Player p) { return p.getData(ONBOARDING); }

    static void register(IEventBus modBus) { TYPES.register(modBus); }
}
