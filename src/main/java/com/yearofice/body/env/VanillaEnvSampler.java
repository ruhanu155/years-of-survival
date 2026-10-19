package com.yearofice.body.env;

import com.yearofice.body.clock.BodyClock;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The basic environment sample of the slice, read from vanilla until the Environment field exists:
 *
 * <ul>
 *   <li>Air: the biome's base temperature as °C = 28·t − 8 (plains 0.8 → 14 °C, taiga 0.25 → −1 °C, snowy plains
 *       0 → −8 °C), −6.5 °C a km above y 64, and a daily swing of ±5 °C peaking at 15:00.</li>
 *   <li>Precipitation: rain or snow where vanilla rains on the player's head, 2.5 mm/h (8 in a thunderstorm).</li>
 *   <li>Wind: 2 m/s, 5 in rain, 9 in a thunderstorm; halved under a roof.</li>
 *   <li>Water: air − 4 °C, between 1 and 20, and the depth the player stands in.</li>
 *   <li>Heat: lit campfires (Steady), fire blocks (Low) and lava (Hot) within 8 m.</li>
 * </ul>
 *
 * A per-player override, set by {@code /yoi body env} and by GameTests, replaces the sampled air, wind and
 * precipitation.
 */
public final class VanillaEnvSampler implements EnvSampler {

    /** A debug or test override; {@code waterC} NaN keeps the derived value. */
    public record EnvOverride(double airC, double windMs, Precip precip, double precipMmH, double waterC) {}

    private final Map<UUID, EnvOverride> overrides = new ConcurrentHashMap<>();

    /** Under a roof: no rain, half the wind, no warmth bonus (Shelter states come with the Build system). */
    public static final ShelterState ROOF = new ShelterState("roof", 0, 0.5, 0);

    public void override(ServerPlayer p, EnvOverride o) {
        if (o == null) overrides.remove(p.getUUID());
        else overrides.put(p.getUUID(), o);
    }

    public EnvOverride overrideOf(ServerPlayer p) { return overrides.get(p.getUUID()); }

    public static double biomeAirC(float baseTemperature, int y) {
        double c = 28 * baseTemperature - 8;
        if (y > 64) c -= (y - 64) * 0.0065;
        return c;
    }

    public static double diurnalC(long minute) {
        double phase = (BodyClock.minuteOfDay(minute) - 15 * 60) / 1440.0 * 2 * Math.PI;
        return 5 * Math.cos(phase);
    }

    @Override
    public Sample sample(ServerPlayer p, long minute) {
        ServerLevel level = p.serverLevel();
        BlockPos feet = p.blockPosition();
        BlockPos head = BlockPos.containing(p.getEyePosition());
        Biome biome = level.getBiome(feet).value();

        boolean roofed = !level.canSeeSky(head.above());
        double air = biomeAirC(biome.getBaseTemperature(), feet.getY()) + diurnalC(minute);
        Precip precip = Precip.NONE;
        double mmH = 0;
        if (level.isRainingAt(head.above()) || (level.isRaining() && !roofed && biome.coldEnoughToSnow(feet))) {
            precip = biome.coldEnoughToSnow(feet) || air < 0 ? Precip.SNOW : Precip.RAIN;
            mmH = level.isThundering() ? 8 : 2.5;
        }
        double wind = level.isThundering() ? 9 : (level.isRaining() ? 5 : 2);
        double waterC = Math.max(1, Math.min(20, air - 4));

        EnvOverride o = overrides.get(p.getUUID());
        if (o != null) {
            air = o.airC();
            wind = o.windMs();
            precip = o.precip();
            mmH = o.precipMmH();
            if (!Double.isNaN(o.waterC())) waterC = o.waterC();
        }

        List<HeatSource> heat = heatSources(level, p);
        EnvSample env = EnvSample.outdoors(air, wind).withPrecip(precip, mmH).withWater(waterC);
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
        List<HeatSource> out = new ArrayList<>(2);
        Vec3 at = p.position().add(0, 0.9, 0);
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
