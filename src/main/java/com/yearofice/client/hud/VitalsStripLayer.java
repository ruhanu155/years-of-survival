package com.yearofice.client.hud;

import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.core.YearOfIce;
import com.yearofice.client.ui.messenger.Pal;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code yearofice:vitals_strip}, top right: 9 × 9 need icons, right to left, each shown only once its need passes
 * the first warning, so the strip is empty when all is well (Blueprint › What the HUD shows). The slice's icons:
 * Warmth (below 70), Wet (above 30%), Water (below 60), Calories (24 h balance below −1,000), Energy (below 5)
 * and Condition (below 60). Each glyph has its own shape, so colour is never the only cue. The strip goes dark
 * with no messenger carried or its battery at 0, and hides with F1.
 */
public final class VitalsStripLayer implements LayeredDraw.Layer {
    private static ResourceLocation sprite(String n) {
        return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "hud/" + n);
    }

    static final ResourceLocation WARMTH = sprite("warmth"), WET = sprite("wet"), WATER = sprite("water"),
            CALORIES = sprite("calories"), ENERGY = sprite("energy"), CONDITION = sprite("condition");

    /** Pixels between icons and from the screen edge. */
    static final int SIZE = 9, GAP = 3, EDGE = 4;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        BodySyncPayload p = ClientBody.get();
        if (p == null || p.battery() <= 0) return;
        int x = g.guiWidth() - EDGE - SIZE;
        int y = EDGE;
        boolean numbers = false; // "Numbers on the strip" option, off by default (Accessibility, later)
        x = icon(g, x, y, ClientBody.warmthWarn(p), WARMTH, Math.round(p.warmth()), numbers);
        x = icon(g, x, y, ClientBody.wetWarn(p), WET, Math.round(p.wetness() * 100), numbers);
        x = icon(g, x, y, ClientBody.waterWarn(p), WATER, Math.round(p.water()), numbers);
        x = icon(g, x, y, ClientBody.caloriesWarn(p), CALORIES, Math.round(p.balance24h()), numbers);
        x = icon(g, x, y, ClientBody.energyWarn(p), ENERGY, Math.round(p.energy()), numbers);
        icon(g, x, y, ClientBody.conditionWarn(p), CONDITION, Math.round(p.condition()), numbers);
    }

    private static int icon(GuiGraphics g, int x, int y, boolean show, ResourceLocation s, long value, boolean numbers) {
        if (!show) return x;
        g.blitSprite(s, x, y, SIZE, SIZE);
        if (numbers) {
            String v = Long.toString(value);
            var font = Minecraft.getInstance().font;
            g.drawString(font, v, x + SIZE / 2 - font.width(v) / 2, y + SIZE + 2, Pal.SNOW, true);
        }
        return x - SIZE - GAP;
    }
}
