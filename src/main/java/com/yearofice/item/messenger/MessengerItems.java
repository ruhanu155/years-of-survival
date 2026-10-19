package com.yearofice.item.messenger;

import com.mojang.serialization.Codec;
import com.yearofice.core.YearOfIce;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Trail Messenger and its data components (Minecraft engine › The messenger). The id stays
 * {@code yearofice:messenger}; its in-world name, "Trail Messenger", is the lang entry (engine rule 5).
 *
 * <p>{@code yearofice:battery} is in hundredths of a percent. It belongs to the Item model layer's
 * {@code item.component.YoiComponents} list; it is registered here until that class lands, and moves there with
 * the same id.
 */
public final class MessengerItems {
    private MessengerItems() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    /** Battery in hundredths of a percent, 0–10,000. */
    public static final Supplier<DataComponentType<Integer>> BATTERY = COMPONENTS.registerComponentType("battery",
            b -> b.persistent(Codec.intRange(0, 10_000)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final int FULL_BATTERY = 10_000;

    public static final DeferredItem<TrailMessengerItem> MESSENGER = ITEMS.register("messenger",
            () -> new TrailMessengerItem(new Item.Properties().stacksTo(1).component(BATTERY.get(), FULL_BATTERY)));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
    }
}
