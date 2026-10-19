package com.yearofice.ecosystem;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.ai.Sensing;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.carcass.Freshness;
import com.yearofice.food.YoiFoodItem;
import com.yearofice.ecosystem.carcass.WholeGameItem;
import com.yearofice.ecosystem.species.MooseEntity;
import com.yearofice.ecosystem.species.SnowshoeHareEntity;
import com.yearofice.ecosystem.species.bird.WillowPtarmiganEntity;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The ecosystem's registry entries for the M2.5 slice. Every id here is in {@code studio/inventory.json}.
 *
 * <p>{@code yearofice:fresh} is registered once, in {@code item.component.YoiComponents} (RECONCILE 169); carcasses
 * and their meat write it through {@link Freshness}. The two meats are {@link YoiFoodItem}s, eaten through the food
 * package's bite rules.
 */
public final class EcosystemRegistry {
    private EcosystemRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);
    public static final DeferredRegister<SensorType<?>> SENSORS = DeferredRegister.create(Registries.SENSOR_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<MemoryModuleType<?>> MEMORIES = DeferredRegister.create(Registries.MEMORY_MODULE_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<Activity> ACTIVITIES = DeferredRegister.create(Registries.ACTIVITY, YearOfIce.MOD_ID);

    // ---- entities (Land animals in depth › The roster and sheet; Birds and the air › Grouse and ptarmigan) ----

    public static final DeferredHolder<EntityType<?>, EntityType<SnowshoeHareEntity>> SNOWSHOE_HARE = ENTITIES.register("snowshoe_hare",
            () -> EntityType.Builder.of(SnowshoeHareEntity::new, MobCategory.CREATURE).sized(0.4f, 0.4f).clientTrackingRange(10).build("yearofice:snowshoe_hare"));
    public static final DeferredHolder<EntityType<?>, EntityType<WillowPtarmiganEntity>> WILLOW_PTARMIGAN = ENTITIES.register("willow_ptarmigan",
            () -> EntityType.Builder.of(WillowPtarmiganEntity::new, MobCategory.CREATURE).sized(0.42f, 0.42f).clientTrackingRange(10).build("yearofice:willow_ptarmigan"));
    public static final DeferredHolder<EntityType<?>, EntityType<MooseEntity>> MOOSE = ENTITIES.register("moose",
            () -> EntityType.Builder.of(MooseEntity::new, MobCategory.CREATURE).sized(1.4f, 2.3f).clientTrackingRange(10).build("yearofice:moose"));
    /** Bestiary › In Minecraft: carcasses: 0.5 × 0.3 blocks. */
    public static final DeferredHolder<EntityType<?>, EntityType<CarcassEntity>> CARCASS_SMALL = ENTITIES.register("carcass_small",
            () -> EntityType.Builder.<CarcassEntity>of(CarcassEntity::new, MobCategory.MISC).sized(0.5f, 0.3f).clientTrackingRange(8).build("yearofice:carcass_small"));

    // ---- components and attachments ----

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CarcassRecord>> CARCASS_COMPONENT =
            COMPONENTS.registerComponentType("carcass", b -> b.persistent(CarcassRecord.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CarcassRecord.CODEC)));
    public static final Supplier<AttachmentType<CarcassRecord>> CARCASS = ATTACHMENTS.register("carcass",
            () -> AttachmentType.builder(() -> CarcassRecord.EMPTY).serialize(CarcassRecord.CODEC).build());

    // ---- items (Bestiary › In Minecraft: carcasses; Items › Food values; item_family/) ----

    public static final DeferredItem<WholeGameItem> WHOLE_GAME = ITEMS.registerItem("whole_game", WholeGameItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SNOWSHOE_HARE_MEAT = ITEMS.register("snowshoe_hare_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> WILLOW_PTARMIGAN_MEAT = ITEMS.register("willow_ptarmigan_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> HARE_PELT = ITEMS.registerItem("hare_pelt", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> SINEW = ITEMS.registerItem("sinew", Item::new, new Item.Properties().stacksTo(32));

    // ---- Brain parts (Minecraft development › Brain: sensors, memories and activities) ----

    public static final DeferredHolder<SensorType<?>, SensorType<Sensing.Sight>> SIGHT = SENSORS.register("sight", () -> new SensorType<>(Sensing.Sight::new));
    public static final DeferredHolder<SensorType<?>, SensorType<Sensing.Hearing>> HEARING = SENSORS.register("hearing", () -> new SensorType<>(Sensing.Hearing::new));
    public static final DeferredHolder<SensorType<?>, SensorType<Sensing.Scent>> SCENT = SENSORS.register("scent", () -> new SensorType<>(Sensing.Scent::new));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<LivingEntity>> THREAT = MEMORIES.register("threat", () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<LivingEntity>> LOOK_TARGET = MEMORIES.register("look_target", () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<Activity, Activity> ROUTINE = ACTIVITIES.register("routine", () -> new Activity("yearofice:routine"));
    public static final DeferredHolder<Activity, Activity> ALERT = ACTIVITIES.register("alert", () -> new Activity("yearofice:alert"));
    public static final DeferredHolder<Activity, Activity> FREEZE = ACTIVITIES.register("freeze", () -> new Activity("yearofice:freeze"));
    public static final DeferredHolder<Activity, Activity> FLEE = ACTIVITIES.register("flee", () -> new Activity("yearofice:flee"));

    static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        SENSORS.register(modBus);
        MEMORIES.register(modBus);
        ACTIVITIES.register(modBus);
    }
}
