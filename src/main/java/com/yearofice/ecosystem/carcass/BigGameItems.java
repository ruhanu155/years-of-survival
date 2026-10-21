package com.yearofice.ecosystem.carcass;

import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.food.YoiFoodItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The parts big game and predators give (Blueprint › Butchering and yields): a meat per species, a hide or a pelt,
 * and the shared organs, fat, marrow bones, antlers and claws. Registered into the ecosystem's item register so the
 * mod constructor needs no new line. The meats and organs are {@link YoiFoodItem}s, eaten through the food rules;
 * their numbers are in {@code data_maps/item/food.json}.
 */
public final class BigGameItems {
    private BigGameItems() {}

    public static final String[] MEATS = {"moose", "caribou", "mule_deer", "black_bear", "grizzly", "wolf", "coyote", "red_fox", "lynx", "wolverine"};
    public static final String[] HIDES = {"moose", "caribou", "mule_deer", "black_bear", "grizzly"};
    public static final String[] PELTS = {"wolf", "coyote", "red_fox", "lynx", "wolverine"};
    public static final String[] ORGANS = {"animal_fat", "heart", "liver", "kidney", "tongue"};
    public static final String[] MATERIALS = {"marrow_bone", "antler", "claw"};

    public static final Map<String, DeferredItem<Item>> ALL = new LinkedHashMap<>();

    static {
        for (String s : MEATS) food(s + "_meat");
        for (String s : ORGANS) food(s);
        for (String s : HIDES) plain(s + "_hide", 8);
        for (String s : PELTS) plain(s + "_pelt", 16);
        plain("marrow_bone", 16);
        plain("antler", 4);
        plain("claw", 32);
    }

    static void food(String id) { ALL.put(id, EcosystemRegistry.ITEMS.register(id, () -> new YoiFoodItem(new Item.Properties().stacksTo(80)))); }

    static void plain(String id, int stack) { ALL.put(id, EcosystemRegistry.ITEMS.registerItem(id, Item::new, new Item.Properties().stacksTo(stack))); }

    /** Forces the class to load, so the items register before the register is bound. */
    public static void init() {}
}
