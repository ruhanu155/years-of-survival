package com.yearofice.fire;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.fire.LayPart;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** What an item is to a fire: a lay part, a way to light it, or nothing; and how wet it is. */
public final class FireParts {
    private FireParts() {}

    /** {@code yearofice:fuel}: wood, armload share and default moisture per fuel item. */
    public static final DataMapType<Item, FuelData> FUEL = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "fuel"), Registries.ITEM, FuelData.CODEC).synced(FuelData.CODEC, false).build();

    /** Default moisture by part when a stack carries no {@code yearofice:moisture}: dry kit as it comes from the crate. */
    public static final int TINDER_MOISTURE = 10;
    public static final int KINDLING_MOISTURE = 15;
    public static final int MATCH_MOISTURE = 0;
    public static final int BOARD_MOISTURE = 15;

    static void registerDataMaps(RegisterDataMapTypesEvent event) { event.register(FUEL); }

    public static FuelData fuel(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getItemHolder().getData(FUEL);
    }

    /** The lay part an item makes, or null. */
    public static LayPart part(ItemStack stack) {
        if (stack.is(FireRegistry.TINDER_BUNDLE.get())) return LayPart.TINDER;
        if (stack.is(FireRegistry.KINDLING.get())) return LayPart.KINDLING;
        if (fuel(stack) != null) return LayPart.FUEL;
        return null;
    }

    /** True for anything the fire ring reacts to when held. */
    public static boolean handled(ItemStack stack) {
        return part(stack) != null || stack.is(FireRegistry.MATCH.get()) || stack.is(FireRegistry.BOW_DRILL.get())
                || stack.is(FireRegistry.FIRE_STARTER.get()) || FireRingBlockEntity.container(stack) != null;
    }

    /** The stack's moisture: its component, or the part's default. */
    public static int moisture(ItemStack stack) {
        Integer m = stack.get(FireRegistry.MOISTURE.get());
        if (m != null) return m;
        if (stack.is(FireRegistry.TINDER_BUNDLE.get())) return TINDER_MOISTURE;
        if (stack.is(FireRegistry.KINDLING.get())) return KINDLING_MOISTURE;
        if (stack.is(FireRegistry.MATCH.get())) return MATCH_MOISTURE;
        if (stack.is(FireRegistry.BOW_DRILL.get())) return BOARD_MOISTURE;
        FuelData f = fuel(stack);
        return f != null ? f.moisturePct() : 0;
    }
}
