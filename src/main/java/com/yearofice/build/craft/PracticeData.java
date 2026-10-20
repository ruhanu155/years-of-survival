package com.yearofice.build.craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * The value of the {@code yearofice:practice} attachment (Crafting and building in depth › Practice): reps per
 * skill by id, what each gained on the game day {@code day}, and that day. Immutable; the codec starts at v1 with
 * every field defaulted. Practice never decays.
 */
public record PracticeData(Map<String, Double> reps, Map<String, Double> today, long day) {
    public static final PracticeData EMPTY = new PracticeData(Map.of(), Map.of(), -1);

    public static final Codec<PracticeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("reps", Map.of()).forGetter(PracticeData::reps),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("today", Map.of()).forGetter(PracticeData::today),
            Codec.LONG.optionalFieldOf("day", -1L).forGetter(PracticeData::day)
    ).apply(i, PracticeData::new));

    public double reps(String skill) { return reps.getOrDefault(skill, 0.0); }

    /** What {@code skill} gained on {@code onDay}: nothing if the record is from another day. */
    public double gainedToday(String skill, long onDay) { return day == onDay ? today.getOrDefault(skill, 0.0) : 0.0; }

    /** This record after {@code gained} reps in {@code skill} on {@code onDay}. */
    public PracticeData with(String skill, double gained, long onDay) {
        Map<String, Double> r = new HashMap<>(reps);
        Map<String, Double> t = day == onDay ? new HashMap<>(today) : new HashMap<>();
        r.merge(skill, gained, Double::sum);
        t.merge(skill, gained, Double::sum);
        return new PracticeData(Map.copyOf(r), Map.copyOf(t), onDay);
    }

    /** This record with {@code skill} set to {@code value} reps (debug and tests). */
    public PracticeData set(String skill, double value) {
        Map<String, Double> r = new HashMap<>(reps);
        r.put(skill, Math.max(0, value));
        return new PracticeData(Map.copyOf(r), today, day);
    }
}
