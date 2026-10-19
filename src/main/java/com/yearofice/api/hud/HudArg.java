package com.yearofice.api.hud;

/**
 * One typed argument of a {@link HudLine} (Layer 8): an int, a fixed-point number with a unit tag (hundredths:
 * {@code 1250} with {@code "kg"} is 12.5 kg), an item id, a species id, or plain text for lines a system words
 * itself (prompts offered with {@code offerPrompt}). The client formats numbers through {@code client.ui.Units},
 * metric first.
 */
public record HudArg(Kind kind, long value, String text) {
    public enum Kind { INT, FIXED, ITEM, SPECIES, TEXT }

    /** Unit tags the client knows: kg, g, c (°C), m, km, cm, mm, l, ml, kcal, pct, min, h, ms (m/s). */
    public static HudArg fixed(double v, String unit) { return new HudArg(Kind.FIXED, Math.round(v * 100), unit); }

    public static HudArg of(long v) { return new HudArg(Kind.INT, v, ""); }

    public static HudArg item(String id) { return new HudArg(Kind.ITEM, 0, id); }

    public static HudArg species(String id) { return new HudArg(Kind.SPECIES, 0, id); }

    public static HudArg text(String s) { return new HudArg(Kind.TEXT, 0, s); }

    public HudArg {
        if (text == null) text = "";
    }
}
