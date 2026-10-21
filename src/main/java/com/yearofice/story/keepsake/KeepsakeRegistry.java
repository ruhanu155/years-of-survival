package com.yearofice.story.keepsake;

import com.mojang.serialization.Codec;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The one item {@code yearofice:note} and its component {@code yearofice:note_id} (RECONCILE 79): every keepsake is a note with an id. */
public final class KeepsakeRegistry {
    private KeepsakeRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> NOTE_ID =
            COMPONENTS.registerComponentType("note_id", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final DeferredItem<KeepsakeItem> NOTE = ITEMS.registerItem("note", KeepsakeItem::new, new Item.Properties().stacksTo(1).fireResistant());

    public static ItemStack stack(String id) {
        ItemStack s = new ItemStack(NOTE.get());
        s.set(NOTE_ID.get(), id.contains(":") ? id.substring(id.indexOf(':') + 1) : id);
        return s;
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
    }
}
