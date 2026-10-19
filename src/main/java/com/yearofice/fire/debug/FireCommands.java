package com.yearofice.fire.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.yearofice.fire.BowDrillService;
import com.yearofice.fire.FireBurn;
import com.yearofice.fire.FireConfig;
import com.yearofice.fire.FireEnv;
import com.yearofice.fire.FireIndex;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.fire.FireSkill;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.fire.BowDrill;
import com.yearofice.sim.fire.BowDrillJudge;
import com.yearofice.sim.fire.Fire;
import com.yearofice.sim.fire.FireHeat;
import com.yearofice.sim.fire.FireTuning;
import com.yearofice.sim.fire.FireWeather;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi fire}: the fire's debug commands (Blueprint › The fire state machine: "/yoi fire state and /yoi fire
 * fuel set a fire"). Each acts on the nearest fire ring within 8 blocks.
 *
 * <ul>
 *   <li>{@code info}: state, setting, fuel and time left, light (block state and measured), heat in °C and kW.</li>
 *   <li>{@code state <out|ember|tinder|small|low|steady|hot|coals|banked>}: put the fire into a state.</li>
 *   <li>{@code fuel <armloads>}: set the fuel queue (dead standing spruce at 18%).</li>
 *   <li>{@code light}: light the lay as a struck match and two puffs would (no roll); says why when it cannot.</li>
 *   <li>{@code bank}: bank coals.</li>
 *   <li>{@code advance <minutes>}: run the fire forward in game minutes.</li>
 *   <li>{@code weather <clear|light_rain|heavy_rain|snow|world> [wind_ms]}: override the weather at every fire.</li>
 *   <li>{@code drill [roll]}: an Automatic-assist bow-drill attempt with the held drill (rolls the practice chance).</li>
 *   <li>{@code skill [reps]}: show or set the Fire skill reps.</li>
 *   <li>{@code kit}: the slice fire kit (fire ring, 3 matches as in the crate, bow drill, tinder, kindling).</li>
 * </ul>
 * The {@code yoi} root is registered by core; Brigadier merges this subtree into it.
 */
public final class FireCommands {
    private FireCommands() {}

    private static final SimpleCommandExceptionType NO_RING = new SimpleCommandExceptionType(Component.literal("No fire ring within 8 blocks."));

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        String[] states = Arrays.stream(FireBurn.values()).map(FireBurn::getSerializedName).toArray(String[]::new);
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("fire")
                .then(Commands.literal("info").executes(FireCommands::info))
                .then(Commands.literal("state").then(Commands.argument("state", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(states, b)).executes(FireCommands::state)))
                .then(Commands.literal("fuel").then(Commands.argument("armloads", DoubleArgumentType.doubleArg(0, 6)).executes(c -> {
                    FireRingBlockEntity be = ring(c);
                    be.setFuel(DoubleArgumentType.getDouble(c, "armloads"));
                    return report(c, be);
                })))
                .then(Commands.literal("light").executes(c -> {
                    FireRingBlockEntity be = ring(c);
                    ItemStack match = new ItemStack(FireRegistry.MATCH.get());
                    Fire.Result r = be.strikeMatch(null, match, 0.0);
                    if (r.ok()) {
                        be.puff();
                        be.puff();
                    } else {
                        c.getSource().sendFailure(Component.literal(r.line()));
                        return 0;
                    }
                    return report(c, be);
                }))
                .then(Commands.literal("bank").executes(c -> {
                    FireRingBlockEntity be = ring(c);
                    Fire.Result r = be.bank();
                    if (!r.ok()) {
                        c.getSource().sendFailure(Component.literal(r.line()));
                        return 0;
                    }
                    return report(c, be);
                }))
                .then(Commands.literal("advance").then(Commands.argument("minutes", DoubleArgumentType.doubleArg(1, 1440)).executes(c -> {
                    FireRingBlockEntity be = ring(c);
                    be.stepMinutes((int) DoubleArgumentType.getDouble(c, "minutes"));
                    return report(c, be);
                })))
                .then(Commands.literal("weather").then(Commands.argument("kind", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"clear", "light_rain", "heavy_rain", "snow", "world"}, b))
                        .executes(c -> weather(c, 0))
                        .then(Commands.argument("wind_ms", DoubleArgumentType.doubleArg(0, 40)).executes(c -> weather(c, DoubleArgumentType.getDouble(c, "wind_ms"))))))
                .then(Commands.literal("drill").executes(c -> drill(c, -1))
                        .then(Commands.argument("roll", DoubleArgumentType.doubleArg(0, 1)).executes(c -> drill(c, DoubleArgumentType.getDouble(c, "roll")))))
                .then(Commands.literal("skill").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    c.getSource().sendSuccess(() -> Component.literal("Fire skill: " + FireSkill.reps(p) + " reps"), false);
                    return 1;
                }).then(Commands.argument("reps", DoubleArgumentType.doubleArg(0, 1000)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    FireSkill.setReps(p, DoubleArgumentType.getDouble(c, "reps"));
                    c.getSource().sendSuccess(() -> Component.literal("Fire skill set to " + FireSkill.reps(p) + " reps"), false);
                    return 1;
                })))
                .then(Commands.literal("kit").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    give(p, new ItemStack(FireRegistry.FIRE_RING_ITEM.get()));
                    give(p, new ItemStack(FireRegistry.MATCH.get(), 3));
                    give(p, new ItemStack(FireRegistry.BOW_DRILL.get()));
                    give(p, new ItemStack(FireRegistry.TINDER_BUNDLE.get(), 4));
                    give(p, new ItemStack(FireRegistry.KINDLING.get(), 8));
                    c.getSource().sendSuccess(() -> Component.literal("Fire kit given."), false);
                    return 1;
                }))));
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    static FireRingBlockEntity ring(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        Vec3 at = c.getSource().getPosition();
        BlockPos p = FireIndex.INSTANCE.nearest(level, at.x, at.y, at.z, 8);
        if (p == null || !(level.getBlockEntity(p) instanceof FireRingBlockEntity be)) throw NO_RING.create();
        return be;
    }

    private static int state(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        FireBurn b = FireBurn.byName(StringArgumentType.getString(c, "state"));
        if (b == null) {
            c.getSource().sendFailure(Component.literal("Unknown state. Use out, ember, tinder, small, low, steady, hot, coals or banked."));
            return 0;
        }
        FireRingBlockEntity be = ring(c);
        be.forceState(b.state, b.setting);
        return report(c, be);
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return report(c, ring(c));
    }

    private static int report(CommandContext<CommandSourceStack> c, FireRingBlockEntity be) {
        FireTuning t = FireConfig.tuning();
        Fire f = be.fire();
        FireHeat h = be.heat();
        BlockPos p = be.getBlockPos();
        int measured = c.getSource().getLevel().getBrightness(LightLayer.BLOCK, p);
        int shown = be.getBlockState().getLightEmission(c.getSource().getLevel(), p);
        String line = String.format("%s | state %s, %s | fuel %.2f armloads, %.0f min left | light %d (measured %d) | heat +%.1f C, %.1f kW | lay: tinder %s, kindling %d%s",
                be.readout(t), f.state().name().toLowerCase(), Fire.word(f.setting()), f.armloads(), f.minutesLeft(t), shown, measured,
                h.bonusC(), h.kW(), f.hasTinder() ? f.tinderMoisture() + "%" : "none", f.kindling(), f.layInOrder() ? "" : " (out of order)");
        c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int weather(CommandContext<CommandSourceStack> c, double wind) {
        String kind = StringArgumentType.getString(c, "kind");
        FireWeather w = switch (kind) {
            case "clear" -> new FireWeather(Precip.NONE, false, wind, false);
            case "light_rain" -> new FireWeather(Precip.DRIZZLE, false, wind, false);
            case "heavy_rain" -> new FireWeather(Precip.RAIN, true, wind, false);
            case "snow" -> new FireWeather(Precip.SNOW, false, wind, false);
            default -> null;
        };
        FireEnv.setOverride(c.getSource().getLevel(), w);
        c.getSource().sendSuccess(() -> Component.literal(w == null ? "Fire weather follows the world." : "Fire weather: " + kind + ", wind " + wind + " m/s."), false);
        return 1;
    }

    /** An Automatic-assist attempt: the practice chance decides the ember; the judge applies wet wood, tinder and energy. */
    private static int drill(CommandContext<CommandSourceStack> c, double roll) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        FireRingBlockEntity be = ring(c);
        FireTuning t = FireConfig.tuning();
        String why = be.fire().cannotLight(t);
        if (why != null) {
            c.getSource().sendFailure(Component.literal(why));
            return 0;
        }
        ItemStack drill = p.getMainHandItem().is(FireRegistry.BOW_DRILL.get()) ? p.getMainHandItem() : ItemStack.EMPTY;
        if (drill.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Hold the bow drill."));
            return 0;
        }
        double r = roll < 0 ? p.getRandom().nextDouble() : roll;
        double reps = FireSkill.reps(p);
        boolean ember = r < BowDrill.autoChance(reps, be.weather().windMs(), p.isShiftKeyDown(), t);
        double seconds = BowDrill.practiced(reps, t) ? BowDrill.fastestHeldS() : 75;
        BowDrillJudge.Summary s = ember
                ? new BowDrillJudge.Summary(BowDrillJudge.Claim.FLAME, 120, 120, 100, 3, BowDrill.practiced(reps, t))
                : new BowDrillJudge.Summary(BowDrillJudge.Claim.NO_EMBER, 120, 40, 70, 0, false);
        BowDrillJudge.Verdict v = BowDrillJudge.judge(s, new BowDrillJudge.Facts(seconds, reps,
                com.yearofice.fire.FireParts.moisture(drill), be.fire().tinderMoisture(), FireSkill.energyBolts(p)), t);
        BowDrillService.apply(p, be, drill, v, t);
        c.getSource().sendSuccess(() -> Component.literal("Bow drill: " + v.outcome() + " (" + v.kcal() + " kcal, "
                + String.format("%.1f", v.gameMinutes()) + " game min)"), false);
        return 1;
    }
}
