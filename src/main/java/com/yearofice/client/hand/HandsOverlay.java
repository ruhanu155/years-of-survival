package com.yearofice.client.hand;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.client.camera.EncounterCamera;
import com.yearofice.core.YearOfIce;
import com.yearofice.net.YoiNet;
import com.yearofice.sound.YoiSounds;
import java.io.InputStreamReader;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client clip player and first-person hands layer (RECONCILE 171). Clips come from the server as
 * {@code encounter_clip}; each also kicks {@link EncounterCamera}. The hands are drawn from the cubes of
 * {@code geo/hands.geo.json} posed by {@code animations/hands.animation.json} (the sampler is {@link HandClips}).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class HandsOverlay {
    private HandsOverlay() {}

    static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "encounter_hands");
    private static Map<String, HandClips.Clip> clips = Map.of();
    private static HandClips.Clip active;
    private static int age;
    private static volatile boolean drawn;

    /** True once the hands layer has drawn since the last {@link #resetDrawn} (for the screenshot check). */
    public static boolean drawn() { return drawn; }

    public static void resetDrawn() { drawn = false; }

    public static boolean active() { return active != null; }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        YoiNet.encounterSink = p -> Minecraft.getInstance().execute(() -> play(p.clip()));
        try (var in = HandsOverlay.class.getResourceAsStream("/assets/yearofice/animations/hands.animation.json")) {
            clips = HandClips.parse(JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject());
        } catch (Exception ex) {
            YearOfIce.LOG.error("hand clips failed to load", ex);
        }
    }

    /** Plays {@code name}; the empty name ends the encounter. */
    public static void play(String name) {
        if (name.isEmpty()) { active = null; EncounterCamera.endEncounter(); return; }
        HandClips.Clip c = clips.get(name);
        if (c == null) return;
        active = c;
        age = 0;
        EncounterCamera.play(EncounterCamera.Clip.valueOf(name.toUpperCase(java.util.Locale.ROOT)));
        if (name.equals("cover_neck")) { EncounterCamera.stayDown(); sound(YoiSounds.BREATH_PANIC.get()); }
        if (name.equals("stagger")) sound(YoiSounds.HIT_GRUNT.get());
    }

    private static void sound(SoundEvent s) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, 1f, 0.8f));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (active == null) return;
        age++;
        if (active.name().equals("cover_neck") && age % 80 == 0) sound(YoiSounds.BREATH_PANIC.get());
        if (!active.hold() && age / 20f > active.length()) active = null;
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, LAYER, HandsOverlay::render);
    }

    static void render(GuiGraphics g, net.minecraft.client.DeltaTracker dt) {
        if (active == null) return;
        float t = (age + dt.getGameTimeDeltaPartialTick(false)) / 20f;
        int w = g.guiWidth(), h = g.guiHeight();
        float s = h / 140f;
        for (String bone : new String[] {"left_hand", "right_hand"}) {
            HandClips.Pose p = active.sample(bone, t);
            boolean left = bone.startsWith("left");
            g.pose().pushPose();
            g.pose().translate(w * (left ? 0.38f : 0.62f) + p.x() * s, h + p.y() * s, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(p.rotZ()));
            g.pose().scale(s, s, 1);
            int sx = left ? -6 : -6;
            g.fill(sx, -34, sx + 12, 0, 0xFF3B4A3A);       // sleeve
            g.fill(sx - 1, -46, sx + 13, -34, 0xFFC99A74);  // hand
            g.pose().popPose();
        }
        drawn = true;
    }
}
