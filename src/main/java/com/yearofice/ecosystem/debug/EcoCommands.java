package com.yearofice.ecosystem.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.spawn.Habitats;
import com.yearofice.ecosystem.spawn.SlotSpawner;
import com.yearofice.ecosystem.species.SnowshoeHareEntity;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.sim.ecosystem.Density;
import com.yearofice.sim.time.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi spawn} and {@code /yoi eco}: the ecosystem's debug commands.
 *
 * <ul>
 *   <li>{@code spawn <hare|snowshoe_hare|ptarmigan|willow_ptarmigan|moose> [count]}: bodies 3 m in front of the
 *       source, through the slot spawner (so they are counted), as {@code debug:} slots.</li>
 *   <li>{@code eco density}: the hare cycle's year and multiplier, densities, the hare habitat share within 32 m and
 *       the hares whose home ranges cover this point; slots, empty slots and bodies by species within 128 m.</li>
 *   <li>{@code eco info}: the nearest animal within 32 m: state, awareness, threat and flee progress.</li>
 * </ul>
 * The {@code yoi} root is registered by core; Brigadier merges this subtree into it.
 */
public final class EcoCommands {
    private EcoCommands() {}

    static final String[] NAMES = {"hare", "snowshoe_hare", "ptarmigan", "willow_ptarmigan", "moose"};
    private static final SimpleCommandExceptionType UNKNOWN = new SimpleCommandExceptionType(Component.literal("Unknown species."));

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn").then(Commands.argument("species", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(NAMES, b))
                        .executes(c -> spawn(c, 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 20)).executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "count"))))))
                .then(Commands.literal("eco")
                        .then(Commands.literal("density").executes(EcoCommands::density))
                        .then(Commands.literal("info").executes(EcoCommands::info))));
    }

    public static EntityType<? extends YoiAnimal> type(String name) {
        return switch (name) {
            case "hare", "snowshoe_hare" -> EcosystemRegistry.SNOWSHOE_HARE.get();
            case "ptarmigan", "willow_ptarmigan" -> EcosystemRegistry.WILLOW_PTARMIGAN.get();
            case "moose" -> EcosystemRegistry.MOOSE.get();
            default -> null;
        };
    }

    static int spawn(CommandContext<CommandSourceStack> c, int count) throws CommandSyntaxException {
        EntityType<? extends YoiAnimal> t = type(StringArgumentType.getString(c, "species"));
        if (t == null) throw UNKNOWN.create();
        CommandSourceStack src = c.getSource();
        ServerLevel level = src.getLevel();
        Vec3 look = Vec3.directionFromRotation(0, src.getRotation().y);
        int made = 0;
        for (int i = 0; i < count; i++) {
            Vec3 at = src.getPosition().add(look.scale(3 + i * 0.8));
            BlockPos ground = Habitats.surface(level, (int) Math.floor(at.x), (int) Math.floor(at.z));
            if (SlotSpawner.spawn(level, t, ground, "debug:" + UUID.randomUUID()) != null) made++;
        }
        int n = made;
        src.sendSuccess(() -> Component.literal("Spawned " + n + " " + EntityType.getKey(t).getPath() + "."), true);
        return n;
    }

    static int density(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        ServerLevel level = src.getLevel();
        BlockPos at = BlockPos.containing(src.getPosition());
        long minute = SliceClock.minute(level);
        SpeciesDef hare = SpeciesBook.get(SnowshoeHareEntity.ID);
        double mult = SlotSpawner.hareMultiplier(level, minute);
        double share = Habitats.share(level, at, 32, hare.habitat(), 32, level.getSeed());
        double n = Density.coveringPoint(EcoConfig.tuning().hareDensityPerKm2(), hare.homeRangeM(), share, mult);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Day %d (%s), hare cycle year %d, x%.2f | hares %.0f/km2 at K, ptarmigan groups %.0f/km2, moose %.1f/km2%n",
                Calendar.absDayOfMinute(minute), Calendar.shortDate(Calendar.dayOfYearOfMinute(minute)), SlotSpawner.cycleYear(level, minute) + 1, mult,
                EcoConfig.tuning().hareDensityPerKm2(), EcoConfig.tuning().ptarmiganGroupsPerKm2(), EcoConfig.tuning().mooseDensityPerKm2()));
        sb.append(String.format("Hare habitat within 32 m: %.0f%% | hares whose range covers here: %.2f%n", share * 100, n));
        for (Map.Entry<String, int[]> e : SlotSpawner.census(level, at, 128).entrySet()) {
            int[] v = e.getValue();
            sb.append(String.format("%s: %d slots within 128 m, %d empty, %d bodies%n", e.getKey(), v[0], v[1], v[2]));
        }
        sb.append("Bodies in the level: ").append(SlotSpawner.bodies()).append(" of ").append(EcoConfig.tuning().activeCap());
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    static int info(CommandContext<CommandSourceStack> c) {
        CommandSourceStack src = c.getSource();
        ServerLevel level = src.getLevel();
        Vec3 p = src.getPosition();
        List<YoiAnimal> list = level.getEntitiesOfClass(YoiAnimal.class, new AABB(p, p).inflate(32));
        YoiAnimal a = null;
        for (YoiAnimal x : list) if (a == null || x.distanceToSqr(p) < a.distanceToSqr(p)) a = x;
        if (a == null) {
            src.sendFailure(Component.literal("No animal within 32 blocks."));
            return 0;
        }
        LivingEntity th = a.awareness().top();
        YoiAnimal f = a;
        String line = String.format("%s (%s, slot %s): %s, awareness %.0f%s, flee %.1f m, home %s",
                a.speciesPath(), a.male() ? "male" : "female", a.slotKey().isEmpty() ? "none" : a.slotKey(), a.state(),
                a.awareness().max(), th == null ? "" : String.format(" toward %s at %.1f m", th.getName().getString(), th.distanceTo(a)),
                a.fleeCoveredM(), a.home().toShortString());
        src.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }
}
