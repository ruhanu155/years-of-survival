package com.yearofice.ecosystem.carcass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;

/**
 * The carcass record (Bestiary › The carcass entity, In Minecraft: carcasses), slice fields: species, sex, cause,
 * the game minute of death, whole weight in grams, the coat it died in, and its {@code fresh}. The same codec
 * serves the {@code yearofice:carcass} attachment on {@code carcass_small} and the {@code yearofice:carcass} data
 * component on {@code whole_game}. The damage map, rigor, core temperature and dressing steps join it in M5–M6.
 */
public record CarcassRecord(String species, int sex, String cause, long deathMinute, int weightG, int coat, Fresh fresh) {
    public static final CarcassRecord EMPTY = new CarcassRecord("yearofice:snowshoe_hare", 0, "none", 0, 1500, 0,
            new Fresh(FoodState.RAW, Freshness.CLOCK_AIR_SLICE, 1, 1, 0, true));

    public static final Codec<CarcassRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("species").forGetter(CarcassRecord::species),
            Codec.INT.optionalFieldOf("sex", 0).forGetter(CarcassRecord::sex),
            Codec.STRING.optionalFieldOf("cause", "unknown").forGetter(CarcassRecord::cause),
            Codec.LONG.fieldOf("death_minute").forGetter(CarcassRecord::deathMinute),
            Codec.INT.fieldOf("weight_g").forGetter(CarcassRecord::weightG),
            Codec.INT.optionalFieldOf("coat", 0).forGetter(CarcassRecord::coat),
            Freshness.CODEC.fieldOf("fresh").forGetter(CarcassRecord::fresh)
    ).apply(i, CarcassRecord::new));

    public String path() {
        int c = species.indexOf(':');
        return c < 0 ? species : species.substring(c + 1);
    }
}
