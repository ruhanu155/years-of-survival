package com.yearofice.build.outpost;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The cabin tells' sounds (B8): {@code world.frost_drip} and {@code world.icicle_crack}, generated originals. */
public final class OutpostSounds {
    private OutpostSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> FROST_DRIP = SOUNDS.register("world.frost_drip",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "world.frost_drip")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ICICLE_CRACK = SOUNDS.register("world.icicle_crack",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "world.icicle_crack")));
}
