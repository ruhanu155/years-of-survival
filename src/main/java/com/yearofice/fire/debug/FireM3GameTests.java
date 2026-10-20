package com.yearofice.fire.debug;

import com.mojang.authlib.GameProfile;
import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.WaterContent;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.env.LightState;
import com.yearofice.body.BodyAttachments;
import com.yearofice.build.BuildRegistry;
import com.yearofice.core.YearOfIce;
import com.yearofice.env.light.HeldLights;
import com.yearofice.env.light.LightField;
import com.yearofice.fire.FireConfig;
import com.yearofice.fire.FireDangerIndex;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.fire.FlameBlockEntity;
import com.yearofice.fire.light.LightBurn;
import com.yearofice.fire.light.LightItem;
import com.yearofice.fire.light.LightItems;
import com.yearofice.fire.water.WaterContainerItem;
import com.yearofice.fire.water.WaterRegistry;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.WaterPurity;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.fire.FireBuild;
import com.yearofice.sim.fire.FireDanger;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.fire.FireWeather;
import com.yearofice.sim.fire.Vessel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m3_fire: water, light, the fire builds, the ferro rod, the smother and the spread, on a real server and on
 * Standard (Ready for development › M3, lane C). Every ring is paused so the live clock never moves it; spans run through
 * {@link FireRingBlockEntity#stepMinutes}. Mock players are detached server players, as in the body tests.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class FireM3GameTests {
    private FireM3GameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final String BATCH = "m3_fire";
    static final BlockPos RING = FireGameTests.RING;

    @GameTestGenerator
    public static Collection<TestFunction> m3FireTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:fire/pot_boils_2l_steady", 200, FireM3GameTests::potBoils2lSteady));
        out.add(test("yearofice:fire/boiled_water_drinks_safe", 200, FireM3GameTests::boiledWaterDrinksSafe));
        out.add(test("yearofice:fire/pot_rules", 200, FireM3GameTests::potRules));
        out.add(test("yearofice:fire/pot_survives_save", 200, FireM3GameTests::potSurvivesSave));
        out.add(test("yearofice:water/fill_from_source", 200, FireM3GameTests::fillFromSource));
        out.add(test("yearofice:fire/ferro_rod", 200, FireM3GameTests::ferroRod));
        out.add(test("yearofice:fire/builds", 200, FireM3GameTests::builds));
        out.add(test("yearofice:fire/smother_guard", 200, FireM3GameTests::smotherGuard));
        out.add(test("yearofice:fire/steady_rain_out_30min", 200, FireM3GameTests::steadyRainOut));
        out.add(test("yearofice:fire/spread_packed_fire", 200, FireM3GameTests::spreadPackedFire));
        out.add(test("yearofice:fire/dakota_never_sparks", 200, FireM3GameTests::dakotaNeverSparks));
        out.add(test("yearofice:fire/roster_lists_my_fires", 200, FireM3GameTests::rosterListsMyFires));
        out.add(test("yearofice:light/held_light_field", 200, FireM3GameTests::heldLightField));
        out.add(test("yearofice:light/burn_down_and_rain", 200, FireM3GameTests::burnDownAndRain));
        return out;
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, TEMPLATE, maxTicks, 0, true, body);
    }

    /** A detached server player with a body, as {@code BodyGameTests.player}. */
    static ServerPlayer player(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "fire-test");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        p.setData(BodyAttachments.BODY.get(), new BodyState());
        return p;
    }

    static FireRingBlockEntity ring(GameTestHelper h) {
        FireRingBlockEntity be = FireGameTests.ring(h);
        be.pause(true);
        FireRingBlockEntity.resetSmotherGuard();
        return be;
    }

    /** A burning Steady fire with plenty of fuel. */
    static FireRingBlockEntity steadyFire(GameTestHelper h) {
        FireRingBlockEntity be = ring(h);
        be.forceState(FireState.FIRE, FireSetting.STEADY);
        be.setFuel(4);
        return be;
    }

    static ItemStack water(Item item, double litres, WaterPurity.Source src) {
        ItemStack s = new ItemStack(item);
        WaterContent.write(s, Vessel.fresh(litres, src));
        return s;
    }

    static Vessel potWater(FireRingBlockEntity be) { return WaterContent.read(be.pot()); }

    /** Boils a 2 L pot of river water on a Steady fire; returns the minutes to the first boil. */
    static int boil(GameTestHelper h, FireRingBlockEntity be, ServerPlayer p) {
        ItemStack pot = water(WaterRegistry.COOKING_POT.get(), 2, WaterPurity.Source.RIVER);
        h.assertTrue(be.placePot(p, pot, WaterContainerItem.data(pot), 0.99).ok(), "the pot would not stand on the fire");
        int minutes = 0;
        while (!potWater(be).boiling() && minutes < 90) {
            be.stepMinutes(1);
            minutes++;
        }
        return minutes;
    }

    /** Blueprint › Boiling: a 2 L pot on a Steady fire boils in 30 ±5 game minutes, and is safe 5 minutes later. */
    static void potBoils2lSteady(GameTestHelper h) {
        FireRingBlockEntity be = steadyFire(h);
        ServerPlayer p = player(h);
        int minutes = boil(h, be, p);
        h.assertTrue(minutes >= 25 && minutes <= 35, "a 2 L pot boiled after " + minutes + " game min, wanted 30 +-5");
        h.assertFalse(potWater(be).boiled(), "the water is not safe the moment it boils");
        be.stepMinutes(3);
        h.assertFalse(potWater(be).boiled(), "3 minutes at a boil is not enough");
        be.stepMinutes(2);
        Vessel v = potWater(be);
        h.assertTrue(v.boiled() && v.litres() == 2.0, "boiled after 5 more minutes: " + v);
        h.succeed();
    }

    /** The container's water keeps its source; drinking it boiled never rolls the gut illness, unboiled still does. */
    static void boiledWaterDrinksSafe(GameTestHelper h) {
        FireRingBlockEntity be = steadyFire(h);
        ServerPlayer p = player(h);
        boil(h, be, p);
        be.stepMinutes(6);
        ItemStack pot = be.takePot();
        Vessel v = WaterContent.read(pot);
        h.assertTrue(v.boiled(), "the pot should hold boiled water");
        BodyService.get().drink(p, 0.25, v.source(), v.boiled());
        h.assertTrue(BodyAttachments.of(p).gutOnset < 0, "boiled water must not make anyone ill");
        h.assertTrue(BodyAttachments.of(p).water > 0, "the sip counts as a drink");
        // The water in an unboiled bottle still says what it is.
        ItemStack bottle = water(WaterRegistry.STEEL_BOTTLE.get(), 1, WaterPurity.Source.CREEK);
        Vessel u = WaterContent.read(bottle);
        h.assertTrue(!u.boiled() && u.source() == WaterPurity.Source.CREEK, "unboiled creek water: " + u);
        h.succeed();
    }

    /** What stands on a fire and what does not: a bottle in the coals only, a pot that tips, an empty pot, a cold fire. */
    static void potRules(GameTestHelper h) {
        FireRingBlockEntity be = steadyFire(h);
        ServerPlayer p = player(h);
        ItemStack bottle = water(WaterRegistry.STEEL_BOTTLE.get(), 1, WaterPurity.Source.RIVER);
        h.assertFalse(be.placePot(p, bottle, WaterContainerItem.data(bottle), 0.99).ok(), "a bottle must not stand in the flames");
        h.assertTrue(be.pot().isEmpty(), "nothing on the fire");
        ItemStack bucket = water(WaterRegistry.BARK_BUCKET.get(), 4, WaterPurity.Source.RIVER);
        h.assertFalse(be.placePot(p, bucket, WaterContainerItem.data(bucket), 0.99).ok(), "bark would burn");
        ItemStack empty = new ItemStack(WaterRegistry.COOKING_POT.get());
        h.assertFalse(be.placePot(p, empty, WaterContainerItem.data(empty), 0.99).ok(), "an empty pot has nothing to boil");
        // Tipping: a pot set on a burning fire spills when the roll is under pot_tip_pct.
        ItemStack pot = water(WaterRegistry.COOKING_POT.get(), 2, WaterPurity.Source.RIVER);
        h.assertFalse(be.placePot(p, pot, WaterContainerItem.data(pot), 0.0).ok(), "the pot should tip on roll 0");
        h.assertTrue(WaterContent.read(pot).isEmpty(), "a tipped pot is empty");
        // On coals the bottle stands and nothing tips.
        be.forceState(FireState.COALS, null);
        h.assertTrue(be.placePot(p, bottle, WaterContainerItem.data(bottle), 0.0).ok(), "a bottle stands in the coals");
        be.stepMinutes(20);
        h.assertTrue(potWater(be).heat() > 0.5, "the bottle warms in the coals: " + potWater(be));
        h.assertFalse(be.takePot().isEmpty(), "the bottle comes back");
        // A cold ring takes no pot.
        be.forceState(FireState.OUT, null);
        ItemStack again = water(WaterRegistry.COOKING_POT.get(), 1, WaterPurity.Source.RIVER);
        h.assertFalse(be.placePot(p, again, WaterContainerItem.data(again), 0.99).ok(), "no pot on a cold ring");
        h.succeed();
    }

    /** The pot, the build and the builder are in the ring's save. */
    static void potSurvivesSave(GameTestHelper h) {
        FireRingBlockEntity be = ring(h);
        ServerPlayer p = player(h);
        h.assertTrue(be.setBuild(FireBuild.TEEPEE).ok(), "a teepee on a bare ring");
        be.forceState(FireState.FIRE, FireSetting.STEADY);
        be.setFuel(4);
        boil(h, be, p);
        h.assertTrue(be.builder() != null, "the ring remembers who placed the pot");
        CompoundTag tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
        FireRingBlockEntity copy = new FireRingBlockEntity(h.absolutePos(RING), be.getBlockState());
        copy.loadWithComponents(tag, h.getLevel().registryAccess());
        Vessel a = potWater(be), b = potWater(copy);
        h.assertTrue(a.equals(b), "pot water " + a + " vs " + b);
        h.assertTrue(copy.fire().build() == FireBuild.TEEPEE, "build " + copy.fire().build());
        h.assertTrue(p.getUUID().equals(copy.builder()), "builder " + copy.builder());
        h.succeed();
    }

    /** A container fills from a water source with that water's kind, tops up only the same, and refuses a mix. */
    static void fillFromSource(GameTestHelper h) {
        BlockPos water = new BlockPos(0, 1, 0);
        h.setBlock(water, Blocks.WATER);
        ServerPlayer p = player(h);
        ServerLevel level = h.getLevel();
        BlockPos abs = h.absolutePos(water);
        WaterPurity.Source src = BodyService.get().waterSourceAt(level, abs);
        ItemStack bottle = new ItemStack(WaterRegistry.STEEL_BOTTLE.get());
        WaterContainerItem.fill(level, p, bottle, WaterContainerItem.data(bottle), WaterContent.read(bottle), abs);
        Vessel v = WaterContent.read(bottle);
        h.assertTrue(Math.abs(v.litres() - 1.0) < 1e-9 && !v.boiled() && v.source() == src, "filled: " + v);
        // Boiled water in a container is not topped up from the river.
        WaterContent.write(bottle, new Vessel(0.5, src, true, 0, 0));
        WaterContainerItem.fill(level, p, bottle, WaterContainerItem.data(bottle), WaterContent.read(bottle), abs);
        Vessel w = WaterContent.read(bottle);
        h.assertTrue(w.boiled() && Math.abs(w.litres() - 0.5) < 1e-9, "boiled water stays as it is: " + w);
        h.succeed();
    }

    /** The ferro rod: 90% in calm air, 60% in wind; the rod wears a notch a strike; the spark makes an ember. */
    static void ferroRod(GameTestHelper h) {
        FireRingBlockEntity be = ring(h);
        FireGameTests.layInOrder(h, be);
        ServerPlayer p = player(h);
        ItemStack rod = new ItemStack(FireRegistry.FIRE_STARTER.get());
        be.setWeatherOverride(new FireWeather(Precip.NONE, false, 8, false));
        h.assertFalse(be.strikeFerro(p, rod, 0.75).ok(), "a 0.75 roll misses in wind (60%)");
        h.assertTrue(rod.getDamageValue() == 1, "a miss still wears the rod: " + rod.getDamageValue());
        be.setWeatherOverride(FireWeather.CALM);
        h.assertTrue(be.strikeFerro(p, rod, 0.75).ok(), "a 0.75 roll lights in calm air (90%)");
        h.assertTrue(rod.getDamageValue() == 2, "rod wear " + rod.getDamageValue());
        h.assertTrue(be.fire().state() == FireState.EMBER, "the spark makes an ember, was " + be.fire().state());
        be.puff();
        be.puff();
        be.stepMinutes(5);
        h.assertTrue(be.fire().state() == FireState.FIRE, "a fire after 5 min, was " + be.fire().state());
        h.succeed();
    }

    /** Builds change the burn, take their own wood, and a Dakota hole needs a pit. */
    static void builds(GameTestHelper h) {
        FireRingBlockEntity be = ring(h);
        double[] left = new double[4];
        for (FireBuild b : FireBuild.values()) {
            if (b == FireBuild.DAKOTA) continue;
            h.setBlock(RING, Blocks.AIR);
            be = ring(h);
            h.assertTrue(be.setBuild(b).ok(), "set " + b);
            be.forceState(FireState.FIRE, FireSetting.STEADY);
            be.setFuel(1);
            left[b.ordinal()] = be.fire().minutesLeft(FireConfig.tuning());
        }
        h.setBlock(RING, Blocks.AIR);
        be = ring(h);
        double ringMin = left[FireBuild.RING.ordinal()];
        h.assertTrue(Math.abs(left[FireBuild.TEEPEE.ordinal()] / ringMin - 1.5) < 0.01, "teepee 60/40 of a ring: " + left[1] / ringMin);
        h.assertTrue(Math.abs(left[FireBuild.LONG.ordinal()] / ringMin - 4.5) < 0.01, "long fire 180/40 of a ring: " + left[2] / ringMin);
        // A teepee stands on splits, not whole logs.
        h.assertTrue(be.setBuild(FireBuild.TEEPEE).ok(), "a teepee on a bare ring");
        h.assertFalse(be.addPart(new ItemStack(Items.SPRUCE_LOG), FireConfig.tuning()).ok(), "a teepee takes no whole log");
        // A Dakota hole needs a pit of three walls.
        h.assertFalse(be.setBuild(FireBuild.DAKOTA).ok(), "no Dakota hole without a pit");
        h.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        h.setBlock(new BlockPos(2, 1, 1), Blocks.STONE);
        h.setBlock(new BlockPos(1, 1, 0), Blocks.STONE);
        h.assertTrue(be.hasPit(), "three walls make a pit");
        h.assertTrue(be.setBuild(FireBuild.DAKOTA).ok(), "a Dakota hole in a pit");
        h.assertTrue(be.fire().bare(), "the lay is still bare");
        // The empty hand walks the builds on a bare ring.
        be.setBuild(FireBuild.RING);
        h.useBlock(RING, h.makeMockPlayer(GameType.SURVIVAL));
        h.assertTrue(be.fire().build() == FireBuild.TEEPEE, "the empty hand picks the next build, was " + be.fire().build());
        h.succeed();
    }

    /** Pouring on a fire needs a second deliberate use the first time each day; coals are never guarded. */
    static void smotherGuard(GameTestHelper h) {
        FireRingBlockEntity be = steadyFire(h);
        ServerPlayer p = player(h);
        ItemStack pot = water(WaterRegistry.COOKING_POT.get(), 1, WaterPurity.Source.RIVER);
        h.assertFalse(be.pourOn(p, pot).ok(), "the first pour is only armed");
        h.assertTrue(be.fire().state() == FireState.FIRE && !WaterContent.read(pot).isEmpty(), "nothing poured yet");
        h.assertTrue(be.pourOn(p, pot).ok(), "the second use pours");
        h.assertTrue(be.fire().state() == FireState.OUT, "put out, was " + be.fire().state());
        h.assertTrue(WaterContent.read(pot).isEmpty(), "the pot is empty");
        // Same day, same player: no guard any more.
        be.forceState(FireState.FIRE, FireSetting.STEADY);
        WaterContent.write(pot, Vessel.fresh(1, WaterPurity.Source.RIVER));
        h.assertTrue(be.pourOn(p, pot).ok() && be.fire().state() == FireState.OUT, "a second fire the same day goes out at once");
        // Coals are never guarded, even for a new player.
        FireRingBlockEntity.resetSmotherGuard();
        be.forceState(FireState.COALS, null);
        WaterContent.write(pot, Vessel.fresh(1, WaterPurity.Source.RIVER));
        h.assertTrue(be.pourOn(player(h), pot).ok() && be.fire().state() == FireState.OUT, "coals go out at once");
        h.succeed();
    }

    /** Heavy rain takes a Steady fire down a state each 10 game minutes: out inside 30. */
    static void steadyRainOut(GameTestHelper h) {
        FireRingBlockEntity be = steadyFire(h);
        be.setWeatherOverride(FireWeather.heavyRain());
        be.stepMinutes(10);
        h.assertTrue(be.fire().state() == FireState.FIRE, "still burning at 10 min, was " + be.fire().state());
        be.stepMinutes(20);
        h.assertTrue(be.fire().state() == FireState.OUT, "out by 30 min, was " + be.fire().state());
        h.succeed();
    }

    static BlockPos bed(GameTestHelper h) {
        BlockPos bed = new BlockPos(1, 1, 2);
        h.setBlock(bed, BuildRegistry.BOUGH_BED.get());
        return bed;
    }

    /** A packed fire's spark lights a burnable piece within 2 m as a flame that burns the piece away. */
    static void spreadPackedFire(GameTestHelper h) {
        FireRingBlockEntity be = steadyFire(h);
        be.setFuel(5);
        h.assertTrue(be.fire().packed(FireConfig.tuning()), "5 armloads is packed");
        BlockPos bed = bed(h);
        FireDangerIndex.pin(h.getLevel(), FireDanger.Level.EXTREME);
        try {
            h.assertFalse(be.rollSpread(0.99), "a 0.99 roll misses even at Extreme (15%)");
            h.assertTrue(h.getBlockState(bed).is(BuildRegistry.BOUGH_BED.get()), "the bed is still a bed");
            h.assertTrue(be.rollSpread(0.0), "a 0.0 roll lights it");
            h.assertTrue(h.getBlockState(bed).is(FireRegistry.FLAME.get()), "the bed is a flame: " + h.getBlockState(bed));
            FlameBlockEntity flame = h.getBlockEntity(bed);
            h.assertTrue(flame.minutesLeft() == Math.round(FireConfig.tuning().flameBurnMin()), "flame timer " + flame.minutesLeft());
            flame.clockMinutes(flame.minutesLeft());
            h.assertTrue(h.getBlockState(bed).isAir(), "the flame and the piece are gone: " + h.getBlockState(bed));
        } finally {
            FireDangerIndex.pin(h.getLevel(), null);
        }
        h.succeed();
    }

    /** A Dakota hole never sparks, packed or not, whatever the danger. */
    static void dakotaNeverSparks(GameTestHelper h) {
        h.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        h.setBlock(new BlockPos(2, 1, 1), Blocks.STONE);
        h.setBlock(new BlockPos(1, 1, 0), Blocks.STONE);
        FireRingBlockEntity be = ring(h);
        h.assertTrue(be.setBuild(FireBuild.DAKOTA).ok(), "a Dakota hole in a pit");
        be.forceState(FireState.FIRE, FireSetting.STEADY);
        be.setFuel(6);
        BlockPos bed = bed(h);
        FireDangerIndex.pin(h.getLevel(), FireDanger.Level.EXTREME);
        try {
            be.stepMinutes(300);
            h.assertTrue(h.getBlockState(bed).is(BuildRegistry.BOUGH_BED.get()), "a Dakota hole lit the bed: " + h.getBlockState(bed));
        } finally {
            FireDangerIndex.pin(h.getLevel(), null);
        }
        h.succeed();
    }

    /** The roster lists the fires a player built, lit, least fuel first, with the day's wood; another player sees none. */
    static void rosterListsMyFires(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.setPos(h.absolutePos(RING).getCenter());
        FireRingBlockEntity a = ring(h);
        h.assertTrue(a.addPart(new ItemStack(FireRegistry.TINDER_BUNDLE.get()), FireConfig.tuning(), p).ok(), "lay tinder");
        a.forceState(FireState.FIRE, FireSetting.STEADY);
        a.setFuel(3);
        BlockPos second = new BlockPos(0, 1, 0);
        h.setBlock(second.below(), Blocks.STONE);
        h.setBlock(second, FireRegistry.FIRE_RING.get());
        FireRingBlockEntity b = h.getBlockEntity(second);
        b.pause(true);
        b.setWeatherOverride(FireWeather.CALM);
        h.assertTrue(b.addPart(new ItemStack(FireRegistry.TINDER_BUNDLE.get()), FireConfig.tuning(), p).ok(), "lay tinder");
        b.forceState(FireState.FIRE, FireSetting.LOW);
        b.setFuel(1);
        List<com.yearofice.fire.FireRoster.Entry> es = com.yearofice.fire.FireRoster.of(p);
        h.assertTrue(es.size() == 2, "two fires listed: " + es.size());
        h.assertTrue(es.get(0).row().minutesLeft() <= es.get(1).row().minutesLeft(), "least fuel first");
        h.assertTrue(com.yearofice.fire.FireRoster.header(es).startsWith("2 fires: about "), com.yearofice.fire.FireRoster.header(es));
        b.forceState(FireState.COALS, null);
        h.assertTrue(com.yearofice.fire.FireRoster.of(p).size() == 2, "coals are listed");
        b.forceState(FireState.OUT, null);
        h.assertTrue(com.yearofice.fire.FireRoster.of(p).size() == 1, "a fire that is out is not");
        h.assertTrue(com.yearofice.fire.FireRoster.of(player(h)).isEmpty(), "somebody else's fires are not mine");
        // A pot on a fire under 15 minutes of fuel turns it amber.
        a.forceState(FireState.COALS, null);
        a.forceState(FireState.FIRE, FireSetting.STEADY);
        a.setFuel(0.1);
        ItemStack pot = water(WaterRegistry.COOKING_POT.get(), 1, WaterPurity.Source.RIVER);
        h.assertTrue(a.placePot(p, pot, WaterContainerItem.data(pot), 0.99).ok(), "pot on");
        h.assertTrue(a.rosterRow(100).amber(), "amber with a pot and under 15 minutes: " + a.rosterRow(100));
        h.succeed();
    }

    static ItemStack lit(Item item) {
        ItemStack s = new ItemStack(item);
        LightState.set(s, true, s.getItemHolder().getData(com.yearofice.api.env.LightData.MAP).burnMin());
        return s;
    }

    /** A lit torch in hand lights the blocks round it by distance; unlit, it lights nothing; env.light reports it. */
    static void heldLightField(GameTestHelper h) {
        ServerPlayer p = player(h);
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(1, 1, 1));
        p.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, lit(LightItems.BIRCH_BARK_TORCH.get()));
            HeldLights.refresh(p, 0);
            BlockPos eye = BlockPos.containing(p.getEyePosition());
            int near = LightField.held(level, eye);
            h.assertTrue(near >= 9 && near <= 10, "a bark torch at the carrier: " + near);
            int five = LightField.held(level, eye.east(5));
            h.assertTrue(five >= 4 && five <= 6, "5 blocks away: " + five);
            h.assertTrue(LightField.held(level, eye.east(17)) == 0, "beyond 16 blocks nothing");
            h.assertTrue(EnvService.get().light(level, eye) >= near, "env.light carries the held light");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LightItems.BIRCH_BARK_TORCH.get()));
            HeldLights.refresh(p, 0);
            h.assertTrue(LightField.held(level, eye) == 0, "an unlit torch gives no light");
            // A flat-cold headlamp loses 3 levels below -30 C.
            p.setItemInHand(InteractionHand.MAIN_HAND, lit(LightItems.HEADLAMP.get()));
            HeldLights.refresh(p, 0);
            int warm = LightField.held(level, eye);
            HeldLights.refresh(p, -35);
            int cold = LightField.held(level, eye);
            h.assertTrue(warm - cold == HeldLights.COLD_DROP, "headlamp " + warm + " warm, " + cold + " at -35 C");
        } finally {
            HeldLights.forget(p.getUUID());
        }
        h.succeed();
    }

    /** Lights burn down by game minutes; rain puts a torch out and a pitch torch holds; the headlamp drains in the cold. */
    static void burnDownAndRain(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack torch = lit(LightItems.BIRCH_BARK_TORCH.get());
        p.getInventory().setItem(0, torch);
        LightBurn.burn(p, 10, 0.5, false, false, 0);
        h.assertTrue(LightItem.lit(torch) && torch.get(LightState.LIGHT_STATE.get()).left() == 5, "5 minutes left: " + torch.get(LightState.LIGHT_STATE.get()));
        LightBurn.burn(p, 5, 0.5, false, false, 0);
        h.assertTrue(p.getInventory().getItem(0).isEmpty(), "a burned-out torch is gone");

        ItemStack bark = lit(LightItems.BIRCH_BARK_TORCH.get());
        p.getInventory().setItem(0, bark);
        LightBurn.burn(p, 1, 0.5, true, false, 0);
        h.assertFalse(LightItem.lit(bark), "light rain puts a bark torch out");
        h.assertTrue(bark.get(LightState.LIGHT_STATE.get()).left() == 15, "and it keeps its minutes");

        ItemStack pitch = lit(LightItems.PITCH_TORCH.get());
        p.getInventory().setItem(1, pitch);
        LightBurn.burn(p, 1, 0.0, true, false, 0);
        h.assertTrue(LightItem.lit(pitch), "a pitch torch holds in light rain");
        LightBurn.burn(p, 1, 0.9, true, true, 0);
        h.assertTrue(LightItem.lit(pitch), "and in heavy rain on a good roll");
        LightBurn.burn(p, 1, 0.1, true, true, 0);
        h.assertFalse(LightItem.lit(pitch), "heavy rain has a 20% chance a minute to put it out");

        ItemStack lamp = lit(LightItems.HEADLAMP.get());
        p.getInventory().setItem(2, lamp);
        LightBurn.burn(p, 10, 0.0, true, true, -35);
        h.assertTrue(LightItem.lit(lamp) && lamp.get(LightState.LIGHT_STATE.get()).left() == 1200 - 20,
                "a headlamp ignores rain and drains twice as fast below -30 C: " + lamp.get(LightState.LIGHT_STATE.get()));
        h.succeed();
    }
}
