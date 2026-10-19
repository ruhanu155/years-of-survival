package com.yearofice.client.minigame.bowdrill;

import com.yearofice.fire.net.BowDrillOpenPayload;
import net.minecraft.client.Minecraft;

/** Client entry for the bow drill: the server's open payload lands here and opens the screen. */
public final class BowDrillClient {
    private BowDrillClient() {}

    public static void open(BowDrillOpenPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.setScreen(new BowDrillScreen(p));
    }
}
