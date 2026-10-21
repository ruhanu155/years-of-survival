package com.yearofice.body.gametest;

import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyTicker;
import com.yearofice.body.clothing.ClothingItems;
import com.yearofice.body.clothing.StartOutfit;
import com.yearofice.body.env.VanillaEnvSampler;
import com.yearofice.core.YearOfIce;
import com.yearofice.gametest.TestFilter;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.env.Precip;
import com.yearofice.travel.TravelItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** M7 lane F, "Wear it": the body reads what is worn. At −25 °C, no fire: mittens hold the hands, the start kit does not. */
@GameTestHolder(YearOfIce.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WearGameTests {
    private WearGameTests() {}

    private static final String BATCH = "m7_wear";

    @GameTestGenerator
    public static Collection<TestFunction> wearTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(t("yearofice:body/mittens_hold_hands_at_minus25", 400, WearGameTests::mittensHold));
        out.add(t("yearofice:body/start_kit_fails_at_minus25", 400, WearGameTests::startKitFails));
        out.add(t("yearofice:body/naked_vs_kit", 400, WearGameTests::nakedVsKit));
        return TestFilter.apply(out);
    }

    private static TestFunction t(String name, int ticks, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, YearOfIce.MOD_ID + ":gametest/empty3", ticks, 0, true, body);
    }

    private static ServerPlayer cold(GameTestHelper h) {
        ServerPlayer p = BodyGameTests.player(h);
        BodyTicker.VANILLA_ENV.override(p, new VanillaEnvSampler.EnvOverride(-25, 0, Precip.NONE, 0, Double.NaN));
        return p;
    }

    /** Parka, mukluks and fur mittens (hat) over the start inners. */
    private static void fullKit(ServerPlayer p) {
        StartOutfit.dress(p);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(TravelItems.PARKA.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(TravelItems.MUKLUKS.get()));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(TravelItems.FUR_HAT.get()));
        com.yearofice.item.inventory.PackSlots.set(p, "hands_outer", 0, new ItemStack(TravelItems.FUR_MITTENS.get()));
    }

    private static int handFrost(ServerPlayer p) {
        int[] f = BodyAttachments.of(p).parts.partFrost();
        return Math.max(f[Part.LEFT_HAND.ordinal()], f[Part.RIGHT_HAND.ordinal()]);
    }

    /** Minutes until the hands show frost, or -1 if they never do in {@code minutes}. */
    private static int frostAt(ServerPlayer p, int minutes) {
        long m = 12 * 60;
        for (int i = 0; i < minutes; i++) {
            BodyTicker.step(p, m + i, 1, Activity.REST, false, false, BodyTuning.STANDARD);
            if (handFrost(p) > 0) return i + 1;
        }
        return -1;
    }

    private static double feels(ServerPlayer p) {
        BodyTicker.step(p, 12 * 60, 1, Activity.REST, false, false, BodyTuning.STANDARD);
        return BodyAttachments.of(p).lastFeel.totalC();
    }

    private static void mittensHold(GameTestHelper h) {
        ServerPlayer p = cold(h);
        try {
            fullKit(p);
            h.assertTrue(com.yearofice.api.body.ClothingService.get().worn(p).size() == 9, "nine garments read, got "
                    + com.yearofice.api.body.ClothingService.get().worn(p).size());
            int at = frostAt(p, 60);
            h.assertTrue(at < 0, "hands crossed the frost threshold at minute " + at);
            h.succeed();
        } finally {
            BodyTicker.VANILLA_ENV.override(p, null);
        }
    }

    private static void startKitFails(GameTestHelper h) {
        ServerPlayer p = cold(h);
        try {
            StartOutfit.dress(p);
            int at = frostAt(p, 60);
            h.assertTrue(at > 0, "start-kit hands never showed frost in 60 min at -25 C");
            h.succeed();
        } finally {
            BodyTicker.VANILLA_ENV.override(p, null);
        }
    }

    /** Garment-table sums at −25 °C, still air. (The proxy's −20.5 / +3.6 °C are with wind and work, not this still-air case.) */
    private static void nakedVsKit(GameTestHelper h) {
        ServerPlayer naked = cold(h), start = cold(h), full = cold(h);
        try {
            StartOutfit.mark(naked);
            StartOutfit.dress(start);
            fullKit(full);
            double n = feels(naked), s = feels(start), f = feels(full);
            YearOfIce.LOG.info("wear feels: naked {} start {} full {}", n, s, f);
            h.assertTrue(n < s - 5 && s < f - 15, "ordering naked " + n + " < start " + s + " < full " + f);
            // Still air, dry: feels-like = air + the garment table's warmth sum (start set 15, the full set 41).
            h.assertTrue(Math.abs(n - -25.0) <= 0.5, "naked feels " + n + ", wanted -25");
            h.assertTrue(Math.abs(s - (-25 + 15)) <= 0.5, "start kit feels " + s + ", table -10");
            h.assertTrue(Math.abs(f - (-25 + 41)) <= 0.5, "full kit feels " + f + ", table +16");
            h.succeed();
        } finally {
            for (ServerPlayer p : new ServerPlayer[] {naked, start, full}) BodyTicker.VANILLA_ENV.override(p, null);
        }
    }
}
