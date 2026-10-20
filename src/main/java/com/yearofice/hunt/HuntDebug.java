package com.yearofice.hunt;

import com.yearofice.sim.hunt.Shot;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** The last shot each player made, for {@code /yoi hunt shot} and the GameTests. */
public final class HuntDebug {
    private HuntDebug() {}

    private static final Map<UUID, String> LAST = new ConcurrentHashMap<>();
    private static volatile Shot.Outcome lastOutcome;

    static void remember(@Nullable ServerPlayer p, String species, String head, double dist, Shot.Angle angle, Shot.Outcome o, @Nullable String zone) {
        lastOutcome = o;
        String s = String.format(Locale.ROOT, "%s with a %s head at %.1f m, %s: zone %s, E_eff %.1f J, %s (%s)", species, head, dist, angle, zone, o.effJ(), o.result(), o.note());
        LAST.put(p == null ? new UUID(0, 0) : p.getUUID(), s);
    }

    public static String last(@Nullable ServerPlayer p) { return LAST.getOrDefault(p == null ? new UUID(0, 0) : p.getUUID(), "No shot yet."); }

    public static Shot.Outcome lastOutcome() { return lastOutcome; }
}
