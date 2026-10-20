package com.yearofice.gametest;

import com.yearofice.api.clock.ClockService;
import com.yearofice.core.YearOfIce;
import com.yearofice.hud.fieldnote.FieldNoteDefs;
import com.yearofice.hud.fieldnote.FieldNotes;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.sim.hud.FieldNoteQueue;
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

/** Field-note card GameTests (Creative concepts › Field-note cards). Batch {@code m3_field_note} runs alone: it asks the close clock. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class FieldNoteGameTests {
    private FieldNoteGameTests() {}

    static final String BATCH = "m3_field_note";
    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:hud/field_note_cap", 40, FieldNoteGameTests::cap));
        out.add(test("yearofice:hud/field_note_quiet_in_danger", 100, FieldNoteGameTests::quietInDanger));
        return TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, TEMPLATE, maxTicks, 0, true, body);
    }

    static ServerPlayer withMessenger(GameTestHelper h, String name) {
        ServerPlayer p = LayerGameTests.player(h, name);
        ItemStack m = new ItemStack(MessengerItems.MESSENGER.get());
        m.set(MessengerItems.BATTERY.get(), 5000);
        p.setItemInHand(InteractionHand.MAIN_HAND, m);
        return p;
    }

    /** {@code hud/field_note_cap}: two triggers in one minute show 10 real minutes apart, in order, and never twice. */
    static void cap(GameTestHelper h) {
        ServerPlayer p = withMessenger(h, "noter");
        h.assertTrue(FieldNoteDefs.get("yearofice:needle_tea") != null, "the data loaded");
        h.assertTrue(FieldNotes.fire(p, "yearofice:needle_tea"), "first trigger files");
        h.assertTrue(FieldNotes.fire(p, "yearofice:rose_hips"), "second trigger files");
        h.assertTrue(!FieldNotes.fire(p, "yearofice:needle_tea"), "never twice");
        h.assertTrue(FieldNotes.record(p).filed.size() == 2, "both are in the notebook at once");
        long t0 = 1000;
        h.assertTrue("yearofice:needle_tea".equals(FieldNotes.pump(p, t0)), "the first shows");
        h.assertTrue(FieldNotes.pump(p, t0 + 20 * 60) == null, "a minute later the second waits");
        h.assertTrue(FieldNotes.pump(p, t0 + FieldNoteQueue.GAP_TICKS - 1) == null, "still inside the 10 minutes");
        h.assertTrue("yearofice:rose_hips".equals(FieldNotes.pump(p, t0 + FieldNoteQueue.GAP_TICKS)), "then the second, in order");
        h.assertTrue(FieldNotes.record(p).unseen.isEmpty(), "both seen");
        h.succeed();
    }

    /** {@code hud/field_note_quiet_in_danger}: no card during the close clock; it shows once the clock ends. */
    static void quietInDanger(GameTestHelper h) {
        ServerPlayer p = withMessenger(h, "scared");
        FieldNotes.fire(p, "yearofice:birch_bark");
        int[] t = {0};
        boolean[] leaked = {false};
        String[] shown = {null};
        h.onEachTick(() -> {
            t[0]++;
            if (t[0] <= 20) {
                ClockService.get().requestRate(p, Rate.CLOSE, "test bear charge");
                if (t[0] >= 3 && ClockService.get().rate(p) == Rate.CLOSE && FieldNotes.pump(p, 5_000_000L + t[0]) != null) leaked[0] = true;
            } else if (shown[0] == null && ClockService.get().rate(p) != Rate.CLOSE) {
                shown[0] = FieldNotes.pump(p, 5_000_000L + t[0]);
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(t[0] >= 25, "waiting");
            h.assertTrue(!leaked[0], "no card shows during the close clock");
            h.assertTrue("yearofice:birch_bark".equals(shown[0]), "the card shows after the close clock ends");
        });
    }
}
