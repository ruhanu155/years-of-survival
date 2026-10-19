package com.yearofice.food.traps;

import com.yearofice.api.body.BodyService;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.clock.SliceClock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/**
 * {@code yearofice:snare_wire}: 1 m of snare wire with a wire lock. Used on the ground it sets a snare: 2 game
 * minutes of work as a time-lapse, charged to the body as work through the swing, refused below the minimum energy.
 */
public class SnareWireItem extends BlockItem {
    public SnareWireItem(Block block, Properties props) { super(block, props); }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        if (ctx.getPlayer() instanceof ServerPlayer sp && BodyService.get().energy(sp) < EcoConfig.tuning().minWorkEnergy()) {
            SnareBlockEntity.checkLine(sp, "too_tired");
            return InteractionResult.FAIL;
        }
        InteractionResult r = super.place(ctx);
        if (r.consumesAction() && ctx.getPlayer() instanceof ServerPlayer sp) {
            sp.swing(InteractionHand.MAIN_HAND, true);
            SliceClock.advance(sp, TrapConfig.tuning().setMinutes());
        }
        return r;
    }
}
