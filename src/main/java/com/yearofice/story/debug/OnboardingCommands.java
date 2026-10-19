package com.yearofice.story.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.sim.story.FirstNight;
import com.yearofice.sim.story.FirstNightStep;
import com.yearofice.sim.story.StoryDay;
import com.yearofice.story.Onboarding;
import com.yearofice.story.OnboardingRecord;
import com.yearofice.story.StoryClock;
import com.yearofice.story.StoryConfig;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi onboarding}: the guided first night's debug commands, on yourself or a named player.
 *
 * <ul>
 *   <li>{@code status}: status, step, the minutes it began and the first fire caught, the ring, the beacon target
 *       and the dusk warning's state.</li>
 *   <li>{@code skip}: guidance off for good (what a returning player gets).</li>
 *   <li>{@code reset}: start the night again from step 1 now.</li>
 * </ul>
 * The {@code yoi} root is registered by core; Brigadier merges this subtree into it.
 */
public final class OnboardingCommands {
    private OnboardingCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("onboarding")
                .then(Commands.literal("status").executes(c -> status(c, self(c)))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> status(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("skip").executes(c -> skip(c, self(c)))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> skip(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("reset").executes(c -> reset(c, self(c)))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> reset(c, EntityArgument.getPlayer(c, "player")))))));
    }

    private static ServerPlayer self(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return c.getSource().getPlayerOrException();
    }

    private static int status(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        OnboardingRecord r = Onboarding.of(p);
        FirstNight n = r.night();
        FirstNightStep s = n.step();
        BlockPos target = Onboarding.target(p);
        String line = "Onboarding " + p.getGameProfile().getName() + ": " + n.status().name().toLowerCase(Locale.ROOT)
                + (n.active() ? ", step " + s.number() + "/" + FirstNightStep.COUNT + " " + s.id : "")
                + (n.startMinute() >= 0 ? " | began day " + StoryDay.day(n.startMinute()) + " " + StoryDay.hhmm(n.startMinute()) : "")
                + (n.fireLitMinute() >= 0 ? " | fire at " + StoryDay.hhmm(n.fireLitMinute()) : " | no fire yet")
                + (n.endMinute() >= 0 ? " | ended " + StoryDay.hhmm(n.endMinute()) + " (" + n.ending().name().toLowerCase(Locale.ROOT) + ")" : "")
                + " | ring " + (r.ring() == null ? "-" : r.ring().toShortString())
                + " | beacon " + (StoryConfig.beacon() ? (target == null ? "no target" : target.toShortString()) : "off")
                + " | dusk warning " + (r.warn().fired(Onboarding.DUSK_NO_FIRE) ? "given" : "armed")
                + " | now " + StoryDay.hhmm(StoryClock.minute(p.server));
        c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int skip(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        Onboarding.skip(p, StoryClock.minute(p.server));
        return status(c, p);
    }

    private static int reset(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        Onboarding.reset(p, StoryClock.minute(p.server));
        return status(c, p);
    }
}
