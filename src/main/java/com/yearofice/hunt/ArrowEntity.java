package com.yearofice.hunt;

import com.yearofice.api.env.EnvService;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.hunt.wound.WoundTracker;
import com.yearofice.sim.hunt.ArrowHead;
import com.yearofice.sim.hunt.Ballistics;
import com.yearofice.sim.hunt.HitZones;
import com.yearofice.sim.hunt.HuntData;
import com.yearofice.sim.hunt.Shot;
import com.yearofice.sim.hunt.Weapon;
import com.yearofice.sim.hunt.WoundModel;
import com.yearofice.api.story.AmbitionTracker;
import com.yearofice.story.ambition.Ambitions;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The arrow in flight (Blueprint › Weapons and arrowheads; The hit-zone model). Flight is {@link Ballistics}: gravity and
 * drag toward the moving air, so a crosswind drifts it. On an animal the shaft is traced through the species'
 * {@code hitzone/} ellipsoids in the animal's body frame, {@link Shot} judges it and the wound (if any) goes to
 * {@link WoundTracker}. Into turf or wood the arrow is found again 95% of the time (50% in snow).
 */
public class ArrowEntity extends Projectile implements ItemSupplier {
    static final double DT = 0.05;
    private static final int MAX_AGE = 400;

    private Vec3 vel = Vec3.ZERO; // m/s
    private Vec3 origin = Vec3.ZERO;
    private double energyJ, effectiveRangeM = 25;
    private String head = HuntItems.DEFAULT_HEAD;
    private double[] windOverride; // {x, z} the air's velocity; tests set it
    private final Set<UUID> skip = new HashSet<>();
    private int age;
    private double lastDropM, lastDriftM;
    private boolean touchedAnimal;

    public ArrowEntity(EntityType<? extends ArrowEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void launch(LivingEntity shooter, Vec3 from, Vec3 dir, double speedMps, double energyJ, Weapon w, String head) {
        setOwner(shooter);
        setPos(from);
        this.origin = from;
        this.vel = dir.normalize().scale(speedMps);
        this.energyJ = energyJ;
        this.effectiveRangeM = w.effectiveRangeM();
        this.head = head;
        face();
    }

    /** Test hook: the air's velocity (m/s) instead of the environment's wind. */
    public ArrowEntity withWind(double x, double z) { this.windOverride = new double[] {x, z}; return this; }

    public Vec3 velocity() { return vel; }

    public Vec3 origin() { return origin; }

    public boolean flying() { return !isRemoved() && vel.lengthSqr() > 0; }

    private void face() {
        setYRot((float) (Math.toDegrees(Math.atan2(-vel.x, vel.z))));
        setXRot((float) (Math.toDegrees(Math.atan2(-vel.y, Math.sqrt(vel.x * vel.x + vel.z * vel.z)))));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {}

    @Override
    public ItemStack getItem() { return HuntItems.arrow(head, 1); }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {}

    double[] wind() {
        if (windOverride != null) return windOverride;
        try {
            var w = EnvService.get().wind((ServerLevel) level(), blockPosition());
            double r = Math.toRadians(w.fromDeg());
            return new double[] {-Math.sin(r) * w.ms(), Math.cos(r) * w.ms()};
        } catch (IllegalStateException e) {
            return new double[] {0, 0};
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            setPos(getX() + vel.x * DT, getY() + vel.y * DT, getZ() + vel.z * DT);
            return;
        }
        if (vel.lengthSqr() == 0) return;
        if (++age > MAX_AGE) { HuntDebug.note(getOwner() instanceof ServerPlayer sp ? sp : null, String.format(java.util.Locale.ROOT, "shaft flew out at %s, %.1f m out", blockPosition().toShortString(), origin.distanceTo(position()))); discard(); return; }
        double[] w = wind();
        var s = Ballistics.step(new Ballistics.State(getX(), getY(), getZ(), vel.x, vel.y, vel.z), w[0], w[1], DT, HuntConfig.tuning());
        Vec3 from = position(), to = new Vec3(s.x(), s.y(), s.z());
        vel = new Vec3(s.vx(), s.vy(), s.vz());
        Vec3 stop = null;
        BlockHitResult bh = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 limit = bh.getType() == HitResult.Type.MISS ? to : bh.getLocation();
        LivingEntity hit = null;
        double best = from.distanceToSqr(limit);
        AABB box = new AABB(from, limit).inflate(1.0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator() && !skip.contains(e.getUUID())
                && !(e == getOwner() && age < 6))) {
            Optional<Vec3> c = e.getBoundingBox().inflate(0.15).clip(from, limit);
            if (c.isPresent() && from.distanceToSqr(c.get()) <= best) { best = from.distanceToSqr(c.get()); hit = e; stop = c.get(); }
        }
        lastDropM = Math.max(lastDropM, origin.y - getY());
        if (hit != null) {
            if (onAnimal(sl, hit, from, stop)) return;
            skip.add(hit.getUUID()); // clean miss: the shaft flies on
        } else if (bh.getType() != HitResult.Type.MISS) {
            setPos(bh.getLocation());
            stuck(sl, bh.getBlockPos());
            return;
        }
        setPos(to);
        setDeltaMovement(vel.scale(DT));
        face();
    }

    private void stuck(ServerLevel sl, BlockPos hitBlock) {
        boolean snow = sl.getBlockState(hitBlock).is(net.minecraft.tags.BlockTags.SNOW);
        if (!touchedAnimal) {
            HuntDebug.note(getOwner() instanceof ServerPlayer sp ? sp : null, String.format(java.util.Locale.ROOT, "shaft stuck in %s at %s, %.1f m out, %.2f m below the loose, after %d ticks",
                    BuiltInRegistries.BLOCK.getKey(sl.getBlockState(hitBlock).getBlock()).getPath(), hitBlock.toShortString(), origin.distanceTo(position()), origin.y - getY(), age));
        }
        double keep = snow ? 0.5 : HuntData.head(head).reuse() > 0 ? 0.95 : 0.95;
        if (sl.random.nextDouble() < keep && !sl.getFluidState(blockPosition()).is(net.minecraft.tags.FluidTags.WATER)) {
            sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.1, getZ(), getItem()));
        }
        vel = Vec3.ZERO;
        discard();
    }

    /** Resolves a hit on {@code e}. False when the shaft passes (a clean miss). */
    boolean onAnimal(ServerLevel sl, LivingEntity e, Vec3 from, Vec3 at) {
        touchedAnimal = true;
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
        ServerPlayer shooter = getOwner() instanceof ServerPlayer sp ? sp : null;
        HitZones zones;
        try { zones = HuntData.zones(species); } catch (IllegalArgumentException | IllegalStateException ex) { zones = null; }
        if (zones == null) { // not a huntable species: an ordinary hit
            e.hurt(sl.damageSources().arrow(null, getOwner()), (float) Math.max(1, energyJ / 6));
            discard();
            return true;
        }
        double yaw = Math.toRadians(e.yBodyRot), fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = fz, rz = -fx;
        Vec3 d = vel.normalize();
        double[] lo = local(from, e, fx, fz, rx, rz), ld = {d.x * rx + d.z * rz, d.y, d.x * fx + d.z * fz};
        String pose = e.isSleeping() || e.getPose() == net.minecraft.world.entity.Pose.SLEEPING ? "bedded" : "standing";
        double maxT = from.distanceTo(at) + 2.5;
        HitZones.Trace tr = zones.trace(pose, lo, ld, maxT);
        String zoneName = tr == null ? null : tr.zone().name();
        if (zoneName == null && e.getBoundingBox().clip(from, from.add(d.scale(maxT))).isPresent()) {
            double hitY = at.y - e.getY();
            double legY = zones.zones(pose).stream().filter(HitZones.Zone::halfSpace).mapToDouble(HitZones.Zone::belowY).findFirst().orElse(0.6);
            zoneName = hitY < legY ? "legs" : "gut";
        }
        ArrowHead h = HuntData.head(head);
        Shot.Angle angle = Shot.Angle.of(ld[2]);
        double dist = origin.distanceTo(at);
        Shot.Outcome o = Shot.resolve(zoneName, energyJ, h, dist, effectiveRangeM, angle, zones.killThresholdJ(),
                tr != null && HitZones.shoulder(tr), sl.random.nextDouble(), HuntConfig.tuning());
        HuntDebug.remember(shooter, species, head, dist, angle, o, zoneName);
        double minute = SliceClock.minuteD(sl);
        if (shooter != null && zones.killThresholdJ() >= 20) {
            if (o.result() == Shot.Result.KILL && o.channel() == Shot.Channel.VITALS) {
                if (AmbitionTracker.tally(shooter, "hunt.clean_run", 1) >= 10) {
                    Ambitions.fire(shooter, "hunt.clean_10", (long) minute);
                }
            } else if (o.result() != Shot.Result.MISS) {
                AmbitionTracker.tally(shooter, "hunt.clean_run", -AmbitionTracker.tally(shooter, "hunt.clean_run"));
            }
        }
        if (shooter != null && o.result() == Shot.Result.KILL && o.channel() == Shot.Channel.NONE && species.equals("grizzly")) {
            Ambitions.fire(shooter, "hunt.grizzly_bow", (long) minute);
        }
        switch (o.result()) {
            case MISS -> { return false; }
            case STUN -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5));
            case ANGER -> { if (shooter != null) e.setLastHurtByMob(shooter); }
            case KILL, WOUND -> {
                if (o.channel() == Shot.Channel.NONE) {
                    kill(sl, e);
                } else {
                    var wound = WoundModel.open(o.channel(), minute, h.bleedFactor(), sl.random.nextDouble());
                    WoundTracker.wound(sl, e, wound, shooter, head, sl.random.nextDouble(), true);
                    if (shooter != null) e.setLastHurtByMob(shooter);
                    return finish();
                }
            }
        }
        return finish();
    }

    private boolean finish() {
        vel = Vec3.ZERO;
        discard();
        return true;
    }

    private void kill(ServerLevel sl, LivingEntity e) {
        if (sl.random.nextDouble() < HuntData.head(head).reuse()) sl.addFreshEntity(new ItemEntity(sl, e.getX(), e.getY() + 0.3, e.getZ(), getItem()));
        if (e instanceof YoiAnimal a) DeathPipeline.kill(a, sl.damageSources().generic(), "arrow");
        else e.kill();
    }

    /** World point to the animal's body frame: x across (right), y up, z forward. */
    static double[] local(Vec3 p, LivingEntity e, double fx, double fz, double rx, double rz) {
        double dx = p.x - e.getX(), dz = p.z - e.getZ();
        return new double[] {dx * rx + dz * rz, p.y - e.getY(), dx * fx + dz * fz};
    }
}
