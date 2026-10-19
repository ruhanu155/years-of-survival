package com.yearofice.item.inventory.mixin;

import com.yearofice.item.inventory.PackRules;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The engine's one {@code Inventory} mixin (Minecraft engine › Inventory and packs, Locked rows): the free-slot and
 * merge-slot searches skip pack rows the worn pack does not open, so pickups and shift-clicks never land there.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow @Final public NonNullList<ItemStack> items;
    @Shadow @Final public Player player;
    @Shadow public int selected;

    @Shadow public abstract ItemStack getItem(int index);

    @Shadow
    private boolean hasRemainingSpaceForItem(ItemStack destination, ItemStack origin) { throw new AssertionError(); }

    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void yearofice$freeSlot(CallbackInfoReturnable<Integer> cir) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty() && PackRules.isOpen(player, i)) {
                cir.setReturnValue(i);
                return;
            }
        }
        cir.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
    private void yearofice$spaceSlot(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (hasRemainingSpaceForItem(getItem(selected), stack)) {
            cir.setReturnValue(selected);
            return;
        }
        if (hasRemainingSpaceForItem(getItem(40), stack)) {
            cir.setReturnValue(40);
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            if (PackRules.isOpen(player, i) && hasRemainingSpaceForItem(items.get(i), stack)) {
                cir.setReturnValue(i);
                return;
            }
        }
        cir.setReturnValue(-1);
    }
}
