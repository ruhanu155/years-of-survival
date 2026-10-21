package com.yearofice.build.outpost;

import com.yearofice.core.YearOfIce;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Outpost-lane items: the B11 hot tent kit. */
public final class OutpostRegistry {
    private OutpostRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final DeferredItem<Item> CANVAS_WALL_TENT = ITEMS.registerSimpleItem("canvas_wall_tent", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> STOVE_JACK = ITEMS.registerSimpleItem("stove_jack", new Item.Properties());
    public static final DeferredItem<Item> TENT_STOVE = ITEMS.registerSimpleItem("tent_stove", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> CAN_STOVE = ITEMS.registerSimpleItem("can_stove", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SPARK_SCREEN = ITEMS.registerSimpleItem("spark_screen", new Item.Properties());

    static void register(IEventBus modBus) { ITEMS.register(modBus); }
}
