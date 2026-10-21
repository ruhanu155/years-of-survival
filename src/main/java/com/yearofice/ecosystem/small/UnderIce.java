package com.yearofice.ecosystem.small;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.hunt.fishing.FishingSpecies;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.life.LifeTuning;
import com.yearofice.sim.life.Winter;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Life under the ice (Waters and fish › Winterkill; Water life in depth › Life under the ice). Each still water body's
 * dissolved oxygen is a closed form of the winter's hardness, so a body that loads after months away reads the same
 * answer as one that was watched. At breakup (about April 26) a body that ran out of oxygen shows its dead fish: carcasses
 * floating at the surface, bait at best. Ice and snow are the calendar stand-ins in {@link Winter} until the world lane's
 * IceService and SnowService land (NEEDS posted on the board); {@link #ice} and {@link #snow} are the seam.
 */
public final class UnderIce {
    private UnderIce() {}

    /** Freeze-over is November 1; breakup comes {@link Winter#DAYS} days later. */
    public static final int FREEZE = Calendar.dayOfYear(11, 1), BREAKUP = FREEZE + Winter.DAYS;
    public static final String CAUSE = "winterkill";

    /** Overrides the winter's hardness for the debug command (NaN draws it from the seed). */
    public static volatile double pinnedHardness = Double.NaN;
    public static volatile long carcassesPlaced;

    /** The ice thickness (m) and snow (cm) {@code d} days after freeze-over; replace with IceService/SnowService reads. */
    public static volatile java.util.function.BiFunction<Double, Integer, Double> ice = (hard, d) -> Winter.iceM(d, hard);
    public static volatile java.util.function.BiFunction<Double, Integer, Double> snow = (hard, d) -> Winter.snowCm(d, hard);

    private static final java.util.Set<String> DONE = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void clear() {
        DONE.clear();
        pinnedHardness = Double.NaN;
        carcassesPlaced = 0;
    }

    public static double hardness(ServerLevel level, int year) {
        if (!Double.isNaN(pinnedHardness)) return pinnedHardness;
        return Winter.hardness(Thresholds.stream(level.getSeed(), "winter_hardness", year).nextDouble());
    }

    /** The oxygen type of a spot type, or null for water that does not winterkill (rivers, creeks, deep pools). */
    public static String oxygenType(String spotType) {
        return Winter.demand(spotType) > 0 ? spotType : null;
    }

    public static boolean fed(String spotType) { return spotType.equals("weed_shelf") || spotType.startsWith("lake"); }

    public static Winter.Result winter(ServerLevel level, FishSpotService.Spot spot, int year, String species) {
        double hard = hardness(level, year);
        return Winter.simulate(spot.type(), Math.max(0.5, spot.depthM()), fed(spot.type()), Winter.speciesFactor(species), LifeConfig.tuning(),
                d -> ice.apply(hard, d), d -> snow.apply(hard, d));
    }

    /** Shows the winter's dead at one spot: up to the cap of fish carcasses at the surface. Returns the number placed. */
    public static int breakup(ServerLevel level, FishSpotService.Spot spot, int year) {
        LifeTuning t = LifeConfig.tuning();
        if (oxygenType(spot.type()) == null) return 0;
        List<String> kinds = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < spot.species().size(); i++) {
            String sp = FishingSpecies.canon(spot.species().get(i));
            if (!FishingSpecies.has(sp)) continue;
            double lost = winter(level, spot, year, sp).lostShare();
            int stock = i < spot.stock().size() ? spot.stock().get(i) : 0;
            int n = (int) Math.round(lost * stock / 10.0);
            if (n > 0) {
                kinds.add(sp);
                counts.add(n);
            }
        }
        int placed = 0, cap = t.winterkillCarcassMax();
        for (int i = 0; i < kinds.size() && placed < cap; i++) {
            for (int j = 0; j < counts.get(i) && placed < cap; j++) {
                for (int k = 0; k < 12; k++) {
                    double ang = (placed * 7 + k) * 2.399963, r = Math.min(spot.radiusM(), 0.7 * k + 1);
                    BlockPos col = spot.centre().offset((int) Math.round(Math.cos(ang) * r), 0, (int) Math.round(Math.sin(ang) * r));
                    BlockPos w = LifeCarcass.surfaceWater(level, col.getX(), col.getZ(), spot.centre().getY());
                    if (w == null) continue;
                    var c = LifeCarcass.spawn(level, SmallLifeRegistry.SALMON_CARCASS.get(), kinds.get(i), new Vec3(w.getX() + 0.5, w.getY() + 0.6, w.getZ() + 0.5), CAUSE, 0.4);
                    if (c != null) {
                        placed++;
                        carcassesPlaced++;
                    }
                    break;
                }
            }
        }
        return placed;
    }

    /** True once a spot already shows this breakup's dead (a winterkill carcass from the last five game days). */
    public static boolean shown(ServerLevel level, FishSpotService.Spot spot) {
        long now = SliceClock.minute(level);
        for (var c : LifeCarcass.withCause(level, spot.centre(), spot.radiusM() + 4, CAUSE)) {
            if (now - c.record().deathMinute() < 5L * Calendar.MINUTES_PER_DAY) return true;
        }
        return false;
    }

    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 600 != 211) return;
        ServerLevel level = e.getServer().overworld();
        int doy = Calendar.dayOfYearOfMinute(SliceClock.minute(level));
        if (doy < BREAKUP || doy > BREAKUP + 4) return;
        int year = Calendar.yearOfDay(Calendar.absDayOfMinute(SliceClock.minute(level)));
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            for (FishSpotService.Spot s : FishSpotService.get().spotsNear(level, p.blockPosition(), 48)) {
                if (DONE.add(s.centre().asLong() + ":" + year) && !shown(level, s)) breakup(level, s, year);
            }
        }
    }
}
