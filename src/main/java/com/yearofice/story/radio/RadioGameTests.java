package com.yearofice.story.radio;

import com.yearofice.api.story.AmbitionTracker;
import com.yearofice.api.weather.WeatherService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.weather.StormKind;
import com.yearofice.story.Journal;
import com.yearofice.story.ambition.Ambitions;
import com.yearofice.story.keepsake.KeepsakeItem;
import com.yearofice.story.keepsake.KeepsakeRegistry;
import com.yearofice.story.keepsake.NamedAnimals;
import com.yearofice.story.keepsake.OutpostKeepsakes;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** M9 lane D story GameTests: the sked reads the real forecast, a drop lands with its manifest, ambitions open on a trigger, keepsakes clip once. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class RadioGameTests {
    private RadioGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:story/radio_sked_reads_forecast", 100, RadioGameTests::skedReadsForecast));
        out.add(test("yearofice:story/supply_drop_lands", 600, RadioGameTests::dropLands));
        out.add(test("yearofice:story/ambition_opens_on_trigger", 100, RadioGameTests::ambitionOpens));
        out.add(test("yearofice:story/keepsake_clip_and_place", 100, RadioGameTests::keepsakes));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m9d", name, TEMPLATE, maxTicks, 0, true, body);
    }

    /** An unattached server player that only this test holds (same recipe as the M2.5 story tests). */
    static ServerPlayer bot(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "m9d_bot");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 1, 1)));
        p.moveTo(at.x, at.y, at.z, 0, 0);
        return p;
    }

    static void skedReadsForecast(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        var weather = WeatherService.get();
        long day = -1;
        for (long d = 0; d < 400 && day < 0; d++)
            if (weather.at(h.getLevel(), pos, (d + 1) * 1440L + 720).kind() != StormKind.CLEAR) day = d;
        h.assertTrue(day >= 0, "the year table has a storm day in the first 400 days");
        StormKind kind = weather.at(h.getLevel(), pos, (day + 1) * 1440L + 720).kind();
        var lines = Radio.skedLines(h.getLevel(), pos, day * 1440L + 19 * 60);
        String want = "Tomorrow: " + RadioData.skeds().getAsJsonObject("forecast").get(kind.name().toLowerCase(Locale.ROOT)).getAsString();
        h.assertTrue(lines.size() == 8, "open, intro, five outlook days and a news line, got " + lines.size());
        h.assertTrue(lines.get(2).equals(want), "tomorrow's line reads the real storm: want '" + want + "' got '" + lines.get(2) + "'");
        h.assertTrue(!want.contains("Fair"), "the storm day is not read as fair");
        // the battery: a dead desk gives no sked, a charged one pays for it
        h.setBlock(new BlockPos(1, 1, 1), RadioRegistry.RADIO_DESK.get());
        var be = (RadioDeskBlockEntity) h.getBlockEntity(new BlockPos(1, 1, 1));
        h.assertTrue(!be.drain(RadioConfig.get("sked_cost_pct")), "a flat battery cannot pay for a sked");
        be.charge(RadioConfig.get("battery_item_pct"));
        h.assertTrue(be.drain(RadioConfig.get("sked_cost_pct")) && Math.abs(be.battery() - 27) < 1e-9, "a sked costs 3% of a 30% battery, left " + be.battery());
        h.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
        h.succeed();
    }

    static void dropLands(GameTestHelper h) {
        BlockPos zone = h.absolutePos(new BlockPos(1, 1, 1));
        // scatter: the crate is released within the configured radius of the zone
        var probe = SupplyDrop.fly(h.getLevel(), zone);
        double off = Math.hypot(probe.getX() - (zone.getX() + 0.5), probe.getZ() - (zone.getZ() + 0.5));
        h.assertTrue(off <= RadioConfig.get("scatter_m") + 1.5, "released " + off + " m from the zone");
        probe.discard();
        var drop = SupplyDrop.fly(h.getLevel(), zone, 0);
        int x = drop.blockPosition().getX(), z = drop.blockPosition().getZ();
        h.succeedWhen(() -> {
            int y = h.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos at = new BlockPos(x, y, z);
            h.assertTrue(drop.isRemoved(), "the parachute is still coming down");
            h.assertTrue(h.getLevel().getBlockState(at).is(Blocks.BARREL), "the crate stands where the parachute came down");
            var c = (Container) h.getLevel().getBlockEntity(at);
            boolean spray = false, seed = false;
            for (int i = 0; i < c.getContainerSize(); i++) {
                String id = BuiltInRegistries.ITEM.getKey(c.getItem(i).getItem()).toString();
                spray |= id.equals("yearofice:bear_spray");
                seed |= id.equals("yearofice:turnip_seed");
            }
            h.assertTrue(spray && seed, "the manifest filled the crate (bear spray " + spray + ", turnip seed " + seed + ")");
            h.getLevel().setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        });
    }

    static void ambitionOpens(GameTestHelper h) {
        var p = bot(h);
        Ambitions.refresh(p, 0);
        h.assertTrue(AmbitionTracker.isOpen(p, "spark") && !AmbitionTracker.isOpen(p, "pike"), "day 0 opens the first days, not the first summer");
        h.assertTrue(!AmbitionTracker.isOpen(p, "bonded"), "bonded waits for its trigger");
        Ambitions.fire(p, "wolf.tolerant", 100);
        h.assertTrue(AmbitionTracker.isOpen(p, "bonded"), "a wolf at Tolerant opens Bonded");
        h.assertTrue(Ambitions.page(p).stream().anyMatch(l -> l.startsWith("[ ] ???")), "a hidden ambition reads ??? until done");
        Ambitions.fire(p, "wolf.bonded", 200);
        h.assertTrue(AmbitionTracker.isDone(p, "bonded") && !AmbitionTracker.isOpen(p, "bonded"), "Bonded closes on its measure");
        h.assertTrue(Journal.entries(p).stream().filter(e -> "ambition".equals(e.kind()) && "bonded".equals(e.cause())).count() == 1, "one journal line");
        Ambitions.fire(p, "wolf.bonded", 300);
        h.assertTrue(Journal.entries(p).stream().filter(e -> "ambition".equals(e.kind()) && "bonded".equals(e.cause())).count() == 1, "a repeat adds nothing");
        h.assertTrue(AmbitionTracker.isDone(p, "wolf_at_the_door"), "the same trigger closed Wolf at the door on the way");
        Ambitions.refresh(p, 40);
        h.assertTrue(AmbitionTracker.isOpen(p, "pike"), "June 1 opens the first summer");
        h.assertTrue(Ambitions.all().size() >= 60, "the catalogue holds " + Ambitions.all().size());
        h.succeed();
    }

    static void keepsakes(GameTestHelper h) {
        var p = bot(h);
        var stack = KeepsakeRegistry.stack("fur_ledger");
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        ((KeepsakeItem) stack.getItem()).use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        ((KeepsakeItem) stack.getItem()).use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(Journal.entries(p).stream().filter(e -> "keepsake".equals(e.kind())).count() == 1, "reading twice clips the journal once");
        h.assertTrue(stack.getHoverName().getString().equals("The fur ledger"), "the name comes from the data: " + stack.getHoverName().getString());
        int n = OutpostKeepsakes.place(h.getLevel(), h.absolutePos(BlockPos.ZERO), "trapline_cabin");
        h.assertTrue(n == 2, "the trapline cabin holds two keepsakes, placed " + n);
        var c = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1, 1, 1)));
        h.assertTrue(c.getItem(0).is(KeepsakeRegistry.NOTE.get()), "a note sits in the cabin's box");
        h.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
        h.setBlock(new BlockPos(1, 1, 2), Blocks.AIR);
        String a = NamedAnimals.met(p, "wolf", 1440), b = NamedAnimals.met(p, "wolf", 2880);
        h.assertTrue(!a.isEmpty() && a.equals(b) && a.equals(NamedAnimals.nameOf(p, "wolf")), "a wolf met twice keeps its name: " + a + "/" + b);
        h.assertTrue(Journal.entries(p).stream().anyMatch(e -> "named_animal_again".equals(e.kind()) && e.line().contains(a)), "the second meeting is journalled by name");
        h.assertTrue(NamedAnimals.met(p, "moose", 1).isEmpty(), "moose have no names");
        h.succeed();
    }
}
