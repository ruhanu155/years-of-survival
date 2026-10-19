package com.yearofice.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * {@code client.hud.MinigameSlot} (reconcile 8): the one lower-centre slot of the {@code yearofice:minigame} layer.
 * Owners register a renderer and when they want the slot; the layer gives it to the first that wants it, so two
 * overlays (a fishing fight and a call rhythm bar) can never stack.
 */
public final class MinigameSlot {
    private MinigameSlot() {}

    record Owner(String id, BooleanSupplier wants, LayeredDraw.Layer renderer) {}

    private static final List<Owner> OWNERS = new ArrayList<>();

    /** Registers an owner once, at client set-up. */
    public static synchronized void register(String id, BooleanSupplier wants, LayeredDraw.Layer renderer) {
        OWNERS.removeIf(o -> o.id().equals(id));
        OWNERS.add(new Owner(id, wants, renderer));
    }

    /** The owner holding the slot now, or null. */
    public static synchronized String holder() {
        for (Owner o : OWNERS) if (o.wants().getAsBoolean()) return o.id();
        return null;
    }

    static synchronized void render(GuiGraphics g, DeltaTracker d) {
        for (Owner o : OWNERS) {
            if (o.wants().getAsBoolean()) {
                o.renderer().render(g, d);
                return;
            }
        }
    }
}
