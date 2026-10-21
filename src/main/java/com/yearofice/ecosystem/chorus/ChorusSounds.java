package com.yearofice.ecosystem.chorus;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The three original dawn-chorus beds (tools/soundgen/chorus.py): a few singers, the full chorus, and the white-night chorus. */
public final class ChorusSounds {
    private ChorusSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> SPARSE = reg("ambient.dawn_chorus_sparse");
    public static final DeferredHolder<SoundEvent, SoundEvent> FULL = reg("ambient.dawn_chorus_full");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHITE_NIGHT = reg("ambient.dawn_chorus_white_night");

    public static void register(IEventBus modBus) { SOUNDS.register(modBus); }
}
