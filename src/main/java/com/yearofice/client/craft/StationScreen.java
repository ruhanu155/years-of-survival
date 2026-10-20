package com.yearofice.client.craft;

import com.yearofice.build.craft.StationCraft;
import com.yearofice.build.craft.StationListPayload;
import com.yearofice.build.craft.StationPickPayload;
import com.yearofice.sim.build.Practice;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The station screen (Crafting and building in depth › Stations, Recipes): the recipes of the Hands and any workbench
 * in reach, each with its time and calories after practice, its Clumsy / Handy / Practiced word, what is held of each
 * input and what is wrong, if anything. Click a recipe to start it; the work runs at the Work rate while you stand
 * near. The server judged every row and judges the pick again; this only shows. It does not pause the world.
 */
public final class StationScreen extends Screen {
    private static final int ROW_H = 34, PANEL_W = 300, C_DARK = 0xE01A1512, C_FRAME = 0xFF45372B, C_TEXT = 0xFFE3DDD0, C_DIM = 0xFF9C9383,
            C_GREEN = 0xFF8DB27F, C_RED = 0xFFC9694F, C_AMBER = 0xFFEC9A2A, C_HOVER = 0x40FFFFFF;

    private final StationListPayload list;
    private int scroll;

    public StationScreen(StationListPayload list) {
        super(Component.translatable(list.hasBench() ? "station.yearofice.title_bench" : "station.yearofice.title_hands"));
        this.list = list;
    }

    @Override public boolean isPauseScreen() { return false; }

    private int left() { return (width - PANEL_W) / 2; }
    private int top() { return 28; }
    private int visibleRows() { return Math.max(1, (height - top() - 26) / ROW_H); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int l = left(), t = top();
        int rows = Math.min(visibleRows(), list.rows().size() - scroll);
        g.fill(l - 6, t - 22, l + PANEL_W + 6, t + Math.max(1, rows) * ROW_H + 8, C_DARK);
        g.renderOutline(l - 6, t - 22, PANEL_W + 12, Math.max(1, rows) * ROW_H + 30, C_FRAME);
        g.drawString(font, title, l, t - 15, C_TEXT, false);
        if (list.rows().isEmpty()) g.drawString(font, Component.translatable("station.yearofice.none"), l, t + 4, C_DIM, false);
        for (int i = 0; i < rows; i++) {
            StationListPayload.Row r = list.rows().get(scroll + i);
            int y = t + i * ROW_H;
            boolean ok = r.problem() == StationCraft.Problem.NONE.ordinal();
            if (ok && mx >= l && mx < l + PANEL_W && my >= y && my < y + ROW_H) g.fill(l - 2, y, l + PANEL_W + 2, y + ROW_H - 2, C_HOVER);
            g.renderItem(r.result(), l, y + 2);
            g.renderItemDecorations(font, r.result(), l, y + 2);
            g.drawString(font, r.result().getHoverName(), l + 22, y + 1, ok ? C_TEXT : C_DIM, false);
            Practice.Word w = Practice.Word.values()[Math.max(0, Math.min(2, r.word()))];
            g.drawString(font, Component.translatable("station.yearofice.time_kcal", fmtMin(r.minutes()), r.kcal(), w.text), l + 22, y + 11, C_DIM, false);
            g.drawString(font, needs(r), l + 22, y + 21, ok ? C_GREEN : C_RED, false);
        }
        if (list.rows().size() > rows) {
            g.drawString(font, Component.translatable("station.yearofice.scroll", scroll + 1, scroll + rows, list.rows().size()), l, t + rows * ROW_H, C_DIM, false);
        }
        super.render(g, mx, my, pt);
    }

    private Component needs(StationListPayload.Row r) {
        if (r.problem() != StationCraft.Problem.NONE.ordinal() && r.problem() != StationCraft.Problem.INPUTS.ordinal()
                && r.problem() != StationCraft.Problem.TOOL.ordinal()) {
            return Component.translatable("station.yearofice.problem." + StationCraft.Problem.values()[r.problem()].name().toLowerCase(Locale.ROOT));
        }
        var out = Component.empty();
        boolean first = true;
        for (StationListPayload.Need n : r.needs()) {
            if (!first) out.append(", ");
            first = false;
            out.append(n.have() + "/" + n.count() + " ").append(n.name());
        }
        if (!r.tool().getString().isEmpty()) {
            out.append(first ? "" : "; ").append(Component.translatable("station.yearofice.tool", r.tool()))
                    .append(r.toolOk() ? "" : " (" + Component.translatable("station.yearofice.missing").getString() + ")");
        }
        return out;
    }

    static String fmtMin(double min) {
        if (min >= 90) return String.format(Locale.ROOT, "%.1f h", min / 60);
        return min == Math.rint(min) ? ((long) min) + " min" : String.format(Locale.ROOT, "%.1f min", min);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int l = left(), t = top();
        if (button == 0 && mx >= l && mx < l + PANEL_W && my >= t) {
            int i = (int) ((my - t) / ROW_H) + scroll;
            if (i >= 0 && i < list.rows().size() && (my - t) / ROW_H < visibleRows()) {
                StationListPayload.Row r = list.rows().get(i);
                if (r.problem() == StationCraft.Problem.NONE.ordinal()) {
                    PacketDistributor.sendToServer(new StationPickPayload(r.id(), list.hasBench(), list.bench()));
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, list.rows().size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }
}
