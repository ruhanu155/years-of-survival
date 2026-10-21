package com.yearofice.ecosystem;

import com.yearofice.ecosystem.carcass.BigGameItems;
import com.yearofice.ecosystem.debug.EcoCommands;
import com.yearofice.ecosystem.spawn.NoVanillaMobs;
import com.yearofice.ecosystem.spawn.SlotSpawner;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.food.traps.TrapRegistry;
import com.yearofice.food.traps.debug.TrapCommands;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Wires the ecosystem slice and the snare: registries, attributes, the species and item-family loaders, the slot
 * spawner, the no-monsters catch-all, {@code /yoi spawn}, {@code /yoi eco} and {@code /yoi snare}, and config
 * reloads. Called once from the mod constructor.
 */
public final class EcosystemSystem {
    private EcosystemSystem() {}

    public static void init(IEventBus modBus) {
        EcosystemRegistry.register(modBus);
        TrapRegistry.register(modBus);
        com.yearofice.ecosystem.raid.RaidRegistry.register(modBus);
        modBus.addListener(EcosystemSystem::attributes);
        modBus.addListener((ModConfigEvent.Loading e) -> EcoConfig.invalidate());
        modBus.addListener((ModConfigEvent.Reloading e) -> EcoConfig.invalidate());
        modBus.addListener(EcosystemSystem::creativeTabs);
        NeoForge.EVENT_BUS.addListener(SpeciesBook::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(EcoCommands::register);
        NeoForge.EVENT_BUS.addListener(TrapCommands::register);
        NeoForge.EVENT_BUS.addListener(com.yearofice.ecosystem.debug.BigGameCommands::register);
        NeoForge.EVENT_BUS.register(com.yearofice.ecosystem.HuntHooks.class);
        NeoForge.EVENT_BUS.addListener(SlotSpawner::onTick);
        NeoForge.EVENT_BUS.addListener(com.yearofice.ecosystem.theft.TheftService::onTick);
        NeoForge.EVENT_BUS.addListener(com.yearofice.ecosystem.theft.TheftCommands::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> { SlotSpawner.clear(); com.yearofice.ecosystem.theft.TheftService.clear(); com.yearofice.ecosystem.ai.ScentPoints.clear(); });
        // Belt and braces: our animals never reach vanilla's death, but nothing they could drop survives either.
        NeoForge.EVENT_BUS.addListener((LivingDropsEvent e) -> { if (e.getEntity() instanceof YoiAnimal) e.setCanceled(true); });
        NeoForge.EVENT_BUS.addListener((LivingExperienceDropEvent e) -> { if (e.getEntity() instanceof YoiAnimal) e.setCanceled(true); });
        // M6 lane D: door entry, rack raids, talk as you walk.
        NeoForge.EVENT_BUS.addListener(com.yearofice.ecosystem.raid.ShelterEntry::onTick);
        NeoForge.EVENT_BUS.addListener(com.yearofice.ecosystem.raid.ShelterEntry::onUse);
        NeoForge.EVENT_BUS.addListener(com.yearofice.ecosystem.raid.RaidCommands::register);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> { com.yearofice.ecosystem.raid.ShelterEntry.clear(); com.yearofice.ecosystem.encounter.TalkWalk.clear(); });
        NoVanillaMobs.register();
    }

    static void attributes(EntityAttributeCreationEvent e) {
        e.put(EcosystemRegistry.SNOWSHOE_HARE.get(), YoiAnimal.attributes(3).build());
        e.put(EcosystemRegistry.WILLOW_PTARMIGAN.get(), YoiAnimal.attributes(2).build());
        e.put(EcosystemRegistry.MOOSE.get(), YoiAnimal.attributes(120).build());
        // M5 lane A: health in half-hearts of damage a hit zone's kill threshold overrides anyway.
        e.put(EcosystemRegistry.RED_SQUIRREL.get(), YoiAnimal.attributes(2).build());
        e.put(EcosystemRegistry.MARTEN.get(), YoiAnimal.attributes(4).build());
        e.put(EcosystemRegistry.ERMINE.get(), YoiAnimal.attributes(2).build());
        e.put(EcosystemRegistry.MINK.get(), YoiAnimal.attributes(3).build());
        e.put(EcosystemRegistry.MUSKRAT.get(), YoiAnimal.attributes(4).build());
        e.put(EcosystemRegistry.BEAVER.get(), YoiAnimal.attributes(14).build());
        e.put(EcosystemRegistry.PORCUPINE.get(), YoiAnimal.attributes(8).build());
        e.put(EcosystemRegistry.WOOD_FROG.get(), YoiAnimal.attributes(1).build());
        e.put(EcosystemRegistry.YELLOWJACKET.get(), YoiAnimal.attributes(1).build());
        e.put(EcosystemRegistry.CRITTER.get(), YoiAnimal.attributes(2).build());
        e.put(EcosystemRegistry.SPRUCE_GROUSE.get(), YoiAnimal.attributes(2).build());
        e.put(EcosystemRegistry.RUFFED_GROUSE.get(), YoiAnimal.attributes(2).build());
        e.put(EcosystemRegistry.ROCK_PTARMIGAN.get(), YoiAnimal.attributes(2).build());
        // M6 lane A: health in half-hearts a hit zone's kill threshold overrides anyway.
        e.put(EcosystemRegistry.CARIBOU.get(), YoiAnimal.attributes(60).build());
        e.put(EcosystemRegistry.MULE_DEER.get(), YoiAnimal.attributes(40).build());
        e.put(EcosystemRegistry.BLACK_BEAR.get(), YoiAnimal.attributes(80).build());
        e.put(EcosystemRegistry.GRIZZLY.get(), YoiAnimal.attributes(120).build());
        e.put(EcosystemRegistry.WOLF.get(), YoiAnimal.attributes(40).build());
        e.put(EcosystemRegistry.COYOTE.get(), YoiAnimal.attributes(10).build());
        e.put(EcosystemRegistry.RED_FOX.get(), YoiAnimal.attributes(6).build());
        e.put(EcosystemRegistry.LYNX.get(), YoiAnimal.attributes(10).build());
        e.put(EcosystemRegistry.WOLVERINE.get(), YoiAnimal.attributes(12).build());
    }

    /** Until the studio adds a Year of Ice tab, the slice's game items sit in the vanilla tabs. */
    static void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(TrapRegistry.SNARE_WIRE.get());
        } else if (e.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            e.accept(EcosystemRegistry.SNOWSHOE_HARE_MEAT.get());
            e.accept(EcosystemRegistry.WILLOW_PTARMIGAN_MEAT.get());
            e.accept(EcosystemRegistry.RED_SQUIRREL_MEAT.get());
            e.accept(EcosystemRegistry.MARTEN_MEAT.get());
            e.accept(EcosystemRegistry.MINK_MEAT.get());
            e.accept(EcosystemRegistry.MUSKRAT_MEAT.get());
            e.accept(EcosystemRegistry.PORCUPINE_MEAT.get());
            e.accept(EcosystemRegistry.BEAVER_MEAT.get());
            e.accept(EcosystemRegistry.SPRUCE_GROUSE_MEAT.get());
            e.accept(EcosystemRegistry.RUFFED_GROUSE_MEAT.get());
            e.accept(EcosystemRegistry.ROCK_PTARMIGAN_MEAT.get());
            for (String k : new String[] {"animal_fat", "heart", "liver", "kidney", "tongue"}) e.accept(BigGameItems.ALL.get(k).get());
            for (String sp : BigGameItems.MEATS) e.accept(BigGameItems.ALL.get(sp + "_meat").get());
        } else if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            for (String k : new String[] {"marrow_bone", "antler", "claw"}) e.accept(BigGameItems.ALL.get(k).get());
            for (String sp : BigGameItems.HIDES) e.accept(BigGameItems.ALL.get(sp + "_hide").get());
            for (String sp : BigGameItems.PELTS) e.accept(BigGameItems.ALL.get(sp + "_pelt").get());
            e.accept(EcosystemRegistry.HARE_PELT.get());
            e.accept(EcosystemRegistry.SINEW.get());
            e.accept(EcosystemRegistry.ERMINE_PELT.get());
            e.accept(EcosystemRegistry.BEAVER_PELT.get());
            e.accept(EcosystemRegistry.MARTEN_PELT.get());
            e.accept(EcosystemRegistry.MINK_PELT.get());
            e.accept(EcosystemRegistry.MUSKRAT_PELT.get());
            e.accept(EcosystemRegistry.RED_SQUIRREL_PELT.get());
        }
    }
}
