package com.yearofice.build.craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A recipe's tool: an item id or an item tag written {@code #yearofice:knives} (Crafting and building in depth ›
 * Data and tests, "tool"). Tools in the pack count at the Hands station; they are worn, not consumed.
 */
public record ToolReq(ResourceLocation id, boolean tag) {
    public static final Codec<ToolReq> CODEC = Codec.STRING.comapFlatMap(ToolReq::parse, ToolReq::text);

    static DataResult<ToolReq> parse(String s) {
        boolean tag = s.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(tag ? s.substring(1) : s);
        return id == null ? DataResult.error(() -> "not a resource location: " + s) : DataResult.success(new ToolReq(id, tag));
    }

    public String text() { return (tag ? "#" : "") + id; }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return tag ? stack.is(TagKey.create(Registries.ITEM, id)) : BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    /** The name the screen shows: an item's own name, or a tag's {@code tag.item.<ns>.<path>} line. */
    public Component display() {
        if (tag) return Component.translatable("tag.item." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
        Item item = BuiltInRegistries.ITEM.get(id);
        return item.getDescription();
    }
}
