package com.yearofice.body.clothing;

import com.mojang.brigadier.CommandDispatcher;
import com.yearofice.api.body.ClothingService;
import com.yearofice.body.BodyAttachments;
import com.yearofice.sim.body.WornGarment;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi wear [dress|strip]}: worn items per zone with warmth and wet state; dress = start kit, strip = bare skin. */
@EventBusSubscriber(modid = com.yearofice.core.YearOfIce.MOD_ID)
public final class WearCommand {
    private WearCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("wear")
                .executes(c -> list(c.getSource(), c.getSource().getPlayerOrException()))
                .then(Commands.literal("dress").executes(c -> {
                    StartOutfit.dress(c.getSource().getPlayerOrException());
                    return list(c.getSource(), c.getSource().getPlayerOrException());
                }))));
    }

    static int list(CommandSourceStack src, ServerPlayer p) {
        double[] wet = BodyAttachments.of(p).slotWet;
        StringBuilder b = new StringBuilder("Worn" + (StartOutfit.dressed(p) ? "" : " (not dressed: implied start kit)") + "\n");
        double sum = 0;
        for (WornGarment w : ClothingService.get().worn(p)) {
            var g = w.garment();
            sum += g.warmthC();
            b.append(String.format(Locale.ROOT, "%-7s %-5s %-24s warmth %+.1f wind %.2f wet %.0f%% cond %.0f%%%n", g.zone().id(),
                    g.layer().name().toLowerCase(Locale.ROOT), g.id(), g.warmthC(), g.windBlock(), wet[g.slot()] * 100,
                    w.condition() * 100));
        }
        b.append(String.format(Locale.ROOT, "Dry sum %+.1f C", sum));
        src.sendSuccess(() -> Component.literal(b.toString()), false);
        return 1;
    }
}
