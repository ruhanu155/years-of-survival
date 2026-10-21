package com.yearofice.hunt;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The hunt's ids (Ready for development › M6: "the arrow projectile; bows; calls and blinds"): bows, arrows and
 * arrowheads, the spear, the two calls, the ground blind, the tree stand and the blood patch.
 */
public final class HuntRegistry {
    private HuntRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);

    public static final DeferredItem<BowItem> SELF_BOW = ITEMS.registerItem("self_bow", p -> new BowItem(p, "self_bow"), new Item.Properties().stacksTo(1).durability(500));
    public static final DeferredItem<BowItem> KIT_BOW = ITEMS.registerItem("kit_bow", p -> new BowItem(p, "kit_bow"), new Item.Properties().stacksTo(1).durability(1000));
    public static final DeferredItem<BowItem> LONGBOW = ITEMS.registerItem("longbow", p -> new BowItem(p, "longbow"), new Item.Properties().stacksTo(1).durability(500));
    public static final DeferredItem<SpearItem> SPEAR = ITEMS.registerItem("spear", SpearItem::new, new Item.Properties().stacksTo(1).durability(200));
    public static final DeferredItem<Item> ARROW = ITEMS.registerItem("arrow", Item::new, new Item.Properties().stacksTo(32));
    public static final DeferredItem<Item> ARROWHEAD = ITEMS.registerItem("arrowhead", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<BowItem> SINEW_BOW = ITEMS.registerItem("sinew_bow", p -> new BowItem(p, "sinew_bow"), new Item.Properties().stacksTo(1).durability(500));
    public static final DeferredItem<Item> BOW_STAVE = ITEMS.registerItem("bow_stave", Item::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SINEW_CORD = ITEMS.registerItem("sinew_cord", Item::new, new Item.Properties().stacksTo(32));
    public static final DeferredItem<Item> FLETCHING = ITEMS.registerItem("fletching", Item::new, new Item.Properties().stacksTo(32));
    public static final DeferredItem<Item> FEATHER = ITEMS.registerItem("feather", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<CallItem> MOOSE_CALL = ITEMS.registerItem("moose_call", p -> new CallItem(p, "moose_call"), new Item.Properties().stacksTo(1));
    public static final DeferredItem<CallItem> SQUEAK_CALL = ITEMS.registerItem("squeak_call", p -> new CallItem(p, "squeak_call"), new Item.Properties().stacksTo(1));

    private static final BlockBehaviour.Properties CAMO = BlockBehaviour.Properties.of().strength(0.5f).noOcclusion().sound(SoundType.GRASS)
            .pushReaction(PushReaction.DESTROY);
    public static final DeferredBlock<HuntBlocks.PoleBlind> POLE_BLIND = BLOCKS.registerBlock("pole_blind", HuntBlocks.PoleBlind::new, CAMO);
    public static final DeferredBlock<HuntBlocks.TreeStand> TREE_STAND = BLOCKS.registerBlock("tree_stand", HuntBlocks.TreeStand::new,
            BlockBehaviour.Properties.of().strength(1.0f).noOcclusion().sound(SoundType.WOOD));
    public static final DeferredBlock<BloodTrailBlock> BLOOD_TRAIL = BLOCKS.registerBlock("blood_trail", BloodTrailBlock::new,
            BlockBehaviour.Properties.of().noCollission().instabreak().noLootTable().noOcclusion().sound(SoundType.SLIME_BLOCK).pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> POLE_BLIND_ITEM = ITEMS.registerSimpleBlockItem("pole_blind", POLE_BLIND, new Item.Properties());
    public static final DeferredItem<BlockItem> TREE_STAND_ITEM = ITEMS.registerSimpleBlockItem("tree_stand", TREE_STAND, new Item.Properties());

    public static final DeferredHolder<EntityType<?>, EntityType<ArrowEntity>> ARROW_ENTITY = ENTITIES.register("arrow",
            () -> EntityType.Builder.<ArrowEntity>of(ArrowEntity::new, MobCategory.MISC).sized(0.2f, 0.2f).noSave().clientTrackingRange(8)
                    .updateInterval(1).build("arrow"));

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }
}
