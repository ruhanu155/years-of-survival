package com.yearofice.client.minigame.fishing;

import com.yearofice.core.YearOfIce;
import com.yearofice.hunt.fishing.BobberEntity;
import com.yearofice.hunt.fishing.FightSession;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.hunt.fishing.net.FishInputPayload;
import com.yearofice.hunt.fishing.net.FishResultPayload;
import com.yearofice.hunt.fishing.net.FishStatePayload;
import com.yearofice.sim.fishing.FightModel;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The client's half of the rod (Blueprint › Fishing methods › The fight, as built; Minecraft development › Minigames
 * in Minecraft terms). Before a fight, left mouse with the rod and a bobber out sends the strike. Once the server
 * opens a fight, the client runs the same lockstep {@link FightModel} each tick on its own inputs (left mouse held
 * reels, released gives line; right mouse held lifts the rod, pressed grabs; W walks the bank toward the fish),
 * sends the input bytes every 4 ticks, and snaps to the server's state whenever the two differ. It never decides
 * an outcome: the server's {@code fish_result} closes the overlay.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class FishingClient {
    private FishingClient() {}

    static final int HISTORY = 200;
    static final int RESULT_SHOW_TICKS = 30;
    static final int ORPHAN_TICKS = 100;

    /** One fight as the client runs it. */
    static final class Fight {
        final int id;
        final FightModel.Params params;
        FightModel.State state;
        final Map<Integer, FightModel.State> history = new HashMap<>();
        final Map<Integer, Byte> inputs = new HashMap<>();
        final ByteArrayOutputStream pending = new ByteArrayOutputStream();
        int pendingFirst = -1;
        boolean useWasDown = true;
        @Nullable FightModel.Outcome result;
        int resultTicks;
        int overTicks;

        Fight(int id, FightModel.Params params, FightModel.State start) {
            this.id = id;
            this.params = params;
            this.state = start;
            history.put(start.tick(), start);
        }
    }

    private static @Nullable Fight fight;
    private static long lastStrikeTick = -100;

    static @Nullable Fight current() { return fight; }

    /** A fight is open and not yet decided: the rod's mouse buttons belong to it. */
    public static boolean active() { return fight != null && fight.result == null; }

    public static void onState(FishStatePayload p) {
        if (p.open() != null) {
            fight = new Fight(p.fightId(), p.open(), p.state());
            return;
        }
        Fight f = fight;
        if (f == null || f.id != p.fightId()) return;
        reconcile(f, p.state());
    }

    /** Lockstep check: the server's state at its tick must equal ours there; if not, take it and replay our inputs. */
    static void reconcile(Fight f, FightModel.State server) {
        FightModel.State mine = f.history.get(server.tick());
        if (server.equals(mine)) return;
        int now = f.state.tick();
        FightModel.State x = server;
        f.history.put(x.tick(), x);
        for (int t = server.tick() + 1; t <= now && !x.over(); t++) {
            Byte b = f.inputs.get(t);
            if (b == null) break;
            x = FightModel.step(f.params, x, FightSession.sanitize(b, f.params.assist()));
            f.history.put(t, x);
        }
        f.state = x;
    }

    public static void onResult(FishResultPayload p) {
        Fight f = fight;
        if (f == null || f.id != p.fightId()) return;
        f.result = p.outcome();
        f.resultTicks = RESULT_SHOW_TICKS;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Fight f = fight;
        if (f == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            fight = null;
            return;
        }
        if (f.result != null) {
            if (--f.resultTicks <= 0) fight = null;
            return;
        }
        if (f.state.over()) {
            flush(f);
            if (++f.overTicks > ORPHAN_TICKS) fight = null;
            return;
        }
        boolean free = mc.screen == null;
        boolean retrieve = free && mc.options.keyAttack.isDown();
        boolean useDown = free && mc.options.keyUse.isDown();
        boolean grab = useDown && !f.useWasDown;
        f.useWasDown = useDown;
        boolean walk = free && mc.options.keyUp.isDown();
        FightModel.Input in = new FightModel.Input(retrieve, useDown, FightSession.side(f.params.assist()), walk, grab);
        byte b = in.toByte();
        int t = f.state.tick() + 1;
        f.inputs.put(t, b);
        f.state = FightModel.step(f.params, f.state, FightSession.sanitize(b, f.params.assist()));
        f.history.put(t, f.state);
        f.history.remove(t - HISTORY);
        f.inputs.remove(t - HISTORY);
        if (f.pendingFirst < 0) f.pendingFirst = t;
        f.pending.write(b);
        if (f.pending.size() >= FightSession.SEND_EVERY || f.state.over()) flush(f);
    }

    private static void flush(Fight f) {
        if (f.pending.size() == 0) return;
        PacketDistributor.sendToServer(new FishInputPayload(f.id, f.pendingFirst, f.pending.toByteArray()));
        f.pending.reset();
        f.pendingFirst = -1;
    }

    /** The rod's mouse: the strike before a fight, and nothing vanilla during one. */
    @SubscribeEvent
    public static void click(InputEvent.InteractionKeyMappingTriggered e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (active()) {
            if (e.isAttack() || e.isUseItem()) {
                e.setCanceled(true);
                e.setSwingHand(false);
            }
            return;
        }
        if (!e.isAttack() || !p.getMainHandItem().is(FishingRegistry.ROD.get()) || !BobberEntity.clientHasBobber(p)) return;
        e.setCanceled(true);
        long now = mc.level.getGameTime();
        if (now - lastStrikeTick >= 5) {
            lastStrikeTick = now;
            e.setSwingHand(true);
            PacketDistributor.sendToServer(FishInputPayload.strike());
        } else {
            e.setSwingHand(false);
        }
    }
}
