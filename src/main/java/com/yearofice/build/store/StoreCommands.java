package com.yearofice.build.store;

import com.yearofice.api.build.StoreService;
import com.yearofice.build.cellar.CellarBlockEntity;
import com.yearofice.build.cellar.CellarIndex;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi store info}: the temperature, spoil rate and dryness at the player's feet, and the cellar room there if any. */
public final class StoreCommands {
    private StoreCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("store").then(Commands.literal("info").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BlockPos pos = p.blockPosition();
                    StoreService s = StoreService.INSTANCE;
                    CellarBlockEntity cellar = CellarIndex.at(p.level(), pos);
                    String line = String.format(Locale.ROOT, "store at %s: %.1f C, spoil rate x%.2f, dry %.0f%%%s", pos.toShortString(),
                            s.tempC(p.level(), pos), s.rate(p.level(), pos), 100 * s.dryness(p.level(), pos),
                            cellar == null ? "" : " | in " + (cellar.permafrost() ? "permafrost" : "root") + " cellar, " + cellar.room().cells().size() + " cells");
                    c.getSource().sendSuccess(() -> Component.literal(line), false);
                    return 1;
                }))));
    }
}
