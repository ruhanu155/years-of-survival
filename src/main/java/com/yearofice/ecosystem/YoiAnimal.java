package com.yearofice.ecosystem;

import com.mojang.serialization.Dynamic;
import com.yearofice.ecosystem.ai.AwarenessBook;
import com.yearofice.ecosystem.ai.ThreatSources;
import com.yearofice.ecosystem.ai.YoiBrain;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.ecosystem.Awareness;
import com.yearofice.sim.ecosystem.Gaits;
import com.yearofice.sim.ecosystem.Posture;
import com.yearofice.sim.ecosystem.PreyRules;
import com.yearofice.sim.time.Calendar;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The base of every living animal (Minecraft development › The entity class tree): a {@link PathfinderMob} and a
 * GeckoLib {@link GeoEntity}, never {@code Animal}, so breeding, taming, anger and growth are absent by
 * construction. It carries the Brain ({@link YoiBrain}), awareness, the slot it was placed in, and every
 * no-vanilla override of the slice:
 *
 * <ul>
 *   <li>Damage: a player's hit kills huntable small game outright (the slice's stand-in for the hit-zone
 *       resolver: small game dies to 2 J, "a blunt kills"); a non-huntable animal ignores player damage and runs.
 *       {@code /kill} and the void run the death pipeline; all other vanilla damage is ignored.</li>
 *   <li>Death: {@link DeathPipeline} replaces {@code die}: a carcass, no drops, no experience, no tip-over.</li>
 *   <li>Saving: wild bodies are never saved into chunks; the slot spawner places them again
 *       ({@code shouldBeSaved} false). Nothing despawns by distance.</li>
 *   <li>No vanilla sounds or goals.</li>
 * </ul>
 * Synced to the client: the state, a movement hint for the gait animation, the coat, sex and antler scale.
 */
public abstract class YoiAnimal extends PathfinderMob implements GeoEntity {
    static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(YoiAnimal.class, EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Byte> MOVE = SynchedEntityData.defineId(YoiAnimal.class, EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Byte> COAT = SynchedEntityData.defineId(YoiAnimal.class, EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Byte> SEX = SynchedEntityData.defineId(YoiAnimal.class, EntityDataSerializers.BYTE);
    static final EntityDataAccessor<Byte> ANTLER = SynchedEntityData.defineId(YoiAnimal.class, EntityDataSerializers.BYTE);

    public static final int MOVE_IDLE = 0, MOVE_WALK = 1, MOVE_RUN = 2, MOVE_FLY = 3, MOVE_FEED = 4;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private AwarenessBook awareness;
    protected AnimalState state = AnimalState.ROUTINE;
    protected BlockPos home;
    private String slotKey = "";
    private boolean scentEnabled = true;
    private boolean placed;

    // flee plan
    protected Vec3 fleeFrom = Vec3.ZERO;
    protected double fleeTargetM, fleeCoveredM;
    protected Vec3 waypoint;
    protected Vec3 lastPos, checkpoint;
    protected boolean zigLeft;
    protected int fleeTicks, stuckLegs, legTicks, legLimit;
    protected boolean direct;
    protected long hideUntilTick;
    // routine
    protected int routineTimer;

    protected YoiAnimal(EntityType<? extends YoiAnimal> type, Level level) {
        super(type, level);
    }

    /** The species id, {@code yearofice:<id>}. */
    public abstract String speciesId();

    /** The species' path, the name of its animation file. */
    public String speciesPath() { return speciesId().substring(speciesId().indexOf(':') + 1); }

    public SpeciesDef species() { return SpeciesBook.get(speciesId()); }

    public AwarenessBook awareness() {
        if (awareness == null) awareness = new AwarenessBook();
        return awareness;
    }

    public AnimalState state() { return state; }

    public AnimalState syncedState() { return AnimalState.byId(entityData.get(STATE)); }

    public int moveHint() { return entityData.get(MOVE); }

    public int coat() { return entityData.get(COAT); }

    public boolean male() { return entityData.get(SEX) == 1; }

    public float antlerScale() { return (entityData.get(ANTLER) & 0xFF) / 255f; }

    public void setMale(boolean m) { entityData.set(SEX, (byte) (m ? 1 : 0)); }

    public String slotKey() { return slotKey; }

    public void setSlot(String key, BlockPos home) {
        this.slotKey = key;
        this.home = home;
    }

    public BlockPos home() { return home == null ? blockPosition() : home; }

    public boolean scentEnabled() { return scentEnabled; }

    /** GameTests pin the wind off so scent never decides a result. */
    public void setScentEnabled(boolean on) { scentEnabled = on; }

    /** True when a player's hit can take this animal now. */
    public boolean huntableNow() { return species().huntable(); }

    public static AttributeSupplier.Builder attributes(double health) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(STATE, (byte) 0).define(MOVE, (byte) 0).define(COAT, (byte) 0).define(SEX, (byte) 0).define(ANTLER, (byte) 0);
    }

    // ---- Brain ----------------------------------------------------------------------------------------------

    @Override
    protected Brain.Provider<YoiAnimal> brainProvider() { return Brain.provider(YoiBrain.memories(), YoiBrain.sensors()); }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) { return YoiBrain.makeBrain(brainProvider().makeBrain(dynamic)); }

    @SuppressWarnings("unchecked")
    @Override
    public Brain<YoiAnimal> getBrain() { return (Brain<YoiAnimal>) super.getBrain(); }

    @Override
    protected void customServerAiStep() {
        getBrain().tick((ServerLevel) level(), this);
        super.customServerAiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (tickCount % 100 == 1 || !placed)) {
            placed = true;
            if (home == null) home = blockPosition();
            updateLooks(Calendar.dayOfYearOfMinute(SliceClock.minute(level())));
        }
    }

    /** Sets the looks for a day of the year now (the debug command and GameTests use it; the tick uses the clock). */
    public final void refreshLooks(int doy) { updateLooks(doy); }

    /** Coat, plumage and antlers from the date (Land animals in depth › Seasonal looks). */
    protected void updateLooks(int doy) {}

    protected void setCoat(int coat) { entityData.set(COAT, (byte) coat); }

    protected void setAntler(double scale) { entityData.set(ANTLER, (byte) Math.round(Math.max(0, Math.min(1, scale)) * 255)); }

    protected void setMove(int m) { if (entityData.get(MOVE) != m) entityData.set(MOVE, (byte) m); }

    /** Core behaviour, every tick: decay, the threat memory, and the state transition. */
    public void think(ServerLevel level) {
        long t = level.getGameTime();
        AwarenessBook ab = awareness();
        ab.decay(t);
        LivingEntity threat = ab.top();
        Brain<YoiAnimal> brain = getBrain();
        if (threat != null) {
            brain.setMemory(EcosystemRegistry.THREAT.get(), threat);
            brain.setMemory(EcosystemRegistry.LOOK_TARGET.get(), threat);
        } else {
            brain.eraseMemory(EcosystemRegistry.THREAT.get());
            brain.eraseMemory(EcosystemRegistry.LOOK_TARGET.get());
        }
        PreyRules.Behavior b = species().behavior();
        double a = ab.of(threat);
        double dist = threat == null ? Double.POSITIVE_INFINITY : distanceTo(threat);
        double flush = 0;
        if (threat != null && b.freezes()) {
            Posture p = ThreatSources.posture(threat, t);
            flush = Awareness.flushDistance(b.flushM(), p, ThreatSources.approachCos(threat, position(), t), false);
        }
        AnimalState next = PreyRules.next(state, b, a, dist, flush, state == AnimalState.FLEE && fleeFinished(), t < hideUntilTick);
        if (next == state) return;
        if (state == AnimalState.FLEE) {
            int minutes = b.hideMinMin() + random.nextInt(b.hideMaxMin() - b.hideMinMin() + 1);
            hideUntilTick = t + Math.round(minutes * SliceClock.TICKS_PER_MINUTE);
        }
        state = next;
        entityData.set(STATE, (byte) next.ordinal());
        brain.setActiveActivityIfPossible(YoiBrain.activity(next));
    }

    /** The state's activity just started. */
    public void enterState(AnimalState s) {
        switch (s) {
            case FLEE -> startFlee();
            case ROUTINE -> routineTimer = 20 + random.nextInt(60);
            default -> getNavigation().stop();
        }
    }

    public void tickState(ServerLevel level, AnimalState s) {
        switch (s) {
            case ROUTINE -> tickRoutine(level);
            case ALERT, FREEZE -> {
                getNavigation().stop();
                setMove(MOVE_IDLE);
                LivingEntity th = getBrain().getMemory(EcosystemRegistry.LOOK_TARGET.get()).orElse(null);
                if (th != null && s == AnimalState.ALERT) getLookControl().setLookAt(th, 30, 30);
            }
            case FLEE -> tickFlee(level);
        }
    }

    // ---- Routine ----------------------------------------------------------------------------------------------

    /** Feed and wander inside the home range at walking pace. */
    protected void tickRoutine(ServerLevel level) {
        if (--routineTimer > 0) {
            if (getNavigation().isDone() && moveHint() == MOVE_WALK) setMove(MOVE_IDLE);
            return;
        }
        double r = wanderRadius();
        if (random.nextFloat() < 0.55f) {
            getNavigation().stop();
            setMove(MOVE_FEED);
            routineTimer = 100 + random.nextInt(200);
            return;
        }
        BlockPos h = home();
        double x = h.getX() + 0.5 + (random.nextDouble() * 2 - 1) * r;
        double z = h.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * r;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        if (getNavigation().moveTo(x, y, z, Gaits.input(species().walkMps()))) setMove(MOVE_WALK);
        routineTimer = 60 + random.nextInt(140);
    }

    protected double wanderRadius() { return 8; }

    // ---- Flee -------------------------------------------------------------------------------------------------

    protected void startFlee() {
        PreyRules.Behavior b = species().behavior();
        LivingEntity th = getBrain().getMemory(EcosystemRegistry.THREAT.get()).orElse(null);
        fleeFrom = th == null ? position().add(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5) : th.position();
        fleeTargetM = b.fleeMinM() + random.nextDouble() * (b.fleeMaxM() - b.fleeMinM());
        fleeCoveredM = 0;
        fleeTicks = 0;
        stuckLegs = 0;
        waypoint = null;
        lastPos = position();
        zigLeft = random.nextBoolean();
    }

    public boolean fleeFinished() { return fleeCoveredM >= fleeTargetM || stuckLegs > 8; }

    public double fleeCoveredM() { return fleeCoveredM; }

    public String fleeDebug() { return "target " + fleeTargetM + " stuck " + stuckLegs + " legLimit " + legLimit + " direct " + direct; }

    /** Speed now, in m/s: the species' flee gait (the moose walks off for 2 s, then trots). */
    protected double fleeMps() {
        SpeciesDef d = species();
        return switch (d.fleeGait()) {
            case "trot" -> fleeTicks < 40 ? d.walkMps() : d.trotMps();
            default -> d.runMps();
        };
    }

    protected void tickFlee(ServerLevel level) {
        fleeTicks++;
        legTicks++;
        Vec3 p = position();
        if (lastPos != null) fleeCoveredM += Math.hypot(p.x - lastPos.x, p.z - lastPos.z);
        lastPos = p;
        LivingEntity th = getBrain().getMemory(EcosystemRegistry.THREAT.get()).orElse(null);
        if (th != null) fleeFrom = th.position();
        boolean reached = waypoint != null && Math.hypot(waypoint.x - p.x, waypoint.z - p.z) < 1.2;
        boolean stalled = false;
        if (legTicks % 20 == 0) {
            stalled = checkpoint != null && Math.hypot(p.x - checkpoint.x, p.z - checkpoint.z) < 0.4 && legTicks > 0;
            checkpoint = p;
        }
        if (stalled && !direct && waypoint != null) {
            // The path stalls (an edge it will not drop from, a crowd): run the line straight.
            direct = true;
            getNavigation().stop();
            getMoveControl().setWantedPosition(waypoint.x, waypoint.y, waypoint.z, Gaits.input(fleeMps()));
            setMove(fleeMps() > 2.5 ? MOVE_RUN : MOVE_WALK);
            return;
        }
        if (stalled) {
            stuckLegs++;
            nextLeg(level);
        } else if (waypoint == null || reached) {
            nextLeg(level);
        } else if (legTicks > legLimit) {
            // The leg took half as long again as it should: a wall, a bank, deep water. Try another line.
            stuckLegs++;
            nextLeg(level);
        } else if (direct && onGround() && legTicks % 40 == 0 && legTicks >= 80
                && getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, Gaits.input(fleeMps()))) {
            direct = false;
        } else if (direct || getNavigation().isDone()) {
            direct = true;
            getMoveControl().setWantedPosition(waypoint.x, waypoint.y, waypoint.z, Gaits.input(fleeMps()));
        } else if (fleeTicks == 40 && species().fleeGait().equals("trot")) {
            getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, Gaits.input(fleeMps()));
        }
        setMove(fleeMps() > 2.5 ? MOVE_RUN : MOVE_WALK);
    }

    /** The next leg away from the threat; zigzaggers bend 35° left and right on short bursts. */
    protected void nextLeg(ServerLevel level) {
        Vec3 p = position();
        Vec3 away = new Vec3(p.x - fleeFrom.x, 0, p.z - fleeFrom.z);
        if (away.lengthSqr() < 1e-4) away = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
        away = away.normalize();
        double left = fleeTargetM - fleeCoveredM;
        double leg;
        double turn = (random.nextDouble() - 0.5) * 20;
        // After a stall, swing the line further aside each time, left and right in turn, to get round the block.
        if (stuckLegs > 0) turn += (stuckLegs % 2 == 0 ? 1 : -1) * Math.min(150, 45 * ((stuckLegs + 1) / 2));
        if (species().zigzag()) {
            zigLeft = !zigLeft;
            turn += zigLeft ? 35 : -35;
            leg = 4 + random.nextDouble() * 2;
        } else {
            leg = Math.min(24, Math.max(6, left));
        }
        double rad = Math.toRadians(turn);
        Vec3 dir = new Vec3(away.x * Math.cos(rad) - away.z * Math.sin(rad), 0, away.x * Math.sin(rad) + away.z * Math.cos(rad));
        Vec3 w = p.add(dir.scale(leg));
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(w.x), (int) Math.floor(w.z));
        waypoint = new Vec3(w.x, y, w.z);
        legTicks = 0;
        legLimit = 40 + (int) Math.ceil(leg / Math.max(0.5, Math.min(fleeMps(), species().trotMps())) * 20 * 1.5);
        direct = !getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, Gaits.input(fleeMps()));
        // No path (a drop, water): run straight on with the move control.
        if (direct) getMoveControl().setWantedPosition(waypoint.x, waypoint.y, waypoint.z, Gaits.input(fleeMps()));
    }

    // ---- damage and death -------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) return false;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            DeathPipeline.kill(this, source, "command");
            return true;
        }
        Entity by = source.getEntity();
        if (by instanceof Player p) {
            if (!huntableNow()) {
                awareness().spike(p, Awareness.MAX, level().getGameTime());
                return false;
            }
            DeathPipeline.kill(this, source, "player");
            return true;
        }
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource src) { return false; }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void checkDespawn() {}

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource src) { return null; }

    @Override
    protected @Nullable SoundEvent getDeathSound() { return null; }

    @Override
    public boolean canBeLeashed() { return false; }

    // ---- GeckoLib ------------------------------------------------------------------------------------------------

    private static final Map<String, RawAnimation> LOOPS = new HashMap<>();

    protected static RawAnimation loop(String name) {
        synchronized (LOOPS) {
            return LOOPS.computeIfAbsent(name, n -> RawAnimation.begin().thenLoop(n));
        }
    }

    /** The base controller's animation for the synced state and movement hint. */
    protected String animKey(AnimalState s, int move) {
        return switch (s) {
            case FLEE -> "flee";
            case ALERT, FREEZE -> "alert";
            case ROUTINE -> switch (move) {
                case MOVE_WALK -> "walk";
                case MOVE_RUN -> "run";
                case MOVE_FEED -> "forage";
                default -> "idle";
            };
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 4,
                st -> st.setAndContinue(loop("animation." + com.yearofice.ecosystem.species.SpeciesArt.animSpecies(speciesPath()) + "." + animKey(syncedState(), moveHint())))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
}
