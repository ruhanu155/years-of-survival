package com.yearofice.api.build;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What stops a fish wheel turning (Water power, crossings and craft › Fish wheel): no current, ice over the water, or
 * debris. The fishing settle asks how many of the hours it is settling the wheel actually turned; the wheel block clears
 * debris on a sneak-use. Owner: {@code build.craft.water}. Not installed, a wheel always turns.
 */
public interface WheelService {
    /** Of {@code hours} game hours at the wheel at {@code pos}, how many it turned; the rest of the debris budget is spent. */
    int turningHours(ServerLevel level, BlockPos pos, int hours);

    /** Clears the debris from the wheel at {@code pos}. */
    void clearDebris(ServerLevel level, BlockPos pos);

    static WheelService get() {
        WheelService s = Holder.service;
        return s != null ? s : Holder.ALWAYS;
    }

    static void install(WheelService s) { Holder.service = s; }

    final class Holder {
        private Holder() {}

        static volatile WheelService service;
        static final WheelService ALWAYS = new WheelService() {
            @Override public int turningHours(ServerLevel level, BlockPos pos, int hours) { return hours; }

            @Override public void clearDebris(ServerLevel level, BlockPos pos) {}
        };
    }
}
