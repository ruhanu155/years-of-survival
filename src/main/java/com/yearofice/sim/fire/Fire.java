package com.yearofice.sim.fire;

import com.yearofice.sim.env.FireSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One fire: its lay (tinder, kindling, armloads, in the order they went in), its state and setting, and the
 * timers that move it. Pure Java; {@code com.yearofice.fire.FireRingBlockEntity} owns one and steps it each game
 * minute ({@link #stepMinute}) and each tick while an ember lives ({@link #tick}).
 *
 * <p>The machine (Blueprint › Fire, water and light › The fire state machine): a match or a bow-drill ember in
 * dry tinder becomes a tinder flame; the flame catches two handfuls of dry kindling in its 2 minutes, or dies; a
 * small fire burns 20 minutes on its kindling and catches an armload; a fire burns armloads at its setting;
 * a fire that burned at least one armload leaves coals for an hour; banked coals keep 6 hours and relight with
 * fresh tinder and one puff. Rain drops an uncovered fire one state per 10 (heavy) or 30 (light) minutes, and
 * snowfall puts out a Low fire in 20 minutes and slows the others by a quarter.
 *
 * <p>Order matters: tinder, then kindling, then fuel. A lay built out of order does not light ("skipping one is
 * why it smokes and dies").
 */
public final class Fire {

    /** One entry in an unlit lay, in the order it went in. */
    public record LayEntry(LayPart part, Wood wood, int moisturePct, double amount) {}

    /** Why a fire changed state, or why an action did nothing. {@code line} is the player-facing reason. */
    public record Change(FireState from, FireState to, String line) {
        public boolean changed() { return from != to; }
    }

    /** The result of an action on the fire: did it take, and the line to show. */
    public record Result(boolean ok, String line) {
        static Result ok(String line) { return new Result(true, line); }
        static Result no(String line) { return new Result(false, line); }
    }

    private FireState state = FireState.OUT;
    private FireSetting setting = FireSetting.STEADY;
    private int minutesInState;
    private int catchMinutes;
    private double rainMinutes;
    private int snowMinutes;
    private int puffsNeeded;
    private int emberTicks;
    private boolean coalBed;
    private double burnedArmloads;
    private boolean packedWarned;

    private double tinderMoisture = -1;
    private int kindling;
    private double kindlingMoisture;
    private final List<LayEntry> lay = new ArrayList<>();
    private final List<Armload> fuel = new ArrayList<>();

    // ---- reading -------------------------------------------------------------------------------------------

    public FireState state() { return state; }
    public FireSetting setting() { return setting; }
    public int minutesInState() { return minutesInState; }
    public boolean hasTinder() { return tinderMoisture >= 0; }
    public int tinderMoisture() { return (int) Math.ceil(Math.max(0, tinderMoisture)); }
    public int kindling() { return kindling; }
    public int kindlingMoisture() { return (int) Math.ceil(kindlingMoisture); }
    public List<LayEntry> lay() { return Collections.unmodifiableList(lay); }
    public List<Armload> fuel() { return Collections.unmodifiableList(fuel); }
    public double burnedArmloads() { return burnedArmloads; }
    public int puffsNeeded() { return puffsNeeded; }
    public int emberTicks() { return emberTicks; }
    public double rainMinutes() { return rainMinutes; }

    public double armloads() {
        double s = 0;
        for (Armload a : fuel) s += a.amount();
        return s;
    }

    /** The armload burning now (the front of the queue while the state is {@link FireState#FIRE}). */
    public Armload burning() { return state == FireState.FIRE && !fuel.isEmpty() ? fuel.get(0) : null; }

    /** The top layer of the unlit lay, or null for a bare ring. */
    public LayPart top() { return lay.isEmpty() ? null : lay.get(lay.size() - 1).part(); }

    /** True when every entry of the lay went in after the layer it belongs on. */
    public boolean layInOrder() {
        LayPart top = null;
        for (LayEntry e : lay) {
            if (!LayPart.inOrder(top, e.part())) return false;
            top = e.part();
        }
        return true;
    }

    public int light() { return FireRules.light(state, setting); }

    /** Heat within 3 m in °C, after the burning wood's heat and moisture factors, never over +25. */
    public double heatC() {
        double base = FireRules.baseHeatC(state, setting);
        Armload a = burning();
        if (a != null) base *= a.wood.heatFactor * FireRules.moistureHeat(a.moisturePct);
        return Math.max(0, Math.min(25, base));
    }

    /**
     * Heat output in kW: a fire's armloads an hour × 11.8 kWh × its heat factors. Small fire, tinder flame,
     * coals and banked coals use fixed estimates (banked 0.3 kW is the stove figure in Warm rooms).
     */
    public double kW(FireTuning t) {
        return switch (state) {
            case OUT, EMBER -> 0;
            case TINDER -> 0.3;
            case SMALL -> 2.0;
            case COALS -> 1.0;
            case BANKED -> 0.3;
            case FIRE -> {
                Armload a = burning();
                double perHour = 60 * FireRules.fuelUse(setting) / (t.ringArmloadMin() * a.wood.burnFactor);
                yield perHour * FireRules.KWH_PER_ARMLOAD * a.wood.heatFactor * FireRules.moistureHeat(a.moisturePct);
            }
        };
    }

    public int smoke() { return FireRules.smoke(state, burning()); }

    /** Game minutes until this state ends on its own: fuel left for a fire, the timer for the others. */
    public double minutesLeft(FireTuning t) {
        return switch (state) {
            case OUT, EMBER -> 0;
            case TINDER -> Math.max(0, t.tinderFlameMin() - minutesInState);
            case SMALL -> Math.max(0, t.smallFireMin() - minutesInState);
            case COALS -> Math.max(0, t.coalsMin() - minutesInState);
            case BANKED -> Math.max(0, t.bankedMin() - minutesInState);
            case FIRE -> {
                double m = 0;
                for (Armload a : fuel) m += a.amount() * t.ringArmloadMin() * a.wood.burnFactor / FireRules.fuelUse(setting);
                yield m;
            }
        };
    }

    /** True when the fire holds more than it should: more than 3 armloads, or Hot with more than 2. */
    public boolean packed(FireTuning t) {
        double n = armloads();
        return n > t.packedArmloads() + 1e-9 || (state == FireState.FIRE && setting == FireSetting.HOT && n > t.packedHotArmloads() + 1e-9);
    }

    // ---- building the lay ----------------------------------------------------------------------------------

    /**
     * Adds one part. Tinder and kindling go only on an unlit ring, a tinder flame (kindling) or banked coals
     * (fresh tinder to relight). Fuel goes on at any time up to the ring's limit. A part added out of order is
     * accepted, so the player can see the mistake, but the lay will not light until it is taken back off.
     */
    public Result add(LayPart part, Wood wood, int moisturePct, double amount, FireTuning t) {
        switch (part) {
            case TINDER -> {
                if (hasTinder()) return Result.no("There is tinder in the ring already.");
                if (state != FireState.OUT && state != FireState.BANKED) return Result.no("Tinder is for lighting a fire.");
                tinderMoisture = moisturePct;
            }
            case KINDLING -> {
                if (state != FireState.OUT && state != FireState.BANKED && state != FireState.TINDER && state != FireState.EMBER)
                    return Result.no("The fire is past kindling. Add an armload.");
                if (kindling >= 2 * t.kindlingNeeded()) return Result.no("That is enough kindling.");
                kindlingMoisture = kindling == 0 ? moisturePct : Math.max(kindlingMoisture, moisturePct);
                kindling++;
            }
            case FUEL -> {
                if (wood == null || amount <= 0) return Result.no("That will not burn.");
                if (armloads() + amount > t.maxArmloads() + 1e-9) return Result.no("The ring is full.");
                Armload last = fuel.isEmpty() ? null : fuel.get(fuel.size() - 1);
                if (last != null && last.sameKind(wood, moisturePct) && last != burning()) last.add(amount);
                else fuel.add(new Armload(wood, moisturePct, amount));
            }
        }
        boolean unlit = state == FireState.OUT;
        if (unlit || (state == FireState.BANKED && part != LayPart.FUEL)) {
            boolean ordered = LayPart.inOrder(top(), part);
            lay.add(new LayEntry(part, wood, moisturePct, amount));
            if (!ordered) {
                return Result.ok(switch (part) {
                    case TINDER -> "Tinder goes under the kindling, not on top.";
                    case KINDLING -> "Kindling goes on tinder. Lay tinder first.";
                    case FUEL -> "Fuel on bare tinder smothers it. Lay kindling first.";
                });
            }
        }
        if (part == LayPart.FUEL && packed(t)) {
            if (!packedWarned) {
                packedWarned = true;
                return Result.ok("Packed too full: it may spread.");
            }
        } else if (!packed(t)) {
            packedWarned = false;
        }
        return Result.ok(null);
    }

    /** Takes the last part of an unlit lay back off; returns it, or null when there is nothing to take. */
    public LayEntry takeBack() {
        if (state != FireState.OUT || lay.isEmpty()) return null;
        LayEntry e = lay.remove(lay.size() - 1);
        switch (e.part()) {
            case TINDER -> tinderMoisture = -1;
            case KINDLING -> {
                kindling = Math.max(0, kindling - 1);
                kindlingMoisture = 0;
                for (LayEntry k : lay) if (k.part() == LayPart.KINDLING) kindlingMoisture = Math.max(kindlingMoisture, k.moisturePct());
            }
            case FUEL -> {
                for (int i = fuel.size() - 1; i >= 0; i--) {
                    Armload a = fuel.get(i);
                    if (a.sameKind(e.wood(), e.moisturePct())) {
                        a.burn(e.amount());
                        if (a.spent()) fuel.remove(i);
                        break;
                    }
                }
            }
        }
        return e;
    }

    // ---- lighting ------------------------------------------------------------------------------------------

    /** Why a lay cannot take a flame now, or null when it can. */
    public String cannotLight(FireTuning t) {
        if (state == FireState.BANKED) return hasTinder() ? null : "Lay fresh tinder on the coals.";
        if (state != FireState.OUT) return "The fire is already going.";
        if (!hasTinder()) return "Lay tinder first.";
        if (!layInOrder()) return "The lay is wrong: tinder, then kindling, then fuel.";
        if (tinderMoisture > t.tinderMaxMoisture()) return "The tinder is wet. It will not catch.";
        return null;
    }

    /** The chance a match lights: 95% in calm air, 50% in wind; a damp match never does. */
    public static double matchChance(double windMs, int matchMoisture, FireTuning t) {
        if (matchMoisture > t.matchMaxMoisture()) return 0;
        return (windMs >= t.matchWindMs() ? t.matchWindPct() : t.matchCalmPct()) / 100.0;
    }

    /** The ember's life in ticks: 60 s, halved in wind over 6 m/s unless the player is sheltered. */
    public static int emberLifeTicks(double windMs, boolean sheltered, FireTuning t) {
        int s = t.emberLifeS();
        if (windMs > t.emberWindMs() && !sheltered) s /= 2;
        return s * 20;
    }

    /**
     * Strikes a match on the lay. {@code roll} is a uniform draw in [0, 1). On success the tinder holds a match
     * flame (an {@link FireState#EMBER}) that needs two puffs (Blueprint › Blowing a flame from coals or a match).
     */
    public Result strikeMatch(double roll, int matchMoisture, FireWeather w, boolean sheltered, FireTuning t) {
        String why = cannotLight(t);
        if (why != null) return Result.no(why);
        double p = matchChance(w.windMs(), matchMoisture, t);
        if (matchMoisture > t.matchMaxMoisture()) return Result.no("The match is damp. It will not strike.");
        if (roll >= p) return Result.no(w.windMs() >= t.matchWindMs() ? "The wind took the match." : "The match broke.");
        if (state == FireState.BANKED) return relight();
        enter(FireState.EMBER);
        puffsNeeded = 2;
        emberTicks = emberLifeTicks(w.windMs(), sheltered, t);
        return Result.ok("The tinder smoulders. Blow on it.");
    }

    /** A bow-drill ember, already blown to a flame in the minigame: the tinder catches. */
    public Result emberCaught(FireTuning t) {
        String why = cannotLight(t);
        if (why != null) return Result.no(why);
        if (state == FireState.BANKED) return relight();
        enter(FireState.TINDER);
        return Result.ok("The tinder catches.");
    }

    /** One puff of breath: grows an ember, or relights banked coals under fresh tinder. */
    public Result puff(FireTuning t) {
        if (state == FireState.EMBER) {
            puffsNeeded = Math.max(0, puffsNeeded - 1);
            if (puffsNeeded > 0) return Result.ok("It glows. Again.");
            enter(FireState.TINDER);
            return Result.ok("Flame.");
        }
        if (state == FireState.BANKED) {
            if (!hasTinder()) return Result.no("Lay fresh tinder on the coals first.");
            return relight();
        }
        return Result.no(null);
    }

    private Result relight() {
        enter(FireState.SMALL);
        consumeLay();
        burnedArmloads = Math.max(1, burnedArmloads);
        coalBed = true;
        return Result.ok("The coals take the tinder.");
    }

    /** Banks coals under ash (hold right mouse, 3 s). */
    public Result bank() {
        if (state != FireState.COALS) return Result.no(state == FireState.BANKED ? "Already banked." : "Only coals can be banked.");
        enter(FireState.BANKED);
        return Result.ok("Coals banked under ash. They keep about 6 hours.");
    }

    public Result setSetting(FireSetting s) {
        if (state != FireState.FIRE) return Result.no("Only a burning fire has a setting.");
        setting = s;
        return Result.ok("Fire, " + word(s) + ".");
    }

    public static String word(FireSetting s) {
        return switch (s) {
            case LOW -> "Low";
            case STEADY -> "Steady";
            case HOT -> "Hot";
        };
    }

    // ---- time ----------------------------------------------------------------------------------------------

    /** One server tick: only the ember counts real time. Returns a change when the ember dies. */
    public Change tick() {
        if (state != FireState.EMBER) return null;
        if (--emberTicks > 0) return null;
        FireState from = state;
        enter(FireState.OUT);
        return new Change(from, FireState.OUT, "The ember died. Blow sooner.");
    }

    /** One game minute. Returns the change, if the state changed. */
    public Change stepMinute(FireWeather w, FireTuning t) {
        FireState from = state;
        minutesInState++;

        // Rain on an uncovered, burning fire: one state per interval. Banked coals sit under ash and are covered.
        if (state.burning() && w.rainOnFire()) {
            rainMinutes += 1;
            int step = w.heavy() ? t.heavyRainStepMin() : t.lightRainStepMin();
            if (rainMinutes >= step) {
                rainMinutes -= step;
                return dropForRain(from);
            }
        } else {
            rainMinutes = 0;
        }
        if (state == FireState.OUT && w.rainOnFire()) wetLay(t.rainWetsPerHour() / 60.0);

        if (state == FireState.FIRE && setting == FireSetting.LOW && w.snowOnFire()) {
            if (++snowMinutes >= t.snowLowOutMin()) {
                enter(FireState.OUT);
                return new Change(from, state, "Snow put out the Low fire.");
            }
        } else {
            snowMinutes = 0;
        }

        switch (state) {
            case OUT, EMBER -> { }
            case TINDER -> {
                if (kindling >= t.kindlingNeeded() && kindlingMoisture <= t.tinderMaxMoisture()) {
                    enter(FireState.SMALL);
                    consumeLay();
                    return new Change(from, state, "The kindling caught. Add an armload.");
                }
                if (minutesInState > t.tinderFlameMin()) {
                    String why = kindling == 0 ? "The tinder burned out: no kindling."
                            : kindling < t.kindlingNeeded() ? "The tinder burned out: not enough kindling."
                            : "The tinder burned out: the kindling is wet.";
                    enter(FireState.OUT);
                    consumeLay();
                    return new Change(from, state, why);
                }
            }
            case SMALL -> {
                if (catchNext(coalBed ? Source.COALS : Source.SMALL, t)) return new Change(from, state, "The wood caught.");
                if (minutesInState > t.smallFireMin()) {
                    boolean anyGreen = fuel.stream().anyMatch(Armload::green);
                    enter(burnedArmloads > 0 ? FireState.COALS : FireState.OUT);
                    return new Change(from, state, state == FireState.COALS ? "Burned down to coals."
                            : anyGreen ? "The small fire burned out: green wood needs a Hot fire."
                            : fuel.isEmpty() ? "The small fire burned out: add an armload." : "The small fire burned out: that wood needs a Fire.");
                }
            }
            case FIRE -> {
                double rate = FireRules.fuelUse(setting) / t.ringArmloadMin();
                if (w.snowOnFire()) rate *= t.snowBurnFactor();
                if (!w.covered() && w.windMs() > t.windBurnMs()) rate *= t.windBurnFactor();
                double left = 1.0; // one minute of burning
                while (left > 1e-9 && !fuel.isEmpty()) {
                    Armload a = fuel.get(0);
                    double share = left * rate / a.wood.burnFactor;
                    double unused = a.burn(share);
                    burnedArmloads += share - unused;
                    left = share <= 0 ? 0 : left * unused / share;
                    if (a.spent()) {
                        fuel.remove(0);
                        if (!frontCatches(Source.FIRE)) break;
                    }
                }
                if (fuel.isEmpty() || !frontCatches(Source.FIRE)) {
                    enter(FireState.COALS);
                    return new Change(from, state, fuel.isEmpty() ? "Burned down to coals." : "Burned down to coals: green wood needs a Hot fire.");
                }
            }
            case COALS -> {
                if (catchNext(Source.COALS, t)) return new Change(from, state, "The coals took the wood.");
                if (minutesInState > t.coalsMin()) {
                    enter(FireState.OUT);
                    return new Change(from, state, "The coals went cold.");
                }
            }
            case BANKED -> {
                if (minutesInState > t.bankedMin()) {
                    enter(FireState.OUT);
                    return new Change(from, state, "The banked coals went cold.");
                }
            }
        }
        return state != from ? new Change(from, state, null) : null;
    }

    /** Runs {@code minutes} game minutes in the same weather: the closed-form catch-up for a ring out of sight. */
    public Change advance(int minutes, FireWeather w, FireTuning t) {
        Change last = null;
        for (int i = 0; i < minutes; i++) {
            Change c = stepMinute(w, t);
            if (c != null) last = c;
        }
        return last;
    }

    // ---- debug ---------------------------------------------------------------------------------------------

    /** /yoi fire state: puts the fire straight into a state, adding one armload of dead spruce when a fire needs fuel. */
    public void force(FireState s, FireSetting set, FireTuning t) {
        consumeLay();
        if (s == FireState.FIRE) {
            setting = set == null ? setting : set;
            if (fuel.isEmpty()) fuel.add(new Armload(Wood.DEAD_SPRUCE, 18, 1.0));
        }
        if (s == FireState.COALS || s == FireState.BANKED) burnedArmloads = Math.max(1, burnedArmloads);
        enter(s);
        if (s == FireState.EMBER) {
            puffsNeeded = 2;
            emberTicks = emberLifeTicks(0, true, t);
        }
    }

    /** /yoi fire fuel: replaces the queue with {@code armloads} of one wood. */
    public void setFuel(double armloads, Wood wood, int moisturePct) {
        fuel.clear();
        if (armloads > 0) fuel.add(new Armload(wood, moisturePct, armloads));
        if (state == FireState.FIRE && fuel.isEmpty()) enter(FireState.COALS);
    }

    // ---- saving --------------------------------------------------------------------------------------------

    /** Everything a save needs, in plain fields; {@link #restore} is the inverse. */
    public record Snapshot(FireState state, FireSetting setting, int minutesInState, int catchMinutes, double rainMinutes,
                           int snowMinutes, int puffsNeeded, int emberTicks, double burnedArmloads, boolean packedWarned,
                           boolean coalBed, double tinderMoisture, int kindling, double kindlingMoisture, List<LayEntry> lay,
                           List<Armload> fuel) {}

    public Snapshot snapshot() {
        List<Armload> f = new ArrayList<>();
        for (Armload a : fuel) f.add(new Armload(a.wood, a.moisturePct, a.amount()));
        return new Snapshot(state, setting, minutesInState, catchMinutes, rainMinutes, snowMinutes, puffsNeeded, emberTicks,
                burnedArmloads, packedWarned, coalBed, tinderMoisture, kindling, kindlingMoisture, List.copyOf(lay), f);
    }

    public static Fire restore(Snapshot s) {
        Fire f = new Fire();
        f.state = s.state();
        f.setting = s.setting();
        f.minutesInState = s.minutesInState();
        f.catchMinutes = s.catchMinutes();
        f.rainMinutes = s.rainMinutes();
        f.snowMinutes = s.snowMinutes();
        f.puffsNeeded = s.puffsNeeded();
        f.emberTicks = s.emberTicks();
        f.burnedArmloads = s.burnedArmloads();
        f.packedWarned = s.packedWarned();
        f.coalBed = s.coalBed();
        f.tinderMoisture = s.tinderMoisture();
        f.kindling = s.kindling();
        f.kindlingMoisture = s.kindlingMoisture();
        f.lay.addAll(s.lay());
        for (Armload a : s.fuel()) f.fuel.add(new Armload(a.wood, a.moisturePct, a.amount()));
        return f;
    }

    // ---- internals -----------------------------------------------------------------------------------------

    private enum Source { SMALL, FIRE, COALS }

    private boolean catches(Armload a, Source s) {
        return switch (s) {
            case SMALL -> FireRules.catchesFromSmall(a);
            case FIRE -> FireRules.catchesFromFire(a, setting);
            case COALS -> FireRules.catchesFromCoals(a);
        };
    }

    private boolean frontCatches(Source s) {
        if (fuel.isEmpty()) return false;
        for (int i = 0; i < fuel.size(); i++) {
            if (catches(fuel.get(i), s)) {
                if (i > 0) fuel.add(0, fuel.remove(i));
                return true;
            }
        }
        return false;
    }

    /** Small fire or coals: the first armload that can catch takes {@code armloadCatchMin} minutes, then burns. */
    private boolean catchNext(Source s, FireTuning t) {
        if (!frontCatches(s)) {
            catchMinutes = 0;
            return false;
        }
        if (++catchMinutes < t.armloadCatchMin()) return false;
        enter(FireState.FIRE);
        return true;
    }

    private Change dropForRain(FireState from) {
        if (state == FireState.FIRE && setting != FireSetting.LOW) {
            setting = setting == FireSetting.HOT ? FireSetting.STEADY : FireSetting.LOW;
            return new Change(from, state, "Rain knocked the fire down to " + word(setting) + ".");
        }
        enter(FireState.OUT);
        consumeLay();
        return new Change(from, state, "Rain put the fire out.");
    }

    private void wetLay(double points) {
        if (hasTinder()) tinderMoisture = Math.min(100, tinderMoisture + points);
        if (kindling > 0) kindlingMoisture = Math.min(100, kindlingMoisture + points);
    }

    private void consumeLay() {
        tinderMoisture = -1;
        kindling = 0;
        kindlingMoisture = 0;
        lay.clear();
    }

    private void enter(FireState s) {
        if (s != state) {
            if (s == FireState.OUT) {
                burnedArmloads = 0;
                setting = FireSetting.STEADY;
            }
            if (s != FireState.SMALL) coalBed = false;
            state = s;
        }
        minutesInState = 0;
        catchMinutes = 0;
        snowMinutes = 0;
        if (s != FireState.EMBER) {
            puffsNeeded = 0;
            emberTicks = 0;
        }
    }
}
