package com.yearofice.client.pack;

import com.yearofice.client.hud.HudDebug;
import com.yearofice.client.hud.HudSmoke;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireRegistry;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.item.inventory.PackItems;
import com.yearofice.item.inventory.PackSlots;
import com.yearofice.item.inventory.PackSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Adds the pack screen to the HUD smoke (video 2, A9): the inventory with no pack (9 belt slots and the outline) and
 * with a crate backpack (rows A and B only), in survival, at GUI scale 2 and 4.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class PackSmoke {
    private PackSmoke() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        if (!"hud".equals(System.getenv("YEAROFICE_SMOKE"))) return;
        for (int scale : new int[] {2, 4}) {
            HudSmoke.queue(new HudSmoke.Shot("pack_none", scale, () -> open(false)));
            HudSmoke.queue(new HudSmoke.Shot("pack_crate", scale, () -> open(true)));
        }
    }

    static void open(boolean crate) {
        Minecraft mc = Minecraft.getInstance();
        HudDebug.set(HudDebug.Mode.OFF);
        mc.options.hideGui = false;
        var server = mc.getSingleplayerServer();
        if (server != null && mc.player != null) {
            var id = mc.player.getUUID();
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null) return;
                p.setGameMode(GameType.SURVIVAL);
                p.getInventory().setItem(1, new ItemStack(FishingRegistry.ROD.get()));
                p.getInventory().setItem(2, new ItemStack(FireRegistry.MATCH.get(), 3));
                if (!crate) PackSlots.setBack(p, ItemStack.EMPTY);
                if (crate) {
                    if (PackSlots.back(p).isEmpty()) PackSlots.setBack(p, new ItemStack(PackItems.CRATE_BACKPACK.get()));
                    p.getInventory().setItem(10, new ItemStack(FishingRegistry.WORM.get(), 12));
                    p.getInventory().setItem(20, new ItemStack(FireRegistry.BOW_DRILL.get()));
                }
                PackSync.refresh(p);
            });
        }
        mc.execute(() -> mc.setScreen(new InventoryScreen(mc.player)));
    }
}
