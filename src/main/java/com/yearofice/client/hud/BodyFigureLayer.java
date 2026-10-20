package com.yearofice.client.hud;

import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.client.ui.messenger.MessengerSmoke;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * {@code yearofice:body_figure}: the 24 x 48 outline, top left, drawn from the server's {@code part_hp},
 * {@code part_frost}, lost digits and crooked bits (server decides, client shows). Colour is never the only cue:
 * a frozen part is vermilion with diagonal hatching, a hurt part is amber with a crack, a lost digit is a gap in
 * the hand or foot, a crooked limb is drawn off the straight. It shows only when a part needs attention.
 */
public final class BodyFigureLayer implements LayeredDraw.Layer {
    /** Part ordinals: head, torso, L arm, R arm, L hand, R hand, L leg, R leg, L foot, R foot. */
    static final int[][] BOX = {
            {8, 0, 8, 8}, {6, 9, 12, 16}, {1, 9, 5, 12}, {19, 9, 5, 12}, {0, 22, 5, 5}, {19, 22, 5, 5},
            {7, 26, 5, 14}, {12, 26, 5, 14}, {6, 41, 6, 6}, {12, 41, 6, 6}};
    static final int OK = 0xC0F2EEE6, HURT = 0xC0E0A030, BAD = 0xC0D8402C, COLD = 0xC07AA0D8;

    /** True when any part is off enough to draw the figure. */
    public static boolean needed(BodySyncPayload p) {
        BodySyncPayload.Parts q = p.parts();
        if (q.fingers() != 0 || q.toes() != 0 || q.crooked() != 0 || q.bleeding()) return true;
        for (int i = 0; i < BodySyncPayload.Parts.N; i++) if (q.hp()[i] < 100 || q.frost()[i] > 0) return true;
        return false;
    }

    /** The fill of one part: hp below 40 vermilion, below 80 amber, frost cold blue, else the plain outline. */
    public static int fill(BodySyncPayload.Parts q, int part) {
        if (q.frost()[part] >= 2) return BAD;
        if (q.hp()[part] < 40) return BAD;
        if (q.frost()[part] > 0) return COLD;
        if (q.hp()[part] < 80) return HURT;
        return OK;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker d) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || StackLayers.batteryDark()) return;
        BodySyncPayload p = HudDebug.on() ? MessengerSmoke.sample() : ClientBody.get();
        if (p == null || !needed(p)) return;
        draw(g, p.parts(), 4, 4);
    }

    /** Draws the figure with its top-left at {@code x, y}. */
    public static void draw(GuiGraphics g, BodySyncPayload.Parts q, int x, int y) {
        for (int i = 0; i < BOX.length; i++) {
            int[] b = BOX[i];
            int bx = x + b[0] + ((q.crooked() >> i) & 1) * 2;
            int by = y + b[1];
            int c = fill(q, i);
            g.fill(bx, by, bx + b[2], by + b[3], c);
            if (c == BAD || q.frost()[i] >= 2) {
                for (int k = 0; k < b[2] + b[3]; k += 3) {
                    int hx = bx + Math.min(k, b[2] - 1), hy = by + Math.max(0, k - b[2] + 1);
                    if (hy < by + b[3]) g.fill(hx, hy, hx + 1, hy + 1, 0xFF101418);
                }
            } else if (c == HURT) {
                g.fill(bx + b[2] / 2, by + 1, bx + b[2] / 2 + 1, by + b[3] - 1, 0xFF101418);
            } else if (c == COLD) {
                g.fill(bx + b[2] / 2 - 1, by + b[3] / 2, bx + b[2] / 2 + 2, by + b[3] / 2 + 1, 0xFFF2EEE6);
            }
        }
        // Lost digits: a dark notch on each hand and foot per digit lost.
        notch(g, x, y, 4, q.fingers() & 0x1F);
        notch(g, x, y, 5, (q.fingers() >> 5) & 0x1F);
        notch(g, x, y, 8, q.toes() & 0x1F);
        notch(g, x, y, 9, (q.toes() >> 5) & 0x1F);
        if (q.bleeding()) g.fill(x + 10, y + 12, x + 12, y + 15, BAD);
    }

    private static void notch(GuiGraphics g, int x, int y, int part, int mask) {
        int n = Integer.bitCount(mask);
        if (n == 0) return;
        int[] b = BOX[part];
        int bottom = y + b[1] + b[3];
        for (int k = 0; k < Math.min(n, b[2]); k++) g.fill(x + b[0] + k, bottom - 2, x + b[0] + k + 1, bottom, 0xFF101418);
    }
}
