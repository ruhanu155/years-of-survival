package com.yearofice.build.cellar;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.build.multiblock.MultiblockData;
import com.yearofice.sim.build.Cellars;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Validates the sealed room behind a cellar hatch and gives its temperature (Decay and time › Snow as a cold store;
 * Minecraft engine › Explore, don't dig: nothing is dug, the room is built at floor level). Root cellar: the room's
 * walls are earth-banked. Permafrost cellar: packed with ice or sawdust. Numbers come from {@code multiblock/<id>.json}.
 */
public class CellarBlockEntity extends BlockEntity {
    /** Why a room failed, or {@code null} when it is a cellar. */
    public record Room(Set<BlockPos> cells, double bankShare, double packShare, double coverM, String fault) {}

    private Room room;
    private long checkedAt = Long.MIN_VALUE;
    private double pinAir = Double.NaN, pinGround = Double.NaN;

    public CellarBlockEntity(BlockPos pos, BlockState state) { super(CellarRegistry.CELLAR_ENTITY.get(), pos, state); }

    public boolean permafrost() { return getBlockState().is(CellarRegistry.PERMAFROST_CELLAR.get()); }

    private String id() { return permafrost() ? "permafrost_cellar" : "root_cellar"; }

    /** GameTests pin the air and the ground; real play reads the environment. */
    public void pinClimate(double airC, double groundC) { pinAir = airC; pinGround = groundC; room = null; }

    @Override
    public void setLevel(net.minecraft.world.level.Level l) {
        super.setLevel(l);
        if (!l.isClientSide) CellarIndex.add(l, worldPosition);
    }

    @Override
    public void setRemoved() {
        if (level != null) CellarIndex.remove(level, worldPosition);
        super.setRemoved();
    }

    /** The room behind the hatch, re-measured at most every 2 s. */
    public Room room() {
        if (level == null) return new Room(Set.of(), 0, 0, 0, "no_level");
        long now = level.getGameTime();
        if (room == null || now - checkedAt > 40 || now < checkedAt) { room = measure(); checkedAt = now; }
        return room;
    }

    public boolean valid() { return room().fault() == null; }

    public boolean contains(BlockPos p) { return p.equals(worldPosition) || room().cells().contains(p); }

    private Room measure() {
        int max = (int) MultiblockData.num(id(), "max_cells");
        Set<BlockPos> cells = new HashSet<>();
        ArrayDeque<BlockPos> work = new ArrayDeque<>();
        BlockPos seed = worldPosition.relative(getBlockState().getValue(CellarBlock.FACING));
        if (open(seed)) { cells.add(seed); work.add(seed); }
        if (cells.isEmpty()) return new Room(cells, 0, 0, 0, "no_room");
        while (!work.isEmpty()) {
            BlockPos c = work.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (n.equals(worldPosition) || cells.contains(n) || !open(n)) continue;
                if (cells.size() >= max) return new Room(cells, 0, 0, 0, "leaks");
                cells.add(n);
                work.add(n);
            }
        }
        int faces = 0, earth = 0, pack = 0;
        int topY = Integer.MIN_VALUE;
        BlockPos top = null;
        for (BlockPos c : cells) {
            if (c.getY() > topY) { topY = c.getY(); top = c; }
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (cells.contains(n) || n.equals(worldPosition)) continue;
                BlockState s = level.getBlockState(n);
                faces++;
                if (earth(s)) earth++;
                if (packing(s)) pack++;
            }
        }
        double cover = 0;
        for (int i = 1; i <= 3 && top != null; i++) if (earth(level.getBlockState(top.above(i)))) cover++;
        double bank = faces == 0 ? 0 : earth / (double) faces, packShare = faces == 0 ? 0 : pack / (double) faces;
        String fault = null;
        if (permafrost()) { if (packShare * 100 < MultiblockData.num(id(), "min_pack_pct")) fault = "not_packed"; }
        else if (bank * 100 < MultiblockData.num(id(), "min_bank_pct")) fault = "not_banked";
        return new Room(cells, bank, packShare, permafrost() ? 0 : Math.min(cover, MultiblockData.num(id(), "cover_max_m")), fault);
    }

    private boolean open(BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getCollisionShape(level, p).isEmpty() && !s.is(Blocks.WATER) && !s.is(Blocks.LAVA);
    }

    static boolean earth(BlockState s) { return s.is(BlockTags.DIRT) || s.is(BlockTags.MINEABLE_WITH_SHOVEL) && !s.isAir(); }

    static boolean packing(BlockState s) {
        return s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.HAY_BLOCK)
                || BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().contains("sawdust");
    }

    /** The temperature in the room, or the open air's when the hatch has no valid room behind it. */
    public double tempC() {
        double air = Double.isNaN(pinAir) ? airC() : pinAir;
        Room r = room();
        if (r.fault() != null) return air;
        double ground = Double.isNaN(pinGround) ? groundC() : pinGround;
        return permafrost() ? Cellars.permafrostC(ground, air, r.packShare()) : Cellars.rootC(ground, air, r.bankShare(), r.coverM());
    }

    private double airC() {
        return level instanceof ServerLevel sl ? EnvService.get().airC(sl, worldPosition) : 5;
    }

    private double groundC() {
        return level instanceof ServerLevel sl ? EnvService.get().groundC(sl, worldPosition, MultiblockData.num(id(), "depth_m")) : 2;
    }
}
