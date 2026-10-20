package com.yearofice.client.hud;

import java.util.List;

/**
 * The 16 GUI layers in draw order, bottom to top (Minecraft development › The HUD layer stack; Layer 8). Pure data:
 * {@link HudLayers} registers them in this order and a JUnit test holds the list to the doc's table.
 */
public final class HudStack {
    private HudStack() {}

    public static final List<String> ORDER = List.of(
            "headwear", "frost", "body_fx", "threat_flash", "vitals_strip", "body_figure", "load_meter", "carry_slot",
            "pickup_line", "messenger_line", "prompt_line", "work_ring", "breath_ring", "captions", "minigame", "field_note_card");

    /** Layers 1–3 are the view, not the HUD: they still draw with F1. */
    public static final int VIEW_LAYERS = 3;

    /** The layers that go dark with the messenger's battery at 0: the vitals strip, the body figure, the messenger line. */
    public static final List<String> BATTERY_LAYERS = List.of("vitals_strip", "body_figure", "messenger_line");

    /** 1-based position in the stack. */
    public static int number(String id) { return ORDER.indexOf(id) + 1; }
}
