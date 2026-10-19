package com.yearofice.body.env;

import com.yearofice.sim.body.ColdWater;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.HeatSource;
import com.yearofice.sim.env.ShelterState;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * What the body tick's step 1 reads about the place a player stands (Minecraft development › Layer 2,
 * {@code EnvService.sample}). The body reads the environment only through this interface; when the Environment
 * field lands, its service replaces {@link VanillaEnvSampler} here.
 */
public interface EnvSampler {

    /** One sample: the field record, the heat sources around the player, the open shelter and the water depth. */
    record Sample(EnvSample env, List<HeatSource> heat, ShelterState shelter, ColdWater.Depth depth) {}

    Sample sample(ServerPlayer player, long minute);
}
