package com.yearofice.client.render;

import com.yearofice.build.outpost.OutpostSounds;
import com.yearofice.build.outpost.Tells;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.build.CabinTells;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * B8, the cabin tells on the client: frost puffs at leaky seams, meltwater drips and a cracking icicle under a warm
 * roof edge, found by {@link Tells} within 8 blocks every half second. Cold is read from snow on the ground and warmth
 * assumed indoors until the temperature sync and RoomService.leaks land (HOOKS); the strengths come from
 * {@link CabinTells}. Particles stand in for the frost-mark overlay art.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class RoomTells {
    private RoomTells() {}

    private static final double INSIDE_C = 15, OUTSIDE_C = -25;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || mc.level.getGameTime() % 10 != 0) return;
        BlockPos at = mc.player.blockPosition();
        var rnd = mc.level.random;
        // the room model's own leaks when a server is local (RoomService), else the tagged-seam scan
        var srv = mc.getSingleplayerServer();
        var room = srv == null ? null : com.yearofice.api.build.RoomService.get().roomAt(srv.getLevel(mc.level.dimension()), at);
        for (BlockPos seam : room != null ? room.leaks() : Tells.leakySeams(mc.level, at, 8)) {
            if (!Tells.coldOutside(mc.level, seam)) continue;
            if (rnd.nextDouble() < CabinTells.frost(INSIDE_C, OUTSIDE_C, 2) * 0.35)
                mc.level.addParticle(ParticleTypes.SNOWFLAKE, seam.getX() + rnd.nextDouble(), seam.getY() + rnd.nextDouble(), seam.getZ() + rnd.nextDouble(), 0, -0.01, 0);
        }
        for (BlockPos edge : Tells.roofEdges(mc.level, at, 8)) {
            if (rnd.nextDouble() < CabinTells.dripsPerMin(INSIDE_C, -1) / 12.0)
                mc.level.addParticle(ParticleTypes.DRIPPING_WATER, edge.getX() + rnd.nextDouble(), edge.getY() - 0.1, edge.getZ() + rnd.nextDouble(), 0, 0, 0);
            if (rnd.nextInt(300) == 0)
                mc.level.playLocalSound(edge.below(), OutpostSounds.ICICLE_CRACK.get(), SoundSource.AMBIENT, 0.8f, 0.9f + rnd.nextFloat() * 0.2f, false);
            else if (rnd.nextInt(30) == 0)
                mc.level.playLocalSound(edge.below(), OutpostSounds.FROST_DRIP.get(), SoundSource.AMBIENT, 0.6f, 0.9f + rnd.nextFloat() * 0.2f, false);
        }
    }
}
