package com.yearofice.gametest;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.sim.time.Rate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Layer 8's GameTests (Messenger and HUD bus › Tests). Batch {@code m2_hud} runs alone: the close-clock test asks
 * the Close rate, which slows the world clock for every test running beside it.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class HudGameTests {
    private HudGameTests() {}

    static final String BATCH = "m2_hud";

    @GameTestGenerator
    public static Collection<TestFunction> hudTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:hud/no_trap_lines", 40, HudGameTests::noTrapLines));
        out.add(test("yearofice:hud/close_clock_filter", 100, HudGameTests::closeClockFilter));
        out.add(test("yearofice:hud/battery_zero_to_journal", 40, HudGameTests::batteryZeroToJournal));
        out.add(test("yearofice:hud/dedup_and_channels", 40, HudGameTests::dedupAndChannels));
        return out;
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, LayerGameTests.TEMPLATE, maxTicks, 0, true, body);
    }

    static ServerPlayer withMessenger(GameTestHelper h, String name, int battery) {
        ServerPlayer p = LayerGameTests.player(h, name);
        ItemStack m = new ItemStack(MessengerItems.MESSENGER.get());
        m.set(MessengerItems.BATTERY.get(), battery);
        p.setItemInHand(InteractionHand.MAIN_HAND, m);
        return p;
    }

    /** {@code hud/no_trap_lines}: a line whose source is a trap is refused (an exception in development). */
    static void noTrapLines(GameTestHelper h) {
        ServerPlayer p = withMessenger(h, "trapper", 5000);
        boolean refused = false;
        try {
            HudBus.get().post(p, HudLine.of(HudLineType.EVENT, "snare_caught", "food.traps", HudArg.species("yearofice:snowshoe_hare")));
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        h.assertTrue(refused, "a trap line is refused");
        h.assertTrue(HudBus.get().sent(p).isEmpty() && HudBus.get().journal(p).isEmpty(), "and goes nowhere");
        h.succeed();
    }

    /** {@code hud/close_clock_filter}: a priority-2 line during the close clock shows only after it ends; priority 3 at once. */
    static void closeClockFilter(GameTestHelper h) {
        ServerPlayer p = withMessenger(h, "charged", 5000);
        HudBus bus = HudBus.get();
        HudLine alert = HudLine.of(HudLineType.ALERT, "test_alert", "test");
        HudLine warn = HudLine.of(HudLineType.WARN, "test_warn", "test");
        int[] t = {0};
        h.onEachTick(() -> {
            t[0]++;
            if (t[0] <= 20) ClockService.get().requestRate(p, Rate.CLOSE, "test bear charge");
            if (t[0] == 5) {
                h.assertTrue(ClockService.get().rate(p) == Rate.CLOSE, "on the close clock");
                bus.post(p, alert);
                bus.post(p, warn);
            }
            if (t[0] == 10) {
                h.assertTrue(bus.sent(p).contains(warn), "priority 3 shows during the close clock");
                h.assertTrue(!bus.sent(p).contains(alert) && bus.held(p).contains(alert), "priority 2 waits");
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(t[0] >= 25, "waiting");
            h.assertTrue(ClockService.get().rate(p) != Rate.CLOSE, "close clock over");
            h.assertTrue(bus.sent(p).contains(alert) && bus.held(p).isEmpty(), "the held line shows once the close clock ends");
        });
    }

    /** {@code hud/battery_zero_to_journal}: with the battery at 0, or no messenger, messenger lines go to the journal only. */
    static void batteryZeroToJournal(GameTestHelper h) {
        HudBus bus = HudBus.get();
        ServerPlayer dead = withMessenger(h, "dead", 0);
        ServerPlayer none = LayerGameTests.player(h, "none");
        ServerPlayer live = withMessenger(h, "live", 100);
        HudLine body = HudLine.of(HudLineType.BODY, "test_weight", "body", HudArg.fixed(68.9, "kg"));
        for (ServerPlayer p : List.of(dead, none, live)) bus.post(p, body);
        h.runAfterDelay(2, () -> {
            h.assertTrue(bus.journal(dead).contains(body) && bus.sent(dead).isEmpty(), "battery 0: journal only");
            h.assertTrue(bus.journal(none).contains(body) && bus.sent(none).isEmpty(), "no messenger: journal only");
            h.assertTrue(bus.sent(live).contains(body) && bus.journal(live).isEmpty(), "a live messenger shows it");
            // Pickup lines answer the player's own action: no battery needed.
            HudLine pickup = HudLine.of(HudLineType.PICKUP, "ate", "body", HudArg.of(250), HudArg.of(40));
            bus.post(none, pickup);
            h.runAfterDelay(2, () -> {
                h.assertTrue(bus.sent(none).contains(pickup), "pickup lines need no messenger");
                h.succeed();
            });
        });
    }

    /** The same key and arguments within 10 game minutes are dropped; journal lines never reach the screen. */
    static void dedupAndChannels(GameTestHelper h) {
        HudBus bus = HudBus.get();
        ServerPlayer p = withMessenger(h, "repeat", 5000);
        HudLine a = HudLine.of(HudLineType.SEASON, "greening", "story");
        HudLine b = HudLine.of(HudLineType.SEASON, "greening", "story", HudArg.of(2));
        bus.post(p, a);
        bus.post(p, a);
        bus.post(p, b);
        HudLine j = HudLine.of(HudLineType.JOURNAL, "felled", "build");
        bus.post(p, j);
        h.runAfterDelay(2, () -> {
            long sentA = bus.sent(p).stream().filter(a::equals).count();
            h.assertTrue(sentA == 1, "a repeat within 10 minutes is dropped: " + sentA);
            h.assertTrue(bus.sent(p).contains(b), "other arguments are another line");
            h.assertTrue(!bus.sent(p).contains(j) && bus.journal(p).contains(j), "journal lines stay off the screen");
            boolean tooMany = false;
            try {
                HudLine.of(HudLineType.ALERT, "x", "t", HudArg.of(1), HudArg.of(2), HudArg.of(3), HudArg.of(4), HudArg.of(5), HudArg.of(6), HudArg.of(7));
            } catch (IllegalArgumentException e) {
                tooMany = true;
            }
            h.assertTrue(tooMany, "at most 6 arguments");
            h.succeed();
        });
    }
}
