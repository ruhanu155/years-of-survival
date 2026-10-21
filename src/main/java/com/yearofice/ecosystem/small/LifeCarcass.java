package com.yearofice.ecosystem.small;

import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.carcass.Freshness;
import com.yearofice.ecosystem.clock.SliceClock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Carcasses the world makes without a hit (every death makes a carcass): spent salmon and winterkill fish. */
public final class LifeCarcass {
    private LifeCarcass() {}

    /** Places a fish carcass of {@code speciesPath} ({@code chum_salmon}) at {@code at}; null when the level refuses. */
    public static <T extends CarcassEntity> T spawn(ServerLevel level, EntityType<T> type, String speciesPath, Vec3 at, String cause, double kg) {
        Item meat = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("yearofice", speciesPath));
        double minute = SliceClock.minuteD(level);
        CarcassRecord r = new CarcassRecord("yearofice:" + speciesPath, level.random.nextInt(2), cause, (long) Math.floor(minute),
                (int) Math.round(kg * 1000), 0, Freshness.wholeCarcass(meat, minute));
        T c = type.create(level);
        if (c == null) return null;
        c.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0);
        c.setRecord(r);
        return level.addFreshEntity(c) ? c : null;
    }

    /** Carcasses of any kind within {@code r} of {@code at} whose record names {@code cause}. */
    public static List<CarcassEntity> withCause(ServerLevel level, net.minecraft.core.BlockPos at, double r, String cause) {
        return level.getEntitiesOfClass(CarcassEntity.class, new AABB(at).inflate(r, 16, r), c -> c.record().cause().equals(cause));
    }

    /** Open water at a column: the surface cell of a water column (water with air above it), or null. */
    public static net.minecraft.core.BlockPos surfaceWater(ServerLevel level, int x, int z, int aroundY) {
        for (int dy = 8; dy >= -12; dy--) {
            net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(x, aroundY + dy, z);
            if (level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER) && !level.getFluidState(p.above()).is(net.minecraft.tags.FluidTags.WATER)) return p;
        }
        return null;
    }
}
