package com.yearofice.ecosystem.bird;

import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.spawn.Habitats;
import com.yearofice.sim.bird.BirdRules;
import com.yearofice.sim.bird.BirdTuning;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Flocks and the bird spawner. A flock here is a shared id on its members, each a real {@link BirdEntity}: the
 * blueprint's one-entity {@code BirdFlockEntity} with boids drawn on the client is the later round, and this holds
 * the same rules (members keep 1 to 3 body lengths apart and steer back to the flock). Birds are placed round the
 * players by habitat and month, within the Performance caps; they are not saved.
 */
public final class BirdService {
    private BirdService() {}

    private static final AtomicInteger NEXT_FLOCK = new AtomicInteger(1);
    /** Totals since the server started, for {@code /yoi flock status} and the GameTests. */
    public static volatile long scavengedG, fishTaken, thefts;

    public static void clear() {
        scavengedG = 0;
        fishTaken = 0;
        thefts = 0;
    }

    /** The other members of a bird's flock within 96 m. */
    public static List<BirdEntity> mates(ServerLevel level, BirdEntity self) {
        List<BirdEntity> out = new ArrayList<>();
        if (self.flockId() == 0) return out;
        for (BirdEntity b : level.getEntitiesOfClass(BirdEntity.class, self.getBoundingBox().inflate(96, 48, 96))) {
            if (b != self && b.flockId() == self.flockId() && b.isAlive()) out.add(b);
        }
        return out;
    }

    /** Places a flock of {@code n} (0 draws the size from the species' range), scattered 6 m round {@code at}. */
    public static List<BirdEntity> form(ServerLevel level, String path, BlockPos at, int n) {
        BirdRules.Role role = BirdRules.role(path);
        List<BirdEntity> out = new ArrayList<>();
        var type = BirdRegistry.type(path);
        if (role == null || type == null) return out;
        BirdTuning t = BirdConfig.tuning();
        int id = BirdRules.flocks(role) ? NEXT_FLOCK.getAndIncrement() : 0;
        int count = n > 0 ? n : BirdRules.flockSize(role, t, level.random.nextDouble());
        for (int i = 0; i < count; i++) {
            BirdEntity b = type.create(level);
            if (b == null) continue;
            double ang = level.random.nextDouble() * Math.PI * 2, r = count == 1 ? 0 : 2 + level.random.nextDouble() * 4;
            int x = at.getX() + (int) Math.round(Math.cos(ang) * r), z = at.getZ() + (int) Math.round(Math.sin(ang) * r);
            BlockPos ground = Habitats.surface(level, x, z);
            b.moveTo(x + 0.5, ground.getY(), z + 0.5, level.random.nextFloat() * 360f, 0);
            b.setSlot("", ground);
            b.setMale(level.random.nextBoolean());
            b.setFlock(id);
            if (level.addFreshEntity(b)) out.add(b);
        }
        return out;
    }

    /** Flock ids alive near a point, and the singles, for the caps. */
    public static int[] census(ServerLevel level, BlockPos at, double r) {
        java.util.Set<Integer> flocks = new java.util.HashSet<>();
        int singles = 0;
        for (BirdEntity b : level.getEntitiesOfClass(BirdEntity.class, new AABB(at).inflate(r, 128, r))) {
            if (b.flockId() != 0) flocks.add(b.flockId());
            else singles++;
        }
        return new int[] {singles, flocks.size()};
    }

    // ---- the spawner --------------------------------------------------------------------------------------------

    private static boolean habitatOk(ServerLevel level, BlockPos ground, BirdRules.Role role) {
        return switch (role) {
            case DABBLER, GULL, DIVER, RAPTOR -> Habitats.is(level, ground, "pond_edge") || Habitats.is(level, ground, "bank");
            case JAY, OWL -> Habitats.is(level, ground, "forest");
            case CORVID -> true;
        };
    }

    /** Every 5 s, tops up the birds near each player to the caps, one placement a pass. */
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 100 != 37) return;
        ServerLevel level = e.getServer().overworld();
        BirdTuning t = BirdConfig.tuning();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            int[] c = census(level, p.blockPosition(), 128);
            int month = Calendar.month(Calendar.dayOfYearOfMinute(SliceClock.minute(level)));
            int hour = Calendar.minuteOfDayOfMinute(SliceClock.minute(level)) / 60;
            String path = BirdRegistry.IDS.get(level.random.nextInt(BirdRegistry.IDS.size()));
            BirdRules.Role role = BirdRules.role(path);
            if (!BirdRules.inSeason(path, month) || !BirdRules.active(role, hour)) continue;
            boolean flock = BirdRules.flocks(role);
            if (flock ? c[1] >= t.flockCap() : c[0] >= t.activeCap()) continue;
            double ang = level.random.nextDouble() * Math.PI * 2, r = 24 + level.random.nextDouble() * (t.spawnRadiusM() - 24);
            BlockPos ground = Habitats.surface(level, p.getBlockX() + (int) (Math.cos(ang) * r), p.getBlockZ() + (int) (Math.sin(ang) * r));
            if (!level.hasChunkAt(ground) || !habitatOk(level, ground, role)) continue;
            form(level, path, ground, 0);
        }
    }
}
