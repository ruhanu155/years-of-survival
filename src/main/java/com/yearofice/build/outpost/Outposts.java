package com.yearofice.build.outpost;

import com.yearofice.sim.build.OutpostDecay;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Stamps, ages and audits outposts (Open world in depth › Outposts; Decay and time › Wood). */
public final class Outposts {
    private Outposts() {}

    /** One block the layout wants. */
    public record Piece(BlockPos pos, BlockState state, String role) {}

    /** The blocks the layout wants at {@code origin}: later fills win, and a clear fill takes earlier blocks away. */
    public static List<Piece> layout(OutpostDef def, BlockPos origin) {
        Map<BlockPos, Piece> m = new LinkedHashMap<>();
        for (OutpostDef.Fill f : def.fills()) {
            for (int x = f.from()[0]; x <= f.to()[0]; x++)
                for (int y = f.from()[1]; y <= f.to()[1]; y++)
                    for (int z = f.from()[2]; z <= f.to()[2]; z++) {
                        BlockPos p = origin.offset(x, y, z);
                        if (f.state().isAir()) m.remove(p);
                        else m.put(p, new Piece(p, f.state(), f.role()));
                    }
        }
        return new ArrayList<>(m.values());
    }

    /** Places the outpost new (clears the box first). */
    public static void stamp(ServerLevel l, BlockPos origin, OutpostDef def) {
        for (int x = 0; x < def.size()[0]; x++)
            for (int y = 0; y < def.size()[1]; y++)
                for (int z = 0; z < def.size()[2]; z++) l.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        for (Piece p : layout(def, origin)) l.setBlock(p.pos(), p.state(), 2);
    }

    /**
     * Ages the outpost {@code years}: roof pieces roll first at their exposed rate, then every other piece at the
     * sheltered rate if something still roofs it and the exposed one if it has gone, so a lost roof rots the floor.
     * Returns the pieces lost.
     */
    public static int age(ServerLevel l, BlockPos origin, OutpostDef def, double years, long seed) {
        int lost = 0;
        List<Piece> all = layout(def, origin);
        for (int pass = 0; pass < 2; pass++) {
            for (Piece p : all) {
                if ((p.role().equals("roof")) != (pass == 0)) continue;
                BlockState now = l.getBlockState(p.pos());
                DecayRow row = DecayRow.of(now);
                if (row == null || now.isAir()) continue;
                boolean open = pass == 0 || !roofed(l, p.pos());
                double rate = open ? row.rotPctExposed() : row.rotPctSheltered();
                if (OutpostDecay.lost(seed, p.pos().getX(), p.pos().getY(), p.pos().getZ(), rate, years)) {
                    l.setBlock(p.pos(), row.leftover(), 2);
                    lost++;
                }
            }
        }
        return lost;
    }

    /** Something solid above within 6 blocks (placed or left standing). */
    static boolean roofed(ServerLevel l, BlockPos p) {
        for (int i = 1; i <= 6; i++) if (!l.getBlockState(p.above(i)).isAir()) return true;
        return false;
    }

    /** The repair list: wanted pieces that are missing or have rotted away. */
    public static List<Piece> missing(ServerLevel l, BlockPos origin, OutpostDef def) {
        List<Piece> out = new ArrayList<>();
        for (Piece p : layout(def, origin)) if (l.getBlockState(p.pos()) != p.state()) out.add(p);
        return out;
    }
}
