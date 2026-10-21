package com.yearofice.ecosystem.livestock;

import com.yearofice.core.YearOfIce;
import com.yearofice.food.YoiFoodItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Livestock's entity types and the items their carcasses dress into (M9 lane C). */
public final class LivestockRegistry {
    private LivestockRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GoatEntity>> GOAT = ENTITIES.register("goat",
            () -> EntityType.Builder.of(GoatEntity::new, MobCategory.CREATURE).sized(0.7f, 0.9f).clientTrackingRange(10).build("yearofice:goat"));
    public static final DeferredHolder<EntityType<?>, EntityType<ReindeerEntity>> REINDEER = ENTITIES.register("reindeer",
            () -> EntityType.Builder.of(ReindeerEntity::new, MobCategory.CREATURE).sized(0.9f, 1.3f).clientTrackingRange(10).build("yearofice:reindeer"));
    public static final DeferredHolder<EntityType<?>, EntityType<HenEntity>> HEN = ENTITIES.register("hen",
            () -> EntityType.Builder.of(HenEntity::new, MobCategory.CREATURE).sized(0.4f, 0.5f).clientTrackingRange(10).build("yearofice:hen"));

    public static final DeferredItem<Item> GOAT_MEAT = ITEMS.register("goat_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> REINDEER_MEAT = ITEMS.register("reindeer_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> HEN_MEAT = ITEMS.register("hen_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> GOAT_HIDE = ITEMS.registerItem("goat_hide", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> REINDEER_HIDE = ITEMS.registerItem("reindeer_hide", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> GOAT_WOOL = ITEMS.registerItem("goat_wool", Item::new, new Item.Properties().stacksTo(64));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(LivestockRegistry::attributes);
    }

    static void attributes(EntityAttributeCreationEvent e) {
        e.put(GOAT.get(), com.yearofice.ecosystem.YoiAnimal.attributes(20).build());
        e.put(REINDEER.get(), com.yearofice.ecosystem.YoiAnimal.attributes(40).build());
        e.put(HEN.get(), com.yearofice.ecosystem.YoiAnimal.attributes(3).build());
    }
}
