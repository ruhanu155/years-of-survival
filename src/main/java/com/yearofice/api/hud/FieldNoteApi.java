package com.yearofice.api.hud;

import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Field-note cards (Creative concepts › Field-note cards): a system that owns a trigger the data cannot see by itself
 * (a first mineral lick, an ice-thickness read) calls {@link #trigger}. The note is the id of
 * {@code data/yearofice/field_note/<id>.json}. The HUD package files it, queues it and shows it when the rules allow.
 */
public final class FieldNoteApi {
    private FieldNoteApi() {}

    private static volatile BiConsumer<ServerPlayer, String> sink = (p, id) -> { };

    public static void install(BiConsumer<ServerPlayer, String> s) { sink = s; }

    /** Fire the note {@code noteId} (for example {@code yearofice:needle_tea}) for {@code player}. Safe to repeat. */
    public static void trigger(ServerPlayer player, String noteId) { sink.accept(player, noteId); }
}
