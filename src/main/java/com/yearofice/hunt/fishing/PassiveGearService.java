package com.yearofice.hunt.fishing;

import com.yearofice.api.decay.DecayService;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.fishing.BiteModel;
import com.yearofice.sim.fishing.Draws;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishStock;
import com.yearofice.sim.fishing.FishingTuning;
import com.yearofice.sim.fishing.HookBait;
import com.yearofice.sim.fishing.PassiveGear;
import com.yearofice.sim.fishing.SizeDraw;
import com.yearofice.sim.fishing.SliceBite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Set lines, gill nets and fish traps, checked on a clock (Blueprint › Fishing methods › Passive gear; Waters and
 * fish › Passive gear while away). A piece of gear is a record in the overworld's saved data; it settles over the
 * dawn (06:00) and dusk (18:00) periods since it was last settled, whole game hours only, each period from its own
 * stream (seed, "trap", pos, day, period) through {@link PassiveGear#settle}, so settling every period as it
 * passes and settling a month at once give the same catch. The fish a piece holds wait in it until the player
 * checks it. The spot's species and stocks come from {@link SpotFish}; off a generated spot a set line or net
 * catches nothing (it is in no water the world knows).
 */
public final class PassiveGearService {
    private PassiveGearService() {}

    public enum Kind {
        SET_LINE(PassiveGear.Kind.SET_LINE), GILL_NET(PassiveGear.Kind.GILL_NET), FISH_TRAP(PassiveGear.Kind.WEIR);

        final PassiveGear.Kind sim;

        Kind(PassiveGear.Kind sim) { this.sim = sim; }

        public static Kind parse(String s) { return valueOf(s.toUpperCase(Locale.ROOT)); }
    }

    /** One fish held in a piece of gear. */
    public record Held(String species, int grams) {}

    /** A placed piece of gear. {@code segments} counts joined net segments, {@code hooks} baited hooks on a line. */
    public static final class Gear {
        public final Kind kind;
        public final BlockPos pos;
        public int segments, hooks;
        public double wingM;
        /** Bait key on a set line's hooks (empty: bare hooks catch nothing). */
        public String bait = "";
        public long lastMinute;
        public final List<Held> held = new ArrayList<>();

        Gear(Kind kind, BlockPos pos, int segments, int hooks, long lastMinute) {
            this.kind = kind;
            this.pos = pos;
            this.segments = segments;
            this.hooks = hooks;
            this.lastMinute = lastMinute;
        }

        public int holds() {
            return switch (kind) {
                case SET_LINE -> Math.max(1, hooks) * PassiveGear.HOLDS_HOOK;
                case GILL_NET -> Math.max(1, segments) * PassiveGear.HOLDS_NET_SEGMENT;
                case FISH_TRAP -> PassiveGear.HOLDS_WEIR;
            };
        }
    }

    /** What a settle did, for the player's line and the tests. */
    public record Settled(int caught, int escaped, int stolen, int eaten, int turnedAway, int periods) {}

    public static Book book(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(Book.FACTORY, "yearofice_fish_gear");
    }

    public static Gear place(ServerLevel level, Kind kind, BlockPos pos, int segmentsOrHooks, long nowMinute) {
        Gear g = new Gear(kind, pos.immutable(), kind == Kind.GILL_NET ? segmentsOrHooks : 0, kind == Kind.SET_LINE ? segmentsOrHooks : 0, nowMinute);
        book(level).gear.put(pos.asLong(), g);
        book(level).setDirty();
        return g;
    }

    public static Gear at(ServerLevel level, BlockPos pos) { return book(level).gear.get(pos.asLong()); }

    /** The nearest gear of a kind within {@code reach} blocks (horizontal; 3 vertical), or null. */
    public static Gear nearest(ServerLevel level, BlockPos pos, Kind kind, int reach) {
        Gear best = null;
        double bd = Double.MAX_VALUE;
        for (Gear g : book(level).gear.values()) {
            if (g.kind != kind || Math.abs(g.pos.getY() - pos.getY()) > 3) continue;
            double d = Math.sqrt(g.pos.distSqr(new BlockPos(pos.getX(), g.pos.getY(), pos.getZ())));
            if (d <= reach && d < bd) {
                bd = d;
                best = g;
            }
        }
        return best;
    }

    /** A net segment placed at {@code pos}: joins the net within reach (+1 segment) or starts one. */
    public static Gear addSegment(ServerLevel level, BlockPos pos, long nowMinute) {
        Gear g = nearest(level, pos, Kind.GILL_NET, 6);
        if (g == null) return place(level, Kind.GILL_NET, pos, 1, nowMinute);
        settle(level, g, nowMinute);
        g.segments++;
        book(level).setDirty();
        return g;
    }

    /** The block is gone: its gear leaves the book and what it held drops on the spot. */
    public static void remove(ServerLevel level, BlockPos pos) {
        Gear g = book(level).gear.remove(pos.asLong());
        if (g == null) return;
        for (Held h : g.held) {
            ItemStack fish = FishingService.landedFish(h.species(), h.grams());
            DecayService.get().stamp(level, fish, FoodState.RAW);
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, fish);
        }
        book(level).setDirty();
    }

    /** Periods end at minute-of-day 360 and 1080; returns the next boundary after {@code minute}. */
    static long nextBoundary(long minute) {
        long dayStart = Math.floorDiv(minute, 1440) * 1440;
        for (long b : new long[] {dayStart + 360, dayStart + 1080, dayStart + 1440 + 360}) if (b > minute) return b;
        return dayStart + 2880;
    }

    /** Settles the gear up to {@code now}, a period at a time, adding what it caught to its holds. */
    public static Settled settle(ServerLevel level, Gear g, long now) {
        FishingTuning t = FishingConfig.tuning();
        int caught = 0, escaped = 0, stolen = 0, eaten = 0, away = 0, periods = 0;
        long seed = level.getSeed() ^ Draws.name("trap");
        while (g.lastMinute < now) {
            long boundary = nextBoundary(g.lastMinute);
            long end = Math.min(boundary, now);
            boolean atBoundary = end == boundary;
            int hours = atBoundary ? (int) Math.max(1, Math.ceil((end - g.lastMinute) / 60.0)) : (int) ((end - g.lastMinute) / 60);
            if (hours <= 0) break;
            long start = g.lastMinute;
            long day = FishingClock.day(end - 1);
            int period = atBoundary ? (FishingClock.minuteOfDay(end) == 360 ? 0 : 1) : 2;
            List<SpotFish.Entry> fish = new ArrayList<>();
            double q = 0;
            String type = "";
            SpotFish.Here here = SpotFish.at(level, g.pos, FishSeason.dayOfYear(day), t);
            if (here.spot().isPresent()) {
                q = here.spot().get().quality();
                type = here.spot().get().type();
                fish.addAll(here.fish());
            }
            if (!fish.isEmpty()) {
                double[] mean = new double[fish.size()];
                double[] frac = new double[fish.size()];
                for (int i = 0; i < mean.length; i++) {
                    SpotFish.Entry e = fish.get(i);
                    FishStock st = SpotFish.stock(level, SliceSpots.key(g.pos), day, e);
                    frac[i] = st.fraction();
                    mean[i] = hourly(g, e, q, type, FishSeason.dayOfYear(day), st, here, level);
                }
                PassiveGear.Hour hour = new PassiveGear.Hour(mean);
                List<PassiveGear.Hour> hs = new ArrayList<>();
                for (int i = 0; i < hours; i++) hs.add(hour);
                List<PassiveGear.Catch> cs = PassiveGear.settle(level.getSeed(), g.pos.getX(), g.pos.getY(), g.pos.getZ(), day, period,
                        g.kind.sim, Math.max(0, g.holds() - g.held.size()), hs, false, false);
                int idx = 0;
                for (PassiveGear.Catch c : cs) {
                    switch (c.fate()) {
                        case KEPT -> {
                            SpotFish.Entry e = fish.get(c.species());
                            double u = Draws.u(seed, g.pos.asLong() ^ day, period * 64L + idx, SliceBite.SALT_SIZE);
                            double kg = SizeDraw.kg(FishingSpecies.byId(e.id()), 1.0, frac[c.species()], q, u, 1.0, 0.5);
                            g.held.add(new Held(e.id(), (int) Math.round(kg * 1000)));
                            SliceSpots.stocks(level).harvest(SliceSpots.speciesKey(SliceSpots.key(g.pos), e.id()), day, e.k(), e.r(), 1, start + hours * 30L);
                            caught++;
                        }
                        case ESCAPED -> escaped++;
                        case STOLEN -> stolen++;
                        case EATEN_IN_NET -> eaten++;
                        case TURNED_AWAY -> away++;
                    }
                    idx++;
                }
            }
            g.lastMinute = atBoundary ? end : start + hours * 60L;
            periods++;
        }
        book(level).setDirty();
        return new Settled(caught, escaped, stolen, eaten, away, periods);
    }

    /** One species' hourly mean at this gear. */
    static double hourly(Gear g, SpotFish.Entry e, double q, String spotType, int doy, FishStock st, SpotFish.Here here, ServerLevel level) {
        var data = FishingSpecies.data(e.id());
        double mSeason = data.season().factor(doy, spotType);
        return switch (g.kind) {
            case GILL_NET -> {
                boolean lake = spotType.startsWith("lake") || spotType.equals("weed_shelf");
                double width = here.spot().map(s -> Math.max(4.0, s.radiusM() * 2)).orElse(12.0);
                double cov = PassiveGear.netCoverage(g.segments, g.wingM, width, lake);
                yield PassiveGear.netHourly(data.netCPerHour(), q, cov, mSeason, st.fraction());
            }
            case FISH_TRAP -> PassiveGear.weirHourly(q, mSeason, st.fraction());
            case SET_LINE -> {
                var in = new SliceBite.Inputs(data.species(), data.season(), spotType, true, q, here.spot().map(s -> s.depthM()).orElse(2.0),
                        here.spot().map(s -> s.radiusM()).orElse(5.0), st.n(), st.k(), g.bait.isEmpty() ? HookBait.NONE : HookBait.fitted(g.bait, 1000, 100000),
                        doy, BiteModel.TimeBand.DAY, BiteModel.Weather.CLOUDY, 5.0, 0.0, false);
                yield g.hooks * PassiveGear.setLineHourly(SliceBite.pBite(in));
            }
        };
    }

    /** The player checks the gear: settle to now, then hand over everything it holds. */
    public static List<ItemStack> check(ServerLevel level, ServerPlayer p, Gear g, long now) {
        settle(level, g, now);
        List<ItemStack> out = new ArrayList<>();
        for (Held h : g.held) {
            ItemStack fish = FishingService.landedFish(h.species(), h.grams());
            DecayService.get().stamp(level, fish, FoodState.RAW);
            out.add(fish);
            ItemStack give = fish.copy();
            if (p != null && !p.getInventory().add(give)) p.drop(give, false);
        }
        g.held.clear();
        g.bait = "";
        book(level).setDirty();
        return out;
    }

    public static final class Book extends SavedData {
        static final SavedData.Factory<Book> FACTORY = new SavedData.Factory<>(Book::new, Book::load, null);
        final Map<Long, Gear> gear = new HashMap<>();

        public Map<Long, Gear> all() { return gear; }

        public static Book load(CompoundTag tag, HolderLookup.Provider regs) {
            Book b = new Book();
            for (Tag el : tag.getList("gear", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) el;
                BlockPos pos = BlockPos.of(c.getLong("pos"));
                Gear g = new Gear(Kind.parse(c.getString("kind")), pos, c.getInt("segments"), c.getInt("hooks"), c.getLong("last"));
                g.wingM = c.getDouble("wing_m");
                g.bait = c.getString("bait");
                for (Tag h : c.getList("held", Tag.TAG_COMPOUND)) {
                    CompoundTag ht = (CompoundTag) h;
                    g.held.add(new Held(ht.getString("species"), ht.getInt("grams")));
                }
                b.gear.put(pos.asLong(), g);
            }
            return b;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
            ListTag list = new ListTag();
            for (Gear g : gear.values()) {
                CompoundTag c = new CompoundTag();
                c.putString("kind", g.kind.name());
                c.putLong("pos", g.pos.asLong());
                c.putInt("segments", g.segments);
                c.putInt("hooks", g.hooks);
                c.putDouble("wing_m", g.wingM);
                if (!g.bait.isEmpty()) c.putString("bait", g.bait); // optional: absent in 0.4.0 saves, so the fixture holds
                c.putLong("last", g.lastMinute);
                ListTag held = new ListTag();
                for (Held h : g.held) {
                    CompoundTag ht = new CompoundTag();
                    ht.putString("species", h.species());
                    ht.putInt("grams", h.grams());
                    held.add(ht);
                }
                c.put("held", held);
                list.add(c);
            }
            tag.put("gear", list);
            return tag;
        }
    }
}
