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
        if (SpeciesArt.fish(species)) return com.yearofice.hunt.fishing.FishEntity.isSchool(species) ? "rig_fish_school" : "rig_fish_resident";
        if (SpeciesArt.GROUSE.contains(species)) return "rig_grouse";
        return SpeciesArt.KNOWN.contains(species) ? species : SpeciesArt.PLACEHOLDER;
    }

    static ResourceLocation model(String species) { return rl("geo/entity/" + geo(species) + ".geo.json"); }

    static ResourceLocation animation(String species) {
        return rl("animations/entity/" + SpeciesArt.animSpecies(species) + ".animation.json");
    }

    /** The texture for a coat byte; the moose has summer coats only so far, a bull or a cow. */
    static ResourceLocation texture(String species, int coat, boolean male) {
        if (SpeciesArt.fish(species)) return rl("textures/entity/fish/" + species + ".png");
        SeasonalLooks.Coat c = SeasonalLooks.Coat.byId(coat);
        if (SpeciesArt.SINGLE_COAT.contains(species)) return rl("textures/entity/" + species + "/adult_all.png");
        if (species.equals("spruce_grouse") || species.equals("ruffed_grouse")) return rl("textures/entity/" + species + "/" + (male ? "male" : "female") + "_adult.png");
        if (!SpeciesArt.KNOWN.contains(species) && !SpeciesArt.GROUSE.contains(species)) return rl("textures/entity/wood_frog/adult_all.png");
        return switch (species) {
            case "rock_ptarmigan" -> rl("textures/entity/rock_ptarmigan/adult_" + (c == SeasonalLooks.Coat.MOTTLED ? "moult" : c.key) + ".png");
            case "moose" -> rl("textures/entity/moose/" + (male ? "bull" : "cow") + "_summer.png");
            case "willow_ptarmigan" -> rl("textures/entity/willow_ptarmigan/adult_" + (c == SeasonalLooks.Coat.MOTTLED ? "moult" : c.key) + ".png");
            default -> rl("textures/entity/" + species + "/adult_" + (c == SeasonalLooks.Coat.MOULT ? "mottled" : c.key) + ".png");
        };
    }
}
