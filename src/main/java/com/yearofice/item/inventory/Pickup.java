package com.yearofice.item.inventory;

import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * {@code item.inventory.Pickup} (Player systems and client › Inventory and slots, No walk-over pickup; Full pack):
 * walking over an item never picks it up, unless it is in {@code #yearofice:auto_pickup} and the player turned
 * auto-pickup on. A pickup is the use key on the item ({@code yearofice:pickup}); the server checks reach, the
 * worn pack's {@code max_kg} and the 45 kg total, and takes what fits into open slots. What does not fit stays on
 * the ground, glowing, with "Pack full: 2 left on the ground".
 */
public final class Pickup {
    private Pickup() {}

    public static final TagKey<Item> AUTO_PICKUP = ItemTags.create(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "auto_pickup"));

    public static void onPickupPre(ItemEntityPickupEvent.Pre e) {
        if (!(e.getPlayer() instanceof ServerPlayer p)) return;
        boolean auto = PackAttachments.rules(p).autoPickup() && e.getItemEntity().getItem().is(AUTO_PICKUP);
        if (!auto) e.setCanPickup(TriState.FALSE);
    }

    /** What happened, for the tests and the line. */
    public enum Result { TAKEN, PARTIAL, FULL, TOO_HEAVY, OUT_OF_REACH, GONE }

    public static Result request(ServerPlayer p, int entityId) {
        Entity e = p.serverLevel().getEntity(entityId);
        if (!(e instanceof ItemEntity item)) return Result.GONE;
        return pickUp(p, item);
    }

    public static Result pickUp(ServerPlayer p, ItemEntity item) {
        if (!item.isAlive() || item.getItem().isEmpty()) return Result.GONE;
        double reach = PackConfig.get("pickup_reach_m") + 1.5;
        if (item.distanceToSqr(p.getEyePosition()) > reach * reach) return Result.OUT_OF_REACH;
        ItemStack stack = item.getItem();
        double unitKg = LoadCalculator.stackKg(stack.copyWithCount(1), true);
        double kg = LoadCalculator.kgOf(p);
        double limit = Math.min(LoadCalculator.maxKgOf(p), PackConfig.get("refuse_over_kg"));
        int fitWeight = unitKg <= 0 ? stack.getCount() : (int) Math.floor((limit - kg) / unitKg + 1e-9);
        if (fitWeight <= 0) {
            boolean overTotal = kg + unitKg > PackConfig.get("refuse_over_kg");
            line(p, overTotal ? "too_heavy" : "pack_full_kg", HudArg.fixed(limit, "kg"));
            item.setGlowingTag(true);
            return overTotal ? Result.TOO_HEAVY : Result.FULL;
        }
        int want = Math.min(stack.getCount(), fitWeight);
        ItemStack moving = stack.copyWithCount(want);
        p.getInventory().add(moving);
        int taken = want - moving.getCount();
        if (taken <= 0) {
            line(p, "left", HudArg.of(stack.getCount()));
            item.setGlowingTag(true);
            return Result.FULL;
        }
        p.take(item, taken);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        stack.shrink(taken);
        line(p, "got", HudArg.of(taken), HudArg.item(id));
        if (stack.isEmpty()) {
            item.discard();
            return Result.TAKEN;
        }
        item.setItem(stack);
        item.setGlowingTag(true);
        line(p, "left", HudArg.of(stack.getCount()));
        return Result.PARTIAL;
    }

    /** Puts a made or butchered stack in the pack, or drops it at the feet with the "Pack full" line (never silently). */
    public static void giveOrDrop(ServerPlayer p, ItemStack s) {
        int n = s.getCount();
        if (p.getInventory().add(s)) return;
        int left = s.getCount();
        if (left <= 0) return;
        ItemEntity ie = p.drop(s, false);
        if (ie != null) ie.setGlowingTag(true);
        line(p, "left", HudArg.of(left));
    }

    static void line(ServerPlayer p, String id, HudArg... args) {
        HudBus.get().post(p, HudLine.of(HudLineType.PICKUP, id, "item.inventory", args));
    }
}
