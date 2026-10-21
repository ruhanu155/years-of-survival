package com.yearofice.ecosystem.pool;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Scent that has to be re-posted to last: a rack with meat, a smokehouse with a fire, a fish-cleaning spot. A poster
 * calls {@link #post} whenever it steps an hour; a post lapses after 3 game hours without one. (Gut piles stand in
 * {@code ScentPoints}; carcasses are read from their entities.)
 */
public final class ScentSources {
    private ScentSources() {}

    public static final String RACK = "rack_meat", SMOKEHOUSE = "smokehouse", CLEANING = "cleaning_spot";

    public record Post(BlockPos pos, String kind, double strength, long untilMinute) {}

    private static final Map<ResourceKey<Level>, Map<BlockPos, Post>> POSTS = new ConcurrentHashMap<>();

    public static void post(ServerLevel level, BlockPos pos, String kind, double strength) {
        long now = com.yearofice.ecosystem.clock.SliceClock.minute(level);
        POSTS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(pos.immutable(), new Post(pos.immutable(), kind, strength, now + 180));
    }

    /** The live posts at this minute; lapsed ones are dropped. */
    public static Map<BlockPos, Post> live(ServerLevel level, long minute) {
        Map<BlockPos, Post> m = POSTS.get(level.dimension());
        if (m == null) return Map.of();
        Iterator<Post> it = m.values().iterator();
        Map<BlockPos, Post> out = new HashMap<>();
        while (it.hasNext()) {
            Post p = it.next();
            if (minute > p.untilMinute()) it.remove();
            else out.put(p.pos(), p);
        }
        return out;
    }

    public static void clear() { POSTS.clear(); }
}
