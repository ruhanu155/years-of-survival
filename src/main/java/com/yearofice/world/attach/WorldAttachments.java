package com.yearofice.world.attach;

import com.yearofice.core.YearOfIce;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The world's chunk attachments (Minecraft development › Chunk attachments), registered here as each lands.
 * M2: {@code placed_mask} (RECONCILE 167 moves it here from the M1 SavedData). On this pin NeoForge copies a
 * proto-chunk's attachments to the {@code LevelChunk} on promotion (prototype W2), so set pieces mark the mask
 * during generation and it survives.
 */
public final class WorldAttachments {
    private WorldAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);

    public static final Supplier<AttachmentType<PlacedMaskData>> PLACED_MASK = TYPES.register("placed_mask",
            () -> AttachmentType.builder(PlacedMaskData::new).serialize(new IAttachmentSerializer<Tag, PlacedMaskData>() {
                @Override
                public PlacedMaskData read(IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                    return PlacedMaskData.read(tag);
                }

                @Override
                public Tag write(PlacedMaskData data, HolderLookup.Provider provider) { return data.write(); }
            }).build());

    public static void register(IEventBus modBus) { TYPES.register(modBus); }
}
