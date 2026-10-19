package com.yearofice.fire;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** The fire ring's {@code lay} block state: the highest layer showing (tinder, kindling, fuel), for the model only. */
public enum FireLay implements StringRepresentable {
    NONE,
    TINDER,
    KINDLING,
    FUEL;

    @Override
    public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
