package com.yearofice.client.render.entity;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.ecosystem.SeasonalLooks;
import net.minecraft.resources.ResourceLocation;

/**
 * Where each slice species' art lives (Minecraft development › Models, rigs and animation controllers): land
 * species have their own geometry, birds share {@code rig_<rig>.geo.json}; textures are
 * {@code textures/entity/<id>/<variant>_<coat>.png}; animations resolve to {@code animations/entity/<id>.animation.json}.
 */
final class Looks {
    private Looks() {}

    static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path); }

    static String geo(String species) {
        return switch (species) {
            case "willow_ptarmigan" -> "rig_grouse";
            default -> species;
        };
    }

    static ResourceLocation model(String species) { return rl("geo/entity/" + geo(species) + ".geo.json"); }

    static ResourceLocation animation(String species) { return rl("animations/entity/" + species + ".animation.json"); }

    /** The texture for a coat byte; the moose has summer coats only so far, a bull or a cow. */
    static ResourceLocation texture(String species, int coat, boolean male) {
        SeasonalLooks.Coat c = SeasonalLooks.Coat.byId(coat);
        return switch (species) {
            case "moose" -> rl("textures/entity/moose/" + (male ? "bull" : "cow") + "_summer.png");
            case "willow_ptarmigan" -> rl("textures/entity/willow_ptarmigan/adult_" + (c == SeasonalLooks.Coat.MOTTLED ? "moult" : c.key) + ".png");
            default -> rl("textures/entity/" + species + "/adult_" + (c == SeasonalLooks.Coat.MOULT ? "mottled" : c.key) + ".png");
        };
    }
}
