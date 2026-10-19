package com.yearofice.food.cooking;

import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.fire.FireHeat;
import com.yearofice.sim.food.Doneness;
import com.yearofice.sim.food.Roasting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the spit reads of the fire below it, through {@code api.fire} only: the ring's {@link FireHeat} (state and
 * setting) from {@link FireHeatLookup}, which lists every fire that gives heat. A ring that is out, an ember or banked
 * is not in that list and does not cook.
 */
public final class RingHeat {
    private RingHeat() {}

    public static final ResourceLocation FIRE_RING = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "fire_ring");

    /** True if the block is a fire ring (by id, so cooking needs no class from the fire package). */
    public static boolean isRing(BlockState s) { return FIRE_RING.equals(BuiltInRegistries.BLOCK.getKey(s.getBlock())); }

    /** The fire at {@code ring} as a cook sees it. */
    public static Doneness.Fire at(Level level, BlockPos ring) {
        for (FireHeat h : FireHeatLookup.INSTANCE.fires(level)) {
            if (h.x() == ring.getX() && h.y() == ring.getY() && h.z() == ring.getZ()) return Roasting.fireOf(h.state(), h.setting());
        }
        return Doneness.Fire.OUT;
    }
}
