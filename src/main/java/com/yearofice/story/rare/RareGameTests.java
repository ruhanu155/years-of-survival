package com.yearofice.story.rare;

import com.google.gson.JsonParser;
import com.yearofice.api.env.EnvService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.story.MistRule;
import com.yearofice.sim.story.SkyLore;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** M7 lane D story GameTests: sky signs are honest odds against the real year table, and a year of mist mornings. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class RareGameTests {
    private RareGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:story/sky_lore", 200, RareGameTests::skyLore));
        out.add(test("yearofice:story/mist_count", 200, RareGameTests::mistCount));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m7d", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static void skyLore(GameTestHelper h) {
        var in = RareGameTests.class.getResourceAsStream("/data/yearofice/story/sky_signs.json");
        var table = SkyLore.signs(JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject());
        h.assertTrue(table.get("aurora").lore().length() > 20 && table.get("sun_dogs").lore().length() > 20, "aurora and sun-dog lore clips exist");
        EnvService env = EnvService.get();
        Random rng = new Random(11);
        int[] shownWhenTrue = new int[1], trials = new int[1];
        var sunDogs = table.get("sun_dogs");
        for (long d = 0; d < 365; d++) {
            var today = env.day(h.getLevel(), d);
            var tom = env.day(h.getLevel(), d + 1);
            var t = new SkyLore.Tomorrow(tom.meanC(), today.meanC(), tom.precipMm(), tom.windMs());
            if (!t.holds(sunDogs.truth())) continue;
            for (int i = 0; i < 100; i++) { trials[0]++; if (SkyLore.shown(table, t, rng).contains(sunDogs)) shownWhenTrue[0]++; }
        }
        h.assertTrue(trials[0] > 5000, "the year had enough cold-holds days: " + trials[0]);
        double f = shownWhenTrue[0] / (double) trials[0];
        h.assertTrue(Math.abs(f - sunDogs.pTrue()) < 0.03, "sun dogs show on " + f + " of the days they are true, table says " + sunDogs.pTrue());
        h.assertTrue(SkyLore.right(SkyLore.Level.PRACTICED) == 0.8, "practiced reads are right 80%");
        h.succeed();
    }

    static void mistCount(GameTestHelper h) {
        EnvService env = EnvService.get();
        int mist = 0;
        for (long d = 1; d <= 365; d++) {
            long minute = d * 1440 + 5 * 60;
            var hr = env.valleyAt(h.getLevel(), minute);
            double water = env.waterCAt(h.getLevel(), EnvService.WaterKind.RIVER, minute);
            boolean wet = env.day(h.getLevel(), d - 1).precipMm() >= 0.5;
            if (MistRule.mist(water >= 1, hr.windMs(), water, hr.airC(), wet)) mist++;
        }
        h.assertTrue(mist >= 25 && mist <= 40, "about 25-40 mist mornings a year, got " + mist);
        h.succeed();
    }
}
