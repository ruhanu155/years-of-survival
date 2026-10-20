package com.yearofice.data.migrate;

import com.mojang.brigadier.CommandDispatcher;
import com.yearofice.core.YearOfIce;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * Wires {@code data.migrate} (Saves across versions): the id aliases into the registries, the version gate and
 * pre-upgrade backup when a world opens, the retired-component conversion on player login, and
 * {@code /yoi migrate status}. Called once from the mod constructor.
 */
public final class MigrateSystem {
    private MigrateSystem() {}

    private static volatile WorldGate.Outcome last;

    public static WorldGate.Outcome last() { return last; }

    public static SaveVersion jarVersion() {
        return ModList.get().getModContainerById(YearOfIce.MOD_ID)
                .map(c -> SaveVersion.parse(c.getModInfo().getVersion().toString())).orElse(SaveVersion.ZERO);
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(Aliases::onRegister);
        NeoForge.EVENT_BUS.addListener(MigrateSystem::onAboutToStart);
        NeoForge.EVENT_BUS.addListener(MigrateSystem::onLogin);
        NeoForge.EVENT_BUS.addListener(MigrateSystem::registerCommands);
    }

    /** Throws {@link WorldGate.NewerWorldException} for a world a newer jar saved, which stops the load. */
    static void onAboutToStart(ServerAboutToStartEvent e) {
        Path world = e.getServer().getWorldPath(LevelResource.ROOT);
        try {
            WorldGate.Outcome o = WorldGate.open(world, jarVersion());
            last = o;
            if (o.kind() == WorldGate.Kind.UPGRADE) YearOfIce.LOG.info("[migrate] {} Backup: {}", o.message(), o.backup());
            else YearOfIce.LOG.info("[migrate] {}", o.message());
        } catch (IOException ex) {
            throw new UncheckedIOException("Year of Ice could not back up this world before upgrading it", ex);
        }
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) {
            int n = LegacyConverter.convertPlayer(p);
            if (n > 0) YearOfIce.LOG.info("[migrate] {} stack(s) of {} rewritten from retired ids or components", n, p.getGameProfile().getName());
        }
    }

    static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("migrate").then(Commands.literal("status").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    Path world = server.getWorldPath(LevelResource.ROOT);
                    String saved, backups;
                    try {
                        SaveVersion v = WorldGate.read(world);
                        saved = v == null ? "none" : v.toString();
                        backups = String.join(", ", Backup.list(world));
                    } catch (IOException ex) {
                        saved = "unreadable";
                        backups = "?";
                    }
                    WorldGate.Outcome o = last;
                    String text = "Jar " + jarVersion() + " | world stamp " + saved + " | opened as "
                            + (o == null ? "?" : o.kind().name().toLowerCase(java.util.Locale.ROOT))
                            + " | aliases " + Aliases.shared().count() + " | migrations " + Migrations.shared().count()
                            + " | legacy rules " + LegacyConverter.rules().size()
                            + " | backups " + (backups.isEmpty() ? "none" : backups);
                    ctx.getSource().sendSuccess(() -> Component.literal(text), false);
                    return 1;
                }))));
    }
}
