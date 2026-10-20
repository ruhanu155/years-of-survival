package com.yearofice.hud.messenger;

import net.minecraft.server.level.ServerPlayer;

/** Kept for existing callers; the rule now lives on the API as {@link com.yearofice.api.hud.MessengerAccess}. */
public final class MessengerAccess {
    private MessengerAccess() {}

    public static final String NECK = com.yearofice.api.hud.MessengerAccess.NECK;

    public static int battery(ServerPlayer p) { return com.yearofice.api.hud.MessengerAccess.battery(p); }

    public static boolean live(ServerPlayer p) { return com.yearofice.api.hud.MessengerAccess.live(p); }
}
