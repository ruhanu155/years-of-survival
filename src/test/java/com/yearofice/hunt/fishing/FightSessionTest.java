package com.yearofice.hunt.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.fishing.FightModel;
import com.yearofice.sim.fishing.FishSpecies;
import com.yearofice.sim.fishing.Tackle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The server half of the lockstep fight: fed the client's input bytes in 4-tick batches that arrive late (150 and
 * 300 ms, as the Blueprint's {@code hunt/fight_lockstep} test asks), it reaches exactly the client's state; a client
 * that goes silent has idle input stepped for it after the timeout, so the fish spits the hook.
 */
class FightSessionTest {
    static FightModel.Params params(long id) {
        return new FightModel.Params(FishSpecies.arcticGrayling(), 0.8, Tackle.plantFiberWillow(), 1234, id, 6, false, false,
                false, 8, false, false, false);
    }

    /** A scripted angler: reel, ease off on runs, grab a tired fish in reach. */
    static byte script(FightModel.State s) {
        boolean run = s.phase() == FightModel.Phase.RUN || s.phase() == FightModel.Phase.JUMP;
        boolean grab = s.phase() == FightModel.Phase.TIRED && s.lineOutM() <= 2;
        return new FightModel.Input(!run, false, FightModel.Side.INLINE, false, grab).toByte();
    }

    @Test
    void delayedBatchesReachTheClientsState() {
        for (int delayTicks : new int[] {3, 6}) {
            FightModel.Params p = params(delayTicks);
            FightSession server = new FightSession(1, p, 0, 60);
            FightModel.State client = FightModel.start(p);
            List<byte[]> batches = new ArrayList<>();
            List<Integer> firsts = new ArrayList<>();
            List<Long> arrive = new ArrayList<>();
            byte[] pending = new byte[FightSession.SEND_EVERY];
            int n = 0, first = 1;
            for (long now = 1; now < 2400 && !server.over(); now++) {
                if (!client.over()) {
                    byte b = script(client);
                    client = FightModel.step(p, client, FightSession.sanitize(b, false));
                    pending[n++] = b;
                    if (n == pending.length || client.over()) {
                        byte[] out = new byte[n];
                        System.arraycopy(pending, 0, out, 0, n);
                        batches.add(out);
                        firsts.add(first);
                        arrive.add(now + delayTicks);
                        first += n;
                        n = 0;
                    }
                }
                while (!arrive.isEmpty() && arrive.get(0) <= now) {
                    server.accept(firsts.remove(0), batches.remove(0), now);
                    arrive.remove(0);
                }
                server.serverTick(now);
            }
            assertTrue(server.over(), "the fight ended");
            assertEquals(client, server.state(), "lockstep at " + delayTicks + " ticks of delay");
            assertEquals(FightModel.Outcome.LANDED, server.state().outcome());
        }
    }

    @Test
    void silentClientLosesTheFish() {
        FightSession s = new FightSession(2, params(9), 0, 60);
        long now = 0;
        while (!s.over() && now < 2000) s.serverTick(++now);
        assertTrue(s.over());
        assertEquals(FightModel.Outcome.SPIT, s.state().outcome(), "slack for 3 s spits the hook");
        assertTrue(now >= 60 + 60, "no idle stepping before the timeout");
    }

    @Test
    void futureAndRepeatedInputsAreIgnored() {
        FightSession s = new FightSession(3, params(11), 100, 60);
        byte reel = new FightModel.Input(true, false, FightModel.Side.INLINE, false, false).toByte();
        s.accept(1, new byte[] {reel, reel}, 101);
        assertEquals(2, s.state().tick());
        s.accept(1, new byte[] {0, 0, reel}, 103);
        assertEquals(3, s.state().tick(), "ticks 1 and 2 were already stepped");
        s.accept(500, new byte[] {reel}, 103);
        assertEquals(3, s.state().tick(), "too far ahead of real time");
        assertEquals(FightModel.Side.AGAINST, FightSession.sanitize(reel, true).side(), "the assist sets side pressure");
    }
}
