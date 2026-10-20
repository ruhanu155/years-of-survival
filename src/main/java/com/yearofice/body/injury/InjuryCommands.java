package com.yearofice.body.injury;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyTicker;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.injury.BackStrain;
import com.yearofice.sim.body.part.BodyRecord;
import com.yearofice.sim.body.part.Digits;
import com.yearofice.sim.body.part.Part;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi body injure|treat|heal|digits|roll} (op level 2). They join the {@code /yoi body} tree the body
 * package registers; Brigadier merges the two.
 *
 * <pre>
 *   /yoi body injure &lt;source&gt; [part]      /yoi body treat &lt;action&gt; [part]      /yoi body heal
 *   /yoi body digits &lt;part&gt;               /yoi body roll stumble|strain [count]
 * </pre>
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class InjuryCommands {
    private InjuryCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("body")
                        .then(Commands.literal("injure").then(Commands.argument("source", StringArgumentType.word())
                                .executes(c -> injure(c, null))
                                .then(Commands.argument("part", StringArgumentType.word())
                                        .executes(c -> injure(c, StringArgumentType.getString(c, "part"))))))
                        .then(Commands.literal("treat").then(Commands.argument("action", StringArgumentType.word())
                                .executes(c -> treat(c, null))
                                .then(Commands.argument("part", StringArgumentType.word())
                                        .executes(c -> treat(c, StringArgumentType.getString(c, "part"))))))
                        .then(Commands.literal("heal").executes(InjuryCommands::heal))
                        .then(Commands.literal("digits").then(Commands.argument("part", StringArgumentType.word())
                                .executes(InjuryCommands::digits)))
                        .then(Commands.literal("roll")
                                .then(Commands.literal("stumble").executes(c -> rollStumble(c)))
                                .then(Commands.literal("strain").executes(c -> rollStrain(c))))));
    }

    private static Part part(String id) { return id == null ? null : Part.byId(id.toLowerCase(Locale.ROOT)); }

    private static int injure(CommandContext<CommandSourceStack> c, String part) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String src = StringArgumentType.getString(c, "source");
        boolean ok = InjuryService.injure(p, src, part(part), Double.NaN, "debug");
        c.getSource().sendSuccess(() -> Component.literal(ok ? "Injured by " + src + ": Condition "
                + String.format(Locale.ROOT, "%.2f", BodyAttachments.of(p).condition()) : "No source " + src), true);
        return ok ? 1 : 0;
    }

    private static int treat(CommandContext<CommandSourceStack> c, String part) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String a = StringArgumentType.getString(c, "action");
        boolean ok = InjuryService.treat(p, a, part(part));
        c.getSource().sendSuccess(() -> Component.literal(ok ? "Treated: " + a : "Nothing to treat with " + a), true);
        return ok ? 1 : 0;
    }

    /** Every part and Condition back to full and every problem cleared; lost digits and crooked bones stay. */
    static int heal(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        BodyState s = BodyAttachments.of(p);
        BodyRecord old = s.parts;
        BodyRecord h = BodyRecord.healthy();
        s.parts = new BodyRecord(BodyRecord.SCHEMA, 100, 0, h.partHp(), h.partFrost(), old.lostDigits(), old.crooked(),
                old.injurySeq(), java.util.List.of());
        BodyTicker.sync(p, BodyTicker.clock.minute(p.server), true);
        c.getSource().sendSuccess(() -> Component.literal("Healed: Condition 100, no problems"), true);
        return 1;
    }

    private static int digits(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Part part = part(StringArgumentType.getString(c, "part"));
        BodyState s = BodyAttachments.of(p);
        s.parts = s.parts.withDigits(Digits.loseNext(s.parts.lostDigits(), part));
        int[] d = s.parts.lostDigits();
        c.getSource().sendSuccess(() -> Component.literal("Lost digits: fingers " + Digits.count(d[Digits.FINGERS])
                + ", toes " + Digits.count(d[Digits.TOES]) + "; crafting x"
                + String.format(Locale.ROOT, "%.2f", Digits.craftingFactor(d))), true);
        return 1;
    }

    private static int rollStumble(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        var o = InjuryService.stumble(p, p.getRandom().nextDouble(), p.getRandom().nextDouble());
        c.getSource().sendSuccess(() -> Component.literal("Stumble roll: " + o), true);
        return 1;
    }

    private static int rollStrain(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        BodyState s = BodyAttachments.of(p);
        var lift = new BackStrain.Lift(40, 40, false, s.energy, s.warmth, false);
        boolean hit = InjuryService.heavyLift(p, lift, p.getRandom().nextDouble(), false);
        c.getSource().sendSuccess(() -> Component.literal("40 kg lift at the cap: chance "
                + String.format(Locale.ROOT, "%.3f", InjuryService.strainChance(lift)) + (hit ? ", strained" : ", fine")), true);
        return 1;
    }
}
