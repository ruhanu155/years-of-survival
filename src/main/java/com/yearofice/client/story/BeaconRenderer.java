package com.yearofice.client.story;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.client.hud.ClientBody;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.story.BeaconLook;
import com.yearofice.sim.story.OnboardingTuning;
import com.yearofice.story.StoryConfig;
import com.yearofice.story.net.OnboardingSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * {@code client.story.BeaconRenderer}: the guided night's objective beacon, a soft light column over the current
 * step's target, drawn in world space in {@code RenderLevelStageEvent} after weather (Minecraft engine › The HUD:
 * "a world-space light column drawn in RenderLevelStageEvent, not a GUI layer"), so it registers no GUI layer.
 *
 * <p>Look rules: one column at a time; faint (30% at most) and fading to nothing on the way up; visible through
 * trees (no depth test, additive, so it reads as light and never hides what is behind it); gone within 4 m of the
 * target and growing to full strength by 12 m ({@link BeaconLook}); hidden while any screen is open, with F1, and
 * when the messenger is dead or not carried. No screen-wide effect of any kind.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class BeaconRenderer {
    private BeaconRenderer() {}

    /** Warm pale amber, the colour of the fire the night is about. */
    static final float R = 1.0f, G = 0.84f, B = 0.58f;
    static final int SEGMENTS = 8;

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        OnboardingSyncPayload p = ClientOnboarding.get();
        if (!ClientOnboarding.active(p) || !p.beacon() || !p.hasTarget()) return;
        BodySyncPayload body = ClientBody.get();
        if (body != null && body.battery() <= 0) return;

        OnboardingTuning t = StoryConfig.tuning();
        BlockPos target = p.target();
        Vec3 cam = e.getCamera().getPosition();
        double tx = target.getX() + 0.5, tz = target.getZ() + 0.5, base = target.getY() + 0.1;
        double dx = tx - cam.x, dz = tz - cam.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float alpha = (float) BeaconLook.alpha(dist, t.beaconFadeNearM(), t.beaconFadeFullM(), t.beaconAlphaPct() / 100.0);
        if (alpha <= 0.005f) return;

        // A billboard turned to the camera about the vertical axis.
        double nx = dist < 1e-6 ? 1 : -dz / dist, nz = dist < 1e-6 ? 0 : dx / dist;
        Matrix4f m = e.getPoseStack().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float x = (float) (tx - cam.x), z = (float) (tz - cam.z), y0 = (float) (base - cam.y);
        column(bb, m, x, y0, z, nx, nz, BeaconLook.HALF_WIDTH * 3, alpha * 0.35f);
        column(bb, m, x, y0, z, nx, nz, BeaconLook.HALF_WIDTH, alpha);
        MeshData mesh = bb.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void column(BufferBuilder bb, Matrix4f m, float x, float y0, float z, double nx, double nz,
                               double half, float alpha) {
        float ox = (float) (nx * half), oz = (float) (nz * half);
        for (int i = 0; i < SEGMENTS; i++) {
            double h0 = (double) i / SEGMENTS, h1 = (double) (i + 1) / SEGMENTS;
            float ya = y0 + (float) (h0 * BeaconLook.HEIGHT), yb = y0 + (float) (h1 * BeaconLook.HEIGHT);
            float a0 = alpha * (float) BeaconLook.along(h0), a1 = alpha * (float) BeaconLook.along(h1);
            bb.addVertex(m, x - ox, ya, z - oz).setColor(R, G, B, a0);
            bb.addVertex(m, x + ox, ya, z + oz).setColor(R, G, B, a0);
            bb.addVertex(m, x + ox, yb, z + oz).setColor(R, G, B, a1);
            bb.addVertex(m, x - ox, yb, z - oz).setColor(R, G, B, a1);
        }
    }
}
