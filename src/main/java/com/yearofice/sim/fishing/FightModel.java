package com.yearofice.sim.fishing;

/**
 * The fishing fight as a pure lockstep step function (Blueprint › Fishing methods › The fight, as built; Fish
 * states; Tension; Spikes; Stamina; How a fish is lost; Landing).
 *
 * <p>{@code step(params, state, input)} runs 20 times a real second on both client and server. It uses only
 * {@code + − × ÷}, {@code min} and {@code max} on doubles and draws every random number from a hash of
 * (seed, fight id, tick, salt), never from call order or {@code java.util.Random}, so the same recorded inputs
 * give the same fight on both sides whatever the network delay. Inputs travel as one byte a tick
 * ({@link Input#toByte}).
 *
 * <p><b>From Tide 2.</b> Tide's catch minigame is a timing bar judged on the client with
 * {@code new Random()}, with server-side win, fail and timeout events and a guard that cancels the game when the
 * hook or player vanishes. We keep the lifecycle (explicit terminal outcomes, a cancel path the adapter calls
 * when the hook is gone) and reject the rest: the blueprint's fight is a tension model, and Tide's
 * client-authoritative randomness would break lockstep. No Tide code is copied.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class FightModel {
    private FightModel() {}

    public static final int STEPS_PER_SECOND = 20;
    public static final double DT = 1.0 / STEPS_PER_SECOND;

    public static final double HOLD_PULL = 0.3, HOLD_SPEED = 0.2;
    public static final double TIRED_BELOW = 0.15, TIRED_PULL = 0.1;
    public static final double RUN_SPIKE = 1.3, JUMP_SPIKE = 1.6, JUMP_SPIKE_GIVING = 1.3;
    public static final double SPIKE_S = 0.15, JUMP_AIR_S = 0.8;
    public static final double DIVE_PULL = 0.6;
    public static final double ALL_OUT_FACTOR = 1.5;
    public static final double SPIT_UNDER = 0.05, SPIT_S = 3.0, SPIT_S_GORGE = 6.0, SPIT_RESET_S = 1.0;
    public static final double RECOVER_UNDER = 0.10, RECOVER_S = 5.0, RECOVER_STAMINA = 0.2;
    public static final double RECOVER_PRESSURE_BELOW = 0.2, RECOVER_RATE = 0.25;
    public static final double RED_ABOVE = 0.9;
    public static final double SNAG_GIVE_S = 3.0;
    public static final double LAND_REACH_M = 2.0, NET_REACH_M = 3.0;
    public static final double GRAB_TIRED = 0.9, GRAB_FRESH = 0.3, NET_TIRED = 0.95;
    public static final double WALK_FOLLOW_MPS = 0.7;
    public static final double BRITTLE_ABOVE = 0.8, BRITTLE_CHANCE = 0.02;
    /** For DIVE and BORE species, the share of run starts that are dives. Not in the blueprint; tunable. */
    public static final double DIVE_SHARE = 0.5;

    public enum Phase { HOLD, RUN, JUMP, DIVE, TIRED, LANDED, LOST }

    public enum Outcome { NONE, LANDED, SNAP, SPIT, BITE_THROUGH, SNAG, ROD_BROKE, CANCELLED }

    /** Side pressure relative to the fish's direction: against 1.5, in line 1.0, same side 0.5. */
    public enum Side {
        INLINE(1.0), AGAINST(1.5), SAME(0.5);

        public final double tire;

        Side(double tire) { this.tire = tire; }
    }

    /** The fight's fixed inputs. */
    public record Params(FishSpecies species, double kg, Tackle tackle, long seed, long fightId, double castM,
                         boolean playerWeak, boolean coldHands, boolean assist, double waterTempC,
                         boolean snagsAtSpot, boolean brittleRod, boolean landingNet) {
        public double sEff() { return tackle.effectiveStrengthKg(); }

        public double coldWater() { return waterTempC < 4.0 ? 0.7 : 1.0; }

        public double fullPull() { return species.runPullX() * kg; }
    }

    /** One tick's controls. Encoded as one byte for the {@code fish_input} payload. */
    public record Input(boolean retrieve, boolean lift, Side side, boolean walkToward, boolean grab) {
        public static final Input IDLE = new Input(false, false, Side.INLINE, false, false);

        public byte toByte() {
            int b = (retrieve ? 1 : 0) | (lift ? 2 : 0) | (side.ordinal() << 2) | (walkToward ? 16 : 0) | (grab ? 32 : 0);
            return (byte) b;
        }

        public static Input fromByte(byte v) {
            int b = v & 0xFF;
            int side = (b >> 2) & 3;
            if (side > 2) throw new IllegalArgumentException("bad side bits");
            return new Input((b & 1) != 0, (b & 2) != 0, Side.values()[side], (b & 16) != 0, (b & 32) != 0);
        }
    }

    /** The whole fight state. Immutable; equal states mean equal fights. */
    public record State(int tick, Phase phase, Outcome outcome, double stamina, double lineOutM, double tensionKg,
                        double phaseLeftS, double pendingRunS, double nextRunS, double nextSpecialS,
                        double spikeLeftS, double spikeX, double slackS, double tensionOnS, double calmS,
                        double diveGiveS, boolean biteThroughArmed, boolean specialSeen, boolean reachedRed,
                        double redS) {
        public boolean over() { return phase == Phase.LANDED || phase == Phase.LOST; }

        /** Tension as a share of S_eff, for the arc. */
        public double tensionShare(Params p) { return tensionKg / p.sEff(); }

        State with(Phase ph, Outcome o) {
            return new State(tick, ph, o, stamina, lineOutM, tensionKg, phaseLeftS, pendingRunS, nextRunS,
                    nextSpecialS, spikeLeftS, spikeX, slackS, tensionOnS, calmS, diveGiveS, biteThroughArmed,
                    specialSeen, reachedRed, redS);
        }
    }

    /** The hook is set: a fresh fish at full stamina, line out to the cast, bite-through rolled now. */
    public static State start(Params p) {
        FishSpecies s = p.species();
        boolean armed = !p.tackle().wireLeader() && u(p, 0, 1) < s.biteThroughNoLeader();
        double nextRun = between(s.runEveryS(), u(p, 0, 2));
        double nextSpecial = specialEvery(s, u(p, 0, 3));
        return new State(0, Phase.HOLD, Outcome.NONE, 1.0, p.castM(), 0, 0, 0, nextRun, nextSpecial,
                0, 1, 0, 0, 0, 0, armed, false, false, 0);
    }

    /** The adapter ends a fight when the hook or player is gone (Tide's cancel guard). */
    public static State cancel(State s) { return s.over() ? s : s.with(Phase.LOST, Outcome.CANCELLED); }

    public static State step(Params p, State s, Input in) {
        if (s.over()) return s;
        FishSpecies sp = p.species();
        final int t = s.tick() + 1;
        final double sEff = p.sEff();
        final boolean giving = !in.retrieve();

        Phase phase = s.phase();
        double stamina = s.stamina();
        double phaseLeft = s.phaseLeftS() - DT;
        double pendingRun = s.pendingRunS();
        double nextRun = s.nextRunS();
        double nextSpecial = s.nextSpecialS();
        double spikeLeft = s.spikeLeftS() - DT;
        double spikeX = spikeLeft > 0 ? s.spikeX() : 1.0;
        boolean specialSeen = s.specialSeen();
        double calm = s.calmS();
        boolean newSpike = false;

        // 1. Phase timers.
        switch (phase) {
            case HOLD -> {
                nextRun -= DT;
                if (nextRun <= 0 && stamina >= TIRED_BELOW) {
                    double len = between(sp.runLengthS(), u(p, t, 10));
                    boolean diver = sp.special() == FishSpecies.Special.DIVE || sp.special() == FishSpecies.Special.BORE;
                    if (diver && u(p, t, 11) < DIVE_SHARE) {
                        phase = Phase.DIVE;
                        phaseLeft = len;
                        if (!specialSeen && s.biteThroughArmed()) return lost(s, t, Outcome.BITE_THROUGH);
                        specialSeen = true;
                    } else if (u(p, t, 12) < sp.jumpChance()) {
                        phase = Phase.JUMP;
                        phaseLeft = JUMP_AIR_S;
                        pendingRun = len;
                    } else {
                        phase = Phase.RUN;
                        phaseLeft = len;
                        spikeX = RUN_SPIKE;
                        spikeLeft = SPIKE_S;
                        newSpike = true;
                    }
                }
            }
            case JUMP -> {
                if (phaseLeft <= 0) {
                    phase = Phase.RUN;
                    phaseLeft = pendingRun;
                    pendingRun = 0;
                    spikeX = giving ? JUMP_SPIKE_GIVING : JUMP_SPIKE;
                    spikeLeft = SPIKE_S;
                    newSpike = true;
                }
            }
            case RUN, DIVE -> {
                if (phaseLeft <= 0) {
                    phase = Phase.HOLD;
                    nextRun = between(sp.runEveryS(), u(p, t, 13)) / Math.max(0.25, stamina);
                }
            }
            case TIRED -> {
                // handled with tension below
            }
            default -> { }
        }

        // 2. Head-shakes and twists: spikes on their own clock, outside jumps and tiredness.
        boolean shaker = sp.special() == FishSpecies.Special.HEAD_SHAKE || sp.special() == FishSpecies.Special.TWIST;
        if (shaker && phase != Phase.JUMP && phase != Phase.TIRED) {
            nextSpecial -= DT;
            if (nextSpecial <= 0) {
                if (!specialSeen && s.biteThroughArmed()) return lost(s, t, Outcome.BITE_THROUGH);
                specialSeen = true;
                spikeX = Math.max(spikeX, sp.specialSpike());
                spikeLeft = SPIKE_S;
                newSpike = true;
                nextSpecial = specialEvery(sp, u(p, t, 14));
            }
        }

        // 3. The fish's pull and speed away.
        double run = p.fullPull() * (0.4 + 0.6 * stamina) * p.coldWater();
        double pull;
        double speed;
        switch (phase) {
            case RUN -> { pull = run; speed = Math.min(3.0, 1.0 + 0.1 * p.kg()); }
            case JUMP -> { pull = 0; speed = 0; }
            case DIVE -> { pull = DIVE_PULL * run; speed = 0.5 * Math.min(3.0, 1.0 + 0.1 * p.kg()); }
            case TIRED -> { pull = TIRED_PULL * p.kg(); speed = 0; }
            default -> { pull = HOLD_PULL * p.kg(); speed = HOLD_SPEED; }
        }

        // 4. Line and tension.
        Tackle tk = p.tackle();
        double flex = Math.min(1.0, tk.flex() + (in.lift() ? Tackle.LIFT_FLEX : 0));
        double retrieveSpeed = tk.retrieveSpeedMps() * (in.lift() ? 0.5 : 1) * (p.playerWeak() ? 0.7 : 1)
                * (p.coldHands() ? 0.8 : 1);
        double line = s.lineOutM();
        double tension;
        boolean allOut = line >= tk.lineCarriedM();
        if (phase == Phase.JUMP) {
            tension = 0;
        } else if (allOut) {
            tension = ALL_OUT_FACTOR * pull;
        } else if (in.retrieve()) {
            tension = pull + tk.retrievePullKg();
            boolean gains = phase == Phase.HOLD || phase == Phase.TIRED || (phase == Phase.DIVE && in.lift());
            if (gains) line -= Math.max(0, retrieveSpeed - speed) * DT;
        } else {
            tension = tk.giveLineShare() * pull;
            line += speed * DT;
        }
        if (tk.drag() && tension > tk.dragShare() * sEff) {
            tension = tk.dragShare() * sEff;
            line += speed * DT;
        }
        if (in.walkToward()) line -= WALK_FOLLOW_MPS * DT;
        line = Math.max(0.0, Math.min(tk.lineCarriedM(), line));

        double felt = spikeLeft > 0 ? tension * (1.0 + (spikeX - 1.0) * (1.0 - flex)) : tension;

        if (felt >= sEff) return lost(s, t, Outcome.SNAP);
        if (newSpike && p.brittleRod() && felt > BRITTLE_ABOVE * sEff && u(p, t, 15) < BRITTLE_CHANCE) {
            return lost(s, t, Outcome.ROD_BROKE);
        }
        boolean red = felt > RED_ABOVE * sEff;

        // 5. Stamina.
        double fullPull = p.fullPull();
        if (tension < RECOVER_PRESSURE_BELOW * fullPull) {
            stamina += RECOVER_RATE * sp.tirePerS() * DT;
        } else {
            double tire = sp.tirePerS() * (p.waterTempC() < 4.0 ? 0.8 : 1.0) * (p.assist() ? 1.3 : 1.0);
            stamina -= tire * (tension / fullPull) * in.side().tire * DT;
        }
        stamina = Math.max(0.0, Math.min(1.0, stamina));

        // 6. Tiredness and recovery.
        if (phase != Phase.JUMP && phase != Phase.TIRED && stamina < TIRED_BELOW) {
            phase = Phase.TIRED;
            calm = 0;
        } else if (phase == Phase.TIRED) {
            calm = tension < RECOVER_UNDER * sEff ? calm + DT : 0;
            if (calm >= RECOVER_S) {
                phase = Phase.HOLD;
                stamina = Math.max(stamina, RECOVER_STAMINA);
                nextRun = between(sp.runEveryS(), u(p, t, 16));
                calm = 0;
            }
        }

        // 7. Spit.
        double slack = s.slackS();
        double on = s.tensionOnS();
        if (tension < SPIT_UNDER * sEff) {
            slack += DT;
            on = 0;
        } else {
            on += DT;
            if (on >= SPIT_RESET_S) slack = 0;
        }
        if (slack >= (tk.gorge() ? SPIT_S_GORGE : SPIT_S)) return lost(s, t, Outcome.SPIT);

        // 8. Snag: a diving fish given line for over 3 s at a spot with snags.
        double diveGive = (phase == Phase.DIVE && giving && p.snagsAtSpot()) ? s.diveGiveS() + DT : 0;
        if (diveGive > SNAG_GIVE_S) return lost(s, t, Outcome.SNAG);

        // 9. Landing.
        if (in.grab()) {
            double reach = p.landingNet() ? NET_REACH_M : LAND_REACH_M;
            if (line <= reach) {
                boolean tired = phase == Phase.TIRED;
                double chance = tired ? (p.landingNet() ? NET_TIRED : GRAB_TIRED) : GRAB_FRESH;
                if (u(p, t, 17) < chance) {
                    return new State(t, Phase.LANDED, Outcome.LANDED, stamina, line, tension, 0, 0, 0, 0, 0, 1,
                            slack, on, calm, 0, s.biteThroughArmed(), specialSeen, s.reachedRed() || red,
                            s.redS() + (red ? DT : 0));
                }
                // A failed grab triggers a run.
                phase = Phase.RUN;
                stamina = Math.max(stamina, TIRED_BELOW);
                phaseLeft = between(sp.runLengthS(), u(p, t, 18));
                spikeX = RUN_SPIKE;
                spikeLeft = SPIKE_S;
            }
        }

        return new State(t, phase, Outcome.NONE, stamina, line, tension, Math.max(0, phaseLeft), pendingRun,
                nextRun, nextSpecial, Math.max(0, spikeLeft), spikeX, slack, on, calm, diveGive,
                s.biteThroughArmed(), specialSeen, s.reachedRed() || red, s.redS() + (red ? DT : 0));
    }

    private static State lost(State s, int tick, Outcome o) {
        return new State(tick, Phase.LOST, o, s.stamina(), s.lineOutM(), s.tensionKg(), 0, 0, 0, 0, 0, 1,
                s.slackS(), s.tensionOnS(), s.calmS(), s.diveGiveS(), s.biteThroughArmed(), s.specialSeen(),
                s.reachedRed(), s.redS());
    }

    /** Line wear after a fight: −3% condition on plant fiber, −9% if the arc reached red (Tackle › Lines). */
    public static double lineWearPct(double perFightPct, State end) {
        return end.reachedRed() ? 3.0 * perFightPct : perFightPct;
    }

    private static double between(double[] range, double u) { return range[0] + (range[1] - range[0]) * u; }

    private static double specialEvery(FishSpecies s, double u) {
        double[] r = s.specialEveryS();
        return r[1] > 0 ? between(r, u) : Double.MAX_VALUE;
    }

    /** A uniform draw in [0, 1) from (seed, fight id, tick, salt): SplitMix64's finaliser, pure integer maths. */
    static double u(Params p, int tick, int salt) {
        long z = p.seed() ^ (p.fightId() * 0x9E3779B97F4A7C15L) ^ ((long) tick << 21) ^ ((long) salt * 0xD1B54A32D192ED03L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (z >>> 11) * 0x1.0p-53;
    }
}
