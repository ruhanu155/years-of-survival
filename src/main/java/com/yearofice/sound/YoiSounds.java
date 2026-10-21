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
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DAWN_SUMMER = reg("music.dawn_summer");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DAY_SUMMER = reg("music.day_summer");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_FIRE_SUMMER = reg("music.fire_summer");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DAWN_FALL = reg("music.dawn_fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DAY_FALL = reg("music.day_fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_FIRE_FALL = reg("music.fire_fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DAWN_WINTER = reg("music.dawn_winter");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DAY_WINTER = reg("music.day_winter");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_FIRE_WINTER = reg("music.fire_winter");

    /** The piece for a season (0 spring, 1 summer, 2 fall, 3 winter) and kind (0 dawn, 1 day, 2 fire). */
    public static SoundEvent music(int season, int kind) {
        return switch (season) {
            case 1 -> kind == 0 ? MUSIC_DAWN_SUMMER.get() : kind == 1 ? MUSIC_DAY_SUMMER.get() : MUSIC_FIRE_SUMMER.get();
            case 2 -> kind == 0 ? MUSIC_DAWN_FALL.get() : kind == 1 ? MUSIC_DAY_FALL.get() : MUSIC_FIRE_FALL.get();
            case 3 -> kind == 0 ? MUSIC_DAWN_WINTER.get() : kind == 1 ? MUSIC_DAY_WINTER.get() : MUSIC_FIRE_WINTER.get();
            default -> kind == 0 ? MUSIC_DAWN.get() : kind == 1 ? MUSIC_DAY.get() : MUSIC_FIRE.get();
        };
    }
    public static final DeferredHolder<SoundEvent, SoundEvent> BREATH_PANIC = reg("body.breath_panic");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIT_GRUNT = reg("body.hit_grunt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BITE_CRUNCH = reg("body.bite_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> PACK_TEAR = reg("item.pack_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> BACK_TWINGE = reg("body.back_twinge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKIN_STING = reg("body.skin_sting");

    // M6 lane D: encounter sounds (RECONCILE 171), bone crushing (B5) and the survivor's voice (B7).
    public static final DeferredHolder<SoundEvent, SoundEvent> GRIZZLY_JAW_POP = reg("entity.grizzly.jaw_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_BEAR_HUFF = reg("entity.black_bear.huff");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACK_BEAR_JAW_POP = reg("entity.black_bear.jaw_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_CRUSH = reg("work.bone_crush");
    public static final DeferredHolder<SoundEvent, SoundEvent> TALK_WALK = reg("body.talk_walk");

    public static void init(IEventBus modBus) { SOUNDS.register(modBus); }
}
