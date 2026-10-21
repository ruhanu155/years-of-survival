package com.yearofice.ecosystem.species;

import java.util.Set;

/**
 * Which species have their own art (geometry, textures, animations). A data-only species (Minecraft development ›
 * Data-only species) has none until the art department ships it, so it draws, and plays animations, as the wood
 * frog's placeholder. Shared by the client's model lookup and the animation names the server-side classes pick.
 */
public final class SpeciesArt {
    private SpeciesArt() {}

    public static final Set<String> KNOWN = Set.of("snowshoe_hare", "moose", "red_squirrel", "marten", "ermine", "mink", "muskrat",
            "beaver", "porcupine", "wood_frog", "yellowjacket", "brown_lemming", "collared_lemming", "bog_lemming", "singing_vole",
            "little_brown_bat", "boreal_toad");
    public static final Set<String> GROUSE = Set.of("willow_ptarmigan", "spruce_grouse", "ruffed_grouse", "rock_ptarmigan");
    /** One coat all year: the texture is {@code adult_all}. */
    public static final Set<String> SINGLE_COAT = Set.of("red_squirrel", "marten", "mink", "muskrat", "beaver", "porcupine", "wood_frog",
            "yellowjacket", "brown_lemming", "bog_lemming", "singing_vole", "little_brown_bat", "boreal_toad");
    public static final String PLACEHOLDER = "wood_frog";

    public static boolean has(String species) { return KNOWN.contains(species) || GROUSE.contains(species); }

    /** A fish species (M7: a spawned-out salmon or a winterkill body is drawn on the fish rig). */
    public static boolean fish(String species) { return com.yearofice.hunt.fishing.FishingSpecies.has(species); }

    /** The species whose animation names this one plays. */
    public static String animSpecies(String species) { return fish(species) ? "fish" : has(species) ? species : PLACEHOLDER; }
}
