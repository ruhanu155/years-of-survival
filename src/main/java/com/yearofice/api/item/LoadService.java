package com.yearofice.api.item;

import com.yearofice.sim.pack.LoadBands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The Item model's load reading (Layer 6, Load; Player systems and client › Inventory and slots): what a player
 * carries, the worn pack's limit, and the band, which the body reads as a calorie factor and the movement speed
 * reads as the {@code yearofice:load} modifier.
 */
public interface LoadService {

    /** Kilograms carried: hotbar, off-hand, armor, pack rows, Curios slots, and the contents of carried packs. */
    double kg(Player player);

    /** The worn pack's {@code max_kg}, or the belt's 5 kg with no pack. */
    double maxKg(Player player);

    /** Pack rows open (0 with no pack). */
    int openRows(Player player);

    /** The band now, with Strength from the body. */
    LoadBands.Band band(ServerPlayer player);

    /** The body's calorie multiplier for the band. */
    double kcalFactor(ServerPlayer player);

    static LoadService get() {
        LoadService s = Holder.service;
        if (s == null) throw new IllegalStateException("LoadService not installed yet");
        return s;
    }

    static void install(LoadService service) {
        if (Holder.service != null) throw new IllegalStateException("LoadService already installed");
        Holder.service = service;
    }

    final class Holder {
        private static volatile LoadService service;

        private Holder() {}
    }
}
