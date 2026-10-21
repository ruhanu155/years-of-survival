package com.yearofice.story.rare;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.env.weather.WeatherConfig;
import com.yearofice.sim.weather.StrikeRisk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** A bolt can hit a player standing near its point (the dial lightning_player_risk); indoors under a roof is safe. */
public final class StrikeHarm {
    private StrikeHarm() {}

    public static final float DAMAGE = 12f;

    public static int apply(ServerLevel level, BlockPos top, int seed) {
        Mulberry32 r = new Mulberry32(seed ^ 0x5bd1e995);
        int hurt = 0;
        for (ServerPlayer p : level.players()) {
            double d = Math.sqrt(p.blockPosition().distSqr(top));
            if (d >= StrikeRisk.REACH_M) continue;
            double chance = StrikeRisk.chance(d, WeatherConfig.tuning().lightningPlayerRisk(), level.canSeeSky(p.blockPosition()));
            if (r.nextDouble() < chance && p.hurt(level.damageSources().lightningBolt(), DAMAGE)) hurt++;
        }
        return hurt;
    }
}
