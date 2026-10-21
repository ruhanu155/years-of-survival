package com.yearofice.body;

import com.yearofice.api.item.LoadService;
import com.yearofice.body.clock.BodyClock;
import com.yearofice.body.clock.ServiceBodyClock;
import com.yearofice.body.env.EnvSampler;
import com.yearofice.body.env.VanillaEnvSampler;
import com.yearofice.body.net.BodyNet;
import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.messenger.MessengerSlots;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.body.BodyModel;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.DayPlan;
import com.yearofice.sim.body.FeelsLike;
import com.yearofice.sim.body.Metabolism;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * {@code body.BodyTicker}: runs the body model once per crossed game minute for every player in survival or
 * adventure, through the clock (Minecraft development › Layer 7 and The body tick). More than 10 minutes crossed
 * in one tick (the night skip, {@code /yoi time forward}) step in closed form. Collapse goes to {@link SosFlow};
 * the owner's vitals go out as {@code body_sync}.
 *
 * <p>Time and environment come in through {@link BodyClock} and {@link EnvSampler} (Layer 1's {@code ClockService} and
 * Layer 2's {@code EnvService}); the load band's calorie factor comes from {@link LoadService#kcalFactor}.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class BodyTicker {
    private BodyTicker() {}

    public static volatile BodyClock clock = new ServiceBodyClock();
    public static final VanillaEnvSampler VANILLA_ENV = new VanillaEnvSampler();
    public static volatile EnvSampler env = VANILLA_ENV;
    static final ActivitySampler ACTIVITY = new ActivitySampler();

    /** A jump larger than this is a calendar edit, not elapsed time, and is not stepped. */
    public static final long MAX_STEP_MINUTES = 2 * 1440;

    private static long lastMinute = Long.MIN_VALUE;
    private static final Map<UUID, BodySyncPayload> LAST_SENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SENT_MINUTE = new ConcurrentHashMap<>();
    private static final Map<UUID, EnvSampler.Sample> LAST_SAMPLE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent e) {
        lastMinute = Long.MIN_VALUE;
        LAST_SENT.clear();
        LAST_SENT_MINUTE.clear();
        LAST_SENT_TICK.clear();
        PENDING.clear();
        LAST_SAMPLE.clear();
    }

    /** Re-anchors the ticker after a calendar jump so the jump is not stepped. */
    public static void resync(MinecraftServer server) { lastMinute = clock.minute(server); }

    @SubscribeEvent
    public static void onTickPre(ServerTickEvent.Pre e) {
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (stepped(p)) ACTIVITY.sample(p);
        }
    }

    @SubscribeEvent
    public static void onTickPost(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        flushPending(server);
        long now = clock.minute(server);
        if (lastMinute == Long.MIN_VALUE || now < lastMinute || now - lastMinute > MAX_STEP_MINUTES) {
            if (lastMinute != Long.MIN_VALUE && now != lastMinute) {
                YearOfIce.LOG.info("Body clock jumped {} minutes; bodies not stepped across the jump", now - lastMinute);
            }
            lastMinute = now;
            return;
        }
        if (now == lastMinute) return;
        long start = lastMinute;
        int minutes = (int) (now - lastMinute);
        lastMinute = now;
        BodyTuning t = BodyConfig.tuning();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!stepped(p)) continue;
            ActivitySampler.Result a = ACTIVITY.take(p);
            step(p, start, minutes, a.activity(), a.asleep(), a.moving(), t);
        }
    }

    /** Creative and spectator players are not stepped; nor is a player who is away on an SOS flight. */
    public static boolean stepped(ServerPlayer p) {
        return !p.isCreative() && !p.isSpectator() && BodyAttachments.of(p).sos == 0;
    }

    /** One player's step: sample the place, run the model, hand a collapse to SOS, sync. */
    public static BodyModel.Result step(ServerPlayer p, long start, int minutes, Activity activity, boolean asleep,
                                        boolean moving, BodyTuning t) {
        BodyModel.Inputs in = inputs(p, start, activity, asleep, moving);
        BodyState s = BodyAttachments.of(p);
        BodyModel.Result r = BodyModel.step(s, in, t, start, minutes);
        after(p, s, r, start + minutes, minutes);
        return r;
    }

    /** Fast-forward through a scripted day plan (debug and tests): one closed-form chunk per plan hour. */
    public static BodyModel.Result stepPlan(ServerPlayer p, long start, int minutes, DayPlan plan, BodyTuning t) {
        BodyModel.Inputs in = inputs(p, start, Activity.REST, false, false);
        BodyState s = BodyAttachments.of(p);
        BodyModel.Result r = plan.run(s, in, t, start, minutes);
        after(p, s, r, start + minutes, minutes);
        return r;
    }

    public static BodyModel.Inputs inputs(ServerPlayer p, long minute, Activity activity, boolean asleep, boolean moving) {
        EnvSampler.Sample smp = env.sample(p, minute);
        LAST_SAMPLE.put(p.getUUID(), smp);
        skinContext(BodyAttachments.of(p));
        // M7 lane F: a dressed player's body reads what is actually worn (null = the implied start kit, undressed mocks).
        BodyAttachments.of(p).worn = com.yearofice.body.clothing.StartOutfit.dressed(p)
                ? com.yearofice.api.body.ClothingService.get().worn(p) : null;
        // Sleep quality and the bed's insulation from the bed under the sleeper (bare ground 0.70, +0 °C).
        com.yearofice.api.body.SleepService.Spot bed = asleep ? com.yearofice.body.sleep.Sleep.spotFor(p) : null;
        return new BodyModel.Inputs(activity, asleep, bed == null ? 0 : bed.quality(), smp.env(), smp.heat(), smp.shelter(),
                smp.depth(), moving, loadKcal(p), bed == null ? 0 : bed.warmthC());
    }

    /** The load band's calorie factor for what {@code p} carries (1.0 under the first band). */
    public static double loadKcal(ServerPlayer p) {
        try {
            return LoadService.get().kcalFactor(p);
        } catch (IllegalStateException notInstalled) {
            return 1.0;
        }
    }

    /** The date and the sun for the skin rules (B13): the body model has no clock of its own. */
    /** Tests stand in for the clock: set after the real context is read, cleared when done. */
    public static volatile java.util.function.Consumer<BodyState> skinHook = null;

    private static void skinContext(BodyState s) {
        try {
            com.yearofice.api.clock.ClockService c = com.yearofice.api.clock.ClockService.get();
            s.ctxDayOfYear = c.dayOfYear();
            s.ctxSunDeg = c.sunElevation();
        } catch (IllegalStateException notInstalled) {
            s.ctxDayOfYear = -1;
            s.ctxSunDeg = Double.NaN;
        }
        java.util.function.Consumer<BodyState> hook = skinHook;
        if (hook != null) hook.accept(s);
    }

    /** Posts the lines the illness, rest, skin and hair rules raised: one messenger line per cause (every vital change explained). */
    public static void drainEvents(ServerPlayer p, BodyState s) {
        if (s.events.isEmpty()) return;
        java.util.List<String> lines = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(s.events));
        s.events.clear();
        for (String id : lines) {
            com.yearofice.api.hud.HudBus.get().post(p, com.yearofice.api.hud.HudLine.of(
                    com.yearofice.api.hud.HudLineType.BODY, id, "body"));
            if (id.equals("face_sunburn") || id.equals("face_windburn")) {
                p.level().playSound(null, p.blockPosition(), com.yearofice.sound.YoiSounds.SKIN_STING.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.0f);
            }
        }
    }

    private static void after(ServerPlayer p, BodyState s, BodyModel.Result r, long now, int minutes) {
        com.yearofice.body.injury.InjuryTick.step(s, now - minutes, minutes, BodyConfig.tuning());
        drainEvents(p, s);
        if (r.collapsed()) {
            SosFlow.trigger(p, switch (r.collapse()) {
                case BMI -> SosFlow.Cause.STARVATION;
                case WARMTH -> SosFlow.Cause.HYPOTHERMIA;
                case CONDITION -> SosFlow.Cause.CONDITION;
                case NONE -> SosFlow.Cause.MANUAL;
            });
            return;
        }
        VitalExplainer.check(p, s, now);
        sync(p, now, false);
    }

    /** Real ticks between two unforced {@code body_sync} packets to one player: at most one a second. */
    public static final int MIN_SYNC_TICKS = 20;

    private static final Map<UUID, Long> LAST_SENT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SENT = new ConcurrentHashMap<>();

    /** Tests drive the tick count a sync sees. */
    public static volatile java.util.function.LongSupplier tickSource = null;

    /** {@code body_sync} packets sent to {@code p} since the last {@link #resetSentCount}. */
    public static int sentCount(ServerPlayer p) { return SENT.getOrDefault(p.getUUID(), 0); }

    public static void resetSentCount(ServerPlayer p) {
        SENT.remove(p.getUUID());
        LAST_SENT.remove(p.getUUID());
        LAST_SENT_MINUTE.remove(p.getUUID());
        LAST_SENT_TICK.remove(p.getUUID());
        PENDING.remove(p.getUUID());
    }

    /**
     * Sends {@code body_sync} if a shown value moved over 1%, an hour passed, or {@code force}; never more than one
     * unforced packet per second to a player (a change inside the second waits for it and goes out then).
     */
    public static void sync(ServerPlayer p, long minute, boolean force) {
        BodySyncPayload payload = payload(p, minute);
        BodySyncPayload last = LAST_SENT.get(p.getUUID());
        Long lastMin = LAST_SENT_MINUTE.get(p.getUUID());
        if (!(force || payload.differs(last) || lastMin == null || minute - lastMin >= 60)) return;
        long tick = tickSource != null ? tickSource.getAsLong() : p.server.getTickCount();
        Long lastTick = LAST_SENT_TICK.get(p.getUUID());
        if (!force && lastTick != null && tick - lastTick < MIN_SYNC_TICKS) {
            PENDING.put(p.getUUID(), minute);
            return;
        }
        PENDING.remove(p.getUUID());
        LAST_SENT.put(p.getUUID(), payload);
        LAST_SENT_MINUTE.put(p.getUUID(), minute);
        LAST_SENT_TICK.put(p.getUUID(), tick);
        SENT.merge(p.getUUID(), 1, Integer::sum);
        BodyNet.sendToOwner(p, payload);
    }

    /** Sends what the one-second limit held back. */
    private static void flushPending(MinecraftServer server) {
        if (PENDING.isEmpty()) return;
        long tick = server.getTickCount();
        for (Map.Entry<UUID, Long> en : PENDING.entrySet()) {
            Long lt = LAST_SENT_TICK.get(en.getKey());
            if (lt != null && tick - lt < MIN_SYNC_TICKS) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            PENDING.remove(en.getKey());
            if (p != null) sync(p, en.getValue(), false);
        }
    }

    public static BodySyncPayload payload(ServerPlayer p, long minute) {
        BodyState s = BodyAttachments.of(p);
        BodyTuning t = BodyConfig.tuning();
        EnvSampler.Sample smp = LAST_SAMPLE.get(p.getUUID());
        if (smp == null) smp = env.sample(p, minute);
        FeelsLike f = s.lastFeel;
        float[] feel = f == null ? new float[BodySyncPayload.FEEL_TERMS] : new float[] {(float) f.airC(),
                (float) f.windChillC(), (float) f.wetPenaltyC(), (float) f.clothingC(), (float) f.shelterC(),
                (float) f.fireC(), (float) f.activityC(), (float) f.bodyC()};
        double feels = f == null ? smp.env().airC() : f.totalC();
        double bmi = s.bmi();
        return new BodySyncPayload(minute, (float) s.kg, (float) bmi, (float) s.kcalInToday(minute),
                (float) s.kcalOutToday(minute), (float) s.balance24h(), (float) s.energy, (float) s.lastEnergyCap,
                (float) s.warmth, (float) feels, feel, (float) s.wetness(), (float) s.water,
                (float) s.stomachPct(t.stomachKg()), (float) s.condition(),
                (float) Metabolism.strength(s.condition(), bmi), (float) s.macroMeter(Metabolism.CARBS),
                (float) s.macroMeter(Metabolism.FAT), (float) s.macroMeter(Metabolism.PROTEIN),
                (float) s.drunkMlToday(minute), s.gutIll(minute), s.sos, s.lastActivity.ordinal(),
                (float) smp.env().airC(), smp.env().precip().ordinal(), (float) smp.env().precipMmH(),
                (float) smp.env().windMs(), smp.shelter() != com.yearofice.sim.env.ShelterState.NONE,
                messengerBattery(p), parts(s), s.exposureMin);
    }

    static BodySyncPayload.Parts parts(BodyState s) {
        var r = s.parts;
        int[] hp = r.partHp(), frost = r.partFrost();
        for (int i = 0; i < hp.length; i++) {
            hp[i] = Math.max(0, Math.min(127, hp[i]));
            frost[i] = Math.max(0, Math.min(127, frost[i]));
        }
        boolean bleeding = r.problems().stream().anyMatch(pr -> pr.bleedPerMinute() > 0);
        int[] d = r.lostDigits();
        return new BodySyncPayload.Parts(hp, frost, d[0], d[1], r.crooked(), r.problems().size(), bleeding);
    }

    /**
     * The battery of the messenger the player can read: in a hand or the neck slot, as the HUD bus finds it
     * ({@link MessengerSlots}); −1 with none there. A messenger deep in the pack does not show vitals.
     */
    static int messengerBattery(ServerPlayer p) { return MessengerSlots.battery(p); }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p, clock.minute(p.server), true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        ACTIVITY.forget(e.getEntity().getUUID());
        LAST_SENT.remove(e.getEntity().getUUID());
        LAST_SENT_MINUTE.remove(e.getEntity().getUUID());
        LAST_SENT_TICK.remove(e.getEntity().getUUID());
        PENDING.remove(e.getEntity().getUUID());
        LAST_SAMPLE.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p, clock.minute(p.server), true);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p, clock.minute(p.server), true);
    }
}
