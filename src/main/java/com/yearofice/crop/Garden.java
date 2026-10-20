package com.yearofice.crop;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.plant.PlantDials;
import com.yearofice.sim.crop.CropModel;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.WeatherModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** The garden's rules on the world: tilling classes, sowing, the daily growth and frost pass, and harvest (Crop farming in play). */
public final class Garden {
    private Garden() {}

    public record Sow(int seeds, int up, double p, double soilC, boolean inWindow) {}

    /** The soil class a block tills into, or null when it cannot be tilled (bog, frozen soil, bedrock). */
    public static @Nullable GardenBedBlock.Soil soilOf(BlockState st) {
        var key = BuiltInRegistries.BLOCK.getKey(st.getBlock());
        if (!key.getNamespace().equals("yearofice")) return null;
        return switch (key.getPath()) {
            case "silt" -> GardenBedBlock.Soil.SILT;
            case "loess", "grass_turf", "tundra_turf" -> GardenBedBlock.Soil.LOAM;
            case "forest_duff", "moss_mat" -> GardenBedBlock.Soil.PODZOL;
            case "gravel", "river_sand", "till" -> GardenBedBlock.Soil.GRAVEL;
            default -> null;
        };
    }

    /** Days in the soil's running mean of the air. */
    public static final int SOIL_DAYS = 7;

    /**
     * Soil temperature: the 7-day running mean of the air (the six finished days and today's air at the bed) plus a
     * tilled bare bed's +1 C. A pinned air ({@code /yoi env set}, GameTests) holds for the whole week.
     */
    public static double soilC(ServerLevel level, BlockPos pos) {
        double now = EnvService.get().airC(level, pos);
        var ov = EnvService.get().override(level);
        double[] means = new double[SOIL_DAYS];
        means[0] = now;
        long today = ClockService.get().absDay();
        boolean pinned = ov != null && !Double.isNaN(ov.airC());
        for (int i = 1; i < SOIL_DAYS; i++) {
            means[i] = pinned || today - i < 0 ? now : EnvService.get().day(level, today - i).meanC();
        }
        return CropModel.soilMeanC(means, 1.0);
    }

    /** Sows one block on a bed: rolls each seed once and places the crop. {@code soilOverrideC} NaN reads the air. */
    public static @Nullable Sow sow(ServerLevel level, BlockPos bed, CropSpec spec, double soilOverrideC) {
        return sow(level, bed, spec, soilOverrideC, 0);
    }

    /** As {@link #sow(ServerLevel, BlockPos, CropSpec, double)} with seed that is {@code yearsOld} years old. */
    public static @Nullable Sow sow(ServerLevel level, BlockPos bed, CropSpec spec, double soilOverrideC, int yearsOld) {
        BlockState bs = level.getBlockState(bed);
        if (!(bs.getBlock() instanceof GardenBedBlock) || !level.getBlockState(bed.above()).isAir()) return null;
        double soilC = Double.isNaN(soilOverrideC) ? soilC(level, bed) : soilOverrideC;
        double p = CropModel.germination(spec.baseGermination, soilC, bs.getValue(GardenBedBlock.MOISTURE), CropModel.Depth.RIGHT, yearsOld, false,
                GardenConfig.tuning().germinationScale());
        int up = 0;
        for (int i = 0; i < spec.seedsPerBlock; i++) if (level.random.nextDouble() < p) up++;
        level.setBlock(bed.above(), CropRegistry.cropBlock(spec.id).defaultBlockState(), 3);
        int today = (int) ClockService.get().absDay();
        if (level.getBlockEntity(bed.above()) instanceof CropBlockEntity be) be.sow(today, spec.seedsPerBlock, up, soilC);
        int doy = ClockService.get().dayOfYear();
        return new Sow(spec.seedsPerBlock, up, p, soilC, CropModel.inWindow(doy, spec.windowFromDoy, spec.windowToDoy));
    }

    /** Dry-run: the share of {@code n} seeds that come up on this bed (changes nothing). */
    public static double dryRun(ServerLevel level, BlockPos bed, CropSpec spec, int n, double soilOverrideC) {
        BlockState bs = level.getBlockState(bed);
        double soilC = Double.isNaN(soilOverrideC) ? soilC(level, bed) : soilOverrideC;
        int moisture = bs.getBlock() instanceof GardenBedBlock ? bs.getValue(GardenBedBlock.MOISTURE) : 1;
        double p = CropModel.germination(spec.baseGermination, soilC, moisture, CropModel.Depth.RIGHT, 0, false, GardenConfig.tuning().germinationScale());
        int up = 0;
        for (int i = 0; i < n; i++) if (level.random.nextDouble() < p) up++;
        return n == 0 ? 0 : (double) up / n;
    }

    /** The crouch card on a bed. */
    public static String card(ServerLevel level, BlockPos bed, @Nullable CropSpec seed) {
        BlockState bs = level.getBlockState(bed);
        double soilC = soilC(level, bed);
        StringBuilder sb = new StringBuilder("Bed: ").append(bs.getValue(GardenBedBlock.SOIL).getSerializedName()).append(", soil ")
                .append(Math.round(soilC)).append(" C, moisture ").append(bs.getValue(GardenBedBlock.MOISTURE)).append(", weeds ")
                .append(bs.getValue(GardenBedBlock.WEEDS));
        if (seed != null) {
            double p = CropModel.germination(seed.baseGermination, soilC, bs.getValue(GardenBedBlock.MOISTURE), CropModel.Depth.RIGHT, 0, false,
                    GardenConfig.tuning().germinationScale());
            sb.append(". ").append(seed.name()).append(" here: about ").append(Math.round(p * 100)).append("%");
            if (!CropModel.inWindow(ClockService.get().dayOfYear(), seed.windowFromDoy, seed.windowToDoy)) sb.append(" (unlikely to ripen before frost)");
        }
        if (level.getBlockEntity(bed.above()) instanceof CropBlockEntity be && be.spec() != null) {
            CropSpec s = be.spec();
            sb.append(". ").append(s.name()).append(" stage ").append(be.stage()).append(", ").append(Math.round(be.units())).append(" of ")
                    .append(Math.round(s.ripeUnits)).append(" units, ").append(be.up()).append(" of ").append(be.seeds()).append(" up");
        }
        return sb.append('.').toString();
    }

    /** One night at {@code lowC} over a crop: the hard freeze, seedling frost, potato leaf frost and head frost. */
    public static void nightFrost(CropBlockEntity be, CropSpec s, double lowC) {
        if (be.dead()) return;
        if (CropModel.hardFreeze(lowC, s.hardFreezeC)) { be.kill(); return; }
        boolean seedling = be.units() < s.sproutUnits + (s.ripeUnits - s.sproutUnits) * 0.45;
        if (seedling && lowC <= s.seedlingMinC - (s.seedlingMinC == s.headMinC ? 0 : 0)) {
            if (s.seedlingMinC == s.headMinC && s.headYieldMult == 1.0 && s.id.endsWith("potato")) be.markFrosted(true);
            else be.kill();
            return;
        }
        if (s.id.endsWith("potato") && lowC <= s.headMinC) { be.markFrosted(true); return; }
        be.multFrost(CropModel.headFrostMult(lowC, be.units(), s.sensFrom, s.sensTo, s.headMinC, s.headYieldMult));
    }

    /** One finished day over a crop and its bed: rain and moisture, dry days, growth units and the night's frost. */
    public static void dayTick(ServerLevel level, BlockPos cropPos, CropBlockEntity be, long endedDay) {
        if (endedDay <= be.lastDone()) return;
        be.setLastDone((int) endedDay);
        CropSpec s = be.spec();
        if (s == null || be.dead()) return;
        WeatherModel.Day day = EnvService.get().day(level, endedDay);
        int doy = Math.floorMod((int) endedDay, 365);
        int month = Calendar.month(doy);
        BlockPos bedPos = cropPos.below();
        BlockState bed = level.getBlockState(bedPos);
        boolean wet = day.precipMm() >= 1.0 && day.meanC() > 0;
        int moisture = bed.getBlock() instanceof GardenBedBlock ? bed.getValue(GardenBedBlock.MOISTURE) : 1;
        if (day.precipMm() >= 5.0 && day.meanC() > 0) moisture = 3;
        else if (moisture == 3) moisture = 2;
        else if (!wet && day.meanC() + day.swingC() / 2 > 15) moisture = Math.max(0, moisture - 1);
        if (bed.getBlock() instanceof GardenBedBlock && bed.getValue(GardenBedBlock.MOISTURE) != moisture) {
            level.setBlock(bedPos, bed.setValue(GardenBedBlock.MOISTURE, moisture), 3);
        }
        be.setDry(wet || moisture >= 2 ? 0 : be.dryDays() + 1);
        double low = day.meanC() - day.swingC() / 2.0 + GardenConfig.frostOffsetC();
        if (be.dryDays() < 7 && !be.frosted()) be.addUnits(CropModel.growthUnits(day.meanC(), month, PlantDials.warmth));
        nightFrost(be, s, low);
    }

    /** Most days a returning player's garden catches up at once (a year). */
    public static final int CATCH_UP_MAX_DAYS = 365;

    /**
     * The garden's catch-up: runs every finished day since the crop was last worked through the same daily pass, from the
     * drawn weather of those days, so a garden left alone grows, dries and freezes as if watched. Returns the days run.
     */
    public static int catchUp(ServerLevel level, BlockPos cropPos, CropBlockEntity be) {
        long today = ClockService.get().absDay();
        long from = Math.max(be.lastDone() + 1L, today - CATCH_UP_MAX_DAYS);
        int n = 0;
        for (long d = from; d <= today - 1; d++) {
            dayTick(level, cropPos, be, d);
            n++;
        }
        return n;
    }

    /** Expected grams of the main harvest at the crop's present numbers. */
    public static int yieldGrams(CropBlockEntity be, CropSpec s, double soilYield) {
        double f = s.grain() ? CropModel.standYield(be.stand(), s.baseGermination) : (be.up() > 0 ? 1.0 : 0.0);
        return (int) Math.round(s.kgPerBlock * 1000.0 * f * be.frostMult() * GardenConfig.tuning().yieldScale() * soilYield);
    }

    /** Harvests a ripe crop: gives the yield (and straw) and clears the block. Returns the main stack, or empty when not ripe. */
    public static ItemStack harvest(ServerLevel level, BlockPos cropPos, @Nullable ServerPlayer p) {
        if (!(level.getBlockEntity(cropPos) instanceof CropBlockEntity be) || be.spec() == null) return ItemStack.EMPTY;
        CropSpec s = be.spec();
        if (be.stage() < 4 || be.dead()) return ItemStack.EMPTY;
        BlockState bed = level.getBlockState(cropPos.below());
        double soilYield = bed.getBlock() instanceof GardenBedBlock ? bed.getValue(GardenBedBlock.SOIL).yield : 1.0;
        int g = yieldGrams(be, s, soilYield);
        ItemStack out = new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(s.yieldItem)));
        out.set(YoiComponents.WEIGHT_G.get(), Math.max(1, g));
        if (s.grain()) out.set(CropRegistry.SEED_LOT.get(), ClockService.get().year());
        level.setBlock(cropPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        if (p != null && g > 0 && !p.getInventory().add(out.copy())) p.drop(out.copy(), false);
        if (p != null && s.strawKg > 0) {
            ItemStack straw = new ItemStack(CropRegistry.STRAW.get());
            straw.set(YoiComponents.WEIGHT_G.get(), (int) Math.round(s.strawKg * 1000 * (s.grain() ? CropModel.standYield(be.stand(), s.baseGermination) : 1)));
            if (!p.getInventory().add(straw)) p.drop(straw, false);
        }
        return out;
    }
}
