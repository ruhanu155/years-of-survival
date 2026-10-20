package com.yearofice.build.craft;

import com.yearofice.core.YearOfIce;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * {@code yearofice:station_list}, server to client: the recipes a player sees at a station, each already judged on the
 * server (the problem, the time and calories after practice, the Clumsy/Handy/Practiced word, what is held of each
 * input). The screen only shows it; the server re-checks everything when a recipe is picked.
 */
public record StationListPayload(boolean hasBench, BlockPos bench, List<Row> rows) implements CustomPacketPayload {
    public static final Type<StationListPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "station_list"));

    /** One input line: the icon, its name, how many are needed and how many are held. */
    public record Need(ItemStack icon, Component name, int count, int have) {}

    /** One recipe row. {@code problem} is a {@link StationCraft.Problem} ordinal; {@code word} a {@code Practice.Word} ordinal. */
    public record Row(ResourceLocation id, ItemStack result, int tier, int problem, float minutes, int kcal, int word, List<Need> needs,
                      Component tool, boolean toolOk) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, StationListPayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> {
                b.writeBoolean(p.hasBench);
                b.writeBlockPos(p.bench);
                b.writeVarInt(p.rows.size());
                for (Row r : p.rows) {
                    b.writeResourceLocation(r.id);
                    ItemStack.STREAM_CODEC.encode(b, r.result);
                    b.writeVarInt(r.tier);
                    b.writeVarInt(r.problem);
                    b.writeFloat(r.minutes);
                    b.writeVarInt(r.kcal);
                    b.writeVarInt(r.word);
                    b.writeVarInt(r.needs.size());
                    for (Need n : r.needs) {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(b, n.icon);
                        ComponentSerialization.STREAM_CODEC.encode(b, n.name);
                        b.writeVarInt(n.count);
                        b.writeVarInt(n.have);
                    }
                    ComponentSerialization.STREAM_CODEC.encode(b, r.tool);
                    b.writeBoolean(r.toolOk);
                }
            },
            b -> {
                boolean hasBench = b.readBoolean();
                BlockPos bench = b.readBlockPos();
                int n = b.readVarInt();
                List<Row> rows = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    ResourceLocation id = b.readResourceLocation();
                    ItemStack result = ItemStack.STREAM_CODEC.decode(b);
                    int tier = b.readVarInt(), problem = b.readVarInt();
                    float minutes = b.readFloat();
                    int kcal = b.readVarInt(), word = b.readVarInt();
                    int m = b.readVarInt();
                    List<Need> needs = new ArrayList<>(m);
                    for (int k = 0; k < m; k++) {
                        needs.add(new Need(ItemStack.OPTIONAL_STREAM_CODEC.decode(b), ComponentSerialization.STREAM_CODEC.decode(b), b.readVarInt(), b.readVarInt()));
                    }
                    Component tool = ComponentSerialization.STREAM_CODEC.decode(b);
                    rows.add(new Row(id, result, tier, problem, minutes, kcal, word, needs, tool, b.readBoolean()));
                }
                return new StationListPayload(hasBench, bench, rows);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
