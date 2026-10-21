package com.yearofice.build.craft.water;

import com.yearofice.api.build.WheelService;
import com.yearofice.api.world.IceService;
import com.yearofice.build.metal.MetalConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The fish wheel's conditions, on top of {@code hunt.fishing}'s passive gear (which owns the block, the basket and the
 * catch through the spot stocks that {@code WaterLife} also reads): it turns only in a current (water below and on two
 * sides), never under ice (IceService), and gathers debris each hour it turns until the dial's limit, when it stops
 * until a player clears it. Debris lives in the SavedData {@code yearofice_wheel_debris}; this class is its only writer.
 */
public final class WheelWorks implements WheelService {
    /** Water below (or at) the wheel and on two sides: a wheel needs a current to turn. */
    public static boolean inCurrent(Level level, BlockPos pos) {
        if (!level.getFluidState(pos.below()).is(FluidTags.WATER) && !level.getFluidState(pos).is(FluidTags.WATER)) return false;
        int sides = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER) || level.getFluidState(pos.relative(d).below()).is(FluidTags.WATER)) sides++;
        }
        return sides >= 2;
    }

    static boolean iced(ServerLevel level, BlockPos pos) {
        try {
            IceService ice = IceService.get();
            return ice.thicknessCm(level, pos) > 0 || ice.thicknessCm(level, pos.below()) > 0;
        } catch (IllegalStateException notInstalled) {
            return false;
        }
    }

    public static Debris debris(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(Debris.FACTORY, "yearofice_wheel_debris");
    }

    public static double debrisAt(ServerLevel level, BlockPos pos) { return debris(level).at.getOrDefault(pos.asLong(), 0.0); }

    @Override
    public int turningHours(ServerLevel level, BlockPos pos, int hours) {
        if (hours <= 0 || iced(level, pos) || !inCurrent(level, pos)) return 0;
        double rate = MetalConfig.get("wheel_foul_per_h"), max = MetalConfig.get("wheel_foul_max");
        Debris book = debris(level);
        double d = book.at.getOrDefault(pos.asLong(), 0.0);
        int room = d >= max ? 0 : rate <= 0 ? hours : (int) Math.ceil((max - d) / rate);
        int turned = Math.min(hours, room);
        if (turned > 0 && rate > 0) {
            book.at.put(pos.asLong(), d + turned * rate);
            book.setDirty();
        }
        return turned;
    }

    /** Sets the debris at a wheel (GameTests and {@code /yoi}). */
    public static void setDebris(ServerLevel level, BlockPos pos, double d) {
        debris(level).at.put(pos.asLong(), d);
        debris(level).setDirty();
    }

    @Override
    public void clearDebris(ServerLevel level, BlockPos pos) {
        Debris book = debris(level);
        if (book.at.remove(pos.asLong()) != null) book.setDirty();
    }

    public static final class Debris extends SavedData {
        static final SavedData.Factory<Debris> FACTORY = new SavedData.Factory<>(Debris::new, Debris::load, null);
        final Map<Long, Double> at = new HashMap<>();

        static Debris load(CompoundTag tag, HolderLookup.Provider regs) {
            Debris d = new Debris();
            for (Tag t : tag.getList("wheels", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                d.at.put(c.getLong("pos"), c.getDouble("debris"));
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
            ListTag l = new ListTag();
            at.forEach((k, v) -> {
                CompoundTag c = new CompoundTag();
                c.putLong("pos", k);
                c.putDouble("debris", v);
                l.add(c);
            });
            tag.put("wheels", l);
            return tag;
        }
    }
}
