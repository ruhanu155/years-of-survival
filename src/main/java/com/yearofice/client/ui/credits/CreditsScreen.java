package com.yearofice.client.ui.credits;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.core.YearOfIce;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** The credits screen: every entry of {@code assets/yearofice/credits.json} (asset, how it was made, its licence). */
public final class CreditsScreen extends Screen {
    private final @Nullable Screen parent;
    private final List<String> lines = new ArrayList<>();
    private int scroll;

    public CreditsScreen(@Nullable Screen parent) {
        super(Component.translatable("credits.yearofice.title"));
        this.parent = parent;
    }

    /** One line per entry: {@code asset_id - source_type - licence}. Pure over the parsed JSON. */
    public static List<String> lines(JsonObject root) {
        List<String> out = new ArrayList<>();
        JsonArray arr = root.getAsJsonArray("entries");
        if (arr == null) return out;
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            String src = o.has("source_type") ? o.get("source_type").getAsString() : "?";
            String lic = o.has("licence") ? o.get("licence").getAsString() : "?";
            String extra = o.has("source") ? " (" + o.get("source").getAsString() + ")" : "";
            out.add(o.get("asset_id").getAsString() + "  -  " + src + "  -  " + lic + extra);
        }
        return out;
    }

    @Override
    protected void init() {
        lines.clear();
        try (var in = Minecraft.getInstance().getResourceManager().open(
                ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "credits.json"))) {
            lines.addAll(lines(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()));
        } catch (Exception ex) {
            lines.add("credits.json unreadable: " + ex.getMessage());
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 50, height - 26, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Math.max(0, Math.min(Math.max(0, lines.size() - visible()), scroll - (int) Math.signum(dy) * 3));
        return true;
    }

    private int visible() { return Math.max(1, (height - 70) / 10); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("credits.yearofice.count", lines.size()), width / 2, 26, 0xA0A0A0);
        int y = 44;
        for (int i = scroll; i < Math.min(lines.size(), scroll + visible()); i++, y += 10) {
            g.drawString(font, font.plainSubstrByWidth(lines.get(i), width - 40), 20, y, 0xE0E0E0, false);
        }
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
