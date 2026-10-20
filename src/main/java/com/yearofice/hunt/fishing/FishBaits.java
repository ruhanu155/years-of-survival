package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.FishingTuning;
import com.yearofice.sim.fishing.HookBait;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Which items go on a hook (Blueprint › Fishing in depth › Tackle, Bait; Waters and fish › Fishing ids). Each item
 * maps to the bait key the species files price ({@code bait: {roe: 2.0, spoon: 1.2, ...}}). Baits are used up on the
 * hook (a bite count and a clock); a lure (spoon, fly) stays on the line and is not used up.
 */
public final class FishBaits {
    private FishBaits() {}

    private static final int LURE_BITES = 1000, LURE_MINUTES = 100_000;

    /** The bait key an item carries, or empty if it is not bait. */
    public static Optional<String> key(ItemStack s) {
        if (s.isEmpty()) return Optional.empty();
        Item i = s.getItem();
        if (i == FishingRegistry.WORM.get()) return Optional.of("worm");
        if (i == FishingRegistry.ROE.get()) return Optional.of("roe");
        if (i == FishingRegistry.FISH_CHUNK.get()) return Optional.of("fish_chunk");
        if (i == FishingRegistry.FISH_GUTS.get()) return Optional.of("guts");
        // a whole fish cut up on the hook is fish_chunk bait (Waters and fish: "cut from any fish")
        if (FishingRegistry.FISH.values().stream().anyMatch(d -> d.get() == i)) return Optional.of("fish_chunk");
        if (i == FishingRegistry.SPOON.get()) return Optional.of("spoon");
        if (i == FishingRegistry.FLY.get()) return Optional.of("fly");
        return Optional.empty();
    }

    public static boolean isLure(String key) { return key.equals("spoon") || key.equals("fly"); }

    /** The bait as it sits on a fresh hook. */
    public static HookBait fresh(String key, FishingTuning t) {
        if (isLure(key)) return HookBait.fitted(key, LURE_BITES, LURE_MINUTES);
        return switch (key) {
            case "worm" -> HookBait.fitted(key, t.wormBites(), t.wormMinutes());
            case "roe" -> HookBait.fitted(key, 3, 30);
            default -> HookBait.fitted(key, 3, 60);
        };
    }
}
