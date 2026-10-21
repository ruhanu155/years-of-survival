package com.yearofice.world.regions;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.yearofice.core.YearOfIce;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import com.yearofice.worldgen.regions.Regions;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** The dev border as data ({@code region/dev_border.json}) and {@code /yoi border <ring>}, which lifts it for test worlds. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class DevBorder {
    private DevBorder() {}

    /** Sets the overworld border to the width of {@code ring} (1 = the home valley's edge, 3 = the vanilla limit). */
    public static double apply(MinecraftServer server, int ring) {
        var b = server.overworld().getWorldBorder();
        b.setCenter(0, 0);
        b.setSize(Regions.borderSize(ring));
        return b.getSize();
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("border")
                .then(Commands.argument("ring", IntegerArgumentType.integer(1, 3)).executes(c -> {
                    int ring = IntegerArgumentType.getInteger(c, "ring");
                    double size = apply(c.getSource().getServer(), ring);
                    c.getSource().sendSuccess(() -> Component.literal("Border set to ring " + ring + ": " + (long) size + " blocks."), true);
                    return ring;
                }))));
    }
}
