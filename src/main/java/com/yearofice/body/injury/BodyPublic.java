package com.yearofice.body.injury;

import com.yearofice.body.BodyAttachments;
import com.yearofice.body.net.BodyPublicPayload;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Problems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Builds and sends {@code body_public} to the players who can see this one: when it changes, and every 10 seconds. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class BodyPublic {
    private BodyPublic() {}

    private static final Map<UUID, Integer> LAST = new ConcurrentHashMap<>();

    /** The flag byte for a body: only what shows from outside. */
    public static int flags(BodyState s) {
        int f = 0;
        for (Problem q : s.parts.problems()) {
            if (q.bleedPerMinute() > 0) f |= BodyPublicPayload.BLEEDING;
            if (Problems.INFECTION.equals(q.id()) && q.stage() >= 2) f |= BodyPublicPayload.FEVER;
            if (Problems.BROKEN_BONE.equals(q.id())) {
                if (q.part().group == Part.Group.LEGS || q.part().group == Part.Group.FEET) f |= BodyPublicPayload.LIMP;
                if (q.part().group == Part.Group.ARMS || q.part().group == Part.Group.HANDS) f |= BodyPublicPayload.ARM_HURT;
                if (q.isSplinted()) f |= BodyPublicPayload.SPLINTED;
            }
        }
        for (Part p : Part.values()) {
            if ((p.group == Part.Group.LEGS || p.group == Part.Group.FEET) && s.parts.hp(p) < 50) f |= BodyPublicPayload.LIMP;
        }
        if (s.sos == 1 || s.condition() <= 0) f |= BodyPublicPayload.DOWN;
        return f;
    }

    public static BodyPublicPayload payload(ServerPlayer p) {
        return new BodyPublicPayload(p.getId(), flags(BodyAttachments.of(p)));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || p.connection == null) return;
        BodyPublicPayload pl = payload(p);
        Integer last = LAST.put(p.getUUID(), pl.flags());
        if (last != null && last == pl.flags() && p.tickCount % 200 != 0) return;
        if (p.connection.hasChannel(BodyPublicPayload.TYPE)) PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, pl);
    }
}
