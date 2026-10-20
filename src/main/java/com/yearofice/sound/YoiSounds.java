package com.yearofice.sound;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events owned by the look-and-story department: the field-note card sound and the three Spirits pieces. */
public final class YoiSounds {
    private YoiSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> FIELD_NOTE_CARD = reg("ui.field_note_card");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DAWN = reg("music.dawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DAY = reg("music.day");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_FIRE = reg("music.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BREATH_PANIC = reg("body.breath_panic");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIT_GRUNT = reg("body.hit_grunt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BITE_CRUNCH = reg("body.bite_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> PACK_TEAR = reg("item.pack_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> BACK_TWINGE = reg("body.back_twinge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKIN_STING = reg("body.skin_sting");

    public static void init(IEventBus modBus) { SOUNDS.register(modBus); }
}
