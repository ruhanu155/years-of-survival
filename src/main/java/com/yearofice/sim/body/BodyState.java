package com.yearofice.sim.body;

import com.yearofice.sim.body.part.BodyRecord;
import java.util.Arrays;

/**
 * The whole {@code yearofice:body} record at schema 2, as the body model works on it (Minecraft development ›
 * The body attachment; Body tab › The body record, extended). Mutable on purpose: {@link BodyModel#step} works
 * in place so a step allocates nothing but the clothing copy. Only the body ticker writes it.
 *
 * <p>Hourly bins are a ring indexed by hour of day (kcal, water) or by hour mod 72 (macros), cleared as the
 * clock enters each hour, so "today" is bins {@code 0..hour} and the 24 h balance is the whole ring.
 *
 * <p>{@code slotWet} is the slice's stand-in for worn-garment wetness: one value per clothing slot (zone ×
 * layer), bare skin while nothing is worn. When clothing items land, their {@code yearofice:wetness}
 * components take over and this field is retired with a codec default.
 */
public final class BodyState {
    public static final int SCHEMA = 3;
    public static final int MACRO_HOURS = 72;

    public int heightCm = Metabolism.DEFAULT_HEIGHT_CM;
    public int sex = 0;
    public double kg = Metabolism.DEFAULT_KG;
    public double stomachKg = 0;
    public double water = 100;
    public double warmth = 100;
    public double energy = 10;
    public int awakeMin = 0;
    public boolean asleep = false;
    /** Minutes of the current sleep; a sleep of 4 h or more clears the awake count. */
    public int sleepRun = 0;
    public double sleepQ = 0;
    /** 0 none, 1 collapsing, 2 away, 3 returning. */
    public int sos = 0;
    /** kcal absorbed since the last step, gut factors already applied. */
    public double pendingKcal = 0;

    public final double[] kcalIn = new double[24];
    public final double[] kcalOut = new double[24];
    public final double[] drunkMl = new double[24];
    /** 72 hours × carbs, fat, protein grams. */
    public final double[] macroG = new double[MACRO_HOURS * 3];
    /** The absolute game hour the bins were last advanced to; −1 before the first step. */
    public long binHour = -1;

    /** Game minute a pending gut illness shows, or −1. */
    public long gutOnset = -1;
    /** Game minute an active gut illness ends, or −1. */
    public long gutUntil = -1;

    public final double[] slotWet = new double[Layer.SLOTS];

    /** Per-part health, Condition, frost and problems (the Body tab's schema-2 fields). */
    public BodyRecord parts = BodyRecord.healthy();

    // --- Schema 3 (M4 lane B): illness, metabolism extras, rest days, skin and hair ---------------------------
    /** Vitamin C reserve in days, 0–30; falls 1 a day (Blueprint › Health, Vitamin C). */
    public double vitC = 30;
    /** The nausea meter, 0–100. */
    public double nausea = 0;
    /** Active and pending illnesses, sorted by start then id. */
    public final java.util.List<IllnessModel.Active> ill = new java.util.ArrayList<>();
    /** Rest days: the run of finished Rest days and today's work, away and sleep minutes. */
    public int restRun = 0, restAwayMin = 0, restSleepMin = 0;
    public double restWorkMin = 0;
    /** The absolute day the Rest counters belong to; −1 before the first step. */
    public long restDay = -1;
    /** Hair and beard: game days since the last trim; per-player {@code facial_hair} setting. */
    public double hairDays = 0;
    public boolean facialHair = true;
    /** Game minute face grease and glove grease wear off, or −1. */
    public long faceGreaseUntil = -1, gloveGreaseUntil = -1;
    /** Minutes of sun on snow, wind and cold breath accrued toward sunburn, windburn and an iced beard. */
    public double sunMin = 0, windMin = 0, iceMin = 0;
    public boolean beardIced = false;

    // Adapter inputs for the skin rules, set before a step (NaN or −1: not known, the rule is off).
    public transient double ctxSunDeg = Double.NaN;
    public transient int ctxDayOfYear = -1;
    /** 0 bare face, 1 ruffed hood, 2 fully covered. */
    public transient int ctxFace = 0;
    /** Messenger line ids the last steps raised; the ticker drains it. */
    public final transient java.util.List<String> events = new java.util.ArrayList<>();

    // Derived each step, never saved.
    public transient FeelsLike lastFeel = null;
    /** "Outside like this: about N min" (ExposureSolver.shown): 0 now, -1 hours or never. */
    public transient int exposureMin = -1;
    /** True while the last step was on an unbroken Rest day near warmth. */
    public transient boolean restActive = false;
    public transient double lastEnergyCap = 10;
    public transient Activity lastActivity = Activity.REST;
    public transient double lastAirC = 10;

    public double bmi() { return Metabolism.bmi(kg, heightCm); }

    public double bmr() { return Metabolism.bmr(kg, heightCm, sex); }

    public double condition() { return parts.condition(); }

    public int beardStage() { return Skin.beardStage(this); }

    public boolean gutIll(long minute) { return gutUntil >= 0 && minute < gutUntil; }

    public ClothingState clothing() { return ClothingState.naked().withWet(slotWet.clone()); }

    public double wetness() { return Clothing.wetness(clothing()); }

    /** Clears every bin the clock has passed since the last step. */
    public void advanceBins(long minute) {
        long hour = Math.floorDiv(minute, 60);
        if (binHour < 0) {
            binHour = hour;
            return;
        }
        if (hour <= binHour) return;
        long from = Math.max(binHour + 1, hour - MACRO_HOURS + 1);
        for (long h = from; h <= hour; h++) {
            int d = (int) Math.floorMod(h, 24);
            if (h > hour - 24) {
                kcalIn[d] = 0;
                kcalOut[d] = 0;
                drunkMl[d] = 0;
            }
            int m = (int) Math.floorMod(h, MACRO_HOURS) * 3;
            macroG[m] = macroG[m + 1] = macroG[m + 2] = 0;
        }
        binHour = hour;
    }

    private static int hourOfDay(long minute) { return (int) Math.floorMod(Math.floorDiv(minute, 60), 24); }

    public double kcalInToday(long minute) { return sumToHour(kcalIn, hourOfDay(minute)); }

    public double kcalOutToday(long minute) { return sumToHour(kcalOut, hourOfDay(minute)); }

    public double drunkMlToday(long minute) { return sumToHour(drunkMl, hourOfDay(minute)); }

    public double balance24h() { return Arrays.stream(kcalIn).sum() - Arrays.stream(kcalOut).sum(); }

    /** The 72 h macro meter, 0–1: intake over three days of the daily target, capped. */
    public double macroMeter(int macro) {
        double sum = 0;
        for (int h = 0; h < MACRO_HOURS; h++) sum += macroG[h * 3 + macro];
        return Math.min(1, sum / (3 * Metabolism.MACRO_TARGET_G[macro]));
    }

    public double stomachPct(double capacityKg) { return Math.min(1, stomachKg / capacityKg); }

    private static double sumToHour(double[] bins, int hour) {
        double s = 0;
        for (int h = 0; h <= hour; h++) s += bins[h];
        return s;
    }

    public BodyState copy() {
        BodyState c = new BodyState();
        c.heightCm = heightCm; c.sex = sex; c.kg = kg; c.stomachKg = stomachKg; c.water = water;
        c.warmth = warmth; c.energy = energy; c.awakeMin = awakeMin; c.asleep = asleep; c.sleepRun = sleepRun;
        c.sleepQ = sleepQ; c.sos = sos; c.pendingKcal = pendingKcal; c.binHour = binHour;
        c.gutOnset = gutOnset; c.gutUntil = gutUntil; c.parts = parts;
        c.vitC = vitC; c.nausea = nausea; c.ill.addAll(ill); c.restRun = restRun; c.restWorkMin = restWorkMin;
        c.restAwayMin = restAwayMin; c.restSleepMin = restSleepMin; c.restDay = restDay; c.hairDays = hairDays;
        c.facialHair = facialHair; c.faceGreaseUntil = faceGreaseUntil; c.gloveGreaseUntil = gloveGreaseUntil;
        c.sunMin = sunMin; c.windMin = windMin; c.iceMin = iceMin; c.beardIced = beardIced;
        System.arraycopy(kcalIn, 0, c.kcalIn, 0, 24);
        System.arraycopy(kcalOut, 0, c.kcalOut, 0, 24);
        System.arraycopy(drunkMl, 0, c.drunkMl, 0, 24);
        System.arraycopy(macroG, 0, c.macroG, 0, macroG.length);
        System.arraycopy(slotWet, 0, c.slotWet, 0, slotWet.length);
        return c;
    }
}
