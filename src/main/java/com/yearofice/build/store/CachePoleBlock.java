package com.yearofice.build.store;

import com.yearofice.sim.build.CacheLegs;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** One metre of a cache leg. An axe peels it (barked to peeled); four tin cans band it (peeled to banded). */
public class CachePoleBlock extends Block {

    public enum Dress implements StringRepresentable {
        BARKED(CacheLegs.Leg.BARKED), PEELED(CacheLegs.Leg.PEELED), BANDED(CacheLegs.Leg.BANDED);
        public final CacheLegs.Leg leg;
        Dress(CacheLegs.Leg l) { leg = l; }
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        public static Dress of(CacheLegs.Leg l) { return values()[l.ordinal()]; }
    }

    public static final EnumProperty<Dress> LEG = EnumProperty.create("leg", Dress.class);

    public CachePoleBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LEG, Dress.BARKED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LEG); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        Dress leg = state.getValue(LEG);
        if (leg == Dress.BARKED && stack.is(ItemTags.AXES)) {
            if (!level.isClientSide) level.setBlock(pos, state.setValue(LEG, Dress.PEELED), 3);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (leg == Dress.PEELED && stack.is(com.yearofice.fire.water.WaterRegistry.TIN_CAN.get()) && stack.getCount() >= 4) {
            if (!level.isClientSide) {
                if (!player.getAbilities().instabuild) stack.shrink(4);
                level.setBlock(pos, state.setValue(LEG, Dress.BANDED), 3);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
