package com.yearofice.fire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.yearofice.core.YearOfIce;

/**
 * Where a packed fire's spark lands (Blueprint › Fire, water and light › Spread): a burnable piece within 2 m. The
 * pieces are the blocks tagged {@code yearofice:burnable} (a woodpile's logs, a bough bed, a chopping block); the
 * nearest one becomes a {@link FlameBlock}. Vanilla fire spread is off; nothing else here lights anything.
 */
public final class Spread {
    private Spread() {}

    public static final TagKey<Block> BURNABLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "burnable"));

    /** The reach of a spark from a hearth, in blocks. */
    public static final int REACH = 2;

    /** The nearest burnable block within {@code reach} of {@code from}, or null. */
    public static BlockPos nearestBurnable(Level level, BlockPos from, int reach) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-reach, -reach, -reach), from.offset(reach, reach, reach))) {
            if (p.equals(from)) continue;
            BlockState st = level.getBlockState(p);
            if (!st.is(BURNABLE)) continue;
            double d = p.distSqr(from);
            if (d < bd) {
                bd = d;
                best = p.immutable();
            }
        }
        return best;
    }

    /** Lights the nearest burnable piece within {@code reach}; returns where, or null when there was none. */
    public static BlockPos igniteNear(Level level, BlockPos from, int reach) {
        BlockPos at = nearestBurnable(level, from, reach);
        if (at == null) return null;
        level.setBlock(at, FireRegistry.FLAME.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(at) instanceof FlameBlockEntity f) f.setMinutesLeft((int) Math.round(FireConfig.tuning().flameBurnMin()));
        return at;
    }
}
