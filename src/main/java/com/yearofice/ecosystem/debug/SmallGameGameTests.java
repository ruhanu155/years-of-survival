package com.yearofice.ecosystem.debug;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.carcass.Butchering;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.carcass.WholeGameItem;
import com.yearofice.ecosystem.species.SmallGameEntity;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.ecosystem.species.bird.GrouseEntity;
import com.yearofice.ecosystem.spawn.SlotSpawner;
import com.yearofice.ecosystem.theft.TheftService;
import com.yearofice.sim.ecosystem.SeasonalLooks;
import com.yearofice.sim.ecosystem.Theft;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m5a, ecosystem: the small game, furbearers and grouse roster of M5 lane A (Land animals in depth › Mob sheets:
 * small game and furbearers; Birds and the air › Grouse and ptarmigan). Ids follow the Blueprint's:
 * {@code eco/small_game_spawns_by_data}, {@code data/data_only_species_spawns}, {@code eco/carcass_on_death},
 * {@code eco/seasonal_coat}, {@code eco/theft_toggle}.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class SmallGameGameTests {
    private SmallGameGameTests() {}

    static final String TEMPLATE = EcoGameTests.TEMPLATE;
    static final BlockPos AT = EcoGameTests.AT;

    /** The twelve species the roster adds, by id; the first nine are small-game bodies, the last three grouse. */
    static final List<String> ROSTER = List.of("red_squirrel", "marten", "ermine", "mink", "muskrat", "beaver", "porcupine",
            "wood_frog", "yellowjacket", "spruce_grouse", "ruffed_grouse", "rock_ptarmigan");

    @GameTestGenerator
    public static Collection<TestFunction> smallGameTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:eco/small_game_spawns_by_data", 100, SmallGameGameTests::spawnsByData));
        out.add(test("yearofice:data/data_only_species_spawns", 200, SmallGameGameTests::dataOnly));
        out.add(test("yearofice:eco/carcass_on_death", 100, SmallGameGameTests::carcassOnDeath));
        out.add(test("yearofice:eco/seasonal_coat", 60, SmallGameGameTests::seasonalCoat));
        out.add(test("yearofice:eco/theft_toggle", 60, SmallGameGameTests::theftToggle));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m5a", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static YoiAnimal spawn(GameTestHelper h, String species, int slot) {
        BlockPos at = h.absolutePos(AT.offset(slot % 3 - 1, 0, slot / 3 % 3 - 1));
        YoiAnimal a = SlotSpawner.spawnSpecies(h.getLevel(), "yearofice:" + species, at, "debug:test-" + species);
        h.assertTrue(a != null, species + " did not spawn");
        a.setScentEnabled(false);
        SlotSpawner.untrack("debug:test-" + species);
        return a;
    }

    /** Each rostered species has a file with a spawn block and the spawner places its body, typed by the registry. */
    static void spawnsByData(GameTestHelper h) {
        List<YoiAnimal> bodies = new ArrayList<>();
        for (int i = 0; i < ROSTER.size(); i++) {
            String sp = ROSTER.get(i);
            SpeciesDef d = SpeciesBook.get("yearofice:" + sp);
            h.assertTrue(SpeciesBook.has("yearofice:" + sp) && d.spawns(), sp + " has no species file with a spawn block");
            h.assertTrue(!d.habitat().isEmpty() && d.spawn().densityPerKm2() > 0, sp + " has no habitat or density");
            YoiAnimal a = spawn(h, sp, i);
            h.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(a.getType()).getPath().equals(sp), sp + " is a " + a.getType());
            h.assertTrue(a.speciesId().equals("yearofice:" + sp), sp + " reports " + a.speciesId());
            boolean grouse = sp.endsWith("grouse") || sp.endsWith("ptarmigan");
            h.assertTrue(grouse ? a instanceof GrouseEntity : a instanceof SmallGameEntity, sp + " is on the wrong rig class " + a.getClass().getSimpleName());
            bodies.add(a);
        }
        h.runAfterDelay(40, () -> {
            for (YoiAnimal a : bodies) {
                h.assertTrue(a.isAlive(), a.speciesPath() + " is gone (" + a.getRemovalReason() + ")");
                a.discard();
            }
            h.succeed();
        });
    }

    /**
     * A species defined only in {@code data/yearofice/species/data_only_probe.json}, with no registry entry of its own,
     * spawns through the generic body, wanders, and on a player's hit leaves a carcass of its own species whose
     * dressing follows its own butchering table.
     */
    static void dataOnly(GameTestHelper h) {
        final String id = "yearofice:data_only_probe";
        SpeciesDef d = SpeciesBook.get(id);
        h.assertTrue(SpeciesBook.has(id) && d.spawns() && d.entityType().equals("yearofice:critter"), "the probe's file: " + d.entityType());
        h.assertTrue(!BuiltInRegistries.ENTITY_TYPE.containsKey(net.minecraft.resources.ResourceLocation.parse(id)), "the probe must have no body of its own");
        YoiAnimal a = spawn(h, "data_only_probe", 4);
        h.assertTrue(a.getType() == EcosystemRegistry.CRITTER.get() && a.speciesId().equals(id), "body " + a.getType() + " species " + a.speciesId());
        h.assertTrue(a.species().walkMps() > 0 && a.species().wholeKg() == 0.2, "numbers read from the file");
        ServerPlayer p = EcoGameTests.player(h);
        p.moveTo(a.getX() + 1, a.getY(), a.getZ(), 0, 0);
        h.runAfterDelay(30, () -> {
            h.assertTrue(a.isAlive(), "the probe is gone");
            h.assertTrue(a.hurt(h.getLevel().damageSources().playerAttack(p), 1f), "the hit did not take");
            h.runAfterDelay(5, () -> {
                AABB box = new AABB(a.position(), a.position()).inflate(6);
                List<CarcassEntity> cs = h.getLevel().getEntitiesOfClass(CarcassEntity.class, box);
                h.assertTrue(cs.size() == 1 && cs.get(0).record().species().equals(id), "carcasses " + cs.size());
                List<ItemStack> parts = Butchering.parts(h.getLevel(), cs.get(0).record());
                h.assertTrue(parts.size() == 1 && parts.get(0).is(EcosystemRegistry.SNOWSHOE_HARE_MEAT.get()), "parts " + parts);
                cs.get(0).discard();
                h.succeed();
            });
        });
    }

    /** Every death makes a carcass of its size class, with its record, no vanilla loot, and dressing leaves its parts and guts. */
    static void carcassOnDeath(GameTestHelper h) {
        ServerPlayer p = EcoGameTests.player(h);
        ServerLevel l = h.getLevel();
        // The template has no floor, and a gut pile needs ground: lay it under the dressing spot for the test.
        var floor = net.minecraft.world.level.block.Blocks.STONE;
        BlockPos floorAt = AT.below();
        var oldFloor = h.getBlockState(floorAt);
        h.setBlock(floorAt, floor);
        record Case(String sp, String cls, net.minecraft.world.entity.EntityType<CarcassEntity> type) {}
        List<Case> cases = List.of(
                new Case("red_squirrel", "tiny", EcosystemRegistry.CARCASS_TINY.get()),
                new Case("mink", "small", EcosystemRegistry.CARCASS_SMALL.get()),
                new Case("spruce_grouse", "small", EcosystemRegistry.CARCASS_SMALL.get()),
                new Case("beaver", "medium", EcosystemRegistry.CARCASS_MEDIUM.get()),
                new Case("porcupine", "medium", EcosystemRegistry.CARCASS_MEDIUM.get()));
        List<YoiAnimal> bodies = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            YoiAnimal a = spawn(h, cases.get(i).sp(), i);
            p.moveTo(a.getX() + 0.5, a.getY(), a.getZ(), 0, 0);
            h.assertTrue(a.hurt(l.damageSources().playerAttack(p), 1f), cases.get(i).sp() + ": the hit did not take");
            bodies.add(a);
        }
        // A yellowjacket is not game: no class, no carcass.
        YoiAnimal wasp = spawn(h, "yellowjacket", 5);
        h.assertTrue(wasp.hurt(l.damageSources().playerAttack(p), 1f) || !wasp.huntableNow(), "yellowjacket hit");
        h.runAfterDelay(5, () -> {
            for (int i = 0; i < cases.size(); i++) {
                Case c = cases.get(i);
                YoiAnimal a = bodies.get(i);
                AABB box = a.getBoundingBox().inflate(4);
                h.assertTrue(a.isRemoved(), c.sp() + ": the living animal should be gone");
                CarcassEntity found = null;
                for (CarcassEntity e : l.getEntitiesOfClass(CarcassEntity.class, box)) if (e.record().species().equals("yearofice:" + c.sp())) found = e;
                h.assertTrue(found != null, c.sp() + ": no carcass");
                h.assertTrue(found.getType() == c.type(), c.sp() + ": a " + found.getType() + " instead of the " + c.cls() + " carcass");
                SpeciesDef d = SpeciesBook.get("yearofice:" + c.sp());
                h.assertTrue(d.carcassClass().equals(c.cls()), c.sp() + ": class " + d.carcassClass());
                double kg = found.record().weightG() / 1000.0;
                h.assertTrue(kg >= d.wholeKg() * 0.9 && kg <= d.wholeKg() * 1.1, c.sp() + ": weight " + kg);
                h.assertTrue(l.getEntitiesOfClass(ItemEntity.class, box).isEmpty() && l.getEntitiesOfClass(ExperienceOrb.class, box).isEmpty(),
                        c.sp() + ": vanilla loot or experience");
            }
            h.assertTrue(l.getEntitiesOfClass(CarcassEntity.class, wasp.getBoundingBox().inflate(0.5)).isEmpty() || wasp.isAlive(), "a yellowjacket left a carcass");
            // Dress the beaver: 6.5 kg of meat in 100 g units and its pelt, then a gut pile where the work was done.
            CarcassEntity beaver = null;
            for (CarcassEntity e : l.getEntitiesOfClass(CarcassEntity.class, new AABB(h.absolutePos(AT)).inflate(6))) if (e.record().species().equals("yearofice:beaver")) beaver = e;
            h.assertTrue(beaver != null, "no beaver carcass");
            CarcassRecord r = beaver.record();
            int meat = 0;
            boolean pelt = false;
            for (ItemStack s : Butchering.parts(l, r)) {
                if (s.is(EcosystemRegistry.BEAVER_MEAT.get())) meat += s.getCount();
                if (s.is(EcosystemRegistry.BEAVER_PELT.get())) pelt = true;
            }
            h.assertTrue(meat == 65 && pelt, "beaver parts: meat " + meat + " pelt " + pelt);
            BlockPos feet = h.absolutePos(AT.offset(0, 0, 0));
            p.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0, 0);
            Butchering.finish(p, WholeGameItem.of(r));
            boolean pile = false;
            for (BlockPos q : BlockPos.betweenClosed(feet.offset(-1, -1, -1), feet.offset(1, 1, 1))) if (l.getBlockState(q).is(EcosystemRegistry.GUT_PILE.get())) pile = true;
            h.assertTrue(pile, "dressing the beaver left no gut pile");
            for (CarcassEntity e : l.getEntitiesOfClass(CarcassEntity.class, new AABB(h.absolutePos(AT)).inflate(6))) e.discard();
            h.setBlock(floorAt, oldFloor.getBlock());
            for (BlockPos q : BlockPos.betweenClosed(feet.offset(-1, -1, -1), feet.offset(1, 1, 1))) if (l.getBlockState(q).is(EcosystemRegistry.GUT_PILE.get())) l.removeBlock(q, false);
            h.succeed();
        });
    }

    /** Hare, ermine and rock ptarmigan turn white by date; the grouse and the squirrel keep one coat all year. */
    static void seasonalCoat(GameTestHelper h) {
        int jan15 = Calendar.dayOfYear(1, 15), jul15 = Calendar.dayOfYear(7, 15), oct10 = Calendar.dayOfYear(10, 10), apr20 = Calendar.dayOfYear(4, 20);
        YoiAnimal hare = spawn(h, "snowshoe_hare", 0), ermine = spawn(h, "ermine", 1), rock = spawn(h, "rock_ptarmigan", 2),
                spruce = spawn(h, "spruce_grouse", 3), squirrel = spawn(h, "red_squirrel", 4);
        int w = SeasonalLooks.Coat.WINTER.ordinal(), s = SeasonalLooks.Coat.SUMMER.ordinal(), m = SeasonalLooks.Coat.MOTTLED.ordinal(),
                mo = SeasonalLooks.Coat.MOULT.ordinal();
        hare.refreshLooks(jan15);
        ermine.refreshLooks(jan15);
        rock.refreshLooks(jan15);
        h.assertTrue(hare.coat() == w && ermine.coat() == w && rock.coat() == w, "January: hare " + hare.coat() + " ermine " + ermine.coat() + " rock " + rock.coat());
        hare.refreshLooks(jul15);
        ermine.refreshLooks(jul15);
        rock.refreshLooks(jul15);
        h.assertTrue(hare.coat() == s && ermine.coat() == s && rock.coat() == s, "July: hare " + hare.coat() + " ermine " + ermine.coat() + " rock " + rock.coat());
        ermine.refreshLooks(oct10);
        rock.refreshLooks(oct10);
        h.assertTrue(ermine.coat() == m, "ermine on Oct 10 " + ermine.coat());
        h.assertTrue(rock.coat() == mo, "rock ptarmigan on Oct 10 " + rock.coat());
        ermine.refreshLooks(apr20);
        h.assertTrue(ermine.coat() == m, "ermine on Apr 20 " + ermine.coat());
        for (int doy : new int[] {jan15, jul15, oct10, apr20}) {
            spruce.refreshLooks(doy);
            squirrel.refreshLooks(doy);
            h.assertTrue(spruce.coat() == s && squirrel.coat() == s, "the grouse and the squirrel keep one coat, day " + doy);
        }
        // The pure dates: ermine white Oct 15 - Apr 15 exactly (Bestiary).
        h.assertTrue(SeasonalLooks.ermine(Calendar.dayOfYear(10, 15)) == SeasonalLooks.Coat.WINTER && SeasonalLooks.ermine(Calendar.dayOfYear(4, 15)) == SeasonalLooks.Coat.WINTER
                && SeasonalLooks.ermine(Calendar.dayOfYear(10, 14)) == SeasonalLooks.Coat.MOTTLED && SeasonalLooks.ermine(Calendar.dayOfYear(5, 10)) == SeasonalLooks.Coat.SUMMER, "ermine dates");
        // And from the clock: a ticking body shows the look of today's date.
        h.runAfterDelay(3, () -> {
            int today = Calendar.dayOfYearOfMinute(com.yearofice.ecosystem.clock.SliceClock.minute(h.getLevel()));
            h.assertTrue(hare.coat() == SeasonalLooks.hare(today).ordinal(), "hare from the clock: " + hare.coat() + " on day " + today);
            for (YoiAnimal a : List.of(hare, ermine, rock, spruce, squirrel)) a.discard();
            h.succeed();
        });
    }

    /** The player's {@code rack_thieves} option: off, nothing is ever taken; on, the night thieves take meat from a far store. */
    static void theftToggle(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos at = h.absolutePos(AT);
        Theft.Context far = new Theft.Context(true, false, false, false, false, false, true, 200);
        Theft.Context near = new Theft.Context(true, false, false, false, false, false, true, 10);
        Boolean before = null;
        TheftService.setEnabled(false);
        try {
            SimpleContainer off = stocked();
            int stolenOff = rollMany(l, at, off, far, 3000);
            h.assertTrue(stolenOff == 0 && count(off) == 40, "toggle off: " + stolenOff + " thefts, " + count(off) + " left");
            TheftService.setEnabled(true);
            SimpleContainer nearBox = stocked();
            int stolenNear = rollMany(l, at, nearBox, near, 3000);
            h.assertTrue(stolenNear == 0 && count(nearBox) == 40, "a player within 30 m: " + stolenNear + " thefts");
            SimpleContainer on = stocked();
            int stolenOn = rollMany(l, at, on, far, 3000);
            // Marten 2% + fox 4% a night hour (first to win), so about 5.9% of 3000 hours.
            h.assertTrue(stolenOn > 100 && stolenOn < 300, "toggle on: " + stolenOn + " thefts");
            // The same store and hours reproduce the same thefts (a replayed rack loses what it would have lost live).
            SimpleContainer again = stocked();
            h.assertTrue(rollMany(l, at, again, far, 3000) == stolenOn, "the rolls must be deterministic for a store and its hours");
        } finally {
            TheftService.setEnabled(before);
        }
        h.succeed();
    }

    static SimpleContainer stocked() {
        SimpleContainer c = new SimpleContainer(9);
        c.setItem(0, new ItemStack(EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 40));
        return c;
    }

    static int count(SimpleContainer c) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) n += c.getItem(i).getCount();
        return n;
    }

    /** Rolls {@code hours} hours, topping the stack back up after every theft so the store never empties. */
    static int rollMany(ServerLevel l, BlockPos at, SimpleContainer c, Theft.Context ctx, int hours) {
        int stolen = 0;
        for (int hr = 0; hr < hours; hr++) {
            if (TheftService.hour(l, at, c, hr, ctx).isPresent()) {
                stolen++;
                c.setItem(0, new ItemStack(EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 40));
            }
        }
        return stolen;
    }
}
