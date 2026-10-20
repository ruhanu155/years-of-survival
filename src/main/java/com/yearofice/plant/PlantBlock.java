package com.yearofice.plant;

import com.yearofice.sim.plant.PlantPhase;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One plant clump (Plants and fungi › Plants as blocks): a cross-model block with two properties the server sets and
 * random ticks never touch, {@code phase} (eight values) and {@code taken} (0 untouched, 3 stripped bare). No block
 * item and no loot table: breaking one gives nothing; the harvest verbs give the parts ({@link HarvestVerbs}).
 */
public final class PlantBlock extends Block {
    /** The eight phases as a block property; {@link #sim()} is the pure enum. */
    public enum Phase implements StringRepresentable {
        DORMANT, BURIED, BUD, LEAF, FLOWER, FRUIT, SEED, SENESCENT;

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }

        public PlantPhase sim() { return PlantPhase.values()[ordinal()]; }

        public static Phase of(PlantPhase p) { return values()[p.ordinal()]; }
    }

    public static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);
    public static final IntegerProperty TAKEN = IntegerProperty.create("taken", 0, 3);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    private final String species;

    public PlantBlock(Properties props, String species) {
        super(props);
        this.species = species;
        registerDefaultState(stateDefinition.any().setValue(PHASE, Phase.LEAF).setValue(TAKEN, 0));
    }

    public static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.GRASS)
                .noOcclusion().noLootTable().pushReaction(PushReaction.DESTROY);
    }

    public String species() { return species; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(PHASE, TAKEN); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected boolean isRandomlyTicking(BlockState s) { return false; }
}
