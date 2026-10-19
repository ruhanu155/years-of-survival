package com.yearofice.build.wood;

import com.yearofice.api.event.ClockEvent;
import com.yearofice.build.work.WorkProgressPayload;
import com.yearofice.build.work.WorkService;
import com.yearofice.build.work.WorkStartPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Building's M2 wood system: felling and bucking, A1 placed logs never fall, rounds to splits on the chopping block,
 * B4 seasoning of the pieces, hold-to-work and its payloads, and {@code /yoi tree} and {@code /yoi wood}.
 */
public final class WoodSystem {
    private WoodSystem() {}

    public static void init(IEventBus modBus) {
        WoodRegistry.register(modBus);
        modBus.addListener(RegisterDataMapTypesEvent.class, ToolData::register);
        modBus.addListener(RegisterPayloadHandlersEvent.class, WoodSystem::payloads);
        WorkService.resolver = WoodJobs::resolve;
        NeoForge.EVENT_BUS.addListener(PlacedLogs::placed);
        NeoForge.EVENT_BUS.addListener(PlacedLogs::breakSpeed);
        NeoForge.EVENT_BUS.addListener(WoodSystem::rightClick);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> WorkService.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(WoodSystem::day);
        NeoForge.EVENT_BUS.addListener(WoodSystem::jump);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> { WorkService.reset(); FellingService.reset(); });
        NeoForge.EVENT_BUS.addListener(WoodCommands::register);
    }

    static void payloads(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToServer(WorkStartPayload.TYPE, WorkStartPayload.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) WorkService.beat(sp, p);
        }));
        r.playToClient(WorkProgressPayload.TYPE, WorkProgressPayload.STREAM_CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> WorkSink.sink.accept(p)));
    }

    /** A round on a stump turns the stump into a chopping block holding it. */
    static void rightClick(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getLevel() instanceof ServerLevel l) || !(e.getEntity() instanceof ServerPlayer p) || p.isShiftKeyDown()) return;
        if (WoodJobs.roundOnStump(l, e.getPos(), e.getItemStack(), p)) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Every game day the pieces players carry are settled (B4), so the shared moisture the fire reads stays current. */
    static void day(ClockEvent.Day e) { settleAll(e.absDay()); }

    static void jump(ClockEvent.Jump e) { settleAll(WoodItems.today()); }

    static void settleAll(long day) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) WoodItems.settle(p, day);
    }

    /** Where the client's work ring takes its payload; the client installs it, so a server never loads client code. */
    public static final class WorkSink {
        private WorkSink() {}

        public static volatile java.util.function.Consumer<WorkProgressPayload> sink = p -> { };
    }
}
