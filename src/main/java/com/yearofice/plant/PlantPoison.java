package com.yearofice.plant;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.InjurySpec;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.sim.body.part.InjurySource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Poison and taste sickness through the body api (Plants and fungi › Poisonings). A bite of an unknown toxic plant
 * starts {@code taste_sick} (its own file: onset one hour, energy cap 5 for 24 h) and the Condition loss lands when
 * the one-hour taste timer runs out. Eating a toxic part starts {@code poison_plant} and the loss lands at the plant's
 * onset; eating one the player did not know posts the Messenger line. The timers live in memory: a restart inside the
 * hour forgets the Condition loss (the illness itself is the body's and survives).
 */
public final class PlantPoison {
    private PlantPoison() {}

    public static final int TASTE_TIMER_MIN = 60;
    static final InjurySource SOURCE = new InjurySource("yearofice:plant_poison", 0, 1, Map.of(), Map.of(), List.of(), 0, true);

    record Pending(long dueMinute, double loss, String cause) {}

    private static final Map<UUID, List<Pending>> QUEUE = new ConcurrentHashMap<>();

    public static long nowMinute() { return ClockService.get().nowDs() / 600; }

    /** A toxic bite of an unknown plant: sickness starts now, the Condition loss in one hour. */
    public static void tasted(ServerPlayer p, PlantSpec spec, double loss) {
        BodyService.get().beginIllness(p, "taste_sick");
        QUEUE.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(new Pending(nowMinute() + TASTE_TIMER_MIN, loss, "plant_taste"));
    }

    /** A toxic part eaten: poison starts, the loss lands at onset; an unknown plant also posts the Messenger line. */
    public static void eaten(ServerPlayer p, PlantSpec spec, boolean wasKnown) {
        if (spec.toxic == null) return;
        BodyService.get().beginIllness(p, "poison_plant");
        double loss = spec.toxic.conditionLoss() * ForageConfig.tuning().poisonScale();
        int onset = (spec.toxic.onsetLoMin() + spec.toxic.onsetHiMin()) / 2;
        QUEUE.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(new Pending(nowMinute() + onset, loss, "plant_poison"));
        if (!wasKnown && HudBus.installed()) {
            HudBus.get().post(p, HudLine.of(HudLineType.WARN, "plant_poison", "plant", HudArg.text(spec.name)));
        }
    }

    /** Minutes still to wait for the oldest pending loss of a player, or -1 (for /yoi plant and tests). */
    public static long pendingMinutes(ServerPlayer p) {
        List<Pending> l = QUEUE.get(p.getUUID());
        if (l == null || l.isEmpty()) return -1;
        long min = Long.MAX_VALUE;
        for (Pending x : l) min = Math.min(min, x.dueMinute());
        return Math.max(0, min - nowMinute());
    }

    public static void onMinute(ClockEvent.Minute e) {
        if (QUEUE.isEmpty()) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) flush(p, e.last());
    }

    /** Applies every pending loss of {@code p} that is due at absolute minute {@code now}. */
    public static void flush(ServerPlayer p, long now) {
        List<Pending> list = QUEUE.get(p.getUUID());
        if (list == null) return;
        for (int i = list.size() - 1; i >= 0; i--) {
            Pending x = list.get(i);
            if (x.dueMinute() > now) continue;
            list.remove(i);
            if (x.loss() > 0) BodyService.get().applyInjury(p, InjurySpec.of(SOURCE).withLoss(x.loss()), "yearofice:" + x.cause());
        }
        if (list.isEmpty()) QUEUE.remove(p.getUUID());
    }
}
