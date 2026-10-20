package com.yearofice.client.camera;

import com.yearofice.client.hud.ClientHud;
import com.yearofice.core.YearOfIce;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only camera feel for encounter hand clips (cover_neck, push_up, desperate_strike, catch_fall, stagger):
 * a short roll/pitch kick that eases out, and the warn-once {@code stay_down} line while playing dead. The server
 * decides what happens; {@link #play} is called by the client's clip driver (the body Stagger event, play-dead state).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class EncounterCamera {
    private EncounterCamera() {}

    public enum Clip {
        COVER_NECK(0f, 6f, 40), PUSH_UP(0f, -4f, 30), DESPERATE_STRIKE(3f, 5f, 12), CATCH_FALL(0f, 12f, 25), STAGGER(8f, 4f, 20);
        final float roll, pitch; final int ticks;
        Clip(float roll, float pitch, int ticks) { this.roll = roll; this.pitch = pitch; this.ticks = ticks; }
    }

    private static @Nullable Clip current;
    private static int left;
    private static boolean warnedStayDown;

    public static void play(Clip clip) { current = clip; left = clip.ticks; }

    /** Warn once per encounter that moving while playing dead is a risk. */
    public static void stayDown() {
        if (warnedStayDown) return;
        warnedStayDown = true;
        ClientHud.localPrompt("stay_down", Component.translatable("messenger.yearofice.encounter.stay_down"));
    }

    public static void endEncounter() { warnedStayDown = false; current = null; left = 0; }

    public static boolean warned() { return warnedStayDown; }

    /** Strength of the kick in 0..1 for the remaining ticks (pure, for tests). */
    static float ease(int left, int total) { float t = Math.max(0, Math.min(1, left / (float) total)); return t * t; }

    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e) {
        if (left > 0 && --left == 0) current = null;
    }

    @SubscribeEvent
    public static void angles(ViewportEvent.ComputeCameraAngles e) {
        if (current == null || Minecraft.getInstance().player == null) return;
        float k = ease(left, current.ticks);
        e.setRoll(e.getRoll() + current.roll * k);
        e.setPitch(e.getPitch() + current.pitch * k);
    }
}
