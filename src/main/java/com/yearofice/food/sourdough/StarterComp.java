package com.yearofice.food.sourdough;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.sim.food.Starter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** {@code yearofice:starter}: a sourdough starter's state, the minute it was last stepped and the minute it began. */
public record StarterComp(Starter.Data data, long lastTickMinute, long bornMinute) {
    public static final Codec<StarterComp> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("hydration_pct").forGetter(c -> c.data.hydrationPct()),
            Codec.DOUBLE.fieldOf("unfed_days").forGetter(c -> c.data.unfedDays()),
            Codec.INT.fieldOf("revive_feeds").forGetter(c -> c.data.reviveFeeds()),
            Codec.BOOL.fieldOf("frozen").forGetter(c -> c.data.frozen()),
            Codec.BOOL.fieldOf("deep_frozen").forGetter(c -> c.data.deepFrozen()),
            Codec.LONG.fieldOf("last_fed_minute").forGetter(c -> c.data.lastFedMinute()),
            Codec.BOOL.fieldOf("cold").forGetter(c -> c.data.cold()),
            Codec.BOOL.fieldOf("dead").forGetter(c -> c.data.dead()),
            Codec.LONG.fieldOf("last_tick_minute").forGetter(StarterComp::lastTickMinute),
            Codec.LONG.fieldOf("born_minute").forGetter(StarterComp::bornMinute)
    ).apply(i, (h, u, r, f, df, lf, c, d, lt, b) -> new StarterComp(new Starter.Data(h, u, r, f, df, lf, c, d), lt, b)));

    public static final StreamCodec<RegistryFriendlyByteBuf, StarterComp> STREAM = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static StarterComp fresh(long minute) { return new StarterComp(Starter.Data.fresh(minute), minute, minute); }

    public StarterComp with(Starter.Data d, long tick) { return new StarterComp(d, tick, bornMinute); }
}
