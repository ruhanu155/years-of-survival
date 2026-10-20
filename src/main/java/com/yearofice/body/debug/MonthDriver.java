package com.yearofice.body.debug;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.body.BodyAttachments;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.carcass.Butchering;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.food.Meals;
import com.yearofice.food.cooking.SpitBlockEntity;
import com.yearofice.food.cooking.SpitPlacing;
import com.yearofice.food.preserve.PreserveRegistry;
import com.yearofice.food.preserve.RackBlockEntity;
import com.yearofice.food.traps.SnareBlockEntity;
import com.yearofice.food.traps.SnareSite;
import com.yearofice.food.traps.TrapRegistry;
import com.yearofice.hunt.fishing.FishBaits;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.hunt.fishing.PassiveGearService;
import com.yearofice.plant.HarvestVerbs;
import com.yearofice.plant.PlantBlock;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.WaterPurity;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The M5 proxy check's player ({@code YEAROFICE_SMOKE=m5month}; never active otherwise): a scripted good player who
 * takes the crate kit and plays 30 game days at the accelerated clock, in survival, with no debug commands. Each day
 * the survivor lights the fire, boils and drinks creek water, forages the start patches, checks one snare and one set
 * line (digging worms for bait), cooks and dries what they catch, and sleeps beside the fire. Everything goes through
 * the same services the blocks and items call. Chore minutes are a ledger (hands-on work and waiting are kept apart),
 * not a clock cost, because the driver acts instantly; fuel logs come from the supply the survivor is assumed to cut.
 * The log {@code smoke/m5_month.log} has one DAY line per day (kcal in and out, BMI, warmth minimum, chore minutes).
 * BMI at or under 19 on any day is a FAIL that is reported with its day, never tuned away.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class MonthDriver {
    private MonthDriver() {}

    public static final boolean ON = "m5month".equals(System.getenv("YEAROFICE_SMOKE"));
    public static volatile boolean done = false;
    public static volatile String verdict = "";
    /** A screenshot name the client should grab now; the client clears it. */
    public static volatile String shot = null;

    static final int DAYS = 30;
    static final Set<String> SAFE = Set.of("blueberry_bush", "blueberry", "fireweed", "dandelion", "lowbush_cranberry", "highbush_cranberry", "rose_hips",
            "bearberry", "spruce_tips", "cloudberry", "crowberry", "fiddlehead", "sourdock", "wild_chive", "wild_rhubarb");

    private static int tick = 0;
    private static long start = -1, d0 = 0;
    private static PrintWriter out;
    private static BlockPos ringPos, snarePos, linePos, rackPos, drinkSpot;
    private static final List<BlockPos> snares = new ArrayList<>();
    static final int SNARES = Math.max(1, Integer.parseInt(System.getenv().getOrDefault("YEAROFICE_MONTH_SNARES", "1")));
    private static FireRingBlockEntity ring;
    private static final Map<String, Double> chores = new LinkedHashMap<>();
    private static final Set<String> did = new java.util.HashSet<>();
    private static double warmMin = 100, kcalAte = 0, litres = 0;
    private static int breadEaten = 0, forageItems = 0, snareCatches = 0, fishCaught = 0, matchesLeft = 3, litToday = 0;
    private static int lastDay = 0, firstLow = -1;
    private static double minBmi = 99;
    private static int minBmiDay = 0;
    private static final List<String> failures = new ArrayList<>();
    private static final Map<String, Integer> foodTally = new LinkedHashMap<>();
    private static List<BlockPos> plants = new ArrayList<>();
    private static final List<BlockPos> wormSpots = new ArrayList<>();
    private static int wormCursor = 0, dropped = 0;

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
            YearOfIce.LOG.error("Month driver failed", ex);
            finish("FAIL driver exception " + ex);
        }
    }

    static String fmt(double v) { return String.format(java.util.Locale.ROOT, "%.1f", v); }

    static void log(String s) { out.println(s); out.flush(); YearOfIce.LOG.info("M5MONTH {}", s); }

    static String clock(long m) { long d = Math.floorMod(m, 1440L); return String.format("%02d:%02d", d / 60, d % 60); }

    static void chore(String kind, double min) { chores.merge(kind, min, Double::sum); }

    private static void begin(ServerPlayer p, Path dir) {
        try {
            Path f = dir.resolve("smoke/m5_month.log");
            Files.createDirectories(f.getParent());
            out = new PrintWriter(Files.newBufferedWriter(f));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        p.setGameMode(GameType.SURVIVAL);
        int moved = 0;
        BlockPos c = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-24, -6, -24), c.offset(24, 8, 24))) {
            if (p.level().getBlockEntity(q) instanceof BarrelBlockEntity b) {
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
                boolean worn = com.yearofice.item.inventory.PackSlots.setBack(p, st.copy());
                if (worn) p.getInventory().setItem(i, ItemStack.EMPTY);
                log("backpack worn on the back: " + worn);
                break;
            }
        }
        ClockService.get().forceRate(Rate.SLEEP);
        start = ClockService.get().nowMinute();
        d0 = Math.floorDiv(start, 1440L) * 1440L;
        BodyState s = BodyAttachments.of(p);
        log("START minute " + start + " clock " + clock(start) + " kit stacks " + moved + " kg " + fmt(s.kg) + " height " + s.heightCm + " bmi " + fmt(s.bmi()));
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!st.isEmpty()) log("  kit: " + BuiltInRegistries.ITEM.getKey(st.getItem()) + " x" + st.getCount());
        }
        ServerLevel level = p.serverLevel();
        scanPlants(p, level);
        setUpCamp(p, level);
    }

    private static boolean solidTop(ServerLevel l, BlockPos p) {
        return l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir() && l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP)
                && l.getFluidState(p).isEmpty();
    }

    private static void scanPlants(ServerPlayer p, ServerLevel level) {
        plants.clear();
        BlockPos c = p.blockPosition();
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-50, -14, -50), c.offset(50, 14, 50))) {
            if (level.getBlockState(q).getBlock() instanceof PlantBlock pb) {
                seen.merge(pb.species().replace("yearofice:", ""), 1, Integer::sum);
                if (SAFE.contains(pb.species().replace("yearofice:", ""))) plants.add(q.immutable());
            }
        }
        log("PLANTS within 50 m: " + seen + " ; safe set " + plants.size());
    }

    private static void setUpCamp(ServerPlayer p, ServerLevel level) {
        BlockPos c = p.blockPosition();
        // fire ring, rack and drinking spot next to the player
        BlockPos bed = null;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-14, -2, -14), c.offset(14, 4, 14))) {
            if (bed == null && solidTop(level, q) && !level.canSeeSky(q) && !level.canSeeSky(q.above())) bed = q.immutable();
        }
        if (bed != null) {
            p.teleportTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5);
            for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                BlockPos r = bed.offset(o[0], 0, o[1]);
                if (ringPos == null && solidTop(level, r)) ringPos = r.immutable();
            }
        }
        if (ringPos == null) {
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-6, -3, -6), c.offset(6, 3, 6))) if (ringPos == null && solidTop(level, q)) ringPos = q.immutable();
        }
        log("bed " + (bed == null ? "none (open ground)" : bed.toShortString() + " under a roof"));
        if (ringPos == null) ringPos = c;
        level.setBlock(ringPos, FireRegistry.FIRE_RING.get().defaultBlockState(), 3);
        ring = level.getBlockEntity(ringPos) instanceof FireRingBlockEntity r ? r : null;
        for (int[] o : new int[][] {{2, 1}, {-2, 1}, {1, 2}, {1, -2}, {2, 2}, {-2, -2}, {3, 0}, {-3, 0}}) {
            BlockPos r = ringPos.offset(o[0], 0, o[1]);
            if (solidTop(level, r) && !r.equals(ringPos)) { rackPos = r; break; }
        }
        if (rackPos != null) {
            level.setBlock(rackPos, PreserveRegistry.DRYING_RACK.get().defaultBlockState(), 3);
            log("camp: fire ring " + ringPos.toShortString() + ", drying rack " + rackPos.toShortString());
        }
        // snares: the best habitat sites within 40 m, 12 blocks apart
        List<Object[]> sites = new ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-40, -6, -40), c.offset(40, 6, 40))) {
            if (!solidTop(level, q) || level.getBlockState(q.below()).is(Blocks.WATER)) continue;
            if (Math.abs(q.getX() - c.getX()) % 5 != 0 || Math.abs(q.getZ() - c.getZ()) % 5 != 0) continue;
            var rd = SnareSite.read(level, q, Direction.NORTH);
            double sc = rd.habitatShare() + (rd.quality().besideCover() ? 0.5 : 0) + (rd.quality().onTrail() ? 1 : 0);
            sites.add(new Object[] {q.immutable(), sc});
        }
        sites.sort((a, b) -> Double.compare((double) b[1], (double) a[1]));
        for (Object[] st : sites) {
            BlockPos q = (BlockPos) st[0];
            boolean far = true;
            for (BlockPos o : snares) if (o.distSqr(q) < 144) far = false;
            if (!far || snares.size() >= SNARES) continue;
            snares.add(q);
            level.setBlock(q, TrapRegistry.SNARE.get().defaultBlockState(), 3);
            if (level.getBlockEntity(q) instanceof SnareBlockEntity be) be.set(level);
            log("snare set at " + q.toShortString() + " site score " + fmt((double) st[1]));
        }
        if (snares.isEmpty()) log("NO SNARE SITE");
        snarePos = snares.isEmpty() ? null : snares.get(0);
        chore("snare", 12 * snares.size());
        // set line: water within 60 m, a shore cell beside it
        var spots = com.yearofice.api.world.FishSpotService.get().spotsNear(level, c, 200);
        log("fish spots within 200 m: " + spots.size() + (spots.isEmpty() ? "" : " nearest " + spots.get(0).type() + " at " + spots.get(0).centre().toShortString() + " r " + fmt(spots.get(0).radiusM()) + " species " + spots.get(0).species()));
        for (var sp : spots) {
            for (BlockPos q : BlockPos.betweenClosed(sp.centre().offset(-6, -6, -6), sp.centre().offset(6, 6, 6))) {
                if (level.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER) && level.getBlockState(q.above()).isAir()
                        && (linePos == null || q.distSqr(sp.centre()) < linePos.distSqr(sp.centre()))) linePos = q.immutable();
            }
            if (linePos != null) break;
        }
        for (int r = 4; r <= 140 && linePos == null; r += 2) {
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-r, -25, -r), c.offset(r, 25, r))) {
                if (Math.max(Math.abs(q.getX() - c.getX()), Math.abs(q.getZ() - c.getZ())) != r) continue;
                if (level.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER) && level.getBlockState(q.above()).isAir()) {
                    linePos = q.immutable();
                    break;
                }
            }
        }
        log("water for the set line: " + (linePos == null ? "NONE within 140 m" : linePos.toShortString() + " (" + fmt(Math.sqrt(linePos.distSqr(c))) + " m away)"));
        if (linePos != null) {
            ItemStack item = new ItemStack(FishingRegistry.SET_LINE_ITEM.get());
            ItemStack held = p.getMainHandItem().copy();
            p.setItemInHand(InteractionHand.MAIN_HAND, item);
            item.useOn(new UseOnContext(level, p, InteractionHand.MAIN_HAND, item, new BlockHitResult(Vec3.atCenterOf(linePos), Direction.UP, linePos, false)));
            p.setItemInHand(InteractionHand.MAIN_HAND, held);
            var g = PassiveGearService.at(level, linePos.above());
            if (g == null) g = PassiveGearService.at(level, linePos);
            if (g == null) {
                PassiveGearService.place(level, PassiveGearService.Kind.SET_LINE, linePos, 5, FishBaitsClock.minute(level));
                log("set line recorded directly at " + linePos.toShortString() + " (item click found no place)");
            } else log("set line placed at " + g.pos.toShortString() + " hooks " + g.hooks);
            chore("water", 0);
            chore("fish", 12);
        } else log("NO WATER FOR THE SET LINE within 60 m");
        drinkSpot = linePos;
    }

    /** Minute helper kept apart so the driver reads the same clock the gear settles on. */
    static final class FishBaitsClock {
        static long minute(ServerLevel l) { return com.yearofice.hunt.fishing.FishingClock.minute(l.getServer()); }
    }

    /** Into the pack; if there is no room the item falls at the survivor's feet, which is logged (and picked up again before the next meal). */
    private static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) { p.drop(st, false); dropped++; log("  (no room: dropped " + BuiltInRegistries.ITEM.getKey(st.getItem()) + ")"); }
    }

    private static void pickUpDrops(ServerPlayer p, ServerLevel level) {
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(6))) {
            ItemStack st = e.getItem().copy();
            if (p.getInventory().add(st)) e.discard(); else log("  (still no room for " + st + ")");
        }
    }

    private static boolean once(String key, long m) { return did.add(key + "@" + Math.floorDiv(m - d0, 1440L)); }

    private static void step(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long m = ClockService.get().nowMinute();
        int day = (int) Math.floorDiv(m - d0, 1440L) + 1;
        double h = Math.floorMod(m, 1440L) / 60.0;
        BodyState s = BodyAttachments.of(p);
        BodyService body = BodyService.get();
        if (!p.isAlive()) { finish("FAIL the player died on day " + day); return; }
        if (s.sos != 0) { finish("FAIL collapse: SOS state " + s.sos + " on day " + day + " at " + clock(m)); return; }
        warmMin = Math.min(warmMin, s.warmth);
        if (once("sample" + (int) h, m)) {
            log(String.format(java.util.Locale.ROOT, "  [d%d %s] warmth %.0f water %.0f energy %.1f kg %.1f stomach %.2f kcal24 %.0f %s fire %s %.1f", day, clock(m), s.warmth, s.water, s.energy, s.kg,
                    s.stomachKg, s.balance24h(), p.isSleeping() ? "asleep" : "awake", ring == null ? "-" : ring.fire().state().name(), ring == null ? 0 : ring.fire().armloads()));
        }
        if (day > lastDay) { newDay(day); }
        if (day > DAYS) { finish(""); return; }
        // bed
        boolean night = h >= 22 || h < 6;
        if (night) {
            if (!p.isSleeping() && tick % 20 == 0) {
                com.yearofice.body.sleep.Sleep.class.cast(com.yearofice.api.body.SleepService.get()).lieOnGround(p, m);
            }
        } else if (p.isSleeping()) body.wake(p, "morning");
        // fire: lit each morning, relit when it has died, fed whenever it runs low
        if (h >= 6.5 && once("fire", m)) lightFire(p, level, m);
        if (ring != null && !ring.isRemoved() && tick % 20 == 0) {
            String st = ring.fire().state().name();
            if ((st.equals("OUT") || st.equals("BANKED")) && (h >= 16.5 || h < 6) && once("relight" + (m / 30), m)) lightFire(p, level, m);
            else if ((st.equals("FIRE") || st.equals("SMALL") || st.equals("COALS")) && ring.fire().armloads() < (p.isSleeping() ? 0.25 : 0.8)) {
                var ft = com.yearofice.fire.FireConfig.tuning();
                int fed = 0;
                for (int i = 0; i < 12; i++) if (ring.addPart(new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG), ft, p).ok()) fed++; else break;
                if (fed > 0) { if (p.isSleeping()) body.wake(p, "tends the fire"); chore("fire", 1.5); }
            }
        }
        // the spit is watched while it cooks: off the fire at Done (or when it burns), onto the plate for the next meal
        if (!night && ringPos != null && tick % 10 == 0 && level.getBlockEntity(ringPos.above()) instanceof SpitBlockEntity sp && !sp.empty()
                && (sp.ring() == com.yearofice.sim.food.Doneness.Ring.DONE || sp.ring() == com.yearofice.sim.food.Doneness.Ring.BURNT)) {
            ItemStack off = sp.takeOff();
            if (!off.isEmpty()) { give(p, off); chore("food", 2); log("[d" + day + " " + clock(m) + "] spit off the fire: " + off.getCount() + " " + BuiltInRegistries.ITEM.getKey(off.getItem()).getPath()); }
        }
        if (!night) {
            if (h >= 7.0 && once("boil1", m)) boilAndDrink(p, level, 2.0);
            if (h >= 13.0 && once("boil2", m)) boilAndDrink(p, level, 2.0);
            if (h >= 7.5 && once("forage", m)) forage(p, level);
            if (h >= 8.0 && once("check1", m)) checkTraps(p, level, m);
            if (h >= 18.5 && once("check2", m)) checkTraps(p, level, m);
            if (h >= 8.5 && once("worms", m)) rebait(p, level);
            if (h >= 7.2 && once("meal1", m)) meal(p, level, "breakfast");
            if (h >= 12.5 && once("meal2", m)) meal(p, level, "lunch");
            if (h >= 18.8 && once("meal3", m)) { cookEvening(p, level); meal(p, level, "dinner"); }
            if (h >= 10 && once("rack", m)) rackCheck(p, level);
            if (h >= 6 && h < 22 && once("drink" + (int) (Math.floorMod(m, 1440L) / 90), m)) {
                body.drink(p, 0.5, WaterPurity.Source.CREEK, true);
                litres += 0.5;
            }
        }
        if (h >= 23.9 || (h < 0.05 && false)) { /* day closes in newDay */ }
        if (day == 7 && h >= 19.0 && once("shot7", m)) shot = "m5_day07";
        if (day == 30 && h >= 19.0 && once("shot30", m)) shot = "m5_day30";
    }

    private static void newDay(int day) {
        if (lastDay > 0) closeDay(lastDay);
        lastDay = day;
        chores.clear();
        warmMin = 100;
        kcalAte = 0;
        litToday = 0;
    }

    private static void closeDay(int day) {
        ServerPlayer p = null;
        // called from the tick that first sees the new day; the player is the one in step()
        var players = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        p = players.get(0);
        BodyState s = BodyAttachments.of(p);
        double in = 0, outK = 0;
        for (double v : s.kcalIn) in += v;
        for (double v : s.kcalOut) outK += v;
        double hands = 0, wait = 0;
        for (var e : chores.entrySet()) { if (e.getKey().endsWith("_wait")) wait += e.getValue(); else hands += e.getValue(); }
        double waking = day == 1 ? 600 : 960;
        double bmi = s.bmi();
        if (bmi < minBmi) { minBmi = bmi; minBmiDay = day; }
        if (bmi <= 19 && firstLow < 0) { firstLow = day; failures.add("BMI " + fmt(bmi) + " on day " + day); }
        if (hands + wait > 0.7 * waking) failures.add("chores " + fmt((hands + wait) / waking * 100) + "% of waking time on day " + day);
        log(String.format(java.util.Locale.ROOT,
                "DAY %2d kcal_in %.0f kcal_out %.0f net %.0f kg %.2f bmi %.2f warmth_min %.1f water %.0f energy %.1f cond %.0f | water_L %.1f ate bread %d forage %d snare %d fish %d | chores hands %.0f wait %.0f of %.0f waking (%.0f%% / %.0f%%) %s",
                day, in, outK, in - outK, s.kg, bmi, warmMin, s.water, s.energy, s.condition(), litres, breadEaten, forageItems, snareCatches, fishCaught,
                hands, wait, waking, hands / waking * 100, (hands + wait) / waking * 100, chores));
        litres = 0;
    }

    // ---- chores ------------------------------------------------------------------------------------------------

    private static void lightFire(ServerPlayer p, ServerLevel level, long m) {
        chore("fire", 18); // cut and carry the day's wood, lay the fire
        if (ring == null || ring.isRemoved()) { log("[d" + lastDay + "] no fire ring"); return; }
        if (ring.fire().state().name().equals("FIRE") || ring.fire().state().name().equals("COALS")) { log("[d" + lastDay + "] fire still alive: " + ring.fire().state()); return; }
        var t = com.yearofice.fire.FireConfig.tuning();
        ring.addPart(new ItemStack(FireRegistry.TINDER_BUNDLE.get()), t, p);
        ring.addPart(new ItemStack(FireRegistry.KINDLING.get()), t, p);
        ring.addPart(new ItemStack(FireRegistry.KINDLING.get()), t, p);
        for (int i = 0; i < 4; i++) ring.addPart(new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG), t, p);
        boolean lit;
        String how;
        ItemStack match = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(FireRegistry.MATCH.get())) { match = st; break; }
        }
        if (!match.isEmpty()) {
            lit = ring.strikeMatch(p, match, 0.0).ok();
            how = "match";
            chore("fire", 3);
        } else {
            // the bow drill from the kit's cord: effort and time as a middling set, then the ember catches (RECONCILE: stand-in until the minigame drives)
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.yearofice.api.fire.FireEffortEvent(p, "bow_drill", 60, 8));
            lit = ring.emberCaught(p).ok();
            how = "bow drill";
            chore("fire", 8);
        }
        ring.puff();
        ring.puff();
        log("[d" + lastDay + " " + clock(m) + "] fire lit by " + how + ": " + (lit ? "yes" : "NO") + " " + ring.fire().state());
    }

    private static void boilAndDrink(ServerPlayer p, ServerLevel level, double litresBoiled) {
        double min = WaterPurity.minutesToSafe(WaterPurity.Heat.STEADY_FIRE, litresBoiled);
        chore("water", 4);
        chore("water_wait", min);
        BodyService.get().drink(p, 0.5, WaterPurity.Source.CREEK, true);
        litres += 0.5;
    }

    private static void forage(ServerPlayer p, ServerLevel level) {
        chore("food", 10); // walk out and back (patches within 50 m)
        int got = 0;
        for (BlockPos q : plants) {
            if (!(level.getBlockState(q).getBlock() instanceof PlantBlock)) continue;
            var r = HarvestVerbs.act(p, level, q, HarvestVerbs.verbFor(ItemStack.EMPTY, false));
            if (r.ok() && !r.stack().isEmpty()) {
                give(p, r.stack().copy());
                foodTally.merge("forage_" + BuiltInRegistries.ITEM.getKey(r.stack().getItem()).getPath(), r.stack().getCount(), Integer::sum);
                got++;
                chore("food", 1.5);
            }
        }
        forageItems += got;
        if (got == 0) { scanPlants(p, level); }
    }

    private static void rebait(ServerPlayer p, ServerLevel level) {
        if (linePos == null) return;
        var t = com.yearofice.hunt.fishing.FishingConfig.tuning();
        if (wormSpots.isEmpty()) {
            BlockPos c = linePos;
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-40, -12, -40), c.offset(40, 12, 40))) {
                if (!level.getBlockState(q).is(net.minecraft.tags.BlockTags.DIRT) || !level.getBlockState(q.above()).isAir()) continue;
                boolean wet = false;
                int r = t.wormDigRange();
                for (BlockPos w : BlockPos.betweenClosed(q.offset(-r, -2, -r), q.offset(r, 1, r))) if (level.getFluidState(w).is(net.minecraft.tags.FluidTags.WATER)) { wet = true; break; }
                if (wet) wormSpots.add(q.immutable());
            }
            Map<String, Integer> tops = new LinkedHashMap<>();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-40, -12, -40), c.offset(40, 12, 40))) {
                if (level.getBlockState(q.above()).isAir() && !level.getBlockState(q).isAir())
                    tops.merge(BuiltInRegistries.BLOCK.getKey(level.getBlockState(q).getBlock()).toString(), 1, Integer::sum);
            }
            log("ground blocks near the set line: " + tops);
            log("riparian soil for worms within 40 m of the set line: " + wormSpots.size());
        }
        int worms = 0, tries = 0;
        while (worms < 3 && tries < 14 && !wormSpots.isEmpty()) {
            BlockPos q = wormSpots.get(wormCursor++ % wormSpots.size());
            tries++;
            chore("fish", 2.5);
            if (tries == 1) chore("fish", 20); // the walk to the spot and back
            worms += com.yearofice.hunt.fishing.WormDigging.wormsAt(level, q, level.getBlockState(q), t);
        }
        var g = PassiveGearService.at(level, linePos.above());
        if (g == null) g = PassiveGearService.at(level, linePos);
        log("  [d" + lastDay + "] worms dug " + worms + " in " + tries + " tries");
        if (g != null && worms > 0) { g.bait = "worm"; chore("fish", 3); }
    }

    private static void checkTraps(ServerPlayer p, ServerLevel level, long m) {
        for (BlockPos sp : snares) {
            if (!(level.getBlockEntity(sp) instanceof SnareBlockEntity be)) continue;
            chore("snare", 8);
            CarcassRecord r = be.collect();
            if (r != null) {
                snareCatches++;
                chore("snare", Butchering.minutes(r));
                for (ItemStack part : Butchering.parts(level, r)) {
                    foodTally.merge(BuiltInRegistries.ITEM.getKey(part.getItem()).getPath(), part.getCount(), Integer::sum);
                    give(p, part);
                }
                log("[d" + lastDay + " " + clock(m) + "] SNARE CATCH " + r.species() + " at " + sp.toShortString());
                if (level.getBlockEntity(sp) instanceof SnareBlockEntity b2 && b2.broken()) { b2.set(level); chore("snare", 10); }
            }
        }
        if (linePos != null) {
            var g = PassiveGearService.at(level, linePos.above());
            if (g == null) g = PassiveGearService.at(level, linePos);
            if (g != null) {
                chore("fish", 8);
                var got = PassiveGearService.check(level, p, g, FishBaitsClock.minute(level));
                fishCaught += got.size();
                for (ItemStack f : got) foodTally.merge(BuiltInRegistries.ITEM.getKey(f.getItem()).getPath(), 1, Integer::sum);
                if (!got.isEmpty()) log("[d" + lastDay + " " + clock(m) + "] SET LINE " + got.size() + " fish");
            }
        }
    }

    /** Meat and fish in the pack: half goes on the drying rack when it is free, the rest on the spit over the fire. */
    private static void cookEvening(ServerPlayer p, ServerLevel level) {
        pickUpDrops(p, level);
        if (ring == null || ring.isRemoved()) return;
        List<ItemStack> raw = new ArrayList<>();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            String id = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
            if (!st.isEmpty() && (id.endsWith("_meat") || isFish(st))
                    && st.get(com.yearofice.item.component.YoiComponents.DONENESS.get()) == null) raw.add(st);
        }
        log("  [d" + lastDay + "] cookEvening: raw stacks " + raw.size() + (raw.isEmpty() ? "" : " first " + BuiltInRegistries.ITEM.getKey(raw.get(0).getItem()) + " x" + raw.get(0).getCount()));
        for (ItemStack st : raw) {
            if (rackPos != null && level.getBlockEntity(rackPos) instanceof RackBlockEntity rack && !rack.done() && rack.progressH() == 0 && st.getCount() >= 4) {
                ItemStack half = st.split(st.getCount() / 2);
                int n = rack.putOn(half);
                log("  rack putOn -> " + n);
                if (n > 0) { chore("food", 3); log("[d" + lastDay + "] " + n + " strips on the drying rack"); }
                if (!half.isEmpty()) st.grow(half.getCount());
            }
            if (st.isEmpty()) continue;
            if (!(level.getBlockEntity(ringPos.above()) instanceof SpitBlockEntity sp) || sp.empty()) {
                ItemStack prevHand = p.getMainHandItem().copy();
                int slot = p.getInventory().selected;
                p.getInventory().setItem(slot, st.copy());
                st.setCount(0);
                boolean placed = SpitPlacing.tryPlace(level, ringPos, p, InteractionHand.MAIN_HAND);
                log("  spit placed: " + placed);
                ItemStack left = p.getInventory().getItem(slot);
                p.getInventory().setItem(slot, prevHand);
                if (!left.isEmpty() && !p.getInventory().add(left)) p.drop(left, false);
                if (placed) {
                    chore("food", 3);
                    if (level.getBlockEntity(ringPos.above()) instanceof SpitBlockEntity sb && sb.spit() != null) chore("food_wait", sb.spit().cookMinutes());
                    pendingSpit = true;
                }
            }
        }
    }

    static boolean isFish(ItemStack st) {
        for (var d : FishingRegistry.FISH.values()) if (st.is(d.get())) return true;
        return false;
    }

    private static boolean pendingSpit = false;

    private static void rackCheck(ServerPlayer p, ServerLevel level) {
        if (rackPos != null && level.getBlockEntity(rackPos) instanceof RackBlockEntity rack && rack.done()) {
            for (ItemStack st : rack.takeAll()) if (!p.getInventory().add(st)) p.drop(st, false);
            chore("food", 3);
            log("[d" + lastDay + "] took the dried batch off the rack");
        }
    }

    /** One meal: cooked, dried and foraged food first, pilot bread to fill the gap against what the day burns. */
    private static void meal(ServerPlayer p, ServerLevel level, String name) {
        pickUpDrops(p, level);
        BodyState s = BodyAttachments.of(p);
        if (level.getBlockEntity(ringPos.above()) instanceof SpitBlockEntity sp && !sp.empty()) {
            log("  spit ring " + sp.ring());
            if (sp.ring() == com.yearofice.sim.food.Doneness.Ring.DONE || sp.ring() == com.yearofice.sim.food.Doneness.Ring.RARE) {
                ItemStack off = sp.takeOff();
                if (!off.isEmpty() && !p.getInventory().add(off)) p.drop(off, false);
                chore("food", 2);
            }
        }
        int bites = 0, greens = 0;
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < p.getInventory().getContainerSize() && bites < 40; i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (st.isEmpty()) continue;
                String id = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
                boolean bread = id.equals("pilot_bread");
                boolean prepared = st.get(com.yearofice.item.component.YoiComponents.DONENESS.get()) != null || st.has(com.yearofice.item.component.YoiComponents.NUTRIENTS.get());
                if (pass == 0 && (bread || !prepared || !isEatable(level, st, id))) continue;
                if (pass == 1 && (bread || prepared || !isEatable(level, st, id))) continue;
                if (pass == 2 && !bread) continue;
                while (!st.isEmpty() && bites < 40) {
                    if (pass == 1 && greens >= 10) break;
                    if (pass == 2 && s.balance24h() + s.pendingKcal >= 0) break;
                    var b = Meals.eat(p, st);
                    if (!b.eaten()) { if (pass == 2) log("  bread refused: " + b.outcome() + " stomach " + fmt(s.stomachKg)); break; }
                    bites++;
                    if (pass == 1) greens++;
                    if (bread) breadEaten++;
                    else foodTally.merge("ate_" + id, 1, Integer::sum);
                }
            }
        }
        { StringBuilder sb = new StringBuilder(); for (int i = 0; i < p.getInventory().getContainerSize(); i++) { ItemStack q = p.getInventory().getItem(i); if (!q.isEmpty()) sb.append(BuiltInRegistries.ITEM.getKey(q.getItem()).getPath()).append('x').append(q.getCount()).append(' '); }
          log("  after meal: bites " + bites + " greens " + greens + " inv " + sb); }
        chore("food", 4 + bites * 0.3);
    }

    private static boolean isEatable(ServerLevel level, ItemStack st, String id) {
        if (!Meals.edible(level, st)) return false;
        boolean flesh = id.endsWith("_meat") || isFish(st);
        return !flesh || st.get(com.yearofice.item.component.YoiComponents.DONENESS.get()) != null || st.has(com.yearofice.item.component.YoiComponents.NUTRIENTS.get());
    }

    private static void finish(String failure) {
        if (!failure.isEmpty()) failures.add(failure.replaceFirst("^FAIL ", ""));
        if (lastDay > 0 && lastDay <= DAYS && failure.isEmpty()) closeDay(lastDay);
        verdict = failures.isEmpty() ? "PASS" : "FAIL " + failures;
        log("RESULT " + verdict + " | min BMI " + fmt(minBmi) + " on day " + minBmiDay + " | food tally " + foodTally);
        out.close();
        done = true;
    }
}
