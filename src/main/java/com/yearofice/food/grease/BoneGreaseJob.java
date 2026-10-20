package com.yearofice.food.grease;

import com.yearofice.food.DishMaker;
import com.yearofice.food.FoodItems;
import com.yearofice.sim.food.BoneGrease;
import com.yearofice.sim.food.Dishes;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The simmer-cool-skim step of bone grease (Items › B5), the server side of {@link BoneGrease}. One
 * {@code crushed_bone} is 1 kg of pounded marrow bone; one {@code bone_grease} is 50 g, so a stack of grease counts
 * grams-per-kg the way the tuning says. The water left is a {@code dish} with {@code dish_id} {@code skimmed_broth}.
 */
public final class BoneGreaseJob {
    private BoneGreaseJob() {}

    public record Result(ItemStack grease, ItemStack broth, double greaseKg, String warning) {}

    /** Items of grease for {@code boneKg}: kilograms of grease divided by the 50 g one item weighs. */
    public static int greaseItems(double boneKg, BoneGrease.Fire fire) {
        return (int) Math.round(BoneGrease.yieldKg(boneKg, GreaseConfig.gPerKg(), GreaseConfig.boilPenalty(), fire) * 1000.0 / 50.0);
    }

    /** Simmers {@code boneKg} of crushed bone on {@code fire}; the pot's water is 1 L per kg. */
    public static Result simmer(ServerLevel level, double boneKg, BoneGrease.Fire fire) {
        double kg = BoneGrease.yieldKg(boneKg, GreaseConfig.gPerKg(), GreaseConfig.boilPenalty(), fire);
        ItemStack grease = new ItemStack(FoodItems.BONE_GREASE.get(), greaseItems(boneKg, fire));
        ItemStack broth = DishMaker.make(level, List.<ItemStack>of(), Dishes.Method.STEEP, "skimmed_broth", BoneGrease.brothL(boneKg) * 1000.0);
        String warn = fire == BoneGrease.Fire.LOW ? "" : "Too hot: the grease is mixing in";
        return new Result(grease, broth, kg, warn);
    }
}
