package com.yearofice.food;

import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.food.Dishes;
import com.yearofice.sim.food.Nutrients;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Finishes a cooked mix as one {@code yearofice:dish} (Items › Food values: "Cooked dishes are one item"): the
 * ingredients' nutrients summed at the moment it finishes, times the method's factors, with {@code weight_g} and, when
 * the notebook names the mix, {@code dish_id}. Its freshness starts at the cooked state.
 */
public final class DishMaker {
    private DishMaker() {}

    /** The nutrients of a stack: what it carries (a dish, dried food), else its food row for its weight and count. */
    public static @Nullable Nutrients of(ItemStack s) {
        Nutrients carried = s.get(YoiComponents.NUTRIENTS.get());
        if (carried != null) return carried.times(s.getCount());
        FoodMap.Row row = FoodMap.row(s);
        if (row == null) return null;
        return Nutrients.of(row.values("dish"), Meals.grams(s) * s.getCount(), row.waterMlPer100g());
    }

    /** Makes the dish. {@code waterG} is water added to the pot (it adds weight and mL, no energy). */
    public static ItemStack make(ServerLevel level, List<ItemStack> ingredients, Dishes.Method method, @Nullable String dishId, double waterG) {
        List<Nutrients> parts = new ArrayList<>();
        double grams = waterG;
        for (ItemStack s : ingredients) {
            Nutrients n = of(s);
            if (n == null) continue;
            parts.add(n);
            grams += Meals.grams(s) * s.getCount();
        }
        Nutrients sum = Dishes.cook(parts, method);
        Nutrients dish = new Nutrients(sum.kcal(), sum.proteinG(), sum.fatG(), sum.vitCMg(), sum.waterMl() + waterG);
        ItemStack out = new ItemStack(FoodItems.DISH.get());
        out.set(YoiComponents.WEIGHT_G.get(), (int) Math.round(grams));
        out.set(YoiComponents.NUTRIENTS.get(), dish);
        if (dishId != null) out.set(YoiComponents.DISH_ID.get(), dishId);
        DecayService.get().stamp(level, out, FoodState.COOKED);
        return out;
    }
}
