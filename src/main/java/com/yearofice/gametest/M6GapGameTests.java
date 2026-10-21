package com.yearofice.gametest;

import com.yearofice.core.YearOfIce;
import com.yearofice.core.hash.Mulberry32;
import com.yearofice.ecosystem.encounter.WorldToggles;
import com.yearofice.ecosystem.theft.TheftService;
import com.yearofice.food.FoodItems;
import com.yearofice.sim.ecosystem.Theft;
import com.yearofice.sim.ecosystem.TrophyDraw;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * The M6 GAP test names the blueprint lists (RECONCILE 292), where the behaviour exists: {@code ecology/rack_thief_rates},
 * {@code ecology/rack_thieves_toggle} and {@code hunt/trophy_class_top5}. Not here, because the behaviour is not built:
 * hud/door_open_warn (no door_open line), story/trophy_wall_ambitions (no big_five/winter_coats data),
 * encounter/bear_contact_sequence (no bite counts) -- listed in RECONCILE 303 follow-ups.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class M6GapGameTests {
    private M6GapGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> m6gapTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:ecology/rack_thief_rates", 100, M6GapGameTests::rackRates));
        out.add(test("yearofice:ecology/rack_thieves_toggle", 100, M6GapGameTests::rackToggle));
        out.add(test("yearofice:hunt/trophy_class_top5", 100, M6GapGameTests::top5));
        return out;
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m6gap", name, M6DGameTests.TEMPLATE, maxTicks, 0, true, body);
    }

    static int hoursWithTheft(Theft.Context c, int hours, int seed) {
        Mulberry32 r = new Mulberry32(seed);
        List<Theft.Stack> hide = List.of(new Theft.Stack(Theft.Kind.HIDE, 1.0, 1));
        int n = 0;
        for (int i = 0; i < hours; i++) if (Theft.rollHour(c, 1.0, r, hide, java.util.Set.of(Theft.Thief.MARTEN, Theft.Thief.GRAY_JAY, Theft.Thief.RAVEN, Theft.Thief.RED_SQUIRREL)).isPresent()) n++;
        return n;
    }

    /** An open rack with a hide, 10,000 night hours, the player far away: fox 4% +-0.4; fenced with the gate shut 0; a tamed wolf within 20 m 1% +-0.2. */
    static void rackRates(GameTestHelper h) {
        var open = new Theft.Context(true, false, false, false, false, false, false, 50);
        int fox = hoursWithTheft(open, 10_000, 7);
        h.assertTrue(fox >= 360 && fox <= 440, "open rack fox thefts in 10,000 night hours: " + fox + " (want 4% +-0.4)");
        int fenced = hoursWithTheft(new Theft.Context(true, true, false, false, false, false, false, 50), 10_000, 7);
        h.assertTrue(fenced == 0, "behind the fence with the gate closed: " + fenced);
        int wolf = hoursWithTheft(new Theft.Context(true, false, false, false, true, false, false, 50), 10_000, 7);
        h.assertTrue(wolf >= 80 && wolf <= 120, "with a tamed wolf within 20 m: " + wolf + " (want 1% +-0.2)");
        h.succeed();
    }

    /** Option off: 1,000 game days (24,000 hours) of unattended racks lose nothing; on mid-world, the next hour can lose. */
    static void rackToggle(GameTestHelper h) {
        var l = h.getLevel();
        BlockPos at = h.absolutePos(M6DGameTests.DOOR);
        var ctx = new Theft.Context(true, false, false, false, false, false, true, 200); // near spruce: the marten takes dried fish
        try {
            WorldToggles.setRacks(false);
            SimpleContainer c = new SimpleContainer(9);
            c.setItem(0, new ItemStack(FoodItems.DRIED_FISH.get(), 40));
            int taken = 0;
            for (int i = 0; i < 24_000; i++) if (TheftService.hour(l, at, c, i, ctx).isPresent()) taken++;
            h.assertTrue(taken == 0 && c.getItem(0).getCount() == 40, "option off: " + taken + " thefts in 1,000 game days");
            WorldToggles.setRacks(true);
            int on = 0;
            for (int i = 24_000; i < 26_000 && on == 0; i++) if (TheftService.hour(l, at, c, i, ctx).isPresent()) on++;
            h.assertTrue(on > 0, "turned on mid-world, thefts resume");
            h.succeed();
        } finally {
            WorldToggles.setRacks(null);
        }
    }

    /** 10,000 drawn prime bulls: 5% +-0.4 reach trophy class. (The stick's 1 cm reading has no stick item yet.) */
    static void top5(GameTestHelper h) {
        Mulberry32 r = new Mulberry32(1649);
        int trophy = 0;
        for (int i = 0; i < 10_000; i++) if (TrophyDraw.draw(150, 10, 0, r).cls().equals("trophy")) trophy++;
        h.assertTrue(trophy >= 460 && trophy <= 540, "prime bulls in trophy class: " + trophy + " of 10,000 (want 5% +-0.4)");
        h.succeed();
    }
}
