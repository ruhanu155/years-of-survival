package com.yearofice.sim.fire;

import com.yearofice.sim.env.FireSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * A stove's firebox: a queue of armloads, a setting, creosote in the chimney, and the room's smoke. Pure; the block
 * entity feeds it game minutes. Creosote builds per armload burned (wet wood worse) and 3 an hour on Low.
 */
public final class StoveBurn {
    public final Stove.Kind kind;
    private final List<Armload> queue = new ArrayList<>();
    private FireSetting setting = FireSetting.STEADY;
    private boolean lit;
    private double creosote;
    private double smokePct;
    private double burnedArmloads;
    private double hotMinutes;

    public StoveBurn(Stove.Kind kind) { this.kind = kind; }

    public List<Armload> queue() { return queue; }
    public FireSetting setting() { return setting; }
    public void setting(FireSetting s) { setting = s; }
    public boolean lit() { return lit; }
    public void lit(boolean l) { lit = l; }
    public double creosote() { return creosote; }
    public void creosote(double c) { creosote = Math.max(0, Math.min(Stove.CREOSOTE_MAX, c)); }
    public double smokePct() { return smokePct; }
    public void smokePct(double s) { smokePct = Math.max(0, Math.min(100, s)); }
    public double burnedArmloads() { return burnedArmloads; }

    public double armloads() {
        double a = 0;
        for (Armload l : queue) a += l.amount();
        return a;
    }

    /** Adds a share of an armload if the box has room; returns the share taken. */
    public double load(Wood w, int moisturePct, double share) {
        double room = kind.maxArmloads() - armloads();
        double take = Math.min(room, share);
        if (take <= 1e-9) return 0;
        for (Armload l : queue) if (l.sameKind(w, moisturePct)) { l.add(take); return take; }
        queue.add(new Armload(w, moisturePct, take));
        return take;
    }

    /** Lights the box if the head armload will catch (green wood will not catch cold). */
    public boolean light() {
        if (queue.isEmpty()) return false;
        boolean any = false;
        for (Armload a : queue) if (!a.green()) any = true;
        if (!any) return false;
        lit = true;
        return true;
    }

    public void douse() { lit = false; }

    /** Heat to the room right now, watts (0 when out). */
    public double outputW(double draft) {
        if (!lit || queue.isEmpty()) return 0;
        Armload a = queue.get(0);
        return Stove.outputW(kind, setting, draft, a.wood, a.moisturePct);
    }

    /** What one span of burning did. */
    public record Result(double minutesBurned, double burnedArmloads, boolean chimneyFire) {}

    /**
     * Burns {@code minutes} at the given draft. A chimney fire is rolled once per whole hour burned on Hot; it puts
     * the fire out and clears the creosote. Smoke rises 10% a minute while unflued and burning, and clears 20% a
     * minute otherwise (an open door or a chimney holds the room clear).
     */
    public Result step(double minutes, double draft, boolean flued, RandomGenerator rng) {
        double remaining = minutes, burned = 0, used = 0;
        boolean fire = false;
        while (lit && remaining > 1e-9) {
            if (queue.isEmpty()) { lit = false; break; }
            Armload a = queue.get(0);
            double mpa = Stove.minutesPerArmload(kind, setting, draft, a.wood);
            double use = Math.min(remaining, a.amount() * mpa);
            double share = use / mpa;
            a.burn(share);
            burned += share;
            used += use;
            remaining -= use;
            creosote(creosote + Stove.creosotePerArmload(a.wood, a.moisturePct) * share
                    + (setting == FireSetting.LOW ? Stove.LOW_HOUR_CREOSOTE * use / 60.0 : 0));
            if (setting == FireSetting.HOT) {
                hotMinutes += use;
                while (hotMinutes >= 60) {
                    hotMinutes -= 60;
                    if (!fire && rng.nextDouble() < Stove.chimneyFireChance(creosote, setting)) fire = true;
                }
            }
            if (a.spent()) queue.remove(0);
            if (fire) break;
        }
        if (queue.isEmpty() && lit) lit = false;
        if (fire) { lit = false; creosote = 0; }
        burnedArmloads += burned;
        boolean smoky = used > 0 && !flued;
        smokePct(smokePct + (smoky ? Stove.SMOKE_PCT_PER_MIN * used : 0) - (flued || used <= 0 ? 20 * minutes : 0));
        return new Result(used, burned, fire);
    }
}
