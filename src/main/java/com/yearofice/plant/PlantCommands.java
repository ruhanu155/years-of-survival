package com.yearofice.plant;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.api.clock.ClockService;
import com.yearofice.sim.plant.PatchModel;
import com.yearofice.sim.plant.PlantNotes;
import com.yearofice.sim.plant.PlantPhase;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi plant} (Plants and fungi › Debug commands), op level 2, on the plant under the crosshair:
 * {@code info}, {@code phase <phase> [patch]}, {@code patch reset|strip|health <h>}, {@code identify <species|all|none> [player]},
 * {@code year <berry_index|cone_crop|warmth> <value>}.
 */
public final class PlantCommands {
    private PlantCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("plant")
                .then(Commands.literal("info").executes(PlantCommands::info))
                .then(Commands.literal("phase").then(Commands.argument("phase", StringArgumentType.word())
                        .executes(c -> phase(c, false))
                        .then(Commands.literal("patch").executes(c -> phase(c, true)))))
                .then(Commands.literal("patch")
                        .then(Commands.literal("reset").executes(c -> patch(c, "reset", 0)))
                        .then(Commands.literal("strip").executes(c -> patch(c, "strip", 0)))
                        .then(Commands.literal("health").then(Commands.argument("h", DoubleArgumentType.doubleArg(0.2, 1.3))
                                .executes(c -> patch(c, "health", DoubleArgumentType.getDouble(c, "h"))))))
                .then(Commands.literal("identify").then(Commands.argument("species", StringArgumentType.word())
                        .executes(c -> identify(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> identify(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("year").then(Commands.argument("what", StringArgumentType.word())
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 3)).executes(PlantCommands::year))))));
    }

    static BlockPos target(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        HitResult h = p.pick(6.0, 0f, false);
        if (h instanceof BlockHitResult b && h.getType() == HitResult.Type.BLOCK) return b.getBlockPos();
        throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Look at a plant block.")).create();
    }

    private static PlantBlock plantAt(ServerLevel level, BlockPos pos) throws CommandSyntaxException {
        if (level.getBlockState(pos).getBlock() instanceof PlantBlock pb) return pb;
        throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("That is not a plant block.")).create();
    }

    static int say(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = target(c);
        PlantBlock pb = plantAt(level, pos);
        PlantSpec spec = PlantData.get(pb.species());
        BlockState st = level.getBlockState(pos);
        StringBuilder sb = new StringBuilder(pb.species()).append(" phase ").append(st.getValue(PlantBlock.PHASE).getSerializedName())
                .append(" taken ").append(st.getValue(PlantBlock.TAKEN));
        int doy = ClockService.get().dayOfYear();
        sb.append(" | day ").append(doy).append(" dd ").append(Math.round(com.yearofice.sim.plant.PhaseModel.ddAt(doy, PlantDials.warmth)));
        if (spec != null) {
            sb.append(" | form ").append(spec.form).append(spec.toxic != null ? " | toxic " + spec.toxic.grade() : "");
            PatchData d = PatchLedger.data(level.getChunkAt(pos));
            PatchData.Patch p = d.patches.get(spec.id);
            if (p != null) sb.append(" | patch S ").append(String.format(Locale.ROOT, "%.2f", p.s)).append(" H ").append(String.format(Locale.ROOT, "%.2f", p.h))
                    .append(" p ").append(String.format(Locale.ROOT, "%.2f", PatchLedger.pickedShare(p))).append(" last day ").append(p.lastDay);
            Integer ready = d.ready.get(pos.asLong());
            if (ready != null) sb.append(" | regrows day ").append(ready);
        }
        return say(c, sb.toString());
    }

    private static int phase(CommandContext<CommandSourceStack> c, boolean patch) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = target(c);
        PlantBlock pb = plantAt(level, pos);
        PlantBlock.Phase ph;
        try {
            ph = PlantBlock.Phase.of(PlantPhase.of(StringArgumentType.getString(c, "phase")));
        } catch (IllegalArgumentException ex) {
            return say(c, "Phases: dormant buried bud leaf flower fruit seed senescent.");
        }
        int n = 0;
        int r = patch ? 8 : 0;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
            BlockState st = level.getBlockState(q);
            if (st.getBlock() == pb) {
                level.setBlock(q, st.setValue(PlantBlock.PHASE, ph), 3);
                n++;
            }
        }
        return say(c, "Set " + n + " clump(s) to " + ph.getSerializedName() + ".");
    }

    private static int patch(CommandContext<CommandSourceStack> c, String what, double h) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = target(c);
        PlantSpec spec = PlantData.get(plantAt(level, pos).species());
        if (spec == null) return say(c, "No data.");
        PatchData.Patch p = PatchLedger.patch(level, pos, spec);
        switch (what) {
            case "reset" -> { p.h = 1.0; p.picked = 0; p.lastDay = -1; PatchLedger.data(level.getChunkAt(pos)).ready.clear(); }
            case "strip" -> p.picked = p.cap;
            default -> p.h = PatchModel.clampH(h);
        }
        level.getChunkAt(pos).setUnsaved(true);
        return say(c, spec.id + " patch: H " + String.format(Locale.ROOT, "%.2f", p.h) + ", picked " + Math.round(PatchLedger.pickedShare(p) * 100) + "%.");
    }

    private static int identify(CommandContext<CommandSourceStack> c, ServerPlayer who) {
        String s = StringArgumentType.getString(c, "species");
        if (s.equals("none")) {
            for (String k : PlantCatalog.SPECIES) Knowledge.forget(who, "yearofice:" + k);
            return say(c, "Notebook cleared for " + who.getGameProfile().getName() + ".");
        }
        if (s.equals("all")) {
            for (String k : PlantCatalog.SPECIES) Knowledge.identify(who, "yearofice:" + k);
            return say(c, "Every plant identified for " + who.getGameProfile().getName() + ".");
        }
        String id = s.contains(":") ? s : "yearofice:" + s;
        if (PlantData.get(id) == null) return say(c, "No such plant: " + id);
        Knowledge.identify(who, id);
        return say(c, id + " identified (" + PlantNotes.Level.IDENTIFIED + ").");
    }

    private static int year(CommandContext<CommandSourceStack> c) {
        String what = StringArgumentType.getString(c, "what");
        double v = DoubleArgumentType.getDouble(c, "value");
        switch (what) {
            case "berry_index" -> PlantDials.berryIndex = v;
            case "cone_crop" -> PlantDials.coneCrop = v;
            case "warmth" -> PlantDials.warmth = v;
            default -> { return say(c, "Use berry_index, cone_crop or warmth."); }
        }
        return say(c, what + " = " + v);
    }
}
