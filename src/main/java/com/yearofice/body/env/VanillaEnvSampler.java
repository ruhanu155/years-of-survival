package com.yearofice.body.env;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.sim.body.ColdWater;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.env.HeatSource;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.env.ShelterState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The body's environment sample, from the Environment field ({@link EnvService}, Layer 2), at the player's feet:
 *
 * <ul>
 *   <li>Air, wind, precipitation and water: {@link EnvService#sample} now, or {@link EnvService#sampleAbstract} for
 *       another minute (a scripted day plan's start). The water is the field's creek water.</li>
 *   <li>Under a roof (no sky above the head) no precipitation reaches the player; the {@link #ROOF} shelter halves
 *       the wind chill.</li>
 *   <li>Heat: every fire ring the fire system reports within 8 m ({@link FireHeatLookup}), plus vanilla lit
 *       campfires (Steady), fire blocks (Low) and lava (Hot), which have no fire system of their own.</li>
 * </ul>
 *
 * A per-player override, set by {@code /yoi body env} and by GameTests, replaces the sampled air, wind and
 * precipitation for that player alone, so a test never moves another test's weather.
 *
 * <p>The name is historical (it read the biome until M2 lane B); other packages' tests hold it, so it stays.
 */
public final class VanillaEnvSampler implements EnvSampler {

    /** A debug or test override; {@code waterC} NaN keeps the field's water. */
    public record EnvOverride(double airC, double windMs, Precip precip, double precipMmH, double waterC) {}

    private final Map<UUID, EnvOverride> overrides = new ConcurrentHashMap<>();

    /** Under a roof: no rain, half the wind, no warmth bonus (Shelter states come with the Build system). */
    public static final ShelterState ROOF = new ShelterState("roof", 0, 0.5, 0);

    public void override(ServerPlayer p, EnvOverride o) {
        if (o == null) overrides.remove(p.getUUID());
        else overrides.put(p.getUUID(), o);
    }

    public EnvOverride overrideOf(ServerPlayer p) { return overrides.get(p.getUUID()); }

    /** The field's sample at the player's feet at {@code minute}: the live sample within a minute of now. */
    public static EnvSample field(ServerPlayer p, long minute) {
        ServerLevel level = p.serverLevel();
        EnvService env = EnvService.get();
        long now = ClockService.installed() ? ClockService.get().nowMinute() : minute;
        if (Math.abs(minute - now) <= 1) return env.sample(level, p.blockPosition());
        return env.sampleAbstract(level, p.getX(), p.getY(), p.getZ(), minute);
    }

    @Override
    public Sample sample(ServerPlayer p, long minute) {
        ServerLevel level = p.serverLevel();
        BlockPos head = BlockPos.containing(p.getEyePosition());
        boolean roofed = !level.canSeeSky(head.above());

        EnvOverride o = overrides.get(p.getUUID());
        EnvSample env;
        if (o != null) {
            double water = Double.isNaN(o.waterC()) ? field(p, minute).waterC() : o.waterC();
            env = EnvSample.outdoors(o.airC(), o.windMs()).withPrecip(o.precip(), o.precipMmH()).withWater(water);
        } else {
            env = field(p, minute);
            if (roofed) env = env.withPrecip(Precip.NONE, 0);
        }
        List<HeatSource> heat = heatSources(level, p);
        return new Sample(env, heat, roofed ? ROOF : ShelterState.NONE, depth(p));
    }

    /** How deep the player stands: by the water's height against the body, swimming once over 1.2 m. */
    public static ColdWater.Depth depth(ServerPlayer p) {
        if (p.isUnderWater() || p.isSwimming()) return ColdWater.Depth.SWIM;
        double h = p.getFluidHeight(FluidTags.WATER);
        if (h <= 0) return ColdWater.Depth.NONE;
        if (h < 0.3) return ColdWater.Depth.ANKLE;
        if (h < 0.6) return ColdWater.Depth.KNEE;
        if (h < 1.2) return ColdWater.Depth.THIGH;
        return ColdWater.Depth.SWIM;
    }

    private static final int R = 8, RY = 3;

    static List<HeatSource> heatSources(ServerLevel level, ServerPlayer p) {
        Vec3 at = p.position().add(0, 0.9, 0);
        List<HeatSource> out = new ArrayList<>(FireHeatLookup.INSTANCE.sourcesNear(level, at.x, at.y, at.z));
        BlockPos c = p.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                for (int dy = -RY; dy <= RY; dy++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState s = level.getBlockState(m);
                    FireSetting f = fireOf(s);
                    if (f == null) continue;
                    double d = Vec3.atCenterOf(m).distanceTo(at);
                    if (d <= R) out.add(HeatSource.openFire(f, d));
                }
            }
        }
        return out;
    }

    static FireSetting fireOf(BlockState s) {
        if (s.getBlock() instanceof CampfireBlock) return s.getValue(CampfireBlock.LIT) ? FireSetting.STEADY : null;
        if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) return FireSetting.LOW;
        if (s.is(Blocks.LAVA)) return FireSetting.HOT;
        return null;
    }
}
