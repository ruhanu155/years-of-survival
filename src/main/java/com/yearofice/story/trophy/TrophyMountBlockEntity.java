package com.yearofice.story.trophy;

import com.yearofice.item.component.TrophySize;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** What the plate reads: species, size (measure, value, z, class), coat and the day it was hung. */
public class TrophyMountBlockEntity extends BlockEntity {
    private String species = "";
    private TrophySize size;
    private int coat, day;

    public TrophyMountBlockEntity(BlockPos pos, BlockState state) { super(TrophyRegistry.TROPHY_MOUNT_ENTITY.get(), pos, state); }

    public boolean filled() { return size != null; }

    public void hang(String species, TrophySize size, int day) {
        this.species = species;
        this.size = size;
        this.day = day;
        setChanged();
    }

    public String species() { return species; }

    public TrophySize size() { return size; }

    public int coat() { return coat; }

    public int day() { return day; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (size == null) return;
        tag.putString("species", species);
        tag.put("size", TrophySize.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, size).getOrThrow());
        tag.putInt("coat", coat);
        tag.putInt("day", day);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        if (!tag.contains("size")) { size = null; return; }
        species = tag.getString("species");
        size = TrophySize.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("size")).result().orElse(null);
        coat = tag.getInt("coat");
        day = tag.getInt("day");
    }
}
