package com.yearofice.body;

import com.google.gson.JsonObject;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.body.clock.BodyClock;
import com.yearofice.body.net.BodyNet;
import com.yearofice.body.net.SosFlightPayload;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.sim.body.BodyModel;
import com.yearofice.sim.body.BodyState;
import com.yearofice.story.Journal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Collapse and the Endless SOS (Minecraft development › Collapse, SOS and the flight), a server state machine:
 * COLLAPSE (input held, 4 real seconds) → FLIGHT (spectator, the flight screen and the "why I left" line) → the
 * jump (the left pack, 14 game days in closed form, the return on the gravel bar with the starting kit, the
 * journal). Hardcore ends the run instead: the world flag, a spectator, a run record.
 *
 * <p>A player the server is not ticking (a GameTest bot) runs straight through in one call; a live player is
 * driven by {@link #onServerTick}. {@link #advance} is the one step either way.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class SosFlow {
    private SosFlow() {}

    public enum Cause {
        STARVATION("starvation"), HYPOTHERMIA("hypothermia"), CONDITION("condition"), DROWNING("drowning"),
        COMMAND("command"), FELL("fell"), MANUAL("manual");

        public final String id;

        Cause(String id) { this.id = id; }
    }

    public enum Phase { COLLAPSE, FLIGHT }

    static final class Flight {
        final Cause cause;
        Phase phase = Phase.COLLAPSE;
        int ticks;
        String line = "";
        boolean lineIn;
        final ResourceKey<net.minecraft.world.level.Level> dim;
        final BlockPos spot;

        Flight(Cause cause, ServerPlayer p) {
            this.cause = cause;
            this.dim = p.level().dimension();
            this.spot = p.blockPosition();
        }
    }

    public static final String FLIGHTS_KEY = "yearofice:flights_out";
    private static final Map<UUID, Flight> FLIGHTS = new ConcurrentHashMap<>();

    /** Tests flip Hardcore on without a second world. */
    public static volatile BooleanSupplier hardcoreOverride = null;

    public static boolean hardcore(ServerPlayer p) {
        BooleanSupplier o = hardcoreOverride;
        return o != null ? o.getAsBoolean() : p.serverLevel().getLevelData().isHardcore();
    }

    public static boolean active(ServerPlayer p) { return FLIGHTS.containsKey(p.getUUID()); }

    /** Starts the collapse. Ignored while one is already under way. */
    public static void trigger(ServerPlayer p, Cause cause) {
        BodyState s = BodyAttachments.of(p);
        if (s.sos != 0) return;
        s.sos = 1;
        Flight f = new Flight(cause, p);
        FLIGHTS.put(p.getUUID(), f);
        YearOfIce.LOG.info("SOS: {} collapsing ({})", p.getGameProfile().getName(), cause.id);
        if (p.server.getPlayerList().getPlayer(p.getUUID()) != p) {
            for (int i = 0; i < 4 && FLIGHTS.containsKey(p.getUUID()); i++) advance(p, true);
        }
        BodyTicker.sync(p, BodyTicker.clock.minute(p.server), true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (FLIGHTS.isEmpty()) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (FLIGHTS.containsKey(p.getUUID())) advance(p, false);
        }
    }

    /** The "why I left" line from the flight screen: ends the wait. */
    public static void onLine(ServerPlayer p, String line) {
        Flight f = FLIGHTS.get(p.getUUID());
        if (f == null || f.phase != Phase.FLIGHT) return;
        f.line = clean(line);
        f.lineIn = true;
    }

    static String clean(String line) {
        String t = line == null ? "" : line.replaceAll("[\\p{Cntrl}]", " ").strip();
        int max = SosConfig.lineMax();
        return t.length() > max ? t.substring(0, max) : t;
    }

    /** One step of the machine; {@code rush} finishes the current phase now. */
    public static void advance(ServerPlayer p, boolean rush) {
        Flight f = FLIGHTS.get(p.getUUID());
        if (f == null) return;
        f.ticks++;
        if (f.phase == Phase.COLLAPSE) {
            p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
            p.hurtMarked = true;
            if (rush || f.ticks >= SosConfig.collapseTicks()) sent(p, f);
        } else if (rush || f.lineIn || f.ticks >= SosConfig.flightTicks()) {
            jump(p, f);
        }
    }

    private static void sent(ServerPlayer p, Flight f) {
        long minute = BodyTicker.clock.minute(p.server);
        String when = BodyClock.hhmm(minute);
        int day = BodyClock.day(minute);
        HudBus.get().post(p, HudLine.of(HudLineType.WARN, "sos_sent", "body", HudArg.text(when), HudArg.of(day),
                HudArg.key("messenger.yearofice.cause." + f.cause.id)));
        boolean hard = hardcore(p);
        if (hard) {
            endRun(p, f, minute, day);
            return;
        }
        f.phase = Phase.FLIGHT;
        f.ticks = 0;
        p.setGameMode(GameType.SPECTATOR);
        BodyNet.sendSos(p, new SosFlightPayload(f.cause.id, when, day, false));
    }

    private static void endRun(ServerPlayer p, Flight f, long minute, int day) {
        RunState.get(p.server).end(f.cause.id, minute);
        p.setGameMode(GameType.SPECTATOR);
        BodyAttachments.of(p).sos = 2;
        Journal.record(p, Journal.RUN_ENDED, f.cause.id, "", minute, day);
        HudBus.get().post(p, HudLine.of(HudLineType.WARN, "run_ended", "body"));
        writeRunRecord(p, f, minute, day);
        BodyNet.sendSos(p, new SosFlightPayload(f.cause.id, BodyClock.hhmm(minute), day, true));
        FLIGHTS.remove(p.getUUID());
        BodyTicker.sync(p, minute, true);
    }

    /** {@code <world>/yearofice/run_record.json}, copied to {@code <game dir>/yearofice/records/}. */
    static void writeRunRecord(ServerPlayer p, Flight f, long minute, int day) {
        JsonObject o = new JsonObject();
        o.addProperty("player", p.getGameProfile().getName());
        o.addProperty("cause", f.cause.id);
        o.addProperty("minute", minute);
        o.addProperty("day", day);
        o.addProperty("flights_out", flightsOut(p));
        o.addProperty("bmi", BodyAttachments.of(p).bmi());
        o.addProperty("version", YearOfIce.MOD_ID);
        String json = o.toString();
        try {
            Path world = p.server.getWorldPath(LevelResource.ROOT).resolve("yearofice");
            Files.createDirectories(world);
            Files.writeString(world.resolve("run_record.json"), json);
            Path records = FMLPaths.GAMEDIR.get().resolve("yearofice").resolve("records");
            Files.createDirectories(records);
            Files.writeString(records.resolve("run_" + p.getGameProfile().getName() + "_d" + day + ".json"), json);
        } catch (Exception ex) {
            YearOfIce.LOG.warn("Could not write the run record: {}", ex.toString());
        }
    }

    /** The jump: the left pack, 14 game days, the return, the kit, the journal. */
    private static void jump(ServerPlayer p, Flight f) {
        FLIGHTS.remove(p.getUUID());
        BodyState s = BodyAttachments.of(p);
        long before = BodyTicker.clock.minute(p.server);
        List<ItemStack> left = takeCarried(p);
        ServerLevel from = p.server.getLevel(f.dim) != null ? p.server.getLevel(f.dim) : p.serverLevel();
        if (!left.isEmpty()) LeftPack.place(from, f.spot, left);
        if (ClockService.installed()) ClockService.get().advance(SosConfig.awayDays() * 1440L);
        BodyTicker.resync(p.server);
        long minute = BodyTicker.clock.minute(p.server);
        BodyModel.sosReturn(s, BodyConfig.tuning());
        s.sos = 3;
        teleportToReturnPoint(p);
        p.setGameMode(GameType.SURVIVAL);
        p.clearFire();
        p.setHealth(p.getMaxHealth());
        p.resetFallDistance();
        giveStartingKit(p);
        com.yearofice.body.clothing.StartOutfit.dress(p);
        countFlight(p);
        Journal.record(p, Journal.FLIGHT, f.cause.id, f.line, before, BodyClock.day(before));
        YearOfIce.LOG.info("SOS: {} back after {} min, {} stacks left at {}", p.getGameProfile().getName(), minute - before,
                left.size(), f.spot);
        s.sos = 0;
        BodyTicker.sync(p, minute, true);
    }

    /** Everything carried except worn armour and the messenger. */
    static List<ItemStack> takeCarried(ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.items.size(); i++) take(inv.items, i, out);
        for (int i = 0; i < inv.offhand.size(); i++) take(inv.offhand, i, out);
        // The Curios back-pack slot and any other neck/back slot (the inner pocket) go into the left pack too.
        for (String slot : new String[] {com.yearofice.item.inventory.PackSlots.BACK, com.yearofice.item.inventory.PackSlots.NECK}) {
            for (int i = 0; i < com.yearofice.item.inventory.PackSlots.size(p, slot); i++) {
                ItemStack st = com.yearofice.item.inventory.PackSlots.get(p, slot, i);
                if (st.isEmpty() || st.is(MessengerItems.MESSENGER.get())) continue;
                out.add(st.copy());
                com.yearofice.item.inventory.PackSlots.set(p, slot, i, ItemStack.EMPTY);
            }
        }
        return out;
    }

    private static void take(net.minecraft.core.NonNullList<ItemStack> list, int i, List<ItemStack> out) {
        ItemStack st = list.get(i);
        if (st.isEmpty() || st.is(MessengerItems.MESSENGER.get())) return;
        out.add(st.copy());
        list.set(i, ItemStack.EMPTY);
    }

    static void giveStartingKit(ServerPlayer p) {
        ResourceKey<net.minecraft.world.level.storage.loot.LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "player/starting_kit"));
        LootParams params = new LootParams.Builder(p.serverLevel()).create(LootContextParamSets.EMPTY);
        for (ItemStack st : p.server.reloadableRegistries().getLootTable(key).getRandomItems(params)) {
            if (!p.getInventory().add(st)) p.drop(st, false);
        }
    }

    /** The return point: the gravel bar below the outpost (start-site search); the overworld spawn without one. */
    public static void teleportToReturnPoint(ServerPlayer p) {
        ServerLevel overworld = p.server.overworld();
        int[] bar = null;
        try {
            com.yearofice.worldgen.ValleyWorld w = com.yearofice.worldgen.gen.ValleyWorlds.forLevelSeed(overworld.getSeed());
            bar = w == null ? null : w.sosReturn();
        } catch (RuntimeException | LinkageError ignored) {
            // no valley world in this level (flat test worlds)
        }
        if (bar != null) {
            p.teleportTo(overworld, bar[0] + 0.5, bar[1] + 1, bar[2] + 0.5, p.getYRot(), p.getXRot());
            return;
        }
        BlockPos spawn = overworld.getSharedSpawnPos();
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        p.teleportTo(overworld, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, p.getYRot(), p.getXRot());
    }

    static void countFlight(ServerPlayer p) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(FLIGHTS_KEY, persisted.getInt(FLIGHTS_KEY) + 1);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static int flightsOut(ServerPlayer p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(FLIGHTS_KEY);
    }

    /** A flight cut short by a quit finishes at the next login: the screen is gone, so it goes straight to the jump. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || FLIGHTS.containsKey(p.getUUID())) return;
        if (BodyAttachments.of(p).sos != 1) return;
        Flight f = new Flight(Cause.MANUAL, p);
        f.phase = Phase.FLIGHT;
        f.lineIn = true;
        FLIGHTS.put(p.getUUID(), f);
    }
}
