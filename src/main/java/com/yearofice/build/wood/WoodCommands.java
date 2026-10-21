package com.yearofice.build.wood;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.sim.fire.Seasoning;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.wood.Felling;
import com.yearofice.sim.wood.FellingGame;
import com.yearofice.sim.wood.ToolStats;
import com.yearofice.sim.wood.TreeKind;
import com.yearofice.sim.wood.TreeMarks;
import com.yearofice.sim.wood.TrunkSearch;
import com.yearofice.world.logs.FelledLogEntity;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi tree fell [bearing]|info} (Ready for development › M2; Trees, felling and firewood › Build) and
 * {@code /yoi wood info|dry <days>}: fell the looked-at tree at once along a bearing (default: away from you, toward
 * where you are not), print its record, diameter, lean and fall line, and read or step the held wood's moisture.
 */
public final class WoodCommands {
    private WoodCommands() {}

    static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("tree")
                        .then(Commands.literal("info").executes(WoodCommands::info))
                        .then(Commands.literal("fell").executes(c -> fell(c, Double.NaN))
                                .then(Commands.argument("bearing", DoubleArgumentType.doubleArg(0, 360))
                                        .executes(c -> fell(c, DoubleArgumentType.getDouble(c, "bearing"))))))
                .then(Commands.literal("wood")
                        .then(Commands.literal("info").executes(WoodCommands::woodInfo))
                        .then(Commands.literal("dry").then(Commands.argument("days", IntegerArgumentType.integer(1, 730))
                                .executes(c -> dry(c, IntegerArgumentType.getInteger(c, "days")))))));
    }

    static BlockPos looked(ServerPlayer p) {
        HitResult h = p.pick(8, 1f, false);
        return h instanceof BlockHitResult b && h.getType() == HitResult.Type.BLOCK ? b.getBlockPos() : null;
    }

    static int fail(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendFailure(Component.literal(msg));
        return 0;
    }

    static int info(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        BlockPos at = looked(p);
        if (at == null) return fail(c, "Look at a tree trunk.");
        TrunkSearch.Trunk t = FellingService.trunk(l, at);
        StringBuilder sb = new StringBuilder();
        if (t == null) {
            sb.append(WoodBlocks.isStop(l, at, l.getBlockState(at)) ? "Placed log: cut one block out (" : "Not a rooted tree: cut one block out (")
              .append(fmt(minutesFor(p, true))).append(" min).");
        } else {
            TreeKind k = FellingService.kind(t);
            BlockPos base = FellingService.base(t);
            double d = FellingService.diameterCm(t);
            TreeLedger.Planned plan = TreeLedger.plannedAt(l, base);
            int hash = plan != null ? plan.tree().seed() : FellingService.seedOf(base);
            boolean drunken = plan != null && plan.tree().lean() >= 0;
            sb.append(k.display).append(", green. Base ").append(base.toShortString()).append(", ").append(t.height()).append(" m, ")
              .append(Math.round(d)).append(" cm. Leans ").append(Math.round(Felling.leanDeg(hash, drunken))).append("° ")
              .append(Felling.compass(Felling.leanBearing(hash))).append(".");
            sb.append(plan != null ? " Plan: " + plan.tree().kind() + " " + plan.tree().height() + " m" : " Not in the plan (a test or placed tree)");
            long mark = TreeLedger.markAt(l, base);
            if (mark != 0) sb.append(", mark ").append(TreeMarks.state(mark)).append(" day ").append(TreeMarks.day(mark));
            ToolStats tool = ToolData.of(p.getMainHandItem());
            if (tool != null) {
                int n = Felling.swings(tool.swings25(), d);
                sb.append(Felling.canFell(tool, d) ? String.format(Locale.ROOT, ". Fell: %d swings, %s min, %d kcal", n,
                        fmt(n * tool.swingMin()), Math.round(Felling.kcal(n * tool.swingMin()))) : ". Too thick for this tool");
            }
            FellingGame g = FellingService.gameIfAny(l, base);
            double line = g != null && g.arrowLocked() ? g.fallBearing()
                    : Felling.bearing(p.getX() - (base.getX() + 0.5), p.getZ() - (base.getZ() + 0.5));
            sb.append(". Fall line ").append(Felling.compass(line)).append(" (").append(Math.round(line)).append("°)");
            if (g != null) sb.append(String.format(Locale.ROOT, ", notch %.1f/%d, back cut %.1f/%d", g.notch(), g.notchNeeded(), g.back(), g.backNeeded()));
            if (t.stoppedAt() != null) sb.append(". Ends at a placed block");
            sb.append(".");
        }
        String msg = sb.toString();
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    static double minutesFor(ServerPlayer p, boolean placed) {
        ToolStats t = ToolData.of(p.getMainHandItem());
        return t == null ? ToolStats.CRATE_HATCHET.buckMin() : t.buckMin();
    }

    static int fell(CommandContext<CommandSourceStack> c, double bearing) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        BlockPos at = looked(p);
        TrunkSearch.Trunk t = at == null ? null : FellingService.trunk(l, at);
        if (t == null) return fail(c, "Look at a rooted natural trunk (placed logs never fall).");
        BlockPos base = FellingService.base(t);
        double b = Double.isNaN(bearing) ? Felling.bearing(p.getX() - (base.getX() + 0.5), p.getZ() - (base.getZ() + 0.5)) : bearing;
        FellingService.fell(l, t, b, p);
        c.getSource().sendSuccess(() -> Component.literal("Felled " + FellingService.kind(t).display + " (" + t.height() + " m) toward "
                + Felling.compass(b) + "."), true);
        return 1;
    }

    static int woodInfo(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ItemStack s = p.getMainHandItem();
        StringBuilder sb = new StringBuilder();
        long today = WoodItems.today();
        WoodPieceItem piece = WoodItems.piece(s);
        if (piece != null) {
            double m = WoodItems.moistureNow(s, today);
            long dry = Seasoning.forecast(WoodConfig.rates(), m, today, Seasoning.SEASONED_PCT, Seasoning.Place.RACK, piece.form, 730);
            sb.append(piece.wood.id()).append(' ').append(piece.form.name().toLowerCase(Locale.ROOT)).append(String.format(Locale.ROOT, ": %.1f%% moisture", m))
              .append(", burns as ").append(piece.wood.fireWood).append(". Under cover: ")
              .append(dry < 0 ? "not dry within 2 years" : "dry by about " + date(dry)).append(".");
        }
        ToolStats t = ToolData.of(s);
        if (t != null) sb.append(String.format(Locale.ROOT, "Tool: fells to %s cm, %d swings at 25 cm, %s min a swing, limb %s, buck %s an armload, split %d (%d on the crack).",
                fmt(t.fellMaxCm()), t.swings25(), fmt(t.swingMin()), fmt(t.limbMin()), fmt(t.buckMin()), t.splitStrikes(), t.splitCrackStrikes()));
        HitResult h = p.pick(8, 1f, false);
        var logs = p.serverLevel().getEntitiesOfClass(FelledLogEntity.class, p.getBoundingBox().inflate(6));
        for (FelledLogEntity log : logs) {
            sb.append(String.format(Locale.ROOT, " Felled %s: %.1f m, %d rounds, %s, %.1f%% moisture.", log.kind().display, log.lengthM(),
                    log.roundsLeft(), log.limbed() ? "limbed" : "limbs on", log.moistureNow(today)));
        }
        if (sb.isEmpty()) return fail(c, "Hold a round, a split or a wood tool, or stand by a felled log.");
        String msg = sb.toString().trim();
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    static int dry(CommandContext<CommandSourceStack> c, int days) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ItemStack s = p.getMainHandItem();
        if (WoodItems.piece(s) == null) return fail(c, "Hold a round or a split.");
        long today = WoodItems.today();
        double m = WoodItems.moistureNow(s, today);
        WoodItems.Stamp st = s.get(WoodRegistry.SEASONING.get());
        long from = st != null ? Math.min(st.day(), today) : today;
        WoodItems.stamp(s, Seasoning.dry(WoodConfig.rates(), m, today, today + days, Seasoning.Place.RACK, WoodItems.piece(s).form), today);
        double now = WoodItems.moistureNow(s, today);
        c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Dried %d days under cover from today: %.1f%% to %.1f%%.", days, m, now)), false);
        return from >= 0 ? 1 : 0;
    }

    static String date(long absDay) {
        int doy = Calendar.dayOfYearOfDay(absDay);
        return Calendar.monthName(Calendar.month(doy)) + " " + Calendar.dayOfMonth(doy);
    }

    static String fmt(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v); }
}
