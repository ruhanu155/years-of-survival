package com.yearofice.ecosystem.encounter;

import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.ecosystem.species.BearEntity;
import com.yearofice.hunt.HuntConfig;
import com.yearofice.hunt.danger.Encounters;
import com.yearofice.sim.ecosystem.BearContact;
import com.yearofice.sim.ecosystem.BearContact.Phase;
import com.yearofice.sim.hunt.EncounterRules;
import com.yearofice.sim.hunt.EncounterRules.Charge;
import com.yearofice.sim.hunt.EncounterRules.Reaction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Wires {@code Encounters.bearCharge} into the live bear (RECONCILE 306): a bear whose senses a player has startled
 * (moved inside 25 m) or who is on carrion (inside 50 m) walks the contact sequence: notice, huff, rush, and a bluff
 * stop or contact. One driver per bear body; {@link #trace} records each phase with its distance for the GameTest.
 */
public final class BearContactDriver {
    /** Test hooks: pin the three rolls (notice length, rush outcome, bluff stop). */
    public volatile Double noticeRoll, chargeRoll, stopRoll;

    public Phase phase = Phase.NONE;
    public final List<String> trace = new ArrayList<>();
    public Charge outcome = Charge.NONE;

    private ServerPlayer target;
    private long phaseEnd, cooldownUntil, startTick;
    private double commitM, stopM, roll;
    private Reaction reaction = Reaction.STAND_GROUND;
    private Vec3 lastPos;
    private long lastTick;
    private double speed;

    private static double flat(BearEntity b, ServerPlayer p) { return Math.hypot(b.getX() - p.getX(), b.getZ() - p.getZ()); }

    public void tick(BearEntity b, ServerLevel level) {
        long t = level.getGameTime();
        if (phase == Phase.NONE) {
            if (t < cooldownUntil || t % 5 != 0) return;
            scan(b, level, t);
            return;
        }
        if (target == null || !target.isAlive() || target.level() != level) { end(b, t); return; }
        sample(target, t);
        double d = flat(b, target);
        switch (phase) {
            case NOTICE -> {
                b.holdAndLook(target);
                if (d > BearContact.ON_CARRION_M + 10) end(b, t);
                else if (t >= phaseEnd) {
                    if (d > BearContact.ON_CARRION_M) { end(b, t); return; }
                    enter(Phase.HUFF, d, t, BearContact.HUFF_TICKS);
                    b.huff();
                }
            }
            case HUFF -> {
                b.holdAndLook(target);
                if (t < phaseEnd) return;
                commitM = d;
                reaction = BearContact.reaction(speed);
                roll = chargeRoll != null ? chargeRoll : b.getRandom().nextDouble();
                boolean grizzly = b.speciesId().endsWith("grizzly");
                outcome = EncounterRules.bearCharge(d, false, grizzly, reaction, roll, HuntConfig.tuning());
                if (outcome == Charge.NONE) { end(b, t); return; }
                stopM = outcome == Charge.BLUFF ? BearContact.bluffStopM(stopRoll != null ? stopRoll : b.getRandom().nextDouble()) : BearContact.CONTACT_M;
                enter(Phase.CHARGE, d, t, BearContact.RUSH_TIMEOUT_TICKS);
            }
            case CHARGE -> {
                b.rushAt(target);
                if (d <= stopM || t >= phaseEnd) {
                    boolean grizzly = b.speciesId().endsWith("grizzly");
                    Encounters.bearCharge(target, commitM, false, grizzly, reaction, roll);
                    line(HudLineType.NEARMISS, outcome == Charge.BLUFF ? "bear_bluff" : "bear_contact", d);
                    enter(Phase.LEAVE, d, t, BearContact.LEAVE_TICKS);
                    b.stopMoving();
                }
            }
            case LEAVE -> {
                b.holdAndLook(target);
                if (t >= phaseEnd) {
                    if (outcome == Charge.CONTACT) Encounters.bearLeaves(target);
                    end(b, t);
                }
            }
            default -> { }
        }
    }

    private void scan(BearEntity b, ServerLevel level, long t) {
        ServerPlayer best = null;
        double bd = BearContact.ON_CARRION_M;
        for (var le : com.yearofice.ecosystem.ai.ThreatSources.near(level, b.position(), BearContact.ON_CARRION_M, b)) {
            if (!(le instanceof ServerPlayer p) || p.isSpectator() || p.isCreative() || !p.isAlive()) continue;
            double d = flat(b, p);
            if (d <= bd) { bd = d; best = p; }
        }
        if (best == null) { lastPos = null; return; }
        boolean first = lastPos == null || !best.equals(target);
        sample(best, t);
        if (first) { target = best; return; }
        boolean moved = speed > 0.1;
        if (!BearContact.provokes(bd, moved, b.onCarrion())) return;
        target = best;
        startTick = t;
        trace.clear();
        double nr = noticeRoll != null ? noticeRoll : b.getRandom().nextDouble();
        enter(Phase.NOTICE, bd, t, BearContact.noticeTicks(nr));
        line(HudLineType.ALERT, "bear_noticed", bd);
    }

    private void sample(ServerPlayer p, long t) {
        Vec3 now = p.position();
        if (lastPos != null && t > lastTick) speed = Math.hypot(now.x - lastPos.x, now.z - lastPos.z) / ((t - lastTick) / 20.0);
        lastPos = now;
        lastTick = t;
    }

    private void enter(Phase p, double d, long t, int ticks) {
        phase = p;
        phaseEnd = t + ticks;
        trace.add(p.name() + "@" + Math.round(d * 10) / 10.0);
    }

    private void end(BearEntity b, long t) {
        phase = Phase.NONE;
        cooldownUntil = t + BearContact.COOLDOWN_TICKS;
        b.stopMoving();
        target = null;
        lastPos = null;
    }

    private void line(HudLineType type, String id, double d) {
        if (target == null || !HudBus.installed()) return;
        HudBus.get().post(target, HudLine.of(type, id, "ecosystem.bear", HudArg.of(Math.round(d))));
    }
}
