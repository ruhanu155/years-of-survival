package com.yearofice.client.minigame.bowdrill;

import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.net.BowDrillOpenPayload;
import com.yearofice.fire.net.MinigameResultPayload;
import com.yearofice.sim.fire.BowDrill;
import com.yearofice.sim.fire.BowDrillJudge.Claim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The bow-drill minigame (Blueprint › The bow drill, step by step; Player systems and client › Minigames in Minecraft
 * terms): a non-pausing, see-through screen over the world. Saw the bow with the mouse (left and right) or A and D,
 * hold right mouse (or W) for pressure in the green band, keep the beat; at 100 heat tip the ember into the tinder
 * (E or Space) and blow in puffs of 1–1.5 s (hold right mouse or Space). Practiced players hold right mouse 6 s
 * instead of stroking. The gauges are drawn here; when {@code client.hud.MinigameSlot} lands they move into the
 * {@code yearofice:minigame} layer's single slot.
 *
 * <p>The heat meter is {@link BowDrill.HeatMeter}, the same model the server's judge and the JUnit bots use. The
 * screen sends one {@link MinigameResultPayload}; the server measures the time and decides.
 */
public class BowDrillScreen extends Screen {
    private enum Phase { SEAT, STROKE, HOLD, TIP, BLOW, DONE }

    private static final double DT = 0.05;
    private static final double STROKE_PX = 18;
    private static final double TIMEOUT_S = 150;

    private static final int C_DARK = 0xC01A1512;
    private static final int C_FRAME = 0xFF45372B;
    private static final int C_TEXT = 0xFFE3DDD0;
    private static final int C_DIM = 0xFF9C9383;
    private static final int C_GREEN = 0xFF587456;
    private static final int C_AMBER = 0xFFEC9A2A;
    private static final int C_RED = 0xFF9C2516;
    private static final int[] C_FIRE = {0xFF5A1C06, 0xFF9A3A0A, 0xFFD0640F, 0xFFEC9A2A, 0xFFF7CF6A};

    private final BlockPos pos;
    private final boolean practiced;
    private final BowDrill.HeatMeter meter;
    private Phase phase = Phase.SEAT;
    private double phaseS;
    private double strokeS;
    private double pressure;
    private boolean pressing;
    private boolean blowing;
    private double emberLeftS;
    private double puffS;
    private int goodPuffs;
    private double lastMouseX = Double.NaN;
    private double travel;
    private int dir;
    private int lastKey;
    private double holdS;
    private boolean tipping;
    private String note = "";
    private boolean sent;

    public BowDrillScreen(BowDrillOpenPayload p) {
        super(Component.translatable("screen.yearofice.bow_drill"));
        this.pos = p.pos();
        this.practiced = p.practiced();
        this.meter = new BowDrill.HeatMeter(p.boardWet());
        this.emberLeftS = p.emberLifeTicks() / 20.0;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ---- input ---------------------------------------------------------------------------------------------

    @Override
    public void mouseMoved(double x, double y) {
        if (Double.isNaN(lastMouseX)) {
            lastMouseX = x;
            return;
        }
        double dx = x - lastMouseX;
        lastMouseX = x;
        if (phase != Phase.STROKE || dx == 0) return;
        int d = dx > 0 ? 1 : -1;
        if (d != dir) {
            if (travel >= STROKE_PX) stroke();
            dir = d;
            travel = 0;
        }
        travel += Math.abs(dx);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            pressing = true;
            blowing = true;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            pressing = false;
            releaseBlow();
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_D) {
            if (phase == Phase.STROKE && key != lastKey) stroke();
            lastKey = key;
            return true;
        }
        if (key == GLFW.GLFW_KEY_W) {
            pressing = true;
            return true;
        }
        if (key == GLFW.GLFW_KEY_E || key == GLFW.GLFW_KEY_SPACE) {
            if (phase == Phase.TIP) tipping = true;
            else blowing = true;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_W) pressing = false;
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_E) releaseBlow();
        return super.keyReleased(key, scan, mods);
    }

    private void stroke() {
        boolean stalled = meter.stroke(strokeS, pressure);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), FireRegistry.BOW_DRILL_STROKE.get(),
                    SoundSource.PLAYERS, 0.5f, stalled ? 0.6f : 0.9f + (float) (meter.heat() / 400.0), false);
        }
        if (stalled) {
            pressure = 0;
            note = "Too much pressure: the string slipped.";
        }
    }

    private void releaseBlow() {
        if (phase != Phase.BLOW || !blowing) {
            blowing = false;
            return;
        }
        blowing = false;
        if (puffS >= BowDrill.PUFF_MIN_S && puffS <= BowDrill.PUFF_MAX_S) {
            goodPuffs++;
            note = goodPuffs >= BowDrill.PUFFS_FOR_FLAME ? "" : "Good puff. Again.";
        } else if (puffS > 0) {
            note = "Too short. Breathe out longer.";
        }
        puffS = 0;
        if (goodPuffs >= BowDrill.PUFFS_FOR_FLAME) finish(Claim.FLAME);
    }

    // ---- time ----------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (phase == Phase.DONE) return;
        phaseS += DT;
        switch (phase) {
            case SEAT -> {
                if (phaseS >= BowDrill.SEAT_S) enter(practiced ? Phase.HOLD : Phase.STROKE);
            }
            case STROKE -> {
                strokeS += DT;
                pressure = Math.max(0, Math.min(1, pressure + (pressing ? 0.6 : -0.9) * DT));
                meter.tick(DT);
                smoke(meter.smoke());
                if (meter.ember()) {
                    enter(Phase.TIP);
                    note = "Ember! Tip it into the tinder: E or Space.";
                } else if (strokeS > TIMEOUT_S) {
                    finish(Claim.NO_EMBER);
                }
            }
            case HOLD -> {
                holdS = pressing ? holdS + DT : 0;
                if (holdS >= BowDrill.PRACTICED_HOLD_S) {
                    for (int i = 0; i < 120; i++) meter.stroke(i * BowDrill.BEAT_S, 0.55);
                    if (meter.ember()) {
                        enter(Phase.TIP);
                        note = "Ember! Tip it into the tinder: E or Space.";
                    } else {
                        finish(Claim.NO_EMBER);
                    }
                } else if (phaseS > TIMEOUT_S) {
                    finish(Claim.NO_EMBER);
                }
            }
            case TIP -> {
                emberLeftS -= DT;
                if (!tipping) phaseS = 0;
                else if (phaseS >= BowDrill.TIP_S) {
                    enter(Phase.BLOW);
                    note = "Blow: hold right mouse or Space 1 to 1.5 s, three times.";
                }
            }
            case BLOW -> {
                emberLeftS -= DT;
                smoke(1);
                if (blowing) {
                    puffS += DT;
                    if (puffS > BowDrill.PUFF_MAX_S) finish(Claim.SCATTERED);
                }
            }
            case DONE -> { }
        }
        if ((phase == Phase.TIP || phase == Phase.BLOW) && emberLeftS <= 0) finish(Claim.EMBER_DIED);
    }

    private void enter(Phase p) {
        phase = p;
        phaseS = 0;
    }

    private void smoke(int level) {
        Minecraft mc = Minecraft.getInstance();
        if (level <= 0 || mc.level == null) return;
        if (mc.level.random.nextInt(level == 2 ? 2 : 5) == 0)
            mc.level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 0, 0.02, 0);
    }

    private void finish(Claim claim) {
        if (sent) return;
        sent = true;
        phase = Phase.DONE;
        PacketDistributor.sendToServer(new MinigameResultPayload(MinigameResultPayload.BOW_DRILL, pos, claim.ordinal(),
                meter.strokes(), meter.onBeatStrokes(), (int) Math.floor(meter.peak() + 1e-6), goodPuffs, practiced));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        if (!sent) finish(Claim.ABORTED);
        else super.onClose();
    }

    // ---- drawing -------------------------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // See-through: the world stays visible; only the gauge panel is drawn.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int w = 220, h = 92;
        int x = (width - w) / 2, y = height - h - 40;
        g.fill(x, y, x + w, y + h, C_DARK);
        g.renderOutline(x, y, w, h, C_FRAME);
        g.drawString(font, title, x + 6, y + 5, C_TEXT, false);

        // Heat bar with the smoke marks at 40 and 75 and the ember at 100.
        int bx = x + 6, by = y + 20, bw = w - 12, bh = 8;
        g.fill(bx, by, bx + bw, by + bh, 0xFF241C16);
        int fill = (int) Math.round(bw * Math.min(1, meter.heat() / 100.0));
        int step = meter.heat() >= 75 ? 4 : meter.heat() >= 40 ? 3 : meter.heat() >= 15 ? 2 : 1;
        g.fill(bx, by, bx + fill, by + bh, C_FIRE[step]);
        g.fill(bx + (int) (bw * 0.40), by - 1, bx + (int) (bw * 0.40) + 1, by + bh + 1, C_DIM);
        g.fill(bx + (int) (bw * 0.75), by - 1, bx + (int) (bw * 0.75) + 1, by + bh + 1, C_DIM);
        g.drawString(font, Component.literal("Heat " + (int) meter.heat()), bx, by + bh + 2, C_DIM, false);

        // Pressure gauge: green band 40–70%, amber to 85%, red past it.
        int py = by + 24;
        g.fill(bx, py, bx + bw, py + 6, 0xFF241C16);
        g.fill(bx + (int) (bw * BowDrill.PRESS_LOW), py, bx + (int) (bw * BowDrill.PRESS_HIGH), py + 6, C_GREEN);
        g.fill(bx + (int) (bw * BowDrill.PRESS_HIGH), py, bx + (int) (bw * BowDrill.PRESS_STALL), py + 6, 0xFF8A6C16);
        g.fill(bx + (int) (bw * BowDrill.PRESS_STALL), py, bx + bw, py + 6, C_RED);
        int px = bx + (int) Math.round(bw * pressure);
        g.fill(px - 1, py - 2, px + 1, py + 8, C_TEXT);

        // Beat marker: pulses 1.5 times a second, bright inside the window.
        boolean onBeat = BowDrill.onBeat(strokeS);
        int mx = x + w - 20, my = y + 4;
        g.fill(mx, my, mx + 12, my + 12, phase == Phase.STROKE ? (onBeat ? C_FIRE[4] : C_FIRE[1]) : 0xFF33281F);

        // Breath gauge while blowing: red past 1.5 s.
        if (phase == Phase.BLOW || phase == Phase.TIP) {
            int gy = py + 12;
            g.fill(bx, gy, bx + bw, gy + 5, 0xFF241C16);
            double frac = Math.min(1, puffS / 2.0);
            g.fill(bx, gy, bx + (int) (bw * frac), gy + 5, puffS > BowDrill.PUFF_MAX_S ? C_RED : puffS >= BowDrill.PUFF_MIN_S ? C_GREEN : C_AMBER);
            g.fill(bx + (int) (bw * 0.5), gy - 1, bx + (int) (bw * 0.5) + 1, gy + 6, C_DIM);
            g.fill(bx + (int) (bw * 0.75), gy - 1, bx + (int) (bw * 0.75) + 1, gy + 6, C_DIM);
        }

        String help = switch (phase) {
            case SEAT -> "Kneel, foot on the board, seat the spindle...";
            case STROKE -> "Saw: mouse left-right or A/D on the beat. Hold right mouse (or W) for pressure.";
            case HOLD -> "Practiced: hold right mouse 6 s. " + String.format("%.1f s", holdS);
            case TIP -> !tipping ? "Tip the ember: E or Space. Ember " + (int) Math.ceil(Math.max(0, emberLeftS)) + " s."
                    : "Tipping the ember into the tinder...";
            case BLOW -> "Puffs " + goodPuffs + "/" + BowDrill.PUFFS_FOR_FLAME + ". Ember " + (int) Math.ceil(Math.max(0, emberLeftS)) + " s.";
            case DONE -> "";
        };
        g.drawString(font, Component.literal(help), x + 6, y + h - 22, C_TEXT, false);
        if (!note.isEmpty()) g.drawString(font, Component.literal(note), x + 6, y + h - 11, C_AMBER, false);
    }
}
