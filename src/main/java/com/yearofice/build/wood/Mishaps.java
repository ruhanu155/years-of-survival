package com.yearofice.build.wood;

import com.yearofice.body.injury.InjuryService;
import com.yearofice.sim.wood.Mishap;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Applies a felling mishap to a player through the injury path (BodyService.applyInjury via InjuryService.injure). */
public final class Mishaps {
    private Mishaps() {}

    /** The lines the Trail Messenger gives (Trees, felling and firewood › On screen and sound). */
    public static String line(Mishap.Kind k) {
        return switch (k) {
            case BARBER_CHAIR -> "Barber chair: the trunk split.";
            case HUNG_TREE -> "Hung up, and it came down.";
            case FALLING_TREE -> "The trunk caught you.";
            case GLANCING_BRANCH -> "A branch glanced off you.";
        };
    }

    /** Lean in degrees of the tree at {@code base}: 0 to 25, fixed by position. ponytail: trees carry no lean field yet; upgrade: read the trunk's own lean. */
    public static double leanDeg(BlockPos base) {
        return Math.floorMod(base.getX() * 73856093L ^ base.getZ() * 19349663L ^ base.getY() * 83492791L, 2500) / 100.0;
    }

    /**
     * The mishaps of one real felling job, rolled when the tree goes: a barber chair when it leans over 15 degrees and the
     * back cut was tried before the notch (or from the wrong side), and a hang-up when the fall line passes a living trunk
     * taller than half this one. Both go through {@link #apply} to the feller. Returns the kind that happened, or null.
     */
    public static @org.jetbrains.annotations.Nullable Mishap.Kind roll(ServerLevel l, ServerPlayer p, BlockPos base,
                                                                       double bearingDeg, int heightM, boolean backCutFirst, boolean oneSided) {
        var rng = new java.util.SplittableRandom(l.random.nextLong());
        if (com.yearofice.sim.wood.Mishap.happens(rng, com.yearofice.sim.wood.Mishap.barberChance(leanDeg(base), backCutFirst, oneSided,
                false, WoodConfig.get("barber_chair_chance")))) {
            apply(p, com.yearofice.sim.wood.Mishap.Kind.BARBER_CHAIR);
            return com.yearofice.sim.wood.Mishap.Kind.BARBER_CHAIR;
        }
        double[] u = com.yearofice.sim.wood.Felling.unit(bearingDeg);
        double dx = u[0], dz = u[1];
        for (int s = 2; s <= heightM; s++) {
            int x = base.getX() + (int) Math.round(dx * s), z = base.getZ() + (int) Math.round(dz * s);
            for (int dy = heightM / 2 + 1; dy <= heightM; dy++) {
                if (!(l.getBlockState(new BlockPos(x, base.getY() + dy, z)).getBlock() instanceof com.yearofice.worldgen.block.YoiLogBlock)) continue;
                if (com.yearofice.sim.wood.Mishap.happens(rng, com.yearofice.sim.wood.Mishap.hangUpChance(0, dy, heightM, WoodConfig.get("hang_up_chance")))) {
                    apply(p, com.yearofice.sim.wood.Mishap.Kind.HUNG_TREE);
                    return com.yearofice.sim.wood.Mishap.Kind.HUNG_TREE;
                }
                return null; // one living trunk in the line is rolled once
            }
        }
        return null;
    }

    /** Injures {@code p} as the mishap does and knocks them back a block from the stump side. Returns false if the injury source is unknown. */
    public static boolean apply(ServerPlayer p, Mishap.Kind k) {
        boolean ok = InjuryService.injure(p, k.injurySource, null, Double.NaN, "felling:" + k.name().toLowerCase(Locale.ROOT));
        if (ok) {
            Vec3 back = p.getLookAngle().multiply(-0.5, 0, -0.5);
            p.setDeltaMovement(p.getDeltaMovement().add(back.x, 0.2, back.z));
            p.hurtMarked = true;
            p.sendSystemMessage(Component.literal(line(k)));
        }
        return ok;
    }
}
