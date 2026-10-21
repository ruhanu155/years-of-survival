package com.yearofice.ecosystem.raid;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.encounter.EncounterConfig;
import com.yearofice.ecosystem.encounter.WorldToggles;
import com.yearofice.ecosystem.theft.TheftService;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.ecosystem.RaidOdds;
import com.yearofice.sim.ecosystem.Theft;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Door entry (video 2, A2; RECONCILE 170): the server side of {@link RaidOdds}. An open door or hatch watched here
 * rolls once a game hour while the player is more than 20 m away; a fox takes one light food item, a wolf or bear
 * stays inside (the standoff rules own it from there). Behind {@code [world] animals_enter_open_doors}. Doors join the
 * watch when a player uses them. Brain activity {@code raid_shelter} and memory {@code open_door_target} carry a
 * visible animal; the visit itself is resolved by roll.
 */
public final class ShelterEntry {
    private ShelterEntry() {}

    /** Wolf pack state at a position (hunger drive, pack within 300 m); the wolf lane replaces this. */
    public static volatile Function<BlockPos, double[]> packNear = p -> new double[] {0, 0};

    private static final Map<BlockPos, ServerLevel> DOORS = new HashMap<>();
    private static int entries;

    public record Entry(RaidOdds.Raider raider, String text) {}

    public static int entries() { return entries; }

    public static void watch(ServerLevel level, BlockPos pos) { synchronized (DOORS) { DOORS.put(pos.immutable(), level); } }

    public static void clear() { synchronized (DOORS) { DOORS.clear(); } entries = 0; }

    public static boolean isOpen(BlockState s) {
        return (s.getBlock() instanceof DoorBlock || s.getBlock() instanceof TrapDoorBlock) && s.getValue(BlockStateProperties.OPEN);
    }

    /** A container with food within 5 blocks of the door, or null. */
    public static Container storeNear(ServerLevel level, BlockPos door) {
        for (BlockPos p : BlockPos.betweenClosed(door.offset(-5, -2, -5), door.offset(5, 2, 5))) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof Container c && scent(c) > 0) return c;
        }
        return null;
    }

    /** Food scent on the Items 0-3 scale: raw meat 0.7, fish 0.4 (dried keeps 0.4), hides and seeds 0. */
    public static double scent(Container c) {
        double s = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            Theft.Stack st = TheftService.classify(c.getItem(i));
            if (st == null) continue;
            if (st.kind() == Theft.Kind.MEAT) s = Math.max(s, 0.7);
            else if (st.kind() == Theft.Kind.FISH) s = Math.max(s, 0.4);
        }
        return s;
    }

    public static RaidOdds.Door context(ServerLevel level, BlockPos door, Container store, long minute) {
        int hod = (int) ((minute / 60) % 24);
        int doy = Calendar.dayOfYearOfMinute(minute);
        double nearest = Double.MAX_VALUE;
        for (Player p : level.players()) if (!p.isSpectator()) nearest = Math.min(nearest, Math.sqrt(p.distanceToSqr(door.getX() + 0.5, door.getY(), door.getZ() + 0.5)));
        double[] pack = packNear.apply(door);
        return new RaidOdds.Door(isOpen(level.getBlockState(door)), hod >= 22 || hod < 5, doy >= 305 || doy < 90,
                store == null ? 0 : scent(store), pack[0], pack[1] > 0, nearest);
    }

    /** One game hour at one door. Empty if the option is off, nobody came, or the door is shut. */
    public static volatile BlockPos streamOriginOverride;

    public static Optional<Entry> hour(ServerLevel level, BlockPos door, Container store, long hourIndex, RaidOdds.Door ctx) {
        if (!WorldToggles.animalsEnterOpenDoors()) return Optional.empty();
        // GameTest hook: a pinned origin keeps the statistical door test on one stream whatever the batch layout
        BlockPos o = streamOriginOverride != null ? streamOriginOverride : door;
        var r = Thresholds.stream(level.getSeed(), "door_entry", o.getX(), o.getY(), o.getZ(), hourIndex);
        Optional<RaidOdds.Raider> hit = RaidOdds.rollHour(ctx, EncounterConfig.doorScale(), r);
        if (hit.isEmpty()) return Optional.empty();
        RaidOdds.Raider who = hit.get();
        entries++;
        String text = switch (who) {
            case FOX -> {
                String took = takeLight(store);
                yield took == null ? "Fox tracks at the cabin door." : "Fox tracks at the cabin door. 1 " + took + " gone.";
            }
            case WOLF -> "Wolf tracks at the cabin door. It is still inside.";
            case BEAR -> "Bear tracks at the cabin door. It is still inside.";
        };
        return Optional.of(new Entry(who, text));
    }

    private static String takeLight(Container store) {
        if (store == null) return null;
        for (int i = 0; i < store.getContainerSize(); i++) {
            ItemStack s = store.getItem(i);
            Theft.Stack st = TheftService.classify(s);
            if (st == null || st.kg() > 1.0 || (st.kind() != Theft.Kind.MEAT && st.kind() != Theft.Kind.FISH)) continue;
            String name = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            s.shrink(1);
            store.setChanged();
            return name;
        }
        return null;
    }

    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel() instanceof ServerLevel sl) {
            BlockPos p = e.getPos();
            if (sl.getBlockState(p).getBlock() instanceof DoorBlock || sl.getBlockState(p).getBlock() instanceof TrapDoorBlock) watch(sl, p);
        }
    }

    public static void onTick(ServerTickEvent.Post e) {
        long ticksPerHour = Math.round(SliceClock.TICKS_PER_MINUTE * 60);
        if (e.getServer().getTickCount() % ticksPerHour != 0 || !WorldToggles.animalsEnterOpenDoors()) return;
        List<Map.Entry<BlockPos, ServerLevel>> list;
        synchronized (DOORS) { list = new ArrayList<>(DOORS.entrySet()); }
        for (var en : list) {
            ServerLevel level = en.getValue();
            BlockPos pos = en.getKey();
            if (!level.isLoaded(pos)) continue;
            BlockState s = level.getBlockState(pos);
            if (!(s.getBlock() instanceof DoorBlock) && !(s.getBlock() instanceof TrapDoorBlock)) { synchronized (DOORS) { DOORS.remove(pos); } continue; }
            long minute = SliceClock.minute(level);
            Container store = storeNear(level, pos);
            hour(level, pos, store, minute / 60, context(level, pos, store, minute))
                    .ifPresent(en2 -> YearOfIce.LOG.info("door entry at {}: {}", pos.toShortString(), en2.text()));
        }
    }
}
