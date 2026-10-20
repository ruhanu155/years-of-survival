package com.yearofice.crop;

import com.yearofice.api.clock.ClockService;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.crop.Milling;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Hand threshing and milling (Crop farming in play): the flail in the main hand and a grain sheaf in the off hand
 * threshes it into grain and straw; the hand quern and a stack of grain (barley or wheat) mills it into flour at
 * 1 kg an hour. Both take game time: the clock advances by the work, which the body's activity sampler charges.
 * Grain carries its harvest year as {@code yearofice:seed_lot}, which ages the seed.
 */
public final class Grain {
    private Grain() {}

    /** Grams a stack weighs: its {@code weight_g} component times nothing else (a harvest is one item), or 100 g an item without one. */
    public static int grams(ItemStack s) {
        Integer w = s.get(YoiComponents.WEIGHT_G.get());
        return w != null ? w : 100 * s.getCount();
    }

    /** Years the seed lot in {@code s} has aged; 0 without the component. */
    public static int lotYears(ItemStack s) {
        Integer y = s.get(CropRegistry.SEED_LOT.get());
        return y == null ? 0 : Milling.lotYears(y, ClockService.get().year());
    }

    static ItemStack weighed(net.minecraft.world.item.Item item, int g) {
        ItemStack out = new ItemStack(item);
        out.set(YoiComponents.WEIGHT_G.get(), Math.max(1, g));
        return out;
    }

    /** Does whichever of the two jobs the main-hand tool does; null when the player holds neither tool. */
    public static @Nullable String use(ServerPlayer p) {
        ItemStack tool = p.getMainHandItem(), off = p.getOffhandItem();
        if (tool.is(CropRegistry.FLAIL.get())) return thresh(p, tool, off);
        if (tool.is(CropRegistry.HAND_QUERN.get())) return mill(p, off);
        return null;
    }

    static String thresh(ServerPlayer p, ItemStack flail, ItemStack off) {
        if (!off.is(CropRegistry.GRAIN_SHEAF.get())) return "Hold a grain sheaf in your other hand to thresh it.";
        int sheafG = off.getCount() * (off.get(YoiComponents.WEIGHT_G.get()) != null ? off.get(YoiComponents.WEIGHT_G.get()) : 1000);
        int grain = Milling.grainG(sheafG), straw = Milling.strawG(sheafG), min = Milling.threshMinutes(sheafG);
        off.shrink(off.getCount());
        ItemStack g = weighed(CropRegistry.BARLEY.get(), grain);
        g.set(CropRegistry.SEED_LOT.get(), ClockService.get().year());
        give(p, g);
        give(p, weighed(CropRegistry.STRAW.get(), straw));
        flail.hurtAndBreak(1, p, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        ClockService.get().advance(min);
        return "Threshed " + sheafG / 1000.0 + " kg of sheaf into " + grain + " g of grain and " + straw + " g of straw in " + min + " min.";
    }

    static String mill(ServerPlayer p, ItemStack grain) {
        if (!grain.is(CropRegistry.BARLEY.get()) && !grain.is(CropRegistry.WHEAT.get())) return "Hold grain in your other hand to grind it.";
        int g = grams(grain), min = Milling.millMinutes(g);
        grain.shrink(grain.getCount());
        give(p, weighed(CropRegistry.BARLEY_FLOUR.get(), g));
        ClockService.get().advance(min);
        return "Ground " + g + " g of grain into flour in " + min + " min.";
    }

    static void give(ServerPlayer p, ItemStack s) { if (!p.getInventory().add(s)) p.drop(s, false); }
}
