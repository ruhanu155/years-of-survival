package com.yearofice.food.pan;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** What the pan works on, checked on both sides (the client asks it before it sends a hold). */
public final class PanTargets {
    private PanTargets() {}

    public static boolean isPan(ItemStack s) { return s.is(PanRegistry.GOLD_PAN.get()) || s.is(PanRegistry.BIRCH_PAN.get()); }

    /** Placer gravel, or any gravel. */
    public static boolean isGravel(BlockState s) { return s.is(PanRegistry.PLACER_GRAVEL.get()) || s.is(Blocks.GRAVEL); }
}
