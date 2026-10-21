package com.yearofice.ecosystem.herd;

import com.yearofice.api.story.AmbitionTracker;
import com.yearofice.ecosystem.ai.Sensing;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.companion.CompanionConfig;
import com.yearofice.ecosystem.companion.CompanionRegistry;
import com.yearofice.ecosystem.species.rig.BigGameEntity;
import com.yearofice.sim.companion.LeadFenceMath;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * B12, the caribou lead fence (Land animals in depth › B12). {@link #scan} reads {@code yearofice:lead_fence} blocks into
 * wings (connected runs, four blocks to a segment), finds the mouth where two wings end 15-25 m apart (or a
 * {@code caribou_snare} stands) and the blind. {@link #consider} decides for a band meeting a wing: within 30 degrees of
 * its heading and unscented it follows the wing to the mouth with the chance {@link LeadFenceMath#followChance} gives
 * for the wing's length. Followers walk to the mouth ({@link #walk}), stand there 30-90 s, and may be held by the
 * snare. Taking a caribou within 25 m of a fence closes the ambition "At the fence".
 */
public final class LeadFence {
    private LeadFence() {}

    public static final int SCAN_RADIUS = 40;
    public static final double WING_REACH_M = 30, MOUTH_MAX_M = 25;

    public record Wing(List<BlockPos> cells, int segments, Vec3 farEnd, Vec3 mouthEnd, double headingDeg) {
        public double lengthM() { return cells.size(); }
    }

    /** {@code mouth} is null when the fence has no mouth yet; {@code snares} are the caribou snare blocks found. */
    public record Layout(List<Wing> wings, Vec3 mouth, List<BlockPos> snares) {
        public Wing nearest(Vec3 at) {
            Wing best = null;
            double bd = Double.MAX_VALUE;
            for (Wing w : wings) for (BlockPos c : w.cells()) {
                double d = Vec3.atCenterOf(c).distanceToSqr(at);
                if (d < bd) {
                    bd = d;
                    best = w;
                }
            }
            return best == null || Math.sqrt(bd) > WING_REACH_M ? null : best;
        }
    }

    // ---- scanning ------------------------------------------------------------------------------------------------------

    public static Layout scan(ServerLevel level, BlockPos center, int radius) {
        Set<BlockPos> fence = new HashSet<>();
        List<BlockPos> snares = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        var lf = CompanionRegistry.LEAD_FENCE.get();
        var sn = CompanionRegistry.CARIBOU_SNARE.get();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) for (int y = -3; y <= 3; y++) {
            m.set(center.getX() + x, center.getY() + y, center.getZ() + z);
            if (!level.isLoaded(m)) continue;
            var b = level.getBlockState(m).getBlock();
            if (b == lf) fence.add(m.immutable());
            else if (b == sn) snares.add(m.immutable());
        }
        List<Wing> wings = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos start : fence) {
            if (!seen.add(start)) continue;
            List<BlockPos> comp = new ArrayList<>();
            java.util.ArrayDeque<BlockPos> q = new java.util.ArrayDeque<>();
            q.add(start);
            while (!q.isEmpty()) {
                BlockPos p = q.poll();
                comp.add(p);
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) {
                    BlockPos n = p.offset(dx, dy, dz);
                    if (fence.contains(n) && seen.add(n)) q.add(n);
                }
            }
            BlockPos a = farthest(comp, comp.get(0)), b = farthest(comp, a);
            wings.add(new Wing(comp, comp.size() / 4, Vec3.atCenterOf(a), Vec3.atCenterOf(b), 0));
        }
        return withMouth(wings, snares);
    }

    private static BlockPos farthest(List<BlockPos> cells, BlockPos from) {
        BlockPos best = from;
        double bd = -1;
        for (BlockPos c : cells) {
            double d = c.distSqr(from);
            if (d > bd) {
                bd = d;
                best = c;
            }
        }
        return best;
    }

    /** Pairs the wings' ends into a mouth (two wings), else a lone wing's end by the snare. Headings run far end to mouth. */
    static Layout withMouth(List<Wing> raw, List<BlockPos> snares) {
        Vec3 mouth = null;
        List<Wing> out = new ArrayList<>();
        if (raw.size() >= 2) {
            double bd = Double.MAX_VALUE;
            Wing wa = null, wb = null;
            boolean swapA = false, swapB = false;
            for (int i = 0; i < raw.size(); i++) for (int j = i + 1; j < raw.size(); j++) {
                Wing a = raw.get(i), b = raw.get(j);
                for (int sa = 0; sa < 2; sa++) for (int sb = 0; sb < 2; sb++) {
                    Vec3 ea = sa == 0 ? a.farEnd() : a.mouthEnd(), eb = sb == 0 ? b.farEnd() : b.mouthEnd();
                    double d = ea.distanceTo(eb);
                    if (d < bd) {
                        bd = d;
                        wa = a;
                        wb = b;
                        swapA = sa == 0;
                        swapB = sb == 0;
                    }
                }
            }
            if (wa != null && bd <= MOUTH_MAX_M) {
                Vec3 ea = swapA ? wa.farEnd() : wa.mouthEnd(), eb = swapB ? wb.farEnd() : wb.mouthEnd();
                mouth = ea.add(eb).scale(0.5);
                for (Wing w : raw) {
                    if (w == wa) out.add(orient(w, swapA ? w.mouthEnd() : w.farEnd(), ea));
                    else if (w == wb) out.add(orient(w, swapB ? w.mouthEnd() : w.farEnd(), eb));
                    else out.add(w);
                }
                return new Layout(out, mouth, snares);
            }
        }
        if (!raw.isEmpty() && !snares.isEmpty()) {
            Vec3 s = Vec3.atCenterOf(snares.get(0));
            for (Wing w : raw) {
                boolean farNear = w.farEnd().distanceTo(s) < w.mouthEnd().distanceTo(s);
                Vec3 mouthEnd = farNear ? w.farEnd() : w.mouthEnd(), far = farNear ? w.mouthEnd() : w.farEnd();
                out.add(orient(w, far, mouthEnd));
            }
            return new Layout(out, s, snares);
        }
        return new Layout(raw, null, snares);
    }

    private static Wing orient(Wing w, Vec3 far, Vec3 mouthEnd) {
        return new Wing(w.cells(), w.segments(), far, mouthEnd, Sensing.bearing(far, mouthEnd));
    }

    // ---- bands ---------------------------------------------------------------------------------------------------------

    private static final class Lead {
        final Vec3 mouth;
        final long until;
        long lingerUntil;
        Lead(Vec3 mouth, long until) {
            this.mouth = mouth;
            this.until = until;
        }
    }

    private static final Map<UUID, Lead> LEADS = new HashMap<>();
    private static final Map<UUID, Long> DECIDED = new HashMap<>();
    /** Bands decided as followers (by herd leader id) and bands that did not; GameTests and /yoi herd fence read them. */
    private static int followed, ignored;

    public static void clear() {
        LEADS.clear();
        DECIDED.clear();
        followed = ignored = 0;
    }

    public static int followed() { return followed; }

    public static int ignored() { return ignored; }

    public static boolean leading(BigGameEntity a) { return LEADS.containsKey(a.getUUID()); }

    /** One band at {@code pos} heading {@code headingDeg} meets the nearest wing: true when it follows it to the mouth. */
    public static boolean decide(Layout layout, Vec3 pos, double headingDeg, boolean scented, RandomGenerator rng) {
        Wing wing = layout.mouth() == null ? null : layout.nearest(pos);
        return wing != null && LeadFenceMath.bandFollows(CompanionConfig.tuning(), wing.segments(), headingDeg, wing.headingDeg(), scented, rng);
    }

    /** A caribou meets a fence: decides for its whole herd (same test group). Returns true if the band follows the wing. */
    public static boolean consider(ServerLevel level, BigGameEntity a, RandomGenerator rng) {
        long t = level.getGameTime();
        Long last = DECIDED.get(a.getUUID());
        if (last != null && t - last < 6000) return LEADS.containsKey(a.getUUID());
        Layout layout = scan(level, a.blockPosition(), SCAN_RADIUS);
        if (layout.mouth() == null || layout.nearest(a.position()) == null) return false;
        // wind blowing from the mouth (the blind) toward the band carries the scent to it
        double bearingToBand = Sensing.bearing(layout.mouth(), a.position());
        boolean scented = Math.abs(Mth.wrapDegrees(bearingToBand - Sensing.windToward(level, a))) <= 45;
        boolean follows = decide(layout, a.position(), Mth.wrapDegrees(a.getYRot()), scented, rng);
        List<BigGameEntity> band = new ArrayList<>(a.herd());
        band.add(a);
        for (BigGameEntity b : band) {
            DECIDED.put(b.getUUID(), t);
            if (follows) LEADS.put(b.getUUID(), new Lead(layout.mouth(), t + 4000));
        }
        if (follows) followed++;
        else ignored++;
        return follows;
    }

    /** Called from {@code BigGameEntity.tickRoutine}: walks a following caribou to the mouth, stands it there, may snare it. */
    public static boolean walk(BigGameEntity a, ServerLevel level) {
        Lead l = LEADS.get(a.getUUID());
        if (l == null) return false;
        long t = level.getGameTime();
        if (t > l.until && l.lingerUntil == 0 || l.lingerUntil != 0 && t > l.lingerUntil || a.state() != com.yearofice.sim.ecosystem.AnimalState.ROUTINE) {
            LEADS.remove(a.getUUID());
            return false;
        }
        double d = Math.hypot(l.mouth.x - a.getX(), l.mouth.z - a.getZ());
        if (d <= 4) {
            if (l.lingerUntil == 0) {
                l.lingerUntil = t + (30 + a.getRandom().nextInt(61)) * 20L;
                maybeSnare(level, a);
            }
            a.leadWalk(null);
            return true;
        }
        Vec3 target = l.mouth;
        if (d > 30) target = new Vec3(a.getX() + (l.mouth.x - a.getX()) / d * 30, l.mouth.y, a.getZ() + (l.mouth.z - a.getZ()) / d * 30);
        a.leadWalk(target);
        return true;
    }

    /** One chance a pass: the snare in the mouth holds the animal with probability {@code caribou_snare_hold}. */
    private static void maybeSnare(ServerLevel level, BigGameEntity a) {
        for (BlockPos p : BlockPos.betweenClosed(a.blockPosition().offset(-6, -2, -6), a.blockPosition().offset(6, 2, 6))) {
            if (!level.getBlockState(p).is(CompanionRegistry.CARIBOU_SNARE.get())) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof CaribouSnareBlockEntity sb)) continue;
            sb.notePass();
            if (!sb.holding() && a.getRandom().nextDouble() < CompanionConfig.tuning().caribouSnareHold()) {
                sb.hold(a.getUUID());
                a.setNoAi(true);
                a.getNavigation().stop();
            }
            return;
        }
    }

    // ---- events --------------------------------------------------------------------------------------------------------

    static boolean migrating(ServerLevel level) {
        int month = Calendar.month(Calendar.dayOfYearOfMinute(SliceClock.minute(level)));
        return month == 4 || month == 5 || month == 9 || month == 10;
    }

    public static void onTick(ServerTickEvent.Post e) {
        var server = e.getServer();
        if (server.getTickCount() % 40 != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            if (!migrating(level) || level.players().isEmpty()) continue;
            Set<UUID> seenBand = new HashSet<>();
            for (ServerPlayer p : level.players()) {
                for (BigGameEntity c : level.getEntitiesOfClass(BigGameEntity.class, new AABB(p.position(), p.position()).inflate(96),
                        c -> c.isAlive() && c.speciesId().endsWith("caribou") && c.testGroup() == null)) {
                    if (seenBand.add(c.getUUID())) consider(level, c, new java.util.SplittableRandom(level.random.nextLong()));
                }
            }
        }
    }

    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof BigGameEntity c) || !c.speciesId().endsWith("caribou") || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (c.level() instanceof ServerLevel level && !scan(level, c.blockPosition(), 25).wings().isEmpty()) AmbitionTracker.complete(p, AmbitionTracker.AT_THE_FENCE);
    }
}
