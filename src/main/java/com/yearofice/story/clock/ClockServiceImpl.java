package com.yearofice.story.clock;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.event.RateChangedEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.core.hash.StageHash;
import com.yearofice.core.rules.YoiConfig;
import com.yearofice.net.YoiNet;
import com.yearofice.net.payload.ClockSyncPayload;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.time.Moon;
import com.yearofice.sim.time.Rate;
import com.yearofice.sim.time.RateArbiter;
import com.yearofice.sim.time.Rates;
import com.yearofice.sim.time.Sun;
import com.yearofice.sim.time.VanillaTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/**
 * {@code story.clock}: the one world clock (Layer 1). Each server tick, after every system has asked for its
 * rates, {@link #tick} resolves each player's rate by priority, runs the world at the slowest, adds that many game
 * deciseconds, posts the crossed clock events, writes vanilla's day time from the true sun, and syncs.
 *
 * <p>The clock asks two rates itself, because it can see them: Travel for a player moving at
 * {@code [clock] travel_from_m_per_s} or more, and Sleep for a player in vanilla's sleeping pose. Everything else
 * (close, work, until done, waiting) is asked by the system that knows.
 */
public final class ClockServiceImpl implements ClockService {
    private MinecraftServer server;
    private ClockData data;
    private Rate worldRate = Rate.WORLD;
    private Rate forced;
    private final Map<UUID, EnumSet<Rate>> requests = new HashMap<>();
    private final Map<UUID, Rate> lastPlayerRate = new HashMap<>();
    private final Map<UUID, Vec3> lastPos = new HashMap<>();
    private double moonOffset;
    private int ticks;
    /** Hundredths of a ds carried from tick to tick at a rate that is not a whole ds a tick (Day 1 on some days). */
    private int carryCenti;
    /** For tests: the minute events posted by the last step, newest last, at most 64. */
    final List<ClockEvent> recent = new ArrayList<>();

    // ---- lifecycle ----------------------------------------------------------------------------------------

    void start(MinecraftServer server) {
        this.server = server;
        ServerLevel overworld = server.overworld();
        int len = Rates.STANDARD.nearestAllowed(YoiConfig.SPEC.isLoaded() ? YoiConfig.DAY_LENGTH_MIN.get() : 24);
        data = overworld.getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                () -> new ClockData(0, len), ClockData::load, null), ClockData.ID);
        if (!rates().allowed(data.dayLengthMin)) {
            data.dayLengthMin = rates().nearestAllowed(data.dayLengthMin);
            data.setDirty();
        }
        moonOffset = Moon.offsetDays(StageHash.of(overworld.getSeed() + ":moon"));
        for (ServerLevel l : server.getAllLevels()) l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        requests.clear();
        lastPlayerRate.clear();
        lastPos.clear();
        worldRate = Rate.WORLD;
        forced = null;
        ticks = 0;
        carryCenti = 0;
        writeDayTime();
        YearOfIce.LOG.info("Clock: {} ({}-minute day)", Calendar.format(data.gameDs), data.dayLengthMin);
    }

    void stop() {
        server = null;
        data = null;
    }

    Rates rates() { return ClockResources.current.rates(); }

    // ---- reads --------------------------------------------------------------------------------------------

    @Override public long nowDs() { return data == null ? 0 : data.gameDs; }

    @Override public long nowMinute() { return Calendar.minute(nowDs()); }

    @Override public long absDay() { return Calendar.absDay(nowDs()); }

    @Override public int minuteOfDay() { return Calendar.minuteOfDay(nowDs()); }

    @Override public int dayOfYear() { return Calendar.dayOfYear(nowDs()); }

    @Override public int year() { return Calendar.year(nowDs()); }

    @Override public Rate rate() { return worldRate; }

    @Override public Rate rate(ServerPlayer player) { return lastPlayerRate.getOrDefault(player.getUUID(), Rate.WORLD); }

    @Override public int dsPerTick() { return dsPerTick(worldRate); }

    @Override public int dsPerTick(Rate rate) { return rates().dsPerTick(rate, dayLengthMin()); }

    @Override public int dayLengthMin() { return data == null ? 24 : data.dayLengthMin; }

    @Override public Sun sun() { return ClockResources.current.sun(); }

    @Override public double sunElevation() { return sun().elevation(absDay(), minuteOfDay()); }

    @Override public double daylightHours(long absDay) { return sun().daylightHours(absDay); }

    @Override public long dawnMinute(long absDay) { return absDay * Calendar.MINUTES_PER_DAY + sun().dawnMinute(absDay); }

    @Override public long duskMinute(long absDay) { return absDay * Calendar.MINUTES_PER_DAY + sun().duskMinute(absDay); }

    @Override public double moonPhase() { return Moon.phase(nowMinute(), moonOffset); }

    @Override
    public void requestRate(ServerPlayer player, Rate rate, String reason) {
        requests.computeIfAbsent(player.getUUID(), k -> EnumSet.noneOf(Rate.class)).add(rate);
    }

    // ---- admin --------------------------------------------------------------------------------------------

    @Override
    public void setDay(long day) {
        if (data == null) return;
        long from = data.gameDs;
        long to = (day - 1) * Calendar.DS_PER_DAY + Math.floorMod(from, Calendar.DS_PER_DAY);
        move(from, Math.max(0, to), true);
    }

    @Override
    public void setNow(long gameDs) {
        if (data == null) return;
        move(data.gameDs, Math.max(0, gameDs), true);
    }

    @Override
    public void advance(long minutes) {
        if (data == null || minutes <= 0) return;
        long from = data.gameDs;
        move(from, from + minutes * Calendar.DS_PER_MINUTE, false);
    }

    @Override
    public void setDayLength(int minutes) {
        if (!rates().allowed(minutes)) throw new IllegalArgumentException("day length " + minutes + " is not one of the allowed steps");
        if (data == null) return;
        data.dayLengthMin = minutes;
        data.setDirty();
        syncAll();
    }

    @Override
    public void forceRate(Rate rate) {
        forced = rate;
    }

    @Override public Rate forcedRate() { return forced; }

    // ---- the tick -----------------------------------------------------------------------------------------

    void tick() {
        if (data == null || server == null) return;
        autoRequests();
        Map<UUID, Rate> perPlayer = new HashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            perPlayer.put(p.getUUID(), RateArbiter.forPlayer(requests.getOrDefault(p.getUUID(), EnumSet.noneOf(Rate.class))));
        }
        // Players who asked but are not in the list (GameTest players) count too.
        for (Map.Entry<UUID, EnumSet<Rate>> e : requests.entrySet()) {
            perPlayer.putIfAbsent(e.getKey(), RateArbiter.forPlayer(e.getValue()));
        }
        requests.clear();
        lastPlayerRate.clear();
        lastPlayerRate.putAll(perPlayer);
        Rate next = forced != null ? forced : RateArbiter.forWorld(perPlayer.values(), rates(), dayLengthMin());
        boolean changed = next != worldRate;
        if (changed) {
            Rate was = worldRate;
            worldRate = next;
            carryCenti = 0;
            NeoForge.EVENT_BUS.post(new RateChangedEvent(was, next));
        }
        long from = data.gameDs;
        carryCenti += rates().centiDsPerTick(worldRate, dayLengthMin());
        int step = carryCenti / 100;
        carryCenti -= step * 100;
        move(from, from + step, false);
        ticks++;
        if (changed || ticks % 20 == 0) syncAll();
    }

    private void autoRequests() {
        double minStep = ClockConfig.travelFromMps() / 20.0;
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            seen.add(p.getUUID());
            if (p.isSpectator()) continue;
            Vec3 now = p.position();
            Vec3 was = lastPos.put(p.getUUID(), now);
            if (p.isSleeping()) {
                requestRate(p, Rate.SLEEP, "sleeping");
            } else if (was != null && minStep > 0) {
                double dx = now.x - was.x, dz = now.z - was.z;
                if (dx * dx + dz * dz >= minStep * minStep && dx * dx + dz * dz < 100) requestRate(p, Rate.TRAVEL, "moving");
            }
        }
        lastPos.keySet().retainAll(seen);
    }

    /** Moves the clock from {@code from} to {@code to}, posting the crossed events or one Jump. */
    private void move(long from, long to, boolean admin) {
        data.gameDs = to;
        data.setDirty();
        long m0 = Calendar.minute(from), m1 = Calendar.minute(to);
        long crossed = m1 - m0;
        recent.clear();
        if (admin && to != from || crossed > 60 || crossed < 0) {
            post(new ClockEvent.Jump(from, to));
        } else if (crossed > 0) {
            post(new ClockEvent.Minute(m0 + 1, (int) crossed));
            Sun sun = sun();
            for (long m = m0 + 1; m <= m1; m++) {
                if (Math.floorMod(m, 60L) == 0) post(new ClockEvent.Hour(m));
                long day = Calendar.absDayOfMinute(m);
                if (Math.floorMod(m, (long) Calendar.MINUTES_PER_DAY) == 0) post(new ClockEvent.Day(day));
                if (day * Calendar.MINUTES_PER_DAY + sun.dawnMinute(day) == m) post(new ClockEvent.Dawn(day, m));
                for (long d = day - 1; d <= day; d++) {
                    if (d * Calendar.MINUTES_PER_DAY + sun.duskMinute(d) == m) post(new ClockEvent.Dusk(d, m));
                }
            }
        }
        writeDayTime();
        if (admin) syncAll();
    }

    private void post(ClockEvent e) {
        if (recent.size() < 64) recent.add(e);
        NeoForge.EVENT_BUS.post(e);
    }

    /** dayTime follows the sun: the vanilla time whose sun stands at the true elevation, on the right half. */
    private void writeDayTime() {
        if (server == null) return;
        long day = absDay();
        int mod = minuteOfDay();
        double e = sun().elevation(day, mod);
        long t = VanillaTime.dayTime(e, sun().rising(day, mod));
        long dayTime = Math.max(0, day) * VanillaTime.TICKS_PER_DAY + t;
        boolean fast = dsPerTick() >= rates().workDs();
        for (ServerLevel l : server.getAllLevels()) {
            l.setDayTime(dayTime);
            if (fast) {
                server.getPlayerList().broadcastAll(new ClientboundSetTimePacket(l.getGameTime(), dayTime, false), l.dimension());
            }
        }
    }

    void syncAll() {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p);
    }

    void sync(ServerPlayer p) {
        if (data == null) return;
        YoiNet.send(p, new ClockSyncPayload(data.gameDs, dsPerTick(), (byte) worldRate.ordinal(),
                (byte) rate(p).ordinal(), (short) dayLengthMin()));
    }

    /** True once a server is running the clock. */
    boolean running() { return data != null; }
}
