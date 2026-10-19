package com.yearofice.client.render.entity;

import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.species.MooseEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** The GeckoLib model for every slice animal: geometry, coat texture and animations by species. */
public class YoiAnimalModel<T extends YoiAnimal> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T a) { return Looks.model(a.speciesPath()); }

    @Override
    public ResourceLocation getTextureResource(T a) { return Looks.texture(a.speciesPath(), a.coat(), a.male()); }

    @Override
    public ResourceLocation getAnimationResource(T a) { return Looks.animation(a.speciesPath()); }

    /** Bull antlers scale with the date; a cow carries none. */
    @Override
    public void setCustomAnimations(T a, long instanceId, AnimationState<T> state) {
        super.setCustomAnimations(a, instanceId, state);
        if (!(a instanceof MooseEntity)) return;
        float s = a.male() ? Math.max(0.001f, a.antlerScale()) : 0.001f;
        for (String bone : new String[] {"antler_l", "antler_r"}) {
            getBone(bone).ifPresent(b -> {
                b.setHidden(!a.male() || a.antlerScale() <= 0.01f);
                b.setScaleX(s);
                b.setScaleY(s);
                b.setScaleZ(s);
            });
        }
    }
}
