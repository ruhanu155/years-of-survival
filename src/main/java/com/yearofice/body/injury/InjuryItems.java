package com.yearofice.body.injury;

import com.yearofice.core.YearOfIce;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The treatment items the injury lane needs before the medicine lane lands: the medkit bandage. */
public final class InjuryItems {
    private InjuryItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final DeferredItem<Item> BANDAGE = ITEMS.registerItem("bandage", Item::new, new Item.Properties().stacksTo(16));

    public static void register(IEventBus modBus) { ITEMS.register(modBus); }
}
