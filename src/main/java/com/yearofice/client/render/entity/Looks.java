package com.yearofice.client.render.entity;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.species.SpeciesArt;
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
        if (SpeciesArt.GROUSE.contains(species)) return "rig_grouse";
        return SpeciesArt.KNOWN.contains(species) ? species : SpeciesArt.artOf(species);
    }

    static ResourceLocation model(String species) { return rl("geo/entity/" + geo(species) + ".geo.json"); }

    static ResourceLocation animation(String species) {
        return rl("animations/entity/" + SpeciesArt.animSpecies(species) + ".animation.json");
    }

    /** The texture for a coat byte; the moose has summer coats only so far, a bull or a cow. */
    static ResourceLocation texture(String species, int coat, boolean male) {
        String art = SpeciesArt.artOf(species);
        if (!art.equals(species) && !art.equals(SpeciesArt.PLACEHOLDER)) species = art;
        SeasonalLooks.Coat c = SeasonalLooks.Coat.byId(coat);
        if (SpeciesArt.SINGLE_COAT.contains(species)) return rl("textures/entity/" + species + "/adult_all.png");
        if (species.equals("spruce_grouse") || species.equals("ruffed_grouse")) return rl("textures/entity/" + species + "/" + (male ? "male" : "female") + "_adult.png");
        if (!SpeciesArt.KNOWN.contains(species) && !SpeciesArt.GROUSE.contains(species)) return rl("textures/entity/wood_frog/adult_all.png");
        return switch (species) {
            case "mule_deer" -> rl("textures/entity/mule_deer/" + (male ? "buck" : "doe") + "_" + (c == SeasonalLooks.Coat.WINTER ? "winter" : c == SeasonalLooks.Coat.MOULT || c == SeasonalLooks.Coat.MOTTLED ? "moult" : "summer") + ".png");
            case "wolf" -> rl("textures/entity/wolf/adult_gray.png");
            case "red_fox" -> rl("textures/entity/red_fox/adult_red.png");
            case "black_bear" -> rl("textures/entity/black_bear/adult_black.png");
            case "grizzly" -> rl("textures/entity/grizzly/adult_summer.png");
            case "rock_ptarmigan" -> rl("textures/entity/rock_ptarmigan/adult_" + (c == SeasonalLooks.Coat.MOTTLED ? "moult" : c.key) + ".png");
            case "moose" -> rl("textures/entity/moose/" + (male ? "bull" : "cow") + "_summer.png");
            case "willow_ptarmigan" -> rl("textures/entity/willow_ptarmigan/adult_" + (c == SeasonalLooks.Coat.MOTTLED ? "moult" : c.key) + ".png");
            default -> rl("textures/entity/" + species + "/adult_" + (c == SeasonalLooks.Coat.MOULT ? "mottled" : c.key) + ".png");
        };
    }
}
