package com.yearofice.hunt.fishing;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishStock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;

/**
 * Keeps visible fish in the spots near players (Blueprint › Fish as living mobs). Every few seconds, for each spot
 * within {@link #NEAR_M} of a player, each in-season species has a number of fish swimming there equal to
 * {@code ceil(N / 10)} of its stock (at most 6, a school up to 10); fish are placed in the spot's water, and fish
 * with no player within {@link #FAR_M} are removed (nothing is saved). Landing a fish takes one away
 * ({@link #removeOne}), so the water empties as the stock does.
 */
public final class FishSpawner {
    private FishSpawner() {}

    public static final double NEAR_M = 48, FAR_M = 72;
    static final int PERIOD_TICKS = 100;

    public static int wanted(String species, FishStock st) {
        int cap = FishEntity.isSchool(species) ? 10 : 6;
        return (int) Math.max(0, Math.min(cap, Math.ceil(st.n() / 10.0)));
    }

    /** Server tick: every {@link #PERIOD_TICKS}. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % PERIOD_TICKS != 7) return;
        List<ServerPlayer> players = level.players();
        for (ServerPlayer p : players) populateAround(level, p.blockPosition());
        sweep(level);
    }

    /** Fills every spot near {@code pos} up to its wanted count. Returns the number spawned. */
    public static int populateAround(ServerLevel level, BlockPos pos) {
        int spawned = 0;
        int doy = FishSeason.dayOfYear(FishingClock.day(FishingClock.minute(level.getServer())));
        for (FishSpotService.Spot spot : FishSpotService.get().spotsNear(level, pos, NEAR_M)) {
            for (SpotFish.Entry e : SpotFish.at(level, spot.centre(), doy, FishingConfig.tuning()).fish()) {
                if (!SpotFish.present(e.id(), doy, spot.type())) continue;
                long day = FishingClock.day(FishingClock.minute(level.getServer()));
                FishStock st = SpotFish.stock(level, SliceSpots.key(spot.centre()), day, e);
                int want = wanted(e.id(), st) - countIn(level, spot, e.id());
                for (int i = 0; i < want; i++) if (spawn(level, spot, e.id(), i)) spawned++;
            }
        }
        return spawned;
    }

    static AABB box(FishSpotService.Spot spot) {
        double r = spot.radiusM() + 2;
        BlockPos c = spot.centre();
        return new AABB(c.getX() - r, c.getY() - 12, c.getZ() - r, c.getX() + r + 1, c.getY() + 8, c.getZ() + r + 1);
    }

    public static List<FishEntity> in(ServerLevel level, FishSpotService.Spot spot) {
        return level.getEntitiesOfClass(FishEntity.class, box(spot));
    }

    static int countIn(ServerLevel level, FishSpotService.Spot spot, String species) {
        int n = 0;
        for (FishEntity f : in(level, spot)) if (species.equals(f.species())) n++;
        return n;
    }

    /** Places one fish in open water of the spot: the centre column first, then rings outward. */
    static boolean spawn(ServerLevel level, FishSpotService.Spot spot, String species, int salt) {
        net.minecraft.world.entity.EntityType<? extends FishEntity> type = FishingRegistry.fishType(species);
        if (type == null) return false;
        BlockPos c = spot.centre();
        for (int k = 0; k < 24; k++) {
            double ang = (salt * 7 + k) * 2.399963, r = Math.min(spot.radiusM(), 0.6 * k + (k == 0 ? 0 : 1));
            BlockPos column = c.offset((int) Math.round(Math.cos(ang) * r), 0, (int) Math.round(Math.sin(ang) * r));
            BlockPos water = waterIn(level, column);
            if (water == null) continue;
            FishEntity f = type.create(level);
            if (f == null) return false;
            f.setSpecies(species);
            f.moveTo(water.getX() + 0.5, water.getY() + 0.3, water.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
            level.addFreshEntity(f);
            return true;
        }
        return false;
    }

    /** A water cell with water above it (a fish needs two cells) near this column's surface. */
    static BlockPos waterIn(ServerLevel level, BlockPos column) {
        for (int dy = 6; dy >= -10; dy--) {
            BlockPos p = column.above(dy);
            if (level.getFluidState(p).is(FluidTags.WATER) && level.getFluidState(p.above()).is(FluidTags.WATER)) {
                return p.below().getY() >= level.getMinBuildHeight() && level.getFluidState(p.below()).is(FluidTags.WATER) ? p.below() : p;
            }
        }
        return null;
    }

    /** Removes fish with no player near. Returns the number removed. */
    public static int sweep(ServerLevel level) {
        int n = 0;
        List<FishEntity> all = new ArrayList<>();
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) if (e instanceof FishEntity f) all.add(f);
        for (FishEntity f : all) {
            if (level.getNearestPlayer(f, FAR_M) == null) {
                f.discard();
                n++;
            }
        }
        return n;
    }

    /** A fish of this species was landed at {@code water}: the nearest one in the water goes. */
    public static boolean removeOne(ServerLevel level, BlockPos water, String species) {
        FishEntity best = null;
        double bd = 20 * 20;
        for (FishEntity f : level.getEntitiesOfClass(FishEntity.class, new AABB(water).inflate(20))) {
            if (!species.equals(f.species())) continue;
            double d = f.distanceToSqr(water.getX() + 0.5, water.getY() + 0.5, water.getZ() + 0.5);
            if (d < bd) {
                bd = d;
                best = f;
            }
        }
        if (best == null) return false;
        best.discard();
        return true;
    }
}
