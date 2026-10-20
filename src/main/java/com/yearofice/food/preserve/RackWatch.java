package com.yearofice.food.preserve;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The seam between the racks and the thieves at the rack (video 2, A3; Structures and buildables › Thieves at the
 * rack). The rolls themselves are the Ecosystem's ({@code TheftService}, lane A); the Food department's racks and cache
 * join the watch when they load, and only while the player's {@code rack_thieves} toggle is on (on by default in every
 * preset). Ecosystem points {@link #watcher} at {@code TheftService::watch} and {@link #toggle} at
 * {@code TheftService::enabled}; until then the seam does nothing, so racks lose nothing to thieves.
 */
public final class RackWatch {
    private RackWatch() {}

    /** Receives each rack or cache that should be watched for thieves. */
    public static volatile BiConsumer<ServerLevel, BlockPos> watcher = (level, pos) -> {};
    /** The player's {@code rack_thieves} option; default on. */
    public static volatile BooleanSupplier toggle = () -> true;

    public static boolean enabled() { return toggle.getAsBoolean(); }

    /** Called when a rack or cache loads or is placed. */
    public static void watch(ServerLevel level, BlockPos pos) {
        if (enabled()) watcher.accept(level, pos);
    }
}
