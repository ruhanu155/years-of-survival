package com.yearofice.build.pieces;

import com.yearofice.api.clock.ClockService;
import com.yearofice.build.rooms.Rooms;
import com.yearofice.plant.PlantRegistry;
import com.yearofice.story.ambition.Ambitions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A log wall course: carries a {@code chink} property ({@code none}, {@code moss}, {@code clay}) that seals its seam. */
public class WallLogBlock extends Block {
    public enum Chink implements StringRepresentable {
        NONE("none"),
        MOSS("moss"),
        CLAY("clay");

        private final String name;
        Chink(String name) { this.name = name; }

        @Override public String getSerializedName() { return name; }
    }

    public static final EnumProperty<Chink> CHINK = EnumProperty.create("chink", Chink.class);

    public WallLogBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(CHINK, Chink.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(CHINK);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        Chink cur = state.getValue(CHINK);
        Item item = stack.getItem();
        var mossHolder = PlantRegistry.PARTS.get("sphagnum_moss");
        boolean isMoss = mossHolder != null && item == mossHolder.get();
        boolean isClay = item == Items.CLAY_BALL || item == Items.CLAY;
        Chink next = null;
        if (isMoss && cur == Chink.NONE) next = Chink.MOSS;
        else if (isClay && cur != Chink.CLAY) next = Chink.CLAY;
        if (next == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(CHINK, next), 3);
            level.playSound(null, pos, next == Chink.MOSS ? SoundEvents.MOSS_PLACE : SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            Rooms.dirty(level, pos);
            if (player instanceof ServerPlayer sp && ClockService.installed()) {
                var r = Rooms.INSTANCE.roomAt(level, sp.blockPosition());
                if (r != null && r.sourceW() > 0) {
                    Ambitions.fire(sp, "build.cabin_with_stove", ClockService.get().nowMinute());
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
