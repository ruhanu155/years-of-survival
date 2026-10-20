package com.yearofice.body.illness;

import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyConfig;
import com.yearofice.body.BodyTicker;
import com.yearofice.core.hash.Mulberry32;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.IllnessModel;
import com.yearofice.sim.body.Skin;
import com.yearofice.sim.body.Wellness;
import net.minecraft.server.level.ServerPlayer;

/**
 * What other systems call to make a player ill, feed them vitamin C or rub grease in (they never write
 * {@code yearofice:body}). The onset inside an incubation window is rolled from the world seed, the player and the
 * minute, so the same cause on the same minute gives the same onset.
 */
public final class Illnesses {
    private Illnesses() {}

    /** Starts {@code id} (a register file the body steps), rolling its onset. Returns false for an unknown id. */
    public static boolean begin(ServerPlayer p, String id) {
        BodyState s = BodyAttachments.of(p);
        long now = BodyTicker.clock.minute(p.server);
        Mulberry32 rng = new Mulberry32(com.yearofice.core.hash.StageHash.of(
                p.server.overworld().getSeed() + ":ill:" + id + ":" + p.getStringUUID() + ":" + now));
        boolean ok = IllnessModel.begin(s, id, now, rng.nextDouble(), Wellness.tuning(BodyConfig.tuning()));
        if (ok) BodyTicker.sync(p, now, true);
        return ok;
    }

    public static boolean cure(ServerPlayer p, String id) {
        boolean ok = IllnessModel.cure(BodyAttachments.of(p), id);
        BodyTicker.sync(p, BodyTicker.clock.minute(p.server), true);
        return ok;
    }

    /** Vitamin C from a food or tea, in reserve days (Blueprint › Health, Vitamin C). */
    public static void addVitaminC(ServerPlayer p, double days) { IllnessModel.addVitC(BodyAttachments.of(p), days); }

    /** Rubs grease into the face (B13): protects for the preset's hours. */
    public static void greaseFace(ServerPlayer p) {
        Skin.greaseFace(BodyAttachments.of(p), BodyTicker.clock.minute(p.server), Wellness.tuning(BodyConfig.tuning()));
        BodyTicker.drainEvents(p, BodyAttachments.of(p));
    }

    /** Rubs grease into the gloves (B13): wetness gain x0.7 for three days. */
    public static void greaseGloves(ServerPlayer p) {
        Skin.greaseGloves(BodyAttachments.of(p), BodyTicker.clock.minute(p.server), Wellness.tuning(BodyConfig.tuning()));
    }
}
