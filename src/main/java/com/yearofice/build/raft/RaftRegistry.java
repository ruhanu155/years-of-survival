package com.yearofice.build.raft;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The raft entity type. */
public final class RaftRegistry {
    private RaftRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<RaftEntity>> RAFT = ENTITIES.register("raft",
            () -> EntityType.Builder.<RaftEntity>of(RaftEntity::new, MobCategory.MISC).sized(1.8f, 0.4f).clientTrackingRange(10).build("raft"));

    public static void register(IEventBus modBus) { ENTITIES.register(modBus); }
}
