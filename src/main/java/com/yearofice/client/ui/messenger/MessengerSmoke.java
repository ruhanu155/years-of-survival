package com.yearofice.client.ui.messenger;

import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.client.hud.ClientBody;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.env.Precip;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The client smoke for the messenger (Definition of done › the client smoke's screenshots). Off unless the
 * environment variable {@code YEAROFICE_SMOKE=messenger} is set: then, once the title screen is up, it opens the
 * messenger with a sample reading (a thin, cold, wet evening), saves one screenshot per page to
 * {@code <game dir>/screenshots/messenger_<page>.png} and closes the game. It never runs in a normal launch.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class MessengerSmoke {
    private MessengerSmoke() {}

    static final boolean ON = "messenger".equals(System.getenv("YEAROFICE_SMOKE"));
    private static int ticks = -1;
    private static int page = 0;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null || mc.screen == null) return;
        ticks++;
        if (ticks == 40) {
            if (ClientBody.get() == null) ClientBody.accept(sample());
            MessengerScreen s = new MessengerScreen();
            s.setPage(0);
            mc.setScreen(s);
        }
        if (ticks > 40 && (ticks - 40) % 20 == 0 && mc.screen instanceof MessengerScreen s) {
            String name = "messenger_" + MessengerPage.ALL[s.page()].id() + ".png";
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                    msg -> YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()));
            YearOfIce.LOG.info("Smoke: saved {}", new File(new File(mc.gameDirectory, "screenshots"), name));
            page++;
            if (page >= MessengerPage.ALL.length) {
                YearOfIce.LOG.info("SMOKE MESSENGER DONE");
                mc.stop();
            } else {
                s.setPage(page);
            }
        }
    }

    /** A sample reading for the screenshot: day 143, 19:40, thin, cold and wet after a creek crossing. */
    static BodySyncPayload sample() {
        float[] feel = {-6f, 3.2f, 6.5f, 0f, 3f, 0f, 3f, -1f};
        return new BodySyncPayload(142 * 1440 + 19 * 60 + 40, 62.4f, 18.6f, 820f, 2410f, -1590f, 3.6f, 8f, 46f,
                -10.7f, feel, 0.65f, 52f, 0.22f, 100f, 27f, 0.41f, 0.18f, 0.66f, 750f, false, 0,
                Activity.WALK.ordinal(), -6f, Precip.SNOW.ordinal(), 2.5f, 4f, false, 7400);
    }
}
