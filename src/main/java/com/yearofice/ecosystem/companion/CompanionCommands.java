package com.yearofice.ecosystem.companion;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.herd.LeadFence;
import com.yearofice.sim.companion.CompanionTuning;
import com.yearofice.sim.companion.Trust;
import com.yearofice.sim.companion.WolfLeaving;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * M9 lane A debug: {@code /yoi wolf [info|pup|orphan|follow|stay|hunt|name|trust|leave|return]}, {@code /yoi sled
 * [spawn|info|hitch|drive]} and {@code /yoi herd fence <pos>}.
 */
public final class CompanionCommands {
    private CompanionCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("wolf").executes(c -> info(c))
                        .then(Commands.literal("info").executes(c -> info(c)))
                        .then(Commands.literal("pup").executes(c -> spawn(c, 0)))
                        .then(Commands.literal("orphan").executes(c -> spawn(c, 10)))
                        .then(Commands.literal("follow").executes(c -> command(c, WolfEntity.Command.FOLLOW)))
                        .then(Commands.literal("stay").executes(c -> command(c, WolfEntity.Command.STAY)))
                        .then(Commands.literal("hunt").executes(c -> command(c, WolfEntity.Command.HUNT)))
                        .then(Commands.literal("trust").then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 100)).executes(CompanionCommands::trust)))
                        .then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.word()).executes(CompanionCommands::name)))
                        .then(Commands.literal("leave")
                                .then(Commands.literal("test").executes(CompanionCommands::leaveTest))
                                .then(Commands.literal("now").executes(CompanionCommands::leaveNow)))
                        .then(Commands.literal("return").executes(CompanionCommands::bringBack)))
                .then(Commands.literal("sled")
                        .then(Commands.literal("spawn").executes(CompanionCommands::sledSpawn))
                        .then(Commands.literal("info").executes(CompanionCommands::sledInfo))
                        .then(Commands.literal("hitch").executes(CompanionCommands::sledHitch)))
                .then(Commands.literal("herd").then(Commands.literal("fence").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(CompanionCommands::fence)))));
    }

    static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    static WolfEntity nearest(CommandContext<CommandSourceStack> c) {
        Vec3 p = c.getSource().getPosition();
        WolfEntity best = null;
        for (WolfEntity w : c.getSource().getLevel().getEntitiesOfClass(WolfEntity.class, new AABB(p, p).inflate(64), w -> w.trust() != null))
            if (best == null || w.distanceToSqr(p) < best.distanceToSqr(p)) best = w;
        return best;
    }

    static int info(CommandContext<CommandSourceStack> c) {
        WolfEntity w = nearest(c);
        if (w == null) {
            c.getSource().sendFailure(Component.literal("No tameable wolf within 64 blocks (/yoi wolf pup)."));
            return 0;
        }
        Trust t = w.trust();
        return say(c, String.format("%s: trust %.0f (%s, %s), age %d days (%.0f%% grown), command %s, owner %s, warnings %d, shared kills %d, neglect %d days%s",
                w.wolfName().isEmpty() ? "wolf" : w.wolfName(), t.value(), t.rank(), t.stage(), w.ageDays(), w.growth() * 100, w.command(),
                w.owned() ? "yes" : "none", w.warnings(), w.sharedKills(), t.neglectDays(), w.harnessed() ? ", harnessed" : ""));
    }

    static int spawn(CommandContext<CommandSourceStack> c, double start) {
        ServerLevel level = c.getSource().getLevel();
        WolfEntity w = EcosystemRegistry.WOLF.get().create(level);
        w.moveTo(c.getSource().getPosition().add(4, 0, 0));
        w.makeTameable(start, start > 0 ? 120 : 60);
        level.addFreshEntity(w);
        return say(c, "A " + (start > 0 ? "orphan" : "den pup") + " wolf stands 4 blocks away. Leave meat on the ground and step back.");
    }

    static int command(CommandContext<CommandSourceStack> c, WolfEntity.Command cmd) {
        WolfEntity w = nearest(c);
        if (w == null) return 0;
        w.setCommand(cmd);
        return say(c, "wolf: " + cmd.name().toLowerCase());
    }

    static int trust(CommandContext<CommandSourceStack> c) {
        WolfEntity w = nearest(c);
        if (w == null) {
            c.getSource().sendFailure(Component.literal("No tameable wolf within 64 blocks."));
            return 0;
        }
        w.trust().set(DoubleArgumentType.getDouble(c, "value"));
        if (!w.owned() && c.getSource().getEntity() instanceof ServerPlayer p) w.setOwner(p);
        return say(c, "trust " + w.trust().value() + " (" + w.trust().rank() + ")");
    }

    static int name(CommandContext<CommandSourceStack> c) {
        WolfEntity w = nearest(c);
        if (w == null) return 0;
        w.setName(StringArgumentType.getString(c, "name"));
        return say(c, "named " + w.wolfName());
    }

    static int leaveTest(CommandContext<CommandSourceStack> c) {
        WolfEntity w = nearest(c);
        double trust = w == null ? 60 : w.trust().value();
        CompanionTuning t = CompanionConfig.tuning();
        WolfLeaving.Stats s = WolfLeaving.simulate(t, trust, false, 1000, 1L);
        WolfLeaving.Stats halved = WolfLeaving.simulate(t, trust, true, 1000, 1L);
        return say(c, String.format("1,000 Februaries at trust %.0f (wolf_leaves %d): lost %.1f%% (%.1f%% if fed and roofed), came back %.0f%% of the lost.",
                trust, t.wolfLeaves(), s.lostPct(), halved.lostPct(), s.returnedOfLostPct()));
    }

    static int leaveNow(CommandContext<CommandSourceStack> c) {
        WolfEntity w = nearest(c);
        if (w == null || !(w.owner(c.getSource().getLevel()) instanceof ServerPlayer p)) {
            c.getSource().sendFailure(Component.literal("No owned wolf within 64 blocks."));
            return 0;
        }
        CompanionTuning t = CompanionConfig.tuning();
        int back = t.wolfLeaves() == CompanionTuning.PERMANENT ? 0 : t.returnMinDays();
        w.leave(p, back);
        return say(c, back > 0 ? "the wolf left; it returns in " + back + " days (/yoi wolf return to bring it now)" : "the wolf left for good");
    }

    static int bringBack(CommandContext<CommandSourceStack> c) {
        if (!(c.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        WolfEntity w = Companions.bring(p, c.getSource().getLevel());
        return w == null ? 0 : say(c, "the wolf is back");
    }

    static int sledSpawn(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        SledEntity s = CompanionRegistry.SLED.get().create(level);
        s.moveTo(c.getSource().getPosition().add(2, 0, 0));
        level.addFreshEntity(s);
        return say(c, "sled placed");
    }

    static SledEntity sled(CommandContext<CommandSourceStack> c) {
        Vec3 p = c.getSource().getPosition();
        SledEntity best = null;
        for (SledEntity s : c.getSource().getLevel().getEntitiesOfClass(SledEntity.class, new AABB(p, p).inflate(32)))
            if (best == null || s.distanceToSqr(p) < best.distanceToSqr(p)) best = s;
        return best;
    }

    static int sledInfo(CommandContext<CommandSourceStack> c) {
        SledEntity s = sled(c);
        if (s == null) return 0;
        var tun = CompanionConfig.tuning();
        var surf = s.surface();
        return say(c, String.format("sled: team %d, surface %s, speed %.1f m/s, capacity %.0f kg here, cargo %.0f kg", s.teamSize(), surf, s.speedMps(),
                com.yearofice.sim.companion.SledMath.capacityKg(tun, s.teamSize(), surf), s.cargoKg()));
    }

    static int sledHitch(CommandContext<CommandSourceStack> c) {
        SledEntity s = sled(c);
        return s == null ? 0 : say(c, s.hitchNear(16) + " wolves hitched, team " + s.teamSize());
    }

    static int fence(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(c, "pos");
        LeadFence.Layout l = LeadFence.scan(c.getSource().getLevel(), pos, LeadFence.SCAN_RADIUS);
        if (l.wings().isEmpty()) return say(c, "no lead fence within " + LeadFence.SCAN_RADIUS + " blocks");
        CompanionTuning t = CompanionConfig.tuning();
        StringBuilder sb = new StringBuilder("lead fence: ").append(l.wings().size()).append(" wing(s), mouth ")
                .append(l.mouth() == null ? "none yet" : String.format("%.0f %.0f %.0f", l.mouth().x, l.mouth().y, l.mouth().z));
        for (LeadFence.Wing w : l.wings())
            sb.append(String.format("%n  wing of %d blocks = %d segments, heading %.0f, follow %.0f%%", w.cells().size(), w.segments(), w.headingDeg(),
                    100 * com.yearofice.sim.companion.LeadFenceMath.followChance(t, w.segments())));
        sb.append("\n  snares: ").append(l.snares().size());
        return say(c, sb.toString());
    }
}
