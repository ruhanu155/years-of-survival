package com.yearofice.ecosystem.species.bird;

import com.yearofice.ecosystem.YoiBird;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.ecosystem.SeasonalLooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * {@code yearofice:willow_ptarmigan} on the grouse rig (Birds and the air › Grouse and ptarmigan): walks and feeds,
 * freezes at 30, and flushes inside its flush distance (3 m base): a vertical roar of 1 s, a short flight at
 * 12 m/s some 3–5 m up for 25–35 m (the Blueprint's "30 m flight"), then it lands. White Oct 20 – Apr 20, the
 * moult blending over 21 days.
 */
public class WillowPtarmiganEntity extends YoiBird {
    public static final String ID = "yearofice:willow_ptarmigan";
    static final double CLIMB_PER_TICK = 0.25;

    private int phase; // 0 takeoff, 1 flight, 2 landing
    private double cruiseY;

    public WillowPtarmiganEntity(EntityType<? extends WillowPtarmiganEntity> type, Level level) { super(type, level); }

    @Override
    public String speciesId() { return ID; }

    @Override
    protected void updateLooks(int doy) { setCoat(SeasonalLooks.ptarmigan(doy).ordinal()); }

    @Override
    protected double wanderRadius() { return 6; }

    @Override
    protected void startFlee() {
        super.startFlee();
        phase = 0;
        cruiseY = getY() + 3 + random.nextDouble() * 2;
        getNavigation().stop();
        setNoGravity(true);
    }

    @Override
    protected void tickFlee(ServerLevel level) {
        fleeTicks++;
        Vec3 p = position();
        if (lastPos != null) fleeCoveredM += Math.hypot(p.x - lastPos.x, p.z - lastPos.z);
        lastPos = p;
        Vec3 away = new Vec3(p.x - fleeFrom.x, 0, p.z - fleeFrom.z);
        away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
        double speed = species().flyMps() / 20.0;
        setMove(MOVE_FLY);
        if (phase == 0) {
            setDeltaMovement(away.scale(speed * 0.3).add(0, CLIMB_PER_TICK, 0));
            if (getY() >= cruiseY || fleeTicks > 20) phase = 1;
        } else if (phase == 1) {
            setDeltaMovement(away.scale(speed).add(0, (cruiseY - getY()) * 0.2, 0));
            if (fleeCoveredM >= fleeTargetM - 4) phase = 2;
        } else {
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
            setDeltaMovement(away.scale(speed * 0.4).add(0, -0.2, 0));
            if (onGround() || getY() <= ground + 0.05) {
                setNoGravity(false);
                fleeCoveredM = Math.max(fleeCoveredM, fleeTargetM);
            }
        }
        lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.FEET, p.add(away));
        if (fleeTicks > 200) {
            setNoGravity(false);
            fleeCoveredM = fleeTargetM;
        }
    }

    @Override
    public boolean fleeFinished() { return super.fleeFinished() && !isNoGravity(); }

    @Override
    public void enterState(AnimalState s) {
        if (s != AnimalState.FLEE) setNoGravity(false);
        super.enterState(s);
    }

    @Override
    protected String animKey(AnimalState s, int move) {
        if (s == AnimalState.FLEE) return move == MOVE_FLY ? (phase == 2 ? "glide" : "fly") : "run";
        return super.animKey(s, move);
    }
}
