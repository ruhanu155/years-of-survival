package com.yearofice.world.ice;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.world.IceModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Ice blocks (Minecraft engine › Ice, snow and permafrost): {@code skim_ice}, {@code river_ice} with {@code stage}
 * thin, safe or rotten, and {@code ice_hole}. Ice is one block thick on screen whatever its true thickness; the
 * thickness lives in {@code yearofice_ice}. None ticks at random or melts by light (vanilla ice never placed).
 */
public final class IceBlocks {
    private IceBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);

    public enum StageProp implements StringRepresentable {
        THIN, SAFE, ROTTEN;

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final EnumProperty<StageProp> STAGE = EnumProperty.create("stage", StageProp.class);

    private static BlockBehaviour.Properties ice(MapColor c, float strength) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(strength).friction(0.98F).sound(SoundType.GLASS)
                .noOcclusion().isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false);
    }

    /** A skim breaks under anything that stands on it (Ice state table). */
    public static final class Skim extends Block {
        public Skim(Properties p) { super(p); }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            if (!level.isClientSide && !entity.isSpectator()) level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static final class River extends Block {
        public River(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(STAGE, StageProp.SAFE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(STAGE); }
    }

    public static final DeferredBlock<Skim> SKIM_ICE = BLOCKS.registerBlock("skim_ice", Skim::new, ice(MapColor.ICE, 0.1F));
    public static final DeferredBlock<River> RIVER_ICE = BLOCKS.registerBlock("river_ice", River::new, ice(MapColor.ICE, 0.5F));
    /** An open hole: water to swim in and fish from; it skins over by the ice rules ({@code IceService.tickHoles}). */
    public static final class Hole extends Block {
        public Hole(Properties p) { super(p); }

        @Override
        protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s) {
            return net.minecraft.world.level.material.Fluids.WATER.getSource(false);
        }
    }

    public static final DeferredBlock<Hole> ICE_HOLE = BLOCKS.registerBlock("ice_hole", Hole::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(-1.0F, 0).noLootTable().noCollission().noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));

    public static BlockState stateFor(IceModel.Stage s) {
        return switch (s) {
            case OPEN -> Blocks.WATER.defaultBlockState();
            case SKIM -> SKIM_ICE.get().defaultBlockState();
            case THIN -> RIVER_ICE.get().defaultBlockState().setValue(STAGE, StageProp.THIN);
            case SAFE -> RIVER_ICE.get().defaultBlockState().setValue(STAGE, StageProp.SAFE);
            case ROTTEN -> RIVER_ICE.get().defaultBlockState().setValue(STAGE, StageProp.ROTTEN);
        };
    }

    public static boolean isIce(BlockState s) { return s.is(SKIM_ICE.get()) || s.is(RIVER_ICE.get()); }
}
