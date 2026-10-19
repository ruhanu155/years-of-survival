package com.yearofice.item;

import com.yearofice.api.clock.ClockService;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.inventory.PackConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Dropped items last 7 game days (reconcile 36; Vanilla systems we replace): vanilla's 5-real-minute despawn is
 * switched off on every dropped item, which is stamped with the game minute it fell; an hourly sweep, and the join
 * of an item loaded with its chunk, remove the expired ones. Items in {@code #yearofice:never_expires} (the
 * messenger, packs; keepsakes and notes as they ship) never expire. Sunk gear becoming {@code lost_gear} is M5.
 */
public final class ItemExpiry {
    private ItemExpiry() {}

    public static final String STAMP = "yearofice:dropped_min";
    public static final TagKey<Item> NEVER_EXPIRES = ItemTags.create(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "never_expires"));

    static long now() { return ClockService.installed() ? ClockService.get().nowMinute() : 0; }

    public static long stamp(ItemEntity e) {
        CompoundTag t = e.getPersistentData();
        if (!t.contains(STAMP)) t.putLong(STAMP, now());
        return t.getLong(STAMP);
    }

    public static void setStamp(ItemEntity e, long minute) { e.getPersistentData().putLong(STAMP, minute); }

    public static boolean expired(ItemEntity e) {
        if (e.getItem().is(NEVER_EXPIRES)) return false;
        return now() - stamp(e) >= Math.round(PackConfig.get("dropped_item_days") * 1440);
    }

    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ItemEntity item)) return;
        stamp(item);
        item.setUnlimitedLifetime();
        if (expired(item)) e.setCanceled(true);
    }

    /** Removes the expired items in every loaded level; returns how many. */
    public static int sweep(MinecraftServer server) {
        int n = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<ItemEntity> gone = new ArrayList<>();
            for (Entity e : level.getAllEntities()) if (e instanceof ItemEntity i && expired(i)) gone.add(i);
            for (ItemEntity i : gone) i.discard();
            n += gone.size();
        }
        return n;
    }
}
