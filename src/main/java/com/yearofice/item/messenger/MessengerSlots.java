package com.yearofice.item.messenger;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Where a player carries a working Trail Messenger (Layer 8, Bus rules; Player systems and client › Inventory and
 * slots): a hand, or the Curios {@code neck} slot it arrives in. One definition, read by the HUD bus (which lines can
 * show) and the body ({@code body_sync}'s battery).
 */
public final class MessengerSlots {
    private MessengerSlots() {}

    public static final String NECK = "neck";

    /** The battery of the best messenger in a hand or the neck slot, or −1 with none there. */
    public static int battery(ServerPlayer p) {
        int best = -1;
        for (ItemStack s : new ItemStack[] {p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.is(MessengerItems.MESSENGER.get())) best = Math.max(best, TrailMessengerItem.battery(s));
        }
        // The neck slot's own stacks, read now: Curios' findCurios caches its answer for the game tick and searches
        // every slot type whatever slot it is given.
        var neck = CuriosApi.getCuriosInventory(p).flatMap(i -> i.getStacksHandler(NECK));
        if (neck.isPresent()) {
            var stacks = neck.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack s = stacks.getStackInSlot(i);
                if (s.is(MessengerItems.MESSENGER.get())) best = Math.max(best, TrailMessengerItem.battery(s));
            }
        }
        return best;
    }
}
