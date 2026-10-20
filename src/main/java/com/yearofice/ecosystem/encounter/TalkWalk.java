package com.yearofice.ecosystem.encounter;

import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.core.YearOfIce;
import com.yearofice.core.hash.Mulberry32;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.net.payload.TalkPayload;
import com.yearofice.sim.ecosystem.TalkWalk.Outcome;
import com.yearofice.sim.story.WarnOnce;
import com.yearofice.sound.YoiSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Talk as you walk (B7): the double-tap V toggle arrives on {@code yearofice:talk}; while on, the survivor's voice goes
 * out every 15 game seconds at 80 m (50 m in dense cover, half beside rapids), every animal inside gains +40 awareness,
 * and a bear or moose that hears the player first turns Alert and moves off 85% of the time (70% for a sow), with the
 * "surprised inside 25 m" trigger switched off for it ({@link #meet}). Talking scares game and no call works.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class TalkWalk {
    private TalkWalk() {}

    public static final double AWARENESS_GAIN = 40;
    public static final double PICKING_SPEED_MULT = 0.95;
    private static final Set<UUID> TALKING = new HashSet<>();
    private static final Map<UUID, WarnOnce> WARN = new HashMap<>();

    public static boolean isTalking(ServerPlayer p) { synchronized (TALKING) { return TALKING.contains(p.getUUID()); } }

    public static void set(ServerPlayer p, boolean on) {
        synchronized (TALKING) { if (on) TALKING.add(p.getUUID()); else TALKING.remove(p.getUUID()); }
    }

    public static void clear() { synchronized (TALKING) { TALKING.clear(); } WARN.clear(); }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToServer(TalkPayload.TYPE, TalkPayload.STREAM_CODEC, (p, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) set(sp, p.walk());
        });
    }

    /** Dense cover: willow, alder, a berry patch or any leaves within two blocks of the player. */
    public static boolean denseCover(ServerLevel level, BlockPos at) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-2, 0, -2), at.offset(2, 2, 2))) {
            var s = level.getBlockState(p);
            if (s.is(BlockTags.LEAVES) || s.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)) n++;
        }
        return n >= 6;
    }

    public static double radius(ServerLevel level, BlockPos at, boolean rapids) {
        return com.yearofice.sim.ecosystem.TalkWalk.radius(denseCover(level, at), rapids, EncounterConfig.talk());
    }

    /**
     * The meeting of a player and an unaware animal at {@code metres}: what the bear or moose does. The animal's brain
     * calls this before its "surprised inside 25 m" rule; {@link Outcome#SURPRISED_DEFENSIVE} never comes back while
     * the player is talking and inside the heard radius.
     */
    public static Outcome meet(ServerPlayer p, double metres, boolean cover, boolean rapids, boolean sow, Mulberry32 r) {
        var t = EncounterConfig.talk();
        return com.yearofice.sim.ecosystem.TalkWalk.approach(isTalking(p), metres, com.yearofice.sim.ecosystem.TalkWalk.radius(cover, rapids, t), sow, t, r);
    }

    /** One voice: the sound, and +40 awareness to every animal inside the heard radius. Returns the animals reached. */
    public static int emit(ServerPlayer p, boolean rapids) {
        ServerLevel level = p.serverLevel();
        double r = radius(level, p.blockPosition(), rapids);
        level.playSeededSound(null, p.getX(), p.getY(), p.getZ(), YoiSounds.TALK_WALK.get(), SoundSource.PLAYERS, 0.8f, 1f, level.random.nextLong());
        int n = 0;
        long tick = level.getGameTime();
        for (YoiAnimal a : level.getEntitiesOfClass(YoiAnimal.class, new AABB(p.blockPosition()).inflate(r), x -> x.distanceTo(p) <= r)) {
            double cur = a.awareness().of(p);
            a.awareness().spike(p, cur + AWARENESS_GAIN, tick);
            n++;
        }
        return n;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        long minute = SliceClock.minute(p.serverLevel());
        // Warn once: thick brush in the grizzly season (July 15 - September 15), until the player talks.
        int doy = com.yearofice.sim.time.Calendar.dayOfYearOfMinute(minute);
        boolean season = doy >= 196 && doy <= 258;
        boolean brush = season && !isTalking(p) && denseCover(p.serverLevel(), p.blockPosition());
        if (WARN.computeIfAbsent(p.getUUID(), k -> new WarnOnce()).update("talk_in_brush", brush, minute) && HudBus.installed())
            HudBus.get().post(p, HudLine.of(HudLineType.WARN, "talk_in_brush", "encounter"));
        if (!isTalking(p)) return;
        // 15 game seconds, but never faster than once every 2 real seconds.
        long periodS = Math.max(2, Math.round(15 * SliceClock.TICKS_PER_MINUTE / 60.0 / 20.0));
        if ((p.tickCount / 20) % periodS == 0) emit(p, false);
    }
}
