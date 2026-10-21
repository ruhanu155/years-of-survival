package com.yearofice.env.weather;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The weather team's blocks and sounds (Weather and storms › Blocks, ops and passes). */
public final class WeatherRegistry {
    private WeatherRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);

    public static final DeferredBlock<SheetBlock> GLAZE_LAYER = BLOCKS.registerBlock("glaze_layer", SheetBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.1f).friction(0.98f).sound(SoundType.GLASS)
                    .noOcclusion().replaceable().pushReaction(PushReaction.DESTROY).noLootTable());
    public static final DeferredBlock<SheetBlock> HAIL_LAYER = BLOCKS.registerBlock("hail_layer", SheetBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1f).friction(0.75f).sound(SoundType.POWDER_SNOW)
                    .noOcclusion().replaceable().pushReaction(PushReaction.DESTROY).noLootTable());
    public static final DeferredBlock<FallenLimbBlock> FALLEN_LIMB = BLOCKS.registerBlock("fallen_limb", FallenLimbBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.8f).sound(SoundType.WOOD).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredHolder<SoundEvent, SoundEvent> STRIKE = sound("weather.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUNDER = sound("weather.thunder");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, id)));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        SOUNDS.register(bus);
    }
}
