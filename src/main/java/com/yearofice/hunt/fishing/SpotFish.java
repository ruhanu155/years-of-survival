package com.yearofice.hunt.fishing;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.sim.fishing.FishData;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishStock;
import com.yearofice.sim.fishing.FishingTuning;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Which fish a bobber can meet. Where {@link FishSpotService} has a spot around the water, its type, q, radius and
 * species list apply and each species' capacity K is the spot's starting stock x the stock preset; anywhere else the
 * slice's grayling rule in {@link SliceSpots} stands, so the M2.5 behaviour is unchanged.
 */
public final class SpotFish {
    private SpotFish() {}

    /** One species at the water: id, today's capacity K and the growth rate. */
    public record Entry(String id, double k, double r) {}

    /** The service spot (empty off a generated spot) and the species it holds that have a fish file. */
    public record Here(Optional<FishSpotService.Spot> spot, List<Entry> fish) {}

    public static Here at(ServerLevel level, BlockPos water, int dayOfYear, FishingTuning t) {
        Optional<FishSpotService.Spot> sp = FishSpotService.get().spotAt(level, water);
        if (sp.isEmpty()) return new Here(sp, List.of());
        List<Entry> out = new ArrayList<>();
        FishSpotService.Spot s = sp.get();
        for (int i = 0; i < s.species().size(); i++) {
            String id = FishingSpecies.canon(s.species().get(i));
            if (!FishingSpecies.has(id)) continue;
            FishData d = FishingSpecies.data(id);
            int init = i < s.stock().size() ? s.stock().get(i) : 0;
            if (init <= 0) continue;
            double runX = RunLedger.strengthNow(level, id, FishingClock.day(FishingClock.minute(level.getServer())));
            out.add(new Entry(id, init * t.stockX() * runX, d.rPerMonth() <= 0 ? 0.3 : d.rPerMonth()));
        }
        return new Here(sp, out);
    }

    /** True if the species is in season (m_season above zero) at this spot type and day. */
    public static boolean present(String id, int day, String spotType) {
        return FishingSpecies.season(id).factor(day, spotType) > 0;
    }

    /** Stock of one species at a water cell. */
    public static FishStock stock(ServerLevel level, long cellKey, long absDay, Entry e) {
        return SliceSpots.stocks(level).get(SliceSpots.speciesKey(cellKey, e.id()), absDay, e.k(), e.r());
    }

    /** As {@link #stock(ServerLevel, long, long, Entry)} for a spot of a known type: no migration into a tarn or pond. */
    public static FishStock stock(ServerLevel level, long cellKey, long absDay, Entry e, String spotType) {
        double mig = com.yearofice.sim.fishing.SpotMigration.migrates(spotType) ? FishingConfig.tuning().spotMigrationPerDay() : 0;
        return SliceSpots.stocks(level).get(SliceSpots.speciesKey(cellKey, e.id()), absDay, e.k(), e.r(), mig);
    }
}
