package com.yearofice.hunt.danger;

import com.yearofice.api.event.EncounterStateEvent;
import com.yearofice.hunt.HuntConfig;
import com.yearofice.sim.hunt.EncounterRules;
import com.yearofice.sim.hunt.EncounterRules.Charge;
import com.yearofice.sim.hunt.EncounterRules.Reaction;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Danger in detail, the server side (Blueprint › Danger in detail): decides what a charging bear or a circling pack
 * does to the player and posts {@link EncounterStateEvent}s for the hand clips and camera kicks (RECONCILE 171). The
 * bear entities and their senses are ecosystem's; they call {@link #bearCharge} when a bear commits.
 */
public final class Encounters {
    private Encounters() {}

    /**
     * A bear commits to a rush. A bluff stops short (ENDED); contact strikes (STRIKE) and then pins the player (PINNED),
     * or leaves a player who is already lying flat playing dead (PLAY_DEAD).
     */
    public static Charge bearCharge(ServerPlayer p, double distM, boolean wounded, boolean grizzly, Reaction reaction, double roll) {
        Charge c = EncounterRules.bearCharge(distM, wounded, grizzly, reaction, roll, HuntConfig.tuning());
        switch (c) {
            case BLUFF -> post(p, EncounterStateEvent.State.ENDED);
            case CONTACT -> {
                post(p, EncounterStateEvent.State.STRIKE);
                post(p, reaction == Reaction.PLAY_DEAD ? EncounterStateEvent.State.PLAY_DEAD : EncounterStateEvent.State.PINNED);
            }
            case NONE -> { }
        }
        return c;
    }

    /** The player gets up after a bear leaves. */
    public static void bearLeaves(ServerPlayer p) {
        post(p, EncounterStateEvent.State.GET_UP);
        post(p, EncounterStateEvent.State.ENDED);
    }

    /** A pack closes in: STRIKE when the rush rules say so, else it keeps circling. */
    public static boolean wolvesRush(ServerPlayer p, int wolves, double secondsSeen, boolean alone, boolean fire) {
        boolean rush = EncounterRules.packRushes(wolves, secondsSeen, alone, fire);
        if (rush) post(p, EncounterStateEvent.State.STRIKE);
        else if (wolves > 0) {
            com.yearofice.story.ambition.Ambitions.fire(p, "wolf.drove_off_no_loss",
                    com.yearofice.api.clock.ClockService.installed() ? com.yearofice.api.clock.ClockService.get().nowMinute() : 0);
        }
        return rush;
    }

    static void post(ServerPlayer p, EncounterStateEvent.State s) { NeoForge.EVENT_BUS.post(new EncounterStateEvent(p, s)); }
}
