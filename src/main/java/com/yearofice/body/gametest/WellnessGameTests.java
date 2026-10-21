package com.yearofice.body.gametest;

import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyTicker;
import com.yearofice.body.env.EnvSampler;
import com.yearofice.core.YearOfIce;
import com.yearofice.gametest.TestFilter;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.body.BodyModel;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.ColdWater;
import com.yearofice.sim.body.IllnessModel;
import com.yearofice.sim.body.Skin;
import com.yearofice.sim.body.Healing;
import com.yearofice.sim.body.RestDay;
import com.yearofice.sim.body.WellnessTuning;
import com.yearofice.sim.body.part.PartModel;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.env.HeatSource;
import com.yearofice.sim.env.ShelterState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch {@code m4}, lane B (illness, metabolism, Rest days, skin and hair): the body is driven through the same
 * entry point the ticker uses ({@link BodyTicker#step}), a game day in a call, never real time. Standard preset.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class WellnessGameTests {
    private WellnessGameTests() {}

    static final String BATCH = "m4";
    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BodyTuning T = BodyTuning.STANDARD;
    static final WellnessTuning W = WellnessTuning.STANDARD;

    @GameTestGenerator
    public static Collection<TestFunction> wellnessTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:body/nausea_decay", WellnessGameTests::nauseaDecay));
        out.add(test("yearofice:body/rest_heals_double", WellnessGameTests::restHealsDouble));
        out.add(test("yearofice:body/face_grease_prevents_sunburn", WellnessGameTests::faceGrease));
        out.add(test("yearofice:body/beard_stages", WellnessGameTests::beardStages));
        out.add(test("yearofice:body/fever_sleep_slow", WellnessGameTests::feverSleepSlow));
        out.add(test("yearofice:body/rest_day_part_rates", WellnessGameTests::restDayPartRates));
        out.add(test("yearofice:body/frostbite_live", WellnessGameTests::frostbiteLive));
        out.add(test("yearofice:body/frost_stage_messages", WellnessGameTests::frostStageMessages));
        out.add(test("yearofice:body/cold_exposure_no_silent_transition", WellnessGameTests::coldNoSilentTransition));
        out.add(test("yearofice:body/ground_bed_camera_direction", WellnessGameTests::groundBedDirection));
        out.add(test("yearofice:data/migrate_schema3", WellnessGameTests::migrateSchema3));
        return TestFilter.apply(out);
    }

    static TestFunction test(String name, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, TEMPLATE, 100, 0, true, body);
    }

    /** A sampler that answers one fixed place: the air, the heat around, an open sky. */
    record Fixed(EnvSample env, List<HeatSource> heat) implements EnvSampler {
        @Override public Sample sample(ServerPlayer p, long minute) {
            return new Sample(env, heat, ShelterState.NONE, ColdWater.Depth.NONE);
        }
    }

    static void with(EnvSampler sampler, Consumer<EnvSampler> run) {
        EnvSampler before = BodyTicker.env;
        BodyTicker.env = sampler;
        try {
            run.accept(sampler);
        } finally {
            BodyTicker.env = before;
        }
    }

    static GameTestAssertException fail(String m) { return new GameTestAssertException(m); }

    static void feed(BodyState s, long m) {
        BodyModel.eat(s, T, m, 300, 900, 0, 0, 0, 0);
        BodyModel.drink(s, m, 1.0, -1);
    }

    // --- nausea ------------------------------------------------------------------------------------------------

    static void nauseaDecay(GameTestHelper h) {
        ServerPlayer p = BodyGameTests.player(h);
        BodyState s = BodyAttachments.of(p);
        with(new Fixed(EnvSample.outdoors(15, 0), List.of()), x -> {
            s.nausea = 80;
            BodyTicker.step(p, 600, 120, Activity.REST, false, false, T);
            h.assertTrue(Math.abs(s.nausea - 60) < 0.5, "awake 2 h: nausea " + s.nausea + ", expected 60 (-10 an hour)");
            BodyTicker.step(p, 720, 120, Activity.SLEEP, true, false, T);
            h.assertTrue(Math.abs(s.nausea - 30) < 0.5, "asleep 2 h: nausea " + s.nausea + ", expected 30 (-15 an hour)");
            // 100 vomits: half the last three hours' calories and half a litre, back to 20.
            feed(s, 840);
            double kcal = 0;
            for (double v : s.kcalIn) kcal += v;
            double kg = s.kg, water = s.water;
            s.nausea = 100;
            BodyTicker.step(p, 840, 1, Activity.REST, false, false, T);
            h.assertTrue(s.nausea >= 19 && s.nausea <= 20.1, "after a vomit nausea is " + s.nausea);
            double left = 0;
            for (double v : s.kcalIn) left += v;
            h.assertTrue(left < kcal * 0.6 && left > kcal * 0.4, "calories kept " + left + " of " + kcal);
            h.assertTrue(s.water < water - 10, "water " + water + " -> " + s.water);
            // The meal (900 kcal) is absorbed in the same step; the vomit takes half of it back out.
            h.assertTrue(s.kg < kg + 900 / T.kcalPerKg() - 0.1, "weight " + kg + " -> " + s.kg);
        });
        h.succeed();
    }

    // --- rest days ---------------------------------------------------------------------------------------------

    /** Condition gained over one game day for a hurt player, resting and sleeping, with or without a fire. */
    static double healedADay(GameTestHelper h, boolean fire) {
        ServerPlayer p = BodyGameTests.player(h);
        BodyState s = BodyAttachments.of(p);
        s.parts = PartModel.systemic(s.parts, 30);
        double before = s.parts.condition();
        List<HeatSource> heat = fire ? List.of(HeatSource.openFire(FireSetting.STEADY, 1.0)) : List.of();
        double[] gained = new double[1];
        with(new Fixed(EnvSample.outdoors(15, 0), heat), x -> {
            for (int hr = 0; hr < 24; hr++) {
                long m = hr * 60L;
                boolean night = hr < 6 || hr >= 22;
                s.warmth = 100;
                feed(s, m);
                BodyTicker.step(p, m, 60, night ? Activity.SLEEP : Activity.REST, night, false, T);
            }
            gained[0] = s.parts.condition() - before;
        });
        return gained[0];
    }

    static void restHealsDouble(GameTestHelper h) {
        double rest = healedADay(h, true);
        double away = healedADay(h, false);
        h.assertTrue(away > 0.5, "an ordinary day heals " + away);
        double ratio = rest / away;
        h.assertTrue(Math.abs(ratio - 2.0) < 0.1, "Rest day heals " + rest + " against " + away + ": x" + ratio + ", expected x2");
        h.succeed();
    }

    // --- skin --------------------------------------------------------------------------------------------------

    static EnvSample aprilSnow() {
        EnvSample e = EnvSample.outdoors(2, 1);
        return new EnvSample(e.airC(), e.feelsLikeC(), e.groundC(), e.waterC(), e.moisture(), e.windDirDeg(),
                e.windMs(), e.gustMs(), 40, false, 0, e.precip(), 0, 10_000, 15, 0, 0, 0, 10, 1013, 0, 60, "", 0, 0, 0);
    }

    static boolean burned(BodyState s) {
        return s.parts.problems().stream().anyMatch(p -> p.id().equals("sunburn"));
    }

    static void faceGrease(GameTestHelper h) {
        ServerPlayer bare = BodyGameTests.player(h), greased = BodyGameTests.player(h);
        BodyState sb = BodyAttachments.of(bare), sg = BodyAttachments.of(greased);
        with(new Fixed(aprilSnow(), List.of()), x -> {
            Skin.greaseFace(sg, 12 * 60, W);
            // 4 game hours on April snow at noon (day 100, sun 40 degrees). BodyTicker.skinHook stands in for the clock.
            BodyTicker.skinHook = st -> { st.ctxDayOfYear = 100; st.ctxSunDeg = 40; };
            try {
                for (int hr = 0; hr < 4; hr++) {
                    sb.warmth = sg.warmth = 100;
                    feed(sb, 12 * 60 + hr * 60L);
                    feed(sg, 12 * 60 + hr * 60L);
                    BodyTicker.step(bare, 12 * 60 + hr * 60L, 60, Activity.REST, false, false, T);
                    BodyTicker.step(greased, 12 * 60 + hr * 60L, 60, Activity.REST, false, false, T);
                }
            } finally {
                BodyTicker.skinHook = null;
            }
        });
        h.assertTrue(burned(sb), "a bare face should be Sunburned after 4 h on April snow");
        h.assertTrue(!burned(sg), "a greased face should not be");
        h.succeed();
    }

    /** Fever slows sleep recovery to x0.7 (RECONCILE 213): the same hour in bed restores 0.7 as many bolts. */
    static void feverSleepSlow(GameTestHelper h) {
        ServerPlayer well = BodyGameTests.player(h), sick = BodyGameTests.player(h);
        BodyState sw = BodyAttachments.of(well), ss = BodyAttachments.of(sick);
        with(new Fixed(EnvSample.outdoors(15, 0), List.of()), x -> {
            for (BodyState s : new BodyState[] {sw, ss}) { s.energy = 2; s.warmth = 100; s.water = 100; feed(s, 0); }
            com.yearofice.sim.body.IllnessModel.start(ss, "fever", 0, 1440);
            BodyTicker.step(well, 0, 60, Activity.SLEEP, true, false, T);
            BodyTicker.step(sick, 0, 60, Activity.SLEEP, true, false, T);
        });
        double gw = sw.energy - 2, gs = ss.energy - 2;
        h.assertTrue(gw > 0.5, "a well sleeper restores energy: " + gw);
        h.assertTrue(Math.abs(gs / gw - W.feverSleepMult()) < 0.02, "fever restores x0.7: " + gs + " / " + gw);
        h.succeed();
    }

    /** Rest-day rates by part: sprain x2, broken bone x1.5, the rest x1; Condition doubles on an unbroken Rest day. */
    static void restDayPartRates(GameTestHelper h) {
        h.assertTrue(RestDay.problemRate("sprain", W) == 2.0, "sprain x2");
        h.assertTrue(RestDay.problemRate("broken_bone", W) == 1.5, "break x1.5");
        h.assertTrue(RestDay.problemRate("cut", W) == 1.0, "other problems x1");
        ServerPlayer p = BodyGameTests.player(h);
        BodyState s = BodyAttachments.of(p);
        s.warmth = 100;
        var in = new com.yearofice.sim.body.BodyModel.Inputs(Activity.REST, false, 1, EnvSample.outdoors(15, 0), List.of(),
                com.yearofice.sim.env.ShelterState.NONE, com.yearofice.sim.body.ColdWater.Depth.NONE, false);
        double normal = Healing.rate(s, in, com.yearofice.sim.body.IllnessModel.Effects.NONE, false, W);
        double rest = Healing.rate(s, in, com.yearofice.sim.body.IllnessModel.Effects.NONE, true, W);
        h.assertTrue(Math.abs(rest - 2 * normal) < 1e-9, "Condition doubles on a Rest day: " + rest + " vs " + normal);
        // End to end: a wrapped sprain (2 days) is over inside 25 hours of rest by a fire, not away from one; a splint's
        // clock is moved 1.5x as fast.
        ServerPlayer atFire = BodyGameTests.player(h), away = BodyGameTests.player(h);
        for (ServerPlayer q : new ServerPlayer[] {atFire, away}) {
            BodyState qs = BodyAttachments.of(q);
            qs.parts = com.yearofice.sim.body.injury.Treatments.add(qs.parts,
                    com.yearofice.sim.body.part.Problem.fresh("sprain", com.yearofice.sim.body.part.Part.LEFT_LEG, "", 1, 5000).wrapped(5000));
        }
        for (ServerPlayer q : new ServerPlayer[] {atFire, away}) {
            List<HeatSource> heat = q == atFire ? List.of(HeatSource.openFire(FireSetting.STEADY, 1.0)) : List.of();
            with(new Fixed(EnvSample.outdoors(15, 0), heat), x -> {
                BodyState qs = BodyAttachments.of(q);
                for (int hr = 0; hr < 25; hr++) {
                    qs.warmth = 100;
                    feed(qs, 5000 + hr * 60L);
                    BodyTicker.step(q, 5000 + hr * 60L, 60, Activity.REST, false, false, T);
                }
            });
        }
        h.assertTrue(BodyAttachments.of(atFire).parts.problems().isEmpty(), "the sprain healed in 25 h on a Rest day");
        h.assertTrue(BodyAttachments.of(away).parts.problems().size() == 1, "and is still there away from a fire");
        var bone = com.yearofice.sim.body.part.Problem.fresh("broken_bone", com.yearofice.sim.body.part.Part.LEFT_LEG, "", 1, 0).splinted(1000);
        var shifted = com.yearofice.body.injury.InjuryTick.restRates(
                com.yearofice.sim.body.part.BodyRecord.healthy().withProblems(List.of(bone)), 600, W);
        h.assertTrue(shifted.problems().get(0).splint() == 1000 - 300, "a splint runs 1.5x: moved 300 earlier over 600 min, got "
                + shifted.problems().get(0).splint());
        h.succeed();
    }

    /** Bare hands at -30 C: frostnip at 30 min through the live tick, a frostbite problem, an exposure number. */
    static void frostbiteLive(GameTestHelper h) {
        ServerPlayer p = BodyGameTests.player(h);
        BodyState s = BodyAttachments.of(p);
        com.yearofice.sim.body.part.Part hand = com.yearofice.sim.body.part.Part.LEFT_HAND;
        with(new Fixed(EnvSample.outdoors(-30, 0), List.of()), x -> {
            s.warmth = 100;
            feed(s, 0);
            BodyTicker.step(p, 0, 20, Activity.REST, false, false, T.withKit(0));
            h.assertTrue(s.parts.partFrost()[hand.ordinal()] > 0 && s.parts.partFrost()[hand.ordinal()] < 300,
                    "20 min at -30: frost building but short of frostnip, got " + s.parts.partFrost()[hand.ordinal()]);
            h.assertTrue(s.exposureMin >= 0, "the exposure number is solved, got " + s.exposureMin);
            BodyTicker.step(p, 20, 15, Activity.REST, false, false, T.withKit(0));
        });
        int f = s.parts.partFrost()[hand.ordinal()];
        h.assertTrue(com.yearofice.sim.body.FrostModel.stage(f) >= 1, "35 min bare-handed at -30 C: frostnip, got " + f);
        h.assertTrue(s.parts.problems().stream().anyMatch(q -> q.id().equals("frostbite") && q.part() == hand),
                "a frostbite problem is on the hand");
        // Warmed up, frostnip clears.
        with(new Fixed(EnvSample.outdoors(15, 0), List.of()), x -> {
            for (int k = 0; k < 2; k++) { s.warmth = 100; feed(s, 35 + k * 60L); BodyTicker.step(p, 35 + k * 60L, 60, Activity.REST, false, false, T); }
        });
        h.assertTrue(s.parts.partFrost()[hand.ordinal()] == 0, "frostnip clears within an hour of rewarming");
        h.succeed();
    }

    /** Frost keys posted to the player's screen, in order (the bus's recent-lines list is capped, so callers read it often). */
    private static List<String> frostLines(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        var bus = com.yearofice.api.hud.HudBus.get();
        java.util.stream.Stream.concat(bus.sent(p).stream(), bus.journal(p).stream()).forEach(l -> {
            int i = l.key().indexOf("body.frost_");
            if (i >= 0) out.add(l.key().substring(i + 5));
        });
        return out;
    }

    /** A hand frozen through every stage at -30 C with the start kit: one line per stage change, then rewarming says clear. */
    static void frostStageMessages(GameTestHelper h) {
        ServerPlayer p = BodyGameTests.player(h);
        BodyState s = BodyAttachments.of(p);
        com.yearofice.sim.body.part.Part hand = com.yearofice.sim.body.part.Part.LEFT_HAND;
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        with(new Fixed(EnvSample.outdoors(-30, 0), List.of()), x -> {
            for (int k = 0; k < 40; k++) {
                s.warmth = 100;
                feed(s, k * 15L);
                BodyTicker.step(p, k * 15L, 15, Activity.REST, false, false, T.withKit(0));
                seen.addAll(frostLines(p));
            }
        });
        h.assertTrue(com.yearofice.sim.body.FrostModel.stage(s.parts.partFrost()[hand.ordinal()]) == 3, "the bare hand reached deep frostbite");
        for (String k : new String[] {"frost_hands_nip", "frost_hands_superficial", "frost_hands_deep"})
            h.assertTrue(seen.contains(k), "missing Messenger line " + k + ", saw " + seen);
        h.succeed();
    }

    /** Two game hours of cold: every zone stage change (counted from the frost counters) has one line. */
    static void coldNoSilentTransition(GameTestHelper h) {
        ServerPlayer p = BodyGameTests.player(h);
        BodyState s = BodyAttachments.of(p);
        int[] lastStage = new int[com.yearofice.sim.body.Zone.values().length];
        java.util.Map<String, Integer> expected = new java.util.HashMap<>();
        java.util.Map<String, Integer> posted = new java.util.HashMap<>();
        with(new Fixed(EnvSample.outdoors(-30, 0), List.of()), x -> {
            for (int k = 0; k < 120; k++) {
                s.warmth = 100;
                feed(s, k);
                BodyTicker.step(p, k, 1, Activity.REST, false, false, T.withKit(0));
                int[] now = com.yearofice.sim.body.FrostStep.zoneStages(s.parts.partFrost());
                for (com.yearofice.sim.body.Zone z : com.yearofice.sim.body.Zone.values()) {
                    int i = z.ordinal();
                    for (int st = lastStage[i] + 1; st <= now[i]; st++)
                        expected.merge(com.yearofice.sim.body.FrostStep.eventKey(z, st - 1, st), 1, Integer::sum);
                    if (now[i] < lastStage[i]) expected.merge(com.yearofice.sim.body.FrostStep.eventKey(z, lastStage[i], now[i]), 1, Integer::sum);
                    lastStage[i] = now[i];
                }
                for (String key : frostLines(p)) posted.putIfAbsent(key, 1);
            }
        });
        h.assertTrue(!expected.isEmpty(), "two hours at -30 C froze something");
        for (String key : expected.keySet())
            h.assertTrue(posted.containsKey(key), "transition " + key + " happened with no Messenger line; posted " + posted.keySet());
        h.succeed();
    }

    /** A sleeper's camera asks the bed for its direction; the ground marker has no facing and used to crash the client. */
    static void groundBedDirection(GameTestHelper h) {
        var st = com.yearofice.body.LeftPack.GROUND_BED.get().defaultBlockState();
        net.minecraft.core.Direction d = st.getBedDirection(h.getLevel(), h.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)));
        h.assertTrue(d != null, "the ground bed answers a bed direction");
        h.succeed();
    }

    // --- beard -------------------------------------------------------------------------------------------------

    static void beardStages(GameTestHelper h) {
        ServerPlayer p = BodyGameTests.player(h);
        BodyState s = BodyAttachments.of(p);
        int[] expect = {0, 1, 2, 3, 4};
        with(new Fixed(EnvSample.outdoors(15, 0), List.of()), x -> {
            h.assertTrue(Skin.beardStage(s, W) == 0, "Stubble from day 0, got " + Skin.stageName(Skin.beardStage(s, W)));
            for (int day = 1; day <= 120; day++) {
                long m = (day - 1) * 1440L;
                for (int half = 0; half < 2; half++) {
                    s.warmth = 100;
                    s.kg = 77;
                    feed(s, m + half * 720L);
                    BodyTicker.step(p, m + half * 720L, 720, Activity.REST, half == 0 && false, false, T);
                }
                if (day % 30 == 0) {
                    int want = expect[day / 30];
                    h.assertTrue(Skin.beardStage(s, W) == want, "day " + day + ": stage " + Skin.stageName(Skin.beardStage(s, W))
                            + ", expected " + Skin.stageName(want));
                }
            }
        });
        // Back to Full (day 60), for the cold test.
        Skin.trim(s, 2, W);
        h.assertTrue(Skin.beardStage(s, W) == 2 && Skin.faceBonusC(s, 0, W) == 2.0, "Full beard: safe 2 C colder");
        h.assertTrue(new com.yearofice.body.net.BodyPublicPayload(1, com.yearofice.body.injury.BodyPublic.flags(s)).beard() == 2,
                "body_public carries the beard stage");
        long t0 = 200_000;
        with(new Fixed(EnvSample.outdoors(-30, 0), List.of()), x -> {
            s.warmth = 100;
            BodyTicker.step(p, t0, 60, Activity.REST, false, false, T);
        });
        h.assertTrue(s.beardIced, "a Full beard ices after an hour below -25 C");
        h.assertTrue(Skin.faceBonusC(s, t0 + 60, W) == 0.0, "iced: the face bonus is gone");
        with(new Fixed(EnvSample.outdoors(10, 0), List.of(HeatSource.openFire(FireSetting.STEADY, 1.0))), x -> {
            s.warmth = 100;
            BodyTicker.step(p, t0 + 60, 10, Activity.REST, false, false, T);
        });
        h.assertTrue(!s.beardIced, "10 minutes at a fire thaws it");
        h.assertTrue(Skin.faceBonusC(s, t0 + 70, W) == 2.0, "and the bonus is back");
        s.facialHair = false;
        h.assertTrue(Skin.beardStage(s, W) == -1, "setting off: no beard");
        h.succeed();
    }

    // --- migration ---------------------------------------------------------------------------------------------

    static void migrateSchema3(GameTestHelper h) {
        String root = System.getProperty("yearofice.fixtures");
        if (root == null) throw fail("system property yearofice.fixtures is not set; the Gradle gameTestServer run passes it");
        Path old = Path.of(root).resolve("0.3.0/attachments/body.snbt");
        Tag raw = snbt(old);
        h.assertTrue(((CompoundTag) raw).getInt("schema") == 2, "the 0.3.0 body fixture is schema 2");
        BodyState s = com.yearofice.body.BodyCodec.CODEC.parse(NbtOps.INSTANCE, raw).getOrThrow(GameTestAssertExceptionSupplier.INSTANCE);
        h.assertTrue(s.vitC == 30 && s.nausea == 0 && s.ill.isEmpty() && s.hairDays == 0 && s.facialHair,
                "schema 3 fields take their defaults on a schema 2 save");
        h.assertTrue(s.restRun == 0 && !s.beardIced && s.faceGreaseUntil == -1, "so do the Rest, skin and grease fields");
        h.assertTrue(Math.abs(s.kg - 71.5) < 0.01 && s.heightCm == 178, "and the old numbers are kept");
        ServerPlayer p = BodyGameTests.player(h);
        p.setData(BodyAttachments.BODY.get(), s);
        CompoundTag saved = p.serializeAttachments(h.getLevel().registryAccess());
        CompoundTag body = saved == null ? null : saved.getCompound("yearofice:body");
        h.assertTrue(body != null && body.getInt("schema") == 3, "it saves as schema 3: " + body);
        // The schema 3 fixture (0.4.0) loads with its values.
        Tag fresh = snbt(Path.of(root).resolve("0.4.0/attachments/body.snbt"));
        BodyState t = com.yearofice.body.BodyCodec.CODEC.parse(NbtOps.INSTANCE, fresh).getOrThrow(GameTestAssertExceptionSupplier.INSTANCE);
        h.assertTrue(t.vitC == 12.5 && t.nausea == 35 && t.restRun == 2 && t.hairDays == 45 && !t.facialHair,
                "0.4.0 body: " + t.vitC + " " + t.nausea + " " + t.restRun + " " + t.hairDays);
        h.assertTrue(t.ill.size() == 2 && IllnessModel.listed(t, "tapeworm") && IllnessModel.listed(t, "scurvy"), "the illness list loads");
        h.succeed();
    }

    static Tag snbt(Path file) {
        try {
            return TagParser.parseTag("{v:" + Files.readString(file, StandardCharsets.UTF_8).trim() + "}").get("v");
        } catch (IOException | com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw fail(file + ": " + e.getMessage());
        }
    }

    enum GameTestAssertExceptionSupplier implements java.util.function.Function<String, GameTestAssertException> {
        INSTANCE;
        @Override public GameTestAssertException apply(String m) { return fail(m); }
    }
}
