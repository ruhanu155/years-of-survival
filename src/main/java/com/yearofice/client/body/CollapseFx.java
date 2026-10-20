package com.yearofice.client.body;

import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.client.hud.ClientBody;
import com.yearofice.core.YearOfIce;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * The collapse, client side (Collapse and SOS): while the server says the body is down ({@code sos == 1}) the world
 * sinks to grey and the player's input is blocked. The server already decided; this only shows it.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class CollapseFx {
    private CollapseFx() {}

    private static float grey;

    private static boolean down() {
        BodySyncPayload p = ClientBody.get();
        return p != null && p.sos() == 1;
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent e) {
        if (!down()) return;
        var in = e.getInput();
        in.leftImpulse = 0;
        in.forwardImpulse = 0;
        in.up = in.down = in.left = in.right = in.jumping = in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Pre e) {
        grey = down() ? Math.min(1f, grey + 0.03f) : Math.max(0f, grey - 0.08f);
        if (grey <= 0.001f) return;
        GuiGraphics g = e.getGuiGraphics();
        int a = Math.round(grey * 200);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (a << 24) | 0x707070);
    }
}
