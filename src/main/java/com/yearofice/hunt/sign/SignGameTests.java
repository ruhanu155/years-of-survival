package com.yearofice.hunt.sign;

import com.yearofice.api.build.GateService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.practice.PracticeService;
import com.yearofice.build.craft.CraftRegistry;
import com.yearofice.build.craft.StationCraft;
import com.yearofice.build.work.WorkService;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.debug.EcoGameTests;
import com.yearofice.env.weather.WeatherSystem;
import com.yearofice.hunt.HuntItems;
import com.yearofice.hunt.HuntRegistry;
import com.yearofice.item.inventory.KitItems;
import com.yearofice.sim.build.Skill;
import com.yearofice.sim.build.Tier;
import com.yearofice.sim.hunt.Sign;
import com.yearofice.sim.wood.WoodKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** M6 lane E GameTests: sign laid, aged and followed; the bow chain and the arrow batch through the craft api. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class SignGameTests {
    private SignGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    /** Animals stand at Y; the floor is one below. A test's far region is {@code index * FAR_M} blocks along +z. */
    static final int Y = 30, FAR_M = 4096, FAR_TRAIL = 5 * FAR_M;

    @GameTestGenerator
    public static Collection<TestFunction> signTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:hunt/sign_laid_by_moose", 600, SignGameTests::laidByMoose));
        out.add(test("yearofice:hunt/sign_ages_and_erases", 100, SignGameTests::agesAndErases));
        out.add(test("yearofice:hunt/track_leads_to_animal", 200, SignGameTests::trackLeads));
        out.add(test("yearofice:craft/self_bow_chain", 100, SignGameTests::selfBowChain));
        out.add(test("yearofice:craft/arrow_batch", 100, SignGameTests::arrowBatch));
        out.add(test("yearofice:craft/feathers_from_bird", 100, SignGameTests::feathersFromBird));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m6_sign", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static void floor(GameTestHelper h, int x0, int x1, int side, int zOff, net.minecraft.world.level.block.Block b) {
        for (int x = x0; x <= x1; x++) for (int z = -side; z <= side; z++) h.setBlock(new BlockPos(1 + x, Y - 1, 1 + z + zOff), b);
    }

    // ---- sign -------------------------------------------------------------------------------------------------

    /** hunt/sign_laid_by_moose: a moose walked 14+ m east over snow leaves a line of tracks, each heading east. */
    static void laidByMoose(GameTestHelper h) {
        floor(h, -4, 34, 4, 0, Blocks.SNOW_BLOCK);
        var moose = h.spawn(EcosystemRegistry.MOOSE.get(), new BlockPos(1, Y, 1));
        Vec3 target = moose.position().add(24, 0, 0);
        moose.respondToCall(target, h.getLevel().getGameTime() + 1200);
        moose.getNavigation().stop(); // a wander path begun before the call would otherwise run first
        BlockPos start = moose.blockPosition();
        h.onEachTick(() -> {
            if (moose.answeringCall()) return;
            List<BlockPos> tracks = new ArrayList<>();
            for (BlockPos p : SignService.within(h.getLevel(), start, 40)) {
                Sign.Read r = SignService.read(h.getLevel(), p);
                if (r != null && r.kind() == Sign.Kind.TRACK && r.species().equals("moose")) tracks.add(p);
            }
            h.assertTrue(tracks.size() >= 6, "a 14 m walk leaves at least 6 prints at 1.5 m: " + tracks.size());
            for (BlockPos p : tracks) {
                Sign.Read r = SignService.read(h.getLevel(), p);
                h.assertTrue(r.dx() > 0.8 && Math.abs(r.dz()) < 0.6, "heading east at " + p.toShortString() + ": " + r.dx() + "," + r.dz());
                h.assertTrue(r.age() != Sign.Age.OLD, "laid this walk, not old: " + r.ageMin());
            }
            moose.discard();
            h.succeed();
        });
    }

    /** hunt/sign_ages_and_erases: fresh, hours, day, old by the clock; snowfall erases a snow print, calm weather does not. */
    static void agesAndErases(GameTestHelper h) {
        try {
            agesBody(h);
        } finally {
            WeatherSystem.calmOverride = false;
            EnvService.get().setOverride(h.getLevel(), null);
        }
    }

    static void agesBody(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        WeatherSystem.calmOverride = true;
        EnvService.get().setOverride(l, new EnvService.WeatherOverride(Double.NaN, Double.NaN, com.yearofice.sim.env.Precip.NONE, 0));
        floor(h, 0, 6, 3, 0, Blocks.SNOW_BLOCK);
        BlockPos a = h.absolutePos(new BlockPos(2, Y, 1)), b = h.absolutePos(new BlockPos(4, Y, 1));
        h.assertTrue(SignService.lay(l, a, Sign.Kind.TRACK, "yearofice:moose", 1, 0, 1) != null, "a print goes down on snow");
        h.assertTrue(SignService.lay(l, b, Sign.Kind.TRACK, "yearofice:moose", 0, -1, 1) != null, "a second print");
        SignBlockEntity be = (SignBlockEntity) l.getBlockEntity(a);
        double made = be.madeMin;
        Sign.Read r = SignService.read(l, a);
        h.assertTrue(r.age() == Sign.Age.FRESH && r.text().contains("east"), "fresh and east: " + r.text());
        be.madeMin = made - 600;
        h.assertTrue(SignService.read(l, a).age() == Sign.Age.HOURS, "10 hours is Hours");
        be.madeMin = made - 2000;
        h.assertTrue(SignService.read(l, a).age() == Sign.Age.DAY, "a day and a half is Day");
        be.madeMin = made - 5000;
        h.assertTrue(SignService.read(l, a).age() == Sign.Age.OLD && !SignService.review(l, a), "three days and more: Old but still on the snow");
        be.madeMin = made - 11000;
        h.assertTrue(SignService.review(l, a) && l.getBlockState(a).isAir(), "a week on, the snow print is gone");
        // Weather: calm and clear keeps a fresh print; snowfall takes it.
        {
            var env = EnvService.get();
            h.assertTrue(!SignService.review(l, b) && !l.getBlockState(b).isAir(), "calm weather leaves the print");
            env.setOverride(l, new EnvService.WeatherOverride(-10, Double.NaN, com.yearofice.sim.env.Precip.SNOW, 3));
            h.assertTrue(SignService.review(l, b) && l.getBlockState(b).isAir(), "snowfall erases the snow print");
        }
        // Fresh snow: 2 cm or more deeper than when the print was made hides it.
        EnvService.get().setOverride(l, new EnvService.WeatherOverride(Double.NaN, Double.NaN, com.yearofice.sim.env.Precip.NONE, 0));
        BlockPos d = h.absolutePos(new BlockPos(5, Y, 2));
        SignService.lay(l, d, Sign.Kind.TRACK, "yearofice:moose", 1, 0, 1);
        SignBlockEntity bd = (SignBlockEntity) l.getBlockEntity(d);
        bd.snowCm0 += 1;
        h.assertTrue(!SignService.review(l, d), "1 cm of new snow leaves it");
        bd.snowCm0 -= 3;
        h.assertTrue(SignService.review(l, d) && l.getBlockState(d).isAir(), "2 cm and more of new snow erases it");
        // Practice: a stranger reads 0.5, a Practiced tracker 1.
        BlockPos c = h.absolutePos(new BlockPos(3, Y, 3));
        SignService.lay(l, c, Sign.Kind.SCAT, "yearofice:moose", 0, 0, 0);
        ServerPlayer p = EcoGameTests.player(h);
        PracticeService.get().set(p, Skill.TRAPPING, 0);
        double lo = SignService.read(l, c, p).confidence();
        PracticeService.get().set(p, Skill.TRAPPING, Skill.TRAPPING.n);
        double hi = SignService.read(l, c, p).confidence();
        h.assertTrue(lo == 0.5 && hi == 1.0, "confidence follows practice: " + lo + " -> " + hi);
        h.succeed();
    }

    /** hunt/track_leads_to_animal: from 150 m out, nearest() hops along fresher and fresher prints to within 40 m of the moose. */
    static void trackLeads(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        Vec3 mid = h.absolutePos(new BlockPos(1 + 75, Y, 1 + FAR_TRAIL)).getCenter();
        for (int x = -24; x <= 200; x += 24) EcoGameTests.force(h, mid.add(x - 75, 0, 0), 1, true);
        boolean[] built = {false};
        h.onEachTick(() -> {
            if (h.getTick() < 40) return;
            if (!built[0]) {
                built[0] = true;
                build(h, l);
                return;
            }
        });
        h.runAfterDelay(41, () -> {
            BlockPos start = h.absolutePos(new BlockPos(1, Y, 1 + FAR_TRAIL));
            var moose = l.getEntitiesOfClass(com.yearofice.ecosystem.species.MooseEntity.class, new net.minecraft.world.phys.AABB(start).inflate(200, 20, 20));
            h.assertTrue(moose.size() == 1, "the moose is at the end");
            BlockPos cur = SignService.nearest(l, start, 12, "moose");
            h.assertTrue(cur != null, "a print within 12 m of the start");
            int steps = 0;
            while (true) {
                double age = SignService.read(l, cur).ageMin();
                BlockPos next = SignService.nearest(l, cur, 12, "yearofice:moose", age - 0.01);
                if (next == null) break;
                cur = next;
                steps++;
            }
            double d = Math.hypot(cur.getX() - moose.get(0).getX(), cur.getZ() - moose.get(0).getZ());
            h.assertTrue(steps >= 30, "followed the line: " + steps + " steps");
            h.assertTrue(d <= 40, "the trail ends " + d + " m from the moose");
            h.assertTrue(SignService.read(l, cur).species().equals("moose"), "never crossed to the wolf's line");
            moose.get(0).discard();
            for (int x = -24; x <= 200; x += 24) EcoGameTests.force(h, mid.add(x - 75, 0, 0), 1, false);
            h.succeed();
        });
    }

    /** A 150 m zig-zag of moose prints oldest at the start, a wolf's line beside it, and the moose at the far end. */
    static void build(GameTestHelper h, ServerLevel l) {
        floor(h, -5, 160, 8, FAR_TRAIL, Blocks.SNOW_BLOCK);
        double now = SignService.now(l);
        for (int x = 0; x <= 150; x += 2) {
            int z = (int) Math.round(3 * Math.sin(x / 20.0));
            BlockPos at = h.absolutePos(new BlockPos(1 + x, Y, 1 + z + FAR_TRAIL));
            BlockPos laid = SignService.lay(l, at, Sign.Kind.TRACK, "yearofice:moose", 1, 0, 1);
            if (laid != null) ((SignBlockEntity) l.getBlockEntity(laid)).madeMin = now - (150 - x) * 2;
            BlockPos w = SignService.lay(l, h.absolutePos(new BlockPos(1 + x, Y, 1 + FAR_TRAIL + 7)), Sign.Kind.TRACK, "yearofice:wolf", 0, 1, 1);
            if (w != null) ((SignBlockEntity) l.getBlockEntity(w)).madeMin = now - 10;
        }
        var moose = h.spawn(EcosystemRegistry.MOOSE.get(), new BlockPos(1 + 152, Y, 1 + FAR_TRAIL));
        moose.pinDriveAct(com.yearofice.sim.ecosystem.Drives.Act.BED);
    }

    // ---- craft ------------------------------------------------------------------------------------------------

    static ServerPlayer bot(GameTestHelper h, String name) {
        ServerPlayer p = EcoGameTests.player(h);
        Vec3 at = h.absoluteVec(new Vec3(1.5, 2, 1.5));
        p.moveTo(at.x, at.y, at.z, 0, 0);
        p.getInventory().clearContent();
        h.setBlock(new BlockPos(2, 2, 1), CraftRegistry.WORKBENCH.get());
        return p;
    }

    static ResourceLocation rl(String s) { return ResourceLocation.parse(s); }

    static int count(Player p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    /** Runs a craft to its end; the job must start. */
    static void run(GameTestHelper h, ServerPlayer p, String id) {
        var job = StationCraft.start(p, rl("yearofice:station/" + id), null);
        h.assertTrue(job != null, "the " + id + " job starts");
        int guard = 0;
        while (!StationCraft.advance(p, job, WorkService.dsPerTick()) && guard++ < 100000) { }
        h.assertTrue(job.finished(), id + " finishes");
    }

    static StationCraft.Plan plan(GameTestHelper h, ServerPlayer p, String id) {
        var holder = StationCraft.byId(h.getLevel(), rl("yearofice:station/" + id));
        h.assertTrue(holder != null, id + " recipe loads");
        return StationCraft.plan(p, holder, StationCraft.stations(p, null));
    }

    /** craft/self_bow_chain: seasoned birch -> stave, sinew -> cord, stave + cord -> bow; gates and practice honoured. */
    static void selfBowChain(GameTestHelper h) {
        ServerPlayer p = bot(h, "bow_bot");
        p.getInventory().add(new ItemStack(CraftRegistry.KNIFE.get()));
        h.assertTrue(plan(h, p, "bow_stave").problem() == StationCraft.Problem.GATE, "Bushcraft is shut until a fire opens it");
        GateService.get().open(p, Tier.BUSHCRAFT, "test");
        p.getInventory().add(com.yearofice.build.wood.WoodItems.split(WoodKind.BIRCH, 1, 10, 0));
        run(h, p, "bow_stave");
        h.assertTrue(count(p, HuntRegistry.BOW_STAVE.get()) == 1, "a stave");
        h.assertTrue(plan(h, p, "self_bow").problem() == StationCraft.Problem.INPUTS, "no cord yet");
        p.getInventory().add(new ItemStack(com.yearofice.ecosystem.EcosystemRegistry.SINEW.get(), 2));
        run(h, p, "sinew_cord");
        h.assertTrue(count(p, HuntRegistry.SINEW_CORD.get()) == 1 && count(p, EcosystemRegistry.SINEW.get()) == 0, "two sinew twisted into one cord");
        var clumsy = plan(h, p, "self_bow");
        h.assertTrue(clumsy.problem() == StationCraft.Problem.NONE, "ready: " + clumsy.problem());
        PracticeService.get().set(p, Skill.WOODCRAFT, Skill.WOODCRAFT.n);
        var practiced = plan(h, p, "self_bow");
        h.assertTrue(practiced.minutes() < clumsy.minutes() && practiced.kcal() == clumsy.kcal(), "practice shortens the tillering, never the kcal");
        run(h, p, "self_bow");
        h.assertTrue(count(p, HuntRegistry.SELF_BOW.get()) == 1 && count(p, HuntRegistry.BOW_STAVE.get()) == 0 && count(p, HuntRegistry.SINEW_CORD.get()) == 0,
                "a bow, and the stave and cord are used up");
        p.getInventory().add(new ItemStack(HuntRegistry.LONGBOW.get()));
        p.getInventory().add(new ItemStack(EcosystemRegistry.SINEW.get(), 15));
        h.assertTrue(plan(h, p, "sinew_bow").problem() == StationCraft.Problem.GATE, "the sinew-backed bow waits for Hide and Bone");
        GateService.get().open(p, Tier.HIDE_AND_BONE, "test");
        h.assertTrue(plan(h, p, "sinew_bow").problem() == StationCraft.Problem.NONE, "and opens with it");
        h.succeed();
    }

    /** craft/arrow_batch: nodule -> heads, feathers -> fletching, shafts + heads + fletching -> six arrows; and a spear. */
    static void arrowBatch(GameTestHelper h) {
        ServerPlayer p = bot(h, "arrow_bot");
        GateService.get().open(p, Tier.BUSHCRAFT, "test");
        p.getInventory().add(new ItemStack(CraftRegistry.KNIFE.get()));
        p.getInventory().add(new ItemStack(CraftRegistry.ROUGH_STONE.get()));
        p.getInventory().add(new ItemStack(CraftRegistry.FLINT_NODULE.get(), 2));
        PracticeService.get().set(p, Skill.KNAPPING, Skill.KNAPPING.n);
        run(h, p, "arrowhead_knap");
        h.assertTrue(count(p, HuntRegistry.ARROWHEAD.get()) == 4, "a nodule knaps to 4 heads: " + count(p, HuntRegistry.ARROWHEAD.get()));
        p.getInventory().add(new ItemStack(HuntRegistry.ARROWHEAD.get(), 2));
        p.getInventory().add(new ItemStack(HuntRegistry.FEATHER.get(), 6));
        run(h, p, "fletching");
        run(h, p, "fletching");
        h.assertTrue(count(p, HuntRegistry.FLETCHING.get()) == 2 && count(p, HuntRegistry.FEATHER.get()) == 0, "6 feathers -> 2 fletchings");
        h.assertTrue(plan(h, p, "arrow").problem() == StationCraft.Problem.INPUTS, "no shafts yet");
        p.getInventory().add(new ItemStack(Items.STICK, 6));
        run(h, p, "arrow");
        h.assertTrue(count(p, HuntRegistry.ARROW.get()) == 6, "six arrows: " + count(p, HuntRegistry.ARROW.get()));
        h.assertTrue(count(p, Items.STICK) == 0 && count(p, HuntRegistry.FLETCHING.get()) == 0 && count(p, HuntRegistry.ARROWHEAD.get()) == 0, "shafts, fletching and heads are used up");
        for (ItemStack s : p.getInventory().items) if (s.is(HuntRegistry.ARROW.get())) h.assertTrue(HuntItems.headOf(s).equals(HuntItems.DEFAULT_HEAD), "stone-headed");
        p.getInventory().add(new ItemStack(CraftRegistry.TOOL_HANDLE.get()));
        p.getInventory().add(new ItemStack(HuntRegistry.ARROWHEAD.get()));
        p.getInventory().add(new ItemStack(KitItems.CORD.get()));
        run(h, p, "spear");
        h.assertTrue(count(p, HuntRegistry.SPEAR.get()) == 1, "a spear from handle, head and cord");
        h.succeed();
    }

    /** craft/feathers_from_bird: dressing a spruce grouse yields feathers, and they craft into fletching. */
    static void feathersFromBird(GameTestHelper h) {
        var grouse = h.spawn(EcosystemRegistry.SPRUCE_GROUSE.get(), new BlockPos(1, 2, 1));
        h.runAfterDelay(2, () -> {
            var rec = com.yearofice.ecosystem.wound.DeathPipeline.record(grouse, grouse.species(), "test", com.yearofice.ecosystem.clock.SliceClock.minuteD(h.getLevel()));
            int feathers = 0;
            for (ItemStack s : com.yearofice.ecosystem.carcass.Butchering.parts(h.getLevel(), rec)) if (s.is(HuntRegistry.FEATHER.get())) feathers += s.getCount();
            h.assertTrue(feathers == 15, "a grouse gives 15 feathers: " + feathers);
            grouse.discard();
            ServerPlayer p = bot(h, "feather_bot");
            GateService.get().open(p, Tier.BUSHCRAFT, "test");
            p.getInventory().add(new ItemStack(CraftRegistry.KNIFE.get()));
            p.getInventory().add(new ItemStack(HuntRegistry.FEATHER.get(), feathers));
            for (int i = 0; i < 5; i++) run(h, p, "fletching");
            h.assertTrue(count(p, HuntRegistry.FLETCHING.get()) == 5 && count(p, HuntRegistry.FEATHER.get()) == 0, "15 feathers -> 5 fletchings");
            h.succeed();
        });
    }
}
