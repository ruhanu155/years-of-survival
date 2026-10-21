package com.yearofice.ecosystem.small;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.species.SmallGameEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * M7 lane C entity types (Land animals in depth › Mob sheets): the lemmings, singing vole, little brown bat and boreal
 * toad on the small-mammal and amphibian bodies; the three biting-insect swarms; the spawned-out salmon.
 */
public final class SmallLifeRegistry {
    private SmallLifeRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<LemmingEntity>> BROWN_LEMMING = ENTITIES.register("brown_lemming",
            () -> EntityType.Builder.of(LemmingEntity::new, MobCategory.CREATURE).sized(0.15f, 0.12f).clientTrackingRange(8).build("yearofice:brown_lemming"));
    public static final DeferredHolder<EntityType<?>, EntityType<LemmingEntity>> COLLARED_LEMMING = ENTITIES.register("collared_lemming",
            () -> EntityType.Builder.of(LemmingEntity::new, MobCategory.CREATURE).sized(0.15f, 0.12f).clientTrackingRange(8).build("yearofice:collared_lemming"));
    public static final DeferredHolder<EntityType<?>, EntityType<LemmingEntity>> BOG_LEMMING = ENTITIES.register("bog_lemming",
            () -> EntityType.Builder.of(LemmingEntity::new, MobCategory.CREATURE).sized(0.15f, 0.12f).clientTrackingRange(8).build("yearofice:bog_lemming"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> SINGING_VOLE = ENTITIES.register("singing_vole",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.15f, 0.12f).clientTrackingRange(8).build("yearofice:singing_vole"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> LITTLE_BROWN_BAT = ENTITIES.register("little_brown_bat",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.15f, 0.12f).clientTrackingRange(8).build("yearofice:little_brown_bat"));
    public static final DeferredHolder<EntityType<?>, EntityType<SmallGameEntity>> BOREAL_TOAD = ENTITIES.register("boreal_toad",
            () -> EntityType.Builder.of(SmallGameEntity::new, MobCategory.CREATURE).sized(0.2f, 0.16f).clientTrackingRange(8).build("yearofice:boreal_toad"));

    public static final DeferredHolder<EntityType<?>, EntityType<InsectSwarmEntity>> MOSQUITO = swarm("mosquito");
    public static final DeferredHolder<EntityType<?>, EntityType<InsectSwarmEntity>> BLACK_FLY = swarm("black_fly");
    public static final DeferredHolder<EntityType<?>, EntityType<InsectSwarmEntity>> NO_SEE_UM = swarm("no_see_um");

    /** Bestiary › In Minecraft: carcasses: a salmon is a small carcass, 0.6 x 0.25 blocks. */
    public static final DeferredHolder<EntityType<?>, EntityType<SalmonCarcassEntity>> SALMON_CARCASS = ENTITIES.register("salmon_carcass",
            () -> EntityType.Builder.<SalmonCarcassEntity>of(SalmonCarcassEntity::new, MobCategory.MISC).sized(0.6f, 0.25f).clientTrackingRange(8).build("yearofice:salmon_carcass"));

    private static DeferredHolder<EntityType<?>, EntityType<InsectSwarmEntity>> swarm(String id) {
        return ENTITIES.register(id, () -> EntityType.Builder.<InsectSwarmEntity>of(InsectSwarmEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f).noSave().clientTrackingRange(4).updateInterval(4).build("yearofice:" + id));
    }

    public static void register(IEventBus modBus) { ENTITIES.register(modBus); }
}
