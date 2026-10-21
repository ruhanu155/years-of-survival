package com.yearofice.body.debug;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyConfig;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.food.Meals;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.Clothing;
import com.yearofice.sim.body.ClothingState;
import com.yearofice.sim.body.StartKit;
import com.yearofice.sim.body.Warmth;
import com.yearofice.sim.body.WarmthContext;
import com.yearofice.sim.body.WaterPurity;
import com.yearofice.sim.body.Zone;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.time.Rate;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The M7 proxy check's player ({@code YEAROFICE_SMOKE=m7week}; never active otherwise), standing in for the gate "a
 * December week: winter is dangerous but fair". Survival, no debug commands: the crate kit plus the M4-M7 winter kit
 * (mittens, parka, boots, dried food, matches), a fire through the fire api, boiled water, sleep under a roof, the
 * day's chores in a ledger. Seven days at the accelerated clock; day 4 is a -25 C day (the weather override is the
 * driver's test seam) with two hours outdoors. Every vital change must have a Messenger line: crossings are compared
 * with the HudBus journal. Log {@code smoke/m7week.log}; the client (BodySmoke) grabs the shots named in {@link #shot}.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class WeekDriver {
    private WeekDriver() {}

    public static final boolean ON = "m7week".equals(System.getenv("YEAROFICE_SMOKE"));
    public static volatile boolean done = false;
    public static volatile String verdict = "";
    public static volatile String shot = null;

    static final int DAYS = 7;
    static final long DEC1 = 214; // absolute day of 1 December in year 1 (Calendar)

    private static int tick = 0, shotUntil = 0;
    private static long start = -1, d0 = 0;
    private static PrintWriter out;
    private static BlockPos ringPos, bedPos, outdoor;
    private static FireRingBlockEntity ring;
    private static final Map<String, Double> chores = new LinkedHashMap<>();
    private static final Set<String> did = new java.util.HashSet<>();
    private static final List<String> failures = new ArrayList<>();
    private static final List<String> unmessaged = new ArrayList<>();
    private static int lastDay = 0, crossings = 0, covered = 0, ateBites = 0, matches = 0;
    private static double warmMin = 100, handsExpMin = 99, mitExpMin = 99, handsFrostMax = 0, handsFrostWindow = 0, airMin = 99;
    private static double minBmi = 99, litres = 0, kcalFood = 0;
    private static boolean cold25 = false;
    private static ServerPlayer pl;

    // crossing tracker (the same lines VitalExplainer raises, plus frost stage per part)
    private record Pend(long minute, String label, String key, int before, int due, boolean asleep) {}
    private static final Map<String, Integer> posted = new java.util.concurrent.ConcurrentHashMap<>();
    static final boolean LEAN_FIRST = "leanfirst".equals(System.getenv("YEAROFICE_M7_DIET"));
    private static final Map<String, double[]> diet = new LinkedHashMap<>();
    private static int mealNo = 0;
    private static Map<String, Integer> prevCounts = new java.util.HashMap<>();
    private static final List<Pend> pend = new ArrayList<>();
    private static boolean[] was = null;
    private static int[] wasBand = new int[Part.COUNT], wasFrostStage = new int[Part.COUNT];
    private static int wasDigits = 0;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (!ON || done || !ClockService.installed() || !HudBus.installed()) return;
        var server = e.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
        tick++;
        if (tick < 200) return;
        try {
            if (start < 0) begin(p, server.getServerDirectory());
            step(p);
        } catch (RuntimeException ex) {
            YearOfIce.LOG.error("Week driver failed", ex);
            if (out == null) { done = true; verdict = "FAIL driver exception " + ex; return; }
            finish("FAIL driver exception " + ex);
        }
    }

    static String fmt(double v) { return String.format(java.util.Locale.ROOT, "%.1f", v); }

    static void log(String s) { out.println(s); out.flush(); YearOfIce.LOG.info("M7WEEK {}", s); }

    static String clock(long m) { long d = Math.floorMod(m, 1440L); return String.format("%02d:%02d", d / 60, d % 60); }

    static void chore(String kind, double min) { chores.merge(kind, min, Double::sum); }

    static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse("yearofice:" + id)); }

    private static boolean solidTop(ServerLevel l, BlockPos p) {
        return l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir() && l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP)
                && l.getFluidState(p).isEmpty();
    }

    private static void begin(ServerPlayer p, Path dir) {
        try {
            Path f = dir.resolve("smoke/m7week.log");
            Files.createDirectories(f.getParent());
            out = new PrintWriter(Files.newBufferedWriter(f));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        p.setGameMode(GameType.SURVIVAL);
        ServerLevel level = p.serverLevel();
        int moved = 0;
        BlockPos c = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-24, -6, -24), c.offset(24, 8, 24))) {
            if (level.getBlockEntity(q) instanceof BarrelBlockEntity b) {
                b.unpackLootTable(p);
                for (int i = 0; i < b.getContainerSize(); i++) {
                    ItemStack st = b.getItem(i);
                    if (!st.isEmpty()) { p.getInventory().add(st.copy()); b.setItem(i, ItemStack.EMPTY); moved++; }
                }
                if (moved > 0) break;
            }
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(com.yearofice.item.inventory.PackItems.CRATE_BACKPACK.get())) {
                if (com.yearofice.item.inventory.PackSlots.setBack(p, st.copy())) p.getInventory().setItem(i, ItemStack.EMPTY);
                break;
            }
        }
        // GRANT: the M4-M7 winter kit has no crate source in the slice; a player would craft or find it. Logged, never hidden.
        String[][] grants = {{"fur_mittens", "1"}, {"fur_hat", "1"}, {"dried_meat", "20"}, {"parka", "1"}, {"mukluks", "1"}, {"pemmican", "30"}, {"dried_fish", "20"}, {"pilot_bread", "12"}, {"match", "12"}};
        for (String[] g : grants) {
            Item it = item(g[0]);
            if (it == net.minecraft.world.item.Items.AIR) { log("GRANT-MISSING " + g[0] + " is not a registered item"); continue; }
            p.getInventory().add(new ItemStack(it, Integer.parseInt(g[1])));
            log("GRANT " + g[0] + " x" + g[1]);
        }
        // the December jump is the driver's own hook; bodies are not stepped across a jump (BodyTicker)
        com.yearofice.hud.HudBusImpl.tap = (pp, l) -> posted.merge(l.key(), 1, Integer::sum);
        ClockService ck = ClockService.get();
        ck.setNow((DEC1 * 1440L + 6 * 60) * 600L);
        com.yearofice.body.BodyTicker.resync(p.server);
        ck.forceRate(Rate.SLEEP);
        start = ck.nowMinute();
        d0 = Math.floorDiv(start, 1440L) * 1440L;
        BodyState s = BodyAttachments.of(p);
        log("START minute " + start + " clock " + clock(start) + " absDay " + ck.absDay() + " dayOfYear " + ck.dayOfYear() + " kg " + fmt(s.kg) + " bmi " + fmt(s.bmi()));
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!st.isEmpty()) log("  kit: " + BuiltInRegistries.ITEM.getKey(st.getItem()) + " x" + st.getCount());
        }
        for (String id : new String[] {"parka", "mukluks", "fur_mittens", "fur_hat"}) {
            ItemStack st = ItemStack.EMPTY;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (BuiltInRegistries.ITEM.getKey(p.getInventory().getItem(i).getItem()).getPath().equals(id)) { st = p.getInventory().getItem(i); break; }
            if (st.isEmpty()) { log("EQUIP " + id + " not in pack"); continue; }
            ItemStack held = p.getMainHandItem().copy();
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, st);
            var r = st.use(level, p, net.minecraft.world.InteractionHand.MAIN_HAND);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
            if (r.getResult().consumesAction()) { for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i) == st && st.isEmpty()) p.getInventory().setItem(i, ItemStack.EMPTY); }
            log("EQUIP " + id + " by use action: " + r.getResult());
        }
        for (var w : com.yearofice.api.body.ClothingService.get().worn(p)) log("WORN " + w.garment().zone().id() + " " + w.garment().layer() + " " + w.garment().id() + " warmth " + w.garment().warmthC() + " safe_to " + w.garment().safeToC());
        camp(p, level);
        kitTable(s);
    }

    private static ClothingState wornState(ServerPlayer p, BodyState s) {
        ClothingState c = s.clothing();
        for (var w : com.yearofice.api.body.ClothingService.get().worn(p)) c = c.wear(w);
        return c;
    }

    /** The blueprint garment table through the body's own Warmth: what the pack's winter kit would be worth if it were worn. */
    private static void kitTable(BodyState s) {
        ClothingState worn = StartKit.wear(s.clothing(), BodyConfig.tuning().kitWarmthC());
        ClothingState full = worn;
        for (String id : new String[] {"parka", "mukluks", "fur_mittens"}) {
            var row = item(id).builtInRegistryHolder().getData(com.yearofice.body.clothing.WearMap.WEAR);
            if (row != null) full = full.wear(com.yearofice.sim.body.WornGarment.fresh(row.garment(id)));
        }
        for (double air : new double[] {-14, -25}) {
            var env = com.yearofice.sim.env.EnvSample.outdoors(air, 0);
            log(String.format(java.util.Locale.ROOT, "TABLE air %.0f C no wind, resting, no fire: feels-like start kit %.1f C, with parka+mukluks+mittens %.1f C; hands exposure %.1f vs %.1f (safe-to %.0f vs %.0f); kit dial %.1f",
                    air, Warmth.feelsLike(env, worn, List.of(), WarmthContext.RESTING).totalC(), Warmth.feelsLike(env, full, List.of(), WarmthContext.RESTING).totalC(),
                    Warmth.zoneExposureC(env, worn, List.of(), WarmthContext.RESTING, Zone.HANDS), Warmth.zoneExposureC(env, full, List.of(), WarmthContext.RESTING, Zone.HANDS),
                    Clothing.zoneSafeToC(worn, Zone.HANDS), Clothing.zoneSafeToC(full, Zone.HANDS), BodyConfig.tuning().kitWarmthC()));
        }
    }

    private static void camp(ServerPlayer p, ServerLevel level) {
        BlockPos c = p.blockPosition();
        BlockPos bed = null;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-12, -2, -12), c.offset(12, 3, 12))) {
            if (bed != null) break;
            boolean ok = true;
            for (int dx = -1; dx <= 2 && ok; dx++) for (int dz = -1; dz <= 1 && ok; dz++) {
                BlockPos r = q.offset(dx, 0, dz);
                ok = level.getBlockState(r.below()).isFaceSturdy(level, r.below(), Direction.UP) && level.getFluidState(r).isEmpty();
            }
            if (ok) bed = q.immutable();
        }
        if (bed == null) bed = c;
        bedPos = bed;
        ringPos = bed.east();
        // fell the trees on the camp site (a player would): logs and leaves within 8 m of the fire, so the camp is clear to see
        int felled = 0;
        for (BlockPos q : BlockPos.betweenClosed(bed.offset(-8, 0, -8), bed.offset(9, 14, 8))) {
            String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(q).getBlock()).getPath();
            if (path.contains("log") || path.contains("leaves") || path.contains("leaf") || path.contains("branch") || path.contains("needle") || path.contains("trunk") || path.contains("stump")) {
                level.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
                felled++;
            }
        }
        log("camp site cleared: " + felled + " tree blocks felled within 8 m");
        // the lean-to: a plank roof on four posts over bed and fire (the build pieces of M8 are not in this lane)
        for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 4; dy++) level.setBlock(bed.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
        for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlock(bed.offset(dx, 3, dz), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
        for (int[] o : new int[][] {{-1, -1}, {-1, 1}, {2, -1}, {2, 1}}) for (int dy = 0; dy <= 2; dy++) level.setBlock(bed.offset(o[0], dy, o[1]), Blocks.SPRUCE_LOG.defaultBlockState(), 3);
        level.setBlock(ringPos, FireRegistry.FIRE_RING.get().defaultBlockState(), 3);
        ring = level.getBlockEntity(ringPos) instanceof FireRingBlockEntity r ? r : null;
        p.teleportTo(level, bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 270f, 15f);
        for (int r = 8; r <= 16 && outdoor == null; r++) {
            for (int a = 0; a < 360 && outdoor == null; a += 30) {
                BlockPos q = bed.offset((int) (r * Math.cos(Math.toRadians(a))), 0, (int) (r * Math.sin(Math.toRadians(a))));
                for (int dy = -3; dy <= 3; dy++) if (outdoor == null && solidTop(level, q.offset(0, dy, 0)) && level.canSeeSky(q.offset(0, dy, 0))) outdoor = q.offset(0, dy, 0).immutable();
            }
        }
        log("camp: bed " + bed.toShortString() + " ring " + ringPos.toShortString() + " roofed (plank roof y+3), open ground for chores " + (outdoor == null ? "NONE" : outdoor.toShortString()));
    }

    private static boolean once(String key, long m) { return did.add(key + "@" + Math.floorDiv(m - d0, 1440L)); }

    private static void aimShot(ServerPlayer p, ServerLevel level, String name) {
        if (p.isSleeping()) BodyService.get().wake(p, "morning");
        Vec3 look = Vec3.atCenterOf(ringPos).add(0, 0.5, 0);
        // first person from the bed end of the lean-to, two blocks from the fire: what the sleeper sees
        Vec3 at = new Vec3(bedPos.getX() - 0.5 + 0.5, bedPos.getY(), bedPos.getZ() + 0.5);
        double dx = look.x - at.x, dz = look.z - at.z, dy = look.y - (at.y + 1.6);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        p.teleportTo(level, at.x, at.y, at.z, yaw, pitch);
        shot = name;
        shotUntil = tick + 60;
        ClockService.get().forceRate(Rate.WORLD); // 60 ticks at the Sleep rate would be three game hours with the driver frozen
        log("SHOT " + name + " aimed at the fire from the bed end, 2 m, yaw " + fmt(yaw) + " pitch " + fmt(pitch));
    }

    private static void step(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ClockService ck = ClockService.get();
        long m = ck.nowMinute();
        int day = (int) Math.floorDiv(m - d0, 1440L) + 1;
        double h = Math.floorMod(m, 1440L) / 60.0;
        BodyState s = BodyAttachments.of(p);
        BodyService body = BodyService.get();
        pl = p;
        if (!p.isAlive()) { finish("FAIL the player died on day " + day); return; }
        if (s.sos != 0) { finish("FAIL collapse: SOS state " + s.sos + " on day " + day + " at " + clock(m)); return; }
        track(p, s, m, day);
        if (day > lastDay) newDay(day);
        if (day > DAYS) { finish(""); return; }
        if (tick < shotUntil) return; // the camera holds still for the shot
        if (ck.forcedRate() != Rate.SLEEP) ck.forceRate(Rate.SLEEP);
        boolean night = h >= 22 || h < 6;
        // day 4 is the cold day: a clean -25 C air for the 24 h, two hours of wood work in the open
        if (day == 4 && h >= 0 && !cold25) {
            EnvService.get().setOverride(level, new EnvService.WeatherOverride(-25, Double.NaN, null, Double.NaN));
            cold25 = true;
            log("[d4] weather override -25 C set for the day (driver test seam)");
        }
        if (day == 5 && cold25) { EnvService.get().setOverride(level, null); cold25 = false; log("[d5] weather override cleared"); }
        if (night) {
            if (!p.isSleeping() && tick % 20 == 0) {
                var r = com.yearofice.body.sleep.Sleep.class.cast(com.yearofice.api.body.SleepService.get()).lieOnGround(p, m);
                if (r != com.yearofice.api.body.SleepService.Refusal.NONE && did.add("lie" + (m / 60))) log("  [d" + day + " " + clock(m) + "] lie down refused: " + r + " at " + p.blockPosition().toShortString() + " energy " + fmt(s.energy));
            }
        } else if (p.isSleeping()) body.wake(p, "morning");
        if (h >= 6.5 && once("fire", m)) lightFire(p, m);
        if (ring != null && !ring.isRemoved() && tick % 20 == 0) {
            String st = ring.fire().state().name();
            if ((st.equals("OUT") || st.equals("BANKED")) && (h >= 16.5 || h < 6) && once("relight" + (m / 30), m)) lightFire(p, m);
            else if ((st.equals("FIRE") || st.equals("SMALL") || st.equals("COALS")) && ring.fire().armloads() < (p.isSleeping() ? 0.1 : (night || h >= 21.5) ? 4.0 : 0.8)) {
                var ft = com.yearofice.fire.FireConfig.tuning();
                int fed = 0;
                for (int i = 0; i < 12; i++) if (ring.addPart(new ItemStack(night || h >= 21.5 ? net.minecraft.world.item.Items.BIRCH_LOG : net.minecraft.world.item.Items.SPRUCE_LOG), ft, p).ok()) fed++; else break;
                if (fed > 0) { if (p.isSleeping()) body.wake(p, "tends the fire"); chore("fire", 1.5); }
            }
        }
        if (!night) {
            if (h >= 7.0 && once("boil1", m)) boil(p);
            if (h >= 13.0 && once("boil2", m)) boil(p);
            if (h >= 7.2 && once("meal1", m)) meal(p);
            if (h >= 12.5 && once("meal2", m)) meal(p);
            if (h >= 18.8 && once("meal3", m)) meal(p);
            if (h >= 10.0 && h < 12.0 && outdoor != null && !p.isSleeping()) {
                // chores in the open: wood cutting and carrying; the survivor stands at the cutting spot
                if (once("out", m)) { p.teleportTo(level, outdoor.getX() + 0.5, outdoor.getY(), outdoor.getZ() + 0.5, 0, 10f); chore("wood", 120); log("[d" + day + " " + clock(m) + "] out to the open ground for wood, back at 12:00"); }
            } else if (h >= 12.0 && h < 12.2 && outdoor != null && once("back", m)) {
                p.teleportTo(level, bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 270f, 15f);
            }
            if (h >= 6 && h < 22 && once("drink" + (int) (Math.floorMod(m, 1440L) / 90), m)) { body.drink(p, 0.5, WaterPurity.Source.CREEK, true); litres += 0.5; }
        }
        if (day == 1 && m >= ck.duskMinute(ck.absDay()) - 25 && did.add("dusk1")) aimShot(p, level, "m7_first_dusk");
        if (s.warmth < 18 && did.add("coldshot")) aimShot(p, level, "m7_cold_d" + day + "_vitals");
        if (day == 4 && h >= 12.3 && once("shot4", m)) aimShot(p, level, "m7_d4_minus25_hands");
        if (day == 3 && h >= 12.3 && once("shot3", m)) aimShot(p, level, "m7_day3_camp");
        if (day == 7 && h >= 12.6 && once("shot7", m)) aimShot(p, level, "m7_day7_vitals");
    }

    /** Samples hands, warmth and the crossings every tick; hourly detail goes to the log. */
    private static void track(ServerPlayer p, BodyState s, long m, int day) {
        warmMin = Math.min(warmMin, s.warmth);
        minBmi = Math.min(minBmi, s.bmi());
        for (Part part : new Part[] {Part.LEFT_HAND, Part.RIGHT_HAND}) {
            handsFrostMax = Math.max(handsFrostMax, s.parts.frost(part));
            if (cold25) handsFrostWindow = Math.max(handsFrostWindow, s.parts.frost(part));
        }
        if (tick % 10 == 0) {
            ServerLevel level = p.serverLevel();
            var env = EnvService.get().sample(level, p.blockPosition());
            airMin = Math.min(airMin, env.airC());
            ClothingState worn = wornState(p, s);
            var row = item("fur_mittens").builtInRegistryHolder().getData(com.yearofice.body.clothing.WearMap.WEAR);
            ClothingState withMit = true ? worn : worn.wear(com.yearofice.sim.body.WornGarment.fresh(row.garment("fur_mittens")));
            double e1 = Warmth.zoneExposureC(env, worn, List.of(), WarmthContext.RESTING, Zone.HANDS);
            double e2 = Warmth.zoneExposureC(env, withMit, List.of(), WarmthContext.RESTING, Zone.HANDS);
            // margin above the zone's safe-to limit (negative: below it)
            handsExpMin = Math.min(handsExpMin, e1 - Clothing.zoneSafeToC(worn, Zone.HANDS));
            mitExpMin = Math.min(mitExpMin, e2 - Clothing.zoneSafeToC(withMit, Zone.HANDS));
        }
        // crossings VitalExplainer is meant to explain
        boolean[] now = {s.warmth < 70, s.water < 60, s.energy < 5, s.condition() < 60, s.balance24h() < -1000};
        String[] labels = {"warmth_low", "water_low", "energy_low", "condition_low", "calories_low"};
        Map<String, Integer> prev = prevCounts;
        prevCounts = new java.util.HashMap<>();
        for (String k : new String[] {"warmth_low", "water_low", "energy_low", "condition_low", "calories_low", "part_worse", "frost", "digit_lost"}) prevCounts.put(k, count(p, k));
        if (was != null) {
            for (int i = 0; i < now.length; i++) if (now[i] && !was[i]) add(m, labels[i], labels[i], prev.getOrDefault(labels[i], 0));
            for (Part part : Part.values()) {
                int b = s.parts.band(part).ordinal();
                if (b > wasBand[part.ordinal()]) add(m, "part_worse:" + part, "part_worse", prev.getOrDefault("part_worse", 0));
                int fs = com.yearofice.sim.body.FrostModel.stage(s.parts.frost(part));
                if (fs > wasFrostStage[part.ordinal()]) add(m, "frost_stage" + fs + ":" + part, "frost", prev.getOrDefault("frost", 0));
            }
            int dg = Integer.bitCount(s.parts.lostDigits()[0]) + Integer.bitCount(s.parts.lostDigits()[1]);
            if (dg > wasDigits) add(m, "digit_lost", "digit_lost", prev.getOrDefault("digit_lost", 0));
        }
        was = now;
        for (Part part : Part.values()) { wasBand[part.ordinal()] = s.parts.band(part).ordinal(); wasFrostStage[part.ordinal()] = com.yearofice.sim.body.FrostModel.stage(s.parts.frost(part)); }
        wasDigits = Integer.bitCount(s.parts.lostDigits()[0]) + Integer.bitCount(s.parts.lostDigits()[1]);
        List<Pend> toAdd = new ArrayList<>();
        for (var it = pend.iterator(); it.hasNext();) {
            Pend q = it.next();
            if (q.asleep && p.isSleeping()) continue; // lines raised asleep post at wake ("While you slept")
            if (q.asleep) { it.remove(); toAdd.add(new Pend(q.minute, q.label, q.key, q.before, tick + 6, false)); continue; }
            if (tick < q.due) continue;
            it.remove();
            boolean ok = count(p, q.key) > q.before;
            if (ok) covered++;
            else {
                unmessaged.add(clock(q.minute) + " d" + day + " " + q.label);
                StringBuilder ks = new StringBuilder();
                for (var l : HudBus.get().journal(p)) ks.append(l.key()).append(' ');
                log("UNMESSAGED vital change at " + clock(q.minute) + " d" + day + ": " + q.label + " ; journal keys: " + ks);
            }
        }
        pend.addAll(toAdd);
        if (Math.floorMod(m, 1440L) / 60 == 6 && day >= 2 && did.add("morning" + day)) {
            double metaCap = com.yearofice.sim.body.Metabolism.energyCap(s.balance24h(), s.bmi(), s.water, s.awakeMin, s.gutIll(m));
            String why = switch (s.lastCapCause) { case 0 -> "none"; case 1 -> "hungry"; case 2 -> "thin"; case 3 -> "thirsty"; case 4 -> "gut"; case 5 -> "awake"; default -> "ill"; };
            StringBuilder ill = new StringBuilder();
            for (var a : s.ill) ill.append(a).append(' ');
            log(String.format(java.util.Locale.ROOT, "MORNING d%d energy %.1f cap %.1f | rest line messenger.yearofice.vitals.capped(%d, messenger.yearofice.vitals.cause.%s [synced capCause]) | engine: metabolic cap %.1f, illness active [%s], awakeMin %d, balance24h %.0f, water %.0f | macro 72h carb %.2f fat %.2f protein %.2f",
                    day, s.energy, s.lastEnergyCap, Math.round(s.lastEnergyCap), why, metaCap, ill.toString().trim(), s.awakeMin, s.balance24h(), s.water, s.macroMeter(0), s.macroMeter(1), s.macroMeter(2)));
            if (s.lastEnergyCap < 8) failures.add("morning energy cap " + fmt(s.lastEnergyCap) + " < 8 on day " + day);
        }
        if (did.add("sample" + (m / 60))) {
            log(String.format(java.util.Locale.ROOT, "  [d%d %s] air %.1f warmth %.0f water %.0f energy %.1f kg %.1f kcal24 %.0f %s fire %s handsFrost %d/%d cap %.1f", day, clock(m),
                    EnvService.get().airC(p.serverLevel(), p.blockPosition()), s.warmth, s.water, s.energy, s.kg, s.balance24h(), p.isSleeping() ? "asleep" : "awake",
                    ring == null ? "-" : ring.fire().state().name() + " " + fmt(ring.fire().armloads()), s.parts.frost(Part.LEFT_HAND), s.parts.frost(Part.RIGHT_HAND), s.lastEnergyCap));
        }
    }

    private static void add(long m, String label, String key, int jsize) {
        crossings++;
        pend.add(new Pend(m, label, key, jsize, tick + 6, pl != null && pl.isSleeping()));
    }

    /** Lines the bus holds for the player whose key contains {@code key} ("" counts all), over journal, screen and held lists. */
    private static int count(ServerPlayer p, String key) {
        int n = 0;
        for (var e : posted.entrySet()) if (e.getKey().contains(key)) n += e.getValue();
        return n;
    }

    private static void newDay(int day) {
        if (lastDay > 0) closeDay(lastDay);
        lastDay = day;
        chores.clear();
        warmMin = 100;
        handsExpMin = 99;
        mitExpMin = 99;
        litres = 0;
    }

    private static void closeDay(int day) {
        BodyState s = BodyAttachments.of(pl);
        double in = 0, outK = 0;
        for (double v : s.kcalIn) in += v;
        for (double v : s.kcalOut) outK += v;
        double busy = 0;
        for (var e : chores.entrySet()) busy += e.getValue();
        double waking = 960;
        if (s.bmi() <= 19) failures.add("BMI " + fmt(s.bmi()) + " on day " + day);
        if (busy > 0.7 * waking) failures.add("chores " + fmt(busy / waking * 100) + "% of waking time on day " + day);
        log(String.format(java.util.Locale.ROOT, "DAY %d kcal_in %.0f kcal_out %.0f net %.0f kg %.2f bmi %.2f warmth_min %.1f hands_margin_worn_kit %.1f C hands_margin_with_mittens %.1f C hands_frost_max %.0f air_min %.1f water_L %.1f | chores %.0f min of %.0f waking, free %.0f | %s",
                day, in, outK, in - outK, s.kg, s.bmi(), warmMin, handsExpMin, mitExpMin, handsFrostMax, airMin, litres, busy, waking, waking - busy, chores));
    }

    private static void lightFire(ServerPlayer p, long m) {
        chore("fire", 18);
        if (ring == null || ring.isRemoved()) { log("no fire ring"); return; }
        String st0 = ring.fire().state().name();
        if (st0.equals("FIRE") || st0.equals("COALS")) return;
        var t = com.yearofice.fire.FireConfig.tuning();
        ring.addPart(new ItemStack(FireRegistry.TINDER_BUNDLE.get()), t, p);
        ring.addPart(new ItemStack(FireRegistry.KINDLING.get()), t, p);
        ring.addPart(new ItemStack(FireRegistry.KINDLING.get()), t, p);
        for (int i = 0; i < 4; i++) ring.addPart(new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG), t, p);
        ItemStack match = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(FireRegistry.MATCH.get())) { match = p.getInventory().getItem(i); break; }
        boolean lit;
        if (!match.isEmpty()) { lit = ring.strikeMatch(p, match, 0.0).ok(); matches++; chore("fire", 3); }
        else { lit = ring.emberCaught(p).ok(); chore("fire", 8); }
        ring.puff();
        ring.puff();
        log("[" + clock(m) + "] fire lit " + (match.isEmpty() ? "bow drill" : "match") + ": " + (lit ? "yes" : "NO") + " " + ring.fire().state());
    }

    private static void boil(ServerPlayer p) {
        chore("water", 4);
        chore("water_wait", WaterPurity.minutesToSafe(WaterPurity.Heat.STEADY_FIRE, 2.0));
        BodyService.get().drink(p, 0.5, WaterPurity.Source.CREEK, true);
        litres += 0.5;
    }

    // per 100 g from food.json: kcal, protein g, fat g (carbs = the rest of the kcal at 4 kcal/g)
    private static final Map<String, double[]> ROW = Map.of("pemmican", new double[] {560, 30.8, 46.7}, "dried_meat", new double[] {288, 62.5, 4.25},
            "dried_fish", new double[] {350, 62, 11.3}, "pilot_bread", new double[] {400, 10, 8.9});

    private static int bite(ServerPlayer p, String id, BodyState s, int day) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.isEmpty() || !BuiltInRegistries.ITEM.getKey(st.getItem()).getPath().equals(id)) continue;
            double before = s.pendingKcal;
            var b = Meals.eat(p, st);
            if (!b.eaten()) { log("  meal refused for " + id + ": " + b.outcome() + " stomach " + fmt(s.stomachKg)); return -1; }
            double[] r = ROW.get(id);
            double g = b.grams(), kcal = g * r[0] / 100, pr = g * r[1] / 100, fat = g * r[2] / 100, carb = Math.max(0, (kcal - 4 * pr - 9 * fat) / 4);
            double[] tot = diet.computeIfAbsent("d" + day + " " + id, k -> new double[5]);
            tot[0] += g; tot[1] += kcal; tot[2] += fat; tot[3] += pr; tot[4] += carb;
            log(String.format(java.util.Locale.ROOT, "  BITE d%d %s %.0f g %.0f kcal fat %.1f protein %.1f carb %.1f (pending %+.0f) | macro meters 72h carb %.2f fat %.2f protein %.2f",
                    day, id, g, kcal, fat, pr, carb, s.pendingKcal - before, s.macroMeter(0), s.macroMeter(1), s.macroMeter(2)));
            return 1;
        }
        return 0;
    }

    private static void meal(ServerPlayer p) {
        BodyState s = BodyAttachments.of(p);
        int day = lastDay, bites = 0;
        mealNo++;
        if (LEAN_FIRST) {
            // v4 diet: dried meat first (inventory order), pemmican and fish after, bread to fill
            for (String id : new String[] {"dried_meat", "pemmican", "dried_fish", "pilot_bread"}) {
                while (bites < 40 && s.balance24h() + s.pendingKcal < 0) { int r = bite(p, id, s, day); if (r <= 0) break; bites++; }
            }
        } else {
            // a player's meal: pemmican with every meal (2 bites), alternate dried fish / dried meat (2 bites), bread to fill
            // (only while the 24 h balance is short: a player stops when fed)
            if (s.balance24h() + s.pendingKcal < 0 && bite(p, "pemmican", s, day) > 0) bites++;
            String alt = mealNo % 2 == 0 ? "dried_fish" : "dried_meat";
            if (s.balance24h() + s.pendingKcal < 0 && bite(p, alt, s, day) > 0) bites++;
            while (bites < 40 && s.balance24h() + s.pendingKcal < 0) { int r = bite(p, "pilot_bread", s, day); if (r <= 0) break; bites++; }
        }
        ateBites += bites;
        chore("food", 4 + bites * 0.3);
    }

    private static void finish(String failure) {
        if (!failure.isEmpty()) failures.add(failure.replaceFirst("^FAIL ", ""));
        if (lastDay > 0 && lastDay <= DAYS && failure.isEmpty()) closeDay(lastDay);
        else if (lastDay > 0 && !failure.isEmpty()) closeDay(lastDay);
        if (!unmessaged.isEmpty()) failures.add("vital changes without a Messenger line: " + unmessaged.size());
        if (handsFrostWindow > 0) failures.add("hands took frost points (max " + fmt(handsFrostWindow) + ") on the -25 C day with the worn kit");
        verdict = failures.isEmpty() ? "PASS" : "FAIL " + failures;
        for (var e : diet.entrySet()) { double[] v = e.getValue(); log(String.format(java.util.Locale.ROOT, "DIET %s %.0f g %.0f kcal fat %.0f protein %.0f carb %.0f", e.getKey(), v[0], v[1], v[2], v[3], v[4])); }
        log("DIET MODE " + (LEAN_FIRST ? "leanfirst (v4)" : "player (v5)"));
        log("RESULT " + verdict);
        log(String.format(java.util.Locale.ROOT, "SUMMARY crossings %d covered %d unmessaged %d | hands frost max (week) %.0f, on the -25 C day %.0f | worst hands margin vs safe-to: worn kit %.1f C, mittens %.1f C | min BMI %.2f | bites %d matches %d",
                crossings, covered, unmessaged.size(), handsFrostMax, handsFrostWindow, handsExpMin, mitExpMin, minBmi, ateBites, matches));
        out.close();
        done = true;
    }
}
