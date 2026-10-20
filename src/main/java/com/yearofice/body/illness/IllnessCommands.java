package com.yearofice.body.illness;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyConfig;
import com.yearofice.body.BodyTicker;
import com.yearofice.core.YearOfIce;
import com.yearofice.core.rules.YoiConfig;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.IllnessDefs;
import com.yearofice.sim.body.IllnessModel;
import com.yearofice.sim.body.Skin;
import com.yearofice.sim.body.Wellness;
import com.yearofice.sim.body.WellnessTuning;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Lane B's {@code /yoi} commands, op level 2 (they join the tree {@code BodyCommands} and core register):
 *
 * <pre>
 *   /yoi body ill                         lists illnesses, vitamin C, nausea, Rest run, hair
 *   /yoi body ill &lt;illness&gt; [frac]        starts a register illness (onset rolled to frac 0-1, default 0)
 *   /yoi body ill cure &lt;illness&gt;          ends it
 *   /yoi body vitc &lt;days&gt;                 sets the vitamin C reserve
 *   /yoi body hair &lt;days&gt;                 sets hair_days;  /yoi body face grease|gloves|beard on|off
 *   /yoi preset gentle|standard|brutal    switches the realism preset
 * </pre>
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class IllnessCommands {
    private IllnessCommands() {}

    private static final SuggestionProvider<CommandSourceStack> ILLNESSES =
            (c, b) -> SharedSuggestionProvider.suggest(IllnessDefs.IDS, b);

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("body")
                        .then(Commands.literal("ill").executes(c -> list(c))
                                .then(Commands.literal("cure").then(Commands.argument("illness", StringArgumentType.word())
                                        .suggests(ILLNESSES).executes(c -> cure(c))))
                                .then(Commands.argument("illness", StringArgumentType.word()).suggests(ILLNESSES)
                                        .executes(c -> begin(c, 0))
                                        .then(Commands.argument("frac", DoubleArgumentType.doubleArg(0, 1))
                                                .executes(c -> begin(c, DoubleArgumentType.getDouble(c, "frac"))))))
                        .then(Commands.literal("vitc").then(Commands.argument("days", DoubleArgumentType.doubleArg(0, 30))
                                .executes(c -> vitc(c, DoubleArgumentType.getDouble(c, "days")))))
                        .then(Commands.literal("hair").then(Commands.argument("days", DoubleArgumentType.doubleArg(0, 1000))
                                .executes(c -> hair(c, DoubleArgumentType.getDouble(c, "days")))))
                        .then(Commands.literal("face")
                                .then(Commands.literal("grease").executes(c -> face(c, true)))
                                .then(Commands.literal("gloves").executes(c -> face(c, false)))
                                .then(Commands.literal("beard").then(Commands.argument("on", BoolArgumentType.bool())
                                        .executes(c -> beard(c, BoolArgumentType.getBool(c, "on")))))))
                .then(preset()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> preset() {
        LiteralArgumentBuilder<CommandSourceStack> p = Commands.literal("preset");
        for (YoiConfig.Preset v : YoiConfig.Preset.values()) {
            p.then(Commands.literal(v.name().toLowerCase(Locale.ROOT)).executes(c -> setPreset(c, v)));
        }
        return p.executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal("preset " + BodyConfig.tuning().preset()), false);
            return 1;
        });
    }

    private static int setPreset(CommandContext<CommandSourceStack> c, YoiConfig.Preset v) {
        YoiConfig.PRESET.set(v);
        c.getSource().sendSuccess(() -> Component.literal("preset " + v.name().toLowerCase(Locale.ROOT)
                + " (body: " + BodyConfig.tuning().preset() + ")"), true);
        return 1;
    }

    private static ServerPlayer me(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return c.getSource().getPlayerOrException();
    }

    private static int list(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = me(c);
        long m = BodyTicker.clock.minute(p.server);
        String text = wellnessLines(BodyAttachments.of(p), m);
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int begin(CommandContext<CommandSourceStack> c, double frac) throws CommandSyntaxException {
        ServerPlayer p = me(c);
        String id = StringArgumentType.getString(c, "illness");
        BodyState s = BodyAttachments.of(p);
        long now = BodyTicker.clock.minute(p.server);
        boolean ok = IllnessModel.begin(s, id, now, frac, Wellness.tuning(BodyConfig.tuning()));
        BodyTicker.sync(p, now, true);
        c.getSource().sendSuccess(() -> Component.literal(ok ? "started " + id + "\n" + wellnessLines(s, now)
                : id + " is not an illness the body steps"), true);
        return ok ? 1 : 0;
    }

    private static int cure(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = me(c);
        String id = StringArgumentType.getString(c, "illness");
        boolean ok = Illnesses.cure(p, id);
        c.getSource().sendSuccess(() -> Component.literal(ok ? "cured " + id : id + " was not on the list"), true);
        return ok ? 1 : 0;
    }

    private static int vitc(CommandContext<CommandSourceStack> c, double days) throws CommandSyntaxException {
        ServerPlayer p = me(c);
        BodyAttachments.of(p).vitC = days;
        c.getSource().sendSuccess(() -> Component.literal("vitamin C reserve " + days + " days"), true);
        return 1;
    }

    private static int hair(CommandContext<CommandSourceStack> c, double days) throws CommandSyntaxException {
        ServerPlayer p = me(c);
        BodyState s = BodyAttachments.of(p);
        s.hairDays = days;
        WellnessTuning w = Wellness.tuning(BodyConfig.tuning());
        c.getSource().sendSuccess(() -> Component.literal("hair_days " + days + ", beard "
                + Skin.stageName(Skin.beardStage(s, w))), true);
        BodyTicker.sync(p, BodyTicker.clock.minute(p.server), true);
        return 1;
    }

    private static int face(CommandContext<CommandSourceStack> c, boolean face) throws CommandSyntaxException {
        ServerPlayer p = me(c);
        if (face) Illnesses.greaseFace(p); else Illnesses.greaseGloves(p);
        c.getSource().sendSuccess(() -> Component.literal(face ? "face greased" : "gloves greased"), true);
        return 1;
    }

    private static int beard(CommandContext<CommandSourceStack> c, boolean on) throws CommandSyntaxException {
        ServerPlayer p = me(c);
        BodyAttachments.of(p).facialHair = on;
        c.getSource().sendSuccess(() -> Component.literal("facial_hair " + (on ? "on" : "off")), true);
        return 1;
    }

    /** Illness, vitamin C, nausea, Rest and hair, for {@code /yoi body ill} and {@code show}. */
    public static String wellnessLines(BodyState s, long m) {
        StringBuilder b = new StringBuilder();
        WellnessTuning w = Wellness.tuning(BodyConfig.tuning());
        b.append(String.format(Locale.ROOT, "Vitamin C %.1f d, nausea %.0f, Rest run %d (work %.0f, away %d, slept %d min)%n",
                s.vitC, s.nausea, s.restRun, s.restWorkMin, s.restAwayMin, s.restSleepMin));
        b.append(String.format(Locale.ROOT, "Hair %.1f d, beard %s%s, face grease %s, glove grease %s%n", s.hairDays,
                Skin.stageName(Skin.beardStage(s, w)), s.beardIced ? " (iced)" : "",
                Skin.greased(s, m) ? (s.faceGreaseUntil - m) + " min" : "no",
                s.gloveGreaseUntil > m ? "yes" : "no"));
        if (s.gutIll(m)) b.append("gut illness until ").append(s.gutUntil).append('\n');
        else if (s.gutOnset >= 0) b.append("gut illness shows at ").append(s.gutOnset).append('\n');
        if (s.ill.isEmpty()) b.append("No listed illnesses");
        for (IllnessModel.Active a : s.ill) {
            b.append(a.id()).append(a.start() > m ? " incubating, shows in " + (a.start() - m) + " min"
                    : String.format(Locale.ROOT, " %.1f h in", IllnessModel.hours(a, m)))
                    .append(a.until() < 0 ? ", until cured" : ", ends in " + (a.until() - m) + " min").append('\n');
        }
        return b.toString().stripTrailing();
    }
}
