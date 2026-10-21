package com.yearofice.hud;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.event.PromptOfferEvent;
import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.core.YearOfIce;
import com.yearofice.hud.messenger.MessengerAccess;
import com.yearofice.net.YoiNet;
import com.yearofice.net.payload.HudLinePayload;
import com.yearofice.sim.time.Rate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

/**
 * {@code hud.HudBus} (Layer 8): every line a server system has for a player. {@link #post} applies the bus rules
 * in this order: no trap lines; journal lines go to the journal; messenger lines need a live messenger in a neck
 * slot or a hand, else they go to the journal; the same key and arguments within 10 game minutes are dropped;
 * during the player's close clock a messenger line under priority 3 waits. What passes is flushed to the owner at
 * the end of the tick, one {@code hud_line} payload a line.
 *
 * <p>Pickup and prompt lines answer the player's own action, so they never wait for the close clock and need no
 * battery. The journal attachment is M4; until then the session keeps up to 200 journal lines a player.
 */
public final class HudBusImpl implements HudBus {
    static final int DEDUP_MIN = 10, SENT_KEEP = 32, JOURNAL_KEEP = 200;

    private static final class PlayerState {
        final Deque<HudLine> outbox = new ArrayDeque<>();
        final List<HudLine> held = new ArrayList<>();
        final Deque<HudLine> sent = new ArrayDeque<>();
        final Deque<HudLine> journal = new ArrayDeque<>();
        final Map<String, Long> lastShown = new HashMap<>();
        ServerPlayer player;
    }

    private final Map<UUID, PlayerState> players = new ConcurrentHashMap<>();

    private PlayerState state(ServerPlayer p) {
        PlayerState s = players.computeIfAbsent(p.getUUID(), k -> new PlayerState());
        s.player = p;
        return s;
    }

    static boolean fromTrap(String source) {
        return source.equals("trap") || source.startsWith("trap.") || source.startsWith("food.traps") || source.startsWith("traps");
    }

    private static long now() { return ClockService.installed() ? ClockService.get().nowMinute() : 0; }

    /** Append-only observer of every line offered to the bus (the proxy checks read it; null in play). */
    public static volatile java.util.function.BiConsumer<ServerPlayer, HudLine> tap = null;

    @Override
    public void post(ServerPlayer p, HudLine line) {
        var tp = tap;
        if (tp != null) tp.accept(p, line);
        if (fromTrap(line.source())) {
            if (!FMLEnvironment.production) throw new IllegalArgumentException("No trap catches on the HUD: " + line.key() + " from " + line.source());
            return;
        }
        PlayerState s = state(p);
        HudLineType.Channel ch = line.type().channel;
        if (ch == HudLineType.Channel.JOURNAL || ch == HudLineType.Channel.MESSENGER && !MessengerAccess.live(p)) {
            journal(s, line);
            return;
        }
        String dedup = line.key() + line.args();
        Long last = s.lastShown.get(dedup);
        long m = now();
        if (ch == HudLineType.Channel.MESSENGER && last != null && m - last < DEDUP_MIN) return;
        if (ch == HudLineType.Channel.MESSENGER) s.lastShown.put(dedup, m);
        if (ch == HudLineType.Channel.MESSENGER && line.priority() < 3 && closeClock(p)) {
            s.held.add(line);
            return;
        }
        s.outbox.add(line);
    }

    static boolean closeClock(ServerPlayer p) {
        return ClockService.installed() && ClockService.get().rate(p) == Rate.CLOSE;
    }

    private static void journal(PlayerState s, HudLine line) {
        s.journal.add(line);
        while (s.journal.size() > JOURNAL_KEEP) s.journal.removeFirst();
    }

    @Override
    public void offerPrompt(ServerPlayer p, String key, String text, int priority) {
        if (NeoForge.EVENT_BUS.post(new PromptOfferEvent(p, key, text, priority)).isCanceled()) return;
        HudLine line = new HudLine(HudLineType.PROMPT, Math.max(0, Math.min(3, priority)), key, List.of(HudArg.text(text)), "prompt");
        state(p).outbox.add(line);
    }

    @Override
    public void clearPrompt(ServerPlayer p, String key) {
        state(p).outbox.add(new HudLine(HudLineType.PROMPT, 0, key, List.of(), HudLinePayload.CLEAR));
    }

    @Override public List<HudLine> journal(ServerPlayer p) { return List.copyOf(state(p).journal); }

    @Override public List<HudLine> sent(ServerPlayer p) { return List.copyOf(state(p).sent); }

    @Override public List<HudLine> held(ServerPlayer p) { return List.copyOf(state(p).held); }

    /** End of the server tick: release held lines whose close clock ended, then send every outbox. */
    void flush(MinecraftServer server) {
        for (PlayerState s : players.values()) {
            ServerPlayer p = s.player;
            if (p == null) continue;
            if (!s.held.isEmpty() && !closeClock(p)) {
                s.outbox.addAll(s.held);
                s.held.clear();
            }
            while (!s.outbox.isEmpty()) {
                HudLine line = s.outbox.removeFirst();
                s.sent.add(line);
                while (s.sent.size() > SENT_KEEP) s.sent.removeFirst();
                YoiNet.send(p, HudLinePayload.of(line));
            }
        }
    }

    void forget(UUID id) { players.remove(id); }

    void clear() { players.clear(); }

    static void log(String s) { YearOfIce.LOG.debug("[hud] {}", s); }
}
