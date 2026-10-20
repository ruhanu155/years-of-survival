package com.yearofice.hunt.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.hunt.BloodTrail;
import com.yearofice.hunt.HuntConfig;
import com.yearofice.hunt.HuntDebug;
import com.yearofice.hunt.HuntItems;
import com.yearofice.hunt.danger.Encounters;
import com.yearofice.hunt.wound.WoundTracker;
import com.yearofice.sim.hunt.Ballistics;
import com.yearofice.sim.hunt.EncounterRules;
import com.yearofice.sim.hunt.HitZones;
import com.yearofice.sim.hunt.HuntData;
import com.yearofice.sim.hunt.Shot;
import com.yearofice.sim.hunt.WoundModel;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi hunt}: aim (drop and drift by distance for a weapon), shot (a broadside-to-head-on trace through a
 * species' zones), last (the last arrow's verdict), track (the next blood patch), wound (opens a wound on the nearest
 * animal), give (a stack of arrows with a head) and encounter (bear or wolf outcome posts).
 */
public final class HuntCommands {
    private HuntCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("hunt")
                .then(Commands.literal("aim").executes(c -> aim(c, "longbow", 0)).then(Commands.argument("weapon", StringArgumentType.word())
                        .executes(c -> aim(c, StringArgumentType.getString(c, "weapon"), 0))
                        .then(Commands.argument("wind_ms", DoubleArgumentType.doubleArg(0, 30))
                                .executes(c -> aim(c, StringArgumentType.getString(c, "weapon"), DoubleArgumentType.getDouble(c, "wind_ms"))))))
                .then(Commands.literal("shot").then(Commands.argument("species", StringArgumentType.word())
                        .executes(c -> shot(c, "longbow", "knapped_stone", 20, "broadside"))
                        .then(Commands.argument("weapon", StringArgumentType.word()).then(Commands.argument("head", StringArgumentType.word())
                                .then(Commands.argument("dist_m", DoubleArgumentType.doubleArg(0, 100)).then(Commands.argument("angle", StringArgumentType.word())
                                        .executes(c -> shot(c, StringArgumentType.getString(c, "weapon"), StringArgumentType.getString(c, "head"),
                                                DoubleArgumentType.getDouble(c, "dist_m"), StringArgumentType.getString(c, "angle")))))))))
                .then(Commands.literal("last").executes(c -> say(c, HuntDebug.last(c.getSource().getPlayer()))))
                .then(Commands.literal("track").executes(HuntCommands::track))
                .then(Commands.literal("wound").then(Commands.argument("channel", StringArgumentType.word()).executes(HuntCommands::wound)))
                .then(Commands.literal("give").then(Commands.argument("head", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.getInventory().add(HuntItems.arrow(StringArgumentType.getString(c, "head"), 16));
                    return say(c, "16 arrows.");
                })))
                .then(Commands.literal("encounter").then(Commands.literal("bear").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var r = Encounters.bearCharge(p, 12, false, true, EncounterRules.Reaction.STAND_GROUND, p.getRandom().nextDouble());
                    return say(c, "bear: " + r);
                })).then(Commands.literal("wolf").executes(c -> say(c, "wolves rush: "
                        + Encounters.wolvesRush(c.getSource().getPlayerOrException(), 4, 25, true, false)))))));
    }

    static int say(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    static int aim(CommandContext<CommandSourceStack> c, String weapon, double wind) {
        var w = HuntData.weapon(weapon);
        var t = HuntConfig.tuning();
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "%s: %.1f J full draw, %.0f m/s, ready %.1f s, sway %.1f cm at 20 m, wind %.0f m/s across.\n",
                weapon, w.fullEnergyJ(), w.arrowSpeedMps(), w.drawTimeS(), w.swayCm(20, 0, 0, 0, t), wind));
        for (int d : new int[] {10, 20, 30, 40}) {
            double[] r = Ballistics.dropAndDrift(w.arrowSpeedMps(), d, wind, t);
            sb.append(String.format(Locale.ROOT, "%d m: drop %.0f cm, drift %.0f cm, %.2f s; range factor %.2f\n", d, r[0] * 100, r[1] * 100, r[2], Shot.rangeFactor(d, w.effectiveRangeM(), t)));
        }
        return say(c, sb.toString().trim());
    }

    static int shot(CommandContext<CommandSourceStack> c, String weapon, String headId, double dist, String angleName) {
        var zones = HuntData.zones(StringArgumentType.getString(c, "species"));
        var w = HuntData.weapon(weapon);
        var head = HuntData.head(headId);
        Shot.Angle angle = Shot.Angle.valueOf(angleName.toUpperCase(Locale.ROOT));
        double[] d = switch (angle) {
            case BROADSIDE -> new double[] {-1, 0, 0};
            case QUARTERING_AWAY -> new double[] {-0.7, 0, 0.7};
            case QUARTERING_TOWARD -> new double[] {-0.7, 0, -0.7};
            case HEAD_ON -> new double[] {0, 0, -1};
            case STRAIGHT_AWAY -> new double[] {0, 0, 1};
        };
        var heart = zones.zones("standing").stream().filter(z -> z.name().equals("heart_lungs")).findFirst().orElseThrow();
        double[] o = {heart.center()[0] - d[0] * 3, heart.center()[1] - d[1] * 3, heart.center()[2] - d[2] * 3};
        var tr = zones.trace("standing", o, d, 8);
        Shot.Outcome out = Shot.resolve(tr == null ? null : tr.zone().name(), w.fullEnergyJ(), head, dist, w.effectiveRangeM(), angle, zones.killThresholdJ(),
                tr != null && HitZones.shoulder(tr), 0.5, HuntConfig.tuning());
        String wound = out.channel() == Shot.Channel.NONE ? "" : String.format(Locale.ROOT, "; dies in %.0f min, %.0f drops per 10 m",
                WoundModel.open(out.channel(), 0, head.bleedFactor(), 0.5).dieAfterMin(), WoundModel.open(out.channel(), 0, 1, 0.5).dropsPer10m());
        return say(c, String.format(Locale.ROOT, "%s %s/%s at %.0f m (%s) aimed at the heart and lungs: zone %s, E_eff %.1f J of %.1f needed, %s via %s%s",
                zones.species(), weapon, headId, dist, angle, tr == null ? "none" : tr.zone().name(), out.effJ(), zones.killThresholdJ(), out.result(), out.channel(), wound));
    }

    static int track(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        BlockPos at = p.blockPosition();
        BlockPos n = BloodTrail.next(p.serverLevel(), at, 8);
        if (n == null) return say(c, "No blood within 8 blocks.");
        int[] cnt = BloodTrail.count(p.serverLevel(), at, 8);
        return say(c, "Nearest blood " + n.toShortString() + " to the " + BloodTrail.compass(n.getX() - at.getX(), n.getZ() - at.getZ()) + "; " + cnt[0] + " patches, " + cnt[1] + " drops near.");
    }

    static int wound(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Shot.Channel ch = Shot.Channel.valueOf(StringArgumentType.getString(c, "channel").toUpperCase(Locale.ROOT));
        LivingEntity t = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8), e -> !(e instanceof Player) && e.isAlive())
                .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
        if (t == null) return say(c, "No animal within 8 blocks.");
        var w = WoundModel.open(ch, com.yearofice.ecosystem.clock.SliceClock.minuteD(p.serverLevel()), 1, p.getRandom().nextDouble());
        WoundTracker.wound(p.serverLevel(), t, w, p, HuntItems.DEFAULT_HEAD, p.getRandom().nextDouble(), true);
        return say(c, "Wounded " + t.getName().getString() + ": " + ch + ", " + w.look());
    }
}
