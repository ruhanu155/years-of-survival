package com.yearofice.hunt.fishing;

import com.yearofice.api.decay.DecayService;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.api.body.BodyService;
import com.yearofice.hunt.fishing.net.FishInputPayload;
import com.yearofice.hunt.fishing.net.FishResultPayload;
import com.yearofice.hunt.fishing.net.FishStatePayload;
import com.yearofice.sim.fishing.BiteModel;
import com.yearofice.sim.fishing.Draws;
import com.yearofice.sim.fishing.FightModel;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishSpecies;
import com.yearofice.sim.fishing.FishStock;
import com.yearofice.sim.fishing.FishingTuning;
import com.yearofice.sim.fishing.HookBait;
import com.yearofice.sim.fishing.SizeDraw;
import com.yearofice.sim.fishing.SliceBite;
import com.yearofice.sim.fishing.StrikeJudge;
import com.yearofice.sim.fishing.Tackle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import com.yearofice.api.env.EnvService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The server's rod fishing (Blueprint › Fishing in depth; Food systems › Fishing minigame, five phases):
 *
 * <ol>
 *   <li><b>Cast.</b> Right mouse held 0–1.5 s and released; the bobber flies to where the arc ends, ±10% in range
 *       and ±5°. With no bait on the hook a worm is taken from the pack and fitted first.</li>
 *   <li><b>Wait.</b> While the bobber floats, a bite rolls each game minute ({@link SliceBite}, the Blueprint's
 *       P_bite at the slice spot); a worm lasts 2 bites or 30 minutes. False nibbles shake the bobber.</li>
 *   <li><b>Strike.</b> The rod tip twitches, then the bobber dips; a left click inside the steel hook's 0.6 s
 *       window (plus the round-trip grace) sets the hook ({@link StrikeJudge}). An unanswered dip loses the worm.</li>
 *   <li><b>Fight.</b> The lockstep {@link FightModel} ({@link FightSession}) on the close clock.</li>
 *   <li><b>Land.</b> Right mouse grabs a tired fish within reach; the fish goes into the pack as
 *       {@code yearofice:arctic_grayling} with its weight in {@code yearofice:weight_g}.</li>
 * </ol>
 * Line breaks, spat hooks and bite-throughs come from the fight; each fight wears the line (the rod's durability).
 */
public final class FishingService {
    private FishingService() {}

    /** The slice rod's hook: steel, 5% lost on the strike, window 0–0.6 s after the dip (Tackle › Hooks). */
    public static final StrikeJudge.Hook HOOK = StrikeJudge.Hook.steel();

    private static final Map<UUID, BobberEntity> BOBBERS = new HashMap<>();
    private static final AtomicInteger NEXT_FIGHT = new AtomicInteger();

    static void clear() {
        BOBBERS.clear();
        FishingClock.clear();
    }

    // ---- lookups ------------------------------------------------------------------------------------------------

    public static @Nullable BobberEntity bobberOf(ServerPlayer p) {
        BobberEntity b = BOBBERS.get(p.getUUID());
        return b != null && !b.isRemoved() ? b : null;
    }

    public static boolean inFight(ServerPlayer p) {
        BobberEntity b = bobberOf(p);
        return b != null && b.fight != null && !b.fight.over();
    }

    /** The players in a fight now (their bobbers' owners), for the close-clock requests. */
    static java.util.List<ServerPlayer> fighters() {
        java.util.List<ServerPlayer> out = new java.util.ArrayList<>();
        for (BobberEntity b : BOBBERS.values()) {
            if (!b.isRemoved() && b.fight != null && !b.fight.over() && b.owner != null && !b.owner.isRemoved()) out.add(b.owner);
        }
        return out;
    }

    public static int activeFights() {
        int n = 0;
        for (BobberEntity b : BOBBERS.values()) if (!b.isRemoved() && b.fight != null && !b.fight.over()) n++;
        return n;
    }

    public static ItemStack heldRod(Player p) {
        if (p.getMainHandItem().is(FishingRegistry.ROD.get())) return p.getMainHandItem();
        if (p.getOffhandItem().is(FishingRegistry.ROD.get())) return p.getOffhandItem();
        return ItemStack.EMPTY;
    }

    static boolean ownerStillFishing(BobberEntity b) {
        ServerPlayer o = b.owner;
        return o != null && !o.isRemoved() && o.isAlive() && o.level() == b.level() && !heldRod(o).isEmpty()
                && o.distanceToSqr(b) < 48 * 48 && BOBBERS.get(o.getUUID()) == b;
    }

    /** Sends only over a connection that has the channel (a GameTest's loopback player has none). */
    static void send(ServerPlayer p, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (p.connection != null && p.connection.hasChannel(payload)) PacketDistributor.sendToPlayer(p, payload);
    }

    static void say(@Nullable Player p, String line) {
        if (p instanceof ServerPlayer sp && line != null) com.yearofice.api.hud.HudBus.get().offerPrompt(sp, "fishing", line, 2);
    }

    // ---- cast ---------------------------------------------------------------------------------------------------

    /** Fits a worm from the off hand or the pack onto a bare hook. */
    static boolean fitWorm(ServerPlayer p, ItemStack rod) {
        FishingTuning t = FishingConfig.tuning();
        ItemStack worm = p.getOffhandItem().is(FishingRegistry.WORM.get()) ? p.getOffhandItem() : ItemStack.EMPTY;
        if (worm.isEmpty()) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.is(FishingRegistry.WORM.get())) {
                    worm = s;
                    break;
                }
            }
        }
        if (worm.isEmpty()) return false;
        if (!p.getAbilities().instabuild) worm.shrink(1);
        RodItem.setBait(rod, HookBait.fitted("worm", t.wormBites(), t.wormMinutes()));
        say(p, "Worm on the hook.");
        return true;
    }

    /** Right mouse released after {@code heldS} seconds. */
    public static void cast(ServerPlayer p, ItemStack rod, double heldS) {
        if (bobberOf(p) != null || rod.isEmpty()) return;
        if (!RodItem.bait(rod).present() && !fitWorm(p, rod)) {
            say(p, "No bait on the hook. Worms live in the soil by the water.");
            return;
        }
        FishingTuning t = FishingConfig.tuning();
        ServerLevel level = p.serverLevel();
        long seed = level.getSeed() ^ Draws.name("cast");
        double u1 = Draws.u(seed, p.getId(), level.getGameTime(), 1), u2 = Draws.u(seed, p.getId(), level.getGameTime(), 2);
        double charge = Mth.clamp(heldS / t.castChargeS(), 0.1, 1.0);
        double d = Math.max(1.5, t.castRangeM() * charge * (1 + (u1 * 2 - 1) * t.castScatterPct() / 100.0));
        float yaw = (float) (p.getYRot() + (u2 * 2 - 1) * t.castScatterDeg());
        Vec3 eye = p.getEyePosition();
        double rad = yaw * Mth.DEG_TO_RAD;
        double tx = eye.x - Math.sin(rad) * d, tz = eye.z + Math.cos(rad) * d;
        // Aim just under the surface, so the arc's last step crosses into the water.
        Vec3 target = new Vec3(tx, landingY(level, tx, eye.y, tz) - 0.2, tz);
        BobberEntity b = new BobberEntity(FishingRegistry.BOBBER.get(), level);
        b.bind(p);
        b.launch(eye.add(p.getLookAngle().scale(0.4)), target, (int) Math.round(6 + d * 0.9), 0.6 + d * 0.12);
        level.addFreshEntity(b);
        BOBBERS.put(p.getUUID(), b);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5f, 0.6f);
    }

    /** The top of the water or ground under (x, z), searched down from the eye (so a canopy overhead is not the target). */
    static double landingY(ServerLevel level, double x, double eyeY, double z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(eyeY), Mth.floor(z));
        for (int i = 0; i < 24; i++, p.move(0, -1, 0)) {
            if (level.getFluidState(p).is(FluidTags.WATER)) return p.getY() + level.getFluidState(p).getHeight(level, p);
            BlockState s = level.getBlockState(p);
            if (!s.getCollisionShape(level, p).isEmpty()) return p.getY() + s.getCollisionShape(level, p).max(net.minecraft.core.Direction.Axis.Y);
        }
        return eyeY - 2;
    }

    /** Debug and tests: a floating bobber for {@code p} on the water block {@code water}, as if just cast there. */
    public static BobberEntity placeBobber(ServerPlayer p, BlockPos water) {
        BobberEntity old = bobberOf(p);
        if (old != null) old.discard();
        ServerLevel level = p.serverLevel();
        BobberEntity b = new BobberEntity(FishingRegistry.BOBBER.get(), level);
        b.bind(p);
        double y = water.getY() + level.getFluidState(water).getHeight(level, water);
        b.setPos(water.getX() + 0.5, y, water.getZ() + 0.5);
        level.addFreshEntity(b);
        BOBBERS.put(p.getUUID(), b);
        landedInWater(b, water);
        return b;
    }

    static void landedInWater(BobberEntity b, BlockPos at) {
        b.setState(BobberEntity.State.FLOATING);
        b.castM = b.owner == null ? 1 : Math.max(0.5, Math.sqrt(sq(b.getX() - b.owner.getX()) + sq(b.getZ() - b.owner.getZ())));
        b.lastMinute = FishingClock.minute(b.level().getServer());
        b.minutesInWater = 0;
        b.level().playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.2f, 1.4f);
    }

    static void landedOnGround(BobberEntity b) { say(b.owner, "Landed short of the water. Reel in and cast again."); }

    private static double sq(double v) { return v * v; }

    // ---- wait ---------------------------------------------------------------------------------------------------

    /** What the bite roll reads at the bobber in game minute {@code minute}. */
    public static SliceBite.Inputs inputs(BobberEntity b, long minute) {
        ServerLevel level = (ServerLevel) b.level();
        FishingTuning t = FishingConfig.tuning();
        SliceSpots.Spot spot = SliceSpots.at(level, b.waterBlock(), t);
        long day = FishingClock.day(minute);
        SliceSpots.Stocks stocks = SliceSpots.stocks(level);
        FishStock st = stocks.get(spot.key(), day, t);
        return new SliceBite.Inputs(FishingSpecies.grayling(), FishingSpecies.season(FishingSpecies.GRAYLING),
                SliceSpots.SPOT_TYPE, spot.atSpot(), t.spotQ(), spot.depthM(), t.spotRadiusM(), st.n(), st.k(), b.bait(),
                FishSeason.dayOfYear(day), FishingClock.band(minute), weather(level, b.blockPosition()),
                waterC(level, minute), t.turbidity(), stocks.spooked(spot.key(), minute));
    }

    /**
     * The bite roll's weather from the Environment field: the day's state, with Wet and Severe split by what falls
     * at the bobber (snow, or rain and the rest); a Wet or Severe day between its showers reads Cloudy.
     */
    static BiteModel.Weather weather(ServerLevel level, BlockPos at) {
        com.yearofice.sim.weather.WeatherModel.Hour h = EnvService.get().valley(level);
        com.yearofice.sim.env.Precip p = EnvService.get().precip(level, at);
        boolean snow = p == com.yearofice.sim.env.Precip.SNOW;
        return switch (h.state()) {
            case CLEAR -> BiteModel.Weather.CLEAR;
            case CLOUDY -> BiteModel.Weather.CLOUDY;
            case WET -> p == com.yearofice.sim.env.Precip.NONE ? BiteModel.Weather.CLOUDY : snow ? BiteModel.Weather.SNOW : BiteModel.Weather.RAIN;
            case SEVERE -> p == com.yearofice.sim.env.Precip.NONE ? BiteModel.Weather.CLOUDY : snow ? BiteModel.Weather.BLIZZARD : BiteModel.Weather.STORM;
        };
    }

    /** Water at the slice spot, a creek mouth: the Environment field's creek value (Blueprint › Conditions). */
    public static double waterC(ServerLevel level, long minute) {
        return EnvService.get().waterCAt(level, EnvService.WaterKind.CREEK, minute);
    }

    static long biteSeed(ServerLevel level) { return level.getSeed() ^ Draws.name("bite"); }

    static void floatTick(BobberEntity b) {
        ServerLevel level = (ServerLevel) b.level();
        FishingTuning t = FishingConfig.tuning();
        long now = FishingClock.minute(level.getServer());
        if (b.holdRolls || b.lastMinute == Long.MIN_VALUE || now - b.lastMinute > 10 || now < b.lastMinute) b.lastMinute = now;
        while (b.lastMinute < now && !b.isRemoved() && b.state() == BobberEntity.State.FLOATING) minuteStep(b, ++b.lastMinute);
        switch (b.cue()) {
            case TWITCH -> {
                if (--b.cueTicks <= 0) {
                    b.setCue(StrikeJudge.Cue.DIP, 0);
                    b.dipTick = level.getGameTime();
                    level.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.5f, 1.1f);
                }
            }
            case DIP -> {
                if (level.getGameTime() - b.dipTick > Math.round(StrikeJudge.dipOpenS(HOOK, t.strikeGraceS()) * 20)) {
                    b.setCue(StrikeJudge.Cue.NONE, 0);
                    setBait(b, HookBait.NONE);
                    say(b.owner, "The fish took the worm.");
                }
            }
            case NIBBLE -> {
                if (--b.cueTicks <= 0) b.setCue(StrikeJudge.Cue.NONE, 0);
            }
            case NONE -> { }
        }
    }

    /** One game minute in the water: the bite roll (or a nibble), then the worm's clock. */
    static void minuteStep(BobberEntity b, long minute) {
        ServerLevel level = (ServerLevel) b.level();
        FishingTuning t = FishingConfig.tuning();
        b.minutesInWater++;
        HookBait bait = b.bait();
        if (!bait.present()) return;
        if (b.cue() == StrikeJudge.Cue.NONE) {
            SliceBite.Inputs in = inputs(b, minute);
            long key = SliceSpots.key(b.waterBlock());
            long seed = biteSeed(level);
            if (SliceBite.bites(in, seed, key, minute)) {
                double lead = t.leadMinS() + (t.leadMaxS() - t.leadMinS()) * Draws.u(seed, key, minute, SliceBite.SALT_LEAD);
                b.setCue(StrikeJudge.Cue.TWITCH, (int) Math.max(1, Math.round(lead * 20)));
            } else if (SliceBite.nibbles(in, t.nibbleShare(), seed, key, minute)) {
                b.setCue(StrikeJudge.Cue.NIBBLE, (int) Math.max(1, Math.round(t.nibbleS() * 20)));
            }
        }
        HookBait after = bait.afterMinutes(1);
        setBait(b, after);
        if (!after.present() && b.cue() == StrikeJudge.Cue.NONE) say(b.owner, "The worm has washed off the hook.");
    }

    /** Debug: a real bite starts now (the twitch, then the dip). */
    public static void forceBite(BobberEntity b) {
        FishingTuning t = FishingConfig.tuning();
        b.setCue(StrikeJudge.Cue.TWITCH, (int) Math.max(1, Math.round(t.leadMinS() * 20)));
    }

    static void setBait(BobberEntity b, HookBait bait) {
        if (b.owner == null) return;
        ItemStack rod = heldRod(b.owner);
        if (!rod.isEmpty()) RodItem.setBait(rod, bait);
    }

    // ---- strike -------------------------------------------------------------------------------------------------

    public static void onInput(ServerPlayer p, FishInputPayload in) {
        BobberEntity b = bobberOf(p);
        if (b == null) return;
        if (in.fightId() == FishInputPayload.STRIKE) {
            strike(p);
            return;
        }
        if (b.fight != null && b.fight.fightId == in.fightId()) b.fight.accept(in.firstTick(), in.inputs(), p.level().getGameTime());
    }

    /** Left mouse on the bobber before a fight. Returns the verdict, or null when there is nothing to strike. */
    public static @Nullable StrikeJudge.Verdict strike(ServerPlayer p) {
        BobberEntity b = bobberOf(p);
        if (b == null || b.state() != BobberEntity.State.FLOATING || b.fight != null) return null;
        ServerLevel level = p.serverLevel();
        FishingTuning t = FishingConfig.tuning();
        long seed = biteSeed(level);
        long gt = level.getGameTime();
        StrikeJudge.Cue cue = b.cue();
        double since = (gt - b.dipTick) / 20.0;
        StrikeJudge.Verdict v = StrikeJudge.judge(cue, since, HOOK, t.strikeGraceS(), t.missBaitLossPct(),
                Draws.u(seed, p.getId(), gt, SliceBite.SALT_HOOK), Draws.u(seed, p.getId(), gt, SliceBite.SALT_BAIT));
        HookBait bait = b.bait();
        if (cue == StrikeJudge.Cue.DIP) bait = bait.afterBite();
        if (v.baitGone()) bait = HookBait.NONE;
        if (v.result() != StrikeJudge.Result.TWITCHED) b.setCue(StrikeJudge.Cue.NONE, 0);
        switch (v.result()) {
            case TWITCHED -> { }
            case SPOOKED -> {
                long minute = FishingClock.minute(level.getServer());
                SliceSpots.stocks(level).spook(SliceSpots.key(b.waterBlock()), minute + Math.round(t.spookMin()));
                say(p, v.baitGone() ? "Struck at a nibble. They're spooked, and the worm is gone." : "Struck at a nibble. They're spooked.");
            }
            case EARLY, LATE -> say(p, v.baitGone() ? "Missed. The worm is gone." : "Missed.");
            case HOOK_LOST -> say(p, v.baitGone() ? "The hook didn't set. The worm is gone." : "The hook didn't set.");
            case HOOKED -> { }
        }
        setBait(b, bait);
        if (v.hooked()) startFight(b);
        return v;
    }

    // ---- fight --------------------------------------------------------------------------------------------------

    static void startFight(BobberEntity b) { startFight(b, Double.NaN); }

    /** Hooks the fish; tests may fix its weight. */
    static void startFight(BobberEntity b, double kgOverride) {
        ServerPlayer owner = b.owner;
        if (owner == null) return;
        ServerLevel level = (ServerLevel) b.level();
        FishingTuning t = FishingConfig.tuning();
        long minute = FishingClock.minute(level.getServer());
        SliceSpots.Spot spot = SliceSpots.at(level, b.waterBlock(), t);
        FishStock st = SliceSpots.stocks(level).get(spot.key(), FishingClock.day(minute), t);
        FishSpecies sp = FishingSpecies.grayling();
        long seed = level.getSeed() ^ Draws.name("fight");
        int id = NEXT_FIGHT.incrementAndGet();
        double kg = SizeDraw.kg(sp, t.baitDepthM(), st.fraction(), t.spotQ(), Draws.u(seed, id, 0, SliceBite.SALT_SIZE),
                Draws.u(seed, id, 0, SliceBite.SALT_TROPHY), Draws.u(seed, id, 0, SliceBite.SALT_BAND));
        if (!Double.isNaN(kgOverride)) kg = kgOverride;
        boolean wet = b.minutesInWater >= t.lineWetMin() || EnvService.get().precip(level, b.blockPosition()).liquid();
        Tackle tackle = Tackle.plantFiberWillow().withWet(wet).withCondition(RodItem.lineCondition(heldRod(owner)));
        double castM = Mth.clamp(b.castM, 0.5, tackle.lineCarriedM());
        FightModel.Params params = new FightModel.Params(sp, kg, tackle, seed, id, castM, weak(owner), false,
                t.assistOn(), waterC(level, minute), false, false, false);
        b.fight = new FightSession(id, params, level.getGameTime(), t.inputTimeoutTicks());
        b.setState(BobberEntity.State.HOOKED);
        b.setCue(StrikeJudge.Cue.NONE, 0);
        send(owner, new FishStatePayload(id, params, b.fight.state()));
    }

    /** Under 3 energy bolts the player retrieves at ×0.7 (Fishing methods › Stamina). */
    static boolean weak(ServerPlayer p) {
        try {
            return BodyService.get().energy(p) < 3;
        } catch (RuntimeException e) {
            return false;
        }
    }

    static void fightTick(BobberEntity b) {
        FightSession f = b.fight;
        if (f == null) {
            b.setState(BobberEntity.State.FLOATING);
            return;
        }
        f.serverTick(b.level().getGameTime());
        follow(b, f.state().lineOutM());
        if (f.sendDue() && b.owner != null) send(b.owner, new FishStatePayload(f.fightId, null, f.state()));
        if (f.over()) finish(b);
    }

    /** Keeps the bobber on the line: lineOut metres from the angler, along the cast. */
    private static void follow(BobberEntity b, double lineOutM) {
        ServerPlayer o = b.owner;
        if (o == null) return;
        double dx = b.getX() - o.getX(), dz = b.getZ() - o.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h < 1e-3) return;
        double r = Math.max(0.5, lineOutM) / h;
        double x = o.getX() + dx * r, z = o.getZ() + dz * r;
        double y = b.getY();
        BlockPos at = BlockPos.containing(x, y - 0.3, z);
        if (b.level().getFluidState(at).is(FluidTags.WATER)) {
            b.setPos(x, b.getY(), z);
        }
    }

    static void finish(BobberEntity b) {
        FightSession f = b.fight;
        ServerPlayer o = b.owner;
        if (f == null) return;
        b.fight = null;
        FishingTuning t = FishingConfig.tuning();
        FightModel.State s = f.state();
        FightModel.Outcome out = s.outcome();
        ItemStack rod = o == null ? ItemStack.EMPTY : heldRod(o);
        int grams = 0;
        if (!rod.isEmpty() && out != FightModel.Outcome.CANCELLED) RodItem.wearLine(rod, FightModel.lineWearPct(t.lineWearPct(), s));
        String line;
        switch (out) {
            case LANDED -> {
                grams = (int) Math.round(f.params.kg() * 1000);
                if (o != null) {
                    ItemStack fish = landedFish(grams);
                    // A whole, uncleaned fish: raw life 3.6 days at 5 °C (k 0.6), from its food row.
                    DecayService.get().stamp(o.level(), fish, FoodState.RAW);
                    if (!o.getInventory().add(fish)) o.drop(fish, false);
                    long minute = FishingClock.minute(o.server);
                    SliceSpots.stocks((ServerLevel) b.level()).harvest(SliceSpots.key(b.waterBlock()), FishingClock.day(minute), t, 1);
                }
                line = String.format(Locale.ROOT, "Landed a grayling, %.2f kg.", grams / 1000.0);
            }
            case SNAP -> {
                if (!rod.isEmpty()) {
                    RodItem.wearLine(rod, t.snapWearPct());
                    RodItem.setBait(rod, HookBait.NONE);
                }
                line = "The line snapped. Hook and worm are gone; you tie on a spare from the winder.";
            }
            case SPIT -> line = "Slack line. It spat the hook.";
            case BITE_THROUGH -> {
                if (!rod.isEmpty()) RodItem.setBait(rod, HookBait.NONE);
                line = "It bit through the line.";
            }
            case SNAG -> {
                if (!rod.isEmpty()) RodItem.setBait(rod, HookBait.NONE);
                line = "Snagged. The line parted.";
            }
            case ROD_BROKE -> line = "The rod cracked.";
            default -> line = "The fish is gone.";
        }
        say(o, line);
        if (o != null) send(o, new FishResultPayload(f.fightId, out, f.params.species().id(), grams));
        b.discard();
    }

    public static ItemStack landedFish(int grams) {
        ItemStack fish = new ItemStack(FishingRegistry.ARCTIC_GRAYLING.get());
        fish.set(YoiComponents.WEIGHT_G.get(), grams);
        return fish;
    }

    // ---- ending -------------------------------------------------------------------------------------------------

    public static void reelIn(ServerPlayer p, BobberEntity b, String line) {
        b.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 0.6f, 1.0f);
        say(p, line);
        b.discard();
    }

    /** The angler is gone, switched hands or walked off: a hooked fish is lost. */
    static void drop(BobberEntity b, String line) {
        if (b.fight != null) {
            b.fight.cancel();
            finish(b);
            return;
        }
        b.discard();
    }

    static void removed(BobberEntity b) {
        if (b.owner != null) BOBBERS.remove(b.owner.getUUID(), b);
        if (b.fight != null && b.owner != null && !b.fight.over()) {
            b.fight.cancel();
            send(b.owner, new FishResultPayload(b.fight.fightId, FightModel.Outcome.CANCELLED,
                    b.fight.params.species().id(), 0));
            b.fight = null;
        }
    }
}
