package com.yearofice.body.clothing;

import com.yearofice.core.YearOfIce;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The start clothes (Blueprint › Clothing, Start tier); the winter set is {@code travel.TravelItems}. Rows: {@code wear.json}. */
public final class ClothingItems {
    private ClothingItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    private static DeferredItem<Item> w(String id) { return ITEMS.registerItem(id, WornItem::new, new Item.Properties()); }

    public static final DeferredItem<Item> FLEECE_JACKET = w("fleece_jacket"), RAIN_SHELL = w("rain_shell"),
            WORK_PANTS = w("work_pants"), WOOL_SOCKS = w("wool_socks"), HIKING_BOOTS = w("hiking_boots"),
            WOOL_BEANIE = w("wool_beanie"), WORK_GLOVES = w("work_gloves");

    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
