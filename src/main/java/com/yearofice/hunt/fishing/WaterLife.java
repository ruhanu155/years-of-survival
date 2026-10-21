package com.yearofice.hunt.fishing;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishStock;
import com.yearofice.sim.fishing.SalmonRun;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What the ecosystem lanes may ask of the water without touching the fishing internals: the spots near a point, how
 * full they are, which run is passing, and taking one fish for a predator (an eagle, a bear, an otter). Taking a
 * fish lowers the same stock a catch does and removes one visible fish from the spot.
 */
public final class WaterLife {
    private WaterLife() {}

    public static List<FishSpotService.Spot> spotsNear(ServerLevel level, BlockPos pos, double radius) {
        return FishSpotService.get().spotsNear(level, pos, radius);
    }

    /** The fullest in-season species at a spot: its id and N/K, or null if the spot holds nothing in season now. */
    public record Best(String species, double fraction, double n) {}

    public static Best fullest(ServerLevel level, FishSpotService.Spot spot) {
        long minute = FishingClock.minute(level.getServer());
        long day = FishingClock.day(minute);
        int doy = FishSeason.dayOfYear(day);
        Best best = null;
        for (SpotFish.Entry e : SpotFish.at(level, spot.centre(), doy, FishingConfig.tuning()).fish()) {
            if (!SpotFish.present(e.id(), doy, spot.type())) continue;
            FishStock st = SpotFish.stock(level, SliceSpots.key(spot.centre()), day, e, spot.type());
            if (st.n() < 1) continue;
            if (best == null || st.fraction() > best.fraction()) best = new Best(e.id(), st.fraction(), st.n());
        }
        return best;
    }

    /** Takes one fish of the fullest in-season species from the spot for a predator; returns its species or null. */
    public static String takeFish(ServerLevel level, FishSpotService.Spot spot) {
        Best b = fullest(level, spot);
        if (b == null) return null;
        long minute = FishingClock.minute(level.getServer());
        long day = FishingClock.day(minute);
        for (SpotFish.Entry e : SpotFish.at(level, spot.centre(), FishSeason.dayOfYear(day), FishingConfig.tuning()).fish()) {
            if (!e.id().equals(b.species())) continue;
            SliceSpots.stocks(level).harvest(SliceSpots.speciesKey(SliceSpots.key(spot.centre()), e.id()), day, e.k(), e.r(), 1, minute);
            FishSpawner.removeOne(level, spot.centre(), e.id());
            return e.id();
        }
        return null;
    }

    /** The strongest salmon run passing today, 0 to 1 (bears fish where this is above 0). */
    public static double runStrength(ServerLevel level, FishSpotService.Spot spot) {
        int doy = FishSeason.dayOfYear(FishingClock.day(FishingClock.minute(level.getServer())));
        double best = 0;
        for (String s : spot.species()) {
            String id = FishingSpecies.canon(s);
            if (SalmonRun.isRun(id) && FishingSpecies.has(id) && !FishingSpecies.season(id).resident()) best = Math.max(best, SalmonRun.strength(id, doy));
        }
        return best;
    }
}
