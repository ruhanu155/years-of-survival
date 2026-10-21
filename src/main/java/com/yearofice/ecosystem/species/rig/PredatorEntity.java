package com.yearofice.ecosystem.species.rig;

import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.LandAnimal;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.ai.ScentPoints;
import com.yearofice.ecosystem.ai.Sensing;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.ecosystem.Awareness;
import com.yearofice.sim.ecosystem.Gaits;
import com.yearofice.sim.ecosystem.Predation;
import com.yearofice.sim.time.Calendar;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The predator rigs' body (Land animals in depth › Mob sheets: predators; Blueprint › Who hunts whom): wolf,
 * coyote, red fox, lynx, wolverine and, through {@link com.yearofice.ecosystem.species.BearEntity}, the bears. A
 * predator runs the same state machine as prey against the player; its Feed act is the hunt: inside its hunting
 * hours, hungry and off cooldown, it takes the nearest prey from its species file that it sees or smells, chases
 * at the file's gait for at most {@code chase_cap_m} metres (a wolf 150, a lynx 60) and kills inside the kill
 * radius through the death pipeline, so the prey leaves a carcass. With no prey it follows carrion scent: carcasses
 * and gut piles, in the downwind cone of their smell, and eats there.
 */
public class PredatorEntity extends LandAnimal {
    protected Predation.Chase chase;
    protected YoiAnimal quarry;
    protected Vec3 chaseLast;
    protected long cooldownUntil, nextScanTick, nextSmellTick, eatUntil;
    protected Object scentTarget;
    private double lastChaseM;
    private int givenUp, kills, scavenged;

    public PredatorEntity(EntityType<? extends PredatorEntity> type, Level level) { super(type, level); }

    @Override
    public String speciesId() { return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).toString(); }

    public boolean chasing() { return chase != null; }

    /** Metres run in the last chase that ended (or the one in progress). */
    public double lastChaseM() { return chase != null ? chase.ranM() : lastChaseM; }

    public int chasesGivenUp() { return givenUp; }

    public int kills() { return kills; }

    public int scavenges() { return scavenged; }

    @Override
    protected void startFlee() {
        endChase(false);
        scentTarget = null;
        super.startFlee();
    }

    @Override
    protected double wanderRadius() { return 24; }

    @Override
    protected void tickFeed(ServerLevel level) {
        Predation.Rule rule = species().predator();
        long t = level.getGameTime();
        if (tickCount % 100 == 0) maybeCall(level);
        if (chase != null) {
            tickChase(level, rule);
            return;
        }
        if (rule != null && t >= cooldownUntil && hasDrives() && driveState().hunger > 0.35 && (huntAnyHour || rule.huntsAt((int) hourNow(level))) && t >= nextScanTick) {
            nextScanTick = t + 20;
            YoiAnimal prey = findPrey(level, rule);
            if (prey != null) {
                beginChase(prey, rule);
                return;
            }
        }
        if (hasDrives() && driveState().hunger > 0.3 && scavenge(level, t)) return;
        super.tickFeed(level);
    }

    boolean hasDrives() { return species().hasDrives(); }

    private boolean huntAnyHour;

    /** GameTests: hunt whatever the hour. */
    public void huntAnyHour() { huntAnyHour = true; }

    private long lastCallDay = -1;

    /** The species' dusk or season call, once a game day (M5 debt: calls). */
    protected void maybeCall(ServerLevel level) {
        long minute = SliceClock.minute(level);
        long day = Calendar.absDayOfMinute(minute);
        if (day == lastCallDay) return;
        var call = com.yearofice.sim.ecosystem.CallRules.routine(speciesId(), Calendar.dayOfYearOfMinute(minute), (int) hourNow(level));
        if (call == null) return;
        lastCallDay = day;
        com.yearofice.ecosystem.call.Calls.emit(level, position(), call.sound(), call.rangeM());
    }

    // ---- the hunt -----------------------------------------------------------------------------------------------

    /** The nearest prey of the file's list that this predator sees (in view, clear line) or smells (it is downwind of it). */
    protected YoiAnimal findPrey(ServerLevel level, Predation.Rule rule) {
        SpeciesDef d = species();
        double reach = Math.max(rule.detectM(), 1);
        AABB box = getBoundingBox().inflate(reach, 16, reach);
        YoiAnimal best = null;
        double bd = Double.MAX_VALUE;
        for (YoiAnimal a : level.getEntitiesOfClass(YoiAnimal.class, box, a -> a != this && a.isAlive() && rule.takes(a.speciesId()) && java.util.Objects.equals(a.testGroup(), testGroup()))) {
            double dist = distanceTo(a);
            if (dist > reach || dist >= bd) continue;
            boolean seen = dist <= d.senses().sightM() && Awareness.inView(d.senses().fovDeg(), getYHeadRot(), Sensing.bearing(position(), a.position()))
                    && Sensing.cover(level, getEyePosition(), a.getEyePosition()) > 0;
            boolean smelt = false;
            if (!seen && dist <= d.senses().smellM()) {
                double toward = Sensing.windToward(level, a);
                smelt = Math.abs(Mth.wrapDegrees(Sensing.bearing(a.position(), position()) - toward)) <= 25;
            }
            if (seen || smelt) {
                best = a;
                bd = dist;
            }
        }
        return best;
    }

    /** Paths toward a point that may lie beyond the path search range (follow range 48): a waypoint 30 m along the line when far. */
    protected boolean pathToward(Vec3 target, double speed) {
        Vec3 p = position();
        double dx = target.x - p.x, dz = target.z - p.z, d = Math.hypot(dx, dz);
        if (d > 30) {
            target = new Vec3(p.x + dx / d * 30, target.y, p.z + dz / d * 30);
        }
        return getNavigation().moveTo(target.x, target.y, target.z, speed);
    }

    protected void beginChase(YoiAnimal prey, Predation.Rule rule) {
        quarry = prey;
        chase = new Predation.Chase(rule.chaseCapM());
        chaseLast = position();
        pathToward(prey.position(), Gaits.input(rule.chaseMps()));
        setMove(MOVE_RUN);
    }

    protected void tickChase(ServerLevel level, Predation.Rule rule) {
        if (quarry == null || !quarry.isAlive() || quarry.isRemoved()) {
            endChase(false);
            return;
        }
        Vec3 p = position();
        chase.advance(Math.hypot(p.x - chaseLast.x, p.z - chaseLast.z));
        chaseLast = p;
        getLookControl().setLookAt(quarry, 30, 30);
        double reach = rule.killRadiusM() + quarry.getBbWidth() / 2 + getBbWidth() / 2;
        if (distanceTo(quarry) <= reach && Math.abs(quarry.getY() - getY()) < 2.5) {
            YoiAnimal prey = quarry;
            kills++;
            driveState().hunger = Math.max(0, driveState().hunger - 0.4);
            endChase(true);
            DeathPipeline.kill(prey, level.damageSources().mobAttack(this), "predator");
            return;
        }
        if (chase.over()) {
            givenUp++;
            endChase(false);
            return;
        }
        if (tickCount % 5 == 0 || getNavigation().isDone()) pathToward(quarry.position(), Gaits.input(rule.chaseMps()));
        setMove(MOVE_RUN);
    }

    protected void endChase(boolean killed) {
        if (chase == null) return;
        lastChaseM = chase.ranM();
        chase = null;
        quarry = null;
        SpeciesDef d = species();
        long cd = Math.round((d.predator() == null ? 60 : d.predator().cooldownMin()) * SliceClock.TICKS_PER_MINUTE);
        cooldownUntil = level().getGameTime() + (killed ? cd : cd / 2);
        getNavigation().stop();
        setMove(MOVE_IDLE);
    }

    // ---- carrion ------------------------------------------------------------------------------------------------

    /** Follows carrion scent downwind-in-cone to a carcass or gut pile and eats; true while it is doing that. */
    protected boolean scavenge(ServerLevel level, long t) {
        if (scentTarget != null && t < eatUntil) {
            getNavigation().stop();
            setMove(MOVE_FEED);
            return true;
        }
        if (scentTarget != null && eatUntil != 0 && t >= eatUntil) {
            finishEating(level);
            return true;
        }
        if (t < nextSmellTick) return scentTarget != null && walkTo(level);
        nextSmellTick = t + 40;
        SpeciesDef d = species();
        double smell = d.senses().smellM() * 3.0; // carrion: tripled for bears and wolverines (fresh meat), see plume table
        double scale = speciesId().endsWith("bear") || speciesId().endsWith("grizzly") || speciesId().endsWith("wolverine") ? 3.0 : 1.5;
        smell = d.senses().smellM() * scale;
        double windToward = Sensing.windToward(level, this);
        Object best = null;
        double bd = Double.MAX_VALUE;
        for (CarcassEntity c : level.getEntitiesOfClass(CarcassEntity.class, getBoundingBox().inflate(smell, 32, smell))) {
            double dist = distanceTo(c);
            if (dist > smell || dist >= bd) continue;
            if (dist > 30 && !downwind(c.position(), windToward)) continue;
            best = c;
            bd = dist;
        }
        for (BlockPos pt : ScentPoints.in(level)) {
            double dist = Math.sqrt(pt.distSqr(blockPosition()));
            if (dist > smell || dist >= bd) continue;
            if (dist > 30 && !downwind(Vec3.atCenterOf(pt), windToward)) continue;
            best = pt;
            bd = dist;
        }
        scentTarget = best;
        eatUntil = 0;
        return best != null && walkTo(level);
    }

    /** True when this predator sits inside the 25° cone the wind carries the source's scent into. */
    boolean downwind(Vec3 source, double windToward) {
        return Math.abs(Mth.wrapDegrees(Sensing.bearing(source, position()) - windToward)) <= 25;
    }

    boolean walkTo(ServerLevel level) {
        Vec3 at = scentTarget instanceof CarcassEntity c ? c.position() : Vec3.atCenterOf((BlockPos) scentTarget);
        if (scentTarget instanceof CarcassEntity c && (c.isRemoved())) { scentTarget = null; return false; }
        if (scentTarget instanceof BlockPos bp && level.isLoaded(bp) && !level.getBlockState(bp).is(EcosystemRegistry.GUT_PILE.get())) {
            ScentPoints.remove(level, bp);
            scentTarget = null;
            return false;
        }
        if (position().distanceToSqr(at) < 6.25) {
            eatUntil = level.getGameTime() + 20 * 40;
            getNavigation().stop();
            setMove(MOVE_FEED);
            scavenged++;
            return true;
        }
        if (getNavigation().isDone()) pathToward(at, Gaits.input(species().trotMps()));
        setMove(MOVE_WALK);
        return true;
    }

    /** A meal: a gut pile is eaten; a carcass loses 15 kg (the bear's day), or goes when little remains. */
    void finishEating(ServerLevel level) {
        if (scentTarget instanceof BlockPos bp) {
            ScentPoints.remove(level, bp);
            if (level.getBlockState(bp).is(EcosystemRegistry.GUT_PILE.get())) level.removeBlock(bp, false);
            driveState().hunger = Math.max(0, driveState().hunger - 0.5);
        } else if (scentTarget instanceof CarcassEntity c && !c.isRemoved()) {
            CarcassRecord r = c.record();
            int eat = (int) (speciesId().endsWith("wolf") ? 8000 : 15000);
            if (r.weightG() <= eat) c.discard();
            else c.setRecord(new CarcassRecord(r.species(), r.sex(), r.cause(), r.deathMinute(), r.weightG() - eat, r.coat(), r.fresh()));
            driveState().hunger = Math.max(0, driveState().hunger - 0.6);
        }
        scentTarget = null;
        eatUntil = 0;
        nextSmellTick = level.getGameTime() + 200;
    }

    @Override
    protected String animKey(AnimalState s, int move) {
        return switch (s) {
            case FLEE -> "run";
            case FREEZE -> "alert";
            default -> super.animKey(s, move);
        };
    }

    /** Debug line for {@code /yoi animal}. */
    public String huntDebug() {
        return String.format("state %s act %s hunger %.2f pts %d scan %d/%d wind %s denned %s; ", state(), driveAct(), driveState().hunger, ScentPoints.in((ServerLevel) level()).size(), nextScanTick, level().getGameTime(), windTowardOverride(), denned((ServerLevel) level()))
                + String.format("nav %s moveHint %s vel %.2f ground %s pos %s; ", !getNavigation().isDone(), moveHint(), getDeltaMovement().horizontalDistance(), onGround(), blockPosition().toShortString())
                + String.format("chase %s, last %.0f m, gave up %d, kills %d, carrion %d, target %s", chase != null, lastChaseM(), givenUp, kills, scavenged,
                scentTarget == null ? "none" : scentTarget instanceof BlockPos b ? b.toShortString() : "carcass");
    }

}
