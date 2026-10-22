package com.yearofice.ecosystem.spawn;

import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.species.MooseEntity;
import com.yearofice.ecosystem.species.SnowshoeHareEntity;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.ecosystem.species.bird.WillowPtarmiganEntity;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.ecosystem.Density;
import com.yearofice.sim.ecosystem.EcoTuning;
import com.yearofice.sim.ecosystem.HareCycle;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The slice's fixed-slot spawner (Ready for development M2.5: "the fixed-slot spawner ... the first form of
 * PopulationSpawner"), in {@code ecosystem.spawn} as the Director placed it. No vanilla spawning places our
 * animals.
 *
 * <p>Each species divides the world into square cells (hare 64 m, ptarmigan 128 m, moose 512 m). A cell's slots
 * come from the seed and its habitat: 24 hashed columns are tested against the species file's habitat names, the
 * expected count is density × cell area × habitat share (× the hare cycle's year for hares), and the slots sit on
 * hashed habitat columns, so the same moose returns to the same willow flat. Every 40 ticks a slot within the
 * Active radius of a player gets its body, unless it is empty after a death; a body beyond the demote radius of
 * every player is removed (its slot keeps it). Bodies are never saved, so a world holds one truth.
 */
public final class SlotSpawner {
    private SlotSpawner() {}

    public record Slot(String key, String species, BlockPos pos) {}

    record Cell(long minute, double share, List<Slot> slots, boolean complete) {}

    record Kind(String id, int cellM, int groupMin, int groupMax, Supplier<? extends EntityType<? extends YoiAnimal>> type,
                ToDoubleFunction<EcoTuning> density, boolean generic) {
        Kind(String id, int cellM, int groupMin, int groupMax, Supplier<? extends EntityType<? extends YoiAnimal>> type,
             ToDoubleFunction<EcoTuning> density) {
            this(id, cellM, groupMin, groupMax, type, density, false);
        }
    }

    static final List<Kind> KINDS = List.of(
            new Kind(SnowshoeHareEntity.ID, 64, 1, 1, EcosystemRegistry.SNOWSHOE_HARE, EcoTuning::hareDensityPerKm2),
            new Kind(WillowPtarmiganEntity.ID, 128, 2, 5, EcosystemRegistry.WILLOW_PTARMIGAN, EcoTuning::ptarmiganGroupsPerKm2),
            new Kind(MooseEntity.ID, 512, 1, 1, EcosystemRegistry.MOOSE, EcoTuning::mooseDensityPerKm2));

    /**
     * The three slice species plus every species file with a {@code "spawn"} block (M5: the roster is data). A file's
     * {@code entity_type} names the body; one that names {@code yearofice:critter} is a data-only species, a generic
     * body that takes its species from the file (Minecraft development › Data-only species).
     */
    @SuppressWarnings("unchecked")
    static List<Kind> kinds() {
        List<Kind> out = new ArrayList<>(KINDS);
        for (SpeciesDef d : SpeciesBook.all()) {
            if (!d.spawns() || !EcoConfig.flagOn(d.contentFlag())) continue;
            boolean dup = false;
            for (Kind k : KINDS) if (k.id().equals(d.id())) dup = true;
            if (dup) continue;
            var rl = net.minecraft.resources.ResourceLocation.parse(d.entityType());
            if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) continue;
            SpeciesDef.Spawn sp = d.spawn();
            out.add(new Kind(d.id(), sp.cellM(), sp.groupMin(), sp.groupMax(),
                    () -> (EntityType<? extends YoiAnimal>) net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(rl),
                    t -> sp.densityPerKm2() * densityMult(sp.densityGroup(), t), !d.entityType().equals(d.id())));
        }
        return out;
    }

    /** The density dial of a species file's {@code density_group} (small game, big game or predator). */
    static double densityMult(String group, EcoTuning t) {
        return switch (group) {
            case "big_game" -> t.bigGameDensityMult();
            case "predator" -> t.predatorDensityMult();
            default -> t.smallGameDensityMult();
        };
    }

    /** Every species the spawner places, by id (for the debug command's suggestions). */
    public static List<String> speciesIds() {
        List<String> out = new ArrayList<>();
        for (Kind k : kinds()) out.add(k.id());
        return out;
    }

    private static final Map<String, Cell> CELLS = new ConcurrentHashMap<>();
    private static final Map<String, UUID> BODIES = new ConcurrentHashMap<>();

    public static boolean has(String key) { return BODIES.containsKey(key); }

    public static void clear() {
        CELLS.clear();
        BODIES.clear();
        SIGNED.clear();
    }

    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 40 != 0) return;
        ServerLevel level = e.getServer().overworld();
        EcoTuning t = EcoConfig.tuning();
        demote(level, t);
        RareSighting.tick(level);
        List<? extends Player> players = level.players();
        if (players.isEmpty()) return;
        long minute = SliceClock.minute(level);
        SlotData data = SlotData.of(level);
        int alive = BODIES.size();
        for (Player p : players) {
            if (p.isSpectator()) continue;
            int doy = Calendar.dayOfYearOfMinute(minute);
            for (Kind k : kinds()) {
                // Bears are in their dens from October 20 to April 20 (M5 debt: hibernation dates).
                if (com.yearofice.sim.ecosystem.Dormancy.denned(k.id(), doy)) continue;
                int r = (int) Math.ceil(t.activeRadiusM());
                int x0 = Math.floorDiv(p.getBlockX() - r, k.cellM()), x1 = Math.floorDiv(p.getBlockX() + r, k.cellM());
                int z0 = Math.floorDiv(p.getBlockZ() - r, k.cellM()), z1 = Math.floorDiv(p.getBlockZ() + r, k.cellM());
                for (int cx = x0; cx <= x1; cx++) {
                    for (int cz = z0; cz <= z1; cz++) {
                        Cell c = cell(level, k, cx, cz, minute);
                        if (c == null) continue;
                        for (Slot s : c.slots()) {
                            if (alive >= t.activeCap()) return;
                            if (BODIES.containsKey(s.key()) || data.empty(s.key(), minute)) continue;
                            if (p.blockPosition().distSqr(s.pos()) > t.activeRadiusM() * t.activeRadiusM()) {
                                scatterSign(level, k, s, minute);
                                continue;
                            }
                            if (!level.isPositionEntityTicking(s.pos())) continue;
                            if (spawnKind(level, k, s.pos(), s.key()) != null) alive++;
                        }
                    }
                }
            }
        }
    }

    private static final Map<String, Long> SIGNED = new ConcurrentHashMap<>();

    /**
     * A slot whose chunk is loaded but whose body has not appeared (the player is beyond the Active radius) still
     * leaves sign: tracks, browse and scat within 30 m of the slot, once a game day. Big game only; hares and birds
     * are met, not tracked. The sign points the player at the animal before its body exists, which is how a hunt by
     * sign starts (Bestiary › Reading sign).
     */
    static void scatterSign(ServerLevel level, Kind k, Slot s, long minute) {
        if (k.cellM() < 256) return;
        Long last = SIGNED.get(s.key());
        if (last != null && minute - last < Calendar.MINUTES_PER_DAY) return;
        if (!level.hasChunkAt(s.pos())) return;
        SIGNED.put(s.key(), minute);
        var r = Thresholds.stream(level.getSeed(), "slotsign", s.key().hashCode(), (int) (minute / Calendar.MINUTES_PER_DAY), 0);
        double ang = r.nextDouble() * Math.PI * 2;
        double dx = Math.cos(ang), dz = Math.sin(ang);
        com.yearofice.sim.hunt.Sign.Kind[] kinds = {com.yearofice.sim.hunt.Sign.Kind.TRACK, com.yearofice.sim.hunt.Sign.Kind.TRACK,
                com.yearofice.sim.hunt.Sign.Kind.BROWSE, com.yearofice.sim.hunt.Sign.Kind.TRACK, com.yearofice.sim.hunt.Sign.Kind.SCAT,
                com.yearofice.sim.hunt.Sign.Kind.TRACK, com.yearofice.sim.hunt.Sign.Kind.BED};
        for (int i = 0; i < kinds.length; i++) {
            double dist = 4 + i * 4 + r.nextInt(3);
            int x = (int) Math.round(s.pos().getX() - dx * dist + r.nextInt(3) - 1), z = (int) Math.round(s.pos().getZ() - dz * dist + r.nextInt(3) - 1);
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            BlockPos foot = Habitats.surface(level, x, z);
            com.yearofice.hunt.sign.SignService.lay(level, foot, kinds[i], s.species(), dx, dz, 0);
        }
    }

    static void demote(ServerLevel level, EcoTuning t) {
        double r2 = t.demoteRadiusM() * t.demoteRadiusM();
        Iterator<Map.Entry<String, UUID>> it = BODIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, UUID> en = it.next();
            Entity e = level.getEntity(en.getValue());
            if (e == null || e.isRemoved()) {
                com.yearofice.ecosystem.pool.ValleyPool.onLeave(level, en.getKey(), e);
                it.remove();
                continue;
            }
            boolean near = false;
            for (Player p : level.players()) if (!p.isSpectator() && p.distanceToSqr(e) <= r2) near = true;
            if (!near) {
                com.yearofice.ecosystem.pool.ValleyPool.onLeave(level, en.getKey(), e);
                e.discard();
                it.remove();
            }
        }
    }

    /** Minutes between refreshes of a cell whose sampled columns are not all loaded yet. */
    static final int PARTIAL_REFRESH_MIN = 20;

    /**
     * The cell's slots. Each of the 24 hashed columns decides for itself (its own hash against the per-column
     * probability that gives the species' density × cell area × habitat share), so the slots a player sees do not
     * depend on which chunks were loaded when the cell was first looked at: a cell at the edge of the loaded area
     * yields its riverbank slots as soon as those columns load, and the same columns stay slots as more of the cell
     * loads (M6 hunt proxy: cells were computed only once their centre chunk was loaded, so a player walking a
     * creek never met the moose whose cell centre lay 300 m inland). A fully sampled cell is kept for a game day; a
     * partly loaded one is re-sampled every {@link #PARTIAL_REFRESH_MIN} minutes.
     */
    static Cell cell(ServerLevel level, Kind k, int cx, int cz, long minute) {
        String key = k.id() + ":" + cx + ":" + cz;
        Cell c = CELLS.get(key);
        if (c != null && minute - c.minute() < (c.complete() ? Calendar.MINUTES_PER_DAY : PARTIAL_REFRESH_MIN)) return c;
        SpeciesDef d = SpeciesBook.get(k.id());
        var r = Thresholds.stream(level.getSeed(), "slots", k.id().hashCode(), cx, cz);
        double mult = k.id().equals(SnowshoeHareEntity.ID) ? hareMultiplier(level, minute) : com.yearofice.ecosystem.small.LemmingIndex.multiplier(k.id(), level);
        int samples = 24, tested = 0, habitatN = 0;
        // expected slots over the whole cell = density × area × share × mult; per tested habitat column that is this probability
        double q = Density.inCell(k.density().applyAsDouble(EcoConfig.tuning()), k.cellM(), 1.0, mult) / samples;
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < samples; i++) {
            int x = cx * k.cellM() + r.nextInt(k.cellM());
            int z = cz * k.cellM() + r.nextInt(k.cellM());
            double u = r.nextDouble();
            int members = k.groupMin() + r.nextInt(k.groupMax() - k.groupMin() + 1);
            int[] jit = new int[members * 2];
            for (int j = 0; j < jit.length; j++) jit[j] = r.nextInt(5) - 2;
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            tested++;
            BlockPos g = Habitats.surface(level, x, z);
            if (!Habitats.any(level, g, d.habitat())) continue;
            habitatN++;
            if (u >= q) continue;
            for (int m = 0; m < members; m++) {
                BlockPos pm = m == 0 ? g : Habitats.surface(level, g.getX() + jit[m * 2], g.getZ() + jit[m * 2 + 1]);
                slots.add(new Slot(key + ":" + i + (members > 1 ? "." + m : ""), k.id(), pm));
            }
        }
        double share = tested == 0 ? 0 : habitatN / (double) tested;
        c = new Cell(minute, share, List.copyOf(slots), tested == samples);
        CELLS.put(key, c);
        return c;
    }

    /** The hare cycle's multiplier for this year, or the pinned year from {@code [ecosystem] hare_cycle_year}. */
    public static double hareMultiplier(ServerLevel level, long minute) {
        int pinned = EcoConfig.hareCycleYear();
        if (pinned >= 0) return HareCycle.TABLE[pinned];
        return HareCycle.multiplier(HareCycle.phase(level.getSeed()), (Calendar.yearOfDay(Calendar.absDayOfMinute(minute)) - 1));
    }

    public static int cycleYear(ServerLevel level, long minute) {
        int pinned = EcoConfig.hareCycleYear();
        return pinned >= 0 ? pinned : HareCycle.cycleYear(HareCycle.phase(level.getSeed()), (Calendar.yearOfDay(Calendar.absDayOfMinute(minute)) - 1));
    }

    /** Stops tracking a body (a GameTest keeps its own animals with no player near; the demote pass would remove them). */
    public static void untrack(String key) { BODIES.remove(key); }

    /** Places the body of a kind; a generic body is given the kind's species. */
    static YoiAnimal spawnKind(ServerLevel level, Kind k, BlockPos pos, String key) {
        YoiAnimal a = spawn(level, k.type().get(), pos, key, k.generic() ? k.id() : null);
        return a;
    }

    /** Places a species by its file, for the debug command and GameTests: a registered body or the generic one. */
    public static YoiAnimal spawnSpecies(ServerLevel level, String speciesId, BlockPos pos, String key) {
        for (Kind k : kinds()) if (k.id().equals(speciesId)) return spawnKind(level, k, pos, key);
        return null;
    }

    /** Places a body, tagged with its slot. Debug spawns use a "debug:" key and are counted like the rest. */
    public static <T extends YoiAnimal> T spawn(ServerLevel level, EntityType<T> type, BlockPos pos, String key) {
        return spawn(level, type, pos, key, null);
    }

    public static <T extends YoiAnimal> T spawn(ServerLevel level, EntityType<T> type, BlockPos pos, String key, String species) {
        T a = type.create(level);
        if (a == null) return null;
        if (species != null && a instanceof com.yearofice.ecosystem.species.SmallGameEntity g) g.setSpecies(species);
        long h = Thresholds.stream(level.getSeed(), "sex", key.hashCode()).nextUInt();
        a.setMale((h & 1) == 0);
        a.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, (float) (h % 360), 0);
        a.setSlot(key, pos);
        if (!level.addFreshEntity(a)) return null;
        BODIES.put(key, a.getUUID());
        return a;
    }

    /** A death: the slot waits out the species' respawn days. */
    public static void reportDeath(ServerLevel level, YoiAnimal a, boolean byPlayer) {
        String key = a.slotKey();
        if (key.isEmpty()) return;
        BODIES.remove(key);
        if (key.startsWith("debug:")) return;
        if (key.startsWith("pool:")) { com.yearofice.ecosystem.pool.ValleyPool.onDeath(level, key); return; }
        SpeciesDef d = a.species();
        SlotData.of(level).harvest(key, SliceClock.minute(level) + Math.round(d.respawnDays() * Calendar.MINUTES_PER_DAY));
    }

    /** A snare took a hare: the nearest hare slot within its home range empties, and its body goes. */
    public static void harvestNear(ServerLevel level, BlockPos at, long minute, double respawnDays) {
        SpeciesDef d = SpeciesBook.get(SnowshoeHareEntity.ID);
        Slot best = null;
        double bd = d.homeRangeM() * d.homeRangeM();
        SlotData data = SlotData.of(level);
        for (Map.Entry<String, Cell> e : CELLS.entrySet()) {
            for (Slot s : e.getValue().slots()) {
                if (!s.species().equals(SnowshoeHareEntity.ID) || data.empty(s.key(), minute)) continue;
                double dd = s.pos().distSqr(at);
                if (dd < bd) {
                    bd = dd;
                    best = s;
                }
            }
        }
        if (best == null) return;
        data.harvest(best.key(), minute + Math.round(respawnDays * Calendar.MINUTES_PER_DAY));
        UUID id = BODIES.remove(best.key());
        if (id != null) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
        }
    }

    /** For {@code /yoi eco density}: cached slots and live bodies by species near a point. */
    public static Map<String, int[]> census(ServerLevel level, BlockPos at, double radius) {
        Map<String, int[]> out = new HashMap<>();
        long minute = SliceClock.minute(level);
        SlotData data = SlotData.of(level);
        for (Kind k : kinds()) out.put(k.id(), new int[3]);
        for (Cell c : CELLS.values()) {
            for (Slot s : c.slots()) {
                if (s.pos().distSqr(at) > radius * radius) continue;
                int[] v = out.get(s.species());
                if (v == null) continue;
                v[0]++;
                if (data.empty(s.key(), minute)) v[1]++;
                if (BODIES.containsKey(s.key())) v[2]++;
            }
        }
        return out;
    }

    public static int bodies() { return BODIES.size(); }
}
