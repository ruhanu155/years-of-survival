package com.yearofice.client.hud;

import com.yearofice.body.net.BodySyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;

/**
 * The layers of the stack that are not their own class (Layer 8 table; The HUD layer stack). Each is thin until its
 * system ships, and draws its test content while {@link HudDebug} is on. What already has data draws for real:
 * frost (body warmth), the pickup, messenger and prompt lines (the bus), the breath ring (vanilla air, whose bar is
 * hidden), captions (vanilla's subtitles, moved into the stack) and the minigame slot.
 */
final class StackLayers {
    private StackLayers() {}

    static final int INK = 0xFFF2EEE6, DIM = 0xFF9A968E, AMBER = 0xFFE0A030, RED = 0xFFD8402C, BOX = 0x90101418,
            FROST = 0xE8F4FF;

    static Minecraft mc() { return Minecraft.getInstance(); }

    /** True when HUD layers (4–15) should not draw: F1, or no player. */
    static boolean hudHidden() { return mc().player == null || mc().options.hideGui; }

    /** The messenger's battery is 0 (or none is carried): layers 5, 6 and 10 go dark. */
    static boolean batteryDark() {
        if (HudDebug.on()) return HudDebug.batteryZero();
        BodySyncPayload p = ClientBody.get();
        return p == null || p.battery() <= 0;
    }

    static void centered(GuiGraphics g, Font f, Component c, int cx, int y, int color, boolean box) {
        int w = f.width(c);
        if (box) g.fill(cx - w / 2 - 3, y - 2, cx + w / 2 + 3, y + f.lineHeight, BOX);
        g.drawString(f, c, cx - w / 2, y, color, true);
    }

    /** A ring of radius r from share a to share b, starting at the top and running clockwise. */
    static void arc(GuiGraphics g, int cx, int cy, int r, double from, double to, int color) {
        int steps = (int) Math.max(8, 2 * Math.PI * r * (to - from) * 1.5);
        for (int i = 0; i <= steps; i++) {
            double a = (from + (to - from) * i / steps) * 2 * Math.PI - Math.PI / 2;
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    static void label(GuiGraphics g, String text, int x, int y) {
        g.drawString(mc().font, text, x, y, DIM, true);
    }

    // ---- 1–3: the view (drawn under F1 too) ---------------------------------------------------------------

    /** 1. Head-net mesh, goggle slits, hood rim: the garment's view, once clothing ships (M4). */
    static final class Headwear implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (mc().player == null || !HudDebug.on()) return;
            int w = g.guiWidth();
            g.fillGradient(0, 0, w, 10, 0x80000000, 0x00000000);
        }
    }

    /** 2. Frost by warmth stage: the screen edge frosts as warmth falls under 60. */
    static final class Frost implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (mc().player == null) return;
            BodySyncPayload p = ClientBody.get();
            double share = HudDebug.on() ? 0.6 : p == null ? 0 : Math.max(0, Math.min(1, (60 - p.warmth()) / 60.0));
            if (share <= 0) return;
            int a = (int) Math.round(150 * share);
            int edge = (int) Math.round(10 + 40 * share);
            int c = (a << 24) | FROST, clear = FROST;
            int w = g.guiWidth(), h = g.guiHeight();
            g.fillGradient(0, 0, w, edge, c, clear);
            g.fillGradient(0, h - edge, w, h, clear, c);
            for (int i = 0; i < edge; i++) {
                int alpha = (int) Math.round(a * (1 - i / (double) edge));
                g.fill(i, 0, i + 1, h, (alpha << 24) | FROST);
                g.fill(w - i - 1, 0, w - i, h, (alpha << 24) | FROST);
            }
        }
    }

    /** 3. Fever shimmer, sickness dim, blackout fade and the rest (M4). */
    static final class BodyFx implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (mc().player == null || !HudDebug.on()) return;
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0x18000000);
        }
    }

    // ---- 4–15: the HUD ------------------------------------------------------------------------------------

    /** 4. Edge pulse while a screen is open and a threat is within 30 m (M6). */
    static final class ThreatFlash implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden() || !HudDebug.on()) return;
            int w = g.guiWidth(), h = g.guiHeight(), c = 0xA0D8402C;
            g.fill(0, 0, w, 2, c);
            g.fill(0, h - 2, w, h, c);
            g.fill(0, 0, 2, h, c);
            g.fill(w - 2, 0, w, h, c);
        }
    }

    /** 6. The five-zone body figure, top left, when a zone needs attention (M4). */
    static final class BodyFigure implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden() || batteryDark() || !HudDebug.on()) return;
            int x = 4, y = 4;
            g.fill(x + 4, y, x + 10, y + 6, 0xC0E0A030);
            g.fill(x + 2, y + 7, x + 12, y + 17, 0xC0F2EEE6);
            g.fill(x, y + 7, x + 2, y + 15, 0xC0D8402C);
            g.fill(x + 12, y + 7, x + 14, y + 15, 0xC0F2EEE6);
            g.fill(x + 3, y + 18, x + 6, y + 28, 0xC07AA0D8);
            g.fill(x + 8, y + 18, x + 11, y + 28, 0xC0F2EEE6);
        }
    }

    /** 8. The wide slot over the hotbar while a load is carried (the carry attachment). */
    static final class CarrySlot implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden() || !HudDebug.on() || HudDebug.batteryZero()) return;
            int cx = g.guiWidth() / 2, y = g.guiHeight() - 22;
            g.fill(cx - 91, y, cx + 91, y + 22, 0xC0202428);
            g.renderOutline(cx - 91, y, 182, 22, 0xFF9A968E);
            centered(g, mc().font, Component.literal("Spruce log  40 kg"), cx, y + 7, INK, false);
        }
    }

    /** 9. "+3 birch bark" above the hotbar for 2 s. */
    static final class PickupLine implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden()) return;
            ClientHud.Shown s = ClientHud.pickup();
            Component c = s != null ? s.text() : HudDebug.on() ? Component.literal("+3 birch bark") : null;
            if (c == null) return;
            centered(g, mc().font, c, g.guiWidth() / 2, g.guiHeight() - 59, INK, false);
        }
    }

    /** 10. One unprompted messenger line, top centre. */
    static final class MessengerLine implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden() || batteryDark()) return;
            ClientHud.Shown s = ClientHud.messenger();
            Component c = s != null ? s.text() : HudDebug.on() ? Component.literal("Dark in 1 hour. Camp is 1.8 km.") : null;
            if (c == null) return;
            int color = s != null && s.priority() >= 3 ? AMBER : INK;
            centered(g, mc().font, c, g.guiWidth() / 2, 8, color, true);
        }
    }

    /** 11. Key, action and weight under the crosshair; the second row for look readouts. */
    static final class PromptLine implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden()) return;
            int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
            ClientHud.Shown p = ClientHud.prompt(), r = ClientHud.readout();
            Component row1 = p != null ? p.text() : HudDebug.on() ? Component.literal("RMB  Pick up flint (0.4 kg)") : null;
            Component row2 = r != null ? r.text() : HudDebug.on() ? Component.literal("Ice 12 cm") : null;
            if (row1 != null) centered(g, mc().font, row1, cx, cy + 14, INK, false);
            if (row2 != null) centered(g, mc().font, row2, cx, cy + 25, DIM, false);
        }
    }

    /** 12. The work ring and clock face at the crosshair while a job fills (hold-to-work). */
    static final class WorkRing implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden() || BreathRing.showing()) return;
            int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
            com.yearofice.client.work.ClientWork.Ring ring = com.yearofice.client.work.ClientWork.ring();
            if (ring == null && !HudDebug.on()) return;
            double share = ring != null ? ring.share() : 0.65;
            arc(g, cx, cy, 10, 0, 1, 0x553A3A38);
            arc(g, cx, cy, 10, 0, share, INK);
            // The clock face: a hand racing round with game time at the time-lapse rate (one turn a game hour).
            double minute = (com.yearofice.client.clock.ClientClock.nowDs() / 600.0) % 60.0;
            double a = minute / 60.0 * 2 * Math.PI - Math.PI / 2;
            for (int r = 2; r <= 7; r++) {
                int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
                g.fill(x, y, x + 1, y + 1, DIM);
            }
            if (ring != null) {
                String label = Component.translatable("work.yearofice." + ring.phase().name().toLowerCase(java.util.Locale.ROOT)).getString()
                        + (ring.of() > 1 ? " " + ring.done() + "/" + ring.of() : "");
                centered(g, mc().font, Component.literal(label), cx, cy - 24, INK, false);
            }
        }
    }

    /** 13. Breath under water: vanilla's air supply as a ring, since its bubble bar is hidden. Hides the work ring. */
    static final class BreathRing implements LayeredDraw.Layer {
        static boolean showing() {
            var p = mc().player;
            return p != null && (p.isEyeInFluid(FluidTags.WATER) || p.getAirSupply() < p.getMaxAirSupply());
        }

        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden() || !showing()) return;
            var p = mc().player;
            double share = Math.max(0, p.getAirSupply()) / (double) p.getMaxAirSupply();
            int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
            arc(g, cx, cy, 12, 0, 1, 0x553A3A38);
            arc(g, cx, cy, 12, 0, share, share < 0.3 ? RED : 0xFF9CC8F0);
        }
    }

    /** 14. Captions, bottom right: vanilla's subtitles drawn from inside the stack until directional captions ship. */
    static final class Captions implements LayeredDraw.Layer {
        private SubtitleOverlay overlay;

        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden()) return;
            if (HudDebug.on()) {
                Component c = Component.literal("[Huff, close, left]");
                int w = mc().font.width(c);
                g.fill(g.guiWidth() - w - 8, g.guiHeight() - 40, g.guiWidth() - 2, g.guiHeight() - 28, 0xCC000000);
                g.drawString(mc().font, c, g.guiWidth() - w - 5, g.guiHeight() - 38, INK, false);
                return;
            }
            if (overlay == null) overlay = new SubtitleOverlay(mc());
            overlay.render(g);
        }
    }

    /** 15. The minigame slot: one owner at a time ({@link MinigameSlot}). */
    static final class Minigame implements LayeredDraw.Layer {
        @Override
        public void render(GuiGraphics g, DeltaTracker d) {
            if (hudHidden()) return;
            if (MinigameSlot.holder() != null) {
                MinigameSlot.render(g, d);
            } else if (HudDebug.on()) {
                int cx = g.guiWidth() / 2, y = g.guiHeight() - 72;
                g.fill(cx - 40, y, cx + 40, y + 5, 0x553A3A38);
                g.fill(cx - 40, y, cx + 12, y + 5, AMBER);
                g.fill(cx + 4, y - 2, cx + 6, y + 7, INK);
            }
        }
    }
}
