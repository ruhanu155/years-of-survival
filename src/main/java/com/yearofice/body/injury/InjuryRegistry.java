package com.yearofice.body.injury;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound ids of the injury lane. The file for {@code body.back_twinge} is lane D's (look). */
public final class InjuryRegistry {
    private InjuryRegistry() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT,
            YearOfIce.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BACK_TWINGE = SOUNDS.register("body.back_twinge",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID,
                    "body.back_twinge")));

    public static void register(IEventBus modBus) { SOUNDS.register(modBus); }
}
