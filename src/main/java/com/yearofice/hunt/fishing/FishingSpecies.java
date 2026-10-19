package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishSpecies;
import java.util.Map;

/**
 * The species the slice's rod can take. The Blueprint wants every species read from
 * {@code data/yearofice/fish/<id>.json} by {@code ecosystem.fish.FishSpecies} (Fishing in depth › Species data);
 * that loader is Ecosystem's and lands with M5's resident fish. Until then the slice uses the sim's reference
 * grayling, whose numbers are the Blueprint's row, and both sides of the lockstep fight resolve a species by id here.
 */
public final class FishingSpecies {
    private FishingSpecies() {}

    public static final String GRAYLING = "arctic_grayling";

    private static final Map<String, FishSpecies> BY_ID = Map.of(GRAYLING, FishSpecies.arcticGrayling());
    private static final Map<String, FishSeason> SEASONS = Map.of(GRAYLING, FishSeason.arcticGrayling());

    public static FishSpecies byId(String id) {
        FishSpecies s = BY_ID.get(id);
        if (s == null) throw new IllegalArgumentException("unknown fish species " + id);
        return s;
    }

    public static FishSeason season(String id) { return SEASONS.get(id); }

    public static FishSpecies grayling() { return BY_ID.get(GRAYLING); }
}
