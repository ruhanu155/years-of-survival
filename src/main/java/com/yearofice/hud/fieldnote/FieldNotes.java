package com.yearofice.hud.fieldnote;

import com.mojang.brigadier.CommandDispatcher;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.fire.FireStateEvent;
import com.yearofice.api.hud.FieldNoteApi;
import com.yearofice.api.hud.MessengerAccess;
import com.yearofice.core.YearOfIce;
import com.yearofice.hud.net.FieldNotePayload;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.hud.FieldNoteQueue;
import com.yearofice.sim.time.Rate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Field-note cards, server half (Creative concepts › Field-note cards; A8). Triggers (an item picked up, crafted or
 * eaten, a block placed, a fire lit near the player, or {@link FieldNoteApi}) file the note in the player's
 * notebook record at once and queue it; {@link FieldNoteQueue} decides when it shows. A card waits while the close
 * clock runs (which covers a danger alert) and while no working messenger is carried. The server decides; the
 * client only draws the card ({@code client.hud.FieldNoteCardLayer}).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class FieldNotes {
    private FieldNotes() {}

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);
    public static final Supplier<AttachmentType<FieldNoteRecord>> RECORD = TYPES.register("field_notes",
            () -> AttachmentType.builder(FieldNoteRecord::new).serialize(FieldNoteRecord.CODEC).copyOnDeath().build());

    /** Receives {@code field_note} on the client. */
    public static volatile Consumer<FieldNotePayload> clientSink = p -> { };

    private static final Map<UUID, FieldNoteQueue> QUEUES = new ConcurrentHashMap<>();

    public static void init(IEventBus modBus) {
        TYPES.register(modBus);
        com.yearofice.sound.YoiSounds.init(modBus);
        modBus.addListener(FieldNotes::registerPayloads);
        NeoForge.EVENT_BUS.addListener(FieldNotes::registerCommands);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new FieldNoteDefs()));
        FieldNoteApi.install(FieldNotes::fire);
    }

    static void registerPayloads(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToClient(FieldNotePayload.TYPE, FieldNotePayload.STREAM_CODEC, (p, ctx) -> clientSink.accept(p));
    }

    // ---- state ----------------------------------------------------------------------------------------------

    public static FieldNoteQueue queue(ServerPlayer p) {
        return QUEUES.computeIfAbsent(p.getUUID(), u -> {
            FieldNoteQueue q = new FieldNoteQueue();
            p.getData(RECORD.get()).filed.keySet().forEach(q::markKnown);
            return q;
        });
    }

    public static FieldNoteRecord record(ServerPlayer p) { return p.getData(RECORD.get()); }

    private static long gameMinute() { return ClockService.installed() ? ClockService.get().nowMinute() : 0; }

    // ---- triggers -------------------------------------------------------------------------------------------

    /** Fires note {@code noteId} for the player: files it in the notebook and queues the card. */
    public static boolean fire(ServerPlayer p, String noteId) {
        if (FieldNoteDefs.get(noteId) == null) return false;
        if (!queue(p).trigger(noteId, gameMinute())) return false;
        FieldNoteRecord r = record(p);
        r.filed.put(noteId, ClockService.installed() ? (int) ClockService.get().absDay() : 0);
        r.unseen.add(noteId);
        return true;
    }

    /** Fires every note whose trigger is {@code type} with {@code id}. */
    public static void trigger(ServerPlayer p, String type, String id) {
        for (FieldNoteDefs.Def d : FieldNoteDefs.forTrigger(type, id)) fire(p, d.id());
    }

    private static String idOf(net.minecraft.world.item.ItemStack s) { return BuiltInRegistries.ITEM.getKey(s.getItem()).toString(); }

    @SubscribeEvent
    public static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post e) {
        if (e.getPlayer() instanceof ServerPlayer p) trigger(p, "item_pickup", idOf(e.getOriginalStack()));
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) trigger(p, "item_crafted", idOf(e.getCrafting()));
    }

    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish e) {
        if (e.getEntity() instanceof ServerPlayer p) trigger(p, "item_eaten", idOf(e.getItem()));
    }

    @SubscribeEvent
    public static void onPlaced(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            trigger(p, "block_placed", BuiltInRegistries.BLOCK.getKey(e.getPlacedBlock().getBlock()).toString());
        }
    }

    @SubscribeEvent
    public static void onFire(FireStateEvent e) {
        if (e.from() == e.to() || e.to() == FireState.OUT || e.level().isClientSide) return;
        var near = e.level().getNearestPlayer(e.pos().getX() + .5, e.pos().getY(), e.pos().getZ() + .5, 10, false);
        if (near instanceof ServerPlayer p) trigger(p, "fire_state", e.to().name().toLowerCase(java.util.Locale.ROOT));
    }

    // ---- showing --------------------------------------------------------------------------------------------

    /** True when a card must wait: the close clock (a danger alert asks for it) or no working messenger. */
    public static boolean blocked(ServerPlayer p) {
        boolean close = ClockService.installed() && ClockService.get().rate(p) == Rate.CLOSE;
        return close || !MessengerAccess.live(p);
    }

    /** Shows the next card if the rules allow. Returns the note shown, or null. */
    public static String pump(ServerPlayer p, long nowTick) {
        var shown = queue(p).poll(nowTick, gameMinute(), blocked(p));
        if (shown.isEmpty()) return null;
        record(p).unseen.remove(shown.get());
        boolean first = !ClockService.installed() || ClockService.get().year() <= 1;
        if (p.connection != null && p.connection.hasChannel(FieldNotePayload.TYPE)) {
            PacketDistributor.sendToPlayer(p, new FieldNotePayload(shown.get(), first));
        }
        return shown.get();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        int t = e.getServer().getTickCount();
        if (t % 20 != 0) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) pump(p, t);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { QUEUES.remove(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { QUEUES.clear(); }

    // ---- /yoi fieldnote -------------------------------------------------------------------------------------

    static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("fieldnote")
                .then(Commands.literal("list").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    FieldNoteRecord r = record(p);
                    c.getSource().sendSuccess(() -> Component.literal(FieldNoteDefs.count() + " notes defined; "
                            + r.filed.size() + " filed, " + r.unseen.size() + " unseen, " + queue(p).pending() + " queued: "
                            + r.filed), false);
                    return r.filed.size();
                }))
                .then(Commands.literal("fire").then(Commands.argument("id", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            String id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(c, "id").toString();
                            boolean ok = fire(p, id);
                            c.getSource().sendSuccess(() -> Component.literal(ok ? "queued " + id : "unknown or already filed: " + id), false);
                            return ok ? 1 : 0;
                        })))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    FieldNoteRecord r = record(p);
                    r.filed.clear();
                    r.unseen.clear();
                    QUEUES.remove(p.getUUID());
                    c.getSource().sendSuccess(() -> Component.literal("field notes cleared"), false);
                    return 1;
                }))));
    }
}
