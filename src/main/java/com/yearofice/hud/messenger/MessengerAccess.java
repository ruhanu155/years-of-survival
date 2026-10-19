package com.yearofice.hud.messenger;

import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.item.messenger.TrailMessengerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Where the bus looks for the Trail Messenger (Layer 8, Bus rules): a hand, or the Curios {@code neck} slot. Only a
 * device with battery above 0 carries messenger lines; otherwise they go to the journal.
 */
public final class MessengerAccess {
    private MessengerAccess() {}

    public static final String NECK = "neck";

    /** The battery of the best carried messenger in a hand or the neck slot, or −1 with none there. */
    public static int battery(ServerPlayer p) {
        int best = -1;
        for (ItemStack s : new ItemStack[] {p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.is(MessengerItems.MESSENGER.get())) best = Math.max(best, TrailMessengerItem.battery(s));
        }
        var inv = CuriosApi.getCuriosInventory(p);
        if (inv.isPresent()) {
            for (var r : inv.get().findCurios(s -> s.is(MessengerItems.MESSENGER.get()), true, NECK)) {
                best = Math.max(best, TrailMessengerItem.battery(r.stack()));
            }
        }
        return best;
    }

    /** True when messenger lines can reach the screen. */
    public static boolean live(ServerPlayer p) { return battery(p) > 0; }
}
