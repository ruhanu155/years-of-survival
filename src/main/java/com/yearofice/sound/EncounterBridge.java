package com.yearofice.sound;

import com.yearofice.api.event.EncounterStateEvent;
import com.yearofice.api.event.StaggerEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.net.YoiNet;
import com.yearofice.net.payload.EncounterClipPayload;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Server side of the encounter hand clips: turns api events into clip names for the owning client. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class EncounterBridge {
    private EncounterBridge() {}

    /** Clips for a stumble outcome: every outcome staggers; a fall also throws the hands out. */
    public static List<String> clipsForStagger(String outcome) {
        return "fall".equals(outcome) ? List.of("catch_fall", "stagger") : List.of("stagger");
    }

    public static String clipForState(EncounterStateEvent.State s) {
        return switch (s) {
            case PLAY_DEAD -> "cover_neck";
            case PINNED, GET_UP -> "push_up";
            case STRIKE -> "desperate_strike";
            case ENDED -> "";
        };
    }

    @SubscribeEvent
    public static void onStagger(StaggerEvent e) {
        for (String c : clipsForStagger(e.outcome())) send(e.player(), c);
    }

    @SubscribeEvent
    public static void onState(EncounterStateEvent e) { send(e.player(), clipForState(e.state())); }

    private static void send(ServerPlayer p, String clip) { YoiNet.send(p, new EncounterClipPayload(clip)); }
}
