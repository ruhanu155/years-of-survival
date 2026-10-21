package com.yearofice.body.clothing;

import com.yearofice.api.body.ClothingService;
import com.yearofice.item.inventory.PackSlots;
import com.yearofice.sim.body.Garment;
import com.yearofice.sim.body.Layer;
import com.yearofice.sim.body.WornGarment;
import com.yearofice.sim.body.Zone;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Where each clothing slot lives (vanilla armor for outer head/torso/legs/feet, Curios for the rest) and the reader over them. */
public final class ClothingSlots implements ClothingService {
    public static final ClothingSlots INSTANCE = new ClothingSlots();

    private ClothingSlots() {}

    /** The vanilla armor slot for a zone and layer, or null when the slot is a Curios slot. */
    public static EquipmentSlot armor(Zone z, Layer l) {
        if (l != Layer.OUTER) return null;
        return switch (z) {
            case HEAD -> EquipmentSlot.HEAD;
            case TORSO -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            case HANDS -> null;
        };
    }

    /** The Curios slot id ({@code hands_outer}, {@code torso_inner} ...) for a non-armor slot. */
    public static String curios(Zone z, Layer l) { return z.id() + "_" + l.name().toLowerCase(java.util.Locale.ROOT); }

    public static WearMap.Wear row(ItemStack st) {
        return st.isEmpty() ? null : BuiltInRegistries.ITEM.wrapAsHolder(st.getItem()).getData(WearMap.WEAR);
    }

    public static ItemStack stackAt(Player p, Zone z, Layer l) {
        EquipmentSlot a = armor(z, l);
        return a != null ? p.getItemBySlot(a) : PackSlots.get(p, curios(z, l), 0);
    }

    /** Puts {@code st} in the slot if empty; true when placed. */
    public static boolean put(Player p, Zone z, Layer l, ItemStack st) {
        if (!stackAt(p, z, l).isEmpty()) return false;
        EquipmentSlot a = armor(z, l);
        if (a != null) {
            p.setItemSlot(a, st);
            return true;
        }
        return PackSlots.set(p, curios(z, l), 0, st);
    }

    @Override
    public List<WornGarment> worn(ServerPlayer p) {
        List<WornGarment> out = new ArrayList<>(Layer.SLOTS);
        for (Zone z : Zone.values()) {
            for (Layer l : Layer.values()) {
                ItemStack st = stackAt(p, z, l);
                WearMap.Wear w = row(st);
                if (w == null) continue;
                Garment g = w.garment(BuiltInRegistries.ITEM.getKey(st.getItem()).toString());
                if (g.zone() != z || g.layer() != l) continue; // an item in the wrong slot does nothing
                double cond = st.isDamageableItem() ? 1.0 - (double) st.getDamageValue() / st.getMaxDamage() : 1.0;
                out.add(new WornGarment(g, cond));
            }
        }
        return out;
    }
}
