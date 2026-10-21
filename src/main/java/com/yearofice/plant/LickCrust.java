package com.yearofice.plant;

import com.yearofice.api.clock.ClockService;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.plant.LickModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Scraping and boiling lick salt (A4). Right mouse on crust with a knife takes 50 g of {@code lick_crust}
 * (15 game minutes of work, which the work service paces); a fire with a pot boils 2 kg of crust down to 150 g of
 * {@code lick_salt} and burns 1 armload. Both items are in {@code #yearofice:salt}.
 */
public final class LickCrust {
    private LickCrust() {}

    public static ItemStack crust(int grams) {
        ItemStack s = new ItemStack(LickRegistry.LICK_CRUST.get());
        s.set(YoiComponents.WEIGHT_G.get(), grams);
        return s;
    }

    public static ItemStack salt(int grams) {
        ItemStack s = new ItemStack(LickRegistry.LICK_SALT.get());
        s.set(YoiComponents.WEIGHT_G.get(), grams);
        return s;
    }

    /** A scrape's take and the game minutes of light work it costs (paced by hold-to-work when that lands). */
    public record Scrape(ItemStack stack, int minutes) {}

    /** One scrape at {@code pos}; the stack is empty when the lick is frozen or bare. */
    public static Scrape scrape(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SaltCrustBlockEntity be)) return new Scrape(ItemStack.EMPTY, 0);
        int doy = ClockService.get().dayOfYear();
        if (!LickModel.thawed(doy)) return new Scrape(ItemStack.EMPTY, 0); // "Frozen mud. Nothing to scrape."
        be.catchUp(level, (int) ClockService.get().absDay());
        double got = be.take(LickModel.SCRAPE_G);
        level.playSound(null, pos, LickRegistry.CRUST_SCRAPE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        return got <= 0 ? new Scrape(ItemStack.EMPTY, 0) : new Scrape(crust((int) Math.round(got)), LickModel.SCRAPE_MIN);
    }

    /** Boils 2 kg of crust (given as grams on hand) over a lit fire: 150 g of salt, one armload burned. Empty when it cannot. */
    public static ItemStack boil(FireRingBlockEntity ring, int crustG) {
        if (crustG < LickModel.BOIL_CRUST_G) return ItemStack.EMPTY;
        double fuel = ring.fire().armloads();
        if (fuel < 1.0) return ItemStack.EMPTY;
        ring.setFuel(fuel - 1.0);
        return salt(LickModel.BOIL_SALT_G);
    }

    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        if (!(level.getBlockState(e.getPos()).getBlock() instanceof SaltCrustBlock)) return;
        String tool = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath();
        if (!tool.equals("fixed_blade_knife") && !tool.equals("knife")) return;
        ItemStack got = scrape(level, e.getPos()).stack();
        if (got.isEmpty()) {
            Inspect.say(p, "Frozen mud or bare ground. Nothing to scrape.");
        } else {
            if (!p.getInventory().add(got.copy())) p.drop(got.copy(), false);
            Inspect.say(p, "White crust: " + got.get(YoiComponents.WEIGHT_G.get()) + " g scraped.");
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Licks near players catch up on the day pass; untouched licks catch up when next scraped. */
    public static void onDay(com.yearofice.api.event.ClockEvent.Day e) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!(p.level() instanceof ServerLevel level)) continue;
            BlockPos c = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-16, -4, -16), c.offset(16, 4, 16))) {
                if (level.isLoaded(q) && level.getBlockEntity(q) instanceof SaltCrustBlockEntity be) be.catchUp(level, (int) e.absDay());
            }
        }
    }
}
