package com.yearofice.item.inventory.mixin;

import com.yearofice.item.inventory.PackRules;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No ghost slots (video 2, A9; RECONCILE 170): a player-inventory slot in a pack row the worn pack does not open is
 * inactive, so no screen draws it, hovers it or places into it, the pack screen and every container screen alike.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow @Final public Container container;

    @Shadow public abstract int getContainerSlot();

    private boolean yearofice$locked() {
        return container instanceof Inventory inv && !PackRules.isOpen(inv.player, getContainerSlot());
    }

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void yearofice$active(CallbackInfoReturnable<Boolean> cir) {
        if (yearofice$locked()) cir.setReturnValue(false);
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void yearofice$mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (yearofice$locked()) cir.setReturnValue(false);
    }
}
