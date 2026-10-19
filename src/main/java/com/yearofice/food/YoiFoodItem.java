package com.yearofice.food;

import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * One of our foods: eaten with the use key like a vanilla food, one item a bite, through {@link Meals} (raw or cooked,
 * by its freshness and doneness). It carries no {@code minecraft:food} component, so vanilla hunger never sees it
 * (the body holds the food bar full) and the body's vanilla-food path leaves it alone.
 */
public class YoiFoodItem extends Item {
    public YoiFoodItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!Meals.edible(level, stack)) {
            if (player instanceof ServerPlayer sp) HudBus.get().post(sp, HudLine.of(HudLineType.PICKUP, "too_far_gone", "food"));
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return FoodConfig.tuning().eatTicks(); }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) Meals.eat(sp, stack);
        return stack;
    }
}
