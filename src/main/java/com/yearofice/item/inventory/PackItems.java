package com.yearofice.item.inventory;

import com.yearofice.core.YearOfIce;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The packs (Blueprint › Packs and containers). M2 registers the starting kit's crate backpack (2 rows, 30 kg);
 * the hide pack and pack frame arrive with their recipes, and their rows already sit in {@code pack.json}.
 */
public final class PackItems {
    private PackItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final DeferredItem<Item> CRATE_BACKPACK = ITEMS.registerItem("crate_backpack", Item::new, new Item.Properties().stacksTo(1));

    public static void register(IEventBus modBus) { ITEMS.register(modBus); }
}
