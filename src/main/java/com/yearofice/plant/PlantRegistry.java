package com.yearofice.plant;

import com.yearofice.core.YearOfIce;
import com.yearofice.food.YoiFoodItem;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The plant blocks (one per species, no block item) and the part items named after their species. */
public final class PlantRegistry {
    private PlantRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final Map<String, net.neoforged.neoforge.registries.DeferredBlock<PlantBlock>> SPECIES = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> PARTS = new LinkedHashMap<>();
    /** Material from any plant that is not a food ({@code custom_data.species} says which). */
    public static final DeferredItem<Item> PLANT_MATERIAL = ITEMS.registerItem("plant_material", Item::new, new Item.Properties().stacksTo(64));

    static {
        for (String id : PlantCatalog.SPECIES) {
            String full = YearOfIce.MOD_ID + ":" + id;
            SPECIES.put(id, BLOCKS.registerBlock(id, p -> new PlantBlock(p, full), PlantBlock.props()));
        }
        Set<String> food = new HashSet<>(Arrays.asList(PlantCatalog.FOOD_PARTS));
        for (String id : PlantCatalog.PARTS) {
            PARTS.put(id, food.contains(id)
                    ? ITEMS.registerItem(id, YoiFoodItem::new, new Item.Properties().stacksTo(64))
                    : ITEMS.registerItem(id, Item::new, new Item.Properties().stacksTo(64)));
        }
    }

    public static Block block(String species) { return SPECIES.get(species).get(); }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
