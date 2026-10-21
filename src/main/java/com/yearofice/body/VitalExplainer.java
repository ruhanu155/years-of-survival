package com.yearofice.body;

import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.part.Part;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every vital change explained (Blueprint › What the HUD shows): when a strip icon turns on, a part falls into a
 * worse band or a digit is lost, one Messenger line says what and why. One line per change: only the crossing posts,
 * and the bus drops the same line inside 10 game minutes.
 */
public final class VitalExplainer {
    private VitalExplainer() {}

    private record Last(boolean cold, boolean thirsty, boolean tired, boolean hurt, boolean hungry, int[] band, int digits) {}

    private static final Map<UUID, Last> LAST = new ConcurrentHashMap<>();

    public static void forget(UUID id) { LAST.remove(id); SLEPT.remove(id); }

    public static void check(net.minecraft.server.level.ServerPlayer p, BodyState s, long minute) {
        boolean cold = s.warmth < 70, thirsty = s.water < 60, tired = s.energy < 5, hurt = s.condition() < 60;
        boolean hungry = s.balance24h() < -1000;
        int[] band = new int[Part.COUNT];
        for (Part part : Part.values()) band[part.ordinal()] = s.parts.band(part).ordinal();
        int digits = Integer.bitCount(s.parts.lostDigits()[0]) + Integer.bitCount(s.parts.lostDigits()[1]);
        Last was = LAST.put(p.getUUID(), new Last(cold, thirsty, tired, hurt, hungry, band, digits));
        if (was == null || !HudBus.installed()) return;
        HudBus bus = HudBus.get();
        if (cold && !was.cold) say(bus, p, "warmth_low", HudArg.of(Math.round(s.warmth)));
        if (thirsty && !was.thirsty) say(bus, p, "water_low", HudArg.of(Math.round(s.water)));
        if (tired && !was.tired) say(bus, p, "energy_low", HudArg.fixed(s.energy, ""));
        if (hurt && !was.hurt) say(bus, p, "condition_low", HudArg.of(Math.round(s.condition())));
        if (hungry && !was.hungry) say(bus, p, "calories_low", HudArg.of(Math.round(s.balance24h())));
        if (!cold && was.cold) say(bus, p, "warmth_ok", HudArg.of(Math.round(s.warmth)));
        if (!thirsty && was.thirsty) say(bus, p, "water_ok", HudArg.of(Math.round(s.water)));
        if (!tired && was.tired) say(bus, p, "energy_ok", HudArg.fixed(s.energy, ""));
        if (!hurt && was.hurt) say(bus, p, "condition_ok", HudArg.of(Math.round(s.condition())));
        if (!hungry && was.hungry) say(bus, p, "calories_ok", HudArg.of(Math.round(s.balance24h())));
        for (Part part : Part.values()) {
            int i = part.ordinal();
            if (band[i] < was.band[i]) {
                say(bus, p, "part_better", HudArg.key("body.yearofice.part." + part.name().toLowerCase(java.util.Locale.ROOT)),
                        HudArg.of(s.parts.hp(part)));
            }
            if (band[i] > was.band[i]) {
                say(bus, p, "part_worse", HudArg.key("body.yearofice.part." + part.name().toLowerCase(java.util.Locale.ROOT)),
                        HudArg.of(s.parts.hp(part)));
            }
        }
        if (digits > was.digits) say(bus, p, "digit_lost", HudArg.of(digits));
    }

    private static void say(HudBus bus, net.minecraft.server.level.ServerPlayer p, String id, HudArg... args) {
        deliver(bus, p, HudLine.of(HudLineType.BODY, id, "body", args));
    }

    /** Lines raised while the player sleeps wait here (a sleeper sees no HUD) and post at wake. */
    private static final Map<UUID, java.util.List<HudLine>> SLEPT = new ConcurrentHashMap<>();
    private static final int MAX_SLEPT = 12;

    /** Posts {@code line} now, or holds it for the wake when the player is asleep. */
    public static void deliver(HudBus bus, net.minecraft.server.level.ServerPlayer p, HudLine line) {
        if (p.isSleeping()) {
            var q = SLEPT.computeIfAbsent(p.getUUID(), k -> new java.util.concurrent.CopyOnWriteArrayList<>());
            if (q.size() < MAX_SLEPT) q.add(line);
        } else bus.post(p, line);
    }

    /** At wake: a "while you slept" line, then what happened, in order. */
    public static void flush(net.minecraft.server.level.ServerPlayer p) {
        if (p.isSleeping() || !HudBus.installed()) return;
        var q = SLEPT.remove(p.getUUID());
        if (q == null || q.isEmpty()) return;
        HudBus bus = HudBus.get();
        bus.post(p, HudLine.of(HudLineType.BODY, "while_slept", "body"));
        for (HudLine l : q) bus.post(p, l);
    }
}
