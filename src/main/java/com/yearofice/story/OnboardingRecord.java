package com.yearofice.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.sim.story.FirstNight;
import com.yearofice.sim.story.FirstNightStep;
import com.yearofice.sim.story.WarnOnce;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code yearofice:onboarding} attachment: one per player, written only by the story package. It holds the
 * guided night's state machine, the warn-once memory, the start site the night began at and the ring the player
 * set. Saved with snake_case keys at schema 1; kept across death, so a death never restarts the prompts.
 */
public final class OnboardingRecord {
    public static final int SCHEMA = 1;

    FirstNight night = new FirstNight();
    final WarnOnce warn = new WarnOnce();
    @Nullable BlockPos startSite;
    @Nullable BlockPos ring;

    public FirstNight night() { return night; }
    public WarnOnce warn() { return warn; }
    public @Nullable BlockPos startSite() { return startSite; }
    public @Nullable BlockPos ring() { return ring; }

    private record Warned(String id, boolean fired, long falseSince) {
        static final Codec<Warned> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Warned::id),
                Codec.BOOL.fieldOf("fired").forGetter(Warned::fired),
                Codec.LONG.optionalFieldOf("false_since", -1L).forGetter(Warned::falseSince)
        ).apply(i, Warned::new));
    }

    private record Saved(int schema, String status, String step, long startMinute, long stepMinute, long fireLitMinute,
                         long endMinute, String ending, Optional<BlockPos> startSite, Optional<BlockPos> ring,
                         List<Warned> warned) {}

    private static final Codec<Saved> SAVED = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema", SCHEMA).forGetter(Saved::schema),
            Codec.STRING.optionalFieldOf("status", "new").forGetter(Saved::status),
            Codec.STRING.optionalFieldOf("step", FirstNightStep.TINDER.id).forGetter(Saved::step),
            Codec.LONG.optionalFieldOf("start_minute", -1L).forGetter(Saved::startMinute),
            Codec.LONG.optionalFieldOf("step_minute", -1L).forGetter(Saved::stepMinute),
            Codec.LONG.optionalFieldOf("fire_lit_minute", -1L).forGetter(Saved::fireLitMinute),
            Codec.LONG.optionalFieldOf("end_minute", -1L).forGetter(Saved::endMinute),
            Codec.STRING.optionalFieldOf("ending", "none").forGetter(Saved::ending),
            BlockPos.CODEC.optionalFieldOf("start_site").forGetter(Saved::startSite),
            BlockPos.CODEC.optionalFieldOf("ring").forGetter(Saved::ring),
            Warned.CODEC.listOf().optionalFieldOf("warned", List.of()).forGetter(Saved::warned)
    ).apply(i, Saved::new));

    public static final Codec<OnboardingRecord> CODEC = SAVED.xmap(OnboardingRecord::fromSaved, OnboardingRecord::toSaved);

    private static OnboardingRecord fromSaved(Saved s) {
        OnboardingRecord r = new OnboardingRecord();
        r.night = FirstNight.restore(FirstNight.Status.valueOf(s.status().toUpperCase(Locale.ROOT)),
                FirstNightStep.byId(s.step()), s.startMinute(), s.stepMinute(), s.fireLitMinute(), s.endMinute(),
                FirstNight.Ending.valueOf(s.ending().toUpperCase(Locale.ROOT)));
        r.startSite = s.startSite().orElse(null);
        r.ring = s.ring().orElse(null);
        Map<String, WarnOnce.Entry> w = new TreeMap<>();
        for (Warned x : s.warned()) w.put(x.id(), new WarnOnce.Entry(x.fired(), x.falseSince()));
        r.warn.restore(w);
        return r;
    }

    private static Saved toSaved(OnboardingRecord r) {
        FirstNight n = r.night;
        List<Warned> w = new ArrayList<>();
        r.warn.entries().forEach((id, e) -> w.add(new Warned(id, e.fired(), e.falseSince())));
        return new Saved(SCHEMA, n.status().name().toLowerCase(Locale.ROOT), n.step().id, n.startMinute(),
                n.stepMinute(), n.fireLitMinute(), n.endMinute(), n.ending().name().toLowerCase(Locale.ROOT),
                Optional.ofNullable(r.startSite), Optional.ofNullable(r.ring), w);
    }
}
