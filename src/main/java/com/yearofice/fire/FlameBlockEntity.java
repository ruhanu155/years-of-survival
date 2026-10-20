package com.yearofice.fire;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The timer of a {@link FlameBlock}, stepped by game minutes from {@link FireClock}. At 0 the flame and the piece under
 * it are gone. Each minute a flame has {@link #NEIGHBOUR_PCT}% to light one burnable piece next to it (a woodpile
 * burns down together).
 */
public class FlameBlockEntity extends BlockEntity {
    /** Chance a minute a flame lights a burnable piece next to it, percent. */
    public static final double NEIGHBOUR_PCT = 15;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOADED = new HashMap<>();

    private int minutesLeft = 10;

    public FlameBlockEntity(BlockPos pos, BlockState state) { super(FireRegistry.FLAME_ENTITY.get(), pos, state); }

    public int minutesLeft() { return minutesLeft; }

    public void setMinutesLeft(int m) { minutesLeft = m; setChanged(); }

    static Set<BlockPos> loaded(Level level) { return LOADED.getOrDefault(level.dimension(), Set.of()); }

    static void clear() { LOADED.clear(); }

    /** Steps {@code minutes} game minutes; the flame burns out (and the piece with it) when they are used up. */
    public void clockMinutes(int minutes) {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < minutes; i++) {
            if (--minutesLeft <= 0) {
                level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
                return;
            }
            if (level.random.nextDouble() * 100 < NEIGHBOUR_PCT) Spread.igniteNear(level, worldPosition, 1);
        }
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) LOADED.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(worldPosition.immutable());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            Set<BlockPos> s = LOADED.get(level.dimension());
            if (s != null) s.remove(worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putInt("minutes_left", minutesLeft);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("minutes_left")) minutesLeft = tag.getInt("minutes_left");
    }
}
