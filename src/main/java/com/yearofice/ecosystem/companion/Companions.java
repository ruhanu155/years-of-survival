package com.yearofice.ecosystem.companion;

import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.sim.time.Calendar;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The wolf that left in February and will come back (the Standard preset): its whole entity record waits on the
 * player's persisted data under {@code yearofice:wolf_away} with a {@code return_day}; the wolf keeps its id and its
 * trust, and walks back into camp at 40 trust or more.
 */
public final class Companions {
    private Companions() {}

    static final String KEY = "yearofice:wolf_away";

    private static CompoundTag root(ServerPlayer p) { return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG); }

    static void store(ServerPlayer p, WolfEntity w, long returnDay) {
        CompoundTag wolf = new CompoundTag();
        w.saveWithoutId(wolf);
        wolf.putString("id", EntityType.getKey(w.getType()).toString());
        CompoundTag rec = new CompoundTag();
        rec.put("wolf", wolf);
        rec.putLong("return_day", returnDay);
        CompoundTag r = root(p);
        r.put(KEY, rec);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, r);
    }

    public static boolean away(ServerPlayer p) { return root(p).contains(KEY); }

    public static long returnDay(ServerPlayer p) { return away(p) ? root(p).getCompound(KEY).getLong("return_day") : -1; }

    /** Hourly on each player: brings the wolf back when its day comes. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 200 != 0 || !away(p)) return;
        ServerLevel level = p.serverLevel();
        long day = Calendar.absDayOfMinute(SliceClock.minute(level));
        if (day >= returnDay(p)) bring(p, level);
    }

    /** Spawns the stored wolf 30-40 m from the player (it walks into camp). */
    public static WolfEntity bring(ServerPlayer p, ServerLevel level) {
        if (!away(p)) return null;
        CompoundTag rec = root(p).getCompound(KEY);
        CompoundTag tag = rec.getCompound("wolf");
        Optional<Entity> made = EntityType.create(tag, level);
        CompoundTag r = root(p);
        r.remove(KEY);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, r);
        if (made.isEmpty() || !(made.get() instanceof WolfEntity w)) return null;
        double a = level.random.nextDouble() * Math.PI * 2;
        w.moveTo(p.getX() + Math.cos(a) * 35, p.getY(), p.getZ() + Math.sin(a) * 35, 0, 0);
        w.setOwner(p);
        if (w.trust().value() < 40) w.trust().set(40);
        level.addFreshEntity(w);
        long day = Calendar.absDayOfMinute(SliceClock.minute(level));
        com.yearofice.story.Journal.record(p, "wolf", "returned", "Day " + (day + 1) + ". "
                + (w.wolfName().isEmpty() ? "The wolf" : w.wolfName()) + " came back out of the trees.", SliceClock.minute(level), (int) day);
        p.displayClientMessage(Component.literal((w.wolfName().isEmpty() ? "Your wolf" : w.wolfName()) + " is back."), false);
        return w;
    }
}
