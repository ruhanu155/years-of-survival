package com.yearofice.ecosystem.theft;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.item.YoiDataMaps;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.ecosystem.Theft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Theft by animals at the camp's stores (Structures and buildables › Thieves at the rack; Items › Theft by animals),
 * the server side of {@link Theft}: classifies a store's stacks by item tag, builds the context from the world,
 * rolls the hour from a stream keyed by the store and the hour, and moves the item out. Behind the player's toggle
 * ({@code rack_thieves}, RECONCILE 170): off, no animal takes anything and no stream is drawn.
 *
 * <p>Stores join with {@link #watch}; the drying and smoke racks and the cache (Food department, M5) call it when
 * placed. Each game hour every watched store in a loaded chunk rolls once. Item tags: {@code yearofice:theft_meat},
 * {@code theft_fish}, {@code theft_hide}, {@code theft_berry_seed}.
 */
public final class TheftService {
    private TheftService() {}

    static final TagKey<Item> MEAT = tag("theft_meat"), FISH = tag("theft_fish"), HIDE = tag("theft_hide"), BERRY_SEED = tag("theft_berry_seed");
    private static volatile Boolean override;
    private static final Map<BlockPos, ServerLevel> WATCHED = new HashMap<>();
    /** Thefts since the server started, for {@code /yoi theft status}. */
    private static int thefts;

    static TagKey<Item> tag(String name) { return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, name)); }

    /** The player's toggle: the runtime override if set, else the config default (on). */
    public static boolean enabled() { return override != null ? override : EcoConfig.rackThievesDefault(); }

    public static void setEnabled(Boolean on) { override = on; }

    public static int thefts() { return thefts; }

    public static void watch(ServerLevel level, BlockPos pos) { synchronized (WATCHED) { WATCHED.put(pos.immutable(), level); } }

    public static void unwatch(ServerLevel level, BlockPos pos) { synchronized (WATCHED) { WATCHED.remove(pos); } }

    public static void clear() {
        synchronized (WATCHED) { WATCHED.clear(); }
        override = null;
        thefts = 0;
    }

    /** What a stack is to a thief, or null when no thief wants it. */
    static Theft.Stack classify(ItemStack s) {
        if (s.isEmpty()) return null;
        Theft.Kind kind = s.is(MEAT) ? Theft.Kind.MEAT : s.is(FISH) ? Theft.Kind.FISH : s.is(HIDE) ? Theft.Kind.HIDE : s.is(BERRY_SEED) ? Theft.Kind.BERRY_SEED : null;
        if (kind == null) return null;
        double kg = YoiDataMaps.mapWeightG(s).map(g -> g / 1000.0).orElse(0.1);
        return new Theft.Stack(kind, kg, s.getCount());
    }

    /**
     * One game hour at one store. Returns the theft, or empty if the toggle is off, nobody came, or nothing was
     * wanted. The stream is keyed by the store position and the hour index, so replaying missed hours gives the
     * same thefts.
     */
    public static Optional<String> hour(ServerLevel level, BlockPos at, Container store, long hourIndex, Theft.Context ctx) {
        if (!enabled()) return Optional.empty();
        List<Theft.Stack> stacks = new ArrayList<>();
        int[] slot = new int[store.getContainerSize()];
        int n = 0;
        for (int i = 0; i < store.getContainerSize(); i++) {
            Theft.Stack st = classify(store.getItem(i));
            if (st == null) continue;
            slot[n++] = i;
            stacks.add(st);
        }
        if (stacks.isEmpty()) return Optional.empty();
        var r = Thresholds.stream(level.getSeed(), "theft", at.getX(), at.getY(), at.getZ(), hourIndex);
        Optional<Theft.Hit> hit = Theft.rollHour(ctx, EcoConfig.tuning().theftMult(), r, stacks);
        if (hit.isEmpty()) return Optional.empty();
        Theft.Hit h = hit.get();
        ItemStack s = store.getItem(slot[h.stack()]);
        String name = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        s.shrink(h.units());
        store.setChanged();
        thefts++;
        return Optional.of(h.thief().name().toLowerCase() + " took " + h.units() + " " + name);
    }

    /** The context at a store this minute: night from the clock, roof from the sky, the nearest player. */
    public static Theft.Context context(ServerLevel level, BlockPos at, long minute) {
        int hourOfDay = (int) ((minute / 60) % 24);
        boolean night = hourOfDay >= 22 || hourOfDay < 5;
        double nearest = Double.MAX_VALUE;
        for (Player p : level.players()) if (!p.isSpectator()) nearest = Math.min(nearest, Math.sqrt(p.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5)));
        boolean roofed = !level.canSeeSky(at.above());
        return new Theft.Context(night, false, roofed, false, false, false, spruceNear(level, at), nearest);
    }

    /** A spruce log or leaf block within 12 blocks stands in for "within 300 m of spruce". */
    static boolean spruceNear(ServerLevel level, BlockPos at) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -12; dx <= 12; dx += 3)
            for (int dz = -12; dz <= 12; dz += 3)
                for (int dy = 0; dy <= 8; dy += 2) {
                    m.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (BuiltInRegistries.BLOCK.getKey(level.getBlockState(m).getBlock()).getPath().contains("spruce")) return true;
                }
        return false;
    }

    /** Each game hour, every watched store in a loaded chunk rolls once. */
    public static void onTick(ServerTickEvent.Post e) {
        long ticksPerHour = Math.round(SliceClock.TICKS_PER_MINUTE * 60);
        if (e.getServer().getTickCount() % ticksPerHour != 0 || !enabled()) return;
        List<Map.Entry<BlockPos, ServerLevel>> list;
        synchronized (WATCHED) { list = new ArrayList<>(WATCHED.entrySet()); }
        for (var en : list) {
            ServerLevel level = en.getValue();
            BlockPos pos = en.getKey();
            if (!level.isLoaded(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof Container c)) {
                synchronized (WATCHED) { WATCHED.remove(pos); }
                continue;
            }
            long minute = SliceClock.minute(level);
            hour(level, pos, c, minute / 60, context(level, pos, minute)).ifPresent(msg -> YearOfIce.LOG.info("theft at {}: {}", pos.toShortString(), msg));
        }
    }

    // ---- M6 lane C: live thieves (jays and ravens in the air) -------------------------------------------------------

    /** The watched stores in this level within {@code r} blocks of {@code pos}. */
    public static List<BlockPos> storesNear(ServerLevel level, BlockPos pos, double r) {
        List<BlockPos> out = new ArrayList<>();
        synchronized (WATCHED) {
            for (var en : WATCHED.entrySet()) {
                if (en.getValue() == level && en.getKey().distSqr(pos) <= r * r) out.add(en.getKey());
            }
        }
        return out;
    }

    /** This thief's chance per game hour at this store, as the hourly roll would use it (0 when the toggle is off). */
    public static double chancePerHour(ServerLevel level, BlockPos at, Theft.Thief thief) {
        if (!enabled()) return 0;
        long minute = SliceClock.minute(level);
        return Theft.chance(thief, context(level, at, minute), EcoConfig.tuning().theftMult());
    }

    /**
     * A live bird reached the store and takes what it can carry. Unlike {@link #hour} this is not a roll: the
     * bird's own visit chance ({@link #chancePerHour}) already decided it came. Returns what it took, if anything.
     */
    public static Optional<String> birdVisit(ServerLevel level, BlockPos at, Theft.Thief thief) {
        if (!enabled() || !(level.getBlockEntity(at) instanceof Container store)) return Optional.empty();
        for (int i = 0; i < store.getContainerSize(); i++) {
            Theft.Stack st = classify(store.getItem(i));
            if (st == null || !Theft.takes(thief, st)) continue;
            ItemStack s = store.getItem(i);
            int units = Math.min(s.getCount(), Theft.unitsTaken(thief, st));
            String name = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            s.shrink(units);
            store.setChanged();
            thefts++;
            return Optional.of(thief.name().toLowerCase() + " took " + units + " " + name);
        }
        return Optional.empty();
    }
}
