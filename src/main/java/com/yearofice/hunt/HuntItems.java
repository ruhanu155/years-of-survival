package com.yearofice.hunt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Stack helpers: an arrow carries its head's id in custom data (data {@code arrowhead/<id>.json}). */
public final class HuntItems {
    private HuntItems() {}

    public static final String DEFAULT_HEAD = "knapped_stone";

    public static ItemStack arrow(String head, int count) {
        ItemStack s = new ItemStack(HuntRegistry.ARROW.get(), count);
        s.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY.update(t -> t.putString("head", head)));
        return s;
    }

    public static String headOf(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null) return DEFAULT_HEAD;
        String h = d.copyTag().getString("head");
        return h.isEmpty() ? DEFAULT_HEAD : h;
    }
}
