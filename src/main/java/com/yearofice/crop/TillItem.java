package com.yearofice.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The hoe and digging stick: right mouse on tillable soil makes a {@link GardenBedBlock} (3 or 6 game minutes a block, paced by hold-to-work later). */
public class TillItem extends Item {
    public final int minutes;

    public TillItem(Properties props, int minutes) {
        super(props);
        this.minutes = minutes;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState st = level.getBlockState(pos);
        GardenBedBlock.Soil soil = Garden.soilOf(st);
        if (soil == null || !level.getBlockState(pos.above()).isAir()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos, CropRegistry.GARDEN_BED.get().defaultBlockState().setValue(GardenBedBlock.SOIL, soil), 3);
            ctx.getItemInHand().hurtAndBreak(1, (net.minecraft.server.level.ServerLevel) level, ctx.getPlayer(), i -> { });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static String id(Item i) { return BuiltInRegistries.ITEM.getKey(i).getPath(); }
}
