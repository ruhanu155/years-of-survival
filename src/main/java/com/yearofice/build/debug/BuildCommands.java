package com.yearofice.build.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.SleepService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.build.BuildConfig;
import com.yearofice.build.BuildRegistry;
import com.yearofice.build.camp.BedBuilding;
import com.yearofice.build.camp.BoughBedBlock;
import com.yearofice.build.camp.BoughCutting;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi bed}: the bough bed and sleep's debug commands.
 *
 * <ul>
 *   <li>{@code info}: asleep or not, the bed under the player (quality, °C), energy and warmth, boughs carried, the
 *       bed and bough job numbers.</li>
 *   <li>{@code place}: lays a bough bed at the player's feet, its head the way they face (no boughs, no time).</li>
 *   <li>{@code sleep}: lies down on the nearest bough bed within 4 blocks, as right-clicking it would.</li>
 *   <li>{@code wake}: wakes the player through {@code BodyService.wake}.</li>
 *   <li>{@code boughs [n]}: gives spruce boughs (default one bed's worth).</li>
 * </ul>
 */
public final class BuildCommands {
    private BuildCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("bed")
                .then(Commands.literal("info").executes(BuildCommands::info))
                .then(Commands.literal("place").executes(BuildCommands::place))
                .then(Commands.literal("sleep").executes(BuildCommands::sleep))
                .then(Commands.literal("wake").executes(c -> {
                    BodyService.get().wake(c.getSource().getPlayerOrException(), "command");
                    return 1;
                }))
                .then(Commands.literal("boughs").executes(c -> boughs(c, BuildConfig.bedBoughs()))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 640)).executes(c -> boughs(c, IntegerArgumentType.getInteger(c, "n")))))));
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        SleepService.Spot s = SleepService.get().spot(p);
        int mod = ClockService.get().minuteOfDay();
        String line = String.format(Locale.ROOT,
                "Bed %s: %s | q %.2f, bed %+.0f °C%s | energy %.1f bolts, warmth %.0f | boughs %d | now %02d:%02d"
                        + " | cut %.0f min for %d, bed %d boughs in %.0f min",
                p.getGameProfile().getName(), p.isSleeping() ? "asleep" : "awake", s.quality(), s.warmthC(),
                s.bed() == null ? "" : " at " + s.bed().toShortString(), BodyService.get().energy(p), BodyService.get().warmth(p),
                BoughCutting.carried(p), mod / 60, mod % 60, BuildConfig.boughCutMin(), BuildConfig.boughsPerCut(),
                BuildConfig.bedBoughs(), BuildConfig.bedMin());
        c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int place(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Direction facing = p.getDirection();
        BlockPos foot = p.blockPosition();
        String why = BedBuilding.problem(p.level(), foot, facing, null);
        if (why != null) {
            c.getSource().sendFailure(Component.translatable("building.yearofice." + why, BuildConfig.bedBoughs(), 0));
            return 0;
        }
        BoughBedBlock bed = BuildRegistry.BOUGH_BED.get();
        p.level().setBlock(foot, bed.foot(facing), 3);
        p.level().setBlock(foot.relative(facing), bed.head(facing), 3);
        c.getSource().sendSuccess(() -> Component.literal("Bough bed at " + foot.toShortString() + ", head " + facing.getName()), true);
        return 1;
    }

    private static int sleep(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        BlockPos at = p.blockPosition();
        BlockPos head = null;
        double best = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-4, -2, -4), at.offset(4, 2, 4))) {
            BlockState st = p.level().getBlockState(q);
            if (!st.is(BuildRegistry.BOUGH_BED.get())) continue;
            BlockPos h = BoughBedBlock.headOf(st, q);
            double d = h.distSqr(at);
            if (d < best) {
                best = d;
                head = h.immutable();
            }
        }
        if (head == null) {
            c.getSource().sendFailure(Component.literal("No bough bed within 4 blocks."));
            return 0;
        }
        SleepService.Refusal r = SleepService.get().lieDown(p, head);
        c.getSource().sendSuccess(() -> Component.literal("Lie down: " + r.name().toLowerCase(Locale.ROOT)), false);
        return r == SleepService.Refusal.NONE ? 1 : 0;
    }

    private static int boughs(CommandContext<CommandSourceStack> c, int n) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ItemStack s = new ItemStack(BuildRegistry.SPRUCE_BOUGHS.get(), n);
        p.getInventory().add(s);
        if (!s.isEmpty()) p.drop(s, false);
        c.getSource().sendSuccess(() -> Component.literal("Gave " + n + " spruce boughs"), false);
        return 1;
    }
}
