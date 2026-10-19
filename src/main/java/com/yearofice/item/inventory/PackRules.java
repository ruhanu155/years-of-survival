package com.yearofice.item.inventory;

import com.yearofice.item.YoiDataMaps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * {@code item.inventory.PackRules}: which of the player's 41 slots are open. Slots 0–8 (the belt and pockets),
 * 36–39 (armor) and 40 (off-hand) always are; pack rows A, B and C (9–17, 18–26, 27–35) open by the worn pack's
 * {@code rows} in {@code pack.json}. A creative player has every row. Both sides read it the same way, from the
 * synced {@code back} slot and data map.
 */
public final class PackRules {
    private PackRules() {}

    public static final int ROW_START = 9, ROW_END = 36, ROW_LEN = 9;

    public static int openRows(Player p) {
        if (p.getAbilities().instabuild) return 3;
        YoiDataMaps.Pack pack = YoiDataMaps.pack(PackSlots.back(p));
        return pack == null ? 0 : pack.rows();
    }

    /** True when inventory slot {@code index} may hold items. */
    public static boolean isOpen(Player p, int index) {
        if (index < ROW_START || index >= ROW_END) return true;
        return (index - ROW_START) / ROW_LEN < openRows(p);
    }

    /** The row a slot belongs to: 0 for A, 1 for B, 2 for C, −1 for any other slot. */
    public static int row(int index) { return index < ROW_START || index >= ROW_END ? -1 : (index - ROW_START) / ROW_LEN; }

    public static boolean isPack(ItemStack s) { return YoiDataMaps.pack(s) != null; }
}
