package com.yearofice.build.pieces;

import com.yearofice.api.build.PieceData;
import com.yearofice.sim.build.Stamp;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

/** {@code /yoi struct save|stamp}: a built structure's pieces relative to an anchor, kept as NBT beside the world, replayed as queued jobs. */
public final class Structs {
    private Structs() {}

    public record Saved(List<BlockPos> rel, List<BlockState> states, List<Stamp.Piece> pieces) {}

    static Path file(ServerLevel l, String name) {
        return l.getServer().getWorldPath(LevelResource.ROOT).resolve("yearofice_structs").resolve(name.replaceAll("[^a-z0-9_]", "_") + ".nbt");
    }

    static double labour(BlockState s) {
        PieceData d = s.getBlock().builtInRegistryHolder().getData(PieceData.MAP);
        return d == null ? 0 : d.labourMin();
    }

    /** Records every piece (a block with piece data) in the box around {@code anchor}. */
    public static int save(ServerLevel l, String name, BlockPos anchor, int radius, int up) throws IOException {
        ListTag out = new ListTag();
        for (BlockPos p : BlockPos.betweenClosed(anchor.offset(-radius, 0, -radius), anchor.offset(radius, up, radius))) {
            BlockState s = l.getBlockState(p);
            if (labour(s) <= 0) continue;
            CompoundTag c = new CompoundTag();
            BlockPos r = p.subtract(anchor);
            c.putIntArray("at", new int[] {r.getX(), r.getY(), r.getZ()});
            c.put("state", NbtUtils.writeBlockState(s));
            out.add(c);
        }
        Path f = file(l, name);
        Files.createDirectories(f.getParent());
        CompoundTag root = new CompoundTag();
        root.put("pieces", out);
        NbtIo.writeCompressed(root, f);
        return out.size();
    }

    public static Saved load(ServerLevel l, String name) throws IOException {
        CompoundTag root = NbtIo.readCompressed(file(l, name), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        List<BlockPos> rel = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        List<Stamp.Piece> pieces = new ArrayList<>();
        var blocks = l.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (Tag t : root.getList("pieces", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            int[] a = c.getIntArray("at");
            BlockState s = NbtUtils.readBlockState(blocks, c.getCompound("state"));
            rel.add(new BlockPos(a[0], a[1], a[2]));
            states.add(s);
            pieces.add(new Stamp.Piece(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), labour(s)));
        }
        return new Saved(rel, states, pieces);
    }

    /** Queues the saved pieces at {@code anchor}; returns the player minutes the stamp costs. */
    public static double stamp(ServerLevel l, String name, BlockPos anchor) throws IOException {
        Saved s = load(l, name);
        // Walls first, bottom up, so each course has its support when it lands.
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < s.rel().size(); i++) order.add(i);
        order.sort(java.util.Comparator.comparingInt(i -> s.rel().get(i).getY()));
        for (int i : order) PieceJobs.queue(l, anchor.offset(s.rel().get(i)), s.states().get(i));
        return Stamp.stampMin(s.pieces(), PiecesConfig.stampPct() / 100.0, PiecesConfig.stampSetupMin());
    }
}
