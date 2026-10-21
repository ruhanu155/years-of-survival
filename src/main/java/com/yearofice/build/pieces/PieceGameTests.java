package com.yearofice.build.pieces;

import com.yearofice.api.build.HeatData;
import com.yearofice.api.build.Room;
import com.yearofice.api.build.RoomService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.event.RoomChangedEvent;
import com.yearofice.build.rooms.Rooms;
import com.yearofice.core.YearOfIce;
import com.yearofice.gametest.TestFilter;
import com.yearofice.sim.build.Heat;
import com.yearofice.sim.env.Precip;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** Batch m8a, building pieces and rooms: enclosure detection, the −30 °C night, the slab floor's U and the path's speed. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class PieceGameTests {
    private PieceGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final int UP = 70;

    @GameTestGenerator
    public static Collection<TestFunction> pieceTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(new TestFunction("m8a", "yearofice:build/room_detects_cabin", TEMPLATE, 80, 0, true, PieceGameTests::roomDetectsCabin));
        out.add(new TestFunction("m8a", "yearofice:build/cabin_holds_5c", TEMPLATE, 60, 0, true, PieceGameTests::cabinHolds5c));
        out.add(new TestFunction("m8a", "yearofice:build/slab_floor_u", TEMPLATE, 60, 0, true, PieceGameTests::slabFloorU));
        out.add(new TestFunction("m8a", "yearofice:build/path_speed", TEMPLATE, 60, 0, true, PieceGameTests::pathSpeed));
        out.add(new TestFunction("m8a", "yearofice:build/stove_heats_real_room", TEMPLATE, 100, 0, true, PieceGameTests::stoveHeatsRealRoom));
        out.add(new TestFunction("m8a", "yearofice:build/stove_state_saves", TEMPLATE, 60, 0, true, PieceGameTests::stoveStateSaves));
        return TestFilter.apply(out);
    }

    /** A 4x4x2 cabin at x offset {@code ox}: walls on the ring, a door frame, a window, a roof and {@code floor}. Interior is 1..4. */
    static void cabin(GameTestHelper h, int ox, Block floor) {
        Block wall = PieceRegistry.WALL_LOG.get();
        for (int x = 0; x <= 5; x++) for (int z = 0; z <= 5; z++) {
            h.setBlock(new BlockPos(ox + x, UP, z), floor);
            h.setBlock(new BlockPos(ox + x, UP + 3, z), PieceRegistry.ROOF_POLE_SOD.get());
            boolean ring = x == 0 || x == 5 || z == 0 || z == 5;
            for (int y = 1; y <= 2; y++) h.setBlock(new BlockPos(ox + x, UP + y, z), ring ? wall : Blocks.AIR);
        }
        h.setBlock(new BlockPos(ox + 2, UP + 1, 0), PieceRegistry.DOOR_FRAME.get());
        h.setBlock(new BlockPos(ox + 3, UP + 2, 0), PieceRegistry.WINDOW_FRAME.get());
    }

    static BlockPos inside(GameTestHelper h, int ox) { return h.absolutePos(new BlockPos(ox + 2, UP + 1, 2)); }

    static void night(GameTestHelper h) {
        EnvService.get().setOverride(h.getLevel(), new EnvService.WeatherOverride(-30, 3, Precip.NONE, 0));
    }

    static void clear(GameTestHelper h) {
        EnvService.get().setOverride(h.getLevel(), null);
        Rooms.clearStubs(h.getLevel());
    }

    static void roomDetectsCabin(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        AtomicInteger events = new AtomicInteger();
        java.util.function.Consumer<RoomChangedEvent> on = e -> events.incrementAndGet();
        NeoForge.EVENT_BUS.addListener(RoomChangedEvent.class, on);
        night(h);
        try {
            cabin(h, 0, PieceRegistry.SLAB_FLOOR.get());
            BlockPos in = inside(h, 0);
            Room r = RoomService.get().roomAt(l, in);
            if (r == null) h.fail("a closed cabin was not found as a room");
            if (r.volumeM3() != 32) h.fail("volume " + r.volumeM3() + " not 32");
            if (!r.leaks().isEmpty()) h.fail("a closed cabin has leaks: " + r.leaks());
            if (!RoomService.get().airAt(l, in)) h.fail("airAt false inside the cabin");
            if (RoomService.get().airAt(l, h.absolutePos(new BlockPos(2, UP + 5, 2)))) h.fail("airAt true above the roof");
            // Open the door: one leak. Remove a wall block: no room.
            h.setBlock(new BlockPos(2, UP + 1, 0), PieceRegistry.DOOR_FRAME.get().defaultBlockState().setValue(DoorFrameBlock.OPEN, true));
            Room open = RoomService.get().roomAt(l, in);
            if (open == null || open.leaks().size() != 1) h.fail("an open door should be one leak: " + (open == null ? "no room" : open.leaks()));
            if (open.uWPerK() <= r.uWPerK()) h.fail("an open door did not raise the loss");
            h.setBlock(new BlockPos(5, UP + 1, 2), Blocks.AIR);
            if (RoomService.get().roomAt(l, in) != null) h.fail("a breached wall is still a room");
            h.runAfterDelay(10, () -> {
                clear(h);
                NeoForge.EVENT_BUS.unregister(on);
                if (events.get() == 0) h.fail("no RoomChangedEvent after the block changes");
                h.succeed();
            });
        } catch (RuntimeException ex) {
            clear(h);
            NeoForge.EVENT_BUS.unregister(on);
            throw ex;
        }
    }

    static void cabinHolds5c(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        night(h);
        try {
            cabin(h, 0, PieceRegistry.SLAB_FLOOR.get());
            BlockPos in = inside(h, 0);
            Rooms.stubSource(l, in, 4000);
            Room r = RoomService.get().roomAt(l, in);
            if (r == null) h.fail("no room");
            if (r.outsideC() > -29 || r.outsideC() < -31) h.fail("outside should be -30 C: " + r.outsideC());
            if (r.sourceW() < 3999) h.fail("stove source not counted: " + r.sourceW());
            if (r.feelsLikeC() <= 5) h.fail("cabin feels " + r.feelsLikeC() + " C at -30 C with 4 kW");
            h.setBlock(new BlockPos(5, UP + 1, 2), Blocks.AIR);
            h.setBlock(new BlockPos(5, UP + 2, 2), Blocks.AIR);
            Room gone = RoomService.get().roomAt(l, in);
            if (gone != null && gone.feelsLikeC() > 5) h.fail("a cabin missing a wall still held " + gone.feelsLikeC());
            // The same envelope with the missing wall counted as an opening, by the pure model.
            Heat.Balance b = Heat.solve(r.volumeM3() > 0 ? List.of(new Heat.Face(Heat.Part.WALL, 24, 0.9, 0)) : List.of(), 8, 0, 4000, -30, -5, 3);
            if (b.feelsLikeC() > 5) h.fail("pure model with an open wall held " + b.feelsLikeC());
            clear(h);
            h.succeed();
        } catch (RuntimeException ex) {
            clear(h);
            throw ex;
        }
    }

    /** M8 lane D: a real lit tin stove with a real chimney in a sealed 4x4x3 (48 m3 less the stove column) cabin at -30 C, no stub. */
    static void stoveHeatsRealRoom(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        night(h);
        try {
            Block wall = PieceRegistry.WALL_LOG.get();
            for (int x = 0; x <= 5; x++) for (int z = 0; z <= 5; z++) {
                h.setBlock(new BlockPos(x, UP, z), PieceRegistry.SLAB_FLOOR.get());
                h.setBlock(new BlockPos(x, UP + 4, z), PieceRegistry.ROOF_POLE_SOD.get());
                boolean ring = x == 0 || x == 5 || z == 0 || z == 5;
                for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, UP + y, z), ring ? wall : Blocks.AIR);
            }
            BlockPos at = new BlockPos(1, UP + 1, 1);
            h.setBlock(at, com.yearofice.fire.stove.StoveRegistry.TIN_STOVE.get());
            for (int y = 2; y <= 5; y++) h.setBlock(new BlockPos(1, UP + y, 1), com.yearofice.fire.stove.StoveRegistry.CHIMNEY_PIPE.get());
            com.yearofice.fire.stove.StoveBlockEntity be = h.getBlockEntity(at);
            be.pause(true);
            BlockPos in = inside(h, 0);
            Room cold = RoomService.get().roomAt(l, in);
            if (cold == null || cold.volumeM3() != 45 || cold.sourceW() != 0) h.fail("cold cabin: " + cold);
            be.loadFuel(com.yearofice.sim.fire.Wood.BIRCH, 10, 2.0);
            if (!be.light()) h.fail("the stove did not light");
            be.setting(com.yearofice.sim.env.FireSetting.STEADY);
            Room hot = RoomService.get().roomAt(l, in);
            if (hot.sourceW() < 1000 || Math.abs(hot.sourceW() - be.outputW()) > 1) h.fail("room got " + hot.sourceW() + " W, stove gives " + be.outputW());
            if (hot.feelsLikeC() <= 5) h.fail("STEADY " + hot.sourceW() + " W feels " + hot.feelsLikeC() + " C at -30 C");
            be.setting(com.yearofice.sim.env.FireSetting.LOW);
            Room low = RoomService.get().roomAt(l, in);
            System.out.println("YOI stove_heats_real_room: volume " + hot.volumeM3() + " U " + hot.uWPerK() + " STEADY " + hot.sourceW() + " W feels " + hot.feelsLikeC()
                    + " C, LOW " + low.sourceW() + " W feels " + low.feelsLikeC() + " C");
            if (low.sourceW() >= hot.sourceW()) h.fail("LOW did not lower the watts");
            be.douse();
            Room out = RoomService.get().roomAt(l, in);
            if (out.sourceW() != 0 || out.feelsLikeC() > 0) h.fail("a doused stove still heats: " + out.sourceW() + " W " + out.feelsLikeC() + " C");
            clear(h);
            h.succeed();
        } catch (RuntimeException ex) {
            clear(h);
            throw ex;
        }
    }

    /** M8 lane D: a stove's fire survives save and load, and a saved structure keeps its stove and chimney pipes. */
    static void stoveStateSaves(GameTestHelper h) {
        try { stoveStateSaves0(h); } catch (java.io.IOException ex) { h.fail(ex.toString()); }
    }

    static void stoveStateSaves0(GameTestHelper h) throws java.io.IOException {
        ServerLevel l = h.getLevel();
        BlockPos at = new BlockPos(2, UP + 1, 2);
        h.setBlock(at.below(), Blocks.STONE);
        h.setBlock(at, com.yearofice.fire.stove.StoveRegistry.TIN_STOVE.get());
        h.setBlock(at.above(), com.yearofice.fire.stove.StoveRegistry.CHIMNEY_PIPE.get());
        com.yearofice.fire.stove.StoveBlockEntity be = h.getBlockEntity(at);
        be.pause(true);
        be.loadFuel(com.yearofice.sim.fire.Wood.BIRCH, 10, 2.0);
        be.loadFuel(com.yearofice.sim.fire.Wood.SPRUCE, 20, 1.0);
        h.assertTrue(be.light(), "lights");
        be.setting(com.yearofice.sim.env.FireSetting.HOT);
        be.creosote(7);
        be.stepMinutes(30);
        net.minecraft.nbt.CompoundTag tag = be.saveWithFullMetadata(l.registryAccess());
        BlockPos abs = h.absolutePos(at);
        com.yearofice.fire.stove.StoveBlockEntity fresh = new com.yearofice.fire.stove.StoveBlockEntity(abs, l.getBlockState(abs));
        fresh.loadWithComponents(tag, l.registryAccess());
        h.assertTrue(fresh.lit() == be.lit() && be.lit(), "lit survives");
        h.assertTrue(fresh.setting() == be.setting(), "setting survives: " + fresh.setting());
        h.assertTrue(Math.abs(fresh.armloads() - be.armloads()) < 1e-6, "fuel survives: " + fresh.armloads() + " vs " + be.armloads());
        h.assertTrue(Math.abs(fresh.creosote() - be.creosote()) < 1e-6 && Math.abs(fresh.smokePct() - be.smokePct()) < 1e-6, "creosote and smoke survive");
        h.assertTrue(tag.contains("last_minute") || !com.yearofice.api.clock.ClockService.installed() || be.lit(), "the stove keeps when it last ran");
        Structs.save(l, "m8d_fixture", abs, 3, 4);
        Structs.Saved sv = Structs.load(l, "m8d_fixture");
        boolean stove = sv.states().stream().anyMatch(s -> s.is(com.yearofice.fire.stove.StoveRegistry.TIN_STOVE.get()));
        boolean pipe = sv.states().stream().anyMatch(s -> s.is(com.yearofice.fire.stove.StoveRegistry.CHIMNEY_PIPE.get()));
        h.assertTrue(stove && pipe, "a saved structure keeps its stove and pipe: " + sv.states());
        be.douse();
        h.succeed();
    }

    static void slabFloorU(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        night(h);
        try {
            HeatData slab = PieceRegistry.SLAB_FLOOR.get().builtInRegistryHolder().getData(HeatData.MAP);
            if (slab == null || slab.uWPerM2K() >= HeatData.DEFAULT_SOLID_U) h.fail("slab floor U should be below a bare solid's " + HeatData.DEFAULT_SOLID_U);
            cabin(h, 0, PieceRegistry.SLAB_FLOOR.get());
            cabin(h, 8, Blocks.STONE);
            Rooms.stubSource(l, inside(h, 0), 2000);
            Rooms.stubSource(l, inside(h, 8), 2000);
            Room a = RoomService.get().roomAt(l, inside(h, 0)), b = RoomService.get().roomAt(l, inside(h, 8));
            if (a == null || b == null) h.fail("cabins not found: " + a + " " + b);
            if (a.feelsLikeC() <= b.feelsLikeC()) h.fail("slab floor (" + a.feelsLikeC() + ") should be warmer than bare stone (" + b.feelsLikeC() + ")");
            clear(h);
            h.succeed();
        } catch (RuntimeException ex) {
            clear(h);
            throw ex;
        }
    }

    static void pathSpeed(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(new BlockPos(1, UP + 1, 1));
        double last = 0;
        for (PathBlock.Surface s : PathBlock.Surface.values()) {
            h.getLevel().setBlock(at, PieceRegistry.PATH.get().defaultBlockState().setValue(PathBlock.SURFACE, s), 3);
            p.moveTo(Vec3.atBottomCenterOf(at).add(0, 0.0625, 0));
            double bonus = PathSpeed.update(p);
            if (bonus <= last) h.fail(s + " path bonus " + bonus + " should beat " + last);
            last = bonus;
            double base = p.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
            if (Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - base * (1 + bonus)) > 1e-6) h.fail(s + " speed not applied");
        }
        h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        if (PathSpeed.update(p) != 0 || p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(PathSpeed.ID) != null) h.fail("bonus lingers off the path");
        h.succeed();
    }
}
