package com.yearofice.client.story;

import com.yearofice.body.net.BodySyncPayload;
import com.yearofice.client.ui.messenger.Fmt;
import com.yearofice.client.ui.messenger.Lcd;
import com.yearofice.client.ui.messenger.Pal;
import com.yearofice.sim.story.FirstNightStep;
import com.yearofice.story.net.OnboardingSyncPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * The Trail Messenger's Guide page (Blueprint › Start site, Messenger voice: "Each line shows once; the Guide tab in
 * the messenger (M) replays the current step"): the step count, the current step's line in full, the six steps with
 * the done ones ticked, and today's sunset. After the night it says guidance is off.
 */
public final class GuidePage {
    private GuidePage() {}

    static Component tr(String key, Object... args) { return Component.translatable("messenger.yearofice.guide." + key, args); }

    /** Draws the page; {@code MessengerPage.GUIDE} calls it. */
    public static void render(Lcd l, BodySyncPayload body) {
        OnboardingSyncPayload p = ClientOnboarding.get();
        if (!ClientOnboarding.active(p)) {
            l.text(tr("off"), Pal.DIM);
            l.row();
            return;
        }
        FirstNightStep now = ClientOnboarding.step(p);
        l.text(tr("step", now.number(), FirstNightStep.COUNT), Pal.INK);
        l.right(tr("sunset", Fmt.hhmm(p.sunsetMinute())), Pal.INK);
        l.row();
        Component line = Component.translatable("messenger.yearofice.prompt.first_night_" + now.id);
        for (String row : wrap(line.getString(), l.w)) {
            l.text(row, Pal.INK);
            l.row();
        }
        if (now == FirstNightStep.SLEEP) {
            l.text(tr("near_fire", p.nearFireM()), Pal.DIM);
            l.row();
        }
        l.gap(2);
        l.rule();
        for (FirstNightStep s : FirstNightStep.values()) {
            boolean done = s.ordinal() < now.ordinal();
            String mark = done ? "x" : (s == now ? ">" : "-");
            l.text(mark, done ? Pal.GREEN : (s == now ? Pal.INK : Pal.DIM));
            l.textAt(10, tr(s.id), done ? Pal.DIM : (s == now ? Pal.INK : Pal.DIM));
            l.row();
        }
    }

    /** Greedy word wrap to the LCD width. */
    static List<String> wrap(String text, int width) {
        Font f = Minecraft.getInstance().font;
        List<String> out = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = row.isEmpty() ? word : row + " " + word;
            if (f.width(next) > width && !row.isEmpty()) {
                out.add(row.toString());
                row = new StringBuilder(word);
            } else {
                row = new StringBuilder(next);
            }
        }
        if (!row.isEmpty()) out.add(row.toString());
        return out;
    }
}
