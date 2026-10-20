package com.yearofice.api.hud;

import com.yearofice.item.messenger.MessengerSlots;
import net.minecraft.server.level.ServerPlayer;

/**
 * Where a system looks for the Trail Messenger (Layer 8, Bus rules): a hand, or the Curios {@code neck} slot. Only a
 * device with battery above 0 carries messenger lines; otherwise they go to the journal. Public on the API so other
 * systems (the field-note cards, onboarding) share the HUD bus's own rule instead of rebuilding it.
 */
public final class MessengerAccess {
    private MessengerAccess() {}

    public static final String NECK = MessengerSlots.NECK;

    /** The battery of the best carried messenger in a hand or the neck slot, or -1 with none there. */
    public static int battery(ServerPlayer p) { return MessengerSlots.battery(p); }

    /** True when messenger lines can reach the screen. */
    public static boolean live(ServerPlayer p) { return battery(p) > 0; }
}
