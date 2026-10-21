package com.yearofice.ecosystem;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.ai.Sensing;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.carcass.Freshness;
import com.yearofice.food.YoiFoodItem;
import com.yearofice.ecosystem.carcass.WholeGameItem;
import com.yearofice.ecosystem.species.BearEntity;
import com.yearofice.ecosystem.species.MooseEntity;
import com.yearofice.ecosystem.species.rig.BigGameEntity;
import com.yearofice.ecosystem.species.rig.PredatorEntity;
import com.yearofice.ecosystem.species.SmallGameEntity;
import com.yearofice.ecosystem.species.SnowshoeHareEntity;
import com.yearofice.ecosystem.species.bird.GrouseEntity;
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
    public static final net.neoforged.neoforge.registries.DeferredRegister.Blocks BLOCKS = net.neoforged.neoforge.registries.DeferredRegister.createBlocks(YearOfIce.MOD_ID);
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

    /** What dressing leaves behind (Decay and time › Carcasses and gut piles). */
    public static final net.neoforged.neoforge.registries.DeferredBlock<com.yearofice.ecosystem.carcass.GutPileBlock> GUT_PILE = BLOCKS.registerBlock("gut_pile",
            com.yearofice.ecosystem.carcass.GutPileBlock::new, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED).strength(0.1f).noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));

    // ---- M5 lane A: small game, furbearers, grouse (Land animals in depth › Mob sheets: small game and furbearers) ----
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> RED_SQUIRREL = ENTITIES.register("red_squirrel",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.3f, 0.35f).clientTrackingRange(10).build("yearofice:red_squirrel"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> MARTEN = ENTITIES.register("marten",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.35f, 0.56f).clientTrackingRange(10).build("yearofice:marten"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> ERMINE = ENTITIES.register("ermine",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.2f, 0.28f).clientTrackingRange(10).build("yearofice:ermine"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> MINK = ENTITIES.register("mink",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.3f, 0.53f).clientTrackingRange(10).build("yearofice:mink"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> MUSKRAT = ENTITIES.register("muskrat",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.35f, 0.3f).clientTrackingRange(10).build("yearofice:muskrat"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> BEAVER = ENTITIES.register("beaver",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.6f, 0.5f).clientTrackingRange(10).build("yearofice:beaver"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> PORCUPINE = ENTITIES.register("porcupine",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.5f, 0.4f).clientTrackingRange(10).build("yearofice:porcupine"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> WOOD_FROG = ENTITIES.register("wood_frog",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.15f, 0.16f).clientTrackingRange(10).build("yearofice:wood_frog"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> YELLOWJACKET = ENTITIES.register("yellowjacket",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.1f, 0.12f).clientTrackingRange(10).build("yearofice:yellowjacket"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> CRITTER = ENTITIES.register("critter",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.3f, 0.3f).clientTrackingRange(10).build("yearofice:critter"));
    public static final DeferredHolder<EntityType<?>, EntityType<GrouseEntity>> SPRUCE_GROUSE = ENTITIES.register("spruce_grouse",
            () -> EntityType.Builder.of(GrouseEntity::new, MobCategory.CREATURE).sized(0.45f, 0.45f).clientTrackingRange(10).build("yearofice:spruce_grouse"));
    public static final DeferredHolder<EntityType<?>, EntityType<GrouseEntity>> RUFFED_GROUSE = ENTITIES.register("ruffed_grouse",
            () -> EntityType.Builder.of(GrouseEntity::new, MobCategory.CREATURE).sized(0.45f, 0.45f).clientTrackingRange(10).build("yearofice:ruffed_grouse"));
    public static final DeferredHolder<EntityType<?>, EntityType<GrouseEntity>> ROCK_PTARMIGAN = ENTITIES.register("rock_ptarmigan",
            () -> EntityType.Builder.of(GrouseEntity::new, MobCategory.CREATURE).sized(0.42f, 0.42f).clientTrackingRange(10).build("yearofice:rock_ptarmigan"));
    /** Bestiary › In Minecraft: carcasses: tiny 0.3 × 0.2, medium 0.9 × 0.45 blocks. */
    public static final DeferredHolder<EntityType<?>, EntityType<CarcassEntity>> CARCASS_TINY = ENTITIES.register("carcass_tiny",
            () -> EntityType.Builder.<CarcassEntity>of(CarcassEntity::new, MobCategory.MISC).sized(0.3f, 0.2f).clientTrackingRange(8).build("yearofice:carcass_tiny"));
    public static final DeferredHolder<EntityType<?>, EntityType<CarcassEntity>> CARCASS_MEDIUM = ENTITIES.register("carcass_medium",
            () -> EntityType.Builder.<CarcassEntity>of(CarcassEntity::new, MobCategory.MISC).sized(0.9f, 0.45f).clientTrackingRange(8).build("yearofice:carcass_medium"));

    // ---- M6 lane A: big game, predators and the large carcasses (Land animals in depth › Mob sheets: big game, predators) ----
    public static final DeferredHolder<EntityType<?>, EntityType<BigGameEntity>> CARIBOU = ENTITIES.register("caribou",
            () -> EntityType.Builder.of(BigGameEntity::new, MobCategory.CREATURE).sized(1.0f, 1.5f).clientTrackingRange(10).build("yearofice:caribou"));
    public static final DeferredHolder<EntityType<?>, EntityType<BigGameEntity>> MULE_DEER = ENTITIES.register("mule_deer",
            () -> EntityType.Builder.of(BigGameEntity::new, MobCategory.CREATURE).sized(0.8f, 1.35f).clientTrackingRange(10).build("yearofice:mule_deer"));
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> BLACK_BEAR = ENTITIES.register("black_bear",
            () -> EntityType.Builder.of(BearEntity::new, MobCategory.CREATURE).sized(1.0f, 1.1f).clientTrackingRange(10).build("yearofice:black_bear"));
    public static final DeferredHolder<EntityType<?>, EntityType<BearEntity>> GRIZZLY = ENTITIES.register("grizzly",
            () -> EntityType.Builder.of(BearEntity::new, MobCategory.CREATURE).sized(1.3f, 1.5f).clientTrackingRange(10).build("yearofice:grizzly"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.yearofice.ecosystem.companion.WolfEntity>> WOLF = ENTITIES.register("wolf",
            () -> EntityType.Builder.of(com.yearofice.ecosystem.companion.WolfEntity::new, MobCategory.CREATURE).sized(0.8f, 0.9f).clientTrackingRange(10).build("yearofice:wolf"));
    public static final DeferredHolder<EntityType<?>, EntityType<PredatorEntity>> COYOTE = ENTITIES.register("coyote",
            () -> EntityType.Builder.of(PredatorEntity::new, MobCategory.CREATURE).sized(0.6f, 0.65f).clientTrackingRange(10).build("yearofice:coyote"));
    public static final DeferredHolder<EntityType<?>, EntityType<PredatorEntity>> RED_FOX = ENTITIES.register("red_fox",
            () -> EntityType.Builder.of(PredatorEntity::new, MobCategory.CREATURE).sized(0.5f, 0.5f).clientTrackingRange(10).build("yearofice:red_fox"));
    public static final DeferredHolder<EntityType<?>, EntityType<PredatorEntity>> LYNX = ENTITIES.register("lynx",
            () -> EntityType.Builder.of(PredatorEntity::new, MobCategory.CREATURE).sized(0.6f, 0.65f).clientTrackingRange(10).build("yearofice:lynx"));
    public static final DeferredHolder<EntityType<?>, EntityType<PredatorEntity>> WOLVERINE = ENTITIES.register("wolverine",
            () -> EntityType.Builder.of(PredatorEntity::new, MobCategory.CREATURE).sized(0.6f, 0.5f).clientTrackingRange(10).build("yearofice:wolverine"));
    /** Bestiary › In Minecraft: carcasses: large 1.6 × 0.8, huge 2.6 × 1.2 blocks. */
    public static final DeferredHolder<EntityType<?>, EntityType<CarcassEntity>> CARCASS_LARGE = ENTITIES.register("carcass_large",
            () -> EntityType.Builder.<CarcassEntity>of(CarcassEntity::new, MobCategory.MISC).sized(1.6f, 0.8f).clientTrackingRange(8).build("yearofice:carcass_large"));
    public static final DeferredHolder<EntityType<?>, EntityType<CarcassEntity>> CARCASS_HUGE = ENTITIES.register("carcass_huge",
            () -> EntityType.Builder.<CarcassEntity>of(CarcassEntity::new, MobCategory.MISC).sized(2.6f, 1.2f).clientTrackingRange(8).build("yearofice:carcass_huge"));

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
    public static final DeferredItem<Item> RED_SQUIRREL_MEAT = ITEMS.register("red_squirrel_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> MARTEN_MEAT = ITEMS.register("marten_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> MINK_MEAT = ITEMS.register("mink_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> MUSKRAT_MEAT = ITEMS.register("muskrat_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> PORCUPINE_MEAT = ITEMS.register("porcupine_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> BEAVER_MEAT = ITEMS.register("beaver_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> SPRUCE_GROUSE_MEAT = ITEMS.register("spruce_grouse_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> RUFFED_GROUSE_MEAT = ITEMS.register("ruffed_grouse_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> ROCK_PTARMIGAN_MEAT = ITEMS.register("rock_ptarmigan_meat", () -> new YoiFoodItem(new Item.Properties().stacksTo(80)));
    public static final DeferredItem<Item> MARTEN_PELT = ITEMS.registerItem("marten_pelt", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> MINK_PELT = ITEMS.registerItem("mink_pelt", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> MUSKRAT_PELT = ITEMS.registerItem("muskrat_pelt", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> RED_SQUIRREL_PELT = ITEMS.registerItem("red_squirrel_pelt", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> ERMINE_PELT = ITEMS.registerItem("ermine_pelt", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> BEAVER_PELT = ITEMS.registerItem("beaver_pelt", Item::new, new Item.Properties().stacksTo(8));
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
    // M6 lane D: the raid activities and their memories (A2 door entry, A3 rack thieves). The visit itself is resolved by roll.
    public static final DeferredHolder<Activity, Activity> RAID_SHELTER = ACTIVITIES.register("raid_shelter", () -> new Activity("yearofice:raid_shelter"));
    public static final DeferredHolder<Activity, Activity> RACK_RAID = ACTIVITIES.register("rack_raid", () -> new Activity("yearofice:rack_raid"));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<net.minecraft.core.GlobalPos>> OPEN_DOOR_TARGET =
            MEMORIES.register("open_door_target", () -> new MemoryModuleType<>(Optional.empty()));
    public static final DeferredHolder<MemoryModuleType<?>, MemoryModuleType<net.minecraft.core.GlobalPos>> RACK_TARGET =
            MEMORIES.register("rack_target", () -> new MemoryModuleType<>(Optional.empty()));

    static void register(IEventBus modBus) {
        com.yearofice.ecosystem.carcass.BigGameItems.init();
        BLOCKS.register(modBus);
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        SENSORS.register(modBus);
        MEMORIES.register(modBus);
        ACTIVITIES.register(modBus);
    }
}
