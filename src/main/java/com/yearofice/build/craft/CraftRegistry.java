package com.yearofice.build.craft;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Building's M3 crafting entries: the workbench and its block entity, the recipe type {@code yearofice:station}, the
 * knapping and bench items and the crate knife. Every id here is in {@code studio/inventory.json}.
 */
public final class CraftRegistry {
    private CraftRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, YearOfIce.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<StationRecipe>> STATION = RECIPE_TYPES.register("station", () -> RecipeType.simple(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "station")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StationRecipe>> STATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("station", () -> StationRecipe.SERIALIZER);

    public static final DeferredBlock<WorkbenchBlock> WORKBENCH = BLOCKS.registerBlock("workbench", WorkbenchBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> WORKBENCH_ITEM = ITEMS.registerSimpleBlockItem("workbench", WORKBENCH, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> WORKBENCH_ENTITY =
            BLOCK_ENTITIES.register("workbench", () -> BlockEntityType.Builder.of(WorkbenchBlockEntity::new, WORKBENCH.get()).build(null));

    /** Durability numbers from Tool durability; {@code props.json} repeats them as {@code max_condition}. */
    public static final int KNIFE_DURABILITY = 6000, FLINT_KNIFE_DURABILITY = 300;

    /** The crate's fixed-blade knife (Tool durability: durability 6,000, Edge 400). */
    public static final DeferredItem<Item> KNIFE = ITEMS.registerItem("knife", Item::new, new Item.Properties().durability(KNIFE_DURABILITY));
    /** A fist-sized rough stone, picked up and not made: the hammerstone (Recipes › More Start recipes). */
    public static final DeferredItem<Item> ROUGH_STONE = ITEMS.registerItem("rough_stone", Item::new, new Item.Properties().stacksTo(20));
    /** A flint core from a gravel bar, to be knapped. */
    public static final DeferredItem<Item> FLINT_NODULE = ITEMS.registerItem("flint_nodule", Item::new, new Item.Properties().stacksTo(20));
    /** A knapped flint knife (durability 300, Edge 100). */
    public static final DeferredItem<Item> FLINT_KNIFE = ITEMS.registerItem("flint_knife", Item::new, new Item.Properties().durability(FLINT_KNIFE_DURABILITY));
    public static final DeferredItem<Item> STAKE = ITEMS.registerItem("stake", Item::new, new Item.Properties().stacksTo(50));
    public static final DeferredItem<Item> TOOL_HANDLE = ITEMS.registerItem("tool_handle", Item::new, new Item.Properties().stacksTo(20));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        Wear.COMPONENTS.register(modBus);
        CraftAttachments.register(modBus);
    }
}
