package com.yearofice.gametest;

import com.yearofice.api.event.GarmentTearEvent;
import com.yearofice.api.hud.HudBus;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.injury.BodyPublic;
import com.yearofice.body.injury.InjuryItems;
import com.yearofice.body.injury.InjuryService;
import com.yearofice.body.injury.Treat;
import com.yearofice.body.net.BodyPublicPayload;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.injury.Infection;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Problems;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** M4 lane A2: the treat flow, infection, unsplinted loss, garment tears and body_public. Batch {@code m4_injury2}. */
@GameTestHolder(com.yearofice.core.YearOfIce.MOD_ID)
public final class InjuryCompletionGameTests {
    private InjuryCompletionGameTests() {}

    static final String BATCH = "m4_injury2";

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:body/treat_consumes_item", InjuryCompletionGameTests::treatConsumesItem));
        out.add(test("yearofice:body/infection_progresses", InjuryCompletionGameTests::infectionProgresses));
        out.add(test("yearofice:body/unsplinted_loss", InjuryCompletionGameTests::unsplintedLoss));
        out.add(test("yearofice:body/garment_tear", InjuryCompletionGameTests::garmentTear));
        out.add(test("yearofice:net/body_public", InjuryCompletionGameTests::bodyPublic));
        return TestFilter.apply(out);
    }

    private static TestFunction test(String name, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, LayerGameTests.TEMPLATE, 100, 0, true, body);
    }

    private static ServerPlayer fresh(GameTestHelper h) {
        ServerPlayer p = LayerGameTests.player(h, "injury2-test");
        p.setData(BodyAttachments.BODY.get(), new BodyState());
        p.getInventory().clearContent();
        return p;
    }

    private static void add(ServerPlayer p, Problem pr) {
        BodyState s = BodyAttachments.of(p);
        s.parts = com.yearofice.sim.body.injury.Treatments.add(s.parts, pr);
    }

    private static long lines(ServerPlayer p, String id) {
        return HudBus.get().journal(p).stream().filter(l -> l.key().endsWith("." + id)).count();
    }

    private static int bandages(ServerPlayer p) {
        int n = 0;
        for (ItemStack st : p.getInventory().items) if (st.is(InjuryItems.BANDAGE.get())) n += st.getCount();
        return n;
    }

    /** Treating a cut spends one bandage and dresses it; with none left the Messenger says so; Treat all stops once. */
    static void treatConsumesItem(GameTestHelper h) {
        ServerPlayer p = fresh(h);
        add(p, Problem.fresh(Problems.CUT, Part.LEFT_LEG, "shin", 2, 0));
        p.getInventory().add(new ItemStack(InjuryItems.BANDAGE.get(), 2));
        Treat.Result r = Treat.treatPart(p, Part.LEFT_LEG);
        h.assertTrue(r.outcome() == Treat.Outcome.TREATED, "treated, got " + r.outcome());
        h.assertTrue(bandages(p) == 1, "one bandage spent, " + bandages(p) + " left");
        h.assertTrue(BodyAttachments.of(p).parts.problems().get(0).dressed(), "the cut is dressed");

        // Out of bandages: the line fires and nothing is consumed or changed.
        p = fresh(h);
        add(p, Problem.fresh(Problems.CUT, Part.RIGHT_LEG, "shin", 1, 0));
        r = Treat.treatPart(p, Part.RIGHT_LEG);
        h.assertTrue(r.outcome() == Treat.Outcome.OUT_OF_ITEM, "no bandage, got " + r.outcome());
        h.assertTrue(lines(p, "out_of") == 1, "the 'Out of bandages' line posts once");
        h.assertTrue(!BodyAttachments.of(p).parts.problems().get(0).dressed(), "still undressed");

        // Treat all: deep cut first, then the minor cut is left with the line once.
        p = fresh(h);
        add(p, Problem.fresh(Problems.CUT, Part.LEFT_ARM, "forearm", 1, 0));
        add(p, Problem.fresh(Problems.CUT, Part.RIGHT_LEG, "shin", 2, 0));
        p.getInventory().add(new ItemStack(InjuryItems.BANDAGE.get(), 1));
        int n = Treat.treatAll(p);
        h.assertTrue(n == 1 && bandages(p) == 0, "one treated with one bandage, got " + n);
        Problem still = BodyAttachments.of(p).parts.problems().stream().filter(q -> !q.dressed()).findFirst().orElse(null);
        h.assertTrue(still != null && still.part() == Part.LEFT_ARM, "the minor cut is the one left bleeding");
        h.assertTrue(lines(p, "out_of") == 1, "the out-of line fires once");
        h.succeed();
    }

    /** Undressed 12 h: infection stage 1; day 1: stage 2; 84 h on: stage 3; a redressed stage 1 stays; stage 1 costs 10 a day. */
    static void infectionProgresses(GameTestHelper h) {
        BodyState s = new BodyState();
        s.parts = com.yearofice.sim.body.injury.Treatments.add(s.parts, Problem.fresh(Problems.CUT, Part.LEFT_LEG, "shin", 1, 0));
        long t = 0;
        t = step(s, t, 11 * 60);
        h.assertTrue(noInfection(s), "no infection at 11 h");
        t = step(s, t, 2 * 60);
        h.assertTrue(stageOf(s) == 1, "stage 1 after 12 h, got " + stageOf(s));
        double c0 = s.condition();
        t = step(s, t, 22 * 60); // 35 h in: the infection is 23 h old, still inside its first day
        h.assertTrue(stageOf(s) == 1, "still stage 1 at 35 h, got " + stageOf(s));
        t = step(s, t, 24 * 60);
        h.assertTrue(stageOf(s) == 2, "stage 2 at 48 h, got " + stageOf(s));
        h.assertTrue(s.condition() < c0, "stage 1 cost Condition, " + c0 + " -> " + s.condition());
        t = step(s, t, 3 * 24 * 60);
        h.assertTrue(stageOf(s) == 3, "stage 3 at day 5, got " + stageOf(s));
        double c3 = s.condition();
        step(s, t, 24 * 60);
        h.assertTrue(c3 - s.condition() >= 19.5 && c3 - s.condition() <= 20.5 || s.condition() <= 0.01,
                "stage 3 costs 20 a day, lost " + (c3 - s.condition()));

        // A cleaned and redressed stage 1 does not advance.
        BodyState d = new BodyState();
        d.parts = com.yearofice.sim.body.injury.Treatments.add(d.parts, Problem.fresh(Problems.INFECTION, Part.LEFT_ARM, "forearm", 1, 0));
        d.parts = com.yearofice.sim.body.injury.Treatments.bandage(d.parts, 0, 0);
        step(d, 0, 4 * 24 * 60);
        h.assertTrue(stageOf(d) == 1, "a redressed stage 1 holds, got " + stageOf(d));
        h.succeed();
    }

    private static long step(BodyState s, long from, int minutes) {
        // One game hour at a time, the way the ticker catches up.
        int left = minutes;
        long t = from;
        while (left > 0) {
            int m = Math.min(60, left);
            s.parts = Infection.advance(s.parts, t, m, 1.0);
            t += m;
            left -= m;
        }
        return t;
    }

    private static boolean noInfection(BodyState s) {
        return s.parts.problems().stream().noneMatch(q -> q.id().equals(Problems.INFECTION));
    }

    private static int stageOf(BodyState s) {
        return s.parts.problems().stream().filter(q -> q.id().equals(Problems.INFECTION)).mapToInt(Problem::stage).findFirst().orElse(0);
    }

    /** A break left unsplinted costs 5 Condition a day on its part; a splinted one costs nothing. */
    static void unsplintedLoss(GameTestHelper h) {
        BodyState s = new BodyState();
        s.parts = com.yearofice.sim.body.injury.Treatments.add(s.parts, Problem.fresh(Problems.BROKEN_BONE, Part.LEFT_LEG, "shin", 1, 0));
        step(s, 0, 3 * 24 * 60);
        h.assertTrue(Math.abs(s.condition() - 85) < 0.01, "three days unsplinted: Condition 85, got " + s.condition());
        h.assertTrue(s.parts.hp(Part.LEFT_LEG) < 100, "the loss lands on the leg, hp " + s.parts.hp(Part.LEFT_LEG));
        BodyState t = new BodyState();
        t.parts = com.yearofice.sim.body.injury.Treatments.add(t.parts, Problem.fresh(Problems.BROKEN_BONE, Part.LEFT_LEG, "shin", 1, 0));
        t.parts = com.yearofice.sim.body.injury.Treatments.splint(t.parts, 0, 0);
        step(t, 0, 3 * 24 * 60);
        h.assertTrue(Math.abs(t.condition() - 100) < 0.01 && t.parts.hp(Part.LEFT_LEG) == 100, "splinted: no loss, got " + t.condition());
        h.succeed();
    }

    /** A bear swipe posts the tear for the hit zone's outer garment; a knife slip does not. */
    static void garmentTear(GameTestHelper h) {
        List<GarmentTearEvent> got = new ArrayList<>();
        Consumer<GarmentTearEvent> l = e -> got.add(e);
        NeoForge.EVENT_BUS.addListener(l);
        try {
            ServerPlayer p = fresh(h);
            InjuryService.injure(p, "bear_swipe", Part.TORSO, Double.NaN, "test");
            h.assertTrue(got.size() == 1, "one tear, got " + got.size());
            h.assertTrue(got.get(0).zone().equals("torso") && Math.abs(got.get(0).amount() - 0.15) < 1e-9, "torso -0.15, got "
                    + got.get(0).zone() + " " + got.get(0).amount());
            InjuryService.injure(p, "knife_slip", Part.LEFT_HAND, Double.NaN, "test");
            h.assertTrue(got.size() == 1, "a knife slip tears nothing");
        } finally {
            NeoForge.EVENT_BUS.unregister(l);
        }
        h.succeed();
    }

    /** body_public carries only what shows (flags), round-trips, and is tiny. */
    static void bodyPublic(GameTestHelper h) {
        ServerPlayer p = fresh(h);
        h.assertTrue(BodyPublic.flags(BodyAttachments.of(p)) == 0, "a healthy body shows nothing");
        add(p, Problem.fresh(Problems.BROKEN_BONE, Part.RIGHT_LEG, "shin", 1, 0));
        add(p, Problem.fresh(Problems.CUT, Part.LEFT_ARM, "forearm", 2, 0));
        BodyPublicPayload pl = BodyPublic.payload(p);
        h.assertTrue(pl.has(BodyPublicPayload.LIMP) && pl.has(BodyPublicPayload.BLEEDING) && !pl.has(BodyPublicPayload.FEVER),
                "limp and bleeding show, flags " + pl.flags());
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        BodyPublicPayload.STREAM_CODEC.encode(buf, pl);
        h.assertTrue(buf.readableBytes() <= 4, "the packet is tiny, " + buf.readableBytes() + " bytes");
        BodyPublicPayload back = BodyPublicPayload.STREAM_CODEC.decode(buf);
        h.assertTrue(back.equals(pl), "round trip");
        h.succeed();
    }
}
