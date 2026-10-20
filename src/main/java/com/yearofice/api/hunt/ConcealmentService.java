package com.yearofice.api.hunt;

import com.yearofice.sim.hunt.Blinds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a blind or tree stand does to an animal's senses (Blueprint › Setting up the hunt › Build sheet). Ecosystem
 * multiplies the animal's sight range for the player by {@link Blinds.Effect#sightX()}, its hearing by {@code noiseX()}
 * and skips the scent plume while {@code scentClearM()} covers the distance. {@code hunt} installs it.
 */
public interface ConcealmentService {
    Blinds.Effect effectFor(ServerLevel level, ServerPlayer player, double animalDistM, boolean animalInFront, boolean bigPrey, boolean bear);

    static ConcealmentService get() { return Holder.service; }

    static void install(ConcealmentService s) { Holder.service = s; }

    final class Holder {
        private static volatile ConcealmentService service = (l, p, d, f, b, bear) -> Blinds.Effect.NONE;

        private Holder() {}
    }
}
