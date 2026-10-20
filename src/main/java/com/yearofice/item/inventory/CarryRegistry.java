package com.yearofice.item.inventory;

import com.mojang.serialization.Codec;
import com.yearofice.core.YearOfIce;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The lift-a-box entries (video 2, A7): the player attachment {@code yearofice:carry} (a lifted box; synced to its own
 * player only), the block-entity attachment {@code yearofice:box_stamp} (the game minute a box was last touched, for
 * freezing down) and the sounds {@code carry.box_lift} and {@code carry.box_set}.
 */
public final class CarryRegistry {
    private CarryRegistry() {}

    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);

    /** The load in the player's arms; it survives logout, and a death sets it down (BoxCarry). */
    public static final Supplier<AttachmentType<CarryData>> CARRY = TYPES.register("carry",
            () -> AttachmentType.builder(() -> CarryData.NONE).serialize(CarryData.CODEC)
                    .sync((holder, to) -> holder == to, ByteBufCodecs.fromCodecWithRegistries(CarryData.CODEC)).build());

    /** On a box's block entity: the Clock minute it was last opened, lifted, set down or chipped. */
    public static final Supplier<AttachmentType<Long>> BOX_STAMP = TYPES.register("box_stamp",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> BOX_LIFT = sound("carry.box_lift");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOX_SET = sound("carry.box_set");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, name)));
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SOUNDS.register(modBus);
    }
}
