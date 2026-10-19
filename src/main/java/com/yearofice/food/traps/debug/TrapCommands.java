package com.yearofice.food.traps.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.food.traps.SnareBlockEntity;
import com.yearofice.food.traps.TrapConfig;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.ecosystem.Snare;
import com.yearofice.sim.ecosystem.TrapTuning;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi snare} (and {@code /yoi trap}, the Ready-for-development spelling) on the nearest snare within 8
 * blocks.
 *
 * <ul>
 *   <li>{@code info}: status, set (trail, cover, funnel, bait), multiplier, habitat share, hares in range, P per
 *       roll, the last and next roll, catches and the held catch.</li>
 *   <li>{@code roll}: moves the world's time on to the next dawn or dusk and settles the snare there, then says
 *       what the roll did. Rolls only ever happen at dawn and dusk.</li>
 * </ul>
 */
public final class TrapCommands {
    private TrapCommands() {}

    private static final SimpleCommandExceptionType NONE = new SimpleCommandExceptionType(Component.literal("No snare within 8 blocks."));

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        for (String root : new String[] {"snare", "trap"}) {
            LiteralArgumentBuilder<CommandSourceStack> n = Commands.literal(root)
                    .then(Commands.literal("info").executes(TrapCommands::info))
                    .then(Commands.literal("roll").executes(TrapCommands::roll));
            d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(n));
        }
    }

    static SnareBlockEntity nearest(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos at = BlockPos.containing(c.getSource().getPosition());
        SnareBlockEntity best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-8, -4, -8), at.offset(8, 4, 8))) {
            if (level.getBlockEntity(p) instanceof SnareBlockEntity be && be.state() != null) {
                double dd = p.distSqr(at);
                if (dd < bd) {
                    bd = dd;
                    best = be;
                }
            }
        }
        if (best == null) throw NONE.create();
        return best;
    }

    static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        SnareBlockEntity be = nearest(c);
        be.settle(true);
        c.getSource().sendSuccess(() -> Component.literal(describe(be)), false);
        return 1;
    }

    static int roll(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        SnareBlockEntity be = nearest(c);
        ServerLevel level = c.getSource().getLevel();
        Snare.Rolls rolls = be.env().rolls();
        long now = be.now();
        long next = rolls.atOrBefore(now) + 1;
        long at = rolls.minute(next);
        double p = Snare.pNow(be.state(), be.env(), now);
        long minutes = (long) Math.ceil(at - SliceClock.minuteD(level)) + 1;
        com.yearofice.api.clock.ClockService.get().advance(Math.max(0, minutes));
        int before = be.state().catches();
        be.settle(true);
        boolean caught = be.state().catches() > before;
        String line = String.format("%s roll on day %d: P %.3f, %s.", Snare.Rolls.isDawn(next) ? "Dawn" : "Dusk",
                Calendar.absDayOfMinute(at), p, caught ? "a hare, caught at " + clock(be.state().held().minute()) : "nothing");
        c.getSource().sendSuccess(() -> Component.literal(line), true);
        return caught ? 1 : 0;
    }

    public static String describe(SnareBlockEntity be) {
        TrapTuning t = TrapConfig.tuning();
        Snare.State s = be.state();
        Snare.SetQuality q = be.quality();
        long now = be.now();
        Snare.Rolls rolls = be.env().rolls();
        long next = rolls.atOrBefore(now) + 1;
        String held = s.held() == null ? "none" : "hare caught " + clock(s.held().minute()) + (s.visible(now) == null ? " (not yet)" : "");
        return String.format("Snare: %s | trail %s, cover %s, funnel %s, bait %s: x%.2f | hare habitat %.0f%% | hares in range %.2f | P per roll %.3f%n"
                        + "Last roll %d, next %s %s | catches %d of %d | held: %s",
                s.status(), q.onTrail(), q.besideCover(), q.funnel(), q.baited(), q.multiplier(t), be.habitatShare() * 100,
                Snare.effectiveHares(s, be.env(), next), Snare.pNow(s, be.env(), now), s.lastRoll(),
                Snare.Rolls.isDawn(next) ? "dawn" : "dusk", clock(rolls.minute(next)),
                s.catches(), t.wireCatches(), held);
    }

    static String clock(long minute) {
        int m = Calendar.minuteOfDayOfMinute(minute);
        return String.format("day %d %02d:%02d", Calendar.absDayOfMinute(minute), m / 60, m % 60);
    }
}
