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

    record Cell(long minute, double share, List<Slot> slots) {}

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
                            if (p.blockPosition().distSqr(s.pos()) > t.activeRadiusM() * t.activeRadiusM()) continue;
                            if (!level.isPositionEntityTicking(s.pos())) continue;
                            if (spawnKind(level, k, s.pos(), s.key()) != null) alive++;
                        }
                    }
                }
            }
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

    /** The cell's slots, computed when its centre chunk is loaded and kept for a game day. */
    static Cell cell(ServerLevel level, Kind k, int cx, int cz, long minute) {
        String key = k.id() + ":" + cx + ":" + cz;
        Cell c = CELLS.get(key);
        if (c != null && minute - c.minute() < Calendar.MINUTES_PER_DAY) return c;
        BlockPos center = new BlockPos(cx * k.cellM() + k.cellM() / 2, 64, cz * k.cellM() + k.cellM() / 2);
        if (!level.hasChunkAt(center)) return c;
        SpeciesDef d = SpeciesBook.get(k.id());
        var r = Thresholds.stream(level.getSeed(), "slots", k.id().hashCode(), cx, cz);
        List<BlockPos> habitat = new ArrayList<>();
        int samples = 24, tested = 0;
        for (int i = 0; i < samples; i++) {
            int x = cx * k.cellM() + r.nextInt(k.cellM());
            int z = cz * k.cellM() + r.nextInt(k.cellM());
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            tested++;
            BlockPos g = Habitats.surface(level, x, z);
            if (Habitats.any(level, g, d.habitat())) habitat.add(g);
        }
        double share = tested == 0 ? 0 : habitat.size() / (double) tested;
        double mult = k.id().equals(SnowshoeHareEntity.ID) ? hareMultiplier(level, minute) : com.yearofice.ecosystem.small.LemmingIndex.multiplier(k.id(), level);
        double expected = Density.inCell(k.density().applyAsDouble(EcoConfig.tuning()), k.cellM(), share, mult);
        int n = habitat.isEmpty() ? 0 : Density.count(expected, r.nextDouble());
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BlockPos at = habitat.get(r.nextInt(habitat.size()));
            int members = k.groupMin() + r.nextInt(k.groupMax() - k.groupMin() + 1);
            for (int m = 0; m < members; m++) {
                BlockPos p = m == 0 ? at : Habitats.surface(level, at.getX() + r.nextInt(5) - 2, at.getZ() + r.nextInt(5) - 2);
                slots.add(new Slot(key + ":" + i + (members > 1 ? "." + m : ""), k.id(), p));
            }
        }
        c = new Cell(minute, share, List.copyOf(slots));
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
