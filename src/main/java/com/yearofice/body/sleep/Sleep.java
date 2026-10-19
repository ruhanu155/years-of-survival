package com.yearofice.body.sleep;

import com.yearofice.api.body.BodyReadout;
import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.SleepService;
import com.yearofice.api.build.BedData;
import com.yearofice.api.clock.ClockService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.SleepRules;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * {@code body.sleep}: our sleep in place of vanilla's (Minecraft development › Vanilla needs replaced, "Beds and the
 * night skip" and "Beds set the spawn"). It implements {@link SleepService}:
 *
 * <ul>
 *   <li><b>Lying down.</b> A bed block (one with a {@code yearofice:bed} data-map row) calls {@link #lieDown}. It puts
 *       the player straight into vanilla's sleeping pose with {@code LivingEntity.startSleeping}, skipping
 *       {@code ServerPlayer.startSleepInBed} and with it vanilla's "only at night", "monsters nearby" and respawn
 *       point. The Blueprint's rule replaces them: energy below 7, or the clock between 21:00 and 06:00.</li>
 *   <li><b>Staying asleep.</b> {@code CanContinueSleepingEvent} keeps a sleeper on one of our beds asleep in the
 *       daytime (vanilla wakes everyone once its sun is up, which at 64°N in May is most of the night).</li>
 *   <li><b>No night skip.</b> On start every level's {@code playersSleepingPercentage} is set to 101, vanilla's own
 *       "never skip" value: with 100 one sleeping player would be woken by the skip after 5 seconds. The clock runs
 *       the Sleep rate for anyone in the pose instead, and {@code SleepFinishedTimeEvent} keeps the day time as a
 *       second guard. Vanilla beds cannot be slept in or set a spawn.</li>
 *   <li><b>Waking.</b> Every tick each sleeper is checked against {@link SleepRules#check} (cold, wet, thirst, an
 *       animal nearby, morning, rested); a cause wakes them through {@link BodyService#wake}, which names it. The
 *       sneak and jump keys (client) and the bed screen's button wake them through vanilla's stop-sleeping packet.</li>
 *   <li><b>The spot.</b> The body tick reads {@link #spot}: the bed's quality and insulation, or bare ground.</li>
 * </ul>
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class Sleep implements SleepService {
    private Sleep() {}

    static final Sleep INSTANCE = new Sleep();

    /** {@code playersSleepingPercentage} above 100: vanilla never skips the night or wakes everyone. */
    public static final int NO_SKIP_PERCENT = 101;
    /** Ticks between scans for animals near a sleeper. */
    static final int DANGER_SCAN_TICKS = 10;

    public static final TagKey<EntityType<?>> LARGE_ANIMALS = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "large_animals"));
    public static final TagKey<EntityType<?>> PREDATORS = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "predators"));

    /** One sleep: the minute it began (taken at its first check) and whether that was in the clock's night. */
    record Lie(long startMinute, boolean night, BlockPos bed) {}

    private static final Map<UUID, Lie> LIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> DANGER = new ConcurrentHashMap<>();

    public static void init() { SleepService.install(INSTANCE); }

    // ---- SleepService --------------------------------------------------------------------------------------

    @Override
    public Refusal lieDown(ServerPlayer p, BlockPos head, long minute) {
        if (p.isSleeping() || p.isPassenger() || !p.isAlive() || p.isSpectator()) return Refusal.BUSY;
        Level level = p.level();
        BlockState st = level.getBlockState(head);
        if (bedData(st) == null || !st.isBed(level, head, p)) return Refusal.NOT_A_BED;
        if (st.hasProperty(BlockStateProperties.OCCUPIED) && st.getValue(BlockStateProperties.OCCUPIED)) return Refusal.OCCUPIED;
        BlockPos above = head.above();
        if (level.getBlockState(above).isSuffocating(level, above)) return Refusal.OBSTRUCTED;
        int mod = (int) Math.floorMod(minute, 1440L);
        if (!SleepRules.mayLieDown(BodyService.get().energy(p), mod, SleepConfig.tuning())) return Refusal.NOT_TIRED;
        p.startSleeping(head);
        // Vanilla lays a sleeper 11/16 up, on its 9/16 bed; ours lie on their own, lower surface.
        double top = st.getShape(level, head).isEmpty() ? 0 : st.getShape(level, head).max(Direction.Axis.Y);
        p.setPos(p.getX(), head.getY() + top + 0.125, p.getZ());
        if (level instanceof ServerLevel sl) sl.updateSleepingPlayerList();
        LIES.remove(p.getUUID());
        DANGER.remove(p.getUUID());
        return Refusal.NONE;
    }

    @Override
    public Spot spot(ServerPlayer p) { return spotOf(p, SleepConfig.tuning()); }

    /** The body tick's read: the spot under {@code p} on the active tuning. */
    public static Spot spotFor(ServerPlayer p) { return spotOf(p, SleepConfig.tuning()); }

    static Spot spotOf(ServerPlayer p, SleepRules.Tuning t) {
        if (!p.isSleeping()) return new Spot(SleepRules.quality(Double.NaN, t), 0, null);
        BlockPos bed = p.getSleepingPos().orElse(null);
        BedData d = bed == null || !p.level().isLoaded(bed) ? null : bedData(p.level().getBlockState(bed));
        if (d == null) return new Spot(SleepRules.quality(Double.NaN, t), 0, null);
        return new Spot(SleepRules.quality(d.quality(), t), SleepRules.warmthC(d.sleepWarmthC()), bed);
    }

    @Override
    public @Nullable String check(ServerPlayer p, long minute) {
        if (!p.isSleeping()) {
            LIES.remove(p.getUUID());
            return null;
        }
        SleepRules.Tuning t = SleepConfig.tuning();
        int mod = (int) Math.floorMod(minute, 1440L);
        BlockPos bed = p.getSleepingPos().orElse(BlockPos.ZERO);
        Lie lie = LIES.get(p.getUUID());
        if (lie == null || !lie.bed().equals(bed) || minute < lie.startMinute()) {
            lie = new Lie(minute, SleepRules.isNight(mod, t), bed);
            LIES.put(p.getUUID(), lie);
        }
        boolean danger = DANGER.computeIfAbsent(p.getUUID(), k -> scanDanger(p, t));
        if (p.tickCount % DANGER_SCAN_TICKS == 0) {
            danger = scanDanger(p, t);
            DANGER.put(p.getUUID(), danger);
        }
        BodyReadout r = BodyService.get().readout(p);
        SleepRules.Facts f = new SleepRules.Facts(mod, lie.night(), (int) Math.min(Integer.MAX_VALUE, minute - lie.startMinute()),
                r.warmth(), r.wetness(), r.water(), r.energy(), r.energyCap(), danger);
        SleepRules.Wake w = SleepRules.check(f, t);
        if (w == SleepRules.Wake.NONE) return null;
        BodyService.get().wake(p, String.format(Locale.ROOT, "%02d:%02d, %s", mod / 60, mod % 60, w.said));
        LIES.remove(p.getUUID());
        DANGER.remove(p.getUUID());
        YearOfIce.LOG.debug("[sleep] {} woke at {}: {}", p.getGameProfile().getName(), mod, w.id());
        return w.id();
    }

    /** A large animal within {@code large_animal_m} or a predator within {@code predator_m} of the sleeper. */
    static boolean scanDanger(ServerPlayer p, SleepRules.Tuning t) {
        double r = Math.max(t.largeAnimalM(), t.predatorM());
        if (r <= 0) return false;
        AABB box = p.getBoundingBox().inflate(r);
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !(e instanceof Player))) {
            double d = e.distanceTo(p);
            if (e.getType().is(PREDATORS) && d <= t.predatorM()) return true;
            if (e.getType().is(LARGE_ANIMALS) && d <= t.largeAnimalM()) return true;
        }
        return false;
    }

    static @Nullable BedData bedData(BlockState st) { return st.getBlockHolder().getData(BedData.MAP); }

    /** True when {@code st} is one of our beds. */
    public static boolean isOurBed(BlockState st) { return bedData(st) != null; }

    /** The line a refusal shows on the action bar, or null. */
    public static @Nullable Component refusalLine(Refusal r) {
        return r == Refusal.NONE ? null : Component.translatable("body.yearofice.sleep." + r.name().toLowerCase(Locale.ROOT));
    }

    // ---- events ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (!ClockService.installed()) return;
        long minute = ClockService.get().nowMinute();
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (p.isSleeping()) INSTANCE.check(p, minute);
            else LIES.remove(p.getUUID());
        }
    }

    /** Our beds keep their sleepers at any hour; vanilla's daytime check would wake them at its sunrise. */
    @SubscribeEvent
    public static void onContinue(CanContinueSleepingEvent e) {
        if (e.getProblem() != Player.BedSleepingProblem.NOT_POSSIBLE_NOW) return;
        BlockPos pos = e.getEntity().getSleepingPos().orElse(null);
        if (pos != null && e.getEntity().level().isLoaded(pos) && isOurBed(e.getEntity().level().getBlockState(pos))) {
            e.setContinueSleeping(true);
        }
    }

    /** No night skip: if vanilla ever finishes a sleep, the day time stays where the clock put it. */
    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent e) {
        e.setTimeAddition(e.getLevel().getLevelData().getDayTime());
    }

    /** Vanilla beds are not beds here: no sleep, no spawn point, no explosion in other dimensions. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().getBlockState(e.getPos()).getBlock() instanceof BedBlock) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /** Beds never set the spawn (the return point is always the outpost). */
    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent e) {
        BlockPos at = e.getNewSpawn();
        if (at == null || e.isForced()) return;
        Level level = e.getEntity().level();
        if (!level.isLoaded(at)) return;
        BlockState st = level.getBlockState(at);
        if (st.getBlock() instanceof BedBlock || isOurBed(st)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        for (ServerLevel l : e.getServer().getAllLevels()) {
            l.getGameRules().getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE).set(NO_SKIP_PERCENT, e.getServer());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        LIES.remove(e.getEntity().getUUID());
        DANGER.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        LIES.clear();
        DANGER.clear();
    }

    /** Drops a test bot's sleep memory. */
    public static void forget(ServerPlayer p) {
        LIES.remove(p.getUUID());
        DANGER.remove(p.getUUID());
    }
}
