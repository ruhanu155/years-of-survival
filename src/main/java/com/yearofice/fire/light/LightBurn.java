package com.yearofice.fire.light;

import com.yearofice.api.env.EnvService;
import com.yearofice.api.env.LightData;
import com.yearofice.api.env.LightState;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.env.light.HeldLights;
import com.yearofice.sim.env.Precip;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Lit lights burn down by game minutes, wherever they are carried: each {@link ClockEvent.Minute} takes the minutes
 * off every lit stack in every player's inventory. A torch or candle that reaches 0 is gone; a flat headlamp goes
 * dark and stays. Rain puts out a light that rain treats as {@code out} outdoors, and has a 20% chance a minute
 * against one that {@code holds} in heavy rain (Ideas from viewers › Pitch lights › Weather and cold). A battery lamp
 * below −30 °C drains twice as fast (Blueprint › Light sources in detail).
 */
public final class LightBurn {
    private LightBurn() {}

    /** Heavy rain's chance a minute to put out a light that holds in light rain. */
    public static final double HEAVY_RAIN_OUT = 0.20;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(LightBurn::onMinute);
    }

    private static void onMinute(ClockEvent.Minute e) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) burn(p, e.count(), p.getRandom().nextDouble());
    }

    /** Burns every lit light a player carries by {@code minutes}; {@code roll} in [0, 1) is the heavy-rain draw. */
    public static void burn(ServerPlayer p, int minutes, double roll) {
        if (minutes <= 0) return;
        boolean wet = false, heavy = false;
        double air = 0;
        if (EnvService.installed()) {
            EnvService env = EnvService.get();
            Precip pr = env.precip(p.serverLevel(), p.blockPosition());
            wet = pr.liquid() && p.serverLevel().canSeeSky(p.blockPosition().above());
            heavy = wet && env.heavy(p.serverLevel());
            air = env.airC(p.serverLevel(), p.blockPosition());
        }
        burn(p, minutes, roll, wet, heavy, air);
    }

    /** {@link #burn(ServerPlayer, int, double)} with the weather given: rain on the carrier, heavy rain, air temperature. */
    public static void burn(ServerPlayer p, int minutes, double roll, boolean wet, boolean heavy, double air) {
        if (minutes <= 0) return;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            LightData d = LightItem.data(s);
            if (d == null || !LightItem.lit(s)) continue;
            if (rainOut(d, wet, heavy, roll)) {
                LightState.set(s, false, LightState.minutesLeft(s, d));
                p.level().playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.3f);
                continue;
            }
            if (d.burnMin() <= 0) continue;
            int use = minutes * (LightItem.battery(d) && air < HeldLights.COLD_C ? 2 : 1);
            int left = LightState.minutesLeft(s, d) - use;
            if (left > 0) {
                LightState.set(s, true, left);
            } else if (LightItem.battery(d)) {
                LightState.set(s, false, 0);
            } else {
                p.getInventory().setItem(i, ItemStack.EMPTY);
                p.level().playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.0f);
            }
        }
    }

    /** True when the weather puts this light out this minute. */
    public static boolean rainOut(LightData d, boolean wet, boolean heavy, double roll) {
        if (!wet) return false;
        return switch (d.rain()) {
            case "never" -> false;
            case "holds" -> heavy && roll < HEAVY_RAIN_OUT;
            default -> true;
        };
    }
}
