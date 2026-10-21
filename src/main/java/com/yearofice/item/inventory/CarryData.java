package com.yearofice.item.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The value of the {@code yearofice:carry} attachment in mode {@code arms}, for a lifted box (Minecraft development ›
 * Hand-carried loads and carcasses, Lift a full box): the block state it stood as (facing kept), the block entity's
 * full NBT with its contents (freshness stamps travel in the stacks), and the weight and stack count the carry slot
 * reads. Empty when the state is air. v1, every field defaulted.
 */
public record CarryData(BlockState state, CompoundTag blockEntity, double kg, int stacks) {
    public static final CarryData NONE = new CarryData(Blocks.AIR.defaultBlockState(), new CompoundTag(), 0, 0);

    public static final Codec<CarryData> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockState.CODEC.optionalFieldOf("state", Blocks.AIR.defaultBlockState()).forGetter(CarryData::state),
            CompoundTag.CODEC.optionalFieldOf("block_entity", new CompoundTag()).forGetter(CarryData::blockEntity),
            Codec.DOUBLE.optionalFieldOf("kg", 0.0).forGetter(CarryData::kg),
            Codec.INT.optionalFieldOf("stacks", 0).forGetter(CarryData::stacks)
    ).apply(i, CarryData::new));

    public boolean isEmpty() { return state.isAir(); }

    /** The carry slot's line: "Chest · 24 kg · 19 stacks". */
    public String slotLine() {
        String name = state.getBlock().getName().getString();
        return String.format(java.util.Locale.ROOT, "%s · %.0f kg · %d stack%s", name, kg, stacks, stacks == 1 ? "" : "s");
    }

    public String blockId() { return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(); }
}
