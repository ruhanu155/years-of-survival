package com.yearofice.build.rooms;

import com.yearofice.api.build.HeatData;
import com.yearofice.api.build.Room;
import com.yearofice.api.build.RoomService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.event.RoomChangedEvent;
import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.build.pieces.DoorFrameBlock;
import com.yearofice.build.pieces.PieceRegistry;
import com.yearofice.build.pieces.PiecesConfig;
import com.yearofice.sim.build.Heat;
import com.yearofice.sim.build.RoomScan;
import com.yearofice.sim.fire.FireHeat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/** Room finding and the heat balance: flood fill over the level, cached per cell, dirtied by block changes and rescanned on a budget. */
public final class Rooms implements RoomService {
    public static final Rooms INSTANCE = new Rooms();
    /** Test/sim heat sources: level → pos → watts (the stove stub of {@code build/cabin_holds_5c}). */

    private static final java.util.Set<ServerLevel> ACTIVE = ConcurrentHashMap.newKeySet();

    private Rooms() {}

    public static void init() {
        RoomService.INSTANCE.set(INSTANCE);
        NeoForge.EVENT_BUS.addListener((BlockEvent.NeighborNotifyEvent e) -> {
            if (e.getLevel() instanceof ServerLevel l) dirty(l, e.getPos());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            for (ServerLevel l : e.getServer().getAllLevels()) {
                if (!ACTIVE.contains(l)) continue;
                RoomsData d = RoomsData.get(l);
                if (d.dirty.isEmpty()) continue;
                for (int i = 0; i < PiecesConfig.scansPerTick() && !d.dirty.isEmpty(); i++) {
                    BlockPos p = d.dirty.poll();
                    RoomService.INSTANCE.roomAt(l, p);
                    NeoForge.EVENT_BUS.post(new RoomChangedEvent(l, p, INSTANCE.entry(l, p) != null));
                }
            }
        });
    }

    public static void reset() { ACTIVE.clear(); com.yearofice.api.fire.HeatSources.clearAll(); }

    public static void stubSource(Level level, BlockPos pos, double watts) { com.yearofice.api.fire.HeatSources.post(level, pos, watts); }
    public static void clearStubs(Level level) { com.yearofice.api.fire.HeatSources.clear(level); }

    /** A block changed at {@code pos}: drop the rooms it touches and queue a rescan of its air neighbours. */
    public static void dirty(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel l)) return;
        if (!ACTIVE.contains(l)) return; // nothing was ever asked about: nothing to dirty
        RoomsData d = RoomsData.get(l);
        d.open.clear();
        d.byCell.values().removeIf(e -> pos.getX() >= e.minX - 1 && pos.getX() <= e.maxX + 1 && pos.getY() >= e.minY - 1
                && pos.getY() <= e.maxY + 1 && pos.getZ() >= e.minZ - 1 && pos.getZ() <= e.maxZ + 1);
        if (d.dirty.size() < 256) for (Direction dir : Direction.values()) d.dirty.add(pos.relative(dir));
    }

    static RoomScan.Cell cell(ServerLevel l, long p) {
        BlockPos pos = new BlockPos(RoomScan.ux(p), RoomScan.uy(p), RoomScan.uz(p));
        if (!l.isLoaded(pos)) return RoomScan.Cell.LEAK;
        BlockState s = l.getBlockState(pos);
        Block b = s.getBlock();
        if (b instanceof DoorFrameBlock && s.getValue(DoorFrameBlock.OPEN)) return RoomScan.Cell.LEAK;
        boolean piece = s.is(PieceRegistry.WALL) || s.is(PieceRegistry.ROOF) || s.is(PieceRegistry.FLOOR) || s.is(PieceRegistry.DOOR);
        if (!piece && s.getCollisionShape(l, pos).isEmpty()) return RoomScan.Cell.AIR;
        HeatData h = b.builtInRegistryHolder().getData(HeatData.MAP);
        return RoomScan.Cell.solid(h != null ? h.uWPerM2K() : HeatData.DEFAULT_SOLID_U);
    }

    @Nullable RoomsData.Entry entry(ServerLevel l, BlockPos pos) {
        ACTIVE.add(l);
        RoomsData d = RoomsData.get(l);
        long key = pos.asLong();
        RoomsData.Entry e = d.byCell.get(RoomScan.pack(pos.getX(), pos.getY(), pos.getZ()));
        if (e == null) {
            Long neg = d.open.get(key);
            if (neg != null && l.getGameTime() - neg < 40) return null;
            RoomScan.Result r = RoomScan.scan(p -> cell(l, p), pos.getX(), pos.getY(), pos.getZ(), PiecesConfig.roomMax());
            if (!r.enclosed()) {
                if (d.open.size() > 4096) d.open.clear();
                d.open.put(key, l.getGameTime());
                return null;
            }
            e = new RoomsData.Entry();
            e.scan = r;
            e.minX = e.minY = e.minZ = Integer.MAX_VALUE;
            e.maxX = e.maxY = e.maxZ = Integer.MIN_VALUE;
            for (long c : r.air()) {
                int x = RoomScan.ux(c), y = RoomScan.uy(c), z = RoomScan.uz(c);
                e.minX = Math.min(e.minX, x); e.maxX = Math.max(e.maxX, x);
                e.minY = Math.min(e.minY, y); e.maxY = Math.max(e.maxY, y);
                e.minZ = Math.min(e.minZ, z); e.maxZ = Math.max(e.maxZ, z);
                d.byCell.put(c, e);
            }
            e.stamp = Long.MIN_VALUE / 2;
        }
        if (l.getGameTime() - e.stamp >= PiecesConfig.cacheTicks()) {
            e.room = balance(l, e);
            e.stamp = l.getGameTime();
            d.remember(e.room.seed(), e.room.volumeM3(), e.room.uWPerK());
        }
        return e;
    }

    /** A stove, bloomery or tent stove is a solid block: it heats the room whose air touches it. ponytail: a source between two rooms heats both. */
    private static boolean sourceOnWall(RoomsData.Entry e, BlockPos c) {
        for (Direction d : Direction.values()) if (e.byCellHas(c.getX() + d.getStepX(), c.getY() + d.getStepY(), c.getZ() + d.getStepZ())) return true;
        return false;
    }

    private static Room balance(ServerLevel l, RoomsData.Entry e) {
        BlockPos seed = new BlockPos(e.minX, e.minY, e.minZ);
        BlockPos above = new BlockPos((e.minX + e.maxX) / 2, e.maxY + 3, (e.minZ + e.maxZ) / 2);
        EnvService env = EnvService.get();
        double out = env.airC(l, above), wind = env.wind(l, above).ms(), ground = env.groundC(l, seed, 1.0);
        double snowM = Math.min(1.0, env.snowCm(l, above) / 100.0);
        List<Heat.Face> faces = new ArrayList<>();
        for (Heat.Face f : e.scan.faces()) faces.add(f.part() == Heat.Part.ROOF ? new Heat.Face(f.part(), f.areaM2(), f.u(), snowM) : f);
        double w = com.yearofice.api.fire.HeatSources.wattsIn(l, c -> e.byCellHas(c.getX(), c.getY(), c.getZ()) || sourceOnWall(e, c));
        Heat.Balance b = Heat.solve(faces, e.scan.leaks().size(), 0, w, out, ground, wind);
        List<BlockPos> leaks = new ArrayList<>();
        for (long c : e.scan.leaks()) leaks.add(new BlockPos(RoomScan.ux(c), RoomScan.uy(c), RoomScan.uz(c)));
        int walls = 0, snow = 0, flames = 0;
        for (long c : e.scan.air()) {
            BlockPos at = new BlockPos(RoomScan.ux(c), RoomScan.uy(c), RoomScan.uz(c));
            for (Direction d : Direction.values()) {
                BlockPos n = at.relative(d);
                if (e.scan.air().contains(RoomScan.pack(n.getX(), n.getY(), n.getZ()))) continue;
                BlockState s = l.getBlockState(n);
                walls++;
                if (s.is(com.yearofice.travel.SnowRoom.SNOW_WALLS)) snow++;
                if (s.is(net.minecraft.tags.BlockTags.FIRE) || (s.getBlock() instanceof net.minecraft.world.level.block.CampfireBlock && s.getValue(net.minecraft.world.level.block.CampfireBlock.LIT))) flames++;
            }
        }
        return new Room(seed, e.scan.volume(), b.gWPerC(), b.feelsLikeC(), out, w, leaks, walls == 0 ? 0 : snow / (double) walls, flames);
    }

    @Override
    public @Nullable Room roomAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel l)) return null;
        RoomsData.Entry e = entry(l, pos);
        return e == null ? null : e.room;
    }
}
