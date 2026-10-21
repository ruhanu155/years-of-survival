package com.yearofice.client.season;

import com.yearofice.client.clock.ClientClock;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.world.SeasonModel;
import com.yearofice.sim.world.SeasonPreset;
import com.yearofice.worldgen.block.WorldBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Seasonal colour for leaves and grass by the calendar (Seasonal variants by colour remap): the runtime tint the
 * art doc keeps for tinted blocks. The colour comes from {@link SeasonModel} on the Standard calendar numbers (the
 * common config is not synced to clients); the block models carry {@code tintindex 0} where a block takes it.
 * Birch and willow go gold, the rest of the green turns with the grass; evergreens are untouched.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SeasonTint {
    private SeasonTint() {}

    private static int doy() {
        return Calendar.dayOfYear(ClientClock.nowDs());
    }

    @SubscribeEvent
    public static void colours(RegisterColorHandlersEvent.Block e) {
        var t = SeasonPreset.STANDARD.season;
        e.register((state, level, pos, tint) -> tint == 0 ? SeasonModel.leafColour(doy(), t, true) : -1,
                WorldBlocks.BIRCH_LEAVES.get(), WorldBlocks.WILLOW_BRUSH.get());
        e.register((state, level, pos, tint) -> tint == 0 ? SeasonModel.grassColour(doy(), t) : -1,
                WorldBlocks.GRASS_TURF.get(), WorldBlocks.TUNDRA_TURF.get());
    }
}
