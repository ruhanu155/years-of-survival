package com.yearofice.body.illness;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Face grease (B13): sneak and use any item in {@code #yearofice:grease} (lard, animal fat, bone grease) to rub 10 g
 * into the face. {@code action/grease_face.json} and {@code action/grease_gloves.json} hold the numbers; the one
 * game minute hold arrives with the work service (the use is instant until then).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class Grease {
    private Grease() {}

    public static final TagKey<Item> GREASE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "grease"));

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        ItemStack stack = e.getItemStack();
        if (!stack.is(GREASE)) return;
        Illnesses.greaseFace(p);
        if (!p.isCreative()) stack.shrink(1);
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.SLIME_SQUISH_SMALL,
                SoundSource.PLAYERS, 0.5f, 1.2f);
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }
}
