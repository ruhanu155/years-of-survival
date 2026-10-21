package com.yearofice.story.trophy;

import com.yearofice.api.event.TrophyHungEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.story.Journal;
import com.yearofice.story.collection.RecordBook;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The trophy wall's ambitions (A6): "Beyond: trophy wall" opens with the first mount hung (no deadline, no gold).
 * The first mount opens {@link com.yearofice.api.story.AmbitionTracker#TROPHY_WALL} and writes one journal line; the record book keeps the sizes.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class TrophyAmbitions {
    private TrophyAmbitions() {}

    public static final String KIND = "trophy_wall";

    /** Mounts this player has hung: the species on the record book's game page. */
    public static int countHung(ServerPlayer p) { return RecordBook.page(p).size() + 1; }

    @SubscribeEvent
    public static void onHung(TrophyHungEvent e) {
        ServerPlayer p = e.player();
        com.yearofice.api.story.AmbitionTracker.open(p, com.yearofice.api.story.AmbitionTracker.TROPHY_WALL);
        if (Journal.entries(p).stream().noneMatch(j -> KIND.equals(j.kind())))
            Journal.record(p, KIND, e.species(), "First trophy hung: " + e.species().replace('_', ' ') + ".", 0, 0);
    }
}
