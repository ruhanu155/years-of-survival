package com.yearofice.ecosystem.bird;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi flock}: {@code <species> [count]} places a flock (or a single bird) 4 m in front of the source,
 * {@code status} prints the birds and flocks within 128 m and the totals scavenged, fished and stolen, and
 * {@code clear} removes the birds near the source.
 */
public final class BirdCommands {
    private BirdCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("flock")
                .then(Commands.literal("status").executes(BirdCommands::status))
                .then(Commands.literal("clear").executes(BirdCommands::clear))
                .then(Commands.argument("species", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(BirdRegistry.IDS, b))
                        .executes(c -> spawn(c, 0))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 40)).executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "count")))))));
    }

    static int spawn(CommandContext<CommandSourceStack> c, int n) {
        String sp = StringArgumentType.getString(c, "species");
        if (!BirdRegistry.IDS.contains(sp)) {
            c.getSource().sendFailure(Component.literal("Unknown bird. Try " + BirdRegistry.IDS));
            return 0;
        }
        ServerLevel level = c.getSource().getLevel();
        var look = c.getSource().getRotation();
        double yaw = Math.toRadians(look.y);
        BlockPos at = BlockPos.containing(c.getSource().getPosition().add(-Math.sin(yaw) * 4, 0, Math.cos(yaw) * 4));
        var out = BirdService.form(level, sp, at, n);
        c.getSource().sendSuccess(() -> Component.literal("Placed " + out.size() + " " + sp + (out.size() > 0 && out.get(0).flockId() != 0 ? " in flock " + out.get(0).flockId() : "")), false);
        return out.size();
    }

    static int status(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        int[] n = BirdService.census(level, BlockPos.containing(c.getSource().getPosition()), 128);
        c.getSource().sendSuccess(() -> Component.literal("Singles " + n[0] + ", flocks " + n[1] + "; scavenged " + BirdService.scavengedG
                + " g, fish taken " + BirdService.fishTaken + ", store thefts " + BirdService.thefts), false);
        return 1;
    }

    static int clear(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        int k = 0;
        for (BirdEntity b : level.getEntitiesOfClass(BirdEntity.class, new net.minecraft.world.phys.AABB(BlockPos.containing(c.getSource().getPosition())).inflate(128, 128, 128))) {
            b.discard();
            k++;
        }
        int removed = k;
        c.getSource().sendSuccess(() -> Component.literal("Removed " + removed + " birds."), false);
        return removed;
    }
}
