package com.yearofice.hud.fieldnote;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** {@code yearofice:field_notes}: every card the player has triggered (id to game day) and which are still unseen. */
public final class FieldNoteRecord {
    public final Map<String, Integer> filed = new LinkedHashMap<>();
    public final Set<String> unseen = new LinkedHashSet<>();

    public static final Codec<FieldNoteRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("filed", Map.of()).forGetter(r -> r.filed),
            Codec.STRING.listOf().optionalFieldOf("unseen", List.of()).forGetter(r -> new ArrayList<>(r.unseen))
    ).apply(i, (f, u) -> {
        FieldNoteRecord r = new FieldNoteRecord();
        r.filed.putAll(f);
        r.unseen.addAll(u);
        return r;
    }));
}
