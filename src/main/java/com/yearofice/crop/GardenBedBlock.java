package com.yearofice.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

/** {@code yearofice:garden_bed}: tilled soil, one square metre. States: {@code moisture} 0-3, {@code weeds} 0-3, {@code soil} class, {@code fed} 0-3. */
public final class GardenBedBlock extends Block {
    public enum Soil implements StringRepresentable {
        SILT(1.2), LOAM(1.0), PODZOL(0.8), GRAVEL(0.6);

        public final double yield;

        Soil(double y) { yield = y; }

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final IntegerProperty MOISTURE = IntegerProperty.create("moisture", 0, 3);
    public static final IntegerProperty WEEDS = IntegerProperty.create("weeds", 0, 3);
    public static final IntegerProperty FED = IntegerProperty.create("fed", 0, 3);
    public static final EnumProperty<Soil> SOIL = EnumProperty.create("soil", Soil.class);

    public GardenBedBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(MOISTURE, 1).setValue(WEEDS, 0).setValue(FED, 0).setValue(SOIL, Soil.LOAM));
    }

    public static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.ROOTED_DIRT).noLootTable();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(MOISTURE, WEEDS, FED, SOIL); }

    @Override
    protected boolean isRandomlyTicking(BlockState s) { return false; }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) { return true; }
}
