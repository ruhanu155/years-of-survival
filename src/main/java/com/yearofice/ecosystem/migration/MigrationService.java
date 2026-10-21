package com.yearofice.ecosystem.migration;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.small.LifeCarcass;
import com.yearofice.ecosystem.small.LifeConfig;
import com.yearofice.ecosystem.small.SalmonCarcassEntity;
import com.yearofice.ecosystem.small.SmallLifeRegistry;
import com.yearofice.sim.fishing.SalmonRun;
import com.yearofice.sim.life.LifeTuning;
import com.yearofice.sim.life.MigrationDef;
import com.yearofice.sim.life.SalmonDieoff;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The year's arrivals and departures at runtime: which species are in the valley today (the bird spawner asks), which
 * waves and runs are on, and the spawned-out salmon that drift and lodge from ten days after a run's peak to freeze-up
 * (Waters and fish › The water calendar). {@code breakupEarlyDays} is the spring warmth: 7 pulls waves 2 and 3 forward 5 days.
 */
public final class MigrationService {
    private MigrationService() {}

    /** Spring warmth in days of early breakup; {@code /yoi migration shift} sets it, the weather lane will drive it. */
    public static volatile double breakupEarlyDays;
    /** Freeze-up day for the die-off season's end: October 25. */
    public static final int FREEZE_UP = Calendar.dayOfYear(10, 25);

    public static void clear() { breakupEarlyDays = 0; }

    public static int doy(ServerLevel level) { return Calendar.dayOfYearOfMinute(SliceClock.minute(level)); }

    public static int year(ServerLevel level) { return Calendar.yearOfDay(Calendar.absDayOfMinute(SliceClock.minute(level))); }

    /** True when the bird is in the valley now: its migration window, drawn per year, else the spawner's own month rule. */
    public static boolean present(ServerLevel level, String species) {
        return MigrationData.calendar().present(species, doy(level), level.getSeed(), year(level), breakupEarlyDays, LifeConfig.tuning().waveJitterDays());
    }

    public static List<MigrationDef> active(ServerLevel level) { return MigrationData.calendar().active(doy(level), breakupEarlyDays); }

    /** Salmon runs whose fish are dying today. */
    public static List<MigrationDef> dying(ServerLevel level) {
        List<MigrationDef> out = new ArrayList<>();
        int doy = doy(level);
        double days = LifeConfig.tuning().salmonDieoffDays();
        for (MigrationDef d : MigrationData.calendar().all()) {
            if (d.kind().equals("salmon_run") && SalmonDieoff.dying(doy, d.peak(), FREEZE_UP, days)) out.add(d);
        }
        return out;
    }

    /** Spent bodies of {@code species} within a spot's area. */
    public static List<SalmonCarcassEntity> bodies(ServerLevel level, FishSpotService.Spot spot, String species) {
        double r = spot.radiusM() + 4;
        BlockPos c = spot.centre();
        return level.getEntitiesOfClass(SalmonCarcassEntity.class, new AABB(c).inflate(r, 12, r), b -> b.speciesPath().equals(species));
    }

    /** Tops one spot up to the die-off's body count for a run species; returns how many it placed. */
    public static int dieOff(ServerLevel level, FishSpotService.Spot spot, String species) {
        LifeTuning t = LifeConfig.tuning();
        int idx = spot.species().indexOf(species);
        if (idx < 0) return 0;
        int stock = idx < spot.stock().size() ? spot.stock().get(idx) : 0;
        int want = SalmonDieoff.bodies(stock, 0.05, t.salmonCarcassMax());
        int have = bodies(level, spot, species).size(), placed = 0;
        double kg = SalmonRun.RUNS.containsKey(species) ? 4.5 : 2.0;
        for (int i = have; i < want; i++) {
            for (int k = 0; k < 12; k++) {
                double ang = (i * 7 + k) * 2.399963, r = Math.min(spot.radiusM(), 0.7 * k + 1);
                BlockPos col = spot.centre().offset((int) Math.round(Math.cos(ang) * r), 0, (int) Math.round(Math.sin(ang) * r));
                BlockPos w = LifeCarcass.surfaceWater(level, col.getX(), col.getZ(), spot.centre().getY());
                if (w == null) continue;
                if (LifeCarcass.spawn(level, SmallLifeRegistry.SALMON_CARCASS.get(), species, new Vec3(w.getX() + 0.5, w.getY() + 0.6, w.getZ() + 0.5), "spawned_out", kg) != null) placed++;
                break;
            }
        }
        return placed;
    }

    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 200 != 61) return;
        ServerLevel level = e.getServer().overworld();
        List<MigrationDef> dying = dying(level);
        if (dying.isEmpty()) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            for (FishSpotService.Spot spot : FishSpotService.get().spotsNear(level, p.blockPosition(), 48)) {
                for (MigrationDef d : dying) dieOff(level, spot, d.run());
            }
        }
    }
}
