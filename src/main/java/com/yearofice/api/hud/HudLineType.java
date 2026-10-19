package com.yearofice.api.hud;

import java.util.Locale;

/**
 * The bus's line types (Minecraft development › Layer 8, Line types): each has its channel and default priority.
 * Lang keys are {@code messenger.yearofice.<type>.<id>}.
 */
public enum HudLineType {
    PICKUP(Channel.PICKUP, 1),
    PROMPT(Channel.PROMPT, 2),
    READOUT(Channel.READOUT, 1),
    ALERT(Channel.MESSENGER, 2),
    BODY(Channel.MESSENGER, 2),
    WARN(Channel.MESSENGER, 3),
    SEASON(Channel.MESSENGER, 1),
    LOOK(Channel.MESSENGER, 1),
    CARD(Channel.MESSENGER, 1),
    EVENT(Channel.MESSENGER, 2),
    NEARMISS(Channel.MESSENGER, 2),
    JOURNAL(Channel.JOURNAL, 0);

    /** Where a line shows: the pickup line, the two prompt rows, the messenger line, or the journal page only. */
    public enum Channel { PICKUP, PROMPT, READOUT, MESSENGER, JOURNAL }

    public final Channel channel;
    public final int priority;

    HudLineType(Channel channel, int priority) {
        this.channel = channel;
        this.priority = priority;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
}
