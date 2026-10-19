package com.yearofice.food.debug;

import com.mojang.authlib.GameProfile;
import com.yearofice.api.body.BodyService;
import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.food.FreshnessPass;
import com.yearofice.food.Meals;
import com.yearofice.food.cooking.SpitBlockEntity;
import com.yearofice.food.cooking.SpitPlacing;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.fire.FireWeather;
import com.yearofice.sim.food.Doneness;
import com.yearofice.sim.food.EatingLadder;
import com.yearofice.sim.food.FoodValues;
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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m2_5, cooking and freshness on a real server (Ready for development › M2.5 test 4 and the slice brief's
 * cooking tests). Each test builds its own ring on stone inside {@code gametest/empty3}, pins the ring's weather,
 * pauses its spit and steps ring and spit together one game minute at a time (the closed-form catch-up), so neither
 * the real tick nor another test's clock moves reach it. Players are unattached server players, never in the level's
 * player list. Freshness is checked from the stack's own due point, never by moving the shared day time.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class FoodGameTests {
    private FoodGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos RING = new BlockPos(1, 1, 1);

    @GameTestGenerator
    public static Collection<TestFunction> foodTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:food/slice_cook_meat", FoodGameTests::sliceCookMeat));
        out.add(test("yearofice:food/burnt_when_left", FoodGameTests::burntWhenLeft));
        out.add(test("yearofice:food/fish_cooked_then_eaten", FoodGameTests::fishCookedThenEaten));
        out.add(test("yearofice:food/decayed_eating", FoodGameTests::decayedEating));
        return out;
    }

    static TestFunction test(String name, Consumer<GameTestHelper> body) {
        return new TestFunction("m2_5", name, TEMPLATE, 100, 0, true, body);
    }

    // ---- fixtures ------------------------------------------------------------------------------------------

    static FireRingBlockEntity ring(GameTestHelper h, FireSetting setting, double armloads) {
        h.setBlock(RING.below(), Blocks.STONE);
        h.setBlock(RING, FireRegistry.FIRE_RING.get());
        FireRingBlockEntity be = h.getBlockEntity(RING);
        be.setWeatherOverride(FireWeather.CALM);
        be.forceState(FireState.FIRE, setting);
        be.setFuel(armloads);
        be.stepMinutes(0);
        return be;
    }

    static ServerPlayer player(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "food-test");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        return p;
    }

    /** A raw stack stamped exactly new (p = 0), as a just-dressed hare or a just-landed fish is to within one band. */
    static ItemStack rawExact(GameTestHelper h, Item item, int count, Integer weightG) {
        ItemStack s = new ItemStack(item, count);
        if (weightG != null) s.set(YoiComponents.WEIGHT_G.get(), weightG);
        FoodValues v = FoodMap.values(s);
        h.assertTrue(v != null, "no food row for " + item);
        double life = v.refLifeDh(FoodState.RAW);
        double d = DecayService.get().dNow(h.getLevel());
        s.set(YoiComponents.FRESH.get(), new Fresh(FoodState.RAW, DecayService.CLOCK_AIR_SLICE, d + life, life, 0, true));
        return s;
    }

    /** Sets a spit over the ring from the player's hand, as right mouse on the ring does, and pauses its tick. */
    static SpitBlockEntity spitOn(GameTestHelper h, ServerPlayer p, ItemStack food) {
        p.setItemInHand(InteractionHand.MAIN_HAND, food);
        h.assertTrue(SpitPlacing.tryPlace(h.getLevel(), h.absolutePos(RING), p, InteractionHand.MAIN_HAND), "the food did not go on a spit");
        SpitBlockEntity spit = h.getBlockEntity(RING.above());
        spit.pause(true);
        return spit;
    }

    static void step(FireRingBlockEntity ring, SpitBlockEntity spit, int minutes) {
        for (int i = 0; i < minutes; i++) {
            ring.stepMinutes(1);
            spit.stepMinutes(1);
        }
    }

    static Fresh fresh(ItemStack s) { return s.get(YoiComponents.FRESH.get()); }

    // ---- tests ---------------------------------------------------------------------------------------------

    /**
     * Ready for development › M2.5 test 4: raw hare roasts to cooked in 20 game minutes, and cooked meat lasts its
     * {@code spoil_days_at_5c} at 5 °C ±5%. A whole hare's 9 units on a Steady fire: 25 minutes' cook time (Blueprint ›
     * Cooking), so Raw until 12.5, cooked (Rare) from 12.5 and still at 20, Done from 22.5. Cooked hare's safe life
     * (to Turning) is 9 days at 5 °C.
     */
    static void sliceCookMeat(GameTestHelper h) {
        FireRingBlockEntity ring = ring(h, FireSetting.STEADY, 1.0);
        ServerPlayer p = player(h);
        SpitBlockEntity spit = spitOn(h, p, rawExact(h, EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 9, null));
        h.assertTrue(spit.load().getCount() == 9 && p.getMainHandItem().isEmpty(), "the whole hare should be on the spit");
        h.assertTrue(spit.fire() == Doneness.Fire.STEADY, "the spit reads " + spit.fire());
        h.assertTrue(Math.abs(spit.spit().cookMinutes() - 25) < 1e-9, "cook time " + spit.spit().cookMinutes());
        step(ring, spit, 12);
        h.assertTrue(spit.ring() == Doneness.Ring.RAW && fresh(spit.load()).state() == FoodState.RAW, "raw at 12 min, was " + spit.ring());
        step(ring, spit, 8);
        h.assertTrue(spit.ring() == Doneness.Ring.RARE, "cooked (Rare) at 20 min, was " + spit.ring());
        h.assertTrue(fresh(spit.load()).state() == FoodState.COOKED, "the stack turns cooked once it is Rare");
        step(ring, spit, 3);
        h.assertTrue(spit.ring() == Doneness.Ring.DONE, "Done at 23 min, was " + spit.ring());
        ItemStack out = spit.takeOff();
        h.assertTrue(out.getCount() == 9 && out.is(EcosystemRegistry.SNOWSHOE_HARE_MEAT.get()), "took off " + out);
        h.assertTrue(out.get(YoiComponents.DONENESS.get()) == Doneness.Ring.DONE, "doneness " + out.get(YoiComponents.DONENESS.get()));
        Fresh f = fresh(out);
        h.assertTrue(f.state() == FoodState.COOKED, "state " + f.state());
        double days = DecayService.get().daysUntil(h.getLevel(), out, 0.7);
        h.assertTrue(DecayService.get().airC(h.getLevel()) == 5.0, "GameTests run at the 5 C stand-in");
        h.assertTrue(Math.abs(days - 9.0) <= 0.45, "cooked hare keeps " + days + " days at 5 C, want 9 +-5%");
        double d = DecayService.get().dNow(h.getLevel());
        h.assertTrue(f.stage(d + 0.95 * 9 * 24) == 1, "Aging at 95% of 9 days");
        h.assertTrue(f.stage(d + 1.05 * 9 * 24) == 2, "Turning at 105% of 9 days");
        h.succeed();
    }

    /**
     * Left on the fire, meat burns at 150% of its cook time on Steady (37.5 min for a whole hare) and at 112.5% on
     * Hot; turning the fire down does not unburn it, and a burnt bite books 70% of its calories.
     */
    static void burntWhenLeft(GameTestHelper h) {
        FireRingBlockEntity ring = ring(h, FireSetting.STEADY, 3.0);
        ServerPlayer p = player(h);
        SpitBlockEntity spit = spitOn(h, p, rawExact(h, EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 9, null));
        step(ring, spit, 37);
        h.assertTrue(spit.ring() == Doneness.Ring.DONE, "Done at 37 min on Steady, was " + spit.ring());
        step(ring, spit, 1);
        h.assertTrue(spit.ring() == Doneness.Ring.BURNT, "burnt at 38 min on Steady, was " + spit.ring());
        ring.setSetting(FireSetting.LOW);
        step(ring, spit, 1);
        h.assertTrue(spit.ring() == Doneness.Ring.BURNT, "a Low fire does not unburn it");
        ItemStack burnt = spit.takeOff();
        h.assertTrue(burnt.get(YoiComponents.DONENESS.get()) == Doneness.Ring.BURNT, "taken off burnt");

        ring.setFuel(2.0); // a Hot fire packs over 2 armloads
        ring.setSetting(FireSetting.HOT);
        // A third of a hare (8.3 min cook time), so two armloads at Hot's x3 fuel use outlast it: burnt at 112.5%, 9.4 min.
        SpitBlockEntity hot = spitOn(h, p, rawExact(h, EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 3, null));
        h.assertTrue(hot.fire() == Doneness.Fire.HOT, "the spit reads " + hot.fire());
        step(ring, hot, 9);
        h.assertTrue(hot.ring() == Doneness.Ring.DONE, "Done at 9 min on Hot, was " + hot.ring());
        step(ring, hot, 1);
        h.assertTrue(hot.ring() == Doneness.Ring.BURNT && hot.fire() == Doneness.Fire.HOT, "burnt at 10 min on Hot, was " + hot.ring());

        double before = BodyService.get().readout(p).balance24h();
        Meals.Bite bite = Meals.eat(p, burnt);
        double kcal = BodyService.get().readout(p).balance24h() - before;
        h.assertTrue(bite.eaten() && burnt.getCount() == 8, "one burnt unit eaten, " + burnt.getCount() + " left");
        h.assertTrue(Math.abs(kcal - 115 * 0.7) < 1, "a burnt 100 g unit books " + kcal + " kcal, want 80.5");
        h.succeed();
    }

    /**
     * A whole grayling roasts on the spit in 12 game minutes (under 1 kg), keeps its weight, and eating it through the
     * item books its own weight_g: 400 g at 80 kcal per 100 g is 320 kcal.
     */
    static void fishCookedThenEaten(GameTestHelper h) {
        FireRingBlockEntity ring = ring(h, FireSetting.STEADY, 1.0);
        ServerPlayer p = player(h);
        ItemStack fish = rawExact(h, FishingRegistry.ARCTIC_GRAYLING.get(), 1, 400);
        h.assertTrue(Math.abs(fresh(fish).lifeDh() - 3.6 * 24 / 0.7) < 1e-6, "a whole raw grayling's life is 3.6 days");
        SpitBlockEntity spit = spitOn(h, p, fish);
        h.assertTrue(Math.abs(spit.spit().cookMinutes() - 12) < 1e-9, "fish cook time " + spit.spit().cookMinutes());
        step(ring, spit, 10);
        h.assertTrue(spit.ring() == Doneness.Ring.RARE, "Rare at 10 min, was " + spit.ring());
        step(ring, spit, 1);
        h.assertTrue(spit.ring() == Doneness.Ring.DONE, "Done at 11 min, was " + spit.ring());
        ItemStack cooked = spit.takeOff();
        h.assertTrue(fresh(cooked).state() == FoodState.COOKED && cooked.get(YoiComponents.WEIGHT_G.get()) == 400,
                "cooked whole fish keeps its 400 g");
        h.assertTrue(Math.abs(DecayService.get().daysUntil(h.getLevel(), cooked, 0.7) - 9.0) <= 0.45, "cooked fish keeps 9 days");
        p.setItemInHand(InteractionHand.MAIN_HAND, cooked);
        double before = BodyService.get().readout(p).balance24h();
        cooked.finishUsingItem(h.getLevel(), p);
        double kcal = BodyService.get().readout(p).balance24h() - before;
        h.assertTrue(cooked.isEmpty(), "the fish should be eaten");
        h.assertTrue(Math.abs(kcal - 320) < 1, "a 400 g grayling books " + kcal + " kcal, want 320");
        h.succeed();
    }

    /**
     * Decay and time › Eating decayed food: Spoiled cooked meat is eaten with an 80% gut-illness chance (logged:
     * BodyService has no illness hook for food yet) and still books its calories; raw Turning meat is 50% and nausea
     * +40; Rotten food cannot be eaten, and the freshness pass removes it.
     */
    static void decayedEating(GameTestHelper h) {
        ServerPlayer p = player(h);
        ServerLevel level = h.getLevel();
        DecayService decay = DecayService.get();
        double cond = BodyService.get().readout(p).condition(), bmi = BodyService.get().readout(p).bmi();

        ItemStack cooked = new ItemStack(EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 3);
        decay.stamp(level, cooked, FoodState.COOKED);
        cooked.set(YoiComponents.DONENESS.get(), Doneness.Ring.DONE);
        decay.age(level, cooked, 0.95 * 9 / 0.7);
        h.assertTrue(decay.stage(level, cooked) == EatingLadder.SPOILED, "spoiled, stage " + decay.stage(level, cooked));
        double before = BodyService.get().readout(p).balance24h();
        Meals.Bite b = Meals.eat(p, cooked);
        double kcal = BodyService.get().readout(p).balance24h() - before;
        h.assertTrue(b.eaten() && Math.abs(kcal - 115) < 1, "spoiled cooked meat still feeds: " + kcal);
        h.assertTrue(EatingLadder.risk(EatingLadder.Family.GAME_MEAT, EatingLadder.Prep.COOKED, EatingLadder.SPOILED).gutIllness() == 0.8
                && b.outcome().gutIllness() == EatingLadder.gutIllnessChance(EatingLadder.Family.GAME_MEAT, EatingLadder.Prep.COOKED,
                EatingLadder.SPOILED, cond, bmi), "gut illness " + b.outcome().gutIllness());

        ItemStack raw = new ItemStack(EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 2);
        decay.stamp(level, raw, FoodState.RAW);
        decay.age(level, raw, 0.8 * 6 / 0.7);
        h.assertTrue(decay.stage(level, raw) == EatingLadder.TURNING, "turning, stage " + decay.stage(level, raw));
        Meals.Bite r = Meals.eat(p, raw);
        h.assertTrue(r.eaten() && r.outcome().nauseaNow() == 40 && r.outcome().gutIllness() >= 0.5, "raw turning: " + r.outcome());

        decay.age(level, cooked, 2);
        h.assertTrue(decay.stage(level, cooked) == EatingLadder.ROTTEN && !Meals.edible(level, cooked), "rotten should not be edible");
        before = BodyService.get().readout(p).balance24h();
        Meals.Bite gone = Meals.eat(p, cooked);
        h.assertTrue(!gone.eaten() && cooked.getCount() == 2, "rotten food is refused");
        h.assertTrue(BodyService.get().readout(p).balance24h() == before, "nothing booked for rotten food");
        p.getInventory().setItem(0, cooked);
        h.assertTrue(FreshnessPass.settle(p) == 1 && p.getInventory().getItem(0).isEmpty(), "the pass removes rotten food");
        h.succeed();
    }
}
