package com.yearofice.sim.story;

import java.util.ArrayList;
import java.util.List;

/**
 * One player's guided first night as a state machine (Blueprint › Start site › The guided first night): it runs
 * from waking until the first sleep, one step at a time, and after the first sleep it is gone for good. Each step
 * advances only on {@link FirstNightFacts} the server gathered from game events and checks, never on a timer.
 *
 * <p>Statuses: {@link Status#NEW} until the first join decides; {@link Status#ACTIVE} while the prompts run;
 * {@link Status#DONE} after the player slept by a fire, or stayed up until the first morning; {@link Status#SKIPPED}
 * when the player turned it off, the world has it off, or the player is a returning one. Plain data, no Minecraft.
 */
public final class FirstNight {

    public enum Status { NEW, ACTIVE, DONE, SKIPPED }

    /** How the guided night ended. */
    public enum Ending { NONE, SLEPT, STAYED_UP, SKIPPED, RETURNING, OFF }

    private Status status = Status.NEW;
    private FirstNightStep step = FirstNightStep.TINDER;
    private long startMinute = -1;
    private long stepMinute = -1;
    private long fireLitMinute = -1;
    private long endMinute = -1;
    private Ending ending = Ending.NONE;

    /** What one check changed: the steps it closed, the step now current, and whether the night just ended. */
    public record Advance(List<FirstNightStep> completed, FirstNightStep now, boolean finished) {
        public static final Advance NOTHING = new Advance(List.of(), null, false);

        public boolean moved() { return !completed.isEmpty() || finished; }
    }

    public FirstNight() {}

    /** Restores a saved record (the attachment's codec). */
    public static FirstNight restore(Status status, FirstNightStep step, long startMinute, long stepMinute,
                                     long fireLitMinute, long endMinute, Ending ending) {
        FirstNight n = new FirstNight();
        n.status = status;
        n.step = step;
        n.startMinute = startMinute;
        n.stepMinute = stepMinute;
        n.fireLitMinute = fireLitMinute;
        n.endMinute = endMinute;
        n.ending = ending;
        return n;
    }

    public Status status() { return status; }
    public FirstNightStep step() { return step; }
    public long startMinute() { return startMinute; }
    public long stepMinute() { return stepMinute; }
    /** The minute the first fire caught, or −1. */
    public long fireLitMinute() { return fireLitMinute; }
    public long endMinute() { return endMinute; }
    public Ending ending() { return ending; }
    public boolean active() { return status == Status.ACTIVE; }

    /** Starts the night at step 1 (first join, or {@code /yoi onboarding reset}). */
    public void start(long minute) {
        status = Status.ACTIVE;
        step = FirstNightStep.TINDER;
        startMinute = minute;
        stepMinute = minute;
        fireLitMinute = -1;
        endMinute = -1;
        ending = Ending.NONE;
    }

    /** Turns the night off for good with a reason (the switch, a returning player, {@code /yoi onboarding skip}). */
    public void skip(long minute, Ending why) {
        if (status == Status.DONE) return;
        status = Status.SKIPPED;
        endMinute = minute;
        ending = why;
    }

    /** Back to {@link Status#NEW}: the next join or check decides again. */
    public void reset() {
        status = Status.NEW;
        step = FirstNightStep.TINDER;
        startMinute = stepMinute = fireLitMinute = endMinute = -1;
        ending = Ending.NONE;
    }

    /**
     * One check. Closes the current step and every following one the facts already prove, in order; ends the night
     * when the last step closes, or at {@code firstMorning} for a player who never slept by a fire.
     */
    public Advance advance(FirstNightFacts f, long minute, long firstMorning) {
        if (status != Status.ACTIVE) return Advance.NOTHING;
        List<FirstNightStep> done = new ArrayList<>();
        if (f.fireLit() && fireLitMinute < 0) fireLitMinute = minute;
        while (step.doneBy(f)) {
            done.add(step);
            FirstNightStep next = step.next();
            if (next == null) {
                finish(minute, Ending.SLEPT);
                return new Advance(done, null, true);
            }
            step = next;
            stepMinute = minute;
        }
        if (minute >= firstMorning) {
            finish(minute, Ending.STAYED_UP);
            return new Advance(done, null, true);
        }
        return done.isEmpty() ? Advance.NOTHING : new Advance(done, step, false);
    }

    private void finish(long minute, Ending why) {
        status = Status.DONE;
        endMinute = minute;
        ending = why;
    }
}
