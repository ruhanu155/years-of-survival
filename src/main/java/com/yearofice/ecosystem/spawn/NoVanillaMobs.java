package com.yearofice.ecosystem.spawn;

import com.yearofice.core.YearOfIce;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * No monsters (Minecraft engine › No monsters, step 3, and Content pipeline › Foreign mobs): the catch-all on
 * {@code EntityJoinLevelEvent} cancels any entity in {@code #yearofice:banned_vanilla} and any mob outside our
 * namespace, so spawn eggs, commands, natural spawning, patrols, traders and phantoms all come to nothing. A second
 * listener, last in line, counts the vanilla mobs that still joined: the no-monsters check reads it.
 */
public final class NoVanillaMobs {
    private NoVanillaMobs() {}

    public static final TagKey<EntityType<?>> BANNED = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "banned_vanilla"));

    /** Vanilla mobs (or banned entities) that joined a level since the last reset. */
    public static final AtomicInteger JOINED = new AtomicInteger();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, NoVanillaMobs::onJoin);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, NoVanillaMobs::count);
    }

    public static boolean banned(Entity e) {
        if (e.getType().is(BANNED)) return true;
        return e instanceof Mob && !BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(YearOfIce.MOD_ID);
    }

    static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && banned(e.getEntity())) e.setCanceled(true);
    }

    static void count(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && banned(e.getEntity())) JOINED.incrementAndGet();
    }
}
