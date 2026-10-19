package com.yearofice.client.hud;

import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * 7. {@code yearofice:load_meter}, right of the hotbar: "22 / 30 kg" against the worn pack's limit, amber from
 * 15 kg and red from 30 kg (Blueprint › Inventory and backpack). The reading comes from {@link #reading}, which the
 * pack's client half sets; with no reading it draws nothing.
 */
public final class LoadMeterLayer implements LayeredDraw.Layer {
    /** What the meter shows: carried kg and the pack's limit. */
    public record Reading(double kg, double maxKg) {}

    public static volatile java.util.function.Supplier<Reading> reading = () -> null;

    public static String text(Reading r) {
        return String.format(Locale.ROOT, "%.0f / %.0f kg", r.kg(), r.maxKg());
    }

    public static int color(double kg) { return kg >= 30 ? StackLayers.RED : kg >= 15 ? StackLayers.AMBER : StackLayers.INK; }

    @Override
    public void render(GuiGraphics g, DeltaTracker d) {
        if (StackLayers.hudHidden()) return;
        Reading r = HudDebug.on() ? new Reading(22, 30) : reading.get();
        if (r == null) return;
        int x = g.guiWidth() / 2 + 91 + 8, y = g.guiHeight() - 15;
        // A small pack glyph, then the reading.
        g.fill(x, y - 1, x + 7, y + 8, 0xFF8A6A44);
        g.fill(x + 1, y - 3, x + 6, y - 1, 0xFF6A4E32);
        g.drawString(StackLayers.mc().font, Component.literal(text(r)), x + 10, y, color(r.kg()), true);
    }
}
