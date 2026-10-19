package com.yearofice.client.food;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yearofice.food.cooking.SpitBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the food on a spit's crossbar (Blueprint › Cooking: the doneness ring shows on the food). The stack comes down
 * in the block-entity update with its {@code yearofice:fresh} state and {@code yearofice:doneness}, so its item model
 * picks the raw, cooked or burnt look ({@link FoodClient#look}); the food turns slowly on the bar. Up to three
 * pieces show for a stack of units.
 */
public class SpitRenderer implements BlockEntityRenderer<SpitBlockEntity> {
    @Override
    public void render(SpitBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack load = be.load();
        if (load.isEmpty() || be.getLevel() == null) return;
        int shown = Math.min(3, load.getCount());
        float turn = ((be.getLevel().getGameTime() % 360) + partialTick) * 0.6f;
        for (int i = 0; i < shown; i++) {
            pose.pushPose();
            float x = shown == 1 ? 0.5f : 0.3f + 0.4f * i / (shown - 1);
            pose.translate(x, 0.34f, 0.5f);
            pose.mulPose(Axis.XP.rotationDegrees(turn + i * 40));
            pose.scale(0.42f, 0.42f, 0.42f);
            Minecraft.getInstance().getItemRenderer().renderStatic(load, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong() + i);
            pose.popPose();
        }
    }
}
