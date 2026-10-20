package com.yearofice.fire.water;

import com.yearofice.api.body.ContainerData;
import com.yearofice.api.body.WaterContent;
import com.yearofice.core.YearOfIce;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Water containers (Blueprint › Fire, water and light › Carrying water; Items › Containers). Every id is in
 * {@code studio/inventory.json}; litres and where each may stand to boil are in {@code data_maps/item/container.json},
 * not here. The component {@code yearofice:water} is {@link WaterContent}.
 */
public final class WaterRegistry {
    private WaterRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    private static DeferredItem<WaterContainerItem> container(String id) {
        return ITEMS.registerItem(id, WaterContainerItem::new, new Item.Properties().stacksTo(1));
    }

    /** The steel water bottle from the crate: 1 L, sits in the coals to boil or thaw. */
    public static final DeferredItem<WaterContainerItem> STEEL_BOTTLE = container("steel_bottle");
    /** The cooking pot from the crate: 2 L. */
    public static final DeferredItem<WaterContainerItem> COOKING_POT = container("cooking_pot");
    /** The birch-bark bucket: 4 L; it cannot go on a fire. */
    public static final DeferredItem<WaterContainerItem> BARK_BUCKET = container("bark_bucket");
    /** A tin can from the station: 0.4 L, boils on coals. */
    public static final DeferredItem<WaterContainerItem> TIN_CAN = container("tin_can");
    /** A jerrycan from the station or the drift: 15 L of water at camp. */
    public static final DeferredItem<WaterContainerItem> JERRYCAN = container("jerrycan");

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        WaterContent.COMPONENTS.register(modBus);
        modBus.addListener((RegisterDataMapTypesEvent e) -> e.register(ContainerData.MAP));
    }
}
