package com.yearofice.build.wood;

import com.yearofice.sim.fire.Seasoning;
import com.yearofice.sim.wood.WoodKind;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A {@code <wood>_round} or {@code <wood>_split}: 3.75 kg, stacking to 4 (one armload). Its moisture is the shared
 * {@code yearofice:moisture} component; {@code yearofice:seasoning} keeps the exact figure and the day it was
 * settled, so B4's fractional daily rates add up ({@link WoodItems#settle}).
 */
public class WoodPieceItem extends Item {
    public final WoodKind wood;
    public final Seasoning.Form form;

    public WoodPieceItem(WoodKind wood, Seasoning.Form form, Properties p) {
        super(p);
        this.wood = wood;
        this.form = form;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        int m = WoodItems.moisturePct(stack);
        String band = m >= 50 ? "green" : m >= 30 ? "wet" : m >= Seasoning.SEASONED_PCT ? "part_dry" : "seasoned";
        lines.add(Component.translatable("item.yearofice.wood.moisture." + band).withStyle(ChatFormatting.GRAY));
    }
}
