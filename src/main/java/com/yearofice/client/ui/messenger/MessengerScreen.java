package com.yearofice.client.ui.messenger;

import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.client.hud.ClientBody;
import com.yearofice.core.YearOfIce;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * {@code client.ui.messenger.MessengerScreen}: the Trail Messenger held up, one screen with pages (Minecraft
 * development › Messenger screens). Opens on use or M; Q and E, the arrow keys or the wheel turn pages; 1–3 jump
 * to one; M or Esc closes. It never pauses (the Gentle pause comes with the preset sync). The device is the
 * generated {@code gui/messenger/device.png}; the LCD rectangle below matches {@code tools/artgen/gui_messenger.py}.
 */
public class MessengerScreen extends Screen {
    public static final ResourceLocation DEVICE =
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "textures/gui/messenger/device.png");
    public static final int TEX = 256;
    /** The LCD inside the device texture (gui_messenger.py DEVICE["lcd"], inclusive corners). */
    public static final int LCD_X = 36, LCD_Y = 30, LCD_W = 184, LCD_H = 157;
    private static final int PAD = 4;

    private static int lastPage = 0;
    private int page;

    public MessengerScreen() {
        super(Component.translatable("item.yearofice.messenger"));
        this.page = lastPage;
    }

    public int page() { return page; }

    public void setPage(int p) {
        int n = MessengerPage.ALL.length;
        page = Math.floorMod(p, n);
        lastPage = page;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        float scale = Math.min(1f, (height - 8) / (float) TEX);
        int size = Math.round(TEX * scale);
        int x0 = (width - size) / 2, y0 = (height - size) / 2;
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale(scale, scale, 1);
        g.blit(DEVICE, 0, 0, 0, 0, TEX, TEX, TEX, TEX);
        drawLcd(g);
        g.pose().popPose();
    }

    private void drawLcd(GuiGraphics g) {
        int x = LCD_X + PAD, y = LCD_Y + PAD - 1, w = LCD_W - 2 * PAD;
        // Tabs.
        int tx = x;
        for (int i = 0; i < MessengerPage.ALL.length; i++) {
            Component t = MessengerPage.ALL[i].title();
            int tw = font.width(t);
            if (i == page) {
                g.fill(tx - 2, y - 1, tx + tw + 2, y + 9, Pal.INK);
                g.drawString(font, t, tx, y, Pal.LCD_LIGHT, false);
            } else {
                g.drawString(font, t, tx, y, Pal.DIM, false);
            }
            tx += tw + 8;
        }
        BodySyncPayload p = ClientBody.get();
        Lcd lcd = new Lcd(g, font, x, y + 12, w, LCD_H - 12 - PAD);
        if (p == null) {
            lcd.text(Component.translatable("messenger.yearofice.vitals.waiting"), Pal.DIM);
            return;
        }
        // Status line: day, time, battery.
        lcd.text(Component.translatable("messenger.yearofice.vitals.status", Fmt.day(p.minute()), Fmt.hhmm(p.minute())),
                Pal.INK);
        if (p.battery() >= 0) lcd.right(Component.translatable("messenger.yearofice.vitals.battery",
                p.battery() / 100), p.battery() < 2000 ? Pal.RED : Pal.INK);
        lcd.row();
        lcd.rule();
        MessengerPage.ALL[page].render(lcd, p);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (MessengerClient.OPEN.matches(key, scan)) {
            onClose();
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_LEFT -> { setPage(page - 1); return true; }
            case GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_RIGHT -> { setPage(page + 1); return true; }
            default -> { }
        }
        if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + MessengerPage.ALL.length) {
            setPage(key - GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (dy != 0) setPage(page + (dy < 0 ? 1 : -1));
        return true;
    }
}
