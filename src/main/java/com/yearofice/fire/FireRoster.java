package com.yearofice.fire;

import com.yearofice.sim.fire.FireRosterMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The fire roster (Every concept from the video › Fire time and the fire roster): every lit fire a player built, within
 * 2 km, read from the loaded rings. A ring remembers who laid its first part or struck it ({@code builder}), so the roster
 * needs no store of its own: it is the index of loaded rings, filtered. Fires in unloaded chunks drop off the list
 * until the chunk loads (the roster page and a persistent per-player store follow with the Fires screen).
 */
public final class FireRoster {
    private FireRoster() {}

    /** One listed fire: where, its row and the words to show. */
    public record Entry(BlockPos pos, FireRosterMath.Row row, String readout) {}

    /** The fires {@code p} built that are lit, least fuel first, at most 16. */
    public static List<Entry> of(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        List<Entry> all = new ArrayList<>();
        for (BlockPos pos : FireIndex.INSTANCE.rings(level)) {
            if (!(level.getBlockEntity(pos) instanceof FireRingBlockEntity be) || be.isRemoved() || !p.getUUID().equals(be.builder())) continue;
            double d = Math.sqrt(pos.distToCenterSqr(p.getX(), p.getY(), p.getZ()));
            all.add(new Entry(pos, be.rosterRow(d), be.readout(FireConfig.tuning())));
        }
        List<FireRosterMath.Row> kept = FireRosterMath.sorted(all.stream().map(Entry::row).toList());
        List<Entry> out = new ArrayList<>();
        for (FireRosterMath.Row r : kept) for (Entry e : all) if (e.row() == r) out.add(e);
        out.sort(Comparator.comparingDouble(e -> e.row().minutesLeft()));
        return out;
    }

    public static String header(List<Entry> entries) { return FireRosterMath.header(entries.stream().map(Entry::row).toList()); }
}
