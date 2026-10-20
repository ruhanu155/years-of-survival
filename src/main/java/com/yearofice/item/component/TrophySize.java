package com.yearofice.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** {@code yearofice:trophy_size}: how big a kill was, carried from the butchering to the head or antler (A5). */
public record TrophySize(String measure, double value, double z, String cls) {
    public static final Codec<TrophySize> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("measure").forGetter(TrophySize::measure),
            Codec.DOUBLE.fieldOf("value").forGetter(TrophySize::value),
            Codec.DOUBLE.fieldOf("z").forGetter(TrophySize::z),
            Codec.STRING.fieldOf("class").forGetter(TrophySize::cls)).apply(i, TrophySize::new));
    public static final StreamCodec<ByteBuf, TrophySize> STREAM = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TrophySize::measure, ByteBufCodecs.DOUBLE, TrophySize::value,
            ByteBufCodecs.DOUBLE, TrophySize::z, ByteBufCodecs.STRING_UTF8, TrophySize::cls, TrophySize::new);
}
