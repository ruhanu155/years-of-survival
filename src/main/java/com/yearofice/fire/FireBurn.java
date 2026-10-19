package com.yearofice.fire;

import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.FireRules;
import com.yearofice.sim.fire.FireState;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** The fire ring's {@code burn} block state: the sim's state, with the setting folded in for a fire. Drives light and model. */
public enum FireBurn implements StringRepresentable {
    OUT(FireState.OUT, null),
    EMBER(FireState.EMBER, null),
    TINDER(FireState.TINDER, null),
    SMALL(FireState.SMALL, null),
    LOW(FireState.FIRE, FireSetting.LOW),
    STEADY(FireState.FIRE, FireSetting.STEADY),
    HOT(FireState.FIRE, FireSetting.HOT),
    COALS(FireState.COALS, null),
    BANKED(FireState.BANKED, null);

    public final FireState state;
    public final FireSetting setting;
    public final int light;

    FireBurn(FireState state, FireSetting setting) {
        this.state = state;
        this.setting = setting;
        this.light = FireRules.light(state, setting == null ? FireSetting.STEADY : setting);
    }

    public static FireBurn of(FireState s, FireSetting set) {
        if (s == FireState.FIRE) return switch (set) {
            case LOW -> LOW;
            case STEADY -> STEADY;
            case HOT -> HOT;
        };
        for (FireBurn b : values()) if (b.state == s) return b;
        return OUT;
    }

    public static FireBurn byName(String name) {
        for (FireBurn b : values()) if (b.getSerializedName().equals(name)) return b;
        return null;
    }

    @Override
    public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
}
