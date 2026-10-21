package com.yearofice.story.radio;

import com.google.gson.JsonObject;
import com.yearofice.api.weather.WeatherService;
import com.yearofice.sim.story.RadioMath;
import com.yearofice.sim.weather.StormKind;
import com.yearofice.worldgen.regions.Regions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** The supply drop: a plane pass, a parachute, a crate within the scatter of the zone (Open world in depth › The radio, Delivery). */
public final class SupplyDrop {
    private SupplyDrop() {}

    /** Pete flies in clear or cloudy weather (no storm up) with wind under 8 m/s. */
    public static boolean flyable(ServerLevel level, BlockPos at, long minute) {
        if (!WeatherService.installed()) return true;
        var w = WeatherService.get().at(level, at, minute);
        return RadioMath.canFly(w.kind() == StormKind.CLEAR, w.windMs());
    }

    /** The zone for a player: their region's drop zone when it is loaded and inside the border, else where they stand. */
    public static BlockPos zoneFor(ServerLevel level, ServerPlayer p) {
        var r = Regions.at(p.blockPosition().getX(), p.blockPosition().getZ());
        if (r != null) {
            BlockPos z = new BlockPos(r.dropZone()[0], 0, r.dropZone()[1]);
            if (level.hasChunkAt(z) && level.getWorldBorder().isWithinBounds(z)) return z;
        }
        return p.blockPosition();
    }

    /** Sends the plane over {@code zone}: the pass sounds now and the parachute is released over a scattered point. Returns it. */
    public static SupplyParachute fly(ServerLevel level, BlockPos zone) { return fly(level, zone, RadioConfig.get("scatter_m")); }

    /** As {@link #fly(ServerLevel, BlockPos)} with an explicit scatter radius in metres (0 lands on the zone). */
    public static SupplyParachute fly(ServerLevel level, BlockPos zone, double scatter) {
        long h = zone.asLong() * 31 + level.getGameTime();
        double ang = RadioMath.pick(h, 1, 3600) / 3600.0 * Math.PI * 2, rad = scatter * RadioMath.pick(h, 2, 1000) / 1000.0;
        int x = zone.getX() + (int) Math.round(Math.cos(ang) * rad), z = zone.getZ() + (int) Math.round(Math.sin(ang) * rad);
        level.getChunk(x >> 4, z >> 4);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        double y = ground + RadioConfig.get("drop_height_m");
        // ponytail: a stock sound stands in for the plane pass; upgrade: yearofice:signal.plane_pass from the audio lane
        level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.AMBIENT, 6.0f, 0.5f);
        JsonObject d = RadioData.skeds().getAsJsonObject("drop");
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(x, y, z) < 800 * 800) p.sendSystemMessage(Component.literal(d.get("plane_line").getAsString()));
        SupplyParachute e = new SupplyParachute(RadioRegistry.SUPPLY_PARACHUTE.get(), level);
        e.setPos(x + 0.5, y, z + 0.5);
        level.addFreshEntity(e);
        return e;
    }
}
