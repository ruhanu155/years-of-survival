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
import com.yearofice.sim.ecosystem.FieldCare;
import com.yearofice.sim.ecosystem.Yields;
import net.minecraft.server.level.ServerPlayer;
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
    public static List<ItemStack> parts(ServerLevel level, CarcassRecord r) { return parts(level, r, new FieldCare.Result(0, 0), false); }

    static final java.util.Set<String> SCALED = java.util.Set.of("meat", "animal_fat", "heart", "liver", "kidney", "tongue", "marrow_bone", "sinew");

    /**
     * Big game: the table gives the reference animal's parts (the file's {@code whole_kg}); an individual's meat, fat,
     * organs, marrow and sinew scale with its own live weight ({@link Yields#scale}), fat follows the season
     * ({@link Yields#fatFactor}), only a bull moose, a buck or a caribou carries antlers, late gutting loses a quarter
     * of the meat, and the share the quartering deadline missed comes out Turning ({@link FieldCare}).
     */
    public static List<ItemStack> parts(ServerLevel level, CarcassRecord r, FieldCare.Result care, boolean frozen) {
        SpeciesDef d = SpeciesBook.get(r.species());
        String table = d.butcherTable().isEmpty() ? YearOfIce.MOD_ID + ":butchering/" + r.path() : d.butcherTable();
        LootTable lt = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(table)));
        List<ItemStack> out = new ArrayList<>(lt.getRandomItems(new LootParams.Builder(level).create(LootContextParamSets.EMPTY)));
        // guts are bait, one per 0.1 kg up to four (Blueprint: Guts 10% "Bait (one per 0.1 kg)"); the rest stays as the gut pile
        int guts = Math.min(4, (int) Math.floor(d.gutsKg() / 0.1 + 1e-6));
        if (guts > 0) out.add(new ItemStack(com.yearofice.hunt.fishing.FishingRegistry.FISH_GUTS.get(), guts));
        boolean big = d.carcassClass().equals("large") || d.carcassClass().equals("huge");
        if (big) out = scaleBig(level, r, d, out, care, frozen);
        double dNow = Freshness.dNow(level);
        List<String> meat = new ArrayList<>();
        for (SpeciesDef.Part part : d.parts()) if (part.kind().equals("meat")) meat.add(part.item());
        for (ItemStack s : out) {
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            if (meat.contains(id)) {
                Item item = s.getItem();
                Fresh f = s.has(YoiComponents.FRESH.get()) ? s.get(YoiComponents.FRESH.get()) : Freshness.dressed(r.fresh(), item, dNow);
                s.set(YoiComponents.FRESH.get(), f);
            }
        }
        return out;
    }

    static List<ItemStack> scaleBig(ServerLevel level, CarcassRecord r, SpeciesDef d, List<ItemStack> in, FieldCare.Result care, boolean frozen) {
        double ratio = Yields.scale(r.weightG() / 1000.0, d.wholeKg());
        int doy = com.yearofice.sim.time.Calendar.dayOfYearOfMinute(SliceClock.minute(level));
        boolean male = r.sex() == 1;
        double dNow = Freshness.dNow(level);
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : in) {
            String path = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            if (path.equals("antler") && !(male || r.species().endsWith("caribou"))) continue;
            if (path.equals("antler")) {
                // the head's size travels with the antler to the trophy wall (A5): the kill's weight against its species' reference animal
                double kg = r.weightG() / 1000.0, z = (kg - d.wholeKg()) / (0.15 * d.wholeKg());
                s.set(YoiComponents.TROPHY_SIZE.get(), new com.yearofice.item.component.TrophySize("kg", kg, z, z >= 2 ? "monster" : z >= 1 ? "trophy" : z >= -1 ? "fair" : "small"));
            }
            if (frozen && (path.endsWith("_hide") || path.endsWith("_pelt"))) continue;
            boolean meat = path.endsWith("_meat");
            String kind = meat ? "meat" : path;
            if (SCALED.contains(kind)) {
                double k = ratio * (path.equals("animal_fat") ? Yields.fatFactor(r.species(), male, doy) : 1.0);
                if (meat) {
                    int total = Math.max(0, (int) Math.round(s.getCount() * ratio));
                    int kept = (int) Math.round(total * (1 - care.lost()));
                    int turned = (int) Math.round(kept * care.turning());
                    if (kept - turned > 0) out.add(s.copyWithCount(kept - turned));
                    if (turned > 0) {
                        ItemStack t = s.copyWithCount(turned);
                        t.set(YoiComponents.FRESH.get(), Freshness.turning(s.getItem(), dNow));
                        out.add(t);
                    }
                    continue;
                }
                int n = (int) Math.round(s.getCount() * k);
                if (n > 0) out.add(s.copyWithCount(n));
            } else {
                out.add(s);
            }
        }
        return out;
    }

    /** Steps run through {@code last} (inclusive), in game minutes, in the order of the species file. */
    public static double minutesThrough(SpeciesDef d, String last) {
        double t = 0;
        for (java.util.Map.Entry<String, Double> e : d.steps().entrySet()) {
            t += e.getValue();
            if (e.getKey().equals(last)) break;
        }
        return t;
    }

    /**
     * Dresses a large or huge carcass where it lies (Blueprint › Field care): the steps run as one time-lapse of
     * their total game minutes, so the clock moves on and the deadlines are judged at the steps' own finish times
     * ({@code gut} done at the position and gut steps, {@code quarter} at the quarter step). Frozen carcasses take
     * three times as long and lose the hide. The parts drop beside the body; the guts stay as a gut pile.
     */
    public static boolean dressInPlace(ServerPlayer p, CarcassEntity c) {
        CarcassRecord r = c.record();
        ServerLevel level = p.serverLevel();
        SpeciesDef d = SpeciesBook.get(r.species());
        if (BodyService.get().energy(p) < EcoConfig.tuning().minWorkEnergy()) {
            p.displayClientMessage(Component.translatable("ecosystem.yearofice.too_tired"), true);
            return false;
        }
        double airC = com.yearofice.api.env.EnvService.installed() ? com.yearofice.api.env.EnvService.get().airC(level, c.blockPosition()) : 5;
        double sinceH = Math.max(0, (SliceClock.minuteD(level) - r.deathMinute()) / 60.0);
        boolean frozen = sinceH >= FieldCare.freezesSolidHours(r.species(), airC);
        double minutes = minutes(r) * (frozen ? 3 : 1);
        double gutH = sinceH + minutesThrough(d, "gut") / 60.0;
        double quarterH = sinceH + minutesThrough(d, "quarter") / 60.0 * (frozen ? 3 : 1);
        FieldCare.Result care = FieldCare.dress(gutH, quarterH, airC);
        List<ItemStack> parts = parts(level, r, care, frozen);
        SliceClock.advance(p, (int) Math.round(minutes));
        p.swing(InteractionHand.MAIN_HAND, true);
        net.minecraft.world.phys.Vec3 at = c.position().add(0, 0.5, 0);
        for (ItemStack part : parts) {
            while (!part.isEmpty()) {
                int n = Math.min(part.getCount(), Math.max(1, part.getMaxStackSize()));
                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, at.x, at.y, at.z, part.split(n)));
            }
        }
        leaveGuts(level, c.blockPosition(), d);
        p.displayClientMessage(Component.translatable("ecosystem.yearofice.dressed_big",
                Component.translatable("entity." + r.species().replace(':', '.')), Math.round(care.usable() * 100)), true);
        c.discard();
        return true;
    }
}
