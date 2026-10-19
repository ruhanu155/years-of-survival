package com.yearofice.client.hud;

import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.client.ui.messenger.Fmt;
import com.yearofice.client.ui.messenger.Pal;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.item.messenger.TrailMessengerItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * {@code yearofice:messenger_corner}: while the Trail Messenger is in a hand, a small device sits in that hand's
 * bottom corner with the glance readout: time and day, warmth and feels-like, water, energy and the 24 h balance.
 * Right-click (use) opens the full screen.
 */
public final class MessengerCornerLayer implements LayeredDraw.Layer {
    public static final ResourceLocation CORNER =
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "textures/gui/messenger/corner.png");
    /** Texture size and the LCD inside it (gui_messenger.py CORNER). */
    static final int W = 96, H = 80, LCD_X = 9, LCD_Y = 9, LCD_W = 78, LCD_H = 56;
    static final float TEXT_SCALE = 0.75f;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.screen != null) return;
        InteractionHand hand = held(mc);
        if (hand == null) return;
        boolean right = (hand == InteractionHand.MAIN_HAND) == (mc.options.mainHand().get()
                == net.minecraft.world.entity.HumanoidArm.RIGHT);
        int x = right ? g.guiWidth() - W - 2 : 2;
        int y = g.guiHeight() - H - 2;
        g.blit(CORNER, x, y, 0, 0, W, H, W, H);
        ItemStack stack = mc.player.getItemInHand(hand);
        BodySyncPayload p = ClientBody.get();
        Font f = mc.font;
        g.pose().pushPose();
        g.pose().translate(x + LCD_X + 2, y + LCD_Y + 2, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1);
        int row = 0;
        int lineH = 10;
        if (TrailMessengerItem.battery(stack) <= 0) {
            g.drawString(f, Component.translatable("messenger.yearofice.corner.dead"), 0, 0, Pal.DIM, false);
        } else if (p == null) {
            g.drawString(f, Component.translatable("messenger.yearofice.vitals.waiting"), 0, 0, Pal.DIM, false);
        } else {
            g.drawString(f, Fmt.hhmm(p.minute()) + "  " + Component.translatable("messenger.yearofice.corner.day",
                    Fmt.day(p.minute())).getString(), 0, row++ * lineH, Pal.INK, false);
            g.drawString(f, Component.translatable("messenger.yearofice.corner.warmth", Math.round(p.warmth()),
                    Fmt.c(p.feelsLikeC())), 0, row++ * lineH, ClientBody.warmthWarn(p) ? Pal.RED : Pal.INK, false);
            g.drawString(f, Component.translatable("messenger.yearofice.corner.water", Math.round(p.water()),
                    Fmt.pct(p.wetness())), 0, row++ * lineH, ClientBody.waterWarn(p) ? Pal.BLUE : Pal.INK, false);
            g.drawString(f, Component.translatable("messenger.yearofice.corner.energy", Fmt.one(p.energy()),
                    Math.round(p.energyCap())), 0, row++ * lineH, ClientBody.energyWarn(p) ? Pal.RED : Pal.INK, false);
            g.drawString(f, Component.translatable("messenger.yearofice.corner.kcal", Fmt.signed(p.balance24h())),
                    0, row * lineH, ClientBody.caloriesWarn(p) ? Pal.RED : Pal.INK, false);
        }
        g.pose().popPose();
    }

    static InteractionHand held(Minecraft mc) {
        if (mc.player.getMainHandItem().is(MessengerItems.MESSENGER.get())) return InteractionHand.MAIN_HAND;
        if (mc.player.getOffhandItem().is(MessengerItems.MESSENGER.get())) return InteractionHand.OFF_HAND;
        return null;
    }
}
