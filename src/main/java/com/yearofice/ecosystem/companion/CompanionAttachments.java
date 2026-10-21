package com.yearofice.ecosystem.companion;

import com.mojang.serialization.Codec;
import com.yearofice.core.YearOfIce;
import java.util.List;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** {@code trust} (wolf entity): the {@link com.yearofice.sim.companion.Trust} save array. Only WolfEntity writes it. */
public final class CompanionAttachments {
    private CompanionAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);

    public static final Supplier<AttachmentType<List<Double>>> TRUST = TYPES.register("trust",
            () -> AttachmentType.<List<Double>>builder(() -> List.<Double>of()).serialize(Codec.DOUBLE.listOf()).build());

    public static void register(IEventBus modBus) { TYPES.register(modBus); }
}
