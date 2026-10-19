package com.yearofice.item.inventory;

import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** The Curios {@code back} and {@code neck} slots (M2; RECONCILE 162), read the same way on both sides. */
public final class PackSlots {
    private PackSlots() {}

    public static final String BACK = "back", NECK = "neck";

    static Optional<ICurioStacksHandler> handler(Player p, String slot) {
        return CuriosApi.getCuriosInventory(p).flatMap(i -> i.getStacksHandler(slot));
    }

    /** The stack in the {@code back} slot, or empty. */
    public static ItemStack back(Player p) {
        return handler(p, BACK).map(h -> h.getStacks().getSlots() > 0 ? h.getStacks().getStackInSlot(0) : ItemStack.EMPTY).orElse(ItemStack.EMPTY);
    }

    /** Puts {@code stack} in the {@code back} slot; false if the player has no such slot. */
    public static boolean setBack(Player p, ItemStack stack) { return set(p, BACK, 0, stack); }

    public static boolean set(Player p, String slot, int index, ItemStack stack) {
        Optional<ICurioStacksHandler> h = handler(p, slot);
        if (h.isEmpty() || h.get().getStacks().getSlots() <= index) return false;
        h.get().getStacks().setStackInSlot(index, stack);
        return true;
    }

    public static ItemStack get(Player p, String slot, int index) {
        return handler(p, slot).map(h -> h.getStacks().getSlots() > index ? h.getStacks().getStackInSlot(index) : ItemStack.EMPTY)
                .orElse(ItemStack.EMPTY);
    }

    public static int size(Player p, String slot) { return handler(p, slot).map(h -> h.getStacks().getSlots()).orElse(0); }
}
