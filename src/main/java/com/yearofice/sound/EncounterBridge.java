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

    /** The hand clip for a species' contact beat (RECONCILE 171); {@code ""} when that beat has none. */
    public static String clipForOutcome(String species, String outcome) {
        return switch (species) {
            case "grizzly" -> switch (outcome) {
                case "warn" -> "still_hands";
                case "bluff_charge" -> "hold_ground";
                case "contact" -> "cover_neck";
                case "retreat" -> "back_away";
                default -> "";
            };
            case "black_bear" -> switch (outcome) {
                case "warn", "bluff_charge" -> "raise_arms";
                case "contact" -> "fend_off";
                case "retreat" -> "back_away";
                default -> "";
            };
            case "moose" -> switch (outcome) {
                case "warn" -> "still_hands";
                case "bluff_charge" -> "brace";
                case "contact" -> "shield_face";
                case "retreat" -> "back_away";
                default -> "";
            };
            case "wolf" -> switch (outcome) {
                case "warn" -> "hold_ground";
                case "contact" -> "fend_off";
                case "retreat" -> "back_away";
                default -> "";
            };
            case "coyote" -> switch (outcome) {
                case "warn" -> "raise_arms";
                case "contact" -> "fend_off";
                case "retreat" -> "back_away";
                default -> "";
            };
            default -> "";
        };
    }

    @SubscribeEvent
    public static void onOutcome(com.yearofice.api.event.EncounterOutcomeEvent e) {
        String c = clipForOutcome(e.species(), e.outcome());
        if (!c.isEmpty()) send(e.player(), c);
    }

    @SubscribeEvent
    public static void onStagger(StaggerEvent e) {
        for (String c : clipsForStagger(e.outcome())) send(e.player(), c);
    }

    @SubscribeEvent
    public static void onState(EncounterStateEvent e) { send(e.player(), clipForState(e.state())); }

    private static void send(ServerPlayer p, String clip) { YoiNet.send(p, new EncounterClipPayload(clip)); }
}
