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
                .then(Commands.literal("spot").executes(FishingCommands::spot))
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
