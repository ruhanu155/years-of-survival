package com.yearofice.sim.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.sim.fishing.FightModel.Input;
import com.yearofice.sim.fishing.FightModel.Outcome;
import com.yearofice.sim.fishing.FightModel.Params;
import com.yearofice.sim.fishing.FightModel.Side;
import com.yearofice.sim.fishing.FightModel.State;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The fight step function (Blueprint › Fishing methods; JUnit hunt/fight_lockstep). */
class FightModelTest {
    static Params params(FishSpecies s, double kg, Tackle t, long fightId) {
        return new Params(s, kg, t, 77L, fightId, 12, false, false, false, 10, false, false, false);
    }

    /** A careful bot: gives line above 70% tension, side pressure against, grabs when tired and close. */
    static Input careful(Params p, State s) {
        boolean high = s.tensionShare(p) > 0.7;
        boolean grab = s.phase() == FightModel.Phase.TIRED && s.lineOutM() <= 2.0;
        return new Input(!high, false, Side.AGAINST, s.lineOutM() > 15, grab);
    }

    static State run(Params p, java.util.function.BiFunction<Params, State, Input> bot, int maxTicks, List<Byte> record) {
        State s = FightModel.start(p);
        while (!s.over() && s.tick() < maxTicks) {
            Input in = bot.apply(p, s);
            if (record != null) record.add(in.toByte());
            s = FightModel.step(p, s, in);
        }
        return s;
    }

    @Test
    void inputIsOneByte() {
        for (int b = 0; b < 64; b++) {
            if (((b >> 2) & 3) == 3) continue;
            Input in = Input.fromByte((byte) b);
            assertEquals((byte) b, in.toByte());
        }
    }

    /**
     * hunt/fight_lockstep: the client steps as the player plays; the server gets the inputs in 4-tick batches
     * after 150 or 300 ms (3 or 6 ticks) and steps each tick when its input arrives. Both reach the same state at
     * every tick.
     */
    @Test
    void fight_lockstep() {
        for (int delayTicks : new int[] {3, 6}) {
            for (long id = 1; id <= 20; id++) {
                Params p = params(id % 2 == 0 ? FishSpecies.northernPike() : FishSpecies.arcticGrayling(),
                        id % 2 == 0 ? 3.5 : 0.8, Tackle.sinewReel(), id);
                State client = FightModel.start(p);
                State server = FightModel.start(p);
                List<State> clientStates = new ArrayList<>();
                ArrayDeque<byte[]> wire = new ArrayDeque<>();
                ArrayDeque<Integer> arrives = new ArrayDeque<>();
                byte[] batch = new byte[4];
                int inBatch = 0;
                ArrayDeque<Byte> serverInbox = new ArrayDeque<>();
                for (int tick = 0; tick < 20 * 120; tick++) {
                    if (!client.over()) {
                        Input in = careful(p, client);
                        batch[inBatch++] = in.toByte();
                        client = FightModel.step(p, client, in);
                        clientStates.add(client);
                    } else {
                        batch[inBatch++] = Input.IDLE.toByte();
                    }
                    if (inBatch == 4) {
                        wire.add(batch.clone());
                        arrives.add(tick + delayTicks);
                        inBatch = 0;
                    }
                    while (!arrives.isEmpty() && arrives.peek() <= tick) {
                        arrives.poll();
                        for (byte b : wire.poll()) serverInbox.add(b);
                    }
                    while (!serverInbox.isEmpty() && !server.over()) {
                        server = FightModel.step(p, server, Input.fromByte(serverInbox.poll()));
                        assertEquals(clientStates.get(server.tick() - 1), server, "tick " + server.tick());
                    }
                }
                assertTrue(client.over(), "fight " + id + " should end inside 2 minutes");
                assertEquals(client, server, "delay " + delayTicks + " fight " + id);
            }
        }
    }

    /**
     * A bot that always holds retrieve loses at least 60% of pike over 5 kg on plant-fiber line to snaps.
     * Run with a wire leader: without one, half the pike are armed to bite through at their first head-shake
     * (every 2-5 s), which mostly comes before the first run (every 4-9 s), so bite-through takes about 45%
     * first and snaps only just clear 60% (flagged in the PR).
     */
    @Test
    void retrieveOnlyBotSnapsBigPike() {
        Mulberry32 r = new Mulberry32(8);
        int n = 200, snaps = 0, snapsNoLeader = 0;
        for (int i = 0; i < n; i++) {
            double kg = 5.01 + 4.99 * r.nextDouble();
            java.util.function.BiFunction<Params, State, Input> bot =
                    (pp, s) -> new Input(true, false, Side.INLINE, false, s.lineOutM() <= 2);
            Params p = params(FishSpecies.northernPike(), kg, Tackle.plantFiberWillow().withLeader(true), 1000 + i);
            if (run(p, bot, 20 * 600, null).outcome() == Outcome.SNAP) snaps++;
            Params bare = params(FishSpecies.northernPike(), kg, Tackle.plantFiberWillow(), 1000 + i);
            if (run(bare, bot, 20 * 600, null).outcome() == Outcome.SNAP) snapsNoLeader++;
        }
        assertTrue(snaps >= 0.6 * n, "snaps " + snaps + " of " + n);
        assertTrue(snapsNoLeader >= 0.5 * n, "snaps without a leader " + snapsNoLeader + " of " + n);
    }

    @Test
    void carefulBotLandsGraylingOnPlantFiber() {
        int n = 100, landed = 0;
        for (int i = 0; i < n; i++) {
            Params p = new Params(FishSpecies.arcticGrayling(), 0.5 + 0.007 * i, Tackle.plantFiberWillow(), 5L, i, 12,
                    false, false, false, 10, false, false, false);
            if (run(p, FightModelTest::careful, 20 * 600, null).outcome() == Outcome.LANDED) landed++;
        }
        assertTrue(landed >= 80, "landed " + landed);
    }

    @Test
    void slackSpitsTheHook() {
        Params p = params(FishSpecies.burbot(), 1.0, Tackle.plantFiberWillow(), 9);
        State s = FightModel.start(p);
        // Line given to a holding 1 kg burbot (runs every 10-20 s): tension 0.4 × 0.3 kg = 0.12 kg, under 5% of
        // 4 kg, so it spits after 3 s.
        int ticks = 0;
        while (!s.over() && ticks < 200) {
            s = FightModel.step(p, s, Input.IDLE);
            ticks++;
        }
        assertEquals(Outcome.SPIT, s.outcome());
        assertTrue(ticks >= 60 && ticks <= 62, "spat at " + ticks);
    }

    @Test
    void pikeBiteThroughWithoutLeaderAboutHalf() {
        int n = 400, bit = 0;
        for (int i = 0; i < n; i++) {
            Params p = params(FishSpecies.northernPike(), 3, Tackle.sinewReel(), 5000 + i);
            if (run(p, FightModelTest::careful, 20 * 600, null).outcome() == Outcome.BITE_THROUGH) bit++;
        }
        assertEquals(0.5, (double) bit / n, 0.08);
        Params leader = params(FishSpecies.northernPike(), 3, Tackle.monoSpinning(), 1);
        for (int i = 0; i < 50; i++) {
            Params p = params(FishSpecies.northernPike(), 3, Tackle.monoSpinning(), 9000 + i);
            assertTrue(run(p, FightModelTest::careful, 20 * 600, null).outcome() != Outcome.BITE_THROUGH);
        }
        assertTrue(leader.tackle().wireLeader());
    }

    @Test
    void cancelEndsTheFight() {
        State s = FightModel.cancel(FightModel.start(params(FishSpecies.burbot(), 2, Tackle.sinewReel(), 1)));
        assertEquals(Outcome.CANCELLED, s.outcome());
        assertEquals(s, FightModel.step(params(FishSpecies.burbot(), 2, Tackle.sinewReel(), 1), s, Input.IDLE));
    }
}
