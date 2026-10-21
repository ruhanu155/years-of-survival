package com.yearofice.gametest;

import com.yearofice.api.build.GateService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.practice.PracticeService;
import com.yearofice.build.craft.CraftRegistry;
import com.yearofice.build.craft.StationCraft;
import com.yearofice.build.craft.StationNet;
import com.yearofice.build.craft.Wear;
import com.yearofice.build.work.WorkService;
import com.yearofice.build.work.WorkStartPayload;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireSkill;
import com.yearofice.item.inventory.BoxCarry;
import com.yearofice.item.inventory.CarryRegistry;
import com.yearofice.sim.build.Skill;
import com.yearofice.sim.build.Tier;
import com.yearofice.sim.build.ToolWear;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M3 lane B: practice, gates, station crafts, tool wear and lifting a full box (Crafting and building in depth;
 * Player systems and client › A7). Each test uses its own detached bot and finishes inside one tick.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class CraftGameTests {
    private CraftGameTests() {}

    static final String BATCH = "m3_craft";

    @GameTestGenerator
    public static Collection<TestFunction> craftTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:craft/practice_attachment", CraftGameTests::practice));
        out.add(test("yearofice:craft/gates_independent", CraftGameTests::gates));
        out.add(test("yearofice:craft/stakes_craft", CraftGameTests::stakes));
        out.add(test("yearofice:craft/tool_wear_edge", CraftGameTests::wear));
        out.add(test("yearofice:pack/lift_box_30kg", CraftGameTests::lift30));
        out.add(test("yearofice:pack/box_keeps_contents", CraftGameTests::keepsContents));
        return out;
    }

    static TestFunction test(String name, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, LayerGameTests.TEMPLATE, 100, 0, true, body);
    }

    static ServerPlayer bot(GameTestHelper h, String name) {
        ServerPlayer p = LayerGameTests.player(h, name);
        var at = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        p.moveTo(at.x, at.y, at.z, 0, 0);
        p.getInventory().clearContent();
        return p;
    }

    static ResourceLocation rl(String s) { return ResourceLocation.parse(s); }

    // ---- practice and gates -----------------------------------------------------------------------------

    static void practice(GameTestHelper h) {
        ServerPlayer p = bot(h, "practice_bot");
        PracticeService s = PracticeService.get();
        h.assertTrue(s.reps(p, Skill.FIRE) == 0, "a new player has no reps");
        double got = 0;
        for (int i = 0; i < 10; i++) got += s.add(p, Skill.FIRE, 1);
        h.assertTrue(Math.abs(got - 2.0) < 1e-9, "fire's daily cap is N/5 = 2 reps, gained " + got);
        h.assertTrue(Math.abs(s.reps(p, Skill.FIRE) - 2.0) < 1e-9, "reps total 2");
        h.assertTrue(Math.abs(FireSkill.reps(p) - 2.0) < 1e-9, "FireSkill reads the attachment");
        FireSkill.setReps(p, 7);
        h.assertTrue(s.reps(p, Skill.FIRE) == 7, "FireSkill writes the attachment");
        s.set(p, Skill.KNAPPING, 25);
        h.assertTrue(s.practiced(p, Skill.KNAPPING), "25 reps is practiced");
        h.assertTrue(!s.practiced(p, Skill.CORDAGE), "other skills are untouched");
        h.succeed();
    }

    static void gates(GameTestHelper h) {
        ServerPlayer p = bot(h, "gate_bot");
        GateService g = GateService.get();
        h.assertTrue(g.isOpen(p, Tier.START), "Start is open in a new world");
        h.assertTrue(!g.isOpen(p, Tier.BUSHCRAFT) && !g.isOpen(p, Tier.IRON), "the others are shut");
        h.assertTrue(g.open(p, Tier.IRON, "test"), "iron opens");
        h.assertTrue(g.isOpen(p, Tier.IRON) && !g.isOpen(p, Tier.BUSHCRAFT) && !g.isOpen(p, Tier.HIDE_AND_BONE), "gates open independently");
        h.assertTrue(!g.open(p, Tier.IRON, "again"), "opening twice reports false");
        h.assertTrue(com.yearofice.build.craft.CraftSystem.openBushcraftAt(h.absolutePos(BlockPos.ZERO), List.of(p)) >= 0, "fire opens bushcraft");
        h.assertTrue(g.isOpen(p, Tier.BUSHCRAFT), "a lit fire opens Bushcraft");
        h.succeed();
    }

    // ---- crafting ---------------------------------------------------------------------------------------

    static void stakes(GameTestHelper h) {
        ServerPlayer p = bot(h, "stakes_bot");
        ResourceLocation id = rl("yearofice:station/stakes");
        h.assertTrue(StationCraft.byId(h.getLevel(), id) != null, "the stakes recipe loads");
        h.assertTrue(StationCraft.all(h.getLevel()).size() >= 4, "the first bushcraft set loads");
        h.assertTrue(StationCraft.start(p, id, null) == null, "no stick, no knife: no job");
        p.getInventory().add(new ItemStack(CraftRegistry.KNIFE.get()));
        p.getInventory().add(new ItemStack(Items.STICK));
        var holder = StationCraft.byId(h.getLevel(), id);
        var before = StationCraft.plan(p, holder, StationCraft.stations(p, null));
        h.assertTrue(before.problem() == StationCraft.Problem.NONE, "ready: " + before.problem());
        var list = StationNet.list(p, null);
        h.assertTrue(list.rows().stream().anyMatch(r -> r.id().equals(id)), "the hands list shows stakes");
        var job = StationCraft.start(p, id, null);
        h.assertTrue(job != null, "the job starts");
        int guard = 0;
        while (!StationCraft.advance(p, job, WorkService.dsPerTick()) && guard++ < 10000) { }
        h.assertTrue(job.finished(), "the job finishes");
        int stakes = 0, sticks = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(CraftRegistry.STAKE.get())) stakes += s.getCount();
            if (s.is(Items.STICK)) sticks += s.getCount();
        }
        h.assertTrue(stakes == 2 && sticks == 0, "one stick became 2 stakes: " + stakes + "/" + sticks);
        h.assertTrue(PracticeService.get().reps(p, Skill.WOODCRAFT) >= 1.0, "the job counted a rep");
        p.getInventory().add(new ItemStack(Items.STICK));
        var after = StationCraft.plan(p, holder, StationCraft.stations(p, null));
        h.assertTrue(after.kcal() == before.kcal(), "practice never changes the kcal");
        h.assertTrue(after.minutes() <= before.minutes(), "practice only shortens the time");
        h.succeed();
    }

    static void wear(GameTestHelper h) {
        ServerPlayer p = bot(h, "wear_bot");
        ItemStack knife = new ItemStack(CraftRegistry.KNIFE.get());
        h.assertTrue(Wear.edge(knife) == ToolWear.EDGE_SHARP, "a new knife is sharp");
        h.assertTrue(Wear.scaleDs(knife, 1500) == 1500, "a sharp edge costs no time");
        Wear.setEdge(knife, 0);
        h.assertTrue(Wear.dull(knife), "edge 0 is dull");
        h.assertTrue(Wear.scaleDs(knife, 1500) < 1500, "a dull edge slows the work");
        Wear.hone(knife, ToolWear.EDGE_SHARP);
        h.assertTrue(Wear.edge(knife) > 0, "honing restores edge");
        int before = knife.getDamageValue();
        Wear.use(p, knife, 3);
        h.assertTrue(knife.getDamageValue() > before || Wear.edge(knife) < ToolWear.EDGE_SHARP, "use wears the tool");
        h.assertTrue(CraftRegistry.KNIFE.get().getDefaultInstance().getMaxDamage() == CraftRegistry.KNIFE_DURABILITY, "durability comes from the data");
        h.succeed();
    }

    // ---- A7: lifting a full box ---------------------------------------------------------------------------

    static BlockPos chestAt(GameTestHelper h) { return h.absolutePos(new BlockPos(2, 2, 1)); }

    static void floor(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) l.setBlock(h.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState(), 3);
    }

    static Container box(GameTestHelper h) { return (Container) h.getLevel().getBlockEntity(chestAt(h)); }

    static WorkStartPayload press(BlockPos pos) { return new WorkStartPayload(true, pos, -1, (byte) 1, 0.5f, 1f, 0.5f); }

    static long minute() { return ClockService.installed() ? ClockService.get().nowMinute() : 0; }

    static void lift30(GameTestHelper h) {
        floor(h);
        ServerLevel l = h.getLevel();
        BlockPos pos = chestAt(h);
        l.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        Container c = box(h);
        var round = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl("yearofice:spruce_round"));
        c.setItem(0, new ItemStack(round, 4));
        c.setItem(1, new ItemStack(CraftRegistry.ROUGH_STONE.get(), 5));
        var chk = BoxCarry.check(l, pos);
        h.assertTrue(Math.abs(chk.totalKg() - 30.0) < 0.01, "chest 12 kg + 18 kg of contents is 30 kg, was " + chk.totalKg());
        h.assertTrue(chk.problem() == BoxCarry.Problem.NONE, "30 kg lifts: " + chk.problem());
        ServerPlayer p = bot(h, "lift_bot");
        p.setShiftKeyDown(true);
        var job = BoxCarry.resolve(p, press(pos));
        h.assertTrue(job != null, "Shift+RMB on a 30 kg box starts a lift");
        for (int i = 0; i < 19; i++) job.work(p, 150);
        h.assertTrue(!job.finished() && !BoxCarry.carrying(p), "not lifted before 1 s");
        job.work(p, 150);
        h.assertTrue(job.finished() && BoxCarry.carrying(p), "lifted at exactly 20 ticks");
        h.assertTrue(Math.abs(BoxCarry.kg(p) - 30.0) < 0.01, "the carried weight counts: " + BoxCarry.kg(p));
        h.assertTrue(l.getBlockState(pos).isAir(), "the chest left the world");
        h.assertTrue(BoxCarry.setDown(p, pos, true), "set it back");
        h.assertTrue(!BoxCarry.carrying(p) && box(h).getItem(1).getCount() == 5, "contents kept");

        // 0.6 kg more is refused with a line, and nothing moves.
        box(h).setItem(2, new ItemStack(CraftRegistry.ROUGH_STONE.get(), 1));
        h.assertTrue(BoxCarry.check(l, pos).problem() == BoxCarry.Problem.TOO_HEAVY, "over the cap is refused");
        h.assertTrue(BoxCarry.resolve(p, press(pos)) == null, "no job for an over-cap box");
        h.assertTrue(!BoxCarry.carrying(p) && l.getBlockState(pos).is(Blocks.CHEST), "the box stayed put");
        box(h).setItem(2, ItemStack.EMPTY);

        // Frozen to the ground after a day below zero: a 2 s chip with an edged tool first.
        var saved = BoxCarry.frost;
        try {
            BoxCarry.frost = new BoxCarry.Frost() {
                @Override public double airC(ServerLevel lv, BlockPos at) { return -10; }
                @Override public boolean groundFrozen(ServerLevel lv, BlockPos at) { return true; }
            };
            BlockEntity be = l.getBlockEntity(pos);
            be.setData(CarryRegistry.BOX_STAMP.get(), minute() - 25 * 60L);
            h.assertTrue(BoxCarry.check(l, pos).problem() == BoxCarry.Problem.FROZEN, "a day in the frost freezes it down");
            h.assertTrue(BoxCarry.resolve(p, press(pos)) == null, "no lift while frozen");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(CraftRegistry.KNIFE.get()));
            var chip = BoxCarry.resolve(p, press(pos));
            h.assertTrue(chip != null, "the knife starts a chip");
            for (int i = 0; i < 40; i++) chip.work(p, 150);
            h.assertTrue(chip.finished(), "the chip takes 40 ticks");
            h.assertTrue(BoxCarry.check(l, pos).problem() == BoxCarry.Problem.NONE, "chipped free");
        } finally {
            BoxCarry.frost = saved;
        }
        h.succeed();
    }

    static void keepsContents(GameTestHelper h) {
        floor(h);
        ServerLevel l = h.getLevel();
        BlockPos pos = chestAt(h);
        l.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST), 3);
        Container c = box(h);
        ItemStack[] items = {new ItemStack(CraftRegistry.ROUGH_STONE.get(), 1), new ItemStack(CraftRegistry.STAKE.get(), 1),
                new ItemStack(CraftRegistry.FLINT_NODULE.get(), 1), new ItemStack(CraftRegistry.TOOL_HANDLE.get(), 1)};
        for (int i = 0; i < 27; i++) {
            ItemStack s = items[i % items.length].copy();
            s.setCount(1 + i % 2);
            s.set(DataComponents.CUSTOM_NAME, Component.literal("slot " + i));
            c.setItem(i, s);
        }
        List<ItemStack> want = new ArrayList<>();
        for (int i = 0; i < 27; i++) want.add(c.getItem(i).copy());
        ServerPlayer p = bot(h, "keep_bot");
        var chk = BoxCarry.check(l, pos);
        h.assertTrue(chk.problem() == BoxCarry.Problem.NONE, "27 stacks fit under the cap: " + chk.problem() + " " + chk.totalKg());
        for (int n = 0; n < 100; n++) {
            h.assertTrue(BoxCarry.lift(p, pos), "lift " + n);
            h.assertTrue(BoxCarry.setDown(p, pos, true), "set down " + n);
        }
        Container after = box(h);
        for (int i = 0; i < 27; i++) {
            h.assertTrue(ItemStack.matches(want.get(i), after.getItem(i)), "slot " + i + " kept after 100 cycles");
        }
        h.assertTrue(l.getBlockState(pos).getValue(ChestBlock.FACING) == Direction.EAST, "facing kept");
        h.succeed();
    }
}
