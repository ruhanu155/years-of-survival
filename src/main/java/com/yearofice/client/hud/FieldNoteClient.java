package com.yearofice.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.yearofice.core.YearOfIce;
import com.yearofice.hud.fieldnote.FieldNoteOptions;
import com.yearofice.hud.fieldnote.FieldNotes;
import com.yearofice.hud.net.FieldNotePayload;
import com.yearofice.sound.YoiSounds;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Field-note cards, client half (Creative concepts › Field-note cards): shows the server's card for 8 real seconds
 * at the bottom left, J ({@code key.yearofice.dismiss_card}) closes it sooner. The client option
 * {@code field_note_cards} is On, First year only or Off; Off still files cards in the notebook (the server does that).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class FieldNoteClient {
    private FieldNoteClient() {}

    public static final long SHOW_MS = 8000;

    public static final KeyMapping DISMISS = new KeyMapping("key.yearofice.dismiss_card", InputConstants.Type.KEYSYM,
            InputConstants.KEY_J, "key.categories.yearofice");

    private static @Nullable String note;
    private static long untilMs;

    /** The note on screen now, or null. */
    public static @Nullable String current() { return Util.getMillis() < untilMs ? note : null; }

    public static long remainingMs() { return Math.max(0, untilMs - Util.getMillis()); }

    static void show(String id) {
        note = id;
        untilMs = Util.getMillis() + SHOW_MS;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(YoiSounds.FIELD_NOTE_CARD.get(), 1.0f));
    }

    public static void dismiss() { untilMs = 0; }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) { e.register(DISMISS); }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        FieldNotes.clientSink = (FieldNotePayload p) -> Minecraft.getInstance().execute(() -> {
            if (FieldNoteOptions.shows(FieldNoteOptions.MODE.get(), p.firstYear())) show(p.note());
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        while (DISMISS.consumeClick()) dismiss();
    }
}
