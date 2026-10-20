package com.yearofice.build.craft;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The stacks a player holds, as the recipe type's input. */
public record StationInput(List<ItemStack> stacks) implements RecipeInput {
    @Override public ItemStack getItem(int i) { return stacks.get(i); }

    @Override public int size() { return stacks.size(); }
}
