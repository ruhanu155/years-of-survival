package com.yearofice.body.injury;

import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.sim.body.part.Injuries;
import com.yearofice.sim.body.part.Part;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/** Every injury change posts a Messenger line that says what happened and where (Trail Messenger and journal lines). */
public final class InjuryMessages {
    private InjuryMessages() {}

    public static String partWord(Part p) { return p.id().replace('_', ' '); }

    public static String causeWord(String cause) {
        String c = cause.contains(":") ? cause.substring(cause.indexOf(':') + 1) : cause;
        return c.replace('_', ' ');
    }

    /** After {@code applyInjury}: one line per new problem, else one line for the hit. */
    public static void injured(ServerPlayer p, String sourceId, String cause, Injuries.Result r) {
        if (r.hitPack()) {
            HudBus.get().post(p, HudLine.of(HudLineType.WARN, "pack_took_it", "body"));
            return;
        }
        if (r.problem() != null) {
            HudBus.get().post(p, HudLine.of(HudLineType.WARN, lineId(r.problem().id(), r.problem().stage()), "body",
                    HudArg.text(partWord(r.problem().part())),
                    HudArg.text(causeWord(cause))));
        } else if (!r.partsHit().isEmpty()) {
            HudBus.get().post(p, HudLine.of(HudLineType.WARN, "hurt", "body",
                    HudArg.text(partWord(r.partsHit().get(0))), HudArg.text(causeWord(cause))));
        }
    }

    /** Lang id of the line for a new problem; cut, burn and sprain have a second-stage wording. */
    static String lineId(String problem, int stage) {
        boolean staged = problem.equals("cut") || problem.equals("burn") || problem.equals("sprain");
        return "problem_" + problem + (staged && stage >= 2 ? "_2" : "");
    }

    public static void line(ServerPlayer p, String id, String... text) {
        HudArg[] args = new HudArg[text.length];
        for (int i = 0; i < text.length; i++) args[i] = HudArg.text(text[i]);
        HudBus.get().post(p, HudLine.of(HudLineType.WARN, id, "body", args));
    }

    static String lower(String s) { return s.toLowerCase(Locale.ROOT); }
}
