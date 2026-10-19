package com.yearofice.item.messenger;

import com.yearofice.core.YearOfIce;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The player arrives with the Trail Messenger round the neck (the Curios {@code neck} slot, or the pockets when the
 * slot is missing): it is given once, on the first join of each world. The flag lives
 * in the persisted player data, so a flight out never gives a second one. The rest of the starting kit is M3
 * ({@code loot_table/player/starting_kit.json}).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class MessengerKit {
    private MessengerKit() {}

    public static final String GIVEN_KEY = "yearofice:messenger_given";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) giveIfFirstJoin(p);
    }

    /** Gives the messenger if this player never had one here. Returns true if it gave one. */
    public static boolean giveIfFirstJoin(ServerPlayer p) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(GIVEN_KEY)) return false;
        ItemStack stack = new ItemStack(MessengerItems.MESSENGER.get());
        // On a lanyard in the first neck slot (Inventory and slots), so its lines reach the screen; else the pockets.
        boolean worn = com.yearofice.item.inventory.PackSlots.get(p, com.yearofice.item.inventory.PackSlots.NECK, 0).isEmpty()
                && com.yearofice.item.inventory.PackSlots.set(p, com.yearofice.item.inventory.PackSlots.NECK, 0, stack);
        if (!worn && !p.getInventory().add(stack)) p.drop(stack, false);
        persisted.putBoolean(GIVEN_KEY, true);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return true;
    }
}
