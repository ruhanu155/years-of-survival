package com.yearofice.story;

import com.yearofice.item.component.TrophySize;
import com.yearofice.story.collection.RecordBook;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * First kills (Open world in depth › Firsts): the first time a player kills a species, one {@link Journal} line of
 * kind {@code first_kill} is written, and never again for that species. A kill with a trophy size also goes to the
 * record book's game page.
 */
public final class FirstKills {
    private FirstKills() {}

    public static final String KEY = "yearofice:first_kills";
    public static final String KIND = "first_kill";

    /** A player's kill of one of our animals writes the first-kill line. */
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) {
        if (!(e.getEntity() instanceof com.yearofice.ecosystem.YoiAnimal a) || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        long minute = com.yearofice.ecosystem.clock.SliceClock.minute(p.serverLevel());
        onKill(p, a.speciesPath(), e.getSource().getMsgId(), null, minute, (int) com.yearofice.sim.time.Calendar.absDayOfMinute(minute));
    }

    /** Records a kill of {@code species}; returns true if it was the first (and a journal line was written). */
    public static boolean onKill(ServerPlayer p, String species, String cause, TrophySize size, long minute, int day) {
        if (size != null) RecordBook.record(p, species, size, day);
        CompoundTag root = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag seen = root.getList(KEY, Tag.TAG_STRING);
        for (int i = 0; i < seen.size(); i++) if (seen.getString(i).equals(species)) return false;
        seen.add(StringTag.valueOf(species));
        root.put(KEY, seen);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
        Journal.record(p, KIND, species + ":" + cause, "First kill: " + species.replace('_', ' ') + ".", minute, day);
        return true;
    }
}
