package com.yearofice.ecosystem.bird;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.species.bird.WillowPtarmiganEntity;
import com.yearofice.ecosystem.theft.TheftService;
import com.yearofice.hunt.fishing.WaterLife;
import com.yearofice.sim.bird.BirdRules;
import com.yearofice.sim.bird.BirdTuning;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.ecosystem.Theft;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;

/**
 * The M6 birds on the grouse rig's flush (Birds and the air › the M6 sheets): raven, gray jay, Steller's jay, bald
 * eagle, great horned owl, mallard, northern pintail, common loon and mew gull. The species file gives the numbers;
 * {@link BirdRules} gives the roles. In its routine a bird follows its flock, goes to a carcass (corvids, eagle,
 * gull, jays), to a camp store (jays and ravens, through {@link TheftService}) or to a fishing spot (the eagle,
 * through {@link WaterLife}); a threat makes it flush like the ptarmigan. It flies short hops with the move
 * control, not a flight controller: the 11-state controller of the full birds sheet is a later round.
 */
public class BirdEntity extends WillowPtarmiganEntity {
    public enum Goal { NONE, FLOCK, CARRION, STORE, FISH }

    private BirdRules.Role role;
    private int flockId;
    private Goal goalKind = Goal.NONE;
    private Vec3 goal;
    private UUID carcassId;
    private BlockPos storePos;
    private FishSpotService.Spot spot;
    private double pendingG, eatenThisVisit;
    private long fedUntilTick;
    private double delayMin = -1;
    private int eatTick;
    private final int salt = (int) (Math.abs(getUUID().getLeastSignificantBits()) % 10);
    private String lastFish = "";

    public BirdEntity(EntityType<? extends BirdEntity> type, Level level) { super(type, level); }

    @Override
    public String speciesId() { return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).toString(); }

    public BirdRules.Role role() {
        if (role == null) role = BirdRules.role(speciesPath());
        return role;
    }

    public int flockId() { return flockId; }

    public void setFlock(int id) { flockId = id; }

    public Goal goalKind() { return goalKind; }

    public String lastFish() { return lastFish; }

    @Override
    protected void updateLooks(int doy) { setCoat(0); }

    @Override
    protected double wanderRadius() { return BirdRules.flocks(role()) ? 10 : 6; }

    @Override
    protected void startFlee() {
        endGoal();
        super.startFlee();
    }

    @Override
    protected void tickRoutine(ServerLevel level) {
        if (role() == null) {
            super.tickRoutine(level);
            return;
        }
        if (goalKind == Goal.NONE || goalKind == Goal.FLOCK) {
            if (tickCount % 10 == salt) decide(level);
        }
        if (goalKind != Goal.NONE) {
            act(level);
            return;
        }
        if (floatsHere(level)) {
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
            setMove(MOVE_IDLE);
            return;
        }
        if (isNoGravity()) setNoGravity(false);
        super.tickRoutine(level);
    }

    boolean floatsHere(ServerLevel level) {
        return level.getFluidState(blockPosition()).is(FluidTags.WATER) || level.getFluidState(blockPosition().below()).is(FluidTags.WATER);
    }

    // ---- choosing a goal ----------------------------------------------------------------------------------------

    void decide(ServerLevel level) {
        BirdTuning t = BirdConfig.tuning();
        long now = level.getGameTime();
        BirdRules.Role r = role();
        if (BirdRules.scavenges(r) && now >= fedUntilTick) {
            CarcassEntity c = nearestCarcass(level, t.scavengeRangeM());
            if (c != null && SliceClock.minuteD(level) - c.record().deathMinute() >= arrivalDelay(t)) {
                goalKind = Goal.CARRION;
                carcassId = c.getUUID();
                goal = c.position();
                return;
            }
        }
        if ((r == BirdRules.Role.JAY || r == BirdRules.Role.CORVID) && now >= fedUntilTick && TheftService.enabled()) {
            Theft.Thief thief = r == BirdRules.Role.CORVID ? Theft.Thief.RAVEN : Theft.Thief.GRAY_JAY;
            for (BlockPos store : TheftService.storesNear(level, blockPosition(), t.thiefRangeM())) {
                double p = TheftService.chancePerHour(level, store, thief) * (10.0 / SliceClock.TICKS_PER_MINUTE) / 60.0;
                if (random.nextDouble() < p) {
                    goalKind = Goal.STORE;
                    storePos = store;
                    goal = Vec3.atCenterOf(store).add(0, 1, 0);
                    return;
                }
            }
        }
        if (r == BirdRules.Role.RAPTOR && now >= fedUntilTick) {
            for (FishSpotService.Spot s : WaterLife.spotsNear(level, blockPosition(), t.eagleSpotRangeM())) {
                WaterLife.Best b = WaterLife.fullest(level, s);
                if (b == null) continue;
                if (random.nextDouble() < BirdRules.eagleTakeChance(t, b.fraction(), 10.0 / SliceClock.TICKS_PER_MINUTE)) {
                    startFishing(s);
                    return;
                }
            }
        }
        if (BirdRules.flocks(r) && flockId != 0) {
            List<double[]> others = new ArrayList<>();
            for (BirdEntity b : BirdService.mates(level, this)) others.add(new double[] {b.getX(), b.getZ()});
            double[] d = BirdRules.steer(getX(), getZ(), others, t, 3.0);
            if (Math.hypot(d[0], d[1]) > 0.2) {
                double x = getX() + d[0], z = getZ() + d[1];
                goalKind = Goal.FLOCK;
                goal = new Vec3(x, surfaceY(level, x, z), z);
            } else if (goalKind == Goal.FLOCK) {
                endGoal();
            }
        }
    }

    /** Sends the bird to take a fish at the spot now (the natural trigger rolls {@link BirdRules#eagleTakeChance}). */
    public void startFishing(FishSpotService.Spot s) {
        goalKind = Goal.FISH;
        spot = s;
        goal = new Vec3(s.centre().getX() + 0.5, s.centre().getY() + 1.2, s.centre().getZ() + 0.5);
    }

    double arrivalDelay(BirdTuning t) {
        if (delayMin < 0) delayMin = BirdRules.arrivalDelayMin(role(), t, (Math.abs(getUUID().getMostSignificantBits()) % 10_000) / 10_000.0);
        return delayMin;
    }

    CarcassEntity nearestCarcass(ServerLevel level, double range) {
        CarcassEntity best = null;
        double bd = Double.MAX_VALUE;
        for (CarcassEntity c : level.getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(range, 24, range), x -> x.record().weightG() > 0)) {
            double d = c.distanceToSqr(this);
            if (d < bd && d <= range * range) {
                bd = d;
                best = c;
            }
        }
        return best;
    }

    double surfaceY(ServerLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)) + 0.05;
    }

    // ---- acting on it -------------------------------------------------------------------------------------------

    void act(ServerLevel level) {
        Vec3 g = goal;
        CarcassEntity c = null;
        if (goalKind == Goal.CARRION) {
            c = level.getEntity(carcassId) instanceof CarcassEntity ce && !ce.isRemoved() && ce.record().weightG() > 0 ? ce : null;
            if (c == null) {
                endGoal();
                return;
            }
            g = c.position();
        }
        if (g == null) {
            endGoal();
            return;
        }
        Vec3 d = g.subtract(position());
        double dist = d.length();
        if (dist > 0.9) {
            double step = Math.min(Math.min(8.0, Math.max(3.0, species().flyMps())) / 20.0, dist);
            setNoGravity(true);
            setMove(MOVE_FLY);
            setDeltaMovement(d.scale(step / dist));
            lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.FEET, position().add(d.x, 0, d.z));
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        setMove(MOVE_IDLE);
        switch (goalKind) {
            case FLOCK -> endGoal();
            case CARRION -> eat(level, c);
            case STORE -> {
                Theft.Thief thief = role() == BirdRules.Role.CORVID ? Theft.Thief.RAVEN : Theft.Thief.GRAY_JAY;
                TheftService.birdVisit(level, storePos, thief).ifPresent(msg -> BirdService.thefts++);
                fedUntilTick = level.getGameTime() + hours(4);
                endGoal();
            }
            case FISH -> {
                String sp = WaterLife.takeFish(level, spot);
                if (sp != null) {
                    BirdService.fishTaken++;
                    lastFish = sp;
                }
                fedUntilTick = level.getGameTime() + hours(6);
                endGoal();
            }
            default -> endGoal();
        }
    }

    static long hours(double h) { return Math.round(h * 60 * SliceClock.TICKS_PER_MINUTE); }

    void eat(ServerLevel level, CarcassEntity c) {
        if (++eatTick < 20) return;
        eatTick = 0;
        BirdTuning t = BirdConfig.tuning();
        pendingG += BirdRules.eatGrams(role(), t, 20.0 / SliceClock.TICKS_PER_MINUTE);
        int take = (int) Math.floor(pendingG);
        if (take >= 1) {
            pendingG -= take;
            CarcassRecord r = c.record();
            int left = Math.max(0, r.weightG() - take);
            take = r.weightG() - left;
            c.setRecord(new CarcassRecord(r.species(), r.sex(), r.cause(), r.deathMinute(), left, r.coat(), r.fresh()));
            if (left <= 0) c.discard();
            BirdService.scavengedG += take;
            eatenThisVisit += take;
        }
        double appetite = switch (role()) {
            case CORVID -> 300;
            case RAPTOR -> 500;
            case JAY -> t.jayGramsPerVisit();
            default -> 100;
        };
        if (eatenThisVisit >= appetite || c.isRemoved()) {
            eatenThisVisit = 0;
            fedUntilTick = level.getGameTime() + hours(2);
            endGoal();
        }
    }

    void endGoal() {
        goalKind = Goal.NONE;
        goal = null;
        carcassId = null;
        storePos = null;
        spot = null;
        eatTick = 0;
        setNoGravity(false);
        setMove(MOVE_IDLE);
    }

    @Override
    protected String animKey(AnimalState s, int move) {
        if (s == AnimalState.ROUTINE && move == MOVE_FLY) return "fly";
        return super.animKey(s, move);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 4,
                st -> st.setAndContinue(loop("animation." + speciesPath() + "." + animKey(syncedState(), moveHint())))));
    }
}
