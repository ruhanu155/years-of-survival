package com.yearofice.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Posted on the game bus when a big animal's contact beat changes (RECONCILE 171): {@code species} is the id path
 * ({@code grizzly}, {@code black_bear}, {@code moose}, {@code wolf}, {@code coyote}) and {@code outcome} one of
 * {@code warn}, {@code bluff_charge}, {@code contact} or {@code retreat}. The sound bridge maps the pair to a hand clip.
 */
public final class EncounterOutcomeEvent extends Event {
    private final ServerPlayer player;
    private final String species, outcome;

    public EncounterOutcomeEvent(ServerPlayer player, String species, String outcome) {
        this.player = player;
        this.species = species;
        this.outcome = outcome;
    }

    public ServerPlayer player() { return player; }

    public String species() { return species; }

    public String outcome() { return outcome; }
}
