package com.yearofice.gametest;

import com.yearofice.body.BodyAttachments;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireRegistry;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.item.ItemExpiry;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.item.inventory.LoadCalculator;
import com.yearofice.item.inventory.PackAttachments;
import com.yearofice.item.inventory.PackConfig;
import com.yearofice.item.inventory.PackSkill;
import com.yearofice.item.inventory.PackItems;
import com.yearofice.item.inventory.PackRules;
import com.yearofice.item.inventory.PackSlots;
import com.yearofice.item.inventory.PackSync;
import com.yearofice.item.inventory.Pickup;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.Metabolism;
import com.yearofice.sim.pack.LoadBands;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * The pack's GameTests (Player systems and client › GameTests and client tests; Ready for development › M2; video
 * 2, A11): rows never fill past the pack, taking the pack off and on keeps every stack, walking over items picks
 * none up, the four load bands within 2%, Strength scales the bands, and dropped items expire after 7 game days.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class PackGameTests {
    private PackGameTests() {}

    static final String BATCH = "m2_pack";

    @GameTestGenerator
    public static Collection<TestFunction> packTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:pack/rows_never_fill", 100, PackGameTests::rowsNeverFill));
        out.add(test("yearofice:pack/takeoff_roundtrip", 100, PackGameTests::takeoffRoundtrip));
        out.add(test("yearofice:pack/no_walkover_pickup", 100, PackGameTests::noWalkoverPickup));
        out.add(test("yearofice:pack/load_bands", 100, PackGameTests::loadBands));
        out.add(test("yearofice:pack/strength_scales_bands", 100, PackGameTests::strengthScalesBands));
        out.add(test("yearofice:pack/screen_shows_band", 100, PackGameTests::screenShowsBand));
        out.add(test("yearofice:pack/hauling_practice", 100, PackGameTests::haulingPractice));
        out.add(test("yearofice:pack/pack_min_strength", 100, PackGameTests::packMinStrength));
        out.add(test("yearofice:layers/item_expiry", 100, PackGameTests::itemExpiry));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, LayerGameTests.TEMPLATE, maxTicks, 0, true, body);
    }

    static ServerPlayer player(GameTestHelper h, String name) {
        ServerPlayer p = LayerGameTests.player(h, name);
        Vec3 at = h.absoluteVec(new Vec3(1.5, 2, 1.5));
        p.moveTo(at.x, at.y, at.z, 0, 0);
        p.getInventory().clearContent();
        return p;
    }

    static void wearCrate(GameTestHelper h, ServerPlayer p) {
        h.assertTrue(PackSlots.size(p, PackSlots.BACK) == 1, "the player has one Curios back slot, has " + PackSlots.size(p, PackSlots.BACK));
        h.assertTrue(PackSlots.size(p, PackSlots.NECK) == 2, "the player has two Curios neck slots");
        PackSlots.setBack(p, new ItemStack(PackItems.CRATE_BACKPACK.get()));
        PackSync.tick(p);
        h.assertTrue(PackRules.openRows(p) == 2, "a crate backpack opens rows A and B");
    }

    static ItemEntity drop(GameTestHelper h, ServerPlayer p, ItemStack s) {
        ServerLevel level = h.getLevel();
        ItemEntity e = new ItemEntity(level, p.getX(), p.getY(), p.getZ(), s);
        e.setNoPickUpDelay();
        level.addFreshEntity(e);
        return e;
    }

    /** {@code pack/rows_never_fill}: 500 pickups with a crate backpack never land in row C. */
    static void rowsNeverFill(GameTestHelper h) {
        ServerPlayer p = player(h, "filler");
        wearCrate(h, p);
        ItemStack[] kinds = {new ItemStack(FishingRegistry.ROD.get()), new ItemStack(FishingRegistry.WORM.get(), 3),
                new ItemStack(FireRegistry.MATCH.get(), 2), new ItemStack(FireRegistry.BOW_DRILL.get()),
                new ItemStack(Items.STICK, 2), new ItemStack(FishingRegistry.ROD.get())};
        int taken = 0;
        for (int i = 0; i < 500; i++) {
            ItemStack s = kinds[i % kinds.length].copy();
            if (i % 7 == 0) s.set(DataComponents.CUSTOM_NAME, Component.literal("n" + i));
            ItemEntity e = drop(h, p, s);
            Pickup.Result r = Pickup.pickUp(p, e);
            if (r == Pickup.Result.TAKEN || r == Pickup.Result.PARTIAL) taken++;
            e.discard();
            if (i % 25 == 0) PackSync.tick(p);
            for (int slot = 27; slot < 36; slot++) {
                if (!p.getInventory().getItem(slot).isEmpty()) h.fail("row C slot " + slot + " filled at pickup " + i);
            }
        }
        // Vanilla's own add path obeys the rows too.
        h.assertFalse(p.getInventory().add(new ItemStack(Items.DIAMOND_SWORD)) && !p.getInventory().getItem(27).isEmpty(), "vanilla add into row C");
        for (int slot = 27; slot < 36; slot++) h.assertTrue(p.getInventory().getItem(slot).isEmpty(), "row C stays empty");
        h.assertTrue(taken > 18, "some pickups landed: " + taken);
        h.succeed();
    }

    /** {@code pack/takeoff_roundtrip}: 100 off-and-on cycles keep every stack and component. */
    static void takeoffRoundtrip(GameTestHelper h) {
        ServerPlayer p = player(h, "packer");
        wearCrate(h, p);
        ItemStack rod = new ItemStack(FishingRegistry.ROD.get());
        rod.setDamageValue(17);
        ItemStack fish = new ItemStack(FishingRegistry.ARCTIC_GRAYLING.get());
        fish.set(YoiComponents.WEIGHT_G.get(), 612);
        ItemStack named = new ItemStack(FishingRegistry.WORM.get(), 7);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("bait tin"));
        p.getInventory().setItem(9, rod);
        p.getInventory().setItem(17, fish);
        p.getInventory().setItem(20, named);
        p.getInventory().setItem(3, new ItemStack(FireRegistry.MATCH.get(), 5));
        PackSync.refresh(p);
        List<ItemStack> before = rows(p);
        for (int c = 0; c < 100; c++) {
            ItemStack pack = PackSlots.back(p);
            PackSlots.setBack(p, ItemStack.EMPTY);
            PackSync.tick(p);
            for (int slot = 9; slot < 36; slot++) {
                if (!p.getInventory().getItem(slot).isEmpty()) h.fail("cycle " + c + ": slot " + slot + " still full with the pack off");
            }
            h.assertTrue(LoadCalculator.stackKg(pack, true) > LoadCalculator.stackKg(pack, false), "the pack carries its contents");
            PackSlots.setBack(p, pack);
            PackSync.tick(p);
            List<ItemStack> after = rows(p);
            for (int i = 0; i < before.size(); i++) {
                if (!ItemStack.matches(before.get(i), after.get(i))) h.fail("cycle " + c + ": slot " + (9 + i) + " was " + before.get(i) + ", now " + after.get(i));
            }
        }
        h.assertTrue(p.getInventory().getItem(3).getCount() == 5, "the belt is untouched");
        h.succeed();
    }

    static List<ItemStack> rows(ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        for (int slot = 9; slot < 36; slot++) out.add(p.getInventory().getItem(slot).copy());
        return out;
    }

    /** {@code pack/no_walkover_pickup}: walking across 20 dropped items picks none up; with auto-pickup on, only tagged items. */
    static void noWalkoverPickup(GameTestHelper h) {
        ServerPlayer p = player(h, "walker");
        List<ItemEntity> items = new ArrayList<>();
        for (int i = 0; i < 20; i++) items.add(drop(h, p, new ItemStack(i % 2 == 0 ? Items.FLINT : FishingRegistry.WORM.get())));
        for (ItemEntity e : items) e.playerTouch(p);
        h.assertTrue(p.getInventory().isEmpty(), "walking over items picks none up");
        h.assertTrue(items.stream().allMatch(ItemEntity::isAlive), "all 20 still on the ground");
        PackAttachments.setAutoPickup(p, true);
        for (ItemEntity e : items) e.playerTouch(p);
        h.assertTrue(p.getInventory().countItem(Items.FLINT) == 10, "auto-pickup takes the 10 tagged flints");
        h.assertTrue(p.getInventory().countItem(FishingRegistry.WORM.get()) == 0, "and none of the untagged worms");
        // The use key picks up anything.
        Pickup.Result r = Pickup.pickUp(p, items.get(1));
        h.assertTrue(r == Pickup.Result.TAKEN && p.getInventory().countItem(FishingRegistry.WORM.get()) == 1, "use picks up: " + r);
        items.forEach(ItemEntity::discard);
        h.succeed();
    }

    /** Puts a single stack weighing {@code kg} on the belt and returns the player's speed factor. */
    static double speedAt(ServerPlayer p, double kg) {
        p.getInventory().clearContent();
        ItemStack load = new ItemStack(FishingRegistry.WORM.get());
        load.set(YoiComponents.WEIGHT_G.get(), (int) Math.round(kg * 1000));
        p.getInventory().setItem(0, load);
        PackSync.refresh(p);
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        return speed.getValue() / speed.getBaseValue();
    }

    /** The four load bands within 2% of the table (Blueprint › Inventory by weight). */
    static void loadBands(GameTestHelper h) {
        ServerPlayer p = player(h, "loaded");
        LoadBands bands = PackConfig.bands();
        double strength = com.yearofice.api.body.BodyService.get().strength(p);
        double m = bands.multiplier(strength);
        double[] kgs = {10 * m, 22 * m, 37 * m, 50 * m};
        double[] want = {1.0, 0.85, 0.65, 0.30};
        for (int i = 0; i < 4; i++) {
            double got = speedAt(p, kgs[i]);
            h.assertTrue(Math.abs(got - want[i]) <= 0.02 * want[i], String.format("%.1f kg: speed x%.3f, want x%.2f", kgs[i], got, want[i]));
            h.assertTrue(PackSync.band(p) == LoadBands.Band.values()[i], "band " + PackSync.band(p));
        }
        h.assertTrue(Math.abs(LoadCalculator.kgOf(p) - 50 * m) < 0.01, "kg read back");
        p.setSprinting(true);
        PackSync.tick(p);
        h.assertFalse(p.isSprinting(), "no running in the 30-45 kg band and above");
        h.succeed();
    }

    /** {@code pack/strength_scales_bands} (video 2, A11): ×0.8 at Strength 0 to ×1.15 at Strength 100, linear. */
    static void strengthScalesBands(GameTestHelper h) {
        ServerPlayer p = player(h, "lifter");
        LoadBands bands = PackConfig.bands();
        h.assertTrue(Math.abs(bands.multiplier(0) - 0.8) < 1e-9 && Math.abs(bands.multiplier(100) - 1.15) < 1e-9
                && Math.abs(bands.multiplier(50) - 0.975) < 1e-9, "the multiplier's ends and middle");
        BodyState s = BodyAttachments.of(p);
        s.kg = Metabolism.kgForBmi(24, s.heightCm);
        double strong = com.yearofice.api.body.BodyService.get().strength(p);
        h.assertTrue(strong > 99, "a fed body is strong: " + strong);
        h.assertTrue(Math.abs(speedAt(p, 16) - 1.0) < 1e-6, "16 kg is light at Strength 100 (edge 17.25 kg)");
        s.kg = Metabolism.kgForBmi(17, s.heightCm);
        double weak = com.yearofice.api.body.BodyService.get().strength(p);
        h.assertTrue(weak < 1, "BMI 17 leaves no strength: " + weak);
        h.assertTrue(Math.abs(speedAt(p, 13) - 0.85) < 0.02 * 0.85, "13 kg is heavy at Strength 0 (edge 12 kg)");
        h.assertTrue(Math.abs(speedAt(p, 37) - 0.30) < 0.02 * 0.30, "37 kg drags at Strength 0 (edge 36 kg)");
        h.succeed();
    }

    /** {@code pack/screen_shows_band}: the readout the server syncs holds Strength-scaled edges and the cap. */
    static void screenShowsBand(GameTestHelper h) {
        ServerPlayer p = player(h, "reader");
        BodyState s = BodyAttachments.of(p);
        s.kg = Metabolism.kgForBmi(17, s.heightCm);
        PackSkill.setReps(p, 0);
        wearCrate(h, p);
        PackSync.refresh(p);
        var weak = PackSync.readout(p);
        h.assertTrue(weak != null, "a readout was built");
        h.assertTrue(Math.abs(weak.edge0() - 12) < 0.01 && Math.abs(weak.edge1() - 24) < 0.01 && Math.abs(weak.edge2() - 36) < 0.01,
                "edges 12/24/36 kg at Strength 0: " + weak.edge0() + "/" + weak.edge1() + "/" + weak.edge2());
        h.assertTrue(weak.capKg() > 0, "the pack's cap is stated: " + weak.capKg());
        s.kg = Metabolism.kgForBmi(24, s.heightCm);
        PackSkill.setReps(p, 300);
        PackSync.refresh(p);
        var strong = PackSync.readout(p);
        h.assertTrue(strong.strength() > 99, "Strength synced: " + strong.strength());
        h.assertTrue(Math.abs(strong.edge0() - 15 * 1.15 * 1.1) < 0.01, "edge 0 at Strength 100 and full practice: " + strong.edge0());
        h.assertTrue(strong.edge2() > weak.edge2(), "Strength lifts the edges");
        h.succeed();
    }

    /** {@code pack/hauling_practice} (A11): 300 reps lift the band edges 10%; carrying 15 kg+ on foot earns reps. */
    static void haulingPractice(GameTestHelper h) {
        ServerPlayer p = player(h, "hauler");
        BodyState s = BodyAttachments.of(p);
        s.kg = Metabolism.kgForBmi(17, s.heightCm);
        PackSkill.setReps(p, 0);
        h.assertTrue(Math.abs(speedAt(p, 13) - 0.85) < 0.02 * 0.85, "13 kg is heavy at Strength 0 and no practice (edge 12 kg)");
        PackSkill.setReps(p, 300);
        h.assertTrue(Math.abs(speedAt(p, 13) - 1.0) < 1e-6, "13 kg is light at full practice (edge 13.2 kg)");
        PackSkill.setReps(p, 150);
        h.assertTrue(Math.abs(speedAt(p, 12.4) - 1.0) < 1e-6 && Math.abs(speedAt(p, 12.7) - 0.85) < 0.02 * 0.85, "half practice moves the edge to 12.6 kg");
        PackSkill.setReps(p, 0);
        speedAt(p, 20);
        p.setOnGround(true);
        p.setDeltaMovement(0.2, 0, 0);
        int need = (int) PackConfig.get("hauling_rep_ticks");
        for (int i = 0; i < need; i++) PackSkill.tick(p);
        h.assertTrue(PackSkill.reps(p) == 1, "walking under 20 kg for one interval earns one rep: " + PackSkill.reps(p));
        speedAt(p, 5);
        for (int i = 0; i < need; i++) PackSkill.tick(p);
        h.assertTrue(PackSkill.reps(p) == 1, "5 kg earns nothing");
        h.succeed();
    }

    /** {@code pack/pack_min_strength} (A11): a frame pack worn below its minimum Strength holds only 30 kg. */
    static void packMinStrength(GameTestHelper h) {
        ServerPlayer p = player(h, "frame");
        BodyState s = BodyAttachments.of(p);
        PackSkill.setReps(p, 0);
        s.kg = Metabolism.kgForBmi(17, s.heightCm);
        PackSlots.setBack(p, new ItemStack(PackItems.LARGE_PACK_FRAME.get()));
        PackSync.tick(p);
        h.assertTrue(LoadCalculator.minStrengthOf(p) == 60, "large frame needs Strength 60");
        h.assertTrue(LoadCalculator.maxKgOf(p) == 30, "weak: 30 kg limit, got " + LoadCalculator.maxKgOf(p));
        h.assertTrue(LoadCalculator.bandOf(p, 31) == LoadBands.Band.OVERLOADED, "weak: over 30 kg is Overloaded");
        s.kg = Metabolism.kgForBmi(24, s.heightCm);
        h.assertTrue(LoadCalculator.maxKgOf(p) == 55, "strong: 55 kg limit, got " + LoadCalculator.maxKgOf(p));
        h.assertTrue(LoadCalculator.bandOf(p, 31) != LoadBands.Band.OVERLOADED, "strong: 31 kg is not Overloaded");
        PackSlots.setBack(p, new ItemStack(PackItems.PACK_FRAME.get()));
        PackSync.tick(p);
        h.assertTrue(LoadCalculator.minStrengthOf(p) == 40 && LoadCalculator.maxKgOf(p) == 45, "frame pack: 45 kg at Strength 40+");
        h.succeed();
    }

    /** {@code layers/item_expiry}: a dropped knife is there after 6 game days and gone after 8; a keepsake stays after 30. */
    static void itemExpiry(GameTestHelper h) {
        ServerPlayer p = player(h, "dropper");
        ItemEntity knife = drop(h, p, new ItemStack(FishingRegistry.ROD.get()));
        ItemEntity keep = drop(h, p, new ItemStack(MessengerItems.MESSENGER.get()));
        long now = com.yearofice.api.clock.ClockService.get().nowMinute();
        h.assertTrue(knife.getAge() < 0, "vanilla's 5-minute despawn is off");
        ItemExpiry.setStamp(knife, now - 6 * 1440);
        ItemExpiry.setStamp(keep, now - 30 * 1440);
        ItemExpiry.sweep(h.getLevel().getServer());
        h.assertTrue(knife.isAlive(), "after 6 game days it is still there");
        ItemExpiry.setStamp(knife, now - 8 * 1440);
        ItemExpiry.sweep(h.getLevel().getServer());
        h.assertFalse(knife.isAlive(), "after 8 game days it is gone");
        h.assertTrue(keep.isAlive(), "the messenger never expires");
        keep.discard();
        h.succeed();
    }
}
