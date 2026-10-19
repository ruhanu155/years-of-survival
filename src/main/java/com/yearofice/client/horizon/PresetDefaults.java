package com.yearofice.client.horizon;

import com.yearofice.core.YearOfIce;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Selects {@code yearofice:year_of_ice} in the Create World screen by default (World creation options and presets):
 * our data pack makes the normal preset list ours only, but vanilla still starts the screen on
 * {@code minecraft:normal}. Once per screen, so a player's own choice is kept across resizes.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class PresetDefaults {
    private PresetDefaults() {}

    private static final Set<CreateWorldScreen> DONE = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof CreateWorldScreen s) || !DONE.add(s)) return;
        select(s.getUiState(), "year_of_ice");
    }

    /** Picks our preset {@code yearofice:<name>} from the normal list; true if found. */
    public static boolean select(WorldCreationUiState ui, String name) {
        for (WorldCreationUiState.WorldTypeEntry t : ui.getNormalPresetList()) {
            if (t.preset() != null && t.preset().unwrapKey().map(k -> k.location().getNamespace().equals(YearOfIce.MOD_ID)
                    && k.location().getPath().equals(name)).orElse(false)) {
                ui.setWorldType(t);
                return true;
            }
        }
        return false;
    }
}
