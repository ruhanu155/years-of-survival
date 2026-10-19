package com.yearofice.body.intake;

import com.yearofice.api.body.BodyService;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyConfig;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Eating vanilla foods in the slice. Vanilla's food bar is held full (Vanilla needs replaced), which would stop
 * every food that needs hunger, so each vanilla food is made always-edible; what it does to the body comes from
 * its {@code yearofice:food} row through {@link BodyService#eat}, never from vanilla nutrition. Our own foods
 * carry no {@code minecraft:food} component and go through {@code food.YoiFoodItem} when the Food department
 * lands it.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class Eating {
    private Eating() {}

    /** Every vanilla food can be eaten at any hunger: hunger is held full and never gates eating. */
    @EventBusSubscriber(modid = YearOfIce.MOD_ID)
    public static final class Components {
        private Components() {}

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void alwaysEdible(ModifyDefaultComponentsEvent e) {
            e.getAllItems().forEach(item -> {
                FoodProperties f = item.components().get(DataComponents.FOOD);
                if (f == null || f.canAlwaysEat()) return;
                FoodProperties edible = new FoodProperties(f.nutrition(), f.saturation(), true, f.eatSeconds(),
                        f.usingConvertsTo(), f.effects());
                e.modify(item, b -> b.set(DataComponents.FOOD, edible));
            });
        }
    }

    @SubscribeEvent
    public static void onStart(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (FoodMap.row(e.getItem()) == null) return;
        if (BodyAttachments.of(p).stomachKg >= BodyConfig.tuning().stomachKg()) {
            p.displayClientMessage(Component.translatable("messenger.yearofice.warn.stomach_full"), true);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ItemStack eaten = e.getItem();
        // Only vanilla foods: ours (food.YoiFoodItem) have no minecraft:food and book their own bite through food.Meals.
        if (!eaten.has(DataComponents.FOOD)) return;
        FoodMap.Row row = FoodMap.row(eaten);
        if (row == null) return;
        BodyService.get().eat(p, eaten, row.weightG());
    }
}
