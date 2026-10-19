package com.yearofice.build.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The round set on a chopping block (Trees, felling and firewood › Splitting): one round, the direction of the crack
 * in its end grain (0–3, drawn by the client), and the strikes already landed off the crack.
 */
public class ChoppingBlockEntity extends BlockEntity {
    private ItemStack round = ItemStack.EMPTY;
    private int crack;
    private int strikes;

    public ChoppingBlockEntity(BlockPos pos, BlockState state) { super(WoodRegistry.CHOPPING_BLOCK_ENTITY.get(), pos, state); }

    public ItemStack round() { return round; }

    public int crack() { return crack; }

    public int strikes() { return strikes; }

    public boolean hasRound() { return !round.isEmpty(); }

    /** Sets one round on the block with a crack direction; false if one is already there. */
    public boolean setRound(ItemStack one, int crackDir) {
        if (hasRound() || !WoodItems.isRound(one)) return false;
        round = one.copyWithCount(1);
        crack = Math.floorMod(crackDir, 4);
        strikes = 0;
        changed();
        return true;
    }

    /** Takes the round off (empty if none). */
    public ItemStack takeRound() {
        ItemStack r = round;
        round = ItemStack.EMPTY;
        strikes = 0;
        changed();
        return r;
    }

    public void addStrike() {
        strikes++;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (!round.isEmpty()) tag.put("round", round.save(regs));
        tag.putByte("crack", (byte) crack);
        tag.putByte("strikes", (byte) strikes);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        round = tag.contains("round") ? ItemStack.parse(regs, tag.getCompound("round")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        crack = tag.getByte("crack");
        strikes = tag.getByte("strikes");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag t = new CompoundTag();
        saveAdditional(t, regs);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
