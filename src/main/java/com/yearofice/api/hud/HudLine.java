package com.yearofice.api.hud;

import java.util.List;

/**
 * A line for the player (Layer 8): type, priority, lang key, up to 6 typed arguments and the posting system.
 *
 * @param key    a full lang key, {@code messenger.yearofice.<type>.<id>}
 * @param source the posting system ({@code "body"}, {@code "fire"}, {@code "food.traps"} ...); lines from a trap
 *               are refused (no trap catches on the HUD)
 */
public record HudLine(HudLineType type, int priority, String key, List<HudArg> args, String source) {
    public static final int MAX_ARGS = 6;

    public HudLine {
        args = List.copyOf(args);
        if (args.size() > MAX_ARGS) throw new IllegalArgumentException("a HUD line takes at most 6 arguments: " + key);
        if (priority < 0 || priority > 3) throw new IllegalArgumentException("priority 0-3: " + key);
        if (source == null) source = "";
    }

    /** A line of {@code type} at its default priority, keyed {@code messenger.yearofice.<type>.<id>}. */
    public static HudLine of(HudLineType type, String id, String source, HudArg... args) {
        return new HudLine(type, type.priority, "messenger.yearofice." + type.id() + "." + id, List.of(args), source);
    }

    public HudLine withPriority(int p) { return new HudLine(type, p, key, args, source); }
}
