package com.yearofice.ecosystem.pool;

import com.yearofice.api.env.EnvService;
import com.yearofice.api.weather.WeatherService;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.ai.ScentPoints;
import com.yearofice.ecosystem.ai.Sensing;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.GutPileBlock;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.ecosystem.spawn.Habitats;
import com.yearofice.ecosystem.spawn.SlotSpawner;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.ecosystem.Density;
import com.yearofice.sim.ecosystem.PoolModel;
import com.yearofice.sim.ecosystem.PoolModel.Indiv;
import com.yearofice.sim.ecosystem.PoolModel.Mode;
import com.yearofice.sim.time.Calendar;
import com.yearofice.worldgen.regions.Regions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * SavedData {@code yearofice_valley_pool} (RECONCILE 306, "The valley's bears"): per region, the bears, wolf packs,
 * wolverines and lynx that live beyond the 150 m active radius, as counted individuals (species density x region area,
 * seeded per world) with a home-range centre and an abstract position and state: ranging, denned, approaching a
 * scent, returning home. Scent sources post to {@link ScentPoints} / {@link ScentSources} / carcass entities with a
 * km-scale reach along the wind; an individual inside it rolls interest each game hour (curiosity x hunger x season)
 * and walks at its species speed. When its walk crosses a player's active radius it is instantiated at that edge
 * through {@link SlotSpawner} and continues under live AI; when the live body leaves ({@code SlotSpawner.demote}) it
 * returns to the pool. Live + pooled + dead always equals the population.
 */
public final class ValleyPool extends SavedData {
    public static final List<String> SPECIES = List.of("yearofice:grizzly", "yearofice:black_bear", "yearofice:wolf",
            "yearofice:wolverine", "yearofice:lynx");

    public record Area(String id, int x0, int x1, int z0, int z1) {
        double km2() { return (x1 - x0 + 1) * (double) (z1 - z0 + 1) / 1e6; }
    }

    /** A recorded arrival of an abstract individual at a source (tests and the debug command read these). */
    public record Reach(String indiv, String species, BlockPos source, double distM, long minute) {}

    /** Test hooks: a detached pool that {@link #of} hands out (and the hourly tick leaves alone), and a pinned wind (yaw toward, m/s). */
    public static volatile ValleyPool testPool;
    public static volatile double[] windOverride;
    /** A test pool's own regions and seed perturbation; null regions means the real ones. */
    public List<Area> areasOverride;
    public long salt;
    /** Test anchors standing in for players (x, z): an approach that crosses the radius around one instantiates. */
    public List<double[]> anchors;
    public static final List<Reach> REACHED = new ArrayList<>();

    final Map<String, Indiv> all = new LinkedHashMap<>();
    final java.util.Set<String> seeded = new java.util.HashSet<>();
    int out, in;

    public static ValleyPool of(ServerLevel level) {
        ValleyPool t = testPool;
        if (t != null) return t;
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(ValleyPool::new, ValleyPool::load, null), "yearofice_valley_pool");
    }

    // ---- save ----------------------------------------------------------------------------------------------------

    static ValleyPool load(CompoundTag tag, HolderLookup.Provider regs) {
        ValleyPool p = new ValleyPool();
        p.out = tag.getInt("out");
        p.in = tag.getInt("in");
        for (Tag t : tag.getList("seeded", Tag.TAG_STRING)) p.seeded.add(t.getAsString());
        for (Tag t : tag.getList("individuals", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Indiv a = new Indiv();
            a.id = c.getString("id");
            a.species = c.getString("species");
            a.region = c.getString("region");
            a.homeX = c.getDouble("home_x");
            a.homeZ = c.getDouble("home_z");
            a.x = c.getDouble("x");
            a.z = c.getDouble("z");
            a.tx = c.getDouble("tx");
            a.tz = c.getDouble("tz");
            a.hunger = c.getDouble("hunger");
            a.members = c.getInt("members");
            a.dead = c.getBoolean("dead");
            a.sourceKey = c.getLong("source");
            try { a.mode = Mode.valueOf(c.getString("mode")); } catch (IllegalArgumentException e) { a.mode = Mode.RANGING; }
            p.all.put(a.id, a);
        }
        return p;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        tag.putInt("out", out);
        tag.putInt("in", in);
        ListTag s = new ListTag();
        for (String k : seeded) s.add(net.minecraft.nbt.StringTag.valueOf(k));
        tag.put("seeded", s);
        ListTag l = new ListTag();
        for (Indiv a : all.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", a.id);
            c.putString("species", a.species);
            c.putString("region", a.region);
            c.putDouble("home_x", a.homeX);
            c.putDouble("home_z", a.homeZ);
            c.putDouble("x", a.x);
            c.putDouble("z", a.z);
            c.putDouble("tx", a.tx);
            c.putDouble("tz", a.tz);
            c.putDouble("hunger", a.hunger);
            c.putInt("members", a.members);
            c.putBoolean("dead", a.dead);
            c.putLong("source", a.sourceKey);
            // bodies are never saved: a live one comes back as an animal walking home
            c.putString("mode", a.live > 0 ? Mode.RETURNING.name() : a.mode.name());
            l.add(c);
        }
        tag.put("individuals", l);
        return tag;
    }

    // ---- seeding -------------------------------------------------------------------------------------------------

    List<Area> areas() {
        if (areasOverride != null) return areasOverride;
        List<Area> out = new ArrayList<>();
        for (Regions.Region r : Regions.all()) out.add(new Area(r.id(), r.x()[0], r.x()[1], r.z()[0], r.z()[1]));
        return out;
    }

    /** Seeds every region that has no individuals yet: count = density x km2 x the predator dial, rounded by a seeded roll. */
    public void ensureSeeded(ServerLevel level) {
        for (Area area : areas()) {
            for (String sp : SPECIES) {
                String key = area.id() + "|" + sp;
                if (seeded.contains(key) || !SpeciesBook.has(sp)) continue;
                seeded.add(key);
                setDirty();
                SpeciesDef d = SpeciesBook.get(sp);
                if (!d.spawns()) continue;
                var r = Thresholds.stream(level.getSeed() + salt, "pool", area.id().hashCode(), sp.hashCode());
                double expected = d.spawn().densityPerKm2() * EcoConfig.tuning().predatorDensityMult() * area.km2();
                int n = Density.count(expected, r.nextDouble());
                for (int i = 0; i < n; i++) {
                    Indiv a = new Indiv();
                    a.id = area.id() + "/" + sp.substring(sp.indexOf(':') + 1) + "#" + i;
                    a.species = sp;
                    a.region = area.id();
                    a.homeX = a.x = area.x0() + r.nextDouble() * (area.x1() - area.x0());
                    a.homeZ = a.z = area.z0() + r.nextDouble() * (area.z1() - area.z0());
                    a.hunger = 0.3 + r.nextDouble() * 0.5;
                    int g0 = d.spawn().groupMin(), g1 = d.spawn().groupMax();
                    a.members = g0 + r.nextInt(g1 - g0 + 1);
                    all.put(a.id, a);
                }
            }
        }
    }

    /** Test hook: forget everything and start from {@code areas} with a perturbed seed. */
    public void reset(List<Area> areas, long salt) {
        all.clear();
        seeded.clear();
        out = in = 0;
        areasOverride = areas;
        this.salt = salt;
        REACHED.clear();
    }

    public java.util.Collection<Indiv> individuals() { return all.values(); }

    public int population(String species) {
        int n = 0;
        for (Indiv a : all.values()) if (species == null || a.species.equals(species)) n++;
        return n;
    }

    /** {pooled (abstract, alive), live (a body is out), dead}. */
    public int[] census() {
        int[] c = new int[3];
        for (Indiv a : all.values()) {
            if (a.dead) c[2]++;
            else if (a.live > 0) c[1]++;
            else c[0]++;
        }
        return c;
    }

    public int instantiated() { return out; }

    public int returned() { return in; }

    // ---- scent sources -------------------------------------------------------------------------------------------

    public record Src(BlockPos pos, String kind, double reachM, double towardDeg) {}

    static double[] wind(ServerLevel level, BlockPos pos) {
        double[] w = windOverride;
        if (w != null) return w;
        var e = EnvService.get().wind(level, pos);
        return new double[] {Sensing.windTowardDeg(level, pos), e.ms()};
    }

    static double precip(ServerLevel level, BlockPos pos) {
        return WeatherService.installed() ? WeatherService.get().current(level, pos).precipMmPerH() : 0;
    }

    /** Every source that smells now, with its reach and the yaw the wind carries it toward. */
    public static List<Src> sources(ServerLevel level, long minute) {
        PoolConfig.PoolTuning t = PoolConfig.tuning();
        List<Src> out = new ArrayList<>();
        for (BlockPos p : ScentPoints.in(level)) {
            double strength = 1;
            if (level.isLoaded(p)) {
                var st = level.getBlockState(p);
                if (!st.is(EcosystemRegistry.GUT_PILE.get())) continue;
                strength = 1.0 - 0.25 * st.getValue(GutPileBlock.AGE);
            }
            out.add(src(level, p, "gut_pile", t.gutPileBaseM(), strength, t));
        }
        for (CarcassEntity c : level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(CarcassEntity.class), e -> !e.isRemoved())) {
            out.add(src(level, c.blockPosition(), "carcass", t.carcassBaseM(), 1, t));
        }
        for (ScentSources.Post p : ScentSources.live(level, minute).values()) {
            double base = switch (p.kind()) {
                case ScentSources.SMOKEHOUSE -> t.smokehouseBaseM();
                case ScentSources.RACK -> t.rackBaseM();
                default -> t.gutPileBaseM();
            };
            out.add(src(level, p.pos(), p.kind(), base, p.strength(), t));
        }
        return out;
    }

    private static Src src(ServerLevel level, BlockPos p, String kind, double base, double strength, PoolConfig.PoolTuning t) {
        double[] w = wind(level, p);
        return new Src(p, kind, PoolModel.reachM(base, strength, w[1], precip(level, p), t.reachScale()), w[0]);
    }

    // ---- the hour ------------------------------------------------------------------------------------------------

    static long lastHour = Long.MIN_VALUE;

    public static void onTick(ServerTickEvent.Post e) {
        if (testPool != null) return;
        if (e.getServer().getTickCount() % 40 != 0) return;
        ServerLevel level = e.getServer().overworld();
        long hour = SliceClock.minute(level) / 60;
        if (lastHour == Long.MIN_VALUE) lastHour = hour - 1;
        if (hour <= lastHour) return;
        ValleyPool pool = of(level);
        pool.ensureSeeded(level);
        for (long h = Math.max(lastHour + 1, hour - 72); h <= hour; h++) pool.hour(level, h * 60);
        lastHour = hour;
    }

    public static void clear() { lastHour = Long.MIN_VALUE; REACHED.clear(); testPool = null; windOverride = null; }

    static double speedMps(String species) { return Math.max(1.0, SpeciesBook.get(species).trotMps() * 0.5); }

    /** One game hour at {@code minute} (an absolute game minute): hunger, den, drift, new interest, then the walks. */
    public void hour(ServerLevel level, long minute) {
        int doy = Calendar.dayOfYearOfMinute(minute);
        List<Src> srcs = null;
        PoolConfig.PoolTuning t = PoolConfig.tuning();
        for (Indiv a : all.values()) {
            if (a.dead || a.live > 0) continue;
            PoolModel.Kind k = PoolModel.kind(a.species);
            a.hunger = PoolModel.hungrier(k, a.hunger);
            var r = Thresholds.stream(level.getSeed() + salt, "poolhr", a.id.hashCode(), minute / 60);
            double range = SpeciesBook.get(a.species).homeRangeM();
            if (com.yearofice.sim.ecosystem.Dormancy.denned(a.species, doy)) {
                a.mode = Mode.DENNED;
                a.x = a.homeX;
                a.z = a.homeZ;
                continue;
            }
            if (a.mode == Mode.DENNED) a.mode = Mode.RANGING;
            if (a.mode == Mode.RANGING) {
                a.x = a.homeX + (r.nextDouble() - 0.5) * 0.5 * range;
                a.z = a.homeZ + (r.nextDouble() - 0.5) * 0.5 * range;
                double roll = r.nextDouble();
                if (srcs == null) srcs = sources(level, minute);
                Src best = null;
                for (Src s : srcs) {
                    if (!PoolModel.inReach(s.pos().getX(), s.pos().getZ(), s.reachM(), s.towardDeg(), a.x, a.z)) continue;
                    if (best == null || s.reachM() > best.reachM()) best = s;
                }
                if (best != null && roll < PoolModel.interestPerHour(k, a.hunger, PoolModel.season(a.species, doy), t.interestScale())) {
                    a.mode = Mode.APPROACHING;
                    a.tx = best.pos().getX() + 0.5;
                    a.tz = best.pos().getZ() + 0.5;
                    a.sourceKey = best.pos().asLong();
                }
            }
        }
        advance(level, minute, 3600, srcs == null ? null : srcs);
        setDirty();
    }

    /** Walks every approaching or returning individual {@code seconds}; a source that is gone turns an approach home. */
    public void advance(ServerLevel level, long minute, double seconds, List<Src> srcs) {
        if (srcs == null) {
            boolean any = false;
            for (Indiv a : all.values()) if (a.mode == Mode.APPROACHING && !a.dead && a.live == 0) any = true;
            srcs = any ? sources(level, minute) : List.of();
        }
        List<double[]> viewers = new ArrayList<>();
        for (Player p : level.players()) if (!p.isSpectator()) viewers.add(new double[] {p.getX(), p.getZ()});
        if (anchors != null) viewers.addAll(anchors);
        double activeR = EcoConfig.tuning().activeRadiusM();
        for (Indiv a : all.values()) {
            if (a.dead || a.live > 0) continue;
            double speed = speedMps(a.species);
            if (a.mode == Mode.APPROACHING) {
                boolean still = false;
                for (Src s : srcs) if (s.pos().asLong() == a.sourceKey) still = true;
                if (!still) {
                    a.mode = Mode.RETURNING;
                    continue;
                }
                double x0 = a.x, z0 = a.z;
                PoolModel.walk(a, a.tx, a.tz, speed, seconds);
                double demoteR = EcoConfig.tuning().demoteRadiusM();
                for (double[] v : viewers) {
                    // a body only lives within the demote radius of a player: a pile beyond it is reached by the walk, not by a body that would be demoted on the way
                    if (Math.hypot(a.tx - v[0], a.tz - v[1]) > demoteR - 15) continue;
                    double[] e = PoolModel.entry(x0, z0, a.x, a.z, v[0], v[1], activeR * 0.95);
                    if (e != null && instantiate(level, a, e[0], e[1])) break;
                }
                if (a.live > 0) continue;
                if (Math.hypot(a.tx - a.x, a.tz - a.z) <= PoolModel.ARRIVE_M + 1) {
                    REACHED.add(new Reach(a.id, a.species, BlockPos.containing(a.tx, 64, a.tz), Math.hypot(a.tx - a.x, a.tz - a.z), minute));
                    a.hunger = Math.max(0, a.hunger - 0.5);
                    a.mode = Mode.RETURNING;
                }
            } else if (a.mode == Mode.RETURNING) {
                PoolModel.walk(a, a.homeX, a.homeZ, speed, seconds);
                if (Math.hypot(a.homeX - a.x, a.homeZ - a.z) <= PoolModel.ARRIVE_M + 1) a.mode = Mode.RANGING;
            }
        }
        setDirty();
    }

    /** The approach reached the active radius around {@code p}: its body (a pack's bodies) appears at the edge. */
    private boolean instantiate(ServerLevel level, Indiv a, double ex, double ez) {
        BlockPos at = new BlockPos((int) Math.floor(ex), 64, (int) Math.floor(ez));
        if (!level.hasChunkAt(at) || SlotSpawner.bodies() + a.members > EcoConfig.tuning().activeCap()) return false;
        int made = 0;
        for (int m = 0; m < a.members; m++) {
            BlockPos g = Habitats.surface(level, at.getX() + m * 2, at.getZ());
            if (SlotSpawner.spawnSpecies(level, a.species, g, "pool:" + a.id + "#" + m) != null) made++;
        }
        if (made == 0) return false;
        if (Boolean.getBoolean("yearofice.trace.scent")) System.out.printf("[scent-trace] POOL instantiate %s at %.0f,%.0f target %.0f,%.0f dist-to-target %.0f t=%d%n", a.species, ex, ez, a.tx, a.tz, Math.hypot(a.tx - ex, a.tz - ez), level.getGameTime());
        a.live = made;
        a.x = ex;
        a.z = ez;
        out++;
        setDirty();
        return true;
    }

    // ---- hooks from SlotSpawner ----------------------------------------------------------------------------------

    private static Indiv byKey(ValleyPool pool, String key) {
        if (key == null || !key.startsWith("pool:")) return null;
        String id = key.substring(5, key.lastIndexOf('#') < 0 ? key.length() : key.lastIndexOf('#'));
        return pool.all.get(id);
    }

    /** A live body went away (demoted beyond the radius, or unloaded): its individual walks home from where it stood. */
    public static void onLeave(ServerLevel level, String key, net.minecraft.world.entity.Entity e) {
        ValleyPool pool = of(level);
        Indiv a = byKey(pool, key);
        if (a == null || a.live <= 0) return;
        if (Boolean.getBoolean("yearofice.trace.scent") && e != null) System.out.printf("[scent-trace] POOL onLeave %s at %.0f,%.0f target %.0f,%.0f dist-to-target %.0f t=%d%n", a.species, e.getX(), e.getZ(), a.tx, a.tz, Math.hypot(a.tx - e.getX(), a.tz - e.getZ()), level.getGameTime());
        a.live--;
        if (e != null) {
            a.x = e.getX();
            a.z = e.getZ();
        }
        if (a.live == 0) {
            a.mode = Mode.RETURNING;
            a.sourceKey = 0;
            pool.in++;
        }
        pool.setDirty();
    }

    /** A live body died: the pack shrinks; an individual with no members left is dead for good. */
    public static void onDeath(ServerLevel level, String key) {
        ValleyPool pool = of(level);
        Indiv a = byKey(pool, key);
        if (a == null || a.live <= 0) return;
        a.live--;
        a.members--;
        if (a.members <= 0) {
            a.dead = true;
            a.live = 0;
            pool.in++;
        } else if (a.live == 0) {
            a.mode = Mode.RETURNING;
            pool.in++;
        }
        pool.setDirty();
    }

    // ---- debug ---------------------------------------------------------------------------------------------------

    /** {@code /yoi pool pull}: the nearest ranging individual of a species starts at 500 m and walks at the player. */
    public static Indiv pull(ServerLevel level, Player p, String species) {
        ValleyPool pool = of(level);
        pool.ensureSeeded(level);
        Indiv best = null;
        double bd = Double.MAX_VALUE;
        for (Indiv a : pool.all.values()) {
            if (!a.species.equals(species) || a.dead || a.live > 0) continue;
            double d = Math.hypot(a.homeX - p.getX(), a.homeZ - p.getZ());
            if (d < bd) { bd = d; best = a; }
        }
        if (best == null) return null;
        double ang = p.getRandom().nextDouble() * Math.PI * 2;
        best.x = p.getX() + Math.cos(ang) * 500;
        best.z = p.getZ() + Math.sin(ang) * 500;
        best.tx = p.getX();
        best.tz = p.getZ();
        best.mode = Mode.APPROACHING;
        best.sourceKey = p.blockPosition().asLong();
        List<Src> fake = List.of(new Src(p.blockPosition(), "debug", 1, 0));
        pool.advance(level, SliceClock.minute(level), 600, fake);
        return best;
    }
}
