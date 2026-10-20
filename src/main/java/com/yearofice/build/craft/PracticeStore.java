package com.yearofice.build.craft;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.practice.PracticeService;
import com.yearofice.sim.build.Practice;
import com.yearofice.sim.build.Skill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * {@code build.craft.PracticeService} of the doc (the {@code yearofice:practice} attachment's only writer). The
 * game day for the daily cap is the Clock's absolute day. The fire team's earlier stand-in, reps in the player's
 * persistent data under {@code yearofice_fire}, is read once and moved into the attachment.
 */
public final class PracticeStore implements PracticeService {
    static final String LEGACY_FIRE = "yearofice_fire";

    @Override
    public double reps(Player p, Skill s) {
        migrate(p);
        return p.getData(CraftAttachments.PRACTICE.get()).reps(s.id());
    }

    @Override public double add(Player p, Skill s, double reps) { return add(p, s, reps, s.n); }

    @Override
    public double add(Player p, Skill s, double reps, int n) {
        migrate(p);
        PracticeData d = p.getData(CraftAttachments.PRACTICE.get());
        long day = day(p);
        double g = Practice.gain(d.gainedToday(s.id(), day), reps, n);
        if (g > 0) p.setData(CraftAttachments.PRACTICE.get(), d.with(s.id(), g, day));
        return g;
    }

    @Override
    public void set(Player p, Skill s, double reps) {
        migrate(p);
        p.setData(CraftAttachments.PRACTICE.get(), p.getData(CraftAttachments.PRACTICE.get()).set(s.id(), reps));
    }

    static long day(Player p) {
        return ClockService.installed() ? ClockService.get().absDay() : p.level().getGameTime() / 24000L;
    }

    /** Moves the fire stand-in's reps into the attachment, once. */
    static void migrate(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(LEGACY_FIRE)) return;
        CompoundTag old = root.getCompound(LEGACY_FIRE);
        root.remove(LEGACY_FIRE);
        double reps = old.getDouble("reps");
        PracticeData d = p.getData(CraftAttachments.PRACTICE.get());
        if (reps > d.reps(Skill.FIRE.id())) p.setData(CraftAttachments.PRACTICE.get(), d.set(Skill.FIRE.id(), reps));
    }
}
