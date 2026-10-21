package com.yearofice.crop;

import com.mojang.brigadier.CommandDispatcher;
import com.yearofice.api.env.EnvService;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi crop info|germ <crop> <n>|grow <units>|frost <low_c>|ripen} (Crop farming in play › Debug and tuning for crops), op level 2. */
public final class CropCommands {
    private CropCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("crop")
                .then(Commands.literal("info").executes(CropCommands::info))
                .then(Commands.literal("germ").then(Commands.argument("crop", StringArgumentType.word())
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 100000)).executes(CropCommands::germ))))
                .then(Commands.literal("grow").then(Commands.argument("units", DoubleArgumentType.doubleArg(0, 5000)).executes(CropCommands::grow)))
                .then(Commands.literal("frost").then(Commands.argument("low_c", DoubleArgumentType.doubleArg(-60, 20)).executes(CropCommands::frost)))
                .then(Commands.literal("ripen").executes(CropCommands::ripen))
                .then(Commands.literal("stovehouse").executes(CropCommands::stovehouse))));
    }

    private static BlockPos target(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        HitResult h = p.pick(6.0, 0f, false);
        if (h instanceof BlockHitResult b && h.getType() == HitResult.Type.BLOCK) return b.getBlockPos();
        throw new SimpleCommandExceptionType(Component.literal("Look at a bed or a crop.")).create();
    }

    private static int say(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static String f(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    private static BlockPos cropOrAbove(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getBlock() instanceof CropBlock ? p : p.above();
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = target(c);
        BlockPos cp = cropOrAbove(level, pos);
        BlockPos bed = level.getBlockState(pos).getBlock() instanceof CropBlock ? pos.below() : pos;
        StringBuilder sb = new StringBuilder();
        BlockState bs = level.getBlockState(bed);
        if (bs.getBlock() instanceof GardenBedBlock) {
            sb.append("Bed: soil ").append(bs.getValue(GardenBedBlock.SOIL).getSerializedName()).append(", ").append(f(Garden.soilC(level, bed)))
                    .append(" C, moisture ").append(bs.getValue(GardenBedBlock.MOISTURE)).append(", weeds ").append(bs.getValue(GardenBedBlock.WEEDS))
                    .append(", fed ").append(bs.getValue(GardenBedBlock.FED));
        } else sb.append("Not a bed.");
        if (level.getBlockEntity(cp) instanceof CropBlockEntity be && be.spec() != null) {
            CropSpec s = be.spec();
            double soilYield = bs.getBlock() instanceof GardenBedBlock ? bs.getValue(GardenBedBlock.SOIL).yield : 1.0;
            sb.append("\nCrop ").append(s.id).append(": stage ").append(be.stage()).append(", units ").append(f(be.units())).append("/").append(f(s.ripeUnits))
                    .append(", sown day ").append(be.sownDay()).append(", seeds ").append(be.seeds()).append(" up ").append(be.up())
                    .append(", frost x").append(f(be.frostMult())).append(be.frosted() ? " frosted" : "").append(be.dead() ? " dead" : "")
                    .append(", expected ").append(Garden.yieldGrams(be, s, soilYield)).append(" g");
        }
        return say(c, sb.toString());
    }

    private static int germ(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = target(c);
        String id = StringArgumentType.getString(c, "crop");
        CropSpec s = CropData.get(id.contains(":") ? id : "yearofice:" + id);
        if (s == null) return say(c, "No such crop: " + id);
        int n = IntegerArgumentType.getInteger(c, "n");
        double share = Garden.dryRun(level, level.getBlockState(pos).getBlock() instanceof CropBlock ? pos.below() : pos, s, n, Double.NaN);
        return say(c, s.name() + " x" + n + ": " + f(share * 100) + "% came up at soil " + f(Garden.soilC(level, pos)) + " C.");
    }

    private static int grow(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos cp = cropOrAbove(level, target(c));
        if (!(level.getBlockEntity(cp) instanceof CropBlockEntity be)) return say(c, "No crop there.");
        be.addUnits(DoubleArgumentType.getDouble(c, "units"));
        return say(c, "Units " + f(be.units()) + ", stage " + be.stage() + ".");
    }

    private static int frost(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos at = target(c);
        double low = DoubleArgumentType.getDouble(c, "low_c") + GardenConfig.frostOffsetC();
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-16, -4, -16), at.offset(16, 4, 16))) {
            if (level.getBlockState(q).getBlock() instanceof CropBlock && level.getBlockEntity(q) instanceof CropBlockEntity be && be.spec() != null) {
                Garden.nightFrost(be, be.spec(), low);
                n++;
            }
        }
        return say(c, "A night at " + f(low) + " C over " + n + " crop(s).");
    }

    private static int ripen(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos cp = cropOrAbove(level, target(c));
        if (!(level.getBlockEntity(cp) instanceof CropBlockEntity be) || be.spec() == null) return say(c, "No crop there.");
        if (be.up() == 0) return say(c, "Nothing came up on this block.");
        be.addUnits(be.spec().ripeUnits - be.units());
        return say(c, "Ripe: stage " + be.stage() + ".");
    }

    /** M9 lane C: what the room around the looked-at bed gives a crop today. */
    private static int stovehouse(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos cp = cropOrAbove(level, target(c));
        long today = com.yearofice.api.clock.ClockService.get().absDay();
        var in = com.yearofice.crop.stovehouse.StovehouseAdapter.inside(level, cp, today, EnvService.get().airC(level, cp));
        if (in == null) return say(c, "Open ground: no room around " + cp.toShortString());
        return say(c, "room_c=" + f(in.roomC()) + " glazing=" + in.glazing() + " units_today=" + f(in.units()) + " night_low_c=" + f(in.lowC())
                + " turnip_days=" + f(com.yearofice.sim.crop.Stovehouse.daysToRipe(700, in.roomC(), com.yearofice.api.clock.ClockService.get().daylightHours(today), in.glazing())));
    }
}
