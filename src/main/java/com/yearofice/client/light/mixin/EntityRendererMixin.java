package com.yearofice.client.light.mixin;

import com.yearofice.client.light.HeldLight;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Held light (prototype P4): entities, and the item in a hand, are drawn with the block light raised by the carried
 * lights. This path is the same with Sodium, which is why it is the part that always works.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
    private void yearofice$held(Entity entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(HeldLight.liftBlock(cir.getReturnValueI(), pos));
    }
}
