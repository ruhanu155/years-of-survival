package com.yearofice.hunt;

import com.yearofice.api.env.EnvService;
import com.yearofice.api.hunt.ConcealmentService;
import com.yearofice.sim.hunt.Blinds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Reads the blocks around a player into a {@link Blinds.Effect} for the animals (the {@link ConcealmentService} {@code hunt} installs). */
public final class HuntConcealment implements ConcealmentService {
    /** GameTest hook: the world clock starts in deep winter, so a blind test pins the snow it stands in. */
    public static volatile Double snowOverrideCm;

    @Override
    public Blinds.Effect effectFor(ServerLevel level, ServerPlayer p, double animalDistM, boolean inFront, boolean bigPrey, boolean bear) {
        Blinds.Kind kind = Blinds.Kind.NONE;
        boolean fresh = false;
        BlockPos at = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
            var s = level.getBlockState(q);
            if (s.is(HuntRegistry.POLE_BLIND.get())) {
                kind = Blinds.Kind.GROUND_BLIND;
                fresh = s.getValue(HuntBlocks.PoleBlind.FRESH);
                break;
            }
        }
        if (level.getBlockState(at.below()).is(HuntRegistry.TREE_STAND.get()) || level.getBlockState(at).is(HuntRegistry.TREE_STAND.get())) kind = Blinds.Kind.TREE_STAND;
        if (kind == Blinds.Kind.NONE) return Blinds.Effect.NONE;
        Vec3 v = p.getDeltaMovement();
        boolean still = v.horizontalDistanceSqr() < 1e-4;
        boolean drawing = p.isUsingItem() && p.getUseItem().getItem() instanceof com.yearofice.hunt.BowItem;
        double snow = 0;
        if (snowOverrideCm != null) snow = snowOverrideCm;
        else try { snow = EnvService.get().snowCm(level, at); } catch (IllegalStateException ignored) { /* env not installed */ }
        return Blinds.effect(kind, still, drawing, animalDistM, inFront, fresh ? 0.5 : 2.0, bigPrey, bear, snow, HuntConfig.tuning());
    }
}
