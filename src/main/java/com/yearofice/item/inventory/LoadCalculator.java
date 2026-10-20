package com.yearofice.item.inventory;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.item.LoadService;
import com.yearofice.item.YoiDataMaps;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.pack.LoadBands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * {@code item.inventory.LoadCalculator} (Layer 6, Load): the Item model's weight summed over every slot. One item
 * weighs its {@code yearofice:weight_g} component, else its {@code weight.json} entry, else
 * {@code [pack] default_item_g}. A pack in a hand or the grid adds its packed contents; the worn pack's rows are
 * already in the grid, so it counts only itself. Common code: the client's load meter runs the same sum.
 */
public final class LoadCalculator implements LoadService {

    /** Grams of one item of {@code stack}. */
    public static double unitG(ItemStack stack) {
        Integer g = stack.get(YoiComponents.WEIGHT_G.get());
        if (g != null) return g;
        return YoiDataMaps.mapWeightG(stack).map(Integer::doubleValue).orElseGet(() -> PackConfig.get("default_item_g"));
    }

    /** Kilograms of the whole stack, with a pack's packed contents unless it is worn. */
    public static double stackKg(ItemStack stack, boolean withContents) {
        if (stack.isEmpty()) return 0;
        double g = stack.getCount() * unitG(stack);
        if (withContents && PackRules.isPack(stack)) {
            ItemContainerContents c = stack.get(DataComponents.CONTAINER);
            if (c != null) for (ItemStack in : c.nonEmptyItems()) g += in.getCount() * unitG(in);
        }
        return g / 1000.0;
    }

    public static double kgOf(Player p) {
        double kg = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) kg += stackKg(inv.getItem(i), true);
        var curios = CuriosApi.getCuriosInventory(p);
        if (curios.isPresent()) {
            ItemStack back = PackSlots.back(p);
            var all = curios.get().getEquippedCurios();
            for (int i = 0; i < all.getSlots(); i++) {
                ItemStack s = all.getStackInSlot(i);
                kg += stackKg(s, s != back);
            }
        }
        return kg;
    }

    public static double maxKgOf(Player p) {
        YoiDataMaps.Pack pack = YoiDataMaps.pack(PackSlots.back(p));
        if (pack == null) return PackConfig.get("no_pack_kg");
        return p instanceof ServerPlayer sp ? LoadBands.usableMaxKg(pack.maxKg(), pack.minStrength(), strength(sp)) : pack.maxKg();
    }

    static double strength(ServerPlayer p) {
        try {
            return BodyService.get().strength(p);
        } catch (RuntimeException e) {
            return 100;
        }
    }

    /** The worn pack's {@code min_strength} (0 with none). */
    public static double minStrengthOf(Player p) {
        YoiDataMaps.Pack pack = YoiDataMaps.pack(PackSlots.back(p));
        return pack == null ? 0 : pack.minStrength();
    }

    /** The band for a carried {@code kg}: Strength, Hauling practice and the worn pack's minimum Strength. */
    public static LoadBands.Band bandOf(ServerPlayer p, double kg) {
        return PackConfig.bands().band(kg, strength(p), PackSkill.reps(p), minStrengthOf(p));
    }

    @Override public double kg(Player player) { return kgOf(player); }

    @Override public double maxKg(Player player) { return maxKgOf(player); }

    @Override public int openRows(Player player) { return PackRules.openRows(player); }

    @Override
    public LoadBands.Band band(ServerPlayer player) { return bandOf(player, kgOf(player)); }

    @Override
    public double kcalFactor(ServerPlayer player) { return PackConfig.bands().kcal(band(player)); }
}
