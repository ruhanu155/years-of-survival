package com.yearofice.api.body;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Sleep (Blueprint › Survival tuning › Sleep; Minecraft development › Vanilla needs replaced, "Beds and the night
 * skip"): our beds only, any hour the rules allow, no spawn point, no night skip. A bed block calls {@link #lieDown};
 * the clock sees the vanilla sleeping pose and runs the Sleep rate; the body tick reads {@link #spot} for the bed's
 * quality and warmth; the wake rules end the sleep through {@link BodyService#wake}. The body package implements it.
 */
public interface SleepService {

    /** Why a player could not lie down; NONE means they are asleep now. */
    enum Refusal { NONE, NOT_TIRED, OCCUPIED, OBSTRUCTED, NOT_A_BED, BUSY }

    /** Where a sleeper lies: the sleep quality factor q, the bed's insulation (°C) and the bed, or null on the ground. */
    record Spot(double quality, double warmthC, @Nullable BlockPos bed) {}

    /**
     * Lies {@code player} down on the bed at {@code head} (the bed's head half): the vanilla sleeping pose, with none
     * of vanilla's checks (time of day, monsters, the respawn point). The Blueprint's "when the player can sleep" rule
     * applies instead.
     */
    default Refusal lieDown(ServerPlayer player, BlockPos head) {
        return lieDown(player, head, com.yearofice.api.clock.ClockService.installed()
                ? com.yearofice.api.clock.ClockService.get().nowMinute() : 0);
    }

    /** As {@link #lieDown(ServerPlayer, BlockPos)}, with "when the player can sleep" read at absolute minute {@code minute}. */
    Refusal lieDown(ServerPlayer player, BlockPos head, long minute);

    /** The bed under a sleeping player now (quality 0 for a player who is awake). */
    Spot spot(ServerPlayer player);

    /**
     * Runs the wake rules on a sleeper at absolute game minute {@code minute}; a cause wakes them through
     * {@link BodyService#wake} and is returned by id ({@code cold}, {@code morning} ...), else null.
     */
    @Nullable String check(ServerPlayer player, long minute);

    static SleepService get() {
        SleepService s = Holder.service;
        if (s == null) throw new IllegalStateException("SleepService not installed yet");
        return s;
    }

    static boolean installed() { return Holder.service != null; }

    /** Called once by the body package at mod construction. */
    static void install(SleepService service) {
        if (Holder.service != null) throw new IllegalStateException("SleepService already installed");
        Holder.service = service;
    }

    final class Holder {
        private static volatile SleepService service;

        private Holder() {}
    }
}
