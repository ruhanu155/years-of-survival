package com.yearofice.client.body;

import com.yearofice.core.YearOfIce;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Getting up from a bed with the sneak or jump key. While the player lies down vanilla shows its in-bed chat screen,
 * which swallows movement keys; here sneak (any time) or jump (with the chat line empty, so a typed space stays a
 * space) sends vanilla's stop-sleeping packet, the same one the screen's "Leave Bed" button and Escape send. The server
 * decides: it wakes the player only if they are asleep.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class SleepKeys {
    private SleepKeys() {}

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre e) {
        if (!(e.getScreen() instanceof InBedChatScreen screen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isSleeping()) return;
        boolean sneak = mc.options.keyShift.matches(e.getKeyCode(), e.getScanCode());
        boolean jump = mc.options.keyJump.matches(e.getKeyCode(), e.getScanCode()) && chatEmpty(screen);
        if (!sneak && !jump) return;
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
        e.setCanceled(true);
    }

    static boolean chatEmpty(InBedChatScreen screen) {
        for (GuiEventListener c : screen.children()) {
            if (c instanceof EditBox box && !box.getValue().isEmpty()) return false;
        }
        return true;
    }
}
