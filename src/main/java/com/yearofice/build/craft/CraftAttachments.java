package com.yearofice.build.craft;

import com.yearofice.core.YearOfIce;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The player attachments of crafting: {@code yearofice:practice} (reps per skill) and {@code yearofice:gates} (the
 * tier mask). Both survive death: practice survives an Endless-mode SOS and gates never close (Crafting and building
 * in depth › Practice, Tiers and gates). The only writers are {@link PracticeStore} and {@link GateStore}.
 */
public final class CraftAttachments {
    private CraftAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);

    public static final Supplier<AttachmentType<PracticeData>> PRACTICE = TYPES.register("practice",
            () -> AttachmentType.builder(() -> PracticeData.EMPTY).serialize(PracticeData.CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<GateData>> GATES = TYPES.register("gates",
            () -> AttachmentType.builder(() -> GateData.NEW).serialize(GateData.CODEC).copyOnDeath().build());

    static void register(IEventBus modBus) { TYPES.register(modBus); }
}
