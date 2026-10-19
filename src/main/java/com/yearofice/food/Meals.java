package com.yearofice.food;

import com.yearofice.api.body.BodyReadout;
import com.yearofice.api.body.BodyService;
import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.core.YearOfIce;
import com.yearofice.food.cooking.CookingData;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.DecayLadder;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.food.Doneness;
import com.yearofice.sim.food.EatingLadder;
import com.yearofice.sim.food.Meal;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Eating our own foods (Blueprint › Cooking; Decay and time › Eating decayed food): one bite is one item, weighed by
 * the stack's {@code yearofice:weight_g} when it carries one (a whole fish) and otherwise by its {@code yearofice:food}
 * row ({@code weight_g}, default a 100 g unit; Layer 6: the component "overrides the map for one stack"). The bite's
 * stage, state and doneness go through {@link Meal}, then the body books it with {@link BodyService#eat}.
 *
 * <p>What the body cannot take yet is logged, not dropped silently: {@code BodyService} has no hook for gut illness,
 * nausea, parasites or Spirits from food, so the rolled outcome is written to the log and the player hears "That
 * tasted off." Burnt food's 70% of calories is booked by passing 70% of the grams, which also fills the stomach 70%:
 * the body needs a calorie share on {@code eat} to do it properly (listed for Body in the PR).
 */
public final class Meals {
    private Meals() {}

    /** One bite's result, for the caller and the tests. */
    public record Bite(boolean eaten, Meal.Outcome outcome, double grams, double gramsBooked, int stage) {}

    /** Grams one item of this stack weighs: its {@code weight_g} component, else its food row's. */
    public static double grams(ItemStack stack) {
        Integer g = stack.get(YoiComponents.WEIGHT_G.get());
        if (g != null && g > 0) return g;
        FoodMap.Row row = FoodMap.row(stack);
        return row == null ? 0 : row.weightG();
    }

    /** The eating-ladder family: from {@code cooking/foods.json}, else a whole piece is a fish and the rest game meat. */
    static EatingLadder.Family family(ItemStack stack) {
        CookingData.Food f = CookingData.of(stack.getItem());
        if (f != null) return f.family();
        return stack.has(YoiComponents.WEIGHT_G.get()) ? EatingLadder.Family.FISH : EatingLadder.Family.GAME_MEAT;
    }

    /** True while the stack can be bitten into at all (not rotten). Both sides. */
    public static boolean edible(Level level, ItemStack stack) {
        if (FoodMap.row(stack) == null) return false;
        if (!stack.has(YoiComponents.FRESH.get())) return true;
        return DecayService.get().stage(level, stack) < EatingLadder.ROTTEN;
    }

    /** Eats one item of {@code stack}; shrinks it on success. */
    public static Bite eat(ServerPlayer p, ItemStack stack) {
        Level level = p.level();
        FoodMap.Row row = FoodMap.row(stack);
        if (row == null || stack.isEmpty()) return new Bite(false, null, 0, 0, 0);
        DecayService decay = DecayService.get();
        Fresh f = decay.settle(level, stack);
        if (f == null) f = decay.stamp(level, stack, FoodState.RAW);
        double p01 = f == null ? 0 : f.progress(decay.dNow(level));
        int stage = DecayLadder.FOOD.stageOf(p01);
        FoodState state = f == null ? FoodState.RAW : f.state();
        Doneness.Ring ring = stack.get(YoiComponents.DONENESS.get());
        BodyReadout body = BodyService.get().readout(p);
        double u = Thresholds.stream(p.server.overworld().getSeed(), "meal", p.getUUID().getMostSignificantBits(),
                p.getUUID().getLeastSignificantBits(), (long) Math.floor(decay.dNow(level) * 60)).nextDouble();
        Meal.Outcome o = Meal.bite(family(stack), state, ring, stage, body.condition(), body.bmi(), u);
        double grams = grams(stack);
        if (o.refused()) {
            p.displayClientMessage(Component.translatable("food.yearofice.too_far_gone"), true);
            return new Bite(false, o, grams, 0, stage);
        }
        double booked = grams * o.kcalShare();
        if (!BodyService.get().eat(p, stack, booked)) return new Bite(false, o, grams, 0, stage);
        if (o.gutIllness() > 0 || o.nauseaNow() > 0 || o.parasites() > 0 || o.spirits() != 0) {
            YearOfIce.LOG.info("[food] {} ate {} ({}, {}, {}): gut illness {}% rolled {}, nausea +{}, parasites {}%, Spirits {}"
                            + " (logged only: BodyService has no illness, nausea, parasite or Spirits hook for food yet)",
                    p.getGameProfile().getName(), stack.getItem(), state.key(), ring == null ? "-" : ring.name().toLowerCase(java.util.Locale.ROOT),
                    DecayLadder.FOOD.names().get(stage), Math.round(o.gutIllness() * 100), o.gutIll() ? "ill" : "well",
                    Math.round(o.nauseaNow()), Math.round(o.parasites() * 1000) / 10.0, o.spirits());
        }
        if (stage >= EatingLadder.TURNING) p.displayClientMessage(Component.translatable("food.yearofice.tasted_off"), false);
        else if (ring == Doneness.Ring.BURNT) p.displayClientMessage(Component.translatable("food.yearofice.burnt"), false);
        if (!p.getAbilities().instabuild) stack.shrink(1);
        return new Bite(true, o, grams, booked, stage);
    }

    /** The ring a stack carries, or null for raw food. */
    public static @Nullable Doneness.Ring ring(ItemStack stack) { return stack.get(YoiComponents.DONENESS.get()); }
}
