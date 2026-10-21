package com.yearofice.story.radio;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.yearofice.api.clock.ClockService;
import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.regions.Regions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi radio sked} reads today's sked to you; {@code /yoi radio drop [region]} sends a supply drop to a region's zone, or to you. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class RadioCommands {
    private RadioCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        var drop = Commands.literal("drop").executes(c -> drop(c.getSource(), null))
                .then(Commands.argument("region", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Regions.IDS, b))
                        .executes(c -> drop(c.getSource(), StringArgumentType.getString(c, "region"))));
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("radio")
                .then(Commands.literal("sked").executes(c -> sked(c.getSource()))).then(drop)));
    }

    private static int sked(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        long minute = ClockService.installed() ? ClockService.get().nowMinute() : 0;
        var lines = Radio.skedLines(src.getLevel(), p.blockPosition(), minute);
        lines.forEach(l -> src.sendSuccess(() -> Component.literal(l), false));
        return lines.size();
    }

    private static int drop(CommandSourceStack src, String region) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos zone;
        if (region == null) zone = src.getPlayerOrException().blockPosition();
        else {
            var r = Regions.byId(region);
            if (r == null) { src.sendFailure(Component.literal("Unknown region " + region)); return 0; }
            zone = new BlockPos(r.dropZone()[0], 0, r.dropZone()[1]);
        }
        SupplyDrop.fly(src.getLevel(), zone);
        src.sendSuccess(() -> Component.literal("Supply drop released over " + zone.getX() + ", " + zone.getZ() + "."), true);
        return 1;
    }
}
