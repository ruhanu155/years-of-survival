package com.yearofice.body;

import com.yearofice.body.clock.BodyClock;
import com.yearofice.body.clock.VanillaDayClock;
import com.yearofice.body.env.EnvSampler;
import com.yearofice.body.env.VanillaEnvSampler;
import com.yearofice.body.net.BodyNet;
import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.item.messenger.TrailMessengerItem;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
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
 * <p>Time and environment come in through {@link BodyClock} and {@link EnvSampler}, so the Clock and Environment
 * layers replace the stand-ins without touching this class.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class BodyTicker {
    private BodyTicker() {}

    public static volatile BodyClock clock = new VanillaDayClock();
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
        after(p, s, r, start + minutes);
        return r;
    }

    /** Fast-forward through a scripted day plan (debug and tests): one closed-form chunk per plan hour. */
    public static BodyModel.Result stepPlan(ServerPlayer p, long start, int minutes, DayPlan plan, BodyTuning t) {
        BodyModel.Inputs in = inputs(p, start, Activity.REST, false, false);
        BodyState s = BodyAttachments.of(p);
        BodyModel.Result r = plan.run(s, in, t, start, minutes);
        after(p, s, r, start + minutes);
        return r;
    }

    public static BodyModel.Inputs inputs(ServerPlayer p, long minute, Activity activity, boolean asleep, boolean moving) {
        EnvSampler.Sample smp = env.sample(p, minute);
        LAST_SAMPLE.put(p.getUUID(), smp);
        double quality = asleep ? (p.isSleeping() ? 1.0 : 0.7) : 0;
        return new BodyModel.Inputs(activity, asleep, quality, smp.env(), smp.heat(), smp.shelter(), smp.depth(),
                moving);
    }

    private static void after(ServerPlayer p, BodyState s, BodyModel.Result r, long now) {
        if (r.collapsed()) {
            SosFlow.trigger(p, switch (r.collapse()) {
                case BMI -> SosFlow.Cause.STARVATION;
                case WARMTH -> SosFlow.Cause.HYPOTHERMIA;
                case CONDITION -> SosFlow.Cause.CONDITION;
                case NONE -> SosFlow.Cause.MANUAL;
            });
            return;
        }
        sync(p, now, false);
    }

    /** Sends {@code body_sync} if a shown value moved over 1%, an hour passed, or {@code force}. */
    public static void sync(ServerPlayer p, long minute, boolean force) {
        BodySyncPayload payload = payload(p, minute);
        BodySyncPayload last = LAST_SENT.get(p.getUUID());
        Long lastMin = LAST_SENT_MINUTE.get(p.getUUID());
        if (force || payload.differs(last) || lastMin == null || minute - lastMin >= 60) {
            LAST_SENT.put(p.getUUID(), payload);
            LAST_SENT_MINUTE.put(p.getUUID(), minute);
            BodyNet.sendToOwner(p, payload);
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
                messengerBattery(p));
    }

    /** The best battery among carried messengers, or −1 with none carried. */
    static int messengerBattery(ServerPlayer p) {
        Inventory inv = p.getInventory();
        int best = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(MessengerItems.MESSENGER.get())) best = Math.max(best, TrailMessengerItem.battery(st));
        }
        return best;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p, clock.minute(p.server), true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        ACTIVITY.forget(e.getEntity().getUUID());
        LAST_SENT.remove(e.getEntity().getUUID());
        LAST_SENT_MINUTE.remove(e.getEntity().getUUID());
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
