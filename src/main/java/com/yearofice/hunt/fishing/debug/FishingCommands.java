package com.yearofice.hunt.fishing.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.yearofice.hunt.fishing.BobberEntity;
import com.yearofice.hunt.fishing.FishingClock;
import com.yearofice.hunt.fishing.FishingConfig;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.hunt.fishing.FishingService;
import com.yearofice.hunt.fishing.FishingSpecies;
import com.yearofice.hunt.fishing.PassiveGearService;
import com.yearofice.hunt.fishing.SpotFish;
import com.yearofice.hunt.fishing.FightSession;
import com.yearofice.hunt.fishing.RodItem;
import com.yearofice.hunt.fishing.SliceSpots;
import com.yearofice.sim.fishing.BiteModel;
import com.yearofice.sim.fishing.FightModel;
import com.yearofice.sim.fishing.FishStock;
import com.yearofice.sim.fishing.FishingTuning;
import com.yearofice.sim.fishing.HookBait;
import com.yearofice.sim.fishing.SliceBite;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi fish}: the rod's debug commands (Ready for development › M2.5 debug commands; Minecraft engine › Ruhan's
 * project rules: every system ships {@code /yoi} commands).
 *
 * <ul>
 *   <li>{@code spot}: the water the player looks at (or the bobber's, or underfoot): spot or not, depth, q, the
 *       grayling stock, and why it fails the rule when it does.</li>
 *   <li>{@code bite}: a real bite on the player's floating bobber now (the twitch, then the dip).</li>
 *   <li>{@code info}: the bobber, the bait, the line, P_bite this minute with each factor, and the fight's state.</li>
 *   <li>{@code kit}: a rod and 8 worms.</li>
 * </ul>
 * The {@code yoi} root is registered by core; Brigadier merges this subtree into it.
 */
public final class FishingCommands {
    private FishingCommands() {}

    private static final SimpleCommandExceptionType NO_BOBBER = new SimpleCommandExceptionType(Component.literal("No bobber in the water. Cast first."));
    private static final SimpleCommandExceptionType NO_WATER = new SimpleCommandExceptionType(Component.literal("No water in sight or underfoot."));

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("fish")
                .then(Commands.literal("spot").executes(FishingCommands::spot)
                        .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                .executes(FishingCommands::spotAt)))
                .then(Commands.literal("fight")
                        .then(Commands.argument("species", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .then(Commands.argument("kg", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.01, 100))
                                        .executes(FishingCommands::fight))))
                .then(Commands.literal("gear")
                        .then(Commands.literal("place")
                                .then(Commands.argument("kind", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
                                                .executes(FishingCommands::gearPlace))))
                        .then(Commands.literal("check").executes(FishingCommands::gearCheck))
                        .then(Commands.literal("settle")
                                .then(Commands.argument("hours", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 24 * 400))
                                        .executes(FishingCommands::gearSettle))))
                .then(Commands.literal("bite").executes(FishingCommands::bite))
                .then(Commands.literal("info").executes(FishingCommands::info))
                .then(Commands.literal("kit").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.getInventory().add(new ItemStack(FishingRegistry.ROD.get()));
                    p.getInventory().add(new ItemStack(FishingRegistry.WORM.get(), 8));
                    c.getSource().sendSuccess(() -> Component.literal("A rod and 8 worms."), false);
                    return 1;
                }))));
    }

    static BlockPos lookedAtWater(ServerPlayer p) throws CommandSyntaxException {
        BobberEntity b = FishingService.bobberOf(p);
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(24)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.WATER, p));
        if (hit.getType() == HitResult.Type.BLOCK && p.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) return surface(p, hit.getBlockPos());
        if (b != null && b.state() == BobberEntity.State.FLOATING) return surface(p, BlockPos.containing(b.getX(), b.getY() - 0.05, b.getZ()));
        BlockPos feet = p.blockPosition();
        if (p.level().getFluidState(feet).is(FluidTags.WATER)) return surface(p, feet);
        throw NO_WATER.create();
    }

    private static BlockPos surface(ServerPlayer p, BlockPos w) {
        BlockPos q = w;
        for (int i = 0; i < 16 && p.level().getFluidState(q.above()).is(FluidTags.WATER); i++) q = q.above();
        return q;
    }

    private static int spot(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = p.serverLevel();
        FishingTuning t = FishingConfig.tuning();
        BlockPos w = lookedAtWater(p);
        SliceSpots.Spot s = SliceSpots.at(level, w, t);
        long minute = FishingClock.minute(level.getServer());
        FishStock st = SliceSpots.stocks(level).get(s.key(), FishingClock.day(minute), t);
        String msg = s.atSpot()
                ? String.format(Locale.ROOT, "Spot: %s at %d %d %d. q %.2f, radius %.0f m, depth %.2f m. Grayling %.0f / %.0f (m_stock %.2f).%s",
                        SliceSpots.SPOT_TYPE, w.getX(), w.getY(), w.getZ(), t.spotQ(), t.spotRadiusM(), s.depthM(), st.n(), st.k(),
                        st.mStock(), SliceSpots.stocks(level).spooked(s.key(), minute) ? " Spooked." : "")
                : String.format(Locale.ROOT, "Not a spot at %d %d %d (depth %.2f m): it needs water %d x %d blocks across and %.1f m deep in every column. Bites here x%.2f.",
                        w.getX(), w.getY(), w.getZ(), s.depthM(), t.spotMinWidth(), t.spotMinWidth(), t.spotMinDepthM(), BiteModel.mPlace(1e9, 1));
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return s.atSpot() ? 1 : 0;
    }

    /** {@code /yoi fish spot <pos>}: the generated spot around a block, each species' stock and the last catch. */
    private static int spotAt(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "pos");
        FishingTuning t = FishingConfig.tuning();
        long minute = FishingClock.minute(level.getServer());
        long day = FishingClock.day(minute);
        SpotFish.Here here = SpotFish.at(level, pos, com.yearofice.sim.fishing.FishSeason.dayOfYear(day), t);
        if (here.spot().isEmpty()) {
            c.getSource().sendSuccess(() -> Component.literal("No generated fishing spot holds " + pos.toShortString() + "."), false);
            return 0;
        }
        var s = here.spot().get();
        long cell = SliceSpots.key(pos);
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "Spot %s at %s: q %.2f, radius %.0f m, depth %.1f m.", s.type(),
                s.centre().toShortString(), s.quality(), s.radiusM(), s.depthM()));
        for (SpotFish.Entry e : here.fish()) {
            FishStock st = SpotFish.stock(level, cell, day, e);
            long last = SliceSpots.stocks(level).lastCatch(SliceSpots.speciesKey(cell, e.id()));
            sb.append(String.format(Locale.ROOT, " %s %.0f/%.0f (season %.2f, last catch %s);", e.id(), st.n(), st.k(),
                    FishingSpecies.season(e.id()).factor(com.yearofice.sim.fishing.FishSeason.dayOfYear(day), s.type()),
                    last == Long.MIN_VALUE ? "never" : (minute - last) / 60 + " h ago"));
        }
        String msg = sb.toString();
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return here.fish().size();
    }

    /** {@code /yoi fish fight <species> <kg>}: the fish on the floating bobber is this species and weight; the fight screen opens. */
    private static int fight(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String id = FishingSpecies.canon(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "species"));
        if (!FishingSpecies.has(id)) {
            c.getSource().sendFailure(Component.literal("Unknown species " + id + ". Known: " + FishingSpecies.IDS));
            return 0;
        }
        BobberEntity b = bobber(p);
        if (b.state() != BobberEntity.State.FLOATING) {
            c.getSource().sendFailure(Component.literal("The bobber is " + b.state().name().toLowerCase(Locale.ROOT) + "."));
            return 0;
        }
        b.biter = id;
        FishingService.startFight(b, com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "kg"));
        c.getSource().sendSuccess(() -> Component.literal("Hooked a " + id + "."), false);
        return 1;
    }

    private static int gearPlace(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        PassiveGearService.Kind k;
        try {
            k = PassiveGearService.Kind.parse(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "kind"));
        } catch (IllegalArgumentException e) {
            c.getSource().sendFailure(Component.literal("Kinds: set_line, gill_net, fish_trap."));
            return 0;
        }
        int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "count");
        PassiveGearService.place(p.serverLevel(), k, p.blockPosition(), n, FishingClock.minute(p.server));
        c.getSource().sendSuccess(() -> Component.literal("Placed " + k.name().toLowerCase(Locale.ROOT) + " (" + n + ") here."
                + (k == PassiveGearService.Kind.GILL_NET && n < com.yearofice.sim.fishing.PassiveGear.NET_MIN_SEGMENTS ? " Under 4 joined segments it never catches." : "")), false);
        return 1;
    }

    private static PassiveGearService.Gear nearestGear(ServerPlayer p) {
        PassiveGearService.Gear best = null;
        double bd = 36;
        for (PassiveGearService.Gear g : PassiveGearService.book(p.serverLevel()).all().values()) {
            double d = g.pos.distSqr(p.blockPosition());
            if (d <= bd) { bd = d; best = g; }
        }
        return best;
    }

    private static int gearCheck(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        PassiveGearService.Gear g = nearestGear(p);
        if (g == null) {
            c.getSource().sendFailure(Component.literal("No gear within 6 blocks."));
            return 0;
        }
        var items = PassiveGearService.check(p.serverLevel(), p, g, FishingClock.minute(p.server));
        c.getSource().sendSuccess(() -> Component.literal("Checked " + g.kind.name().toLowerCase(Locale.ROOT) + ": " + items.size() + " fish."), false);
        return items.size();
    }

    private static int gearSettle(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        PassiveGearService.Gear g = nearestGear(p);
        if (g == null) {
            c.getSource().sendFailure(Component.literal("No gear within 6 blocks."));
            return 0;
        }
        long to = g.lastMinute + 60L * com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "hours");
        var r = PassiveGearService.settle(p.serverLevel(), g, to);
        c.getSource().sendSuccess(() -> Component.literal("Settled " + r.periods() + " periods: " + r + "; holds " + g.held.size() + "."), false);
        return r.caught();
    }

    private static BobberEntity bobber(ServerPlayer p) throws CommandSyntaxException {
        BobberEntity b = FishingService.bobberOf(p);
        if (b == null) throw NO_BOBBER.create();
        return b;
    }

    private static int bite(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        BobberEntity b = bobber(p);
        if (b.state() != BobberEntity.State.FLOATING) {
            c.getSource().sendFailure(Component.literal("The bobber is " + b.state().name().toLowerCase(Locale.ROOT) + "."));
            return 0;
        }
        ItemStack rod = FishingService.heldRod(p);
        if (!RodItem.bait(rod).present()) RodItem.setBait(rod, HookBait.fitted("worm", FishingConfig.tuning().wormBites(), FishingConfig.tuning().wormMinutes()));
        FishingService.forceBite(b);
        c.getSource().sendSuccess(() -> Component.literal("A bite: the rod tip twitches, then the bobber dips. Strike on the dip."), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        StringBuilder sb = new StringBuilder();
        ItemStack rod = FishingService.heldRod(p);
        if (!rod.isEmpty()) {
            HookBait bait = RodItem.bait(rod);
            sb.append(String.format(Locale.ROOT, "Rod: line %d%%, bait %s.", RodItem.lineCondition(rod),
                    bait.present() ? bait.bait() + " (" + bait.bitesLeft() + " bites, " + bait.minutesLeft() + " min)" : "none"));
        } else {
            sb.append("No rod in hand.");
        }
        BobberEntity b = FishingService.bobberOf(p);
        if (b == null) {
            sb.append(" No bobber out.");
        } else {
            sb.append(" Bobber ").append(b.state().name().toLowerCase(Locale.ROOT)).append(", cue ").append(b.cue().name().toLowerCase(Locale.ROOT)).append('.');
            if (b.state() == BobberEntity.State.FLOATING) {
                long minute = FishingClock.minute(p.server);
                SliceBite.Inputs in = FishingService.inputs(b, minute);
                BiteModel.Conditions cnd = SliceBite.conditions(in);
                sb.append(String.format(Locale.ROOT,
                        " P_bite %.4f/min: b %.3f, q %.2f, bait %.2f, time %s %.2f, weather %s %.2f, season %.2f (day %d), water %.2f, stock %.2f, place %.2f%s.",
                        SliceBite.pBite(in), in.species().bitesPerMin(), in.spotQ(), in.species().baitMultiplier(in.bait().bait()),
                        in.time().name().toLowerCase(Locale.ROOT), BiteModel.mTime(in.species(), cnd), in.weather().name().toLowerCase(Locale.ROOT),
                        BiteModel.mWeather(cnd), cnd.seasonFactor(), in.dayOfYear(), BiteModel.mWater(in.species(), cnd),
                        BiteModel.mStock(in.stockN(), in.stockK()), BiteModel.mPlace(cnd.distanceToSpotM(), cnd.spotRadiusM()),
                        in.spooked() ? ", spooked x0.5" : ""));
            }
            FightSession f = b.fight();
            if (f != null) {
                FightModel.State s = f.state();
                sb.append(String.format(Locale.ROOT, " Fight %d: %.2f kg grayling, %s, tension %.0f%% of %.2f kg, stamina %.2f, line out %.1f m, tick %d.",
                        f.fightId(), f.params().kg(), s.phase().name().toLowerCase(Locale.ROOT), 100 * s.tensionShare(f.params()), f.params().sEff(),
                        s.stamina(), s.lineOutM(), s.tick()));
            }
        }
        String msg = sb.toString();
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}
