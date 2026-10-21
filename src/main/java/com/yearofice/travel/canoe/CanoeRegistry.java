package com.yearofice.travel.canoe;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The canoe entity and its carried form ({@code yearofice:canoe}, entity and item share the id). */
public final class CanoeRegistry {
    private CanoeRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CanoeEntity>> CANOE_ENTITY = ENTITIES.register("canoe",
            () -> EntityType.Builder.<CanoeEntity>of(CanoeEntity::new, MobCategory.MISC).sized(1.375f, 0.5625f).eyeHeight(0.5625f)
                    .clientTrackingRange(10).build("canoe"));
    /** The carried canoe: a 30 kg item (the portage); use on water sets it down. */
    public static final DeferredItem<Item> CANOE = ITEMS.registerItem("canoe", CanoeItem::new, new Item.Properties().stacksTo(1));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
    }
}
