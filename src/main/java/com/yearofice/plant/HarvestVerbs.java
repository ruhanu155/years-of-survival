package com.yearofice.plant;

import com.yearofice.api.clock.ClockService;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.plant.PlantPhase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The harvest verbs (Plants and fungi › Harvest verbs): pick, strip, cut, dig and uproot, chosen by the tool in hand
 * and the plant's phase. Each action takes a third of a clump's units, raises {@code taken}, and books the patch
 * ledger; the clump comes back on its own clock. (Peel, tap and coppice belong to later milestones.)
 */
public final class HarvestVerbs {
    private HarvestVerbs() {}

    public record Result(boolean ok, String message, ItemStack stack) {
        static Result no(String m) { return new Result(false, m, ItemStack.EMPTY); }
    }

    /** The verb for the tool in hand: bare hand picks (strips when crouched), knife cuts, digging stick and shovel dig (crouched with a shovel: uproot). */
    public static String verbFor(ItemStack tool, boolean crouching) {
        if (tool.isEmpty()) return crouching ? "strip" : "pick";
        String id = BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath();
        return switch (id) {
            case "fixed_blade_knife", "knife", "hatchet" -> "cut";
            case "digging_stick", "hoe" -> "dig";
            case "shovel" -> crouching ? "uproot" : "dig";
            default -> "pick";
        };
    }

    public static Result act(ServerPlayer pl, ServerLevel level, BlockPos pos, String verbName) {
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof PlantBlock pb)) return Result.no("There is nothing to harvest here.");
        PlantSpec spec = PlantData.get(pb.species());
        if (spec == null) return Result.no("This plant has no data.");
        String shown = Knowledge.shownName(pl, spec);
        PlantPhase phase = st.getValue(PlantBlock.PHASE).sim();
        com.yearofice.sim.plant.ForageTuning t = tuning();
        int today = (int) ClockService.get().absDay();
        // Fall back to the verbs the plant has: a knife on a berry still picks.
        String name = verbName;
        String lookup = name.equals("strip") ? "pick" : name;
        if (!spec.verbs.containsKey(lookup)) {
            for (String alt : List.of("pick", "cut", "dig")) if (spec.verbs.containsKey(alt)) { name = alt.equals(verbName) ? name : alt; lookup = alt; break; }
        }
        PlantSpec.Verb v = spec.verbs.get(lookup);
        if (v == null) return Result.no("You can't do that to " + shown + ".");
        if (name.equals("uproot")) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return new Result(true, "You pull up " + shown + ".", new ItemStack(Items.STICK));
        }
        if (name.equals("strip")) {
            PlantSpec.Verb sv = spec.verbs.get("strip");
            if (sv != null && !sv.allowed()) return Result.no("You can't strip " + shown + ": " + sv.reason().replace('_', ' ') + ".");
        }
        if (phase == PlantPhase.BURIED) return Result.no("It is under snow. Clear it first.");
        PatchData d = PatchLedger.data(level.getChunkAt(pos));
        long key = pos.asLong();
        Integer ready = d.ready.get(key);
        int taken = st.getValue(PlantBlock.TAKEN);
        if (taken > 0 && ready != null && today >= ready && v.phases().contains(phase)) {
            taken = 0;
            d.ready.remove(key);
        }
        if (!v.phases().contains(phase)) return Result.no("There is nothing to take from " + shown + " in the " + phase.key() + " phase.");
        PatchData.Patch patch = PatchLedger.patch(level, pos, spec);
        double yield = t.yieldScale() * patch.s * patch.h * fYear();
        double grams;
        double units;
        int newTaken;
        int days;
        switch (name) {
            case "strip" -> {
                int left = 3 - taken;
                if (left <= 0) return Result.no("There is nothing left on this clump.");
                grams = v.unitG() * v.unitsPerClump() * yield * left / 3.0;
                units = v.unitsPerClump() * left / 3.0 * 1.5;
                newTaken = 3;
                days = regrow(level, spec, t);
            }
            case "dig" -> {
                if (taken >= 2) return Result.no("Only the root division is left. Leave it to grow.");
                grams = v.unitG() * yield;
                units = 1.0;
                newTaken = taken + 1;
                days = 300;
            }
            default -> {
                if (taken >= 3) return Result.no("There is nothing left on this clump.");
                grams = v.unitG() * v.unitsPerClump() * yield / 3.0;
                units = v.unitsPerClump() / 3.0 * v.strippedWeight();
                newTaken = taken + 1;
                days = name.equals("cut") ? (int) Math.round(14 * t.regrowScale()) : regrow(level, spec, t);
            }
        }
        level.setBlock(pos, st.setValue(PlantBlock.TAKEN, newTaken), 3);
        d.ready.put(key, today + Math.max(1, days));
        patch.picked += units;
        patch.lastDay = today;
        level.getChunkAt(pos).setUnsaved(true);
        ItemStack out = part(v.part(), spec, Math.max(1, (int) Math.round(grams)));
        if (!pl.getInventory().add(out.copy())) pl.drop(out.copy(), false);
        boolean named = Knowledge.seen(pl, spec, phase);
        String msg = shown + ": " + Math.round(grams) + " g (" + (named ? "now you know it: " + spec.name + "; " : "") + "clump " + newTaken + "/3, patch "
                + Math.round(PatchLedger.pickedShare(patch) * 100) + "% picked, health " + com.yearofice.sim.plant.PatchModel.word(patch.h) + ")";
        return new Result(true, msg, out);
    }

    static com.yearofice.sim.plant.ForageTuning tuning() { return ForageConfig.tuning(); }

    static double fYear() { return PlantDials.berryIndex; }

    private static int regrow(ServerLevel level, PlantSpec spec, com.yearofice.sim.plant.ForageTuning t) {
        int span = Math.max(0, spec.regrowHiDays - spec.regrowLoDays);
        return (int) Math.round((spec.regrowLoDays + (span == 0 ? 0 : level.random.nextInt(span + 1))) * t.regrowScale());
    }

    static ItemStack part(String itemId, PlantSpec spec, int grams) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId.isEmpty() ? "yearofice:plant_material" : itemId));
        boolean material = item == Items.AIR || itemId.equals("yearofice:plant_material") || itemId.isEmpty();
        ItemStack s = new ItemStack(material ? PlantRegistry.PLANT_MATERIAL.get() : item);
        s.set(YoiComponents.WEIGHT_G.get(), grams);
        if (material) {
            CompoundTag tag = new CompoundTag();
            tag.putString("species", spec.id);
            s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return s;
    }

}
