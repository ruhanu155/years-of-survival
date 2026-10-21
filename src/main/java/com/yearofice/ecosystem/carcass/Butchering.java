package com.yearofice.ecosystem.carcass;

import com.yearofice.item.component.YoiComponents;
import com.yearofice.api.body.BodyService;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.ecosystem.Dressing;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Dressing small game (Bestiary › Dressing by size class; Blueprint › Field care, small game): one hold-to-work
 * job that runs the species' steps (a hare: gut 3, skin 5 and joint 5 game minutes) as a time-lapse at the Work
 * rate, 5 game minutes a real second, so a hare takes 2.6 real seconds. The parts come from
 * {@code loot_table/butchering/<species>.json} in 100 g units; meat parts named in {@code item_family/} carry the
 * carcass's freshness onto the meat's own life.
 *
 * <p>The job's calories go through the body: the hand swings every 10 ticks, which the body's activity sampler
 * books as heavy work for the game minutes the time-lapse passes. It stops below the minimum energy. The cleaning
 * spot, practice, the knife and the separate steps on a carcass card land in M5.
 */
public final class Butchering {
    private Butchering() {}

    /** Set by the client at setup, so tooltips can read the client level's time without a client import here. */
    public static volatile Supplier<Level> CLIENT_LEVEL = () -> null;

    static Level clientLevel() { return CLIENT_LEVEL.get(); }

    public static double minutes(CarcassRecord r) {
        SpeciesDef d = SpeciesBook.get(r.species());
        double m = d.dressMinutes();
        if (m <= 0) m = Dressing.scaled(13, Dressing.HARE_REF_KG, Math.max(0.05, r.weightG() / 1000.0));
        return m;
    }

    public static int holdTicks(CarcassRecord r) { return Dressing.holdTicks(minutes(r), EcoConfig.tuning().workMinPerS()); }

    static void workTick(ServerPlayer p, int remaining) {
        if (remaining % 10 == 0) p.swing(InteractionHand.MAIN_HAND, true);
        if (BodyService.get().energy(p) < EcoConfig.tuning().minWorkEnergy()) {
            p.displayClientMessage(Component.translatable("ecosystem.yearofice.too_tired"), true);
            p.stopUsingItem();
            return;
        }
        SliceClock.lapse(p, EcoConfig.tuning().workMinPerS());
    }

    /** The job is done: the carcass becomes its parts. */
    public static ItemStack finish(ServerPlayer p, ItemStack stack) {
        CarcassRecord r = WholeGameItem.record(stack);
        if (r == null) return stack;
        List<ItemStack> parts = parts(p.serverLevel(), r);
        stack.shrink(1);
        for (ItemStack part : parts) if (!p.getInventory().add(part)) p.drop(part, false);
        leaveGuts(p.serverLevel(), p.blockPosition(), SpeciesBook.get(r.species()));
        p.displayClientMessage(Component.translatable("ecosystem.yearofice.dressed",
                Component.translatable("entity." + r.species().replace(':', '.'))), true);
        return stack;
    }

    /** Guts of 50 g or more are left where the work was done, as a gut pile (Decay and time › Carcasses and gut piles). */
    public static boolean leaveGuts(ServerLevel level, net.minecraft.core.BlockPos at, SpeciesDef d) {
        if (d.gutsKg() < 0.05) return false;
        net.minecraft.core.BlockPos spot = null;
        for (net.minecraft.core.BlockPos c : new net.minecraft.core.BlockPos[] {at, at.north(), at.east(), at.south(), at.west()}) {
            if ((level.getBlockState(c).isAir() || level.getBlockState(c).canBeReplaced()) && level.getFluidState(c).isEmpty()
                    && level.getBlockState(c.below()).isFaceSturdy(level, c.below(), net.minecraft.core.Direction.UP)) {
                spot = c;
                break;
            }
        }
        if (spot == null) return false;
        level.setBlock(spot, EcosystemRegistry.GUT_PILE.get().defaultBlockState(), 3);
        return true;
    }

    /** Rolls the species' butchering table and stamps meat with the carcass's freshness. */
    public static List<ItemStack> parts(ServerLevel level, CarcassRecord r) {
        SpeciesDef d = SpeciesBook.get(r.species());
        String table = d.butcherTable().isEmpty() ? YearOfIce.MOD_ID + ":butchering/" + r.path() : d.butcherTable();
        LootTable lt = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(table)));
        List<ItemStack> out = new ArrayList<>(lt.getRandomItems(new LootParams.Builder(level).create(LootContextParamSets.EMPTY)));
        // guts are bait, one per 0.1 kg up to four (Blueprint: Guts 10% "Bait (one per 0.1 kg)"); the rest stays as the gut pile
        int guts = Math.min(4, (int) Math.floor(d.gutsKg() / 0.1 + 1e-6));
        if (guts > 0) out.add(new ItemStack(com.yearofice.hunt.fishing.FishingRegistry.FISH_GUTS.get(), guts));
        double dNow = Freshness.dNow(level);
        List<String> meat = new ArrayList<>();
        for (SpeciesDef.Part part : d.parts()) if (part.kind().equals("meat")) meat.add(part.item());
        for (ItemStack s : out) {
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            if (meat.contains(id)) {
                Item item = s.getItem();
                Fresh f = Freshness.dressed(r.fresh(), item, dNow);
                s.set(YoiComponents.FRESH.get(), f);
            }
        }
        return out;
    }
}
