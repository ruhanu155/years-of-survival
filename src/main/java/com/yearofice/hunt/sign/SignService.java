package com.yearofice.hunt.sign;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.practice.PracticeService;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.sim.build.Skill;
import com.yearofice.sim.hunt.Sign;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The hunt's sign api (Blueprint › Tracks and sign): {@link #lay} puts a track, scat, browse or bed down,
 * {@link #read} says what a reader sees, {@link #nearest} finds sign by species, and {@link #review} erases what age or
 * weather has taken. The only writer of the four sign blocks is this package.
 */
public final class SignService {
    private SignService() {}

    private static final Map<ResourceKey<Level>, Set<BlockPos>> LIVE = new ConcurrentHashMap<>();
    /** Fresh snow that hides a print (Blueprint: 2 cm hides small tracks). */
    static final double NEW_SNOW_CM = 2.0;
    private static volatile Sign.Rules rules;

    static double snowCm(ServerLevel l, BlockPos p) { return com.yearofice.api.env.EnvService.installed() ? com.yearofice.api.env.EnvService.get().snowCm(l, p) : 0; }

    public static Sign.Rules rules() {
        Sign.Rules r = rules;
        if (r == null) rules = r = Sign.Rules.standard();
        return r;
    }

    static void track(ServerLevel l, BlockPos p) { LIVE.computeIfAbsent(l.dimension(), k -> ConcurrentHashMap.newKeySet()).add(p.immutable()); }

    static void untrack(ServerLevel l, BlockPos p) {
        Set<BlockPos> s = LIVE.get(l.dimension());
        if (s != null) s.remove(p);
    }

    public static void clearAll() { LIVE.clear(); }

    public static double now(Level l) { return ClockService.installed() ? SliceClock.minuteD(l) : 0; }

    /** The ground a print at {@code foot} would be pressed in, or null where nothing holds a print (air below, stone, water). */
    public static @Nullable Sign.Substrate substrateAt(ServerLevel l, BlockPos foot) {
        BlockState here = l.getBlockState(foot), below = l.getBlockState(foot.below());
        if (here.is(Blocks.SNOW) || below.is(Blocks.SNOW) || below.is(Blocks.SNOW_BLOCK) || below.is(Blocks.POWDER_SNOW)) return Sign.Substrate.SNOW;
        if (below.is(Blocks.MUD) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.CLAY) || below.is(Blocks.ROOTED_DIRT)) return Sign.Substrate.MUD;
        if (below.is(net.minecraft.tags.BlockTags.SAND) || below.is(Blocks.GRAVEL)) return Sign.Substrate.SAND;
        if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.PODZOL) || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.MYCELIUM)) return Sign.Substrate.MOSS;
        return null;
    }

    /** Lays one piece of sign standing at {@code foot}; returns where it went (a snow layer lifts it one up) or null if the ground takes none. */
    public static @Nullable BlockPos lay(ServerLevel l, BlockPos foot, Sign.Kind kind, String species, double dx, double dz, int gait) {
        Sign.Substrate sub = substrateAt(l, foot);
        if (sub == null) return null;
        BlockPos at = l.getBlockState(foot).is(Blocks.SNOW) ? foot.above() : foot;
        if (!l.getBlockState(at).isAir()) return null;
        l.setBlock(at, SignRegistry.block(kind).defaultBlockState(), 2);
        if (!(l.getBlockEntity(at) instanceof SignBlockEntity be)) return null;
        double n = Math.hypot(dx, dz);
        be.species = species.substring(species.indexOf(':') + 1);
        be.madeMin = now(l);
        be.hx = n < 1e-6 ? 0 : (float) (dx / n);
        be.hz = n < 1e-6 ? 0 : (float) (dz / n);
        be.gait = gait;
        be.substrate = sub;
        be.snowCm0 = snowCm(l, at);
        be.setChanged();
        l.scheduleTick(at, be.getBlockState().getBlock(), reviewTicks());
        return at;
    }

    /** Ticks between two looks at the weather: ten game minutes. */
    static int reviewTicks() {
        int ds = ClockService.installed() ? Math.max(1, ClockService.get().dsPerTick()) : 1;
        return (int) Math.min(Integer.MAX_VALUE / 2, Math.max(20, 10 * 600L / ds));
    }

    /** The weather truth at the sign: the env layer's precipitation (so the storm table and {@code /yoi env set} both work). */
    public static Sign.Precip precipAt(ServerLevel l, BlockPos p) {
        if (!com.yearofice.api.env.EnvService.installed() || !l.canSeeSky(p)) return Sign.Precip.NONE;
        return switch (com.yearofice.api.env.EnvService.get().precip(l, p)) {
            case DRIZZLE, RAIN, FREEZING_RAIN -> Sign.Precip.RAIN;
            case SNOW, MIXED, HAIL -> Sign.Precip.SNOW;
            default -> Sign.Precip.NONE;
        };
    }

    /** Removes the sign at {@code pos} if age or the weather has erased it, else looks again later. Returns true if removed. */
    public static boolean review(ServerLevel l, BlockPos pos) {
        if (!(l.getBlockEntity(pos) instanceof SignBlockEntity be)) return false;
        Sign.Kind kind = ((SignBlock) be.getBlockState().getBlock()).kind;
        boolean newSnow = be.substrate == Sign.Substrate.SNOW && kind == Sign.Kind.TRACK && snowCm(l, pos) - be.snowCm0 >= NEW_SNOW_CM;
        if (newSnow || Sign.erased(rules(), kind, be.substrate, now(l) - be.madeMin, precipAt(l, pos))) {
            l.removeBlock(pos, false);
            return true;
        }
        l.scheduleTick(pos, be.getBlockState().getBlock(), reviewTicks());
        return false;
    }

    /** What the sign at {@code pos} shows, at full confidence. */
    public static @Nullable Sign.Read read(ServerLevel l, BlockPos pos) { return read(l, pos, null); }

    /** What {@code p} makes of the sign at {@code pos}: confidence grows with Trapping practice. */
    public static @Nullable Sign.Read read(ServerLevel l, BlockPos pos, @Nullable Player p) {
        if (!(l.getBlockEntity(pos) instanceof SignBlockEntity be)) return null;
        double conf = 1.0;
        if (p != null) {
            try {
                conf = Sign.confidence(PracticeService.get().reps(p, Skill.TRAPPING), Skill.TRAPPING.n);
            } catch (IllegalStateException e) {
                // no practice service (bare test world): a stranger to tracking
                conf = Sign.confidence(0, Skill.TRAPPING.n);
            }
        }
        return Sign.read(rules(), be.species, ((SignBlock) be.getBlockState().getBlock()).kind, be.madeMin, now(l), be.hx, be.hz, be.gait, conf);
    }

    /** The nearest sign of {@code species} (null or "" for any) within {@code radius} blocks. */
    public static @Nullable BlockPos nearest(ServerLevel l, BlockPos from, double radius, @Nullable String species) {
        return nearest(l, from, radius, species, Double.MAX_VALUE);
    }

    /** As {@link #nearest(ServerLevel, BlockPos, double, String)} but only sign younger than {@code maxAgeMin} (following a trail toward its fresh end). */
    public static @Nullable BlockPos nearest(ServerLevel l, BlockPos from, double radius, @Nullable String species, double maxAgeMin) {
        Set<BlockPos> set = LIVE.get(l.dimension());
        if (set == null) return null;
        String sp = species == null ? "" : species.substring(species.indexOf(':') + 1);
        BlockPos best = null;
        double bestD = radius * radius;
        for (BlockPos p : set) {
            double d = p.distSqr(from);
            if (d > bestD || !l.isLoaded(p) || !(l.getBlockEntity(p) instanceof SignBlockEntity be)) continue;
            if (!sp.isEmpty() && !be.species.equals(sp)) continue;
            if (now(l) - be.madeMin >= maxAgeMin) continue;
            best = p;
            bestD = d;
        }
        return best;
    }

    /** Every live sign within {@code radius} of {@code from} (debug). */
    public static int clear(ServerLevel l, BlockPos from, double radius) {
        Set<BlockPos> set = LIVE.get(l.dimension());
        if (set == null) return 0;
        int n = 0;
        for (BlockPos p : java.util.List.copyOf(set)) {
            if (p.distSqr(from) <= radius * radius) {
                l.removeBlock(p, false);
                n++;
            }
        }
        return n;
    }

    public static java.util.List<BlockPos> within(ServerLevel l, BlockPos from, double radius) {
        Set<BlockPos> set = LIVE.get(l.dimension());
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        if (set != null) for (BlockPos p : set) if (p.distSqr(from) <= radius * radius) out.add(p);
        out.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(from)));
        return out;
    }
}
