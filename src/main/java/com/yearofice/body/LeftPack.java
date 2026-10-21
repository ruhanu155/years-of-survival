package com.yearofice.body;

import com.mojang.serialization.MapCodec;
import com.yearofice.core.YearOfIce;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@code yearofice:left_pack}: everything the player carried except worn clothing and the messenger, left at the
 * collapse spot by the Endless SOS (Collapse and SOS, step 4). It never expires; breaking it spills the items.
 */
public final class LeftPack {
    private LeftPack() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    public static final DeferredBlock<PackBlock> BLOCK = BLOCKS.registerBlock("left_pack", PackBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f).sound(SoundType.WOOL).noOcclusion());

    /** {@code yearofice:ground_bed}: the one-night marker under a sleeper on bare ground; removed on waking. */
    public static final DeferredBlock<GroundBed> GROUND_BED = BLOCKS.registerBlock("ground_bed", GroundBed::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0f).noOcclusion().noCollission().replaceable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackEntity>> ENTITY =
            BLOCK_ENTITIES.register("left_pack", () -> BlockEntityType.Builder.of(PackEntity::new, BLOCK.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    /** Puts {@code items} in a pack at {@code pos}, or the nearest free air cell above it. Returns the pack's position. */
    public static BlockPos place(Level level, BlockPos pos, List<ItemStack> items) {
        BlockPos at = pos;
        for (int i = 0; i < 24 && (!level.getBlockState(at).canBeReplaced() || !level.getFluidState(at).isEmpty()); i++) at = at.above();
        level.setBlock(at, BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(at) instanceof PackEntity be) {
            be.items.addAll(items);
            be.setChanged();
        }
        return at;
    }

    public static final class GroundBed extends Block {
        private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

        public GroundBed(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(net.minecraft.world.level.block.BedBlock.OCCUPIED, false)
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> b) {
            b.add(net.minecraft.world.level.block.BedBlock.OCCUPIED, net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        }

        @Override
        public boolean isBed(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                             net.minecraft.world.entity.LivingEntity sleeper) { return true; }

        /** The camera of a sleeper reads the bed's direction; this marker has no {@code facing}, so it answers north. */
        @Override
        public net.minecraft.core.Direction getBedDirection(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
            return net.minecraft.core.Direction.NORTH;
        }

        @Override
        protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s, net.minecraft.world.level.BlockGetter l,
                BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext c) { return SHAPE; }
    }

    public static final class PackBlock extends BaseEntityBlock {
        public static final MapCodec<PackBlock> CODEC = simpleCodec(PackBlock::new);

        public PackBlock(Properties p) { super(p); }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

        @Override
        protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PackEntity(pos, state); }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
            if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof PackEntity be) {
                for (ItemStack s : be.items) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
                be.items.clear();
            }
            super.onRemove(state, level, pos, next, moved);
        }
    }

    public static final class PackEntity extends BlockEntity {
        public final List<ItemStack> items = new ArrayList<>();

        public PackEntity(BlockPos pos, BlockState state) { super(ENTITY.get(), pos, state); }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) {
            super.saveAdditional(tag, r);
            ListTag list = new ListTag();
            for (ItemStack s : items) list.add(s.save(r));
            tag.put("items", list);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) {
            super.loadAdditional(tag, r);
            items.clear();
            ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) ItemStack.parse(r, list.getCompound(i)).ifPresent(items::add);
        }
    }
}
