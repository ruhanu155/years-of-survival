package com.yearofice.api.body;

import com.yearofice.sim.body.WornGarment;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a player wears (M7 lane F). The four vanilla armor slots carry the outer layers of head, torso, legs and feet;
 * Curios {@code head_inner, torso_inner, legs_inner, feet_inner, hands_inner, hands_outer} carry the rest (Minecraft
 * engine › Clothing slots). Rows come from {@code data_maps/item/wear.json}. The body package implements it.
 */
public interface ClothingService {
    /** The garments worn now, one per slot, condition from durability; server side only. */
    List<WornGarment> worn(ServerPlayer p);

    static ClothingService get() {
        ClothingService s = Holder.service;
        if (s == null) throw new IllegalStateException("ClothingService not installed yet");
        return s;
    }

    static void install(ClothingService service) {
        if (Holder.service != null) throw new IllegalStateException("ClothingService already installed");
        Holder.service = service;
    }

    final class Holder {
        private Holder() {}

        private static volatile ClothingService service;
    }
}
