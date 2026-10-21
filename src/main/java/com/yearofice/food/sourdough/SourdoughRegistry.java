package com.yearofice.food.sourdough;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** B6 sourdough: the starter item and its {@code yearofice:starter} component. */
public final class SourdoughRegistry {
    private SourdoughRegistry() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StarterComp>> STARTER =
            COMPONENTS.registerComponentType("starter", b -> b.persistent(StarterComp.CODEC).networkSynchronized(StarterComp.STREAM));

    public static final DeferredItem<Item> SOURDOUGH_STARTER = ITEMS.register("sourdough_starter", () -> new StarterItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
    }
}
