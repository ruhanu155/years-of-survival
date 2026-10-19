package com.yearofice.food.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.food.FoodConfig;
import com.yearofice.food.Meals;
import com.yearofice.food.cooking.SpitBlockEntity;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.DecayLadder;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.food.Doneness;
import com.yearofice.sim.food.FoodValues;
import com.yearofice.sim.food.Roasting;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi food} and {@code /yoi decay} (Ready for development › M2.5 debug commands).
 *
 * <ul>
 *   <li>{@code food info}: the held stack's grams a bite, kcal, freshness (points, stage, state, days to Turning at the
 *       stand-in temperature, due and life on its clock), doneness, and the nearest spit's load and progress.</li>
 *   <li>{@code decay advance <days>}: ages every food stack in the player's inventory, and every spit load within
 *       16 blocks, by that many game days at the clock's temperature. It moves the food, not the world clock, so other
 *       players' and GameTests' food is untouched.</li>
 * </ul>
 */
public final class FoodCommands {
    private FoodCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("food").then(Commands.literal("info").executes(FoodCommands::info)))
                .then(Commands.literal("decay").then(Commands.literal("advance")
                        .then(Commands.argument("days", DoubleArgumentType.doubleArg(0, 400)).executes(FoodCommands::advance)))));
    }

    static String fmt(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = p.serverLevel();
        DecayService decay = DecayService.get();
        ItemStack s = p.getMainHandItem();
        StringBuilder out = new StringBuilder();
        FoodValues v = FoodMap.values(s);
        if (v == null) {
            out.append(s.isEmpty() ? "Empty hand." : s.getHoverName().getString() + " is not food.");
        } else {
            double g = Meals.grams(s);
            out.append(s.getHoverName().getString()).append(" x").append(s.getCount()).append(": ").append(fmt(g)).append(" g a bite")
                    .append(s.has(YoiComponents.WEIGHT_G.get()) ? " (weight_g)" : " (food row)")
                    .append(", ").append(Math.round(v.kcal(g))).append(" kcal (").append(fmt(v.kcalPer100g())).append(" per 100 g)");
            Fresh f = decay.settle(level, s);
            double dNow = decay.dNow(level);
            if (f == null) {
                out.append("\nNo freshness stamp yet (stamped raw on first settle).");
            } else {
                double pr = f.progress(dNow);
                out.append("\nState ").append(f.state().key()).append(", freshness ").append(DecayLadder.tooltipFreshness(pr))
                        .append(" (").append(DecayLadder.FOOD.nameOf(pr)).append(", p ").append(fmt(pr)).append("), spoils in ")
                        .append(fmt(decay.daysUntil(level, s, 0.7))).append(" days at ").append(fmt(decay.airC(level))).append(" C")
                        .append("\nClock air:slice, D now ").append(fmt(dNow)).append(" dh, due ").append(fmt(f.dueDh()))
                        .append(" dh, life ").append(fmt(f.lifeDh())).append(" dh");
            }
            Doneness.Ring r = s.get(YoiComponents.DONENESS.get());
            out.append("\nDoneness ").append(r == null ? "raw" : r.name().toLowerCase(Locale.ROOT));
        }
        SpitBlockEntity spit = nearestSpit(level, p.blockPosition(), 4);
        if (spit != null && !spit.empty() && spit.spit() != null) {
            Roasting.Spit sp = spit.spit();
            Doneness.Fire fire = spit.fire();
            out.append("\nSpit: ").append(spit.load().getCount()).append(" x ").append(spit.load().getHoverName().getString())
                    .append(", ").append(spit.ring().name().toLowerCase(Locale.ROOT)).append(" at ").append(Math.round(sp.progress() * 100))
                    .append("% of ").append(fmt(sp.cookMinutes())).append(" min, fire ").append(fire.name().toLowerCase(Locale.ROOT))
                    .append(", Done in ").append(fmt(sp.minutesUntil(Doneness.DONE_FROM, fire, FoodConfig.tuning()))).append(" min");
        }
        String text = out.toString();
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int advance(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = p.serverLevel();
        double days = DoubleArgumentType.getDouble(c, "days");
        DecayService decay = DecayService.get();
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.has(YoiComponents.FRESH.get())) {
                decay.age(level, s, days);
                n++;
            }
        }
        int r = 16;
        for (BlockPos q : BlockPos.betweenClosed(p.blockPosition().offset(-r, -4, -r), p.blockPosition().offset(r, 4, r))) {
            if (level.getBlockEntity(q) instanceof SpitBlockEntity be && !be.empty()) {
                decay.age(level, be.load(), days);
                n++;
            }
        }
        int count = n;
        c.getSource().sendSuccess(() -> Component.literal("Aged " + count + " food stacks by " + fmt(days) + " days at "
                + fmt(decay.airC(level)) + " C."), false);
        return count;
    }

    static SpitBlockEntity nearestSpit(ServerLevel level, BlockPos at, int r) {
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-r, -2, -r), at.offset(r, 2, r))) {
            if (level.getBlockEntity(q) instanceof SpitBlockEntity be) return be;
        }
        return null;
    }
}
