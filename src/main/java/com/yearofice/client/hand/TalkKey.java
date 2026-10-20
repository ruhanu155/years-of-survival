package com.yearofice.client.hand;

import com.mojang.blaze3d.platform.InputConstants;
import com.yearofice.core.YearOfIce;
import com.yearofice.net.payload.TalkPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Talk as you walk (B7): double-tap V toggles the survivor's voice. Two presses within {@link #WINDOW_TICKS} flip the
 * toggle and send {@code yearofice:talk} with the new state; the server decides what the voice does.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class TalkKey {
    private TalkKey() {}

    public static final int WINDOW_TICKS = 8;
    public static final KeyMapping KEY = new KeyMapping("key.yearofice.talk_walk", InputConstants.Type.KEYSYM, InputConstants.KEY_V, "key.categories.yearofice");
    private static long lastTap = Long.MIN_VALUE / 2, tick;
    private static boolean on;

    /** Pure rule: a press at {@code now} is the second of a double-tap when the last one was within the window. */
    public static boolean isDoubleTap(long lastTapTick, long now) { return now - lastTapTick <= WINDOW_TICKS; }

    public static boolean talking() { return on; }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent e) { e.register(KEY); }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        tick++;
        if (Minecraft.getInstance().player == null) { on = false; return; }
        while (KEY.consumeClick()) {
            if (isDoubleTap(lastTap, tick)) {
                on = !on;
                PacketDistributor.sendToServer(new TalkPayload(on));
                lastTap = Long.MIN_VALUE / 2;
            } else {
                lastTap = tick;
            }
        }
    }
}
