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

    public static final DeferredItem<Item> SKATES = one("skates"), SKIS = one("skis"), CLIMBING_SKINS = one("climbing_skins"),
            FUR_MITTENS = one("fur_mittens"), FUR_HAT = one("fur_hat"), PARKA = one("parka"), HIDE_COAT = one("hide_coat"),
            HIDE_PANTS = one("hide_pants"), MUKLUKS = one("mukluks"), HARE_FUR_LINERS = one("hare_fur_liners"),
            MOCCASINS = one("moccasins");
}
