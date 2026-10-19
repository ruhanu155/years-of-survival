package com.yearofice.client.minigame.fishing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yearofice.core.YearOfIce;
import com.yearofice.hunt.fishing.BobberEntity;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.sim.fishing.StrikeJudge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Draws {@code yearofice:bobber}: a birch-bark float that bobs on the water, shakes on a nibble or the rod-tip
 * twitch before a bite, and goes under on the dip; and the line from the rod tip, sagging between them. Placeholder
 * art from {@code tools/artgen} (G0).
 */
public final class BobberRenderer extends EntityRenderer<BobberEntity> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "textures/entity/bobber.png");
    static final RenderType TYPE = RenderType.entityCutout(TEXTURE);
    static final int SEGMENTS = 16;

    public BobberRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(BobberEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        float time = e.tickCount + pt;
        StrikeJudge.Cue cue = e.cue();
        float dy = 0, dx = 0;
        if (e.state() == BobberEntity.State.FLOATING) dy = 0.03f * Mth.sin(time * 0.15f);
        switch (cue) {
            case DIP -> dy = -0.3f;
            case TWITCH -> dx = 0.03f * Mth.sin(time * 2.1f);
            case NIBBLE -> {
                dx = 0.04f * Mth.sin(time * 2.7f);
                dy -= 0.04f;
            }
            case NONE -> { }
        }
        if (e.state() == BobberEntity.State.HOOKED) {
            dy = -0.15f;
            dx = 0.05f * Mth.sin(time * 1.3f);
        }
        pose.pushPose();
        pose.translate(dx, dy + 0.1f, 0);
        pose.pushPose();
        pose.scale(0.4f, 0.4f, 0.4f);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose last = pose.last();
        VertexConsumer vc = buffers.getBuffer(TYPE);
        quad(vc, last, light, 0, 0, 0, 1);
        quad(vc, last, light, 1, 0, 1, 1);
        quad(vc, last, light, 1, 1, 1, 0);
        quad(vc, last, light, 0, 1, 0, 0);
        pose.popPose();

        Player owner = e.ownerPlayer();
        if (owner != null) {
            Vec3 hand = handPos(owner, pt);
            Vec3 at = e.getPosition(pt).add(dx, dy + 0.1, 0);
            float fx = (float) (hand.x - at.x), fy = (float) (hand.y - at.y), fz = (float) (hand.z - at.z);
            VertexConsumer line = buffers.getBuffer(RenderType.lineStrip());
            PoseStack.Pose lp = pose.last();
            for (int i = 0; i <= SEGMENTS; i++) lineVertex(line, lp, fx, fy, fz, i / (float) SEGMENTS, (i + 1) / (float) SEGMENTS);
        }
        pose.popPose();
        super.render(e, yaw, pt, pose, buffers, light);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, int light, float x, float y, float u, float v) {
        vc.addVertex(p, x - 0.5f, y - 0.5f, 0).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(p, 0, 1, 0);
    }

    /** A point at share {@code a} along a line that sags in a parabola toward the bobber; its normal points to the next. */
    private static void lineVertex(VertexConsumer vc, PoseStack.Pose p, float fx, float fy, float fz, float a, float b) {
        float x0 = fx * a, y0 = fy * (a * a + a) * 0.5f, z0 = fz * a;
        float x1 = fx * b - x0, y1 = fy * (b * b + b) * 0.5f - y0, z1 = fz * b - z0;
        float len = Mth.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
        if (len < 1e-6f) len = 1;
        vc.addVertex(p, x0, y0, z0).setColor(0xFF1A1512).setNormal(p, x1 / len, y1 / len, z1 / len);
    }

    /** The rod tip: in first person from the camera's near plane, in third person beside the body. */
    private Vec3 handPos(Player p, float pt) {
        int side = p.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (!p.getMainHandItem().is(FishingRegistry.ROD.get())) side = -side;
        Minecraft mc = Minecraft.getInstance();
        if (entityRenderDispatcher.options.getCameraType().isFirstPerson() && p == mc.player) {
            double fovScale = 960.0 / entityRenderDispatcher.options.fov().get();
            Vec3 off = entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(side * 0.525f, -0.1f).scale(fovScale);
            return p.getEyePosition(pt).add(off);
        }
        float body = Mth.lerp(pt, p.yBodyRotO, p.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(body), cos = Mth.cos(body);
        double s = p.getScale();
        double across = side * 0.35 * s, ahead = 0.8 * s;
        return p.getEyePosition(pt).add(-cos * across - sin * ahead, (p.isCrouching() ? -0.19 : 0) - 0.45 * s, -sin * across + cos * ahead);
    }

    @Override
    public ResourceLocation getTextureLocation(BobberEntity e) { return TEXTURE; }
}
