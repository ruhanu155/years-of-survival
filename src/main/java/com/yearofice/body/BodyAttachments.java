package com.yearofice.body;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.BodyState;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * {@code yearofice:body}: one record per player, schema 2, saved through {@link BodyCodec#CODEC} and kept across
 * death ({@code copyOnDeath}); the SOS rules then set the return values in {@code BodyEvents.onClone}. Written
 * only by the body package.
 */
public final class BodyAttachments {
    private BodyAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);

    public static final Supplier<AttachmentType<BodyState>> BODY = TYPES.register("body",
            () -> AttachmentType.builder(() -> new BodyState()).serialize(BodyCodec.CODEC).copyOnDeath().build());

    public static BodyState of(Player p) { return p.getData(BODY); }

    public static void register(IEventBus modBus) { TYPES.register(modBus); }
}
