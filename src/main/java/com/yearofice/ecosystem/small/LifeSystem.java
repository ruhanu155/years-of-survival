package com.yearofice.ecosystem.small;

import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.chorus.ChorusCommands;
import com.yearofice.ecosystem.chorus.ChorusService;
import com.yearofice.ecosystem.chorus.ChorusSounds;
import com.yearofice.ecosystem.migration.MigrationCommands;
import com.yearofice.ecosystem.migration.MigrationData;
import com.yearofice.ecosystem.migration.MigrationService;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Wires M7 lane C, the life of the year: the small mammals and toad, the biting-insect swarms, the spawned-out salmon,
 * the migration calendar, the dawn chorus, the seasonal windows of the fungi and aquatic plants, and life under the ice.
 */
public final class LifeSystem {
    private LifeSystem() {}

    public static void init(IEventBus modBus) {
        SmallLifeRegistry.register(modBus);
        ChorusSounds.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            for (var t : java.util.List.of(SmallLifeRegistry.BROWN_LEMMING, SmallLifeRegistry.COLLARED_LEMMING, SmallLifeRegistry.BOG_LEMMING,
                    SmallLifeRegistry.SINGING_VOLE, SmallLifeRegistry.LITTLE_BROWN_BAT, SmallLifeRegistry.BOREAL_TOAD)) {
                e.put((EntityType<? extends YoiAnimal>) t.get(), YoiAnimal.attributes(1).build());
            }
            for (var t : java.util.List.of(SmallLifeRegistry.MOSQUITO, SmallLifeRegistry.BLACK_FLY, SmallLifeRegistry.NO_SEE_UM)) { /* plain entities, no attributes */ }
        });
        modBus.addListener((ModConfigEvent.Loading e) -> LifeConfig.invalidate());
        modBus.addListener((ModConfigEvent.Reloading e) -> LifeConfig.invalidate());
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> {
            e.addListener(new com.yearofice.plant.fungi.FungusWindows());
            e.addListener(new com.yearofice.plant.aquatic.AquaticWindows());
        });
        NeoForge.EVENT_BUS.addListener(MigrationData::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(InsectService::onTick);
        NeoForge.EVENT_BUS.addListener(MigrationService::onTick);
        NeoForge.EVENT_BUS.addListener(ChorusService::onTick);
        NeoForge.EVENT_BUS.addListener(UnderIce::onTick);
        NeoForge.EVENT_BUS.addListener(LifeCommands::register);
        NeoForge.EVENT_BUS.addListener(MigrationCommands::register);
        NeoForge.EVENT_BUS.addListener(ChorusCommands::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            InsectService.clear();
            LemmingIndex.clear();
            MigrationService.clear();
            ChorusService.clear();
            UnderIce.clear();
        });
    }
}
