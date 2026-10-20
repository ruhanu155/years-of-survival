package com.yearofice.client.ui.credits;

import com.mojang.brigadier.Command;
import com.yearofice.core.YearOfIce;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Where the credits screen opens from: a small button on the pause and title screens, and {@code /yoicredits}. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class CreditsAccess {
    private CreditsAccess() {}

    @SubscribeEvent
    public static void screens(ScreenEvent.Init.Post e) {
        if (e.getScreen() instanceof PauseScreen || e.getScreen() instanceof TitleScreen) {
            var s = e.getScreen();
            e.addListener(Button.builder(Component.translatable("credits.yearofice.button"),
                    b -> Minecraft.getInstance().setScreen(new CreditsScreen(s))).bounds(4, s.height - 24, 70, 20).build());
        }
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoicredits").executes(c -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(new CreditsScreen(null)));
            return Command.SINGLE_SUCCESS;
        }));
    }
}
