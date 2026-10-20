package com.yearofice.plant;

import com.mojang.serialization.Codec;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.plant.PlantNotes;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** {@code plant_notes} (player: the plant notebook) and {@code patches} (chunk: the patch ledger). */
public final class PlantAttachments {
    private PlantAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);

    public static final Codec<PlantNotes> NOTES_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(PlantNotes::new, PlantNotes::raw);

    public static final Supplier<AttachmentType<PlantNotes>> PLANT_NOTES = TYPES.register("plant_notes",
            () -> AttachmentType.builder(() -> PlantNotes.EMPTY).serialize(NOTES_CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<PatchData>> PATCHES = TYPES.register("patches",
            () -> AttachmentType.builder(PatchData::new).serialize(PatchData.CODEC).build());

    public static void register(IEventBus modBus) { TYPES.register(modBus); }

    static Map<String, Integer> empty() { return Map.of(); }
}
