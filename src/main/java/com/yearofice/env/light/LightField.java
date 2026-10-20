package com.yearofice.env.light;

import com.yearofice.sim.env.HeldLightMath;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Gameplay light, 0–15 (Minecraft development › Gameplay light): the largest of sky light, vanilla block light at the
 * position and the carried lights near it. {@code EnvService.light} returns {@link #at}; animals, crafting in the dark and
 * the light cone read that number.
 */
public final class LightField {
    private LightField() {}

    /** The carried lights' contribution at a block (its centre), 0 when nobody carries one near. */
    public static int held(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        return HeldLightMath.at(HeldLights.near(level, x, y, z), x, y, z);
    }

    /** {@code vanilla} (sky and block light) raised by the carried lights. */
    public static int at(ServerLevel level, BlockPos pos, int vanilla) { return Math.max(vanilla, held(level, pos)); }
}
