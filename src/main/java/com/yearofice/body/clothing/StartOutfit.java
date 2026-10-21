package com.yearofice.body.clothing;

import com.yearofice.sim.body.Layer;
import com.yearofice.sim.body.Outfit;
import com.yearofice.sim.body.Zone;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The start kit as actually worn: on a player's first login (and on an SOS return) each start garment is put in its
 * empty slot. {@link #dressed} marks a player the body reads clothing for; without it the body keeps the old implied kit.
 */
@EventBusSubscriber(modid = com.yearofice.core.YearOfIce.MOD_ID)
public final class StartOutfit {
    private StartOutfit() {}

    static final String KEY = "yoi_dressed";

    public static boolean dressed(Player p) { return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(KEY); }

    /** Marks the player as dressed without putting anything on (naked tests). */
    public static void mark(Player p) {
        CompoundTag t = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        t.putBoolean(KEY, true);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, t);
    }

    /** Puts every start garment into its empty slot and marks the player dressed. */
    public static void dress(Player p) {
        for (String id : Outfit.START_IDS) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(ResourceLocation.fromNamespaceAndPath(com.yearofice.core.YearOfIce.MOD_ID, id));
            ItemStack st = new ItemStack(item);
            WearMap.Wear w = ClothingSlots.row(st);
            if (w == null) continue;
            ClothingSlots.put(p, Zone.valueOf(w.zone().toUpperCase(Locale.ROOT)), Layer.valueOf(w.layer().toUpperCase(Locale.ROOT)), st);
        }
        mark(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !dressed(p)) dress(p);
    }
}
