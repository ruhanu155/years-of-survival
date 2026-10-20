package com.yearofice.client.hud;

import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 16. {@code yearofice:field_note_card}, bottom left: a small paper card with the real fact, a rule, and the rule's
 * game line (Creative concepts › Field-note cards). Fades in and out over a quarter second.
 */
public final class FieldNoteCardLayer implements LayeredDraw.Layer {
    static final int W = 156, PAD = 5;
    static final int PAPER = 0xF0E4DCC2, EDGE = 0xFF7A6A4A, INK = 0xFF2B2A26, HEAD = 0xFF8A5A2A;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        String id = FieldNoteClient.current();
        if (HudDebug.on() && id == null) id = "yearofice:needle_tea";
        if (id == null || mc.options.hideGui || mc.player == null || mc.screen != null) return;
        // The data files live on the server; the lang keys follow the id: field_note.<namespace>.<path>.fact / .rule.
        String base = "field_note." + id.replace(':', '.');
        String factKey = base + ".fact", ruleKey = base + ".rule";
        Font f = mc.font;
        List<FormattedCharSequence> fact = f.split(Component.translatable(factKey), W - 2 * PAD);
        List<FormattedCharSequence> rule = f.split(Component.translatable(ruleKey), W - 2 * PAD);
        int head = 11, h = PAD * 2 + head + (fact.size() + rule.size()) * 9 + 4;
        long left = FieldNoteClient.remainingMs();
        float a = HudDebug.on() ? 1f : (float) Math.min(1.0, Math.min(left, FieldNoteClient.SHOW_MS - left) / 250.0 + 0.0);
        if (a <= 0.02f) return;
        int al = (int) (a * 255) << 24;
        int x = 6, y = g.guiHeight() - 30 - h;
        if (MessengerCornerLayer.occupiesLeft(mc)) y -= MessengerCornerLayer.H + 6;
        g.fill(x - 1, y - 1, x + W + 1, y + h + 1, (EDGE & 0xFFFFFF) | al);
        g.fill(x, y, x + W, y + h, (PAPER & 0xFFFFFF) | ((int) (a * 0xF0) << 24));
        g.drawString(f, Component.translatable("field_note.yearofice.title"), x + PAD, y + PAD, (HEAD & 0xFFFFFF) | al, false);
        int ty = y + PAD + head;
        for (FormattedCharSequence s : fact) { g.drawString(f, s, x + PAD, ty, (INK & 0xFFFFFF) | al, false); ty += 9; }
        ty += 4;
        for (FormattedCharSequence s : rule) { g.drawString(f, s, x + PAD, ty, (HEAD & 0xFFFFFF) | al, false); ty += 9; }
    }
}
