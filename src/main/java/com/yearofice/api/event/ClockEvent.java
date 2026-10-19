package com.yearofice.api.event;

import net.neoforged.bus.api.Event;

/**
 * Clock events (Minecraft development › Layer 1, Clock events; Events and hooks), posted on
 * {@code NeoForge.EVENT_BUS} by {@code story.clock} at the end of the server tick that crosses them. Order within
 * a tick: {@link Minute} once, then {@link Hour}, {@link Day}, {@link Dawn} and {@link Dusk} in time order. A step
 * of more than 60 game minutes in one tick (the SOS flight, an admin time set) posts only {@link Jump}, and every
 * listener runs its closed form. Listener priorities: fires and rooms HIGH, body NORMAL, decay LOW.
 */
public abstract class ClockEvent extends Event {

    /** One tick's crossed minutes: the first crossed and how many (at most 60). */
    public static final class Minute extends ClockEvent {
        private final long first;
        private final int count;

        public Minute(long first, int count) {
            this.first = first;
            this.count = count;
        }

        /** The first absolute minute crossed this tick. */
        public long first() { return first; }

        /** Minutes crossed this tick, 1–60. */
        public int count() { return count; }

        /** The last absolute minute crossed (the minute now). */
        public long last() { return first + count - 1; }
    }

    /** The clock reached a whole hour. */
    public static final class Hour extends ClockEvent {
        private final long minute;

        public Hour(long minute) { this.minute = minute; }

        public long minute() { return minute; }

        public int hourOfDay() { return (int) Math.floorMod(minute / 60, 24L); }
    }

    /** 00:00 of a new day. */
    public static final class Day extends ClockEvent {
        private final long absDay;

        public Day(long absDay) { this.absDay = absDay; }

        public long absDay() { return absDay; }
    }

    /** The sun climbed through −6° (or the midnight-sun fallback): traps, birds and the chorus key off this. */
    public static final class Dawn extends ClockEvent {
        private final long absDay;
        private final long minute;

        public Dawn(long absDay, long minute) {
            this.absDay = absDay;
            this.minute = minute;
        }

        public long absDay() { return absDay; }

        public long minute() { return minute; }
    }

    /** The sun sank through −6° (or the fallback). {@code absDay} is the day whose evening this is. */
    public static final class Dusk extends ClockEvent {
        private final long absDay;
        private final long minute;

        public Dusk(long absDay, long minute) {
            this.absDay = absDay;
            this.minute = minute;
        }

        public long absDay() { return absDay; }

        public long minute() { return minute; }
    }

    /** More than 60 game minutes in one tick, or any admin set: run the closed form from {@code fromDs} to {@code toDs}. */
    public static final class Jump extends ClockEvent {
        private final long fromDs;
        private final long toDs;

        public Jump(long fromDs, long toDs) {
            this.fromDs = fromDs;
            this.toDs = toDs;
        }

        public long fromDs() { return fromDs; }

        public long toDs() { return toDs; }
    }
}
