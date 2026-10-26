package com.yearofice.story.ambition;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.event.TrophyHungEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.story.Journal;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Wires the calendar-driven ambitions and the ambition page ({@code /yoi ambitions}). */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class AmbitionEvents {
    private AmbitionEvents() {}

    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && ClockService.installed()) Ambitions.refresh(p, ClockService.get().absDay());
    }

    @SubscribeEvent
    public static void day(ClockEvent.Day e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            long min = e.absDay() * 1440L;
            int doy = com.yearofice.sim.time.Calendar.dayOfYear(e.absDay());
            int month = com.yearofice.sim.time.Calendar.month(doy);
            Ambitions.refresh(p, e.absDay());
            // "survive_minus_40": the day that ended had a low of -40 C or colder at the player, and the player is here to see the next
            try {
                var d = com.yearofice.api.env.EnvService.get().day(p.serverLevel(), e.absDay() - 1);
                if (d != null && d.meanC() - d.swingC() / 2.0 <= -40) Ambitions.fire(p, "body.survived_minus_40", min);
            } catch (RuntimeException ignored) { }
            // "see_the_thaw": the river's breakup (the season core's last ice event for the river class)
            try {
                var core = com.yearofice.world.season.SeasonState.get(p.serverLevel().getServer().overworld()).core;
                if (core.lastEvent[com.yearofice.sim.world.IceModel.BodyClass.RIVER.ordinal()] == com.yearofice.sim.world.IceModel.Event.BREAKUP)
                    Ambitions.fire(p, "season.first_breakup", min);
            } catch (RuntimeException ignored) { }
            if (e.absDay() == 365) {
                Ambitions.fire(p, "calendar.year2_may1", min);
                if (Journal.flights(p) == 0) Ambitions.fire(p, "calendar.year2_may1_no_sos", min);
            }
            // body.bmi20_30_days
            try {
                if (com.yearofice.api.body.BodyService.get().readout(p).bmi() >= 20) {
                    if (com.yearofice.api.story.AmbitionTracker.tally(p, "body.bmi20_streak", 1) >= 30) {
                        Ambitions.fire(p, "body.bmi20_30_days", min);
                    }
                } else {
                    com.yearofice.api.story.AmbitionTracker.tally(p, "body.bmi20_streak", -com.yearofice.api.story.AmbitionTracker.tally(p, "body.bmi20_streak"));
                }
            } catch (RuntimeException ignored) { }
            // body.no_frostbite_to_april & build.no_animal_entry
            if (doy == com.yearofice.sim.time.Calendar.dayOfYear(11, 1)) {
                com.yearofice.api.story.AmbitionTracker.tally(p, "body.frostbitten_winter", -com.yearofice.api.story.AmbitionTracker.tally(p, "body.frostbitten_winter"));
                com.yearofice.api.story.AmbitionTracker.tally(p, "build.animal_entered_winter", -com.yearofice.api.story.AmbitionTracker.tally(p, "build.animal_entered_winter"));
            }
            if (doy == com.yearofice.sim.time.Calendar.dayOfYear(4, 1) && e.absDay() >= 300) {
                if (com.yearofice.api.story.AmbitionTracker.tally(p, "body.frostbitten_winter") == 0) {
                    Ambitions.fire(p, "body.no_frostbite_to_april", min);
                }
                if (com.yearofice.api.story.AmbitionTracker.tally(p, "build.animal_entered_winter") == 0) {
                    Ambitions.fire(p, "build.no_animal_entry", min);
                }
            }
            // spirits.steady_dec_jan
            if (doy == com.yearofice.sim.time.Calendar.dayOfYear(12, 1)) {
                com.yearofice.api.story.AmbitionTracker.tally(p, "spirits.steady_days", -com.yearofice.api.story.AmbitionTracker.tally(p, "spirits.steady_days"));
            }
            if (month == 12 || month == 1) {
                try {
                    var r = com.yearofice.api.body.BodyService.get().readout(p);
                    if (r.condition() >= 60 && r.warmth() >= 45) {
                        if (com.yearofice.api.story.AmbitionTracker.tally(p, "spirits.steady_days", 1) >= 60) {
                            Ambitions.fire(p, "spirits.steady_dec_jan", min);
                        }
                    }
                } catch (RuntimeException ignored) { }
            }
            // livestock.winter_survived
            if (doy == com.yearofice.sim.time.Calendar.dayOfYear(4, 30) && e.absDay() >= 300) {
                var box = new net.minecraft.world.phys.AABB(p.blockPosition()).inflate(64);
                boolean goat = !p.serverLevel().getEntitiesOfClass(com.yearofice.ecosystem.livestock.GoatEntity.class, box, net.minecraft.world.entity.LivingEntity::isAlive).isEmpty();
                boolean hen = !p.serverLevel().getEntitiesOfClass(com.yearofice.ecosystem.livestock.HenEntity.class, box, net.minecraft.world.entity.LivingEntity::isAlive).isEmpty();
                if (goat && hen) Ambitions.fire(p, "livestock.winter_survived", min);
            }
            // fire.solstice_kept, build.cellar_100kg_roots, food.cache_30_days, food.dried_fish_10kg
            checkHomesteadStores(p, min, doy);
        }
    }

    private static void checkHomesteadStores(ServerPlayer p, long min, int doy) {
        var sl = p.serverLevel();
        var center = p.blockPosition();
        double totalKcal = 0, driedFishG = 0, cellarRootG = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            var st = p.getInventory().getItem(i);
            if (st.isEmpty()) continue;
            totalKcal += stackKcal(st);
            if (isDriedFish(st)) driedFishG += com.yearofice.food.Meals.grams(st) * st.getCount();
        }
        boolean solsticeLit = false;
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(center.offset(-16, -6, -16), center.offset(16, 6, 16))) {
            if (!sl.isLoaded(pos)) continue;
            var be = sl.getBlockEntity(pos);
            if (be instanceof com.yearofice.food.preserve.StoreBlockEntity sbe) {
                var cellar = com.yearofice.build.cellar.CellarIndex.at(sl, pos);
                boolean unfrozenCellar = cellar != null && cellar.tempC() > -0.5;
                for (var st : sbe.items()) {
                    totalKcal += stackKcal(st);
                    if (isDriedFish(st)) driedFishG += com.yearofice.food.Meals.grams(st) * st.getCount();
                    if (unfrozenCellar && (st.is(com.yearofice.crop.CropRegistry.POTATO.get())
                            || st.is(com.yearofice.crop.CropRegistry.TURNIP.get())
                            || st.is(com.yearofice.crop.CropRegistry.SEED_POTATO.get()))) {
                        cellarRootG += com.yearofice.food.Meals.grams(st) * st.getCount();
                    }
                }
            } else if (be instanceof com.yearofice.fire.FireRingBlockEntity fbe) {
                if (fbe.fire().state().burning() || fbe.fire().state() == com.yearofice.sim.fire.FireState.COALS) {
                    solsticeLit = true;
                }
            }
        }
        if (totalKcal >= 30 * 2500) Ambitions.fire(p, "food.cache_30_days", min);
        if (driedFishG >= 10_000) Ambitions.fire(p, "food.dried_fish_10kg", min);
        if (cellarRootG >= 100_000 && (doy >= com.yearofice.sim.time.Calendar.dayOfYear(11, 1) || doy <= com.yearofice.sim.time.Calendar.dayOfYear(3, 31))) {
            Ambitions.fire(p, "build.cellar_100kg_roots", min);
        }
        if (solsticeLit && (doy == com.yearofice.sim.time.Calendar.dayOfYear(12, 21) || doy == com.yearofice.sim.time.Calendar.dayOfYear(12, 22))) {
            Ambitions.fire(p, "fire.solstice_kept", min);
        }
    }

    private static double stackKcal(net.minecraft.world.item.ItemStack st) {
        var nut = st.get(com.yearofice.item.component.YoiComponents.NUTRIENTS.get());
        if (nut != null) return nut.kcal() * st.getCount();
        var row = com.yearofice.body.intake.FoodMap.row(st);
        if (row == null) return 0;
        double g = com.yearofice.food.Meals.grams(st);
        return row.values("raw").kcal(g) * st.getCount();
    }

    private static boolean isDriedFish(net.minecraft.world.item.ItemStack st) {
        if (st.is(com.yearofice.food.FoodItems.DRIED_FISH.get())) return true;
        var f = st.get(com.yearofice.item.component.YoiComponents.FRESH.get());
        return f != null && (f.state() == com.yearofice.sim.decay.FoodState.DRIED || f.state() == com.yearofice.sim.decay.FoodState.SMOKED)
                && st.has(com.yearofice.item.component.YoiComponents.WEIGHT_G.get());
    }

    @SubscribeEvent
    public static void hung(TrophyHungEvent e) { Ambitions.fire(e.player(), "trophy.hung", ClockService.installed() ? ClockService.get().nowMinute() : 0); }

    @SubscribeEvent
    public static void crafted(com.yearofice.api.build.StationCraftEvent e) {
        if (e.failed()) return;
        long m = ClockService.installed() ? ClockService.get().nowMinute() : 0;
        if (e.station().getPath().equals("forge")) Ambitions.fire(e.player(), "craft.forged_iron", m);
        if (e.recipe().getPath().contains("fur_mittens")) Ambitions.fire(e.player(), "craft.fur_mittens", m);
        String made = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.made().getItem()).getPath();
        if (made.equals("pemmican")) {
            double kg = com.yearofice.api.story.AmbitionTracker.tally(e.player(), "food.pemmican_kg", com.yearofice.item.inventory.LoadCalculator.stackKg(e.made(), true));
            if (kg >= 5) Ambitions.fire(e.player(), "food.pemmican_5kg", m);
        }
        String dishId = e.made().get(com.yearofice.item.component.YoiComponents.DISH_ID.get());
        if ((made.contains("bread") || (dishId != null && dishId.contains("bread")))
                && com.yearofice.api.story.AmbitionTracker.tally(e.player(), "crop.grew_grain") > 0) {
            Ambitions.fire(e.player(), "food.bread_from_own_wheat", m);
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("ambitions")
                .executes(c -> {
                    var lines = Ambitions.page(c.getSource().getPlayerOrException());
                    lines.forEach(l -> c.getSource().sendSuccess(() -> Component.literal(l), false));
                    return lines.size();
                })));
    }
}
