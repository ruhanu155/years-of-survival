package com.yearofice.api.decay;

import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Layer 5, Decay and freshness: the only way other systems read or write {@code yearofice:fresh} (Minecraft
 * development › Layer 5, API). The decay package implements it and installs itself at mod construction; callers use
 * {@link #get()}.
 *
 * <p>The slice has one clock, handle {@link #CLOCK_AIR_SLICE} ({@code air:slice}): each game hour adds f_T of the
 * valley floor's air that hour from {@code EnvService} (one hour at 5 °C is 1.0), summed from May 1 of year 1. In M5
 * {@code clockAt} starts returning real keys and stacks are rehomed. Works on both sides: the client computes D(now)
 * from the synced {@code decay_heads} head for tooltips.
 */
public interface DecayService {
    int CLOCK_AIR_SLICE = 0;

    /** Decay-hours on the slice clock at a game minute (minutes from day 0's midnight), past or future. */
    double dAt(double gameMinute);

    /** Decay-hours on the slice clock now. */
    double dNow(Level level);

    /** The clock a stack at this level sits on (the slice has one). */
    default int clockAt(Level level) { return CLOCK_AIR_SLICE; }

    /** Stamps a food stack new now in {@code state}, from its {@code yearofice:food} row; null if it has no life for that state. */
    @Nullable Fresh stamp(Level level, ItemStack stack, FoodState state);

    /** The stack's freshness, settled now (a stack past p = 1 is flagged rotten); null if it carries none. */
    @Nullable Fresh settle(Level level, ItemStack stack);

    /** Decay progress p now, 0 new to 1 rotten; 0 for a stack without freshness. */
    double progress(Level level, ItemStack stack);

    /** The food stage now: 0 Fresh, 1 Aging, 2 Turning, 3 Spoiled, 4 Rotten. */
    int stage(Level level, ItemStack stack);

    /** Game days until p reaches {@code target} where the stack sits now (∞ if frozen or never). */
    double daysUntil(Level level, ItemStack stack, double target);

    /** Changes the stack's state (cooking), keeping its decay progress on the new state's life. */
    void changeState(Level level, ItemStack stack, FoodState state);

    /** Ages a stack by {@code days} game days at its clock's temperature ({@code /yoi decay advance}). */
    void age(Level level, ItemStack stack, double days);

    /** The air the slice clock reads now (the valley floor's, this hour), °C. */
    double airC(Level level);

    static DecayService get() {
        DecayService s = Holder.service;
        if (s == null) throw new IllegalStateException("DecayService not installed yet");
        return s;
    }

    /** Called once by the decay package at mod construction. */
    static void install(DecayService service) {
        if (Holder.service != null) throw new IllegalStateException("DecayService already installed");
        Holder.service = service;
    }

    final class Holder {
        private static volatile DecayService service;

        private Holder() {}
    }
}
