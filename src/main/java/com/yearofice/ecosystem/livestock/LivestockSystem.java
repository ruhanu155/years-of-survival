package com.yearofice.ecosystem.livestock;

import com.yearofice.api.build.Room;
import com.yearofice.api.build.RoomService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.crop.CropRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.food.FoodItems;
import com.yearofice.sim.livestock.Care;
import com.yearofice.sim.livestock.Growth;
import com.yearofice.sim.livestock.LivestockTuning;
import com.yearofice.sim.livestock.PenRisk;
import com.yearofice.sim.livestock.Produce;
import com.yearofice.sim.livestock.Spec;
import com.yearofice.sim.time.Calendar;
import java.util.List;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Livestock at runtime (Raising animals): the hourly care step with catch-up after a reload, the daily growth, milk,
 * eggs and births, the night-raid roll on a pen, and the hand interactions. The numbers are in {@code sim.livestock}.
 */
public final class LivestockSystem {
    private LivestockSystem() {}

    /** Longest catch-up on a reload: 30 game days of hours. */
    public static final int MAX_CATCH_UP_HOURS = 720;
    /** Share of the daily ration an animal finds for itself in May to September. */
    private static double browseShare(String id, int month) {
        if (month < 5 || month > 9) return 0;
        return switch (id) { case "hen" -> 0.7; case "reindeer" -> 0.6; default -> 0.5; };
    }
    private static final ResourceLocation MASS = ResourceLocation.fromNamespaceAndPath("yearofice", "livestock_mass");

    public static LivestockTuning tuning() { return LivestockConfig.tuning(); }

    // ---- the tick ------------------------------------------------------------------------------------------------

    public static void tick(YoiAnimal e, LivestockCore c) {
        if (!(e.level() instanceof ServerLevel level) || e.tickCount % 20 != 7) return;
        long nowHour = ClockService.get().nowMinute() / 60;
        if (c.lastHour == Long.MIN_VALUE) c.lastHour = nowHour;
        long behind = nowHour - c.lastHour;
        if (behind <= 0) { applyScale(e, c); return; }
        long from = Math.max(c.lastHour, nowHour - MAX_CATCH_UP_HOURS);
        for (long h = from + 1; h <= nowHour; h++) {
            hour(e, level, c, h, h == nowHour);
            if (e.isRemoved()) return;
        }
        c.lastHour = nowHour;
        applyScale(e, c);
    }

    private static void applyScale(YoiAnimal e, LivestockCore c) {
        var inst = e.getAttribute(Attributes.SCALE);
        if (inst == null) return;
        double s = Math.max(0.3, Math.sqrt(c.massKg() / c.spec().adultKg()));
        double cur = inst.getModifier(MASS) == null ? 0 : inst.getModifier(MASS).amount();
        if (Math.abs(cur - (s - 1)) < 0.02) return;
        inst.removeModifier(MASS);
        inst.addPermanentModifier(new AttributeModifier(MASS, s - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    public static void hour(YoiAnimal e, ServerLevel level, LivestockCore c, long absHour, boolean live) {
        long minute = absHour * 60;
        long day = Calendar.absDayOfMinute(minute);
        int doy = Calendar.dayOfYearOfDay(day);
        int month = Calendar.month(doy);
        int hourOfDay = (int) (absHour % 24);
        LivestockTuning t = tuning();
        String id = e.speciesPath();
        double roomC = tempC(e, level, minute, live);
        if (live && hourOfDay % 3 == 0) scanPen(e, level, c);
        c.stepHour(t, browseShare(id, month), live ? waterNear(e, level) : c.waterL > 0, roomC);
        if (c.care.dead()) { DeathPipeline.kill(e, level.damageSources().generic(), "starved"); return; }
        if (hourOfDay == 3) nightRoll(e, level, c, day, t);
        if (e.isRemoved()) return;
        if (hourOfDay == 0) daily(e, level, c, day, month, doy, t);
    }

    private static double tempC(YoiAnimal e, ServerLevel level, long minute, boolean live) {
        BlockPos p = e.blockPosition();
        Room room = RoomService.get().roomAt(level, p);
        if (room != null && live) return room.feelsLikeC();
        if (live) return EnvService.get().airC(level, p);
        return EnvService.get().sampleAbstract(level, e.getX(), e.getY(), e.getZ(), minute).airC();
    }

    private static boolean waterNear(YoiAnimal e, ServerLevel level) {
        BlockPos p = e.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-4, -2, -4), p.offset(4, 1, 4))) {
            var s = level.getBlockState(q);
            if (s.is(Blocks.WATER) || s.is(Blocks.SNOW) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.SNOW_BLOCK)) return true;
        }
        return false;
    }

    private static void scanPen(YoiAnimal e, ServerLevel level, LivestockCore c) {
        Pens.Pen pen = Pens.scan(level, e.blockPosition());
        c.penned = pen.penned();
        c.fence = pen.fence();
    }

    // ---- the night: predators and escapes -----------------------------------------------------------------------

    /** A hash in [0, 1) of the day and the 16-block cell: every animal of a pen sees the same roll. */
    public static double cellRoll(long day, int cx, int cz, int salt) {
        long h = day * 0x9E3779B97F4A7C15L ^ (cx * 0xC2B2AE3D27D4EB4FL) ^ (cz * 0x165667B19E3779F9L) ^ salt;
        return new SplittableRandom(h).nextDouble();
    }

    /** True when the night of {@code day} brings a predator to this animal's pen (or to the open ground it stands on). */
    public static boolean raided(YoiAnimal e, LivestockCore c, long day, LivestockTuning t) {
        int cx = e.blockPosition().getX() >> 4, cz = e.blockPosition().getZ() >> 4;
        // Predators in the region: about three nights in ten.
        if (cellRoll(day, cx >> 3, cz >> 3, 1) >= 0.30) return false;
        boolean out = !c.penned || day == c.escapedDay;
        if (c.penned && cellRoll(day, cx, cz, 2) < PenRisk.escapeChance(c.fence, t)) { c.escapedDay = day; out = true; }
        PenRisk.Fence f = out ? PenRisk.Fence.NONE : c.fence;
        return cellRoll(day, cx, cz, 3) < PenRisk.attackChance(f, false, false, t);
    }

    public static void nightRoll(YoiAnimal e, ServerLevel level, LivestockCore c, long day, LivestockTuning t) {
        if (!raided(e, c, day, t)) return;
        // One animal of the cell is taken: the lowest id among those in reach.
        List<YoiAnimal> mates = level.getEntitiesOfClass(YoiAnimal.class, new AABB(e.blockPosition()).inflate(16),
                a -> a instanceof Livestock && a.isAlive() && java.util.Objects.equals(a.testGroup(), e.testGroup()));
        YoiAnimal victim = mates.stream().min(java.util.Comparator.comparing(YoiAnimal::getUUID)).orElse(e);
        if (victim == e) DeathPipeline.kill(e, level.damageSources().mobAttack(null), "predator");
    }

    // ---- the day -------------------------------------------------------------------------------------------------

    public static void daily(YoiAnimal e, ServerLevel level, LivestockCore c, long day, int month, int doy, LivestockTuning t) {
        Spec sp = c.spec();
        boolean suckling = c.stage() == Growth.Stage.SUCKLING;
        boolean milk = suckling && c.care.producing();
        c.stepDay(t, milk || !suckling);
        if (c.ageDays > sp.lifespanYears() * 365) { DeathPipeline.kill(e, level.damageSources().generic(), "old_age"); return; }
        if (!c.adult() || !c.female) return;
        String id = e.speciesPath();
        // Eggs and milk.
        if (id.equals("hen")) {
            double light = Math.max(8, Math.min(16, ClockService.get().daylightHours(day)));
            c.eggAcc += Produce.eggsPerDay(light, tempC(e, level, day * 1440, true), c.care.fed()) * 1.0;
            while (c.eggAcc >= 1) {
                c.eggAcc -= 1;
                drop(level, e, new ItemStack(FoodItems.HEN_EGG.get()));
            }
        } else if (sp.milkLPerDay() > 0) {
            c.milkOwedL = Math.min(sp.milkLPerDay() * 2, c.milkOwedL + c.milkToday(day, c.feedKg <= 0 && c.feedDebtKg > 0));
        }
        // Births and breeding.
        if (c.dueDay >= 0 && day >= c.dueDay) {
            c.dueDay = -1;
            c.lastBirthDay = day;
            spawnYoung(e, level, c);
        } else if (c.dueDay < 0 && Growth.inRut(sp, month) && c.care.producing() && hasMate(e, level)) {
            c.dueDay = dueDay(sp, id, day);
        }
    }

    private static long dueDay(Spec sp, String id, long day) {
        if (id.equals("hen")) return day + 21;
        long d = day + 140;
        while (Calendar.month(Calendar.dayOfYearOfDay(d)) != sp.birthMonth()) d++;
        return d;
    }

    private static boolean hasMate(YoiAnimal e, ServerLevel level) {
        return !level.getEntitiesOfClass(YoiAnimal.class, new AABB(e.blockPosition()).inflate(10),
                a -> a != e && a.isAlive() && a instanceof Livestock l && !l.core().female && l.core().adult()
                        && a.speciesId().equals(e.speciesId()) && java.util.Objects.equals(a.testGroup(), e.testGroup())).isEmpty();
    }

    private static void spawnYoung(YoiAnimal mother, ServerLevel level, LivestockCore c) {
        @SuppressWarnings("unchecked")
        EntityType<? extends YoiAnimal> type = (EntityType<? extends YoiAnimal>) mother.getType();
        YoiAnimal kid = type.create(level);
        if (kid == null) return;
        kid.moveTo(mother.getX() + 0.4, mother.getY(), mother.getZ(), mother.getYRot(), 0);
        kid.setTestGroup(mother.testGroup());
        if (kid instanceof Livestock l) {
            l.core().progress = 0;
            l.core().ageDays = 0;
            l.core().lastHour = c.lastHour;
        }
        level.addFreshEntity(kid);
    }

    private static void drop(ServerLevel level, YoiAnimal e, ItemStack s) {
        level.addFreshEntity(new ItemEntity(level, e.getX(), e.getY() + 0.3, e.getZ(), s));
    }

    // ---- hands ---------------------------------------------------------------------------------------------------

    public static boolean isFeed(ItemStack s) {
        return s.is(CropRegistry.STRAW.get()) || s.is(Items.WHEAT) || s.is(Items.HAY_BLOCK);
    }

    public static InteractionResult interact(YoiAnimal e, LivestockCore c, Player player, InteractionHand hand) {
        if (!(e.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        if (isFeed(held)) {
            c.feedKg += 0.5;
            if (!player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (held.is(Items.WATER_BUCKET)) {
            c.waterL += 5;
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            return InteractionResult.CONSUME;
        }
        if (held.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("yearofice", "knives"))) && player.isShiftKeyDown() && c.adult()) {
            DeathPipeline.kill(e, level.damageSources().playerAttack(player), "butcher");
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && c.female && c.spec().milkLPerDay() > 0 && c.milkOwedL >= 0.25) {
            c.milkOwedL -= 0.25;
            ItemStack milk = new ItemStack(FoodItems.GOAT_MILK.get());
            if (!player.addItem(milk)) player.drop(milk, false);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // ---- status --------------------------------------------------------------------------------------------------

    public static String status(YoiAnimal e, LivestockCore c) {
        return String.format(java.util.Locale.ROOT, "%s %s %s kg=%.1f age_d=%.0f fed=%.0f water=%.0f warm=%.0f cond=%.0f pen=%s feed_kg=%.1f debt_kg=%.1f milk_l=%.2f due=%d",
                e.speciesPath(), c.female ? "f" : "m", c.stage(), c.massKg(), c.ageDays, c.care.fed(), c.care.watered(), c.care.warm(),
                c.care.condition(), c.penned ? c.fence : "open", c.feedKg, c.feedDebtKg, c.milkOwedL, c.dueDay);
    }
}
