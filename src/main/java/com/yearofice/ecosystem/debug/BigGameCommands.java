package com.yearofice.ecosystem.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.api.env.EnvService;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.ecosystem.species.rig.BigGameEntity;
import com.yearofice.ecosystem.species.rig.PredatorEntity;
import com.yearofice.ecosystem.spawn.Habitats;
import com.yearofice.ecosystem.spawn.SlotSpawner;
import com.yearofice.sim.ecosystem.FieldCare;
import com.yearofice.sim.ecosystem.Straggler;
import com.yearofice.sim.ecosystem.Young;
import com.yearofice.sim.time.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * M6 lane A debug commands: {@code /yoi carcass <target> info}, {@code /yoi herd}, {@code /yoi kin},
 * {@code /yoi show scent|tiers} and {@code /yoi deer straggler|info}.
 */
public final class BigGameCommands {
    private BigGameCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("carcass").then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.literal("info").executes(BigGameCommands::carcassInfo))))
                .then(Commands.literal("herd").executes(BigGameCommands::herd))
                .then(Commands.literal("kin").executes(BigGameCommands::kin))
                .then(Commands.literal("show")
                        .then(Commands.literal("scent").executes(BigGameCommands::scent))
                        .then(Commands.literal("tiers").executes(BigGameCommands::tiers)))
                .then(Commands.literal("deer")
                        .then(Commands.literal("straggler").executes(BigGameCommands::straggler))
                        .then(Commands.literal("info").executes(BigGameCommands::deerInfo))));
    }

    static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    static int carcassInfo(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Entity e = EntityArgument.getEntity(c, "target");
        if (!(e instanceof CarcassEntity ce)) {
            c.getSource().sendFailure(Component.literal("Not a carcass."));
            return 0;
        }
        ServerLevel level = c.getSource().getLevel();
        CarcassRecord r = ce.record();
        SpeciesDef d = SpeciesBook.get(r.species());
        double airC = EnvService.installed() ? EnvService.get().airC(level, ce.blockPosition()) : 5;
        double since = (SliceClock.minuteD(level) - r.deathMinute()) / 60.0;
        double quarterBy = FieldCare.quarterByHours(airC);
        double frozenAt = FieldCare.freezesSolidHours(r.species(), airC);
        FieldCare.Result res = FieldCare.dress(since + d.steps().values().stream().limit(2).mapToDouble(Double::doubleValue).sum() / 60.0,
                since + com.yearofice.ecosystem.carcass.Butchering.minutesThrough(d, "quarter") / 60.0, airC);
        return say(c, String.format("%s %s, %.1f kg, class %s, died by %s %.1f h ago, air %.1f C%n"
                        + "gut by 1 h, quarter by %.0f h, freezes solid at %s h%n"
                        + "dressed now: %.0f%% of the meat usable (late gut loses %.0f%%, turning %.0f%%), dressing takes %.0f game min%s",
                r.species(), r.sex() == 1 ? "male" : "female", r.weightG() / 1000.0, d.carcassClass(), r.cause(), since, airC,
                quarterBy, Double.isInfinite(frozenAt) ? "never" : String.format("%.0f", frozenAt),
                res.usable() * 100, res.lost() * 100, res.turning() * 100, com.yearofice.ecosystem.carcass.Butchering.minutes(r),
                r.weightG() > CarcassEntity.CARRY_MAX_G ? "; too heavy to lift, dressed in place" : "; can be lifted whole"));
    }

    static YoiAnimal nearest(CommandContext<CommandSourceStack> c, double r) {
        Vec3 p = c.getSource().getPosition();
        YoiAnimal best = null;
        for (YoiAnimal a : c.getSource().getLevel().getEntitiesOfClass(YoiAnimal.class, new AABB(p, p).inflate(r)))
            if (best == null || a.distanceToSqr(p) < best.distanceToSqr(p)) best = a;
        return best;
    }

    static int herd(CommandContext<CommandSourceStack> c) {
        YoiAnimal a = nearest(c, 64);
        if (a == null) {
            c.getSource().sendFailure(Component.literal("No animal within 64 blocks."));
            return 0;
        }
        List<? extends YoiAnimal> mates = a instanceof BigGameEntity b ? b.herd() : c.getSource().getLevel().getEntitiesOfClass(YoiAnimal.class,
                a.getBoundingBox().inflate(BigGameEntity.SPREAD_M), x -> x != a && x.speciesId().equals(a.speciesId()));
        StringBuilder sb = new StringBuilder(String.format("%s at %s: %d herd-mate(s) within %.0f m; alarm spreads to them in about half a second%n",
                a.speciesPath(), a.blockPosition().toShortString(), mates.size(), BigGameEntity.SPREAD_M));
        sb.append("self ").append(a.state()).append(", awareness ").append(Math.round(a.awareness().max()));
        for (YoiAnimal m : mates) sb.append(String.format("%n- %s %s, awareness %d", m.speciesPath(), m.state(), Math.round(m.awareness().max())));
        return say(c, sb.toString());
    }

    static int kin(CommandContext<CommandSourceStack> c) {
        YoiAnimal a = nearest(c, 64);
        if (a == null) {
            c.getSource().sendFailure(Component.literal("No animal within 64 blocks."));
            return 0;
        }
        long minute = SliceClock.minute(c.getSource().getLevel());
        int doy = Calendar.dayOfYearOfMinute(minute);
        String sp = a.speciesPath();
        Young.Spec spec = Young.TABLE.get(sp);
        List<YoiAnimal> same = c.getSource().getLevel().getEntitiesOfClass(YoiAnimal.class, a.getBoundingBox().inflate(100), x -> x.speciesId().equals(a.speciesId()));
        String young = spec == null ? "no young in the file" : Young.hasYoung(sp, doy)
                ? String.format("young now: %s, litter %d-%d, mother defends/keeps them %d weeks", spec.plan(), spec.litterMin(), spec.litterMax(), spec.weeks())
                : String.format("no young at %s (born %d/%d to %d/%d)", Calendar.shortDate(doy), spec.fromMonth(), spec.fromDay(), spec.toMonth(), spec.toDay());
        return say(c, String.format("%s, %s: %d of its kind within 100 m; %s", sp, a.male() ? "male" : "female", same.size(), young));
    }

    static int scent(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos at = BlockPos.containing(c.getSource().getPosition());
        var w = EnvService.get().wind(level, at);
        StringBuilder sb = new StringBuilder(String.format("Wind from %.0f deg, %.1f m/s (gust %.1f). Your plume is a 25-degree cone toward %.0f deg.%n",
                w.fromDeg(), w.ms(), w.gustMs(), (w.fromDeg() + 180) % 360));
        Map<String, double[]> by = new TreeMap<>();
        for (SpeciesDef d : SpeciesBook.all()) if (d.senses().smellM() > 0) by.put(d.id().substring(d.id().indexOf(':') + 1),
                new double[] {d.senses().smellM(), d.senses().sightM(), d.senses().hearingM()});
        by.forEach((k, v) -> sb.append(String.format("%s: smell %.0f m, sight %.0f m, hears %.0f m%n", k, v[0], v[1], v[2])));
        return say(c, sb.toString().stripTrailing());
    }

    static int tiers(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos at = BlockPos.containing(c.getSource().getPosition());
        Map<String, int[]> census = SlotSpawner.census(level, at, 512);
        int slots = 0, bodies = 0;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, int[]> e : census.entrySet()) {
            slots += e.getValue()[0];
            bodies += e.getValue()[2];
            if (e.getValue()[0] > 0) sb.append(String.format("%n%s: %d slots, %d bodies", e.getKey().substring(e.getKey().indexOf(':') + 1), e.getValue()[0], e.getValue()[2]));
        }
        return say(c, String.format("Active tier (bodies within %.0f m): %d bodies of %d allowed. Within 512 m: %d slots. Nearby and Abstract tiers arrive with M7.%s",
                EcoConfig.tuning().activeRadiusM(), SlotSpawner.bodies(), EcoConfig.tuning().activeCap(), slots, sb));
    }

    static int straggler(CommandContext<CommandSourceStack> c) {
        if (!EcoConfig.flagOn("yearofice:deer")) {
            c.getSource().sendFailure(Component.literal("The deer content flag (yearofice:deer) is off."));
            return 0;
        }
        ServerLevel level = c.getSource().getLevel();
        Vec3 look = Vec3.directionFromRotation(0, c.getSource().getRotation().y);
        Vec3 at = c.getSource().getPosition().add(look.scale(4));
        BlockPos g = Habitats.surface(level, (int) Math.floor(at.x), (int) Math.floor(at.z));
        var deer = SlotSpawner.spawn(level, com.yearofice.ecosystem.EcosystemRegistry.MULE_DEER.get(), g, "debug:straggler-" + UUID.randomUUID());
        if (deer == null) return 0;
        deer.setMale(true);
        return say(c, "A yearling mule deer buck, driven off by his mother, stands in front of you.");
    }

    static int deerInfo(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        long minute = SliceClock.minute(level);
        int year = Calendar.yearOfDay(Calendar.absDayOfMinute(minute));
        StringBuilder sb = new StringBuilder("deer flag " + (EcoConfig.flagOn("yearofice:deer") ? "on" : "off") + "; straggler years (N=4, winter snow 80 cm):");
        boolean prev = false;
        for (int y = 1; y <= 10; y++) {
            Straggler.Year s = Straggler.roll(level.getSeed(), y, prev, 80, 4);
            prev = s.straggler();
            if (s.straggler()) sb.append(String.format("%n year %d: arrives day %d after Jun 1, stays %d days, %s", y, s.arrivalDay(), s.stayDays(),
                    s.killedByWolves() ? "wolves kill him" : "walks out"));
        }
        return say(c, sb + "\nthis is year " + year);
    }
}
