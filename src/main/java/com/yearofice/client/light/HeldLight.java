package com.yearofice.client.light;

import com.yearofice.api.env.LightData;
import com.yearofice.api.env.LightState;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.env.HeldLightMath;
import com.yearofice.sim.env.HeldLightMath.Source;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Held light on the screen (Minecraft development › Held light; prototype P4): the lit torches, candles and lamps that
 * players carry, drawn as light with no light blocks and no other mod. Client only; the server's number is
 * {@code env.light}, and this class only shows. Both read the same data map ({@code yearofice:light}) and the same
 * arithmetic ({@link HeldLightMath}), so what the screen shows is what the animals and the crafting-in-the-dark
 * rule count.
 *
 * <p>Twice a tick-pair (10 times a second) it takes the lights of every player in the level (up to 8 within 16 blocks
 * of the camera) into an immutable list. Two mixins read the list: {@code LevelRenderer.getLightColor} lifts a block's
 * drawn light, so terrain meshes built or rebuilt while a light is near show it, and
 * {@code EntityRenderer.getBlockLightLevel} lifts entities and the held item. When a light moves to another block (or
 * goes on or off) the sections round its old and new place are marked for rebuild, at most 10 times a second.
 *
 * <p>With Sodium or Embeddium the terrain does not go through {@code LevelRenderer.getLightColor}, so the terrain
 * sections are not rebuilt (it would cost frames and show nothing): entities and the held item still light, which is
 * the Engine's plan until P4 finds a terrain path there.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class HeldLight {
    private HeldLight() {}

    /** Sections are rebuilt at most this often (client ticks): every 2 ticks is 10 times a second. */
    static final int REFRESH_TICKS = 2;

    private record Spot(int x, int y, int z, int level) {}

    private static volatile List<Source> sources = List.of();
    private static List<Spot> spots = List.of();
    private static int ticks;
    private static final boolean TERRAIN = !(ModList.get().isLoaded("sodium") || ModList.get().isLoaded("embeddium"));

    /** The carried lights now (immutable; safe to read from the chunk-meshing threads). */
    public static List<Source> sources() { return sources; }

    /** Held light on a block, 0-15 (the block's centre against the carried lights). */
    public static int at(BlockPos pos) {
        List<Source> s = sources;
        return s.isEmpty() ? 0 : HeldLightMath.at(s, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /** {@code packed} (block light in bits 4-7, sky in bits 20-23) with the block light raised by the held light. */
    public static int lift(int packed, BlockPos pos) {
        int held = at(pos);
        if (held <= 0) return packed;
        int block = (packed >> 4) & 0xF;
        return held <= block ? packed : (packed & ~0xF0) | (held << 4);
    }

    /** {@code blockLight} (0-15) raised by the held light. */
    public static int liftBlock(int blockLight, BlockPos pos) { return Math.max(blockLight, at(pos)); }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            clear();
            return;
        }
        if (++ticks % REFRESH_TICKS != 0) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().isInitialized() ? mc.gameRenderer.getMainCamera().getPosition() : mc.player.getEyePosition();
        List<Source> found = new ArrayList<>(4);
        for (AbstractClientPlayer p : level.players()) {
            Vec3 eye = p.getEyePosition();
            if (eye.distanceToSqr(cam) > HeldLightMath.RANGE * HeldLightMath.RANGE) continue;
            Vec3 look = p.getLookAngle();
            for (InteractionHand h : InteractionHand.values()) {
                LightData d = shining(p.getItemInHand(h));
                if (d != null) found.add(new Source(eye.x, eye.y, eye.z, d.level(), d.coneDeg(), look.x, look.y, look.z));
            }
        }
        found.sort((a, b) -> Double.compare(dist2(a, cam), dist2(b, cam)));
        if (found.size() > HeldLightMath.MAX_SOURCES) found = new ArrayList<>(found.subList(0, HeldLightMath.MAX_SOURCES));
        sources = List.copyOf(found);
        List<Spot> now = new ArrayList<>(found.size());
        for (Source s : found) now.add(new Spot((int) Math.floor(s.x()), (int) Math.floor(s.y()), (int) Math.floor(s.z()), s.level()));
        rebuild(mc, spots, now);
        spots = now;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { clear(); }

    private static void clear() {
        sources = List.of();
        spots = List.of();
    }

    private static double dist2(Source s, Vec3 c) {
        double dx = s.x() - c.x, dy = s.y() - c.y, dz = s.z() - c.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** The light a stack gives while carried, or null: the data map row, lit (or always-on) and with minutes left. */
    static LightData shining(ItemStack stack) {
        if (stack.isEmpty()) return null;
        LightData d = stack.getItemHolder().getData(LightData.MAP);
        if (d == null || !LightState.shining(stack, d)) return null;
        if (d.burnMin() > 0 && LightState.minutesLeft(stack, d) <= 0) return null;
        return d;
    }

    /** Marks the sections round every light that moved to another block, appeared, vanished or changed level. */
    private static void rebuild(Minecraft mc, List<Spot> before, List<Spot> after) {
        if (!TERRAIN || mc.levelRenderer == null) return;
        List<Spot> moved = new ArrayList<>();
        for (Spot s : before) if (!after.contains(s)) moved.add(s);
        for (Spot s : after) if (!before.contains(s)) moved.add(s);
        for (Spot s : moved) {
            int r = s.level() + 1;
            mc.levelRenderer.setBlocksDirty(s.x() - r, s.y() - r, s.z() - r, s.x() + r, s.y() + r, s.z() + r);
        }
    }
}
