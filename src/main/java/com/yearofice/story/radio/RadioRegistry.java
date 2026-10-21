package com.yearofice.story.radio;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The radio desk, its block entity, the spare battery and the supply parachute. */
public final class RadioRegistry {
    private RadioRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);

    public static final DeferredBlock<RadioDeskBlock> RADIO_DESK = BLOCKS.registerBlock("radio_desk", RadioDeskBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> RADIO_DESK_ITEM = ITEMS.registerSimpleBlockItem("radio_desk", RADIO_DESK, new Item.Properties());
    public static final DeferredItem<Item> RADIO_BATTERY = ITEMS.registerSimpleItem("radio_battery", new Item.Properties().stacksTo(4));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioDeskBlockEntity>> RADIO_DESK_ENTITY =
            BLOCK_ENTITIES.register("radio_desk", () -> BlockEntityType.Builder.of(RadioDeskBlockEntity::new, RADIO_DESK.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<SupplyParachute>> SUPPLY_PARACHUTE = ENTITIES.register("supply_parachute",
            () -> EntityType.Builder.<SupplyParachute>of(SupplyParachute::new, MobCategory.MISC).sized(1.0f, 1.0f).clientTrackingRange(16)
                    .updateInterval(2).build("supply_parachute"));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
    }
}
