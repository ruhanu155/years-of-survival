package com.yearofice.build.craft;

import com.mojang.serialization.Codec;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.build.ToolWear;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tool durability and Edge (Crafting and building in depth › Tool durability). Durability is the stack's vanilla
 * damage; Edge is the component {@code yearofice:edge}, 0 to 120, where a stack without it reads 100 (sharp). A use
 * is one swing or strike, or one game minute of cutting: {@link #use} charges the durability (double when cold for
 * wooden and stone tools), takes {@code 100 / edge_uses} off the Edge, chips an iron edge below -40 C, warns once at
 * 25% and breaks the tool at 0. Under 30 Edge a job takes x1.5 time ({@link #timeMult}).
 */
public final class Wear {
    private Wear() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);

    /** {@code yearofice:edge}: the sharpness of a cutting tool, 0-120 (a missing component reads 100). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> EDGE = COMPONENTS.registerComponentType("edge",
            b -> b.persistent(Codec.doubleRange(0, ToolWear.EDGE_MAX)).networkSynchronized(ByteBufCodecs.DOUBLE));

    public static double edge(ItemStack tool) { return tool.getOrDefault(EDGE.get(), ToolWear.EDGE_SHARP); }

    public static void setEdge(ItemStack tool, double edge) { tool.set(EDGE.get(), Math.max(0, Math.min(ToolWear.EDGE_MAX, edge))); }

    public static boolean dull(ItemStack tool) {
        WearData w = WearData.of(tool);
        return w != null && w.hasEdge() && ToolWear.dull(edge(tool));
    }

    /** The job-time multiplier of this tool's edge: 1.5 when dull, else 1. */
    public static double timeMult(ItemStack tool) {
        WearData w = WearData.of(tool);
        return w != null && w.hasEdge() ? ToolWear.timeMult(edge(tool)) : 1.0;
    }

    public static double kcalMult(ItemStack tool) {
        WearData w = WearData.of(tool);
        return w != null && w.hasEdge() ? ToolWear.kcalMult(edge(tool)) : 1.0;
    }

    /** Game deciseconds of work {@code ds} as the tool's edge slows it: x1.5 time means two thirds of the work a tick. */
    public static long scaleDs(ItemStack tool, long ds) {
        double m = timeMult(tool);
        return m == 1.0 ? ds : Math.max(1, Math.round(ds / m));
    }

    /** Restores the edge to at least {@code to} (a hone). */
    public static void hone(ItemStack tool, double to) { setEdge(tool, ToolWear.hone(edge(tool), to)); }

    /**
     * Charges {@code uses} uses to {@code tool}, a stack held by {@code p}. Returns true if the tool broke (the stack
     * is then empty).
     */
    public static boolean use(ServerPlayer p, ItemStack tool, int uses) {
        if (tool.isEmpty() || uses <= 0) return false;
        WearData w = WearData.of(tool);
        double air = airC(p);
        boolean woodOrStone = w != null && w.woodOrStone();
        if (w != null && w.hasEdge()) {
            double e = ToolWear.afterUses(edge(tool), w.edgeUses(), uses);
            if (w.material().equals("iron") && air < ToolWear.COLD_CHIP_C) {
                for (int i = 0; i < uses; i++) {
                    if (p.getRandom().nextDouble() < ToolWear.COLD_CHIP_CHANCE) e = Math.max(0, e - ToolWear.COLD_CHIP_EDGE);
                }
            }
            setEdge(tool, e);
        }
        if (!tool.isDamageableItem()) return false;
        int max = tool.getMaxDamage();
        boolean wasWorn = ToolWear.worn(tool.getDamageValue(), max);
        int cost = uses * ToolWear.durabilityCost(air, woodOrStone);
        String name = BuiltInRegistries.ITEM.getKey(tool.getItem()).toString();
        boolean[] broke = {false};
        tool.hurtAndBreak(cost, p.serverLevel(), p, item -> broke[0] = true);
        if (broke[0] || tool.isEmpty()) {
            say(p, "tool_broke", HudArg.item(name));
            return true;
        }
        if (!wasWorn && ToolWear.worn(tool.getDamageValue(), max)) say(p, "tool_worn", HudArg.item(name));
        return false;
    }

    static double airC(ServerPlayer p) {
        return EnvService.installed() ? EnvService.get().airC(p.serverLevel(), p.blockPosition()) : 0.0;
    }

    static void say(ServerPlayer p, String id, HudArg... args) {
        if (HudBus.installed()) HudBus.get().post(p, HudLine.of(HudLineType.WARN, id, "building", args));
    }
}
