package com.yearofice.client.hand;

import com.yearofice.client.hud.HudDebug;
import com.yearofice.client.hud.HudSmoke;
import com.yearofice.core.YearOfIce;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client screenshot check {@code encounter_play_dead} (M4): in the HUD smoke ({@code YEAROFICE_SMOKE=hud}) plays
 * {@code cover_neck}, saves {@code hud_encounter_play_dead_s2.png} and fails the run unless the hands layer drew.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class EncounterSmoke {
    private EncounterSmoke() {}

    private static int armed = -1;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        if (!"hud".equals(System.getenv("YEAROFICE_SMOKE"))) return;
        HudSmoke.queue(new HudSmoke.Shot("encounter_play_dead", 2, () -> {
            HudDebug.set(HudDebug.Mode.OFF);
            Minecraft.getInstance().options.hideGui = false;
            HandsOverlay.resetDrawn();
            HandsOverlay.play("cover_neck");
            armed = 0;
        }));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (armed < 0) return;
        armed++;
        if (armed == 13 && (!HandsOverlay.drawn() || !HandsOverlay.active()))
            throw new IllegalStateException("encounter_play_dead: hands overlay not on screen");
        if (armed == 13) YearOfIce.LOG.info("SMOKE ENCOUNTER hands drawn");
        if (armed >= 18) { armed = -1; HandsOverlay.play(""); } // after the screenshot (taken at tick 15)
    }
}
