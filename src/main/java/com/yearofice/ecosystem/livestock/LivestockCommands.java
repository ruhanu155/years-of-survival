package com.yearofice.ecosystem.livestock;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.ecosystem.YoiAnimal;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi livestock spawn <goat|reindeer|hen> [n] [adult|young]}, {@code feed <kg>}, {@code status} (op level 2). */
public final class LivestockCommands {
    private LivestockCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("livestock")
                .then(Commands.literal("spawn").then(Commands.argument("kind", StringArgumentType.word())
                        .executes(c -> spawn(c, 1, true))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 40)).executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "n"), true))
                                .then(Commands.literal("young").executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "n"), false))))))
                .then(Commands.literal("feed").then(Commands.argument("kg", IntegerArgumentType.integer(1, 500)).executes(LivestockCommands::feed)))
                .then(Commands.literal("status").executes(LivestockCommands::status))));
    }

    private static int say(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static EntityType<?> type(String kind) {
        return switch (kind) {
            case "goat" -> LivestockRegistry.GOAT.get();
            case "reindeer" -> LivestockRegistry.REINDEER.get();
            case "hen" -> LivestockRegistry.HEN.get();
            default -> null;
        };
    }

    private static int spawn(CommandContext<CommandSourceStack> c, int n, boolean adult) throws CommandSyntaxException {
        EntityType<?> t = type(StringArgumentType.getString(c, "kind"));
        if (t == null) return say(c, "kind is goat, reindeer or hen");
        var level = c.getSource().getLevel();
        var pos = c.getSource().getPosition();
        for (int i = 0; i < n; i++) {
            var e = t.create(level);
            if (!(e instanceof YoiAnimal a) || !(e instanceof Livestock l)) continue;
            a.moveTo(pos.x + (i % 4) * 0.8, pos.y, pos.z + (i / 4) * 0.8, 0, 0);
            if (!adult) { l.core().progress = 0; l.core().ageDays = 0; }
            l.core().female = i % 5 != 4;
            if (l.core().female && adult) l.core().lastBirthDay = com.yearofice.api.clock.ClockService.get().absDay() - 30;
            level.addFreshEntity(a);
        }
        return say(c, "spawned " + n + " " + (adult ? "adult " : "young ") + StringArgumentType.getString(c, "kind"));
    }

    private static int feed(CommandContext<CommandSourceStack> c) {
        double kg = IntegerArgumentType.getInteger(c, "kg");
        var level = c.getSource().getLevel();
        var near = level.getEntitiesOfClass(YoiAnimal.class, new AABB(c.getSource().getPosition(), c.getSource().getPosition()).inflate(8), a -> a instanceof Livestock);
        for (YoiAnimal a : near) {
            var core = ((Livestock) a).core();
            core.feedKg += kg;
            core.waterL += kg;
        }
        return say(c, "fed " + near.size() + " animals " + kg + " kg of hay and " + kg + " L of water each");
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        var level = c.getSource().getLevel();
        var near = level.getEntitiesOfClass(YoiAnimal.class, new AABB(c.getSource().getPosition(), c.getSource().getPosition()).inflate(16), a -> a instanceof Livestock);
        near.forEach(a -> say(c, LivestockSystem.status(a, ((Livestock) a).core())));
        return say(c, near.size() + " animals within 16 blocks");
    }
}
