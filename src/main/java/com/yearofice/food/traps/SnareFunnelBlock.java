package com.yearofice.food.traps;

import net.minecraft.world.level.block.Block;

/**
 * {@code yearofice:snare_funnel}: a bundle of brush and sticks laid beside a trail snare to narrow the run (Every
 * concept from the video › Snare funnels). Two of them, one each side within 1 block across the snare's facing, make a
 * funnel: catch chance x1.3 inside the x6 place cap. It is in the tag {@code yearofice:funnel_materials} with logs and
 * rocks. A block set over the loop, or a wall across the run, blocks it ({@link SnareSite}).
 */
public class SnareFunnelBlock extends Block {
    public SnareFunnelBlock(Properties props) { super(props); }
}
