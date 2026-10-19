package com.yearofice.core.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.yearofice.core.hash.StageHash;
import com.yearofice.core.rules.YoiConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Root of the /yoi debug tree. Each system adds its own subcommands
 * (/yoi set calories, /yoi time day, /yoi spawn ...) as it lands. Operator level 2.
 */
public final class YoiCommands {
    private YoiCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi")
                .requires(s -> s.hasPermission(2))
                .then(com.yearofice.worldgen.debug.WorldgenCommands.node())
                .then(Commands.literal("info").executes(ctx -> {
                    String version = ModList.get().getModContainerById("yearofice")
                            .map(c -> c.getModInfo().getVersion().toString()).orElse("?");
                    long seed = ctx.getSource().getLevel().getSeed();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Year of Ice " + version + " | preset " + YoiConfig.PRESET.get()
                                    + " | seed " + seed + " | landform seed "
                                    + Integer.toUnsignedString(StageHash.stageSeed(seed, "landform"))), false);
                    return 1;
                })));
    }
}
