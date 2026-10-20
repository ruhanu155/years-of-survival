package com.yearofice.item.inventory;

import com.yearofice.core.YearOfIce;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Placeholder items for the starting kit (steel_bottle, cooking_pot and headlamp belong to the fire lane) (Blueprint > Start site > Starting kit). They carry ids, weights and
 * names only; the crafting, fire, water and light lanes give them their behaviour.
 */
public final class KitItems {
    private KitItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final DeferredItem<Item> FIXED_BLADE_KNIFE = ITEMS.registerItem("fixed_blade_knife", Item::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PILOT_BREAD = ITEMS.registerItem("pilot_bread", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> MEDKIT = ITEMS.registerItem("medkit", Item::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> BEAR_SPRAY = ITEMS.registerItem("bear_spray", Item::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> CORD = ITEMS.registerItem("cord", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> SEED_PACKET = ITEMS.registerItem("seed_packet", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> COMPASS = ITEMS.registerItem("compass", Item::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PLANT_GUIDE = ITEMS.registerItem("plant_guide", Item::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SOLAR_PANEL_SMALL = ITEMS.registerItem("solar_panel_small", Item::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> MOSQUITO_HEAD_NET = ITEMS.registerItem("mosquito_head_net", Item::new, new Item.Properties().stacksTo(1));

    public static void register(IEventBus modBus) { ITEMS.register(modBus); }
}
