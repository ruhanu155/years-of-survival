package com.yearofice.gametest;

import com.mojang.authlib.GameProfile;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireEnv;
import com.yearofice.hunt.fishing.FishingService;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.fire.FireWeather;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.time.Rate;
import com.yearofice.sim.time.VanillaTime;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * The shared layers' GameTests (Minecraft development › Shared-layer GameTests; Layer 1 and Layer 2 tests).
 * Every test that moves or pins the clock runs in a batch of its own and puts the clock back, so it never leaks
 * into another test's game time; the env tests pin the valley weather on their level and clear it after.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class LayerGameTests {
    private LayerGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final String RATES = "m2_clock_rates", SLOWEST = "m2_clock_slowest", DAWN = "m2_clock_dawn", ENV = "m2_env", M2 = "m2";

    @GameTestGenerator
    public static Collection<TestFunction> layerTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test(RATES, "yearofice:clock/rates_exact", 1200, LayerGameTests::ratesExact));
        out.add(test(SLOWEST, "yearofice:clock/multiplayer_slowest", 200, LayerGameTests::multiplayerSlowest));
        out.add(test(DAWN, "yearofice:clock/dawn_fallback_june", 100, LayerGameTests::dawnFallbackJune));
        out.add(test(M2, "yearofice:clock/rules_and_save", 100, LayerGameTests::rulesAndSave));
        out.add(test(ENV, "yearofice:env/one_answer", 100, LayerGameTests::oneAnswer));
        out.add(test(ENV, "yearofice:env/override_drives_vanilla", 100, LayerGameTests::overrideDrivesVanilla));
        out.add(test(ENV, "yearofice:env/lapse_and_water", 100, LayerGameTests::lapseAndWater));
        return out;
    }

    static TestFunction test(String batch, String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction(batch, name, TEMPLATE, maxTicks, 0, true, body);
    }

    @AfterBatch(batch = RATES)
    public static void afterRates(ServerLevel level) { reset(); }

    @AfterBatch(batch = SLOWEST)
    public static void afterSlowest(ServerLevel level) { reset(); }

    @AfterBatch(batch = ENV)
    public static void afterEnv(ServerLevel level) {
        for (ServerLevel l : level.getServer().getAllLevels()) EnvService.get().setOverride(l, null);
    }

    static void reset() {
        ClockService.get().forceRate(null);
        if (ClockService.get().dayLengthMin() != 24) ClockService.get().setDayLength(24);
    }

    /** A detached player: real enough to ask rates, never in the player list. */
    static ServerPlayer player(GameTestHelper h, String name) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        return p;
    }

    // ---- clock --------------------------------------------------------------------------------------------

    /**
     * {@code clock/rates_exact}: each rate, pinned, adds exactly its ds every real tick for 60 ticks (the full
     * real-minute sums are JUnit's {@code ClockModelTest.ratesExact}); then on the 72-minute day World adds exactly
     * a third of the 24-minute day's and every other rate the same.
     */
    static void ratesExact(GameTestHelper h) {
        ClockService c = ClockService.get();
        long before = c.nowDs();
        int[][] want = {{24, 3}, {24, 6}, {24, 30}, {24, 150}, {24, 600}, {24, 1800}, {72, 10}, {72, 6}, {72, 1800}};
        Rate[] rates = {Rate.CLOSE, Rate.TRAVEL, Rate.WORLD, Rate.WORK, Rate.UNTIL_DONE, Rate.SLEEP, Rate.WORLD, Rate.TRAVEL, Rate.SLEEP};
        int span = 60;
        int[] phase = {-1};
        int[] tick = {0};
        long[] start = {0};
        h.onEachTick(() -> {
            if (phase[0] >= rates.length) return;
            if (phase[0] >= 0 && tick[0] == span) {
                long moved = c.nowDs() - start[0];
                long expect = (long) span * want[phase[0]][1];
                if (moved != expect) {
                    h.fail(rates[phase[0]].id() + " on the " + want[phase[0]][0] + "-minute day moved " + moved + " ds in " + span + " ticks, want " + expect);
                }
            }
            if (phase[0] < 0 || tick[0] == span) {
                phase[0]++;
                tick[0] = 0;
                if (phase[0] >= rates.length) return;
                if (c.dayLengthMin() != want[phase[0]][0]) c.setDayLength(want[phase[0]][0]);
                c.forceRate(rates[phase[0]]);
                start[0] = c.nowDs();
            }
            tick[0]++;
        });
        h.succeedWhen(() -> {
            h.assertTrue(phase[0] >= rates.length, "running phase " + phase[0]);
            reset();
            c.setNow(before);
        });
    }

    /** {@code clock/multiplayer_slowest}: a walker (Travel) and a worker (Work) give the travel rate; dayTime follows the sun. */
    static void multiplayerSlowest(GameTestHelper h) {
        ClockService c = ClockService.get();
        reset();
        ServerPlayer walker = player(h, "walker"), worker = player(h, "worker");
        int[] ticks = {0};
        long[] start = {0};
        h.onEachTick(() -> {
            if (ticks[0] == 2) start[0] = c.nowDs();
            c.requestRate(walker, Rate.TRAVEL, "test walking");
            c.requestRate(worker, Rate.WORK, "test working");
            ticks[0]++;
        });
        h.succeedWhen(() -> {
            h.assertTrue(ticks[0] >= 22, "waiting");
            h.assertTrue(c.rate() == Rate.TRAVEL, "world rate " + c.rate().id());
            h.assertTrue(c.rate(worker) == Rate.WORK && c.rate(walker) == Rate.TRAVEL, "each player's own rate");
            long moved = c.nowDs() - start[0];
            h.assertTrue(moved == 20L * 6, "travel adds 6 ds a tick: " + moved + " in 20 ticks");
            long t = Math.floorMod(h.getLevel().getDayTime(), 24_000L);
            long want = VanillaTime.dayTime(c.sunElevation(), c.sun().rising(c.absDay(), c.minuteOfDay()));
            h.assertTrue(Math.abs(t - want) <= 1, "day time " + t + " follows the sun (" + want + ")");
            h.assertTrue(!h.getLevel().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT), "doDaylightCycle is off");
        });
    }

    /** Collects the clock events posted while registered. */
    public static final class Recorder {
        final List<ClockEvent> events = new ArrayList<>();

        @SubscribeEvent
        public void dawn(ClockEvent.Dawn e) { events.add(e); }

        @SubscribeEvent
        public void dusk(ClockEvent.Dusk e) { events.add(e); }

        @SubscribeEvent
        public void day(ClockEvent.Day e) { events.add(e); }

        @SubscribeEvent
        public void jump(ClockEvent.Jump e) { events.add(e); }
    }

    /**
     * {@code clock/dawn_fallback_june}: June 21 gets one Dawn and one Dusk, by the midnight-sun fallback; a 25-hour
     * step posts a Jump and no minute events.
     */
    static void dawnFallbackJune(GameTestHelper h) {
        ClockService c = ClockService.get();
        long before = c.nowDs();
        Recorder r = new Recorder();
        NeoForge.EVENT_BUS.register(r);
        try {
            long jun21 = Calendar.dayOfYear(6, 21);
            c.setNow(jun21 * Calendar.DS_PER_DAY);
            r.events.clear();
            for (int i = 0; i < 24; i++) c.advance(60);
            long dawns = r.events.stream().filter(e -> e instanceof ClockEvent.Dawn).count();
            long dusks = r.events.stream().filter(e -> e instanceof ClockEvent.Dusk).count();
            long days = r.events.stream().filter(e -> e instanceof ClockEvent.Day).count();
            h.assertTrue(dawns == 1 && dusks == 1, "June 21: " + dawns + " dawns, " + dusks + " dusks");
            h.assertTrue(days == 1, "one day boundary");
            h.assertTrue(c.sun().dawnIsFallback(jun21) && c.sun().duskIsFallback(jun21), "midnight sun: the fallback");
            r.events.clear();
            c.advance(25 * 60);
            h.assertTrue(r.events.size() == 1 && r.events.get(0) instanceof ClockEvent.Jump, "a 25 h step is one Jump: " + r.events);
        } finally {
            NeoForge.EVENT_BUS.unregister(r);
            c.setNow(before);
        }
        h.succeed();
    }

    /** The clock's saved record round-trips and refuses a newer version; vanilla's clocks are off. */
    static void rulesAndSave(GameTestHelper h) {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putLong("game_ds", 123_456_789L);
        tag.putInt("day_length_min", 45);
        var loaded = com.yearofice.story.clock.ClockData.load(tag, h.getLevel().registryAccess());
        var saved = loaded.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess());
        h.assertTrue(saved.getLong("game_ds") == 123_456_789L && saved.getInt("day_length_min") == 45, "round trip " + saved);
        tag.putInt("v", 2);
        boolean refused = false;
        try {
            com.yearofice.story.clock.ClockData.load(tag, h.getLevel().registryAccess());
        } catch (IllegalStateException e) {
            refused = true;
        }
        h.assertTrue(refused, "a v2 record is refused, not read wrong");
        GameRules rules = h.getLevel().getGameRules();
        h.assertTrue(!rules.getBoolean(GameRules.RULE_DAYLIGHT) && !rules.getBoolean(GameRules.RULE_WEATHER_CYCLE), "vanilla time and weather are off");
        h.succeed();
    }

    // ---- env ----------------------------------------------------------------------------------------------

    /** {@code layers/one_question}, M2 part: the env sample, fire and fishing read the same air, wind and water. */
    static void oneAnswer(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        EnvService env = EnvService.get();
        for (int i = 0; i < 5; i++) {
            BlockPos pos = h.absolutePos(new BlockPos(1, 1 + i % 2, 1));
            EnvSample s = env.sample(level, pos);
            h.assertTrue(Math.abs(env.airC(level, pos) - s.airC()) < 0.01, "air");
            FireWeather fw = FireEnv.sample(level, pos, null);
            h.assertTrue(Math.abs(fw.windMs() - env.wind(level, pos).ms()) < 1e-9, "fire reads env wind: " + fw.windMs());
            h.assertTrue(fw.precip() == env.precip(level, pos), "fire reads env precipitation");
            double water = FishingService.waterC(level, ClockService.get().nowMinute());
            h.assertTrue(Math.abs(water - env.waterC(level, pos, EnvService.WaterKind.CREEK)) < 1e-9, "fishing reads env water");
        }
        h.succeed();
    }

    /** {@code /yoi env set}: an override reaches fire and vanilla's rain, and clearing it restores the draw. */
    static void overrideDrivesVanilla(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        EnvService env = EnvService.get();
        BlockPos pos = h.absolutePos(new BlockPos(1, 2, 1));
        env.setOverride(level, new EnvService.WeatherOverride(-12, 8, Precip.SNOW, 1.0));
        try {
            ClockService.get().advance(1);
            h.assertTrue(level.getLevelData().isRaining(), "vanilla precipitation follows the env");
            FireWeather fw = FireEnv.sample(level, pos, null);
            h.assertTrue(fw.precip() == Precip.SNOW && Math.abs(fw.windMs() - 8) < 1e-9, "fire sees snow and 8 m/s: " + fw);
            h.assertTrue(env.airC(level, pos) == -12, "override air");
        } finally {
            env.setOverride(level, null);
            ClockService.get().advance(1);
        }
        h.assertTrue(level.getLevelData().isRaining() == (env.valley(level).precip() != Precip.NONE), "cleared: vanilla follows the draw again");
        h.succeed();
    }

    /** Air lapses −0.65 °C per 100 m on a July noon; creeks run colder than lakes in summer. */
    static void lapseAndWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        EnvService env = EnvService.get();
        long noon = Calendar.dayOfYear(7, 15) * 1440L + 12 * 60;
        double floor = env.sampleAbstract(level, 0, 150, 0, noon).airC();
        double up = env.sampleAbstract(level, 0, 250, 0, noon).airC();
        h.assertTrue(Math.abs(floor - up - 0.65) < 1e-6, "lapse " + (floor - up));
        h.assertTrue(env.waterCAt(level, EnvService.WaterKind.CREEK, noon) < env.waterCAt(level, EnvService.WaterKind.LAKE, noon), "creek colder than lake");
        h.assertTrue(env.hourlySeries(level, BlockPos.ZERO, noon * 600, (noon + 300) * 600).size() == 6, "5 hours give 6 records");
        h.succeed();
    }
}
