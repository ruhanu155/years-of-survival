package com.yearofice.build.metal;

import com.mojang.brigadier.context.CommandContext;
import com.yearofice.build.craft.StationCraft;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkService;
import com.yearofice.core.YearOfIce;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * M9 lane B's iron tier wiring: the furnace and forge as stations (a bloomery or forge within reach), the work-job
 * resolver for smelting, consolidating and sluicing, and {@code /yoi metal}. Owns {@code build.metal}.
 */
public final class MetalSystem {
    private MetalSystem() {}

    public static final ResourceLocation FURNACE = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "furnace");
    public static final ResourceLocation FORGE = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "forge");
    public static final ResourceLocation CANOE_BED = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "canoe_bed");
    public static final TagKey<Block> FURNACE_WALLS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "furnace_walls"));

    public static void init(IEventBus modBus) {
        MetalRegistry.register(modBus);
        com.yearofice.api.build.WheelService.install(new com.yearofice.build.craft.water.WheelWorks());
        StationCraft.STATION_PROVIDERS.add(MetalSystem::stations);
        var prev = WorkService.resolver;
        WorkService.resolver = (p, w) -> {
            WorkJob j = resolve(p, w.pos());
            return j != null ? j : prev.apply(p, w);
        };
        NeoForge.EVENT_BUS.addListener(MetalSystem::commands);
    }

    /** The furnace station is a built bloomery within reach; the forge a forge block (which does the furnace's work too). */
    static void stations(ServerPlayer p, java.util.Set<ResourceLocation> out) {
        ServerLevel l = p.serverLevel();
        int r = (int) Math.ceil(MetalConfig.get("work_reach_m"));
        BlockPos at = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-r, -r, -r), at.offset(r, r, r))) {
            var s = l.getBlockState(q);
            if (s.is(MetalRegistry.BLOOMERY.get()) && BloomeryBlock.built(l, q)) out.add(FURNACE);
            else if (s.is(MetalRegistry.FORGE.get())) {
                out.add(FORGE);
                out.add(FURNACE);
            } else if (s.is(MetalRegistry.CANOE_BED.get())) out.add(CANOE_BED);
        }
    }

    /** The job a hold on {@code pos} starts, or null. */
    public static WorkJob resolve(ServerPlayer p, BlockPos pos) {
        ServerLevel l = p.serverLevel();
        var s = l.getBlockState(pos);
        if (s.is(MetalRegistry.SLUICE_BOX.get())) return SluiceJob.resolve(p, pos);
        boolean bloom = p.getMainHandItem().is(MetalRegistry.IRON_BLOOM.get()) && ForgeJob.charcoalIn(p) >= com.yearofice.sim.build.Smelt.CHARCOAL_PER_CONSOLIDATION;
        if (s.is(MetalRegistry.FORGE.get())) return bloom ? new ForgeJob(pos, true) : null;
        if (s.is(MetalRegistry.BLOOMERY.get()) && BloomeryBlock.built(l, pos)) {
            if (bloom) return new ForgeJob(pos, false);
            if (l.getBlockEntity(pos) instanceof BloomeryBlockEntity be && be.canSmelt()) return new SmeltJob(pos);
        }
        return null;
    }

    private static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("metal").then(Commands.literal("info").executes(MetalSystem::info))));
    }

    /** {@code /yoi metal info}: the bloomery within 6 blocks, its charge and what a smelt would make. */
    private static int info(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        BlockPos at = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-6, -3, -6), at.offset(6, 3, 6))) {
            if (l.getBlockEntity(q) instanceof BloomeryBlockEntity be) {
                String line = "bloomery " + q.toShortString() + ": " + be.quarters() / 40.0 + " nodules of ore, " + be.charcoal() + " kg charcoal, "
                        + be.blooms() + " bloom(s) waiting, built=" + BloomeryBlock.built(l, q);
                c.getSource().sendSuccess(() -> Component.literal(line), false);
                return 1;
            }
        }
        c.getSource().sendSuccess(() -> Component.literal("no bloomery within 6 blocks"), false);
        return 0;
    }
}
