package com.yearofice.world;

import com.yearofice.build.craft.CraftConfig;
import com.yearofice.build.craft.CraftRegistry;
import com.yearofice.world.season.SeasonConfig;
import com.yearofice.sim.world.SeasonModel;
import com.yearofice.worldgen.Archetype;
import com.yearofice.worldgen.block.WorldBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Two ground rules on what digging gives (Blueprint › Valley archetypes: flint lies on the bars at twice the usual
 * density in braided flats; Gathering materials: frozen ground can't be dug, so a clay bank yields nothing from freeze-up
 * to thaw). The loot tables hold the base 1 % flint; this adds the braided-flats extra roll and clears frozen clay.
 */
public final class GroundYield {
    private GroundYield() {}

    /** The base flint chance in {@code loot_table/blocks/gravel.json} (one 0.5 kg nodule per 100 m2 of bar). */
    public static final double FLINT_BASE = 0.01;

    /** Extra flint chance for a gravel drop: base x (mult - 1) on braided flats, else 0. */
    public static double extraFlint(boolean braided, double mult) { return braided ? FLINT_BASE * Math.max(0, mult - 1) : 0; }

    @SubscribeEvent
    public static void drops(BlockDropsEvent e) {
        ServerLevel level = e.getLevel();
        var state = e.getState();
        if (state.is(WorldBlocks.CLAY_BANK.get())) {
            if (SeasonModel.groundFrozen(com.yearofice.api.clock.ClockService.get().dayOfYear(), SeasonConfig.season())) e.getDrops().clear();
        } else if (state.is(WorldBlocks.GRAVEL.get())) {
            var vw = WorldHooks.world(level);
            double p = extraFlint(vw != null && vw.seed.archetype() == Archetype.BRAIDED_FLATS, CraftConfig.get("braided_flint_mult"));
            if (p > 0 && level.random.nextDouble() < p) {
                var pos = e.getPos();
                e.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(CraftRegistry.FLINT_NODULE.get())));
            }
        }
    }
}
