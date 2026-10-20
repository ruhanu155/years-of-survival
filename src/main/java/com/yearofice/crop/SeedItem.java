package com.yearofice.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A seed: right mouse on a bed sows one block (one seed piece, or 20 g of grain), which rolls germination once. */
public class SeedItem extends Item {
    public final String crop;

    public SeedItem(Properties props, String crop) {
        super(props);
        this.crop = crop;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof GardenBedBlock)) return InteractionResult.PASS;
        CropSpec spec = CropData.get(crop);
        if (spec == null) return InteractionResult.PASS;
        if (ctx.getPlayer() instanceof ServerPlayer cp && cp.isShiftKeyDown()) {
            com.yearofice.plant.Inspect.say(cp, Garden.card(level, pos, spec));
            return InteractionResult.CONSUME;
        }
        Garden.Sow s = Garden.sow(level, pos, spec, Double.NaN, Grain.lotYears(ctx.getItemInHand()));
        if (s == null) return InteractionResult.PASS;
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            com.yearofice.plant.Inspect.say(sp, spec.name() + " sown: " + s.up() + " of " + s.seeds() + " came up (soil " + Math.round(s.soilC()) + " C"
                    + (s.inWindow() ? "" : ", unlikely to ripen before frost") + ").");
            if (!sp.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
