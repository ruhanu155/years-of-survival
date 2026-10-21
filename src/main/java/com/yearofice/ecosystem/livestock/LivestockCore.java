package com.yearofice.ecosystem.livestock;

import com.yearofice.sim.livestock.Care;
import com.yearofice.sim.livestock.Growth;
import com.yearofice.sim.livestock.LivestockTuning;
import com.yearofice.sim.livestock.Produce;
import com.yearofice.sim.livestock.Spec;
import net.minecraft.nbt.CompoundTag;

/**
 * One animal's kept state, written into its own NBT (no attachment, so no save fixture): care, growth, the feed and
 * water it carries, when it last calved or laid, and the hour it was last stepped so a reloaded chunk catches up.
 * The Minecraft-facing work (blocks, rooms, the clock) is in {@link LivestockSystem}; this holds numbers.
 */
public final class LivestockCore {
    public Care.State care = Care.State.FRESH;
    public double progress = 1.0, ageDays = 0, feedKg = 0, waterL = 0, feedDebtKg = 0, eggAcc = 0, milkOwedL = 0;
    public boolean female = true;
    public long lastHour = Long.MIN_VALUE, lastBirthDay = -1000, dueDay = -1, milkOffUntilDay = -1, lastEggDay = -1;
    public int bond = 0;
    /** Seen by the pen scan each hour; not saved (a reload rescans). */
    public boolean penned;
    public long escapedDay = -1;
    public com.yearofice.sim.livestock.PenRisk.Fence fence = com.yearofice.sim.livestock.PenRisk.Fence.NONE;

    public boolean adult() { return Growth.stage(spec, progress, ageDays) == Growth.Stage.ADULT; }

    private Spec spec;

    /** A new core is a grown animal; a newborn sets {@code progress} and {@code ageDays} to 0. */
    public LivestockCore(Spec spec) {
        this.spec = spec;
        this.ageDays = spec.matureDays() * 1.5;
    }

    public Spec spec() { return spec; }

    public Growth.Stage stage() { return Growth.stage(spec, progress, ageDays); }

    public double massKg() { return Growth.massKg(spec, progress); }

    public void save(CompoundTag t) {
        t.putDouble("fed", care.fed());
        t.putDouble("watered", care.watered());
        t.putDouble("warm", care.warm());
        t.putDouble("condition", care.condition());
        t.putDouble("zero_hours", care.zeroHours());
        t.putDouble("progress", progress);
        t.putDouble("age_days", ageDays);
        t.putDouble("feed_kg", feedKg);
        t.putDouble("water_l", waterL);
        t.putDouble("feed_debt_kg", feedDebtKg);
        t.putDouble("egg_acc", eggAcc);
        t.putDouble("milk_owed_l", milkOwedL);
        t.putBoolean("female", female);
        t.putLong("last_hour", lastHour);
        t.putLong("last_birth_day", lastBirthDay);
        t.putLong("due_day", dueDay);
        t.putLong("milk_off_until_day", milkOffUntilDay);
        t.putLong("last_egg_day", lastEggDay);
        t.putInt("bond", bond);
        t.putLong("escaped_day", escapedDay);
    }

    public void load(CompoundTag t) {
        if (!t.contains("fed")) return;
        care = new Care.State(t.getDouble("fed"), t.getDouble("watered"), t.getDouble("warm"), t.getDouble("condition"), t.getDouble("zero_hours"));
        progress = t.getDouble("progress");
        ageDays = t.getDouble("age_days");
        feedKg = t.getDouble("feed_kg");
        waterL = t.getDouble("water_l");
        feedDebtKg = t.getDouble("feed_debt_kg");
        eggAcc = t.getDouble("egg_acc");
        milkOwedL = t.getDouble("milk_owed_l");
        female = t.getBoolean("female");
        lastHour = t.getLong("last_hour");
        lastBirthDay = t.getLong("last_birth_day");
        dueDay = t.getLong("due_day");
        milkOffUntilDay = t.getLong("milk_off_until_day");
        lastEggDay = t.getLong("last_egg_day");
        bond = t.getInt("bond");
        escapedDay = t.getLong("escaped_day");
    }

    /** One game hour of care. {@code browseShare} of the ration is free (grazing); the rest comes from the carried feed. */
    public void stepHour(LivestockTuning t, double browseShare, boolean waterNear, double roomC) {
        double ration = spec.feedKgPerDay() / 24 * (stage() == Growth.Stage.SUCKLING ? 0 : 1);
        double browse = ration * browseShare;
        double need = ration - browse;
        double eaten = Math.min(feedKg, need);
        feedKg -= eaten;
        feedDebtKg += need - eaten;
        double fedFraction = ration <= 0 ? 1 : (browse + eaten) / ration;
        double wration = spec.waterLPerDay() / 24;
        double waterFraction = 1;
        if (!waterNear) {
            double drunk = Math.min(waterL, wration);
            waterL -= drunk;
            waterFraction = wration <= 0 ? 1 : drunk / wration;
        }
        care = Care.step(care, spec, t, fedFraction, waterFraction, roomC);
    }

    /** One game day of growth and age. */
    public void stepDay(LivestockTuning t, boolean hasMilk) {
        ageDays += 1;
        progress = Growth.dayStep(spec, t, progress, ageDays, care.fed(), hasMilk);
    }

    public double milkToday(long absDay, boolean hayOnly) {
        if (!female || !adult() || absDay < milkOffUntilDay || !care.producing()) return 0;
        return Produce.milkLPerDay(spec, absDay - lastBirthDay, care.fed(), hayOnly);
    }
}
