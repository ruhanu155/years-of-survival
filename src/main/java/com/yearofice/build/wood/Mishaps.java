package com.yearofice.build.wood;

import com.yearofice.body.injury.InjuryService;
import com.yearofice.sim.wood.Mishap;
import java.util.Locale;
import net.minecraft.network.chat.Component;
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
