package com.yearofice.client.body;

import com.yearofice.body.SosConfig;
import com.yearofice.body.net.SosFlightPayload;
import com.yearofice.body.net.SosLinePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@code client.screen.SosFlightScreen} (and the run-ended card in Hardcore): "SOS sent 14:22, day 143", the cause,
 * and the one-line "why I left" field (60 characters, skippable). Sending or skipping tells the server, which then
 * runs the jump. The server never waits longer than {@code flight_ticks}.
 */
public final class SosFlightScreen extends Screen {
    private final SosFlightPayload info;
    private EditBox line;

    public SosFlightScreen(SosFlightPayload info) {
        super(Component.translatable(info.runEnded() ? "screen.yearofice.run_ended" : "screen.yearofice.sos_flight"));
        this.info = info;
    }

    @Override
    protected void init() {
        if (info.runEnded()) return;
        int cx = width / 2;
        line = new EditBox(font, cx - 120, height / 2 + 10, 240, 18, Component.translatable("screen.yearofice.sos_line"));
        line.setMaxLength(60);
        line.setHint(Component.translatable("screen.yearofice.sos_line_hint"));
        addRenderableWidget(line);
        setInitialFocus(line);
        addRenderableWidget(Button.builder(Component.translatable("screen.yearofice.sos_send"), b -> send(line.getValue()))
                .bounds(cx - 120, height / 2 + 34, 118, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.yearofice.sos_skip"), b -> send(""))
                .bounds(cx + 2, height / 2 + 34, 118, 20).build());
    }

    private void send(String text) {
        PacketDistributor.sendToServer(new SosLinePayload(text));
        onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0xF0101418);
        // White noise: a few deterministic flecks, so the screenshot is stable.
        long seed = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime() / 2;
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < 160; i++) {
            int x = r.nextInt(width), y = r.nextInt(height);
            g.fill(x, y, x + 1, y + 1, 0x30F2EEE6);
        }
        Component head = info.runEnded()
                ? Component.translatable("screen.yearofice.run_ended_line", info.when(), info.day())
                : Component.translatable("screen.yearofice.sos_sent_line", info.when(), info.day());
        g.drawCenteredString(font, head, width / 2, height / 2 - 30, 0xFFF2EEE6);
        g.drawCenteredString(font, Component.translatable("messenger.yearofice.cause." + info.cause()), width / 2,
                height / 2 - 16, 0xFF9A968E);
        if (!info.runEnded()) {
            g.drawCenteredString(font, Component.translatable("screen.yearofice.sos_away", SosConfig.awayDays()), width / 2,
                    height / 2 - 4, 0xFF9A968E);
        }
        super.render(g, mx, my, dt);
    }
}
