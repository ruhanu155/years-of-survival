package com.yearofice.food;

import com.yearofice.core.YearOfIce;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The ten named foods of Items › Food values (RECONCILE 137) and the cooked {@code dish}. Their numbers are rows of
 * {@code data_maps/item/food.json}; a dish carries its own {@code yearofice:nutrients} and {@code yearofice:dish_id}
 * (set by {@link Dishes}), and a dried stack carries the nutrients of the fresh food it was dried from.
 */
public final class FoodItems {
    private FoodItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    private static DeferredItem<YoiFoodItem> food(String id) { return ITEMS.registerItem(id, YoiFoodItem::new, new Item.Properties()); }

    public static final DeferredItem<YoiFoodItem> PEMMICAN = food("pemmican");
    public static final DeferredItem<YoiFoodItem> DRIED_FISH = food("dried_fish");
    /** Drying-rack output for meat, as {@link #DRIED_FISH} is for fish (food.json row {@code dried_meat}). */
    public static final DeferredItem<YoiFoodItem> DRIED_MEAT = food("dried_meat");
    public static final DeferredItem<YoiFoodItem> HEN_EGG = food("hen_egg");
    public static final DeferredItem<YoiFoodItem> GOAT_MILK = food("goat_milk");
    public static final DeferredItem<YoiFoodItem> ROSE_HIPS = food("rose_hips");
    public static final DeferredItem<YoiFoodItem> SPRUCE_TIPS = food("spruce_tips");
    public static final DeferredItem<YoiFoodItem> BARLEY_FLOUR = food("barley_flour");
    public static final DeferredItem<YoiFoodItem> SAUERKRAUT = food("sauerkraut");
    public static final DeferredItem<YoiFoodItem> BIRCH_SAP = food("birch_sap");
    public static final DeferredItem<YoiFoodItem> BIRCH_SYRUP = food("birch_syrup");
    /** A cooked mix: one item with {@code weight_g}, {@code nutrients} and (when the notebook names it) {@code dish_id}. */
    public static final DeferredItem<YoiFoodItem> DISH = ITEMS.registerItem("dish", YoiFoodItem::new, new Item.Properties().stacksTo(1));

    /** B5 bone grease: 1 crushed_bone is 1 kg of pounded marrow bone; 1 bone_grease is 50 g of rendered fat. */
    public static final DeferredItem<YoiFoodItem> CRUSHED_BONE = food("crushed_bone");
    public static final DeferredItem<YoiFoodItem> BONE_GREASE = food("bone_grease");

    /** The ten named foods, for tests and {@code /yoi food}. */
    public static List<DeferredItem<YoiFoodItem>> named() {
        return List.of(PEMMICAN, DRIED_FISH, HEN_EGG, GOAT_MILK, ROSE_HIPS, SPRUCE_TIPS, BARLEY_FLOUR, SAUERKRAUT, BIRCH_SAP, BIRCH_SYRUP);
    }

    public static void register(IEventBus modBus) { ITEMS.register(modBus); }
}
