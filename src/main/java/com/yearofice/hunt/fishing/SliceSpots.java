package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishStock;
import com.yearofice.sim.fishing.FishingTuning;
import com.yearofice.sim.fishing.SpotRule;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The slice's fishing spot until stage 4's spots are stored in region layers (Minecraft engine › Fishing › Bites):
 * any water at least 2 × 2 blocks across and 1.5 m deep in every column ({@link SpotRule}) is the slice's one spot
 * type, a creek mouth on the valley's creek, with q from config. Water that fails the rule is "elsewhere"
 * (m_place 0.05).
 *
 * <p>Stocks (Blueprint › Fishing in depth › Fish stocks) are kept per 8 × 8-block cell of spot water in the
 * overworld's saved data: K = 30 grayling × q (×5 during the spawn), N starts full, each landed fish takes one, and
 * on each new month N regrows by the logistic rule (r 0.5, river immigration 0.05, ×0.25 from November to April).
 */
public final class SliceSpots {
    private SliceSpots() {}

    public static final String SPOT_TYPE = "creek_mouth";
    public static final double GRAYLING_R = 0.5;
    static final int MAX_DEPTH_BLOCKS = 16;

    /** What the bobber's column is. {@code key} names the stock cell. */
    public record Spot(boolean atSpot, double depthM, long key, BlockPos water) {}

    /** Depth of the water column at (x, z) whose surface block is at {@code y}, in metres; 0 if not water there. */
    public static double depthM(Level level, int x, int y, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        FluidState top = level.getFluidState(p);
        if (!top.is(FluidTags.WATER)) return 0.0;
        double h = top.getHeight(level, p);
        int n = 0;
        while (n < MAX_DEPTH_BLOCKS && level.getFluidState(p).is(FluidTags.WATER)) {
            n++;
            p.move(0, -1, 0);
        }
        return SpotRule.columnDepthM(n, h);
    }

    public static Spot at(Level level, BlockPos water, FishingTuning t) {
        int y = water.getY();
        double d = depthM(level, water.getX(), y, water.getZ());
        boolean spot = d > 0 && SpotRule.isSpot((x, z) -> depthM(level, x, y, z), water.getX(), water.getZ(),
                t.spotMinWidth(), t.spotMinDepthM());
        return new Spot(spot, d, key(water), water.immutable());
    }

    public static long key(BlockPos p) { return ((long) (p.getX() >> 3) << 32) ^ ((p.getZ() >> 3) & 0xFFFFFFFFL); }

    /** K for the grayling at the slice spot on this game-year day. */
    public static double capacity(int dayOfYear, FishingTuning t) {
        double k = t.graylingCapacity() * t.spotQ() * t.stockX();
        return FishingSpecies.season(FishingSpecies.GRAYLING).spawning(dayOfYear, SPOT_TYPE) ? k * t.spawnCapacityX() : k;
    }

    /** Months since the first May, for regrowth. */
    static long monthIndex(long absoluteDay) {
        long year = Math.floorDiv(absoluteDay - 1, 365L);
        int doy = FishSeason.dayOfYear(absoluteDay);
        return year * 12 + Math.floorMod(FishSeason.month(doy) - 5, 12);
    }

    public static Stocks stocks(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(Stocks.FACTORY, "yearofice_fish_stocks");
    }

    /** The grayling stocks of every spot cell fished so far. */
    public static final class Stocks extends SavedData {
        static final SavedData.Factory<Stocks> FACTORY = new SavedData.Factory<>(Stocks::new, Stocks::load, null);

        private final Map<Long, double[]> cells = new HashMap<>();
        private final Map<Long, Long> spookedUntil = new HashMap<>();

        /** The stock now, regrown to this month. */
        public FishStock get(long key, long absoluteDay, FishingTuning t) {
            int doy = FishSeason.dayOfYear(absoluteDay);
            double k = capacity(doy, t);
            long mi = monthIndex(absoluteDay);
            double[] e = cells.get(key);
            if (e == null) return FishStock.full(k);
            FishStock s = new FishStock(e[0], Math.max(e[0], k)).rescale(k);
            long months = Math.min(12, mi - (long) e[1]);
            long m0 = (long) e[1];
            for (long i = 1; i <= months; i++) {
                int calMonth = Math.floorMod((int) ((m0 + i) % 12) + 4, 12) + 1;
                s = s.regrow(GRAYLING_R, FishStock.IMMIGRATION_RIVER, calMonth);
            }
            return s;
        }

        public void harvest(long key, long absoluteDay, FishingTuning t, int fish) {
            FishStock s = get(key, absoluteDay, t).harvest(fish);
            cells.put(key, new double[] {s.n(), monthIndex(absoluteDay)});
            setDirty();
        }

        public void set(long key, long absoluteDay, double n) {
            cells.put(key, new double[] {n, monthIndex(absoluteDay)});
            setDirty();
        }

        public boolean spooked(long key, long minute) { return spookedUntil.getOrDefault(key, Long.MIN_VALUE) > minute; }

        public void spook(long key, long untilMinute) { spookedUntil.put(key, untilMinute); }

        public static Stocks load(CompoundTag tag, HolderLookup.Provider regs) {
            Stocks s = new Stocks();
            for (Tag el : tag.getList("cells", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) el;
                s.cells.put(c.getLong("key"), new double[] {c.getDouble("n"), c.getLong("month")});
            }
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
            ListTag list = new ListTag();
            cells.forEach((k, v) -> {
                CompoundTag c = new CompoundTag();
                c.putLong("key", k);
                c.putDouble("n", v[0]);
                c.putLong("month", (long) v[1]);
                list.add(c);
            });
            tag.put("cells", list);
            return tag;
        }
    }
}
