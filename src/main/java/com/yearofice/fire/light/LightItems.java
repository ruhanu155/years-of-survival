package com.yearofice.fire.light;

import com.yearofice.core.YearOfIce;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Carried lights (Items › Containers, light and signals › Light; Ideas from viewers › Pitch lights). Every id is in
 * {@code studio/inventory.json}; level, cone, burn time and how rain treats each are in
 * {@code data_maps/item/light.json}. The Coleman lantern (a fuel lamp from Station Nine) and the tin lantern (M8) are
 * not here yet.
 */
public final class LightItems {
    private LightItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    private static DeferredItem<LightItem> light(String id) {
        return ITEMS.registerItem(id, LightItem::new, new Item.Properties().stacksTo(1));
    }

    /** A stick wrapped in birch bark: level 10, 15 game minutes; rain puts it out. */
    public static final DeferredItem<LightItem> BIRCH_BARK_TORCH = light("birch_bark_torch");
    /** A stick, a bark wrap and pitch: level 8, 30 game minutes; holds in light rain. */
    public static final DeferredItem<LightItem> PITCH_TORCH = light("pitch_torch");
    /** A tallow candle: level 6, 4 hours. */
    public static final DeferredItem<LightItem> TALLOW_CANDLE = light("tallow_candle");
    /** A pitch candle: level 6, 3 hours. */
    public static final DeferredItem<LightItem> PITCH_CANDLE = light("pitch_candle");
    /** The crate headlamp, wide mode: level 12, 20 hours of battery. */
    public static final DeferredItem<LightItem> HEADLAMP = light("headlamp");
}
