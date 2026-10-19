package com.yearofice.client.ui.messenger;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A text cursor over the messenger's LCD: rows of 10 GUI pixels, left and right aligned text, bars and the bolt
 * row. Pages draw only through this, so the device layout lives in one place.
 */
public final class Lcd {
    public static final int ROW = 10;

    private final GuiGraphics g;
    private final Font font;
    public final int x, y, w, h;
    private int cy;

    public Lcd(GuiGraphics g, Font font, int x, int y, int w, int h) {
        this.g = g;
        this.font = font;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.cy = y;
    }

    public int cursor() { return cy; }

    public void gap(int px) { cy += px; }

    public void row() { cy += ROW; }

    public void text(Component c, int color) { g.drawString(font, c, x, cy, color, false); }

    public void text(String s, int color) { g.drawString(font, s, x, cy, color, false); }

    public void textAt(int dx, String s, int color) { g.drawString(font, s, x + dx, cy, color, false); }

    public void textAt(int dx, Component c, int color) { g.drawString(font, c, x + dx, cy, color, false); }

    public void right(String s, int color) { g.drawString(font, s, x + w - font.width(s), cy, color, false); }

    public void right(Component c, int color) { g.drawString(font, c, x + w - font.width(c), cy, color, false); }

    public void line(Component left, int lc, String right, int rc) {
        text(left, lc);
        right(right, rc);
        row();
    }

    /** A horizontal rule across the LCD. */
    public void rule() {
        g.fill(x, cy + 1, x + w, cy + 2, Pal.DIM);
        cy += 4;
    }

    /** A bar of {@code share} (0–1) from {@code dx} to the right edge, 6 px tall, on the current row. */
    public void bar(int dx, double share, int color) {
        int bx = x + dx, bw = w - dx, by = cy + 1;
        g.fill(bx, by, bx + bw, by + 6, Pal.TROUGH);
        int fw = (int) Math.round(Math.max(0, Math.min(1, share)) * bw);
        if (fw > 0) g.fill(bx, by, bx + fw, by + 6, color);
        g.fill(bx, by + 6, bx + bw, by + 7, Pal.DIM);
    }

    /**
     * The BMI bar from 15 to 27: band colours, the SOS mark at 17 and a pointer at {@code bmi}. Bands are also
     * told apart by the tick marks at 17, 18.5, 20 and 25, so colour is never the only cue.
     */
    public void bmiBar(double bmi) {
        double lo = 15, hi = 27;
        int bx = x, bw = w, by = cy + 1;
        double[] edges = {lo, 17, 18.5, 20, 25, hi};
        int[] colours = {Pal.RED, Pal.ORANGE, Pal.YELLOW, Pal.GREEN, Pal.BROWN};
        for (int i = 0; i < colours.length; i++) {
            int x0 = bx + (int) Math.round((edges[i] - lo) / (hi - lo) * bw);
            int x1 = bx + (int) Math.round((edges[i + 1] - lo) / (hi - lo) * bw);
            g.fill(x0, by + 2, x1, by + 6, colours[i]);
        }
        for (int i = 1; i < edges.length - 1; i++) {
            int tx = bx + (int) Math.round((edges[i] - lo) / (hi - lo) * bw);
            g.fill(tx, by, tx + 1, by + 8, Pal.INK);
        }
        int sos = bx + (int) Math.round((17 - lo) / (hi - lo) * bw);
        g.drawString(font, "SOS", sos - font.width("SOS") - 1, by + 9, Pal.RED, false);
        int px = bx + (int) Math.round((Math.max(lo, Math.min(hi, bmi)) - lo) / (hi - lo) * bw);
        g.fill(px - 1, by - 1, px + 2, by + 9, Pal.INK);
        g.fill(px, by, px + 1, by + 8, Pal.LCD_LIGHT);
        cy += ROW + 9;
    }

    /** Ten bolts: filled to {@code energy}; segments above the cap hatched. */
    public void bolts(int dx, double energy, double cap) {
        int n = 10, gapPx = 2;
        int seg = (w - dx - gapPx * (n - 1)) / n;
        for (int i = 0; i < n; i++) {
            int sx = x + dx + i * (seg + gapPx), sy = cy + 1;
            boolean capped = i >= Math.floor(cap + 1e-6);
            g.fill(sx, sy, sx + seg, sy + 7, Pal.TROUGH);
            double fill = Math.max(0, Math.min(1, energy - i));
            if (fill > 0) g.fill(sx, sy, sx + (int) Math.round(fill * seg), sy + 7, Pal.YELLOW);
            if (capped) {
                for (int k = 0; k < seg + 7; k += 3) {
                    int hx = sx + Math.min(seg - 1, k), hy = sy + Math.max(0, k - seg + 1);
                    g.fill(hx, hy, hx + 1, hy + 1, Pal.DIM);
                    int hx2 = sx + Math.max(0, k - 6), hy2 = sy + Math.min(6, k);
                    g.fill(hx2, hy2, hx2 + 1, hy2 + 1, Pal.DIM);
                }
            }
            g.fill(sx, sy + 7, sx + seg, sy + 8, Pal.DIM);
        }
    }
}
