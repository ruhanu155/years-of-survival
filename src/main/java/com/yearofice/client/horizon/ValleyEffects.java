package com.yearofice.client.horizon;

import com.yearofice.core.YearOfIce;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * {@code yearofice:valley} dimension effects (Coordinates and height › Clouds): the overworld's sky with the cloud
 * deck at 1,800, above every peak, so clouds never cut through the valley walls. The custom sky, lightmap,
 * aurora and the storm deck at 400–700 m replace this with the Look department's sky renderer.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class ValleyEffects extends DimensionSpecialEffects {
    public static final float CLOUD_LEVEL = 1800.0F;

    public ValleyEffects() { super(CLOUD_LEVEL, true, SkyType.NORMAL, false, false); }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
        return fog.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int y) { return false; }

    @SubscribeEvent
    public static void register(RegisterDimensionSpecialEffectsEvent e) {
        e.register(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "valley"), new ValleyEffects());
    }
}
