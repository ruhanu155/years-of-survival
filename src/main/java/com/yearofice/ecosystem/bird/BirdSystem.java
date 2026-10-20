package com.yearofice.ecosystem.bird;

import com.yearofice.ecosystem.YoiAnimal;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Wires {@code ecosystem.bird}: registry, attributes, the spawner tick, {@code /yoi flock} and config reloads. */
public final class BirdSystem {
    private BirdSystem() {}

    public static void init(IEventBus modBus) {
        BirdRegistry.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            for (var t : BirdRegistry.TYPES.values()) e.put(t.get(), YoiAnimal.attributes(3).build());
        });
        modBus.addListener((ModConfigEvent.Loading e) -> BirdConfig.invalidate());
        modBus.addListener((ModConfigEvent.Reloading e) -> BirdConfig.invalidate());
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) e.accept(BirdRegistry.BIRD_MEAT.get());
        });
        NeoForge.EVENT_BUS.addListener(BirdService::onTick);
        NeoForge.EVENT_BUS.addListener(BirdCommands::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> BirdService.clear());
    }
}
