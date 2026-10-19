package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.FightModel;
import java.util.HashMap;
import java.util.Map;

/**
 * The server's half of the lockstep fight (Minecraft development › Minigames in Minecraft terms: "Lockstep:
 * {@code sim.fishing.FightModel} on both sides, 20 steps a second"). The client steps the model with its own inputs
 * and sends them, one byte a tick, in batches ({@code fish_input}); the server steps the same model with the same
 * bytes, so both reach the same state, and sends its state 5 times a second ({@code fish_state}) so a client that
 * drifted snaps back. The server's state is the one that counts.
 *
 * <p>A client that falls more than {@code timeoutTicks} behind real time has idle input stepped for it, so a
 * frozen or vanished client cannot hold a fish forever. Inputs for ticks already stepped are ignored, as are
 * inputs from further ahead than real time allows.
 */
public final class FightSession {
    public static final int SEND_EVERY = 4;
    static final int AHEAD_SLACK = 20;

    final int fightId;
    final FightModel.Params params;
    final long openedTick;
    final int timeoutTicks;
    private final Map<Integer, Byte> inputs = new HashMap<>();
    private FightModel.State state;
    private int lastSent;

    FightSession(int fightId, FightModel.Params params, long openedTick, int timeoutTicks) {
        this.fightId = fightId;
        this.params = params;
        this.openedTick = openedTick;
        this.timeoutTicks = timeoutTicks;
        this.state = FightModel.start(params);
    }

    public int fightId() { return fightId; }

    public FightModel.State state() { return state; }

    public FightModel.Params params() { return params; }

    public boolean over() { return state.over(); }

    /** Side pressure: the assist sets it against the fish; without it the slice holds the rod in line (no heading yet). */
    public static FightModel.Side side(boolean assist) { return assist ? FightModel.Side.AGAINST : FightModel.Side.INLINE; }

    /** The input the model reads: the client's byte with the side forced to the slice's rule. */
    public static FightModel.Input sanitize(byte b, boolean assist) {
        FightModel.Input in;
        try {
            in = FightModel.Input.fromByte(b);
        } catch (IllegalArgumentException e) {
            in = FightModel.Input.IDLE;
        }
        return new FightModel.Input(in.retrieve(), in.lift(), side(assist), in.walkToward(), in.grab());
    }

    /** Accepts the client's inputs for ticks {@code first}, {@code first + 1}, … and steps as far as they reach. */
    public void accept(int first, byte[] bytes, long now) {
        long allowed = (now - openedTick) + AHEAD_SLACK;
        for (int i = 0; i < bytes.length; i++) {
            int t = first + i;
            if (t <= state.tick() || t > allowed) continue;
            inputs.putIfAbsent(t, bytes[i]);
        }
        stepAvailable();
    }

    /** Each server tick: idle input for any tick the client is too late with. */
    public void serverTick(long now) {
        long due = (now - openedTick) - timeoutTicks;
        while (!state.over() && state.tick() < due) {
            inputs.putIfAbsent(state.tick() + 1, FightModel.Input.IDLE.toByte());
            stepAvailable();
        }
    }

    private void stepAvailable() {
        while (!state.over()) {
            Byte b = inputs.remove(state.tick() + 1);
            if (b == null) break;
            state = FightModel.step(params, state, sanitize(b, params.assist()));
        }
    }

    /** Ends the fight at once (the angler left, switched hands or died). */
    public void cancel() { state = FightModel.cancel(state); }

    /** True when a state payload is due: every {@value #SEND_EVERY} steps and at the end. */
    boolean sendDue() {
        if (state.over() || state.tick() >= lastSent + SEND_EVERY) {
            lastSent = state.tick();
            return true;
        }
        return false;
    }
}
