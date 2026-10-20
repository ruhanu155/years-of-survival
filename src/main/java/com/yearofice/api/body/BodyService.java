package com.yearofice.api.body;

import com.yearofice.sim.body.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Layer 7, Body: the only way other systems read or change a player's body (Minecraft development › Layer 7 and
 * Body tab › Java classes). The body package implements it and installs itself at mod construction; callers use
 * {@link #get()}.
 *
 * <p>This is the M2.5 slice: vitals, eating, drinking, the one injury path and waking. {@code canDraw},
 * {@code effort}, {@code scent}, {@code partHealth}, {@code factor}, {@code canUse} and {@code limp} land with
 * M4 and M6.
 */
public interface BodyService {

    /** Warmth meter, 0–100. */
    double warmth(ServerPlayer player);

    /** Condition, 0–100. */
    double condition(ServerPlayer player);

    /** Strength = Condition · clamp((BMI − 17) / 6, 0, 1); derived, never stored. */
    double strength(ServerPlayer player);

    double bmi(ServerPlayer player);

    /** Energy in bolts, 0–10. */
    double energy(ServerPlayer player);

    /** Water meter, 0–100 (1 point is 30 mL). */
    double water(ServerPlayer player);

    /** The vitals as the HUD and journal read them. */
    BodyReadout readout(ServerPlayer player);

    /** Condition and warmth over 30, not in water (encounters may stage). */
    boolean isSafeToStage(ServerPlayer player);

    /**
     * The only way an injury reaches a player (DIRECTOR RULING 90): animals, falls, tools and fire alike.
     *
     * @param cause a short id for the journal and logs, for example {@code "fall"} or {@code "yearofice:bear_bite"}
     */
    void applyInjury(ServerPlayer player, InjurySpec injury, String cause);

    /**
     * Eats {@code grams} of {@code stack}'s food. Reads {@code data_maps/item/food.json}; returns false when the
     * item has no food row or the stomach is full.
     */
    boolean eat(ServerPlayer player, ItemStack stack, double grams);

    /** Drinks {@code litres} from a source of the given purity; untreated water rolls gut illness. */
    void drink(ServerPlayer player, double litres, WaterPurity.Source source, boolean boiled);

    /** What kind of water the open water at {@code pos} is (river, lake, a beaver pond): the purity a fill or a sip carries. */
    WaterPurity.Source waterSourceAt(ServerLevel level, BlockPos pos);

    /** Ends sleep or waiting at once and says why. */
    void wake(ServerPlayer player, String cause);

    /** Starts an illness register file ({@code taste_sick}, {@code poison_plant}, ...), rolling its onset; false for an unknown id. */
    boolean beginIllness(ServerPlayer player, String illnessId);

    static BodyService get() {
        BodyService s = Holder.service;
        if (s == null) throw new IllegalStateException("BodyService not installed yet");
        return s;
    }

    /** Called once by the body package at mod construction. */
    static void install(BodyService service) {
        if (Holder.service != null) throw new IllegalStateException("BodyService already installed");
        Holder.service = service;
    }

    final class Holder {
        private static volatile BodyService service;

        private Holder() {}
    }
}
