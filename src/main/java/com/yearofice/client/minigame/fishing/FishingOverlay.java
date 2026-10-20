package com.yearofice.client.minigame.fishing;

import com.yearofice.sim.fishing.FightModel;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * The fight overlay in the {@code minigame} layer (Blueprint › Fishing methods › What the player sees): a thin arc
 * around the crosshair shows tension from 0 to 100% of the line's strength now, white under 70%, amber to 90%, red
 * above, flashing on each spike. Slack turns it grey, with one tick filling per second toward the spit. There is no
 * stamina bar; "18 / 30 m" shows under the arc only when more than 75% of the line is out, and a keycap prompt
 * "Grab" shows when a tired fish is in reach. With the assist on, the safe band is shaded.
 */
public final class FishingOverlay implements LayeredDraw.Layer {
    static final int RADIUS = 13;
    static final double START_DEG = 135, SWEEP_DEG = 270;
    static final int WHITE = 0xFFF2EEE6, AMBER = 0xFFE0A030, RED = 0xFFD8402C, GREY = 0xFF7A7A78, TRACK = 0x553A3A38,
            SAFE = 0x4466C070, FLASH = 0xFFFFFFFF;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        FishingClient.Fight f = FishingClient.current();
        Minecraft mc = Minecraft.getInstance();
        if (f == null || mc.options.hideGui) return;
        FightModel.Params p = f.params;
        FightModel.State s = f.state;
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        double share = Math.max(0, Math.min(1, s.tensionShare(p)));
        boolean slack = s.tensionKg() < FightModel.SPIT_UNDER * p.sEff();
        boolean spike = s.spikeLeftS() > 0;
        int color = slack ? GREY : spike ? FLASH : share < 0.7 ? WHITE : share < FightModel.RED_ABOVE ? AMBER : RED;

        // The fish on the line, drawn per species (original art), shaking on each spike.
        var art = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.yearofice.core.YearOfIce.MOD_ID,
                "textures/gui/fish/" + p.species().id() + ".png");
        int shake = spike ? (int) ((Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) % 2) * 2 - 1 : 0;
        g.blit(art, cx - 24 + shake, cy - RADIUS - 30, 0, 0, 48, 24, 48, 24);

        arc(g, cx, cy, 0, 1, TRACK);
        if (p.assist()) {
            double lo = Math.min(0.7, FightModel.RECOVER_PRESSURE_BELOW * p.fullPull() / p.sEff());
            arc(g, cx, cy, lo, 0.7, SAFE);
        }
        if (!slack) arc(g, cx, cy, 0, share, color);

        Font font = mc.font;
        if (slack) {
            double spitS = p.tackle().gorge() ? FightModel.SPIT_S_GORGE : FightModel.SPIT_S;
            int ticks = (int) Math.ceil(spitS);
            int filled = (int) Math.min(ticks, Math.floor(s.slackS()));
            for (int i = 0; i < ticks; i++) {
                int x = cx - ticks * 3 + i * 6 + 1;
                g.fill(x, cy + RADIUS + 4, x + 4, cy + RADIUS + 6, i < filled ? WHITE : TRACK);
            }
        }
        int y = cy + RADIUS + 9;
        double carried = p.tackle().lineCarriedM();
        if (s.lineOutM() > 0.75 * carried) {
            String m = String.format(Locale.ROOT, "%.0f / %.0f m", s.lineOutM(), carried);
            g.drawString(font, m, cx - font.width(m) / 2, y, WHITE, true);
            y += 10;
        }
        double reach = p.landingNet() ? FightModel.NET_REACH_M : FightModel.LAND_REACH_M;
        if (s.phase() == FightModel.Phase.TIRED && s.lineOutM() <= reach && f.result == null) {
            String key = mc.options.keyUse.getTranslatedKeyMessage().getString();
            String m = "[" + key + "] Grab";
            g.drawString(font, m, cx - font.width(m) / 2, y, WHITE, true);
        }
    }

    /** Dots along the arc from {@code from} to {@code to}, as shares of the sweep. */
    static void arc(GuiGraphics g, int cx, int cy, double from, double to, int color) {
        if (to <= from) return;
        int steps = 90;
        for (int i = (int) Math.floor(from * steps); i <= (int) Math.ceil(to * steps) && i <= steps; i++) {
            double a = Math.toRadians(START_DEG + SWEEP_DEG * i / steps);
            int x = cx + (int) Math.round(Math.cos(a) * RADIUS);
            int y = cy + (int) Math.round(Math.sin(a) * RADIUS);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }
}
