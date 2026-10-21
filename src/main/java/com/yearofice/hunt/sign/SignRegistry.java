package com.yearofice.hunt.sign;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.hunt.Sign;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The four sign blocks (track, scat, browse, bed) and their one block entity. */
public final class SignRegistry {
    private SignRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    private static final BlockBehaviour.Properties P = BlockBehaviour.Properties.of().noCollission().instabreak().noLootTable().noOcclusion()
            .sound(SoundType.SAND).pushReaction(PushReaction.DESTROY);

    public static final DeferredBlock<SignBlock> TRACK = BLOCKS.registerBlock("track", p -> new SignBlock(p, Sign.Kind.TRACK), P);
    public static final DeferredBlock<SignBlock> SCAT = BLOCKS.registerBlock("scat", p -> new SignBlock(p, Sign.Kind.SCAT), P);
    public static final DeferredBlock<SignBlock> BROWSE = BLOCKS.registerBlock("browse", p -> new SignBlock(p, Sign.Kind.BROWSE), P);
    public static final DeferredBlock<SignBlock> BED = BLOCKS.registerBlock("bed", p -> new SignBlock(p, Sign.Kind.BED), P);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SignBlockEntity>> SIGN = TYPES.register("sign",
            () -> BlockEntityType.Builder.of(SignBlockEntity::new, TRACK.get(), SCAT.get(), BROWSE.get(), BED.get()).build(null));

    public static SignBlock block(Sign.Kind k) {
        return switch (k) { case TRACK -> TRACK.get(); case SCAT -> SCAT.get(); case BROWSE -> BROWSE.get(); case BED -> BED.get(); };
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        TYPES.register(bus);
    }
}
