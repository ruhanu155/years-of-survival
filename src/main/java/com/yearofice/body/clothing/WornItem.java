package com.yearofice.body.clothing;

import com.yearofice.sim.body.Layer;
import com.yearofice.sim.body.Zone;
import java.util.Locale;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A garment: its slot comes from its {@code yearofice:wear} row. Right-click puts it on (armor slot or its Curios slot). */
public class WornItem extends Item implements Equipable {
    public WornItem(Properties p) { super(p.stacksTo(1)); }

    private WearMap.Wear row(ItemStack st) { return ClothingSlots.row(st); }

    private static Zone zone(WearMap.Wear w) { return Zone.valueOf(w.zone().toUpperCase(Locale.ROOT)); }

    private static Layer layer(WearMap.Wear w) { return Layer.valueOf(w.layer().toUpperCase(Locale.ROOT)); }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        var w = net.minecraft.core.registries.BuiltInRegistries.ITEM.wrapAsHolder(this).getData(WearMap.WEAR);
        EquipmentSlot a = w == null ? null : ClothingSlots.armor(zone(w), layer(w));
        return a == null ? EquipmentSlot.MAINHAND : a; // MAINHAND = a Curios garment, not an armor piece
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (getEquipmentSlot() != EquipmentSlot.MAINHAND) return swapWithEquipmentSlot(this, level, player, hand);
        WearMap.Wear w = row(st);
        if (w == null || !ClothingSlots.stackAt(player, zone(w), layer(w)).isEmpty()) return InteractionResultHolder.pass(st);
        if (!level.isClientSide && !ClothingSlots.put(player, zone(w), layer(w), st.copyWithCount(1))) return InteractionResultHolder.pass(st);
        if (!level.isClientSide) st.shrink(1);
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }
}
