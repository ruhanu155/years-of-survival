package com.yearofice.travel;

import com.yearofice.core.YearOfIce;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Travel gear (snowshoes slot) and the winter clothing set; worn rows live in {@code data_maps/item/wear.json}. */
public final class TravelItems {
    private TravelItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    private static DeferredItem<Item> one(String id) { return ITEMS.registerItem(id, Item::new, new Item.Properties().stacksTo(1)); }

    private static DeferredItem<Item> worn(String id) { return ITEMS.registerItem(id, com.yearofice.body.clothing.WornItem::new, new Item.Properties()); }

    public static final DeferredItem<Item> SKATES = one("skates"), SKIS = one("skis"), CLIMBING_SKINS = one("climbing_skins"),
            FUR_MITTENS = worn("fur_mittens"), FUR_HAT = worn("fur_hat"), PARKA = worn("parka"), HIDE_COAT = worn("hide_coat"),
            HIDE_PANTS = worn("hide_pants"), MUKLUKS = worn("mukluks"), HARE_FUR_LINERS = worn("hare_fur_liners"),
            MOCCASINS = worn("moccasins");
}
