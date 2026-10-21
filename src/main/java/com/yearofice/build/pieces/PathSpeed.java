package com.yearofice.build.pieces;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Walking on a laid path is faster by its surface's bonus (Structures and buildables › Paths); a movement-speed modifier, set every few ticks. */
public final class PathSpeed {
    private PathSpeed() {}

    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "path_speed");

    static void init() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 4 == 0) update(p);
        });
    }

    /** The bonus fraction under the player's feet (0 off a path). */
    public static double bonus(Player p) {
        BlockPos at = p.blockPosition();
        for (BlockPos q : new BlockPos[] {at, at.below()}) {
            BlockState s = p.level().getBlockState(q);
            if (s.getBlock() instanceof PathBlock) return PiecesConfig.pathPct(s.getValue(PathBlock.SURFACE).getSerializedName()) / 100.0;
        }
        return 0;
    }

    /** Applies (or removes) the modifier; returns the bonus now in force. */
    public static double update(Player p) {
        AttributeInstance a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) return 0;
        double b = bonus(p);
        AttributeModifier cur = a.getModifier(ID);
        if (cur != null && Math.abs(cur.amount() - b) < 1e-9) return b;
        a.removeModifier(ID);
        if (b > 0) a.addTransientModifier(new AttributeModifier(ID, b, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return b;
    }
}
