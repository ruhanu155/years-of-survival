package com.yearofice.story.trophy;

import com.mojang.serialization.MapCodec;
import com.yearofice.api.event.TrophyHungEvent;
import com.yearofice.item.component.TrophySize;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.story.collection.RecordBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;

/**
 * {@code yearofice:trophy_mount} (A6): a head or antler item with a {@code trophy_size} hung on a wall face. It stores
 * species, size, class, coat and date; its plate reads them on inspect. State {@code plate}: {@code plain} or
 * {@code arvid}.
 */
public class TrophyMountBlock extends BaseEntityBlock {
    public static final MapCodec<TrophyMountBlock> CODEC = simpleCodec(TrophyMountBlock::new);
    public static final EnumProperty<Plate> PLATE = EnumProperty.create("plate", Plate.class);

    public enum Plate implements StringRepresentable {
        PLAIN, ARVID;

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public TrophyMountBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH).setValue(PLATE, Plate.PLAIN));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(HorizontalDirectionalBlock.FACING, PLATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TrophyMountBlockEntity(pos, state); }

    /** Hang: a held item with a trophy_size fills an empty mount. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        TrophySize size = held.get(YoiComponents.TROPHY_SIZE.get());
        if (size == null || !(level.getBlockEntity(pos) instanceof TrophyMountBlockEntity be) || be.filled()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        String species = BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
        int day = (int) (level.getDayTime() / 24000L);
        be.hang(species, size, day);
        held.shrink(1);
        if (player instanceof ServerPlayer sp) {
            RecordBook.record(sp, species, size, day);
            NeoForge.EVENT_BUS.post(new TrophyHungEvent(sp, species, size.cls(), TrophyAmbitions.countHung(sp)));
        }
        return ItemInteractionResult.CONSUME;
    }

    /** Inspect: the plate reads species, size, class and day. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TrophyMountBlockEntity be && be.filled()) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.literal(be.species().replace('_', ' ') + ": " + Math.round(be.size().value()) + " " + be.size().measure()
                        + " (" + be.size().cls() + "), day " + be.day()), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
