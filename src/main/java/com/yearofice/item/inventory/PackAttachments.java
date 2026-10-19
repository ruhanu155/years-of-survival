package com.yearofice.item.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * {@code yearofice:pack_rules} (The save and sync matrix: a player attachment): the player's own pack options. In
 * M2 that is the auto-pickup switch (Blueprint › Moving things: arrows, feathers and flint flakes, off by default).
 * The record starts at v1 with every field defaulted, per the versioned-codec rule.
 */
public final class PackAttachments {
    private PackAttachments() {}

    public record Rules(boolean autoPickup) {
        public static final Rules DEFAULT = new Rules(false);
        public static final Codec<Rules> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("auto_pickup", false).forGetter(Rules::autoPickup)).apply(i, Rules::new));
    }

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, YearOfIce.MOD_ID);

    public static final Supplier<AttachmentType<Rules>> PACK_RULES = TYPES.register("pack_rules",
            () -> AttachmentType.builder(() -> Rules.DEFAULT).serialize(Rules.CODEC).copyOnDeath().build());

    public static Rules rules(Player p) { return p.getData(PACK_RULES.get()); }

    public static void setAutoPickup(Player p, boolean on) { p.setData(PACK_RULES.get(), new Rules(on)); }

    public static void register(IEventBus modBus) { TYPES.register(modBus); }
}
