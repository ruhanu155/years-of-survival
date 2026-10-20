package com.yearofice.client.injury;

import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.body.net.TreatPayload;
import com.yearofice.client.hud.ClientBody;
import com.yearofice.sim.body.part.Part;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The self-care radial (Body tab › Sneak and use opens the self-care radial): the ten parts around a small figure,
 * lit where something is wrong, "All" at the centre. Hold the mouse on a part to treat it, let go to stop; the
 * server picks the treatment, runs the hold and spends the item. The client only shows and asks.
 */
public final class TreatRadial extends Screen {
    private static final int R = 62, W = 54, H = 14;
    private int held = Integer.MIN_VALUE;

    public TreatRadial() { super(Component.translatable("screen.yearofice.treat")); }

    @Override
    public boolean isPauseScreen() { return false; }

    private int cx() { return width / 2; }

    private int cy() { return height / 2; }

    /** Centre of slot {@code i}: parts 0..9 on a ring, starting at the top. */
    private int[] slot(int i) {
        double a = -Math.PI / 2 + i * 2 * Math.PI / Part.COUNT;
        return new int[] {cx() + (int) Math.round(Math.cos(a) * R * 1.35), cy() + (int) Math.round(Math.sin(a) * R)};
    }

    private int hit(double mx, double my) {
        for (int i = 0; i < Part.COUNT; i++) {
            int[] c = slot(i);
            if (Math.abs(mx - c[0]) <= W / 2.0 && Math.abs(my - c[1]) <= H / 2.0) return i;
        }
        return Math.abs(mx - cx()) <= 20 && Math.abs(my - cy()) <= 8 ? TreatPayload.ALL : Integer.MIN_VALUE;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        BodySyncPayload p = ClientBody.get();
        g.drawCenteredString(font, title, cx(), cy() - R - 24, 0xF2EEE6);
        int over = hit(mx, my);
        for (int i = 0; i < Part.COUNT; i++) {
            int[] c = slot(i);
            Part part = Part.values()[i];
            int hp = p == null || p.parts() == null ? 100 : p.parts().hp()[i];
            int col = hp >= 100 ? 0x70404040 : hp >= 75 ? 0xC0A07020 : 0xC0A02C20;
            if (over == i || held == i) col |= 0x00404040;
            g.fill(c[0] - W / 2, c[1] - H / 2, c[0] + W / 2, c[1] + H / 2, col);
            String name = part.id().replace('_', ' ');
            g.drawCenteredString(font, name, c[0], c[1] - 4, 0xF2EEE6);
        }
        g.fill(cx() - 20, cy() - 8, cx() + 20, cy() + 8, over == TreatPayload.ALL || held == TreatPayload.ALL ? 0xC0508050 : 0x90305030);
        g.drawCenteredString(font, Component.translatable("screen.yearofice.treat.all"), cx(), cy() - 4, 0xF2EEE6);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int h = hit(mx, my);
        if (button == 0 && h != Integer.MIN_VALUE) {
            held = h;
            PacketDistributor.sendToServer(new TreatPayload(h));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && held != Integer.MIN_VALUE) {
            held = Integer.MIN_VALUE;
            PacketDistributor.sendToServer(new TreatPayload(TreatPayload.CANCEL));
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void removed() {
        if (held != Integer.MIN_VALUE) PacketDistributor.sendToServer(new TreatPayload(TreatPayload.CANCEL));
    }
}
