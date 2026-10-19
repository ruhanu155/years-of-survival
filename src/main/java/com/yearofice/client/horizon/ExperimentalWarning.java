package com.yearofice.client.horizon;

import com.yearofice.core.YearOfIce;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * No "Experimental Settings" warning for our own presets (World creation options and presets). Vanilla calls any
 * overworld that is not its own noise generator experimental ({@code WorldDimensions.checkStability}), so every
 * Year of Ice world would ask; NeoForge 21.1 has no setting for it. When Create World is about to open that
 * warning and the chosen preset is {@code yearofice:*}, the warning is skipped and its "Proceed" runs, the same as
 * clicking it. Other presets (vanilla's, other mods') still warn. NeoForge already marks the world confirmed, so
 * it never asks again when the world is reopened.
 *
 * <p>Reads {@link ConfirmScreen#callback} through the access transformer ({@code META-INF/accesstransformer.cfg}).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class ExperimentalWarning {
    private ExperimentalWarning() {}

    static final String KEY = "selectWorld.warning.experimental.title";

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening e) {
        if (!(e.getNewScreen() instanceof ConfirmScreen confirm) || !isWarning(confirm)) return;
        if (!(e.getCurrentScreen() instanceof CreateWorldScreen create) || !ours(create.getUiState())) return;
        e.setCanceled(true);
        YearOfIce.LOG.info("Experimental-settings warning skipped for preset {}", presetId(create.getUiState()));
        // Next tick, outside the screen change: proceed exactly as the warning's "Proceed" button would.
        Minecraft.getInstance().tell(() -> confirm.callback.accept(true));
    }

    static boolean isWarning(Screen s) {
        return s.getTitle().getContents() instanceof TranslatableContents t && KEY.equals(t.getKey());
    }

    /** True when the Create World screen's chosen preset is one of ours. */
    static boolean ours(WorldCreationUiState ui) {
        return YearOfIce.MOD_ID.equals(presetId(ui).split(":", 2)[0]);
    }

    static String presetId(WorldCreationUiState ui) {
        WorldCreationUiState.WorldTypeEntry t = ui.getWorldType();
        if (t == null || t.preset() == null) return "";
        return t.preset().unwrapKey().map(k -> k.location().toString()).orElse("");
    }
}
