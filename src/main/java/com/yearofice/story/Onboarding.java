package com.yearofice.story;

import com.yearofice.api.body.BodyReadout;
import com.yearofice.api.body.BodyService;
import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.fire.FireStateEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.fire.FireHeat;
import com.yearofice.sim.story.FirstNight;
import com.yearofice.sim.story.FirstNightFacts;
import com.yearofice.sim.story.FirstNightStep;
import com.yearofice.sim.story.OnboardingTuning;
import com.yearofice.sim.story.StoryDay;
import com.yearofice.story.net.OnboardingSyncPayload;
import com.yearofice.story.net.StoryNet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;

/**
 * {@code story.Onboarding}: the guided first night of the M2.5 slice (Blueprint › Start site › The guided first
 * night). The server runs one {@link FirstNight} per player, saved in {@code yearofice:onboarding}; the six prompts
 * go out through {@link StoryLines}, and the objective beacon's target goes to the owner in
 * {@code yearofice:onboarding_sync} for {@code client.story} to draw.
 *
 * <p>Every step closes on a real game event or a check, read only through {@code com.yearofice.api} and vanilla:
 * tinder and kindling in the pack; a fire ring placed ({@code BlockEvent.EntityPlaceEvent}) or found within reach;
 * {@code api.fire.FireStateEvent} to a flaming state at that ring, or a warm fire in {@link FireHeatLookup}; food
 * (any item with a {@code yearofice:food} row) in the pack; a meal booked by {@link BodyService} (its readout's
 * stomach or today's kcal rose); and sleep within the near-fire radius of a warm fire. A player who stays up instead
 * finishes at the first morning. Items and blocks are named by id, so story never imports another system's classes.
 *
 * <p>Start: the first join of a world, when {@code [onboarding] enabled} is on and the player has never played in
 * it. A player who has played before (play time over 0 with no record) is a returning player and never gets it.
 * Until lane A's outpost lands, the start site is the world spawn.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class Onboarding {
    private Onboarding() {}

    /** Warn-once id: dusk with no fire (Blueprint › Start site, Lines for things that go wrong). */
    public static final String DUSK_NO_FIRE = "dusk_no_fire";
    /** A warm fire this near keeps the dusk warning quiet: the heat index's own reach (8 m). */
    static final double FIRE_QUIET_M = FireHeat.ZERO_RADIUS_M;
    /** Server ticks between checks, and between beacon searches. */
    static final int CHECK_TICKS = 20, SCAN_TICKS = 200;

    static final ResourceLocation TINDER = id("tinder_bundle"), KINDLING = id("kindling"), FIRE_RING = id("fire_ring"),
            FOOD_MAP = id("food");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path); }

    /** What the server keeps per player in memory only: event flags, the last readout, the last beacon and sync. */
    static final class Live {
        final ServerPlayer player;
        boolean fireSeen;
        double lastStomach = Double.NaN;
        double lastKcal = Double.NaN;
        long scannedAt = Long.MIN_VALUE;
        FirstNightStep scannedFor;
        @Nullable BlockPos target;
        @Nullable OnboardingSyncPayload sent;

        Live(ServerPlayer p) { player = p; }
    }

    private static final Map<UUID, Live> LIVE = new ConcurrentHashMap<>();

    static Live live(ServerPlayer p) {
        Live l = LIVE.get(p.getUUID());
        if (l == null || l.player != p) {
            l = new Live(p);
            LIVE.put(p.getUUID(), l);
        }
        return l;
    }

    public static OnboardingRecord of(ServerPlayer p) { return StoryAttachments.of(p); }

    // ---- start, skip, reset ---------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) onJoin(p, StoryClock.minute(p.server));
    }

    /** The spawn hook: a new player's first join starts the night; anyone else's join only resyncs. */
    public static void onJoin(ServerPlayer p, long minute) {
        OnboardingRecord r = of(p);
        if (r.night.status() == FirstNight.Status.NEW) {
            if (!StoryConfig.enabled()) {
                r.night.skip(minute, FirstNight.Ending.OFF);
            } else if (p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > 0) {
                r.night.skip(minute, FirstNight.Ending.RETURNING);
            } else {
                start(p, minute);
                return;
            }
        }
        live(p).sent = null;
        sync(p, r, live(p), minute);
    }

    /** Starts (or restarts) the night now, at the spawn point as the start site, and says the wake lines. */
    public static void start(ServerPlayer p, long minute) {
        OnboardingRecord r = of(p);
        r.night.start(minute);
        r.ring = null;
        r.warn.clear();
        r.startSite = p.serverLevel().getSharedSpawnPos();
        Live l = live(p);
        l.fireSeen = false;
        l.lastStomach = l.lastKcal = Double.NaN;
        l.scannedFor = null;
        l.sent = null;
        OnboardingTuning t = StoryConfig.tuning();
        int doy = com.yearofice.sim.time.Calendar.dayOfYearOfDay(StoryDay.absDay(minute));
        String date = com.yearofice.sim.time.Calendar.monthName(com.yearofice.sim.time.Calendar.month(doy)) + " "
                + com.yearofice.sim.time.Calendar.dayOfMonth(doy);
        StoryLines.post(p, prompt("wake", StoryDay.day(minute), date, StoryDay.hhmm(minute)));
        StoryLines.post(p, prompt("dark", StoryDay.hhmm(StoryClock.sunsetOf(minute))));
        StoryLines.post(p, stepLine(FirstNightStep.TINDER, t));
        sync(p, r, l, minute);
    }

    /** {@code /yoi onboarding skip}: guidance off for good. */
    public static void skip(ServerPlayer p, long minute) {
        OnboardingRecord r = of(p);
        boolean was = r.night.active();
        r.night.skip(minute, FirstNight.Ending.SKIPPED);
        if (was) StoryLines.post(p, prompt("skipped"));
        sync(p, r, live(p), minute);
    }

    /** {@code /yoi onboarding reset}: forget the night and start it again from step 1. */
    public static void reset(ServerPlayer p, long minute) {
        of(p).night.reset();
        start(p, minute);
    }

    /** Drops the in-memory half (logout, a test's cleanup). */
    public static void forget(ServerPlayer p) {
        Live l = LIVE.get(p.getUUID());
        if (l != null && l.player == p) LIVE.remove(p.getUUID());
        StoryLines.forget(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) forget(p);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        LIVE.clear();
        StoryLines.clear();
    }

    // ---- the check ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        long gameTime = e.getServer().overworld().getGameTime();
        boolean check = gameTime % CHECK_TICKS == 0;
        long minute = check ? StoryClock.minute(e.getServer()) : 0;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (check && !p.isSpectator()) check(p, minute);
            StoryLines.drain(p, p.serverLevel().getGameTime());
        }
    }

    /**
     * One check of one player at game minute {@code minute}: gathers the facts, advances the night, posts the next
     * prompt or the end line, runs the dusk warning, refreshes the beacon and syncs. The tick calls it once a
     * second with the clock's minute; the GameTest calls it with its own scripted minute.
     */
    public static FirstNight.Advance check(ServerPlayer p, long minute) {
        OnboardingRecord r = of(p);
        Live l = live(p);
        if (!r.night.active()) {
            sync(p, r, l, minute);
            return FirstNight.Advance.NOTHING;
        }
        OnboardingTuning t = StoryConfig.tuning();
        FirstNightFacts f = facts(p, r, l, t);
        FirstNight.Advance a = r.night.advance(f, minute, StoryDay.firstMorning(r.night.startMinute(), StoryClock.DAWN));
        if (a.finished()) {
            StoryLines.post(p, prompt("done"));
            YearOfIce.LOG.info("[story] {} finished the guided first night ({}) at {}", p.getGameProfile().getName(),
                    r.night.ending().name().toLowerCase(java.util.Locale.ROOT), StoryDay.hhmm(minute));
        } else if (a.moved()) {
            StoryLines.post(p, stepLine(a.now(), t));
        }
        if (r.night.active()) duskWarning(p, r, minute, StoryDay.inDuskWindow(minute, t.duskLeadMin(), StoryClock.SUNSET, StoryClock.DAWN));
        sync(p, r, l, minute);
        return a;
    }

    /**
     * Warn once, {@code dusk_no_fire}: dusk (from {@code dusk_lead_min} before sunset until dawn) and no fire lit
     * tonight and none warm within 8 m. One line, then quiet until the condition has been false for 24 game hours.
     */
    static void duskWarning(ServerPlayer p, OnboardingRecord r, long minute, boolean dusk) {
        boolean noFire = r.night.fireLitMinute() < 0 && warmFireWithin(p, FIRE_QUIET_M, null) == null;
        if (r.warn.update(DUSK_NO_FIRE, dusk && noFire, minute)) {
            StoryLines.post(p, Component.translatable("messenger.yearofice.warn." + DUSK_NO_FIRE,
                    StoryDay.hhmm(StoryClock.sunsetOf(minute))));
        }
    }

    /** {@code ClockEvent.Dusk}: the sun is down; a player on the night with no fire hears it now if not before. */
    @SubscribeEvent
    public static void onDusk(ClockEvent.Dusk e) {
        for (Live l : LIVE.values()) {
            OnboardingRecord r = of(l.player);
            if (r.night.active() && l.player.server.getPlayerList().getPlayer(l.player.getUUID()) == l.player) {
                duskWarning(l.player, r, e.minute(), true);
            }
        }
    }

    /** {@code ClockEvent.Dawn}: the first morning may end the night of a player who stayed up; check at once. */
    @SubscribeEvent
    public static void onDawn(ClockEvent.Dawn e) {
        for (Live l : LIVE.values()) {
            if (l.player.server.getPlayerList().getPlayer(l.player.getUUID()) == l.player) check(l.player, e.minute());
        }
    }

    /** {@code ClockEvent.Hour}: look for the beacon's target again (the player has moved; a creek or birch is nearer). */
    @SubscribeEvent
    public static void onHour(ClockEvent.Hour e) {
        for (Live l : LIVE.values()) l.scannedFor = null;
    }

    /** The facts for one check (see the class comment for where each comes from). */
    static FirstNightFacts facts(ServerPlayer p, OnboardingRecord r, Live l, OnboardingTuning t) {
        Item tinder = BuiltInRegistries.ITEM.get(TINDER), kindling = BuiltInRegistries.ITEM.get(KINDLING);
        DataMapType<Item, ?> food = foodMap();
        boolean hasTinder = false, hasKindling = false, hasFood = false;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(tinder)) hasTinder = true;
            else if (s.is(kindling)) hasKindling = true;
            else if (food != null && s.getItemHolder().getData(food) != null) hasFood = true;
        }

        ServerLevel level = p.serverLevel();
        if (r.ring != null && !isRing(level, r.ring)) r.ring = null;
        if (r.ring == null) r.ring = nearestRing(p, t.reachM());
        boolean ring = r.ring != null;

        boolean fireLit = l.fireSeen;
        if (!fireLit) {
            FireHeat warm = r.ring != null ? warmFireAt(level, r.ring) : warmFireWithin(p, t.reachM(), null);
            fireLit = warm != null;
        }

        BodyReadout body = BodyService.get().readout(p);
        boolean ate = (!Double.isNaN(l.lastStomach) && body.stomachPct() > l.lastStomach + 0.005)
                || (!Double.isNaN(l.lastKcal) && body.kcalInToday() > l.lastKcal + 0.5);
        l.lastStomach = body.stomachPct();
        l.lastKcal = body.kcalInToday();

        boolean asleepByFire = p.isSleeping() && warmFireWithin(p, t.nearFireRadiusM(), null) != null;
        return new FirstNightFacts(hasTinder, hasKindling, ring, fireLit, hasFood, ate, asleepByFire);
    }

    private static volatile DataMapType<Item, ?> foodMap;

    /** {@code yearofice:food}, looked up by id (the body registers it). */
    static @Nullable DataMapType<Item, ?> foodMap() {
        DataMapType<Item, ?> m = foodMap;
        if (m == null) {
            m = RegistryManager.getDataMap(Registries.ITEM, FOOD_MAP);
            foodMap = m;
        }
        return m;
    }

    static boolean isRing(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return true;
        Block b = level.getBlockState(pos).getBlock();
        return BuiltInRegistries.BLOCK.getKey(b).equals(FIRE_RING);
    }

    /** The nearest fire ring within {@code reachM} of the player, or null. */
    static @Nullable BlockPos nearestRing(ServerPlayer p, double reachM) {
        Level level = p.level();
        Block ringBlock = BuiltInRegistries.BLOCK.get(FIRE_RING);
        int r = (int) Math.ceil(reachM);
        BlockPos c = p.blockPosition();
        BlockPos best = null;
        double bd = reachM * reachM;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 2, r))) {
            if (!level.getBlockState(q).is(ringBlock)) continue;
            double d = q.distToCenterSqr(p.getX(), p.getY(), p.getZ());
            if (d <= bd) {
                bd = d;
                best = q.immutable();
            }
        }
        return best;
    }

    static @Nullable FireHeat warmFireAt(Level level, BlockPos pos) {
        for (FireHeat h : FireHeatLookup.INSTANCE.fires(level)) {
            if (h.x() == pos.getX() && h.y() == pos.getY() && h.z() == pos.getZ()) return h;
        }
        return null;
    }

    /** The nearest warm fire within {@code radiusM} of the player (or of {@code from}), or null. */
    static @Nullable FireHeat warmFireWithin(ServerPlayer p, double radiusM, @Nullable BlockPos from) {
        double x = from == null ? p.getX() : from.getX() + 0.5;
        double y = from == null ? p.getY() : from.getY();
        double z = from == null ? p.getZ() : from.getZ() + 0.5;
        FireHeat best = null;
        double bd = radiusM;
        for (FireHeat h : FireHeatLookup.INSTANCE.fires(p.level())) {
            double d = h.distanceTo(x, y, z);
            if (d <= bd) {
                bd = d;
                best = h;
            }
        }
        return best;
    }

    // ---- events ---------------------------------------------------------------------------------------------

    /** A fire ring set by a player on step 1 or 2 becomes that player's ring. */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!BuiltInRegistries.BLOCK.getKey(e.getPlacedBlock().getBlock()).equals(FIRE_RING)) return;
        OnboardingRecord r = of(p);
        if (r.night.active() && r.night.step().ordinal() <= FirstNightStep.LIGHT.ordinal()) r.ring = e.getPos().immutable();
    }

    /**
     * {@code api.fire.FireStateEvent}: a fire that catches (tinder flame or more) at a player's ring, or within reach
     * of a player who has no ring yet, lights that player's step 3. Only players on the night are looked at.
     */
    @SubscribeEvent
    public static void onFireState(FireStateEvent e) {
        if (e.level().isClientSide() || !e.to().flaming() || e.from().flaming()) return;
        OnboardingTuning t = StoryConfig.tuning();
        BlockPos pos = e.pos();
        for (Live l : LIVE.values()) {
            ServerPlayer p = l.player;
            if (p.level() != e.level()) continue;
            OnboardingRecord r = of(p);
            if (!r.night.active()) continue;
            boolean mine = r.ring != null ? r.ring.equals(pos)
                    : pos.distToCenterSqr(p.getX(), p.getY(), p.getZ()) <= t.reachM() * t.reachM();
            if (mine) {
                l.fireSeen = true;
                if (r.ring == null) r.ring = pos;
            }
        }
    }

    // ---- lines and sync -------------------------------------------------------------------------------------

    static Component prompt(String id, Object... args) {
        return Component.translatable("messenger.yearofice.prompt.first_night_" + id, args);
    }

    static Component stepLine(FirstNightStep s, OnboardingTuning t) {
        return s == FirstNightStep.SLEEP ? prompt(s.id, (int) Math.round(t.nearFireRadiusM())) : prompt(s.id);
    }

    /** Refreshes the beacon target when the step changed or 10 s passed, and sends the sync if anything moved. */
    static void sync(ServerPlayer p, OnboardingRecord r, Live l, long minute) {
        OnboardingTuning t = StoryConfig.tuning();
        boolean active = r.night.active();
        if (active) {
            long now = p.serverLevel().getGameTime();
            FirstNightStep s = r.night.step();
            if (l.scannedFor != s || now - l.scannedAt >= SCAN_TICKS) {
                l.target = BeaconTargets.find(p, r, s, t.searchRadius());
                l.scannedFor = s;
                l.scannedAt = now;
            }
        } else {
            l.target = null;
        }
        BlockPos target = l.target;
        OnboardingSyncPayload payload = new OnboardingSyncPayload(r.night.status().ordinal(), r.night.step().ordinal(),
                StoryConfig.beacon(), target != null, target == null ? BlockPos.ZERO : target,
                (int) Math.round(t.nearFireRadiusM()), StoryClock.sunsetOf(minute));
        if (!payload.equals(l.sent)) {
            l.sent = payload;
            StoryNet.sendToOwner(p, payload);
        }
    }

    /** The beacon's current target for a player (status and tests), or null. */
    public static @Nullable BlockPos target(ServerPlayer p) {
        Live l = LIVE.get(p.getUUID());
        return l == null ? null : l.target;
    }
}
